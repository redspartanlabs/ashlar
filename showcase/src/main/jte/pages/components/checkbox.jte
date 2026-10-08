@import dev.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import dev.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

<%-- No Composition section - Checkbox has no distinctive composition
     pattern beyond ordinary form use, so it's left out rather than
     forced. --%>
!{String quickStartCode = """
    @template.ashlar.components.checkbox(
        name = "subscribe",
        label = "Subscribe to the newsletter"
    )""";}
!{String statesCode = """
    @template.ashlar.components.checkbox(
        name = "disabledUnchecked",
        label = "Disabled (unchecked)",
        disabled = true
    )
    @template.ashlar.components.checkbox(
        name = "disabledChecked",
        label = "Disabled (checked)",
        disabled = true,
        checked = true
    )
    @template.ashlar.components.checkbox(
        name = "selectAll",
        label = "Select all items",
        helpText = "Some, but not all, items on this page are currently selected.",
        indeterminate = true
    )
    @template.ashlar.components.checkbox(
        name = "longLabel",
        label = "I have read and agree to the extensive terms of service, privacy policy, and data processing agreement governing my use of this platform and all associated services",
        helpText = "Long labels and descriptions should wrap naturally without breaking the layout."
    )""";}
!{String groupCode = """
    @template.ashlar.components.checkbox(name = "languages", value = "java", label = "Java", checked = true)
    @template.ashlar.components.checkbox(name = "languages", value = "csharp", label = "C#")
    @template.ashlar.components.checkbox(name = "languages", value = "rust", label = "Rust", checked = true)
    @template.ashlar.components.checkbox(name = "languages", value = "typescript", label = "TypeScript")""";}
!{String errorCode = """
    @template.ashlar.components.checkbox(
        name = "flaggedTerms",
        label = "Accept terms and conditions",
        error = true,
        errorMessage = "You must accept the terms to continue."
    )""";}
!{String requiredValidationCode = """
    <form action="" method="get">
        @template.ashlar.components.checkbox(
            name = "acceptTerms",
            label = "I accept the terms and conditions",
            required = true
        )
        @template.ashlar.components.checkbox(
            name = "subscribeNewsletter",
            label = "Subscribe to the newsletter",
            checked = true
        )
        @template.ashlar.components.checkbox(
            name = "disabledOption",
            label = "Not available on your plan",
            disabled = true,
            checked = true
        )
        @template.ashlar.components.button(text = "Submit", type = "Primary", buttonType = "submit")
    </form>""";}
!{List<ApiParam> checkboxApi = List.of(
    ApiParam.required("name", "String", "The field name values submit under."),
    ApiParam.required("label", "String", "Always rendered and associated with the control via a real <label for>."),
    ApiParam.optional("value", "String", "\"on\"", "The value submitted when checked - the HTML default, rarely overridden."),
    ApiParam.optional("checked", "boolean", "false", "Initial checked state."),
    ApiParam.optional("disabled", "boolean", "false", "Disables the control. Not focusable or submitted."),
    ApiParam.optional("required", "boolean", "false", "Enables native constraint validation - the box must be checked to submit."),
    ApiParam.optional("indeterminate", "boolean", "false", "Visual third state (a dash, not a check). The one thing here that needs JavaScript - see Accessibility."),
    ApiParam.optional("id", "String", "\"\"", "Defaults to name when omitted."),
    ApiParam.optional("helpText", "String", "\"\"", "Supporting text below the label."),
    ApiParam.optional("error", "boolean", "false", "Shows errorMessage and applies error styling."),
    ApiParam.optional("errorMessage", "String", "\"\"", "Rendered with role=\"alert\" when error is true.")
);}
!{List<PageAnchor> sections = List.of(
    new PageAnchor("quick-start", "Quick Start"),
    new PageAnchor("how-to-use", "How to Use"),
    new PageAnchor("variants", "Variants & States"),
    new PageAnchor("best-practices", "Best Practices"),
    new PageAnchor("pitfalls", "Pitfalls"),
    new PageAnchor("accessibility", "Accessibility"),
    new PageAnchor("api-reference", "API Reference"),
    new PageAnchor("related", "Related Components")
);}

@template.layout.page(
contextPath = contextPath,
title = "Checkbox",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "checkbox", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A real &lt;input type="checkbox"&gt; tinted via <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">accent-color</code> - native focus, native Space-to-toggle, and required/error validation all still the browser's own. Indeterminate is the one exception that needs JavaScript, since it has no HTML attribute at all.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3">
                    @template.ashlar.components.checkbox(name = "quickStartSubscribe", label = "Subscribe to the newsletter")
                </div>
            `
            )
        </section>

        <section id="how-to-use" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "How to Use",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Reach for Checkbox when a visitor is agreeing to something or picking any number of independent options from a set (zero, one, or several can be checked at once). For a single setting that takes effect immediately rather than being submitted with a form, use <a href="${contextPath}/components/toggle" class="text-blue-700 hover:underline dark:text-blue-400">Toggle</a> instead - visually different, but the same real checkbox underneath. For choosing exactly one option from a visible set, use <a href="${contextPath}/components/radio-group" class="text-blue-700 hover:underline dark:text-blue-400">Radio Group</a>.</p>
            `
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Basic: unchecked, checked, with description</p>
                        <div class="mt-3">
                            @template.ashlar.components.checkbox(
                            name = "rememberMe",
                            label = "Remember me"
                            )
                            @template.ashlar.components.checkbox(
                            name = "marketingEmails",
                            label = "Send me marketing emails",
                            checked = true
                            )
                            @template.ashlar.components.checkbox(
                            name = "notifications",
                            label = "Enable notifications",
                            helpText = "Receive notifications when new data is available.",
                            checked = true
                            )
                        </div>
                    </div>

                    @template.layout.example(
                    label = "States: disabled, indeterminate, long label",
                    code = statesCode,
                    content = @`
                        <div class="flex flex-col gap-2">
                            @template.ashlar.components.checkbox(
                            name = "disabledUnchecked",
                            label = "Disabled (unchecked)",
                            disabled = true
                            )
                            @template.ashlar.components.checkbox(
                            name = "disabledChecked",
                            label = "Disabled (checked)",
                            disabled = true,
                            checked = true
                            )
                            @template.ashlar.components.checkbox(
                            name = "selectAll",
                            label = "Select all items",
                            helpText = "Some, but not all, items on this page are currently selected.",
                            indeterminate = true
                            )
                            @template.ashlar.components.checkbox(
                            name = "longLabel",
                            label = "I have read and agree to the extensive terms of service, privacy policy, and data processing agreement governing my use of this platform and all associated services",
                            helpText = "Long labels and descriptions should wrap naturally without breaking the layout."
                            )
                        </div>
                    `
                    )

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Checkbox group: multiple checkboxes sharing a name</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Each box is fully independent - toggling one never affects the others - but all four submit under the same <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">languages</code> name.</p>
                        @template.layout.example(
                        code = groupCode,
                        content = @`
                            <div class="mt-3">
                                @template.ashlar.components.checkbox(name = "languages", value = "java", label = "Java", checked = true)
                                @template.ashlar.components.checkbox(name = "languages", value = "csharp", label = "C#")
                                @template.ashlar.components.checkbox(name = "languages", value = "rust", label = "Rust", checked = true)
                                @template.ashlar.components.checkbox(name = "languages", value = "typescript", label = "TypeScript")
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Server-reported error</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Forced via the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">error</code> param - the same way a controller would re-render a field after a failed server-side check.</p>
                        @template.layout.example(
                        code = errorCode,
                        content = @`
                            <div class="mt-3">
                                @template.ashlar.components.checkbox(
                                name = "flaggedTerms",
                                label = "Accept terms and conditions",
                                error = true,
                                errorMessage = "You must accept the terms to continue."
                                )
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Required validation, keyboard/Tab order &amp; form submission</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Tab through the three boxes below (the disabled one is skipped entirely), toggle with <kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">Space</kbd>, then submit empty to see the browser's own validation UI on the required box. Submitting successfully reflects only the checked boxes' values in the URL's query string - the unchecked and disabled ones send nothing at all.</p>
                        @template.layout.example(
                        code = requiredValidationCode,
                        content = @`
                            <form class="mt-3 flex flex-col gap-4" action="" method="get">
                                <div>
                                    @template.ashlar.components.checkbox(
                                    name = "acceptTerms",
                                    label = "I accept the terms and conditions",
                                    required = true
                                    )
                                    @template.ashlar.components.checkbox(
                                    name = "subscribeNewsletter",
                                    label = "Subscribe to the newsletter",
                                    checked = true
                                    )
                                    @template.ashlar.components.checkbox(
                                    name = "disabledOption",
                                    label = "Not available on your plan",
                                    disabled = true,
                                    checked = true
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

        <section id="best-practices" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Best Practices",
            content = @`
                <div class="grid gap-6 sm:grid-cols-2">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Do</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Reserve <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">indeterminate</code> for a parent checkbox summarizing a group where some, but not all, children are checked.</li>
                            <li>Group related checkboxes under one visible heading or <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">helpText</code> when the relationship between them isn't obvious from the labels alone.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Avoid</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Using Checkbox for a single setting that should take effect immediately - that's Toggle's job.</li>
                            <li>Writing a label that assumes the checked state explains itself ("Yes") - state the actual choice being made.</li>
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
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">There is no <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">readonly</code> param.</strong> It has no effect on a checkbox per the HTML spec - only text-entry-like controls respect it - so it isn't exposed here. Use <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">disabled</code> instead.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">Indeterminate clears itself.</strong> The moment a user actually toggles the box, the browser resets <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">indeterminate</code> back to false on its own - expected native behavior, not something to work around.</li>
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
                            <li>Native focus, native Space-to-toggle, native required-field constraint validation.</li>
                            <li>Pure-CSS error styling (<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-invalid:</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">user-invalid:</code> variants) - no JavaScript <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">invalid</code> listener.</li>
                            <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">indeterminate</code> set once via the DOM property on load - the one case this library genuinely cannot server-render, since the HTML spec has no attribute for it.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>A <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">label</code> that states the actual choice, not just an affirmative word.</li>
                            <li>An <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">errorMessage</code> worth reading aloud - it renders with <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="alert"</code>.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.layout.api-reference(apiParams = checkboxApi)
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/toggle" class="text-blue-700 hover:underline dark:text-blue-400">Toggle</a> for a single immediate-effect setting &middot;
                    <a href="${contextPath}/components/radio-group" class="text-blue-700 hover:underline dark:text-blue-400">Radio Group</a> for exactly one choice from a visible set &middot;
                    <a href="${contextPath}/components/form-field" class="text-blue-700 hover:underline dark:text-blue-400">Form Field</a> for wiring help/error text onto a custom control
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "checkbox", contextPath = contextPath)
    </div>
`
)
