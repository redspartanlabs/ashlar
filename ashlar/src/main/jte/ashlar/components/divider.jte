@import dev.redspartanlabs.ashlar.divider.DividerOrientation
@import dev.redspartanlabs.ashlar.divider.DividerStyles

@param String orientation = "Horizontal"

<%-- A visual separator, not a layout primitive: it draws the line, the
     surrounding page owns all spacing (margin, gap, padding) around it.

     Horizontal is a real <hr> - the correct native element for a thematic
     break, with an implicit `separator` role and a default orientation of
     "horizontal" per the HTML-AAM, so no ARIA is added for it.

     Vertical has no native element to reach for - <hr> is defined as a
     paragraph-level break in the flow of content, and forcing it vertical
     via CSS would misrepresent that semantic even though it can be made to
     look right. A <div role="separator" aria-orientation="vertical"> is the
     standard accessible pattern for this (see the WAI-ARIA APG toolbar
     separator example) and is used here instead. --%>
!{DividerOrientation resolvedOrientation = DividerOrientation.from(orientation);}

@if (resolvedOrientation == DividerOrientation.VERTICAL)
    <div
            role="separator"
            aria-orientation="vertical"
            class="${DividerStyles.vertical()}"
            data-divider
            data-divider-orientation="vertical"
    ></div>
@else
    <hr class="${DividerStyles.horizontal()}" data-divider data-divider-orientation="horizontal">
@endif
