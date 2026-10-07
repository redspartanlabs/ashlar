@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import com.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

<%-- No Composition section - nothing distinctive beyond ordinary form
     use. --%>
!{String quickStartCode = """
    @template.ashlar.components.number-input(
        name = "quantity",
        label = "Quantity",
        min = "0",
        step = "1"
    )""";}
!{String minMaxCode = """
    @template.ashlar.components.number-input(
        name = "ageNumberInput",
        label = "Age",
        value = "25",
        min = "0",
        max = "120",
        step = "1",
        helpText = "Must be between 0 and 120."
    )
    @template.ashlar.components.number-input(
        name = "rating",
        label = "Rating",
        value = "4.5",
        min = "0",
        max = "5",
        step = "0.5",
        helpText = "Half-point increments, 0 to 5."
    )""";}
!{String disabledReadonlyCode = """
    @template.ashlar.components.number-input(
        name = "disabledSeats",
        label = "Seats (disabled)",
        value = "5",
        disabled = true
    )
    @template.ashlar.components.number-input(
        name = "readonlyAccountId",
        label = "Account ID (readonly)",
        value = "1009",
        readonly = true,
        helpText = "Assigned automatically and can't be changed."
    )""";}
!{String errorCode = """
    @template.ashlar.components.number-input(
        name = "flaggedDiscount",
        label = "Discount Percentage",
        value = "150",
        min = "0",
        max = "100",
        error = true,
        errorMessage = "Discount percentage must be between 0 and 100."
    )""";}
!{String requiredCode = """
    <form action="" method="get">
        @template.ashlar.components.number-input(
            name = "requiredQuantity",
            label = "Quantity",
            value = "1",
            min = "1",
            max = "100",
            step = "1",
            required = true
        )
        @template.ashlar.components.button(text = "Submit", type = "Primary", buttonType = "submit")
        @template.ashlar.components.button(text = "Reset", type = "Ghost", buttonType = "reset")
    </form>""";}
!{List<ApiParam> numberInputApi = List.of(
    ApiParam.required("name", "String", "The field name values submit under."),
    ApiParam.required("label", "String", "Always rendered and associated with the control via a real <label for>."),
    ApiParam.optional("value", "String", "\"\"", "Initial value."),
    ApiParam.optional("placeholder", "String", "\"\"", "Shown only while the field is empty."),
    ApiParam.optional("min", "String", "\"\"", "Native min constraint - a plain string, written straight into the markup."),
    ApiParam.optional("max", "String", "\"\"", "Native max constraint, same handling as min."),
    ApiParam.optional("step", "String", "\"\"", "Native step constraint - accepts decimals (\"0.5\", \"0.01\") exactly like whole numbers."),
    ApiParam.optional("required", "boolean", "false", "Enables native constraint validation."),
    ApiParam.optional("disabled", "boolean", "false", "Disables the control. Not focusable or submitted."),
    ApiParam.optional("readonly", "boolean", "false", "Visible and focusable, but not editable or spinnable. Still submitted."),
    ApiParam.optional("id", "String", "\"\"", "Defaults to name when omitted."),
    ApiParam.optional("helpText", "String", "\"\"", "Supporting text below the field."),
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
title = "Number Input",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "number-input", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A native &lt;input type="number"&gt; - min/max/step/required validation, the spin buttons, and keyboard behavior are all the browser's own. Zero JavaScript.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3 max-w-sm">
                    @template.ashlar.components.number-input(name = "quickStartQuantity", label = "Quantity", min = "0", step = "1")
                </div>
            `
            )
        </section>

        <section id="how-to-use" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "How to Use",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Reach for Number Input when a value is genuinely a number a visitor types or spins - a quantity, an age, a rating. It has no formatting or masking of its own; for a value that happens to contain digits but isn't really numeric (a phone number, a ZIP code, a credit card number), use <a href="${contextPath}/components/text-input" class="text-blue-700 hover:underline dark:text-blue-400">Text Input</a> with <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">inputmode="numeric"</code> instead, the way its own Quick Start and "real-world examples" sections show.</p>
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
                            @template.ashlar.components.number-input(
                            name = "quantity",
                            label = "Quantity",
                            value = "10"
                            )
                            @template.ashlar.components.number-input(
                            name = "amount",
                            label = "Amount",
                            placeholder = "0.00",
                            step = "0.01",
                            min = "0"
                            )
                        </div>
                    </div>

                    @template.layout.example(
                    label = "Min/max & decimal step",
                    code = minMaxCode,
                    content = @`
                        <div class="grid gap-4 sm:grid-cols-2">
                            @template.ashlar.components.number-input(
                            name = "ageNumberInput",
                            label = "Age",
                            value = "25",
                            min = "0",
                            max = "120",
                            step = "1",
                            helpText = "Must be between 0 and 120."
                            )
                            @template.ashlar.components.number-input(
                            name = "rating",
                            label = "Rating",
                            value = "4.5",
                            min = "0",
                            max = "5",
                            step = "0.5",
                            helpText = "Half-point increments, 0 to 5."
                            )
                        </div>
                    `
                    )

                    @template.layout.example(
                    label = "Disabled & readonly",
                    code = disabledReadonlyCode,
                    content = @`
                        <div class="grid gap-4 sm:grid-cols-2">
                            @template.ashlar.components.number-input(
                            name = "disabledSeats",
                            label = "Seats (disabled)",
                            value = "5",
                            disabled = true
                            )
                            @template.ashlar.components.number-input(
                            name = "readonlyAccountId",
                            label = "Account ID (readonly)",
                            value = "1009",
                            readonly = true,
                            helpText = "Assigned automatically and can't be changed."
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
                                @template.ashlar.components.number-input(
                                name = "flaggedDiscount",
                                label = "Discount Percentage",
                                value = "150",
                                min = "0",
                                max = "100",
                                error = true,
                                errorMessage = "Discount percentage must be between 0 and 100."
                                )
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Required validation, constraints &amp; form submission</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Try submitting empty, then a value below 1 or above 100, then a valid whole number - the browser's own validation UI takes over for each case. Submitting successfully reflects the value in the URL's query string; Reset restores the original value.</p>
                        @template.layout.example(
                        code = requiredCode,
                        content = @`
                            <form class="mt-3 flex flex-col gap-4" action="" method="get">
                                <div class="max-w-sm">
                                    @template.ashlar.components.number-input(
                                    name = "requiredQuantity",
                                    label = "Quantity",
                                    value = "1",
                                    min = "1",
                                    max = "100",
                                    step = "1",
                                    required = true
                                    )
                                </div>
                                <div class="flex gap-3">
                                    @template.ashlar.components.button(text = "Submit", type = "Primary", buttonType = "submit")
                                    @template.ashlar.components.button(text = "Reset", type = "Ghost", buttonType = "reset")
                                </div>
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
                            <li>Set <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">min</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">max</code> whenever a real range exists - that's what drives both the browser's validation and the spin buttons' limits.</li>
                            <li>Use a decimal <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">step</code> ("0.01", "0.5") for currency or fractional values - the native spinner handles it without rounding drift.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Avoid</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Reaching for Number Input to collect an identifier that happens to look numeric (phone, ZIP, account number) - see How to Use.</li>
                            <li>Assuming <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">min</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">max</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">step</code> are validated server-side by this component - they're native HTML attributes the browser enforces, nothing more.</li>
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
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300"><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">min</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">max</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">step</code> are Strings, not numbers.</strong> They're written straight into the HTML attribute with no Java-side parsing or validation - a typo like <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">min = "zero"</code> won't fail at render time, it'll just produce an attribute the browser ignores.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">No custom spinner styling.</strong> The increment/decrement buttons are whatever the browser renders natively - there's no param to restyle or hide them.</li>
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
                            <li>Native spin buttons that are keyboard- and screen-reader-accessible for free, and correctly respect <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">min</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">max</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">step</code> without drift.</li>
                            <li>Pure-CSS error styling, same <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">user-invalid:</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-invalid:</code> approach as Text Input - no JavaScript.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>A <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">label</code>, and real <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">min</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">max</code> values - without them there's no constraint for the browser to enforce or announce.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.layout.api-reference(apiParams = numberInputApi)
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/text-input" class="text-blue-700 hover:underline dark:text-blue-400">Text Input</a> for numeric-looking identifiers that aren't really numbers &middot;
                    <a href="${contextPath}/components/date-picker" class="text-blue-700 hover:underline dark:text-blue-400">Date Picker</a> for values that are dates, not bare numbers
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "number-input", contextPath = contextPath)
    </div>
`
)
