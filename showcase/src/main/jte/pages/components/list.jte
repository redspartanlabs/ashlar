@import com.redspartanlabs.ashlar.icon.Icon
@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param String contextPath

<%-- Batch 6 documentation page. Existing "Basic" / "Navigation-style, with
     icons" / "Trailing content" / "Ordered" / "Long content wraps" /
     "In context" examples kept verbatim, moved under Variants & States/
     Composition. Added: Quick Start, formal grammar headings, How to Use,
     Composition (list-item documented here, not as a standalone page),
     Accessibility, two API Reference tables, Related Components.

     Verified against source before writing:
     - list.jte's own 2 @params and its own doc comment in full. Confirmed
       this is purely static composition - no JS file, no data-* hook -
       nothing here opens, closes, filters, sorts, or paginates; that
       behavior belongs to Data Table, not a plain list.
     - Confirmed `ordered` is a real semantic choice between <ul> and
       <ol>, not a styling variant: does position 2 mean "second" to a
       screen reader, or just "next"? Confirmed no role/aria-label on the
       wrapper by design - <ul>/<ol> already say "a list" natively, and
       unlike Breadcrumbs'/Pagination's own APG-assigned landmark names, a
       generic List has no name to borrow.
     - list-item.jte's own 5 @params and its own doc comment. Confirmed a
       row with `href` becomes one whole-row <a> (not a link buried in
       otherwise-static text, so the click target and keyboard focus
       target are the same element) and gets a trailing chevron -
       confirmed the chevron is driven purely by whether href is set (a
       fact already known at render time), explicitly not the same
       guess-from-label-text mistake pagination-item.jte's own doc
       comment warns against making elsewhere.
     - Confirmed `trailing` is gg.jte.Content, not a String, specifically
       because what belongs at the end of a row varies (a Badge, a
       timestamp, a value) and none of those is well served by a single
       string param - already demonstrated in the existing "Trailing
       content" example with both plain text and a composed Badge. --%>
!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.components.list(\n"
        + "    items = @" + backtick + "\n"
        + "        @template.ashlar.components.list-item(label = \"Overview\")\n"
        + "        @template.ashlar.components.list-item(label = \"Settings\")\n"
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

@template.layout.page(
contextPath = contextPath,
title = "List",
content = @`
    @template.layout.component-nav(currentSlug = "list", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">List</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A semantic collection of related rows - a bordered, divided &lt;ul&gt; or &lt;ol&gt; surface, not a data table. No JavaScript: nothing here sorts, filters, or paginates. Each row is a separate item call the caller composes.</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3 max-w-md">
                        @template.ashlar.components.list(
                        items = @`
                            @template.ashlar.components.list-item(label = "Overview")
                            @template.ashlar.components.list-item(label = "Settings")
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">How to Use</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Reach for List for a static set of related rows - settings links, a key/value summary, a short feed - where nothing needs sorting, filtering, searching, or pagination. The moment any of those become real requirements, that's <a href="${contextPath}/components/data-table" class="text-blue-700 hover:underline dark:text-blue-400">Data Table</a>'s job, not something to bolt onto this one. Set <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">ordered = true</code> only when the position of a row is itself meaningful information (a numbered procedure), not just to get numeral-looking bullets.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
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
                            <div class="mt-3 max-w-md">
                                @template.ashlar.components.list(
                                items = @`
                                    @template.ashlar.components.list-item(label = "Account", description = "Profile, email, and password", href = "#", icon = Icon.USER)
                                    @template.ashlar.components.list-item(label = "Security", description = "Two-factor authentication and sessions", href = "#", icon = Icon.SHIELD)
                                    @template.ashlar.components.list-item(label = "Notifications", description = "Email and in-app alerts", href = "#", icon = Icon.BELL)
                                `
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Trailing content</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A Badge, a value, or any other small composed element can sit at the end of a row - useful for metadata-style pairs.</p>
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
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Ordered</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400"><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">ordered = true</code> renders an &lt;ol&gt; - position itself is part of what the list communicates.</p>
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
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Long content wraps</p>
                            <div class="mt-3 max-w-md">
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
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composition</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">List is the wrapper half of a wrapper/item pair - the same split Breadcrumbs, Pagination, and Stepper each use for their own entries. <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">list-item</code> is not a separate catalog component; it only ever exists composed into List's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">items</code> slot, so its full API is documented here, in the table below. A List commonly sits inside a Card's body, shown here the way it would appear in a real page.</p>
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
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
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
                </div>

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

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/data-table" class="text-blue-700 hover:underline dark:text-blue-400">Data Table</a> for sorting, searching, or paginating rows &middot;
                        <a href="${contextPath}/components/breadcrumbs" class="text-blue-700 hover:underline dark:text-blue-400">Breadcrumbs</a>,
                        <a href="${contextPath}/components/pagination" class="text-blue-700 hover:underline dark:text-blue-400">Pagination</a>, and
                        <a href="${contextPath}/components/stepper" class="text-blue-700 hover:underline dark:text-blue-400">Stepper</a>, the same wrapper/item split applied to other kinds of entries &middot;
                        <a href="${contextPath}/components/card" class="text-blue-700 hover:underline dark:text-blue-400">Card</a>, a common host
                    </p>
                </div>
            </div>
        </section>
`
)
