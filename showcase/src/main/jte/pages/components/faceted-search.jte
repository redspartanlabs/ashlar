@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import com.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

<%-- Composition is used heavily here rather than being one small
     section - this component has no surface, form, or results of its
     own, so real usage is almost entirely about what it's combined
     with. That's also why the two Composition examples below (results
     beside it; the same markup moved into a Drawer) don't carry their
     own source disclosure: both are already explained in full by the
     prose directly above them, and both recombine facet/facet-option
     shapes whose own exact source is already shown, with a disclosure,
     earlier on this same page under Variants & States. --%>
!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.components.faceted-search(\n"
        + "    content = @" + backtick + "\n"
        + "        @template.ashlar.components.facet(\n"
        + "            title = \"Status\",\n"
        + "            name = \"status\",\n"
        + "            content = @" + backtick + "\n"
        + "                @template.ashlar.components.facet-option(label = \"Running\", value = \"running\")\n"
        + "                @template.ashlar.components.facet-option(label = \"Failed\", value = \"failed\")\n"
        + "            " + backtick + "\n"
        + "        )\n"
        + "    " + backtick + "\n"
        + ")";}
!{String countsCode = "@template.ashlar.components.faceted-search(\n"
        + "    content = @" + backtick + "\n"
        + "        @template.ashlar.components.facet(\n"
        + "            title = \"Status\",\n"
        + "            name = \"status\",\n"
        + "            content = @" + backtick + "\n"
        + "                @template.ashlar.components.facet-option(label = \"Running\", value = \"running\", count = 42, checked = true)\n"
        + "                @template.ashlar.components.facet-option(label = \"Ready\", value = \"ready\", count = 18)\n"
        + "                @template.ashlar.components.facet-option(label = \"Failed\", value = \"failed\", count = 3)\n"
        + "                @template.ashlar.components.facet-option(label = \"Maintenance\", value = \"maintenance\", count = 0, disabled = true)\n"
        + "            " + backtick + "\n"
        + "        )\n"
        + "    " + backtick + "\n"
        + ")";}
!{String dimensionsCode = "@template.ashlar.components.faceted-search(\n"
        + "    content = @" + backtick + "\n"
        + "        @template.ashlar.components.facet(\n"
        + "            title = \"Status\",\n"
        + "            name = \"status\",\n"
        + "            content = @" + backtick + "\n"
        + "                @template.ashlar.components.facet-option(label = \"Running\", value = \"running\", count = 42)\n"
        + "                @template.ashlar.components.facet-option(label = \"Ready\", value = \"ready\", count = 18)\n"
        + "                @template.ashlar.components.facet-option(label = \"Failed\", value = \"failed\", count = 3)\n"
        + "            " + backtick + "\n"
        + "        )\n"
        + "        @template.ashlar.components.facet(\n"
        + "            title = \"Machine\",\n"
        + "            name = \"machine\",\n"
        + "            content = @" + backtick + "\n"
        + "                @template.ashlar.components.facet-option(label = \"ACB-01\", value = \"acb-01\", count = 82)\n"
        + "                @template.ashlar.components.facet-option(label = \"ACB-02\", value = \"acb-02\", count = 61)\n"
        + "                @template.ashlar.components.facet-option(label = \"ACB-03\", value = \"acb-03\", count = 44)\n"
        + "                @template.ashlar.components.facet-option(label = \"ACB-04\", value = \"acb-04\", count = 12)\n"
        + "            " + backtick + "\n"
        + "        )\n"
        + "        @template.ashlar.components.facet(\n"
        + "            title = \"Type\",\n"
        + "            name = \"type\",\n"
        + "            content = @" + backtick + "\n"
        + "                @template.ashlar.components.facet-option(label = \"Batch\", value = \"batch\", count = 96)\n"
        + "                @template.ashlar.components.facet-option(label = \"Interactive\", value = \"interactive\", count = 71)\n"
        + "                @template.ashlar.components.facet-option(label = \"Scheduled maintenance window\", value = \"scheduled\", count = 8)\n"
        + "            " + backtick + "\n"
        + "        )\n"
        + "    " + backtick + "\n"
        + ")";}
!{String ownedFormCode = "<form method=\"get\">\n"
        + "    @template.ashlar.components.faceted-search(\n"
        + "        content = @" + backtick + "\n"
        + "            @template.ashlar.components.facet(\n"
        + "                title = \"Status\",\n"
        + "                name = \"status\",\n"
        + "                content = @" + backtick + "\n"
        + "                    @template.ashlar.components.facet-option(label = \"Running\", value = \"running\", count = 42)\n"
        + "                    @template.ashlar.components.facet-option(label = \"Ready\", value = \"ready\", count = 18)\n"
        + "                " + backtick + "\n"
        + "            )\n"
        + "            @template.ashlar.components.facet(\n"
        + "                title = \"Machine\",\n"
        + "                name = \"machine\",\n"
        + "                content = @" + backtick + "\n"
        + "                    @template.ashlar.components.facet-option(label = \"ACB-01\", value = \"acb-01\", count = 82)\n"
        + "                    @template.ashlar.components.facet-option(label = \"ACB-02\", value = \"acb-02\", count = 61)\n"
        + "                " + backtick + "\n"
        + "            )\n"
        + "        " + backtick + "\n"
        + "    )\n"
        + "\n"
        + "    @template.ashlar.utilities.form-actions(\n"
        + "        content = @" + backtick + "\n"
        + "            @template.ashlar.components.button(text = \"Clear\", type = \"Ghost\", buttonType = \"reset\")\n"
        + "            @template.ashlar.components.button(text = \"Apply filters\", type = \"Primary\", buttonType = \"submit\")\n"
        + "        " + backtick + "\n"
        + "    )\n"
        + "</form>";}
!{List<ApiParam> facetedSearchApi = List.of(
    ApiParam.required("content", "gg.jte.Content", "One or more facet calls. No form, button, or results - the page supplies all of that.")
);}
!{List<ApiParam> facetApi = List.of(
    ApiParam.required("title", "String", "The <legend> text - the accessible name for every option inside, with no extra aria- attribute needed."),
    ApiParam.required("name", "String", "Set once here; every option inside submits under this field name automatically via FacetContext."),
    ApiParam.required("content", "gg.jte.Content", "One or more facet-option calls.")
);}
!{List<ApiParam> facetOptionApi = List.of(
    ApiParam.required("label", "String", "The visible, clickable row text."),
    ApiParam.required("value", "String", "The value this checkbox submits when checked."),
    ApiParam.optional("count", "Integer", "null", "Supplied by the application, rendered exactly as given. Shown visually as a bare number (aria-hidden) and announced separately as \"N results\"."),
    ApiParam.optional("checked", "boolean", "false", "How the application renders the current filter state back - this component never remembers state itself."),
    ApiParam.optional("disabled", "boolean", "false", "Visible but not selectable - e.g. a value with zero current matches.")
);}
!{List<PageAnchor> sections = List.of(
    new PageAnchor("quick-start", "Quick Start"),
    new PageAnchor("how-to-use", "How to Use"),
    new PageAnchor("variants", "Variants & States"),
    new PageAnchor("composition", "Composition"),
    new PageAnchor("best-practices", "Best Practices"),
    new PageAnchor("pitfalls", "Pitfalls"),
    new PageAnchor("accessibility", "Accessibility"),
    new PageAnchor("api-reference", "API Reference"),
    new PageAnchor("related", "Related Components")
);}

@template.layout.page(
contextPath = contextPath,
title = "Faceted Search",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "faceted-search", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Narrow a dataset across several named dimensions. Native checkboxes inside a real fieldset, so the whole thing works with no JavaScript: the page owns the form, the application decides what the selected values mean.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3 max-w-xs rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                    @template.ashlar.components.faceted-search(
                    content = @`
                        @template.ashlar.components.facet(
                            title = "Status",
                            name = "quickStartStatus",
                            content = @`
                                @template.ashlar.components.facet-option(label = "Running", value = "running")
                                @template.ashlar.components.facet-option(label = "Failed", value = "failed")
                            `
                        )
                    `
                    )
                </div>
            `
            )
        </section>

        <section id="how-to-use" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "How to Use",
            content = @`<p class="text-sm text-slate-500 dark:text-slate-400">Reach for Faceted Search when visitors narrow a list by checking off values across more than one independent dimension (status, machine, type...) - not for a single free-text query, which is <a href="${contextPath}/components/search-input" class="text-blue-700 hover:underline dark:text-blue-400">Search Input</a>'s job, and not for a single mutually-exclusive choice, which has no dedicated facet type yet (see Pitfalls). This component only ever renders the checkboxes themselves - everything else on the page (the form, the submit/reset buttons, the results, the count) is assembled by you from other Ashlar pieces, demonstrated below.</p>`
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">A single facet</p>
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
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Counts, selection and unavailable values</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Counts are supplied by the application - the component never calculates them, and announces "42 results" rather than reading the bare number into the label. "Maintenance" is disabled, and "Running" is rendered already checked, which is how an application renders the current filter state back.</p>
                        @template.layout.example(
                        code = countsCode,
                        content = @`
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
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Several dimensions</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Each facet is its own field name, so a submission carries them separately: <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">?status=running&amp;machine=acb-01&amp;machine=acb-03&amp;type=batch</code>.</p>
                        @template.layout.example(
                        code = dimensionsCode,
                        content = @`
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
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Inside an application-owned form</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The component never renders a <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">&lt;form&gt;</code>, a submit button or a reset. The page does, using Form Actions and Button. Select a couple of values and submit: the browser puts them in the query string, which is the application's to read. It does not come back checked here because this demo page has no server-side filtering - a real page would render the current state back through <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">checked</code>.</p>
                        @template.layout.example(
                        code = ownedFormCode,
                        content = @`
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
                        `
                        )
                    </div>
                </div>
            `
            )
        </section>

        <section id="composition" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Composition",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">This is the real subject of the component: it has no surface, no form, and no results list of its own, so every real page composes it with something else. Two compositions are worth seeing in full below - results beside it, and the same markup moved into a Drawer for small screens with zero changes to the facets themselves.</p>

                <div class="mt-4 flex flex-col gap-8">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Beside results</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The usual composition: filters in a column, results beside them. The component has no surface of its own, so the page decides - here a Card. Below the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">lg</code> breakpoint the column stacks above the results. The results column's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">min-w-0</code> is a real requirement, not decoration - without it, Table's own min-content width refuses to narrow and pushes the whole page sideways instead, the same flexbox/grid rule Faceted Search's own container cites for itself.</p>
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
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Compact on a phone</p>
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
            `
            )
        </section>

        <section id="best-practices" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Best Practices",
            content = @`
                <div class="grid gap-6 sm:grid-cols-2">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Do</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Render <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">checked</code> from the current request's query parameters, so a refreshed or shared URL reproduces the same filter state.</li>
                            <li>Give every facet on a page a distinct <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">name</code> - options only submit correctly grouped when the name is unique per dimension.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Avoid</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Nesting a facet-option outside any facet - it will render with no field name and never submit (see Pitfalls).</li>
                            <li>Reaching for this when there's only one dimension and no count/disabled need - a plain Checkbox Group may be simpler.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="pitfalls" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Pitfalls & Troubleshooting",
            content = @`
                <ul class="list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">A facet-option outside any facet silently never submits.</strong> It renders with <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">name=""</code> rather than an error or a stale name from elsewhere on the page - browsers don't submit a control with an empty name, so the checkbox exists but is quietly inert.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">There is no single-select facet.</strong> Every facet is multi-select checkboxes; a mutually-exclusive dimension isn't a variant of this component to configure, it's simply not built yet.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">Nothing here remembers selection between requests.</strong> <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">checked</code> has to be set by your own code from the incoming request every time, or a submitted filter will visually reset on the next page load even though the URL still has it.</li>
                </ul>
            `
            )
        </section>

        <section id="accessibility" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Accessibility",
            content = @`
                <div class="grid gap-6 sm:grid-cols-2">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Ashlar provides</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>A real &lt;fieldset&gt;/&lt;legend&gt; per facet, so each option group has a spoken name for free - no aria-label or aria-labelledby needed.</li>
                            <li>A count announced as "N results" rather than a bare number read into the label, avoiding the ambiguity of "Running 42".</li>
                            <li>Plain native checkboxes throughout - focus, Space-to-toggle, and disabled are all the browser's own.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>The enclosing &lt;form&gt;, its submit control, and any results/count feedback - none of that exists here.</li>
                            <li>A unique <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">name</code> per facet on the page.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "API Reference",
            content = @`
                <div class="flex flex-col gap-6">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">faceted-search</p>
                        @template.layout.api-reference(apiParams = facetedSearchApi)
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">facet</p>
                        @template.layout.api-reference(apiParams = facetApi)
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">facet-option</p>
                        @template.layout.api-reference(apiParams = facetOptionApi)
                    </div>
                </div>
            `
            )
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/search-input" class="text-blue-700 hover:underline dark:text-blue-400">Search Input</a> for a single free-text query instead of dimensioned filters &middot;
                    <a href="${contextPath}/components/table" class="text-blue-700 hover:underline dark:text-blue-400">Table</a> and
                    <a href="${contextPath}/components/data-table" class="text-blue-700 hover:underline dark:text-blue-400">Data Table</a> as the usual results neighbor &middot;
                    <a href="${contextPath}/components/drawer" class="text-blue-700 hover:underline dark:text-blue-400">Drawer</a> for the compact-on-mobile composition &middot;
                    <a href="${contextPath}/components/checkbox" class="text-blue-700 hover:underline dark:text-blue-400">Checkbox</a> for a single, unrelated boolean field
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "faceted-search", contextPath = contextPath)
    </div>
`
)
