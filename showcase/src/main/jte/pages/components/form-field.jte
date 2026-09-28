@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Form Field",
content = @`
    @template.layout.component-nav(currentSlug = "form-field", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Form Field</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Label, help text, error text and required marker around a control this library doesn't already wrap - a native element, or something bespoke.</p>

            <div class="mt-4 rounded-md border border-amber-300 bg-amber-50 p-4 text-sm text-amber-900 dark:border-amber-700/60 dark:bg-amber-950/40 dark:text-amber-200">
                <p class="font-medium">Don't wrap the library's own form controls in this.</p>
                <p class="mt-1">Text Input, Textarea, Select, Checkbox, Radio Group, Toggle, Number Input, Combobox, Date Picker, Date Range Picker, File Upload and Search Input already render their own label, help text, error text and field spacing. Wrapping one here gives you two labels and two copies of the help text. Use their own <code class="rounded bg-amber-100 px-1 py-0.5 text-xs dark:bg-amber-900/50">label</code> / <code class="rounded bg-amber-100 px-1 py-0.5 text-xs dark:bg-amber-900/50">helpText</code> parameters instead.</p>
            </div>

            <div class="mt-4 rounded-md border border-slate-200 bg-slate-50 p-4 text-sm text-slate-700 dark:border-slate-700 dark:bg-slate-800/50 dark:text-slate-300">
                <p class="font-medium text-slate-900 dark:text-slate-100">The wiring contract</p>
                <p class="mt-1">Form Field names the control and renders the supporting text with predictable ids. It cannot reach into your control to add attributes, so you add these yourself:</p>
                <pre class="mt-2 overflow-x-auto rounded bg-slate-900 p-3 text-xs text-slate-100 dark:bg-slate-950"><code>id="{fieldId}"
aria-describedby="{fieldId}-help {fieldId}-error"
aria-invalid="true"   <%-- when an error is showing --%>
required              <%-- when the field is required --%></code></pre>
            </div>

            <div class="mt-6 flex flex-col gap-2">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic</h3>
                    <div class="mt-3">
                        @template.ashlar.components.form-field(
                        fieldId = "brand-colour",
                        label = "Brand colour",
                        control = @`<input type="color" id="brand-colour" value="#2563eb" class="h-9 w-16 cursor-pointer rounded-md border border-slate-300 bg-white p-1 dark:border-slate-600 dark:bg-slate-800">`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">With help text</h3>
                    <div class="mt-3">
                        @template.ashlar.components.form-field(
                        fieldId = "timezone",
                        label = "Reporting timezone",
                        helpText = "Scheduled exports use this timezone.",
                        control = @`
                            <select id="timezone" aria-describedby="timezone-help" class="block w-full max-w-sm rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100">
                                <option>UTC</option>
                                <option>Europe/London</option>
                                <option>America/Denver</option>
                            </select>
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Required</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The asterisk is decorative; the semantics come from <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">required</code> on the control itself.</p>
                    <div class="mt-3">
                        @template.ashlar.components.form-field(
                        fieldId = "invoice-ref",
                        label = "Invoice reference",
                        required = true,
                        helpText = "Printed at the top of every invoice.",
                        control = @`<input type="text" id="invoice-ref" required aria-describedby="invoice-ref-help" class="block w-full max-w-sm rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100">`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Validation error</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Help text stays visible alongside the error - it's usually what explains how to fix it - so the control lists both ids.</p>
                    <div class="mt-3">
                        @template.ashlar.components.form-field(
                        fieldId = "seat-count",
                        label = "Seats",
                        required = true,
                        helpText = "Between 1 and 500.",
                        errorMessage = "Enter a number between 1 and 500.",
                        control = @`<input type="number" id="seat-count" value="0" required aria-invalid="true" aria-describedby="seat-count-help seat-count-error" class="block w-full max-w-sm rounded-md border border-red-500 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm focus:border-red-500 focus:outline-none focus:ring-2 focus:ring-red-500 dark:border-red-500 dark:bg-slate-800 dark:text-slate-100">`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">A composed control</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">One field, several inputs. The label points at the first one.</p>
                    <div class="mt-3">
                        @template.ashlar.components.form-field(
                        fieldId = "card-expiry-month",
                        label = "Card expiry",
                        helpText = "Month and year as printed on the card.",
                        control = @`
                            <div class="flex max-w-sm items-center gap-2">
                                <input type="text" id="card-expiry-month" inputmode="numeric" placeholder="MM" maxlength="2" aria-label="Expiry month" aria-describedby="card-expiry-month-help" class="w-16 rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100">
                                <span class="text-slate-400" aria-hidden="true">/</span>
                                <input type="text" id="card-expiry-year" inputmode="numeric" placeholder="YY" maxlength="2" aria-label="Expiry year" aria-describedby="card-expiry-month-help" class="w-16 rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100">
                            </div>
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Long label and long messages</h3>
                    <div class="mt-3 max-w-sm">
                        @template.ashlar.components.form-field(
                        fieldId = "retention",
                        label = "Default retention period applied to newly created records",
                        required = true,
                        helpText = "Records older than this are moved to the archive automatically, and archived records stop counting toward your plan's storage limit once the trash is emptied.",
                        errorMessage = "Choose a retention period of at least 30 days - shorter periods conflict with the compliance policy on this account.",
                        control = @`<input type="text" id="retention" value="7 days" required aria-invalid="true" aria-describedby="retention-help retention-error" class="block w-full rounded-md border border-red-500 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm focus:border-red-500 focus:outline-none focus:ring-2 focus:ring-red-500 dark:border-red-500 dark:bg-slate-800 dark:text-slate-100">`
                        )
                    </div>
                </div>
            </div>
        </section>
`
)
