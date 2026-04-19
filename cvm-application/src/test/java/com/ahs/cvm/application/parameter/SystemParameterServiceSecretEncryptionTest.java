package com.ahs.cvm.application.parameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.application.tenant.TenantContext;
import com.ahs.cvm.domain.enums.SystemParameterType;
import com.ahs.cvm.persistence.parameter.SystemParameter;
import com.ahs.cvm.persistence.parameter.SystemParameterAuditLog;
import com.ahs.cvm.persistence.parameter.SystemParameterAuditLogRepository;
import com.ahs.cvm.persistence.parameter.SystemParameterRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SystemParameterServiceSecretEncryptionTest {

    private SystemParameterRepository parameterRepository;
    private SystemParameterAuditLogRepository auditRepository;
    private SystemParameterSecretCipher cipher;
    private SystemParameterService service;
    private UUID tenantId;
    private List<SystemParameter> saved;
    private List<SystemParameterAuditLog> audits;

    @BeforeEach
    void setUp() {
        parameterRepository = mock(SystemParameterRepository.class);
        auditRepository = mock(SystemParameterAuditLogRepository.class);
        cipher = new SystemParameterSecretCipher("iteration-45-test-key");
        service = new SystemParameterService(
                parameterRepository, auditRepository,
                new SystemParameterValidator(), cipher);
        tenantId = UUID.randomUUID();
        TenantContext.set(tenantId);
        saved = new ArrayList<>();
        audits = new ArrayList<>();
        given(parameterRepository.save(any(SystemParameter.class))).willAnswer(inv -> {
            SystemParameter p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(UUID.randomUUID());
            }
            saved.add(p);
            return p;
        });
        given(auditRepository.save(any(SystemParameterAuditLog.class))).willAnswer(inv -> {
            SystemParameterAuditLog a = inv.getArgument(0);
            audits.add(a);
            return a;
        });
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Create: sensitive-Wert wird verschluesselt in die DB geschrieben, View bleibt maskiert")
    void create_verschluesselt_sensitive() {
        given(parameterRepository.findByTenantIdAndParamKey(tenantId, "cvm.llm.claude.api-key"))
                .willReturn(Optional.empty());

        SystemParameterView view = service.create(new SystemParameterCommands.CreateCommand(
                "cvm.llm.claude.api-key", "Anthropic Key", null, null,
                "AI_LLM", "claude", SystemParameterType.PASSWORD,
                "sk-ant-geheim-123", null, false, null, null, null,
                true, false, null, null, true), "admin");

        assertThat(saved).hasSize(1);
        String storedValue = saved.get(0).getValue();
        assertThat(storedValue).startsWith(SystemParameterSecretCipher.ENC_PREFIX);
        assertThat(storedValue).doesNotContain("sk-ant-geheim-123");
        assertThat(cipher.decrypt(storedValue)).isEqualTo("sk-ant-geheim-123");

        // View ist maskiert (alter Masking-Code in SystemParameterView)
        assertThat(view.value()).isEqualTo("***");

        // Audit-Log traegt den Klartext-Alt-/Neu-Wert nur maskiert (analog zum bestehenden Verhalten)
        assertThat(audits).hasSize(1);
        assertThat(audits.get(0).getNewValue()).isEqualTo("***");
    }

    @Test
    @DisplayName("ChangeValue: verschluesselter Alt-Wert wird vor dem Audit-Vergleich entschluesselt, neuer Wert erneut verschluesselt")
    void change_value_entschluesselt_alt_wert() {
        SystemParameter bestehend = SystemParameter.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .paramKey("cvm.llm.claude.api-key")
                .label("Key")
                .category("AI_LLM")
                .type(SystemParameterType.PASSWORD)
                .sensitive(true)
                .value(cipher.encrypt("alt-geheim"))
                .build();
        given(parameterRepository.findById(bestehend.getId())).willReturn(Optional.of(bestehend));

        service.changeValue(bestehend.getId(),
                new SystemParameterCommands.ChangeValueCommand("neu-geheim", "Rotation"),
                "admin");

        assertThat(saved).hasSize(1);
        String storedValue = saved.get(0).getValue();
        assertThat(storedValue).startsWith(SystemParameterSecretCipher.ENC_PREFIX);
        assertThat(cipher.decrypt(storedValue)).isEqualTo("neu-geheim");

        // Audit: alt/neu maskiert, aber Reason durchgereicht
        assertThat(audits).hasSize(1);
        assertThat(audits.get(0).getReason()).isEqualTo("Rotation");
        assertThat(audits.get(0).getOldValue()).isEqualTo("***");
        assertThat(audits.get(0).getNewValue()).isEqualTo("***");
    }

    @Test
    @DisplayName("Nicht-sensitive Werte bleiben unveraendert (kein enc:-Praefix)")
    void nicht_sensitive_unveraendert() {
        given(parameterRepository.findByTenantIdAndParamKey(tenantId, "cvm.scheduler.enabled"))
                .willReturn(Optional.empty());

        service.create(new SystemParameterCommands.CreateCommand(
                "cvm.scheduler.enabled", "Scheduler", null, null,
                "SCHEDULER", "global", SystemParameterType.BOOLEAN,
                "true", "true", true, null, null, null,
                false, true, null, null, true), "admin");

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getValue()).isEqualTo("true");
    }

    @Test
    @DisplayName("Reset eines sensiblen Parameters: Default-Wert (null) wird als null gespeichert")
    void reset_sensitive_null() {
        SystemParameter bestehend = SystemParameter.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .paramKey("cvm.feed.nvd.api-key")
                .label("Key")
                .category("ENRICHMENT")
                .type(SystemParameterType.PASSWORD)
                .sensitive(true)
                .value(cipher.encrypt("alt"))
                .defaultValue(null)
                .build();
        given(parameterRepository.findById(bestehend.getId())).willReturn(Optional.of(bestehend));

        service.reset(bestehend.getId(), "admin");

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getValue()).isNull();
    }
}
