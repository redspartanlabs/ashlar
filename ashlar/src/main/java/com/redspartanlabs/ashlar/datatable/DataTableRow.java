package com.redspartanlabs.ashlar.datatable;

import java.util.Map;

/**
 * One row: the values a person reads, and optionally the values a machine
 * sorts by.
 *
 * <pre>
 * display: "September 14, 2026"   sort: "2026-09-14"
 * display: "1,250"                sort: "1250"
 * </pre>
 *
 * <p>This split is what keeps date and number handling correct without any
 * locale logic in the component. The previous version stored only display
 * strings and tried to recover meaning from them at render time - parsing
 * {@code "2024-01-12"} as UTC midnight and then printing it in the
 * viewer's timezone, which showed "Jan 11, 2024" to anyone west of
 * Greenwich. The application already knows both forms of its own data; it
 * supplies them, and the component never converts anything.
 *
 * <p>Sort values are optional. A column whose display text already sorts
 * correctly - most text columns - needs none, and
 * {@link #DataTableRow(Map)} exists for that case.
 *
 * <p>Values are display strings rather than {@code Object}: the moment a
 * cell holds a {@code LocalDate} or a {@code BigDecimal}, something has to
 * format it, and that something would be this component.
 */
public record DataTableRow(Map<String, String> values, Map<String, String> sortValues) {

    public DataTableRow {
        values = values == null ? Map.of() : Map.copyOf(values);
        sortValues = sortValues == null ? Map.of() : Map.copyOf(sortValues);
    }

    public DataTableRow(Map<String, String> values) {
        this(values, Map.of());
    }

    /** The display value for a column, or an empty string when absent. */
    public String value(String key) {
        return values.getOrDefault(key, "");
    }

    /**
     * The comparison value for a column: the explicit sort value when the
     * caller supplied one, otherwise empty so the cell falls back to its own
     * displayed text.
     */
    public String sortValue(String key) {
        return sortValues.getOrDefault(key, "");
    }
}
