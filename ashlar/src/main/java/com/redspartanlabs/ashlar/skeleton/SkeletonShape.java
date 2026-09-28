package com.redspartanlabs.ashlar.skeleton;

/**
 * The corner treatment of a skeleton placeholder. Unknown or missing values
 * fall back to RECTANGLE. Only two shapes exist - there is no dedicated
 * "avatar" or "circle" value: a ROUNDED skeleton with equal width and height
 * already reads as a circle, and with unequal width/height it reads as a
 * pill, so a second shape value would just be RECTANGLE with different
 * radius, not a new concept.
 */
public enum SkeletonShape {
    RECTANGLE, ROUNDED;

    public static SkeletonShape from(String value) {
        if (value == null) {
            return RECTANGLE;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return RECTANGLE;
        }
    }
}
