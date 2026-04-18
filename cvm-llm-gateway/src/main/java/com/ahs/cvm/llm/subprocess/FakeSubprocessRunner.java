package com.ahs.cvm.llm.subprocess;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default-Subprocess-Runner. Liefert ein konfigurierbares Ergebnis
 * statt einen echten Prozess zu starten. Aktiv, solange kein anderer
 * Runner als Bean registriert ist und das Reachability-Feature nicht
 * explizit auf real gestellt wurde
 * ({@code cvm.ai.reachability.subprocess.real=false} Default).
 *
 * <p>Tests koennen ueber {@link #setResponseFactory(Function)} eine
 * eigene Antwort-Factory setzen.
 */
@Component
@ConditionalOnProperty(prefix = "cvm.ai.reachability.subprocess",
        name = "real", havingValue = "false", matchIfMissing = true)
@ConditionalOnMissingBean(name = "claudeCodeSubprocessRunner")
public class FakeSubprocessRunner implements SubprocessRunner {

    private static final String DEFAULT_JSON = """
            {"findings":{"callSites":[]},"summary":"Fake-Reachability ohne Treffer."}
            """;

    private final List<SubprocessRequest> aufrufe = new ArrayList<>();
    private Function<SubprocessRequest, SubprocessResult> responseFactory;

    public FakeSubprocessRunner() {
        this.responseFactory = req -> new SubprocessResult(
                0, DEFAULT_JSON, "", 5L, false);
    }

    public void setResponseFactory(Function<SubprocessRequest, SubprocessResult> factory) {
        this.responseFactory = factory;
    }

    @Override
    public SubprocessResult run(SubprocessRequest request) {
        aufrufe.add(request);
        return responseFactory.apply(request);
    }

    public List<SubprocessRequest> aufrufe() {
        return List.copyOf(aufrufe);
    }
}
