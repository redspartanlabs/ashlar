@param String contextPath

<%-- The reference implementation also imported tabs.Tab here, but never used
     the type - the page's only "Tab" references are prose about keyboard tab
     order. Dropped rather than dragging Tabs into this batch for a dead
     import. --%>

@template.layout.page(
contextPath = contextPath,
title = "Textarea",
content = @`
    @template.layout.component-nav(currentSlug = "textarea", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Textarea</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The multiline sibling of Text Input - same validation/help/error conventions, no <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">pattern</code> (a &lt;textarea&gt; has none natively), and an opt-in live character count.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic: empty, placeholder, pre-populated</h3>
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
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">States: disabled, readonly, help text</h3>
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
                    <div class="mt-4 max-w-md">
                        @template.ashlar.components.textarea(
                        name = "bio",
                        label = "Bio",
                        placeholder = "Tell us about yourself...",
                        helpText = "This will be visible on your public profile.",
                        rows = 3
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Sizing: short, normal, larger</h3>
                    <div class="grid gap-4 sm:grid-cols-3">
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Constraints &amp; live character count</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Two independent counters. Typing, deleting, and pasting all update the count; the browser's own maxlength enforcement prevents going over, and the count turns red once you hit it.</p>
                    <div class="grid gap-4 sm:grid-cols-2">
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
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Server-reported error</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Forced via the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">error</code> param - the same way a controller would re-render a field after a failed server-side check.</p>
                    <div class="mt-3 max-w-md">
                        @template.ashlar.components.textarea(
                        name = "flaggedFeedback",
                        label = "Feedback",
                        value = "This product is",
                        error = true,
                        errorMessage = "Feedback must be at least 20 characters."
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Required validation, keyboard/Tab order &amp; form submission</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Tab in from the field above, try submitting empty - the browser's own validation UI takes over and the border turns red once you've interacted with it - then fill it in and submit to see the value reflected in the URL's query string.</p>
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
                </div>
            </div>
        </section>
`
)
