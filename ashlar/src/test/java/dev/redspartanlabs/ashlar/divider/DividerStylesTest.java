package dev.redspartanlabs.ashlar.divider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Pins what {@link DividerStyles} emits (Ashlar's own classes and nothing else, so a Tailwind utility sneaking back
 * in would make the divider depend on Tailwind again) and how {@link DividerOrientation} resolves its input.
 */
class DividerStylesTest {

    @Test
    void horizontalIsTheBaseClassPlusTheHorizontalModifier() {
        assertEquals("ash-divider ash-divider--horizontal", DividerStyles.horizontal());
    }

    @Test
    void verticalIsTheBaseClassPlusTheVerticalModifier() {
        assertEquals("ash-divider ash-divider--vertical", DividerStyles.vertical());
    }

    @Test
    void everyTokenIsAnAshlarClassAndNoTailwindUtilityRemains() {
        for (String classes : new String[] {DividerStyles.horizontal(), DividerStyles.vertical()}) {
            for (String token : classes.split(" ")) {
                assertEquals(true, token.startsWith("ash-divider"), token);
            }
            assertFalse(classes.contains("border-"), classes);
            assertFalse(classes.contains("dark:"), classes);
        }
        assertNotEquals(DividerStyles.horizontal(), DividerStyles.vertical());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Vertical", "vertical", "VERTICAL", "  Vertical  "})
    void verticalIsRecognisedWhateverItsCaseOrPadding(String value) {
        assertEquals(DividerOrientation.VERTICAL, DividerOrientation.from(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Horizontal", "horizontal", "HORIZONTAL", "  horizontal "})
    void horizontalIsRecognisedWhateverItsCaseOrPadding(String value) {
        assertEquals(DividerOrientation.HORIZONTAL, DividerOrientation.from(value));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "diagonal", "vertical-ish", "up"})
    void nullBlankAndUnrecognisedValuesFallBackToHorizontal(String value) {
        assertEquals(DividerOrientation.HORIZONTAL, DividerOrientation.from(value));
    }
}
