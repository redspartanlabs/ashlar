package dev.redspartanlabs.ashlar.stepper;

/**
 * A step's position relative to the caller's current progress through the
 * sequence. Named to match {@code BadgeVariant}/{@code AlertVariant}'s own
 * small, explicit vocabulary rather than a boolean "done" flag plus a
 * separate "is this the active one" flag - a step is always in exactly one
 * of these three states, never some combination, so one enum says that
 * directly.
 *
 * <p>Unknown or missing values fall back to UPCOMING, not COMPLETED: a
 * typo'd status should never cause a step the caller didn't actually finish
 * to render with a checkmark - understating progress is a far safer default
 * than overstating it.
 */
public enum StepperStatus {
    COMPLETED, CURRENT, UPCOMING;

    public static StepperStatus from(String value) {
        if (value == null) {
            return UPCOMING;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UPCOMING;
        }
    }
}
