@param String label
@param String href = null
@param boolean current = false

<%-- One crumb: a plain <a href> for anywhere the user can actually navigate
     to, or plain text for the page they're already on - no button, no click
     handler, no JS. `current=true` wins over `href` even when both are
     supplied: the current page can't also be a link to itself without
     confusing the one signal (`aria-current="page"`) assistive tech relies on
     to say where the user is, so this stays correct by construction rather
     than by validating the caller's input.

     The separator lives inside this same <li>, not as its own list item -
     a bare decorative element can't be a direct child of <ol> (only <li> is
     valid there), and giving the separator its own <li> would inflate the
     "item N of M" count a screen reader announces for a list that's actually
     one item shorter. `aria-hidden="true"` (rather than a `::before` with
     generated CSS content) is the more certain of the two techniques the
     ARIA APG allows here: some AT/browser combinations have, at times,
     exposed `::before` text content despite it being "decorative," where an
     explicitly hidden real element leaves no ambiguity.

     `group-first:hidden` hides that separator specifically when this <li> is
     the first child of its parent <ol> - each breadcrumb-item call only
     knows its own label/href/current, never its position among siblings, so
     the position check has to happen in CSS against the actual rendered DOM
     rather than in Java against a list index. --%>
!{boolean isCurrent = current;}
!{boolean isLink = !isCurrent && href != null && !href.isEmpty();}
!{String ariaCurrentAttr = isCurrent ? "page" : null;}

<li class="group flex items-center gap-1.5">
    <span aria-hidden="true" data-breadcrumb-separator class="group-first:hidden text-slate-400 dark:text-slate-600">/</span>
    @if (isLink)
        <a href="${href}" class="break-words text-slate-500 rounded-sm hover:text-slate-700 hover:underline underline-offset-2 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 dark:text-slate-400 dark:hover:text-slate-200">${label}</a>
    @else
        <span aria-current="${ariaCurrentAttr}" class="${isCurrent ? "break-words font-medium text-slate-900 dark:text-slate-100" : "break-words text-slate-500 dark:text-slate-400"}">${label}</span>
    @endif
</li>
