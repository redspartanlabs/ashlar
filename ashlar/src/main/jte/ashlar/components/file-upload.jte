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
     native rendering to reuse: the input is visually hidden (`sr-only`,
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
     of it, `has-[...]` (a real ancestor-reacts-to-descendant relationship)
     is what drives the card's focus/error/disabled treatment here, not
     `peer` (which only ever matches later siblings, the relationship
     toggle.jte's own track/thumb actually have with their input).

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

<div class="mb-6" data-file-upload>
    <label for="${inputId}" class="mb-2 block text-sm font-medium text-slate-700 dark:text-slate-300">
        ${label}
        @if (required)
            <span class="text-red-600 dark:text-red-400" aria-hidden="true"> *</span>
        @endif
    </label>

    <label class="flex w-full max-w-sm cursor-pointer flex-col items-center gap-3 rounded-lg border-2 border-dashed border-slate-300 bg-slate-50 px-6 py-8 text-center transition-colors motion-reduce:transition-none hover:border-slate-400 hover:bg-slate-100 has-[:focus-visible]:outline-none has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-blue-500 has-[:focus-visible]:ring-offset-2 has-[:disabled]:cursor-not-allowed has-[:disabled]:opacity-60 has-[:disabled]:hover:border-slate-300 has-[:disabled]:hover:bg-slate-50 has-[:user-invalid]:border-red-400 has-[[aria-invalid=true]]:border-red-400 dark:border-slate-600 dark:bg-slate-800/50 dark:hover:border-slate-500 dark:hover:bg-slate-800 dark:has-[:disabled]:hover:border-slate-600 dark:has-[:disabled]:hover:bg-slate-800/50 dark:has-[:user-invalid]:border-red-500 dark:has-[[aria-invalid=true]]:border-red-500">
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
                class="sr-only"
        >

        <span aria-hidden="true" class="text-slate-400 dark:text-slate-500">
            @template.ashlar.components.icon(icon = Icon.UPLOAD, classes = "h-6 w-6")
        </span>

        <p data-file-upload-status data-idle-text="${idleText}" class="max-w-full truncate text-sm font-medium text-slate-700 dark:text-slate-300">${idleText}</p>

        <span class="${chooseButtonClasses} pointer-events-none">Choose File</span>
    </label>

    @if (hasHelp)
        <p id="${helpId}" class="mt-1 text-sm text-slate-500 dark:text-slate-400">${helpText}</p>
    @endif

    @if (showError)
        <p id="${errorId}" class="mt-1 text-sm text-red-600 dark:text-red-400" role="alert">${errorMessage}</p>
    @endif
</div>

<script type="module" src="${AshlarAssets.js("file-upload.js")}"></script>
