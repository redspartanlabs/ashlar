@import com.redspartanlabs.ashlar.icon.Icon

@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Drawer",
content = @`
    @template.layout.component-nav(currentSlug = "drawer", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Drawer</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A full-height panel anchored to the left or right edge, for navigation, settings, filters, or a form that needs more room than a Popover. It is modal: the page behind it is covered, its scroll is locked, and Tab stays inside until you close it with the close button, Escape, or the backdrop.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Opens from the right by default. Close it with the header button, Escape, or by clicking the backdrop.</p>
                    <div class="mt-3">
                        @template.ashlar.components.button(text = "Open drawer", type = "Secondary", attributes = "data-drawer-open=\"basicDrawer\"")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Placement</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400"><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">placement</code> takes Right (the default) or Left.</p>
                    <div class="mt-3 flex flex-wrap gap-3">
                        @template.ashlar.components.button(text = "From the left", type = "Secondary", attributes = "data-drawer-open=\"leftDrawer\"")
                        @template.ashlar.components.button(text = "From the right", type = "Secondary", attributes = "data-drawer-open=\"basicDrawer\"")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Interactive content and a footer</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Form controls inside are reachable with Tab and operating them does not dismiss the drawer. What Save actually does is the application's business - this component only owns opening, closing and focus.</p>
                    <div class="mt-3">
                        @template.ashlar.components.button(text = "Edit account", type = "Primary", attributes = "data-drawer-open=\"formDrawer\"")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Long content scrolls</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The header and footer stay put while the body scrolls, and the page behind stays where it was.</p>
                    <div class="mt-3">
                        @template.ashlar.components.button(text = "Open long drawer", type = "Secondary", attributes = "data-drawer-open=\"longDrawer\"")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Several triggers, one drawer</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Any element carrying <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">data-drawer-open</code> with the drawer's id opens it, and focus returns to whichever one was used.</p>
                    <div class="mt-3 flex flex-wrap items-center gap-3">
                        @template.ashlar.components.button(text = "Open from here", type = "Ghost", attributes = "data-drawer-open=\"basicDrawer\"")
                        @template.ashlar.components.button(text = "Or from here", type = "Ghost", attributes = "data-drawer-open=\"basicDrawer\"")
                    </div>
                </div>
            </div>
        </section>

        @template.ashlar.components.drawer(
        id = "basicDrawer",
        title = "Account details",
        body = @`
            <p>Acme Manufacturing has been an active enterprise account since January 2024, currently using three of five seats.</p>
            <p class="mt-3">Billing renews on the first of each month. Changes to the plan take effect at the start of the next cycle.</p>
        `
        )

        @template.ashlar.components.drawer(
        id = "leftDrawer",
        title = "Navigation",
        placement = "Left",
        body = @`
            <nav class="flex flex-col gap-1">
                <a href="#" class="rounded-md px-3 py-2 font-medium text-slate-700 hover:bg-slate-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 dark:text-slate-200 dark:hover:bg-slate-800">Dashboard</a>
                <a href="#" class="rounded-md px-3 py-2 font-medium text-slate-700 hover:bg-slate-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 dark:text-slate-200 dark:hover:bg-slate-800">Accounts</a>
                <a href="#" class="rounded-md px-3 py-2 font-medium text-slate-700 hover:bg-slate-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 dark:text-slate-200 dark:hover:bg-slate-800">Reports</a>
                <a href="#" class="rounded-md px-3 py-2 font-medium text-slate-700 hover:bg-slate-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 dark:text-slate-200 dark:hover:bg-slate-800">Settings</a>
            </nav>
        `
        )

        @template.ashlar.components.drawer(
        id = "formDrawer",
        title = "Edit account",
        body = @`
            @template.ashlar.components.text-input(name = "account-name", label = "Account name", value = "Acme Manufacturing")
            @template.ashlar.components.text-input(name = "account-owner", label = "Owner email", type = "email", value = "dana.whitfield@acme.example")
            @template.ashlar.components.checkbox(name = "account-notify", label = "Email me about plan changes", checked = true)
        `,
        footer = @`
            @template.ashlar.components.button(text = "Cancel", type = "Secondary", attributes = "data-drawer-close=\"formDrawer\"")
            @template.ashlar.components.button(text = "Save changes", type = "Primary")
        `
        )

        @template.ashlar.components.drawer(
        id = "longDrawer",
        title = "Release history",
        body = @`
            <p>Version 2.4.0 adds bulk export, improves Data Table sort performance, and fixes a rendering issue in Combobox under Firefox.</p>
            <p class="mt-3">Version 2.3.0 introduced role-based approval workflows for high-value transactions and reworked the notification preferences screen.</p>
            <p class="mt-3">Version 2.2.0 moved the platform from a single-region deployment to a multi-region architecture.</p>
            <p class="mt-3">Version 2.1.0 added support for custom fields across every major record type.</p>
            <p class="mt-3">Version 2.0.0 rebuilt the reporting engine to support scheduled exports.</p>
            <p class="mt-3">Version 1.9.0 added the audit log and per-seat permission overrides.</p>
            <p class="mt-3">Version 1.8.0 introduced the first version of the public API and its accompanying documentation.</p>
            <p class="mt-3">Version 1.7.0 reworked billing to support annual plans alongside monthly ones.</p>
            <p class="mt-3">Version 1.6.0 added saved filters to every list view in the product.</p>
            <p class="mt-3">Version 1.5.0 shipped the original dashboard and the account overview screen.</p>
        `,
        footer = @`
            @template.ashlar.components.button(text = "Close", type = "Secondary", attributes = "data-drawer-close=\"longDrawer\"")
        `
        )
`
)
