package dev.redspartanlabs.ashlar.css;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the Timeline Item's source contract: the template keeps its five parameters and defaults, its {@code <li>} with a decorative
 * {@code aria-hidden} marker, the optional date, the title as a plain paragraph, the status text for a non-neutral variant (now
 * {@code ash-sr-only}), the optional projected content and the optional Icon sized by Ashlar's own Icon modifier, and carries only Ashlar's
 * classes; the stylesheet is scoped to {@code ash-timeline-item}, owns the marker (round whatever the skin), the connector (a real element
 * that bridges exactly the gap), the last entry's own rules, and sets nothing on the caller's projected content. The rendered result is
 * measured in a real browser by the showcase's {@code TimelineParityTest}; this reads the sources only.
 */
class TimelineItemContractTest {

    private static String template;
    private static String css;
    private static List<CssScanner.Rule> rules;
    private static Path module;

    @BeforeAll
    static void load() throws IOException {
        String generated = System.getProperty("ashlar.css.generated");
        assertNotNull(generated, "ashlar.css.generated must be set by the Gradle test task");
        for (Path p = Path.of(generated).toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("src/main/css/ashlar.css"))) {
                module = p;
                break;
            }
        }
        assertNotNull(module, "could not find the ashlar module above " + generated);
        template = Files.readString(module.resolve("src/main/jte/ashlar/components/timeline-item.jte"), StandardCharsets.UTF_8).replace("\r\n", "\n");
        css = Files.readString(module.resolve("src/main/css/components/timeline-item.css"), StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "");
        rules = CssScanner.flatRules(css);
    }

    private static String withoutJteComments(String source) {
        return source.replaceAll("(?s)<%--.*?--%>", "");
    }

    // ---- the template ----

    @Test
    void theFiveParametersAndTheirDefaultsAreUnchanged() {
        List<String> declared = new ArrayList<>();
        Matcher m = Pattern.compile("(?m)^@(?:param|import) .*$").matcher(template);
        while (m.find()) {
            declared.add(m.group());
        }
        assertEquals(List.of("@import dev.redspartanlabs.ashlar.icon.Icon", "@import dev.redspartanlabs.ashlar.badge.BadgeVariant",
                "@import dev.redspartanlabs.ashlar.timeline.TimelineStyles", "@param String title", "@param String date = \"\"",
                "@param gg.jte.Content content = null", "@param Icon icon = null", "@param String variant = \"Neutral\""), declared);
    }

    @Test
    void theVariantStillGoesThroughBadgeVariantFromSoAnUnknownValueFallsBackToNeutral() {
        String markup = withoutJteComments(template);
        assertTrue(markup.contains("BadgeVariant resolvedVariant = BadgeVariant.from(variant);"));
        assertTrue(markup.contains("String markerClasses = TimelineStyles.marker(resolvedVariant);"));
        assertTrue(markup.contains("String statusText = TimelineStyles.statusText(resolvedVariant);"));
        assertTrue(markup.contains("boolean hasDate = !date.isEmpty();") && markup.contains("boolean hasContent = content != null;") && markup.contains("boolean hasIcon = icon != null;"));
    }

    @Test
    void theEntryIsAnLiWithTheRailMarkerConnectorAndBodyInTheApprovedStructure() {
        String markup = withoutJteComments(template).replaceAll("(?m)^(@param|@import) .*$", "").replaceAll("(?m)^!\\{.*$", "").replaceAll("\\s+", " ").trim();
        assertEquals("<li class=\"ash-timeline-item\"> <div class=\"ash-timeline-item__rail\"> <span aria-hidden=\"true\" class=\"${markerClasses}\">"
                + " @if (hasIcon) @template.ashlar.components.icon(icon = icon, classes = \"ash-icon--sm\") @endif </span>"
                + " <div class=\"ash-timeline-item__connector\"></div> </div>"
                + " <div class=\"ash-timeline-item__body\">"
                + " @if (hasDate) <p class=\"ash-timeline-item__date\">${date}</p> @endif"
                + " <p class=\"ash-timeline-item__title\"> @if (hasStatusText) <span class=\"ash-sr-only\">${statusText}</span> @endif ${title} </p>"
                + " @if (hasContent) <div class=\"ash-timeline-item__content\"> ${content} </div> @endif"
                + " </div> </li>", markup);
    }

    @Test
    void theMarkerIsDecorativeAndNothingElseIsHiddenOrGivenARoleAHookOrAnEscapeHatch() {
        String markup = withoutJteComments(template);
        assertEquals(1, count(markup, "aria-hidden=\"true\""), "only the marker (and with it, its Icon) is hidden");
        assertTrue(markup.contains("<span aria-hidden=\"true\" class=\"${markerClasses}\">"));
        for (String forbidden : List.of("role=", "aria-label", "data-", "style=", "id=", "<h1", "<h2", "<h3", "<time")) {
            assertFalse(markup.contains(forbidden), "the entry adds no " + forbidden + " (the title is a paragraph and the date is plain text)");
        }
    }

    @Test
    void theStatusTextUsesTheSharedVisuallyHiddenUtilityNotATailwindClass() {
        String markup = withoutJteComments(template);
        assertEquals(1, count(markup, "<span class=\"ash-sr-only\">${statusText}</span>"));
        assertFalse(Pattern.compile("(?<![\\w-])sr-only").matcher(markup).find());
    }

    @Test
    void noTailwindUtilityRemainsInTheTemplate() {
        String markup = withoutJteComments(template);
        for (String utility : List.of("group", "relative", "flex", "gap-", "pb-", "pt-", "last:", "w-8", "h-8", "w-0.5", "shrink-0", "items-center", "justify-center",
                "z-10", "rounded", "border-", "absolute", "-bottom-8", "top-8", "bg-", "dark:", "text-xs", "text-sm", "text-slate", "font-", "mt-", "min-w-0",
                "flex-1", "break-words", "h-4", "w-4", "slate-")) {
            assertFalse(Pattern.compile("(?<![\\w-])" + Pattern.quote(utility)).matcher(markup).find(), "a Tailwind utility is back: " + utility);
        }
    }

    // ---- the stylesheet ----

    private static final String DARK = ":where([data-theme=\"dark\"], [data-theme=\"dark\"] *)";
    private static final String CONNECTOR_HIDDEN = ".ash-timeline-item:last-child > .ash-timeline-item__rail > .ash-timeline-item__connector";

    private static Map<String, String> declarations(String selector) {
        List<CssScanner.Rule> found = rules.stream().filter(r -> r.selector().equals(selector)).toList();
        assertEquals(1, found.size(), selector + " must be defined exactly once");
        return CssScanner.declarations(found.get(0).block());
    }

    @Test
    void everySelectorIsScopedToTheTimelineItemAndNothingIsGlobalConditionalOrAPseudoElement() {
        List<String> expected = new ArrayList<>(List.of(".ash-timeline-item", ".ash-timeline-item" + DARK, ".ash-timeline-item:last-child",
                ".ash-timeline-item__rail", ".ash-timeline-item__marker"));
        for (String variant : List.of("neutral", "info", "success", "warning", "danger")) {
            expected.add(".ash-timeline-item__marker--" + variant);
            expected.add(".ash-timeline-item__marker--" + variant + DARK);
        }
        expected.addAll(List.of(".ash-timeline-item__connector", CONNECTOR_HIDDEN, ".ash-timeline-item__body", ".ash-timeline-item__date",
                ".ash-timeline-item__title", ".ash-timeline-item__content"));
        assertEquals(expected, rules.stream().map(CssScanner.Rule::selector).toList());
        for (CssScanner.Rule rule : rules) {
            assertEquals("", rule.media(), "no media conditions: " + rule.selector());
        }
        assertFalse(css.contains("!important"));
        assertFalse(css.contains("@import") || css.contains("@layer") || css.contains("@keyframes"), "the entry file assigns the layer");
        assertFalse(css.contains("::before") || css.contains("::after") || css.contains(":before") || css.contains(":after"), "the connector is a real element");
    }

    @Test
    void theEntryStatesItsLayoutAndOneGapPropertyDrivesBothItsPaddingAndTheConnectorsReach() {
        Map<String, String> entry = declarations(".ash-timeline-item");
        assertEquals("var(--ash-space-8)", entry.get("--ash-timeline-item-gap"), "the default is the existing 2rem step");
        assertEquals("var(--ash-timeline-item-gap)", entry.get("padding-bottom"));
        assertEquals("relative", entry.get("position"));
        assertEquals("flex", entry.get("display"));
        assertEquals("var(--ash-space-4)", entry.get("gap"));
        assertEquals("calc(1 / 0.75)", entry.get("--ash-timeline-item-date-leading"), "the ratio lives in a property so a minifier cannot fold it");
        assertEquals("calc(1.25 / 0.875)", entry.get("--ash-timeline-item-leading"));
        // The same property, negated, is how far the connector reaches past the rail column: the connector bridges exactly the gap.
        assertEquals("calc(-1 * var(--ash-timeline-item-gap))", declarations(".ash-timeline-item__connector").get("bottom"));
        assertEquals(2, count(css, "--ash-timeline-item-gap)"), "the padding and the connector use the property, nothing else uses a gap number");
        assertFalse(css.contains("bottom: -2rem") || css.contains("bottom: 2rem"), "no second copy of the gap value");
    }

    @Test
    void theLastEntryHasNoGapAndHidesItsOwnConnectorThroughItsOwnRailWithChildCombinatorsOnly() {
        assertEquals(Map.of("padding-bottom", "0"), declarations(".ash-timeline-item:last-child"));
        assertEquals(Map.of("display", "none"), declarations(CONNECTOR_HIDDEN));
        assertTrue(CONNECTOR_HIDDEN.startsWith(".ash-timeline-item:last-child > "), "the entry itself, not an ancestor");
        assertEquals(2, count(CONNECTOR_HIDDEN, " > "), "li > rail > connector: the entry's own connector by child combinators");
        // The old rule hid anything under ANY .group:last-child ancestor; a descendant form would reach a nested Timeline's connectors again.
        assertFalse(css.contains(".ash-timeline-item:last-child .") || css.contains("group"), "no descendant form, no group");
        assertFalse(css.contains("first-child") || css.contains("only-child") || css.contains("nth-") || css.contains("+ ") || css.contains("~ "), "only the last child is special");
    }

    @Test
    void theMarkerIsATwoRemCircleWithATwoPixelSolidBorderAboveTheRailAndNeverUsesASkinRadius() {
        Map<String, String> marker = declarations(".ash-timeline-item__marker");
        assertEquals("2rem", marker.get("width"));
        assertEquals("2rem", marker.get("height"));
        assertEquals("2px solid var(--ash-timeline-marker-border)", marker.get("border"), "a border is not drawn without a style, and preflight is gone");
        assertEquals("calc(infinity * 1px)", marker.get("border-radius"), "the same full circle rounded-full was, whatever the skin");
        assertEquals("10", marker.get("z-index"));
        assertEquals("relative", marker.get("position"));
        assertEquals("0", marker.get("flex-shrink"));
        assertEquals("flex", marker.get("display"));
        assertEquals("center", marker.get("align-items"));
        assertEquals("center", marker.get("justify-content"));
        assertEquals("var(--ash-timeline-marker-bg)", marker.get("background-color"));
        assertEquals("var(--ash-timeline-marker-fg)", marker.get("color"));
        assertFalse(css.contains("--ash-radius") || css.contains("--radius"), "a skin's radius must not change a circle");
    }

    @Test
    void theConnectorIsARealTwoPixelLineFromTheMarkersBottomEdgeInTheEntrysRailColour() {
        assertEquals(Map.of("position", "absolute", "top", "2rem", "bottom", "calc(-1 * var(--ash-timeline-item-gap))", "width", "0.125rem",
                "background-color", "var(--ash-timeline-item-rail)"), declarations(".ash-timeline-item__connector"));
        Map<String, String> rail = declarations(".ash-timeline-item__rail");
        assertEquals(Map.of("position", "relative", "display", "flex", "width", "2rem", "flex-shrink", "0", "flex-direction", "column", "align-items", "center"), rail,
                "the connector has no left or transform: the centred rail column positions it");
        assertFalse(declarations(".ash-timeline-item__connector").containsKey("left") || css.contains("transform"));
    }

    @Test
    void theColoursDefaultToTheTailwindPaletteWithItsExactFallbacksAndFlipInTheDarkTheme() {
        Map<String, String> light = declarations(".ash-timeline-item");
        assertEquals("var(--color-slate-200, oklch(92.9% 0.013 255.508))", light.get("--ash-timeline-item-rail"));
        assertEquals("var(--color-slate-500, oklch(55.4% 0.046 257.417))", light.get("--ash-timeline-item-date"));
        assertEquals("var(--color-slate-900, oklch(20.8% 0.042 265.755))", light.get("--ash-timeline-item-title"));
        assertEquals("var(--color-slate-600, oklch(44.6% 0.043 257.281))", light.get("--ash-timeline-item-content"));
        assertEquals(Map.of(
                "--ash-timeline-item-rail", "var(--color-slate-700, oklch(37.2% 0.044 257.287))",
                "--ash-timeline-item-date", "var(--color-slate-400, oklch(70.4% 0.04 256.788))",
                "--ash-timeline-item-title", "var(--color-slate-100, oklch(96.8% 0.007 247.896))",
                "--ash-timeline-item-content", "var(--color-slate-300, oklch(86.9% 0.022 252.894))"), declarations(".ash-timeline-item" + DARK));
    }

    private static Map<String, String> palette(String border, String bg, String fg) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("--ash-timeline-marker-border", border);
        m.put("--ash-timeline-marker-bg", bg);
        m.put("--ash-timeline-marker-fg", fg);
        return m;
    }

    @Test
    void theFiveMarkerVariantsKeepTheirOwnLightAndDarkPaletteWithExactFallbacks() {
        String slate300 = "var(--color-slate-300, oklch(86.9% 0.022 252.894))";
        String slate600 = "var(--color-slate-600, oklch(44.6% 0.043 257.281))";
        assertEquals(palette(slate300, "var(--color-white, #fff)", "var(--color-slate-400, oklch(70.4% 0.04 256.788))"), declarations(".ash-timeline-item__marker--neutral"));
        assertEquals(palette(slate600, "var(--color-slate-900, oklch(20.8% 0.042 265.755))", "var(--color-slate-500, oklch(55.4% 0.046 257.417))"),
                declarations(".ash-timeline-item__marker--neutral" + DARK));
        assertEquals(palette("var(--color-sky-500, oklch(68.5% 0.169 237.323))", "var(--color-sky-50, oklch(97.7% 0.013 236.62))", "var(--color-sky-600, oklch(58.8% 0.158 241.966))"),
                declarations(".ash-timeline-item__marker--info"));
        assertEquals(palette("var(--color-sky-500, oklch(68.5% 0.169 237.323))", "var(--color-sky-950, oklch(29.3% 0.066 243.157))", "var(--color-sky-400, oklch(74.6% 0.16 232.661))"),
                declarations(".ash-timeline-item__marker--info" + DARK));
        assertEquals(palette("var(--color-green-500, oklch(72.3% 0.219 149.579))", "var(--color-green-50, oklch(98.2% 0.018 155.826))", "var(--color-green-600, oklch(62.7% 0.194 149.214))"),
                declarations(".ash-timeline-item__marker--success"));
        assertEquals(palette("var(--color-green-500, oklch(72.3% 0.219 149.579))", "var(--color-green-950, oklch(26.6% 0.065 152.934))", "var(--color-green-400, oklch(79.2% 0.209 151.711))"),
                declarations(".ash-timeline-item__marker--success" + DARK));
        assertEquals(palette("var(--color-amber-500, oklch(76.9% 0.188 70.08))", "var(--color-amber-50, oklch(98.7% 0.022 95.277))", "var(--color-amber-600, oklch(66.6% 0.179 58.318))"),
                declarations(".ash-timeline-item__marker--warning"));
        assertEquals(palette("var(--color-amber-500, oklch(76.9% 0.188 70.08))", "var(--color-amber-950, oklch(27.9% 0.077 45.635))", "var(--color-amber-400, oklch(82.8% 0.189 84.429))"),
                declarations(".ash-timeline-item__marker--warning" + DARK));
        assertEquals(palette("var(--color-red-500, oklch(63.7% 0.237 25.331))", "var(--color-red-50, oklch(97.1% 0.013 17.38))", "var(--color-red-600, oklch(57.7% 0.245 27.325))"),
                declarations(".ash-timeline-item__marker--danger"));
        assertEquals(palette("var(--color-red-500, oklch(63.7% 0.237 25.331))", "var(--color-red-950, oklch(25.8% 0.092 26.042))", "var(--color-red-400, oklch(70.4% 0.191 22.216))"),
                declarations(".ash-timeline-item__marker--danger" + DARK));
        assertFalse(css.contains("ash-badge") || css.contains("--ash-badge"), "the marker palette is the Timeline's own, not the Badge's");
    }

    @Test
    void theBodyAndTheTextOwnTheirSpacingTypographyAndWrappingAndStateTheZeroMarginsPreflightUsedToGive() {
        assertEquals(Map.of("min-width", "0", "flex", "1", "padding-block", "0.25rem"), declarations(".ash-timeline-item__body"));
        assertEquals(Map.of("margin", "0", "font-size", "0.75rem", "line-height", "var(--ash-timeline-item-date-leading)", "font-weight", "500",
                "color", "var(--ash-timeline-item-date)"), declarations(".ash-timeline-item__date"));
        assertEquals(Map.of("margin", "0.125rem 0 0", "overflow-wrap", "break-word", "font-size", "0.875rem", "line-height", "var(--ash-timeline-item-leading)",
                "font-weight", "600", "color", "var(--ash-timeline-item-title)"), declarations(".ash-timeline-item__title"));
        assertEquals(Map.of("margin-top", "0.25rem", "overflow-wrap", "break-word", "font-size", "0.875rem", "line-height", "var(--ash-timeline-item-leading)",
                "color", "var(--ash-timeline-item-content)"), declarations(".ash-timeline-item__content"));
    }

    @Test
    void theCallersProjectedContentIsTheirsNoDescendantResetsAreAdded() {
        for (CssScanner.Rule rule : rules) {
            assertFalse(rule.selector().matches(".*__content\\s+[a-z.*>:\\[].*"), "nothing inside the content wrapper is styled: " + rule.selector());
            assertFalse(rule.selector().matches(".*__content\\s*>.*"), "nothing inside the content wrapper is styled: " + rule.selector());
        }
        assertEquals(1, rules.stream().filter(r -> r.selector().contains("__content")).count(), "the wrapper only");
        assertFalse(css.contains(" p ") || css.contains(" p{") || css.contains("> p") || css.contains(" p,"), "no paragraph reset");
    }

    @Test
    void theIconIsSizedByTheIconAndTheMarkerOnlyGivesItAColourAndCentresIt() {
        assertFalse(css.contains("ash-icon") || css.contains("svg"), "the Icon owns its own size and display");
        assertTrue(declarations(".ash-timeline-item__marker").get("color").equals("var(--ash-timeline-marker-fg)"), "the Icon draws in currentColor");
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }
}
