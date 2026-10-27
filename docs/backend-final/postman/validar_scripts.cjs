/* Sandbox local: sin require/fetch/red en los scripts Postman. No llama API. */
const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const input = JSON.parse(fs.readFileSync(0, 'utf8'));
const items = input.collection.item.flatMap(group => group.item);
const globalPre = input.collection.event[0].script.exec.join('\n');
let compiled = 0;
for (const code of [globalPre, ...items.flatMap(item => item.event.map(event => event.script.exec.join('\n')))]) {
  new vm.Script(code);
  compiled++;
}
function scripts(item, kind) {
  return item.event.filter(event => event.listen === kind).map(event => event.script.exec.join('\n'));
}
function fixture(item) {
  const code = scripts(item, 'test').join('\n');
  const status = Number(code.match(/HTTP esperado (\d+)/)[1]);
  const fields = [...code.matchAll(/pm\.expect\((?:data|item)\)\.to\.have\.property\('([^']+)'\)/g)].map(match => match[1]);
  const data = Object.fromEntries(fields.map(field => [field,
    field === 'id' || field.startsWith('id') ? 1 :
    field.startsWith('total') ? 0 :
    ['activo','alcanceGlobal'].includes(field) ? true :
    field === 'requiereMantenimiento' ? false :
    ['laboratorios','equiposPorEstado','mantenimientosPorEstado','porTipo','porEstado'].includes(field) ? [] :
    field.startsWith('fecha') ? '2026-10-27T00:00:00Z' : 'sintetico'
  ]));
  if (item.request.url.raw.endsWith('/auth/login')) {
    const role = item.name.match(/(ADMIN|GESTOR|LECTOR)/)[1];
    const suffix = role[0] + role.slice(1).toLowerCase();
    Object.assign(data, {accessToken:'token-sintetico', tokenType:'Bearer', expiresIn:1800,
      usuario:{id:1,userName:'verificacion_'+suffix,nombre:'Prueba',apellido:'API',email:'prueba@example.invalid',rol:role,activo:true,fechaCreacion:'2026-10-27T00:00:00Z'}});
  }
  if (status >= 400) data.status = status;
  return {status, data: code.includes("pm.expect(data).to.be.an('array')") ? [data] : data};
}
function sandbox(item, response = fixture(item), overrides = {}) {
  const env = new Map(input.environment.values.map(item => [item.key, String(item.value)]));
  for (const key of env.keys()) {
    if (/^id/.test(key)) env.set(key,'1');
    if (/^token/.test(key)) env.set(key,'token-sintetico');
    if (/^password/.test(key)) env.set(key, String.fromCharCode(97).repeat(15));
  }
  for (const suffix of ['Admin','Gestor','Lector']) env.set('userName'+suffix,'verificacion_'+suffix);
  env.set('habilitarEscrituras','true'); env.set('sufijo','verificacion');
  env.set('nuevoUserName','cuenta_verificacion'); env.set('nuevoEmail','prueba@example.invalid');
  env.set('fechaProgramadaMantenimiento','2026-10-27');
  for (const [key,value] of Object.entries(overrides)) env.set(key,value);
  const local = new Map();
  const failures = [];
  const resolve = text => text.replace(/\{\{(\w+)\}\}/g, (whole,key) => local.has(key) ? local.get(key) : env.has(key) ? env.get(key) : whole);
  function expect(value) {
    const chain = {
      negate:false,
      get to(){return this;}, get be(){return this;}, get have(){return this;},
      get and(){return this;}, get at(){return this;}, get not(){this.negate=!this.negate;return this;},
      a(type){ return this.an(type); },
      an(type){assert.equal(type === 'array' ? Array.isArray(value) : typeof value === type,true);return this;},
      property(key){assert.equal(Object.prototype.hasOwnProperty.call(value,key),true);return this;},
      status(code){assert.equal(response.status,code);return this;},
      eql(other){assert.equal(JSON.stringify(value),JSON.stringify(other));return this;},
      match(regex){assert.equal(regex.test(value),true);return this;},
      above(other){assert.equal(value>other,true);return this;},
      least(other){assert.equal(value>=other,true);return this;},
      get empty(){const empty=value==null || value.length===0;assert.equal(this.negate?!empty:empty,true);return this;},
    };
    return chain;
  }
  const pm = {
    variables:{get:key=>local.has(key)?local.get(key):env.get(key),set:(key,value)=>local.set(key,value),replaceIn:resolve},
    environment:{get:key=>env.get(key),set:(key,value)=>env.set(key,value),unset:key=>env.delete(key)},
    request:{method:item.request.method,body:item.request.body,
      url:{toString:()=>item.request.url.raw,query:{each:fn=>(item.request.url.query||[]).forEach(fn)}}},
    response:{code:response.status,
      headers:{get:key=>key.toLowerCase()==='location'?'http://127.0.0.1:1/api/recurso/1':'application/json'},
      json:()=>{if(response.status===204)throw new Error('Sin JSON');return response.data;},
      text:()=>response.status===204?'':JSON.stringify(response.data),
      to:{have:{status:code=>assert.equal(response.status,code)}}},
    test:(name,fn)=>{try{fn();}catch(_){failures.push(name);}},expect,
  };
  const context = vm.createContext({pm});
  return {env,local,failures,resolve,
    pre(){vm.runInContext(globalPre,context,{timeout:1000});for(const code of scripts(item,'prerequest'))vm.runInContext(code,context,{timeout:1000});},
    test(){for(const code of scripts(item,'test'))vm.runInContext(code,context,{timeout:1000});}};
}
let executed=0, guarded=0, rejectedLogins=0, passwordCases=0;
for (const item of items) {
  const run=sandbox(item);run.pre();
  if(item.request.body) JSON.parse(run.resolve(item.request.body.raw));
  run.test();assert.deepEqual(run.failures,[]);executed++;
  if(item.request.method!=='GET' && !item.request.url.raw.endsWith('/auth/login')){
    assert.throws(()=>sandbox(item,undefined,{habilitarEscrituras:'false'}).pre());guarded++;
  }
}
for (const item of items.filter(item=>item.request.url.raw.endsWith('/auth/login'))) {
  const suffix=item.name.match(/(ADMIN|GESTOR|LECTOR)/)[1].toLowerCase();
  const roleKey=suffix[0].toUpperCase()+suffix.slice(1);
  assert.throws(()=>sandbox(item,undefined,{['password'+roleKey]:''}).pre());
  for (const mutate of [
    data=>{data.accessToken='';}, data=>{data.tokenType='Basic';}, data=>{data.expiresIn=0;},
    data=>{data.usuario.id=0;}, data=>{data.usuario.rol='OTRO';},data=>{data.usuario.activo=false;},
    data=>{data.usuario.userName='otra_identidad';}, data=>{data.usuario.passwordHash='campo-no-admitido';},
  ]) {
    const response=fixture(item);mutate(response.data);
    const run=sandbox(item,response);run.pre();run.test();
    assert.equal(run.env.has('token'+roleKey),false);assert.equal(run.env.has('idUsuario'+roleKey),false);
    assert.equal(run.failures.length>0,true);rejectedLogins++;
  }
}
const reset=items.find(item=>item.request.url.raw.endsWith('/password'));
for (const [value,valid] of [
  [String.fromCharCode(97).repeat(14),false],[String.fromCharCode(97).repeat(15),true],
  ['\u{1F600}'.repeat(18),true],['\u{1F600}'.repeat(19),false],['\u001C'.repeat(15),false],
  ['\u00A0'.repeat(15),false],['\u2003'.repeat(15),false],[' '+String.fromCharCode(97).repeat(13)+' ',true],
]) {
  const run=sandbox(reset,undefined,{passwordRestablecimiento:value});
  if(valid)run.pre();else assert.throws(()=>run.pre());passwordCases++;
}
process.stdout.write(JSON.stringify({scripts_compilados:compiled,solicitudes_simuladas:executed,
  escrituras_bloqueadas:guarded,logins_invalidos_sin_token:rejectedLogins,
  casos_password_unicode:passwordCases,http_real:false}));
