@import dev.redspartanlabs.ashlar.icon.Icon
@import dev.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import dev.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.components.list(\n"
        + "    items = @" + backtick + "\n"
        + "        @template.ashlar.components.list-item(label = \"Overview\")\n"
        + "        @template.ashlar.components.list-item(label = \"Settings\")\n"
        + "    " + backtick + "\n"
        + ")";}
!{String navIconsCode = "@template.ashlar.components.list(\n"
        + "    items = @" + backtick + "\n"
        + "        @template.ashlar.components.list-item(label = \"Account\", description = \"Profile, email, and password\", href = \"#\", icon = Icon.USER)\n"
        + "        @template.ashlar.components.list-item(label = \"Security\", description = \"Two-factor authentication and sessions\", href = \"#\", icon = Icon.SHIELD)\n"
        + "        @template.ashlar.components.list-item(label = \"Notifications\", description = \"Email and in-app alerts\", href = \"#\", icon = Icon.BELL)\n"
        + "    " + backtick + "\n"
        + ")";}
!{String trailingContentCode = "@template.ashlar.components.list(\n"
        + "    items = @" + backtick + "\n"
        + "        @template.ashlar.components.list-item(\n"
        + "            label = \"Plan\",\n"
        + "            trailing = @" + backtick + "<span class=\"font-medium text-slate-700 dark:text-slate-300\">Enterprise</span>" + backtick + "\n"
        + "        )\n"
        + "        @template.ashlar.components.list-item(\n"
        + "            label = \"Status\",\n"
        + "            trailing = @" + backtick + "@template.ashlar.components.badge(text = \"Active\", variant = \"Success\")" + backtick + "\n"
        + "        )\n"
        + "        @template.ashlar.components.list-item(\n"
        + "            label = \"Renews\",\n"
        + "            trailing = @" + backtick + "<span class=\"font-medium text-slate-700 dark:text-slate-300\">Jan 14, 2027</span>" + backtick + "\n"
        + "        )\n"
        + "    " + backtick + "\n"
        + ")";}
!{String orderedCode = "@template.ashlar.components.list(\n"
        + "    ordered = true,\n"
        + "    items = @" + backtick + "\n"
        + "        @template.ashlar.components.list-item(label = \"Submit the request\", description = \"Fill in the account details and reason for change\")\n"
        + "        @template.ashlar.components.list-item(label = \"Manager review\", description = \"Routed automatically based on account owner\")\n"
        + "        @template.ashlar.components.list-item(label = \"Access granted\", description = \"Usually within one business day\")\n"
        + "    " + backtick + "\n"
        + ")";}
!{String longContentCode = "@template.ashlar.components.list(\n"
        + "    items = @" + backtick + "\n"
        + "        @template.ashlar.components.list-item(\n"
        + "            label = \"Quarterly compliance report for enterprise accounts\",\n"
        + "            description = \"Generated automatically on the first business day of each quarter and distributed to every account owner on file.\",\n"
        + "            icon = Icon.FILE_TEXT,\n"
        + "            trailing = @" + backtick + "@template.ashlar.components.badge(text = \"Draft\")" + backtick + "\n"
        + "        )\n"
        + "    " + backtick + "\n"
        + ")";}
!{List<ApiParam> listApi = List.of(
    ApiParam.required("items", "gg.jte.Content", "One or more list-item(...) calls."),
    ApiParam.optional("ordered", "boolean", "false", "true renders <ol> instead of <ul> - a real semantic choice (position is meaningful), not a styling option.")
);}
!{List<ApiParam> listItemApi = List.of(
    ApiParam.required("label", "String", "The row's primary text."),
    ApiParam.optional("description", "String", "\"\"", "Secondary text below the label."),
    ApiParam.optional("href", "String", "null", "Set this to make the whole row a real <a> with a trailing chevron. Left unset, the row is non-interactive and gets no chevron."),
    ApiParam.optional("icon", "Icon", "null", "Always decorative - leading, before the label."),
    ApiParam.optional("trailing", "gg.jte.Content", "null", "Arbitrary content at the row's end - a value, a timestamp, a composed Badge.")
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
title = "List",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "list", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A semantic collection of related rows - a bordered, divided &lt;ul&gt; or &lt;ol&gt; surface, not a data table. No JavaScript: nothing here sorts, filters, or paginates. Each row is a separate item call the caller composes.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3 max-w-md">
                    @template.ashlar.components.list(
                    items = @`
                        @template.ashlar.components.list-item(label = "Overview")
                        @template.ashlar.components.list-item(label = "Settings")
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
                <p class="text-sm text-slate-500 dark:text-slate-400">Reach for List for a static set of related rows - settings links, a key/value summary, a short feed - where nothing needs sorting, filtering, searching, or pagination. The moment any of those become real requirements, that's <a href="${contextPath}/components/data-table" class="text-blue-700 hover:underline dark:text-blue-400">Data Table</a>'s job, not something to bolt onto this one. Set <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">ordered = true</code> only when the position of a row is itself meaningful information (a numbered procedure), not just to get numeral-looking bullets.</p>
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
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Navigation-style, with icons</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A row with an <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">href</code> becomes a real link and gets a trailing chevron automatically.</p>
                        @template.layout.example(
                        code = navIconsCode,
                        content = @`
                            <div class="mt-3 max-w-md">
                                @template.ashlar.components.list(
                                items = @`
                                    @template.ashlar.components.list-item(label = "Account", description = "Profile, email, and password", href = "#", icon = Icon.USER)
                                    @template.ashlar.components.list-item(label = "Security", description = "Two-factor authentication and sessions", href = "#", icon = Icon.SHIELD)
                                    @template.ashlar.components.list-item(label = "Notifications", description = "Email and in-app alerts", href = "#", icon = Icon.BELL)
                                `
                                )
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Trailing content</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A Badge, a value, or any other small composed element can sit at the end of a row - useful for metadata-style pairs.</p>
                        @template.layout.example(
                        code = trailingContentCode,
                        content = @`
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
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Ordered</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400"><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">ordered = true</code> renders an &lt;ol&gt; - position itself is part of what the list communicates.</p>
                        @template.layout.example(
                        code = orderedCode,
                        content = @`
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
                        `
                        )
                    </div>

                    @template.layout.example(
                    label = "Long content wraps",
                    code = longContentCode,
                    content = @`
                        <div class="max-w-md">
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
                <p class="text-sm text-slate-500 dark:text-slate-400">List is the wrapper half of a wrapper/item pair - the same split Breadcrumbs, Pagination, and Stepper each use for their own entries. <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">list-item</code> is not a separate catalog component; it only ever exists composed into List's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">items</code> slot, so its full API is documented here, in the table below. A List commonly sits inside a Card's body, shown here the way it would appear in a real page.</p>
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
                            <li>A real native &lt;ul&gt; or &lt;ol&gt;, so "a list" is already communicated with no role or aria-label needed.</li>
                            <li>A linked row that is one whole &lt;a&gt; - the visible click target and the keyboard focus target are always the same element, never a link buried inside otherwise-static row text.</li>
                            <li>A decorative leading icon and trailing chevron - the row's meaning comes entirely from its label/description text.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Your own landmark/label around the whole list if this particular one needs to be identifiable to assistive tech as more than "a list" - there's no APG-assigned name for a generic list to borrow.</li>
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
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">list</p>
                        @template.layout.api-reference(apiParams = listApi)
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">list-item</p>
                        @template.layout.api-reference(apiParams = listItemApi)
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
                    <a href="${contextPath}/components/data-table" class="text-blue-700 hover:underline dark:text-blue-400">Data Table</a> for sorting, searching, or paginating rows &middot;
                    <a href="${contextPath}/components/breadcrumbs" class="text-blue-700 hover:underline dark:text-blue-400">Breadcrumbs</a>,
                    <a href="${contextPath}/components/pagination" class="text-blue-700 hover:underline dark:text-blue-400">Pagination</a>, and
                    <a href="${contextPath}/components/stepper" class="text-blue-700 hover:underline dark:text-blue-400">Stepper</a>, the same wrapper/item split applied to other kinds of entries &middot;
                    <a href="${contextPath}/components/card" class="text-blue-700 hover:underline dark:text-blue-400">Card</a>, a common host
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "list", contextPath = contextPath)
    </div>
`
)
