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
 * The Breadcrumbs' and the Breadcrumb Item's pre-migration Tailwind class strings, read from {@code /parity/breadcrumbs/breadcrumbs-legacy.html}
 * (the one source of truth for what "legacy" means), and the rewrite that turns migrated Breadcrumbs markup back into the markup the
 * components rendered before.
 *
 * <p>Every migrated class value is one fixed string, so the rewrite is a plain lookup from the class value of an element to its legacy classes.
 * It fails loudly when an {@code ash-breadcrumb} class value is not one of the known parts, rather than leaving some of it migrated.
 * Everything else on the page is left exactly as served.
 */
final class BreadcrumbsLegacyMarkup {

    static final List<String> ROLES = List.of("list", "item", "separator", "link", "text", "current", "hover-swatch");

    /** The migrated class value of each part, exactly as the templates write it, and the legacy role it stands for. */
    static final Map<String, String> MIGRATED = Map.of(
            "ash-breadcrumbs__list", "list",
            "ash-breadcrumb-item", "item",
            "ash-breadcrumb-item__separator", "separator",
            "ash-breadcrumb-item__link", "link",
            "ash-breadcrumb-item__text", "text",
            "ash-breadcrumb-item__text ash-breadcrumb-item__text--current", "current");

    private static final Pattern LEGACY = Pattern.compile("data-legacy-role=\"([a-z-]+)\" class=\"([^\"]+)\"");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("class=\"([^\"]*)\"");
    /** The landmark's class: the legacy landmark carried none, so the rewrite removes it. */
    private static final Pattern NAV_CLASS = Pattern.compile("(<nav\\b[^>]*?) class=\"ash-breadcrumbs\"");
    private static final Pattern BREADCRUMB_TOKEN = Pattern.compile("(?<![\\w-])ash-breadcrumb");

    private final Map<String, String> legacyByRole = new LinkedHashMap<>();

    BreadcrumbsLegacyMarkup() throws IOException {
        String html;
        try (InputStream in = BreadcrumbsLegacyMarkup.class.getResourceAsStream("/parity/breadcrumbs/breadcrumbs-legacy.html")) {
            if (in == null) {
                throw new IOException("missing /parity/breadcrumbs/breadcrumbs-legacy.html");
            }
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = LEGACY.matcher(html);
        while (m.find()) {
            if (legacyByRole.put(m.group(1), m.group(2)) != null) {
                throw new IllegalStateException("breadcrumbs-legacy.html defines " + m.group(1) + " twice");
            }
        }
        if (!legacyByRole.keySet().equals(Set.copyOf(ROLES))) {
            throw new IllegalStateException("breadcrumbs-legacy.html must define exactly " + ROLES + ", found " + legacyByRole.keySet());
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

    /** How many migrated Breadcrumbs lists {@code html} contains. */
    static int count(String html) {
        Matcher m = CLASS_ATTRIBUTE.matcher(html);
        int n = 0;
        while (m.find()) {
            if (m.group(1).trim().equals("ash-breadcrumbs")) {
                n++;
            }
        }
        return n;
    }

    /** Rewrites every migrated Breadcrumbs list and Breadcrumb Item in {@code html} to its legacy classes. Everything else is untouched. */
    String rewrite(String source) {
        String html = NAV_CLASS.matcher(source).replaceAll("$1");
        Matcher m = CLASS_ATTRIBUTE.matcher(html);
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        while (m.find()) {
            String value = m.group(1);
            if (!BREADCRUMB_TOKEN.matcher(value).find()) {
                continue;
            }
            String role = MIGRATED.get(value);
            if (role == null) {
                throw new IllegalStateException("an ash-breadcrumb class value that is not a known Breadcrumbs or Breadcrumb Item part: " + value);
            }
            out.append(html, cursor, m.start()).append("class=\"").append(classes(role)).append('"');
            cursor = m.end();
        }
        return out.append(html.substring(cursor)).toString();
    }
}
