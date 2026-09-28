package com.redspartanlabs.ashlar.spinner;

/**
 * The single place that maps a spinner size to Tailwind classes.
 *
 * <p>Deliberately its own two-tone (neutral ring + blue-600/500 accent arc)
 * rather than button.jte's inline spinner, which uses {@code border-current}
 * so it matches whatever text color its specific button variant happens to
 * have. This component is meant to also stand on its own - on a page
 * background, inside a card, next to a data table - where inheriting
 * ambient text color would read as accidental rather than as the same
 * blue-600 brand accent already used for primary buttons, links, and focus
 * rings everywhere else in the library.
 */
public final class SpinnerStyles {

    private SpinnerStyles() {
    }

    private static String size(SpinnerSize size) {
        return switch (size) {
            case SMALL -> "h-4 w-4 border-2";
            case MEDIUM -> "h-6 w-6 border-2";
            case LARGE -> "h-8 w-8 border-[3px]";
            case XLARGE -> "h-12 w-12 border-4";
        };
    }

    public static String classes(SpinnerSize size) {
        return "inline-block shrink-0 animate-spin rounded-full border-slate-200 border-t-blue-600 " +
                "motion-reduce:animate-none dark:border-slate-700 dark:border-t-blue-500 " +
                size(size);
    }
}
