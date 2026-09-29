@param String label
@param String href = null

<%-- A real `<a href>` when the caller supplies one, or non-interactive text
     otherwise - the same "missing href means disabled, not a dead link"
     convention breadcrumb-item.jte and pagination-item.jte already
     established, applied here for the same reason: a `href="#"` fallback
     would be a link that goes nowhere and silently swallows the click,
     which is worse than admitting there's nothing to navigate to.

     `tabindex="-1"` on both, always: dropdown-menu.js's roving-tabindex
     code (see its own comment) is what puts exactly one *enabled* item into
     the Tab order - and only while the menu is actually open - so a closed
     menu's items are never independently reachable, and a disabled item is
     never reachable at all. This is a real primitive JTE can't express with
     an `@if` alone, since "which one" depends on runtime keyboard state,
     not on anything known at render time.

     `role="menuitem"` on the disabled span too, not omitted: the WAI-ARIA
     APG menu pattern expects every entry in a `role="menu"` container to be
     a menuitem, `aria-disabled` and all - dropping the role here would make
     this item a plain, unannounced text node instead of a menu entry a
     screen reader user is told exists but can't currently activate. --%>
!{boolean isLink = href != null && !href.isEmpty();}

@if (isLink)
    <a href="${href}" role="menuitem" tabindex="-1" data-dropdown-menu-item class="block px-3 py-2 text-sm text-slate-700 hover:bg-slate-50 focus:bg-slate-100 focus:outline-none dark:text-slate-200 dark:hover:bg-slate-700 dark:focus:bg-slate-700">${label}</a>
@else
    <span role="menuitem" aria-disabled="true" tabindex="-1" data-dropdown-menu-item class="block px-3 py-2 text-sm text-slate-300 dark:text-slate-600">${label}</span>
@endif
