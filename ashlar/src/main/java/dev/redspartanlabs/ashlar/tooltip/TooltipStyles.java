package dev.redspartanlabs.ashlar.tooltip;

/**
 * Maps a {@link TooltipPlacement} to the Ashlar classes that position the
 * bubble and its small pointer arrow relative to the trigger. The single
 * place that knows which placement modifier goes with which side, the same
 * division of labor as {@code BadgeStyles}/{@code StepperStyles}; the
 * geometry itself is in tooltip.css.
 */
public final class TooltipStyles {

    private TooltipStyles() {
    }

    /** Positions the bubble on the given side of the trigger, centered on
     *  the cross axis ({@code ash-tooltip__bubble--top|right|bottom|left}).
     *  Horizontal (TOP/BOTTOM) placements are nudged at runtime by
     *  tooltip.js if they'd clip the viewport; this only sets the resting
     *  position. */
    public static String position(TooltipPlacement placement) {
        return switch (placement) {
            case TOP -> "ash-tooltip__bubble--top";
            case BOTTOM -> "ash-tooltip__bubble--bottom";
            case LEFT -> "ash-tooltip__bubble--left";
            case RIGHT -> "ash-tooltip__bubble--right";
        };
    }

    /** Puts the arrow on the bubble's edge that faces the trigger
     *  ({@code ash-tooltip__arrow--top|right|bottom|left}). */
    public static String arrow(TooltipPlacement placement) {
        return switch (placement) {
            case TOP -> "ash-tooltip__arrow--top";
            case BOTTOM -> "ash-tooltip__arrow--bottom";
            case LEFT -> "ash-tooltip__arrow--left";
            case RIGHT -> "ash-tooltip__arrow--right";
        };
    }
}
