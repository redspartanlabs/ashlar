package dev.redspartanlabs.ashlar.spinner;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The spinner's public Java surface: the size enum and the one place its sizes become CSS classes. */
class SpinnerStylesTest {

    @Test
    void theFourSizesAreUnchangedAndInTheirDeclaredOrder() {
        assertEquals(List.of(SpinnerSize.SMALL, SpinnerSize.MEDIUM, SpinnerSize.LARGE, SpinnerSize.XLARGE), List.of(SpinnerSize.values()));
    }

    @Test
    void everySizeBecomesTheBaseClassAndExactlyOneSizeModifier() {
        assertEquals("ash-spinner__ring ash-spinner__ring--small", SpinnerStyles.classes(SpinnerSize.SMALL));
        assertEquals("ash-spinner__ring ash-spinner__ring--medium", SpinnerStyles.classes(SpinnerSize.MEDIUM));
        assertEquals("ash-spinner__ring ash-spinner__ring--large", SpinnerStyles.classes(SpinnerSize.LARGE));
        assertEquals("ash-spinner__ring ash-spinner__ring--xlarge", SpinnerStyles.classes(SpinnerSize.XLARGE));
    }

    @Test
    void noSizeCarriesATailwindUtility() {
        for (SpinnerSize size : SpinnerSize.values()) {
            for (String token : SpinnerStyles.classes(size).split(" ")) {
                assertTrue(token.startsWith("ash-spinner__ring"), "not an Ashlar spinner class: " + token);
            }
        }
    }

    @Test
    void unknownBlankAndMissingSizesFallBackToMediumAndKnownOnesIgnoreCaseAndSpaces() {
        assertEquals(SpinnerSize.MEDIUM, SpinnerSize.from(null));
        assertEquals(SpinnerSize.MEDIUM, SpinnerSize.from(""));
        assertEquals(SpinnerSize.MEDIUM, SpinnerSize.from("huge"));
        assertEquals(SpinnerSize.SMALL, SpinnerSize.from("Small"));
        assertEquals(SpinnerSize.LARGE, SpinnerSize.from("  large "));
        assertEquals(SpinnerSize.XLARGE, SpinnerSize.from("XLARGE"));
        assertEquals(SpinnerSize.MEDIUM, SpinnerSize.from("Medium"));
    }
}
