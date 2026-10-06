@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param String contextPath

<%-- Batch 12 (final) documentation page. Existing "Title only" / "Title +
     description" / "Title + actions" / "Title + description + actions" /
     "Multiple actions" / "Long title" / "Long description" / "Narrow /
     mobile layout" examples kept verbatim. Added: Quick Start, formal
     grammar headings, How to Use, Composition, Accessibility, API
     Reference, Related Components.

     Verified against source before writing:
     - page-header.jte's own 3 @params and doc comment. Confirmed `title`
       always renders as a real <h1> with no titleLevel escape hatch -
       this utility exists to BE a page's one primary heading, not a
       general-purpose heading wrapper, so there is exactly one correct
       level for it.
     - Confirmed `actions` is gg.jte.Content, matching card.jte's own
       footer slot precisely: this utility owns layout only (which
       actions exist, how many, what they do is entirely the calling
       page's decision).
     - Confirmed the responsive reflow is plain flex (`flex-col
       sm:flex-row`) with no fixed widths on either side - the heading
       column wraps onto as many lines as a long title/description need,
       and `flex-wrap` on the actions group lets several actions wrap
       onto their own line rather than overflowing. --%>
!{String quickStartCode = """
    @template.ashlar.utilities.page-header(
        title = "Dashboard",
        description = "Overview of system activity."
    )""";}
!{List<ApiParam> pageHeaderApi = List.of(
    ApiParam.required("title", "String", "Rendered as a real <h1> - the page's one primary heading, with no option to render at a different level."),
    ApiParam.optional("description", "String", "\"\"", "Supporting text below the title."),
    ApiParam.optional("actions", "gg.jte.Content", "null", "Composed Button(s) or other controls - which actions exist, how many, and what they do is entirely the calling page's decision.")
);}

@template.layout.page(
contextPath = contextPath,
title = "Page Header",
content = @`
    @template.layout.component-nav(currentSlug = "page-header", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Page Header</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A layout utility for a page's own title, optional description, and optional application-supplied actions. No surface of its own, no JavaScript - it owns structure, not appearance or behavior.</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.page-header(
                        title = "Dashboard",
                        description = "Overview of system activity."
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">How to Use</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Use exactly one Page Header per page, at the top of your <a href="${contextPath}/components/page-shell" class="text-blue-700 hover:underline dark:text-blue-400">Page Shell</a>, for the page's one real &lt;h1&gt;. For a subsection heading beneath it, that's <a href="${contextPath}/components/page-section" class="text-blue-700 hover:underline dark:text-blue-400">Page Section</a>'s job, not a second Page Header.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Title only</p>
                            <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                                @template.ashlar.utilities.page-header(title = "Dashboard")
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Title + description</p>
                            <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                                @template.ashlar.utilities.page-header(
                                title = "Dashboard",
                                description = "Overview of system activity."
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Title + actions</p>
                            <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                                @template.ashlar.utilities.page-header(
                                title = "Dashboard",
                                actions = @`@template.ashlar.components.button(text = "New report", type = "Primary")`
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Title + description + actions</p>
                            <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                                @template.ashlar.utilities.page-header(
                                title = "Dashboard",
                                description = "Overview of system activity.",
                                actions = @`@template.ashlar.components.button(text = "New report", type = "Primary")`
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Multiple actions</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Actions are supplied by the caller - Page Header only arranges them and lets them wrap.</p>
                            <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                                @template.ashlar.utilities.page-header(
                                title = "Accounts",
                                description = "Manage enterprise accounts and their access.",
                                actions = @`
                                    @template.ashlar.components.button(text = "Export", type = "Secondary")
                                    @template.ashlar.components.button(text = "Invite member", type = "Ghost")
                                    @template.ashlar.components.button(text = "New account", type = "Primary")
                                `
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Long title</p>
                            <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                                @template.ashlar.utilities.page-header(
                                title = "Quarterly Compliance and Access Review for Enterprise Accounts",
                                actions = @`@template.ashlar.components.button(text = "Export", type = "Secondary")`
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Long description</p>
                            <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                                @template.ashlar.utilities.page-header(
                                title = "Audit Log",
                                description = "A complete, chronological record of every access change, permission grant, and administrative action taken across all enterprise accounts, retained for the duration required by your organization's compliance policy."
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Narrow / mobile layout</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Below the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">sm</code> breakpoint the actions move below the heading and wrap instead of overflowing.</p>
                            <div class="mt-3 max-w-xs rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                                @template.ashlar.utilities.page-header(
                                title = "Dashboard",
                                description = "Overview of system activity.",
                                actions = @`
                                    @template.ashlar.components.button(text = "Export", type = "Secondary")
                                    @template.ashlar.components.button(text = "New report", type = "Primary")
                                `
                                )
                            </div>
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composition</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Page Header is the top block of the same layout family as <a href="${contextPath}/components/page-shell" class="text-blue-700 hover:underline dark:text-blue-400">Page Shell</a> and <a href="${contextPath}/components/page-section" class="text-blue-700 hover:underline dark:text-blue-400">Page Section</a> - see Page Shell's own page for the full composition shown together. <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">actions</code> accepts ordinary composed Button calls, the same shape as Card's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">footer</code> slot.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Ashlar provides</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>A real &lt;h1&gt; - the page's one primary heading, with no risk of landing at the wrong level since there's no parameter to change it.</li>
                            </ul>
                        </div>
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>Using this exactly once per page - a second &lt;h1&gt; elsewhere would contradict the one real page title.</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div>
                    @template.layout.api-reference(apiParams = pageHeaderApi)
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/page-shell" class="text-blue-700 hover:underline dark:text-blue-400">Page Shell</a>, the outer wrapper this sits inside &middot;
                        <a href="${contextPath}/components/page-section" class="text-blue-700 hover:underline dark:text-blue-400">Page Section</a> for the subsection heading beneath it &middot;
                        <a href="${contextPath}/components/form-actions" class="text-blue-700 hover:underline dark:text-blue-400">Form Actions</a> for a form's own end-aligned buttons instead of page-level actions
                    </p>
                </div>
            </div>
        </section>
`
)
