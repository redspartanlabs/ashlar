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

     Sizing is the viewport's width up to 28rem (components/drawer.css):
     full-bleed on a phone, a fixed column on anything wider. That's the one
     rule that keeps a drawer usable at narrow widths without a second mobile
     layout.

     The styling is Ashlar's own: the shared overlay primitives
     (components/overlay.css) plus the Drawer's edge, size, border and slide
     (components/drawer.css). Closed by default: the root carries `hidden`
     and data-state="closed"; drawer.js removes `hidden`, flips data-state to
     "open", and restores `hidden` once the exit has finished. The placement
     is a class on the root and on the panel, so the closed position needs no
     class handed to the script: the former data-drawer-closed-class
     attribute is gone (see the CHANGELOG). --%>
!{DrawerPlacement resolvedPlacement = DrawerPlacement.from(placement);}
!{String titleId = id + "-title";}

<div
        id="${id}"
        data-drawer
        data-state="closed"
        class="ash-overlay ash-drawer ${DrawerStyles.alignment(resolvedPlacement)}"
        hidden
>
    <div data-drawer-backdrop class="ash-overlay__backdrop"></div>

    <%-- `position: relative` (on every overlay surface) puts the panel above its
         own backdrop sibling, which is what makes a click on the panel
         structurally unable to be mistaken for a backdrop click - the backdrop
         simply never receives it. --%>
    <div
            data-drawer-panel
            role="dialog"
            aria-modal="true"
            aria-labelledby="${titleId}"
            tabindex="-1"
            class="ash-overlay__surface ash-drawer__panel ${DrawerStyles.border(resolvedPlacement)}"
    >
        <div class="ash-overlay__header">
            <h2 id="${titleId}" class="ash-overlay__title">${title}</h2>

            <button
                    type="button"
                    data-drawer-close="${id}"
                    aria-label="Close"
                    class="ash-overlay__close"
            >
                @template.ashlar.components.icon(icon = Icon.CLOSE)
            </button>
        </div>

        <%-- The panel is a flex column, so this is the only part that
             scrolls: the header and footer stay put while long content
             moves under them. ash-drawer__body stops a touch scroll that
             reaches the end of this region from chaining into the page
             behind the drawer. --%>
        <div class="ash-overlay__body ash-drawer__body">
            @if (body != null)
                ${body}
            @endif
        </div>

        @if (footer != null)
            <div class="ash-overlay__footer">
                ${footer}
            </div>
        @endif
    </div>
</div>

<script type="module" src="${AshlarAssets.js("drawer.js")}"></script>
