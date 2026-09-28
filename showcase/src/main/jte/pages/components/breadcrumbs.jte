@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Breadcrumbs",
content = @`
    @template.layout.component-nav(currentSlug = "breadcrumbs", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Breadcrumbs</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Shows where the current page sits within a hierarchy. A real &lt;nav&gt;/&lt;ol&gt; trail of links, ending in the current page's own text - no JavaScript, no click handlers, no router integration.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic hierarchy</h3>
                    <div class="mt-3">
                        @template.ashlar.components.breadcrumbs(
                        items = @`
                            @template.ashlar.components.breadcrumb-item(label = "Home", href = "/")
                            @template.ashlar.components.breadcrumb-item(label = "Products", href = "/products")
                            @template.ashlar.components.breadcrumb-item(label = "Widgets", current = true)
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Deeper hierarchy</h3>
                    <div class="mt-3">
                        @template.ashlar.components.breadcrumbs(
                        items = @`
                            @template.ashlar.components.breadcrumb-item(label = "Home", href = "/")
                            @template.ashlar.components.breadcrumb-item(label = "Accounts", href = "/accounts")
                            @template.ashlar.components.breadcrumb-item(label = "Acme Manufacturing", href = "/accounts/acme-manufacturing")
                            @template.ashlar.components.breadcrumb-item(label = "Orders", href = "/accounts/acme-manufacturing/orders")
                            @template.ashlar.components.breadcrumb-item(label = "Order #48213", current = true)
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Long labels</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Labels wrap in place rather than overflowing the page - resize the window to see it.</p>
                    <div class="mt-3 max-w-sm">
                        @template.ashlar.components.breadcrumbs(
                        items = @`
                            @template.ashlar.components.breadcrumb-item(label = "Home", href = "/")
                            @template.ashlar.components.breadcrumb-item(label = "Reports", href = "/reports")
                            @template.ashlar.components.breadcrumb-item(label = "Quarterly Enterprise Revenue Summary by Region and Product Line", current = true)
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">In page context</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Breadcrumbs sit above a page's own title, not inside a Card - they're page-level navigation, not page content.</p>
                    <div class="mt-3 rounded-lg border border-slate-200 bg-slate-50 p-6 dark:border-slate-700 dark:bg-slate-800/50">
                        @template.ashlar.components.breadcrumbs(
                        items = @`
                            @template.ashlar.components.breadcrumb-item(label = "Home", href = "/")
                            @template.ashlar.components.breadcrumb-item(label = "Settings", href = "/settings")
                            @template.ashlar.components.breadcrumb-item(label = "Team Members", current = true)
                        `
                        )
                        <h1 class="mt-3 text-xl font-semibold text-slate-900 dark:text-slate-100">Team Members</h1>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Manage who has access to this workspace.</p>
                    </div>
                </div>
            </div>
        </section>
`
)
