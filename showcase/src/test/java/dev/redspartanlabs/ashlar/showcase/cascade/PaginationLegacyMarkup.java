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
 * The Pagination's and the Pagination Item's pre-migration Tailwind class strings, read from {@code /parity/pagination/pagination-legacy.html}
 * (the one source of truth for what "legacy" means), and the rewrite that turns migrated Pagination markup back into the markup the
 * components rendered before.
 *
 * <p>Every migrated class value is one fixed string, so the rewrite is a plain lookup from the class value of an element to its legacy classes.
 * The landmark and the {@code <li>} carried no class before, so the rewrite removes theirs. It fails loudly when an {@code ash-pagination}
 * class value is not one of the known parts, rather than leaving some of it migrated. Everything else on the page is left exactly as served.
 */
final class PaginationLegacyMarkup {

    static final List<String> ROLES = List.of("list", "link", "current", "disabled", "hover-swatch", "hover-fill-swatch");

    /** The migrated class value of each part, exactly as the templates write it, and the legacy role it stands for. */
    static final Map<String, String> MIGRATED = Map.of(
            "ash-pagination__list", "list",
            "ash-pagination-item__control ash-pagination-item__control--link", "link",
            "ash-pagination-item__control ash-pagination-item__control--current", "current",
            "ash-pagination-item__control ash-pagination-item__control--disabled", "disabled");

    private static final Pattern LEGACY = Pattern.compile("data-legacy-role=\"([a-z-]+)\" class=\"([^\"]+)\"");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("class=\"([^\"]*)\"");
    /** The landmark's and the {@code <li>}'s class: the legacy ones carried none, so the rewrite removes them. */
    private static final Pattern NAV_CLASS = Pattern.compile("(<nav\\b[^>]*?) class=\"ash-pagination\"");
    private static final Pattern LI_CLASS = Pattern.compile("(<li\\b[^>]*?) class=\"ash-pagination-item\"");
    private static final Pattern PAGINATION_TOKEN = Pattern.compile("(?<![\\w-])ash-pagination");

    private final Map<String, String> legacyByRole = new LinkedHashMap<>();

    PaginationLegacyMarkup() throws IOException {
        String html;
        try (InputStream in = PaginationLegacyMarkup.class.getResourceAsStream("/parity/pagination/pagination-legacy.html")) {
            if (in == null) {
                throw new IOException("missing /parity/pagination/pagination-legacy.html");
            }
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = LEGACY.matcher(html);
        while (m.find()) {
            if (legacyByRole.put(m.group(1), m.group(2)) != null) {
                throw new IllegalStateException("pagination-legacy.html defines " + m.group(1) + " twice");
            }
        }
        if (!legacyByRole.keySet().equals(Set.copyOf(ROLES))) {
            throw new IllegalStateException("pagination-legacy.html must define exactly " + ROLES + ", found " + legacyByRole.keySet());
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

    /** How many migrated Pagination landmarks {@code html} contains. */
    static int count(String html) {
        Matcher m = CLASS_ATTRIBUTE.matcher(html);
        int n = 0;
        while (m.find()) {
            if (m.group(1).trim().equals("ash-pagination")) {
                n++;
            }
        }
        return n;
    }

    /** Rewrites every migrated Pagination and Pagination Item in {@code html} to its legacy classes. Everything else is untouched. */
    String rewrite(String source) {
        String html = LI_CLASS.matcher(NAV_CLASS.matcher(source).replaceAll("$1")).replaceAll("$1");
        Matcher m = CLASS_ATTRIBUTE.matcher(html);
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        while (m.find()) {
            String value = m.group(1);
            if (!PAGINATION_TOKEN.matcher(value).find()) {
                continue;
            }
            String role = MIGRATED.get(value);
            if (role == null) {
                throw new IllegalStateException("an ash-pagination class value that is not a known Pagination or Pagination Item part: " + value);
            }
            out.append(html, cursor, m.start()).append("class=\"").append(classes(role)).append('"');
            cursor = m.end();
        }
        return out.append(html.substring(cursor)).toString();
    }
}
