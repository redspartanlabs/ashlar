@import java.util.List
@import com.redspartanlabs.ashlar.select.SelectOption
@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam

@param String contextPath

<%-- Canonical documentation page (Batch 1), same grammar as Data Table and
     Modal. The pre-existing "Default & preselected value" / "Disabled
     options" / "Clearable" / "Type-ahead & keyboard interaction" /
     "Disabled Select" / "Required validation & form submission" examples
     are kept as Variants & States - they already stated the keyboard
     contract precisely and needed no rewriting. What's new: Quick Start,
     How to Use (the real Select/Combobox/Dropdown Menu decision, each
     component's own page already states its own side of it), Composition,
     Best Practices, Pitfalls, a restructured Accessibility section, and an
     API Reference table verified against this component's exact @param
     list (confirmed by grep against the source - notably Select has no
     helpText/error/errorMessage params at all, unlike most other form
     components; that's a real, deliberate API difference, not an
     oversight, so it's called out rather than silently worked around). --%>
!{List<SelectOption> quickStartOptions = List.of(
    SelectOption.of("development", "Development"),
    SelectOption.of("staging", "Staging"),
    SelectOption.of("production", "Production")
);}
!{String quickStartCode = """
    !{List<SelectOption> options = List.of(
        SelectOption.of("development", "Development"),
        SelectOption.of("staging", "Staging"),
        SelectOption.of("production", "Production")
    );}

    @template.ashlar.components.select(
        name = "environment",
        label = "Environment",
        options = options
    )""";}
!{List<ApiParam> selectApi = List.of(
    ApiParam.required("name", "String", "The field name values submit under."),
    ApiParam.required("label", "String", "Always rendered and always associated with the control - there is no labelless variant."),
    ApiParam.required("options", "List<SelectOption>", "Built via SelectOption.of(value, label), optionally .disabled(true)."),
    ApiParam.optional("value", "String", "\"\"", "The preselected option's value."),
    ApiParam.optional("placeholder", "String", "\"Select an option\"", "Shown only while nothing is selected."),
    ApiParam.optional("required", "boolean", "false", "Enables native constraint validation through the hidden <select>."),
    ApiParam.optional("disabled", "boolean", "false", "Disables the whole control."),
    ApiParam.optional("clearable", "boolean", "false", "Adds a clear (&times;) button that removes the selection entirely once one is made."),
    ApiParam.optional("requiredMessage", "String", "\"Please make a selection.\"", "Validation message, announced via role=\"alert\" when a required Select is submitted empty.")
);}

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

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3 max-w-sm">
                        @template.ashlar.components.select(
                        name = "quickStartEnvironment",
                        label = "Environment",
                        options = quickStartOptions
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">How to Use</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Reach for Select when a field has one fixed, mutually-exclusive list of options that submits a single value - a status, a region, a plan. For a long list a visitor needs to search rather than scan, use <a href="${contextPath}/components/combobox" class="text-blue-700 hover:underline dark:text-blue-400">Combobox</a> instead - same hidden-&lt;select&gt; mechanics, with type-to-filter added. For a trigger that performs actions or navigates rather than submitting a form value, use <a href="${contextPath}/components/dropdown-menu" class="text-blue-700 hover:underline dark:text-blue-400">Dropdown Menu</a> - it is not a form control at all.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>

                    <div class="mt-4 flex flex-col gap-8">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Default &amp; preselected value</p>
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
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Disabled options</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">US West and EU Central are at capacity - visible but not choosable, by keyboard or mouse.</p>
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
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Clearable</p>
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
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Type-ahead &amp; keyboard interaction</p>
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
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Disabled Select</p>
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
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Required validation &amp; form submission</p>
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
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composition</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The common pairing is one or more Selects inside a <a href="${contextPath}/components/filter-bar" class="text-blue-700 hover:underline dark:text-blue-400">Filter Bar</a>, alongside Search Input and Date Picker - each Select stays a normal form field, Filter Bar only arranges the row.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Best Practices</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Do</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>Use <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">clearable</code> for genuinely optional fields where "nothing selected" is a valid, different answer from any option.</li>
                                <li>Mark individual options <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">.disabled(true)</code> rather than removing them, when a choice exists but is temporarily unavailable - it stays visible and explainable.</li>
                            </ul>
                        </div>
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Avoid</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>A list long enough that a visitor would rather search than scan - that is Combobox's job.</li>
                                <li>Using Select for an action or a navigation trigger - it always submits a value; it is not a menu.</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Pitfalls &amp; Troubleshooting</h3>
                    <ul class="mt-2 list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">No built-in help text or error-message param.</strong> Unlike most other form components in this library, Select has neither <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">helpText</code> nor <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">errorMessage</code> - only <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">requiredMessage</code> for the browser's own native validation UI. A custom server-reported error needs its own surrounding markup; there is no parameter for it.</li>
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">Disabled options are still visible.</strong> If a choice genuinely should not be discoverable at all, remove it from <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">options</code> rather than marking it disabled.</li>
                    </ul>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Ashlar provides</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>Full keyboard operation: open/close, arrow-key movement, Home/End, and type-ahead jump-to-match.</li>
                                <li>A real, hidden native <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">&lt;select&gt;</code> so form submission and <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">required</code> validation are the browser's own, not reimplemented.</li>
                                <li>Disabled options are skipped by both keyboard and type-ahead, not just visually dimmed.</li>
                                <li>The required-field validation message renders with <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="alert"</code>, so it's announced the moment it appears.</li>
                            </ul>
                        </div>
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">label</code> - required, always rendered; there is no labelless mode to accidentally ship.</li>
                                <li>A <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">requiredMessage</code> worth reading aloud, if the default doesn't fit the field.</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div>
                    @template.layout.api-reference(apiParams = selectApi)
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/combobox" class="text-blue-700 hover:underline dark:text-blue-400">Combobox</a> for a searchable, type-to-filter version &middot;
                        <a href="${contextPath}/components/dropdown-menu" class="text-blue-700 hover:underline dark:text-blue-400">Dropdown Menu</a> for actions and navigation instead of a submitted value &middot;
                        <a href="${contextPath}/components/radio-group" class="text-blue-700 hover:underline dark:text-blue-400">Radio Group</a> when every option should stay visible at once &middot;
                        <a href="${contextPath}/components/filter-bar" class="text-blue-700 hover:underline dark:text-blue-400">Filter Bar</a> for arranging Select alongside other filter controls
                    </p>
                </div>
            </div>
        </section>
`
)
