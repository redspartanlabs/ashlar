@import com.redspartanlabs.ashlar.icon.Icon

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

     `min-w-0` on the label/description column is what lets a long label or
     description wrap inside the row instead of pushing the icon or trailing
     content out past the list's own border - the same fix the Stepper
     component needed for its labels, for the same underlying flexbox
     reason (a flex child's minimum width defaults to its content's, not to
     what's actually available). --%>
!{boolean isLink = href != null && !href.isEmpty();}
!{boolean hasDescription = !description.isEmpty();}
!{boolean hasIcon = icon != null;}
!{boolean hasTrailing = trailing != null;}

!{String rowClasses = "flex items-center gap-3 px-4 py-3 text-sm";}
!{String interactiveClasses = isLink ? " hover:bg-slate-50 focus:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-blue-500 dark:hover:bg-slate-800/50" : "";}

<li>
    @if (isLink)
        <a href="${href}" class="${rowClasses}${interactiveClasses}">
            @if (hasIcon)
                <span class="shrink-0 text-slate-400 dark:text-slate-500" aria-hidden="true">
                    @template.ashlar.components.icon(icon = icon, classes = "h-5 w-5")
                </span>
            @endif

            <span class="min-w-0 flex-1">
                <span class="block font-medium text-slate-900 dark:text-slate-100">${label}</span>
                @if (hasDescription)
                    <span class="block text-slate-500 dark:text-slate-400">${description}</span>
                @endif
            </span>

            @if (hasTrailing)
                <span class="shrink-0 text-slate-500 dark:text-slate-400">
                    ${trailing}
                </span>
            @endif

            <span class="shrink-0 text-slate-400 dark:text-slate-500" aria-hidden="true">
                @template.ashlar.components.icon(icon = Icon.CHEVRON_RIGHT, classes = "h-4 w-4")
            </span>
        </a>
    @else
        <div class="${rowClasses}">
            @if (hasIcon)
                <span class="shrink-0 text-slate-400 dark:text-slate-500" aria-hidden="true">
                    @template.ashlar.components.icon(icon = icon, classes = "h-5 w-5")
                </span>
            @endif

            <span class="min-w-0 flex-1">
                <span class="block font-medium text-slate-900 dark:text-slate-100">${label}</span>
                @if (hasDescription)
                    <span class="block text-slate-500 dark:text-slate-400">${description}</span>
                @endif
            </span>

            @if (hasTrailing)
                <span class="shrink-0 text-slate-500 dark:text-slate-400">
                    ${trailing}
                </span>
            @endif
        </div>
    @endif
</li>
