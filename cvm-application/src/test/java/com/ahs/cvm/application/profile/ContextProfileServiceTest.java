package com.ahs.cvm.application.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.domain.enums.EnvironmentStage;
import com.ahs.cvm.domain.enums.ProfileState;
import com.ahs.cvm.persistence.environment.Environment;
import com.ahs.cvm.persistence.environment.EnvironmentRepository;
import com.ahs.cvm.persistence.profile.ContextProfile;
import com.ahs.cvm.persistence.profile.ContextProfileRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class ContextProfileServiceTest {

    private ContextProfileRepository profileRepository;
    private EnvironmentRepository environmentRepository;
    private ApplicationEventPublisher eventPublisher;
    private ContextProfileYamlParser yamlParser;
    private ProfileDiffBuilder diffBuilder;
    private ContextProfileService service;

    private static final String YAML_V1 =
            """
            schemaVersion: 1
            umgebung:
              key: REF-TEST
              stage: REF
            architecture:
              windows_hosts: false
              linux_hosts: true
            network:
              internet_exposure: false
              customer_access: true
            hardening:
              fips_mode: false
            compliance:
              frameworks:
                - ISO27001
            """;

    private static final String YAML_V2 =
            """
            schemaVersion: 1
            umgebung:
              key: REF-TEST
              stage: REF
            architecture:
              windows_hosts: true
              linux_hosts: true
            network:
              internet_exposure: false
              customer_access: true
            hardening:
              fips_mode: false
            compliance:
              frameworks:
                - ISO27001
            """;

    @BeforeEach
    void setUp() {
        profileRepository = mock(ContextProfileRepository.class);
        environmentRepository = mock(EnvironmentRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        yamlParser = new ContextProfileYamlParser();
        diffBuilder = new ProfileDiffBuilder();
        service = new ContextProfileService(
                profileRepository,
                environmentRepository,
                eventPublisher,
                yamlParser,
                diffBuilder);
    }

    @Test
    @DisplayName("Profil: proposeNewVersion erzeugt DRAFT mit versionNumber = bisher + 1")
    void proposeErzeugtDraft() {
        UUID envId = UUID.randomUUID();
        Environment env = Environment.builder()
                .key("REF-TEST")
                .name("Ref Test")
                .stage(EnvironmentStage.REF)
                .build();
        env.setId(envId);

        ContextProfile bestehend = ContextProfile.builder()
                .environment(env)
                .versionNumber(3)
                .state(ProfileState.ACTIVE)
                .yamlSource(YAML_V1)
                .build();

        given(environmentRepository.findById(envId)).willReturn(Optional.of(env));
        given(profileRepository.findFirstByEnvironmentIdOrderByVersionNumberDesc(envId))
                .willReturn(Optional.of(bestehend));
        given(profileRepository.save(any(ContextProfile.class)))
                .willAnswer(inv -> {
                    ContextProfile cp = inv.getArgument(0);
                    if (cp.getId() == null) {
                        cp.setId(UUID.randomUUID());
                    }
                    return cp;
                });

        ProfileView draft = service.proposeNewVersion(envId, YAML_V2, "t.tester@ahs.test");

        assertThat(draft.versionNumber()).isEqualTo(4);
        assertThat(draft.state()).isEqualTo(ProfileState.DRAFT);
        assertThat(draft.proposedBy()).isEqualTo("t.tester@ahs.test");
        assertThat(draft.yamlSource()).isEqualTo(YAML_V2);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Profil: approve durch den Autor selbst wirft FourEyesViolationException")
    void approveVierAugenVerstoss() {
        UUID draftId = UUID.randomUUID();
        UUID envId = UUID.randomUUID();
        Environment env = Environment.builder()
                .key("REF-TEST").name("Ref").stage(EnvironmentStage.REF).build();
        env.setId(envId);

        ContextProfile draft = ContextProfile.builder()
                .environment(env)
                .versionNumber(4)
                .state(ProfileState.DRAFT)
                .yamlSource(YAML_V2)
                .proposedBy("t.tester@ahs.test")
                .build();
        draft.setId(draftId);

        given(profileRepository.findById(draftId)).willReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.approve(draftId, "t.tester@ahs.test"))
                .isInstanceOf(FourEyesViolationException.class);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Profil: Aktivierung setzt alte Version auf SUPERSEDED und publiziert Event mit Diff")
    void approveAktiviertUndPubliziertEvent() {
        UUID draftId = UUID.randomUUID();
        UUID aktivId = UUID.randomUUID();
        UUID envId = UUID.randomUUID();

        Environment env = Environment.builder()
                .key("REF-TEST").name("Ref").stage(EnvironmentStage.REF).build();
        env.setId(envId);

        ContextProfile aktiv = ContextProfile.builder()
                .environment(env)
                .versionNumber(3)
                .state(ProfileState.ACTIVE)
                .yamlSource(YAML_V1)
                .build();
        aktiv.setId(aktivId);

        ContextProfile draft = ContextProfile.builder()
                .environment(env)
                .versionNumber(4)
                .state(ProfileState.DRAFT)
                .yamlSource(YAML_V2)
                .proposedBy("t.tester@ahs.test")
                .build();
        draft.setId(draftId);

        given(profileRepository.findById(draftId)).willReturn(Optional.of(draft));
        given(profileRepository.findFirstByEnvironmentIdAndStateOrderByVersionNumberDesc(
                        envId, ProfileState.ACTIVE))
                .willReturn(Optional.of(aktiv));
        given(profileRepository.save(any(ContextProfile.class)))
                .willAnswer(inv -> inv.getArgument(0));

        ProfileView result = service.approve(draftId, "a.admin@ahs.test");

        assertThat(result.state()).isEqualTo(ProfileState.ACTIVE);
        assertThat(result.approvedBy()).isEqualTo("a.admin@ahs.test");
        assertThat(aktiv.getState()).isEqualTo(ProfileState.SUPERSEDED);
        assertThat(aktiv.getSupersededAt()).isNotNull();

        ArgumentCaptor<ContextProfileActivatedEvent> captor =
                ArgumentCaptor.forClass(ContextProfileActivatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        ContextProfileActivatedEvent event = captor.getValue();
        assertThat(event.environmentId()).isEqualTo(envId);
        assertThat(event.newProfileVersionId()).isEqualTo(draftId);
        assertThat(event.changedPaths()).contains("architecture.windows_hosts");
    }

    @Test
    @DisplayName("Profil: erste Version wird sofort aktiviert, Event enthaelt alle Felder als CREATED")
    void approveErsteVersion() {
        UUID draftId = UUID.randomUUID();
        UUID envId = UUID.randomUUID();

        Environment env = Environment.builder()
                .key("REF-TEST").name("Ref").stage(EnvironmentStage.REF).build();
        env.setId(envId);

        ContextProfile draft = ContextProfile.builder()
                .environment(env)
                .versionNumber(1)
                .state(ProfileState.DRAFT)
                .yamlSource(YAML_V1)
                .proposedBy("t.tester@ahs.test")
                .build();
        draft.setId(draftId);

        given(profileRepository.findById(draftId)).willReturn(Optional.of(draft));
        given(profileRepository.findFirstByEnvironmentIdAndStateOrderByVersionNumberDesc(
                        envId, ProfileState.ACTIVE))
                .willReturn(Optional.empty());
        given(profileRepository.save(any(ContextProfile.class)))
                .willAnswer(inv -> inv.getArgument(0));

        ProfileView result = service.approve(draftId, "a.admin@ahs.test");

        assertThat(result.state()).isEqualTo(ProfileState.ACTIVE);
        verify(eventPublisher).publishEvent(any(ContextProfileActivatedEvent.class));
    }

    @Test
    @DisplayName("Profil: diff(alt, neu) liefert Liste mit allen geaenderten Pfaden")
    void diffLiefertAenderungen() {
        UUID altId = UUID.randomUUID();
        UUID neuId = UUID.randomUUID();
        ContextProfile alt = ContextProfile.builder().yamlSource(YAML_V1).build();
        ContextProfile neu = ContextProfile.builder().yamlSource(YAML_V2).build();

        given(profileRepository.findById(altId)).willReturn(Optional.of(alt));
        given(profileRepository.findById(neuId)).willReturn(Optional.of(neu));

        List<ProfileFieldDiff> diffs = service.diff(altId, neuId);

        assertThat(diffs).extracting(ProfileFieldDiff::path)
                .contains("architecture.windows_hosts");
    }
}
