@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Status Line",
content = @`
    @template.layout.component-nav(currentSlug = "status-line", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Status Line</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A single inline status/informational message - muted typography only, no surface, no JavaScript. Not a replacement for Alert or Badge.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic</h3>
                    <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.status-line(
                        content = @`Last updated September 21, 2026`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Longer message</h3>
                    <div class="mt-3 max-w-md rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.status-line(
                        content = @`No results matched your filters. Try widening the date range or removing one of the applied account filters.`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Rich content</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A message can contain a link or another existing component - Status Line never needs to understand it.</p>
                    <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.status-line(
                        content = @`Changes saved. <a href="/components/status-line" class="text-blue-700 hover:underline dark:text-blue-400">View audit log</a>.`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Alongside an existing component</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Status Line stays plain text-level while a Badge carries its own status semantics - the two are not competing for the same job.</p>
                    <div class="mt-3 flex flex-wrap items-center gap-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.components.badge(text = "Operational", variant = "Success")
                        @template.ashlar.utilities.status-line(
                        content = @`All systems normal as of 9:41 AM.`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">In a Page Section, above a Data Table's Form Actions</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A record count is exactly the kind of small informational line this utility exists for.</p>
                    <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.page-section(
                        title = "Accounts",
                        description = "Enterprise accounts currently on file.",
                        content = @`
                            @template.ashlar.components.card(
                            body = @`
                                <div class="flex flex-col items-start gap-3">
                                    @template.ashlar.utilities.status-line(
                                    content = @`Showing 24 of 128 records.`
                                    )
                                    @template.ashlar.components.badge(text = "3 items selected", variant = "Info")
                                </div>
                            `,
                            footer = @`
                                @template.ashlar.utilities.form-actions(
                                    content = @`
                                        @template.ashlar.components.button(text = "Export selected", type = "Secondary")
                                        @template.ashlar.components.button(text = "Archive selected", type = "Primary")
                                    `
                                )
                            `
                            )
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Narrow / mobile layout</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A long message wraps like any other paragraph text - no truncation, no fixed width.</p>
                    <div class="mt-3 max-w-xs rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.status-line(
                        content = @`No results matched your filters. Try widening the date range or removing one of the applied account filters.`
                        )
                    </div>
                </div>
            </div>
        </section>
`
)
