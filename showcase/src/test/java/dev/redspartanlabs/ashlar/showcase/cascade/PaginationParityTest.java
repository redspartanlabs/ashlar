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
 * The Pagination's and the Pagination Item's parity gates, measured in real Chrome, on the shared parity harness (unchanged).
 *
 * <p>Evidence is a controlled oracle: the legacy Tailwind markup (compiled by the real Tailwind CLI) next to the migrated markup, in the
 * same browser, in three bar widths for each of the 22 theme and skin configurations, with Tailwind present and with Tailwind removed.
 * The cases are a lone current page, a basic bar, a bar on its first page (a disabled Previous), a larger range with an ellipsis, a
 * mixed bar (a current entry that is also given an {@code href}, which must not become a link, an empty {@code href}, a missing one, and
 * a long label), and an unbroken word. Every part of every entry is measured: the landmark, the list, the {@code <li>}, and the link, the
 * current page or the disabled entry. The narrow widths make the bar wrap.
 *
 * <p>A hover cannot be held in a measurement, so the text colour and the fill a link takes on hover are compared through swatches painted
 * with those colours, in both themes; the keyboard focus ring IS measured, one frame per Chrome run because only one frame holds focus.
 *
 * <p>Evidence is Chrome only. Computed-style and box parity is the visual evidence; there is no screenshot comparison.
 */
class PaginationParityTest {

    private static final String ORACLE = ParityFrames.generated("pagination-oracle.css");
    private static final String SCOPED = ParityFrames.generated("pagination-legacy-scoped.css");
    private static final List<ParityConfigs.Config> CONFIGS = ParityConfigs.all();
    private static final List<String> EXTRA_PROPS = List.of("flex-wrap", "flex-direction", "column-gap", "row-gap", "align-items", "justify-content", "list-style-type",
            "list-style-position", "line-height", "font-size", "font-weight", "overflow-wrap", "text-decoration-line", "text-decoration-style",
            "text-underline-offset", "cursor", "z-index");

    private record Layout(String name, String style) {
    }

    private static final List<Layout> LAYOUTS = List.of(
            new Layout("wide", "box-sizing:border-box;width:600px"),
            new Layout("narrow", "box-sizing:border-box;width:240px"),
            new Layout("tiny", "box-sizing:border-box;width:120px"));

    private static final String LONG = "Quarterly Enterprise Revenue Summary by Region";
    private static final String UNBROKEN = "Supercalifragilisticexpialidocious".repeat(2);

    private static final Map<String, List<PaginationFixtures.Entry>> CASES = new LinkedHashMap<>();

    static {
        CASES.put("single", List.of(PaginationFixtures.entry("single-1", "1", null, true)));
        CASES.put("basic", List.of(
                PaginationFixtures.entry("basic-1", "1", "?page=1", false),
                PaginationFixtures.entry("basic-2", "2", null, true),
                PaginationFixtures.entry("basic-3", "3", "?page=3", false)));
        CASES.put("first", List.of(
                PaginationFixtures.entry("first-1", "Previous", null, false),
                PaginationFixtures.entry("first-2", "1", null, true),
                PaginationFixtures.entry("first-3", "2", "?page=2", false),
                PaginationFixtures.entry("first-4", "3", "?page=3", false),
                PaginationFixtures.entry("first-5", "Next", "?page=2", false)));
        CASES.put("range", List.of(
                PaginationFixtures.entry("range-1", "Previous", "?page=3", false),
                PaginationFixtures.entry("range-2", "1", "?page=1", false),
                PaginationFixtures.entry("range-3", "2", "?page=2", false),
                PaginationFixtures.entry("range-4", "3", "?page=3", false),
                PaginationFixtures.entry("range-5", "4", null, true),
                PaginationFixtures.entry("range-6", "5", "?page=5", false),
                PaginationFixtures.entry("range-7", "…", null, false),
                PaginationFixtures.entry("range-8", "10", "?page=10", false),
                PaginationFixtures.entry("range-9", "Next", "?page=5", false)));
        CASES.put("mixed", List.of(
                PaginationFixtures.entry("mixed-1", "1", "/1", false),
                PaginationFixtures.entry("mixed-2", "Plain", null, false),
                PaginationFixtures.entry("mixed-3", "Empty", "", false),
                PaginationFixtures.entry("mixed-4", "Wins", "/wins", true),
                PaginationFixtures.entry("mixed-5", "Last", "/last", false),
                PaginationFixtures.entry("mixed-6", "100", "/100", false)));
        CASES.put("long", List.of(
                PaginationFixtures.entry("long-1", "1", "/1", false),
                PaginationFixtures.entry("long-2", UNBROKEN, "/reports", false),
                PaginationFixtures.entry("long-3", LONG, null, true),
                PaginationFixtures.entry("long-4", LONG, "/long", false),
                PaginationFixtures.entry("long-5", LONG, null, false)));
    }

    private static final List<Layout> CONTROL_LAYOUTS = List.of(LAYOUTS.get(0), LAYOUTS.get(2));
    private static final Set<String> CONTROL_CASES = Set.of("range", "mixed", "long");

    private static final List<ParityConfigs.Config> FOCUS_CONFIGS = List.of(ParityConfigs.get("light/none"), ParityConfigs.get("dark/none"));
    private static final String FOCUS_CONTROL_RING = ".ash-pagination-item__control--link:focus-visible{box-shadow:none}";
    private static final String FOCUS_CONTROL_OUTLINE = ".ash-pagination-item__control--link:focus{outline-style:auto}";

    // ---- state of the measurement ----

    private static PaginationLegacyMarkup legacy;
    private static PaginationFixtures fixtures;
    private static ParityProbe.Probe probe;
    private static final Map<String, List<ParityProbe.Finding>> PRESENT = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> REMOVED_SCOPED = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> REMOVED_FULL = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> SCOPED_FIDELITY = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> CONTROLS = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> FOCUS = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> FOCUS_CONTROLS = new LinkedHashMap<>();
    private static final Map<String, ParityProbe.Probe> FOCUS_PROBES = new LinkedHashMap<>();
    private static final Map<String, Integer> APPLIED_PRESENT = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_REMOVED = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_SCOPED = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_FOCUS = new TreeMap<>();
    private static final Map<String, Integer> ROWS = new LinkedHashMap<>();

    // ---- markup ----

    private static String markup(boolean legacyClasses) {
        return markup(legacyClasses, LAYOUTS, CASES.keySet());
    }

    private static String markup(boolean legacyClasses, List<Layout> layouts, Set<String> caseNames) {
        StringBuilder sb = new StringBuilder();
        for (Layout layout : layouts) {
            for (Map.Entry<String, List<PaginationFixtures.Entry>> c : CASES.entrySet()) {
                if (!caseNames.contains(c.getKey())) {
                    continue;
                }
                StringBuilder items = new StringBuilder();
                for (PaginationFixtures.Entry e : c.getValue()) {
                    items.append(fixtures.item(e, legacyClasses, true));
                }
                sb.append("<div style=\"margin:4px 0\"><div data-layout=\"").append(layout.name()).append("\" style=\"").append(layout.style()).append("\">")
                        .append(fixtures.bar(c.getKey(), items.toString(), legacyClasses, true)).append("</div></div>");
            }
        }
        return sb.append("<div data-layout=\"swatch\">").append(fixtures.hoverSwatches(legacyClasses)).append("</div>").toString();
    }

    /** One link, focused by a script as soon as its frame loads, as the only link of its frame, next to a current page and a disabled entry. */
    private static String focusBody(boolean legacyClasses) {
        String home = fixtures.item(PaginationFixtures.entry("focus-1", "1", "/", false), legacyClasses, true).replaceFirst("<a ", "<a data-focus ");
        String current = fixtures.item(PaginationFixtures.entry("focus-2", "2", null, true), legacyClasses, true);
        String disabled = fixtures.item(PaginationFixtures.entry("focus-3", "Next", null, false), legacyClasses, true);
        return fixtures.bar("focus", home + current + disabled, legacyClasses, true) + "<script>document.querySelector('[data-focus]').focus();</script>";
    }

    // ---- control experiments: each breaks the migrated bar in ONE way and must be detected ----

    private record Control(String name, String config, String css, String expectedProperty) {
    }

    private static final List<Control> CONTROL_LIST = List.of(
            new Control("list no longer wraps", "light/none", ".ash-pagination__list{flex-wrap:nowrap}", "flex-wrap"),
            new Control("wrong gap along the line", "light/none", ".ash-pagination__list{column-gap:0.5rem}", "column-gap"),
            new Control("wrong gap between wrapped lines", "light/none", ".ash-pagination__list{row-gap:0.5rem}", "row-gap"),
            new Control("list is not centred", "light/none", ".ash-pagination__list{align-items:flex-start}", "align-items"),
            new Control("list text is the wrong size", "light/none", ".ash-pagination__list{font-size:1rem}", "font-size"),
            new Control("list line height is wrong", "light/none", ".ash-pagination__list{--ash-pagination-leading:1.75}", "line-height"),
            new Control("native list padding comes back", "light/none", ".ash-pagination__list{padding-inline-start:40px}", "padding-left"),
            new Control("native list margin comes back", "light/none", ".ash-pagination__list{margin-block:16px}", "margin-top"),
            new Control("the bullets come back", "light/none", "ul.ash-pagination__list{list-style:disc}", "list-style-type"),
            new Control("entries are the wrong height", "light/none", ".ash-pagination-item__control{height:2.25rem}", "height"),
            new Control("entries have the wrong minimum width", "light/none", ".ash-pagination-item__control{min-width:2.25rem}", "min-width"),
            new Control("entries have the wrong padding", "light/none", ".ash-pagination-item__control{padding-left:0.75rem;padding-right:0.75rem}", "padding-left"),
            new Control("entries do not centre their label", "light/none", ".ash-pagination-item__control{justify-content:flex-start}", "justify-content"),
            new Control("entries are blocks", "light/none", ".ash-pagination-item__control{display:inline-block}", "display"),
            new Control("entries are not medium weight", "light/none", ".ash-pagination-item__control{font-weight:400}", "font-weight"),
            new Control("link border is the wrong width", "light/none", ".ash-pagination-item__control--link{border-width:2px}", "border-top-width"),
            new Control("link border is the wrong colour", "light/none", ".ash-pagination-item{--ash-pagination-item-border:red}", "border-top-color"),
            new Control("dark link border ignores the theme", "dark/none", ".ash-pagination-item{--ash-pagination-item-border:var(--color-slate-200)}", "border-top-color"),
            new Control("wrong link colour", "light/none", ".ash-pagination-item{--ash-pagination-item-link:red}", "color"),
            new Control("dark link ignores the theme", "dark/none", ".ash-pagination-item{--ash-pagination-item-link:var(--color-slate-600)}", "color"),
            new Control("link is underlined", "light/none", ".ash-pagination-item__control--link{text-decoration:underline}", "text-decoration-line"),
            new Control("link radius ignores the skin", "light/axiom", ".ash-pagination-item__control--link{border-radius:0.375rem}", "border-top-left-radius"),
            new Control("link is not rounded", "light/none", ".ash-pagination-item__control--link{border-radius:0}", "border-top-left-radius"),
            new Control("current page is not rounded", "light/none", ".ash-pagination-item__control--current{border-radius:0}", "border-top-left-radius"),
            new Control("current page radius ignores the skin", "light/axiom", ".ash-pagination-item__control--current{border-radius:0.375rem}", "border-top-left-radius"),
            new Control("wrong current fill", "light/none", ".ash-pagination-item{--ash-pagination-item-current:red}", "background-color"),
            new Control("dark current ignores the theme", "dark/none", ".ash-pagination-item{--ash-pagination-item-current:var(--color-blue-600)}", "background-color"),
            new Control("wrong current text colour", "light/none", ".ash-pagination-item{--ash-pagination-item-current-text:black}", "color"),
            new Control("current page has no border", "light/none", ".ash-pagination-item__control--current{border-width:0}", "border-top-width"),
            new Control("wrong disabled colour", "light/none", ".ash-pagination-item{--ash-pagination-item-disabled:red}", "color"),
            new Control("dark disabled ignores the theme", "dark/none", ".ash-pagination-item{--ash-pagination-item-disabled:var(--color-slate-300)}", "color"),
            new Control("disabled entry gets a border", "light/none", ".ash-pagination-item__control--disabled{border:1px solid red}", "border-top-width"),
            new Control("disabled entry looks clickable", "light/none", ".ash-pagination-item__control--disabled{cursor:pointer}", "cursor"),
            new Control("colour ignores the skin", "light/axiom", ".ash-pagination-item{--ash-pagination-item-link:#475569}", "color"),
            new Control("wrong hover text colour", "light/none", ".ash-pagination-item{--ash-pagination-item-link-hover:red}", "background-color"),
            new Control("wrong hover fill", "light/none", ".ash-pagination-item{--ash-pagination-item-link-hover-bg:red}", "background-color"),
            new Control("dark hover ignores the theme", "dark/none", ".ash-pagination-item{--ash-pagination-item-link-hover-bg:var(--color-slate-50)}", "background-color"));

    // ---- running ----

    @BeforeAll
    static void measure() throws Exception {
        ParitySession session = ParitySession.open("pagination-parity");
        legacy = new PaginationLegacyMarkup();
        fixtures = new PaginationFixtures(legacy);
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

        probe = ParityFrames.measure(session, frames.toString(), EXTRA_PROPS, "pagination frames");
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

        // The keyboard focus ring: one frame per Chrome run, because only one frame holds focus.
        for (ParityConfigs.Config c : FOCUS_CONFIGS) {
            ParityProbe.Probe l = focusRun(session, c, "L", ParityFrames.with(ORACLE, ParityConfigs.SKIN_LINKS), "", true);
            ParityProbe.Probe p = focusRun(session, c, "P", ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), "", false);
            ParityProbe.Probe s = focusRun(session, c, "S", ParityFrames.with(SCOPED, ParityConfigs.SKIN_LINKS), environment, true);
            ParityProbe.Probe r = focusRun(session, c, "R", ParityFrames.with(ParityFrames.ASHLAR, ParityConfigs.SKIN_LINKS), environment, false);
            FOCUS_PROBES.put(c.id() + "|L", l);
            FOCUS_PROBES.put(c.id() + "|P", p);
            FOCUS_PROBES.put(c.id() + "|R", r);
            FOCUS.put(c.id() + " present", ParityProbe.compare(c.id() + " (focused, Tailwind present)", l.props(), l.rows(), p.rows(), APPLIED_FOCUS));
            FOCUS.put(c.id() + " removed", ParityProbe.compare(c.id() + " (focused, Tailwind removed)", s.props(), s.rows(), r.rows(), APPLIED_FOCUS));
        }
        ParityConfigs.Config light = ParityConfigs.get("light/none");
        ParityProbe.Probe legacyFocused = FOCUS_PROBES.get("light/none|L");
        FOCUS_CONTROLS.put("focus ring dropped", ParityProbe.compare("control: focus ring dropped", legacyFocused.props(), legacyFocused.rows(),
                focusRun(session, light, "C1", ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), FOCUS_CONTROL_RING, false).rows(), new TreeMap<>()));
        FOCUS_CONTROLS.put("browser outline comes back", ParityProbe.compare("control: browser outline comes back", legacyFocused.props(), legacyFocused.rows(),
                focusRun(session, light, "C2", ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), FOCUS_CONTROL_OUTLINE, false).rows(), new TreeMap<>()));
    }

    private static ParityProbe.Probe focusRun(ParitySession session, ParityConfigs.Config config, String id, List<String> sheets, String css, boolean legacyClasses) throws Exception {
        String frame = ParityFrames.frame(config.id() + "|" + id, config, sheets, css, focusBody(legacyClasses));
        return ParityFrames.measure(session, frame, EXTRA_PROPS, "focus " + config.id() + "|" + id);
    }

    /** The measurements are tens of thousands of rows; every suite in this JVM is run one after another, so this one lets go of them when it is done. */
    @AfterAll
    static void clear() {
        probe = null;
        List.of(PRESENT, REMOVED_SCOPED, REMOVED_FULL, SCOPED_FIDELITY, CONTROLS, FOCUS, FOCUS_CONTROLS).forEach(Map::clear);
        FOCUS_PROBES.clear();
        List.of(APPLIED_PRESENT, APPLIED_REMOVED, APPLIED_SCOPED, APPLIED_FOCUS, ROWS).forEach(Map::clear);
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

    private static List<ParityProbe.Row> rows(String phase, String kind, String name, String layout) {
        return probe.phase(phase).stream().filter(r -> r.kind().equals(kind) && r.variant().equals(name + "/" + layout)).toList();
    }

    private static ParityProbe.Row one(String phase, String kind, String name, String layout) {
        List<ParityProbe.Row> found = rows(phase, kind, name, layout);
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

    // ---- tests ----

    @Test
    void theConfigurationsAreTheTwentyTwoThemeAndSkinCombinations() {
        assertEquals(22, CONFIGS.size());
    }

    @Test
    void controlledOracleWithTailwindPresentMatchesTheLegacyPaginationInEveryConfigurationCaseAndWidth() {
        assertEquals(List.of(), lines(PRESENT));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheLegacyPaginationGivenTheSameSurroundings() {
        assertEquals(List.of(), lines(REMOVED_SCOPED));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheFullLegacyRendering() {
        assertEquals(List.of(), lines(REMOVED_FULL));
    }

    @Test
    void theScopedLegacyStylesheetRendersTheLegacyPaginationExactlyAsTheFullOneDoes() {
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
    void theCasesAreCompleteEveryPartIsMeasuredAndTheNarrowWidthsReallyWrapTheBar() {
        Map<String, Integer> kinds = new TreeMap<>();
        for (ParityProbe.Row r : probe.phase("light/none|L")) {
            kinds.merge(r.kind(), 1, Integer::sum);
        }
        for (String kind : List.of("nav", "list", "item", "link", "current", "disabled", "hover-swatch", "hover-fill-swatch")) {
            assertTrue(kinds.getOrDefault(kind, 0) > 0, "no " + kind + " was measured: " + kinds);
        }
        long entries = CASES.values().stream().mapToLong(List::size).sum();
        assertEquals(entries * LAYOUTS.size(), kinds.get("item").longValue());
        assertEquals((long) CASES.size() * LAYOUTS.size(), kinds.get("nav").longValue());
        assertEquals((long) CASES.size() * LAYOUTS.size(), kinds.get("list").longValue());
        for (String kind : List.of("link", "current", "disabled")) {
            long expected = CASES.values().stream().flatMap(List::stream).filter(e -> e.kind().equals(kind)).count();
            assertEquals(expected * LAYOUTS.size(), kinds.get(kind).longValue(), kind);
        }
        assertTrue(ROWS.values().stream().allMatch(n -> n.equals(ROWS.get("light/none|L"))), "every configuration measures the same rows");
        for (String phase : OWN_PHASES) {
            // One entry is 32px tall; a bar that does not fit wraps onto further lines, with a 6px gap between lines.
            assertEquals(32.0, num(phase, "list", "basic", "wide", "@h"), 0.01, phase + ": a short bar is one line");
            assertEquals(32.0, num(phase, "list", "range", "wide", "@h"), 0.01, phase + ": the larger range fits one line at 600px");
            assertTrue(num(phase, "list", "range", "narrow", "@h") >= 70.0, phase + ": the larger range wraps at 240px");
            assertTrue(num(phase, "list", "range", "tiny", "@h") >= 70.0, phase + ": and at 120px");
        }
    }

    @Test
    void theListIsAWrappingCentredRowWithTheNativeListStylingTakenAway() {
        for (String phase : OWN_PHASES) {
            for (Layout layout : LAYOUTS) {
                String where = phase + " " + layout.name();
                assertEquals("flex", text(phase, "list", "basic", layout.name(), "display"), where);
                assertEquals("row", text(phase, "list", "basic", layout.name(), "flex-direction"), where);
                assertEquals("wrap", text(phase, "list", "basic", layout.name(), "flex-wrap"), where);
                assertEquals("center", text(phase, "list", "basic", layout.name(), "align-items"), where);
                assertEquals("6px", text(phase, "list", "basic", layout.name(), "column-gap"), where + ": along the line");
                assertEquals("6px", text(phase, "list", "basic", layout.name(), "row-gap"), where + ": between lines");
                assertEquals("14px", text(phase, "list", "basic", layout.name(), "font-size"), where);
                assertEquals("20px", text(phase, "list", "basic", layout.name(), "line-height"), where);
                assertEquals("none", text(phase, "list", "basic", layout.name(), "list-style-type"), where + ": no bullets");
                for (String side : List.of("padding-left", "margin-top", "margin-left")) {
                    assertEquals("0px", text(phase, "list", "basic", layout.name(), side), where + " " + side);
                }
            }
        }
    }

    @Test
    void everyEntryIsTheSameThirtyTwoPixelTallCentredInlineFlexBoxAtLeastThirtyTwoWideWithMediumText() {
        for (String phase : OWN_PHASES) {
            for (String[] entry : new String[][]{{"link", "basic-1"}, {"current", "basic-2"}, {"disabled", "first-1"}}) {
                ParityProbe.Row row = one(phase, entry[0], entry[1], "wide");
                String where = phase + " " + entry[0];
                assertEquals("inline-flex", probe.value(row, "display"), where);
                assertEquals("center", probe.value(row, "align-items"), where);
                assertEquals("center", probe.value(row, "justify-content"), where);
                assertEquals("32px", probe.value(row, "height"), where);
                assertEquals("32px", probe.value(row, "min-width"), where);
                assertEquals("8px", probe.value(row, "padding-left"), where);
                assertEquals("8px", probe.value(row, "padding-right"), where);
                assertEquals("500", probe.value(row, "font-weight"), where);
            }
            // A one-character label is exactly the minimum width; a longer label is wider.
            assertEquals(32.0, num(phase, "link", "basic-1", "wide", "@w"), 0.01, phase + ": 1 is the minimum width");
            assertEquals(32.0, num(phase, "current", "basic-2", "wide", "@w"), 0.01, phase);
            assertTrue(num(phase, "disabled", "first-1", "wide", "@w") > 32.0, phase + ": Previous is wider than the minimum");
            assertTrue(num(phase, "link", "mixed-6", "wide", "@w") > 32.0, phase + ": 100 is wider than the minimum");
        }
    }

    @Test
    void theLinkIsBorderedRoundedWithTheSkinsMediumRadiusNeverUnderlinedAndTheOnlyEntryThatLooksClickable() {
        for (String phase : OWN_PHASES) {
            ParityProbe.Row link = one(phase, "link", "basic-1", "wide");
            assertEquals("none", probe.value(link, "text-decoration-line"), phase);
            assertEquals("pointer", probe.value(link, "cursor"), phase);
            assertEquals("rgba(0, 0, 0, 0)", probe.value(link, "background-color"), phase + ": no fill at rest");
            assertEquals("6px", probe.value(link, "border-top-left-radius"), phase + ": rounded-md");
            for (String side : List.of("top", "right", "bottom", "left")) {
                assertEquals("1px", probe.value(link, "border-" + side + "-width"), phase);
                assertEquals("solid", probe.value(link, "border-" + side + "-style"), phase);
            }
            assertNotEquals("pointer", text(phase, "disabled", "first-1", "wide", "cursor"), phase + ": a disabled entry does not look clickable");
            assertNotEquals("pointer", text(phase, "current", "basic-2", "wide", "cursor"), phase + ": the current page is not a link");
        }
        // A skin that sets --radius-md changes the radius, exactly as Tailwind's rounded-md did.
        for (String suffix : List.of("P", "L", "R")) {
            assertEquals("4px", text("light/axiom|" + suffix, "link", "basic-1", "wide", "border-top-left-radius"), suffix);
            assertEquals("4px", text("light/axiom|" + suffix, "current", "basic-2", "wide", "border-top-left-radius"), suffix);
        }
    }

    @Test
    void theCurrentPageIsWhiteOnBlueWithAMatchingBorderAndTheDisabledEntryHasNoBorderRadiusOrFill() {
        for (String phase : OWN_PHASES) {
            ParityProbe.Row current = one(phase, "current", "basic-2", "wide");
            assertEquals("rgb(255, 255, 255)", probe.value(current, "color"), phase + ": white in both themes");
            assertEquals(probe.value(current, "background-color"), probe.value(current, "border-top-color"), phase + ": the border is the fill's colour");
            assertEquals("1px", probe.value(current, "border-top-width"), phase);
            assertEquals("6px", probe.value(current, "border-top-left-radius"), phase);
            ParityProbe.Row disabled = one(phase, "disabled", "first-1", "wide");
            assertEquals("0px", probe.value(disabled, "border-top-width"), phase + ": no border");
            assertEquals("0px", probe.value(disabled, "border-top-left-radius"), phase + ": no radius");
            assertEquals("rgba(0, 0, 0, 0)", probe.value(disabled, "background-color"), phase + ": no fill");
        }
        for (String suffix : List.of("P", "L", "R")) {
            assertNotEquals(text("light/none|" + suffix, "current", "basic-2", "wide", "background-color"), text("dark/none|" + suffix, "current", "basic-2", "wide", "background-color"),
                    suffix + ": the current page's blue differs between themes (blue-600, blue-500)");
        }
    }

    @Test
    void theEntriesStatesAreToldApartByColourInBothThemes() {
        for (String theme : List.of("light/none|P", "dark/none|P")) {
            String link = text(theme, "link", "basic-1", "wide", "color");
            String disabled = text(theme, "disabled", "first-1", "wide", "color");
            String current = text(theme, "current", "basic-2", "wide", "color");
            assertEquals(3, new TreeSet<>(List.of(link, disabled, current)).size(), theme + ": link, disabled and current text colours differ");
            assertNotEquals(text(theme, "link", "basic-1", "wide", "border-top-color"), text(theme, "current", "basic-2", "wide", "border-top-color"), theme);
        }
        for (String[] pair : new String[][]{{"link", "basic-1"}, {"disabled", "first-1"}}) {
            assertNotEquals(text("light/none|P", pair[0], pair[1], "wide", "color"), text("dark/none|P", pair[0], pair[1], "wide", "color"), pair[0] + " differs in dark");
        }
        assertNotEquals(text("light/none|P", "link", "basic-1", "wide", "border-top-color"), text("dark/none|P", "link", "basic-1", "wide", "border-top-color"), "the link's border differs in dark");
    }

    @Test
    void aCurrentEntryGivenAnHrefIsTheCurrentPageAndAnEmptyOrMissingHrefOnAnotherEntryIsADisabledEntryNeverALink() {
        for (String phase : OWN_PHASES) {
            assertEquals(0, rows(phase, "link", "mixed-4", "wide").size(), phase + ": current wins over href");
            assertEquals(1, rows(phase, "current", "mixed-4", "wide").size(), phase);
            assertEquals(0, rows(phase, "link", "mixed-2", "wide").size(), phase + ": no href");
            assertEquals(0, rows(phase, "link", "mixed-3", "wide").size(), phase + ": empty href");
            assertEquals(1, rows(phase, "disabled", "mixed-2", "wide").size(), phase);
            assertEquals(1, rows(phase, "disabled", "mixed-3", "wide").size(), phase);
            assertEquals(1, rows(phase, "link", "mixed-5", "wide").size(), phase + ": a later entry with an href is a link");
        }
    }

    @Test
    void theColoursAHoverPaintsMatchTheLegacyHoverInEveryConfigurationBecauseTheyAreComparedThroughSwatches() {
        // The swatch rows are part of the comparisons above (kinds hover-swatch and hover-fill-swatch); this states that they carry real, distinct colours, different in the two themes.
        for (String kind : List.of("hover-swatch", "hover-fill-swatch")) {
            String name = kind.equals("hover-swatch") ? "hover" : "hover-fill";
            String light = text("light/none|P", kind, name, "swatch", "background-color");
            String dark = text("dark/none|P", kind, name, "swatch", "background-color");
            assertNotEquals("rgba(0, 0, 0, 0)", light);
            assertNotEquals("rgba(0, 0, 0, 0)", dark);
            assertNotEquals(light, dark, kind + " differs between themes");
            assertEquals(light, text("light/none|L", kind, name, "swatch", "background-color"), kind);
            assertEquals(dark, text("dark/none|L", kind, name, "swatch", "background-color"), kind);
        }
        assertNotEquals(text("light/none|P", "hover-swatch", "hover", "swatch", "background-color"), text("light/none|P", "hover-fill-swatch", "hover-fill", "swatch", "background-color"));
    }

    @Test
    void theKeyboardFocusRingIsTheLegacyTwoPixelBlueRingAndTheBrowsersOwnOutlineIsOffInBothThemesAndModes() {
        assertEquals(List.of(), lines(FOCUS));
        for (ParityConfigs.Config c : FOCUS_CONFIGS) {
            ParityProbe.Probe migrated = FOCUS_PROBES.get(c.id() + "|P");
            ParityProbe.Row row = migrated.rows().stream().filter(r -> r.kind().equals("link")).findFirst().orElseThrow();
            String shadow = migrated.value(row, "box-shadow");
            assertTrue(shadow.contains("0px 0px 0px 2px") && !shadow.contains("inset") && !shadow.equals("none"), c.id() + ": the focused link draws a 2px outer ring: " + shadow);
            assertEquals("none", migrated.value(row, "outline-style"), c.id() + ": the browser's focus outline is off");
        }
    }

    @Test
    void theFocusControlsAreDetected() {
        assertTrue(FOCUS_CONTROLS.get("focus ring dropped").stream().anyMatch(f -> f.property().equals("box-shadow")), "a dropped ring must be detected in box-shadow");
        assertTrue(FOCUS_CONTROLS.get("browser outline comes back").stream().anyMatch(f -> f.property().equals("outline-style")), "the outline coming back must be detected in outline-style");
    }

    @Test
    void theOracleIsNotVacuousEveryLegacyClassIsCompiledInTheCumulativeAndScopedStylesheets() throws IOException {
        Path dir = Path.of(System.getProperty("ashlar.cascade.dir"));
        String oracle = Files.readString(dir.resolve("pagination-oracle.css"), StandardCharsets.UTF_8);
        String scoped = Files.readString(dir.resolve("pagination-legacy-scoped.css"), StandardCharsets.UTF_8);
        // The shared oracle check does not escape ":", "/" or "." in every position this component's classes use, so they are checked here with Tailwind's own escaping.
        // Every legacy class is compiled (variants are written as pseudo-class, where() or media selectors).
        List<String> tokens = new ArrayList<>(legacy.allTokens());
        assertEquals(List.of(), tokens.stream().filter(t -> !compiles(oracle, t)).toList(), "the Pagination oracle does not compile these");
        assertEquals(List.of(), tokens.stream().filter(t -> !compiles(scoped, t)).toList(), "the scoped stylesheet does not compile these");
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
        assertEquals(List.of(), ParityOracle.uncompiled(oracle, earlier), "the Pagination oracle lost earlier legacy classes");
        List<String> later = new ArrayList<>();
        later.addAll(new StepperLegacyMarkup().allTokens());
        later.addAll(new BreadcrumbsLegacyMarkup().allTokens());
        assertEquals(List.of(), later.stream().filter(t -> !t.equals("group")).filter(t -> !compiles(oracle, t)).toList(), "the Pagination oracle lost the Stepper's or the Breadcrumbs' legacy classes");
        assertFalse(scoped.contains("html,:host"), "the scoped legacy stylesheet must not carry Tailwind's page-wide base");
        assertFalse(oracle.contains("ash-pagination") || scoped.contains("ash-pagination"), "the legacy oracle holds no migrated Pagination rule");
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
        // so each side where the migrated element has no border style at all is exactly one use, in both removed comparisons.
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
        // The focused frames also compare the ring's transparent offset layers (H1) and the same borderless sides (H2); nothing else.
        assertTrue(APPLIED_FOCUS.keySet().stream().allMatch(k -> k.equals("H2") || k.equals("H1")), "the focus comparisons use only H1 and H2: " + APPLIED_FOCUS);
    }

    // ---- what the real pages serve ----

    private static final Pattern ITEM = Pattern.compile("<li class=\"ash-pagination-item\">(.*?)</li>", Pattern.DOTALL);
    private static final Pattern PART = Pattern.compile("^(?:<a href=\"[^\"]+\" class=\"" + PaginationFixtures.LINK + "\">[^<]+</a>"
            + "|<span aria-current=\"page\" class=\"" + PaginationFixtures.CURRENT + "\">[^<]+</span>"
            + "|<span aria-disabled=\"true\" class=\"" + PaginationFixtures.DISABLED + "\">[^<]+</span>)$");
    private static final Pattern LEGACY_UTILITY = Pattern.compile("class=\"[^\"]*(?<![\\w-])(flex|flex-wrap|items-center|gap-1\\.5|text-sm|inline-flex|h-8|min-w-8|justify-center|"
            + "rounded-md|border|border-[a-z]+-\\d+|px-2|font-medium|text-slate-\\d+|text-white|bg-[a-z]+-\\d+|focus[a-z:-]*|hover:[a-z:-]+|dark:[a-z:-]+)(?![\\w-])");

    @Test
    void everyBarTheRealPagesServeHasTheApprovedShapeSemanticsAndNoTailwindOfItsOwn() throws Exception {
        ParitySession session = ParitySession.open("pagination-parity");
        HttpClient client = HttpClient.newHttpClient();
        List<String> paths = new ArrayList<>(List.of("/ashlar/", "/ashlar/components"));
        for (ComponentPage page : ComponentCatalog.PAGES) {
            paths.add("/ashlar/components/" + page.slug());
        }
        int bars = 0;
        int items = 0;
        int currents = 0;
        int links = 0;
        int disabled = 0;
        List<String> problems = new ArrayList<>();
        try (RunningShowcase app = RunningShowcase.start(session.bootJar())) {
            for (String path : paths) {
                HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + path)).GET().build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                assertEquals(200, response.statusCode(), path);
                String body = response.body();
                int lists = count(body, "<ul class=\"ash-pagination__list\">");
                assertEquals(lists, count(body, "<nav aria-label=\"Pagination\" data-pagination class=\"ash-pagination\">"), path + ": every list sits in the named landmark with its hook");
                assertEquals(PaginationLegacyMarkup.count(body), lists, path + ": every ash-pagination class must be on a bar's own landmark");
                assertEquals(0, count(body, "<ul class=\"flex flex-wrap items-center gap-1.5"), path + ": no page still serves the old classes");
                bars += lists;
                int pageCurrents = 0;
                Matcher m = ITEM.matcher(body);
                int pageItems = 0;
                while (m.find()) {
                    String entry = m.group(1).strip();
                    String shown = entry.substring(0, Math.min(110, entry.length()));
                    if (!entry.contains("ash-pagination-item__control")) {
                        continue;
                    }
                    items++;
                    pageItems++;
                    if (!PART.matcher(entry).find()) {
                        problems.add(path + ": an entry is not exactly one link, current page or disabled entry: " + shown);
                    }
                    if (count(entry, "aria-current=\"page\"") != count(entry, "--current")) {
                        problems.add(path + ": aria-current=\"page\" must be on the current page and only there: " + shown);
                    }
                    if (count(entry, "aria-disabled=\"true\"") != count(entry, "--disabled")) {
                        problems.add(path + ": aria-disabled=\"true\" must be on a disabled entry and only there: " + shown);
                    }
                    pageCurrents += count(entry, "aria-current=\"page\"");
                    links += count(entry, "<a ");
                    disabled += count(entry, "aria-disabled=\"true\"");
                    if (LEGACY_UTILITY.matcher(entry).find()) {
                        problems.add(path + ": an entry still carries a Tailwind utility of its own: " + shown);
                    }
                }
                currents += pageCurrents;
                assertTrue(pageCurrents <= lists, path + ": no bar has two current pages");
                assertTrue(lists == 0 || pageItems > 0, path + ": a bar has entries");
            }
        }
        assertEquals(List.of(), problems);
        assertTrue(bars >= 6 && items >= 20 && links >= 10 && currents >= 5 && disabled >= 2, "bars=" + bars + " items=" + items + " links=" + links + " currents=" + currents + " disabled=" + disabled);
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }
}
