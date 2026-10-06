@import com.redspartanlabs.ashlar.icon.Icon
@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param String contextPath

<%-- Batch 7 documentation page. Existing "Basic" / "Files and folders" /
     "Organizational hierarchy" / "Deep nesting" / "Long labels" /
     "Multiple instances stay independent" examples kept verbatim, moved
     under Variants & States/Composition. Added: Quick Start, formal
     grammar headings, How to Use, Composition (tree-item documented here,
     not as a standalone page), Accessibility, two API Reference tables,
     Related Components, Pitfalls.

     Verified against source before writing - this is a real WAI-ARIA APG
     tree, not merely tree-shaped, confirmed by reading tree.js in full
     (182 lines), not inferred from the visual nesting alone:
     - tree.jte's own single @param and doc comment. Confirmed role="tree"
       on a real <ul>, zero aria-label (no APG-assigned name for a generic
       tree to borrow, same reasoning list.jte's wrapper gives), and that
       TreeContext.reset() runs once per Tree instance so several on one
       page never share a depth counter.
     - tree-item.jte's own 5 @params and its extensive doc comment.
       Confirmed the leaf/parent distinction is purely "children present or
       absent" - no separate flag. Confirmed role="treeitem"/aria-expanded/
       aria-level all live on the <li> itself, not the toggle button -
       the button carries tabindex="-1" and aria-hidden="true", reachable
       by click/tap but invisible to Tab and to linear screen-reader
       reading, since the treeitem IS the control per the APG pattern.
       Confirmed no aria-posinset/aria-setsize - deliberately omitted
       because the full hierarchy renders server-side with no
       virtualization, and assistive tech computes both by counting once
       every sibling is already in the DOM.
     - CONFIRMED NO disabled-item SUPPORT AT ALL: tree-item.jte has no
       disabled @param, and tree.js has no disabled handling anywhere -
       this isn't an oversight to infer, it's the complete absence of the
       feature in both files.
     - CONFIRMED arbitrary nesting depth: TreeContext is a plain
       stack-depth counter with enterChildren()/exitChildren(), no depth
       limit anywhere - the existing "Deep nesting" example demonstrates
       5 levels, and nothing in the source caps it there.
     - tree.js in full. Confirmed real roving-tabindex keyboard
       navigation per the WAI-ARIA APG tree pattern: Arrow Up/Down move
       focus between VISIBLE items only (never into a collapsed branch -
       `inert` keeps it out of both Tab order and this component's own
       keyboard walk); Arrow Right expands a collapsed parent or moves
       focus into its first child if already expanded; Arrow Left
       collapses an expanded parent or moves focus to the parent item;
       Home/End jump to the first/last visible item; Enter/Space toggles
       a parent or activates a leaf's link. Confirmed clicking the
       toggle button is the only click-based way to expand/collapse -
       clicking elsewhere on a row only moves focus (via a focusin
       listener), it never toggles. --%>
!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.components.tree(\n"
        + "    items = @" + backtick + "\n"
        + "        @template.ashlar.components.tree-item(label = \"Fruit\")\n"
        + "        @template.ashlar.components.tree-item(label = \"Vegetables\")\n"
        + "    " + backtick + "\n"
        + ")";}
!{List<ApiParam> treeApi = List.of(
    ApiParam.required("items", "gg.jte.Content", "One or more tree-item(...) calls.")
);}
!{List<ApiParam> treeItemApi = List.of(
    ApiParam.required("label", "String", "The row's text."),
    ApiParam.optional("href", "String", "null", "Renders the label as a real <a>. Works independently of expanded state - a row can be both a link and a parent."),
    ApiParam.optional("icon", "Icon", "null", "Always decorative, before the label."),
    ApiParam.optional("expanded", "boolean", "false", "Initial expand state. Only meaningful when children is set."),
    ApiParam.optional("children", "gg.jte.Content", "null", "One or more nested tree-item(...) calls. Presence or absence of this is the entire leaf/parent distinction - there is no separate flag.")
);}

@template.layout.page(
contextPath = contextPath,
title = "Tree View",
content = @`
    @template.layout.component-nav(currentSlug = "tree", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Tree View</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A semantic, keyboard-navigable hierarchy - not a data table, not a menu. The caller composes nested items; the component owns expand/collapse and tree keyboard behavior only. Click a chevron, or focus a row and try Arrow Up/Down/Left/Right, Home, End, and Enter.</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3 max-w-md">
                        @template.ashlar.components.tree(
                        items = @`
                            @template.ashlar.components.tree-item(label = "Fruit")
                            @template.ashlar.components.tree-item(label = "Vegetables")
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">How to Use</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Reach for Tree View for a genuine hierarchy a visitor expands and collapses themselves - a file browser, an org chart, nested categories. For a flat set of rows with no parent/child relationship, use <a href="${contextPath}/components/list" class="text-blue-700 hover:underline dark:text-blue-400">List</a> instead; its items are simpler and it carries none of the keyboard machinery a tree needs. The whole hierarchy renders server-side on first load - there is no lazy-loading a branch's children on demand, so a tree with a very large number of nodes renders (and ships) all of them up front.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Basic</p>
                            <div class="mt-3 max-w-md">
                                @template.ashlar.components.tree(
                                items = @`
                                    @template.ashlar.components.tree-item(label = "Fruit")
                                    @template.ashlar.components.tree-item(label = "Vegetables")
                                    @template.ashlar.components.tree-item(label = "Grains")
                                `
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Files and folders</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Folders are expandable; files are leaves with no expand control. A file with an <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">href</code> is a real link.</p>
                            <div class="mt-3 max-w-md">
                                @template.ashlar.components.tree(
                                items = @`
                                    @template.ashlar.components.tree-item(label = "src", icon = Icon.FOLDER, expanded = true, children = @`
                                        @template.ashlar.components.tree-item(label = "components", icon = Icon.FOLDER, expanded = true, children = @`
                                            @template.ashlar.components.tree-item(label = "button.jte", icon = Icon.FILE, href = "#")
                                            @template.ashlar.components.tree-item(label = "icon.jte", icon = Icon.FILE, href = "#")
                                        `)
                                        @template.ashlar.components.tree-item(label = "pages", icon = Icon.FOLDER, children = @`
                                            @template.ashlar.components.tree-item(label = "home.jte", icon = Icon.FILE, href = "#")
                                        `)
                                        @template.ashlar.components.tree-item(label = "app.js", icon = Icon.FILE, href = "#")
                                    `)
                                    @template.ashlar.components.tree-item(label = "static", icon = Icon.FOLDER, children = @`
                                        @template.ashlar.components.tree-item(label = "styles.css", icon = Icon.FILE, href = "#")
                                    `)
                                    @template.ashlar.components.tree-item(label = "README.md", icon = Icon.FILE_TEXT, href = "#")
                                `
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Organizational hierarchy</p>
                            <div class="mt-3 max-w-md">
                                @template.ashlar.components.tree(
                                items = @`
                                    @template.ashlar.components.tree-item(label = "Acme Manufacturing", icon = Icon.USER, expanded = true, children = @`
                                        @template.ashlar.components.tree-item(label = "Engineering", icon = Icon.USER, children = @`
                                            @template.ashlar.components.tree-item(label = "Platform team", icon = Icon.USER)
                                            @template.ashlar.components.tree-item(label = "Product team", icon = Icon.USER)
                                        `)
                                        @template.ashlar.components.tree-item(label = "Sales", icon = Icon.USER)
                                        @template.ashlar.components.tree-item(label = "Support", icon = Icon.USER)
                                    `)
                                `
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Deep nesting</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Indentation and <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-level</code> both compound naturally with depth.</p>
                            <div class="mt-3 max-w-md">
                                @template.ashlar.components.tree(
                                items = @`
                                    @template.ashlar.components.tree-item(label = "Level 1", expanded = true, children = @`
                                        @template.ashlar.components.tree-item(label = "Level 2", expanded = true, children = @`
                                            @template.ashlar.components.tree-item(label = "Level 3", expanded = true, children = @`
                                                @template.ashlar.components.tree-item(label = "Level 4", expanded = true, children = @`
                                                    @template.ashlar.components.tree-item(label = "Level 5 - a leaf")
                                                `)
                                            `)
                                        `)
                                    `)
                                `
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Long labels</p>
                            <div class="mt-3 max-w-md">
                                @template.ashlar.components.tree(
                                items = @`
                                    @template.ashlar.components.tree-item(label = "Quarterly compliance reports for enterprise accounts", icon = Icon.FOLDER, expanded = true, children = @`
                                        @template.ashlar.components.tree-item(label = "Q3 2026 enterprise compliance summary and audit trail.pdf", icon = Icon.FILE, href = "#")
                                    `)
                                `
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Multiple instances stay independent</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Expanding one tree never affects the other.</p>
                            <div class="mt-3 grid gap-4 sm:grid-cols-2">
                                <div class="max-w-sm">
                                    <p class="mb-2 text-sm font-medium text-slate-700 dark:text-slate-300">Categories</p>
                                    @template.ashlar.components.tree(
                                    items = @`
                                        @template.ashlar.components.tree-item(label = "Electronics", icon = Icon.FOLDER, children = @`
                                            @template.ashlar.components.tree-item(label = "Laptops")
                                            @template.ashlar.components.tree-item(label = "Phones")
                                        `)
                                        @template.ashlar.components.tree-item(label = "Furniture", icon = Icon.FOLDER, children = @`
                                            @template.ashlar.components.tree-item(label = "Chairs")
                                        `)
                                    `
                                    )
                                </div>
                                <div class="max-w-sm">
                                    <p class="mb-2 text-sm font-medium text-slate-700 dark:text-slate-300">Regions</p>
                                    @template.ashlar.components.tree(
                                    items = @`
                                        @template.ashlar.components.tree-item(label = "Americas", icon = Icon.FOLDER, children = @`
                                            @template.ashlar.components.tree-item(label = "United States")
                                            @template.ashlar.components.tree-item(label = "Canada")
                                        `)
                                        @template.ashlar.components.tree-item(label = "Europe", icon = Icon.FOLDER, children = @`
                                            @template.ashlar.components.tree-item(label = "Germany")
                                        `)
                                    `
                                    )
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composition</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Tree is the wrapper half of a wrapper/item pair, the same split List, Timeline, and Stepper each use. <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">tree-item</code> is not a separate catalog component; it only ever exists composed into Tree's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">items</code> or a parent item's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">children</code> slot, documented here in the table below. Every level of nesting is just another ordinary <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">tree-item</code> call composed into the one above it - there is no separate "root item" or "branch item" type.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Pitfalls &amp; Troubleshooting</h3>
                    <ul class="mt-2 list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">A row that is both a link and a parent only expands from its chevron.</strong> Clicking the label text itself navigates the link immediately - it never toggles the row, even though the row also has children. This is deliberate (one click shouldn't have two conflicting meanings), but it means the expand control is a small, specific target on that kind of row.</li>
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">There is no disabled state.</strong> Neither tree-item's params nor tree.js support disabling a node - every node is always interactive.</li>
                    </ul>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Ashlar provides</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>The real WAI-ARIA APG tree pattern: <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="tree"</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="treeitem"</code>, server-computed <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-level</code>, and <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-expanded</code> only on items that actually have children.</li>
                                <li>Full roving-tabindex keyboard navigation per the APG: Arrow Up/Down between visible rows, Arrow Right/Left to expand/collapse or move to a child/parent, Home/End, and Enter/Space - exactly one row is ever a Tab stop at a time.</li>
                                <li>A collapsed branch removed from both the Tab order and this component's own keyboard navigation (via <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">inert</code>), not merely hidden visually.</li>
                                <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-posinset</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-setsize</code> are correctly left out - with the full hierarchy already in the DOM, assistive tech computes both itself.</li>
                            </ul>
                        </div>
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>Your own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-labelledby</code> on the tree if this particular instance needs to be identifiable as more than "a tree" - there's no APG-assigned landmark name to borrow for a generic one.</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div class="flex flex-col gap-6">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">tree</p>
                        @template.layout.api-reference(apiParams = treeApi)
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">tree-item</p>
                        @template.layout.api-reference(apiParams = treeItemApi)
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/list" class="text-blue-700 hover:underline dark:text-blue-400">List</a> for a flat set of rows with no hierarchy &middot;
                        <a href="${contextPath}/components/accordion" class="text-blue-700 hover:underline dark:text-blue-400">Accordion</a>, which shares this component's expand/collapse grid-animation technique &middot;
                        <a href="${contextPath}/components/dropdown-menu" class="text-blue-700 hover:underline dark:text-blue-400">Dropdown Menu</a>, another roving-focus keyboard pattern in this library
                    </p>
                </div>
            </div>
        </section>
`
)
