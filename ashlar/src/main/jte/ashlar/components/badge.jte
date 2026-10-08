@import dev.redspartanlabs.ashlar.badge.BadgeVariant
@import dev.redspartanlabs.ashlar.badge.BadgeStyles
@import dev.redspartanlabs.ashlar.icon.Icon

@param String text
@param String variant = "Neutral"
@param Icon icon = null

<%-- A static label, not a control: a plain <span>, no role, no JavaScript,
     no click handling. `role="status"` is deliberately not applied here -
     that would claim this badge is a live-region announcement, which is
     true of at most some call sites and not a property of the badge itself;
     a page that actually needs that wraps this in its own aria-live region.

     The icon is optional and purely reinforcing - the variant's meaning
     already lives in the visible text, never in color or icon alone, so the
     icon is always decorative (aria-hidden via icon.jte's own default). --%>
!{BadgeVariant resolvedVariant = BadgeVariant.from(variant);}

<span class="${BadgeStyles.container(resolvedVariant)}" data-badge data-badge-variant="${resolvedVariant.name().toLowerCase()}">
    @if (icon != null)
        @template.ashlar.components.icon(icon = icon, classes = "h-3 w-3")
    @endif
    ${text}
</span>
