@import java.util.List
@import java.util.Map
@import com.redspartanlabs.ashlar.datatable.DataTableColumn
@import com.redspartanlabs.ashlar.datatable.DataTableRow
@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import com.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import com.redspartanlabs.ashlar.showcase.datatable.SampleAccountData

@param String contextPath

<%-- filterBarPatternCode below is built with the oneBacktick + text-block
     splicing technique rather than plain concatenation, because it needs
     to show a nested filter-bar content call as literal text - see
     oneBacktick's own declaration for why. Every other code string on
     this page needs no such technique, since data-table() itself takes a
     List<DataTableColumn>/List<DataTableRow> model, never a content
     param. --%>
!{List<DataTableColumn> quickStartColumns = List.of(
    DataTableColumn.of("name", "Name"),
    DataTableColumn.badge("role", "Role", Map.of("Admin", "Info", "Member", "Neutral")),
    DataTableColumn.date("joined", "Joined")
);}
!{List<DataTableRow> quickStartRows = List.of(
    new DataTableRow(
        Map.of("name", "Dana Whitfield", "role", "Admin", "joined", "January 12, 2024"),
        Map.of("joined", "2024-01-12")
    ),
    new DataTableRow(
        Map.of("name", "Marcus Lee", "role", "Member", "joined", "February 3, 2024"),
        Map.of("joined", "2024-02-03")
    ),
    new DataTableRow(
        Map.of("name", "Priya Natarajan", "role", "Member", "joined", "November 22, 2023"),
        Map.of("joined", "2023-11-22")
    )
);}
!{String quickStartCode = """
    !{List<DataTableColumn> columns = List.of(
        DataTableColumn.of("name", "Name"),
        DataTableColumn.badge("role", "Role", Map.of("Admin", "Info", "Member", "Neutral")),
        DataTableColumn.date("joined", "Joined")
    );}
    !{List<DataTableRow> rows = List.of(
        new DataTableRow(
            Map.of("name", "Dana Whitfield", "role", "Admin", "joined", "January 12, 2024"),
            Map.of("joined", "2024-01-12")
        ),
        new DataTableRow(
            Map.of("name", "Marcus Lee", "role", "Member", "joined", "February 3, 2024"),
            Map.of("joined", "2024-02-03")
        ),
        new DataTableRow(
            Map.of("name", "Priya Natarajan", "role", "Member", "joined", "November 22, 2023"),
            Map.of("joined", "2023-11-22")
        )
    );}

    @template.ashlar.components.data-table(
        caption = "Team members",
        columns = columns,
        rows = rows,
        rowKey = "name"
    )""";}
!{// The backtick below is assembled at render time, never written as a
  // literal character in this file's own source - JTE's raw-content
  // scanner found the literal at-sign-then-backtick sequence here once
  // (inside this very snippet's text block) and genuinely parsed the rest
  // of the file as that block's markup, leaking gg.jte.html.HtmlContent
  // internals into the page. See usage-snippet.jte's doc comment for the
  // full explanation.
  String oneBacktick = "`";}
!{String filterBarPatternCode = """
    <form method="get">
        @template.ashlar.utilities.filter-bar(
            content = @"""
    + oneBacktick + """

                @template.ashlar.components.search-input(name = "q", label = "Search accounts")
                @template.ashlar.components.select(name = "status", label = "Status", options = statuses, clearable = true)
                @template.ashlar.components.button(text = "Apply", type = "Primary", buttonType = "submit")
            """
    + oneBacktick + """

        )
    </form>

    @template.ashlar.components.data-table(
        caption = "Customer accounts",
        columns = columns,
        rows = rows,
        rowKey = "id",
        searchable = false
    )""";}
!{String accountsCode = """
    @template.ashlar.components.data-table(
        caption = "Customer accounts",
        columns = SampleAccountData.columns(),
        rows = SampleAccountData.rows(),
        rowKey = "id",
        selectable = true,
        pageSize = 6,
        searchPlaceholder = "Search accounts"
    )""";}
!{String readOnlyCode = """
    @template.ashlar.components.data-table(
        caption = "Accounts, reference view",
        captionVisible = true,
        columns = SampleAccountData.columns(),
        rows = SampleAccountData.rows(),
        rowKey = "id",
        searchable = false,
        pageSize = 5
    )""";}
!{String noRecordsCode = """
    @template.ashlar.components.data-table(
        caption = "Archived accounts",
        columns = SampleAccountData.columns(),
        rows = SampleAccountData.noRows(),
        emptyTitle = "No archived accounts",
        emptyBody = "Accounts appear here once they have been archived."
    )""";}
!{List<ApiParam> dataTableApi = List.of(
    ApiParam.required("caption", "String", "The table's accessible name - always present in the DOM; captionVisible controls whether it's also shown on screen."),
    ApiParam.required("columns", "List<DataTableColumn>", "Column definitions, built via DataTableColumn.of/text/mono/number/date/badge(...)."),
    ApiParam.required("rows", "List<DataTableRow>", "Row data: display values, plus optional sort values for columns where the displayed text isn't what should be compared."),
    ApiParam.optional("captionVisible", "boolean", "false", "Show the caption visually instead of screen-reader-only."),
    ApiParam.optional("rowKey", "String", "\"\"", "Names the identity column. Required for selection; also gives each row a screen-reader row header. Omit it and there is no row header and no selection column at all - the component will not guess which field identifies a record."),
    ApiParam.optional("pageSize", "int", "10", "Rows per page. Pagination is client-side, over the rows already rendered."),
    ApiParam.optional("selectable", "boolean", "false", "Adds a selection checkbox column. Needs rowKey to work."),
    ApiParam.optional("searchable", "boolean", "true", "Adds a client-side search field filtering by each row's full rendered text."),
    ApiParam.optional("searchPlaceholder", "String", "\"Search\"", "Placeholder text for the search field."),
    ApiParam.optional("emptyTitle", "String", "\"No records found\"", "Heading shown through the real Empty State component when rows is empty."),
    ApiParam.optional("emptyBody", "String", "\"\"", "Supporting text for the same Empty State.")
);}
!{List<PageAnchor> sections = List.of(
    new PageAnchor("quick-start", "Quick Start"),
    new PageAnchor("how-to-use", "How to Use"),
    new PageAnchor("variants", "Variants & States"),
    new PageAnchor("composition", "Composition"),
    new PageAnchor("best-practices", "Best Practices"),
    new PageAnchor("common-patterns", "Common Patterns"),
    new PageAnchor("pitfalls", "Pitfalls"),
    new PageAnchor("accessibility", "Accessibility"),
    new PageAnchor("api-reference", "API Reference"),
    new PageAnchor("related", "Related Components")
);}

@template.layout.page(
contextPath = contextPath,
title = "Data Table",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "data-table", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The model-driven table: declare columns, hand over rows. It renders through Table, so both share one DOM, one script and one set of theme rules - this adds the column model and optional selection. It presents data; it never fetches, formats or mutates it.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3">
                    @template.ashlar.components.data-table(
                    caption = "Team members",
                    columns = quickStartColumns,
                    rows = quickStartRows,
                    rowKey = "name"
                    )
                </div>
            `
            )
        </section>

        <section id="how-to-use" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "How to Use",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Reach for Data Table when you have a list of uniform records and want sorting, search, pagination, and row selection without writing JavaScript - it owns all of that behavior once you hand it columns and rows. Reach for <a href="${contextPath}/components/table" class="text-blue-700 hover:underline dark:text-blue-400">Table</a> instead when a row needs content Data Table's three presentations (text, monospace, badge) can't express - an action menu, an icon, a nested component - or when paging has to happen on the server rather than over rows already in the DOM.</p>
            `
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Accounts - sorting, search, paging, selection</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Eight columns: a monospaced identifier, text, a Badge status, dates and two numeric columns. Sort the numbers and the dates - they compare as numbers and dates, not as the formatted strings you can see. Select rows, then page or search: the selection follows the rows. Search for something absent (try "zzz") for the no-results state.</p>
                        @template.layout.example(
                        code = accountsCode,
                        content = @`
                            <div class="mt-3">
                                @template.ashlar.components.data-table(
                                caption = "Customer accounts",
                                columns = SampleAccountData.columns(),
                                rows = SampleAccountData.rows(),
                                rowKey = "id",
                                selectable = true,
                                pageSize = 6,
                                searchPlaceholder = "Search accounts"
                                )
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Read-only, no selection</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Selection and search are both opt-out, and <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">rowKey</code> is useful without either: it still names the column that identifies a row, so that cell stays the row's header for a screen reader.</p>
                        @template.layout.example(
                        code = readOnlyCode,
                        content = @`
                            <div class="mt-3">
                                @template.ashlar.components.data-table(
                                caption = "Accounts, reference view",
                                captionVisible = true,
                                columns = SampleAccountData.columns(),
                                rows = SampleAccountData.rows(),
                                rowKey = "id",
                                searchable = false,
                                pageSize = 5
                                )
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">No records at all</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">"There is nothing here" is a different situation from "your search matched nothing," and it is the library's own Empty State in both cases. There is no <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">empty</code> parameter to pass - the component can see that the row list is empty.</p>
                        @template.layout.example(
                        code = noRecordsCode,
                        content = @`
                            <div class="mt-3">
                                @template.ashlar.components.data-table(
                                caption = "Archived accounts",
                                columns = SampleAccountData.columns(),
                                rows = SampleAccountData.noRows(),
                                emptyTitle = "No archived accounts",
                                emptyBody = "Accounts appear here once they have been archived."
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
                <p class="text-sm text-slate-500 dark:text-slate-400">Internally, every badge column renders through the real <a href="${contextPath}/components/badge" class="text-blue-700 hover:underline dark:text-blue-400">Badge</a> component - <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">DataTableColumn.badge(...)</code> only supplies the value-to-variant mapping - and the empty states above render through the real <a href="${contextPath}/components/empty-state" class="text-blue-700 hover:underline dark:text-blue-400">Empty State</a>, not a reimplementation of either. Externally, the common pairing is a <a href="${contextPath}/components/filter-bar" class="text-blue-700 hover:underline dark:text-blue-400">Filter Bar</a> above the table - see Common Patterns below.</p>
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
                            <li>Pass <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">rowKey</code> whenever rows have a natural identity, even if the table isn't selectable yet - it's what gives every row a screen-reader header.</li>
                            <li>Declare <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">.number(...)</code> for numeric columns - it strips everything but digits, a decimal point, and a sign before comparing, so values like "$128.40" or "1,204,981" already sort correctly with no extra work.</li>
                            <li>Declare <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">.date(...)</code> for date columns - it compares via <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">Date.parse</code>, so a display format like "September 14, 2026" orders chronologically instead of by month name.</li>
                            <li>Supply an explicit sort value on <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">DataTableRow</code> only when neither parser fits what's displayed - a relative phrase like "2 days ago" has no date <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">Date.parse</code> can read. The display value stays for the reader; the sort value is only ever compared, never shown.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Avoid</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Assuming "select all" selects every matching record - see Pitfalls below.</li>
                            <li>Building a column that needs row actions, an icon, or any content beyond text/mono/badge - that's Table's job, not Data Table's.</li>
                            <li>Leaving both Data Table's own search and an external Filter Bar's search active at once - pick one.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="common-patterns" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Common Patterns",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Filter Bar above, Data Table below - the application owns the &lt;form&gt;, Filter Bar owns the control row, and Data Table's own search is turned off since Filter Bar already has one:</p>
                <div class="mt-3">
                    @template.layout.usage-snippet(label = "", code = filterBarPatternCode)
                </div>
            `
            )
        </section>

        <section id="pitfalls" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Pitfalls & Troubleshooting",
            content = @`
                <ul class="list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">"Select all" only selects the current page.</strong> Selection state is scoped to what's visible on screen when it's checked, not every row matching the current search. Build server-side "select all matching" as a separate affordance if you need it.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">A display-formatted date through a text column sorts alphabetically, not chronologically.</strong> <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">.of(...)</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">.text(...)</code> already compares plain numbers correctly (it won't put "10" before "9" - the comparison is locale-numeric-aware), but it has no notion of calendar time: "September 14, 2026" sorts by month name, not by date. Declare the column <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">.date(...)</code> (compared via <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">Date.parse</code>) instead.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">No custom cell content.</strong> Presentation is text, monospace, or Badge - nothing else. A row-actions menu or any other rich cell belongs on Table, which is composition-driven.</li>
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
                            <li>An <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-live="polite"</code> status region announcing the visible row count, search results, and current page.</li>
                            <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-sort</code> on sortable headers, kept in sync as sort state changes.</li>
                            <li>A real <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">&lt;th scope="row"&gt;</code> for the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">rowKey</code> column when present, so a screen reader announces which row a selection checkbox belongs to.</li>
                            <li>Keyboard-operable sort buttons and pagination controls - real <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">&lt;button&gt;</code> elements, no custom key handling needed.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">caption</code> - the table's one required accessible name.</li>
                            <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">rowKey</code> - without it there is no row header and no selection column; the component won't guess which field identifies a record.</li>
                            <li>Column labels worth hearing - they're exactly what gets announced on every <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-sort</code> change.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.layout.api-reference(apiParams = dataTableApi)
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/table" class="text-blue-700 hover:underline dark:text-blue-400">Table</a> for composition-driven rows with custom cell content or server-side paging &middot;
                    <a href="${contextPath}/components/filter-bar" class="text-blue-700 hover:underline dark:text-blue-400">Filter Bar</a> for search/filter controls above it &middot;
                    <a href="${contextPath}/components/badge" class="text-blue-700 hover:underline dark:text-blue-400">Badge</a> for status columns &middot;
                    <a href="${contextPath}/components/empty-state" class="text-blue-700 hover:underline dark:text-blue-400">Empty State</a>, used automatically when rows is empty
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "data-table", contextPath = contextPath)
    </div>
`
)
