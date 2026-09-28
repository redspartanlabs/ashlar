@param gg.jte.Content items
@param boolean ordered = false

<%-- The wrapper only establishes the bordered/divided surface - each row is
     a separate @template.ashlar.components.list-item(...) call composed into
     `items`, the same wrapper/item split as accordion.jte, breadcrumbs.jte
     and pagination.jte. Purely static composition: nothing here opens,
     closes, filters, sorts or paginates, so there is no JS file and no
     `data-*` hook for one to find - that behavior belongs to Data Table, not
     to a plain list of items.

     `ordered` picks between `<ul>` and `<ol>` rather than exposing a generic
     "as" or "tag" escape hatch: those are the only two native list elements,
     and the choice between them is a real semantic one (does position 2
     mean "second" to a screen reader, or just "next"), not a styling
     variant - the same reasoning breadcrumbs.jte's own doc comment gives for
     choosing `<ol>` there and pagination.jte's for choosing `<ul>`. Most
     lists aren't ordered, so `false` is the default.

     No `role`/`aria-label` on the wrapper: `<ul>`/`<ol>` already communicate
     "a list" natively, and unlike breadcrumbs.jte's "Breadcrumb" or
     pagination.jte's "Pagination" (both fixed names the WAI-ARIA APG
     assigns to those specific patterns), a generic List has no APG name to
     borrow - adding one here would be a parameter that silently does
     nothing for most callers, the same reason card.jte refuses
     `role="region"`. A caller who needs this particular list named or
     labelled can wrap it in their own landmark. --%>
@if (ordered)
    <ol class="divide-y divide-slate-200 overflow-hidden rounded-lg border border-slate-200 dark:divide-slate-700 dark:border-slate-700">
        ${items}
    </ol>
@else
    <ul class="divide-y divide-slate-200 overflow-hidden rounded-lg border border-slate-200 dark:divide-slate-700 dark:border-slate-700">
        ${items}
    </ul>
@endif
