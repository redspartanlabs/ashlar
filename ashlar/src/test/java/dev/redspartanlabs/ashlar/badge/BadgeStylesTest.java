package dev.redspartanlabs.ashlar.badge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins what {@link BadgeStyles} emits: Ashlar's own classes and nothing else. A Tailwind
 * utility sneaking back in would make the badge depend on Tailwind again.
 */
class BadgeStylesTest {

    @ParameterizedTest
    @EnumSource(BadgeVariant.class)
    void everyVariantIsTheBaseClassPlusItsOwnModifier(BadgeVariant variant) {
        assertEquals("ash-badge ash-badge--" + variant.name().toLowerCase(Locale.ROOT), BadgeStyles.container(variant));
    }

    @ParameterizedTest
    @EnumSource(BadgeVariant.class)
    void everyClassIsAnAshBadgeClass(BadgeVariant variant) {
        for (String token : BadgeStyles.container(variant).split(" ")) {
            assertTrue(token.matches("ash-badge(--[a-z]+)?"), variant + " emitted a non-badge class: " + token);
        }
    }

    @Test
    void theFiveVariantsMapToTheFiveDocumentedModifiers() {
        assertEquals(List.of("ash-badge ash-badge--neutral", "ash-badge ash-badge--info", "ash-badge ash-badge--success",
                        "ash-badge ash-badge--warning", "ash-badge ash-badge--danger"),
                List.of(BadgeVariant.values()).stream().map(BadgeStyles::container).toList());
    }
}
