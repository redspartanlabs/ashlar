// Matches the 150ms transition of the menu in dropdown-menu.css.
const TRANSITION_DURATION_MS = 150;

function prefersReducedMotion() {
    return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

// Runs `callback` once the menu's CSS transition has visibly finished, with
// a timer fallback so an interrupted or reduced-motion (genuinely
// zero-length) transition can't leave the menu stuck mid-close. Same
// technique as select.js/modal.js's own copy of this helper.
//
// The wait belongs to one close, and isCurrent() says whether that close is
// still the latest change. A later open, or a later open and close, makes it
// stale, and a stale close must not hide anything: the menu reopened during
// its fade-out would be hidden while it is open. The close can end two ways,
// its transitionend and its fallback timer, and each checks that on its own.
// Whichever arrives first stops the other, so a stale wait leaves nothing
// behind.
function afterTransition(element, isCurrent, callback) {
    if (prefersReducedMotion()) {
        callback();
        return;
    }

    let timer = 0;
    function stop() {
        element.removeEventListener("transitionend", onTransitionEnd);
        window.clearTimeout(timer);
    }
    function onTransitionEnd(event) {
        if (event.target !== element) {
            return;
        }
        stop();
        if (isCurrent()) {
            callback();
        }
    }
    function onTimeout() {
        stop();
        if (isCurrent()) {
            callback();
        }
    }

    element.addEventListener("transitionend", onTransitionEnd);
    timer = window.setTimeout(onTimeout, TRANSITION_DURATION_MS + 50);
}

function initDropdownMenu(root) {
    const trigger = root.querySelector("[data-dropdown-menu-trigger]");
    const chevron = root.querySelector("[data-dropdown-menu-chevron]");
    const menu = root.querySelector("[data-dropdown-menu-list]");
    const items = Array.from(root.querySelectorAll("[data-dropdown-menu-item]"));

    // Private to this instance - nothing here is shared with any other
    // Dropdown Menu on the page.
    //
    // Whether the menu is meant to be open, flipped synchronously with
    // intent. It is NOT read back off the hidden attribute of the menu, which
    // lags: it is only set once the fade-out has finished. Reading it meant
    // that for 150ms after a close the menu still looked open to the code, so
    // a click, Enter, Space or an arrow key on the trigger in that window was
    // dropped (the menu neither reopened nor closed), and an Escape in that
    // window was consumed here instead of reaching an enclosing Modal or
    // Drawer. The lifecycle counts every open and close, so a close can tell
    // whether a later change overtook it.
    let opened = false;
    let lifecycle = 0;

    function enabledItems() {
        return items.filter((item) => item.getAttribute("aria-disabled") !== "true");
    }

    function isOpen() {
        return opened;
    }

    // `focusTarget` is "first", "last", or omitted. Omitted is the
    // mouse-click case, where focus deliberately stays on the trigger
    // rather than jumping into the menu - the same thing a native OS menu
    // or a <select> does when opened by clicking it.
    function openMenu(focusTarget) {
        if (isOpen()) {
            return;
        }
        opened = true;
        lifecycle += 1;
        if (menu.hidden) {
            menu.hidden = false;
            // Force the browser to register the closed state above before the
            // next style change flips it to open - without this, both
            // changes would land in the same paint and no transition would be
            // visible.
            void menu.offsetWidth;
        }
        // Otherwise it is still on screen, fading out: it only turns back to
        // open, and the running fade reverses from wherever it got to.
        menu.dataset.state = "open";
        trigger.setAttribute("aria-expanded", "true");
        chevron.dataset.state = "open";

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
        if (!isOpen()) {
            return;
        }
        opened = false;
        lifecycle += 1;
        const exit = lifecycle;
        menu.dataset.state = "closed";
        trigger.setAttribute("aria-expanded", "false");
        chevron.dataset.state = "closed";

        if (returnFocusToTrigger) {
            trigger.focus();
        }

        // Only while this close is still the latest change: the menu may have
        // been reopened during its fade-out, or reopened and closed again.
        afterTransition(menu, () => exit === lifecycle, () => {
            menu.hidden = true;
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
                case "Tab":
                    // Tab and Shift+Tab from the trigger both leave the
                    // Dropdown Menu (no item is in the Tab order), so an
                    // open menu closes, as it does on Tab from an item. No
                    // preventDefault: the browser moves focus on its own,
                    // and focus is not pulled back to the trigger.
                    closeMenu(false);
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
