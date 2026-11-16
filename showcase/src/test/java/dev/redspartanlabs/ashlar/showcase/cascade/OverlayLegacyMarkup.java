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
 * The pre-migration Tailwind class strings of Modal and Drawer, read from {@code /parity/overlay/overlay-legacy.html} (the one source of truth for what "legacy" means), and
 * the rewrite that turns migrated markup back into the markup the templates rendered, and the scripts then changed, before the migration.
 *
 * <p>The migrated markup says its state with attributes: {@code hidden} on the root while it is not rendered, and {@code data-state="open"} on the root for the open visual
 * state. The legacy markup said the same with classes, which the legacy scripts toggled: the root carried {@code hidden} while not rendered and {@code flex} while shown, and
 * the backdrop, the dialog and the panel swapped their closed classes for the open ones. The rewrite reads the attributes and writes the classes, so a test can render either
 * state in either form. It also puts back what only the legacy markup had (the Drawer root's {@code data-drawer-closed-class}, which the legacy drawer.js read) and removes
 * what only the migrated markup has ({@code hidden}, {@code data-state}).
 *
 * <p>The close button's icon is rewritten to what the templates passed it ({@code classes = "h-5 w-5"}, so {@code ash-icon h-5 w-5}); {@link IconLegacyMarkup} then takes the
 * Icon itself back to its own legacy form, exactly as for every other family. Markup that carries no overlay class comes back unchanged; an unknown overlay class fails loudly.
 */
final class OverlayLegacyMarkup {

    static final List<String> ROLES = List.of("modal-root", "modal-backdrop", "modal-dialog-small", "modal-dialog-medium", "modal-dialog-large", "drawer-root-left",
            "drawer-root-right", "drawer-backdrop", "drawer-panel-left", "drawer-panel-right", "header", "title", "close", "close-icon", "modal-body", "drawer-body", "footer",
            "state-root-shown", "state-backdrop-open", "state-dialog-open", "state-panel-open");

    private static final Pattern LEGACY = Pattern.compile("data-legacy-role=\"([a-z-]+)\" class=\"([^\"]+)\"");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("(?<![\\w-])class=\"([^\"]*)\"");
    private static final Pattern OWN_TOKEN = Pattern.compile("(?<![\\w-])ash-(?:overlay|modal|drawer)(?:__[a-z-]+)?(?:--[a-z]+)?(?![a-zA-Z0-9_-])");
    private static final Pattern TAG = Pattern.compile("<([a-z0-9]+)\\b[^>]*>");
    private static final Pattern HIDDEN = Pattern.compile("\\s+hidden(?=[\\s>/])");
    private static final Pattern STATE = Pattern.compile("\\s+data-state=\"(open|closed)\"");

    private final Map<String, String> legacyByRole = new LinkedHashMap<>();

    OverlayLegacyMarkup() throws IOException {
        String html;
        try (InputStream in = OverlayLegacyMarkup.class.getResourceAsStream("/parity/overlay/overlay-legacy.html")) {
            if (in == null) {
                throw new IOException("missing /parity/overlay/overlay-legacy.html");
            }
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = LEGACY.matcher(html);
        while (m.find()) {
            if (legacyByRole.put(m.group(1), m.group(2)) != null) {
                throw new IllegalStateException("overlay-legacy.html defines " + m.group(1) + " twice");
            }
        }
        if (!legacyByRole.keySet().equals(Set.copyOf(ROLES))) {
            throw new IllegalStateException("overlay-legacy.html must define exactly " + ROLES + ", found " + legacyByRole.keySet());
        }
    }

    String classes(String role) {
        String found = legacyByRole.get(role);
        if (found == null) {
            throw new IllegalStateException("no legacy classes for " + role);
        }
        return found;
    }

    /** Every legacy class token, for the oracle's non-vacuity check (the close icon's own ash-icon is the Icon's, rewritten by IconLegacyMarkup, and is left out). */
    Set<String> allTokens() {
        return legacyByRole.entrySet().stream().flatMap(e -> Arrays.stream(e.getValue().split("\\s+")))
                .filter(t -> !t.startsWith("ash-")).collect(Collectors.toCollection(TreeSet::new));
    }

    /** {@code legacy} with each of {@code closed} replaced in place by the token at the same position of {@code open} (a missing token is appended). */
    private static String swap(String legacy, String closedTokens, String openTokens) {
        List<String> tokens = new java.util.ArrayList<>(Arrays.asList(legacy.split("\\s+")));
        String[] closed = closedTokens.split("\\s+");
        String[] open = openTokens.split("\\s+");
        for (int i = 0; i < closed.length; i++) {
            int at = tokens.indexOf(closed[i]);
            if (at < 0) {
                throw new IllegalStateException("legacy classes without " + closed[i] + ": " + legacy);
            }
            tokens.set(at, open[i]);
        }
        return String.join(" ", tokens);
    }

    /** The legacy state of one root (from the migrated root tag): shown (no hidden attribute) and open (data-state="open"). */
    private record RootState(boolean shown, boolean open) {
    }

    /** Rewrites every migrated Modal and Drawer part in {@code html} to its legacy classes and state. Everything else is untouched. */
    String rewrite(String html) {
        Matcher tags = TAG.matcher(html);
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        RootState root = null;
        boolean drawer = false;
        boolean inClose = false;
        while (tags.find()) {
            String tag = tags.group();
            Matcher cls = CLASS_ATTRIBUTE.matcher(tag);
            if (!cls.find()) {
                continue;
            }
            Set<String> tokens = Set.of(cls.group(1).trim().split("\\s+"));
            String legacy;
            String rewritten = tag;
            if (inClose && tokens.equals(Set.of("ash-icon"))) {
                legacy = classes("close-icon");
                inClose = false;
            } else if (!OWN_TOKEN.matcher(cls.group(1)).find()) {
                continue;
            } else if (tokens.equals(Set.of("ash-overlay", "ash-modal")) || tokens.equals(Set.of("ash-overlay", "ash-drawer", "ash-drawer--left"))
                    || tokens.equals(Set.of("ash-overlay", "ash-drawer", "ash-drawer--right"))) {
                Matcher state = STATE.matcher(tag);
                if (!state.find()) {
                    throw new IllegalStateException("an overlay root without data-state: " + tag);
                }
                root = new RootState(!HIDDEN.matcher(tag).find(), state.group(1).equals("open"));
                drawer = tokens.contains("ash-drawer");
                String role = !drawer ? "modal-root" : tokens.contains("ash-drawer--left") ? "drawer-root-left" : "drawer-root-right";
                legacy = classes(role);
                if (root.shown()) {
                    legacy = swap(legacy, "hidden", classes("state-root-shown"));
                }
                rewritten = HIDDEN.matcher(STATE.matcher(tag).replaceFirst("")).replaceFirst("");
                if (drawer) {
                    String closed = tokens.contains("ash-drawer--left") ? "-translate-x-full" : "translate-x-full";
                    rewritten = rewritten.replaceFirst("\\sdata-drawer(?=[\\s>])", " data-drawer data-drawer-closed-class=\"" + closed + "\"");
                }
            } else if (tokens.equals(Set.of("ash-overlay__backdrop"))) {
                requireRoot(root, tag);
                legacy = classes(drawer ? "drawer-backdrop" : "modal-backdrop");
                if (root.open()) {
                    legacy = swap(legacy, "opacity-0", classes("state-backdrop-open"));
                }
            } else if (tokens.size() == 3 && tokens.containsAll(Set.of("ash-overlay__surface", "ash-modal__dialog"))) {
                requireRoot(root, tag);
                String size = tokens.contains("ash-modal__dialog--sm") ? "small" : tokens.contains("ash-modal__dialog--md") ? "medium"
                        : tokens.contains("ash-modal__dialog--lg") ? "large" : null;
                if (size == null) {
                    throw new IllegalStateException("a dialog without a known size: " + cls.group(1));
                }
                legacy = classes("modal-dialog-" + size);
                if (root.open()) {
                    legacy = swap(legacy, "opacity-0 scale-95 -translate-y-1", classes("state-dialog-open"));
                }
            } else if (tokens.size() == 3 && tokens.containsAll(Set.of("ash-overlay__surface", "ash-drawer__panel"))) {
                requireRoot(root, tag);
                boolean left = tokens.contains("ash-drawer__panel--left");
                if (!left && !tokens.contains("ash-drawer__panel--right")) {
                    throw new IllegalStateException("a panel without a known edge: " + cls.group(1));
                }
                legacy = classes(left ? "drawer-panel-left" : "drawer-panel-right");
                if (root.open()) {
                    legacy = swap(legacy, left ? "-translate-x-full" : "translate-x-full", classes("state-panel-open"));
                }
            } else if (tokens.equals(Set.of("ash-overlay__header"))) {
                legacy = classes("header");
            } else if (tokens.equals(Set.of("ash-overlay__title"))) {
                legacy = classes("title");
            } else if (tokens.equals(Set.of("ash-overlay__close"))) {
                legacy = classes("close");
                inClose = true;
            } else if (tokens.equals(Set.of("ash-overlay__body"))) {
                legacy = classes("modal-body");
            } else if (tokens.equals(Set.of("ash-overlay__body", "ash-drawer__body"))) {
                legacy = classes("drawer-body");
            } else if (tokens.equals(Set.of("ash-overlay__footer"))) {
                legacy = classes("footer");
            } else {
                throw new IllegalStateException("a class value that carries an overlay class but is not a known part: " + cls.group(1));
            }
            out.append(html, cursor, tags.start()).append(rewritten.replace("class=\"" + cls.group(1) + "\"", "class=\"" + legacy + "\""));
            cursor = tags.end();
        }
        return out.append(html.substring(cursor)).toString();
    }

    private static void requireRoot(RootState root, String tag) {
        if (root == null) {
            throw new IllegalStateException("an overlay part outside any overlay root: " + tag);
        }
    }
}
