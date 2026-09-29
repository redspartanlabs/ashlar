@import com.redspartanlabs.ashlar.icon.Icon

@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Sidebar",
content = @`
    @template.layout.component-nav(currentSlug = "sidebar", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Sidebar</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A vertical list of destinations, for a persistent side navigation column. No surface, no width and no collapse behavior of its own - the same "the caller supplies the surface" policy Faceted Search follows, since a sidebar sits inside a fixed column on one page, inside a Card on another, and inside a Drawer on a phone.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic</h3>
                    <div class="mt-3 max-w-xs rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.components.sidebar(
                        items = @`
                            @template.ashlar.components.sidebar-item(label = "Overview", href = "#", current = true)
                            @template.ashlar.components.sidebar-item(label = "Accounts", href = "#")
                            @template.ashlar.components.sidebar-item(label = "Reports", href = "#")
                            @template.ashlar.components.sidebar-item(label = "Settings", href = "#")
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">With icons</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Purely decorative - the row's accessible name still comes from its label alone.</p>
                    <div class="mt-3 max-w-xs rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.components.sidebar(
                        items = @`
                            @template.ashlar.components.sidebar-item(label = "Dashboard", href = "#", icon = Icon.GRID, current = true)
                            @template.ashlar.components.sidebar-item(label = "Accounts", href = "#", icon = Icon.USERS)
                            @template.ashlar.components.sidebar-item(label = "Billing", href = "#", icon = Icon.CREDIT_CARD)
                            @template.ashlar.components.sidebar-item(label = "Settings", href = "#", icon = Icon.SETTINGS)
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Beside page content</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The usual composition: a fixed-width column beside the main content, stacking above it below the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">lg</code> breakpoint - the identical layout faceted-search.jte's own "Beside results" example uses.</p>
                    <div class="mt-3 grid gap-6 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700 lg:grid-cols-[13rem_1fr]">
                        @template.ashlar.components.sidebar(
                        ariaLabel = "Workspace",
                        items = @`
                            @template.ashlar.components.sidebar-item(label = "Overview", href = "#", icon = Icon.GRID, current = true)
                            @template.ashlar.components.sidebar-item(label = "Team", href = "#", icon = Icon.USERS)
                            @template.ashlar.components.sidebar-item(label = "Reports", href = "#", icon = Icon.CHART_BAR)
                        `
                        )
                        <div>
                            <h4 class="text-base font-semibold text-slate-900 dark:text-slate-100">Overview</h4>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Sidebar owns none of this - it only renders the column on the left. Everything to the right is ordinary page content the caller composed beside it.</p>
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Off-canvas on a phone</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Sidebar deliberately contains no drawer of its own, the same choice faceted-search.jte's own showcase makes and for the same reason: a page that needs it out of the way on a small screen puts the identical <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">items</code> content inside the library's existing Drawer - no second mechanism, and the entries behave identically in both places.</p>
                    <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.components.button(text = "Menu", type = "Secondary", attributes = "data-drawer-open=\"sidebarDrawer\"")

                        @template.ashlar.components.drawer(
                        id = "sidebarDrawer",
                        title = "Menu",
                        placement = "Left",
                        body = @`
                            @template.ashlar.components.sidebar(
                            items = @`
                                @template.ashlar.components.sidebar-item(label = "Overview", href = "#", icon = Icon.GRID, current = true)
                                @template.ashlar.components.sidebar-item(label = "Accounts", href = "#", icon = Icon.USERS)
                                @template.ashlar.components.sidebar-item(label = "Settings", href = "#", icon = Icon.SETTINGS)
                            `
                            )
                        `
                        )
                    </div>
                </div>
            </div>
        </section>
`
)
