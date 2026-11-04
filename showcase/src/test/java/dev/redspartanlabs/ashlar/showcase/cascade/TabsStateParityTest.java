package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hover, active and keyboard focus of Nav Link and Sidebar Item, which a measurement of a resting page cannot hold. Chrome is driven through the shared
 * {@link DevToolsSession} (extracted unchanged from {@code ButtonStateParityTest}); the states are FORCED on the real elements, so the same rules run that run
 * for a user: the migrated links' {@code :hover} and {@code :focus-visible} rules next to the legacy links' Tailwind {@code hover:} and {@code focus-visible:} ones,
 * and the skins' masthead rules that key on {@code data-nav-link} (Civic recolours a link, its hover fill and its focus outline there). The device is stated to
 * Chrome as one that can hover (the rules sit inside {@code @media (hover: hover)}, as Tailwind's did).
 *
 * <p>A page is built for each of the 22 theme and skin configurations with the legacy markup (Tailwind oracle), with the migrated markup beside Tailwind, and
 * with the migrated markup alone (Tailwind removed). Every link and current-page item is measured at rest and with each state forced; each state's computed
 * colours, shadow, outline and decoration must equal the legacy element's. The current page, which is text, must not change in any state. Negative controls
 * break the migrated stylesheet in one state and show each gate can fail.
 */
class TabsStateParityTest {

    private static final String ORACLE = ParityFrames.generated("tabs-oracle.css");
    private static final String SCOPED = ParityFrames.generated("tabs-legacy-scoped.css");
    private static final List<String> PROPS = List.of("color", "background-color", "border-bottom-color", "border-top-color", "border-bottom-width", "box-shadow", "outline-style", "outline-width", "outline-color", "text-decoration-line", "cursor", "opacity", "font-weight");
    private static final String NO_MOTION = "*,*::before,*::after{transition:none !important;animation-play-state:paused !important}";

    private record State(String name, List<String> forced) {
    }

    private static final List<State> STATES = List.of(new State("rest", List.of()), new State("hover", List.of("hover")), new State("active", List.of("active")),
            new State("focus-visible", List.of("focus", "focus-visible")), new State("hover+focus-visible", List.of("hover", "focus", "focus-visible")));

    // ---- markup: three tab groups (basic, with a disabled tab, and every tab disabled) and one visible panel ----

    private static String state(String name) {
        return " data-state-name=\"" + name + "\"";
    }

    private static String tab(String group, String id, String label, boolean selected, boolean disabled, String name) {
        return "<button type=\"button\" id=\"" + group + "-tab-" + id + "\" data-tab data-tab-id=\"" + id + "\" role=\"tab\" aria-selected=\"" + selected + "\" aria-controls=\"" + group + "-panel-" + id
                + "\" tabindex=\"" + (selected ? "0" : "-1") + "\"" + (disabled ? " disabled" : "") + " class=\"ash-tabs__tab\"" + state(name) + "><span class=\"ash-tabs__label\">" + label + "</span></button>";
    }

    private static String list(String group, String tabs) {
        return "<div id=\"" + group + "\" data-tabs class=\"ash-tabs\"><div data-tabs-list role=\"tablist\" aria-label=\"" + group + "\" class=\"ash-tabs__list\">" + tabs + "</div></div>";
    }

    private static String markup(boolean legacyMarkup, TabsLegacyMarkup tabs, IconLegacyMarkup icons) {
        String html = list("basic", tab("basic", "overview", "Overview", false, false, "inactive") + tab("basic", "details", "Details", true, false, "selected"))
                + "<div id=\"basic-panel-details\" data-tab-panel data-tabs-id=\"basic\" data-tab-id=\"details\" role=\"tabpanel\" aria-labelledby=\"basic-tab-details\" tabindex=\"0\" class=\"ash-tab-panel\""
                + state("panel") + ">Panel</div>"
                + list("disabled", tab("disabled", "usage", "Usage", true, false, "selected-beside-disabled") + tab("disabled", "invoices", "Invoices", false, true, "disabled"))
                + list("alldisabled", tab("alldisabled", "one", "One", true, true, "selected-disabled") + tab("alldisabled", "two", "Two", false, true, "inactive-disabled"));
        // Every element of the components carries data-parity (the scoped legacy stylesheet\u0027s preflight keys on it).
        html = html.replaceAll("(<[a-z]+ [^>]*class=\"ash-[^\"]*\")", "$1 data-parity=\"state\"");
        return legacyMarkup ? icons.rewrite(tabs.rewrite(html)) : html;
    }

    // ---- measurement ----

    private static Map<String, String> measure(DevToolsSession cdp, int expectedSheets) throws Exception {
        String sanity = cdp.evaluate("(function(){var ok=[].every.call(document.styleSheets,function(s){try{return s.cssRules.length>0}catch(e){return true}});"
                + "return document.styleSheets.length+'|'+ok+'|'+getComputedStyle(document.querySelector('[data-state-name]')).display})()");
        String[] parts = sanity.split("\\|");
        if (Integer.parseInt(parts[0]) != expectedSheets || !parts[1].equals("true") || !parts[2].equals("flex")) {
            throw new IllegalStateException("INFRASTRUCTURE FAILURE, not a parity result: the page did not load its stylesheets: " + sanity + " (expected " + expectedSheets + ")");
        }
        List<Integer> nodes = cdp.nodes("[data-state-name]");
        Map<String, String> values = new LinkedHashMap<>();
        String reader = "(function(){var props=[" + PROPS.stream().map(p -> "'" + p + "'").collect(Collectors.joining(",")) + "];var out=[];"
                + "[].forEach.call(document.querySelectorAll('[data-state-name]'),function(e){var cs=getComputedStyle(e);props.forEach(function(p){out.push(e.getAttribute('data-state-name')+'\\t'+p+'\\t'+cs.getPropertyValue(p))})});"
                + "return out.join('\\n')})()";
        for (State state : STATES) {
            cdp.force(nodes, state.forced());
            for (String line : cdp.evaluate(reader).split("\n")) {
                String[] f = line.split("\t", -1);
                values.put(state.name() + "|" + f[0] + "|" + f[1], f[2]);
            }
            cdp.force(nodes, List.of());
        }
        return values;
    }

    private static String page(ParityConfigs.Config c, List<String> sheets, String extraCss, String body) {
        StringBuilder links = new StringBuilder();
        sheets.forEach(href -> links.append("<link rel=\"stylesheet\" href=\"").append(href).append("\">"));
        String skin = c.skin() == null ? "" : " data-skin=\"" + c.skin() + "\"";
        return "<!doctype html><html data-theme=\"" + c.theme() + "\"" + skin + "><head>" + links + "<style>" + NO_MOTION + extraCss + "</style></head>"
                + "<body style=\"margin:0;padding:8px;width:900px\">" + body + "</body></html>";
    }

    private static boolean same(String property, String legacy, String migrated) {
        return property.equals("box-shadow") ? ParityNormalizer.shadowLayers(legacy).equals(ParityNormalizer.shadowLayers(migrated)) : legacy.equals(migrated);
    }

    private static List<String> differences(String label, Map<String, String> legacy, Map<String, String> migrated) {
        List<String> out = new ArrayList<>();
        if (!legacy.keySet().equals(migrated.keySet())) {
            out.add(label + ": the two pages measured different things");
            return out;
        }
        for (Map.Entry<String, String> e : legacy.entrySet()) {
            String[] key = e.getKey().split("\\|");
            if (!same(key[2], e.getValue(), migrated.get(e.getKey()))) {
                out.add(label + " | " + e.getKey() + " | " + e.getValue() + " -> " + migrated.get(e.getKey()));
            }
        }
        return out;
    }

    // ---- control experiments ----

    private record Control(String name, String config, String css, String state, String expectedProperty, String withoutHook) {
        Control(String name, String config, String css, String state, String expectedProperty) {
            this(name, config, css, state, expectedProperty, "");
        }
    }

    private static final List<Control> CONTROL_LIST = List.of(
            new Control("inactive hover text is wrong", "light/none", "@media (hover:hover){.ash-tabs__tab:hover{color:red}}", "hover", "color"),
            new Control("inactive hover border is wrong", "light/none", "@media (hover:hover){.ash-tabs__tab:hover{border-color:red}}", "hover", "border-bottom-color"),
            new Control("selected hover text is wrong", "light/none", "@media (hover:hover){.ash-tabs__tab[aria-selected=\"true\"]:hover{color:red}}", "hover", "color"),
            new Control("inactive hover is missing", "light/none", ".ash-tabs__tab:not([aria-selected=\"true\"]):not(:disabled):hover{color:var(--color-slate-500);border-color:transparent}", "hover", "border-bottom-color"),
            new Control("a disabled tab changes colour on hover", "light/none", ".ash-tabs__tab:disabled:hover{color:red}", "hover", "color"),
            new Control("a disabled tab gains a border on hover", "light/none", ".ash-tabs__tab:disabled:hover{border-color:red}", "hover", "border-bottom-color"),
            new Control("a selected-disabled tab keeps its border on hover", "light/none", ".ash-tabs__tab:disabled:hover{border-color:var(--color-blue-600)}", "hover", "border-bottom-color"),
            new Control("the focus ring is the wrong colour", "light/none", ".ash-tabs__tab:focus-visible{box-shadow:0 0 0 2px #fff,0 0 0 4px red}", "focus-visible", "box-shadow"),
            new Control("the focus ring has no offset", "light/none", ".ash-tabs__tab:focus-visible{box-shadow:0 0 0 4px var(--color-blue-500)}", "focus-visible", "box-shadow"),
            new Control("the focus ring is missing", "light/none", ".ash-tabs__tab:focus-visible{box-shadow:none}", "focus-visible", "box-shadow"),
            new Control("a focused tab draws an outline", "light/none", ".ash-tabs__tab:focus{outline-style:solid}", "focus-visible", "outline-style"),
            new Control("the panel loses its focus indicator", "light/none", ".ash-tab-panel:focus-visible{outline:none}", "focus-visible", "outline-style"),
            new Control("dark inactive hover text is wrong", "dark/none", "@media (hover:hover){.ash-tabs__tab:hover{color:var(--color-slate-700)}}", "hover", "color"),
            new Control("dark inactive hover border is wrong", "dark/none", "@media (hover:hover){.ash-tabs__tab:hover{border-color:var(--color-slate-300)}}", "hover", "border-bottom-color"),
            new Control("dark selected hover text is wrong", "dark/none", "@media (hover:hover){.ash-tabs__tab[aria-selected=\"true\"]:hover{color:var(--color-blue-700)}}", "hover", "color"));

    // ---- running ----

    private static final Map<String, List<String>> PRESENT = new LinkedHashMap<>();
    private static final Map<String, List<String>> REMOVED = new LinkedHashMap<>();
    private static final Map<String, List<String>> SCOPED_FIDELITY = new LinkedHashMap<>();
    private static final Map<String, List<String>> CONTROLS = new LinkedHashMap<>();
    private static final Map<String, Map<String, String>> LEGACY_VALUES = new LinkedHashMap<>();
    private static int comparisons;

    @BeforeAll
    static void measureEverything() throws Exception {
        ParitySession session = ParitySession.open("navigation-state-parity");
        TabsLegacyMarkup tabs = new TabsLegacyMarkup();
        IconLegacyMarkup icons = new IconLegacyMarkup();
        String migrated = markup(false, tabs, icons);
        String legacy = markup(true, tabs, icons);
        String environment = ParityFrames.environment(session.generatedDir());
        int sheets = 2 + ParityConfigs.SKIN_LINKS.size();
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir()); DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
            for (ParityConfigs.Config c : ParityConfigs.all()) {
                cdp.navigate(server.registerRaw(page(c, ParityFrames.with(ORACLE, ParityConfigs.SKIN_LINKS), "", legacy)));
                Map<String, String> expected = measure(cdp, sheets);
                if (List.of("light/none", "dark/none").contains(c.id())) {
                    LEGACY_VALUES.put(c.id(), expected);
                }
                cdp.navigate(server.registerRaw(page(c, ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), "", migrated)));
                PRESENT.put(c.id(), differences(c.id() + " (Tailwind present)", expected, measure(cdp, sheets)));
                cdp.navigate(server.registerRaw(page(c, ParityFrames.with(ParityFrames.ASHLAR, ParityConfigs.SKIN_LINKS), environment, migrated)));
                REMOVED.put(c.id(), differences(c.id() + " (Tailwind removed)", expected, measure(cdp, sheets)));
                cdp.navigate(server.registerRaw(page(c, ParityFrames.with(SCOPED, ParityConfigs.SKIN_LINKS), environment, legacy)));
                SCOPED_FIDELITY.put(c.id(), differences(c.id() + " (scoped legacy vs full legacy)", expected, measure(cdp, sheets)));
                comparisons += expected.size() * 3;
            }
            for (Control control : CONTROL_LIST) {
                ParityConfigs.Config c = ParityConfigs.get(control.config());
                String body = control.withoutHook().isEmpty() ? migrated : migrated.replace(control.withoutHook(), " data-x");
                cdp.navigate(server.registerRaw(page(c, ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), control.css(), body)));
                CONTROLS.put(control.name(), differences("control: " + control.name(), LEGACY_VALUES.get(c.id()), measure(cdp, sheets)));
            }
        }
    }

    @AfterAll
    static void clear() {
        List.of(PRESENT, REMOVED, SCOPED_FIDELITY, CONTROLS).forEach(Map::clear);
        LEGACY_VALUES.clear();
    }

    private static List<String> flat(Map<String, List<String>> findings) {
        List<String> out = new ArrayList<>();
        long total = 0;
        for (List<String> list : findings.values()) {
            for (String line : list) {
                if (out.size() < 25) {
                    out.add(line);
                }
                total++;
            }
        }
        if (total > out.size()) {
            out.add("(" + (total - out.size()) + " more not shown)");
        }
        return out;
    }

    // ---- tests ----

    @Test
    void everyConfigurationStateAndElementWasMeasuredAtRestAndInEveryForcedState() {
        assertEquals(22, PRESENT.size());
        assertEquals(22, REMOVED.size());
        assertEquals(22, SCOPED_FIDELITY.size());
        // 22 configurations x 5 states x 7 elements x 13 properties, compared three ways.
        assertEquals(22 * STATES.size() * 7 * PROPS.size() * 3, comparisons);
    }

    @Test
    void withTailwindPresentHoverActiveAndFocusVisibleMatchTheLegacyTabsInAllTwentyTwoConfigurations() {
        assertEquals(List.of(), flat(PRESENT));
    }

    @Test
    void withTailwindRemovedHoverActiveAndFocusVisibleMatchTheLegacyTabsInAllTwentyTwoConfigurations() {
        assertEquals(List.of(), flat(REMOVED));
    }

    @Test
    void theScopedLegacyStylesheetBehavesExactlyAsTheFullLegacyOneInEveryState() {
        assertEquals(List.of(), flat(SCOPED_FIDELITY));
    }

    @Test
    void everyNegativeControlIsDetectedInTheStateAndPropertyItBreaks() {
        for (Control control : CONTROL_LIST) {
            List<String> findings = CONTROLS.get(control.name());
            assertFalse(findings.isEmpty(), "the control was NOT detected: " + control.name());
            assertTrue(findings.stream().anyMatch(f -> f.contains("| " + control.state() + "|") && f.contains("|" + control.expectedProperty() + " |")),
                    control.name() + " should be detected in " + control.state() + "/" + control.expectedProperty() + " but was: " + findings.stream().limit(5).toList());
        }
    }

    @Test
    void theLegacyOracleActuallyChangesInEachStateSoTheComparisonIsNotVacuous() {
        Map<String, String> v = LEGACY_VALUES.get("light/none");
        assertNotEquals(v.get("rest|inactive|color"), v.get("hover|inactive|color"), "an inactive tab has a hover ink");
        assertNotEquals(v.get("rest|inactive|border-bottom-color"), v.get("hover|inactive|border-bottom-color"), "and a hover border");
        assertNotEquals(v.get("rest|selected|color"), v.get("hover|selected|color"), "a selected tab has a darker hover ink");
        assertEquals(v.get("rest|selected|border-bottom-color"), v.get("hover|selected|border-bottom-color"), "but keeps its border");
        assertEquals(v.get("rest|disabled|color"), v.get("hover|disabled|color"), "a disabled tab does not change on hover");
        assertEquals(v.get("rest|disabled|border-bottom-color"), v.get("hover|disabled|border-bottom-color"));
        assertEquals("rgba(0, 0, 0, 0)", v.get("hover|selected-disabled|border-bottom-color"), "a selected-disabled tab loses its border on hover");
        assertNotEquals("rgba(0, 0, 0, 0)", v.get("rest|selected-disabled|border-bottom-color"), "but has it at rest");
        assertNotEquals(v.get("rest|inactive|box-shadow"), v.get("focus-visible|inactive|box-shadow"), "a focused tab has a ring");
        assertTrue(v.get("focus-visible|inactive|box-shadow").contains("oklch(0.623 0.214 259.815) 0px 0px 0px 4px"), "a 2px blue-500 ring after a 2px offset: " + v.get("focus-visible|inactive|box-shadow"));
        assertEquals("none", v.get("focus-visible|inactive|outline-style"));
        assertEquals("not-allowed", v.get("rest|disabled|cursor"));
        assertEquals("auto", v.get("focus-visible|panel|outline-style"), "the panel keeps the browser\u0027s own focus indicator");
        assertEquals("none", v.get("rest|panel|outline-style"));
        Map<String, String> dark = LEGACY_VALUES.get("dark/none");
        assertNotEquals(dark.get("hover|inactive|color"), v.get("hover|inactive|color"), "dark mode has its own hover");
        assertNotEquals(dark.get("hover|inactive|border-bottom-color"), v.get("hover|inactive|border-bottom-color"));
        assertEquals(new TreeSet<>(List.of("rest", "hover", "active", "focus-visible", "hover+focus-visible")), new TreeSet<>(STATES.stream().map(State::name).toList()));
    }
}
