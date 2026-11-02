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

     Stacked by default, side-by-side from 40rem up (components/key-value.css) -
     the same mobile-first reflow page-header.jte and page-shell.jte already
     use, so a long label or value always has room to wrap instead of
     forcing the row wider. The value may shrink below its content, which keeps
     a long word/URL from pushing the row past the list's own width once
     wrapped inline. --%>
<div class="ash-key-value">
    <dt class="ash-key-value__label">${label}</dt>
    <dd class="ash-key-value__value">${value}</dd>
</div>
