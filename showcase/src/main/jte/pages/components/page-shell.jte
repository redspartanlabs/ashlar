@import dev.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import dev.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

<%-- Batch 12 (final) documentation page. Existing "A full page
     composition" / "Narrow / mobile layout" examples kept verbatim.
     "Wide content stays constrained" is kept but its demonstration table
     is now a real @template.ashlar.components.table(...) call instead of a
     hand-rolled <table> - the same class of fix already applied to Badge
     and Pagination's own examples in earlier batches, for the same
     reason: Table already has its own overflow-x-auto scroller, so using
     the real component is both more honest and a better demonstration of
     "wide content stays contained" than a hand-rolled copy of the same
     idea. Added: Quick Start, formal grammar headings, How to Use,
     Composition, Accessibility, API Reference, Related Components.

     Verified against source before writing:
     - page-shell.jte's own single @param (content) and doc comment.
       Confirmed this is a plain <div>, not <main> - the surrounding
       application layout already owns the document's one <main>
       landmark, so this utility only needs to sit inside it, not
       duplicate it. Confirmed the width/padding/gap values
       (max-w-6xl, px-4/sm:px-6/lg:px-8, flex flex-col gap-10) match
       layout/page.jte's own already-established <main> convention
       exactly, rather than inventing a second one.
     - Confirmed `gap-10` only spaces top-level blocks apart from each
       other - it never reaches into one block and restyles it. A Page
       Header composed as one child still lays out its own
       title/description/actions exactly as page-header.jte defines,
       untouched by this container's own flex context. --%>
!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.utilities.page-shell(\n"
        + "    content = @" + backtick + "\n"
        + "        @template.ashlar.utilities.page-header(title = \"Dashboard\")\n"
        + "    " + backtick + "\n"
        + ")";}
!{List<ApiParam> pageShellApi = List.of(
    ApiParam.required("content", "gg.jte.Content", "One or more top-level blocks - typically a Page Header followed by one or more Page Sections - spaced apart from each other, never restyled.")
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
title = "Page Shell",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "page-shell", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The outer layout boundary for a page's own content - readable width, horizontal padding, and vertical spacing between top-level blocks. No surface, no JavaScript, no opinion about what's inside.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3 rounded-lg border border-dashed border-slate-300 bg-slate-50 dark:border-slate-600 dark:bg-slate-950/40">
                    @template.ashlar.utilities.page-shell(
                    content = @`
                        @template.ashlar.utilities.page-header(title = "Dashboard")
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
                <p class="text-sm text-slate-500 dark:text-slate-400">Reach for Page Shell as the single outermost wrapper around an application page's own content, inside your layout's real &lt;main&gt;. It never duplicates that landmark itself - it only establishes the readable-width column and the vertical rhythm between the blocks you place inside it.</p>
            `
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">A full page composition</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Page Shell wrapping a Page Header and several Page Sections, the way a real application page would compose them. The dashed border below marks the shell's own boundary for this showcase only - it isn't part of Page Shell itself.</p>
                        <div class="mt-3 rounded-lg border border-dashed border-slate-300 bg-slate-50 dark:border-slate-600 dark:bg-slate-950/40">
                            @template.ashlar.utilities.page-shell(
                            content = @`
                                @template.ashlar.utilities.page-header(
                                    title = "Dashboard",
                                    description = "Overview of system activity.",
                                    actions = @`@template.ashlar.components.button(text = "New report", type = "Primary")`
                                )
                                @template.ashlar.utilities.page-section(
                                    title = "Account Information",
                                    description = "Manage the account details for this user.",
                                    content = @`
                                        @template.ashlar.components.card(
                                            body = @`<p class="text-sm text-slate-600 dark:text-slate-300">Name, email, and role settings live here.</p>`
                                        )
                                    `
                                )
                                @template.ashlar.utilities.page-section(
                                    title = "Recent Activity",
                                    content = @`
                                        @template.ashlar.components.card(
                                            body = @`<p class="text-sm text-slate-600 dark:text-slate-300">Dana Whitfield updated billing contact - 2 hours ago.</p>`
                                        )
                                    `
                                )
                            `
                            )
                        </div>
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Wide content stays constrained</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Page Shell caps content at a comfortable reading width even when the viewport is much wider - the dashed line marks the shell's own edge. The table below is wider than the shell itself and scrolls inside its own surface (Table's own built-in behavior), rather than pushing the shell or the page wider.</p>
                        <div class="rounded-lg border border-dashed border-slate-300 bg-slate-50 dark:border-slate-600 dark:bg-slate-950/40">
                            @template.ashlar.utilities.page-shell(
                            content = @`
                                @template.ashlar.utilities.page-section(
                                    title = "Wide table",
                                    content = @`
                                        @template.ashlar.components.table(
                                        caption = "Enterprise accounts",
                                        columns = @`
                                            @template.ashlar.components.table-column(label = "Account")
                                            @template.ashlar.components.table-column(label = "Owner")
                                            @template.ashlar.components.table-column(label = "Plan")
                                            @template.ashlar.components.table-column(label = "Status")
                                            @template.ashlar.components.table-column(label = "Renewal")
                                        `,
                                        rows = @`
                                            @template.ashlar.components.table-row(cells = @`
                                                @template.ashlar.components.table-cell(text = "Acme Logistics", header = true, nowrap = true)
                                                @template.ashlar.components.table-cell(text = "Dana Whitfield", nowrap = true)
                                                @template.ashlar.components.table-cell(text = "Enterprise", nowrap = true)
                                                @template.ashlar.components.table-cell(content = @`@template.ashlar.components.badge(text = "Active", variant = "Success")`)
                                                @template.ashlar.components.table-cell(text = "March 2027", nowrap = true)
                                            `)
                                        `
                                        )
                                    `
                                )
                            `
                            )
                        </div>
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Narrow / mobile layout</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Horizontal padding shrinks at narrow widths but never disappears, and nothing overflows.</p>
                        <div class="max-w-xs rounded-lg border border-dashed border-slate-300 bg-slate-50 dark:border-slate-600 dark:bg-slate-950/40">
                            @template.ashlar.utilities.page-shell(
                            content = @`
                                @template.ashlar.utilities.page-header(
                                    title = "Dashboard",
                                    description = "Overview of system activity.",
                                    actions = @`@template.ashlar.components.button(text = "New report", type = "Primary")`
                                )
                                @template.ashlar.utilities.page-section(
                                    title = "Account Information",
                                    content = @`
                                        @template.ashlar.components.card(
                                            body = @`<p class="text-sm text-slate-600 dark:text-slate-300">Name, email, and role settings live here.</p>`
                                        )
                                    `
                                )
                            `
                            )
                        </div>
                    </div>
                </div>
            `
            )
        </section>

        <section id="composition" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Composition",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Page Shell is the outermost of a small layout family it does not itself call: <a href="${contextPath}/components/page-header" class="text-blue-700 hover:underline dark:text-blue-400">Page Header</a> for the page's one &lt;h1&gt;, then one or more <a href="${contextPath}/components/page-section" class="text-blue-700 hover:underline dark:text-blue-400">Page Section</a> blocks for its &lt;h2&gt; subsections - none of the three composes another internally; a page author assembles all three together, as shown above. <a href="${contextPath}/components/footer" class="text-blue-700 hover:underline dark:text-blue-400">Footer</a> matches the same width/padding rhythm but is its own separate landmark, typically placed outside the shell rather than as one of its children.</p>
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
                            <li>Nothing landmark-related by design - a plain &lt;div&gt;, since the surrounding layout already owns the page's one &lt;main&gt;.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>The &lt;main&gt; (or other) landmark this sits inside - this utility is never a substitute for one.</li>
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
                @template.layout.api-reference(apiParams = pageShellApi)
            `
            )
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/page-header" class="text-blue-700 hover:underline dark:text-blue-400">Page Header</a> and
                    <a href="${contextPath}/components/page-section" class="text-blue-700 hover:underline dark:text-blue-400">Page Section</a>, the usual contents &middot;
                    <a href="${contextPath}/components/footer" class="text-blue-700 hover:underline dark:text-blue-400">Footer</a>, matching the same width rhythm
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "page-shell", contextPath = contextPath)
    </div>
`
)
