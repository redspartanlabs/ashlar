@import com.redspartanlabs.ashlar.AshlarAssets

@param String name
@param String label
@param String value = "on"
@param boolean checked = false
@param boolean disabled = false
@param boolean required = false
@param boolean indeterminate = false
@param String id = ""
@param String helpText = ""
@param boolean error = false
@param String errorMessage = ""

<%-- A real <input type="checkbox">, styled via `accent-*` rather than
     `appearance-none` plus a hand-drawn checkmark - the browser keeps
     drawing its own check/indeterminate mark, just tinted to match the
     library's palette. That's what keeps this a native control end to
     end: native focus ring, native Space-to-toggle, native "checkedness"
     constraint validation for `required`, and the same pure-CSS
     `user-invalid:`/`aria-invalid:` error styling text-input.jte and
     textarea.jte already use, with no JS `invalid` listener needed here
     either.

     `readonly` is deliberately not a param: it has no effect on a
     checkbox per the HTML spec (only text-entry-like controls respect
     it), so exposing it would be a false affordance - a checkbox that
     looks like it should resist toggling but doesn't.

     `indeterminate` is the one genuine exception to "no JavaScript": it
     is a DOM/IDL property with no HTML attribute equivalent at all, so
     there is no way to server-render it directly. checkbox.js sets it
     once on load for any checkbox that asks for it; the browser takes
     over completely from there, including automatically clearing it back
     to false the moment a user actually toggles the box - nothing here
     needs to track that transition itself. --%>
!{String checkboxId = id.isEmpty() ? name : id;}
!{String helpId = checkboxId + "-help";}
!{String errorId = checkboxId + "-error";}
!{boolean hasHelp = !helpText.isEmpty();}
!{boolean showError = error && !errorMessage.isEmpty();}
!{String describedByAttr = hasHelp && showError ? helpId + " " + errorId : hasHelp ? helpId : showError ? errorId : null;}

<%-- Smart attributes: a null/false expression makes JTE omit the attribute entirely. --%>
!{String ariaInvalidAttr = error ? "true" : null;}
!{String indeterminateAttr = indeterminate ? "true" : null;}

!{String checkboxClasses = "mt-0.5 h-4 w-4 shrink-0 cursor-pointer rounded border border-slate-300 bg-white accent-blue-600 " +
        "focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 " +
        "disabled:cursor-not-allowed disabled:opacity-60 " +
        "user-invalid:border-red-500 user-invalid:accent-red-600 " +
        "aria-invalid:border-red-500 aria-invalid:accent-red-600 " +
        "dark:border-slate-600 dark:bg-slate-800 " +
        "dark:user-invalid:border-red-500 dark:aria-invalid:border-red-500";}

<div class="mb-4">
    <div class="flex items-start gap-3">
        <input
                type="checkbox"
                id="${checkboxId}"
                name="${name}"
                value="${value}"
                checked="${checked}"
                disabled="${disabled}"
                required="${required}"
                aria-invalid="${ariaInvalidAttr}"
                aria-describedby="${describedByAttr}"
                data-checkbox-indeterminate="${indeterminateAttr}"
                class="${checkboxClasses}"
        >

        <div class="flex-1">
            <label for="${checkboxId}" class="cursor-pointer text-sm font-medium text-slate-700 dark:text-slate-300">
                ${label}
                @if (required)
                    <span class="text-red-600 dark:text-red-400" aria-hidden="true"> *</span>
                @endif
            </label>

            @if (hasHelp)
                <p id="${helpId}" class="mt-0.5 text-sm text-slate-500 dark:text-slate-400">${helpText}</p>
            @endif

            @if (showError)
                <p id="${errorId}" class="mt-0.5 text-sm text-red-600 dark:text-red-400" role="alert">${errorMessage}</p>
            @endif
        </div>
    </div>
</div>

@if (indeterminate)
    <script type="module" src="${AshlarAssets.js("checkbox.js")}"></script>
@endif
