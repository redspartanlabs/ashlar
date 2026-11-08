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
 * Checkbox and Radio Group in a page that has Ashlar's stylesheet and NO Tailwind, and so no Tailwind Preflight. The parity fixtures all load Tailwind utilities (a host app has them),
 * so on their own they cannot see a control that quietly depends on a host utility or a Preflight reset: the native checkbox's browser margin (Preflight zeroes it), a paragraph's
 * 16px margins, a fieldset's border, padding and minimum width, a legend's padding. This page loads only {@code ashlar.css}.
 *
 * <p>Three kinds of evidence. (1) Absolute, from the migrated rendering alone: a plain {@code h-4 w-4} box is not 16px (Tailwind is absent); a native control is 16 by 16px with a 2px
 * top margin and no other; a label is 14px/20px at weight 500; a fieldset has no border width, no padding and no minimum width; the vertical and horizontal option layouts have their
 * measured gaps; the help and error margins are 2px (Checkbox) and 8px (Radio Group); the focus ring has its white offset; a disabled option dims its label. (2) The scoped legacy
 * rendering (only the utilities and the few preflight rules the controls used, see {@code choice-legacy-scoped-entry.css}) must equal the FULL legacy rendering, so it is a faithful
 * stand-in for "the legacy controls without Tailwind's page-wide reset". (3) The migrated rendering with Ashlar alone must equal that scoped legacy rendering, control by control, in
 * six configurations covering both themes and skins that change a radius, a fill or the focus colour. BOTH sides carry the showcase's own dark {@code color-scheme}, which changes how
 * the browser draws a native control.
 *
 * <p>What is not compared, and why: {@code font-family} is not measured at all (both pages pin the same font on a wrapper); the style and the colour of a zero-width border on the
 * fieldset and the native input are not compared, only the width (Preflight's blanket {@code border: 0 solid currentcolor} gives every element an invisible {@code solid} style and a
 * text-coloured border the bare page lacks, and a fieldset's own groove style is invisible at width 0); the outline colour of the native input (its outline style is none); and on the native checkbox and radio INPUT, the text properties {@code font-size}, {@code line-height}, {@code letter-spacing}, {@code text-transform} and {@code color}, which
 * Preflight's form-control reset inherits and which the browser's own control has no text to apply them to. Fully transparent zero-size shadow layers are normalised as in
 * {@link ChoiceParityTest}. Nothing else is excluded.
 *
 * <p>Firefox and Safari are not covered: only Chrome is driven.
 */
class ChoiceTailwindFreeTest {

    private static final List<String> CONFIGS = List.of("light/none", "dark/none", "light/ledger", "light/foundry", "dark/axiom", "dark/harbor");
    private static final Set<String> INPUT_TEXT_PROPERTIES = Set.of("font-size", "line-height", "letter-spacing", "text-transform", "color");

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
            if (p[1].equals("border-top-style")) {
                return false;
            }
            // A zero-width border has a colour (currentcolor, which Preflight's reset and the browser's own default resolve differently) that is never drawn, and so has an outline whose style is none.
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
        ParitySession session = ParitySession.open("choice-tailwind-free");
        List<ChoiceParityTest.Field> fields = ChoiceParityTest.fields();
        String[] bodies = ChoiceParityTest.bodies(fields);
        String probe = "<div id=\"probe\" class=\"h-4 w-4\"></div>";
        String environment = ParityFrames.environment(session.generatedDir());
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            for (String id : CONFIGS) {
                ParityConfigs.Config config = ParityConfigs.get(id);
                String full = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("choice-oracle.css")), config, "", bodies[1] + probe, ChoiceBrowser.INDETERMINATE));
                String scoped = server.registerRaw(insertEnvironment(TextEntryBrowser.page(List.of(ParityFrames.generated("choice-legacy-scoped.css")), config, "", bodies[1] + probe, ChoiceBrowser.INDETERMINATE), environment));
                String free = server.registerRaw(insertEnvironment(TextEntryBrowser.page(List.of(ParityFrames.ASHLAR), config, "", bodies[0] + probe, ChoiceBrowser.INDETERMINATE), environment));
                String fullM;
                String scopedM;
                String freeM;
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    fullM = ChoiceParityTest.measure(cdp, full, fields);
                    scopedM = ChoiceParityTest.measure(cdp, scoped, fields);
                    freeM = ChoiceParityTest.measure(cdp, free, fields);
                    if (id.equals("light/none")) {
                        controlProbe = cdp.evaluate("(function(){var p=document.getElementById('probe').getBoundingClientRect();return p.width+'x'+p.height})()");
                    }
                }
                configurations++;
                for (ChoiceParityTest.Field f : fields) {
                    String prefix = f.id() + ":";
                    String where = id + " " + f.id() + " (" + f.spec().kind() + " " + f.spec().key() + ")";
                    FINDINGS_SCOPED_VS_FULL.addAll(TextEntryBrowser.differences(where + " scoped-vs-full", comparable(ChoiceParityTest.slice(fullM, prefix)), comparable(ChoiceParityTest.slice(scopedM, prefix)), APPLIED));
                    FINDINGS_FREE_VS_SCOPED.addAll(TextEntryBrowser.differences(where + " free-vs-scoped", comparable(ChoiceParityTest.slice(scopedM, prefix)), comparable(ChoiceParityTest.slice(freeM, prefix)), APPLIED));
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

    private static String free(Map<String, String> map, String field, String tag, String property) {
        return map.entrySet().stream().filter(e -> e.getKey().startsWith(field + ":") && e.getKey().contains(":" + tag + "|") && e.getKey().endsWith("|" + property))
                .map(Map.Entry::getValue).findFirst().orElseThrow(() -> new AssertionError("no " + property + " of " + tag + " in " + field));
    }

    /** As {@link #free} but the LAST matching element (a Radio Group's own help text follows its options' descriptions). */
    private static String freeLast(Map<String, String> map, String field, String tag, String property) {
        String found = null;
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (e.getKey().startsWith(field + ":") && e.getKey().contains(":" + tag + "|") && e.getKey().endsWith("|" + property)) {
                found = e.getValue();
            }
        }
        if (found == null) {
            throw new AssertionError("no " + property + " of " + tag + " in " + field);
        }
        return found;
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
    void theMigratedControlsStandOnTheirOwnWithoutTailwind() {
        Map<String, String> l = FREE_LIGHT;
        String cb = id("checkbox", "unchecked");
        assertEquals("16px", free(l, cb, "input", "width"));
        assertEquals("16px", free(l, cb, "input", "height"));
        assertEquals("2px", free(l, cb, "input", "margin-top"));
        assertEquals("0px", free(l, cb, "input", "margin-left"), "the browser's own 4px margin is gone: Preflight used to remove it and the component states it");
        assertEquals("0px", free(l, cb, "input", "margin-bottom"));
        assertEquals("auto", free(l, cb, "input", "appearance"), "the control stays native");
        assertEquals("pointer", free(l, cb, "input", "cursor"));
        assertEquals("14px", free(l, cb, "label", "font-size"));
        assertEquals("20px", free(l, cb, "label", "line-height"));
        assertEquals("500", free(l, cb, "label", "font-weight"));
        String help = id("checkbox", "help and error");
        assertEquals("2px", free(l, help, "p", "margin-top"), "a Checkbox's help and error margin");
        assertEquals("0px", free(l, help, "p", "margin-bottom"), "a paragraph's browser margin is gone");
        String rg = id("radio", "selected");
        assertEquals("0px", free(l, rg, "fieldset", "border-top-width"));
        assertEquals("0px", free(l, rg, "fieldset", "padding-top"));
        assertEquals("0px", free(l, rg, "fieldset", "padding-left"));
        assertEquals("0px", free(l, rg, "fieldset", "min-width"));
        assertEquals("0px", free(l, rg, "fieldset", "margin-left"));
        assertEquals("0px", free(l, rg, "legend", "padding-left"));
        assertEquals("8px", free(l, rg, "legend", "margin-bottom"));
        assertEquals("column", free(l, rg, "div", "flex-direction"));
        assertEquals("12px", free(l, rg, "div", "row-gap"));
        assertEquals("default", free(l, rg, "input", "cursor"), "a radio has no pointer cursor of its own");
        String horizontal = id("radio", "horizontal");
        assertTrue(FREE_LIGHT.entrySet().stream().anyMatch(e -> e.getKey().startsWith(horizontal + ":") && e.getKey().endsWith("|flex-wrap") && e.getValue().equals("wrap")), "horizontal options wrap");
        assertTrue(FREE_LIGHT.entrySet().stream().anyMatch(e -> e.getKey().startsWith(horizontal + ":") && e.getKey().endsWith("|column-gap") && e.getValue().equals("32px")), "with 32px between options");
        String helpOnly = id("radio", "help only");
        assertEquals("8px", freeLast(l, helpOnly, "p", "margin-top"), "a Radio Group's help and error margin (the last paragraph of the group)");
        String disabledGroup = id("radio", "whole group disabled");
        assertNotEquals(free(l, rg, "label", "color"), free(l, disabledGroup, "label", "color"), "a disabled group dims its labels");
        String ring = free(l, id("checkbox", "focus-visible"), "input", "box-shadow");
        assertTrue(TextEntryBrowser.visibleShadow(ring).startsWith("rgb(255, 255, 255) 0px 0px 0px 2px, "), "the focus ring keeps its white offset: " + ring);
        assertTrue(TextEntryBrowser.visibleShadow(free(FREE_DARK, id("checkbox", "focus-visible"), "input", "box-shadow")).startsWith("rgb(255, 255, 255) 0px 0px 0px 2px, "), "in dark mode too");
        assertEquals("0.6", free(l, id("checkbox", "disabled"), "input", "opacity"));
    }

    @Test
    void theScopedLegacyRenderingEqualsTheFullLegacyRendering() {
        assertEquals(6, configurations);
        assertEquals(List.of(), FINDINGS_SCOPED_VS_FULL.stream().limit(40).toList(), FINDINGS_SCOPED_VS_FULL.size() + " differences");
    }

    @Test
    void theMigratedControlsWithoutTailwindEqualTheLegacyControlsWithoutPageWideTailwind() {
        assertEquals(List.of(), FINDINGS_FREE_VS_SCOPED.stream().limit(40).toList(), FINDINGS_FREE_VS_SCOPED.size() + " differences, by property: " + byProperty(FINDINGS_FREE_VS_SCOPED));
    }

    @Test
    void onlyInvisibleShadowLayersWereNormalised() {
        assertEquals(List.of("invisible-shadow-layers"), List.copyOf(APPLIED.keySet()), APPLIED.toString());
    }
}
