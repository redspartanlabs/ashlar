package com.redspartanlabs.ashlar.radio;

/**
 * One choice in a Radio Group's option list: its submitted value, its
 * visible label, an optional supporting description, and whether it can
 * currently be chosen. Mirrors
 * {@link com.redspartanlabs.ashlar.select.SelectOption}'s builder shape,
 * with the addition of {@code description} since a radio option's
 * supporting text is part of the option itself rather than the group.
 */
public final class RadioOption {

    private final String value;
    private final String label;
    private String description;
    private boolean disabled;

    private RadioOption(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public static RadioOption of(String value, String label) {
        return new RadioOption(value, label);
    }

    public RadioOption description(String description) {
        this.description = description;
        return this;
    }

    public RadioOption disabled(boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    public String value() {
        return value;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public boolean disabled() {
        return disabled;
    }
}
