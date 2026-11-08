@import java.util.List
@import dev.redspartanlabs.ashlar.radio.RadioOption

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
     way checkbox.jte styles its own control - `accent-color` instead of
     `appearance: none`, so the browser keeps drawing its own selection
     dot, just tinted to match the library's palette - both through the
     shared choice.css.

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
!{String optionsLayoutClasses = isHorizontal ? "ash-radio-group__options ash-radio-group__options--horizontal" : "ash-radio-group__options";}

<%-- Smart attributes: a null/false expression makes JTE omit the attribute entirely. --%>
!{String ariaInvalidAttr = error ? "true" : null;}

<fieldset class="ash-radio-group" disabled="${disabled}" aria-describedby="${describedByAttr}">
    <legend class="ash-radio-group__legend">
        ${label}
        @if (required)
            <span class="ash-field__required" aria-hidden="true"> *</span>
        @endif
    </legend>

    <div class="${optionsLayoutClasses}">
        @for (RadioOption option : options)
            !{String optionId = groupId + "-" + option.value();}
            !{boolean isSelected = option.value().equals(selectedValue);}
            !{boolean hasDescription = option.description() != null && !option.description().isEmpty();}

            <div class="ash-choice">
                <input
                        type="radio"
                        id="${optionId}"
                        name="${name}"
                        value="${option.value()}"
                        checked="${isSelected}"
                        disabled="${option.disabled()}"
                        required="${required}"
                        aria-invalid="${ariaInvalidAttr}"
                        class="ash-choice__input"
                >

                <div class="ash-choice__body">
                    <label for="${optionId}" class="ash-choice__label">${option.label()}</label>
                    @if (hasDescription)
                        <p class="ash-choice__description">${option.description()}</p>
                    @endif
                </div>
            </div>
        @endfor
    </div>

    @if (hasHelp)
        <p id="${helpId}" class="ash-choice__description ash-radio-group__help">${helpText}</p>
    @endif

    @if (showError)
        <p id="${errorId}" class="ash-choice__error ash-radio-group__error" role="alert">${errorMessage}</p>
    @endif
</fieldset>
