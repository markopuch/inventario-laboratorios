package com.utec.inventario.security;

import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.LongSupplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.utec.inventario.exception.LoginRateLimitException;

/** Contadores acotados por instancia. No persiste claves, usuarios ni direcciones. */
@Component
public class LoginRateLimiter {
    private final int maxFailures;
    private final long failureWindow;
    private final int ipAttempts;
    private final long ipWindow;
    private final int maxEntries;
    private final LongSupplier nanoTime;
    private final Map<String, IpWindow> sources = new HashMap<>();
    private final Map<UserSource, UserWindow> users = new HashMap<>();
    private long lastCleanup;

    @Autowired
    public LoginRateLimiter(
            @Value("${app.login-rate-limit.max-failures:5}") int maxFailures,
            @Value("${app.login-rate-limit.failure-window-seconds:900}") int failureWindowSeconds,
            @Value("${app.login-rate-limit.ip-attempts:60}") int ipAttempts,
            @Value("${app.login-rate-limit.ip-window-seconds:60}") int ipWindowSeconds,
            @Value("${app.login-rate-limit.max-entries:10000}") int maxEntries) {
        this(maxFailures, failureWindowSeconds, ipAttempts, ipWindowSeconds, maxEntries, System::nanoTime);
    }

    LoginRateLimiter(int maxFailures, int failureWindowSeconds, int ipAttempts, int ipWindowSeconds,
            int maxEntries, LongSupplier nanoTime) {
        if (maxFailures < 1 || maxFailures > 1000 || failureWindowSeconds < 1 || failureWindowSeconds > 86400
                || ipAttempts < 1 || ipAttempts > 100000 || ipWindowSeconds < 1 || ipWindowSeconds > 86400
                || maxEntries < 1 || maxEntries > 100000) {
            throw new IllegalArgumentException("La configuración del límite de login está fuera de rango.");
        }
        this.maxFailures = maxFailures;
        this.failureWindow = Duration.ofSeconds(failureWindowSeconds).toNanos();
        this.ipAttempts = ipAttempts;
        this.ipWindow = Duration.ofSeconds(ipWindowSeconds).toNanos();
        this.maxEntries = maxEntries;
        this.nanoTime = nanoTime;
        this.lastCleanup = nanoTime.getAsLong();
    }

    /** Se llama antes de parsear el body/JWT para limitar también solicitudes malformadas. */
    public synchronized void checkSource(String remoteAddress) {
        long now = nanoTime.getAsLong();
        cleanup(now);
        String source = source(remoteAddress);
        IpWindow window = sources.get(source);
        if (window != null && elapsed(now, window.since, ipWindow)) {
            sources.remove(source);
            window = null;
        }
        if (window == null) {
            if (sources.size() >= maxEntries) throw new LoginRateLimitException(seconds(ipWindow));
            window = new IpWindow(now);
            sources.put(source, window);
        }
        if (window.attempts >= ipAttempts) throw new LoginRateLimitException(remaining(now, window.since, ipWindow));
        window.attempts++;
    }

    /** Reserva un intento antes de BCrypt; solicitudes concurrentes no eluden el límite. */
    public synchronized Attempt beginUser(String userName, String remoteAddress) {
        long now = nanoTime.getAsLong();
        cleanup(now);
        UserSource key = new UserSource(userName.trim().toLowerCase(Locale.ROOT), source(remoteAddress));
        UserWindow window = users.get(key);
        if (window != null) expireFailures(window, now);
        if (window == null) {
            if (users.size() >= maxEntries) throw new LoginRateLimitException(seconds(Math.min(ipWindow, failureWindow)));
            window = new UserWindow();
            users.put(key, window);
        }
        if (window.failures >= maxFailures) {
            throw new LoginRateLimitException(remaining(now, window.firstFailure, failureWindow));
        }
        if (window.failures + window.inFlight >= maxFailures) {
            throw new LoginRateLimitException(1);
        }
        window.inFlight++;
        return new Attempt(key, window);
    }

    private synchronized void finish(Attempt attempt, boolean success, boolean failure) {
        if (attempt.completed) return;
        attempt.completed = true;
        UserWindow window = attempt.window;
        window.inFlight--;
        long now = nanoTime.getAsLong();
        expireFailures(window, now);
        if (success) window.failures = 0;
        else if (failure) {
            if (window.failures == 0) window.firstFailure = now;
            window.failures++;
        }
        if (window.failures == 0 && window.inFlight == 0) users.remove(attempt.key, window);
    }

    private void cleanup(long now) {
        if (!elapsed(now, lastCleanup, Math.min(ipWindow, failureWindow))) return;
        sources.entrySet().removeIf(entry -> elapsed(now, entry.getValue().since, ipWindow));
        users.entrySet().removeIf(entry -> {
            UserWindow window = entry.getValue();
            expireFailures(window, now);
            return window.failures == 0 && window.inFlight == 0;
        });
        lastCleanup = now;
    }

    private void expireFailures(UserWindow window, long now) {
        if (window.failures > 0 && elapsed(now, window.firstFailure, failureWindow)) window.failures = 0;
    }

    private static boolean elapsed(long now, long since, long duration) { return now - since >= duration; }
    private static long remaining(long now, long since, long duration) { return seconds(Math.max(1, duration - (now - since))); }
    private static long seconds(long nanos) { return Math.max(1, (nanos + 999_999_999L) / 1_000_000_000L); }
    private static String source(String value) { return value == null || value.isBlank() ? "unknown" : value; }

    public final class Attempt implements AutoCloseable {
        private final UserSource key;
        private final UserWindow window;
        private boolean completed;
        private Attempt(UserSource key, UserWindow window) { this.key = key; this.window = window; }
        public void succeeded() { finish(this, true, false); }
        public void failed() { finish(this, false, true); }
        @Override public void close() { finish(this, false, false); }
    }

    private record UserSource(String userName, String source) { }
    private static final class UserWindow { private int failures; private int inFlight; private long firstFailure; }
    private static final class IpWindow {
        private final long since;
        private int attempts;
        private IpWindow(long since) { this.since = since; }
    }
}
