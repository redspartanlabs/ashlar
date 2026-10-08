package dev.redspartanlabs.ashlar.badge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BadgeVariantTest {

    @Test
    void fromParsesTheExactEnumName() {
        assertEquals(BadgeVariant.NEUTRAL, BadgeVariant.from("NEUTRAL"));
        assertEquals(BadgeVariant.INFO, BadgeVariant.from("INFO"));
        assertEquals(BadgeVariant.SUCCESS, BadgeVariant.from("SUCCESS"));
        assertEquals(BadgeVariant.WARNING, BadgeVariant.from("WARNING"));
        assertEquals(BadgeVariant.DANGER, BadgeVariant.from("DANGER"));
    }

    @Test
    void fromIsCaseInsensitive() {
        assertEquals(BadgeVariant.DANGER, BadgeVariant.from("danger"));
        assertEquals(BadgeVariant.SUCCESS, BadgeVariant.from("Success"));
    }

    @Test
    void fromTrimsWhitespace() {
        assertEquals(BadgeVariant.WARNING, BadgeVariant.from("  warning  "));
    }

    @Test
    void fromNullFallsBackToNeutral() {
        assertEquals(BadgeVariant.NEUTRAL, BadgeVariant.from(null));
    }

    @Test
    void fromUnrecognizedValueFallsBackToNeutral() {
        assertEquals(BadgeVariant.NEUTRAL, BadgeVariant.from("Error"));
        assertEquals(BadgeVariant.NEUTRAL, BadgeVariant.from("not-a-variant"));
    }

    @Test
    void fromEmptyStringFallsBackToNeutral() {
        assertEquals(BadgeVariant.NEUTRAL, BadgeVariant.from(""));
    }
}
