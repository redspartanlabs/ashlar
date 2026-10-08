package dev.redspartanlabs.ashlar.badge;

/**
 * The semantic meaning of a badge. Named to match {@code ButtonVariant} and
 * {@code ToastVariant} rather than inventing a separate vocabulary. Unknown
 * or missing values fall back to NEUTRAL so a typo in a template never
 * renders an unstyled badge.
 */
public enum BadgeVariant {
    NEUTRAL, INFO, SUCCESS, WARNING, DANGER;

    public static BadgeVariant from(String value) {
        if (value == null) {
            return NEUTRAL;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NEUTRAL;
        }
    }
}
