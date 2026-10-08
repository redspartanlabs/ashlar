@import dev.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import dev.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

<%-- Batch 12 (final) documentation page. Existing "Basic" / "Long label" /
     "Long value" / "Rich values" / "Inside a Card, inside a Page Section"
     / "Narrow / mobile layout" examples kept verbatim. Added: Quick
     Start, formal grammar headings, How to Use, Composition (key-value
     documented here, not as a standalone page), Accessibility, two API
     Reference tables, Related Components.

     Verified against source before writing:
     - key-value-list.jte's own single @param (content) and doc comment.
       CONFIRMED this is a genuine semantic <dl> for labeled record data
       (record details, metadata, configuration summaries) - a real
       utility built on the definition-list pattern, not a generic
       presentation primitive invented for this purpose. Confirmed
       content is a single gg.jte.Content slot, not a Java collection:
       the caller supplies real key-value.jte entries, so a value can be
       plain text, a link, a Badge, or any other component without this
       template ever needing to know what it is.
     - key-value.jte's own 2 @params (label, value - both required) and
       doc comment. CONFIRMED a value is gg.jte.Content, not String,
       matching list-item.jte's own trailing slot exactly - this
       template has no more business understanding its content than
       list-item.jte understands its own trailing content. Confirmed the
       wrapper <div> around each dt/dd pair is valid <dl> content per
       the HTML spec (a <dl> may hold bare dt/dd groups or
       <div>-wrapped ones) and is specifically what makes a per-entry
       CSS grid possible - each pair gets its own two-column track
       without key-value-list.jte having to flatten every dt/dd into one
       shared grid. Confirmed the responsive reflow (stacked by default,
       side-by-side from sm: up) matches page-header.jte's and
       page-shell.jte's own mobile-first convention. --%>
!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.utilities.key-value-list(\n"
        + "    content = @" + backtick + "\n"
        + "        @template.ashlar.utilities.key-value(label = \"Account ID\", value = @" + backtick + "ACME-10482" + backtick + ")\n"
        + "        @template.ashlar.utilities.key-value(label = \"Owner\", value = @" + backtick + "Dana Whitfield" + backtick + ")\n"
        + "    " + backtick + "\n"
        + ")";}
!{List<ApiParam> keyValueListApi = List.of(
    ApiParam.required("content", "gg.jte.Content", "One or more key-value(...) entries.")
);}
!{List<ApiParam> keyValueApi = List.of(
    ApiParam.required("label", "String", "The term - rendered as a real <dt>."),
    ApiParam.required("value", "gg.jte.Content", "The description - plain text, a link, a Badge, or any other composed component, rendered as a real <dd>.")
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
title = "Key-Value List",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "key-value-list", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A semantic <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">&lt;dl&gt;</code> for labeled record data - record details, metadata, configuration summaries. No surface, no JavaScript, no opinion about what a value is.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3 max-w-md rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                    @template.ashlar.utilities.key-value-list(
                    content = @`
                        @template.ashlar.utilities.key-value(label = "Account ID", value = @`ACME-10482`)
                        @template.ashlar.utilities.key-value(label = "Owner", value = @`Dana Whitfield`)
                    `
                    )
                </div>
            `
            )
        </section>

        <section id="how-to-use" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "How to Use",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Reach for Key-Value List any time you're showing a fixed set of labeled facts about one record - not a repeating collection of similar rows, which is <a href="${contextPath}/components/list" class="text-blue-700 hover:underline dark:text-blue-400">List</a>'s job. It has no surface of its own, so pair it with a <a href="${contextPath}/components/card" class="text-blue-700 hover:underline dark:text-blue-400">Card</a> whenever the details need a visible boundary.</p>
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
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Long label</p>
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
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Long value</p>
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
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Narrow / mobile layout</p>
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
            `
            )
        </section>

        <section id="composition" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Composition",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Key-Value List is the wrapper half of a wrapper/item pair, the same split List and Timeline use. <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">key-value</code> is not a separate catalog component; it only ever exists composed into Key-Value List's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">content</code> slot, documented here in the table below. A value can be a link, a Badge, or any other existing component - shown below - since the template never needs to understand what it is.</p>
                <div class="mt-3 max-w-md rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                    @template.ashlar.utilities.key-value-list(
                    content = @`
                        @template.ashlar.utilities.key-value(label = "Account ID", value = @`<a href="/components/key-value-list" class="text-blue-700 hover:underline dark:text-blue-400">ACME-10482</a>`)
                        @template.ashlar.utilities.key-value(label = "Status", value = @`@template.ashlar.components.badge(text = "Active", variant = "Success")`)
                        @template.ashlar.utilities.key-value(label = "Owner", value = @`Dana Whitfield`)
                    `
                    )
                </div>
                <p class="mt-3 text-sm text-slate-500 dark:text-slate-400">Key-Value List owns semantics and layout only - a surrounding <a href="${contextPath}/components/card" class="text-blue-700 hover:underline dark:text-blue-400">Card</a> owns the visual surface, inside a <a href="${contextPath}/components/page-section" class="text-blue-700 hover:underline dark:text-blue-400">Page Section</a>.</p>
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
                            <li>A real &lt;dl&gt; with real &lt;dt&gt;/&lt;dd&gt; pairs, already expressing "a list of term/description groups" natively with no role or aria-* needed.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Labels that read clearly on their own - this utility has no opinion about wording.</li>
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
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">key-value-list</p>
                        @template.layout.api-reference(apiParams = keyValueListApi)
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">key-value</p>
                        @template.layout.api-reference(apiParams = keyValueApi)
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
                    <a href="${contextPath}/components/list" class="text-blue-700 hover:underline dark:text-blue-400">List</a> for a repeating collection of rows instead of fixed record details &middot;
                    <a href="${contextPath}/components/card" class="text-blue-700 hover:underline dark:text-blue-400">Card</a>, the usual surface &middot;
                    <a href="${contextPath}/components/badge" class="text-blue-700 hover:underline dark:text-blue-400">Badge</a>, a common value
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "key-value-list", contextPath = contextPath)
    </div>
`
)
