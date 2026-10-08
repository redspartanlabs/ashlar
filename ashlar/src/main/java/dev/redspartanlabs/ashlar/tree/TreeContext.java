package dev.redspartanlabs.ashlar.tree;

/**
 * Tracks how deep the tree-item currently being rendered sits, purely so
 * each item can compute its own {@code aria-level} without the caller
 * manually passing a level number for every node.
 *
 * <p>Why this exists at all: {@code tree-item.jte}'s {@code children} is an
 * opaque {@code gg.jte.Content} composed of further independent
 * {@code tree-item.jte} calls - by the time one of those calls runs, it has
 * no way to know how many ancestors it's nested under (the same visibility
 * problem {@code AccordionExpansion}'s own doc comment describes). Depth,
 * unlike Accordion's "which one wins" question, is a plain running count -
 * increment when entering a level, decrement when leaving it - which is
 * exactly what a small stack-depth counter is for.
 *
 * <p>Scoped with a {@link ThreadLocal}, not passed as a parameter, for the
 * same reason as {@code AccordionExpansion}: JTE renders one page
 * synchronously on a single thread, so a depth counter that lives for the
 * duration of one {@code tree.jte} call is exactly what a thread-local
 * gives you for free. {@code tree.jte} calls {@link #reset()} immediately
 * before dereferencing its own {@code items}, so every Tree View instance -
 * including several on one showcase page, and requests that reuse a worker
 * thread - starts counting from a clean depth of zero regardless of what an
 * earlier tree left behind.
 *
 * <p>Deliberately does not track {@code aria-posinset}/{@code aria-setsize}:
 * this component always renders the complete hierarchy server-side (no
 * virtualization, no lazy-loaded branches), and the WAI-ARIA specification
 * says explicitly that when the full set of siblings is present in the DOM,
 * assistive technology computes position-in-set and set-size itself by
 * counting - authors do not need to supply either attribute. Adding them
 * here would mean maintaining a second, harder bookkeeping problem (an
 * item's own siblings aren't known until all of them have been composed by
 * the caller) purely to duplicate what already happens for free.
 */
public final class TreeContext {

    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private TreeContext() {
    }

    public static void reset() {
        DEPTH.set(0);
    }

    /** The 1-indexed {@code aria-level} of the item being rendered right now. */
    public static int currentLevel() {
        return DEPTH.get() + 1;
    }

    /** Call immediately before dereferencing a {@code children} block - every
     *  item composed inside it is one level deeper than this one. */
    public static void enterChildren() {
        DEPTH.set(DEPTH.get() + 1);
    }

    /** Call immediately after a {@code children} block has been rendered, to
     *  restore the depth for this item's own remaining siblings. */
    public static void exitChildren() {
        DEPTH.set(DEPTH.get() - 1);
    }
}
