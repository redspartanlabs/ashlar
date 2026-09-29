@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Stat / Metric Card",
content = @`
    @template.layout.component-nav(currentSlug = "stat-card", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Stat / Metric Card</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A single KPI-style metric in its own small surface - dashboards, result pages, summary sections. No JavaScript, no formatting or calculation: the caller supplies the value exactly as it should read.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic</h3>
                    <div class="mt-3 max-w-xs">
                        @template.ashlar.components.stat-card(
                        label = "Active users",
                        value = "2,481"
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">With supporting text</h3>
                    <div class="mt-3 max-w-xs">
                        @template.ashlar.components.stat-card(
                        label = "Records migrated",
                        value = "12,481",
                        description = "Across the selected date range"
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">With a trend badge</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The component draws no conclusions about direction - the caller decides whether a change is good or bad and picks the Badge variant accordingly.</p>
                    <div class="mt-3 grid gap-4 sm:grid-cols-2">
                        @template.ashlar.components.stat-card(
                        label = "Revenue",
                        value = "$128.4K",
                        description = "vs. $114.7K last month",
                        change = @`@template.ashlar.components.badge(text = "+12%", variant = "Success")`
                        )
                        @template.ashlar.components.stat-card(
                        label = "Avg. response time",
                        value = "142ms",
                        description = "vs. 118ms last month",
                        change = @`@template.ashlar.components.badge(text = "+24ms", variant = "Danger")`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Non-numeric value</h3>
                    <div class="mt-3 max-w-xs">
                        @template.ashlar.components.stat-card(
                        label = "System status",
                        value = "Operational",
                        change = @`@template.ashlar.components.badge(text = "All systems normal", variant = "Success")`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Long label and value</h3>
                    <div class="mt-3 max-w-xs">
                        @template.ashlar.components.stat-card(
                        label = "Quarterly compliance reports generated for enterprise accounts",
                        value = "1,204,981",
                        description = "Includes archived and superseded reports"
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">In a dashboard layout</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Several stat cards side by side, the way they'd appear at the top of a real dashboard.</p>
                    <div class="mt-3 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                        @template.ashlar.components.stat-card(
                        label = "Revenue",
                        value = "$128.4K",
                        change = @`@template.ashlar.components.badge(text = "+12%", variant = "Success")`
                        )
                        @template.ashlar.components.stat-card(
                        label = "Active accounts",
                        value = "342"
                        )
                        @template.ashlar.components.stat-card(
                        label = "Open support tickets",
                        value = "18",
                        change = @`@template.ashlar.components.badge(text = "+5", variant = "Warning")`
                        )
                        @template.ashlar.components.stat-card(
                        label = "Uptime",
                        value = "99.98%"
                        )
                    </div>
                </div>
            </div>
        </section>
`
)
