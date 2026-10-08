@import dev.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import dev.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

<%-- Lean grammar, deliberately: no How to Use, Best Practices, or
     Common Patterns section - the intro paragraph and the two
     Variants & States groups already say everything a caller needs,
     and forcing those headings here would just restate it. --%>
!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.components.alert(\n"
        + "    variant = \"Warning\",\n"
        + "    title = \"Approaching seat limit\",\n"
        + "    body = @" + backtick + "<p>This account is using 9 of 10 available seats.</p>" + backtick + "\n"
        + ")";}
!{String semanticVariantsCode = "@template.ashlar.components.alert(\n"
        + "    variant = \"Info\",\n"
        + "    title = \"Scheduled maintenance\",\n"
        + "    body = @" + backtick + "<p>The billing service will be unavailable Saturday 10:00 PM - 2:00 AM ET for planned maintenance.</p>" + backtick + "\n"
        + ")\n"
        + "@template.ashlar.components.alert(\n"
        + "    variant = \"Success\",\n"
        + "    title = \"Changes saved\",\n"
        + "    body = @" + backtick + "<p>Your account settings were updated successfully.</p>" + backtick + "\n"
        + ")\n"
        + "@template.ashlar.components.alert(\n"
        + "    variant = \"Warning\",\n"
        + "    title = \"Approaching seat limit\",\n"
        + "    body = @" + backtick + "<p>This account is using 9 of 10 available seats. Add seats before inviting more teammates.</p>" + backtick + "\n"
        + ")\n"
        + "@template.ashlar.components.alert(\n"
        + "    variant = \"Error\",\n"
        + "    title = \"Unable to process payment\",\n"
        + "    body = @" + backtick + "<p>The card on file was declined. Update your payment method to avoid a service interruption.</p>" + backtick + "\n"
        + ")";}
!{String bodyWithoutTitleCode = "@template.ashlar.components.alert(\n"
        + "    variant = \"Info\",\n"
        + "    body = @" + backtick + "<p>Reports are generated nightly at 1:00 AM ET.</p>" + backtick + "\n"
        + ")\n"
        + "@template.ashlar.components.alert(\n"
        + "    variant = \"Warning\",\n"
        + "    body = @" + backtick + "<p>You have unsaved changes. Leaving this page will discard them.</p>" + backtick + "\n"
        + ")";}
!{List<ApiParam> alertApi = List.of(
    ApiParam.optional("variant", "String", "\"Info\"", "Info/Success/Warning/Error. Unrecognized values silently fall back to Info. Note: Error, not Danger, unlike Button/Toast/Badge."),
    ApiParam.optional("title", "String", "\"\"", "Rendered as a real <h3> when non-empty. Omit it for a short notice that doesn't need one."),
    ApiParam.optional("body", "gg.jte.Content", "null", "The message itself. Omitted entirely from the DOM when null, rather than rendering an empty container.")
);}
!{List<PageAnchor> sections = List.of(
    new PageAnchor("quick-start", "Quick Start"),
    new PageAnchor("variants", "Variants & States"),
    new PageAnchor("pitfalls", "Pitfalls"),
    new PageAnchor("accessibility", "Accessibility"),
    new PageAnchor("api-reference", "API Reference"),
    new PageAnchor("related", "Related Components")
);}

@template.layout.page(
contextPath = contextPath,
title = "Alert",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "alert", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A persistent, in-flow contextual message - not a Toast. No JavaScript, no dismiss button, no timer; it's part of the page for as long as the condition it describes is true.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3 max-w-sm">
                    @template.ashlar.components.alert(
                    variant = "Warning",
                    title = "Approaching seat limit",
                    body = @`<p>This account is using 9 of 10 available seats.</p>`
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
                    label = "Semantic variants, with a title",
                    code = semanticVariantsCode,
                    content = @`
                        <div class="flex flex-col gap-4">
                            @template.ashlar.components.alert(
                            variant = "Info",
                            title = "Scheduled maintenance",
                            body = @`
                                <p>The billing service will be unavailable Saturday 10:00 PM - 2:00 AM ET for planned maintenance.</p>
                            `
                            )
                            @template.ashlar.components.alert(
                            variant = "Success",
                            title = "Changes saved",
                            body = @`
                                <p>Your account settings were updated successfully.</p>
                            `
                            )
                            @template.ashlar.components.alert(
                            variant = "Warning",
                            title = "Approaching seat limit",
                            body = @`
                                <p>This account is using 9 of 10 available seats. Add seats before inviting more teammates.</p>
                            `
                            )
                            @template.ashlar.components.alert(
                            variant = "Error",
                            title = "Unable to process payment",
                            body = @`
                                <p>The card on file was declined. Update your payment method to avoid a service interruption.</p>
                            `
                            )
                        </div>
                    `
                    )

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Body without a title</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The title is optional - a short notice doesn't need one.</p>
                        @template.layout.example(
                        code = bodyWithoutTitleCode,
                        content = @`
                            <div class="mt-3 flex flex-col gap-4">
                                @template.ashlar.components.alert(
                                variant = "Info",
                                body = @`
                                    <p>Reports are generated nightly at 1:00 AM ET.</p>
                                `
                                )
                                @template.ashlar.components.alert(
                                variant = "Warning",
                                body = @`
                                    <p>You have unsaved changes. Leaving this page will discard them.</p>
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

        <section id="pitfalls" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Pitfalls & Troubleshooting",
            content = @`
                <ul class="list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">An Alert injected after page load is not automatically announced.</strong> It has no live-region role by design (see Accessibility) - if your application adds one dynamically via JavaScript rather than on initial render, wrap it in your own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="status"</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-live</code> region, or reach for Toast instead.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">The fourth variant is <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">"Error"</code>, not <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">"Danger"</code>.</strong> Button, Toast, and Badge all spell their equivalent variant Danger - passing that string here silently falls back to Info rather than erroring.</li>
                </ul>
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
                            <li>A sr-only severity label ("Warning:", etc.) read before the title/body, so meaning never depends on color or the decorative icon alone.</li>
                            <li>A real &lt;h3&gt; for the title when one is given, so it participates in the page's own heading structure rather than being invisible to it.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Your own live-region wiring if an alert is ever inserted dynamically after the initial page render - this component intentionally has none.</li>
                            <li>A heading level for the page that makes sense with Alert's title landing at &lt;h3&gt;.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.layout.api-reference(apiParams = alertApi)
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/toast" class="text-blue-700 hover:underline dark:text-blue-400">Toast</a> for a transient, JS-managed notification instead of a persistent one &middot;
                    <a href="${contextPath}/components/badge" class="text-blue-700 hover:underline dark:text-blue-400">Badge</a> for a smaller inline severity indicator &middot;
                    <a href="${contextPath}/components/card" class="text-blue-700 hover:underline dark:text-blue-400">Card</a>, which shares this component's real-&lt;h3&gt; title treatment
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "alert", contextPath = contextPath)
    </div>
`
)
