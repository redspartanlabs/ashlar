@import com.redspartanlabs.ashlar.icon.Icon

@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Popover",
content = @`
    @template.layout.component-nav(currentSlug = "popover", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Popover</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A surface opened by clicking a control, holding content you can actually read and operate - the counterpart to Tooltip, which is hover-revealed and text-only. Non-modal: it doesn't trap focus or cover the page the way Modal does. Click the trigger again, click outside, press Escape, or Tab past the end to dismiss.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic</h3>
                    <div class="mt-3">
                        @template.ashlar.components.popover(
                        title = "Storage limit",
                        trigger = @`@template.ashlar.components.button(text = "What counts as storage?", type = "Secondary")`,
                        content = @`<p>Uploaded files, exported reports, and attachments on records all count toward your plan's storage limit. Deleted items stop counting once the trash is emptied.</p>`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Interactive content</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Controls inside the surface are reachable with Tab and operating them does not dismiss it. The Popover owns opening and closing; what these controls actually do is the application's business.</p>
                    <div class="mt-3">
                        @template.ashlar.components.popover(
                        title = "Filter results",
                        trigger = @`@template.ashlar.components.button(text = "Filters", type = "Secondary", icon = Icon.FILTER)`,
                        content = @`
                            <div class="flex flex-col gap-2">
                                @template.ashlar.components.checkbox(name = "status-active", label = "Active", checked = true)
                                @template.ashlar.components.checkbox(name = "status-trial", label = "Trial")
                                @template.ashlar.components.checkbox(name = "status-churned", label = "Churned")
                            </div>
                            <div class="mt-4 flex justify-end gap-2">
                                @template.ashlar.components.button(text = "Reset", type = "Ghost", size = "Small")
                                @template.ashlar.components.button(text = "Apply", type = "Primary", size = "Small")
                            </div>
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Richer content</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Anything composable goes in - here a Badge and a link alongside text.</p>
                    <div class="mt-3">
                        @template.ashlar.components.popover(
                        title = "Acme Manufacturing",
                        trigger = @`@template.ashlar.components.button(text = "Account details", type = "Secondary")`,
                        content = @`
                            <div class="flex items-center gap-2">
                                @template.ashlar.components.badge(text = "Enterprise", variant = "Success")
                                <span class="text-xs text-slate-500 dark:text-slate-400">Since January 2024</span>
                            </div>
                            <p class="mt-2">Three seats in use of five. Billing renews on the first of each month.</p>
                            <a href="#" class="mt-2 inline-block rounded-sm font-medium text-blue-600 hover:text-blue-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 dark:text-blue-400 dark:hover:text-blue-300">View full account</a>
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Placement</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400"><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">placement</code> takes Bottom (the default), Top, Right, or Left.</p>
                    <%-- Spaced out so each surface has somewhere to open into: a
                         popover is an overlay and will cover its neighbours, which
                         makes a tight row useless for seeing what each side does. --%>
                    <div class="mt-4 flex flex-col items-center gap-40 py-16">
                        @template.ashlar.components.popover(
                        placement = "Top",
                        trigger = @`@template.ashlar.components.button(text = "Top", type = "Secondary")`,
                        content = @`<p>Opens above the trigger.</p>`
                        )
                        @template.ashlar.components.popover(
                        placement = "Right",
                        trigger = @`@template.ashlar.components.button(text = "Right", type = "Secondary")`,
                        content = @`<p>Opens to the right of the trigger.</p>`
                        )
                        @template.ashlar.components.popover(
                        placement = "Bottom",
                        trigger = @`@template.ashlar.components.button(text = "Bottom", type = "Secondary")`,
                        content = @`<p>Opens below the trigger - the default.</p>`
                        )
                        @template.ashlar.components.popover(
                        placement = "Left",
                        trigger = @`@template.ashlar.components.button(text = "Left", type = "Secondary")`,
                        content = @`<p>Opens to the left of the trigger.</p>`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Keyboard and dismissal</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Tab to the trigger and press Enter or Space to open. Tab again to move into the surface. Escape closes it from anywhere inside and returns focus to the trigger; tabbing past the last control closes it without pulling focus back.</p>
                    <div class="mt-3 flex items-center gap-3">
                        <span class="text-sm text-slate-500 dark:text-slate-400">Tab starts here &rarr;</span>
                        @template.ashlar.components.popover(
                        title = "Rename workspace",
                        trigger = @`@template.ashlar.components.button(text = "Rename", type = "Secondary")`,
                        content = @`
                            @template.ashlar.components.text-input(name = "workspace-name", label = "Workspace name", value = "Acme Manufacturing")
                            <div class="flex justify-end">
                                @template.ashlar.components.button(text = "Save", type = "Primary", size = "Small")
                            </div>
                        `
                        )
                        @template.ashlar.components.button(text = "After the popover", type = "Ghost")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Near an edge</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A trigger at the right edge of its container - the surface shifts along its centring axis to stay on screen rather than clipping.</p>
                    <div class="mt-3 flex justify-end">
                        @template.ashlar.components.popover(
                        title = "Row actions",
                        trigger = @`@template.ashlar.components.button(text = "More", type = "Secondary", icon = Icon.MORE_HORIZONTAL, iconOnly = true)`,
                        content = @`<p>Anchored at the far edge of its container, this surface is nudged back inside the viewport instead of running off it.</p>`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Multiple instances stay independent</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Each Popover owns its own state - no shared registry.</p>
                    <div class="mt-3 flex flex-wrap gap-3">
                        @template.ashlar.components.popover(
                        trigger = @`@template.ashlar.components.button(text = "First", type = "Secondary")`,
                        content = @`<p>The first popover's content.</p>`
                        )
                        @template.ashlar.components.popover(
                        trigger = @`@template.ashlar.components.button(text = "Second", type = "Secondary")`,
                        content = @`<p>The second popover's content.</p>`
                        )
                        @template.ashlar.components.popover(
                        trigger = @`@template.ashlar.components.button(text = "Third", type = "Secondary")`,
                        content = @`<p>The third popover's content.</p>`
                        )
                    </div>
                </div>
            </div>
        </section>
`
)
