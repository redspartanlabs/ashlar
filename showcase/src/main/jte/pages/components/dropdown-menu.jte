@import dev.redspartanlabs.ashlar.icon.Icon
@import dev.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import dev.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

<%-- KNOWN LIMITATION, confirmed directly in dropdown-menu.js, not
     assumed: there is no viewport-edge correction and no portal at
     all - unlike Popover/Tooltip, which at least nudge within the
     viewport, this panel is plain position: absolute; left: 0 with no
     repositioning logic. The "Beneath a data table" example below
     deliberately uses a hand-rolled table with no scroll container
     (not the real Table component), specifically to avoid triggering
     this exact clipping - that choice is intentional, not something to
     "fix" into a real Table. --%>
!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.components.dropdown-menu(\n"
        + "    label = \"Actions\",\n"
        + "    items = @" + backtick + "\n"
        + "        @template.ashlar.components.dropdown-menu-item(label = \"Edit\", href = \"/edit\")\n"
        + "        @template.ashlar.components.dropdown-menu-item(label = \"Delete\", href = \"/delete\")\n"
        + "    " + backtick + "\n"
        + ")";}
!{String basicActionsCode = "@template.ashlar.components.dropdown-menu(\n"
        + "    label = \"Actions\",\n"
        + "    items = @" + backtick + "\n"
        + "        @template.ashlar.components.dropdown-menu-item(label = \"Edit\", href = \"/items/1/edit\")\n"
        + "        @template.ashlar.components.dropdown-menu-item(label = \"Duplicate\", href = \"/items/1/duplicate\")\n"
        + "        @template.ashlar.components.dropdown-menu-item(label = \"Archive\")\n"
        + "    " + backtick + "\n"
        + ")";}
!{String iconTriggerCode = "@template.ashlar.components.dropdown-menu(\n"
        + "    label = \"Settings\",\n"
        + "    icon = Icon.SETTINGS,\n"
        + "    items = @" + backtick + "\n"
        + "        @template.ashlar.components.dropdown-menu-item(label = \"General\", href = \"/settings/general\")\n"
        + "        @template.ashlar.components.dropdown-menu-item(label = \"Notifications\", href = \"/settings/notifications\")\n"
        + "        @template.ashlar.components.dropdown-menu-item(label = \"Security\", href = \"/settings/security\")\n"
        + "    " + backtick + "\n"
        + ")";}
!{String tableCode = "<table class=\"w-full text-left text-sm\">\n"
        + "    <thead>\n"
        + "        <tr>\n"
        + "            <th>Record</th>\n"
        + "            <th><span class=\"sr-only\">Actions</span></th>\n"
        + "        </tr>\n"
        + "    </thead>\n"
        + "    <tbody>\n"
        + "        <tr>\n"
        + "            <td>Acme Manufacturing</td>\n"
        + "            <td class=\"text-right\">\n"
        + "                @template.ashlar.components.dropdown-menu(\n"
        + "                label = \"Actions\",\n"
        + "                items = @" + backtick + "\n"
        + "                    @template.ashlar.components.dropdown-menu-item(label = \"View\", href = \"/accounts/acme-manufacturing\")\n"
        + "                    @template.ashlar.components.dropdown-menu-item(label = \"Edit\", href = \"/accounts/acme-manufacturing/edit\")\n"
        + "                    @template.ashlar.components.dropdown-menu-item(label = \"Deactivate\", href = \"/accounts/acme-manufacturing/deactivate\")\n"
        + "                " + backtick + "\n"
        + "                )\n"
        + "            </td>\n"
        + "        </tr>\n"
        + "        <!-- One menu per row; each subsequent row repeats this pattern with its own account's href values. -->\n"
        + "    </tbody>\n"
        + "</table>";}
!{List<ApiParam> dropdownMenuApi = List.of(
    ApiParam.required("label", "String", "The trigger button's visible text - its accessible name always comes from this, never from icon alone."),
    ApiParam.required("items", "gg.jte.Content", "One or more dropdown-menu-item(...) calls."),
    ApiParam.optional("icon", "Icon", "null", "Optional leading icon on the trigger. Always decorative.")
);}
!{List<ApiParam> dropdownMenuItemApi = List.of(
    ApiParam.required("label", "String", "The item's visible text."),
    ApiParam.optional("href", "String", "null", "Renders a real <a role=\"menuitem\">. Omitted entirely, this item renders as non-interactive, aria-disabled text instead - there is no separate disabled flag.")
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
title = "Dropdown Menu",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "dropdown-menu", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A compact set of actions or destinations exposed from a trigger - not Select, not Combobox, not a Modal. Fully keyboard-operable: focus the trigger below and try <kbd class="rounded border border-slate-300 bg-slate-100 px-1 text-xs dark:border-slate-600 dark:bg-slate-800">Enter</kbd>, <kbd class="rounded border border-slate-300 bg-slate-100 px-1 text-xs dark:border-slate-600 dark:bg-slate-800">&darr;</kbd>, <kbd class="rounded border border-slate-300 bg-slate-100 px-1 text-xs dark:border-slate-600 dark:bg-slate-800">&uarr;</kbd>, <kbd class="rounded border border-slate-300 bg-slate-100 px-1 text-xs dark:border-slate-600 dark:bg-slate-800">Home</kbd>, <kbd class="rounded border border-slate-300 bg-slate-100 px-1 text-xs dark:border-slate-600 dark:bg-slate-800">End</kbd>, <kbd class="rounded border border-slate-300 bg-slate-100 px-1 text-xs dark:border-slate-600 dark:bg-slate-800">Escape</kbd>, and <kbd class="rounded border border-slate-300 bg-slate-100 px-1 text-xs dark:border-slate-600 dark:bg-slate-800">Tab</kbd> once it's open.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3">
                    @template.ashlar.components.dropdown-menu(
                    label = "Actions",
                    items = @`
                        @template.ashlar.components.dropdown-menu-item(label = "Edit", href = "/edit")
                        @template.ashlar.components.dropdown-menu-item(label = "Delete", href = "/delete")
                    `
                    )
                </div>
            `
            )
        </section>

        <section id="how-to-use" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "How to Use",
            content = @`<p class="text-sm text-slate-500 dark:text-slate-400">Reach for Dropdown Menu for a short list of actions or links exposed from one trigger - a row's "Actions" menu, an account menu. There is no submenu support, so a deeply nested menu structure isn't something to reach for here at all. For choosing a value that a form submits, use <a href="${contextPath}/components/select" class="text-blue-700 hover:underline dark:text-blue-400">Select</a> or <a href="${contextPath}/components/combobox" class="text-blue-700 hover:underline dark:text-blue-400">Combobox</a> instead - this component's items are actions/destinations, not form values.</p>`
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Basic actions</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Archive has no <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">href</code>, so it renders disabled rather than as a dead link.</p>
                        @template.layout.example(
                        code = basicActionsCode,
                        content = @`
                            <div class="mt-3">
                                @template.ashlar.components.dropdown-menu(
                                label = "Actions",
                                items = @`
                                    @template.ashlar.components.dropdown-menu-item(label = "Edit", href = "/items/1/edit")
                                    @template.ashlar.components.dropdown-menu-item(label = "Duplicate", href = "/items/1/duplicate")
                                    @template.ashlar.components.dropdown-menu-item(label = "Archive")
                                `
                                )
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Account-style navigation</p>
                        <div class="mt-3">
                            @template.ashlar.components.dropdown-menu(
                            label = "Account",
                            items = @`
                                @template.ashlar.components.dropdown-menu-item(label = "Profile", href = "/account/profile")
                                @template.ashlar.components.dropdown-menu-item(label = "Settings", href = "/account/settings")
                                @template.ashlar.components.dropdown-menu-item(label = "Sign out", href = "/sign-out")
                            `
                            )
                        </div>
                    </div>

                    @template.layout.example(
                    label = "Icon-enhanced trigger",
                    code = iconTriggerCode,
                    content = @`
                        <div class="mt-3">
                            @template.ashlar.components.dropdown-menu(
                            label = "Settings",
                            icon = Icon.SETTINGS,
                            items = @`
                                @template.ashlar.components.dropdown-menu-item(label = "General", href = "/settings/general")
                                @template.ashlar.components.dropdown-menu-item(label = "Notifications", href = "/settings/notifications")
                                @template.ashlar.components.dropdown-menu-item(label = "Security", href = "/settings/security")
                            `
                            )
                        </div>
                    `
                    )

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Beneath a data table</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Each row gets its own independent menu instance. Links here are static demonstration hrefs - nothing here mutates the table.</p>
                        <%-- Two columns, not three: this table is hand-rolled markup
                             for the showcase (not table.jte), so it has no scroll
                             container of its own - and it can't be given one, because
                             the ancestor that would need it also contains this menu's
                             absolutely-positioned panel, which any non-visible
                             overflow on that ancestor would clip. A Status column
                             was here too, but its own minimum content width was
                             exactly what pushed the row past a narrow viewport -
                             dropping it (not essential to demonstrating a menu inside
                             a table row) fits the remaining content without ever
                             touching overflow at all. --%>
                        @template.layout.example(
                        code = tableCode,
                        content = @`
                            <div class="mt-3 rounded-xl border border-slate-200 dark:border-slate-700">
                                <table class="w-full text-left text-sm">
                                    <thead class="border-b border-slate-200 bg-slate-50 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:border-slate-700 dark:bg-slate-800/50 dark:text-slate-400">
                                        <tr>
                                            <th class="px-4 py-2.5">Record</th>
                                            <th class="px-4 py-2.5"><span class="sr-only">Actions</span></th>
                                        </tr>
                                    </thead>
                                    <tbody class="divide-y divide-slate-200 text-slate-700 dark:divide-slate-700 dark:text-slate-300">
                                        <tr>
                                            <td class="px-4 py-2.5">Acme Manufacturing</td>
                                            <td class="px-4 py-2.5 text-right">
                                                @template.ashlar.components.dropdown-menu(
                                                label = "Actions",
                                                items = @`
                                                    @template.ashlar.components.dropdown-menu-item(label = "View", href = "/accounts/acme-manufacturing")
                                                    @template.ashlar.components.dropdown-menu-item(label = "Edit", href = "/accounts/acme-manufacturing/edit")
                                                    @template.ashlar.components.dropdown-menu-item(label = "Deactivate", href = "/accounts/acme-manufacturing/deactivate")
                                                `
                                                )
                                            </td>
                                        </tr>
                                        <tr>
                                            <td class="px-4 py-2.5">Kestrel Aerospace</td>
                                            <td class="px-4 py-2.5 text-right">
                                                @template.ashlar.components.dropdown-menu(
                                                label = "Actions",
                                                items = @`
                                                    @template.ashlar.components.dropdown-menu-item(label = "View", href = "/accounts/kestrel-aerospace")
                                                    @template.ashlar.components.dropdown-menu-item(label = "Edit", href = "/accounts/kestrel-aerospace/edit")
                                                    @template.ashlar.components.dropdown-menu-item(label = "Deactivate", href = "/accounts/kestrel-aerospace/deactivate")
                                                `
                                                )
                                            </td>
                                        </tr>
                                        <tr>
                                            <td class="px-4 py-2.5">Bramble Logistics</td>
                                            <td class="px-4 py-2.5 text-right">
                                                @template.ashlar.components.dropdown-menu(
                                                label = "Actions",
                                                items = @`
                                                    @template.ashlar.components.dropdown-menu-item(label = "View", href = "/accounts/bramble-logistics")
                                                    @template.ashlar.components.dropdown-menu-item(label = "Edit", href = "/accounts/bramble-logistics/edit")
                                                    @template.ashlar.components.dropdown-menu-item(label = "Deactivate", href = "/accounts/bramble-logistics/deactivate")
                                                `
                                                )
                                            </td>
                                        </tr>
                                    </tbody>
                                </table>
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Multiple instances stay independent</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">No shared JavaScript state - opening one never affects the others.</p>
                        <div class="mt-3 flex flex-wrap gap-3">
                            @template.ashlar.components.dropdown-menu(
                            label = "Actions",
                            items = @`
                                @template.ashlar.components.dropdown-menu-item(label = "Edit", href = "?item=1&action=edit")
                                @template.ashlar.components.dropdown-menu-item(label = "Delete", href = "?item=1&action=delete")
                            `
                            )
                            @template.ashlar.components.dropdown-menu(
                            label = "Settings",
                            items = @`
                                @template.ashlar.components.dropdown-menu-item(label = "General", href = "?panel=general")
                                @template.ashlar.components.dropdown-menu-item(label = "Billing", href = "?panel=billing")
                            `
                            )
                            @template.ashlar.components.dropdown-menu(
                            label = "Account",
                            items = @`
                                @template.ashlar.components.dropdown-menu-item(label = "Profile", href = "?panel=profile")
                                @template.ashlar.components.dropdown-menu-item(label = "Sign out", href = "/sign-out")
                            `
                            )
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
                <p class="text-sm text-slate-500 dark:text-slate-400">Dropdown Menu is the wrapper half of a wrapper/item pair, the same split Accordion and Breadcrumbs use. <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">dropdown-menu-item</code> is not a separate catalog component; it only ever exists composed into the menu's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">items</code> slot, documented here in the table below. There is no submenu slot on an item - a flat list of actions is the entire supported shape.</p>
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
                            <li>Keep the item list flat and short - a row's "Actions" menu, an account menu. There is no submenu support, so a deeply nested menu structure isn't something to design toward here.</li>
                            <li>Omit <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">href</code> on purpose for an action that isn't currently available, the way "Archive" does above - it renders as non-interactive text instead of a dead link.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Avoid</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Placing a trigger inside any <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">overflow-hidden</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">overflow-x-auto</code> ancestor - including a real <a href="${contextPath}/components/table" class="text-blue-700 hover:underline dark:text-blue-400">Table</a>'s own horizontal scroller - without first confirming the panel has room; there is no viewport-edge correction or portal to rescue it. See Pitfalls.</li>
                            <li>Reaching for this component to choose a form value - that's <a href="${contextPath}/components/select" class="text-blue-700 hover:underline dark:text-blue-400">Select</a> or <a href="${contextPath}/components/combobox" class="text-blue-700 hover:underline dark:text-blue-400">Combobox</a>'s job.</li>
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
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">The open menu has no viewport-edge correction at all.</strong> Unlike Popover and Tooltip, which at least nudge back inside the viewport, Dropdown Menu's panel is plain <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">position: absolute; left: 0</code> with no repositioning logic - it will clip against the viewport edge with no correction, and is clipped the same way by any <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">overflow-hidden</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">overflow-x-auto</code> ancestor, such as Table's own horizontal scroller. The "Beneath a data table" example above works around this deliberately, by using a hand-rolled table with no scroll container rather than the real Table component - see that example's own comment in source.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">There is no submenu support.</strong> Every item is a flat, single-level entry - nesting one dropdown-menu-item inside another isn't a supported structure.</li>
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
                            <li>The real WAI-ARIA APG menu pattern: <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="menu"</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="menuitem"</code>, <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-haspopup="menu"</code>, and <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-expanded</code> on the trigger.</li>
                            <li>Real roving focus onto the items themselves (not just <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-activedescendant</code>), since a link item's native Enter-activation needs it actually focused.</li>
                            <li>Escape that stops its own propagation after closing, so it never also closes an enclosing Modal/Drawer/Popover in the same keypress.</li>
                            <li>Focus returned to the trigger after Escape or activating an item - but deliberately not after an outside click, where the user is already elsewhere on purpose.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>A layout that keeps the menu clear of any clipping ancestor - see Pitfalls.</li>
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
                <div class="flex flex-col gap-6">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">dropdown-menu</p>
                        @template.layout.api-reference(apiParams = dropdownMenuApi)
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">dropdown-menu-item</p>
                        @template.layout.api-reference(apiParams = dropdownMenuItemApi)
                    </div>
                </div>
            `
            )
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/popover" class="text-blue-700 hover:underline dark:text-blue-400">Popover</a> for richer, composed content instead of a flat action list &middot;
                    <a href="${contextPath}/components/select" class="text-blue-700 hover:underline dark:text-blue-400">Select</a> for choosing a form value rather than triggering an action &middot;
                    <a href="${contextPath}/components/table" class="text-blue-700 hover:underline dark:text-blue-400">Table</a>, whose own scroll wrapper is the source of the clipping limitation above
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "dropdown-menu", contextPath = contextPath)
    </div>
`
)
