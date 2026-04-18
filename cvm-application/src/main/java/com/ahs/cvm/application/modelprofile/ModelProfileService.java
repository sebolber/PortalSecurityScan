package com.ahs.cvm.application.modelprofile;

import com.ahs.cvm.persistence.environment.Environment;
import com.ahs.cvm.persistence.environment.EnvironmentRepository;
import com.ahs.cvm.persistence.modelprofile.LlmModelProfile;
import com.ahs.cvm.persistence.modelprofile.LlmModelProfileRepository;
import com.ahs.cvm.persistence.modelprofile.ModelProfileChangeLog;
import com.ahs.cvm.persistence.modelprofile.ModelProfileChangeLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
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

    public static class VierAugenViolationException extends RuntimeException {
        public VierAugenViolationException(String message) {
            super(message);
        }
    }
}
