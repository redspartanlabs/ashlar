@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param String contextPath

<%-- Batch 12 (final) documentation page. Existing "Simple copyright line" /
     "Copyright plus links" / "In page context" examples kept verbatim.
     Added: Quick Start, formal grammar headings, How to Use, Composition,
     Accessibility, API Reference, Related Components.

     Verified against source before writing:
     - footer.jte's own single @param (content) and doc comment.
       Confirmed this renders a real <footer> - the page's closing
       landmark - matching the same mx-auto/max-w-6xl/px-4 width rhythm
       layout/page.jte's own header bar and <main> already use.
     - Confirmed the muted text-sm text-slate-500 default is applied
       here deliberately (not left to the caller) because it matches a
       footer's usual register in nearly every real case, while still
       being ordinary inherited text styling a caller's own composed
       markup can override, not classes stamped onto that markup. --%>
!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.utilities.footer(\n"
        + "    content = @" + backtick + "<p>&copy; 2026 Acme Enterprise Console.</p>" + backtick + "\n"
        + ")";}
!{List<ApiParam> footerApi = List.of(
    ApiParam.required("content", "gg.jte.Content", "The footer's own content - copyright text, a link list, a status line - entirely composed by the caller.")
);}

@template.layout.page(
contextPath = contextPath,
title = "Footer",
content = @`
    @template.layout.component-nav(currentSlug = "footer", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Footer</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The page's closing landmark: a top rule and the library's own width/padding rhythm, matching the header bar and <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">&lt;main&gt;</code> above it. No surface, no forced columns - the content is entirely the caller's.</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3 rounded-lg border border-dashed border-slate-200 dark:border-slate-700">
                        @template.ashlar.utilities.footer(
                        content = @`<p>&copy; 2026 Acme Enterprise Console.</p>`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">How to Use</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Render exactly one Footer near the end of the page, outside <a href="${contextPath}/components/page-shell" class="text-blue-700 hover:underline dark:text-blue-400">Page Shell</a> rather than as one of its children - it's its own landmark with its own width rhythm, the same way this showcase's own layout places it.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Simple copyright line</p>
                            <div class="mt-3 rounded-lg border border-dashed border-slate-200 dark:border-slate-700">
                                @template.ashlar.utilities.footer(
                                content = @`<p>&copy; 2026 Acme Enterprise Console. All rights reserved.</p>`
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Copyright plus links</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A common shape: status text on the left, a short link list on the right, composed by the caller with ordinary flex utilities.</p>
                            <div class="mt-3 rounded-lg border border-dashed border-slate-200 dark:border-slate-700">
                                @template.ashlar.utilities.footer(
                                content = @`
                                    <div class="flex flex-col items-start gap-3 sm:flex-row sm:items-center sm:justify-between">
                                        <p>&copy; 2026 Acme Enterprise Console.</p>
                                        <div class="flex flex-wrap items-center gap-4">
                                            <a href="#" class="hover:text-slate-700 hover:underline underline-offset-2 dark:hover:text-slate-200">Privacy</a>
                                            <a href="#" class="hover:text-slate-700 hover:underline underline-offset-2 dark:hover:text-slate-200">Terms</a>
                                            <a href="#" class="hover:text-slate-700 hover:underline underline-offset-2 dark:hover:text-slate-200">Status</a>
                                        </div>
                                    </div>
                                `
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">In page context</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Sits below a page's own content, closing it off the same way this very showcase page's own footer does.</p>
                            <div class="mt-3 rounded-lg border border-dashed border-slate-200 dark:border-slate-700">
                                <div class="p-6">
                                    <h4 class="text-base font-semibold text-slate-900 dark:text-slate-100">Quarterly Report</h4>
                                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Ordinary page content sits above the footer with no special coordination between the two.</p>
                                </div>
                                @template.ashlar.utilities.footer(
                                content = @`<p>&copy; 2026 Acme Enterprise Console.</p>`
                                )
                            </div>
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composition</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Footer accepts arbitrary composed content the same free-form way <a href="${contextPath}/components/form-actions" class="text-blue-700 hover:underline dark:text-blue-400">Form Actions</a> and <a href="${contextPath}/components/page-section" class="text-blue-700 hover:underline dark:text-blue-400">Page Section</a> already do - copyright text, a link list, or a composed <a href="${contextPath}/components/status-line" class="text-blue-700 hover:underline dark:text-blue-400">Status Line</a> all work, with Footer owning only where the closing region sits on the page, never what's inside it.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Ashlar provides</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>A real &lt;footer&gt; landmark, native to the browser and assistive tech with no role needed.</li>
                            </ul>
                        </div>
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>Everything inside it - there is no default content, and no assumed structure beyond the one landmark.</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div>
                    @template.layout.api-reference(apiParams = footerApi)
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/page-shell" class="text-blue-700 hover:underline dark:text-blue-400">Page Shell</a>, matching the same width rhythm &middot;
                        <a href="${contextPath}/components/status-line" class="text-blue-700 hover:underline dark:text-blue-400">Status Line</a>, a common composed content choice
                    </p>
                </div>
            </div>
        </section>
`
)
