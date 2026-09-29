@import com.redspartanlabs.ashlar.icon.Icon

@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Tree View",
content = @`
    @template.layout.component-nav(currentSlug = "tree", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Tree View</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A semantic, keyboard-navigable hierarchy - not a data table, not a menu. The caller composes nested items; the component owns expand/collapse and tree keyboard behavior only. Click a chevron, or focus a row and try Arrow Up/Down/Left/Right, Home, End, and Enter.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic</h3>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Files and folders</h3>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Organizational hierarchy</h3>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Deep nesting</h3>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Long labels</h3>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Multiple instances stay independent</h3>
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
        </section>
`
)
