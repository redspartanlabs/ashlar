@import java.util.List
@import com.redspartanlabs.ashlar.select.SelectOption
@import com.redspartanlabs.ashlar.showcase.datatable.SampleAccountData
@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam

@param String contextPath

<%-- Batch 12 (final) documentation page. Existing "Quick filters over a
     dataset" / "Filters already applied" / "Two filters" / "Stacking on a
     phone" / "Above the data it filters" examples kept verbatim. Added:
     Quick Start, formal grammar headings, How to Use, Composition,
     Pitfalls, Accessibility, API Reference, Related Components.

     Verified against source before writing:
     - filter-bar.jte's own 2 @params (content, label) and its unusually
       extensive doc comment. CONFIRMED, concretely, why this utility
       exists at all rather than being redundant with ordinary flex
       utilities: every Ashlar form control is built for a stacked-form
       box model (a block <label>, a control, mb-6/mb-4 trailing
       rhythm), and dropping several into a plain row leaves dead space
       underneath and fields sized by their own placeholder text. This
       isn't hypothetical - the component's own doc comment cites a real
       precedent: table.jte hand-rolled its own search field rather than
       compose search-input.jte, precisely because search-input's
       stacked-form box model didn't fit a toolbar. Filter Bar normalizes
       exactly that mismatch in one place: `[&>*]:mb-0` strips the
       trailing margin every control already carries, and
       `w-full sm:w-48` (via `:not(button)`, so a submit/clear button
       keeps its own size) gives fields one predictable column width
       instead of each sizing itself from its placeholder.
     - CONFIRMED, explicitly, the real distinction from Faceted Search:
       Faceted Search is a vertical sidebar of checkbox-only facets,
       where every option submits under one shared name - it "cannot
       hold a Select, a Date Picker or a Button." Filter Bar is the
       other pattern: a horizontal row of whatever heterogeneous
       controls the application already has. The component's own doc
       comment states directly that "the two never compose with each
       other, so neither can be expressed as the other."
     - CONFIRMED there is no actions slot and no JavaScript at all -
       buttons go inline in `content`, after the last filter, since "a
       filter row is read left to right as one sequence."
     - CONFIRMED NO CHIPS, NO CLEAR BUTTON BEHAVIOR, NO RESET LOGIC: all
       three would require knowing which filters are active, which means
       knowing what the filters mean - explicitly the application's job,
       not this utility's, per its own doc comment's direct statement.
     - Confirmed a real <fieldset> with a real, sr-only <legend> - the
       same grouping facet.jte/radio-group.jte already use - sr-only
       because a row of controls sitting under a page heading is already
       self-evident on screen, the same reasoning table.jte's own
       <caption> uses. --%>
!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.utilities.filter-bar(\n"
        + "    content = @" + backtick + "\n"
        + "        @template.ashlar.components.search-input(name = \"q\", label = \"Search\")\n"
        + "        @template.ashlar.components.button(text = \"Apply\", type = \"Primary\", buttonType = \"submit\")\n"
        + "    " + backtick + "\n"
        + ")";}
!{List<ApiParam> filterBarApi = List.of(
    ApiParam.required("content", "gg.jte.Content", "The filter controls (and, inline after them, any submit/clear buttons) - rendered in exactly the order supplied, never inspected or reordered."),
    ApiParam.optional("label", "String", "\"Filters\"", "An sr-only <legend> naming the fieldset for assistive tech - a visible \"Filters\" label is Page Section's job, not this one's.")
);}

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

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        <form method="get">
                            @template.ashlar.utilities.filter-bar(
                            content = @`
                                @template.ashlar.components.search-input(name = "qQuickStart", label = "Search")
                                @template.ashlar.components.button(text = "Apply", type = "Primary", buttonType = "submit")
                            `
                            )
                        </form>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">How to Use</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Reach for Filter Bar when you have a handful of heterogeneous controls - a search field, a couple of Selects, maybe a Date Picker - that need to sit in one compact row above a table or list, without the dead space and placeholder-driven sizing a plain flex row of stacked-form controls would leave. Reach for <a href="${contextPath}/components/faceted-search" class="text-blue-700 hover:underline dark:text-blue-400">Faceted Search</a> instead when every filter is a checkbox option under one name - the two patterns don't compose with each other, so pick based on what the controls actually are, not how the row looks.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Quick filters over a dataset</p>
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
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Filters already applied</p>
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
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Two filters</p>
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
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Stacking on a phone</p>
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
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composition</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">In context, with <a href="${contextPath}/components/page-section" class="text-blue-700 hover:underline dark:text-blue-400">Page Section</a> supplying the visible heading and <a href="${contextPath}/components/data-table" class="text-blue-700 hover:underline dark:text-blue-400">Data Table</a> supplying the results. The utility has no surface of its own, so it sits directly on whatever the page is - here a dashed frame, on a real page a Card or nothing at all.</p>
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

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Pitfalls &amp; Troubleshooting</h3>
                    <ul class="mt-2 list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">There are no filter chips, no clear-button behavior, and no reset logic built in.</strong> All three require knowing which filters are active and what they mean - this utility only arranges controls, it never reads them. "Clear" in the examples above is an ordinary submit button under its own name, not a feature of Filter Bar.</li>
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">There is no actions slot.</strong> Buttons go inline in <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">content</code>, after the last filter - a filter row reads left to right as one sequence, so a separate slot would only exist to push buttons to the right, which isn't the pattern.</li>
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">Filter Bar and Faceted Search never compose with each other.</strong> One is a horizontal row of heterogeneous controls; the other is a vertical sidebar of checkbox-only facets sharing one field name. Picking the wrong one for a given set of filters isn't a styling problem to fix - it's the wrong component.</li>
                    </ul>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Ashlar provides</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>A real &lt;fieldset&gt; with a real, sr-only &lt;legend&gt; naming the group for assistive tech - with no aria-label or role="group" needed.</li>
                            </ul>
                        </div>
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>The enclosing &lt;form&gt;, its method, and every field's own name - this utility owns none of that.</li>
                                <li>A visible "Filters" heading via Page Section, if the page needs one - the legend here is intentionally sr-only.</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div>
                    @template.layout.api-reference(apiParams = filterBarApi)
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/faceted-search" class="text-blue-700 hover:underline dark:text-blue-400">Faceted Search</a> for checkbox-only facets instead of heterogeneous controls &middot;
                        <a href="${contextPath}/components/form-actions" class="text-blue-700 hover:underline dark:text-blue-400">Form Actions</a> for a form's own end-aligned footer buttons &middot;
                        <a href="${contextPath}/components/data-table" class="text-blue-700 hover:underline dark:text-blue-400">Data Table</a>, the usual results neighbor
                    </p>
                </div>
            </div>
        </section>
`
)
