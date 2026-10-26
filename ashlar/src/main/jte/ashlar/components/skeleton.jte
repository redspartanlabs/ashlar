@import dev.redspartanlabs.ashlar.skeleton.SkeletonShape
@import dev.redspartanlabs.ashlar.skeleton.SkeletonStyles

@param String width = "100%"
@param String height = "1rem"
@param String shape = "Rectangle"

<%-- A single placeholder shape - nothing more. Text lines, avatars,
     thumbnails, whole card layouts are all compositions of several of these,
     assembled by the page the same way it assembles Badge or Divider; there
     is deliberately no CardSkeleton/AvatarSkeleton, because those are
     application layouts, not library primitives.

     `width`/`height` go through an inline `style` attribute rather than a
     class for the same reason progress.jte's fill width does: a stylesheet
     can't have a class for an arbitrary runtime string, and these two values
     describe the *shape being represented*, not arbitrary styling - there's
     no `class`/`style` param a caller can inject anything else through.
     JTE's own `${}` output escaping keeps this attribute well-formed
     regardless of what's passed.

     `aria-hidden="true"` unconditionally: a skeleton is never itself
     announced. Whether a group of them should be exposed as "loading" to
     assistive tech (an `aria-busy` region, a `role="status"` with its own
     text) is a decision about the surrounding composition, not something
     this primitive can know or should guess at - matching card.jte's own
     refusal to add a role the call site hasn't actually asked for.

     The pulse is an opacity animation defined by components/skeleton.css - no
     JavaScript, no duration/speed knob. That stylesheet removes it for users
     who asked for less motion, the same way spinner.jte's own continuous
     animation is removed. --%>
!{SkeletonShape resolvedShape = SkeletonShape.from(shape);}

<div
        aria-hidden="true"
        class="${SkeletonStyles.classes(resolvedShape)}"
        style="width: ${width}; height: ${height}"
        data-skeleton
        data-skeleton-shape="${resolvedShape.name().toLowerCase()}"
></div>
