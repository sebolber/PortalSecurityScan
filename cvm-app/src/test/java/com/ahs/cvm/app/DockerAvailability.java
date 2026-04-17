package com.ahs.cvm.app;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Hilfsklasse zur bedingten Aktivierung von Integrationstests.
 *
 * <p>Prueft robust, ob ein Docker-Daemon erreichbar ist. Greift dabei zuerst auf
 * die Umgebungsvariable {@code DOCKER_HOST} zu und faellt danach auf den
 * Unix-Socket {@code /var/run/docker.sock} zurueck. Wird von JUnits
 * {@link org.junit.jupiter.api.condition.EnabledIf} ueber den voll qualifizierten
 * Methodennamen referenziert.
 */
public final class DockerAvailability {

    private static final String DEFAULT_UNIX_SOCKET = "/var/run/docker.sock";

    private DockerAvailability() {}

    public static boolean isAvailable() {
        String dockerHost = System.getenv("DOCKER_HOST");
        if (dockerHost != null && !dockerHost.isBlank()) {
            return pingDockerHost(dockerHost);
        }
        return Files.exists(Path.of(DEFAULT_UNIX_SOCKET));
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
