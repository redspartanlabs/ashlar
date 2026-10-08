package dev.redspartanlabs.ashlar.alert;

import dev.redspartanlabs.ashlar.icon.Icon;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AlertVariantTest {

    @Test
    void fromParsesTheExactEnumName() {
        assertEquals(AlertVariant.INFO, AlertVariant.from("INFO"));
        assertEquals(AlertVariant.SUCCESS, AlertVariant.from("SUCCESS"));
        assertEquals(AlertVariant.WARNING, AlertVariant.from("WARNING"));
        assertEquals(AlertVariant.ERROR, AlertVariant.from("ERROR"));
    }

    @Test
    void fromIsCaseInsensitive() {
        assertEquals(AlertVariant.ERROR, AlertVariant.from("error"));
        assertEquals(AlertVariant.SUCCESS, AlertVariant.from("Success"));
    }

    @Test
    void fromTrimsWhitespace() {
        assertEquals(AlertVariant.WARNING, AlertVariant.from("  warning  "));
    }

    @Test
    void fromNullFallsBackToInfo() {
        assertEquals(AlertVariant.INFO, AlertVariant.from(null));
    }

    @Test
    void fromUnrecognizedValueFallsBackToInfo() {
        assertEquals(AlertVariant.INFO, AlertVariant.from("Danger"));
        assertEquals(AlertVariant.INFO, AlertVariant.from("not-a-variant"));
    }

    @Test
    void fromEmptyStringFallsBackToInfo() {
        assertEquals(AlertVariant.INFO, AlertVariant.from(""));
    }

    @Test
    void labelMatchesEachVariant() {
        assertEquals("Information", AlertVariant.INFO.label());
        assertEquals("Success", AlertVariant.SUCCESS.label());
        assertEquals("Warning", AlertVariant.WARNING.label());
        assertEquals("Error", AlertVariant.ERROR.label());
    }

    @Test
    void defaultIconMatchesEachVariant() {
        assertEquals(Icon.INFO_CIRCLE, AlertVariant.INFO.defaultIcon());
        assertEquals(Icon.CHECK_CIRCLE, AlertVariant.SUCCESS.defaultIcon());
        assertEquals(Icon.WARNING_TRIANGLE, AlertVariant.WARNING.defaultIcon());
        assertEquals(Icon.X_CIRCLE, AlertVariant.ERROR.defaultIcon());
    }

    @ParameterizedTest
    @EnumSource(AlertVariant.class)
    void everyVariantHasANonNullLabelAndIcon(AlertVariant variant) {
        assertNotNull(variant.label());
        assertNotNull(variant.defaultIcon());
    }
}
