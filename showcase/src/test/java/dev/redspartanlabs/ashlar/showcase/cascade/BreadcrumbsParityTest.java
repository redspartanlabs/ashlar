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
 * The Breadcrumbs' and the Breadcrumb Item's parity gates, measured in real Chrome, on the shared parity harness (unchanged).
 *
 * <p>Evidence is a controlled oracle: the legacy Tailwind markup (compiled by the real Tailwind CLI) next to the migrated markup, in the
 * same browser, in three trail widths for each of the 22 theme and skin configurations, with Tailwind present and with Tailwind removed.
 * The cases are a lone current page, two, three and five crumbs, long labels including an unbroken word, and a mixed trail (plain text, an
 * empty {@code href}, and a current crumb that is also given an {@code href}, which must not become a link). Every part of every crumb is
 * measured: the landmark, the list, the crumb, its separator, and its link or text. The narrow widths make the trail wrap.
 *
 * <p>A hover cannot be held in a measurement, so the colour a link takes on hover is compared through a swatch painted with that colour,
 * in both themes; the keyboard focus ring IS measured, one frame per Chrome run because only one frame holds focus.
 *
 * <p>One behavior deliberately differs from the legacy rendering and is therefore NOT compared with it: a trail placed under an unrelated
 * first element. The legacy separator was hidden by {@code group-first:hidden}, which matches any {@code .group:first-child} ancestor, so
 * every separator of such a trail vanished. The crumb now hides only its own separator, and only when it is the first crumb. That case is
 * measured on its own, next to the legacy rendering that shows the old leak, and a control shows the old over-reach would be caught.
 *
 * <p>Evidence is Chrome only. Computed-style and box parity is the visual evidence; there is no screenshot comparison.
 */
class BreadcrumbsParityTest {

    private static final String ORACLE = ParityFrames.generated("breadcrumbs-oracle.css");
    private static final String SCOPED = ParityFrames.generated("breadcrumbs-legacy-scoped.css");
    private static final List<ParityConfigs.Config> CONFIGS = ParityConfigs.all();
    private static final List<String> EXTRA_PROPS = List.of("flex-wrap", "flex-direction", "column-gap", "row-gap", "align-items", "list-style-type",
            "list-style-position", "line-height", "font-size", "font-weight", "overflow-wrap", "text-decoration-line", "text-decoration-style",
            "text-underline-offset", "cursor", "z-index");

    private record Layout(String name, String style) {
    }

    private static final List<Layout> LAYOUTS = List.of(
            new Layout("wide", "box-sizing:border-box;width:400px"),
            new Layout("narrow", "box-sizing:border-box;width:200px"),
            new Layout("tiny", "box-sizing:border-box;width:120px"));

    private static final String LONG = "Quarterly Enterprise Revenue Summary by Region and Product Line";
    private static final String UNBROKEN = "Supercalifragilisticexpialidocious".repeat(2);

    private static final Map<String, List<BreadcrumbsFixtures.Entry>> CASES = new LinkedHashMap<>();

    static {
        CASES.put("single", List.of(BreadcrumbsFixtures.entry("single-1", "Home", null, true)));
        CASES.put("two", List.of(
                BreadcrumbsFixtures.entry("two-1", "Home", "/", false),
                BreadcrumbsFixtures.entry("two-2", "Widgets", null, true)));
        CASES.put("three", List.of(
                BreadcrumbsFixtures.entry("three-1", "Home", "/", false),
                BreadcrumbsFixtures.entry("three-2", "Products", "/products", false),
                BreadcrumbsFixtures.entry("three-3", "Widgets", null, true)));
        CASES.put("deep", List.of(
                BreadcrumbsFixtures.entry("deep-1", "Home", "/", false),
                BreadcrumbsFixtures.entry("deep-2", "Accounts", "/accounts", false),
                BreadcrumbsFixtures.entry("deep-3", "Acme Manufacturing", "/accounts/acme-manufacturing", false),
                BreadcrumbsFixtures.entry("deep-4", "Orders", "/accounts/acme-manufacturing/orders", false),
                BreadcrumbsFixtures.entry("deep-5", "Order #48213", null, true)));
        CASES.put("long", List.of(
                BreadcrumbsFixtures.entry("long-1", "Home", "/", false),
                BreadcrumbsFixtures.entry("long-2", UNBROKEN, "/reports", false),
                BreadcrumbsFixtures.entry("long-3", LONG, null, true)));
        CASES.put("mixed", List.of(
                BreadcrumbsFixtures.entry("mixed-1", "Home", "/", false),
                BreadcrumbsFixtures.entry("mixed-2", "Plain", null, false),
                BreadcrumbsFixtures.entry("mixed-3", "Empty", "", false),
                BreadcrumbsFixtures.entry("mixed-4", "Wins", "/wins", true),
                BreadcrumbsFixtures.entry("mixed-5", "Last", "/last", false)));
    }

    private static final List<Layout> CONTROL_LAYOUTS = List.of(LAYOUTS.get(0), LAYOUTS.get(2));
    private static final Set<String> CONTROL_CASES = Set.of("three", "long", "mixed");

    private static final List<ParityConfigs.Config> GROUP_CONFIGS = List.of(ParityConfigs.get("light/none"), ParityConfigs.get("dark/none"));
    private static final List<ParityConfigs.Config> FOCUS_CONFIGS = List.of(ParityConfigs.get("light/none"), ParityConfigs.get("dark/none"));
    private static final String FOCUS_CONTROL_RING = ".ash-breadcrumb-item__link:focus-visible{box-shadow:none}";
    private static final String FOCUS_CONTROL_OUTLINE = ".ash-breadcrumb-item__link:focus{outline-style:auto}";

    // ---- state of the measurement ----

    private static BreadcrumbsLegacyMarkup legacy;
    private static BreadcrumbsFixtures fixtures;
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
            for (Map.Entry<String, List<BreadcrumbsFixtures.Entry>> c : CASES.entrySet()) {
                if (!caseNames.contains(c.getKey())) {
                    continue;
                }
                StringBuilder items = new StringBuilder();
                for (BreadcrumbsFixtures.Entry e : c.getValue()) {
                    items.append(fixtures.item(e, legacyClasses, true));
                }
                sb.append("<div style=\"margin:4px 0\"><div data-layout=\"").append(layout.name()).append("\" style=\"").append(layout.style()).append("\">")
                        .append(fixtures.trail(c.getKey(), items.toString(), legacyClasses, true)).append("</div></div>");
            }
        }
        return sb.append("<div data-layout=\"swatch\">").append(fixtures.hoverSwatch(legacyClasses)).append("</div>").toString();
    }

    /** A trail under an unrelated element that is a first child and carries the {@code group} class the legacy separator rule looked for. */
    private static String groupedMarkup(boolean legacyClasses) {
        StringBuilder items = new StringBuilder();
        for (BreadcrumbsFixtures.Entry e : CASES.get("three")) {
            items.append(fixtures.item(new BreadcrumbsFixtures.Entry("grp-" + e.name(), e.label(), e.href(), e.current()), legacyClasses, true));
        }
        return "<div data-layout=\"grouped\" style=\"box-sizing:border-box;width:400px\"><div class=\"group\">"
                + fixtures.trail("grouped", items.toString(), legacyClasses, true) + "</div></div>";
    }

    /** One link, focused by a script as soon as its frame loads, as the only content of its frame. */
    private static String focusBody(boolean legacyClasses) {
        String home = fixtures.item(BreadcrumbsFixtures.entry("focus-1", "Home", "/", false), legacyClasses, true).replaceFirst("<a ", "<a data-focus ");
        String current = fixtures.item(BreadcrumbsFixtures.entry("focus-2", "Widgets", null, true), legacyClasses, true);
        return fixtures.trail("focus", home + current, legacyClasses, true) + "<script>document.querySelector('[data-focus]').focus();</script>";
    }

    // ---- control experiments: each breaks the migrated trail in ONE way and must be detected ----

    private record Control(String name, String config, String css, String expectedProperty) {
    }

    private static final List<Control> CONTROL_LIST = List.of(
            new Control("list no longer wraps", "light/none", ".ash-breadcrumbs__list{flex-wrap:nowrap}", "flex-wrap"),
            new Control("wrong gap along the line", "light/none", ".ash-breadcrumbs__list{column-gap:0.5rem}", "column-gap"),
            new Control("wrong gap between wrapped lines", "light/none", ".ash-breadcrumbs__list{row-gap:0.5rem}", "row-gap"),
            new Control("list is not centred", "light/none", ".ash-breadcrumbs__list{align-items:flex-start}", "align-items"),
            new Control("list text is the wrong size", "light/none", ".ash-breadcrumbs__list{font-size:1rem}", "font-size"),
            new Control("list line height is wrong", "light/none", ".ash-breadcrumbs__list{--ash-breadcrumbs-leading:1.75}", "line-height"),
            new Control("native list padding comes back", "light/none", ".ash-breadcrumbs__list{padding-inline-start:40px}", "padding-left"),
            new Control("native list margin comes back", "light/none", ".ash-breadcrumbs__list{margin-block:16px}", "margin-top"),
            new Control("the numerals come back", "light/none", "ol.ash-breadcrumbs__list{list-style:decimal}", "list-style-type"),
            new Control("wrong gap inside a crumb", "light/none", ".ash-breadcrumb-item{gap:0.5rem}", "column-gap"),
            new Control("crumb is not centred", "light/none", ".ash-breadcrumb-item{align-items:flex-start}", "align-items"),
            new Control("the first crumb shows its separator", "light/none", ".ash-breadcrumb-item:first-child > .ash-breadcrumb-item__separator{display:block}", "display"),
            new Control("no separator is shown", "light/none", ".ash-breadcrumb-item__separator{display:none}", "display"),
            new Control("wrong separator colour", "light/none", ".ash-breadcrumb-item{--ash-breadcrumb-item-separator:red}", "color"),
            new Control("dark separator ignores the theme", "dark/none", ".ash-breadcrumb-item{--ash-breadcrumb-item-separator:var(--color-slate-400)}", "color"),
            new Control("wrong link colour", "light/none", ".ash-breadcrumb-item{--ash-breadcrumb-item-link:red}", "color"),
            new Control("dark link ignores the theme", "dark/none", ".ash-breadcrumb-item{--ash-breadcrumb-item-link:var(--color-slate-500)}", "color"),
            new Control("link is underlined at rest", "light/none", ".ash-breadcrumb-item__link{text-decoration:underline}", "text-decoration-line"),
            new Control("link underline is the wrong distance away", "light/none", ".ash-breadcrumb-item__link{text-underline-offset:4px}", "text-underline-offset"),
            new Control("link radius ignores the skin", "light/axiom", ".ash-breadcrumb-item__link{border-radius:0.25rem}", "border-top-left-radius"),
            new Control("link is not rounded", "light/none", ".ash-breadcrumb-item__link{border-radius:0}", "border-top-left-radius"),
            new Control("long words no longer break in a link", "light/none", ".ash-breadcrumb-item__link{overflow-wrap:normal}", "overflow-wrap"),
            new Control("long words no longer break in text", "light/none", ".ash-breadcrumb-item__text{overflow-wrap:normal}", "overflow-wrap"),
            new Control("current crumb is not medium weight", "light/none", ".ash-breadcrumb-item__text--current{font-weight:400}", "font-weight"),
            new Control("plain text is medium weight", "light/none", ".ash-breadcrumb-item__text{font-weight:500}", "font-weight"),
            new Control("wrong current colour", "light/none", ".ash-breadcrumb-item{--ash-breadcrumb-item-current:red}", "color"),
            new Control("dark current ignores the theme", "dark/none", ".ash-breadcrumb-item{--ash-breadcrumb-item-current:var(--color-slate-900)}", "color"),
            new Control("wrong plain text colour", "light/none", ".ash-breadcrumb-item{--ash-breadcrumb-item-text:red}", "color"),
            new Control("dark plain text ignores the theme", "dark/none", ".ash-breadcrumb-item{--ash-breadcrumb-item-text:var(--color-slate-500)}", "color"),
            new Control("colour ignores the skin", "light/civic", ".ash-breadcrumb-item{--ash-breadcrumb-item-current:#0f172a}", "color"),
            new Control("wrong hover colour", "light/none", ".ash-breadcrumb-item{--ash-breadcrumb-item-link-hover:red}", "background-color"),
            new Control("dark hover ignores the theme", "dark/none", ".ash-breadcrumb-item{--ash-breadcrumb-item-link-hover:var(--color-slate-700)}", "background-color"));

    /** The old over-reach, a rule that reaches a separator through an unrelated ancestor, applied to the migrated rendering. */
    private static final String GROUP_OVERREACH = ".group .ash-breadcrumb-item__separator{display:none}";

    // ---- running ----

    @BeforeAll
    static void measure() throws Exception {
        ParitySession session = ParitySession.open("breadcrumbs-parity");
        legacy = new BreadcrumbsLegacyMarkup();
        fixtures = new BreadcrumbsFixtures(legacy);
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
        // The group ancestor: not compared with the legacy rendering (that is the corrected behavior); measured next to it.
        String groupedMigrated = groupedMarkup(false);
        String groupedLegacy = groupedMarkup(true);
        for (ParityConfigs.Config c : GROUP_CONFIGS) {
            frames.append(ParityFrames.frame(c.id() + "|G", c, ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), "", groupedMigrated));
            frames.append(ParityFrames.frame(c.id() + "|GR", c, ParityFrames.with(ParityFrames.ASHLAR, ParityConfigs.SKIN_LINKS), environment, groupedMigrated));
            frames.append(ParityFrames.frame(c.id() + "|GL", c, ParityFrames.with(ORACLE, ParityConfigs.SKIN_LINKS), "", groupedLegacy));
        }
        frames.append(ParityFrames.frame("group-overreach|G", ParityConfigs.get("light/none"), ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), GROUP_OVERREACH, groupedMigrated));

        probe = ParityFrames.measure(session, frames.toString(), EXTRA_PROPS, "breadcrumbs frames");
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
    void controlledOracleWithTailwindPresentMatchesTheLegacyBreadcrumbsInEveryConfigurationCaseAndWidth() {
        assertEquals(List.of(), lines(PRESENT));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheLegacyBreadcrumbsGivenTheSameSurroundings() {
        assertEquals(List.of(), lines(REMOVED_SCOPED));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheFullLegacyRendering() {
        assertEquals(List.of(), lines(REMOVED_FULL));
    }

    @Test
    void theScopedLegacyStylesheetRendersTheLegacyBreadcrumbsExactlyAsTheFullOneDoes() {
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
    void theCasesAreCompleteEveryPartIsMeasuredAndTheNarrowWidthsReallyWrapTheTrail() {
        Map<String, Integer> kinds = new TreeMap<>();
        for (ParityProbe.Row r : probe.phase("light/none|L")) {
            kinds.merge(r.kind(), 1, Integer::sum);
        }
        for (String kind : List.of("nav", "list", "item", "separator", "link", "text", "current", "hover-swatch")) {
            assertTrue(kinds.getOrDefault(kind, 0) > 0, "no " + kind + " was measured: " + kinds);
        }
        long crumbs = CASES.values().stream().mapToLong(List::size).sum();
        assertEquals(crumbs * LAYOUTS.size(), kinds.get("item").longValue());
        assertEquals(crumbs * LAYOUTS.size(), kinds.get("separator").longValue(), "every crumb has a separator in the markup");
        assertEquals((long) CASES.size() * LAYOUTS.size(), kinds.get("nav").longValue());
        long currents = CASES.values().stream().flatMap(List::stream).filter(BreadcrumbsFixtures.Entry::current).count();
        long links = CASES.values().stream().flatMap(List::stream).filter(BreadcrumbsFixtures.Entry::isLink).count();
        assertEquals(currents * LAYOUTS.size(), kinds.get("current").longValue());
        assertEquals(links * LAYOUTS.size(), kinds.get("link").longValue());
        assertEquals((crumbs - currents - links) * LAYOUTS.size(), kinds.get("text").longValue());
        assertTrue(ROWS.values().stream().allMatch(n -> n.equals(ROWS.get("light/none|L"))), "every configuration measures the same rows");
        for (String phase : OWN_PHASES) {
            // One line is 20px tall; the five-crumb trail and the long labels wrap onto further lines when narrow, with a 4px gap between lines.
            assertEquals(20.0, num(phase, "list", "three", "wide", "@h"), 0.01, phase + ": a short trail is one line");
            assertTrue(num(phase, "list", "deep", "tiny", "@h") >= 44.0, phase + ": a long trail wraps at 120px");
            assertTrue(num(phase, "list", "long", "narrow", "@h") > 20.0, phase + ": long labels wrap at 200px");
            // Words wrap inside the trail. (An UNBROKEN word does not shrink its crumb, which is a flex item that cannot get narrower than the word: it overflows
            // the trail in the legacy rendering too, and the comparisons above prove the migrated one does exactly the same.)
            for (Layout layout : LAYOUTS) {
                double width = num(phase, "nav", "long", layout.name(), "@w");
                assertTrue(num(phase, "item", "long-3", layout.name(), "@w") <= width + 0.01, phase + " " + layout.name() + " the long label stays inside the trail");
                assertTrue(num(phase, "item", "long-2", layout.name(), "@w") > num(phase, "item", "long-1", layout.name(), "@w"), phase + " " + layout.name() + " the unbroken word keeps its width");
            }
        }
    }

    @Test
    void theListIsAWrappingCentredRowWithTheNativeListStylingTakenAwayAndTheCrumbsAreRowsWithASmallGap() {
        for (String phase : OWN_PHASES) {
            for (Layout layout : LAYOUTS) {
                String where = phase + " " + layout.name();
                assertEquals("flex", text(phase, "list", "three", layout.name(), "display"), where);
                assertEquals("row", text(phase, "list", "three", layout.name(), "flex-direction"), where);
                assertEquals("wrap", text(phase, "list", "three", layout.name(), "flex-wrap"), where);
                assertEquals("center", text(phase, "list", "three", layout.name(), "align-items"), where);
                assertEquals("6px", text(phase, "list", "three", layout.name(), "column-gap"), where + ": along the line");
                assertEquals("4px", text(phase, "list", "three", layout.name(), "row-gap"), where + ": between lines");
                assertEquals("14px", text(phase, "list", "three", layout.name(), "font-size"), where);
                assertEquals("20px", text(phase, "list", "three", layout.name(), "line-height"), where);
                assertEquals("none", text(phase, "list", "three", layout.name(), "list-style-type"), where + ": no numerals");
                for (String side : List.of("padding-left", "margin-top", "margin-left")) {
                    assertEquals("0px", text(phase, "list", "three", layout.name(), side), where + " " + side);
                }
                assertEquals("flex", text(phase, "item", "three-2", layout.name(), "display"), where);
                assertEquals("center", text(phase, "item", "three-2", layout.name(), "align-items"), where);
                assertEquals("6px", text(phase, "item", "three-2", layout.name(), "column-gap"), where);
            }
        }
    }

    @Test
    void onlyTheFirstCrumbsSeparatorIsHiddenAndItIsNotMeasuredAsTakingUpRoom() {
        for (String phase : OWN_PHASES) {
            for (Layout layout : LAYOUTS) {
                for (Map.Entry<String, List<BreadcrumbsFixtures.Entry>> c : CASES.entrySet()) {
                    List<BreadcrumbsFixtures.Entry> crumbs = c.getValue();
                    for (int i = 0; i < crumbs.size(); i++) {
                        String name = crumbs.get(i).name();
                        String where = phase + " " + layout.name() + " " + name;
                        if (i == 0) {
                            assertEquals("none", text(phase, "separator", name, layout.name(), "display"), where + ": the first crumb has no separator");
                            assertEquals(0.0, num(phase, "separator", name, layout.name(), "@w"), 0.01, where);
                        } else {
                            assertNotEquals("none", text(phase, "separator", name, layout.name(), "display"), where + ": every later crumb has one");
                            assertTrue(num(phase, "separator", name, layout.name(), "@w") > 0, where);
                        }
                    }
                }
            }
        }
    }

    @Test
    void theLinkIsNotUnderlinedAtRestSitsTwoPixelsOffTheTextTakesTheSkinsSmallRadiusAndNeverFillsAnything() {
        for (String phase : OWN_PHASES) {
            ParityProbe.Row link = one(phase, "link", "three-2", "wide");
            assertEquals("none", probe.value(link, "text-decoration-line"), phase);
            assertEquals("2px", probe.value(link, "text-underline-offset"), phase + ": the offset applies at rest too");
            assertEquals("4px", probe.value(link, "border-top-left-radius"), phase + ": rounded-sm");
            assertEquals("rgba(0, 0, 0, 0)", probe.value(link, "background-color"), phase);
            assertEquals("pointer", probe.value(link, "cursor"), phase);
        }
        // A skin that sets --radius-sm changes the radius, exactly as Tailwind's rounded-sm did.
        assertEquals("2px", text("light/axiom|P", "link", "three-2", "wide", "border-top-left-radius"));
        assertEquals("2px", text("light/axiom|L", "link", "three-2", "wide", "border-top-left-radius"));
        assertEquals("2px", text("light/axiom|R", "link", "three-2", "wide", "border-top-left-radius"));
        assertEquals("3px", text("light/civic|P", "link", "three-2", "wide", "border-top-left-radius"));
    }

    @Test
    void theCrumbsStatesAreToldApartByColourAndWeightInBothThemesAndOnlyTheCurrentCrumbIsMedium() {
        for (String theme : List.of("light/none|P", "dark/none|P")) {
            String link = text(theme, "link", "mixed-1", "wide", "color");
            String plain = text(theme, "text", "mixed-2", "wide", "color");
            String current = text(theme, "current", "mixed-4", "wide", "color");
            String separator = text(theme, "separator", "mixed-2", "wide", "color");
            assertEquals(link, plain, theme + ": plain text reads like a link at rest");
            assertEquals(3, new TreeSet<>(List.of(link, current, separator)).size(), theme + ": link, current and separator colours differ");
            assertEquals("500", text(theme, "current", "mixed-4", "wide", "font-weight"), theme);
            assertEquals("400", text(theme, "text", "mixed-2", "wide", "font-weight"), theme);
            assertEquals("400", text(theme, "link", "mixed-1", "wide", "font-weight"), theme);
            assertEquals("break-word", text(theme, "link", "mixed-1", "wide", "overflow-wrap"), theme);
            assertEquals("break-word", text(theme, "text", "mixed-2", "wide", "overflow-wrap"), theme);
            assertEquals("break-word", text(theme, "current", "mixed-4", "wide", "overflow-wrap"), theme);
        }
        for (String[] pair : new String[][]{{"link", "mixed-1"}, {"text", "mixed-2"}, {"current", "mixed-4"}, {"separator", "mixed-2"}}) {
            assertNotEquals(text("light/none|P", pair[0], pair[1], "wide", "color"), text("dark/none|P", pair[0], pair[1], "wide", "color"), pair[0] + " differs in dark");
        }
    }

    @Test
    void aCurrentCrumbGivenAnHrefIsTextAndAnEmptyOrMissingHrefIsPlainTextNeverALink() {
        for (String phase : OWN_PHASES) {
            assertEquals(0, rows(phase, "link", "mixed-4", "wide").size(), phase + ": current wins over href");
            assertEquals(1, rows(phase, "current", "mixed-4", "wide").size(), phase);
            assertEquals(0, rows(phase, "link", "mixed-2", "wide").size(), phase + ": no href");
            assertEquals(0, rows(phase, "link", "mixed-3", "wide").size(), phase + ": empty href");
            assertEquals(1, rows(phase, "text", "mixed-2", "wide").size(), phase);
            assertEquals(1, rows(phase, "text", "mixed-3", "wide").size(), phase);
            assertEquals(1, rows(phase, "link", "mixed-5", "wide").size(), phase + ": a later crumb with an href is a link");
        }
    }

    @Test
    void theColourAHoverPaintsMatchesTheLegacyHoverInEveryConfigurationBecauseItIsComparedThroughASwatch() {
        // The swatch rows are part of the comparisons above (kind hover-swatch); this states that they carry a real colour, different in the two themes.
        String light = text("light/none|P", "hover-swatch", "hover", "swatch", "background-color");
        String dark = text("dark/none|P", "hover-swatch", "hover", "swatch", "background-color");
        assertNotEquals("rgba(0, 0, 0, 0)", light);
        assertNotEquals("rgba(0, 0, 0, 0)", dark);
        assertNotEquals(light, dark, "the dark hover is slate-200, the light one slate-700");
        assertEquals(light, text("light/none|L", "hover-swatch", "hover", "swatch", "background-color"));
        assertEquals(dark, text("dark/none|L", "hover-swatch", "hover", "swatch", "background-color"));
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
    void aTrailUnderAnUnrelatedFirstGroupElementKeepsItsSeparatorsAndTheLegacyRenderingShowsTheLeakThatWasCorrected() {
        for (ParityConfigs.Config c : GROUP_CONFIGS) {
            for (String suffix : List.of("G", "GR")) {
                String phase = c.id() + "|" + suffix;
                assertEquals("none", text(phase, "separator", "grp-three-1", "grouped", "display"), phase + ": the first crumb still has none");
                assertNotEquals("none", text(phase, "separator", "grp-three-2", "grouped", "display"), phase + ": a later crumb keeps its separator");
                assertNotEquals("none", text(phase, "separator", "grp-three-3", "grouped", "display"), phase);
            }
            // The legacy rule matched the unrelated ancestor, so no separator of that trail was drawn.
            String legacyPhase = c.id() + "|GL";
            assertEquals("none", text(legacyPhase, "separator", "grp-three-2", "grouped", "display"), legacyPhase + ": the old rule reached the separator under an unrelated .group:first-child");
            assertEquals("none", text(legacyPhase, "separator", "grp-three-3", "grouped", "display"), legacyPhase);
        }
    }

    @Test
    void theOldOverReachWouldBeCaughtByTheGroupGate() {
        assertEquals("none", text("group-overreach|G", "separator", "grp-three-2", "grouped", "display"), "a rule that reaches the group ancestor hides the separator again, and the gate sees it");
        assertNotEquals("none", text("light/none|G", "separator", "grp-three-2", "grouped", "display"));
    }

    @Test
    void theOracleIsNotVacuousEveryLegacyClassIsCompiledInTheCumulativeAndScopedStylesheets() throws IOException {
        Path dir = Path.of(System.getProperty("ashlar.cascade.dir"));
        String oracle = Files.readString(dir.resolve("breadcrumbs-oracle.css"), StandardCharsets.UTF_8);
        String scoped = Files.readString(dir.resolve("breadcrumbs-legacy-scoped.css"), StandardCharsets.UTF_8);
        // The shared oracle check does not escape ":", "/" or "." in every position this component's classes use, so they are checked here with Tailwind's own escaping.
        // "group" is a marker class Tailwind writes no rule for; every other legacy class is compiled (variants are written as pseudo-class, where() or media selectors).
        List<String> tokens = legacy.allTokens().stream().filter(t -> !t.equals("group")).toList();
        assertEquals(List.of(), tokens.stream().filter(t -> !compiles(oracle, t)).toList(), "the Breadcrumbs oracle does not compile these");
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
        assertEquals(List.of(), ParityOracle.uncompiled(oracle, earlier), "the Breadcrumbs oracle lost earlier legacy classes");
        List<String> stepper = new StepperLegacyMarkup().allTokens().stream().filter(t -> !t.equals("group")).toList();
        assertEquals(List.of(), stepper.stream().filter(t -> !compiles(oracle, t)).toList(), "the Breadcrumbs oracle lost the Stepper's legacy classes");
        assertFalse(scoped.contains("html,:host"), "the scoped legacy stylesheet must not carry Tailwind's page-wide base");
        assertFalse(oracle.contains("ash-breadcrumb") || scoped.contains("ash-breadcrumb"), "the legacy oracle holds no migrated Breadcrumbs rule");
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

    private static final Pattern ITEM = Pattern.compile("<li class=\"ash-breadcrumb-item\">(.*?)</li>", Pattern.DOTALL);
    private static final Pattern PART = Pattern.compile(
            "^<span aria-hidden=\"true\" data-breadcrumb-separator class=\"ash-breadcrumb-item__separator\">/</span>\\s*"
                    + "(?:<a href=\"[^\"]+\" class=\"ash-breadcrumb-item__link\">[^<]+</a>"
                    + "|<span class=\"ash-breadcrumb-item__text\">[^<]+</span>"
                    + "|<span aria-current=\"page\" class=\"ash-breadcrumb-item__text ash-breadcrumb-item__text--current\">[^<]+</span>)\\s*$");
    private static final Pattern LEGACY_UTILITY = Pattern.compile("class=\"[^\"]*(?<![\\w-])(group|flex|flex-wrap|items-center|gap-1\\.5|gap-x-1\\.5|gap-y-1|text-sm|break-words|rounded-sm|"
            + "underline-offset-2|font-medium|text-slate-\\d+|focus[a-z:-]*|hover:[a-z:-]+|dark:[a-z:-]+|group-first:hidden)(?![\\w-])");

    @Test
    void everyTrailTheRealPagesServeHasTheApprovedShapeSemanticsAndNoTailwindOfItsOwn() throws Exception {
        ParitySession session = ParitySession.open("breadcrumbs-parity");
        HttpClient client = HttpClient.newHttpClient();
        List<String> paths = new ArrayList<>(List.of("/ashlar/", "/ashlar/components"));
        for (ComponentPage page : ComponentCatalog.PAGES) {
            paths.add("/ashlar/components/" + page.slug());
        }
        int trails = 0;
        int items = 0;
        int currents = 0;
        int links = 0;
        List<String> problems = new ArrayList<>();
        try (RunningShowcase app = RunningShowcase.start(session.bootJar())) {
            for (String path : paths) {
                HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + path)).GET().build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                assertEquals(200, response.statusCode(), path);
                String body = response.body();
                int lists = count(body, "<ol class=\"ash-breadcrumbs__list\">");
                assertEquals(lists, count(body, "<nav aria-label=\"Breadcrumb\" data-breadcrumbs class=\"ash-breadcrumbs\">"), path + ": every list sits in the named landmark with its hook");
                assertEquals(BreadcrumbsLegacyMarkup.count(body), lists, path + ": every ash-breadcrumbs class must be on a trail's own landmark");
                assertEquals(0, count(body, "<ol class=\"flex flex-wrap"), path + ": no page still serves the old classes");
                trails += lists;
                int pageCurrents = 0;
                Matcher m = ITEM.matcher(body);
                while (m.find()) {
                    items++;
                    String entry = m.group(1).strip();
                    String shown = entry.substring(0, Math.min(110, entry.length()));
                    if (!PART.matcher(entry).find()) {
                        problems.add(path + ": a crumb is not a separator then exactly one link, text or current text: " + shown);
                    }
                    if (count(entry, "aria-hidden=\"true\"") != 1) {
                        problems.add(path + ": only the separator is hidden: " + shown);
                    }
                    if (count(entry, "aria-current=\"page\"") != count(entry, "ash-breadcrumb-item__text--current")) {
                        problems.add(path + ": aria-current=\"page\" must be on the current crumb and only there: " + shown);
                    }
                    if (count(entry, "ash-breadcrumb-item__link") > 0 && count(entry, "ash-breadcrumb-item__text") > 0) {
                        problems.add(path + ": a crumb is either a link or text: " + shown);
                    }
                    pageCurrents += count(entry, "aria-current=\"page\"");
                    links += count(entry, "<a ");
                    if (LEGACY_UTILITY.matcher(entry).find()) {
                        problems.add(path + ": a crumb still carries a Tailwind utility of its own: " + shown);
                    }
                }
                currents += pageCurrents;
                assertEquals(lists, pageCurrents, path + ": every trail has exactly one current crumb");
            }
        }
        assertEquals(List.of(), problems);
        assertEquals(trails, currents);
        assertTrue(trails >= 5 && items >= 16 && links >= 8, "trails=" + trails + " items=" + items + " links=" + links + " currents=" + currents);
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }
}
