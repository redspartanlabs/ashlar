@param String contextPath
@param String index
@param String skin
@param String tagline

<%-- One row in the homepage's "One system. Ten identities." list - plain
     text, not a badge or pill: the composition above it doesn't change
     when a visitor picks one, only its rendered identity does, and a row
     in an index reads as a destination rather than a filter control.

     A plain link to `?skin=<name>`, not a client-side switcher: the
     existing pre-paint bootstrap in `layout/page.jte` already reads that
     query parameter, applies `data-skin` before first paint, and persists
     the choice to `localStorage` - this row only needs to be an honest
     link into a mechanism that already exists, not a new one.

     `tagline` is one word pulled from each theme's own CSS design-rationale
     comments (see theme-<name>.css), not guessed from the skin's name, so
     the copy stays accurate to what each theme actually does. --%>
!{String label = Character.toUpperCase(skin.charAt(0)) + skin.substring(1);}

<a href="${contextPath}/?skin=${skin}#themes" class="group flex items-baseline gap-4 py-3 first:pt-0 last:pb-0 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500">
    <span class="font-mono text-xs text-slate-400 dark:text-slate-600">${index}</span>
    <span class="text-sm font-medium text-slate-900 group-hover:text-blue-700 dark:text-slate-100 dark:group-hover:text-blue-400">${label}</span>
    <span class="text-sm text-slate-500 dark:text-slate-400">${tagline}</span>
</a>
