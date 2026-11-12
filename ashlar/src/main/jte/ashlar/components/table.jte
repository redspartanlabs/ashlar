@import dev.redspartanlabs.ashlar.icon.Icon
@import dev.redspartanlabs.ashlar.table.TableContext
@import dev.redspartanlabs.ashlar.AshlarAssets

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
!{String captionClasses = captionVisible ? "ash-table__caption" : "ash-sr-only";}

<div
        data-table
        data-table-page-size="${pageSize}"
        class="ash-table"
>
    @if (searchable || selectable)
        <div class="ash-table__toolbar">
            <%-- Selection is counted here, not announced. The status region
                 further down is for filter and page changes, which the user
                 did not necessarily cause directly; ticking a box is already
                 confirmed by the checkbox's own state, and routing it through
                 a live region would narrate every click. --%>
            @if (selectable)
                <div data-table-selection-bar class="ash-table__selection" hidden>
                    <span data-table-selection-count></span>
                    <button
                            type="button"
                            data-table-clear-selection
                            class="ash-table__clear"
                    >
                        Clear selection
                    </button>
                </div>
            @endif

            @if (searchable)
            <label class="ash-table__search">
                <span class="ash-table__label ash-sr-only">${searchPlaceholder}</span>
                <span class="ash-table__search-icon" aria-hidden="true">
                    @template.ashlar.components.icon(icon = Icon.SEARCH, classes = "ash-icon--sm")
                </span>
                <input
                        type="search"
                        data-table-search
                        placeholder="${searchPlaceholder}"
                        class="ash-table__search-input"
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
        <div data-table-loading class="ash-table__loading">
            @template.ashlar.components.spinner(size = "Small", label = "")
            <span>Loading records&hellip;</span>
        </div>
    @else
        <%-- The scroller is the only thing allowed to overflow: a wide table
             scrolls inside its own surface instead of pushing the page
             sideways. --%>
        <div data-table-scroll class="ash-table__scroll" hidden="${empty}">
            <table class="ash-table__table">
                <caption class="${captionClasses}">${caption}</caption>
                <thead class="ash-table__head">
                    <tr>
                        @if (selectable)
                            <%-- Select-all covers the rows currently on
                                 screen, which is why its label says so: with
                                 paging and a search box in the same toolbar,
                                 "select all" without a scope is a promise the
                                 table cannot keep. --%>
                            <th scope="col" class="ash-table__select">
                                <label class="ash-table__check">
                                    <span class="ash-table__label ash-sr-only">Select all rows on this page</span>
                                    <input
                                            type="checkbox"
                                            data-table-select-all
                                            class="ash-table__checkbox"
                                    >
                                </label>
                            </th>
                        @endif
                        ${columns}
                    </tr>
                </thead>
                <tbody data-table-body class="ash-table__body">
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
        <div data-table-empty="none" class="ash-table__empty" hidden="${!empty}">
            @template.ashlar.components.empty-state(
            icon = Icon.INBOX,
            title = emptyTitle,
            body = @`@if (!emptyBody.isEmpty())<p>${emptyBody}</p>@endif`
            )
        </div>

        <div data-table-empty="filtered" class="ash-table__empty" hidden>
            @template.ashlar.components.empty-state(
            icon = Icon.SEARCH,
            title = "No matching records",
            body = @`<p>No rows match your search. Try a shorter or different term.</p>`
            )
        </div>
    @endif

    @if (paginated || hasFooterContent || searchable)
        <div class="ash-table__footer">
            <%-- The local count is only rendered when the component is the
                 one deciding what is on screen. An application paging on the
                 server knows the real totals and puts its own summary in
                 `footer`; printing "Showing 4 of 4 records" next to "Page 2
                 of 5" would be the component contradicting the truth. --%>
            @if (paginated || searchable)
                <p data-table-status aria-live="polite" class="ash-table__status"></p>
            @endif

            @if (hasFooterContent)
                ${footer}
            @endif

            @if (paginated)
                <div data-table-pagination class="ash-table__pager">
                    <button
                            type="button"
                            data-table-prev
                            class="ash-table__page-button"
                    >
                        <span aria-hidden="true">@template.ashlar.components.icon(icon = Icon.CHEVRON_LEFT, classes = "ash-icon--sm")</span>
                        Previous
                    </button>

                    <span data-table-page-indicator class="ash-table__page-indicator"></span>

                    <button
                            type="button"
                            data-table-next
                            class="ash-table__page-button"
                    >
                        Next
                        <span aria-hidden="true">@template.ashlar.components.icon(icon = Icon.CHEVRON_RIGHT, classes = "ash-icon--sm")</span>
                    </button>
                </div>
            @endif
        </div>
    @endif
</div>

<script type="module" src="${AshlarAssets.js("table.js")}"></script>
