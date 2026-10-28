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
 * Pins the Stepper Item's source contract: the template keeps its two required parameters, its {@code <li>} with {@code aria-current} on the
 * current step only, its decorative connector row and indicator, the label with its status text (now {@code ash-sr-only}) and the check
 * Icon sized by Ashlar's own Icon modifier, and says the step's state once, as a modifier on the {@code <li>}; the stylesheet is scoped to
 * {@code ash-stepper-item}, reads every state colour from that modifier, keeps the counter {@code step} and the indicator's generated number,
 * keeps the indicator round whatever the skin, hides the last step's own line through its own connector, and follows the container's 24rem
 * breakpoint. The rendered result is measured in a real browser by the showcase's {@code StepperParityTest}; this reads the sources only.
 */
class StepperItemContractTest {

    private static String template;
    private static String css;
    private static List<CssScanner.Rule> rules;
    private static List<CssScanner.Item> containerBlocks;

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
        template = Files.readString(module.resolve("src/main/jte/ashlar/components/stepper-item.jte"), StandardCharsets.UTF_8).replace("\r\n", "\n");
        css = Files.readString(module.resolve("src/main/css/components/stepper-item.css"), StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "");
        rules = CssScanner.flatRules(css);
        containerBlocks = CssScanner.items(css).stream().filter(i -> i.prelude().startsWith("@container")).toList();
    }

    private static String withoutJteComments(String source) {
        return source.replaceAll("(?s)<%--.*?--%>", "");
    }

    // ---- the template ----

    @Test
    void theTwoRequiredParametersAndTheirNoDefaultsAreUnchanged() {
        List<String> declared = new ArrayList<>();
        Matcher m = Pattern.compile("(?m)^@(?:param|import) .*$").matcher(template);
        while (m.find()) {
            declared.add(m.group());
        }
        assertEquals(List.of("@import dev.redspartanlabs.ashlar.stepper.StepperStatus", "@import dev.redspartanlabs.ashlar.stepper.StepperStyles",
                "@import dev.redspartanlabs.ashlar.icon.Icon", "@param String label", "@param String status"), declared);
    }

    @Test
    void theStatusStillGoesThroughStepperStatusFromSoAnUnknownValueIsAnUpcomingStepAndOnlyTheCurrentStepIsMarked() {
        String markup = withoutJteComments(template);
        assertTrue(markup.contains("StepperStatus resolvedStatus = StepperStatus.from(status);"));
        assertTrue(markup.contains("boolean isCompleted = resolvedStatus == StepperStatus.COMPLETED;"));
        assertTrue(markup.contains("String ariaCurrentAttr = resolvedStatus == StepperStatus.CURRENT ? \"step\" : null;"));
        assertTrue(markup.contains("String statusText = StepperStyles.statusText(resolvedStatus);"));
        assertTrue(markup.contains("String indicatorClasses = StepperStyles.indicator(resolvedStatus);"));
        assertTrue(markup.contains("String connectorClasses = StepperStyles.connector(resolvedStatus);"));
        assertTrue(markup.contains("String labelClasses = StepperStyles.label(resolvedStatus);"));
    }

    @Test
    void theStateIsOneModifierOnTheLiBuiltFromTheResolvedStatus() {
        String markup = withoutJteComments(template);
        assertTrue(markup.contains("String itemClasses = \"ash-stepper-item ash-stepper-item--\" + resolvedStatus.name().toLowerCase();"));
        assertEquals(1, count(markup, "ash-stepper-item--"), "the state is written once, here");
    }

    @Test
    void theStepIsAnLiWithAConnectorRowAnIndicatorAndALabelInTheApprovedStructure() {
        String markup = withoutJteComments(template).replaceAll("(?m)^(@param|@import) .*$", "").replaceAll("(?m)^!\\{.*$", "").replaceAll("\\s+", " ").trim();
        assertEquals("<li aria-current=\"${ariaCurrentAttr}\" class=\"${itemClasses}\">"
                + " <div class=\"ash-stepper-item__connector\" aria-hidden=\"true\"> <div class=\"${connectorClasses}\"></div> </div>"
                + " <span aria-hidden=\"true\" class=\"${indicatorClasses}\">"
                + " @if (isCompleted) @template.ashlar.components.icon(icon = Icon.CHECK, classes = \"ash-icon--sm\") @endif </span>"
                + " <span class=\"ash-stepper-item__text\">"
                + " <span class=\"${labelClasses}\">${label}<span class=\"ash-sr-only\">, ${statusText}</span></span> </span> </li>", markup);
    }

    @Test
    void onlyTheConnectorRowAndTheIndicatorAreHiddenAndNothingElseGetsARoleAHookOrAnEscapeHatch() {
        String markup = withoutJteComments(template);
        assertEquals(2, count(markup, "aria-hidden=\"true\""), "the connector row and the indicator (and with it, its check Icon)");
        assertEquals(1, count(markup, "aria-current=\""), "aria-current sits on the <li> itself");
        for (String forbidden : List.of("role=", "aria-label", "data-", "style=", "id=", "href", "<a ", "<button", "tabindex", "onclick")) {
            assertFalse(markup.contains(forbidden), "the step is not interactive and adds no " + forbidden);
        }
    }

    @Test
    void theStatusTextUsesTheSharedVisuallyHiddenUtilityNotATailwindClass() {
        String markup = withoutJteComments(template);
        assertEquals(1, count(markup, "<span class=\"ash-sr-only\">, ${statusText}</span>"));
        assertFalse(Pattern.compile("(?<![\\w-])sr-only").matcher(markup).find());
    }

    @Test
    void noTailwindUtilityRemainsInTheTemplate() {
        String markup = withoutJteComments(template);
        for (String utility : List.of("group", "relative", "flex", "gap-", "@sm:", "@container", "[counter-", "before:", "min-w-0", "flex-1", "items-", "top-4", "left-0", "right-0",
                "hidden", "h-0.5", "w-full", "z-10", "h-8", "w-8", "shrink-0", "rounded", "text-sm", "font-semibold", "pt-1", "break-words", "h-4", "w-4", "text-center", "slate-", "blue-")) {
            assertFalse(Pattern.compile("(?<![\\w-])" + Pattern.quote(utility)).matcher(markup).find(), "a Tailwind utility is back: " + utility);
        }
    }

    // ---- the stylesheet ----

    private static final String DARK = ":where([data-theme=\"dark\"], [data-theme=\"dark\"] *)";
    private static final String NUMBER = ".ash-stepper-item--current > .ash-stepper-item__indicator::before, .ash-stepper-item--upcoming > .ash-stepper-item__indicator::before";
    private static final String LAST = ".ash-stepper-item:last-child > .ash-stepper-item__connector";

    private static String norm(String selector) {
        return selector.replaceAll("\\s+", " ").trim();
    }

    private static Map<String, String> declarations(List<CssScanner.Rule> from, String selector) {
        List<CssScanner.Rule> found = from.stream().filter(r -> norm(r.selector()).equals(selector)).toList();
        assertEquals(1, found.size(), selector + " must be defined exactly once");
        return CssScanner.declarations(found.get(0).block());
    }

    private static Map<String, String> declarations(String selector) {
        return declarations(rules, selector);
    }

    @Test
    void everySelectorIsScopedToTheStepperItemAndNothingIsGlobalOrAMediaQuery() {
        List<String> expected = new ArrayList<>(List.of(".ash-stepper-item"));
        for (String state : List.of("completed", "current", "upcoming")) {
            expected.add(".ash-stepper-item--" + state);
            expected.add(".ash-stepper-item--" + state + DARK);
        }
        expected.addAll(List.of(".ash-stepper-item__connector", ".ash-stepper-item__line", ".ash-stepper-item__indicator", NUMBER, ".ash-stepper-item__text",
                ".ash-stepper-item__label", ".ash-stepper-item--current .ash-stepper-item__label", LAST));
        assertEquals(expected, rules.stream().map(r -> norm(r.selector())).toList());
        for (CssScanner.Rule rule : rules) {
            assertEquals("", rule.media(), "no media conditions: " + rule.selector());
        }
        assertFalse(css.contains("@media"), "the responsive switch is a container query, never a viewport media query");
        assertFalse(css.contains("!important"));
        assertFalse(css.contains("@import") || css.contains("@layer") || css.contains("@keyframes"), "the entry file assigns the layer");
        assertFalse(css.contains("group"), "no group: the last step's rule belongs to the step");
    }

    @Test
    void theCounterIsTheOneNamedStepAndEveryStepAddsOneToIt() {
        Map<String, String> item = declarations(".ash-stepper-item");
        assertEquals("step", item.get("counter-increment"));
        assertEquals("relative", item.get("position"));
        assertEquals("flex", item.get("display"));
        assertEquals("0", item.get("min-width"));
        assertEquals("1", item.get("flex"));
        assertEquals("flex-start", item.get("align-items"));
        assertEquals("var(--ash-space-3)", item.get("gap"));
        assertEquals("calc(1.25 / 0.875)", item.get("--ash-stepper-item-leading"), "the ratio lives in a property so a minifier cannot fold it");
    }

    @Test
    void theNumberIsGeneratedContentOfTheCounterOnlyForTheCurrentAndUpcomingSteps() {
        assertEquals(Map.of("content", "counter(step)"), declarations(NUMBER));
        assertFalse(NUMBER.contains("completed"), "a completed step shows the check, not a number");
        assertEquals(2, count(NUMBER, " > .ash-stepper-item__indicator::before"), "the step's own indicator, by child combinators");
    }

    @Test
    void theContainerBlockIsTheSameTwentyFourRemAsTheListsAndSwitchesTheStepToACentredColumnWithItsLine() {
        assertEquals(1, containerBlocks.size());
        assertEquals("@container (min-width: 24rem)", norm(containerBlocks.get(0).prelude()));
        List<CssScanner.Rule> inside = CssScanner.flatRules(containerBlocks.get(0).body());
        assertEquals(List.of(".ash-stepper-item", ".ash-stepper-item__connector", ".ash-stepper-item__text"), inside.stream().map(r -> norm(r.selector())).toList());
        assertEquals(Map.of("flex-direction", "column", "align-items", "center", "gap", "var(--ash-space-2)"), declarations(inside, ".ash-stepper-item"));
        assertEquals(Map.of("display", "flex"), declarations(inside, ".ash-stepper-item__connector"));
        assertEquals(Map.of("align-items", "center", "padding-top", "0", "text-align", "center"), declarations(inside, ".ash-stepper-item__text"));
        assertFalse(css.contains("min-width: 40rem") || css.contains("@media"), "the window's width is never consulted");
    }

    @Test
    void theConnectorIsADecorativeRowAtTheIndicatorsMiddleAcrossTheWholeStepHiddenUntilTheContainerIsWide() {
        assertEquals(Map.of("position", "absolute", "top", "var(--ash-space-4)", "right", "0", "left", "0", "display", "none", "align-items", "center"),
                declarations(".ash-stepper-item__connector"));
        assertEquals(Map.of("width", "100%", "height", "0.125rem", "background-color", "var(--ash-stepper-item-line)"), declarations(".ash-stepper-item__line"));
    }

    @Test
    void theLastStepHidesItsOwnConnectorThroughItsOwnChildCombinatorAndNothingReachesAnAncestor() {
        assertEquals(Map.of("display", "none"), declarations(LAST));
        assertTrue(LAST.startsWith(".ash-stepper-item:last-child > "), "the step itself, not an ancestor");
        assertEquals(1, count(LAST, " > "), "li > connector: the step's own connector by a child combinator");
        // The rule sits after the container block so that, at equal order, its higher specificity (and not the order) keeps the last line hidden.
        assertTrue(css.indexOf("@container") < css.indexOf(LAST));
        assertFalse(css.contains("first-child") || css.contains("only-child") || css.contains("nth-") || css.contains("+ ") || css.contains("~ "), "only the last child is special");
    }

    @Test
    void theIndicatorIsATwoRemCircleWithATwoPixelSolidBorderAboveTheLineAndNeverUsesASkinRadius() {
        Map<String, String> indicator = declarations(".ash-stepper-item__indicator");
        assertEquals("2rem", indicator.get("width"));
        assertEquals("2rem", indicator.get("height"));
        assertEquals("2px solid var(--ash-stepper-item-indicator-border)", indicator.get("border"), "a border is not drawn without a style, and preflight is gone");
        assertEquals("calc(infinity * 1px)", indicator.get("border-radius"), "the same full circle rounded-full was, whatever the skin");
        assertEquals("10", indicator.get("z-index"));
        assertEquals("relative", indicator.get("position"));
        assertEquals("0", indicator.get("flex-shrink"));
        assertEquals("flex", indicator.get("display"));
        assertEquals("center", indicator.get("align-items"));
        assertEquals("center", indicator.get("justify-content"));
        assertEquals("var(--ash-stepper-item-indicator-bg)", indicator.get("background-color"));
        assertEquals("var(--ash-stepper-item-indicator-fg)", indicator.get("color"));
        assertEquals("0.875rem", indicator.get("font-size"));
        assertEquals("var(--ash-stepper-item-leading)", indicator.get("line-height"));
        assertEquals("600", indicator.get("font-weight"));
        assertFalse(css.contains("--ash-radius") || css.contains("--radius"), "a skin's radius must not change a circle");
    }

    private static Map<String, String> props(String border, String bg, String fg, String line, String label) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("--ash-stepper-item-indicator-border", border);
        m.put("--ash-stepper-item-indicator-bg", bg);
        if (fg != null) {
            m.put("--ash-stepper-item-indicator-fg", fg);
        }
        m.put("--ash-stepper-item-line", line);
        m.put("--ash-stepper-item-label", label);
        return m;
    }

    @Test
    void eachStateKeepsItsOwnLightAndDarkPaletteWithExactFallbacks() {
        String blue600 = "var(--color-blue-600, oklch(54.6% 0.245 262.881))";
        String blue500 = "var(--color-blue-500, oklch(62.3% 0.214 259.815))";
        String blue400 = "var(--color-blue-400, oklch(70.7% 0.165 254.624))";
        String white = "var(--color-white, #fff)";
        String slate900 = "var(--color-slate-900, oklch(20.8% 0.042 265.755))";
        String slate700 = "var(--color-slate-700, oklch(37.2% 0.044 257.287))";
        String slate600 = "var(--color-slate-600, oklch(44.6% 0.043 257.281))";
        String slate500 = "var(--color-slate-500, oklch(55.4% 0.046 257.417))";
        String slate400 = "var(--color-slate-400, oklch(70.4% 0.04 256.788))";
        String slate300 = "var(--color-slate-300, oklch(86.9% 0.022 252.894))";
        String slate200 = "var(--color-slate-200, oklch(92.9% 0.013 255.508))";
        String slate100 = "var(--color-slate-100, oklch(96.8% 0.007 247.896))";
        assertEquals(props(blue600, blue600, white, blue600, slate700), declarations(".ash-stepper-item--completed"));
        assertEquals(props(blue500, blue500, null, blue500, "var(--color-slate-300, oklch(86.9% 0.022 252.894))"), declarations(".ash-stepper-item--completed" + DARK),
                "the completed check stays white in dark: the dark rule does not set the foreground");
        assertEquals(props(blue600, white, blue600, slate200, slate900), declarations(".ash-stepper-item--current"));
        assertEquals(props(blue500, slate900, blue400, slate700, slate100), declarations(".ash-stepper-item--current" + DARK));
        assertEquals(props(slate300, white, slate400, slate200, slate400), declarations(".ash-stepper-item--upcoming"));
        assertEquals(props(slate600, slate900, slate500, slate700, slate500), declarations(".ash-stepper-item--upcoming" + DARK));
    }

    @Test
    void theLabelColumnAndTheLabelOwnTheirSpacingTypographyAndWrappingAndOnlyTheCurrentLabelIsSemibold() {
        assertEquals(Map.of("display", "flex", "min-width", "0", "flex-direction", "column", "padding-top", "0.25rem"), declarations(".ash-stepper-item__text"));
        assertEquals(Map.of("overflow-wrap", "break-word", "font-size", "0.875rem", "line-height", "var(--ash-stepper-item-leading)", "color", "var(--ash-stepper-item-label)"),
                declarations(".ash-stepper-item__label"));
        assertEquals(Map.of("font-weight", "600"), declarations(".ash-stepper-item--current .ash-stepper-item__label"));
    }

    @Test
    void theIconIsSizedByTheIconAndTheComponentSetsNothingOnTheSrOnlyOrTheCallersContent() {
        assertFalse(css.contains("ash-icon") || css.contains("svg") || css.contains("sr-only"), "the Icon owns its size and the utility owns the hiding");
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }
}
