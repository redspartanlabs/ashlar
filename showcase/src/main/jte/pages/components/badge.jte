@import com.redspartanlabs.ashlar.icon.Icon
@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import com.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

!{String quickStartCode = """
    @template.ashlar.components.badge(
        text = "Active",
        variant = "Success"
    )""";}
!{String semanticVariantsCode = """
    @template.ashlar.components.badge(text = "Draft", variant = "Neutral")
    @template.ashlar.components.badge(text = "In Review", variant = "Info")
    @template.ashlar.components.badge(text = "Active", variant = "Success")
    @template.ashlar.components.badge(text = "High Priority", variant = "Warning")
    @template.ashlar.components.badge(text = "Failed", variant = "Danger")""";}
!{String leadingIconCode = """
    @template.ashlar.components.badge(text = "Completed", variant = "Success", icon = Icon.CHECK_CIRCLE)
    @template.ashlar.components.badge(text = "Needs Attention", variant = "Warning", icon = Icon.WARNING_TRIANGLE)
    @template.ashlar.components.badge(text = "Rejected", variant = "Danger", icon = Icon.X_CIRCLE)""";}
!{String inlineCode = """
    @template.ashlar.components.badge(text = "Production", variant = "Success")
    @template.ashlar.components.badge(text = "High Priority", variant = "Warning")""";}
!{List<ApiParam> badgeApi = List.of(
    ApiParam.required("text", "String", "The only carrier of meaning - color and the optional icon are always reinforcing, never the sole signal."),
    ApiParam.optional("variant", "String", "\"Neutral\"", "Neutral/Info/Success/Warning/Danger. Unrecognized values silently fall back to Neutral."),
    ApiParam.optional("icon", "Icon", "null", "Always decorative (aria-hidden) - the text already says what the icon would.")
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
title = "Badge",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "badge", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A static label for status, category, or metadata - plain HTML and CSS, no JavaScript, not a button or a control.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3">
                    @template.ashlar.components.badge(text = "Active", variant = "Success")
                </div>
            `
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
                    @template.layout.example(
                    label = "Semantic variants",
                    code = semanticVariantsCode,
                    content = @`
                        <div class="flex flex-wrap items-center gap-3">
                            @template.ashlar.components.badge(text = "Draft", variant = "Neutral")
                            @template.ashlar.components.badge(text = "In Review", variant = "Info")
                            @template.ashlar.components.badge(text = "Active", variant = "Success")
                            @template.ashlar.components.badge(text = "High Priority", variant = "Warning")
                            @template.ashlar.components.badge(text = "Failed", variant = "Danger")
                        </div>
                    `
                    )

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">With an optional leading icon</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Reinforcing, never the sole carrier of meaning - the text alone already says it.</p>
                        @template.layout.example(
                        code = leadingIconCode,
                        content = @`
                            <div class="mt-3 flex flex-wrap items-center gap-3">
                                @template.ashlar.components.badge(text = "Completed", variant = "Success", icon = Icon.CHECK_CIRCLE)
                                @template.ashlar.components.badge(text = "Needs Attention", variant = "Warning", icon = Icon.WARNING_TRIANGLE)
                                @template.ashlar.components.badge(text = "Rejected", variant = "Danger", icon = Icon.X_CIRCLE)
                            </div>
                        `
                        )
                    </div>

                    @template.layout.example(
                    label = "Inline with text",
                    code = inlineCode,
                    content = @`
                        <p class="text-sm text-slate-700 dark:text-slate-300">
                            Job Header JOB-1042 is currently
                            @template.ashlar.components.badge(text = "Production", variant = "Success")
                            and flagged
                            @template.ashlar.components.badge(text = "High Priority", variant = "Warning")
                            for review.
                        </p>
                    `
                    )
                </div>
            `
            )
        </section>

        <section id="composition" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Composition",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Badge is a common passenger inside other components' own content slots rather than something they build in for you: <a href="${contextPath}/components/stat-card" class="text-blue-700 hover:underline dark:text-blue-400">Stat Card</a>'s <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">change</code> slot is designed specifically to hold one, and a Table or Data Table status column reads faster as a badge than as plain text.</p>
                <div class="mt-3 overflow-x-auto rounded-lg border border-slate-200 dark:border-slate-700">
                    @template.ashlar.components.table(
                    caption = "Job status",
                    columns = @`
                        @template.ashlar.components.table-column(label = "Job Header")
                        @template.ashlar.components.table-column(label = "Status")
                    `,
                    rows = @`
                        @template.ashlar.components.table-row(cells = @`
                            @template.ashlar.components.table-cell(text = "JOB-1001", header = true, mono = true, nowrap = true)
                            @template.ashlar.components.table-cell(content = @`@template.ashlar.components.badge(text = "Active", variant = "Success")`)
                        `)
                        @template.ashlar.components.table-row(cells = @`
                            @template.ashlar.components.table-cell(text = "JOB-1002", header = true, mono = true, nowrap = true)
                            @template.ashlar.components.table-cell(content = @`@template.ashlar.components.badge(text = "Pending", variant = "Warning")`)
                        `)
                        @template.ashlar.components.table-row(cells = @`
                            @template.ashlar.components.table-cell(text = "JOB-1003", header = true, mono = true, nowrap = true)
                            @template.ashlar.components.table-cell(content = @`@template.ashlar.components.badge(text = "Completed", variant = "Success")`)
                        `)
                        @template.ashlar.components.table-row(cells = @`
                            @template.ashlar.components.table-cell(text = "JOB-1004", header = true, mono = true, nowrap = true)
                            @template.ashlar.components.table-cell(content = @`@template.ashlar.components.badge(text = "Failed", variant = "Danger")`)
                        `)
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
                            <li>A plain, inert &lt;span&gt; with no role - this is static metadata, not a live-region announcement. A page that genuinely needs one to be announced wraps it in its own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-live</code> region.</li>
                            <li>A decorative icon - never a second, competing accessible name alongside the text.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Text that stands on its own without color - "Failed", not just a red dot. Nothing here distinguishes variants for anyone who can't see the accent color.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.layout.api-reference(apiParams = badgeApi)
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/stat-card" class="text-blue-700 hover:underline dark:text-blue-400">Stat Card</a>, whose <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">change</code> slot is designed to hold one &middot;
                    <a href="${contextPath}/components/alert" class="text-blue-700 hover:underline dark:text-blue-400">Alert</a> for a larger, persistent severity message instead of inline metadata &middot;
                    <a href="${contextPath}/components/toast" class="text-blue-700 hover:underline dark:text-blue-400">Toast</a> for a transient one
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "badge", contextPath = contextPath)
    </div>
`
)
