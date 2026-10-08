@import dev.redspartanlabs.ashlar.table.TableContext

@param gg.jte.Content cells
@param String rowKey = ""
@param String rowLabel = ""

<%-- One row. Deliberately thin: it exists so a caller composes rows and
     cells the same wrapper/item way the rest of the library composes
     repeated content (list/list-item, tree/tree-item, timeline/
     timeline-item), rather than hand-writing <tr> and hoping the classes
     match.

     The hover tint is the row's only unconditional behaviour. There is no
     click handler and no row actions: this table is for reading, and a row
     that quietly becomes clickable is how a read-oriented table turns into
     a CRUD grid. A caller who needs a row to lead somewhere puts a real
     link in a cell, where it can be seen, tabbed to and announced.

     SELECTION. When the enclosing table was rendered with `selectable`,
     every row emits a leading checkbox cell - asked of TableContext rather
     than of a per-row flag, so the body can never disagree with the header
     about how many columns there are. `rowKey` is the identity the
     selection is tracked by and is supplied by the caller: nothing here
     assumes a row has an `id` field. A selectable table whose row has no
     key still renders the cell, empty, because a missing checkbox is a
     misaligned row. `rowLabel` names the checkbox ("Select row ACC-1001")
     so the control is not announced as an unlabelled checkbox. --%>
!{boolean selectable = TableContext.isSelectable();}
!{boolean hasKey = !rowKey.isEmpty();}
!{String rowKeyAttr = hasKey ? rowKey : null;}
!{String checkboxLabel = rowLabel.isEmpty() ? "Select row" : "Select row " + rowLabel;}

<tr data-table-row data-row-key="${rowKeyAttr}" class="hover:bg-slate-50 dark:hover:bg-slate-800/60">
    @if (selectable)
        <td class="w-10 px-4 py-2.5">
            @if (hasKey)
                <label class="flex items-center">
                    <span class="sr-only">${checkboxLabel}</span>
                    <input
                            type="checkbox"
                            data-table-select-row
                            class="h-4 w-4 cursor-pointer rounded border border-slate-300 bg-white accent-blue-600 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 dark:border-slate-600 dark:bg-slate-800"
                    >
                </label>
            @endif
        </td>
    @endif

    ${cells}
</tr>
