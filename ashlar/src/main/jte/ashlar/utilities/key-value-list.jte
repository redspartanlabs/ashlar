@param gg.jte.Content content

<%-- A semantic `<dl>` for labeled record data - record details, metadata,
     configuration summaries - styled consistently but with no surface of
     its own (no border/background/shadow, contrast card.jte; a caller
     wanting a surface places this inside a Card the same way
     page-section.jte's own content does).

     `content` is a single `gg.jte.Content` slot, not a Java collection: the
     caller supplies real `key-value.jte` entries (or hand-written dt/dd
     pairs), so a value can be plain text, a link, a Badge, or any other
     existing component without this template ever needing to know what it
     is - the same reasoning form-actions.jte gives for not inspecting its
     own children.

     No `role="list"`/`aria-*`: `<dl>` already expresses "a list of
     term/description groups" natively, the same reasoning list.jte gives
     for omitting a role on `<ul>`/`<ol>`.

     `flex flex-col gap-3` spaces entries apart without imposing anything on
     an entry's own internal layout - each entry (typically a
     `key-value.jte` div) remains free to lay out its own label/value
     columns exactly as it defines them. --%>
<dl class="flex flex-col gap-3">
    ${content}
</dl>
