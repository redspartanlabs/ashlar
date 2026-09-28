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
     conventions and its zero-JavaScript, pure-CSS `user-invalid:`/
     `aria-invalid:` error styling. `type` isn't a param here the way it
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

!{String inputClasses = "block w-full max-w-sm rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm placeholder:text-slate-400 " +
        "hover:border-slate-400 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500 " +
        "disabled:cursor-not-allowed disabled:border-slate-200 disabled:bg-slate-50 disabled:text-slate-400 " +
        "read-only:cursor-default read-only:bg-slate-50 read-only:hover:border-slate-300 " +
        "user-invalid:border-red-500 user-invalid:hover:border-red-600 user-invalid:focus:border-red-500 user-invalid:focus:ring-red-500 " +
        "aria-invalid:border-red-500 aria-invalid:hover:border-red-600 aria-invalid:focus:border-red-500 aria-invalid:focus:ring-red-500 " +
        "dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:hover:border-slate-500 " +
        "dark:disabled:border-slate-700 dark:disabled:bg-slate-800 dark:disabled:text-slate-500 " +
        "dark:read-only:bg-slate-900 dark:read-only:hover:border-slate-600 " +
        "dark:user-invalid:border-red-500 dark:aria-invalid:border-red-500";}

<div class="mb-6">
    <label for="${inputId}" class="mb-2 block text-sm font-medium text-slate-700 dark:text-slate-300">
        ${label}
        @if (required)
            <span class="text-red-600 dark:text-red-400" aria-hidden="true"> *</span>
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
        <p id="${helpId}" class="mt-1 text-sm text-slate-500 dark:text-slate-400">${helpText}</p>
    @endif

    @if (showError)
        <p id="${errorId}" class="mt-1 text-sm text-red-600 dark:text-red-400" role="alert">${errorMessage}</p>
    @endif
</div>
