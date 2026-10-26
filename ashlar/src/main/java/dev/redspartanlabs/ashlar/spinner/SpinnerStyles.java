package dev.redspartanlabs.ashlar.spinner;

import java.util.Locale;

/**
 * The single place that maps a spinner size to its CSS classes.
 *
 * <p>The classes are Ashlar's own ({@code ash-spinner__ring} plus one {@code ash-spinner__ring--<size>}
 * modifier), styled by {@code components/spinner.css} in the generated {@code ashlar.css}. They no longer
 * depend on Tailwind.
 *
 * <p>The look is unchanged: its own two-tone ring (a neutral track and a blue accent arc) rather than
 * button.jte's inline spinner, which uses {@code border-current} so it matches whatever text color its
 * specific button variant happens to have. This component is meant to also stand on its own - on a page
 * background, inside a card, next to a data table - where inheriting ambient text color would read as
 * accidental rather than as the same blue brand accent already used for primary buttons, links, and focus
 * rings everywhere else in the library.
 */
public final class SpinnerStyles {

    private SpinnerStyles() {
    }

    /** The ring's class string: the base class and exactly one size modifier. */
    public static String classes(SpinnerSize size) {
        return "ash-spinner__ring ash-spinner__ring--" + size.name().toLowerCase(Locale.ROOT);
    }
}
