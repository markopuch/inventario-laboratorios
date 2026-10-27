package com.utec.inventario.security;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.utec.inventario.exception.LoginRateLimitException;

class LoginRateLimiterTest {
    private final AtomicLong now = new AtomicLong();
    private final LoginRateLimiter limiter = new LoginRateLimiter(5, 15, 3, 10, 100, now::get);

    private void fail(LoginRateLimiter value, String user) {
        try (LoginRateLimiter.Attempt attempt = value.beginUser(user)) { attempt.failed(); }
    }

    @Test
    void cincoFallosNormalizadosBloqueanElSextoSinIdentificarLaCuenta() {
        for (int i = 0; i < 5; i++) fail(limiter, "  MaRkO  ");
        LoginRateLimitException error = assertThrows(LoginRateLimitException.class,
                () -> limiter.beginUser("marko"));
        assertEquals(15, error.getRetryAfterSeconds());
        assertEquals(LoginRateLimitException.MESSAGE, error.getMessage());
        assertFalse(error.getMessage().contains("marko"));
    }

    @Test
    void cincoFallosEntrePeersDiferentesCompartenElPresupuestoDelNombre() {
        for (int i = 0; i < 5; i++) {
            limiter.checkSource("peer-" + i);
            fail(limiter, i % 2 == 0 ? "  MiSmO  " : "mismo");
        }
        assertDoesNotThrow(() -> limiter.checkSource("peer-nuevo"));
        assertEquals(15, assertThrows(LoginRateLimitException.class,
                () -> limiter.beginUser("MISMO")).getRetryAfterSeconds());
        try (LoginRateLimiter.Attempt attempt = limiter.beginUser("otro")) {
            attempt.succeeded();
        }
    }

    @Test
    void exitoReiniciaSoloLosFallosDelUsuario() {
        for (int i = 0; i < 4; i++) fail(limiter, "a");
        try (LoginRateLimiter.Attempt attempt = limiter.beginUser("a")) { attempt.succeeded(); }
        for (int i = 0; i < 5; i++) fail(limiter, "a");
        assertThrows(LoginRateLimitException.class, () -> limiter.beginUser("a"));
        try (LoginRateLimiter.Attempt attempt = limiter.beginUser("b")) { attempt.succeeded(); }
        assertThrows(LoginRateLimitException.class, () -> limiter.beginUser("a"));
    }

    @Test
    void ventanaExpiraExactamenteYRechazosNoLaProlongan() {
        for (int i = 0; i < 5; i++) fail(limiter, "a");
        now.set(Duration.ofSeconds(14).toNanos() + 900_000_000L);
        assertEquals(1, assertThrows(LoginRateLimitException.class, () -> limiter.beginUser("a")).getRetryAfterSeconds());
        now.set(Duration.ofSeconds(15).toNanos());
        try (LoginRateLimiter.Attempt attempt = limiter.beginUser("a")) { attempt.succeeded(); }
    }

    @Test
    void limitePorOrigenCuentaSolicitudesYNoSeReiniciaConUnLoginCorrecto() {
        for (int i = 0; i < 3; i++) {
            limiter.checkSource("ip");
            try (LoginRateLimiter.Attempt attempt = limiter.beginUser("a")) { attempt.succeeded(); }
        }
        assertEquals(10, assertThrows(LoginRateLimitException.class, () -> limiter.checkSource("ip")).getRetryAfterSeconds());
        assertDoesNotThrow(() -> limiter.checkSource("otra-ip"));
        now.set(Duration.ofSeconds(10).toNanos());
        assertDoesNotThrow(() -> limiter.checkSource("ip"));
    }

    @Test
    void cerrarPorErrorInternoLiberaLaReservaSinContarFalloNiDuplicarFinalizacion() {
        for (int i = 0; i < 20; i++) limiter.beginUser("a").close();
        LoginRateLimiter.Attempt attempt = limiter.beginUser("a");
        attempt.failed(); attempt.failed(); attempt.close();
        for (int i = 0; i < 4; i++) fail(limiter, "a");
        assertThrows(LoginRateLimitException.class, () -> limiter.beginUser("a"));
    }

    @Test
    @Timeout(10)
    void reservasConcurrentesEntrePeersImpidenSaltarElLimiteAntesDeBcrypt() throws Exception {
        try (var executor = Executors.newFixedThreadPool(20)) {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<LoginRateLimiter.Attempt>> results = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                String source = "peer-" + i;
                results.add(executor.submit(() -> {
                    start.await();
                    limiter.checkSource(source);
                    try { return limiter.beginUser("a"); }
                    catch (LoginRateLimitException exception) { return null; }
                }));
            }
            start.countDown();
            List<LoginRateLimiter.Attempt> allowed = new ArrayList<>();
            for (var future : results) { var value = future.get(); if (value != null) allowed.add(value); }
            assertEquals(5, allowed.size());
            assertEquals(1, assertThrows(LoginRateLimitException.class, () -> limiter.beginUser("a")).getRetryAfterSeconds());
            allowed.forEach(LoginRateLimiter.Attempt::failed);
            assertEquals(15, assertThrows(LoginRateLimitException.class, () -> limiter.beginUser("a")).getRetryAfterSeconds());
        }
    }

    @Test
    void capacidadAcotadaNoExpulsaBloqueosActivosYRecuperaEspacioAlExpirar() {
        LoginRateLimiter small = new LoginRateLimiter(1, 15, 1, 10, 2, now::get);
        fail(small, "a"); fail(small, "b");
        assertThrows(LoginRateLimitException.class, () -> small.beginUser("c"));
        assertThrows(LoginRateLimitException.class, () -> small.beginUser("a"));
        small.checkSource("ip-a"); small.checkSource("ip-b");
        assertThrows(LoginRateLimitException.class, () -> small.checkSource("ip-c"));
        now.set(Duration.ofSeconds(15).toNanos());
        assertDoesNotThrow(() -> small.checkSource("ip-c"));
        small.beginUser("c").close();
    }

    @Test
    void reservaPendienteNoSePierdeAunqueExpireLaVentana() {
        LoginRateLimiter small = new LoginRateLimiter(1, 15, 100, 10, 2, now::get);
        LoginRateLimiter.Attempt pending = small.beginUser("a");
        now.set(Duration.ofSeconds(20).toNanos());
        assertThrows(LoginRateLimitException.class, () -> small.beginUser("a"));
        pending.failed();
        assertEquals(15, assertThrows(LoginRateLimitException.class, () -> small.beginUser("a")).getRetryAfterSeconds());
    }

    @Test
    void origenDesconocidoNoGeneraExcepcionNiUnContadorNuevoPorPeticion() {
        limiter.checkSource(null); limiter.checkSource(""); limiter.checkSource(" ");
        assertThrows(LoginRateLimitException.class, () -> limiter.checkSource(null));
    }

    @Test
    void configuracionInvalidaFallaAlArrancar() {
        assertThrows(IllegalArgumentException.class, () -> new LoginRateLimiter(0, 15, 3, 10, 100, now::get));
        assertThrows(IllegalArgumentException.class, () -> new LoginRateLimiter(5, 0, 3, 10, 100, now::get));
        assertThrows(IllegalArgumentException.class, () -> new LoginRateLimiter(5, 15, 0, 10, 100, now::get));
        assertThrows(IllegalArgumentException.class, () -> new LoginRateLimiter(5, 15, 3, 0, 100, now::get));
        assertThrows(IllegalArgumentException.class, () -> new LoginRateLimiter(5, 15, 3, 10, 0, now::get));
    }
}
