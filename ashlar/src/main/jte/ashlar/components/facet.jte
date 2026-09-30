@import com.redspartanlabs.ashlar.facet.FacetContext

@param String title
@param String name
@param gg.jte.Content content

<%-- One filtering dimension: a real <fieldset> with a real <legend>, the
     same grouping radio-group.jte already uses and for the same reasons.
     The legend gives the group its accessible name for free, so a screen
     reader announces "Status, Running, checkbox" rather than leaving the
     options floating with no idea what they belong to - and no aria-label
     or aria-labelledby is needed to achieve it.

     `name` is set once here and handed to the options through
     FacetContext (see that class for why): every checkbox in a facet must
     submit under the same field name for the browser to send them as
     repeated values of one dimension, and repeating that string on every
     option is how two of them end up disagreeing.

     There is no `type` parameter. A facet means "a named dimension of the
     data", and v1 expresses that as multi-select checkboxes; a genuine
     single-select or range facet can be designed when something actually
     needs one, against a real requirement rather than a guess.

     The options are wrapped in a plain <div> rather than a list: they are
     form controls, not a list of content, and a <ul> here would have a
     screen reader announce "list, 3 items" over the top of the fieldset
     that already says what this is. --%>
<fieldset data-facet data-facet-name="${name}" class="min-w-0">
    <legend class="mb-1.5 text-sm font-semibold text-slate-900 dark:text-slate-100">${title}</legend>

    <div class="flex flex-col">
        !{FacetContext.enter(name);}
        ${content}
        !{FacetContext.exit();}
    </div>
</fieldset>
