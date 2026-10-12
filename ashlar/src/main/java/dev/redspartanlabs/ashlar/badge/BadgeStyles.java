package dev.redspartanlabs.ashlar.badge;

import java.util.Locale;

/**
 * The single place that maps a badge variant to its CSS classes.
 *
 * <p>The classes are Ashlar's own ({@code ash-badge} plus one {@code ash-badge--<variant>}
 * modifier), styled by {@code components/badge.css} in the generated {@code ashlar.css}.
 * They no longer depend on Tailwind. The look is the same "soft pill" language as before - a
 * tinted background with an inset ring rather than a solid fill - because a badge is passive
 * metadata, not a call to action, and shouldn't visually compete with real buttons sitting
 * next to it.
 */
public final class BadgeStyles {

    private BadgeStyles() {
    }

    /** The full class string for a badge: the base class and exactly one variant modifier. */
    public static String container(BadgeVariant variant) {
        return "ash-badge ash-badge--" + variant.name().toLowerCase(Locale.ROOT);
    }
}
