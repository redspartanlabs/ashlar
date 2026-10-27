@import dev.redspartanlabs.ashlar.icon.Icon

@param String label
@param String description = ""
@param String href = null
@param Icon icon = null
@param gg.jte.Content trailing = null

<%-- One row: a real `<a href>` when the caller supplies one, or
     non-interactive content otherwise - the same "missing href means plain
     content, not a dead link" convention breadcrumb-item.jte,
     pagination-item.jte and dropdown-menu-item.jte all already established.
     A clickable row is the whole `<li>`'s content wrapped in one `<a>`
     (a "navigate to this record" list, say), not a link buried inside
     otherwise-static text, which is what keeps the click target and the
     keyboard focus target the same element.

     `icon` and the auto chevron are both purely decorative (`aria-hidden`):
     the row's meaning comes entirely from `label`/`description`, matching
     dropdown-menu.jte's own leading-icon reasoning. The chevron specifically
     is not the same mistake pagination-item.jte's doc comment warns against
     (guessing meaning from a label's text) - it's drawn purely from whether
     `href` is set, a fact already known at render time, not inferred from
     what the label says.

     `trailing` is `gg.jte.Content`, not a `String`, because what belongs on
     the right edge of a row varies by list - a Badge showing status, a
     timestamp, a value in a metadata pair - and none of those is well
     served by a single string parameter. It stays a single content slot
     rather than a second full item shape, keeping this one small primitive
     instead of forking into "list item" and "metadata item" variants that
     would otherwise be identical.

     `min-width: 0` on the label/description column (components/list-item.css)
     is what lets a long label or description wrap inside the row instead of
     pushing the icon or trailing content out past the list's own border - the
     same fix the Stepper
     component needed for its labels, for the same underlying flexbox
     reason (a flex child's minimum width defaults to its content's, not to
     what's actually available). --%>
!{boolean isLink = href != null && !href.isEmpty();}
!{boolean hasDescription = !description.isEmpty();}
!{boolean hasIcon = icon != null;}
!{boolean hasTrailing = trailing != null;}

<li class="ash-list-item">
    @if (isLink)
        <a href="${href}" class="ash-list-item__row ash-list-item__row--link">
            @if (hasIcon)
                <span class="ash-list-item__icon" aria-hidden="true">
                    @template.ashlar.components.icon(icon = icon, classes = "ash-icon--md")
                </span>
            @endif

            <span class="ash-list-item__body">
                <span class="ash-list-item__label">${label}</span>
                @if (hasDescription)
                    <span class="ash-list-item__description">${description}</span>
                @endif
            </span>

            @if (hasTrailing)
                <span class="ash-list-item__trailing">
                    ${trailing}
                </span>
            @endif

            <span class="ash-list-item__chevron" aria-hidden="true">
                @template.ashlar.components.icon(icon = Icon.CHEVRON_RIGHT, classes = "ash-icon--sm")
            </span>
        </a>
    @else
        <div class="ash-list-item__row">
            @if (hasIcon)
                <span class="ash-list-item__icon" aria-hidden="true">
                    @template.ashlar.components.icon(icon = icon, classes = "ash-icon--md")
                </span>
            @endif

            <span class="ash-list-item__body">
                <span class="ash-list-item__label">${label}</span>
                @if (hasDescription)
                    <span class="ash-list-item__description">${description}</span>
                @endif
            </span>

            @if (hasTrailing)
                <span class="ash-list-item__trailing">
                    ${trailing}
                </span>
            @endif
        </div>
    @endif
</li>
