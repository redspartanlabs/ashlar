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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The List's parity gates, measured in real Chrome, on the shared parity harness (unchanged since the Icon migration).
 *
 * <p>Evidence is a controlled oracle: the List's legacy Tailwind markup (compiled by the real Tailwind CLI) next to its migrated markup, in
 * the same browser, in seven layouts for each of the 22 theme and skin configurations, with Tailwind present and with Tailwind removed.
 * The cases are what the List's separators are about: a list of one row, of two, of three (first, middle, last), an ordered list, a list
 * of raw {@code <li>} children and of a raw {@code <div>} child (separators apply to ANY direct child), an empty list, and a list nested
 * inside a row (the outer separators must not reach into it). Every list and every row is measured: border, radius, separator, native list
 * reset (including that an ordered list shows no numerals), spacing, box. Negative controls show each gate can fail. The Item-level
 * measurements are in {@code ListItemParityTest}; every list on the real showcase page is also compared by {@code ShowcaseRuntimeTest}.
 *
 * <p>Evidence is Chrome only. Computed-style and box parity is the visual evidence; there is no screenshot comparison.
 */
class ListParityTest {

    private static final String ORACLE = ParityFrames.generated("list-oracle.css");
    private static final String SCOPED = ParityFrames.generated("list-legacy-scoped.css");
    private static final List<ParityConfigs.Config> CONFIGS = ParityConfigs.all();
    private static final List<String> EXTRA_PROPS = List.of("list-style-type", "list-style-position", "list-style-image", "text-decoration-line",
            "line-height", "font-size", "font-weight");

    private record Layout(String name, String style, String siblingStyle) {
    }

    private static final List<Layout> LAYOUTS = List.of(
            new Layout("block", "box-sizing:border-box;width:300px", ""),
            new Layout("narrow", "box-sizing:border-box;width:160px", ""),
            new Layout("column-flex", "box-sizing:border-box;display:flex;flex-direction:column;width:300px", ""),
            new Layout("column-flex-centered", "box-sizing:border-box;display:flex;flex-direction:column;align-items:center;width:300px", ""),
            new Layout("row-flex", "box-sizing:border-box;display:flex;flex-direction:row;align-items:flex-start;width:300px", "display:block;flex:none;width:60px"),
            new Layout("grid", "box-sizing:border-box;display:grid;width:300px", ""),
            new Layout("card", "box-sizing:border-box;width:300px;padding:16px;margin-top:4px", ""));

    /** The kinds of measured element that are the caller's own content, not the component's: preflight reached them too, so a full-legacy comparison leaves them out. */
    private static final java.util.Set<String> CALLER_KINDS = java.util.Set.of("raw");

    private static List<ParityProbe.Row> owned(List<ParityProbe.Row> rows) {
        return rows.stream().filter(r -> !CALLER_KINDS.contains(r.kind())).toList();
    }

    private static final List<String> CASES = List.of("ul-1", "ul-2", "ul-3", "ol-3", "raw-li-3", "raw-mixed", "nested", "empty");

    // ---- state of the measurement ----

    private static ListLegacyMarkup legacy;
    private static ListFixtures fixtures;
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

    private static ListFixtures.Item row(String name, String label) {
        return new ListFixtures.Item(name, label, "", false, null, null);
    }

    private static String children(String name, boolean legacyClasses) {
        String a = name + "-a";
        String b = name + "-b";
        String c = name + "-c";
        switch (name) {
            case "ul-1":
                return fixtures.item(row(a, "Overview"), legacyClasses, true, false);
            case "ul-2":
                return fixtures.item(row(a, "Overview"), legacyClasses, true, false) + fixtures.item(row(b, "Activity"), legacyClasses, true, false);
            case "ul-3":
            case "ol-3":
                return fixtures.item(row(a, "Overview"), legacyClasses, true, false) + fixtures.item(row(b, "Activity"), legacyClasses, true, false)
                        + fixtures.item(row(c, "Settings"), legacyClasses, true, false);
            case "raw-li-3":
                return ListFixtures.rawLi("First raw row", true, a) + ListFixtures.rawLi("Second raw row", true, b) + ListFixtures.rawLi("Third raw row", true, c);
            case "raw-mixed":
                return ListFixtures.rawLi("A raw li", true, a) + ListFixtures.rawDiv("A raw div", true, b) + ListFixtures.rawLi("Another raw li", true, c);
            case "nested": {
                String inner = fixtures.list("nested-inner", false, fixtures.item(row("nested-n1", "Inner one"), legacyClasses, true, false)
                        + fixtures.item(row("nested-n2", "Inner two"), legacyClasses, true, false), legacyClasses, true, "nested-list");
                ListFixtures.Item outer = new ListFixtures.Item("nested-o1", "Outer one", "", false, null, inner);
                return fixtures.item(outer, legacyClasses, true, false) + fixtures.item(row("nested-o2", "Outer two"), legacyClasses, true, false);
            }
            case "empty":
                return "";
            default:
                throw new IllegalArgumentException(name);
        }
    }

    private static String markup(boolean legacyClasses) {
        StringBuilder sb = new StringBuilder();
        for (Layout layout : LAYOUTS) {
            for (String name : CASES) {
                String sibling = layout.siblingStyle().isEmpty() ? "" : "<span style=\"" + layout.siblingStyle() + "\">S</span>";
                sb.append("<div style=\"margin:4px 0\"><div data-layout=\"").append(layout.name()).append("\" style=\"").append(layout.style()).append("\">").append(sibling)
                        .append(fixtures.list(name, name.startsWith("ol"), children(name, legacyClasses), legacyClasses, true, "list")).append("</div></div>");
            }
        }
        return sb.toString();
    }

    // ---- control experiments: each breaks the migrated List in ONE way and must be detected ----

    private record Control(String name, String config, String css, String expectedProperty) {
    }

    private static final List<Control> CONTROL_LIST = List.of(
            new Control("wrong divider colour", "light/none", ":where(.ash-list > :not(:last-child)){border-color:red}", "border-bottom-color"),
            new Control("divider on the last child", "light/none", ".ash-list > :last-child{border-bottom:1px solid red}", "border-bottom-width"),
            new Control("no divider", "light/none", ":where(.ash-list > :not(:last-child)){border-bottom-width:0}", "border-bottom-width"),
            new Control("divider reaches nested descendants", "light/none", ".ash-list :not(:last-child){border-bottom:1px solid red}", "border-bottom-width"),
            new Control("wrong outer border colour", "light/none", ".ash-list{--ash-list-border:red}", "border-top-color"),
            new Control("dark border ignores the theme", "dark/none", ".ash-list{--ash-list-border:var(--color-slate-200);--ash-list-divider:var(--color-slate-200)}", "border-top-color"),
            new Control("colour ignores the skin", "light/civic", ".ash-list{--ash-list-border:#e2e8f0;--ash-list-divider:#e2e8f0}", "border-top-color"),
            new Control("literal radius ignores the skin", "light/axiom", ".ash-list{border-radius:0.5rem}", "border-top-left-radius"),
            new Control("outer border two pixels", "light/none", ".ash-list{border-width:2px}", "border-top-width"),
            new Control("marker comes back", "light/none", ".ash-list{list-style:disc}", "list-style-type"),
            new Control("ordered list shows numerals", "light/none", "ol.ash-list{list-style:decimal}", "list-style-type"),
            new Control("native padding comes back", "light/none", ".ash-list{padding-inline-start:40px}", "padding-left"),
            new Control("native margin comes back", "light/none", ".ash-list{margin-block:16px}", "margin-top"),
            new Control("overflow is visible", "light/none", ".ash-list{overflow:visible}", "overflow-x"));

    // ---- running ----

    @BeforeAll
    static void measure() throws Exception {
        ParitySession session = ParitySession.open("list-parity");
        legacy = new ListLegacyMarkup();
        fixtures = new ListFixtures(legacy, new IconLegacyMarkup());
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
        for (Control control : CONTROL_LIST) {
            frames.append(ParityFrames.frame("ctl:" + control.name() + "|P", ParityConfigs.get(control.config()),
                    ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), control.css(), migrated));
        }
        probe = ParityFrames.measure(session, frames.toString(), EXTRA_PROPS, "list frames");
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
            CONTROLS.put(control.name(), ParityProbe.compare("control: " + control.name(), props, probe.phase(control.config() + "|L"),
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

    /** The values of one property for the rows of a kind in a case and layout, in document order. A list row is named by its case; its rows by the case and a suffix. */
    private static List<String> values(String phase, String kind, String caseName, String layout, String property) {
        return probe.phase(phase).stream().filter(r -> r.kind().equals(kind) && (r.variant().equals(caseName + "/" + layout) || r.variant().startsWith(caseName + "-") && r.variant().endsWith("/" + layout)))
                .map(r -> probe.value(r, property)).toList();
    }

    // ---- tests ----

    @Test
    void theConfigurationsAreTheTwentyTwoThemeAndSkinCombinations() {
        assertEquals(22, CONFIGS.size());
    }

    @Test
    void controlledOracleWithTailwindPresentMatchesTheLegacyListInEveryConfigurationCaseAndLayout() {
        assertEquals(List.of(), lines(PRESENT));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheLegacyListGivenTheSameSurroundings() {
        assertEquals(List.of(), lines(REMOVED_SCOPED));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheFullLegacyRendering() {
        assertEquals(List.of(), lines(REMOVED_FULL));
    }

    @Test
    void theScopedLegacyStylesheetRendersTheLegacyListExactlyAsTheFullOneDoes() {
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
    void theSeparatorsAreOnEveryDirectChildButTheLastAndNeverOnTheLastOrIntoANestedList() {
        for (String phase : List.of("light/none|L", "light/none|P", "light/none|R")) {
            assertEquals(List.of("0px"), values(phase, "item", "ul-1", "block", "border-bottom-width"), phase + " one row");
            assertEquals(List.of("1px", "0px"), values(phase, "item", "ul-2", "block", "border-bottom-width"), phase + " two rows");
            assertEquals(List.of("1px", "1px", "0px"), values(phase, "item", "ul-3", "block", "border-bottom-width"), phase + " three rows");
            assertEquals(List.of("1px", "1px", "0px"), values(phase, "item", "ol-3", "block", "border-bottom-width"), phase + " ordered");
            assertEquals(List.of("1px", "1px", "0px"), values(phase, "raw", "raw-li-3", "block", "border-bottom-width"), phase + " raw li children get the same separators");
            assertEquals(List.of("1px", "1px", "0px"), values(phase, "raw", "raw-mixed", "block", "border-bottom-width"), phase + " a raw div child too");
            // Nested, in document order: outer one (1px), the two inner rows (1px, 0px), outer two (0px). The outer rule does not reach into the inner list.
            assertEquals(List.of("1px", "1px", "0px", "0px"), values(phase, "item", "nested", "block", "border-bottom-width"), phase + " nested");
            assertEquals(List.of("1px", "0px"), values(phase, "item", "nested", "block", "border-bottom-width").subList(1, 3), phase + " inner rows");
        }
    }

    @Test
    void theListStatesItsSurfaceAndTheNativeResetIncludingNoNumeralsForAnOrderedList() {
        for (String phase : List.of("light/none|L", "light/none|P", "light/none|R")) {
            for (String kind : List.of("ul-3", "ol-3")) {
                String key = phase + " " + kind;
                assertEquals(List.of("none"), values(phase, "list", kind, "block", "list-style-type"), key);
                assertEquals(List.of("0px"), values(phase, "list", kind, "block", "margin-top"), key);
                assertEquals(List.of("0px"), values(phase, "list", kind, "block", "padding-left"), key);
                assertEquals(List.of("1px"), values(phase, "list", kind, "block", "border-top-width"), key);
                assertEquals(List.of("hidden"), values(phase, "list", kind, "block", "overflow-x"), key);
                assertEquals(List.of("8px"), values(phase, "list", kind, "block", "border-top-left-radius"), key);
            }
        }
        // The skin-aware radius: Axiom sets --radius-lg to 6px, and the list follows it exactly as rounded-lg did.
        assertEquals(List.of("6px"), values("light/axiom|P", "list", "ul-3", "block", "border-top-left-radius"));
        assertEquals(List.of("6px"), values("light/axiom|L", "list", "ul-3", "block", "border-top-left-radius"));
    }

    @Test
    void theCasesAndLayoutsAreCompleteAndTheSeparatorsAreReallyMeasuredNotAbsent() {
        long listsPerFrame = LAYOUTS.size() * CASES.size();
        assertEquals(listsPerFrame, probe.phase("light/none|L").stream().filter(r -> r.kind().equals("list")).count());
        long separators = probe.phase("light/none|L").stream().filter(r -> (r.kind().equals("item") || r.kind().equals("raw")) && probe.value(r, "border-bottom-width").equals("1px")).count();
        // Per layout: ul-2 has 1, ul-3 2, ol-3 2, raw-li-3 2, raw-mixed 2, nested 2 (outer one, inner one) = 11.
        assertEquals(11L * LAYOUTS.size(), separators, "separators drawn in the legacy oracle");
        assertTrue(ROWS.values().stream().allMatch(n -> n.equals(ROWS.get("light/none|L"))), "every configuration measures the same rows");
    }

    @Test
    void theOracleIsNotVacuousEveryLegacyClassIsCompiledInTheCumulativeAndScopedStylesheets() throws IOException {
        Path dir = Path.of(System.getProperty("ashlar.cascade.dir"));
        String oracle = Files.readString(dir.resolve("list-oracle.css"), StandardCharsets.UTF_8);
        String scoped = Files.readString(dir.resolve("list-legacy-scoped.css"), StandardCharsets.UTF_8);
        List<String> divide = List.of("divide-y", "divide-slate-200", "dark:divide-slate-700");
        assertEquals(List.of(), ParityOracle.uncompiled(oracle, legacy.allTokens().stream().filter(t -> !divide.contains(t)).toList()), "the List oracle does not compile these");
        // divide-* are written by Tailwind as a rule on the list's children (.divide-y>:not(:last-child), with a dark where() in between), so check the class.
        for (String token : divide) {
            String selector = "." + token.replaceAll("([:/.])", "\\\\$1");
            assertTrue(oracle.contains(selector), "the oracle has no rule for " + token);
            assertTrue(scoped.contains(selector), "the scoped stylesheet has no rule for " + token);
        }
        assertEquals(List.of(), ParityOracle.uncompiled(scoped, legacy.allTokens().stream().filter(t -> !divide.contains(t)).toList()), "the scoped stylesheet does not compile these");
        List<String> earlier = new ArrayList<>();
        new BadgeLegacyMarkup().variants().values().forEach(classes -> earlier.addAll(Arrays.asList(classes.split("\\s+"))));
        earlier.addAll(new AlertLegacyMarkup().allTokens());
        earlier.addAll(new DividerLegacyMarkup().allTokens());
        earlier.addAll(new ProgressLegacyMarkup().allTokens());
        earlier.addAll(new EmptyStateLegacyMarkup().allTokens());
        earlier.addAll(new IconLegacyMarkup().allTokens());
        earlier.addAll(new SpinnerLegacyMarkup().allTokens());
        earlier.addAll(new SkeletonLegacyMarkup().allTokens());
        assertEquals(List.of(), ParityOracle.uncompiled(oracle, earlier), "the List oracle lost earlier legacy classes");
        assertFalse(scoped.contains("html,:host"), "the scoped legacy stylesheet must not carry Tailwind's page-wide base");
    }

    @Test
    void normalizationsStayWithinTheApprovedRulesAndTailwindPresentNeedsNone() {
        assertEquals(Map.of(), APPLIED_PRESENT, "Tailwind present needs no normalization at all");
        assertEquals(Map.of(), APPLIED_SCOPED, "the scoped legacy rendering must match the full one without any normalization");
        // Tailwind removed: only H2 (a border style is invisible at width 0; preflight gave solid, the browser gives none). Every legacy border
        // is solid, so each side where the migrated element has no border style at all is exactly one use, in both removed comparisons.
        int props = probe.props().indexOf("border-top-style");
        assertTrue(props >= 0);
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

    private static final Pattern LIST_TAG = Pattern.compile("<(ul|ol) class=\"ash-list\">(.*?)</\\1>", Pattern.DOTALL);

    @Test
    void everyListTheRealPagesServeIsANativeUlOrOlWithOnlyAshlarsClassAndItsRowsAreDirectChildren() throws Exception {
        ParitySession session = ParitySession.open("list-parity");
        HttpClient client = HttpClient.newHttpClient();
        List<String> paths = new ArrayList<>(List.of("/ashlar/", "/ashlar/components"));
        for (ComponentPage page : ComponentCatalog.PAGES) {
            paths.add("/ashlar/components/" + page.slug());
        }
        int lists = 0;
        int ordered = 0;
        int rows = 0;
        Map<String, Integer> perPage = new TreeMap<>();
        List<String> problems = new ArrayList<>();
        try (RunningShowcase app = RunningShowcase.start(session.bootJar())) {
            for (String path : paths) {
                HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + path)).GET().build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                assertEquals(200, response.statusCode(), path);
                String body = response.body();
                Matcher m = LIST_TAG.matcher(body);
                int found = 0;
                while (m.find()) {
                    found++;
                    if (m.group(1).equals("ol")) {
                        ordered++;
                    }
                    // Directly inside the list: only list items (this page projects only List Items); strip them and nothing but whitespace must remain.
                    String rest = m.group(2).replaceAll("(?s)<li class=\"ash-list-item\">.*?</li>", "").trim();
                    int items = (int) Pattern.compile("<li class=\"ash-list-item\">").matcher(m.group(2)).results().count();
                    rows += items;
                    if (!rest.isEmpty()) {
                        problems.add(path + ": unexpected content directly inside a list: " + rest.substring(0, Math.min(80, rest.length())));
                    }
                    if (items == 0) {
                        problems.add(path + ": a list without rows");
                    }
                }
                assertEquals(ListLegacyMarkup.count(body), found, path + ": every ash-list class must be on a recognised list");
                assertEquals(0, count(body, "<ul class=\"divide-y divide-slate-200 overflow-hidden") + count(body, "<ol class=\"divide-y divide-slate-200 overflow-hidden"), path + ": no page still serves the old list classes");
                if (found > 0) {
                    perPage.put(path, found);
                }
                lists += found;
            }
        }
        assertEquals(List.of(), problems);
        assertTrue(perPage.containsKey("/ashlar/components/list"), "the List page renders Lists: " + perPage);
        assertTrue(lists >= 7 && ordered >= 1 && rows >= 18, "lists=" + lists + " ordered=" + ordered + " rows=" + rows);
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }
}
