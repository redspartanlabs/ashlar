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
 * The pre-migration Tailwind class strings of Faceted Search, Facet and Facet Option, read from {@code /parity/facet/facet-legacy.html} (the one source of truth for what "legacy"
 * means), and the rewrite that turns migrated markup back into the markup the templates rendered before.
 *
 * <p>Two things the templates used to choose are read from the markup the rewrite is given. An option's row was either the enabled string or the disabled one, chosen from the
 * {@code disabled} parameter; the rewrite reads that from the {@code ash-facet-option--disabled} modifier. Its checkbox ended with {@code cursor-pointer} only when the option was not
 * disabled; the rewrite reads that from the checkbox's own {@code disabled} attribute. The announced count ({@code ash-sr-only}) is rewritten only inside a Facet Option's row: the same
 * class elsewhere on a page belongs to another component and is left exactly as served. Everything else on the page is left alone, and an unknown Facets class fails loudly rather
 * than leaving some of it migrated.
 *
 * <p>Markup that carries no Facets class at all (which is what the templates render until they are migrated) comes back unchanged.
 */
final class FacetLegacyMarkup {

    static final List<String> ROLES = List.of("faceted-search", "facet", "legend", "options", "row", "row-disabled", "input", "input-disabled", "label", "count", "announced-count");

    private static final Pattern LEGACY = Pattern.compile("data-legacy-role=\"([a-z-]+)\" class=\"([^\"]+)\"");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("class=\"([^\"]*)\"");
    private static final Pattern OWN_TOKEN = Pattern.compile("(?<![\\w-])ash-(?:faceted-search|facet)(?:-option)?(?:__[a-z-]+)?(?:--[a-z]+)?(?![a-zA-Z0-9_-])");
    private static final Pattern TAG = Pattern.compile("<([a-z0-9]+)\\b[^>]*>");
    private static final Pattern DISABLED = Pattern.compile("\\sdisabled(?:=\"[^\"]*\")?(?=[\\s>/])");

    private final Map<String, String> legacyByRole = new LinkedHashMap<>();

    FacetLegacyMarkup() throws IOException {
        String html;
        try (InputStream in = FacetLegacyMarkup.class.getResourceAsStream("/parity/facet/facet-legacy.html")) {
            if (in == null) {
                throw new IOException("missing /parity/facet/facet-legacy.html");
            }
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = LEGACY.matcher(html);
        while (m.find()) {
            if (legacyByRole.put(m.group(1), m.group(2)) != null) {
                throw new IllegalStateException("facet-legacy.html defines " + m.group(1) + " twice");
            }
        }
        if (!legacyByRole.keySet().equals(Set.copyOf(ROLES))) {
            throw new IllegalStateException("facet-legacy.html must define exactly " + ROLES + ", found " + legacyByRole.keySet());
        }
    }

    String classes(String role) {
        String found = legacyByRole.get(role);
        if (found == null) {
            throw new IllegalStateException("no legacy classes for " + role);
        }
        return found;
    }

    /** Every legacy class string, by role. */
    Map<String, String> all() {
        return Map.copyOf(legacyByRole);
    }

    /** Every legacy class token, for the oracle's non-vacuity check. */
    Set<String> allTokens() {
        return legacyByRole.values().stream().flatMap(s -> Arrays.stream(s.split("\\s+"))).collect(Collectors.toCollection(TreeSet::new));
    }

    /** True when the label that is open at {@code position} is a Facet Option's row (its class is a Facet Option class). */
    private static boolean insideOptionRow(String html, int position) {
        int open = html.lastIndexOf("<label", position);
        if (open < 0 || open < html.lastIndexOf("</label>", position)) {
            return false;
        }
        Matcher cls = CLASS_ATTRIBUTE.matcher(html.substring(open, html.indexOf('>', open) + 1));
        return cls.find() && Arrays.asList(cls.group(1).trim().split("\\s+")).contains("ash-facet-option");
    }

    /** Rewrites every migrated Faceted Search, Facet and Facet Option part in {@code html} to its legacy classes. Everything else is untouched. */
    String rewrite(String html) {
        Matcher tags = TAG.matcher(html);
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        while (tags.find()) {
            String tag = tags.group();
            Matcher cls = CLASS_ATTRIBUTE.matcher(tag);
            if (!cls.find()) {
                continue;
            }
            Set<String> tokens = Set.of(cls.group(1).trim().split("\\s+"));
            String legacy;
            if (tokens.equals(Set.of("ash-sr-only"))) {
                if (!insideOptionRow(html, tags.start())) {
                    continue;
                }
                legacy = classes("announced-count");
            } else if (!OWN_TOKEN.matcher(cls.group(1)).find()) {
                continue;
            } else if (tokens.equals(Set.of("ash-faceted-search"))) {
                legacy = classes("faceted-search");
            } else if (tokens.equals(Set.of("ash-facet"))) {
                legacy = classes("facet");
            } else if (tokens.equals(Set.of("ash-facet__legend"))) {
                legacy = classes("legend");
            } else if (tokens.equals(Set.of("ash-facet__options"))) {
                legacy = classes("options");
            } else if (tokens.equals(Set.of("ash-facet-option"))) {
                legacy = classes("row");
            } else if (tokens.equals(Set.of("ash-facet-option", "ash-facet-option--disabled"))) {
                legacy = classes("row-disabled");
            } else if (tokens.equals(Set.of("ash-facet-option__input"))) {
                legacy = classes(DISABLED.matcher(tag).find() ? "input-disabled" : "input");
            } else if (tokens.equals(Set.of("ash-facet-option__label"))) {
                legacy = classes("label");
            } else if (tokens.equals(Set.of("ash-facet-option__count"))) {
                legacy = classes("count");
            } else {
                throw new IllegalStateException("a class value that carries a Facets class but is not a known part: " + cls.group(1));
            }
            out.append(html, cursor, tags.start()).append(tag.replace("class=\"" + cls.group(1) + "\"", "class=\"" + legacy + "\""));
            cursor = tags.end();
        }
        return out.append(html.substring(cursor)).toString();
    }
}
