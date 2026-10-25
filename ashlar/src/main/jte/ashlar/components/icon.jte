@import dev.redspartanlabs.ashlar.icon.Icon
@import dev.redspartanlabs.ashlar.icon.IconSvg

@param Icon icon
@param String classes = ""
@param String label = ""

<%-- Decorative by default (aria-hidden). Pass `label` only when this icon is
     not paired with visible text/aria-label elsewhere, i.e. it must convey
     meaning on its own.

     `classes` is added after Ashlar's own `ash-icon` class; it no longer
     replaces a default size. `ash-icon` (components/icon.css) gives the icon
     its default size, 1.25rem, and its inline behaviour. Add a size modifier
     (ash-icon--xs, --sm, --md, --lg, --xl), a Tailwind size utility, or set
     --ash-icon-size to change the size. --%>
!{String iconClasses = (classes == null || classes.isBlank()) ? "ash-icon" : "ash-icon " + classes.trim();}
!{String ariaHiddenAttr = label.isEmpty() ? "true" : null;}
!{String roleAttr = label.isEmpty() ? null : "img";}
!{String labelAttr = label.isEmpty() ? null : label;}

<svg
        class="${iconClasses}"
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
