@param String label
@param gg.jte.Content value

<%-- One `<dt>`/`<dd>` pair, wrapped in a `<div>` - valid `<dl>` content per
     the HTML spec (a `<dl>` may contain either bare dt/dd groups, or one or
     more `<div>`s each holding a dt/dd group), and the wrapper is exactly
     what makes a per-entry CSS grid possible: each pair gets its own
     `[label-column]_[value-column]` grid track, so labels line up in a
     classic two-column relationship without key-value-list.jte having to
     flatten every dt/dd into one shared grid and rely on `nth-child` math.

     `value` is `gg.jte.Content`, not `String`, matching list-item.jte's own
     `trailing` slot: a value is exactly as likely to be a Badge, a link, or
     other existing component as it is plain text, and this template has no
     more business understanding it than list-item.jte understands its
     trailing content.

     Stacked by default (`grid-cols-1`), side-by-side from `sm:` up - the
     same mobile-first reflow page-header.jte and page-shell.jte already
     use, so a long label or value always has room to wrap instead of
     forcing the row wider. `min-w-0` on the value keeps a long word/URL
     from pushing the row past the list's own width once wrapped inline. --%>
<div class="grid grid-cols-1 gap-x-6 gap-y-1 sm:grid-cols-[minmax(8rem,12rem)_minmax(0,1fr)] sm:items-baseline">
    <dt class="text-sm font-medium text-slate-500 dark:text-slate-400">${label}</dt>
    <dd class="min-w-0 break-words text-sm text-slate-900 dark:text-slate-100">${value}</dd>
</div>
