@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param String contextPath

<%-- Batch 3 documentation page. Existing "Basic block" / "Text" /
     "Avatar + text" / "Composed inside a Card" / "Different dimensions" /
     "Rounded shape, standalone" examples are kept verbatim, moved under
     Variants & States/Composition. Added: Quick Start, formal grammar
     headings, Accessibility split, API Reference, Related Components.

     DELIBERATE OMISSION: no Pitfalls section. This component is too
     simple to have a genuine one - its only real behavioral nuance (that
     it never announces a "loading" state itself; see Accessibility) is
     already a "you provide" responsibility, not a trap to fall into, so
     stating it twice under two headings would be the checklist-filling
     this batch's brief explicitly says not to do.

     Verified against source before writing:
     - skeleton.jte's own 3 @params and its own doc comment in full.
       Confirmed width/height go through an inline style attribute
       (not a Tailwind class) specifically because Tailwind's build-time
       scanner can't generate a class for an arbitrary runtime string -
       the same reason progress.jte's fill width does the same thing.
       Confirmed JTE's own ${} escaping is what keeps that attribute
       well-formed regardless of input, with no dedicated class/style
       param for a caller to inject anything else through.
     - Confirmed aria-hidden="true" is unconditional - a skeleton is
       never itself announced, and whether a group of them should expose
       a "loading" state to assistive tech is explicitly left to the
       surrounding composition (the same restraint Card's own role
       decisions use), not something this primitive guesses at.
     - SkeletonShape.java: only 2 shapes (Rectangle/Rounded) - confirmed
       by its own doc comment there is no dedicated avatar/circle value,
       since a Rounded skeleton with equal width and height already
       reads as a circle. Falls back silently to Rectangle on an
       unrecognized string.
     - Confirmed there is deliberately no CardSkeleton/AvatarSkeleton -
       composite loading layouts are assembled by the page from several
       of these, the same way a page assembles Badge or Divider, rather
       than the library shipping a layout-specific skeleton for every
       shape a caller might want. --%>
!{String quickStartCode = """
    @template.ashlar.components.skeleton(
        width = "100%",
        height = "1rem"
    )""";}
!{List<ApiParam> skeletonApi = List.of(
    ApiParam.optional("width", "String", "\"100%\"", "Any CSS width value. Rendered through an inline style attribute, HTML-escaped - not a Tailwind class."),
    ApiParam.optional("height", "String", "\"1rem\"", "Any CSS height value, same mechanism as width."),
    ApiParam.optional("shape", "String", "\"Rectangle\"", "Rectangle or Rounded. Unrecognized values silently fall back to Rectangle. There is no separate avatar/circle shape - see Variants & States.")
);}

@template.layout.page(
contextPath = contextPath,
title = "Skeleton",
content = @`
    @template.layout.component-nav(currentSlug = "skeleton", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Skeleton</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A single placeholder shape - no JavaScript, no CardSkeleton/AvatarSkeleton. Loading layouts are compositions of this one primitive, assembled the same way a page assembles Badge or Divider.</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3 max-w-sm">
                        @template.ashlar.components.skeleton(width = "100%", height = "1rem")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Basic block</p>
                            <div class="mt-3 max-w-sm">
                                @template.ashlar.components.skeleton(width = "100%", height = "6rem")
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Text</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Several lines with different widths, composed directly by the page.</p>
                            <div class="mt-3 flex max-w-sm flex-col gap-2">
                                @template.ashlar.components.skeleton(width = "100%", height = "0.875rem")
                                @template.ashlar.components.skeleton(width = "100%", height = "0.875rem")
                                @template.ashlar.components.skeleton(width = "60%", height = "0.875rem")
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Avatar + text</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A Rounded skeleton with equal width and height reads as an avatar - there is no separate avatar variant.</p>
                            <div class="mt-3 flex max-w-sm items-center gap-3">
                                @template.ashlar.components.skeleton(width = "3rem", height = "3rem", shape = "Rounded")
                                <div class="flex flex-1 flex-col gap-2">
                                    @template.ashlar.components.skeleton(width = "70%", height = "0.875rem")
                                    @template.ashlar.components.skeleton(width = "45%", height = "0.875rem")
                                </div>
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Different dimensions</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Width and height are the only knobs - any loading shape is built from these.</p>
                            <div class="mt-3 flex max-w-sm flex-wrap items-end gap-3">
                                @template.ashlar.components.skeleton(width = "2rem", height = "2rem")
                                @template.ashlar.components.skeleton(width = "4rem", height = "1.5rem")
                                @template.ashlar.components.skeleton(width = "8rem", height = "1rem")
                                @template.ashlar.components.skeleton(width = "5rem", height = "5rem")
                            </div>
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
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composition</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Skeleton is a neutral primitive, unaware of what wraps it - the example below composes a thumbnail-shaped block plus text lines inside an ordinary Card, exactly the way an application would assemble any real loading layout from several of these.</p>
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
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
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
                </div>

                <div>
                    @template.layout.api-reference(apiParams = skeletonApi)
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/spinner" class="text-blue-700 hover:underline dark:text-blue-400">Spinner</a> for a single indeterminate loading indicator instead of a content-shaped placeholder &middot;
                        <a href="${contextPath}/components/card" class="text-blue-700 hover:underline dark:text-blue-400">Card</a>, the usual surface a group of skeletons sits inside
                    </p>
                </div>
            </div>
        </section>
`
)
