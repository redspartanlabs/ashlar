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
 * The pre-migration Tailwind class strings of the four page composition utilities migrated together (Page Shell, Page Header, Page
 * Section and Footer), read from {@code /parity/page-composition/page-composition-legacy.html} (the one source of truth for what "legacy"
 * means), and the rewrite that turns migrated markup back into the markup the templates rendered before.
 *
 * <p>The rewrite replaces each migrated class value with the legacy class value of the same part. Everything else on the page, including
 * the caller's projected content, is left exactly as served. It fails loudly when a class value that carries one of the four utilities'
 * classes is not one of the known parts, rather than leaving some of it migrated.
 */
final class PageCompositionLegacyMarkup {

    static final List<String> ROLES = List.of("page-shell", "page-header", "page-header-heading", "page-header-title", "page-header-description",
            "page-header-actions", "page-section", "page-section-title", "page-section-description", "footer", "footer-inner");

    /** The migrated class value of each part, exactly as the templates write it. */
    static final Map<String, String> MIGRATED = Map.ofEntries(
            Map.entry("page-shell", "ash-page-shell"),
            Map.entry("page-header", "ash-page-header"),
            Map.entry("page-header-heading", "ash-page-header__heading"),
            Map.entry("page-header-title", "ash-page-header__title"),
            Map.entry("page-header-description", "ash-page-header__description"),
            Map.entry("page-header-actions", "ash-page-header__actions"),
            Map.entry("page-section", "ash-page-section"),
            Map.entry("page-section-title", "ash-page-section__title"),
            Map.entry("page-section-description", "ash-page-section__description"),
            Map.entry("footer", "ash-footer"),
            Map.entry("footer-inner", "ash-footer__inner"));

    private static final Pattern LEGACY = Pattern.compile("data-legacy-role=\"([a-z-]+)\" class=\"([^\"]+)\"");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("class=\"([^\"]*)\"");
    private static final Pattern OWN_TOKEN = Pattern.compile("(?<![\\w-])ash-(?:page-shell|page-header|page-section|footer)");

    private final Map<String, String> legacyByRole = new LinkedHashMap<>();

    PageCompositionLegacyMarkup() throws IOException {
        String html;
        try (InputStream in = PageCompositionLegacyMarkup.class.getResourceAsStream("/parity/page-composition/page-composition-legacy.html")) {
            if (in == null) {
                throw new IOException("missing /parity/page-composition/page-composition-legacy.html");
            }
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = LEGACY.matcher(html);
        while (m.find()) {
            if (legacyByRole.put(m.group(1), m.group(2)) != null) {
                throw new IllegalStateException("page-composition-legacy.html defines " + m.group(1) + " twice");
            }
        }
        if (!legacyByRole.keySet().equals(Set.copyOf(ROLES))) {
            throw new IllegalStateException("page-composition-legacy.html must define exactly " + ROLES + ", found " + legacyByRole.keySet());
        }
    }

    /** The legacy classes of a part. */
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

    /** Rewrites every migrated Page Shell, Page Header, Page Section and Footer in {@code html} to its legacy classes. Everything else is untouched. */
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
                throw new IllegalStateException("a class value that carries a Page Shell, Page Header, Page Section or Footer class but is not a known part: " + value);
            }
            out.append(html, cursor, m.start()).append("class=\"").append(legacy).append('"');
            cursor = m.end();
        }
        return out.append(html.substring(cursor)).toString();
    }
}
