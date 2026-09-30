@param gg.jte.Content content

<%-- The outer layout boundary for a page's own content - width, horizontal
     padding, and vertical spacing between top-level blocks only, no visual
     surface of its own (no border/background/shadow, contrast card.jte).
     `max-w-6xl`, the `px-4`/`sm:px-6`/`lg:px-8` horizontal-padding steps,
     and the `flex flex-col gap-10` vertical rhythm all match the library's
     own already-established page-layout convention (layout/page.jte's own
     `<main>`), rather than inventing a second one.

     `flex flex-col gap-10` spaces top-level blocks (a Page Header, then
     each Page Section, then whatever else) apart from one another - it
     does not reach into any one block and restyle it: a Page Section
     supplied as a single child still lays out its own heading/description/
     content exactly as page-section.jte defines, untouched by this
     container's own flex context.

     A plain `<div>`, not `<main>`: the surrounding application page/layout
     already owns the document's one `<main>` landmark - this utility only
     needs to sit inside it, not duplicate it. --%>
<div class="mx-auto flex w-full max-w-6xl flex-col gap-10 px-4 py-8 sm:px-6 lg:px-8">
    ${content}
</div>
