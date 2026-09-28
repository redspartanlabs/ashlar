@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Accordion",
content = @`
    @template.layout.component-nav(currentSlug = "accordion", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Accordion</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Independently expandable, vertically stacked sections - distinct from Tabs, which switch between peer panels. Real &lt;button&gt; disclosure controls; no custom keyboard handling needed. <strong class="font-medium text-slate-700 dark:text-slate-300">At most one section is open at a time</strong> - opening a section closes whichever one was open before it.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic - single section open at a time</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">All three start closed. Open one, then open another - the first closes automatically. Click an open section again to close it, leaving all three closed.</p>
                    <div class="mt-3 max-w-lg">
                        @template.ashlar.components.accordion(
                        id = "basicAccordion",
                        items = @`
                            @template.ashlar.components.accordion-item(
                            accordionId = "basicAccordion",
                            itemId = "one",
                            title = "Section One",
                            content = @`<p>Content for section one appears here once it's opened.</p>`
                            )
                            @template.ashlar.components.accordion-item(
                            accordionId = "basicAccordion",
                            itemId = "two",
                            title = "Section Two",
                            content = @`<p>Content for section two appears here once it's opened.</p>`
                            )
                            @template.ashlar.components.accordion-item(
                            accordionId = "basicAccordion",
                            itemId = "three",
                            title = "Section Three",
                            content = @`<p>Content for section three appears here once it's opened.</p>`
                            )
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Initially expanded, with a safety check</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Shipping <em>and</em> Returns are both marked <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">expanded = true</code> below - an invalid combination under the single-open contract. Only Shipping, the first one in document order, actually renders open; Returns silently renders closed. This is resolved entirely on the server - there is no flash of both being open before JavaScript "fixes" it.</p>
                    <div class="mt-3 max-w-lg">
                        @template.ashlar.components.accordion(
                        id = "faqAccordion",
                        items = @`
                            @template.ashlar.components.accordion-item(
                            accordionId = "faqAccordion",
                            itemId = "shipping",
                            title = "Shipping",
                            expanded = true,
                            content = @`<p>Standard shipping takes 3-5 business days. Expedited options are available at checkout.</p>`
                            )
                            @template.ashlar.components.accordion-item(
                            accordionId = "faqAccordion",
                            itemId = "returns",
                            title = "Returns",
                            expanded = true,
                            content = @`<p>Items may be returned within 30 days of delivery in their original condition.</p>`
                            )
                            @template.ashlar.components.accordion-item(
                            accordionId = "faqAccordion",
                            itemId = "warranty",
                            title = "Warranty",
                            content = @`<p>All products carry a 1-year limited warranty against manufacturing defects.</p>`
                            )
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composing existing components inside a section</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Accordion doesn't have its own rich-content API - a section's content is ordinary JTE content, so Badge and Button compose into it normally.</p>
                    <div class="mt-3 max-w-lg">
                        @template.ashlar.components.accordion(
                        id = "releaseAccordion",
                        items = @`
                            @template.ashlar.components.accordion-item(
                            accordionId = "releaseAccordion",
                            itemId = "v2",
                            title = "Version 2.4.0",
                            expanded = true,
                            content = @`
                                <div class="flex items-center gap-2">
                                    @template.ashlar.components.badge(text = "Latest", variant = "Success")
                                    <span>Released September 2026</span>
                                </div>
                                <p class="mt-2">Adds bulk export, improves Data Table sort performance, and fixes a rendering issue in Combobox under Firefox.</p>
                                @template.ashlar.components.button(text = "View full changelog", type = "Secondary", size = "Small")
                            `
                            )
                            @template.ashlar.components.accordion-item(
                            accordionId = "releaseAccordion",
                            itemId = "history",
                            title = "Release history",
                            content = @`
                                <p>Acme Manufacturing has been an active enterprise account since January 2024. Over that time the platform has moved from a single-region deployment to a multi-region architecture, added support for custom fields across every major record type, introduced role-based approval workflows for high-value transactions, and rebuilt the reporting engine to support scheduled exports. Earlier release notes are archived and available on request from your account representative.</p>
                            `
                            )
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Multiple Accordion instances stay independent</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Each Accordion enforces single-open on its own - opening a section in A never closes anything in B, even though both use the item id <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">"a"</code>.</p>
                    <div class="mt-3 grid gap-4 sm:grid-cols-2">
                        <div>
                            <p class="mb-2 text-sm font-medium text-slate-700 dark:text-slate-300">Accordion A</p>
                            @template.ashlar.components.accordion(
                            id = "instanceA",
                            items = @`
                                @template.ashlar.components.accordion-item(accordionId = "instanceA", itemId = "a", title = "A1", content = @`<p>Content from Accordion A.</p>`)
                                @template.ashlar.components.accordion-item(accordionId = "instanceA", itemId = "b", title = "A2", content = @`<p>Content from Accordion A.</p>`)
                            `
                            )
                        </div>
                        <div>
                            <p class="mb-2 text-sm font-medium text-slate-700 dark:text-slate-300">Accordion B</p>
                            @template.ashlar.components.accordion(
                            id = "instanceB",
                            items = @`
                                @template.ashlar.components.accordion-item(accordionId = "instanceB", itemId = "a", title = "B1", content = @`<p>Content from Accordion B.</p>`)
                                @template.ashlar.components.accordion-item(accordionId = "instanceB", itemId = "b", title = "B2", content = @`<p>Content from Accordion B.</p>`)
                            `
                            )
                        </div>
                    </div>
                </div>
            </div>
        </section>
`
)
