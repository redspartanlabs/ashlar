@import com.redspartanlabs.ashlar.icon.Icon
@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param String contextPath

<%-- Batch 10 documentation page. Existing "Basic actions" / "Account-style
     navigation" / "Icon-enhanced trigger" / "Beneath a data table" /
     "Multiple instances stay independent" examples kept verbatim. The
     "Beneath a data table" example's own existing comment already
     correctly explains - and deliberately avoids triggering - the exact
     clipping limitation this batch investigates (a hand-rolled table with
     no overflow wrapper, specifically because the real Table component's
     own overflow-x-auto would clip this menu's absolutely-positioned
     panel). That comment is accurate and is NOT the Badge/Pagination kind
     of defect, so it is kept exactly as-is rather than "fixed" into a
     real Table - doing so would actually break the demonstration. Added:
     Quick Start, formal grammar headings, How to Use, Composition
     (dropdown-menu-item documented here), Accessibility, two API
     Reference tables, Related Components, Pitfalls.

     Verified against source before writing - independently, not inferred
     from Modal/Tooltip/Popover:
     - dropdown-menu.jte's own 3 @params and doc comment. CONFIRMED the
       real WAI-ARIA APG "menu" pattern (role="menu"/role="menuitem"),
       distinct from a list - a dropdown's items are actions exposed from
       one trigger, not navigable pages. No id param - DropdownMenuIds
       generates one, since trigger and menu are both rendered by this
       one call with nothing external to coordinate against.
     - dropdown-menu-item.jte's own 2 @params. CONFIRMED NO SUBMENU
       SUPPORT: there is no nesting mechanism of any kind. Confirmed
       "missing href means disabled, not a dead link" - there is no
       separate disabled boolean; omitting href is what disables an item.
       Confirmed every item carries tabindex="-1" unconditionally - roving
       tabindex puts exactly one item in the Tab order, and only while the
       menu is open, which @if alone can't express since it depends on
       runtime keyboard state.
     - dropdown-menu.js in full. CONFIRMED THE ESCAPE FIX IS PRESENT in
       the current implementation (per this batch's own explicit
       instruction not to assume otherwise): both Escape branches
       (trigger-focused and item-focused) call event.stopPropagation()
       after closing, with the exact comment explaining why - so an
       enclosing Modal/Drawer/Popover's own Escape listener never also
       fires on the same keypress. CONFIRMED mouse-click-to-open leaves
       focus on the trigger (matching a native <select>/OS menu), while
       keyboard-open (Enter/Space/ArrowDown/ArrowUp on the trigger) moves
       focus into the first/last item. CONFIRMED Tab closes the menu
       without trapping focus - real DOM focus moves onto the items
       themselves (not aria-activedescendant), because a link item's
       native Enter-activation depends on it actually being focused.
       CONFIRMED outside-click closes without returning focus to the
       trigger, while Escape and item-activation both do.
     - CONFIRMED NO PORTAL AND NO VIEWPORT-EDGE CORRECTION AT ALL: unlike
       Popover and Tooltip (both of which at least nudge within the
       viewport via a reposition() function), dropdown-menu.js has no
       positioning logic whatsoever - the panel is plain
       `position: absolute; left: 0` with nothing to correct it. This
       means Dropdown Menu is clipped by both the viewport AND any
       overflow-hidden/overflow-x-auto ancestor (e.g. Table's own
       scroller) with even less mitigation than Popover/Tooltip have -
       documented as a real, source-confirmed Pitfall below. --%>
!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.components.dropdown-menu(\n"
        + "    label = \"Actions\",\n"
        + "    items = @" + backtick + "\n"
        + "        @template.ashlar.components.dropdown-menu-item(label = \"Edit\", href = \"/edit\")\n"
        + "        @template.ashlar.components.dropdown-menu-item(label = \"Delete\", href = \"/delete\")\n"
        + "    " + backtick + "\n"
        + ")";}
!{List<ApiParam> dropdownMenuApi = List.of(
    ApiParam.required("label", "String", "The trigger button's visible text - its accessible name always comes from this, never from icon alone."),
    ApiParam.required("items", "gg.jte.Content", "One or more dropdown-menu-item(...) calls."),
    ApiParam.optional("icon", "Icon", "null", "Optional leading icon on the trigger. Always decorative.")
);}
!{List<ApiParam> dropdownMenuItemApi = List.of(
    ApiParam.required("label", "String", "The item's visible text."),
    ApiParam.optional("href", "String", "null", "Renders a real <a role=\"menuitem\">. Omitted entirely, this item renders as non-interactive, aria-disabled text instead - there is no separate disabled flag.")
);}

@template.layout.page(
contextPath = contextPath,
title = "Dropdown Menu",
content = @`
    @template.layout.component-nav(currentSlug = "dropdown-menu", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Dropdown Menu</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A compact set of actions or destinations exposed from a trigger - not Select, not Combobox, not a Modal. Fully keyboard-operable: focus the trigger below and try <kbd class="rounded border border-slate-300 bg-slate-100 px-1 text-xs dark:border-slate-600 dark:bg-slate-800">Enter</kbd>, <kbd class="rounded border border-slate-300 bg-slate-100 px-1 text-xs dark:border-slate-600 dark:bg-slate-800">&darr;</kbd>, <kbd class="rounded border border-slate-300 bg-slate-100 px-1 text-xs dark:border-slate-600 dark:bg-slate-800">&uarr;</kbd>, <kbd class="rounded border border-slate-300 bg-slate-100 px-1 text-xs dark:border-slate-600 dark:bg-slate-800">Home</kbd>, <kbd class="rounded border border-slate-300 bg-slate-100 px-1 text-xs dark:border-slate-600 dark:bg-slate-800">End</kbd>, <kbd class="rounded border border-slate-300 bg-slate-100 px-1 text-xs dark:border-slate-600 dark:bg-slate-800">Escape</kbd>, and <kbd class="rounded border border-slate-300 bg-slate-100 px-1 text-xs dark:border-slate-600 dark:bg-slate-800">Tab</kbd> once it's open.</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3">
                        @template.ashlar.components.dropdown-menu(
                        label = "Actions",
                        items = @`
                            @template.ashlar.components.dropdown-menu-item(label = "Edit", href = "/edit")
                            @template.ashlar.components.dropdown-menu-item(label = "Delete", href = "/delete")
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">How to Use</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Reach for Dropdown Menu for a short list of actions or links exposed from one trigger - a row's "Actions" menu, an account menu. There is no submenu support, so a deeply nested menu structure isn't something to reach for here at all. For choosing a value that a form submits, use <a href="${contextPath}/components/select" class="text-blue-700 hover:underline dark:text-blue-400">Select</a> or <a href="${contextPath}/components/combobox" class="text-blue-700 hover:underline dark:text-blue-400">Combobox</a> instead - this component's items are actions/destinations, not form values.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Basic actions</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Archive has no <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">href</code>, so it renders disabled rather than as a dead link.</p>
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

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Icon-enhanced trigger</p>
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
                        </div>

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
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composition</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Dropdown Menu is the wrapper half of a wrapper/item pair, the same split Accordion and Breadcrumbs use. <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">dropdown-menu-item</code> is not a separate catalog component; it only ever exists composed into the menu's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">items</code> slot, documented here in the table below. There is no submenu slot on an item - a flat list of actions is the entire supported shape.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Pitfalls &amp; Troubleshooting</h3>
                    <ul class="mt-2 list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">The open menu has no viewport-edge correction at all.</strong> Unlike Popover and Tooltip, which at least nudge back inside the viewport, Dropdown Menu's panel is plain <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">position: absolute; left: 0</code> with no repositioning logic - it will clip against the viewport edge with no correction, and is clipped the same way by any <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">overflow-hidden</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">overflow-x-auto</code> ancestor, such as Table's own horizontal scroller. The "Beneath a data table" example above works around this deliberately, by using a hand-rolled table with no scroll container rather than the real Table component - see that example's own comment in source.</li>
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">There is no submenu support.</strong> Every item is a flat, single-level entry - nesting one dropdown-menu-item inside another isn't a supported structure.</li>
                    </ul>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
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
                </div>

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

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/popover" class="text-blue-700 hover:underline dark:text-blue-400">Popover</a> for richer, composed content instead of a flat action list &middot;
                        <a href="${contextPath}/components/select" class="text-blue-700 hover:underline dark:text-blue-400">Select</a> for choosing a form value rather than triggering an action &middot;
                        <a href="${contextPath}/components/table" class="text-blue-700 hover:underline dark:text-blue-400">Table</a>, whose own scroll wrapper is the source of the clipping limitation above
                    </p>
                </div>
            </div>
        </section>
`
)
