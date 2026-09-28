@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Card",
content = @`
    @template.layout.component-nav(currentSlug = "card", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Card</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A content surface - border, radius, and background only. The page decides width, placement, and spacing around it.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic (body only)</h3>
                    <div class="mt-3 max-w-sm">
                        @template.ashlar.components.card(
                        body = @`
                            <p>A Card with no title and no footer - just the surface and its content.</p>
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">With a header</h3>
                    <div class="mt-3 max-w-sm">
                        @template.ashlar.components.card(
                        title = "Account details",
                        body = @`
                            <p>Acme Manufacturing has been an active enterprise account since January 2024, currently on the Enterprise plan with 3 seats.</p>
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">With a header, body, and footer</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The footer is a plain composition slot - these are the same Button components used everywhere else, not a Card-specific action API.</p>
                    <div class="mt-3 max-w-sm">
                        @template.ashlar.components.card(
                        title = "Delete account",
                        body = @`
                            <p>This permanently removes the account and all associated records. This action cannot be undone.</p>
                        `,
                        footer = @`
                            @template.ashlar.components.button(text = "Cancel", type = "Ghost")
                            @template.ashlar.components.button(text = "Delete", type = "Danger")
                        `
                        )
                    </div>
                </div>

                <%-- Restored in Batch 2, which brought Divider and Text Input
                     across. Verbatim from the reference implementation. --%>
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composing existing components inside a Card</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Card stays a neutral container - Badge, Divider, and a Text Input all render normally inside it, unaware they're inside a Card.</p>
                    <div class="mt-3 grid gap-4 sm:grid-cols-2">
                        @template.ashlar.components.card(
                        title = "Kestrel Aerospace",
                        body = @`
                            <div class="flex items-center gap-2">
                                <span class="text-slate-500 dark:text-slate-400">Status:</span>
                                @template.ashlar.components.badge(text = "Active", variant = "Success")
                            </div>
                            <div class="my-4">
                                @template.ashlar.components.divider()
                            </div>
                            <p class="text-slate-500 dark:text-slate-400">Government - onboarded 2023-07-14</p>
                        `
                        )
                        @template.ashlar.components.card(
                        title = "Invite a teammate",
                        body = @`
                            @template.ashlar.components.text-input(
                            name = "cardContactEmail",
                            label = "Email address",
                            placeholder = "name@company.com"
                            )
                        `,
                        footer = @`
                            @template.ashlar.components.button(text = "Send Invite", type = "Primary", size = "Small")
                        `
                        )
                    </div>
                </div>
            </div>
        </section>
`
)
