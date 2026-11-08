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
 * Pins the stylesheets of Checkbox and Radio Group (choice.css, checkbox.css, radio-group.css). The rendered result is measured in a real browser by the showcase's
 * {@code ChoiceParityTest}, {@code ChoicePixelTest}, {@code ChoiceTailwindFreeTest} and {@code ChoiceBehaviourTest}; this reads the sources only: that every selector belongs to the
 * family, that the native control is left native (no appearance, no border, no radius), that its states are the native selectors in the order they were measured, that there is no
 * hover and no motion, that customisation is by ancestor (the public properties are only read), and that every palette colour has its fallback.
 */
class ChoiceCssContractTest {

    private static final String INPUT = ".ash-choice__input";
    private static final String BLUE_600 = "var(--ash-choice-accent, var(--color-blue-600, oklch(54.6% 0.245 262.881)))";
    private static final String RED_600 = "var(--ash-choice-invalid, var(--color-red-600, oklch(57.7% 0.245 27.325)))";

    private static String stripped(String name) throws IOException {
        return css(name).replaceAll("(?s)/\\*.*?\\*/", "");
    }

    private static void assertScoped(String name, int expectedRules) throws IOException {
        String s = stripped(name);
        assertFalse(s.contains("!important") || s.contains("@import") || s.contains("@layer") || s.contains("@keyframes") || s.contains("@media"), name);
        assertFalse(s.contains("data-checkbox") || s.contains("data-theme=\"dark\"]:") , name + ": styling is by class and native state; dark is the :where() form");
        assertFalse(Pattern.compile("(^|[,{}]\\s*)(\\*|html|body|button|div|span|svg|input|label|p|fieldset|legend)\\s*[,{:.\\[]").matcher(s).find(), name + ": no bare element selector");
        List<CssScanner.Rule> rules = rules(name);
        assertEquals(expectedRules, rules.size(), name);
        for (CssScanner.Rule r : rules) {
            assertTrue(Pattern.compile("^\\.ash-(choice|checkbox|radio-group)").matcher(r.selector()).find(), name + ": a selector that does not start with a Checkbox or Radio Group class: " + r.selector());
        }
    }

    // ---- scoping ----

    @Test
    void everySelectorIsScopedToTheFamilysOwnClassesAndThereIsNoMediaHoverOrMotion() throws IOException {
        assertScoped("choice", 16);
        assertScoped("checkbox", 1);
        assertScoped("radio-group", 10);
        for (String name : List.of("choice", "checkbox", "radio-group")) {
            assertFalse(stripped(name).contains(":hover") || stripped(name).contains("transition") || stripped(name).contains("animation"), name + ": the family has no hover rule and no motion");
        }
    }

    // ---- the control stays native ----

    @Test
    void theControlIsLeftNativeAndDeclaresOnlyWhatPaintsOrSizesIt() throws IOException {
        Map<String, String> base = declarations("choice", INPUT);
        assertEquals(List.of("flex-shrink", "margin", "width", "height", "background-color", "accent-color"), List.copyOf(base.keySet()),
                "no appearance, no border and no radius: a native-appearance control ignores both of those, which the pixel suite proves");
        assertEquals("0", base.get("flex-shrink"));
        assertEquals("0.125rem 0 0", base.get("margin"));
        assertEquals("1rem", base.get("width"));
        assertEquals("1rem", base.get("height"));
        assertEquals("var(--color-white, #fff)", base.get("background-color"));
        assertEquals(BLUE_600, base.get("accent-color"));
        assertEquals("var(--color-slate-800, oklch(27.9% 0.041 260.031))", declarations("choice", INPUT + DARK).get("background-color"));
        String all = stripped("choice") + stripped("checkbox") + stripped("radio-group");
        assertFalse(all.contains("appearance") || all.contains("border-radius") || all.contains("border:") || all.contains("border-color"), "nothing is drawn over the native control");
    }

    @Test
    void theStatesAreTheNativeSelectorsInTheMeasuredCascadeOrder() throws IOException {
        String s = stripped("choice").replaceAll("\\s+", " ");
        List<String> ordered = List.of(INPUT + " { flex-shrink", INPUT + DARK + " {", INPUT + "[type=\"checkbox\"] {", INPUT + ":focus {", INPUT + ":focus-visible {", INPUT + ":disabled {",
                INPUT + ":user-invalid {", INPUT + "[aria-invalid=\"true\"] {");
        int at = -1;
        for (String marker : ordered) {
            int found = s.indexOf(marker);
            assertTrue(found > at, "out of order, missing or duplicated: " + marker + " (after position " + at + ", found " + found + ")");
            assertEquals(found, s.lastIndexOf(marker), "defined once: " + marker);
            at = found;
        }
        assertFalse(s.contains(".is-") || s.contains("--invalid {") || s.contains("--disabled") || s.contains("--checked") || s.contains(":checked"), "no state class, and the check is the browser's");
        assertFalse(s.contains(":not("), "no negation that would change specificity");
    }

    @Test
    void theFocusRingIsAWhiteOffsetThenTwoPixelsOfFocusColourOnFocusVisibleOnlyAndTheOutlineIsRemovedOnFocus() throws IOException {
        assertEquals("none", declarations("choice", INPUT + ":focus").get("outline-style"));
        Map<String, String> ring = declarations("choice", INPUT + ":focus-visible");
        assertEquals(List.of("box-shadow"), List.copyOf(ring.keySet()));
        assertEquals("0 0 0 2px #fff, 0 0 0 4px var(--ash-choice-ring, var(--color-blue-500, oklch(62.3% 0.214 259.815)))", ring.get("box-shadow"),
                "the offset is the literal white, in dark mode too, and the ring reads the focus colour");
    }

    @Test
    void disabledInvalidAndCursorKeepTheirMeasuredValues() throws IOException {
        Map<String, String> disabled = declarations("choice", INPUT + ":disabled");
        assertEquals("not-allowed", disabled.get("cursor"));
        assertEquals("0.6", disabled.get("opacity"));
        assertEquals("pointer", declarations("choice", INPUT + "[type=\"checkbox\"]").get("cursor"), "a checkbox has a pointer cursor and a radio has none of its own");
        assertEquals(RED_600, declarations("choice", INPUT + ":user-invalid").get("accent-color"));
        assertEquals(RED_600, declarations("choice", INPUT + "[aria-invalid=\"true\"]").get("accent-color"));
    }

    // ---- text ----

    @Test
    void theLabelDescriptionAndErrorAreTheMeasuredFourteenPixelText() throws IOException {
        Map<String, String> label = declarations("choice", ".ash-choice__label");
        assertEquals("pointer", label.get("cursor"));
        assertEquals("0.875rem", label.get("font-size"));
        assertEquals("var(--ash-choice-leading)", label.get("line-height"));
        assertEquals("calc(1.25 / 0.875)", label.get("--ash-choice-leading"));
        assertEquals("var(--font-weight-medium, 500)", label.get("font-weight"));
        assertEquals("var(--ash-choice-label, var(--color-slate-700, oklch(37.2% 0.044 257.287)))", label.get("color"));
        assertEquals("var(--ash-choice-label, var(--color-slate-300, oklch(86.9% 0.022 252.894)))", declarations("choice", ".ash-choice__label" + DARK).get("color"));
        Map<String, String> description = declarations("choice", ".ash-choice__description");
        assertEquals("0.125rem 0 0", description.get("margin"), "a Checkbox's help text and a Radio option's description sit 2px under their label");
        assertEquals("var(--ash-choice-description, var(--color-slate-500, oklch(55.4% 0.046 257.417)))", description.get("color"));
        assertEquals("var(--ash-choice-description, var(--color-slate-400, oklch(70.4% 0.04 256.788)))", declarations("choice", ".ash-choice__description" + DARK).get("color"));
        Map<String, String> error = declarations("choice", ".ash-choice__error");
        assertEquals("0.125rem 0 0", error.get("margin"));
        assertEquals("var(--ash-choice-error, var(--color-red-600, oklch(57.7% 0.245 27.325)))", error.get("color"));
        assertEquals("var(--ash-choice-error, var(--color-red-400, oklch(70.4% 0.191 22.216)))", declarations("choice", ".ash-choice__error" + DARK).get("color"));
        Map<String, String> row = declarations("choice", ".ash-choice");
        assertEquals("flex", row.get("display"));
        assertEquals("flex-start", row.get("align-items"));
        assertEquals("var(--ash-space-3)", row.get("gap"));
        assertEquals("1 1 0%", declarations("choice", ".ash-choice__body").get("flex"));
    }

    // ---- Checkbox ----

    @Test
    void theCheckboxWrapperIsOneRemBelowAndNothingElse() throws IOException {
        assertEquals(Map.of("margin", "0 0 var(--ash-space-4)"), declarations("checkbox", ".ash-checkbox"));
    }

    // ---- Radio Group ----

    @Test
    void theRadioGroupClearsTheFieldsetsBrowserStylingAndLaysOutItsOptions() throws IOException {
        Map<String, String> fieldset = declarations("radio-group", ".ash-radio-group");
        assertEquals("0 0 var(--ash-space-6)", fieldset.get("margin"));
        assertEquals("0", fieldset.get("border-width"), "the width only, as the old border-0 did: the style is left alone");
        assertEquals("0", fieldset.get("padding"));
        assertEquals("0", fieldset.get("min-width"));
        Map<String, String> legend = declarations("radio-group", ".ash-radio-group__legend");
        assertEquals("block", legend.get("display"));
        assertEquals("0 0 var(--ash-space-2)", legend.get("margin"));
        assertEquals("0", legend.get("padding"));
        assertEquals("calc(1.25 / 0.875)", legend.get("--ash-choice-leading"));
        assertEquals("var(--ash-choice-label, var(--color-slate-300, oklch(86.9% 0.022 252.894)))", declarations("radio-group", ".ash-radio-group__legend" + DARK).get("color"));
        Map<String, String> options = declarations("radio-group", ".ash-radio-group__options");
        assertEquals("flex", options.get("display"));
        assertEquals("column", options.get("flex-direction"));
        assertEquals("var(--ash-space-3)", options.get("gap"));
        Map<String, String> horizontal = declarations("radio-group", ".ash-radio-group__options--horizontal");
        assertEquals("row", horizontal.get("flex-direction"));
        assertEquals("wrap", horizontal.get("flex-wrap"));
        assertEquals("var(--ash-space-8)", horizontal.get("column-gap"));
        assertEquals(Map.of("margin-top", "var(--ash-space-2)"), declarations("radio-group", ".ash-radio-group__help,\n.ash-radio-group__error"), "a Radio Group's own help and error sit 8px under the options");
    }

    @Test
    void aDisabledRadioOptionDimsItsLabelAndDescriptionThroughTheInputsOwnDisabledState() throws IOException {
        String option = ".ash-radio-group .ash-choice__input:disabled + .ash-choice__body ";
        Map<String, String> label = declarations("radio-group", option + ".ash-choice__label");
        assertEquals("not-allowed", label.get("cursor"));
        assertEquals("var(--color-slate-400, oklch(70.4% 0.04 256.788))", label.get("color"));
        assertEquals("var(--color-slate-500, oklch(55.4% 0.046 257.417))", declarations("radio-group", option + ".ash-choice__label" + DARK).get("color"));
        assertEquals("var(--color-slate-300, oklch(86.9% 0.022 252.894))", declarations("radio-group", option + ".ash-choice__description").get("color"));
        assertEquals("var(--color-slate-600, oklch(44.6% 0.043 257.281))", declarations("radio-group", option + ".ash-choice__description" + DARK).get("color"));
        // A Checkbox's label has no such rule: it stays as it is while disabled.
        assertFalse(stripped("checkbox").contains("disabled") && stripped("checkbox").contains("label"), "the checkbox stylesheet never dims a label");
        assertFalse(stripped("choice").contains(".ash-choice__label:disabled") || stripped("choice").contains("input:disabled +"), "the shared stylesheet never dims a label either");
    }

    // ---- customisation by ancestor ----

    @Test
    void thePublicPropertiesAreOnlyReadNeverDeclaredSoAnAncestorCanSetThem() throws IOException {
        List<String> publicProps = List.of("accent", "invalid", "ring", "label", "description", "error");
        for (String name : List.of("choice", "checkbox", "radio-group")) {
            String s = stripped(name);
            for (String prop : publicProps) {
                assertFalse(Pattern.compile("(^|[;{\\s])--ash-choice-" + Pattern.quote(prop) + "\\s*:").matcher(s).find(), name + " must not declare --ash-choice-" + prop);
                assertFalse(s.contains("var(--ash-choice-" + prop + ")"), name + ": every read of --ash-choice-" + prop + " has a fallback");
            }
        }
        List<String> declared = new ArrayList<>();
        for (String name : List.of("choice", "checkbox", "radio-group")) {
            Matcher m = Pattern.compile("(?m)^\\s*(--[a-z-]+)\\s*:").matcher(stripped(name));
            while (m.find()) {
                declared.add(name + ":" + m.group(1));
            }
        }
        assertEquals(List.of("choice:--ash-choice-leading", "choice:--ash-choice-leading", "choice:--ash-choice-leading", "radio-group:--ash-choice-leading"), declared,
                "the only declared custom property is the internal line-height ratio, on each text rule (the Radio Group's help text is outside the row)");
    }

    @Test
    void everyPaletteColourHasItsFallbackAndDarkIsTheZeroSpecificityForm() throws IOException {
        for (String name : List.of("choice", "radio-group")) {
            String s = stripped(name);
            Matcher m = Pattern.compile("var\\(\\s*(--color-[a-z]+(?:-\\d+)?)\\s*(,)?").matcher(s);
            int n = 0;
            while (m.find()) {
                assertEquals(",", m.group(2), name + ": " + m.group(1) + " has a fallback");
                n++;
            }
            assertTrue(n > 5, name);
            assertFalse(s.contains(".dark") || s.contains("prefers-color-scheme"), name);
            assertFalse(s.replace(":where([data-theme=\"dark\"], [data-theme=\"dark\"] *)", "").contains("data-theme"), name + ": a dark selector outside the zero-specificity :where() form");
        }
    }

    // ---- placement ----

    @Test
    void theEntryFileImportsEachInTheComponentsLayerSharedFirstAndTheTokensAreNotTouched() throws IOException {
        String entry = read("src/main/css/ashlar.css");
        for (String name : List.of("choice", "checkbox", "radio-group")) {
            assertTrue(entry.contains("@import \"components/" + name + ".css\" layer(ash.components);"), name);
        }
        assertTrue(entry.indexOf("components/choice.css") < entry.indexOf("components/checkbox.css") && entry.indexOf("components/choice.css") < entry.indexOf("components/radio-group.css"),
                "the shared sheet comes first, so the Radio Group's equal-specificity margin override wins");
        assertTrue(entry.indexOf("components/field.css") < entry.indexOf("components/choice.css"), "the required marker (ash-field__required) is defined before it is used");
        String tokens = read("src/main/css/tokens/tokens.css");
        assertFalse(tokens.contains("--ash-choice") || tokens.contains("--ash-checkbox") || tokens.contains("--ash-radio"), "the family adds nothing to the shared tokens");
    }
}
