@import com.redspartanlabs.ashlar.accordion.AccordionExpansion
@import com.redspartanlabs.ashlar.AshlarAssets

@param String id
@param gg.jte.Content items

<%-- The wrapper only establishes the bordered/divided surface and the JS
     hook - each section's button+panel pair is rendered by a separate
     @template.ashlar.components.accordion-item(...) call, composed directly into
     `items` the same way card.jte's own `body` accepts arbitrary composed
     content. This is a different split than tabs.jte/tab-panel.jte's: a
     Tab's panel is a detached sibling placed elsewhere on the page (tabs.js
     finds it via a shared id-prefix query against the whole document,
     because Tab.java - see its own doc comment - can't carry JTE content in
     a Java builder chain, so the tablist and the panels can't be rendered
     from one shared list either). An accordion item has no such split: it's
     a single self-contained @template call producing one button+panel pair,
     nested directly inside this wrapper's own `items` content - so
     accordion.js only ever needs to search *within* `root`, not the whole
     document, and two Accordion instances can never see each other's
     buttons no matter what ids or classes they happen to share.

     No `role`/`aria-label` on the wrapper: there is no ARIA "accordion"
     role, and a plain grouping `<div>` has no accessible-naming role for
     `aria-label` to attach to - adding one here would be a parameter that
     silently does nothing, which is worse than not offering it. Each
     item's own button+panel pair (accordion-item.jte) carries all the real
     accessibility relationships.

     The reset() here, immediately before `items` is dereferenced, is what
     scopes AccordionExpansion.resolve() (see its own doc comment) to just
     THIS accordion's items - so if more than one asks for
     `expanded = true`, only the first one actually renders open, and a
     second Accordion instance later on the same page starts its own count
     fresh rather than inheriting this one's claimed state. --%>
!{AccordionExpansion.reset();}

<div id="${id}" data-accordion class="divide-y divide-slate-200 overflow-hidden rounded-lg border border-slate-200 dark:divide-slate-700 dark:border-slate-700">
    ${items}
</div>

<script type="module" src="${AshlarAssets.js("accordion.js")}"></script>
