package com.ahs.cvm.application.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ahs.cvm.domain.enums.AhsSeverity;
import com.ahs.cvm.domain.enums.RuleOrigin;
import com.ahs.cvm.domain.enums.RuleStatus;
import com.ahs.cvm.persistence.rule.Rule;
import com.ahs.cvm.persistence.rule.RuleRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RuleServiceTest {

    private final RuleRepository ruleRepository = mock(RuleRepository.class);
    private final ConditionParser parser = new ConditionParser();
    private final RuleService service = new RuleService(ruleRepository, parser);

    @Test
    @DisplayName("Rule: Draft wird mit DRAFT-Status angelegt und Condition wird validiert")
    void draftWirdAngelegt() {
        given(ruleRepository.save(any(Rule.class))).willAnswer(inv -> inv.getArgument(0));

        RuleView draft = service.proposeRule(
                new RuleService.RuleDraftInput(
                        "nicht-windows",
                        "Nur Windows relevant",
                        null,
                        AhsSeverity.LOW,
                        "{\"eq\": {\"path\": \"profile.architecture.windows_hosts\", \"value\": false}}",
                        "CVE {cve.id} ist nur Windows-relevant",
                        java.util.List.of("profile.architecture.windows_hosts"),
                        RuleOrigin.MANUAL),
                "a.admin@ahs.test");

        assertThat(draft.status()).isEqualTo(RuleStatus.DRAFT);
        assertThat(draft.createdBy()).isEqualTo("a.admin@ahs.test");
        assertThat(draft.ruleKey()).isEqualTo("nicht-windows");
    }

    @Test
    @DisplayName("Rule: Ungueltiges Condition-JSON fuehrt zu RuleConditionException und keinem Save")
    void ungueltigesJson() {
        assertThatThrownBy(() -> service.proposeRule(
                new RuleService.RuleDraftInput(
                        "kaputt",
                        "x",
                        null,
                        AhsSeverity.LOW,
                        "{\"nichtOp\": {\"path\": \"cve.id\", \"value\": 1}}",
                        "x",
                        null,
                        RuleOrigin.MANUAL),
                "a.admin@ahs.test"))
                .isInstanceOf(RuleConditionException.class);
    }

    @Test
    @DisplayName("Rule: Aktivierung durch den Autor verletzt Vier-Augen-Prinzip")
    void aktivierungSelberAutor() {
        UUID id = UUID.randomUUID();
        Rule draft = Rule.builder()
                .id(id)
                .ruleKey("k")
                .name("n")
                .status(RuleStatus.DRAFT)
                .proposedSeverity(AhsSeverity.LOW)
                .conditionJson("{\"eq\": {\"path\": \"cve.kev\", \"value\": true}}")
                .rationaleTemplate("r")
                .createdBy("t.tester@ahs.test")
                .origin(RuleOrigin.MANUAL)
                .build();
        given(ruleRepository.findById(id)).willReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.activate(id, "t.tester@ahs.test"))
                .isInstanceOf(RuleFourEyesViolationException.class);
    }

    @Test
    @DisplayName("Rule: Aktivierung durch anderen User setzt ACTIVE und activated_by")
    void aktivierungErfolgreich() {
        UUID id = UUID.randomUUID();
        Rule draft = Rule.builder()
                .id(id)
                .ruleKey("k")
                .name("n")
                .status(RuleStatus.DRAFT)
                .proposedSeverity(AhsSeverity.LOW)
                .conditionJson("{\"eq\": {\"path\": \"cve.kev\", \"value\": true}}")
                .rationaleTemplate("r")
                .createdBy("t.tester@ahs.test")
                .origin(RuleOrigin.MANUAL)
                .build();
        given(ruleRepository.findById(id)).willReturn(Optional.of(draft));
        given(ruleRepository.save(any(Rule.class))).willAnswer(inv -> inv.getArgument(0));

        RuleView aktiv = service.activate(id, "a.admin@ahs.test");

        assertThat(aktiv.status()).isEqualTo(RuleStatus.ACTIVE);
        assertThat(aktiv.activatedBy()).isEqualTo("a.admin@ahs.test");
        assertThat(aktiv.activatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Rule: Aktivierung einer bereits aktiven Regel wirft IllegalStateException")
    void aktivierungUngueltig() {
        UUID id = UUID.randomUUID();
        Rule aktiv = Rule.builder()
                .id(id)
                .ruleKey("k")
                .name("n")
                .status(RuleStatus.ACTIVE)
                .proposedSeverity(AhsSeverity.LOW)
                .conditionJson("{\"eq\": {\"path\": \"cve.kev\", \"value\": true}}")
                .rationaleTemplate("r")
                .createdBy("t.tester@ahs.test")
                .origin(RuleOrigin.MANUAL)
                .build();
        given(ruleRepository.findById(id)).willReturn(Optional.of(aktiv));

        assertThatThrownBy(() -> service.activate(id, "a.admin@ahs.test"))
                .isInstanceOf(IllegalStateException.class);
    }
}
