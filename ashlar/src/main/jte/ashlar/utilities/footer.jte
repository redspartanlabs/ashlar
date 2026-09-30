@param gg.jte.Content content

<%-- The page's closing landmark - a `<footer>` with the library's own
     established width/padding rhythm (the same `mx-auto max-w-6xl px-4`
     layout/page.jte already applies to the header bar and `<main>`) and a
     top rule separating it from whatever content precedes it, so a caller
     never has to re-derive that measurement to keep a page's edges aligned.

     A layout utility, not a content surface - no background of its own
     beyond the page's own (contrast card.jte, which exists precisely to be
     a surface), matching page-shell.jte's and page-header.jte's own
     reasoning: this arranges where a page's closing content sits, and the
     content itself - copyright text, a link list, a status line - is
     entirely the caller's, composed the same free-form way form-actions.jte
     and page-section.jte already accept arbitrary content.

     `text-sm text-slate-500` is applied here rather than left to the
     caller, because a footer's default register - present, but quieter
     than the page above it - is true of nearly every real footer this will
     ever hold; a caller with genuinely different content (a heading, a
     prominent CTA) can still override color/size on what they compose in,
     since these are the ordinary inherited text defaults, not classes
     stamped onto the caller's own markup. --%>
<footer class="border-t border-slate-200 py-8 text-sm text-slate-500 dark:border-slate-700 dark:text-slate-400">
    <div class="mx-auto max-w-6xl px-4">
        ${content}
    </div>
</footer>
