@import com.redspartanlabs.ashlar.radio.RadioOption
@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param String contextPath

<%-- Batch 2A documentation page, same grammar as Batch 1. Existing example
     groups kept verbatim as Variants & States. No Composition section -
     nothing distinctive beyond ordinary form use.

     Verified against source before writing: radio-group.jte's own @param
     list (11 params) and RadioOption.java, plus the component's own doc
     comment - real <fieldset>/<legend>: `disabled` on the fieldset
     natively disables every descendant radio at once (no per-option loop
     needed), <legend> gives the group its accessible name for free (no
     aria-label needed), and per the HTML spec a same-name radio group's
     `required` constraint is satisfied the instant any member is checked,
     even though `required` is written on every individual input. Arrow
     keys move *and* select within a native radio group - not a separate
     "move" vs "activate" step the way a custom listbox needs. --%>
!{List<RadioOption> quickStartOptions = List.of(
    RadioOption.of("free", "Free"),
    RadioOption.of("pro", "Pro"),
    RadioOption.of("enterprise", "Enterprise")
);}
!{String quickStartCode = """
    !{List<RadioOption> options = List.of(
        RadioOption.of("free", "Free"),
        RadioOption.of("pro", "Pro"),
        RadioOption.of("enterprise", "Enterprise")
    );}

    @template.ashlar.components.radio-group(
        name = "plan",
        label = "Plan",
        options = options
    )""";}
!{List<ApiParam> radioGroupApi = List.of(
    ApiParam.required("name", "String", "The shared field name every radio in the group submits under."),
    ApiParam.required("label", "String", "Rendered as the group's <legend> - its accessible name, with no separate aria-label needed."),
    ApiParam.required("options", "List<RadioOption>", "Built via RadioOption.of(value, label), optionally .description(...) and/or .disabled(true)."),
    ApiParam.optional("selectedValue", "String", "\"\"", "The preselected option's value."),
    ApiParam.optional("required", "boolean", "false", "Enables native constraint validation - satisfied the instant any option is checked."),
    ApiParam.optional("disabled", "boolean", "false", "Disables every option at once via the native <fieldset disabled>."),
    ApiParam.optional("orientation", "String", "\"vertical\"", "\"vertical\" (stacked) or \"horizontal\" (wrapped row)."),
    ApiParam.optional("id", "String", "\"\"", "Defaults to name when omitted."),
    ApiParam.optional("helpText", "String", "\"\"", "Supporting text below the options."),
    ApiParam.optional("error", "boolean", "false", "Shows errorMessage and applies error styling."),
    ApiParam.optional("errorMessage", "String", "\"\"", "Rendered with role=\"alert\" when error is true.")
);}

@template.layout.page(
contextPath = contextPath,
title = "Radio Group",
content = @`
    @template.layout.component-nav(currentSlug = "radio-group", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Radio Group</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A &lt;fieldset&gt;/&lt;legend&gt; wrapping native &lt;input type="radio"&gt; options - selection, deselection, keyboard, and required validation are all the browser's own. Zero JavaScript.</p>

            !{List<RadioOption> radioEnvironments = List.of(
                RadioOption.of("development", "Development").description("For local development and testing."),
                RadioOption.of("staging", "Staging").description("For integration and pre-production validation."),
                RadioOption.of("production", "Production").description("For live production workloads.")
            );}
            !{List<RadioOption> plans = List.of(
                RadioOption.of("free", "Free"),
                RadioOption.of("pro", "Pro"),
                RadioOption.of("enterprise", "Enterprise")
            );}
            !{List<RadioOption> regionsWithUnavailable = List.of(
                RadioOption.of("us-east", "US East"),
                RadioOption.of("us-west", "US West"),
                RadioOption.of("eu-central", "EU Central").disabled(true)
            );}

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3 max-w-sm">
                        @template.ashlar.components.radio-group(name = "quickStartPlan", label = "Plan", options = quickStartOptions)
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">How to Use</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Reach for Radio Group when every option should stay visible and comparable at once, and exactly one must be chosen - a plan, a deployment target, a shipping method. Once the list is long enough that scanning stops being easy, switch to <a href="${contextPath}/components/select" class="text-blue-700 hover:underline dark:text-blue-400">Select</a> instead - the same "exactly one" contract, collapsed behind a trigger. For a single on/off setting, neither applies - use <a href="${contextPath}/components/toggle" class="text-blue-700 hover:underline dark:text-blue-400">Toggle</a> or <a href="${contextPath}/components/checkbox" class="text-blue-700 hover:underline dark:text-blue-400">Checkbox</a>.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Basic &amp; preselected value</p>
                            <div class="mt-3 grid gap-6 sm:grid-cols-2">
                                @template.ashlar.components.radio-group(
                                name = "plan",
                                label = "Plan",
                                options = plans
                                )
                                @template.ashlar.components.radio-group(
                                name = "preselectedPlan",
                                label = "Plan",
                                options = plans,
                                selectedValue = "pro"
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Descriptions &amp; help text</p>
                            <div class="mt-3 max-w-md">
                                @template.ashlar.components.radio-group(
                                name = "deploymentTarget",
                                label = "Deployment Target",
                                options = radioEnvironments,
                                selectedValue = "staging",
                                helpText = "Choose the environment where the deployment should occur."
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Disabled group &amp; individually disabled option</p>
                            <div class="mt-3 grid gap-6 sm:grid-cols-2">
                                @template.ashlar.components.radio-group(
                                name = "disabledPlan",
                                label = "Plan (disabled group)",
                                options = plans,
                                selectedValue = "free",
                                disabled = true
                                )
                                @template.ashlar.components.radio-group(
                                name = "region",
                                label = "Region",
                                options = regionsWithUnavailable,
                                helpText = "EU Central is temporarily unavailable."
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Horizontal orientation</p>
                            <div class="mt-3">
                                @template.ashlar.components.radio-group(
                                name = "horizontalPlan",
                                label = "Plan",
                                options = plans,
                                selectedValue = "free",
                                orientation = "horizontal"
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Server-reported error</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Forced via the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">error</code> param - the same way a controller would re-render a field after a failed server-side check.</p>
                            <div class="mt-3">
                                @template.ashlar.components.radio-group(
                                name = "flaggedPlan",
                                label = "Plan",
                                options = plans,
                                error = true,
                                errorMessage = "Please select a plan to continue."
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Required validation, form submission &amp; reset</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Two independent required groups, plus an optional one. Tab through the options (arrow keys move <em>and</em> select within a group, the disabled option is skipped entirely), try submitting empty, then fill both in and submit to see the values reflected in the URL's query string. Reset restores each group to its original selection.</p>
                            <form class="mt-3 flex flex-col gap-4" action="" method="get">
                                <div class="grid gap-6 sm:grid-cols-2">
                                    @template.ashlar.components.radio-group(
                                    name = "requiredEnvironment",
                                    label = "Environment",
                                    options = radioEnvironments,
                                    required = true
                                    )
                                    @template.ashlar.components.radio-group(
                                    name = "requiredPlan",
                                    label = "Plan",
                                    options = plans,
                                    selectedValue = "pro",
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
                                <li>Use <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">.description(...)</code> when an option's label alone doesn't say enough to choose confidently.</li>
                                <li>Keep the option count small - if a visitor would rather search than scan, that's Select's job (see How to Use).</li>
                            </ul>
                        </div>
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Avoid</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>Disabling the whole group to represent one unavailable option - mark that single <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">RadioOption</code> <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">.disabled(true)</code> instead, so the rest stay choosable.</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Pitfalls &amp; Troubleshooting</h3>
                    <ul class="mt-2 list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300"><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">required</code> looks per-option but isn't.</strong> It's written on every individual radio, but per the HTML spec a same-name group's required constraint is satisfied the instant <em>any</em> member is checked - the group reads as "required," not each option.</li>
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">Arrow keys both move and select.</strong> Unlike a custom listbox (Select, Combobox), a native radio group has no separate "highlight, then confirm" step - pressing an arrow key immediately changes the checked option.</li>
                    </ul>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Ashlar provides</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>A real <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">&lt;fieldset&gt;</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">&lt;legend&gt;</code> - the group's accessible name comes from the legend natively, and <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">disabled</code> on the fieldset disables every option at once with no per-option loop.</li>
                                <li>Native selection, deselection, arrow-key movement, and required validation - all browser behavior, nothing reimplemented.</li>
                            </ul>
                        </div>
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>A <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">label</code> that names the choice being made, since it becomes the group's one legend.</li>
                                <li>Per-option <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">.description(...)</code> where a label alone would leave the choice ambiguous.</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div>
                    @template.layout.api-reference(apiParams = radioGroupApi)
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/select" class="text-blue-700 hover:underline dark:text-blue-400">Select</a> for the same one-choice contract behind a trigger &middot;
                        <a href="${contextPath}/components/toggle" class="text-blue-700 hover:underline dark:text-blue-400">Toggle</a> and <a href="${contextPath}/components/checkbox" class="text-blue-700 hover:underline dark:text-blue-400">Checkbox</a> for binary choices
                    </p>
                </div>
            </div>
        </section>
`
)
