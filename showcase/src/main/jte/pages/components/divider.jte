@import com.redspartanlabs.ashlar.icon.Icon

@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Divider",
content = @`
    @template.layout.component-nav(currentSlug = "divider", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Divider</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A visual separator - a real &lt;hr&gt; horizontally, <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="separator"</code> vertically. The surrounding layout owns all spacing; the divider only draws the line.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Horizontal, between content sections</h3>
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

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Vertical, inside a toolbar</h3>
                    <div class="mt-3 flex h-10 items-center gap-3 rounded-md border border-slate-200 bg-slate-50 px-3 dark:border-slate-700 dark:bg-slate-800/50">
                        @template.ashlar.components.button(text = "Save", type = "Ghost", size = "Small", icon = Icon.SAVE, iconOnly = true)
                        @template.ashlar.components.button(text = "Edit", type = "Ghost", size = "Small", icon = Icon.EDIT, iconOnly = true)
                        @template.ashlar.components.divider(orientation = "Vertical")
                        @template.ashlar.components.button(text = "Delete", type = "Ghost", size = "Small", icon = Icon.DELETE, iconOnly = true)
                    </div>
                </div>
            </div>
        </section>
`
)
