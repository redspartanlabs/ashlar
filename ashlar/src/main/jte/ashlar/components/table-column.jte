@param String label
@param boolean sortable = false
@param String align = "left"
@param String type = "text"
@param String width = ""

<%-- One column heading. A real <th scope="col">, so every cell beneath it is
     associated with it by the table's own semantics - no ARIA needed to say
     what HTML already says.

     `sortable` renders a real <button> inside the heading rather than a
     click handler on the <th>: that is what makes the control reachable by
     Tab, operable with Enter and Space, and announced as a button, all for
     free. `aria-sort` lives on the <th> (where the spec puts it) and starts
     at "none"; table.js moves it to "ascending"/"descending" as the sort
     changes, so assistive tech is told which column is ordering the table
     and which way.

     `type` is a sorting hint, not a formatter - the component never
     reformats a caller's value. It tells table.js how to compare: "text"
     collates, "number" compares numerically, "date" compares as dates. A
     cell can override the compared value with its own `sortValue` when what
     is displayed is not what should be ordered ("2 hours ago", "Mar 14,
     2024").

     `align` is declared here and again on the column's cells, because that
     is how HTML tables actually work - CSS cannot set text alignment from a
     <col>, and inferring it in JavaScript would mean a table that sits
     misaligned until a script runs. --%>
!{String alignClass = "right".equals(align) ? " ash-table__column--end" : "center".equals(align) ? " ash-table__column--center" : "";}
!{String widthClass = width.isEmpty() ? "" : " " + width;}
!{String ariaSort = sortable ? "none" : null;}
!{String justifyClass = "right".equals(align) ? " ash-table__sort--end" : "center".equals(align) ? " ash-table__sort--center" : "";}

<th
        scope="col"
        data-table-column
        data-table-type="${type}"
        aria-sort="${ariaSort}"
        class="ash-table__column${alignClass}${widthClass}"
>
    @if (sortable)
        <%-- `text-transform: inherit` on the sort button (table-column.css) is
             load-bearing, not decoration. A <button> does not inherit its
             text-transform, which silently cuts it off from its own heading:
             under a theme that sets header casing (Ledger uppercases table
             headings) a sortable column rendered in sentence case while the
             non-sortable column beside it rendered in caps - the same table
             disagreeing with itself. Restoring inheritance lets every theme
             style `thead th` once and have it reach the label, without the
             component hard-coding any theme's typography. --%>
        <button
                type="button"
                data-table-sort
                class="ash-table__sort${justifyClass}"
        >
            <span>${label}</span>
            <%-- The neutral glyph marks the column as sortable before anyone
                 has sorted it; table.js swaps it for an arrow once this
                 column is the active sort. Matching data-table.jte's own
                 indicator keeps the two tables reading the same way. --%>
            <span data-table-sort-icon aria-hidden="true" class="ash-table__sort-icon">&#8645;</span>
        </button>
    @else
        ${label}
    @endif
</th>
