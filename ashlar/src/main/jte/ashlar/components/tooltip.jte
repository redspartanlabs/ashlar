@import com.redspartanlabs.ashlar.tooltip.TooltipIds
@import com.redspartanlabs.ashlar.tooltip.TooltipPlacement
@import com.redspartanlabs.ashlar.tooltip.TooltipStyles
@import com.redspartanlabs.ashlar.AshlarAssets

@param gg.jte.Content trigger
@param String content
@param String placement = "Top"

<%-- Wraps caller-provided `trigger` content with a hidden-until-needed
     bubble, without ever touching what `trigger` actually is. Button, Icon,
     and Link stay completely unaware this exists - the association is
     built here and in tooltip.js, not inside them. This is deliberately
     not the wrapper/item pattern the rest of the library uses (Accordion,
     Breadcrumbs, Stepper): those compose several *of the library's own*
     items into one caller-authored sequence, where Tooltip composes exactly
     one piece of arbitrary caller content with one piece of text - a single
     content slot is the right shape for that, not a list.

     `content` is a plain `String`, not `gg.jte.Content`: a tooltip answers
     "what does this control mean," a short run of text, not "give me a
     surface to put other things in" - that second job is the future
     Popover's, not this component's. Keeping `content` a String forecloses
     a caller quietly turning this into a Popover by composing buttons,
     forms, or nested components into it, and gets JTE's own HTML-escaping
     on `${content}` for free.

     Why JS has to find the actual interactive element itself (see
     tooltip.js) rather than this template placing `aria-describedby`
     directly: `trigger` is opaque, already-rendered content by the time it
     reaches `${trigger}` - there's no AST here to reach into and add an
     attribute to whatever real button/link/span is inside it, only a
     block of markup to place as-is.

     `role="tooltip"` + `aria-describedby` (toggled by JS, not always
     present) is the WAI-ARIA APG's own tooltip pattern: the description is
     only "active" while the bubble is actually visible, so the attribute
     tracks that rather than sitting there unconditionally. `pointer-events-
     none` on the bubble keeps it from ever being a hover target of its own,
     which is what stops a hide-on-mouseleave from racing against a mouse
     crossing over the bubble on its way to somewhere else - there is
     nothing to hover in the first place.

     `placement` picks which side the bubble sits on; TOP is the default
     because it's the one that stays clear of the pointer and of whatever
     sits below a control in a dense form. tooltip.js nudges the bubble back
     inside the viewport along the axis it's centered on (and slides the
     arrow the other way so it keeps pointing at the trigger), but it does
     NOT flip to the opposite side: a placement with no room on the side it
     was asked for is the caller picking the wrong side, and silently
     overriding that would make placement unpredictable. Two real limits
     follow from that, both inherent to staying CSS-positioned rather than
     becoming a positioning engine: a tooltip whose chosen side has no room
     will clip on that side, and one inside an `overflow-hidden` ancestor is
     clipped by that ancestor. Pick the side with room; TOP plus the nudge
     covers the ordinary cases. --%>
!{String tooltipId = TooltipIds.next();}
!{TooltipPlacement resolvedPlacement = TooltipPlacement.from(placement);}
!{String positionClasses = TooltipStyles.position(resolvedPlacement);}
!{String arrowClasses = TooltipStyles.arrow(resolvedPlacement);}

<span class="relative inline-block" data-tooltip data-tooltip-placement="${resolvedPlacement.name().toLowerCase()}">
    <span data-tooltip-trigger class="inline-block">
        ${trigger}
    </span>
    <span
            id="${tooltipId}"
            role="tooltip"
            data-tooltip-bubble
            class="pointer-events-none absolute z-20 hidden w-max max-w-64 scale-95 whitespace-normal rounded-md bg-slate-900 px-2.5 py-1.5 text-center text-xs font-medium text-white opacity-0 shadow-lg transition duration-150 ease-out motion-reduce:transition-none dark:bg-slate-100 dark:text-slate-900 ${positionClasses}"
    >
        ${content}
        <span aria-hidden="true" data-tooltip-arrow class="absolute h-2 w-2 rotate-45 bg-slate-900 dark:bg-slate-100 ${arrowClasses}"></span>
    </span>
</span>

<script type="module" src="${AshlarAssets.js("tooltip.js")}"></script>
