@import com.redspartanlabs.ashlar.icon.Icon
@import com.redspartanlabs.ashlar.AshlarAssets

@param String name
@param String label
@param String value = ""
@param String placeholder = "Search..."
@param boolean required = false
@param boolean disabled = false
@param String id = ""
@param String helpText = ""
@param boolean error = false
@param String errorMessage = ""

<%-- A specialized, narrower text-input.jte: real <input type="search">
     (the correct native semantics for this - browsers expose it as an
     implicit `searchbox`, not a generic `textbox`, and it's what lets
     some browsers' own Escape-to-clear behavior apply for free), a
     decorative leading search icon reusing text-input.jte's own
     leading-icon slot pattern, and a real clear <button> built in rather
     than offered as a configurable icon slot - a search field without a
     way to clear it isn't really a search field, so unlike Select's
     `clearable` flag this isn't optional configuration.

     `type="search"` has one native side effect worth handling
     deliberately: WebKit/Blink draw their own "x" clear affordance
     once the field has text, which would visually double up with this
     component's own clear button. The `[&::-webkit-search-cancel-button]`
     arbitrary variant below turns that native one off; this component's
     own button is the only clear affordance a user ever sees.

     JavaScript is only responsible for what native HTML/CSS genuinely
     can't do: keeping the clear button's visibility in sync with the
     field's content as the user actually types, not just at whatever
     `value` this template was first rendered with. Clicking it never
     touches any hidden hand-off value the way select.jte/combobox.jte's
     clear buttons do - this input already IS the real, sole submitted
     control, so clearing it is just clearing it. --%>
!{String inputId = id.isEmpty() ? name : id;}
!{String helpId = inputId + "-help";}
!{String errorId = inputId + "-error";}
!{boolean hasHelp = !helpText.isEmpty();}
!{boolean showError = error && !errorMessage.isEmpty();}
!{String describedByAttr = hasHelp && showError ? helpId + " " + errorId : hasHelp ? helpId : showError ? errorId : null;}
!{boolean hasValue = !value.isEmpty();}
!{String clearDisplay = hasValue ? "flex" : "hidden";}

<%-- Smart attributes: a null/false expression makes JTE omit the attribute entirely. --%>
!{String placeholderAttr = placeholder.isEmpty() ? null : placeholder;}
!{String ariaInvalidAttr = error ? "true" : null;}

!{String inputClasses = "block w-full rounded-md border border-slate-300 bg-white py-2 pl-9 pr-9 text-sm text-slate-900 shadow-sm placeholder:text-slate-400 " +
        "[&::-webkit-search-cancel-button]:appearance-none " +
        "hover:border-slate-400 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500 " +
        "disabled:cursor-not-allowed disabled:border-slate-200 disabled:bg-slate-50 disabled:text-slate-400 " +
        "user-invalid:border-red-500 user-invalid:hover:border-red-600 user-invalid:focus:border-red-500 user-invalid:focus:ring-red-500 " +
        "aria-invalid:border-red-500 aria-invalid:hover:border-red-600 aria-invalid:focus:border-red-500 aria-invalid:focus:ring-red-500 " +
        "dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:hover:border-slate-500 " +
        "dark:disabled:border-slate-700 dark:disabled:bg-slate-800 dark:disabled:text-slate-500 " +
        "dark:user-invalid:border-red-500 dark:aria-invalid:border-red-500";}

<div class="mb-6" data-search-input>
    <label for="${inputId}" class="mb-2 block text-sm font-medium text-slate-700 dark:text-slate-300">
        ${label}
        @if (required)
            <span class="text-red-600 dark:text-red-400" aria-hidden="true"> *</span>
        @endif
    </label>

    <div class="relative w-full max-w-sm">
        <span class="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3 text-slate-400 dark:text-slate-500">
            @template.ashlar.components.icon(icon = Icon.SEARCH, classes = "h-4 w-4")
        </span>

        <input
                type="search"
                id="${inputId}"
                name="${name}"
                value="${value}"
                placeholder="${placeholderAttr}"
                required="${required}"
                disabled="${disabled}"
                aria-invalid="${ariaInvalidAttr}"
                aria-describedby="${describedByAttr}"
                data-search-input-field
                class="${inputClasses}"
        >

        <button
                type="button"
                disabled="${disabled}"
                aria-label="Clear search"
                data-search-input-clear
                class="${clearDisplay} absolute inset-y-0 right-0 items-center rounded-md pr-3 text-slate-400 hover:text-slate-600 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 disabled:opacity-40 dark:text-slate-500 dark:hover:text-slate-300"
        >
            @template.ashlar.components.icon(icon = Icon.CLOSE, classes = "h-4 w-4")
        </button>
    </div>

    @if (hasHelp)
        <p id="${helpId}" class="mt-1 text-sm text-slate-500 dark:text-slate-400">${helpText}</p>
    @endif

    @if (showError)
        <p id="${errorId}" class="mt-1 text-sm text-red-600 dark:text-red-400" role="alert">${errorMessage}</p>
    @endif
</div>

<script type="module" src="${AshlarAssets.js("search-input.js")}"></script>
