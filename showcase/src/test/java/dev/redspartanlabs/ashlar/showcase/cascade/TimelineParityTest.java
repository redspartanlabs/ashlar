package dev.redspartanlabs.ashlar.showcase.cascade;

import dev.redspartanlabs.ashlar.icon.Icon;
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
import java.util.Set;
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
 * The Timeline's and the Timeline Item's parity gates, measured in real Chrome, on the shared parity harness (unchanged since the Icon
 * migration).
 *
 * <p>Evidence is a controlled oracle: the legacy Tailwind markup (compiled by the real Tailwind CLI) next to the migrated markup, in the
 * same browser, in four widths (down to one narrow enough to force the title to wrap) for each of the 22 theme and skin configurations,
 * with Tailwind present and with Tailwind removed. The cases are what a timeline is about: one entry, two, three (first, middle, last),
 * entries with only a title, all five marker variants with and without an Icon, long wrapping and unbroken text. Every part of every entry
 * is measured: the list, the entry, the rail column, the marker and its Icon, the connector, the body, the date, the title, the status text
 * and the content wrapper. The geometric invariant the connector exists for, that it ends exactly where the next entry starts, is asserted
 * on the measured boxes; the last entry has neither connector nor gap. Negative controls show each gate can fail.
 *
 * <p>One behavior deliberately differs from the legacy rendering and is therefore NOT compared with it: a Timeline nested inside an entry's
 * content. The legacy connector was hidden by {@code group-last:hidden}, which matched any {@code .group:last-child} ancestor, so every
 * connector of a Timeline nested inside the last entry vanished. The entry now hides only its own connector. The nested cases are measured
 * on their own, next to the legacy rendering that shows the old leak, and a control shows the old over-reach would be caught.
 *
 * <p>Evidence is Chrome only. Computed-style and box parity is the visual evidence; there is no screenshot comparison.
 */
class TimelineParityTest {

    private static final String ORACLE = ParityFrames.generated("timeline-oracle.css");
    private static final String SCOPED = ParityFrames.generated("timeline-legacy-scoped.css");
    private static final List<ParityConfigs.Config> CONFIGS = ParityConfigs.all();
    private static final List<String> EXTRA_PROPS = List.of("z-index", "overflow-wrap", "column-gap", "row-gap", "align-items", "justify-content", "flex-direction",
            "list-style-type", "list-style-position", "text-decoration-line", "line-height", "font-size", "font-weight");

    /** The kinds of measured element that are the caller's own content, not the component's: preflight reached them too, so a full-legacy comparison leaves them out. */
    private static final Set<String> CALLER_KINDS = Set.of("content-child");

    private static List<ParityProbe.Row> owned(List<ParityProbe.Row> rows) {
        return rows.stream().filter(r -> !CALLER_KINDS.contains(r.kind())).toList();
    }

    private record Layout(String name, String style) {
    }

    private static final List<Layout> LAYOUTS = List.of(
            new Layout("wide", "box-sizing:border-box;width:360px"),
            new Layout("very-narrow", "box-sizing:border-box;width:140px"),
            new Layout("shrink-to-fit", "box-sizing:border-box;display:flex;flex-direction:column;align-items:flex-start;width:420px"));

    private static final String LONG_TITLE = "Quarterly compliance report for enterprise accounts and every other account that has to wrap";
    private static final String LONG_CONTENT = "Generated automatically on the first business day of each quarter and distributed to every account owner on file, including a full audit trail.";
    private static final String UNBROKEN = "Supercalifragilisticexpialidocious".repeat(4);

    private static final Map<String, List<TimelineFixtures.Entry>> CASES = new LinkedHashMap<>();

    static {
        CASES.put("one", List.of(TimelineFixtures.entry("one-1", "Created", "", null, null, "neutral")));
        CASES.put("two", List.of(
                TimelineFixtures.entry("two-1", "Version 2.4.0", "March 2026", "Adds bulk export.", null, "neutral"),
                TimelineFixtures.entry("two-2", "Version 2.3.0", "January 2026", null, null, "neutral")));
        CASES.put("three", List.of(
                TimelineFixtures.entry("three-1", "Order placed", "Sep 21, 9:02 AM", "Confirmation sent.", Icon.CHECK, "info"),
                TimelineFixtures.entry("three-2", "Shipment delayed", "", null, Icon.TAG, "warning"),
                TimelineFixtures.entry("three-3", "Out for delivery", "Sep 22", "Arrives today.", Icon.CHECK, "success")));
        CASES.put("title-only", List.of(
                TimelineFixtures.entry("title-only-1", "Request submitted", "", null, null, "neutral"),
                TimelineFixtures.entry("title-only-2", "Access granted", "", null, null, "neutral")));
        CASES.put("variants", List.of(
                TimelineFixtures.entry("variants-1", "Neutral entry", "Day 1", null, Icon.TAG, "neutral"),
                TimelineFixtures.entry("variants-2", "Info entry", "Day 2", "Some detail.", null, "info"),
                TimelineFixtures.entry("variants-3", "Success entry", "Day 3", null, Icon.CHECK, "success"),
                TimelineFixtures.entry("variants-4", "Warning entry", "Day 4", "Some detail.", Icon.TAG, "warning"),
                TimelineFixtures.entry("variants-5", "Danger entry", "Day 5", null, null, "danger")));
        CASES.put("long", List.of(
                TimelineFixtures.entry("long-1", LONG_TITLE, "Q3 2026", LONG_CONTENT, Icon.USER, "info"),
                TimelineFixtures.entry("long-2", UNBROKEN, "", UNBROKEN, null, "warning"),
                TimelineFixtures.entry("long-3", "Previous report", "Q2 2026", null, null, "neutral")));
    }

    // ---- state of the measurement ----

    private static TimelineLegacyMarkup legacy;
    private static TimelineFixtures fixtures;
    private static ParityProbe.Probe probe;
    private static final Map<String, List<ParityProbe.Finding>> PRESENT = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> REMOVED_SCOPED = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> REMOVED_FULL = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> SCOPED_FIDELITY = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> CONTROLS = new LinkedHashMap<>();
    private static final Map<String, Integer> APPLIED_PRESENT = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_REMOVED = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_SCOPED = new TreeMap<>();
    private static final Map<String, Integer> ROWS = new LinkedHashMap<>();

    // ---- markup ----

    /** The control experiments need far fewer rows than the full matrix: two widths and the four cases that exercise every part. */
    private static final List<Layout> CONTROL_LAYOUTS = List.of(LAYOUTS.get(0), LAYOUTS.get(1));
    private static final Set<String> CONTROL_CASES = Set.of("one", "three", "variants", "long");

    private static String markup(boolean legacyClasses) {
        return markup(legacyClasses, LAYOUTS, CASES.keySet());
    }

    private static String markup(boolean legacyClasses, List<Layout> layouts, Set<String> caseNames) {
        StringBuilder sb = new StringBuilder();
        for (Layout layout : layouts) {
            for (Map.Entry<String, List<TimelineFixtures.Entry>> c : CASES.entrySet()) {
                if (!caseNames.contains(c.getKey())) {
                    continue;
                }
                StringBuilder items = new StringBuilder();
                for (TimelineFixtures.Entry e : c.getValue()) {
                    items.append(fixtures.item(e, legacyClasses, true));
                }
                sb.append("<div style=\"margin:4px 0\"><div data-layout=\"").append(layout.name()).append("\" style=\"").append(layout.style()).append("\">")
                        .append(fixtures.timeline(c.getKey(), items.toString(), legacyClasses, true, "timeline")).append("</div></div>");
            }
        }
        return sb.toString();
    }

    /**
     * Two outer entries; each holds a nested Timeline of two entries in its content. The second outer entry is the last, so in the legacy
     * rendering its nested connectors are hidden by the group-last rule, and in the migrated rendering they are not.
     */
    private static String nestedMarkup(boolean legacyClasses) {
        TimelineFixtures.Entry a1 = TimelineFixtures.entry("nest-a1", "Inner A one", "", null, null, "neutral");
        TimelineFixtures.Entry a2 = TimelineFixtures.entry("nest-a2", "Inner A two", "", null, null, "neutral");
        TimelineFixtures.Entry b1 = TimelineFixtures.entry("nest-b1", "Inner B one", "", null, null, "neutral");
        TimelineFixtures.Entry b2 = TimelineFixtures.entry("nest-b2", "Inner B two", "", null, null, "neutral");
        String innerA = fixtures.timeline("nest-a", fixtures.item(a1, legacyClasses, true) + fixtures.item(a2, legacyClasses, true), legacyClasses, true, "timeline");
        String innerB = fixtures.timeline("nest-b", fixtures.item(b1, legacyClasses, true) + fixtures.item(b2, legacyClasses, true), legacyClasses, true, "timeline");
        TimelineFixtures.Entry o1 = TimelineFixtures.entry("nest-o1", "Outer one", "", null, null, "neutral");
        TimelineFixtures.Entry o2 = TimelineFixtures.entry("nest-o2", "Outer two", "", null, null, "neutral");
        String outer = fixtures.timeline("nest", fixtures.item(o1, legacyClasses, true, innerA) + fixtures.item(o2, legacyClasses, true, innerB), legacyClasses, true, "timeline");
        return "<div data-layout=\"nested\" style=\"box-sizing:border-box;width:360px\">" + outer + "</div>";
    }

    // ---- control experiments: each breaks the migrated Timeline in ONE way and must be detected ----

    private record Control(String name, String config, String css, String expectedProperty) {
    }

    private static final List<Control> CONTROL_LIST = List.of(
            new Control("connector stops at the entry instead of bridging the gap", "light/none", ".ash-timeline-item__connector{bottom:0}", "@h"),
            new Control("connector starts at the wrong top", "light/none", ".ash-timeline-item__connector{top:0}", "@y"),
            new Control("connector is the wrong width", "light/none", ".ash-timeline-item__connector{width:0.25rem}", "width"),
            new Control("connector is off centre", "light/none", ".ash-timeline-item__rail{align-items:flex-start}", "@x"),
            new Control("wrong rail colour", "light/none", ".ash-timeline-item{--ash-timeline-item-rail:red}", "background-color"),
            new Control("dark rail ignores the theme", "dark/none", ".ash-timeline-item{--ash-timeline-item-rail:var(--color-slate-200)}", "background-color"),
            new Control("the last entry keeps its connector", "light/none", ".ash-timeline-item:last-child > .ash-timeline-item__rail > .ash-timeline-item__connector{display:block}", "display"),
            new Control("the last entry keeps its gap", "light/none", ".ash-timeline-item:last-child{padding-bottom:var(--ash-timeline-item-gap)}", "padding-bottom"),
            new Control("the gap is the wrong size", "light/none", ".ash-timeline-item{--ash-timeline-item-gap:1rem}", "padding-bottom"),
            new Control("the marker sits under the rail", "light/none", ".ash-timeline-item__marker{z-index:auto}", "z-index"),
            new Control("marker radius follows the skin", "light/axiom", ".ash-timeline-item__marker{border-radius:var(--ash-radius-lg)}", "border-top-left-radius"),
            new Control("marker border is three pixels", "light/none", ".ash-timeline-item__marker{border-width:3px}", "border-top-width"),
            new Control("marker is the wrong size", "light/none", ".ash-timeline-item__marker{width:1.5rem}", "width"),
            new Control("dark marker fill ignores the theme", "dark/none", ".ash-timeline-item__marker--success{--ash-timeline-marker-bg:var(--color-green-50)}", "background-color"),
            new Control("dark marker icon colour ignores the theme", "dark/none", ".ash-timeline-item__marker--danger{--ash-timeline-marker-fg:var(--color-red-600)}", "color"),
            new Control("wrong marker border colour", "light/none", ".ash-timeline-item__marker--info{--ash-timeline-marker-border:red}", "border-top-color"),
            new Control("marker colour ignores the skin", "light/civic", ".ash-timeline-item__marker--neutral{--ash-timeline-marker-bg:#fff}", "background-color"),
            new Control("wrong gap between marker and body", "light/none", ".ash-timeline-item{gap:8px}", "column-gap"),
            new Control("body can no longer shrink", "light/none", ".ash-timeline-item__body{min-width:auto}", "min-width"),
            new Control("long text no longer wraps at any character", "light/none", ".ash-timeline-item__title{overflow-wrap:normal}", "overflow-wrap"),
            new Control("content no longer wraps at any character", "light/none", ".ash-timeline-item__content{overflow-wrap:normal}", "overflow-wrap"),
            new Control("wrong body padding", "light/none", ".ash-timeline-item__body{padding-block:0}", "padding-top"),
            new Control("title loses its top margin", "light/none", ".ash-timeline-item__title{margin-top:0}", "margin-top"),
            new Control("content loses its top margin", "light/none", ".ash-timeline-item__content{margin-top:0}", "margin-top"),
            new Control("title is not semibold", "light/none", ".ash-timeline-item__title{font-weight:500}", "font-weight"),
            new Control("wrong title colour", "light/none", ".ash-timeline-item__title{color:red}", "color"),
            new Control("dark title ignores the theme", "dark/none", ".ash-timeline-item{--ash-timeline-item-title:var(--color-slate-900)}", "color"),
            new Control("title colour ignores the skin", "light/civic", ".ash-timeline-item{--ash-timeline-item-title:#0f172a}", "color"),
            new Control("wrong date colour", "light/none", ".ash-timeline-item__date{color:red}", "color"),
            new Control("wrong date size", "light/none", ".ash-timeline-item__date{font-size:0.875rem}", "font-size"),
            new Control("wrong content colour", "light/none", ".ash-timeline-item__content{color:red}", "color"),
            new Control("wrong content line height", "light/none", ".ash-timeline-item__content{line-height:1.2}", "line-height"),
            new Control("status text is no longer visually hidden", "light/none", ".ash-sr-only{position:static;width:auto;height:auto;margin:0;overflow:visible;clip-path:none}", "position"),
            new Control("icon is the wrong size", "light/none", ".ash-timeline-item__marker .ash-icon{--ash-icon-size:1.25rem}", "width"),
            new Control("native list padding comes back", "light/none", ".ash-timeline{padding-inline-start:40px}", "padding-left"),
            new Control("native list margin comes back", "light/none", ".ash-timeline{margin-block:16px}", "margin-top"),
            new Control("the numerals come back", "light/none", "ol.ash-timeline{list-style:decimal}", "list-style-type"),
            new Control("the list is not a column", "light/none", ".ash-timeline{flex-direction:row}", "flex-direction"));

    /** The nested control: the old over-reach, a descendant instead of the entry's own rail, is applied to the migrated rendering. */
    private static final String NESTED_OVERREACH = ".ash-timeline-item:last-child .ash-timeline-item__connector{display:none}";

    private static final List<ParityConfigs.Config> NESTED_CONFIGS = List.of(ParityConfigs.get("light/none"), ParityConfigs.get("dark/none"));

    // ---- running ----

    @BeforeAll
    static void measure() throws Exception {
        ParitySession session = ParitySession.open("timeline-parity");
        legacy = new TimelineLegacyMarkup();
        fixtures = new TimelineFixtures(legacy, new IconLegacyMarkup());
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
        // The nested cases are not compared with the legacy rendering (that is the corrected behavior); they are measured next to it.
        String nestedMigrated = nestedMarkup(false);
        String nestedLegacy = nestedMarkup(true);
        for (ParityConfigs.Config c : NESTED_CONFIGS) {
            frames.append(ParityFrames.frame(c.id() + "|N", c, ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), "", nestedMigrated));
            frames.append(ParityFrames.frame(c.id() + "|NR", c, ParityFrames.with(ParityFrames.ASHLAR, ParityConfigs.SKIN_LINKS), environment, nestedMigrated));
            frames.append(ParityFrames.frame(c.id() + "|NL", c, ParityFrames.with(ORACLE, ParityConfigs.SKIN_LINKS), "", nestedLegacy));
        }
        frames.append(ParityFrames.frame("nested-overreach|N", ParityConfigs.get("light/none"), ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), NESTED_OVERREACH, nestedMigrated));

        probe = ParityFrames.measure(session, frames.toString(), EXTRA_PROPS, "timeline frames");
        List<String> props = probe.props();
        for (ParityConfigs.Config c : CONFIGS) {
            List<ParityProbe.Row> legacyRows = probe.phase(c.id() + "|L");
            ROWS.put(c.id() + "|L", legacyRows.size());
            PRESENT.put(c.id(), ParityProbe.compare(c.id() + " (Tailwind present)", props, legacyRows, probe.phase(c.id() + "|P"), APPLIED_PRESENT));
            REMOVED_SCOPED.put(c.id(), ParityProbe.compare(c.id() + " (Tailwind removed vs scoped legacy)", props, probe.phase(c.id() + "|S"), probe.phase(c.id() + "|R"), APPLIED_REMOVED));
            REMOVED_FULL.put(c.id(), ParityProbe.compare(c.id() + " (Tailwind removed vs full legacy, the components' own elements)", props, owned(legacyRows), owned(probe.phase(c.id() + "|R")), APPLIED_REMOVED));
            SCOPED_FIDELITY.put(c.id(), ParityProbe.compare(c.id() + " (scoped legacy vs full legacy, the components' own elements)", props, owned(legacyRows), owned(probe.phase(c.id() + "|S")), APPLIED_SCOPED));
        }
        for (Control control : CONTROL_LIST) {
            CONTROLS.put(control.name(), ParityProbe.compare("control: " + control.name(), props, probe.phase(control.config() + "|LC"),
                    probe.phase("ctl:" + control.name() + "|P"), new TreeMap<>()));
        }
    }

    /** The measurements are tens of thousands of rows; every suite in this JVM is run one after another, so this one lets go of them when it is done. */
    @AfterAll
    static void clear() {
        probe = null;
        List.of(PRESENT, REMOVED_SCOPED, REMOVED_FULL, SCOPED_FIDELITY, CONTROLS).forEach(Map::clear);
        List.of(APPLIED_PRESENT, APPLIED_REMOVED, APPLIED_SCOPED, ROWS).forEach(Map::clear);
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
        String value = text(phase, kind, name, layout, property);
        return Double.parseDouble(value.replace("px", ""));
    }

    private static final List<String> OWN_PHASES = List.of("light/none|L", "light/none|P", "light/none|R", "dark/none|L", "dark/none|P", "dark/none|R");

    // ---- tests ----

    @Test
    void theConfigurationsAreTheTwentyTwoThemeAndSkinCombinations() {
        assertEquals(22, CONFIGS.size());
    }

    @Test
    void controlledOracleWithTailwindPresentMatchesTheLegacyTimelineInEveryConfigurationCaseAndWidth() {
        assertEquals(List.of(), lines(PRESENT));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheLegacyTimelineGivenTheSameSurroundings() {
        assertEquals(List.of(), lines(REMOVED_SCOPED));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheFullLegacyRenderingForTheComponentsOwnElements() {
        assertEquals(List.of(), lines(REMOVED_FULL));
    }

    @Test
    void theScopedLegacyStylesheetRendersTheLegacyTimelineExactlyAsTheFullOneDoes() {
        assertEquals(List.of(), lines(SCOPED_FIDELITY));
    }

    @Test
    void everyNegativeControlIsDetectedInTheProperty() {
        for (Control control : CONTROL_LIST) {
            List<ParityProbe.Finding> findings = CONTROLS.get(control.name());
            assertFalse(findings.isEmpty(), "the control was NOT detected: " + control.name());
            assertTrue(findings.stream().anyMatch(f -> f.property().equals(control.expectedProperty())),
                    control.name() + " should be detected in " + control.expectedProperty() + " but was detected in "
                            + findings.stream().map(ParityProbe.Finding::property).collect(Collectors.toCollection(TreeSet::new)));
        }
    }

    @Test
    void theCasesAreCompleteEveryPartIsMeasuredAndTheNarrowWidthsReallyWrapTheTitle() {
        Map<String, Integer> kinds = new TreeMap<>();
        for (ParityProbe.Row r : probe.phase("light/none|L")) {
            kinds.merge(r.kind(), 1, Integer::sum);
        }
        for (String kind : List.of("timeline", "item", "rail", "marker", "glyph", "connector", "body", "date", "title", "status", "content", "content-child")) {
            assertTrue(kinds.getOrDefault(kind, 0) > 0, "no " + kind + " was measured: " + kinds);
        }
        long entries = CASES.values().stream().mapToLong(List::size).sum();
        assertEquals(entries * LAYOUTS.size(), kinds.get("item").longValue());
        assertEquals((long) CASES.size() * LAYOUTS.size(), kinds.get("timeline").longValue());
        assertTrue(ROWS.values().stream().allMatch(n -> n.equals(ROWS.get("light/none|L"))), "every configuration measures the same rows");
        // A one-line title is 20px tall and the long one wraps onto several lines when narrow, so the wrapping is really exercised.
        double oneLine = num("light/none|L", "title", "title-only-1", "wide", "@h");
        double wide = num("light/none|L", "title", "long-1", "wide", "@h");
        double narrow = num("light/none|L", "title", "long-1", "very-narrow", "@h");
        assertEquals(20.0, oneLine, 0.01);
        assertTrue(narrow > oneLine * 3, "the title wraps onto several lines when narrow: " + narrow);
        assertTrue(wide < narrow);
        // The unbroken words break instead of pushing the entry out of its box, and the body is exactly what is left beside the 2rem rail and the 1rem gap.
        for (String phase : OWN_PHASES) {
            for (Layout layout : LAYOUTS) {
                double timeline = num(phase, "timeline", "long", layout.name(), "@w");
                for (String name : List.of("long-1", "long-2", "long-3")) {
                    double item = num(phase, "item", name, layout.name(), "@w");
                    double body = num(phase, "body", name, layout.name(), "@w");
                    assertTrue(item <= timeline + 0.01, phase + " " + layout.name() + " " + name + ": the entry (" + item + ") stays inside the timeline (" + timeline + ")");
                    assertEquals(item - 48, body, 0.05, phase + " " + layout.name() + " " + name + ": the body is what is left beside the rail and the gap");
                }
            }
        }
    }

    @Test
    void theConnectorEndsExactlyWhereTheNextEntryStartsAndTheLastEntryHasNeitherConnectorNorGap() {
        for (String phase : OWN_PHASES) {
            for (Layout layout : LAYOUTS) {
                for (Map.Entry<String, List<TimelineFixtures.Entry>> c : CASES.entrySet()) {
                    List<TimelineFixtures.Entry> entries = c.getValue();
                    for (int i = 0; i < entries.size(); i++) {
                        String name = entries.get(i).name();
                        String where = phase + " " + layout.name() + " " + name;
                        if (i < entries.size() - 1) {
                            assertNotEquals("none", text(phase, "connector", name, layout.name(), "display"), where + ": a connector");
                            assertEquals("32px", text(phase, "item", name, layout.name(), "padding-bottom"), where + ": the gap");
                            // In the list's coordinates: the entry's top, plus the rail column's top in the entry, plus the connector's top and height in the rail.
                            double bottom = num(phase, "item", name, layout.name(), "@y") + num(phase, "rail", name, layout.name(), "@y")
                                    + num(phase, "connector", name, layout.name(), "@y") + num(phase, "connector", name, layout.name(), "@h");
                            double next = num(phase, "item", entries.get(i + 1).name(), layout.name(), "@y");
                            assertEquals(next, bottom, 0.05, where + ": the connector ends where the next entry starts");
                            assertEquals(32.0, num(phase, "connector", name, layout.name(), "@y"), 0.01, where + ": it starts at the marker's bottom edge");
                            assertEquals(2.0, num(phase, "connector", name, layout.name(), "@w"), 0.01, where + ": two pixels wide");
                            // Centred in the 2rem rail column.
                            assertEquals(15.0, num(phase, "connector", name, layout.name(), "@x"), 0.01, where + ": centred in the rail column");
                        } else {
                            assertEquals("none", text(phase, "connector", name, layout.name(), "display"), where + ": the last entry has no connector");
                            assertEquals("0px", text(phase, "item", name, layout.name(), "padding-bottom"), where + ": the last entry has no gap");
                        }
                    }
                }
            }
        }
    }

    @Test
    void aSingleEntryHasNeitherConnectorNorGap() {
        for (String phase : OWN_PHASES) {
            assertEquals("none", text(phase, "connector", "one-1", "wide", "display"), phase);
            assertEquals("0px", text(phase, "item", "one-1", "wide", "padding-bottom"), phase);
            // The entry is exactly as tall as its body: 4 + 20 (the title) + 4, which is taller than nothing and shorter than the 32px marker, so the marker sets it.
            assertEquals(32.0, num(phase, "item", "one-1", "wide", "@h"), 0.01, phase);
        }
    }

    @Test
    void theMarkerIsAThirtyTwoPixelCircleWithATwoPixelSolidBorderAboveTheRailWhateverTheSkin() {
        for (String phase : OWN_PHASES) {
            ParityProbe.Row marker = one(phase, "marker", "three-1", "wide");
            assertEquals("32px", probe.value(marker, "width"), phase);
            assertEquals("32px", probe.value(marker, "height"), phase);
            for (String side : List.of("top", "right", "bottom", "left")) {
                assertEquals("2px", probe.value(marker, "border-" + side + "-width"), phase);
                assertEquals("solid", probe.value(marker, "border-" + side + "-style"), phase);
            }
            assertEquals("10", probe.value(marker, "z-index"), phase + ": above the connector");
            assertEquals("relative", probe.value(marker, "position"), phase);
            assertEquals("0", probe.value(marker, "flex-shrink"), phase);
            assertEquals("absolute", probe.value(one(phase, "connector", "three-1", "wide"), "position"), phase);
        }
        // Round whatever the skin: the radius is the same in a skin that rounds everything and in one that rounds nothing.
        String plain = text("light/none|P", "marker", "three-1", "wide", "border-top-left-radius");
        for (String skin : List.of("light/axiom", "light/ledger", "light/harbor")) {
            assertEquals(plain, text(skin + "|P", "marker", "three-1", "wide", "border-top-left-radius"), skin);
            assertEquals(text(skin + "|L", "marker", "three-1", "wide", "border-top-left-radius"), text(skin + "|P", "marker", "three-1", "wide", "border-top-left-radius"), skin);
        }
        assertTrue(Double.parseDouble(plain.replace("px", "")) >= 16, "a full circle: the radius is at least half the 32px marker (" + plain + ")");
    }

    @Test
    void theFiveMarkerVariantsAreDistinctAndDifferInTheDarkTheme() {
        List<String> names = List.of("variants-1", "variants-2", "variants-3", "variants-4", "variants-5");
        for (String theme : List.of("light/none|P", "dark/none|P")) {
            List<String> borders = names.stream().map(n -> text(theme, "marker", n, "wide", "border-top-color")).toList();
            assertEquals(5, new TreeSet<>(borders).size(), theme + " borders: " + borders);
            assertTrue(borders.stream().noneMatch(b -> b.equals("rgba(0, 0, 0, 0)")), borders.toString());
        }
        for (String name : names) {
            assertNotEquals(text("light/none|P", "marker", name, "wide", "background-color"), text("dark/none|P", "marker", name, "wide", "background-color"), name + " fill");
            assertNotEquals(text("light/none|P", "marker", name, "wide", "color"), text("dark/none|P", "marker", name, "wide", "color"), name + " icon colour");
            assertEquals(text("light/none|L", "marker", name, "wide", "background-color"), text("light/none|P", "marker", name, "wide", "background-color"), name);
        }
        // Dark mode only changes the neutral border; the four state borders keep the same -500 colour in both themes, as before.
        assertNotEquals(text("light/none|P", "marker", "variants-1", "wide", "border-top-color"), text("dark/none|P", "marker", "variants-1", "wide", "border-top-color"));
        for (String name : names.subList(1, 5)) {
            assertEquals(text("light/none|P", "marker", name, "wide", "border-top-color"), text("dark/none|P", "marker", name, "wide", "border-top-color"), name);
        }
    }

    @Test
    void theStatusTextIsThereForEveryNonNeutralEntryVisuallyHiddenAndAbsentForNeutral() {
        for (String phase : List.of("light/none|L", "light/none|P", "light/none|R")) {
            long statuses = probe.phase(phase).stream().filter(r -> r.kind().equals("status") && r.variant().endsWith("/wide")).count();
            long nonNeutral = CASES.values().stream().flatMap(List::stream).filter(e -> !e.variant().equals("neutral")).count();
            assertEquals(nonNeutral, statuses, phase);
            ParityProbe.Row status = one(phase, "status", "variants-2", "wide");
            assertEquals("absolute", probe.value(status, "position"), phase);
            assertEquals("1px", probe.value(status, "width"), phase);
            assertEquals("1px", probe.value(status, "height"), phase);
        }
    }

    @Test
    void theIconModifierGivesTheExactLegacySizeInsideTheMarker() {
        for (String phase : List.of("light/none|L", "light/none|P", "light/none|R")) {
            ParityProbe.Row glyph = one(phase, "glyph", "three-1", "wide");
            assertEquals("16px", probe.value(glyph, "width"), phase);
            assertEquals("16px", probe.value(glyph, "height"), phase);
        }
    }

    @Test
    void aNestedTimelineKeepsItsOwnConnectorsWhateverTheEntryAroundItIsAndTheLegacyRenderingShowsTheLeakThatWasCorrected() {
        for (ParityConfigs.Config c : NESTED_CONFIGS) {
            for (String suffix : List.of("N", "NR")) {
                String phase = c.id() + "|" + suffix;
                // The outer list: the first entry bridges to the second, the last (the one holding the second nested Timeline) has none of its own.
                assertEquals("block", text(phase, "connector", "nest-o1", "nested", "display"), phase);
                assertEquals("none", text(phase, "connector", "nest-o2", "nested", "display"), phase + ": the last outer entry hides ITS connector");
                // Each nested Timeline: its first entry keeps its connector, its last has none - including the one inside the LAST outer entry.
                assertEquals("block", text(phase, "connector", "nest-a1", "nested", "display"), phase);
                assertEquals("none", text(phase, "connector", "nest-a2", "nested", "display"), phase);
                assertEquals("block", text(phase, "connector", "nest-b1", "nested", "display"), phase + ": a nested connector is not hidden because the outer entry is last");
                assertEquals("none", text(phase, "connector", "nest-b2", "nested", "display"), phase);
                assertEquals("0px", text(phase, "item", "nest-o2", "nested", "padding-bottom"), phase);
                assertEquals("32px", text(phase, "item", "nest-b1", "nested", "padding-bottom"), phase + ": the nested entry keeps its own gap");
                // The nested rail is a real, bridging line.
                double bottom = num(phase, "item", "nest-b1", "nested", "@y") + num(phase, "rail", "nest-b1", "nested", "@y")
                        + num(phase, "connector", "nest-b1", "nested", "@y") + num(phase, "connector", "nest-b1", "nested", "@h");
                assertEquals(num(phase, "item", "nest-b2", "nested", "@y"), bottom, 0.05, phase);
            }
            // The legacy rendering hid every connector inside the last outer entry (its group-last matched the ancestor): that leak is what was corrected.
            String legacyPhase = c.id() + "|NL";
            assertEquals("block", text(legacyPhase, "connector", "nest-a1", "nested", "display"), legacyPhase);
            assertEquals("none", text(legacyPhase, "connector", "nest-b1", "nested", "display"), legacyPhase + ": the old rule reached a nested Timeline's connector");
        }
    }

    @Test
    void theOldOverReachWouldBeCaughtByTheNestedGate() {
        // The descendant form of the last-entry rule hides the nested connector inside the last outer entry again.
        assertEquals("none", text("nested-overreach|N", "connector", "nest-b1", "nested", "display"), "a descendant selector reproduces the leak, and the gate sees it");
        assertEquals("block", text("light/none|N", "connector", "nest-b1", "nested", "display"));
    }

    @Test
    void theOracleIsNotVacuousEveryLegacyClassIsCompiledInTheCumulativeAndScopedStylesheets() throws IOException {
        Path dir = Path.of(System.getProperty("ashlar.cascade.dir"));
        String oracle = Files.readString(dir.resolve("timeline-oracle.css"), StandardCharsets.UTF_8);
        String scoped = Files.readString(dir.resolve("timeline-legacy-scoped.css"), StandardCharsets.UTF_8);
        // "group" is a marker class Tailwind writes no rule for; every other legacy class is compiled (group-last, last and dark are written as pseudo-class or where() selectors).
        assertEquals(List.of("group"), ParityOracle.uncompiled(oracle, legacy.allTokens()), "the Timeline oracle does not compile these");
        assertEquals(List.of("group"), ParityOracle.uncompiled(scoped, legacy.allTokens()), "the scoped stylesheet does not compile these");
        assertTrue(oracle.contains(".group-last\\:hidden:is("), "the group-last rule is what the legacy connector used");
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
        assertEquals(List.of(), ParityOracle.uncompiled(oracle, earlier), "the Timeline oracle lost earlier legacy classes");
        assertFalse(scoped.contains("html,:host"), "the scoped legacy stylesheet must not carry Tailwind's page-wide base");
    }

    @Test
    void normalizationsStayWithinTheApprovedRulesAndTailwindPresentNeedsNone() {
        assertEquals(Map.of(), APPLIED_PRESENT, "Tailwind present needs no normalization at all");
        assertEquals(Map.of(), APPLIED_SCOPED, "the scoped legacy rendering must match the full one without any normalization");
        // Tailwind removed: only H2 (a border style is invisible at width 0; preflight gave solid, the browser gives none). Every legacy border is solid,
        // so each side where the migrated element has no border style at all is exactly one use, in both removed comparisons. The marker's border is real
        // (2px solid) and uses none.
        long none = 0;
        for (ParityConfigs.Config c : CONFIGS) {
            for (ParityProbe.Row legacyRow : owned(probe.phase(c.id() + "|L"))) {
                for (String side : List.of("top", "right", "bottom", "left")) {
                    assertEquals("solid", probe.value(legacyRow, "border-" + side + "-style"), "every legacy border style is solid (preflight)");
                }
            }
            for (ParityProbe.Row row : owned(probe.phase(c.id() + "|R"))) {
                for (String side : List.of("top", "right", "bottom", "left")) {
                    if (probe.value(row, "border-" + side + "-style").equals("none")) {
                        none++;
                    }
                }
            }
        }
        assertTrue(none > 0);
        assertEquals(Map.of("H2", (int) (2 * none)), APPLIED_REMOVED, "exactly H2, one use per borderless side, in both removed comparisons");
    }

    // ---- what the real pages serve ----

    private static final Pattern ITEM = Pattern.compile("<li class=\"ash-timeline-item\">(.*?)</li>", Pattern.DOTALL);
    private static final Pattern LEGACY_UTILITY = Pattern.compile("class=\"[^\"]*(?<![\\w-])(group|relative|shrink-0|h-8|w-8|h-4|w-4|rounded-full|border-2|-bottom-8|top-8|w-0\\.5|sr-only|min-w-0|break-words|flex-col)(?![\\w-])");

    @Test
    void everyTimelineTheRealPagesServeHasTheApprovedShapeDecorativeSemanticsAndNoTailwindOfItsOwn() throws Exception {
        ParitySession session = ParitySession.open("timeline-parity");
        HttpClient client = HttpClient.newHttpClient();
        List<String> paths = new ArrayList<>(List.of("/ashlar/", "/ashlar/components"));
        for (ComponentPage page : ComponentCatalog.PAGES) {
            paths.add("/ashlar/components/" + page.slug());
        }
        int timelines = 0;
        int items = 0;
        int statuses = 0;
        int icons = 0;
        List<String> problems = new ArrayList<>();
        try (RunningShowcase app = RunningShowcase.start(session.bootJar())) {
            for (String path : paths) {
                HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + path)).GET().build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                assertEquals(200, response.statusCode(), path);
                String body = response.body();
                int lists = count(body, "<ol data-timeline class=\"ash-timeline\"");
                assertEquals(TimelineLegacyMarkup.count(body), lists, path + ": every ash-timeline class must be on the Timeline's own <ol>");
                assertEquals(0, count(body, "<ol data-timeline class=\"flex flex-col\""), path + ": no page still serves the old list classes");
                timelines += lists;
                Matcher m = ITEM.matcher(body);
                while (m.find()) {
                    items++;
                    String entry = m.group(1);
                    String shown = entry.strip().substring(0, Math.min(90, entry.strip().length()));
                    Matcher marker = Pattern.compile("<span aria-hidden=\"true\" class=\"ash-timeline-item__marker ash-timeline-item__marker--(neutral|info|success|warning|danger)\">").matcher(entry);
                    if (!entry.strip().startsWith("<div class=\"ash-timeline-item__rail\">") || !marker.find()) {
                        problems.add(path + ": an entry without the approved rail and marker: " + shown);
                        continue;
                    }
                    String variant = marker.group(1);
                    int status = count(entry, "<span class=\"ash-sr-only\">");
                    statuses += status;
                    if (status != (variant.equals("neutral") ? 0 : 1)) {
                        problems.add(path + ": the status text must appear exactly on non-neutral entries (" + variant + ", " + status + ")");
                    }
                    int glyphs = count(entry, "class=\"ash-icon ash-icon--sm\"");
                    icons += glyphs;
                    if (count(entry, "class=\"ash-timeline-item__connector\"") != 1 || count(entry, "class=\"ash-timeline-item__body\"") != 1
                            || count(entry, "class=\"ash-timeline-item__title\"") != 1 || count(entry, "class=\"ash-timeline-item__date\"") > 1
                            || count(entry, "class=\"ash-timeline-item__content\"") > 1 || glyphs > 1) {
                        problems.add(path + ": one connector, one body, one title, at most one date, content and Icon: " + shown);
                    }
                    // The marker and its Icon are decorative: the marker is hidden and so is the Icon inside it.
                    if (count(entry, "aria-hidden=\"true\"") != 1 + glyphs) {
                        problems.add(path + ": only the marker and its Icon are hidden: " + shown);
                    }
                    if (LEGACY_UTILITY.matcher(entry).find()) {
                        problems.add(path + ": a Timeline Item still carries a Tailwind utility of its own: " + shown);
                    }
                }
            }
        }
        assertEquals(List.of(), problems);
        assertTrue(timelines >= 6 && items >= 17 && statuses >= 4 && icons >= 5, "timelines=" + timelines + " items=" + items + " statuses=" + statuses + " icons=" + icons);
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }
}
