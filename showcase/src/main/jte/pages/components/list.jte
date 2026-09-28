@import com.redspartanlabs.ashlar.icon.Icon

@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "List",
content = @`
    @template.layout.component-nav(currentSlug = "list", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">List</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A semantic collection of related rows - a bordered, divided &lt;ul&gt; or &lt;ol&gt; surface, not a data table. No JavaScript: nothing here sorts, filters, or paginates. Each row is a separate item call the caller composes.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic</h3>
                    <div class="mt-3 max-w-md">
                        @template.ashlar.components.list(
                        items = @`
                            @template.ashlar.components.list-item(label = "Overview")
                            @template.ashlar.components.list-item(label = "Activity")
                            @template.ashlar.components.list-item(label = "Settings")
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Navigation-style, with icons</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A row with an <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">href</code> becomes a real link and gets a trailing chevron automatically.</p>
                    <div class="mt-3 max-w-md">
                        @template.ashlar.components.list(
                        items = @`
                            @template.ashlar.components.list-item(label = "Account", description = "Profile, email, and password", href = "#", icon = Icon.USER)
                            @template.ashlar.components.list-item(label = "Security", description = "Two-factor authentication and sessions", href = "#", icon = Icon.SHIELD)
                            @template.ashlar.components.list-item(label = "Notifications", description = "Email and in-app alerts", href = "#", icon = Icon.BELL)
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Trailing content</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A Badge, a value, or any other small composed element can sit at the end of a row - useful for metadata-style pairs.</p>
                    <div class="mt-3 max-w-md">
                        @template.ashlar.components.list(
                        items = @`
                            @template.ashlar.components.list-item(
                                label = "Plan",
                                trailing = @`<span class="font-medium text-slate-700 dark:text-slate-300">Enterprise</span>`
                            )
                            @template.ashlar.components.list-item(
                                label = "Status",
                                trailing = @`@template.ashlar.components.badge(text = "Active", variant = "Success")`
                            )
                            @template.ashlar.components.list-item(
                                label = "Renews",
                                trailing = @`<span class="font-medium text-slate-700 dark:text-slate-300">Jan 14, 2027</span>`
                            )
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Ordered</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400"><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">ordered = true</code> renders an &lt;ol&gt; - position itself is part of what the list communicates.</p>
                    <div class="mt-3 max-w-md">
                        @template.ashlar.components.list(
                        ordered = true,
                        items = @`
                            @template.ashlar.components.list-item(label = "Submit the request", description = "Fill in the account details and reason for change")
                            @template.ashlar.components.list-item(label = "Manager review", description = "Routed automatically based on account owner")
                            @template.ashlar.components.list-item(label = "Access granted", description = "Usually within one business day")
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Long content wraps</h3>
                    <div class="mt-3 max-w-md">
                        @template.ashlar.components.list(
                        items = @`
                            @template.ashlar.components.list-item(
                                label = "Quarterly compliance report for enterprise accounts",
                                description = "Generated automatically on the first business day of each quarter and distributed to every account owner on file.",
                                icon = Icon.FILE_TEXT,
                                trailing = @`@template.ashlar.components.badge(text = "Draft")`
                            )
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">In context</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Composed inside a Card, the way it would appear in a real page.</p>
                    <div class="mt-3 max-w-md">
                        @template.ashlar.components.card(
                        title = "Recent files",
                        body = @`
                            @template.ashlar.components.list(
                            items = @`
                                @template.ashlar.components.list-item(label = "Q3 forecast.xlsx", description = "Edited 2 hours ago", href = "#", icon = Icon.FILE)
                                @template.ashlar.components.list-item(label = "Onboarding checklist.pdf", description = "Edited yesterday", href = "#", icon = Icon.FILE_TEXT)
                                @template.ashlar.components.list-item(label = "Team photos", description = "12 files", href = "#", icon = Icon.FOLDER)
                            `
                            )
                        `
                        )
                    </div>
                </div>
            </div>
        </section>
`
)
