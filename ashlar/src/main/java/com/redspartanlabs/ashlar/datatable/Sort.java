package com.redspartanlabs.ashlar.datatable;

/**
 * How a column's values are compared when the column is sorted.
 *
 * <p>Deliberately separate from {@link Presentation}: how a value is
 * displayed and how it is ordered are different questions, and conflating
 * them is what produced the previous component's bugs - a column marked
 * "date" was formatted by the browser but still compared as a string.
 *
 * <p>Comparison uses the row's sort value when one is supplied (see
 * {@link DataTableRow}), so a human-readable "September 14, 2026" orders by
 * the ISO date behind it rather than alphabetically.
 */
public enum Sort {
    TEXT,
    NUMBER,
    DATE;

    /** The value table-column.jte passes to the shared table script. */
    public String token() {
        return name().toLowerCase();
    }
}
