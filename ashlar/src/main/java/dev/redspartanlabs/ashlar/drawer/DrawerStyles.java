package dev.redspartanlabs.ashlar.drawer;

/**
 * Maps a {@link DrawerPlacement} to the classes that decide which edge the
 * panel sits against, which way it slides when closed, and which side gets
 * the dividing border - the single place that knows the geometry, the same
 * division of labor as {@code PopoverStyles}/{@code BadgeStyles}.
 *
 * <p>The classes are Ashlar's own, styled by {@code components/drawer.css} in
 * the generated {@code ashlar.css}; they no longer depend on Tailwind. The
 * method names are kept from when they returned Tailwind utilities. The root
 * carries one placement modifier ({@code ash-drawer--left} or
 * {@code --right}) and the panel one more ({@code ash-drawer__panel--left} or
 * {@code --right}), which now carries the inward border and the closed
 * position together: {@link #border} and {@link #closedTransform} therefore
 * return the same class. drawer.js no longer needs the closed position handed
 * to it, so the template no longer writes it into a data attribute.
 */
public final class DrawerStyles {

    private DrawerStyles() {
    }

    /** How the overlay lines its panel up: a drawer fills the height and
     *  hugs one edge, so the only thing to decide is which end of the main
     *  axis the panel is pushed to. The root's placement modifier. */
    public static String alignment(DrawerPlacement placement) {
        return placement == DrawerPlacement.LEFT ? "ash-drawer--left" : "ash-drawer--right";
    }

    /** Where the panel rests while closed - off the edge it belongs to, so
     *  it slides in along the shortest path. The panel's placement modifier,
     *  which drawer.css combines with the root's data-state="closed"; the
     *  same class as {@link #border}. */
    public static String closedTransform(DrawerPlacement placement) {
        return panel(placement);
    }

    /** Only the inward-facing edge gets a border; the other three sit
     *  against the viewport where a border would just be a seam. The panel's
     *  placement modifier; the same class as {@link #closedTransform}. */
    public static String border(DrawerPlacement placement) {
        return panel(placement);
    }

    private static String panel(DrawerPlacement placement) {
        return placement == DrawerPlacement.LEFT ? "ash-drawer__panel--left" : "ash-drawer__panel--right";
    }
}
