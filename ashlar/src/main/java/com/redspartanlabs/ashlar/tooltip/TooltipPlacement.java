package com.redspartanlabs.ashlar.tooltip;

/**
 * Which side of the trigger the tooltip bubble appears on. A small, closed
 * vocabulary - not a coordinate system or an auto-flipping "best fit"
 * calculation - because that's the smallest API that actually satisfies the
 * one concrete need (a trigger sitting where the default TOP placement
 * would clip against a container edge), matching every other enum-driven
 * variant param already in this library (see {@code BadgeVariant},
 * {@code DividerOrientation}).
 */
public enum TooltipPlacement {
    TOP, RIGHT, BOTTOM, LEFT;

    public static TooltipPlacement from(String value) {
        if (value == null) {
            return TOP;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TOP;
        }
    }
}
