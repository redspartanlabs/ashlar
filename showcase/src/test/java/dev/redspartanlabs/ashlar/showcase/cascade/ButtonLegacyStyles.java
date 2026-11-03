package dev.redspartanlabs.ashlar.showcase.cascade;

import dev.redspartanlabs.ashlar.button.ButtonSize;
import dev.redspartanlabs.ashlar.button.ButtonVariant;

/**
 * TEST-ONLY. The pre-migration Tailwind class strings of Button, copied verbatim from the former ButtonStyles (the one source of truth
 * for what "legacy" means). The Tailwind oracle scans this file so these classes stay compiled, and ButtonLegacyMarkup rewrites
 * migrated Buttons back to the strings this class returns.
 */
final class ButtonLegacyStyles {

    private ButtonLegacyStyles() {
    }

    /** Classes shared by every button regardless of variant/size. */
    private static String base() {
        return "inline-flex items-center justify-center gap-2 rounded-md font-medium " +
                "transition-colors duration-150 ease-in-out cursor-pointer select-none " +
                "focus:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 " +
                "disabled:cursor-not-allowed disabled:opacity-60 " +
                "aria-disabled:cursor-not-allowed aria-disabled:opacity-60";
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
    static String iconSizeClasses(ButtonSize size) {
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
                    "focus-visible:ring-blue-500 disabled:hover:bg-blue-600 aria-disabled:hover:bg-blue-600";
            case SECONDARY -> "bg-white text-slate-700 border border-slate-300 shadow-sm hover:bg-slate-50 " +
                    "active:bg-slate-100 focus-visible:ring-blue-500 disabled:hover:bg-white aria-disabled:hover:bg-white " +
                    "dark:bg-slate-800 dark:text-slate-200 dark:border-slate-600 dark:hover:bg-slate-700 " +
                    "dark:active:bg-slate-600 dark:disabled:hover:bg-slate-800 dark:aria-disabled:hover:bg-slate-800";
            case SUCCESS -> "bg-green-600 text-white shadow-sm hover:bg-green-700 active:bg-green-800 " +
                    "focus-visible:ring-green-500 disabled:hover:bg-green-600 aria-disabled:hover:bg-green-600";
            case WARNING -> "bg-amber-500 text-white shadow-sm hover:bg-amber-600 active:bg-amber-700 " +
                    "focus-visible:ring-amber-500 disabled:hover:bg-amber-500 aria-disabled:hover:bg-amber-500";
            case DANGER -> "bg-red-600 text-white shadow-sm hover:bg-red-700 active:bg-red-800 " +
                    "focus-visible:ring-red-500 disabled:hover:bg-red-600 aria-disabled:hover:bg-red-600";
            case INFO -> "bg-sky-600 text-white shadow-sm hover:bg-sky-700 active:bg-sky-800 " +
                    "focus-visible:ring-sky-500 disabled:hover:bg-sky-600 aria-disabled:hover:bg-sky-600";
            case GHOST -> "bg-transparent text-slate-700 hover:bg-slate-100 active:bg-slate-200 " +
                    "focus-visible:ring-blue-500 disabled:hover:bg-transparent aria-disabled:hover:bg-transparent " +
                    "dark:text-slate-300 dark:hover:bg-slate-800 dark:active:bg-slate-700";
            case LINK -> "bg-transparent text-blue-600 shadow-none underline-offset-4 hover:underline " +
                    "hover:text-blue-700 active:text-blue-800 focus-visible:ring-blue-500 disabled:hover:no-underline aria-disabled:hover:no-underline " +
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
    static String classes(ButtonVariant variant, ButtonSize size, boolean iconOnly, boolean fullWidth, boolean pressed) {
        String widthClass = fullWidth ? " w-full" : "";
        String variantClasses = pressed ? pressedVariant(variant) : ButtonLegacyStyles.variant(variant);
        return base() + " " + size(size, iconOnly) + " " + variantClasses + widthClass;
    }

    /** Exposed so the toggle-button script can read the two class sets to swap between. */
    static String restingClasses(ButtonVariant variant, ButtonSize size, boolean iconOnly, boolean fullWidth) {
        return classes(variant, size, iconOnly, fullWidth, false);
    }

    static String pressedClasses(ButtonVariant variant, ButtonSize size, boolean iconOnly, boolean fullWidth) {
        return classes(variant, size, iconOnly, fullWidth, true);
    }

}
