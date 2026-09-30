@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Faceted Search",
content = @`
    @template.layout.component-nav(currentSlug = "faceted-search", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Faceted Search</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Narrow a dataset across several named dimensions. Native checkboxes inside a real fieldset, so the whole thing works with no JavaScript: the page owns the form, the application decides what the selected values mean.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">A single facet</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">One dimension, three values. The name is written once on the facet; every option submits under it.</p>
                    <div class="mt-3 max-w-xs rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.components.faceted-search(
                        content = @`
                            @template.ashlar.components.facet(
                                title = "Status",
                                name = "status",
                                content = @`
                                    @template.ashlar.components.facet-option(label = "Running", value = "running")
                                    @template.ashlar.components.facet-option(label = "Ready", value = "ready")
                                    @template.ashlar.components.facet-option(label = "Failed", value = "failed")
                                `
                            )
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Counts, selection and unavailable values</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Counts are supplied by the application - the component never calculates them, and announces "42 results" rather than reading the bare number into the label. "Maintenance" is disabled, and "Running" is rendered already checked, which is how an application renders the current filter state back.</p>
                    <div class="mt-3 max-w-xs rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.components.faceted-search(
                        content = @`
                            @template.ashlar.components.facet(
                                title = "Status",
                                name = "status",
                                content = @`
                                    @template.ashlar.components.facet-option(label = "Running", value = "running", count = 42, checked = true)
                                    @template.ashlar.components.facet-option(label = "Ready", value = "ready", count = 18)
                                    @template.ashlar.components.facet-option(label = "Failed", value = "failed", count = 3)
                                    @template.ashlar.components.facet-option(label = "Maintenance", value = "maintenance", count = 0, disabled = true)
                                `
                            )
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Several dimensions</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Each facet is its own field name, so a submission carries them separately: <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">?status=running&amp;machine=acb-01&amp;machine=acb-03&amp;type=batch</code>.</p>
                    <div class="mt-3 max-w-xs rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.components.faceted-search(
                        content = @`
                            @template.ashlar.components.facet(
                                title = "Status",
                                name = "status",
                                content = @`
                                    @template.ashlar.components.facet-option(label = "Running", value = "running", count = 42)
                                    @template.ashlar.components.facet-option(label = "Ready", value = "ready", count = 18)
                                    @template.ashlar.components.facet-option(label = "Failed", value = "failed", count = 3)
                                `
                            )
                            @template.ashlar.components.facet(
                                title = "Machine",
                                name = "machine",
                                content = @`
                                    @template.ashlar.components.facet-option(label = "ACB-01", value = "acb-01", count = 82)
                                    @template.ashlar.components.facet-option(label = "ACB-02", value = "acb-02", count = 61)
                                    @template.ashlar.components.facet-option(label = "ACB-03", value = "acb-03", count = 44)
                                    @template.ashlar.components.facet-option(label = "ACB-04", value = "acb-04", count = 12)
                                `
                            )
                            @template.ashlar.components.facet(
                                title = "Type",
                                name = "type",
                                content = @`
                                    @template.ashlar.components.facet-option(label = "Batch", value = "batch", count = 96)
                                    @template.ashlar.components.facet-option(label = "Interactive", value = "interactive", count = 71)
                                    @template.ashlar.components.facet-option(label = "Scheduled maintenance window", value = "scheduled", count = 8)
                                `
                            )
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Inside an application-owned form</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The component never renders a <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">&lt;form&gt;</code>, a submit button or a reset. The page does, using Form Actions and Button. Select a couple of values and submit: the browser puts them in the query string, which is the application's to read. It does not come back checked here because this demo page has no server-side filtering - a real page would render the current state back through <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">checked</code>.</p>
                    <div class="mt-3 max-w-xs rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        <form method="get" class="flex flex-col gap-4">
                            @template.ashlar.components.faceted-search(
                            content = @`
                                @template.ashlar.components.facet(
                                    title = "Status",
                                    name = "status",
                                    content = @`
                                        @template.ashlar.components.facet-option(label = "Running", value = "running", count = 42)
                                        @template.ashlar.components.facet-option(label = "Ready", value = "ready", count = 18)
                                    `
                                )
                                @template.ashlar.components.facet(
                                    title = "Machine",
                                    name = "machine",
                                    content = @`
                                        @template.ashlar.components.facet-option(label = "ACB-01", value = "acb-01", count = 82)
                                        @template.ashlar.components.facet-option(label = "ACB-02", value = "acb-02", count = 61)
                                    `
                                )
                            `
                            )

                            @template.ashlar.utilities.form-actions(
                            content = @`
                                @template.ashlar.components.button(text = "Clear", type = "Ghost", buttonType = "reset")
                                @template.ashlar.components.button(text = "Apply filters", type = "Primary", buttonType = "submit")
                            `
                            )
                        </form>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Beside results</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The usual composition: filters in a column, results beside them. The component has no surface of its own, so the page decides - here a Card. Below the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">lg</code> breakpoint the column stacks above the results.</p>
                    <div class="mt-3 grid gap-6 lg:grid-cols-[15rem_1fr]">
                        <form method="get">
                            @template.ashlar.components.card(
                            title = "Filters",
                            body = @`
                                @template.ashlar.components.faceted-search(
                                content = @`
                                    @template.ashlar.components.facet(
                                        title = "Status",
                                        name = "status",
                                        content = @`
                                            @template.ashlar.components.facet-option(label = "Running", value = "running", count = 42, checked = true)
                                            @template.ashlar.components.facet-option(label = "Ready", value = "ready", count = 18)
                                            @template.ashlar.components.facet-option(label = "Failed", value = "failed", count = 3)
                                        `
                                    )
                                    @template.ashlar.components.facet(
                                        title = "Machine",
                                        name = "machine",
                                        content = @`
                                            @template.ashlar.components.facet-option(label = "ACB-01", value = "acb-01", count = 82)
                                            @template.ashlar.components.facet-option(label = "ACB-02", value = "acb-02", count = 61)
                                        `
                                    )
                                `
                                )
                            `,
                            footer = @`
                                @template.ashlar.utilities.form-actions(
                                    content = @`@template.ashlar.components.button(text = "Apply", type = "Primary", buttonType = "submit", size = "Small")`
                                )
                            `
                            )
                        </form>

                        <%-- min-w-0: a grid item's min-width defaults to auto,
                             meaning "never shrink below my content's minimum".
                             The Table in this column has a min-content width
                             of its own, so without this the results column
                             refuses to narrow past it and pushes the whole
                             page sideways instead - the same flexbox/grid rule
                             list-item.jte and faceted-search.jte already cite
                             for their own min-w-0. The Table's own scroller
                             handles the width once the column is allowed to
                             narrow. --%>
                        <div class="min-w-0">
                            @template.ashlar.utilities.status-line(content = @`42 jobs match the current filters.`)
                            <div class="mt-3">
                                @template.ashlar.components.table(
                                caption = "Jobs matching the current filters",
                                columns = @`
                                    @template.ashlar.components.table-column(label = "Job", sortable = true)
                                    @template.ashlar.components.table-column(label = "Machine", sortable = true)
                                    @template.ashlar.components.table-column(label = "Status")
                                `,
                                rows = @`
                                    @template.ashlar.components.table-row(cells = @`
                                        @template.ashlar.components.table-cell(text = "JH-2026-0412", header = true, mono = true, nowrap = true)
                                        @template.ashlar.components.table-cell(text = "ACB-01", mono = true, nowrap = true)
                                        @template.ashlar.components.table-cell(content = @`@template.ashlar.components.badge(text = "Running", variant = "Info")`)
                                    `)
                                    @template.ashlar.components.table-row(cells = @`
                                        @template.ashlar.components.table-cell(text = "JH-2026-0410", header = true, mono = true, nowrap = true)
                                        @template.ashlar.components.table-cell(text = "ACB-01", mono = true, nowrap = true)
                                        @template.ashlar.components.table-cell(content = @`@template.ashlar.components.badge(text = "Running", variant = "Info")`)
                                    `)
                                    @template.ashlar.components.table-row(cells = @`
                                        @template.ashlar.components.table-cell(text = "JH-2026-0407", header = true, mono = true, nowrap = true)
                                        @template.ashlar.components.table-cell(text = "ACB-02", mono = true, nowrap = true)
                                        @template.ashlar.components.table-cell(content = @`@template.ashlar.components.badge(text = "Running", variant = "Info")`)
                                    `)
                                `
                                )
                            </div>
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Compact on a phone</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Faceted Search deliberately contains no drawer or modal of its own. A page that needs filters out of the way on a small screen puts the same markup inside the library's existing Drawer - no new mechanism, and the facets behave identically in both places.</p>
                    <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.components.button(text = "Filters (1)", type = "Secondary", attributes = "data-drawer-open=\"facetDrawer\"")

                        @template.ashlar.components.drawer(
                        id = "facetDrawer",
                        title = "Filters",
                        body = @`
                            <form method="get" id="facetDrawerForm">
                                @template.ashlar.components.faceted-search(
                                content = @`
                                    @template.ashlar.components.facet(
                                        title = "Status",
                                        name = "status",
                                        content = @`
                                            @template.ashlar.components.facet-option(label = "Running", value = "running", count = 42, checked = true)
                                            @template.ashlar.components.facet-option(label = "Ready", value = "ready", count = 18)
                                            @template.ashlar.components.facet-option(label = "Failed", value = "failed", count = 3)
                                        `
                                    )
                                    @template.ashlar.components.facet(
                                        title = "Machine",
                                        name = "machine",
                                        content = @`
                                            @template.ashlar.components.facet-option(label = "ACB-01", value = "acb-01", count = 82)
                                            @template.ashlar.components.facet-option(label = "ACB-02", value = "acb-02", count = 61)
                                            @template.ashlar.components.facet-option(label = "ACB-03", value = "acb-03", count = 44)
                                        `
                                    )
                                `
                                )
                            </form>
                        `,
                        footer = @`
                            @template.ashlar.utilities.form-actions(
                                content = @`
                                    @template.ashlar.components.button(text = "Clear", type = "Ghost", buttonType = "reset", attributes = "form=\"facetDrawerForm\"")
                                    @template.ashlar.components.button(text = "Apply filters", type = "Primary", buttonType = "submit", attributes = "form=\"facetDrawerForm\"")
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
