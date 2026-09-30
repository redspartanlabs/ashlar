package com.redspartanlabs.ashlar.datepicker;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * The small, fixed set of date DISPLAY formats the date picker supports.
 * This only controls what the user sees in the trigger - the value
 * submitted with the form is always the ISO "yyyy-MM-dd" string in the
 * hidden input, regardless of display format.
 *
 * <p>Deliberately does not support anything like "MM/yyyy": dropping the day
 * would change what is actually being picked (a month, not a date). That
 * would not be a display variation of this component - it would be a
 * different component (a month picker). See the final explanation for more
 * on this distinction.
 *
 * <p>To add a new display format: add a constant with its pattern here, then
 * add a matching case to the small formatDate() switch in date-picker.js -
 * that switch is what re-renders the trigger client-side after the page has
 * loaded, since Java can only format the initial, server-rendered value.
 *
 * <p>Written in plain pre-Java-14 style (classic enum with a field, a for
 * loop, try/catch - no switch expressions, no text blocks) since this
 * project may investigate Java 8 compatibility later.
 */
public enum DateDisplayFormat {
    ISO("yyyy-MM-dd"),
    US_SLASH("MM/dd/yyyy"),
    EUROPEAN_SLASH("dd/MM/yyyy"),
    LONG("MMMM d, yyyy"),
    MEDIUM("MMM d, yyyy");

    private final String pattern;

    DateDisplayFormat(String pattern) {
        this.pattern = pattern;
    }

    public String pattern() {
        return pattern;
    }

    /** Falls back to MEDIUM for an unrecognized pattern, so a typo never breaks rendering. */
    public static DateDisplayFormat fromPattern(String pattern) {
        for (DateDisplayFormat candidate : values()) {
            if (candidate.pattern.equals(pattern)) {
                return candidate;
            }
        }
        return MEDIUM;
    }

    /**
     * Formats an ISO ("yyyy-MM-dd") date string for the initial server-rendered
     * trigger label. Returns "" if isoDate is blank or not a valid date, so a
     * bad initial value degrades to "no selection" instead of an error page.
     */
    public static String formatIso(String isoDate, DateDisplayFormat format) {
        if (isoDate == null || isoDate.isBlank()) {
            return "";
        }
        try {
            LocalDate date = LocalDate.parse(isoDate);
            return date.format(DateTimeFormatter.ofPattern(format.pattern(), Locale.ENGLISH));
        } catch (DateTimeParseException e) {
            return "";
        }
    }
}
