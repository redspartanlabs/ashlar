package dev.redspartanlabs.ashlar.timeline;

import dev.redspartanlabs.ashlar.badge.BadgeVariant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins what {@link TimelineStyles} emits: Ashlar's own marker classes and nothing else (a Tailwind utility sneaking back in would make
 * the marker depend on Tailwind again), and the status text a non-neutral entry announces, which the migration did not touch.
 */
class TimelineStylesTest {

    @ParameterizedTest
    @EnumSource(BadgeVariant.class)
    void everyVariantIsTheMarkerClassPlusItsOwnModifier(BadgeVariant variant) {
        assertEquals("ash-timeline-item__marker ash-timeline-item__marker--" + variant.name().toLowerCase(Locale.ROOT), TimelineStyles.marker(variant));
    }

    @ParameterizedTest
    @EnumSource(BadgeVariant.class)
    void everyClassIsATimelineItemMarkerClass(BadgeVariant variant) {
        for (String token : TimelineStyles.marker(variant).split(" ")) {
            assertTrue(token.matches("ash-timeline-item__marker(--[a-z]+)?"), variant + " emitted a non-marker class: " + token);
        }
    }

    @Test
    void theFiveVariantsMapToTheFiveDocumentedModifiers() {
        assertEquals(List.of("ash-timeline-item__marker ash-timeline-item__marker--neutral", "ash-timeline-item__marker ash-timeline-item__marker--info",
                        "ash-timeline-item__marker ash-timeline-item__marker--success", "ash-timeline-item__marker ash-timeline-item__marker--warning",
                        "ash-timeline-item__marker ash-timeline-item__marker--danger"),
                List.of(BadgeVariant.values()).stream().map(TimelineStyles::marker).toList());
    }

    @Test
    void theStatusPrefixIsEmptyForNeutralAndNamesTheVariantOtherwiseExactlyAsBefore() {
        assertEquals("", TimelineStyles.statusText(BadgeVariant.NEUTRAL));
        assertEquals("Info: ", TimelineStyles.statusText(BadgeVariant.INFO));
        assertEquals("Success: ", TimelineStyles.statusText(BadgeVariant.SUCCESS));
        assertEquals("Warning: ", TimelineStyles.statusText(BadgeVariant.WARNING));
        assertEquals("Danger: ", TimelineStyles.statusText(BadgeVariant.DANGER));
    }

    @Test
    void anUnknownOrMissingVariantStringFallsBackToNeutralThroughBadgeVariant() {
        assertEquals(BadgeVariant.NEUTRAL, BadgeVariant.from(null));
        assertEquals(BadgeVariant.NEUTRAL, BadgeVariant.from("no-such-variant"));
        assertEquals("ash-timeline-item__marker ash-timeline-item__marker--neutral", TimelineStyles.marker(BadgeVariant.from("no-such-variant")));
        assertEquals(BadgeVariant.DANGER, BadgeVariant.from(" danger "));
    }
}
