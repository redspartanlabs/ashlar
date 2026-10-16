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
 * The Empty State's pre-migration Tailwind class strings, read from {@code /parity/empty-state/empty-state-legacy.html}
 * (the one source of truth for what "legacy" means), and the rewrite that turns migrated Empty State markup back into
 * the markup the component rendered before.
 *
 * <p>The rewrite only touches the {@code class} attribute of an Empty State's own elements: each migrated class value
 * is replaced by the legacy class value of the same role. Everything else on the page, including the caller's body and
 * action content and the Icon's other attributes, is left exactly as served. It fails loudly when an
 * {@code ash-empty-state} class is not one of the known roles, rather than leaving some of it migrated.
 */
final class EmptyStateLegacyMarkup {

    static final List<String> ROLES = List.of("wrapper", "icon", "glyph", "title", "body", "action");

    /** The migrated class value of each role, exactly as the component renders it (the glyph's is the Icon's own classes plus the one passed in). */
    static final Map<String, String> MIGRATED = Map.of(
            "wrapper", "ash-empty-state",
            "icon", "ash-empty-state__icon",
            "glyph", "inline-block shrink-0 ash-empty-state__glyph",
            "title", "ash-empty-state__title",
            "body", "ash-empty-state__body",
            "action", "ash-empty-state__action");

    private static final Pattern LEGACY = Pattern.compile("<(?:div|svg|h3) data-legacy-role=\"([a-z-]+)\" class=\"([^\"]+)\"");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("class=\"([^\"]*)\"");
    private static final Pattern WRAPPER = Pattern.compile("<div class=\"ash-empty-state\" data-empty-state>");

    private final Map<String, String> legacyByRole = new LinkedHashMap<>();

    EmptyStateLegacyMarkup() throws IOException {
        String html;
        try (InputStream in = EmptyStateLegacyMarkup.class.getResourceAsStream("/parity/empty-state/empty-state-legacy.html")) {
            if (in == null) {
                throw new IOException("missing /parity/empty-state/empty-state-legacy.html");
            }
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = LEGACY.matcher(html);
        while (m.find()) {
            if (legacyByRole.put(m.group(1), m.group(2)) != null) {
                throw new IllegalStateException("empty-state-legacy.html defines " + m.group(1) + " twice");
            }
        }
        if (!legacyByRole.keySet().equals(Set.copyOf(ROLES))) {
            throw new IllegalStateException("empty-state-legacy.html must define exactly " + ROLES + ", found " + legacyByRole.keySet());
        }
    }

    /** The legacy classes of a role. */
    String classes(String role) {
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

    /** The configuration the in-page probe needs to flip an Empty State to its legacy classes: migrated class value to legacy class value. */
    String mapJson() {
        return ROLES.stream().map(role -> "\"" + MIGRATED.get(role) + "\":\"" + classes(role) + "\"")
                .collect(Collectors.joining(",", "{", "}"));
    }

    /** How many migrated Empty State components {@code html} contains. */
    static int count(String html) {
        Matcher m = WRAPPER.matcher(html);
        int n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }

    /** Rewrites every migrated Empty State in {@code html} to its legacy classes. Everything else is untouched. */
    String rewrite(String html) {
        Map<String, String> legacyByMigrated = new LinkedHashMap<>();
        MIGRATED.forEach((role, migrated) -> legacyByMigrated.put(migrated, classes(role)));
        Matcher m = CLASS_ATTRIBUTE.matcher(html);
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        while (m.find()) {
            String value = m.group(1);
            if (!Pattern.compile("\\bash-empty-state").matcher(value).find()) {
                continue;
            }
            String legacy = legacyByMigrated.get(value);
            if (legacy == null) {
                throw new IllegalStateException("an ash-empty-state class value that is not a known Empty State role: " + value);
            }
            out.append(html, cursor, m.start()).append("class=\"").append(legacy).append('"');
            cursor = m.end();
        }
        return out.append(html.substring(cursor)).toString();
    }
}
