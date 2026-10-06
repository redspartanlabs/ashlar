@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param String contextPath

<%-- Batch 10 documentation page. Existing "Basic" / "Placement" /
     "Interactive content and a footer" / "Long content scrolls" /
     "Several triggers, one drawer" examples, plus the four drawer
     instances rendered after the section, kept verbatim. Added: Quick
     Start, formal grammar headings, How to Use, Composition,
     Accessibility, API Reference, Related Components, Pitfalls.

     THE ONE THING THIS PAGE HAS TO GET RIGHT, per this batch's own
     explicit caution: do not assume Drawer is equivalent to Modal with a
     different animation. Verified directly, independently of Modal's own
     audit: Drawer genuinely IS modal (role="dialog", aria-modal="true",
     a real backdrop, body scroll lock, a real focus trap) - that part
     really does match Modal. But several real, source-confirmed
     differences exist and are documented below rather than assumed away:
     title is required here (Modal's is optional); the close button and
     Escape are always active, with no closeOnEscape-style parameter to
     turn either off, because nothing in this component's own use cases
     ever needed that flexibility; and only two placements exist
     (Left/Right) - a top/bottom "sheet" is explicitly a different
     component by DrawerPlacement's own doc comment, not a missing
     variant of this one.

     Verified against source before writing:
     - drawer.jte's own 5 @params and extensive doc comment. Confirmed
       the trigger contract (data-drawer-open="{id}"/
       data-drawer-close="{id}") is identical to Modal's, for the same
       reason: a page-level surface that several different controls can
       open, which a wrapper/trigger pairing couldn't express. Confirmed
       sizing is fixed (w-screen max-w-md) - full-bleed on a phone, a
       capped column otherwise - with no size parameter.
     - drawer.js in full. CONFIRMED a real focus trap (Tab wraps at both
       ends back into the panel, and focus landing outside the panel
       while open is pulled back in) and a real, idempotent body-scroll
       lock (removes the inline overflow property on close rather than
       restoring a saved previous value, specifically so a second
       drawer's lock can never be clobbered by a first one's delayed
       unlock). CONFIRMED focus returns to whichever specific trigger
       element actually opened it (captured at open time), immediately
       on close rather than waiting for the exit transition to finish.
     - CONFIRMED THE SAME ESCAPE-PROPAGATION GAP FOUND FOR POPOVER:
       Drawer's own Escape listener is bound to `document`, with no
       stopPropagation call - identical in shape to Modal's and
       Popover's own document-level listeners. A Drawer opened from
       inside an already-open Modal (or vice versa), with Escape
       pressed, closes both in the same keypress, for the same reason
       documented on Popover's page: sibling listeners on the same
       target (document) all fire regardless of stopPropagation, and
       none of the three calls stopImmediatePropagation either. --%>
!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.components.button(text = \"Open drawer\", attributes = \"data-drawer-open=\\\"exampleDrawer\\\"\")\n\n"
        + "@template.ashlar.components.drawer(\n"
        + "    id = \"exampleDrawer\",\n"
        + "    title = \"Account details\",\n"
        + "    body = @" + backtick + "<p>Drawer content.</p>" + backtick + "\n"
        + ")";}
!{List<ApiParam> drawerApi = List.of(
    ApiParam.required("id", "String", "Matched by data-drawer-open/data-drawer-close attributes elsewhere on the page - any number of triggers can share one id."),
    ApiParam.required("title", "String", "Required, unlike Modal's optional title - a dialog must have an accessible name, and this is where it comes from (aria-labelledby)."),
    ApiParam.optional("placement", "String", "\"Right\"", "Left or Right only - there is no Top/Bottom; a sheet-style panel is a deliberately different component."),
    ApiParam.optional("body", "gg.jte.Content", "null", "The scrollable main content. The header and footer stay fixed while only this region scrolls."),
    ApiParam.optional("footer", "gg.jte.Content", "null", "Composed actions, typically Button calls - not a Drawer-specific action API.")
);}

@template.layout.page(
contextPath = contextPath,
title = "Drawer",
content = @`
    @template.layout.component-nav(currentSlug = "drawer", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Drawer</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A full-height panel anchored to the left or right edge, for navigation, settings, filters, or a form that needs more room than a Popover. It is modal: the page behind it is covered, its scroll is locked, and Tab stays inside until you close it with the close button, Escape, or the backdrop.</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3">
                        @template.ashlar.components.button(text = "Open drawer", type = "Secondary", attributes = "data-drawer-open=\"quickStartDrawer\"")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">How to Use</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Reach for Drawer for navigation, filters, or a form that genuinely needs to block the page and hold focus while it's open - the same modal commitment as <a href="${contextPath}/components/modal" class="text-blue-700 hover:underline dark:text-blue-400">Modal</a>, just anchored to an edge with room to scroll rather than centered. If the content doesn't need to block the page at all, <a href="${contextPath}/components/popover" class="text-blue-700 hover:underline dark:text-blue-400">Popover</a> is the non-modal alternative. Place every drawer instance once, anywhere on the page (this page places its own after the main section), and open it from any number of triggers carrying its id - see Composition.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
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
                            <div class="mt-3 flex flex-wrap gap-3">
                                @template.ashlar.components.button(text = "From the left", type = "Secondary", attributes = "data-drawer-open=\"leftDrawer\"")
                                @template.ashlar.components.button(text = "From the right", type = "Secondary", attributes = "data-drawer-open=\"basicDrawer\"")
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Interactive content and a footer</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Form controls inside are reachable with Tab and operating them does not dismiss the drawer. What Save actually does is the application's business - this component only owns opening, closing and focus.</p>
                            <div class="mt-3">
                                @template.ashlar.components.button(text = "Edit account", type = "Primary", attributes = "data-drawer-open=\"formDrawer\"")
                            </div>
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
                            <div class="mt-3 flex flex-wrap items-center gap-3">
                                @template.ashlar.components.button(text = "Open from here", type = "Ghost", attributes = "data-drawer-open=\"basicDrawer\"")
                                @template.ashlar.components.button(text = "Or from here", type = "Ghost", attributes = "data-drawer-open=\"basicDrawer\"")
                            </div>
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composition</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Drawer uses the same trigger contract as Modal, not the wrapper/item or single-slot patterns the rest of the library uses: the drawer itself is rendered once, anywhere on the page, and any element elsewhere carrying <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">data-drawer-open="{id}"</code> opens it - demonstrated in "Several triggers, one drawer" above, where two independent buttons both open the same instance. <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">body</code> and <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">footer</code> are ordinary composition slots, the same shape as Card's - Text Input, Checkbox, and Button all compose in normally, shown in "Interactive content and a footer" above.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Pitfalls &amp; Troubleshooting</h3>
                    <ul class="mt-2 list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">A Drawer opened inside an open Modal closes both of them on one Escape press.</strong> Drawer's Escape listener (like Modal's and Popover's) is bound to the whole document and doesn't stop propagation, so nothing prevents an Escape meant for one from also reaching the other's own listener on the same keypress - the same architectural gap documented on Popover's page.</li>
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">Escape and the close button can't be turned off.</strong> Unlike Modal, there is no parameter for either - both are hard-coded on, since a keyboard user inside a focus-trapped Drawer always needs a way out.</li>
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">There is no Top/Bottom placement.</strong> Only Left and Right exist; a sheet-style panel sliding from the top or bottom is a different component with a different sizing and gesture model, not a missing value here.</li>
                    </ul>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
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
                </div>

                <div>
                    @template.layout.api-reference(apiParams = drawerApi)
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/modal" class="text-blue-700 hover:underline dark:text-blue-400">Modal</a>, the same modal commitment, centered rather than edge-anchored &middot;
                        <a href="${contextPath}/components/popover" class="text-blue-700 hover:underline dark:text-blue-400">Popover</a> for content that doesn't need to block the page
                    </p>
                </div>
            </div>
        </section>

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
