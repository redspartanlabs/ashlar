// A short delay before showing, so quickly moving the pointer across
// several triggers in a row doesn't flash a tooltip open on each one.
// Hiding has no delay - the bubble is `pointer-events-none` (see
// tooltip.jte's own comment), so there's nothing to race against.
const SHOW_DELAY_MS = 150;

// Matches tooltip.jte's `duration-150` transition class on the bubble.
const TRANSITION_DURATION_MS = 150;

// How close the bubble may get to the viewport edge before it's nudged back.
const VIEWPORT_MARGIN_PX = 8;

// How far from the bubble's own end the arrow must stay when it's nudged, so
// it can never slide onto (or past) a rounded corner: the `rounded-md` radius
// plus half the rotated arrow's diagonal.
const ARROW_INSET_PX = 12;

const FOCUSABLE_SELECTOR = [
    "a[href]",
    "button:not([disabled])",
    "input:not([disabled])",
    "select:not([disabled])",
    "textarea:not([disabled])",
    "[tabindex]",
].join(", ");

function prefersReducedMotion() {
    return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

// Runs `callback` once the bubble's CSS transition has visibly finished,
// with a timer fallback so an interrupted or reduced-motion (genuinely
// zero-length) transition can't leave it stuck mid-hide. Same technique as
// select.js/modal.js/dropdown-menu.js's own copy of this helper.
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

function clamp(value, limit) {
    return Math.max(-limit, Math.min(limit, value));
}

function initTooltip(root) {
    const triggerSlot = root.querySelector("[data-tooltip-trigger]");
    const bubble = root.querySelector("[data-tooltip-bubble]");
    const arrow = root.querySelector("[data-tooltip-arrow]");
    const placement = root.dataset.tooltipPlacement;
    const isVerticalPlacement = placement === "top" || placement === "bottom";

    // `trigger` is arbitrary caller content (see tooltip.jte's own comment)
    // - this is what finds the actual thing a user hovers or tabs to
    // inside it. When nothing inside is natively focusable (a plain status
    // indicator, say), the slot itself is made a keyboard stop so the
    // tooltip stays reachable rather than permanently hover-only.
    let anchor = triggerSlot.querySelector(FOCUSABLE_SELECTOR);
    if (!anchor) {
        anchor = triggerSlot;
        if (!anchor.hasAttribute("tabindex")) {
            anchor.setAttribute("tabindex", "0");
        }
    }

    // Hover and focus are two independent signals that can overlap (a
    // mouse click focuses a button while the pointer is still over it) -
    // tracking both as plain booleans and showing whenever either is true
    // is what keeps this a single state model instead of two competing
    // timers that could each try to show/hide on their own.
    let isHovered = false;
    let isFocused = false;
    let showTimer = null;

    function isOpen() {
        return !bubble.classList.contains("hidden");
    }

    // Adds/removes only this tooltip's own id, rather than overwriting the
    // attribute wholesale: a trigger may already be described by something
    // the application put there (a field's help text, say), and clobbering
    // that would silently cost the user information Tooltip doesn't own.
    function describeTrigger() {
        const tokens = (anchor.getAttribute("aria-describedby") || "").split(/\s+/).filter(Boolean);
        if (!tokens.includes(bubble.id)) {
            tokens.push(bubble.id);
        }
        anchor.setAttribute("aria-describedby", tokens.join(" "));
    }

    function undescribeTrigger() {
        const tokens = (anchor.getAttribute("aria-describedby") || "")
            .split(/\s+/)
            .filter((token) => token && token !== bubble.id);
        if (tokens.length > 0) {
            anchor.setAttribute("aria-describedby", tokens.join(" "));
        } else {
            anchor.removeAttribute("aria-describedby");
        }
    }

    // Keeps the bubble inside the viewport on the axis it's centered on -
    // horizontal for top/bottom, vertical for left/right - and slides the
    // arrow the opposite way by the same amount so it still points at the
    // trigger rather than drifting off with the bubble.
    //
    // Deliberately NOT a positioning engine: no flipping to the opposite
    // side, no re-measuring on scroll/resize. It reads two rectangles once
    // per show and nudges along one axis. A placement with no room on the
    // side it was asked for (a Left tooltip on a trigger already against
    // the left edge) is the caller choosing the wrong placement, not
    // something this should silently override - see the component's own
    // doc comment.
    //
    // Sizes come from offsetWidth/offsetHeight, not getBoundingClientRect:
    // this runs while the bubble is still at `scale-95`, and a scaled rect
    // would make every correction come out ~5% short. Layout size ignores
    // the scale entirely.
    function reposition() {
        bubble.style.removeProperty("translate");
        arrow.style.removeProperty("translate");

        const anchorRect = root.getBoundingClientRect();
        const viewportSize = isVerticalPlacement
            ? document.documentElement.clientWidth
            : document.documentElement.clientHeight;
        const bubbleSize = isVerticalPlacement ? bubble.offsetWidth : bubble.offsetHeight;
        const anchorCenter = isVerticalPlacement
            ? anchorRect.left + anchorRect.width / 2
            : anchorRect.top + anchorRect.height / 2;

        const restingStart = anchorCenter - bubbleSize / 2;
        const restingEnd = restingStart + bubbleSize;

        let shift = 0;
        if (restingStart < VIEWPORT_MARGIN_PX) {
            shift = VIEWPORT_MARGIN_PX - restingStart;
        } else if (restingEnd > viewportSize - VIEWPORT_MARGIN_PX) {
            shift = viewportSize - VIEWPORT_MARGIN_PX - restingEnd;
        }
        if (shift === 0) {
            return;
        }

        // The arrow can only travel as far as the bubble's own edge allows;
        // past that it would sit on a corner and stop reading as a pointer.
        const arrowShift = clamp(-shift, Math.max(0, bubbleSize / 2 - ARROW_INSET_PX));

        if (isVerticalPlacement) {
            bubble.style.translate = `calc(-50% + ${shift}px)`;
            arrow.style.translate = `calc(-50% + ${arrowShift}px)`;
        } else {
            bubble.style.translate = `0 calc(-50% + ${shift}px)`;
            arrow.style.translate = `0 calc(-50% + ${arrowShift}px)`;
        }
    }

    function show() {
        showTimer = null;
        if (isOpen()) {
            return;
        }
        bubble.classList.remove("hidden");

        // Place it with transitions switched off, then force the browser to
        // register that whole "closed but already positioned" state, and
        // only then flip to open. `translate` is one of the properties the
        // bubble's `transition` class covers, so without this a clamped
        // tooltip would visibly slide sideways into place while it faded
        // in - only the fade and scale should ever be animated.
        bubble.style.transition = "none";
        reposition();
        void bubble.offsetWidth;
        bubble.style.removeProperty("transition");

        bubble.classList.remove("opacity-0", "scale-95");
        bubble.classList.add("opacity-100", "scale-100");
        describeTrigger();
    }

    function hide() {
        window.clearTimeout(showTimer);
        showTimer = null;
        if (!isOpen()) {
            return;
        }
        bubble.classList.remove("opacity-100", "scale-100");
        bubble.classList.add("opacity-0", "scale-95");
        undescribeTrigger();

        afterTransition(bubble, () => {
            bubble.classList.add("hidden");
            bubble.style.removeProperty("translate");
            arrow.style.removeProperty("translate");
        });
    }

    function sync() {
        if (isHovered || isFocused) {
            if (!isOpen() && showTimer === null) {
                showTimer = window.setTimeout(show, SHOW_DELAY_MS);
            }
        } else {
            hide();
        }
    }

    // Pointer events rather than mouseenter/mouseleave so touch can be told
    // apart from a real pointer. A tap synthesizes mouseenter but often
    // never a matching mouseleave, which would leave a tooltip stuck open
    // with no way to dismiss it; ignoring touch here means a tap simply
    // focuses the control (if it's focusable) and the focus path below
    // handles it, instead of Tooltip inventing a tap-to-toggle gesture that
    // would make it behave like a Popover.
    anchor.addEventListener("pointerenter", (event) => {
        if (event.pointerType === "touch") {
            return;
        }
        isHovered = true;
        sync();
    });
    anchor.addEventListener("pointerleave", (event) => {
        if (event.pointerType === "touch") {
            return;
        }
        isHovered = false;
        sync();
    });

    anchor.addEventListener("focus", () => {
        isFocused = true;
        sync();
    });
    anchor.addEventListener("blur", () => {
        isFocused = false;
        sync();
    });

    // Escape dismisses without moving focus or touching anything else on
    // the page. It's on the document, not the trigger, because a tooltip
    // shown by hover alone leaves focus wherever it already was - a
    // trigger-scoped listener would never see the key. The isOpen() guard
    // keeps it a no-op for every other tooltip on the page.
    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && isOpen()) {
            hide();
        }
    });
}

document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-tooltip]").forEach(initTooltip);
});
