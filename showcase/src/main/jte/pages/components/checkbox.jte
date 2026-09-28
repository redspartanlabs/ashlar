@param String contextPath


@template.layout.page(
contextPath = contextPath,
title = "Checkbox",
content = @`
    @template.layout.component-nav(currentSlug = "checkbox", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Checkbox</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A real &lt;input type="checkbox"&gt; tinted via <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">accent-color</code> - native focus, native Space-to-toggle, and required/error validation all still the browser's own. Indeterminate is the one exception that needs JavaScript, since it has no HTML attribute at all.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic: unchecked, checked, with description</h3>
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

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">States: disabled, indeterminate, long label</h3>
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

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Checkbox group: multiple checkboxes sharing a name</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Each box is fully independent - toggling one never affects the others - but all four submit under the same <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">languages</code> name.</p>
                    <div class="mt-3">
                        @template.ashlar.components.checkbox(name = "languages", value = "java", label = "Java", checked = true)
                        @template.ashlar.components.checkbox(name = "languages", value = "csharp", label = "C#")
                        @template.ashlar.components.checkbox(name = "languages", value = "rust", label = "Rust", checked = true)
                        @template.ashlar.components.checkbox(name = "languages", value = "typescript", label = "TypeScript")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Server-reported error</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Forced via the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">error</code> param - the same way a controller would re-render a field after a failed server-side check.</p>
                    <div class="mt-3">
                        @template.ashlar.components.checkbox(
                        name = "flaggedTerms",
                        label = "Accept terms and conditions",
                        error = true,
                        errorMessage = "You must accept the terms to continue."
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Required validation, keyboard/Tab order &amp; form submission</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Tab through the three boxes below (the disabled one is skipped entirely), toggle with <kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">Space</kbd>, then submit empty to see the browser's own validation UI on the required box. Submitting successfully reflects only the checked boxes' values in the URL's query string - the unchecked and disabled ones send nothing at all.</p>
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
                </div>
            </div>
        </section>
`
)
