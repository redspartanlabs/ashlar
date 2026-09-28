@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Number Input",
content = @`
    @template.layout.component-nav(currentSlug = "number-input", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Number Input</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A native &lt;input type="number"&gt; - min/max/step/required validation, the spin buttons, and keyboard behavior are all the browser's own. Zero JavaScript.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic &amp; placeholder</h3>
                    <div class="grid gap-4 sm:grid-cols-2">
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Min/max &amp; decimal step</h3>
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
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Disabled &amp; readonly</h3>
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
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Server-reported error</h3>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Required validation, constraints &amp; form submission</h3>
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
        </section>
`
)
