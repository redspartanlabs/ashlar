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
     `accent-*`), a track-and-thumb switch has no native browser rendering
     to reuse - no browser draws a "switch" for <input type="checkbox">.
     So here the real input is visually hidden (`sr-only`, not
     `display:none`: it stays in the accessibility tree, focusable, and
     is exactly what native Tab/Space/label-click/form submission/reset
     all still operate on) and the track + thumb are two sibling <span>s
     driven entirely by Tailwind's `peer-*` variants reading that
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

<%-- Smart attributes: a null/false expression makes JTE omit the attribute entirely. --%>
!{String ariaInvalidAttr = error ? "true" : null;}

<div class="mb-4">
    <label class="inline-flex cursor-pointer items-center gap-3 has-[:disabled]:cursor-not-allowed ${labelStart ? "flex-row-reverse" : ""}">
        <span class="relative inline-flex h-6 w-11 shrink-0 items-center">
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
                    class="peer sr-only"
            >
            <%-- Track. Decorative: the real state lives on the input above,
                 this only ever reflects it via peer-* variants. --%>
            <span
                    aria-hidden="true"
                    class="absolute inset-0 rounded-full bg-slate-300 transition-colors duration-150 motion-reduce:transition-none hover:bg-slate-400 dark:bg-slate-600 dark:hover:bg-slate-500 peer-checked:bg-blue-600 peer-checked:hover:bg-blue-700 dark:peer-checked:bg-blue-500 dark:peer-checked:hover:bg-blue-400 peer-disabled:hover:bg-slate-300 dark:peer-disabled:hover:bg-slate-600 peer-checked:peer-disabled:hover:bg-blue-600 dark:peer-checked:peer-disabled:hover:bg-blue-500 peer-disabled:opacity-60 peer-focus-visible:outline-none peer-focus-visible:ring-2 peer-focus-visible:ring-blue-500 peer-focus-visible:ring-offset-2 peer-aria-invalid:ring-2 peer-aria-invalid:ring-red-500 dark:peer-aria-invalid:ring-red-500"
            ></span>

            <%-- Thumb. A sibling of the input (not nested in the track span)
                 so `peer-checked:` can reach it directly - Tailwind's peer
                 mechanism only matches later siblings, not descendants of
                 siblings. --%>
            <span
                    aria-hidden="true"
                    class="pointer-events-none absolute left-0.5 top-0.5 h-5 w-5 rounded-full bg-white shadow-sm transition-transform duration-150 motion-reduce:transition-none peer-checked:translate-x-5"
            ></span>
        </span>

        <span class="text-sm font-medium text-slate-700 dark:text-slate-300">
            ${label}
            @if (required)
                <span class="text-red-600 dark:text-red-400" aria-hidden="true"> *</span>
            @endif
        </span>
    </label>

    @if (hasHelp)
        <p id="${helpId}" class="mt-1 text-sm text-slate-500 dark:text-slate-400">${helpText}</p>
    @endif

    @if (showError)
        <p id="${errorId}" class="mt-1 text-sm text-red-600 dark:text-red-400" role="alert">${errorMessage}</p>
    @endif
</div>
