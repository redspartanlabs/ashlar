@param String fieldId
@param String label
@param gg.jte.Content control
@param String helpText = ""
@param String errorMessage = ""
@param boolean required = false

<%-- The label/help/error scaffolding around a control this library does NOT
     already wrap - a native <select>, an <input type="color">, a bespoke
     composed control, anything from outside.

     It exists for exactly that gap, and nothing else. Every control this
     library ships (text-input, textarea, checkbox, radio-group, toggle,
     number-input, combobox, date-picker, date-range-picker, file-upload,
     search-input, select) already renders its own <label>, its own help and
     error text, its own required marker and its own field spacing, and
     wires `aria-describedby`/`aria-invalid` onto the real control itself.
     Wrapping one of those in this component produces two labels, two copies
     of the help text and doubled field spacing - so don't. The showcase page
     says the same thing where a caller will actually read it.

     `fieldId` is required, and is the id of the control the caller renders
     inside `control`. It is the join between the two halves: the <label>
     points at it, and the help/error paragraphs derive their own ids from it
     (`{fieldId}-help`, `{fieldId}-error`). There is no id generation here on
     purpose - a generated id would be invisible to the caller, and the
     caller is the only one who can put it on their own control.

     What this component can and cannot do, stated plainly because it's the
     whole contract:

       it does    - name the control (<label for>), render help and error
                    text with stable, predictable ids, show the required
                    marker, and own the vertical rhythm of the field.

       it cannot  - put `aria-describedby`, `aria-invalid` or `required` on
                    the control. `control` arrives here as already-rendered
                    markup, not something a template can reach into and add
                    an attribute to. Tooltip and Popover hit the same wall
                    and solved it with JavaScript; a Form Field has no
                    runtime behaviour to justify shipping any, so instead the
                    ids are deterministic and the caller wires them:

                        aria-describedby="{fieldId}-help {fieldId}-error"
                        aria-invalid="true"        (when showing an error)
                        required                   (when required)

     That last point is why the asterisk here is `aria-hidden` and purely
     visual, exactly as it is in every other control in this library: the
     real required semantics come from the control's own `required`
     attribute, not from decoration. An asterisk on its own would be a lie
     told to sighted users only.

     Help and error are both rendered when both are set, rather than the
     error replacing the help: the help text is usually still the thing that
     explains how to fix the error. That also means the describedby value
     above lists both ids. --%>
!{String helpId = fieldId + "-help";}
!{String errorId = fieldId + "-error";}
!{boolean hasHelp = !helpText.isEmpty();}
!{boolean hasError = !errorMessage.isEmpty();}

<div class="mb-6">
    <label for="${fieldId}" class="mb-2 block text-sm font-medium text-slate-700 dark:text-slate-300">
        ${label}
        @if (required)
            <span class="text-red-600 dark:text-red-400" aria-hidden="true"> *</span>
        @endif
    </label>

    <%-- No width or layout imposed on the control: it belongs to the caller,
         and a max-width here would quietly fight whatever they brought. --%>
    ${control}

    @if (hasHelp)
        <p id="${helpId}" class="mt-1 text-sm text-slate-500 dark:text-slate-400">${helpText}</p>
    @endif

    @if (hasError)
        <p id="${errorId}" class="mt-1 text-sm text-red-600 dark:text-red-400" role="alert">${errorMessage}</p>
    @endif
</div>
