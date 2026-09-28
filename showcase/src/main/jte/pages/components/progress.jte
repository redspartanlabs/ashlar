@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Progress",
content = @`
    @template.layout.component-nav(currentSlug = "progress", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Progress</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A determinate, server-rendered progress indicator - no JavaScript, no animation. Distinct from Spinner: this is for measurable progress toward a known value, not indeterminate "work is happening."</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic</h3>
                    <div class="mt-3 flex max-w-sm flex-col gap-4">
                        @template.ashlar.components.progress(value = 0)
                        @template.ashlar.components.progress(value = 25)
                        @template.ashlar.components.progress(value = 50)
                        @template.ashlar.components.progress(value = 75)
                        @template.ashlar.components.progress(value = 100)
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">With a label</h3>
                    <div class="mt-3 flex max-w-sm flex-col gap-4">
                        @template.ashlar.components.progress(value = 65, label = "Migration progress")
                        @template.ashlar.components.progress(value = 40, label = "Processing records")
                        @template.ashlar.components.progress(value = 90, label = "Upload progress")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Without percentage display</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400"><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">showPercentage = false</code> - the label, if any, still shows.</p>
                    <div class="mt-3 flex max-w-sm flex-col gap-4">
                        @template.ashlar.components.progress(value = 55, label = "Syncing files", showPercentage = false)
                        @template.ashlar.components.progress(value = 30, showPercentage = false)
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Sizes</h3>
                    <div class="mt-3 flex max-w-sm flex-col gap-4">
                        @template.ashlar.components.progress(value = 60, label = "Small", size = "Small")
                        @template.ashlar.components.progress(value = 60, label = "Medium (default)", size = "Medium")
                        @template.ashlar.components.progress(value = 60, label = "Large", size = "Large")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Out-of-range values are clamped, not rejected</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A negative value or one over 100 still renders a valid, safe bar rather than broken markup - the displayed percentage and the fill width always agree.</p>
                    <div class="mt-3 flex max-w-sm flex-col gap-4">
                        @template.ashlar.components.progress(value = -20, label = "value = -20")
                        @template.ashlar.components.progress(value = 140, label = "value = 140")
                    </div>
                </div>
            </div>
        </section>
`
)
