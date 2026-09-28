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

                <%-- DEFERRED: the "Composing existing components inside a Card"
                     section is omitted here, not deleted. It demonstrates Card
                     staying neutral around a Badge, a Divider and a Text Input,
                     and Divider and Text Input have not migrated yet. Batch 1
                     is not expanded to satisfy a showcase page; this section is
                     restored verbatim from the reference implementation in the
                     batch that brings those two components across. The Card
                     component itself is fully migrated and unchanged. --%>
            </div>
        </section>
`
)
