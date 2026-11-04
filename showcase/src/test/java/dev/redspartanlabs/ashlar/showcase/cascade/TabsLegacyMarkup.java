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
 * The pre-migration Tailwind class strings of Tabs and Tab Panel (migrated together), read from {@code /parity/tabs/tabs-legacy.html} (the one source of truth
 * for what "legacy" means), and the rewrite that turns migrated markup back into the markup the templates rendered before.
 *
 * <p>A tab's legacy classes depended on whether it was the active one, so the rewrite reads each {@code <button>}'s own {@code aria-selected}: the active string for
 * {@code "true"}, the inactive string otherwise (a disabled tab had the same classes as its enabled twin). The root {@code <div>} and the tab's label {@code <span>}
 * had no classes before, so their whole class attribute is removed. A panel keeps the native {@code hidden} attribute that now carries its visibility: the legacy
 * panel was hidden by a {@code hidden} class, which has the same computed effect (and the script no longer toggles classes, so a class would be stale after the
 * script reconciles a panel). Everything else on the page (the {@code data-*} hooks, the ARIA attributes, the caller's panel content, the Icon inside a tab) is left
 * exactly as served. It fails loudly when a class value that carries one of the two components' classes is not a known part.
 */
final class TabsLegacyMarkup {

    /** The parts that had Tailwind classes. */
    static final List<String> ROLES = List.of("tabs-list", "tab-active", "tab-inactive", "tab-badge", "tab-panel");

    /** The parts that had no class before the migration: the rewrite removes the whole class attribute. */
    static final Set<String> CLASSLESS = Set.of("tabs", "tab-label");

    /** The migrated class value of each part, exactly as the templates write it ({@code tab} stands for both the active and the inactive tab). */
    static final Map<String, String> MIGRATED = Map.ofEntries(
            Map.entry("tabs", "ash-tabs"),
            Map.entry("tabs-list", "ash-tabs__list"),
            Map.entry("tab", "ash-tabs__tab"),
            Map.entry("tab-label", "ash-tabs__label"),
            Map.entry("tab-badge", "ash-tabs__badge"),
            Map.entry("tab-panel", "ash-tab-panel"));

    private static final Pattern LEGACY = Pattern.compile("data-legacy-role=\"([a-z-]+)\" class=\"([^\"]+)\"");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("class=\"([^\"]*)\"");
    private static final Pattern BUTTON_TAG = Pattern.compile("<button\\b[^>]*\\bclass=\"ash-tabs__tab\"[^>]*>");
    private static final Pattern OWN_TOKEN = Pattern.compile("(?<![\\w-])ash-tab(?:s|-panel)(?![a-zA-Z0-9])");

    private final Map<String, String> legacyByRole = new LinkedHashMap<>();

    TabsLegacyMarkup() throws IOException {
        String html;
        try (InputStream in = TabsLegacyMarkup.class.getResourceAsStream("/parity/tabs/tabs-legacy.html")) {
            if (in == null) {
                throw new IOException("missing /parity/tabs/tabs-legacy.html");
            }
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = LEGACY.matcher(html);
        while (m.find()) {
            if (legacyByRole.put(m.group(1), m.group(2)) != null) {
                throw new IllegalStateException("tabs-legacy.html defines " + m.group(1) + " twice");
            }
        }
        if (!legacyByRole.keySet().equals(Set.copyOf(ROLES))) {
            throw new IllegalStateException("tabs-legacy.html must define exactly " + ROLES + ", found " + legacyByRole.keySet());
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

    /** Rewrites every migrated Tabs and Tab Panel part in {@code html} to its legacy classes. Everything else is untouched. */
    String rewrite(String html) {
        // The tab buttons first: their legacy classes depend on their own aria-selected.
        Matcher tags = BUTTON_TAG.matcher(html);
        StringBuilder withTabs = new StringBuilder();
        int at = 0;
        while (tags.find()) {
            String tag = tags.group();
            String legacy = classes(tag.contains("aria-selected=\"true\"") ? "tab-active" : "tab-inactive");
            withTabs.append(html, at, tags.start()).append(tag.replace("class=\"ash-tabs__tab\"", "class=\"" + legacy + "\""));
            at = tags.end();
        }
        withTabs.append(html.substring(at));
        html = withTabs.toString();

        Map<String, String> legacyByMigrated = new LinkedHashMap<>();
        MIGRATED.forEach((role, migrated) -> {
            if (!role.equals("tab")) {
                // The legacy badge keeps the migrated class as an inert hook (the oracle has no Ashlar stylesheet): the real-page probe marks the Tabs badge by it, and the comparer
                // normalizes only an element marked on BOTH sides.
                legacyByMigrated.put(migrated, CLASSLESS.contains(role) ? "" : role.equals("tab-badge") ? classes(role) + " " + migrated : classes(role));
            }
        });
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
                throw new IllegalStateException("a class value that carries a Tabs or Tab Panel class but is not a known part: " + value);
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
