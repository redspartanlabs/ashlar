@import com.redspartanlabs.ashlar.icon.Icon

@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Tooltip",
content = @`
    @template.layout.component-nav(currentSlug = "tooltip", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Tooltip</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A short contextual explanation associated with a trigger - not a Popover, not a Menu. Appears on hover and keyboard focus. Button and Icon below are completely unmodified; Tooltip wraps them without either knowing it exists.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic button</h3>
                    <div class="mt-3">
                        @template.ashlar.components.tooltip(
                        content = "Save your changes",
                        trigger = @`
                            @template.ashlar.components.button(text = "Save", type = "Primary")
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Placement</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400"><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">placement</code> takes Top (the default), Right, Bottom, or Left. Hover or tab to each button below.</p>
                    <%-- Centred with generous gaps purely so each bubble has
                         empty space to open into - a tooltip is an overlay and
                         will happily cover a neighbour, which makes a tightly
                         packed row useless for actually seeing what each
                         placement does. --%>
                    <div class="mt-4 flex flex-col items-center gap-14 py-10">
                        @template.ashlar.components.tooltip(
                        content = "Placed above",
                        placement = "Top",
                        trigger = @`@template.ashlar.components.button(text = "Top", type = "Secondary")`
                        )
                        @template.ashlar.components.tooltip(
                        content = "Placed to the right",
                        placement = "Right",
                        trigger = @`@template.ashlar.components.button(text = "Right", type = "Secondary")`
                        )
                        @template.ashlar.components.tooltip(
                        content = "Placed below",
                        placement = "Bottom",
                        trigger = @`@template.ashlar.components.button(text = "Bottom", type = "Secondary")`
                        )
                        @template.ashlar.components.tooltip(
                        content = "Placed to the left",
                        placement = "Left",
                        trigger = @`@template.ashlar.components.button(text = "Left", type = "Secondary")`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Status indicator (non-interactive trigger)</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">This icon isn't a button or link - Tooltip makes it keyboard-reachable on its own so the explanation isn't hover-only.</p>
                    <div class="mt-3 flex items-center gap-2 text-sm text-slate-700 dark:text-slate-300">
                        <span>Sync status</span>
                        @template.ashlar.components.tooltip(
                        content = "Last synced 3 minutes ago",
                        trigger = @`
                            <span class="text-slate-400 dark:text-slate-500">
                                @template.ashlar.components.icon(icon = Icon.HELP_CIRCLE, classes = "h-4 w-4")
                            </span>
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Link</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Tooltip isn't tied to Button - any trigger works.</p>
                    <div class="mt-3 text-sm">
                        @template.ashlar.components.tooltip(
                        content = "Opens in the billing portal",
                        trigger = @`<a href="#" class="font-medium text-blue-600 hover:text-blue-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 rounded-sm dark:text-blue-400 dark:hover:text-blue-300">Manage subscription</a>`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Keyboard focus</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Tab to the button below - the tooltip should appear on focus, the same as it does on hover, and disappear when focus moves away.</p>
                    <div class="mt-3">
                        @template.ashlar.components.tooltip(
                        content = "Permanently deletes this record",
                        trigger = @`
                            @template.ashlar.components.button(text = "Delete", type = "Danger")
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Long text</h3>
                    <div class="mt-3">
                        @template.ashlar.components.tooltip(
                        content = "This action cannot be undone and will also remove any associated attachments, comments, and revision history.",
                        trigger = @`
                            @template.ashlar.components.button(text = "Archive", type = "Secondary")
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Near an edge</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A trigger at the right edge of its container - the tooltip shifts to stay on screen instead of clipping.</p>
                    <div class="mt-3 flex justify-end">
                        @template.ashlar.components.tooltip(
                        content = "More actions for this record",
                        trigger = @`
                            @template.ashlar.components.button(text = "More", type = "Secondary", icon = Icon.MORE_HORIZONTAL, iconOnly = true)
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Multiple instances stay independent</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">No shared state - hovering one never affects the others.</p>
                    <div class="mt-3 flex gap-2">
                        @template.ashlar.components.tooltip(
                        content = "Save",
                        trigger = @`@template.ashlar.components.button(text = "Save", icon = Icon.SAVE, iconOnly = true, type = "Ghost")`
                        )
                        @template.ashlar.components.tooltip(
                        content = "Edit",
                        trigger = @`@template.ashlar.components.button(text = "Edit", icon = Icon.EDIT, iconOnly = true, type = "Ghost")`
                        )
                        @template.ashlar.components.tooltip(
                        content = "Delete",
                        trigger = @`@template.ashlar.components.button(text = "Delete", icon = Icon.DELETE, iconOnly = true, type = "Ghost")`
                        )
                        @template.ashlar.components.tooltip(
                        content = "Archive",
                        trigger = @`@template.ashlar.components.button(text = "Archive", icon = Icon.ARCHIVE, iconOnly = true, type = "Ghost")`
                        )
                    </div>
                </div>
            </div>
        </section>
`
)
