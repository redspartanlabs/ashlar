// Matches duration-200 on the toast container in ToastStyles.java.
const TRANSITION_DURATION_MS = 200;
// How long an announcement stays in a live region before being cleaned up.
const ANNOUNCEMENT_LIFETIME_MS = 7000;
// A toast whose timer ran down while hovered/focused gets at least this long
// after the pointer/focus leaves, so it can't vanish the instant you look away.
const MINIMUM_RESUME_MS = 1000;

// Swipe-to-dismiss thresholds.
const SWIPE_DISTANCE_PX = 88; // drag this far and releasing dismisses, regardless of speed
const SWIPE_VELOCITY_PX_PER_MS = 0.5; // a flick this fast dismisses even on a short drag
const SWIPE_MIN_FLICK_DISTANCE_PX = 24; // ignore velocity on a near-stationary pointer (jitter/click)
const SWIPE_AXIS_LOCK_PX = 6; // movement needed before committing to horizontal vs. vertical
const SWIPE_FADE_DISTANCE_PX = SWIPE_DISTANCE_PX; // opacity reaches its floor around the dismiss threshold
const SWIPE_FADE_FLOOR = 0.4;

function prefersReducedMotion() {
    return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

// Runs callback once the element's CSS transition has finished, with a timer
// fallback for when transitionend never fires (interrupted, or reduced motion).
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

function announce(toast) {
    const politeness = toast.dataset.toastPoliteness === "assertive" ? "assertive" : "polite";
    const announcer = document.querySelector(`[data-toast-announcer="${politeness}"]`);
    if (!announcer) {
        return;
    }
    const text = (selector) => (toast.querySelector(selector)?.textContent || "").trim();
    // "Error: Save failed. The server could not be reached." - the period gives
    // screen readers a pause between title and message.
    const body = [text("[data-toast-title]"), text("[data-toast-message]")]
        .filter(Boolean)
        .map((part) => (/[.!?]$/.test(part) ? part : `${part}.`))
        .join(" ");

    // Appending a new node (rather than replacing text) means two quick toasts
    // are both announced, and a repeated identical toast is announced again.
    const line = document.createElement("p");
    line.textContent = `${text("[data-toast-label]")} ${body}`;
    announcer.appendChild(line);
    window.setTimeout(() => line.remove(), ANNOUNCEMENT_LIFETIME_MS);
}

function initToast(toast, { removeOnDismiss }) {
    const duration = Math.max(0, Number(toast.dataset.toastDuration) || 0);

    let remaining = duration;
    let timerId = null;
    let timerStartedAt = 0;
    let isHovered = false;
    let isFocused = false;
    let returnFocusTo = null;

    // Drag state. All of it lives in this closure, same as the timer state
    // above, so multiple toasts - and multiple concurrent pointers across
    // different toasts - never interfere with each other.
    let isDragging = false;
    let dragAxis = null; // null while undecided, then "horizontal" or "vertical"
    let dragPointerId = null;
    let dragStartX = 0;
    let dragStartY = 0;
    let dragX = 0;
    let dragLastX = 0;
    let dragLastTime = 0;
    let dragVelocity = 0;

    function state() {
        return toast.dataset.toastState;
    }

    function startTimer() {
        if (duration === 0 || timerId !== null || isHovered || isFocused || isDragging || state() !== "open") {
            return;
        }
        timerStartedAt = Date.now();
        timerId = window.setTimeout(dismiss, remaining);
    }

    function pauseTimer() {
        if (timerId === null) {
            return;
        }
        window.clearTimeout(timerId);
        timerId = null;
        remaining = Math.max(remaining - (Date.now() - timerStartedAt), MINIMUM_RESUME_MS);
    }

    function show() {
        if (state() === "open") {
            return;
        }
        remaining = duration;
        // A pre-rendered toast keeps its server-rendered position in the region;
        // move it to the end so it joins the stack like any other new toast.
        toast.parentElement?.appendChild(toast);
        toast.dataset.toastState = "closed";
        // Reading offsetWidth forces the browser to apply the "closed"
        // (transparent, offset) styles now, so switching to "open" right after
        // animates. Deliberately not requestAnimationFrame: rAF never fires in a
        // background tab, which would leave a toast raised while the user is
        // elsewhere stuck invisible (but still focusable) until they return.
        void toast.offsetWidth;
        toast.dataset.toastState = "open";
        startTimer();
        announce(toast);
    }

    // When the toast holding focus disappears, send focus somewhere sensible
    // instead of letting it fall back to <body>.
    function moveFocusBeforeRemoval() {
        if (!toast.contains(document.activeElement)) {
            return;
        }
        const siblings = [toast.nextElementSibling, toast.previousElementSibling];
        for (const sibling of siblings) {
            const target = sibling?.dataset.toastState === "open" && sibling.querySelector("button, a[href]");
            if (target) {
                target.focus();
                return;
            }
        }
        if (returnFocusTo && document.contains(returnFocusTo)) {
            returnFocusTo.focus();
        }
    }

    function dismiss() {
        if (state() !== "open") {
            return;
        }
        // If a drag is still in progress (e.g. Escape pressed, or the auto-dismiss
        // timer fires - which startTimer() above already prevents by pausing while
        // isDragging, but this stays as a defensive guard against any other caller),
        // release the pointer cleanly rather than leaving it captured on a toast
        // that's about to close.
        cancelDrag();
        window.clearTimeout(timerId);
        timerId = null;
        moveFocusBeforeRemoval();
        toast.dataset.toastState = "closed";
        afterTransition(toast, () => {
            resetDragStyles();
            if (removeOnDismiss) {
                toast.remove();
            } else {
                toast.dataset.toastState = "hidden";
            }
        });
    }

    // Clears any inline transform/opacity/transition left by a drag, letting the
    // existing data-[toast-state=...] classes fully own the toast's appearance
    // again. Safe to call whether or not a drag ever happened.
    function resetDragStyles() {
        toast.style.transition = "";
        toast.style.transform = "";
        toast.style.opacity = "";
        toast.style.userSelect = "";
    }

    function cancelDrag() {
        if (!isDragging) {
            return;
        }
        isDragging = false;
        dragAxis = null;
        if (dragPointerId !== null) {
            try {
                toast.releasePointerCapture(dragPointerId);
            } catch (error) {
                // Capture may already be gone (pointercancel, detached element) - fine.
            }
        }
        dragPointerId = null;
    }

    function onPointerDown(event) {
        // Ignore secondary touch points, non-primary mouse buttons, and presses
        // that start on the close button or an action - those must behave like
        // normal clicks, never like the start of a drag.
        if (!event.isPrimary || (event.pointerType === "mouse" && event.button !== 0)) {
            return;
        }
        if (state() !== "open" || event.target.closest("button, a[href]")) {
            return;
        }
        isDragging = true;
        dragAxis = null;
        dragPointerId = event.pointerId;
        dragStartX = dragLastX = event.clientX;
        dragStartY = event.clientY;
        dragLastTime = Date.now();
        dragX = 0;
        dragVelocity = 0;
        pauseTimer();
        try {
            toast.setPointerCapture(event.pointerId);
        } catch (error) {
            // Some pointer sources (or synthetic events) don't have an active
            // capture session; the drag still works via the listeners below,
            // it just won't keep tracking if the pointer leaves the toast.
        }
        toast.style.transition = "none";
        toast.style.userSelect = "none";
    }

    function onPointerMove(event) {
        if (!isDragging || event.pointerId !== dragPointerId) {
            return;
        }
        const dx = event.clientX - dragStartX;
        const dy = event.clientY - dragStartY;

        if (dragAxis === null) {
            if (Math.abs(dx) < SWIPE_AXIS_LOCK_PX && Math.abs(dy) < SWIPE_AXIS_LOCK_PX) {
                return;
            }
            dragAxis = Math.abs(dx) > Math.abs(dy) ? "horizontal" : "vertical";
        }
        // Vertical gestures are left alone entirely (no transform, no
        // preventDefault) so touch-pan-y / native scroll behavior takes over.
        if (dragAxis === "vertical") {
            return;
        }
        event.preventDefault();

        dragX = dx;
        const now = Date.now();
        const elapsed = now - dragLastTime;
        if (elapsed > 0) {
            dragVelocity = (event.clientX - dragLastX) / elapsed;
        }
        dragLastX = event.clientX;
        dragLastTime = now;

        toast.style.transform = `translateX(${dx}px)`;
        const fade = Math.min(Math.abs(dx) / SWIPE_FADE_DISTANCE_PX, 1);
        toast.style.opacity = String(1 - fade * (1 - SWIPE_FADE_FLOOR));
    }

    function finishDrag() {
        const dx = dragX;
        const wasHorizontalDrag = dragAxis === "horizontal";
        cancelDrag();

        if (!wasHorizontalDrag) {
            resetDragStyles();
            startTimer();
            return;
        }

        const pastDistance = Math.abs(dx) >= SWIPE_DISTANCE_PX;
        const flicked = Math.abs(dx) >= SWIPE_MIN_FLICK_DISTANCE_PX && Math.abs(dragVelocity) >= SWIPE_VELOCITY_PX_PER_MS;

        if (pastDistance || flicked) {
            // Keep sliding in the direction the user was already dragging, out past
            // the toast's own width, rather than snapping to the default close
            // position - this is what makes a flick read as "thrown away" instead
            // of just closing.
            const direction = dx < 0 ? -1 : 1;
            const exitX = direction * (toast.offsetWidth + 80);
            toast.style.transition = prefersReducedMotion() ? "none" : "";
            toast.style.transform = `translateX(${exitX}px)`;
            toast.style.opacity = "0";
            dismiss();
        } else {
            toast.style.transition = "";
            toast.style.transform = "";
            toast.style.opacity = "";
            startTimer();
        }
    }

    function onPointerUp(event) {
        if (!isDragging || event.pointerId !== dragPointerId) {
            return;
        }
        finishDrag();
    }

    function onPointerCancel(event) {
        if (!isDragging || event.pointerId !== dragPointerId) {
            return;
        }
        cancelDrag();
        resetDragStyles();
        startTimer();
    }

    toast.addEventListener("mouseenter", () => {
        isHovered = true;
        pauseTimer();
    });
    toast.addEventListener("mouseleave", () => {
        isHovered = false;
        startTimer();
    });
    toast.addEventListener("focusin", (event) => {
        isFocused = true;
        pauseTimer();
        const region = toast.closest("[data-toast-region]");
        if (event.relatedTarget && region && !region.contains(event.relatedTarget)) {
            returnFocusTo = event.relatedTarget;
        }
    });
    toast.addEventListener("focusout", (event) => {
        if (toast.contains(event.relatedTarget)) {
            return;
        }
        isFocused = false;
        startTimer();
    });
    toast.addEventListener("click", (event) => {
        if (event.target.closest("[data-toast-dismiss]")) {
            dismiss();
        }
    });
    toast.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && toast.querySelector("[data-toast-close]")) {
            event.preventDefault();
            dismiss();
        }
    });

    // Pointer Events give one implementation for mouse, touch, and pen. { passive:
    // false } on pointermove is required for the preventDefault() inside it (once a
    // drag locks to the horizontal axis) to actually take effect.
    toast.addEventListener("pointerdown", onPointerDown);
    toast.addEventListener("pointermove", onPointerMove, { passive: false });
    toast.addEventListener("pointerup", onPointerUp);
    toast.addEventListener("pointercancel", onPointerCancel);

    if (!removeOnDismiss && toast.id) {
        document.addEventListener("click", (event) => {
            if (event.target.closest(`[data-toast-show="${CSS.escape(toast.id)}"]`)) {
                show();
            }
        });
    }

    if (state() === "open") {
        startTimer();
        // Give assistive tech a moment to register the live regions on page load.
        window.setTimeout(() => announce(toast), 250);
    }

    return { show, dismiss };
}

/**
 * Creates and shows a toast by cloning the matching server-rendered template.
 * options: { variant, title, message, duration, dismissible }
 * Returns { element, dismiss }, or null if the page has no toast region.
 */
export function showToast({ variant = "info", title = "", message = "", duration, dismissible = true } = {}) {
    const region = document.querySelector("[data-toast-region]");
    const template = document.querySelector(`[data-toast-template="${CSS.escape(String(variant).toLowerCase())}"]`)
        || document.querySelector('[data-toast-template="info"]');
    if (!region || !template) {
        console.warn("showToast: page has no toast region; render @template.ashlar.components.toast-region()");
        return null;
    }

    const toast = template.content.firstElementChild.cloneNode(true);

    const titleElement = toast.querySelector("[data-toast-title]");
    const messageElement = toast.querySelector("[data-toast-message]");
    titleElement.textContent = title;
    titleElement.classList.toggle("hidden", !title);
    messageElement.textContent = message;
    messageElement.classList.toggle("hidden", !message);

    if (duration !== undefined) {
        toast.dataset.toastDuration = String(Math.max(0, Number(duration) || 0));
    }

    const closeButton = toast.querySelector("[data-toast-close]");
    const isPersistent = toast.dataset.toastDuration === "0";
    if (closeButton) {
        if (!dismissible && !isPersistent) {
            closeButton.remove();
        } else if (title) {
            closeButton.setAttribute("aria-label", `Dismiss notification: ${title}`);
        }
    }

    region.appendChild(toast);
    const controller = initToast(toast, { removeOnDismiss: true });
    controller.show();
    return { element: toast, dismiss: controller.dismiss };
}

document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-toast-region] [data-toast]").forEach((toast) => {
        initToast(toast, { removeOnDismiss: false });
    });

    document.addEventListener("click", (event) => {
        const trigger = event.target.closest("[data-toast-trigger]");
        if (!trigger) {
            return;
        }
        showToast({
            variant: trigger.dataset.toastVariant,
            title: trigger.dataset.toastTitle,
            message: trigger.dataset.toastMessage,
            duration: trigger.dataset.toastDuration,
            dismissible: trigger.dataset.toastDismissible !== "false",
        });
    });
});
