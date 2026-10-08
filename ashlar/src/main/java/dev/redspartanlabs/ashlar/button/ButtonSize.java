package dev.redspartanlabs.ashlar.button;

/**
 * The physical size of a button. Unknown or missing values fall back to
 * MEDIUM.
 */
public enum ButtonSize {
    XSMALL, SMALL, MEDIUM, LARGE, XLARGE;

    public static ButtonSize from(String value) {
        if (value == null) {
            return MEDIUM;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEDIUM;
        }
    }
}
