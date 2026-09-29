@import com.redspartanlabs.ashlar.tree.TreeContext
@import com.redspartanlabs.ashlar.AshlarAssets

@param gg.jte.Content items

<%-- The wrapper only establishes the `role="tree"` landmark and resets the
     depth counter each tree-item.jte call reads to compute its own
     `aria-level` - the same wrapper/item split as accordion.jte and
     list.jte, and the same reason: each item is a separate, independently
     composed @template.ashlar.components.tree-item(...) call, opaque to its
     siblings and ancestors, so any cross-item bookkeeping has to happen
     through a small shared helper rather than through parameters.

     `role="tree"` on a real `<ul>`: the WAI-ARIA APG tree pattern doesn't
     require a specific host element, but a `<ul>` degrades to a plain
     (if flat) list if CSS or the role were ever stripped, the same
     reasoning breadcrumbs.jte and pagination.jte already use for choosing
     real list elements over a bare `<div>`.

     No `aria-label` here: unlike breadcrumbs.jte's "Breadcrumb" or
     pagination.jte's "Pagination" (both fixed names the APG assigns to
     those specific patterns), a generic tree has no such name to borrow -
     the same reasoning list.jte's own doc comment gives for leaving its
     wrapper unlabelled. A caller who needs this particular tree named can
     supply `aria-labelledby` pointing at their own heading, same as any
     other landmark-ish structure on the page.

     tree.js does the same per-instance root-scoping every interactive
     component in this library already uses (accordion.js, dropdown-menu.js):
     it only ever queries inside `root`, so multiple Tree View instances on
     one page can never see or interfere with each other's items. --%>
!{TreeContext.reset();}

<ul role="tree" data-tree class="flex flex-col gap-0.5 text-sm">
    ${items}
</ul>

<script type="module" src="${AshlarAssets.js("tree.js")}"></script>
