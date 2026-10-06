@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param String contextPath

<%-- Batch 2C documentation page. Existing "Default" / "Display formats" /
     "Min / max range" / "Disabled & required" / "Multiple independent
     instances" examples are kept verbatim (moved under Variants & States) -
     all five were already accurate. Added: Quick Start, formal grammar
     headings, Composition, Pitfalls, Accessibility split, API Reference,
     Related Components.

     Verified against source before writing, including the specific items
     the earlier audit flagged as needing a closer look:
     - date-picker.jte's own @param list (10 params).
     - DateDisplayFormat.java in full: `value`/the submitted form value is
       ALWAYS ISO yyyy-MM-dd regardless of `format` - format only changes
       the trigger's displayed text. Confirmed only 5 fixed display
       formats exist (ISO/US_SLASH/EUROPEAN_SLASH/LONG/MEDIUM) and an
       unrecognized pattern string silently falls back to MEDIUM, same
       silent-fallback shape as Button's type/size. Confirmed (from the
       enum's own doc comment) that a month-only or year-only display is
       deliberately NOT supported - picking a date without a day would be
       a different component, not a format variation of this one.
     - date-picker.js in full (738 lines) - this is where the audit's
       flagged question lives. The panel's `role="dialog" aria-modal="false"`
       is intentional and correct for what it actually does: a non-modal
       popover calendar with no focus trap and no scroll lock (unlike
       Modal's real aria-modal="true"), closed by Escape, an outside
       click, or focus leaving the component entirely - confirmed by
       reading the panel's own open/close functions, not assumed from the
       attribute alone. Confirmed the real constraint-bearing control is a
       visually/accessibly hidden but NOT type="hidden" text input
       (tabindex=-1, sr-only) - type="hidden" inputs are barred from
       constraint validation per spec, which is exactly why this isn't one.
       Confirmed min/max are re-read live from the trigger's own dataset on
       every check (not cached at init) specifically so an external script
       can change them after the fact - date-range-picker.js is named
       directly in that comment as the reason, and does exactly this (see
       Date Range Picker's own Composition section). Confirmed Escape
       calls stopPropagation() after closing, for the same
       nested-overlay reason Combobox's Escape handler does. --%>
!{String quickStartCode = """
    @template.ashlar.components.date-picker(
        name = "startDate",
        label = "Start Date"
    )""";}
!{List<ApiParam> datePickerApi = List.of(
    ApiParam.required("name", "String", "The field name the ISO date submits under."),
    ApiParam.required("label", "String", "Associated with the trigger via a real <label for>."),
    ApiParam.optional("value", "String", "\"\"", "Initial selection, as an ISO yyyy-MM-dd string. Blank or unparseable degrades to \"no selection\", not an error."),
    ApiParam.optional("format", "String", "\"MMM d, yyyy\"", "Display only - one of DateDisplayFormat's 5 fixed patterns. The submitted value is always ISO regardless of this. An unrecognized pattern silently falls back to MEDIUM (\"MMM d, yyyy\")."),
    ApiParam.optional("placeholder", "String", "\"Select a date\"", "Shown in the trigger while nothing is selected."),
    ApiParam.optional("minDate", "String", "\"\"", "ISO date. Days, months, and years with no enabled day are disabled in the calendar."),
    ApiParam.optional("maxDate", "String", "\"\"", "ISO date. Same disabling behavior as minDate."),
    ApiParam.optional("disabled", "boolean", "false", "Disables the trigger and clear button. Does not block form submission by itself - see Pitfalls."),
    ApiParam.optional("required", "boolean", "false", "Enables native constraint validation on the real (visually hidden) text input."),
    ApiParam.optional("requiredMessage", "String", "\"Please select a date.\"", "Shown with role=\"alert\" when a required submit is attempted while empty.")
);}

@template.layout.page(
contextPath = contextPath,
title = "Date Picker",
content = @`
    @template.layout.component-nav(currentSlug = "date-picker", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Date Picker</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A reusable calendar date field: click to open, type nothing, always submits an unambiguous ISO date regardless of how it's displayed.</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3 max-w-sm">
                        @template.ashlar.components.date-picker(name = "quickStartDate", label = "Start Date")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">How to Use</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Reach for Date Picker whenever you need a single, unambiguous calendar date and want to rule out free-text date parsing entirely - there is no typing involved, only clicking (or arrow-keying) a day. For two related dates that must stay in order (a start and an end), use <a href="${contextPath}/components/date-range-picker" class="text-blue-700 hover:underline dark:text-blue-400">Date Range Picker</a> instead of two independent Date Pickers - it couples their constraints for you (see that page's Composition section).</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Default</p>
                            <div class="mt-3 max-w-sm">
                                @template.ashlar.components.date-picker(
                                name = "defaultDate",
                                label = "Date"
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Display formats</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Same underlying value, different presentation - the form always submits <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">yyyy-MM-dd</code>.</p>
                            <div class="mt-3 grid gap-4 sm:grid-cols-2">
                                @template.ashlar.components.date-picker(
                                name = "formatLong",
                                label = "Long (MMMM d, yyyy)",
                                format = "MMMM d, yyyy",
                                value = "2026-09-15"
                                )
                                @template.ashlar.components.date-picker(
                                name = "formatUs",
                                label = "US (MM/dd/yyyy)",
                                format = "MM/dd/yyyy",
                                value = "2026-09-15"
                                )
                                @template.ashlar.components.date-picker(
                                name = "formatEu",
                                label = "European (dd/MM/yyyy)",
                                format = "dd/MM/yyyy",
                                value = "2026-09-15"
                                )
                                @template.ashlar.components.date-picker(
                                name = "formatIso",
                                label = "ISO (yyyy-MM-dd)",
                                format = "yyyy-MM-dd",
                                value = "2026-09-15"
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Min / max range</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Restricted to 2020&ndash;2030. Navigate outside that range to see disabled days - and try opening the month/year views (click the header title) to see whole months and years disabled too.</p>
                            <div class="mt-3 max-w-sm">
                                @template.ashlar.components.date-picker(
                                name = "rangeDate",
                                label = "Within 2020-2030",
                                minDate = "2020-01-01",
                                maxDate = "2030-12-31"
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Disabled &amp; required</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Try submitting without picking a date - Required shows an error; Disabled can't block submission at all (see Pitfalls).</p>
                            <form class="mt-3 flex flex-col gap-4" action="" method="get">
                                <div class="grid gap-4 sm:grid-cols-2">
                                    @template.ashlar.components.date-picker(
                                    name = "disabledDate",
                                    label = "Disabled",
                                    value = "2026-09-15",
                                    disabled = true
                                    )
                                    @template.ashlar.components.date-picker(
                                    name = "requiredDate",
                                    label = "Required",
                                    required = true
                                    )
                                </div>
                                @template.ashlar.components.button(text = "Submit", type = "Primary", buttonType = "submit")
                            </form>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Multiple independent instances</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A date range made of two entirely separate, both required, component instances - submitting with only one filled in should only error the other. This is the pattern <a href="${contextPath}/components/date-range-picker" class="text-blue-700 hover:underline dark:text-blue-400">Date Range Picker</a> replaces when the two dates need to stay in order; use two plain Date Pickers only when they're genuinely unrelated, as here.</p>
                            <form class="mt-3 flex flex-col gap-4" action="" method="get">
                                <div class="grid gap-4 sm:grid-cols-2">
                                    @template.ashlar.components.date-picker(
                                    name = "startDate",
                                    label = "Start Date",
                                    required = true
                                    )
                                    @template.ashlar.components.date-picker(
                                    name = "endDate",
                                    label = "End Date",
                                    required = true
                                    )
                                </div>
                                @template.ashlar.components.button(text = "Submit", type = "Primary", buttonType = "submit")
                            </form>
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composition</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Date Picker exposes its min/max constraints as live, externally-readable <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">data-min-date</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">data-max-date</code> attributes that its own JavaScript re-reads on every check rather than caching once. That's not incidental - it's the exact mechanism <a href="${contextPath}/components/date-range-picker" class="text-blue-700 hover:underline dark:text-blue-400">Date Range Picker</a> relies on to keep its two internal Date Pickers from ever allowing an out-of-order selection, without reimplementing any calendar logic of its own.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Best Practices</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Do</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>Set <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">minDate</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">maxDate</code> whenever a date outside some range is genuinely invalid (e.g. no past dates for a scheduling form) rather than validating it only server-side.</li>
                                <li>Pick a display <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">format</code> that matches your audience's convention (US vs. European slash order) - it's free, since the submitted value never changes.</li>
                            </ul>
                        </div>
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Avoid</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>Using two independent Date Pickers for a start/end pair that must stay in order - that's Date Range Picker's job.</li>
                                <li>Relying on <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">disabled</code> as a security boundary - it's a client-side convenience only (see Pitfalls).</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Pitfalls &amp; Troubleshooting</h3>
                    <ul class="mt-2 list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300"><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">disabled</code> doesn't block submission on its own.</strong> It only prevents the trigger and clear button from being operated. A disabled field with a pre-set <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">value</code> still submits that value normally, exactly like a disabled native input would if it weren't disabled - this is ordinary HTML form behavior, not a bug.</li>
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">An unrecognized <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">format</code> string doesn't error.</strong> It silently falls back to MEDIUM ("MMM d, yyyy") rather than surfacing the typo.</li>
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">There is no month-only or year-only mode.</strong> Only the day/month/year views exist to navigate to a full date - picking just a month or year isn't a supported end state, by design.</li>
                    </ul>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Ashlar provides</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>A non-modal <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="dialog"</code> calendar panel (<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-modal="false"</code>) - intentionally not a focus-trapping modal, since it's a lightweight popover attached to one field, not a page-blocking dialog.</li>
                                <li>Full roving-tabindex keyboard navigation: arrow keys move by day/week (or by month/year in the drill-up views), Home/End jump to the start/end of the month, and Escape closes the panel and returns focus to the trigger.</li>
                                <li>Escape stops its own propagation after closing, so it never also closes an enclosing Modal/Drawer/Popover in the same keypress.</li>
                                <li>A real (visually hidden, not type="hidden") text input so native required-field constraint validation works, since a type="hidden" input can't participate in it at all.</li>
                            </ul>
                        </div>
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>A <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">requiredMessage</code> worth reading aloud - it renders with <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="alert"</code>.</li>
                                <li>Server-side enforcement of <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">disabled</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">minDate</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">maxDate</code> - all three are client-side UX only.</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div>
                    @template.layout.api-reference(apiParams = datePickerApi)
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/date-range-picker" class="text-blue-700 hover:underline dark:text-blue-400">Date Range Picker</a> for a coupled start/end pair &middot;
                        <a href="${contextPath}/components/form-field" class="text-blue-700 hover:underline dark:text-blue-400">Form Field</a> for wiring help/error text onto a custom control
                    </p>
                </div>
            </div>
        </section>
`
)
