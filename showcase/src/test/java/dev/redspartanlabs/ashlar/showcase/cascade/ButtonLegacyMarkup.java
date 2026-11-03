package dev.redspartanlabs.ashlar.showcase.cascade;

import dev.redspartanlabs.ashlar.button.ButtonSize;
import dev.redspartanlabs.ashlar.button.ButtonVariant;

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
 * The pre-migration Tailwind class strings of Button, and the rewrite that turns migrated Button markup back into the markup the template
 * rendered before.
 *
 * <p>The classes of the {@code <button>} itself depend on its variant, size, icon-only, full-width and pressed state, so they come from
 * {@link ButtonLegacyStyles} (a verbatim copy of the former {@code ButtonStyles}), not from a fixed table. The classes of its parts (the
 * spinner, the hidden icon, the screen-reader text) are read from {@code /parity/button/button-legacy.html}.
 *
 * <p>The rewrite reads each Button's state from the markup it is given, as the old template did: a button is pressed when it carries
 * {@code ash-btn--pressed} or {@code aria-pressed="true"}, and it is loading when it carries {@code aria-busy="true"}, which is what showed
 * the spinner and hid the icon. Everything else (the {@code data-*} hooks, the ARIA attributes, the caller's content) is left exactly as
 * served. It fails loudly on a class value that carries an {@code ash-btn} class but is not a known part, rather than leaving it migrated.
 */
final class ButtonLegacyMarkup {

    static final List<String> PART_ROLES = List.of("spinner-shown", "spinner-hidden", "icon-hidden", "sr-only", "pointer-events-none");

    private static final Pattern LEGACY = Pattern.compile("<span data-legacy-role=\"([a-z-]+)\" class=\"([^\"]+)\"");
    private static final Pattern TAG = Pattern.compile("<button\\b[^>]*>|</button>|<span\\b[^>]*>");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile(" class=\"([^\"]*)\"");
    private static final Pattern OWN_TOKEN = Pattern.compile("(?<![\\w-])ash-btn(?![a-zA-Z0-9])");
    private static final Pattern BUTTON_CLASSES = Pattern.compile(
            "^ash-btn ash-btn--(primary|secondary|success|warning|danger|info|ghost|link) ash-btn--(xs|sm|md|lg|xl)( ash-btn--icon)?( ash-btn--block)?( ash-btn--pressed)?( .+)?$");

    private final Map<String, String> parts = new LinkedHashMap<>();

    ButtonLegacyMarkup() throws IOException {
        String html;
        try (InputStream in = ButtonLegacyMarkup.class.getResourceAsStream("/parity/button/button-legacy.html")) {
            if (in == null) {
                throw new IOException("missing /parity/button/button-legacy.html");
            }
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = LEGACY.matcher(html);
        while (m.find()) {
            if (parts.put(m.group(1), m.group(2)) != null) {
                throw new IllegalStateException("button-legacy.html defines " + m.group(1) + " twice");
            }
        }
        if (!parts.keySet().equals(Set.copyOf(PART_ROLES))) {
            throw new IllegalStateException("button-legacy.html must define exactly " + PART_ROLES + ", found " + parts.keySet());
        }
    }

    /** The legacy classes of a part (not of the button element itself). */
    String part(String role) {
        String found = parts.get(role);
        if (found == null) {
            throw new IllegalStateException("no legacy classes for " + role);
        }
        return found;
    }

    private static ButtonSize sizeOf(String modifier) {
        return switch (modifier) {
            case "xs" -> ButtonSize.XSMALL;
            case "sm" -> ButtonSize.SMALL;
            case "md" -> ButtonSize.MEDIUM;
            case "lg" -> ButtonSize.LARGE;
            case "xl" -> ButtonSize.XLARGE;
            default -> throw new IllegalArgumentException(modifier);
        };
    }

    /** The legacy classes of the button element for a variant, size and state: what the former ButtonStyles produced. */
    static String legacyButton(String variant, String size, boolean iconOnly, boolean fullWidth, boolean pressed) {
        return ButtonLegacyStyles.classes(ButtonVariant.valueOf(variant.toUpperCase()), sizeOf(size), iconOnly, fullWidth, pressed);
    }

    /** Every legacy class token, for the oracle's non-vacuity check: the parts', and every button string the cases can produce. */
    Set<String> allTokens() {
        Set<String> tokens = new TreeSet<>();
        parts.values().forEach(s -> tokens.addAll(Arrays.asList(s.split("\\s+"))));
        for (ButtonVariant v : ButtonVariant.values()) {
            for (ButtonSize s : ButtonSize.values()) {
                for (boolean icon : new boolean[] {false, true}) {
                    for (boolean pressed : new boolean[] {false, true}) {
                        tokens.addAll(Arrays.asList(ButtonLegacyStyles.classes(v, s, icon, true, pressed).split("\\s+")));
                    }
                }
            }
        }
        return tokens;
    }

    /** How many migrated Buttons ({@code <button>} or a decorative element with the Button classes) {@code html} contains. */
    static int countButtons(String html) {
        Matcher m = CLASS_ATTRIBUTE.matcher(html);
        int n = 0;
        while (m.find()) {
            if (BUTTON_CLASSES.matcher(m.group(1)).matches()) {
                n++;
            }
        }
        return n;
    }

    /** How many class attributes in {@code html} are exactly {@code value}. */
    static int countClass(String html, String value) {
        Matcher m = CLASS_ATTRIBUTE.matcher(html);
        int n = 0;
        while (m.find()) {
            if (m.group(1).equals(value)) {
                n++;
            }
        }
        return n;
    }

    /** Rewrites every migrated Button in {@code html} to the markup it had before the migration. Everything else is untouched. */
    String rewrite(String html) {
        Matcher tags = TAG.matcher(html);
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        boolean loading = false;
        while (tags.find()) {
            String tag = tags.group();
            if (tag.equals("</button>")) {
                loading = false;
                continue;
            }
            Matcher cls = CLASS_ATTRIBUTE.matcher(tag);
            if (tag.startsWith("<button") && tag.contains("aria-busy=\"true\"")) {
                loading = true;
            }
            if (!cls.find() || !OWN_TOKEN.matcher(cls.group(1)).find() && !cls.group(1).contains("ash-btn__")) {
                continue;
            }
            String value = cls.group(1);
            String replacement = rewriteClass(tag, value, loading);
            String rewritten = replacement == null
                    ? tag.substring(0, cls.start()) + tag.substring(cls.end())
                    : tag.substring(0, cls.start()) + " class=\"" + replacement + "\"" + tag.substring(cls.end());
            out.append(html, cursor, tags.start()).append(rewritten);
            cursor = tags.end();
        }
        return out.append(html.substring(cursor)).toString();
    }

    /** The legacy class value for one class attribute, or null when the legacy part had no class attribute at all. */
    private String rewriteClass(String tag, String value, boolean loading) {
        Matcher button = BUTTON_CLASSES.matcher(value);
        if (button.matches()) {
            boolean pressed = button.group(5) != null || tag.contains("aria-pressed=\"true\"");
            String legacy = legacyButton(button.group(1), button.group(2), button.group(3) != null, button.group(4) != null, pressed);
            // A pressed button that is not a toggle keeps ash-btn--pressed, which has no style of its own here (the oracle has no Ashlar stylesheet): it is
            // the hook the Foundry skin selects to tell a pressed Ghost from a resting one, as aria-pressed is for a toggle, so both sides carry it.
            if (button.group(5) != null) {
                legacy += " ash-btn--pressed";
            }
            return button.group(6) == null ? legacy : legacy + button.group(6);
        }
        switch (value) {
            case "ash-btn__spinner":
                return part(loading ? "spinner-shown" : "spinner-hidden");
            case "ash-btn__icon":
                return loading ? part("icon-hidden") : null;
            case "ash-btn__label":
                return null;
            case "ash-sr-only ash-btn__label":
            case "ash-sr-only ash-btn__status":
                return part("sr-only");
            default:
                throw new IllegalStateException("a class value that carries a Button class but is not a known part: " + value);
        }
    }

    /** The class tokens of {@code values} joined, for tests. */
    static String join(List<String> values) {
        return values.stream().collect(Collectors.joining(" "));
    }
}
