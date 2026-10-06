@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param String contextPath

<%-- Batch 2A documentation page, same grammar as Batch 1. Existing "Basic" /
     "States" / "Checkbox group" / "Server-reported error" / "Required
     validation" examples are kept verbatim as Variants & States. No
     Composition section - Checkbox doesn't have a distinctive composition
     pattern beyond ordinary form use, so it's left out rather than forced.

     Verified against source before writing: checkbox.jte's own @param list
     (11 params, including the readonly-is-not-a-param decision) and
     checkbox.js in full - indeterminate is set once on DOMContentLoaded via
     the DOM property (there's no HTML attribute for it), and the browser
     clears it back to false the instant a user actually toggles the box;
     the script tag itself is only emitted when indeterminate=true is used
     anywhere on the page, so a plain checkbox loads zero JavaScript. --%>
!{String quickStartCode = """
    @template.ashlar.components.checkbox(
        name = "subscribe",
        label = "Subscribe to the newsletter"
    )""";}
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

@template.layout.page(
contextPath = contextPath,
title = "Checkbox",
content = @`
    @template.layout.component-nav(currentSlug = "checkbox", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Checkbox</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A real &lt;input type="checkbox"&gt; tinted via <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">accent-color</code> - native focus, native Space-to-toggle, and required/error validation all still the browser's own. Indeterminate is the one exception that needs JavaScript, since it has no HTML attribute at all.</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3">
                        @template.ashlar.components.checkbox(name = "quickStartSubscribe", label = "Subscribe to the newsletter")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">How to Use</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Reach for Checkbox when a visitor is agreeing to something or picking any number of independent options from a set (zero, one, or several can be checked at once). For a single setting that takes effect immediately rather than being submitted with a form, use <a href="${contextPath}/components/toggle" class="text-blue-700 hover:underline dark:text-blue-400">Toggle</a> instead - visually different, but the same real checkbox underneath. For choosing exactly one option from a visible set, use <a href="${contextPath}/components/radio-group" class="text-blue-700 hover:underline dark:text-blue-400">Radio Group</a>.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
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

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">States: disabled, indeterminate, long label</p>
                            <div class="mt-3">
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
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Checkbox group: multiple checkboxes sharing a name</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Each box is fully independent - toggling one never affects the others - but all four submit under the same <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">languages</code> name.</p>
                            <div class="mt-3">
                                @template.ashlar.components.checkbox(name = "languages", value = "java", label = "Java", checked = true)
                                @template.ashlar.components.checkbox(name = "languages", value = "csharp", label = "C#")
                                @template.ashlar.components.checkbox(name = "languages", value = "rust", label = "Rust", checked = true)
                                @template.ashlar.components.checkbox(name = "languages", value = "typescript", label = "TypeScript")
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Server-reported error</p>
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
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Required validation, keyboard/Tab order &amp; form submission</p>
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
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Best Practices</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
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
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Pitfalls &amp; Troubleshooting</h3>
                    <ul class="mt-2 list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">There is no <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">readonly</code> param.</strong> It has no effect on a checkbox per the HTML spec - only text-entry-like controls respect it - so it isn't exposed here. Use <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">disabled</code> instead.</li>
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">Indeterminate clears itself.</strong> The moment a user actually toggles the box, the browser resets <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">indeterminate</code> back to false on its own - expected native behavior, not something to work around.</li>
                    </ul>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
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
                </div>

                <div>
                    @template.layout.api-reference(apiParams = checkboxApi)
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/toggle" class="text-blue-700 hover:underline dark:text-blue-400">Toggle</a> for a single immediate-effect setting &middot;
                        <a href="${contextPath}/components/radio-group" class="text-blue-700 hover:underline dark:text-blue-400">Radio Group</a> for exactly one choice from a visible set &middot;
                        <a href="${contextPath}/components/form-field" class="text-blue-700 hover:underline dark:text-blue-400">Form Field</a> for wiring help/error text onto a custom control
                    </p>
                </div>
            </div>
        </section>
`
)
