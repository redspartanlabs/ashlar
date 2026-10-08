@import dev.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import dev.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

!{String quickStartCode = """
    @template.ashlar.components.textarea(
        name = "notes",
        label = "Notes",
        rows = 4
    )""";}
!{String basicCode = """
    @template.ashlar.components.textarea(
        name = "notes",
        label = "Notes"
    )
    @template.ashlar.components.textarea(
        name = "comments",
        label = "Comments",
        placeholder = "Share your thoughts..."
    )
    @template.ashlar.components.textarea(
        name = "address",
        label = "Address",
        value = "123 Main St\\nSuite 400\\nAustin, TX 78701",
        rows = 3
    )""";}
!{String statesCode = """
    @template.ashlar.components.textarea(
        name = "internalNotes",
        label = "Internal Notes",
        value = "Escalated to billing on 2026-09-10.",
        disabled = true
    )
    @template.ashlar.components.textarea(
        name = "ticketDescription",
        label = "Ticket Description",
        value = "Customer reports checkout fails on the final step with a timeout error.",
        readonly = true,
        helpText = "Submitted by the customer and can't be edited."
    )""";}
!{String fixedHeightCode = """
    @template.ashlar.components.textarea(
        name = "fixedHeight",
        label = "Short reply",
        rows = 3,
        resizable = false,
        helpText = "Can't be resized by dragging."
    )""";}
!{String constraintsCode = """
    @template.ashlar.components.textarea(
        name = "productReview",
        label = "Product Review",
        placeholder = "What did you think?",
        helpText = "At least 20 characters.",
        minlength = 20,
        maxlength = 500,
        showCharacterCount = true,
        rows = 5
    )
    @template.ashlar.components.textarea(
        name = "supportMessage",
        label = "Support Message",
        maxlength = 160,
        showCharacterCount = true,
        rows = 4
    )""";}
!{String errorCode = """
    @template.ashlar.components.textarea(
        name = "flaggedFeedback",
        label = "Feedback",
        value = "This product is",
        error = true,
        errorMessage = "Feedback must be at least 20 characters."
    )""";}
!{String requiredCode = """
    <form action="" method="get">
        @template.ashlar.components.textarea(
            name = "requiredFeedback",
            label = "Feedback",
            placeholder = "What could we do better?",
            required = true,
            rows = 4
        )
        @template.ashlar.components.button(text = "Submit", type = "Primary", buttonType = "submit")
    </form>""";}
!{List<ApiParam> textareaApi = List.of(
    ApiParam.required("name", "String", "The field name values submit under."),
    ApiParam.required("label", "String", "Always rendered and associated with the control via a real <label for>."),
    ApiParam.optional("value", "String", "\"\"", "Initial value."),
    ApiParam.optional("placeholder", "String", "\"\"", "Shown only while the field is empty."),
    ApiParam.optional("required", "boolean", "false", "Enables native constraint validation."),
    ApiParam.optional("disabled", "boolean", "false", "Disables the control. Not focusable or submitted."),
    ApiParam.optional("readonly", "boolean", "false", "Visible and focusable, but not editable. Still submitted."),
    ApiParam.optional("id", "String", "\"\"", "Defaults to name when omitted."),
    ApiParam.optional("helpText", "String", "\"\"", "Supporting text below the field."),
    ApiParam.optional("error", "boolean", "false", "Shows errorMessage and applies error styling."),
    ApiParam.optional("errorMessage", "String", "\"\"", "Rendered with role=\"alert\" when error is true."),
    ApiParam.optional("minlength", "Integer", "null", "Native minlength constraint."),
    ApiParam.optional("maxlength", "Integer", "null", "Native maxlength constraint. Also what a live character count displays against."),
    ApiParam.optional("autocomplete", "String", "\"\"", "Native autocomplete token."),
    ApiParam.optional("rows", "int", "4", "Initial visible height, in text rows."),
    ApiParam.optional("resizable", "boolean", "true", "Whether a visitor can drag-resize the field vertically."),
    ApiParam.optional("showCharacterCount", "boolean", "false", "Adds a live N / max counter, associated via aria-describedby.")
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
title = "Textarea",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "textarea", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The multiline sibling of Text Input - same validation/help/error conventions, no <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">pattern</code> (a &lt;textarea&gt; has none natively), and an opt-in live character count.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3 max-w-sm">
                    @template.ashlar.components.textarea(name = "quickStartNotes", label = "Notes", rows = 4)
                </div>
            `
            )
        </section>

        <section id="how-to-use" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "How to Use",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Reach for Textarea for free text that's genuinely expected to run more than one line - a comment, a description, feedback. For a single line, <a href="${contextPath}/components/text-input" class="text-blue-700 hover:underline dark:text-blue-400">Text Input</a> is the better fit even when the value could technically be long - a textarea signals "write as much as you need" to a visitor before they've typed anything.</p>
            `
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
                    @template.layout.example(
                    label = "Basic: empty, placeholder, pre-populated",
                    code = basicCode,
                    content = @`
                        <div class="grid gap-4 sm:grid-cols-2">
                            @template.ashlar.components.textarea(
                            name = "notes",
                            label = "Notes"
                            )
                            @template.ashlar.components.textarea(
                            name = "comments",
                            label = "Comments",
                            placeholder = "Share your thoughts..."
                            )
                        </div>
                        <div class="mt-4">
                            @template.ashlar.components.textarea(
                            name = "address",
                            label = "Address",
                            value = "123 Main St\nSuite 400\nAustin, TX 78701",
                            rows = 3
                            )
                        </div>
                    `
                    )

                    @template.layout.example(
                    label = "States: disabled, readonly, help text",
                    code = statesCode,
                    content = @`
                        <div class="grid gap-4 sm:grid-cols-2">
                            @template.ashlar.components.textarea(
                            name = "internalNotes",
                            label = "Internal Notes",
                            value = "Escalated to billing on 2026-09-10.",
                            disabled = true
                            )
                            @template.ashlar.components.textarea(
                            name = "ticketDescription",
                            label = "Ticket Description",
                            value = "Customer reports checkout fails on the final step with a timeout error.",
                            readonly = true,
                            helpText = "Submitted by the customer and can't be edited."
                            )
                        </div>
                    `
                    )

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Sizing: short, normal, larger</p>
                        <div class="mt-3 grid gap-4 sm:grid-cols-3">
                            @template.ashlar.components.textarea(
                            name = "summary",
                            label = "Short Summary",
                            rows = 2
                            )
                            @template.ashlar.components.textarea(
                            name = "description",
                            label = "Description",
                            rows = 4
                            )
                            @template.ashlar.components.textarea(
                            name = "detailedFeedback",
                            label = "Detailed Feedback",
                            rows = 8
                            )
                        </div>
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Fixed height: resizable = false</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">No drag handle - the field stays exactly the height <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">rows</code> sets.</p>
                        @template.layout.example(
                        code = fixedHeightCode,
                        content = @`
                            <div class="mt-3 max-w-sm">
                                @template.ashlar.components.textarea(
                                name = "fixedHeight",
                                label = "Short reply",
                                rows = 3,
                                resizable = false,
                                helpText = "Can't be resized by dragging."
                                )
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Constraints &amp; live character count</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Two independent counters. Typing, deleting, and pasting all update the count; the browser's own maxlength enforcement prevents going over, and the count turns red once you hit it.</p>
                        @template.layout.example(
                        code = constraintsCode,
                        content = @`
                            <div class="mt-3 grid gap-4 sm:grid-cols-2">
                                @template.ashlar.components.textarea(
                                name = "productReview",
                                label = "Product Review",
                                placeholder = "What did you think?",
                                helpText = "At least 20 characters.",
                                minlength = 20,
                                maxlength = 500,
                                showCharacterCount = true,
                                rows = 5
                                )
                                @template.ashlar.components.textarea(
                                name = "supportMessage",
                                label = "Support Message",
                                maxlength = 160,
                                showCharacterCount = true,
                                rows = 4
                                )
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Server-reported error</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Forced via the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">error</code> param - the same way a controller would re-render a field after a failed server-side check.</p>
                        @template.layout.example(
                        code = errorCode,
                        content = @`
                            <div class="mt-3 max-w-md">
                                @template.ashlar.components.textarea(
                                name = "flaggedFeedback",
                                label = "Feedback",
                                value = "This product is",
                                error = true,
                                errorMessage = "Feedback must be at least 20 characters."
                                )
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Required validation, keyboard/Tab order &amp; form submission</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Tab in from the field above, try submitting empty - the browser's own validation UI takes over and the border turns red once you've interacted with it - then fill it in and submit to see the value reflected in the URL's query string.</p>
                        @template.layout.example(
                        code = requiredCode,
                        content = @`
                            <form class="mt-3 flex flex-col gap-4" action="" method="get">
                                <div class="max-w-md">
                                    @template.ashlar.components.textarea(
                                    name = "requiredFeedback",
                                    label = "Feedback",
                                    placeholder = "What could we do better?",
                                    required = true,
                                    rows = 4
                                    )
                                </div>
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
                            <li>Turn on <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">showCharacterCount</code> whenever <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">maxlength</code> is set - a hard cutoff with no visible count is easy to hit by surprise.</li>
                            <li>Size <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">rows</code> to the content you actually expect, not the maximum you'll allow - a visitor can always resize up.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Avoid</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Setting <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">resizable = false</code> without a good reason - visitors generally expect to be able to make a textarea bigger.</li>
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
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">No <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">pattern</code> param.</strong> Unlike Text Input, a native <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">&lt;textarea&gt;</code> has no pattern-matching constraint at all - validate shape server-side if you need more than length.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">The character count isn't announced on every keystroke.</strong> It's deliberately not an <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-live</code> region - that would be far too chatty for screen reader users - so it's linked via <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-describedby</code> instead, discoverable without being spoken continuously.</li>
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
                            <li>Pure-CSS error styling, same <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">user-invalid:</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-invalid:</code> approach as Text Input.</li>
                            <li>The character count, when enabled, discoverable via <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-describedby</code> without interrupting typing with live announcements.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>A <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">label</code>, and an <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">errorMessage</code> worth reading aloud - it renders with <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="alert"</code>.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.layout.api-reference(apiParams = textareaApi)
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/text-input" class="text-blue-700 hover:underline dark:text-blue-400">Text Input</a> for single-line text &middot;
                    <a href="${contextPath}/components/form-field" class="text-blue-700 hover:underline dark:text-blue-400">Form Field</a> for wiring help/error text onto a custom control
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "textarea", contextPath = contextPath)
    </div>
`
)
