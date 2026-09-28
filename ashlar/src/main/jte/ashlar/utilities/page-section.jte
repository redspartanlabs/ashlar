@param String title
@param String description = ""
@param gg.jte.Content content

<%-- A structural utility for grouping a page's own content under a
     subsection heading - not a content surface: no border, background, or
     shadow of its own (contrast card.jte, which exists precisely to be a
     surface). A page can freely place a Card, a Table, a Form, or anything
     else inside `content`, and this utility never influences how that
     content looks - only its vertical relationship to the heading above it.

     `title` always renders as `<h2>`, with no `titleLevel` escape hatch:
     this utility exists to BE the one-level-down heading beneath a page's
     own `<h1>` (page-header.jte), not a general-purpose heading wrapper -
     matching that utility's own reasoning for omitting the same parameter.

     No `role="region"`/`aria-labelledby`: a `<section>` immediately
     preceded by its own `<h2>` is already an accessible-name-bearing
     landmark by native HTML semantics in every browser/AT combination that
     matters here, the same reasoning card.jte already gives for omitting
     `role="region"` on itself.

     `content` is a single required `gg.jte.Content`, not `form`/`table`/
     `card`/`body`-style specialized slots: this utility owns only the
     heading-to-content spacing relationship, never the content's own
     shape, so one plain slot is both sufficient and correct - adding more
     would just be this utility guessing at structure it has no business
     knowing about. --%>
!{boolean hasDescription = !description.isEmpty();}

<section class="flex flex-col gap-4">
    <div>
        <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">${title}</h2>
        @if (hasDescription)
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">${description}</p>
        @endif
    </div>

    ${content}
</section>
