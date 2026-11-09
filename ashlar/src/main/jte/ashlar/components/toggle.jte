@param String name
@param String label
@param String value = "on"
@param boolean checked = false
@param boolean disabled = false
@param boolean required = false
@param String id = ""
@param String helpText = ""
@param boolean error = false
@param String errorMessage = ""
@param String labelPosition = "end"

<%-- A real <input type="checkbox"> underneath a track-and-thumb visual,
     wrapped in one implicit <label> - clicking anywhere in the switch,
     including the track/thumb, toggles the real input via plain native
     label behavior, no click handler needed.

     Unlike checkbox.jte (which visibly styles the real input itself via
     `accent-color`), a track-and-thumb switch has no native browser rendering
     to reuse - no browser draws a "switch" for <input type="checkbox">.
     So here the real input is visually hidden (`ash-sr-only`, not
     `display:none`: it stays in the accessibility tree, focusable, and
     is exactly what native Tab/Space/label-click/form submission/reset
     all still operate on) and the track + thumb are two sibling <span>s
     driven entirely by sibling selectors (toggle.css) reading that
     input's :checked/:disabled/:focus-visible/aria-invalid state. CSS
     alone handles the visual transition - nothing here needs JS to add
     or remove a class when the input's checked state changes.

     No `indeterminate`: a switch represents a definite ON/OFF state by
     design, unlike Checkbox where "some but not all" is a real state a
     group can be in. No custom unchecked-value mechanism either -
     native HTML simply submits nothing for an unchecked checkbox, and
     that's preserved as-is rather than invented around. --%>
!{String toggleId = id.isEmpty() ? name : id;}
!{String helpId = toggleId + "-help";}
!{String errorId = toggleId + "-error";}
!{boolean hasHelp = !helpText.isEmpty();}
!{boolean showError = error && !errorMessage.isEmpty();}
!{String describedByAttr = hasHelp && showError ? helpId + " " + errorId : hasHelp ? helpId : showError ? errorId : null;}
!{boolean labelStart = "start".equalsIgnoreCase(labelPosition);}
!{String labelClasses = labelStart ? "ash-toggle__label ash-toggle__label--start" : "ash-toggle__label";}

<%-- Smart attributes: a null/false expression makes JTE omit the attribute entirely. --%>
!{String ariaInvalidAttr = error ? "true" : null;}

<div class="ash-toggle">
    <label class="${labelClasses}">
        <span class="ash-toggle__switch">
            <input
                    type="checkbox"
                    id="${toggleId}"
                    name="${name}"
                    value="${value}"
                    checked="${checked}"
                    disabled="${disabled}"
                    required="${required}"
                    aria-invalid="${ariaInvalidAttr}"
                    aria-describedby="${describedByAttr}"
                    class="ash-toggle__input ash-sr-only"
            >
            <%-- Track. Decorative: the real state lives on the input above,
                 this only ever reflects it via sibling selectors. --%>
            <span aria-hidden="true" class="ash-toggle__track"></span>

            <%-- Thumb. A sibling of the input (not nested in the track span)
                 so `:checked ~` can reach it directly - the general sibling
                 combinator only matches later siblings, not descendants of
                 siblings. --%>
            <span aria-hidden="true" class="ash-toggle__thumb"></span>
        </span>

        <span class="ash-toggle__text">
            ${label}
            @if (required)
                <span class="ash-field__required" aria-hidden="true"> *</span>
            @endif
        </span>
    </label>

    @if (hasHelp)
        <p id="${helpId}" class="ash-choice__description ash-toggle__help">${helpText}</p>
    @endif

    @if (showError)
        <p id="${errorId}" class="ash-choice__error ash-toggle__error" role="alert">${errorMessage}</p>
    @endif
</div>
