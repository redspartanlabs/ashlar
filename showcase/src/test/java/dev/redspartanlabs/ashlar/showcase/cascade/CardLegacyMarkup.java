package dev.redspartanlabs.ashlar.showcase.cascade;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The pre-migration Tailwind class strings of Card and Stat Card (migrated together), read from {@code /parity/card/card-legacy.html}
 * (the one source of truth for what "legacy" means), and the rewrite that turns migrated markup back into the markup the templates
 * rendered before.
 *
 * <p>The rewrite replaces each migrated class value with the legacy class value of the same part. The one part with no legacy class is the
 * Stat Card's {@code <dl>}, which had none: its whole class attribute is removed. Everything else on the page, including the caller's
 * projected content and the {@code data-card}, {@code data-card-header}, {@code data-card-footer} and {@code data-stat-card} attributes
 * (which the legacy rendering carries too, so the skins select the same hooks on both sides), is left exactly as served. It fails loudly
 * when a class value that carries one of the two components' classes is not one of the known parts, rather than leaving some of it migrated.
 */
final class CardLegacyMarkup {

    /** The parts that had Tailwind classes. */
    static final List<String> ROLES = List.of("card", "card-header", "card-title", "card-body", "card-footer", "stat-card", "stat-card-label",
            "stat-card-values", "stat-card-value", "stat-card-change", "stat-card-description");

    /** The Stat Card's {@code <dl>}: a migrated part that had no class before. */
    static final String LIST_ROLE = "stat-card-list";

    /** The migrated class value of each part, exactly as the templates write it. */
    static final Map<String, String> MIGRATED = Map.ofEntries(
            Map.entry("card", "ash-card"),
            Map.entry("card-header", "ash-card__header"),
            Map.entry("card-title", "ash-card__title"),
            Map.entry("card-body", "ash-card__body"),
            Map.entry("card-footer", "ash-card__footer"),
            Map.entry("stat-card", "ash-stat-card"),
            Map.entry(LIST_ROLE, "ash-stat-card__list"),
            Map.entry("stat-card-label", "ash-stat-card__label"),
            Map.entry("stat-card-values", "ash-stat-card__values"),
            Map.entry("stat-card-value", "ash-stat-card__value"),
            Map.entry("stat-card-change", "ash-stat-card__change"),
            Map.entry("stat-card-description", "ash-stat-card__description"));

    private static final Pattern LEGACY = Pattern.compile("data-legacy-role=\"([a-z-]+)\" class=\"([^\"]+)\"");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("class=\"([^\"]*)\"");
    private static final Pattern OWN_TOKEN = Pattern.compile("(?<![\\w-])ash-(?:stat-)?card(?![a-zA-Z0-9])");

    private final Map<String, String> legacyByRole = new LinkedHashMap<>();

    CardLegacyMarkup() throws IOException {
        String html;
        try (InputStream in = CardLegacyMarkup.class.getResourceAsStream("/parity/card/card-legacy.html")) {
            if (in == null) {
                throw new IOException("missing /parity/card/card-legacy.html");
            }
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = LEGACY.matcher(html);
        while (m.find()) {
            if (legacyByRole.put(m.group(1), m.group(2)) != null) {
                throw new IllegalStateException("card-legacy.html defines " + m.group(1) + " twice");
            }
        }
        if (!legacyByRole.keySet().equals(Set.copyOf(ROLES))) {
            throw new IllegalStateException("card-legacy.html must define exactly " + ROLES + ", found " + legacyByRole.keySet());
        }
    }

    /** The legacy classes of a part; the empty string for the Stat Card's list, which had none. */
    String classes(String role) {
        if (role.equals(LIST_ROLE)) {
            return "";
        }
        String found = legacyByRole.get(role);
        if (found == null) {
            throw new IllegalStateException("no legacy classes for " + role);
        }
        return found;
    }

    /** Every legacy class token, for the oracle's non-vacuity check. */
    Set<String> allTokens() {
        return legacyByRole.values().stream().flatMap(s -> Arrays.stream(s.split("\\s+"))).collect(Collectors.toCollection(TreeSet::new));
    }

    /** How many migrated parts of one kind {@code html} contains (a class attribute that is exactly the part's class). */
    static int count(String html, String role) {
        String migrated = MIGRATED.get(role);
        Matcher m = CLASS_ATTRIBUTE.matcher(html);
        int n = 0;
        while (m.find()) {
            if (m.group(1).trim().equals(migrated)) {
                n++;
            }
        }
        return n;
    }

    /** Rewrites every migrated Card and Stat Card in {@code html} to its legacy classes. Everything else is untouched. */
    String rewrite(String html) {
        Map<String, String> legacyByMigrated = new LinkedHashMap<>();
        MIGRATED.forEach((role, migrated) -> legacyByMigrated.put(migrated, classes(role)));
        Matcher m = CLASS_ATTRIBUTE.matcher(html);
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        while (m.find()) {
            String value = m.group(1);
            if (!OWN_TOKEN.matcher(value).find()) {
                continue;
            }
            String legacy = legacyByMigrated.get(value);
            if (legacy == null) {
                throw new IllegalStateException("a class value that carries a Card or Stat Card class but is not a known part: " + value);
            }
            if (legacy.isEmpty()) {
                // The part had no class: drop the attribute and the space before it.
                int start = m.start();
                while (start > cursor && html.charAt(start - 1) == ' ') {
                    start--;
                }
                out.append(html, cursor, start);
            } else {
                out.append(html, cursor, m.start()).append("class=\"").append(legacy).append('"');
            }
            cursor = m.end();
        }
        return out.append(html.substring(cursor)).toString();
    }
}
