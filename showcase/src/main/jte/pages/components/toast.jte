@import dev.redspartanlabs.ashlar.icon.Icon
@import dev.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import dev.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import dev.redspartanlabs.ashlar.AshlarAssets
@import java.util.List

@param String contextPath

!{String quickStartCode = """
    @template.ashlar.components.toast-region()

    @template.ashlar.components.button(
        text = "Save",
        attributes = "data-toast-trigger data-toast-variant=\\"success\\" data-toast-title=\\"Saved\\""
    )""";}
!{String burstDemoCode = """
    showToast({ variant: "success", title: "Toast A", message: "Closes after 2 seconds.", duration: 2000 });
    showToast({ variant: "info", title: "Toast B", message: "Closes after 4 seconds.", duration: 4000 });
    showToast({ variant: "warning", title: "Toast C", message: "Closes after 6 seconds.", duration: 6000 });
    showToast({ variant: "neutral", title: "Toast D", message: "Closes after 8 seconds.", duration: 8000 });
    showToast({ variant: "danger", title: "Toast E", message: "Persistent - dismiss it manually." });""";}
!{List<ApiParam> toastApi = List.of(
    ApiParam.optional("id", "String", "\"\"", "Needed only to re-show this exact toast later via data-toast-show=\"{id}\" - see Composition."),
    ApiParam.optional("variant", "String", "\"Info\"", "Success/Info/Warning/Danger/Neutral. Unrecognized values silently fall back to Info."),
    ApiParam.optional("title", "String", "\"\"", "Rendered as a <p>, hidden entirely (not just empty) when left blank."),
    ApiParam.optional("message", "String", "\"\"", "Same hidden-when-blank treatment as title."),
    ApiParam.optional("icon", "Icon", "null", "Overrides the variant's own default icon."),
    ApiParam.optional("dismissible", "boolean", "true", "Shows a close button. Forced true regardless of this value whenever the resolved duration is 0 - see Pitfalls."),
    ApiParam.optional("duration", "Integer", "null", "Milliseconds; 0 = persistent. null uses the variant's own default (0 for Danger, 5000 for everything else)."),
    ApiParam.optional("visible", "boolean", "true", "Initial data-toast-state. false for a server-rendered toast meant to be revealed later by id - see Composition."),
    ApiParam.optional("action", "gg.jte.Content", "null", "Extra buttons/links below the message, e.g. \"Undo\". Omitted entirely from the DOM when null.")
);}
!{List<ApiParam> toastRegionApi = List.of(
    ApiParam.optional("toasts", "gg.jte.Content", "null", "Zero or more toast(...) calls that should already exist in the DOM on page load - typically visible = false ones meant to be revealed later by id.")
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
title = "Toast",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "toast", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A transient, JavaScript-managed notification - not Alert's persistent, in-flow message. Notifications stack top-right and pause while hovered or focused. Errors stay until dismissed; everything else closes after 5 seconds unless configured otherwise. <kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">Esc</kbd> dismisses a focused toast. Try dragging a toast horizontally (mouse or touch) &mdash; a short drag snaps back, a longer drag or a quick flick dismisses it; the close button still works too.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Toast needs two things on the page: the region (once, anywhere - typically near the end of the page via <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">afterMain</code>) and a trigger. No region, no JavaScript runs at all - see Pitfalls.</p>
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3">
                    @template.ashlar.components.button(text = "Save", attributes = "data-toast-trigger data-toast-variant=\"success\" data-toast-title=\"Saved\"")
                </div>
            `
            )
        </section>

        <section id="how-to-use" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "How to Use",
            content = @`<p class="text-sm text-slate-500 dark:text-slate-400">There are two ways to show one, and they serve different needs. For an ad-hoc notification reacting to something that just happened client-side (a save succeeded, a copy completed), use the declarative <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">data-toast-trigger</code> attributes on any element, or call <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">showToast()</code> directly from your own JavaScript (e.g. inside a fetch's success handler) - both clone one of the region's own per-variant templates and the resulting toast is fully removed from the DOM on dismiss. For a toast the server already knows needs to exist - rendered with the rest of the page, but shown a moment later by id (the "Undo delete" pattern in Content below) - render it with <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">visible = false</code> inside the region's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">toasts</code> slot, then reveal it with a <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">data-toast-show="{id}"</code> trigger; that one is only ever hidden again on dismiss, never removed.</p>`
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Variants</p>
                        <div class="mt-3 flex flex-wrap items-center gap-3">
                            @template.ashlar.components.button(text = "Success", type = "Success", attributes = "data-toast-trigger data-toast-variant=\"success\" data-toast-title=\"Changes saved\" data-toast-message=\"Your changes have been saved successfully.\"")
                            @template.ashlar.components.button(text = "Info", type = "Info", attributes = "data-toast-trigger data-toast-variant=\"info\" data-toast-title=\"Export started\" data-toast-message=\"You can keep working while the report is prepared.\"")
                            @template.ashlar.components.button(text = "Warning", type = "Warning", attributes = "data-toast-trigger data-toast-variant=\"warning\" data-toast-title=\"Session expiring soon\" data-toast-message=\"Save your work within the next 5 minutes.\"")
                            @template.ashlar.components.button(text = "Danger", type = "Danger", attributes = "data-toast-trigger data-toast-variant=\"danger\" data-toast-title=\"Save failed\" data-toast-message=\"The server could not be reached. Your changes have not been saved.\"")
                            @template.ashlar.components.button(text = "Neutral", type = "Secondary", attributes = "data-toast-trigger data-toast-variant=\"neutral\" data-toast-title=\"New comment\" data-toast-message=\"Dana Whitfield commented on ACC-1004.\"")
                        </div>
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Timing &amp; dismissal</p>
                        <div class="mt-3 flex flex-wrap items-center gap-3">
                            @template.ashlar.components.button(text = "Short (2s)", type = "Secondary", attributes = "data-toast-trigger data-toast-variant=\"success\" data-toast-title=\"Copied\" data-toast-duration=\"2000\"")
                            @template.ashlar.components.button(text = "Long (10s)", type = "Secondary", attributes = "data-toast-trigger data-toast-variant=\"info\" data-toast-title=\"Import queued\" data-toast-message=\"This one stays for 10 seconds.\" data-toast-duration=\"10000\"")
                            @template.ashlar.components.button(text = "Persistent", type = "Secondary", attributes = "data-toast-trigger data-toast-variant=\"warning\" data-toast-title=\"Review required\" data-toast-message=\"This stays until you dismiss it.\" data-toast-duration=\"0\"")
                            @template.ashlar.components.button(text = "No close button (4s)", type = "Secondary", attributes = "data-toast-trigger data-toast-variant=\"neutral\" data-toast-message=\"Auto-dismisses only; there is no close button.\" data-toast-duration=\"4000\" data-toast-dismissible=\"false\"")
                        </div>
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Content</p>
                        <div class="mt-3 flex flex-wrap items-center gap-3">
                            @template.ashlar.components.button(text = "Title only", type = "Secondary", attributes = "data-toast-trigger data-toast-variant=\"success\" data-toast-title=\"Password updated\"")
                            @template.ashlar.components.button(text = "Message only", type = "Secondary", attributes = "data-toast-trigger data-toast-variant=\"info\" data-toast-message=\"3 new accounts were assigned to you.\"")
                            @template.ashlar.components.button(text = "Long message", type = "Secondary", attributes = "data-toast-trigger data-toast-variant=\"danger\" data-toast-title=\"Import completed with errors\" data-toast-message=\"14 of 120 rows could not be imported because the Owner column referenced users who no longer exist, or the Created date was not a valid ISO-8601 date. Download the error report from the Imports page for row-level details.\"")
                            @template.ashlar.components.button(text = "With action (server-rendered)", type = "Secondary", attributes = "data-toast-show=\"undoDeleteToast\"")
                        </div>
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Stacking &amp; independent timers</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Creates five toasts at once via <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">showToast()</code>: 2s, 4s, 6s, 8s, and persistent. Each keeps its own independent timer - hovering one never pauses the others.</p>
                        @template.layout.example(
                        code = burstDemoCode,
                        content = @`
                            <div class="mt-3">
                                @template.ashlar.components.button(text = "Show 5 toasts", type = "Primary", attributes = "data-toast-burst-demo")
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
                <p class="text-sm text-slate-500 dark:text-slate-400">Toast Region is not a separate catalog component - it's the other half of this one, documented here rather than on its own page. A page renders it exactly once (this page does so via <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">afterMain</code>, since it's fixed-positioned and doesn't belong inside the normal content flow). It holds three things at once: the visible stacking container itself (fixed top-right, full-width below the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">sm</code> breakpoint); two separate, visually hidden live regions (one <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-live="polite"</code>, one <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-live="assertive"</code>) that actually carry announcements, kept apart from the visible toasts themselves; and one hidden <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">&lt;template&gt;</code> per variant that <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">showToast()</code> clones from, so every toast it creates still comes from this library's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">toast.jte</code> markup rather than JS-built HTML.</p>
                <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">The region's <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">&lt;script&gt;</code> tag - the only place <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">toast.js</code> is loaded - lives on <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">toast-region.jte</code>, not <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">toast.jte</code>. A toast only becomes interactive by being a descendant of the region at the moment the page's JavaScript runs - see Pitfalls for what that means in practice.</p>
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
                            <li>Render exactly one Toast Region per page, near the end, via <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">afterMain</code> or an equivalent out-of-flow slot.</li>
                            <li>Let Danger default to persistent rather than giving it a short <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">duration</code> - a failure a user doesn't get to read defeats the point of surfacing it.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Avoid</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Using Toast for something a user must still be able to find after leaving and returning to the page - it's deliberately transient. Alert is for a condition that's still true.</li>
                            <li>Stacking so many triggers that several toasts fire at once routinely - nothing here caps or queues them, so they will all stack simultaneously.</li>
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
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">No Toast Region on the page means no Toast JavaScript at all.</strong> <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">toast.js</code> is only ever loaded by <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">toast-region.jte</code>'s own &lt;script&gt; tag. Calling <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">showToast()</code> without one logs a console warning and returns null rather than throwing.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">A toast rendered outside the region is inert, not just unstyled.</strong> Only <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">[data-toast-region] [data-toast]</code> elements get wired up on page load - its close button, auto-dismiss timer, and swipe-to-dismiss all silently do nothing anywhere else, even though the markup renders identically.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">A persistent toast always gets a close button, even if you pass <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">dismissible = false</code>.</strong> <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">duration = 0</code> overrides it, by design, so a toast can never be stuck on screen with no way to dismiss it.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">Escape only dismisses a toast that has a close button.</strong> A non-persistent toast with <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">dismissible = false</code> can't be Escape-dismissed either - it can only expire on its own timer.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">There is no queue and no cap.</strong> Every triggered toast shows immediately and stacks; nothing here limits how many can be on screen at once or defers a new one until an old one closes.</li>
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
                            <li>Announcements through two separate hidden live regions, not by making each visible toast itself a live region - this is why a toast's own close button or action links are never read aloud, and why two quick toasts are each announced once instead of one replacing the other.</li>
                            <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">assertive</code> announcement only for Danger - everything else waits politely for the screen reader to finish what it's already saying.</li>
                            <li>A 250ms delay before announcing a toast that's already visible on page load, so assistive tech has a moment to register the live regions first.</li>
                            <li>Focus moved somewhere sensible when a focused toast is dismissed - to a control in an adjacent still-open toast if one exists, otherwise back to whatever had focus before the toast appeared.</li>
                            <li>The auto-dismiss timer pauses on hover and on focus, and resumes with at least a second remaining - a toast being read can't vanish mid-read.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>The Toast Region itself, exactly once - without it, none of the above runs (see Pitfalls).</li>
                            <li>Restraint on how many toasts fire at once - independently announced toasts arriving in a burst can still overwhelm a screen reader user even though each is individually correct.</li>
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
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">toast</p>
                        @template.layout.api-reference(apiParams = toastApi)
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">toast-region</p>
                        @template.layout.api-reference(apiParams = toastRegionApi)
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
                    <a href="${contextPath}/components/alert" class="text-blue-700 hover:underline dark:text-blue-400">Alert</a> for a persistent, in-flow message instead of a transient one - note its equivalent severe variant is spelled "Error", not "Danger" as it is here &middot;
                    <a href="${contextPath}/components/button" class="text-blue-700 hover:underline dark:text-blue-400">Button</a>, the usual trigger via <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">data-toast-trigger</code>
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "toast", contextPath = contextPath)
    </div>
`,
afterMain = @`
    @template.ashlar.components.toast-region(
    toasts = @`
        @template.ashlar.components.toast(
        id = "undoDeleteToast",
        variant = "Neutral",
        icon = Icon.DELETE,
        title = "Record deleted",
        message = "ACC-1009 Ironclad Security was moved to the trash.",
        duration = 8000,
        visible = false,
        action = @`
            <button type="button" data-toast-dismiss class="rounded font-medium text-blue-600 hover:text-blue-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 dark:text-blue-400 dark:hover:text-blue-300">Undo</button>
            <button type="button" data-toast-dismiss class="rounded font-medium text-slate-600 hover:text-slate-800 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 dark:text-slate-300 dark:hover:text-slate-100">View trash</button>
        `
        )
    `
    )

    <script type="module">
        import { showToast } from "${AshlarAssets.js("toast.js")}";

        document.querySelector("[data-toast-burst-demo]")?.addEventListener("click", () => {
            showToast({ variant: "success", title: "Toast A", message: "Closes after 2 seconds.", duration: 2000 });
            showToast({ variant: "info", title: "Toast B", message: "Closes after 4 seconds.", duration: 4000 });
            showToast({ variant: "warning", title: "Toast C", message: "Closes after 6 seconds.", duration: 6000 });
            showToast({ variant: "neutral", title: "Toast D", message: "Closes after 8 seconds.", duration: 8000 });
            showToast({ variant: "danger", title: "Toast E", message: "Persistent - dismiss it manually." });
        });
    </script>
`
)
