@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Theme Toggle",
content = @`
    @template.layout.component-nav(currentSlug = "theme-toggle", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Theme Toggle</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Switches between light and dark mode. Theme is global, document-wide state rather than something this one instance owns - every toggle on the page reflects and controls the same <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">data-theme</code> attribute, so clicking any of the ones below changes them all at once, including the one in this page's own header.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Default</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">No parameters required. The icon, label, and <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-pressed</code> state are all set correctly before this page's own JavaScript runs, from the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">data-theme</code> the inline head script already applied pre-paint.</p>
                    <div class="mt-3">
                        @template.ashlar.components.theme-toggle()
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Multiple instances stay in sync</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Unlike most components in this library, instances are not independent - there is one theme for the whole document. Toggle either of these and both update together.</p>
                    <div class="mt-3 flex flex-wrap items-center gap-3">
                        @template.ashlar.components.theme-toggle(id = "themeToggleShowcaseA")
                        @template.ashlar.components.theme-toggle(id = "themeToggleShowcaseB")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">In a Navbar's actions slot</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Its most common home - composed into Navbar's trailing area, where it stays visible even when the link list collapses on a narrow screen. See the Navbar page for more.</p>
                    <div class="mt-3 overflow-hidden rounded-lg border border-dashed border-slate-200 dark:border-slate-700">
                        @template.ashlar.components.navbar(
                        brand = @`<a href="#" class="text-lg font-semibold text-slate-900 dark:text-slate-100">Acme Console</a>`,
                        items = @`
                            @template.ashlar.components.nav-link(label = "Dashboard", href = "#", current = true)
                            @template.ashlar.components.nav-link(label = "Settings", href = "#")
                        `,
                        actions = @`@template.ashlar.components.theme-toggle(id = "themeToggleShowcaseNavbar")`
                        )
                    </div>
                </div>
            </div>
        </section>
`
)
