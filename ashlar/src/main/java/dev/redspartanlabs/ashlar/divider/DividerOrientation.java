package dev.redspartanlabs.ashlar.divider;

/**
 * The axis a divider separates along. Unknown or missing values fall back to
 * HORIZONTAL, matching the other size/variant enums in this library.
 */
public enum DividerOrientation {
    HORIZONTAL, VERTICAL;

    public static DividerOrientation from(String value) {
        if (value == null) {
            return HORIZONTAL;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return HORIZONTAL;
        }
    }
}
