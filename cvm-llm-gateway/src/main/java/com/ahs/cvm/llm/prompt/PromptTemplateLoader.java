package com.ahs.cvm.llm.prompt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Laedt Prompt-Templates aus {@code classpath:cvm/llm/prompts/}.
 * Format: eine einfache Properties-artige Struktur. Der Loader
 * zerlegt die Datei in die drei Abschnitte {@code #system},
 * {@code #user} und optional {@code #version}.
 *
 * <p>Beispiel ({@code cvm/llm/prompts/assessment.propose.st}):
 * <pre>
 * #version: v1
 * #system:
 * Du bist ein CVE-Gutachter.
 * #user:
 * Bewerte ${cveKey} fuer ${produkt}.
 * </pre>
 */
@Component
public class PromptTemplateLoader {

    private final Map<String, PromptTemplate> cache = new ConcurrentHashMap<>();

    public PromptTemplate load(String id) {
        return cache.computeIfAbsent(id, this::read);
    }

    PromptTemplate read(String id) {
        String pfad = "cvm/llm/prompts/" + id + ".st";
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(pfad)) {
            if (in == null) {
                throw new IllegalArgumentException("Template fehlt: " + pfad);
            }
            String quelle = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return parse(id, quelle);
        } catch (IOException ex) {
            throw new IllegalStateException("Template nicht lesbar: " + pfad, ex);
        }
    }

    static PromptTemplate parse(String id, String quelle) {
        String version = "v1";
        StringBuilder system = new StringBuilder();
        StringBuilder user = new StringBuilder();
        String current = null;
        for (String line : quelle.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#version:")) {
                version = trimmed.substring("#version:".length()).trim();
                continue;
            }
            if (trimmed.equals("#system:")) {
                current = "system";
                continue;
            }
            if (trimmed.equals("#user:")) {
                current = "user";
                continue;
            }
            if ("system".equals(current)) {
                system.append(line).append('\n');
            } else if ("user".equals(current)) {
                user.append(line).append('\n');
            }
        }
        return new PromptTemplate(id, version,
                system.toString().strip(),
                user.toString().strip());
    }
}
