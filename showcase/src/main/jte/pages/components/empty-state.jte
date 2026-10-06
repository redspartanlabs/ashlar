@import com.redspartanlabs.ashlar.icon.Icon
@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param String contextPath

<%-- Batch 5 documentation page. Existing "Basic" / "With body" / "With an
     action" / "Without an icon" / "Composed inside a Card" examples kept
     verbatim, moved under Variants & States/Composition. Added: Quick
     Start, formal grammar headings, Accessibility, API Reference, Related
     Components.

     Verified against source before writing:
     - empty-state.jte's own 4 @params and its extensive doc comment.
       Confirmed title is required - unlike Card's and Alert's own
       optional titles - because this component's entire reason to exist
       IS its message, so a titleless instance would be a contradiction,
       the same reasoning badge.jte's required text already applies.
     - Confirmed this is semantically distinct from Skeleton, not a
       different visual treatment of the same idea: the content HAS
       loaded here, and there is currently none of it, the same
       distinction Progress draws from Spinner.
     - Confirmed title renders as a real <h3>, matching Card's and
       Alert's own choice, specifically because this component commonly
       nests inside a Card or a Table's own empty region rather than
       standing directly under a page's h1/h2.
     - Confirmed action is gg.jte.Content, not an actionText/actionUrl
       pair - this component has no idea what "an action" is and
       shouldn't, the same reasoning card.jte's own footer slot uses for
       composed Buttons rather than Card inventing a button API.
     - Confirmed via grep that Table composes this component directly for
       its own empty-result region - a real, verified relationship, not
       assumed from the name. --%>
!{String quickStartCode = """
    @template.ashlar.components.empty-state(
        icon = Icon.INBOX,
        title = "No messages"
    )""";}
!{List<ApiParam> emptyStateApi = List.of(
    ApiParam.required("title", "String", "The component's entire reason to exist - rendered as a real <h3>, matching Card's and Alert's own title treatment."),
    ApiParam.optional("body", "gg.jte.Content", "null", "Supporting text below the title."),
    ApiParam.optional("icon", "Icon", "null", "Always decorative - the title already says in words what the icon would."),
    ApiParam.optional("action", "gg.jte.Content", "null", "Ordinary composed Button(s)/link(s) - this component has no action API of its own.")
);}

@template.layout.page(
contextPath = contextPath,
title = "Empty State",
content = @`
    @template.layout.component-nav(currentSlug = "empty-state", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Empty State</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Content has loaded, and there is currently none of it - distinct from Skeleton, which is for content that hasn't loaded yet. No JavaScript; the action slot accepts real, composed Button content rather than owning its own action API.</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3 rounded-lg border border-slate-200 dark:border-slate-700">
                        @template.ashlar.components.empty-state(icon = Icon.INBOX, title = "No messages")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Basic</p>
                            <div class="mt-3 rounded-lg border border-slate-200 dark:border-slate-700">
                                @template.ashlar.components.empty-state(
                                icon = Icon.INBOX,
                                title = "No messages"
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">With body</p>
                            <div class="mt-3 rounded-lg border border-slate-200 dark:border-slate-700">
                                @template.ashlar.components.empty-state(
                                icon = Icon.SEARCH,
                                title = "No results found",
                                body = @`Try adjusting your filters or search terms.`
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">With an action</p>
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
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Without an icon</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The icon is optional - the component stays complete without one.</p>
                            <div class="mt-3 rounded-lg border border-slate-200 dark:border-slate-700">
                                @template.ashlar.components.empty-state(
                                title = "No activity in the last 30 days"
                                )
                            </div>
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composition</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Empty State is built to nest inside something else's own content region, unaware it's inside it - most often a <a href="${contextPath}/components/card" class="text-blue-700 hover:underline dark:text-blue-400">Card</a>'s body, or a <a href="${contextPath}/components/table" class="text-blue-700 hover:underline dark:text-blue-400">Table</a>'s own empty-result region, which composes this component directly.</p>
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

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Ashlar provides</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>A real &lt;h3&gt; for the title, so it participates in the page's own heading structure.</li>
                                <li>A decorative icon with no accessible name of its own - the title already says the same thing in words, so an icon label here would be redundant rather than additional.</li>
                            </ul>
                        </div>
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>A heading level for the page that makes sense with this component's title landing at &lt;h3&gt;.</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div>
                    @template.layout.api-reference(apiParams = emptyStateApi)
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/skeleton" class="text-blue-700 hover:underline dark:text-blue-400">Skeleton</a> for content that hasn't loaded yet, rather than content that loaded empty &middot;
                        <a href="${contextPath}/components/card" class="text-blue-700 hover:underline dark:text-blue-400">Card</a> and
                        <a href="${contextPath}/components/table" class="text-blue-700 hover:underline dark:text-blue-400">Table</a>, its usual hosts
                    </p>
                </div>
            </div>
        </section>
`
)
