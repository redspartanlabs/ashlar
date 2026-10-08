package dev.redspartanlabs.ashlar.stepper;

/**
 * The single place that maps a step's {@link StepperStatus} to its Tailwind
 * classes and accessible status text, the same division of labor as
 * {@code BadgeStyles} - the enum says what a step *is*, this says how that
 * looks and reads.
 */
public final class StepperStyles {

    private StepperStyles() {
    }

    /** The circular step indicator. Completed vs. upcoming differ by more
     *  than color alone - a solid fill vs. a hollow ring, a checkmark icon
     *  vs. a plain number - so the distinction still reads in grayscale or
     *  to someone who can't perceive the color difference. */
    public static String indicator(StepperStatus status) {
        return switch (status) {
            case COMPLETED -> "border-2 border-blue-600 bg-blue-600 text-white " +
                    "dark:border-blue-500 dark:bg-blue-500";
            case CURRENT -> "border-2 border-blue-600 bg-white text-blue-600 " +
                    "dark:border-blue-500 dark:bg-slate-900 dark:text-blue-400";
            case UPCOMING -> "border-2 border-slate-300 bg-white text-slate-400 " +
                    "dark:border-slate-600 dark:bg-slate-900 dark:text-slate-500";
        };
    }

    /** The connector trailing this step, toward the next one. Belongs to
     *  the step it leads away from - a completed step's own trailing
     *  connector reads as "the path so far is done," independent of what
     *  the next step's status is. */
    public static String connector(StepperStatus status) {
        return status == StepperStatus.COMPLETED
                ? "bg-blue-600 dark:bg-blue-500"
                : "bg-slate-200 dark:bg-slate-700";
    }

    public static String label(StepperStatus status) {
        return switch (status) {
            case COMPLETED -> "text-slate-700 dark:text-slate-300";
            case CURRENT -> "font-semibold text-slate-900 dark:text-slate-100";
            case UPCOMING -> "text-slate-400 dark:text-slate-500";
        };
    }

    /** What a screen reader announces for this step beyond its visible
     *  label - a bare checkmark icon (or a plain number that looks
     *  identical for CURRENT and UPCOMING) carries no state on its own once
     *  announced without color. */
    public static String statusText(StepperStatus status) {
        return switch (status) {
            case COMPLETED -> "Completed";
            case CURRENT -> "Current step";
            case UPCOMING -> "Not started";
        };
    }
}
