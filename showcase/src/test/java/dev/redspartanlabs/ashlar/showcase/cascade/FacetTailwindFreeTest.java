package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static dev.redspartanlabs.ashlar.showcase.cascade.FacetParityTest.id;
import static dev.redspartanlabs.ashlar.showcase.cascade.FacetParityTest.nth;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Faceted Search, Facet and Facet Option in a page that has Ashlar's stylesheet and NO Tailwind, and so no Tailwind Preflight. The parity fixtures all load Tailwind utilities (a host app
 * has them), so on their own they cannot see a component that quietly depends on a host utility or a Preflight reset: the native checkbox's browser margin (Preflight zeroes it), a
 * fieldset's border, padding and minimum width, a legend's padding. This page loads only {@code ashlar.css}.
 *
 * <p>Four kinds of evidence. (1) Absolute, from the migrated rendering alone: a plain {@code h-4 w-4} box is not 16px (Tailwind is absent); a native checkbox is 16 by 16px with no margin;
 * a fieldset has no border width, no padding, no margin and no minimum width; a legend has no padding and 6px below it; the facets are 20px apart and the options stack with no gap; a row
 * is a 10px-gapped, centered flex row with 6px and 8px padding; a count is 12px/16px with tabular figures; the announced count is visually hidden; the focus ring keeps its white offset in
 * both themes; a disabled row dims as a whole. (2) The scoped legacy rendering (only the utilities and the few preflight rules the facets used, see {@code facet-legacy-scoped-entry.css})
 * must equal the FULL legacy rendering, so it is a faithful stand-in for "the legacy facets without Tailwind's page-wide reset". (3) The migrated rendering with Ashlar alone must equal that
 * scoped legacy rendering, element by element, in six configurations covering both themes and skins that change a radius, a fill or the focus colour. (4) The trusted-input behaviour
 * sequence of {@link FacetBehaviourTest} gives the same transcript without Tailwind as with it. BOTH sides carry the showcase's own dark {@code color-scheme}, which changes how the
 * browser draws a native control.
 *
 * <p>What is not compared, and why: the style and the colour of a zero-width border on the fieldset and the native input (Preflight's blanket {@code border: 0 solid currentcolor} gives
 * every element an invisible {@code solid} style the bare page lacks), the outline colour of the native input (its outline style is none), and on the native checkbox INPUT the text
 * properties {@code font-size}, {@code line-height}, {@code letter-spacing}, {@code text-transform} and {@code color}, which Preflight's form-control reset inherits and which the
 * browser's own control has no text to apply them to (a skin that sets tabular figures on the page shows it for {@code font-variant-numeric}); and the box-sizing of the fixture's own
 * column, the first element of every field, which is not part of a component (it has no padding and no border, so its box is the same, and its box is compared). Fully transparent zero-size shadow layers are normalised as in {@link FacetParityTest}. Nothing else is excluded.
 *
 * <p>Firefox and Safari are not covered: only Chrome is driven.
 */
class FacetTailwindFreeTest {

    private static final List<String> CONFIGS = List.of("light/none", "dark/none", "light/ledger", "light/foundry", "dark/axiom", "dark/harbor");
    private static final Set<String> INPUT_TEXT_PROPERTIES = Set.of("font-size", "line-height", "letter-spacing", "text-transform", "color", "font-variant-numeric");

    private static final List<String> FINDINGS_SCOPED_VS_FULL = new ArrayList<>();
    private static final List<String> FINDINGS_FREE_VS_SCOPED = new ArrayList<>();
    private static final Map<String, Integer> APPLIED = new LinkedHashMap<>();
    private static final Map<String, String> FREE_LIGHT = new LinkedHashMap<>();
    private static final Map<String, String> FREE_DARK = new LinkedHashMap<>();
    private static String controlProbe;
    private static int configurations;
    private static List<String> legacyTranscript;
    private static List<String> freeTranscript;

    private static String insertEnvironment(String page, String environment) {
        return page.replace("</head>", "<style>" + environment + "</style></head>");
    }

    /** Drops the lines this suite does not compare (see the class comment) from a measurement. */
    private static String comparable(String measurement) {
        return measurement.lines().filter(l -> {
            String[] p = l.split("\t", -1);
            if (p.length != 3) {
                return true;
            }
            if (p[1].equals("border-top-style")) {
                return false;
            }
            if (p[0].endsWith(":0:div") && p[1].equals("box-sizing")) {
                return false;
            }
            if ((p[0].endsWith(":fieldset") || p[0].endsWith(":input")) && p[1].matches("border-(?:top|right|bottom|left)-color")) {
                return false;
            }
            if (p[0].endsWith(":input") && p[1].equals("outline-color")) {
                return false;
            }
            return !(p[0].endsWith(":input") && INPUT_TEXT_PROPERTIES.contains(p[1]));
        }).collect(Collectors.joining("\n"));
    }

    @BeforeAll
    static void measure() throws Exception {
        ParitySession session = ParitySession.open("facet-tailwind-free");
        List<FacetParityTest.Field> fields = FacetParityTest.fields();
        String[] bodies = FacetParityTest.bodies(fields);
        String probe = "<div id=\"probe\" class=\"h-4 w-4\"></div>";
        String environment = ParityFrames.environment(session.generatedDir());
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            for (String id : CONFIGS) {
                ParityConfigs.Config config = ParityConfigs.get(id);
                String full = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("facet-oracle.css")), config, "", bodies[1] + probe, ""));
                String scoped = server.registerRaw(insertEnvironment(TextEntryBrowser.page(List.of(ParityFrames.generated("facet-legacy-scoped.css")), config, "", bodies[1] + probe, ""), environment));
                String free = server.registerRaw(insertEnvironment(TextEntryBrowser.page(List.of(ParityFrames.ASHLAR), config, "", bodies[0] + probe, ""), environment));
                String fullM;
                String scopedM;
                String freeM;
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    fullM = FacetParityTest.measure(cdp, full, fields);
                    scopedM = FacetParityTest.measure(cdp, scoped, fields);
                    freeM = FacetParityTest.measure(cdp, free, fields);
                    if (id.equals("light/none")) {
                        controlProbe = cdp.evaluate("(function(){var p=document.getElementById('probe').getBoundingClientRect();return p.width+'x'+p.height})()");
                    }
                }
                configurations++;
                for (FacetParityTest.Field f : fields) {
                    String prefix = f.id() + ":";
                    String where = id + " " + f.id() + " (" + f.spec().key() + ")";
                    FINDINGS_SCOPED_VS_FULL.addAll(TextEntryBrowser.differences(where + " scoped-vs-full", comparable(FacetParityTest.slice(fullM, prefix)), comparable(FacetParityTest.slice(scopedM, prefix)), APPLIED));
                    FINDINGS_FREE_VS_SCOPED.addAll(TextEntryBrowser.differences(where + " free-vs-scoped", comparable(FacetParityTest.slice(scopedM, prefix)), comparable(FacetParityTest.slice(freeM, prefix)), APPLIED));
                }
                if (id.equals("light/none") || id.equals("dark/none")) {
                    Map<String, String> target = id.startsWith("light") ? FREE_LIGHT : FREE_DARK;
                    for (String line : freeM.split("\n")) {
                        String[] p = line.split("\t", -1);
                        if (p.length == 3) {
                            target.put(p[0] + "|" + p[1], p[2]);
                        }
                    }
                }
            }
            // The behaviour sequence, with Tailwind (legacy) and without it (migrated, Ashlar alone), each in its own browser.
            ParityConfigs.Config light = ParityConfigs.get("light/none");
            String script = "<script>" + FacetBehaviourTest.SNAPSHOT + "</script>";
            String legacyUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("facet-oracle.css")), light, "", new FacetLegacyMarkup().rewrite(FacetBehaviourTest.BODY), script));
            String freeUrl = server.registerRaw(insertEnvironment(TextEntryBrowser.page(List.of(ParityFrames.ASHLAR), light, "", FacetBehaviourTest.BODY, script), environment));
            legacyTranscript = FacetBehaviourTest.transcript(session, legacyUrl, FacetBehaviourTest.steps());
            freeTranscript = FacetBehaviourTest.transcript(session, freeUrl, FacetBehaviourTest.steps());
        }
    }

    private static Map<String, Integer> byProperty(List<String> findings) {
        Map<String, Integer> counts = new java.util.TreeMap<>();
        for (String finding : findings) {
            String[] p = finding.split(" \\| ");
            counts.merge(p.length > 2 ? p[1].replaceAll("^\\S+:\\d+:", "") + "." + p[2] : p[1], 1, Integer::sum);
        }
        return counts;
    }

    @Test
    void tailwindIsReallyAbsentFromThePage() {
        assertNotEquals("16x16", controlProbe, "a plain h-4 w-4 box must NOT be 16px here: " + controlProbe);
    }

    @Test
    void theMigratedFacetsStandOnTheirOwnWithoutTailwind() {
        Map<String, String> l = FREE_LIGHT;
        String f = id("counts and a checked option");
        // Elements of the states fixture: div#0 wrapper, div#1 faceted-search, fieldset, legend, div#2 options, then per option label, input, span (text), span (count), span (announced).
        assertEquals("flex", nth(l, f, "div", 1, "display"));
        assertEquals("column", nth(l, f, "div", 1, "flex-direction"));
        assertEquals("20px", nth(l, f, "div", 1, "row-gap"));
        assertEquals("0px", nth(l, f, "div", 1, "min-width"));
        assertEquals("0px", nth(l, f, "fieldset", 0, "border-top-width"));
        assertEquals("0px", nth(l, f, "fieldset", 0, "border-left-width"));
        assertEquals("0px", nth(l, f, "fieldset", 0, "padding-top"));
        assertEquals("0px", nth(l, f, "fieldset", 0, "padding-left"));
        assertEquals("0px", nth(l, f, "fieldset", 0, "margin-left"), "the browser's own 2px fieldset margin is gone: Preflight used to remove it and the component states it");
        assertEquals("0px", nth(l, f, "fieldset", 0, "margin-bottom"));
        assertEquals("0px", nth(l, f, "fieldset", 0, "min-width"), "the browser's min-content minimum width is gone");
        assertEquals("0px", nth(l, f, "legend", 0, "padding-left"), "the browser's 2px legend padding is gone");
        assertEquals("0px", nth(l, f, "legend", 0, "padding-right"));
        assertEquals("6px", nth(l, f, "legend", 0, "margin-bottom"));
        assertEquals("0px", nth(l, f, "legend", 0, "margin-left"));
        assertEquals("14px", nth(l, f, "legend", 0, "font-size"));
        assertEquals("20px", nth(l, f, "legend", 0, "line-height"));
        assertEquals("600", nth(l, f, "legend", 0, "font-weight"));
        assertEquals("column", nth(l, f, "div", 2, "flex-direction"));
        assertEquals("normal", nth(l, f, "div", 2, "row-gap"), "the options have no gap");
        assertEquals("flex", nth(l, f, "label", 0, "display"));
        assertEquals("center", nth(l, f, "label", 0, "align-items"));
        assertEquals("10px", nth(l, f, "label", 0, "column-gap"));
        assertEquals("6px", nth(l, f, "label", 0, "padding-top"));
        assertEquals("6px", nth(l, f, "label", 0, "padding-bottom"));
        assertEquals("8px", nth(l, f, "label", 0, "padding-left"));
        assertEquals("8px", nth(l, f, "label", 0, "padding-right"));
        assertEquals("14px", nth(l, f, "label", 0, "font-size"));
        assertEquals("20px", nth(l, f, "label", 0, "line-height"));
        assertEquals("pointer", nth(l, f, "label", 0, "cursor"));
        assertEquals("16px", nth(l, f, "input", 0, "width"));
        assertEquals("16px", nth(l, f, "input", 0, "height"));
        assertEquals("0px", nth(l, f, "input", 0, "margin-top"), "the browser's own 3px margin is gone");
        assertEquals("0px", nth(l, f, "input", 0, "margin-left"), "and its 4px one");
        assertEquals("0px", nth(l, f, "input", 0, "margin-bottom"));
        assertEquals("auto", nth(l, f, "input", 0, "appearance"), "the control stays native");
        assertEquals("pointer", nth(l, f, "input", 0, "cursor"));
        assertEquals("not-allowed", nth(l, f, "input", 3, "cursor"));
        assertEquals("1", nth(l, f, "input", 3, "opacity"), "a disabled checkbox is not dimmed twice");
        assertEquals("0.6", nth(l, f, "label", 3, "opacity"), "a disabled row is dimmed as a whole");
        assertEquals("not-allowed", nth(l, f, "label", 3, "cursor"));
        assertEquals("break-word", nth(l, f, "span", 0, "overflow-wrap"));
        assertEquals("1", nth(l, f, "span", 0, "flex-grow"));
        assertEquals("0px", nth(l, f, "span", 0, "min-width"));
        assertEquals("12px", nth(l, f, "span", 1, "font-size"));
        assertEquals("16px", nth(l, f, "span", 1, "line-height"));
        assertEquals("tabular-nums", nth(l, f, "span", 1, "font-variant-numeric"));
        assertEquals("0", nth(l, f, "span", 1, "flex-shrink"), "the visible count never shrinks");
        assertEquals("absolute", nth(l, f, "span", 2, "position"), "the announced count is visually hidden");
        assertEquals("1px", nth(l, f, "span", 2, "width"));
        assertEquals("1px", nth(l, f, "span", 2, "height"));
        assertEquals("hidden", nth(l, f, "span", 2, "overflow-x"));
        assertNotEquals(nth(l, f, "span", 0, "color"), nth(l, f, "span", 1, "color"), "the count is muted");
        assertNotEquals(nth(l, f, "label", 0, "color"), nth(l, f, "legend", 0, "color"), "the legend is darker than the text");
    }

    @Test
    void theFocusRingKeepsItsWhiteOffsetAndAHoveredRowIsFilledEvenWithoutTailwind() {
        String ring = nth(FREE_LIGHT, id("focus-visible on an option"), "input", 1, "box-shadow");
        assertTrue(TextEntryBrowser.visibleShadow(ring).startsWith("rgb(255, 255, 255) 0px 0px 0px 2px, "), "the focus ring keeps its white offset: " + ring);
        assertTrue(TextEntryBrowser.visibleShadow(nth(FREE_DARK, id("focus-visible on an option"), "input", 1, "box-shadow")).startsWith("rgb(255, 255, 255) 0px 0px 0px 2px, "), "in dark mode too");
        assertEquals("none", nth(FREE_LIGHT, id("focus only"), "input", 1, "box-shadow"), "a plain focus draws no ring");
        assertEquals("none", nth(FREE_LIGHT, id("focus only"), "input", 1, "outline-style"));
        assertEquals("rgba(0, 0, 0, 0)", nth(FREE_LIGHT, id("counts and a checked option"), "label", 1, "background-color"));
        assertNotEquals("rgba(0, 0, 0, 0)", nth(FREE_LIGHT, id("hover on an option"), "label", 1, "background-color"));
        assertEquals("rgba(0, 0, 0, 0)", nth(FREE_LIGHT, id("hover on a disabled option"), "label", 3, "background-color"), "a disabled row has no hover fill");
        assertNotEquals(nth(FREE_LIGHT, id("hover on an option"), "label", 1, "background-color"), nth(FREE_DARK, id("hover on an option"), "label", 1, "background-color"), "dark mode fills it differently");
        assertNotEquals(nth(FREE_LIGHT, id("single facet"), "input", 0, "background-color"), nth(FREE_DARK, id("single facet"), "input", 0, "background-color"), "the checkbox background follows the theme");
    }

    @Test
    void theScopedLegacyRenderingEqualsTheFullLegacyRendering() {
        assertEquals(6, configurations);
        assertEquals(List.of(), FINDINGS_SCOPED_VS_FULL.stream().limit(40).toList(), FINDINGS_SCOPED_VS_FULL.size() + " differences, by property: " + byProperty(FINDINGS_SCOPED_VS_FULL));
    }

    @Test
    void theMigratedFacetsWithoutTailwindEqualTheLegacyFacetsWithoutPageWideTailwind() {
        assertEquals(List.of(), FINDINGS_FREE_VS_SCOPED.stream().limit(40).toList(), FINDINGS_FREE_VS_SCOPED.size() + " differences, by property: " + byProperty(FINDINGS_FREE_VS_SCOPED));
    }

    @Test
    void onlyInvisibleShadowLayersWereNormalised() {
        assertTrue(List.of("invisible-shadow-layers").containsAll(APPLIED.keySet()), APPLIED.toString());
    }

    @Test
    void theTrustedInputSequenceGivesTheSameTranscriptWithoutTailwindAsWithIt() {
        assertEquals(FacetBehaviourTest.steps().size(), freeTranscript.size());
        List<String> differences = new ArrayList<>();
        for (int i = 0; i < freeTranscript.size(); i++) {
            if (!freeTranscript.get(i).equals(legacyTranscript.get(i))) {
                differences.add("step #" + i + " " + FacetBehaviourTest.steps().get(i).action() + "\n  legacy: " + legacyTranscript.get(i) + "\n  free:   " + freeTranscript.get(i));
            }
        }
        assertEquals(List.of(), differences);
    }
}
