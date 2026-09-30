@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Page Header",
content = @`
    @template.layout.component-nav(currentSlug = "page-header", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Page Header</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A layout utility for a page's own title, optional description, and optional application-supplied actions. No surface of its own, no JavaScript - it owns structure, not appearance or behavior.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Title only</h3>
                    <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.page-header(
                        title = "Dashboard"
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Title + description</h3>
                    <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.page-header(
                        title = "Dashboard",
                        description = "Overview of system activity."
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Title + actions</h3>
                    <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.page-header(
                        title = "Dashboard",
                        actions = @`@template.ashlar.components.button(text = "New report", type = "Primary")`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Title + description + actions</h3>
                    <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.page-header(
                        title = "Dashboard",
                        description = "Overview of system activity.",
                        actions = @`@template.ashlar.components.button(text = "New report", type = "Primary")`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Multiple actions</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Actions are supplied by the caller - Page Header only arranges them and lets them wrap.</p>
                    <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.page-header(
                        title = "Accounts",
                        description = "Manage enterprise accounts and their access.",
                        actions = @`
                            @template.ashlar.components.button(text = "Export", type = "Secondary")
                            @template.ashlar.components.button(text = "Invite member", type = "Ghost")
                            @template.ashlar.components.button(text = "New account", type = "Primary")
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Long title</h3>
                    <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.page-header(
                        title = "Quarterly Compliance and Access Review for Enterprise Accounts",
                        actions = @`@template.ashlar.components.button(text = "Export", type = "Secondary")`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Long description</h3>
                    <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.page-header(
                        title = "Audit Log",
                        description = "A complete, chronological record of every access change, permission grant, and administrative action taken across all enterprise accounts, retained for the duration required by your organization's compliance policy."
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Narrow / mobile layout</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Below the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">sm</code> breakpoint the actions move below the heading and wrap instead of overflowing.</p>
                    <div class="mt-3 max-w-xs rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.page-header(
                        title = "Dashboard",
                        description = "Overview of system activity.",
                        actions = @`
                            @template.ashlar.components.button(text = "Export", type = "Secondary")
                            @template.ashlar.components.button(text = "New report", type = "Primary")
                        `
                        )
                    </div>
                </div>
            </div>
        </section>
`
)
