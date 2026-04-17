package com.ahs.cvm.application.profile;

import com.ahs.cvm.domain.enums.ProfileState;
import com.ahs.cvm.persistence.environment.Environment;
import com.ahs.cvm.persistence.environment.EnvironmentRepository;
import com.ahs.cvm.persistence.profile.ContextProfile;
import com.ahs.cvm.persistence.profile.ContextProfileRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anwendungsservice fuer Kontextprofile.
 *
 * <ul>
 *   <li>{@link #latestActiveFor(UUID)} &ndash; aktuell aktive Version einer Umgebung.</li>
 *   <li>{@link #proposeNewVersion(UUID, String, String)} &ndash; legt DRAFT an.</li>
 *   <li>{@link #approve(UUID, String)} &ndash; Vier-Augen-Freigabe, alte Version
 *       wird {@code SUPERSEDED}, neue Version {@code ACTIVE},
 *       {@link ContextProfileActivatedEvent} wird publiziert.</li>
 *   <li>{@link #diff(UUID, UUID)} &ndash; typisierter Feld-Diff zweier Versionen.</li>
 * </ul>
 */
@Service
public class ContextProfileService {

    private static final Logger log = LoggerFactory.getLogger(ContextProfileService.class);

    private final ContextProfileRepository profileRepository;
    private final EnvironmentRepository environmentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ContextProfileYamlParser yamlParser;
    private final ProfileDiffBuilder diffBuilder;

    public ContextProfileService(
            ContextProfileRepository profileRepository,
            EnvironmentRepository environmentRepository,
            ApplicationEventPublisher eventPublisher,
            ContextProfileYamlParser yamlParser,
            ProfileDiffBuilder diffBuilder) {
        this.profileRepository = profileRepository;
        this.environmentRepository = environmentRepository;
        this.eventPublisher = eventPublisher;
        this.yamlParser = yamlParser;
        this.diffBuilder = diffBuilder;
    }

    @Transactional(readOnly = true)
    public Optional<ProfileView> latestActiveFor(UUID environmentId) {
        return profileRepository
                .findFirstByEnvironmentIdAndStateOrderByVersionNumberDesc(
                        environmentId, ProfileState.ACTIVE)
                .map(ProfileView::from);
    }

    @Transactional
    public ProfileView proposeNewVersion(
            UUID environmentId, String yamlSource, String proposedBy) {
        if (proposedBy == null || proposedBy.isBlank()) {
            throw new IllegalArgumentException("proposedBy darf nicht leer sein.");
        }
        Environment environment = environmentRepository
                .findById(environmentId)
                .orElseThrow(() -> new ProfileNotFoundException(
                        "Umgebung " + environmentId + " nicht gefunden."));

        ParsedProfile parsed = yamlParser.parse(yamlSource);

        int neueVersion = profileRepository
                .findFirstByEnvironmentIdOrderByVersionNumberDesc(environmentId)
                .map(ContextProfile::getVersionNumber)
                .map(v -> v + 1)
                .orElse(1);

        ContextProfile draft = ContextProfile.builder()
                .environment(environment)
                .versionNumber(neueVersion)
                .validFrom(Instant.now())
                .yamlSource(parsed.yamlSource())
                .proposedBy(proposedBy)
                .state(ProfileState.DRAFT)
                .needsReview(Boolean.FALSE)
                .build();

        log.info(
                "Profil-Draft angelegt: env={}, version={}, by={}",
                environmentId,
                neueVersion,
                proposedBy);
        return ProfileView.from(profileRepository.save(draft));
    }

    @Transactional
    public ProfileView approve(UUID profileVersionId, String approverId) {
        if (approverId == null || approverId.isBlank()) {
            throw new IllegalArgumentException("approverId darf nicht leer sein.");
        }
        ContextProfile draft = profileRepository
                .findById(profileVersionId)
                .orElseThrow(() -> new ProfileNotFoundException(profileVersionId));
        if (draft.getState() != ProfileState.DRAFT) {
            throw new IllegalStateException(
                    "Profil " + profileVersionId + " ist nicht im Status DRAFT (ist "
                            + draft.getState() + ").");
        }
        if (Objects.equals(approverId, draft.getProposedBy())) {
            throw new FourEyesViolationException(
                    "Vier-Augen-Prinzip verletzt: Approver '"
                            + approverId
                            + "' ist identisch mit dem Autor.");
        }

        UUID environmentId = draft.getEnvironment().getId();
        Optional<ContextProfile> bisher = profileRepository
                .findFirstByEnvironmentIdAndStateOrderByVersionNumberDesc(
                        environmentId, ProfileState.ACTIVE);

        Set<String> geaendertePfade;
        Instant jetzt = Instant.now();

        if (bisher.isPresent()) {
            ContextProfile alt = bisher.get();
            geaendertePfade = diffPfade(alt.getYamlSource(), draft.getYamlSource());
            alt.setState(ProfileState.SUPERSEDED);
            alt.setSupersededAt(jetzt);
            profileRepository.save(alt);
        } else {
            geaendertePfade = diffPfade(null, draft.getYamlSource());
        }

        draft.setState(ProfileState.ACTIVE);
        draft.setApprovedBy(approverId);
        draft.setApprovedAt(jetzt);
        draft.setValidFrom(jetzt);
        ContextProfile gespeichert = profileRepository.save(draft);

        eventPublisher.publishEvent(new ContextProfileActivatedEvent(
                environmentId,
                gespeichert.getId(),
                gespeichert.getVersionNumber(),
                geaendertePfade));

        log.info(
                "Profil aktiviert: env={}, version={}, pfade={}",
                environmentId,
                gespeichert.getVersionNumber(),
                geaendertePfade.size());

        return ProfileView.from(gespeichert);
    }

    @Transactional(readOnly = true)
    public Optional<UUID> environmentOf(UUID profileVersionId) {
        return profileRepository
                .findById(profileVersionId)
                .map(p -> p.getEnvironment().getId());
    }

    @Transactional(readOnly = true)
    public List<ProfileFieldDiff> diff(UUID oldVersionId, UUID newVersionId) {
        ContextProfile alt = profileRepository
                .findById(oldVersionId)
                .orElseThrow(() -> new ProfileNotFoundException(oldVersionId));
        ContextProfile neu = profileRepository
                .findById(newVersionId)
                .orElseThrow(() -> new ProfileNotFoundException(newVersionId));
        return diffBuilder.diff(
                yamlParser.parse(alt.getYamlSource()).tree(),
                yamlParser.parse(neu.getYamlSource()).tree());
    }

    private Set<String> diffPfade(String altYaml, String neuYaml) {
        var neuTree = yamlParser.parse(neuYaml).tree();
        var altTree = altYaml == null ? null : yamlParser.parse(altYaml).tree();
        return diffBuilder.diff(altTree, neuTree).stream()
                .map(ProfileFieldDiff::path)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }
}
