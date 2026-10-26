package dev.redspartanlabs.ashlar.skeleton;

/**
 * The single place that maps a skeleton's shape to its CSS classes.
 *
 * <p>The classes are Ashlar's own ({@code ash-skeleton}, plus {@code ash-skeleton--rounded} for the fully
 * rounded shape), styled by {@code components/skeleton.css} in the generated {@code ashlar.css}. They no
 * longer depend on Tailwind.
 *
 * <p>The look is unchanged: the same neutral slate surface used for every other inert/track surface in the
 * library (progress.jte's track, spinner.jte's ring) - a placeholder should read as "empty surface," not
 * draw attention with its own color.
 */
public final class SkeletonStyles {

    private SkeletonStyles() {
    }

    /** The class string for a skeleton: the base class, and the rounded modifier for the ROUNDED shape. */
    public static String classes(SkeletonShape shape) {
        return switch (shape) {
            case RECTANGLE -> "ash-skeleton";
            case ROUNDED -> "ash-skeleton ash-skeleton--rounded";
        };
    }
}
