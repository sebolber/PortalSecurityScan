package com.ahs.cvm.ai.executive;

import java.util.List;

/**
 * LLM-Zusammenfassung fuer den Executive-Report (Iteration 19,
 * CVM-50). Harte Limits: max 5 Bullets, je max 140 Zeichen, Headline
 * max 80 Zeichen; {@link Validator} kuerzt bzw. wirft bei Verletzung.
 */
public record ExecutiveSummary(
        String headline,
        String ampel,
        List<String> bullets) {

    public static final int MAX_BULLETS = 5;
    public static final int MAX_BULLET_CHARS = 140;
    public static final int MAX_HEADLINE_CHARS = 80;

    public ExecutiveSummary {
        bullets = bullets == null ? List.of() : List.copyOf(bullets);
    }

    public static class Validator {
        public ExecutiveSummary enforce(ExecutiveSummary raw) {
            String head = raw.headline() == null ? "" : raw.headline();
            if (head.length() > MAX_HEADLINE_CHARS) {
                head = head.substring(0, MAX_HEADLINE_CHARS);
            }
            String ampel = raw.ampel();
            if (ampel == null
                    || !(ampel.equals("GREEN") || ampel.equals("YELLOW")
                            || ampel.equals("RED"))) {
                ampel = "YELLOW";
            }
            List<String> bullets = raw.bullets().stream()
                    .limit(MAX_BULLETS)
                    .map(b -> b == null ? ""
                            : b.length() > MAX_BULLET_CHARS
                                    ? b.substring(0, MAX_BULLET_CHARS)
                                    : b)
                    .toList();
            return new ExecutiveSummary(head, ampel, bullets);
        }
    }
}
