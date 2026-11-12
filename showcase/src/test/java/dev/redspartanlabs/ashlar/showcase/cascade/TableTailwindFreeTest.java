package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Table family without Tailwind. Three renderings of the same tables in six configurations are measured, every element for {@link TableBrowser#PROPERTIES}: the legacy tables with the
 * full oracle (Tailwind and its preflight over the whole page); the legacy tables with only the utilities and the preflight rules they used (the scoped stylesheet, in the same bare page
 * environment); and the migrated tables with Ashlar's stylesheet alone, in that environment. The scoped legacy rendering must equal the full one (so it is a faithful stand-in), and the
 * migrated rendering without Tailwind must equal the scoped legacy one. That is what shows the migrated family stands on its own: every reset Preflight used to supply (the table's collapsed
 * borders, a button's and an input's font, colour and background, a paragraph's margin, a heading cell's weight) is stated by the stylesheet that needs it.
 *
 * <p>What is not compared, and why: the style of a zero-width border (invisible at width 0, and Preflight's blanket {@code border: 0 solid} gives every element a {@code solid} style the bare
 * page lacks), and the colour of a zero-width border on the elements that have no border of their own (a {@code currentcolor} that Preflight's reset and the browser's own default resolve
 * differently, never drawn). Fully transparent zero-size shadow layers are normalised as in {@link TableParityTest}. Nothing else is excluded.
 *
 * <p>Firefox and Safari are not covered: only Chrome is driven.
 */
class TableTailwindFreeTest {

    private static final List<String> CONFIGS = List.of("light/none", "dark/none", "light/ledger", "light/foundry", "dark/axiom", "dark/harbor");

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
        // A side whose border has no width draws nothing, whatever its colour: collect those sides first.
        java.util.Set<String> zeroWidth = new java.util.HashSet<>();
        measurement.lines().forEach(l -> {
            String[] p = l.split("\t", -1);
            if (p.length == 3 && p[1].matches("border-(?:top|right|bottom|left)-width") && p[2].equals("0px")) {
                zeroWidth.add(p[0] + "|" + p[1].replace("-width", "-color"));
            }
        });
        return measurement.lines().filter(l -> {
            String[] p = l.split("\t", -1);
            if (p.length != 3) {
                return true;
            }
            if (p[1].matches("border-(?:top|right|bottom|left)-style") || zeroWidth.contains(p[0] + "|" + p[1])) {
                return false;
            }
            // The unclassed span and tr get Preflight's universal border-box from nothing but the bare page's lack of it; neither has padding, border or a width of its own, and every box is compared.
            // The test's own wrapper div (element 0 of every field) is scaffolding, not part of the component, and is unmarked, so it has no reset on the scoped page.
            return !(p[1].equals("box-sizing") && (p[0].endsWith(":span") || p[0].endsWith(":tr") || p[0].endsWith(":0:div")));
        }).collect(Collectors.joining("\n"));
    }

    @BeforeAll
    static void measure() throws Exception {
        ParitySession session = ParitySession.open("table-tailwind-free");
        List<TableParityTest.Field> fields = TableParityTest.fields();
        // The variants table's last column carries a width class the CALLER supplies ({@code w-32}). It is the caller's own Tailwind utility, not the component's: the full oracle happens to
        // contain it, while the scoped stylesheet holds only what the component itself used, and without Tailwind no page has it at all. It is taken out of all three renderings so that the
        // comparison is about the component; the passthrough itself is covered, with Tailwind, by TableParityTest (same class on both sides, 22 configurations).
        String[] bodies = TableParityTest.bodies(fields);
        for (int i = 0; i < bodies.length; i++) {
            bodies[i] = bodies[i].replaceAll(" w-32(?=[\" ])", "");
        }
        String probe = "<div id=\"probe\" class=\"h-4 w-4\"></div>";
        String environment = ParityFrames.environment(session.generatedDir());
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            for (String id : CONFIGS) {
                ParityConfigs.Config config = ParityConfigs.get(id);
                String full = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("table-oracle.css")), config, "", bodies[1] + probe, TableBrowser.INDETERMINATE));
                String scoped = server.registerRaw(insertEnvironment(TextEntryBrowser.page(List.of(ParityFrames.generated("table-legacy-scoped.css")), config, "", bodies[1] + probe, TableBrowser.INDETERMINATE), environment));
                String free = server.registerRaw(insertEnvironment(TextEntryBrowser.page(List.of(ParityFrames.ASHLAR), config, "", bodies[0] + probe, TableBrowser.INDETERMINATE), environment));
                String fullM;
                String scopedM;
                String freeM;
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    fullM = TableParityTest.measure(cdp, full, fields, null, true);
                    scopedM = TableParityTest.measure(cdp, scoped, fields, null, true);
                    freeM = TableParityTest.measure(cdp, free, fields, null, true);
                    if (id.equals("light/none")) {
                        controlProbe = cdp.evaluate("(function(){var p=document.getElementById('probe').getBoundingClientRect();return p.width+'x'+p.height})()");
                    }
                }
                configurations++;
                for (TableParityTest.Field f : fields) {
                    String prefix = f.id() + ":";
                    String where = id + " " + f.id() + " (" + f.spec().key() + ")";
                    FINDINGS_SCOPED_VS_FULL.addAll(TextEntryBrowser.differences(where + " scoped-vs-full", comparable(TableParityTest.slice(fullM, prefix)), comparable(TableParityTest.slice(scopedM, prefix)), APPLIED));
                    FINDINGS_FREE_VS_SCOPED.addAll(TextEntryBrowser.differences(where + " free-vs-scoped", comparable(TableParityTest.slice(scopedM, prefix)), comparable(TableParityTest.slice(freeM, prefix)), APPLIED));
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

    private static String id(String key) {
        for (int i = 0; i < TableBrowser.CASES.size(); i++) {
            if (TableBrowser.CASES.get(i).key().equals(key)) {
                return "tb" + (i + 1);
            }
        }
        throw new AssertionError("no case " + key);
    }

    /** The first element of the given tag in a case, and one property of it. */
    private static String free(Map<String, String> map, String key, String tag, String property) {
        String field = id(key);
        return map.entrySet().stream().filter(e -> e.getKey().startsWith(field + ":") && e.getKey().contains(":" + tag + "|") && e.getKey().endsWith("|" + property))
                .map(Map.Entry::getValue).findFirst().orElseThrow(() -> new AssertionError("no " + property + " of " + tag + " in " + key));
    }

    /** As {@link #free}, but the {@code nth} (0-based) element of that tag in the case. */
    private static String freeAt(Map<String, String> map, String key, String tag, int nth, String property) {
        String field = id(key);
        return map.entrySet().stream().filter(e -> e.getKey().startsWith(field + ":") && e.getKey().contains(":" + tag + "|") && e.getKey().endsWith("|" + property))
                .skip(nth).map(Map.Entry::getValue).findFirst().orElseThrow(() -> new AssertionError("no " + property + " of " + tag + " #" + nth + " in " + key));
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
    void theScopedLegacyStylesheetRendersTheLegacyTablesExactlyAsTheFullOracleDoes() {
        assertEquals(CONFIGS.size(), configurations);
        assertEquals(List.of(), FINDINGS_SCOPED_VS_FULL.stream().limit(40).toList(), FINDINGS_SCOPED_VS_FULL.size() + " differences, by element and property: " + byProperty(FINDINGS_SCOPED_VS_FULL));
    }

    @Test
    void theMigratedTablesWithoutTailwindEqualTheScopedLegacyTables() {
        assertEquals(List.of(), FINDINGS_FREE_VS_SCOPED.stream().limit(40).toList(), FINDINGS_FREE_VS_SCOPED.size() + " differences, by element and property: " + byProperty(FINDINGS_FREE_VS_SCOPED));
    }

    @Test
    void theMigratedTablesStandOnTheirOwnWithoutTailwind() {
        Map<String, String> l = FREE_LIGHT;
        assertEquals("collapse", free(l, "plain", "table", "border-collapse"), "Preflight's collapsed borders are stated");
        assertEquals("0px", free(l, "plain", "table", "text-indent"));
        assertEquals("14px", free(l, "plain", "table", "font-size"));
        assertEquals("1px", free(l, "plain", "caption", "width"), "a hidden caption is the visually hidden box");
        assertEquals("16px", free(l, "caption visible", "caption", "font-size"));
        assertEquals("10px", free(l, "plain", "th", "padding-top"));
        assertEquals("16px", free(l, "plain", "th", "padding-left"));
        assertEquals("600", free(l, "plain", "th", "font-weight"), "the heading weight is stated, not the browser's bold");
        assertEquals("left", free(l, "plain", "th", "text-align"));
        assertEquals("1px", freeAt(l, "plain", "tr", 1, "border-bottom-width"), "the first body row carries the divider");
        assertEquals("0px", freeAt(l, "plain", "tr", 4, "border-bottom-width"), "and the last does not");
        assertEquals("0px", free(l, "searchable", "p", "margin-top"), "a paragraph's browser margin is gone");
        assertEquals("14px", free(l, "searchable", "input", "font-size"), "a search field reads the table's size, not the browser's 13.33px");
        assertEquals("32px", free(l, "searchable", "input", "padding-left"));
        assertEquals("1px", free(l, "searchable", "input", "border-top-width"));
        assertEquals("16px", free(l, "selectable, nothing selected", "input", "width"));
        assertEquals("0px", free(l, "selectable, nothing selected", "input", "margin-top"), "the browser's own checkbox margin is gone");
        assertEquals("pointer", free(l, "selectable, nothing selected", "input", "cursor"));
        assertEquals("flex", free(l, "selectable, nothing selected", "label", "display"));
        assertEquals("flex", free(l, "paged, first page", "button", "display"), "a page button is inline-flex, blockified as an item of the pager's flex row");
        assertEquals("0px", free(l, "paged, first page", "button", "margin-top"));
        assertEquals("rgba(0, 0, 0, 0)", free(l, "paged, first page", "button", "background-color"), "a button's browser grey is gone");
        assertEquals("0.5", free(l, "paged, first page", "button", "opacity"), "the first page's Previous is the disabled button, dimmed");
        assertEquals("inline-flex", free(l, "sortable, none sorted", "button", "display"));
        assertEquals("600", free(l, "sortable, none sorted", "button", "font-weight"));
        assertEquals(free(l, "sortable, none sorted", "th", "text-transform"), free(l, "sortable, none sorted", "button", "text-transform"), "the sort button reads its heading's casing");
        assertTrue(TextEntryBrowser.visibleShadow(free(l, "search focus", "input", "box-shadow")).contains("0px 0px 0px 2px"), "the search field draws its ring: " + free(l, "search focus", "input", "box-shadow"));
        assertTrue(TextEntryBrowser.visibleShadow(freeAt(FREE_DARK, "row checkbox focus-visible", "input", 1, "box-shadow")).startsWith("rgb(255, 255, 255) 0px 0px 0px 2px, "), "the row checkbox ring keeps its white offset, in dark mode too");
        assertNotEquals(freeAt(l, "plain", "div", 1, "background-color"), freeAt(FREE_DARK, "plain", "div", 1, "background-color"), "the dark theme still flips the surface");
        assertEquals("12px", freeAt(l, "plain", "div", 1, "border-top-left-radius"), "the surface radius is the Tailwind --radius-xl");
    }
}
