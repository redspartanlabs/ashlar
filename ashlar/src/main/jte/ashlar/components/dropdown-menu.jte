@import dev.redspartanlabs.ashlar.dropdownmenu.DropdownMenuIds
@import dev.redspartanlabs.ashlar.icon.Icon
@import dev.redspartanlabs.ashlar.AshlarAssets

@param String label
@param gg.jte.Content items
@param Icon icon = null

<%-- A trigger button plus a menu surface, composed the same wrapper/item way
     as accordion.jte and breadcrumbs.jte - each entry is a separate
     @template.ashlar.components.dropdown-menu-item(...) call composed into `items`.
     Unlike those two, this one genuinely needs JS: a menu's open/closed
     state, keyboard roving focus, and outside-click dismissal are all
     runtime behavior with no HTML-only equivalent (contrast Breadcrumbs and
     Pagination, which are pure navigation and needed none).

     No `id` parameter: the trigger's `aria-controls` and the menu's own
     `id` only ever need to agree with each other, and both are rendered by
     this one component call - there's nothing external for a caller-chosen
     id to coordinate against, unlike Accordion/Tabs/Modal where several
     separately-called templates must agree on a shared id. DropdownMenuIds
     generates it instead (see its own doc comment).

     `role="menu"` + `role="menuitem"`, not `<ul>/<li>`: this is the
     WAI-ARIA APG "menu" pattern, a distinct widget from a list - unlike
     pagination.jte's `<ul>` (a genuine list of navigable pages), a
     dropdown's items are actions/destinations exposed from one trigger, and
     the menu/menuitem roles are what mark that widget's semantics for
     assistive tech.

     The menu starts `hidden` (not rendered) with data-state="closed" (its
     closed visual state), rather than hiding each item, so the whole surface
     enters and exits together as one animated unit; the chevron carries its
     own data-state. dropdown-menu.js sets all three, and dropdown-menu.css
     styles them: a 150ms fade and scale on the menu, a turn of the chevron,
     and neither under reduced motion. The open/closed state itself is never
     gated on the transition finishing (see dropdown-menu.js's own comment on
     this).

     `icon` is optional and purely decorative (`aria-hidden`, same as
     button.jte's own leading icon) - the trigger's accessible name always
     comes from its visible text, never the icon alone. --%>
!{String menuId = DropdownMenuIds.next();}
!{String triggerId = menuId + "-trigger";}

<div class="ash-dropdown-menu" data-dropdown-menu>
    <button
            type="button"
            id="${triggerId}"
            data-dropdown-menu-trigger
            aria-haspopup="menu"
            aria-expanded="false"
            aria-controls="${menuId}"
            class="ash-dropdown-menu__trigger"
    >
        @if (icon != null)
            <span aria-hidden="true" class="ash-dropdown-menu__icon">
                @template.ashlar.components.icon(icon = icon, classes = "ash-icon--sm")
            </span>
        @endif
        <span class="ash-dropdown-menu__label">${label}</span>
        <span data-dropdown-menu-chevron data-state="closed" aria-hidden="true" class="ash-dropdown-menu__chevron">
            @template.ashlar.components.icon(icon = Icon.CHEVRON_DOWN, classes = "ash-icon--sm")
        </span>
    </button>

    <div
            id="${menuId}"
            role="menu"
            aria-labelledby="${triggerId}"
            data-dropdown-menu-list
            data-state="closed"
            hidden
            class="ash-dropdown-menu__list"
    >
        ${items}
    </div>
</div>

<script type="module" src="${AshlarAssets.js("dropdown-menu.js")}"></script>
