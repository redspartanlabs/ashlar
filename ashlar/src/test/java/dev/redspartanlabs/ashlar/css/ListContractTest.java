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
 * Pins the List's source contract: the template keeps its two parameters and its native list semantics (an {@code <ul>}, or an
 * {@code <ol>} when ordered, and nothing else: no role, no label, no hook, no class parameter) and carries only Ashlar's class; the
 * stylesheet is scoped to {@code ash-list}, owns the native list reset (including for an ordered list), the surface, the skin-aware
 * radius token, and the separators between EVERY direct child but the last, with no specificity. The rendered result is measured in a
 * real browser by the showcase's {@code ListParityTest}; this reads the sources only.
 */
class ListContractTest {

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
        template = Files.readString(module.resolve("src/main/jte/ashlar/components/list.jte"), StandardCharsets.UTF_8).replace("\r\n", "\n");
        css = Files.readString(module.resolve("src/main/css/components/list.css"), StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "");
        rules = CssScanner.flatRules(css);
    }

    private static String withoutJteComments(String source) {
        return source.replaceAll("(?s)<%--.*?--%>", "");
    }

    // ---- the template ----

    @Test
    void theTwoParametersAndTheirDefaultsAreUnchanged() {
        List<String> declared = new ArrayList<>();
        Matcher m = Pattern.compile("(?m)^@(?:param|import) .*$").matcher(template);
        while (m.find()) {
            declared.add(m.group());
        }
        assertEquals(List.of("@param gg.jte.Content items", "@param boolean ordered = false"), declared);
    }

    @Test
    void anUnorderedListIsAUlAndAnOrderedListIsAnOlAndTheItemsAreProjectedUntouched() {
        String markup = withoutJteComments(template).replaceAll("(?m)^@param .*$", "").replaceAll("\\s+", " ").trim();
        assertEquals("@if (ordered) <ol class=\"ash-list\"> ${items} </ol> @else <ul class=\"ash-list\"> ${items} </ul> @endif", markup);
    }

    @Test
    void theListAddsNoRoleNoLabelNoHookAndNoEscapeHatch() {
        String markup = withoutJteComments(template);
        for (String forbidden : List.of("role=", "aria-", "data-", "style=", "id=")) {
            assertFalse(markup.contains(forbidden), "the list adds no " + forbidden);
        }
        assertEquals(2, count(markup, "class=\""), "one class attribute per branch, both the same");
    }

    @Test
    void noTailwindUtilityRemainsInTheTemplate() {
        String markup = withoutJteComments(template);
        for (String utility : List.of("divide-", "overflow-hidden", "rounded", "border", "dark:", "slate-")) {
            assertFalse(Pattern.compile("(?<![\\w-])" + Pattern.quote(utility)).matcher(markup).find(), "a Tailwind utility is back: " + utility);
        }
    }

    // ---- the stylesheet ----

    private static final String DARK = ".ash-list:where([data-theme=\"dark\"], [data-theme=\"dark\"] *)";
    private static final String SEPARATOR = ":where(.ash-list > :not(:last-child))";

    private static String block(String selector) {
        List<CssScanner.Rule> found = rules.stream().filter(r -> r.selector().equals(selector)).toList();
        assertEquals(1, found.size(), selector + " must be defined exactly once");
        return found.get(0).block();
    }

    private static Map<String, String> declarations(String selector) {
        return CssScanner.declarations(block(selector));
    }

    @Test
    void everySelectorIsScopedToTheListAndNothingIsGlobalOrConditional() {
        assertEquals(List.of(".ash-list", DARK, SEPARATOR), rules.stream().map(CssScanner.Rule::selector).toList());
        for (CssScanner.Rule rule : rules) {
            assertEquals("", rule.media(), "no media conditions: " + rule.selector());
        }
        assertFalse(css.contains("!important"));
        assertFalse(css.contains("@import") || css.contains("@layer") || css.contains("@keyframes"), "the entry file assigns the layer; a list does not animate");
    }

    @Test
    void theSurfaceOwnsTheNativeListResetIncludingForAnOrderedListAndNoMarker() {
        Map<String, String> list = declarations(".ash-list");
        assertEquals("none", list.get("list-style"), "no marker, and an <ol> shows no numerals either, exactly as before");
        assertEquals("0", list.get("margin"));
        assertEquals("0", list.get("padding"));
        assertEquals("hidden", list.get("overflow"));
        assertEquals("1px solid var(--ash-list-border)", list.get("border"), "a border is not drawn without a style, and preflight is gone");
    }

    @Test
    void theRadiusIsTheSkinAwareTokenThatEqualsTailwindsRoundedLgAndNeverALiteral() {
        assertEquals("var(--ash-radius-lg)", declarations(".ash-list").get("border-radius"));
        String tokens = readTokens();
        assertTrue(tokens.contains("--ash-radius-lg: var(--radius-lg, 0.5rem);"), "the token must still be Tailwind's own radius variable with its 0.5rem fallback");
        assertFalse(css.contains("0.5rem") || css.contains("8px"), "a literal radius would not follow a skin (comments are stripped from css above)");
    }

    private static String readTokens() {
        try {
            return Files.readString(module.resolve("src/main/css/tokens/tokens.css"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void theColoursDefaultToTheTailwindPaletteWithItsExactFallbacksAndFlipInTheDarkTheme() {
        Map<String, String> light = declarations(".ash-list");
        assertEquals("var(--color-slate-200, oklch(92.9% 0.013 255.508))", light.get("--ash-list-border"));
        assertEquals("var(--color-slate-200, oklch(92.9% 0.013 255.508))", light.get("--ash-list-divider"));
        assertEquals(Map.of(
                "--ash-list-border", "var(--color-slate-700, oklch(37.2% 0.044 257.287))",
                "--ash-list-divider", "var(--color-slate-700, oklch(37.2% 0.044 257.287))"), declarations(DARK));
    }

    @Test
    void theSeparatorIsTheListsAndAppliesToEveryDirectChildButTheLastWithNoSpecificity() {
        assertEquals(Map.of("border-top-style", "solid", "border-bottom-style", "solid", "border-top-width", "0", "border-bottom-width", "1px",
                "border-color", "var(--ash-list-divider)"), declarations(SEPARATOR), "exactly what divide-y set: a bottom line, nothing on the left or right");
        assertTrue(SEPARATOR.startsWith(":where(") && SEPARATOR.contains(" > :not(:last-child)"), "any direct child, never a descendant, never only list items");
        assertFalse(css.contains("ash-list-item"), "the list does not know its items; the separator selects direct children, whatever they are");
        assertFalse(css.contains("first-child") || css.contains("only-child") || css.contains("nth-"), "only the last child is special");
    }

    // ---- placement ----

    @Test
    void theEntryFileImportsTheListBeforeTheListItemInTheComponentsLayer() throws IOException {
        String source = Files.readString(module.resolve("src/main/css/ashlar.css"), StandardCharsets.UTF_8);
        int list = source.indexOf("@import \"components/list.css\" layer(ash.components);");
        int item = source.indexOf("@import \"components/list-item.css\" layer(ash.components);");
        assertTrue(list >= 0 && item > list);
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }
}
