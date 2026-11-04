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
 * The pre-migration Tailwind class strings of Nav Link, Navbar, Sidebar and Sidebar Item (migrated together), read from
 * {@code /parity/navigation/navigation-legacy.html} (the one source of truth for what "legacy" means), and the rewrite that turns migrated markup back
 * into the markup the templates rendered before.
 *
 * <p>The rewrite replaces each migrated class value with the legacy class value of the same part. Everything else on the page (the
 * {@code data-nav-link}, {@code data-navbar} and {@code data-sidebar} hooks, {@code aria-current}, the caller's projected content, the Icon inside a Sidebar
 * Item) is left exactly as served. It fails loudly when a class value that carries one of the four components' classes is not one of the known parts, rather
 * than leaving some of it migrated.
 */
final class NavigationLegacyMarkup {

    /** The parts that had Tailwind classes. */
    static final List<String> ROLES = List.of("nav-link", "nav-link-current", "sidebar-item", "sidebar-item-current", "sidebar-item-icon", "sidebar-item-label",
            "sidebar-list", "navbar", "navbar-inner", "navbar-end", "navbar-nav", "navbar-list", "navbar-actions", "navbar-menu", "navbar-menu-list");

    /** The parts that had no class before the migration (a plain <li> or <nav>): the rewrite removes the whole class attribute. */
    static final Set<String> CLASSLESS = Set.of("nav-link-item", "sidebar-item-entry", "sidebar");

    /** The migrated class value of each part, exactly as the templates write it. */
    static final Map<String, String> MIGRATED = Map.ofEntries(
            Map.entry("nav-link-item", "ash-nav-link__item"),
            Map.entry("sidebar-item-entry", "ash-sidebar-item__entry"),
            Map.entry("sidebar", "ash-sidebar"),
            Map.entry("nav-link", "ash-nav-link"),
            Map.entry("nav-link-current", "ash-nav-link ash-nav-link--current"),
            Map.entry("sidebar-item", "ash-sidebar-item"),
            Map.entry("sidebar-item-current", "ash-sidebar-item ash-sidebar-item--current"),
            Map.entry("sidebar-item-icon", "ash-sidebar-item__icon"),
            Map.entry("sidebar-item-label", "ash-sidebar-item__label"),
            Map.entry("sidebar-list", "ash-sidebar__list"),
            Map.entry("navbar", "ash-navbar"),
            Map.entry("navbar-inner", "ash-navbar__inner"),
            Map.entry("navbar-end", "ash-navbar__end"),
            Map.entry("navbar-nav", "ash-navbar__nav"),
            Map.entry("navbar-list", "ash-navbar__list"),
            Map.entry("navbar-actions", "ash-navbar__actions"),
            Map.entry("navbar-menu", "ash-navbar__menu"),
            Map.entry("navbar-menu-list", "ash-navbar__menu-list"));

    private static final Pattern LEGACY = Pattern.compile("data-legacy-role=\"([a-z-]+)\" class=\"([^\"]+)\"");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("class=\"([^\"]*)\"");
    private static final Pattern OWN_TOKEN = Pattern.compile("(?<![\\w-])ash-(?:nav-link|navbar|sidebar)(?![a-zA-Z0-9])");

    private final Map<String, String> legacyByRole = new LinkedHashMap<>();

    NavigationLegacyMarkup() throws IOException {
        String html;
        try (InputStream in = NavigationLegacyMarkup.class.getResourceAsStream("/parity/navigation/navigation-legacy.html")) {
            if (in == null) {
                throw new IOException("missing /parity/navigation/navigation-legacy.html");
            }
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = LEGACY.matcher(html);
        while (m.find()) {
            if (legacyByRole.put(m.group(1), m.group(2)) != null) {
                throw new IllegalStateException("navigation-legacy.html defines " + m.group(1) + " twice");
            }
        }
        if (!legacyByRole.keySet().equals(Set.copyOf(ROLES))) {
            throw new IllegalStateException("navigation-legacy.html must define exactly " + ROLES + ", found " + legacyByRole.keySet());
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

    /** Rewrites every migrated Navigation part in {@code html} to its legacy classes. Everything else is untouched. */
    String rewrite(String html) {
        Map<String, String> legacyByMigrated = new LinkedHashMap<>();
        MIGRATED.forEach((role, migrated) -> legacyByMigrated.put(migrated, CLASSLESS.contains(role) ? "" : classes(role)));
        Matcher m = CLASS_ATTRIBUTE.matcher(html);
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        while (m.find()) {
            String value = m.group(1);
            if (!OWN_TOKEN.matcher(value).find()) {
                continue;
            }
            String legacy = legacyByMigrated.get(value.trim());
            if (legacy == null) {
                throw new IllegalStateException("a class value that carries a Navigation class but is not a known part: " + value);
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
