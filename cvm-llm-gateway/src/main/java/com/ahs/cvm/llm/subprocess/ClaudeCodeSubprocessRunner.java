package com.ahs.cvm.llm.subprocess;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Realer Subprocess-Runner via {@link ProcessBuilder}. Aktiv ueber
 * {@code cvm.ai.reachability.subprocess.real=true}.
 *
 * <p>Sicherheits-Hinweise:
 * <ul>
 *   <li>{@code requireReadOnly}-Flag wird bereits im
 *       {@link SubprocessRunner.SubprocessRequest}-Konstruktor
 *       erzwungen.</li>
 *   <li>{@code stdin} wird auf {@code Redirect.INHERIT-NULL}
 *       konfiguriert (kein Input).</li>
 *   <li>Netzwerk-Sandboxing ist Sache der Deployment-Schicht
 *       (OpenShift NetworkPolicy + ggf. Seccomp). Ein TODO im
 *       Konzept-Dokument hinterlegt das.</li>
 * </ul>
 */
@Component("claudeCodeSubprocessRunner")
@ConditionalOnProperty(prefix = "cvm.ai.reachability.subprocess",
        name = "real", havingValue = "true")
public class ClaudeCodeSubprocessRunner implements SubprocessRunner {

    private static final Logger log =
            LoggerFactory.getLogger(ClaudeCodeSubprocessRunner.class);

    @Override
    public SubprocessResult run(SubprocessRequest request) {
        ProcessBuilder pb = new ProcessBuilder(request.command())
                .directory(request.workingDirectory().toFile())
                .redirectErrorStream(false);
        pb.environment().putAll(request.environment());
        Instant start = Instant.now();
        Process process;
        try {
            process = pb.start();
        } catch (IOException ex) {
            return new SubprocessResult(
                    -1, "", "Failed to start: " + ex.getMessage(), 0L, false);
        }
        try {
            // stdin schliessen: kein Input erlaubt.
            process.getOutputStream().close();
        } catch (IOException ignored) {
            // stdin war evtl. nie offen.
        }

        boolean finished;
        try {
            finished = process.waitFor(
                    request.timeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return new SubprocessResult(-1, "", "interrupted",
                    Instant.now().toEpochMilli() - start.toEpochMilli(), true);
        }
        long durationMs = Instant.now().toEpochMilli() - start.toEpochMilli();
        if (!finished) {
            process.destroyForcibly();
            log.warn("Subprocess Timeout nach {} ms (cmd={})",
                    durationMs, request.command());
            return new SubprocessResult(-1, "", "timeout", durationMs, true);
        }
        String stdout = readAll(process.getInputStream());
        String stderr = readAll(process.getErrorStream());
        return new SubprocessResult(
                process.exitValue(), stdout, stderr, durationMs, false);
    }

    private static String readAll(InputStream in) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out);
            return out.toString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "";
        }
    }
}
