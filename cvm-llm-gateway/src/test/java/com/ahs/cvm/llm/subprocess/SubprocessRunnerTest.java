package com.ahs.cvm.llm.subprocess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ahs.cvm.llm.subprocess.SubprocessRunner.SubprocessRequest;
import com.ahs.cvm.llm.subprocess.SubprocessRunner.SubprocessResult;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SubprocessRunnerTest {

    @Test
    @DisplayName("SubprocessRequest: leeres command wirft IllegalArgumentException")
    void leeresCommand() {
        assertThatThrownBy(() -> new SubprocessRequest(
                List.of(), Path.of("/tmp"), null, Duration.ofSeconds(5),
                Map.of(), false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SubprocessRequest: requireReadOnly ohne --read-only wirft")
    void requireReadOnlyOhneFlag() {
        assertThatThrownBy(() -> new SubprocessRequest(
                List.of("claude", "code"),
                Path.of("/tmp"), null, Duration.ofSeconds(5),
                Map.of(), true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--read-only");
    }

    @Test
    @DisplayName("SubprocessRequest: requireReadOnly mit --read-only ist gueltig")
    void requireReadOnlyMitFlag() {
        SubprocessRequest req = new SubprocessRequest(
                List.of("claude", "code", "--read-only"),
                Path.of("/tmp"), null, Duration.ofSeconds(5),
                Map.of(), true);
        assertThat(req.command()).contains("--read-only");
    }

    @Test
    @DisplayName("SubprocessRequest: timeout <= 0 wirft")
    void timeoutNegativ() {
        assertThatThrownBy(() -> new SubprocessRequest(
                List.of("x"), Path.of("/tmp"), null,
                Duration.ZERO, Map.of(), false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("FakeSubprocessRunner: liefert default-JSON und protokolliert Aufrufe")
    void fakeRunner() {
        FakeSubprocessRunner runner = new FakeSubprocessRunner();
        SubprocessRequest req = new SubprocessRequest(
                List.of("claude", "--read-only"),
                Path.of("/tmp"), null, Duration.ofSeconds(5),
                Map.of(), true);
        SubprocessResult res = runner.run(req);
        assertThat(res.ok()).isTrue();
        assertThat(res.stdout()).contains("Fake-Reachability");
        assertThat(runner.aufrufe()).hasSize(1);
    }

    @Test
    @DisplayName("FakeSubprocessRunner: setResponseFactory ueberschreibt Default")
    void fakeRunnerSetzt() {
        FakeSubprocessRunner runner = new FakeSubprocessRunner();
        runner.setResponseFactory(req -> new SubprocessResult(
                -1, "", "timeout", 100L, true));
        SubprocessResult res = runner.run(new SubprocessRequest(
                List.of("x"), Path.of("/tmp"), null, Duration.ofSeconds(1),
                Map.of(), false));
        assertThat(res.ok()).isFalse();
        assertThat(res.timedOut()).isTrue();
    }
}
