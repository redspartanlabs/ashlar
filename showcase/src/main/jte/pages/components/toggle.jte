@param String contextPath


@template.layout.page(
contextPath = contextPath,
title = "Toggle / Switch",
content = @`
    @template.layout.component-nav(currentSlug = "toggle", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Toggle / Switch</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A real &lt;input type="checkbox"&gt; underneath - visually hidden, styled entirely via CSS <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">peer-*</code> variants. Zero JavaScript; toggling, focus, and form behavior are all native.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic &amp; checked</h3>
                    @template.ashlar.components.toggle(
                    name = "marketingEmailsToggle",
                    label = "Send me marketing emails"
                    )
                    @template.ashlar.components.toggle(
                    name = "autoSave",
                    label = "Auto-save changes",
                    checked = true
                    )
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Label + description</h3>
                    @template.ashlar.components.toggle(
                    name = "notificationsToggle",
                    label = "Enable notifications",
                    helpText = "Receive alerts when important events occur.",
                    checked = true
                    )
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Disabled (off &amp; on)</h3>
                    @template.ashlar.components.toggle(
                    name = "disabledOff",
                    label = "Not available on your plan",
                    disabled = true
                    )
                    @template.ashlar.components.toggle(
                    name = "disabledOn",
                    label = "Always enabled for admins",
                    checked = true,
                    disabled = true
                    )
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Label position</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Same switch, label text on either side.</p>
                    <div class="mt-3 flex flex-col gap-4">
                        @template.ashlar.components.toggle(
                        name = "labelEnd",
                        label = "Switch, then label (default)"
                        )
                        @template.ashlar.components.toggle(
                        name = "labelStart",
                        label = "Label, then switch",
                        labelPosition = "start"
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Multiple independent switches</h3>
                    <div class="flex flex-col gap-3">
                        @template.ashlar.components.toggle(name = "enableNotifications", label = "Enable notifications", checked = true)
                        @template.ashlar.components.toggle(name = "autoSaveChanges", label = "Auto-save changes", checked = true)
                        @template.ashlar.components.toggle(name = "enableTelemetry", label = "Enable telemetry")
                        @template.ashlar.components.toggle(name = "maintenanceMode", label = "Maintenance mode")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Server-reported error</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Forced via the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">error</code> param - the same way a controller would re-render a field after a failed server-side check.</p>
                    <div class="mt-3">
                        @template.ashlar.components.toggle(
                        name = "flaggedAcknowledge",
                        label = "I acknowledge the maintenance window",
                        error = true,
                        errorMessage = "You must acknowledge the maintenance window to continue."
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Required, keyboard/Tab order &amp; form submission</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Tab to the switch (the disabled one above is skipped entirely), toggle with <kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">Space</kbd>, then submit off to see the browser's own validation UI on the required switch. Reset restores the original checked state.</p>
                    <form class="mt-3 flex flex-col gap-4" action="" method="get">
                        @template.ashlar.components.toggle(
                        name = "acceptTermsToggle",
                        label = "I accept the terms and conditions",
                        required = true
                        )
                        <div class="flex gap-3">
                            @template.ashlar.components.button(text = "Submit", type = "Primary", buttonType = "submit")
                            @template.ashlar.components.button(text = "Reset", type = "Ghost", buttonType = "reset")
                        </div>
                    </form>
                </div>
            </div>
        </section>
`
)
