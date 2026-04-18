package com.ahs.cvm.application.modelprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.application.modelprofile.ModelProfileService.ModelProfileChangeView;
import com.ahs.cvm.application.modelprofile.ModelProfileService.SwitchCommand;
import com.ahs.cvm.application.modelprofile.ModelProfileService.VierAugenViolationException;
import com.ahs.cvm.persistence.environment.Environment;
import com.ahs.cvm.persistence.environment.EnvironmentRepository;
import com.ahs.cvm.persistence.modelprofile.LlmModelProfile;
import com.ahs.cvm.persistence.modelprofile.LlmModelProfile.Provider;
import com.ahs.cvm.persistence.modelprofile.LlmModelProfileRepository;
import com.ahs.cvm.persistence.modelprofile.ModelProfileChangeLog;
import com.ahs.cvm.persistence.modelprofile.ModelProfileChangeLogRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ModelProfileServiceTest {

    private static final UUID ENV_ID = UUID.randomUUID();
    private static final UUID OLD_PROFILE = UUID.randomUUID();
    private static final UUID NEW_PROFILE = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-04-18T10:00:00Z");

    private EnvironmentRepository envRepo;
    private LlmModelProfileRepository profileRepo;
    private ModelProfileChangeLogRepository logRepo;
    private ModelProfileService service;

    @BeforeEach
    void setUp() {
        envRepo = mock(EnvironmentRepository.class);
        profileRepo = mock(LlmModelProfileRepository.class);
        logRepo = mock(ModelProfileChangeLogRepository.class);
        service = new ModelProfileService(envRepo, profileRepo, logRepo,
                Clock.fixed(NOW, ZoneOffset.UTC));

        Environment env = Environment.builder()
                .id(ENV_ID).key("PROD").name("PROD").llmModelProfileId(OLD_PROFILE)
                .build();
        LlmModelProfile p = LlmModelProfile.builder()
                .id(NEW_PROFILE).profileKey("k").provider(Provider.OLLAMA_ONPREM)
                .modelId("llama3").costBudgetEurMonthly(new BigDecimal("10")).build();
        given(envRepo.findById(ENV_ID)).willReturn(Optional.of(env));
        given(profileRepo.findById(NEW_PROFILE)).willReturn(Optional.of(p));
        given(logRepo.save(any(ModelProfileChangeLog.class)))
                .willAnswer(inv -> {
                    ModelProfileChangeLog l = inv.getArgument(0);
                    if (l.getId() == null) {
                        l.setId(UUID.randomUUID());
                    }
                    return l;
                });
    }

    @Test
    @DisplayName("Wechsel: gueltig -> Env aktualisiert, Log geschrieben")
    void gueltig() {
        ModelProfileChangeView v = service.switchProfile(new SwitchCommand(
                ENV_ID, NEW_PROFILE, "a.admin@ahs.test",
                "j.meyer@ahs.test", "Neues Budget"));

        assertThat(v.previousProfileId()).isEqualTo(OLD_PROFILE);
        assertThat(v.newProfileId()).isEqualTo(NEW_PROFILE);
        verify(logRepo).save(any(ModelProfileChangeLog.class));
        verify(envRepo).save(any(Environment.class));
    }

    @Test
    @DisplayName("Wechsel: changedBy == fourEyesConfirmer -> Vier-Augen-Verstoss")
    void vierAugenVerletzt() {
        assertThatThrownBy(() -> service.switchProfile(new SwitchCommand(
                ENV_ID, NEW_PROFILE, "x@y", "x@y", null)))
                .isInstanceOf(VierAugenViolationException.class);
    }

    @Test
    @DisplayName("Wechsel: unbekannte Environment -> IllegalArgument")
    void envFehlt() {
        given(envRepo.findById(ENV_ID)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.switchProfile(new SwitchCommand(
                ENV_ID, NEW_PROFILE, "a@x", "b@x", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Environment nicht gefunden");
    }

    @Test
    @DisplayName("Wechsel: unbekanntes Profil -> IllegalArgument")
    void profilFehlt() {
        given(profileRepo.findById(NEW_PROFILE)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.switchProfile(new SwitchCommand(
                ENV_ID, NEW_PROFILE, "a@x", "b@x", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LlmModelProfile");
    }

    @Test
    @DisplayName("Wechsel: leerer changedBy -> Fehler")
    void leererChangedBy() {
        assertThatThrownBy(() -> service.switchProfile(new SwitchCommand(
                ENV_ID, NEW_PROFILE, "", "b", null)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
