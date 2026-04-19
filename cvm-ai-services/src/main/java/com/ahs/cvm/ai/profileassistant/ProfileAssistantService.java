package com.ahs.cvm.ai.profileassistant;

import com.ahs.cvm.application.profile.ContextProfileService;
import com.ahs.cvm.application.profile.ProfileView;
import com.ahs.cvm.llm.AiCallAuditService;
import com.ahs.cvm.llm.LlmClient;
import com.ahs.cvm.llm.LlmClient.LlmRequest;
import com.ahs.cvm.llm.LlmClient.LlmResponse;
import com.ahs.cvm.llm.LlmClient.Message;
import com.ahs.cvm.llm.LlmClientSelector;
import com.ahs.cvm.llm.prompt.PromptTemplate;
import com.ahs.cvm.llm.prompt.PromptTemplateLoader;
import com.ahs.cvm.persistence.assessment.AssessmentRepository;
import com.ahs.cvm.persistence.environment.Environment;
import com.ahs.cvm.persistence.environment.EnvironmentRepository;
import com.ahs.cvm.persistence.profileassist.ProfileAssistSession;
import com.ahs.cvm.persistence.profileassist.ProfileAssistSessionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dialogischer Profil-Assistent (Iteration 18, CVM-43).
 *
 * <p><strong>Invariante:</strong> {@link #finalizeDraft(UUID, String)} ruft
 * {@link ContextProfileService#proposeNewVersion} - also den regulaeren
 * Draft-Workflow. Es gibt keinen Direkt-Schreibpfad auf das aktive
 * Profil.
 */
@Service
public class ProfileAssistantService {

    private static final Logger log = LoggerFactory.getLogger(ProfileAssistantService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String USE_CASE = "PROFILE_WIZARD";
    private static final String TEMPLATE_ID = "profile-wizard";

    private final ProfileAssistantConfig config;
    private final ProfileAssistSessionRepository sessionRepo;
    private final EnvironmentRepository environmentRepo;
    private final AssessmentRepository assessmentRepo;
    private final ContextProfileService profileService;
    private final AiCallAuditService auditService;
    private final LlmClientSelector clientSelector;
    private final PromptTemplateLoader templateLoader;

    public ProfileAssistantService(
            ProfileAssistantConfig config,
            ProfileAssistSessionRepository sessionRepo,
            EnvironmentRepository environmentRepo,
            AssessmentRepository assessmentRepo,
            ContextProfileService profileService,
            AiCallAuditService auditService,
            LlmClientSelector clientSelector,
            PromptTemplateLoader templateLoader) {
        this.config = config;
        this.sessionRepo = sessionRepo;
        this.environmentRepo = environmentRepo;
        this.assessmentRepo = assessmentRepo;
        this.profileService = profileService;
        this.auditService = auditService;
        this.clientSelector = clientSelector;
        this.templateLoader = templateLoader;
    }

    @Transactional
    public StartResult start(UUID environmentId, String startedBy) {
        if (!config.enabledEffective()) {
            throw new IllegalStateException("Profile-Assistant deaktiviert.");
        }
        if (startedBy == null || startedBy.isBlank()) {
            throw new IllegalArgumentException("startedBy darf nicht leer sein.");
        }
        Environment env = environmentRepo.findById(environmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Umgebung nicht gefunden: " + environmentId));
        ProfileAssistSession session = sessionRepo.save(ProfileAssistSession.builder()
                .environment(env)
                .startedBy(startedBy)
                .status("ACTIVE")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(config.sessionTtl()))
                .build());
        String frage = naechsteFrage(env, session);
        session.setPendingQuestion(frage);
        sessionRepo.save(session);
        return new StartResult(session.getId(), frage, session.getExpiresAt());
    }

    @Transactional
    public ReplyResult reply(UUID sessionId, String fieldPath, String answer) {
        ProfileAssistSession session = loadLiveSession(sessionId);
        List<Map<String, Object>> dialog = parseDialog(session.getDialogJson());
        Map<String, Object> eintrag = new LinkedHashMap<>();
        eintrag.put("fieldPath", fieldPath);
        eintrag.put("answer", answer);
        eintrag.put("at", Instant.now().toString());
        dialog.add(eintrag);
        session.setDialogJson(serialize(dialog));
        String naechste = naechsteFrage(session.getEnvironment(), session);
        session.setPendingQuestion(naechste);
        sessionRepo.save(session);
        return new ReplyResult(session.getId(), naechste,
                naechste == null || naechste.isBlank());
    }

    @Transactional
    public FinalizeResult finalizeDraft(UUID sessionId, String proposedBy) {
        ProfileAssistSession session = loadLiveSession(sessionId);
        List<Map<String, Object>> dialog = parseDialog(session.getDialogJson());
        String yaml = buildYaml(session.getEnvironment(), dialog);
        // ContextProfileService.proposeNewVersion => regulaerer DRAFT-Workflow.
        ProfileView draft = profileService.proposeNewVersion(
                session.getEnvironment().getId(), yaml, proposedBy);
        session.setStatus("FINALIZED");
        session.setPendingQuestion(null);
        sessionRepo.save(session);
        log.info("ProfileAssist {} finalisiert -> Draft {}", sessionId, draft.id());
        return new FinalizeResult(sessionId, draft.id(), draft.versionNumber());
    }

    private ProfileAssistSession loadLiveSession(UUID sessionId) {
        ProfileAssistSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Session nicht gefunden: " + sessionId));
        if (!"ACTIVE".equals(session.getStatus())) {
            throw new IllegalStateException(
                    "Session " + sessionId + " ist " + session.getStatus());
        }
        if (session.getExpiresAt() != null
                && Instant.now().isAfter(session.getExpiresAt())) {
            session.setStatus("EXPIRED");
            sessionRepo.save(session);
            throw new IllegalStateException(
                    "Session " + sessionId + " ist abgelaufen.");
        }
        return session;
    }

    private String naechsteFrage(Environment env, ProfileAssistSession session) {
        if (!config.enabledEffective()) {
            return null;
        }
        Map<String, Object> vars = new HashMap<>();
        vars.put("profil", "(aktuelles Profil hier gekuerzt)");
        vars.put("relevantFields", topFelder());
        vars.put("dialog", session.getDialogJson());

        PromptTemplate t = templateLoader.load(TEMPLATE_ID);
        try {
            LlmClient client = clientSelector.select(env.getId(), USE_CASE);
            LlmRequest req = new LlmRequest(
                    USE_CASE, t.id(), t.version(),
                    t.renderSystem(Map.of()),
                    List.of(new Message(Message.Role.USER, t.renderUser(vars))),
                    null, 0.2, 512, env.getId(), session.getStartedBy(),
                    null, Map.of());
            LlmResponse res = auditService.execute(client, req);
            JsonNode out = res.structuredOutput();
            return out == null ? null : out.path("question").asText("");
        } catch (RuntimeException ex) {
            log.warn("Profil-Assistent-LLM fehlgeschlagen: {}", ex.getMessage());
            return null;
        }
    }

    private String topFelder() {
        Map<String, Integer> counts = new HashMap<>();
        assessmentRepo.findAll().stream()
                .limit(100)
                .forEach(a -> {
                    if (a.getRationaleSourceFields() != null) {
                        for (String f : a.getRationaleSourceFields()) {
                            counts.merge(f, 1, Integer::sum);
                        }
                    }
                });
        return counts.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(
                        Map.Entry::getValue).reversed())
                .limit(10)
                .map(e -> e.getKey() + " (" + e.getValue() + ")")
                .collect(Collectors.joining(", "));
    }

    private List<Map<String, Object>> parseDialog(String json) {
        try {
            return MAPPER.readValue(json, new TypeReference<>() { });
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    private String serialize(List<Map<String, Object>> dialog) {
        try {
            return MAPPER.writeValueAsString(dialog);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private String buildYaml(Environment env, List<Map<String, Object>> dialog) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Generiert vom Profil-Assistenten (Draft)\n");
        sb.append("environment: ").append(env.getKey()).append('\n');
        sb.append("generated_from:\n");
        sb.append("  session_at: ").append(Instant.now()).append('\n');
        sb.append("answers:\n");
        for (Map<String, Object> eintrag : dialog) {
            sb.append("  - path: ").append(eintrag.getOrDefault("fieldPath", "")).append('\n');
            sb.append("    value: ").append(eintrag.getOrDefault("answer", "")).append('\n');
        }
        return sb.toString();
    }

    public record StartResult(UUID sessionId, String question, Instant expiresAt) {}
    public record ReplyResult(UUID sessionId, String nextQuestion, boolean done) {}
    public record FinalizeResult(UUID sessionId, UUID draftProfileId, int versionNumber) {}
}
