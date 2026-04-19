package com.ahs.cvm.integration.osv;

import com.ahs.cvm.application.cve.ComponentVulnerabilityLookup;
import jakarta.annotation.PostConstruct;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Spring-Bean-Adapter fuer den {@link OsvJsonlMirror} (Iteration 72,
 * CVM-309). Verdraengt bei Aktivierung
 * ({@code cvm.enrichment.osv.mirror.enabled=true}) via {@link Primary}
 * die Netz-basierte {@link OsvComponentLookup}.
 *
 * <p>Liest beim Spring-Start die JSONL-Datei einmal ein. Ein
 * manueller Reload ueber {@link OsvJsonlMirror#reload()} ist
 * moeglich (derzeit ohne Admin-UI).
 */
@Component
@Primary
@ConditionalOnProperty(
        prefix = "cvm.enrichment.osv.mirror",
        name = "enabled",
        havingValue = "true")
public class OsvJsonlMirrorLookup implements ComponentVulnerabilityLookup {

    private static final Logger log = LoggerFactory.getLogger(OsvJsonlMirrorLookup.class);

    private final OsvJsonlMirror mirror;
    private final String configuredFile;

    public OsvJsonlMirrorLookup(
            @Value("${cvm.enrichment.osv.mirror.file:}") String file) {
        this.configuredFile = file == null ? "" : file;
        this.mirror = new OsvJsonlMirror(
                configuredFile.isBlank() ? null : Path.of(configuredFile));
    }

    @PostConstruct
    void loadAtStartup() {
        if (configuredFile.isBlank()) {
            log.warn(
                    "OSV-Mirror aktiviert, aber cvm.enrichment.osv.mirror.file ist leer. "
                            + "Lookup liefert leere Ergebnisse.");
            return;
        }
        mirror.reload();
    }

    @Override
    public boolean isEnabled() {
        return !configuredFile.isBlank();
    }

    @Override
    public Map<String, List<String>> findCveIdsForPurls(List<String> purls) {
        return mirror.findCveIdsForPurls(purls);
    }

    /** Fuer Tests/Admin-Tools: Index neu laden. */
    public void reload() {
        mirror.reload();
    }
}
