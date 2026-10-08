package dev.redspartanlabs.ashlar.accordion;

/**
 * Resolves however many {@code accordion-item.jte} calls asked for
 * {@code expanded = true} down to at most one, in document order, entirely
 * at render time - so the very first HTML the browser sees already matches
 * the single-open contract, with no JavaScript "repair" pass needed.
 *
 * <p>Why this exists at all: {@code accordion.jte}'s {@code items} is an
 * opaque {@code gg.jte.Content} - by the time it's composed of several
 * independent {@code accordion-item.jte} calls, none of those calls has any
 * visibility into its siblings (the same reason {@code Tab.java} can't carry
 * panel content - see its own doc comment). Cross-item coordination isn't
 * expressible through parameters here, so it happens through this tiny
 * shared, request-scoped flag instead.
 *
 * <p>Scoped with a {@link ThreadLocal}, not a per-accordion-id map: JTE
 * renders one page - and so one accordion instance's items - synchronously
 * on a single thread, and a {@code Content} block's body only actually runs
 * when the callee dereferences it (here, {@code accordion.jte}'s own
 * {@code ${items}}), not eagerly at the call site. {@code accordion.jte}
 * calls {@link #reset()} immediately before that dereference, so every
 * accordion instance - including the several on one showcase page - starts
 * its own items with a clean slate regardless of what any earlier instance,
 * or an earlier request on a reused worker thread, left behind.
 */
public final class AccordionExpansion {

    private static final ThreadLocal<Boolean> CLAIMED = ThreadLocal.withInitial(() -> false);

    private AccordionExpansion() {
    }

    public static void reset() {
        CLAIMED.set(false);
    }

    /** Call once per accordion-item render. Returns whether this item should
     *  actually render expanded, given what it asked for and whether an
     *  earlier sibling in the same accordion already claimed the one open
     *  slot. */
    public static boolean resolve(boolean requestedExpanded) {
        if (!requestedExpanded || CLAIMED.get()) {
            return false;
        }
        CLAIMED.set(true);
        return true;
    }
}
