@import dev.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import dev.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

<%-- Batch 11 documentation page. Existing "Default" / "Multiple instances
     stay in sync" / "In a Navbar's actions slot" examples kept verbatim.
     Added: Quick Start, formal grammar headings, How to Use, Composition,
     Pitfalls (justified by the pre-paint/persistence behavior verified
     below), Accessibility, API Reference, Related Components.

     Verified against source before writing:
     - theme-toggle.jte's own single @param (id) and doc comment.
       CONFIRMED the button is always server-rendered AS IF light mode
       (Sun icon visible, "Light" label, aria-pressed="false") since the
       server has no way to know the client's stored preference - theme.js
       corrects every toggle on the page to the real state immediately at
       DOMContentLoaded, using the data-theme attribute the page's own
       inline head script already set before first paint.
     - theme.js in full. CONFIRMED theme state is genuinely global and
       document-wide, not per-instance like every other component in
       this library - there is no per-root init function; every
       [data-theme-toggle] button on the page reflects and controls the
       exact same shared state, confirmed directly by its own top
       comment. Clicking any one toggle updates all of them via a single
       document-wide query, demonstrated in the existing "Multiple
       instances stay in sync" example.
     - CONFIRMED A LIVE OS-PREFERENCE LISTENER EXISTS, but only applies
       conditionally: `matchMedia("(prefers-color-scheme: dark)")`'s own
       change event only calls applyTheme() when getStoredTheme() is
       null - i.e. only for a visitor who has never explicitly clicked a
       toggle. The first explicit click permanently switches that
       browser from "follows the OS" to "remembers its own choice" via
       localStorage, for good, on that browser - a real, non-obvious
       behavioral fact, not previously documented.
     - CONFIRMED localStorage access failures (private browsing,
       sandboxed iframes, disabled storage) are caught and degrade to
       treating nothing as stored, on both read and write - the page
       never breaks, it just can't remember the choice.
     - THEME VS. SKIN - verified directly in layout/page.jte's own
       bootstrap script, not assumed to be the same mechanism: these are
       two entirely independent attributes on <html>, with separate
       validation lists, separate localStorage keys ("theme" vs "skin"),
       and different defaults. Theme always resolves to something
       (light or dark, falling back to the OS preference and then to
       light); an unset or invalid skin leaves data-skin absent
       entirely, which is what keeps every skin's CSS inert - there is
       no "default skin" the way there's a default theme. Confirmed
       theme-toggle.jte's own doc comment states explicitly that
       theme.js does NOT repeat the pre-paint bootstrap logic - by
       design, this component only ever reflects and changes state that
       bootstrap already established; initializing it is the
       application layout's responsibility, not this component's. --%>
!{String quickStartCode = """
    @template.ashlar.components.theme-toggle()""";}
!{List<ApiParam> themeToggleApi = List.of(
    ApiParam.optional("id", "String", "\"\"", "Only needed if you must target this specific instance from your own code - theme.js already finds every toggle on the page on its own, so most callers never need this.")
);}
!{List<PageAnchor> sections = List.of(
    new PageAnchor("quick-start", "Quick Start"),
    new PageAnchor("how-to-use", "How to Use"),
    new PageAnchor("variants", "Variants & States"),
    new PageAnchor("composition", "Composition"),
    new PageAnchor("pitfalls", "Pitfalls & Troubleshooting"),
    new PageAnchor("accessibility", "Accessibility"),
    new PageAnchor("api-reference", "API Reference"),
    new PageAnchor("related", "Related Components")
);}

@template.layout.page(
contextPath = contextPath,
title = "Theme Toggle",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "theme-toggle", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Switches between light and dark mode. Theme is global, document-wide state rather than something this one instance owns - every toggle on the page reflects and controls the same <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">data-theme</code> attribute, so clicking any of the ones below changes them all at once, including the one in this page's own header.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3">
                    @template.ashlar.components.theme-toggle()
                </div>
            `
            )
        </section>

        <section id="how-to-use" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "How to Use",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Place one Theme Toggle somewhere persistently visible - a Navbar's <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">actions</code> slot is this component's documented most common home, since that area never collapses on narrow screens. This component only ever reflects and changes existing theme state; it does not initialize it. Your own page layout needs the pre-paint bootstrap script shown in Composition below - without it, the page would render once in the wrong theme and then flip, which is the exact flash this component's own design is built to avoid.</p>
            `
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Default</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">No parameters required. The icon, label, and <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-pressed</code> state are all set correctly before this page's own JavaScript runs, from the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">data-theme</code> the inline head script already applied pre-paint.</p>
                        <div class="mt-3">
                            @template.ashlar.components.theme-toggle()
                        </div>
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Multiple instances stay in sync</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Unlike most components in this library, instances are not independent - there is one theme for the whole document. Toggle either of these and both update together.</p>
                        <div class="mt-3 flex flex-wrap items-center gap-3">
                            @template.ashlar.components.theme-toggle(id = "themeToggleShowcaseA")
                            @template.ashlar.components.theme-toggle(id = "themeToggleShowcaseB")
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
                <p class="text-sm text-slate-500 dark:text-slate-400">Its most common home - composed into <a href="${contextPath}/components/navbar" class="text-blue-700 hover:underline dark:text-blue-400">Navbar</a>'s trailing <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">actions</code> area, where it stays visible even when the link list collapses on a narrow screen (Navbar deliberately never duplicates <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">actions</code> into its mobile drawer, specifically so there's never a second, possibly-disagreeing copy of this toggle on the page).</p>
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
                <p class="mt-3 text-sm text-slate-500 dark:text-slate-400">This component depends on - but does not itself provide - the page's own pre-paint bootstrap, which sets <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">data-theme</code> on &lt;html&gt; from localStorage (falling back to <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">prefers-color-scheme</code>, then to light) before the stylesheet can paint anything. That bootstrap is application-layout responsibility, not something Theme Toggle ships - see this showcase's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">layout/page.jte</code> for the exact inline script.</p>
            `
            )
        </section>

        <section id="pitfalls" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Pitfalls & Troubleshooting",
            content = @`
                <ul class="list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">Without the page's own pre-paint bootstrap, this always renders light first and flashes to the real theme.</strong> The server has no way to know the client's stored preference, so the button (and the rest of the page, if nothing else sets <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">data-theme</code> early) renders light by default and only corrects once this component's own script runs.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">The live OS-preference listener stops applying after the first explicit click.</strong> Before any explicit choice, changing your OS's light/dark setting updates the page live. The moment someone clicks a toggle, that browser remembers the explicit choice in localStorage and the OS-preference listener is permanently skipped for it from then on - this is by design, not a bug, but easy to misread as "the live-update stopped working."</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">Theme and skin are two separate, independent attributes.</strong> This component only ever reads/writes <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">data-theme</code> (light/dark) - it has no relationship to <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">data-skin</code> (the ten ported visual themes), which is bootstrapped separately, has no default value at all, and is not something Theme Toggle can switch.</li>
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
                            <li>A real native &lt;button&gt;, so Enter/Space and focus are the browser's own - no custom keyboard handling.</li>
                            <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-pressed</code> reflecting dark/light state, kept correct across every instance on the page at once.</li>
                            <li>An accessible name from the button's own visible "Light"/"Dark" label text - no separate aria-label needed.</li>
                            <li>Graceful degradation when localStorage is unavailable (private browsing, a sandboxed iframe) - the toggle still works for the current page view, it just can't remember the choice.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>The pre-paint bootstrap script itself - see Composition and Pitfalls.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "API Reference",
            content = @`
                @template.layout.api-reference(apiParams = themeToggleApi)
            `
            )
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/navbar" class="text-blue-700 hover:underline dark:text-blue-400">Navbar</a>, this component's most common home
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "theme-toggle", contextPath = contextPath)
    </div>
`
)
