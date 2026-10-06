@import com.redspartanlabs.ashlar.showcase.catalog.ComponentCatalog
@import com.redspartanlabs.ashlar.showcase.catalog.ComponentPage

@param String contextPath

<%-- The catalog index: every migrated component and utility, grouped the
     way ComponentCatalog itself groups them - split out of the homepage
     (pages/home.jte) so `/` can introduce Ashlar and `/components` can be
     the thing it points visitors at. Same data `pages/home.jte` used to
     render inline, now on its own route with a real page-level `<h1>`
     via page-header.jte. --%>
@template.layout.page(
contextPath = contextPath,
title = "Components",
description = "Every migrated Ashlar component and layout utility, grouped by category - forms, feedback, content, navigation, and utilities.",
componentsActive = true,
content = @`
    @template.ashlar.utilities.page-header(
    title = "Components",
    description = "Sixty-one components and nine layout utilities, each documented with its full API and every variant shown in place."
    )

    <div class="grid gap-x-8 gap-y-10 sm:grid-cols-2 lg:grid-cols-3">
        @for (var entry : ComponentCatalog.byCategory().entrySet())
            <div>
                <h2 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">${entry.getKey()}</h2>
                <ul class="mt-3 flex flex-col gap-2">
                    @for (ComponentPage page : entry.getValue())
                        <li>
                            <a href="${contextPath}/components/${page.slug()}" class="text-sm text-slate-700 hover:text-blue-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 dark:text-slate-300 dark:hover:text-blue-400">${page.name()}</a>
                        </li>
                    @endfor
                </ul>
            </div>
        @endfor
    </div>
`
)
