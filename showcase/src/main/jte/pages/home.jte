@import java.util.List
@import com.redspartanlabs.ashlar.icon.Icon
@import com.redspartanlabs.ashlar.select.SelectOption
@import com.redspartanlabs.ashlar.showcase.datatable.SampleAccountData

@param String contextPath

<%-- The Ashlar front door. Renders through the shared layout/page.jte
     shell like every other page - same Navbar, Footer, theme/skin
     bootstrap - so the homepage is just another page the library happens
     to render, not a bespoke exception to it.

     Deliberately NOT a stat-grid-and-feature-card marketing page: no
     component-count dashboard, no card-wrapped "proof" panel, no grid of
     component-preview tiles, no pill-shaped theme picker. One real
     application composition (reusing SampleAccountData, the Data Table
     catalog page's own already-invented demo data, rather than a second
     fake dataset), one short statement of the design philosophy behind
     it (via key-value-list.jte, not a feature grid), and a plain index of
     the ten themes - then a single link into the catalog, which now lives
     at its own route (pages/components-index.jte) instead of being
     reproduced here. --%>
!{List<SelectOption> accountStatuses = List.of(
    SelectOption.of("active", "Active"),
    SelectOption.of("pending", "Pending"),
    SelectOption.of("inactive", "Inactive"),
    SelectOption.of("error", "Error")
);}
!{List<SelectOption> accountTypes = List.of(
    SelectOption.of("enterprise", "Enterprise"),
    SelectOption.of("government", "Government"),
    SelectOption.of("smb", "SMB"),
    SelectOption.of("non-profit", "Non-Profit")
);}

@template.layout.page(
contextPath = contextPath,
title = "Ashlar",
description = "Ashlar is a server-rendered UI component system for Java and Kotlin, built with JTE.",
componentsActive = false,
content = @`

    <%-- HERO. Typography and restraint, not a stat dashboard: one real
         <h1>, two sentences, two plain text links. No filled button.
         Positive technical identity (what Ashlar is) rather than
         framework-comparison copy (what it isn't) - Java and Kotlin are
         the languages/ecosystem, JTE is the template technology Ashlar is
         built with, not three equivalent platforms. --%>
    <section class="max-w-2xl py-10">
        <p class="text-xs font-semibold uppercase tracking-widest text-blue-600 dark:text-blue-400">Server-rendered UI, built with JTE</p>
        <h1 class="mt-3 text-5xl font-bold tracking-tight text-slate-900 dark:text-slate-100">Ashlar</h1>
        <p class="mt-6 text-xl leading-relaxed text-slate-700 dark:text-slate-300">A UI component system for Java and Kotlin, built with JTE. Real HTML, composed on the server, with one design system behind every component.</p>
        <p class="mt-4 text-base leading-relaxed text-slate-500 dark:text-slate-400">JTE templates compose the UI, Java or Kotlin supplies the data and behavior, and the browser receives the finished HTML.</p>

        <div class="mt-8 flex flex-wrap items-center gap-6">
            <a href="#composition" class="inline-flex items-center gap-1.5 text-sm font-semibold text-slate-900 underline-offset-4 hover:text-blue-700 hover:underline dark:text-slate-100 dark:hover:text-blue-400">
                See it in use
                @template.ashlar.components.icon(icon = Icon.ARROW_DOWN, classes = "h-3.5 w-3.5")
            </a>
            <a href="https://github.com/redspartanlabs/ashlar" class="inline-flex items-center gap-1.5 text-sm text-slate-500 underline-offset-4 hover:text-slate-800 hover:underline dark:text-slate-400 dark:hover:text-slate-200">
                View on GitHub
                @template.ashlar.components.icon(icon = Icon.EXTERNAL_LINK, classes = "h-3.5 w-3.5")
            </a>
        </div>
    </section>

    <%-- THE COMPOSITION. No enclosing Card - only the components' own
         surfaces (Stat Card's border, Data Table's border). Real data
         from SampleAccountData, the same demo data the Data Table
         catalog page already uses. Closes with a short, honest technical
         note and one real, representative call, instead of a separate
         "installation" section. --%>
    <section id="composition" class="scroll-mt-20">
    @template.ashlar.utilities.page-section(
    title = "Account Registry",
    description = "A composition, not a demo: Stat Cards, a Filter Bar, and a sortable, paginated Data Table, working the way they would in a real application.",
    content = @`
        <div class="flex flex-col gap-6">
            <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
                @template.ashlar.components.stat-card(
                label = "Total accounts",
                value = "1,248",
                change = @`@template.ashlar.components.badge(text = "+32 this month", variant = "Success")`
                )
                @template.ashlar.components.stat-card(
                label = "Active",
                value = "892",
                description = "71% of all accounts"
                )
                @template.ashlar.components.stat-card(
                label = "Needs attention",
                value = "41",
                change = @`@template.ashlar.components.badge(text = "Pending + Error", variant = "Warning")`
                )
            </div>

            <form method="get" class="flex flex-col gap-4">
                @template.ashlar.utilities.filter-bar(
                content = @`
                    @template.ashlar.components.search-input(name = "demoSearch", label = "Search accounts", placeholder = "Search by name or account ID...")
                    @template.ashlar.components.select(name = "demoStatus", label = "Status", options = accountStatuses, placeholder = "Any status", clearable = true)
                    @template.ashlar.components.select(name = "demoType", label = "Account type", options = accountTypes, placeholder = "Any type", clearable = true)
                    @template.ashlar.components.button(text = "Apply", type = "Primary", buttonType = "submit")
                `
                )
            </form>

            @template.ashlar.components.data-table(
            caption = "Customer accounts",
            columns = SampleAccountData.columns(),
            rows = SampleAccountData.rows(),
            rowKey = "id",
            searchable = false,
            selectable = true,
            pageSize = 5
            )

            <div class="border-t border-slate-200 pt-6 dark:border-slate-800">
                <p class="max-w-2xl text-sm text-slate-500 dark:text-slate-400">Everything above is a real <code class="rounded bg-slate-100 px-1 py-0.5 font-mono text-xs text-slate-700 dark:bg-slate-800 dark:text-slate-300">&#64;template.ashlar...</code> call, rendered to HTML by JTE before it reaches a browser. The response is the interface.</p>
                <pre class="mt-3 overflow-x-auto rounded bg-slate-900 p-3 text-xs text-slate-100 dark:bg-slate-950"><code>&#64;template.ashlar.components.data-table(
caption = "Customer accounts",
columns = SampleAccountData.columns(),
rows = SampleAccountData.rows(),
selectable = true
)</code></pre>
            </div>
        </div>
    `
    )
    </section>

    <%-- PHILOSOPHY. A short paragraph (the 61/9 figures appear once, in
         passing) and a key-value-list.jte of principles - a spec sheet,
         not a feature-card grid. --%>
    @template.ashlar.utilities.page-section(
    title = "A system, not a handful of primitives",
    description = "Sixty-one components and nine layout utilities share one set of design tokens, one accessibility contract, and the same decisions about density and hierarchy. A new component inherits that system rather than inventing its own corner of it.",
    content = @`
        @template.ashlar.utilities.key-value-list(
        content = @`
            @template.ashlar.utilities.key-value(label = "Deliberate", value = @`Every default has a reason behind it, not a framework convention standing in for one.`)
            @template.ashlar.utilities.key-value(label = "Useful density", value = @`Interfaces are built to hold real data, not padded out to look finished before it arrives.`)
            @template.ashlar.utilities.key-value(label = "Composition", value = @`Components are built to combine - a Stat Card's change is a Badge; a Data Table's cell is whatever the column needs.`)
            @template.ashlar.utilities.key-value(label = "Earned complexity", value = @`An option exists because an application needed it, not because it might someday.`)
            @template.ashlar.utilities.key-value(label = "Accessibility as structure", value = @`Semantics come from real HTML - a Stat Card is a <code class="rounded bg-slate-100 px-1 py-0.5 font-mono text-xs dark:bg-slate-800">&lt;dl&gt;</code>, a Page Section is a landmark by heading alone.`)
        `
        )
    `
    )

    <%-- THEMES. Tokens are :root-scoped, so two skins can't render
         side-by-side on one page - the honest demonstration is a plain
         index of links into the existing, unmodified ?skin= bootstrap,
         not a row of pill-shaped swatches. Picking one restyles every
         component on this page at once, including the table above. --%>
    <section id="themes" class="scroll-mt-20">
    @template.ashlar.utilities.page-section(
    title = "One system. Ten identities.",
    description = "Every component on this page is styled from shared design tokens, not hardcoded colors. Choosing a skin doesn't swap a palette - it restyles everything at once, including the table you just read.",
    content = @`
        <div class="grid grid-cols-1 divide-y divide-slate-200 sm:grid-cols-2 sm:gap-x-12 sm:divide-y-0 dark:divide-slate-800">
            <div class="divide-y divide-slate-200 dark:divide-slate-800">
                @template.layout.home-theme-row(contextPath = contextPath, index = "01", skin = "axiom", tagline = "analytical")
                @template.layout.home-theme-row(contextPath = contextPath, index = "02", skin = "civic", tagline = "institutional")
                @template.layout.home-theme-row(contextPath = contextPath, index = "03", skin = "fathom", tagline = "atmospheric")
                @template.layout.home-theme-row(contextPath = contextPath, index = "04", skin = "foundry", tagline = "machined")
                @template.layout.home-theme-row(contextPath = contextPath, index = "05", skin = "harbor", tagline = "calm")
            </div>
            <div class="divide-y divide-slate-200 dark:divide-slate-800">
                @template.layout.home-theme-row(contextPath = contextPath, index = "06", skin = "keystone", tagline = "utilitarian")
                @template.layout.home-theme-row(contextPath = contextPath, index = "07", skin = "ledger", tagline = "ruled")
                @template.layout.home-theme-row(contextPath = contextPath, index = "08", skin = "sterling", tagline = "understated")
                @template.layout.home-theme-row(contextPath = contextPath, index = "09", skin = "summit", tagline = "structured")
                @template.layout.home-theme-row(contextPath = contextPath, index = "10", skin = "veridian", tagline = "comfortable")
            </div>
        </div>
        <p class="mt-6 text-sm text-slate-500 dark:text-slate-400">Ashlar's own default styling - what you're most likely seeing now - needs no skin at all. A choice above persists in this browser until you pick another.</p>
    `
    )
    </section>

    <%-- CLOSE. One paragraph, one link - the catalog itself lives at
         /components now, so it isn't reproduced here. --%>
    <section class="border-t border-slate-200 pt-10 dark:border-slate-800">
        <p class="max-w-2xl text-sm text-slate-600 dark:text-slate-400">Sixty-one components and nine layout utilities, each documented with its full API and every variant shown in place.</p>
        <a href="${contextPath}/components" class="mt-3 inline-flex items-center gap-1.5 text-sm font-semibold text-slate-900 underline-offset-4 hover:text-blue-700 hover:underline dark:text-slate-100 dark:hover:text-blue-400">
            Browse the catalog
            @template.ashlar.components.icon(icon = Icon.ARROW_RIGHT, classes = "h-3.5 w-3.5")
        </a>
    </section>
`
)
