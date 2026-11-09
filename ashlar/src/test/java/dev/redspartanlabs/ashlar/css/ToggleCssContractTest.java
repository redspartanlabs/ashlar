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
 * Pins toggle.css, migrated with the Choice Controls (PR 2). The rendered result is measured in a real browser by the showcase's {@code ToggleParityTest}, {@code TogglePixelTest},
 * {@code ToggleTailwindFreeTest}, {@code ToggleBehaviourTest}, {@code ToggleMotionTest} and {@code ToggleCustomPropertiesTest}; this reads the source only: that every selector belongs to
 * Toggle, that the state is the input's native selectors read through the sibling combinator, in the cascade order that was measured, the geometry, the motion, the public properties
 * (only read, never declared) and the palette fallbacks.
 */
class ToggleCssContractTest {

    private static final String TRACK = ".ash-toggle__track";
    private static final String THUMB = ".ash-toggle__thumb";
    private static final String INPUT = ".ash-toggle__input";
    private static final String HOVER = "@media (hover: hover)";
    private static final String REDUCED = "@media (prefers-reduced-motion: reduce)";
    private static final String SLATE_300 = "oklch(86.9% 0.022 252.894)";
    private static final String SLATE_400 = "oklch(70.4% 0.04 256.788)";
    private static final String SLATE_500 = "oklch(55.4% 0.046 257.417)";
    private static final String SLATE_600 = "oklch(44.6% 0.043 257.281)";
    private static final String BLUE_400 = "oklch(70.7% 0.165 254.624)";
    private static final String BLUE_500 = "oklch(62.3% 0.214 259.815)";
    private static final String BLUE_600 = "oklch(54.6% 0.245 262.881)";
    private static final String BLUE_700 = "oklch(48.8% 0.243 264.376)";
    private static final String RED_500 = "oklch(63.7% 0.237 25.331)";

    private static String stripped() throws IOException {
        return css("toggle").replaceAll("(?s)/\\*.*?\\*/", "");
    }

    // ---- scoping ----

    @Test
    void everySelectorBelongsToToggleAndNothingGlobalIsWritten() throws IOException {
        String s = stripped();
        assertFalse(s.contains("!important") || s.contains("@import") || s.contains("@layer") || s.contains("@keyframes"), "no important, no import, no layer, no keyframes");
        assertFalse(s.replace(DARK, "").contains("data-theme"), "dark is the zero-specificity :where() form");
        assertFalse(Pattern.compile("(^|[,{}]\\s*)(\\*|html|body|button|div|span|svg|input|label|p)\\s*[,{:.\\[]").matcher(s).find(), "no bare element selector");
        List<CssScanner.Rule> rules = rules("toggle");
        assertEquals(23, rules.size());
        for (CssScanner.Rule r : rules) {
            for (String part : r.selector().replace(DARK, "").split(",")) {
                assertTrue(part.trim().startsWith(".ash-toggle"), "a selector that does not start with a Toggle class: " + r.selector());
            }
        }
        assertEquals(List.of(HOVER, REDUCED), rules.stream().map(CssScanner.Rule::media).filter(m -> !m.isEmpty()).distinct().toList(), "the only at-rules are hover and reduced motion");
    }

    @Test
    void thereIsNoStateClassAndNoScriptHookAndTheCheckedStateIsTheBrowsers() throws IOException {
        String s = stripped();
        assertFalse(s.contains(".is-") || s.contains("--checked") || s.contains("--disabled") || s.contains("--invalid") || s.contains("data-toggle"), "state is the input's native selectors");
        assertTrue(s.contains(INPUT + ":checked ~ " + TRACK) && s.contains(INPUT + ":checked ~ " + THUMB) && s.contains(INPUT + ":disabled ~ " + TRACK)
                && s.contains(INPUT + ":focus-visible ~ " + TRACK) && s.contains(INPUT + "[aria-invalid=\"true\"] ~ " + TRACK));
        assertFalse(s.contains("+ " + TRACK), "the track and thumb are reached as later siblings of the input, not only the adjacent one");
    }

    // ---- layout ----

    @Test
    void theWrapperTheLabelRowAndTheLabelPositionAreTheMeasuredOnes() throws IOException {
        assertEquals(Map.of("margin", "0 0 var(--ash-space-4)"), declarations("toggle", ".ash-toggle"));
        Map<String, String> label = declarations("toggle", ".ash-toggle__label");
        assertEquals(List.of("display", "align-items", "gap", "cursor"), List.copyOf(label.keySet()));
        assertEquals("inline-flex", label.get("display"));
        assertEquals("center", label.get("align-items"));
        assertEquals("var(--ash-space-3)", label.get("gap"));
        assertEquals("pointer", label.get("cursor"));
        assertEquals(Map.of("cursor", "not-allowed"), declarations("toggle", ".ash-toggle__label:has(:disabled)"));
        assertEquals(Map.of("flex-direction", "row-reverse"), declarations("toggle", ".ash-toggle__label--start"));
    }

    @Test
    void theSwitchIs44By24AndTheThumbIs20By20TwoPixelsInAndMovesTwentyPixelsWhenChecked() throws IOException {
        Map<String, String> sw = declarations("toggle", ".ash-toggle__switch");
        assertEquals("relative", sw.get("position"));
        assertEquals("inline-flex", sw.get("display"));
        assertEquals("0", sw.get("flex-shrink"));
        assertEquals("center", sw.get("align-items"));
        assertEquals("2.75rem", sw.get("width"));
        assertEquals("1.5rem", sw.get("height"));
        Map<String, String> thumb = declarations("toggle", THUMB);
        assertEquals("absolute", thumb.get("position"));
        assertEquals("0.125rem", thumb.get("top"));
        assertEquals("0.125rem", thumb.get("left"));
        assertEquals("1.25rem", thumb.get("width"));
        assertEquals("1.25rem", thumb.get("height"));
        assertEquals("none", thumb.get("pointer-events"));
        assertEquals("var(--color-white, #fff)", thumb.get("background-color"));
        assertEquals(Map.of("translate", "1.25rem"), declarations("toggle", INPUT + ":checked ~ " + THUMB));
        Map<String, String> track = declarations("toggle", TRACK);
        assertEquals("absolute", track.get("position"));
        assertEquals("0", track.get("inset"));
    }

    @Test
    void bothShapesAreFullyRoundedWithTheSameInfinityTailwindUses() throws IOException {
        assertEquals("calc(infinity * 1px)", declarations("toggle", TRACK).get("border-radius"));
        assertEquals("calc(infinity * 1px)", declarations("toggle", THUMB).get("border-radius"));
    }

    // ---- colour and state precedence ----

    @Test
    void theTrackColoursAreTheMeasuredOnesInLightAndDarkAndEachReadsItsPublicProperty() throws IOException {
        assertEquals("var(--ash-toggle-track-off, var(--color-slate-300, " + SLATE_300 + "))", declarations("toggle", TRACK).get("background-color"));
        assertEquals("var(--ash-toggle-track-off, var(--color-slate-600, " + SLATE_600 + "))", declarations("toggle", TRACK + DARK).get("background-color"));
        assertEquals("var(--ash-toggle-track-on, var(--color-blue-600, " + BLUE_600 + "))", declarations("toggle", INPUT + ":checked ~ " + TRACK).get("background-color"));
        assertEquals("var(--ash-toggle-track-on, var(--color-blue-500, " + BLUE_500 + "))", declarations("toggle", INPUT + ":checked ~ " + TRACK + DARK).get("background-color"));
    }

    @Test
    void hoverIsInsideTheHoverMediaQueryNeverAppliesToADisabledToggleAndCheckedHoverOutranksUncheckedHover() throws IOException {
        String rest = INPUT + ":not(:disabled) ~ " + TRACK + ":hover";
        String checked = INPUT + ":not(:disabled):checked ~ " + TRACK + ":hover";
        assertEquals("var(--ash-toggle-track-off, var(--color-slate-400, " + SLATE_400 + "))", declarations("toggle", rest, HOVER).get("background-color"));
        assertEquals("var(--ash-toggle-track-off, var(--color-slate-500, " + SLATE_500 + "))", declarations("toggle", rest + DARK, HOVER).get("background-color"));
        assertEquals("var(--ash-toggle-track-on, var(--color-blue-700, " + BLUE_700 + "))", declarations("toggle", checked, HOVER).get("background-color"));
        assertEquals("var(--ash-toggle-track-on, var(--color-blue-400, " + BLUE_400 + "))", declarations("toggle", checked + DARK, HOVER).get("background-color"));
        String s = stripped().replaceAll("\\s+", " ");
        assertEquals(1, count(s, "@media (hover: hover) {"), "one hover block");
        assertFalse(s.substring(0, s.indexOf("@media (hover: hover)")).contains(":hover"), "no hover rule outside the media query");
        assertTrue(s.indexOf(rest + " {") < s.indexOf(rest + DARK + " {") && s.indexOf(checked + " {") < s.indexOf(checked + DARK + " {"), "dark follows light, so it wins at equal specificity");
        assertTrue(s.indexOf(rest + DARK + " {") < s.indexOf(checked + " {"), "the checked hover rules come after the unchecked ones (and outrank them)");
    }

    @Test
    void theCascadeOrderOfRestCheckedDisabledInvalidAndFocusIsTheMeasuredOne() throws IOException {
        List<String> selectors = rules("toggle").stream().filter(x -> x.media().isEmpty()).map(CssScanner.Rule::selector).toList();
        List<String> ordered = List.of(TRACK, TRACK + DARK, INPUT + ":checked ~ " + TRACK, INPUT + ":checked ~ " + TRACK + DARK, INPUT + ":disabled ~ " + TRACK,
                INPUT + "[aria-invalid=\"true\"] ~ " + TRACK, INPUT + ":focus-visible ~ " + TRACK, INPUT + "[aria-invalid=\"true\"]:focus-visible ~ " + TRACK);
        int at = -1;
        for (String marker : ordered) {
            int found = selectors.indexOf(marker);
            assertTrue(found > at, "out of order or missing: " + marker);
            assertEquals(found, selectors.lastIndexOf(marker), "defined once: " + marker);
            at = found;
        }
        assertEquals(Map.of("opacity", "0.6"), declarations("toggle", INPUT + ":disabled ~ " + TRACK), "a disabled toggle dims the track only");
    }

    @Test
    void theFocusRingIsAWhiteOffsetThenTwoPixelsOfFocusColourAndAnInvalidToggleDrawsARedRingOfItsOwn() throws IOException {
        assertEquals("0 0 0 2px #fff, 0 0 0 4px var(--ash-toggle-ring, var(--color-blue-500, " + BLUE_500 + "))",
                declarations("toggle", INPUT + ":focus-visible ~ " + TRACK).get("box-shadow"), "the offset is the literal white, in dark mode too");
        assertEquals("0 0 0 2px var(--color-red-500, " + RED_500 + ")", declarations("toggle", INPUT + "[aria-invalid=\"true\"] ~ " + TRACK).get("box-shadow"));
        assertEquals("0 0 0 2px #fff, 0 0 0 4px var(--color-red-500, " + RED_500 + ")", declarations("toggle", INPUT + "[aria-invalid=\"true\"]:focus-visible ~ " + TRACK).get("box-shadow"),
                "invalid and keyboard-focused: the white offset and a red ring, as the later invalid colour always won");
        assertFalse(stripped().contains("outline-style") || stripped().contains("outline:"), "the hidden input's own outline cannot be seen, so none is touched (the track has none)");
    }

    // ---- motion ----

    @Test
    void theTrackFadesItsColourAndTheThumbSlidesFor150MillisecondsWithTheStandardEaseAndReducedMotionTurnsBothOff() throws IOException {
        Map<String, String> track = declarations("toggle", TRACK);
        assertEquals("color, background-color, border-color, outline-color, text-decoration-color, fill, stroke, --tw-gradient-from, --tw-gradient-via, --tw-gradient-to", track.get("transition-property"),
                "the list Tailwind's transition-colors computed to, kept so the computed style is identical");
        assertEquals("150ms", track.get("transition-duration"));
        assertEquals("cubic-bezier(0.4, 0, 0.2, 1)", track.get("transition-timing-function"));
        Map<String, String> thumb = declarations("toggle", THUMB);
        assertEquals("transform, translate, scale, rotate", thumb.get("transition-property"));
        assertEquals("150ms", thumb.get("transition-duration"));
        assertEquals("cubic-bezier(0.4, 0, 0.2, 1)", thumb.get("transition-timing-function"));
        assertEquals(Map.of("transition-property", "none"), declarations("toggle", ".ash-toggle__track,\n  .ash-toggle__thumb", REDUCED));
    }

    // ---- the thumb's shadow ----

    @Test
    void theThumbShadowReadsItsPublicPropertyWithTailwindsShadowSmAsTheFallback() throws IOException {
        assertEquals("var(--ash-toggle-thumb-shadow, 0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1))", declarations("toggle", THUMB).get("box-shadow"));
    }

    // ---- text ----

    @Test
    void theLabelTextHelpAndErrorAreTheMeasuredText() throws IOException {
        Map<String, String> text = declarations("toggle", ".ash-toggle__text");
        assertEquals("0.875rem", text.get("font-size"));
        assertEquals("var(--ash-toggle-leading)", text.get("line-height"));
        assertEquals("calc(1.25 / 0.875)", text.get("--ash-toggle-leading"));
        assertEquals("var(--font-weight-medium, 500)", text.get("font-weight"));
        assertFalse(text.containsKey("cursor"), "the text inherits the label's cursor, so a disabled toggle's text shows not-allowed");
        assertEquals("var(--ash-toggle-label-color, var(--color-slate-700, oklch(37.2% 0.044 257.287)))", text.get("color"));
        assertEquals("var(--ash-toggle-label-color, var(--color-slate-300, " + SLATE_300 + "))", declarations("toggle", ".ash-toggle__text" + DARK).get("color"));
        assertEquals(Map.of("margin-top", "0.25rem"), declarations("toggle", ".ash-toggle__help,\n.ash-toggle__error"), "T1: the legacy 4px, kept (a Checkbox's is 2px and a Radio Group's 8px)");
    }

    // ---- customisation by ancestor ----

    @Test
    void thePublicPropertiesAreOnlyReadNeverDeclaredSoAnAncestorCanSetThem() throws IOException {
        String s = stripped();
        List<String> publicProps = List.of("track-off", "track-on", "thumb-shadow", "ring", "label-color");
        for (String prop : publicProps) {
            assertFalse(Pattern.compile("(^|[;{\\s])--ash-toggle-" + Pattern.quote(prop) + "\\s*:").matcher(s).find(), "must not declare --ash-toggle-" + prop);
            assertFalse(s.contains("var(--ash-toggle-" + prop + ")"), "every read of --ash-toggle-" + prop + " has a fallback");
            assertTrue(s.contains("var(--ash-toggle-" + prop + ","), "--ash-toggle-" + prop + " is read");
        }
        Matcher declared = Pattern.compile("(?m)^\\s*(--[a-z-]+)\\s*:").matcher(s);
        List<String> found = new java.util.ArrayList<>();
        while (declared.find()) {
            found.add(declared.group(1));
        }
        assertEquals(List.of("--ash-toggle-leading"), found, "the only declared custom property is the internal line-height ratio");
        Matcher reads = Pattern.compile("var\\(--ash-toggle-([a-z-]+)").matcher(s);
        while (reads.find()) {
            assertTrue(publicProps.contains(reads.group(1)) || reads.group(1).equals("leading"), "an undocumented property: " + reads.group(1));
        }
    }

    @Test
    void everyPaletteColourHasItsFallback() throws IOException {
        Matcher m = Pattern.compile("var\\(\\s*(--color-[a-z]+(?:-\\d+)?)\\s*(,)?").matcher(stripped());
        int n = 0;
        while (m.find()) {
            assertEquals(",", m.group(2), m.group(1) + " has a fallback");
            n++;
        }
        assertTrue(n >= 14, "palette reads: " + n);
    }

    // ---- placement ----

    @Test
    void theEntryFileImportsItInTheComponentsLayerAfterTheSharedChoiceSheetAndTheTokensAreNotTouched() throws IOException {
        String entry = read("src/main/css/ashlar.css");
        assertTrue(entry.contains("@import \"components/toggle.css\" layer(ash.components);"));
        assertTrue(entry.indexOf("components/choice.css") < entry.indexOf("components/toggle.css"), "the shared sheet comes first, so Toggle's equal-specificity margin override wins");
        assertTrue(entry.indexOf("components/field.css") < entry.indexOf("components/toggle.css"), "the required marker (ash-field__required) is defined before it is used");
        assertTrue(entry.indexOf("components/toggle.css") < entry.indexOf("utilities/sr-only.css"), "ash-sr-only is in the later utilities layer, so it wins over anything here");
        assertFalse(read("src/main/css/tokens/tokens.css").contains("--ash-toggle"), "Toggle adds nothing to the shared tokens");
    }

    private static int count(String s, String what) {
        int n = 0;
        for (int at = s.indexOf(what); at >= 0; at = s.indexOf(what, at + 1)) {
            n++;
        }
        return n;
    }
}
