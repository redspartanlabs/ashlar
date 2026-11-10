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
 * File Upload in a page that has Ashlar's stylesheet and NO Tailwind, and so no Tailwind Preflight. The parity fixtures all load Tailwind utilities (a host app has them), so on their own
 * they cannot see an upload that quietly depends on a host utility or a Preflight reset: a paragraph's 16px margins, a label's box, the hidden input's browser margin and border, an svg's
 * default size (an Icon that was given h-6 w-6 is not 24px without Tailwind). This page loads only {@code ashlar.css}.
 *
 * <p>Three kinds of evidence. (1) Absolute, from the migrated rendering alone: a plain {@code h-4 w-4} box is not 16px (Tailwind is absent); the card is 384px wide with its dashed 2px
 * border, 8px radius, 32px by 24px padding and 12px gap, the icon is 24px (the Icon's own large size, not a Tailwind utility), the status is one 14px/20px line that is cut off with an
 * ellipsis, the Choose File span takes no pointer events, help and error sit 4px below the card, the hidden input is a 1px clipped box, and the colours and the focus ring (white offset in
 * dark too) are the palette's. (2) The scoped legacy rendering (only the utilities and the few preflight rules the upload used, see {@code file-upload-legacy-scoped-entry.css}) must equal
 * the FULL legacy rendering, so it is a faithful stand-in for "the legacy upload without Tailwind's page-wide reset". (3) The migrated rendering with Ashlar alone must equal that scoped
 * legacy rendering, element by element, in six configurations covering both themes and skins. Both sides carry the showcase's own page environment and the component's own script.
 *
 * <p>What is not compared, and why: {@code font-family} is not measured at all (both pages pin the same font on a wrapper); the style and the colour of a zero-width border are not
 * compared, only the width, which is asserted to be 0 on every element of the migrated rendering except the card (Preflight's blanket {@code border: 0 solid currentcolor} gives every
 * element an invisible {@code solid} style and a text-coloured border the bare page lacks); and on the hidden INPUT, the properties Preflight's form-control reset sets that nothing can
 * show of an input that is clipped away: {@code font-size}, {@code line-height}, {@code letter-spacing}, {@code text-transform}, {@code color}, {@code background-color},
 * {@code border-radius} and the outline colour (the pixel suite compares the pictures). Fully transparent zero-size shadow layers and the hidden input's {@code clip} are set aside as in
 * {@link FileUploadParityTest}. Nothing else is excluded.
 *
 * <p>Firefox and Safari are not covered: only Chrome is driven.
 */
class FileUploadTailwindFreeTest {

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
        ParitySession session = ParitySession.open("file-upload-tailwind-free");
        List<FileUploadParityTest.Field> fields = FileUploadParityTest.fields();
        String[] bodies = FileUploadParityTest.bodies(fields);
        String probe = "<div id=\"probe\" class=\"h-4 w-4\"></div>";
        String environment = ParityFrames.environment(session.generatedDir());
        String scripts = FileUploadBrowser.script();
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            for (String id : CONFIGS) {
                ParityConfigs.Config config = ParityConfigs.get(id);
                String full = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("file-upload-oracle.css")), config, "", bodies[1] + probe, scripts));
                String scoped = server.registerRaw(insertEnvironment(TextEntryBrowser.page(List.of(ParityFrames.generated("file-upload-legacy-scoped.css")), config, "", bodies[1] + probe, scripts), environment));
                String free = server.registerRaw(insertEnvironment(TextEntryBrowser.page(List.of(ParityFrames.ASHLAR), config, "", bodies[0] + probe, scripts), environment));
                String fullM;
                String scopedM;
                String freeM;
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    fullM = FileUploadParityTest.measure(cdp, full, fields);
                    scopedM = FileUploadParityTest.measure(cdp, scoped, fields);
                    freeM = FileUploadParityTest.measure(cdp, free, fields);
                    if (id.equals("light/none")) {
                        controlProbe = cdp.evaluate("(function(){var p=document.getElementById('probe').getBoundingClientRect();return p.width+'x'+p.height})()");
                    }
                }
                configurations++;
                Map<String, Integer> ignored = new LinkedHashMap<>();
                String scopedLegacy = FileUploadBrowser.setAsideSrOnlyClip(scopedM, false, ignored);
                String freeMigrated = FileUploadBrowser.setAsideSrOnlyClip(freeM, true, APPLIED);
                String fullLegacy = FileUploadBrowser.setAsideSrOnlyClip(fullM, false, ignored);
                for (FileUploadParityTest.Field f : fields) {
                    String prefix = f.id() + ":";
                    String where = id + " " + f.id() + " (" + f.spec().key() + ")";
                    FINDINGS_SCOPED_VS_FULL.addAll(TextEntryBrowser.differences(where + " scoped-vs-full", comparable(FileUploadParityTest.slice(fullLegacy, prefix)), comparable(FileUploadParityTest.slice(scopedLegacy, prefix)), ignored));
                    FINDINGS_FREE_VS_SCOPED.addAll(TextEntryBrowser.differences(where + " free-vs-scoped", comparable(FileUploadParityTest.slice(scopedLegacy, prefix)), comparable(FileUploadParityTest.slice(freeMigrated, prefix)), APPLIED));
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
        for (int i = 0; i < FileUploadBrowser.CASES.size(); i++) {
            if (FileUploadBrowser.CASES.get(i).key().equals(key)) {
                return "fu" + (i + 1);
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
    void theMigratedUploadsStandOnTheirOwnWithoutTailwind() {
        Map<String, String> l = FREE_LIGHT;
        String t = id("idle");
        // elements of an idle upload, by tag: div 0 the root; label 0 the field label, label 1 the card; the input; span 0 the icon wrapper, span 1 Choose File; the svg; p 0 the status
        assertEquals("24px", free(l, t, "div", 0, "margin-bottom"), "the root's 1.5rem");
        assertEquals("8px", free(l, t, "label", 0, "margin-bottom"));
        assertEquals("14px", free(l, t, "label", 0, "font-size"));
        assertEquals("20px", free(l, t, "label", 0, "line-height"));
        assertEquals("500", free(l, t, "label", 0, "font-weight"));
        assertEquals("flex", free(l, t, "label", 1, "display"));
        assertEquals("column", free(l, t, "label", 1, "flex-direction"));
        assertEquals("384px", free(l, t, "label", 1, "width"), "100% of the column, at most 24rem");
        assertEquals("384px", free(l, t, "label", 1, "max-width"));
        assertEquals("2px", free(l, t, "label", 1, "border-top-width"));
        assertEquals("dashed", free(l, t, "label", 1, "border-top-style"));
        assertEquals("8px", free(l, t, "label", 1, "border-top-left-radius"), "var(--radius-lg, 0.5rem) with no theme");
        assertEquals("32px", free(l, t, "label", 1, "padding-top"));
        assertEquals("24px", free(l, t, "label", 1, "padding-left"));
        assertEquals("12px", free(l, t, "label", 1, "row-gap"));
        assertEquals("center", free(l, t, "label", 1, "align-items"));
        assertEquals("center", free(l, t, "label", 1, "text-align"));
        assertEquals("pointer", free(l, t, "label", 1, "cursor"));
        assertEquals("border-box", free(l, t, "label", 1, "box-sizing"));
        assertEquals("not-allowed", free(l, id("disabled"), "label", 1, "cursor"));
        assertEquals("0.6", free(l, id("disabled"), "label", 1, "opacity"));
        assertEquals("1px", free(l, t, "input", 0, "width"), "the hidden input is a 1px box");
        assertEquals("1px", free(l, t, "input", 0, "height"));
        assertEquals("absolute", free(l, t, "input", 0, "position"));
        assertEquals("inset(50%)", free(l, t, "input", 0, "clip-path"));
        assertEquals("0px", free(l, t, "input", 0, "border-top-width"), "the browser's own file input border is gone");
        assertEquals("24px", free(l, t, "svg", 0, "width"), "the Icon is 24px by its own large size, not by a Tailwind utility");
        assertEquals("24px", free(l, t, "svg", 0, "height"));
        assertEquals("14px", free(l, t, "p", 0, "font-size"));
        assertEquals("20px", free(l, t, "p", 0, "line-height"));
        assertEquals("500", free(l, t, "p", 0, "font-weight"));
        assertEquals("0px", free(l, t, "p", 0, "margin-top"), "a paragraph's browser margin is gone");
        assertEquals("0px", free(l, t, "p", 0, "margin-bottom"));
        assertEquals("nowrap", free(l, t, "p", 0, "white-space"));
        assertEquals("ellipsis", free(l, t, "p", 0, "text-overflow"));
        assertEquals("hidden", free(l, t, "p", 0, "overflow-x"));
        assertEquals("none", free(l, t, "span", 1, "pointer-events"), "the Choose File span takes no pointer events, with no Tailwind utility");
        assertEquals("32px", free(l, t, "span", 1, "height"));
        String help = id("help and error");
        // the required marker is span 0 here, so the status is p 0, the help p 1 and the error p 2
        assertEquals("4px", free(l, help, "p", 1, "margin-top"), "D3: help sits 4px below the card");
        assertEquals("0px", free(l, help, "p", 1, "margin-bottom"));
        assertEquals("4px", free(l, help, "p", 2, "margin-top"), "and so does the error");
        assertEquals("oklch(0.869 0.022 252.894)", free(l, t, "label", 1, "border-top-color"));
        assertEquals("oklch(0.984 0.003 247.858)", free(l, t, "label", 1, "background-color"));
        assertEquals("oklch(0.446 0.043 257.281)", free(FREE_DARK, t, "label", 1, "border-top-color"), "dark mode follows data-theme");
        assertTrue(free(FREE_DARK, t, "label", 1, "background-color").startsWith("oklab(0.279 "), "the dark background is the palette's slate-800 at half opacity: " + free(FREE_DARK, t, "label", 1, "background-color"));
        assertEquals("oklch(0.704 0.04 256.788)", free(l, id("hover zone"), "label", 1, "border-top-color"), "hover: slate-400");
        assertEquals("oklch(0.704 0.191 22.216)", free(l, id("invalid"), "label", 1, "border-top-color"), "invalid: red-400");
        assertEquals("oklch(0.637 0.237 25.331)", free(FREE_DARK, id("invalid"), "label", 1, "border-top-color"), "and red-500 in dark");
        String ring = free(l, id("focus-visible"), "label", 1, "box-shadow");
        assertTrue(TextEntryBrowser.visibleShadow(ring).startsWith("rgb(255, 255, 255) 0px 0px 0px 2px, "), "the focus ring keeps its white offset: " + ring);
        assertTrue(TextEntryBrowser.visibleShadow(free(FREE_DARK, id("focus-visible"), "label", 1, "box-shadow")).startsWith("rgb(255, 255, 255) 0px 0px 0px 2px, "), "in dark mode too");
        assertEquals("none", TextEntryBrowser.visibleShadow(free(l, id("focus only"), "label", 1, "box-shadow")), "a plain focus draws no ring");
        assertEquals("188", free(l, id("narrow 240 long name"), "p", 0, "@w").replace(".0", ""), "a long name is cut off to the card's content width (240 - 2*2 - 2*24)");
    }

    @Test
    void everyElementOfEveryMigratedUploadHasNoBorderWidthExceptTheCardSoTheExcludedBorderStyleAndColourPaintNothing() {
        List<String> wide = FREE_LIGHT.entrySet().stream().filter(e -> e.getKey().matches(".*\\|border-(?:top|right|bottom|left)-width") && !e.getValue().equals("0px")
                && !e.getKey().matches("fu\\d+:\\d+:label\\|.*") && !"32px".equals(FREE_LIGHT.get(e.getKey().replaceAll("\\|border-.*", "|height")))).map(Map.Entry::getKey).toList();
        assertEquals(List.of(), wide, "only the card (a label) and the Choose File Button (a 32px span, whose 1px border is the Button's own) have a border; the field label is a label too, so it is checked below");
        assertTrue(FREE_LIGHT.keySet().stream().filter(k -> k.endsWith("|border-top-width")).count() > 200);
        assertEquals("0px", free(FREE_LIGHT, id("idle"), "label", 0, "border-top-width"), "the field label has no border");
        assertEquals("2px", free(FREE_LIGHT, id("idle"), "label", 1, "border-top-width"), "the card has its 2px dashed border");
    }

    @Test
    void theScopedLegacyRenderingEqualsTheFullLegacyRendering() {
        assertEquals(6, configurations);
        assertEquals(List.of(), FINDINGS_SCOPED_VS_FULL.stream().limit(40).toList(), FINDINGS_SCOPED_VS_FULL.size() + " differences, by property: " + byProperty(FINDINGS_SCOPED_VS_FULL));
    }

    @Test
    void theMigratedUploadsWithoutTailwindEqualTheLegacyUploadsWithoutPageWideTailwind() {
        assertEquals(List.of(), FINDINGS_FREE_VS_SCOPED.stream().limit(40).toList(), FINDINGS_FREE_VS_SCOPED.size() + " differences, by property: " + byProperty(FINDINGS_FREE_VS_SCOPED));
    }

    @Test
    void onlyInvisibleShadowLayersAndTheHiddenInputsClipWereSetAside() {
        assertEquals(Set.of("invisible-shadow-layers", "sr-only-clip-migrated"), APPLIED.keySet(), APPLIED.toString());
    }
}
