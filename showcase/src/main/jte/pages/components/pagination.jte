@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import com.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

<%-- Batch 8 documentation page. Existing "Basic pagination" / "Previous /
     Next" / "Larger page range" / "Multiple instances stay independent"
     examples kept verbatim, moved under Variants & States. "Beneath a
     data table" is kept but its result-set table is now a real
     @template.ashlar.components.table(...) call instead of a hand-rolled
     <table> - the same class of fix already applied to Badge's "Inside a
     table" example in Batch 5, for the same reason (the original
     pre-Batch-0 audit flagged hand-rolled tables in showcase examples as
     a known defect). Pagination's own behavior is unchanged; this only
     fixes what the example around it demonstrates. Added: Quick Start,
     formal grammar headings, How to Use, Composition (pagination-item
     documented here, not as a standalone page), Accessibility, two API
     Reference tables, Related Components.

     Verified against source before writing:
     - pagination.jte's own single @param and doc comment. Confirmed
       zero JavaScript anywhere - navigation is real <a href>s the
       browser handles on its own - and a fixed aria-label="Pagination"
       for the same APG-landmark-name reason breadcrumbs.jte's own
       aria-label is fixed. Confirmed <ul>, not <ol>: page numbers
       already carry their own visible order ("1","2","3"), so <ol>'s
       implicit numbering would be redundant noise read ahead of a
       "Previous" entry that isn't a numbered page at all - confirmed
       this matches the WAI-ARIA APG's own pagination example.
     - pagination-item.jte's own 3 @params and its extensive doc
       comment. CONFIRMED PRECISELY, not inferred: a disabled
       Previous/Next and an ellipsis render through the exact SAME
       branch (not current, no href) - there is no separate `disabled`
       param and no separate ellipsis concept at all; an ellipsis is
       just an ordinary pagination-item with label="…" and no href.
       CONFIRMED the component never inspects `label` to decide
       anything - its own doc comment states this explicitly, which is
       also why there are no automatic chevron icons for Previous/Next:
       the API has no way to know a label means "Previous" rather than
       any other string, so the visible label text is the only thing
       that communicates it. Confirmed current=true wins over href even
       when both are supplied, the same precedence rule breadcrumb-item
       uses and for the same aria-current reason. --%>
!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.components.pagination(\n"
        + "    items = @" + backtick + "\n"
        + "        @template.ashlar.components.pagination-item(label = \"1\", href = \"?page=1\")\n"
        + "        @template.ashlar.components.pagination-item(label = \"2\", current = true)\n"
        + "    " + backtick + "\n"
        + ")";}
!{List<ApiParam> paginationApi = List.of(
    ApiParam.required("items", "gg.jte.Content", "One or more pagination-item(...) calls.")
);}
!{List<ApiParam> paginationItemApi = List.of(
    ApiParam.required("label", "String", "The entry's visible text - never inspected to infer meaning. A plain string like \"Previous\" or \"…\" is rendered exactly as given."),
    ApiParam.optional("href", "String", "null", "Renders a real <a>. Ignored when current is true."),
    ApiParam.optional("current", "boolean", "false", "Marks this as the page already being viewed. A non-current item with no href covers two different cases at once - a disabled Previous/Next, and an ellipsis - both rendered as non-interactive text with aria-disabled=\"true\".")
);}
!{List<PageAnchor> sections = List.of(
    new PageAnchor("quick-start", "Quick Start"),
    new PageAnchor("how-to-use", "How to Use"),
    new PageAnchor("variants", "Variants & States"),
    new PageAnchor("composition", "Composition"),
    new PageAnchor("accessibility", "Accessibility"),
    new PageAnchor("api-reference", "API Reference"),
    new PageAnchor("related", "Related Components")
);}

@template.layout.page(
contextPath = contextPath,
title = "Pagination",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "pagination", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Semantic navigation between pages of a larger result set - no JavaScript, no URL construction, no page-count calculation. The caller supplies every label, href, and current flag; the component only renders them.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3">
                    @template.ashlar.components.pagination(
                    items = @`
                        @template.ashlar.components.pagination-item(label = "1", href = "?page=1")
                        @template.ashlar.components.pagination-item(label = "2", current = true)
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
                <p class="text-sm text-slate-500 dark:text-slate-400">Pagination only ever renders the entries it's given - your controller decides the page numbers, whether an ellipsis is needed, and which window of pages to show (page 4 of 50 rarely lists all 50). Give Previous/Next a real <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">href</code> only when that direction is actually available; omit it to render disabled automatically. For a model-driven table with its own built-in client-side paging instead of real page links, see <a href="${contextPath}/components/data-table" class="text-blue-700 hover:underline dark:text-blue-400">Data Table</a> or Table's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">pageSize</code>.</p>
            `
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Basic pagination</p>
                        <div class="mt-3">
                            @template.ashlar.components.pagination(
                            items = @`
                                @template.ashlar.components.pagination-item(label = "1", href = "?page=1")
                                @template.ashlar.components.pagination-item(label = "2", current = true)
                                @template.ashlar.components.pagination-item(label = "3", href = "?page=3")
                            `
                            )
                        </div>
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Previous / Next</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The first example is on page 1, so Previous has no <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">href</code> and renders disabled. The second is mid-range, so both controls are active.</p>
                        <div class="mt-3 flex flex-col gap-3">
                            @template.ashlar.components.pagination(
                            items = @`
                                @template.ashlar.components.pagination-item(label = "Previous")
                                @template.ashlar.components.pagination-item(label = "1", current = true)
                                @template.ashlar.components.pagination-item(label = "2", href = "?page=2")
                                @template.ashlar.components.pagination-item(label = "3", href = "?page=3")
                                @template.ashlar.components.pagination-item(label = "Next", href = "?page=2")
                            `
                            )
                            @template.ashlar.components.pagination(
                            items = @`
                                @template.ashlar.components.pagination-item(label = "Previous", href = "?page=1")
                                @template.ashlar.components.pagination-item(label = "1", href = "?page=1")
                                @template.ashlar.components.pagination-item(label = "2", current = true)
                                @template.ashlar.components.pagination-item(label = "3", href = "?page=3")
                                @template.ashlar.components.pagination-item(label = "Next", href = "?page=3")
                            `
                            )
                        </div>
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Larger page range</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The application decides which numbers and ellipses to show - Pagination just renders whatever items it's given.</p>
                        <div class="mt-3">
                            @template.ashlar.components.pagination(
                            items = @`
                                @template.ashlar.components.pagination-item(label = "Previous", href = "?page=3")
                                @template.ashlar.components.pagination-item(label = "1", href = "?page=1")
                                @template.ashlar.components.pagination-item(label = "2", href = "?page=2")
                                @template.ashlar.components.pagination-item(label = "3", href = "?page=3")
                                @template.ashlar.components.pagination-item(label = "4", current = true)
                                @template.ashlar.components.pagination-item(label = "5", href = "?page=5")
                                @template.ashlar.components.pagination-item(label = "…")
                                @template.ashlar.components.pagination-item(label = "10", href = "?page=10")
                                @template.ashlar.components.pagination-item(label = "Next", href = "?page=5")
                            `
                            )
                        </div>
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Multiple instances stay independent</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">No ids, no JavaScript, no shared state - each is just its own rendered markup.</p>
                        <div class="mt-3 grid gap-4 sm:grid-cols-2">
                            <div>
                                <p class="mb-2 text-sm font-medium text-slate-700 dark:text-slate-300">Search results</p>
                                @template.ashlar.components.pagination(
                                items = @`
                                    @template.ashlar.components.pagination-item(label = "1", current = true)
                                    @template.ashlar.components.pagination-item(label = "2", href = "?page=2")
                                `
                                )
                            </div>
                            <div>
                                <p class="mb-2 text-sm font-medium text-slate-700 dark:text-slate-300">Audit log</p>
                                @template.ashlar.components.pagination(
                                items = @`
                                    @template.ashlar.components.pagination-item(label = "1", href = "?page=1")
                                    @template.ashlar.components.pagination-item(label = "2", current = true)
                                    @template.ashlar.components.pagination-item(label = "3", href = "?page=3")
                                `
                                )
                            </div>
                        </div>
                    </div>
                </div>
            `
            )
        </section>

        <section id="composition" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Composition",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Pagination is the wrapper half of a wrapper/item pair, the same split Breadcrumbs uses. <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">pagination-item</code> is not a separate catalog component; it only ever exists composed into Pagination's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">items</code> slot, documented here in the table below. Pagination is a separate component from <a href="${contextPath}/components/data-table" class="text-blue-700 hover:underline dark:text-blue-400">Data Table</a> - it composes underneath a result set rather than owning it, and is also exactly what <a href="${contextPath}/components/table" class="text-blue-700 hover:underline dark:text-blue-400">Table</a>'s own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">footer</code> slot is for when an application pages on the server.</p>
                <div class="mt-3 overflow-hidden rounded-xl border border-slate-200 dark:border-slate-700">
                    @template.ashlar.components.table(
                    caption = "Accounts",
                    captionVisible = false,
                    columns = @`
                        @template.ashlar.components.table-column(label = "Account")
                        @template.ashlar.components.table-column(label = "Plan")
                        @template.ashlar.components.table-column(label = "Status")
                    `,
                    rows = @`
                        @template.ashlar.components.table-row(cells = @`
                            @template.ashlar.components.table-cell(text = "Acme Manufacturing", header = true)
                            @template.ashlar.components.table-cell(text = "Enterprise")
                            @template.ashlar.components.table-cell(content = @`@template.ashlar.components.badge(text = "Active", variant = "Success")`)
                        `)
                        @template.ashlar.components.table-row(cells = @`
                            @template.ashlar.components.table-cell(text = "Kestrel Aerospace", header = true)
                            @template.ashlar.components.table-cell(text = "Enterprise")
                            @template.ashlar.components.table-cell(content = @`@template.ashlar.components.badge(text = "Active", variant = "Success")`)
                        `)
                        @template.ashlar.components.table-row(cells = @`
                            @template.ashlar.components.table-cell(text = "Bramble Logistics", header = true)
                            @template.ashlar.components.table-cell(text = "Standard")
                            @template.ashlar.components.table-cell(content = @`@template.ashlar.components.badge(text = "Trial", variant = "Warning")`)
                        `)
                    `,
                    footer = @`
                        <p class="text-sm text-slate-500 dark:text-slate-400">Showing 1-3 of 24 accounts</p>
                        @template.ashlar.components.pagination(
                        items = @`
                            @template.ashlar.components.pagination-item(label = "Previous")
                            @template.ashlar.components.pagination-item(label = "1", current = true)
                            @template.ashlar.components.pagination-item(label = "2", href = "?page=2")
                            @template.ashlar.components.pagination-item(label = "3", href = "?page=3")
                            @template.ashlar.components.pagination-item(label = "…")
                            @template.ashlar.components.pagination-item(label = "8", href = "?page=8")
                            @template.ashlar.components.pagination-item(label = "Next", href = "?page=2")
                        `
                        )
                    `
                    )
                </div>
                <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">Links above are static demonstration hrefs, not real navigation.</p>
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
                            <li>A real &lt;nav aria-label="Pagination"&gt; landmark, named exactly as the WAI-ARIA APG specifies.</li>
                            <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-current="page"</code> on the current entry, and <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-disabled="true"</code> on a disabled Previous/Next or an ellipsis - neither is a real link, so neither is focusable or looks clickable.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Every <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">href</code>, label, and <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">current</code> flag - there is no page-count calculation or URL construction here at all.</li>
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
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">pagination</p>
                        @template.layout.api-reference(apiParams = paginationApi)
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">pagination-item</p>
                        @template.layout.api-reference(apiParams = paginationItemApi)
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
                    <a href="${contextPath}/components/breadcrumbs" class="text-blue-700 hover:underline dark:text-blue-400">Breadcrumbs</a>, the same wrapper/item split applied to a hierarchy rather than page numbers &middot;
                    <a href="${contextPath}/components/table" class="text-blue-700 hover:underline dark:text-blue-400">Table</a>, whose footer slot is this component's usual home &middot;
                    <a href="${contextPath}/components/data-table" class="text-blue-700 hover:underline dark:text-blue-400">Data Table</a> for built-in client-side paging instead of real page links
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "pagination", contextPath = contextPath)
    </div>
`
)
