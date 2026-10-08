@import dev.redspartanlabs.ashlar.icon.Icon
@import dev.redspartanlabs.ashlar.icon.IconSvg

@param Icon icon
@param String classes = "h-5 w-5"
@param String label = ""

<%-- Decorative by default (aria-hidden). Pass `label` only when this icon is
     not paired with visible text/aria-label elsewhere, i.e. it must convey
     meaning on its own. --%>
!{String ariaHiddenAttr = label.isEmpty() ? "true" : null;}
!{String roleAttr = label.isEmpty() ? null : "img";}
!{String labelAttr = label.isEmpty() ? null : label;}

<svg
        class="inline-block shrink-0 ${classes}"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="1.5"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="${ariaHiddenAttr}"
        role="${roleAttr}"
        aria-label="${labelAttr}"
>$unsafe{IconSvg.bodyFor(icon)}</svg>
