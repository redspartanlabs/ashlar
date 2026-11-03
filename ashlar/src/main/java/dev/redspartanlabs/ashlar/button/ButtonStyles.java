package dev.redspartanlabs.ashlar.button;

import java.util.Locale;

/**
 * The single place that maps a button's variant/size/state to its CSS classes.
 *
 * <p>The classes are Ashlar's own: {@code ash-btn} plus one variant modifier ({@code ash-btn--primary}, ...), one size modifier
 * ({@code ash-btn--xs} ... {@code ash-btn--xl}) and, when they apply, {@code ash-btn--icon}, {@code ash-btn--block} and
 * {@code ash-btn--pressed}. They are styled by {@code components/button.css} in the generated {@code ashlar.css} and no longer depend on
 * Tailwind. Hover, active, focus-visible, disabled, loading and dark mode are not classes: they are CSS state selectors
 * ({@code :hover}, {@code :active}, {@code :focus-visible}, {@code :disabled}, {@code [aria-disabled="true"]},
 * {@code [aria-busy="true"]}) and {@code data-theme="dark"}.
 *
 * <p>The pressed look has two carriers, and a button uses exactly one of them. A rendered toggle Button carries
 * {@code aria-pressed}, which its script flips, so its markup never carries {@code ash-btn--pressed} and the two can never disagree. Any
 * other pressed button (a Button rendered with {@code pressed = true} and {@code toggle = false}, or a consumer's own element that uses
 * these classes) has no {@code aria-pressed}, so it carries {@code ash-btn--pressed}; that is what {@link #pressedClasses} returns.
 *
 * <p>To add a new variant: add a constant to {@link ButtonVariant}, then give it a matching {@code .ash-btn--<name>} rule (and its pressed
 * rule) in {@code button.css}. Nothing else needs to change: the JTE template and the script read state, not individual variants.
 */
public final class ButtonStyles {

    private ButtonStyles() {
    }

    private static String variant(ButtonVariant variant) {
        return "ash-btn--" + variant.name().toLowerCase(Locale.ROOT);
    }

    private static String size(ButtonSize size) {
        return switch (size) {
            case XSMALL -> "ash-btn--xs";
            case SMALL -> "ash-btn--sm";
            case MEDIUM -> "ash-btn--md";
            case LARGE -> "ash-btn--lg";
            case XLARGE -> "ash-btn--xl";
        };
    }

    /**
     * The icon size class paired to each button size, so icons scale with the button. These are the Icon component's own size modifiers
     * ({@code ash-icon--xs} 0.75rem, {@code --sm} 1rem, {@code --md} 1.25rem, {@code --lg} 1.5rem), which is what the former
     * {@code h-3 w-3}, {@code h-4 w-4}, {@code h-5 w-5} and {@code h-6 w-6} were.
     */
    public static String iconSizeClasses(ButtonSize size) {
        return switch (size) {
            case XSMALL -> "ash-icon--xs";
            case SMALL -> "ash-icon--sm";
            case MEDIUM -> "ash-icon--sm";
            case LARGE -> "ash-icon--md";
            case XLARGE -> "ash-icon--lg";
        };
    }

    /**
     * Full class string for a button in its resting or pressed state: {@code ash-btn}, the variant, the size, then {@code ash-btn--icon}
     * when {@code iconOnly}, {@code ash-btn--block} when {@code fullWidth} and {@code ash-btn--pressed} when {@code pressed}, in that order.
     */
    public static String classes(ButtonVariant variant, ButtonSize size, boolean iconOnly, boolean fullWidth, boolean pressed) {
        StringBuilder classes = new StringBuilder("ash-btn ").append(variant(variant)).append(' ').append(size(size));
        if (iconOnly) {
            classes.append(" ash-btn--icon");
        }
        if (fullWidth) {
            classes.append(" ash-btn--block");
        }
        if (pressed) {
            classes.append(" ash-btn--pressed");
        }
        return classes.toString();
    }

    /** The classes of a button that is not pressed: {@code classes(..., false)}. */
    public static String restingClasses(ButtonVariant variant, ButtonSize size, boolean iconOnly, boolean fullWidth) {
        return classes(variant, size, iconOnly, fullWidth, false);
    }

    /**
     * The classes of the same button while pressed: {@link #restingClasses} plus {@code ash-btn--pressed}, which draws the pressed look
     * for an element that has no {@code aria-pressed} to carry it. A rendered toggle Button does not use this: its pressed look comes from
     * {@code aria-pressed="true"}, so toggling it changes an attribute and no class.
     */
    public static String pressedClasses(ButtonVariant variant, ButtonSize size, boolean iconOnly, boolean fullWidth) {
        return classes(variant, size, iconOnly, fullWidth, true);
    }

    /** Defends against an empty/invalid HTML button type silently becoming "submit". */
    public static String normalizeButtonType(String value) {
        if (value == null) {
            return "button";
        }
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "submit", "reset" -> normalized;
            default -> "button";
        };
    }
}
