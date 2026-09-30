@param gg.jte.Content content
@param String label = "Filters"

<%-- Arranges an application's quick filters in one compact row above a
     dataset. Layout only: it creates no control, owns no form, knows no
     field names, and never inspects what it was given - the same contract
     form-actions.jte has for a form's buttons.

     WHY THIS EXISTS AND FACETED SEARCH DOESN'T COVER IT. Faceted Search is
     a vertical sidebar of checkbox facets, where the options themselves are
     the filtering UI and every option in a facet submits under one name. A
     `facet` cannot hold a Select, a Date Picker or a Button. This is the
     other pattern: a handful of heterogeneous controls the application
     already has, laid out horizontally near the data they filter. The two
     never compose with each other, so neither can be expressed as the
     other.

     WHAT IT ACTUALLY SOLVES. Every form control in this library is built
     for a stacked form: a block <label>, then a control capped at
     `max-w-sm`, then `mb-6` of trailing rhythm. Drop three of them into a
     plain flex row and you get 24px of dead space under the row and fields
     sized by whatever their placeholder text happens to be. That is not a
     hypothetical - table.jte hand-rolled its own search field rather than
     compose search-input.jte, precisely because the component's stacked-form
     box model does not fit a toolbar. This utility normalises that in one
     place instead of every consuming page re-solving it:

       * `[&>*]:mb-0` drops the stacked-form trailing margin from whatever
         controls were handed over, including the `mb-4` ones (Checkbox,
         Toggle). The container owns the rhythm between filters now, and it
         is `gap-3` - the same gap page-header.jte and form-actions.jte use
         between adjacent controls.
       * `w-full sm:w-48` gives fields one predictable column width instead
         of letting each size itself from its placeholder. Below `sm` that
         becomes one full-width field per line, which is the whole of the
         narrow-screen story: things stack, nothing scrolls sideways. The
         `max-w-sm` inside each control never binds at 12rem, so it stays
         out of the way rather than needing to be overridden.
       * `:not(button)` keeps that width off a Clear/Apply button, which
         should stay its own size, and `items-end` sits it on the same line
         as the field boxes rather than the tops of their labels - the same
         reason it survives a filter label wrapping to two lines.

     NO ACTIONS SLOT. The buttons go in `content`, inline after the last
     filter, which is where the pattern actually puts them. A second slot
     would only exist to push them to the far right, and a filter row is
     read left to right as one sequence - Form Actions is the utility for
     end-aligned buttons, and it is still the right one inside a real form's
     footer.

     NO CHIPS, NO CLEAR BUTTON, NO RESET BEHAVIOUR. All three require
     knowing which filters are active, which means knowing what the filters
     mean. That is the application's, and a utility that starts reading its
     own children is on its way to being a query engine.

     NO SURFACE - no border, background or shadow. A filter row sits bare
     under a page heading on one page, inside a Card on another, and inside
     a Drawer on a phone; faceted-search.jte declines a surface for exactly
     these three cases and this is the same region. It also means there is
     nothing here for a theme to restyle: every visible pixel belongs to the
     composed controls, which all four themes already cover.

     A real <fieldset> with a real <legend>, the grouping facet.jte and
     radio-group.jte already use. The legend names the region for a screen
     reader with no aria-label or role="group" needed - and it is `sr-only`
     because a row of controls labelled "Status", "Account type" and "Owner"
     sitting under a page heading is already self-evident on screen, the way
     table.jte's <caption> is. A page that wants the word "Filters" visible
     has Page Section for that. --%>
<fieldset class="min-w-0 border-0 p-0">
    <legend class="sr-only">${label}</legend>

    <div class="flex flex-wrap items-end gap-3 [&>*]:mb-0 [&>*:not(button)]:w-full sm:[&>*:not(button)]:w-48">
        ${content}
    </div>
</fieldset>
