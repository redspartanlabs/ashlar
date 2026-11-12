package dev.redspartanlabs.ashlar.showcase.cascade;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What the Table parity suites share: the migrated markup of the Table family exactly as its templates render it (the contract tests pin the templates, and
 * {@link TableRealPagesTest} and {@link ShowcaseRuntimeTest} check the REAL rendered pages against the legacy rewrite), the cases (every state the family has, with the pseudo-class
 * forced on its target where there is one), the in-page measurement, and the scripts. The pages and the comparison are Text Entry's ({@link TextEntryBrowser#page},
 * {@link TextEntryBrowser#differences}), and the legacy rendering is this markup rewritten by {@link TableLegacyMarkup}, then {@link IconLegacyMarkup}.
 *
 * <p>The nested Spinner and Empty State of the loading and empty states are NOT part of these fixtures: their wrappers are, with a plain word inside. Those two components are measured
 * on their own, in a table-shaped wrapper, by {@code SpinnerParityTest} and {@code EmptyStateParityTest}, and in place on the real pages by the same suites and
 * {@link ShowcaseRuntimeTest}. A runtime state (a sorted heading, a selected row, a shown selection bar, a hidden region) is frozen into the markup the way table.js leaves it.
 */
final class TableBrowser {

    private TableBrowser() {
    }

    private static final String SVG = "<svg class=\"ash-icon ash-icon--sm\" viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M9 5l7 7-7 7\"/></svg>";

    static Map<String, Object> o(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    private static boolean b(Map<String, Object> m, String k) {
        return Boolean.TRUE.equals(m.get(k));
    }

    private static int n(Map<String, Object> m, String k, int d) {
        return m.containsKey(k) ? (Integer) m.get(k) : d;
    }

    // ---------------------------------------------------------------- the data

    record Column(String label, boolean sortable, String align, String type, String width) {
    }

    record Cell(String text, String content, String align, boolean header, boolean mono, boolean nowrap, String sortValue) {
        static Cell of(String text) {
            return new Cell(text, null, "left", false, false, false, "");
        }
    }

    private static final List<Column> BASIC = List.of(new Column("Account", true, "left", "text", ""), new Column("Owner", true, "left", "text", ""), new Column("Seats", true, "right", "number", ""));

    private static final String[][] ROWS = {
            {"ACC-1001", "Avery Stone", "1,204"}, {"ACC-1002", "Blake Rivera", "38"}, {"ACC-1003", "Casey Lin", "7"}, {"ACC-1004", "Devon Park", "410"}, {"ACC-1005", "Emery Shaw", "95"},
            {"ACC-1006", "Finley Cruz", "2"}};

    /** Every cell variety on one table: alignment, monospace, no-wrap, a row header, a blank value, rich content, and a column that carries the caller's width class. */
    private static final List<Column> VARIANT_COLUMNS = List.of(new Column("Name", true, "left", "text", ""), new Column("Code", false, "left", "text", ""),
            new Column("Amount", true, "right", "number", ""), new Column("Flag", false, "center", "text", ""), new Column("Note", false, "left", "text", "w-32"));

    // ---------------------------------------------------------------- the migrated markup

    private static String attr(boolean on, String name) {
        return on ? " " + name : "";
    }

    private static String column(Column c, String sort) {
        String align = c.align().equals("right") ? " ash-table__column--end" : c.align().equals("center") ? " ash-table__column--center" : "";
        String justify = c.align().equals("right") ? " ash-table__sort--end" : c.align().equals("center") ? " ash-table__sort--center" : "";
        String ariaSort = c.sortable() ? " aria-sort=\"" + (sort == null ? "none" : sort) + "\"" : "";
        String glyph = sort == null ? "&#8645;" : sort.equals("ascending") ? "&uarr;" : "&darr;";
        return "<th scope=\"col\" data-table-column data-table-type=\"" + c.type() + "\"" + ariaSort + " class=\"ash-table__column" + align + (c.width().isEmpty() ? "" : " " + c.width()) + "\">"
                + (c.sortable() ? "<button type=\"button\" data-table-sort class=\"ash-table__sort" + justify + "\"><span>" + c.label() + "</span><span data-table-sort-icon aria-hidden=\"true\" class=\"ash-table__sort-icon\">"
                        + glyph + "</span></button>" : c.label()) + "</th>";
    }

    private static String cell(Cell c) {
        String align = c.align().equals("right") ? " ash-table__cell--end" : c.align().equals("center") ? " ash-table__cell--center" : "";
        String classes = "ash-table__cell" + align + (c.mono() ? " ash-table__cell--mono" : "") + (c.nowrap() ? " ash-table__cell--nowrap" : "") + (c.header() ? " ash-table__cell--header" : "");
        String inner = c.content() != null ? c.content() : c.text().isBlank()
                ? "<span aria-hidden=\"true\" class=\"ash-table__blank\">&mdash;</span><span class=\"ash-table__label ash-sr-only\">No value</span>" : c.text();
        String tag = c.header() ? "th scope=\"row\"" : "td";
        return "<" + tag + " data-table-cell" + (c.sortValue().isEmpty() ? "" : " data-sort-value=\"" + c.sortValue() + "\"") + " class=\"" + classes + "\">" + inner + "</" + (c.header() ? "th" : "td") + ">";
    }

    private static String check(String attributes, String name) {
        return "<label class=\"ash-table__check\"><span class=\"ash-table__label ash-sr-only\">" + name + "</span><input type=\"checkbox\" " + attributes + " class=\"ash-table__checkbox\"></label>";
    }

    private static String row(String key, boolean selectable, boolean selected, boolean hidden, List<Cell> cells) {
        StringBuilder sb = new StringBuilder("<tr data-table-row data-row-key=\"" + key + "\" class=\"ash-table__row\"" + attr(selected, "data-selected") + attr(hidden, "hidden") + ">");
        if (selectable) {
            sb.append("<td class=\"ash-table__select\">").append(check("data-table-select-row" + attr(selected, "checked"), "Select row " + key)).append("</td>");
        }
        cells.forEach(c -> sb.append(cell(c)));
        return sb.append("</tr>").toString();
    }

    /** A Table, as table.jte renders it (with the state table.js would have left it in, where the options say so). */
    @SuppressWarnings("unchecked")
    static String table(Map<String, Object> m) {
        boolean variants = b(m, "variants");
        boolean searchable = b(m, "searchable");
        boolean selectable = b(m, "selectable");
        boolean sortable = b(m, "sortable") || m.containsKey("sort");
        int pageSize = n(m, "pageSize", 0);
        int page = n(m, "page", 1);
        int rowCount = variants ? 2 : n(m, "rows", 4);
        boolean empty = b(m, "empty");
        boolean filtered = b(m, "filtered");
        boolean loading = b(m, "loading");
        List<Integer> selected = (List<Integer>) m.getOrDefault("selected", List.of());
        String sort = (String) m.get("sort");
        int sortColumn = sort == null ? -1 : Integer.parseInt(sort.split(":")[0]);
        String sortDirection = sort == null ? null : sort.split(":")[1];
        String footer = (String) m.getOrDefault("footer", "");
        List<Column> columns = new ArrayList<>(variants ? VARIANT_COLUMNS : BASIC);
        if (!sortable) {
            columns.replaceAll(c -> new Column(c.label(), false, c.align(), c.type(), c.width()));
        }
        boolean paginated = pageSize > 0;
        StringBuilder sb = new StringBuilder("<div data-table data-table-page-size=\"" + pageSize + "\" class=\"ash-table\">");
        if (searchable || selectable) {
            sb.append("<div class=\"ash-table__toolbar\">");
            if (selectable) {
                sb.append("<div data-table-selection-bar class=\"ash-table__selection\"").append(attr(selected.isEmpty(), "hidden")).append("><span data-table-selection-count>")
                        .append(selected.isEmpty() ? "" : selected.size() + " selected")
                        .append("</span><button type=\"button\" data-table-clear-selection class=\"ash-table__clear\">Clear selection</button></div>");
            }
            if (searchable) {
                sb.append("<label class=\"ash-table__search\"><span class=\"ash-table__label ash-sr-only\">Search</span><span class=\"ash-table__search-icon\" aria-hidden=\"true\">").append(SVG)
                        .append("</span><input type=\"search\" data-table-search placeholder=\"Search\" class=\"ash-table__search-input\"></label>");
            }
            sb.append("</div>");
        }
        if (loading) {
            sb.append("<div data-table-loading class=\"ash-table__loading\"><span>Working</span><span>Loading records&hellip;</span></div>");
        } else {
            sb.append("<div data-table-scroll class=\"ash-table__scroll\"").append(attr(empty || filtered, "hidden")).append("><table class=\"ash-table__table\"><caption class=\"")
                    .append(b(m, "captionVisible") ? "ash-table__caption" : "ash-sr-only").append("\">Accounts</caption><thead class=\"ash-table__head\"><tr>");
            if (selectable) {
                boolean all = selected.size() == rowCount && rowCount > 0;
                boolean some = !selected.isEmpty() && !all;
                sb.append("<th scope=\"col\" class=\"ash-table__select\">").append(check("data-table-select-all" + attr(all, "checked") + attr(some, "data-indeterminate"), "Select all rows on this page")).append("</th>");
            }
            for (int i = 0; i < columns.size(); i++) {
                sb.append(column(columns.get(i), i == sortColumn ? sortDirection : null));
            }
            sb.append("</tr></thead><tbody data-table-body class=\"ash-table__body\">");
            if (variants) {
                sb.append(row("V-1", selectable, selected.contains(0), false, List.of(new Cell("Avery Stone", null, "left", true, false, false, ""), new Cell("AV-0042", null, "left", false, true, true, ""),
                        new Cell("1,204", null, "right", false, false, true, "1204"), new Cell("Yes", null, "center", false, false, false, ""), Cell.of("A fairly long note that wraps inside its narrow column"))));
                sb.append(row("V-2", selectable, selected.contains(1), false, List.of(new Cell("Blake Rivera", null, "left", true, false, false, ""), new Cell("", null, "left", false, false, false, ""),
                        new Cell("", null, "right", false, false, true, ""), new Cell("No", null, "center", false, false, false, ""), new Cell("", "<a href=\"#\" data-parity=\"raw\">A link</a>", "left", false, false, false, ""))));
            } else {
                for (int r = 0; r < rowCount; r++) {
                    String[] d = ROWS[r];
                    boolean hidden = paginated && (r < (page - 1) * pageSize || r >= page * pageSize);
                    sb.append(row(d[0], selectable, selected.contains(r), hidden, List.of(new Cell(d[0], null, "left", true, true, false, ""), Cell.of(d[1]), new Cell(d[2], null, "right", false, false, true, d[2].replace(",", "")))));
                }
            }
            sb.append("</tbody></table></div>");
            sb.append("<div data-table-empty=\"none\" class=\"ash-table__empty\"").append(attr(!empty, "hidden")).append("><span>No records found</span></div>");
            sb.append("<div data-table-empty=\"filtered\" class=\"ash-table__empty\"").append(attr(!filtered, "hidden")).append("><span>No matching records</span></div>");
        }
        if (paginated || !footer.isEmpty() || searchable) {
            int totalPages = Math.max(1, (rowCount + pageSize - 1) / Math.max(1, pageSize));
            sb.append("<div class=\"ash-table__footer\">");
            if (paginated || searchable) {
                sb.append("<p data-table-status aria-live=\"polite\" class=\"ash-table__status\">")
                        .append(filtered ? "No records match &ldquo;zzz&rdquo;" : paginated ? "Showing " + ((page - 1) * pageSize + 1) + "-" + Math.min(page * pageSize, rowCount) + " of " + rowCount + " records" : "Showing " + rowCount + " of " + rowCount + " records")
                        .append("</p>");
            }
            sb.append(footer);
            if (paginated) {
                sb.append("<div data-table-pagination class=\"ash-table__pager\"><button type=\"button\" data-table-prev class=\"ash-table__page-button\"").append(attr(page <= 1, "disabled"))
                        .append("><span aria-hidden=\"true\">").append(SVG).append("</span>Previous</button><span data-table-page-indicator class=\"ash-table__page-indicator\">Page ").append(page).append(" of ")
                        .append(totalPages).append("</span><button type=\"button\" data-table-next class=\"ash-table__page-button\"").append(attr(page >= totalPages, "disabled")).append(">Next<span aria-hidden=\"true\">")
                        .append(SVG).append("</span></button></div>");
            }
            sb.append("</div>");
        }
        return sb.append("</div>").toString();
    }

    private static final Pattern TAG_START = Pattern.compile("<(div|span|p|label|input|button|svg|table|caption|thead|tbody|tr|th|td)(?=[\\s>])");

    /** Marks every element of the component so the scoped legacy stylesheet can address exactly those. */
    static String marked(String html) {
        Matcher m = TAG_START.matcher(html);
        StringBuilder out = new StringBuilder();
        int at = 0;
        while (m.find()) {
            out.append(html, at, m.end());
            int close = html.indexOf('>', m.end());
            if (!html.substring(m.end(), close).contains("data-parity")) {
                out.append(" data-parity");
            }
            at = m.end();
        }
        return out.append(html.substring(at)).toString();
    }

    /** Sets the DOM-only {@code indeterminate} property that table.js sets for a partly selected page, so a parity page needs no script. */
    static final String INDETERMINATE = "<script>document.querySelectorAll('[data-indeterminate]').forEach(function(e){e.indeterminate=true})</script>";

    // ---------------------------------------------------------------- the cases

    /** One case: a key, the options that build it, the pseudo-classes forced on its target while it is measured, and the target (a selector inside the case's table). */
    record Case(String key, Map<String, Object> options, List<String> pseudo, String target) {
    }

    private static Case c(String key, Map<String, Object> options) {
        return new Case(key, options, List.of(), null);
    }

    private static Case c(String key, Map<String, Object> options, List<String> pseudo, String target) {
        return new Case(key, options, pseudo, target);
    }

    private static final List<Integer> TWO = List.of(1, 2);
    private static final List<Integer> ALL = List.of(0, 1, 2, 3);
    private static final List<String> FOCUS = List.of("focus", "focus-visible");

    static final List<Case> CASES = List.of(
            c("plain", o()),
            c("caption visible", o("captionVisible", true)),
            c("sortable, none sorted", o("sortable", true)),
            c("sorted ascending", o("sort", "0:ascending")),
            c("sorted descending, right aligned", o("sort", "2:descending")),
            c("sort button hover", o("sortable", true), List.of("hover"), "[data-table-sort]"),
            c("sort button focus-visible", o("sortable", true), FOCUS, "[data-table-sort]"),
            c("active sort button hover", o("sort", "0:ascending"), List.of("hover"), "[data-table-sort]"),
            c("searchable", o("searchable", true)),
            c("search focus", o("searchable", true), List.of("focus"), "[data-table-search]"),
            c("search hover", o("searchable", true), List.of("hover"), "[data-table-search]"),
            c("selectable, nothing selected", o("selectable", true)),
            c("selectable, two selected", o("selectable", true, "selected", TWO)),
            c("selectable, every row selected", o("selectable", true, "selected", ALL)),
            c("row hover", o(), List.of("hover"), "tbody tr:nth-child(2)"),
            c("selected row hover", o("selectable", true, "selected", TWO), List.of("hover"), "tbody tr:nth-child(2)"),
            c("row checkbox focus-visible", o("selectable", true), FOCUS, "[data-table-select-row]"),
            c("select-all focus-visible", o("selectable", true), FOCUS, "[data-table-select-all]"),
            c("clear selection focus-visible", o("selectable", true, "selected", TWO), FOCUS, "[data-table-clear-selection]"),
            c("clear selection hover", o("selectable", true, "selected", TWO), List.of("hover"), "[data-table-clear-selection]"),
            c("paged, first page", o("pageSize", 2, "rows", 6, "page", 1)),
            c("paged, middle page", o("pageSize", 2, "rows", 6, "page", 2)),
            c("paged, last page", o("pageSize", 2, "rows", 6, "page", 3)),
            c("pager button hover", o("pageSize", 2, "rows", 6, "page", 2), List.of("hover"), "[data-table-next]"),
            c("pager button focus-visible", o("pageSize", 2, "rows", 6, "page", 2), FOCUS, "[data-table-next]"),
            c("pager disabled button hover", o("pageSize", 2, "rows", 6, "page", 1), List.of("hover"), "[data-table-prev]"),
            c("loading", o("loading", true)),
            c("loading, searchable", o("loading", true, "searchable", true)),
            c("empty from the server", o("empty", true)),
            c("filtered to nothing", o("searchable", true, "filtered", true)),
            c("footer slot", o("footer", "<span>Page 2 of 5</span>")),
            c("cell and column variants", o("variants", true)),
            c("cell and column variants, selectable", o("variants", true, "selectable", true, "selected", List.of(0))),
            c("everything together", o("captionVisible", true, "searchable", true, "selectable", true, "selected", List.of(0), "sort", "1:ascending", "pageSize", 2, "rows", 6, "page", 2)));

    static String target(Case c, String id) {
        return "[data-field=\"" + id + "\"] " + c.target();
    }

    // ---------------------------------------------------------------- measurement

    static final List<String> PROPERTIES = new ArrayList<>(TextEntryBrowser.PROPERTIES);

    static {
        PROPERTIES.addAll(List.of("accent-color", "border-collapse", "border-spacing", "caption-side", "overflow-wrap", "font-family", "text-indent", "text-underline-offset", "border-right-style",
                "border-bottom-style", "border-left-style", "outline-color", "outline-offset", "flex-direction", "flex-wrap", "flex-basis", "clip-path", "text-decoration-color"));
    }

    private static String jsArray(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            sb.append(i == 0 ? "" : ",").append('\'').append(values.get(i)).append('\'');
        }
        return sb.append(']').toString();
    }

    /** One line per element and property (tab separated: field, element position and tag, property, value), plus every element's box. Elements inside an svg are not measured. */
    static String measureScript(String fieldId) {
        return measureScript(fieldId, false);
    }

    /** As above; with {@code skipRaw}, the content a caller supplies (marked {@code data-parity="raw"}, unstyled on both sides of a Tailwind-free comparison) is left out. */
    static String measureScript(String fieldId, boolean skipRaw) {
        return "(function(){var props=" + jsArray(PROPERTIES) + ";var out=[];var root=document.querySelector('[data-field=\"" + fieldId + "\"]');"
                + "var els=[root].concat([].slice.call(root.querySelectorAll('*'))).filter(function(e){return !(e.closest('svg')&&e.tagName.toLowerCase()!=='svg')&&!(" + skipRaw
                + "&&e.getAttribute('data-parity')==='raw')});"
                + "els.forEach(function(e,n){var s=getComputedStyle(e);var name='" + fieldId + ":'+n+':'+e.tagName.toLowerCase();"
                + "props.forEach(function(p){out.push([name,p,s.getPropertyValue(p)].join('\\t'))});"
                + "var r=e.getBoundingClientRect();out.push([name,'@x',String(Math.round(r.x*100)/100)].join('\\t'));out.push([name,'@y',String(Math.round(r.y*100)/100)].join('\\t'));"
                + "out.push([name,'@w',String(Math.round(r.width*100)/100)].join('\\t'));out.push([name,'@h',String(Math.round(r.height*100)/100)].join('\\t'));});"
                + "return out.join('\\n')})()";
    }

    // ---------------------------------------------------------------- scripts

    /** A classpath script, as an inline classic script (table.js has no import or export). */
    static String script(Class<?> anchor, String resource) throws IOException {
        try (InputStream in = anchor.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("missing " + resource);
            }
            String js = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (js.contains("</script")) {
                throw new IllegalStateException(resource + " contains a closing script tag");
            }
            return "<script>" + js + "</script>";
        }
    }
}
