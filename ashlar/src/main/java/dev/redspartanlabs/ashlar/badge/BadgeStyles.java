package dev.redspartanlabs.ashlar.badge;

/**
 * The single place that maps a badge variant to Tailwind classes.
 *
 * <p>This is deliberately the same "soft pill" language data-table.js
 * already hand-rolls for its own status column (rounded-full, ring-1
 * ring-inset, a tinted background rather than a solid fill) rather than
 * reusing ButtonStyles' high-contrast solid-fill look - a badge is passive
 * metadata, not a call to action, and shouldn't visually compete with real
 * buttons sitting next to it.
 */
public final class BadgeStyles {

    private BadgeStyles() {
    }

    private static String base() {
        return "inline-flex items-center gap-1 whitespace-nowrap rounded-full px-2 py-1 text-xs font-medium ring-1 ring-inset";
    }

    private static String accent(BadgeVariant variant) {
        return switch (variant) {
            case NEUTRAL -> "bg-slate-100 text-slate-600 ring-slate-500/20 " +
                    "dark:bg-slate-800 dark:text-slate-400 dark:ring-slate-500/30";
            case INFO -> "bg-sky-50 text-sky-700 ring-sky-600/20 " +
                    "dark:bg-sky-950 dark:text-sky-300 dark:ring-sky-500/30";
            case SUCCESS -> "bg-green-50 text-green-700 ring-green-600/20 " +
                    "dark:bg-green-950 dark:text-green-300 dark:ring-green-500/30";
            case WARNING -> "bg-amber-50 text-amber-700 ring-amber-600/20 " +
                    "dark:bg-amber-950 dark:text-amber-300 dark:ring-amber-500/30";
            case DANGER -> "bg-red-50 text-red-700 ring-red-600/20 " +
                    "dark:bg-red-950 dark:text-red-300 dark:ring-red-500/30";
        };
    }

    public static String container(BadgeVariant variant) {
        return base() + " " + accent(variant);
    }
}
