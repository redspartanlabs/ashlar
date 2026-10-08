package dev.redspartanlabs.ashlar.modal;

/**
 * The physical width of a modal dialog. Unknown or missing values fall back
 * to MEDIUM, the same safe-default pattern used by ButtonSize/ButtonVariant.
 */
public enum ModalSize {
    SMALL, MEDIUM, LARGE;

    public static ModalSize from(String value) {
        if (value == null) {
            return MEDIUM;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEDIUM;
        }
    }

    public String maxWidthClass() {
        return switch (this) {
            case SMALL -> "max-w-sm";
            case MEDIUM -> "max-w-lg";
            case LARGE -> "max-w-2xl";
        };
    }
}
