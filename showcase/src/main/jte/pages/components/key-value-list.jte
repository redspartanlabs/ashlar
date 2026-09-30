@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Key-Value List",
content = @`
    @template.layout.component-nav(currentSlug = "key-value-list", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Key-Value List</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A semantic <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">&lt;dl&gt;</code> for labeled record data - record details, metadata, configuration summaries. No surface, no JavaScript, no opinion about what a value is.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic</h3>
                    <div class="mt-3 max-w-md rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.key-value-list(
                        content = @`
                            @template.ashlar.utilities.key-value(label = "Account ID", value = @`ACME-10482`)
                            @template.ashlar.utilities.key-value(label = "Owner", value = @`Dana Whitfield`)
                            @template.ashlar.utilities.key-value(label = "Status", value = @`Active`)
                            @template.ashlar.utilities.key-value(label = "Created", value = @`September 18, 2026`)
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Long label</h3>
                    <div class="mt-3 max-w-md rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.key-value-list(
                        content = @`
                            @template.ashlar.utilities.key-value(label = "Quarterly compliance review reference number", value = @`QCR-2026-Q3-00481`)
                            @template.ashlar.utilities.key-value(label = "Status", value = @`Active`)
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Long value</h3>
                    <div class="mt-3 max-w-md rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.key-value-list(
                        content = @`
                            @template.ashlar.utilities.key-value(label = "Description", value = @`A complete, chronological record of every access change, permission grant, and administrative action taken across all enterprise accounts.`)
                            @template.ashlar.utilities.key-value(label = "Contact", value = @`compliance-notifications-enterprise-accounts@acme-enterprise-console.example.com`)
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Rich values</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A value can be a link, a Badge, or any other existing component - Key-Value List never needs to know what it is.</p>
                    <div class="mt-3 max-w-md rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.key-value-list(
                        content = @`
                            @template.ashlar.utilities.key-value(label = "Account ID", value = @`<a href="/components/key-value-list" class="text-blue-700 hover:underline dark:text-blue-400">ACME-10482</a>`)
                            @template.ashlar.utilities.key-value(label = "Status", value = @`@template.ashlar.components.badge(text = "Active", variant = "Success")`)
                            @template.ashlar.utilities.key-value(label = "Owner", value = @`Dana Whitfield`)
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Inside a Card, inside a Page Section</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Key-Value List owns semantics and layout only - the Card owns the visual surface.</p>
                    <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.page-section(
                        title = "Account Information",
                        description = "Key details for this account.",
                        content = @`
                            @template.ashlar.components.card(
                            body = @`
                                @template.ashlar.utilities.key-value-list(
                                content = @`
                                    @template.ashlar.utilities.key-value(label = "Account ID", value = @`ACME-10482`)
                                    @template.ashlar.utilities.key-value(label = "Owner", value = @`Dana Whitfield`)
                                    @template.ashlar.utilities.key-value(label = "Plan", value = @`Enterprise`)
                                    @template.ashlar.utilities.key-value(label = "Status", value = @`@template.ashlar.components.badge(text = "Active", variant = "Success")`)
                                `
                                )
                            `
                            )
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Narrow / mobile layout</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Below the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">sm</code> breakpoint each entry stacks its label above its value.</p>
                    <div class="mt-3 max-w-xs rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                        @template.ashlar.utilities.key-value-list(
                        content = @`
                            @template.ashlar.utilities.key-value(label = "Account ID", value = @`ACME-10482`)
                            @template.ashlar.utilities.key-value(label = "Owner", value = @`Dana Whitfield`)
                            @template.ashlar.utilities.key-value(label = "Status", value = @`@template.ashlar.components.badge(text = "Active", variant = "Success")`)
                        `
                        )
                    </div>
                </div>
            </div>
        </section>
`
)
