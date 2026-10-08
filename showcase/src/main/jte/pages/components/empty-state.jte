@import dev.redspartanlabs.ashlar.icon.Icon
@import dev.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import dev.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

!{String backtick = "`";}
!{String quickStartCode = """
    @template.ashlar.components.empty-state(
        icon = Icon.INBOX,
        title = "No messages"
    )""";}
!{String withBodyCode = "@template.ashlar.components.empty-state(\n"
        + "    icon = Icon.SEARCH,\n"
        + "    title = \"No results found\",\n"
        + "    body = @" + backtick + "Try adjusting your filters or search terms." + backtick + "\n"
        + ")";}
!{String withActionCode = "@template.ashlar.components.empty-state(\n"
        + "    icon = Icon.FOLDER,\n"
        + "    title = \"No projects yet\",\n"
        + "    body = @" + backtick + "Create your first project to start tracking work." + backtick + ",\n"
        + "    action = @" + backtick + "\n"
        + "        @template.ashlar.components.button(text = \"Create Project\", type = \"Primary\")\n"
        + "    " + backtick + "\n"
        + ")";}
!{String withoutIconCode = """
    @template.ashlar.components.empty-state(
        title = "No activity in the last 30 days"
    )""";}
!{List<ApiParam> emptyStateApi = List.of(
    ApiParam.required("title", "String", "The component's entire reason to exist - rendered as a real <h3>, matching Card's and Alert's own title treatment."),
    ApiParam.optional("body", "gg.jte.Content", "null", "Supporting text below the title."),
    ApiParam.optional("icon", "Icon", "null", "Always decorative - the title already says in words what the icon would."),
    ApiParam.optional("action", "gg.jte.Content", "null", "Ordinary composed Button(s)/link(s) - this component has no action API of its own.")
);}
!{List<PageAnchor> sections = List.of(
    new PageAnchor("quick-start", "Quick Start"),
    new PageAnchor("variants", "Variants & States"),
    new PageAnchor("composition", "Composition"),
    new PageAnchor("accessibility", "Accessibility"),
    new PageAnchor("api-reference", "API Reference"),
    new PageAnchor("related", "Related Components")
);}

@template.layout.page(
contextPath = contextPath,
title = "Empty State",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "empty-state", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Content has loaded, and there is currently none of it - distinct from Skeleton, which is for content that hasn't loaded yet. No JavaScript; the action slot accepts real, composed Button content rather than owning its own action API.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3 rounded-lg border border-slate-200 dark:border-slate-700">
                    @template.ashlar.components.empty-state(icon = Icon.INBOX, title = "No messages")
                </div>
            `
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Basic</p>
                        <div class="mt-3 rounded-lg border border-slate-200 dark:border-slate-700">
                            @template.ashlar.components.empty-state(
                            icon = Icon.INBOX,
                            title = "No messages"
                            )
                        </div>
                    </div>

                    @template.layout.example(
                    label = "With body",
                    code = withBodyCode,
                    content = @`
                        <div class="rounded-lg border border-slate-200 dark:border-slate-700">
                            @template.ashlar.components.empty-state(
                            icon = Icon.SEARCH,
                            title = "No results found",
                            body = @`Try adjusting your filters or search terms.`
                            )
                        </div>
                    `
                    )

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">With an action</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The action is an ordinary Button, composed by the page - Empty State has no action API of its own.</p>
                        @template.layout.example(
                        code = withActionCode,
                        content = @`
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
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Without an icon</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The icon is optional - the component stays complete without one.</p>
                        @template.layout.example(
                        code = withoutIconCode,
                        content = @`
                            <div class="rounded-lg border border-slate-200 dark:border-slate-700">
                                @template.ashlar.components.empty-state(
                                title = "No activity in the last 30 days"
                                )
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
                <p class="text-sm text-slate-500 dark:text-slate-400">Empty State is built to nest inside something else's own content region, unaware it's inside it - most often a <a href="${contextPath}/components/card" class="text-blue-700 hover:underline dark:text-blue-400">Card</a>'s body, or a <a href="${contextPath}/components/table" class="text-blue-700 hover:underline dark:text-blue-400">Table</a>'s own empty-result region, which composes this component directly.</p>
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
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.layout.api-reference(apiParams = emptyStateApi)
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/skeleton" class="text-blue-700 hover:underline dark:text-blue-400">Skeleton</a> for content that hasn't loaded yet, rather than content that loaded empty &middot;
                    <a href="${contextPath}/components/card" class="text-blue-700 hover:underline dark:text-blue-400">Card</a> and
                    <a href="${contextPath}/components/table" class="text-blue-700 hover:underline dark:text-blue-400">Table</a>, its usual hosts
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "empty-state", contextPath = contextPath)
    </div>
`
)
