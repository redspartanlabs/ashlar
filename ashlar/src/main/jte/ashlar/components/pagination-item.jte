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

!{String linkClasses = "inline-flex h-8 min-w-8 items-center justify-center rounded-md border border-slate-200 px-2 font-medium text-slate-600 hover:bg-slate-50 hover:text-slate-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800 dark:hover:text-slate-100";}
!{String currentClasses = "inline-flex h-8 min-w-8 items-center justify-center rounded-md border border-blue-600 bg-blue-600 px-2 font-medium text-white dark:border-blue-500 dark:bg-blue-500";}
!{String disabledClasses = "inline-flex h-8 min-w-8 items-center justify-center px-2 font-medium text-slate-300 dark:text-slate-600";}

<li>
    @if (isLink)
        <a href="${href}" class="${linkClasses}">${label}</a>
    @elseif (isCurrent)
        <span aria-current="${ariaCurrentAttr}" class="${currentClasses}">${label}</span>
    @else
        <span aria-disabled="${ariaDisabledAttr}" class="${disabledClasses}">${label}</span>
    @endif
</li>
