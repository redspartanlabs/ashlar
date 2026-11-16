package dev.redspartanlabs.ashlar.drawer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DrawerStylesTest {

    @Test
    void placementFallsBackToRight() {
        assertEquals(DrawerPlacement.LEFT, DrawerPlacement.from("Left"));
        assertEquals(DrawerPlacement.RIGHT, DrawerPlacement.from("Right"));
        assertEquals(DrawerPlacement.RIGHT, DrawerPlacement.from("Top"));
        assertEquals(DrawerPlacement.RIGHT, DrawerPlacement.from(null));
    }

    @Test
    void theMethodsKeepTheirNamesAndReturnTheDrawersOwnPlacementModifiers() {
        assertEquals("ash-drawer--left", DrawerStyles.alignment(DrawerPlacement.LEFT));
        assertEquals("ash-drawer--right", DrawerStyles.alignment(DrawerPlacement.RIGHT));
        assertEquals("ash-drawer__panel--left", DrawerStyles.border(DrawerPlacement.LEFT));
        assertEquals("ash-drawer__panel--right", DrawerStyles.border(DrawerPlacement.RIGHT));
    }

    @Test
    void theClosedPositionIsCarriedByThePanelsPlacementModifier() {
        for (DrawerPlacement p : DrawerPlacement.values()) {
            assertEquals(DrawerStyles.border(p), DrawerStyles.closedTransform(p), p.name());
        }
    }
}
