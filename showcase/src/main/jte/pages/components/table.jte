@import dev.redspartanlabs.ashlar.showcase.table.SampleJobHeader
@import dev.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import dev.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

!{var jobs = SampleJobHeader.sample();}

<%-- Known, still-unaddressed ashlar/ documentation defect (discovered
     during the original audit, not fixed since it's outside this
     component-page scope): table.jte's own doc comment inside ashlar/
     describes a stale, pre-rewrite implementation - it claims Data
     Table takes a List<Map<String,String>>, serializes it to JSON, and
     builds every row client-side at runtime. data-table.jte's own
     current doc comment explicitly says that behavior is gone. This
     page is written from data-table.jte's own current, authoritative
     description, not table.jte's stale one. A maintainer should correct
     table.jte's own comment directly inside ashlar/ when convenient. --%>
!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.components.table(\n"
        + "    caption = \"Environment endpoints\",\n"
        + "    captionVisible = true,\n"
        + "    columns = @" + backtick + "\n"
        + "        @template.ashlar.components.table-column(label = \"Environment\")\n"
        + "    " + backtick + ",\n"
        + "    rows = @" + backtick + "\n"
        + "        @template.ashlar.components.table-row(cells = @" + backtick + "\n"
        + "            @template.ashlar.components.table-cell(text = \"Production\", header = true)\n"
        + "        " + backtick + ")\n"
        + "    " + backtick + "\n"
        + ")";}
!{String batchJobCode = "@template.ashlar.components.table(\n"
        + "    caption = \"Batch jobs submitted in the last seven days\",\n"
        + "    searchable = true,\n"
        + "    searchPlaceholder = \"Search jobs\",\n"
        + "    pageSize = 6,\n"
        + "    columns = @" + backtick + "\n"
        + "        @template.ashlar.components.table-column(label = \"Job\", sortable = true)\n"
        + "        @template.ashlar.components.table-column(label = \"Description\", sortable = true)\n"
        + "        @template.ashlar.components.table-column(label = \"Owner\", sortable = true)\n"
        + "        @template.ashlar.components.table-column(label = \"Status\", sortable = true)\n"
        + "        @template.ashlar.components.table-column(label = \"Submitted\", sortable = true, type = \"date\")\n"
        + "        @template.ashlar.components.table-column(label = \"Records\", sortable = true, type = \"number\", align = \"right\")\n"
        + "    " + backtick + ",\n"
        + "    rows = @" + backtick + "\n"
        + "        @for (var job : jobs)\n"
        + "            @template.ashlar.components.table-row(cells = @" + backtick + "\n"
        + "                @template.ashlar.components.table-cell(text = job.jobNumber(), header = true, mono = true, nowrap = true)\n"
        + "                @template.ashlar.components.table-cell(text = job.description())\n"
        + "                @template.ashlar.components.table-cell(text = job.owner(), nowrap = true)\n"
        + "                @template.ashlar.components.table-cell(content = @" + backtick + "@template.ashlar.components.badge(text = job.status(), variant = job.badgeVariant())" + backtick + ", sortValue = job.status())\n"
        + "                @template.ashlar.components.table-cell(text = job.submittedDisplay(), sortValue = job.submittedIso(), nowrap = true)\n"
        + "                @template.ashlar.components.table-cell(text = job.recordsDisplay(), sortValue = job.recordsSortValue(), align = \"right\", nowrap = true)\n"
        + "            " + backtick + ")\n"
        + "        @endfor\n"
        + "    " + backtick + "\n"
        + ")";}
!{String selectableCode = "@template.ashlar.components.table(\n"
        + "    caption = \"Batch jobs (selectable)\",\n"
        + "    selectable = true,\n"
        + "    pageSize = 5,\n"
        + "    columns = @" + backtick + "\n"
        + "        @template.ashlar.components.table-column(label = \"Job\", sortable = true)\n"
        + "        @template.ashlar.components.table-column(label = \"Owner\")\n"
        + "        @template.ashlar.components.table-column(label = \"Status\")\n"
        + "    " + backtick + ",\n"
        + "    rows = @" + backtick + "\n"
        + "        @for (var job : jobs)\n"
        + "            @template.ashlar.components.table-row(\n"
        + "                rowKey = job.jobNumber(),\n"
        + "                rowLabel = job.jobNumber(),\n"
        + "                cells = @" + backtick + "\n"
        + "                    @template.ashlar.components.table-cell(text = job.jobNumber(), header = true, mono = true, nowrap = true)\n"
        + "                    @template.ashlar.components.table-cell(text = job.owner(), nowrap = true)\n"
        + "                    @template.ashlar.components.table-cell(content = @" + backtick + "@template.ashlar.components.badge(text = job.status(), variant = job.badgeVariant())" + backtick + ")\n"
        + "                " + backtick + "\n"
        + "            )\n"
        + "        @endfor\n"
        + "    " + backtick + "\n"
        + ")";}
!{String serverPagingCode = "@template.ashlar.components.table(\n"
        + "    caption = \"Batch jobs, page 2 of 4\",\n"
        + "    columns = @" + backtick + "\n"
        + "        @template.ashlar.components.table-column(label = \"Job\")\n"
        + "        @template.ashlar.components.table-column(label = \"Description\")\n"
        + "        @template.ashlar.components.table-column(label = \"Submitted\", type = \"date\")\n"
        + "        @template.ashlar.components.table-column(label = \"Records\", type = \"number\", align = \"right\")\n"
        + "    " + backtick + ",\n"
        + "    rows = @" + backtick + "\n"
        + "        @for (var job : jobs.subList(4, 8))\n"
        + "            @template.ashlar.components.table-row(cells = @" + backtick + "\n"
        + "                @template.ashlar.components.table-cell(text = job.jobNumber(), header = true, mono = true, nowrap = true)\n"
        + "                @template.ashlar.components.table-cell(text = job.description())\n"
        + "                @template.ashlar.components.table-cell(text = job.submittedDisplay(), nowrap = true)\n"
        + "                @template.ashlar.components.table-cell(text = job.recordsDisplay(), align = \"right\", nowrap = true)\n"
        + "            " + backtick + ")\n"
        + "        @endfor\n"
        + "    " + backtick + ",\n"
        + "    footer = @" + backtick + "\n"
        + "        @template.ashlar.components.pagination(items = @" + backtick + "\n"
        + "            @template.ashlar.components.pagination-item(label = \"Previous\", href = \"#\")\n"
        + "            @template.ashlar.components.pagination-item(label = \"1\", href = \"#\")\n"
        + "            @template.ashlar.components.pagination-item(label = \"2\", href = \"#\", current = true)\n"
        + "            @template.ashlar.components.pagination-item(label = \"3\", href = \"#\")\n"
        + "            @template.ashlar.components.pagination-item(label = \"4\", href = \"#\")\n"
        + "            @template.ashlar.components.pagination-item(label = \"Next\", href = \"#\")\n"
        + "        " + backtick + ")\n"
        + "    " + backtick + "\n"
        + ")";}
!{String emptyCode = "@template.ashlar.components.table(\n"
        + "    caption = \"Failed jobs\",\n"
        + "    empty = true,\n"
        + "    emptyTitle = \"No failed jobs\",\n"
        + "    emptyBody = \"Nothing has failed in the selected period.\",\n"
        + "    columns = @" + backtick + "\n"
        + "        @template.ashlar.components.table-column(label = \"Job\")\n"
        + "        @template.ashlar.components.table-column(label = \"Description\")\n"
        + "        @template.ashlar.components.table-column(label = \"Submitted\", type = \"date\")\n"
        + "    " + backtick + ",\n"
        + "    rows = @" + backtick + backtick + "\n"
        + ")";}
!{String loadingCode = "@template.ashlar.components.table(\n"
        + "    caption = \"Batch jobs\",\n"
        + "    loading = true,\n"
        + "    columns = @" + backtick + "\n"
        + "        @template.ashlar.components.table-column(label = \"Job\")\n"
        + "        @template.ashlar.components.table-column(label = \"Description\")\n"
        + "    " + backtick + ",\n"
        + "    rows = @" + backtick + backtick + "\n"
        + ")";}
!{List<ApiParam> tableApi = List.of(
    ApiParam.required("caption", "String", "A real <caption>. Visually hidden (sr-only) by default - captionVisible shows it for a table with no heading of its own above it."),
    ApiParam.required("columns", "gg.jte.Content", "One or more table-column(...) calls."),
    ApiParam.required("rows", "gg.jte.Content", "One or more table-row(...) calls."),
    ApiParam.optional("captionVisible", "boolean", "false", "Shows the caption visibly instead of sr-only."),
    ApiParam.optional("searchable", "boolean", "false", "Adds a search box that filters rows already in the DOM by substring, case-insensitive, across each row's full text."),
    ApiParam.optional("selectable", "boolean", "false", "Adds a leading checkbox column, a select-all control (scoped to the current page), and a selection bar with a clear-selection action."),
    ApiParam.optional("searchPlaceholder", "String", "\"Search\"", "Placeholder for the search box."),
    ApiParam.optional("pageSize", "int", "0", "0 = no client-side paging (show all rows, or page on the server and compose your own Pagination into footer). Greater than 0 pages the rows already in the DOM, client-side."),
    ApiParam.optional("empty", "boolean", "false", "Shows the library's own Empty State instead of the table body. The caller decides this - it is not auto-detected from rows."),
    ApiParam.optional("loading", "boolean", "false", "Shows a Spinner and loading text instead of the table body. The caller decides this; nothing here fetches anything."),
    ApiParam.optional("emptyTitle", "String", "\"No records found\"", "Title shown when empty is true."),
    ApiParam.optional("emptyBody", "String", "\"\"", "Supporting text shown when empty is true."),
    ApiParam.optional("footer", "gg.jte.Content", "null", "Composed content below the table - typically a real Pagination component when the application pages on the server.")
);}
!{List<ApiParam> tableColumnApi = List.of(
    ApiParam.required("label", "String", "The column heading text."),
    ApiParam.optional("sortable", "boolean", "false", "Renders a real <button> inside the <th> and enables table.js's 3-state sort (ascending -> descending -> server order) for this column."),
    ApiParam.optional("align", "String", "\"left\"", "left/center/right. Must also be set on that column's own cells - CSS can't align from a <col>."),
    ApiParam.optional("type", "String", "\"text\"", "A sort-comparison hint, not a formatter: text collates, number compares numerically, date compares as dates. Never reformats the displayed value."),
    ApiParam.optional("width", "String", "\"\"", "An optional Tailwind width class for the column.")
);}
!{List<ApiParam> tableRowApi = List.of(
    ApiParam.required("cells", "gg.jte.Content", "One or more table-cell(...) calls."),
    ApiParam.optional("rowKey", "String", "\"\"", "The identity this row's selection is tracked by, and what survives sort/search/page. Required for a row to get a working checkbox when the table is selectable."),
    ApiParam.optional("rowLabel", "String", "\"\"", "Names the row's checkbox (\"Select row ACC-1001\") so it isn't announced as a bare, unlabeled checkbox.")
);}
!{List<ApiParam> tableCellApi = List.of(
    ApiParam.optional("text", "String", "\"\"", "The ordinary case - a value the caller already has as a string."),
    ApiParam.optional("content", "gg.jte.Content", "null", "Takes over when the cell holds something richer than text - a Badge, a link, a Timeline. This is the whole reason Table's cells can do what Data Table's string-only cells cannot."),
    ApiParam.optional("align", "String", "\"left\"", "Must match the owning column's own align."),
    ApiParam.optional("header", "boolean", "false", "Renders a <th scope=\"row\"> instead of a <td> - opt-in, since only the caller knows which column identifies the row."),
    ApiParam.optional("mono", "boolean", "false", "Monospace type, for identifiers/codes."),
    ApiParam.optional("nowrap", "boolean", "false", "Prevents wrapping - the default is to wrap/break words."),
    ApiParam.optional("sortValue", "String", "\"\"", "The machine-comparable form of a value whose display form doesn't sort correctly (\"2 hours ago\", \"Mar 14, 2024\"). table.js compares this instead of the visible text when present.")
);}
!{List<PageAnchor> sections = List.of(
    new PageAnchor("quick-start", "Quick Start"),
    new PageAnchor("how-to-use", "How to Use"),
    new PageAnchor("variants", "Variants & States"),
    new PageAnchor("composition", "Composition"),
    new PageAnchor("pitfalls", "Pitfalls"),
    new PageAnchor("accessibility", "Accessibility"),
    new PageAnchor("api-reference", "API Reference"),
    new PageAnchor("related", "Related Components")
);}

@template.layout.page(
contextPath = contextPath,
title = "Table",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "table", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A read-oriented table: real server-rendered table markup, with real client-side sorting, searching, paging, and selection layered over the rows it was given. It presents the rows it was handed - it never fetches, formats, or mutates them, and every one of those behaviors operates only on rows already in the DOM.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3">
                    @template.ashlar.components.table(
                    caption = "Environment endpoints",
                    captionVisible = true,
                    columns = @`
                        @template.ashlar.components.table-column(label = "Environment")
                    `,
                    rows = @`
                        @template.ashlar.components.table-row(cells = @`
                            @template.ashlar.components.table-cell(text = "Production", header = true)
                        `)
                    `
                    )
                </div>
            `
            )
        </section>

        <section id="how-to-use" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "How to Use",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Reach for Table when a row needs content beyond plain text - a Badge, a link, an icon, anything the caller already has composed - or when paging genuinely happens on the server. Reach for <a href="${contextPath}/components/data-table" class="text-blue-700 hover:underline dark:text-blue-400">Data Table</a> instead when you have a uniform list of records and would rather hand over a column/row model than hand-write the table-column/table-row/table-cell loop yourself; see Composition below for exactly how the two relate. For a short set of rows with no columns at all, <a href="${contextPath}/components/list" class="text-blue-700 hover:underline dark:text-blue-400">List</a> is usually simpler.</p>
            `
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Batch job review</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The case this was built for: identifiers, descriptions, states, dates and counts, with search, sortable columns and client-side paging over the rows the page was handed.</p>
                        @template.layout.example(
                        code = batchJobCode,
                        content = @`
                            <div class="mt-3">
                                @template.ashlar.components.table(
                                caption = "Batch jobs submitted in the last seven days",
                                searchable = true,
                                searchPlaceholder = "Search jobs",
                                pageSize = 6,
                                columns = @`
                                    @template.ashlar.components.table-column(label = "Job", sortable = true)
                                    @template.ashlar.components.table-column(label = "Description", sortable = true)
                                    @template.ashlar.components.table-column(label = "Owner", sortable = true)
                                    @template.ashlar.components.table-column(label = "Status", sortable = true)
                                    @template.ashlar.components.table-column(label = "Submitted", sortable = true, type = "date")
                                    @template.ashlar.components.table-column(label = "Records", sortable = true, type = "number", align = "right")
                                `,
                                rows = @`
                                    @for (var job : jobs)
                                        @template.ashlar.components.table-row(cells = @`
                                            @template.ashlar.components.table-cell(text = job.jobNumber(), header = true, mono = true, nowrap = true)
                                            @template.ashlar.components.table-cell(text = job.description())
                                            @template.ashlar.components.table-cell(text = job.owner(), nowrap = true)
                                            @template.ashlar.components.table-cell(content = @`@template.ashlar.components.badge(text = job.status(), variant = job.badgeVariant())`, sortValue = job.status())
                                            @template.ashlar.components.table-cell(text = job.submittedDisplay(), sortValue = job.submittedIso(), nowrap = true)
                                            @template.ashlar.components.table-cell(text = job.recordsDisplay(), sortValue = job.recordsSortValue(), align = "right", nowrap = true)
                                        `)
                                    @endfor
                                `
                                )
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Selectable rows</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Each row's <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">rowKey</code> is what selection is tracked by - try sorting or searching with a few rows checked; the selection follows the row, not its position. Select-all is scoped to this page only.</p>
                        @template.layout.example(
                        code = selectableCode,
                        content = @`
                            <div class="mt-3">
                                @template.ashlar.components.table(
                                caption = "Batch jobs (selectable)",
                                selectable = true,
                                pageSize = 5,
                                columns = @`
                                    @template.ashlar.components.table-column(label = "Job", sortable = true)
                                    @template.ashlar.components.table-column(label = "Owner")
                                    @template.ashlar.components.table-column(label = "Status")
                                `,
                                rows = @`
                                    @for (var job : jobs)
                                        @template.ashlar.components.table-row(
                                        rowKey = job.jobNumber(),
                                        rowLabel = job.jobNumber(),
                                        cells = @`
                                            @template.ashlar.components.table-cell(text = job.jobNumber(), header = true, mono = true, nowrap = true)
                                            @template.ashlar.components.table-cell(text = job.owner(), nowrap = true)
                                            @template.ashlar.components.table-cell(content = @`@template.ashlar.components.badge(text = job.status(), variant = job.badgeVariant())`)
                                        `
                                        )
                                    @endfor
                                `
                                )
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">The small case</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">No search, no paging, no sorting - a caption, columns and rows. A blank value renders an em dash and announces "No value" rather than leaving a hole.</p>
                        <div class="mt-3">
                            @template.ashlar.components.table(
                            caption = "Environment endpoints",
                            captionVisible = true,
                            columns = @`
                                @template.ashlar.components.table-column(label = "Environment")
                                @template.ashlar.components.table-column(label = "Region")
                                @template.ashlar.components.table-column(label = "Contact")
                            `,
                            rows = @`
                                @template.ashlar.components.table-row(cells = @`
                                    @template.ashlar.components.table-cell(text = "Production", header = true)
                                    @template.ashlar.components.table-cell(text = "us-west")
                                    @template.ashlar.components.table-cell(text = "platform-oncall")
                                `)
                                @template.ashlar.components.table-row(cells = @`
                                    @template.ashlar.components.table-cell(text = "Staging", header = true)
                                    @template.ashlar.components.table-cell(text = "us-west")
                                    @template.ashlar.components.table-cell(text = "")
                                `)
                                @template.ashlar.components.table-row(cells = @`
                                    @template.ashlar.components.table-cell(text = "Development", header = true)
                                    @template.ashlar.components.table-cell(text = "eu-central")
                                    @template.ashlar.components.table-cell(text = "app-team")
                                `)
                            `
                            )
                        </div>
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Wide data scrolls inside the table</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Nine columns. The table scrolls horizontally within its own surface, so the page never does - narrow the window to see it.</p>
                        <div class="mt-3">
                            @template.ashlar.components.table(
                            caption = "Batch jobs with operational detail",
                            columns = @`
                                @template.ashlar.components.table-column(label = "Job", sortable = true)
                                @template.ashlar.components.table-column(label = "Description", sortable = true)
                                @template.ashlar.components.table-column(label = "Owner")
                                @template.ashlar.components.table-column(label = "Status")
                                @template.ashlar.components.table-column(label = "Submitted", sortable = true, type = "date")
                                @template.ashlar.components.table-column(label = "Records", sortable = true, type = "number", align = "right")
                                @template.ashlar.components.table-column(label = "Source")
                                @template.ashlar.components.table-column(label = "Target")
                                @template.ashlar.components.table-column(label = "Priority")
                            `,
                            rows = @`
                                @for (var job : jobs.subList(0, 6))
                                    @template.ashlar.components.table-row(cells = @`
                                        @template.ashlar.components.table-cell(text = job.jobNumber(), header = true, mono = true, nowrap = true)
                                        @template.ashlar.components.table-cell(text = job.description(), nowrap = true)
                                        @template.ashlar.components.table-cell(text = job.owner(), nowrap = true)
                                        @template.ashlar.components.table-cell(content = @`@template.ashlar.components.badge(text = job.status(), variant = job.badgeVariant())`)
                                        @template.ashlar.components.table-cell(text = job.submittedDisplay(), sortValue = job.submittedIso(), nowrap = true)
                                        @template.ashlar.components.table-cell(text = job.recordsDisplay(), sortValue = job.recordsSortValue(), align = "right", nowrap = true)
                                        @template.ashlar.components.table-cell(text = "ERP-01", mono = true, nowrap = true)
                                        @template.ashlar.components.table-cell(text = "WHS-02", mono = true, nowrap = true)
                                        @template.ashlar.components.table-cell(text = "Normal", nowrap = true)
                                    `)
                                @endfor
                            `
                            )
                        </div>
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">When the application pages on the server</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Leave <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">pageSize</code> at 0, hand over one page of rows, and compose the existing Pagination component into <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">footer</code>. The table renders the page it was given and states nothing about totals it cannot know.</p>
                        @template.layout.example(
                        code = serverPagingCode,
                        content = @`
                            <div class="mt-3">
                                @template.ashlar.components.table(
                                caption = "Batch jobs, page 2 of 4",
                                columns = @`
                                    @template.ashlar.components.table-column(label = "Job")
                                    @template.ashlar.components.table-column(label = "Description")
                                    @template.ashlar.components.table-column(label = "Submitted", type = "date")
                                    @template.ashlar.components.table-column(label = "Records", type = "number", align = "right")
                                `,
                                rows = @`
                                    @for (var job : jobs.subList(4, 8))
                                        @template.ashlar.components.table-row(cells = @`
                                            @template.ashlar.components.table-cell(text = job.jobNumber(), header = true, mono = true, nowrap = true)
                                            @template.ashlar.components.table-cell(text = job.description())
                                            @template.ashlar.components.table-cell(text = job.submittedDisplay(), nowrap = true)
                                            @template.ashlar.components.table-cell(text = job.recordsDisplay(), align = "right", nowrap = true)
                                        `)
                                    @endfor
                                `,
                                footer = @`
                                    @template.ashlar.components.pagination(items = @`
                                        @template.ashlar.components.pagination-item(label = "Previous", href = "#")
                                        @template.ashlar.components.pagination-item(label = "1", href = "#")
                                        @template.ashlar.components.pagination-item(label = "2", href = "#", current = true)
                                        @template.ashlar.components.pagination-item(label = "3", href = "#")
                                        @template.ashlar.components.pagination-item(label = "4", href = "#")
                                        @template.ashlar.components.pagination-item(label = "Next", href = "#")
                                    `)
                                `
                                )
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Nothing to show</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The library's own Empty State, inside the table's surface. Searching a table down to nothing shows a different, recoverable message - try searching the first table for "zzz".</p>
                        @template.layout.example(
                        code = emptyCode,
                        content = @`
                            <div class="mt-3">
                                @template.ashlar.components.table(
                                caption = "Failed jobs",
                                empty = true,
                                emptyTitle = "No failed jobs",
                                emptyBody = "Nothing has failed in the selected period.",
                                columns = @`
                                    @template.ashlar.components.table-column(label = "Job")
                                    @template.ashlar.components.table-column(label = "Description")
                                    @template.ashlar.components.table-column(label = "Submitted", type = "date")
                                `,
                                rows = @``
                                )
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">While the application is fetching</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A caller that already knows it is waiting renders the table with <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">loading = true</code>. The table reports the wait; it does not do the fetching.</p>
                        @template.layout.example(
                        code = loadingCode,
                        content = @`
                            <div class="mt-3">
                                @template.ashlar.components.table(
                                caption = "Batch jobs",
                                loading = true,
                                columns = @`
                                    @template.ashlar.components.table-column(label = "Job")
                                    @template.ashlar.components.table-column(label = "Description")
                                `,
                                rows = @``
                                )
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
                <p class="text-sm text-slate-500 dark:text-slate-400">Table is a wrapper over three smaller pieces, the same wrapper/item shape as List and Tree but one level deeper: <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">table-column</code> composes into Table's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">columns</code>; <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">table-row</code> composes into <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">rows</code>; <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">table-cell</code> composes into a row's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">cells</code>. None of the three has a standalone catalog page - all three are documented here, in the four API tables below.</p>
                <p class="mt-2 text-sm text-slate-500 dark:text-slate-400"><strong class="font-medium text-slate-700 dark:text-slate-300">Table and <a href="${contextPath}/components/data-table" class="text-blue-700 hover:underline dark:text-blue-400">Data Table</a> are not two separate implementations.</strong> Data Table's own body is a single call to this component - it loops a <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">List&lt;DataTableColumn&gt;</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">List&lt;DataTableRow&gt;</code> model into exactly the table-column/table-row/table-cell calls shown on this page, so both share one DOM contract, one client-side script, and one set of theme rules. The real choice between them is <em>model vs. composition</em>, not "static vs. interactive" - sorting, searching, and paging are real and client-side in both, because both run through the same script. Reach for Data Table when a Java model is more convenient than writing the loop yourself and every cell is plain text, a monospace value, or a Badge; reach for Table directly the moment a cell needs anything richer, or you want full control over the markup.</p>
            `
            )
        </section>

        <section id="pitfalls" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Pitfalls & Troubleshooting",
            content = @`
                <ul class="list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">A Dropdown Menu or Popover placed inside a table cell can be clipped by the table's own horizontal scroller.</strong> Both position their open panel with plain CSS, with no mechanism to escape an ancestor's <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">overflow-x-auto</code> - inside a wide table, the open panel can be cut off or scroll away with the table body instead of staying put. This is a real limitation of those two components' current positioning, not something this page's markup can work around; until it's addressed at the component level, prefer a dedicated action column with a plain link or button over a Dropdown Menu/Popover trigger inside a scrollable table.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">Sorting, searching, and paging only ever see the rows already in the DOM.</strong> A table showing page 3 of a server-paged dataset sorts and searches those rows only, never the other pages - an application paging on the server should generally also sort and search on the server, and mark its columns unsortable here.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">A selectable row with no <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">rowKey</code> still renders its checkbox cell, empty.</strong> This keeps every row's column count aligned with the header - a missing checkbox control there would otherwise shift that row's remaining cells one column to the left.</li>
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
                            <li>A real &lt;caption&gt;, &lt;th scope="col"&gt; for every column, and opt-in &lt;th scope="row"&gt; for whichever column identifies a row - no ARIA needed for any of that, since HTML already says it.</li>
                            <li>A sortable column's real &lt;button&gt; with <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-sort</code> on the &lt;th&gt; itself, updated live as the sort changes.</li>
                            <li>An <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-live="polite"</code> status region reporting search/page results - but never the loading state, which is server-rendered content, not a live update.</li>
                            <li>A blank cell announced as "No value" rather than letting a bare em dash be read aloud as "em dash".</li>
                            <li>A selection checkbox always labeled with the actual row (<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">rowLabel</code>) rather than left as an unlabeled control.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>A <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">rowKey</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">rowLabel</code> per row on a selectable table - without one, that row's checkbox is silently inert.</li>
                            <li>Server-side sort/search for any table whose rows span more than one server-paged request.</li>
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
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">table</p>
                        @template.layout.api-reference(apiParams = tableApi)
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">table-column</p>
                        @template.layout.api-reference(apiParams = tableColumnApi)
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">table-row</p>
                        @template.layout.api-reference(apiParams = tableRowApi)
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">table-cell</p>
                        @template.layout.api-reference(apiParams = tableCellApi)
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
                    <a href="${contextPath}/components/data-table" class="text-blue-700 hover:underline dark:text-blue-400">Data Table</a>, built directly on this component from a Java model &middot;
                    <a href="${contextPath}/components/list" class="text-blue-700 hover:underline dark:text-blue-400">List</a> for rows with no columns &middot;
                    <a href="${contextPath}/components/pagination" class="text-blue-700 hover:underline dark:text-blue-400">Pagination</a> for server-side paging composed into footer &middot;
                    <a href="${contextPath}/components/empty-state" class="text-blue-700 hover:underline dark:text-blue-400">Empty State</a> and
                    <a href="${contextPath}/components/spinner" class="text-blue-700 hover:underline dark:text-blue-400">Spinner</a>, both composed internally &middot;
                    <a href="${contextPath}/components/badge" class="text-blue-700 hover:underline dark:text-blue-400">Badge</a>, a common cell content
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "table", contextPath = contextPath)
    </div>
`
)
