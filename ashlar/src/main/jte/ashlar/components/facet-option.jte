@import dev.redspartanlabs.ashlar.facet.FacetContext

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
     count, and wraps the control in the Checkbox's own wrapper, which carries
     a 1rem margin below it that would space a filter list out like a form. So
     the control is implemented here instead, as a native checkbox tinted with
     `accent-color` (see facet-option.css), which is what keeps the browser
     drawing the check and keeps the two looking like the same library rather
     than two checkbox styles. It does not reuse the ash-choice__input class
     either: that class dims a disabled control on its own and moves it off the
     line, and this row is already dimmed as a whole.

     THE COUNT IS ANNOUNCED, NOT JUST SHOWN. The visible number is
     aria-hidden and a screen reader is given "42 results" instead, because
     a label reading "Running 42" is genuinely ambiguous - 42 could be part
     of the name. The count is rendered exactly as supplied: the application
     owns what it counts and how it is worded, and a component that
     reformatted or recalculated it would be inventing data.

     Disabled state is resolved here rather than with a `has-[:disabled]`
     variant, because whether this option is disabled is already known at
     render time - the server can simply write the right class. --%>
!{boolean hasCount = count != null;}
!{String countLabel = hasCount ? (count == 1 ? "1 result" : count + " results") : "";}
!{String rowClasses = disabled ? "ash-facet-option ash-facet-option--disabled" : "ash-facet-option";}

<label class="${rowClasses}">
    <input
            type="checkbox"
            name="${FacetContext.currentName()}"
            value="${value}"
            checked="${checked}"
            disabled="${disabled}"
            class="ash-facet-option__input"
    >

    <span class="ash-facet-option__label">${label}</span>

    @if (hasCount)
        <span aria-hidden="true" class="ash-facet-option__count">${count}</span>
        <span class="ash-sr-only">${countLabel}</span>
    @endif
</label>
