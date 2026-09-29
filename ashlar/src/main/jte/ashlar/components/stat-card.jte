@param String label
@param String value
@param String description = ""
@param gg.jte.Content change = null

<%-- A single metric, in its own small bordered surface - reusing card.jte's
     own outer treatment (rounded-xl border, matching bg/border colors) for
     visual consistency, but not card.jte's title/body/footer slots: a stat
     card's actual hierarchy (a small label, then a value that dominates
     everything else) doesn't fit a bordered title bar and a separate body
     region, and forcing `label` through card.jte's `title` would render it
     at title weight, not the muted, secondary weight a metric's name
     actually needs here. Unlike list.jte/timeline.jte/tree.jte - each a
     single wrapper meant to hold many rows, and meant to be composed
     *inside* an existing Card - a dashboard is normally a grid of several
     independent stat cards side by side, so this one owns its own surface
     the way Card itself does, rather than expecting a caller to supply one.

     `<dl>`/`<dt>`/`<dd>`, not a generic pair of `<div>`s: a metric is
     exactly the "term paired with its value(s)" relationship `<dl>` exists
     for, so the label/value relationship the component contract asks for
     comes from native semantics instead of any ARIA - no
     `aria-describedby` or similar is needed because `<dt>`/`<dd>` already
     says it. `description`, when present, is a second `<dd>` for the same
     `<dt>` (a `<dt>` having more than one `<dd>` is valid HTML), not a
     child of the value's own `<dd>` - it describes the metric as a whole,
     not the value string specifically.

     `label` stays a plain `<dt>`, not a heading: the same reasoning
     list-item.jte and timeline-item.jte already give for their own labels -
     a dashboard showing several of these shouldn't turn heading navigation
     into a list of a dozen near-identical stops.

     `value` is a plain `String`, not a number: this component does no
     formatting, parsing, or calculation of its own (see
     `change`'s own reasoning below) - the caller already has a real number
     and has already decided how it should read ("12,481", "$48.2K",
     "99.9%", or a plain word like "Active"), and this only ever displays
     exactly that string.

     `change` is `gg.jte.Content`, not a string-plus-variant pair: a
     directional change ("+12% vs last month") is exactly the kind of small
     colored indicator badge.jte already exists to render, and which
     direction is actually *good* depends entirely on what the metric is
     (rising revenue is good, rising response time isn't) - a fact only the
     caller knows. Composing `@template.ashlar.components.badge(...)` here reuses
     that existing color vocabulary instead of this component inventing a
     second one, or guessing at meaning it has no way to know. --%>
!{boolean hasDescription = !description.isEmpty();}
!{boolean hasChange = change != null;}

<div class="rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-700 dark:bg-slate-900" data-stat-card>
    <dl>
        <dt class="text-sm font-medium text-slate-500 dark:text-slate-400">${label}</dt>
        <dd class="mt-2 flex flex-wrap items-baseline gap-x-2 gap-y-1">
            <span class="break-words text-3xl font-semibold tracking-tight text-slate-900 dark:text-slate-100">${value}</span>
            @if (hasChange)
                <span class="shrink-0">${change}</span>
            @endif
        </dd>
        @if (hasDescription)
            <dd class="mt-1 break-words text-sm text-slate-500 dark:text-slate-400">${description}</dd>
        @endif
    </dl>
</div>
