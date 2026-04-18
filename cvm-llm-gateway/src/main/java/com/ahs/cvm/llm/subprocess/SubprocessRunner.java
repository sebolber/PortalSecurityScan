package com.ahs.cvm.llm.subprocess;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Abstraktion ueber externe Subprozesse (Iteration 15, CVM-40).
 *
 * <p>Konkreter Use-Case: Claude Code CLI gegen einen
 * Git-Checkout fuer die Reachability-Analyse. Die Abstraktion lebt
 * im LLM-Gateway, weil Subprozess-Aufrufe Audit- und Sicherheits-
 * pflichten haben (analog zum HTTP-LlmClient).
 */
public interface SubprocessRunner {

    SubprocessResult run(SubprocessRequest request);

    /**
     * @param command Befehlszeile (programm + Argumente).
     * @param workingDirectory Arbeitsverzeichnis, in dem der
     *     Prozess gestartet wird.
     * @param promptFile optionale Prompt-Datei, wird vom Aufrufer
     *     vorab geschrieben (nur fuer Audit/Logging).
     * @param timeout maximale Laufzeit; danach {@code destroyForcibly}.
     * @param environment zusaetzliche Umgebungs-Variablen.
     * @param requireReadOnly wenn {@code true}, muss
     *     {@code --read-only} in {@link #command} sein - sonst
     *     wirft der Runner sofort.
     */
    record SubprocessRequest(
            List<String> command,
            Path workingDirectory,
            Path promptFile,
            Duration timeout,
            Map<String, String> environment,
            boolean requireReadOnly) {

        public SubprocessRequest {
            if (command == null || command.isEmpty()) {
                throw new IllegalArgumentException("command darf nicht leer sein.");
            }
            if (workingDirectory == null) {
                throw new IllegalArgumentException("workingDirectory darf nicht null sein.");
            }
            if (timeout == null || timeout.isNegative() || timeout.isZero()) {
                throw new IllegalArgumentException("timeout muss > 0 sein.");
            }
            command = List.copyOf(command);
            environment = environment == null ? Map.of() : Map.copyOf(environment);
            if (requireReadOnly && command.stream().noneMatch("--read-only"::equals)) {
                throw new IllegalArgumentException(
                        "requireReadOnly=true: command muss --read-only enthalten.");
            }
        }
    }

    /**
     * Ergebnis eines Subprozess-Aufrufs.
     *
     * @param exitCode Exit-Code (-1 wenn Timeout).
     * @param stdout Standard-Ausgabe.
     * @param stderr Standard-Fehler.
     * @param durationMs Gemessene Laufzeit.
     * @param timedOut {@code true}, wenn der Aufruf via Timeout
     *     beendet wurde.
     */
    record SubprocessResult(
            int exitCode,
            String stdout,
            String stderr,
            long durationMs,
            boolean timedOut) {

        public boolean ok() {
            return !timedOut && exitCode == 0;
        }
    }
}
