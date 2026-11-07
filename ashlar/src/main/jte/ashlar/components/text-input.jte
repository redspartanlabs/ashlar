@import dev.redspartanlabs.ashlar.icon.Icon
@import dev.redspartanlabs.ashlar.AshlarAssets

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
     `:user-invalid`/`[aria-invalid="true"]` selectors in field.css, rather than a
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

!{String inputClasses = "ash-field__control ash-field__control--readonly" + (leadingIcon != null ? " ash-field__control--leading" : "") + (trailingIcon != null ? " ash-field__control--trailing" : "");}

<div class="ash-field">
    <label for="${inputId}" class="ash-field__label">
        ${label}
        @if (required)
            <span class="ash-field__required" aria-hidden="true"> *</span>
        @endif
    </label>

    <div class="ash-field__box">
        @if (leadingIcon != null)
            <span class="ash-field__icon">
                @template.ashlar.components.icon(icon = leadingIcon, classes = "ash-icon--sm")
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
            <span class="ash-field__icon ash-field__icon--end">
                @template.ashlar.components.icon(icon = trailingIcon, classes = "ash-icon--sm")
            </span>
        @endif
    </div>

    @if (hasHelp)
        <p id="${helpId}" class="ash-field__help">${helpText}</p>
    @endif

    @if (showError)
        <p id="${errorId}" class="ash-field__error" role="alert">${errorMessage}</p>
    @endif
</div>

@if (hasMask)
    <script type="module" src="${AshlarAssets.js("text-input.js")}"></script>
@endif
