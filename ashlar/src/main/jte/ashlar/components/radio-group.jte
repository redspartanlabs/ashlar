@import java.util.List
@import com.redspartanlabs.ashlar.radio.RadioOption

@param String name
@param String label
@param List<RadioOption> options
@param String selectedValue = ""
@param boolean required = false
@param boolean disabled = false
@param String orientation = "vertical"
@param String id = ""
@param String helpText = ""
@param boolean error = false
@param String errorMessage = ""

<%-- A real, native radio group: a <fieldset>/<legend> wrapping one
     <input type="radio"> per option, all sharing `name`. Styled the same
     way checkbox.jte styles its own control - `accent-*` instead of
     `appearance-none`, so the browser keeps drawing its own selection
     dot, just tinted to match the library's palette - with `rounded-full`
     in place of checkbox.jte's `rounded` as the one necessary visual
     difference between the two.

     <fieldset> is doing real work here, not just semantic decoration:
     `disabled` on the fieldset natively disables every descendant radio
     at once, so whole-group-disabled needs no per-option loop of its own,
     and `<legend>` gives the group its accessible name for free - no
     aria-label/aria-labelledby needed. `required` still has to live on
     each individual radio (fieldset itself isn't a validation target),
     but per the HTML spec a same-name radio group's required constraint
     is satisfied the instant *any* member is checked, so this reads
     naturally as "the group is required" despite being expressed
     per-input. Selection, deselection, keyboard (arrow keys move *and*
     select within a native radio group), and required validation are
     all native - none of that is reimplemented here. --%>
!{String groupId = id.isEmpty() ? name : id;}
!{String helpId = groupId + "-help";}
!{String errorId = groupId + "-error";}
!{boolean hasHelp = !helpText.isEmpty();}
!{boolean showError = error && !errorMessage.isEmpty();}
!{String describedByAttr = hasHelp && showError ? helpId + " " + errorId : hasHelp ? helpId : showError ? errorId : null;}
!{boolean isHorizontal = "horizontal".equalsIgnoreCase(orientation);}
!{String optionsLayoutClasses = isHorizontal ? "flex flex-wrap gap-x-8 gap-y-3" : "flex flex-col gap-3";}

<%-- Smart attributes: a null/false expression makes JTE omit the attribute entirely. --%>
!{String ariaInvalidAttr = error ? "true" : null;}

!{String radioClasses = "mt-0.5 h-4 w-4 shrink-0 rounded-full border border-slate-300 bg-white accent-blue-600 " +
        "focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 " +
        "disabled:cursor-not-allowed disabled:opacity-60 " +
        "user-invalid:border-red-500 user-invalid:accent-red-600 " +
        "aria-invalid:border-red-500 aria-invalid:accent-red-600 " +
        "dark:border-slate-600 dark:bg-slate-800 " +
        "dark:user-invalid:border-red-500 dark:aria-invalid:border-red-500";}

<fieldset class="mb-6 min-w-0 border-0 p-0" disabled="${disabled}" aria-describedby="${describedByAttr}">
    <legend class="mb-2 block text-sm font-medium text-slate-700 dark:text-slate-300">
        ${label}
        @if (required)
            <span class="text-red-600 dark:text-red-400" aria-hidden="true"> *</span>
        @endif
    </legend>

    <div class="${optionsLayoutClasses}">
        @for (RadioOption option : options)
            !{String optionId = groupId + "-" + option.value();}
            !{boolean isSelected = option.value().equals(selectedValue);}
            !{boolean hasDescription = option.description() != null && !option.description().isEmpty();}
            !{boolean optionDisabled = disabled || option.disabled();}
            !{String optionLabelClasses = optionDisabled ? "cursor-not-allowed text-slate-400 dark:text-slate-500" : "cursor-pointer text-slate-700 dark:text-slate-300";}
            !{String optionDescriptionClasses = optionDisabled ? "text-slate-300 dark:text-slate-600" : "text-slate-500 dark:text-slate-400";}

            <div class="flex items-start gap-3">
                <input
                        type="radio"
                        id="${optionId}"
                        name="${name}"
                        value="${option.value()}"
                        checked="${isSelected}"
                        disabled="${option.disabled()}"
                        required="${required}"
                        aria-invalid="${ariaInvalidAttr}"
                        class="${radioClasses}"
                >

                <div class="flex-1">
                    <label for="${optionId}" class="text-sm font-medium ${optionLabelClasses}">${option.label()}</label>
                    @if (hasDescription)
                        <p class="mt-0.5 text-sm ${optionDescriptionClasses}">${option.description()}</p>
                    @endif
                </div>
            </div>
        @endfor
    </div>

    @if (hasHelp)
        <p id="${helpId}" class="mt-2 text-sm text-slate-500 dark:text-slate-400">${helpText}</p>
    @endif

    @if (showError)
        <p id="${errorId}" class="mt-2 text-sm text-red-600 dark:text-red-400" role="alert">${errorMessage}</p>
    @endif
</fieldset>
