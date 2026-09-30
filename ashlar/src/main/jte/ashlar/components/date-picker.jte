@import com.redspartanlabs.ashlar.icon.Icon
@import com.redspartanlabs.ashlar.datepicker.DateDisplayFormat
@import com.redspartanlabs.ashlar.AshlarAssets

@param String name
@param String label
@param String value = ""
@param String format = "MMM d, yyyy"
@param String placeholder = "Select a date"
@param String minDate = ""
@param String maxDate = ""
@param boolean disabled = false
@param boolean required = false
@param String requiredMessage = "Please select a date."

!{DateDisplayFormat resolvedFormat = DateDisplayFormat.fromPattern(format);}
!{String initialDisplayValue = DateDisplayFormat.formatIso(value, resolvedFormat);}
!{boolean hasInitialValue = !initialDisplayValue.isEmpty();}
!{String triggerId = name + "-trigger";}
!{String panelId = name + "-panel";}
!{String errorId = name + "-error";}
!{String valueClasses = hasInitialValue ? "" : "text-slate-400 dark:text-slate-500";}
!{String clearButtonDisplay = hasInitialValue ? "flex" : "hidden";}
!{String triggerErrorClasses = "flex w-full items-center justify-between gap-2 rounded-md border border-red-500 bg-white py-2 pl-3 pr-8 text-left text-slate-900 shadow-sm hover:border-red-600 focus:border-red-500 focus:outline-none focus:ring-2 focus:ring-red-500 disabled:cursor-not-allowed disabled:border-slate-200 disabled:bg-slate-50 disabled:text-slate-400 dark:bg-slate-800 dark:text-slate-100 dark:border-red-500 dark:hover:border-red-400 dark:disabled:border-slate-700 dark:disabled:bg-slate-800 dark:disabled:text-slate-500";}

<%-- Smart attributes: a null/false expression makes JTE omit the attribute entirely. --%>
!{String minDateAttr = minDate.isEmpty() ? null : minDate;}
!{String maxDateAttr = maxDate.isEmpty() ? null : maxDate;}
!{String requiredAttr = required ? "true" : null;}

<div
        class="mb-6"
        data-date-picker
        data-format="${resolvedFormat.pattern()}"
        data-placeholder="${placeholder}"
        data-min-date="${minDateAttr}"
        data-max-date="${maxDateAttr}"
>
    <label for="${triggerId}" class="mb-2 block text-sm font-medium text-slate-700 dark:text-slate-300">
        ${label}
        @if (required)
            <span class="text-red-600 dark:text-red-400" aria-hidden="true"> *</span>
        @endif
    </label>

    <div class="relative w-full max-w-sm">
        <button
                type="button"
                id="${triggerId}"
                data-date-picker-trigger
                disabled="${disabled}"
                aria-haspopup="dialog"
                aria-expanded="false"
                aria-controls="${panelId}"
                aria-required="${requiredAttr}"
                data-date-picker-error-classes="${triggerErrorClasses}"
                class="flex w-full items-center justify-between gap-2 rounded-md border border-slate-300 bg-white py-2 pl-3 pr-8 text-left text-slate-900 shadow-sm hover:border-slate-400 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:cursor-not-allowed disabled:border-slate-200 disabled:bg-slate-50 disabled:text-slate-400 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:hover:border-slate-500 dark:disabled:border-slate-700 dark:disabled:bg-slate-800 dark:disabled:text-slate-500"
        >
            <span data-date-picker-value class="${valueClasses} truncate">${hasInitialValue ? initialDisplayValue : placeholder}</span>
            @template.ashlar.components.icon(icon = Icon.CALENDAR, classes = "h-4 w-4 shrink-0 text-slate-400 dark:text-slate-500")
        </button>

        <button
                type="button"
                data-date-picker-clear
                disabled="${disabled}"
                aria-label="Clear date"
                class="${clearButtonDisplay} absolute inset-y-0 right-1 items-center rounded px-1 text-slate-400 hover:text-slate-600 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 disabled:opacity-40 dark:text-slate-500 dark:hover:text-slate-300"
        >
            @template.ashlar.components.icon(icon = Icon.CLOSE, classes = "h-4 w-4")
        </button>

        <%-- A real (not type=hidden) input so `required` can participate in the
             form's native constraint validation - hidden inputs are barred from
             constraint validation entirely, per spec. Visually and
             accessibly hidden (sr-only + aria-hidden) and unreachable by Tab
             (tabindex=-1): the trigger button is the control a user actually
             sees and operates; this only exists so the browser has something
             to validate and submit. --%>
        <input
                type="text"
                name="${name}"
                data-date-picker-input
                class="sr-only"
                tabindex="-1"
                aria-hidden="true"
                disabled="${disabled}"
                required="${required}"
                value="${value}"
        >

        <div
                id="${panelId}"
                data-date-picker-panel
                role="dialog"
                aria-modal="false"
                aria-label="Choose date"
                class="absolute z-10 mt-2 hidden w-72 rounded-xl bg-white p-4 shadow-lg ring-1 ring-slate-200 dark:bg-slate-800 dark:ring-slate-700"
        >
            <div class="mb-4 flex items-center justify-between">
                <button
                        type="button"
                        data-date-picker-prev
                        aria-label="Previous month"
                        class="rounded-md p-2 text-slate-500 hover:bg-slate-100 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:text-slate-400 dark:hover:bg-slate-700"
                >
                    @template.ashlar.components.icon(icon = Icon.CHEVRON_LEFT, classes = "h-4 w-4")
                </button>

                <button
                        type="button"
                        data-date-picker-title
                        aria-live="polite"
                        class="rounded px-1 font-semibold text-slate-900 hover:text-blue-600 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:cursor-default disabled:hover:text-slate-900 dark:text-slate-100 dark:hover:text-blue-400 dark:disabled:hover:text-slate-100"
                ></button>

                <button
                        type="button"
                        data-date-picker-next
                        aria-label="Next month"
                        class="rounded-md p-2 text-slate-500 hover:bg-slate-100 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:text-slate-400 dark:hover:bg-slate-700"
                >
                    @template.ashlar.components.icon(icon = Icon.CHEVRON_RIGHT, classes = "h-4 w-4")
                </button>
            </div>

            <div data-date-picker-weekdays class="mb-2 grid grid-cols-7 text-center text-xs font-medium text-slate-500 dark:text-slate-400">
                <div>Sun</div>
                <div>Mon</div>
                <div>Tue</div>
                <div>Wed</div>
                <div>Thu</div>
                <div>Fri</div>
                <div>Sat</div>
            </div>

            <div data-date-picker-days class="grid grid-cols-7 gap-1 text-center"></div>

            <button
                    type="button"
                    data-date-picker-today
                    class="mt-3 w-full rounded-md py-1.5 text-center text-sm font-medium text-blue-600 hover:bg-blue-50 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:cursor-not-allowed disabled:text-slate-300 disabled:hover:bg-transparent dark:text-blue-400 dark:hover:bg-slate-700 dark:disabled:text-slate-600"
            >
                Today
            </button>
        </div>

        <p class="sr-only" data-date-picker-status aria-live="polite"></p>

        <p id="${errorId}" data-date-picker-error class="mt-1 hidden text-sm text-red-600 dark:text-red-400" role="alert">${requiredMessage}</p>
    </div>
</div>

<script type="module" src="${AshlarAssets.js("date-picker.js")}"></script>
