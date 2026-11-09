package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Toggle in a page that has Ashlar's stylesheet and NO Tailwind, and so no Tailwind Preflight. The parity fixtures all load Tailwind utilities (a host app has them), so on their own
 * they cannot see a toggle that quietly depends on a host utility or a Preflight reset: a paragraph's 16px margins, a label's and a span's box, the hidden input's browser margin and
 * border. This page loads only {@code ashlar.css}.
 *
 * <p>Three kinds of evidence. (1) Absolute, from the migrated rendering alone: a plain {@code h-4 w-4} box is not 16px (Tailwind is absent); the switch is 44 by 24, the thumb 20 by 20 and
 * 20px along when checked, both fully rounded; the hidden input is a 1px clipped box; the label text is 14px/20px at weight 500; help and error sit 4px below the row; the track has its
 * colours and the focus ring its white offset (in dark too). (2) The scoped legacy rendering (only the utilities and the few preflight rules the toggle used, see
 * {@code toggle-legacy-scoped-entry.css}) must equal the FULL legacy rendering, so it is a faithful stand-in for "the legacy toggle without Tailwind's page-wide reset". (3) The migrated
 * rendering with Ashlar alone must equal that scoped legacy rendering, element by element, in six configurations covering both themes and skins. Both sides carry the showcase's own page
 * environment.
 *
 * <p>What is not compared, and why: {@code font-family} is not measured at all (both pages pin the same font on a wrapper); the style and the colour of a zero-width border are not
 * compared, only the width, which is asserted to be 0 on every element of the migrated rendering (Preflight's blanket {@code border: 0 solid currentcolor} gives every element an invisible
 * {@code solid} style and a text-coloured border the bare page lacks); and on the hidden INPUT, the properties Preflight's form-control reset sets that nothing can show of an input that is
 * clipped away: {@code font-size}, {@code line-height}, {@code letter-spacing}, {@code text-transform}, {@code color}, {@code background-color}, {@code border-radius} and the outline colour
 * (the pixel suite compares the pictures). Fully transparent zero-size shadow layers and the hidden input's {@code clip} are set aside as in {@link ToggleParityTest}. Nothing else is
 * excluded.
 *
 * <p>Firefox and Safari are not covered: only Chrome is driven.
 */
class ToggleTailwindFreeTest {

    private static final List<String> CONFIGS = List.of("light/none", "dark/none", "light/ledger", "light/foundry", "dark/axiom", "dark/harbor");
    private static final Set<String> INPUT_INERT_PROPERTIES = Set.of("font-size", "line-height", "letter-spacing", "text-transform", "color", "background-color", "outline-color",
            "border-top-left-radius", "border-top-right-radius", "border-bottom-right-radius", "border-bottom-left-radius");

    private static final List<String> FINDINGS_SCOPED_VS_FULL = new ArrayList<>();
    private static final List<String> FINDINGS_FREE_VS_SCOPED = new ArrayList<>();
    private static final Map<String, Integer> APPLIED = new LinkedHashMap<>();
    private static final Map<String, String> FREE_LIGHT = new LinkedHashMap<>();
    private static final Map<String, String> FREE_DARK = new LinkedHashMap<>();
    private static String controlProbe;
    private static int configurations;

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
            if (p[1].equals("border-top-style") || p[1].matches("border-(?:top|right|bottom|left)-color")) {
                return false;
            }
            return !(p[0].endsWith(":input") && INPUT_INERT_PROPERTIES.contains(p[1]));
        }).collect(Collectors.joining("\n"));
    }

    @BeforeAll
    static void measure() throws Exception {
        ParitySession session = ParitySession.open("toggle-tailwind-free");
        List<ToggleParityTest.Field> fields = ToggleParityTest.fields();
        String[] bodies = ToggleParityTest.bodies(fields);
        String probe = "<div id=\"probe\" class=\"h-4 w-4\"></div>";
        String environment = ParityFrames.environment(session.generatedDir());
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            for (String id : CONFIGS) {
                ParityConfigs.Config config = ParityConfigs.get(id);
                String full = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("toggle-oracle.css")), config, "", bodies[1] + probe, ""));
                String scoped = server.registerRaw(insertEnvironment(TextEntryBrowser.page(List.of(ParityFrames.generated("toggle-legacy-scoped.css")), config, "", bodies[1] + probe, ""), environment));
                String free = server.registerRaw(insertEnvironment(TextEntryBrowser.page(List.of(ParityFrames.ASHLAR), config, "", bodies[0] + probe, ""), environment));
                String fullM;
                String scopedM;
                String freeM;
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    fullM = ToggleParityTest.measure(cdp, full, fields);
                    scopedM = ToggleParityTest.measure(cdp, scoped, fields);
                    freeM = ToggleParityTest.measure(cdp, free, fields);
                    if (id.equals("light/none")) {
                        controlProbe = cdp.evaluate("(function(){var p=document.getElementById('probe').getBoundingClientRect();return p.width+'x'+p.height})()");
                    }
                }
                configurations++;
                Map<String, Integer> ignored = new LinkedHashMap<>();
                String scopedLegacy = ToggleBrowser.setAsideSrOnlyClip(scopedM, false, ignored);
                String freeMigrated = ToggleBrowser.setAsideSrOnlyClip(freeM, true, APPLIED);
                String fullLegacy = ToggleBrowser.setAsideSrOnlyClip(fullM, false, ignored);
                for (ToggleParityTest.Field f : fields) {
                    String prefix = f.id() + ":";
                    String where = id + " " + f.id() + " (" + f.spec().key() + ")";
                    FINDINGS_SCOPED_VS_FULL.addAll(TextEntryBrowser.differences(where + " scoped-vs-full", comparable(ToggleParityTest.slice(fullLegacy, prefix)), comparable(ToggleParityTest.slice(scopedLegacy, prefix)), ignored));
                    FINDINGS_FREE_VS_SCOPED.addAll(TextEntryBrowser.differences(where + " free-vs-scoped", comparable(ToggleParityTest.slice(scopedLegacy, prefix)), comparable(ToggleParityTest.slice(freeMigrated, prefix)), APPLIED));
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
        }
    }

    private static String free(Map<String, String> map, String field, String tag, int nth, String property) {
        int seen = 0;
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (e.getKey().startsWith(field + ":") && e.getKey().contains(":" + tag + "|") && e.getKey().endsWith("|" + property) && seen++ == nth) {
                return e.getValue();
            }
        }
        throw new AssertionError("no " + property + " of " + tag + "#" + nth + " in " + field);
    }

    private static String id(String key) {
        for (int i = 0; i < ToggleBrowser.CASES.size(); i++) {
            if (ToggleBrowser.CASES.get(i).key().equals(key)) {
                return "tg" + (i + 1);
            }
        }
        throw new AssertionError("no case " + key);
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
    void theMigratedTogglesStandOnTheirOwnWithoutTailwind() {
        Map<String, String> l = FREE_LIGHT;
        String t = id("unchecked");
        // spans by position: 0 switch, 1 track, 2 thumb, 3 text
        assertEquals("44px", free(l, t, "span", 0, "width"));
        assertEquals("24px", free(l, t, "span", 0, "height"));
        assertEquals("44px", free(l, t, "span", 1, "width"), "the track fills the switch");
        assertEquals("24px", free(l, t, "span", 1, "height"));
        assertEquals("20px", free(l, t, "span", 2, "width"));
        assertEquals("20px", free(l, t, "span", 2, "height"));
        assertEquals("2px", free(l, t, "span", 2, "left"));
        assertEquals("2px", free(l, t, "span", 2, "top"));
        assertEquals("3.35544e+07px", free(l, t, "span", 1, "border-top-left-radius"), "fully rounded, as Tailwind's rounded-full computes");
        assertEquals("3.35544e+07px", free(l, t, "span", 2, "border-top-left-radius"));
        assertEquals("none", free(l, t, "span", 2, "translate"));
        assertEquals("20px", free(l, id("checked"), "span", 2, "translate"));
        assertEquals("1px", free(l, t, "input", 0, "width"), "the hidden input is a 1px box");
        assertEquals("1px", free(l, t, "input", 0, "height"));
        assertEquals("absolute", free(l, t, "input", 0, "position"));
        assertEquals("inset(50%)", free(l, t, "input", 0, "clip-path"));
        assertEquals("0px", free(l, t, "input", 0, "border-top-width"), "the browser's own checkbox border is gone");
        assertEquals("inline-flex", free(l, t, "label", 0, "display"));
        assertEquals("12px", free(l, t, "label", 0, "column-gap"));
        assertEquals("pointer", free(l, t, "label", 0, "cursor"));
        assertEquals("not-allowed", free(l, id("disabled"), "label", 0, "cursor"));
        assertEquals("row-reverse", free(l, id("label start"), "label", 0, "flex-direction"));
        assertEquals("14px", free(l, t, "span", 3, "font-size"));
        assertEquals("20px", free(l, t, "span", 3, "line-height"));
        assertEquals("500", free(l, t, "span", 3, "font-weight"));
        assertEquals("16px", free(l, id("help only"), "div", 0, "margin-bottom"), "the wrapper's 1rem");
        String help = id("help and error");
        assertEquals("4px", free(l, help, "p", 0, "margin-top"), "T1: help sits 4px below the row");
        assertEquals("0px", free(l, help, "p", 0, "margin-bottom"), "a paragraph's browser margin is gone");
        assertEquals("4px", free(l, help, "p", 1, "margin-top"), "and so does the error");
        assertEquals("oklch(0.869 0.022 252.894)", free(l, t, "span", 1, "background-color"));
        assertEquals("oklch(0.546 0.245 262.881)", free(l, id("checked"), "span", 1, "background-color"));
        assertEquals("oklch(0.446 0.043 257.281)", free(FREE_DARK, t, "span", 1, "background-color"), "dark mode follows data-theme");
        assertEquals("0.6", free(l, id("disabled"), "span", 1, "opacity"));
        String ring = free(l, id("focus-visible"), "span", 1, "box-shadow");
        assertTrue(TextEntryBrowser.visibleShadow(ring).startsWith("rgb(255, 255, 255) 0px 0px 0px 2px, "), "the focus ring keeps its white offset: " + ring);
        assertTrue(TextEntryBrowser.visibleShadow(free(FREE_DARK, id("focus-visible"), "span", 1, "box-shadow")).startsWith("rgb(255, 255, 255) 0px 0px 0px 2px, "), "in dark mode too");
        assertTrue(free(l, t, "span", 2, "box-shadow").contains("0px 1px 3px 0px"), "the thumb has its shadow");
    }

    @Test
    void everyElementOfEveryMigratedToggleHasNoBorderWidthSoTheExcludedBorderStyleAndColourPaintNothing() {
        List<String> wide = FREE_LIGHT.entrySet().stream().filter(e -> e.getKey().matches(".*\\|border-(?:top|right|bottom|left)-width") && !e.getValue().equals("0px")).map(Map.Entry::getKey).toList();
        assertEquals(List.of(), wide);
        assertTrue(FREE_LIGHT.keySet().stream().filter(k -> k.endsWith("|border-top-width")).count() > 200);
    }

    @Test
    void theScopedLegacyRenderingEqualsTheFullLegacyRendering() {
        assertEquals(6, configurations);
        assertEquals(List.of(), FINDINGS_SCOPED_VS_FULL.stream().limit(40).toList(), FINDINGS_SCOPED_VS_FULL.size() + " differences, by property: " + byProperty(FINDINGS_SCOPED_VS_FULL));
    }

    @Test
    void theMigratedTogglesWithoutTailwindEqualTheLegacyTogglesWithoutPageWideTailwind() {
        assertEquals(List.of(), FINDINGS_FREE_VS_SCOPED.stream().limit(40).toList(), FINDINGS_FREE_VS_SCOPED.size() + " differences, by property: " + byProperty(FINDINGS_FREE_VS_SCOPED));
    }

    @Test
    void onlyInvisibleShadowLayersAndTheHiddenInputsClipWereSetAside() {
        assertEquals(Set.of("invisible-shadow-layers", "sr-only-clip-migrated"), APPLIED.keySet(), APPLIED.toString());
    }
}
