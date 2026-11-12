@param String text = ""
@param gg.jte.Content content = null
@param String align = "left"
@param boolean header = false
@param boolean mono = false
@param boolean nowrap = false
@param String sortValue = ""

<%-- One cell. `text` covers the ordinary case (a value the caller already
     has as a string) and `content` takes over when the cell holds something
     richer - a Badge for a status, a link to a record, a Timeline. That
     split is list-item.jte's `label` + `trailing` convention applied here,
     and it is the whole reason this component can do what Data Table's
     string-only `Map<String, String>` cells cannot.

     `header = true` makes the cell a <th scope="row">. The column that
     identifies a row (a job number, an account id) is a heading for that
     row, and marking it as one is what lets a screen reader announce
     "JOB-10482" as context when reading the cells beside it. It is opt-in
     because only the caller knows which column identifies the record.

     `sortValue` is the machine-comparable form of a value whose display
     form does not sort correctly - "Mar 14, 2024" or "2 hours ago" sort
     sensibly by their ISO date, "1,204,981" by its bare number. Given it,
     table.js compares this instead of the visible text.

     An empty value renders an em dash rather than a blank cell, so a row
     with nothing in a column still reads as a row with a known gap - with
     the dash hidden from assistive tech and "No value" said instead,
     because a screen reader announcing "em dash" tells nobody anything.

     `mono` is offered and not applied by default: identifiers and codes
     scan better in a monospace face, prose does not, and the caller is the
     one who knows which a column holds. --%>
!{boolean hasContent = content != null;}
!{boolean isBlank = !hasContent && text.isBlank();}
!{String alignClass = "right".equals(align) ? " ash-table__cell--end" : "center".equals(align) ? " ash-table__cell--center" : "";}
!{String monoClass = mono ? " ash-table__cell--mono" : "";}
!{String wrapClass = nowrap ? " ash-table__cell--nowrap" : "";}
!{String sortAttr = sortValue.isEmpty() ? null : sortValue;}
!{String cellClasses = "ash-table__cell" + alignClass + monoClass + wrapClass;}

@if (header)
    <th scope="row" data-table-cell data-sort-value="${sortAttr}" class="${cellClasses} ash-table__cell--header">
        @if (hasContent)
            ${content}
        @elseif (isBlank)
            <span aria-hidden="true" class="ash-table__blank">&mdash;</span><span class="ash-table__label ash-sr-only">No value</span>
        @else
            ${text}
        @endif
    </th>
@else
    <td data-table-cell data-sort-value="${sortAttr}" class="${cellClasses}">
        @if (hasContent)
            ${content}
        @elseif (isBlank)
            <span aria-hidden="true" class="ash-table__blank">&mdash;</span><span class="ash-table__label ash-sr-only">No value</span>
        @else
            ${text}
        @endif
    </td>
@endif
