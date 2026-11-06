package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hover, active and keyboard focus of Tree and Tree Item, which a measurement of a resting page cannot hold. Chrome is driven through the shared {@link DevToolsSession}; the
 * states are FORCED on the real elements, so the same rules run that run for a user: the migrated row's {@code :hover} and {@code li:focus-visible > row} rules next to the
 * legacy row's Tailwind {@code hover:} and {@code [li:focus-visible>&]:} ones. The device is stated to Chrome as one that can hover (the rules sit inside
 * {@code @media (hover: hover)}, as Tailwind's did; {@link TreeHoverMediaTest} shows they apply to nothing on a device that cannot).
 *
 * <p>One tree is measured: an expanded parent (with an icon) holding an expanded parent, which holds a link leaf and a collapsed parent with a leaf, then a plain leaf. A page
 * is built for each of the 22 theme and skin configurations with the legacy markup (Tailwind oracle), with the migrated markup beside Tailwind, and with the migrated markup
 * alone (Tailwind removed). Every element is measured at rest and in each scenario; each scenario's computed colours, fill, shadow, outline, underline, chevron turn and panel
 * row must equal the legacy element's. The scenarios include the nesting ones: focus-visible on a nested item must style that item's row and no other, and hovering a nested
 * link must not light the rows of the items above it. The two state-keyed rules (the chevron turn on the item's {@code aria-expanded} and the collapsed row on the panel's
 * {@code inert}) are measured next to their legacy inline-style and class equivalents. Negative controls break the migrated stylesheet in one state and show each gate can fail.
 */
class TreeStateParityTest {

    private static final String ORACLE = ParityFrames.generated("tree-oracle.css");
    private static final String SCOPED = ParityFrames.generated("tree-legacy-scoped.css");
    private static final List<String> PROPS = List.of("color", "background-color", "box-shadow", "outline-style", "outline-width", "outline-color", "text-decoration-line", "text-decoration-color",
            "cursor", "opacity", "font-weight", "rotate", "grid-template-rows", "height");
    private static final String NO_MOTION = "*,*::before,*::after{transition:none !important;animation-play-state:paused !important}";

    private static final List<String> ELEMENTS = List.of(
            "a-li", "a-row", "a-toggle", "a-chevron", "a-icon", "a-label", "a-panel",
            "b-li", "b-row", "b-toggle", "b-chevron", "b-label", "b-panel",
            "c-li", "c-row", "c-spacer", "c-label",
            "d-li", "d-row", "d-toggle", "d-chevron", "d-label", "d-panel",
            "e-li", "e-row", "e-spacer", "e-label",
            "f-li", "f-row", "f-spacer", "f-label");

    private static final List<String> ALL_FOCUS = List.of("focus", "focus-visible");

    /** What is forced where: the element names and the pseudo-classes. */
    private record Scenario(String name, List<String> elements, List<String> forced) {
    }

    private static final List<Scenario> SCENARIOS = List.of(
            new Scenario("rest", List.of(), List.of()),
            new Scenario("hover-all", ELEMENTS, List.of("hover")),
            new Scenario("active-all", ELEMENTS, List.of("active")),
            new Scenario("focus-visible-all", ELEMENTS, ALL_FOCUS),
            new Scenario("hover+focus-visible-all", ELEMENTS, List.of("hover", "focus", "focus-visible")),
            new Scenario("li-a-focus-visible", List.of("a-li"), ALL_FOCUS),
            new Scenario("li-b-focus-visible", List.of("b-li"), ALL_FOCUS),
            new Scenario("li-c-focus-visible", List.of("c-li"), ALL_FOCUS),
            new Scenario("li-d-focus-visible", List.of("d-li"), ALL_FOCUS),
            new Scenario("link-c-focus", List.of("c-label"), ALL_FOCUS),
            new Scenario("row-a-hover", List.of("a-row"), List.of("hover")),
            new Scenario("row-b-hover", List.of("b-row"), List.of("hover")),
            new Scenario("toggle-a-hover", List.of("a-toggle", "a-row", "a-li"), List.of("hover")),
            new Scenario("toggle-d-hover", List.of("d-toggle", "d-row", "d-li", "b-panel", "b-li", "a-panel", "a-li"), List.of("hover")),
            new Scenario("link-c-hover", List.of("c-label", "c-row", "c-li", "b-panel", "b-li", "a-panel", "a-li"), List.of("hover")),
            new Scenario("li-b-focus-visible+row-b-hover", List.of("b-li", "b-row"), List.of("hover", "focus", "focus-visible")),
            new Scenario("li-b-focus-visible+row-a-hover", List.of("b-li", "a-row"), List.of("hover", "focus", "focus-visible")));

    // ---- markup: one tree with an expanded parent holding an expanded parent, a link leaf, a collapsed parent and a leaf ----

    private static final String SVG = "<svg class=\"ash-icon ash-icon--sm\" viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M9 5l7 7-7 7\"/></svg>";

    private static String n(String name) {
        return " data-state-name=\"" + name + "\"";
    }

    private static String li(String id, int level, Boolean expanded, String inner) {
        return "<li role=\"treeitem\"" + (expanded == null ? "" : " aria-expanded=\"" + expanded + "\"") + " aria-level=\"" + level + "\" tabindex=\"-1\" data-tree-item class=\"ash-tree__item\"" + n(id + "-li") + ">" + inner + "</li>";
    }

    private static String row(String id, boolean parent, boolean icon, boolean link, String label) {
        StringBuilder sb = new StringBuilder("<div class=\"ash-tree__row\"" + n(id + "-row") + ">");
        if (parent) {
            sb.append("<button type=\"button\" tabindex=\"-1\" aria-hidden=\"true\" data-tree-toggle class=\"ash-tree__toggle\"").append(n(id + "-toggle")).append(">")
                    .append("<span data-tree-chevron class=\"ash-tree__chevron\"").append(n(id + "-chevron")).append(">").append(SVG).append("</span></button>");
        } else {
            sb.append("<span class=\"ash-tree__spacer\" aria-hidden=\"true\"").append(n(id + "-spacer")).append("></span>");
        }
        if (icon) {
            sb.append("<span class=\"ash-tree__icon\" aria-hidden=\"true\"").append(n(id + "-icon")).append(">").append(SVG).append("</span>");
        }
        sb.append(link ? "<a href=\"#" + id + "\" tabindex=\"-1\" class=\"ash-tree__label\"" + n(id + "-label") + ">" + label + "</a>"
                : "<span class=\"ash-tree__label\"" + n(id + "-label") + ">" + label + "</span>");
        return sb.append("</div>").toString();
    }

    private static String panel(String id, boolean expanded, String children) {
        return "<div data-tree-children" + (expanded ? "" : " inert") + " class=\"ash-tree__panel\"" + n(id + "-panel") + "><div class=\"ash-tree__clip\"><ul role=\"group\" class=\"ash-tree__group\">"
                + children + "</ul></div></div>";
    }

    private static String markup(boolean legacyMarkup, TreeLegacyMarkup tree, IconLegacyMarkup icons) {
        String e = li("e", 4, null, row("e", false, false, false, "Epsilon"));
        String d = li("d", 3, false, row("d", true, false, false, "Delta") + panel("d", false, e));
        String c = li("c", 3, null, row("c", false, false, true, "Gamma link"));
        String b = li("b", 2, true, row("b", true, false, false, "Beta") + panel("b", true, c + d));
        String f = li("f", 2, null, row("f", false, false, false, "Zeta"));
        String a = li("a", 1, true, row("a", true, true, false, "Alpha") + panel("a", true, b + f));
        String html = "<ul id=\"t\" role=\"tree\" data-tree class=\"ash-tree\">" + a + "</ul>";
        // Every element of the components carries data-parity (the scoped legacy stylesheet's preflight keys on it).
        html = html.replaceAll("(<[a-z0-9]+ [^>]*class=\"ash-[^\"]*\")", "$1 data-parity=\"state\"");
        return legacyMarkup ? icons.rewrite(tree.rewrite(html)) : html;
    }

    // ---- measurement ----

    private static Map<String, String> measure(DevToolsSession cdp, int expectedSheets) throws Exception {
        String sanity = cdp.evaluate("(function(){var ok=[].every.call(document.styleSheets,function(s){try{return s.cssRules.length>0}catch(e){return true}});"
                + "return document.styleSheets.length+'|'+ok+'|'+getComputedStyle(document.querySelector('[data-state-name=\"a-row\"]')).display})()");
        String[] parts = sanity.split("\\|");
        if (Integer.parseInt(parts[0]) != expectedSheets || !parts[1].equals("true") || !parts[2].equals("flex")) {
            throw new IllegalStateException("INFRASTRUCTURE FAILURE, not a parity result: the page did not load its stylesheets: " + sanity + " (expected " + expectedSheets + ")");
        }
        // One query, so every node id is from the same document snapshot; the names are read in the same (document) order and zipped with the ids.
        List<Integer> nodeIds = cdp.nodes("[data-state-name]");
        String[] names = cdp.evaluate("[].map.call(document.querySelectorAll('[data-state-name]'),function(e){return e.getAttribute('data-state-name')}).join(',')").split(",");
        if (names.length != ELEMENTS.size() || nodeIds.size() != ELEMENTS.size() || !new java.util.HashSet<>(List.of(names)).equals(new java.util.HashSet<>(ELEMENTS))) {
            throw new IllegalStateException("INFRASTRUCTURE FAILURE: expected " + ELEMENTS.size() + " measured elements, found " + nodeIds.size() + " nodes and names " + List.of(names));
        }
        Map<String, Integer> ids = new LinkedHashMap<>();
        for (int i = 0; i < names.length; i++) {
            ids.put(names[i], nodeIds.get(i));
        }
        Map<String, String> values = new LinkedHashMap<>();
        String reader = "(function(){var props=[" + PROPS.stream().map(p -> "'" + p + "'").collect(Collectors.joining(",")) + "];var out=[];"
                + "[].forEach.call(document.querySelectorAll('[data-state-name]'),function(e){var cs=getComputedStyle(e);props.forEach(function(p){out.push(e.getAttribute('data-state-name')+'\\t'+p+'\\t'+cs.getPropertyValue(p))})});"
                + "return out.join('\\n')})()";
        for (Scenario scenario : SCENARIOS) {
            List<Integer> nodes = scenario.elements().stream().map(ids::get).toList();
            if (!nodes.isEmpty()) {
                cdp.force(nodes, scenario.forced());
            }
            for (String line : cdp.evaluate(reader).split("\n")) {
                String[] f = line.split("\t", -1);
                values.put(scenario.name() + "|" + f[0] + "|" + f[1], f[2]);
            }
            if (!nodes.isEmpty()) {
                cdp.force(nodes, List.of());
            }
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

    private record Control(String name, String config, String css, String scenario, String expectedProperty) {
    }

    private static final String TURN = ".ash-tree__item[aria-expanded=\"true\"] > .ash-tree__row > .ash-tree__toggle > .ash-tree__chevron";
    private static final String FOCUSED = ".ash-tree__item:focus-visible > .ash-tree__row";

    private static final List<Control> CONTROL_LIST = List.of(
            new Control("the hover fill is the wrong colour", "light/none", "@media (hover:hover){.ash-tree__row:hover{background-color:red}}", "row-a-hover", "background-color"),
            new Control("the hover fill is missing", "light/none", ".ash-tree__row:hover{background-color:transparent}", "row-a-hover", "background-color"),
            new Control("the hover fill is also at rest", "light/none", ".ash-tree__row{background-color:var(--color-slate-100)}", "rest", "background-color"),
            new Control("the dark hover fill ignores the theme", "dark/none", "@media (hover:hover){.ash-tree__row:hover{background-color:var(--color-slate-100)}}", "row-a-hover", "background-color"),
            new Control("hovering a row changes its text colour", "light/none", "@media (hover:hover){.ash-tree__row:hover{color:red}}", "row-a-hover", "color"),
            new Control("hovering a nested link lights the row above it", "light/none", "@media (hover:hover){.ash-tree__item:hover > .ash-tree__row{background-color:var(--color-slate-100)}}", "link-c-hover", "background-color"),
            new Control("the focus fill is the wrong colour", "light/none", FOCUSED + "{background-color:red}", "li-a-focus-visible", "background-color"),
            new Control("the dark focus fill ignores the theme", "dark/none", FOCUSED + "{background-color:var(--color-blue-50)}", "li-a-focus-visible", "background-color"),
            new Control("the focus ring is the wrong colour", "light/none", FOCUSED + "{box-shadow:inset 0 0 0 2px red}", "li-a-focus-visible", "box-shadow"),
            new Control("the focus ring is not inset", "light/none", FOCUSED + "{box-shadow:0 0 0 2px var(--color-blue-500)}", "li-a-focus-visible", "box-shadow"),
            new Control("the focus ring is four pixels", "light/none", FOCUSED + "{box-shadow:inset 0 0 0 4px var(--color-blue-500)}", "li-a-focus-visible", "box-shadow"),
            new Control("the focus ring is missing", "light/none", FOCUSED + "{box-shadow:none}", "li-a-focus-visible", "box-shadow"),
            new Control("a focused parent lights up the rows nested under it (descendant selector)", "light/none",
                    ".ash-tree__item:focus-visible .ash-tree__row{background-color:var(--color-blue-50);box-shadow:inset 0 0 0 2px var(--color-blue-500)}", "li-a-focus-visible", "box-shadow"),
            new Control("a focused item lights up its own row AND the row above (parent selector)", "light/none",
                    ".ash-tree__item:has(> .ash-tree__panel .ash-tree__item:focus-visible) > .ash-tree__row{box-shadow:inset 0 0 0 2px var(--color-blue-500)}", "li-c-focus-visible", "box-shadow"),
            new Control("hover beats the focus fill", "light/none", "@media (hover:hover){.ash-tree__row:hover{background-color:var(--ash-tree-hover) !important}}", "li-b-focus-visible+row-b-hover", "background-color"),
            new Control("a focused item draws an outline", "light/none", ".ash-tree__item:focus{outline-style:solid}", "li-a-focus-visible", "outline-style"),
            new Control("a focused link draws an outline", "light/none", "a.ash-tree__label:focus{outline-style:solid}", "link-c-focus", "outline-style"),
            new Control("the toggle hover colour is missing", "light/none", ".ash-tree__toggle:hover{color:var(--ash-tree-toggle)}", "toggle-a-hover", "color"),
            new Control("the toggle hover colour is wrong", "light/none", "@media (hover:hover){.ash-tree__toggle:hover{color:red}}", "toggle-a-hover", "color"),
            new Control("the dark toggle hover colour ignores the theme", "dark/none", "@media (hover:hover){.ash-tree__toggle:hover{color:var(--color-slate-600)}}", "toggle-a-hover", "color"),
            new Control("the toggle also changes at rest", "light/none", ".ash-tree__toggle{color:var(--color-slate-600)}", "rest", "color"),
            new Control("a link is not underlined on hover", "light/none", "a.ash-tree__label:hover{text-decoration-line:none}", "link-c-hover", "text-decoration-line"),
            new Control("a link is underlined at rest", "light/none", "a.ash-tree__label{text-decoration-line:underline}", "rest", "text-decoration-line"),
            new Control("an expanded chevron is not turned", "light/none", TURN + "{rotate:none}", "rest", "rotate"),
            new Control("a collapsed chevron is turned", "light/none", ".ash-tree__chevron{rotate:90deg}", "rest", "rotate"),
            new Control("a collapsed child turns because an ancestor is expanded (descendant selector)", "light/none", ".ash-tree__item[aria-expanded=\"true\"] .ash-tree__chevron{rotate:90deg}", "rest", "rotate"),
            new Control("a collapsed panel is open", "light/none", ".ash-tree__panel[inert]{grid-template-rows:1fr}", "rest", "grid-template-rows"),
            new Control("an expanded panel is collapsed", "light/none", ".ash-tree__panel:not([inert]){grid-template-rows:0fr}", "rest", "grid-template-rows"));

    // ---- running ----

    private static final Map<String, List<String>> PRESENT = new LinkedHashMap<>();
    private static final Map<String, List<String>> REMOVED = new LinkedHashMap<>();
    private static final Map<String, List<String>> SCOPED_FIDELITY = new LinkedHashMap<>();
    private static final Map<String, List<String>> CONTROLS = new LinkedHashMap<>();
    private static final Map<String, Map<String, String>> LEGACY_VALUES = new LinkedHashMap<>();
    private static int comparisons;

    @BeforeAll
    static void measureEverything() throws Exception {
        ParitySession session = ParitySession.open("tree-state-parity");
        TreeLegacyMarkup tree = new TreeLegacyMarkup();
        IconLegacyMarkup icons = new IconLegacyMarkup();
        String migrated = markup(false, tree, icons);
        String legacy = markup(true, tree, icons);
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
                cdp.navigate(server.registerRaw(page(c, ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), control.css(), migrated)));
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

    private static String v(String config, String scenario, String element, String property) {
        String value = LEGACY_VALUES.get(config).get(scenario + "|" + element + "|" + property);
        assertTrue(value != null, scenario + "|" + element + "|" + property);
        return value;
    }

    // ---- tests ----

    @Test
    void everyConfigurationScenarioAndElementWasMeasuredAtRestAndInEveryForcedState() {
        assertEquals(22, PRESENT.size());
        assertEquals(22, REMOVED.size());
        assertEquals(22, SCOPED_FIDELITY.size());
        assertEquals(22 * SCENARIOS.size() * ELEMENTS.size() * PROPS.size() * 3, comparisons);
    }

    @Test
    void withTailwindPresentHoverActiveAndFocusVisibleMatchTheLegacyTreeInAllTwentyTwoConfigurations() {
        assertEquals(List.of(), flat(PRESENT));
    }

    @Test
    void withTailwindRemovedHoverActiveAndFocusVisibleMatchTheLegacyTreeInAllTwentyTwoConfigurations() {
        assertEquals(List.of(), flat(REMOVED));
    }

    @Test
    void theScopedLegacyStylesheetBehavesExactlyAsTheFullLegacyOneInEveryState() {
        assertEquals(List.of(), flat(SCOPED_FIDELITY));
    }

    @Test
    void everyNegativeControlIsDetectedInTheScenarioAndPropertyItBreaks() {
        for (Control control : CONTROL_LIST) {
            List<String> findings = CONTROLS.get(control.name());
            assertFalse(findings.isEmpty(), "the control was NOT detected: " + control.name());
            assertTrue(findings.stream().anyMatch(f -> f.contains("| " + control.scenario() + "|") && f.contains("|" + control.expectedProperty() + " |")),
                    control.name() + " should be detected in " + control.scenario() + "/" + control.expectedProperty() + " but was: " + findings.stream().limit(5).toList());
        }
    }

    @Test
    void theLegacyOracleActuallyChangesInEachStateSoTheComparisonIsNotVacuous() {
        String light = "light/none";
        assertNotEquals(v(light, "rest", "a-row", "background-color"), v(light, "row-a-hover", "a-row", "background-color"), "a row has a hover fill");
        assertEquals(v(light, "rest", "a-row", "color"), v(light, "row-a-hover", "a-row", "color"), "the text does not change on hover");
        assertNotEquals(v(light, "rest", "a-row", "box-shadow"), v(light, "li-a-focus-visible", "a-row", "box-shadow"), "a focused item's row has a ring");
        assertTrue(v(light, "li-a-focus-visible", "a-row", "box-shadow").contains("inset"), "an inset ring: " + v(light, "li-a-focus-visible", "a-row", "box-shadow"));
        assertNotEquals(v(light, "rest", "a-row", "background-color"), v(light, "li-a-focus-visible", "a-row", "background-color"), "and a focus fill");
        assertEquals(v(light, "li-a-focus-visible", "a-row", "background-color"), v(light, "hover+focus-visible-all", "a-row", "background-color"), "the focus fill wins over the hover fill");
        assertNotEquals(v(light, "row-a-hover", "a-row", "background-color"), v(light, "hover+focus-visible-all", "a-row", "background-color"), "so the hover fill really is overridden");
        assertEquals(v(light, "li-b-focus-visible+row-b-hover", "b-row", "background-color"), v(light, "li-a-focus-visible", "a-row", "background-color"), "focus beats hover on a nested row too");
        assertEquals("none", v(light, "li-a-focus-visible", "a-li", "outline-style"), "a focused item draws no outline");
        assertNotEquals(v(light, "rest", "a-toggle", "color"), v(light, "toggle-a-hover", "a-toggle", "color"), "the toggle changes colour on hover");
        assertEquals("none", v(light, "rest", "c-label", "text-decoration-line"));
        assertEquals("underline", v(light, "link-c-hover", "c-label", "text-decoration-line"), "a link is underlined on hover");
        assertEquals("none", v(light, "link-c-focus", "c-label", "outline-style"), "a focused link draws no outline");
        assertEquals("90deg", v(light, "rest", "a-chevron", "rotate"), "an expanded chevron is a quarter turn");
        assertEquals("90deg", v(light, "rest", "b-chevron", "rotate"), "and so is a nested expanded one");
        assertEquals("none", v(light, "rest", "d-chevron", "rotate"), "a collapsed child of an expanded parent is NOT turned");
        assertEquals("0px", v(light, "rest", "d-panel", "grid-template-rows"), "a collapsed panel is a zero row");
        assertNotEquals("0px", v(light, "rest", "b-panel", "grid-template-rows"), "and an expanded one is not");
        assertNotEquals(v(light, "rest", "d-panel", "height"), v(light, "rest", "b-panel", "height"), "and an expanded one is taller");
        Map<String, String> dark = LEGACY_VALUES.get("dark/none");
        assertNotEquals(dark.get("row-a-hover|a-row|background-color"), LEGACY_VALUES.get(light).get("row-a-hover|a-row|background-color"), "dark mode has its own hover fill");
        assertNotEquals(dark.get("li-a-focus-visible|a-row|background-color"), LEGACY_VALUES.get(light).get("li-a-focus-visible|a-row|background-color"), "and its own focus fill");
        assertNotEquals(dark.get("rest|a-row|color"), LEGACY_VALUES.get(light).get("rest|a-row|color"));
    }

    @Test
    void focusAndHoverOnANestedItemNeverReachTheRowsAboveOrBelowIt() {
        for (String config : List.of("light/none", "dark/none")) {
            String own = v(config, "li-b-focus-visible", "b-row", "box-shadow");
            for (String other : List.of("a-row", "c-row", "d-row", "e-row", "f-row")) {
                assertEquals(v(config, "rest", other, "box-shadow"), v(config, "li-b-focus-visible", other, "box-shadow"), config + ": focusing b must not ring " + other);
                assertEquals(v(config, "rest", other, "background-color"), v(config, "li-b-focus-visible", other, "background-color"), config + ": nor fill " + other);
            }
            assertNotEquals(v(config, "rest", "b-row", "box-shadow"), own, config + ": b's own row is ringed");
            for (String other : List.of("a-row", "b-row", "d-row", "f-row")) {
                assertEquals(v(config, "rest", other, "background-color"), v(config, "link-c-hover", other, "background-color"), config + ": hovering the link in c must not fill " + other);
            }
            assertNotEquals(v(config, "rest", "c-row", "background-color"), v(config, "link-c-hover", "c-row", "background-color"), config + ": the row the link is in IS hovered, so it is filled");
        }
    }
}
