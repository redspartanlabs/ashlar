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
 * The pre-migration Tailwind class strings of Tooltip and Popover, read from {@code /parity/floating/floating-legacy.html} (the one source of truth for what "legacy" means),
 * and the rewrite that turns migrated markup back into the markup the templates rendered, and the scripts then changed, before the migration.
 *
 * <p>The migrated markup says its state with attributes on the floating surface itself (the Tooltip's bubble, the Popover's surface): {@code hidden} while it is not rendered
 * and {@code data-state="open"} for the open visual state. The legacy markup said the same with classes, which the legacy scripts toggled: the surface carried {@code hidden}
 * while not rendered, and swapped {@code opacity-0 scale-95} for {@code opacity-100 scale-100} while open. The rewrite reads the attributes and writes the classes, so a test
 * can render either state in either form, and it removes what only the migrated markup has ({@code hidden}, {@code data-state}). Everything else (the hooks, the ARIA, the
 * ids, an inline style a test wrote) is kept. Markup that carries no Tooltip or Popover class comes back unchanged; an unknown one fails loudly.
 */
final class FloatingLegacyMarkup {

    static final List<String> ROLES = List.of("tooltip-root", "tooltip-trigger", "tooltip-bubble", "tooltip-position-top", "tooltip-position-bottom", "tooltip-position-left",
            "tooltip-position-right", "tooltip-arrow", "tooltip-arrow-top", "tooltip-arrow-bottom", "tooltip-arrow-left", "tooltip-arrow-right", "popover-root", "popover-trigger",
            "popover-surface", "popover-position-top", "popover-position-bottom", "popover-position-left", "popover-position-right", "popover-title", "popover-body",
            "state-open");

    private static final Pattern LEGACY = Pattern.compile("data-legacy-role=\"([a-z-]+)\" class=\"([^\"]+)\"");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("(?<![\\w-])class=\"([^\"]*)\"");
    private static final Pattern OWN_TOKEN = Pattern.compile("(?<![\\w-])ash-(?:tooltip|popover)(?:__[a-z-]+)?(?:--[a-z]+)?(?![a-zA-Z0-9_-])");
    private static final Pattern TAG = Pattern.compile("<([a-z0-9]+)\\b[^>]*>");
    private static final Pattern HIDDEN = Pattern.compile("\\s+hidden(?=[\\s>/])");
    private static final Pattern STATE = Pattern.compile("\\s+data-state=\"(open|closed)\"");
    private static final List<String> PLACEMENTS = List.of("top", "right", "bottom", "left");

    private final Map<String, String> legacyByRole = new LinkedHashMap<>();

    FloatingLegacyMarkup() throws IOException {
        String html;
        try (InputStream in = FloatingLegacyMarkup.class.getResourceAsStream("/parity/floating/floating-legacy.html")) {
            if (in == null) {
                throw new IOException("missing /parity/floating/floating-legacy.html");
            }
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = LEGACY.matcher(html);
        while (m.find()) {
            if (legacyByRole.put(m.group(1), m.group(2)) != null) {
                throw new IllegalStateException("floating-legacy.html defines " + m.group(1) + " twice");
            }
        }
        if (!legacyByRole.keySet().equals(Set.copyOf(ROLES))) {
            throw new IllegalStateException("floating-legacy.html must define exactly " + ROLES + ", found " + legacyByRole.keySet());
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

    /** The placement a modifier names ({@code ash-tooltip__bubble--left} is "left"), or null. */
    private static String placement(Set<String> tokens, String base) {
        String found = null;
        for (String p : PLACEMENTS) {
            if (tokens.contains(base + "--" + p)) {
                if (found != null) {
                    throw new IllegalStateException("two placements on one element: " + tokens);
                }
                found = p;
            }
        }
        return found;
    }

    /** The surface's legacy classes in the state its attributes say: shown (no hidden attribute) removes hidden, open swaps the closed classes for the open ones in place. */
    private String stated(String legacy, String tag) {
        Matcher state = STATE.matcher(tag);
        if (!state.find()) {
            throw new IllegalStateException("a floating surface without data-state: " + tag);
        }
        List<String> tokens = new ArrayList<>(Arrays.asList(legacy.split("\\s+")));
        if (!HIDDEN.matcher(tag).find()) {
            if (!tokens.remove("hidden")) {
                throw new IllegalStateException("legacy classes without hidden: " + legacy);
            }
        }
        if (state.group(1).equals("open")) {
            String[] open = classes("state-open").split("\\s+");
            String[] closed = {"opacity-0", "scale-95"};
            for (int i = 0; i < closed.length; i++) {
                int at = tokens.indexOf(closed[i]);
                if (at < 0) {
                    throw new IllegalStateException("legacy classes without " + closed[i] + ": " + legacy);
                }
                tokens.set(at, open[i]);
            }
        }
        return String.join(" ", tokens);
    }

    /** Rewrites every migrated Tooltip and Popover part in {@code html} to its legacy classes and state. Everything else is untouched. */
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
            if (tokens.equals(Set.of("ash-tooltip"))) {
                legacy = classes("tooltip-root");
            } else if (tokens.equals(Set.of("ash-tooltip__trigger"))) {
                legacy = classes("tooltip-trigger");
            } else if (tokens.size() == 2 && tokens.contains("ash-tooltip__bubble") && placement(tokens, "ash-tooltip__bubble") != null) {
                legacy = stated(classes("tooltip-bubble"), tag) + " " + classes("tooltip-position-" + placement(tokens, "ash-tooltip__bubble"));
                rewritten = HIDDEN.matcher(STATE.matcher(tag).replaceFirst("")).replaceFirst("");
            } else if (tokens.size() == 2 && tokens.contains("ash-tooltip__arrow") && placement(tokens, "ash-tooltip__arrow") != null) {
                legacy = classes("tooltip-arrow") + " " + classes("tooltip-arrow-" + placement(tokens, "ash-tooltip__arrow"));
            } else if (tokens.equals(Set.of("ash-popover"))) {
                legacy = classes("popover-root");
            } else if (tokens.equals(Set.of("ash-popover__trigger"))) {
                legacy = classes("popover-trigger");
            } else if (tokens.size() == 2 && tokens.contains("ash-popover__surface") && placement(tokens, "ash-popover__surface") != null) {
                legacy = stated(classes("popover-surface"), tag) + " " + classes("popover-position-" + placement(tokens, "ash-popover__surface"));
                rewritten = HIDDEN.matcher(STATE.matcher(tag).replaceFirst("")).replaceFirst("");
            } else if (tokens.equals(Set.of("ash-popover__title"))) {
                legacy = classes("popover-title");
            } else if (tokens.equals(Set.of("ash-popover__body"))) {
                legacy = classes("popover-body");
            } else {
                throw new IllegalStateException("a class value that carries a Tooltip or Popover class but is not a known part: " + cls.group(1));
            }
            out.append(html, cursor, tags.start()).append(rewritten.replace("class=\"" + cls.group(1) + "\"", "class=\"" + legacy + "\""));
            cursor = tags.end();
        }
        return out.append(html.substring(cursor)).toString();
    }
}
