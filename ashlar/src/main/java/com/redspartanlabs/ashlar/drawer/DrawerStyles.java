package com.redspartanlabs.ashlar.drawer;

/**
 * Maps a {@link DrawerPlacement} to the Tailwind classes that decide which
 * edge the panel sits against, which way it slides when closed, and which
 * side gets the dividing border - the single place that knows the geometry,
 * the same division of labor as {@code PopoverStyles}/{@code BadgeStyles}.
 */
public final class DrawerStyles {

    private DrawerStyles() {
    }

    /** How the overlay lines its panel up: a drawer fills the height and
     *  hugs one edge, so the only thing to decide is which end of the main
     *  axis the panel is pushed to. */
    public static String alignment(DrawerPlacement placement) {
        return placement == DrawerPlacement.LEFT ? "justify-start" : "justify-end";
    }

    /** Where the panel rests while closed - off the edge it belongs to, so
     *  it slides in along the shortest path. drawer.js toggles this against
     *  {@code translate-x-0} rather than recomputing anything, and reads
     *  this exact string back out of a data attribute so the two never
     *  disagree about which direction "closed" is. */
    public static String closedTransform(DrawerPlacement placement) {
        return placement == DrawerPlacement.LEFT ? "-translate-x-full" : "translate-x-full";
    }

    /** Only the inward-facing edge gets a border; the other three sit
     *  against the viewport where a border would just be a seam. */
    public static String border(DrawerPlacement placement) {
        return placement == DrawerPlacement.LEFT ? "border-r" : "border-l";
    }
}
