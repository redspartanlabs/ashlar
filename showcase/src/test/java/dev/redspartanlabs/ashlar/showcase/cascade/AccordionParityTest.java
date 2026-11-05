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
 * The static parity gates of Accordion and Accordion Item (migrated together), measured in real Chrome on the shared parity harness (unchanged). Hover and keyboard focus
 * are {@link AccordionStateParityTest}; the script (toggling, single-open, attribute synchronisation) is {@link AccordionScriptTest}.
 *
 * <p>Evidence is a controlled oracle: the components' legacy Tailwind markup (compiled by the real Tailwind CLI) next to their migrated markup, in the same browser, for
 * each of the 22 theme and skin configurations, with Tailwind present and with Tailwind removed. The legacy markup is the migrated markup rewritten by
 * {@link AccordionLegacyMarkup}, so both sides are the same accordions in the same state. The cases are an accordion with its second section expanded, one with every
 * section collapsed, one with its first section expanded and two paragraphs of content, and one in a 220px column whose long titles wrap.
 *
 * <p>The accordion has no breakpoint, so every configuration is measured at 440px, and the four theme and skin configurations that matter for non-colour properties are
 * also measured at 320 and 900px. The whole measurement is repeated once with Chrome forced to {@code prefers-reduced-motion: reduce}. Negative controls show each gate
 * can fail.
 *
 * <p>Evidence is Chrome only. Computed-style and box parity is the visual evidence; there is no screenshot comparison.
 */
class AccordionParityTest {

    private static final String ORACLE = ParityFrames.generated("accordion-oracle.css");
    private static final String SCOPED = ParityFrames.generated("accordion-legacy-scoped.css");
    private static final List<ParityConfigs.Config> CONFIGS = ParityConfigs.all();
    private static final List<ParityConfigs.Config> EDGE_CONFIGS = ParityConfigs.reduced();
    private static final List<Integer> WIDTHS = List.of(440);
    private static final List<Integer> EDGE_WIDTHS = List.of(320, 900);
    private static final List<String> EXTRA_PROPS = List.of("font-family", "font-size", "font-weight", "line-height", "letter-spacing", "text-transform", "text-align", "text-decoration-line",
            "white-space", "cursor", "appearance", "pointer-events", "transition-property", "transition-duration", "transition-delay", "transition-timing-function", "row-gap", "column-gap",
            "align-items", "justify-content", "flex-direction", "outline-style", "background-image", "list-style-type", "rotate", "grid-template-rows");

    private static final Set<String> CALLER_KINDS = Set.of("raw");

    private static List<ParityProbe.Row> owned(List<ParityProbe.Row> rows) {
        return rows.stream().filter(r -> !CALLER_KINDS.contains(r.kind())).toList();
    }

    private record Item(String id, String title, boolean expanded, int paragraphs) {
    }

    /** One accordion: its sections and the layout it sits in. */
    private record Group(String name, List<Item> items, String layout) {
    }

    private static Item i(String id, String title, boolean expanded) {
        return new Item(id, title, expanded, 1);
    }

    private static final List<Group> GROUPS = List.of(
            new Group("basic", List.of(i("one", "Shipping", false), i("two", "Returns", true), i("three", "Warranty", false)), "block"),
            new Group("closed", List.of(i("one", "Account", false), i("two", "Billing", false), i("three", "Privacy", false)), "block"),
            new Group("first", List.of(new Item("one", "Getting started", true, 2), i("two", "Installation", false)), "block"),
            new Group("narrow", List.of(i("one", "A question with a rather long title that has to wrap", false), i("two", "Another long title which also wraps in a column", true),
                    i("three", "Short", false)), "narrow"));

    // ---- state of the measurement ----

    private static AccordionLegacyMarkup accordionLegacy;
    private static IconLegacyMarkup iconLegacy;
    private static ParityProbe.Probe probe;
    private static ParityProbe.Probe reduced;
    private static final Map<String, List<ParityProbe.Finding>> PRESENT = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> REMOVED_SCOPED = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> REMOVED_FULL = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> SCOPED_FIDELITY = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> REDUCED = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> CONTROLS = new LinkedHashMap<>();
    private static final Map<String, Integer> APPLIED_PRESENT = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_REMOVED = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_SCOPED = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_REDUCED = new TreeMap<>();
    private static final Map<String, Integer> ROWS = new LinkedHashMap<>();
    private static String migratedForCounting;

    // ---- markup (what the two templates render, written out) ----

    private static String mark(String kind, String name) {
        return " data-parity=\"" + kind + "\" data-parity-case=\"" + name + "\"";
    }

    private static String item(Group g, Item item) {
        String name = g.name() + "-" + item.id();
        String buttonId = g.name() + "-button-" + item.id();
        String panelId = g.name() + "-panel-" + item.id();
        StringBuilder sb = new StringBuilder("<div data-accordion-item class=\"ash-accordion__item\"" + mark("item", name) + ">");
        sb.append("<h3 class=\"ash-accordion__heading\"").append(mark("heading", name)).append(">");
        sb.append("<button type=\"button\" id=\"").append(buttonId).append("\" data-accordion-button aria-expanded=\"").append(item.expanded()).append("\" aria-controls=\"").append(panelId)
                .append("\" class=\"ash-accordion__button\"").append(mark("button", name)).append(">");
        sb.append("<span class=\"ash-accordion__title\"").append(mark("title", name)).append(">").append(item.title()).append("</span>");
        sb.append("<span data-accordion-chevron class=\"ash-accordion__chevron\"").append(mark("chevron", name)).append(">");
        sb.append("<svg class=\"ash-icon ash-icon--sm\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" aria-hidden=\"true\"").append(mark("chevron-svg", name))
                .append("><path d=\"M9 5l7 7-7 7\"/></svg></span></button></h3>");
        sb.append("<div id=\"").append(panelId).append("\" data-accordion-panel role=\"region\" aria-labelledby=\"").append(buttonId).append("\"").append(item.expanded() ? "" : " inert")
                .append(" class=\"ash-accordion__panel\"").append(mark("panel", name)).append(">");
        sb.append("<div class=\"ash-accordion__clip\"").append(mark("clip", name)).append("><div class=\"ash-accordion__content\"").append(mark("content", name)).append(">");
        for (int p = 1; p <= item.paragraphs(); p++) {
            sb.append("<p style=\"margin:0\"").append(mark("raw", name + "-p" + p)).append(">Content of ").append(item.title()).append(" paragraph ").append(p)
                    .append(" with some words so that the text is longer than one short line.</p>");
        }
        return sb.append("</div></div></div></div>").toString();
    }

    private static String group(Group g) {
        StringBuilder sb = new StringBuilder("<div id=\"" + g.name() + "\" data-accordion class=\"ash-accordion\"" + mark("accordion", g.name()) + ">");
        g.items().forEach(it -> sb.append(item(g, it)));
        return sb.append("</div>").toString();
    }

    private static String markup(boolean legacyMarkup) {
        StringBuilder sb = new StringBuilder();
        for (Group g : GROUPS) {
            String style = g.layout().equals("narrow") ? "box-sizing:border-box;width:220px" : "box-sizing:border-box";
            sb.append("<div style=\"margin:4px 0\"><div data-layout=\"").append(g.layout()).append("\" style=\"").append(style).append("\">").append(group(g)).append("</div></div>");
        }
        String html = sb.toString();
        return legacyMarkup ? iconLegacy.rewrite(accordionLegacy.rewrite(html)) : html;
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

    // ---- control experiments: each breaks the migrated accordion in ONE way and must be detected ----

    private record Control(String name, String config, int width, String css, String expectedProperty) {
    }

    private static final String EXPANDED_CHEVRON = ".ash-accordion__button[aria-expanded=\"true\"] .ash-accordion__chevron";

    private static final List<Control> CONTROL_LIST = List.of(
            new Control("button inline padding is wrong", "light/none", 440, ".ash-accordion__button{padding-inline:8px}", "padding-left"),
            new Control("button block padding is wrong", "light/none", 440, ".ash-accordion__button{padding-block:8px}", "padding-top"),
            new Control("button text is larger", "light/none", 440, ".ash-accordion__button{font-size:1rem}", "font-size"),
            new Control("button weight is wrong", "light/none", 440, ".ash-accordion__button{font-weight:600}", "font-weight"),
            new Control("button line height is wrong", "light/none", 440, ".ash-accordion__button{--ash-accordion-leading:1.4}", "line-height"),
            new Control("button gap is wrong", "light/none", 440, ".ash-accordion__button{gap:4px}", "column-gap"),
            new Control("button is not full width", "light/none", 440, ".ash-accordion__button{width:auto}", "width"),
            new Control("button text is not left aligned", "light/none", 440, ".ash-accordion__button{text-align:center}", "text-align"),
            new Control("button title colour is wrong", "light/none", 440, ".ash-accordion__button{--ash-accordion-fg:red}", "color"),
            new Control("button dark title ignores the theme", "dark/none", 440, ".ash-accordion__button{--ash-accordion-fg:var(--color-slate-900)}", "color"),
            new Control("the heading keeps its margin", "light/none", 440, ".ash-accordion__heading{margin:8px}", "margin-top"),
            new Control("the heading is bold", "light/none", 440, ".ash-accordion__heading{font-weight:700}", "font-weight"),
            new Control("the wrapper border is the wrong colour", "light/none", 440, ".ash-accordion{--ash-accordion-border:red}", "border-top-color"),
            new Control("the wrapper dark border ignores the theme", "dark/none", 440, ".ash-accordion{--ash-accordion-border:var(--color-slate-200)}", "border-top-color"),
            new Control("the wrapper border is two pixels", "light/none", 440, ".ash-accordion{border-width:2px}", "border-top-width"),
            new Control("the wrapper radius is wrong", "light/none", 440, ".ash-accordion{border-radius:0}", "border-top-left-radius"),
            new Control("the wrapper does not clip", "light/none", 440, ".ash-accordion{overflow:visible}", "overflow-x"),
            new Control("the divider is missing", "light/none", 440, ".ash-accordion__item:not(:last-child){border-bottom-width:0}", "border-bottom-width"),
            new Control("the divider is the wrong colour", "light/none", 440, ".ash-accordion__item:not(:last-child){border-color:red}", "border-bottom-color"),
            new Control("the last section has a divider", "light/none", 440, ".ash-accordion__item:last-child{border-bottom-width:1px}", "border-bottom-width"),
            new Control("the chevron colour is wrong", "light/none", 440, ".ash-accordion__button{--ash-accordion-chevron:red}", "color"),
            new Control("the dark chevron ignores the theme", "dark/none", 440, ".ash-accordion__button{--ash-accordion-chevron:var(--color-slate-400)}", "color"),
            new Control("the chevron may shrink", "light/none", 440, ".ash-accordion__chevron{flex-shrink:1}", "flex-shrink"),
            new Control("an expanded chevron does not turn", "light/none", 440, EXPANDED_CHEVRON + "{rotate:none}", "rotate"),
            new Control("an expanded chevron turns the wrong way", "light/none", 440, EXPANDED_CHEVRON + "{rotate:-90deg}", "rotate"),
            new Control("a collapsed chevron turns", "light/none", 440, ".ash-accordion__chevron{rotate:90deg}", "rotate"),
            new Control("the chevron transition is slower", "light/none", 440, ".ash-accordion__chevron{transition-duration:300ms}", "transition-duration"),
            new Control("the chevron transition animates the wrong properties", "light/none", 440, ".ash-accordion__chevron{transition-property:color}", "transition-property"),
            new Control("the chevron easing is wrong", "light/none", 440, ".ash-accordion__chevron{transition-timing-function:linear}", "transition-timing-function"),
            new Control("the panel transition is slower", "light/none", 440, ".ash-accordion__panel{transition-duration:300ms}", "transition-duration"),
            new Control("the panel transition animates the wrong property", "light/none", 440, ".ash-accordion__panel{transition-property:all}", "transition-property"),
            new Control("the panel easing is wrong", "light/none", 440, ".ash-accordion__panel{transition-timing-function:linear}", "transition-timing-function"),
            new Control("a collapsed panel stays open", "light/none", 440, ".ash-accordion__panel[inert]{grid-template-rows:1fr}", "grid-template-rows"),
            new Control("an open panel collapses", "light/none", 440, ".ash-accordion__panel:not([inert]){grid-template-rows:0fr}", "grid-template-rows"),
            new Control("the panel is not a grid", "light/none", 440, ".ash-accordion__panel{display:block}", "display"),
            new Control("the clip does not clip", "light/none", 440, ".ash-accordion__clip{overflow:visible}", "overflow-x"),
            new Control("the clip may not shrink", "light/none", 440, ".ash-accordion__clip{min-height:auto}", "min-height"),
            new Control("the content padding is wrong", "light/none", 440, ".ash-accordion__content{padding-bottom:8px}", "padding-bottom"),
            new Control("the content top padding is missing", "light/none", 440, ".ash-accordion__content{padding-top:0}", "padding-top"),
            new Control("the content inline padding is wrong", "light/none", 440, ".ash-accordion__content{padding-inline:8px}", "padding-left"),
            new Control("the content text is larger", "light/none", 440, ".ash-accordion__content{font-size:1rem}", "font-size"),
            new Control("the content line height is wrong", "light/none", 440, ".ash-accordion__content{--ash-accordion-leading:1.6}", "line-height"),
            new Control("the content text colour is wrong", "light/none", 440, ".ash-accordion__content{--ash-accordion-content:red}", "color"),
            new Control("the content dark text ignores the theme", "dark/none", 440, ".ash-accordion__content{--ash-accordion-content:var(--color-slate-700)}", "color"));

    // ---- running ----

    private static String framesFor(String environment, String migrated, String legacyMarkup, String suffix) {
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
        return frames.toString();
    }

    @BeforeAll
    static void measure() throws Exception {
        ParitySession session = ParitySession.open("accordion-parity");
        accordionLegacy = new AccordionLegacyMarkup();
        iconLegacy = new IconLegacyMarkup();
        String environment = ParityFrames.environment(session.generatedDir());
        String migrated = markup(false);
        String legacyMarkup = markup(true);
        migratedForCounting = migrated;

        StringBuilder frames = new StringBuilder(framesFor(environment, migrated, legacyMarkup, ""));
        for (Control control : CONTROL_LIST) {
            ParityConfigs.Config c = ParityConfigs.get(control.config());
            frames.append(sized(ParityFrames.frame("ctl:" + control.name() + "|P", c, ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), control.css(), migrated), control.width()));
        }
        probe = ParityFrames.measure(session, frames.toString(), EXTRA_PROPS, "accordion frames");
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

        // The same measurement with Chrome forced to prefers-reduced-motion: reduce: the legacy motion-reduce:transition-none and the migrated media query must agree.
        reduced = ParityFrames.measure(session, framesFor(environment, migrated, legacyMarkup, "reduced"), EXTRA_PROPS, List.of("--force-prefers-reduced-motion"), "accordion reduced-motion frames");
        for (ParityConfigs.Config c : CONFIGS) {
            String at = c.id() + " @440 (reduced motion)";
            REDUCED.put(at, ParityProbe.compare(at + " (Tailwind present)", reduced.props(), reduced.phase(key(c, "L", 440)), reduced.phase(key(c, "P", 440)), APPLIED_REDUCED));
        }
    }

    private static void compareAll(List<String> props, ParityConfigs.Config c, int width) {
        String at = c.id() + " @" + width;
        List<ParityProbe.Row> legacyRows = probe.phase(key(c, "L", width));
        ROWS.put(key(c, "L", width), legacyRows.size());
        PRESENT.put(at, ParityProbe.compare(at + " (Tailwind present)", props, legacyRows, probe.phase(key(c, "P", width)), APPLIED_PRESENT));
        REMOVED_SCOPED.put(at, ParityProbe.compare(at + " (Tailwind removed vs scoped legacy)", props, probe.phase(key(c, "S", width)), probe.phase(key(c, "R", width)), APPLIED_REMOVED));
        REMOVED_FULL.put(at, ParityProbe.compare(at + " (Tailwind removed vs full legacy, the components' own elements)", props, owned(legacyRows), owned(probe.phase(key(c, "R", width))), APPLIED_REMOVED));
        SCOPED_FIDELITY.put(at, ParityProbe.compare(at + " (scoped legacy vs full legacy, the components' own elements)", props, owned(legacyRows), owned(probe.phase(key(c, "S", width))), APPLIED_SCOPED));
    }

    /** The measurements are tens of thousands of rows; every suite in this JVM is run one after another, so this one lets go of them when it is done. */
    @AfterAll
    static void clear() {
        probe = null;
        reduced = null;
        migratedForCounting = null;
        List.of(PRESENT, REMOVED_SCOPED, REMOVED_FULL, SCOPED_FIDELITY, REDUCED, CONTROLS).forEach(Map::clear);
        List.of(APPLIED_PRESENT, APPLIED_REMOVED, APPLIED_SCOPED, APPLIED_REDUCED, ROWS).forEach(Map::clear);
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

    /** One measured value: kind, group, item id (or null for the accordion), property. */
    private static String one(ParityProbe.Probe from, String phase, String kind, String group, String itemId, String property) {
        String caseName = itemId == null ? group : group + "-" + itemId;
        List<ParityProbe.Row> found = from.phase(phase).stream().filter(r -> r.kind().equals(kind) && r.variant().equals(caseName + "/" + layoutOf(group))).toList();
        assertEquals(1, found.size(), phase + " " + kind + " " + caseName);
        return from.value(found.get(0), property);
    }

    private static String one(String phase, String kind, String group, String itemId, String property) {
        return one(probe, phase, kind, group, itemId, property);
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
        assertEquals(22, REDUCED.size());
    }

    @Test
    void controlledOracleWithTailwindPresentMatchesTheLegacyAccordionInEveryConfigurationCaseAndWidth() {
        assertEquals(List.of(), lines(PRESENT));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheLegacyAccordionGivenTheSameSurroundings() {
        assertEquals(List.of(), lines(REMOVED_SCOPED));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheFullLegacyRendering() {
        assertEquals(List.of(), lines(REMOVED_FULL));
    }

    @Test
    void theScopedLegacyStylesheetRendersTheLegacyAccordionExactlyAsTheFullOneDoes() {
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
    void underReducedMotionNeitherTheChevronNorThePanelTransitionsInEitherRenderingAndTheyAgree() {
        assertEquals(List.of(), lines(REDUCED));
        for (String phase : List.of("light/none|L|440", "light/none|P|440")) {
            for (String kind : List.of("chevron", "panel")) {
                assertEquals("none", one(reduced, phase, kind, "basic", "two", "transition-property"), phase + " " + kind + " must not transition under reduced motion");
            }
        }
        // And the same pages DO transition without it, so the reduced run is not vacuous.
        assertNotEquals("none", one("light/none|P|440", "chevron", "basic", "two", "transition-property"));
        assertNotEquals("none", one("light/none|L|440", "panel", "basic", "two", "transition-property"));
    }

    @Test
    void theButtonIsTailwindsRowWithItsSizeSpacingTypeAndNoBorderInBothRenderings() {
        for (String phase : RENDERINGS) {
            assertEquals("flex", one(phase, "button", "basic", "one", "display"), phase);
            assertEquals("16px", one(phase, "button", "basic", "one", "padding-left"), phase);
            assertEquals("12px", one(phase, "button", "basic", "one", "padding-top"), phase);
            assertEquals("12px", one(phase, "button", "basic", "one", "column-gap"), phase);
            assertEquals("14px", one(phase, "button", "basic", "one", "font-size"), phase);
            assertEquals("20px", one(phase, "button", "basic", "one", "line-height"), phase);
            assertEquals("500", one(phase, "button", "basic", "one", "font-weight"), phase);
            assertEquals("space-between", one(phase, "button", "basic", "one", "justify-content"), phase);
            assertEquals("left", one(phase, "button", "basic", "one", "text-align"), phase);
            assertEquals("0px", one(phase, "button", "basic", "one", "border-bottom-width"), phase);
            assertEquals("rgba(0, 0, 0, 0)", one(phase, "button", "basic", "one", "background-color"), phase);
            assertEquals("44", one(phase, "button", "basic", "one", "@h"), phase + ": 12 + 20 + 12");
            assertEquals(one(phase, "item", "basic", "one", "@w"), one(phase, "button", "basic", "one", "@w"), phase + ": full width");
            assertEquals("0px", one(phase, "heading", "basic", "one", "margin-top"), phase);
            assertEquals(one(phase, "item", "basic", "one", "font-size"), one(phase, "heading", "basic", "one", "font-size"), phase + ": the heading inherits its size, it is not an h3 size");
            assertEquals(one(phase, "item", "basic", "one", "font-weight"), one(phase, "heading", "basic", "one", "font-weight"), phase + ": and its weight");
            assertEquals("16", one(phase, "chevron-svg", "basic", "one", "@w"), phase);
            assertEquals("0", one(phase, "chevron", "basic", "one", "flex-shrink"), phase);
        }
    }

    @Test
    void theWrapperIsABorderedRoundedClippedBoxWithARuleUnderEverySectionButTheLast() {
        for (String phase : RENDERINGS) {
            assertEquals("1px", one(phase, "accordion", "basic", null, "border-top-width"), phase);
            assertEquals("8px", one(phase, "accordion", "basic", null, "border-top-left-radius"), phase);
            assertEquals("hidden", one(phase, "accordion", "basic", null, "overflow-x"), phase);
            assertEquals("1px", one(phase, "item", "basic", "one", "border-bottom-width"), phase);
            assertEquals("1px", one(phase, "item", "basic", "two", "border-bottom-width"), phase);
            assertEquals("0px", one(phase, "item", "basic", "three", "border-bottom-width"), phase + ": no rule under the last section");
            assertEquals(one(phase, "accordion", "basic", null, "border-top-color"), one(phase, "item", "basic", "one", "border-bottom-color"), phase + ": the rule and the border are one colour");
        }
    }

    @Test
    void anExpandedSectionShowsItsContentAndTurnsItsChevronAndACollapsedOneIsAZeroRow() {
        for (String phase : RENDERINGS) {
            assertEquals("90deg", one(phase, "chevron", "basic", "two", "rotate"), phase + ": the expanded chevron is a quarter turn");
            assertEquals("none", one(phase, "chevron", "basic", "one", "rotate"), phase + ": a collapsed chevron is not turned");
            assertEquals("grid", one(phase, "panel", "basic", "two", "display"), phase);
            assertTrue(number(one(phase, "panel", "basic", "two", "@h")) > 40, phase + ": an expanded panel is as tall as its content");
            assertEquals("0", one(phase, "panel", "basic", "one", "@h"), phase + ": a collapsed panel has no height");
            assertEquals("0", one(phase, "panel", "closed", "three", "@h"), phase);
            assertEquals("hidden", one(phase, "clip", "basic", "one", "overflow-y"), phase);
            assertEquals("0px", one(phase, "clip", "basic", "one", "min-height"), phase);
            assertEquals("16px", one(phase, "content", "basic", "two", "padding-left"), phase);
            assertEquals("4px", one(phase, "content", "basic", "two", "padding-top"), phase);
            assertEquals("16px", one(phase, "content", "basic", "two", "padding-bottom"), phase);
            assertEquals("14px", one(phase, "content", "basic", "two", "font-size"), phase);
            assertEquals("20px", one(phase, "content", "basic", "two", "line-height"), phase);
        }
    }

    @Test
    void theTransitionsAreTwoHundredMillisecondsEaseOutOnTheExactPropertiesInBothRenderings() {
        for (String phase : RENDERINGS) {
            assertEquals("transform, translate, scale, rotate", one(phase, "chevron", "basic", "one", "transition-property"), phase);
            assertEquals("0.2s", one(phase, "chevron", "basic", "one", "transition-duration"), phase);
            assertEquals("cubic-bezier(0, 0, 0.2, 1)", one(phase, "chevron", "basic", "one", "transition-timing-function"), phase);
            assertEquals("grid-template-rows", one(phase, "panel", "basic", "one", "transition-property"), phase);
            assertEquals("0.2s", one(phase, "panel", "basic", "one", "transition-duration"), phase);
            assertEquals("cubic-bezier(0, 0, 0.2, 1)", one(phase, "panel", "basic", "one", "transition-timing-function"), phase);
        }
    }

    @Test
    void aNarrowColumnWrapsLongTitlesAndKeepsTheChevronWholeAtTheRightEdge() {
        for (String phase : RENDERINGS) {
            assertTrue(number(one(phase, "button", "narrow", "one", "@h")) > 44, phase + ": a long title wraps onto more lines");
            assertEquals("16", one(phase, "chevron-svg", "narrow", "one", "@w"), phase + ": the chevron does not shrink");
            assertEquals("220", one(phase, "accordion", "narrow", null, "@w"), phase);
        }
    }

    @Test
    void darkModeUsesTailwindsDarkColoursInBothRenderings() {
        for (String render : List.of("L", "P", "R")) {
            String light = "light/none|" + render + "|440";
            String dark = "dark/none|" + render + "|440";
            for (String[] part : new String[][] {{"button", "basic", "one", "color"}, {"chevron", "basic", "one", "color"}, {"accordion", "basic", null, "border-top-color"},
                    {"item", "basic", "one", "border-bottom-color"}, {"content", "basic", "two", "color"}}) {
                assertNotEquals(one(light, part[0], part[1], part[2], part[3]), one(dark, part[0], part[1], part[2], part[3]), render + " " + part[0] + " " + part[3]);
            }
        }
    }

    @Test
    void theCasesAreCompleteAndEveryPartIsMeasured() {
        List<ParityProbe.Row> rows = probe.phase("light/none|L|440");
        Map<String, Integer> byKind = new TreeMap<>();
        rows.forEach(r -> byKind.merge(r.kind(), 1, Integer::sum));
        for (String kind : List.of("accordion", "item", "heading", "button", "title", "chevron", "chevron-svg", "panel", "clip", "content")) {
            int written = count(migratedForCounting, "data-parity=\"" + kind + "\"");
            assertTrue(written > 0, kind);
            assertEquals(written, byKind.get(kind), kind + " rows");
        }
        assertEquals(GROUPS.size(), byKind.get("accordion"));
        assertEquals(GROUPS.stream().mapToInt(g -> g.items().size()).sum(), byKind.get("item"));
        assertEquals(byKind.get("item"), byKind.get("panel"), "one panel per section");
        for (String key : ROWS.keySet().stream().filter(k -> k.startsWith("light/none|L|")).toList()) {
            assertEquals(ROWS.get("light/none|L|440"), ROWS.get(key), "every width measures the same elements: " + key);
        }
    }

    @Test
    void theOracleIsNotVacuousEveryLegacyClassIsCompiledInTheCumulativeAndScopedStylesheets() throws IOException {
        Path dir = Path.of(System.getProperty("ashlar.cascade.dir"));
        String oracle = Files.readString(dir.resolve("accordion-oracle.css"), StandardCharsets.UTF_8);
        String scoped = Files.readString(dir.resolve("accordion-legacy-scoped.css"), StandardCharsets.UTF_8);
        List<String> tokens = new ArrayList<>(accordionLegacy.allTokens());
        // ParityOracle recognises a class selector followed by a block, a pseudo-class or a comma; Tailwind writes the variants and arbitrary values differently, so those
        // are looked up by their escaped selector below.
        List<String> plain = tokens.stream().filter(t -> !t.contains(":") && !t.contains("[") && !t.contains("/") && !t.startsWith("divide-")).toList();
        assertEquals(List.of(), ParityOracle.uncompiled(oracle, plain), "the oracle does not compile these");
        assertEquals(List.of(), ParityOracle.uncompiled(scoped, plain), "the scoped stylesheet does not compile these");
        for (String escaped : List.of(".divide-y", ".divide-slate-200", "hover\\:bg-slate-50", "focus-visible\\:ring-inset", "dark\\:hover\\:bg-slate-800\\/50", "dark\\:divide-slate-700", "motion-reduce\\:transition-none",
                "transition-\\[grid-template-rows\\]", "dark\\:border-slate-700", "dark\\:text-slate-100", "dark\\:text-slate-500", "dark\\:text-slate-300")) {
            assertTrue(oracle.contains(escaped) && scoped.contains(escaped), "not compiled: " + escaped);
        }
        List<String> earlier = new ArrayList<>();
        earlier.addAll(new TabsLegacyMarkup().allTokens().stream().filter(t -> !t.contains(".")).toList());
        earlier.addAll(new NavigationLegacyMarkup().allTokens().stream().filter(t -> !t.contains("[")).toList());
        earlier.addAll(new CardLegacyMarkup().allTokens());
        earlier.addAll(new IconLegacyMarkup().allTokens());
        assertEquals(List.of(), ParityOracle.uncompiled(oracle, earlier.stream().filter(t -> !t.contains(":") && !t.contains("/")).toList()), "the oracle lost earlier legacy classes");
        assertFalse(scoped.contains("html,:host"), "the scoped legacy stylesheet must not carry Tailwind's page-wide base");
    }

    @Test
    void normalizationsStayWithinTheApprovedRules() {
        assertEquals(Map.of(), APPLIED_PRESENT, "Tailwind present needs no normalization: nothing here is a pill or a Tailwind radius value");
        assertEquals(Map.of(), APPLIED_SCOPED, "the scoped legacy rendering must match the full one without any normalization");
        assertEquals(Map.of(), APPLIED_REDUCED, "reduced motion needs no normalization");
        assertTrue(Set.of("H2").containsAll(APPLIED_REMOVED.keySet()), "unexpected normalization rule used: " + APPLIED_REMOVED);
    }

    // ---- what the real pages serve ----

    private static final Pattern TAILWIND_ON_A_HOOK = Pattern.compile("class=\"[^\"]*(?:divide-y|rotate-90|min-h-0|justify-between|rounded-lg)[^\"]*\"[^>]*data-accordion");

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }

    @Test
    void everyAccordionTheRealPagesServeKeepsItsNativeStructureRolesAndPairingAndNoInlineStyleOrStateClass() throws Exception {
        ParitySession session = ParitySession.open("accordion-parity");
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
                for (String role : AccordionLegacyMarkup.MIGRATED.keySet()) {
                    totals.merge(role, AccordionLegacyMarkup.count(body, role), Integer::sum);
                }
                accordionLegacy.rewrite(body);
                int roots = AccordionLegacyMarkup.count(body, "accordion");
                int items = AccordionLegacyMarkup.count(body, "item");
                int buttons = AccordionLegacyMarkup.count(body, "accordion-button");
                int panels = AccordionLegacyMarkup.count(body, "accordion-panel");
                // Counted as tags: the documentation also mentions these attributes in prose.
                long rootTags = Pattern.compile("<div id=\"[^\"]+\" data-accordion class=\"ash-accordion\">").matcher(body).results().count();
                long itemTags = Pattern.compile("<div data-accordion-item class=\"ash-accordion__item\">").matcher(body).results().count();
                long buttonTags = Pattern.compile("<button\\b[^>]*\\bdata-accordion-button\\b[^>]*class=\"ash-accordion__button\"").matcher(body).results().count();
                long panelTags = Pattern.compile("<div\\b[^>]*\\bdata-accordion-panel\\b[^>]*\\brole=\"region\"[^>]*class=\"ash-accordion__panel\"").matcher(body).results().count();
                long chevronTags = Pattern.compile("<span data-accordion-chevron class=\"ash-accordion__chevron\">").matcher(body).results().count();
                if (rootTags != roots || itemTags != items || buttonTags != buttons || panelTags != panels || buttons != items || panels != items || chevronTags != items) {
                    problems.add(path + ": hooks/classes differ: roots " + rootTags + "/" + roots + ", items " + itemTags + "/" + items + ", buttons " + buttonTags + "/" + buttons
                            + ", panels " + panelTags + "/" + panels + ", chevrons " + chevronTags);
                }
                if (Pattern.compile("<div\\b[^>]*\\bdata-accordion-panel\\b[^>]*\\bstyle=").matcher(body).find()) {
                    problems.add(path + ": a panel still renders an inline style");
                }
                if (Pattern.compile("<span data-accordion-chevron[^>]*rotate-90").matcher(body).find()) {
                    problems.add(path + ": a chevron still renders rotate-90");
                }
                long expanded = Pattern.compile("<button\\b[^>]*>").matcher(body).results().map(r -> r.group()).filter(t -> t.contains("data-accordion-button") && t.contains("aria-expanded=\"true\"")).count();
                long open = Pattern.compile("<div\\b(?![^>]*\\binert\\b)[^>]*\\bdata-accordion-panel\\b").matcher(body).results().count();
                if (expanded != open) {
                    problems.add(path + ": aria-expanded=true (" + expanded + ") and not-inert panels (" + open + ") disagree");
                }
                if (TAILWIND_ON_A_HOOK.matcher(body).find()) {
                    problems.add(path + ": an accordion still renders its Tailwind classes");
                }
            }
        }
        assertEquals(List.of(), problems);
        for (String role : AccordionLegacyMarkup.MIGRATED.keySet()) {
            assertTrue(totals.getOrDefault(role, 0) > 0, "no " + role + " rendered on any page: " + totals);
        }
        assertEquals(totals.get("item"), totals.get("accordion-panel"));
        assertEquals(totals.get("item"), totals.get("accordion-clip"));
    }
}
