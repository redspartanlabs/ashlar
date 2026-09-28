@param gg.jte.Content items

<%-- The wrapper only establishes the landmark and the ordered list - each
     crumb is rendered by a separate @template.ashlar.components.breadcrumb-item(...)
     call, composed directly into `items`, the same wrapper/item split used by
     accordion.jte and tabs.jte. Unlike those two, a breadcrumb trail has no
     interactive coordination to do (nothing here opens, closes, or switches
     anything), so there is no JS file and no `data-*` hook for one to find -
     this is the library's first purely static composition.

     `aria-label="Breadcrumb"` on the <nav> is what makes this a named
     navigation landmark distinct from the page's other <nav> elements (the
     header, say) - the W3C APG breadcrumb pattern names it exactly this way,
     so it's a fixed string, not a parameter: a caller who wants a different
     landmark name is asking for a different landmark, not a styling choice.

     <ol>, not <ul>: a breadcrumb trail is an ordered hierarchy (Home >
     Products > Widgets is not the same trail as Widgets > Products > Home),
     and <ol> communicates that ordering to assistive tech for free - no
     `role="list"` or numbering is added on top of it because the native
     element already provides both. No extra role on the <nav> either: `nav`
     is already a native landmark. --%>

<nav aria-label="Breadcrumb" data-breadcrumbs>
    <ol class="flex flex-wrap items-center gap-x-1.5 gap-y-1 text-sm">
        ${items}
    </ol>
</nav>
