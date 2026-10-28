package dev.redspartanlabs.ashlar.css;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the Breadcrumbs wrapper's source contract: the template keeps its one parameter, its named navigation landmark and its native ordered
 * list with the {@code data-breadcrumbs} hook (nothing else: no role, no class parameter, no class on the landmark) and carries only
 * Ashlar's class; the stylesheet makes the list a wrapping, centred row with the trail's text size and takes the native list styling away.
 * The rendered result is measured in a real browser by the showcase's {@code BreadcrumbsParityTest}; this reads the sources only.
 */
class BreadcrumbsContractTest {

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
        template = Files.readString(module.resolve("src/main/jte/ashlar/components/breadcrumbs.jte"), StandardCharsets.UTF_8).replace("\r\n", "\n");
        css = Files.readString(module.resolve("src/main/css/components/breadcrumbs.css"), StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "");
        rules = CssScanner.flatRules(css);
    }

    private static String withoutJteComments(String source) {
        return source.replaceAll("(?s)<%--.*?--%>", "");
    }

    // ---- the template ----

    @Test
    void theOneParameterIsUnchanged() {
        List<String> declared = new ArrayList<>();
        Matcher m = Pattern.compile("(?m)^@(?:param|import) .*$").matcher(template);
        while (m.find()) {
            declared.add(m.group());
        }
        assertEquals(List.of("@param gg.jte.Content items"), declared);
    }

    @Test
    void theTrailIsANamedNavigationLandmarkAroundAnOrderedListWithItsHookAndTheItemsAreProjectedUntouched() {
        String markup = withoutJteComments(template).replaceAll("(?m)^@param .*$", "").replaceAll("\\s+", " ").trim();
        assertEquals("<nav aria-label=\"Breadcrumb\" data-breadcrumbs class=\"ash-breadcrumbs\"> <ol class=\"ash-breadcrumbs__list\"> ${items} </ol> </nav>", markup,
                "the landmark is named exactly as the APG names it, and it is the measured wrapper around the ordered list");
    }

    @Test
    void theTrailAddsNoRoleNoStyleAndNoEscapeHatchAndDoesNotKnowItsItems() {
        String markup = withoutJteComments(template);
        for (String forbidden : List.of("role=", "style=", "id=", "ash-breadcrumb-item", "<li", "<a ", "tabindex", "onclick")) {
            assertFalse(markup.contains(forbidden), "the trail adds no " + forbidden);
        }
        assertEquals(2, count(markup, "class=\""), "one class attribute per element");
        assertEquals(1, count(markup, "data-breadcrumbs"), "the existing hook, once");
        assertEquals(1, count(markup, "aria-label=\"Breadcrumb\""), "a fixed landmark name, not a parameter");
        assertEquals(1, count(markup, "<ol"), "an ordered list, not an unordered one");
        assertFalse(markup.contains("<ul"));
    }

    @Test
    void noTailwindUtilityRemainsInTheTemplate() {
        String markup = withoutJteComments(template);
        for (String utility : List.of("flex", "flex-wrap", "items-center", "gap-", "text-sm")) {
            assertFalse(Pattern.compile("(?<![\\w-])" + Pattern.quote(utility)).matcher(markup).find(), "a Tailwind utility is back: " + utility);
        }
    }

    // ---- the stylesheet ----

    private static Map<String, String> declarations(List<CssScanner.Rule> from, String selector) {
        List<CssScanner.Rule> found = from.stream().filter(r -> r.selector().equals(selector)).toList();
        assertEquals(1, found.size(), selector + " must be defined exactly once");
        return CssScanner.declarations(found.get(0).block());
    }

    @Test
    void theOnlySelectorsAreTheLandmarkAndTheListAndNothingIsGlobalOrAMediaQuery() {
        assertEquals(List.of(".ash-breadcrumbs", ".ash-breadcrumbs__list"), rules.stream().map(CssScanner.Rule::selector).toList());
        for (CssScanner.Rule rule : rules) {
            assertEquals("", rule.media(), "no media conditions: " + rule.selector());
        }
        assertFalse(css.contains("@media") || css.contains("@container"), "the trail wraps by itself and consults no width");
        assertFalse(css.contains("!important"));
        assertFalse(css.contains("@import") || css.contains("@layer") || css.contains("@keyframes"), "the entry file assigns the layer");
        assertFalse(css.contains("ash-breadcrumb-item"), "the list does not know its items");
    }

    @Test
    void theListIsAWrappingCentredRowWithTheNativeListResetAndTheTrailsTextSize() {
        Map<String, String> expected = new java.util.LinkedHashMap<>();
        expected.put("--ash-breadcrumbs-leading", "calc(1.25 / 0.875)");
        expected.put("display", "flex");
        expected.put("flex-wrap", "wrap");
        expected.put("align-items", "center");
        expected.put("column-gap", "0.375rem");
        expected.put("row-gap", "0.25rem");
        expected.put("margin", "0");
        expected.put("padding", "0");
        expected.put("list-style", "none");
        expected.put("font-size", "0.875rem");
        expected.put("line-height", "var(--ash-breadcrumbs-leading)");
        assertEquals(expected, declarations(rules, ".ash-breadcrumbs__list"),
                "a wrapping row, 0.375rem along the line and 0.25rem between lines, no marker, margin or padding, and text-sm's size and line height (the ratio in a property so a minifier cannot fold it)");
    }

    @Test
    void theLandmarkOwnsOnlyItsOwnBlockDisplayAndSetsNoTextSizeSoItStaysTheSizeOfTheTextAroundIt() {
        assertEquals(Map.of("display", "block"), declarations(rules, ".ash-breadcrumbs"));
    }

    // ---- placement ----

    @Test
    void theEntryFileImportsTheListBeforeTheItemAfterTheBadgeInTheComponentsLayer() throws IOException {
        String source = Files.readString(module.resolve("src/main/css/ashlar.css"), StandardCharsets.UTF_8);
        int badge = source.indexOf("@import \"components/badge.css\" layer(ash.components);");
        int list = source.indexOf("@import \"components/breadcrumbs.css\" layer(ash.components);");
        int item = source.indexOf("@import \"components/breadcrumb-item.css\" layer(ash.components);");
        assertTrue(badge >= 0 && list > badge && item > list);
        assertEquals(1, count(source, "components/breadcrumbs.css"));
        assertEquals(1, count(source, "components/breadcrumb-item.css"));
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }
}
