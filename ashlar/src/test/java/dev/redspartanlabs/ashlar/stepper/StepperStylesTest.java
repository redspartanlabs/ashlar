package dev.redspartanlabs.ashlar.stepper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins what {@link StepperStyles} emits: Ashlar's own part classes and nothing else (a Tailwind utility sneaking back in would make the step
 * depend on Tailwind again), the status text a step announces, and the status mapping and its fallback, which the migration did not touch.
 * A step's state itself is the modifier stepper-item.jte writes on the {@code <li>}, so the three class methods name only the part.
 */
class StepperStylesTest {

    @ParameterizedTest
    @EnumSource(StepperStatus.class)
    void eachPartClassNamesThePartWhateverTheStatus(StepperStatus status) {
        assertEquals("ash-stepper-item__indicator", StepperStyles.indicator(status));
        assertEquals("ash-stepper-item__line", StepperStyles.connector(status));
        assertEquals("ash-stepper-item__label", StepperStyles.label(status));
    }

    @ParameterizedTest
    @EnumSource(StepperStatus.class)
    void everyClassIsAStepperItemPartClass(StepperStatus status) {
        for (String classes : new String[]{StepperStyles.indicator(status), StepperStyles.connector(status), StepperStyles.label(status)}) {
            for (String token : classes.split(" ")) {
                assertTrue(token.matches("ash-stepper-item__[a-z]+"), status + " emitted a non-part class: " + token);
            }
        }
    }

    @Test
    void theStatusTextIsExactlyWhatScreenReadersHeardBefore() {
        assertEquals("Completed", StepperStyles.statusText(StepperStatus.COMPLETED));
        assertEquals("Current step", StepperStyles.statusText(StepperStatus.CURRENT));
        assertEquals("Not started", StepperStyles.statusText(StepperStatus.UPCOMING));
    }

    @Test
    void anUnknownOrMissingStatusFallsBackToUpcomingNeverToCompleted() {
        assertEquals(StepperStatus.UPCOMING, StepperStatus.from(null));
        assertEquals(StepperStatus.UPCOMING, StepperStatus.from(""));
        assertEquals(StepperStatus.UPCOMING, StepperStatus.from("done"));
        assertEquals(StepperStatus.UPCOMING, StepperStatus.from("Completed!"));
        assertEquals("Not started", StepperStyles.statusText(StepperStatus.from("no-such-status")));
    }

    @Test
    void theStatusIsTrimmedAndCaseInsensitive() {
        assertEquals(StepperStatus.COMPLETED, StepperStatus.from("Completed"));
        assertEquals(StepperStatus.COMPLETED, StepperStatus.from("COMPLETED"));
        assertEquals(StepperStatus.CURRENT, StepperStatus.from(" current "));
        assertEquals(StepperStatus.UPCOMING, StepperStatus.from("Upcoming"));
    }

    @Test
    void theThreeStatesAreTheWholeVocabulary() {
        assertEquals(3, StepperStatus.values().length);
        assertEquals("[COMPLETED, CURRENT, UPCOMING]", java.util.Arrays.toString(StepperStatus.values()));
    }
}
