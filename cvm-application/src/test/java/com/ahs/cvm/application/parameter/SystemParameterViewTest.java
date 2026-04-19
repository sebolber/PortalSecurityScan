package com.ahs.cvm.application.parameter;

import static org.assertj.core.api.Assertions.assertThat;

import com.ahs.cvm.domain.enums.SystemParameterType;
import com.ahs.cvm.persistence.parameter.SystemParameter;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SystemParameterViewTest {

    @Test
    @DisplayName("View-from: restartRequired wird aus dem Katalog uebernommen (cvm.llm.claude.version -> true)")
    void restart_required_aus_katalog() {
        SystemParameter entity = eintrag("cvm.llm.claude.version", "2023-06-01");
        SystemParameterView view = SystemParameterView.from(entity);
        assertThat(view.restartRequired()).isTrue();
    }

    @Test
    @DisplayName("View-from: restartRequired=false fuer Keys, die nicht im Katalog markiert sind (cvm.ai.reachability.enabled)")
    void nicht_markiert_false() {
        SystemParameter entity = eintrag("cvm.ai.reachability.enabled", "false");
        SystemParameterView view = SystemParameterView.from(entity);
        assertThat(view.restartRequired()).isFalse();
    }

    @Test
    @DisplayName("Unbekannte Keys (nicht im Katalog) liefern restartRequired=false")
    void unbekannter_key() {
        SystemParameter entity = eintrag("cvm.custom.unbekannt", "x");
        SystemParameterView view = SystemParameterView.from(entity);
        assertThat(view.restartRequired()).isFalse();
    }

    @Test
    @DisplayName("Sensitive Werte bleiben in der View maskiert")
    void sensitive_wert_maskiert() {
        SystemParameter entity = eintrag("cvm.custom.secret", "super-geheim");
        entity.setSensitive(true);
        SystemParameterView view = SystemParameterView.from(entity);
        assertThat(view.value()).isEqualTo("***");
    }

    private SystemParameter eintrag(String key, String value) {
        return SystemParameter.builder()
                .id(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .paramKey(key)
                .label(key)
                .category("TEST")
                .type(SystemParameterType.STRING)
                .value(value)
                .build();
    }
}
