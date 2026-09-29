@import com.redspartanlabs.ashlar.icon.Icon

@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Timeline",
content = @`
    @template.layout.component-nav(currentSlug = "timeline", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Timeline</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A vertical, chronological sequence of entries - release history, activity feeds, workflow progression. No JavaScript: the connector between entries is drawn per-item in pure CSS.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Release history</h3>
                    <div class="mt-3 max-w-md">
                        @template.ashlar.components.timeline(
                        items = @`
                            @template.ashlar.components.timeline-item(
                                date = "March 2026",
                                title = "Version 2.4.0",
                                content = @`<p>Adds bulk export and improves Data Table sort performance.</p>`
                            )
                            @template.ashlar.components.timeline-item(
                                date = "January 2026",
                                title = "Version 2.3.0",
                                content = @`<p>Introduces role-based approval workflows for high-value transactions.</p>`
                            )
                            @template.ashlar.components.timeline-item(
                                date = "October 2025",
                                title = "Version 2.2.0",
                                content = @`<p>Moves the platform from a single-region deployment to a multi-region architecture.</p>`
                            )
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Order status, with state and icons</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400"><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">variant</code> colors the marker; state is also announced to assistive tech, never color-only.</p>
                    <div class="mt-3 max-w-md">
                        @template.ashlar.components.timeline(
                        items = @`
                            @template.ashlar.components.timeline-item(
                                date = "Sep 21, 9:02 AM",
                                title = "Order placed",
                                variant = "Success",
                                icon = Icon.CHECK
                            )
                            @template.ashlar.components.timeline-item(
                                date = "Sep 21, 11:40 AM",
                                title = "Payment confirmed",
                                variant = "Success",
                                icon = Icon.CHECK
                            )
                            @template.ashlar.components.timeline-item(
                                date = "Sep 22, 8:15 AM",
                                title = "Shipment delayed",
                                content = @`<p>Carrier reported a warehouse delay. New estimate: Sep 24.</p>`,
                                variant = "Warning"
                            )
                            @template.ashlar.components.timeline-item(
                                title = "Out for delivery",
                                variant = "Neutral"
                            )
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Without dates</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A workflow where sequence matters more than timing - <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">date</code> is entirely optional.</p>
                    <div class="mt-3 max-w-md">
                        @template.ashlar.components.timeline(
                        items = @`
                            @template.ashlar.components.timeline-item(title = "Request submitted")
                            @template.ashlar.components.timeline-item(title = "Manager review")
                            @template.ashlar.components.timeline-item(title = "Access granted")
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Long content wraps</h3>
                    <div class="mt-3 max-w-md">
                        @template.ashlar.components.timeline(
                        items = @`
                            @template.ashlar.components.timeline-item(
                                date = "Q3 2026",
                                title = "Quarterly compliance report for enterprise accounts",
                                content = @`<p>Generated automatically on the first business day of each quarter and distributed to every account owner on file, including a full audit trail of access changes made during the period.</p>`
                            )
                            @template.ashlar.components.timeline-item(
                                date = "Q2 2026",
                                title = "Previous report"
                            )
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">In context</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Composed inside a Card, the way it would appear in a real page.</p>
                    <div class="mt-3 max-w-md">
                        @template.ashlar.components.card(
                        title = "Account activity",
                        body = @`
                            @template.ashlar.components.timeline(
                            items = @`
                                @template.ashlar.components.timeline-item(date = "2 hours ago", title = "Dana Whitfield updated billing contact", icon = Icon.TAG)
                                @template.ashlar.components.timeline-item(date = "Yesterday", title = "New API key generated", icon = Icon.TAG)
                                @template.ashlar.components.timeline-item(date = "3 days ago", title = "Account created", variant = "Success", icon = Icon.CHECK)
                            `
                            )
                        `
                        )
                    </div>
                </div>
            </div>
        </section>
`
)
