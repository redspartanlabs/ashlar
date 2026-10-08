@import dev.redspartanlabs.ashlar.icon.Icon
@import dev.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import dev.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

<%-- No Pitfalls section: the two facts that might look like pitfalls
     (date isn't a real time element, title isn't a heading) are both
     deliberate API contract decisions with their own stated rationale,
     already covered in How to Use/Accessibility - duplicating them under
     a manufactured Pitfalls heading would just be restating the same
     fact twice. --%>
!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.components.timeline(\n"
        + "    items = @" + backtick + "\n"
        + "        @template.ashlar.components.timeline-item(date = \"March 2026\", title = \"Version 2.4.0\")\n"
        + "        @template.ashlar.components.timeline-item(date = \"January 2026\", title = \"Version 2.3.0\")\n"
        + "    " + backtick + "\n"
        + ")";}
!{String releaseHistoryCode = "@template.ashlar.components.timeline(\n"
        + "    items = @" + backtick + "\n"
        + "        @template.ashlar.components.timeline-item(\n"
        + "            date = \"March 2026\",\n"
        + "            title = \"Version 2.4.0\",\n"
        + "            content = @" + backtick + "<p>Adds bulk export and improves Data Table sort performance.</p>" + backtick + "\n"
        + "        )\n"
        + "        @template.ashlar.components.timeline-item(\n"
        + "            date = \"January 2026\",\n"
        + "            title = \"Version 2.3.0\",\n"
        + "            content = @" + backtick + "<p>Introduces role-based approval workflows for high-value transactions.</p>" + backtick + "\n"
        + "        )\n"
        + "        @template.ashlar.components.timeline-item(\n"
        + "            date = \"October 2025\",\n"
        + "            title = \"Version 2.2.0\",\n"
        + "            content = @" + backtick + "<p>Moves the platform from a single-region deployment to a multi-region architecture.</p>" + backtick + "\n"
        + "        )\n"
        + "    " + backtick + "\n"
        + ")";}
!{String orderStatusCode = "@template.ashlar.components.timeline(\n"
        + "    items = @" + backtick + "\n"
        + "        @template.ashlar.components.timeline-item(\n"
        + "            date = \"Sep 21, 9:02 AM\",\n"
        + "            title = \"Order placed\",\n"
        + "            variant = \"Success\",\n"
        + "            icon = Icon.CHECK\n"
        + "        )\n"
        + "        @template.ashlar.components.timeline-item(\n"
        + "            date = \"Sep 21, 11:40 AM\",\n"
        + "            title = \"Payment confirmed\",\n"
        + "            variant = \"Success\",\n"
        + "            icon = Icon.CHECK\n"
        + "        )\n"
        + "        @template.ashlar.components.timeline-item(\n"
        + "            date = \"Sep 22, 8:15 AM\",\n"
        + "            title = \"Shipment delayed\",\n"
        + "            content = @" + backtick + "<p>Carrier reported a warehouse delay. New estimate: Sep 24.</p>" + backtick + ",\n"
        + "            variant = \"Warning\"\n"
        + "        )\n"
        + "        @template.ashlar.components.timeline-item(\n"
        + "            title = \"Out for delivery\",\n"
        + "            variant = \"Neutral\"\n"
        + "        )\n"
        + "    " + backtick + "\n"
        + ")";}
!{String withoutDatesCode = "@template.ashlar.components.timeline(\n"
        + "    items = @" + backtick + "\n"
        + "        @template.ashlar.components.timeline-item(title = \"Request submitted\")\n"
        + "        @template.ashlar.components.timeline-item(title = \"Manager review\")\n"
        + "        @template.ashlar.components.timeline-item(title = \"Access granted\")\n"
        + "    " + backtick + "\n"
        + ")";}
!{String longContentCode = "@template.ashlar.components.timeline(\n"
        + "    items = @" + backtick + "\n"
        + "        @template.ashlar.components.timeline-item(\n"
        + "            date = \"Q3 2026\",\n"
        + "            title = \"Quarterly compliance report for enterprise accounts\",\n"
        + "            content = @" + backtick + "<p>Generated automatically on the first business day of each quarter and distributed to every account owner on file, including a full audit trail of access changes made during the period.</p>" + backtick + "\n"
        + "        )\n"
        + "        @template.ashlar.components.timeline-item(\n"
        + "            date = \"Q2 2026\",\n"
        + "            title = \"Previous report\"\n"
        + "        )\n"
        + "    " + backtick + "\n"
        + ")";}
!{List<ApiParam> timelineApi = List.of(
    ApiParam.required("items", "gg.jte.Content", "One or more timeline-item(...) calls, in the order they should read - the component does not sort or validate chronological order.")
);}
!{List<ApiParam> timelineItemApi = List.of(
    ApiParam.required("title", "String", "Rendered as a plain paragraph, not a heading - a timeline entry isn't its own page section."),
    ApiParam.optional("date", "String", "\"\"", "Plain text, not wrapped in <time> - an arbitrary display string is often not valid <time> content without a machine-readable datetime this component can't reliably derive."),
    ApiParam.optional("content", "gg.jte.Content", "null", "Richer supporting content below the title."),
    ApiParam.optional("icon", "Icon", "null", "Shown inside the marker circle. Always decorative."),
    ApiParam.optional("variant", "String", "\"Neutral\"", "Reuses BadgeVariant directly: Neutral/Info/Success/Warning/Danger. Colors the marker and announces the variant name (e.g. \"Danger: \") sr-only before the title - never a guessed meaning like \"Failed\".")
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

@template.layout.page(
contextPath = contextPath,
title = "Timeline",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "timeline", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A vertical, chronological sequence of entries - release history, activity feeds, workflow progression. No JavaScript: the connector between entries is drawn per-item in pure CSS.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3 max-w-md">
                    @template.ashlar.components.timeline(
                    items = @`
                        @template.ashlar.components.timeline-item(date = "March 2026", title = "Version 2.4.0")
                        @template.ashlar.components.timeline-item(date = "January 2026", title = "Version 2.3.0")
                    `
                    )
                </div>
            `
            )
        </section>

        <section id="how-to-use" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "How to Use",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">The order you compose <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">timeline-item</code> calls in is exactly the order they render, top to bottom - there is no reordering, and the component never sorts or checks that the order you gave it is actually chronological. Pick whichever direction reads naturally for the case: the "Release history" example below lists newest-first, the way a changelog usually reads, while a workflow-progression timeline more often reads oldest-first. For a sequence that's also an interactive, step-by-step process rather than a record of what already happened, use <a href="${contextPath}/components/stepper" class="text-blue-700 hover:underline dark:text-blue-400">Stepper</a> instead.</p>
            `
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
                    @template.layout.example(
                    label = "Release history",
                    code = releaseHistoryCode,
                    content = @`
                        <div class="max-w-md">
                            @template.ashlar.components.timeline(
                            items = @`
                                @template.ashlar.components.timeline-item(
                                    date = "March 2026",
                                    title = "Version 2.4.0",
                                    content = @`<p>Adds bulk export and improves Data Table sort performance.</p>`
                                )
                                @template.ashlar.components.timeline-item(
                                    date = "January 2026",
                                    title = "Version 2.3.0",
                                    content = @`<p>Introduces role-based approval workflows for high-value transactions.</p>`
                                )
                                @template.ashlar.components.timeline-item(
                                    date = "October 2025",
                                    title = "Version 2.2.0",
                                    content = @`<p>Moves the platform from a single-region deployment to a multi-region architecture.</p>`
                                )
                            `
                            )
                        </div>
                    `
                    )

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Order status, with state and icons</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400"><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">variant</code> colors the marker; state is also announced to assistive tech, never color-only.</p>
                        @template.layout.example(
                        code = orderStatusCode,
                        content = @`
                            <div class="mt-3 max-w-md">
                                @template.ashlar.components.timeline(
                                items = @`
                                    @template.ashlar.components.timeline-item(
                                        date = "Sep 21, 9:02 AM",
                                        title = "Order placed",
                                        variant = "Success",
                                        icon = Icon.CHECK
                                    )
                                    @template.ashlar.components.timeline-item(
                                        date = "Sep 21, 11:40 AM",
                                        title = "Payment confirmed",
                                        variant = "Success",
                                        icon = Icon.CHECK
                                    )
                                    @template.ashlar.components.timeline-item(
                                        date = "Sep 22, 8:15 AM",
                                        title = "Shipment delayed",
                                        content = @`<p>Carrier reported a warehouse delay. New estimate: Sep 24.</p>`,
                                        variant = "Warning"
                                    )
                                    @template.ashlar.components.timeline-item(
                                        title = "Out for delivery",
                                        variant = "Neutral"
                                    )
                                `
                                )
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Without dates</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A workflow where sequence matters more than timing - <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">date</code> is entirely optional.</p>
                        @template.layout.example(
                        code = withoutDatesCode,
                        content = @`
                            <div class="mt-3 max-w-md">
                                @template.ashlar.components.timeline(
                                items = @`
                                    @template.ashlar.components.timeline-item(title = "Request submitted")
                                    @template.ashlar.components.timeline-item(title = "Manager review")
                                    @template.ashlar.components.timeline-item(title = "Access granted")
                                `
                                )
                            </div>
                        `
                        )
                    </div>

                    @template.layout.example(
                    label = "Long content wraps",
                    code = longContentCode,
                    content = @`
                        <div class="max-w-md">
                            @template.ashlar.components.timeline(
                            items = @`
                                @template.ashlar.components.timeline-item(
                                    date = "Q3 2026",
                                    title = "Quarterly compliance report for enterprise accounts",
                                    content = @`<p>Generated automatically on the first business day of each quarter and distributed to every account owner on file, including a full audit trail of access changes made during the period.</p>`
                                )
                                @template.ashlar.components.timeline-item(
                                    date = "Q2 2026",
                                    title = "Previous report"
                                )
                            `
                            )
                        </div>
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
                <p class="text-sm text-slate-500 dark:text-slate-400">Timeline is the wrapper half of a wrapper/item pair, the same split List and Stepper each use. <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">timeline-item</code> is not a separate catalog component; it only ever exists composed into Timeline's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">items</code> slot, documented here in the table below. Its <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">variant</code> reuses Badge's own vocabulary directly rather than a separate Timeline-specific set, so the same five values mean the same thing everywhere in the library. A Timeline commonly sits inside a Card's body, shown here the way it would appear in a real page.</p>
                <div class="mt-3 max-w-md">
                    @template.ashlar.components.card(
                    title = "Account activity",
                    body = @`
                        @template.ashlar.components.timeline(
                        items = @`
                            @template.ashlar.components.timeline-item(date = "2 hours ago", title = "Dana Whitfield updated billing contact", icon = Icon.TAG)
                            @template.ashlar.components.timeline-item(date = "Yesterday", title = "New API key generated", icon = Icon.TAG)
                            @template.ashlar.components.timeline-item(date = "3 days ago", title = "Account created", variant = "Success", icon = Icon.CHECK)
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
                            <li>A real native &lt;ol&gt;, since entries are genuinely chronological - position itself is real information here, the same reasoning Breadcrumbs applies.</li>
                            <li>A sr-only announcement of a non-neutral variant's name just before the title, since the marker's color and icon are both decorative - state is never communicated by color alone.</li>
                            <li>A plain paragraph, not a heading, for each entry's title - a dozen timeline entries don't each need to be their own stop in heading navigation.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>The actual chronological order - the component renders items in the exact order given and never sorts or validates it.</li>
                            <li>A real &lt;time&gt; element yourself inside <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">content</code> if a given entry's date genuinely needs to be machine-readable - the built-in <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">date</code> param is deliberately plain text only.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "API Reference",
            content = @`
                <div class="flex flex-col gap-6">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">timeline</p>
                        @template.layout.api-reference(apiParams = timelineApi)
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">timeline-item</p>
                        @template.layout.api-reference(apiParams = timelineItemApi)
                    </div>
                </div>
            `
            )
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/stepper" class="text-blue-700 hover:underline dark:text-blue-400">Stepper</a> for an interactive, in-progress process rather than a record of what already happened &middot;
                    <a href="${contextPath}/components/list" class="text-blue-700 hover:underline dark:text-blue-400">List</a>, the same wrapper/item split applied to non-chronological rows &middot;
                    <a href="${contextPath}/components/badge" class="text-blue-700 hover:underline dark:text-blue-400">Badge</a>, whose variant vocabulary this component's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">variant</code> reuses directly &middot;
                    <a href="${contextPath}/components/card" class="text-blue-700 hover:underline dark:text-blue-400">Card</a>, a common host
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "timeline", contextPath = contextPath)
    </div>
`
)
