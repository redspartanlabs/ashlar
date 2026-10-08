@import dev.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import dev.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

<%-- Deliberate omission: no Pitfalls section. This component is too
     simple for a genuine one - its only real behavioral nuance (it
     never announces a "loading" state itself) is already a "you
     provide" responsibility stated once in Accessibility, not a trap
     worth restating under a second heading. --%>
!{String quickStartCode = """
    @template.ashlar.components.skeleton(
        width = "100%",
        height = "1rem"
    )""";}
!{String textCode = """
    @template.ashlar.components.skeleton(width = "100%", height = "0.875rem")
    @template.ashlar.components.skeleton(width = "100%", height = "0.875rem")
    @template.ashlar.components.skeleton(width = "60%", height = "0.875rem")""";}
!{String avatarTextCode = """
    @template.ashlar.components.skeleton(width = "3rem", height = "3rem", shape = "Rounded")
    <div class="flex flex-col gap-2">
        @template.ashlar.components.skeleton(width = "70%", height = "0.875rem")
        @template.ashlar.components.skeleton(width = "45%", height = "0.875rem")
    </div>""";}
!{String differentDimensionsCode = """
    @template.ashlar.components.skeleton(width = "2rem", height = "2rem")
    @template.ashlar.components.skeleton(width = "4rem", height = "1.5rem")
    @template.ashlar.components.skeleton(width = "8rem", height = "1rem")
    @template.ashlar.components.skeleton(width = "5rem", height = "5rem")""";}
!{List<ApiParam> skeletonApi = List.of(
    ApiParam.optional("width", "String", "\"100%\"", "Any CSS width value. Rendered through an inline style attribute, HTML-escaped - not a Tailwind class."),
    ApiParam.optional("height", "String", "\"1rem\"", "Any CSS height value, same mechanism as width."),
    ApiParam.optional("shape", "String", "\"Rectangle\"", "Rectangle or Rounded. Unrecognized values silently fall back to Rectangle. There is no separate avatar/circle shape - see Variants & States.")
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
title = "Skeleton",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "skeleton", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A single placeholder shape - no JavaScript, no CardSkeleton/AvatarSkeleton. Loading layouts are compositions of this one primitive, assembled the same way a page assembles Badge or Divider.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3 max-w-sm">
                    @template.ashlar.components.skeleton(width = "100%", height = "1rem")
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
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Basic block</p>
                        <div class="mt-3 max-w-sm">
                            @template.ashlar.components.skeleton(width = "100%", height = "6rem")
                        </div>
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Text</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Several lines with different widths, composed directly by the page.</p>
                        @template.layout.example(
                        code = textCode,
                        content = @`
                            <div class="mt-3 flex max-w-sm flex-col gap-2">
                                @template.ashlar.components.skeleton(width = "100%", height = "0.875rem")
                                @template.ashlar.components.skeleton(width = "100%", height = "0.875rem")
                                @template.ashlar.components.skeleton(width = "60%", height = "0.875rem")
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Avatar + text</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A Rounded skeleton with equal width and height reads as an avatar - there is no separate avatar variant.</p>
                        @template.layout.example(
                        code = avatarTextCode,
                        content = @`
                            <div class="mt-3 flex max-w-sm items-center gap-3">
                                @template.ashlar.components.skeleton(width = "3rem", height = "3rem", shape = "Rounded")
                                <div class="flex flex-1 flex-col gap-2">
                                    @template.ashlar.components.skeleton(width = "70%", height = "0.875rem")
                                    @template.ashlar.components.skeleton(width = "45%", height = "0.875rem")
                                </div>
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Different dimensions</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Width and height are the only knobs - any loading shape is built from these.</p>
                        @template.layout.example(
                        code = differentDimensionsCode,
                        content = @`
                            <div class="mt-3 flex max-w-sm flex-wrap items-end gap-3">
                                @template.ashlar.components.skeleton(width = "2rem", height = "2rem")
                                @template.ashlar.components.skeleton(width = "4rem", height = "1.5rem")
                                @template.ashlar.components.skeleton(width = "8rem", height = "1rem")
                                @template.ashlar.components.skeleton(width = "5rem", height = "5rem")
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Rounded shape, standalone</p>
                        <div class="mt-3 flex items-center gap-3">
                            @template.ashlar.components.skeleton(width = "2.5rem", height = "2.5rem", shape = "Rounded")
                            @template.ashlar.components.skeleton(width = "3.5rem", height = "3.5rem", shape = "Rounded")
                            @template.ashlar.components.skeleton(width = "8rem", height = "2rem", shape = "Rounded")
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
                <p class="text-sm text-slate-500 dark:text-slate-400">Skeleton is a neutral primitive, unaware of what wraps it - the example below composes a thumbnail-shaped block plus text lines inside an ordinary Card, exactly the way an application would assemble any real loading layout from several of these.</p>
                <div class="mt-3 max-w-sm">
                    @template.ashlar.components.card(
                    body = @`
                        <div class="flex flex-col gap-3">
                            @template.ashlar.components.skeleton(width = "100%", height = "8rem")
                            @template.ashlar.components.skeleton(width = "100%", height = "0.875rem")
                            @template.ashlar.components.skeleton(width = "80%", height = "0.875rem")
                            @template.ashlar.components.skeleton(width = "40%", height = "0.875rem")
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
                            <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-hidden="true"</code> unconditionally - a skeleton is never itself announced to assistive tech.</li>
                            <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">motion-reduce:animate-none</code>, freezing the pulse for anyone who asked for less motion.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Any announcement that a loading state is in progress - a group of skeletons has no built-in <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="status"</code> or <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-busy</code> of its own, since only the surrounding composition knows whether that's appropriate here.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.layout.api-reference(apiParams = skeletonApi)
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/spinner" class="text-blue-700 hover:underline dark:text-blue-400">Spinner</a> for a single indeterminate loading indicator instead of a content-shaped placeholder &middot;
                    <a href="${contextPath}/components/card" class="text-blue-700 hover:underline dark:text-blue-400">Card</a>, the usual surface a group of skeletons sits inside
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "skeleton", contextPath = contextPath)
    </div>
`
)
