@import java.util.List
@import com.redspartanlabs.ashlar.select.SelectOption
@import com.redspartanlabs.ashlar.showcase.datatable.SampleAccountData

@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Filter Bar",
content = @`
    @template.layout.component-nav(currentSlug = "filter-bar", contextPath = contextPath)

        !{List<SelectOption> statuses = List.of(
            SelectOption.of("active", "Active"),
            SelectOption.of("pending", "Pending"),
            SelectOption.of("error", "Error"),
            SelectOption.of("inactive", "Inactive")
        );}
        !{List<SelectOption> accountTypes = List.of(
            SelectOption.of("enterprise", "Enterprise"),
            SelectOption.of("government", "Government"),
            SelectOption.of("smb", "SMB")
        );}
        !{List<SelectOption> owners = List.of(
            SelectOption.of("dana", "Dana Whitfield"),
            SelectOption.of("marcus", "Marcus Lee"),
            SelectOption.of("priya", "Priya Natarajan"),
            SelectOption.of("sofia", "Sofia Marchetti")
        );}

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Filter Bar</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A layout utility for a row of quick filters above a dataset. It arranges controls the application already has - Search Input, Select, Date Picker, Button - and does nothing else: no form, no field names, no filtering. Contrast Faceted Search, which is a sidebar of checkbox facets and is its own filtering UI.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Quick filters over a dataset</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The whole pattern: the page owns the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">&lt;form&gt;</code>, its method and its field names; the bar owns the row. Nothing is filtered here - this demo page has no server-side filtering, so submitting just puts the values in the query string, which is the application's to read. "Clear" submits under its own name rather than resetting the form: on a server-rendered page the current filters <em>are</em> the rendered state, so a native reset would restore them, not remove them.</p>
                    <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        <form method="get" class="flex flex-col gap-4">
                            @template.ashlar.utilities.filter-bar(
                            content = @`
                                @template.ashlar.components.search-input(name = "q", label = "Search accounts", placeholder = "Search accounts")
                                @template.ashlar.components.select(name = "status", label = "Status", options = statuses, placeholder = "Any status", clearable = true)
                                @template.ashlar.components.select(name = "accountType", label = "Account type", options = accountTypes, placeholder = "Any type", clearable = true)
                                @template.ashlar.components.select(name = "owner", label = "Owner", options = owners, placeholder = "Anyone", clearable = true)
                                @template.ashlar.components.date-picker(name = "createdAfter", label = "Created after", placeholder = "Any date")
                                @template.ashlar.components.button(text = "Apply", type = "Primary", buttonType = "submit")
                                @template.ashlar.components.button(text = "Clear", type = "Ghost", buttonType = "submit", attributes = "name=\"clear\" value=\"1\"")
                            `
                            )

                            @template.ashlar.utilities.status-line(content = @`14 accounts match the current filters.`)
                        </form>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Filters already applied</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The same bar with values rendered back into the controls, which is how a server-rendered page shows the current filter state. The utility does not track that state or display it a second time as chips - it cannot know what "active" means, and a row of controls that already show their own values does not need repeating.</p>
                    <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        <form method="get" class="flex flex-col gap-4">
                            @template.ashlar.utilities.filter-bar(
                            content = @`
                                @template.ashlar.components.search-input(name = "q2", label = "Search accounts", value = "cascade", placeholder = "Search accounts")
                                @template.ashlar.components.select(name = "status2", label = "Status", options = statuses, value = "active", clearable = true)
                                @template.ashlar.components.select(name = "accountType2", label = "Account type", options = accountTypes, value = "enterprise", clearable = true)
                                @template.ashlar.components.select(name = "owner2", label = "Owner", options = owners, placeholder = "Anyone", clearable = true)
                                @template.ashlar.components.date-picker(name = "createdAfter2", label = "Created after", value = "2023-01-01")
                                @template.ashlar.components.button(text = "Apply", type = "Primary", buttonType = "submit")
                                @template.ashlar.components.button(text = "Clear", type = "Ghost", buttonType = "submit", attributes = "name=\"clear\" value=\"1\"")
                            `
                            )

                            @template.ashlar.utilities.status-line(content = @`1 account matches the current filters.`)
                        </form>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Two filters</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Fields keep one column width rather than stretching to fill the row, so a short bar looks like a short bar. A submit button is not required either - a page that filters on change, or on Enter in the search field, simply does not pass one.</p>
                    <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        <form method="get">
                            @template.ashlar.utilities.filter-bar(
                            content = @`
                                @template.ashlar.components.search-input(name = "q3", label = "Search accounts", placeholder = "Search accounts")
                                @template.ashlar.components.select(name = "status3", label = "Status", options = statuses, placeholder = "Any status", clearable = true)
                            `
                            )
                        </form>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Stacking on a phone</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Below the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">sm</code> breakpoint every filter becomes one full-width row. There is no horizontal scrolling strip: the reading and tab order stay exactly as written, and nothing pushes the page sideways. This frame is 360px wide to show it without resizing the window.</p>
                    <div class="mt-3 w-[360px] max-w-full rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        <form method="get">
                            @template.ashlar.utilities.filter-bar(
                            content = @`
                                @template.ashlar.components.search-input(name = "q4", label = "Search accounts", placeholder = "Search accounts")
                                @template.ashlar.components.select(name = "status4", label = "Status", options = statuses, placeholder = "Any status", clearable = true)
                                @template.ashlar.components.date-picker(name = "createdAfter4", label = "Created after", placeholder = "Any date")
                                @template.ashlar.components.button(text = "Apply", type = "Primary", buttonType = "submit", fullWidth = true)
                            `
                            )
                        </form>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Above the data it filters</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">In context, with Page Section supplying the visible heading and Data Table supplying the results. The utility has no surface of its own, so it sits directly on whatever the page is - here a dashed frame, on a real page a Card or nothing at all.</p>
                    <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.page-section(
                        title = "Customer Accounts",
                        content = @`
                            <form method="get" class="mb-4">
                                @template.ashlar.utilities.filter-bar(
                                content = @`
                                    @template.ashlar.components.search-input(name = "q5", label = "Search accounts", placeholder = "Search accounts")
                                    @template.ashlar.components.select(name = "status5", label = "Status", options = statuses, placeholder = "Any status", clearable = true)
                                    @template.ashlar.components.select(name = "owner5", label = "Owner", options = owners, placeholder = "Anyone", clearable = true)
                                    @template.ashlar.components.button(text = "Apply", type = "Primary", buttonType = "submit")
                                `
                                )
                            </form>

                            @template.ashlar.components.data-table(
                            caption = "Customer accounts",
                            columns = SampleAccountData.columns(),
                            rows = SampleAccountData.rows(),
                            rowKey = "id",
                            searchable = false,
                            pageSize = 5
                            )
                        `
                        )
                    </div>
                </div>
            </div>
        </section>
`
)
