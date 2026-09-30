@import com.redspartanlabs.ashlar.showcase.table.SampleJobHeader

@param String contextPath

!{var jobs = SampleJobHeader.sample();}

@template.layout.page(
contextPath = contextPath,
title = "Table",
content = @`
    @template.layout.component-nav(currentSlug = "table", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Table</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A read-oriented table: real server-rendered table markup, with sorting, searching and paging layered over the rows it was given. It presents data - it never fetches, formats or mutates it. For an interactive grid with selection and per-row actions, see Data Table.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Batch job review</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The case this was built for: identifiers, descriptions, states, dates and counts, with search, sortable columns and client-side paging over the rows the page was handed.</p>
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
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">The small case</h3>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Wide data scrolls inside the table</h3>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">When the application pages on the server</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Leave <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">pageSize</code> at 0, hand over one page of rows, and compose the existing Pagination component into <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">footer</code>. The table renders the page it was given and states nothing about totals it cannot know.</p>
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
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Nothing to show</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The library's own Empty State, inside the table's surface. Searching a table down to nothing shows a different, recoverable message - try searching the first table for "zzz".</p>
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
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">While the application is fetching</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A caller that already knows it is waiting renders the table with <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">loading = true</code>. The table reports the wait; it does not do the fetching.</p>
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
                </div>
            </div>
        </section>
`
)
