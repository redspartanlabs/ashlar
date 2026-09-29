@import com.redspartanlabs.ashlar.icon.Icon

@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Dropdown Menu",
content = @`
    @template.layout.component-nav(currentSlug = "dropdown-menu", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Dropdown Menu</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A compact set of actions or destinations exposed from a trigger - not Select, not Combobox, not a Modal. Fully keyboard-operable: focus the trigger below and try <kbd class="rounded border border-slate-300 bg-slate-100 px-1 text-xs dark:border-slate-600 dark:bg-slate-800">Enter</kbd>, <kbd class="rounded border border-slate-300 bg-slate-100 px-1 text-xs dark:border-slate-600 dark:bg-slate-800">&darr;</kbd>, <kbd class="rounded border border-slate-300 bg-slate-100 px-1 text-xs dark:border-slate-600 dark:bg-slate-800">&uarr;</kbd>, <kbd class="rounded border border-slate-300 bg-slate-100 px-1 text-xs dark:border-slate-600 dark:bg-slate-800">Home</kbd>, <kbd class="rounded border border-slate-300 bg-slate-100 px-1 text-xs dark:border-slate-600 dark:bg-slate-800">End</kbd>, <kbd class="rounded border border-slate-300 bg-slate-100 px-1 text-xs dark:border-slate-600 dark:bg-slate-800">Escape</kbd>, and <kbd class="rounded border border-slate-300 bg-slate-100 px-1 text-xs dark:border-slate-600 dark:bg-slate-800">Tab</kbd> once it's open.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic actions</h3>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Account-style navigation</h3>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Icon-enhanced trigger</h3>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Beneath a data table</h3>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Multiple instances stay independent</h3>
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
        </section>
`
)
