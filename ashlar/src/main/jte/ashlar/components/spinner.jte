@import com.redspartanlabs.ashlar.spinner.SpinnerSize
@import com.redspartanlabs.ashlar.spinner.SpinnerStyles

@param String size = "Medium"
@param String label = "Loading"

<%-- Pure CSS: a border-radius circle with one edge colored and
     `animate-spin` rotating it - no SVG, no JavaScript, no timers.
     `motion-reduce:animate-none` freezes it for users who asked for less
     motion; unlike a transition, a spinner is an animation that plays
     forever, so it needs to actually stop rather than just skip its
     transition, which is why this uses `animate-none` where the rest of the
     library's motion-reduce guards use `transition-none`.

     `label` follows icon.jte's own convention: non-empty (the default,
     "Loading") makes this an announced `role="status"` region with the
     visual spinner hidden from assistive tech and a sr-only text
     alternative in its place; pass `label = ""` when this spinner sits
     inside something that already owns its own loading announcement (e.g.
     button.jte's own `loading` prop already manages `aria-busy` and its own
     status text) - the visual would otherwise double up as a second,
     redundant announcement. --%>
!{SpinnerSize resolvedSize = SpinnerSize.from(size);}
!{boolean hasLabel = !label.isEmpty();}
!{String roleAttr = hasLabel ? "status" : null;}
!{String ariaLiveAttr = hasLabel ? "polite" : null;}

<span class="inline-flex items-center" role="${roleAttr}" aria-live="${ariaLiveAttr}" data-spinner data-spinner-size="${resolvedSize.name().toLowerCase()}">
    <span aria-hidden="true" class="${SpinnerStyles.classes(resolvedSize)}"></span>
    @if (hasLabel)
        <span class="sr-only">${label}</span>
    @endif
</span>
