package com.ahs.cvm.llm.subprocess;

import static org.assertj.core.api.Assertions.assertThat;

import com.ahs.cvm.llm.subprocess.SubprocessRunner.SubprocessRequest;
import com.ahs.cvm.llm.subprocess.SubprocessRunner.SubprocessResult;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Tests gegen den realen {@link ClaudeCodeSubprocessRunner}. Kein
 * Claude-Code-Binary noetig: wir starten {@code echo} bzw.
 * {@code sleep}, was unter Linux/macOS ausreicht. Tests werden
 * unter Windows uebersprungen.
 */
class ClaudeCodeSubprocessRunnerTest {

    private final ClaudeCodeSubprocessRunner runner = new ClaudeCodeSubprocessRunner();

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("ClaudeCodeSubprocess: echo liefert Exit-Code 0 und stdout")
    void echoOk() {
        SubprocessRequest req = new SubprocessRequest(
                List.of("echo", "hello"),
                Path.of(System.getProperty("java.io.tmpdir")),
                null, Duration.ofSeconds(5), Map.of(), false);
        SubprocessResult res = runner.run(req);
        assertThat(res.exitCode()).isZero();
        assertThat(res.stdout()).contains("hello");
        assertThat(res.timedOut()).isFalse();
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("ClaudeCodeSubprocess: Timeout greift und destroyForcibly wird ausgeloest")
    void timeoutGreift() {
        SubprocessRequest req = new SubprocessRequest(
                List.of("sleep", "5"),
                Path.of(System.getProperty("java.io.tmpdir")),
                null, Duration.ofMillis(200), Map.of(), false);
        SubprocessResult res = runner.run(req);
        assertThat(res.timedOut()).isTrue();
        assertThat(res.exitCode()).isEqualTo(-1);
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("ClaudeCodeSubprocess: nicht existierendes Binary -> exitCode -1, kein Crash")
    void nichtExistierendesBinary() {
        SubprocessRequest req = new SubprocessRequest(
                List.of("/does/not/exist/cvm-fake-binary"),
                Path.of(System.getProperty("java.io.tmpdir")),
                null, Duration.ofSeconds(2), Map.of(), false);
        SubprocessResult res = runner.run(req);
        assertThat(res.exitCode()).isEqualTo(-1);
        assertThat(res.stderr()).contains("Failed to start");
    }
}
