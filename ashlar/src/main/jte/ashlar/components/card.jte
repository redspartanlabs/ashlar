@param String title = ""
@param gg.jte.Content body = null
@param gg.jte.Content footer = null

<%-- A content surface, not a layout primitive: border/radius/background
     only - no width, margin, or grid placement, so the surrounding page
     stays entirely responsible for where and how wide this sits.

     No `role="region"`: a generic reusable Card used many times on one
     page (a dashboard of a dozen cards, say) would turn every one of them
     into a landmark, which defeats the point of landmarks as a short list
     of a page's major regions. A page that genuinely needs one particular
     Card to be a landmark can add that itself at the call site.

     Unlike modal.jte's shadow-xl (an overlay floating above the page, which
     needs a shadow to read as detached from it), Card sits in normal
     document flow next to other content and only needs a border to read as
     a distinct surface - a shadow here would suggest floating that isn't
     happening.

     `title` is a plain string, not richer content, and always renders as an
     <h3> when present - matching modal.jte's own title param exactly. A
     page assembling several Cards under its own h1/h2 structure gets a
     consistent one-level-down heading without having to think about it.

     `data-card`, `data-card-header` and `data-card-footer` are the hooks a skin selects (a band behind the header, a stronger rule above
     the footer). They are the contract; the ash-card classes are the styling and a skin should not depend on them. --%>
!{boolean hasTitle = !title.isEmpty();}

<div class="ash-card" data-card>
    @if (hasTitle)
        <div class="ash-card__header" data-card-header>
            <h3 class="ash-card__title">${title}</h3>
        </div>
    @endif

    @if (body != null)
        <div class="ash-card__body">
            ${body}
        </div>
    @endif

    @if (footer != null)
        <div class="ash-card__footer" data-card-footer>
            ${footer}
        </div>
    @endif
</div>
