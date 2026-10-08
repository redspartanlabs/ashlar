@import dev.redspartanlabs.ashlar.drawer.DrawerPlacement
@import dev.redspartanlabs.ashlar.drawer.DrawerStyles
@import dev.redspartanlabs.ashlar.icon.Icon
@import dev.redspartanlabs.ashlar.AshlarAssets

@param String id
@param String title
@param String placement = "Right"
@param gg.jte.Content body = null
@param gg.jte.Content footer = null

<%-- A full-height panel anchored to one edge of the viewport, opened from
     anywhere on the page by an element carrying
     `data-drawer-open="{id}"` and closed by `data-drawer-close="{id}"` -
     the same trigger contract modal.jte uses, and for the same reason: this
     is a page-level surface, not something that belongs wrapped around a
     trigger the way popover.jte's content slot is. That also lets several
     controls open the same drawer, which a wrapper couldn't express.

     Modal semantics, deliberately, not Popover's. Popover is non-modal - it
     leaves the page usable behind it, so disclosure semantics
     (`aria-expanded` on the trigger, no role on the surface) are the honest
     description of what it does. A Drawer covers the page with a backdrop,
     locks the page's scroll, and traps Tab inside itself; something that
     behaves like a modal dialog has to *say* modal dialog, so the panel is
     `role="dialog"` + `aria-modal="true"`, named by its own heading. For
     the same reason the trigger gets no `aria-expanded`: that attribute
     describes a control that reveals adjacent content, and the APG's dialog
     pattern doesn't use it - claiming it here would describe an interaction
     this isn't.

     `title` is required rather than optional, unlike popover.jte's: a
     dialog must have an accessible name, and the heading is where it comes
     from (`aria-labelledby`). An untitled drawer would be an unnamed
     dialog, which is a defect, not a style choice.

     The close button is always rendered and Escape always closes: both are
     the only ways out for a keyboard user once Tab is trapped inside, so
     neither is something a caller should be able to switch off. modal.jte
     does expose those as parameters; that flexibility isn't carried over
     here because nothing in this component needs it - see the showcase page
     for the cases this actually has to serve.

     Sizing is `w-screen max-w-md`: full-bleed on a phone, a fixed column on
     anything wider. That's the one rule that keeps a drawer usable at
     narrow widths without a second mobile layout. --%>
!{DrawerPlacement resolvedPlacement = DrawerPlacement.from(placement);}
!{String titleId = id + "-title";}
!{String closedTransform = DrawerStyles.closedTransform(resolvedPlacement);}

<div
        id="${id}"
        data-drawer
        data-drawer-closed-class="${closedTransform}"
        class="fixed inset-0 z-40 hidden ${DrawerStyles.alignment(resolvedPlacement)}"
>
    <div
            data-drawer-backdrop
            class="absolute inset-0 bg-slate-900/50 opacity-0 transition-opacity duration-200 ease-out motion-reduce:transition-none"
    ></div>

    <%-- `relative` puts the panel above its own backdrop sibling, which is
         what makes a click on the panel structurally unable to be mistaken
         for a backdrop click - the backdrop simply never receives it. --%>
    <div
            data-drawer-panel
            role="dialog"
            aria-modal="true"
            aria-labelledby="${titleId}"
            tabindex="-1"
            class="relative flex h-full w-screen max-w-md flex-col bg-white shadow-xl transition-transform duration-200 ease-out focus:outline-none motion-reduce:transition-none ${DrawerStyles.border(resolvedPlacement)} border-slate-200 ${closedTransform} dark:border-slate-700 dark:bg-slate-900"
    >
        <div class="flex items-center justify-between gap-4 border-b border-slate-200 px-6 py-4 dark:border-slate-700">
            <h2 id="${titleId}" class="text-base font-semibold text-slate-900 dark:text-slate-100">${title}</h2>

            <button
                    type="button"
                    data-drawer-close="${id}"
                    aria-label="Close"
                    class="rounded p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 dark:hover:bg-slate-800 dark:hover:text-slate-300"
            >
                @template.ashlar.components.icon(icon = Icon.CLOSE, classes = "h-5 w-5")
            </button>
        </div>

        <%-- The panel is a flex column, so this is the only part that
             scrolls: the header and footer stay put while long content
             moves under them. `overscroll-contain` stops a touch scroll
             that reaches the end of this region from chaining into the page
             behind the drawer. --%>
        <div class="flex-1 overflow-y-auto overscroll-contain px-6 py-4 text-sm text-slate-700 dark:text-slate-300">
            @if (body != null)
                ${body}
            @endif
        </div>

        @if (footer != null)
            <div class="flex flex-wrap items-center justify-end gap-3 border-t border-slate-200 bg-slate-50 px-6 py-4 dark:border-slate-700 dark:bg-slate-800">
                ${footer}
            </div>
        @endif
    </div>
</div>

<script type="module" src="${AshlarAssets.js("drawer.js")}"></script>
