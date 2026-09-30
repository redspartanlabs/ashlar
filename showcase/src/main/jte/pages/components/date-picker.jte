@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Date Picker",
content = @`
    @template.layout.component-nav(currentSlug = "date-picker", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Date Picker</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A reusable calendar date field: click to open, type nothing, always submits an unambiguous ISO date.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Default</h3>
                    <div class="mt-3 max-w-sm">
                        @template.ashlar.components.date-picker(
                        name = "defaultDate",
                        label = "Date"
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Display formats</h3>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Min / max range</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Restricted to 2020&ndash;2030. Navigate outside that range to see disabled days.</p>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Disabled &amp; required</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Try submitting without picking a date - Required shows an error; Disabled can't block submission at all.</p>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Multiple independent instances</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A date range made of two entirely separate, both required, component instances - submitting with only one filled in should only error the other.</p>
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
        </section>
`
)
