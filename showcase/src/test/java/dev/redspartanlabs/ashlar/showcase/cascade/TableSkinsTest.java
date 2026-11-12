package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The three skins that style the Table, measured in a browser on the legacy rendering and the migrated one. Axiom sets the end-aligned data cells in its tabular monospace (and keeps a
 * control in such a cell in the reading face); Civic marks a selected row with an inset leading edge; Ledger recesses its text fields, the search field included, and sets the column
 * headings in capitals that the sort button must share. The migrated Table no longer carries the Tailwind classes ({@code text-right}, {@code bg-blue-50/60}, {@code bg-white}) those skin
 * rules were written against, so each rule was extended to the Table's own class or attribute; this test proves that the extension reaches the same elements and produces the same
 * rendering, and that a hand-rolled table the skin also styles is untouched by the change. {@link TableParityTest} compares everything else in all 22 configurations.
 *
 * <p>Every fact is measured twice, once with the legacy markup and the full legacy oracle and once with the migrated markup and the application's stylesheet, and the two must be equal.
 * The expectations below then pin the facts themselves, so "both sides lost the rule" cannot pass.
 */
class TableSkinsTest {

    private static final List<String> CONFIGS = List.of("light/none", "dark/none", "light/axiom", "dark/axiom", "light/civic", "dark/civic", "light/ledger", "dark/ledger");

    private static final Map<String, Map<String, String>> LEGACY = new LinkedHashMap<>();
    private static final Map<String, Map<String, String>> MIGRATED = new LinkedHashMap<>();

    /** One selectable, searchable table with every cell variety, and a button in its numeric cell; then a hand-rolled table with the Tailwind class the Axiom rule also serves. */
    private static String body() {
        String table = TableBrowser.table(TableBrowser.o("variants", true, "selectable", true, "selected", List.of(0), "searchable", true, "sortable", true))
                .replace(">1,204</td>", "><button type=\"button\" id=\"in-cell\">1,204</button></td>");
        String hand = "<div id=\"hand\"><table><thead><tr><th>Amount</th></tr></thead><tbody><tr><td class=\"text-right\" id=\"hand-td\">1,204</td></tr></tbody></table></div>";
        String probes = "<span id=\"p-mono\" style=\"font-family:var(--font-mono)\">m</span><span id=\"p-sans\" style=\"font-family:var(--font-sans)\">s</span>";
        return "<div data-field=\"sk\">" + table + "</div>" + hand + probes;
    }

    private static final Pattern PAIR = Pattern.compile("\"([^\"]+)\":\"((?:[^\"\\\\]|\\\\.)*)\"");

    /** Reads the skin-relevant facts, by structure rather than by class, so the same script measures both renderings. */
    private static final String SCRIPT = "(function(){"
            + "function q(s){return document.querySelector(s)}"
            + "function c(e,p){return e?getComputedStyle(e)[p]:'MISSING'}"
            + "function td(t){return [].slice.call(document.querySelectorAll('[data-field=sk] td')).filter(function(e){return e.textContent.trim()===t})[0]}"
            + "var amount=q('[data-field=sk] td[data-sort-value=\"1204\"]');"
            + "var head=q('[data-field=sk] th[data-table-type=\"number\"]');"
            + "var sort=q('[data-field=sk] th[data-table-type=\"text\"] [data-table-sort]');"
            + "var sortHead=sort&&sort.closest('th');"
            + "var sel=q('[data-field=sk] tr[data-selected]');var unsel=q('[data-field=sk] tr[data-table-row]:not([data-selected])');"
            + "var o={mono:c(q('#p-mono'),'fontFamily'),sans:c(q('#p-sans'),'fontFamily'),"
            + "amountFamily:c(amount,'fontFamily'),amountSize:c(amount,'fontSize'),amountNumeric:c(amount,'fontVariantNumeric'),"
            + "flagFamily:c(td('Yes'),'fontFamily'),flagSize:c(td('Yes'),'fontSize'),headFamily:c(head,'fontFamily'),headSize:c(head,'fontSize'),"
            + "buttonFamily:c(q('#in-cell'),'fontFamily'),buttonSize:c(q('#in-cell'),'fontSize'),"
            + "handFamily:c(q('#hand-td'),'fontFamily'),handSize:c(q('#hand-td'),'fontSize'),handNumeric:c(q('#hand-td'),'fontVariantNumeric'),"
            + "selectedShadow:c(sel,'boxShadow'),selectedFill:c(sel,'backgroundColor'),unselectedShadow:c(unsel,'boxShadow'),"
            + "searchFill:c(q('[data-field=sk] input[type=search]'),'backgroundColor'),"
            + "headTransform:c(sortHead,'textTransform'),sortTransform:c(sort,'textTransform'),headSpacing:c(sortHead,'letterSpacing'),sortSpacing:c(sort,'letterSpacing'),"
            + "headFace:c(sortHead,'fontFamily'),sortFace:c(sort,'fontFamily'),headFontSize:c(sortHead,'fontSize'),sortFontSize:c(sort,'fontSize')};"
            + "return JSON.stringify(o)})()";

    private static Map<String, String> read(DevToolsSession cdp, String url) throws Exception {
        cdp.navigate(url);
        String json = cdp.evaluate(SCRIPT);
        Map<String, String> facts = new LinkedHashMap<>();
        Matcher m = PAIR.matcher(json);
        while (m.find()) {
            facts.put(m.group(1), m.group(2).replace("\\\"", "\""));
        }
        assertTrue(facts.size() >= 25, "the script reads its facts: " + json);
        assertTrue(facts.values().stream().noneMatch("MISSING"::equals), "every element the script looks for exists: " + json);
        return facts;
    }

    @BeforeAll
    static void measure() throws Exception {
        ParitySession session = ParitySession.open("table-skins");
        String migrated = body();
        String legacy = new IconLegacyMarkup().rewrite(new TableLegacyMarkup().rewrite(migrated));
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                for (String id : CONFIGS) {
                    ParityConfigs.Config config = ParityConfigs.get(id);
                    String legacyUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("table-oracle.css")), config, "", legacy, ""));
                    String migratedUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.APP), config, "", migrated, ""));
                    LEGACY.put(id, read(cdp, legacyUrl));
                    MIGRATED.put(id, read(cdp, migratedUrl));
                }
            }
        }
    }

    private static String m(String config, String fact) {
        return MIGRATED.get(config).get(fact);
    }

    @Test
    void everySkinFactIsTheSameInTheLegacyAndTheMigratedRendering() {
        assertEquals(CONFIGS.size(), MIGRATED.size());
        for (String id : CONFIGS) {
            Map<String, String> expected = new LinkedHashMap<>(LEGACY.get(id));
            Map<String, String> actual = new LinkedHashMap<>(MIGRATED.get(id));
            for (String shadow : List.of("selectedShadow", "unselectedShadow")) {
                expected.put(shadow, TextEntryBrowser.visibleShadow(expected.get(shadow)));
                actual.put(shadow, TextEntryBrowser.visibleShadow(actual.get(shadow)));
            }
            assertEquals(expected, actual, id);
        }
    }

    @Test
    void axiomSetsTheEndAlignedDataCellsInTabularMonospaceButNotTheHeadingOrTheOtherCells() {
        for (String id : List.of("light/axiom", "dark/axiom")) {
            assertEquals(m(id, "mono"), m(id, "amountFamily"), id + ": the numeric cell is in the mono face");
            assertEquals("13px", m(id, "amountSize"), id);
            assertTrue(m(id, "amountNumeric").contains("tabular-nums"), id + ": " + m(id, "amountNumeric"));
            assertNotEquals(m(id, "mono"), m(id, "headFamily"), id + ": the numeric column HEADING stays in the label face");
            assertEquals("12px", m(id, "headSize"), id + ": Axiom's small heading size, not the 13px of a reading");
            assertNotEquals(m(id, "mono"), m(id, "flagFamily"), id + ": a centered data cell is not a numeric one");
            assertEquals("14px", m(id, "flagSize"), id);
        }
    }

    @Test
    void axiomKeepsAControlInANumericCellInTheReadingFaceAtItsOwnSize() {
        for (String id : List.of("light/axiom", "dark/axiom")) {
            assertEquals(m(id, "sans"), m(id, "buttonFamily"), id);
            assertEquals("14px", m(id, "buttonSize"), id);
            assertNotEquals(m(id, "mono"), m(id, "buttonFamily"), id);
        }
    }

    @Test
    void axiomStillStylesTheHandRolledTableThatCarriesTheTailwindClass() {
        for (String id : List.of("light/axiom", "dark/axiom")) {
            assertEquals(m(id, "mono"), m(id, "handFamily"), id);
            assertEquals("13px", m(id, "handSize"), id);
            assertTrue(m(id, "handNumeric").contains("tabular-nums"), id);
        }
    }

    @Test
    void withoutAxiomANumericCellIsNotMonospaced() {
        for (String id : List.of("light/none", "dark/none", "light/civic", "light/ledger")) {
            assertNotEquals(m(id, "mono"), m(id, "amountFamily"), id);
            assertEquals("14px", m(id, "amountSize"), id);
        }
    }

    @Test
    void civicMarksTheSelectedRowWithAnInsetLeadingEdgeInBothModesAndOnlyOnThatRow() {
        for (String id : List.of("light/civic", "dark/civic")) {
            String shadow = TextEntryBrowser.visibleShadow(m(id, "selectedShadow"));
            assertTrue(shadow.contains("inset") && shadow.contains("3px 0px 0px 0px"), id + ": " + shadow);
            assertEquals("none", TextEntryBrowser.visibleShadow(m(id, "unselectedShadow")), id + ": an unselected row has no edge");
        }
        for (String id : List.of("light/none", "dark/none", "light/axiom", "light/ledger")) {
            assertEquals("none", TextEntryBrowser.visibleShadow(m(id, "selectedShadow")), id + ": only Civic draws the edge");
        }
    }

    @Test
    void civicStillFillsTheSelectedRow() {
        assertNotEquals("rgba(0, 0, 0, 0)", m("light/none", "selectedFill"), "a selected row is filled");
        assertNotEquals("rgba(0, 0, 0, 0)", m("dark/none", "selectedFill"), "a selected row is filled in dark mode");
    }

    @Test
    void ledgerRecessesTheSearchFieldInLightModeOnly() {
        assertNotEquals("rgb(255, 255, 255)", m("light/ledger", "searchFill"), "Ledger's light text fields are recessed, the search field too");
        assertEquals("rgb(255, 255, 255)", m("light/none", "searchFill"), "no skin: the plain white field");
        assertNotEquals(m("light/ledger", "searchFill"), m("dark/ledger", "searchFill"), "dark mode keeps the dark surface");
        assertNotEquals("rgb(248, 250, 252)", m("dark/ledger", "searchFill"), "the near-white light recess did not leak into dark mode");
    }

    @Test
    void ledgerSetsTheHeadingsInCapitalsAndTheSortButtonSharesThem() {
        assertEquals("uppercase", m("light/ledger", "headTransform"));
        for (String id : List.of("light/ledger", "dark/ledger")) {
            assertEquals(m(id, "headTransform"), m(id, "sortTransform"), id + ": the sort button reads its heading's casing");
            assertEquals(m(id, "headSpacing"), m(id, "sortSpacing"), id + ": and its letter spacing");
            assertEquals(m(id, "headFace"), m(id, "sortFace"), id + ": and its face");
            assertEquals(m(id, "headFontSize"), m(id, "sortFontSize"), id + ": and its size");
        }
        assertEquals("none", m("light/none", "headTransform"));
    }
}
