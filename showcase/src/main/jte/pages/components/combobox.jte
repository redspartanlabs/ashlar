@import com.redspartanlabs.ashlar.select.SelectOption
@import java.util.List

<%-- The reference implementation also imported tabs.Tab here, but never used
     the type - the page's only "Tab" reference is prose about keyboard tab
     order. Tabs has since migrated (Batch 8), so this is no longer a dead
     import kept out to avoid dragging in an unmigrated component; it's
     simply dropped as unused. --%>

@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Combobox",
content = @`
    @template.layout.component-nav(currentSlug = "combobox", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Combobox</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Select's editable sibling - type to filter, backed by the same hidden-&lt;select&gt; trick for native form submission and required validation. Only an explicit selection is ever submitted, never raw search text.</p>

            !{List<SelectOption> countries = List.of(
                SelectOption.of("us", "United States"),
                SelectOption.of("ca", "Canada"),
                SelectOption.of("mx", "Mexico"),
                SelectOption.of("gb", "United Kingdom"),
                SelectOption.of("fr", "France"),
                SelectOption.of("de", "Germany"),
                SelectOption.of("es", "Spain"),
                SelectOption.of("it", "Italy"),
                SelectOption.of("nl", "Netherlands"),
                SelectOption.of("se", "Sweden"),
                SelectOption.of("no", "Norway"),
                SelectOption.of("dk", "Denmark"),
                SelectOption.of("jp", "Japan"),
                SelectOption.of("kr", "South Korea"),
                SelectOption.of("cn", "China"),
                SelectOption.of("in", "India"),
                SelectOption.of("au", "Australia"),
                SelectOption.of("nz", "New Zealand"),
                SelectOption.of("br", "Brazil"),
                SelectOption.of("ar", "Argentina")
            );}
            !{List<SelectOption> assignees = List.of(
                SelectOption.of("jdoe", "Jane Doe"),
                SelectOption.of("bsmith", "Bob Smith"),
                SelectOption.of("mchen", "Mei Chen").disabled(true),
                SelectOption.of("agupta", "Aisha Gupta"),
                SelectOption.of("rmartinez", "Rosa Martinez")
            );}

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic &amp; preselected value</h3>
                    <div class="grid gap-4 sm:grid-cols-2">
                        @template.ashlar.components.combobox(
                        name = "assignee",
                        label = "Assignee",
                        options = assignees
                        )
                        @template.ashlar.components.combobox(
                        name = "preselectedCountry",
                        label = "Country",
                        options = countries,
                        value = "fr",
                        placeholder = "Search countries..."
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Large option set, scrolling &amp; no matches</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">20 countries - the list scrolls rather than growing unbounded. Try typing something that matches nothing, e.g. <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">xyz</code>.</p>
                    <div class="mt-3 max-w-sm">
                        @template.ashlar.components.combobox(
                        name = "country",
                        label = "Country",
                        options = countries,
                        placeholder = "Search countries..."
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Disabled option &amp; disabled combobox</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Mei Chen is visible but unavailable - not selectable by mouse, keyboard, or filtering.</p>
                    <div class="grid gap-4 sm:grid-cols-2">
                        @template.ashlar.components.combobox(
                        name = "assigneeWithUnavailable",
                        label = "Assignee",
                        options = assignees
                        )
                        @template.ashlar.components.combobox(
                        name = "disabledCombobox",
                        label = "Country (disabled)",
                        options = countries,
                        value = "de",
                        disabled = true
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Help text</h3>
                    <div class="max-w-sm">
                        @template.ashlar.components.combobox(
                        name = "helpCountry",
                        label = "Country",
                        options = countries,
                        placeholder = "Search countries...",
                        helpText = "Start typing to filter the list."
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Server-reported error</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Forced via the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">error</code> param - the same way a controller would re-render a field after a failed server-side check.</p>
                    <div class="mt-3 max-w-sm">
                        @template.ashlar.components.combobox(
                        name = "flaggedCountry",
                        label = "Country",
                        options = countries,
                        error = true,
                        errorMessage = "Shipping is not currently available to this country."
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Required validation, keyboard &amp; form submission</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Two independent required comboboxes. Tab in, use <kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">&darr;</kbd>/<kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">&uarr;</kbd> and <kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">Enter</kbd> to select, or type to filter first. Submitting empty shows the browser's own validation UI; submitting successfully reflects the selected option <em>values</em> (not the typed text) in the URL's query string.</p>
                    <form class="mt-3 flex flex-col gap-4" action="" method="get">
                        <div class="grid gap-4 sm:grid-cols-2">
                            @template.ashlar.components.combobox(
                            name = "requiredCountry",
                            label = "Country",
                            options = countries,
                            placeholder = "Search countries...",
                            required = true
                            )
                            @template.ashlar.components.combobox(
                            name = "requiredAssignee",
                            label = "Assignee",
                            options = assignees,
                            required = true
                            )
                        </div>
                        @template.ashlar.components.button(text = "Submit", type = "Primary", buttonType = "submit")
                    </form>
                </div>
            </div>
        </section>
`
)
