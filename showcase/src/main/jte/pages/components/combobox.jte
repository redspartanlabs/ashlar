@import dev.redspartanlabs.ashlar.select.SelectOption
@import dev.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import dev.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

!{String quickStartCode = """
    @template.ashlar.components.combobox(
        name = "assignee",
        label = "Assignee",
        options = List.of(
            SelectOption.of("jdoe", "Jane Doe"),
            SelectOption.of("bsmith", "Bob Smith")
        )
    )""";}
!{String basicPreselectedCode = """
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
    )""";}
!{String disabledCode = """
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
    )""";}
!{String errorCode = """
    @template.ashlar.components.combobox(
        name = "flaggedCountry",
        label = "Country",
        options = countries,
        error = true,
        errorMessage = "Shipping is not currently available to this country."
    )""";}
!{String requiredCode = """
    <form action="" method="get">
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
        @template.ashlar.components.button(text = "Submit", type = "Primary", buttonType = "submit")
    </form>""";}
!{List<ApiParam> comboboxApi = List.of(
    ApiParam.required("name", "String", "The field name the committed value submits under, via a real hidden <select>."),
    ApiParam.required("label", "String", "Associated with the input via a real <label for>."),
    ApiParam.required("options", "List<SelectOption>", "Each a value + label + optional disabled() flag - the same type Select uses."),
    ApiParam.optional("value", "String", "\"\"", "The initially-selected option's value. Must match an option's value() to take effect."),
    ApiParam.optional("placeholder", "String", "\"Search...\"", "Shown only while the input is empty and nothing is selected."),
    ApiParam.optional("required", "boolean", "false", "Enables native constraint validation on the hidden <select>."),
    ApiParam.optional("disabled", "boolean", "false", "Disables the input. Not focusable or submitted."),
    ApiParam.optional("helpText", "String", "\"\"", "Supporting text below the field."),
    ApiParam.optional("error", "boolean", "false", "Shows errorMessage and applies error styling."),
    ApiParam.optional("errorMessage", "String", "\"\"", "Rendered with role=\"alert\" when error is true. Defaults to \"Please make a selection.\" when error is true but this is left empty.")
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
title = "Combobox",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "combobox", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400"><a href="${contextPath}/components/select" class="text-blue-700 hover:underline dark:text-blue-400">Select</a>'s editable sibling - type to filter, backed by the same hidden-&lt;select&gt; trick for native form submission and required validation. Only an explicit selection is ever submitted, never raw search text.</p>

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

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3 max-w-sm">
                    @template.ashlar.components.combobox(name = "quickStartAssignee", label = "Assignee", options = assignees)
                </div>
            `
            )
        </section>

        <section id="how-to-use" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "How to Use",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Reach for Combobox when the option list is long enough that typing to filter genuinely helps - a country, a user, anything with more entries than someone wants to scroll through. For a short, fixed list where type-ahead (jump-to-match, not filter) is enough, use <a href="${contextPath}/components/select" class="text-blue-700 hover:underline dark:text-blue-400">Select</a> instead - visually similar but the trigger is read-only, not an editable text field. For filtering a page's content rather than choosing a form value, use <a href="${contextPath}/components/search-input" class="text-blue-700 hover:underline dark:text-blue-400">Search Input</a> - it has no listbox, no hidden &lt;select&gt;, and submits whatever text is typed, which Combobox deliberately never does.</p>
            `
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Basic &amp; preselected value</p>
                        @template.layout.example(
                        code = basicPreselectedCode,
                        content = @`
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
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Large option set, scrolling &amp; no matches</p>
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
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Disabled option &amp; disabled combobox</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Mei Chen is visible but unavailable - not selectable by mouse, keyboard, or filtering.</p>
                        @template.layout.example(
                        code = disabledCode,
                        content = @`
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
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Help text</p>
                        <div class="mt-3 max-w-sm">
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
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Server-reported error</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Forced via the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">error</code> param - the same way a controller would re-render a field after a failed server-side check.</p>
                        @template.layout.example(
                        code = errorCode,
                        content = @`
                            <div class="mt-3 max-w-sm">
                                @template.ashlar.components.combobox(
                                name = "flaggedCountry",
                                label = "Country",
                                options = countries,
                                error = true,
                                errorMessage = "Shipping is not currently available to this country."
                                )
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Required validation, keyboard &amp; form submission</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Two independent required comboboxes. Tab in, use <kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">&darr;</kbd>/<kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">&uarr;</kbd> and <kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">Enter</kbd> to select, or type to filter first. Submitting empty shows the browser's own validation UI; submitting successfully reflects the selected option <em>values</em> (not the typed text) in the URL's query string.</p>
                        @template.layout.example(
                        code = requiredCode,
                        content = @`
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
                <p class="text-sm text-slate-500 dark:text-slate-400">Combobox reuses Select's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">SelectOption</code> type directly - an option is exactly a value, a label, and an optional disabled flag in both components, so the same option list can feed either one. The real difference is entirely in interaction: Select's trigger is read-only and jumps to a match on type-ahead; Combobox's input is genuinely editable and visually filters the list as you type.</p>
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
                            <li>Use Combobox when the list is long enough that scrolling a plain Select would be tedious.</li>
                            <li>Keep <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">placeholder</code> descriptive of what can be typed ("Search countries...") rather than generic ("Select...").</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Avoid</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Treating it as a free-text field - whatever is typed but never committed is discarded the moment focus leaves.</li>
                            <li>Using Combobox for a short, fixed list of 5-6 options where Select's simpler read-only trigger is enough.</li>
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
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">There is no clear button.</strong> To clear an existing selection, delete all the typed text and then leave the field (blur, click away, or Tab out) - that's what actually clears the hidden value, not pressing Escape or Backspace alone while still focused.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">Typed text that is never committed is always discarded.</strong> Leaving the field with uncommitted text snaps it back to the last real selection (or back to empty, if there wasn't one) - there is no way to submit raw search text instead of a chosen option's value.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">Filtering is plain substring matching, not fuzzy search.</strong> Typing "untd" will not match "United States" - only a literal substring of the label does.</li>
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
                            <li>The full WAI-ARIA combobox-with-listbox-autocomplete wiring: <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="combobox"</code>, <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-expanded</code>, <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-controls</code>, and <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-activedescendant</code> tracking the highlighted (not necessarily committed) option.</li>
                            <li>An <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-live="polite"</code> status region announcing "&lt;label&gt; selected" on every commit.</li>
                            <li>Escape stops its own propagation after closing the list, so it never also closes an enclosing Modal/Drawer/Popover in the same keypress.</li>
                            <li>Native required-field constraint validation via the real hidden &lt;select&gt;.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Option labels distinct enough that substring filtering reliably narrows the list.</li>
                            <li>An <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">errorMessage</code> worth reading aloud - it renders with <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="alert"</code>.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.layout.api-reference(apiParams = comboboxApi)
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/select" class="text-blue-700 hover:underline dark:text-blue-400">Select</a> for a short, fixed list with a read-only trigger &middot;
                    <a href="${contextPath}/components/search-input" class="text-blue-700 hover:underline dark:text-blue-400">Search Input</a> for filtering page content rather than choosing a form value &middot;
                    <a href="${contextPath}/components/form-field" class="text-blue-700 hover:underline dark:text-blue-400">Form Field</a> for wiring help/error text onto a custom control
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "combobox", contextPath = contextPath)
    </div>
`
)
