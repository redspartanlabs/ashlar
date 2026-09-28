package com.redspartanlabs.ashlar.progress;

/**
 * The physical thickness of a progress bar. Unknown or missing values fall
 * back to MEDIUM. Only three sizes exist - unlike {@code SpinnerSize}, a
 * progress bar has no full-page/XLARGE use case: it always sits inline with
 * the content it describes, never as a whole-panel loading state.
 */
public enum ProgressSize {
    SMALL, MEDIUM, LARGE;

    public static ProgressSize from(String value) {
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
