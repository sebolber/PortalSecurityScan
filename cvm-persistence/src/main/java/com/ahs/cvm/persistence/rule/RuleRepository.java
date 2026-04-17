package com.ahs.cvm.persistence.rule;

import com.ahs.cvm.domain.enums.RuleStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleRepository extends JpaRepository<Rule, UUID> {

    List<Rule> findByStatusOrderByCreatedAtDesc(RuleStatus status);

    Optional<Rule> findByRuleKey(String ruleKey);
}
