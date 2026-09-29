@import com.redspartanlabs.ashlar.dropdownmenu.DropdownMenuIds
@import com.redspartanlabs.ashlar.icon.Icon
@import com.redspartanlabs.ashlar.AshlarAssets

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

     Menu items start invisible-but-present (`hidden` on the menu itself)
     rather than `display:none`-per-item, so the whole surface enters/exits
     together as one animated unit - the same technique select.jte's own
     listbox already uses for its trigger-triggered menu, whose transition
     classes (duration-150, scale-95/opacity-0 to scale-100/opacity-100)
     this reuses so both dropdown-shaped popups in the library share exactly
     one animation feel. `motion-reduce:transition-none` on both the panel
     and chevron disables it under reduced motion, without changing what the
     JS does - the open/closed state itself is never gated on the
     transition finishing (see dropdown-menu.js's own comment on this).

     `icon` is optional and purely decorative (`aria-hidden`, same as
     button.jte's own leading icon) - the trigger's accessible name always
     comes from its visible text, never the icon alone. --%>
!{String menuId = DropdownMenuIds.next();}
!{String triggerId = menuId + "-trigger";}

<div class="relative inline-block" data-dropdown-menu>
    <button
            type="button"
            id="${triggerId}"
            data-dropdown-menu-trigger
            aria-haspopup="menu"
            aria-expanded="false"
            aria-controls="${menuId}"
            class="inline-flex h-9 items-center gap-1.5 rounded-md border border-slate-300 bg-white px-3 text-sm font-medium text-slate-700 shadow-sm hover:border-slate-400 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:hover:border-slate-500"
    >
        @if (icon != null)
            <span aria-hidden="true">
                @template.ashlar.components.icon(icon = icon, classes = "h-4 w-4")
            </span>
        @endif
        <span>${label}</span>
        <span data-dropdown-menu-chevron aria-hidden="true" class="shrink-0 text-slate-400 transition-transform duration-150 ease-out motion-reduce:transition-none dark:text-slate-500">
            @template.ashlar.components.icon(icon = Icon.CHEVRON_DOWN, classes = "h-4 w-4")
        </span>
    </button>

    <div
            id="${menuId}"
            role="menu"
            aria-labelledby="${triggerId}"
            data-dropdown-menu-list
            class="absolute left-0 z-10 mt-1.5 hidden min-w-40 max-w-xs origin-top-left scale-95 rounded-md bg-white py-1 opacity-0 shadow-lg ring-1 ring-slate-200 transition duration-150 ease-out motion-reduce:transition-none dark:bg-slate-800 dark:ring-slate-700"
    >
        ${items}
    </div>
</div>

<script type="module" src="${AshlarAssets.js("dropdown-menu.js")}"></script>
