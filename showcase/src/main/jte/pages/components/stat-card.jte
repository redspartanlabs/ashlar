@import dev.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import dev.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

<%-- Not invented as a generic "metric" abstraction - documented exactly
     as the actual template is: label/value/description/change, nothing
     more, no formatting or calculation of its own. This is also NOT
     built on top of Card despite the visually matching bordered surface
     - see Pitfalls. --%>
!{String backtick = "`";}
!{String quickStartCode = """
    @template.ashlar.components.stat-card(
        label = "Active users",
        value = "2,481"
    )""";}
!{String withSupportingTextCode = """
    @template.ashlar.components.stat-card(
        label = "Records migrated",
        value = "12,481",
        description = "Across the selected date range"
    )""";}
!{String trendBadgeCode = "@template.ashlar.components.stat-card(\n"
        + "    label = \"Revenue\",\n"
        + "    value = \"$128.4K\",\n"
        + "    description = \"vs. $114.7K last month\",\n"
        + "    change = @" + backtick + "@template.ashlar.components.badge(text = \"+12%\", variant = \"Success\")" + backtick + "\n"
        + ")\n"
        + "@template.ashlar.components.stat-card(\n"
        + "    label = \"Avg. response time\",\n"
        + "    value = \"142ms\",\n"
        + "    description = \"vs. 118ms last month\",\n"
        + "    change = @" + backtick + "@template.ashlar.components.badge(text = \"+24ms\", variant = \"Danger\")" + backtick + "\n"
        + ")";}
!{String nonNumericCode = "@template.ashlar.components.stat-card(\n"
        + "    label = \"System status\",\n"
        + "    value = \"Operational\",\n"
        + "    change = @" + backtick + "@template.ashlar.components.badge(text = \"All systems normal\", variant = \"Success\")" + backtick + "\n"
        + ")";}
!{String longLabelCode = """
    @template.ashlar.components.stat-card(
        label = "Quarterly compliance reports generated for enterprise accounts",
        value = "1,204,981",
        description = "Includes archived and superseded reports"
    )""";}
!{String dashboardCode = "@template.ashlar.components.stat-card(\n"
        + "    label = \"Revenue\",\n"
        + "    value = \"$128.4K\",\n"
        + "    change = @" + backtick + "@template.ashlar.components.badge(text = \"+12%\", variant = \"Success\")" + backtick + "\n"
        + ")\n"
        + "@template.ashlar.components.stat-card(\n"
        + "    label = \"Active accounts\",\n"
        + "    value = \"342\"\n"
        + ")\n"
        + "@template.ashlar.components.stat-card(\n"
        + "    label = \"Open support tickets\",\n"
        + "    value = \"18\",\n"
        + "    change = @" + backtick + "@template.ashlar.components.badge(text = \"+5\", variant = \"Warning\")" + backtick + "\n"
        + ")\n"
        + "@template.ashlar.components.stat-card(\n"
        + "    label = \"Uptime\",\n"
        + "    value = \"99.98%\"\n"
        + ")";}
!{List<ApiParam> statCardApi = List.of(
    ApiParam.required("label", "String", "Rendered as a plain <dt> - never a heading, so a dashboard of several of these doesn't turn heading navigation into a list of near-identical stops."),
    ApiParam.required("value", "String", "Displayed exactly as given - no formatting, parsing, or calculation. The caller decides how the number should read."),
    ApiParam.optional("description", "String", "\"\"", "A second <dd> under the same <dt>, describing the metric as a whole - not a child of the value itself."),
    ApiParam.optional("change", "gg.jte.Content", "null", "Typically a composed Badge showing a directional change. This component has no opinion on whether a given change is good or bad - only the caller knows that.")
);}
!{List<PageAnchor> sections = List.of(
    new PageAnchor("quick-start", "Quick Start"),
    new PageAnchor("variants", "Variants & States"),
    new PageAnchor("composition", "Composition"),
    new PageAnchor("pitfalls", "Pitfalls"),
    new PageAnchor("accessibility", "Accessibility"),
    new PageAnchor("api-reference", "API Reference"),
    new PageAnchor("related", "Related Components")
);}

@template.layout.page(
contextPath = contextPath,
title = "Stat / Metric Card",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "stat-card", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A single KPI-style metric in its own small surface - dashboards, result pages, summary sections. No JavaScript, no formatting or calculation: the caller supplies the value exactly as it should read.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3 max-w-xs">
                    @template.ashlar.components.stat-card(label = "Active users", value = "2,481")
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
                        <div class="mt-3 max-w-xs">
                            @template.ashlar.components.stat-card(
                            label = "Active users",
                            value = "2,481"
                            )
                        </div>
                    </div>

                    @template.layout.example(
                    label = "With supporting text",
                    code = withSupportingTextCode,
                    content = @`
                        <div class="max-w-xs">
                            @template.ashlar.components.stat-card(
                            label = "Records migrated",
                            value = "12,481",
                            description = "Across the selected date range"
                            )
                        </div>
                    `
                    )

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">With a trend badge</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The component draws no conclusions about direction - the caller decides whether a change is good or bad and picks the Badge variant accordingly.</p>
                        @template.layout.example(
                        code = trendBadgeCode,
                        content = @`
                            <div class="mt-3 grid gap-4 sm:grid-cols-2">
                                @template.ashlar.components.stat-card(
                                label = "Revenue",
                                value = "$128.4K",
                                description = "vs. $114.7K last month",
                                change = @`@template.ashlar.components.badge(text = "+12%", variant = "Success")`
                                )
                                @template.ashlar.components.stat-card(
                                label = "Avg. response time",
                                value = "142ms",
                                description = "vs. 118ms last month",
                                change = @`@template.ashlar.components.badge(text = "+24ms", variant = "Danger")`
                                )
                            </div>
                        `
                        )
                    </div>

                    @template.layout.example(
                    label = "Non-numeric value",
                    code = nonNumericCode,
                    content = @`
                        <div class="max-w-xs">
                            @template.ashlar.components.stat-card(
                            label = "System status",
                            value = "Operational",
                            change = @`@template.ashlar.components.badge(text = "All systems normal", variant = "Success")`
                            )
                        </div>
                    `
                    )

                    @template.layout.example(
                    label = "Long label and value",
                    code = longLabelCode,
                    content = @`
                        <div class="max-w-xs">
                            @template.ashlar.components.stat-card(
                            label = "Quarterly compliance reports generated for enterprise accounts",
                            value = "1,204,981",
                            description = "Includes archived and superseded reports"
                            )
                        </div>
                    `
                    )

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">In a dashboard layout</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Several stat cards side by side, the way they'd appear at the top of a real dashboard.</p>
                        @template.layout.example(
                        code = dashboardCode,
                        content = @`
                            <div class="mt-3 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                                @template.ashlar.components.stat-card(
                                label = "Revenue",
                                value = "$128.4K",
                                change = @`@template.ashlar.components.badge(text = "+12%", variant = "Success")`
                                )
                                @template.ashlar.components.stat-card(
                                label = "Active accounts",
                                value = "342"
                                )
                                @template.ashlar.components.stat-card(
                                label = "Open support tickets",
                                value = "18",
                                change = @`@template.ashlar.components.badge(text = "+5", variant = "Warning")`
                                )
                                @template.ashlar.components.stat-card(
                                label = "Uptime",
                                value = "99.98%"
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
                <p class="text-sm text-slate-500 dark:text-slate-400">The <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">change</code> slot exists specifically to hold a composed <a href="${contextPath}/components/badge" class="text-blue-700 hover:underline dark:text-blue-400">Badge</a> - reusing its existing color vocabulary (Success/Warning/Danger) rather than this component inventing a second one it has no way to apply correctly, since only the caller knows whether a given change is actually good or bad for that particular metric.</p>
            `
            )
        </section>

        <section id="pitfalls" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Pitfalls & Troubleshooting",
            content = @`
                <ul class="list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">This is not built on top of Card</strong>, despite the visually matching bordered surface. It has no <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">title</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">body</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">footer</code> - its own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">label</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">value</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">description</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">change</code> contract is unrelated to Card's.</li>
                </ul>
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
                            <li>Native &lt;dl&gt;/&lt;dt&gt;/&lt;dd&gt; semantics for the label/value relationship - no ARIA needed, since the term-and-its-value pairing is already what &lt;dl&gt; means.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>A <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">value</code> already formatted the way it should be read aloud - this component passes it through unchanged.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.layout.api-reference(apiParams = statCardApi)
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/badge" class="text-blue-700 hover:underline dark:text-blue-400">Badge</a>, the usual content of the change slot &middot;
                    <a href="${contextPath}/components/card" class="text-blue-700 hover:underline dark:text-blue-400">Card</a>, a visually similar but independently-built surface
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "stat-card", contextPath = contextPath)
    </div>
`
)
