@import java.util.List
@import dev.redspartanlabs.ashlar.select.SelectOption
@import dev.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import dev.redspartanlabs.ashlar.showcase.catalog.PageAnchor

@param String contextPath

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
!{String defaultPreselectedCode = """
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
    )""";}
!{String disabledOptionsCode = """
    @template.ashlar.components.select(
        name = "region",
        label = "Region",
        placeholder = "Select a region",
        options = regions
    )""";}
!{String clearableCode = """
    @template.ashlar.components.select(
        name = "clearableStatus",
        label = "Status",
        options = statuses,
        value = "pending",
        clearable = true
    )""";}
!{String keyboardCode = """
    @template.ashlar.components.select(
        name = "keyboardEnvironment",
        label = "Environment",
        placeholder = "Select an environment",
        options = environments
    )""";}
!{String disabledSelectCode = """
    @template.ashlar.components.select(
        name = "disabledEnvironment",
        label = "Environment",
        options = environments,
        value = "production",
        disabled = true
    )""";}
!{String requiredValidationCode = """
    <form action="" method="get">
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
        @template.ashlar.components.button(text = "Submit", type = "Primary", buttonType = "submit")
    </form>""";}
!{List<ApiParam> selectApi = List.of(
    ApiParam.required("name", "String", "The field name values submit under."),
    ApiParam.required("label", "String", "Always rendered and always associated with the control - there is no labelless variant."),
    ApiParam.required("options", "List<SelectOption>", "Built via SelectOption.of(value, label), optionally .disabled(true)."),
    ApiParam.optional("value", "String", "\"\"", "The preselected option's value."),
    ApiParam.optional("placeholder", "String", "\"Select an option\"", "Shown only while nothing is selected."),
    ApiParam.optional("required", "boolean", "false", "Enables native constraint validation through the hidden <select>."),
    ApiParam.optional("disabled", "boolean", "false", "Disables the whole control."),
    ApiParam.optional("clearable", "boolean", "false", "Adds a clear (×) button that removes the selection entirely once one is made."),
    ApiParam.optional("requiredMessage", "String", "\"Please make a selection.\"", "Validation message, announced via role=\"alert\" when a required Select is submitted empty.")
);}
!{List<PageAnchor> sections = List.of(
    new PageAnchor("quick-start", "Quick Start"),
    new PageAnchor("how-to-use", "How to Use"),
    new PageAnchor("variants", "Variants & States"),
    new PageAnchor("composition", "Composition"),
    new PageAnchor("best-practices", "Best Practices"),
    new PageAnchor("pitfalls", "Pitfalls"),
    new PageAnchor("accessibility", "Accessibility"),
    new PageAnchor("api-reference", "API Reference"),
    new PageAnchor("related", "Related Components")
);}

@template.layout.page(
contextPath = contextPath,
title = "Select List",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "select", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A custom listbox dropdown backed by a real, hidden &lt;select&gt; - full keyboard support, type-ahead, and native form submission.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3 max-w-sm">
                    @template.ashlar.components.select(
                    name = "quickStartEnvironment",
                    label = "Environment",
                    options = quickStartOptions
                    )
                </div>
            `
            )
        </section>

        <section id="how-to-use" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "How to Use",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Reach for Select when a field has one fixed, mutually-exclusive list of options that submits a single value - a status, a region, a plan. For a long list a visitor needs to search rather than scan, use <a href="${contextPath}/components/combobox" class="text-blue-700 hover:underline dark:text-blue-400">Combobox</a> instead - same hidden-&lt;select&gt; mechanics, with type-to-filter added. For a trigger that performs actions or navigates rather than submitting a form value, use <a href="${contextPath}/components/dropdown-menu" class="text-blue-700 hover:underline dark:text-blue-400">Dropdown Menu</a> - it is not a form control at all.</p>
            `
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
                    @template.layout.example(
                    label = "Default & preselected value",
                    code = defaultPreselectedCode,
                    content = @`
                        <div class="grid gap-4 sm:grid-cols-2">
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
                    `
                    )

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Disabled options</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">US West and EU Central are at capacity - visible but not choosable, by keyboard or mouse.</p>
                        @template.layout.example(
                        code = disabledOptionsCode,
                        content = @`
                            <div class="mt-3 max-w-sm">
                                @template.ashlar.components.select(
                                name = "region",
                                label = "Region",
                                placeholder = "Select a region",
                                options = regions
                                )
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Clearable</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A preselected value with a clear (&times;) button to remove the selection entirely.</p>
                        @template.layout.example(
                        code = clearableCode,
                        content = @`
                            <div class="mt-3 max-w-sm">
                                @template.ashlar.components.select(
                                name = "clearableStatus",
                                label = "Status",
                                options = statuses,
                                value = "pending",
                                clearable = true
                                )
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Type-ahead &amp; keyboard interaction</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Focus the trigger and press <kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">Enter</kbd>/<kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">Space</kbd> or <kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">&darr;</kbd> to open, arrow keys to move, <kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">Home</kbd>/<kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">End</kbd> to jump, or just start typing - e.g. type <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">s</code> to jump straight to Staging.</p>
                        @template.layout.example(
                        code = keyboardCode,
                        content = @`
                            <div class="mt-3 max-w-sm">
                                @template.ashlar.components.select(
                                name = "keyboardEnvironment",
                                label = "Environment",
                                placeholder = "Select an environment",
                                options = environments
                                )
                            </div>
                        `
                        )
                    </div>

                    @template.layout.example(
                    label = "Disabled Select",
                    code = disabledSelectCode,
                    content = @`
                        <div class="max-w-sm">
                            @template.ashlar.components.select(
                            name = "disabledEnvironment",
                            label = "Environment",
                            options = environments,
                            value = "production",
                            disabled = true
                            )
                        </div>
                    `
                    )

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Required validation &amp; form submission</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Both fields are independent, required Select instances. Try submitting empty, then check the URL's query string for the submitted values.</p>
                        @template.layout.example(
                        code = requiredValidationCode,
                        content = @`
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
                        `
                        )
                    </div>
                </div>
            `
            )
        </section>

        <section id="composition" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Composition",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">The common pairing is one or more Selects inside a <a href="${contextPath}/components/filter-bar" class="text-blue-700 hover:underline dark:text-blue-400">Filter Bar</a>, alongside Search Input and Date Picker - each Select stays a normal form field, Filter Bar only arranges the row.</p>
            `
            )
        </section>

        <section id="best-practices" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Best Practices",
            content = @`
                <div class="grid gap-6 sm:grid-cols-2">
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
            `
            )
        </section>

        <section id="pitfalls" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Pitfalls & Troubleshooting",
            content = @`
                <ul class="list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">No built-in help text or error-message param.</strong> Unlike most other form components in this library, Select has neither <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">helpText</code> nor <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">errorMessage</code> - only <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">requiredMessage</code> for the browser's own native validation UI. A custom server-reported error needs its own surrounding markup; there is no parameter for it.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">Disabled options are still visible.</strong> If a choice genuinely should not be discoverable at all, remove it from <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">options</code> rather than marking it disabled.</li>
                </ul>
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
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.layout.api-reference(apiParams = selectApi)
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/combobox" class="text-blue-700 hover:underline dark:text-blue-400">Combobox</a> for a searchable, type-to-filter version &middot;
                    <a href="${contextPath}/components/dropdown-menu" class="text-blue-700 hover:underline dark:text-blue-400">Dropdown Menu</a> for actions and navigation instead of a submitted value &middot;
                    <a href="${contextPath}/components/radio-group" class="text-blue-700 hover:underline dark:text-blue-400">Radio Group</a> when every option should stay visible at once &middot;
                    <a href="${contextPath}/components/filter-bar" class="text-blue-700 hover:underline dark:text-blue-400">Filter Bar</a> for arranging Select alongside other filter controls
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "select", contextPath = contextPath)
    </div>
`
)
