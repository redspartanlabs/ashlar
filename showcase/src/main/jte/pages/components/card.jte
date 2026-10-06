@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param String contextPath

<%-- Batch 5 documentation page. Existing "Basic (body only)" / "With a
     header" / "With a header, body, and footer" / "Composing existing
     components inside a Card" examples kept verbatim, moved under
     Variants & States/Composition. Added: Quick Start, formal grammar
     headings, Accessibility, API Reference, Related Components.

     Verified against source before writing:
     - card.jte's own 3 @params and its own doc comment in full. Confirmed
       it is a content surface, not a layout primitive - no width, margin,
       or grid placement of its own, so the page stays responsible for
       where and how wide it sits.
     - Confirmed role="region" is deliberately NOT applied: a dashboard of
       a dozen Cards would turn every one into a landmark, defeating the
       point of landmarks as a short list of a page's major regions. A
       page that genuinely needs one particular Card to be a landmark adds
       that itself at the call site - this component doesn't guess.
     - Confirmed title always renders as a real <h3> when present, the
       same choice Alert and Modal make for their own optional titles -
       and confirmed via a direct comparison that Stat Card, despite a
       visually similar bordered surface, does NOT reuse this component's
       title/body/footer slots at all (see Stat Card's own page); the two
       only share a surface convention, not an implementation.
     - Confirmed body and footer are both gg.jte.Content, not a richer
       API of their own - footer is a plain flex row (justify-end),
       intended for ordinary composed Button calls rather than a
       Card-specific action API, exactly like empty-state.jte's own
       action slot. --%>
!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.components.card(\n"
        + "    title = \"Account details\",\n"
        + "    body = @" + backtick + "<p>Acme Manufacturing - Enterprise plan.</p>" + backtick + "\n"
        + ")";}
!{List<ApiParam> cardApi = List.of(
    ApiParam.optional("title", "String", "\"\"", "Rendered as a real <h3> when non-empty, in its own bordered header region."),
    ApiParam.optional("body", "gg.jte.Content", "null", "The main content. Omitted entirely from the DOM when null."),
    ApiParam.optional("footer", "gg.jte.Content", "null", "A flex row (right-aligned) for composed actions - ordinary Button calls, not a Card-specific action API.")
);}

@template.layout.page(
contextPath = contextPath,
title = "Card",
content = @`
    @template.layout.component-nav(currentSlug = "card", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Card</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A content surface - border, radius, and background only. The page decides width, placement, and spacing around it.</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3 max-w-sm">
                        @template.ashlar.components.card(
                        title = "Account details",
                        body = @`<p>Acme Manufacturing - Enterprise plan.</p>`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Basic (body only)</p>
                            <div class="mt-3 max-w-sm">
                                @template.ashlar.components.card(
                                body = @`
                                    <p>A Card with no title and no footer - just the surface and its content.</p>
                                `
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">With a header</p>
                            <div class="mt-3 max-w-sm">
                                @template.ashlar.components.card(
                                title = "Account details",
                                body = @`
                                    <p>Acme Manufacturing has been an active enterprise account since January 2024, currently on the Enterprise plan with 3 seats.</p>
                                `
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">With a header, body, and footer</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The footer is a plain composition slot - these are the same Button components used everywhere else, not a Card-specific action API.</p>
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
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composition</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Card stays a neutral container - Badge, Divider, Text Input, and Empty State all render normally inside it, unaware they're inside a Card.</p>
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
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
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
                </div>

                <div>
                    @template.layout.api-reference(apiParams = cardApi)
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/stat-card" class="text-blue-700 hover:underline dark:text-blue-400">Stat Card</a>, a visually similar surface for a single metric, built independently rather than on top of this &middot;
                        <a href="${contextPath}/components/empty-state" class="text-blue-700 hover:underline dark:text-blue-400">Empty State</a>, which commonly nests inside a Card's body &middot;
                        <a href="${contextPath}/components/modal" class="text-blue-700 hover:underline dark:text-blue-400">Modal</a>, which shares this component's real-&lt;h3&gt; title treatment
                    </p>
                </div>
            </div>
        </section>
`
)
