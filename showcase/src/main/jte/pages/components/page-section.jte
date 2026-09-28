@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Page Section",
content = @`
    @template.layout.component-nav(currentSlug = "page-section", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Page Section</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A layout utility for grouping a page's content under a subsection heading. No surface of its own, no JavaScript - it owns the heading-to-content spacing, not the content's appearance.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Title + content</h3>
                    <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.page-section(
                        title = "Account Information",
                        content = @`<p class="text-sm text-slate-600 dark:text-slate-300">Name, email, and role settings live here.</p>`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Title + description + content</h3>
                    <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.page-section(
                        title = "Account Information",
                        description = "Manage the account details for this user.",
                        content = @`<p class="text-sm text-slate-600 dark:text-slate-300">Name, email, and role settings live here.</p>`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Multiple sections on one page</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Each section owns its own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">&lt;h2&gt;</code>, forming a flat list of subsections beneath a page's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">&lt;h1&gt;</code>.</p>
                    <div class="mt-3 flex flex-col gap-8 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.page-section(
                        title = "Account Information",
                        description = "Manage the account details for this user.",
                        content = @`<p class="text-sm text-slate-600 dark:text-slate-300">Name, email, and role settings live here.</p>`
                        )
                        @template.ashlar.utilities.page-section(
                        title = "Preferences",
                        description = "Configure application preferences.",
                        content = @`<p class="text-sm text-slate-600 dark:text-slate-300">Notification and display settings live here.</p>`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Section containing an existing component</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Page Section never adds a surface of its own - a Card placed inside still looks like a Card.</p>
                    <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.page-section(
                        title = "Recent Activity",
                        description = "The latest activity for this account.",
                        content = @`
                            @template.ashlar.components.card(
                                body = @`<p class="text-sm text-slate-600 dark:text-slate-300">Dana Whitfield updated billing contact - 2 hours ago.</p>`
                            )
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Long title</h3>
                    <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.page-section(
                        title = "Quarterly Compliance and Access Review for Enterprise Accounts",
                        content = @`<p class="text-sm text-slate-600 dark:text-slate-300">Section content goes here.</p>`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Long description</h3>
                    <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.page-section(
                        title = "Audit Log",
                        description = "A complete, chronological record of every access change, permission grant, and administrative action taken across all enterprise accounts, retained for the duration required by your organization's compliance policy.",
                        content = @`<p class="text-sm text-slate-600 dark:text-slate-300">Section content goes here.</p>`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Narrow / mobile layout</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">No fixed widths - the section and its content occupy the available width at any viewport size.</p>
                    <div class="mt-3 max-w-xs rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.page-section(
                        title = "Account Information",
                        description = "Manage the account details for this user.",
                        content = @`<p class="text-sm text-slate-600 dark:text-slate-300">Name, email, and role settings live here.</p>`
                        )
                    </div>
                </div>
            </div>
        </section>
`
)
