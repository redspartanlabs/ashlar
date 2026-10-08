package dev.redspartanlabs.ashlar.datatable;

/**
 * How a column's values are aligned in their cells.
 *
 * <p>Logical rather than physical names: {@code START}/{@code END} say what
 * the alignment means (the reading edge, the trailing edge) rather than
 * which side of the screen it lands on.
 */
public enum Align {
    START,
    END,
    CENTER;

    /** The value table-column.jte and table-cell.jte expect. */
    public String token() {
        return switch (this) {
            case START -> "left";
            case END -> "right";
            case CENTER -> "center";
        };
    }
}
