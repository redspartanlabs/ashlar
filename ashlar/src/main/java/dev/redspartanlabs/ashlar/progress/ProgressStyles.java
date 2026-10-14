package dev.redspartanlabs.ashlar.progress;

import java.util.Locale;

/**
 * The single place that maps a progress bar's size to its CSS classes.
 *
 * <p>The classes are Ashlar's own ({@code ash-progress__track} plus one {@code ash-progress__track--<size>}
 * modifier, and {@code ash-progress__fill}), styled by {@code components/progress.css} in the generated
 * {@code ashlar.css}. They no longer depend on Tailwind.
 *
 * <p>The fill reuses spinner.jte's own accent color (blue-600 / dark:blue-500)
 * for the same reason documented there: this component stands on its own
 * across the page - inline, inside a card, next to a data table - so it uses
 * the same brand-blue accent as primary buttons, links, and Spinner rather
 * than inheriting ambient text color, which would read as accidental. The colours now live in the
 * {@code --ash-progress-track} and {@code --ash-progress-fill} properties on the wrapper.
 */
public final class ProgressStyles {

    private ProgressStyles() {
    }

    /** The track's class string: the base class and exactly one size modifier. */
    public static String track(ProgressSize size) {
        return "ash-progress__track ash-progress__track--" + size.name().toLowerCase(Locale.ROOT);
    }

    /** The fill's class. Its colour comes from the wrapper's {@code --ash-progress-fill} property. */
    public static String fill() {
        return "ash-progress__fill";
    }
}
