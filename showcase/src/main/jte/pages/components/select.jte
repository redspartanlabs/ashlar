@import com.redspartanlabs.ashlar.select.SelectOption
@import java.util.List

@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Select List",
content = @`
    @template.layout.component-nav(currentSlug = "select", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Select</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A custom listbox dropdown backed by a real, hidden &lt;select&gt; - full keyboard support, type-ahead, and native form submission.</p>

            !{List<SelectOption> environments = List.of(
                SelectOption.of("development", "Development"),
                SelectOption.of("test", "Test"),
                SelectOption.of("staging", "Staging"),
                SelectOption.of("production", "Production")
            );}
            !{List<SelectOption> statuses = List.of(
                SelectOption.of("active", "Active"),
                SelectOption.of("pending", "Pending"),
                SelectOption.of("suspended", "Suspended"),
                SelectOption.of("archived", "Archived")
            );}
            !{List<SelectOption> regions = List.of(
                SelectOption.of("us-east", "US East"),
                SelectOption.of("us-west", "US West (at capacity)").disabled(true),
                SelectOption.of("eu-west", "EU West"),
                SelectOption.of("eu-central", "EU Central (at capacity)").disabled(true),
                SelectOption.of("ap-southeast", "AP Southeast")
            );}

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Default &amp; preselected value</h3>
                    <div class="mt-3 grid gap-4 sm:grid-cols-2">
                        @template.ashlar.components.select(
                        name = "environment",
                        label = "Environment",
                        placeholder = "Select an environment",
                        options = environments
                        )
                        @template.ashlar.components.select(
                        name = "status",
                        label = "Status",
                        options = statuses,
                        value = "active"
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Disabled options</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">US West and EU Central are at capacity - they're visible but can't be chosen, by keyboard or mouse.</p>
                    <div class="mt-3 max-w-sm">
                        @template.ashlar.components.select(
                        name = "region",
                        label = "Region",
                        placeholder = "Select a region",
                        options = regions
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Clearable</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A preselected value with a clear (&times;) button to remove the selection entirely.</p>
                    <div class="mt-3 max-w-sm">
                        @template.ashlar.components.select(
                        name = "clearableStatus",
                        label = "Status",
                        options = statuses,
                        value = "pending",
                        clearable = true
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Type-ahead &amp; keyboard interaction</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Focus the trigger and press <kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">Enter</kbd>/<kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">Space</kbd> or <kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">&darr;</kbd> to open, arrow keys to move, <kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">Home</kbd>/<kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">End</kbd> to jump, or just start typing - e.g. type <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">s</code> to jump straight to Staging.</p>
                    <div class="mt-3 max-w-sm">
                        @template.ashlar.components.select(
                        name = "keyboardEnvironment",
                        label = "Environment",
                        placeholder = "Select an environment",
                        options = environments
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Disabled Select</h3>
                    <div class="mt-3 max-w-sm">
                        @template.ashlar.components.select(
                        name = "disabledEnvironment",
                        label = "Environment",
                        options = environments,
                        value = "production",
                        disabled = true
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Required validation &amp; form submission</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Both fields are independent, required Select instances. Try submitting empty, then check the URL's query string for the submitted values.</p>
                    <form class="mt-3 flex flex-col gap-4" action="" method="get">
                        <div class="grid gap-4 sm:grid-cols-2">
                            @template.ashlar.components.select(
                            name = "requiredEnvironment",
                            label = "Environment",
                            placeholder = "Select an environment",
                            options = environments,
                            required = true
                            )
                            @template.ashlar.components.select(
                            name = "requiredStatus",
                            label = "Status",
                            placeholder = "Select a status",
                            options = statuses,
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
