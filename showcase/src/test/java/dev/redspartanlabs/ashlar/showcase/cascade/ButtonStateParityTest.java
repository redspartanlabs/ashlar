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
 * Button's hover, active and keyboard-focus states, which a measurement of a resting page cannot hold. Chrome is driven through its own DevTools
 * protocol, and the states are FORCED on the real elements ({@code CSS.forcePseudoState}), so the same rules run that run for a user: the migrated
 * Button's {@code :hover}, {@code :active} and {@code :focus-visible} rules next to the legacy Button's Tailwind {@code hover:}, {@code active:} and
 * {@code focus-visible:} ones. The device is stated to Chrome as one that can hover (the rules sit inside {@code @media (hover: hover)}, as Tailwind's did).
 *
 * <p>This is isolated to Button: the shared parity harness is not changed, and the small DevTools client below is private to this suite. A page is
 * built for each of the 22 theme and skin configurations with the legacy Buttons (Tailwind oracle), with the migrated Buttons beside Tailwind, and with
 * the migrated Buttons alone (Tailwind removed); every Button is measured at rest and with each state forced, and every state's computed colours,
 * shadows, underline, outline and opacity must equal the legacy Button's. Transitions are switched off on every page, on both sides, so a measurement
 * is the settled value (their duration and timing function are compared at rest by {@link ButtonParityTest}).
 *
 * <p>Negative controls break the migrated stylesheet in one state and show each gate can fail.
 */
class ButtonStateParityTest {

    private static final String ORACLE = ParityFrames.generated("button-oracle.css");
    private static final String SCOPED = ParityFrames.generated("button-legacy-scoped.css");
    private static final List<String> PROPS = List.of("background-color", "color", "box-shadow", "text-decoration-line", "text-underline-offset", "outline-style", "opacity", "cursor",
            "border-top-color", "border-top-left-radius");
    private static final List<String> VARIANTS = List.of("primary", "secondary", "success", "warning", "danger", "info", "ghost", "link");
    private static final List<String> KINDS = List.of("rest", "disabled", "loading", "pressed", "pressed-class");
    private static final String NO_MOTION = "*,*::before,*::after{transition:none !important;animation-play-state:paused !important}";

    /** A state to force: its name and the pseudo-classes forced on every Button. */
    private record State(String name, List<String> forced) {
    }

    private static final List<State> STATES = List.of(new State("rest", List.of()), new State("hover", List.of("hover")), new State("active", List.of("active")),
            new State("focus-visible", List.of("focus", "focus-visible")), new State("hover+active", List.of("hover", "active")),
            new State("hover+focus-visible", List.of("hover", "focus", "focus-visible")));

    // ---- markup ----

    private static String button(String variant, String kind, boolean legacy) {
        boolean pressedAria = kind.equals("pressed");
        String classes = "ash-btn ash-btn--" + variant + " ash-btn--md" + (kind.equals("pressed-class") ? " ash-btn--pressed" : "");
        String name = variant + "-" + kind;
        String html = "<button type=\"button\" class=\"" + classes + "\" data-btn data-btn-variant=\"" + variant + "\""
                + (kind.equals("disabled") ? " disabled" : "") + (kind.equals("loading") ? " aria-busy=\"true\" aria-disabled=\"true\"" : "")
                + (pressedAria ? " data-btn-toggle aria-pressed=\"true\"" : "") + " data-parity=\"button\" data-parity-case=\"" + name + "\">"
                + "<span data-btn-spinner class=\"ash-btn__spinner\" aria-hidden=\"true\"></span><span data-btn-label class=\"ash-btn__label\">Label</span>"
                + "<span class=\"ash-sr-only ash-btn__status\" data-btn-status aria-live=\"polite\">" + (kind.equals("loading") ? "Loading" : "") + "</span></button>";
        return html;
    }

    private static String markup(boolean legacyMarkup, ButtonLegacyMarkup rewriter) {
        StringBuilder sb = new StringBuilder();
        for (String v : VARIANTS) {
            for (String kind : KINDS) {
                sb.append("<div style=\"margin:4px 0\">").append(button(v, kind, false)).append("</div>");
            }
        }
        return legacyMarkup ? rewriter.rewrite(sb.toString()) : sb.toString();
    }

    // ---- measurement ----

    /** The settled computed values of every Button on the page, in the given state: {@code state|case|property -> value}. */
    private static Map<String, String> measure(DevToolsSession cdp, int expectedSheets) throws Exception {
        String sanity = cdp.evaluate("(function(){var ok=[].every.call(document.styleSheets,function(s){try{return s.cssRules.length>0}catch(e){return true}});"
                + "return document.styleSheets.length+'|'+ok+'|'+getComputedStyle(document.querySelector('[data-btn]')).display})()");
        String[] parts = sanity.split("\\|");
        if (Integer.parseInt(parts[0]) != expectedSheets || !parts[1].equals("true") || !parts[2].equals("inline-flex")) {
            throw new IllegalStateException("INFRASTRUCTURE FAILURE, not a parity result: the page did not load its stylesheets: " + sanity + " (expected " + expectedSheets + ")");
        }
        List<Integer> buttons = cdp.nodes("[data-btn]");
        Map<String, String> values = new LinkedHashMap<>();
        String reader = "(function(){var props=" + "[" + PROPS.stream().map(p -> "'" + p + "'").collect(Collectors.joining(",")) + "];var out=[];"
                + "[].forEach.call(document.querySelectorAll('[data-btn]'),function(b){var cs=getComputedStyle(b);props.forEach(function(p){out.push(b.getAttribute('data-parity-case')+'\\t'+p+'\\t'+cs.getPropertyValue(p))})});"
                + "return out.join('\\n')})()";
        for (State state : STATES) {
            cdp.force(buttons, state.forced());
            for (String line : cdp.evaluate(reader).split("\n")) {
                String[] f = line.split("\t", -1);
                values.put(state.name() + "|" + f[0] + "|" + f[1], f[2]);
            }
            cdp.force(buttons, List.of());
        }
        return values;
    }

    private static String page(ParityConfigs.Config c, List<String> sheets, String extraCss, String body) {
        StringBuilder links = new StringBuilder();
        sheets.forEach(href -> links.append("<link rel=\"stylesheet\" href=\"").append(href).append("\">"));
        String skin = c.skin() == null ? "" : " data-skin=\"" + c.skin() + "\"";
        return "<!doctype html><html data-theme=\"" + c.theme() + "\"" + skin + "><head>" + links + "<style>" + NO_MOTION + extraCss + "</style></head>"
                + "<body style=\"margin:0;padding:8px\">" + body + "</body></html>";
    }

    private static boolean same(String property, String legacy, String migrated) {
        if (property.equals("box-shadow")) {
            return ParityNormalizer.shadowLayers(legacy).equals(ParityNormalizer.shadowLayers(migrated));
        }
        return legacy.equals(migrated);
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

    private static final List<Control> CONTROL_LIST = List.of(
            new Control("hover background is wrong", "light/none", "@media (hover:hover){.ash-btn--primary:hover{background-color:red}}", "hover", "background-color"),
            new Control("hover is missing", "light/none", ".ash-btn--success:hover{background-color:var(--color-green-600)}", "hover", "background-color"),
            new Control("active background is wrong", "light/none", ".ash-btn--danger:active{background-color:red}", "active", "background-color"),
            new Control("active does not win over hover", "light/none", ".ash-btn--info:hover:active{background-color:var(--color-sky-700)}", "hover+active", "background-color"),
            new Control("the focus ring is the wrong colour", "light/none", ".ash-btn--primary:focus-visible{box-shadow:0 0 0 2px #fff,0 0 0 4px red}", "focus-visible", "box-shadow"),
            new Control("the focus ring is missing", "light/none", ".ash-btn--secondary:focus-visible{box-shadow:none}", "focus-visible", "box-shadow"),
            new Control("the focus ring is wider", "light/none", ".ash-btn--warning:focus-visible{box-shadow:0 0 0 2px #fff,0 0 0 6px var(--color-amber-500)}", "focus-visible", "box-shadow"),
            new Control("a focused button draws an outline", "light/none", ".ash-btn:focus{outline-style:solid}", "focus-visible", "outline-style"),
            new Control("a disabled button changes on hover", "light/none", ".ash-btn--primary:disabled:hover{background-color:red}", "hover", "background-color"),
            new Control("a loading button changes on hover", "light/none", ".ash-btn--primary[aria-disabled=\"true\"]:hover{background-color:red}", "hover", "background-color"),
            new Control("a pressed button changes on hover", "light/none", ".ash-btn--primary[aria-pressed=\"true\"]:hover{background-color:red}", "hover", "background-color"),
            new Control("a pressed button changes while pressed down", "light/none", ".ash-btn--secondary[aria-pressed=\"true\"]:active{background-color:red}", "active", "background-color"),
            new Control("a link is not underlined on hover", "light/none", ".ash-btn--link:hover{text-decoration-line:none}", "hover", "text-decoration-line"),
            new Control("a disabled link is underlined on hover", "light/none", ".ash-btn--link:disabled:hover{text-decoration-line:underline}", "hover", "text-decoration-line"),
            new Control("a ghost hover is wrong in dark mode", "dark/none", ".ash-btn--ghost:hover{background-color:var(--color-slate-100)}", "hover", "background-color"),
            new Control("a secondary active is wrong in dark mode", "dark/none", ".ash-btn--secondary:active{background-color:var(--color-slate-100)}", "active", "background-color"),
            new Control("Foundry's pressed-down shadow is lost", "light/foundry", "html[data-skin] .ash-btn.ash-btn:active{box-shadow:none}", "active", "box-shadow"),
            new Control("a skin's flat shadow is lost on focus", "light/axiom", "html[data-skin] .ash-btn.ash-btn{--ash-btn-shadow:0 1px 3px 0 rgb(0 0 0 / 0.1)}", "focus-visible", "box-shadow"));

    // ---- running ----

    private static final Map<String, List<String>> PRESENT = new LinkedHashMap<>();
    private static final Map<String, List<String>> REMOVED = new LinkedHashMap<>();
    private static final Map<String, List<String>> SCOPED_FIDELITY = new LinkedHashMap<>();
    private static final Map<String, List<String>> CONTROLS = new LinkedHashMap<>();
    private static final Map<String, Map<String, String>> LEGACY_VALUES = new LinkedHashMap<>();
    private static int comparisons;

    @BeforeAll
    static void measureEverything() throws Exception {
        ParitySession session = ParitySession.open("button-state-parity");
        ButtonLegacyMarkup rewriter = new ButtonLegacyMarkup();
        String migrated = markup(false, rewriter);
        String legacy = markup(true, rewriter);
        String environment = ParityFrames.environment(session.generatedDir());
        int skinSheets = ParityConfigs.SKIN_LINKS.size();
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir()); DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
            for (ParityConfigs.Config c : ParityConfigs.all()) {
                cdp.navigate(server.registerRaw(page(c, ParityFrames.with(ORACLE, ParityConfigs.SKIN_LINKS), "", legacy)));
                Map<String, String> expected = measure(cdp, 2 + skinSheets);
                if (c.id().equals("light/none") || c.id().equals("dark/none") || c.id().equals("light/foundry") || c.id().equals("light/axiom")) {
                    LEGACY_VALUES.put(c.id(), expected);
                }
                cdp.navigate(server.registerRaw(page(c, ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), "", migrated)));
                PRESENT.put(c.id(), differences(c.id() + " (Tailwind present)", expected, measure(cdp, 2 + skinSheets)));
                cdp.navigate(server.registerRaw(page(c, ParityFrames.with(ParityFrames.ASHLAR, ParityConfigs.SKIN_LINKS), environment, migrated)));
                Map<String, String> removed = measure(cdp, 2 + skinSheets);
                REMOVED.put(c.id(), differences(c.id() + " (Tailwind removed)", expected, removed));
                cdp.navigate(server.registerRaw(page(c, ParityFrames.with(SCOPED, ParityConfigs.SKIN_LINKS), environment, legacy)));
                SCOPED_FIDELITY.put(c.id(), differences(c.id() + " (scoped legacy vs full legacy)", expected, measure(cdp, 2 + skinSheets)));
                comparisons += expected.size() * 3;
            }
            for (Control control : CONTROL_LIST) {
                ParityConfigs.Config c = ParityConfigs.get(control.config());
                cdp.navigate(server.registerRaw(page(c, ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), control.css(), migrated)));
                CONTROLS.put(control.name(), differences("control: " + control.name(), LEGACY_VALUES.containsKey(c.id()) ? LEGACY_VALUES.get(c.id()) : Map.of(),
                        measure(cdp, 2 + skinSheets)));
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
    void everyConfigurationStateAndButtonWasMeasuredAtRestAndInEveryForcedState() {
        assertEquals(22, PRESENT.size());
        assertEquals(22, REMOVED.size());
        assertEquals(22, SCOPED_FIDELITY.size());
        // 22 configurations x 6 states x 40 buttons x 10 properties, compared three ways.
        assertEquals(22 * STATES.size() * VARIANTS.size() * KINDS.size() * PROPS.size() * 3, comparisons);
        assertEquals(6, STATES.size());
    }

    @Test
    void withTailwindPresentHoverActiveAndFocusVisibleMatchTheLegacyButtonsInAllTwentyTwoConfigurations() {
        assertEquals(List.of(), flat(PRESENT));
    }

    @Test
    void withTailwindRemovedHoverActiveAndFocusVisibleMatchTheLegacyButtonsInAllTwentyTwoConfigurations() {
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
        assertNotEquals(v.get("rest|primary-rest|background-color"), v.get("hover|primary-rest|background-color"));
        assertNotEquals(v.get("hover|primary-rest|background-color"), v.get("active|primary-rest|background-color"));
        assertNotEquals(v.get("rest|primary-rest|box-shadow"), v.get("focus-visible|primary-rest|box-shadow"));
        assertTrue(v.get("focus-visible|primary-rest|box-shadow").contains("oklch(0.623 0.214 259.815) 0px 0px 0px 4px"), "the focus ring is blue-500: " + v.get("focus-visible|primary-rest|box-shadow"));
        assertEquals("none", v.get("rest|link-rest|text-decoration-line"));
        assertEquals("underline", v.get("hover|link-rest|text-decoration-line"));
        assertEquals("none", v.get("hover|link-disabled|text-decoration-line"), "a disabled link does not underline on hover");
        assertEquals(v.get("rest|primary-disabled|background-color"), v.get("hover|primary-disabled|background-color"), "a disabled button does not change on hover");
        assertEquals(v.get("rest|primary-loading|background-color"), v.get("hover|primary-loading|background-color"), "a loading button does not change on hover");
        assertEquals(v.get("rest|primary-pressed|background-color"), v.get("hover|primary-pressed|background-color"), "a pressed button does not change on hover");
        assertEquals(v.get("rest|primary-pressed|background-color"), v.get("active|primary-pressed|background-color"), "nor while pressed down");
        assertEquals("none", v.get("focus-visible|primary-rest|outline-style"));
        Map<String, String> dark = LEGACY_VALUES.get("dark/none");
        assertNotEquals(dark.get("hover|secondary-rest|background-color"), v.get("hover|secondary-rest|background-color"), "dark mode has its own hover");
        assertNotEquals(dark.get("hover|ghost-rest|background-color"), v.get("hover|ghost-rest|background-color"));
        Map<String, String> foundry = LEGACY_VALUES.get("light/foundry");
        assertNotEquals(foundry.get("rest|primary-rest|box-shadow"), foundry.get("active|primary-rest|box-shadow"), "Foundry's pressed-down shadow is different from its resting one");
        assertEquals(new TreeSet<>(List.of("rest", "hover", "active", "focus-visible", "hover+active", "hover+focus-visible")),
                new TreeSet<>(STATES.stream().map(State::name).toList()));
    }
}
