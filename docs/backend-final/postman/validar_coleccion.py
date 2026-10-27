"""Valida Postman contra fuentes Java y ejecuta scripts en un sandbox sin HTTP.

No inicia servicios, no usa credenciales ni conecta a PostgreSQL. Requiere
Python 3 y Node.js disponibles; solo lee archivos del repositorio.
"""
import json
import re
import shutil
import subprocess
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
JAVA = ROOT / 'backend/inventario/src/main/java/com/utec/inventario'


def check(condition, message):
    if not condition:
        raise ValueError(message)


def fields_of(class_name, kind):
    path = JAVA / 'dto' / kind / (class_name + '.java')
    text = path.read_text(encoding='utf-8-sig')
    text = re.sub(r'@\w+(?:\([^)]*\))?', '', text)
    fields = set(re.findall(r'\bprivate\s+[\w<>,\s?]+\s+(\w+)\s*(?:=[^;]*)?;', text))
    record = re.search(r'public\s+record\s+\w+\(([^)]*)\)', text, re.S)
    if record:
        fields.update(part.strip().split()[-1] for part in record[1].split(','))
    return fields


def normalize(path):
    path = path.replace('{{baseUrl}}', '').split('?')[0]
    return re.sub(r'\{\{[^}]+\}\}|\{[^}]+\}', '{}', path)


def requests(items):
    for item in items:
        if 'request' in item:
            yield item
        else:
            yield from requests(item.get('item', []))


def main():
    collection = json.loads((HERE / 'Inventario-Laboratorios.postman_collection.json').read_text(encoding='utf-8-sig'))
    environment = json.loads((HERE / 'Inventario-Laboratorios.postman_environment.json').read_text(encoding='utf-8-sig'))
    check(collection['info']['schema'] == 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json', 'Formato de colección distinto de 2.1.')
    check(collection['auth'] == {'type': 'bearer', 'bearer': [{'key': 'token', 'value': '{{tokenAdmin}}', 'type': 'string'}]}, 'Bearer ADMIN heredado incorrecto.')
    values = {item['key']: item['value'] for item in environment['values']}
    check(len(values) == len(environment['values']), 'Variables duplicadas.')
    check(values['habilitarEscrituras'] == 'false', 'La plantilla debe bloquear escrituras inicialmente.')
    for key, value in values.items():
        if key.startswith(('password', 'token', 'userName', 'id')) or key == 'nuevoUserName':
            check(value == '', 'Debe entregarse vacía la variable sensible/ID: ' + key)

    expected = {}
    controllers = sorted((JAVA / 'controller').glob('*.java'))
    for path in controllers:
        text = path.read_text(encoding='utf-8-sig')
        base = re.search(r'@RequestMapping\("([^"]+)"\)', text)[1]
        for match in re.finditer(r'@(Get|Post|Put|Patch|Delete)Mapping(?:\("([^"]*)"\))?', text):
            key = (match[1].upper(), normalize(base + (match[2] or '')))
            tail = text[match.end():]
            return_type = re.search(r'\bpublic\s+([\w<>?, ]+)\s+\w+\s*\(', tail)[1]
            response = re.findall(r'\b\w+Response\b', return_type)
            request_dto = re.search(r'@RequestBody\s+(\w+Request)', tail.split('{', 1)[0])
            # IDs in @PathVariable strings contain braces only in the Mapping,
            # not inside a method signature; source annotations can span lines.
            expected[key] = {'response': response[0] if response else None,
                             'request': request_dto[1] if request_dto else None}

    actual = set()
    items = list(requests(collection['item']))
    bodies = filters = 0
    for item in items:
        request = item['request']
        url = request['url']
        raw = url if isinstance(url, str) else url['raw']
        key = (request['method'], normalize(raw))
        check(key in expected, 'Ruta ausente de Controllers: ' + str(key))
        actual.add(key)
        if key == ('POST', '/api/auth/login'):
            check(request.get('auth', {}).get('type') == 'noauth', 'Login debe usar No Auth.')
        for variable in re.findall(r'\{\{([\w]+)\}\}', json.dumps(request)):
            check(variable.removeprefix('json_') in values, 'Variable no declarada: ' + variable)
        description = request['description']
        body = request.get('body')
        if body:
            check(body['mode'] == 'raw' and body['options']['raw']['language'] == 'json', 'Body debe ser JSON raw.')
            dto = re.search(r'DTO de entrada: (\w+Request)\.', description)
            check(dto is not None, 'Falta referencia a DTO: ' + item['name'])
            if expected[key]['request']:
                check(dto[1] == expected[key]['request'], 'DTO distinto del Controller: ' + item['name'])
            template = body['raw']
            def substitute(match):
                name = match[1]
                if name.startswith('json_id'): return '1'
                if name in ('json_activoCatalogo', 'json_activoUsuario'): return 'true'
                if name.startswith('json_'): return json.dumps('valor-sintetico')
                return 'verificacion'
            example = json.loads(re.sub(r'\{\{(\w+)\}\}', substitute, template))
            check(set(example) == fields_of(dto[1], 'request'), 'Campos distintos del DTO ' + dto[1] + ': ' + str(sorted(set(example))))
            bodies += 1
        else:
            check(request['method'] != 'GET' or not body, 'GET con cuerpo inesperado.')
        queries = url.get('query', []) if isinstance(url, dict) else []
        for parameter in queries:
            check(parameter.get('disabled') is True, 'Filtro opcional habilitado inicialmente: ' + parameter['key'])
            check(bool(parameter.get('description')), 'Filtro sin descripción.')
        filter_dto = re.search(r'DTO de filtros: (\w+Request)\.', description)
        if filter_dto:
            check({parameter['key'] for parameter in queries} == fields_of(filter_dto[1], 'request'), 'Filtros distintos de ' + filter_dto[1])
            filters += 1
        events = item.get('event', [])
        test = next(('\n'.join(e['script']['exec']) for e in events if e['listen'] == 'test'), '')
        check('HTTP esperado' in test, 'Falta check de status.')
        response_class = expected[key]['response']
        if response_class and not re.search(r'HTTP esperado (401|403)', test):
            checked = set(re.findall(r"pm\.expect\((?:data|item)\)\.to\.have\.property\('([^']+)'\)", test))
            check(checked <= fields_of(response_class, 'response'), 'Test comprueba campos ajenos a ' + response_class + ': ' + str(checked))
        for event in events:
            code = '\n'.join(event['script']['exec'])
            check(not re.search(r'pm\.sendRequest|\bfetch\s*\(|XMLHttpRequest|\brequire\s*\(|\bimport\s*\(|child_process|process\.', code), 'Script con capacidad externa no admitida.')
    check(actual == set(expected), 'Cobertura distinta del código. Faltan: ' + str(sorted(set(expected) - actual)))
    check(len(actual) == 70, 'El total cambió: revisar documentación y contrato.')
    node = shutil.which('node')
    check(node is not None, 'Se requiere Node.js para validar scripts en VM sin HTTP.')
    result = subprocess.run([node, str(HERE / 'validar_scripts.cjs')],
                            input=json.dumps({'collection': collection, 'environment': environment}),
                            text=True, capture_output=True, encoding='utf-8', check=True)
    sandbox = json.loads(result.stdout)
    print(json.dumps({'controllers': len(controllers), 'operaciones': len(actual),
                      'solicitudes': len(items), 'cuerpos_dto': bodies,
                      'listados_filtro_dto': filters, 'sandbox': sandbox,
                      'http_real': False}, ensure_ascii=False))


if __name__ == '__main__':
    main()
