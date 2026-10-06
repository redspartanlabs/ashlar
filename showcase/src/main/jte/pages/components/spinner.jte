@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param String contextPath

<%-- Batch 3 documentation page. Existing "Sizes" / "Full-page loading
     state" / "A section waiting on data" / "Composed inside a control"
     examples are kept verbatim, moved under Variants & States. Added:
     Quick Start, formal grammar headings, Composition, Accessibility
     split, API Reference, Related Components, Pitfalls.

     Verified against source before writing:
     - spinner.jte's own 2 @params and its own doc comment in full.
       Confirmed the label mechanism precisely: a non-empty label (the
       default, "Loading") makes the whole element an announced
       role="status" region with the visual spinner itself hidden
       (aria-hidden) and a sr-only text alternative in its place;
       label = "" is for when the spinner sits inside something that
       already owns its own loading announcement.
     - SpinnerSize.java: 4 sizes (Small/Medium/Large/XLarge), falling back
       silently to Medium on an unrecognized string. Confirmed XLarge
       exists specifically for a full-page/full-panel loading state - the
       existing "Full-page loading state" example already demonstrates
       exactly that, verified as accurate.
     - Confirmed, by grepping every .jte file in the library, that
       Button's own `loading` state does NOT compose this component - it
       has its own separate, hand-rolled spinner shape
       (data-btn-spinner in button.jte), not a call to
       @template.ashlar.components.spinner. The two look similar but are
       independent implementations - a real fact worth stating plainly
       rather than letting a reader assume otherwise.
     - Confirmed, by the same grep, that Table IS a real internal
       consumer: table.jte's own loading row calls this component
       directly with label = "" (table.jte's own comment explains why:
       the surrounding "Loading records..." text is plain page content,
       not a live region, so the spinner doesn't need to announce
       anything a second time). --%>
!{String quickStartCode = """
    @template.ashlar.components.spinner()""";}
!{List<ApiParam> spinnerApi = List.of(
    ApiParam.optional("size", "String", "\"Medium\"", "Small/Medium/Large/XLarge. Unrecognized values silently fall back to Medium."),
    ApiParam.optional("label", "String", "\"Loading\"", "Non-empty makes this an announced role=\"status\" region with the visual spinner hidden from assistive tech. Pass \"\" when something else already owns the loading announcement.")
);}

@template.layout.page(
contextPath = contextPath,
title = "Spinner",
content = @`
    @template.layout.component-nav(currentSlug = "spinner", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Spinner</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A pure CSS loading indicator - no JavaScript, no timers. The application decides when it's loading; the spinner only shows it.</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3">
                        @template.ashlar.components.spinner()
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Sizes</p>
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
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Full-page loading state</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">XLarge is sized for a whole page or panel waiting on its initial data, not for inline or card-level use.</p>
                            <div class="mt-3 flex h-64 flex-col items-center justify-center gap-4 rounded-lg border border-slate-200 bg-slate-50 dark:border-slate-700 dark:bg-slate-800/50">
                                @template.ashlar.components.spinner(size = "XLarge")
                                <p class="text-sm text-slate-500 dark:text-slate-400">Loading dashboard...</p>
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">A section waiting on data</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The default label ("Loading") makes this a <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="status"</code> region announced to assistive tech; the spinner itself stays decorative.</p>
                            <div class="mt-3 flex max-w-sm flex-col items-center gap-3 rounded-lg border border-slate-200 bg-slate-50 px-6 py-10 text-center dark:border-slate-700 dark:bg-slate-800/50">
                                @template.ashlar.components.spinner(size = "Large")
                                <p class="text-sm text-slate-500 dark:text-slate-400">Loading employee records...</p>
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Composed inside a control</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Here the button's own visible text already says "Saving" - <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">label = ""</code> keeps the spinner purely decorative so assistive tech isn't told "Loading" twice.</p>
                            <button type="button" disabled class="mt-3 inline-flex h-10 cursor-not-allowed items-center gap-2 rounded-md border border-slate-300 bg-white px-4 text-sm font-medium text-slate-700 opacity-60 shadow-sm dark:border-slate-600 dark:bg-slate-800 dark:text-slate-200">
                                @template.ashlar.components.spinner(size = "Small", label = "")
                                Saving...
                            </button>
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composition</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Table's own loading-state row composes this component directly (with <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">label = ""</code>, since its own "Loading records..." text already carries the meaning as plain page content). Button's <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">loading</code> state does <em>not</em> - it has its own independent, hand-rolled spinner visual that looks similar but is a separate implementation, not a call to this component.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Pitfalls &amp; Troubleshooting</h3>
                    <ul class="mt-2 list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">Leaving the default label inside something that already announces loading double-announces it.</strong> If the spinner sits next to text like "Saving..." or inside a control that manages its own status (Button's <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">loading</code>), pass <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">label = ""</code> - otherwise assistive tech hears "Loading" twice.</li>
                    </ul>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
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
                </div>

                <div>
                    @template.layout.api-reference(apiParams = spinnerApi)
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/progress" class="text-blue-700 hover:underline dark:text-blue-400">Progress</a> for measurable progress toward a known value instead of indeterminate "work is happening" &middot;
                        <a href="${contextPath}/components/skeleton" class="text-blue-700 hover:underline dark:text-blue-400">Skeleton</a> for a content-shaped loading placeholder instead of a spinner &middot;
                        <a href="${contextPath}/components/table" class="text-blue-700 hover:underline dark:text-blue-400">Table</a>, which composes this directly for its own loading row
                    </p>
                </div>
            </div>
        </section>
`
)
