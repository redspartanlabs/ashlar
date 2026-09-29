// Toggling is a real <button>'s native click, so a mouse or a touch tap
// always works with no keyboard handling involved. Keyboard users never
// reach that button at all (it's tabindex="-1" - see tree-item.jte's own
// comment) - arrow keys drive expand/collapse directly on the focused
// treeitem instead, per the WAI-ARIA APG tree pattern. Each tree's items
// live inside its own root, so every lookup here only ever searches within
// `root`, never `document` - the same per-instance scoping every other
// interactive component in this library already uses.
function initTree(root) {
    function allItems() {
        return Array.from(root.querySelectorAll("[data-tree-item]"));
    }

    // An item is "visible" (reachable by Arrow Up/Down/Home/End) exactly
    // when none of its ancestor groups are collapsed. `inert` already keeps
    // a collapsed branch out of the real tab order on its own; this is the
    // same check applied to this component's own keyboard navigation, which
    // walks the tree independently of Tab.
    function isVisible(item) {
        return !item.closest("[data-tree-children][inert]");
    }

    function visibleItems() {
        return allItems().filter(isVisible);
    }

    function childrenGroup(item) {
        return item.querySelector(":scope > [data-tree-children]");
    }

    function hasChildren(item) {
        return childrenGroup(item) !== null;
    }

    function isExpanded(item) {
        return item.getAttribute("aria-expanded") === "true";
    }

    // The nearest ancestor treeitem - found by first finding the group
    // `<div>` this item is nested inside (its parent's children container),
    // then the treeitem that owns that group. Root-level items have no such
    // ancestor and return null.
    function parentItem(item) {
        const group = item.parentElement.closest("[data-tree-children]");
        return group ? group.closest("[data-tree-item]") : null;
    }

    function setExpanded(item, expanded) {
        const group = childrenGroup(item);
        if (!group) {
            return;
        }
        item.setAttribute("aria-expanded", String(expanded));
        group.style.gridTemplateRows = expanded ? "1fr" : "0fr";
        group.inert = !expanded;

        const chevron = item.querySelector(":scope > div [data-tree-chevron]");
        if (chevron) {
            chevron.classList.toggle("rotate-90", expanded);
        }
    }

    function toggle(item) {
        if (hasChildren(item)) {
            setExpanded(item, !isExpanded(item));
        }
    }

    // Roving tabindex: exactly one treeitem is ever a Tab stop. Moving focus
    // anywhere in the tree - by click or by arrow key - hands that single
    // stop to whatever just received focus, the same technique
    // dropdown-menu.js already uses for its own roving-focus menu items.
    function focusItem(item) {
        if (!item) {
            return;
        }
        const current = root.querySelector('[data-tree-item][tabindex="0"]');
        if (current && current !== item) {
            current.setAttribute("tabindex", "-1");
        }
        item.setAttribute("tabindex", "0");
        item.focus();
    }

    function moveBy(offset) {
        const items = visibleItems();
        const current = document.activeElement.closest("[data-tree-item]");
        const index = items.indexOf(current);
        const nextIndex = index === -1 ? 0 : Math.min(Math.max(index + offset, 0), items.length - 1);
        focusItem(items[nextIndex]);
    }

    const [first] = allItems();
    if (first) {
        first.setAttribute("tabindex", "0");
    }

    root.addEventListener("click", (event) => {
        const button = event.target.closest("[data-tree-toggle]");
        if (button) {
            toggle(button.closest("[data-tree-item]"));
        }
    });

    // Keeps the roving tabindex correct even when focus arrives some way
    // other than this component's own keyboard handling - clicking a
    // treeitem directly, or a browser extension moving focus programmatically.
    root.addEventListener("focusin", (event) => {
        const item = event.target.closest("[data-tree-item]");
        if (item) {
            focusItem(item);
        }
    });

    root.addEventListener("keydown", (event) => {
        const item = event.target.closest("[data-tree-item]");
        if (!item) {
            return;
        }

        switch (event.key) {
            case "ArrowDown":
                event.preventDefault();
                moveBy(1);
                return;
            case "ArrowUp":
                event.preventDefault();
                moveBy(-1);
                return;
            case "ArrowRight":
                event.preventDefault();
                if (!hasChildren(item)) {
                    return;
                }
                if (!isExpanded(item)) {
                    setExpanded(item, true);
                } else {
                    focusItem(childrenGroup(item).querySelector("[data-tree-item]"));
                }
                return;
            case "ArrowLeft":
                event.preventDefault();
                if (hasChildren(item) && isExpanded(item)) {
                    setExpanded(item, false);
                } else {
                    focusItem(parentItem(item));
                }
                return;
            case "Home":
                event.preventDefault();
                focusItem(visibleItems()[0]);
                return;
            case "End": {
                event.preventDefault();
                const visible = visibleItems();
                focusItem(visible[visible.length - 1]);
                return;
            }
            case "Enter":
            case " ":
                event.preventDefault();
                if (hasChildren(item)) {
                    toggle(item);
                } else {
                    const link = item.querySelector(":scope > div a[href]");
                    if (link) {
                        link.click();
                    }
                }
                return;
            default:
                return;
        }
    });
}

document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-tree]").forEach(initTree);
});
