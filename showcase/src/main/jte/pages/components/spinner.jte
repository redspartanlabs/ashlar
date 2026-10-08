@import dev.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import dev.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

<%-- Composition fact worth stating plainly, confirmed by grep against
     every .jte file in the library: Button's own loading state does NOT
     compose this component - it has its own separate, hand-rolled
     spinner shape, not a call to spinner.jte. The two look similar but
     are independent implementations. Table's loading row IS a real
     internal consumer, with label = "" since its own "Loading
     records..." text already carries the announcement. --%>
!{String quickStartCode = """
    @template.ashlar.components.spinner()""";}
!{String sizesCode = """
    @template.ashlar.components.spinner(size = "Small")
    @template.ashlar.components.spinner()
    @template.ashlar.components.spinner(size = "Large")
    @template.ashlar.components.spinner(size = "XLarge")""";}
!{String fullPageCode = """
    @template.ashlar.components.spinner(size = "XLarge")
    <p>Loading dashboard...</p>""";}
!{String sectionWaitingCode = """
    @template.ashlar.components.spinner(size = "Large")
    <p>Loading employee records...</p>""";}
!{String composedInControlCode = """
    @template.ashlar.components.spinner(size = "Small", label = "")""";}
!{List<ApiParam> spinnerApi = List.of(
    ApiParam.optional("size", "String", "\"Medium\"", "Small/Medium/Large/XLarge. Unrecognized values silently fall back to Medium."),
    ApiParam.optional("label", "String", "\"Loading\"", "Non-empty makes this an announced role=\"status\" region with the visual spinner hidden from assistive tech. Pass \"\" when something else already owns the loading announcement.")
);}
!{List<PageAnchor> sections = List.of(
    new PageAnchor("quick-start", "Quick Start"),
    new PageAnchor("variants", "Variants & States"),
    new PageAnchor("composition", "Composition"),
    new PageAnchor("pitfalls", "Pitfalls"),
    new PageAnchor("accessibility", "Accessibility"),
    new PageAnchor("api-reference", "API Reference"),
    new PageAnchor("related", "Related Components")
);}

@template.layout.page(
contextPath = contextPath,
title = "Spinner",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "spinner", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A pure CSS loading indicator - no JavaScript, no timers. The application decides when it's loading; the spinner only shows it.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3">
                    @template.ashlar.components.spinner()
                </div>
            `
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
                    @template.layout.example(
                    label = "Sizes",
                    code = sizesCode,
                    content = @`
                        <div class="flex flex-wrap items-center gap-6">
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
                    `
                    )

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Full-page loading state</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">XLarge is sized for a whole page or panel waiting on its initial data, not for inline or card-level use.</p>
                        @template.layout.example(
                        code = fullPageCode,
                        content = @`
                            <div class="mt-3 flex h-64 flex-col items-center justify-center gap-4 rounded-lg border border-slate-200 bg-slate-50 dark:border-slate-700 dark:bg-slate-800/50">
                                @template.ashlar.components.spinner(size = "XLarge")
                                <p class="text-sm text-slate-500 dark:text-slate-400">Loading dashboard...</p>
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">A section waiting on data</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The default label ("Loading") makes this a <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="status"</code> region announced to assistive tech; the spinner itself stays decorative.</p>
                        @template.layout.example(
                        code = sectionWaitingCode,
                        content = @`
                            <div class="mt-3 flex max-w-sm flex-col items-center gap-3 rounded-lg border border-slate-200 bg-slate-50 px-6 py-10 text-center dark:border-slate-700 dark:bg-slate-800/50">
                                @template.ashlar.components.spinner(size = "Large")
                                <p class="text-sm text-slate-500 dark:text-slate-400">Loading employee records...</p>
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Composed inside a control</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Here the button's own visible text already says "Saving" - <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">label = ""</code> keeps the spinner purely decorative so assistive tech isn't told "Loading" twice.</p>
                        @template.layout.example(
                        code = composedInControlCode,
                        content = @`
                            <button type="button" disabled class="mt-3 inline-flex h-10 cursor-not-allowed items-center gap-2 rounded-md border border-slate-300 bg-white px-4 text-sm font-medium text-slate-700 opacity-60 shadow-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-200">
                                @template.ashlar.components.spinner(size = "Small", label = "")
                                Saving...
                            </button>
                        `
                        )
                    </div>
                </div>
            `
            )
        </section>

        <section id="composition" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Composition",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Table's own loading-state row composes this component directly (with <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">label = ""</code>, since its own "Loading records..." text already carries the meaning as plain page content). Button's <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">loading</code> state does <em>not</em> - it has its own independent, hand-rolled spinner visual that looks similar but is a separate implementation, not a call to this component.</p>
            `
            )
        </section>

        <section id="pitfalls" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Pitfalls & Troubleshooting",
            content = @`
                <ul class="list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">Leaving the default label inside something that already announces loading double-announces it.</strong> If the spinner sits next to text like "Saving..." or inside a control that manages its own status (Button's <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">loading</code>), pass <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">label = ""</code> - otherwise assistive tech hears "Loading" twice.</li>
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
                            <li>A <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="status"</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-live="polite"</code> region whenever <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">label</code> is non-empty, with the spinning visual itself hidden from assistive tech.</li>
                            <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">motion-reduce:animate-none</code>, stopping the animation outright (not just skipping a transition) for anyone who asked for less motion.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>The decision to pass <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">label = ""</code> whenever this spinner isn't the only thing announcing the loading state.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.layout.api-reference(apiParams = spinnerApi)
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/progress" class="text-blue-700 hover:underline dark:text-blue-400">Progress</a> for measurable progress toward a known value instead of indeterminate "work is happening" &middot;
                    <a href="${contextPath}/components/skeleton" class="text-blue-700 hover:underline dark:text-blue-400">Skeleton</a> for a content-shaped loading placeholder instead of a spinner &middot;
                    <a href="${contextPath}/components/table" class="text-blue-700 hover:underline dark:text-blue-400">Table</a>, which composes this directly for its own loading row
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "spinner", contextPath = contextPath)
    </div>
`
)
