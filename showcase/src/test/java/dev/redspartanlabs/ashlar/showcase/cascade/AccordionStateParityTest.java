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
 * Hover, active and keyboard focus of Accordion and Accordion Item, which a measurement of a resting page cannot hold. Chrome is driven through the shared
 * {@link DevToolsSession}; the states are FORCED on the real elements, so the same rules run that run for a user: the migrated button's {@code :hover} and
 * {@code :focus-visible} rules next to the legacy button's Tailwind {@code hover:} and {@code focus-visible:} ones. The device is stated to Chrome as one that can hover
 * (the rules sit inside {@code @media (hover: hover)}, as Tailwind's did).
 *
 * <p>A page is built for each of the 22 theme and skin configurations with the legacy markup (Tailwind oracle), with the migrated markup beside Tailwind, and with the
 * migrated markup alone (Tailwind removed). Every part is measured at rest and with each state forced; each state's computed colours, fill, shadow, outline, chevron turn
 * and panel row must equal the legacy element's. The accordion has a collapsed section and an expanded one, so the two state-keyed rules (the chevron turn on
 * {@code aria-expanded} and the collapsed row on {@code inert}) are measured next to their legacy inline-style and class equivalents. Negative controls break the migrated
 * stylesheet in one state and show each gate can fail.
 */
class AccordionStateParityTest {

    private static final String ORACLE = ParityFrames.generated("accordion-oracle.css");
    private static final String SCOPED = ParityFrames.generated("accordion-legacy-scoped.css");
    private static final List<String> PROPS = List.of("color", "background-color", "border-bottom-color", "border-top-color", "border-bottom-width", "box-shadow", "outline-style", "outline-width",
            "outline-color", "text-decoration-line", "cursor", "opacity", "font-weight", "rotate", "grid-template-rows", "height");
    private static final String NO_MOTION = "*,*::before,*::after{transition:none !important;animation-play-state:paused !important}";
    private static final int ELEMENTS = 7;

    private record State(String name, List<String> forced) {
    }

    private static final List<State> STATES = List.of(new State("rest", List.of()), new State("hover", List.of("hover")), new State("active", List.of("active")),
            new State("focus-visible", List.of("focus", "focus-visible")), new State("hover+focus-visible", List.of("hover", "focus", "focus-visible")));

    // ---- markup: one accordion with a collapsed section and an expanded one ----

    private static String state(String name) {
        return " data-state-name=\"" + name + "\"";
    }

    private static String section(String id, boolean expanded) {
        String which = expanded ? "expanded" : "collapsed";
        return "<div data-accordion-item class=\"ash-accordion__item\"><h3 class=\"ash-accordion__heading\">"
                + "<button type=\"button\" id=\"a-button-" + id + "\" data-accordion-button aria-expanded=\"" + expanded + "\" aria-controls=\"a-panel-" + id + "\" class=\"ash-accordion__button\""
                + state(which + "-button") + "><span class=\"ash-accordion__title\">Section " + id + "</span>"
                + "<span data-accordion-chevron class=\"ash-accordion__chevron\"" + state(which + "-chevron") + "><svg class=\"ash-icon ash-icon--sm\" viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M9 5l7 7-7 7\"/></svg></span>"
                + "</button></h3><div id=\"a-panel-" + id + "\" data-accordion-panel role=\"region\" aria-labelledby=\"a-button-" + id + "\"" + (expanded ? "" : " inert") + " class=\"ash-accordion__panel\"" + state(which + "-panel") + ">"
                + "<div class=\"ash-accordion__clip\"><div class=\"ash-accordion__content\"" + (expanded ? state("expanded-content") : "") + ">Content " + id + "</div></div></div></div>";
    }

    private static String markup(boolean legacyMarkup, AccordionLegacyMarkup accordion, IconLegacyMarkup icons) {
        String html = "<div id=\"a\" data-accordion class=\"ash-accordion\">" + section("one", false) + section("two", true) + "</div>";
        // Every element of the components carries data-parity (the scoped legacy stylesheet's preflight keys on it).
        html = html.replaceAll("(<[a-z0-9]+ [^>]*class=\"ash-[^\"]*\")", "$1 data-parity=\"state\"");
        return legacyMarkup ? icons.rewrite(accordion.rewrite(html)) : html;
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
        if (nodes.size() != ELEMENTS) {
            throw new IllegalStateException("INFRASTRUCTURE FAILURE: expected " + ELEMENTS + " measured elements, found " + nodes.size());
        }
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

    private record Control(String name, String config, String css, String state, String expectedProperty) {
    }

    private static final String EXPANDED_CHEVRON = ".ash-accordion__button[aria-expanded=\"true\"] .ash-accordion__chevron";

    private static final List<Control> CONTROL_LIST = List.of(
            new Control("the hover fill is the wrong colour", "light/none", "@media (hover:hover){.ash-accordion__button:hover{background-color:red}}", "hover", "background-color"),
            new Control("the hover fill is missing", "light/none", ".ash-accordion__button:hover{background-color:transparent}", "hover", "background-color"),
            new Control("the hover fill is also at rest", "light/none", ".ash-accordion__button{background-color:var(--color-slate-50)}", "rest", "background-color"),
            new Control("the dark hover fill ignores the theme", "dark/none", "@media (hover:hover){.ash-accordion__button:hover{background-color:var(--color-slate-50)}}", "hover", "background-color"),
            new Control("the hover text colour changes", "light/none", "@media (hover:hover){.ash-accordion__button:hover{color:red}}", "hover", "color"),
            new Control("the focus ring is the wrong colour", "light/none", ".ash-accordion__button:focus-visible{box-shadow:inset 0 0 0 2px red}", "focus-visible", "box-shadow"),
            new Control("the focus ring is not inset", "light/none", ".ash-accordion__button:focus-visible{box-shadow:0 0 0 2px var(--color-blue-500)}", "focus-visible", "box-shadow"),
            new Control("the focus ring is four pixels", "light/none", ".ash-accordion__button:focus-visible{box-shadow:inset 0 0 0 4px var(--color-blue-500)}", "focus-visible", "box-shadow"),
            new Control("the focus ring is missing", "light/none", ".ash-accordion__button:focus-visible{box-shadow:none}", "focus-visible", "box-shadow"),
            new Control("a focused button draws an outline", "light/none", ".ash-accordion__button:focus{outline-style:solid}", "focus-visible", "outline-style"),
            new Control("an expanded chevron is not turned", "light/none", EXPANDED_CHEVRON + "{rotate:none}", "rest", "rotate"),
            new Control("a collapsed chevron is turned", "light/none", ".ash-accordion__chevron{rotate:90deg}", "rest", "rotate"),
            new Control("a collapsed panel is open", "light/none", ".ash-accordion__panel[inert]{grid-template-rows:1fr}", "rest", "grid-template-rows"),
            new Control("an expanded panel is collapsed", "light/none", ".ash-accordion__panel:not([inert]){grid-template-rows:0fr}", "rest", "grid-template-rows"));

    // ---- running ----

    private static final Map<String, List<String>> PRESENT = new LinkedHashMap<>();
    private static final Map<String, List<String>> REMOVED = new LinkedHashMap<>();
    private static final Map<String, List<String>> SCOPED_FIDELITY = new LinkedHashMap<>();
    private static final Map<String, List<String>> CONTROLS = new LinkedHashMap<>();
    private static final Map<String, Map<String, String>> LEGACY_VALUES = new LinkedHashMap<>();
    private static int comparisons;

    @BeforeAll
    static void measureEverything() throws Exception {
        ParitySession session = ParitySession.open("accordion-state-parity");
        AccordionLegacyMarkup accordion = new AccordionLegacyMarkup();
        IconLegacyMarkup icons = new IconLegacyMarkup();
        String migrated = markup(false, accordion, icons);
        String legacy = markup(true, accordion, icons);
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

    // ---- tests ----

    @Test
    void everyConfigurationStateAndElementWasMeasuredAtRestAndInEveryForcedState() {
        assertEquals(22, PRESENT.size());
        assertEquals(22, REMOVED.size());
        assertEquals(22, SCOPED_FIDELITY.size());
        assertEquals(22 * STATES.size() * ELEMENTS * PROPS.size() * 3, comparisons);
    }

    @Test
    void withTailwindPresentHoverActiveAndFocusVisibleMatchTheLegacyAccordionInAllTwentyTwoConfigurations() {
        assertEquals(List.of(), flat(PRESENT));
    }

    @Test
    void withTailwindRemovedHoverActiveAndFocusVisibleMatchTheLegacyAccordionInAllTwentyTwoConfigurations() {
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
        assertNotEquals(v.get("rest|collapsed-button|background-color"), v.get("hover|collapsed-button|background-color"), "a button has a hover fill");
        assertNotEquals(v.get("rest|expanded-button|background-color"), v.get("hover|expanded-button|background-color"), "an expanded one too");
        assertEquals(v.get("rest|collapsed-button|color"), v.get("hover|collapsed-button|color"), "the text does not change on hover");
        assertNotEquals(v.get("rest|collapsed-button|box-shadow"), v.get("focus-visible|collapsed-button|box-shadow"), "a focused button has a ring");
        assertTrue(v.get("focus-visible|collapsed-button|box-shadow").contains("inset"), "an inset ring: " + v.get("focus-visible|collapsed-button|box-shadow"));
        assertEquals("none", v.get("focus-visible|collapsed-button|outline-style"), "and no outline");
        assertEquals("90deg", v.get("rest|expanded-chevron|rotate"), "an expanded chevron is a quarter turn");
        assertEquals("none", v.get("rest|collapsed-chevron|rotate"), "a collapsed one is not turned");
        assertEquals("0px", v.get("rest|collapsed-panel|grid-template-rows"), "a collapsed panel is a zero row");
        assertNotEquals("0px", v.get("rest|expanded-panel|grid-template-rows"), "and an expanded one is not");
        assertNotEquals(v.get("rest|collapsed-panel|height"), v.get("rest|expanded-panel|height"), "and an expanded one is taller");
        Map<String, String> dark = LEGACY_VALUES.get("dark/none");
        assertNotEquals(dark.get("hover|collapsed-button|background-color"), v.get("hover|collapsed-button|background-color"), "dark mode has its own hover fill");
        assertNotEquals(dark.get("rest|collapsed-button|color"), v.get("rest|collapsed-button|color"));
        assertEquals(new TreeSet<>(List.of("rest", "hover", "active", "focus-visible", "hover+focus-visible")), new TreeSet<>(STATES.stream().map(State::name).toList()));
    }
}
