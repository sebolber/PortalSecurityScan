package com.ahs.cvm.application.modelprofile;

import com.ahs.cvm.persistence.environment.Environment;
import com.ahs.cvm.persistence.environment.EnvironmentRepository;
import com.ahs.cvm.persistence.modelprofile.LlmModelProfile;
import com.ahs.cvm.persistence.modelprofile.LlmModelProfileRepository;
import com.ahs.cvm.persistence.modelprofile.ModelProfileChangeLog;
import com.ahs.cvm.persistence.modelprofile.ModelProfileChangeLogRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Wechselt das aktive {@link LlmModelProfile} einer Umgebung unter
 * Vier-Augen-Pflicht (Iteration 21, CVM-52).
 *
 * <p>Der Aufrufer (typischerweise ein Admin) gibt den Antragsteller
 * (fachlicher Antrag) und den Bestaetiger an. Sind beide identisch,
 * wirft der Service {@link VierAugenViolationException}. Jeder
 * Wechsel wird in {@code model_profile_change_log} auditierbar
 * festgehalten.
 */
@Service
public class ModelProfileService {

    private static final Logger log = LoggerFactory.getLogger(ModelProfileService.class);

    private static final Pattern PROFILE_KEY_PATTERN =
            Pattern.compile("^[A-Z0-9_]{2,64}$");

    private final EnvironmentRepository environmentRepository;
    private final LlmModelProfileRepository profileRepository;
    private final ModelProfileChangeLogRepository changeLogRepository;
    private final Clock clock;

    public ModelProfileService(
            EnvironmentRepository environmentRepository,
            LlmModelProfileRepository profileRepository,
            ModelProfileChangeLogRepository changeLogRepository,
            Clock clock) {
        this.environmentRepository = environmentRepository;
        this.profileRepository = profileRepository;
        this.changeLogRepository = changeLogRepository;
        this.clock = clock;
    }

    @Transactional
    public ModelProfileChangeView switchProfile(SwitchCommand cmd) {
        if (cmd == null) {
            throw new IllegalArgumentException("SwitchCommand darf nicht null sein.");
        }
        requireNotBlank(cmd.changedBy(), "changedBy");
        requireNotBlank(cmd.fourEyesConfirmer(), "fourEyesConfirmer");
        if (Objects.equals(cmd.changedBy(), cmd.fourEyesConfirmer())) {
            throw new VierAugenViolationException(
                    "Vier-Augen-Verstoss: changedBy == fourEyesConfirmer ("
                            + cmd.changedBy() + ")");
        }
        Environment env = environmentRepository.findById(cmd.environmentId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Environment nicht gefunden: " + cmd.environmentId()));
        LlmModelProfile profile = profileRepository.findById(cmd.newProfileId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "LlmModelProfile nicht gefunden: " + cmd.newProfileId()));

        UUID previous = env.getLlmModelProfileId();
        env.setLlmModelProfileId(profile.getId());
        environmentRepository.save(env);

        ModelProfileChangeLog entry = changeLogRepository.save(
                ModelProfileChangeLog.builder()
                        .environmentId(env.getId())
                        .previousProfileId(previous)
                        .newProfileId(profile.getId())
                        .changedBy(cmd.changedBy())
                        .fourEyesConfirmer(cmd.fourEyesConfirmer())
                        .reason(cmd.reason())
                        .changedAt(Instant.now(clock))
                        .build());
        log.info("Modell-Profil {} -> {} fuer Environment {} (Antrag: {}, "
                        + "Vier-Augen: {})", previous, profile.getId(), env.getId(),
                cmd.changedBy(), cmd.fourEyesConfirmer());
        return new ModelProfileChangeView(
                entry.getId(), env.getId(), previous, profile.getId(),
                entry.getChangedBy(), entry.getFourEyesConfirmer(),
                entry.getReason(), entry.getChangedAt());
    }

    /**
     * Legt ein neues LlmModelProfile an. Wenn das Profil fuer GKV-Daten
     * freigegeben werden soll ({@code approvedForGkvData=true}), ist ein
     * Vier-Augen-Freigeber zwingend; andernfalls wird auch bei abwesender
     * Zweitperson akzeptiert (reiner Sandbox-/Fallback-Fall), das
     * Vier-Augen-Tupel muss aber stets unterschiedlich sein.
     */
    @Transactional
    public ModelProfileView createProfile(CreateCommand cmd) {
        if (cmd == null) {
            throw new IllegalArgumentException("CreateCommand darf nicht null sein.");
        }
        requireNotBlank(cmd.profileKey(), "profileKey");
        requireNotBlank(cmd.modelId(), "modelId");
        requireNotBlank(cmd.approvedBy(), "approvedBy");
        requireNotBlank(cmd.provider(), "provider");
        LlmModelProfile.Provider provider;
        try {
            provider = LlmModelProfile.Provider.valueOf(cmd.provider().trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "provider ungueltig (erwartet CLAUDE_CLOUD|OLLAMA_ONPREM): "
                            + cmd.provider());
        }
        if (cmd.costBudgetEurMonthly() == null
                || cmd.costBudgetEurMonthly().signum() < 0) {
            throw new IllegalArgumentException(
                    "costBudgetEurMonthly muss >= 0 sein.");
        }
        String key = cmd.profileKey().trim();
        if (!PROFILE_KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException(
                    "profileKey ungueltig (erwartet ^[A-Z0-9_]{2,64}$): " + key);
        }
        if (profileRepository.findByProfileKey(key).isPresent()) {
            throw new ProfileKeyConflictException(key);
        }
        if (cmd.approvedForGkvData()) {
            requireNotBlank(cmd.fourEyesConfirmer(),
                    "fourEyesConfirmer (GKV-Freigabe erfordert Vier-Augen)");
        }
        if (cmd.fourEyesConfirmer() != null
                && !cmd.fourEyesConfirmer().isBlank()
                && Objects.equals(cmd.approvedBy(), cmd.fourEyesConfirmer())) {
            throw new VierAugenViolationException(
                    "Vier-Augen-Verstoss: approvedBy == fourEyesConfirmer ("
                            + cmd.approvedBy() + ")");
        }

        LlmModelProfile saved = profileRepository.save(LlmModelProfile.builder()
                .profileKey(key)
                .provider(provider)
                .modelId(cmd.modelId().trim())
                .modelVersion(cmd.modelVersion() == null || cmd.modelVersion().isBlank()
                        ? null
                        : cmd.modelVersion().trim())
                .costBudgetEurMonthly(cmd.costBudgetEurMonthly())
                .approvedForGkvData(cmd.approvedForGkvData())
                .build());

        changeLogRepository.save(ModelProfileChangeLog.builder()
                .environmentId(null)
                .previousProfileId(null)
                .newProfileId(saved.getId())
                .changedBy(cmd.approvedBy().trim())
                .fourEyesConfirmer(cmd.fourEyesConfirmer() == null
                        || cmd.fourEyesConfirmer().isBlank()
                        ? cmd.approvedBy().trim()
                        : cmd.fourEyesConfirmer().trim())
                .reason(cmd.reason())
                .action(ModelProfileChangeLog.Action.PROFILE_CREATED)
                .changedAt(Instant.now(clock))
                .build());

        log.info("Modell-Profil angelegt: {} ({}:{}), GKV-approved={}",
                saved.getProfileKey(), saved.getProvider(), saved.getModelId(),
                saved.isApprovedForGkvData());
        return ModelProfileView.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ModelProfileChangeView> historieFuerEnvironment(UUID environmentId) {
        return changeLogRepository.findByEnvironmentIdOrderByChangedAtDesc(environmentId)
                .stream().map(e -> new ModelProfileChangeView(
                        e.getId(), e.getEnvironmentId(),
                        e.getPreviousProfileId(), e.getNewProfileId(),
                        e.getChangedBy(), e.getFourEyesConfirmer(),
                        e.getReason(), e.getChangedAt())).toList();
    }

    private static void requireNotBlank(String v, String name) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException(name + " darf nicht leer sein.");
        }
    }

    public record SwitchCommand(
            UUID environmentId, UUID newProfileId,
            String changedBy, String fourEyesConfirmer, String reason) {}

    public record ModelProfileChangeView(
            UUID id, UUID environmentId, UUID previousProfileId,
            UUID newProfileId, String changedBy, String fourEyesConfirmer,
            String reason, Instant changedAt) {}

    public record CreateCommand(
            String profileKey,
            String provider,
            String modelId,
            String modelVersion,
            BigDecimal costBudgetEurMonthly,
            boolean approvedForGkvData,
            String approvedBy,
            String fourEyesConfirmer,
            String reason) {}

    public static class VierAugenViolationException extends RuntimeException {
        public VierAugenViolationException(String message) {
            super(message);
        }
    }

    public static class ProfileKeyConflictException extends RuntimeException {
        private final String profileKey;
        public ProfileKeyConflictException(String key) {
            super("profileKey '" + key + "' bereits vergeben.");
            this.profileKey = key;
        }
        public String getProfileKey() {
            return profileKey;
        }
    }
}
