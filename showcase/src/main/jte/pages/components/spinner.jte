@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Spinner",
content = @`
    @template.layout.component-nav(currentSlug = "spinner", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Spinner</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A pure CSS loading indicator - no JavaScript, no timers. The application decides when it's loading; the spinner only shows it.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Sizes</h3>
                    <div class="mt-3 flex flex-wrap items-center gap-6">
                        <div class="flex flex-col items-center gap-2">
                            @template.ashlar.components.spinner(size = "Small")
                            <span class="text-xs text-slate-500 dark:text-slate-400">Small</span>
                        </div>
                        <div class="flex flex-col items-center gap-2">
                            @template.ashlar.components.spinner()
                            <span class="text-xs text-slate-500 dark:text-slate-400">Medium (default)</span>
                        </div>
                        <div class="flex flex-col items-center gap-2">
                            @template.ashlar.components.spinner(size = "Large")
                            <span class="text-xs text-slate-500 dark:text-slate-400">Large</span>
                        </div>
                        <div class="flex flex-col items-center gap-2">
                            @template.ashlar.components.spinner(size = "XLarge")
                            <span class="text-xs text-slate-500 dark:text-slate-400">XLarge</span>
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Full-page loading state</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">XLarge is sized for a whole page or panel waiting on its initial data, not for inline or card-level use.</p>
                    <div class="mt-3 flex h-64 flex-col items-center justify-center gap-4 rounded-lg border border-slate-200 bg-slate-50 dark:border-slate-700 dark:bg-slate-800/50">
                        @template.ashlar.components.spinner(size = "XLarge")
                        <p class="text-sm text-slate-500 dark:text-slate-400">Loading dashboard...</p>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">A section waiting on data</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The default label ("Loading") makes this a <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="status"</code> region announced to assistive tech; the spinner itself stays decorative.</p>
                    <div class="mt-3 flex max-w-sm flex-col items-center gap-3 rounded-lg border border-slate-200 bg-slate-50 px-6 py-10 text-center dark:border-slate-700 dark:bg-slate-800/50">
                        @template.ashlar.components.spinner(size = "Large")
                        <p class="text-sm text-slate-500 dark:text-slate-400">Loading employee records...</p>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composed inside a control</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Here the button's own visible text already says "Saving" - <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">label = ""</code> keeps the spinner purely decorative so assistive tech isn't told "Loading" twice.</p>
                    <button type="button" disabled class="mt-3 inline-flex h-10 cursor-not-allowed items-center gap-2 rounded-md border border-slate-300 bg-white px-4 text-sm font-medium text-slate-700 opacity-60 shadow-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-200">
                        @template.ashlar.components.spinner(size = "Small", label = "")
                        Saving...
                    </button>
                </div>
            </div>
        </section>
`
)
