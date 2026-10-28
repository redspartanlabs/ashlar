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
 * Pins the Timeline wrapper's source contract: the template keeps its one parameter and its native ordered-list semantics (an {@code <ol>}
 * with the {@code data-timeline} hook and nothing else: no role, no label, no class parameter) and carries only Ashlar's class; the
 * stylesheet is scoped to {@code ash-timeline} and owns only the list itself, a flex column with the native list reset, and knows nothing
 * about the entries projected into it. The rendered result is measured in a real browser by the showcase's {@code TimelineParityTest}; this
 * reads the sources only.
 */
class TimelineContractTest {

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
        template = Files.readString(module.resolve("src/main/jte/ashlar/components/timeline.jte"), StandardCharsets.UTF_8).replace("\r\n", "\n");
        css = Files.readString(module.resolve("src/main/css/components/timeline.css"), StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "");
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
    void theTimelineIsAnOrderedListWithItsHookAndTheItemsAreProjectedUntouched() {
        String markup = withoutJteComments(template).replaceAll("(?m)^@param .*$", "").replaceAll("\\s+", " ").trim();
        assertEquals("<ol data-timeline class=\"ash-timeline\"> ${items} </ol>", markup);
    }

    @Test
    void theTimelineAddsNoRoleNoLabelNoStyleAndNoEscapeHatchAndDoesNotKnowItsItems() {
        String markup = withoutJteComments(template);
        for (String forbidden : List.of("role=", "aria-", "style=", "id=", "ash-timeline-item", "<li")) {
            assertFalse(markup.contains(forbidden), "the timeline adds no " + forbidden);
        }
        assertEquals(1, count(markup, "class=\""), "one class attribute");
        assertEquals(1, count(markup, "data-timeline"), "the existing hook, once");
    }

    @Test
    void noTailwindUtilityRemainsInTheTemplate() {
        String markup = withoutJteComments(template);
        for (String utility : List.of("flex", "flex-col", "gap-", "p-", "m-", "list-")) {
            assertFalse(Pattern.compile("(?<![\\w-])" + Pattern.quote(utility)).matcher(markup).find(), "a Tailwind utility is back: " + utility);
        }
    }

    // ---- the stylesheet ----

    private static Map<String, String> declarations(String selector) {
        List<CssScanner.Rule> found = rules.stream().filter(r -> r.selector().equals(selector)).toList();
        assertEquals(1, found.size(), selector + " must be defined exactly once");
        return CssScanner.declarations(found.get(0).block());
    }

    @Test
    void theOnlySelectorIsTheTimelineItselfAndNothingIsGlobalOrConditional() {
        assertEquals(List.of(".ash-timeline"), rules.stream().map(CssScanner.Rule::selector).toList());
        for (CssScanner.Rule rule : rules) {
            assertEquals("", rule.media(), "no media conditions: " + rule.selector());
        }
        assertFalse(css.contains("!important"));
        assertFalse(css.contains("@import") || css.contains("@layer") || css.contains("@keyframes"), "the entry file assigns the layer");
        assertFalse(css.contains("ash-timeline-item") || css.contains("> ") || css.contains("::"), "the wrapper does not know its entries");
    }

    @Test
    void theWrapperIsAFlexColumnWithTheNativeListResetIncludingNoNumerals() {
        assertEquals(Map.of("display", "flex", "flex-direction", "column", "margin", "0", "padding", "0", "list-style", "none"), declarations(".ash-timeline"),
                "no marker (an <ol> shows no numerals, as before), no margin and no padding; nothing about colour, size or radius");
    }

    // ---- placement ----

    @Test
    void theEntryFileImportsTheTimelineBeforeTheTimelineItemInTheComponentsLayer() throws IOException {
        String source = Files.readString(module.resolve("src/main/css/ashlar.css"), StandardCharsets.UTF_8);
        int timeline = source.indexOf("@import \"components/timeline.css\" layer(ash.components);");
        int item = source.indexOf("@import \"components/timeline-item.css\" layer(ash.components);");
        int spinner = source.indexOf("@import \"components/spinner.css\" layer(ash.components);");
        assertTrue(spinner >= 0 && timeline > spinner && item > timeline);
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }
}
