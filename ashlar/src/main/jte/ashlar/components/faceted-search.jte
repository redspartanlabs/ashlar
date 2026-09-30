@param gg.jte.Content content

<%-- The container for a set of facets, and deliberately little else.

     It does not own a <form>. The application does - which is what lets the
     application choose GET or POST, the action, whether submitting is manual
     or automatic, and what the selected values mean. The facets below are
     ordinary form controls that participate in whichever form encloses them,
     so the baseline works with no JavaScript at all: check two boxes, submit,
     and the server receives `?status=running&status=failed`.

     It also does not own a submit button, a reset button, a text search
     field, or a results count. Those belong to the page: Form Actions
     already arranges the buttons, Search Input is its own pattern, and a
     result count belongs beside the results. Folding them in here would turn
     a filter UI into a data-filtering framework that every consumer has to
     accept whole.

     No surface of its own either - no border, no background, no shadow. A
     facet list sits in a sidebar on one page, inside a Card on another, and
     inside a Drawer on a phone; giving it a permanent panel would fight all
     three. The consuming page supplies the surface, the same way it does for
     List, Timeline and Key-Value List.

     The only opinion here is vertical rhythm between facets, and `min-w-0`
     so a long option label wraps inside a narrow column instead of pushing
     the layout sideways. --%>
<div data-faceted-search class="flex min-w-0 flex-col gap-5">
    ${content}
</div>
