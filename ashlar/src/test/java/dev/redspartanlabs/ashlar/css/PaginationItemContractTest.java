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
 * Pins the Pagination Item's source contract: the template keeps its three parameters and its rules (a link only for a non-current entry
 * with a non-empty {@code href}; the current entry is a span with {@code aria-current="page"} even when it is also given an {@code href};
 * every other entry is a span with {@code aria-disabled="true"}), and carries only Ashlar's classes; the stylesheet gives the entry its
 * box, its colours (palette variables with exact Tailwind fallbacks, a dark rule), its link behavior (hover only on a device that can hover,
 * no browser outline, a two pixel outer ring for keyboard focus) and never reaches an ancestor. The rendered result is measured in a real
 * browser by the showcase's {@code PaginationParityTest}; this reads the sources only.
 */
class PaginationItemContractTest {

    private static String template;
    private static String css;
    private static List<CssScanner.Rule> rules;

    @BeforeAll
    static void load() throws IOException {
        String generated = System.getProperty("ashlar.css.generated");
        assertNotNull(generated, "ashlar.css.generated must be set by the Gradle test task");
        Path module = null;
        for (Path p = Path.of(generated).toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("src/main/css/ashlar.css"))) {
                module = p;
                break;
            }
        }
        assertNotNull(module, "could not find the ashlar module above " + generated);
        template = Files.readString(module.resolve("src/main/jte/ashlar/components/pagination-item.jte"), StandardCharsets.UTF_8).replace("\r\n", "\n");
        css = Files.readString(module.resolve("src/main/css/components/pagination-item.css"), StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "");
        rules = CssScanner.flatRules(css);
    }

    private static String withoutJteComments(String source) {
        return source.replaceAll("(?s)<%--.*?--%>", "");
    }

    // ---- the template ----

    @Test
    void theThreeParametersAndTheirDefaultsAreUnchanged() {
        List<String> declared = new ArrayList<>();
        Matcher m = Pattern.compile("(?m)^@(?:param|import) .*$").matcher(template);
        while (m.find()) {
            declared.add(m.group());
        }
        assertEquals(List.of("@param String label", "@param String href = null", "@param boolean current = false"), declared);
    }

    @Test
    void currentStillWinsOverHrefAnEmptyHrefIsNotALinkAndOnlyTheCurrentAndDisabledEntriesAreMarked() {
        String markup = withoutJteComments(template);
        assertTrue(markup.contains("!{boolean isCurrent = current;}"));
        assertTrue(markup.contains("!{boolean isLink = !isCurrent && href != null && !href.isEmpty();}"));
        assertTrue(markup.contains("!{String ariaCurrentAttr = isCurrent ? \"page\" : null;}"));
        assertTrue(markup.contains("!{String ariaDisabledAttr = (!isCurrent && !isLink) ? \"true\" : null;}"));
    }

    @Test
    void theEntryIsAnLiWithExactlyOneLinkTheCurrentPageOrADisabledEntry() {
        String markup = withoutJteComments(template).replaceAll("(?m)^(@param|@import) .*$", "").replaceAll("(?m)^!\\{.*$", "").replaceAll("\\s+", " ").trim();
        assertEquals("<li class=\"ash-pagination-item\">"
                + " @if (isLink) <a href=\"${href}\" class=\"ash-pagination-item__control ash-pagination-item__control--link\">${label}</a>"
                + " @elseif (isCurrent) <span aria-current=\"${ariaCurrentAttr}\" class=\"ash-pagination-item__control ash-pagination-item__control--current\">${label}</span>"
                + " @else <span aria-disabled=\"${ariaDisabledAttr}\" class=\"ash-pagination-item__control ash-pagination-item__control--disabled\">${label}</span>"
                + " @endif </li>", markup);
    }

    @Test
    void nothingGetsARoleAnIdAnEscapeHatchOrAHandlerAndTheLabelIsNeverInspected() {
        String markup = withoutJteComments(template);
        assertEquals(1, count(markup, "aria-current=\""), "aria-current on the current page only");
        assertEquals(1, count(markup, "aria-disabled=\""), "aria-disabled on the disabled entry only");
        for (String forbidden : List.of("role=", "aria-label", "style=", "id=", "tabindex", "onclick", "<button", "<ul", "<ol", "<svg", "ash-icon", "label.")) {
            assertFalse(markup.contains(forbidden), "the entry adds no " + forbidden);
        }
    }

    @Test
    void noTailwindUtilityRemainsInTheTemplate() {
        String markup = withoutJteComments(template);
        for (String utility : List.of("inline-flex", "h-8", "min-w-8", "items-center", "justify-center", "rounded", "border", "px-2", "font-medium", "text-slate", "text-white",
                "bg-", "hover:", "focus", "ring", "dark:")) {
            assertFalse(Pattern.compile("(?<![\\w-])" + Pattern.quote(utility)).matcher(markup).find(), "a Tailwind utility is back: " + utility);
        }
    }

    // ---- the stylesheet ----

    private static final String DARK = ":where([data-theme=\"dark\"], [data-theme=\"dark\"] *)";
    private static final String LINK = ".ash-pagination-item__control--link";
    private static final String HOVER = LINK + ":hover";
    private static final String HOVER_MEDIA = "@media (hover: hover)";

    private static String norm(String selector) {
        return selector.replaceAll("\\s+", " ").trim();
    }

    private static Map<String, String> declarations(String selector) {
        List<CssScanner.Rule> found = rules.stream().filter(r -> norm(r.selector()).equals(selector)).toList();
        assertEquals(1, found.size(), selector + " must be defined exactly once");
        return CssScanner.declarations(found.get(0).block());
    }

    @Test
    void everySelectorIsScopedToTheEntryAndNothingIsGlobalAndOnlyHoverIsConditionalOnADeviceThatCanHover() {
        assertEquals(List.of(".ash-pagination-item", ".ash-pagination-item" + DARK, ".ash-pagination-item__control", LINK, HOVER, LINK + ":focus", LINK + ":focus-visible",
                ".ash-pagination-item__control--current", ".ash-pagination-item__control--disabled"), rules.stream().map(r -> norm(r.selector())).toList());
        for (CssScanner.Rule rule : rules) {
            assertEquals(norm(rule.selector()).equals(HOVER) ? HOVER_MEDIA : "", norm(rule.media()), "only the hover rule is conditional: " + rule.selector());
        }
        assertEquals(1, count(css, "@media"), "one media block: the hover capability, as Tailwind's hover variant was");
        assertFalse(css.contains("@container") || css.contains("@supports"), "no other condition");
        assertFalse(css.contains("prefers-") || css.contains("@media (min-width") || css.contains("@media (max-width"),"the window's width and the user's preferences are never consulted");
        assertFalse(css.contains("!important"));
        assertFalse(css.contains("@import") || css.contains("@layer") || css.contains("@keyframes"), "the entry file assigns the layer");
        assertFalse(css.contains("ash-pagination__list") || css.contains("ash-pagination {"), "the entry knows neither the list nor the landmark around it");
        assertFalse(css.contains("ash-icon") || css.contains("svg"), "the entry owns nothing of an Icon");
    }

    @Test
    void theEntryIsAListItemAndCarriesTheLightPaletteWithExactFallbacks() {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("--ash-pagination-item-border", "var(--color-slate-200, oklch(92.9% 0.013 255.508))");
        expected.put("--ash-pagination-item-link", "var(--color-slate-600, oklch(44.6% 0.043 257.281))");
        expected.put("--ash-pagination-item-link-hover", "var(--color-slate-900, oklch(20.8% 0.042 265.755))");
        expected.put("--ash-pagination-item-link-hover-bg", "var(--color-slate-50, oklch(98.4% 0.003 247.858))");
        expected.put("--ash-pagination-item-current", "var(--color-blue-600, oklch(54.6% 0.245 262.881))");
        expected.put("--ash-pagination-item-current-text", "var(--color-white, #fff)");
        expected.put("--ash-pagination-item-disabled", "var(--color-slate-300, oklch(86.9% 0.022 252.894))");
        expected.put("--ash-pagination-item-focus", "var(--color-blue-500, oklch(62.3% 0.214 259.815))");
        expected.put("display", "list-item");
        assertEquals(expected, declarations(".ash-pagination-item"));
    }

    @Test
    void theDarkRuleRecolourEverythingButTheCurrentTextAndTheFocusRingWhichDoNotChangeBetweenThemes() {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("--ash-pagination-item-border", "var(--color-slate-700, oklch(37.2% 0.044 257.287))");
        expected.put("--ash-pagination-item-link", "var(--color-slate-300, oklch(86.9% 0.022 252.894))");
        expected.put("--ash-pagination-item-link-hover", "var(--color-slate-100, oklch(96.8% 0.007 247.896))");
        expected.put("--ash-pagination-item-link-hover-bg", "var(--color-slate-800, oklch(27.9% 0.041 260.031))");
        expected.put("--ash-pagination-item-current", "var(--color-blue-500, oklch(62.3% 0.214 259.815))");
        expected.put("--ash-pagination-item-disabled", "var(--color-slate-600, oklch(44.6% 0.043 257.281))");
        assertEquals(expected, declarations(".ash-pagination-item" + DARK));
        assertFalse(css.contains("--ash-color-focus"), "the ring is blue-500 in both themes, not the dark focus token");
    }

    @Test
    void everyEntryIsTheSameThirtyTwoPixelTallCentredInlineFlexBoxWithMediumText() {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("display", "inline-flex");
        expected.put("align-items", "center");
        expected.put("justify-content", "center");
        expected.put("height", "2rem");
        expected.put("min-width", "2rem");
        expected.put("padding-left", "0.5rem");
        expected.put("padding-right", "0.5rem");
        expected.put("font-weight", "500");
        assertEquals(expected, declarations(".ash-pagination-item__control"));
        assertFalse(css.contains("font-size") || css.contains("line-height") || css.contains("font-family"), "the list sets the text size; the entry inherits it");
    }

    @Test
    void theLinkIsBorderedRoundedWithTheSkinsMediumRadiusHasItsOwnColourAndIsNeverUnderlined() {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("border-radius", "var(--ash-radius-md)");
        expected.put("border", "1px solid var(--ash-pagination-item-border)");
        expected.put("color", "var(--ash-pagination-item-link)");
        expected.put("text-decoration", "none");
        assertEquals(expected, declarations(LINK));
    }

    @Test
    void theCurrentPageIsWhiteOnTheBlueWithAMatchingBorderAndTheSkinsMediumRadius() {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("border-radius", "var(--ash-radius-md)");
        expected.put("border", "1px solid var(--ash-pagination-item-current)");
        expected.put("background-color", "var(--ash-pagination-item-current)");
        expected.put("color", "var(--ash-pagination-item-current-text)");
        assertEquals(expected, declarations(".ash-pagination-item__control--current"));
    }

    @Test
    void theDisabledEntryOnlyHasItsMutedColourNoBorderNoRadiusNoFillAndNoPointerCursor() {
        assertEquals(Map.of("color", "var(--ash-pagination-item-disabled)"), declarations(".ash-pagination-item__control--disabled"));
        assertFalse(css.contains("cursor"), "a link is pointer by the browser's own default; nothing else is made to look clickable");
    }

    @Test
    void theHoverRuleIsInsideTheHoverCapabilityMediaQueryAndNothingElseIsAndTheSelectorsAreUnchanged() {
        List<CssScanner.Rule> conditional = rules.stream().filter(r -> !r.media().isEmpty()).toList();
        assertEquals(1, conditional.size());
        assertEquals(HOVER, norm(conditional.get(0).selector()));
        assertEquals(HOVER_MEDIA, norm(conditional.get(0).media()));
        // Focus is a keyboard matter, not a pointer one, and applies whatever the device can do.
        for (String selector : List.of(LINK + ":focus", LINK + ":focus-visible", LINK)) {
            assertEquals("", norm(rules.stream().filter(r -> norm(r.selector()).equals(selector)).findFirst().orElseThrow().media()), selector);
        }
    }

    @Test
    void hoverFillsAndRecoloursAndFocusTurnsTheBrowserOutlineOffAndDrawsATwoPixelOuterRingOnlyForKeyboardFocus() {
        assertEquals(Map.of("background-color", "var(--ash-pagination-item-link-hover-bg)", "color", "var(--ash-pagination-item-link-hover)"), declarations(HOVER));
        assertEquals(Map.of("outline-style", "none"), declarations(LINK + ":focus"));
        assertEquals(Map.of("box-shadow", "0 0 0 2px var(--ash-pagination-item-focus)"), declarations(LINK + ":focus-visible"));
        assertFalse(css.contains("inset"), "the ring is outside the link, as before");
        assertFalse(css.contains("underline"), "hover fills the link; it does not underline it, as before");
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }
}
