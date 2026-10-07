@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import com.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.components.card(\n"
        + "    title = \"Account details\",\n"
        + "    body = @" + backtick + "<p>Acme Manufacturing - Enterprise plan.</p>" + backtick + "\n"
        + ")";}
!{String basicCode = "@template.ashlar.components.card(\n"
        + "    body = @" + backtick + "<p>A Card with no title and no footer - just the surface and its content.</p>" + backtick + "\n"
        + ")";}
!{String withHeaderCode = "@template.ashlar.components.card(\n"
        + "    title = \"Account details\",\n"
        + "    body = @" + backtick + "<p>Acme Manufacturing has been an active enterprise account since January 2024, currently on the Enterprise plan with 3 seats.</p>" + backtick + "\n"
        + ")";}
!{String withFooterCode = "@template.ashlar.components.card(\n"
        + "    title = \"Delete account\",\n"
        + "    body = @" + backtick + "<p>This permanently removes the account and all associated records. This action cannot be undone.</p>" + backtick + ",\n"
        + "    footer = @" + backtick + "\n"
        + "        @template.ashlar.components.button(text = \"Cancel\", type = \"Ghost\")\n"
        + "        @template.ashlar.components.button(text = \"Delete\", type = \"Danger\")\n"
        + "    " + backtick + "\n"
        + ")";}
!{List<ApiParam> cardApi = List.of(
    ApiParam.optional("title", "String", "\"\"", "Rendered as a real <h3> when non-empty, in its own bordered header region."),
    ApiParam.optional("body", "gg.jte.Content", "null", "The main content. Omitted entirely from the DOM when null."),
    ApiParam.optional("footer", "gg.jte.Content", "null", "A flex row (right-aligned) for composed actions - ordinary Button calls, not a Card-specific action API.")
);}
!{List<PageAnchor> sections = List.of(
    new PageAnchor("quick-start", "Quick Start"),
    new PageAnchor("variants", "Variants & States"),
    new PageAnchor("composition", "Composition"),
    new PageAnchor("accessibility", "Accessibility"),
    new PageAnchor("api-reference", "API Reference"),
    new PageAnchor("related", "Related Components")
);}

@template.layout.page(
contextPath = contextPath,
title = "Card",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "card", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A content surface - border, radius, and background only. The page decides width, placement, and spacing around it.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3 max-w-sm">
                    @template.ashlar.components.card(
                    title = "Account details",
                    body = @`<p>Acme Manufacturing - Enterprise plan.</p>`
                    )
                </div>
            `
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
                    @template.layout.example(
                    label = "Basic (body only)",
                    code = basicCode,
                    content = @`
                        <div class="max-w-sm">
                            @template.ashlar.components.card(
                            body = @`
                                <p>A Card with no title and no footer - just the surface and its content.</p>
                            `
                            )
                        </div>
                    `
                    )

                    @template.layout.example(
                    label = "With a header",
                    code = withHeaderCode,
                    content = @`
                        <div class="max-w-sm">
                            @template.ashlar.components.card(
                            title = "Account details",
                            body = @`
                                <p>Acme Manufacturing has been an active enterprise account since January 2024, currently on the Enterprise plan with 3 seats.</p>
                            `
                            )
                        </div>
                    `
                    )

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">With a header, body, and footer</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The footer is a plain composition slot - these are the same Button components used everywhere else, not a Card-specific action API.</p>
                        @template.layout.example(
                        code = withFooterCode,
                        content = @`
                            <div class="mt-3 max-w-sm">
                                @template.ashlar.components.card(
                                title = "Delete account",
                                body = @`
                                    <p>This permanently removes the account and all associated records. This action cannot be undone.</p>
                                `,
                                footer = @`
                                    @template.ashlar.components.button(text = "Cancel", type = "Ghost")
                                    @template.ashlar.components.button(text = "Delete", type = "Danger")
                                `
                                )
                            </div>
                        `
                        )
                    </div>
                </div>
            `
            )
        </section>

        <section id="composition" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Composition",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Card stays a neutral container - Badge, Divider, Text Input, and Empty State all render normally inside it, unaware they're inside a Card.</p>
                <div class="mt-3 grid gap-4 sm:grid-cols-2">
                    @template.ashlar.components.card(
                    title = "Kestrel Aerospace",
                    body = @`
                        <div class="flex items-center gap-2">
                            <span class="text-slate-500 dark:text-slate-400">Status:</span>
                            @template.ashlar.components.badge(text = "Active", variant = "Success")
                        </div>
                        <div class="my-4">
                            @template.ashlar.components.divider()
                        </div>
                        <p class="text-slate-500 dark:text-slate-400">Government - onboarded 2023-07-14</p>
                    `
                    )
                    @template.ashlar.components.card(
                    title = "Invite a teammate",
                    body = @`
                        @template.ashlar.components.text-input(
                        name = "cardContactEmail",
                        label = "Email address",
                        placeholder = "name@company.com"
                        )
                    `,
                    footer = @`
                        @template.ashlar.components.button(text = "Send Invite", type = "Primary", size = "Small")
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
                            <li>A real &lt;h3&gt; for the title, participating in the page's own heading structure rather than being invisible to it.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="region"</code> yourself, at the call site, on the rare Card that genuinely is one of the page's major landmarks - this component never adds it by default, since most Cards on a page (a grid of a dozen, say) shouldn't each become one.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.layout.api-reference(apiParams = cardApi)
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/stat-card" class="text-blue-700 hover:underline dark:text-blue-400">Stat Card</a>, a visually similar surface for a single metric, built independently rather than on top of this &middot;
                    <a href="${contextPath}/components/empty-state" class="text-blue-700 hover:underline dark:text-blue-400">Empty State</a>, which commonly nests inside a Card's body &middot;
                    <a href="${contextPath}/components/modal" class="text-blue-700 hover:underline dark:text-blue-400">Modal</a>, which shares this component's real-&lt;h3&gt; title treatment
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "card", contextPath = contextPath)
    </div>
`
)
