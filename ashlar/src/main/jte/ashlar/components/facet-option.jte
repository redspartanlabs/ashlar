@import com.redspartanlabs.ashlar.facet.FacetContext

@param String label
@param String value
@param Integer count = null
@param boolean checked = false
@param boolean disabled = false

<%-- One selectable value. A native checkbox wrapped in its own <label>, so
     the whole row is the hit target and no id has to be invented to tie the
     two together.

     WHY NOT COMPOSE checkbox.jte. It was the first thing tried. Its id
     defaults to its `name`, and every option in a facet deliberately shares
     one name - so a three-option facet would render three controls with the
     same id and three labels pointing at the first one. Working around that
     means asking every caller for a unique id per option, which is exactly
     the repetition this API is trying to avoid. It also has nowhere to put a
     count, and carries `mb-4` form-field spacing that would space a filter
     list out like a form. So the control is implemented here instead, using
     checkbox.jte's own classes for the box itself - `accent-*` so the
     browser keeps drawing the check, just tinted - which is what keeps the
     two looking like the same library rather than two checkbox styles.

     THE COUNT IS ANNOUNCED, NOT JUST SHOWN. The visible number is
     aria-hidden and a screen reader is given "42 results" instead, because
     a label reading "Running 42" is genuinely ambiguous - 42 could be part
     of the name. The count is rendered exactly as supplied: the application
     owns what it counts and how it is worded, and a component that
     reformatted or recalculated it would be inventing data.

     Disabled state is resolved here rather than with a `has-[:disabled]`
     variant, because whether this option is disabled is already known at
     render time - the server can simply write the right classes. --%>
!{boolean hasCount = count != null;}
!{String countLabel = hasCount ? (count == 1 ? "1 result" : count + " results") : "";}
!{String rowState = disabled
        ? "cursor-not-allowed opacity-60"
        : "cursor-pointer hover:bg-slate-100 dark:hover:bg-slate-800";}
!{String boxCursor = disabled ? "" : " cursor-pointer";}

<label class="flex items-center gap-2.5 rounded-md px-2 py-1.5 text-sm ${rowState}">
    <input
            type="checkbox"
            name="${FacetContext.currentName()}"
            value="${value}"
            checked="${checked}"
            disabled="${disabled}"
            class="h-4 w-4 shrink-0 rounded border border-slate-300 bg-white accent-blue-600 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 disabled:cursor-not-allowed dark:border-slate-600 dark:bg-slate-800${boxCursor}"
    >

    <span class="min-w-0 flex-1 break-words text-slate-700 dark:text-slate-300">${label}</span>

    @if (hasCount)
        <span aria-hidden="true" class="shrink-0 text-xs tabular-nums text-slate-500 dark:text-slate-400">${count}</span>
        <span class="sr-only">${countLabel}</span>
    @endif
</label>
