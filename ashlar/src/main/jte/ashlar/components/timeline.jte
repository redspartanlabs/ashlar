@param gg.jte.Content items

<%-- The wrapper only establishes the ordered-list landmark - each entry is a
     separate @template.ashlar.components.timeline-item(...) call composed into
     `items`, the same wrapper/item split as stepper.jte and list.jte. Zero
     JavaScript, for the same reason as Stepper: nothing here ever changes
     after the server renders it once - the connector between entries is
     drawn per-item in pure CSS (see timeline-item.jte), not measured or
     positioned by a script.

     `<ol>`, not `<ul>`: a timeline's entries are chronological - the first
     item genuinely happened before the second - which is exactly the
     ordering breadcrumbs.jte's own doc comment describes for choosing
     `<ol>` there, not the "already-numbered, `<ol>` would just be
     redundant" case pagination.jte's doc comment describes for choosing
     `<ul>`. A timeline entry doesn't carry its own visible ordinal the way
     a page number does, so the list's own implicit numbering is real,
     useful information here.

     No `role`/`aria-label` on the wrapper: same reasoning as list.jte's and
     stepper.jte's own wrappers - there's no APG-assigned landmark name for
     a generic timeline to borrow, and a caller who needs this one labelled
     can supply `aria-labelledby` pointing at their own heading. --%>

<ol data-timeline class="flex flex-col">
    ${items}
</ol>
