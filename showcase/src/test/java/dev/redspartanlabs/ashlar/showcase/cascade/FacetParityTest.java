package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Visual parity of Faceted Search, Facet and Facet Option with Tailwind PRESENT, in all 22 configurations (two themes, no skin and the ten skins): the legacy rendering (the real
 * templates' markup rewritten to the Tailwind classes they used to write, with the cumulative Tailwind oracle) against the migrated rendering (the real application stylesheet, Tailwind
 * and Ashlar together). Every element of every field is measured for {@link FacetBrowser#PROPERTIES}, with its box, in every state the three components have: checked, unchecked,
 * disabled and disabled-and-checked, with a count, without one, with a count of 0 and of 1, a long label that wraps and one that has no break to wrap at, a narrow column, several
 * facets, text that needs escaping, a facet with no options, an option outside any facet, and the forced hover (on a plain, a checked and a disabled option, and on the legend), active,
 * focus-visible (on a plain, a checked and a disabled option) and plain focus.
 *
 * <p>Focus and hover are forced on the real element with the browser's own pseudo-state API, on a browser that is stated to be able to hover. The comparison is EXACT except one
 * documented normalisation, the same as Text Entry's and Choice's: the computed {@code box-shadow} with its fully transparent zero-size layers removed
 * ({@link TextEntryBrowser#visibleShadow}), because Tailwind composes the focus ring from unused layers and the migrated rule writes only what it draws. Every visible layer, the white
 * offset included, is compared exactly. The native checkbox's own painting is compared in pixels by {@link FacetPixelTest}. Nothing is left out of the comparison.
 */
class FacetParityTest {

    private static final List<String> FINDINGS = new ArrayList<>();
    private static final Map<String, Integer> APPLIED = new LinkedHashMap<>();
    private static final Map<String, String> LEGACY_LIGHT = new LinkedHashMap<>();
    private static final Map<String, String> LEGACY_DARK = new LinkedHashMap<>();
    private static int configurations;
    private static int measuredFields;
    private static int measuredValues;
    private static boolean rewritten;

    record Field(String id, FacetBrowser.Case spec, String markup) {
    }

    static List<Field> fields() {
        List<Field> fields = new ArrayList<>();
        int i = 0;
        for (FacetBrowser.Case c : FacetBrowser.CASES) {
            String id = "fx" + (++i);
            fields.add(new Field(id, c, FacetBrowser.build(c, id)));
        }
        return fields;
    }

    static String measure(DevToolsSession cdp, String url, List<Field> fields) throws Exception {
        cdp.navigate(url);
        String sheets = cdp.evaluate("(function(){var ok=[].every.call(document.styleSheets,function(s){try{return s.cssRules.length>0}catch(e){return false}});return document.styleSheets.length+':'+ok})()");
        assertTrue(sheets.endsWith(":true") && !sheets.startsWith("0:"), "every stylesheet must have loaded - infrastructure failure, not a parity result: " + sheets);
        StringBuilder all = new StringBuilder();
        for (Field f : fields) {
            List<String> pseudo = f.spec().pseudo();
            List<Integer> nodes = pseudo.isEmpty() ? List.of() : cdp.nodes(FacetBrowser.target(f.spec(), f.id()));
            if (!pseudo.isEmpty()) {
                assertEquals(1, nodes.size(), "exactly one element to force the state on: " + f.id() + " " + f.spec().key());
                cdp.force(nodes, pseudo);
            }
            all.append(cdp.evaluate(FacetBrowser.measureScript(f.id()))).append('\n');
            if (!pseudo.isEmpty()) {
                cdp.force(nodes, List.of());
            }
        }
        return all.toString();
    }

    static String slice(String all, String prefix) {
        StringBuilder sb = new StringBuilder();
        for (String line : all.split("\n")) {
            if (line.startsWith(prefix)) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString().stripTrailing();
    }

    /** The migrated markup of every field, marked, and the same markup rewritten to the legacy classes. */
    static String[] bodies(List<Field> fields) throws Exception {
        StringBuilder migrated = new StringBuilder();
        fields.forEach(f -> migrated.append(FacetBrowser.marked(f.markup())));
        String legacy = new FacetLegacyMarkup().rewrite(migrated.toString());
        return new String[] {migrated.toString(), legacy};
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("facet-parity");
        List<Field> fields = fields();
        String[] bodies = bodies(fields);
        rewritten = !bodies[0].equals(bodies[1]);
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            for (ParityConfigs.Config config : ParityConfigs.all()) {
                String legacyUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("facet-oracle.css")), config, "", bodies[1], ""));
                String migratedUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.APP), config, "", bodies[0], ""));
                String legacy;
                String migrated;
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    legacy = measure(cdp, legacyUrl, fields);
                    migrated = measure(cdp, migratedUrl, fields);
                }
                configurations++;
                measuredFields += fields.size();
                measuredValues += legacy.split("\n").length;
                if (config.id().equals("light/none") || config.id().equals("dark/none")) {
                    Map<String, String> target = config.theme().equals("light") ? LEGACY_LIGHT : LEGACY_DARK;
                    for (String line : legacy.split("\n")) {
                        String[] p = line.split("\t", -1);
                        if (p.length == 3) {
                            target.put(p[0] + "|" + p[1], p[2]);
                        }
                    }
                }
                for (Field f : fields) {
                    FINDINGS.addAll(TextEntryBrowser.differences(config.id() + " " + f.id() + " (" + f.spec().key() + ")", slice(legacy, f.id() + ":"), slice(migrated, f.id() + ":"), APPLIED));
                }
            }
        }
    }

    /** The {@code n}th (from 0) element with this tag in the field, in document order: its value of the property. */
    static String nth(Map<String, String> map, String field, String tag, int n, String property) {
        int seen = 0;
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (e.getKey().startsWith(field + ":") && e.getKey().contains(":" + tag + "|") && e.getKey().endsWith("|" + property)) {
                if (seen++ == n) {
                    return e.getValue();
                }
            }
        }
        throw new AssertionError("no " + property + " of " + tag + " #" + n + " in " + field);
    }

    static String id(String key) {
        for (int i = 0; i < FacetBrowser.CASES.size(); i++) {
            if (FacetBrowser.CASES.get(i).key().equals(key)) {
                return "fx" + (i + 1);
            }
        }
        throw new AssertionError("no case " + key);
    }

    private static Map<String, Integer> summary() {
        Map<String, Integer> byProperty = new java.util.TreeMap<>();
        for (String finding : FINDINGS) {
            String[] p = finding.split(" \\| ");
            byProperty.merge(p.length > 2 ? p[2] : p[1], 1, Integer::sum);
        }
        return byProperty;
    }

    private static List<String> firstOfEach() {
        Map<String, String> first = new java.util.TreeMap<>();
        for (String finding : FINDINGS) {
            String[] p = finding.split(" \\| ");
            first.putIfAbsent(p.length > 2 ? p[2] : p[1], finding);
        }
        return List.copyOf(first.values());
    }

    @Test
    void theMigratedFacetsAreIdenticalToTheLegacyOnesInAll22Configurations() {
        assertEquals(22, configurations);
        assertEquals(List.of(), FINDINGS.stream().limit(60).toList(), FINDINGS.size() + " differences, by property: " + summary() + "; first of each: " + firstOfEach());
    }

    @Test
    void theLegacyRenderingIsReallyADifferentMarkupFromTheMigratedOne() {
        assertTrue(rewritten, "the two sides must not be the same markup, or the comparison would compare a page with itself");
    }

    @Test
    void theMeasurementCoversEveryFixtureAndIsLargeEnoughToMeanSomething() {
        assertEquals(22 * FacetBrowser.CASES.size(), measuredFields);
        assertTrue(FacetBrowser.CASES.size() >= 22, "fixtures: " + FacetBrowser.CASES.size());
        assertTrue(measuredValues >= 22 * 20000, "values measured per rendering: " + measuredValues);
    }

    @Test
    void theLegacyOracleShowsTheStatesTheComparisonClaimsToCover() {
        Map<String, String> l = LEGACY_LIGHT;
        String hover = id("hover on an option");
        String resting = id("counts and a checked option");
        // The states fixture is Running(42, checked), Ready(18), Failed(3), Maintenance(0, disabled): labels #0..#3.
        assertEquals("rgba(0, 0, 0, 0)", nth(l, resting, "label", 1, "background-color"), "a row has no resting fill");
        assertNotEquals(nth(l, resting, "label", 1, "background-color"), nth(l, hover, "label", 1, "background-color"), "a hovered row is filled");
        assertEquals("rgba(0, 0, 0, 0)", nth(l, id("hover on a disabled option"), "label", 3, "background-color"), "a disabled row has no hover fill");
        assertNotEquals(nth(l, hover, "label", 1, "background-color"), nth(LEGACY_DARK, hover, "label", 1, "background-color"), "the hover fill is different in dark");
        assertEquals("rgba(0, 0, 0, 0)", nth(l, id("hover on the legend"), "legend", 0, "background-color"), "the legend has no hover rule");
        assertEquals("pointer", nth(l, resting, "label", 1, "cursor"));
        assertEquals("not-allowed", nth(l, resting, "label", 3, "cursor"));
        assertEquals("0.6", nth(l, resting, "label", 3, "opacity"), "a disabled row is dimmed as a whole");
        assertEquals("1", nth(l, resting, "label", 1, "opacity"));
        assertEquals("pointer", nth(l, resting, "input", 1, "cursor"));
        assertEquals("not-allowed", nth(l, resting, "input", 3, "cursor"));
        assertEquals("1", nth(l, resting, "input", 3, "opacity"), "the disabled checkbox is not dimmed twice: only the row is");
        assertEquals("16px", nth(l, resting, "input", 0, "width"));
        assertEquals("16px", nth(l, resting, "input", 0, "height"));
        assertEquals("0px", nth(l, resting, "input", 0, "margin-top"));
        assertEquals("auto", nth(l, resting, "input", 0, "appearance"), "the checkbox is native");
        assertTrue(nth(l, id("focus-visible on an option"), "input", 1, "box-shadow").contains("0px 0px 0px 4px"), "the focus ring is drawn: " + nth(l, id("focus-visible on an option"), "input", 1, "box-shadow"));
        assertEquals("none", nth(l, id("focus only"), "input", 1, "box-shadow"), "a plain focus draws no ring");
        assertEquals("none", nth(l, id("focus only"), "input", 1, "outline-style"));
        assertEquals("none", nth(l, id("focus-visible on an option"), "input", 1, "outline-style"));
        assertTrue(TextEntryBrowser.visibleShadow(nth(LEGACY_DARK, id("focus-visible on an option"), "input", 1, "box-shadow")).startsWith("rgb(255, 255, 255) 0px 0px 0px 2px, "),
                "the ring offset is white in dark mode too");
        assertEquals(nth(l, hover, "label", 1, "background-color"), nth(l, id("active on an option"), "label", 1, "background-color"));
        assertNotEquals(nth(l, resting, "input", 1, "background-color"), nth(LEGACY_DARK, resting, "input", 1, "background-color"), "the checkbox background follows the theme");
        assertEquals("14px", nth(l, resting, "label", 0, "font-size"));
        assertEquals("20px", nth(l, resting, "label", 0, "line-height"));
        assertEquals("10px", nth(l, resting, "label", 0, "column-gap"));
        assertEquals("6px", nth(l, resting, "label", 0, "padding-top"));
        assertEquals("8px", nth(l, resting, "label", 0, "padding-left"));
        assertEquals("12px", nth(l, resting, "span", 1, "font-size"), "the visible count is text-xs");
        assertEquals("tabular-nums", nth(l, resting, "span", 1, "font-variant-numeric"));
        assertEquals("600", nth(l, resting, "legend", 0, "font-weight"));
        assertEquals("6px", nth(l, resting, "legend", 0, "margin-bottom"));
        assertEquals("20px", nth(l, id("several facets"), "div", 1, "row-gap"), "the gap between facets");
        assertEquals("break-word", nth(l, id("long label without a break"), "span", 0, "overflow-wrap"));
        assertEquals("absolute", nth(l, resting, "span", 2, "position"), "the announced count is visually hidden");
    }

    @Test
    void onlyInvisibleShadowLayersWereNormalisedAndOnlyOnTheFocusRing() {
        assertEquals(List.of("invisible-shadow-layers"), List.copyOf(APPLIED.keySet()), "no other normalisation exists: " + APPLIED);
        assertTrue(APPLIED.get("invisible-shadow-layers") > 20, "Tailwind's unused transparent ring layers are what the migrated rule leaves out: " + APPLIED);
    }
}
