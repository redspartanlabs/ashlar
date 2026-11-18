@param String label
@param String href = null

<%-- A real `<a href>` when the caller supplies one, or non-interactive text
     otherwise - the same "missing href means disabled, not a dead link"
     convention breadcrumb-item.jte and pagination-item.jte already
     established, applied here for the same reason: a `href="#"` fallback
     would be a link that goes nowhere and silently swallows the click,
     which is worse than admitting there's nothing to navigate to.

     `tabindex="-1"` on both, always: no item is ever in the page's Tab
     order. dropdown-menu.js moves real focus between the enabled items with
     the arrow keys, Home and End while the menu is open (see its own
     comment), and Tab from anywhere in the menu leaves it and closes it, so
     a closed menu's items are never independently reachable, and a disabled
     item is never reached by the keyboard at all.

     `role="menuitem"` on the disabled span too, not omitted: the WAI-ARIA
     APG menu pattern expects every entry in a `role="menu"` container to be
     a menuitem, `aria-disabled` and all - dropping the role here would make
     this item a plain, unannounced text node instead of a menu entry a
     screen reader user is told exists but can't currently activate.

     Styled by dropdown-menu-item.css: ash-dropdown-menu-item, and
     ash-dropdown-menu-item--disabled for the text without an href. --%>
!{boolean isLink = href != null && !href.isEmpty();}

@if (isLink)
    <a href="${href}" role="menuitem" tabindex="-1" data-dropdown-menu-item class="ash-dropdown-menu-item">${label}</a>
@else
    <span role="menuitem" aria-disabled="true" tabindex="-1" data-dropdown-menu-item class="ash-dropdown-menu-item ash-dropdown-menu-item--disabled">${label}</span>
@endif
