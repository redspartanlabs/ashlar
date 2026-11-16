import { lockScroll, lower, raise, releaseKeyboard, restoreFocus, settle, takeKeyboard, unlockScroll } from "./overlay.js";

const FOCUSABLE_SELECTOR = [
    "a[href]",
    "button:not([disabled])",
    "input:not([disabled])",
    "select:not([disabled])",
    "textarea:not([disabled])",
    '[tabindex]:not([tabindex="-1"])',
].join(", ");

function initModal(root) {
    const backdrop = root.querySelector("[data-modal-backdrop]");
    const dialog = root.querySelector("[data-modal-dialog]");

    const closeOnBackdrop = root.dataset.closeOnBackdrop === "true";
    const closeOnEscape = root.dataset.closeOnEscape === "true";

    // The state this component needs: who to give focus back to, whether a
    // close is already animating out, and which open or close is the current
    // one. All private to this instance. What overlays share - the paint
    // order, the keyboard, the page's scroll lock - is coordinated by
    // overlay.js, which this instance joins on open and leaves only when its
    // exit has finished (see closeModal).
    let lastOpener = null;
    let isClosing = false;
    // Every open and close takes a new number; a deferred step (the entry
    // frame, the end of the exit) does nothing if another open or close has
    // happened since it was scheduled.
    let generation = 0;

    const overlay = {
        root,
        escape: handleEscape,
        tab: trapTabKey,
        focusInitial: focusInitialElement,
        focusFirst() {
            const [first] = getFocusableElements();
            (first || dialog).focus();
        },
    };

    // Shown is not-hidden, exactly as it was when this read the "hidden"
    // class: the modal stays mounted for the whole exit, so an opener click
    // during those 200ms still sees an open modal and does nothing.
    function isOpen() {
        return !root.hidden;
    }

    // Moves backdrop and dialog between their "closed" and "open" visual
    // states (never a mix of both): the CSS transitions declared for
    // data-state animate between exactly two known states. The footer/body
    // never get their own state - they're plain children of the dialog, so
    // they move as part of the same single surface.
    function setEntered(isEntered) {
        root.dataset.state = isEntered ? "open" : "closed";
    }

    function getFocusableElements() {
        return Array.from(dialog.querySelectorAll(FOCUSABLE_SELECTOR))
            .filter((el) => el.offsetParent !== null);
    }

    function focusInitialElement() {
        const closeButton = dialog.querySelector("[data-modal-close]");
        if (closeButton) {
            closeButton.focus();
            return;
        }
        const [firstFocusable] = getFocusableElements();
        if (firstFocusable) {
            firstFocusable.focus();
            return;
        }
        dialog.focus();
    }

    function openModal(opener) {
        isClosing = false;
        const current = ++generation;
        lastOpener = opener || document.activeElement;
        root.hidden = false;
        setEntered(false);
        raise(overlay);
        takeKeyboard(overlay);

        // Force the browser to register the "closed" state above before the
        // next frame flips it to "open" - without this, both changes would
        // land in the same paint and no transition would be visible.
        void dialog.offsetWidth;
        requestAnimationFrame(() => {
            if (current === generation) {
                setEntered(true);
            }
        });

        lockScroll(overlay);
        focusInitialElement();
    }

    function closeModal() {
        if (isClosing || !isOpen()) {
            return;
        }
        isClosing = true;
        const current = ++generation;

        // Play the exit transition first. The modal stays mounted (not yet
        // hidden) for the duration of the fade, which is deliberate: it's what
        // makes the fade visible at all, and it keeps the focus trap and
        // Escape/backdrop handling correctly engaged for that entire window -
        // nothing here should be reachable again once the callback below
        // actually runs. It keeps the scroll lock until then, too.
        setEntered(false);

        settle([dialog, backdrop], () => current === generation, () => {
            root.hidden = true;
            isClosing = false;
            lower(overlay);
            releaseKeyboard(overlay);
            unlockScroll(overlay);
            const opener = lastOpener;
            lastOpener = null;
            restoreFocus(opener);
        });
    }

    function handleEscape(event) {
        if (closeOnEscape) {
            event.preventDefault();
            closeModal();
        }
    }

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
        }
    }

    backdrop.addEventListener("click", () => {
        if (closeOnBackdrop && isOpen()) {
            closeModal();
        }
    });

    // Escape and Tab arrive through overlay.js, which offers them only to the
    // topmost overlay that owns the keyboard (this one, while it is open and
    // on top).

    // A page opens/closes this specific modal via any element carrying
    // data-modal-open/data-modal-close with this modal's id - a plain
    // <button> anywhere on the page, not just inside the dialog itself.
    // Each modal instance only reacts to its own id, so multiple modals
    // each get their own listener here rather than sharing one registry.
    document.addEventListener("click", (event) => {
        const opener = event.target.closest(`[data-modal-open="${CSS.escape(root.id)}"]`);
        if (opener && !isOpen()) {
            openModal(opener);
            return;
        }

        const closer = event.target.closest(`[data-modal-close="${CSS.escape(root.id)}"]`);
        if (closer && isOpen()) {
            closeModal();
        }
    });
}

document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-modal]").forEach(initModal);
});
