@import com.redspartanlabs.ashlar.icon.Icon
@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param String contextPath

<%-- Batch 10 documentation page. Existing "Basic" / "Interactive content" /
     "Richer content" / "Placement" / "Keyboard and dismissal" / "Near an
     edge" / "Multiple instances stay independent" examples kept verbatim.
     Added: Quick Start, formal grammar headings, How to Use, Composition,
     Accessibility, API Reference, Related Components, Pitfalls.

     Verified against source before writing - independently, not inferred
     from Tooltip despite the similar positioning code:
     - popover.jte's own 4 @params and extensive doc comment. CONFIRMED
       `content` is real gg.jte.Content (unlike Tooltip's plain String) -
       the entire point of this component is that what's inside can be
       read, focused, and operated: links, buttons, a small form. CONFIRMED
       this deliberately renders NO role="dialog" and no role at all on
       the surface - it's the WAI-ARIA APG Disclosure pattern instead
       (aria-expanded + aria-controls on the trigger, no aria-haspopup,
       since that would claim menu/dialog/listbox semantics this isn't).
       Confirmed `title` is NOT wired to aria-labelledby - without a role
       there's nothing for a label to name, so it's just a visible,
       heading-navigable top line. Confirmed default placement is Bottom
       (not Tooltip's Top) - deliberately, because a popover is opened by
       an intentional click and is large enough to belong below the
       control, covering what follows rather than what was just read.
     - popover.js in full (203 lines). CONFIRMED real focus-into-surface
       behavior differs by trigger: clicking opens without moving focus
       (toggle() only flips classes/ARIA), but Tab naturally continues
       into the surface's own controls since nothing traps it. CONFIRMED
       non-modal: outside click and tabbing past the last control both
       close it; only Escape returns focus to the trigger - outside
       click/tab-away deliberately do NOT, since the user is already
       elsewhere on purpose. CONFIRMED open state is tracked as an
       internal `opened` flag, not read off the hidden class, specifically
       because the class lags the ~150ms fade-out, which had caused a
       real previously-encountered bug (a click during that window closed
       it a second time instead of reopening) - documented directly in
       the code's own comment.
     - A GENUINE FINDING, confirmed by direct comparison with Dropdown
       Menu's own Escape handling (audited independently just before
       this): Popover's Escape listener is bound to `document` directly,
       not to its own root element, and - unlike Dropdown Menu's - it
       does NOT call stopPropagation(). Confirmed Modal's and Drawer's
       own Escape listeners are ALSO bound to `document` with no
       stopPropagation. Because all three sit at the same target
       (document), stopPropagation on any of them couldn't stop the
       others anyway - sibling listeners on one element all fire
       regardless, only stopImmediatePropagation would prevent that, and
       none of the three use it. CONCLUSION: a Popover opened from inside
       an open Modal or Drawer, with Escape pressed, closes BOTH in the
       same keypress - a real, confirmed difference from Dropdown Menu
       (whose root-scoped listener genuinely can and does stop this).
       Reported in this batch's final report as a cross-component
       architecture finding, not fixed here. --%>
!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.components.popover(\n"
        + "    title = \"Storage limit\",\n"
        + "    trigger = @" + backtick + "@template.ashlar.components.button(text = \"What counts?\", type = \"Secondary\")" + backtick + ",\n"
        + "    content = @" + backtick + "<p>Files, exports, and attachments all count.</p>" + backtick + "\n"
        + ")";}
!{List<ApiParam> popoverApi = List.of(
    ApiParam.required("trigger", "gg.jte.Content", "The element that opens the popover on click. A real <button> is expected; anything else gets a synthetic role=\"button\" and tabindex=\"0\" added."),
    ApiParam.required("content", "gg.jte.Content", "Real, composed content - links, buttons, form controls all work and remain fully operable."),
    ApiParam.optional("title", "String", "\"\"", "A plain heading inside the surface. Not wired to aria-labelledby - there is no role on the surface for it to name."),
    ApiParam.optional("placement", "String", "\"Bottom\"", "Top/Right/Bottom/Left. Unrecognized values silently fall back to Bottom. Nudged within the viewport if clipped, but never flipped to the opposite side - see Pitfalls.")
);}

@template.layout.page(
contextPath = contextPath,
title = "Popover",
content = @`
    @template.layout.component-nav(currentSlug = "popover", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Popover</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A surface opened by clicking a control, holding content you can actually read and operate - the counterpart to Tooltip, which is hover-revealed and text-only. Non-modal: it doesn't trap focus or cover the page the way Modal does. Click the trigger again, click outside, press Escape, or Tab past the end to dismiss.</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3">
                        @template.ashlar.components.popover(
                        title = "Storage limit",
                        trigger = @`@template.ashlar.components.button(text = "What counts?", type = "Secondary")`,
                        content = @`<p>Files, exports, and attachments all count.</p>`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">How to Use</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Reach for Popover the moment a tooltip-style explanation needs to hold something clickable - a small filter form, an account summary with a real link, a short settings panel. If the content is a short run of plain text only, <a href="${contextPath}/components/tooltip" class="text-blue-700 hover:underline dark:text-blue-400">Tooltip</a> is the simpler, hover-revealed fit. If it's a flat list of actions, <a href="${contextPath}/components/dropdown-menu" class="text-blue-700 hover:underline dark:text-blue-400">Dropdown Menu</a> already has the right keyboard model built in. If the content needs to block the page or hold a substantial task, that's <a href="${contextPath}/components/modal" class="text-blue-700 hover:underline dark:text-blue-400">Modal</a> or <a href="${contextPath}/components/drawer" class="text-blue-700 hover:underline dark:text-blue-400">Drawer</a>'s job - Popover is never modal.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Basic</p>
                            <div class="mt-3">
                                @template.ashlar.components.popover(
                                title = "Storage limit",
                                trigger = @`@template.ashlar.components.button(text = "What counts as storage?", type = "Secondary")`,
                                content = @`<p>Uploaded files, exported reports, and attachments on records all count toward your plan's storage limit. Deleted items stop counting once the trash is emptied.</p>`
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Interactive content</p>
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
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Richer content</p>
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
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Placement</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400"><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">placement</code> takes Bottom (the default), Top, Right, or Left.</p>
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
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Keyboard and dismissal</p>
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
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Near an edge</p>
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
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Multiple instances stay independent</p>
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
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composition</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Unlike Tooltip's plain-text <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">content</code>, Popover's is real <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">gg.jte.Content</code> - that single difference is the entire reason this component exists alongside Tooltip rather than Tooltip simply growing a richer-content option. Checkboxes, buttons, a Text Input, a Badge, a link - all compose in normally and stay fully operable, demonstrated above in "Interactive content" and "Richer content". The trigger itself is equally opaque composition: any real content works, with a synthetic button role added only when nothing inside is already natively interactive.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Pitfalls &amp; Troubleshooting</h3>
                    <ul class="mt-2 list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">A placement with no room never flips to the opposite side.</strong> Choosing Left for a trigger already against the left edge clips on that side rather than flipping to Right - the same deliberate limitation as Tooltip's, inherent to staying CSS-anchored instead of becoming a positioning engine.</li>
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">A Popover inside an <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">overflow-hidden</code> ancestor is clipped by it</strong> - including Table's own horizontal scroller. There is no portal here either.</li>
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">A Popover opened inside an open Modal or Drawer closes both of them on one Escape press.</strong> Popover's Escape listener (like Modal's and Drawer's) is bound to the whole document and doesn't stop propagation - unlike Dropdown Menu, whose root-scoped listener genuinely isolates itself, nothing here prevents an Escape meant for the Popover from also reaching an enclosing Modal/Drawer's own listener on the same keypress.</li>
                    </ul>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Ashlar provides</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>The WAI-ARIA APG Disclosure pattern, not a dialog: <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-expanded</code> and <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-controls</code> on the trigger, no role at all on the surface - its content reads as ordinary page content, which it is.</li>
                                <li>Genuinely non-modal behavior: Tab passes straight through and back out into the page, matching what's actually announced (no false dialog claim).</li>
                                <li>Focus returned to the trigger specifically on Escape - not on outside click or tabbing away, where the user is already elsewhere on purpose.</li>
                            </ul>
                        </div>
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>A real interactive trigger element when possible - a plain &lt;span&gt; still works via the synthetic role/tabindex fallback, but a real &lt;button&gt; needs no fallback at all.</li>
                                <li>Awareness of the Escape-propagation limitation above if you ever nest a Popover inside a Modal or Drawer.</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div>
                    @template.layout.api-reference(apiParams = popoverApi)
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/tooltip" class="text-blue-700 hover:underline dark:text-blue-400">Tooltip</a> for plain text, hover-revealed instead of click-opened &middot;
                        <a href="${contextPath}/components/dropdown-menu" class="text-blue-700 hover:underline dark:text-blue-400">Dropdown Menu</a> for a flat list of actions with a built-in keyboard model &middot;
                        <a href="${contextPath}/components/modal" class="text-blue-700 hover:underline dark:text-blue-400">Modal</a> and
                        <a href="${contextPath}/components/drawer" class="text-blue-700 hover:underline dark:text-blue-400">Drawer</a> for genuinely modal surfaces
                    </p>
                </div>
            </div>
        </section>
`
)
