@import com.redspartanlabs.ashlar.icon.Icon
@import com.redspartanlabs.ashlar.AshlarAssets

@param String name
@param String label
@param String type = "text"
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
@param String pattern = ""
@param String autocomplete = ""
@param String inputmode = ""
@param String mask = ""
@param Icon leadingIcon = null
@param Icon trailingIcon = null

<%-- A plain, native <input> - deliberately the one component in this library
     that ships with no JavaScript and no data-* behavior contract for the
     common case. Every interactive capability here (required/disabled/
     readonly, minlength/maxlength/pattern, the browser's own validation
     bubble) is native browser behavior, not something this template
     reimplements.

     Even the error *styling* is done in pure CSS, via the
     `user-invalid:`/`aria-invalid:` Tailwind variants, rather than a
     JavaScript `invalid`-event listener like Date Picker/Select use: a
     plain <input>, unlike those two, already IS the control the user sees
     and types into, so there's no hidden native element for JS to
     synchronize a visual copy against. `user-invalid:` (not `invalid:`)
     specifically only matches after the user has interacted with the
     field or a submit was attempted, which is what keeps an untouched
     required field from rendering as an error on first paint.

     `mask` is the one deliberate, opt-in exception to "no JavaScript": a
     small named key (not a pattern DSL) into a tiny formatter registry in
     text-input.js - e.g. `mask="phone"` displays "(505) 555-1234" while
     the value actually submitted under `name` is normalized to digits
     only ("5055551234"). Nothing loads or runs unless `mask` is set, so
     every other Text Input is completely unaffected. `type`, `pattern`,
     `minlength`/`maxlength` stay independent of masking - they still
     validate whatever is currently on screen (the formatted value), same
     as any other field. --%>
!{String resolvedType = type.isEmpty() ? "text" : type;}
!{String inputId = id.isEmpty() ? name : id;}
!{String helpId = inputId + "-help";}
!{String errorId = inputId + "-error";}
!{boolean hasHelp = !helpText.isEmpty();}
!{boolean showError = error && !errorMessage.isEmpty();}
!{String describedByAttr = hasHelp && showError ? helpId + " " + errorId : hasHelp ? helpId : showError ? errorId : null;}
!{boolean hasMask = !mask.isEmpty();}
<%-- "phone" is the only built-in mask today; this default only ever
     narrows an *unset* inputmode, never overrides one the caller chose. --%>
!{String resolvedInputmode = (inputmode.isEmpty() && "phone".equals(mask)) ? "tel" : inputmode;}

<%-- Smart attributes: a null/false expression makes JTE omit the attribute entirely. --%>
!{String placeholderAttr = placeholder.isEmpty() ? null : placeholder;}
!{String patternAttr = pattern.isEmpty() ? null : pattern;}
!{String autocompleteAttr = autocomplete.isEmpty() ? null : autocomplete;}
!{String inputmodeAttr = resolvedInputmode.isEmpty() ? null : resolvedInputmode;}
!{String ariaInvalidAttr = error ? "true" : null;}
!{String maskAttr = hasMask ? mask : null;}

!{String leadingPadding = leadingIcon != null ? "pl-9" : "pl-3";}
!{String trailingPadding = trailingIcon != null ? "pr-9" : "pr-3";}
!{String inputClasses = "block w-full rounded-md border border-slate-300 bg-white py-2 " + leadingPadding + " " + trailingPadding + " text-sm text-slate-900 shadow-sm placeholder:text-slate-400 " +
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

    <div class="relative w-full max-w-sm">
        @if (leadingIcon != null)
            <span class="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3 text-slate-400 dark:text-slate-500">
                @template.ashlar.components.icon(icon = leadingIcon, classes = "h-4 w-4")
            </span>
        @endif

        <input
                type="${resolvedType}"
                id="${inputId}"
                name="${name}"
                value="${value}"
                placeholder="${placeholderAttr}"
                required="${required}"
                disabled="${disabled}"
                readonly="${readonly}"
                minlength="${minlength}"
                maxlength="${maxlength}"
                pattern="${patternAttr}"
                autocomplete="${autocompleteAttr}"
                inputmode="${inputmodeAttr}"
                aria-invalid="${ariaInvalidAttr}"
                aria-describedby="${describedByAttr}"
                data-mask="${maskAttr}"
                class="${inputClasses}"
        >

        @if (trailingIcon != null)
            <span class="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-3 text-slate-400 dark:text-slate-500">
                @template.ashlar.components.icon(icon = trailingIcon, classes = "h-4 w-4")
            </span>
        @endif
    </div>

    @if (hasHelp)
        <p id="${helpId}" class="mt-1 text-sm text-slate-500 dark:text-slate-400">${helpText}</p>
    @endif

    @if (showError)
        <p id="${errorId}" class="mt-1 text-sm text-red-600 dark:text-red-400" role="alert">${errorMessage}</p>
    @endif
</div>

@if (hasMask)
    <script type="module" src="${AshlarAssets.js("text-input.js")}"></script>
@endif
