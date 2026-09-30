@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Date Range Picker",
content = @`
    @template.layout.component-nav(currentSlug = "date-range-picker", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Date Range Picker</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Two independent, unmodified Date Pickers under one &lt;fieldset&gt; - picking a start date raises the end picker's own minimum (and vice versa) through its existing disabled-date rendering, so an out-of-order range is unavailable rather than something to reject after the fact.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic &amp; empty</h3>
                    <div class="max-w-xl">
                        @template.ashlar.components.date-range-picker(
                        startName = "rangeStart",
                        endName = "rangeEnd",
                        label = "Date Range"
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Preselected range &amp; help text</h3>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Same-day range</h3>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Min/max bounds &amp; disabled</h3>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Server-reported error</h3>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Required validation, multiple instances &amp; form submission</h3>
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
        </section>
`
)
