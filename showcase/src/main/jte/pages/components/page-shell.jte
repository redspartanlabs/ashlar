@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Page Shell",
content = @`
    @template.layout.component-nav(currentSlug = "page-shell", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Page Shell</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The outer layout boundary for a page's own content - readable width, horizontal padding, and vertical spacing between top-level blocks. No surface, no JavaScript, no opinion about what's inside.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">A full page composition</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Page Shell wrapping a Page Header and several Page Sections, the way a real application page would compose them. The dashed border below marks the shell's own boundary for this showcase only - it isn't part of Page Shell itself.</p>
                    <div class="mt-3 rounded-lg border border-dashed border-slate-300 bg-slate-50 dark:border-slate-600 dark:bg-slate-950/40">
                        @template.ashlar.utilities.page-shell(
                        content = @`
                            @template.ashlar.utilities.page-header(
                                title = "Dashboard",
                                description = "Overview of system activity.",
                                actions = @`@template.ashlar.components.button(text = "New report", type = "Primary")`
                            )
                            @template.ashlar.utilities.page-section(
                                title = "Account Information",
                                description = "Manage the account details for this user.",
                                content = @`
                                    @template.ashlar.components.card(
                                        body = @`<p class="text-sm text-slate-600 dark:text-slate-300">Name, email, and role settings live here.</p>`
                                    )
                                `
                            )
                            @template.ashlar.utilities.page-section(
                                title = "Recent Activity",
                                content = @`
                                    @template.ashlar.components.card(
                                        body = @`<p class="text-sm text-slate-600 dark:text-slate-300">Dana Whitfield updated billing contact - 2 hours ago.</p>`
                                    )
                                `
                            )
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Wide content stays constrained</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Page Shell caps content at a comfortable reading width even when the viewport is much wider - the dashed line marks the shell's own edge.</p>
                    <div class="rounded-lg border border-dashed border-slate-300 bg-slate-50 dark:border-slate-600 dark:bg-slate-950/40">
                        @template.ashlar.utilities.page-shell(
                        content = @`
                            @template.ashlar.utilities.page-section(
                                title = "Wide table",
                                content = @`
                                    <div class="overflow-x-auto rounded-lg border border-slate-200 dark:border-slate-700">
                                        <table class="w-full min-w-[720px] text-left text-sm">
                                            <thead class="bg-slate-100 dark:bg-slate-800">
                                                <tr>
                                                    <th class="px-4 py-2 font-medium text-slate-700 dark:text-slate-300">Account</th>
                                                    <th class="px-4 py-2 font-medium text-slate-700 dark:text-slate-300">Owner</th>
                                                    <th class="px-4 py-2 font-medium text-slate-700 dark:text-slate-300">Plan</th>
                                                    <th class="px-4 py-2 font-medium text-slate-700 dark:text-slate-300">Status</th>
                                                    <th class="px-4 py-2 font-medium text-slate-700 dark:text-slate-300">Renewal</th>
                                                </tr>
                                            </thead>
                                            <tbody class="divide-y divide-slate-200 dark:divide-slate-700">
                                                <tr>
                                                    <td class="px-4 py-2 text-slate-700 dark:text-slate-300">Acme Logistics</td>
                                                    <td class="px-4 py-2 text-slate-700 dark:text-slate-300">Dana Whitfield</td>
                                                    <td class="px-4 py-2 text-slate-700 dark:text-slate-300">Enterprise</td>
                                                    <td class="px-4 py-2 text-slate-700 dark:text-slate-300">Active</td>
                                                    <td class="px-4 py-2 text-slate-700 dark:text-slate-300">March 2027</td>
                                                </tr>
                                            </tbody>
                                        </table>
                                    </div>
                                `
                            )
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Narrow / mobile layout</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Horizontal padding shrinks at narrow widths but never disappears, and nothing overflows.</p>
                    <div class="max-w-xs rounded-lg border border-dashed border-slate-300 bg-slate-50 dark:border-slate-600 dark:bg-slate-950/40">
                        @template.ashlar.utilities.page-shell(
                        content = @`
                            @template.ashlar.utilities.page-header(
                                title = "Dashboard",
                                description = "Overview of system activity.",
                                actions = @`@template.ashlar.components.button(text = "New report", type = "Primary")`
                            )
                            @template.ashlar.utilities.page-section(
                                title = "Account Information",
                                content = @`
                                    @template.ashlar.components.card(
                                        body = @`<p class="text-sm text-slate-600 dark:text-slate-300">Name, email, and role settings live here.</p>`
                                    )
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
