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
 * Visual parity of the Table family with Tailwind PRESENT, in all 22 configurations (two themes, no skin and the ten skins): the legacy rendering (the migrated markup rewritten to the
 * Tailwind classes the templates used to write, with the cumulative Tailwind oracle) against the migrated rendering (the real application stylesheet, Tailwind and Ashlar together).
 * Every element of every table is measured for {@link TableBrowser#PROPERTIES}, with its box, in every state the family has: plain, with a visible caption, sortable, sorted both ways,
 * searchable, selectable with nothing, some and every row selected, paged on the first, a middle and the last page, loading, empty, filtered to nothing, with a footer slot, with every
 * cell and column variety, and with everything on at once; and in the states a pointer or keyboard puts it in (hover on a row, a selected row, a sort button, a pager button, the clear
 * button and the search field; keyboard focus on each control).
 *
 * <p>Focus and hover are forced on the real control with the browser's own pseudo-state API (:focus together with :focus-visible, as a keyboard focus has both), on a browser that is
 * stated to be able to hover. The comparison is EXACT except one documented normalisation, Text Entry's: the computed {@code box-shadow} with its fully transparent zero-size layers
 * removed ({@link TextEntryBrowser#visibleShadow}). The pictures are compared by {@link TablePixelTest}. The nested Spinner and Empty State are not part of these fixtures (see
 * {@link TableBrowser}).
 */
class TableParityTest {

    private static final List<String> FINDINGS = new ArrayList<>();
    private static final Map<String, Integer> APPLIED = new LinkedHashMap<>();
    private static final Map<String, String> PROBES_LIGHT = new LinkedHashMap<>();
    private static final Map<String, String> PROBES_DARK = new LinkedHashMap<>();
    private static int configurations;
    private static int measuredFields;
    private static int measuredValues;

    record Field(String id, TableBrowser.Case spec, String markup) {
    }

    static List<Field> fields() {
        List<Field> fields = new ArrayList<>();
        int i = 0;
        for (TableBrowser.Case c : TableBrowser.CASES) {
            String id = "tb" + (++i);
            fields.add(new Field(id, c, "<div data-field=\"" + id + "\">" + TableBrowser.marked(TableBrowser.table(c.options())) + "</div>"));
        }
        return fields;
    }

    /** The migrated markup of every field, and the same markup rewritten to the legacy classes (the Icons last, as in the real chain). */
    static String[] bodies(List<Field> fields) throws Exception {
        StringBuilder migrated = new StringBuilder();
        fields.forEach(f -> migrated.append(f.markup()));
        String legacy = new IconLegacyMarkup().rewrite(new TableLegacyMarkup().rewrite(migrated.toString()));
        return new String[] {migrated.toString(), legacy};
    }

    /** A few facts about the rendering, read straight from the browser, so the tests can show the comparison would notice a difference. */
    private static final String PROBE = "(function(f){function q(s){return document.querySelector('[data-field=\"'+f+'\"] '+s)}function c(s,p){var e=q(s);return e?getComputedStyle(e)[p]:'-'}"
            + "return ['icon='+c('[data-table-sort-icon]','color'),'row2='+c('tbody tr:nth-child(2)','backgroundColor'),'row1border='+c('tbody tr:first-child','borderBottomWidth'),"
            + "'lastborder='+c('tbody tr:last-child','borderBottomWidth'),'row2border='+c('tbody tr:nth-child(2)','borderBottomWidth'),'caption='+c('caption','fontSize'),'captionW='+c('caption','width'),'next='+c('[data-table-next]','opacity'),"
            + "'prev='+c('[data-table-prev]','opacity'),'bar='+c('[data-table-selection-bar]','display'),'scroll='+c('[data-table-scroll]','display'),'none='+c('[data-table-empty=\"none\"]','display'),"
            + "'filtered='+c('[data-table-empty=\"filtered\"]','display'),'search='+c('[data-table-search]','boxShadow'),'mono='+c('td:nth-child(2)','fontSize'),'hiddenrow='+c('tbody tr[hidden]','display'),"
            + "'check='+c('[data-table-select-row]','boxShadow'),'sortcolor='+c('[data-table-sort]','color')].join(';')})";

    static String measure(DevToolsSession cdp, String url, List<Field> fields, Map<String, String> probes) throws Exception {
        return measure(cdp, url, fields, probes, false);
    }

    static String measure(DevToolsSession cdp, String url, List<Field> fields, Map<String, String> probes, boolean skipRaw) throws Exception {
        cdp.navigate(url);
        String sheets = cdp.evaluate("(function(){var ok=[].every.call(document.styleSheets,function(s){try{return s.cssRules.length>0}catch(e){return false}});return document.styleSheets.length+':'+ok})()");
        assertTrue(sheets.endsWith(":true") && !sheets.startsWith("0:"), "every stylesheet must have loaded - infrastructure failure, not a parity result: " + sheets);
        StringBuilder all = new StringBuilder();
        for (Field f : fields) {
            List<String> pseudo = f.spec().pseudo();
            List<Integer> nodes = pseudo.isEmpty() ? List.of() : cdp.nodes(TableBrowser.target(f.spec(), f.id()));
            if (!pseudo.isEmpty()) {
                assertFalse(nodes.isEmpty(), f.id() + " " + f.spec().key() + ": the target " + f.spec().target() + " was not found");
                cdp.force(nodes, pseudo);
            }
            all.append(cdp.evaluate(TableBrowser.measureScript(f.id(), skipRaw))).append('\n');
            if (probes != null) {
                probes.put(f.id(), cdp.evaluate(PROBE + "('" + f.id() + "')"));
            }
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

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("table-parity");
        List<Field> fields = fields();
        String[] bodies = bodies(fields);
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            for (ParityConfigs.Config config : ParityConfigs.all()) {
                String legacyUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("table-oracle.css")), config, "", bodies[1], TableBrowser.INDETERMINATE));
                String migratedUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.APP), config, "", bodies[0], TableBrowser.INDETERMINATE));
                Map<String, String> probes = config.id().equals("light/none") ? PROBES_LIGHT : config.id().equals("dark/none") ? PROBES_DARK : null;
                String legacy;
                String migrated;
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    legacy = measure(cdp, legacyUrl, fields, probes);
                    migrated = measure(cdp, migratedUrl, fields, null);
                }
                configurations++;
                measuredFields += fields.size();
                measuredValues += legacy.split("\n").length;
                for (Field f : fields) {
                    FINDINGS.addAll(TextEntryBrowser.differences(config.id() + " " + f.id() + " (" + f.spec().key() + ")", slice(legacy, f.id() + ":"), slice(migrated, f.id() + ":"), APPLIED));
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

    private static String probe(Map<String, String> probes, String key, String name) {
        for (String part : probes.get(id(key)).split(";")) {
            if (part.startsWith(name + "=")) {
                return part.substring(name.length() + 1);
            }
        }
        throw new AssertionError("no probe " + name + " in " + probes.get(id(key)));
    }

    /** Which properties differ, and how many times, so a failure says what moved before it lists instances. */
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
            first.putIfAbsent(p.length > 2 ? p[1].replaceAll("^\\S+:\\d+:", "") + "." + p[2] : p[1], finding);
        }
        return List.copyOf(first.values());
    }

    @Test
    void theMigratedTablesAreIdenticalToTheLegacyOnesInAll22Configurations() {
        assertEquals(22, configurations);
        assertEquals(List.of(), FINDINGS.stream().limit(60).toList(), FINDINGS.size() + " differences, by property: " + summary() + "; first of each: " + firstOfEach());
    }

    @Test
    void theMeasurementCoversEveryStateAndIsLargeEnoughToMeanSomething() {
        assertEquals(22 * TableBrowser.CASES.size(), measuredFields);
        assertTrue(TableBrowser.CASES.size() >= 30, "cases: " + TableBrowser.CASES.size());
        assertTrue(measuredValues >= 22 * 50000, "values measured per rendering: " + measuredValues);
    }

    @Test
    void theLegacyOracleShowsTheStatesTheComparisonClaimsToCover() {
        Map<String, String> l = PROBES_LIGHT;
        assertEquals("1px", probe(l, "plain", "row1border"), "a row but the last carries the divider (Tailwind's divide-y)");
        assertEquals("0px", probe(l, "plain", "lastborder"), "and the last does not");
        assertEquals("1px", probe(l, "paged, first page", "row2border"), "a hidden row is still a child, so the last VISIBLE row keeps its divider when hidden rows follow it");
        assertEquals("0px", probe(l, "paged, first page", "lastborder"), "while the last child, here a hidden row, has none");
        assertEquals("none", probe(l, "paged, first page", "hiddenrow"), "a row the script hides is display: none");
        assertEquals("1px", probe(l, "plain", "captionW"), "a hidden caption is the 1px visually-hidden box");
        assertEquals("16px", probe(l, "caption visible", "caption"), "a visible caption is text-base");
        assertNotEquals(probe(l, "sortable, none sorted", "icon"), probe(l, "sorted ascending", "icon"), "the active sort glyph is blue, the resting one slate");
        assertNotEquals(probe(l, "plain", "row2"), probe(l, "selectable, two selected", "row2"), "a selected row has a fill");
        assertEquals("flex", probe(l, "selectable, two selected", "bar"), "a shown selection bar is flex");
        assertEquals("none", probe(l, "selectable, nothing selected", "bar"), "and a hidden one is not displayed");
        assertEquals("none", probe(l, "empty from the server", "scroll"));
        assertEquals("block", probe(l, "empty from the server", "none"));
        assertEquals("none", probe(l, "plain", "none"));
        assertEquals("block", probe(l, "filtered to nothing", "filtered"));
        assertEquals("0.5", probe(l, "paged, first page", "prev"), "a disabled pager button is dimmed");
        assertEquals("1", probe(l, "paged, first page", "next"));
        assertTrue(TextEntryBrowser.visibleShadow(probe(l, "search focus", "search")).contains("0px 0px 0px 2px"), "the search field draws its focus ring: " + probe(l, "search focus", "search"));
        assertEquals("none", TextEntryBrowser.visibleShadow(probe(l, "searchable", "search")), "and only when focused");
        assertEquals("13px", probe(l, "cell and column variants", "mono"), "a monospaced cell is 0.8125rem");
        assertNotEquals(probe(l, "plain", "row2"), probe(l, "row hover", "row2"), "a hovered row is tinted");
        assertTrue(TextEntryBrowser.visibleShadow(probe(l, "row checkbox focus-visible", "check")).contains("0px 0px 0px 4px"), "a checkbox draws its ring with a white offset");
        assertNotEquals(probe(PROBES_DARK, "sortable, none sorted", "icon"), probe(l, "sortable, none sorted", "icon"), "the dark theme has its own glyph colour");
        assertNotEquals(probe(PROBES_DARK, "sorted ascending", "icon"), probe(PROBES_DARK, "sortable, none sorted", "icon"));
    }

    @Test
    void onlyInvisibleShadowLayersWereNormalisedAndOnlyOnTheFocusRings() {
        assertEquals(List.of("invisible-shadow-layers"), List.copyOf(APPLIED.keySet()), "no other normalisation exists: " + APPLIED);
        assertTrue(APPLIED.get("invisible-shadow-layers") > 100, "Tailwind's unused transparent ring layers are what the migrated rule leaves out: " + APPLIED);
    }
}
