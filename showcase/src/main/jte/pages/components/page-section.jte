@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param String contextPath

<%-- Batch 12 (final) documentation page. Existing "Title + content" /
     "Title + description + content" / "Multiple sections on one page" /
     "Section containing an existing component" / "Long title" / "Long
     description" / "Narrow / mobile layout" examples kept verbatim.
     Added: Quick Start, formal grammar headings, How to Use, Composition,
     Accessibility, API Reference, Related Components.

     Verified against source before writing:
     - page-section.jte's own 3 @params and doc comment. Confirmed
       `title` always renders as <h2> with no titleLevel escape hatch -
       this utility exists to BE the one-level-down heading beneath a
       page's own <h1> (Page Header), not a general-purpose heading
       wrapper.
     - Confirmed no role="region"/aria-labelledby: a <section>
       immediately preceded by its own <h2> is already an
       accessible-name-bearing landmark by native HTML semantics, the
       same reasoning card.jte gives for omitting role="region" on
       itself.
     - Confirmed `content` is a single required gg.jte.Content, not
       specialized form/table/card-style slots - this utility owns only
       the heading-to-content spacing relationship, never the content's
       own shape. --%>
!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.utilities.page-section(\n"
        + "    title = \"Account Information\",\n"
        + "    content = @" + backtick + "<p>Name, email, and role settings live here.</p>" + backtick + "\n"
        + ")";}
!{List<ApiParam> pageSectionApi = List.of(
    ApiParam.required("title", "String", "Rendered as a real <h2> - the one-level-down heading beneath a page's own Page Header <h1>."),
    ApiParam.optional("description", "String", "\"\"", "Supporting text below the title."),
    ApiParam.required("content", "gg.jte.Content", "The section's body - any content, in whatever shape it already has. This utility never influences how it looks, only its position relative to the heading above it.")
);}

@template.layout.page(
contextPath = contextPath,
title = "Page Section",
content = @`
    @template.layout.component-nav(currentSlug = "page-section", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Page Section</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A layout utility for grouping a page's content under a subsection heading. No surface of its own, no JavaScript - it owns the heading-to-content spacing, not the content's appearance.</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.page-section(
                        title = "Account Information",
                        content = @`<p class="text-sm text-slate-600 dark:text-slate-300">Name, email, and role settings live here.</p>`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">How to Use</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Use one Page Section per logical subsection of a page, beneath its single <a href="${contextPath}/components/page-header" class="text-blue-700 hover:underline dark:text-blue-400">Page Header</a>. It never adds a surface - a Card placed inside still looks exactly like a Card - so pair it with one whenever the subsection actually needs a visible boundary.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Title + content</p>
                            <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                                @template.ashlar.utilities.page-section(
                                title = "Account Information",
                                content = @`<p class="text-sm text-slate-600 dark:text-slate-300">Name, email, and role settings live here.</p>`
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Title + description + content</p>
                            <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                                @template.ashlar.utilities.page-section(
                                title = "Account Information",
                                description = "Manage the account details for this user.",
                                content = @`<p class="text-sm text-slate-600 dark:text-slate-300">Name, email, and role settings live here.</p>`
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Multiple sections on one page</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Each section owns its own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">&lt;h2&gt;</code>, forming a flat list of subsections beneath a page's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">&lt;h1&gt;</code>.</p>
                            <div class="mt-3 flex flex-col gap-8 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                                @template.ashlar.utilities.page-section(
                                title = "Account Information",
                                description = "Manage the account details for this user.",
                                content = @`<p class="text-sm text-slate-600 dark:text-slate-300">Name, email, and role settings live here.</p>`
                                )
                                @template.ashlar.utilities.page-section(
                                title = "Preferences",
                                description = "Configure application preferences.",
                                content = @`<p class="text-sm text-slate-600 dark:text-slate-300">Notification and display settings live here.</p>`
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Long title</p>
                            <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                                @template.ashlar.utilities.page-section(
                                title = "Quarterly Compliance and Access Review for Enterprise Accounts",
                                content = @`<p class="text-sm text-slate-600 dark:text-slate-300">Section content goes here.</p>`
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Long description</p>
                            <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                                @template.ashlar.utilities.page-section(
                                title = "Audit Log",
                                description = "A complete, chronological record of every access change, permission grant, and administrative action taken across all enterprise accounts, retained for the duration required by your organization's compliance policy.",
                                content = @`<p class="text-sm text-slate-600 dark:text-slate-300">Section content goes here.</p>`
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Narrow / mobile layout</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">No fixed widths - the section and its content occupy the available width at any viewport size.</p>
                            <div class="mt-3 max-w-xs rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                                @template.ashlar.utilities.page-section(
                                title = "Account Information",
                                description = "Manage the account details for this user.",
                                content = @`<p class="text-sm text-slate-600 dark:text-slate-300">Name, email, and role settings live here.</p>`
                                )
                            </div>
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composition</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Page Section belongs to the same layout family as <a href="${contextPath}/components/page-shell" class="text-blue-700 hover:underline dark:text-blue-400">Page Shell</a> and <a href="${contextPath}/components/page-header" class="text-blue-700 hover:underline dark:text-blue-400">Page Header</a> - see Page Shell's own page for the full composition shown together. Its content never gains a surface from this utility, as shown below with a composed Card.</p>
                    <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.page-section(
                        title = "Recent Activity",
                        description = "The latest activity for this account.",
                        content = @`
                            @template.ashlar.components.card(
                                body = @`<p class="text-sm text-slate-600 dark:text-slate-300">Dana Whitfield updated billing contact - 2 hours ago.</p>`
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
                                <li>A real &lt;section&gt; immediately preceded by its own &lt;h2&gt; - already an accessible-name-bearing landmark by native HTML semantics, with no role or aria-labelledby needed.</li>
                            </ul>
                        </div>
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>A single Page Header's &lt;h1&gt; above these &lt;h2&gt;s, so the heading hierarchy stays correct.</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div>
                    @template.layout.api-reference(apiParams = pageSectionApi)
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/page-header" class="text-blue-700 hover:underline dark:text-blue-400">Page Header</a>, the &lt;h1&gt; above it &middot;
                        <a href="${contextPath}/components/page-shell" class="text-blue-700 hover:underline dark:text-blue-400">Page Shell</a>, the outer wrapper &middot;
                        <a href="${contextPath}/components/card" class="text-blue-700 hover:underline dark:text-blue-400">Card</a> for a subsection that needs a visible surface
                    </p>
                </div>
            </div>
        </section>
`
)
