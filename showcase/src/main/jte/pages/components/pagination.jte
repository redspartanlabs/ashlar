@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Pagination",
content = @`
    @template.layout.component-nav(currentSlug = "pagination", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Pagination</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Semantic navigation between pages of a larger result set - no JavaScript, no URL construction, no page-count calculation. The caller supplies every label, href, and current flag; the component only renders them.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic pagination</h3>
                    <div class="mt-3">
                        @template.ashlar.components.pagination(
                        items = @`
                            @template.ashlar.components.pagination-item(label = "1", href = "?page=1")
                            @template.ashlar.components.pagination-item(label = "2", current = true)
                            @template.ashlar.components.pagination-item(label = "3", href = "?page=3")
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Previous / Next</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The first example is on page 1, so Previous has no <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">href</code> and renders disabled. The second is mid-range, so both controls are active.</p>
                    <div class="mt-3 flex flex-col gap-3">
                        @template.ashlar.components.pagination(
                        items = @`
                            @template.ashlar.components.pagination-item(label = "Previous")
                            @template.ashlar.components.pagination-item(label = "1", current = true)
                            @template.ashlar.components.pagination-item(label = "2", href = "?page=2")
                            @template.ashlar.components.pagination-item(label = "3", href = "?page=3")
                            @template.ashlar.components.pagination-item(label = "Next", href = "?page=2")
                        `
                        )
                        @template.ashlar.components.pagination(
                        items = @`
                            @template.ashlar.components.pagination-item(label = "Previous", href = "?page=1")
                            @template.ashlar.components.pagination-item(label = "1", href = "?page=1")
                            @template.ashlar.components.pagination-item(label = "2", current = true)
                            @template.ashlar.components.pagination-item(label = "3", href = "?page=3")
                            @template.ashlar.components.pagination-item(label = "Next", href = "?page=3")
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Larger page range</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The application decides which numbers and ellipses to show - Pagination just renders whatever items it's given.</p>
                    <div class="mt-3">
                        @template.ashlar.components.pagination(
                        items = @`
                            @template.ashlar.components.pagination-item(label = "Previous", href = "?page=3")
                            @template.ashlar.components.pagination-item(label = "1", href = "?page=1")
                            @template.ashlar.components.pagination-item(label = "2", href = "?page=2")
                            @template.ashlar.components.pagination-item(label = "3", href = "?page=3")
                            @template.ashlar.components.pagination-item(label = "4", current = true)
                            @template.ashlar.components.pagination-item(label = "5", href = "?page=5")
                            @template.ashlar.components.pagination-item(label = "…")
                            @template.ashlar.components.pagination-item(label = "10", href = "?page=10")
                            @template.ashlar.components.pagination-item(label = "Next", href = "?page=5")
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Beneath a data table</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Pagination is a separate component from Data Table - it composes underneath a result set rather than owning it. Links here are static demonstration hrefs, not real navigation.</p>
                    <div class="mt-3 overflow-hidden rounded-xl border border-slate-200 dark:border-slate-700">
                        <div class="overflow-x-auto">
                        <table class="w-full text-left text-sm">
                            <thead class="border-b border-slate-200 bg-slate-50 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:border-slate-700 dark:bg-slate-800/50 dark:text-slate-400">
                                <tr>
                                    <th class="px-4 py-2.5">Account</th>
                                    <th class="px-4 py-2.5">Plan</th>
                                    <th class="px-4 py-2.5">Status</th>
                                </tr>
                            </thead>
                            <tbody class="divide-y divide-slate-200 text-slate-700 dark:divide-slate-700 dark:text-slate-300">
                                <tr>
                                    <td class="px-4 py-2.5">Acme Manufacturing</td>
                                    <td class="px-4 py-2.5">Enterprise</td>
                                    <td class="px-4 py-2.5">Active</td>
                                </tr>
                                <tr>
                                    <td class="px-4 py-2.5">Kestrel Aerospace</td>
                                    <td class="px-4 py-2.5">Enterprise</td>
                                    <td class="px-4 py-2.5">Active</td>
                                </tr>
                                <tr>
                                    <td class="px-4 py-2.5">Bramble Logistics</td>
                                    <td class="px-4 py-2.5">Standard</td>
                                    <td class="px-4 py-2.5">Trial</td>
                                </tr>
                            </tbody>
                        </table>
                        </div>
                        <div class="flex flex-wrap items-center justify-between gap-3 border-t border-slate-200 px-4 py-3 dark:border-slate-700">
                            <p class="text-sm text-slate-500 dark:text-slate-400">Showing 1-3 of 24 accounts</p>
                            @template.ashlar.components.pagination(
                            items = @`
                                @template.ashlar.components.pagination-item(label = "Previous")
                                @template.ashlar.components.pagination-item(label = "1", current = true)
                                @template.ashlar.components.pagination-item(label = "2", href = "?page=2")
                                @template.ashlar.components.pagination-item(label = "3", href = "?page=3")
                                @template.ashlar.components.pagination-item(label = "…")
                                @template.ashlar.components.pagination-item(label = "8", href = "?page=8")
                                @template.ashlar.components.pagination-item(label = "Next", href = "?page=2")
                            `
                            )
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Multiple instances stay independent</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">No ids, no JavaScript, no shared state - each is just its own rendered markup.</p>
                    <div class="mt-3 grid gap-4 sm:grid-cols-2">
                        <div>
                            <p class="mb-2 text-sm font-medium text-slate-700 dark:text-slate-300">Search results</p>
                            @template.ashlar.components.pagination(
                            items = @`
                                @template.ashlar.components.pagination-item(label = "1", current = true)
                                @template.ashlar.components.pagination-item(label = "2", href = "?page=2")
                            `
                            )
                        </div>
                        <div>
                            <p class="mb-2 text-sm font-medium text-slate-700 dark:text-slate-300">Audit log</p>
                            @template.ashlar.components.pagination(
                            items = @`
                                @template.ashlar.components.pagination-item(label = "1", href = "?page=1")
                                @template.ashlar.components.pagination-item(label = "2", current = true)
                                @template.ashlar.components.pagination-item(label = "3", href = "?page=3")
                            `
                            )
                        </div>
                    </div>
                </div>
            </div>
        </section>
`
)
