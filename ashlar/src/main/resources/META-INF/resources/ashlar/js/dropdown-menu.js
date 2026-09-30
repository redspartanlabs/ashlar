// Matches dropdown-menu.jte's `duration-150` transition class on the menu.
const TRANSITION_DURATION_MS = 150;

function prefersReducedMotion() {
    return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

// Runs `callback` once the menu's CSS transition has visibly finished, with
// a timer fallback so an interrupted or reduced-motion (genuinely
// zero-length) transition can't leave the menu stuck mid-close. Same
// technique as select.js/modal.js's own copy of this helper.
function afterTransition(element, callback) {
    if (prefersReducedMotion()) {
        callback();
        return;
    }

    let finished = false;
    function finish() {
        if (finished) {
            return;
        }
        finished = true;
        element.removeEventListener("transitionend", onTransitionEnd);
        callback();
    }
    function onTransitionEnd(event) {
        if (event.target === element) {
            finish();
        }
    }

    element.addEventListener("transitionend", onTransitionEnd);
    window.setTimeout(finish, TRANSITION_DURATION_MS + 50);
}

function initDropdownMenu(root) {
    const trigger = root.querySelector("[data-dropdown-menu-trigger]");
    const chevron = root.querySelector("[data-dropdown-menu-chevron]");
    const menu = root.querySelector("[data-dropdown-menu-list]");
    const items = Array.from(root.querySelectorAll("[data-dropdown-menu-item]"));

    // Private to this instance - nothing here is shared with any other
    // Dropdown Menu on the page.
    let isClosing = false;

    function enabledItems() {
        return items.filter((item) => item.getAttribute("aria-disabled") !== "true");
    }

    function isOpen() {
        return !menu.classList.contains("hidden");
    }

    // `focusTarget` is "first", "last", or omitted. Omitted is the
    // mouse-click case, where focus deliberately stays on the trigger
    // rather than jumping into the menu - the same thing a native OS menu
    // or a <select> does when opened by clicking it.
    function openMenu(focusTarget) {
        if (isOpen()) {
            return;
        }
        isClosing = false;
        menu.classList.remove("hidden");
        // Force the browser to register the "closed" state above before the
        // next style change flips it to "open" - without this, both
        // changes would land in the same paint and no transition would be
        // visible.
        void menu.offsetWidth;
        menu.classList.remove("opacity-0", "scale-95");
        menu.classList.add("opacity-100", "scale-100");
        trigger.setAttribute("aria-expanded", "true");
        chevron.classList.add("rotate-180");

        // Accessibility state above is set synchronously, before any
        // transition runs - correctness never waits on the animation.
        if (focusTarget === "first") {
            const [first] = enabledItems();
            if (first) {
                first.focus();
            }
        } else if (focusTarget === "last") {
            const enabled = enabledItems();
            const last = enabled[enabled.length - 1];
            if (last) {
                last.focus();
            }
        }
    }

    function closeMenu(returnFocusToTrigger) {
        if (!isOpen() || isClosing) {
            return;
        }
        isClosing = true;
        menu.classList.remove("opacity-100", "scale-100");
        menu.classList.add("opacity-0", "scale-95");
        trigger.setAttribute("aria-expanded", "false");
        chevron.classList.remove("rotate-180");

        if (returnFocusToTrigger) {
            trigger.focus();
        }

        afterTransition(menu, () => {
            menu.classList.add("hidden");
            isClosing = false;
        });
    }

    function moveFocusBy(offset) {
        const enabled = enabledItems();
        if (enabled.length === 0) {
            return;
        }
        const currentIndex = enabled.indexOf(document.activeElement);
        const nextIndex = currentIndex === -1
            ? (offset > 0 ? 0 : enabled.length - 1)
            : (currentIndex + offset + enabled.length) % enabled.length;
        enabled[nextIndex].focus();
    }

    function moveFocusToEdge(toStart) {
        const enabled = enabledItems();
        if (enabled.length === 0) {
            return;
        }
        (toStart ? enabled[0] : enabled[enabled.length - 1]).focus();
    }

    trigger.addEventListener("click", () => {
        isOpen() ? closeMenu(false) : openMenu();
    });

    // Scoped to `root`, not `document` - keydown events on a focused menu
    // item still bubble up through it, so one listener here covers both the
    // trigger-focused case and the menu-item-focused case. Real DOM focus
    // has to move onto the items themselves (unlike select.jte's listbox,
    // which tracks a highlighted option with aria-activedescendant while
    // focus stays on the trigger) because a menu item's native
    // Enter-activates-link behavior depends on the link actually being
    // focused.
    root.addEventListener("keydown", (event) => {
        if (event.target === trigger) {
            switch (event.key) {
                case "Enter":
                case " ":
                case "ArrowDown":
                    event.preventDefault();
                    openMenu("first");
                    return;
                case "ArrowUp":
                    event.preventDefault();
                    openMenu("last");
                    return;
                case "Escape":
                    if (isOpen()) {
                        event.preventDefault();
                        // Consumed here, so an enclosing Modal/Drawer/Popover
                        // never also sees this same Escape - each one only
                        // checks event.key, not defaultPrevented, so without
                        // this a single Escape would close this menu *and*
                        // whatever it's nested in. A second, later Escape
                        // (nothing left open here) reaches that ancestor as
                        // normal, since this branch only runs while isOpen().
                        event.stopPropagation();
                        closeMenu(false);
                    }
                    return;
                default:
                    return;
            }
        }

        if (!isOpen()) {
            return;
        }

        switch (event.key) {
            case "ArrowDown":
                event.preventDefault();
                moveFocusBy(1);
                return;
            case "ArrowUp":
                event.preventDefault();
                moveFocusBy(-1);
                return;
            case "Home":
                event.preventDefault();
                moveFocusToEdge(true);
                return;
            case "End":
                event.preventDefault();
                moveFocusToEdge(false);
                return;
            case "Escape":
                event.preventDefault();
                // Consumed here, so an enclosing Modal/Drawer/Popover never
                // also sees this same Escape - each one only checks
                // event.key, not defaultPrevented, so without this a single
                // Escape would close this menu *and* whatever it's nested
                // in. A second, later Escape (nothing left open here)
                // reaches that ancestor as normal, since this branch only
                // runs while isOpen() (checked just above, at the top of
                // this switch).
                event.stopPropagation();
                closeMenu(true);
                return;
            case "Tab":
                // No preventDefault: closing here just tidies state before
                // the browser moves focus on its own, the same pattern
                // select.js's own trigger keydown handler uses for Tab.
                closeMenu(false);
                return;
            case " ":
                // A focused <a> activates on Enter natively, but not on
                // Space - Space is handled explicitly only for that reason.
                if (document.activeElement && document.activeElement.tagName === "A") {
                    event.preventDefault();
                    document.activeElement.click();
                }
                return;
            default:
                return;
        }
    });

    enabledItems().forEach((item) => {
        item.addEventListener("click", () => closeMenu(true));
    });

    document.addEventListener("click", (event) => {
        if (isOpen() && !event.composedPath().includes(root)) {
            closeMenu(false);
        }
    });
}

document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-dropdown-menu]").forEach(initDropdownMenu);
});
