package dev.redspartanlabs.ashlar.css;

import org.junit.jupiter.api.Test;

import java.io.IOException;
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
 * Pins the stylesheets of the Facets family (faceted-search.css, facet.css, facet-option.css). The rendered result is measured in a real browser by the showcase's
 * {@code FacetParityTest}, {@code FacetPixelTest}, {@code FacetTailwindFreeTest}, {@code FacetHoverMediaTest}, {@code FacetMotionTest} and {@code FacetBehaviourTest}; this reads the
 * sources only: that every selector belongs to the family, that the native checkbox is left native (no appearance, no border, no radius), that hover is the only conditional and sits
 * under the hover capability, that nothing moves, that no customization property is introduced (the one internal property is the line-height ratio), that dark mode is the zero-specificity
 * {@code :where()} form, and that every palette colour has its fallback.
 */
class FacetCssContractTest {

    private static final List<String> FILES = List.of("faceted-search", "facet", "facet-option");
    private static final String OPTION = ".ash-facet-option";
    private static final String INPUT = ".ash-facet-option__input";
    private static final String HOVER_MEDIA = "@media (hover: hover)";
    private static final String HOVER_ROW = ".ash-facet-option:where(:not(.ash-facet-option--disabled)):hover";

    private static String stripped(String name) throws IOException {
        return css(name).replaceAll("(?s)/\\*.*?\\*/", "");
    }

    // ---- scoping ----

    @Test
    void everySelectorBelongsToTheFamilyAndNoGlobalRuleOrBareElementSelectorExists() throws IOException {
        int total = 0;
        for (String name : FILES) {
            String s = stripped(name);
            assertFalse(s.contains("!important") || s.contains("@import") || s.contains("@layer") || s.contains("@keyframes") || s.contains("@charset"), name);
            assertFalse(s.contains("data-theme=\"dark\"]:") && !s.contains(DARK), name + ": dark is the :where() form");
            assertFalse(Pattern.compile("(^|[,{}]\\s*)(\\*|html|body|button|div|span|svg|input|label|p|fieldset|legend)\\s*[,{:.\\[]").matcher(s).find(), name + ": no bare element selector");
            for (CssScanner.Rule r : rules(name)) {
                total++;
                assertTrue(Pattern.compile("^\\.ash-(faceted-search|facet)(-option)?(__[a-z]+)?(--[a-z]+)?([:\\s]|$)").matcher(r.selector()).find(), name + ": a selector outside the family: " + r.selector());
            }
        }
        assertEquals(1 + 3 + 14, total, "the rules of the three files");
    }

    @Test
    void theStylesheetsAreRegisteredInTheEntryPointInTheAshComponentsLayer() throws IOException {
        String entry = read("src/main/css/ashlar.css");
        for (String name : FILES) {
            assertTrue(entry.contains("@import \"components/" + name + ".css\" layer(ash.components);"), name);
        }
    }

    // ---- the rules ----

    @Test
    void facetedSearchIsAFlexColumnWithAGapAndAZeroMinimumWidth() throws IOException {
        assertEquals(Map.of("display", "flex", "flex-direction", "column", "gap", "1.25rem", "min-width", "0"), declarations("faceted-search", ".ash-faceted-search"));
    }

    @Test
    void aFacetStatesTheFieldsetAndLegendDefaultsThatPreflightUsedToRemove() throws IOException {
        assertEquals(Map.of("margin", "0", "border-width", "0", "padding", "0", "min-width", "0"), declarations("facet", ".ash-facet"));
        Map<String, String> legend = declarations("facet", ".ash-facet__legend");
        assertEquals("block", legend.get("display"));
        assertEquals("0 0 0.375rem", legend.get("margin"));
        assertEquals("0", legend.get("padding"));
        assertEquals("0.875rem", legend.get("font-size"));
        assertEquals("var(--font-weight-semibold, 600)", legend.get("font-weight"));
        assertEquals("var(--color-slate-900, oklch(20.8% 0.042 265.755))", legend.get("color"));
        assertEquals("var(--color-slate-100, oklch(96.8% 0.007 247.896))", declarations("facet", ".ash-facet__legend" + DARK).get("color"));
        assertEquals(Map.of("display", "flex", "flex-direction", "column"), declarations("facet", ".ash-facet__options"));
    }

    @Test
    void theRowIsACenteredFlexRowWithTheTokenRadiusAndTheDisabledLookIsAModifier() throws IOException {
        Map<String, String> row = declarations("facet-option", OPTION);
        assertEquals("flex", row.get("display"));
        assertEquals("center", row.get("align-items"));
        assertEquals("0.625rem", row.get("gap"));
        assertEquals("var(--ash-radius-md)", row.get("border-radius"));
        assertEquals("0.375rem 0.5rem", row.get("padding"));
        assertEquals("0.875rem", row.get("font-size"));
        assertEquals("pointer", row.get("cursor"));
        assertEquals(Map.of("cursor", "not-allowed", "opacity", "0.6"), declarations("facet-option", OPTION + "--disabled"));
        assertFalse(row.containsKey("opacity"), "an enabled row is not dimmed");
    }

    @Test
    void hoverIsTheOnlyConditionalItSitsUnderTheHoverCapabilityAndNeverReachesADisabledRow() throws IOException {
        List<CssScanner.Rule> all = rules("facet-option");
        for (CssScanner.Rule r : all) {
            boolean hover = r.selector().contains(":hover");
            assertEquals(hover ? HOVER_MEDIA : "", r.media(), r.selector());
            if (hover) {
                assertTrue(r.selector().startsWith(HOVER_ROW), "a hover rule is for an enabled row only: " + r.selector());
            }
        }
        assertEquals("var(--color-slate-100, oklch(96.8% 0.007 247.896))", declarations("facet-option", HOVER_ROW, HOVER_MEDIA).get("background-color"));
        assertEquals("var(--color-slate-800, oklch(27.9% 0.041 260.031))", declarations("facet-option", HOVER_ROW + DARK, HOVER_MEDIA).get("background-color"));
    }

    @Test
    void theCheckboxIsLeftNativeAndItsStatesAreTheNativeSelectors() throws IOException {
        Map<String, String> input = declarations("facet-option", INPUT);
        assertEquals("0", input.get("margin"), "the browser's own margin is cleared, with no top offset (the row centers the box)");
        assertEquals("1rem", input.get("width"));
        assertEquals("1rem", input.get("height"));
        assertEquals("0", input.getOrDefault("flex-shrink", "0"));
        assertEquals("var(--color-blue-600, oklch(54.6% 0.245 262.881))", input.get("accent-color"));
        assertEquals("var(--color-white, #fff)", input.get("background-color"));
        assertEquals("pointer", input.get("cursor"));
        assertFalse(input.containsKey("appearance") || input.containsKey("border") || input.containsKey("border-radius") || input.containsKey("opacity"),
                "no appearance, no border, no radius, and no opacity of its own: the disabled row is already dimmed");
        assertEquals("var(--color-slate-800, oklch(27.9% 0.041 260.031))", declarations("facet-option", INPUT + DARK).get("background-color"));
        assertEquals(Map.of("outline-style", "none"), declarations("facet-option", INPUT + ":focus"));
        assertEquals(Map.of("box-shadow", "0 0 0 2px #fff, 0 0 0 4px var(--color-blue-500, oklch(62.3% 0.214 259.815))"), declarations("facet-option", INPUT + ":focus-visible"));
        assertEquals(Map.of("cursor", "not-allowed"), declarations("facet-option", INPUT + ":disabled"));
    }

    @Test
    void theLabelTextAndTheCountHaveTheirTypeAndColours() throws IOException {
        Map<String, String> label = declarations("facet-option", ".ash-facet-option__label");
        assertEquals("1 1 0%", label.get("flex"));
        assertEquals("0", label.get("min-width"));
        assertEquals("break-word", label.get("overflow-wrap"));
        assertEquals("var(--color-slate-700, oklch(37.2% 0.044 257.287))", label.get("color"));
        assertEquals("var(--color-slate-300, oklch(86.9% 0.022 252.894))", declarations("facet-option", ".ash-facet-option__label" + DARK).get("color"));
        Map<String, String> count = declarations("facet-option", ".ash-facet-option__count");
        assertEquals("0", count.get("flex-shrink"));
        assertEquals("0.75rem", count.get("font-size"));
        assertEquals("tabular-nums", count.get("font-variant-numeric"));
        assertEquals("var(--color-slate-500, oklch(55.4% 0.046 257.417))", count.get("color"));
        assertEquals("var(--color-slate-400, oklch(70.4% 0.04 256.788))", declarations("facet-option", ".ash-facet-option__count" + DARK).get("color"));
    }

    // ---- what is NOT there ----

    @Test
    void nothingMovesAndNoCustomizationPropertyIsIntroduced() throws IOException {
        for (String name : FILES) {
            String s = stripped(name);
            assertFalse(s.contains("transition") || s.contains("animation") || s.contains("@media (prefers"), name + ": the family has no motion");
            Matcher property = Pattern.compile("--ash-facet-[a-z-]+").matcher(s);
            while (property.find()) {
                assertEquals("--ash-facet-leading", property.group(), name + ": the only property is the internal line-height ratio");
            }
            Matcher declared = Pattern.compile("(?m)^\\s*(--[a-z-]+)\\s*:").matcher(s);
            while (declared.find()) {
                assertEquals("--ash-facet-leading", declared.group(1), name + ": a custom property is declared");
            }
        }
    }

    @Test
    void everyPaletteColourHasItsFallbackAndNoRawColourOtherThanTheWhiteOffsetIsWritten() throws IOException {
        for (String name : FILES) {
            String s = stripped(name);
            Matcher palette = Pattern.compile("var\\(\\s*--color-[a-z]+(?:-\\d+)?\\s*(,)?").matcher(s);
            while (palette.find()) {
                assertEquals(",", palette.group(1), name + ": a palette variable without its fallback: " + palette.group());
            }
            assertFalse(Pattern.compile("(?<![\\w-])(rgb|hsl|oklch|#)(?![^;{}]*var\\()").matcher(s.replaceAll("var\\([^;{}]*\\)", "")).find() && !s.contains("#fff"),
                    name + ": a raw colour outside a palette fallback");
        }
    }
}
