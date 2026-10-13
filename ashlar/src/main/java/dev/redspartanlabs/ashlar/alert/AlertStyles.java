package dev.redspartanlabs.ashlar.alert;

import java.util.Locale;

/**
 * The single place that maps an alert variant to its CSS classes.
 *
 * <p>The classes are Ashlar's own ({@code ash-alert} plus one {@code ash-alert--<variant>}
 * modifier, and {@code ash-alert__*} element classes), styled by {@code components/alert.css} in
 * the generated {@code ashlar.css}. They no longer depend on Tailwind.
 *
 * <p>The look is unchanged: its own tinted-banner language - Badge's palette pairing (a light tint
 * with a darker border, text in the same hue) sized up from a pill into a full-width block, not
 * Toast's white background with a left accent border, which exists so a toast reads as a floating
 * card. An alert sits flat in the page's own flow and doesn't need that cue.
 *
 * <p>The variant now lives in one place - the container's modifier class, which sets the
 * {@code --ash-alert-*} colour properties the icon, title and body read. So {@link #icon},
 * {@link #title} and {@link #body} keep their signatures (they are public API) but return the same
 * class for every variant.
 */
public final class AlertStyles {

    private AlertStyles() {
    }

    /** The full class string for an alert: the base class and exactly one variant modifier. */
    public static String container(AlertVariant variant) {
        return "ash-alert ash-alert--" + variant.name().toLowerCase(Locale.ROOT);
    }

    /** The icon's class. Its colour comes from the container's variant, not from this class. */
    public static String icon(AlertVariant variant) {
        return "ash-alert__icon";
    }

    /** The title's class. Its colour comes from the container's variant, not from this class. */
    public static String title(AlertVariant variant) {
        return "ash-alert__title";
    }

    /** The body's class. Its colour comes from the container's variant, not from this class. */
    public static String body(AlertVariant variant) {
        return "ash-alert__body";
    }
}
