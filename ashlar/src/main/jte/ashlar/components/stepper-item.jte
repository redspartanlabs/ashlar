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

     `aria-current="step"` on the `<li>` itself, not an inner element:
     there's no link or button here for it to attach to the way
     breadcrumb-item.jte's does, and the ARIA spec allows `aria-current` on
     any element representing the current item in a set. --%>
!{StepperStatus resolvedStatus = StepperStatus.from(status);}
!{boolean isCompleted = resolvedStatus == StepperStatus.COMPLETED;}
!{String indicatorClasses = StepperStyles.indicator(resolvedStatus);}
!{String connectorClasses = StepperStyles.connector(resolvedStatus);}
!{String labelClasses = StepperStyles.label(resolvedStatus);}
!{String numberClasses = isCompleted ? "" : "before:content-[counter(step)]";}
!{String ariaCurrentAttr = resolvedStatus == StepperStatus.CURRENT ? "step" : null;}
!{String statusText = StepperStyles.statusText(resolvedStatus);}

<li aria-current="${ariaCurrentAttr}" class="group relative flex min-w-0 flex-1 items-start gap-3 [counter-increment:step] @sm:flex-col @sm:items-center @sm:gap-2">
    <div class="absolute left-0 right-0 top-4 hidden items-center @sm:flex @sm:group-last:hidden" aria-hidden="true">
        <div class="h-0.5 w-full ${connectorClasses}"></div>
    </div>

    <span aria-hidden="true" class="relative z-10 flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-sm font-semibold ${indicatorClasses} ${numberClasses}">
        @if (isCompleted)
            @template.ashlar.components.icon(icon = Icon.CHECK, classes = "h-4 w-4")
        @endif
    </span>

    <span class="flex min-w-0 flex-col pt-1 @sm:items-center @sm:pt-0 @sm:text-center">
        <span class="break-words text-sm ${labelClasses}">${label}<span class="sr-only">, ${statusText}</span></span>
    </span>
</li>
