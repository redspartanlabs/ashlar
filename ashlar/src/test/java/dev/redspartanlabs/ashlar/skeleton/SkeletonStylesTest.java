package dev.redspartanlabs.ashlar.skeleton;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The skeleton's public Java surface: the shape enum and the one place its shapes become CSS classes. */
class SkeletonStylesTest {

    @Test
    void theTwoShapesAreUnchangedAndInTheirDeclaredOrder() {
        assertEquals(List.of(SkeletonShape.RECTANGLE, SkeletonShape.ROUNDED), List.of(SkeletonShape.values()));
    }

    @Test
    void aRectangleIsTheBaseClassAndARoundedSkeletonAddsTheModifier() {
        assertEquals("ash-skeleton", SkeletonStyles.classes(SkeletonShape.RECTANGLE));
        assertEquals("ash-skeleton ash-skeleton--rounded", SkeletonStyles.classes(SkeletonShape.ROUNDED));
    }

    @Test
    void unknownBlankAndMissingShapesFallBackToRectangleAndKnownOnesIgnoreCaseAndSpaces() {
        assertEquals(SkeletonShape.RECTANGLE, SkeletonShape.from(null));
        assertEquals(SkeletonShape.RECTANGLE, SkeletonShape.from(""));
        assertEquals(SkeletonShape.RECTANGLE, SkeletonShape.from("circle"));
        assertEquals(SkeletonShape.RECTANGLE, SkeletonShape.from("Rectangle"));
        assertEquals(SkeletonShape.ROUNDED, SkeletonShape.from("Rounded"));
        assertEquals(SkeletonShape.ROUNDED, SkeletonShape.from("  rounded "));
    }
}
