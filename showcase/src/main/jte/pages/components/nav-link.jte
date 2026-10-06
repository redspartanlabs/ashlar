@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param String contextPath

<%-- Batch 2B documentation page (catalog-completion orphan). NavLink had
     zero catalog presence despite being this showcase's own site
     navigation (layout/page.jte calls it directly for "Components" and
     "GitHub") as well as every Navbar example's link list.

     Verified against source: nav-link.jte's own 3 params and its doc
     comment in full - "current wins over href" (a current link renders as
     a non-interactive <span aria-current="page">, never an <a>, even if
     href is also set), the fixed <li> wrapper (it is only ever valid
     inside a <ul>/<ol>, confirmed by every real call site wrapping it in
     Navbar's own <ul>), and the fact that Navbar renders its `items` slot
     TWICE - once for the desktop bar, once for the mobile drawer (verified
     directly in navbar.jte, lines rendering ${items} in two separate <ul>
     elements) - so one nav-link call becomes two <li> elements in the DOM,
     not a bug to work around. --%>
!{String quickStartCode = """
    @template.ashlar.components.nav-link(
        label = "Dashboard",
        href = "/dashboard"
    )""";}
!{List<ApiParam> navLinkApi = List.of(
    ApiParam.required("label", "String", "The visible link text."),
    ApiParam.optional("href", "String", "null", "Where it navigates. Ignored when current is true - see Pitfalls."),
    ApiParam.optional("current", "boolean", "false", "Marks this as the page the user is already on: renders a non-interactive span with aria-current=\"page\" instead of a link, regardless of href.")
);}

@template.layout.page(
contextPath = contextPath,
title = "NavLink",
content = @`
    @template.layout.component-nav(currentSlug = "nav-link", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">NavLink</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">One entry in a navigation link list - a real &lt;a href&gt; for anywhere the visitor can go, or non-interactive current-page text when they're already there. It renders its own &lt;li&gt;, so it's only valid inside a &lt;ul&gt;/&lt;ol&gt; - in practice, inside <a href="${contextPath}/components/navbar" class="text-blue-700 hover:underline dark:text-blue-400">Navbar</a>'s own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">items</code> slot.</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">Shown here inside a &lt;ul&gt; - NavLink always renders a bare &lt;li&gt;, so it needs a real list parent to be valid markup.</p>
                    <ul class="mt-3 flex max-w-xs flex-col gap-1 rounded-md border border-slate-200 p-2 dark:border-slate-700">
                        @template.ashlar.components.nav-link(label = "Dashboard", href = "#")
                    </ul>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">How to Use</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">NavLink exists for exactly one job: the repeated items inside a navigation list, where exactly one entry is usually "where the user already is" and the rest are links elsewhere. That's also precisely how this showcase's own header builds its "Components" / "GitHub" links in <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">layout/page.jte</code>. For a breadcrumb trail instead of a nav bar, use <a href="${contextPath}/components/breadcrumbs" class="text-blue-700 hover:underline dark:text-blue-400">Breadcrumbs</a>' own breadcrumb-item, which follows the identical current-wins-over-href pattern.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Link vs. current page</p>
                            <ul class="mt-3 flex max-w-xs flex-col gap-1 rounded-md border border-slate-200 p-2 dark:border-slate-700">
                                @template.ashlar.components.nav-link(label = "Overview", href = "#")
                                @template.ashlar.components.nav-link(label = "Reports", href = "#", current = true)
                                @template.ashlar.components.nav-link(label = "Settings", href = "#")
                            </ul>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">In context: Navbar's own link list</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The actual, intended usage - see <a href="${contextPath}/components/navbar" class="text-blue-700 hover:underline dark:text-blue-400">Navbar</a> for the full component this feeds into, including why its <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">items</code> slot renders this same markup twice (desktop bar and mobile drawer).</p>
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composition</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">NavLink is built to be passed, one call per entry, into Navbar's <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">items</code> content block. Navbar renders that block twice - once inside its desktop &lt;ul&gt;, once inside its mobile drawer's &lt;ul&gt; - so each NavLink call produces two &lt;li&gt; elements in the final page, not one. This is Navbar's own documented behavior, not something to compensate for here.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Pitfalls &amp; Troubleshooting</h3>
                    <ul class="mt-2 list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300"><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">current = true</code> silently ignores <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">href</code>.</strong> Setting both isn't an error, but only the non-interactive current-page text renders - the href is simply never used. This is intentional (a page can't usefully link to itself while also telling assistive tech it's the current location), but easy to miss if you expect the link to stay clickable.</li>
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">It always renders a bare &lt;li&gt;.</strong> Used outside a &lt;ul&gt;/&lt;ol&gt;, that's invalid markup - NavLink has no standalone mode.</li>
                    </ul>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Ashlar provides</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-current="page"</code> on the current entry, and nowhere else.</li>
                                <li>A real &lt;a href&gt; for every other entry - native focus and keyboard activation, no JavaScript involved.</li>
                            </ul>
                        </div>
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>A &lt;ul&gt; or &lt;ol&gt; parent - NavLink's own &lt;li&gt; assumes one.</li>
                                <li>Exactly one <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">current = true</code> per list, matching the page actually being viewed.</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div>
                    @template.layout.api-reference(apiParams = navLinkApi)
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/navbar" class="text-blue-700 hover:underline dark:text-blue-400">Navbar</a>, NavLink's one real host &middot;
                        <a href="${contextPath}/components/breadcrumbs" class="text-blue-700 hover:underline dark:text-blue-400">Breadcrumbs</a> for the same current-wins-over-href pattern in a trail instead of a nav list &middot;
                        <a href="${contextPath}/components/button" class="text-blue-700 hover:underline dark:text-blue-400">Button</a> for an action rather than navigation
                    </p>
                </div>
            </div>
        </section>
`
)
