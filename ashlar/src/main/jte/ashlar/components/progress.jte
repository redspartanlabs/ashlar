@import dev.redspartanlabs.ashlar.progress.ProgressSize
@import dev.redspartanlabs.ashlar.progress.ProgressStyles

@param int value
@param String label = ""
@param boolean showPercentage = true
@param String size = "Medium"

<%-- A determinate, static progress indicator - render the current value,
     nothing more. Distinct from spinner.jte: that communicates "work is
     happening" with no measurable amount; this communicates a known amount
     of measurable progress. No JavaScript, no transition on the fill's
     width - there is nothing in v1 that ever changes it after render, so a
     transition would be dead code waiting for a future animated/JS-driven
     version rather than something this static component actually needs.

     `value` is clamped to 0-100 before it touches either the displayed
     percentage or the fill width, so the two can never disagree and an
     out-of-range input (negative, or over 100) can never produce a fill
     wider than its track or a nonsensical `aria-valuenow`.

     Accessibility follows the ARIA APG progressbar pattern: `role`,
     `aria-valuemin/max/now` on the element that IS the bar (the track, not
     the fill - the fill is a plain, unlabeled decorative child). When a
     `label` is supplied it doubles as the bar's accessible name via
     `aria-label`, since there's no natural id to hang `aria-labelledby` off
     without inventing one - the same "no ARIA the component doesn't need"
     restraint badge.jte and alert.jte both already apply. An unlabeled bar
     is left with no accessible name at all, matching card.jte's own refusal
     to add a role/label the surrounding page hasn't actually asked for. --%>
!{ProgressSize resolvedSize = ProgressSize.from(size);}
!{int clampedValue = Math.max(0, Math.min(100, value));}
!{boolean hasLabel = !label.isEmpty();}
!{boolean hasHeader = hasLabel || showPercentage;}
!{String ariaLabelAttr = hasLabel ? label : null;}
!{String headerJustify = hasLabel ? "justify-between" : "justify-end";}

<div class="w-full" data-progress data-progress-size="${resolvedSize.name().toLowerCase()}">
    @if (hasHeader)
        <div class="mb-1.5 flex flex-wrap items-center gap-x-3 gap-y-1 text-sm ${headerJustify}">
            @if (hasLabel)
                <span class="font-medium text-slate-700 dark:text-slate-300">${label}</span>
            @endif
            @if (showPercentage)
                <span class="text-slate-500 dark:text-slate-400">${clampedValue}%</span>
            @endif
        </div>
    @endif

    <div
            role="progressbar"
            aria-valuemin="0"
            aria-valuemax="100"
            aria-valuenow="${clampedValue}"
            aria-label="${ariaLabelAttr}"
            class="${ProgressStyles.track(resolvedSize)}"
    >
        <div class="${ProgressStyles.fill()}" style="width: ${clampedValue}%"></div>
    </div>
</div>
