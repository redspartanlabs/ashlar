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

     The separator is not shown when this <li> is the first child of its
     parent <ol> - each breadcrumb-item call only knows its own
     label/href/current, never its position among siblings, so the position
     check has to happen in CSS against the actual rendered DOM rather than in
     Java against a list index. components/breadcrumb-item.css states it as
     the crumb's own rule (`.ash-breadcrumb-item:first-child >
     .ash-breadcrumb-item__separator`), so it cannot reach a crumb's
     separator through some unrelated ancestor. --%>
!{boolean isCurrent = current;}
!{boolean isLink = !isCurrent && href != null && !href.isEmpty();}
!{String ariaCurrentAttr = isCurrent ? "page" : null;}

<li class="ash-breadcrumb-item">
    <span aria-hidden="true" data-breadcrumb-separator class="ash-breadcrumb-item__separator">/</span>
    @if (isLink)
        <a href="${href}" class="ash-breadcrumb-item__link">${label}</a>
    @else
        <span aria-current="${ariaCurrentAttr}" class="${isCurrent ? "ash-breadcrumb-item__text ash-breadcrumb-item__text--current" : "ash-breadcrumb-item__text"}">${label}</span>
    @endif
</li>
