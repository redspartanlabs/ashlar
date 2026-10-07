package com.redspartanlabs.ashlar.showcase.catalog;

import java.util.Set;

/**
 * One entry in a component page's own in-page outline: the id of the
 * &lt;section&gt; it jumps to, and the visible link text.
 *
 * <p>Showcase-side only, same as {@link ComponentPage}/{@link ApiParam}:
 * the outline exists because the documentation grammar is adaptive - a
 * simple component might render four sections, a composite one ten - so
 * each page builds its own list of {@link PageAnchor}s from whichever
 * sections it actually composed, rather than a fixed menu every page
 * pretends to have.
 */
public record PageAnchor(String id, String label) {

    private static final Set<String> START_IDS = Set.of("quick-start", "how-to-use");
    private static final Set<String> EXPLORE_IDS = Set.of("variants", "loading", "composition", "common-patterns");

    /**
     * Classifies a section id into one of three navigation groups, by the
     * reader's likely intent rather than document order: Start (orient
     * yourself), Explore (see what it can do), or Reference (look up one
     * fact you already know you need). Every section id not in the first
     * two sets - best-practices, pitfalls, accessibility, api-reference,
     * related, and any future lookup-shaped section - falls through to
     * Reference, so a new section kind never needs this method edited to
     * render somewhere reasonable.
     */
    public static String groupFor(String id) {
        if (START_IDS.contains(id)) {
            return "Start";
        }
        if (EXPLORE_IDS.contains(id)) {
            return "Explore";
        }
        return "Reference";
    }
}
