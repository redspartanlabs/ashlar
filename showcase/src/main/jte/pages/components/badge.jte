@import com.redspartanlabs.ashlar.icon.Icon
@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param String contextPath

<%-- Batch 5 documentation page. Existing "Semantic variants" / "With an
     optional leading icon" / "Inline with text" examples kept verbatim,
     moved under Variants & States. Added: Quick Start, formal grammar
     headings, Composition, Accessibility, API Reference, Related
     Components.

     REAL FIX, not a rewrite for its own sake: the "Inside a table"
     example previously hand-rolled a raw <table> rather than using the
     real Table component - flagged as a known defect all the way back in
     the original documentation audit (before Batch 0). Replaced with an
     actual @template.ashlar.components.table(...) call using
     table-column/table-row/table-cell, which is both a more honest
     demonstration and a real, verified composition relationship
     (table-cell's own content slot is exactly how a real page would put
     a Badge in a status column). Table's own component behavior is
     unchanged - this only fixes what this one documentation page
     demonstrates.

     Verified against source before writing:
     - badge.jte's own 3 @params and its own doc comment in full.
       Confirmed this is a plain <span>, no role, no JavaScript, and that
       role="status" is deliberately NOT applied - that would claim every
       badge is a live-region announcement, which is true of at most some
       call sites, not a property of the badge itself.
     - BadgeVariant.java: 5 variants (Neutral/Info/Success/Warning/
       Danger), falling back silently to Neutral on an unrecognized
       string. Confirmed the severe variant is spelled "Danger" here,
       matching Button/Toast - the opposite of Alert, which spells its
       own equivalent "Error". Both pages now cross-reference this.
     - Confirmed, by the component's own doc comment, that meaning always
       lives in the visible text, never in color or icon alone - the icon
       is always decorative (aria-hidden via icon.jte's own default), so
       the caller must write a label that stands on its own even in
       grayscale.
     - Confirmed via grep that stat-card.jte's own `change` slot is
       designed specifically to be filled with a Badge - already
       demonstrated on the Stat Card page itself. --%>
!{String quickStartCode = """
    @template.ashlar.components.badge(
        text = "Active",
        variant = "Success"
    )""";}
!{List<ApiParam> badgeApi = List.of(
    ApiParam.required("text", "String", "The only carrier of meaning - color and the optional icon are always reinforcing, never the sole signal."),
    ApiParam.optional("variant", "String", "\"Neutral\"", "Neutral/Info/Success/Warning/Danger. Unrecognized values silently fall back to Neutral."),
    ApiParam.optional("icon", "Icon", "null", "Always decorative (aria-hidden) - the text already says what the icon would.")
);}

@template.layout.page(
contextPath = contextPath,
title = "Badge",
content = @`
    @template.layout.component-nav(currentSlug = "badge", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Badge</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A static label for status, category, or metadata - plain HTML and CSS, no JavaScript, not a button or a control.</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3">
                        @template.ashlar.components.badge(text = "Active", variant = "Success")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Semantic variants</p>
                            <div class="mt-3 flex flex-wrap items-center gap-3">
                                @template.ashlar.components.badge(text = "Draft", variant = "Neutral")
                                @template.ashlar.components.badge(text = "In Review", variant = "Info")
                                @template.ashlar.components.badge(text = "Active", variant = "Success")
                                @template.ashlar.components.badge(text = "High Priority", variant = "Warning")
                                @template.ashlar.components.badge(text = "Failed", variant = "Danger")
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">With an optional leading icon</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Reinforcing, never the sole carrier of meaning - the text alone already says it.</p>
                            <div class="mt-3 flex flex-wrap items-center gap-3">
                                @template.ashlar.components.badge(text = "Completed", variant = "Success", icon = Icon.CHECK_CIRCLE)
                                @template.ashlar.components.badge(text = "Needs Attention", variant = "Warning", icon = Icon.WARNING_TRIANGLE)
                                @template.ashlar.components.badge(text = "Rejected", variant = "Danger", icon = Icon.X_CIRCLE)
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Inline with text</p>
                            <p class="mt-2 text-sm text-slate-700 dark:text-slate-300">
                                Job Header JOB-1042 is currently
                                @template.ashlar.components.badge(text = "Production", variant = "Success")
                                and flagged
                                @template.ashlar.components.badge(text = "High Priority", variant = "Warning")
                                for review.
                            </p>
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composition</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Badge is a common passenger inside other components' own content slots rather than something they build in for you: <a href="${contextPath}/components/stat-card" class="text-blue-700 hover:underline dark:text-blue-400">Stat Card</a>'s <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">change</code> slot is designed specifically to hold one, and a Table or Data Table status column reads faster as a badge than as plain text.</p>
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
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
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
                </div>

                <div>
                    @template.layout.api-reference(apiParams = badgeApi)
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/stat-card" class="text-blue-700 hover:underline dark:text-blue-400">Stat Card</a>, whose <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">change</code> slot is designed to hold one &middot;
                        <a href="${contextPath}/components/alert" class="text-blue-700 hover:underline dark:text-blue-400">Alert</a> for a larger, persistent severity message instead of inline metadata &middot;
                        <a href="${contextPath}/components/toast" class="text-blue-700 hover:underline dark:text-blue-400">Toast</a> for a transient one
                    </p>
                </div>
            </div>
        </section>
`
)
