package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Button used to be styled by Tailwind utility classes, and the skins reached it two ways: Foundry through a class ({@code .bg-transparent}, to tell
 * Ghost and Link from the filled variants), and nine skins, which neutralise a Tailwind shadow class ({@code .shadow-sm}) that Button carried, through
 * a rule that no longer reaches a Button. This pins the replacement: Foundry's two selectors read the existing {@code data-btn-variant} hook (and, to keep a
 * pressed Ghost shadowed as it was, {@code aria-pressed} and the pressed modifier) at exactly one attribute more specificity than before; each of the other
 * nine skins flattens Button's drop shadow through {@code --ash-btn-shadow} only; the skins' other Button rules are unchanged. It reads the skin sources only.
 */
class ButtonSkinSelectorsTest {

    private static final Path SKINS = Path.of("src/main/resources/static/css");
    private static final List<String> NINE = List.of("axiom", "civic", "fathom", "harbor", "keystone", "ledger", "sterling", "summit", "veridian", "foundry");
    private static final String OLD_FIRST = "[data-skin=\"foundry\"] [data-btn]:not(.bg-transparent)";
    private static final String OLD_ACTIVE = "[data-skin=\"foundry\"] [data-btn]:not(.bg-transparent):not(:disabled):active";
    private static final String GHOST_AT_REST = "[data-btn-variant=\"ghost\"]:not([aria-pressed=\"true\"], .ash-btn--pressed)";
    private static final String NEW_FIRST = "[data-skin=\"foundry\"] [data-btn]:not([data-btn-variant=\"link\"], " + GHOST_AT_REST + ")";
    private static final String NEW_ACTIVE = NEW_FIRST + ":not(:disabled):active";

    private static String skin(String name) throws IOException {
        return Files.readString(SKINS.resolve("theme-" + name + ".css"), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static String code(String name) throws IOException {
        return skin(name).replaceAll("(?s)/\\*.*?\\*/", "");
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }

    // ---- specificity, computed (a, b, c): ids, classes/attributes/pseudo-classes, elements ----

    /** The specificity of a selector with no ids or elements beyond the ones written, treating :not/:is/:where as the CSS Selectors rules say. */
    private static int[] specificity(String selector) {
        int[] total = new int[3];
        int i = 0;
        while (i < selector.length()) {
            char c = selector.charAt(i);
            if (c == '.') {
                total[1]++;
                i = identEnd(selector, i + 1);
            } else if (c == '[') {
                total[1]++;
                i = selector.indexOf(']', i) + 1;
            } else if (c == ':') {
                int j = identEnd(selector, i + 1);
                String name = selector.substring(i + 1, j);
                if (j < selector.length() && selector.charAt(j) == '(') {
                    int close = matching(selector, j);
                    String inside = selector.substring(j + 1, close);
                    if (name.equals("not") || name.equals("is")) {
                        total[1] += max(inside)[1];
                    } else if (!name.equals("where")) {
                        total[1]++;
                    }
                    i = close + 1;
                } else {
                    total[1]++;
                    i = j;
                }
            } else if (Character.isLetter(c)) {
                total[2]++;
                i = identEnd(selector, i);
            } else {
                i++;
            }
        }
        return total;
    }

    private static int identEnd(String s, int from) {
        int i = from;
        while (i < s.length() && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '-' || s.charAt(i) == '_')) {
            i++;
        }
        return i;
    }

    private static int matching(String s, int open) {
        int depth = 0;
        for (int i = open; i < s.length(); i++) {
            if (s.charAt(i) == '(') {
                depth++;
            } else if (s.charAt(i) == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        throw new IllegalArgumentException(s);
    }

    /** The most specific of a comma-separated selector list. */
    private static int[] max(String list) {
        int[] best = new int[3];
        int depth = 0;
        int start = 0;
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < list.length(); i++) {
            char c = list.charAt(i);
            if (c == '(' || c == '[') {
                depth++;
            } else if (c == ')' || c == ']') {
                depth--;
            } else if (c == ',' && depth == 0) {
                parts.add(list.substring(start, i).trim());
                start = i + 1;
            }
        }
        parts.add(list.substring(start).trim());
        for (String part : parts) {
            int[] s = specificity(part);
            if (s[1] > best[1] || (s[1] == best[1] && s[2] > best[2])) {
                best = s;
            }
        }
        return best;
    }

    private static String show(int[] s) {
        return "(" + s[0] + "," + s[1] + "," + s[2] + ")";
    }

    @Test
    void theSpecificityFunctionIsRightOnKnownSelectors() {
        assertEquals("(0,1,0)", show(specificity(".a")));
        assertEquals("(0,2,1)", show(specificity("button.a:hover")));
        assertEquals("(0,0,0)", show(specificity(":where(.a .b)")));
        assertEquals("(0,3,0)", show(specificity(".a:is(.b, .c:hover)")));
        assertEquals("(0,3,0)", show(specificity("[x]:not(.a, [b]:not(.c, .d))")));
    }

    // ---- Foundry ----

    @Test
    void foundrysTwoSelectorsNoLongerDependOnATailwindClass() throws IOException {
        String foundry = code("foundry");
        assertEquals(0, count(foundry, "bg-transparent"));
        assertEquals(1, count(foundry, NEW_FIRST + " {"), "the resting rule");
        assertEquals(1, count(foundry, NEW_ACTIVE + " {"), "the pressed-down rule");
        assertEquals(2, count(foundry, "[data-btn]"), "no other Button selector was touched");
    }

    @Test
    void foundrysDeclarationsAndOrderAreUnchanged() throws IOException {
        String foundry = code("foundry");
        Matcher m = Pattern.compile(Pattern.quote(NEW_FIRST) + " \\{\\s*box-shadow: inset 0 1px 0 var\\(--foundry-edge-light\\), var\\(--foundry-drop\\);\\s*\\}\\s*"
                + Pattern.quote(NEW_ACTIVE) + " \\{\\s*box-shadow: inset 0 1px 2px oklch\\(20% 0\\.02 245 / 0\\.25\\);\\s*\\}").matcher(foundry);
        assertTrue(m.find(), "the two rules, in order, with their original declaration values");
    }

    @Test
    void foundrysSelectorsGainExactlyOneAttributeOfSpecificityAndThePseudoClassesAreKept() {
        int[] before = specificity(OLD_FIRST);
        int[] after = specificity(NEW_FIRST);
        assertEquals("(0,3,0)", show(before));
        assertEquals("(0,4,0)", show(after));
        assertEquals(before[1] + 1, after[1], "one attribute more, the amount the owner allowed, to keep a pressed Ghost shadowed");
        assertEquals("(0,5,0)", show(specificity(OLD_ACTIVE)));
        assertEquals("(0,6,0)", show(specificity(NEW_ACTIVE)));
        assertEquals(specificity(OLD_ACTIVE)[1] + 1, specificity(NEW_ACTIVE)[1]);
        // The only thing that raised it is the nested :not for a resting Ghost; without it the selector would be LOWER-or-equal, not higher.
        assertEquals("(0,3,0)", show(specificity("[data-skin=\"foundry\"] [data-btn]:not([data-btn-variant=\"link\"], [data-btn-variant=\"ghost\"])")));
    }

    @Test
    void foundrysSelectorSelectsExactlyWhatBgTransparentExcludedForAllEightVariantsAtRestAndPressed() {
        // The legacy class strings are the oracle: a button had bg-transparent exactly when the old rule skipped it.
        for (var variant : dev.redspartanlabs.ashlar.button.ButtonVariant.values()) {
            for (boolean pressed : new boolean[] {false, true}) {
                boolean legacyExcluded = ButtonLegacyStyles.classes(variant, dev.redspartanlabs.ashlar.button.ButtonSize.MEDIUM, false, false, pressed).contains("bg-transparent");
                String name = variant.name().toLowerCase();
                boolean isLink = name.equals("link");
                boolean isGhostAtRest = name.equals("ghost") && !pressed;
                assertEquals(legacyExcluded, isLink || isGhostAtRest, name + (pressed ? " pressed" : " at rest") + ": the hooks exclude exactly what bg-transparent excluded");
            }
        }
    }

    // ---- all ten skins flatten the drop shadow ----

    @Test
    void eachOfTheTenSkinsFlattensButtonsDropShadowThroughOnlyTheButtonsOwnProperty() throws IOException {
        for (String name : NINE) {
            String css = code(name);
            String rule = "[data-skin=\"" + name + "\"] .ash-btn {\n    --ash-btn-shadow: 0 0 #0000;\n}";
            assertEquals(1, count(css, rule), name);
            assertEquals(1, count(css, ".ash-btn {"), name + ": exactly one rule on the Button class");
            // Nothing else in a skin names a Button class, except Foundry's two selectors, which name the pressed modifier once each.
            assertEquals(name.equals("foundry") ? 2 : 0, count(css, "ash-btn--"), name);
            // The existing Tailwind shadow neutralisation, which other components still use, is preserved.
            assertEquals(1, count(css, "[data-skin=\"" + name + "\"] :is(.shadow-xs, .shadow-sm, .shadow, .shadow-md, .shadow-lg, .shadow-xl, .shadow-2xl) {\n    --tw-shadow: 0 0 #0000;\n}"), name);
            // The new rule comes right after it and before the floating-surface rule, so the cascade among the skin's rules is unchanged.
            int tailwind = css.indexOf(".shadow-2xl) {");
            assertTrue(css.indexOf(rule) > tailwind && css.indexOf(rule) < tailwind + 200, name);
        }
    }

    @Test
    void theSkinsOtherButtonRulesAreUntouched() throws IOException {
        assertEquals(1, count(code("civic"), "[data-skin=\"civic\"] [data-btn] {\n    font-weight: 600;\n}"));
        assertEquals(1, count(code("summit"), "[data-skin=\"summit\"] :is([data-btn], [data-dropdown-menu-item], [data-badge]) {\n    font-weight: 600;\n}"));
        assertTrue(code("axiom").contains(":is(button, a, input, select, textarea, [data-badge], [data-btn]) {"));
        for (String name : List.of("axiom", "civic", "fathom", "harbor", "keystone", "ledger", "sterling", "summit", "veridian", "foundry")) {
            assertFalse(code(name).contains(".bg-transparent"), name + ": no Button selector depends on a Tailwind class");
        }
    }
}
