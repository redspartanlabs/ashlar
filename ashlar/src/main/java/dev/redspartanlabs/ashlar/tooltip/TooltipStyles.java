package dev.redspartanlabs.ashlar.tooltip;

/**
 * Maps a {@link TooltipPlacement} to the Tailwind classes that position the
 * bubble and its small pointer arrow relative to the trigger. The single
 * place that knows the geometry, the same division of labor as
 * {@code BadgeStyles}/{@code StepperStyles}.
 */
public final class TooltipStyles {

    private TooltipStyles() {
    }

    /** Positions the bubble on the given side of the trigger, centered on
     *  the cross axis. Horizontal (TOP/BOTTOM) placements are nudged at
     *  runtime by tooltip.js if they'd clip the viewport; this only sets
     *  the resting position. */
    public static String position(TooltipPlacement placement) {
        return switch (placement) {
            case TOP -> "bottom-full left-1/2 mb-2 -translate-x-1/2";
            case BOTTOM -> "top-full left-1/2 mt-2 -translate-x-1/2";
            case LEFT -> "right-full top-1/2 mr-2 -translate-y-1/2";
            case RIGHT -> "left-full top-1/2 ml-2 -translate-y-1/2";
        };
    }

    public static String arrow(TooltipPlacement placement) {
        return switch (placement) {
            case TOP -> "-bottom-1 left-1/2 -translate-x-1/2";
            case BOTTOM -> "-top-1 left-1/2 -translate-x-1/2";
            case LEFT -> "-right-1 top-1/2 -translate-y-1/2";
            case RIGHT -> "-left-1 top-1/2 -translate-y-1/2";
        };
    }
}
