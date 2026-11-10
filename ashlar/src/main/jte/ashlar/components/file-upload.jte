@import dev.redspartanlabs.ashlar.icon.Icon
@import dev.redspartanlabs.ashlar.button.ButtonStyles
@import dev.redspartanlabs.ashlar.button.ButtonVariant
@import dev.redspartanlabs.ashlar.button.ButtonSize
@import dev.redspartanlabs.ashlar.AshlarAssets

@param String name
@param String label
@param boolean multiple = false
@param String accept = ""
@param boolean required = false
@param boolean disabled = false
@param String id = ""
@param String helpText = ""
@param boolean error = false
@param String errorMessage = ""

<%-- A real <input type="file"> underneath a card, the same architecture
     toggle.jte already established for a control with no attractive
     native rendering to reuse: the input is visually hidden (`ash-sr-only`,
     not `display:none` - it stays focusable and is exactly what Tab,
     Space/Enter-to-open, and native form submission all still operate
     on), and one native mechanism opens the picker with no JS at all -
     clicking anywhere a <label> wraps, including this whole card,
     activates its associated control. The "Choose File" text is a
     decorative <span> carrying Button's own generated classes
     (ButtonStyles.classes(...), not a real @template.ashlar.components.button
     call), never a second real <button> - nesting an actual interactive
     element inside the label would make its own click handling compete
     with the label's native forward-to-control behavior for no benefit.

     Because the input is nested *inside* the label rather than a sibling
     of it, `:has()` (a real ancestor-reacts-to-descendant relationship, in
     file-upload.css) is what drives the card's focus/error/disabled treatment
     here, not a sibling selector (which only ever matches later siblings, the
     relationship toggle.jte's own track/thumb actually have with their input).

     The only JavaScript this component needs - and the only thing native
     HTML/CSS genuinely can't do - is reflecting which file(s) got picked
     back into the card's own text after the fact.

     `aria-label` on the input is deliberate, not decorative: with two real
     <label>s pointing at one control (the one above for visual/semantic
     grouping, this one for click-to-open), browsers concatenate both
     labels' text into the accessible name by default - which would pull
     in the dynamically-changing filename and "Choose File" text too.
     `aria-label` takes precedence over label-derived text entirely, so
     the accessible name stays exactly "${label}" regardless of what's
     currently selected; it has no effect on either label's native
     click-to-open behavior, which is unrelated to naming. --%>
!{String inputId = id.isEmpty() ? name : id;}
!{String helpId = inputId + "-help";}
!{String errorId = inputId + "-error";}
!{boolean hasHelp = !helpText.isEmpty();}
!{boolean showError = error && !errorMessage.isEmpty();}
!{String describedByAttr = hasHelp && showError ? helpId + " " + errorId : hasHelp ? helpId : showError ? errorId : null;}
!{String idleText = multiple ? "Choose files to upload" : "Choose a file to upload";}
!{String chooseButtonClasses = ButtonStyles.classes(ButtonVariant.SECONDARY, ButtonSize.SMALL, false, false, false);}

<%-- Smart attributes: a null/false expression makes JTE omit the attribute entirely. --%>
!{String acceptAttr = accept.isEmpty() ? null : accept;}
!{String ariaInvalidAttr = error ? "true" : null;}

<div class="ash-field ash-file-upload" data-file-upload>
    <label for="${inputId}" class="ash-field__label">
        ${label}
        @if (required)
            <span class="ash-field__required" aria-hidden="true"> *</span>
        @endif
    </label>

    <label class="ash-file-upload__zone">
        <input
                type="file"
                id="${inputId}"
                name="${name}"
                multiple="${multiple}"
                accept="${acceptAttr}"
                required="${required}"
                disabled="${disabled}"
                aria-label="${label}"
                aria-invalid="${ariaInvalidAttr}"
                aria-describedby="${describedByAttr}"
                data-file-upload-input
                class="ash-file-upload__input ash-sr-only"
        >

        <span aria-hidden="true" class="ash-file-upload__icon">
            @template.ashlar.components.icon(icon = Icon.UPLOAD, classes = "ash-icon--lg")
        </span>

        <p data-file-upload-status data-idle-text="${idleText}" class="ash-file-upload__status">${idleText}</p>

        <span class="${chooseButtonClasses} ash-file-upload__choose">Choose File</span>
    </label>

    @if (hasHelp)
        <p id="${helpId}" class="ash-field__help">${helpText}</p>
    @endif

    @if (showError)
        <p id="${errorId}" class="ash-field__error" role="alert">${errorMessage}</p>
    @endif
</div>

<script type="module" src="${AshlarAssets.js("file-upload.js")}"></script>
