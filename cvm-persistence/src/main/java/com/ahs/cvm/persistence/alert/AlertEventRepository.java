package com.ahs.cvm.persistence.alert;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertEventRepository extends JpaRepository<AlertEvent, UUID> {

    Optional<AlertEvent> findByRuleIdAndTriggerKey(UUID ruleId, String triggerKey);
}
