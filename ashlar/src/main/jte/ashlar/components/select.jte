@import java.util.List
@import com.redspartanlabs.ashlar.icon.Icon
@import com.redspartanlabs.ashlar.select.SelectOption
@import com.redspartanlabs.ashlar.select.SelectSupport
@import com.redspartanlabs.ashlar.AshlarAssets

@param String name
@param String label
@param List<SelectOption> options
@param String value = ""
@param String placeholder = "Select an option"
@param boolean required = false
@param boolean disabled = false
@param boolean clearable = false
@param String requiredMessage = "Please make a selection."

<%-- A server-rendered custom Select: a button-triggered listbox (the WAI-ARIA
     "select-only combobox" pattern) backed by a real, visually hidden
     <select> so this participates in native HTML form submission and
     required-field validation without any custom JS form framework - the
     same trick date-picker.jte uses with a hidden <input>. Focus stays on
     the trigger the whole time the listbox is open; aria-activedescendant
     (not a second tab stop per option) tracks which option is highlighted,
     per the APG's guidance for this exact pattern.

     CLEAR BUTTON POSITION (`right-1`, not `right-7`): the trigger reserves
     `pr-8` (32px) of empty padding after its own content specifically so
     a clear button has somewhere to sit without touching the value text -
     but the chevron itself lives INSIDE that content box, flush against
     the start of that padding, so its own box occupies roughly the
     32-48px band measured from the trigger's right edge. `right-7` (28px)
     put the clear button's box at roughly 28-52px - overlapping the
     chevron's 32-48px band by design-math alone, on every `clearable`
     instance, not only a narrow one. `right-1` keeps the clear button's
     ~24px box inside the unused 0-32px band the padding already reserves,
     landing a few pixels short of where the chevron starts - adjacent,
     not overlapping, using space already set aside for exactly this,
     with no width added anywhere.

     TRIGGER PADDING IS NOW CONDITIONAL ON `clearable`. That 32px reserved
     band exists only to hold the clear button - a plain, non-clearable
     Select never renders one, so there was never a reason for it to keep
     reserving that space either. Before this fix `pr-8` was hardcoded
     unconditionally, so even a `clearable = false` instance showed the
     same wide gap after its chevron as a clearable one, for no reason a
     caller could see. `triggerPadding` below picks `pr-8` only when
     `clearable` is true (preserving the exact band the clear-button fix
     above relies on) and the ordinary `pr-3` - matching the trigger's own
     `pl-3` - otherwise, so a plain Select's chevron sits with a normal,
     symmetric gutter instead of a one-sided reservation for a button
     that was never going to render. --%>
!{String triggerPadding = clearable ? "pr-8" : "pr-3";}
!{SelectOption selected = SelectSupport.resolveSelected(options, value);}
!{boolean hasSelection = selected != null;}
!{String triggerId = name + "-trigger";}
!{String labelId = name + "-label";}
!{String listboxId = name + "-listbox";}
!{String errorId = name + "-error";}
!{String valueClasses = hasSelection ? "" : "text-slate-400 dark:text-slate-500";}
!{String clearButtonDisplay = (clearable && hasSelection) ? "flex" : "hidden";}
!{String triggerErrorClasses = "flex w-full items-center justify-between gap-2 rounded-md border border-red-500 bg-white py-2 pl-3 " + triggerPadding + " text-left text-slate-900 shadow-sm hover:border-red-600 focus:border-red-500 focus:outline-none focus:ring-2 focus:ring-red-500 disabled:cursor-not-allowed disabled:border-slate-200 disabled:bg-slate-50 disabled:text-slate-400 dark:bg-slate-800 dark:text-slate-100 dark:border-red-500 dark:hover:border-red-400 dark:disabled:border-slate-700 dark:disabled:bg-slate-800 dark:disabled:text-slate-500";}

<%-- Smart attributes: a null/false expression makes JTE omit the attribute entirely. --%>
!{String requiredAttr = required ? "true" : null;}

<div class="mb-6" data-select data-placeholder="${placeholder}">
    <label id="${labelId}" for="${triggerId}" class="mb-2 block text-sm font-medium text-slate-700 dark:text-slate-300">
        ${label}
        @if (required)
            <span class="text-red-600 dark:text-red-400" aria-hidden="true"> *</span>
        @endif
    </label>

    <div class="relative w-full max-w-sm">
        <button
                type="button"
                id="${triggerId}"
                data-select-trigger
                disabled="${disabled}"
                role="combobox"
                aria-haspopup="listbox"
                aria-expanded="false"
                aria-controls="${listboxId}"
                aria-required="${requiredAttr}"
                data-select-error-classes="${triggerErrorClasses}"
                class="flex w-full items-center justify-between gap-2 rounded-md border border-slate-300 bg-white py-2 pl-3 ${triggerPadding} text-left text-slate-900 shadow-sm hover:border-slate-400 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:cursor-not-allowed disabled:border-slate-200 disabled:bg-slate-50 disabled:text-slate-400 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:hover:border-slate-500 dark:disabled:border-slate-700 dark:disabled:bg-slate-800 dark:disabled:text-slate-500"
        >
            <span data-select-value class="${valueClasses} truncate">${hasSelection ? selected.label() : placeholder}</span>
            <span data-select-chevron class="shrink-0 text-slate-400 transition-transform duration-150 ease-out motion-reduce:transition-none dark:text-slate-500">
                @template.ashlar.components.icon(icon = Icon.CHEVRON_DOWN, classes = "h-4 w-4")
            </span>
        </button>

        @if (clearable)
            <button
                    type="button"
                    data-select-clear
                    disabled="${disabled}"
                    aria-label="Clear selection"
                    class="${clearButtonDisplay} absolute inset-y-0 right-1 items-center rounded px-1 text-slate-400 hover:text-slate-600 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 disabled:opacity-40 dark:text-slate-500 dark:hover:text-slate-300"
            >
                @template.ashlar.components.icon(icon = Icon.CLOSE, classes = "h-4 w-4")
            </button>
        @endif

        <%-- A real <select> so `required`/`name` participate in native form
             submission and constraint validation exactly like any other
             field - hidden inputs are barred from constraint validation
             entirely, per spec. Visually and accessibly hidden: the trigger
             button above is what a user actually sees and operates. --%>
        <select
                name="${name}"
                data-select-input
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
                data-select-menu
                role="listbox"
                aria-labelledby="${labelId}"
                class="absolute z-10 mt-1 hidden max-h-64 w-full origin-top scale-95 overflow-auto rounded-md bg-white py-1 opacity-0 shadow-lg ring-1 ring-slate-200 transition duration-150 ease-out motion-reduce:transition-none dark:bg-slate-800 dark:ring-slate-700"
        >
            @for (SelectOption option : options)
                !{String optionId = triggerId + "-option-" + option.value();}
                !{boolean isSelected = option.value().equals(value);}
                <li
                        id="${optionId}"
                        data-select-option
                        data-select-option-value="${option.value()}"
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
        </ul>

        <p class="sr-only" data-select-status aria-live="polite"></p>

        <p id="${errorId}" data-select-error class="mt-1 hidden text-sm text-red-600 dark:text-red-400" role="alert">${requiredMessage}</p>
    </div>
</div>

<script type="module" src="${AshlarAssets.js("select.js")}"></script>
