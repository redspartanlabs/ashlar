@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Skeleton",
content = @`
    @template.layout.component-nav(currentSlug = "skeleton", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Skeleton</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A single placeholder shape - no JavaScript, no CardSkeleton/AvatarSkeleton. Loading layouts are compositions of this one primitive, assembled the same way a page assembles Badge or Divider.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic block</h3>
                    <div class="mt-3 max-w-sm">
                        @template.ashlar.components.skeleton(width = "100%", height = "6rem")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Text</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Several lines with different widths, composed directly by the page.</p>
                    <div class="mt-3 flex max-w-sm flex-col gap-2">
                        @template.ashlar.components.skeleton(width = "100%", height = "0.875rem")
                        @template.ashlar.components.skeleton(width = "100%", height = "0.875rem")
                        @template.ashlar.components.skeleton(width = "60%", height = "0.875rem")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Avatar + text</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A ROUNDED skeleton with equal width and height reads as an avatar - there is no separate avatar variant.</p>
                    <div class="mt-3 flex max-w-sm items-center gap-3">
                        @template.ashlar.components.skeleton(width = "3rem", height = "3rem", shape = "Rounded")
                        <div class="flex flex-1 flex-col gap-2">
                            @template.ashlar.components.skeleton(width = "70%", height = "0.875rem")
                            @template.ashlar.components.skeleton(width = "45%", height = "0.875rem")
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composed inside a Card</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A thumbnail-shaped block plus text lines, composed inside an ordinary Card - Skeleton is a neutral primitive, unaware it's inside a Card.</p>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Different dimensions</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Width and height are the only knobs - any loading shape is built from these.</p>
                    <div class="mt-3 flex max-w-sm flex-wrap items-end gap-3">
                        @template.ashlar.components.skeleton(width = "2rem", height = "2rem")
                        @template.ashlar.components.skeleton(width = "4rem", height = "1.5rem")
                        @template.ashlar.components.skeleton(width = "8rem", height = "1rem")
                        @template.ashlar.components.skeleton(width = "5rem", height = "5rem")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Rounded shape, standalone</h3>
                    <div class="mt-3 flex items-center gap-3">
                        @template.ashlar.components.skeleton(width = "2.5rem", height = "2.5rem", shape = "Rounded")
                        @template.ashlar.components.skeleton(width = "3.5rem", height = "3.5rem", shape = "Rounded")
                        @template.ashlar.components.skeleton(width = "8rem", height = "2rem", shape = "Rounded")
                    </div>
                </div>
            </div>
        </section>
`
)
