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
 * Pins the source contract of the four layout utilities migrated together: Status Line, Form Actions, Key Value and Key Value List.
 * Each template keeps its parameters and its native element (a {@code <p>}, a {@code <div>}, a {@code <div>} holding a {@code <dt>} and
 * a {@code <dd>}, and a {@code <dl>}), adds no role, no label, no hook and no escape hatch, and carries only Ashlar's classes; each
 * stylesheet is scoped to its own classes. The rendered result is measured in a real browser by the showcase's
 * {@code LayoutUtilitiesParityTest}; this reads the sources only.
 */
class LayoutUtilitiesContractTest {

    private static Path module;

    @BeforeAll
    static void locate() {
        String generated = System.getProperty("ashlar.css.generated");
        assertNotNull(generated, "ashlar.css.generated must be set by the Gradle test task");
        for (Path p = Path.of(generated).toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("src/main/css/ashlar.css"))) {
                module = p;
                break;
            }
        }
        assertNotNull(module, "could not find the ashlar module above " + generated);
    }

    private static String template(String name) throws IOException {
        return Files.readString(module.resolve("src/main/jte/ashlar/utilities/" + name + ".jte"), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static String css(String name) throws IOException {
        return Files.readString(module.resolve("src/main/css/components/" + name + ".css"), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static List<CssScanner.Rule> rules(String name) throws IOException {
        return CssScanner.flatRules(css(name).replaceAll("(?s)/\\*.*?\\*/", ""));
    }

    private static String withoutJteComments(String source) {
        return source.replaceAll("(?s)<%--.*?--%>", "");
    }

    /** The template's markup alone: no comments, no parameter lines, whitespace collapsed. */
    private static String markup(String name) throws IOException {
        return withoutJteComments(template(name)).replaceAll("(?m)^@param .*$", "").replaceAll("\\s+", " ").trim();
    }

    private static List<String> declared(String name) throws IOException {
        List<String> declared = new ArrayList<>();
        Matcher m = Pattern.compile("(?m)^@(?:param|import) .*$").matcher(template(name));
        while (m.find()) {
            declared.add(m.group());
        }
        return declared;
    }

    private static Map<String, String> declarations(String cssName, String selector, String media) throws IOException {
        List<CssScanner.Rule> found = rules(cssName).stream().filter(r -> r.selector().equals(selector) && r.media().equals(media)).toList();
        assertEquals(1, found.size(), selector + " [" + media + "] must be defined exactly once in " + cssName);
        return CssScanner.declarations(found.get(0).block());
    }

    // ---- the templates ----

    @Test
    void theParametersAndTheirDefaultsAreUnchanged() throws IOException {
        assertEquals(List.of("@param gg.jte.Content content"), declared("status-line"));
        assertEquals(List.of("@param gg.jte.Content content"), declared("form-actions"));
        assertEquals(List.of("@param String label", "@param gg.jte.Content value"), declared("key-value"));
        assertEquals(List.of("@param gg.jte.Content content"), declared("key-value-list"));
    }

    @Test
    void eachTemplateKeepsItsNativeElementAndItsContentSlotsUntouched() throws IOException {
        assertEquals("<p class=\"ash-status-line\">${content}</p>", markup("status-line"));
        assertEquals("<div class=\"ash-form-actions\"> ${content} </div>", markup("form-actions"));
        assertEquals("<div class=\"ash-key-value\"> <dt class=\"ash-key-value__label\">${label}</dt> <dd class=\"ash-key-value__value\">${value}</dd> </div>",
                markup("key-value"));
        assertEquals("<dl class=\"ash-key-value-list\"> ${content} </dl>", markup("key-value-list"));
    }

    @Test
    void noTemplateAddsARoleALabelAHookOrAnEscapeHatch() throws IOException {
        for (String name : List.of("status-line", "form-actions", "key-value", "key-value-list")) {
            String markup = withoutJteComments(template(name));
            for (String forbidden : List.of("role=", "aria-", "data-", "style=", "id=", "tabindex")) {
                assertFalse(markup.contains(forbidden), name + " adds no " + forbidden);
            }
        }
    }

    @Test
    void noTailwindUtilityRemainsInAnyTemplate() throws IOException {
        List<String> utilities = List.of("text-sm", "text-slate", "dark:", "flex", "grid", "gap-", "justify-", "items-", "font-medium", "min-w-0", "break-words",
                "sm:", "slate-");
        for (String name : List.of("status-line", "form-actions", "key-value", "key-value-list")) {
            String markup = withoutJteComments(template(name));
            for (String utility : utilities) {
                assertFalse(Pattern.compile("(?<![\\w-])" + Pattern.quote(utility)).matcher(markup).find(), name + ": a Tailwind utility is back: " + utility);
            }
        }
    }

    // ---- the stylesheets ----

    private static void assertScoped(String cssName, List<String> selectors) throws IOException {
        assertEquals(selectors, rules(cssName).stream().map(CssScanner.Rule::selector).toList(), cssName);
        String stripped = css(cssName).replaceAll("(?s)/\\*.*?\\*/", "");
        assertFalse(stripped.contains("!important"), cssName);
        assertFalse(stripped.contains("@import") || stripped.contains("@layer") || stripped.contains("@keyframes"), cssName + ": the entry file assigns the layer");
        assertFalse(Pattern.compile("(^|[,{}]\\s*)(\\*|html|body|dl|dt|dd|p|div)\\s*[,{]").matcher(stripped).find(), cssName + ": no bare element selector");
    }

    @Test
    void everySelectorIsScopedToItsOwnClassesAndNothingIsGlobal() throws IOException {
        String dark = ":where([data-theme=\"dark\"], [data-theme=\"dark\"] *)";
        assertScoped("status-line", List.of(".ash-status-line", ".ash-status-line" + dark));
        assertScoped("form-actions", List.of(".ash-form-actions"));
        assertScoped("key-value", List.of(".ash-key-value", ".ash-key-value" + dark, ".ash-key-value__label", ".ash-key-value__value", ".ash-key-value"));
        assertScoped("key-value-list", List.of(".ash-key-value-list"));
    }

    @Test
    void statusLineIsMutedSmallTextWithItsOwnMarginAndTailwindsExactPaletteFallbacks() throws IOException {
        Map<String, String> line = declarations("status-line", ".ash-status-line", "");
        assertEquals("0", line.get("margin"), "preflight used to remove the paragraph's margin");
        assertEquals("0.875rem", line.get("font-size"));
        assertEquals("var(--ash-status-line-leading)", line.get("line-height"));
        assertEquals("calc(1.25 / 0.875)", line.get("--ash-status-line-leading"), "the ratio is in a property so a minifier cannot fold it");
        assertEquals("var(--color-slate-500, oklch(55.4% 0.046 257.417))", line.get("--ash-status-line-color"));
        assertEquals("var(--ash-status-line-color)", line.get("color"));
        assertEquals(Map.of("--ash-status-line-color", "var(--color-slate-400, oklch(70.4% 0.04 256.788))"),
                declarations("status-line", ".ash-status-line:where([data-theme=\"dark\"], [data-theme=\"dark\"] *)", ""));
    }

    @Test
    void formActionsIsAWrappingEndAlignedRowWithTheSpace3Gap() throws IOException {
        assertEquals(Map.of("display", "flex", "flex-wrap", "wrap", "align-items", "center", "justify-content", "flex-end", "gap", "var(--ash-space-3)"),
                declarations("form-actions", ".ash-form-actions", ""));
        assertTrue(css("form-actions").contains("ash-cluster--end"), "the file explains why it is not the cluster primitive");
        assertFalse(css("form-actions").replaceAll("(?s)/\\*.*?\\*/", "").contains("order"), "it never reorders its content");
    }

    @Test
    void keyValueListIsAFlexColumnWithTheSpace3GapAndNoMarginOfTheBrowsers() throws IOException {
        assertEquals(Map.of("display", "flex", "flex-direction", "column", "gap", "var(--ash-space-3)", "margin", "0"),
                declarations("key-value-list", ".ash-key-value-list", ""));
    }

    @Test
    void keyValueStacksBelowFortyRemAndIsTwoColumnsWithBaselineAlignmentFromFortyRem() throws IOException {
        Map<String, String> pair = declarations("key-value", ".ash-key-value", "");
        assertEquals("grid", pair.get("display"));
        assertEquals("minmax(0, 1fr)", pair.get("grid-template-columns"), "one column below the breakpoint");
        assertEquals("var(--ash-space-6)", pair.get("column-gap"));
        assertEquals("0.25rem", pair.get("row-gap"));
        assertFalse(pair.containsKey("align-items"), "baseline alignment is for the two-column layout only");
        assertEquals(Map.of("grid-template-columns", "minmax(8rem, 12rem) minmax(0, 1fr)", "align-items", "baseline"),
                declarations("key-value", ".ash-key-value", "@media (min-width: 40rem)"), "the breakpoint is Tailwind's sm, 40rem");
    }

    @Test
    void keyValueLabelAndValueOwnTheirMarginsTypographyAndShrinkBehaviour() throws IOException {
        Map<String, String> label = declarations("key-value", ".ash-key-value__label", "");
        assertEquals("0", label.get("margin"));
        assertEquals("0.875rem", label.get("font-size"));
        assertEquals("500", label.get("font-weight"));
        assertEquals("var(--ash-key-value-label)", label.get("color"));
        Map<String, String> value = declarations("key-value", ".ash-key-value__value", "");
        assertEquals("0", value.get("margin"), "a <dd> has a 40px inline-start margin until it is stated away");
        assertEquals("0", value.get("min-width"));
        assertEquals("break-word", value.get("overflow-wrap"));
        assertEquals("0.875rem", value.get("font-size"));
        assertEquals("var(--ash-key-value-value)", value.get("color"));
    }

    @Test
    void keyValueColoursDefaultToTheTailwindPaletteAndFlipInTheDarkTheme() throws IOException {
        Map<String, String> light = declarations("key-value", ".ash-key-value", "");
        assertEquals("var(--color-slate-500, oklch(55.4% 0.046 257.417))", light.get("--ash-key-value-label"));
        assertEquals("var(--color-slate-900, oklch(20.8% 0.042 265.755))", light.get("--ash-key-value-value"));
        assertEquals("calc(1.25 / 0.875)", light.get("--ash-key-value-leading"));
        assertEquals(Map.of("--ash-key-value-label", "var(--color-slate-400, oklch(70.4% 0.04 256.788))",
                        "--ash-key-value-value", "var(--color-slate-100, oklch(96.8% 0.007 247.896))"),
                declarations("key-value", ".ash-key-value:where([data-theme=\"dark\"], [data-theme=\"dark\"] *)", ""));
    }

    // ---- placement ----

    @Test
    void theEntryFileImportsEachInTheComponentsLayerWithKeyValueBeforeItsList() throws IOException {
        String source = Files.readString(module.resolve("src/main/css/ashlar.css"), StandardCharsets.UTF_8);
        for (String name : List.of("status-line", "form-actions", "key-value", "key-value-list")) {
            assertTrue(source.contains("@import \"components/" + name + ".css\" layer(ash.components);"), name);
        }
        assertTrue(source.indexOf("components/key-value.css") < source.indexOf("components/key-value-list.css"));
    }
}
