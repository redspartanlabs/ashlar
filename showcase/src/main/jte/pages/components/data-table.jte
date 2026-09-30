@import com.redspartanlabs.ashlar.showcase.datatable.SampleAccountData

@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Data Table",
content = @`
    @template.layout.component-nav(currentSlug = "data-table", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Data Table</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The model-driven table: declare columns, hand over rows. It renders through Table, so both share one DOM, one script and one set of theme rules - this adds the column model and optional selection. It presents data; it never fetches, formats or mutates it.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accounts</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Eight columns: a monospaced identifier, text, a Badge status, dates and two numeric columns. Sort the numbers and the dates - they compare as numbers and dates, not as the formatted strings you can see. Select rows, then page or search: the selection follows the rows. Search for something absent (try "zzz") for the no-results state.</p>
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
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Read-only, no selection</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Selection and search are both opt-out, and <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">rowKey</code> is useful without either: it still names the column that identifies a row, so that cell stays the row's header for a screen reader. Drop it and there is no row header and no selection column at all, because the component will not guess which field identifies a record.</p>
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
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">No records at all</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">"There is nothing here" is a different situation from "your search matched nothing", and it is the library's own Empty State in both cases. There is no <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">empty</code> parameter to pass - the component can see that the row list is empty.</p>
                    <div class="mt-3">
                        @template.ashlar.components.data-table(
                        caption = "Archived accounts",
                        columns = SampleAccountData.columns(),
                        rows = SampleAccountData.noRows(),
                        emptyTitle = "No archived accounts",
                        emptyBody = "Accounts appear here once they have been archived."
                        )
                    </div>
                </div>
            </div>
        </section>
`
)
