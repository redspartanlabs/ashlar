package dev.redspartanlabs.ashlar.popover;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** PopoverStyles returns the Ashlar placement modifiers, and PopoverPlacement keeps its fallback to BOTTOM. */
class PopoverStylesTest {

    @Test
    void eachPlacementHasItsSurfaceModifier() {
        assertEquals("ash-popover__surface--top", PopoverStyles.position(PopoverPlacement.TOP));
        assertEquals("ash-popover__surface--right", PopoverStyles.position(PopoverPlacement.RIGHT));
        assertEquals("ash-popover__surface--bottom", PopoverStyles.position(PopoverPlacement.BOTTOM));
        assertEquals("ash-popover__surface--left", PopoverStyles.position(PopoverPlacement.LEFT));
    }

    @Test
    void anUnrecognizedPlacementFallsBackToBottom() {
        assertEquals(PopoverPlacement.BOTTOM, PopoverPlacement.from(null));
        assertEquals(PopoverPlacement.BOTTOM, PopoverPlacement.from(""));
        assertEquals(PopoverPlacement.BOTTOM, PopoverPlacement.from("Sideways"));
        assertEquals(PopoverPlacement.LEFT, PopoverPlacement.from(" left "));
        assertEquals(PopoverPlacement.TOP, PopoverPlacement.from("TOP"));
        assertEquals(PopoverPlacement.RIGHT, PopoverPlacement.from("Right"));
    }
}
