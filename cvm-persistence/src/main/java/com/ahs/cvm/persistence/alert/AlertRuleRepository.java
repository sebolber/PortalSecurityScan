package com.ahs.cvm.persistence.alert;

import com.ahs.cvm.domain.enums.AlertTriggerArt;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRuleRepository extends JpaRepository<AlertRule, UUID> {

    List<AlertRule> findByEnabledTrueAndTriggerArt(AlertTriggerArt triggerArt);

    List<AlertRule> findAllByOrderByNameAsc();
}
