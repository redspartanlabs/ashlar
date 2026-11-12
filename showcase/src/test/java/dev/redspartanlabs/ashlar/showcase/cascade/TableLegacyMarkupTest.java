package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The legacy rewrite of the Table family, and the frozen legacy oracle behind it: the class strings are read out of {@code table-legacy.html} (one source of truth for what
 * "legacy" means), the rewrite gives every migrated part exactly those, runtime states are rewritten to what {@code table.js} used to paint, a caller's width class is carried through as
 * the old template carried it, the Icons go through {@link IconLegacyMarkup}, and anything unknown fails loudly. The legacy script beside it is the original, byte for byte.
 */
class TableLegacyMarkupTest {

    /** The SHA-256 of table.js as it was at commit 37b02db (git blob 92669427fe500d1119feae8614629118995f2fb7), the commit before the Table migration. */
    private static final String ORIGINAL_SHA256 = "bbe4bd5a6a6d107c6d5eab917355d1ad8fa70a60935b8cfdb27b9bf9ddf1f1b8";

    private static TableLegacyMarkup legacy;

    @BeforeAll
    static void load() throws Exception {
        legacy = new TableLegacyMarkup();
    }

    private static String classOf(String html, String needle) {
        int at = html.indexOf(needle);
        assertTrue(at >= 0, "no " + needle + " in " + html);
        int open = html.lastIndexOf('<', at);
        Matcher m = Pattern.compile("class=\"([^\"]*)\"").matcher(html.substring(open, html.indexOf('>', at) + 1));
        assertTrue(m.find(), "no class on " + needle);
        return m.group(1);
    }

    @Test
    void theLegacyScriptFixtureIsTheOriginalByteForByte() throws Exception {
        try (InputStream in = TableLegacyMarkupTest.class.getResourceAsStream("/parity/table/table-legacy.js")) {
            assertEquals(ORIGINAL_SHA256, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(in.readAllBytes())), "the frozen legacy table.js was edited");
        }
    }

    @Test
    void theSourceOfTruthDefinesExactlyTheRolesAndTheOldTemplatesClassStrings() {
        assertEquals(46, TableLegacyMarkup.ROLES.size());
        assertEquals("overflow-hidden rounded-xl border border-slate-200 bg-white dark:border-slate-700 dark:bg-slate-900", legacy.classes("root"));
        assertEquals("divide-y divide-slate-200 dark:divide-slate-700", legacy.classes("body"));
        assertEquals("hover:bg-slate-50 dark:hover:bg-slate-800/60", legacy.classes("row"));
        assertEquals("sr-only", legacy.classes("caption-hidden"));
        assertTrue(legacy.allTokens().containsAll(List.of("divide-y", "rounded-xl", "bg-blue-50/60", "dark:bg-blue-950/40", "[text-transform:inherit]", "text-[0.8125rem]", "sm:w-64", "accent-blue-600")));
        assertTrue(legacy.allTokens().size() >= 100, "legacy tokens: " + legacy.allTokens().size());
    }

    @Test
    void everyPartOfAFullTableGetsItsLegacyClassesAndNoMigratedClassSurvives() {
        String migrated = TableBrowser.table(TableBrowser.o("captionVisible", true, "searchable", true, "selectable", true, "selected", List.of(0), "sort", "1:ascending", "pageSize", 2, "rows", 6, "page", 2));
        String rewritten = legacy.rewrite(migrated);
        assertFalse(Pattern.compile("class=\"[^\"]*ash-table").matcher(rewritten).find(), "a migrated Table class survived: " + rewritten);
        assertFalse(rewritten.contains("ash-sr-only"), "the shared utility was rewritten to sr-only wherever the Table used it");
        for (String role : List.of("root", "toolbar", "clear", "search", "search-icon", "search-input", "scroll", "table", "caption-visible", "head", "select", "check", "checkbox", "body", "empty-hidden",
                "footer", "status", "pager", "page-button", "page-indicator", "column-start", "column-end", "sort-start", "sort-end", "sort-icon-active", "row", "row-selected", "cell-left")) {
            String first = role.split("-")[0];
            assertTrue(rewritten.contains("\"" + legacy.classes(role) + "\"") || role.startsWith("column") || role.startsWith("sort") || role.startsWith("cell") || first.equals("empty"),
                    "no element carries the legacy " + role + " classes");
        }
        assertEquals(legacy.classes("root"), classOf(rewritten, "<div data-table "));
        assertEquals(legacy.classes("body"), classOf(rewritten, "<tbody"));
        assertEquals(legacy.classes("caption-visible"), classOf(rewritten, "<caption"));
        assertEquals(legacy.classes("page-button"), classOf(rewritten, "data-table-prev"));
        assertEquals(legacy.classes("checkbox"), classOf(rewritten, "data-table-select-all"));
        assertEquals(legacy.classes("search-input"), classOf(rewritten, "data-table-search"));
    }

    @Test
    void theHiddenAttributeBecomesTheHiddenClassOnTheFourPartsThatUsedIt() {
        String hidden = legacy.rewrite(TableBrowser.table(TableBrowser.o("selectable", true, "searchable", true, "filtered", true)));
        assertEquals(legacy.classes("selection"), classOf(hidden, "data-table-selection-bar"), "a hidden selection bar");
        assertEquals(legacy.classes("scroll-hidden"), classOf(hidden, "data-table-scroll"));
        assertEquals(legacy.classes("empty-hidden"), classOf(hidden, "data-table-empty=\"none\""));
        assertEquals(legacy.classes("empty"), classOf(hidden, "data-table-empty=\"filtered\""), "a shown empty state");
        assertFalse(Pattern.compile("\\shidden(?=[\\s>/])").matcher(hidden).find(), "no hidden attribute is left on those four: " + hidden);
        String shown = legacy.rewrite(TableBrowser.table(TableBrowser.o("empty", true)));
        assertEquals(legacy.classes("scroll-hidden"), classOf(shown, "data-table-scroll"));
        assertEquals(legacy.classes("empty"), classOf(shown, "data-table-empty=\"none\""));
        assertEquals(legacy.classes("empty-hidden"), classOf(shown, "data-table-empty=\"filtered\""));
        String paged = legacy.rewrite(TableBrowser.table(TableBrowser.o("pageSize", 2, "rows", 4, "page", 1)));
        assertEquals(2, count(paged, "<tr data-table-row data-row-key=\"ACC-100[34]\" class=\"hover:bg-slate-50 dark:hover:bg-slate-800/60\" hidden>"), "a row the script hides keeps its hidden attribute, as it always did");
    }

    private static int count(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        int n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }

    @Test
    void runtimeStatesAreRewrittenToWhatTheOldScriptPainted() {
        String sorted = legacy.rewrite(TableBrowser.table(TableBrowser.o("sort", "0:descending")));
        assertEquals(1, count(sorted, "class=\"" + Pattern.quote(legacy.classes("sort-icon-active")) + "\""), "only the sorted heading's glyph is the active colour");
        assertEquals(2, count(sorted, "class=\"" + Pattern.quote(legacy.classes("sort-icon")) + "\""), "the other two stay inactive");
        String selected = legacy.rewrite(TableBrowser.table(TableBrowser.o("selectable", true, "selected", List.of(1, 3))));
        assertEquals(2, count(selected, "class=\"" + Pattern.quote(legacy.classes("row-selected")) + "\""));
        assertEquals(2, count(selected, "class=\"" + Pattern.quote(legacy.classes("row")) + "\""));
        String bar = classOf(selected, "data-table-selection-bar");
        assertEquals(legacy.selectionShown(), bar);
        assertTrue(bar.endsWith(" flex") && !bar.contains("hidden"), "the script swapped hidden for flex: " + bar);
    }

    @Test
    void aCallersWidthClassIsCarriedThroughExactlyWhereTheOldTemplatePutIt() {
        String variants = legacy.rewrite(TableBrowser.table(TableBrowser.o("variants", true)));
        assertEquals("px-4 py-2.5 text-left w-32 font-semibold text-slate-900 dark:text-slate-100", classOf(variants, "w-32"));
        assertEquals("px-4 py-2.5 text-right font-semibold text-slate-900 dark:text-slate-100", classOf(variants, "data-table-type=\"number\""));
        assertEquals("px-4 py-2.5 text-center font-semibold text-slate-900 dark:text-slate-100", classOf(variants, "Flag"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<div class=\"ash-table w-full\"></div>"), "only a column takes the caller's classes");
    }

    @Test
    void theCellClassesAreComposedInTheOrderTheOldTemplateJoinedThem() {
        assertEquals("px-4 py-2.5 text-left break-words text-slate-700 dark:text-slate-300", classOf(legacy.rewrite("<td class=\"ash-table__cell\">x</td>"), "<td"));
        assertEquals("px-4 py-2.5 text-right font-mono text-[0.8125rem] whitespace-nowrap text-slate-700 dark:text-slate-300",
                classOf(legacy.rewrite("<td class=\"ash-table__cell ash-table__cell--end ash-table__cell--mono ash-table__cell--nowrap\">x</td>"), "<td"));
        assertEquals("px-4 py-2.5 text-center whitespace-nowrap font-medium text-slate-900 dark:text-slate-100",
                classOf(legacy.rewrite("<th scope=\"row\" class=\"ash-table__cell ash-table__cell--center ash-table__cell--nowrap ash-table__cell--header\">x</th>"), "<th"));
        assertEquals("text-slate-400 dark:text-slate-500", classOf(legacy.rewrite("<span aria-hidden=\"true\" class=\"ash-table__blank\">&mdash;</span>"), "<span"));
        assertEquals("sr-only", classOf(legacy.rewrite("<span class=\"ash-table__label ash-sr-only\">No value</span>"), "<span"));
    }

    @Test
    void theIconsInTheSearchFieldAndThePagerGoThroughTheIconRewriteToTheSizeTheOldTemplateRendered() throws Exception {
        String full = new IconLegacyMarkup().rewrite(legacy.rewrite(TableBrowser.table(TableBrowser.o("searchable", true, "pageSize", 2, "rows", 4))));
        Matcher m = Pattern.compile("<svg class=\"([^\"]*)\"").matcher(full);
        int icons = 0;
        while (m.find()) {
            assertEquals("inline-block shrink-0 h-4 w-4", m.group(1), "the old template passed classes = \"h-4 w-4\" to the Icon");
            icons++;
        }
        assertEquals(3, icons, "the search icon and the two pager icons");
    }

    @Test
    void otherMarkupIsLeftExactlyAsServedAndAnUnknownTableClassFailsLoudly() {
        String other = "<div class=\"p-4\"><span class=\"ash-badge ash-badge--info\">x</span><caption class=\"sr-only\">y</caption><table class=\"w-full\"><tr class=\"hover:bg-red-50\"></tr></table></div>";
        assertEquals(other, legacy.rewrite(other));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<div class=\"ash-table__bogus\"></div>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<span class=\"ash-table__label\">x</span>"), "a name without the shared utility is not a known part");
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<td class=\"ash-table__cell ash-table__cell--bogus\">x</td>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<button class=\"ash-table__sort ash-table__sort--bogus\">x</button>"));
    }

    @Test
    void countsOnlyExactParts() {
        String html = TableBrowser.table(TableBrowser.o("selectable", true, "rows", 4));
        assertEquals(4, TableLegacyMarkup.count(html, "ash-table__row"));
        assertEquals(5, TableLegacyMarkup.count(html, "ash-table__checkbox"), "one per row and the select-all");
        assertEquals(1, TableLegacyMarkup.count(html, "ash-table"));
        assertEquals(0, TableLegacyMarkup.count(html, "ash-tab"));
    }

    @Test
    void theMigratedClassValuesMapIsTheOnesTheFixturesWrite() {
        for (Map.Entry<String, String> e : TableLegacyMarkup.MIGRATED.entrySet()) {
            String firstToken = e.getValue().split("\\s+")[0];
            assertTrue(TableLegacyMarkup.ROLES.contains(e.getKey()), e.getKey());
            assertTrue(firstToken.startsWith("ash-"), e.getValue());
        }
    }
}
