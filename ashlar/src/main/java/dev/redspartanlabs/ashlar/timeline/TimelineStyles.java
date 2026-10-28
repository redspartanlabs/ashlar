package dev.redspartanlabs.ashlar.timeline;

import dev.redspartanlabs.ashlar.badge.BadgeVariant;

import java.util.Locale;

/**
 * Maps a timeline entry's {@link BadgeVariant} to its marker classes and
 * accessible status text - the single place that knows the geometry, same
 * division of labor as {@code StepperStyles}/{@code BadgeStyles}.
 *
 * <p>Reuses {@code BadgeVariant} rather than a new Timeline-specific enum:
 * Badge's own doc comment already names this exact vocabulary (Neutral,
 * Info, Success, Warning, Danger) as the one shared across
 * {@code ButtonVariant}/{@code ToastVariant} too, and a timeline entry's
 * state ("this deploy succeeded," "this step failed") is the same kind of
 * fact a Badge already exists to represent - inventing a parallel
 * Timeline-only vocabulary would just be that same idea under a different
 * name.
 */
public final class TimelineStyles {

    private TimelineStyles() {
    }

    /** The marker's full class string: the base class and exactly one variant modifier. The classes are Ashlar's own
     *  ({@code ash-timeline-item__marker} plus one {@code ash-timeline-item__marker--<variant>}), styled by
     *  {@code components/timeline-item.css} in the generated {@code ashlar.css}; they no longer depend on Tailwind. Every
     *  variant keeps the same ring-plus-fill shape - only the color changes - so state is never the only thing
     *  distinguishing one marker from another; the entry's own title text is what actually says what happened. */
    public static String marker(BadgeVariant variant) {
        return "ash-timeline-item__marker ash-timeline-item__marker--" + variant.name().toLowerCase(Locale.ROOT);
    }

    /** An `ash-sr-only` prefix announced just before a non-neutral entry's
     *  title, the same reasoning as {@code StepperStyles.statusText()}: the
     *  marker's color and its icon are both `aria-hidden`, so without this
     *  the variant would be communicated by color alone. Named after the
     *  variant itself, not a guessed meaning ("Failed," "Blocked," etc.) -
     *  this component has no way to know what a given entry's DANGER
     *  variant specifically represents, only that it does. Empty for
     *  NEUTRAL, which isn't communicating any particular state. */
    public static String statusText(BadgeVariant variant) {
        return switch (variant) {
            case NEUTRAL -> "";
            case INFO -> "Info: ";
            case SUCCESS -> "Success: ";
            case WARNING -> "Warning: ";
            case DANGER -> "Danger: ";
        };
    }
}
