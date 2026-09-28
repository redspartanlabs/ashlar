package com.redspartanlabs.ashlar.progress;

/**
 * The single place that maps a progress bar's size to Tailwind classes.
 *
 * <p>The fill reuses spinner.jte's own accent color (blue-600 / dark:blue-500)
 * for the same reason documented there: this component stands on its own
 * across the page - inline, inside a card, next to a data table - so it uses
 * the same brand-blue accent as primary buttons, links, and Spinner rather
 * than inheriting ambient text color, which would read as accidental.
 */
public final class ProgressStyles {

    private ProgressStyles() {
    }

    private static String height(ProgressSize size) {
        return switch (size) {
            case SMALL -> "h-1.5";
            case MEDIUM -> "h-2";
            case LARGE -> "h-3";
        };
    }

    public static String track(ProgressSize size) {
        return "w-full overflow-hidden rounded-full bg-slate-200 dark:bg-slate-700 " + height(size);
    }

    public static String fill() {
        return "h-full rounded-full bg-blue-600 dark:bg-blue-500";
    }
}
