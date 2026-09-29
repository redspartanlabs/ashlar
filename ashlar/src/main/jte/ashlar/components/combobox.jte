@import java.util.List
@import com.redspartanlabs.ashlar.icon.Icon
@import com.redspartanlabs.ashlar.select.SelectOption
@import com.redspartanlabs.ashlar.select.SelectSupport
@import com.redspartanlabs.ashlar.AshlarAssets

@param String name
@param String label
@param List<SelectOption> options
@param String value = ""
@param String placeholder = "Search..."
@param boolean required = false
@param boolean disabled = false
@param String helpText = ""
@param boolean error = false
@param String errorMessage = ""

<%-- select.jte's editable sibling: the WAI-ARIA "combobox with list
     autocomplete" pattern - an editable text <input> (not a button) drives
     an aria-controls'd listbox, backed by the same real-hidden-<select>
     trick select.jte uses for native form participation and required
     validation. Reuses select's own SelectOption/SelectSupport directly:
     an option here is exactly value+label+disabled, nothing more, so a
     second near-identical Java type would only be duplication.

     The key behavioral difference from select.jte: typing filters the
     visible options instead of just moving a type-ahead cursor, and the
     input is real, editable text rather than a read-only trigger label.
     Only an explicit commit - clicking an option, or pressing Enter while
     one is active - ever writes to the hidden <select>; combobox.js
     tracks the last committed option itself and reverts the visible text
     (and the hidden value) back to it on blur/Escape whenever the user
     leaves without a new explicit commit, so the submitted value is never
     an arbitrary in-progress search string. --%>
!{SelectOption selected = SelectSupport.resolveSelected(options, value);}
!{boolean hasSelection = selected != null;}
!{String inputId = name + "-input";}
!{String labelId = name + "-label";}
!{String listboxId = name + "-listbox";}
!{String errorId = name + "-error";}
!{String helpId = name + "-help";}
!{boolean hasHelp = !helpText.isEmpty();}
!{boolean showError = error && !errorMessage.isEmpty();}
!{String describedByAttr = hasHelp && showError ? helpId + " " + errorId : hasHelp ? helpId : showError ? errorId : null;}
<%-- Unlike text-input.jte/textarea.jte (whose error styling is pure CSS via
     `user-invalid:`), the real constraint-bearing element here is the
     hidden <select>, not the visible input - so combobox.js has to listen
     for its native 'invalid' event and reveal this error text itself,
     the same way select.js already does. That means this paragraph must
     always exist in the DOM (hidden via class, not omitted via @if), or
     there would be nothing for a native required-validation failure to
     reveal on an instance that didn't start in an error state. --%>
!{String resolvedErrorMessage = errorMessage.isEmpty() ? "Please make a selection." : errorMessage;}
!{String triggerErrorClasses = "block w-full rounded-md border border-red-500 bg-white py-2 pl-3 pr-8 text-sm text-slate-900 shadow-sm placeholder:text-slate-400 hover:border-red-600 focus:border-red-500 focus:outline-none focus:ring-2 focus:ring-red-500 disabled:cursor-not-allowed disabled:border-slate-200 disabled:bg-slate-50 disabled:text-slate-400 dark:bg-slate-800 dark:text-slate-100 dark:border-red-500 dark:hover:border-red-400 dark:disabled:border-slate-700 dark:disabled:bg-slate-800 dark:disabled:text-slate-500";}

<%-- Smart attributes: a null/false expression makes JTE omit the attribute entirely. --%>
!{String requiredAttr = required ? "true" : null;}
!{String ariaInvalidAttr = error ? "true" : null;}

<div class="mb-6" data-combobox data-placeholder="${placeholder}">
    <label id="${labelId}" for="${inputId}" class="mb-2 block text-sm font-medium text-slate-700 dark:text-slate-300">
        ${label}
        @if (required)
            <span class="text-red-600 dark:text-red-400" aria-hidden="true"> *</span>
        @endif
    </label>

    <div class="relative w-full max-w-sm">
        <input
                type="text"
                id="${inputId}"
                data-combobox-input
                value="${hasSelection ? selected.label() : ""}"
                placeholder="${placeholder}"
                disabled="${disabled}"
                autocomplete="off"
                spellcheck="false"
                role="combobox"
                aria-haspopup="listbox"
                aria-expanded="false"
                aria-controls="${listboxId}"
                aria-autocomplete="list"
                aria-required="${requiredAttr}"
                aria-invalid="${ariaInvalidAttr}"
                aria-describedby="${describedByAttr}"
                data-combobox-error-classes="${triggerErrorClasses}"
                class="block w-full rounded-md border border-slate-300 bg-white py-2 pl-3 pr-8 text-sm text-slate-900 shadow-sm placeholder:text-slate-400 hover:border-slate-400 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:cursor-not-allowed disabled:border-slate-200 disabled:bg-slate-50 disabled:text-slate-400 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:hover:border-slate-500 dark:disabled:border-slate-700 dark:disabled:bg-slate-800 dark:disabled:text-slate-500"
        >
        <%-- Positioning (absolute + the pr-3 inset) lives on this outer span,
             which never rotates - it stays a static 28x38 box regardless of
             open/closed state. The rotation itself is on the inner span
             instead, sized to exactly wrap the 16x16 icon with no padding
             of its own. CSS rotates an element around its *own* box center
             by default: if the rotating element's box is wider than the
             icon it contains (as the outer span's padding-inflated box is),
             that center sits off to the icon's side, and rotating there
             swings the icon through a visible arc instead of flipping it
             in place - the "opens out to the right" artifact this fixes. --%>
        <span class="pointer-events-none absolute inset-y-0 right-0 flex items-center pr-3 text-slate-400 dark:text-slate-500">
            <span data-combobox-chevron class="inline-flex transition-transform duration-150 ease-out motion-reduce:transition-none">
                @template.ashlar.components.icon(icon = Icon.CHEVRON_DOWN, classes = "h-4 w-4")
            </span>
        </span>

        <%-- A real <select> so `required`/`name` participate in native form
             submission and constraint validation exactly like select.jte's
             own hidden control. Only ever written to by an explicit commit
             in combobox.js - never by the raw text the user is typing. --%>
        <select
                name="${name}"
                data-combobox-value
                class="sr-only"
                tabindex="-1"
                aria-hidden="true"
                disabled="${disabled}"
                required="${required}"
        >
            <option value=""></option>
            @for (SelectOption option : options)
                <option value="${option.value()}" disabled="${option.disabled()}" selected="${option.value().equals(value)}">${option.label()}</option>
            @endfor
        </select>

        <ul
                id="${listboxId}"
                data-combobox-listbox
                role="listbox"
                aria-labelledby="${labelId}"
                class="absolute z-10 mt-1 hidden max-h-64 w-full origin-top scale-95 overflow-auto rounded-md bg-white py-1 opacity-0 shadow-lg ring-1 ring-slate-200 transition duration-150 ease-out motion-reduce:transition-none dark:bg-slate-800 dark:ring-slate-700"
        >
            @for (SelectOption option : options)
                !{String optionId = inputId + "-option-" + option.value();}
                !{boolean isSelected = option.value().equals(value);}
                <li
                        id="${optionId}"
                        data-combobox-option
                        data-combobox-option-value="${option.value()}"
                        role="option"
                        aria-selected="${String.valueOf(isSelected)}"
                        aria-disabled="${option.disabled() ? "true" : null}"
                        data-active="false"
                        class="group flex cursor-pointer select-none items-center justify-between gap-2 px-3 py-2 text-sm text-slate-700 hover:bg-slate-50 data-[active=true]:bg-blue-50 aria-selected:font-semibold aria-selected:text-blue-600 aria-disabled:cursor-not-allowed aria-disabled:text-slate-300 aria-disabled:hover:bg-transparent dark:text-slate-200 dark:hover:bg-slate-700 dark:data-[active=true]:bg-slate-700 dark:aria-selected:text-blue-400 dark:aria-disabled:text-slate-600 dark:aria-disabled:hover:bg-transparent"
                >
                    <span class="truncate">${option.label()}</span>
                    <span class="hidden shrink-0 text-blue-600 group-aria-selected:inline-flex dark:text-blue-400">
                        @template.ashlar.components.icon(icon = Icon.CHECK, classes = "h-4 w-4")
                    </span>
                </li>
            @endfor
            <li data-combobox-empty class="hidden px-3 py-2 text-sm text-slate-500 dark:text-slate-400">No matching options</li>
        </ul>

        <p class="sr-only" data-combobox-status aria-live="polite"></p>
    </div>

    <%-- Deliberately siblings of the `.relative` wrapper above, not nested
         inside it: the chevron is positioned `absolute inset-y-0` relative
         to that wrapper, so if help/error text lived inside it too, the
         wrapper would grow taller than the input alone and the chevron
         would center itself against that extra height instead of the
         input - visibly off-center. Matches text-input.jte's own
         leading/trailing-icon wrapper for the same reason. --%>
    @if (hasHelp)
        <p id="${helpId}" class="mt-1 text-sm text-slate-500 dark:text-slate-400">${helpText}</p>
    @endif

    <p id="${errorId}" data-combobox-error class="mt-1 ${showError ? "" : "hidden"} text-sm text-red-600 dark:text-red-400" role="alert">${resolvedErrorMessage}</p>
</div>

<script type="module" src="${AshlarAssets.js("combobox.js")}"></script>
