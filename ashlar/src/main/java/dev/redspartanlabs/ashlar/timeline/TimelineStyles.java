package dev.redspartanlabs.ashlar.timeline;

import dev.redspartanlabs.ashlar.badge.BadgeVariant;

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

    /** The marker circle's border/fill/icon color. Every variant keeps the
     *  same ring-plus-fill shape - only the color changes - so state is
     *  never the only thing distinguishing one marker from another; the
     *  entry's own title text is what actually says what happened. */
    public static String marker(BadgeVariant variant) {
        return switch (variant) {
            case NEUTRAL -> "border-slate-300 bg-white text-slate-400 " +
                    "dark:border-slate-600 dark:bg-slate-900 dark:text-slate-500";
            case INFO -> "border-sky-500 bg-sky-50 text-sky-600 " +
                    "dark:border-sky-500 dark:bg-sky-950 dark:text-sky-400";
            case SUCCESS -> "border-green-500 bg-green-50 text-green-600 " +
                    "dark:border-green-500 dark:bg-green-950 dark:text-green-400";
            case WARNING -> "border-amber-500 bg-amber-50 text-amber-600 " +
                    "dark:border-amber-500 dark:bg-amber-950 dark:text-amber-400";
            case DANGER -> "border-red-500 bg-red-50 text-red-600 " +
                    "dark:border-red-500 dark:bg-red-950 dark:text-red-400";
        };
    }

    /** An `sr-only` prefix announced just before a non-neutral entry's
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
