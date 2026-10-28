@import dev.redspartanlabs.ashlar.stepper.StepperStatus
@import dev.redspartanlabs.ashlar.stepper.StepperStyles
@import dev.redspartanlabs.ashlar.icon.Icon

@param String label
@param String status

<%-- A single step: a circular indicator, a label, and (except on the last
     step) a connector trailing toward the next one. Deliberately
     non-interactive - no link, no button, no click handler - because a
     Stepper only ever shows progress the application already decided;
     letting a step be clicked would mean the component deciding on the
     application's behalf whether backward/forward navigation is allowed,
     which is exactly the boundary this component is not supposed to cross
     (see stepper.jte's own comment, and the handoff this was built from).

     The indicator's checkmark/number is `aria-hidden`: it's genuinely
     decorative here, not the only place a step's position or state is
     conveyed. Position already comes from being an `<li>` inside a real
     `<ol>` (a screen reader announces "item 2 of 4" on its own, no ARIA
     needed for that); state comes from the `sr-only` text below, not from
     the glyph. This also sidesteps a real, known accessibility gap: CSS
     generated content (which is what draws the step number here) isn't
     reliably exposed to assistive tech across browsers, so nothing
     meaningful can depend on it actually being read - it's fine as pure
     decoration, not fine as the only source of the step's number.

     The step's state is the one modifier on the `<li>` (`ash-stepper-item--completed`,
     `--current` or `--upcoming`); every owned part reads its colours from it in
     Ashlar's CSS (components/stepper-item.css). That is why the three
     StepperStyles methods below return the same part class whatever the status.

     `aria-current="step"` on the `<li>` itself, not an inner element:
     there's no link or button here for it to attach to the way
     breadcrumb-item.jte's does, and the ARIA spec allows `aria-current` on
     any element representing the current item in a set. --%>
!{StepperStatus resolvedStatus = StepperStatus.from(status);}
!{boolean isCompleted = resolvedStatus == StepperStatus.COMPLETED;}
!{String indicatorClasses = StepperStyles.indicator(resolvedStatus);}
!{String connectorClasses = StepperStyles.connector(resolvedStatus);}
!{String labelClasses = StepperStyles.label(resolvedStatus);}
!{String itemClasses = "ash-stepper-item ash-stepper-item--" + resolvedStatus.name().toLowerCase();}
!{String ariaCurrentAttr = resolvedStatus == StepperStatus.CURRENT ? "step" : null;}
!{String statusText = StepperStyles.statusText(resolvedStatus);}

<li aria-current="${ariaCurrentAttr}" class="${itemClasses}">
    <div class="ash-stepper-item__connector" aria-hidden="true">
        <div class="${connectorClasses}"></div>
    </div>

    <span aria-hidden="true" class="${indicatorClasses}">
        @if (isCompleted)
            @template.ashlar.components.icon(icon = Icon.CHECK, classes = "ash-icon--sm")
        @endif
    </span>

    <span class="ash-stepper-item__text">
        <span class="${labelClasses}">${label}<span class="ash-sr-only">, ${statusText}</span></span>
    </span>
</li>
