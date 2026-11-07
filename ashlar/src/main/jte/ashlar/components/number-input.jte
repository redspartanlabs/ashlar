@param String name
@param String label
@param String value = ""
@param String placeholder = ""
@param String min = ""
@param String max = ""
@param String step = ""
@param boolean required = false
@param boolean disabled = false
@param boolean readonly = false
@param String id = ""
@param String helpText = ""
@param boolean error = false
@param String errorMessage = ""

<%-- A plain, native <input type="number"> - the numeric sibling of
     text-input.jte, sharing its identity/validation/help/error
     conventions and its zero-JavaScript, pure-CSS `:user-invalid`/
     `[aria-invalid="true"]` error styling (field.css). `type` isn't a param here the way it
     is on text-input.jte: this component's whole reason to exist is
     `type="number"`, so there's nothing to make configurable.

     `min`/`max`/`step` are plain Strings, not Java numeric types. Two
     reasons: the HTML attributes are themselves untyped strings that the
     browser parses and validates (a step like "0.5" is exactly as valid
     as "1"), and this component has no domain logic that would ever need
     to read them back as numbers - it only ever writes them straight
     into the markup. Parsing them into Integer/Double here would buy
     nothing and would wrongly assume every Number Input is a whole
     number, which `step="0.5"`-style fields disprove.

     Native browser spin buttons are kept rather than replaced with
     custom increment/decrement controls: they already correctly respect
     min/max/step (including decimal steps, without the floating-point
     drift a hand-rolled `value + step` would risk), are keyboard- and
     screen-reader-accessible for free, and work identically to every
     other native form control in this library. Custom spinner buttons
     would be pure visual polish for a control the browser already
     renders correctly - not worth the added JS and accessibility surface
     for v1. --%>
!{String inputId = id.isEmpty() ? name : id;}
!{String helpId = inputId + "-help";}
!{String errorId = inputId + "-error";}
!{boolean hasHelp = !helpText.isEmpty();}
!{boolean showError = error && !errorMessage.isEmpty();}
!{String describedByAttr = hasHelp && showError ? helpId + " " + errorId : hasHelp ? helpId : showError ? errorId : null;}

<%-- Smart attributes: a null/false expression makes JTE omit the attribute entirely. --%>
!{String placeholderAttr = placeholder.isEmpty() ? null : placeholder;}
!{String minAttr = min.isEmpty() ? null : min;}
!{String maxAttr = max.isEmpty() ? null : max;}
!{String stepAttr = step.isEmpty() ? null : step;}
!{String ariaInvalidAttr = error ? "true" : null;}

!{String inputClasses = "ash-field__control ash-field__control--readonly ash-field__control--narrow";}

<div class="ash-field">
    <label for="${inputId}" class="ash-field__label">
        ${label}
        @if (required)
            <span class="ash-field__required" aria-hidden="true"> *</span>
        @endif
    </label>

    <input
            type="number"
            id="${inputId}"
            name="${name}"
            value="${value}"
            placeholder="${placeholderAttr}"
            min="${minAttr}"
            max="${maxAttr}"
            step="${stepAttr}"
            required="${required}"
            disabled="${disabled}"
            readonly="${readonly}"
            aria-invalid="${ariaInvalidAttr}"
            aria-describedby="${describedByAttr}"
            class="${inputClasses}"
    >

    @if (hasHelp)
        <p id="${helpId}" class="ash-field__help">${helpText}</p>
    @endif

    @if (showError)
        <p id="${errorId}" class="ash-field__error" role="alert">${errorMessage}</p>
    @endif
</div>
