@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param String contextPath

<%-- Batch 2A documentation page, same grammar as Batch 1. Existing example
     groups kept verbatim as Variants & States. No Composition section -
     nothing distinctive beyond ordinary form use.

     Verified against source before writing: number-input.jte's own @param
     list (14 params) and its doc comment - min/max/step are plain Strings,
     not parsed Java numbers, because the component has no domain logic
     that ever reads them back (it only writes them into the markup), and
     because step="0.5" disproves the assumption that every value is a
     whole number. Native spin buttons are kept deliberately rather than
     replaced, since the browser's own already respect min/max/step
     (including decimal steps) without floating-point drift. Zero
     JavaScript - same user-invalid:/aria-invalid: CSS-only error styling
     as Text Input. --%>
!{String quickStartCode = """
    @template.ashlar.components.number-input(
        name = "quantity",
        label = "Quantity",
        min = "0",
        step = "1"
    )""";}
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

@template.layout.page(
contextPath = contextPath,
title = "Number Input",
content = @`
    @template.layout.component-nav(currentSlug = "number-input", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Number Input</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A native &lt;input type="number"&gt; - min/max/step/required validation, the spin buttons, and keyboard behavior are all the browser's own. Zero JavaScript.</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3 max-w-sm">
                        @template.ashlar.components.number-input(name = "quickStartQuantity", label = "Quantity", min = "0", step = "1")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">How to Use</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Reach for Number Input when a value is genuinely a number a visitor types or spins - a quantity, an age, a rating. It has no formatting or masking of its own; for a value that happens to contain digits but isn't really numeric (a phone number, a ZIP code, a credit card number), use <a href="${contextPath}/components/text-input" class="text-blue-700 hover:underline dark:text-blue-400">Text Input</a> with <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">inputmode="numeric"</code> instead, the way its own Quick Start and "real-world examples" sections show.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
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

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Min/max &amp; decimal step</p>
                            <div class="mt-3 grid gap-4 sm:grid-cols-2">
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
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Disabled &amp; readonly</p>
                            <div class="mt-3 grid gap-4 sm:grid-cols-2">
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
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Server-reported error</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Forced via the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">error</code> param - the same way a controller would re-render a field after a failed server-side check.</p>
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
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Required validation, constraints &amp; form submission</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Try submitting empty, then a value below 1 or above 100, then a valid whole number - the browser's own validation UI takes over for each case. Submitting successfully reflects the value in the URL's query string; Reset restores the original value.</p>
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
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Best Practices</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
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
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Pitfalls &amp; Troubleshooting</h3>
                    <ul class="mt-2 list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300"><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">min</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">max</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">step</code> are Strings, not numbers.</strong> They're written straight into the HTML attribute with no Java-side parsing or validation - a typo like <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">min = "zero"</code> won't fail at render time, it'll just produce an attribute the browser ignores.</li>
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">No custom spinner styling.</strong> The increment/decrement buttons are whatever the browser renders natively - there's no param to restyle or hide them.</li>
                    </ul>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
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
                </div>

                <div>
                    @template.layout.api-reference(apiParams = numberInputApi)
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/text-input" class="text-blue-700 hover:underline dark:text-blue-400">Text Input</a> for numeric-looking identifiers that aren't really numbers &middot;
                        <a href="${contextPath}/components/date-picker" class="text-blue-700 hover:underline dark:text-blue-400">Date Picker</a> for values that are dates, not bare numbers
                    </p>
                </div>
            </div>
        </section>
`
)
