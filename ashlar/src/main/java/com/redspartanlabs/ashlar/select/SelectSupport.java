package com.redspartanlabs.ashlar.select;

import java.util.List;

public final class SelectSupport {

    private SelectSupport() {
    }

    /**
     * The option matching {@code value}, or null if none does - including
     * when {@code value} is null/empty, which always means "no selection"
     * regardless of what options exist.
     */
    public static SelectOption resolveSelected(List<SelectOption> options, String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (SelectOption option : options) {
            if (option.value().equals(value)) {
                return option;
            }
        }
        return null;
    }
}
