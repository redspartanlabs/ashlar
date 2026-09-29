@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Navbar",
content = @`
    @template.layout.component-nav(currentSlug = "navbar", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Navbar</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A page's masthead: a brand, its primary destinations, and a trailing area for controls that should always stay visible. Below the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">md</code> breakpoint the link list collapses behind a menu button into a real Drawer - resize the window, or shrink your viewport below about 768px, to see it.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Brand and links only. <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">current = true</code> marks the active destination with <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-current="page"</code>, the same convention breadcrumb-item.jte uses.</p>
                    <div class="mt-3 overflow-hidden rounded-lg border border-dashed border-slate-200 dark:border-slate-700">
                        @template.ashlar.components.navbar(
                        brand = @`<a href="#" class="text-lg font-semibold text-slate-900 dark:text-slate-100">Acme Console</a>`,
                        items = @`
                            @template.ashlar.components.nav-link(label = "Dashboard", href = "#", current = true)
                            @template.ashlar.components.nav-link(label = "Accounts", href = "#")
                            @template.ashlar.components.nav-link(label = "Reports", href = "#")
                            @template.ashlar.components.nav-link(label = "Settings", href = "#")
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">With trailing actions</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400"><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">actions</code> stays visible at every width, even when the links collapse - a Theme Toggle is the common case, but it accepts any composed content.</p>
                    <div class="mt-3 overflow-hidden rounded-lg border border-dashed border-slate-200 dark:border-slate-700">
                        @template.ashlar.components.navbar(
                        brand = @`<a href="#" class="text-lg font-semibold text-slate-900 dark:text-slate-100">Acme Console</a>`,
                        items = @`
                            @template.ashlar.components.nav-link(label = "Dashboard", href = "#")
                            @template.ashlar.components.nav-link(label = "Accounts", href = "#", current = true)
                            @template.ashlar.components.nav-link(label = "Reports", href = "#")
                        `,
                        actions = @`@template.ashlar.components.theme-toggle(id = "navbarShowcaseToggle")`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Custom menu title</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The mobile menu is a real Drawer, so it needs an accessible name - <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">menuTitle</code> sets it (default "Menu"). Try it below a phone-sized viewport: Escape, the backdrop, and the close button all work exactly as Drawer's own page demonstrates.</p>
                    <div class="mt-3 overflow-hidden rounded-lg border border-dashed border-slate-200 dark:border-slate-700">
                        @template.ashlar.components.navbar(
                        brand = @`<a href="#" class="text-lg font-semibold text-slate-900 dark:text-slate-100">Northwind</a>`,
                        items = @`
                            @template.ashlar.components.nav-link(label = "Overview", href = "#", current = true)
                            @template.ashlar.components.nav-link(label = "Projects", href = "#")
                            @template.ashlar.components.nav-link(label = "Team", href = "#")
                            @template.ashlar.components.nav-link(label = "Billing", href = "#")
                        `,
                        menuTitle = "Navigate"
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Two on one page stay independent</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Each call composes its own Drawer with its own generated id - opening one's mobile menu never affects the other's.</p>
                    <div class="mt-3 flex flex-col gap-4">
                        <div class="overflow-hidden rounded-lg border border-dashed border-slate-200 dark:border-slate-700">
                            @template.ashlar.components.navbar(
                            brand = @`<a href="#" class="text-lg font-semibold text-slate-900 dark:text-slate-100">Product A</a>`,
                            items = @`
                                @template.ashlar.components.nav-link(label = "Home", href = "#", current = true)
                                @template.ashlar.components.nav-link(label = "Docs", href = "#")
                            `
                            )
                        </div>
                        <div class="overflow-hidden rounded-lg border border-dashed border-slate-200 dark:border-slate-700">
                            @template.ashlar.components.navbar(
                            brand = @`<a href="#" class="text-lg font-semibold text-slate-900 dark:text-slate-100">Product B</a>`,
                            items = @`
                                @template.ashlar.components.nav-link(label = "Home", href = "#", current = true)
                                @template.ashlar.components.nav-link(label = "Pricing", href = "#")
                            `
                            )
                        </div>
                    </div>
                </div>
            </div>
        </section>
`
)
