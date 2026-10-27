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
 * The List and List Item's pre-migration Tailwind class strings, read from {@code /parity/list/list-legacy.html} (the one source of
 * truth for what "legacy" means), and the rewrite that turns migrated List and List Item markup back into the markup the components
 * rendered before.
 *
 * <p>The rewrite replaces each migrated class value with the legacy class value of the same part, and removes the class the
 * {@code <li>} did not have before. Everything else on the page, including the caller's projected content and the Icons' own markup
 * (which the Icon rewrite handles), is left exactly as served. It fails loudly when an {@code ash-list} class value is not one of the
 * known parts, rather than leaving some of it migrated.
 */
final class ListLegacyMarkup {

    static final List<String> ROLES = List.of("list", "row", "row-link", "icon", "body", "label", "description", "trailing", "chevron");

    /** The migrated class value of each part, exactly as the templates write it. */
    static final Map<String, String> MIGRATED = Map.of(
            "list", "ash-list",
            "row", "ash-list-item__row",
            "row-link", "ash-list-item__row ash-list-item__row--link",
            "icon", "ash-list-item__icon",
            "body", "ash-list-item__body",
            "label", "ash-list-item__label",
            "description", "ash-list-item__description",
            "trailing", "ash-list-item__trailing",
            "chevron", "ash-list-item__chevron");

    private static final Pattern LEGACY = Pattern.compile("data-legacy-role=\"([a-z-]+)\" class=\"([^\"]+)\"");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("class=\"([^\"]*)\"");
    private static final Pattern LIST_ITEM = Pattern.compile("<li class=\"ash-list-item\">");
    private static final Pattern LIST_TOKEN = Pattern.compile("(?<![\\w-])ash-list");

    private final Map<String, String> legacyByRole = new LinkedHashMap<>();

    ListLegacyMarkup() throws IOException {
        String html;
        try (InputStream in = ListLegacyMarkup.class.getResourceAsStream("/parity/list/list-legacy.html")) {
            if (in == null) {
                throw new IOException("missing /parity/list/list-legacy.html");
            }
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = LEGACY.matcher(html);
        while (m.find()) {
            if (legacyByRole.put(m.group(1), m.group(2)) != null) {
                throw new IllegalStateException("list-legacy.html defines " + m.group(1) + " twice");
            }
        }
        java.util.ArrayList<String> required = new java.util.ArrayList<>(ROLES);
        required.add("hover-swatch");
        if (!legacyByRole.keySet().equals(Set.copyOf(required))) {
            throw new IllegalStateException("list-legacy.html must define exactly " + required + ", found " + legacyByRole.keySet());
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

    /** How many migrated Lists {@code html} contains. */
    static int count(String html) {
        Matcher m = CLASS_ATTRIBUTE.matcher(html);
        int n = 0;
        while (m.find()) {
            if (m.group(1).trim().equals("ash-list")) {
                n++;
            }
        }
        return n;
    }

    /** Rewrites every migrated List and List Item in {@code html} to its legacy classes. Everything else is untouched. */
    String rewrite(String html) {
        Map<String, String> legacyByMigrated = new LinkedHashMap<>();
        MIGRATED.forEach((role, migrated) -> legacyByMigrated.put(migrated, classes(role)));
        String withoutItemClass = LIST_ITEM.matcher(html).replaceAll("<li>");
        Matcher m = CLASS_ATTRIBUTE.matcher(withoutItemClass);
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        while (m.find()) {
            String value = m.group(1);
            if (!LIST_TOKEN.matcher(value).find()) {
                continue;
            }
            String legacy = legacyByMigrated.get(value);
            if (legacy == null) {
                throw new IllegalStateException("an ash-list class value that is not a known List or List Item part: " + value);
            }
            out.append(withoutItemClass, cursor, m.start()).append("class=\"").append(legacy).append('"');
            cursor = m.end();
        }
        return out.append(withoutItemClass.substring(cursor)).toString();
    }
}
