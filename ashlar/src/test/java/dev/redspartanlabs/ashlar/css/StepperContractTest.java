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
 * Pins the Stepper wrapper's source contract: the template keeps its one parameter, its container wrapper and its native ordered list with
 * the {@code data-stepper} hook (nothing else: no role, no label, no class parameter) and carries only Ashlar's classes; the stylesheet
 * makes the wrapper a size container, makes the list a column that becomes a row at a container width of 24rem (a container query, never
 * a media query), restarts the counter {@code step}, and owns the native list reset. The rendered result is measured in a real browser by
 * the showcase's {@code StepperParityTest}; this reads the sources only. The container block is inspected here, locally, because the shared
 * scanner flattens only media and supports blocks.
 */
class StepperContractTest {

    private static String template;
    private static String css;
    private static List<CssScanner.Rule> rules;
    private static List<CssScanner.Item> containerBlocks;
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
        template = Files.readString(module.resolve("src/main/jte/ashlar/components/stepper.jte"), StandardCharsets.UTF_8).replace("\r\n", "\n");
        css = Files.readString(module.resolve("src/main/css/components/stepper.css"), StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "");
        rules = CssScanner.flatRules(css);
        containerBlocks = CssScanner.items(css).stream().filter(i -> i.prelude().startsWith("@container")).toList();
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
    void theStepperIsAContainerWrapperAroundAnOrderedListWithItsHookAndTheItemsAreProjectedUntouched() {
        String markup = withoutJteComments(template).replaceAll("(?m)^@param .*$", "").replaceAll("\\s+", " ").trim();
        assertEquals("<div class=\"ash-stepper\"> <ol data-stepper class=\"ash-stepper__list\"> ${items} </ol> </div>", markup,
                "the measured element (the wrapper) and the element that reacts to it (the list) are two elements");
    }

    @Test
    void theStepperAddsNoRoleNoLabelNoStyleAndNoEscapeHatchAndDoesNotKnowItsItems() {
        String markup = withoutJteComments(template);
        for (String forbidden : List.of("role=", "aria-", "style=", "id=", "ash-stepper-item", "<li")) {
            assertFalse(markup.contains(forbidden), "the stepper adds no " + forbidden);
        }
        assertEquals(2, count(markup, "class=\""), "one class attribute per element");
        assertEquals(1, count(markup, "data-stepper"), "the existing hook, once");
    }

    @Test
    void noTailwindUtilityRemainsInTheTemplate() {
        String markup = withoutJteComments(template);
        for (String utility : List.of("@container", "@sm:", "flex", "gap-", "[counter-reset", "counter-reset")) {
            assertFalse(markup.contains(utility), "a Tailwind utility is back: " + utility);
        }
    }

    // ---- the stylesheet ----

    private static Map<String, String> declarations(List<CssScanner.Rule> from, String selector) {
        List<CssScanner.Rule> found = from.stream().filter(r -> r.selector().equals(selector)).toList();
        assertEquals(1, found.size(), selector + " must be defined exactly once");
        return CssScanner.declarations(found.get(0).block());
    }

    @Test
    void theOnlySelectorsAreTheWrapperAndTheListAndNothingIsGlobalOrAMediaQuery() {
        assertEquals(List.of(".ash-stepper", ".ash-stepper__list"), rules.stream().map(CssScanner.Rule::selector).toList());
        for (CssScanner.Rule rule : rules) {
            assertEquals("", rule.media(), "no media conditions: " + rule.selector());
        }
        assertFalse(css.contains("@media"), "the responsive switch is a container query, never a viewport media query");
        assertFalse(css.contains("!important"));
        assertFalse(css.contains("@import") || css.contains("@layer") || css.contains("@keyframes"), "the entry file assigns the layer");
        assertFalse(css.contains("ash-stepper-item"), "the wrapper does not know its items");
    }

    @Test
    void theWrapperIsTheSizeContainerAndNothingElse() {
        assertEquals(Map.of("container-type", "inline-size"), declarations(rules, ".ash-stepper"), "a container cannot answer its own query, so it is a different element from the list");
    }

    @Test
    void theListIsAColumnWithTheNativeListResetAndRestartsTheCounterNamedStep() {
        assertEquals(Map.of("display", "flex", "flex-direction", "column", "gap", "var(--ash-space-6)", "margin", "0", "padding", "0", "list-style", "none",
                "counter-reset", "step"), declarations(rules, ".ash-stepper__list"),
                "1.5rem between stacked steps, no marker (an <ol> shows no numerals), no margin, no padding, and the counter named step as it always was");
    }

    @Test
    void theListBecomesARowWithNoGapAtAContainerWidthOfTwentyFourRemExactly() {
        assertEquals(1, containerBlocks.size());
        assertEquals("@container (min-width: 24rem)", containerBlocks.get(0).prelude().replaceAll("\\s+", " "), "24rem is Tailwind's @sm container breakpoint");
        List<CssScanner.Rule> inside = CssScanner.flatRules(containerBlocks.get(0).body());
        assertEquals(List.of(".ash-stepper__list"), inside.stream().map(CssScanner.Rule::selector).toList());
        assertEquals(Map.of("flex-direction", "row", "gap", "0"), declarations(inside, ".ash-stepper__list"));
    }

    // ---- placement ----

    @Test
    void theEntryFileImportsTheStepperBeforeTheStepperItemAfterTheSpinnerInTheComponentsLayer() throws IOException {
        String source = Files.readString(module.resolve("src/main/css/ashlar.css"), StandardCharsets.UTF_8);
        int spinner = source.indexOf("@import \"components/spinner.css\" layer(ash.components);");
        int stepper = source.indexOf("@import \"components/stepper.css\" layer(ash.components);");
        int item = source.indexOf("@import \"components/stepper-item.css\" layer(ash.components);");
        assertTrue(spinner >= 0 && stepper > spinner && item > stepper);
        assertEquals(1, count(source, "components/stepper.css"));
        assertEquals(1, count(source, "components/stepper-item.css"));
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }
}
