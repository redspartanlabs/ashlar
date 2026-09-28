@import com.redspartanlabs.ashlar.radio.RadioOption
@import java.util.List

@param String contextPath

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

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic &amp; preselected value</h3>
                    <div class="grid gap-6 sm:grid-cols-2">
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Descriptions &amp; help text</h3>
                    <div class="max-w-md">
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Disabled group &amp; individually disabled option</h3>
                    <div class="grid gap-6 sm:grid-cols-2">
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Horizontal orientation</h3>
                    @template.ashlar.components.radio-group(
                    name = "horizontalPlan",
                    label = "Plan",
                    options = plans,
                    selectedValue = "free",
                    orientation = "horizontal"
                    )
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Server-reported error</h3>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Required validation, form submission &amp; reset</h3>
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
        </section>
`
)
