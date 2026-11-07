package dev.redspartanlabs.ashlar.css;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.redspartanlabs.ashlar.css.TextEntrySources.DARK;
import static dev.redspartanlabs.ashlar.css.TextEntrySources.css;
import static dev.redspartanlabs.ashlar.css.TextEntrySources.declarations;
import static dev.redspartanlabs.ashlar.css.TextEntrySources.read;
import static dev.redspartanlabs.ashlar.css.TextEntrySources.rules;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the stylesheets of the Text Entry family (field.css, search-input.css, textarea.css). The rendered result is measured in a real browser by the showcase's
 * {@code TextEntryParityTest}, {@code TextEntryTailwindFreeTest} and {@code TextEntryBehaviourTest}; this reads the sources only: that every selector belongs to the family, that the
 * states are the native selectors, that the rules stay in the cascade order the legacy behaviour was measured in, that hover is limited to hover devices, that customisation is by
 * ancestor (the public properties are only ever read), and that every palette colour has its fallback.
 */
class FieldCssContractTest {

    private static final String SHADOW = "0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1)";
    private static final String CTRL = ".ash-field__control";
    private static final String RO = ".ash-field__control--readonly";

    private static String stripped(String name) throws IOException {
        return css(name).replaceAll("(?s)/\\*.*?\\*/", "");
    }

    private static void assertScoped(String name, int expectedRules) throws IOException {
        String s = stripped(name);
        assertFalse(s.contains("!important"), name);
        assertFalse(s.contains("@import") || s.contains("@layer") || s.contains("@keyframes"), name);
        assertFalse(s.contains("data-search-input") || s.contains("data-textarea-counter") && !s.contains("data-at-limit"), name + ": styling is by class and native state; the data- hooks belong to the scripts");
        assertFalse(Pattern.compile("(^|[,{}]\\s*)(\\*|html|body|button|div|span|svg|input|textarea|label|p)\\s*[,{:.\\[]").matcher(s).find(), name + ": no bare element selector");
        List<CssScanner.Rule> rules = rules(name);
        assertEquals(expectedRules, rules.size(), name);
        for (CssScanner.Rule r : rules) {
            assertTrue(Pattern.compile("^\\.ash-(field|textarea|search-input)__?[a-z-]*").matcher(r.selector()).find() || r.selector().equals(".ash-field"),
                    name + ": a selector that does not start with a Text Entry class: " + r.selector());
        }
    }

    // ---- scoping ----

    @Test
    void everySelectorIsScopedToTheFamilysOwnClassesAndNothingIsGlobal() throws IOException {
        assertScoped("field", 36);
        assertScoped("search-input", 10);
        assertScoped("textarea", 10);
        assertEquals(List.of("@media (hover: hover)"), rules("field").stream().map(CssScanner.Rule::media).filter(m -> !m.isEmpty()).distinct().toList(), "hover is the only conditional in field.css");
        assertEquals(List.of("@media (hover: hover)"), rules("search-input").stream().map(CssScanner.Rule::media).filter(m -> !m.isEmpty()).distinct().toList());
        assertEquals(List.of(), rules("textarea").stream().map(CssScanner.Rule::media).filter(m -> !m.isEmpty()).toList(), "no motion exists in the family, so there is nothing to reduce");
        for (String name : List.of("field", "search-input", "textarea")) {
            assertFalse(stripped(name).contains("transition") || stripped(name).contains("animation"), name + ": the family has no motion");
        }
    }

    @Test
    void everyHoverRuleIsInsideAHoverMediaQuery() throws IOException {
        for (String name : List.of("field", "search-input")) {
            for (CssScanner.Rule r : rules(name)) {
                if (r.selector().contains(":hover")) {
                    assertEquals("@media (hover: hover)", r.media(), name + ": " + r.selector());
                }
            }
        }
        assertFalse(rules("textarea").stream().anyMatch(r -> r.selector().contains(":hover")));
    }

    // ---- the native selectors are the state, in the measured cascade order ----

    @Test
    void theStatesAreTheNativeSelectorsAndNoStateClassExists() throws IOException {
        String s = stripped("field");
        for (String state : List.of(":user-invalid", ":read-only", ":disabled", ":focus", ":hover", "[aria-invalid=\"true\"]", "::placeholder")) {
            assertTrue(s.contains(state), "field.css styles " + state);
        }
        assertFalse(s.contains(".is-") || s.contains("--invalid:") || s.contains("--disabled") || s.contains("--error") || s.contains("--focus"), "no state modifier class");
        assertFalse(s.contains(":focus-visible"), "a field shows its ring on :focus, as before");
        assertFalse(stripped("search-input").contains(":read-only") || stripped("textarea").contains(":read-only"));
    }

    @Test
    void theRulesStayInTheCascadeOrderTheLegacyBehaviourWasMeasuredIn() throws IOException {
        String s = stripped("field").replaceAll("\\s+", " ");
        List<String> ordered = List.of(
                CTRL + " { display: block;",
                CTRL + "::placeholder {",
                CTRL + ":user-invalid {",
                RO + ":read-only {",
                CTRL + ":hover {",
                CTRL + ":user-invalid:hover {",
                RO + ":read-only:hover {",
                CTRL + ":focus {",
                CTRL + ":user-invalid:focus {",
                CTRL + ":disabled {",
                CTRL + "[aria-invalid=\"true\"] {",
                CTRL + "[aria-invalid=\"true\"]:hover {",
                CTRL + "[aria-invalid=\"true\"]:focus {",
                CTRL + DARK + " {",
                CTRL + DARK + ":user-invalid {",
                RO + DARK + ":read-only {",
                CTRL + DARK + ":hover {",
                RO + DARK + ":read-only:hover {",
                CTRL + DARK + ":disabled {",
                CTRL + DARK + "[aria-invalid=\"true\"] {");
        int at = -1;
        for (String marker : ordered) {
            int found = s.indexOf(marker);
            assertTrue(found > at, "out of order, missing or duplicated: " + marker + " (after position " + at + ", found " + found + ")");
            assertEquals(found, s.lastIndexOf(marker), "defined once: " + marker);
            at = found;
        }
    }

    @Test
    void thePrecedenceTheLegacyHadIsKept() throws IOException {
        // Specificity: a state is one class plus one pseudo (0,2,0); the overlap of read-only with hover is (0,3,0); a dark rule's :where() adds none. Source order does the rest.
        Map<String, String> readonlyHover = declarations("field", RO + ":read-only:hover", "@media (hover: hover)");
        assertEquals("var(--color-slate-300, oklch(86.9% 0.022 252.894))", readonlyHover.get("border-color"));
        Map<String, String> darkHover = declarations("field", CTRL + DARK + ":hover", "@media (hover: hover)");
        assertEquals("var(--color-slate-500, oklch(55.4% 0.046 257.417))", darkHover.get("border-color"));
        Map<String, String> darkReadonlyHover = declarations("field", RO + DARK + ":read-only:hover", "@media (hover: hover)");
        assertEquals("var(--color-slate-600, oklch(44.6% 0.043 257.281))", darkReadonlyHover.get("border-color"));
        Map<String, String> hover = declarations("field", CTRL + ":hover", "@media (hover: hover)");
        assertEquals("var(--color-slate-400, oklch(70.4% 0.04 256.788))", hover.get("border-color"));
    }

    // ---- the control ----

    @Test
    void theControlIsTheMeasuredThirtyEightPixelBox() throws IOException {
        Map<String, String> c = declarations("field", CTRL);
        assertEquals("block", c.get("display"));
        assertEquals("border-box", c.get("box-sizing"));
        assertEquals("100%", c.get("width"));
        assertEquals("0", c.get("margin"));
        assertEquals("1px solid var(--ash-field-border, var(--color-slate-300, oklch(86.9% 0.022 252.894)))", c.get("border"));
        assertEquals("var(--ash-radius-md)", c.get("border-radius"));
        assertEquals("var(--ash-field-bg, var(--color-white, #fff))", c.get("background-color"));
        assertEquals("var(--ash-space-2)", c.get("padding-block"));
        assertEquals("var(--ash-space-3)", c.get("padding-left"));
        assertEquals("var(--ash-space-3)", c.get("padding-right"));
        assertEquals("inherit", c.get("font"));
        assertEquals("inherit", c.get("letter-spacing"));
        assertEquals("0.875rem", c.get("font-size"));
        assertEquals("var(--ash-field-leading)", c.get("line-height"));
        assertEquals("var(--ash-field-fg, var(--color-slate-900, oklch(20.8% 0.042 265.755)))", c.get("color"));
        assertEquals("1", c.get("opacity"));
        assertEquals("var(--ash-field-shadow, " + SHADOW + ")", c.get("box-shadow"));
        assertEquals("calc(1.25 / 0.875)", declarations("field", ".ash-field").get("--ash-field-leading"));
        assertEquals("2.25rem", declarations("field", ".ash-field__control--leading").get("padding-left"));
        assertEquals("2.25rem", declarations("field", ".ash-field__control--trailing").get("padding-right"));
        assertEquals("24rem", declarations("field", ".ash-field__control--narrow").get("max-width"));
        assertEquals("24rem", declarations("field", ".ash-field__box").get("max-width"));
    }

    @Test
    void theFocusRingIsAnOuterTwoPixelShadowKeptInFrontOfTheRestingShadowAndTheOutlineIsRemoved() throws IOException {
        Map<String, String> f = declarations("field", CTRL + ":focus");
        assertEquals("none", f.get("outline-style"));
        assertEquals("0 0 0 2px var(--ash-field-ring, var(--color-blue-500, oklch(62.3% 0.214 259.815))), var(--ash-field-shadow, " + SHADOW + ")", f.get("box-shadow"));
        assertEquals("var(--ash-field-ring, var(--color-blue-500, oklch(62.3% 0.214 259.815)))", f.get("border-color"));
        for (String selector : List.of(CTRL + ":user-invalid:focus", CTRL + "[aria-invalid=\"true\"]:focus")) {
            Map<String, String> d = declarations("field", selector);
            assertEquals("0 0 0 2px var(--ash-field-invalid, var(--color-red-500, oklch(63.7% 0.237 25.331))), var(--ash-field-shadow, " + SHADOW + ")", d.get("box-shadow"), selector);
        }
    }

    @Test
    void disabledAndReadOnlyKeepTheirPaletteValues() throws IOException {
        Map<String, String> d = declarations("field", CTRL + ":disabled");
        assertEquals("not-allowed", d.get("cursor"));
        assertEquals("var(--color-slate-200, oklch(92.9% 0.013 255.508))", d.get("border-color"));
        assertEquals("var(--color-slate-50, oklch(98.4% 0.003 247.858))", d.get("background-color"));
        assertEquals("var(--color-slate-400, oklch(70.4% 0.04 256.788))", d.get("color"));
        Map<String, String> r = declarations("field", RO + ":read-only");
        assertEquals("default", r.get("cursor"));
        assertEquals("var(--color-slate-50, oklch(98.4% 0.003 247.858))", r.get("background-color"));
        assertEquals("var(--color-slate-900, oklch(20.8% 0.042 265.755))", declarations("field", RO + DARK + ":read-only").get("background-color"));
    }

    @Test
    void thePlaceholderIsAtFullOpacityInTheMeasuredColour() throws IOException {
        Map<String, String> p = declarations("field", CTRL + "::placeholder");
        assertEquals("var(--ash-field-placeholder, var(--color-slate-400, oklch(70.4% 0.04 256.788)))", p.get("color"));
        assertEquals("1", p.get("opacity"));
    }

    // ---- scaffolding ----

    @Test
    void theScaffoldingIsTheMeasuredLabelHelpErrorAndRequiredMarker() throws IOException {
        Map<String, String> field = declarations("field", ".ash-field");
        assertEquals("0 0 var(--ash-space-6)", field.get("margin"));
        Map<String, String> label = declarations("field", ".ash-field__label");
        assertEquals("block", label.get("display"));
        assertEquals("0 0 var(--ash-space-2)", label.get("margin"));
        assertEquals("var(--font-weight-medium, 500)", label.get("font-weight"));
        assertEquals("0.25rem 0 0", declarations("field", ".ash-field__help").get("margin"));
        assertEquals("0.25rem 0 0", declarations("field", ".ash-field__error").get("margin"));
        assertEquals("var(--ash-field-error, var(--color-red-600, oklch(57.7% 0.245 27.325)))", declarations("field", ".ash-field__required").get("color"));
        Map<String, String> icon = declarations("field", ".ash-field__icon");
        assertEquals("none", icon.get("pointer-events"));
        assertEquals("absolute", icon.get("position"));
        assertEquals("var(--ash-space-3)", icon.get("padding-left"));
        Map<String, String> end = declarations("field", ".ash-field__icon--end");
        assertEquals("auto", end.get("left"));
        assertEquals("0", end.get("right"));
    }

    // ---- customisation by ancestor ----

    @Test
    void thePublicPropertiesAreOnlyReadNeverDeclaredSoAnAncestorCanSetThem() throws IOException {
        List<String> publicProps = List.of("shadow", "border", "bg", "fg", "placeholder", "ring", "invalid", "invalid-hover", "label", "help", "error");
        for (String name : List.of("field", "search-input", "textarea")) {
            String s = stripped(name);
            for (String prop : publicProps) {
                assertFalse(Pattern.compile("(^|[;{\\s])--ash-field-" + Pattern.quote(prop) + "\\s*:").matcher(s).find(), name + " must not declare --ash-field-" + prop);
                assertEquals(false, s.contains("var(--ash-field-" + prop + ")"), name + ": every read of --ash-field-" + prop + " has a fallback");
            }
        }
        assertTrue(stripped("field").contains("var(--ash-field-shadow, " + SHADOW + ")"));
        // The only declared custom properties are the two internal line-height ratios.
        List<String> declared = new ArrayList<>();
        for (String name : List.of("field", "search-input", "textarea")) {
            Matcher m = Pattern.compile("(?m)^\\s*(--[a-z-]+)\\s*:").matcher(stripped(name));
            while (m.find()) {
                declared.add(name + ":" + m.group(1));
            }
        }
        assertEquals(List.of("field:--ash-field-leading", "textarea:--ash-textarea-count-leading"), declared);
    }

    @Test
    void everyPaletteColourHasItsFallbackAndDarkIsTheZeroSpecificityForm() throws IOException {
        for (String name : List.of("field", "search-input", "textarea")) {
            String s = stripped(name);
            Matcher m = Pattern.compile("var\\(\\s*(--color-[a-z]+(?:-\\d+)?)\\s*(,)?").matcher(s);
            int n = 0;
            while (m.find()) {
                assertEquals(",", m.group(2), name + ": " + m.group(1) + " has a fallback");
                n++;
            }
            assertTrue(n > 3, name);
            assertFalse(s.contains(".dark") || s.contains("prefers-color-scheme"), name);
            String withoutWhere = s.replace(":where([data-theme=\"dark\"], [data-theme=\"dark\"] *)", "");
            assertFalse(withoutWhere.contains("data-theme"), name + ": a dark selector outside the zero-specificity :where() form");
        }
    }

    // ---- Search Input ----

    @Test
    void theSearchInputHidesItsClearButtonWithTheNativeAttributeAndAnExplicitRule() throws IOException {
        Map<String, String> clear = declarations("search-input", ".ash-search-input__clear");
        assertEquals("flex", clear.get("display"));
        assertEquals("button", clear.get("appearance"));
        assertEquals("transparent", clear.get("background-color"));
        assertEquals("0 solid", clear.get("border"));
        assertEquals("0 var(--ash-space-3) 0 0", clear.get("padding"));
        assertEquals("none", declarations("search-input", ".ash-search-input__clear[hidden]").get("display"), "an author display: flex would override the attribute's own rule, so it is stated");
        assertEquals("none", declarations("search-input", ".ash-search-input__clear:focus").get("outline-style"));
        assertEquals("0 0 0 2px var(--color-blue-500, oklch(62.3% 0.214 259.815))", declarations("search-input", ".ash-search-input__clear:focus-visible").get("box-shadow"));
        assertEquals("0.4", declarations("search-input", ".ash-search-input__clear:disabled").get("opacity"));
        assertEquals("none", declarations("search-input", ".ash-search-input__field::-webkit-search-cancel-button").get("appearance"));
        assertEquals("2.25rem", declarations("search-input", ".ash-search-input__field").get("padding-left"));
        assertEquals("2.25rem", declarations("search-input", ".ash-search-input__field").get("padding-right"));
        // The hidden rule is defined after the display it must beat.
        String s = stripped("search-input");
        assertTrue(s.indexOf(".ash-search-input__clear[hidden]") > s.indexOf(".ash-search-input__clear {"));
    }

    // ---- Textarea ----

    @Test
    void theTextareaResizesVerticallyUnlessFixedOrDisabledAndItsCountTurnsRedFromAnAttribute() throws IOException {
        assertEquals("vertical", declarations("textarea", ".ash-textarea__control").get("resize"));
        assertEquals("none", declarations("textarea", ".ash-textarea__control--fixed").get("resize"));
        assertEquals("none", declarations("textarea", ".ash-textarea__control:disabled").get("resize"));
        Map<String, String> row = declarations("textarea", ".ash-textarea__row");
        assertEquals("flex", row.get("display"));
        assertEquals("space-between", row.get("justify-content"));
        assertEquals("flex-start", row.get("align-items"));
        assertEquals("var(--ash-space-2)", row.get("gap"));
        assertEquals("0.25rem", row.get("margin-top"));
        Map<String, String> count = declarations("textarea", ".ash-textarea__count");
        assertEquals("0", count.get("flex-shrink"));
        assertEquals("0.75rem", count.get("font-size"));
        assertEquals("calc(1 / 0.75)", count.get("--ash-textarea-count-leading"));
        assertEquals("var(--ash-field-error, var(--color-red-600, oklch(57.7% 0.245 27.325)))", declarations("textarea", ".ash-textarea__count[data-at-limit]").get("color"));
        assertEquals("var(--ash-field-error, var(--color-red-400, oklch(70.4% 0.191 22.216)))", declarations("textarea", ".ash-textarea__count[data-at-limit]" + DARK).get("color"));
        String s = stripped("textarea");
        assertTrue(s.indexOf(".ash-textarea__count[data-at-limit]" + DARK) > s.indexOf(".ash-textarea__count" + DARK + " {"), "the dark at-limit colour comes after the dark resting colour");
    }

    // ---- placement ----

    @Test
    void theEntryFileImportsEachInTheComponentsLayerAndTheTokensAreNotTouched() throws IOException {
        String entry = read("src/main/css/ashlar.css");
        for (String name : List.of("field", "search-input", "textarea")) {
            assertTrue(entry.contains("@import \"components/" + name + ".css\" layer(ash.components);"), name);
        }
        assertTrue(entry.indexOf("components/field.css") < entry.indexOf("components/search-input.css") && entry.indexOf("components/field.css") < entry.indexOf("components/textarea.css"),
                "the shared chrome comes before what builds on it, so an equal-specificity override by the specific sheet wins");
        String tokens = read("src/main/css/tokens/tokens.css");
        assertFalse(tokens.contains("--ash-field") || tokens.contains("--ash-textarea") || tokens.contains("--ash-search"), "the family adds nothing to the shared tokens");
    }
}
