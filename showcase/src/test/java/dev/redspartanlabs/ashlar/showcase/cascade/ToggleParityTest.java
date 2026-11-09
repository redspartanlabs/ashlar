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
 * Visual parity of Toggle with Tailwind PRESENT, in all 22 configurations (two themes, no skin and the ten skins): the legacy rendering (the migrated markup rewritten to the Tailwind
 * classes the template used to write, with the cumulative Tailwind oracle) against the migrated rendering (the real application stylesheet, Tailwind and Ashlar together). Every element
 * of every toggle is measured for {@link ToggleBrowser#PROPERTIES}, with its box, in every state a Toggle has: unchecked, checked, disabled (both), required, invalid (both), the help and
 * error text, both label positions, a long label, the keyboard focus ring (with checked, invalid and disabled), plain focus, and the track hovered (unchecked, checked, disabled, invalid),
 * active, and the label text hovered.
 *
 * <p>Focus and hover are forced on the real element with the browser's own pseudo-state API (:focus together with :focus-visible, as a keyboard focus has both), on a browser that is
 * stated to be able to hover. The comparison is EXACT except two documented, narrow normalisations: Text Entry's (the computed {@code box-shadow} with its fully transparent zero-size
 * layers removed, {@link TextEntryBrowser#visibleShadow}, because Tailwind composes a ring from unused layers and the migrated rule writes only what it draws; every visible layer, the
 * white offset included, is compared exactly) and the hidden input's {@code clip} ({@link ToggleBrowser#setAsideSrOnlyClip}: {@code auto} against {@code rect(0px, 0px, 0px, 0px)},
 * nothing else). Everything else of every element is compared, including the thumb's {@code translate}. The shared parity page switches transitions off so a state is measured at rest; the transitions themselves are measured by {@link ToggleMotionTest}.
 */
class ToggleParityTest {

    private static final List<String> FINDINGS = new ArrayList<>();
    private static final Map<String, Integer> APPLIED = new LinkedHashMap<>();
    private static final Map<String, String> LEGACY_LIGHT = new LinkedHashMap<>();
    private static final Map<String, String> LEGACY_DARK = new LinkedHashMap<>();
    private static int configurations;
    private static int measuredFields;
    private static int measuredValues;

    record Field(String id, ToggleBrowser.Case spec, String markup) {
    }

    static List<Field> fields() {
        List<Field> fields = new ArrayList<>();
        int i = 0;
        for (ToggleBrowser.Case c : ToggleBrowser.CASES) {
            String id = "tg" + (++i);
            fields.add(new Field(id, c, ToggleBrowser.build(c, id)));
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
            List<Integer> nodes = pseudo.isEmpty() ? List.of() : cdp.nodes(ToggleBrowser.target(f.spec(), f.id()));
            if (!pseudo.isEmpty()) {
                cdp.force(nodes, pseudo);
            }
            all.append(cdp.evaluate(ToggleBrowser.measureScript(f.id()))).append('\n');
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

    /** The migrated markup of every field, marked, and the same markup rewritten to the legacy classes (this family's, Choice's and Text Entry's required marker). */
    static String[] bodies(List<Field> fields) throws Exception {
        StringBuilder migrated = new StringBuilder();
        fields.forEach(f -> migrated.append(ChoiceBrowser.marked(f.markup())));
        String legacy = new TextEntryLegacyMarkup().rewrite(new ChoiceLegacyMarkup().rewrite(new ToggleLegacyMarkup().rewrite(migrated.toString())));
        return new String[] {migrated.toString(), legacy};
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("toggle-parity");
        List<Field> fields = fields();
        String[] bodies = bodies(fields);
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            for (ParityConfigs.Config config : ParityConfigs.all()) {
                String legacyUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("toggle-oracle.css")), config, "", bodies[1], ""));
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
                legacy = ToggleBrowser.setAsideSrOnlyClip(legacy, false, APPLIED);
                migrated = ToggleBrowser.setAsideSrOnlyClip(migrated, true, APPLIED);
                for (Field f : fields) {
                    FINDINGS.addAll(TextEntryBrowser.differences(config.id() + " " + f.id() + " (" + f.spec().key() + ")", slice(legacy, f.id() + ":"), slice(migrated, f.id() + ":"), APPLIED));
                }
            }
        }
    }

    private static String legacy(Map<String, String> map, String field, String tag, int nth, String property) {
        int seen = 0;
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (e.getKey().startsWith(field + ":") && e.getKey().contains(":" + tag + "|") && e.getKey().endsWith("|" + property) && seen++ == nth) {
                return e.getValue();
            }
        }
        throw new AssertionError("no legacy " + property + " of " + tag + "#" + nth + " in " + field);
    }

    private static String id(String key) {
        for (int i = 0; i < ToggleBrowser.CASES.size(); i++) {
            if (ToggleBrowser.CASES.get(i).key().equals(key)) {
                return "tg" + (i + 1);
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
    void theMigratedTogglesAreIdenticalToTheLegacyOnesInAll22Configurations() {
        assertEquals(22, configurations);
        assertEquals(List.of(), FINDINGS.stream().limit(60).toList(), FINDINGS.size() + " differences, by property: " + summary() + "; first of each: " + firstOfEach());
    }

    @Test
    void theMeasurementCoversEveryStateAndIsLargeEnoughToMeanSomething() {
        assertTrue(measuredFields >= 22 * 28, "fields measured: " + measuredFields);
        assertTrue(measuredValues >= 22 * 28 * 100, "values measured per rendering: " + measuredValues);
    }

    @Test
    void theLegacyOracleShowsTheStatesTheComparisonClaimsToCover() {
        Map<String, String> l = LEGACY_LIGHT;
        // spans of a toggle, by position: 0 switch, 1 track, 2 thumb, 3 text (then the required marker)
        assertEquals("44px", legacy(l, id("unchecked"), "span", 0, "width"), "the switch is 44 wide");
        assertEquals("24px", legacy(l, id("unchecked"), "span", 0, "height"));
        assertEquals("20px", legacy(l, id("unchecked"), "span", 2, "width"), "the thumb is 20 wide");
        assertEquals("none", legacy(l, id("unchecked"), "span", 2, "translate"));
        assertEquals("20px", legacy(l, id("checked"), "span", 2, "translate"), "the thumb moves 20px when checked");
        assertNotEquals(legacy(l, id("unchecked"), "span", 1, "background-color"), legacy(l, id("checked"), "span", 1, "background-color"), "the track changes colour when checked");
        assertEquals("0.6", legacy(l, id("disabled"), "span", 1, "opacity"));
        assertEquals("not-allowed", legacy(l, id("disabled"), "label", 0, "cursor"));
        assertEquals(legacy(l, id("unchecked"), "span", 1, "background-color"), legacy(l, id("hover track disabled"), "span", 1, "background-color"), "disabled hover keeps the base colour");
        assertNotEquals(legacy(l, id("unchecked"), "span", 1, "background-color"), legacy(l, id("hover track"), "span", 1, "background-color"), "hover changes the colour");
        assertEquals(legacy(l, id("unchecked"), "span", 1, "background-color"), legacy(l, id("hover text"), "span", 1, "background-color"), "hovering the text does not hover the track");
        assertTrue(legacy(l, id("focus-visible"), "span", 1, "box-shadow").contains("0px 0px 0px 4px"), "the focus ring is drawn on the track: " + legacy(l, id("focus-visible"), "span", 1, "box-shadow"));
        assertTrue(TextEntryBrowser.visibleShadow(legacy(LEGACY_DARK, id("focus-visible"), "span", 1, "box-shadow")).startsWith("rgb(255, 255, 255) 0px 0px 0px 2px, "), "the ring offset is white in dark mode too");
        assertTrue(TextEntryBrowser.visibleShadow(legacy(l, id("invalid"), "span", 1, "box-shadow")).matches("oklch\\(0\\.637 0\\.237 25\\.331\\) 0px 0px 0px 2px"),
                "an invalid toggle draws a 2px red-500 ring and nothing else: " + legacy(l, id("invalid"), "span", 1, "box-shadow"));
        assertEquals("none", TextEntryBrowser.visibleShadow(legacy(l, id("focus only"), "span", 1, "box-shadow")), "a plain focus draws no ring");
        assertEquals("row-reverse", legacy(l, id("label start"), "label", 0, "flex-direction"));
        assertEquals("4px", legacy(l, id("help only"), "p", 0, "margin-top"), "T1: the legacy 4px help spacing");
    }

    @Test
    void onlyInvisibleShadowLayersAndTheHiddenInputsClipWereSetAside() {
        assertEquals(java.util.Set.of("invisible-shadow-layers", "sr-only-clip-legacy", "sr-only-clip-migrated"), APPLIED.keySet(), "no other normalisation exists: " + APPLIED);
        assertTrue(APPLIED.get("invisible-shadow-layers") > 100, "Tailwind's unused transparent ring layers are what the migrated rule leaves out: " + APPLIED);
        assertEquals(22 * ToggleBrowser.CASES.size(), APPLIED.get("sr-only-clip-legacy"), "exactly one clip per toggle, with the documented legacy value");
        assertEquals(22 * ToggleBrowser.CASES.size(), APPLIED.get("sr-only-clip-migrated"), "and with the documented migrated value");
    }
}
