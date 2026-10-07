@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import com.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

!{String quickStartCode = """
    @template.ashlar.components.file-upload(
        name = "document",
        label = "Upload Document"
    )""";}
!{String acceptCode = """
    @template.ashlar.components.file-upload(
        name = "pdfUpload",
        label = "Upload PDF",
        accept = ".pdf,application/pdf",
        helpText = "Supported format: PDF only."
    )""";}
!{String multipleCode = """
    @template.ashlar.components.file-upload(
        name = "supportingFiles",
        label = "Upload Supporting Files",
        multiple = true,
        helpText = "You can attach multiple documents."
    )""";}
!{String errorCode = """
    @template.ashlar.components.file-upload(
        name = "flaggedDocument",
        label = "Upload Report",
        error = true,
        errorMessage = "That file type isn't supported. Please upload a PDF or DOCX."
    )""";}
!{String requiredCode = """
    <form action="" method="post" enctype="multipart/form-data">
        @template.ashlar.components.file-upload(
            name = "reportUpload",
            label = "Upload Report",
            required = true,
            helpText = "Supported formats: PDF, DOCX."
        )
        @template.ashlar.components.button(text = "Submit", type = "Primary", buttonType = "submit")
    </form>""";}
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
!{List<PageAnchor> sections = List.of(
    new PageAnchor("quick-start", "Quick Start"),
    new PageAnchor("how-to-use", "How to Use"),
    new PageAnchor("variants", "Variants & States"),
    new PageAnchor("best-practices", "Best Practices"),
    new PageAnchor("pitfalls", "Pitfalls"),
    new PageAnchor("accessibility", "Accessibility"),
    new PageAnchor("api-reference", "API Reference"),
    new PageAnchor("related", "Related Components")
);}

@template.layout.page(
contextPath = contextPath,
title = "File Upload",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "file-upload", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A real &lt;input type="file"&gt; underneath a styled card - clicking anywhere on the card opens the native file picker via plain &lt;label&gt; behavior. The only JavaScript here reflects the selected filename(s) back into the card; selecting, replacing, and actually submitting the file are otherwise entirely native.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3">
                    @template.ashlar.components.file-upload(name = "quickStartDocument", label = "Upload Document")
                </div>
            `
            )
        </section>

        <section id="how-to-use" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "How to Use",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Reach for File Upload for any form that needs to send a real file to the server - it must be submitted inside a <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">&lt;form method="post" enctype="multipart/form-data"&gt;</code>, since a GET form can't carry file content at all (see the Required validation example below). This component is purely the picker and its own form participation - it does not upload anything itself, show progress, preview the file's contents, or talk to any endpoint; all of that is your application's responsibility once the form is submitted.</p>
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
                        @template.ashlar.components.file-upload(
                        name = "document",
                        label = "Upload Document"
                        )
                    </div>

                    @template.layout.example(
                    label = "Accept restriction &amp; help text",
                    code = acceptCode,
                    content = @`
                        @template.ashlar.components.file-upload(
                        name = "pdfUpload",
                        label = "Upload PDF",
                        accept = ".pdf,application/pdf",
                        helpText = "Supported format: PDF only."
                        )
                    `
                    )

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Multiple files</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Select several files at once - try selecting 3-4 files to see the count-based summary, then click again to replace the selection entirely.</p>
                        @template.layout.example(
                        code = multipleCode,
                        content = @`
                            <div class="mt-3">
                                @template.ashlar.components.file-upload(
                                name = "supportingFiles",
                                label = "Upload Supporting Files",
                                multiple = true,
                                helpText = "You can attach multiple documents."
                                )
                            </div>
                        `
                        )
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
                        @template.layout.example(
                        code = errorCode,
                        content = @`
                            <div class="mt-3">
                                @template.ashlar.components.file-upload(
                                name = "flaggedDocument",
                                label = "Upload Report",
                                error = true,
                                errorMessage = "That file type isn't supported. Please upload a PDF or DOCX."
                                )
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Required validation, keyboard &amp; form submission</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Tab to the card (focus is clearly visible), press <kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">Enter</kbd> or <kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">Space</kbd> to open the native file picker, then try submitting empty - the browser's own validation UI takes over, since this component has no custom error-reveal of its own (see Pitfalls). Clicking Submit with a file selected will return a 405: this showcase intentionally has no backend route to receive it - a GET form can't carry file content at all, so this is the one example on this page that genuinely needs POST, and there's nothing to post it to here on purpose.</p>
                        @template.layout.example(
                        code = requiredCode,
                        content = @`
                            <form class="mt-3 flex flex-col gap-4" action="" method="post" enctype="multipart/form-data">
                                @template.ashlar.components.file-upload(
                                name = "reportUpload",
                                label = "Upload Report",
                                required = true,
                                helpText = "Supported formats: PDF, DOCX."
                                )
                                @template.ashlar.components.button(text = "Submit", type = "Primary", buttonType = "submit")
                            </form>
                        `
                        )
                    </div>
                </div>
            `
            )
        </section>

        <section id="best-practices" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Best Practices",
            content = @`
                <div class="grid gap-6 sm:grid-cols-2">
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
            `
            )
        </section>

        <section id="pitfalls" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Pitfalls & Troubleshooting",
            content = @`
                <ul class="list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">An empty required field shows only the browser's native validation bubble.</strong> Unlike Select, Combobox, and Date Picker, this component has no JavaScript <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">invalid</code> listener at all - there's no custom-styled error to reveal on a client-side validation failure. The <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">error</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">errorMessage</code> params only ever render a server-decided error on page load.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">There is no remove/clear button.</strong> Once a file is selected, the only way to change it is picking again (which replaces the selection outright) - there's no affordance to clear it back to empty without a page reload.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">A 405 on submit here is this showcase's own missing backend route, not an Ashlar defect.</strong> The component only builds the native file input and form participation correctly; it was never meant to have anywhere real to send to in this demo.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">With <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">multiple</code>, the status line shows a count, not every filename.</strong> "3 files selected" is intentional - showing every name risks overflowing the card.</li>
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
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.layout.api-reference(apiParams = fileUploadApi)
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/form-field" class="text-blue-700 hover:underline dark:text-blue-400">Form Field</a> for wiring help/error text onto a custom control &middot;
                    <a href="${contextPath}/components/button" class="text-blue-700 hover:underline dark:text-blue-400">Button</a> for the visual style the "Choose File" label borrows
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "file-upload", contextPath = contextPath)
    </div>
`
)
