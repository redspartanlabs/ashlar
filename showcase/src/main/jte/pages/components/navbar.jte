@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import com.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

<%-- Batch 11 documentation page. Existing "Basic" / "With trailing
     actions" / "Custom menu title" / "Two on one page stay independent"
     examples kept verbatim. Added: Quick Start, formal grammar headings,
     How to Use, Composition, Accessibility, API Reference, Related
     Components.

     THE ONE THING THIS PAGE HAS TO GET RIGHT, per this batch's own
     explicit instruction: reconfirm, against the CURRENT source, the
     fact established back in Batch 2B that Navbar renders its items
     slot twice. Verified directly in navbar.jte, not assumed to still
     be true: `${items}` is written once into the desktop <ul> (hidden
     below the md breakpoint) and a second time into the mobile menu's
     own <ul>, inside a real @template.ashlar.components.drawer(...) call.
     gg.jte.Content is just a writeTo(output) method with no internal
     cursor, so calling it twice is exactly as safe as once - but it
     does mean every nav-link (or other) call composed into items
     produces TWO separate DOM nodes on every page load, one per
     rendering. This is not a bug to route around; it's how a caller
     avoids ever composing the same link list a second time by hand.

     Verified against source before writing:
     - navbar.jte's own 5 @params and extensive doc comment.
     - CONFIRMED the mobile menu is a real, unmodified Drawer - "THE
       DRAWER IS BORROWED, NOT REBUILT" per the component's own comment -
       placement Left, so it inherits every bit of Drawer's own
       already-audited behavior (focus trap, Escape, backdrop, scroll
       lock) for free, with zero reimplementation. menuTitle exists only
       because Drawer's own title is required.
     - CONFIRMED the responsive switch is a plain Tailwind VIEWPORT
       breakpoint (`hidden md:block` / `md:hidden`), not a container
       query - distinct from Stepper's own @container-based switch
       audited in Batch 8. This is a real, source-confirmed difference
       worth stating precisely rather than assuming container queries
       are used everywhere in this library.
     - CONFIRMED `actions` is NOT duplicated - it stays in the bar at
       every width rather than moving into the drawer, specifically so a
       control like Theme Toggle never has two live, potentially
       disagreeing copies of itself on the page at once.
     - CONFIRMED Navbar has no JavaScript file of its own at all - its
       only interactive behavior is opening the borrowed Drawer, which is
       Button's and Drawer's own JS, already loaded by those components'
       own calls.
     - Confirmed `aria-current="page"` is only ever generated when the
       caller explicitly passes `current = true` to a composed nav-link -
       Navbar has no idea what the current URL is, so this is never
       inferred automatically. --%>
!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.components.navbar(\n"
        + "    brand = @" + backtick + "<a href=\"/\">Acme</a>" + backtick + ",\n"
        + "    items = @" + backtick + "\n"
        + "        @template.ashlar.components.nav-link(label = \"Dashboard\", href = \"/dashboard\", current = true)\n"
        + "        @template.ashlar.components.nav-link(label = \"Settings\", href = \"/settings\")\n"
        + "    " + backtick + "\n"
        + ")";}
!{List<ApiParam> navbarApi = List.of(
    ApiParam.required("brand", "gg.jte.Content", "A real <a>, and the first thing written inside the header's inner div - some theme stylesheets key their masthead styling off this exact position."),
    ApiParam.required("items", "gg.jte.Content", "One or more nav-link(...) calls. Rendered TWICE - once into the desktop list, once into the mobile drawer's own list - see Composition."),
    ApiParam.optional("actions", "gg.jte.Content", "null", "Controls that stay visible at every width, never duplicated into the mobile drawer - a Theme Toggle is the common case."),
    ApiParam.optional("ariaLabel", "String", "\"Primary\"", "Names this <nav> landmark, distinct from Sidebar's own default (\"Sidebar\") - there is no single APG-assigned name for a generic top navbar."),
    ApiParam.optional("menuTitle", "String", "\"Menu\"", "The mobile drawer's required title - exists only because Drawer's own title param is required.")
);}
!{List<PageAnchor> sections = List.of(
    new PageAnchor("quick-start", "Quick Start"),
    new PageAnchor("how-to-use", "How to Use"),
    new PageAnchor("variants", "Variants & States"),
    new PageAnchor("composition", "Composition"),
    new PageAnchor("best-practices", "Best Practices"),
    new PageAnchor("accessibility", "Accessibility"),
    new PageAnchor("api-reference", "API Reference"),
    new PageAnchor("related", "Related Components")
);}

@template.layout.page(
contextPath = contextPath,
title = "Navbar",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "navbar", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A page's masthead: a brand, its primary destinations, and a trailing area for controls that should always stay visible. Below the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">md</code> breakpoint the link list collapses behind a menu button into a real Drawer - resize the window, or shrink your viewport below about 768px, to see it.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3 overflow-hidden rounded-lg border border-dashed border-slate-200 dark:border-slate-700">
                    @template.ashlar.components.navbar(
                    brand = @`<a href="#" class="text-lg font-semibold text-slate-900 dark:text-slate-100">Acme</a>`,
                    items = @`
                        @template.ashlar.components.nav-link(label = "Dashboard", href = "#", current = true)
                        @template.ashlar.components.nav-link(label = "Settings", href = "#")
                    `
                    )
                </div>
            `
            )
        </section>

        <section id="how-to-use" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "How to Use",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Render exactly one Navbar per page, near the top, with your primary site/app navigation in <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">items</code> and anything that must stay reachable regardless of screen width (a Theme Toggle, a compact search) in <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">actions</code>. If you need a persistent side-navigation column instead of (or alongside) a top bar, that's <a href="${contextPath}/components/sidebar" class="text-blue-700 hover:underline dark:text-blue-400">Sidebar</a>'s job - a genuinely different component with no built-in responsive collapse of its own, unlike this one.</p>
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
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">With trailing actions</p>
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
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Custom menu title</p>
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
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Two on one page stay independent</p>
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
            `
            )
        </section>

        <section id="composition" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Composition",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400"><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">items</code> is composed from <a href="${contextPath}/components/nav-link" class="text-blue-700 hover:underline dark:text-blue-400">NavLink</a> calls - see that page for its own full API. The one fact worth restating here precisely: Navbar renders that same <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">items</code> content twice, once into the desktop list and once more into the mobile drawer's own list, so each NavLink call you write produces two actual list items in the final page - this is not a mistake to guard against, it's exactly what lets you compose the link list once.</p>
                <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">The mobile menu is a real, unmodified <a href="${contextPath}/components/drawer" class="text-blue-700 hover:underline dark:text-blue-400">Drawer</a> - not a separate mobile-navigation component - so it inherits that component's focus trap, Escape handling, backdrop, and scroll lock with no reimplementation. <a href="${contextPath}/components/theme-toggle" class="text-blue-700 hover:underline dark:text-blue-400">Theme Toggle</a>'s own documented "most common home" is this component's <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">actions</code> slot, demonstrated above.</p>
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
                            <li>Put anything that must stay reachable regardless of screen width - a <a href="${contextPath}/components/theme-toggle" class="text-blue-700 hover:underline dark:text-blue-400">Theme Toggle</a>, a compact search - in <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">actions</code>, not <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">items</code>. <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">actions</code> is deliberately never duplicated into the mobile drawer, so a control placed there never ends up as two separate, potentially disagreeing copies of itself on the page at once.</li>
                            <li>Set <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">current = true</code> explicitly on whichever nav-link matches the page actually being viewed - Navbar has no idea what the current URL is, so this is never inferred automatically.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Avoid</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Assuming a stateful or non-idempotent component composed into <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">items</code> renders only once - it renders twice, once into the desktop list and once more into the mobile drawer's own list, since both are real DOM nodes built from the same composed content.</li>
                            <li>Reaching for Navbar when what's actually needed is a persistent side column - that's <a href="${contextPath}/components/sidebar" class="text-blue-700 hover:underline dark:text-blue-400">Sidebar</a>'s job, with no built-in responsive collapse of its own.</li>
                        </ul>
                    </div>
                </div>
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
                            <li>A real &lt;header&gt; and a named &lt;nav&gt; landmark, distinct from any other &lt;nav&gt; on the page via <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">ariaLabel</code>.</li>
                            <li>The mobile menu's full accessible dialog semantics, since it's a real Drawer - see that component's own Accessibility section.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">current = true</code> on whichever nav-link matches the page actually being viewed - Navbar has no idea what the current URL is, so this is never inferred automatically.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "API Reference",
            content = @`
                @template.layout.api-reference(apiParams = navbarApi)
            `
            )
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/nav-link" class="text-blue-700 hover:underline dark:text-blue-400">NavLink</a>, the entries composed into items &middot;
                    <a href="${contextPath}/components/drawer" class="text-blue-700 hover:underline dark:text-blue-400">Drawer</a>, the real component powering the mobile menu &middot;
                    <a href="${contextPath}/components/theme-toggle" class="text-blue-700 hover:underline dark:text-blue-400">Theme Toggle</a>, the usual content of actions &middot;
                    <a href="${contextPath}/components/sidebar" class="text-blue-700 hover:underline dark:text-blue-400">Sidebar</a> for persistent side navigation instead of a top bar
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "navbar", contextPath = contextPath)
    </div>
`
)
