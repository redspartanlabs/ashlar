@import com.redspartanlabs.ashlar.icon.Icon

@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Modal",
content = @`
    @template.layout.component-nav(currentSlug = "modal", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Modals</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Accessible dialogs with focus trapping, Escape/backdrop control, and scroll locking.</p>

            <div class="mt-6 flex flex-wrap gap-3">
                @template.ashlar.components.button(text = "Basic Modal", type = "Secondary", attributes = "data-modal-open=\"basicModal\"")
                @template.ashlar.components.button(text = "Modal with Content", type = "Secondary", attributes = "data-modal-open=\"contentModal\"")
                @template.ashlar.components.button(text = "Modal with Footer", type = "Secondary", attributes = "data-modal-open=\"footerModal\"")
                @template.ashlar.components.button(text = "Small Modal", type = "Secondary", attributes = "data-modal-open=\"smallModal\"")
                @template.ashlar.components.button(text = "Large Modal", type = "Secondary", attributes = "data-modal-open=\"largeModal\"")
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
        </section>
`
)
