package com.ahs.cvm.application.parameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.application.tenant.TenantContext;
import com.ahs.cvm.persistence.parameter.SystemParameter;
import com.ahs.cvm.persistence.parameter.SystemParameterRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SystemParameterResolverTest {

    private SystemParameterRepository parameterRepository;
    private SystemParameterResolver resolver;
    private UUID tenantId;

    private SystemParameterSecretCipher cipher;

    @BeforeEach
    void setUp() {
        parameterRepository = mock(SystemParameterRepository.class);
        cipher = new SystemParameterSecretCipher("resolver-test-key");
        resolver = new SystemParameterResolver(parameterRepository, cipher);
        tenantId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Ohne Tenant-Kontext liefert resolve leeres Optional und die Convenience-Methoden den Fallback")
    void ohne_tenant_fallback() {
        assertThat(resolver.resolve("cvm.x")).isEmpty();
        assertThat(resolver.resolveString("cvm.x", "default")).isEqualTo("default");
        assertThat(resolver.resolveBoolean("cvm.x", true)).isTrue();
        assertThat(resolver.resolveInt("cvm.x", 42)).isEqualTo(42);
        assertThat(resolver.resolveDouble("cvm.x", 0.5d)).isEqualTo(0.5d);
    }

    @Test
    @DisplayName("Kein Eintrag in der DB: Fallback wird zurueckgegeben")
    void kein_eintrag_fallback() {
        TenantContext.set(tenantId);
        given(parameterRepository.findByTenantIdAndParamKey(tenantId, "cvm.x"))
                .willReturn(Optional.empty());
        assertThat(resolver.resolveString("cvm.x", "default")).isEqualTo("default");
    }

    @Test
    @DisplayName("Vorhandener Eintrag: Wert wird typisiert zurueckgegeben")
    void eintrag_typisiert() {
        TenantContext.set(tenantId);
        given(parameterRepository.findByTenantIdAndParamKey(tenantId, "cvm.bool"))
                .willReturn(Optional.of(parameter("cvm.bool", "true")));
        given(parameterRepository.findByTenantIdAndParamKey(tenantId, "cvm.int"))
                .willReturn(Optional.of(parameter("cvm.int", "7")));
        given(parameterRepository.findByTenantIdAndParamKey(tenantId, "cvm.double"))
                .willReturn(Optional.of(parameter("cvm.double", "0.25")));
        given(parameterRepository.findByTenantIdAndParamKey(tenantId, "cvm.str"))
                .willReturn(Optional.of(parameter("cvm.str", "abc")));

        assertThat(resolver.resolveBoolean("cvm.bool", false)).isTrue();
        assertThat(resolver.resolveInt("cvm.int", 1)).isEqualTo(7);
        assertThat(resolver.resolveDouble("cvm.double", 1.0)).isEqualTo(0.25);
        assertThat(resolver.resolveString("cvm.str", "x")).isEqualTo("abc");
    }

    @Test
    @DisplayName("Leerer Wert in der DB wird als nicht-gesetzt interpretiert und der Fallback genommen")
    void leerer_wert_fallback() {
        TenantContext.set(tenantId);
        given(parameterRepository.findByTenantIdAndParamKey(tenantId, "cvm.x"))
                .willReturn(Optional.of(parameter("cvm.x", "   ")));
        assertThat(resolver.resolveString("cvm.x", "default")).isEqualTo("default");
    }

    @Test
    @DisplayName("Ungueltige Zahl in der DB: Fallback wird genommen, keine Exception")
    void ungueltige_zahl_fallback() {
        TenantContext.set(tenantId);
        given(parameterRepository.findByTenantIdAndParamKey(tenantId, "cvm.int"))
                .willReturn(Optional.of(parameter("cvm.int", "not-a-number")));
        assertThat(resolver.resolveInt("cvm.int", 42)).isEqualTo(42);
    }

    @Test
    @DisplayName("Sensitive Parameter werden vor der Rueckgabe entschluesselt")
    void sensitive_wird_entschluesselt() {
        TenantContext.set(tenantId);
        SystemParameter secret = SystemParameter.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .paramKey("cvm.llm.claude.api-key")
                .label("Key")
                .category("AI_LLM")
                .type(com.ahs.cvm.domain.enums.SystemParameterType.PASSWORD)
                .sensitive(true)
                .value(cipher.encrypt("sk-geheim"))
                .build();
        given(parameterRepository.findByTenantIdAndParamKey(tenantId, "cvm.llm.claude.api-key"))
                .willReturn(Optional.of(secret));

        assertThat(resolver.resolve("cvm.llm.claude.api-key")).contains("sk-geheim");
    }

    @Test
    @DisplayName("Boolean akzeptiert true/1/yes case-insensitive, andere Werte fuehren zu false (Fallback egal)")
    void boolean_parsing() {
        TenantContext.set(tenantId);
        given(parameterRepository.findByTenantIdAndParamKey(tenantId, "cvm.a"))
                .willReturn(Optional.of(parameter("cvm.a", "YES")));
        given(parameterRepository.findByTenantIdAndParamKey(tenantId, "cvm.b"))
                .willReturn(Optional.of(parameter("cvm.b", "1")));
        given(parameterRepository.findByTenantIdAndParamKey(tenantId, "cvm.c"))
                .willReturn(Optional.of(parameter("cvm.c", "no")));

        assertThat(resolver.resolveBoolean("cvm.a", false)).isTrue();
        assertThat(resolver.resolveBoolean("cvm.b", false)).isTrue();
        assertThat(resolver.resolveBoolean("cvm.c", true)).isFalse();
    }

    private SystemParameter parameter(String key, String value) {
        return SystemParameter.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .paramKey(key)
                .label(key)
                .category("TEST")
                .type(com.ahs.cvm.domain.enums.SystemParameterType.STRING)
                .value(value)
                .build();
    }
}
