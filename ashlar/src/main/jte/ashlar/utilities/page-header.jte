@param String title
@param String description = ""
@param gg.jte.Content actions = null

<%-- A page-level layout utility, not a content surface: no border/background
     of its own (contrast card.jte, which exists precisely to be one) - it
     only arranges a page's own heading, and the surrounding page stays
     responsible for whatever sits above or below it.

     `title` always renders as `<h1>`, with no `titleLevel` escape hatch:
     this utility exists to BE a page's one primary heading, not a
     general-purpose heading wrapper, so there's exactly one correct level
     for it to render at.

     `actions` is `gg.jte.Content`, not `primaryAction`/`secondaryAction`
     parameters: matching card.jte's own `footer` slot, this utility owns
     layout only - which actions exist, how many, and what they do is the
     calling page's decision, not this utility's.

     Desktop-vs-narrow reflow is `flex flex-col sm:flex-row` with no fixed
     widths on either side: the heading column can wrap onto as many lines
     as a long title/description need, and `flex-wrap` on the actions group
     lets several actions wrap onto their own line rather than forcing
     horizontal overflow. --%>
!{boolean hasDescription = !description.isEmpty();}
!{boolean hasActions = actions != null;}

<header class="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
    <div class="min-w-0">
        <h1 class="text-xl font-semibold text-slate-900 dark:text-slate-100">${title}</h1>
        @if (hasDescription)
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">${description}</p>
        @endif
    </div>

    @if (hasActions)
        <div class="flex flex-wrap items-center gap-3">
            ${actions}
        </div>
    @endif
</header>
