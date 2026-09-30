package com.redspartanlabs.ashlar.datatable;

/**
 * How a column's values are drawn.
 *
 * <p>{@code MONO} suits identifiers and codes, which scan better in a
 * monospace face. {@code BADGE} renders the library's Badge component, with
 * the value-to-variant mapping supplied by the application - the component
 * itself holds no domain vocabulary.
 */
public enum Presentation {
    TEXT,
    MONO,
    BADGE
}
