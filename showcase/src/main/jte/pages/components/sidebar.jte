@import com.redspartanlabs.ashlar.icon.Icon
@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param String contextPath

<%-- Batch 11 documentation page. Existing "Basic" / "With icons" / "Beside
     page content" / "Off-canvas on a phone" examples kept verbatim.
     Added: Quick Start, formal grammar headings, How to Use, Composition
     (sidebar-item documented here, not as a standalone page), Accessibility,
     two API Reference tables, Related Components.

     THE ONE THING THIS PAGE HAS TO GET RIGHT, per this batch's own
     explicit caution: do not assume Sidebar is a Drawer. Verified
     directly: sidebar.jte has no <script> tag, no JS file, no
     responsive breakpoint logic, and no mobile-specific behavior of any
     kind built in - it is a purely static <nav><ul> wrapper, the same
     shape and complexity as Breadcrumbs/Pagination, not a cousin of
     Drawer or Navbar's own borrowed-drawer mechanism. Confirmed directly
     in its own doc comment: "navbar.jte's built-in mobile drawer is not
     that same mechanism reused... a sidebar's relationship to the page
     varies too much from application to application to bake in one
     answer." A caller who wants it off-canvas on a phone must compose
     the identical `items` content into a real Drawer themselves - the
     existing "Off-canvas on a phone" example already demonstrates
     exactly this, manually, and is kept verbatim because it's already
     the accurate pattern.

     Verified against source before writing:
     - sidebar.jte's own 2 @params and doc comment. Confirmed no surface
       of its own (no border, background, or width) - the same policy
       Faceted Search states for the identical reason: this component
       sits inside a fixed column on one page, a Card on another, a
       Drawer on a phone, and a permanent panel would fight all three.
       Confirmed `ariaLabel` defaults to "Sidebar" (not hardcoded, unlike
       Breadcrumbs/Pagination's fixed APG names) specifically so a page
       with both a Navbar and a Sidebar can give the two <nav> landmarks
       distinct names.
     - sidebar-item.jte's own 4 @params and doc comment. CONFIRMED this
       is a genuinely separate component from nav-link.jte, not the same
       item reused with a flag, despite near-identical params - the two
       belong to different visual languages (an inline horizontal row
       versus a block-level vertical list that commonly carries a
       leading icon), the same reasoning that keeps breadcrumb-item.jte
       and pagination-item.jte apart. Confirmed the same "current wins
       over href" precedence as NavLink/Breadcrumb Item, for the same
       aria-current reason. --%>
!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.components.sidebar(\n"
        + "    items = @" + backtick + "\n"
        + "        @template.ashlar.components.sidebar-item(label = \"Overview\", href = \"/\", current = true)\n"
        + "        @template.ashlar.components.sidebar-item(label = \"Settings\", href = \"/settings\")\n"
        + "    " + backtick + "\n"
        + ")";}
!{List<ApiParam> sidebarApi = List.of(
    ApiParam.required("items", "gg.jte.Content", "One or more sidebar-item(...) calls."),
    ApiParam.optional("ariaLabel", "String", "\"Sidebar\"", "Names this <nav> landmark - useful mainly so a page with both a Navbar and a Sidebar can give the two distinct names.")
);}
!{List<ApiParam> sidebarItemApi = List.of(
    ApiParam.required("label", "String", "The entry's text - the row's entire accessible name, even when icon is also set."),
    ApiParam.optional("href", "String", "null", "Renders a real <a>. Ignored when current is true."),
    ApiParam.optional("current", "boolean", "false", "Marks this as the page already being viewed: non-interactive text with aria-current=\"page\" instead of a link, even if href is also set."),
    ApiParam.optional("icon", "Icon", "null", "Always decorative, leading the label.")
);}

@template.layout.page(
contextPath = contextPath,
title = "Sidebar",
content = @`
    @template.layout.component-nav(currentSlug = "sidebar", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Sidebar</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A vertical list of destinations, for a persistent side navigation column. No surface, no width and no collapse behavior of its own - the same "the caller supplies the surface" policy Faceted Search follows, since a sidebar sits inside a fixed column on one page, inside a Card on another, and inside a Drawer on a phone.</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3 max-w-xs rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.components.sidebar(
                        items = @`
                            @template.ashlar.components.sidebar-item(label = "Overview", href = "#", current = true)
                            @template.ashlar.components.sidebar-item(label = "Settings", href = "#")
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">How to Use</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Reach for Sidebar for a persistent, vertical set of destinations beside a page's main content. It has no opinion about layout or responsiveness - you supply the column (a CSS grid, as below) and, separately, whatever happens on narrow screens. For a top masthead that already collapses into a mobile menu on its own, that's <a href="${contextPath}/components/navbar" class="text-blue-700 hover:underline dark:text-blue-400">Navbar</a>'s job, not this one's - the two solve genuinely different layout problems and don't share a collapse mechanism.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Basic</p>
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
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">With icons</p>
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
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composition</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Sidebar is the wrapper half of a wrapper/item pair, the same split Breadcrumbs and Pagination use. <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">sidebar-item</code> is not a separate catalog component; it only ever exists composed into Sidebar's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">items</code> slot, documented here in the table below - a deliberately separate component from NavLink despite similar params, since the two belong to different visual languages (inline row vs. vertical list).</p>
                    <div class="mt-4 flex flex-col gap-8">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Beside page content</p>
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
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Off-canvas on a phone</p>
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
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Ashlar provides</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>A real, named &lt;nav&gt; landmark and a real &lt;ul&gt;, so "a list of navigation links" is already communicated with no extra ARIA.</li>
                                <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-current="page"</code> on the current entry, and nowhere else.</li>
                            </ul>
                        </div>
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>The surrounding layout column - this component has no width or surface of its own.</li>
                                <li>Any off-canvas/collapse behavior for narrow screens, by composing the same items into a real Drawer yourself - there is none built in.</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div class="flex flex-col gap-6">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">sidebar</p>
                        @template.layout.api-reference(apiParams = sidebarApi)
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">sidebar-item</p>
                        @template.layout.api-reference(apiParams = sidebarItemApi)
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/navbar" class="text-blue-700 hover:underline dark:text-blue-400">Navbar</a>, which has its own built-in mobile collapse, unlike this component &middot;
                        <a href="${contextPath}/components/drawer" class="text-blue-700 hover:underline dark:text-blue-400">Drawer</a>, the component to compose this into for off-canvas behavior &middot;
                        <a href="${contextPath}/components/nav-link" class="text-blue-700 hover:underline dark:text-blue-400">NavLink</a>, the closest equivalent for an inline, horizontal link list
                    </p>
                </div>
            </div>
        </section>
`
)
