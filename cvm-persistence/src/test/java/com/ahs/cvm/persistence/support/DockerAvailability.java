package com.ahs.cvm.persistence.support;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Robuste Pruefung, ob ein Docker-Daemon erreichbar ist. Wird von
 * {@link org.junit.jupiter.api.condition.EnabledIf} ueber den voll qualifizierten
 * Methodennamen referenziert.
 */
public final class DockerAvailability {

    private static final String DEFAULT_UNIX_SOCKET = "/var/run/docker.sock";
    private static volatile Boolean cachedAvailability = null;

    private DockerAvailability() {}

    public static boolean isAvailable() {
        Boolean cached = cachedAvailability;
        if (cached != null) {
            return cached;
        }
        boolean result = probe();
        cachedAvailability = result;
        return result;
    }

    /**
     * Signalisiert, dass Testcontainers zwar einen Socket gefunden, den
     * Container aber nicht starten konnte. Nachfolgende
     * {@link #isAvailable()}-Aufrufe liefern {@code false}, damit
     * {@code @EnabledIf}-Guards die abhaengigen Tests skippen, anstatt mit
     * einem {@code ApplicationContextException} aus einem gescheiterten
     * Container-Start zu laufen.
     */
    public static void markContainerStartFailed(Throwable ursache) {
        cachedAvailability = false;
        System.err.println("[DockerAvailability] Testcontainers-Start fehlgeschlagen, "
                + "Slice-Tests werden geskippt: " + ursache.getMessage());
    }

    private static boolean probe() {
        String dockerHost = System.getenv("DOCKER_HOST");
        boolean socketSichtbar;
        if (dockerHost != null && !dockerHost.isBlank()) {
            socketSichtbar = pingDockerHost(dockerHost);
        } else {
            socketSichtbar = Files.exists(Path.of(DEFAULT_UNIX_SOCKET));
        }
        if (!socketSichtbar) {
            return false;
        }
        // Socket sichtbar heisst nicht automatisch, dass Testcontainers die Umgebung
        // erfolgreich aushandeln kann (Docker Desktop auf macOS veroeffentlicht den
        // Socket unter {@code ~/.docker/run/docker.sock}, Testcontainers probiert
        // aber zunaechst feste TCP-Hosts). Wir triggern daher einen echten
        // Testcontainers-Probe und fallen auf "nicht verfuegbar" zurueck, falls der
        // fehlschlaegt.
        try {
            Class<?> factoryCls = Class.forName("org.testcontainers.DockerClientFactory");
            Object factory = factoryCls.getMethod("instance").invoke(null);
            Object ok = factoryCls.getMethod("isDockerAvailable").invoke(factory);
            return Boolean.TRUE.equals(ok);
        } catch (ReflectiveOperationException | RuntimeException probeFehler) {
            System.err.println("[DockerAvailability] Testcontainers-Probe fehlgeschlagen: "
                    + probeFehler.getMessage());
            return false;
        }
    }

    private static boolean pingDockerHost(String dockerHost) {
        try {
            URI uri = URI.create(dockerHost);
            String scheme = uri.getScheme();
            if (scheme == null) return false;
            if (scheme.startsWith("unix")) {
                String path = uri.getPath();
                return path != null && Files.exists(Path.of(path));
            }
            if (scheme.startsWith("tcp") || scheme.startsWith("http")) {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(uri.getHost(), uri.getPort()), 500);
                    return true;
                }
            }
            return false;
        } catch (IOException | IllegalArgumentException e) {
            return false;
        }
    }
}
