package dev.redspartanlabs.ashlar.select;

/**
 * One choice in a Select's option list: its submitted value, its visible
 * label, and whether it can currently be chosen. Mirrors
 * {@link dev.redspartanlabs.ashlar.tabs.Tab}'s builder shape.
 */
public final class SelectOption {

    private final String value;
    private final String label;
    private boolean disabled;

    private SelectOption(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public static SelectOption of(String value, String label) {
        return new SelectOption(value, label);
    }

    public SelectOption disabled(boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    public String value() {
        return value;
    }

    public String label() {
        return label;
    }

    public boolean disabled() {
        return disabled;
    }
}
