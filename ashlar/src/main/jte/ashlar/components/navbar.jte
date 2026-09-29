@import com.redspartanlabs.ashlar.navbar.NavbarIds
@import com.redspartanlabs.ashlar.icon.Icon

@param gg.jte.Content brand
@param gg.jte.Content items
@param gg.jte.Content actions = null
@param String ariaLabel = "Primary"
@param String menuTitle = "Menu"

<%-- The page's own masthead: a brand, a set of destinations, and a trailing
     area for whatever controls the application wants always visible (a
     theme toggle, a search field, an account menu). This is the ONE place
     in the library that owns responsive collapse for itself rather than
     leaving it to the caller - unlike faceted-search.jte, which explicitly
     declines to own a Drawer because a facet list is used in genuinely
     different contexts from page to page, a top navbar collapsing into an
     off-canvas menu below a breakpoint is close to universal, and building
     it once, correctly, is worth more than leaving it as a recipe every
     consuming application re-derives.

     STRUCTURE. `<header> > <div> > ${brand}` is not incidental: several
     theme stylesheets (theme-civic.css's masthead section, most visibly)
     key their site-header styling off exactly this shape -
     `body > header > div > a:first-child` for the brand, `body > header`
     for the bar itself. `brand` has to render as a real `<a>` and has to be
     the first thing this component writes inside that div, or a caller's
     theme choice silently stops reaching the header. The second, inner div
     is new (the old hand-written header in layout/page.jte had it too, for
     the same `justify-between` reason) and holds everything this component
     added: the desktop link list, `actions`, and the mobile trigger.

     ONE `items`, RENDERED TWICE. `${items}` is written once into the
     desktop `<ul>` and once more into the mobile drawer's `<ul>` - `gg.jte.Content`
     is just a `writeTo(output)` method with no internal cursor, so calling
     it twice is exactly as safe as calling it once, and it spares a caller
     from ever composing their nav-link.jte list a second time. `actions` is
     NOT duplicated: it stays in the bar at every width rather than moving
     into the drawer, because the things applications put there (a theme
     toggle, a compact icon button) are exactly the controls that don't
     need to collapse, and duplicating them would risk two live copies of
     the same toggle disagreeing about focus. A caller who genuinely needs
     an action inside the mobile menu can still put it in `items`.

     THE DRAWER IS BORROWED, NOT REBUILT. The mobile menu is a real
     @template.ashlar.components.drawer(...) call, placement Left - the same
     focus-trap, Escape-to-close, and scroll-lock every other Drawer in this
     library gets, for free, because it's the same component. `menuTitle`
     exists only because Drawer's own `title` is required (a dialog must
     have an accessible name); "Menu" is a sensible enough default that most
     callers will never pass this.

     `ariaLabel` distinguishes this landmark from any other `<nav>` on the
     page - Sidebar's own default is "Sidebar" for the same reason - unlike
     breadcrumbs.jte/pagination.jte, which hardcode their aria-label because
     the WAI-ARIA APG names those specific patterns, a top navbar has no one
     universally correct name, so this stays a parameter. The mobile copy of
     `items` reuses the same label: it's the same navigation, only its
     presentation changes with viewport width. --%>
!{String drawerId = NavbarIds.next();}

<header data-navbar class="border-b border-slate-200 bg-white dark:border-slate-700 dark:bg-slate-900">
    <div class="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-4">
        ${brand}

        <div class="flex items-center gap-3">
            <nav aria-label="${ariaLabel}" class="hidden md:block">
                <ul class="flex items-center gap-1 text-sm md:[&>li>a]:w-auto">
                    ${items}
                </ul>
            </nav>

            @if (actions != null)
                <div class="flex items-center gap-3">
                    ${actions}
                </div>
            @endif

            <div class="md:hidden">
                @template.ashlar.components.button(
                iconOnly = true,
                icon = Icon.MENU,
                ariaLabel = "Open menu",
                type = "Ghost",
                attributes = "data-drawer-open=\"" + drawerId + "\" data-navbar-toggle"
                )
            </div>
        </div>
    </div>
</header>

@template.ashlar.components.drawer(
id = drawerId,
title = menuTitle,
placement = "Left",
body = @`
    <nav aria-label="${ariaLabel}">
        <ul class="flex flex-col gap-1">
            ${items}
        </ul>
    </nav>
`
)
