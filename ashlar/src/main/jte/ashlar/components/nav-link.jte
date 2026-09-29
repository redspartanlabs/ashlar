@param String label
@param String href = null
@param boolean current = false

<%-- One entry in a Navbar's link list: a real <a href> for anywhere the
     caller can actually navigate to, or non-interactive text for the page
     the user is already on - the same "current wins over href" precedence
     breadcrumb-item.jte establishes, and for the identical reason: the
     current page can't also be a link to itself without confusing the one
     signal (`aria-current="page"`) assistive tech relies on to say where the
     user is.

     `data-nav-link` marks both branches, not for any script (there is none -
     this is a plain link, like breadcrumb-item.jte and pagination-item.jte)
     but so a theme whose masthead departs from the page's normal background
     - only Civic does this - has a stable hook to re-point this link's ink
     against. See theme-civic.css's own masthead section for the other
     controls (`[data-dropdown-menu-trigger]`, `[data-theme-toggle]`) that
     already needed exactly this.

     `w-full`: this same item renders twice from one Navbar call - once
     inline in the desktop bar, once stacked in the mobile menu drawer - and
     a stacked link should fill the row so the whole width is a tap target,
     not just its own text. navbar.jte's desktop list overrides this back to
     content width with a child-selector utility (the same technique
     filter-bar.jte already uses), so this component itself stays free of
     any "which context am I in" knowledge. --%>
!{boolean isCurrent = current;}
!{boolean isLink = !isCurrent && href != null && !href.isEmpty();}
!{String ariaCurrentAttr = isCurrent ? "page" : null;}

!{String linkClasses = "flex w-full items-center rounded-md px-3 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 hover:text-slate-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-1 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-slate-100";}
!{String currentClasses = "flex w-full items-center rounded-md bg-blue-50 px-3 py-2 text-sm font-semibold text-blue-700 dark:bg-blue-950/50 dark:text-blue-400";}

<li>
    @if (isLink)
        <a href="${href}" data-nav-link class="${linkClasses}">${label}</a>
    @else
        <span aria-current="${ariaCurrentAttr}" data-nav-link class="${currentClasses}">${label}</span>
    @endif
</li>
