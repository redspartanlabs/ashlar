@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Search Input",
content = @`
    @template.layout.component-nav(currentSlug = "search-input", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Search Input</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A real &lt;input type="search"&gt; - a built-in clear button, no separate configuration for it. The only JavaScript keeps that button in sync with what's actually typed.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic &amp; placeholder</h3>
                    <div class="grid gap-4 sm:grid-cols-2">
                        @template.ashlar.components.search-input(
                        name = "searchCustomers",
                        label = "Search Customers"
                        )
                        @template.ashlar.components.search-input(
                        name = "searchJobHeaders",
                        label = "Search Job Headers",
                        placeholder = "Search by job number or title..."
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Pre-populated (clear button visible)</h3>
                    <div class="max-w-sm">
                        @template.ashlar.components.search-input(
                        name = "searchEmployees",
                        label = "Search Employees",
                        value = "Jane Doe"
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Disabled &amp; help text</h3>
                    <div class="grid gap-4 sm:grid-cols-2">
                        @template.ashlar.components.search-input(
                        name = "disabledSearch",
                        label = "Search Reports",
                        value = "Q3 revenue",
                        disabled = true
                        )
                        @template.ashlar.components.search-input(
                        name = "helpSearch",
                        label = "Search Reports",
                        helpText = "Search by report name, owner, or date range."
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Server-reported error</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Forced via the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">error</code> param - the same way a controller would re-render a field after a failed server-side check.</p>
                    <div class="mt-3 max-w-sm">
                        @template.ashlar.components.search-input(
                        name = "flaggedSearch",
                        label = "Search Customers",
                        value = "'; DROP TABLE customers;",
                        error = true,
                        errorMessage = "Search terms can't contain special characters."
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Multiple instances, required validation &amp; a real GET form</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Two independent, required Search Inputs. Type in one and clear it - the other is unaffected. Press <kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">Enter</kbd> in either field to submit the form normally; clicking a clear button never does. Submitting successfully reflects both values in the URL's query string.</p>
                    <form class="mt-3 flex flex-col gap-4" action="" method="get">
                        <div class="grid gap-4 sm:grid-cols-2">
                            @template.ashlar.components.search-input(
                            name = "requiredSearch",
                            label = "Search Customers",
                            required = true
                            )
                            @template.ashlar.components.search-input(
                            name = "secondSearch",
                            label = "Search Reports",
                            required = true
                            )
                        </div>
                        @template.ashlar.components.button(text = "Search", type = "Primary", buttonType = "submit")
                    </form>
                </div>
            </div>
        </section>
`
)
