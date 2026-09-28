package com.redspartanlabs.ashlar.button;

/**
 * The visual/semantic style of a button. Unknown or missing values fall back
 * to PRIMARY so a typo in a template never renders an unstyled button.
 */
public enum ButtonVariant {
    PRIMARY, SECONDARY, SUCCESS, WARNING, DANGER, INFO, GHOST, LINK;

    public static ButtonVariant from(String value) {
        if (value == null) {
            return PRIMARY;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PRIMARY;
        }
    }
}
