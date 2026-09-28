@import com.redspartanlabs.ashlar.alert.AlertVariant
@import com.redspartanlabs.ashlar.alert.AlertStyles
@import com.redspartanlabs.ashlar.icon.Icon

@param String variant = "Info"
@param String title = ""
@param gg.jte.Content body = null

<%-- A persistent, in-flow contextual message - not toast.jte's transient,
     JS-managed notification. That distinction is why this deliberately has
     no `role="alert"`/`role="status"`: both are implicit live regions, and
     this component has no JavaScript and no show/hide mechanism - every
     instance is already part of the page on first render. Giving a
     server-rendered, always-present element a live-region role doesn't make
     it more accessible; in several screen reader/browser combinations it
     causes every alert already on the page to be announced the moment the
     page loads, which is exactly the disruptive behavior live regions exist
     to avoid for content that isn't actually a live update. A future
     component that dynamically injects an alert after page load would
     genuinely need that semantics - this one, today, does not.

     `title`, when present, is a real <h3> - matching card.jte's own title
     treatment, not toast.jte's plain <p>. A toast's title isn't part of the
     page's lasting structure (it disappears on its own); an alert sits in
     the page for as long as the condition it describes is true, so it earns
     a real heading the same way Card's does.

     The sr-only variant label exists so severity is never carried by color
     or the (decorative) icon alone - the same problem toast.jte solves the
     same way. --%>
!{AlertVariant resolvedVariant = AlertVariant.from(variant);}
!{boolean hasTitle = !title.isEmpty();}
!{String bodySpacing = hasTitle ? "mt-1 " : "";}

<div class="${AlertStyles.container(resolvedVariant)}" data-alert data-alert-variant="${resolvedVariant.name().toLowerCase()}">
    @template.ashlar.components.icon(icon = resolvedVariant.defaultIcon(), classes = "mt-0.5 h-5 w-5 " + AlertStyles.icon(resolvedVariant))

    <div class="min-w-0 flex-1">
        <span class="sr-only">${resolvedVariant.label()}:</span>
        @if (hasTitle)
            <h3 class="text-sm font-semibold ${AlertStyles.title(resolvedVariant)}">${title}</h3>
        @endif
        @if (body != null)
            <div class="${bodySpacing}text-sm ${AlertStyles.body(resolvedVariant)}">
                ${body}
            </div>
        @endif
    </div>
</div>
