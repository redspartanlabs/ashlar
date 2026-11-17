package dev.redspartanlabs.ashlar.popover;

/**
 * Maps a {@link PopoverPlacement} to the Ashlar class that positions the
 * surface relative to the trigger - the single place that knows which
 * placement modifier goes with which side, the same division of labor as
 * {@code TooltipStyles}/{@code StepperStyles}; the geometry itself is in
 * popover.css.
 *
 * <p>Deliberately a separate class from {@code TooltipStyles} rather than a
 * shared "floating element" helper: the two components only look similar
 * today. Tooltip is a small hover-revealed label, Popover a clicked surface
 * with its own padding and width, and folding both into one utility would
 * mean every future spacing change to either had to be justified against the
 * other.
 */
public final class PopoverStyles {

    private PopoverStyles() {
    }

    /** Positions the surface on the given side of the trigger, centered on
     *  the cross axis ({@code ash-popover__surface--top|right|bottom|left}).
     *  popover.js nudges it along that same cross axis at
     *  open time if it would otherwise run off the viewport; this only sets
     *  the resting position.
     *
     *  <p>No arrow counterpart to this method, unlike {@code TooltipStyles}:
     *  a Popover has no arrow. Dropdown Menu - the library's other clicked,
     *  anchored surface - doesn't have one either, and the 8px offset plus
     *  the surface's own shadow already read as "this belongs to that
     *  control". An arrow on a ringed surface would also have to hide its
     *  own two inner ring edges behind the surface, which a child element
     *  can't paint behind; every way out of that is more machinery than the
     *  association is worth. */
    public static String position(PopoverPlacement placement) {
        return switch (placement) {
            case TOP -> "ash-popover__surface--top";
            case BOTTOM -> "ash-popover__surface--bottom";
            case LEFT -> "ash-popover__surface--left";
            case RIGHT -> "ash-popover__surface--right";
        };
    }
}
