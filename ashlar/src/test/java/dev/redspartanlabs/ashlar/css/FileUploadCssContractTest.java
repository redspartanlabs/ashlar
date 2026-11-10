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
 * Pins file-upload.css, migrated with the Choice Controls (PR 3). The rendered result is measured in a real browser by the showcase's {@code FileUploadParityTest},
 * {@code FileUploadPixelTest}, {@code FileUploadTailwindFreeTest}, {@code FileUploadBehaviourTest}, {@code FileUploadCustomPropertiesTest} and {@code FileUploadSkinsTest}; this reads the
 * source only: that every selector belongs to File Upload, that the card reacts to its input through {@code :has()} in the cascade order that was measured, the geometry, the motion,
 * the four public properties (only read, never declared) and the palette fallbacks.
 */
class FileUploadCssContractTest {

    private static final String ZONE = ".ash-file-upload__zone";
    private static final String HOVER = "@media (hover: hover)";
    private static final String REDUCED = "@media (prefers-reduced-motion: reduce)";
    private static final String SLATE_50 = "oklch(98.4% 0.003 247.858)";
    private static final String SLATE_100 = "oklch(96.8% 0.007 247.896)";
    private static final String SLATE_300 = "oklch(86.9% 0.022 252.894)";
    private static final String SLATE_400 = "oklch(70.4% 0.04 256.788)";
    private static final String SLATE_500 = "oklch(55.4% 0.046 257.417)";
    private static final String SLATE_600 = "oklch(44.6% 0.043 257.281)";
    private static final String SLATE_700 = "oklch(37.2% 0.044 257.287)";
    private static final String SLATE_800 = "oklch(27.9% 0.041 260.031)";
    private static final String BLUE_500 = "oklch(62.3% 0.214 259.815)";
    private static final String RED_400 = "oklch(70.4% 0.191 22.216)";
    private static final String RED_500 = "oklch(63.7% 0.237 25.331)";

    private static String stripped() throws IOException {
        return css("file-upload").replaceAll("(?s)/\\*.*?\\*/", "");
    }

    private static List<String> order() throws IOException {
        return rules("file-upload").stream().map(r -> r.media() + "|" + r.selector()).toList();
    }

    // ---- scoping ----

    @Test
    void everySelectorBelongsToFileUploadAndNothingGlobalIsWritten() throws IOException {
        String s = stripped();
        assertFalse(s.contains("!important") || s.contains("@import") || s.contains("@layer") || s.contains("@keyframes"), "no important, no import, no layer, no keyframes");
        assertFalse(s.replace(DARK, "").contains("data-theme"), "dark is the zero-specificity :where() form");
        assertFalse(Pattern.compile("(^|[,{}]\\s*)(\\*|html|body|button|div|span|svg|input|label|p)\\s*[,{:.\\[]").matcher(s).find(), "no bare element selector");
        List<CssScanner.Rule> rules = rules("file-upload");
        assertEquals(16, rules.size());
        for (CssScanner.Rule r : rules) {
            for (String part : r.selector().replace(DARK, "").split(",")) {
                assertTrue(part.trim().startsWith(".ash-file-upload"), "a selector that does not start with a File Upload class: " + r.selector());
            }
        }
        assertEquals(List.of(HOVER, REDUCED), rules.stream().map(CssScanner.Rule::media).filter(m -> !m.isEmpty()).distinct().toList(), "the only at-rules are hover and reduced motion");
    }

    @Test
    void theCardReactsToItsInputThroughHasAndThereIsNoStateClassOrScriptHook() throws IOException {
        String s = stripped();
        assertFalse(s.contains(".is-") || s.contains("--invalid") || s.contains("--disabled") || s.contains("data-file-upload"), "state is the input's native selectors");
        for (String state : List.of(":has(:disabled)", ":has(:focus-visible)", ":has([aria-invalid=\"true\"])", ":has(:user-invalid)")) {
            assertTrue(s.contains(ZONE + state) || s.contains(ZONE + ":hover:not(" + state + ")"), state);
        }
        assertFalse(s.contains("~"), "the input is a descendant of the card, not a sibling: no sibling combinator");
    }

    // ---- geometry ----

    @Test
    void theCardIsAFlexColumnOf24RemAtMostWithThe32By24PaddingAndTheDashedTwoPixelBorder() throws IOException {
        Map<String, String> zone = declarations("file-upload", ZONE);
        assertEquals(List.of("box-sizing", "display", "flex-direction", "align-items", "gap", "width", "max-width", "margin", "padding", "border", "border-radius", "background-color", "font-size",
                "line-height", "text-align", "cursor", "transition-property", "transition-duration", "transition-timing-function"), List.copyOf(zone.keySet()));
        assertEquals("border-box", zone.get("box-sizing"));
        assertEquals("flex", zone.get("display"));
        assertEquals("column", zone.get("flex-direction"));
        assertEquals("center", zone.get("align-items"));
        assertEquals("var(--ash-space-3)", zone.get("gap"));
        assertEquals("100%", zone.get("width"));
        assertEquals("24rem", zone.get("max-width"));
        assertEquals("0", zone.get("margin"));
        assertEquals("var(--ash-space-8) var(--ash-space-6)", zone.get("padding"));
        assertEquals("2px dashed var(--ash-file-upload-border, var(--color-slate-300, " + SLATE_300 + "))", zone.get("border"));
        assertEquals("var(--radius-lg, 0.5rem)", zone.get("border-radius"), "a skin's radius reaches the card");
        assertEquals("1rem", zone.get("font-size"));
        assertEquals("1.5", zone.get("line-height"));
        assertEquals("center", zone.get("text-align"));
        assertEquals("pointer", zone.get("cursor"));
    }

    @Test
    void theSharedSpacingTokensAreTheMeasuredLengths() throws IOException {
        String tokens = read("src/main/css/tokens/tokens.css");
        assertTrue(tokens.contains("--ash-space-3: 0.75rem;") && tokens.contains("--ash-space-6: 1.5rem;") && tokens.contains("--ash-space-8: 2rem;"), "12px gap, 24px and 32px padding");
    }

    // ---- colour and state precedence ----

    @Test
    void theRestingColoursAreTheMeasuredOnesInLightAndDarkAndEachReadsItsPublicProperty() throws IOException {
        assertEquals("var(--ash-file-upload-background, var(--color-slate-50, " + SLATE_50 + "))", declarations("file-upload", ZONE).get("background-color"));
        Map<String, String> dark = declarations("file-upload", ZONE + DARK);
        assertEquals(List.of("border-color", "background-color"), List.copyOf(dark.keySet()));
        assertEquals("var(--ash-file-upload-border, var(--color-slate-600, " + SLATE_600 + "))", dark.get("border-color"));
        assertEquals("var(--ash-file-upload-background, color-mix(in oklab, var(--color-slate-800, " + SLATE_800 + ") 50%, transparent))", dark.get("background-color"),
                "the translucent dark background is the fallback only, so a custom background is used as given");
    }

    @Test
    void hoverIsInsideTheHoverMediaQueryNeverAppliesToADisabledCardAndEachRuleReadsTheSamePublicProperty() throws IOException {
        String hover = ZONE + ":hover:not(:has(:disabled))";
        Map<String, String> light = declarations("file-upload", hover, HOVER);
        assertEquals("var(--ash-file-upload-border, var(--color-slate-400, " + SLATE_400 + "))", light.get("border-color"));
        assertEquals("var(--ash-file-upload-background, var(--color-slate-100, " + SLATE_100 + "))", light.get("background-color"));
        Map<String, String> dark = declarations("file-upload", hover + DARK, HOVER);
        assertEquals("var(--ash-file-upload-border, var(--color-slate-500, " + SLATE_500 + "))", dark.get("border-color"));
        assertEquals("var(--ash-file-upload-background, var(--color-slate-800, " + SLATE_800 + "))", dark.get("background-color"), "the dark hover background is the solid palette colour, as it was");
        String s = stripped().replaceAll("\\s+", " ");
        assertEquals(2, count(s, "@media (hover: hover) {"), "two hover blocks: the hover colours, and the disabled-and-invalid exception");
        assertFalse(s.substring(0, s.indexOf("@media (hover: hover)")).contains(":hover"), "no hover rule outside the media query");
        assertTrue(s.indexOf(hover + " {") < s.indexOf(hover + DARK + " {"), "dark follows light, so it wins at equal specificity");
    }

    @Test
    void anInvalidCardsRedBorderComesAfterEveryHoverRuleAndOutranksThemAndACustomBorderWhileTheBackgroundStillHovers() throws IOException {
        List<String> order = order();
        int hoverDark = order.indexOf(HOVER + "|" + ZONE + ":hover:not(:has(:disabled))" + DARK);
        CssScanner.Rule invalid = rules("file-upload").stream().filter(r -> r.media().isEmpty() && r.selector().startsWith(ZONE + ":has([aria-invalid=\"true\"])") && !r.selector().contains(DARK)).findFirst().orElseThrow();
        CssScanner.Rule invalidDark = rules("file-upload").stream().filter(r -> r.media().isEmpty() && r.selector().startsWith(ZONE + ":has([aria-invalid=\"true\"])") && r.selector().contains(DARK)).findFirst().orElse(null);
        assertTrue(rules("file-upload").stream().filter(r -> r.media().isEmpty() && r.selector().contains(":has([aria-invalid=\"true\"])")).count() == 2, "light and dark");
        for (CssScanner.Rule r : rules("file-upload")) {
            if (r.selector().contains(":user-invalid") || r.selector().contains("aria-invalid")) {
                assertTrue(order.indexOf(r.media() + "|" + r.selector()) > hoverDark, "after the hover rules: " + r.selector());
                assertEquals(1, CssScanner.declarations(r.block()).size(), "an invalid card changes only its border colour (the hover background and the blue ring stay)");
            }
        }
        // Every invalid state is listed with and without :hover, because the hover rules have one more pseudo-class of specificity.
        String light = invalid.selector().replaceAll("\\s+", " ");
        for (String part : List.of(ZONE + ":has([aria-invalid=\"true\"])", ZONE + ":has([aria-invalid=\"true\"]):hover", ZONE + ":has(:user-invalid)", ZONE + ":has(:user-invalid):hover")) {
            assertTrue(light.contains(part + ",") || light.endsWith(part), "missing " + part + " in " + light);
        }
        assertEquals("var(--color-red-400, " + RED_400 + ")", CssScanner.declarations(invalid.block()).get("border-color"), "the literal red: a custom border never overrides it");
        assertTrue(invalidDark != null);
        assertEquals("var(--color-red-500, " + RED_500 + ")", CssScanner.declarations(invalidDark.block()).get("border-color"));
        assertTrue(order.indexOf("|" + invalid.selector()) < order.indexOf("|" + invalidDark.selector()), "dark follows light");
    }

    @Test
    void aDisabledCardIsDimmedWithTheNotAllowedCursor() throws IOException {
        assertEquals(Map.of("cursor", "not-allowed", "opacity", "0.6"), declarations("file-upload", ZONE + ":has(:disabled)"));
    }

    @Test
    void theFocusRingIsAWhiteOffsetThenTwoPixelsOfFocusColourAndAnInvalidCardKeepsTheBlueRing() throws IOException {
        Map<String, String> focus = declarations("file-upload", ZONE + ":has(:focus-visible)");
        assertEquals(Map.of("box-shadow", "0 0 0 2px #fff, 0 0 0 4px var(--ash-file-upload-ring, var(--color-blue-500, " + BLUE_500 + "))"), focus, "the offset is the literal white, in dark mode too");
        assertFalse(stripped().contains("outline:") || stripped().contains("outline-style") || stripped().contains("outline-width") || stripped().contains("outline-offset"), "the hidden input's own outline cannot be seen and the card has none");
        assertFalse(stripped().substring(stripped().indexOf(":has([aria-invalid")).contains("box-shadow"), "no invalid rule touches the ring");
        assertFalse(stripped().contains(":has(:focus)") || stripped().contains(":focus-within"), "ordinary (mouse) focus shows no ring");
    }

    // ---- motion ----

    @Test
    void theCardFadesItsColoursFor150MillisecondsWithTheStandardEaseAndReducedMotionTurnsItOff() throws IOException {
        Map<String, String> zone = declarations("file-upload", ZONE);
        assertEquals("color, background-color, border-color, outline-color, text-decoration-color, fill, stroke, --tw-gradient-from, --tw-gradient-via, --tw-gradient-to", zone.get("transition-property"),
                "the list Tailwind's transition-colors computed to, kept so the computed style is identical");
        assertEquals("150ms", zone.get("transition-duration"));
        assertEquals("cubic-bezier(0.4, 0, 0.2, 1)", zone.get("transition-timing-function"));
        assertEquals(Map.of("transition-property", "none"), declarations("file-upload", ZONE, REDUCED));
    }

    // ---- the parts ----

    @Test
    void theIconWrapperTheStatusAndTheChooseSpanAreTheMeasuredOnes() throws IOException {
        assertEquals(Map.of("color", "var(--color-slate-400, " + SLATE_400 + ")"), declarations("file-upload", ".ash-file-upload__icon"));
        assertEquals(Map.of("color", "var(--color-slate-500, " + SLATE_500 + ")"), declarations("file-upload", ".ash-file-upload__icon" + DARK));
        Map<String, String> status = declarations("file-upload", ".ash-file-upload__status");
        assertEquals("calc(1.25 / 0.875)", status.get("--ash-file-upload-leading"));
        assertEquals("var(--ash-file-upload-leading)", status.get("line-height"));
        assertEquals("100%", status.get("max-width"));
        assertEquals("0", status.get("margin"));
        assertEquals("hidden", status.get("overflow"));
        assertEquals("ellipsis", status.get("text-overflow"));
        assertEquals("nowrap", status.get("white-space"));
        assertEquals("0.875rem", status.get("font-size"));
        assertEquals("var(--font-weight-medium, 500)", status.get("font-weight"));
        assertEquals("var(--ash-file-upload-status-color, var(--color-slate-700, " + SLATE_700 + "))", status.get("color"));
        assertEquals("var(--ash-file-upload-status-color, var(--color-slate-300, " + SLATE_300 + "))", declarations("file-upload", ".ash-file-upload__status" + DARK).get("color"));
        assertEquals(Map.of("pointer-events", "none"), declarations("file-upload", ".ash-file-upload__choose"), "D4: the Choose File span takes no pointer events, as the Tailwind utility did");
        assertFalse(stripped().contains("ash-field__"), "the label, marker, help and error are field.css's alone (reused, not restyled)");
    }

    // ---- customisation by ancestor ----

    @Test
    void theFourPublicPropertiesAreOnlyReadNeverDeclaredSoAnAncestorCanSetThem() throws IOException {
        String s = stripped();
        List<String> publicProps = List.of("border", "background", "ring", "status-color");
        for (String prop : publicProps) {
            assertFalse(Pattern.compile("(^|[;{\\s])--ash-file-upload-" + Pattern.quote(prop) + "\\s*:").matcher(s).find(), "must not declare --ash-file-upload-" + prop);
            assertFalse(s.contains("var(--ash-file-upload-" + prop + ")"), "every read of --ash-file-upload-" + prop + " has a fallback");
            assertTrue(s.contains("var(--ash-file-upload-" + prop + ","), "--ash-file-upload-" + prop + " is read");
        }
        Matcher declared = Pattern.compile("(?m)^\\s*(--[a-z-]+)\\s*:").matcher(s);
        List<String> found = new ArrayList<>();
        while (declared.find()) {
            found.add(declared.group(1));
        }
        assertEquals(List.of("--ash-file-upload-leading"), found, "the only declared custom property is the internal line-height ratio");
        Matcher reads = Pattern.compile("var\\(--ash-file-upload-([a-z-]+)").matcher(s);
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
    void theEntryFileImportsItInTheComponentsLayerAfterToggleAndTheTokensAreNotTouched() throws IOException {
        String entry = read("src/main/css/ashlar.css");
        assertTrue(entry.contains("@import \"components/file-upload.css\" layer(ash.components);"));
        assertTrue(entry.indexOf("components/toggle.css") < entry.indexOf("components/file-upload.css"), "after Toggle, the last Choice control");
        assertTrue(entry.indexOf("components/field.css") < entry.indexOf("components/file-upload.css") && entry.indexOf("components/button.css") < entry.indexOf("components/file-upload.css"),
                "after the Field and Button sheets it reuses");
        assertTrue(entry.indexOf("components/file-upload.css") < entry.indexOf("utilities/sr-only.css"), "ash-sr-only is in the later utilities layer, so it wins over anything here");
        assertFalse(read("src/main/css/tokens/tokens.css").contains("--ash-file-upload"), "File Upload adds nothing to the shared tokens");
    }

    private static int count(String s, String what) {
        int n = 0;
        for (int at = s.indexOf(what); at >= 0; at = s.indexOf(what, at + 1)) {
            n++;
        }
        return n;
    }
}
