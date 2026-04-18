package com.ahs.cvm.llm.cost;

import com.ahs.cvm.llm.LlmClient.TokenUsage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Berechnet die Kosten eines Calls in Euro. Preise kommen ueber
 * {@link LlmCostCalculator.PricingProperties} aus {@code application.yaml}.
 * Struktur:
 *
 * <pre>
 * cvm:
 *   llm:
 *     pricing:
 *       claude-sonnet-4-6:
 *         prompt: 0.003
 *         completion: 0.015
 * </pre>
 *
 * <p>Werte sind Euro pro 1000 Tokens. Kein Eintrag => Kosten {@code 0}.
 */
@Component
public class LlmCostCalculator {

    private static final BigDecimal TAUSEND = new BigDecimal("1000");

    private final Map<String, ModelPricing> pricing = new ConcurrentHashMap<>();

    public LlmCostCalculator(PricingProperties properties) {
        if (properties != null && properties.getPricing() != null) {
            properties.getPricing().forEach((modelId, entries) ->
                    pricing.put(modelId, new ModelPricing(
                            toBig(entries.get("prompt")),
                            toBig(entries.get("completion")))));
        }
    }

    public BigDecimal calculate(String modelId, TokenUsage usage) {
        if (usage == null || modelId == null) {
            return BigDecimal.ZERO;
        }
        ModelPricing p = pricing.get(modelId);
        if (p == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal prompt = mul(p.promptEurPer1k(), usage.promptTokens());
        BigDecimal completion = mul(p.completionEurPer1k(), usage.completionTokens());
        return prompt.add(completion).setScale(6, RoundingMode.HALF_UP);
    }

    private static BigDecimal mul(BigDecimal perThousand, Integer tokens) {
        if (tokens == null || perThousand == null) {
            return BigDecimal.ZERO;
        }
        return perThousand.multiply(BigDecimal.valueOf(tokens))
                .divide(TAUSEND, 6, RoundingMode.HALF_UP);
    }

    private static BigDecimal toBig(Object o) {
        if (o == null) {
            return BigDecimal.ZERO;
        }
        if (o instanceof BigDecimal bd) {
            return bd;
        }
        if (o instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return new BigDecimal(o.toString());
    }

    private record ModelPricing(BigDecimal promptEurPer1k, BigDecimal completionEurPer1k) {}

    /**
     * Spring-ConfigurationProperties fuer die Preistabelle. Ein
     * minimaler Default-Bean wird in {@link LlmCostConfig} bereitgestellt.
     */
    @ConfigurationProperties(prefix = "cvm.llm")
    public static class PricingProperties {
        private Map<String, Map<String, Object>> pricing;

        public Map<String, Map<String, Object>> getPricing() {
            return pricing;
        }

        public void setPricing(Map<String, Map<String, Object>> pricing) {
            this.pricing = pricing;
        }
    }

    @Configuration
    @org.springframework.boot.context.properties.EnableConfigurationProperties(PricingProperties.class)
    public static class LlmCostConfig {

        @Autowired(required = false)
        PricingProperties properties;
    }
}
