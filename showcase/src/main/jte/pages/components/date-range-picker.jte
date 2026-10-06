@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param String contextPath

<%-- Batch 2C documentation page. Existing "Basic & empty" / "Preselected
     range & help text" / "Same-day range" / "Min/max bounds & disabled" /
     "Server-reported error" / "Required validation" examples are kept
     verbatim (moved under Variants & States) - all six were already
     accurate and already correctly explained the constraint-coupling in
     the page's own intro paragraph. Added: Quick Start, formal grammar
     headings, an expanded Composition section (this component is closer
     to a composition primitive than a simple input, per this batch's
     brief), Pitfalls, Accessibility split, API Reference, Related
     Components.

     Verified against source before writing, not assumed from "it behaves
     like two Date Pickers":
     - date-range-picker.jte's own @param list (17 params) and its own doc
       comment - confirmed this is two completely unmodified date-picker.jte
       instances inside one <fieldset>/<legend>, the same grouping shape
       radio-group.jte uses.
     - date-range-picker.js in full. Confirmed real cross-field coupling:
       `syncConstraints()` writes the start picker's current value as the
       end picker's live `minDate`, and the end picker's current value as
       the start picker's live `maxDate`, on every `change` - not a
       one-time init. Because date-picker.js re-reads those dataset
       attributes live, this alone makes an out-of-order date simply
       render disabled in either calendar, rather than something rejected
       after the fact. Confirmed a submit-time check (`isRangeInvalid()`)
       exists as defense in depth specifically for a range that arrives
       already invalid (e.g. bad initial server data) rather than one
       built through the calendar - it calls preventDefault(), focuses the
       end picker, and reveals a dedicated range-order error paragraph
       that is deliberately separate from either Date Picker's own
       required-field error (confirmed by the code comment: reusing that
       display would show its unrelated default message).
     - Confirmed what the server actually receives: two ordinary,
       independent ISO date fields under startName/endName - never a
       combined range value or object.
     - Confirmed disabled/required are each a single shared flag for the
       whole fieldset - neither date can be independently disabled, though
       each still has its own independent Clear button and can be
       independently cleared. --%>
!{String quickStartCode = """
    @template.ashlar.components.date-range-picker(
        startName = "rangeStart",
        endName = "rangeEnd",
        label = "Date Range"
    )""";}
!{List<ApiParam> dateRangePickerApi = List.of(
    ApiParam.required("startName", "String", "The start field's name - submits its own independent ISO date."),
    ApiParam.required("endName", "String", "The end field's name - submits its own independent ISO date."),
    ApiParam.required("label", "String", "The <legend> for the whole fieldset."),
    ApiParam.optional("startLabel", "String", "\"Start Date\"", "The start Date Picker's own <label>."),
    ApiParam.optional("endLabel", "String", "\"End Date\"", "The end Date Picker's own <label>."),
    ApiParam.optional("startValue", "String", "\"\"", "Initial start selection, as an ISO date string."),
    ApiParam.optional("endValue", "String", "\"\"", "Initial end selection, as an ISO date string."),
    ApiParam.optional("startPlaceholder", "String", "\"Select a date\"", "Passed straight through to the start Date Picker."),
    ApiParam.optional("endPlaceholder", "String", "\"Select a date\"", "Passed straight through to the end Date Picker."),
    ApiParam.optional("minDate", "String", "\"\"", "Applied to both fields, on top of the live start/end coupling described in Composition."),
    ApiParam.optional("maxDate", "String", "\"\"", "Applied to both fields, on top of the live start/end coupling described in Composition."),
    ApiParam.optional("required", "boolean", "false", "Applies to both fields - there is no independent per-field required."),
    ApiParam.optional("disabled", "boolean", "false", "Applies to both fields via the <fieldset> itself - there is no independent per-field disabled."),
    ApiParam.optional("id", "String", "\"\"", "Defaults to startName + \"-\" + endName when omitted."),
    ApiParam.optional("helpText", "String", "\"\"", "Supporting text below both fields."),
    ApiParam.optional("error", "boolean", "false", "Shows errorMessage and the range-order error paragraph."),
    ApiParam.optional("errorMessage", "String", "\"\"", "Defaults to \"End date must be on or after the start date.\" when error is true but this is left empty.")
);}

@template.layout.page(
contextPath = contextPath,
title = "Date Range Picker",
content = @`
    @template.layout.component-nav(currentSlug = "date-range-picker", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Date Range Picker</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Two independent, unmodified <a href="${contextPath}/components/date-picker" class="text-blue-700 hover:underline dark:text-blue-400">Date Picker</a>s under one &lt;fieldset&gt; - picking a start date raises the end picker's own minimum (and vice versa) through its existing disabled-date rendering, so an out-of-order range is unavailable rather than something to reject after the fact.</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3 max-w-xl">
                        @template.ashlar.components.date-range-picker(startName = "quickStartStart", endName = "quickStartEnd", label = "Date Range")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">How to Use</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Reach for Date Range Picker any time two dates must stay in order - a booking window, a reporting period, a vacation request. Don't reach for two independent <a href="${contextPath}/components/date-picker" class="text-blue-700 hover:underline dark:text-blue-400">Date Picker</a>s instead unless the two dates are genuinely unrelated to each other (see that page's own "Multiple independent instances" example for exactly that case).</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Basic &amp; empty</p>
                            <div class="max-w-xl">
                                @template.ashlar.components.date-range-picker(
                                startName = "rangeStart",
                                endName = "rangeEnd",
                                label = "Date Range"
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Preselected range &amp; help text</p>
                            <div class="max-w-xl">
                                @template.ashlar.components.date-range-picker(
                                startName = "reportingStart",
                                endName = "reportingEnd",
                                label = "Reporting Period",
                                startValue = "2026-09-01",
                                endValue = "2026-09-15",
                                helpText = "Data is aggregated for the selected period, inclusive of both dates."
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Same-day range</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A single-day range is intentionally allowed - end can equal start.</p>
                            <div class="max-w-xl">
                                @template.ashlar.components.date-range-picker(
                                startName = "sameDayStart",
                                endName = "sameDayEnd",
                                label = "Event Date",
                                startValue = "2026-10-01",
                                endValue = "2026-10-01"
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Min/max bounds &amp; disabled</p>
                            <div class="grid gap-4 sm:grid-cols-1 lg:grid-cols-2">
                                <div class="max-w-xl">
                                    @template.ashlar.components.date-range-picker(
                                    startName = "boundedRangeStart",
                                    endName = "boundedRangeEnd",
                                    label = "Booking Window",
                                    minDate = "2026-01-01",
                                    maxDate = "2026-12-31",
                                    helpText = "Restricted to 2026. Navigate outside that year to see disabled days."
                                    )
                                </div>
                                <div class="max-w-xl">
                                    @template.ashlar.components.date-range-picker(
                                    startName = "disabledRangeStart",
                                    endName = "disabledRangeEnd",
                                    label = "Date Range (disabled)",
                                    startValue = "2026-06-01",
                                    endValue = "2026-06-10",
                                    disabled = true
                                    )
                                </div>
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Server-reported error</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Forced via the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">error</code> param - the same way a controller would re-render a field after a failed server-side check.</p>
                            <div class="max-w-xl">
                                @template.ashlar.components.date-range-picker(
                                startName = "flaggedRangeStart",
                                endName = "flaggedRangeEnd",
                                label = "Date Range",
                                startValue = "2026-09-20",
                                endValue = "2026-09-10",
                                error = true
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Required validation, multiple instances &amp; form submission</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Two independent required ranges in one form. Try picking an end date before a chosen start date - it's simply disabled in the calendar. Submitting successfully reflects all four values in the URL's query string.</p>
                            <form class="mt-3 flex flex-col gap-6" action="" method="get">
                                <div class="max-w-xl">
                                    @template.ashlar.components.date-range-picker(
                                    startName = "requiredRangeStart",
                                    endName = "requiredRangeEnd",
                                    label = "Vacation Dates",
                                    required = true
                                    )
                                </div>
                                <div class="max-w-xl">
                                    @template.ashlar.components.date-range-picker(
                                    startName = "secondRangeStart",
                                    endName = "secondRangeEnd",
                                    label = "Blackout Dates",
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
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">This component adds exactly one thing neither Date Picker owns by itself: the relationship between the two dates. Each Date Picker still owns its own calendar, its own value, its own clear button - picking a date in one simply writes the other's live <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">minDate</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">maxDate</code>, reusing Date Picker's own disabled-date rendering rather than duplicating any calendar logic. A submit-time check is the only additional defense, and only for a range that arrives already invalid (bad initial server data) rather than one built through the calendar, where the live coupling already prevents the problem.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Best Practices</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Do</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>Trust the live coupling to prevent an out-of-order pick through the UI - you don't need your own client-side check for that case.</li>
                                <li>Still validate the range server-side - the submitted fields are two ordinary, independently-named values, and a client can submit anything.</li>
                            </ul>
                        </div>
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Avoid</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>Expecting a single combined value - the server receives <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">startName</code> and <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">endName</code> as two separate ISO dates, not a range object or a single delimited string.</li>
                                <li>Expecting to disable or require only one side - both are controlled by one shared <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">disabled</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">required</code>.</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Pitfalls &amp; Troubleshooting</h3>
                    <ul class="mt-2 list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">The range error is separate from each field's required error.</strong> An empty required field shows that Date Picker's own "Please select a date" message; a reversed range shows this component's own message instead. They're deliberately never conflated, so don't expect one <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">errorMessage</code> to cover both failure kinds.</li>
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">Clearing one date doesn't clear the other.</strong> Each Date Picker keeps its own independent Clear button; clearing the start date just removes the end picker's live minimum, it doesn't touch the end value itself.</li>
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">A same-day range is valid.</strong> End equal to start is accepted, not treated as reversed - only end strictly before start is blocked.</li>
                    </ul>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Ashlar provides</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>A real &lt;fieldset&gt;/&lt;legend&gt; grouping both dates under one accessible label, the same pattern Radio Group uses.</li>
                                <li>A submit-time block (not just a visual hint) when the range is reversed - the form genuinely does not submit, and focus moves to the end date so the problem is immediately actionable.</li>
                                <li>Everything Date Picker itself provides, twice over - see that page's own Accessibility section.</li>
                            </ul>
                        </div>
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>An <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">errorMessage</code> worth reading aloud if the default range-order wording doesn't fit your field.</li>
                                <li>Server-side validation of the submitted pair - the live coupling and submit-time check are both client-side UX, not a security boundary.</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div>
                    @template.layout.api-reference(apiParams = dateRangePickerApi)
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/date-picker" class="text-blue-700 hover:underline dark:text-blue-400">Date Picker</a>, the single-date building block this composes &middot;
                        <a href="${contextPath}/components/radio-group" class="text-blue-700 hover:underline dark:text-blue-400">Radio Group</a> for the same fieldset/legend grouping pattern &middot;
                        <a href="${contextPath}/components/form-field" class="text-blue-700 hover:underline dark:text-blue-400">Form Field</a> for wiring help/error text onto a custom control
                    </p>
                </div>
            </div>
        </section>
`
)
