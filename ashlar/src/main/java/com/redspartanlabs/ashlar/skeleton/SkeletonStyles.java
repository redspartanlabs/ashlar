package com.redspartanlabs.ashlar.skeleton;

/**
 * The single place that maps a skeleton's shape to Tailwind classes.
 *
 * <p>Same neutral slate-200/dark:slate-700 surface used for every other
 * inert/track surface in the library (progress.jte's track, spinner.jte's
 * ring) - a placeholder should read as "empty surface," not draw attention
 * with its own color.
 */
public final class SkeletonStyles {

    private SkeletonStyles() {
    }

    private static String radius(SkeletonShape shape) {
        return switch (shape) {
            case RECTANGLE -> "rounded";
            case ROUNDED -> "rounded-full";
        };
    }

    public static String classes(SkeletonShape shape) {
        return "block shrink-0 animate-pulse bg-slate-200 motion-reduce:animate-none dark:bg-slate-700 " + radius(shape);
    }
}
