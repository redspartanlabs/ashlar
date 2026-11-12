package dev.redspartanlabs.ashlar.css;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the source contract of the Table family (Table, Table Column, Table Row, Table Cell, and Data Table, which renders through them), migrated together with {@code table.js}. The
 * templates keep every parameter, default, native element, scope, ARIA attribute and data- hook, carry only Ashlar's classes (plus the caller's own {@code width} class, passed
 * through exactly as before), and render no inline style; Data Table has no class of its own; the script writes state and nothing presentational. The rendered result is measured in a
 * real browser by the showcase's {@code TableParityTest}, {@code TablePixelTest}, {@code TableTailwindFreeTest}, {@code TableHoverMediaTest}, {@code TableSkinsTest},
 * {@code TableLegacyBehaviourTest} and {@code TableScriptTest}; this reads the sources only.
 */
class TableContractTest {

    private static Path module;

    @BeforeAll
    static void locate() {
        String generated = System.getProperty("ashlar.css.generated");
        assertNotNull(generated, "ashlar.css.generated must be set by the Gradle test task");
        for (Path p = Path.of(generated).toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("src/main/css/ashlar.css"))) {
                module = p;
                break;
            }
        }
        assertNotNull(module, "could not find the ashlar module above " + generated);
    }

    private static String read(String relative) throws IOException {
        return Files.readString(module.resolve(relative), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static String template(String name) throws IOException {
        return read("src/main/jte/ashlar/components/" + name + ".jte");
    }

    private static String script() throws IOException {
        return read("src/main/resources/META-INF/resources/ashlar/js/table.js");
    }

    private static String markup(String name) throws IOException {
        return template(name).replaceAll("(?s)<%--.*?--%>", "").replaceAll("(?m)^@(?:param|import) .*$", "").replaceAll("(?m)^!\\{.*\\}$", "").replaceAll("\\s+", " ").replace(" >", ">").trim();
    }

    private static List<String> declared(String name) throws IOException {
        List<String> declared = new ArrayList<>();
        Matcher m = Pattern.compile("(?m)^@(?:param|import) .*$").matcher(template(name));
        while (m.find()) {
            declared.add(m.group());
        }
        return declared;
    }

    private static int count(String haystack, String needle) {
        int n = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }

    // ---- the templates ----

    @Test
    void theParametersAndTheirDefaultsAreUnchanged() throws IOException {
        assertEquals(List.of("@import dev.redspartanlabs.ashlar.icon.Icon", "@import dev.redspartanlabs.ashlar.table.TableContext", "@import dev.redspartanlabs.ashlar.AshlarAssets",
                "@param String caption", "@param gg.jte.Content columns", "@param gg.jte.Content rows", "@param boolean captionVisible = false", "@param boolean searchable = false",
                "@param boolean selectable = false", "@param String searchPlaceholder = \"Search\"", "@param int pageSize = 0", "@param boolean empty = false",
                "@param boolean loading = false", "@param String emptyTitle = \"No records found\"", "@param String emptyBody = \"\"", "@param gg.jte.Content footer = null"),
                declared("table"));
        assertEquals(List.of("@param String label", "@param boolean sortable = false", "@param String align = \"left\"", "@param String type = \"text\"", "@param String width = \"\""),
                declared("table-column"));
        assertEquals(List.of("@import dev.redspartanlabs.ashlar.table.TableContext", "@param gg.jte.Content cells", "@param String rowKey = \"\"", "@param String rowLabel = \"\""),
                declared("table-row"));
        assertEquals(List.of("@param String text = \"\"", "@param gg.jte.Content content = null", "@param String align = \"left\"", "@param boolean header = false",
                "@param boolean mono = false", "@param boolean nowrap = false", "@param String sortValue = \"\""), declared("table-cell"));
        assertEquals(List.of("@import java.util.List", "@import dev.redspartanlabs.ashlar.datatable.DataTableColumn", "@import dev.redspartanlabs.ashlar.datatable.DataTableRow",
                "@param String caption", "@param List<DataTableColumn> columns", "@param List<DataTableRow> rows", "@param boolean captionVisible = false", "@param String rowKey = \"\"",
                "@param int pageSize = 10", "@param boolean selectable = false", "@param boolean searchable = true", "@param String searchPlaceholder = \"Search\"",
                "@param String emptyTitle = \"No records found\"", "@param String emptyBody = \"\""), declared("data-table"));
    }

    @Test
    void theTableKeepsItsElementsHooksAndRegions() throws IOException {
        String m = markup("table");
        for (String fragment : List.of(
                "<div data-table data-table-page-size=\"${pageSize}\" class=\"ash-table\">",
                "<div class=\"ash-table__toolbar\">",
                "<div data-table-selection-bar class=\"ash-table__selection\" hidden>",
                "<span data-table-selection-count></span>",
                "<button type=\"button\" data-table-clear-selection class=\"ash-table__clear\"> Clear selection </button>",
                "<label class=\"ash-table__search\">",
                "<input type=\"search\" data-table-search placeholder=\"${searchPlaceholder}\" class=\"ash-table__search-input\">",
                "<div data-table-loading class=\"ash-table__loading\">",
                "@template.ashlar.components.spinner(size = \"Small\", label = \"\")",
                "<div data-table-scroll class=\"ash-table__scroll\" hidden=\"${empty}\">",
                "<table class=\"ash-table__table\">",
                "<caption class=\"${captionClasses}\">${caption}</caption>",
                "<thead class=\"ash-table__head\">",
                "<th scope=\"col\" class=\"ash-table__select\">",
                "<input type=\"checkbox\" data-table-select-all class=\"ash-table__checkbox\">",
                "<tbody data-table-body class=\"ash-table__body\">",
                "<div data-table-empty=\"none\" class=\"ash-table__empty\" hidden=\"${!empty}\">",
                "<div data-table-empty=\"filtered\" class=\"ash-table__empty\" hidden>",
                "<div class=\"ash-table__footer\">",
                "<p data-table-status aria-live=\"polite\" class=\"ash-table__status\"></p>",
                "<div data-table-pagination class=\"ash-table__pager\">",
                "<button type=\"button\" data-table-prev class=\"ash-table__page-button\">",
                "<button type=\"button\" data-table-next class=\"ash-table__page-button\">",
                "<span data-table-page-indicator class=\"ash-table__page-indicator\"></span>",
                "<script type=\"module\" src=\"${AshlarAssets.js(\"table.js\")}\"></script>")) {
            assertTrue(m.contains(fragment), "missing: " + fragment);
        }
        assertTrue(template("table").contains("!{String captionClasses = captionVisible ? \"ash-table__caption\" : \"ash-sr-only\";}"), "a caption is shown with its own class and hidden with the shared utility");
        assertTrue(template("table").contains("if (selectable) TableContext.enterSelectable();") && template("table").contains("TableContext.exit();"), "the selectable hand-off to the rows is unchanged");
        assertEquals(3, count(m, "classes = \"ash-icon--sm\""), "the search icon and the two pager icons use the small Icon modifier");
        assertTrue(m.contains("icon = Icon.SEARCH, classes = \"ash-icon--sm\""), "the search icon uses the small Icon modifier");
        assertEquals(2, count(m, "class=\"ash-table__label ash-sr-only\""), "the two visually hidden names in the Table itself (the search field's and select-all's) are marked");
    }

    @Test
    void theColumnKeepsItsHeadingSortButtonAndTheCallersWidthPassthrough() throws IOException {
        String m = markup("table-column");
        assertTrue(m.contains("<th scope=\"col\" data-table-column data-table-type=\"${type}\" aria-sort=\"${ariaSort}\" class=\"ash-table__column${alignClass}${widthClass}\">"), m);
        assertTrue(m.contains("<button type=\"button\" data-table-sort class=\"ash-table__sort${justifyClass}\">"));
        assertTrue(m.contains("<span data-table-sort-icon aria-hidden=\"true\" class=\"ash-table__sort-icon\">&#8645;</span>"));
        String t = template("table-column");
        assertTrue(t.contains("!{String widthClass = width.isEmpty() ? \"\" : \" \" + width;}"), "the width class is appended exactly as the caller wrote it, as before");
        assertTrue(t.contains("!{String ariaSort = sortable ? \"none\" : null;}"), "a sortable heading starts at aria-sort none");
        assertTrue(t.contains("\"right\".equals(align) ? \" ash-table__column--end\" : \"center\".equals(align) ? \" ash-table__column--center\" : \"\""));
        assertTrue(t.contains("!{String justifyClass = \"right\".equals(align) ? \" ash-table__sort--end\" : \"center\".equals(align) ? \" ash-table__sort--center\" : \"\";}"), "the sort button follows the heading's alignment");
    }

    @Test
    void theRowKeepsItsSelectionCellItsKeyAndItsLabelledCheckbox() throws IOException {
        String m = markup("table-row");
        assertTrue(m.contains("<tr data-table-row data-row-key=\"${rowKeyAttr}\" class=\"ash-table__row\">"), m);
        assertTrue(m.contains("<td class=\"ash-table__select\"> @if (hasKey) <label class=\"ash-table__check\"> <span class=\"ash-table__label ash-sr-only\">${checkboxLabel}</span>"), m);
        assertTrue(m.contains("<input type=\"checkbox\" data-table-select-row class=\"ash-table__checkbox\">"));
        String t = template("table-row");
        assertTrue(t.contains("!{boolean selectable = TableContext.isSelectable();}") && t.contains("rowLabel.isEmpty() ? \"Select row\" : \"Select row \" + rowLabel"));
    }

    @Test
    void theCellKeepsItsRowHeaderItsBlankMarkAndItsSortValue() throws IOException {
        String m = markup("table-cell");
        assertTrue(m.contains("<th scope=\"row\" data-table-cell data-sort-value=\"${sortAttr}\" class=\"${cellClasses} ash-table__cell--header\">"), m);
        assertTrue(m.contains("<td data-table-cell data-sort-value=\"${sortAttr}\" class=\"${cellClasses}\">"), m);
        assertEquals(2, count(m, "<span aria-hidden=\"true\" class=\"ash-table__blank\">&mdash;</span><span class=\"ash-table__label ash-sr-only\">No value</span>"), "both cells keep the dash and the name");
        String t = template("table-cell");
        assertTrue(t.contains("!{String cellClasses = \"ash-table__cell\" + alignClass + monoClass + wrapClass;}"));
        assertTrue(t.contains("mono ? \" ash-table__cell--mono\" : \"\"") && t.contains("nowrap ? \" ash-table__cell--nowrap\" : \"\""));
    }

    @Test
    void dataTableHasNoClassOfItsOwnAndRendersThroughTheTableFamily() throws IOException {
        String source = template("data-table").replaceAll("(?s)<%--.*?--%>", "");
        assertFalse(source.contains("class="), "Data Table styles nothing itself");
        Matcher m = Pattern.compile("@template\\.ashlar\\.components\\.([a-z-]+)\\(").matcher(source);
        List<String> used = new ArrayList<>();
        while (m.find()) {
            used.add(m.group(1));
        }
        assertEquals(List.of("table", "table-column", "table-row", "table-cell", "badge", "table-cell"), used, "it composes the family and the Badge, and nothing else");
    }

    @Test
    void noTailwindUtilityRemainsInAnyTemplate() throws IOException {
        List<String> utilities = List.of("flex", "grid", "gap-", "border", "px-", "py-", "pl-", "pr-", "pt-", "pb-", "text-", "font-", "bg-", "dark:", "hover:", "focus:", "ring", "rounded", "overflow",
                "truncate", "h-4", "w-4", "w-10", "w-full", "slate-", "blue-", "divide-", "sr-only", "whitespace-", "break-words", "caption-top", "absolute", "relative", "sm:", "accent-", "cursor-",
                "items-", "justify-", "inset-", "pointer-events", "disabled:", "placeholder:");
        for (String name : List.of("table", "table-column", "table-row", "table-cell", "data-table")) {
            String source = template(name).replaceAll("(?s)<%--.*?--%>", "").replaceAll("(?m)^@(?:param|import) .*$", "").replace("ash-sr-only", "");
            for (String utility : utilities) {
                assertFalse(Pattern.compile("(?<![\\w-])" + Pattern.quote(utility)).matcher(source).find(), name + ": a Tailwind utility is back: " + utility);
            }
        }
    }

    @Test
    void noTemplateWritesAnInlineStyleOrAStateClass() throws IOException {
        for (String name : List.of("table", "table-column", "table-row", "table-cell", "data-table")) {
            String source = template(name).replaceAll("(?s)<%--.*?--%>", "");
            assertFalse(source.contains("style="), name + ": no inline style");
            assertFalse(Pattern.compile("class=\"[^\"]*(?<![\\w-])hidden(?![\\w-])").matcher(source).find(), name + ": hiding is the hidden attribute, never a class");
        }
        assertEquals(4, count(markup("table"), " hidden"), "the selection bar and the filtered state render hidden, and the scroller and the 'none' state render it conditionally");
    }

    // ---- the script ----

    @Test
    void theScriptWritesStateAndNothingPresentational() throws IOException {
        String js = script().replaceAll("(?m)^\\s*//.*$", "");
        assertFalse(js.contains("classList") || js.contains("className"), "no class is added, removed or replaced");
        assertFalse(js.contains(".style") || js.contains("setProperty"), "no inline style is written");
        for (String legacy : List.of("bg-blue-50/60", "dark:bg-blue-950/40", "text-blue-600", "dark:text-blue-400", "text-slate-400", "dark:text-slate-500", "\"hidden\"", "\"flex\"")) {
            assertFalse(js.contains(legacy), "a Tailwind class string is back in the script: " + legacy);
        }
        for (String statement : List.of("row.toggleAttribute(\"data-selected\", isSelected);", "selectionBar.hidden = !hasSelection;", "emptyFiltered.hidden = !filteredToNothing;",
                "scroll.hidden = filteredToNothing;", "emptyNone.hidden = true;", "heading.setAttribute(\"aria-sort\", active ? sortDirection : \"none\");", "row.hidden = true;", "row.hidden = false;")) {
            assertTrue(js.contains(statement), "missing: " + statement);
        }
        assertEquals(1, count(js, "icon.innerHTML ="), "the sort glyph is still content the script sets");
    }

    @Test
    void theScriptKeepsItsHooksItsInstanceScopingAndItsWording() throws IOException {
        String js = script().replaceAll("(?m)^\\s*//.*$", "");
        for (String hook : List.of("[data-table-body]", "[data-table-column]", "[data-table-sort]", "[data-table-search]", "[data-table-status]", "[data-table-page-indicator]", "[data-table-prev]",
                "[data-table-next]", "[data-table-scroll]", "[data-table-empty=\"none\"]", "[data-table-empty=\"filtered\"]", "[data-table-select-all]", "[data-table-selection-bar]",
                "[data-table-selection-count]", "[data-table-clear-selection]", "[data-table-select-row]", "[data-table-row]", "data-table-page-size", "data-row-key", "data-sort-value",
                "data-table-type", "[data-table-sort-icon]")) {
            assertTrue(js.contains(hook), "still reads " + hook);
        }
        assertTrue(js.contains("document.addEventListener(\"DOMContentLoaded\"") && js.contains("document.querySelectorAll(\"[data-table]\").forEach(initTable)"));
        assertTrue(js.contains("root.querySelector(") && js.contains("root.querySelectorAll("), "queries stay inside the instance's own root");
        for (String wording : List.of("`${selected.size} selected`", "`Showing ${start + 1}", "${Math.min(end, visible.length)} of ${visible.length} records`", "`Showing ${visible.length} of ${rows.length} records`",
                "`No records match ", "${search ? search.value.trim() : \"\"}", "`Page ${page} of ${totalPages}`")) {
            assertTrue(js.contains(wording), "the status wording is unchanged: " + wording);
        }
        assertTrue(js.contains("localeCompare(right, undefined, { numeric: true, sensitivity: \"base\" })"), "text still collates");
    }
}
