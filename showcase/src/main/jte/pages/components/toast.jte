@import com.redspartanlabs.ashlar.icon.Icon
@import com.redspartanlabs.ashlar.AshlarAssets

@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Toast",
content = @`
    @template.layout.component-nav(currentSlug = "toast", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Toasts</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Notifications stack top-right and pause while hovered or focused. Errors stay until dismissed; everything else closes after 5 seconds unless configured otherwise. <kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">Esc</kbd> dismisses a focused toast. Try dragging a toast horizontally (mouse or touch) &mdash; a short drag snaps back, a longer drag or a quick flick dismisses it; the close button still works too.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants</h3>
                    <div class="mt-3 flex flex-wrap items-center gap-3">
                        @template.ashlar.components.button(text = "Success", type = "Success", attributes = "data-toast-trigger data-toast-variant=\"success\" data-toast-title=\"Changes saved\" data-toast-message=\"Your changes have been saved successfully.\"")
                        @template.ashlar.components.button(text = "Info", type = "Info", attributes = "data-toast-trigger data-toast-variant=\"info\" data-toast-title=\"Export started\" data-toast-message=\"You can keep working while the report is prepared.\"")
                        @template.ashlar.components.button(text = "Warning", type = "Warning", attributes = "data-toast-trigger data-toast-variant=\"warning\" data-toast-title=\"Session expiring soon\" data-toast-message=\"Save your work within the next 5 minutes.\"")
                        @template.ashlar.components.button(text = "Danger", type = "Danger", attributes = "data-toast-trigger data-toast-variant=\"danger\" data-toast-title=\"Save failed\" data-toast-message=\"The server could not be reached. Your changes have not been saved.\"")
                        @template.ashlar.components.button(text = "Neutral", type = "Secondary", attributes = "data-toast-trigger data-toast-variant=\"neutral\" data-toast-title=\"New comment\" data-toast-message=\"Dana Whitfield commented on ACC-1004.\"")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Timing &amp; dismissal</h3>
                    <div class="mt-3 flex flex-wrap items-center gap-3">
                        @template.ashlar.components.button(text = "Short (2s)", type = "Secondary", attributes = "data-toast-trigger data-toast-variant=\"success\" data-toast-title=\"Copied\" data-toast-duration=\"2000\"")
                        @template.ashlar.components.button(text = "Long (10s)", type = "Secondary", attributes = "data-toast-trigger data-toast-variant=\"info\" data-toast-title=\"Import queued\" data-toast-message=\"This one stays for 10 seconds.\" data-toast-duration=\"10000\"")
                        @template.ashlar.components.button(text = "Persistent", type = "Secondary", attributes = "data-toast-trigger data-toast-variant=\"warning\" data-toast-title=\"Review required\" data-toast-message=\"This stays until you dismiss it.\" data-toast-duration=\"0\"")
                        @template.ashlar.components.button(text = "No close button (4s)", type = "Secondary", attributes = "data-toast-trigger data-toast-variant=\"neutral\" data-toast-message=\"Auto-dismisses only; there is no close button.\" data-toast-duration=\"4000\" data-toast-dismissible=\"false\"")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Content</h3>
                    <div class="mt-3 flex flex-wrap items-center gap-3">
                        @template.ashlar.components.button(text = "Title only", type = "Secondary", attributes = "data-toast-trigger data-toast-variant=\"success\" data-toast-title=\"Password updated\"")
                        @template.ashlar.components.button(text = "Message only", type = "Secondary", attributes = "data-toast-trigger data-toast-variant=\"info\" data-toast-message=\"3 new accounts were assigned to you.\"")
                        @template.ashlar.components.button(text = "Long message", type = "Secondary", attributes = "data-toast-trigger data-toast-variant=\"danger\" data-toast-title=\"Import completed with errors\" data-toast-message=\"14 of 120 rows could not be imported because the Owner column referenced users who no longer exist, or the Created date was not a valid ISO-8601 date. Download the error report from the Imports page for row-level details.\"")
                        @template.ashlar.components.button(text = "With action (server-rendered)", type = "Secondary", attributes = "data-toast-show=\"undoDeleteToast\"")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Stacking &amp; independent timers</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Creates five toasts at once via <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">showToast()</code>: 2s, 4s, 6s, 8s, and persistent.</p>
                    <div class="mt-3">
                        @template.ashlar.components.button(text = "Show 5 toasts", type = "Primary", attributes = "data-toast-burst-demo")
                    </div>
                </div>
            </div>
        </section>
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
