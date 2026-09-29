@import com.redspartanlabs.ashlar.icon.Icon

@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Empty State",
content = @`
    @template.layout.component-nav(currentSlug = "empty-state", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Empty State</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Content has loaded, and there is currently none of it - distinct from Skeleton, which is for content that hasn't loaded yet. No JavaScript; the action slot accepts real, composed Button content rather than owning its own action API.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic</h3>
                    <div class="mt-3 rounded-lg border border-slate-200 dark:border-slate-700">
                        @template.ashlar.components.empty-state(
                        icon = Icon.INBOX,
                        title = "No messages"
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">With body</h3>
                    <div class="mt-3 rounded-lg border border-slate-200 dark:border-slate-700">
                        @template.ashlar.components.empty-state(
                        icon = Icon.SEARCH,
                        title = "No results found",
                        body = @`Try adjusting your filters or search terms.`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">With an action</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The action is an ordinary Button, composed by the page - Empty State has no action API of its own.</p>
                    <div class="mt-3 rounded-lg border border-slate-200 dark:border-slate-700">
                        @template.ashlar.components.empty-state(
                        icon = Icon.FOLDER,
                        title = "No projects yet",
                        body = @`Create your first project to start tracking work.`,
                        action = @`
                            @template.ashlar.components.button(text = "Create Project", type = "Primary")
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Without an icon</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The icon is optional - the component stays complete without one.</p>
                    <div class="mt-3 rounded-lg border border-slate-200 dark:border-slate-700">
                        @template.ashlar.components.empty-state(
                        title = "No activity in the last 30 days"
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composed inside a Card</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Empty State doesn't know it's inside a Card, and doesn't need to - it just renders the content it was given.</p>
                    <div class="mt-3 max-w-md">
                        @template.ashlar.components.card(
                        title = "Migration Jobs",
                        body = @`
                            @template.ashlar.components.empty-state(
                            icon = Icon.DATABASE,
                            title = "No migration jobs",
                            body = @`There are no migration jobs scheduled or currently running. Jobs you create will appear here, along with their status and progress as they run.`,
                            action = @`
                                @template.ashlar.components.button(text = "Create Job", type = "Primary", size = "Small")
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
