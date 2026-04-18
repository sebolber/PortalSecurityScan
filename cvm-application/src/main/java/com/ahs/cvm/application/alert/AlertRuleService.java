package com.ahs.cvm.application.alert;

import com.ahs.cvm.domain.enums.AlertSeverity;
import com.ahs.cvm.domain.enums.AlertTriggerArt;
import com.ahs.cvm.persistence.alert.AlertRule;
import com.ahs.cvm.persistence.alert.AlertRuleRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD-Service fuer Alert-Regeln. Validiert minimal: Empfaenger und
 * Template-Name muessen gesetzt sein.
 */
@Service
public class AlertRuleService {

    private final AlertRuleRepository repository;

    public AlertRuleService(AlertRuleRepository repository) {
        this.repository = repository;
    }

    public List<AlertRuleView> findeAlle() {
        return repository.findAllByOrderByNameAsc().stream()
                .map(AlertRuleView::from)
                .toList();
    }

    @Transactional
    public AlertRuleView anlegen(CreateRuleCommand command) {
        if (command.recipients() == null || command.recipients().isEmpty()) {
            throw new IllegalArgumentException("Mindestens ein Empfaenger erforderlich");
        }
        if (command.templateName() == null || command.templateName().isBlank()) {
            throw new IllegalArgumentException("templateName erforderlich");
        }
        AlertRule rule = AlertRule.builder()
                .name(command.name())
                .description(command.description())
                .triggerArt(command.triggerArt())
                .severity(command.severity() == null ? AlertSeverity.WARNING : command.severity())
                .cooldownMinutes(command.cooldownMinutes() == null ? 60 : command.cooldownMinutes())
                .subjectPrefix(command.subjectPrefix() == null ? "[CVM]" : command.subjectPrefix())
                .templateName(command.templateName())
                .recipients(List.copyOf(command.recipients()))
                .conditionJson(command.conditionJson())
                .enabled(command.enabled() == null ? Boolean.TRUE : command.enabled())
                .build();
        AlertRule gespeichert = repository.save(rule);
        return AlertRuleView.from(gespeichert);
    }

    public record CreateRuleCommand(
            String name,
            String description,
            AlertTriggerArt triggerArt,
            AlertSeverity severity,
            Integer cooldownMinutes,
            String subjectPrefix,
            String templateName,
            List<String> recipients,
            String conditionJson,
            Boolean enabled) {}
}
