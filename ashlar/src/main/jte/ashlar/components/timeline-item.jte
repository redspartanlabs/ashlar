@import com.redspartanlabs.ashlar.icon.Icon
@import com.redspartanlabs.ashlar.badge.BadgeVariant
@import com.redspartanlabs.ashlar.timeline.TimelineStyles

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
     non-neutral `variant` is actually announced, as `sr-only` text ahead of
     the title, the same pattern stepper-item.jte's own status text uses.

     The connector trailing this entry toward the next one reuses
     stepper-item.jte's own technique (a marker plus a trailing line,
     `group-last:hidden` on the last entry) rotated from horizontal to
     vertical: the marker column is a flex sibling of the content column,
     stretches to match whichever of the two is taller (flexbox's own
     default `align-items: stretch`), and the connector is an absolutely
     positioned line inside it running from the marker's own bottom edge
     downward - so it always reaches the next entry's marker regardless of
     how much content sits in between, with no JavaScript measuring
     anything.

     The connector's own `bottom` is `-bottom-8`, not `bottom-0`: an
     absolutely positioned child's offsets resolve against its containing
     block's PADDING box, and the `pb-8` gap between entries lives on this
     `<li>`, one level further out than the marker column that stretches to
     match it - so a plain `bottom-0` stopped exactly at that column's own
     edge, short of the gap, leaving a visible break in the line before the
     next marker (caught live in the browser, not just in source). Extending
     by the same 8 (2rem) the gap itself uses is what closes that break
     without duplicating the gap value as a second number that could drift
     out of sync with the real one. --%>
!{BadgeVariant resolvedVariant = BadgeVariant.from(variant);}
!{boolean hasDate = !date.isEmpty();}
!{boolean hasContent = content != null;}
!{boolean hasIcon = icon != null;}
!{String markerClasses = TimelineStyles.marker(resolvedVariant);}
!{String statusText = TimelineStyles.statusText(resolvedVariant);}
!{boolean hasStatusText = !statusText.isEmpty();}

<li class="group relative flex gap-4 pb-8 last:pb-0">
    <div class="relative flex w-8 shrink-0 flex-col items-center">
        <span aria-hidden="true" class="relative z-10 flex h-8 w-8 shrink-0 items-center justify-center rounded-full border-2 ${markerClasses}">
            @if (hasIcon)
                @template.ashlar.components.icon(icon = icon, classes = "h-4 w-4")
            @endif
        </span>
        <div class="absolute -bottom-8 top-8 w-0.5 bg-slate-200 group-last:hidden dark:bg-slate-700"></div>
    </div>

    <div class="min-w-0 flex-1 pb-1 pt-1">
        @if (hasDate)
            <p class="text-xs font-medium text-slate-500 dark:text-slate-400">${date}</p>
        @endif
        <p class="mt-0.5 break-words text-sm font-semibold text-slate-900 dark:text-slate-100">
            @if (hasStatusText)
                <span class="sr-only">${statusText}</span>
            @endif
            ${title}
        </p>
        @if (hasContent)
            <div class="mt-1 break-words text-sm text-slate-600 dark:text-slate-300">
                ${content}
            </div>
        @endif
    </div>
</li>
