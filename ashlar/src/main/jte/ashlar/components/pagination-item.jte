@param String label
@param String href = null
@param boolean current = false

<%-- One entry: a real <a href> for anywhere the caller can actually navigate
     to, or non-interactive text otherwise. `current=true` wins over `href`
     even when both are supplied - same precedence rule as
     breadcrumb-item.jte, for the same reason: the current page can't also be
     a link to itself without confusing the one signal (`aria-current="page"`)
     assistive tech relies on.

     A non-current item with no `href` covers two different callers at once -
     an unavailable Previous/Next ("no page 0 to go back to") and an ellipsis
     ("…", standing in for pages the caller chose not to list) - and both
     render through this same branch rather than a separate parameter for
     each: neither is a real link, neither should be focusable, and neither
     should look clickable, so there's nothing left for a distinct type to
     add. `aria-disabled="true"` communicates "not currently activatable" for
     the Previous/Next case; on the ellipsis's plain `<span>` (which has no
     interactive role to begin with) the same attribute is simply inert. This
     also keeps the component label-agnostic - it never inspects `label` to
     guess whether an item is "Previous," "Next," or an ellipsis, which is
     what keeps it a small, boring primitive rather than a parser.

     No chevron icons: the API has no way to know an item means "Previous"
     or "Next" specifically (that's just a `label` string like any page
     number), and guessing from the label's text would mean parsing
     caller-supplied content to decide how to render it. The visible label
     is what communicates that, same as the rest of the library avoids
     encoding meaning in an icon alone. --%>
!{boolean isCurrent = current;}
!{boolean isLink = !isCurrent && href != null && !href.isEmpty();}
!{String ariaCurrentAttr = isCurrent ? "page" : null;}
!{String ariaDisabledAttr = (!isCurrent && !isLink) ? "true" : null;}

<li class="ash-pagination-item">
    @if (isLink)
        <a href="${href}" class="ash-pagination-item__control ash-pagination-item__control--link">${label}</a>
    @elseif (isCurrent)
        <span aria-current="${ariaCurrentAttr}" class="ash-pagination-item__control ash-pagination-item__control--current">${label}</span>
    @else
        <span aria-disabled="${ariaDisabledAttr}" class="ash-pagination-item__control ash-pagination-item__control--disabled">${label}</span>
    @endif
</li>
