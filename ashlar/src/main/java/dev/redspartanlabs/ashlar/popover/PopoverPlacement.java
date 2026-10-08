package dev.redspartanlabs.ashlar.popover;

/**
 * Which side of the trigger the popover surface opens on. The same closed
 * four-value vocabulary as {@code TooltipPlacement}, but defaulting to
 * BOTTOM rather than TOP: a popover is opened deliberately by clicking a
 * control and is large enough to matter, so it belongs below the control it
 * came from, where it covers whatever follows rather than whatever the user
 * just read above it.
 */
public enum PopoverPlacement {
    TOP, RIGHT, BOTTOM, LEFT;

    public static PopoverPlacement from(String value) {
        if (value == null) {
            return BOTTOM;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return BOTTOM;
        }
    }
}
