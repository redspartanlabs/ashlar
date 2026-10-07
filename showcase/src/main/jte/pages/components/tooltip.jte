@import com.redspartanlabs.ashlar.icon.Icon
@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import com.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.components.tooltip(\n"
        + "    content = \"Save your changes\",\n"
        + "    trigger = @" + backtick + "@template.ashlar.components.button(text = \"Save\", type = \"Primary\")" + backtick + "\n"
        + ")";}
!{String placementCode = "@template.ashlar.components.tooltip(\n"
        + "    content = \"Placed above\",\n"
        + "    placement = \"Top\",\n"
        + "    trigger = @" + backtick + "@template.ashlar.components.button(text = \"Top\", type = \"Secondary\")" + backtick + "\n"
        + ")\n"
        + "@template.ashlar.components.tooltip(\n"
        + "    content = \"Placed to the right\",\n"
        + "    placement = \"Right\",\n"
        + "    trigger = @" + backtick + "@template.ashlar.components.button(text = \"Right\", type = \"Secondary\")" + backtick + "\n"
        + ")\n"
        + "@template.ashlar.components.tooltip(\n"
        + "    content = \"Placed below\",\n"
        + "    placement = \"Bottom\",\n"
        + "    trigger = @" + backtick + "@template.ashlar.components.button(text = \"Bottom\", type = \"Secondary\")" + backtick + "\n"
        + ")\n"
        + "@template.ashlar.components.tooltip(\n"
        + "    content = \"Placed to the left\",\n"
        + "    placement = \"Left\",\n"
        + "    trigger = @" + backtick + "@template.ashlar.components.button(text = \"Left\", type = \"Secondary\")" + backtick + "\n"
        + ")";}
!{String statusIndicatorCode = "@template.ashlar.components.tooltip(\n"
        + "    content = \"Last synced 3 minutes ago\",\n"
        + "    trigger = @" + backtick + "\n"
        + "        <span class=\"text-slate-400 dark:text-slate-500\">\n"
        + "            @template.ashlar.components.icon(icon = Icon.HELP_CIRCLE, classes = \"h-4 w-4\")\n"
        + "        </span>\n"
        + "    " + backtick + "\n"
        + ")";}
!{String linkCode = "@template.ashlar.components.tooltip(\n"
        + "    content = \"Opens in the billing portal\",\n"
        + "    trigger = @" + backtick + "<a href=\"#\" class=\"font-medium text-blue-600 hover:text-blue-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 rounded-sm dark:text-blue-400 dark:hover:text-blue-300\">Manage subscription</a>" + backtick + "\n"
        + ")";}
!{List<ApiParam> tooltipApi = List.of(
    ApiParam.required("trigger", "gg.jte.Content", "The element the tooltip attaches to - left completely unmodified; Button, Icon, a link, anything works without knowing Tooltip exists."),
    ApiParam.required("content", "String", "Plain text, not gg.jte.Content - HTML-escaped automatically. Keeping this a String (not a content slot) is deliberate, so this can't be composed into a Popover."),
    ApiParam.optional("placement", "String", "\"Top\"", "Top/Right/Bottom/Left. Unrecognized values silently fall back to Top. Nudged back inside the viewport along one axis if clipped there, but never flipped to the opposite side - see Pitfalls.")
);}
!{List<PageAnchor> sections = List.of(
    new PageAnchor("quick-start", "Quick Start"),
    new PageAnchor("how-to-use", "How to Use"),
    new PageAnchor("variants", "Variants & States"),
    new PageAnchor("composition", "Composition"),
    new PageAnchor("pitfalls", "Pitfalls"),
    new PageAnchor("accessibility", "Accessibility"),
    new PageAnchor("api-reference", "API Reference"),
    new PageAnchor("related", "Related Components")
);}

@template.layout.page(
contextPath = contextPath,
title = "Tooltip",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "tooltip", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A short contextual explanation associated with a trigger - not a Popover, not a Menu. Appears on hover and keyboard focus. Button and Icon below are completely unmodified; Tooltip wraps them without either knowing it exists.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3">
                    @template.ashlar.components.tooltip(
                    content = "Save your changes",
                    trigger = @`@template.ashlar.components.button(text = "Save", type = "Primary")`
                    )
                </div>
            `
            )
        </section>

        <section id="how-to-use" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "How to Use",
            content = @`<p class="text-sm text-slate-500 dark:text-slate-400">Reach for Tooltip only for a short label-equivalent explanation of what a control means or does - the content is plain text by design, so the moment you want a form, a list of actions, or anything clickable inside the bubble, you want <a href="${contextPath}/components/popover" class="text-blue-700 hover:underline dark:text-blue-400">Popover</a> instead, not a richer Tooltip. For a menu of actions, use <a href="${contextPath}/components/dropdown-menu" class="text-blue-700 hover:underline dark:text-blue-400">Dropdown Menu</a>; for anything that needs to block the page or hold a real task, use <a href="${contextPath}/components/modal" class="text-blue-700 hover:underline dark:text-blue-400">Modal</a> or <a href="${contextPath}/components/drawer" class="text-blue-700 hover:underline dark:text-blue-400">Drawer</a>.</p>`
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Basic button</p>
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
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Placement</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400"><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">placement</code> takes Top (the default), Right, Bottom, or Left. Hover or tab to each button below.</p>
                        @template.layout.example(
                        code = placementCode,
                        content = @`
                            <div class="flex flex-col items-center gap-14 py-10">
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
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Status indicator (non-interactive trigger)</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">This icon isn't a button or link - Tooltip makes it keyboard-reachable on its own so the explanation isn't hover-only.</p>
                        @template.layout.example(
                        code = statusIndicatorCode,
                        content = @`
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
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Link</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Tooltip isn't tied to Button - any trigger works.</p>
                        @template.layout.example(
                        code = linkCode,
                        content = @`
                            <div class="mt-3 text-sm">
                                @template.ashlar.components.tooltip(
                                content = "Opens in the billing portal",
                                trigger = @`<a href="#" class="font-medium text-blue-600 hover:text-blue-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 rounded-sm dark:text-blue-400 dark:hover:text-blue-300">Manage subscription</a>`
                                )
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Keyboard focus</p>
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
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Long text</p>
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
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Near an edge</p>
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
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Multiple instances stay independent</p>
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
            `
            )
        </section>

        <section id="composition" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Composition",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Tooltip wraps exactly one piece of caller-provided <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">trigger</code> content without that content ever knowing it's wrapped - Button, Icon, a plain link, or arbitrary markup all work identically, since the association is built entirely in Tooltip's own template and script, never inside the trigger itself. This is a genuinely different shape from Accordion/List/Tree's wrapper/item pattern: those compose several of the library's own repeated items into one caller-authored sequence, where Tooltip pairs exactly one arbitrary thing with one short piece of text - a single content slot, not a list.</p>
            `
            )
        </section>

        <section id="pitfalls" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Pitfalls & Troubleshooting",
            content = @`
                <ul class="list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">A placement with no room never flips to the opposite side.</strong> Choosing Left for a trigger already against the left edge is treated as the caller's choice, not something this silently overrides - it will clip on that side rather than flip to Right. Pick the side that actually has room; Top (the default) covers most ordinary cases on its own.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">A tooltip inside an <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">overflow-hidden</code> ancestor is clipped by it</strong> - the same limitation already documented for Popover inside Table's own horizontal scroller. There is no portal here either.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">Content can only ever be plain text.</strong> There is no way to put a button, a link, or any composed component inside the bubble - that's deliberately Popover's job, not an oversight here.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">A tap on a non-focusable trigger doesn't show the tooltip at all.</strong> Touch is deliberately excluded from the hover path; the tooltip only becomes reachable on touch if the trigger is also natively focusable (a real button or link), since tapping that moves focus to it.</li>
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
                            <li>The WAI-ARIA tooltip pattern: <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="tooltip"</code> on the bubble, and <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-describedby</code> added to the trigger only while the bubble is actually visible - added, not overwritten, so it never clobbers a describedby the application already had there.</li>
                            <li>Keyboard reachability even for a trigger with nothing natively focusable inside it - the trigger slot itself becomes a Tab stop automatically.</li>
                            <li>Escape dismissal regardless of how the tooltip was shown, since a hover-shown tooltip may leave focus somewhere else entirely.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>A real, natively interactive trigger (a button or link) whenever you want the tooltip reachable without relying on the automatic tabindex fallback.</li>
                            <li>A placement with room to render in - see Pitfalls.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.layout.api-reference(apiParams = tooltipApi)
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/popover" class="text-blue-700 hover:underline dark:text-blue-400">Popover</a> for interactive content instead of plain text &middot;
                    <a href="${contextPath}/components/dropdown-menu" class="text-blue-700 hover:underline dark:text-blue-400">Dropdown Menu</a> for a list of actions &middot;
                    <a href="${contextPath}/components/button" class="text-blue-700 hover:underline dark:text-blue-400">Button</a>, the most common trigger
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "tooltip", contextPath = contextPath)
    </div>
`
)
