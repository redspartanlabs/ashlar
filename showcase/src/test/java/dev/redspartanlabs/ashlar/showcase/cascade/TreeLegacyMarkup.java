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
 * The pre-migration Tailwind class strings of Tree and Tree Item (migrated together), read from {@code /parity/tree/tree-legacy.html} (the one source of truth for what
 * "legacy" means), and the rewrite that turns migrated markup back into the markup the templates rendered before.
 *
 * <p>Three legacy parts depended on state or on the element, so the rewrite reads them from the markup it is given. A chevron had {@code rotate-90} while the
 * {@code aria-expanded} of ITS OWN item was {@code "true"}: the chevron sits in the item's row, before any item nested under it, so the item tag read last when a chevron
 * is met is its own, however deep the tree is. A panel carried an inline {@code grid-template-rows} of {@code 0fr} while it had the {@code inert} attribute and {@code 1fr}
 * otherwise. A label was an {@code <a>} with one string and a {@code <span>} with another. Everything else on the page (the {@code data-*} hooks, the ARIA attributes, the
 * caller's content, the Icon inside a chevron or an item) is left exactly as served. It fails loudly when a class value that carries one of the two components' classes is
 * not a known part.
 */
final class TreeLegacyMarkup {

    /** The parts that had Tailwind classes. */
    static final List<String> ROLES = List.of("tree", "tree-item", "tree-row", "tree-toggle", "tree-chevron", "tree-chevron-open", "tree-spacer", "tree-icon", "tree-label-link", "tree-label",
            "tree-panel", "tree-clip", "tree-group");

    /** The migrated class value of each part, exactly as the templates write it (a chevron has two legacy strings and one migrated class; a label has two legacy strings). */
    static final Map<String, String> MIGRATED = Map.ofEntries(
            Map.entry("tree", "ash-tree"),
            Map.entry("tree-item", "ash-tree__item"),
            Map.entry("tree-row", "ash-tree__row"),
            Map.entry("tree-toggle", "ash-tree__toggle"),
            Map.entry("tree-chevron", "ash-tree__chevron"),
            Map.entry("tree-spacer", "ash-tree__spacer"),
            Map.entry("tree-icon", "ash-tree__icon"),
            Map.entry("tree-label", "ash-tree__label"),
            Map.entry("tree-panel", "ash-tree__panel"),
            Map.entry("tree-clip", "ash-tree__clip"),
            Map.entry("tree-group", "ash-tree__group"));

    private static final Pattern LEGACY = Pattern.compile("data-legacy-role=\"([a-z-]+)\" class=\"([^\"]+)\"");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("class=\"([^\"]*)\"");
    private static final Pattern OWN_TOKEN = Pattern.compile("(?<![\\w-])ash-tree(?![a-zA-Z0-9-])(?:__[a-z]+)?");
    private static final Pattern TAG = Pattern.compile("<([a-z0-9]+)\\b[^>]*>");
    private static final Pattern INERT = Pattern.compile("\\sinert(?=[\\s=>/])");

    private final Map<String, String> legacyByRole = new LinkedHashMap<>();

    TreeLegacyMarkup() throws IOException {
        String html;
        try (InputStream in = TreeLegacyMarkup.class.getResourceAsStream("/parity/tree/tree-legacy.html")) {
            if (in == null) {
                throw new IOException("missing /parity/tree/tree-legacy.html");
            }
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = LEGACY.matcher(html);
        while (m.find()) {
            if (legacyByRole.put(m.group(1), m.group(2)) != null) {
                throw new IllegalStateException("tree-legacy.html defines " + m.group(1) + " twice");
            }
        }
        if (!legacyByRole.keySet().equals(Set.copyOf(ROLES))) {
            throw new IllegalStateException("tree-legacy.html must define exactly " + ROLES + ", found " + legacyByRole.keySet());
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

    /** Rewrites every migrated Tree and Tree Item part in {@code html} to its legacy classes. Everything else is untouched. */
    String rewrite(String html) {
        Map<String, String> roleByMigrated = new LinkedHashMap<>();
        MIGRATED.forEach((role, migrated) -> roleByMigrated.put(migrated, role));

        Matcher tags = TAG.matcher(html);
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        boolean lastItemExpanded = false;
        while (tags.find()) {
            String tag = tags.group();
            Matcher cls = CLASS_ATTRIBUTE.matcher(tag);
            if (!cls.find()) {
                continue;
            }
            String value = cls.group(1).trim();
            if (!OWN_TOKEN.matcher(value).find()) {
                continue;
            }
            String role = roleByMigrated.get(value);
            if (role == null) {
                throw new IllegalStateException("a class value that carries a Tree or Tree Item class but is not a known part: " + value);
            }
            String attribute = "class=\"" + cls.group(1) + "\"";
            String rewritten;
            switch (role) {
                case "tree-item" -> {
                    lastItemExpanded = tag.contains("aria-expanded=\"true\"");
                    rewritten = tag.replace(attribute, "class=\"" + classes(role) + "\"");
                }
                case "tree-chevron" -> rewritten = tag.replace(attribute, "class=\"" + classes(lastItemExpanded ? "tree-chevron-open" : "tree-chevron") + "\"");
                case "tree-label" -> rewritten = tag.replace(attribute, "class=\"" + classes(tags.group(1).equals("a") ? "tree-label-link" : "tree-label") + "\"");
                case "tree-panel" -> {
                    String rows = INERT.matcher(tag).find() ? "0fr" : "1fr";
                    rewritten = tag.replace(attribute, "style=\"grid-template-rows: " + rows + "\" class=\"" + classes(role) + "\"");
                }
                default -> rewritten = tag.replace(attribute, "class=\"" + classes(role) + "\"");
            }
            out.append(html, cursor, tags.start()).append(rewritten);
            cursor = tags.end();
        }
        return out.append(html.substring(cursor)).toString();
    }
}
