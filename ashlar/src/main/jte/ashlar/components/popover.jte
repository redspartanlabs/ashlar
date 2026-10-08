@import dev.redspartanlabs.ashlar.popover.PopoverIds
@import dev.redspartanlabs.ashlar.popover.PopoverPlacement
@import dev.redspartanlabs.ashlar.popover.PopoverStyles
@import dev.redspartanlabs.ashlar.AshlarAssets

@param gg.jte.Content trigger
@param gg.jte.Content content
@param String title = ""
@param String placement = "Bottom"

<%-- A clicked, non-modal surface anchored to a caller-provided trigger.
     Where tooltip.jte answers "what does this control mean" with a short
     string it will not let you put anything else in, this one is the other
     half of that split: `content` is real `gg.jte.Content`, because the
     entire point of a Popover is that what's inside can be read, focused
     and operated - links, buttons, a small form.

     Deliberately NOT `role="dialog"`. modal.jte already owns the dialog
     case, and it earns that role by being modal: `aria-modal="true"`, a
     backdrop, and a focus trap that keeps you inside until you close it.
     This surface does none of that on purpose - you can Tab straight
     through it and back out into the page. Announcing a dialog that doesn't
     behave like one is worse than announcing nothing, because a screen
     reader user is told they've entered something they then silently walk
     out of. What's left is exactly the APG Disclosure pattern - a control
     that shows and hides a section of content - so that's what the trigger
     says: `aria-expanded` for the state, `aria-controls` for the target,
     and no `aria-haspopup` (which would claim a menu/dialog/listbox this
     isn't). The surface itself gets no role at all; its content is ordinary
     page content and reads as such.

     The trigger is opaque composed content, so popover.js finds the real
     interactive element inside it and puts the ARIA and the click handler
     there (same reason tooltip.jte defers this to its own JS - there's no
     AST here to reach into). A real <button> is the expected trigger; the
     fallback for anything else is described in popover.js.

     `title` is optional and renders as a plain heading inside the surface.
     It is not wired to `aria-labelledby`, because without a role there is
     nothing to name - it's there so rich content has a visible, and
     heading-navigable, top line.

     Placement and clamping work the way tooltip's do, implemented
     separately for the reasons PopoverStyles' own doc comment gives - as
     does the decision not to draw an arrow. The same two limits apply, and
     are inherent to staying CSS-anchored rather than becoming a positioning
     engine: a placement with no room on the side it was asked for clips on
     that side (there is no flipping), and an `overflow-hidden` ancestor
     clips the surface. --%>
!{String popoverId = PopoverIds.next();}
!{PopoverPlacement resolvedPlacement = PopoverPlacement.from(placement);}
!{String positionClasses = PopoverStyles.position(resolvedPlacement);}
!{boolean hasTitle = !title.isEmpty();}

<span class="relative inline-block" data-popover data-popover-placement="${resolvedPlacement.name().toLowerCase()}">
    <span data-popover-trigger class="inline-block">
        ${trigger}
    </span>

    <div
            id="${popoverId}"
            data-popover-surface
            class="absolute z-30 hidden w-72 max-w-[calc(100vw-1rem)] scale-95 rounded-lg bg-white p-4 text-left opacity-0 shadow-lg ring-1 ring-slate-200 transition duration-150 ease-out motion-reduce:transition-none dark:bg-slate-800 dark:ring-slate-700 ${positionClasses}"
    >
        @if (hasTitle)
            <h3 class="mb-1 text-sm font-semibold text-slate-900 dark:text-slate-100">${title}</h3>
        @endif

        <div class="text-sm text-slate-600 dark:text-slate-300">
            ${content}
        </div>
    </div>
</span>

<script type="module" src="${AshlarAssets.js("popover.js")}"></script>
