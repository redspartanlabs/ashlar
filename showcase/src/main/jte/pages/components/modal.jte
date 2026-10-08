@import dev.redspartanlabs.ashlar.icon.Icon
@import dev.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import dev.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

<%-- The backtick is isolated here, on its own line, as its own complete
     and already-closed string literal - never typed adjacent to "@"
     within one literal in the text block below. See usage-snippet.jte's
     doc comment for the full explanation. --%>
!{String oneBacktick = "`";}
!{String quickStartCode = """
    @template.ashlar.components.button(text = "Open", type = "Secondary", attributes = "data-modal-open=\\"exampleModal\\"")

    @template.ashlar.components.modal(
        id = "exampleModal",
        title = "Example modal",
        body = @"""
    + oneBacktick + """
<p>Dialog content goes here.</p>"""
    + oneBacktick + """

    )""";}
!{List<ApiParam> modalApi = List.of(
    ApiParam.required("id", "String", "Matches the data-modal-open/data-modal-close attribute any trigger uses to control this instance."),
    ApiParam.required("title", "String", "Rendered as the dialog's accessible name via aria-labelledby - every dialog needs one."),
    ApiParam.optional("size", "String", "\"Medium\"", "Small, Medium, or Large - controls the dialog's max width."),
    ApiParam.optional("closeOnBackdrop", "boolean", "true", "Whether clicking outside the dialog closes it."),
    ApiParam.optional("closeOnEscape", "boolean", "true", "Whether the Escape key closes it."),
    ApiParam.optional("showCloseButton", "boolean", "true", "Whether the header's own close button is rendered."),
    ApiParam.optional("body", "gg.jte.Content", "null", "The dialog's main content - any composed JTE content."),
    ApiParam.optional("footer", "gg.jte.Content", "null", "Typically one or more Buttons, right-aligned; omit for no footer at all.")
);}
!{List<PageAnchor> sections = List.of(
    new PageAnchor("quick-start", "Quick Start"),
    new PageAnchor("how-to-use", "How to Use"),
    new PageAnchor("variants", "Variants & States"),
    new PageAnchor("composition", "Composition"),
    new PageAnchor("best-practices", "Best Practices"),
    new PageAnchor("pitfalls", "Pitfalls"),
    new PageAnchor("accessibility", "Accessibility"),
    new PageAnchor("api-reference", "API Reference"),
    new PageAnchor("related", "Related Components")
);}

@template.layout.page(
contextPath = contextPath,
title = "Modal",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "modal", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Accessible dialogs with focus trapping, Escape/backdrop control, and scroll locking.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3">
                    @template.ashlar.components.button(text = "Open", type = "Secondary", attributes = "data-modal-open=\"quickStartModal\"")
                </div>
                @template.ashlar.components.modal(
                id = "quickStartModal",
                title = "Example modal",
                body = @`<p>Dialog content goes here.</p>`
                )
            `
            )
        </section>

        <section id="how-to-use" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "How to Use",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Modal is for a focused, blocking task that interrupts the current page - confirming a destructive action, a short form, a detail view a visitor needs to finish or dismiss before doing anything else. For content that supports the current page without blocking it, use <a href="${contextPath}/components/popover" class="text-blue-700 hover:underline dark:text-blue-400">Popover</a> or <a href="${contextPath}/components/drawer" class="text-blue-700 hover:underline dark:text-blue-400">Drawer</a> instead - both are non-modal, and Drawer suits more content or a sustained task like filtering. A trigger is any element carrying <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">data-modal-open="&lt;id&gt;"</code>; the modal itself is rendered once, anywhere on the page, and can be opened from as many triggers as you like.</p>
            `
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Every dialog below is already rendered in the page - each button only opens the one with a matching id. Nine independent states: basic, rich content, footer actions, both sizes, each dismissal control turned off in turn, and a destructive confirmation.</p>
                <div class="mt-4 flex flex-wrap gap-3">
                    @template.ashlar.components.button(text = "Basic Modal", type = "Secondary", attributes = "data-modal-open=\"basicModal\"")
                    @template.ashlar.components.button(text = "Modal with Content", type = "Secondary", attributes = "data-modal-open=\"contentModal\"")
                    @template.ashlar.components.button(text = "With Footer", type = "Secondary", attributes = "data-modal-open=\"footerModal\"")
                    @template.ashlar.components.button(text = "Small", type = "Secondary", attributes = "data-modal-open=\"smallModal\"")
                    @template.ashlar.components.button(text = "Large", type = "Secondary", attributes = "data-modal-open=\"largeModal\"")
                    @template.ashlar.components.button(text = "No Close Button", type = "Secondary", attributes = "data-modal-open=\"noCloseButtonModal\"")
                    @template.ashlar.components.button(text = "Escape Disabled", type = "Secondary", attributes = "data-modal-open=\"noEscapeModal\"")
                    @template.ashlar.components.button(text = "Backdrop Click Disabled", type = "Secondary", attributes = "data-modal-open=\"noBackdropModal\"")
                    @template.ashlar.components.button(text = "Delete Record", type = "Danger", icon = Icon.DELETE, attributes = "data-modal-open=\"deleteModal\"")
                </div>

                @template.ashlar.components.modal(
                id = "basicModal",
                title = "Basic Modal",
                body = @`
                    <p>This is a basic modal with a simple message and no footer actions.</p>
                `
                )

                @template.ashlar.components.modal(
                id = "contentModal",
                title = "Account Details",
                body = @`
                    <dl class="grid grid-cols-3 gap-x-4 gap-y-2">
                        <dt class="text-slate-500 dark:text-slate-400">Account</dt>
                        <dd class="col-span-2 text-slate-900 dark:text-slate-100">Acme Manufacturing</dd>
                        <dt class="text-slate-500 dark:text-slate-400">Status</dt>
                        <dd class="col-span-2 text-slate-900 dark:text-slate-100">Active</dd>
                        <dt class="text-slate-500 dark:text-slate-400">Owner</dt>
                        <dd class="col-span-2 text-slate-900 dark:text-slate-100">Dana Whitfield</dd>
                    </dl>
                    <p class="mt-4 text-slate-500 dark:text-slate-400">Any server-rendered JTE content can be passed in as the modal body.</p>
                `
                )

                @template.ashlar.components.modal(
                id = "footerModal",
                title = "Information",
                body = @`
                    <p>This is an example of a Modal with a footer.</p>
                `,
                footer = @`
                    @template.ashlar.components.button(text = "Cancel", type = "Ghost", attributes = "data-modal-close=\"footerModal\"")
                    @template.ashlar.components.button(text = "Continue", type = "Primary", attributes = "data-modal-close=\"footerModal\"")
                `
                )

                @template.ashlar.components.modal(
                id = "smallModal",
                title = "Small Modal",
                size = "Small",
                body = @`<p>This dialog uses the Small size (max-w-sm).</p>`
                )

                @template.ashlar.components.modal(
                id = "largeModal",
                title = "Large Modal",
                size = "Large",
                body = @`<p>This dialog uses the Large size (max-w-2xl), useful for richer content such as tables or multi-column forms.</p>`
                )

                @template.ashlar.components.modal(
                id = "noCloseButtonModal",
                title = "No Close Button",
                showCloseButton = false,
                body = @`<p>There is no close button in the header. Use the footer action, Escape, or the backdrop to close this.</p>`,
                footer = @`
                    @template.ashlar.components.button(text = "Close", type = "Secondary", attributes = "data-modal-close=\"noCloseButtonModal\"")
                `
                )

                @template.ashlar.components.modal(
                id = "noEscapeModal",
                title = "Escape Disabled",
                closeOnEscape = false,
                body = @`<p>Pressing Escape will not close this modal. Use the close button or click the backdrop instead.</p>`
                )

                @template.ashlar.components.modal(
                id = "noBackdropModal",
                title = "Backdrop Click Disabled",
                closeOnBackdrop = false,
                body = @`<p>Clicking outside this dialog will not close it. Use the close button or press Escape instead.</p>`
                )

                @template.ashlar.components.modal(
                id = "deleteModal",
                title = "Delete Record",
                size = "Small",
                body = @`
                    <p>Are you sure you want to delete this record?</p>
                    <p class="mt-2 font-medium text-slate-900 dark:text-slate-100">This action cannot be undone.</p>
                `,
                footer = @`
                    @template.ashlar.components.button(text = "Cancel", type = "Secondary", attributes = "data-modal-close=\"deleteModal\"")
                    @template.ashlar.components.button(text = "Delete", type = "Danger", icon = Icon.DELETE, attributes = "data-modal-close=\"deleteModal\"")
                `
                )
            `
            )
        </section>

        <section id="composition" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Composition",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Any trigger works, not only Button - any element carrying <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">data-modal-open</code> opens it. <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">body</code> and <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">footer</code> accept any composed content - the "Account Details" example above nests a plain <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">&lt;dl&gt;</code>, and form controls (Text Input, Checkbox, Select) work the same way a real edit dialog would use them. Footer buttons close the modal themselves via <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">data-modal-close="&lt;id&gt;"</code> - Modal owns opening and closing only, never what a footer action actually does.</p>
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
                            <li>Keep a dialog to one focused task - confirm, edit, review - not a secondary page.</li>
                            <li>Use Small for confirmations, Large only when content genuinely needs the room (tables, multi-column forms).</li>
                            <li>Leave <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">closeOnEscape</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">closeOnBackdrop</code> at their defaults unless the action is genuinely destructive enough to require an explicit choice.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Avoid</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Opening a Modal from inside another Modal or Drawer - nested overlays fight over focus and Escape.</li>
                            <li>Turning off both <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">showCloseButton</code> and <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">closeOnEscape</code> without a footer action - that leaves only the backdrop as a way out.</li>
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
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">Two different ids triggering the same confusion.</strong> <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">data-modal-open</code>'s value must match the modal's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">id</code> exactly - a mismatch fails silently rather than erroring, since any element can carry that attribute.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">A disabled close button with no other way out.</strong> See "Avoid" above - verify a footer action exists before turning off the header close button and Escape together.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">A Modal containing an open Drawer or Popover closes both of them on one Escape press.</strong> This component's own Escape listener is bound to the whole document with no stopPropagation, the same shape as <a href="${contextPath}/components/drawer" class="text-blue-700 hover:underline dark:text-blue-400">Drawer</a>'s and <a href="${contextPath}/components/popover" class="text-blue-700 hover:underline dark:text-blue-400">Popover</a>'s own listeners - nothing here isolates an Escape meant for a nested one of those from also reaching this Modal's.</li>
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
                            <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="dialog"</code> and <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-modal="true"</code> on the dialog itself.</li>
                            <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-labelledby</code> pointing at the title - the required-<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">title</code> param is what makes this a correctly-named dialog, not optional decoration.</li>
                            <li>Focus trapping while open, and focus returned to the trigger on close.</li>
                            <li>Background scroll locked while a modal is open.</li>
                            <li>Escape and backdrop dismissal, each independently toggleable.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>A <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">title</code> that actually names the dialog's purpose - it's read aloud the moment focus moves in.</li>
                            <li>At least one way to close a dialog where you've turned off the header button, Escape, and backdrop together (see Pitfalls).</li>
                            <li>Accessible names on any interactive content inside <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">body</code> - Modal only manages the dialog shell, not what's inside it.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.layout.api-reference(apiParams = modalApi)
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/drawer" class="text-blue-700 hover:underline dark:text-blue-400">Drawer</a> for more content or a sustained task, non-modal &middot;
                    <a href="${contextPath}/components/popover" class="text-blue-700 hover:underline dark:text-blue-400">Popover</a> for a small, non-blocking surface anchored to a trigger &middot;
                    <a href="${contextPath}/components/tooltip" class="text-blue-700 hover:underline dark:text-blue-400">Tooltip</a> for hover-revealed text only &middot;
                    Button, the usual trigger
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "modal", contextPath = contextPath)
    </div>
`
)
