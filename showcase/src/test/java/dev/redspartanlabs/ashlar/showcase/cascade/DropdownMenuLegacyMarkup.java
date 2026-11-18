package dev.redspartanlabs.ashlar.showcase.cascade;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
 * The pre-migration Tailwind class strings of Dropdown Menu, read from {@code /parity/dropdown-menu/dropdown-menu-legacy.html} (the one source of truth for what "legacy"
 * means), and the rewrite that turns migrated markup back into the markup the templates rendered, and the script then changed, before the migration.
 *
 * <p>The migrated markup says its state with attributes: {@code hidden} on the menu while it is not rendered, {@code data-state="open"} on the menu for its open visual state
 * and on the chevron while it is turned. The legacy markup said the same with classes, which the legacy script toggled: the menu carried {@code hidden} while not rendered and
 * swapped {@code opacity-0 scale-95} for {@code opacity-100 scale-100} while open, and the chevron gained {@code rotate-180}. The rewrite reads the attributes and writes the
 * classes, so a test can render either state in either form, and it removes what only the migrated markup has ({@code hidden}, {@code data-state}, and the class of the two
 * wrappers around the trigger's label and icon, which had none). Everything else (the hooks, the ARIA, the ids) is kept. The icons are left to {@link IconLegacyMarkup},
 * which writes the migrated {@code ash-icon ash-icon--sm} back exactly as it writes the {@code ash-icon h-4 w-4} the template passed before. Markup that carries no Dropdown
 * Menu class comes back unchanged; an unknown one fails loudly.
 */
final class DropdownMenuLegacyMarkup {

    static final List<String> ROLES = List.of("root", "trigger", "chevron", "list", "item-link", "item-disabled", "state-open", "chevron-open");

    private static final Pattern LEGACY = Pattern.compile("data-legacy-role=\"([a-z-]+)\" class=\"([^\"]+)\"");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("(?<![\\w-])class=\"([^\"]*)\"");
    private static final Pattern OWN_TOKEN = Pattern.compile("(?<![\\w-])ash-dropdown-menu(?:-item)?(?:__[a-z-]+)?(?:--[a-z]+)?(?![a-zA-Z0-9_-])");
    private static final Pattern TAG = Pattern.compile("<([a-z0-9]+)\\b[^>]*>");
    private static final Pattern HIDDEN = Pattern.compile("\\s+hidden(?=[\\s>/])");
    private static final Pattern STATE = Pattern.compile("\\s+data-state=\"(open|closed)\"");

    private final Map<String, String> legacyByRole = new LinkedHashMap<>();

    DropdownMenuLegacyMarkup() throws IOException {
        String html;
        try (InputStream in = DropdownMenuLegacyMarkup.class.getResourceAsStream("/parity/dropdown-menu/dropdown-menu-legacy.html")) {
            if (in == null) {
                throw new IOException("missing /parity/dropdown-menu/dropdown-menu-legacy.html");
            }
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = LEGACY.matcher(html);
        while (m.find()) {
            if (legacyByRole.put(m.group(1), m.group(2)) != null) {
                throw new IllegalStateException("dropdown-menu-legacy.html defines " + m.group(1) + " twice");
            }
        }
        if (!legacyByRole.keySet().equals(Set.copyOf(ROLES))) {
            throw new IllegalStateException("dropdown-menu-legacy.html must define exactly " + ROLES + ", found " + legacyByRole.keySet());
        }
    }

    String classes(String role) {
        String found = legacyByRole.get(role);
        if (found == null) {
            throw new IllegalStateException("no legacy classes for " + role);
        }
        return found;
    }

    /** Every legacy class token, for the oracle's non-vacuity check. */
    Set<String> allTokens() {
        return legacyByRole.values().stream().flatMap(v -> Arrays.stream(v.split("\\s+"))).collect(Collectors.toCollection(TreeSet::new));
    }

    private static String state(String tag, String what) {
        Matcher state = STATE.matcher(tag);
        if (!state.find()) {
            throw new IllegalStateException(what + " without data-state: " + tag);
        }
        return state.group(1);
    }

    /** The menu's legacy classes in the state its attributes say: shown (no hidden attribute) removes hidden, open swaps the closed classes for the open ones in place. */
    private String list(String tag) {
        List<String> tokens = new ArrayList<>(Arrays.asList(classes("list").split("\\s+")));
        if (!HIDDEN.matcher(tag).find() && !tokens.remove("hidden")) {
            throw new IllegalStateException("legacy menu classes without hidden: " + tokens);
        }
        if (state(tag, "a menu").equals("open")) {
            String[] open = classes("state-open").split("\\s+");
            String[] closed = {"opacity-0", "scale-95"};
            for (int i = 0; i < closed.length; i++) {
                int at = tokens.indexOf(closed[i]);
                if (at < 0) {
                    throw new IllegalStateException("legacy menu classes without " + closed[i]);
                }
                tokens.set(at, open[i]);
            }
        }
        return String.join(" ", tokens);
    }

    private String chevron(String tag) {
        return classes("chevron") + (state(tag, "a chevron").equals("open") ? " " + classes("chevron-open") : "");
    }

    /** Rewrites every migrated Dropdown Menu part in {@code html} to its legacy classes and state. Everything else is untouched. */
    String rewrite(String html) {
        Matcher tags = TAG.matcher(html);
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        while (tags.find()) {
            String tag = tags.group();
            Matcher cls = CLASS_ATTRIBUTE.matcher(tag);
            if (!cls.find() || !OWN_TOKEN.matcher(cls.group(1)).find()) {
                continue;
            }
            Set<String> tokens = Set.of(cls.group(1).trim().split("\\s+"));
            String legacy;
            String rewritten = tag;
            if (tokens.equals(Set.of("ash-dropdown-menu"))) {
                legacy = classes("root");
            } else if (tokens.equals(Set.of("ash-dropdown-menu__trigger"))) {
                legacy = classes("trigger");
            } else if (tokens.equals(Set.of("ash-dropdown-menu__icon")) || tokens.equals(Set.of("ash-dropdown-menu__label"))) {
                legacy = null;
            } else if (tokens.equals(Set.of("ash-dropdown-menu__chevron"))) {
                legacy = chevron(tag);
                rewritten = STATE.matcher(tag).replaceFirst("");
            } else if (tokens.equals(Set.of("ash-dropdown-menu__list"))) {
                legacy = list(tag);
                rewritten = HIDDEN.matcher(STATE.matcher(tag).replaceFirst("")).replaceFirst("");
            } else if (tokens.equals(Set.of("ash-dropdown-menu-item"))) {
                legacy = classes("item-link");
            } else if (tokens.equals(Set.of("ash-dropdown-menu-item", "ash-dropdown-menu-item--disabled"))) {
                legacy = classes("item-disabled");
            } else {
                throw new IllegalStateException("a class value that carries a Dropdown Menu class but is not a known part: " + cls.group(1));
            }
            String attribute = "class=\"" + cls.group(1) + "\"";
            String replaced = legacy == null ? rewritten.replace(" " + attribute, "") : rewritten.replace(attribute, "class=\"" + legacy + "\"");
            out.append(html, cursor, tags.start()).append(replaced);
            cursor = tags.end();
        }
        return out.append(html.substring(cursor)).toString();
    }
}
