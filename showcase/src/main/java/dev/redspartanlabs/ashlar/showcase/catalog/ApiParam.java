package dev.redspartanlabs.ashlar.showcase.catalog;

/**
 * One row of a component's API reference table: the public, JTE-facing
 * contract a consumer actually needs - not private implementation detail.
 *
 * <p>Showcase-side only, same as {@link ComponentPage}: this describes how
 * the documentation presents a component's {@code @param} list, it is not
 * part of the Ashlar library itself.
 */
public record ApiParam(String name, String type, boolean required, String defaultValue, String description) {

    /** A required parameter has no default to show. */
    public static ApiParam required(String name, String type, String description) {
        return new ApiParam(name, type, true, "", description);
    }

    /** An optional parameter, documented with the default it actually falls back to. */
    public static ApiParam optional(String name, String type, String defaultValue, String description) {
        return new ApiParam(name, type, false, defaultValue, description);
    }
}
