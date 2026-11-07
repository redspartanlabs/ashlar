@import dev.redspartanlabs.ashlar.icon.Icon
@import dev.redspartanlabs.ashlar.AshlarAssets

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
     component's own clear button. The `::-webkit-search-cancel-button`
     rule in search-input.css turns that native one off; this component's
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
!{boolean clearHidden = !hasValue;}

<%-- Smart attributes: a null/false expression makes JTE omit the attribute entirely. --%>
!{String placeholderAttr = placeholder.isEmpty() ? null : placeholder;}
!{String ariaInvalidAttr = error ? "true" : null;}

!{String inputClasses = "ash-field__control ash-search-input__field";}

<div class="ash-field" data-search-input>
    <label for="${inputId}" class="ash-field__label">
        ${label}
        @if (required)
            <span class="ash-field__required" aria-hidden="true"> *</span>
        @endif
    </label>

    <div class="ash-field__box">
        <span class="ash-field__icon">
            @template.ashlar.components.icon(icon = Icon.SEARCH, classes = "ash-icon--sm")
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
                hidden="${clearHidden}"
                aria-label="Clear search"
                data-search-input-clear
                class="ash-search-input__clear"
        >
            @template.ashlar.components.icon(icon = Icon.CLOSE, classes = "ash-icon--sm")
        </button>
    </div>

    @if (hasHelp)
        <p id="${helpId}" class="ash-field__help">${helpText}</p>
    @endif

    @if (showError)
        <p id="${errorId}" class="ash-field__error" role="alert">${errorMessage}</p>
    @endif
</div>

<script type="module" src="${AshlarAssets.js("search-input.js")}"></script>
