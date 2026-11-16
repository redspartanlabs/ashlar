import { lockScroll, lower, raise, releaseKeyboard, restoreFocus, settle, takeKeyboard, unlockScroll } from "./overlay.js";

const FOCUSABLE_SELECTOR = [
    "a[href]",
    "button:not([disabled])",
    "input:not([disabled])",
    "select:not([disabled])",
    "textarea:not([disabled])",
    '[tabindex]:not([tabindex="-1"])',
].join(", ");

function initDrawer(root) {
    const backdrop = root.querySelector("[data-drawer-backdrop]");
    const panel = root.querySelector("[data-drawer-panel]");

    // Open/closed is tracked as intent, flipped synchronously, rather than
    // read back off the `hidden` attribute. That attribute lags by the length
    // of the exit transition, so deriving state from it would mean a click on
    // the trigger during those 200ms saw a drawer that was still "open" and
    // did nothing - the user's click would vanish. Everything below asks this
    // flag, never the DOM.
    let opened = false;
    let lastOpener = null;
    // Every open and close takes a new number; the end of an exit does
    // nothing if the drawer has been opened (or closed again) since.
    let generation = 0;

    const overlay = {
        root,
        escape(event) {
            event.preventDefault();
            close();
        },
        tab: trapTabKey,
        focusInitial: focusInitialElement,
        focusFirst() {
            const [first] = getFocusableElements();
            (first || panel).focus();
        },
    };

    // Which way "closed" is comes from the placement classes the template
    // wrote on the root and the panel (DrawerStyles), so nothing has to be
    // handed over here and the two can't disagree about it.
    function setEntered(isEntered) {
        root.dataset.state = isEntered ? "open" : "closed";
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
        ++generation;
        lastOpener = opener || document.activeElement;

        root.hidden = false;
        setEntered(false);
        raise(overlay);
        takeKeyboard(overlay);

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

        lockScroll(overlay);
        focusInitialElement();
    }

    function close() {
        if (!opened) {
            return;
        }
        opened = false;
        const current = ++generation;
        setEntered(false);

        // The keyboard goes with the intent: from now on Escape and Tab are
        // the next overlay's, if one is open.
        releaseKeyboard(overlay);

        // Focus goes back now, not when the slide-out finishes. The drawer
        // is already closed as far as intent goes, and waiting would leave
        // focus sitting on a control that's visibly sliding off screen.
        const opener = lastOpener;
        lastOpener = null;
        restoreFocus(opener);

        // Released now, for the same reason focus moves now. overlay.js keeps
        // the page locked while any other overlay still holds the lock, and
        // restores the page's own overflow only when the last one lets go.
        unlockScroll(overlay);

        settle([panel, backdrop], () => current === generation, () => {
            root.hidden = true;
            lower(overlay);
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

    // Escape and Tab arrive through overlay.js, which offers them only to the
    // topmost overlay that owns the keyboard (this one, while it is open and
    // on top).

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
