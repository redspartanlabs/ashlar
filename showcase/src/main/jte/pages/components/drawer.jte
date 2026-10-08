@import dev.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import dev.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

<%-- THE ONE THING THIS PAGE HAS TO GET RIGHT: Drawer is not just Modal
     with a different animation. It genuinely IS modal (role="dialog",
     aria-modal="true", a real backdrop, body scroll lock, a real focus
     trap) - but title is required here (Modal's is optional), Escape
     and the close button can't be turned off, and only Left/Right
     placements exist (a sheet-style top/bottom panel is a different
     component by DrawerPlacement's own doc comment, not a missing
     variant of this one).

     THE SAME ESCAPE-PROPAGATION GAP FOUND FOR POPOVER applies here too,
     confirmed independently: Drawer's own Escape listener is bound to
     document with no stopPropagation, identical in shape to Modal's
     and Popover's - a Drawer opened inside an open Modal (or vice
     versa) closes both on one Escape press. Documented in Pitfalls. --%>
!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.components.button(text = \"Open drawer\", attributes = \"data-drawer-open=\\\"exampleDrawer\\\"\")\n\n"
        + "@template.ashlar.components.drawer(\n"
        + "    id = \"exampleDrawer\",\n"
        + "    title = \"Account details\",\n"
        + "    body = @" + backtick + "<p>Drawer content.</p>" + backtick + "\n"
        + ")";}
!{String placementCode = "@template.ashlar.components.button(text = \"From the left\", type = \"Secondary\", attributes = \"data-drawer-open=\\\"leftDrawer\\\"\")\n"
        + "@template.ashlar.components.button(text = \"From the right\", type = \"Secondary\", attributes = \"data-drawer-open=\\\"basicDrawer\\\"\")\n\n"
        + "@template.ashlar.components.drawer(\n"
        + "    id = \"leftDrawer\",\n"
        + "    title = \"Navigation\",\n"
        + "    placement = \"Left\",\n"
        + "    body = @" + backtick + "\n"
        + "        <nav class=\"flex flex-col gap-1\">\n"
        + "            <a href=\"#\">Dashboard</a>\n"
        + "            <a href=\"#\">Accounts</a>\n"
        + "            <a href=\"#\">Reports</a>\n"
        + "            <a href=\"#\">Settings</a>\n"
        + "        </nav>\n"
        + "    " + backtick + "\n"
        + ")";}
!{String interactiveFooterCode = "@template.ashlar.components.button(text = \"Edit account\", type = \"Primary\", attributes = \"data-drawer-open=\\\"formDrawer\\\"\")\n\n"
        + "@template.ashlar.components.drawer(\n"
        + "    id = \"formDrawer\",\n"
        + "    title = \"Edit account\",\n"
        + "    body = @" + backtick + "\n"
        + "        @template.ashlar.components.text-input(name = \"account-name\", label = \"Account name\", value = \"Acme Manufacturing\")\n"
        + "        @template.ashlar.components.text-input(name = \"account-owner\", label = \"Owner email\", type = \"email\", value = \"dana.whitfield@acme.example\")\n"
        + "        @template.ashlar.components.checkbox(name = \"account-notify\", label = \"Email me about plan changes\", checked = true)\n"
        + "    " + backtick + ",\n"
        + "    footer = @" + backtick + "\n"
        + "        @template.ashlar.components.button(text = \"Cancel\", type = \"Secondary\", attributes = \"data-drawer-close=\\\"formDrawer\\\"\")\n"
        + "        @template.ashlar.components.button(text = \"Save changes\", type = \"Primary\")\n"
        + "    " + backtick + "\n"
        + ")";}
!{String severalTriggersCode = "@template.ashlar.components.button(text = \"Open from here\", type = \"Ghost\", attributes = \"data-drawer-open=\\\"basicDrawer\\\"\")\n"
        + "@template.ashlar.components.button(text = \"Or from here\", type = \"Ghost\", attributes = \"data-drawer-open=\\\"basicDrawer\\\"\")";}
!{List<ApiParam> drawerApi = List.of(
    ApiParam.required("id", "String", "Matched by data-drawer-open/data-drawer-close attributes elsewhere on the page - any number of triggers can share one id."),
    ApiParam.required("title", "String", "Required, unlike Modal's optional title - a dialog must have an accessible name, and this is where it comes from (aria-labelledby)."),
    ApiParam.optional("placement", "String", "\"Right\"", "Left or Right only - there is no Top/Bottom; a sheet-style panel is a deliberately different component."),
    ApiParam.optional("body", "gg.jte.Content", "null", "The scrollable main content. The header and footer stay fixed while only this region scrolls."),
    ApiParam.optional("footer", "gg.jte.Content", "null", "Composed actions, typically Button calls - not a Drawer-specific action API.")
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
title = "Drawer",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "drawer", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A full-height panel anchored to the left or right edge, for navigation, settings, filters, or a form that needs more room than a Popover. It is modal: the page behind it is covered, its scroll is locked, and Tab stays inside until you close it with the close button, Escape, or the backdrop.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3">
                    @template.ashlar.components.button(text = "Open drawer", type = "Secondary", attributes = "data-drawer-open=\"quickStartDrawer\"")
                </div>
            `
            )
        </section>

        <section id="how-to-use" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "How to Use",
            content = @`<p class="text-sm text-slate-500 dark:text-slate-400">Reach for Drawer for navigation, filters, or a form that genuinely needs to block the page and hold focus while it's open - the same modal commitment as <a href="${contextPath}/components/modal" class="text-blue-700 hover:underline dark:text-blue-400">Modal</a>, just anchored to an edge with room to scroll rather than centered. If the content doesn't need to block the page at all, <a href="${contextPath}/components/popover" class="text-blue-700 hover:underline dark:text-blue-400">Popover</a> is the non-modal alternative. Place every drawer instance once, anywhere on the page (this page places its own after the main section), and open it from any number of triggers carrying its id - see Composition.</p>`
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Basic</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Opens from the right by default. Close it with the header button, Escape, or by clicking the backdrop.</p>
                        <div class="mt-3">
                            @template.ashlar.components.button(text = "Open drawer", type = "Secondary", attributes = "data-drawer-open=\"basicDrawer\"")
                        </div>
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Placement</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400"><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">placement</code> takes Right (the default) or Left.</p>
                        @template.layout.example(
                        code = placementCode,
                        content = @`
                            <div class="mt-3 flex flex-wrap gap-3">
                                @template.ashlar.components.button(text = "From the left", type = "Secondary", attributes = "data-drawer-open=\"leftDrawer\"")
                                @template.ashlar.components.button(text = "From the right", type = "Secondary", attributes = "data-drawer-open=\"basicDrawer\"")
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Interactive content and a footer</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Form controls inside are reachable with Tab and operating them does not dismiss the drawer. What Save actually does is the application's business - this component only owns opening, closing and focus.</p>
                        @template.layout.example(
                        code = interactiveFooterCode,
                        content = @`
                            <div class="mt-3">
                                @template.ashlar.components.button(text = "Edit account", type = "Primary", attributes = "data-drawer-open=\"formDrawer\"")
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Long content scrolls</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The header and footer stay put while the body scrolls, and the page behind stays where it was.</p>
                        <div class="mt-3">
                            @template.ashlar.components.button(text = "Open long drawer", type = "Secondary", attributes = "data-drawer-open=\"longDrawer\"")
                        </div>
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Several triggers, one drawer</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Any element carrying <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">data-drawer-open</code> with the drawer's id opens it, and focus returns to whichever one was used.</p>
                        @template.layout.example(
                        code = severalTriggersCode,
                        content = @`
                            <div class="mt-3 flex flex-wrap items-center gap-3">
                                @template.ashlar.components.button(text = "Open from here", type = "Ghost", attributes = "data-drawer-open=\"basicDrawer\"")
                                @template.ashlar.components.button(text = "Or from here", type = "Ghost", attributes = "data-drawer-open=\"basicDrawer\"")
                            </div>
                        `
                        )
                    </div>
                </div>
            `
            )
        </section>

        <section id="composition" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Composition",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Drawer uses the same trigger contract as Modal, not the wrapper/item or single-slot patterns the rest of the library uses: the drawer itself is rendered once, anywhere on the page, and any element elsewhere carrying <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">data-drawer-open="{id}"</code> opens it - demonstrated in "Several triggers, one drawer" above, where two independent buttons both open the same instance. <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">body</code> and <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">footer</code> are ordinary composition slots, the same shape as Card's - Text Input, Checkbox, and Button all compose in normally, shown in "Interactive content and a footer" above.</p>
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
                            <li>Render each Drawer instance once on the page and open it from any number of triggers carrying its id - never duplicate the drawer's own markup per trigger.</li>
                            <li>Reach for Drawer only when the content genuinely needs to block the page and hold focus, the same commitment as Modal - use <a href="${contextPath}/components/popover" class="text-blue-700 hover:underline dark:text-blue-400">Popover</a> instead when it doesn't.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Avoid</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Nesting a Drawer inside an open <a href="${contextPath}/components/modal" class="text-blue-700 hover:underline dark:text-blue-400">Modal</a> (or vice versa) - one Escape press closes both; see Pitfalls.</li>
                            <li>Expecting a Top/Bottom placement - only Left and Right exist, by design.</li>
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
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">A Drawer opened inside an open Modal closes both of them on one Escape press.</strong> Drawer's Escape listener (like Modal's and Popover's) is bound to the whole document and doesn't stop propagation, so nothing prevents an Escape meant for one from also reaching the other's own listener on the same keypress - the same architectural gap documented on Popover's page.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">Escape and the close button can't be turned off.</strong> Unlike Modal, there is no parameter for either - both are hard-coded on, since a keyboard user inside a focus-trapped Drawer always needs a way out.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">There is no Top/Bottom placement.</strong> Only Left and Right exist; a sheet-style panel sliding from the top or bottom is a different component with a different sizing and gesture model, not a missing value here.</li>
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
                            <li>Genuine modal semantics matching the behavior: <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="dialog"</code>, <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-modal="true"</code>, and <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-labelledby</code> pointing at the required title.</li>
                            <li>A real focus trap and body scroll lock while open, and both released cleanly on close regardless of how many drawers have opened and closed in between.</li>
                            <li>Focus returned to the exact trigger that opened it, immediately on close rather than waiting for the exit animation.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Awareness of the Escape-propagation limitation above if you ever nest a Drawer inside a Modal, or vice versa.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.layout.api-reference(apiParams = drawerApi)
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/modal" class="text-blue-700 hover:underline dark:text-blue-400">Modal</a>, the same modal commitment, centered rather than edge-anchored &middot;
                    <a href="${contextPath}/components/popover" class="text-blue-700 hover:underline dark:text-blue-400">Popover</a> for content that doesn't need to block the page
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "drawer", contextPath = contextPath)
    </div>

    @template.ashlar.components.drawer(
    id = "quickStartDrawer",
    title = "Account details",
    body = @`<p>Drawer content.</p>`
    )

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
