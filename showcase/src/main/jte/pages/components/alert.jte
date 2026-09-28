@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Alert",
content = @`
    @template.layout.component-nav(currentSlug = "alert", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Alert</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A persistent, in-flow contextual message - not a Toast. No JavaScript, no dismiss button, no timer; it's part of the page for as long as the condition it describes is true.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Semantic variants, with a title</h3>
                    <div class="mt-3 flex flex-col gap-4">
                        @template.ashlar.components.alert(
                        variant = "Info",
                        title = "Scheduled maintenance",
                        body = @`
                            <p>The billing service will be unavailable Saturday 10:00 PM - 2:00 AM ET for planned maintenance.</p>
                        `
                        )
                        @template.ashlar.components.alert(
                        variant = "Success",
                        title = "Changes saved",
                        body = @`
                            <p>Your account settings were updated successfully.</p>
                        `
                        )
                        @template.ashlar.components.alert(
                        variant = "Warning",
                        title = "Approaching seat limit",
                        body = @`
                            <p>This account is using 9 of 10 available seats. Add seats before inviting more teammates.</p>
                        `
                        )
                        @template.ashlar.components.alert(
                        variant = "Error",
                        title = "Unable to process payment",
                        body = @`
                            <p>The card on file was declined. Update your payment method to avoid a service interruption.</p>
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Body without a title</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The title is optional - a short notice doesn't need one.</p>
                    <div class="mt-3 flex flex-col gap-4">
                        @template.ashlar.components.alert(
                        variant = "Info",
                        body = @`
                            <p>Reports are generated nightly at 1:00 AM ET.</p>
                        `
                        )
                        @template.ashlar.components.alert(
                        variant = "Warning",
                        body = @`
                            <p>You have unsaved changes. Leaving this page will discard them.</p>
                        `
                        )
                    </div>
                </div>
            </div>
        </section>
`
)
