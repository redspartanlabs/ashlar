package com.redspartan.ashlar.button;

/**
 * The single place that maps a button's variant/size/state to Tailwind
 * utility classes.
 *
 * <p>To add a new variant: add a constant to {@link ButtonVariant}, then add
 * a matching {@code case} to both {@link #variant} and {@link #pressedVariant}
 * below. Nothing else needs to change - the JTE template and JavaScript both
 * read classes generically rather than knowing about individual variants.
 */
public final class ButtonStyles {

    private ButtonStyles() {
    }

    /** Classes shared by every button regardless of variant/size. */
    private static String base() {
        return "inline-flex items-center justify-center gap-2 rounded-md font-medium " +
                "transition-colors duration-150 ease-in-out cursor-pointer select-none " +
                "focus:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 " +
                "disabled:cursor-not-allowed disabled:opacity-60";
    }

    private static String size(ButtonSize size, boolean iconOnly) {
        return switch (size) {
            case XSMALL -> iconOnly ? "h-6 w-6 text-xs" : "h-6 px-2 text-xs";
            case SMALL -> iconOnly ? "h-8 w-8 text-sm" : "h-8 px-3 text-sm";
            case MEDIUM -> iconOnly ? "h-10 w-10 text-sm" : "h-10 px-4 text-sm";
            case LARGE -> iconOnly ? "h-12 w-12 text-base" : "h-12 px-6 text-base";
            case XLARGE -> iconOnly ? "h-14 w-14 text-lg" : "h-14 px-8 text-lg";
        };
    }

    /** Icon size paired to each button size, so icons scale with the button. */
    public static String iconSizeClasses(ButtonSize size) {
        return switch (size) {
            case XSMALL -> "h-3 w-3";
            case SMALL -> "h-4 w-4";
            case MEDIUM -> "h-4 w-4";
            case LARGE -> "h-5 w-5";
            case XLARGE -> "h-6 w-6";
        };
    }

    /** The button's normal (not pressed) appearance for a variant. */
    private static String variant(ButtonVariant variant) {
        return switch (variant) {
            case PRIMARY -> "bg-blue-600 text-white shadow-sm hover:bg-blue-700 active:bg-blue-800 " +
                    "focus-visible:ring-blue-500 disabled:hover:bg-blue-600";
            case SECONDARY -> "bg-white text-slate-700 border border-slate-300 shadow-sm hover:bg-slate-50 " +
                    "active:bg-slate-100 focus-visible:ring-blue-500 disabled:hover:bg-white " +
                    "dark:bg-slate-800 dark:text-slate-200 dark:border-slate-600 dark:hover:bg-slate-700 " +
                    "dark:active:bg-slate-600 dark:disabled:hover:bg-slate-800";
            case SUCCESS -> "bg-green-600 text-white shadow-sm hover:bg-green-700 active:bg-green-800 " +
                    "focus-visible:ring-green-500 disabled:hover:bg-green-600";
            case WARNING -> "bg-amber-500 text-white shadow-sm hover:bg-amber-600 active:bg-amber-700 " +
                    "focus-visible:ring-amber-500 disabled:hover:bg-amber-500";
            case DANGER -> "bg-red-600 text-white shadow-sm hover:bg-red-700 active:bg-red-800 " +
                    "focus-visible:ring-red-500 disabled:hover:bg-red-600";
            case INFO -> "bg-sky-600 text-white shadow-sm hover:bg-sky-700 active:bg-sky-800 " +
                    "focus-visible:ring-sky-500 disabled:hover:bg-sky-600";
            case GHOST -> "bg-transparent text-slate-700 hover:bg-slate-100 active:bg-slate-200 " +
                    "focus-visible:ring-blue-500 disabled:hover:bg-transparent " +
                    "dark:text-slate-300 dark:hover:bg-slate-800 dark:active:bg-slate-700";
            case LINK -> "bg-transparent text-blue-600 shadow-none underline-offset-4 hover:underline " +
                    "hover:text-blue-700 active:text-blue-800 focus-visible:ring-blue-500 disabled:hover:no-underline " +
                    "dark:text-blue-400 dark:hover:text-blue-300 dark:active:text-blue-200";
        };
    }

    /** The button's appearance while toggled "on" (aria-pressed="true"). */
    private static String pressedVariant(ButtonVariant variant) {
        return switch (variant) {
            case PRIMARY -> "bg-blue-800 text-white shadow-inner focus-visible:ring-blue-500";
            case SECONDARY -> "bg-slate-200 text-slate-900 border border-slate-300 shadow-inner focus-visible:ring-blue-500 " +
                    "dark:bg-slate-600 dark:text-slate-100 dark:border-slate-500";
            case SUCCESS -> "bg-green-800 text-white shadow-inner focus-visible:ring-green-500";
            case WARNING -> "bg-amber-700 text-white shadow-inner focus-visible:ring-amber-500";
            case DANGER -> "bg-red-800 text-white shadow-inner focus-visible:ring-red-500";
            case INFO -> "bg-sky-800 text-white shadow-inner focus-visible:ring-sky-500";
            case GHOST -> "bg-slate-200 text-slate-900 shadow-none focus-visible:ring-blue-500 " +
                    "dark:bg-slate-700 dark:text-slate-100";
            case LINK -> "bg-transparent text-blue-800 shadow-none underline focus-visible:ring-blue-500 " +
                    "dark:text-blue-300";
        };
    }

    /** Full class string for a button in its resting or pressed state. */
    public static String classes(ButtonVariant variant, ButtonSize size, boolean iconOnly, boolean fullWidth, boolean pressed) {
        String widthClass = fullWidth ? " w-full" : "";
        String variantClasses = pressed ? pressedVariant(variant) : ButtonStyles.variant(variant);
        return base() + " " + size(size, iconOnly) + " " + variantClasses + widthClass;
    }

    /** Exposed so the toggle-button script can read the two class sets to swap between. */
    public static String restingClasses(ButtonVariant variant, ButtonSize size, boolean iconOnly, boolean fullWidth) {
        return classes(variant, size, iconOnly, fullWidth, false);
    }

    public static String pressedClasses(ButtonVariant variant, ButtonSize size, boolean iconOnly, boolean fullWidth) {
        return classes(variant, size, iconOnly, fullWidth, true);
    }

    /** Defends against an empty/invalid HTML button type silently becoming "submit". */
    public static String normalizeButtonType(String value) {
        if (value == null) {
            return "button";
        }
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "submit", "reset" -> normalized;
            default -> "button";
        };
    }
}
