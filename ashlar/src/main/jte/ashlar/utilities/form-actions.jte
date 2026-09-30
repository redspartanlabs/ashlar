@param gg.jte.Content content

<%-- A layout-only utility for arranging a form's own action controls -
     spacing, alignment, and wrapping, nothing else (no surface of its own,
     contrast card.jte). It never creates a button, never inspects what it
     was given, and never reorders it: `${content}` renders in exactly the
     DOM order the caller supplied, since action order can itself carry
     meaning the utility has no business changing.

     A plain `<div>`, not `<form>`/`<fieldset>`: this utility doesn't own
     submission or field grouping, only the visual arrangement of whatever
     controls the caller already built - the same reasoning page-shell.jte
     gives for not manufacturing semantic meaning it doesn't actually have.

     No `role="group"`: there is no concrete accessibility requirement this
     satisfies that plain DOM order and each child's own native semantics
     don't already cover.

     `flex flex-wrap items-center justify-end gap-3` end-aligns actions on
     a normal-width form (the common "[Cancel] [Save Changes]" case) while
     still letting them wrap onto their own line at narrow widths instead
     of overflowing - `gap-3` matches page-header.jte's own established
     spacing between multiple actions. --%>
<div class="flex flex-wrap items-center justify-end gap-3">
    ${content}
</div>
