package com.ahs.cvm.llm.cost;

import static org.assertj.core.api.Assertions.assertThat;

import com.ahs.cvm.llm.LlmClient.TokenUsage;
import com.ahs.cvm.llm.cost.LlmCostCalculator.PricingProperties;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LlmCostCalculatorTest {

    @Test
    @DisplayName("CostCalculator: berechnet Preis aus Preistabelle")
    void kostenBerechnung() {
        PricingProperties props = new PricingProperties();
        props.setPricing(Map.of(
                "claude-sonnet-4-6",
                Map.of("prompt", "0.003", "completion", "0.015")));
        LlmCostCalculator calc = new LlmCostCalculator(props);

        BigDecimal cost = calc.calculate("claude-sonnet-4-6", new TokenUsage(1000, 200));

        // 1000 * 0.003 / 1000 = 0.003; 200 * 0.015 / 1000 = 0.003; Summe 0.006000
        assertThat(cost).isEqualByComparingTo("0.006000");
    }

    @Test
    @DisplayName("CostCalculator: fehlende Preistabelle -> 0")
    void keineTabelle() {
        LlmCostCalculator calc = new LlmCostCalculator(new PricingProperties());
        BigDecimal cost = calc.calculate("unbekannt", new TokenUsage(100, 100));
        assertThat(cost).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("CostCalculator: null-Tokens -> 0 ohne NPE")
    void nullTokens() {
        PricingProperties props = new PricingProperties();
        props.setPricing(Map.of("m", Map.of("prompt", "0.01", "completion", "0.02")));
        LlmCostCalculator calc = new LlmCostCalculator(props);
        BigDecimal cost = calc.calculate("m", new TokenUsage(null, null));
        assertThat(cost).isEqualByComparingTo("0");
    }
}
