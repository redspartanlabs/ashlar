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

     Desktop-vs-narrow reflow (components/page-header.css) is a column that
     becomes a row from 40rem, with no fixed widths on either side: the
     heading column can wrap onto as many lines as a long title/description
     need, and the actions group wraps, so several actions wrap onto their own
     line rather than forcing horizontal overflow. --%>
!{boolean hasDescription = !description.isEmpty();}
!{boolean hasActions = actions != null;}

<header class="ash-page-header">
    <div class="ash-page-header__heading">
        <h1 class="ash-page-header__title">${title}</h1>
        @if (hasDescription)
            <p class="ash-page-header__description">${description}</p>
        @endif
    </div>

    @if (hasActions)
        <div class="ash-page-header__actions">
            ${actions}
        </div>
    @endif
</header>
