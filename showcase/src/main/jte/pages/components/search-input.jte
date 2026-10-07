@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import com.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

!{String quickStartCode = """
    @template.ashlar.components.search-input(
        name = "q",
        label = "Search accounts"
    )""";}
!{String prePopulatedCode = """
    @template.ashlar.components.search-input(
        name = "searchEmployees",
        label = "Search Employees",
        value = "Jane Doe"
    )""";}
!{String disabledHelpCode = """
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
    )""";}
!{String errorCode = """
    @template.ashlar.components.search-input(
        name = "flaggedSearch",
        label = "Search Customers",
        value = "'; DROP TABLE customers;",
        error = true,
        errorMessage = "Search terms can't contain special characters."
    )""";}
!{String requiredCode = """
    <form action="" method="get">
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
        @template.ashlar.components.button(text = "Search", type = "Primary", buttonType = "submit")
    </form>""";}
!{List<ApiParam> searchInputApi = List.of(
    ApiParam.required("name", "String", "The field name values submit under."),
    ApiParam.required("label", "String", "Always rendered and associated with the control via a real <label for>."),
    ApiParam.optional("value", "String", "\"\"", "Initial value. The clear button shows automatically whenever the field has content."),
    ApiParam.optional("placeholder", "String", "\"Search...\"", "Shown only while the field is empty."),
    ApiParam.optional("required", "boolean", "false", "Enables native constraint validation."),
    ApiParam.optional("disabled", "boolean", "false", "Disables the control, including the clear button."),
    ApiParam.optional("id", "String", "\"\"", "Defaults to name when omitted."),
    ApiParam.optional("helpText", "String", "\"\"", "Supporting text below the field."),
    ApiParam.optional("error", "boolean", "false", "Shows errorMessage and applies error styling."),
    ApiParam.optional("errorMessage", "String", "\"\"", "Rendered with role=\"alert\" when error is true.")
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
title = "Search Input",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "search-input", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A real &lt;input type="search"&gt; - a built-in clear button, no separate configuration for it. The only JavaScript keeps that button in sync with what's actually typed.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3 max-w-sm">
                    @template.ashlar.components.search-input(name = "quickStartSearch", label = "Search accounts")
                </div>
            `
            )
        </section>

        <section id="how-to-use" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "How to Use",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Reach for Search Input whenever a field's whole job is filtering or finding, not collecting data - it's the one text field in this library with a clear button built in, because a search box without an easy way to clear it isn't really a search box. For a form field that happens to use <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">type="search"</code> styling without that always-on clear behavior, plain <a href="${contextPath}/components/text-input" class="text-blue-700 hover:underline dark:text-blue-400">Text Input</a> already supports any native type. For filtering a long option list rather than free text, use <a href="${contextPath}/components/combobox" class="text-blue-700 hover:underline dark:text-blue-400">Combobox</a> instead.</p>
            `
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Basic &amp; placeholder</p>
                        <div class="mt-3 grid gap-4 sm:grid-cols-2">
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

                    @template.layout.example(
                    label = "Pre-populated (clear button visible)",
                    code = prePopulatedCode,
                    content = @`
                        <div class="max-w-sm">
                            @template.ashlar.components.search-input(
                            name = "searchEmployees",
                            label = "Search Employees",
                            value = "Jane Doe"
                            )
                        </div>
                    `
                    )

                    @template.layout.example(
                    label = "Disabled & help text",
                    code = disabledHelpCode,
                    content = @`
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
                    `
                    )

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Server-reported error</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Forced via the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">error</code> param - the same way a controller would re-render a field after a failed server-side check.</p>
                        @template.layout.example(
                        code = errorCode,
                        content = @`
                            <div class="mt-3 max-w-sm">
                                @template.ashlar.components.search-input(
                                name = "flaggedSearch",
                                label = "Search Customers",
                                value = "'; DROP TABLE customers;",
                                error = true,
                                errorMessage = "Search terms can't contain special characters."
                                )
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Multiple instances, required validation &amp; a real GET form</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Two independent, required Search Inputs. Type in one and clear it - the other is unaffected. Press <kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">Enter</kbd> in either field to submit the form normally; clicking a clear button never does. Submitting successfully reflects both values in the URL's query string.</p>
                        @template.layout.example(
                        code = requiredCode,
                        content = @`
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
                <p class="text-sm text-slate-500 dark:text-slate-400">The common pairing is one or more Search Inputs inside a <a href="${contextPath}/components/filter-bar" class="text-blue-700 hover:underline dark:text-blue-400">Filter Bar</a>, alongside Select and Date Picker - the exact composition Filter Bar's own page and Data Table's Common Patterns both demonstrate.</p>
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
                            <li>Rely on the built-in clear button rather than adding a second one of your own - it's always present, not configurable like Select's <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">clearable</code>.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Avoid</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Intercepting <kbd class="rounded border border-slate-300 bg-slate-100 px-1 text-xs dark:border-slate-600 dark:bg-slate-800">Enter</kbd> to block form submission - a search field that can't be submitted by keyboard breaks an expectation every visitor already has.</li>
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
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">The browser's own "x" is intentionally missing.</strong> WebKit/Blink draw a native clear affordance on <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">type="search"</code> fields once they have text; this component explicitly disables it so its own clear button is the only one shown. Not a bug if you've compared against a plain <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">&lt;input type="search"&gt;</code> and noticed the difference.</li>
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
                            <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">type="search"</code>'s correct native semantics - an implicit <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">searchbox</code> role, not a generic textbox.</li>
                            <li>A clear button with its own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-label="Clear search"</code>, kept in sync with the field's real content as you type.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>A <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">label</code> describing what's being searched, since "Search" alone rarely says enough on a page with more than one.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.layout.api-reference(apiParams = searchInputApi)
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/combobox" class="text-blue-700 hover:underline dark:text-blue-400">Combobox</a> for filtering a fixed option list &middot;
                    <a href="${contextPath}/components/filter-bar" class="text-blue-700 hover:underline dark:text-blue-400">Filter Bar</a> for arranging search alongside other filter controls &middot;
                    <a href="${contextPath}/components/text-input" class="text-blue-700 hover:underline dark:text-blue-400">Text Input</a> for free text without a built-in clear button
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "search-input", contextPath = contextPath)
    </div>
`
)
