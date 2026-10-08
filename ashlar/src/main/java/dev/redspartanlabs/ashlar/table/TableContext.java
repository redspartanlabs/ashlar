package dev.redspartanlabs.ashlar.table;

/**
 * Tells the rows composed inside a table whether that table is rendering a
 * selection column, so a row can emit its own leading checkbox cell without
 * the caller repeating a {@code selectable} flag on every row.
 *
 * <p>Same ThreadLocal hand-off {@code TreeContext} and {@code FacetContext}
 * already use: JTE renders a {@code gg.jte.Content} block at the point the
 * parent dereferences it, on the parent's thread, so a value set immediately
 * before that dereference is visible to every template rendered within it.
 *
 * <p>This exists to keep the header and the body honest about each other. A
 * selection column adds a {@code <th>} to the header row, and if a row were
 * free to decide independently whether to add its matching {@code <td>},
 * one row without a key would silently shift every cell in that row one
 * column to the left. Asking the table once, here, is what makes that
 * impossible.
 */
public final class TableContext {

    private static final ThreadLocal<Boolean> SELECTABLE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private TableContext() {
    }

    /** Called by table.jte immediately before it renders its rows. */
    public static void enterSelectable() {
        SELECTABLE.set(Boolean.TRUE);
    }

    /** Called by table.jte immediately after its rows have rendered. */
    public static void exit() {
        SELECTABLE.remove();
    }

    public static boolean isSelectable() {
        return SELECTABLE.get();
    }
}
