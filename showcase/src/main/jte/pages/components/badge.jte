@import com.redspartanlabs.ashlar.icon.Icon

@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Badge",
content = @`
    @template.layout.component-nav(currentSlug = "badge", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Badge</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A static label for status, category, or metadata - plain HTML and CSS, no JavaScript, not a button or a control.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Semantic variants</h3>
                    <div class="mt-3 flex flex-wrap items-center gap-3">
                        @template.ashlar.components.badge(text = "Draft", variant = "Neutral")
                        @template.ashlar.components.badge(text = "In Review", variant = "Info")
                        @template.ashlar.components.badge(text = "Active", variant = "Success")
                        @template.ashlar.components.badge(text = "High Priority", variant = "Warning")
                        @template.ashlar.components.badge(text = "Failed", variant = "Danger")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">With an optional leading icon</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Reinforcing, never the sole carrier of meaning - the text alone already says it.</p>
                    <div class="mt-3 flex flex-wrap items-center gap-3">
                        @template.ashlar.components.badge(text = "Completed", variant = "Success", icon = Icon.CHECK_CIRCLE)
                        @template.ashlar.components.badge(text = "Needs Attention", variant = "Warning", icon = Icon.WARNING_TRIANGLE)
                        @template.ashlar.components.badge(text = "Rejected", variant = "Danger", icon = Icon.X_CIRCLE)
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Inline with text</h3>
                    <p class="mt-2 text-sm text-slate-700 dark:text-slate-300">
                        Job Header JOB-1042 is currently
                        @template.ashlar.components.badge(text = "Production", variant = "Success")
                        and flagged
                        @template.ashlar.components.badge(text = "High Priority", variant = "Warning")
                        for review.
                    </p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Inside a table</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A status column reads faster as a badge than as plain text - shown here as a small standalone table, not a change to the Data Table component above.</p>
                    <div class="mt-3 overflow-x-auto rounded-lg border border-slate-200 dark:border-slate-700">
                        <table class="min-w-full divide-y divide-slate-200 text-sm dark:divide-slate-700">
                            <thead class="bg-slate-50 dark:bg-slate-800">
                            <tr>
                                <th scope="col" class="px-4 py-3 text-left font-semibold text-slate-900 dark:text-slate-100">Job Header</th>
                                <th scope="col" class="px-4 py-3 text-left font-semibold text-slate-900 dark:text-slate-100">Status</th>
                            </tr>
                            </thead>
                            <tbody class="divide-y divide-slate-200 bg-white dark:divide-slate-700 dark:bg-slate-900">
                            <tr>
                                <td class="px-4 py-3 text-slate-700 dark:text-slate-300">JOB-1001</td>
                                <td class="px-4 py-3">@template.ashlar.components.badge(text = "Active", variant = "Success")</td>
                            </tr>
                            <tr>
                                <td class="px-4 py-3 text-slate-700 dark:text-slate-300">JOB-1002</td>
                                <td class="px-4 py-3">@template.ashlar.components.badge(text = "Pending", variant = "Warning")</td>
                            </tr>
                            <tr>
                                <td class="px-4 py-3 text-slate-700 dark:text-slate-300">JOB-1003</td>
                                <td class="px-4 py-3">@template.ashlar.components.badge(text = "Completed", variant = "Success")</td>
                            </tr>
                            <tr>
                                <td class="px-4 py-3 text-slate-700 dark:text-slate-300">JOB-1004</td>
                                <td class="px-4 py-3">@template.ashlar.components.badge(text = "Failed", variant = "Danger")</td>
                            </tr>
                            <tr>
                                <td class="px-4 py-3 text-slate-700 dark:text-slate-300">JOB-1005</td>
                                <td class="px-4 py-3">@template.ashlar.components.badge(text = "Draft", variant = "Neutral")</td>
                            </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </section>
`
)
