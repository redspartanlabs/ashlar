@import com.redspartanlabs.ashlar.AshlarAssets

@param String name
@param String label
@param String value = ""
@param String placeholder = ""
@param boolean required = false
@param boolean disabled = false
@param boolean readonly = false
@param String id = ""
@param String helpText = ""
@param boolean error = false
@param String errorMessage = ""
@param Integer minlength = null
@param Integer maxlength = null
@param String autocomplete = ""
@param int rows = 4
@param boolean resizable = true
@param boolean showCharacterCount = false

<%-- The multiline sibling of text-input.jte - same identity/validation/help/
     error conventions, native browser behavior wherever the two elements
     share it, and text-input.jte's already-established pattern of styling
     the error state in pure CSS (`user-invalid:`/`aria-invalid:`) rather
     than a JS `invalid` listener.

     Two attributes text-input.jte accepts are deliberately absent here:
     `type` (a <textarea> has no type variants) and `pattern` (a <textarea>
     has no `pattern` attribute at all per the HTML spec - there is no
     native mechanism to recreate, so this component doesn't invent one
     either). Icons and `mask` are likewise absent: neither reads
     naturally against a multiline box.

     `showCharacterCount` is the one opt-in exception to "no JavaScript",
     mirroring how text-input.jte gates its own script behind `mask`: nothing
     loads or runs unless a caller asks for the counter, so a plain Textarea
     stays exactly as JS-free as a plain Text Input. --%>
!{String textareaId = id.isEmpty() ? name : id;}
!{String helpId = textareaId + "-help";}
!{String errorId = textareaId + "-error";}
!{String countId = textareaId + "-count";}
!{boolean hasHelp = !helpText.isEmpty();}
!{boolean showError = error && !errorMessage.isEmpty();}

<%-- Joining conditionally-present ids this way (rather than a nested
     ternary per combination) is what keeps this readable now that there
     are three optional describedby sources instead of text-input.jte's two. --%>
!{String helpPart = hasHelp ? helpId : "";}
!{String errorPart = showError ? errorId : "";}
!{String countPart = showCharacterCount ? countId : "";}
!{String describedByJoined = (helpPart + " " + errorPart + " " + countPart).trim().replaceAll(" +", " ");}
!{String describedByAttr = describedByJoined.isEmpty() ? null : describedByJoined;}

!{int initialCount = value.length();}
!{String counterText = maxlength != null ? initialCount + " / " + maxlength : initialCount + " characters";}

<%-- Smart attributes: a null/false expression makes JTE omit the attribute entirely. --%>
!{String placeholderAttr = placeholder.isEmpty() ? null : placeholder;}
!{String autocompleteAttr = autocomplete.isEmpty() ? null : autocomplete;}
!{String ariaInvalidAttr = error ? "true" : null;}
!{String counterMarkerAttr = showCharacterCount ? "true" : null;}

!{String resizeClass = resizable ? "resize-y" : "resize-none";}
!{String textareaClasses = "block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm placeholder:text-slate-400 " + resizeClass + " " +
        "hover:border-slate-400 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500 " +
        "disabled:cursor-not-allowed disabled:border-slate-200 disabled:bg-slate-50 disabled:text-slate-400 disabled:resize-none " +
        "read-only:cursor-default read-only:bg-slate-50 read-only:hover:border-slate-300 " +
        "user-invalid:border-red-500 user-invalid:hover:border-red-600 user-invalid:focus:border-red-500 user-invalid:focus:ring-red-500 " +
        "aria-invalid:border-red-500 aria-invalid:hover:border-red-600 aria-invalid:focus:border-red-500 aria-invalid:focus:ring-red-500 " +
        "dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:hover:border-slate-500 " +
        "dark:disabled:border-slate-700 dark:disabled:bg-slate-800 dark:disabled:text-slate-500 " +
        "dark:read-only:bg-slate-900 dark:read-only:hover:border-slate-600 " +
        "dark:user-invalid:border-red-500 dark:aria-invalid:border-red-500";}

<div class="mb-6" data-textarea-counter="${counterMarkerAttr}">
    <label for="${textareaId}" class="mb-2 block text-sm font-medium text-slate-700 dark:text-slate-300">
        ${label}
        @if (required)
            <span class="text-red-600 dark:text-red-400" aria-hidden="true"> *</span>
        @endif
    </label>

    <%-- No max-width cap here (unlike text-input.jte's max-w-sm): a
         multiline field benefits from whatever width the caller's own
         layout gives it, rather than being boxed to a short-text-field
         width by default. --%>
    <textarea
            id="${textareaId}"
            name="${name}"
            placeholder="${placeholderAttr}"
            required="${required}"
            disabled="${disabled}"
            readonly="${readonly}"
            minlength="${minlength}"
            maxlength="${maxlength}"
            autocomplete="${autocompleteAttr}"
            rows="${rows}"
            aria-invalid="${ariaInvalidAttr}"
            aria-describedby="${describedByAttr}"
            class="${textareaClasses}"
    >${value}</textarea>

    @if (hasHelp || showCharacterCount)
        <div class="mt-1 flex items-start justify-between gap-2">
            @if (hasHelp)
                <p id="${helpId}" class="text-sm text-slate-500 dark:text-slate-400">${helpText}</p>
            @endif
            @if (showCharacterCount)
                <span id="${countId}" data-textarea-count class="shrink-0 text-xs text-slate-500 dark:text-slate-400">${counterText}</span>
            @endif
        </div>
    @endif

    @if (showError)
        <p id="${errorId}" class="mt-1 text-sm text-red-600 dark:text-red-400" role="alert">${errorMessage}</p>
    @endif
</div>

@if (showCharacterCount)
    <script type="module" src="${AshlarAssets.js("textarea.js")}"></script>
@endif
