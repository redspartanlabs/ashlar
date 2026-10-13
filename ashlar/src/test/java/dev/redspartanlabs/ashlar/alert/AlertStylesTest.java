package dev.redspartanlabs.ashlar.alert;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins what {@link AlertStyles} emits: Ashlar's own classes and nothing else. A Tailwind
 * utility sneaking back in would make the alert depend on Tailwind again.
 */
class AlertStylesTest {

    @ParameterizedTest
    @EnumSource(AlertVariant.class)
    void everyVariantIsTheBaseClassPlusItsOwnModifier(AlertVariant variant) {
        assertEquals("ash-alert ash-alert--" + variant.name().toLowerCase(Locale.ROOT), AlertStyles.container(variant));
    }

    @Test
    void theFourVariantsMapToTheFourDocumentedModifiers() {
        assertEquals(List.of("ash-alert ash-alert--info", "ash-alert ash-alert--success",
                        "ash-alert ash-alert--warning", "ash-alert ash-alert--error"),
                List.of(AlertVariant.values()).stream().map(AlertStyles::container).toList());
    }

    @ParameterizedTest
    @EnumSource(AlertVariant.class)
    void everyEmittedClassIsAnAshAlertClass(AlertVariant variant) {
        for (String classes : List.of(AlertStyles.container(variant), AlertStyles.icon(variant),
                AlertStyles.title(variant), AlertStyles.body(variant))) {
            for (String token : classes.split(" ")) {
                assertTrue(token.matches("ash-alert(--[a-z]+|__[a-z]+)?"), variant + " emitted a non-alert class: " + token);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(AlertVariant.class)
    void theElementClassesAreTheSameForEveryVariant(AlertVariant variant) {
        // The variant's colours come from the container's --ash-alert-* properties, not from per-variant child classes.
        assertEquals("ash-alert__icon", AlertStyles.icon(variant));
        assertEquals("ash-alert__title", AlertStyles.title(variant));
        assertEquals("ash-alert__body", AlertStyles.body(variant));
    }
}
