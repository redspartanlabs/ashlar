package dev.redspartanlabs.ashlar.alert;

/**
 * The single place that maps an alert variant to Tailwind classes.
 *
 * <p>Deliberately its own tinted-banner language: Badge's bg-{color}-50/950
 * background+border pair (not Toast's white-background + left-accent-border,
 * which exists so a toast reads as a floating card stacked above the page -
 * an alert sits flat in the page's own flow and doesn't need that cue) sized
 * up from a pill into a full-width block. Icon color reuses ToastStyles'
 * exact -600/dark:-400 tokens for the same four semantic hues.
 */
public final class AlertStyles {

    private AlertStyles() {
    }

    public static String container(AlertVariant variant) {
        return "flex items-start gap-3 rounded-lg border p-4 " + tint(variant);
    }

    private static String tint(AlertVariant variant) {
        return switch (variant) {
            case INFO -> "border-sky-200 bg-sky-50 dark:border-sky-900 dark:bg-sky-950/50";
            case SUCCESS -> "border-green-200 bg-green-50 dark:border-green-900 dark:bg-green-950/50";
            case WARNING -> "border-amber-200 bg-amber-50 dark:border-amber-900 dark:bg-amber-950/50";
            case ERROR -> "border-red-200 bg-red-50 dark:border-red-900 dark:bg-red-950/50";
        };
    }

    public static String icon(AlertVariant variant) {
        return switch (variant) {
            case INFO -> "text-sky-600 dark:text-sky-400";
            case SUCCESS -> "text-green-600 dark:text-green-400";
            case WARNING -> "text-amber-600 dark:text-amber-400";
            case ERROR -> "text-red-600 dark:text-red-400";
        };
    }

    public static String title(AlertVariant variant) {
        return switch (variant) {
            case INFO -> "text-sky-800 dark:text-sky-200";
            case SUCCESS -> "text-green-800 dark:text-green-200";
            case WARNING -> "text-amber-800 dark:text-amber-200";
            case ERROR -> "text-red-800 dark:text-red-200";
        };
    }

    public static String body(AlertVariant variant) {
        return switch (variant) {
            case INFO -> "text-sky-700 dark:text-sky-300";
            case SUCCESS -> "text-green-700 dark:text-green-300";
            case WARNING -> "text-amber-700 dark:text-amber-300";
            case ERROR -> "text-red-700 dark:text-red-300";
        };
    }
}
