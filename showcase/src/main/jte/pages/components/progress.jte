@import dev.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import dev.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

!{String quickStartCode = """
    @template.ashlar.components.progress(
        value = 65,
        label = "Migration progress"
    )""";}
!{String withoutPercentageCode = """
    @template.ashlar.components.progress(value = 55, label = "Syncing files", showPercentage = false)
    @template.ashlar.components.progress(value = 30, showPercentage = false)""";}
!{String sizesCode = """
    @template.ashlar.components.progress(value = 60, label = "Small", size = "Small")
    @template.ashlar.components.progress(value = 60, label = "Medium (default)", size = "Medium")
    @template.ashlar.components.progress(value = 60, label = "Large", size = "Large")""";}
!{String clampedCode = """
    @template.ashlar.components.progress(value = -20, label = "value = -20")
    @template.ashlar.components.progress(value = 140, label = "value = 140")""";}
!{List<ApiParam> progressApi = List.of(
    ApiParam.required("value", "int", "Clamped to 0-100 before it touches either the displayed percentage or the fill width - never rejected."),
    ApiParam.optional("label", "String", "\"\"", "Shown above the bar and doubles as its aria-label. Omitted entirely (no accessible name) when left empty."),
    ApiParam.optional("showPercentage", "boolean", "true", "Shows the clamped value as a percentage next to the label."),
    ApiParam.optional("size", "String", "\"Medium\"", "Small/Medium/Large. Unrecognized values silently fall back to Medium. There is no XLarge - see Related Components.")
);}
!{List<PageAnchor> sections = List.of(
    new PageAnchor("quick-start", "Quick Start"),
    new PageAnchor("variants", "Variants & States"),
    new PageAnchor("pitfalls", "Pitfalls"),
    new PageAnchor("accessibility", "Accessibility"),
    new PageAnchor("api-reference", "API Reference"),
    new PageAnchor("related", "Related Components")
);}

@template.layout.page(
contextPath = contextPath,
title = "Progress",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "progress", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A determinate, server-rendered progress indicator - no JavaScript, no animation. Distinct from Spinner: this is for measurable progress toward a known value, not indeterminate "work is happening."</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3 max-w-sm">
                    @template.ashlar.components.progress(value = 65, label = "Migration progress")
                </div>
            `
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Basic</p>
                        <div class="mt-3 flex max-w-sm flex-col gap-4">
                            @template.ashlar.components.progress(value = 0)
                            @template.ashlar.components.progress(value = 25)
                            @template.ashlar.components.progress(value = 50)
                            @template.ashlar.components.progress(value = 75)
                            @template.ashlar.components.progress(value = 100)
                        </div>
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">With a label</p>
                        <div class="mt-3 flex max-w-sm flex-col gap-4">
                            @template.ashlar.components.progress(value = 65, label = "Migration progress")
                            @template.ashlar.components.progress(value = 40, label = "Processing records")
                            @template.ashlar.components.progress(value = 90, label = "Upload progress")
                        </div>
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Without percentage display</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400"><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">showPercentage = false</code> - the label, if any, still shows.</p>
                        @template.layout.example(
                        code = withoutPercentageCode,
                        content = @`
                            <div class="mt-3 flex max-w-sm flex-col gap-4">
                                @template.ashlar.components.progress(value = 55, label = "Syncing files", showPercentage = false)
                                @template.ashlar.components.progress(value = 30, showPercentage = false)
                            </div>
                        `
                        )
                    </div>

                    @template.layout.example(
                    label = "Sizes",
                    code = sizesCode,
                    content = @`
                        <div class="flex max-w-sm flex-col gap-4">
                            @template.ashlar.components.progress(value = 60, label = "Small", size = "Small")
                            @template.ashlar.components.progress(value = 60, label = "Medium (default)", size = "Medium")
                            @template.ashlar.components.progress(value = 60, label = "Large", size = "Large")
                        </div>
                    `
                    )

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Out-of-range values are clamped, not rejected</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A negative value or one over 100 still renders a valid, safe bar rather than broken markup - the displayed percentage and the fill width always agree.</p>
                        @template.layout.example(
                        code = clampedCode,
                        content = @`
                            <div class="mt-3 flex max-w-sm flex-col gap-4">
                                @template.ashlar.components.progress(value = -20, label = "value = -20")
                                @template.ashlar.components.progress(value = 140, label = "value = 140")
                            </div>
                        `
                        )
                    </div>
                </div>
            `
            )
        </section>

        <section id="pitfalls" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Pitfalls & Troubleshooting",
            content = @`
                <ul class="list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">There is no built-in transition.</strong> If your application re-renders this with a new <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">value</code> (e.g. via an AJAX poll), the fill jumps to its new width instantly - there's nothing in v1 that animates between two values.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">Out-of-range <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">value</code> is silently clamped, not an error.</strong> Passing -20 or 140 never throws or renders broken markup - it's quietly corrected to 0 or 100, which can mask a real bug upstream if you were expecting validation.</li>
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
                            <li>The ARIA APG progressbar pattern on the track itself: <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="progressbar"</code> plus <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-valuemin</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-valuemax</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-valuenow</code>, always the clamped value.</li>
                            <li>The visible fill as a plain, unlabeled decorative child - only the track carries the ARIA role.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>A <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">label</code> if the bar needs an accessible name - without one, it has none at all, by design.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.layout.api-reference(apiParams = progressApi)
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/spinner" class="text-blue-700 hover:underline dark:text-blue-400">Spinner</a> for indeterminate "work is happening" instead of a known amount &middot;
                    <a href="${contextPath}/components/skeleton" class="text-blue-700 hover:underline dark:text-blue-400">Skeleton</a> for a content-shaped loading placeholder
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "progress", contextPath = contextPath)
    </div>
`
)
