@import com.redspartanlabs.ashlar.accordion.AccordionExpansion
@import com.redspartanlabs.ashlar.icon.Icon

@param String accordionId
@param String itemId
@param String title
@param boolean expanded = false
@param gg.jte.Content content = null

<%-- One disclosure section: a real <button> (native Tab/Enter/Space, no
     custom keyboard handling needed) that toggles the panel it controls.
     `aria-expanded` is the authoritative state; the chevron rotation is
     purely decorative CSS driven off that same button, so it can never
     disagree with what a screen reader announces. `accordionId` folds into
     both generated ids purely so two Accordion instances on one page can
     never collide even if their callers happen to pick the same `itemId`
     - accordion.js itself never needs it, since it only ever queries within
     its own root (see accordion.jte's own comment on this).

     `expanded` is resolved through AccordionExpansion, not used directly:
     the Accordion contract is single-open, so if a caller marks more than
     one item `expanded = true` in the same accordion, only the first one in
     document order actually renders open - see that class's own doc
     comment for why this needs a shared, request-scoped flag rather than
     being resolvable locally. Rendered state matches `resolvedExpanded`
     exactly on first paint - no flash of every panel open before JS runs.

     Unlike card.jte's deliberate refusal to add `role="region"` (an
     arbitrary, unbounded-count generic surface, where the role would turn
     every instance on a page into a landmark), the panel here gets
     `role="region"` + `aria-labelledby` on purpose: without a role, a plain
     `<div>` isn't exposed to assistive tech as a distinct object at all, so
     `aria-labelledby` would silently do nothing - the same problem
     accordion.jte's own comment identifies for `aria-label` on its wrapper.
     Region+labelledby is the standard technique for this specific,
     well-established pattern (an accordion panel is meant to be an
     independently identifiable section, not arbitrary content), and an
     accordion's item count is naturally small - not the unbounded-repeat
     case landmark-spam concerns apply to.

     The panel animates open/closed via the CSS `grid-template-rows: 0fr` -
     `1fr` technique rather than a `hidden` class: a `display:none` panel
     can't transition at all (there's nothing to animate between "not
     rendered" and "rendered"), and no clean, JS-measurement-free way exists
     to transition to/from a height nobody has computed yet. A single-row
     grid whose track size is `0fr` collapsed / `1fr` open, with its one
     child clipped by `overflow-hidden`, animates to the content's own
     natural height with no JavaScript ever measuring anything - the browser's
     layout engine does that part. The 0fr/1fr value itself is set through
     inline `style`, the same way progress.jte's fill width and
     skeleton.jte's width/height already are: a genuinely dynamic per-render
     value Tailwind's build-time class scanner can't enumerate, not a public
     styling hook.

     The grid item that clips the content (`overflow-hidden` + `min-h-0`)
     carries no padding of its own - the padding lives one level deeper, on
     a third, innermost div. Verified live in-browser that padding directly
     on the clipping element itself holds the collapsed row open at the
     padding's own height (16px, matching what was `pb-4`) no matter what
     `min-height` is set to; only once the clipping element's own box is
     genuinely empty does the row reach true 0. The clipping element having
     `overflow-hidden` is still what makes the innermost div's padding+
     content disappear correctly when collapsed.

     `inert` on the collapsed panel is the thing that same technique
     genuinely needs and the old `hidden`-class version didn't: unlike
     `display:none`, a `0fr` grid row doesn't remove its content from the
     tab order or the accessibility tree by itself - a real `<button>` inside
     a collapsed section's content would otherwise still be silently
     reachable by keyboard while invisible. `inert` (a native HTML attribute,
     not a library) closes that gap without any JavaScript measuring or
     managing anything.

     The content div's `pt-1`: without it, the only space above the content
     is the button's own bottom padding (`py-3` = 12px), while `pb-4` gives
     16px below - measured live in-browser, that 4px asymmetry read as the
     content being slightly cramped against its header relative to the room
     it gets at the bottom. `pt-1` closes that gap to an even 16px/16px
     without detaching the content from its header (still no divider, still
     visually one unit) and without a new spacing parameter. --%>
!{boolean resolvedExpanded = AccordionExpansion.resolve(expanded);}
!{String buttonId = accordionId + "-button-" + itemId;}
!{String panelId = accordionId + "-panel-" + itemId;}
!{String rowValue = resolvedExpanded ? "1fr" : "0fr";}
!{String chevronRotation = resolvedExpanded ? "rotate-90" : "";}

<div data-accordion-item>
    <h3>
        <button
                type="button"
                id="${buttonId}"
                data-accordion-button
                aria-expanded="${String.valueOf(resolvedExpanded)}"
                aria-controls="${panelId}"
                class="flex w-full items-center justify-between gap-3 px-4 py-3 text-left text-sm font-medium text-slate-900 hover:bg-slate-50 focus:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-blue-500 dark:text-slate-100 dark:hover:bg-slate-800/50"
        >
            <span>${title}</span>
            <span data-accordion-chevron class="shrink-0 text-slate-400 transition-transform duration-200 ease-out motion-reduce:transition-none dark:text-slate-500 ${chevronRotation}">
                @template.ashlar.components.icon(icon = Icon.CHEVRON_RIGHT, classes = "h-4 w-4")
            </span>
        </button>
    </h3>

    <div
            id="${panelId}"
            data-accordion-panel
            role="region"
            aria-labelledby="${buttonId}"
            inert="${!resolvedExpanded}"
            style="grid-template-rows: ${rowValue}"
            class="grid transition-[grid-template-rows] duration-200 ease-out motion-reduce:transition-none"
    >
        <div class="min-h-0 overflow-hidden">
            <div class="px-4 pt-1 pb-4 text-sm text-slate-700 dark:text-slate-300">
                @if (content != null)
                    ${content}
                @endif
            </div>
        </div>
    </div>
</div>
