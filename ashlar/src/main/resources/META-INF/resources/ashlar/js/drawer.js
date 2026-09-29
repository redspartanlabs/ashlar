const FOCUSABLE_SELECTOR = [
    "a[href]",
    "button:not([disabled])",
    "input:not([disabled])",
    "select:not([disabled])",
    "textarea:not([disabled])",
    '[tabindex]:not([tabindex="-1"])',
].join(", ");

// Matches drawer.jte's `duration-200` on the backdrop and panel.
const TRANSITION_DURATION_MS = 200;

function prefersReducedMotion() {
    return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

// Runs `callback` once the element's CSS transition has visibly finished,
// with a timer fallback so an interrupted or reduced-motion (genuinely
// zero-length) transition can't leave the drawer stuck mid-close. Same
// technique as modal.js/select.js/popover.js's own copy of this helper.
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

function initDrawer(root) {
    const backdrop = root.querySelector("[data-drawer-backdrop]");
    const panel = root.querySelector("[data-drawer-panel]");
    // Which way "closed" is, handed over by the template so this and
    // DrawerStyles can't disagree about it.
    const closedClass = root.dataset.drawerClosedClass;

    // Open/closed is tracked as intent, flipped synchronously, rather than
    // read back off the `hidden` class. That class lags by the length of
    // the exit transition, so deriving state from it would mean a click on
    // the trigger during those 200ms saw a drawer that was still "open" and
    // did nothing - the user's click would vanish. Everything below asks
    // this flag, never the DOM.
    let opened = false;
    let lastOpener = null;

    function setEntered(isEntered) {
        backdrop.classList.toggle("opacity-100", isEntered);
        backdrop.classList.toggle("opacity-0", !isEntered);
        panel.classList.toggle("translate-x-0", isEntered);
        panel.classList.toggle(closedClass, !isEntered);
    }

    // Deliberately idempotent: set the lock, and on the way out just remove
    // the property again. The obvious alternative - remember the previous
    // inline value and put it back - looks more careful but has a much worse
    // failure mode. Its "previous value" is captured at open time, so if
    // anything else has already locked the body (another drawer whose
    // deferred unlock hasn't run yet, say) it faithfully restores "hidden"
    // and the page is stuck unscrollable for good. Removing the property
    // can't get stuck: closing always unlocks, however many times it was
    // locked. The cost is that an inline `overflow` the application itself
    // set on <body> would be dropped, which is a far cheaper mistake than a
    // page that can never scroll again.
    function lockBodyScroll() {
        document.body.style.overflow = "hidden";
    }

    function unlockBodyScroll() {
        document.body.style.removeProperty("overflow");
    }

    // getClientRects() rather than offsetParent: a panel that is itself
    // positioned would report a null offsetParent for its children on some
    // layouts, and this only ever needs to know "is it actually rendered".
    function getFocusableElements() {
        return Array.from(panel.querySelectorAll(FOCUSABLE_SELECTOR))
            .filter((el) => el.getClientRects().length > 0);
    }

    function focusInitialElement() {
        const closeButton = panel.querySelector("[data-drawer-close]");
        if (closeButton) {
            closeButton.focus();
            return;
        }
        const [first] = getFocusableElements();
        if (first) {
            first.focus();
            return;
        }
        panel.focus();
    }

    function open(opener) {
        if (opened) {
            return;
        }
        opened = true;
        lastOpener = opener || document.activeElement;

        root.classList.remove("hidden");
        root.classList.add("flex");
        setEntered(false);

        // Force the browser to commit that "closed" state, then flip to
        // "open" in the same task. Reading offsetWidth flushes layout, which
        // is what gives the transition a start value to animate from, so the
        // slide still plays without handing the open state off to a
        // requestAnimationFrame callback. That matters: rAF does not run in a
        // backgrounded tab, and a drawer opened there would stay logically
        // open - scroll locked, focus trapped - while its panel sat parked
        // off screen. Same reflow-then-toggle the rest of this library's
        // overlays use.
        void panel.offsetWidth;
        setEntered(true);

        lockBodyScroll();
        focusInitialElement();
    }

    function close() {
        if (!opened) {
            return;
        }
        opened = false;
        setEntered(false);

        // Focus goes back now, not when the slide-out finishes. The drawer
        // is already closed as far as intent goes, and waiting would leave
        // focus sitting on a control that's visibly sliding off screen.
        const opener = lastOpener;
        lastOpener = null;
        if (opener && typeof opener.focus === "function") {
            opener.focus();
        }

        // Unlocked now, for the same reason focus moves now, and because
        // deferring it lets one drawer's exit stomp another's lock: close
        // this drawer, open a second one inside the 200ms exit window, and
        // the first drawer's late callback would unlock the page while the
        // second is still modal over it. Doing it synchronously means the
        // lock always reflects the most recent intent.
        unlockBodyScroll();

        afterTransition(panel, () => {
            // Reopened while this exit was still running - it is mounted and
            // locked again already, and the new open() owns that state now.
            if (opened) {
                return;
            }
            root.classList.add("hidden");
            root.classList.remove("flex");
        });
    }

    // Tab is trapped because this drawer is modal: the backdrop hides the
    // page visually and `aria-modal` hides it from assistive tech, so
    // letting Tab walk out into content nobody can see or reach would be
    // the broken half of that promise. Not a trap for its own sake - the
    // close button and Escape are always available to get out.
    function trapTabKey(event) {
        const focusable = getFocusableElements();
        if (focusable.length === 0) {
            event.preventDefault();
            return;
        }

        const first = focusable[0];
        const last = focusable[focusable.length - 1];

        if (event.shiftKey && document.activeElement === first) {
            event.preventDefault();
            last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
            event.preventDefault();
            first.focus();
        } else if (!panel.contains(document.activeElement)) {
            // Focus somehow sits outside the panel while we're open (the
            // page was clicked before the backdrop caught it, say) - pull it
            // back rather than letting Tab continue through the page.
            event.preventDefault();
            first.focus();
        }
    }

    backdrop.addEventListener("click", () => {
        if (opened) {
            close();
        }
    });

    document.addEventListener("keydown", (event) => {
        if (!opened) {
            return;
        }
        if (event.key === "Escape") {
            event.preventDefault();
            close();
        } else if (event.key === "Tab") {
            trapTabKey(event);
        }
    });

    // Any element on the page carrying this drawer's id opens or closes it,
    // so one drawer can be opened from several places. Each instance only
    // ever reacts to its own id rather than sharing a registry.
    document.addEventListener("click", (event) => {
        const opener = event.target.closest(`[data-drawer-open="${CSS.escape(root.id)}"]`);
        if (opener) {
            if (!opened) {
                open(opener);
            }
            return;
        }

        const closer = event.target.closest(`[data-drawer-close="${CSS.escape(root.id)}"]`);
        if (closer && opened) {
            close();
        }
    });
}

document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-drawer]").forEach(initDrawer);
});
