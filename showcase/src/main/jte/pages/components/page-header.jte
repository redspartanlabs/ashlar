@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import com.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

<%-- A genuine, unavoidable exception on this specific page: Page Header's
     own job is to render a real h1, so demonstrating it live puts
     several more h1 elements in the body below the showcase page's own
     real h1 (rendered by component-header.jte). Same category of
     exception as Navbar's own shared "Menu" h2 on other pages - a real
     component faithfully rendering its own correct heading level, not a
     page-authoring defect worth hiding or renaming. --%>
!{String backtick = "`";}
!{String quickStartCode = """
    @template.ashlar.utilities.page-header(
        title = "Dashboard",
        description = "Overview of system activity."
    )""";}
!{List<ApiParam> pageHeaderApi = List.of(
    ApiParam.required("title", "String", "Rendered as a real <h1> - the page's one primary heading, with no option to render at a different level."),
    ApiParam.optional("description", "String", "\"\"", "Supporting text below the title."),
    ApiParam.optional("actions", "gg.jte.Content", "null", "Composed Button(s) or other controls - which actions exist, how many, and what they do is entirely the calling page's decision.")
);}
!{List<PageAnchor> sections = List.of(
    new PageAnchor("quick-start", "Quick Start"),
    new PageAnchor("how-to-use", "How to Use"),
    new PageAnchor("variants", "Variants & States"),
    new PageAnchor("composition", "Composition"),
    new PageAnchor("accessibility", "Accessibility"),
    new PageAnchor("api-reference", "API Reference"),
    new PageAnchor("related", "Related Components")
);}
!{String titleOnlyCode = """
    @template.ashlar.utilities.page-header(title = "Dashboard")""";}
!{String titleDescriptionCode = """
    @template.ashlar.utilities.page-header(
        title = "Dashboard",
        description = "Overview of system activity."
    )""";}
!{String titleActionsCode = "@template.ashlar.utilities.page-header(\n"
        + "    title = \"Dashboard\",\n"
        + "    actions = @" + backtick + "@template.ashlar.components.button(text = \"New report\", type = \"Primary\")" + backtick + "\n"
        + ")";}
!{String titleDescActionsCode = "@template.ashlar.utilities.page-header(\n"
        + "    title = \"Dashboard\",\n"
        + "    description = \"Overview of system activity.\",\n"
        + "    actions = @" + backtick + "@template.ashlar.components.button(text = \"New report\", type = \"Primary\")" + backtick + "\n"
        + ")";}
!{String multipleActionsCode = "@template.ashlar.utilities.page-header(\n"
        + "    title = \"Accounts\",\n"
        + "    description = \"Manage enterprise accounts and their access.\",\n"
        + "    actions = @" + backtick + "\n"
        + "        @template.ashlar.components.button(text = \"Export\", type = \"Secondary\")\n"
        + "        @template.ashlar.components.button(text = \"Invite member\", type = \"Ghost\")\n"
        + "        @template.ashlar.components.button(text = \"New account\", type = \"Primary\")\n"
        + "    " + backtick + "\n"
        + ")";}
!{String longTitleCode = "@template.ashlar.utilities.page-header(\n"
        + "    title = \"Quarterly Compliance and Access Review for Enterprise Accounts\",\n"
        + "    actions = @" + backtick + "@template.ashlar.components.button(text = \"Export\", type = \"Secondary\")" + backtick + "\n"
        + ")";}
!{String longDescriptionCode = """
    @template.ashlar.utilities.page-header(
        title = "Audit Log",
        description = "A complete, chronological record of every access change, permission grant, and administrative action taken across all enterprise accounts, retained for the duration required by your organization's compliance policy."
    )""";}
!{String mobileLayoutCode = "@template.ashlar.utilities.page-header(\n"
        + "    title = \"Dashboard\",\n"
        + "    description = \"Overview of system activity.\",\n"
        + "    actions = @" + backtick + "\n"
        + "        @template.ashlar.components.button(text = \"Export\", type = \"Secondary\")\n"
        + "        @template.ashlar.components.button(text = \"New report\", type = \"Primary\")\n"
        + "    " + backtick + "\n"
        + ")";}

@template.layout.page(
contextPath = contextPath,
title = "Page Header",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "page-header", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A layout utility for a page's own title, optional description, and optional application-supplied actions. No surface of its own, no JavaScript - it owns structure, not appearance or behavior.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                    @template.ashlar.utilities.page-header(
                    title = "Dashboard",
                    description = "Overview of system activity."
                    )
                </div>
            `
            )
        </section>

        <section id="how-to-use" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "How to Use",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Use exactly one Page Header per page, at the top of your <a href="${contextPath}/components/page-shell" class="text-blue-700 hover:underline dark:text-blue-400">Page Shell</a>, for the page's one real &lt;h1&gt;. For a subsection heading beneath it, that's <a href="${contextPath}/components/page-section" class="text-blue-700 hover:underline dark:text-blue-400">Page Section</a>'s job, not a second Page Header.</p>
            `
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
                    @template.layout.example(
                    label = "Title only",
                    code = titleOnlyCode,
                    content = @`
                        <div class="rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                            @template.ashlar.utilities.page-header(title = "Dashboard")
                        </div>
                    `
                    )

                    @template.layout.example(
                    label = "Title + description",
                    code = titleDescriptionCode,
                    content = @`
                        <div class="rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                            @template.ashlar.utilities.page-header(
                            title = "Dashboard",
                            description = "Overview of system activity."
                            )
                        </div>
                    `
                    )

                    @template.layout.example(
                    label = "Title + actions",
                    code = titleActionsCode,
                    content = @`
                        <div class="rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                            @template.ashlar.utilities.page-header(
                            title = "Dashboard",
                            actions = @`@template.ashlar.components.button(text = "New report", type = "Primary")`
                            )
                        </div>
                    `
                    )

                    @template.layout.example(
                    label = "Title + description + actions",
                    code = titleDescActionsCode,
                    content = @`
                        <div class="rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                            @template.ashlar.utilities.page-header(
                            title = "Dashboard",
                            description = "Overview of system activity.",
                            actions = @`@template.ashlar.components.button(text = "New report", type = "Primary")`
                            )
                        </div>
                    `
                    )

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Multiple actions</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Actions are supplied by the caller - Page Header only arranges them and lets them wrap.</p>
                        @template.layout.example(
                        code = multipleActionsCode,
                        content = @`
                            <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                                @template.ashlar.utilities.page-header(
                                title = "Accounts",
                                description = "Manage enterprise accounts and their access.",
                                actions = @`
                                    @template.ashlar.components.button(text = "Export", type = "Secondary")
                                    @template.ashlar.components.button(text = "Invite member", type = "Ghost")
                                    @template.ashlar.components.button(text = "New account", type = "Primary")
                                `
                                )
                            </div>
                        `
                        )
                    </div>

                    @template.layout.example(
                    label = "Long title",
                    code = longTitleCode,
                    content = @`
                        <div class="rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                            @template.ashlar.utilities.page-header(
                            title = "Quarterly Compliance and Access Review for Enterprise Accounts",
                            actions = @`@template.ashlar.components.button(text = "Export", type = "Secondary")`
                            )
                        </div>
                    `
                    )

                    @template.layout.example(
                    label = "Long description",
                    code = longDescriptionCode,
                    content = @`
                        <div class="rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                            @template.ashlar.utilities.page-header(
                            title = "Audit Log",
                            description = "A complete, chronological record of every access change, permission grant, and administrative action taken across all enterprise accounts, retained for the duration required by your organization's compliance policy."
                            )
                        </div>
                    `
                    )

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Narrow / mobile layout</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Below the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">sm</code> breakpoint the actions move below the heading and wrap instead of overflowing.</p>
                        @template.layout.example(
                        code = mobileLayoutCode,
                        content = @`
                            <div class="mt-3 max-w-xs rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                                @template.ashlar.utilities.page-header(
                                title = "Dashboard",
                                description = "Overview of system activity.",
                                actions = @`
                                    @template.ashlar.components.button(text = "Export", type = "Secondary")
                                    @template.ashlar.components.button(text = "New report", type = "Primary")
                                `
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
                <p class="text-sm text-slate-500 dark:text-slate-400">Page Header is the top block of the same layout family as <a href="${contextPath}/components/page-shell" class="text-blue-700 hover:underline dark:text-blue-400">Page Shell</a> and <a href="${contextPath}/components/page-section" class="text-blue-700 hover:underline dark:text-blue-400">Page Section</a> - see Page Shell's own page for the full composition shown together. <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">actions</code> accepts ordinary composed Button calls, the same shape as Card's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">footer</code> slot.</p>
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
                            <li>A real &lt;h1&gt; - the page's one primary heading, with no risk of landing at the wrong level since there's no parameter to change it.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Using this exactly once per page - a second &lt;h1&gt; elsewhere would contradict the one real page title.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.layout.api-reference(apiParams = pageHeaderApi)
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/page-shell" class="text-blue-700 hover:underline dark:text-blue-400">Page Shell</a>, the outer wrapper this sits inside &middot;
                    <a href="${contextPath}/components/page-section" class="text-blue-700 hover:underline dark:text-blue-400">Page Section</a> for the subsection heading beneath it &middot;
                    <a href="${contextPath}/components/form-actions" class="text-blue-700 hover:underline dark:text-blue-400">Form Actions</a> for a form's own end-aligned buttons instead of page-level actions
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "page-header", contextPath = contextPath)
    </div>
`
)
