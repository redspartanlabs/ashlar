@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Footer",
content = @`
    @template.layout.component-nav(currentSlug = "footer", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Footer</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The page's closing landmark: a top rule and the library's own width/padding rhythm, matching the header bar and <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">&lt;main&gt;</code> above it. No surface, no forced columns - the content is entirely the caller's.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Simple copyright line</h3>
                    <div class="mt-3 rounded-lg border border-dashed border-slate-200 dark:border-slate-700">
                        @template.ashlar.utilities.footer(
                        content = @`<p>&copy; 2026 Acme Enterprise Console. All rights reserved.</p>`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Copyright plus links</h3>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">In page context</h3>
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
        </section>
`
)
