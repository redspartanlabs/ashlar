package dev.redspartanlabs.ashlar.showcase.cascade;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The pre-migration Tailwind class strings of the Table family (Table, Table Column, Table Row, Table Cell; Data Table renders through them), read from
 * {@code /parity/table/table-legacy.html} (the one source of truth for what "legacy" means), and the rewrite that turns migrated markup back into the markup the templates rendered
 * before: each part gets its legacy classes, and the four places where the old markup said "hidden" with a class (the selection bar, the scroller and the two empty states) lose the
 * {@code hidden} attribute the migrated markup uses. The runtime states the script used to paint with classes are rewritten too, because a parity fixture freezes them in the markup:
 * a heading with {@code aria-sort="ascending"} or {@code "descending"} gets the glyph colour {@code table.js} used to toggle on, a row with {@code data-selected} gets the two fill
 * classes it used to toggle on, and a selection bar that is shown gets {@code flex} instead of {@code hidden}.
 *
 * <p>The Icons inside the search field and the pager are not rewritten here: they are {@code ash-icon ash-icon--sm}, and {@link IconLegacyMarkup} turns that into the
 * {@code inline-block shrink-0 h-4 w-4} the old template rendered (a test proves it). A caller's own {@code width} class on a column is carried through exactly as the template
 * carried it. Everything else on the page is left exactly as served, and an unknown Table class fails loudly rather than leaving some of it migrated.
 *
 * <p>Apply it BEFORE {@link IconLegacyMarkup}.
 */
final class TableLegacyMarkup {

    static final List<String> ROLES = List.of("root", "toolbar", "selection", "clear", "search", "label", "search-icon", "search-input", "loading", "scroll", "scroll-hidden", "table",
            "caption-visible", "caption-hidden", "head", "select", "check", "checkbox", "body", "empty", "empty-hidden", "footer", "status", "pager", "page-button", "page-indicator",
            "column-start", "column-end", "column-center", "sort-start", "sort-center", "sort-end", "sort-icon", "sort-icon-active", "row", "row-selected", "cell", "cell-left", "cell-right",
            "cell-center", "cell-mono", "cell-nowrap", "cell-wrap", "cell-tail-header", "cell-tail-data", "blank");

    /** The migrated class value (or primary class) each role stands for, exactly as the templates write it. */
    static final Map<String, String> MIGRATED = Map.ofEntries(
            Map.entry("root", "ash-table"), Map.entry("toolbar", "ash-table__toolbar"), Map.entry("selection", "ash-table__selection"), Map.entry("clear", "ash-table__clear"),
            Map.entry("search", "ash-table__search"), Map.entry("label", "ash-table__label ash-sr-only"), Map.entry("search-icon", "ash-table__search-icon"),
            Map.entry("search-input", "ash-table__search-input"), Map.entry("loading", "ash-table__loading"), Map.entry("scroll", "ash-table__scroll"), Map.entry("table", "ash-table__table"),
            Map.entry("caption-visible", "ash-table__caption"), Map.entry("caption-hidden", "ash-sr-only"), Map.entry("head", "ash-table__head"), Map.entry("select", "ash-table__select"),
            Map.entry("check", "ash-table__check"), Map.entry("checkbox", "ash-table__checkbox"), Map.entry("body", "ash-table__body"), Map.entry("empty", "ash-table__empty"),
            Map.entry("footer", "ash-table__footer"), Map.entry("status", "ash-table__status"), Map.entry("pager", "ash-table__pager"), Map.entry("page-button", "ash-table__page-button"),
            Map.entry("page-indicator", "ash-table__page-indicator"), Map.entry("column-start", "ash-table__column"), Map.entry("column-end", "ash-table__column ash-table__column--end"),
            Map.entry("column-center", "ash-table__column ash-table__column--center"), Map.entry("sort-start", "ash-table__sort"), Map.entry("sort-end", "ash-table__sort ash-table__sort--end"),
            Map.entry("sort-center", "ash-table__sort ash-table__sort--center"), Map.entry("sort-icon", "ash-table__sort-icon"), Map.entry("row", "ash-table__row"),
            Map.entry("cell", "ash-table__cell"), Map.entry("blank", "ash-table__blank"));

    private static final Pattern LEGACY = Pattern.compile("data-legacy-role=\"([a-z-]+)\" class=\"([^\"]+)\"");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("class=\"([^\"]*)\"");
    private static final Pattern TAG = Pattern.compile("<([a-zA-Z0-9]+)\\b[^>]*>");
    private static final Pattern HIDDEN_ATTRIBUTE = Pattern.compile("\\shidden(?=[\\s>/])");
    private static final Pattern ARIA_SORT_ACTIVE = Pattern.compile("\\saria-sort=\"(?:ascending|descending)\"");
    private static final Pattern SELECTED_ATTRIBUTE = Pattern.compile("\\sdata-selected(?=[\\s>/=])");

    private final Map<String, String> legacyByRole = new LinkedHashMap<>();

    TableLegacyMarkup() throws IOException {
        String html;
        try (InputStream in = TableLegacyMarkup.class.getResourceAsStream("/parity/table/table-legacy.html")) {
            if (in == null) {
                throw new IOException("missing /parity/table/table-legacy.html");
            }
            html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = LEGACY.matcher(html);
        while (m.find()) {
            if (legacyByRole.put(m.group(1), m.group(2)) != null) {
                throw new IllegalStateException("table-legacy.html defines " + m.group(1) + " twice");
            }
        }
        if (!legacyByRole.keySet().equals(Set.copyOf(ROLES))) {
            throw new IllegalStateException("table-legacy.html must define exactly " + ROLES + ", found " + legacyByRole.keySet());
        }
    }

    String classes(String role) {
        String found = legacyByRole.get(role);
        if (found == null) {
            throw new IllegalStateException("no legacy classes for " + role);
        }
        return found;
    }

    /** Every legacy class token, for the oracle's non-vacuity check. */
    Set<String> allTokens() {
        return legacyByRole.values().stream().flatMap(s -> Arrays.stream(s.split("\\s+"))).collect(Collectors.toCollection(TreeSet::new));
    }

    /** How many elements of {@code html} carry the primary class {@code token} (as a whole class token). */
    static int count(String html, String token) {
        int n = 0;
        Matcher m = CLASS_ATTRIBUTE.matcher(html);
        while (m.find()) {
            if (Arrays.asList(m.group(1).trim().split("\\s+")).contains(token)) {
                n++;
            }
        }
        return n;
    }

    /** The legacy "shown" selection bar: the string with {@code hidden} removed and {@code flex} added, which is what table.js left it with once something was selected. */
    String selectionShown() {
        List<String> tokens = new ArrayList<>(Arrays.asList(classes("selection").split("\\s+")));
        tokens.remove("hidden");
        tokens.add("flex");
        return String.join(" ", tokens);
    }

    private String column(String align, List<String> width) {
        List<String> tokens = new ArrayList<>(Arrays.asList(classes("column-" + align).split("\\s+")));
        tokens.addAll(3, width); // "px-4 py-2.5 <align>" then the caller's width, then the rest, as the template joined them
        return String.join(" ", tokens);
    }

    private String cell(Set<String> own, boolean header) {
        StringBuilder sb = new StringBuilder(classes("cell")).append(' ');
        sb.append(classes(own.contains("ash-table__cell--end") ? "cell-right" : own.contains("ash-table__cell--center") ? "cell-center" : "cell-left"));
        if (own.contains("ash-table__cell--mono")) {
            sb.append(' ').append(classes("cell-mono"));
        }
        sb.append(' ').append(classes(own.contains("ash-table__cell--nowrap") ? "cell-nowrap" : "cell-wrap"));
        sb.append(' ').append(classes(header ? "cell-tail-header" : "cell-tail-data"));
        return sb.toString();
    }

    private static final Set<String> CELL_PARTS = Set.of("ash-table__cell", "ash-table__cell--end", "ash-table__cell--center", "ash-table__cell--mono", "ash-table__cell--nowrap", "ash-table__cell--header");

    /** Rewrites every migrated Table part in {@code html} to its legacy classes. Everything else is untouched. */
    String rewrite(String html) {
        Matcher tags = TAG.matcher(html);
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        boolean activeSort = false;
        while (tags.find()) {
            String tag = tags.group();
            String name = tags.group(1).toLowerCase();
            Matcher cls = CLASS_ATTRIBUTE.matcher(tag);
            if (!cls.find()) {
                continue;
            }
            List<String> tokens = Arrays.asList(cls.group(1).trim().split("\\s+"));
            boolean captionHidden = name.equals("caption") && tokens.equals(List.of("ash-sr-only"));
            if (tokens.stream().noneMatch(t -> t.startsWith("ash-table")) && !captionHidden) {
                continue;
            }
            Set<String> own = tokens.stream().filter(t -> t.startsWith("ash-table")).collect(Collectors.toCollection(LinkedHashSet::new));
            List<String> others = tokens.stream().filter(t -> !t.startsWith("ash-table")).toList();
            boolean hidden = HIDDEN_ATTRIBUTE.matcher(tag).find();
            boolean dropHidden = false;
            String legacy;
            if (captionHidden) {
                legacy = classes("caption-hidden");
                others = List.of();
            } else if (own.equals(Set.of("ash-table"))) {
                legacy = classes("root");
            } else if (own.equals(Set.of("ash-table__toolbar"))) {
                legacy = classes("toolbar");
            } else if (own.equals(Set.of("ash-table__selection"))) {
                legacy = hidden ? classes("selection") : selectionShown();
                dropHidden = true;
            } else if (own.equals(Set.of("ash-table__clear"))) {
                legacy = classes("clear");
            } else if (own.equals(Set.of("ash-table__search"))) {
                legacy = classes("search");
            } else if (own.equals(Set.of("ash-table__label")) && others.equals(List.of("ash-sr-only"))) {
                legacy = classes("label");
                others = List.of();
            } else if (own.equals(Set.of("ash-table__search-icon"))) {
                legacy = classes("search-icon");
            } else if (own.equals(Set.of("ash-table__search-input"))) {
                legacy = classes("search-input");
            } else if (own.equals(Set.of("ash-table__loading"))) {
                legacy = classes("loading");
            } else if (own.equals(Set.of("ash-table__scroll"))) {
                legacy = classes(hidden ? "scroll-hidden" : "scroll");
                dropHidden = true;
            } else if (own.equals(Set.of("ash-table__table"))) {
                legacy = classes("table");
            } else if (own.equals(Set.of("ash-table__caption"))) {
                legacy = classes("caption-visible");
            } else if (own.equals(Set.of("ash-table__head"))) {
                legacy = classes("head");
            } else if (own.equals(Set.of("ash-table__body"))) {
                legacy = classes("body");
            } else if (own.equals(Set.of("ash-table__empty"))) {
                legacy = classes(hidden ? "empty-hidden" : "empty");
                dropHidden = true;
            } else if (own.equals(Set.of("ash-table__footer"))) {
                legacy = classes("footer");
            } else if (own.equals(Set.of("ash-table__status"))) {
                legacy = classes("status");
            } else if (own.equals(Set.of("ash-table__pager"))) {
                legacy = classes("pager");
            } else if (own.equals(Set.of("ash-table__page-button"))) {
                legacy = classes("page-button");
            } else if (own.equals(Set.of("ash-table__page-indicator"))) {
                legacy = classes("page-indicator");
            } else if (own.equals(Set.of("ash-table__select"))) {
                legacy = classes("select");
            } else if (own.equals(Set.of("ash-table__check"))) {
                legacy = classes("check");
            } else if (own.equals(Set.of("ash-table__checkbox"))) {
                legacy = classes("checkbox");
            } else if (own.equals(Set.of("ash-table__blank"))) {
                legacy = classes("blank");
            } else if (own.equals(Set.of("ash-table__row"))) {
                legacy = SELECTED_ATTRIBUTE.matcher(tag).find() ? classes("row-selected") : classes("row");
            } else if (own.equals(Set.of("ash-table__sort-icon"))) {
                legacy = classes(activeSort ? "sort-icon-active" : "sort-icon");
            } else if (own.equals(Set.of("ash-table__sort")) || own.equals(Set.of("ash-table__sort", "ash-table__sort--end")) || own.equals(Set.of("ash-table__sort", "ash-table__sort--center"))) {
                legacy = classes(own.contains("ash-table__sort--end") ? "sort-end" : own.contains("ash-table__sort--center") ? "sort-center" : "sort-start");
            } else if (own.contains("ash-table__column") && CLASS_OK_COLUMN.containsAll(own)) {
                activeSort = ARIA_SORT_ACTIVE.matcher(tag).find();
                legacy = column(own.contains("ash-table__column--end") ? "end" : own.contains("ash-table__column--center") ? "center" : "start", others);
                others = List.of();
            } else if (own.contains("ash-table__cell") && CELL_PARTS.containsAll(own)) {
                legacy = cell(own, own.contains("ash-table__cell--header"));
            } else {
                throw new IllegalStateException("a class value that carries a Table class but is not a known part: " + cls.group(1));
            }
            if (!others.isEmpty() && !own.contains("ash-table__column")) {
                throw new IllegalStateException("a Table part carries a class that is not its own (only a column takes the caller's width): " + cls.group(1));
            }
            String rewritten = tag.replace("class=\"" + cls.group(1) + "\"", "class=\"" + legacy + "\"");
            if (dropHidden) {
                rewritten = HIDDEN_ATTRIBUTE.matcher(rewritten).replaceFirst("");
            }
            out.append(html, cursor, tags.start()).append(rewritten);
            cursor = tags.end();
        }
        return out.append(html.substring(cursor)).toString();
    }

    private static final Set<String> CLASS_OK_COLUMN = Set.of("ash-table__column", "ash-table__column--end", "ash-table__column--center");
}
