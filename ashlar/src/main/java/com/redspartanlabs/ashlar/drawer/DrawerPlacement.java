package com.redspartanlabs.ashlar.drawer;

/**
 * Which edge of the viewport the drawer is anchored to.
 *
 * <p>Only the two horizontal edges, not the four {@code TooltipPlacement} /
 * {@code PopoverPlacement} offer: those position a small surface *around* a
 * trigger, where this picks the edge a full-height panel slides in from.
 * Top- and bottom-anchored panels are a different component (a sheet), with
 * different sizing, a different gesture model and a different relationship
 * to the keyboard on mobile - folding them in here would make one component
 * answer to two contracts.
 */
public enum DrawerPlacement {
    LEFT, RIGHT;

    public static DrawerPlacement from(String value) {
        if (value == null) {
            return RIGHT;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return RIGHT;
        }
    }
}
