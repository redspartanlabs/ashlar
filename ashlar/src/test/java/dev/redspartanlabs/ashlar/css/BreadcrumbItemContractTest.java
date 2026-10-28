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
 * Pins the Breadcrumb Item's source contract: the template keeps its three parameters and its rules (a link only for a non-current crumb
 * with a non-empty {@code href}; the current crumb is plain text with {@code aria-current="page"} even when it is also given an
 * {@code href}), its decorative separator inside the same {@code <li>}, and carries only Ashlar's classes; the stylesheet gives the
 * crumb its colours (palette variables with exact Tailwind fallbacks, a dark rule, the same in both themes for the focus ring), its link
 * behavior and the first crumb's own hidden separator, never reaching an ancestor. The rendered result is measured in a real browser by the
 * showcase's {@code BreadcrumbsParityTest}; this reads the sources only.
 */
class BreadcrumbItemContractTest {

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
        template = Files.readString(module.resolve("src/main/jte/ashlar/components/breadcrumb-item.jte"), StandardCharsets.UTF_8).replace("\r\n", "\n");
        css = Files.readString(module.resolve("src/main/css/components/breadcrumb-item.css"), StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "");
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
    void currentStillWinsOverHrefAnEmptyHrefIsNotALinkAndOnlyTheCurrentCrumbIsMarked() {
        String markup = withoutJteComments(template);
        assertTrue(markup.contains("!{boolean isCurrent = current;}"));
        assertTrue(markup.contains("!{boolean isLink = !isCurrent && href != null && !href.isEmpty();}"));
        assertTrue(markup.contains("!{String ariaCurrentAttr = isCurrent ? \"page\" : null;}"));
    }

    @Test
    void theCrumbIsAnLiWithItsDecorativeSeparatorInsideItThenALinkOrText() {
        String markup = withoutJteComments(template).replaceAll("(?m)^(@param|@import) .*$", "").replaceAll("(?m)^!\\{.*$", "").replaceAll("\\s+", " ").trim();
        assertEquals("<li class=\"ash-breadcrumb-item\">"
                + " <span aria-hidden=\"true\" data-breadcrumb-separator class=\"ash-breadcrumb-item__separator\">/</span>"
                + " @if (isLink) <a href=\"${href}\" class=\"ash-breadcrumb-item__link\">${label}</a>"
                + " @else <span aria-current=\"${ariaCurrentAttr}\""
                + " class=\"${isCurrent ? \"ash-breadcrumb-item__text ash-breadcrumb-item__text--current\" : \"ash-breadcrumb-item__text\"}\">${label}</span>"
                + " @endif </li>", markup);
    }

    @Test
    void onlyTheSeparatorIsHiddenAndNothingElseGetsARoleAnIdAnEscapeHatchOrAHandler() {
        String markup = withoutJteComments(template);
        assertEquals(1, count(markup, "aria-hidden=\"true\""), "the separator, written as a real element");
        assertEquals(1, count(markup, "data-breadcrumb-separator"), "the existing hook, once");
        assertEquals(1, count(markup, "aria-current=\""), "aria-current on the current crumb's own text only");
        assertFalse(markup.contains("::before") || markup.contains("content:"), "the separator is a real hidden element, not generated content");
        for (String forbidden : List.of("role=", "aria-label", "style=", "id=", "tabindex", "onclick", "<button", "<ul", "<ol")) {
            assertFalse(markup.contains(forbidden), "the crumb adds no " + forbidden);
        }
    }

    @Test
    void noTailwindUtilityRemainsInTheTemplate() {
        String markup = withoutJteComments(template);
        for (String utility : List.of("group", "flex", "gap-", "items-center", "break-words", "text-slate", "rounded", "hover:", "focus", "ring", "dark:",
                "underline", "font-medium", "first:", "hidden")) {
            assertFalse(Pattern.compile("(?<![\\w-])" + Pattern.quote(utility)).matcher(markup).find(), "a Tailwind utility is back: " + utility);
        }
    }

    // ---- the stylesheet ----

    private static final String DARK = ":where([data-theme=\"dark\"], [data-theme=\"dark\"] *)";
    private static final String FIRST = ".ash-breadcrumb-item:first-child > .ash-breadcrumb-item__separator";
    private static final String HOVER = ".ash-breadcrumb-item__link:hover";
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
    void everySelectorIsScopedToTheCrumbAndNothingIsGlobalAndOnlyHoverIsConditionalOnADeviceThatCanHover() {
        assertEquals(List.of(".ash-breadcrumb-item", ".ash-breadcrumb-item" + DARK, ".ash-breadcrumb-item__separator", FIRST, ".ash-breadcrumb-item__link",
                HOVER, ".ash-breadcrumb-item__link:focus", ".ash-breadcrumb-item__link:focus-visible", ".ash-breadcrumb-item__text",
                ".ash-breadcrumb-item__text--current"), rules.stream().map(r -> norm(r.selector())).toList());
        for (CssScanner.Rule rule : rules) {
            assertEquals(norm(rule.selector()).equals(HOVER) ? HOVER_MEDIA : "", norm(rule.media()), "only the hover rule is conditional: " + rule.selector());
        }
        assertEquals(1, count(css, "@media"), "one media block: the hover capability, as Tailwind's hover variant was");
        assertFalse(css.contains("@container") || css.contains("@supports"), "no other condition");
        assertFalse(css.contains("prefers-") || css.contains("min-width") || css.contains("max-width"), "the window's width and the user's preferences are never consulted");
        assertFalse(css.contains("!important"));
        assertFalse(css.contains("@import") || css.contains("@layer") || css.contains("@keyframes"), "the entry file assigns the layer");
        assertFalse(css.contains("group"), "no group: the first crumb's rule belongs to the crumb");
    }

    @Test
    void theCrumbIsAFlexRowAndCarriesTheLightPaletteWithExactFallbacks() {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("--ash-breadcrumb-item-separator", "var(--color-slate-400, oklch(70.4% 0.04 256.788))");
        expected.put("--ash-breadcrumb-item-link", "var(--color-slate-500, oklch(55.4% 0.046 257.417))");
        expected.put("--ash-breadcrumb-item-link-hover", "var(--color-slate-700, oklch(37.2% 0.044 257.287))");
        expected.put("--ash-breadcrumb-item-text", "var(--color-slate-500, oklch(55.4% 0.046 257.417))");
        expected.put("--ash-breadcrumb-item-current", "var(--color-slate-900, oklch(20.8% 0.042 265.755))");
        expected.put("--ash-breadcrumb-item-focus", "var(--color-blue-500, oklch(62.3% 0.214 259.815))");
        expected.put("display", "flex");
        expected.put("align-items", "center");
        expected.put("gap", "0.375rem");
        assertEquals(expected, declarations(".ash-breadcrumb-item"));
    }

    @Test
    void theDarkRuleRecoloursEverythingButTheFocusRingWhichIsBlueFiveHundredInBothThemes() {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("--ash-breadcrumb-item-separator", "var(--color-slate-600, oklch(44.6% 0.043 257.281))");
        expected.put("--ash-breadcrumb-item-link", "var(--color-slate-400, oklch(70.4% 0.04 256.788))");
        expected.put("--ash-breadcrumb-item-link-hover", "var(--color-slate-200, oklch(92.9% 0.013 255.508))");
        expected.put("--ash-breadcrumb-item-text", "var(--color-slate-400, oklch(70.4% 0.04 256.788))");
        expected.put("--ash-breadcrumb-item-current", "var(--color-slate-100, oklch(96.8% 0.007 247.896))");
        assertEquals(expected, declarations(".ash-breadcrumb-item" + DARK));
        assertFalse(css.contains("--ash-color-focus"), "the ring is blue-500 in both themes, not the dark focus token");
    }

    @Test
    void theFirstCrumbHidesItsOwnSeparatorThroughItsOwnChildCombinatorAndNothingReachesAnAncestor() {
        assertEquals(Map.of("display", "none"), declarations(FIRST));
        assertTrue(FIRST.startsWith(".ash-breadcrumb-item:first-child > "), "the crumb itself, not an ancestor");
        assertEquals(1, count(FIRST, " > "), "li > separator: the crumb's own separator by a child combinator");
        assertEquals(Map.of("color", "var(--ash-breadcrumb-item-separator)"), declarations(".ash-breadcrumb-item__separator"));
        assertFalse(css.contains("last-child") || css.contains("only-child") || css.contains("nth-") || css.contains("+ ") || css.contains("~ "), "only the first child is special");
    }

    @Test
    void theLinkHasItsOwnColourNoUnderlineAtRestATwoPixelOffsetAndTheSkinsSmallRadius() {
        assertEquals(Map.of("border-radius", "var(--ash-radius-sm)", "overflow-wrap", "break-word", "color", "var(--ash-breadcrumb-item-link)",
                "text-decoration", "none", "text-underline-offset", "2px"), declarations(".ash-breadcrumb-item__link"));
    }

    @Test
    void theHoverRuleIsInsideTheHoverCapabilityMediaQueryAndNothingElseIsAndTheSelectorsAreUnchanged() {
        List<CssScanner.Rule> conditional = rules.stream().filter(r -> !r.media().isEmpty()).toList();
        assertEquals(1, conditional.size());
        assertEquals(HOVER, norm(conditional.get(0).selector()));
        assertEquals(HOVER_MEDIA, norm(conditional.get(0).media()));
        // Focus is a keyboard matter, not a pointer one, and applies whatever the device can do.
        for (String selector : List.of(".ash-breadcrumb-item__link:focus", ".ash-breadcrumb-item__link:focus-visible", ".ash-breadcrumb-item__link")) {
            assertEquals("", norm(rules.stream().filter(r -> norm(r.selector()).equals(selector)).findFirst().orElseThrow().media()), selector);
        }
    }

    @Test
    void hoverRecoloursAndUnderlinesAndFocusTurnsTheBrowserOutlineOffAndDrawsATwoPixelOuterRingOnlyForKeyboardFocus() {
        assertEquals(Map.of("color", "var(--ash-breadcrumb-item-link-hover)", "text-decoration-line", "underline"), declarations(HOVER));
        assertEquals(Map.of("outline-style", "none"), declarations(".ash-breadcrumb-item__link:focus"));
        assertEquals(Map.of("box-shadow", "0 0 0 2px var(--ash-breadcrumb-item-focus)"), declarations(".ash-breadcrumb-item__link:focus-visible"));
        assertFalse(css.contains("inset"), "the ring is outside the link, as before");
    }

    @Test
    void thePlainAndCurrentTextWrapLongWordsAndOnlyTheCurrentCrumbIsMedium() {
        assertEquals(Map.of("overflow-wrap", "break-word", "color", "var(--ash-breadcrumb-item-text)"), declarations(".ash-breadcrumb-item__text"));
        assertEquals(Map.of("font-weight", "500", "color", "var(--ash-breadcrumb-item-current)"), declarations(".ash-breadcrumb-item__text--current"));
    }

    @Test
    void theComponentSetsNoSizeOrFamilyOfItsOwnAndOwnsNothingOfTheIconOrTheListsLayout() {
        assertFalse(css.contains("font-size") || css.contains("line-height") || css.contains("font-family"), "the trail's list sets the text size; the crumb inherits it");
        assertFalse(css.contains("ash-icon") || css.contains("svg") || css.contains("ash-breadcrumbs"), "the crumb knows neither an Icon nor the list around it");
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }
}
