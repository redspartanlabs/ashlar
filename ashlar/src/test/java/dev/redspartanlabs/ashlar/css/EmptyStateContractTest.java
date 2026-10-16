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
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the Empty State's source contract: the template keeps its four parameters, its structure and its accessibility
 * semantics and carries only Ashlar's own classes (a Tailwind utility sneaking back in would make the component depend on
 * Tailwind again), and its stylesheet is scoped to {@code ash-empty-state} classes, declares the three customisation
 * properties with palette fallbacks, and holds the one approved reset for paragraphs. The rendered result is measured in a
 * real browser by the showcase's parity tests; this reads the sources only.
 */
class EmptyStateContractTest {

    private static String template;
    private static List<CssScanner.Rule> rules;
    private static String css;

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
        template = Files.readString(module.resolve("src/main/jte/ashlar/components/empty-state.jte"), StandardCharsets.UTF_8).replace("\r\n", "\n");
        css = Files.readString(module.resolve("src/main/css/components/empty-state.css"), StandardCharsets.UTF_8);
        rules = CssScanner.flatRules(css);
    }

    private static String withoutJteComments(String source) {
        return source.replaceAll("(?s)<%--.*?--%>", "");
    }

    // ---- the template ----

    @Test
    void theFourParametersAndTheirDefaultsAreUnchanged() {
        List<String> declared = new ArrayList<>();
        Matcher m = Pattern.compile("(?m)^@(?:param|import) .*$").matcher(template);
        while (m.find()) {
            declared.add(m.group());
        }
        assertEquals(List.of("@import dev.redspartanlabs.ashlar.icon.Icon", "@param String title", "@param gg.jte.Content body = null",
                "@param Icon icon = null", "@param gg.jte.Content action = null"), declared);
    }

    @Test
    void everyClassAttributeIsOneOfAshlarsFiveAndTheIconIsGivenOnlyTheGlyphClass() {
        String markup = withoutJteComments(template);
        Set<String> values = new TreeSet<>();
        Matcher m = Pattern.compile("class=\"([^\"]*)\"").matcher(markup);
        while (m.find()) {
            values.add(m.group(1));
        }
        assertEquals(Set.of("ash-empty-state", "ash-empty-state__icon", "ash-empty-state__title", "ash-empty-state__body", "ash-empty-state__action"), values);
        assertTrue(markup.contains("@template.ashlar.components.icon(icon = icon, classes = \"ash-empty-state__glyph\")"),
                "the icon is rendered by the Icon component with Ashlar's glyph class and no label (it stays decorative)");
        assertFalse(markup.contains("label ="), "an icon label would make it an accessible name that repeats the title");
    }

    @Test
    void theStructureOrderConditionsAndSemanticsArePreserved() {
        String markup = withoutJteComments(template);
        assertEquals(1, count(markup, "data-empty-state"));
        assertTrue(markup.contains("<div class=\"ash-empty-state\" data-empty-state>"));
        assertEquals(1, count(markup, "<h3 class=\"ash-empty-state__title\">${title}</h3>"));
        assertEquals(0, count(markup, "<h2") + count(markup, "<h4") + count(markup, "role=") + count(markup, "aria-"));
        int icon = markup.indexOf("@if (icon != null)");
        int title = markup.indexOf("<h3");
        int body = markup.indexOf("@if (body != null)");
        int action = markup.indexOf("@if (action != null)");
        assertTrue(icon >= 0 && icon < title && title < body && body < action, "icon, title, body, action in that order");
        assertTrue(markup.contains("${body}") && markup.contains("${action}"));
        assertEquals(3, count(markup, "@endif"));
    }

    @Test
    void noTailwindUtilityRemainsInTheTemplate() {
        String markup = withoutJteComments(template);
        for (String utility : List.of("flex", "items-center", "px-6", "py-12", "text-center", "mb-3", "text-slate", "dark:", "h-10", "w-10",
                "text-base", "font-semibold", "mt-1", "max-w-sm", "text-sm", "mt-4", "flex-wrap", "justify-center", "gap-3")) {
            assertFalse(Pattern.compile("(?<![\\w-])" + Pattern.quote(utility)).matcher(markup).find(), "a Tailwind utility is back: " + utility);
        }
    }

    // ---- the stylesheet ----

    private static String block(String selector) {
        List<CssScanner.Rule> found = rules.stream().filter(r -> r.selector().equals(selector)).toList();
        assertEquals(1, found.size(), selector + " must be defined exactly once");
        return found.get(0).block();
    }

    private static Map<String, String> declarations(String selector) {
        return CssScanner.declarations(block(selector));
    }

    @Test
    void everySelectorIsScopedToAnEmptyStateClassAndNothingIsGlobal() {
        assertFalse(rules.isEmpty());
        for (CssScanner.Rule rule : rules) {
            assertEquals("", rule.media(), "no media conditions: " + rule.selector());
            for (String selector : CssScanner.selectors(rule.selector())) {
                assertTrue(selector.startsWith(".ash-empty-state"), "not scoped to an Empty State class: " + selector);
                assertFalse(selector.contains("*") && !selector.contains("[data-theme"), "no universal selector: " + selector);
            }
        }
        assertFalse(css.contains("!important"));
        assertFalse(css.contains("@import") || css.contains("@layer"), "the entry file assigns the layer, never the module");
    }

    @Test
    void theApprovedParagraphResetIsExactlyOneRuleOnDirectParagraphChildrenOfTheBody() {
        assertEquals(Map.of("margin", "0"), declarations(".ash-empty-state__body > p"));
        List<String> paragraphRules = rules.stream().map(CssScanner.Rule::selector).filter(s -> s.contains(" p") || s.endsWith("p")).toList();
        assertEquals(List.of(".ash-empty-state__body > p"), paragraphRules);
    }

    @Test
    void theThreeCustomisationPropertiesDefaultToTheTailwindPaletteWithItsExactFallbacksAndFlipInTheDarkTheme() {
        Map<String, String> light = declarations(".ash-empty-state");
        assertEquals("var(--color-slate-400, oklch(70.4% 0.04 256.788))", light.get("--ash-empty-state-icon"));
        assertEquals("var(--color-slate-900, oklch(20.8% 0.042 265.755))", light.get("--ash-empty-state-title"));
        assertEquals("var(--color-slate-500, oklch(55.4% 0.046 257.417))", light.get("--ash-empty-state-body"));
        Map<String, String> dark = declarations(".ash-empty-state:where([data-theme=\"dark\"], [data-theme=\"dark\"] *)");
        assertEquals(Set.of("--ash-empty-state-icon", "--ash-empty-state-title", "--ash-empty-state-body"), dark.keySet());
        assertEquals("var(--color-slate-500, oklch(55.4% 0.046 257.417))", dark.get("--ash-empty-state-icon"));
        assertEquals("var(--color-slate-100, oklch(96.8% 0.007 247.896))", dark.get("--ash-empty-state-title"));
        assertEquals("var(--color-slate-400, oklch(70.4% 0.04 256.788))", dark.get("--ash-empty-state-body"));
    }

    @Test
    void theGeometryIsTheOneTheTailwindClassesProduced() {
        Map<String, String> wrapper = declarations(".ash-empty-state");
        assertEquals("flex", wrapper.get("display"));
        assertEquals("column", wrapper.get("flex-direction"));
        assertEquals("center", wrapper.get("align-items"));
        assertEquals("3rem 1.5rem", wrapper.get("padding"));
        assertEquals("center", wrapper.get("text-align"));
        assertEquals("0.75rem", declarations(".ash-empty-state__icon").get("margin-bottom"));
        Map<String, String> body = declarations(".ash-empty-state__body");
        assertEquals("0.25rem", body.get("margin-top"));
        assertEquals("24rem", body.get("max-width"));
        assertEquals("0.875rem", body.get("font-size"));
        Map<String, String> action = declarations(".ash-empty-state__action");
        assertEquals("0.75rem", action.get("gap"));
        assertEquals("1rem", action.get("margin-top"));
        assertEquals("wrap", action.get("flex-wrap"));
        assertEquals("center", action.get("justify-content"));
        Map<String, String> title = declarations(".ash-empty-state__title");
        assertEquals("0", title.get("margin"));
        assertEquals("1rem", title.get("font-size"));
        assertEquals("600", title.get("font-weight"));
    }

    @Test
    void theGlyphStatesWhatTheIconsTailwindClassesAndPreflightDid() {
        // The Icon component still renders inline-block and shrink-0 as Tailwind utilities; without Tailwind they do nothing,
        // and preflight's svg vertical-align: middle is gone too, so the glyph states all three itself.
        Map<String, String> glyph = declarations(".ash-empty-state__glyph");
        assertEquals("inline-block", glyph.get("display"));
        assertEquals("0", glyph.get("flex-shrink"));
        assertEquals("middle", glyph.get("vertical-align"));
        assertEquals("2.5rem", glyph.get("width"));
        assertEquals("2.5rem", glyph.get("height"));
    }

    @Test
    void theEntryFileImportsTheModuleIntoTheComponentsLayerAfterDivider() throws IOException {
        Path entry = Path.of(System.getProperty("ashlar.css.generated")).toAbsolutePath();
        Path module = entry;
        while (module != null && !Files.isRegularFile(module.resolve("src/main/css/ashlar.css"))) {
            module = module.getParent();
        }
        assertNotNull(module);
        String source = Files.readString(module.resolve("src/main/css/ashlar.css"), StandardCharsets.UTF_8);
        int divider = source.indexOf("@import \"components/divider.css\" layer(ash.components);");
        int emptyState = source.indexOf("@import \"components/empty-state.css\" layer(ash.components);");
        assertTrue(divider >= 0 && emptyState > divider);
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }
}
