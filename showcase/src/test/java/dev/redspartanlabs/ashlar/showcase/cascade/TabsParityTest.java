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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The static parity gates of Tabs and Tab Panel (migrated together), measured in real Chrome on the shared parity harness (unchanged). Hover, active and
 * keyboard focus are {@link TabsStateParityTest}; the script (selection, keyboard, focus, reconcile) is {@link TabsScriptTest}.
 *
 * <p>Evidence is a controlled oracle: the components\u0027 legacy Tailwind markup (compiled by the real Tailwind CLI) next to their migrated markup, in the same browser, for
 * each of the 22 theme and skin configurations, with Tailwind present and with Tailwind removed. The legacy markup is the migrated markup rewritten by
 * {@link TabsLegacyMarkup}, so both sides are the same tabs in the same state. The cases are a basic tablist (second tab selected), tabs with icons, tabs with badges, a
 * tablist with a disabled tab beside a selected one, a tablist in which every tab is disabled (so the first, disabled tab is also the selected one), and a tablist of six
 * tabs in a 220px column that wraps; each has its panels, one visible and the rest hidden by the native {@code hidden} attribute.
 *
 * <p>The tabs have no breakpoint, so every configuration is measured at 440px, and the four theme and skin configurations that matter for non-colour properties are
 * also measured at 320 and 900px. Negative controls show each gate can fail.
 *
 * <p>Evidence is Chrome only. Computed-style and box parity is the visual evidence; there is no screenshot comparison.
 */
class TabsParityTest {

    private static final String ORACLE = ParityFrames.generated("tabs-oracle.css");
    private static final String SCOPED = ParityFrames.generated("tabs-legacy-scoped.css");
    private static final List<ParityConfigs.Config> CONFIGS = ParityConfigs.all();
    private static final List<ParityConfigs.Config> EDGE_CONFIGS = ParityConfigs.reduced();
    private static final List<Integer> WIDTHS = List.of(440);
    private static final List<Integer> EDGE_WIDTHS = List.of(320, 900);
    private static final List<String> EXTRA_PROPS = List.of("font-family", "font-size", "font-weight", "line-height", "letter-spacing", "text-transform", "text-align", "text-decoration-line",
            "white-space", "cursor", "appearance", "pointer-events", "transition-duration", "transition-delay", "transition-timing-function", "row-gap", "column-gap", "align-items",
            "justify-content", "flex-wrap", "flex-direction", "outline-style", "background-image", "list-style-type");

    private static final Set<String> CALLER_KINDS = Set.of("raw");

    private static List<ParityProbe.Row> owned(List<ParityProbe.Row> rows) {
        return rows.stream().filter(r -> !CALLER_KINDS.contains(r.kind())).toList();
    }

    private record Tab(String id, String label, boolean disabled, boolean icon, String badge) {
    }

    /** One tab group: its tabs, which one is selected, and the layout it sits in. */
    private record Group(String name, List<Tab> tabs, String selected, String layout) {
    }

    private static Tab t(String id, String label) {
        return new Tab(id, label, false, false, null);
    }

    private static final List<Group> GROUPS = List.of(
            new Group("basic", List.of(t("overview", "Overview"), t("details", "Details"), t("history", "History")), "details", "block"),
            new Group("icons", List.of(new Tab("profile", "Profile", false, true, null), new Tab("settings", "Settings", false, true, null), new Tab("security", "Security", false, true, null)),
                    "profile", "block"),
            new Group("badges", List.of(new Tab("messages", "Messages", false, false, "4"), new Tab("orders", "Orders", false, false, "12"), new Tab("alerts", "Alerts", false, false, "2")),
                    "orders", "block"),
            new Group("disabled", List.of(t("usage", "Usage"), new Tab("invoices", "Invoices", true, false, null), t("settings", "Settings")), "usage", "block"),
            new Group("all-disabled", List.of(new Tab("one", "One", true, false, null), new Tab("two", "Two", true, false, "3"), new Tab("three", "Three", true, true, null)), "one", "block"),
            new Group("wrapped", List.of(t("alpha", "Alpha"), t("bravo", "Bravo"), t("charlie", "Charlie"), t("delta", "Delta"), new Tab("echo", "Echo", false, false, "9"), t("foxtrot", "Foxtrot")),
                    "bravo", "narrow"));

    // ---- state of the measurement ----

    private static TabsLegacyMarkup tabsLegacy;
    private static IconLegacyMarkup iconLegacy;
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
    private static String migratedForCounting;

    // ---- markup (what the two templates render, written out) ----

    private static String mark(String kind, String name) {
        return " data-parity=\"" + kind + "\" data-parity-case=\"" + name + "\"";
    }

    private static String tabButton(Group g, Tab tab) {
        boolean selected = tab.id().equals(g.selected());
        String name = g.name() + "-" + tab.id();
        StringBuilder sb = new StringBuilder("<button type=\"button\" id=\"" + g.name() + "-tab-" + tab.id() + "\" data-tab data-tab-id=\"" + tab.id() + "\" role=\"tab\" aria-selected=\""
                + selected + "\" aria-controls=\"" + g.name() + "-panel-" + tab.id() + "\" tabindex=\"" + (selected ? "0" : "-1") + "\"" + (tab.disabled() ? " disabled" : "")
                + " class=\"ash-tabs__tab\"" + mark("tab", name) + ">");
        if (tab.icon()) {
            sb.append("<svg class=\"ash-icon ash-icon--sm\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" aria-hidden=\"true\"").append(mark("tab-svg", name))
                    .append("><path d=\"M5 12h14\"/></svg>");
        }
        sb.append("<span class=\"ash-tabs__label\"").append(mark("tab-label", name)).append(">").append(tab.label()).append("</span>");
        if (tab.badge() != null) {
            sb.append("<span class=\"ash-tabs__badge\"").append(mark("tab-badge", name)).append(">").append(tab.badge()).append("</span>");
        }
        return sb.append("</button>").toString();
    }

    private static String tabPanel(Group g, Tab tab) {
        boolean active = tab.id().equals(g.selected());
        String name = g.name() + "-" + tab.id();
        return "<div id=\"" + g.name() + "-panel-" + tab.id() + "\" data-tab-panel data-tabs-id=\"" + g.name() + "\" data-tab-id=\"" + tab.id() + "\" role=\"tabpanel\" aria-labelledby=\""
                + g.name() + "-tab-" + tab.id() + "\" tabindex=\"0\"" + (active ? "" : " hidden") + " class=\"ash-tab-panel\"" + mark("tab-panel", name) + ">"
                + "<p style=\"margin:0\"" + mark("raw", name + "-content") + ">Panel " + tab.label() + "</p></div>";
    }

    private static String group(Group g) {
        StringBuilder sb = new StringBuilder("<div id=\"" + g.name() + "\" data-tabs class=\"ash-tabs\"" + mark("tabs", g.name()) + ">");
        sb.append("<div data-tabs-list role=\"tablist\" aria-label=\"").append(g.name()).append("\" class=\"ash-tabs__list\"").append(mark("tabs-list", g.name())).append(">");
        g.tabs().forEach(tab -> sb.append(tabButton(g, tab)));
        sb.append("</div></div>");
        g.tabs().forEach(tab -> sb.append(tabPanel(g, tab)));
        return sb.toString();
    }

    private static String markup(boolean legacyMarkup) {
        StringBuilder sb = new StringBuilder();
        for (Group g : GROUPS) {
            String style = g.layout().equals("narrow") ? "box-sizing:border-box;width:220px" : "box-sizing:border-box";
            sb.append("<div style=\"margin:4px 0\"><div data-layout=\"").append(g.layout()).append("\" style=\"").append(style).append("\">").append(group(g)).append("</div></div>");
        }
        String html = sb.toString();
        return legacyMarkup ? iconLegacy.rewrite(tabsLegacy.rewrite(html)) : html;
    }

    // ---- frames ----

    private static String key(ParityConfigs.Config c, String phase, int width) {
        return c.id() + "|" + phase + "|" + width;
    }

    private static String sized(String frame, int width) {
        return frame.replace("width:440px", "width:" + width + "px");
    }

    private static String frameFor(ParityConfigs.Config c, String phase, int width, String environment, String migrated, String legacyMarkup) {
        String id = key(c, phase, width);
        switch (phase) {
            case "L":
                return sized(ParityFrames.frame(id, c, ParityFrames.with(ORACLE, ParityConfigs.SKIN_LINKS), "", legacyMarkup), width);
            case "P":
                return sized(ParityFrames.frame(id, c, ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), "", migrated), width);
            case "R":
                return sized(ParityFrames.frame(id, c, ParityFrames.with(ParityFrames.ASHLAR, ParityConfigs.SKIN_LINKS), environment, migrated), width);
            case "S":
                return sized(ParityFrames.frame(id, c, ParityFrames.with(SCOPED, ParityConfigs.SKIN_LINKS), environment, legacyMarkup), width);
            default:
                throw new IllegalArgumentException(phase);
        }
    }

    // ---- control experiments: each breaks the migrated tabs in ONE way and must be detected ----

    /** {@code tailwindRemoved}: the control runs beside Ashlar's stylesheet alone (Tailwind's preflight forces [hidden] to display: none !important, so a hidden panel shown cannot show beside it). */
    private record Control(String name, String config, int width, String css, String expectedProperty, boolean tailwindRemoved) {
        Control(String name, String config, int width, String css, String expectedProperty) {
            this(name, config, width, css, expectedProperty, false);
        }
    }

    private static final List<Control> CONTROL_LIST = List.of(
            new Control("tab inline padding is wrong", "light/none", 440, ".ash-tabs__tab{padding-inline:8px}", "padding-left"),
            new Control("tab block padding is wrong", "light/none", 440, ".ash-tabs__tab{padding-block:8px}", "padding-top"),
            new Control("tab text is larger", "light/none", 440, ".ash-tabs__tab{font-size:1rem}", "font-size"),
            new Control("tab weight is wrong", "light/none", 440, ".ash-tabs__tab{font-weight:600}", "font-weight"),
            new Control("tab line height is wrong", "light/none", 440, ".ash-tabs__tab{--ash-tabs-leading:1.4}", "line-height"),
            new Control("tab gap is wrong", "light/none", 440, ".ash-tabs__tab{gap:4px}", "column-gap"),
            new Control("tab border is one pixel", "light/none", 440, ".ash-tabs__tab{border-bottom-width:1px}", "border-bottom-width"),
            new Control("tab transition is slower", "light/none", 440, ".ash-tabs__tab{transition-duration:300ms}", "transition-duration"),
            new Control("tab text colour is wrong", "light/none", 440, ".ash-tabs__tab{--ash-tabs-fg:red}", "color"),
            new Control("tab dark text ignores the theme", "dark/none", 440, ".ash-tabs__tab{--ash-tabs-fg:var(--color-slate-500)}", "color"),
            new Control("the selected border is wrong", "light/none", 440, ".ash-tabs__tab[aria-selected=\"true\"]{--ash-tabs-border:red}", "border-bottom-color"),
            new Control("the selected dark border ignores the theme", "dark/none", 440, ".ash-tabs__tab[aria-selected=\"true\"]{--ash-tabs-border:var(--color-blue-600)}", "border-bottom-color"),
            new Control("the disabled text colour is wrong", "light/none", 440, ".ash-tabs__tab:disabled{--ash-tabs-fg:red}", "color"),
            new Control("the disabled cursor is wrong", "light/none", 440, ".ash-tabs__tab:disabled{cursor:pointer}", "cursor"),
            new Control("the tablist rule is the wrong colour", "light/none", 440, ".ash-tabs{--ash-tabs-rule:red}", "border-bottom-color"),
            new Control("the tablist dark rule ignores the theme", "dark/none", 440, ".ash-tabs{--ash-tabs-rule:var(--color-slate-200)}", "border-bottom-color"),
            new Control("the tablist rule is two pixels", "light/none", 440, ".ash-tabs__list{border-bottom-width:2px}", "border-bottom-width"),
            new Control("the tablist gap is wrong", "light/none", 440, ".ash-tabs__list{gap:0}", "column-gap"),
            new Control("the tablist does not wrap", "light/none", 440, ".ash-tabs__list{flex-wrap:nowrap}", "flex-wrap"),
            new Control("the badge fill is wrong", "light/none", 440, ".ash-tabs{--ash-tabs-badge-bg:red}", "background-color"),
            new Control("the badge dark fill ignores the theme", "dark/none", 440, ".ash-tabs{--ash-tabs-badge-bg:var(--color-slate-100)}", "background-color"),
            new Control("the badge is not a pill", "light/none", 440, ".ash-tabs__badge{border-radius:2px}", "border-top-left-radius"),
            new Control("the badge may be narrower than 1.25rem", "light/none", 440, ".ash-tabs__badge{min-width:0}", "min-width"),
            new Control("the badge padding is wrong", "light/none", 440, ".ash-tabs__badge{padding-inline:0.25rem}", "padding-left"),
            new Control("the badge text is larger", "light/none", 440, ".ash-tabs__badge{font-size:0.875rem}", "font-size"),
            new Control("the tab icon is the wrong size", "light/none", 440, ".ash-tabs__tab .ash-icon{width:20px}", "width"),
            new Control("the panel padding is wrong", "light/none", 440, ".ash-tab-panel{padding-block:8px}", "padding-top"),
            new Control("the panel text colour is wrong", "light/none", 440, ".ash-tab-panel{--ash-tab-panel-fg:red}", "color"),
            new Control("the panel dark text ignores the theme", "dark/none", 440, ".ash-tab-panel{--ash-tab-panel-fg:var(--color-slate-700)}", "color"),
            new Control("the panel text is larger", "light/none", 440, ".ash-tab-panel{font-size:1rem}", "font-size"),
            new Control("a hidden panel is shown", "light/none", 440, ".ash-tab-panel[hidden]{display:block}", "display", true));

    // ---- running ----

    @BeforeAll
    static void measure() throws Exception {
        ParitySession session = ParitySession.open("tabs-parity");
        tabsLegacy = new TabsLegacyMarkup();
        iconLegacy = new IconLegacyMarkup();
        String environment = ParityFrames.environment(session.generatedDir());
        String migrated = markup(false);
        String legacyMarkup = markup(true);
        migratedForCounting = migrated;

        StringBuilder frames = new StringBuilder();
        for (ParityConfigs.Config c : CONFIGS) {
            for (int width : WIDTHS) {
                for (String phase : List.of("L", "P", "R", "S")) {
                    frames.append(frameFor(c, phase, width, environment, migrated, legacyMarkup));
                }
            }
        }
        for (ParityConfigs.Config c : EDGE_CONFIGS) {
            for (int width : EDGE_WIDTHS) {
                for (String phase : List.of("L", "P", "R", "S")) {
                    frames.append(frameFor(c, phase, width, environment, migrated, legacyMarkup));
                }
            }
        }
        for (Control control : CONTROL_LIST) {
            ParityConfigs.Config c = ParityConfigs.get(control.config());
            List<String> sheets = ParityFrames.with(control.tailwindRemoved() ? ParityFrames.ASHLAR : ParityFrames.APP, ParityConfigs.SKIN_LINKS);
            frames.append(sized(ParityFrames.frame("ctl:" + control.name() + "|P", c, sheets, (control.tailwindRemoved() ? environment : "") + control.css(), migrated), control.width()));
        }
        probe = ParityFrames.measure(session, frames.toString(), EXTRA_PROPS, "tabs frames");
        List<String> props = probe.props();
        for (ParityConfigs.Config c : CONFIGS) {
            for (int width : WIDTHS) {
                compareAll(props, c, width);
            }
        }
        for (ParityConfigs.Config c : EDGE_CONFIGS) {
            for (int width : EDGE_WIDTHS) {
                compareAll(props, c, width);
            }
        }
        for (Control control : CONTROL_LIST) {
            ParityConfigs.Config c = ParityConfigs.get(control.config());
            CONTROLS.put(control.name(), ParityProbe.compare("control: " + control.name(), props, probe.phase(key(c, "L", control.width())),
                    probe.phase("ctl:" + control.name() + "|P"), new TreeMap<>()));
        }
    }

    private static void compareAll(List<String> props, ParityConfigs.Config c, int width) {
        String at = c.id() + " @" + width;
        List<ParityProbe.Row> legacyRows = probe.phase(key(c, "L", width));
        ROWS.put(key(c, "L", width), legacyRows.size());
        PRESENT.put(at, ParityProbe.compare(at + " (Tailwind present)", props, legacyRows, probe.phase(key(c, "P", width)), APPLIED_PRESENT));
        REMOVED_SCOPED.put(at, ParityProbe.compare(at + " (Tailwind removed vs scoped legacy)", props, probe.phase(key(c, "S", width)), probe.phase(key(c, "R", width)), APPLIED_REMOVED));
        REMOVED_FULL.put(at, ParityProbe.compare(at + " (Tailwind removed vs full legacy, the components\u0027 own elements)", props, owned(legacyRows), owned(probe.phase(key(c, "R", width))), APPLIED_REMOVED));
        SCOPED_FIDELITY.put(at, ParityProbe.compare(at + " (scoped legacy vs full legacy, the components\u0027 own elements)", props, owned(legacyRows), owned(probe.phase(key(c, "S", width))), APPLIED_SCOPED));
    }

    /** The measurements are tens of thousands of rows; every suite in this JVM is run one after another, so this one lets go of them when it is done. */
    @AfterAll
    static void clear() {
        probe = null;
        migratedForCounting = null;
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

    private static String layoutOf(String groupName) {
        return GROUPS.stream().filter(g -> g.name().equals(groupName)).findFirst().orElseThrow().layout();
    }

    /** One measured value: kind, group, tab id (or null for the group), property. */
    private static String one(String phase, String kind, String group, String tabId, String property) {
        String caseName = tabId == null ? group : group + "-" + tabId;
        List<ParityProbe.Row> found = probe.phase(phase).stream().filter(r -> r.kind().equals(kind) && r.variant().equals(caseName + "/" + layoutOf(group))).toList();
        assertEquals(1, found.size(), phase + " " + kind + " " + caseName);
        return probe.value(found.get(0), property);
    }

    private static double number(String px) {
        return Double.parseDouble(px.replace("px", ""));
    }

    private static final List<String> RENDERINGS = List.of("light/none|L|440", "light/none|P|440", "light/none|R|440", "light/none|S|440");

    // ---- tests ----

    @Test
    void theConfigurationsAreTheTwentyTwoThemeAndSkinCombinationsAndTheWidthsAreMeasured() {
        assertEquals(22, CONFIGS.size());
        assertEquals(4, EDGE_CONFIGS.size());
        assertEquals(22 * WIDTHS.size() + 4 * EDGE_WIDTHS.size(), PRESENT.size());
    }

    @Test
    void controlledOracleWithTailwindPresentMatchesTheLegacyTabsInEveryConfigurationCaseAndWidth() {
        assertEquals(List.of(), lines(PRESENT));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheLegacyTabsGivenTheSameSurroundings() {
        assertEquals(List.of(), lines(REMOVED_SCOPED));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheFullLegacyRendering() {
        assertEquals(List.of(), lines(REMOVED_FULL));
    }

    @Test
    void theScopedLegacyStylesheetRendersTheLegacyTabsExactlyAsTheFullOneDoes() {
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
    void aTabHasTailwindsSizeSpacingBorderAndTypeInBothRenderings() {
        for (String phase : RENDERINGS) {
            assertEquals("flex", one(phase, "tab", "basic", "overview", "display"), phase + ": an inline-flex button, blockified as a flex item");
            assertEquals("12px", one(phase, "tab", "basic", "overview", "padding-left"), phase);
            assertEquals("10px", one(phase, "tab", "basic", "overview", "padding-top"), phase);
            assertEquals("8px", one(phase, "tab", "basic", "overview", "column-gap"), phase);
            assertEquals("14px", one(phase, "tab", "basic", "overview", "font-size"), phase);
            assertEquals("20px", one(phase, "tab", "basic", "overview", "line-height"), phase);
            assertEquals("500", one(phase, "tab", "basic", "overview", "font-weight"), phase);
            assertEquals("2px", one(phase, "tab", "basic", "overview", "border-bottom-width"), phase);
            assertEquals("0px", one(phase, "tab", "basic", "overview", "border-top-width"), phase);
            assertEquals("0px", one(phase, "tab", "basic", "overview", "border-top-left-radius"), phase);
            assertEquals("rgba(0, 0, 0, 0)", one(phase, "tab", "basic", "overview", "background-color"), phase + ": a button has no fill without preflight");
            assertEquals("1px", one(phase, "tabs-list", "basic", null, "border-bottom-width"), phase);
            assertEquals("4px", one(phase, "tabs-list", "basic", null, "column-gap"), phase);
            assertEquals("wrap", one(phase, "tabs-list", "basic", null, "flex-wrap"), phase);
            assertEquals("42", one(phase, "tab", "basic", "overview", "@h"), phase + ": 10 + 20 + 10 of content and padding, and the 2px border");
        }
    }

    @Test
    void theSelectedTabIsBlueWithABlueBorderAndTheInactiveOnesAreSlateWithNone() {
        for (String phase : RENDERINGS) {
            assertNotEquals(one(phase, "tab", "basic", "details", "color"), one(phase, "tab", "basic", "overview", "color"), phase);
            assertEquals("rgba(0, 0, 0, 0)", one(phase, "tab", "basic", "overview", "border-bottom-color"), phase + ": an inactive tab\u0027s border is transparent");
            assertNotEquals("rgba(0, 0, 0, 0)", one(phase, "tab", "basic", "details", "border-bottom-color"), phase + ": the selected tab\u0027s is not");
            assertEquals(one(phase, "tab", "basic", "details", "color"), one(phase, "tab", "basic", "details", "border-bottom-color"), phase + ": text and border are the same blue");
        }
    }

    @Test
    void aDisabledTabIsMutedWithTheNotAllowedCursorAndASelectedDisabledTabKeepsItsBorder() {
        for (String phase : RENDERINGS) {
            assertEquals("not-allowed", one(phase, "tab", "disabled", "invoices", "cursor"), phase);
            assertNotEquals("not-allowed", one(phase, "tab", "disabled", "usage", "cursor"), phase);
            assertNotEquals(one(phase, "tab", "disabled", "settings", "color"), one(phase, "tab", "disabled", "invoices", "color"), phase + ": disabled is lighter than inactive");
            // Every tab disabled: the first is also the selected one. Its text is the disabled colour, its border the selected blue.
            assertEquals(one(phase, "tab", "all-disabled", "two", "color"), one(phase, "tab", "all-disabled", "one", "color"), phase + ": text is the disabled colour");
            assertNotEquals("rgba(0, 0, 0, 0)", one(phase, "tab", "all-disabled", "one", "border-bottom-color"), phase + ": the selected-disabled tab keeps its border");
            assertEquals("rgba(0, 0, 0, 0)", one(phase, "tab", "all-disabled", "two", "border-bottom-color"), phase);
            assertEquals("not-allowed", one(phase, "tab", "all-disabled", "one", "cursor"), phase);
        }
    }

    @Test
    void anIconIsOneRemAndABadgeIsAPillWithItsMinimumWidthAndTheLabelIsTheTabsText() {
        for (String phase : RENDERINGS) {
            assertEquals("16", one(phase, "tab-svg", "icons", "profile", "@w"), phase);
            assertTrue(number(one(phase, "tab-svg", "icons", "profile", "@x")) < number(one(phase, "tab-label", "icons", "profile", "@x")), phase + ": the icon leads");
            assertEquals("20", one(phase, "tab-badge", "badges", "alerts", "@w"), phase + ": min-w-5 (a one-character count)");
            assertEquals("12px", one(phase, "tab-badge", "badges", "orders", "font-size"), phase);
            assertEquals("500", one(phase, "tab-badge", "badges", "orders", "font-weight"), phase);
            assertEquals("6px", one(phase, "tab-badge", "badges", "orders", "padding-left"), phase);
            assertEquals("2px", one(phase, "tab-badge", "badges", "orders", "padding-top"), phase);
            assertEquals("flex", one(phase, "tab-badge", "badges", "orders", "display"), phase + ": an inline-flex pill, blockified as a flex item");
        }
    }

    @Test
    void aNarrowColumnWrapsTheTablistAcrossRowsWithTheListGapBetweenThem() {
        for (String phase : RENDERINGS) {
            Set<String> tops = new TreeSet<>();
            for (String id : List.of("alpha", "bravo", "charlie", "delta", "echo", "foxtrot")) {
                tops.add(one(phase, "tab", "wrapped", id, "@y"));
            }
            assertTrue(tops.size() >= 2, phase + ": six tabs wrap in 220px: " + tops);
            assertEquals("220", one(phase, "tabs-list", "wrapped", null, "@w"), phase + ": the list keeps its container\u0027s width");
        }
    }

    @Test
    void thePanelsAreOneVisibleAndTheRestHiddenWithTailwindsPaddingAndType() {
        for (String phase : RENDERINGS) {
            assertEquals("block", one(phase, "tab-panel", "basic", "details", "display"), phase);
            assertEquals("none", one(phase, "tab-panel", "basic", "overview", "display"), phase);
            assertEquals("none", one(phase, "tab-panel", "basic", "history", "display"), phase);
            assertEquals("16px", one(phase, "tab-panel", "basic", "details", "padding-top"), phase);
            assertEquals("14px", one(phase, "tab-panel", "basic", "details", "font-size"), phase);
            assertEquals("20px", one(phase, "tab-panel", "basic", "details", "line-height"), phase);
            assertEquals("none", one(phase, "tab-panel", "all-disabled", "two", "display"), phase);
        }
    }

    @Test
    void darkModeUsesTailwindsDarkColoursInBothRenderings() {
        for (String render : List.of("L", "P", "R")) {
            String light = "light/none|" + render + "|440";
            String dark = "dark/none|" + render + "|440";
            for (String[] part : new String[][] {{"tab", "basic", "overview", "color"}, {"tab", "basic", "details", "color"}, {"tab", "basic", "details", "border-bottom-color"},
                    {"tab", "disabled", "invoices", "color"}, {"tabs-list", "basic", null, "border-bottom-color"}, {"tab-badge", "badges", "orders", "background-color"},
                    {"tab-badge", "badges", "orders", "color"}, {"tab-panel", "basic", "details", "color"}}) {
                assertNotEquals(one(light, part[0], part[1], part[2], part[3]), one(dark, part[0], part[1], part[2], part[3]), render + " " + part[0] + " " + part[3]);
            }
        }
    }

    @Test
    void theCasesAreCompleteAndEveryPartIsMeasured() {
        List<ParityProbe.Row> rows = probe.phase("light/none|L|440");
        Map<String, Integer> byKind = new TreeMap<>();
        rows.forEach(r -> byKind.merge(r.kind(), 1, Integer::sum));
        for (String kind : List.of("tabs", "tabs-list", "tab", "tab-label", "tab-svg", "tab-badge", "tab-panel")) {
            int written = count(migratedForCounting, "data-parity=\"" + kind + "\"");
            assertTrue(written > 0, kind);
            assertEquals(written, byKind.get(kind), kind + " rows");
        }
        assertEquals(GROUPS.size(), byKind.get("tabs"));
        assertEquals(GROUPS.stream().mapToInt(g -> g.tabs().size()).sum(), byKind.get("tab"));
        assertEquals(byKind.get("tab"), byKind.get("tab-panel"), "one panel per tab");
        for (String key : ROWS.keySet().stream().filter(k -> k.startsWith("light/none|L|")).toList()) {
            assertEquals(ROWS.get("light/none|L|440"), ROWS.get(key), "every width measures the same elements: " + key);
        }
    }

    @Test
    void theOracleIsNotVacuousEveryLegacyClassIsCompiledInTheCumulativeAndScopedStylesheets() throws IOException {
        Path dir = Path.of(System.getProperty("ashlar.cascade.dir"));
        String oracle = Files.readString(dir.resolve("tabs-oracle.css"), StandardCharsets.UTF_8);
        String scoped = Files.readString(dir.resolve("tabs-legacy-scoped.css"), StandardCharsets.UTF_8);
        List<String> tokens = new ArrayList<>(tabsLegacy.allTokens());
        // ParityOracle recognises a class selector followed by a block, a pseudo-class or a comma; Tailwind writes a few of these differently (a decimal class, a
        // attribute-selector variant), so those are looked up by their escaped selector.
        List<String> plain = tokens.stream().filter(t -> !t.startsWith("aria-") && !t.contains(".")).toList();
        assertEquals(List.of(), ParityOracle.uncompiled(oracle, plain), "the oracle does not compile these");
        assertEquals(List.of(), ParityOracle.uncompiled(scoped, plain), "the scoped stylesheet does not compile these");
        assertTrue(oracle.contains("py-2\\.5") && scoped.contains("py-2\\.5") && oracle.contains("px-1\\.5") && scoped.contains("px-1\\.5"), "the decimal spacing classes are compiled");
        assertTrue(scoped.contains("disabled\\:hover\\:border-transparent") && scoped.contains("dark\\:disabled\\:hover\\:text-slate-600"), "the disabled variants are compiled");
        List<String> earlier = new ArrayList<>();
        earlier.addAll(new NavigationLegacyMarkup().allTokens().stream().filter(t -> !t.contains("[")).toList());
        earlier.addAll(new CardLegacyMarkup().allTokens());
        earlier.addAll(new IconLegacyMarkup().allTokens());
        assertEquals(List.of(), ParityOracle.uncompiled(oracle, earlier), "the oracle lost earlier legacy classes");
        assertFalse(scoped.contains("html,:host"), "the scoped legacy stylesheet must not carry Tailwind\u0027s page-wide base");
    }

    @Test
    void normalizationsStayWithinTheApprovedRulesAndTailwindPresentNeedsNone() {
        // The badge is a pill: Tailwind's rounded-full computes to 3.35544e+07px and Ashlar's conventional 9999px to 9999px. They render the same, and the harness's H4 rule
        // (a radius at least half the shorter side is a full pill) is the approved way to compare them. It is the only rule needed, and only for the badge.
        assertTrue(Set.of("H4").containsAll(APPLIED_PRESENT.keySet()), "unexpected normalization rule used with Tailwind present: " + APPLIED_PRESENT);
        assertTrue(APPLIED_PRESENT.getOrDefault("H4", 0) > 0, "the badge radius needs H4: " + APPLIED_PRESENT);
        assertEquals(Map.of(), APPLIED_SCOPED, "the scoped legacy rendering must match the full one without any normalization");
        assertTrue(Set.of("H2", "H4").containsAll(APPLIED_REMOVED.keySet()), "unexpected normalization rule used: " + APPLIED_REMOVED);
    }

    // ---- what the real pages serve ----

    private static final Pattern TAILWIND_ON_A_HOOK = Pattern.compile("class=\"[^\"]*(?:border-b-2|text-blue-600|border-slate-200|py-4 text-sm|rounded-full)[^\"]*\"\\s*(?:data-tabs|data-tab-panel|data-tabs-list|role=\"tab)");

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }

    @Test
    void everyTabAndPanelTheRealPagesServeKeepsItsNativeStructureRolesAndPairingAndNoClassStringAttributes() throws Exception {
        ParitySession session = ParitySession.open("tabs-parity");
        HttpClient client = HttpClient.newHttpClient();
        List<String> paths = new ArrayList<>(List.of("/ashlar/", "/ashlar/components"));
        for (ComponentPage page : ComponentCatalog.PAGES) {
            paths.add("/ashlar/components/" + page.slug());
        }
        Map<String, Integer> totals = new TreeMap<>();
        List<String> problems = new ArrayList<>();
        try (RunningShowcase app = RunningShowcase.start(session.bootJar())) {
            for (String path : paths) {
                HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + path)).GET().build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                assertEquals(200, response.statusCode(), path);
                String body = response.body();
                for (String role : TabsLegacyMarkup.MIGRATED.keySet()) {
                    totals.merge(role, TabsLegacyMarkup.count(body, role), Integer::sum);
                }
                tabsLegacy.rewrite(body);
                int tabs = TabsLegacyMarkup.count(body, "tabs");
                int tab = TabsLegacyMarkup.count(body, "tab");
                int panels = TabsLegacyMarkup.count(body, "tab-panel");
                // Counted as tags: the documentation also mentions these attributes in prose.
                long roots = Pattern.compile("<div id=\"[^\"]+\" data-tabs class=\"ash-tabs\">").matcher(body).results().count();
                long lists = Pattern.compile("<div\\s+data-tabs-list\\s+role=\"tablist\"[^>]*class=\"ash-tabs__list\"").matcher(body).results().count();
                long tabTags = Pattern.compile("<button\\b[^>]*\\brole=\"tab\"[^>]*class=\"ash-tabs__tab\"").matcher(body).results().count();
                long panelTags = Pattern.compile("<div\\b[^>]*\\brole=\"tabpanel\"[^>]*class=\"ash-tab-panel\"").matcher(body).results().count();
                if (roots != tabs || lists != TabsLegacyMarkup.count(body, "tabs-list") || lists != tabs) {
                    int at = body.indexOf("data-tabs-list");
                    problems.add(path + ": data-tabs / the tablist are not on exactly the Tabs and their lists: " + roots + "/" + lists + "/" + tabs + " near: " + body.substring(Math.max(0, at - 120), Math.min(body.length(), at + 700)).replace("\n", " "));
                }
                if (tabTags != tab || panelTags != panels) {
                    problems.add(path + ": role=tab / role=tabpanel tags differ from the tabs and panels: " + tabTags + "/" + tab + ", " + panelTags + "/" + panels);
                }
                if (TabsLegacyMarkup.count(body, "tab-label") != tab) {
                    problems.add(path + ": every tab has one label");
                }
                if (body.contains("data-tab-active-classes") || body.contains("data-tab-inactive-classes")) {
                    problems.add(path + ": a tab still renders a class-string attribute");
                }
                if (TAILWIND_ON_A_HOOK.matcher(body).find()) {
                    problems.add(path + ": a tab or panel still renders its Tailwind classes");
                }
            }
        }
        assertEquals(List.of(), problems);
        for (String role : TabsLegacyMarkup.MIGRATED.keySet()) {
            assertTrue(totals.getOrDefault(role, 0) > 0, "no " + role + " rendered on any page: " + totals);
        }
        assertEquals(totals.get("tab"), totals.get("tab-label"));
    }
}
