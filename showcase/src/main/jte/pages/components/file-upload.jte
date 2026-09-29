@param String contextPath

<%-- The reference implementation also imported tabs.Tab here, but never used
     the type - the page's only "Tab" references are prose about keyboard tab
     order. Dropped rather than dragging Tabs into this batch for a dead
     import. --%>

@template.layout.page(
contextPath = contextPath,
title = "File Upload",
content = @`
    @template.layout.component-nav(currentSlug = "file-upload", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">File Upload</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A real &lt;input type="file"&gt; underneath a styled card - clicking anywhere on the card opens the native file picker via plain &lt;label&gt; behavior. The only JavaScript here reflects the selected filename(s) back into the card.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic</h3>
                    @template.ashlar.components.file-upload(
                    name = "document",
                    label = "Upload Document"
                    )
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accept restriction &amp; help text</h3>
                    @template.ashlar.components.file-upload(
                    name = "pdfUpload",
                    label = "Upload PDF",
                    accept = ".pdf,application/pdf",
                    helpText = "Supported format: PDF only."
                    )
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Multiple files</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Select several files at once - try selecting 3-4 files to see the count-based summary, then click again to replace the selection entirely.</p>
                    <div class="mt-3">
                        @template.ashlar.components.file-upload(
                        name = "supportingFiles",
                        label = "Upload Supporting Files",
                        multiple = true,
                        helpText = "You can attach multiple documents."
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Disabled</h3>
                    @template.ashlar.components.file-upload(
                    name = "disabledDocument",
                    label = "Upload Document",
                    disabled = true
                    )
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Server-reported error</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Forced via the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">error</code> param - the same way a controller would re-render a field after a failed server-side check.</p>
                    <div class="mt-3">
                        @template.ashlar.components.file-upload(
                        name = "flaggedDocument",
                        label = "Upload Report",
                        error = true,
                        errorMessage = "That file type isn't supported. Please upload a PDF or DOCX."
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Required validation, keyboard &amp; form submission</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Tab to the card (focus is clearly visible), press <kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">Enter</kbd> or <kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">Space</kbd> to open the native file picker, then try submitting empty - the browser's own validation UI takes over.</p>
                    <%-- method="post" + multipart/form-data, unlike every other
                         demo form on this page: a GET form can't carry file
                         content at all (browsers submit at most the bare
                         filename as text, silently dropping the rest), so
                         this is the one case where GET's "see it in the
                         query string" convention genuinely doesn't apply.
                         There's no backend route for this POST in the
                         playground on purpose - clicking Submit here will
                         hit a 405, which is expected rather than a bug. --%>
                    <form class="mt-3 flex flex-col gap-4" action="" method="post" enctype="multipart/form-data">
                        @template.ashlar.components.file-upload(
                        name = "reportUpload",
                        label = "Upload Report",
                        required = true,
                        helpText = "Supported formats: PDF, DOCX."
                        )
                        @template.ashlar.components.button(text = "Submit", type = "Primary", buttonType = "submit")
                    </form>
                </div>
            </div>
        </section>
`
)
