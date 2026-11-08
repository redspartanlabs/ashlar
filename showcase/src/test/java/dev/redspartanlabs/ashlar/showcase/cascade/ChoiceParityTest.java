package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Visual parity of Checkbox and Radio Group with Tailwind PRESENT, in all 22 configurations (two themes, no skin and the ten skins): the legacy rendering (the migrated markup rewritten
 * to the Tailwind classes the templates used to write, with the cumulative Tailwind oracle) against the migrated rendering (the real application stylesheet, Tailwind and Ashlar
 * together). Every element of every control is measured for {@link ChoiceBrowser#PROPERTIES}, with its box, in every state the two components have: unchecked, checked,
 * indeterminate, disabled, required, invalid (and the two together), the help and error text, the keyboard focus ring, plain focus, hover and active (which have no rule and so
 * must change nothing), a Radio Group selected, unselected, with descriptions, disabled as a whole and by one option, vertical and horizontal.
 *
 * <p>Focus is forced on the real control with the browser's own pseudo-state API (:focus together with :focus-visible, as a keyboard focus has both), on a browser that is stated to
 * be able to hover. The comparison is EXACT except one documented normalisation, the same as Text Entry's: the computed {@code box-shadow} with its fully transparent zero-size layers
 * removed ({@link TextEntryBrowser#visibleShadow}), because Tailwind composes the focus ring from unused layers and the migrated rule writes only what it draws. Every visible
 * layer, the white offset included, is compared exactly. The native control's own painting is compared in pixels by {@link ChoicePixelTest}. Nothing is left out of the comparison: every property of every element is compared.
 */
class ChoiceParityTest {

    private static final List<String> FINDINGS = new ArrayList<>();
    private static final Map<String, Integer> APPLIED = new LinkedHashMap<>();
    private static final Map<String, String> LEGACY_LIGHT = new LinkedHashMap<>();
    private static final Map<String, String> LEGACY_DARK = new LinkedHashMap<>();
    private static int configurations;
    private static int measuredFields;
    private static int measuredValues;

    record Field(String id, ChoiceBrowser.Case spec, String markup) {
    }

    static List<Field> fields() {
        List<Field> fields = new ArrayList<>();
        int i = 0;
        for (ChoiceBrowser.Case c : ChoiceBrowser.CASES) {
            String id = (c.kind().equals("checkbox") ? "cb" : "rg") + (++i);
            fields.add(new Field(id, c, ChoiceBrowser.build(c, id)));
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
            List<Integer> nodes = pseudo.isEmpty() ? List.of() : cdp.nodes(ChoiceBrowser.target(f.spec(), f.id()));
            if (!pseudo.isEmpty()) {
                cdp.force(nodes, pseudo);
            }
            all.append(cdp.evaluate(ChoiceBrowser.measureScript(f.id()))).append('\n');
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

    /** The migrated markup of every field, marked, and the same markup rewritten to the legacy classes (this family's and Text Entry's required marker). */
    static String[] bodies(List<Field> fields) throws Exception {
        StringBuilder migrated = new StringBuilder();
        fields.forEach(f -> migrated.append(ChoiceBrowser.marked(f.markup())));
        String legacy = new TextEntryLegacyMarkup().rewrite(new ChoiceLegacyMarkup().rewrite(migrated.toString()));
        return new String[] {migrated.toString(), legacy};
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("choice-parity");
        List<Field> fields = fields();
        String[] bodies = bodies(fields);
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            for (ParityConfigs.Config config : ParityConfigs.all()) {
                String legacyUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("choice-oracle.css")), config, "", bodies[1], ChoiceBrowser.INDETERMINATE));
                String migratedUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.APP), config, "", bodies[0], ChoiceBrowser.INDETERMINATE));
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
                    FINDINGS.addAll(TextEntryBrowser.differences(config.id() + " " + f.id() + " (" + f.spec().kind() + " " + f.spec().key() + ")", slice(legacy, f.id() + ":"), slice(migrated, f.id() + ":"), APPLIED));
                }
            }
        }
    }

    private static String legacy(Map<String, String> map, String field, String tag, String property) {
        return map.entrySet().stream().filter(e -> e.getKey().startsWith(field + ":") && e.getKey().contains(":" + tag + "|") && e.getKey().endsWith("|" + property))
                .map(Map.Entry::getValue).findFirst().orElseThrow(() -> new AssertionError("no legacy " + property + " of " + tag + " in " + field));
    }

    private static String id(String kind, String key) {
        for (int i = 0; i < ChoiceBrowser.CASES.size(); i++) {
            ChoiceBrowser.Case c = ChoiceBrowser.CASES.get(i);
            if (c.kind().equals(kind) && c.key().equals(key)) {
                return (kind.equals("checkbox") ? "cb" : "rg") + (i + 1);
            }
        }
        throw new AssertionError("no case " + kind + " " + key);
    }

    /** Which properties differ, and how many times, so a failure says what moved before it lists instances. */
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
    void theMigratedControlsAreIdenticalToTheLegacyOnesInAll22Configurations() {
        assertEquals(22, configurations);
        assertEquals(List.of(), FINDINGS.stream().limit(60).toList(), FINDINGS.size() + " differences, by property: " + summary() + "; first of each: " + firstOfEach());
    }

    @Test
    void theMeasurementCoversEveryStateAndIsLargeEnoughToMeanSomething() {
        assertTrue(measuredFields >= 22 * 30, "fields measured: " + measuredFields);
        assertTrue(measuredValues >= 22 * 4000, "values measured per rendering: " + measuredValues);
    }

    @Test
    void theLegacyOracleShowsTheStatesTheComparisonClaimsToCover() {
        Map<String, String> l = LEGACY_LIGHT;
        String rest = legacy(l, id("checkbox", "unchecked"), "input", "accent-color");
        assertNotEquals(rest, legacy(l, id("checkbox", "invalid"), "input", "accent-color"), "an invalid control is tinted red");
        assertEquals("0.6", legacy(l, id("checkbox", "disabled"), "input", "opacity"));
        assertEquals("not-allowed", legacy(l, id("checkbox", "disabled"), "input", "cursor"));
        assertEquals("pointer", legacy(l, id("checkbox", "disabled"), "label", "cursor"), "a disabled checkbox's label keeps its pointer cursor");
        assertEquals(legacy(l, id("checkbox", "unchecked"), "label", "color"), legacy(l, id("checkbox", "disabled"), "label", "color"), "and its colour");
        assertTrue(legacy(l, id("checkbox", "focus-visible"), "input", "box-shadow").contains("0px 0px 0px 4px"), "the focus ring is drawn: " + legacy(l, id("checkbox", "focus-visible"), "input", "box-shadow"));
        assertEquals("none", legacy(l, id("checkbox", "focus only"), "input", "box-shadow"), "a plain focus draws no ring");
        assertEquals(legacy(l, id("checkbox", "unchecked"), "input", "box-shadow"), legacy(l, id("checkbox", "hover"), "input", "box-shadow"), "hover has no rule");
        assertEquals("16px", legacy(l, id("checkbox", "unchecked"), "input", "width"));
        assertEquals("2px", legacy(l, id("checkbox", "unchecked"), "input", "margin-top"));
        assertEquals("default", legacy(l, id("radio", "selected"), "input", "cursor"), "a radio has no pointer cursor of its own");
        assertNotEquals(legacy(l, id("radio", "selected"), "label", "color"), legacy(l, id("radio", "whole group disabled"), "label", "color"), "a disabled group dims its labels");
        assertEquals("column", legacy(l, id("radio", "selected"), "div", "flex-direction"));
        assertTrue(TextEntryBrowser.visibleShadow(legacy(LEGACY_DARK, id("checkbox", "focus-visible"), "input", "box-shadow")).startsWith("rgb(255, 255, 255) 0px 0px 0px 2px, "), "the ring offset is white in dark mode too");
    }

    @Test
    void onlyInvisibleShadowLayersWereNormalisedAndOnlyOnTheFocusRing() {
        assertEquals(List.of("invisible-shadow-layers"), List.copyOf(APPLIED.keySet()), "no other normalisation exists: " + APPLIED);
        assertTrue(APPLIED.get("invisible-shadow-layers") > 100, "Tailwind's unused transparent ring layers are what the migrated rule leaves out: " + APPLIED);
        assertFalse(APPLIED.isEmpty());
    }
}
