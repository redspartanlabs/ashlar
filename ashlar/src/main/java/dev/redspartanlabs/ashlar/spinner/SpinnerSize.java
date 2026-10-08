package dev.redspartanlabs.ashlar.spinner;

/**
 * The physical size of a spinner. Unknown or missing values fall back to
 * MEDIUM. Caps at XLARGE, matching {@code ButtonSize}'s own ceiling - a
 * full-page/full-panel loading state is a real use case (XLARGE), but
 * nothing in this library goes past XLARGE, so a spinner has no
 * justification to either.
 */
public enum SpinnerSize {
    SMALL, MEDIUM, LARGE, XLARGE;

    public static SpinnerSize from(String value) {
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
