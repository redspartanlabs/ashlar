@param gg.jte.Content content

<%-- The outer layout boundary for a page's own content - width, horizontal
     padding, and vertical spacing between top-level blocks only, no visual
     surface of its own (no border/background/shadow, contrast card.jte).
     The 72rem width cap, the stepped horizontal padding and the vertical
     rhythm (components/page-shell.css) all match the library's
     own already-established page-layout convention (layout/page.jte's own
     `<main>`), rather than inventing a second one.

     The flex column spaces top-level blocks (a Page Header, then
     each Page Section, then whatever else) apart from one another - it
     does not reach into any one block and restyle it: a Page Section
     supplied as a single child still lays out its own heading/description/
     content exactly as page-section.jte defines, untouched by this
     container's own flex context.

     A plain `<div>`, not `<main>`: the surrounding application page/layout
     already owns the document's one `<main>` landmark - this utility only
     needs to sit inside it, not duplicate it. --%>
<div class="ash-page-shell">
    ${content}
</div>
