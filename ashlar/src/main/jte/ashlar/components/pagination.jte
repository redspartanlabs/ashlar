@param gg.jte.Content items

<%-- The wrapper only provides the landmark and the list - each entry is a
     separate @template.ashlar.components.pagination-item(...) call composed into
     `items`, the same wrapper/item split as accordion.jte and
     breadcrumbs.jte. Unlike those two, there's no JS file here at all:
     navigation happens through real `<a href>`s the browser handles on its
     own, so there's nothing for a script to coordinate and no `data-*` hook
     for one to find.

     `aria-label="Pagination"` is a fixed string for the same reason
     breadcrumbs.jte's `aria-label="Breadcrumb"` is: the WAI-ARIA APG names
     this landmark exactly this way, so a caller wanting a different name is
     asking for a different landmark, not a styling choice.

     `<ul>`, not `<ol>`: breadcrumbs.jte uses `<ol>` because a breadcrumb
     trail's 1st/2nd/3rd position *is* the hierarchy being communicated.
     Here the page numbers already carry their own order in their own visible
     text ("1", "2", "3") - `<ol>`'s implicit numbering would just be
     redundant noise read out ahead of a caller-supplied "Previous" control
     that isn't a numbered page at all. The APG's own pagination example
     uses `<ul>` for exactly this reason. --%>

<nav aria-label="Pagination" data-pagination>
    <ul class="flex flex-wrap items-center gap-1.5 text-sm">
        ${items}
    </ul>
</nav>
