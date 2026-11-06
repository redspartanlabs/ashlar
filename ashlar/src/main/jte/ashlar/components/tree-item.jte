@import dev.redspartanlabs.ashlar.tree.TreeContext
@import dev.redspartanlabs.ashlar.icon.Icon

@param String label
@param String href = null
@param Icon icon = null
@param boolean expanded = false
@param gg.jte.Content children = null

<%-- One node. `children` being present or absent is the entire leaf/parent
     distinction - a leaf gets no expand control at all, per the component's
     own contract, rather than a disabled or decorative one.

     `role="treeitem"` and its state (`aria-expanded`, `aria-level`) live on
     this `<li>`, not on the toggle button inside it: per the WAI-ARIA APG
     tree pattern, a treeitem IS the row plus everything nested under it -
     the button is only ever a mouse/touch convenience for the exact same
     toggle a keyboard user drives from the treeitem itself (see tree.js).
     Giving the button its own Tab stop would mean two different ways to
     reach the same control depending on input device, which is what the
     APG's single-tab-stop-per-widget model exists to avoid - so the button
     carries `tabindex="-1"` and `aria-hidden="true"`, reachable by a real
     click or tap but invisible to Tab and to a screen reader's linear
     reading, where it would otherwise announce the exact same "collapsed"
     state the treeitem itself already does.

     No `aria-posinset`/`aria-setsize`: see TreeContext's own doc comment -
     this renders the complete hierarchy server-side, and per the ARIA spec
     assistive tech computes both by counting when the full set is already
     in the DOM.

     `aria-level` comes from `TreeContext.currentLevel()`, read once before
     `children` is dereferenced - visual indentation, by contrast, needs no
     server-side counting at all: the nested `<ul role="group">` carries its
     own fixed left padding, and ordinary box nesting makes that compound
     automatically as the tree gets deeper.

     The group's open/closed animation reuses accordion-item.jte's own
     `grid-template-rows: 0fr` → `1fr` technique, including the same
     `inert` on the collapsed state and for the same reason: a `0fr` row,
     unlike `hidden`, doesn't remove its content from the tab order or the
     accessibility tree by itself, and a collapsed branch's whole nested
     subtree needs that removed, not just its own direct row. The 0fr/1fr
     value is not rendered at all: tree-item.css derives it from the panel's
     `inert` attribute (`0fr` while inert, `1fr` otherwise), and the chevron
     turn from this `<li>`'s own `aria-expanded="true"` through child
     combinators only, so the two attributes are the only state there is, no
     inline style or state class can disagree with them, and a parent's state
     never reaches the items nested under it.

     Unlike accordion-item.jte's single panel, this one grid item has to
     hold an arbitrary NUMBER of nested `<li>` siblings (however many
     `${children}` composes), not one fixed block of content - and the
     0fr/1fr technique only animates correctly when the grid track has
     exactly one grid item. A `<div>` sitting directly inside the `<ul
     role="group">` to gather those siblings into one item would be
     non-conforming markup (a `<ul>`'s children are only ever `<li>`), so the
     grid/clip/inert layer wraps the whole `<ul>` from the *outside*
     instead: `<div grid+inert><div clip><ul
     role="group">${children}</ul></div></div>`. That keeps the real list
     nesting intact at every depth (so the tree still degrades to a plain,
     readable nested list if CSS or JS were ever stripped, the same
     reasoning tree.jte's own doc comment gives for its root `<ul>`) while
     still giving the grid exactly one item to size itself against.

     The row's focus ring is driven by `li:focus-visible > row`, a direct-child
     selector in tree-item.css, rather than an ancestor-matching pattern:
     that would match focus on ANY ancestor carrying the class, and since
     every treeitem at every depth carries the same class, a ring on an
     expanded parent would light up every descendant row nested under it
     too. `li:focus-visible > row` only ever matches this row's own
     immediate parent, so nesting doesn't leak the ring onto content that
     isn't actually focused.

     A row with an `href` renders its label as a real `<a>`; a row that also
     has `children` still gets the same link, entirely independent of its
     expanded state. Clicking anywhere else on the row does nothing beyond
     that - only the button toggles expansion - which is what keeps a row
     that is simultaneously a link and a parent from having two conflicting
     meanings for the same click. --%>
!{int level = TreeContext.currentLevel();}
!{boolean hasChildren = children != null;}
!{boolean isLink = href != null && !href.isEmpty();}
!{boolean hasIcon = icon != null;}
!{String ariaExpandedAttr = hasChildren ? String.valueOf(expanded) : null;}

<li
        role="treeitem"
        aria-expanded="${ariaExpandedAttr}"
        aria-level="${level}"
        tabindex="-1"
        data-tree-item
        class="ash-tree__item"
>
    <div class="ash-tree__row">
        @if (hasChildren)
            <button
                    type="button"
                    tabindex="-1"
                    aria-hidden="true"
                    data-tree-toggle
                    class="ash-tree__toggle"
            >
                <span data-tree-chevron class="ash-tree__chevron">
                    @template.ashlar.components.icon(icon = Icon.CHEVRON_RIGHT, classes = "ash-icon--sm")
                </span>
            </button>
        @else
            <span class="ash-tree__spacer" aria-hidden="true"></span>
        @endif

        @if (hasIcon)
            <span class="ash-tree__icon" aria-hidden="true">
                @template.ashlar.components.icon(icon = icon, classes = "ash-icon--sm")
            </span>
        @endif

        @if (isLink)
            <a href="${href}" tabindex="-1" class="ash-tree__label">${label}</a>
        @else
            <span class="ash-tree__label">${label}</span>
        @endif
    </div>

    @if (hasChildren)
        !{TreeContext.enterChildren();}
        <div
                data-tree-children
                inert="${!expanded}"
                class="ash-tree__panel"
        >
            <div class="ash-tree__clip">
                <ul role="group" class="ash-tree__group">
                    ${children}
                </ul>
            </div>
        </div>
        !{TreeContext.exitChildren();}
    @endif
</li>
