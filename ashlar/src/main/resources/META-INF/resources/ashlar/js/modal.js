const FOCUSABLE_SELECTOR = [
    "a[href]",
    "button:not([disabled])",
    "input:not([disabled])",
    "select:not([disabled])",
    "textarea:not([disabled])",
    '[tabindex]:not([tabindex="-1"])',
].join(", ");

// Matches the duration-200 Tailwind class on the backdrop/dialog. Kept in
// one place so the JS wait time and the CSS transition can't quietly drift
// apart.
const TRANSITION_DURATION_MS = 200;

function prefersReducedMotion() {
    return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

// Runs `callback` once the element's CSS transition has visibly finished.
// Falls back to a timer so a transition that never fires transitionend
// (interrupted, or reduced-motion produces a genuinely zero-length
// transition that fires no event at all) can't leave the modal stuck
// mid-close. Under reduced motion there is nothing to wait for, so the
// callback runs immediately - this is the only place JS looks at the
// media query, purely to avoid an artificial delay; the animation itself
// is suppressed entirely by CSS regardless of what JS does here.
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

function initModal(root) {
    const backdrop = root.querySelector("[data-modal-backdrop]");
    const dialog = root.querySelector("[data-modal-dialog]");

    const closeOnBackdrop = root.dataset.closeOnBackdrop === "true";
    const closeOnEscape = root.dataset.closeOnEscape === "true";

    // The state this component needs: who to give focus back to, what the
    // page's scroll behavior looked like before this modal changed it, and
    // whether a close is already animating out. All three are private to
    // this instance - nothing here is shared with any other modal on the
    // page.
    let lastOpener = null;
    let previousBodyOverflow = null;
    let isClosing = false;

    function isOpen() {
        return !root.classList.contains("hidden");
    }

    // Toggles backdrop/dialog between their "closed" and "open" visual
    // states in matched pairs (never a mix of both), so the CSS
    // transitions declared in the template animate between exactly two
    // known states. The footer/body never get their own classes here -
    // they're plain children of the dialog, so they move as part of the
    // same single surface.
    function setEntered(isEntered) {
        backdrop.classList.toggle("opacity-100", isEntered);
        backdrop.classList.toggle("opacity-0", !isEntered);

        dialog.classList.toggle("opacity-100", isEntered);
        dialog.classList.toggle("opacity-0", !isEntered);
        dialog.classList.toggle("scale-100", isEntered);
        dialog.classList.toggle("scale-95", !isEntered);
        dialog.classList.toggle("translate-y-0", isEntered);
        dialog.classList.toggle("-translate-y-1", !isEntered);
    }

    function lockBodyScroll() {
        previousBodyOverflow = document.body.style.overflow;
        document.body.style.overflow = "hidden";
    }

    function unlockBodyScroll() {
        document.body.style.overflow = previousBodyOverflow;
        previousBodyOverflow = null;
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
        lastOpener = opener || document.activeElement;
        root.classList.remove("hidden");
        root.classList.add("flex");
        setEntered(false);

        // Force the browser to register the "closed" state above before the
        // next frame flips it to "open" - without this, both class changes
        // would land in the same paint and no transition would be visible.
        void dialog.offsetWidth;
        requestAnimationFrame(() => setEntered(true));

        lockBodyScroll();
        focusInitialElement();
    }

    function closeModal() {
        if (isClosing || !isOpen()) {
            return;
        }
        isClosing = true;

        // Play the exit transition first. The modal stays mounted (still
        // "flex", not yet "hidden") for the duration of the fade, which is
        // deliberate: it's what makes the fade visible at all, and it keeps
        // the focus trap and Escape/backdrop handling correctly engaged for
        // that entire window - nothing here should be reachable again once
        // the callback below actually runs.
        setEntered(false);

        afterTransition(dialog, () => {
            root.classList.add("hidden");
            root.classList.remove("flex");
            isClosing = false;
            unlockBodyScroll();
            if (lastOpener && typeof lastOpener.focus === "function") {
                lastOpener.focus();
            }
            lastOpener = null;
        });
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

    document.addEventListener("keydown", (event) => {
        if (!isOpen()) {
            return;
        }
        if (event.key === "Escape" && closeOnEscape) {
            event.preventDefault();
            closeModal();
        } else if (event.key === "Tab") {
            trapTabKey(event);
        }
    });

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
