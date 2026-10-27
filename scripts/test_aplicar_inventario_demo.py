"""Integración HTTP simulada: no usa el backend ni PostgreSQL del usuario."""
import contextlib
import copy
import hashlib
import http.server
import io
import json
import os
from pathlib import Path
import socket
import tempfile
import threading
import unittest
from unittest import mock
import urllib.parse
import urllib.request

import aplicar_inventario_demo as app


class FakeState:
    def __init__(self):
        self.data = {name: [] for name in ("usuarios", "categorias", "subcategorias", "sedes", "areas", "laboratorios", "equipos")}
        self.data["usuarios"] = [self.user(1, "marko", "ADMIN"), self.user(2, "aldo", "GESTOR")]
        self.passwords = {"marko": "MARKO_PRIVATE_SECRET", "aldo": "ALDO_PRIVATE_SECRET"}
        self.assignments = {1: {99}, 2: {98}}
        self.audit = []
        self.requests = []
        self.block_path = None
        self.fail_committed_code = None
        self.fail_lab_code = None
        self.credential_path = None
        self.passwords_written_before_create = []

    @staticmethod
    def user(identifier, name, role):
        return {"id": identifier, "userName": name, "nombre": name.title(), "apellido": "Demo",
                "email": name + "@inventario.test", "cargo": "Cuenta de demostracion", "rol": role, "activo": True}

    def actor(self, token):
        return next((user for user in self.data["usuarios"] if token == "TOKEN_PRIVATE_" + user["userName"]), None)

    def event(self, entity, identifier, actor, action="CREAR"):
        self.audit.append({"id": len(self.audit) + 1, "idUsuarioActor": actor["id"],
                           "userNameActor": actor["userName"], "accion": action, "entidad": entity,
                           "idEntidad": identifier, "cambios": "Operación simulada", "fecha": "2026-10-26T20:00:00Z"})

    def mutations(self):
        return [item for item in self.requests if item[0] != "GET" and item[1] != "/auth/login"]


class FakeHandler(http.server.BaseHTTPRequestHandler):
    def log_message(self, *_args):
        pass

    def do_GET(self):
        self.handle_api("GET")

    def do_POST(self):
        self.handle_api("POST")

    def do_PUT(self):
        self.handle_api("PUT")

    def respond(self, status, value):
        encoded = json.dumps(value).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(encoded)))
        self.end_headers()
        self.wfile.write(encoded)

    def disconnect(self):
        self.connection.shutdown(socket.SHUT_RDWR)
        self.connection.close()

    def handle_api(self, method):
        state = self.server.state
        path = urllib.parse.urlsplit(self.path).path.removeprefix("/api")
        # Los cuerpos privados nunca se incorporan al registro de peticiones.
        state.requests.append((method, path))
        size = int(self.headers.get("Content-Length", 0))
        body = json.loads(self.rfile.read(size)) if size else None
        if path == "/auth/login":
            user = next((row for row in state.data["usuarios"] if row["userName"] == body["userName"]), None)
            if not user or state.passwords.get(body["userName"]) != body["password"]:
                return self.respond(401, {"message": "Error simulado"})
            return self.respond(200, {"accessToken": "TOKEN_PRIVATE_" + user["userName"], "tokenType": "Bearer",
                                     "expiresIn": 1800, "usuario": user})
        actor = state.actor(self.headers.get("Authorization", "").removeprefix("Bearer "))
        if not actor:
            return self.respond(401, {})
        if state.block_path == path:
            return self.respond(403, {"message": "Error simulado"})
        if path == "/auth/me":
            return self.respond(200, actor)
        if path == "/admin/auditoria":
            return self.respond(200, state.audit)
        pieces = path.strip("/").split("/")
        if pieces[:2] == ["admin", "usuarios"] and len(pieces) == 4 and pieces[3] == "laboratorios":
            user_id = int(pieces[2])
            user = next(row for row in state.data["usuarios"] if row["id"] == user_id)
            if method == "PUT":
                state.assignments[user_id] = set(body["idsLaboratorio"])
                state.event("usuario_laboratorio", user_id, actor, "ASIGNAR_LABORATORIOS")
            labs = [{"id": identifier, "codigo": f"L{identifier}", "nombre": "Laboratorio simulado"}
                    for identifier in sorted(state.assignments.get(user_id, set()))]
            return self.respond(200, {"usuario": {key: user[key] for key in ("id", "userName", "nombre", "apellido", "rol")},
                                      "laboratorios": labs})
        if method == "GET" and len(pieces) == 2 and pieces[0] == "admin" and pieces[1] in state.data:
            return self.respond(200, state.data[pieces[1]])
        if method == "GET" and pieces[0] == "equipos" and len(pieces) == 2:
            return self.respond(200, next(row for row in state.data["equipos"] if row["id"] == int(pieces[1])))
        if method != "POST":
            return self.respond(404, {})
        resource = "usuarios" if path == "/admin/usuarios" else path.strip("/")
        if resource not in state.data:
            return self.respond(404, {})
        rows = state.data[resource]
        identifier = max((row["id"] for row in rows), default=0) + 1
        response = copy.deepcopy(body)
        response["id"] = identifier
        if resource != "equipos":
            response["activo"] = True
        if resource == "usuarios":
            stored = json.loads(state.credential_path.read_text(encoding="utf-8"))
            state.passwords_written_before_create.append(stored["users"][body["userName"]]["password"] == body["password"])
            state.passwords[body["userName"]] = body["password"]
            response.pop("password")
        elif resource == "areas":
            response["sede"] = {"id": response.pop("idSede"), "nombre": "Demostracion inventario Excel"}
        elif resource == "laboratorios":
            response["area"] = {"id": response.pop("idArea"), "nombre": "Laboratorios inventario Excel"}
        elif resource == "subcategorias":
            response["categoria"] = {"id": response.pop("idCategoria"), "nombre": "Instrumentación"}
        elif resource == "equipos":
            if actor["rol"] == "GESTOR" and body["idLaboratorio"] not in state.assignments.get(actor["id"], set()):
                return self.respond(403, {})
            lab_id, sub_id = response.pop("idLaboratorio"), response.pop("idSubcategoria")
            lab = next(row for row in state.data["laboratorios"] if row["id"] == lab_id)
            sub = next(row for row in state.data["subcategorias"] if row["id"] == sub_id)
            response["laboratorio"] = {key: lab[key] for key in ("id", "codigo", "nombre")}
            response["subcategoria"] = {key: sub[key] for key in ("id", "nombre")}
            response["responsable"] = None
            response.pop("idResponsable")
        rows.append(response)
        entity = {"usuarios": "usuario", "categorias": "categoria", "subcategorias": "subcategoria", "sedes": "sede",
                  "areas": "area", "laboratorios": "laboratorio", "equipos": "equipo"}[resource]
        state.event(entity, identifier, actor)
        if resource == "equipos" and body["codigoInterno"] == state.fail_committed_code:
            state.fail_committed_code = None
            return self.disconnect()
        if resource == "laboratorios" and body["codigo"] == state.fail_lab_code:
            state.fail_lab_code = None
            return self.disconnect()
        self.respond(201, response)


class MappedOpener:
    """Desvía únicamente el destino fijo del cliente a nuestro servidor efímero."""
    def __init__(self, server):
        self.local_base = f"http://127.0.0.1:{server.server_port}/api"
        self.opener = urllib.request.build_opener(urllib.request.ProxyHandler({}), app.NoRedirect())

    def open(self, request, timeout):
        if not request.full_url.startswith(app.BASE_URL + "/"):
            raise AssertionError("Destino inesperado")
        redirected = urllib.request.Request(self.local_base + request.full_url[len(app.BASE_URL):],
                                            data=request.data, headers=dict(request.header_items()), method=request.method)
        return self.opener.open(redirected, timeout=timeout)


class ImporterTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.environment = mock.patch.dict(os.environ, {"LOCALAPPDATA": self.temporary.name})
        self.environment.start()
        self.folder = Path(self.temporary.name) / "inventario-laboratorios" / "importacion-demo" / ("a" * 12)
        self.folder.mkdir(parents=True)
        self.path = self.folder / "plan.json"
        self.backup = self.folder / "respaldo.dump"
        self.backup.write_bytes(b"PGDMP-SIMULADO-NO-ES-UNA-BASE-REAL")
        app.atomic_json(str(self.backup) + ".verified.json", {"sha256": app.digest_file(self.backup),
                         "format": "PostgreSQL custom", "verified_by": "pg_restore --list"})
        assignments = {"marko": "L414", "aldo": "L413", "jason": "L201", "jorge": "L409"}
        self.plan = {"version": 1, "source_sha256": "a" * 64, "source_name": "inventario.xlsx", "assignments": assignments,
                     "users": [{key: value for key, value in FakeState.user(0, name, role).items() if key != "id"}
                               for name, role in app.DEMO_ROLES.items()], "pending": [], "equipment": []}
        for owner, lab in assignments.items():
            for ordinal in range(3):
                self.plan["equipment"].append({"source_sheet": lab, "source_row": ordinal + 2, "lab": lab, "actor": owner,
                                               "categoria": "Instrumentación", "subcategoria": "Equipos",
                                               "payload": {"codigoInterno": f"{lab}-{ordinal}", "nombre": "Equipo simulado",
                                                           "estado": "OPERATIVO", "requiereMantenimiento": False,
                                                           "serieUtec": f"UTEC-{lab}-{ordinal}", "numeroSerie": None,
                                                           "comentario": "Referencia de prueba", "idResponsable": None}})
        app.atomic_json(self.path, self.plan)
        self.state = FakeState()
        self.state.credential_path = self.folder / "credenciales-demo.json"
        self.server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), FakeHandler)
        self.server.state = self.state
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join()
        self.environment.stop()
        self.temporary.cleanup()

    def importer(self):
        return app.Importer(self.path, self.backup, api=app.Api(MappedOpener(self.server)),
                            password_prompt=lambda prompt: self.state.passwords["marko" if "marko" in prompt else "aldo"])

    def manifest(self):
        return app.load_json(self.folder / "manifest.json")

    def test_validation_is_offline_and_limits_l201(self):
        with mock.patch.object(app, "Api", side_effect=AssertionError("No debe haber HTTP")), contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(app.main(["--plan", str(self.path)]), 0)
        self.assertFalse(self.state.requests)
        self.plan["equipment"][0]["lab"] = "L201"
        self.plan["equipment"][0]["source_sheet"] = "L201"
        self.plan["equipment"][0]["source_row"] = 129
        app.atomic_json(self.path, self.plan)
        with self.assertRaisesRegex(app.ImportFailure, "alcance"):
            app.validate_plan(self.path)

    def test_full_http_flow_assigns_one_lab_and_verifies_five_actors_without_secret_logs(self):
        importer = self.importer()
        output = io.StringIO()
        with contextlib.redirect_stdout(output), contextlib.redirect_stderr(output):
            result = importer.run()
        self.assertEqual(result["equipment"], 12)
        self.assertEqual(result["actors_verified"], ["aldo", "gabriela", "jason", "jorge", "marko"])
        self.assertEqual(len(self.state.data["equipos"]), 12)
        self.assertTrue(all(self.state.passwords_written_before_create))
        credentials = app.load_json(self.state.credential_path)
        self.assertEqual(len({item["password"] for item in credentials["users"].values()}), 3)
        manifest = self.manifest()
        self.assertTrue(manifest["completed"])
        self.assertEqual(manifest["assignments_before"]["marko"], [99])
        lab_ids = {row["codigo"]: row["id"] for row in self.state.data["laboratorios"]}
        users = {row["userName"]: row for row in self.state.data["usuarios"]}
        for owner, lab in self.plan["assignments"].items():
            self.assertEqual(self.state.assignments[users[owner]["id"]], {lab_ids[lab]})
        self.assertNotIn(users["gabriela"]["id"], self.state.assignments)
        serialized = json.dumps(manifest) + output.getvalue()
        for password in self.state.passwords.values():
            self.assertNotIn(password, serialized)
        self.assertNotIn("TOKEN_PRIVATE", serialized)
        self.assertEqual(importer.tokens, {})
        self.assertEqual(importer.passwords, {})

    def test_wrong_existing_role_stops_before_mutations_and_does_not_store_passwords(self):
        self.state.data["usuarios"][1]["rol"] = "ADMIN"
        importer = self.importer()
        with self.assertRaisesRegex(app.ImportFailure, "rol esperado"):
            importer.run()
        self.assertEqual(self.state.mutations(), [])
        self.assertFalse(self.state.credential_path.exists())
        self.assertEqual(importer.tokens, {})

    def test_http_403_stops_without_any_write(self):
        self.state.block_path = "/admin/auditoria"
        with self.assertRaises(app.ApiFailure) as caught:
            self.importer().run()
        self.assertEqual(caught.exception.status, 403)
        self.assertEqual(self.state.mutations(), [])
        self.assertFalse(self.state.credential_path.exists())

    def test_existing_code_conflict_stops_before_new_users_or_equipment(self):
        self.state.data["equipos"].append({"id": 44, "codigoInterno": "L414-0", "nombre": "Otro equipo"})
        with self.assertRaisesRegex(app.ImportFailure, "Código existente incompatible"):
            self.importer().run()
        self.assertEqual(self.state.mutations(), [])
        self.assertFalse(self.state.credential_path.exists())

    def test_preexisting_serial_is_pending_without_inventing_replacement(self):
        self.state.data["equipos"].append({"id": 44, "codigoInterno": "EQUIPO-AJENO", "serieUtec": "UTEC-L414-0"})
        result = self.importer().run()
        self.assertEqual(result["pending"], 1)
        self.assertEqual(self.manifest()["equipment"]["L414:2"]["status"], "pendiente")
        self.assertFalse(any(row["codigoInterno"] == "L414-0" for row in self.state.data["equipos"]))

    @staticmethod
    def serial_resolution():
        return {"field": "numeroSerie", "original_value": "1", "value": None,
                "conflicting_equipment_id": 1,
                "reason": "Serie del Excel coincide con un registro previo; pendiente de verificar"}

    def conflict_plan(self):
        row = next(item for item in self.plan["equipment"] if item["actor"] == "jason")
        row["source_row"] = 57
        row["payload"]["numeroSerie"] = "1"
        app.atomic_json(self.path, self.plan)
        return row

    def test_explicit_serial_resolution_preserves_plan_and_adds_deterministic_reference(self):
        row = self.conflict_plan()
        original_plan = copy.deepcopy(self.plan)
        original_hash = app.digest_file(self.path)
        importer = self.importer()
        importer.prepare_state(importer.backup_hash())
        importer.state["field_resolutions"] = {"L201:57": self.serial_resolution()}
        importer.validate_field_resolutions()
        categories = {"instrumentación": {"id": 4}}
        subcategories = {(4, "equipos"): {"id": 5}}
        labs = {"l201": {"id": 6}}
        body = importer.resolved_body(row, categories, subcategories, labs)
        self.assertIsNone(body["numeroSerie"])
        self.assertIn("Referencia de prueba", body["comentario"])
        self.assertIn('numeroSerie original="1"; registro previo ID=1;', body["comentario"])
        self.assertIn(self.serial_resolution()["reason"], body["comentario"])
        self.assertEqual(body, importer.resolved_body(row, categories, subcategories, labs))
        self.assertEqual(body["comentario"].count("Resolución de importación:"), 1)
        self.assertEqual(self.plan, original_plan)
        self.assertEqual(importer.plan, original_plan)
        self.assertEqual(app.digest_file(self.path), original_hash)

    def test_pending_serial_resolution_resumes_after_lost_response_without_duplicate_post(self):
        source = self.conflict_plan()
        old_equipment = {"id": 1, "codigoInterno": "11", "nombre": "11", "estado": "BAJA", "numeroSerie": "1"}
        self.state.data["equipos"].append(copy.deepcopy(old_equipment))
        result = self.importer().run()
        self.assertEqual(result["pending"], 1)
        manifest = self.manifest()
        self.assertEqual(manifest["equipment"]["L201:57"]["status"], "pendiente")
        manifest["field_resolutions"] = {"L201:57": self.serial_resolution()}
        app.atomic_json(self.folder / "manifest.json", manifest)
        plan_hash = app.digest_file(self.path)
        self.state.fail_committed_code = source["payload"]["codigoInterno"]
        with self.assertRaises(app.ApiFailure):
            self.importer().run()
        self.assertIn("equipo:L201:57", self.manifest()["intents"])
        result = self.importer().run()
        self.assertEqual(result["pending"], 0)
        actual = next(row for row in self.state.data["equipos"] if row["codigoInterno"] == source["payload"]["codigoInterno"])
        self.assertIsNone(actual["numeroSerie"])
        self.assertIn('numeroSerie original="1"', actual["comentario"])
        event = next(row for row in self.state.audit if row["entidad"] == "equipo" and row["idEntidad"] == actual["id"])
        self.assertEqual(event["userNameActor"], "jason")
        self.assertEqual(self.state.data["equipos"][0], old_equipment)
        self.assertEqual(len(self.state.data["equipos"]), 13)
        self.assertEqual(self.manifest()["intents"], {})
        mutations = len(self.state.mutations())
        self.importer().run()
        self.assertEqual(len(self.state.mutations()), mutations)
        self.assertEqual(app.digest_file(self.path), plan_hash)

    def test_invalid_field_resolutions_stop_before_business_writes(self):
        self.conflict_plan()
        importer = self.importer()
        importer.prepare_state(importer.backup_hash())
        valid = self.serial_resolution()
        invalid_entries = [
            [],
            {"L201:999": valid},
            {"L201:57": None},
            {"L201:57": dict(valid, field="codigoInterno")},
            {"L201:57": dict(valid, value="replacement")},
            {"L201:57": dict(valid, original_value="otra-serie")},
            {"L201:57": dict(valid, conflicting_equipment_id=True)},
            {"L201:57": dict(valid, conflicting_equipment_id=0)},
            {"L201:57": dict(valid, conflicting_equipment_id=2147483648)},
            {"L201:57": dict(valid, reason=" ")},
            {"L201:57": dict(valid, reason="x" * 301)},
            {"L201:57": dict(valid, reason=1)},
            {"L201:57": dict(valid, idLaboratorio=99)},
        ]
        for resolutions in invalid_entries:
            with self.subTest(resolutions=resolutions):
                manifest = self.manifest()
                manifest["field_resolutions"] = resolutions
                app.atomic_json(self.folder / "manifest.json", manifest)
                with self.assertRaisesRegex(app.ImportFailure, "[Rr]esoluci"):
                    self.importer().run()
                self.assertEqual(self.state.mutations(), [])
                self.assertFalse(self.state.credential_path.exists())

    def test_commit_then_connection_lost_resumes_by_exact_payload_and_new_audit_without_duplicate(self):
        self.state.fail_committed_code = "L414-0"
        importer = self.importer()
        with self.assertRaises(app.ApiFailure):
            importer.run()
        self.assertEqual(importer.tokens, {})
        self.assertEqual(importer.passwords, {})
        self.assertIn("equipo:L414:2", self.manifest()["intents"])
        self.assertEqual(len(self.state.data["equipos"]), 1)
        self.assertFalse(self.manifest()["completed"])
        result = self.importer().run()
        self.assertEqual(result["equipment"], 12)
        self.assertEqual(len(self.state.data["equipos"]), 12)
        self.assertEqual(len({row["codigoInterno"] for row in self.state.data["equipos"]}), 12)
        self.assertEqual(self.manifest()["intents"], {})
        mutations = len(self.state.mutations())
        self.importer().run()
        self.assertEqual(len(self.state.mutations()), mutations)

    def test_timeout_on_last_lab_recovers_even_when_no_labs_remain_missing(self):
        self.state.fail_lab_code = "L414"
        with self.assertRaises(app.ApiFailure):
            self.importer().run()
        self.assertIn("laboratorio:L414", self.manifest()["intents"])
        self.importer().run()
        self.assertEqual(len(self.state.data["laboratorios"]), 4)
        self.assertEqual(self.manifest()["intents"], {})

    def test_backup_hash_mismatch_stops_before_http(self):
        self.backup.write_bytes(b"PGDMP-MODIFICADO")
        with self.assertRaisesRegex(app.ImportFailure, "verificación"):
            self.importer().run()
        self.assertFalse(self.state.requests)

    def test_gabriela_creates_one_equipment_when_catalogs_already_exist(self):
        self.state.data["sedes"] = [{"id": 1, "nombre": "Sede existente", "activo": True}]
        self.state.data["areas"] = [{"id": 1, "nombre": "Área existente", "sede": {"id": 1}, "activo": True}]
        self.state.data["laboratorios"] = [{"id": index, "codigo": lab, "nombre": "Laboratorio " + lab,
                                             "area": {"id": 1}, "activo": True}
                                            for index, lab in enumerate(sorted(app.LABS), 1)]
        self.state.data["categorias"] = [{"id": 1, "nombre": "Instrumentación", "activo": True}]
        self.state.data["subcategorias"] = [{"id": 1, "nombre": "Equipos", "categoria": {"id": 1}, "activo": True}]
        self.importer().run()
        manifest = self.manifest()
        self.assertEqual(len(manifest["actor_overrides"]), 1)
        self.assertTrue(any(row["entity"] == "equipo" and row["actor"] == "gabriela" for row in manifest["created"]))
        self.assertFalse(any(path in ("/categorias", "/subcategorias") for method, path in self.state.mutations()))

    def test_preexisting_demo_accounts_prompt_without_password_reset_or_persisting_their_passwords(self):
        for identifier, (name, role) in enumerate(app.DEMO_ROLES.items(), 3):
            self.state.data["usuarios"].append(self.state.user(identifier, name, role))
            self.state.passwords[name] = "PRIVATE_EXISTING_" + name
        importer = self.importer()
        importer.prompt = lambda prompt: next(value for name, value in self.state.passwords.items() if name in prompt)
        importer.run()
        self.assertEqual(app.load_json(self.state.credential_path)["users"], {})
        self.assertFalse(any("/password" in path or path == "/admin/usuarios" for method, path in self.state.mutations()))
        self.assertEqual(importer.passwords, {})

    def test_unknown_response_cannot_be_recovered_with_another_actor_audit(self):
        self.state.fail_committed_code = "L414-0"
        with self.assertRaises(app.ApiFailure):
            self.importer().run()
        event = next(row for row in self.state.audit if row["entidad"] == "equipo")
        event["userNameActor"] = "aldo"
        event["idUsuarioActor"] = 2
        before = len(self.state.data["equipos"])
        with self.assertRaisesRegex(app.ImportFailure, "autoría"):
            self.importer().run()
        self.assertEqual(len(self.state.data["equipos"]), before)

    def test_server_duplicates_are_rejected_before_mutations(self):
        self.state.data["usuarios"].append(copy.deepcopy(self.state.data["usuarios"][0]))
        with self.assertRaisesRegex(app.ImportFailure, "repetido"):
            self.importer().run()
        self.assertEqual(self.state.mutations(), [])


if __name__ == "__main__":
    unittest.main(verbosity=2)
