package com.redspartanlabs.ashlar.facet;

/**
 * Carries the enclosing facet's field name down to the options composed
 * inside it, so a caller writes the name once on the facet instead of
 * repeating it on every option.
 *
 * <p>This is the same ThreadLocal hand-off {@code TreeContext} already uses
 * to give a nested tree item its {@code aria-level}, and
 * {@code AccordionExpansion} uses to coordinate a single open panel: JTE
 * renders a {@code gg.jte.Content} block at the point the parent template
 * dereferences it, on the parent's own thread, so a value set immediately
 * before that dereference is visible to every template rendered within it.
 * There is no other way for a parent to pass context into an opaque content
 * block, and inventing a per-option {@code name} parameter instead would put
 * the same string on every line of a filter list and make a mismatch between
 * two options in one facet a silent bug.
 *
 * <p>The name is cleared as soon as the facet's content has rendered, so a
 * {@code facet-option} used outside a facet renders with no name rather than
 * inheriting a stale one from a facet elsewhere on the page.
 */
public final class FacetContext {

    private static final ThreadLocal<String> NAME = new ThreadLocal<>();

    private FacetContext() {
    }

    /** Called by facet.jte immediately before it renders its options. */
    public static void enter(String name) {
        NAME.set(name);
    }

    /** Called by facet.jte immediately after its options have rendered. */
    public static void exit() {
        NAME.remove();
    }

    /**
     * The enclosing facet's name, or an empty string when an option is
     * rendered outside a facet - which produces a control the browser will
     * not submit, rather than one submitted under the wrong dimension.
     */
    public static String currentName() {
        String name = NAME.get();
        return name == null ? "" : name;
    }
}
