package dev.redspartanlabs.ashlar.stepper;

/**
 * The single place that maps a step's {@link StepperStatus} to its CSS classes and accessible status text, the same division of labor as
 * {@code BadgeStyles} - the enum says what a step *is*, this says how that looks and reads.
 *
 * <p>The classes are Ashlar's own, styled by {@code components/stepper-item.css} in the generated {@code ashlar.css}; they no longer
 * depend on Tailwind. A step's state is expressed once, as a modifier on the {@code <li>} that stepper-item.jte writes
 * ({@code ash-stepper-item--completed}, {@code --current} or {@code --upcoming}); the indicator, the connector's line and the label read
 * their colours from it. So the three class methods below name the part, and return the same class whatever the status - the status is
 * still a parameter because that is the method's public shape, and it is how the part's meaning (which a caller of this helper may
 * want) stays tied to a step's state.
 */
public final class StepperStyles {

    private StepperStyles() {
    }

    /** The circular step indicator. Completed vs. upcoming differ by more than color alone - a solid fill vs. a hollow ring, a checkmark
     *  icon vs. a plain number - so the distinction still reads in grayscale or to someone who can't perceive the color difference. */
    public static String indicator(StepperStatus status) {
        return "ash-stepper-item__indicator";
    }

    /** The line trailing this step, toward the next one. Belongs to the step it leads away from - a completed step's own trailing line
     *  reads as "the path so far is done," independent of what the next step's status is. */
    public static String connector(StepperStatus status) {
        return "ash-stepper-item__line";
    }

    public static String label(StepperStatus status) {
        return "ash-stepper-item__label";
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
