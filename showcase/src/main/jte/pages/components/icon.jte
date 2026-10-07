@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import com.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import com.redspartanlabs.ashlar.icon.Icon
@import java.util.List

@param String contextPath

!{String quickStartCode = """
    @template.ashlar.components.icon(icon = Icon.SETTINGS)""";}
!{String sizingCode = """
    @template.ashlar.components.icon(icon = Icon.STAR, classes = "h-3 w-3")
    @template.ashlar.components.icon(icon = Icon.STAR, classes = "h-5 w-5")
    @template.ashlar.components.icon(icon = Icon.STAR, classes = "h-8 w-8")
    @template.ashlar.components.icon(icon = Icon.STAR, classes = "h-12 w-12")""";}
!{String colorCode = """
    <span class="text-slate-400">@template.ashlar.components.icon(icon = Icon.BELL, classes = "h-6 w-6")</span>
    <span class="text-blue-600 dark:text-blue-400">@template.ashlar.components.icon(icon = Icon.BELL, classes = "h-6 w-6")</span>
    <span class="text-green-600 dark:text-green-400">@template.ashlar.components.icon(icon = Icon.BELL, classes = "h-6 w-6")</span>
    <span class="text-red-600 dark:text-red-400">@template.ashlar.components.icon(icon = Icon.BELL, classes = "h-6 w-6")</span>""";}
!{String labelCode = """
    @template.ashlar.components.icon(icon = Icon.CHECK_CIRCLE, classes = "h-6 w-6 text-green-600 dark:text-green-400", label = "Verified")""";}
!{List<ApiParam> iconApi = List.of(
    ApiParam.required("icon", "Icon", "Which icon to render - see the full list below."),
    ApiParam.optional("classes", "String", "\"h-5 w-5\"", "Tailwind size classes. Button and other composing components override this to match their own size."),
    ApiParam.optional("label", "String", "\"\"", "Only pass this when the icon conveys meaning on its own, with no adjacent visible text or aria-label. See Accessibility.")
);}
!{List<PageAnchor> sections = List.of(
    new PageAnchor("quick-start", "Quick Start"),
    new PageAnchor("how-to-use", "How to Use"),
    new PageAnchor("variants", "Variants & States"),
    new PageAnchor("composition", "Composition"),
    new PageAnchor("best-practices", "Best Practices"),
    new PageAnchor("accessibility", "Accessibility"),
    new PageAnchor("api-reference", "API Reference"),
    new PageAnchor("related", "Related Components")
);}

@template.layout.page(
contextPath = contextPath,
title = "Icon",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "icon", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">An inline &lt;svg&gt;, stroke-only and colored entirely via <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">currentColor</code> so it always matches the surrounding text. Every icon is bundled artwork, not a font or an external sprite request - there is nothing to load.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3">
                    @template.ashlar.components.icon(icon = Icon.SETTINGS)
                </div>
            `
            )
        </section>

        <section id="how-to-use" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "How to Use",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Most of the time you won't call Icon directly - you'll pass an <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">Icon</code> value into another component's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">icon</code> param (Button, Alert, Badge, Text Input, and 19 others all have one). Call this component directly only for a bare icon in your own prose or layout, outside any of those built-in slots - exactly what this page's own gallery below does.</p>
            `
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Sizing via classes</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">There is no size param - size is just the Tailwind height/width classes you pass.</p>
                        @template.layout.example(
                        code = sizingCode,
                        content = @`
                            <div class="mt-3 flex flex-wrap items-center gap-4">
                                @template.ashlar.components.icon(icon = Icon.STAR, classes = "h-3 w-3")
                                @template.ashlar.components.icon(icon = Icon.STAR, classes = "h-5 w-5")
                                @template.ashlar.components.icon(icon = Icon.STAR, classes = "h-8 w-8")
                                @template.ashlar.components.icon(icon = Icon.STAR, classes = "h-12 w-12")
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Color via currentColor</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">An icon has no color of its own - it always paints with the text color of whatever wraps it.</p>
                        @template.layout.example(
                        code = colorCode,
                        content = @`
                            <div class="mt-3 flex flex-wrap items-center gap-4">
                                <span class="text-slate-400">@template.ashlar.components.icon(icon = Icon.BELL, classes = "h-6 w-6")</span>
                                <span class="text-blue-600 dark:text-blue-400">@template.ashlar.components.icon(icon = Icon.BELL, classes = "h-6 w-6")</span>
                                <span class="text-green-600 dark:text-green-400">@template.ashlar.components.icon(icon = Icon.BELL, classes = "h-6 w-6")</span>
                                <span class="text-red-600 dark:text-red-400">@template.ashlar.components.icon(icon = Icon.BELL, classes = "h-6 w-6")</span>
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Standalone, meaningful icon: label</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Passing <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">label</code> switches it from decorative (<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-hidden</code>) to a named image (<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="img"</code> + <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-label</code>) - only do this when no visible text or surrounding aria-label already says the same thing.</p>
                        @template.layout.example(
                        code = labelCode,
                        content = @`
                            <div class="mt-3">
                                @template.ashlar.components.icon(icon = Icon.CHECK_CIRCLE, classes = "h-6 w-6 text-green-600 dark:text-green-400", label = "Verified")
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Full set (${Icon.values().length} icons)</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Rendered directly from the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">Icon</code> enum itself, in declaration order, so this list can't drift from what the library actually ships. No source disclosure here - the "source" is a loop over every enum value, not a call worth copying.</p>
                        <div class="mt-3 grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6">
                            @for (Icon value : Icon.values())
                                <div class="flex flex-col items-center gap-1 rounded-md border border-slate-200 p-3 text-center dark:border-slate-700">
                                    @template.ashlar.components.icon(icon = value, classes = "h-5 w-5 text-slate-600 dark:text-slate-300")
                                    <span class="font-mono text-[10px] leading-tight text-slate-500 dark:text-slate-400">${value.name()}</span>
                                </div>
                            @endfor
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
                <p class="text-sm text-slate-500 dark:text-slate-400">Icon is composed internally by 23 other library components - most visibly <a href="${contextPath}/components/button" class="text-blue-700 hover:underline dark:text-blue-400">Button</a>, where the icon's size is already paired to the button's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">size</code> so you never set <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">classes</code> yourself in that context.</p>
            `
            )
        </section>

        <section id="best-practices" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Best Practices",
            content = @`
                <div class="grid gap-6 sm:grid-cols-2">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Do</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Pair an icon with visible text wherever possible - an icon alone rarely identifies an action as reliably as a word does.</li>
                            <li>Use a component's own built-in <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">icon</code> param when one exists, rather than placing a bare Icon call next to it by hand.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Avoid</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Passing <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">label</code> on an icon that already sits next to visible text saying the same thing - that announces the meaning twice.</li>
                            <li>Picking an icon for decoration alone on a control a user must operate - pair it with text or an explicit <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">ariaLabel</code> on the control itself (see Button's Pitfalls).</li>
                        </ul>
                    </div>
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
                            <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-hidden="true"</code> by default - an icon says nothing to assistive tech unless you opt it in.</li>
                            <li>An exhaustive artwork switch - every <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">Icon</code> value is guaranteed to render something; there's no "missing icon" failure at runtime.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">label</code> only on the rare icon that must convey meaning entirely on its own.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.layout.api-reference(apiParams = iconApi)
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/button" class="text-blue-700 hover:underline dark:text-blue-400">Button</a> for the most common icon host &middot;
                    <a href="${contextPath}/components/badge" class="text-blue-700 hover:underline dark:text-blue-400">Badge</a> and
                    <a href="${contextPath}/components/alert" class="text-blue-700 hover:underline dark:text-blue-400">Alert</a> for icon-paired status indicators
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "icon", contextPath = contextPath)
    </div>
`
)
