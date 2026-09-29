package com.redspartanlabs.ashlar.toast;

/**
 * The single place that maps a toast variant to Tailwind classes.
 *
 * <p>Visibility is driven by the {@code data-toast-state} attribute
 * ({@code hidden | closed | open}) rather than by classes toast.js adds and
 * removes: the {@code data-[toast-state=...]:} variants below mean JavaScript
 * only ever changes that one attribute, and never needs its own copy of any
 * class name.
 */
public final class ToastStyles {

    private ToastStyles() {
    }

    public static String container(ToastVariant variant) {
        return "pointer-events-auto flex w-full items-start gap-3 rounded-lg border border-l-4 border-slate-200 bg-white p-4 shadow-lg " +
                "dark:border-slate-700 dark:bg-slate-800 " +
                "translate-x-4 opacity-0 transition duration-200 ease-out motion-reduce:transition-none " +
                // touch-pan-y: lets the browser claim a vertical touch gesture as page
                // scroll natively, so toast.js only ever has to handle horizontal drag.
                "touch-pan-y " +
                "data-[toast-state=open]:translate-x-0 data-[toast-state=open]:opacity-100 data-[toast-state=hidden]:hidden " +
                accent(variant);
    }

    // Each accent is repeated under dark: on purpose. dark:border-slate-700 on
    // the container sets all four sides and, as a variant rule, is emitted after
    // every base utility - so without a dark: accent of its own, the left accent
    // is silently overridden in dark mode.
    private static String accent(ToastVariant variant) {
        return switch (variant) {
            case SUCCESS -> "border-l-green-500 dark:border-l-green-500";
            case INFO -> "border-l-sky-500 dark:border-l-sky-500";
            case WARNING -> "border-l-amber-500 dark:border-l-amber-500";
            case DANGER -> "border-l-red-500 dark:border-l-red-500";
            case NEUTRAL -> "border-l-slate-400 dark:border-l-slate-500";
        };
    }

    public static String icon(ToastVariant variant) {
        return switch (variant) {
            case SUCCESS -> "text-green-600 dark:text-green-400";
            case INFO -> "text-sky-600 dark:text-sky-400";
            case WARNING -> "text-amber-600 dark:text-amber-400";
            case DANGER -> "text-red-600 dark:text-red-400";
            case NEUTRAL -> "text-slate-500 dark:text-slate-400";
        };
    }
}
