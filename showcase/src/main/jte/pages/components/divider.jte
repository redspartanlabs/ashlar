@import dev.redspartanlabs.ashlar.icon.Icon
@import dev.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import dev.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

<%-- Deliberately slim: no How to Use, Composition, Best Practices, or
     Pitfalls section. This is a one-param visual separator with nothing
     left to say once Variants & States and Accessibility have said it -
     forcing those headings in here would be checklist-filling, not
     documentation. --%>
!{String quickStartCode = """
    @template.ashlar.components.divider()""";}
!{String verticalCode = """
    @template.ashlar.components.button(text = "Save", type = "Ghost", size = "Small", icon = Icon.SAVE, iconOnly = true)
    @template.ashlar.components.button(text = "Edit", type = "Ghost", size = "Small", icon = Icon.EDIT, iconOnly = true)
    @template.ashlar.components.divider(orientation = "Vertical")
    @template.ashlar.components.button(text = "Delete", type = "Ghost", size = "Small", icon = Icon.DELETE, iconOnly = true)""";}
!{List<ApiParam> dividerApi = List.of(
    ApiParam.optional("orientation", "String", "\"Horizontal\"", "Horizontal or Vertical. Unrecognized values silently fall back to Horizontal. Horizontal renders a real <hr>; Vertical renders role=\"separator\" aria-orientation=\"vertical\" - there is no native vertical <hr>.")
);}
!{List<PageAnchor> sections = List.of(
    new PageAnchor("quick-start", "Quick Start"),
    new PageAnchor("variants", "Variants & States"),
    new PageAnchor("accessibility", "Accessibility"),
    new PageAnchor("api-reference", "API Reference"),
    new PageAnchor("related", "Related Components")
);}

@template.layout.page(
contextPath = contextPath,
title = "Divider",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "divider", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A visual separator - a real &lt;hr&gt; horizontally, <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="separator"</code> vertically. The surrounding layout owns all spacing; the divider only draws the line.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3 max-w-sm">
                    @template.ashlar.components.divider()
                </div>
            `
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Horizontal, between content sections</p>
                        <div class="mt-3 max-w-md">
                            <div>
                                <p class="text-sm font-medium text-slate-900 dark:text-slate-100">Account details</p>
                                <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Acme Manufacturing - ACC-1001</p>
                            </div>
                            <div class="my-4">
                                @template.ashlar.components.divider()
                            </div>
                            <div>
                                <p class="text-sm font-medium text-slate-900 dark:text-slate-100">Billing contact</p>
                                <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Dana Whitfield - dana.whitfield@acme.example</p>
                            </div>
                        </div>
                    </div>

                    @template.layout.example(
                    label = "Vertical, inside a toolbar",
                    code = verticalCode,
                    content = @`
                        <div class="flex h-10 items-center gap-3 rounded-md border border-slate-200 bg-slate-50 px-3 dark:border-slate-700 dark:bg-slate-800/50">
                            @template.ashlar.components.button(text = "Save", type = "Ghost", size = "Small", icon = Icon.SAVE, iconOnly = true)
                            @template.ashlar.components.button(text = "Edit", type = "Ghost", size = "Small", icon = Icon.EDIT, iconOnly = true)
                            @template.ashlar.components.divider(orientation = "Vertical")
                            @template.ashlar.components.button(text = "Delete", type = "Ghost", size = "Small", icon = Icon.DELETE, iconOnly = true)
                        </div>
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
                            <li>A real native &lt;hr&gt; for Horizontal - no ARIA needed, since its separator role and orientation are already implicit.</li>
                            <li>The WAI-ARIA APG's own toolbar-separator pattern for Vertical, since no native element expresses that orientation.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>All spacing around it - margin, gap, padding - the divider only draws the line itself.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.layout.api-reference(apiParams = dividerApi)
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/card" class="text-blue-700 hover:underline dark:text-blue-400">Card</a>, a common surface to use a horizontal Divider inside &middot;
                    <a href="${contextPath}/components/button" class="text-blue-700 hover:underline dark:text-blue-400">Button</a>, the usual neighbor for a vertical Divider in a toolbar
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "divider", contextPath = contextPath)
    </div>
`
)
