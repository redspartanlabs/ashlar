@import dev.redspartanlabs.ashlar.icon.Icon

@param String label
@param String href = null
@param boolean current = false
@param Icon icon = null

<%-- One entry in a Sidebar's link list. Same "current wins over href"
     precedence as nav-link.jte and breadcrumb-item.jte, and the same reason:
     `aria-current="page"` can't describe a link to itself.

     A separate component from nav-link.jte rather than one item shared by
     both, despite the near-identical params - the two live in genuinely
     different visual languages (an inline horizontal row of text versus a
     block-level vertical list that commonly carries a leading icon), the
     same reasoning that keeps breadcrumb-item.jte and pagination-item.jte
     apart instead of one item guessing its container from a flag. Forcing
     one component to answer to both would mean either parameter, both
     read by the template to decide which of two unrelated layouts to draw.

     `icon` is optional and purely decorative (`aria-hidden`) - the row's
     meaning comes entirely from `label`, the same reasoning list-item.jte
     already gives for its own leading icon. `min-w-0`/`truncate` on the
     label is what lets a long entry clip with an ellipsis instead of
     forcing the sidebar wider than the column a caller placed it in - the
     same flexbox fix list-item.jte's own doc comment explains. --%>
!{boolean isCurrent = current;}
!{boolean isLink = !isCurrent && href != null && !href.isEmpty();}
!{String ariaCurrentAttr = isCurrent ? "page" : null;}
!{boolean hasIcon = icon != null;}

!{String linkClasses = "ash-sidebar-item";}
!{String currentClasses = "ash-sidebar-item ash-sidebar-item--current";}

<li class="ash-sidebar-item__entry">
    @if (isLink)
        <a href="${href}" class="${linkClasses}">
            @if (hasIcon)
                <span class="ash-sidebar-item__icon" aria-hidden="true">
                    @template.ashlar.components.icon(icon = icon, classes = "ash-icon--sm")
                </span>
            @endif
            <span class="ash-sidebar-item__label">${label}</span>
        </a>
    @else
        <span aria-current="${ariaCurrentAttr}" class="${currentClasses}">
            @if (hasIcon)
                <span class="ash-sidebar-item__icon" aria-hidden="true">
                    @template.ashlar.components.icon(icon = icon, classes = "ash-icon--sm")
                </span>
            @endif
            <span class="ash-sidebar-item__label">${label}</span>
        </span>
    @endif
</li>
