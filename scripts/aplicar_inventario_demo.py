"""Carga local mediante la API; sin --aplicar solo valida el plan, sin HTTP.

Los datos, manifiesto, respaldo y credenciales permanecen fuera de Git.
No escribe directamente PostgreSQL ni inventa eventos de auditoría.
"""
from __future__ import annotations

import argparse
import datetime as dt
import getpass
import hashlib
import json
import os
from pathlib import Path
import secrets
import sys
import urllib.error
import urllib.request


BASE_URL = "http://127.0.0.1:8080/api"
OWNERS = ("marko", "aldo", "jason", "jorge")
DEMO_ROLES = {"jason": "ADMIN", "jorge": "GESTOR", "gabriela": "ADMIN"}
LABS = {"L201", "L409", "L413", "L414"}
OPTIONAL_FIELDS = ("serieUtec", "numeroSerie", "marca", "modelo", "anio",
                   "ordenCompra", "ubicacionInterna", "comentario", "idResponsable")
EQUIPMENT_FIELDS = ("codigoInterno", "nombre", "estado", "requiereMantenimiento") + OPTIONAL_FIELDS
LIMITS = {"codigoInterno": 50, "nombre": 150, "serieUtec": 100, "numeroSerie": 100,
          "marca": 100, "modelo": 100, "ordenCompra": 50, "ubicacionInterna": 200}


class ImportFailure(Exception):
    """Mensaje seguro: nunca contiene cuerpos HTTP, claves ni tokens."""


class ApiFailure(ImportFailure):
    def __init__(self, method, path, status=None):
        self.status = status
        super().__init__(f"{method} {path}: HTTP {status}" if status else
                         f"{method} {path}: respuesta desconocida por fallo de conexión; reanude para comprobarla.")


def digest_file(path):
    value = hashlib.sha256()
    with Path(path).open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            value.update(chunk)
    return value.hexdigest()


def digest_json(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True, ensure_ascii=False,
                                     separators=(",", ":")).encode("utf-8")).hexdigest()


def load_json(path):
    try:
        with Path(path).open(encoding="utf-8-sig") as stream:
            return json.load(stream)
    except (OSError, ValueError):
        raise ImportFailure(f"No se pudo leer JSON válido: {Path(path).name}") from None


def atomic_json(path, value):
    path = Path(path)
    temporary = path.with_suffix(path.suffix + ".tmp")
    try:
        descriptor = os.open(temporary, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
        with os.fdopen(descriptor, "w", encoding="utf-8", newline="\n") as stream:
            json.dump(value, stream, ensure_ascii=False, indent=2)
            stream.write("\n")
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(temporary, path)
        os.chmod(path, 0o600)
    finally:
        if temporary.exists():
            temporary.unlink()


def positive_id(value):
    return type(value) is int and 0 < value <= 2147483647


def source_key(row):
    return f"{row['source_sheet']}:{row['source_row']}"


def normalized_payload(plan, row):
    payload = {key: row["payload"].get(key) for key in EQUIPMENT_FIELDS}
    for key, value in payload.items():
        if isinstance(value, str):
            payload[key] = value.strip() or None
    payload["requiereMantenimiento"] = row["payload"].get("requiereMantenimiento", False)
    marker = (f"Origen Excel: {plan['source_sha256']}; "
              f"hoja={row['source_sheet']}; fila={row['source_row']}")
    comment = payload.get("comentario") or ""
    if marker not in comment:
        payload["comentario"] = (comment + "\n" + marker).strip()
    return payload


def validate_plan(path):
    path = Path(path).resolve()
    plan = load_json(path)
    if not isinstance(plan, dict) or plan.get("version") != 1:
        raise ImportFailure("Versión de plan no admitida.")
    sha = plan.get("source_sha256", "")
    if not isinstance(sha, str) or len(sha) != 64 or any(c not in "0123456789abcdef" for c in sha):
        raise ImportFailure("El hash de origen no es SHA-256 válido.")
    local = os.environ.get("LOCALAPPDATA")
    if not local or path != (Path(local) / "inventario-laboratorios" / "importacion-demo" /
                             sha[:12] / "plan.json").resolve():
        raise ImportFailure("El plan debe estar en la carpeta privada LOCALAPPDATA de esta importación.")
    if not isinstance(plan.get("source_name"), str) or not plan["source_name"].strip():
        raise ImportFailure("Falta el nombre del archivo de origen.")
    assignments = plan.get("assignments")
    if (not isinstance(assignments, dict) or set(assignments) != set(OWNERS)
            or set(assignments.values()) != LABS):
        raise ImportFailure("Las asignaciones deben repartir los cuatro laboratorios entre los cuatro usuarios.")
    users = plan.get("users")
    if not isinstance(users, list) or len(users) != 3:
        raise ImportFailure("El plan debe contener las tres cuentas demo.")
    by_name = {}
    for user in users:
        if not isinstance(user, dict):
            raise ImportFailure("Cuenta demo inválida.")
        name = user.get("userName")
        if (name not in DEMO_ROLES or name in by_name or user.get("rol") != DEMO_ROLES[name]
                or user.get("email") != f"{name}@inventario.test" or user.get("activo") is not True
                or set(user) - {"userName", "nombre", "apellido", "email", "cargo", "rol", "activo"}):
            raise ImportFailure("Identidad, rol o campos de cuenta demo inválidos.")
        for field in ("nombre", "apellido"):
            if not isinstance(user.get(field), str) or not 0 < len(user[field].strip()) <= 100:
                raise ImportFailure("Nombre/apellido demo inválido.")
        if user.get("cargo") is not None and (not isinstance(user["cargo"], str) or len(user["cargo"]) > 100):
            raise ImportFailure("Cargo demo inválido.")
        by_name[name] = user
    equipment = plan.get("equipment")
    if not isinstance(equipment, list) or not equipment or not isinstance(plan.get("pending"), list):
        raise ImportFailure("Equipos o pendientes del plan inválidos.")
    seen_rows, seen_codes = set(), set()
    for row in equipment:
        if not isinstance(row, dict) or row.get("lab") not in LABS or row.get("source_sheet") != row["lab"]:
            raise ImportFailure("Hoja/laboratorio del equipo inválido.")
        if type(row.get("source_row")) is not int or row["source_row"] < 2 or (
                row["lab"] == "L201" and row["source_row"] > 128):
            raise ImportFailure("Fila del equipo fuera del alcance autorizado.")
        if row.get("actor") not in OWNERS or assignments[row["actor"]] != row["lab"]:
            raise ImportFailure("El actor no corresponde al laboratorio asignado en el plan.")
        for field in ("categoria", "subcategoria"):
            if not isinstance(row.get(field), str) or not 0 < len(row[field].strip()) <= 100:
                raise ImportFailure("Categoría/subcategoría del plan inválida.")
        body = row.get("payload")
        if not isinstance(body, dict) or set(body) - set(EQUIPMENT_FIELDS):
            raise ImportFailure("El equipo contiene campos fuera del contrato.")
        payload = normalized_payload(plan, row)
        if payload["estado"] not in {"OPERATIVO", "MANTENIMIENTO", "INOPERATIVO"}:
            raise ImportFailure("Un equipo cargable requiere estado conocido; los estados vacíos deben quedar pendientes.")
        if type(payload["requiereMantenimiento"]) is not bool or payload["idResponsable"] is not None:
            raise ImportFailure("Mantenimiento/responsable fuera del plan autorizado.")
        for key, limit in LIMITS.items():
            value = payload[key]
            if value is not None and (not isinstance(value, str) or len(value) > limit):
                raise ImportFailure(f"Campo del equipo inválido: {key}.")
        if not payload["codigoInterno"] or not payload["nombre"]:
            raise ImportFailure("Código y nombre del equipo son obligatorios.")
        if payload["anio"] is not None and (type(payload["anio"]) is not int or not 1900 <= payload["anio"] <= 2100):
            raise ImportFailure("Año de equipo inválido.")
        if source_key(row) in seen_rows or payload["codigoInterno"] in seen_codes:
            raise ImportFailure("El plan repite fila o código interno.")
        seen_rows.add(source_key(row))
        seen_codes.add(payload["codigoInterno"])
    if {row["actor"] for row in equipment} != set(OWNERS):
        raise ImportFailure("Cada propietario necesita al menos un equipo con estado confirmado.")
    return plan


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


class Api:
    def __init__(self, opener=None):
        self.opener = opener or urllib.request.build_opener(urllib.request.ProxyHandler({}), NoRedirect())

    def request(self, method, path, token=None, body=None):
        if not path.startswith("/") or ".." in path or "?" in path or "#" in path:
            raise ImportFailure("Ruta API inválida.")
        headers = {"Accept": "application/json"}
        if token:
            headers["Authorization"] = "Bearer " + token
        data = None
        if body is not None:
            data = json.dumps(body, ensure_ascii=False).encode("utf-8")
            headers["Content-Type"] = "application/json"
        request = urllib.request.Request(BASE_URL + path, data=data, headers=headers, method=method)
        try:
            with self.opener.open(request, timeout=30) as response:
                raw = response.read(20 * 1024 * 1024 + 1)
                if len(raw) > 20 * 1024 * 1024:
                    raise ImportFailure("Respuesta API demasiado grande.")
                return json.loads(raw.decode("utf-8")) if raw else None
        except urllib.error.HTTPError as error:
            error.close()
            raise ApiFailure(method, path, error.code) from None
        except (urllib.error.URLError, TimeoutError, OSError):
            raise ApiFailure(method, path) from None
        except (UnicodeError, ValueError):
            raise ImportFailure(f"{method} {path}: respuesta JSON inválida; compruebe el manifiesto antes de reanudar.") from None


def checked_list(value, label):
    if not isinstance(value, list):
        raise ImportFailure(f"Listado API inválido: {label}.")
    ids = set()
    for row in value:
        if not isinstance(row, dict) or not positive_id(row.get("id")) or row["id"] in ids:
            raise ImportFailure(f"ID inválido o repetido en {label}.")
        ids.add(row["id"])
    return value


def indexed(rows, key, label):
    result = {}
    for row in rows:
        try:
            value = key(row)
            if value in result:
                raise ImportFailure(f"Clave ambigua en {label}; no se modificaron esos registros.")
            result[value] = row
        except (KeyError, TypeError, AttributeError):
            raise ImportFailure(f"Campos API inválidos en {label}.") from None
    return result


class Importer:
    def __init__(self, plan_path, backup, api=None, password_prompt=None):
        self.plan_path = Path(plan_path).resolve()
        self.plan = validate_plan(self.plan_path)
        self.folder = self.plan_path.parent
        self.backup = Path(backup).resolve()
        self.api = api or Api()
        self.prompt = password_prompt or getpass.getpass
        self.tokens, self.passwords, self.actor_ids = {}, {}, {}
        self.manifest_path = self.folder / "manifest.json"
        self.credential_path = self.folder / "credenciales-demo.json"
        self.state = None
        self.snapshot, self.audit = {}, []

    def save(self):
        atomic_json(self.manifest_path, self.state)

    def backup_hash(self):
        try:
            with self.backup.open("rb") as stream:
                header = stream.read(5)
            if self.backup.stat().st_size <= 5 or header != b"PGDMP":
                raise ImportFailure("El respaldo no es un archivo PostgreSQL custom válido.")
        except OSError:
            raise ImportFailure("Falta el respaldo local antes de aplicar.") from None
        sha = digest_file(self.backup)
        marker = load_json(str(self.backup) + ".verified.json")
        if (not isinstance(marker, dict) or marker.get("sha256") != sha
                or marker.get("format") != "PostgreSQL custom"
                or marker.get("verified_by") != "pg_restore --list"):
            raise ImportFailure("El respaldo carece de verificación pg_restore --list para su hash actual.")
        return sha

    def login(self, name, password, expected_role):
        response = self.api.request("POST", "/auth/login", body={"userName": name, "password": password})
        if not isinstance(response, dict) or not isinstance(response.get("accessToken"), str) or not response["accessToken"]:
            raise ImportFailure(f"Login inválido para {name}.")
        user = response.get("usuario")
        self.check_user(user, name, expected_role)
        token = response["accessToken"]
        me = self.api.request("GET", "/auth/me", token)
        self.check_user(me, name, expected_role)
        if me["id"] != user["id"]:
            raise ImportFailure("La identidad de login no coincide con el perfil vigente.")
        self.tokens[name] = token
        self.actor_ids[name] = user["id"]
        return user

    @staticmethod
    def check_user(user, name, role):
        if (not isinstance(user, dict) or not positive_id(user.get("id"))
                or user.get("userName", "").casefold() != name or user.get("rol") != role
                or user.get("activo") is not True):
            raise ImportFailure(f"La cuenta {name} no está activa con el rol esperado {role}.")

    def get(self, path, actor="marko"):
        return self.api.request("GET", path, self.tokens[actor])

    def refresh_snapshot(self):
        for resource in ("usuarios", "categorias", "subcategorias", "sedes", "areas", "laboratorios", "equipos"):
            self.snapshot[resource] = checked_list(self.get("/admin/" + resource), resource)
        self.audit = checked_list(self.get("/admin/auditoria"), "auditoria")
        for event in self.audit:
            if (not isinstance(event.get("userNameActor"), str) or not isinstance(event.get("entidad"), str)
                    or not positive_id(event.get("idEntidad")) or not isinstance(event.get("accion"), str)):
                raise ImportFailure("Evento de auditoría inválido.")

    def prepare_state(self, backup_sha):
        plan_sha = digest_json(self.plan)
        if self.manifest_path.exists():
            self.state = load_json(self.manifest_path)
            if (not isinstance(self.state, dict) or self.state.get("version") != 1
                    or self.state.get("source_sha256") != self.plan["source_sha256"]
                    or self.state.get("plan_sha256") != plan_sha
                    or self.state.get("backup_sha256") != backup_sha):
                raise ImportFailure("El manifiesto no coincide con plan y respaldo; no se reanudó.")
            for key in ("intents", "equipment", "actor_overrides"):
                if not isinstance(self.state.get(key), dict):
                    raise ImportFailure("Manifiesto de reanudación inválido.")
            if not isinstance(self.state.get("created"), list) or not isinstance(self.state.get("audit_baseline_id"), int):
                raise ImportFailure("Manifiesto de reanudación inválido.")
        else:
            self.state = {"version": 1, "source_sha256": self.plan["source_sha256"], "plan_sha256": plan_sha,
                          "backup_sha256": backup_sha,
                          "started_at": dt.datetime.now(dt.timezone.utc).isoformat(),
                          "audit_baseline_id": max((event["id"] for event in self.audit), default=0),
                          "snapshot_ids": {key: [row["id"] for row in value] for key, value in self.snapshot.items()},
                          "assignments": self.plan["assignments"], "intents": {}, "created": [],
                          "equipment": {}, "actor_overrides": {}, "pending_source_count": len(self.plan["pending"]),
                          "completed": False}
        self.validate_field_resolutions()
        self.save()

    def validate_field_resolutions(self):
        """Solo admite omitir una serie conflictiva mediante una decisión explícita.

        El plan y su hash no cambian; el manifiesto conserva la referencia original.
        Se valida antes de crear usuarios, catálogos, asignaciones o equipos.
        """
        resolutions = self.state.get("field_resolutions", {})
        if not isinstance(resolutions, dict):
            raise ImportFailure("Resoluciones de campos del manifiesto inválidas.")
        rows = {source_key(row): row for row in self.plan["equipment"]}
        fields = {"field", "original_value", "value", "conflicting_equipment_id", "reason"}
        for location, resolution in resolutions.items():
            if location not in rows or not isinstance(resolution, dict) or set(resolution) != fields:
                raise ImportFailure("Fila o campos de resolución fuera del plan autorizado.")
            original = normalized_payload(self.plan, rows[location])["numeroSerie"]
            reason = resolution.get("reason")
            if (resolution.get("field") != "numeroSerie" or resolution.get("value") is not None
                    or not isinstance(original, str) or not original
                    or resolution.get("original_value") != original
                    or not positive_id(resolution.get("conflicting_equipment_id"))
                    or not isinstance(reason, str) or not reason.strip() or len(reason) > 300):
                raise ImportFailure("Resolución de serie inválida; no se modificó el plan ni se inició la carga.")

    def credentials(self):
        stored = {"version": 1, "source_sha256": self.plan["source_sha256"], "users": {}}
        if self.credential_path.exists():
            stored = load_json(self.credential_path)
            if (not isinstance(stored, dict) or stored.get("version") != 1
                    or stored.get("source_sha256") != self.plan["source_sha256"]
                    or not isinstance(stored.get("users"), dict)):
                raise ImportFailure("Archivo privado de credenciales incompatible.")
            for name in DEMO_ROLES:
                entry = stored["users"].get(name)
                if entry is None:
                    continue
                password = entry.get("password") if isinstance(entry, dict) else None
                if not isinstance(password, str) or not 4 <= len(password.encode("utf-8")) <= 72:
                    raise ImportFailure("Archivo privado de credenciales incompleto.")
                self.passwords[name] = password
        existing = {user["userName"].casefold() for user in self.snapshot["usuarios"]}
        for name in DEMO_ROLES:
            if name not in existing and name not in self.passwords:
                self.passwords[name] = secrets.token_urlsafe(24)
                stored["users"][name] = {"password": self.passwords[name]}
        # Se escribe y sincroniza ANTES de crear cualquier cuenta con esas claves.
        atomic_json(self.credential_path, stored)

    def write(self, entity, key, path, body, actor, method="POST"):
        # El intento se conserva ANTES del HTTP: un timeout puede ocurrir después del commit.
        self.state["intents"][key] = {"entity": entity, "actor": actor, "path": path, "method": method}
        self.save()
        response = self.api.request(method, path, self.tokens[actor], body)
        if not isinstance(response, dict) or not positive_id(response.get("id")):
            raise ImportFailure(f"Respuesta del alta {entity} inválida; reanude para comprobar el resultado.")
        self.record_creation(entity, key, response["id"], actor)
        return response

    def record_creation(self, entity, key, identifier, actor):
        record = {"entity": entity, "key": key, "id": identifier, "actor": actor}
        existing = next((item for item in self.state["created"] if item["entity"] == entity and item["id"] == identifier), None)
        if existing and existing != record:
            raise ImportFailure("La identidad del alta no coincide con el manifiesto.")
        if not existing:
            self.state["created"].append(record)
        self.state["intents"].pop(key, None)
        self.save()

    def has_event(self, entity, identifier, actor):
        return any(event["id"] > self.state["audit_baseline_id"] and event["entidad"] == entity
                   and event["idEntidad"] == identifier and event["accion"] == "CREAR"
                   and event["userNameActor"].casefold() == actor
                   and event.get("idUsuarioActor") == self.actor_ids.get(actor) for event in self.audit)

    def recover(self, entity, key, identifier, actor):
        intent = self.state["intents"].get(key)
        if intent:
            if intent["entity"] != entity or intent["actor"] != actor or not self.has_event(entity, identifier, actor):
                raise ImportFailure("No se pudo acreditar la autoría de un alta con respuesta desconocida.")
            self.record_creation(entity, key, identifier, actor)

    @staticmethod
    def active(record, label):
        if record.get("activo") is not True:
            raise ImportFailure(f"Existe un registro inactivo para {label}; no se reactivó automáticamente.")
        return record

    def ensure_users(self):
        users = indexed(self.snapshot["usuarios"], lambda row: row["userName"].casefold(), "usuarios")
        for demo in self.plan["users"]:
            name = demo["userName"]
            row = users.get(name)
            if row:
                self.check_user(row, name, demo["rol"])
                if row.get("email", "").casefold() != demo["email"]:
                    raise ImportFailure(f"La cuenta {name} pertenece a otra identidad; no se modificó.")
                self.recover("usuario", "usuario:" + name, row["id"], "marko")
            else:
                row = self.write("usuario", "usuario:" + name, "/admin/usuarios",
                                 dict(demo, password=self.passwords[name]), "marko")
                self.snapshot["usuarios"].append(row)
                users[name] = row
            password = self.passwords.get(name)
            if password is None:
                password = self.prompt(f"Contraseña actual de {name} (oculta): ")
            try:
                self.login(name, password, demo["rol"])
            except ApiFailure as error:
                if error.status != 401:
                    raise
                # Una cuenta demo preexistente puede tener una clave distinta; nunca la restablece.
                password = self.prompt(f"Contraseña actual de {name} (oculta): ")
                self.login(name, password, demo["rol"])
                password = None
        return users

    def indexes(self):
        categories = indexed(self.snapshot["categorias"], lambda row: row["nombre"].casefold(), "categorías")
        subcategories = indexed(self.snapshot["subcategorias"],
                                lambda row: (row["categoria"]["id"], row["nombre"].casefold()), "subcategorías")
        labs = indexed(self.snapshot["laboratorios"], lambda row: row["codigo"].casefold(), "laboratorios")
        equipment = indexed(self.snapshot["equipos"], lambda row: row["codigoInterno"], "equipos")
        for resource, rows in self.snapshot.items():
            if resource != "equipos":
                for row in rows:
                    if type(row.get("activo")) is not bool:
                        raise ImportFailure(f"Estado activo inválido en {resource}.")
        return categories, subcategories, labs, equipment

    def resolved_body(self, row, categories, subcategories, labs):
        cat = categories.get(row["categoria"].casefold())
        lab = labs.get(row["lab"].casefold())
        sub = subcategories.get((cat["id"], row["subcategoria"].casefold())) if cat else None
        if not cat or not lab or not sub:
            return None
        payload = normalized_payload(self.plan, row)
        resolution = (self.state or {}).get("field_resolutions", {}).get(source_key(row))
        if resolution:
            payload["numeroSerie"] = None
            note = ("Resolución de importación: numeroSerie original="
                    + json.dumps(resolution["original_value"], ensure_ascii=False)
                    + f"; registro previo ID={resolution['conflicting_equipment_id']}; "
                    + resolution["reason"].strip())
            payload["comentario"] = (payload["comentario"] + "\n" + note).strip()
        return dict(payload, idLaboratorio=lab["id"], idSubcategoria=sub["id"])

    @staticmethod
    def matches_equipment(existing, expected):
        if expected is None:
            return False
        for key in EQUIPMENT_FIELDS:
            current = (existing.get("responsable") or {}).get("id") if key == "idResponsable" else existing.get(key)
            if current != expected[key]:
                return False
        return ((existing.get("laboratorio") or {}).get("id") == expected["idLaboratorio"]
                and (existing.get("subcategoria") or {}).get("id") == expected["idSubcategoria"])

    def preflight(self):
        categories, subcategories, labs, equipment = self.indexes()
        users = indexed(self.snapshot["usuarios"], lambda row: row["userName"].casefold(), "usuarios")
        indexed(self.snapshot["usuarios"], lambda row: row["email"].casefold(), "emails de usuarios")
        for demo in self.plan["users"]:
            existing = users.get(demo["userName"])
            if existing:
                self.check_user(existing, demo["userName"], demo["rol"])
                if existing.get("email", "").casefold() != demo["email"]:
                    raise ImportFailure("Una cuenta demo existente pertenece a otra identidad; no se modificó.")
            elif any(row["email"].casefold() == demo["email"] for row in users.values()):
                raise ImportFailure("Un email demo ya pertenece a otra cuenta; no se modificó.")
        for name in LABS:
            if name.casefold() in labs:
                self.active(labs[name.casefold()], name)
        for row in self.plan["equipment"]:
            cat = categories.get(row["categoria"].casefold())
            if cat:
                self.active(cat, row["categoria"])
                sub = subcategories.get((cat["id"], row["subcategoria"].casefold()))
                if sub:
                    self.active(sub, row["subcategoria"])
            existing = equipment.get(row["payload"]["codigoInterno"].strip())
            if existing and not self.matches_equipment(existing, self.resolved_body(row, categories, subcategories, labs)):
                raise ImportFailure(f"Código existente incompatible en {source_key(row)}; no se editó ni se inició la carga de equipos.")

    def ensure_organization(self):
        labs = indexed(self.snapshot["laboratorios"], lambda row: row["codigo"].casefold(), "laboratorios")
        for lab in labs.values():
            self.recover("laboratorio", "laboratorio:" + lab["codigo"].upper(), lab["id"], "marko")
        missing = sorted(LABS - {row["codigo"].upper() for row in labs.values()})
        if not missing:
            return
        sede_name, area_name = "Demostracion inventario Excel", "Laboratorios inventario Excel"
        sedes = indexed(self.snapshot["sedes"], lambda row: row["nombre"].casefold(), "sedes")
        sede = sedes.get(sede_name.casefold())
        if sede:
            self.active(sede, sede_name)
            self.recover("sede", "sede:demo", sede["id"], "marko")
        else:
            sede = self.write("sede", "sede:demo", "/sedes", {"nombre": sede_name}, "marko")
            self.snapshot["sedes"].append(sede)
        areas = indexed(self.snapshot["areas"], lambda row: (row["sede"]["id"], row["nombre"].casefold()), "áreas")
        area = areas.get((sede["id"], area_name.casefold()))
        if area:
            self.active(area, area_name)
            self.recover("area", "area:demo", area["id"], "marko")
        else:
            area = self.write("area", "area:demo", "/areas", {"nombre": area_name, "idSede": sede["id"]}, "marko")
            self.snapshot["areas"].append(area)
        for code in missing:
            lab = self.write("laboratorio", "laboratorio:" + code, "/laboratorios",
                             {"nombre": "Laboratorio " + code, "codigo": code, "idArea": area["id"],
                              "estadoOperativo": "OPERATIVO"}, "marko")
            self.snapshot["laboratorios"].append(lab)
        # Recupera también altas que se confirmaron en servidor justo antes de perder conexión.
        for lab in self.snapshot["laboratorios"]:
            self.recover("laboratorio", "laboratorio:" + lab["codigo"].upper(), lab["id"], "marko")

    def ensure_catalogs(self):
        categories, subcategories, _, _ = self.indexes()
        for row in self.plan["equipment"]:
            cat_name, sub_name = row["categoria"].strip(), row["subcategoria"].strip()
            cat_key = "categoria:" + cat_name.casefold()
            cat = categories.get(cat_name.casefold())
            if not cat:
                cat = self.write("categoria", cat_key, "/categorias", {"nombre": cat_name,
                                  "descripcion": "Catálogo de la importación de demostración Excel."}, "gabriela")
                categories[cat_name.casefold()] = cat
                self.snapshot["categorias"].append(cat)
            else:
                self.active(cat, cat_name)
                self.recover("categoria", cat_key, cat["id"], "gabriela")
            sub_key = f"subcategoria:{cat['id']}:{sub_name.casefold()}"
            sub = subcategories.get((cat["id"], sub_name.casefold()))
            if not sub:
                sub = self.write("subcategoria", sub_key, "/subcategorias", {"nombre": sub_name,
                                  "idCategoria": cat["id"], "descripcion": "Clasificación del inventario Excel de demostración."}, "gabriela")
                subcategories[(cat["id"], sub_name.casefold())] = sub
                self.snapshot["subcategorias"].append(sub)
            else:
                self.active(sub, sub_name)
                self.recover("subcategoria", sub_key, sub["id"], "gabriela")

    def ensure_assignments(self, users):
        labs = indexed(self.snapshot["laboratorios"], lambda row: row["codigo"].upper(), "laboratorios")
        for name in OWNERS:
            path = f"/admin/usuarios/{users[name]['id']}/laboratorios"
            response = self.get(path)
            current = self.assignment_ids(response, users[name]["id"])
            self.state.setdefault("assignments_before", {}).setdefault(name, sorted(current))
            self.save()
            wanted = {labs[self.plan["assignments"][name]]["id"]}
            if current != wanted:
                self.state["intents"]["asignacion:" + name] = {"entity": "usuario_laboratorio", "actor": "marko",
                                                               "method": "PUT", "path": path}
                self.save()
                response = self.api.request("PUT", path, self.tokens["marko"], {"idsLaboratorio": sorted(wanted)})
                if self.assignment_ids(response, users[name]["id"]) != wanted:
                    raise ImportFailure("No se confirmó el conjunto de asignaciones.")
            self.state["intents"].pop("asignacion:" + name, None)
            self.save()

    @staticmethod
    def assignment_ids(response, user_id):
        if not isinstance(response, dict) or (response.get("usuario") or {}).get("id") != user_id:
            raise ImportFailure("Respuesta de asignaciones inválida.")
        return {row["id"] for row in checked_list(response.get("laboratorios"), "asignaciones")}

    def load_equipment(self):
        categories, subcategories, labs, equipment = self.indexes()
        serial_indexes = {field: indexed([item for item in equipment.values() if item.get(field)],
                                         lambda item, field=field: item[field], field)
                          for field in ("serieUtec", "numeroSerie")}
        candidates = []
        for row in self.plan["equipment"]:
            key = source_key(row)
            body = self.resolved_body(row, categories, subcategories, labs)
            if body is None:
                raise ImportFailure("No se pudieron resolver catálogos y laboratorio del equipo.")
            existing = equipment.get(body["codigoInterno"])
            actor = self.state["actor_overrides"].get(key, row["actor"])
            if existing:
                if not self.matches_equipment(existing, body):
                    raise ImportFailure(f"El equipo de {key} cambió y requiere revisión; no se sobrescribió.")
                self.recover("equipo", "equipo:" + key, existing["id"], actor)
                self.state["equipment"][key] = {"status": "coincidente", "id": existing["id"], "actor": actor,
                                                "payload_sha256": digest_json(body)}
                self.save()
                continue
            conflict = next((field for field, items in serial_indexes.items() if body.get(field) in items), None)
            if conflict:
                self.state["equipment"][key] = {"status": "pendiente", "reason": "Serie ya utilizada: " + conflict}
                self.save()
                continue
            candidates.append((row, body))
            # Reserva series de los candidatos para detectar duplicados también dentro del plan.
            for field, items in serial_indexes.items():
                if body.get(field):
                    items[body[field]] = {"id": None}
        gabriela_created = any(item["actor"] == "gabriela" for item in self.state["created"])
        if not gabriela_created and not self.state["actor_overrides"]:
            counts = {name: sum(row["actor"] == name for row, _ in candidates) for name in OWNERS}
            eligible = next((row for row, _ in candidates if counts[row["actor"]] >= 2), None)
            if not eligible:
                raise ImportFailure("No hay un alta adicional disponible para Gabriela sin quitar la participación de otro usuario.")
            self.state["actor_overrides"][source_key(eligible)] = "gabriela"
            self.save()
        for row, body in candidates:
            key = source_key(row)
            actor = self.state["actor_overrides"].get(key, row["actor"])
            result = self.write("equipo", "equipo:" + key, "/equipos", body, actor)
            if not self.matches_equipment(result, body):
                raise ImportFailure("El equipo guardado difiere del plan; se detuvo la carga para revisión.")
            self.state["equipment"][key] = {"status": "creado", "id": result["id"], "actor": actor,
                                            "payload_sha256": digest_json(body)}
            self.save()

    def verify(self):
        self.audit = checked_list(self.get("/admin/auditoria"), "auditoria")
        for item in self.state["created"]:
            if not self.has_event(item["entity"], item["id"], item["actor"]):
                raise ImportFailure("Falta la auditoría transaccional de un alta; revise el manifiesto.")
        actors = {item["actor"] for item in self.state["created"]}
        if not set(OWNERS + ("gabriela",)).issubset(actors):
            raise ImportFailure("No se acreditaron altas nuevas de los cinco usuarios; no se inventaron eventos.")
        for name in OWNERS:
            if not any(item["entity"] == "equipo" and item["actor"] == name for item in self.state["created"]):
                raise ImportFailure(f"No se acreditó un equipo nuevo creado por {name}.")
        for row in self.plan["equipment"]:
            entry = self.state["equipment"].get(source_key(row))
            if not entry or entry.get("status") == "pendiente":
                continue
            expected = self.resolved_body(row, *self.indexes()[:3])
            actual = self.get(f"/equipos/{entry['id']}")
            if not self.matches_equipment(actual, expected):
                raise ImportFailure("Falló la comprobación final de un equipo.")
        self.state["completed"] = True
        self.state["verified_at"] = dt.datetime.now(dt.timezone.utc).isoformat()
        self.save()
        return {"created": len(self.state["created"]), "equipment": len(self.state["equipment"]),
                "pending": len(self.plan["pending"]) + sum(item.get("status") == "pendiente"
                                                            for item in self.state["equipment"].values()),
                "actors_verified": sorted(actors)}

    def run(self):
        try:
            backup_sha = self.backup_hash()
            for name, role in (("marko", "ADMIN"), ("aldo", "GESTOR")):
                password = self.prompt(f"Contraseña actual de {name} (oculta): ")
                self.login(name, password, role)
                password = None
            self.refresh_snapshot()
            self.prepare_state(backup_sha)
            self.preflight()
            self.credentials()
            users = self.ensure_users()
            self.ensure_organization()
            self.ensure_catalogs()
            self.ensure_assignments(users)
            self.load_equipment()
            return self.verify()
        finally:
            self.tokens.clear()
            self.passwords.clear()


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--plan", required=True, type=Path)
    parser.add_argument("--backup", type=Path, help="Respaldo custom con archivo vecino .verified.json validado por el wrapper.")
    parser.add_argument("--aplicar", action="store_true", help="Realiza escrituras locales; sin este flag no hay HTTP.")
    args = parser.parse_args(argv)
    try:
        plan = validate_plan(args.plan)
        if not args.aplicar:
            print(f"Plan válido: {len(plan['equipment'])} equipos cargables, {len(plan['pending'])} pendientes. Sin HTTP ni cambios.")
            return 0
        if not args.backup:
            raise ImportFailure("--aplicar exige --backup verificado.")
        if not sys.stdin.isatty():
            raise ImportFailure("La aplicación requiere una terminal interactiva para introducir contraseñas ocultas.")
        result = Importer(args.plan, args.backup).run()
        print(f"Verificación terminada: {result['equipment']} filas resueltas; {result['pending']} pendientes.")
        print("Cinco actores verificados mediante eventos CREAR reales.")
        print(f"Credenciales demo nuevas: {args.plan.resolve().parent / 'credenciales-demo.json'}")
        print("Las altas se confirman por operación; la importación completa no es una única transacción.")
        return 0
    except ImportFailure as error:
        print(f"Carga detenida: {error}", file=sys.stderr)
        print("Conserve plan, respaldo y manifiesto; reanude con los mismos archivos después de revisar el motivo.", file=sys.stderr)
        return 1
    except (KeyboardInterrupt, EOFError):
        print("Carga interrumpida. Se conserva el checkpoint para comprobar las operaciones al reanudar.", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
