package dev.redspartanlabs.ashlar.progress;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins what {@link ProgressStyles} emits (Ashlar's own classes and nothing else, so a Tailwind utility sneaking back
 * in would make the progress bar depend on Tailwind again) and how {@link ProgressSize} resolves its input.
 */
class ProgressStylesTest {

    @ParameterizedTest
    @EnumSource(ProgressSize.class)
    void everySizeIsTheBaseTrackClassPlusItsOwnModifier(ProgressSize size) {
        assertEquals("ash-progress__track ash-progress__track--" + size.name().toLowerCase(Locale.ROOT), ProgressStyles.track(size));
    }

    @Test
    void theThreeSizesMapToTheThreeDocumentedModifiers() {
        assertEquals(List.of("ash-progress__track ash-progress__track--small", "ash-progress__track ash-progress__track--medium",
                        "ash-progress__track ash-progress__track--large"),
                List.of(ProgressSize.values()).stream().map(ProgressStyles::track).toList());
    }

    @Test
    void theFillIsOneConstantClass() {
        assertEquals("ash-progress__fill", ProgressStyles.fill());
    }

    @ParameterizedTest
    @EnumSource(ProgressSize.class)
    void noTailwindUtilityRemains(ProgressSize size) {
        for (String classes : new String[] {ProgressStyles.track(size), ProgressStyles.fill()}) {
            for (String token : classes.split(" ")) {
                assertTrue(token.matches("ash-progress__[a-z]+(--[a-z]+)?"), "not one of Ashlar's own classes: " + token);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Small", "small", "SMALL", "  Small  "})
    void smallIsRecognisedWhateverItsCaseOrPadding(String value) {
        assertEquals(ProgressSize.SMALL, ProgressSize.from(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Large", "large", "LARGE", " large "})
    void largeIsRecognisedWhateverItsCaseOrPadding(String value) {
        assertEquals(ProgressSize.LARGE, ProgressSize.from(value));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "huge", "xlarge", "medium-ish", "Medium"})
    void nullBlankAndUnrecognisedValuesFallBackToMedium(String value) {
        assertEquals(ProgressSize.MEDIUM, ProgressSize.from(value));
    }
}
