package com.ahs.cvm.persistence.rule;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleDryRunResultRepository extends JpaRepository<RuleDryRunResult, UUID> {

    List<RuleDryRunResult> findByRuleIdOrderByExecutedAtDesc(UUID ruleId);
}
