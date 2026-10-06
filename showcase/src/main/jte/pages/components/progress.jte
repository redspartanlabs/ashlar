@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param String contextPath

<%-- Batch 3 documentation page. Existing "Basic" / "With a label" /
     "Without percentage display" / "Sizes" / "Out-of-range values are
     clamped, not rejected" examples are kept verbatim, moved under
     Variants & States. Added: Quick Start, formal grammar headings,
     Accessibility split, API Reference, Related Components, Pitfalls.

     Verified against source before writing:
     - progress.jte's own 4 @params and its own doc comment in full.
       Confirmed value is clamped to 0-100 (Math.max(0, Math.min(100,
       value))) before it touches either the displayed percentage or the
       fill width - an out-of-range input is silently corrected, never
       rejected or thrown, and the two numbers can never disagree.
     - Confirmed there is no transition/animation on the fill's width -
       this is a fully static, server-rendered snapshot with nothing in
       v1 that changes it after render, not an oversight.
     - Confirmed the accessible name comes from aria-label on the track
       element itself (the one with role="progressbar"), set only when
       label is non-empty - an unlabeled bar has no accessible name at
       all, the same restraint Badge and Alert apply rather than
       inventing an aria-label the caller didn't ask for.
     - ProgressSize.java: only 3 sizes (Small/Medium/Large) - confirmed
       by its own doc comment there is deliberately no XLarge, unlike
       Spinner: a progress bar always sits inline with the content it
       describes, never as a whole-panel loading state. --%>
!{String quickStartCode = """
    @template.ashlar.components.progress(
        value = 65,
        label = "Migration progress"
    )""";}
!{List<ApiParam> progressApi = List.of(
    ApiParam.required("value", "int", "Clamped to 0-100 before it touches either the displayed percentage or the fill width - never rejected."),
    ApiParam.optional("label", "String", "\"\"", "Shown above the bar and doubles as its aria-label. Omitted entirely (no accessible name) when left empty."),
    ApiParam.optional("showPercentage", "boolean", "true", "Shows the clamped value as a percentage next to the label."),
    ApiParam.optional("size", "String", "\"Medium\"", "Small/Medium/Large. Unrecognized values silently fall back to Medium. There is no XLarge - see Related Components.")
);}

@template.layout.page(
contextPath = contextPath,
title = "Progress",
content = @`
    @template.layout.component-nav(currentSlug = "progress", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Progress</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A determinate, server-rendered progress indicator - no JavaScript, no animation. Distinct from Spinner: this is for measurable progress toward a known value, not indeterminate "work is happening."</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3 max-w-sm">
                        @template.ashlar.components.progress(value = 65, label = "Migration progress")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
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
                            <div class="mt-3 flex max-w-sm flex-col gap-4">
                                @template.ashlar.components.progress(value = 55, label = "Syncing files", showPercentage = false)
                                @template.ashlar.components.progress(value = 30, showPercentage = false)
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Sizes</p>
                            <div class="mt-3 flex max-w-sm flex-col gap-4">
                                @template.ashlar.components.progress(value = 60, label = "Small", size = "Small")
                                @template.ashlar.components.progress(value = 60, label = "Medium (default)", size = "Medium")
                                @template.ashlar.components.progress(value = 60, label = "Large", size = "Large")
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Out-of-range values are clamped, not rejected</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A negative value or one over 100 still renders a valid, safe bar rather than broken markup - the displayed percentage and the fill width always agree.</p>
                            <div class="mt-3 flex max-w-sm flex-col gap-4">
                                @template.ashlar.components.progress(value = -20, label = "value = -20")
                                @template.ashlar.components.progress(value = 140, label = "value = 140")
                            </div>
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Pitfalls &amp; Troubleshooting</h3>
                    <ul class="mt-2 list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">There is no built-in transition.</strong> If your application re-renders this with a new <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">value</code> (e.g. via an AJAX poll), the fill jumps to its new width instantly - there's nothing in v1 that animates between two values.</li>
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">Out-of-range <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">value</code> is silently clamped, not an error.</strong> Passing -20 or 140 never throws or renders broken markup - it's quietly corrected to 0 or 100, which can mask a real bug upstream if you were expecting validation.</li>
                    </ul>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
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
                </div>

                <div>
                    @template.layout.api-reference(apiParams = progressApi)
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/spinner" class="text-blue-700 hover:underline dark:text-blue-400">Spinner</a> for indeterminate "work is happening" instead of a known amount &middot;
                        <a href="${contextPath}/components/skeleton" class="text-blue-700 hover:underline dark:text-blue-400">Skeleton</a> for a content-shaped loading placeholder
                    </p>
                </div>
            </div>
        </section>
`
)
