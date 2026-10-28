package dev.redspartanlabs.ashlar.showcase.cascade;

import dev.redspartanlabs.ashlar.showcase.catalog.ComponentCatalog;
import dev.redspartanlabs.ashlar.showcase.catalog.ComponentPage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Stepper's and the Stepper Item's parity gates, measured in real Chrome, on the shared parity harness (unchanged).
 *
 * <p>Evidence is a controlled oracle: the legacy Tailwind markup (compiled by the real Tailwind CLI) next to the migrated markup, in the
 * same browser, in four container widths for each of the 22 theme and skin configurations, with Tailwind present and with Tailwind removed.
 * The widths are chosen around the Stepper's container breakpoint, 24rem (384px): 320 and 383 are below it (a column of rows, no lines),
 * 384 is exactly at it and 420 is above it (a row of centred columns with a line behind each indicator). The cases are one step, three
 * (completed, current, upcoming), five, and long labels including an unbroken word. Every part of every step is measured: the container,
 * the list, the step, the connector and its line, the indicator and its check Icon, the label's column, the label and the status text.
 *
 * <p>The number an indicator shows is generated content ({@code ::before}), which the shared probe cannot read. It is compared instead
 * through the indicator's size: in a few extra frames the indicator is let shrink to its content, identically on both sides, so its width is
 * the width of the generated text. Steps 10 and 11 are wider than step 9, which only holds if the counter really counts, and a second
 * Stepper on the same page restarts from 1. A control without the number, and one without the increment, are detected.
 *
 * <p>One behavior deliberately differs from the legacy rendering and is therefore NOT compared with it: a Stepper placed under an unrelated
 * last element. The legacy line was hidden by {@code group-last:hidden}, which matches any {@code .group:last-child} ancestor, so every line
 * of such a Stepper vanished. The step now hides only its own line. That case is measured on its own, next to the legacy rendering that shows
 * the old leak, and a control shows the old over-reach would be caught.
 *
 * <p>Evidence is Chrome only. Computed-style and box parity is the visual evidence; there is no screenshot comparison.
 */
class StepperParityTest {

    private static final String ORACLE = ParityFrames.generated("stepper-oracle.css");
    private static final String SCOPED = ParityFrames.generated("stepper-legacy-scoped.css");
    private static final List<ParityConfigs.Config> CONFIGS = ParityConfigs.all();
    private static final List<String> EXTRA_PROPS = List.of("container-type", "counter-reset", "counter-increment", "flex-direction", "column-gap", "row-gap",
            "align-items", "justify-content", "list-style-type", "list-style-position", "line-height", "font-size", "font-weight", "text-align", "overflow-wrap",
            "z-index", "text-decoration-line");

    private record Layout(String name, String style) {
    }

    private static final List<Layout> LAYOUTS = List.of(
            new Layout("stacked", "box-sizing:border-box;width:320px"),
            new Layout("below", "box-sizing:border-box;width:383px"),
            new Layout("at", "box-sizing:border-box;width:384px"),
            new Layout("wide", "box-sizing:border-box;width:420px"));

    private static final String LONG = "Business information and tax details for the account";
    private static final String UNBROKEN = "Supercalifragilisticexpialidocious".repeat(3);

    private static final Map<String, List<StepperFixtures.Entry>> CASES = new LinkedHashMap<>();

    static {
        CASES.put("single", List.of(StepperFixtures.entry("single-1", "Account", "current")));
        CASES.put("three", List.of(
                StepperFixtures.entry("three-1", "Account", "completed"),
                StepperFixtures.entry("three-2", "Details", "current"),
                StepperFixtures.entry("three-3", "Review", "upcoming")));
        CASES.put("five", List.of(
                StepperFixtures.entry("five-1", "Cart", "completed"),
                StepperFixtures.entry("five-2", "Shipping", "completed"),
                StepperFixtures.entry("five-3", "Payment", "current"),
                StepperFixtures.entry("five-4", "Review", "upcoming"),
                StepperFixtures.entry("five-5", "Confirm", "upcoming")));
        CASES.put("long", List.of(
                StepperFixtures.entry("long-1", LONG, "completed"),
                StepperFixtures.entry("long-2", UNBROKEN, "current"),
                StepperFixtures.entry("long-3", "Complete your application and review", "upcoming")));
    }

    private static final List<Layout> CONTROL_LAYOUTS = List.of(LAYOUTS.get(0), LAYOUTS.get(3));
    private static final java.util.Set<String> CONTROL_CASES = java.util.Set.of("three", "five", "long");

    // ---- the counter cases: the indicator shrinks to its content, so the generated number has a width ----

    private static final String SHRINK = "[data-parity=\"indicator\"]{width:auto !important}";
    private static final List<ParityConfigs.Config> COUNTER_CONFIGS = List.of(ParityConfigs.get("light/none"), ParityConfigs.get("dark/none"), ParityConfigs.get("light/axiom"));

    private static List<StepperFixtures.Entry> counterEntries(String prefix, int count, int completed, int current) {
        List<StepperFixtures.Entry> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(StepperFixtures.entry(prefix + "-" + i, "Step " + i, i <= completed ? "completed" : i == current ? "current" : "upcoming"));
        }
        return list;
    }

    // ---- state of the measurement ----

    private static StepperLegacyMarkup legacy;
    private static StepperFixtures fixtures;
    private static ParityProbe.Probe probe;
    private static final Map<String, List<ParityProbe.Finding>> PRESENT = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> REMOVED_SCOPED = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> REMOVED_FULL = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> SCOPED_FIDELITY = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> COUNTERS = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> CONTROLS = new LinkedHashMap<>();
    private static final Map<String, Integer> APPLIED_PRESENT = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_REMOVED = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_SCOPED = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_COUNTERS = new TreeMap<>();
    private static final Map<String, Integer> ROWS = new LinkedHashMap<>();

    // ---- markup ----

    private static String markup(boolean legacyClasses) {
        return markup(legacyClasses, LAYOUTS, CASES.keySet());
    }

    private static String markup(boolean legacyClasses, List<Layout> layouts, java.util.Set<String> caseNames) {
        StringBuilder sb = new StringBuilder();
        for (Layout layout : layouts) {
            for (Map.Entry<String, List<StepperFixtures.Entry>> c : CASES.entrySet()) {
                if (!caseNames.contains(c.getKey())) {
                    continue;
                }
                StringBuilder items = new StringBuilder();
                for (StepperFixtures.Entry e : c.getValue()) {
                    items.append(fixtures.item(e, legacyClasses, true));
                }
                sb.append("<div style=\"margin:4px 0\"><div data-layout=\"").append(layout.name()).append("\" style=\"").append(layout.style()).append("\">")
                        .append(fixtures.stepper(c.getKey(), items.toString(), legacyClasses, true)).append("</div></div>");
            }
        }
        return sb.toString();
    }

    /** Two Steppers on one page, one with eleven steps and one with three: the second must count from 1 again. */
    private static String counterMarkup(boolean legacyClasses) {
        StringBuilder first = new StringBuilder();
        for (StepperFixtures.Entry e : counterEntries("ctr", 11, 1, 2)) {
            first.append(fixtures.item(e, legacyClasses, true));
        }
        StringBuilder second = new StringBuilder();
        for (StepperFixtures.Entry e : counterEntries("ctr2", 3, 0, 0)) {
            second.append(fixtures.item(e, legacyClasses, true));
        }
        return "<div data-layout=\"counters\" style=\"box-sizing:border-box;width:320px\">" + fixtures.stepper("ctr", first.toString(), legacyClasses, true)
                + fixtures.stepper("ctr2", second.toString(), legacyClasses, true) + "</div>";
    }

    /** A Stepper under an unrelated element that is a last child and carries the {@code group} class the legacy line rule looked for. */
    private static String groupedMarkup(boolean legacyClasses) {
        StringBuilder items = new StringBuilder();
        for (StepperFixtures.Entry e : CASES.get("three")) {
            items.append(fixtures.item(new StepperFixtures.Entry("grp-" + e.name(), e.label(), e.status()), legacyClasses, true));
        }
        return "<div data-layout=\"grouped\" style=\"box-sizing:border-box;width:420px\"><div class=\"group\">" + fixtures.stepper("grouped", items.toString(), legacyClasses, true) + "</div></div>";
    }

    // ---- control experiments: each breaks the migrated Stepper in ONE way and must be detected ----

    private record Control(String name, String config, String css, String expectedProperty) {
    }

    private static final List<Control> CONTROL_LIST = List.of(
            new Control("list stays a column when wide", "light/none", "@container (min-width: 24rem){.ash-stepper__list{flex-direction:column}}", "flex-direction"),
            new Control("steps stay a row when wide", "light/none", "@container (min-width: 24rem){.ash-stepper-item{flex-direction:row}}", "flex-direction"),
            new Control("wrong gap between stacked steps", "light/none", ".ash-stepper__list{gap:1rem}", "row-gap"),
            new Control("wrong gap inside a step", "light/none", ".ash-stepper-item{gap:0.5rem}", "column-gap"),
            new Control("connector at the wrong height", "light/none", ".ash-stepper-item__connector{top:0}", "@y"),
            new Control("the last step keeps its line", "light/none", ".ash-stepper-item:last-child > .ash-stepper-item__connector{display:flex}", "display"),
            new Control("line is the wrong thickness", "light/none", ".ash-stepper-item__line{height:4px}", "height"),
            new Control("wrong completed line colour", "light/none", ".ash-stepper-item--completed{--ash-stepper-item-line:red}", "background-color"),
            new Control("dark line ignores the theme", "dark/none", ".ash-stepper-item--completed{--ash-stepper-item-line:var(--color-blue-600)}", "background-color"),
            new Control("indicator radius follows the skin", "light/axiom", ".ash-stepper-item__indicator{border-radius:var(--ash-radius-lg)}", "border-top-left-radius"),
            new Control("indicator border is three pixels", "light/none", ".ash-stepper-item__indicator{border-width:3px}", "border-top-width"),
            new Control("indicator is the wrong size", "light/none", ".ash-stepper-item__indicator{width:1.5rem}", "width"),
            new Control("indicator sits under the line", "light/none", ".ash-stepper-item__indicator{z-index:auto}", "z-index"),
            new Control("indicator number has the wrong size", "light/none", ".ash-stepper-item__indicator{font-size:1rem}", "font-size"),
            new Control("indicator number is not semibold", "light/none", ".ash-stepper-item__indicator{font-weight:500}", "font-weight"),
            new Control("wrong current number colour", "light/none", ".ash-stepper-item--current{--ash-stepper-item-indicator-fg:red}", "color"),
            new Control("wrong completed check colour", "light/none", ".ash-stepper-item--completed{--ash-stepper-item-indicator-fg:red}", "color"),
            new Control("dark current fill ignores the theme", "dark/none", ".ash-stepper-item--current{--ash-stepper-item-indicator-bg:var(--color-white)}", "background-color"),
            new Control("colour ignores the skin", "light/civic", ".ash-stepper-item--completed{--ash-stepper-item-indicator-bg:#2563eb}", "background-color"),
            new Control("wrong upcoming label colour", "light/none", ".ash-stepper-item--upcoming{--ash-stepper-item-label:red}", "color"),
            new Control("current label is not semibold", "light/none", ".ash-stepper-item--current .ash-stepper-item__label{font-weight:500}", "font-weight"),
            new Control("label is not centred when wide", "light/none", ".ash-stepper-item__text{text-align:left}", "text-align"),
            new Control("label loses its stacked top padding", "light/none", ".ash-stepper-item__text{padding-top:0}", "padding-top"),
            new Control("long words no longer break", "light/none", ".ash-stepper-item__label{overflow-wrap:normal}", "overflow-wrap"),
            new Control("label column can no longer shrink", "light/none", ".ash-stepper-item__text{min-width:auto}", "min-width"),
            new Control("step can no longer shrink", "light/none", ".ash-stepper-item{min-width:auto}", "min-width"),
            new Control("steps are no longer equal width", "light/none", ".ash-stepper-item{flex:none}", "flex-grow"),
            new Control("list no longer restarts the counter", "light/none", ".ash-stepper__list{counter-reset:none}", "counter-reset"),
            new Control("steps no longer count", "light/none", ".ash-stepper-item{counter-increment:none}", "counter-increment"),
            new Control("wrapper is not a container", "light/none", ".ash-stepper{container-type:normal}", "container-type"),
            new Control("native list padding comes back", "light/none", ".ash-stepper__list{padding-inline-start:40px}", "padding-left"),
            new Control("native list margin comes back", "light/none", ".ash-stepper__list{margin-block:16px}", "margin-top"),
            new Control("the numerals come back", "light/none", "ol.ash-stepper__list{list-style:decimal}", "list-style-type"),
            new Control("status text is no longer visually hidden", "light/none", ".ash-sr-only{position:static;width:auto;height:auto;margin:0;overflow:visible;clip-path:none}", "position"),
            new Control("icon is the wrong size", "light/none", ".ash-stepper-item__indicator .ash-icon{--ash-icon-size:1.25rem}", "width"));

    /** Counter controls, measured in the shrink-to-content frames: the number's generated content must matter. */
    private static final List<Control> COUNTER_CONTROLS = List.of(
            new Control("the number is gone", "light/none", ".ash-stepper-item__indicator::before{content:none}", "@w"),
            new Control("steps no longer count (counter)", "light/none", ".ash-stepper-item{counter-increment:none}", "@w"));

    /** The old over-reach, a rule that reaches a connector through an unrelated ancestor, applied to the migrated rendering. */
    private static final String GROUP_OVERREACH = ".group .ash-stepper-item__connector{display:none}";

    private static final List<ParityConfigs.Config> GROUP_CONFIGS = List.of(ParityConfigs.get("light/none"), ParityConfigs.get("dark/none"));

    // ---- running ----

    @BeforeAll
    static void measure() throws Exception {
        ParitySession session = ParitySession.open("stepper-parity");
        legacy = new StepperLegacyMarkup();
        fixtures = new StepperFixtures(legacy, new IconLegacyMarkup());
        String environment = ParityFrames.environment(session.generatedDir());
        String migrated = markup(false);
        String legacyMarkup = markup(true);

        StringBuilder frames = new StringBuilder();
        for (ParityConfigs.Config c : CONFIGS) {
            frames.append(ParityFrames.frame(c.id() + "|L", c, ParityFrames.with(ORACLE, ParityConfigs.SKIN_LINKS), "", legacyMarkup));
            frames.append(ParityFrames.frame(c.id() + "|P", c, ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), "", migrated));
            frames.append(ParityFrames.frame(c.id() + "|R", c, ParityFrames.with(ParityFrames.ASHLAR, ParityConfigs.SKIN_LINKS), environment, migrated));
            frames.append(ParityFrames.frame(c.id() + "|S", c, ParityFrames.with(SCOPED, ParityConfigs.SKIN_LINKS), environment, legacyMarkup));
        }
        String controlMigrated = markup(false, CONTROL_LAYOUTS, CONTROL_CASES);
        String controlLegacy = markup(true, CONTROL_LAYOUTS, CONTROL_CASES);
        for (String configId : CONTROL_LIST.stream().map(Control::config).distinct().toList()) {
            frames.append(ParityFrames.frame(configId + "|LC", ParityConfigs.get(configId), ParityFrames.with(ORACLE, ParityConfigs.SKIN_LINKS), "", controlLegacy));
        }
        for (Control control : CONTROL_LIST) {
            frames.append(ParityFrames.frame("ctl:" + control.name() + "|P", ParityConfigs.get(control.config()),
                    ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), control.css(), controlMigrated));
        }
        // The counter frames: the indicator shrinks to its content, identically on both sides.
        String counterMigrated = counterMarkup(false);
        String counterLegacy = counterMarkup(true);
        for (ParityConfigs.Config c : COUNTER_CONFIGS) {
            frames.append(ParityFrames.frame(c.id() + "|CL", c, ParityFrames.with(ORACLE, ParityConfigs.SKIN_LINKS), SHRINK, counterLegacy));
            frames.append(ParityFrames.frame(c.id() + "|CP", c, ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), SHRINK, counterMigrated));
            frames.append(ParityFrames.frame(c.id() + "|CR", c, ParityFrames.with(ParityFrames.ASHLAR, ParityConfigs.SKIN_LINKS), environment + SHRINK, counterMigrated));
            frames.append(ParityFrames.frame(c.id() + "|CS", c, ParityFrames.with(SCOPED, ParityConfigs.SKIN_LINKS), environment + SHRINK, counterLegacy));
        }
        for (Control control : COUNTER_CONTROLS) {
            frames.append(ParityFrames.frame("ctr-ctl:" + control.name() + "|P", ParityConfigs.get(control.config()),
                    ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), SHRINK + control.css(), counterMigrated));
        }
        // The group ancestor: not compared with the legacy rendering (that is the corrected behavior); measured next to it.
        String groupedMigrated = groupedMarkup(false);
        String groupedLegacy = groupedMarkup(true);
        for (ParityConfigs.Config c : GROUP_CONFIGS) {
            frames.append(ParityFrames.frame(c.id() + "|G", c, ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), "", groupedMigrated));
            frames.append(ParityFrames.frame(c.id() + "|GR", c, ParityFrames.with(ParityFrames.ASHLAR, ParityConfigs.SKIN_LINKS), environment, groupedMigrated));
            frames.append(ParityFrames.frame(c.id() + "|GL", c, ParityFrames.with(ORACLE, ParityConfigs.SKIN_LINKS), "", groupedLegacy));
        }
        frames.append(ParityFrames.frame("group-overreach|G", ParityConfigs.get("light/none"), ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), GROUP_OVERREACH, groupedMigrated));

        probe = ParityFrames.measure(session, frames.toString(), EXTRA_PROPS, "stepper frames");
        List<String> props = probe.props();
        for (ParityConfigs.Config c : CONFIGS) {
            List<ParityProbe.Row> legacyRows = probe.phase(c.id() + "|L");
            ROWS.put(c.id() + "|L", legacyRows.size());
            PRESENT.put(c.id(), ParityProbe.compare(c.id() + " (Tailwind present)", props, legacyRows, probe.phase(c.id() + "|P"), APPLIED_PRESENT));
            REMOVED_SCOPED.put(c.id(), ParityProbe.compare(c.id() + " (Tailwind removed vs scoped legacy)", props, probe.phase(c.id() + "|S"), probe.phase(c.id() + "|R"), APPLIED_REMOVED));
            REMOVED_FULL.put(c.id(), ParityProbe.compare(c.id() + " (Tailwind removed vs full legacy)", props, legacyRows, probe.phase(c.id() + "|R"), APPLIED_REMOVED));
            SCOPED_FIDELITY.put(c.id(), ParityProbe.compare(c.id() + " (scoped legacy vs full legacy)", props, legacyRows, probe.phase(c.id() + "|S"), APPLIED_SCOPED));
        }
        for (Control control : CONTROL_LIST) {
            CONTROLS.put(control.name(), ParityProbe.compare("control: " + control.name(), props, probe.phase(control.config() + "|LC"),
                    probe.phase("ctl:" + control.name() + "|P"), new TreeMap<>()));
        }
        for (ParityConfigs.Config c : COUNTER_CONFIGS) {
            COUNTERS.put(c.id() + " present", ParityProbe.compare(c.id() + " (counters, Tailwind present)", props, probe.phase(c.id() + "|CL"), probe.phase(c.id() + "|CP"), new TreeMap<>()));
            COUNTERS.put(c.id() + " removed", ParityProbe.compare(c.id() + " (counters, Tailwind removed)", props, probe.phase(c.id() + "|CS"), probe.phase(c.id() + "|CR"), APPLIED_COUNTERS));
        }
        for (Control control : COUNTER_CONTROLS) {
            CONTROLS.put(control.name(), ParityProbe.compare("control: " + control.name(), props, probe.phase("light/none|CL"), probe.phase("ctr-ctl:" + control.name() + "|P"), new TreeMap<>()));
        }
    }

    /** The measurements are tens of thousands of rows; every suite in this JVM is run one after another, so this one lets go of them when it is done. */
    @AfterAll
    static void clear() {
        probe = null;
        List.of(PRESENT, REMOVED_SCOPED, REMOVED_FULL, SCOPED_FIDELITY, COUNTERS, CONTROLS).forEach(Map::clear);
        List.of(APPLIED_PRESENT, APPLIED_REMOVED, APPLIED_SCOPED, APPLIED_COUNTERS, ROWS).forEach(Map::clear);
    }

    private static List<String> lines(Map<String, List<ParityProbe.Finding>> findings) {
        // A broken stylesheet makes tens of thousands of findings; the first few say what moved, and building a message from all of them runs out of heap.
        List<String> out = new ArrayList<>();
        long total = 0;
        for (List<ParityProbe.Finding> list : findings.values()) {
            for (ParityProbe.Finding f : list) {
                if (out.size() < 20) {
                    out.add(f.line());
                }
                total++;
            }
        }
        if (total > out.size()) {
            out.add("(" + (total - out.size()) + " more findings not shown)");
        }
        return out;
    }

    private static ParityProbe.Row one(String phase, String kind, String name, String layout) {
        List<ParityProbe.Row> found = probe.phase(phase).stream().filter(r -> r.kind().equals(kind) && r.variant().equals(name + "/" + layout)).toList();
        assertEquals(1, found.size(), phase + " " + kind + " " + name + "/" + layout);
        return found.get(0);
    }

    private static String text(String phase, String kind, String name, String layout, String property) {
        return probe.value(one(phase, kind, name, layout), property);
    }

    private static double num(String phase, String kind, String name, String layout, String property) {
        return Double.parseDouble(text(phase, kind, name, layout, property).replace("px", ""));
    }

    private static final List<String> OWN_PHASES = List.of("light/none|L", "light/none|P", "light/none|R", "dark/none|L", "dark/none|P", "dark/none|R");

    private static boolean horizontal(String layout) {
        return layout.equals("at") || layout.equals("wide");
    }

    // ---- tests ----

    @Test
    void theConfigurationsAreTheTwentyTwoThemeAndSkinCombinations() {
        assertEquals(22, CONFIGS.size());
    }

    @Test
    void controlledOracleWithTailwindPresentMatchesTheLegacyStepperInEveryConfigurationCaseAndWidth() {
        assertEquals(List.of(), lines(PRESENT));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheLegacyStepperGivenTheSameSurroundings() {
        assertEquals(List.of(), lines(REMOVED_SCOPED));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheFullLegacyRendering() {
        assertEquals(List.of(), lines(REMOVED_FULL));
    }

    @Test
    void theScopedLegacyStylesheetRendersTheLegacyStepperExactlyAsTheFullOneDoes() {
        assertEquals(List.of(), lines(SCOPED_FIDELITY));
    }

    @Test
    void theGeneratedNumberHasTheLegacyWidthInEveryCounterConfigurationWithAndWithoutTailwind() {
        assertEquals(List.of(), lines(COUNTERS));
    }

    @Test
    void everyNegativeControlIsDetectedInTheProperty() {
        for (Control control : java.util.stream.Stream.concat(CONTROL_LIST.stream(), COUNTER_CONTROLS.stream()).toList()) {
            List<ParityProbe.Finding> findings = CONTROLS.get(control.name());
            assertFalse(findings.isEmpty(), "the control was NOT detected: " + control.name());
            assertTrue(findings.stream().anyMatch(f -> f.property().equals(control.expectedProperty())),
                    control.name() + " should be detected in " + control.expectedProperty() + " but was detected in "
                            + findings.stream().map(ParityProbe.Finding::property).collect(Collectors.toCollection(TreeSet::new)));
        }
    }

    @Test
    void theCasesAreCompleteEveryPartIsMeasuredAndTheNarrowWidthsReallyWrapTheLabel() {
        Map<String, Integer> kinds = new TreeMap<>();
        for (ParityProbe.Row r : probe.phase("light/none|L")) {
            kinds.merge(r.kind(), 1, Integer::sum);
        }
        for (String kind : List.of("stepper", "list", "item", "connector", "line", "indicator", "glyph", "text", "label", "status")) {
            assertTrue(kinds.getOrDefault(kind, 0) > 0, "no " + kind + " was measured: " + kinds);
        }
        long steps = CASES.values().stream().mapToLong(List::size).sum();
        assertEquals(steps * LAYOUTS.size(), kinds.get("item").longValue());
        assertEquals((long) CASES.size() * LAYOUTS.size(), kinds.get("stepper").longValue());
        assertTrue(ROWS.values().stream().allMatch(n -> n.equals(ROWS.get("light/none|L"))), "every configuration measures the same rows");
        // A one-line label is 20px tall; the long one wraps onto several lines when narrow.
        double oneLine = num("light/none|L", "label", "three-1", "wide", "@h");
        assertEquals(20.0, oneLine, 0.01);
        assertTrue(num("light/none|L", "label", "long-1", "wide", "@h") > oneLine * 2, "the long label wraps in a horizontal step");
        // The unbroken word is broken instead of pushing the step out of its box.
        for (String phase : OWN_PHASES) {
            for (Layout layout : LAYOUTS) {
                double stepper = num(phase, "stepper", "long", layout.name(), "@w");
                for (String name : List.of("long-1", "long-2", "long-3")) {
                    assertTrue(num(phase, "item", name, layout.name(), "@w") <= stepper + 0.01, phase + " " + layout.name() + " " + name + " stays inside the Stepper");
                }
            }
        }
    }

    @Test
    void theLayoutFollowsTheContainersWidthAndSwitchesExactlyAtTwentyFourRem() {
        for (String phase : OWN_PHASES) {
            for (Layout layout : LAYOUTS) {
                String where = phase + " " + layout.name();
                assertEquals(horizontal(layout.name()) ? "row" : "column", text(phase, "list", "three", layout.name(), "flex-direction"), where + " list");
                assertEquals(horizontal(layout.name()) ? "0px" : "24px", text(phase, "list", "three", layout.name(), "row-gap"), where + " list gap");
                assertEquals(horizontal(layout.name()) ? "column" : "row", text(phase, "item", "three-1", layout.name(), "flex-direction"), where + " step");
                assertEquals(horizontal(layout.name()) ? "center" : "flex-start", text(phase, "item", "three-1", layout.name(), "align-items"), where + " step alignment");
                assertEquals(horizontal(layout.name()) ? "8px" : "12px", text(phase, "item", "three-1", layout.name(), "row-gap"), where + " step gap");
                assertEquals(horizontal(layout.name()) ? "center" : "start", text(phase, "label", "three-1", layout.name(), "text-align"), where + " label");
            }
        }
        // Exactly at the breakpoint (384px) it is horizontal; one pixel less (383px) it is not: the query is the container's width, not the window's.
        assertEquals("column", text("light/none|P", "list", "three", "below", "flex-direction"), "383px is still a column of rows");
        assertEquals("row", text("light/none|P", "list", "three", "at", "flex-direction"), "384px is a row of columns");
        assertEquals("column", text("light/none|P", "item", "three-1", "at", "flex-direction"));
        assertEquals("inline-size", text("light/none|P", "stepper", "three", "wide", "container-type"));
        assertEquals("inline-size", text("light/none|L", "stepper", "three", "wide", "container-type"));
    }

    @Test
    void theLineRunsBehindEachIndicatorAcrossTheWholeStepAndTheLastStepAndAStackedStepperHaveNone() {
        for (String phase : OWN_PHASES) {
            for (Layout layout : LAYOUTS) {
                for (Map.Entry<String, List<StepperFixtures.Entry>> c : CASES.entrySet()) {
                    List<StepperFixtures.Entry> steps = c.getValue();
                    for (int i = 0; i < steps.size(); i++) {
                        String name = steps.get(i).name();
                        String where = phase + " " + layout.name() + " " + name;
                        boolean last = i == steps.size() - 1;
                        if (!horizontal(layout.name()) || last) {
                            assertEquals("none", text(phase, "connector", name, layout.name(), "display"), where + ": no line");
                            continue;
                        }
                        assertEquals("flex", text(phase, "connector", name, layout.name(), "display"), where);
                        double item = num(phase, "item", name, layout.name(), "@w");
                        // Across the whole step, at the indicator's middle, behind it: the first step's line therefore shows as a stub before its indicator.
                        assertEquals(0.0, num(phase, "connector", name, layout.name(), "@x"), 0.01, where + ": from the step's left edge");
                        assertEquals(item, num(phase, "connector", name, layout.name(), "@w"), 0.05, where + ": to its right edge");
                        assertEquals(16.0, num(phase, "connector", name, layout.name(), "@y"), 0.01, where + ": at the indicator's middle");
                        assertEquals(2.0, num(phase, "line", name, layout.name(), "@h"), 0.01, where + ": two pixels thick");
                        assertEquals(item, num(phase, "line", name, layout.name(), "@w"), 0.05, where + ": as wide as its step");
                        assertEquals((item - 32) / 2, num(phase, "indicator", name, layout.name(), "@x"), 0.05, where + ": the indicator is centred on the line");
                        assertTrue(num(phase, "indicator", name, layout.name(), "@x") > 0, where + ": a stub of line shows before the indicator");
                        // Continuous: this step ends where the next begins, so the lines join.
                        assertEquals(num(phase, "item", steps.get(i + 1).name(), layout.name(), "@x"), num(phase, "item", name, layout.name(), "@x") + item, 0.05, where + ": touches the next step");
                    }
                }
            }
        }
        // A single step has no line in any width.
        for (String phase : OWN_PHASES) {
            for (Layout layout : LAYOUTS) {
                assertEquals("none", text(phase, "connector", "single-1", layout.name(), "display"), phase + " " + layout.name());
            }
        }
    }

    @Test
    void theIndicatorIsAThirtyTwoPixelCircleWithATwoPixelSolidBorderAboveTheLineWhateverTheSkin() {
        for (String phase : OWN_PHASES) {
            ParityProbe.Row indicator = one(phase, "indicator", "three-2", "wide");
            assertEquals("32px", probe.value(indicator, "width"), phase);
            assertEquals("32px", probe.value(indicator, "height"), phase);
            for (String side : List.of("top", "right", "bottom", "left")) {
                assertEquals("2px", probe.value(indicator, "border-" + side + "-width"), phase);
                assertEquals("solid", probe.value(indicator, "border-" + side + "-style"), phase);
            }
            assertEquals("10", probe.value(indicator, "z-index"), phase);
            assertEquals("relative", probe.value(indicator, "position"), phase);
            assertEquals("0", probe.value(indicator, "flex-shrink"), phase);
            assertEquals("absolute", text(phase, "connector", "three-2", "wide", "position"), phase);
            assertEquals("16px", text(phase, "glyph", "three-1", "wide", "width"), phase + ": the check is a 16px Icon");
        }
        String plain = text("light/none|P", "indicator", "three-2", "wide", "border-top-left-radius");
        for (String skin : List.of("light/axiom", "light/ledger", "light/harbor")) {
            assertEquals(plain, text(skin + "|P", "indicator", "three-2", "wide", "border-top-left-radius"), skin);
            assertEquals(text(skin + "|L", "indicator", "three-2", "wide", "border-top-left-radius"), text(skin + "|P", "indicator", "three-2", "wide", "border-top-left-radius"), skin);
        }
        assertTrue(Double.parseDouble(plain.replace("px", "")) >= 16, "a full circle (" + plain + ")");
    }

    @Test
    void eachStatusHasItsOwnIndicatorLineAndLabelInBothThemesAndOnlyTheCurrentLabelIsSemibold() {
        for (String theme : List.of("light/none|P", "dark/none|P")) {
            // Completed and current share a border colour, so the three are told apart by border, fill and number colour together.
            List<String> looks = List.of("three-1", "three-2", "three-3").stream()
                    .map(n -> text(theme, "indicator", n, "wide", "border-top-color") + "|" + text(theme, "indicator", n, "wide", "background-color") + "|" + text(theme, "indicator", n, "wide", "color")).toList();
            assertEquals(3, new TreeSet<>(looks).size(), theme + ": " + looks);
            assertNotEquals(text(theme, "line", "three-1", "wide", "background-color"), text(theme, "line", "three-2", "wide", "background-color"), theme + ": a completed step's line differs");
            assertEquals(text(theme, "line", "three-2", "wide", "background-color"), text(theme, "line", "five-4", "wide", "background-color"), theme + ": every other step's line is the same grey");
            assertEquals("600", text(theme, "label", "three-2", "wide", "font-weight"), theme);
            assertEquals("400", text(theme, "label", "three-1", "wide", "font-weight"), theme);
            assertEquals("400", text(theme, "label", "three-3", "wide", "font-weight"), theme);
            assertEquals(3, new TreeSet<>(List.of("three-1", "three-2", "three-3").stream().map(n -> text(theme, "label", n, "wide", "color")).toList()).size(), theme + " labels");
        }
        for (String name : List.of("three-1", "three-2", "three-3")) {
            assertNotEquals(text("light/none|P", "indicator", name, "wide", "background-color"), text("dark/none|P", "indicator", name, "wide", "background-color"), name + " fill differs in dark");
        }
        // The completed check is white in both themes, as before; the legacy rendering agrees.
        assertEquals(text("light/none|L", "indicator", "three-1", "wide", "color"), text("light/none|P", "indicator", "three-1", "wide", "color"));
        assertEquals(text("dark/none|L", "indicator", "three-1", "wide", "color"), text("dark/none|P", "indicator", "three-1", "wide", "color"));
    }

    @Test
    void theListRestartsTheCounterAndEveryStepCountsAndTheContainerIsMeasured() {
        for (String phase : OWN_PHASES) {
            assertEquals("step 0", text(phase, "list", "three", "wide", "counter-reset"), phase);
            assertEquals("step 1", text(phase, "item", "three-2", "wide", "counter-increment"), phase);
            assertEquals("inline-size", text(phase, "stepper", "three", "wide", "container-type"), phase);
        }
    }

    @Test
    void theGeneratedNumbersGrowWithTheCountAndASecondStepperOnThePageStartsAgainAtOne() {
        for (ParityConfigs.Config c : COUNTER_CONFIGS) {
            for (String suffix : List.of("CL", "CP", "CR", "CS")) {
                String phase = c.id() + "|" + suffix;
                double step9 = num(phase, "indicator", "ctr-9", "counters", "@w");
                double step10 = num(phase, "indicator", "ctr-10", "counters", "@w");
                double step11 = num(phase, "indicator", "ctr-11", "counters", "@w");
                assertTrue(step10 > step9 + 2 && step11 > step9 + 2, phase + ": two digits are wider than one (" + step9 + ", " + step10 + ", " + step11 + ")");
                // A second Stepper restarts at 1: its third step shows "3", one digit, as wide as the first Stepper's step 3 and narrower than step 10.
                double second = num(phase, "indicator", "ctr2-3", "counters", "@w");
                assertEquals(num(phase, "indicator", "ctr-3", "counters", "@w"), second, 0.05, phase + ": the second Stepper counts from 1 again");
                assertTrue(second < step10, phase);
                // The completed step shows a check, whose width is the 16px Icon plus the border.
                assertEquals(20.0, num(phase, "indicator", "ctr-1", "counters", "@w"), 0.05, phase + ": a check in a 2px border");
            }
        }
    }

    @Test
    void aStepperUnderAnUnrelatedLastGroupElementKeepsItsLinesAndTheLegacyRenderingShowsTheLeakThatWasCorrected() {
        for (ParityConfigs.Config c : GROUP_CONFIGS) {
            for (String suffix : List.of("G", "GR")) {
                String phase = c.id() + "|" + suffix;
                assertEquals("flex", text(phase, "connector", "grp-three-1", "grouped", "display"), phase + ": a first step keeps its line");
                assertEquals("flex", text(phase, "connector", "grp-three-2", "grouped", "display"), phase + ": so does a middle step");
                assertEquals("none", text(phase, "connector", "grp-three-3", "grouped", "display"), phase + ": the last step still has none");
            }
            // The legacy rule matched the unrelated ancestor, so no line of that Stepper was drawn.
            String legacyPhase = c.id() + "|GL";
            assertEquals("none", text(legacyPhase, "connector", "grp-three-1", "grouped", "display"), legacyPhase + ": the old rule reached the line under an unrelated .group:last-child");
            assertEquals("none", text(legacyPhase, "connector", "grp-three-2", "grouped", "display"), legacyPhase);
        }
    }

    @Test
    void theOldOverReachWouldBeCaughtByTheGroupGate() {
        assertEquals("none", text("group-overreach|G", "connector", "grp-three-1", "grouped", "display"), "a rule that reaches the group ancestor hides the line again, and the gate sees it");
        assertEquals("flex", text("light/none|G", "connector", "grp-three-1", "grouped", "display"));
    }

    @Test
    void theOracleIsNotVacuousEveryLegacyClassIsCompiledInTheCumulativeAndScopedStylesheets() throws IOException {
        Path dir = Path.of(System.getProperty("ashlar.cascade.dir"));
        String oracle = Files.readString(dir.resolve("stepper-oracle.css"), StandardCharsets.UTF_8);
        String scoped = Files.readString(dir.resolve("stepper-legacy-scoped.css"), StandardCharsets.UTF_8);
        // The shared oracle check does not escape "@", "(" or ")", which this component's classes use, so they are checked here with Tailwind's own escaping.
        // "group" is a marker class Tailwind writes no rule for; every other legacy class is compiled (variants are written as pseudo-class, where() or container selectors).
        List<String> tokens = legacy.allTokens().stream().filter(t -> !t.equals("group")).toList();
        assertEquals(List.of(), tokens.stream().filter(t -> !compiles(oracle, t)).toList(), "the Stepper oracle does not compile these");
        assertEquals(List.of(), tokens.stream().filter(t -> !compiles(scoped, t)).toList(), "the scoped stylesheet does not compile these");
        assertTrue(oracle.contains("@container (width>=24rem)") || oracle.contains("@container (width >= 24rem)") || oracle.contains("@container (min-width:24rem)")
                || oracle.contains("@container (min-width: 24rem)"), "the legacy container query is the 24rem one");
        for (String icon : List.of("inline-block", "shrink-0", "h-4", "w-4")) {
            assertTrue(ParityOracle.uncompiled(scoped, List.of(icon)).isEmpty(), "the scoped stylesheet lost the Icon's legacy class " + icon);
        }
        List<String> earlier = new ArrayList<>();
        new BadgeLegacyMarkup().variants().values().forEach(classes -> earlier.addAll(Arrays.asList(classes.split("\\s+"))));
        earlier.addAll(new AlertLegacyMarkup().allTokens());
        earlier.addAll(new DividerLegacyMarkup().allTokens());
        earlier.addAll(new ProgressLegacyMarkup().allTokens());
        earlier.addAll(new EmptyStateLegacyMarkup().allTokens());
        earlier.addAll(new IconLegacyMarkup().allTokens());
        earlier.addAll(new SpinnerLegacyMarkup().allTokens());
        earlier.addAll(new SkeletonLegacyMarkup().allTokens());
        earlier.addAll(new ListLegacyMarkup().allTokens().stream().filter(t -> !t.startsWith("divide-") && !t.equals("dark:divide-slate-700")).toList());
        earlier.addAll(new TimelineLegacyMarkup().allTokens().stream().filter(t -> !t.equals("group")).toList());
        assertEquals(List.of(), ParityOracle.uncompiled(oracle, earlier), "the Stepper oracle lost earlier legacy classes");
        assertFalse(scoped.contains("html,:host"), "the scoped legacy stylesheet must not carry Tailwind's page-wide base");
        assertFalse(oracle.contains("ash-stepper") || scoped.contains("ash-stepper"), "the legacy oracle holds no migrated Stepper rule");
    }

    private static boolean compiles(String css, String token) {
        String selector = "." + token.replaceAll("([:/.\\[\\]@()])", "\\\\$1");
        return css.contains(selector + "{") || css.contains(selector + ":") || css.contains(selector + ",");
    }

    @Test
    void normalizationsStayWithinTheApprovedRulesAndTailwindPresentNeedsNone() {
        assertEquals(Map.of(), APPLIED_PRESENT, "Tailwind present needs no normalization at all");
        assertEquals(Map.of(), APPLIED_SCOPED, "the scoped legacy rendering must match the full one without any normalization");
        // Tailwind removed: only H2 (a border style is invisible at width 0; preflight gave solid, the browser gives none). Every legacy border is solid,
        // so each side where the migrated element has no border style at all is exactly one use, in both removed comparisons. The indicator's border is
        // real (2px solid) and uses none.
        long none = 0;
        for (ParityConfigs.Config c : CONFIGS) {
            for (ParityProbe.Row legacyRow : probe.phase(c.id() + "|L")) {
                for (String side : List.of("top", "right", "bottom", "left")) {
                    assertEquals("solid", probe.value(legacyRow, "border-" + side + "-style"), "every legacy border style is solid (preflight)");
                }
            }
            for (ParityProbe.Row row : probe.phase(c.id() + "|R")) {
                for (String side : List.of("top", "right", "bottom", "left")) {
                    if (probe.value(row, "border-" + side + "-style").equals("none")) {
                        none++;
                    }
                }
            }
        }
        assertTrue(none > 0);
        assertEquals(Map.of("H2", (int) (2 * none)), APPLIED_REMOVED, "exactly H2, one use per borderless side, in both removed comparisons");
        assertTrue(APPLIED_COUNTERS.keySet().stream().allMatch(k -> k.equals("H2")), "the counter frames use only H2: " + APPLIED_COUNTERS);
    }

    // ---- what the real pages serve ----

    private static final Pattern ITEM = Pattern.compile("<li (aria-current=\"step\" )?class=\"ash-stepper-item ash-stepper-item--(completed|current|upcoming)\">(.*?)</li>", Pattern.DOTALL);
    private static final Pattern LEGACY_UTILITY = Pattern.compile("class=\"[^\"]*(?<![\\w-])(group|relative|shrink-0|h-8|w-8|h-4|w-4|h-0\\.5|rounded-full|border-2|top-4|sr-only|min-w-0|break-words|flex-col|@container|@sm:[a-z:-]+)(?![\\w-])");

    @Test
    void everyStepperTheRealPagesServeHasTheApprovedShapeStateAndDecorativeSemanticsAndNoTailwindOfItsOwn() throws Exception {
        ParitySession session = ParitySession.open("stepper-parity");
        HttpClient client = HttpClient.newHttpClient();
        List<String> paths = new ArrayList<>(List.of("/ashlar/", "/ashlar/components"));
        for (ComponentPage page : ComponentCatalog.PAGES) {
            paths.add("/ashlar/components/" + page.slug());
        }
        int steppers = 0;
        int items = 0;
        int current = 0;
        int completed = 0;
        List<String> problems = new ArrayList<>();
        try (RunningShowcase app = RunningShowcase.start(session.bootJar())) {
            for (String path : paths) {
                HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + path)).GET().build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                assertEquals(200, response.statusCode(), path);
                String body = response.body();
                int lists = count(body, "<div class=\"ash-stepper\">");
                assertEquals(lists, count(body, "<ol data-stepper class=\"ash-stepper__list\">"), path + ": every wrapper holds the ordered list with its hook");
                assertEquals(StepperLegacyMarkup.count(body), lists, path + ": every ash-stepper class must be on a Stepper's own wrapper");
                assertEquals(0, count(body, "<div class=\"@container\"") + count(body, "<ol data-stepper class=\"flex"), path + ": no page still serves the old classes");
                steppers += lists;
                Matcher m = ITEM.matcher(body);
                while (m.find()) {
                    items++;
                    boolean hasCurrent = m.group(1) != null;
                    String state = m.group(2);
                    String entry = m.group(3);
                    String shown = entry.strip().substring(0, Math.min(90, entry.strip().length()));
                    current += state.equals("current") ? 1 : 0;
                    completed += state.equals("completed") ? 1 : 0;
                    if (hasCurrent != state.equals("current")) {
                        problems.add(path + ": aria-current=\"step\" must be on the current step and only there (" + state + ")");
                    }
                    int check = count(entry, "class=\"ash-icon ash-icon--sm\"");
                    if (check != (state.equals("completed") ? 1 : 0)) {
                        problems.add(path + ": the check Icon must appear exactly on completed steps (" + state + ", " + check + ")");
                    }
                    // The connector row and the indicator are decorative; so is the Icon inside a completed indicator.
                    if (count(entry, "aria-hidden=\"true\"") != 2 + check) {
                        problems.add(path + ": only the connector, the indicator and its check are hidden: " + shown);
                    }
                    if (count(entry, "class=\"ash-stepper-item__connector\" aria-hidden=\"true\"") != 1 || count(entry, "class=\"ash-stepper-item__line\"") != 1
                            || count(entry, "<span aria-hidden=\"true\" class=\"ash-stepper-item__indicator\">") != 1 || count(entry, "class=\"ash-stepper-item__text\"") != 1
                            || count(entry, "class=\"ash-stepper-item__label\"") != 1) {
                        problems.add(path + ": one connector, line, indicator, text column and label: " + shown);
                    }
                    String status = state.equals("completed") ? "Completed" : state.equals("current") ? "Current step" : "Not started";
                    if (count(entry, "<span class=\"ash-sr-only\">, " + status + "</span>") != 1) {
                        problems.add(path + ": the status text for " + state + " must follow the label: " + shown);
                    }
                    if (LEGACY_UTILITY.matcher(entry).find()) {
                        problems.add(path + ": a Stepper Item still carries a Tailwind utility of its own: " + shown);
                    }
                }
            }
        }
        assertEquals(List.of(), problems);
        assertEquals(steppers, current, "every Stepper on the showcase has exactly one current step");
        assertTrue(steppers >= 7 && items >= 24 && completed >= 8, "steppers=" + steppers + " items=" + items + " completed=" + completed + " current=" + current);
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }
}
