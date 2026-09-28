package com.redspartanlabs.ashlar.alert;

import com.redspartanlabs.ashlar.icon.Icon;

/**
 * The semantic meaning of an alert. Unknown or missing values fall back to
 * INFO so a typo in a template never renders an unstyled alert.
 *
 * <p>Named ERROR rather than DANGER - the spelling {@link com.redspartanlabs.ashlar.button.ButtonVariant},
 * {@code ToastVariant} and {@link com.redspartanlabs.ashlar.badge.BadgeVariant}
 * all use for this same red state. ({@code ToastVariant} is {@code @code}
 * rather than {@code @link} only because Toast has not migrated yet; restore
 * the link when it does.) DANGER reads naturally on a button
 * ("this action is dangerous"); an alert is reporting that something already
 * went wrong, which ERROR names more directly. NEUTRAL is also deliberately
 * not carried over from Toast/Badge: an alert with no semantic color would
 * just be a paragraph, so there's no real neutral-alert use case to support.
 */
public enum AlertVariant {
    INFO, SUCCESS, WARNING, ERROR;

    public static AlertVariant from(String value) {
        if (value == null) {
            return INFO;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return INFO;
        }
    }

    /** Prefix read by screen readers so meaning never depends on color or
     *  the (decorative) icon alone - same idea as ToastVariant.label(). */
    public String label() {
        return switch (this) {
            case INFO -> "Information";
            case SUCCESS -> "Success";
            case WARNING -> "Warning";
            case ERROR -> "Error";
        };
    }

    public Icon defaultIcon() {
        return switch (this) {
            case INFO -> Icon.INFO_CIRCLE;
            case SUCCESS -> Icon.CHECK_CIRCLE;
            case WARNING -> Icon.WARNING_TRIANGLE;
            case ERROR -> Icon.X_CIRCLE;
        };
    }
}
