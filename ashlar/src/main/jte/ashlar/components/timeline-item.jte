@import dev.redspartanlabs.ashlar.icon.Icon
@import dev.redspartanlabs.ashlar.badge.BadgeVariant
@import dev.redspartanlabs.ashlar.timeline.TimelineStyles

@param String title
@param String date = ""
@param gg.jte.Content content = null
@param Icon icon = null
@param String variant = "Neutral"

<%-- One entry: a marker circle, an optional date, a title, and optional
     richer content. `title` is the only required field - matching
     list-item.jte's own single-required-field shape - because it's the one
     thing an entry can't meaningfully exist without; a timeline entry with
     no date still says what happened, one with no content still says when
     and what, but one with neither wouldn't be an entry at all.

     `title` renders as a plain paragraph, not a heading: Card's own title
     is an `<h3>` because a Card is a distinct page section worth a
     landmark-adjacent heading, but a Timeline is a sequence of many small
     items the same way List's entries are - list-item.jte already made
     this exact call for the same reason (a screen reader user browsing by
     heading shouldn't have to wade through a dozen timeline entries as if
     each were its own section of the page).

     `date` is a plain string rendered as plain text, not wrapped in
     `<time>`: a caller-supplied display string ("2 hours ago," "Q1 2024",
     "March 2024") is frequently not a value HTML's `<time>` element
     actually accepts as valid content without a separate machine-readable
     `datetime` attribute this component has no reliable way to derive from
     an arbitrary string - guessing at one would risk generating markup
     that's invalid in a way that's worse than not using `<time>` at all.

     The marker's icon and color are both `aria-hidden` and purely
     decorative; `TimelineStyles.statusText()` supplies the one place a
     non-neutral `variant` is actually announced, as `ash-sr-only` text ahead
     of the title, the same pattern stepper-item.jte's own status text uses.

     The connector trailing this entry toward the next one reuses
     stepper-item.jte's own technique (a marker plus a trailing line, hidden
     on the last entry) rotated from horizontal to vertical: the marker column
     is a flex sibling of the content column, stretches to match whichever of
     the two is taller (flexbox's own default `align-items: stretch`), and the
     connector is an absolutely positioned line inside it running from the
     marker's own bottom edge downward - so it always reaches the next entry's
     marker regardless of how much content sits in between, with no JavaScript
     measuring anything. It is a real element, not a pseudo-element.

     The connector's own `bottom` extends past the marker column by exactly
     the gap between entries: an absolutely positioned child's offsets resolve
     against its containing block's PADDING box, and the gap lives on this
     `<li>`, one level further out than the marker column that stretches to
     match it - so stopping at the column's own edge would leave a visible
     break in the line before the next marker. Ashlar's CSS
     (components/timeline-item.css) uses the one property
     `--ash-timeline-item-gap` for both the gap and that extension, so they
     cannot drift apart. The same file hides the last entry's own connector
     (through its own rail, so a nested Timeline keeps its connectors) and
     removes the last entry's gap. --%>
!{BadgeVariant resolvedVariant = BadgeVariant.from(variant);}
!{boolean hasDate = !date.isEmpty();}
!{boolean hasContent = content != null;}
!{boolean hasIcon = icon != null;}
!{String markerClasses = TimelineStyles.marker(resolvedVariant);}
!{String statusText = TimelineStyles.statusText(resolvedVariant);}
!{boolean hasStatusText = !statusText.isEmpty();}

<li class="ash-timeline-item">
    <div class="ash-timeline-item__rail">
        <span aria-hidden="true" class="${markerClasses}">
            @if (hasIcon)
                @template.ashlar.components.icon(icon = icon, classes = "ash-icon--sm")
            @endif
        </span>
        <div class="ash-timeline-item__connector"></div>
    </div>

    <div class="ash-timeline-item__body">
        @if (hasDate)
            <p class="ash-timeline-item__date">${date}</p>
        @endif
        <p class="ash-timeline-item__title">
            @if (hasStatusText)
                <span class="ash-sr-only">${statusText}</span>
            @endif
            ${title}
        </p>
        @if (hasContent)
            <div class="ash-timeline-item__content">
                ${content}
            </div>
        @endif
    </div>
</li>
