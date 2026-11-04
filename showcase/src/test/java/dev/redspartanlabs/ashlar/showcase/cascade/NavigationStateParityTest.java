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
class NavigationStateParityTest {

    private static final String ORACLE = ParityFrames.generated("navigation-oracle.css");
    private static final String SCOPED = ParityFrames.generated("navigation-legacy-scoped.css");
    private static final List<String> PROPS = List.of("background-color", "color", "box-shadow", "outline-style", "outline-color", "text-decoration-line", "font-weight", "opacity",
            "cursor", "border-top-left-radius", "width");
    private static final String NO_MOTION = "*,*::before,*::after{transition:none !important;animation-play-state:paused !important}";

    private record State(String name, List<String> forced) {
    }

    private static final List<State> STATES = List.of(new State("rest", List.of()), new State("hover", List.of("hover")), new State("active", List.of("active")),
            new State("focus-visible", List.of("focus", "focus-visible")), new State("hover+focus-visible", List.of("hover", "focus", "focus-visible")));

    // ---- markup: the masthead (a direct child of body, so the skins' masthead rules apply), the open Drawer's copy of the items, and a Sidebar ----

    private static String state(String name) {
        return " data-state-name=\"" + name + "\"";
    }

    private static String link(String label, boolean current, String name) {
        return current
                ? "<li class=\"ash-nav-link__item\"><span aria-current=\"page\" data-nav-link class=\"ash-nav-link ash-nav-link--current\"" + state(name) + ">" + label + "</span></li>"
                : "<li class=\"ash-nav-link__item\"><a href=\"#\" data-nav-link class=\"ash-nav-link\"" + state(name) + ">" + label + "</a></li>";
    }

    private static String sidebarItem(String label, boolean current, String name) {
        String icon = "<span class=\"ash-sidebar-item__icon\" aria-hidden=\"true\"><svg class=\"ash-icon ash-icon--sm\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" "
                + "stroke-width=\"1.5\" aria-hidden=\"true\"><path d=\"M5 12h14\"/></svg></span>";
        String label2 = "<span class=\"ash-sidebar-item__label\">" + label + "</span>";
        return current
                ? "<li class=\"ash-sidebar-item__entry\"><span aria-current=\"page\" class=\"ash-sidebar-item ash-sidebar-item--current\"" + state(name) + ">" + icon + label2 + "</span></li>"
                : "<li class=\"ash-sidebar-item__entry\"><a href=\"#\" class=\"ash-sidebar-item\"" + state(name) + ">" + icon + label2 + "</a></li>";
    }

    private static String markup(boolean legacyMarkup, NavigationLegacyMarkup navigation, IconLegacyMarkup icons) {
        String html = "<header data-navbar class=\"ash-navbar\"><div class=\"ash-navbar__inner\"><a href=\"#\">Ashlar</a><div class=\"ash-navbar__end\">"
                + "<nav aria-label=\"Primary\" class=\"ash-navbar__nav\"><ul class=\"ash-navbar__list\">" + link("Components", true, "masthead-current") + link("Guides", false, "masthead-link")
                + "</ul></nav></div></div></header>"
                + "<div style=\"box-sizing:border-box;width:280px;margin:8px 0\"><nav aria-label=\"Primary\"><ul class=\"ash-navbar__menu-list\">" + link("Components", true, "drawer-current")
                + link("Guides", false, "drawer-link") + "</ul></nav></div>"
                + "<div style=\"box-sizing:border-box;width:240px;margin:8px 0\"><nav aria-label=\"Sidebar\" data-sidebar class=\"ash-sidebar\"><ul class=\"ash-sidebar__list\">"
                + sidebarItem("Overview", true, "sidebar-current") + sidebarItem("Reports", false, "sidebar-link") + "</ul></nav></div>";
        // Every element of the components carries data-parity (the scoped legacy stylesheet's preflight keys on it); the caller's brand link is raw.
        html = html.replaceAll("(<[a-z]+ [^>]*class=\"ash-[^\"]*\")", "$1 data-parity=\"state\"").replace("<a href=\"#\">Ashlar</a>", "<a href=\"#\" data-parity=\"raw\">Ashlar</a>");
        return legacyMarkup ? icons.rewrite(navigation.rewrite(html)) : html;
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
            new Control("link hover background is wrong", "light/none", "@media (hover:hover){.ash-nav-link:hover{background-color:red}}", "hover", "background-color"),
            new Control("link hover text is wrong", "light/none", "@media (hover:hover){a.ash-nav-link:hover{color:red}}", "hover", "color"),
            new Control("link hover is missing", "light/none", "a.ash-nav-link:hover{background-color:transparent;color:var(--color-slate-600)}", "hover", "background-color"),
            new Control("the current page changes on hover", "light/none", "span.ash-nav-link:hover{background-color:red}", "hover", "background-color"),
            new Control("the focus ring is the wrong colour", "light/none", "a.ash-nav-link:focus-visible{box-shadow:0 0 0 1px #fff,0 0 0 3px red}", "focus-visible", "box-shadow"),
            new Control("the focus ring has no offset", "light/none", "a.ash-nav-link:focus-visible{box-shadow:0 0 0 3px var(--color-blue-500)}", "focus-visible", "box-shadow"),
            new Control("the focus ring is missing", "light/none", "a.ash-nav-link:focus-visible{box-shadow:none}", "focus-visible", "box-shadow"),
            new Control("a focused link draws an outline", "light/none", "a.ash-nav-link:focus{outline-style:solid}", "focus-visible", "outline-style"),
            new Control("dark link hover background is wrong", "dark/none", "@media (hover:hover){a.ash-nav-link:hover{background-color:var(--color-slate-100)}}", "hover", "background-color"),
            new Control("dark link hover text is wrong", "dark/none", "@media (hover:hover){a.ash-nav-link:hover{color:var(--color-slate-900)}}", "hover", "color"),
            new Control("sidebar hover background is wrong", "light/none", "@media (hover:hover){a.ash-sidebar-item:hover{background-color:red}}", "hover", "background-color"),
            new Control("sidebar focus ring is not inset", "light/none", "a.ash-sidebar-item:focus-visible{box-shadow:0 0 0 2px var(--color-blue-500)}", "focus-visible", "box-shadow"),
            new Control("the sidebar current page changes on hover", "light/none", "span.ash-sidebar-item:hover{color:red}", "hover", "color"),
            new Control("sidebar dark hover is wrong", "dark/none", "@media (hover:hover){a.ash-sidebar-item:hover{background-color:var(--color-slate-100)}}", "hover", "background-color"),
            new Control("Civic's masthead hover fill needs data-nav-link", "light/civic", "", "hover", "background-color", " data-nav-link"));

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
        NavigationLegacyMarkup navigation = new NavigationLegacyMarkup();
        IconLegacyMarkup icons = new IconLegacyMarkup();
        String migrated = markup(false, navigation, icons);
        String legacy = markup(true, navigation, icons);
        String environment = ParityFrames.environment(session.generatedDir());
        int sheets = 2 + ParityConfigs.SKIN_LINKS.size();
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir()); DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
            for (ParityConfigs.Config c : ParityConfigs.all()) {
                cdp.navigate(server.registerRaw(page(c, ParityFrames.with(ORACLE, ParityConfigs.SKIN_LINKS), "", legacy)));
                Map<String, String> expected = measure(cdp, sheets);
                if (List.of("light/none", "dark/none", "light/civic").contains(c.id())) {
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
        // 22 configurations x 5 states x 6 elements x 11 properties, compared three ways.
        assertEquals(22 * STATES.size() * 6 * PROPS.size() * 3, comparisons);
    }

    @Test
    void withTailwindPresentHoverActiveAndFocusVisibleMatchTheLegacyNavigationInAllTwentyTwoConfigurations() {
        assertEquals(List.of(), flat(PRESENT));
    }

    @Test
    void withTailwindRemovedHoverActiveAndFocusVisibleMatchTheLegacyNavigationInAllTwentyTwoConfigurations() {
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
        assertNotEquals(v.get("rest|drawer-link|background-color"), v.get("hover|drawer-link|background-color"), "a link has a hover fill");
        assertNotEquals(v.get("rest|drawer-link|color"), v.get("hover|drawer-link|color"), "and a hover ink");
        assertNotEquals(v.get("rest|drawer-link|box-shadow"), v.get("focus-visible|drawer-link|box-shadow"), "and a focus ring");
        assertTrue(v.get("focus-visible|drawer-link|box-shadow").contains("oklch(0.623 0.214 259.815) 0px 0px 0px 3px"), "a 2px blue-500 ring after a 1px offset: " + v.get("focus-visible|drawer-link|box-shadow"));
        assertTrue(v.get("focus-visible|sidebar-link|box-shadow").contains("inset"), "the sidebar ring is inset: " + v.get("focus-visible|sidebar-link|box-shadow"));
        assertEquals("none", v.get("focus-visible|drawer-link|outline-style"));
        assertEquals("none", v.get("rest|drawer-link|text-decoration-line"));
        for (String current : List.of("drawer-current", "masthead-current", "sidebar-current")) {
            for (String property : List.of("background-color", "color")) {
                assertEquals(v.get("rest|" + current + "|" + property), v.get("hover|" + current + "|" + property), current + " is text: it does not change on hover (" + property + ")");
            }
        }
        Map<String, String> dark = LEGACY_VALUES.get("dark/none");
        assertNotEquals(dark.get("hover|drawer-link|background-color"), v.get("hover|drawer-link|background-color"), "dark mode has its own hover");
        Map<String, String> civic = LEGACY_VALUES.get("light/civic");
        assertNotEquals(civic.get("hover|masthead-link|background-color"), v.get("hover|masthead-link|background-color"), "Civic's masthead rules reach the link through data-nav-link");
        assertEquals(new TreeSet<>(List.of("rest", "hover", "active", "focus-visible", "hover+focus-visible")), new TreeSet<>(STATES.stream().map(State::name).toList()));
    }
}
