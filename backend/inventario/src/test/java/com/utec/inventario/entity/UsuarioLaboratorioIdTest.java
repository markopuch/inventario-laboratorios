package com.utec.inventario.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

class UsuarioLaboratorioIdTest {
    @Test
    void claveCompuestaSeComparaPorAmbosIdsYSeConservaAlSerializar() throws Exception {
        UsuarioLaboratorioId first = new UsuarioLaboratorioId(2, 7);
        UsuarioLaboratorioId equivalent = new UsuarioLaboratorioId(2, 7);
        assertEquals(first, equivalent);
        assertEquals(first.hashCode(), equivalent.hashCode());
        assertNotEquals(first, new UsuarioLaboratorioId(3, 7));
        assertNotEquals(first, new UsuarioLaboratorioId(2, 8));
        assertNotEquals(first, null);
        var values = new HashMap<UsuarioLaboratorioId, String>();
        values.put(first, "asignación");
        assertEquals("asignación", values.get(equivalent));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(first); }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            assertEquals(first, in.readObject());
        }
    }
}
