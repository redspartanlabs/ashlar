@import java.util.List
@import com.redspartanlabs.ashlar.datatable.DataTableColumn
@import com.redspartanlabs.ashlar.datatable.DataTableRow

@param String caption
@param List<DataTableColumn> columns
@param List<DataTableRow> rows
@param boolean captionVisible = false
@param String rowKey = ""
@param int pageSize = 10
@param boolean selectable = false
@param boolean searchable = true
@param String searchPlaceholder = "Search"
@param String emptyTitle = "No records found"
@param String emptyBody = ""

<%-- The model-driven table: declare columns, hand over rows, and this
     renders them through Table.

     THE SPLIT WITH TABLE. Table is composition-driven - a caller writes the
     rows themselves, so a cell can hold anything. Data Table is
     model-driven - a caller has a list of records and a list of column
     descriptors and does not want to hand-write a row template for them.
     That difference lives entirely here, in JTE. Underneath, this emits
     exactly Table's own markup, so both tables share one DOM contract, one
     set of hooks, one client-side implementation and one set of theme
     rules. Nothing is inherited and nothing is duplicated: this is a loop
     that calls table-column, table-row and table-cell.

     WHAT IT NO LONGER DOES. The previous version shipped the dataset twice
     (once as a JSON island, never as HTML), rebuilt every row in JavaScript
     on every keystroke, and carried View/Edit/Delete buttons and a delete
     dialog that mutated its own copy of the data. All of that is gone: the
     rows below are real server-rendered HTML, and an application that needs
     actions composes them around this component, where they belong.

     EMPTY STATE. There is no `empty` parameter, because there is nothing
     for a caller to tell us - `rows.isEmpty()` is already known here. The
     other empty case, a search that matches nothing, is only knowable at
     runtime and is handled by the shared script. Table keeps its own
     `empty` flag because its rows are opaque content it genuinely cannot
     count; here that flag is simply computed.

     `rowKey` names the column that identifies a row, and it is the only
     notion of identity here. It has no default, because guessing at an "id"
     field is exactly the assumption that made the old selection break on any
     dataset that did not happen to have one.

     Three things follow from it, and nothing follows from column order:

       * that column's cell is the row's <th scope="row">, because a row
         header is a cell that identifies its row - which is a claim only the
         caller can make. An earlier pass inferred it from position instead,
         which quietly asserted that any model's first column identifies its
         rows: a table whose first column happened to be a status or a date
         would have declared "Active" as the label for everything beside it.
       * it names the selection checkbox ("Select row ACC-1001"), for the
         same reason - "Select row Active" is both ambiguous and repeated
         across rows.
       * it keys the selection itself.

     Without it a table simply has no row header, which is valid and common.
     No header is better than a wrong one, and `rowKey` stays useful on its
     own: a read-only table can declare its identity column without turning
     selection on. --%>
!{boolean isEmpty = rows.isEmpty();}
!{boolean hasRowKey = !rowKey.isEmpty();}
!{boolean selectionEnabled = selectable && hasRowKey;}

@template.ashlar.components.table(
caption = caption,
captionVisible = captionVisible,
searchable = searchable,
searchPlaceholder = searchPlaceholder,
selectable = selectionEnabled,
pageSize = pageSize,
empty = isEmpty,
emptyTitle = emptyTitle,
emptyBody = emptyBody,
columns = @`
    @for (DataTableColumn column : columns)
        @template.ashlar.components.table-column(
        label = column.label(),
        sortable = column.sortable(),
        align = column.align().token(),
        type = column.sort().token()
        )
    @endfor
`,
rows = @`
    @for (DataTableRow row : rows)
        <%-- One value does all three jobs: it keys the row, labels its
             checkbox, and marks its cell as the row header below. --%>
        @template.ashlar.components.table-row(
        rowKey = hasRowKey ? row.value(rowKey) : "",
        rowLabel = hasRowKey ? row.value(rowKey) : "",
        cells = @`
            @for (int i = 0; i < columns.size(); i++)
                !{DataTableColumn column = columns.get(i);}
                !{String value = row.value(column.key());}
                !{boolean isRowHeader = hasRowKey && column.key().equals(rowKey);}
                @if (column.isBadge() && !value.isEmpty())
                    @template.ashlar.components.table-cell(
                    content = @`@template.ashlar.components.badge(text = value, variant = column.badgeVariantFor(value))`,
                    sortValue = value,
                    align = column.align().token(),
                    header = isRowHeader,
                    nowrap = true
                    )
                @else
                    @template.ashlar.components.table-cell(
                    text = value,
                    sortValue = row.sortValue(column.key()),
                    align = column.align().token(),
                    mono = column.isMono(),
                    header = isRowHeader,
                    nowrap = column.sort() != com.redspartanlabs.ashlar.datatable.Sort.TEXT || column.isMono()
                    )
                @endif
            @endfor
        `
        )
    @endfor
`
)
