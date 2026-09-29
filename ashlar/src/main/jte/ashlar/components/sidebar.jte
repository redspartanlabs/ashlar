@param gg.jte.Content items
@param String ariaLabel = "Sidebar"

<%-- A vertical list of destinations - the wrapper only establishes the `<nav>`
     landmark and the list; each entry is a separate
     @template.ashlar.components.sidebar-item(...) call composed into `items`, the
     same wrapper/item split as breadcrumbs.jte and pagination.jte.

     No surface of its own - no border, background, or width - the identical
     policy faceted-search.jte states for the identical reason: a sidebar
     sits inside a fixed-width column on one page, inside a Card on another,
     and inside a Drawer on a phone, and giving it a permanent panel would
     fight all three. A page that wants a persistent two-column layout
     supplies its own column (the same `grid lg:grid-cols-[15rem_1fr]`
     faceted-search.jte's own showcase demonstrates); a page that wants it
     off-canvas on narrow screens puts this same `items` content inside the
     library's existing Drawer - no second collapse mechanism invented here.
     navbar.jte's built-in mobile drawer is not that same mechanism reused:
     a top navbar collapsing below a breakpoint is close to universal, where
     a sidebar's relationship to the page varies too much from application
     to application to bake in one answer.

     `ariaLabel` defaults to "Sidebar" rather than being hardcoded, unlike
     breadcrumbs.jte's/pagination.jte's own fixed strings - those follow a
     WAI-ARIA APG name for one specific pattern, and a page's side
     navigation has no one universally correct name (a docs site might call
     this "Sections", a dashboard "Workspace"). It exists mainly so a page
     that also has a Navbar can give the two `<nav>` landmarks distinct
     names. --%>
<nav aria-label="${ariaLabel}" data-sidebar>
    <ul class="flex flex-col gap-1">
        ${items}
    </ul>
</nav>
