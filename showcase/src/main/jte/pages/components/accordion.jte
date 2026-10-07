@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import com.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

<%-- The one thing this page has to get right: Accordion has NO custom
     keyboard handling at all, confirmed directly in accordion.js - no
     roving tabindex, no Arrow Up/Down between headers. Every header is
     a genuinely separate, ordinary button in the normal Tab sequence;
     this is not the fuller WAI-ARIA APG accordion pattern some other
     libraries implement. --%>
!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.components.accordion(\n"
        + "    id = \"faqAccordion\",\n"
        + "    items = @" + backtick + "\n"
        + "        @template.ashlar.components.accordion-item(\n"
        + "            accordionId = \"faqAccordion\",\n"
        + "            itemId = \"shipping\",\n"
        + "            title = \"Shipping\",\n"
        + "            content = @" + backtick + "<p>Ships in 3-5 business days.</p>" + backtick + "\n"
        + "        )\n"
        + "    " + backtick + "\n"
        + ")";}
!{String basicCode = "@template.ashlar.components.accordion(\n"
        + "    id = \"basicAccordion\",\n"
        + "    items = @" + backtick + "\n"
        + "        @template.ashlar.components.accordion-item(\n"
        + "            accordionId = \"basicAccordion\",\n"
        + "            itemId = \"one\",\n"
        + "            title = \"Section One\",\n"
        + "            content = @" + backtick + "<p>Content for section one appears here once it's opened.</p>" + backtick + "\n"
        + "        )\n"
        + "        @template.ashlar.components.accordion-item(\n"
        + "            accordionId = \"basicAccordion\",\n"
        + "            itemId = \"two\",\n"
        + "            title = \"Section Two\",\n"
        + "            content = @" + backtick + "<p>Content for section two appears here once it's opened.</p>" + backtick + "\n"
        + "        )\n"
        + "        @template.ashlar.components.accordion-item(\n"
        + "            accordionId = \"basicAccordion\",\n"
        + "            itemId = \"three\",\n"
        + "            title = \"Section Three\",\n"
        + "            content = @" + backtick + "<p>Content for section three appears here once it's opened.</p>" + backtick + "\n"
        + "        )\n"
        + "    " + backtick + "\n"
        + ")";}
!{String safetyCheckCode = "@template.ashlar.components.accordion(\n"
        + "    id = \"faqAccordion\",\n"
        + "    items = @" + backtick + "\n"
        + "        @template.ashlar.components.accordion-item(\n"
        + "            accordionId = \"faqAccordion\",\n"
        + "            itemId = \"shipping\",\n"
        + "            title = \"Shipping\",\n"
        + "            expanded = true,\n"
        + "            content = @" + backtick + "<p>Standard shipping takes 3-5 business days. Expedited options are available at checkout.</p>" + backtick + "\n"
        + "        )\n"
        + "        @template.ashlar.components.accordion-item(\n"
        + "            accordionId = \"faqAccordion\",\n"
        + "            itemId = \"returns\",\n"
        + "            title = \"Returns\",\n"
        + "            expanded = true,\n"
        + "            content = @" + backtick + "<p>Items may be returned within 30 days of delivery in their original condition.</p>" + backtick + "\n"
        + "        )\n"
        + "        @template.ashlar.components.accordion-item(\n"
        + "            accordionId = \"faqAccordion\",\n"
        + "            itemId = \"warranty\",\n"
        + "            title = \"Warranty\",\n"
        + "            content = @" + backtick + "<p>All products carry a 1-year limited warranty against manufacturing defects.</p>" + backtick + "\n"
        + "        )\n"
        + "    " + backtick + "\n"
        + ")";}
!{String releaseNotesCode = "@template.ashlar.components.accordion(\n"
        + "    id = \"releaseAccordion\",\n"
        + "    items = @" + backtick + "\n"
        + "        @template.ashlar.components.accordion-item(\n"
        + "            accordionId = \"releaseAccordion\",\n"
        + "            itemId = \"v2\",\n"
        + "            title = \"Version 2.4.0\",\n"
        + "            expanded = true,\n"
        + "            content = @" + backtick + "\n"
        + "                <div class=\"flex items-center gap-2\">\n"
        + "                    @template.ashlar.components.badge(text = \"Latest\", variant = \"Success\")\n"
        + "                    <span>Released September 2026</span>\n"
        + "                </div>\n"
        + "                <p class=\"mt-2\">Adds bulk export, improves Data Table sort performance, and fixes a rendering issue in Combobox under Firefox.</p>\n"
        + "                @template.ashlar.components.button(text = \"View full changelog\", type = \"Secondary\", size = \"Small\")\n"
        + "            " + backtick + "\n"
        + "        )\n"
        + "        @template.ashlar.components.accordion-item(\n"
        + "            accordionId = \"releaseAccordion\",\n"
        + "            itemId = \"history\",\n"
        + "            title = \"Release history\",\n"
        + "            content = @" + backtick + "<p>Acme Manufacturing has been an active enterprise account since January 2024. Over that time the platform has moved from a single-region deployment to a multi-region architecture, added support for custom fields across every major record type, introduced role-based approval workflows for high-value transactions, and rebuilt the reporting engine to support scheduled exports. Earlier release notes are archived and available on request from your account representative.</p>" + backtick + "\n"
        + "        )\n"
        + "    " + backtick + "\n"
        + ")";}
!{List<ApiParam> accordionApi = List.of(
    ApiParam.required("id", "String", "A unique id for this instance - folds into every item's generated button/panel ids so two Accordions on one page never collide."),
    ApiParam.required("items", "gg.jte.Content", "One or more accordion-item(...) calls.")
);}
!{List<ApiParam> accordionItemApi = List.of(
    ApiParam.required("accordionId", "String", "Must match the enclosing accordion's own id - folds into this item's generated ids."),
    ApiParam.required("itemId", "String", "This item's own identity within the accordion. Only needs to be unique within one Accordion instance."),
    ApiParam.required("title", "String", "The section's heading text, wrapped in a real <h3>."),
    ApiParam.optional("expanded", "boolean", "false", "Requests this item render open initially. If more than one item in the same accordion asks for this, only the first in document order actually renders open - see Composition."),
    ApiParam.optional("content", "gg.jte.Content", "null", "The section's body, shown only while expanded.")
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
title = "Accordion",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "accordion", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Independently expandable, vertically stacked sections - distinct from Tabs, which switch between peer panels. Real &lt;button&gt; disclosure controls; no custom keyboard handling needed. <strong class="font-medium text-slate-700 dark:text-slate-300">At most one section is open at a time</strong> - opening a section closes whichever one was open before it.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3 max-w-lg">
                    @template.ashlar.components.accordion(
                    id = "quickStartAccordion",
                    items = @`
                        @template.ashlar.components.accordion-item(
                        accordionId = "quickStartAccordion",
                        itemId = "shipping",
                        title = "Shipping",
                        content = @`<p>Ships in 3-5 business days.</p>`
                        )
                    `
                    )
                </div>
            `
            )
        </section>

        <section id="how-to-use" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "How to Use",
            content = @`<p class="text-sm text-slate-500 dark:text-slate-400">Reach for Accordion for FAQs, settings groups, or any stacked set of sections where showing one at a time reduces visual noise. If more than one section genuinely needs to stay open simultaneously, this isn't the right component - the single-open contract isn't configurable. For switching between peer views of equal importance rather than disclosure sections, use <a href="${contextPath}/components/tabs" class="text-blue-700 hover:underline dark:text-blue-400">Tabs</a> instead.</p>`
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Basic - single section open at a time</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">All three start closed. Open one, then open another - the first closes automatically. Click an open section again to close it, leaving all three closed.</p>
                        @template.layout.example(
                        code = basicCode,
                        content = @`
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
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Initially expanded, with a safety check</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Shipping <em>and</em> Returns are both marked <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">expanded = true</code> below - an invalid combination under the single-open contract. Only Shipping, the first one in document order, actually renders open; Returns silently renders closed. This is resolved entirely on the server - there is no flash of both being open before JavaScript "fixes" it.</p>
                        @template.layout.example(
                        code = safetyCheckCode,
                        content = @`
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
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Multiple Accordion instances stay independent</p>
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
            `
            )
        </section>

        <section id="composition" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Composition",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Accordion is the wrapper half of a wrapper/item pair, the same split List, Tree, and Breadcrumbs use. <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">accordion-item</code> is not a separate catalog component; it only ever exists composed into Accordion's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">items</code> slot, documented here in the table below. Accordion has no rich-content API of its own - a section's content is ordinary JTE content, so Badge, Button, or anything else composes into it normally, shown below.</p>
                @template.layout.example(
                code = releaseNotesCode,
                content = @`
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
                `
                )
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
                            <li>A real &lt;button&gt;, wrapped in a real &lt;h3&gt;, with native Tab/Enter/Space - there is no custom keyboard handling, and no roving-tabindex arrow-key model between headers like Tabs or Tree use.</li>
                            <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-expanded</code> as the authoritative state - the chevron rotation is purely decorative CSS driven off the same button, so it can never disagree with what's announced.</li>
                            <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="region"</code> + <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-labelledby</code> on each panel, and <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">inert</code> while collapsed, so a collapsed panel's content is removed from both the Tab order and the accessibility tree, not merely hidden visually.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>A heading level for the page that makes sense with each section's title landing at &lt;h3&gt;.</li>
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
                <div class="flex flex-col gap-6">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">accordion</p>
                        @template.layout.api-reference(apiParams = accordionApi)
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">accordion-item</p>
                        @template.layout.api-reference(apiParams = accordionItemApi)
                    </div>
                </div>
            `
            )
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/tabs" class="text-blue-700 hover:underline dark:text-blue-400">Tabs</a> for switching between peer panels instead of stacked disclosure &middot;
                    <a href="${contextPath}/components/tree" class="text-blue-700 hover:underline dark:text-blue-400">Tree View</a>, which shares this component's expand/collapse grid-animation technique
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "accordion", contextPath = contextPath)
    </div>
`
)
