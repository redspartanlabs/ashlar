@import com.redspartanlabs.ashlar.icon.Icon
@import com.redspartanlabs.ashlar.table.TableContext
@import com.redspartanlabs.ashlar.AshlarAssets

@param String caption
@param gg.jte.Content columns
@param gg.jte.Content rows
@param boolean captionVisible = false
@param boolean searchable = false
@param boolean selectable = false
@param String searchPlaceholder = "Search"
@param int pageSize = 0
@param boolean empty = false
@param boolean loading = false
@param String emptyTitle = "No records found"
@param String emptyBody = ""
@param gg.jte.Content footer = null

<%-- A read-oriented table: real, server-rendered table markup that a person
     can scan, with sorting, filtering and paging layered on top of what is
     already in the DOM.

     WHY THIS EXISTS ALONGSIDE data-table.jte. Data Table is a JavaScript
     grid: it takes `List<Map<String, String>>`, serialises it into a
     <script type="application/json"> block and builds every row at runtime,
     and it owns selection and per-row actions. That is the right shape for
     an interactive record manager, and the wrong shape here - its cells can
     only ever be strings, and its <tbody> is empty until JS has run. Table
     is the simpler primitive underneath that idea: the rows are composed in
     JTE, so a cell can hold a Badge, a link, or anything else the caller
     already has; the markup is complete and readable before any script
     loads; and there is no selection, no row actions, and no CRUD. Data
     Table is deliberately left alone - if it is ever rebuilt on this, that
     is its own task.

     WHAT IT OWNS, AND WHAT IT DOES NOT. The component presents and
     interacts with the rows it was handed. It knows nothing about where
     they came from: no repository, no service, no query, no fetching. When
     `pageSize` is set it pages the rows it has, client side. When an
     application pages on the server it leaves `pageSize` at 0, hands over
     one page of rows, and composes its own navigation into `footer` - the
     existing Pagination component is exactly that, and re-implementing
     link-based paging here would have duplicated it.

     Sorting is honest about the same boundary: it reorders the rows present
     in the DOM and nothing else. A table showing page 3 of 10 sorts those
     rows, not the other 9 pages - which is why an application that pages on
     the server usually wants to sort on the server too, and simply marks
     its columns unsortable here.

     `caption` is required and is a real <caption>, because a table needs a
     name and this is the element HTML provides for it. It is visually
     hidden by default: the page almost always has its own heading directly
     above, and printing the same words twice helps nobody - `captionVisible`
     turns it on for a table that stands alone.

     No `id` parameter: nothing here needs to coordinate with anything
     outside the component, so the search field is wired with a wrapping
     <label> rather than a for/id pair a caller would have to invent.

     Column visibility toggles and sticky headers were both considered and
     left out of v1. Neither earns its complexity for a read-oriented table
     that pages: a visibility menu is a second UI to design and explain, and
     a sticky header only pays off on a long scroll, which paging removes. --%>
!{boolean hasFooterContent = footer != null;}
!{boolean paginated = pageSize > 0;}
!{String captionClasses = captionVisible
        ? "caption-top px-4 pt-4 pb-2 text-left text-base font-semibold text-slate-900 dark:text-slate-100"
        : "sr-only";}

<div
        data-table
        data-table-page-size="${pageSize}"
        class="overflow-hidden rounded-xl border border-slate-200 bg-white dark:border-slate-700 dark:bg-slate-900"
>
    @if (searchable || selectable)
        <div class="flex flex-col gap-2 border-b border-slate-200 p-3 sm:flex-row sm:items-center sm:justify-between dark:border-slate-700">
            <%-- Selection is counted here, not announced. The status region
                 further down is for filter and page changes, which the user
                 did not necessarily cause directly; ticking a box is already
                 confirmed by the checkbox's own state, and routing it through
                 a live region would narrate every click. --%>
            @if (selectable)
                <div data-table-selection-bar class="hidden items-center gap-2 rounded-md bg-blue-50 px-3 py-1.5 text-sm text-blue-700 dark:bg-blue-950 dark:text-blue-300">
                    <span data-table-selection-count></span>
                    <button
                            type="button"
                            data-table-clear-selection
                            class="rounded font-medium underline-offset-2 hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2"
                    >
                        Clear selection
                    </button>
                </div>
            @endif

            @if (searchable)
            <label class="relative block sm:w-64">
                <span class="sr-only">${searchPlaceholder}</span>
                <span class="pointer-events-none absolute inset-y-0 left-2 flex items-center text-slate-400 dark:text-slate-500" aria-hidden="true">
                    @template.ashlar.components.icon(icon = Icon.SEARCH, classes = "h-4 w-4")
                </span>
                <input
                        type="search"
                        data-table-search
                        placeholder="${searchPlaceholder}"
                        class="block w-full rounded-md border border-slate-300 bg-white py-1.5 pl-8 pr-3 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
                >
            </label>
            @endif
        </div>
    @endif

    @if (loading)
        <%-- The spinner is passed an empty label on purpose: with one it
             becomes an announced live region, and the visible sentence
             beside it already says the same thing - a screen reader would
             have heard "Loading records" twice. Nothing here is a live
             region because nothing changes: the server rendered this state,
             so the text is simply read as page content. --%>
        <div data-table-loading class="flex items-center justify-center gap-3 px-4 py-12 text-sm text-slate-500 dark:text-slate-400">
            @template.ashlar.components.spinner(size = "Small", label = "")
            <span>Loading records&hellip;</span>
        </div>
    @else
        <%-- The scroller is the only thing allowed to overflow: a wide table
             scrolls inside its own surface instead of pushing the page
             sideways. --%>
        <div data-table-scroll class="w-full overflow-x-auto ${empty ? "hidden" : ""}">
            <table class="w-full border-collapse text-left text-sm">
                <caption class="${captionClasses}">${caption}</caption>
                <thead class="bg-slate-50 dark:bg-slate-800">
                    <tr>
                        @if (selectable)
                            <%-- Select-all covers the rows currently on
                                 screen, which is why its label says so: with
                                 paging and a search box in the same toolbar,
                                 "select all" without a scope is a promise the
                                 table cannot keep. --%>
                            <th scope="col" class="w-10 px-4 py-2.5">
                                <label class="flex items-center">
                                    <span class="sr-only">Select all rows on this page</span>
                                    <input
                                            type="checkbox"
                                            data-table-select-all
                                            class="h-4 w-4 cursor-pointer rounded border border-slate-300 bg-white accent-blue-600 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 dark:border-slate-600 dark:bg-slate-800"
                                    >
                                </label>
                            </th>
                        @endif
                        ${columns}
                    </tr>
                </thead>
                <tbody data-table-body class="divide-y divide-slate-200 dark:divide-slate-700">
                    !{if (selectable) TableContext.enterSelectable();}
                    ${rows}
                    !{TableContext.exit();}
                </tbody>
            </table>
        </div>

        <%-- Two states, not one message that changes meaning: "there is
             nothing here" and "your search matched nothing" are different
             situations and the second one is recoverable. Both are the
             library's own Empty State rather than a table-shaped copy of
             it. --%>
        <div data-table-empty="none" class="${empty ? "" : "hidden"} px-4 py-10">
            @template.ashlar.components.empty-state(
            icon = Icon.INBOX,
            title = emptyTitle,
            body = @`@if (!emptyBody.isEmpty())<p>${emptyBody}</p>@endif`
            )
        </div>

        <div data-table-empty="filtered" class="hidden px-4 py-10">
            @template.ashlar.components.empty-state(
            icon = Icon.SEARCH,
            title = "No matching records",
            body = @`<p>No rows match your search. Try a shorter or different term.</p>`
            )
        </div>
    @endif

    @if (paginated || hasFooterContent || searchable)
        <div class="flex flex-col gap-3 border-t border-slate-200 px-4 py-3 sm:flex-row sm:items-center sm:justify-between dark:border-slate-700">
            <%-- The local count is only rendered when the component is the
                 one deciding what is on screen. An application paging on the
                 server knows the real totals and puts its own summary in
                 `footer`; printing "Showing 4 of 4 records" next to "Page 2
                 of 5" would be the component contradicting the truth. --%>
            @if (paginated || searchable)
                <p data-table-status aria-live="polite" class="text-sm text-slate-500 dark:text-slate-400"></p>
            @endif

            @if (hasFooterContent)
                ${footer}
            @endif

            @if (paginated)
                <div data-table-pagination class="flex items-center gap-2">
                    <button
                            type="button"
                            data-table-prev
                            class="inline-flex items-center gap-1 rounded-md border border-slate-300 px-2.5 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 disabled:cursor-not-allowed disabled:opacity-50 dark:border-slate-600 dark:text-slate-200 dark:hover:bg-slate-800"
                    >
                        <span aria-hidden="true">@template.ashlar.components.icon(icon = Icon.CHEVRON_LEFT, classes = "h-4 w-4")</span>
                        Previous
                    </button>

                    <span data-table-page-indicator class="px-1 text-sm text-slate-600 dark:text-slate-300"></span>

                    <button
                            type="button"
                            data-table-next
                            class="inline-flex items-center gap-1 rounded-md border border-slate-300 px-2.5 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 disabled:cursor-not-allowed disabled:opacity-50 dark:border-slate-600 dark:text-slate-200 dark:hover:bg-slate-800"
                    >
                        Next
                        <span aria-hidden="true">@template.ashlar.components.icon(icon = Icon.CHEVRON_RIGHT, classes = "h-4 w-4")</span>
                    </button>
                </div>
            @endif
        </div>
    @endif
</div>

<script type="module" src="${AshlarAssets.js("table.js")}"></script>
