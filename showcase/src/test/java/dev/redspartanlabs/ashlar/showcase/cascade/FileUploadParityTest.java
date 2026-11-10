package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Visual parity of File Upload with Tailwind PRESENT, in all 22 configurations (two themes, no skin and the ten skins): the legacy rendering (the migrated markup rewritten to the Tailwind
 * classes the template used to write, with the cumulative Tailwind oracle) against the migrated rendering (the real application stylesheet, Tailwind and Ashlar together), both carrying the
 * component's own script. Every element of every upload (the Icon's svg included) is measured for {@link FileUploadBrowser#PROPERTIES}, with its box, in every state an upload has: idle,
 * multiple, required, disabled, invalid (with and without a message), help and error text, a long label, the keyboard focus ring (alone, invalid and disabled), plain focus, the card hovered
 * (idle, disabled, invalid, disabled and invalid, with a file selected), active, one, three and a very long filename selected (the status text and its truncation), and the card in narrow
 * columns of 240px and 120px.
 *
 * <p>Focus and hover are forced on the real element with the browser's own pseudo-state API (:focus together with :focus-visible, as a keyboard focus has both), on a browser that is
 * stated to be able to hover; files are selected through the browser's own DevTools file-input call, which fires the same input and change events a chooser does. The comparison is EXACT
 * except two documented, narrow normalisations: Text Entry's (the computed {@code box-shadow} with its fully transparent zero-size layers removed, {@link TextEntryBrowser#visibleShadow},
 * because Tailwind composes a ring from unused layers and the migrated rule writes only what it draws; every visible layer, the white offset included, is compared exactly) and the hidden
 * input's {@code clip} ({@link ToggleBrowser#setAsideSrOnlyClip}: {@code auto} against {@code rect(0px, 0px, 0px, 0px)}, nothing else). The shared parity page switches transitions off so a
 * state is measured at rest; the transitions themselves are measured by {@link FileUploadBehaviourTest}.
 *
 * <p>The :user-invalid state is not a pseudo-class the browser lets a test force; it is measured, with a real blocked submit, by {@link FileUploadBehaviourTest}.
 */
class FileUploadParityTest {

    private static final List<String> FINDINGS = new ArrayList<>();
    private static final Map<String, Integer> APPLIED = new LinkedHashMap<>();
    private static final Map<String, String> LEGACY_LIGHT = new LinkedHashMap<>();
    private static final Map<String, String> LEGACY_DARK = new LinkedHashMap<>();
    private static int configurations;
    private static int measuredFields;
    private static int measuredValues;

    record Field(String id, FileUploadBrowser.Case spec, String markup) {
    }

    static List<Field> fields() {
        List<Field> fields = new ArrayList<>();
        int i = 0;
        for (FileUploadBrowser.Case c : FileUploadBrowser.CASES) {
            String id = "fu" + (++i);
            fields.add(new Field(id, c, FileUploadBrowser.build(c, id)));
        }
        return fields;
    }

    static String measure(DevToolsSession cdp, String url, List<Field> fields) throws Exception {
        cdp.navigate(url);
        String sheets = cdp.evaluate("(function(){var ok=[].every.call(document.styleSheets,function(s){try{return s.cssRules.length>0}catch(e){return false}});return document.styleSheets.length+':'+ok})()");
        assertTrue(sheets.endsWith(":true") && !sheets.startsWith("0:"), "every stylesheet must have loaded - infrastructure failure, not a parity result: " + sheets);
        for (Field f : fields) {
            if (!f.spec().files().isEmpty()) {
                FileUploadBrowser.select(cdp, "#" + f.id(), f.spec().files());
            }
        }
        StringBuilder all = new StringBuilder();
        for (Field f : fields) {
            List<String> pseudo = f.spec().pseudo();
            List<Integer> nodes = pseudo.isEmpty() ? List.of() : cdp.nodes(FileUploadBrowser.target(f.spec(), f.id()));
            if (!pseudo.isEmpty()) {
                cdp.force(nodes, pseudo);
            }
            all.append(cdp.evaluate(FileUploadBrowser.measureScript(f.id()))).append('\n');
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

    /**
     * The migrated markup of every field, marked, and the same markup rewritten to the legacy classes: this family's first (the root pairs ash-field with ash-file-upload, and the Choose File
     * span has one class more than a Button), then Text Entry's (the field label, marker, help and error), then Button's and Icon's.
     */
    static String[] bodies(List<Field> fields) throws Exception {
        StringBuilder migrated = new StringBuilder();
        fields.forEach(f -> migrated.append(FileUploadBrowser.marked(f.markup())));
        String legacy = new IconLegacyMarkup().rewrite(new ButtonLegacyMarkup().rewrite(new TextEntryLegacyMarkup().rewrite(new FileUploadLegacyMarkup().rewrite(migrated.toString()))));
        return new String[] {migrated.toString(), legacy};
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("file-upload-parity");
        List<Field> fields = fields();
        String[] bodies = bodies(fields);
        String scripts = FileUploadBrowser.script();
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            for (ParityConfigs.Config config : ParityConfigs.all()) {
                String legacyUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("file-upload-oracle.css")), config, "", bodies[1], scripts));
                String migratedUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.APP), config, "", bodies[0], scripts));
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
                legacy = FileUploadBrowser.setAsideSrOnlyClip(legacy, false, APPLIED);
                migrated = FileUploadBrowser.setAsideSrOnlyClip(migrated, true, APPLIED);
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
        for (int i = 0; i < FileUploadBrowser.CASES.size(); i++) {
            if (FileUploadBrowser.CASES.get(i).key().equals(key)) {
                return "fu" + (i + 1);
            }
        }
        throw new AssertionError("no case " + key);
    }

    private static Map<String, Integer> summary() {
        Map<String, Integer> byProperty = new java.util.TreeMap<>();
        for (String finding : FINDINGS) {
            String[] p = finding.split(" \\| ");
            byProperty.merge(p.length > 2 ? p[1].replaceAll("^\\S+:\\d+:", "") + "." + p[2] : p[1], 1, Integer::sum);
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
    void theMigratedUploadsAreIdenticalToTheLegacyOnesInAll22Configurations() {
        assertEquals(22, configurations);
        assertEquals(List.of(), FINDINGS.stream().limit(60).toList(), FINDINGS.size() + " differences, by property: " + summary() + "; first of each: " + firstOfEach());
    }

    @Test
    void theMeasurementCoversEveryStateAndIsLargeEnoughToMeanSomething() {
        assertTrue(measuredFields >= 22 * 27, "fields measured: " + measuredFields);
        assertTrue(measuredValues >= 22 * 27 * 400, "values measured per rendering: " + measuredValues);
    }

    @Test
    void theLegacyOracleShowsTheStatesTheComparisonClaimsToCover() {
        Map<String, String> l = LEGACY_LIGHT;
        // elements of an upload, by position: 0 root, 1 field label, (2 required marker when required), card, input, icon wrapper, svg, status, Choose File span
        String idle = id("idle");
        assertEquals("384px", legacy(l, idle, "label", 1, "width"), "the card is 384px (24rem) wide");
        assertEquals("2px", legacy(l, idle, "label", 1, "border-top-width"));
        assertEquals("dashed", legacy(l, idle, "label", 1, "border-top-style"));
        assertEquals("8px", legacy(l, idle, "label", 1, "border-top-left-radius"));
        assertEquals("32px", legacy(l, idle, "label", 1, "padding-top"));
        assertEquals("24px", legacy(l, idle, "label", 1, "padding-left"));
        assertEquals("12px", legacy(l, idle, "label", 1, "row-gap"));
        assertEquals("24px", legacy(l, idle, "div", 0, "margin-bottom"), "the root's 1.5rem");
        assertEquals("24px", legacy(l, idle, "svg", 0, "width"), "the icon is 24px (h-6 w-6)");
        assertEquals("none", legacy(l, idle, "span", 1, "pointer-events"), "the Choose File span takes no pointer events");
        assertEquals("ellipsis", legacy(l, id("selected long name"), "p", 0, "text-overflow"));
        assertTrue(Double.parseDouble(legacy(l, id("selected long name"), "p", 0, "@scrollWidth")) > Double.parseDouble(legacy(l, id("selected long name"), "p", 0, "@w")), "a long name overflows the status line and is truncated");
        assertEquals("report.pdf", legacy(l, id("selected one"), "p", 0, "@text"));
        assertEquals("3 files selected", legacy(l, id("selected three"), "p", 0, "@text"));
        assertNotEquals(legacy(l, idle, "label", 1, "border-top-color"), legacy(l, id("hover zone"), "label", 1, "border-top-color"), "hover changes the border");
        assertEquals(legacy(l, idle, "label", 1, "border-top-color"), legacy(l, id("hover zone disabled"), "label", 1, "border-top-color"), "disabled hover keeps the resting border");
        assertEquals(legacy(l, idle, "label", 1, "border-top-color"), legacy(l, id("hover zone disabled invalid"), "label", 1, "border-top-color"), "the legacy quirk: disabled, invalid and hovered is the resting border, not red");
        assertEquals(legacy(l, id("invalid"), "label", 1, "border-top-color"), legacy(l, id("hover zone invalid"), "label", 1, "border-top-color"), "invalid keeps its red border under hover");
        assertNotEquals(legacy(l, id("invalid"), "label", 1, "background-color"), legacy(l, id("hover zone invalid"), "label", 1, "background-color"), "but the hover background still applies");
        assertEquals("0.6", legacy(l, id("disabled"), "label", 1, "opacity"));
        assertEquals("not-allowed", legacy(l, id("disabled"), "label", 1, "cursor"));
        assertTrue(TextEntryBrowser.visibleShadow(legacy(l, id("focus-visible"), "label", 1, "box-shadow")).startsWith("rgb(255, 255, 255) 0px 0px 0px 2px, "), "the white 2px offset then the ring");
        assertTrue(TextEntryBrowser.visibleShadow(legacy(LEGACY_DARK, id("focus-visible"), "label", 1, "box-shadow")).startsWith("rgb(255, 255, 255) 0px 0px 0px 2px, "), "white in dark mode too");
        assertEquals(TextEntryBrowser.visibleShadow(legacy(l, id("focus-visible"), "label", 1, "box-shadow")), TextEntryBrowser.visibleShadow(legacy(l, id("invalid focus-visible"), "label", 1, "box-shadow")),
                "an invalid card keeps the blue ring");
        assertEquals("none", TextEntryBrowser.visibleShadow(legacy(l, id("focus only"), "label", 1, "box-shadow")), "a plain focus draws no ring");
        assertEquals("4px", legacy(l, id("help"), "p", 1, "margin-top"), "D3: the legacy 4px help spacing");
    }

    @Test
    void onlyInvisibleShadowLayersAndTheHiddenInputsClipWereSetAside() {
        assertEquals(Set.of("invisible-shadow-layers", "sr-only-clip-legacy", "sr-only-clip-migrated"), APPLIED.keySet(), "no other normalisation exists: " + APPLIED);
        assertTrue(APPLIED.get("invisible-shadow-layers") > 100, "Tailwind's unused transparent ring layers are what the migrated rule leaves out: " + APPLIED);
        assertEquals(22 * FileUploadBrowser.CASES.size(), APPLIED.get("sr-only-clip-legacy"), "exactly one clip per upload, with the documented legacy value");
        assertEquals(22 * FileUploadBrowser.CASES.size(), APPLIED.get("sr-only-clip-migrated"), "and with the documented migrated value");
    }
}
