package dev.redspartanlabs.ashlar.tooltip;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** TooltipStyles returns the Ashlar placement modifiers, and TooltipPlacement keeps its fallback to TOP. */
class TooltipStylesTest {

    @Test
    void eachPlacementHasItsBubbleAndArrowModifier() {
        assertEquals("ash-tooltip__bubble--top", TooltipStyles.position(TooltipPlacement.TOP));
        assertEquals("ash-tooltip__bubble--right", TooltipStyles.position(TooltipPlacement.RIGHT));
        assertEquals("ash-tooltip__bubble--bottom", TooltipStyles.position(TooltipPlacement.BOTTOM));
        assertEquals("ash-tooltip__bubble--left", TooltipStyles.position(TooltipPlacement.LEFT));
        assertEquals("ash-tooltip__arrow--top", TooltipStyles.arrow(TooltipPlacement.TOP));
        assertEquals("ash-tooltip__arrow--right", TooltipStyles.arrow(TooltipPlacement.RIGHT));
        assertEquals("ash-tooltip__arrow--bottom", TooltipStyles.arrow(TooltipPlacement.BOTTOM));
        assertEquals("ash-tooltip__arrow--left", TooltipStyles.arrow(TooltipPlacement.LEFT));
    }

    @Test
    void anUnrecognizedPlacementFallsBackToTop() {
        assertEquals(TooltipPlacement.TOP, TooltipPlacement.from(null));
        assertEquals(TooltipPlacement.TOP, TooltipPlacement.from(""));
        assertEquals(TooltipPlacement.TOP, TooltipPlacement.from("Sideways"));
        assertEquals(TooltipPlacement.LEFT, TooltipPlacement.from(" left "));
        assertEquals(TooltipPlacement.RIGHT, TooltipPlacement.from("RIGHT"));
        assertEquals(TooltipPlacement.BOTTOM, TooltipPlacement.from("Bottom"));
    }
}
