@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import com.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

!{String quickStartCode = """
    @template.ashlar.components.date-picker(
        name = "startDate",
        label = "Start Date"
    )""";}
!{String formatsCode = """
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
    )""";}
!{String minMaxCode = """
    @template.ashlar.components.date-picker(
        name = "rangeDate",
        label = "Within 2020-2030",
        minDate = "2020-01-01",
        maxDate = "2030-12-31"
    )""";}
!{String disabledRequiredCode = """
    <form action="" method="get">
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
        @template.ashlar.components.button(text = "Submit", type = "Primary", buttonType = "submit")
    </form>""";}
!{String multipleInstancesCode = """
    <form action="" method="get">
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
        @template.ashlar.components.button(text = "Submit", type = "Primary", buttonType = "submit")
    </form>""";}
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
!{List<PageAnchor> sections = List.of(
    new PageAnchor("quick-start", "Quick Start"),
    new PageAnchor("how-to-use", "How to Use"),
    new PageAnchor("variants", "Variants & States"),
    new PageAnchor("composition", "Composition"),
    new PageAnchor("best-practices", "Best Practices"),
    new PageAnchor("pitfalls", "Pitfalls"),
    new PageAnchor("accessibility", "Accessibility"),
    new PageAnchor("api-reference", "API Reference"),
    new PageAnchor("related", "Related Components")
);}

@template.layout.page(
contextPath = contextPath,
title = "Date Picker",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "date-picker", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A reusable calendar date field: click to open, type nothing, always submits an unambiguous ISO date regardless of how it's displayed.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3 max-w-sm">
                    @template.ashlar.components.date-picker(name = "quickStartDate", label = "Start Date")
                </div>
            `
            )
        </section>

        <section id="how-to-use" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "How to Use",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Reach for Date Picker whenever you need a single, unambiguous calendar date and want to rule out free-text date parsing entirely - there is no typing involved, only clicking (or arrow-keying) a day. For two related dates that must stay in order (a start and an end), use <a href="${contextPath}/components/date-range-picker" class="text-blue-700 hover:underline dark:text-blue-400">Date Range Picker</a> instead of two independent Date Pickers - it couples their constraints for you (see that page's Composition section).</p>
            `
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
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
                        @template.layout.example(
                        code = formatsCode,
                        content = @`
                            <div class="grid gap-4 sm:grid-cols-2">
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
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Min / max range</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Restricted to 2020&ndash;2030. Navigate outside that range to see disabled days - and try opening the month/year views (click the header title) to see whole months and years disabled too.</p>
                        @template.layout.example(
                        code = minMaxCode,
                        content = @`
                            <div class="mt-3 max-w-sm">
                                @template.ashlar.components.date-picker(
                                name = "rangeDate",
                                label = "Within 2020-2030",
                                minDate = "2020-01-01",
                                maxDate = "2030-12-31"
                                )
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Disabled &amp; required</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Try submitting without picking a date - Required shows an error; Disabled can't block submission at all (see Pitfalls).</p>
                        @template.layout.example(
                        code = disabledRequiredCode,
                        content = @`
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
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Multiple independent instances</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A date range made of two entirely separate, both required, component instances - submitting with only one filled in should only error the other. This is the pattern <a href="${contextPath}/components/date-range-picker" class="text-blue-700 hover:underline dark:text-blue-400">Date Range Picker</a> replaces when the two dates need to stay in order; use two plain Date Pickers only when they're genuinely unrelated, as here.</p>
                        @template.layout.example(
                        code = multipleInstancesCode,
                        content = @`
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
                        `
                        )
                    </div>
                </div>
            `
            )
        </section>

        <section id="composition" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Composition",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Date Picker exposes its min/max constraints as live, externally-readable <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">data-min-date</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">data-max-date</code> attributes that its own JavaScript re-reads on every check rather than caching once. That's not incidental - it's the exact mechanism <a href="${contextPath}/components/date-range-picker" class="text-blue-700 hover:underline dark:text-blue-400">Date Range Picker</a> relies on to keep its two internal Date Pickers from ever allowing an out-of-order selection, without reimplementing any calendar logic of its own.</p>
            `
            )
        </section>

        <section id="best-practices" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Best Practices",
            content = @`
                <div class="grid gap-6 sm:grid-cols-2">
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
            `
            )
        </section>

        <section id="pitfalls" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Pitfalls & Troubleshooting",
            content = @`
                <ul class="list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300"><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">disabled</code> doesn't block submission on its own.</strong> It only prevents the trigger and clear button from being operated. A disabled field with a pre-set <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">value</code> still submits that value normally, exactly like a disabled native input would if it weren't disabled - this is ordinary HTML form behavior, not a bug.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">An unrecognized <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">format</code> string doesn't error.</strong> It silently falls back to MEDIUM ("MMM d, yyyy") rather than surfacing the typo.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">There is no month-only or year-only mode.</strong> Only the day/month/year views exist to navigate to a full date - picking just a month or year isn't a supported end state, by design.</li>
                </ul>
            `
            )
        </section>

        <section id="accessibility" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Accessibility",
            content = @`
                <div class="grid gap-6 sm:grid-cols-2">
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
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.layout.api-reference(apiParams = datePickerApi)
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/date-range-picker" class="text-blue-700 hover:underline dark:text-blue-400">Date Range Picker</a> for a coupled start/end pair &middot;
                    <a href="${contextPath}/components/form-field" class="text-blue-700 hover:underline dark:text-blue-400">Form Field</a> for wiring help/error text onto a custom control
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "date-picker", contextPath = contextPath)
    </div>
`
)
