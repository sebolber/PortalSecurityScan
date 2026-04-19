package com.ahs.cvm.ai.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ahs.cvm.domain.enums.AiCallStatus;
import com.ahs.cvm.llm.audit.AiCallAuditPort.AiCallAuditFinalization;
import com.ahs.cvm.llm.audit.AiCallAuditPort.AiCallAuditPending;
import com.ahs.cvm.persistence.ai.AiCallAudit;
import com.ahs.cvm.persistence.ai.AiCallAuditRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class JpaAiCallAuditAdapterTest {

    private final AiCallAuditRepository repository = mock(AiCallAuditRepository.class);
    private final JpaAiCallAuditAdapter adapter = new JpaAiCallAuditAdapter(repository);

    @Test
    @DisplayName("Adapter: persistPending legt PENDING-Eintrag an und liefert Id")
    void persistPending() {
        given(repository.save(any(AiCallAudit.class))).willAnswer(inv -> {
            AiCallAudit a = inv.getArgument(0);
            a.setId(UUID.fromString("aa000000-0000-0000-0000-000000000000"));
            return a;
        });

        UUID id = adapter.persistPending(new AiCallAuditPending(
                "auto-assessment",
                "claude-sonnet-4-6",
                null,
                "assessment.propose",
                "v1",
                "system",
                "user",
                null,
                "t.tester@ahs.test",
                UUID.fromString("00000000-0000-0000-0000-0000000000bb"),
                false,
                Instant.parse("2026-04-18T12:00:00Z")));

        assertThat(id).isNotNull();
        ArgumentCaptor<AiCallAudit> cap = ArgumentCaptor.forClass(AiCallAudit.class);
        verify(repository).save(cap.capture());
        AiCallAudit saved = cap.getValue();
        assertThat(saved.getStatus()).isEqualTo(AiCallStatus.PENDING);
        assertThat(saved.getUseCase()).isEqualTo("auto-assessment");
        assertThat(saved.getPromptTemplateVersion()).isEqualTo("v1");
        assertThat(saved.getEnvironment().getId())
                .isEqualTo(UUID.fromString("00000000-0000-0000-0000-0000000000bb"));
    }

    @Test
    @DisplayName("Adapter: finalize setzt Finalstatus und finalizingAllowed-Flag")
    void finalizeOk() {
        UUID id = UUID.randomUUID();
        AiCallAudit pending = AiCallAudit.builder()
                .id(id)
                .useCase("u")
                .modelId("m")
                .promptTemplateId("t")
                .promptTemplateVersion("v1")
                .systemPrompt("s")
                .userPrompt("u")
                .triggeredBy("x")
                .injectionRisk(false)
                .status(AiCallStatus.PENDING)
                .createdAt(Instant.now())
                .build();
        given(repository.findById(id)).willReturn(Optional.of(pending));
        given(repository.save(any(AiCallAudit.class))).willAnswer(inv -> inv.getArgument(0));

        adapter.finalizeAudit(id, new AiCallAuditFinalization(
                AiCallStatus.OK, "raw", 10, 5, 100,
                new BigDecimal("0.001"), null, null,
                Instant.parse("2026-04-18T12:00:01Z")));

        assertThat(pending.getStatus()).isEqualTo(AiCallStatus.OK);
        assertThat(pending.getRawResponse()).isEqualTo("raw");
        assertThat(pending.getLatencyMs()).isEqualTo(100);
        assertThat(pending.isFinalizingAllowed()).isTrue();
    }

    @Test
    @DisplayName("Adapter: finalize auf bereits finalisiertem Eintrag wirft")
    void doppelFinalize() {
        UUID id = UUID.randomUUID();
        AiCallAudit bereitsOk = AiCallAudit.builder()
                .id(id)
                .useCase("u")
                .modelId("m")
                .promptTemplateId("t")
                .promptTemplateVersion("v1")
                .systemPrompt("s")
                .userPrompt("u")
                .triggeredBy("x")
                .injectionRisk(false)
                .status(AiCallStatus.OK)
                .createdAt(Instant.now())
                .build();
        given(repository.findById(id)).willReturn(Optional.of(bereitsOk));

        assertThatThrownBy(() -> adapter.finalizeAudit(id,
                        new AiCallAuditFinalization(
                                AiCallStatus.OK, null, null, null, null,
                                BigDecimal.ZERO, null, null, Instant.now())))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Adapter: finalize auf unbekannter Id wirft")
    void unbekanntId() {
        UUID id = UUID.randomUUID();
        given(repository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.finalizeAudit(id,
                        new AiCallAuditFinalization(
                                AiCallStatus.OK, null, null, null, null,
                                BigDecimal.ZERO, null, null, Instant.now())))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
