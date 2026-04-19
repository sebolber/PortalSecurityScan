package com.ahs.cvm.persistence.rule;

import com.ahs.cvm.domain.enums.RuleStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleRepository extends JpaRepository<Rule, UUID> {

    List<Rule> findByStatusOrderByCreatedAtDesc(RuleStatus status);

    Optional<Rule> findByRuleKey(String ruleKey);

    /**
     * Iteration 50 (CVM-100): nur aktive (nicht soft-geloeschte) Regeln -
     * Eingabe der Regel-Engine.
     */
    List<Rule> findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(RuleStatus status);

    /**
     * Iteration 50 (CVM-100): alle nicht soft-geloeschten Regeln fuer die
     * Admin-Liste.
     */
    List<Rule> findByDeletedAtIsNullOrderByCreatedAtDesc();
}
