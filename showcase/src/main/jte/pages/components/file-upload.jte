@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param String contextPath

<%-- Batch 2C documentation page. Existing "Basic" / "Accept restriction &
     help text" / "Multiple files" / "Disabled" / "Server-reported error" /
     "Required validation" examples are kept verbatim (moved under
     Variants & States), including the form's own POST/multipart comment
     explaining the intentional 405 - that explanation is correct and now
     also surfaced in prose below, not just a template comment.

     Verified against source before writing:
     - file-upload.jte's own @param list (10 params) and its own doc
       comment - confirmed this is a real, visually-hidden (sr-only, still
       focusable) <input type="file"> inside a <label>, opened by native
       label-click/Space/Enter behavior with zero JavaScript involved in
       opening it.
     - file-upload.js in full (27 lines - genuinely this small). Confirmed
       precisely what it does and does not do: it only reads input.files
       after a native 'change' and writes one status line - either the
       single filename, or (only when multiple is true) a count like "3
       files selected", never every filename. It never touches
       input.files, never validates size or type, and has no 'invalid'
       listener at all - unlike Select/Combobox/Date Picker, an empty
       required File Upload shows only the browser's own native
       validation bubble, not a custom-styled revealed error (see
       Pitfalls - verified by the literal absence of such a listener, not
       inferred from "it probably doesn't").
     - Confirmed there is no drag-and-drop - selection only ever happens
       through the native file picker dialog.
     - Confirmed there is no remove/clear affordance in the markup or JS -
       selecting again through the picker replaces the previous selection
       (ordinary native <input type="file"> behavior), but there is no
       button to clear a selection back to empty once one exists.
     - Confirmed `accept` is passed straight through to the native accept
       attribute - advisory filtering in the OS file picker only, not an
       enforced constraint; a user can typically still choose "All Files".
     - Confirmed the showcase's own "Required validation" example already
       correctly explains why its POST/multipart form has no real
       endpoint: a GET form can't carry file content (browsers submit at
       most the bare filename as text), so this is the one example on
       this site that genuinely needs POST - and the playground
       intentionally has no backend route for it, so Submit here hits a
       405 by design, not an Ashlar defect. --%>
!{String quickStartCode = """
    @template.ashlar.components.file-upload(
        name = "document",
        label = "Upload Document"
    )""";}
!{List<ApiParam> fileUploadApi = List.of(
    ApiParam.required("name", "String", "The field name the native <input type=\"file\"> submits under."),
    ApiParam.required("label", "String", "The visible label above the card. Also set as the input's aria-label - see Accessibility."),
    ApiParam.optional("multiple", "boolean", "false", "Allows selecting more than one file. Changes the status text to a count rather than a filename once more than one is picked."),
    ApiParam.optional("accept", "String", "\"\"", "Passed straight through to the native accept attribute - an advisory filter in the OS picker, not an enforced constraint."),
    ApiParam.optional("required", "boolean", "false", "Enables native constraint validation. No selection shows only the browser's own validation UI - see Pitfalls."),
    ApiParam.optional("disabled", "boolean", "false", "Disables the input. Not focusable, not clickable, not submitted."),
    ApiParam.optional("id", "String", "\"\"", "Defaults to name when omitted."),
    ApiParam.optional("helpText", "String", "\"\"", "Supporting text below the card."),
    ApiParam.optional("error", "boolean", "false", "Shows errorMessage and applies error styling to the card's border."),
    ApiParam.optional("errorMessage", "String", "\"\"", "Rendered with role=\"alert\" when error is true. Unlike most other inputs, this paragraph only exists in the DOM when error is true - there is no client-side error-reveal to keep it present for (see Pitfalls).")
);}

@template.layout.page(
contextPath = contextPath,
title = "File Upload",
content = @`
    @template.layout.component-nav(currentSlug = "file-upload", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">File Upload</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A real &lt;input type="file"&gt; underneath a styled card - clicking anywhere on the card opens the native file picker via plain &lt;label&gt; behavior. The only JavaScript here reflects the selected filename(s) back into the card; selecting, replacing, and actually submitting the file are otherwise entirely native.</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3">
                        @template.ashlar.components.file-upload(name = "quickStartDocument", label = "Upload Document")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">How to Use</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Reach for File Upload for any form that needs to send a real file to the server - it must be submitted inside a <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">&lt;form method="post" enctype="multipart/form-data"&gt;</code>, since a GET form can't carry file content at all (see the Required validation example below). This component is purely the picker and its own form participation - it does not upload anything itself, show progress, preview the file's contents, or talk to any endpoint; all of that is your application's responsibility once the form is submitted.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Basic</p>
                            @template.ashlar.components.file-upload(
                            name = "document",
                            label = "Upload Document"
                            )
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Accept restriction &amp; help text</p>
                            @template.ashlar.components.file-upload(
                            name = "pdfUpload",
                            label = "Upload PDF",
                            accept = ".pdf,application/pdf",
                            helpText = "Supported format: PDF only."
                            )
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Multiple files</p>
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
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Disabled</p>
                            @template.ashlar.components.file-upload(
                            name = "disabledDocument",
                            label = "Upload Document",
                            disabled = true
                            )
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Server-reported error</p>
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
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Required validation, keyboard &amp; form submission</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Tab to the card (focus is clearly visible), press <kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">Enter</kbd> or <kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">Space</kbd> to open the native file picker, then try submitting empty - the browser's own validation UI takes over, since this component has no custom error-reveal of its own (see Pitfalls). Clicking Submit with a file selected will return a 405: this showcase intentionally has no backend route to receive it - a GET form can't carry file content at all, so this is the one example on this page that genuinely needs POST, and there's nothing to post it to here on purpose.</p>
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
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Best Practices</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Do</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>Re-validate file type and size on the server regardless of <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">accept</code> - it's an OS-picker hint, not an enforced constraint.</li>
                                <li>State accepted formats in <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">helpText</code> even when <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">accept</code> is set, since a user can usually still choose a different type via "All Files".</li>
                            </ul>
                        </div>
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Avoid</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>Expecting any upload progress, preview, or server interaction from this component - it is the picker only.</li>
                                <li>Putting it in a GET form - file content simply won't be submitted.</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Pitfalls &amp; Troubleshooting</h3>
                    <ul class="mt-2 list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">An empty required field shows only the browser's native validation bubble.</strong> Unlike Select, Combobox, and Date Picker, this component has no JavaScript <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">invalid</code> listener at all - there's no custom-styled error to reveal on a client-side validation failure. The <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">error</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">errorMessage</code> params only ever render a server-decided error on page load.</li>
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">There is no remove/clear button.</strong> Once a file is selected, the only way to change it is picking again (which replaces the selection outright) - there's no affordance to clear it back to empty without a page reload.</li>
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">A 405 on submit here is this showcase's own missing backend route, not an Ashlar defect.</strong> The component only builds the native file input and form participation correctly; it was never meant to have anywhere real to send to in this demo.</li>
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">With <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">multiple</code>, the status line shows a count, not every filename.</strong> "3 files selected" is intentional - showing every name risks overflowing the card.</li>
                    </ul>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Ashlar provides</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>A real, focusable (not display:none) native input - Tab, Space/Enter-to-open, and native form submission are all the browser's own.</li>
                                <li>A stable accessible name: <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-label</code> is set explicitly so the two &lt;label&gt;s wrapping this control (one for visual grouping, one for click-to-open) don't get concatenated into a changing accessible name as the selected filename updates.</li>
                                <li>Clear focus-visible styling on the whole card, since the real input itself is visually hidden.</li>
                            </ul>
                        </div>
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>An <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">errorMessage</code> worth reading aloud - it renders with <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="alert"</code>, but only on page load (see Pitfalls).</li>
                                <li>Real server-side handling of the submitted file - and its own accessible feedback once that happens.</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div>
                    @template.layout.api-reference(apiParams = fileUploadApi)
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/form-field" class="text-blue-700 hover:underline dark:text-blue-400">Form Field</a> for wiring help/error text onto a custom control &middot;
                        <a href="${contextPath}/components/button" class="text-blue-700 hover:underline dark:text-blue-400">Button</a> for the visual style the "Choose File" label borrows
                    </p>
                </div>
            </div>
        </section>
`
)
