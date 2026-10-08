package dev.redspartanlabs.ashlar.datatable;

import java.util.Map;

/**
 * Describes one column of the Data Table.
 *
 * <p>The three enums are deliberately separate concerns that the previous
 * single {@code type} string ran together:
 *
 * <ul>
 *   <li>{@link Align} - where the value sits in its cell.</li>
 *   <li>{@link Sort} - how values are compared when sorting.</li>
 *   <li>{@link Presentation} - how the value is drawn.</li>
 * </ul>
 *
 * <p>Keeping them apart is what lets a numeric column be right-aligned and
 * compared numerically while still being drawn as plain text, and a status
 * column be drawn as a Badge while still sorting alphabetically. The old
 * {@code "status"} type could not express either, because it meant all
 * three things at once.
 *
 * <p>{@code badgeVariants} maps a domain value to one of the Badge
 * component's variants - {@code {"Active": "Success"}}. The mapping is
 * supplied by the application because the meaning of "Active" is the
 * application's knowledge, not the table's. A value with no mapping falls
 * back to the Badge's own neutral variant.
 *
 * <p>Use the factories rather than the canonical constructor: {@code of},
 * {@code number}, {@code date} and {@code badge} cover every column in
 * practice and keep call sites to one line.
 */
public record DataTableColumn(
        String key,
        String label,
        boolean sortable,
        Align align,
        Sort sort,
        Presentation presentation,
        Map<String, String> badgeVariants
) {

    public DataTableColumn {
        badgeVariants = badgeVariants == null ? Map.of() : Map.copyOf(badgeVariants);
    }

    /** A sortable text column, left aligned - the common case. */
    public static DataTableColumn of(String key, String label) {
        return new DataTableColumn(key, label, true, Align.START, Sort.TEXT, Presentation.TEXT, Map.of());
    }

    /** A text column that is not sortable. */
    public static DataTableColumn text(String key, String label, boolean sortable) {
        return new DataTableColumn(key, label, sortable, Align.START, Sort.TEXT, Presentation.TEXT, Map.of());
    }

    /** An identifier or code: monospaced, compared as text. */
    public static DataTableColumn mono(String key, String label) {
        return new DataTableColumn(key, label, true, Align.START, Sort.TEXT, Presentation.MONO, Map.of());
    }

    /** A number: right aligned so digits line up, compared numerically. */
    public static DataTableColumn number(String key, String label) {
        return new DataTableColumn(key, label, true, Align.END, Sort.NUMBER, Presentation.TEXT, Map.of());
    }

    /** A date: left aligned as text reads, compared chronologically. */
    public static DataTableColumn date(String key, String label) {
        return new DataTableColumn(key, label, true, Align.START, Sort.DATE, Presentation.TEXT, Map.of());
    }

    /** A status drawn as a Badge, using the caller's value-to-variant map. */
    public static DataTableColumn badge(String key, String label, Map<String, String> badgeVariants) {
        return new DataTableColumn(key, label, true, Align.START, Sort.TEXT, Presentation.BADGE, badgeVariants);
    }

    /** The Badge variant for a value, or Neutral when the caller mapped none. */
    public String badgeVariantFor(String value) {
        return badgeVariants.getOrDefault(value, "Neutral");
    }

    public boolean isBadge() {
        return presentation == Presentation.BADGE;
    }

    public boolean isMono() {
        return presentation == Presentation.MONO;
    }
}
