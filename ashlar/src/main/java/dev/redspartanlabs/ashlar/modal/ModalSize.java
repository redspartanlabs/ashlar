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

    /**
     * The dialog's size modifier, styled by {@code components/modal.css} in the
     * generated {@code ashlar.css}: at most 24rem, 32rem or 42rem wide, the
     * widths Tailwind's max-w-sm, max-w-lg and max-w-2xl gave it before. The
     * name is kept from when it returned those Tailwind classes; the classes are
     * now Ashlar's own and no longer depend on Tailwind.
     */
    public String maxWidthClass() {
        return switch (this) {
            case SMALL -> "ash-modal__dialog--sm";
            case MEDIUM -> "ash-modal__dialog--md";
            case LARGE -> "ash-modal__dialog--lg";
        };
    }
}
