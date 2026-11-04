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

     Width: this same item renders twice from one Navbar call - once
     inline in the desktop bar, once stacked in the mobile menu drawer - and
     a stacked link should fill the row so the whole width is a tap target,
     not just its own text, so `ash-nav-link` is full width. navbar.css sets
     a link back to content width in the desktop bar with a child selector,
     so this component itself stays free of any "which context am I in"
     knowledge. --%>
!{boolean isCurrent = current;}
!{boolean isLink = !isCurrent && href != null && !href.isEmpty();}
!{String ariaCurrentAttr = isCurrent ? "page" : null;}

!{String linkClasses = "ash-nav-link";}
!{String currentClasses = "ash-nav-link ash-nav-link--current";}

<li class="ash-nav-link__item">
    @if (isLink)
        <a href="${href}" data-nav-link class="${linkClasses}">${label}</a>
    @else
        <span aria-current="${ariaCurrentAttr}" data-nav-link class="${currentClasses}">${label}</span>
    @endif
</li>
