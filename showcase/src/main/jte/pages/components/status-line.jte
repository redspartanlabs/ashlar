@import dev.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import dev.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

<%-- Batch 12 (final) documentation page. Existing "Basic" / "Longer
     message" / "Rich content" / "Alongside an existing component" / "In a
     Page Section, above a Data Table's Form Actions" / "Narrow / mobile
     layout" examples kept verbatim. Added: Quick Start, formal grammar
     headings, How to Use, Composition, Accessibility, Pitfalls, API
     Reference, Related Components.

     Verified against source before writing:
     - status-line.jte's own single @param (content, gg.jte.Content not
       String) and extensive doc comment. Confirmed this owns only
       typography and spacing for one line of informational prose -
       "Last updated...", "Showing 24 of 128 records" - and is explicitly
       NOT a replacement for Alert (persistent contextual messaging) or
       Badge (small status indicator), both of which already own those
       jobs.
     - Confirmed a plain <p>, not a <div> wrapper - every real use case
       is genuinely one line of phrasing content (plain text, a link, an
       inline Badge).
     - CONFIRMED NO role="status"/aria-live, deliberately, matching
       alert.jte's own reasoning precisely: this is server-rendered,
       already part of the page on first paint, with nothing updating it
       afterward - a live-region role on already-static content doesn't
       make it more accessible, and risks an unwanted announcement on
       page load. The component's own doc comment explicitly notes that
       a caller who dynamically swaps this in via their own JavaScript
       after load is the one introducing live-update behavior, and
       should add that role themselves at that point - this utility has
       no way to know in advance whether that applies, so documented as
       a real Pitfall below rather than left implicit. --%>
!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.utilities.status-line(\n"
        + "    content = @" + backtick + "Last updated September 21, 2026" + backtick + "\n"
        + ")";}
!{List<ApiParam> statusLineApi = List.of(
    ApiParam.required("content", "gg.jte.Content", "The message - plain text, a link, or a small composed component like a Badge. Rendered inside a real <p>.")
);}
!{List<PageAnchor> sections = List.of(
    new PageAnchor("quick-start", "Quick Start"),
    new PageAnchor("how-to-use", "How to Use"),
    new PageAnchor("variants", "Variants & States"),
    new PageAnchor("composition", "Composition"),
    new PageAnchor("pitfalls", "Pitfalls & Troubleshooting"),
    new PageAnchor("accessibility", "Accessibility"),
    new PageAnchor("api-reference", "API Reference"),
    new PageAnchor("related", "Related Components")
);}

@template.layout.page(
contextPath = contextPath,
title = "Status Line",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "status-line", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A single inline status/informational message - muted typography only, no surface, no JavaScript. Not a replacement for Alert or Badge.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                    @template.ashlar.utilities.status-line(
                    content = @`Last updated September 21, 2026`
                    )
                </div>
            `
            )
        </section>

        <section id="how-to-use" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "How to Use",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Reach for Status Line for one quiet, informational sentence near the content it describes - a record count, a last-updated time, a recoverable "no results" message. For a persistent message with real semantic weight, use <a href="${contextPath}/components/alert" class="text-blue-700 hover:underline dark:text-blue-400">Alert</a> instead; for a small inline status indicator, use <a href="${contextPath}/components/badge" class="text-blue-700 hover:underline dark:text-blue-400">Badge</a>. The two are not competing for the same job and are often used right beside each other, shown below.</p>
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
                        <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                            @template.ashlar.utilities.status-line(
                            content = @`Last updated September 21, 2026`
                            )
                        </div>
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Longer message</p>
                        <div class="mt-3 max-w-md rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                            @template.ashlar.utilities.status-line(
                            content = @`No results matched your filters. Try widening the date range or removing one of the applied account filters.`
                            )
                        </div>
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Rich content</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A message can contain a link or another existing component - Status Line never needs to understand it.</p>
                        <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                            @template.ashlar.utilities.status-line(
                            content = @`Changes saved. <a href="/components/status-line" class="text-blue-700 hover:underline dark:text-blue-400">View audit log</a>.`
                            )
                        </div>
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Narrow / mobile layout</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A long message wraps like any other paragraph text - no truncation, no fixed width.</p>
                        <div class="mt-3 max-w-xs rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                            @template.ashlar.utilities.status-line(
                            content = @`No results matched your filters. Try widening the date range or removing one of the applied account filters.`
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
                <p class="text-sm text-slate-500 dark:text-slate-400">Composes naturally alongside <a href="${contextPath}/components/badge" class="text-blue-700 hover:underline dark:text-blue-400">Badge</a> - Status Line stays plain text-level while the Badge carries its own status semantics, so the two never compete for the same job.</p>
                <div class="mt-3 flex flex-wrap items-center gap-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                    @template.ashlar.components.badge(text = "Operational", variant = "Success")
                    @template.ashlar.utilities.status-line(
                    content = @`All systems normal as of 9:41 AM.`
                    )
                </div>
                <p class="mt-3 text-sm text-slate-500 dark:text-slate-400">A realistic composition: a record count inside a <a href="${contextPath}/components/page-section" class="text-blue-700 hover:underline dark:text-blue-400">Page Section</a>, above a <a href="${contextPath}/components/form-actions" class="text-blue-700 hover:underline dark:text-blue-400">Form Actions</a> row - exactly the kind of small informational line this utility exists for.</p>
                <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                    @template.ashlar.utilities.page-section(
                    title = "Accounts",
                    description = "Enterprise accounts currently on file.",
                    content = @`
                        @template.ashlar.components.card(
                        body = @`
                            <div class="flex flex-col items-start gap-3">
                                @template.ashlar.utilities.status-line(
                                content = @`Showing 24 of 128 records.`
                                )
                                @template.ashlar.components.badge(text = "3 items selected", variant = "Info")
                            </div>
                        `,
                        footer = @`
                            @template.ashlar.utilities.form-actions(
                                content = @`
                                    @template.ashlar.components.button(text = "Export selected", type = "Secondary")
                                    @template.ashlar.components.button(text = "Archive selected", type = "Primary")
                                `
                            )
                        `
                        )
                    `
                    )
                </div>
            `
            )
        </section>

        <section id="pitfalls" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Pitfalls & Troubleshooting",
            content = @`
                <ul class="list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">There is no live-region role, on purpose.</strong> This is server-rendered, static content on first paint - adding <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="status"</code> to that would risk an unwanted announcement the moment the page loads. If your own JavaScript swaps this content in after the page has already loaded, that dynamic update is what would need a live region - add it yourself at that point, since this utility has no way to know in advance whether that applies.</li>
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
                            <li>A real &lt;p&gt;, so it reads as ordinary phrasing content with no extra ARIA needed for static text.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>A live-region role yourself if you ever update this content dynamically after page load - see Pitfalls.</li>
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
                @template.layout.api-reference(apiParams = statusLineApi)
            `
            )
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/alert" class="text-blue-700 hover:underline dark:text-blue-400">Alert</a> for a persistent message with real semantic weight &middot;
                    <a href="${contextPath}/components/badge" class="text-blue-700 hover:underline dark:text-blue-400">Badge</a> for a small inline status indicator &middot;
                    <a href="${contextPath}/components/filter-bar" class="text-blue-700 hover:underline dark:text-blue-400">Filter Bar</a>, a common neighbor above a result set
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "status-line", contextPath = contextPath)
    </div>
`
)
