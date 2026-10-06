@import com.redspartanlabs.ashlar.showcase.table.SampleJobHeader
@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param String contextPath

!{var jobs = SampleJobHeader.sample();}

<%-- Batch 7 documentation page. Existing "Batch job review" / "The small
     case" / "Wide data scrolls inside the table" / "When the application
     pages on the server" / "Nothing to show" / "While the application is
     fetching" examples kept verbatim, moved under Variants & States. One
     new example added, "Selectable rows", because selectable was a real,
     fully-implemented param (select-all, a selection bar, per-row
     checkboxes) with zero demonstration anywhere on the previous page -
     a genuine coverage gap, not a correction to anything existing. Added:
     Quick Start, formal grammar headings, How to Use, a Composition
     section covering the whole table/table-column/table-row/table-cell/
     Data Table family, Accessibility, four API Reference tables,
     Related Components, Pitfalls (including the Known Table Issue below).

     Verified against source before writing - including re-reading
     table.js in full (a real client-side sort/search/page engine, not a
     purely static component) and data-table.jte in full, rather than
     trusting either component's own doc comment's framing at face value:

     THE REAL TABLE <-> DATA TABLE RELATIONSHIP, confirmed directly in
     data-table.jte's own source, not inferred from naming or from either
     file's prose: data-table.jte's body is a single
     @template.ashlar.components.table(...) call, with its own `columns`
     and `rows` built by looping DataTableColumn/DataTableRow into
     table-column/table-row/table-cell calls - literally the same markup a
     caller would hand-write for Table directly. Data Table is not a
     parallel reimplementation; it IS a Table underneath, with a Java
     model (List<DataTableColumn>/List<DataTableRow>) standing in for the
     JTE loop a caller would otherwise write by hand. Both share one DOM
     contract, one table.js, and one set of theme rules, confirmed, not
     assumed. The real distinction is model vs. composition, not
     "static vs. interactive" - sorting/searching/paging are real,
     client-side, and identical in both, because both run through the
     exact same table.js.
     CORRECTION NOTE: table.jte's OWN doc comment ("Data Table... takes
     List<Map<String,String>>, serialises it into a JSON block and builds
     every row at runtime... its <tbody> is empty until JS has run") is
     STALE - it describes a previous version of Data Table that
     data-table.jte's own current doc comment explicitly says is gone
     ("WHAT IT NO LONGER DOES... All of that is gone"). This is a real,
     pre-existing inconsistency between the two .jte files' own comments,
     discovered during this audit - reported in this batch's final
     report rather than silently corrected, since fixing a comment inside
     ashlar/ is out of scope for a documentation-only batch. This
     showcase page is written from data-table.jte's own current,
     authoritative description, not table.jte's stale one. The existing
     Data Table showcase page already says "it renders through Table, so
     both share one DOM, one script and one set of theme rules" - that
     wording is accurate and needed no correction.

     TABLE'S OWN REAL INTERACTIVE BEHAVIOR, confirmed in table.js (316
     lines): 3-state sort (ascending -> descending -> back to server
     order) using the exact same comparator and "blank values sort to the
     end regardless of direction" rule Data Table's own page already
     documents, because it is the same code; substring, case-insensitive
     search across a row's full text; client-side paging. All three
     operate ONLY on rows already present in the DOM - confirmed directly
     in the file's own top comment ("it only reorders and hides them. It
     never fetches anything"). Selection is tracked by row key (from
     table-row's own rowKey), not position, so it survives sort/search/
     page - confirmed in syncSelection(), and select-all is explicitly
     scoped to the current page only (confirmed in its own change
     listener using visibleOnPage()).

     KNOWN TABLE ISSUE - OVERFLOW WRAPPER VS. OVERLAY COMPONENTS,
     investigated directly rather than assumed:
     CONFIRMED STILL PRESENT. table.jte's horizontal scroller
     (`data-table-scroll`, `overflow-x-auto`) and both dropdown-menu.jte
     and popover.jte position their floating panel with plain CSS
     `absolute` (confirmed by grep - neither file nor its .js ever moves
     the panel elsewhere in the DOM, i.e. no portal). popover.js's own
     `reposition()` only nudges the panel along one axis to stay inside
     the *viewport* via a CSS `translate` - its own comment states
     plainly "no flipping to the opposite side, no re-measuring on
     scroll" - it never changes `position` to `fixed` and never escapes
     the DOM ancestor it's already in. CONCLUSION: a Dropdown Menu or
     Popover trigger placed inside a Table cell will have its open panel
     clipped by the table's own overflow-x-auto wrapper whenever the
     panel would extend beyond the wrapper's current scroll viewport, and
     the open panel scrolls horizontally with the table body instead of
     staying fixed to the page. This is a genuine architectural
     limitation of the current Dropdown Menu/Popover positioning
     strategy combined with Table's own scroll wrapper, not a
     documentation-only misunderstanding - fixing it would mean giving
     Dropdown Menu/Popover a portal-based (or at minimum
     position: fixed, viewport-relative) positioning strategy, which is
     an ashlar/ change out of scope for this batch. Documented below as a
     real Pitfall with a workaround, and flagged in this batch's final
     report as a candidate for a separately tracked library issue. --%>
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

@template.layout.page(
contextPath = contextPath,
title = "Table",
content = @`
    @template.layout.component-nav(currentSlug = "table", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Table</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A read-oriented table: real server-rendered table markup, with real client-side sorting, searching, paging, and selection layered over the rows it was given. It presents the rows it was handed - it never fetches, formats, or mutates them, and every one of those behaviors operates only on rows already in the DOM.</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
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
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">How to Use</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Reach for Table when a row needs content beyond plain text - a Badge, a link, an icon, anything the caller already has composed - or when paging genuinely happens on the server. Reach for <a href="${contextPath}/components/data-table" class="text-blue-700 hover:underline dark:text-blue-400">Data Table</a> instead when you have a uniform list of records and would rather hand over a column/row model than hand-write the table-column/table-row/table-cell loop yourself; see Composition below for exactly how the two relate. For a short set of rows with no columns at all, <a href="${contextPath}/components/list" class="text-blue-700 hover:underline dark:text-blue-400">List</a> is usually simpler.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Batch job review</p>
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
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Selectable rows</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Each row's <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">rowKey</code> is what selection is tracked by - try sorting or searching with a few rows checked; the selection follows the row, not its position. Select-all is scoped to this page only.</p>
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
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Nothing to show</p>
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
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">While the application is fetching</p>
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
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composition</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Table is a wrapper over three smaller pieces, the same wrapper/item shape as List and Tree but one level deeper: <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">table-column</code> composes into Table's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">columns</code>; <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">table-row</code> composes into <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">rows</code>; <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">table-cell</code> composes into a row's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">cells</code>. None of the three has a standalone catalog page - all three are documented here, in the four API tables below.</p>
                    <p class="mt-2 text-sm text-slate-500 dark:text-slate-400"><strong class="font-medium text-slate-700 dark:text-slate-300">Table and <a href="${contextPath}/components/data-table" class="text-blue-700 hover:underline dark:text-blue-400">Data Table</a> are not two separate implementations.</strong> Data Table's own body is a single call to this component - it loops a <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">List&lt;DataTableColumn&gt;</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">List&lt;DataTableRow&gt;</code> model into exactly the table-column/table-row/table-cell calls shown on this page, so both share one DOM contract, one client-side script, and one set of theme rules. The real choice between them is <em>model vs. composition</em>, not "static vs. interactive" - sorting, searching, and paging are real and client-side in both, because both run through the same script. Reach for Data Table when a Java model is more convenient than writing the loop yourself and every cell is plain text, a monospace value, or a Badge; reach for Table directly the moment a cell needs anything richer, or you want full control over the markup.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Pitfalls &amp; Troubleshooting</h3>
                    <ul class="mt-2 list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">A Dropdown Menu or Popover placed inside a table cell can be clipped by the table's own horizontal scroller.</strong> Both position their open panel with plain CSS, with no mechanism to escape an ancestor's <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">overflow-x-auto</code> - inside a wide table, the open panel can be cut off or scroll away with the table body instead of staying put. This is a real limitation of those two components' current positioning, not something this page's markup can work around; until it's addressed at the component level, prefer a dedicated action column with a plain link or button over a Dropdown Menu/Popover trigger inside a scrollable table.</li>
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">Sorting, searching, and paging only ever see the rows already in the DOM.</strong> A table showing page 3 of a server-paged dataset sorts and searches those rows only, never the other pages - an application paging on the server should generally also sort and search on the server, and mark its columns unsortable here.</li>
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">A selectable row with no <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">rowKey</code> still renders its checkbox cell, empty.</strong> This keeps every row's column count aligned with the header - a missing checkbox control there would otherwise shift that row's remaining cells one column to the left.</li>
                    </ul>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
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
                </div>

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

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/data-table" class="text-blue-700 hover:underline dark:text-blue-400">Data Table</a>, built directly on this component from a Java model &middot;
                        <a href="${contextPath}/components/list" class="text-blue-700 hover:underline dark:text-blue-400">List</a> for rows with no columns &middot;
                        <a href="${contextPath}/components/pagination" class="text-blue-700 hover:underline dark:text-blue-400">Pagination</a> for server-side paging composed into footer &middot;
                        <a href="${contextPath}/components/empty-state" class="text-blue-700 hover:underline dark:text-blue-400">Empty State</a> and
                        <a href="${contextPath}/components/spinner" class="text-blue-700 hover:underline dark:text-blue-400">Spinner</a>, both composed internally &middot;
                        <a href="${contextPath}/components/badge" class="text-blue-700 hover:underline dark:text-blue-400">Badge</a>, a common cell content
                    </p>
                </div>
            </div>
        </section>
`
)
