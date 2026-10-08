@import dev.redspartanlabs.ashlar.icon.Icon
@import dev.redspartanlabs.ashlar.AshlarAssets

@param String startName
@param String endName
@param String label
@param String startLabel = "Start Date"
@param String endLabel = "End Date"
@param String startValue = ""
@param String endValue = ""
@param String startPlaceholder = "Select a date"
@param String endPlaceholder = "Select a date"
@param String minDate = ""
@param String maxDate = ""
@param boolean required = false
@param boolean disabled = false
@param String id = ""
@param String helpText = ""
@param boolean error = false
@param String errorMessage = ""

<%-- Two independent, completely unmodified date-picker.jte instances inside
     one <fieldset>/<legend> - the same "group a couple of related native
     controls under one shared label" structure radio-group.jte already
     established. This component owns exactly one thing neither individual
     Date Picker can own by itself: the *relationship* between the two
     dates. Each Date Picker still owns its own calendar, its own required/
     disabled state, its own value.

     Two small, additive changes to date-picker.js/.jte's shared
     infrastructure made this possible without duplicating any calendar
     logic: its hidden input now fires a real 'change' event when a date is
     picked or cleared (matching select.js/combobox.js's own hidden
     controls, which already did this), and its min/max are re-read live
     instead of cached once at init. date-range-picker.js uses exactly
     those two hooks - selecting a start date raises the end picker's own
     `min`, and vice versa, using date-picker's own existing disabled-date
     rendering to make an out-of-order pick simply unavailable, rather
     than something this component has to detect and reject after the
     fact. A submit-time check is the remaining defense in depth, for a
     range that arrived already invalid (e.g. bad initial server data)
     rather than one assembled through the calendar. --%>
!{String rangeId = id.isEmpty() ? startName + "-" + endName : id;}
!{String helpId = rangeId + "-help";}
!{String errorId = rangeId + "-error";}
!{boolean hasHelp = !helpText.isEmpty();}
!{boolean showError = error && !errorMessage.isEmpty();}
!{String describedByAttr = hasHelp && showError ? helpId + " " + errorId : hasHelp ? helpId : showError ? errorId : null;}
!{String resolvedErrorMessage = errorMessage.isEmpty() ? "End date must be on or after the start date." : errorMessage;}

<fieldset class="mb-6 min-w-0 border-0 p-0" disabled="${disabled}" aria-describedby="${describedByAttr}" data-date-range-picker>
    <legend class="mb-2 block text-sm font-medium text-slate-700 dark:text-slate-300">
        ${label}
        @if (required)
            <span class="text-red-600 dark:text-red-400" aria-hidden="true"> *</span>
        @endif
    </legend>

    <%-- The connector's vertical position can't be a fixed pixel guess, nor
         can it stretch to fill its column's leftover space (flex-1 there
         would include date-picker.jte's own trailing mb-6, dragging the
         centered icon down by half of that margin - exactly the bug this
         replaced). Instead: an invisible spacer mirrors the real label's
         box height, and the row below it copies the trigger button's own
         border/py-2 box model - the classes that actually establish its
         height - rather than trying to measure or stretch to match it.

         That still isn't quite enough on its own: the trigger's real text
         ("Sep 1, 2026") establishes a full line box, but this row's only
         content is a 16px icon, so - even with identical padding/border -
         a flex row sizes to its tallest item, not to line-height (and
         with global border-box sizing, `min-height` constrains the outer
         box, which is already taller than one line, so it can't stand in
         here either). The zero-width invisible span below reproduces the
         trigger's actual mechanism instead of fighting it: real text
         content that forces the same line box, with `w-0 overflow-hidden`
         so it contributes height without taking up any horizontal space
         `justify-center` could otherwise be centering it into. --%>
    <div class="flex flex-col gap-4 sm:flex-row">
        <div class="min-w-0 sm:flex-1" data-date-range-start>
            @template.ashlar.components.date-picker(
            name = startName,
            label = startLabel,
            value = startValue,
            placeholder = startPlaceholder,
            minDate = minDate,
            maxDate = maxDate,
            disabled = disabled,
            required = required
            )
        </div>

        <div class="hidden shrink-0 flex-col sm:flex" aria-hidden="true">
            <span class="invisible mb-2 block text-sm">&nbsp;</span>
            <div class="flex items-center justify-center border border-transparent py-2 text-slate-400 dark:text-slate-500">
                <span class="invisible w-0 overflow-hidden">&nbsp;</span>
                @template.ashlar.components.icon(icon = Icon.CHEVRON_RIGHT, classes = "h-4 w-4")
            </div>
        </div>

        <div class="min-w-0 sm:flex-1" data-date-range-end>
            @template.ashlar.components.date-picker(
            name = endName,
            label = endLabel,
            value = endValue,
            placeholder = endPlaceholder,
            minDate = minDate,
            maxDate = maxDate,
            disabled = disabled,
            required = required
            )
        </div>
    </div>

    @if (hasHelp)
        <p id="${helpId}" class="mt-1 text-sm text-slate-500 dark:text-slate-400">${helpText}</p>
    @endif

    <p id="${errorId}" data-date-range-error class="mt-1 ${showError ? "" : "hidden"} text-sm text-red-600 dark:text-red-400" role="alert">${resolvedErrorMessage}</p>
</fieldset>

<script type="module" src="${AshlarAssets.js("date-range-picker.js")}"></script>
