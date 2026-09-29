package com.redspartanlabs.ashlar.toast;

import com.redspartanlabs.ashlar.icon.Icon;

/**
 * The semantic meaning of a toast. Everything that follows from that meaning -
 * its screen-reader label, how urgently it is announced, whether it disappears
 * on its own, and its default icon - is decided here, once.
 */
public enum ToastVariant {
    SUCCESS, INFO, WARNING, DANGER, NEUTRAL;

    public static ToastVariant from(String value) {
        if (value == null) {
            return INFO;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return INFO;
        }
    }

    /** Prefix read by screen readers so severity never depends on color alone. */
    public String label() {
        return switch (this) {
            case SUCCESS -> "Success";
            case INFO -> "Information";
            case WARNING -> "Warning";
            case DANGER -> "Error";
            case NEUTRAL -> "Notification";
        };
    }

    /** Only errors interrupt; everything else waits for the screen reader to finish. */
    public String politeness() {
        return this == DANGER ? "assertive" : "polite";
    }

    /** Errors stay until dismissed, so nobody loses one by looking away. 0 means persistent. */
    public int defaultDurationMs() {
        return this == DANGER ? 0 : 5000;
    }

    public Icon defaultIcon() {
        return switch (this) {
            case SUCCESS -> Icon.CHECK_CIRCLE;
            case INFO -> Icon.INFO_CIRCLE;
            case WARNING -> Icon.WARNING_TRIANGLE;
            case DANGER -> Icon.X_CIRCLE;
            case NEUTRAL -> Icon.BELL;
        };
    }
}
