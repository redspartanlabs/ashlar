@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import com.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.components.breadcrumbs(\n"
        + "    items = @" + backtick + "\n"
        + "        @template.ashlar.components.breadcrumb-item(label = \"Home\", href = \"/\")\n"
        + "        @template.ashlar.components.breadcrumb-item(label = \"Widgets\", current = true)\n"
        + "    " + backtick + "\n"
        + ")";}
!{String basicHierarchyCode = "@template.ashlar.components.breadcrumbs(\n"
        + "    items = @" + backtick + "\n"
        + "        @template.ashlar.components.breadcrumb-item(label = \"Home\", href = \"/\")\n"
        + "        @template.ashlar.components.breadcrumb-item(label = \"Products\", href = \"/products\")\n"
        + "        @template.ashlar.components.breadcrumb-item(label = \"Widgets\", current = true)\n"
        + "    " + backtick + "\n"
        + ")";}
!{String deeperHierarchyCode = "@template.ashlar.components.breadcrumbs(\n"
        + "    items = @" + backtick + "\n"
        + "        @template.ashlar.components.breadcrumb-item(label = \"Home\", href = \"/\")\n"
        + "        @template.ashlar.components.breadcrumb-item(label = \"Accounts\", href = \"/accounts\")\n"
        + "        @template.ashlar.components.breadcrumb-item(label = \"Acme Manufacturing\", href = \"/accounts/acme-manufacturing\")\n"
        + "        @template.ashlar.components.breadcrumb-item(label = \"Orders\", href = \"/accounts/acme-manufacturing/orders\")\n"
        + "        @template.ashlar.components.breadcrumb-item(label = \"Order #48213\", current = true)\n"
        + "    " + backtick + "\n"
        + ")";}
!{String longLabelsCode = "@template.ashlar.components.breadcrumbs(\n"
        + "    items = @" + backtick + "\n"
        + "        @template.ashlar.components.breadcrumb-item(label = \"Home\", href = \"/\")\n"
        + "        @template.ashlar.components.breadcrumb-item(label = \"Reports\", href = \"/reports\")\n"
        + "        @template.ashlar.components.breadcrumb-item(label = \"Quarterly Enterprise Revenue Summary by Region and Product Line\", current = true)\n"
        + "    " + backtick + "\n"
        + ")";}
!{List<ApiParam> breadcrumbsApi = List.of(
    ApiParam.required("items", "gg.jte.Content", "One or more breadcrumb-item(...) calls.")
);}
!{List<ApiParam> breadcrumbItemApi = List.of(
    ApiParam.required("label", "String", "The crumb's text."),
    ApiParam.optional("href", "String", "null", "Renders a real <a>. Ignored when current is true."),
    ApiParam.optional("current", "boolean", "false", "Marks this as the page already being viewed: renders non-interactive text with aria-current=\"page\" instead of a link, even if href is also set.")
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
title = "Breadcrumbs",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "breadcrumbs", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Shows where the current page sits within a hierarchy. A real &lt;nav&gt;/&lt;ol&gt; trail of links, ending in the current page's own text - no JavaScript, no click handlers, no router integration.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3">
                    @template.ashlar.components.breadcrumbs(
                    items = @`
                        @template.ashlar.components.breadcrumb-item(label = "Home", href = "/")
                        @template.ashlar.components.breadcrumb-item(label = "Widgets", current = true)
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
                <p class="text-sm text-slate-500 dark:text-slate-400">Place exactly one Breadcrumbs instance above a page's own title, reflecting that page's real position in the site hierarchy - it's page-level navigation, not page content, so it doesn't belong inside a Card alongside the content it precedes. Give every crumb except the last a real <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">href</code>; the last one should be <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">current = true</code> with no link, since it names the page the visitor is already on.</p>
            `
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
                    @template.layout.example(
                    label = "Basic hierarchy",
                    code = basicHierarchyCode,
                    content = @`
                        @template.ashlar.components.breadcrumbs(
                        items = @`
                            @template.ashlar.components.breadcrumb-item(label = "Home", href = "/")
                            @template.ashlar.components.breadcrumb-item(label = "Products", href = "/products")
                            @template.ashlar.components.breadcrumb-item(label = "Widgets", current = true)
                        `
                        )
                    `
                    )

                    @template.layout.example(
                    label = "Deeper hierarchy",
                    code = deeperHierarchyCode,
                    content = @`
                        @template.ashlar.components.breadcrumbs(
                        items = @`
                            @template.ashlar.components.breadcrumb-item(label = "Home", href = "/")
                            @template.ashlar.components.breadcrumb-item(label = "Accounts", href = "/accounts")
                            @template.ashlar.components.breadcrumb-item(label = "Acme Manufacturing", href = "/accounts/acme-manufacturing")
                            @template.ashlar.components.breadcrumb-item(label = "Orders", href = "/accounts/acme-manufacturing/orders")
                            @template.ashlar.components.breadcrumb-item(label = "Order #48213", current = true)
                        `
                        )
                    `
                    )

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Long labels</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Labels wrap in place rather than overflowing the page - resize the window to see it. There is no truncation or collapsing mechanism; a long trail simply wraps.</p>
                        @template.layout.example(
                        code = longLabelsCode,
                        content = @`
                            <div class="max-w-sm">
                                @template.ashlar.components.breadcrumbs(
                                items = @`
                                    @template.ashlar.components.breadcrumb-item(label = "Home", href = "/")
                                    @template.ashlar.components.breadcrumb-item(label = "Reports", href = "/reports")
                                    @template.ashlar.components.breadcrumb-item(label = "Quarterly Enterprise Revenue Summary by Region and Product Line", current = true)
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
                <p class="text-sm text-slate-500 dark:text-slate-400">Breadcrumbs is the wrapper half of a wrapper/item pair, the same split List, Tree, and Pagination each use. <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">breadcrumb-item</code> is not a separate catalog component; it only ever exists composed into Breadcrumbs' own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">items</code> slot, documented here in the table below. Breadcrumbs sits above a page's own title, as its own separate element - not inside a Card, since it's navigation, not content.</p>
                <div class="mt-3 rounded-lg border border-slate-200 bg-slate-50 p-6 dark:border-slate-700 dark:bg-slate-800/50">
                    @template.ashlar.components.breadcrumbs(
                    items = @`
                        @template.ashlar.components.breadcrumb-item(label = "Home", href = "/")
                        @template.ashlar.components.breadcrumb-item(label = "Settings", href = "/settings")
                        @template.ashlar.components.breadcrumb-item(label = "Team Members", current = true)
                    `
                    )
                    <h1 class="mt-3 text-xl font-semibold text-slate-900 dark:text-slate-100">Team Members</h1>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Manage who has access to this workspace.</p>
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
                            <li>A real &lt;nav aria-label="Breadcrumb"&gt; landmark, named exactly as the WAI-ARIA APG specifies, so assistive tech can jump straight to it.</li>
                            <li>A real &lt;ol&gt;, so the trail's own hierarchy is announced as an ordered list with no extra ARIA.</li>
                            <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-current="page"</code> on the current crumb, and nowhere else.</li>
                            <li>A hidden (not merely decorative-by-convention) separator, so it's never announced, and never present on the first crumb.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Exactly one <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">current = true</code> crumb per trail, matching the page actually being viewed.</li>
                            <li>Real <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">href</code> values for every other crumb - a trail with no working links isn't useful navigation.</li>
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
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">breadcrumbs</p>
                        @template.layout.api-reference(apiParams = breadcrumbsApi)
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">breadcrumb-item</p>
                        @template.layout.api-reference(apiParams = breadcrumbItemApi)
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
                    <a href="${contextPath}/components/pagination" class="text-blue-700 hover:underline dark:text-blue-400">Pagination</a>, the same wrapper/item split applied to page numbers rather than a hierarchy &middot;
                    <a href="${contextPath}/components/list" class="text-blue-700 hover:underline dark:text-blue-400">List</a> for a flat set of rows, not a trail
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "breadcrumbs", contextPath = contextPath)
    </div>
`
)
