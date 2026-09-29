// Matches popover.jte's `duration-150` transition class on the surface.
const TRANSITION_DURATION_MS = 150;

// How close the surface may get to the viewport edge before it's nudged back.
const VIEWPORT_MARGIN_PX = 8;

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

// Runs `callback` once the surface's CSS transition has visibly finished,
// with a timer fallback so an interrupted or reduced-motion (genuinely
// zero-length) transition can't leave it stuck mid-close. Same technique as
// select.js/modal.js/dropdown-menu.js/tooltip.js's own copy of this helper.
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

function initPopover(root) {
    const triggerSlot = root.querySelector("[data-popover-trigger]");
    const surface = root.querySelector("[data-popover-surface]");
    const placement = root.dataset.popoverPlacement;
    const isVerticalPlacement = placement === "top" || placement === "bottom";

    // `trigger` is arbitrary caller content, so the real control has to be
    // found rather than assumed. A <button> is the expected trigger; when
    // the caller passes something inert instead, the slot itself is made
    // into one - a Popover with no way to open it is worse than borrowing
    // button semantics for the wrapper. Enter/Space are handled explicitly
    // there because only real buttons get that for free.
    let anchor = triggerSlot.querySelector(FOCUSABLE_SELECTOR);
    let anchorIsSynthetic = false;
    if (!anchor) {
        anchor = triggerSlot;
        anchorIsSynthetic = true;
        anchor.setAttribute("tabindex", "0");
        anchor.setAttribute("role", "button");
    }

    // Disclosure semantics, set here rather than in the template because
    // the template can't reach inside `trigger` to find this element. Both
    // are added alongside whatever the application already put on the
    // control rather than replacing anything.
    anchor.setAttribute("aria-controls", surface.id);
    anchor.setAttribute("aria-expanded", "false");

    // The open/closed state is tracked here rather than read back off the
    // `hidden` class, because that class lags: it isn't applied until the
    // fade-out transition finishes. Deriving state from it meant that for
    // ~150ms after a close the popover still looked open to the code, so a
    // click in that window closed it a second time instead of reopening -
    // the user's click just vanished. This flag flips synchronously with
    // intent, and `hidden` stays purely a visual end-state.
    let opened = false;

    // Keeps the surface inside the viewport on the axis it's centered on -
    // horizontal for top/bottom, vertical for left/right. Deliberately not
    // a positioning engine: no flipping to the opposite side, no
    // re-measuring on scroll. It reads two rectangles and nudges along one
    // axis. Sizes come from offsetWidth/offsetHeight rather than
    // getBoundingClientRect because this runs while the surface is still at
    // `scale-95`, and a scaled rect would make every correction come out
    // short.
    function reposition() {
        surface.style.removeProperty("translate");

        const anchorRect = root.getBoundingClientRect();
        const viewportSize = isVerticalPlacement
            ? document.documentElement.clientWidth
            : document.documentElement.clientHeight;
        const surfaceSize = isVerticalPlacement ? surface.offsetWidth : surface.offsetHeight;
        const anchorCenter = isVerticalPlacement
            ? anchorRect.left + anchorRect.width / 2
            : anchorRect.top + anchorRect.height / 2;

        const restingStart = anchorCenter - surfaceSize / 2;
        const restingEnd = restingStart + surfaceSize;

        let shift = 0;
        if (restingStart < VIEWPORT_MARGIN_PX) {
            shift = VIEWPORT_MARGIN_PX - restingStart;
        } else if (restingEnd > viewportSize - VIEWPORT_MARGIN_PX) {
            shift = viewportSize - VIEWPORT_MARGIN_PX - restingEnd;
        }
        if (shift === 0) {
            return;
        }

        surface.style.translate = isVerticalPlacement
            ? `calc(-50% + ${shift}px)`
            : `0 calc(-50% + ${shift}px)`;
    }

    function open() {
        if (opened) {
            return;
        }
        opened = true;
        surface.classList.remove("hidden");

        // Place it with transitions switched off, then force the browser to
        // register that "closed but already positioned" state, and only
        // then flip to open. `translate` is one of the properties the
        // surface's `transition` class covers, so without this a clamped
        // popover would visibly slide sideways as it faded in.
        surface.style.transition = "none";
        reposition();
        void surface.offsetWidth;
        surface.style.removeProperty("transition");

        surface.classList.remove("opacity-0", "scale-95");
        surface.classList.add("opacity-100", "scale-100");
        anchor.setAttribute("aria-expanded", "true");
    }

    // `returnFocus` is true only for dismissals the user drove from the
    // keyboard (Escape). Outside clicks and tabbing away must not yank
    // focus back to the trigger - the user is already somewhere else on
    // purpose, and pulling focus would fight them.
    function close(returnFocus) {
        if (!opened) {
            return;
        }
        opened = false;
        surface.classList.remove("opacity-100", "scale-100");
        surface.classList.add("opacity-0", "scale-95");
        anchor.setAttribute("aria-expanded", "false");

        if (returnFocus) {
            anchor.focus();
        }

        afterTransition(surface, () => {
            // The user may have reopened it while this fade-out was still
            // running; hiding it then would strand an "expanded" trigger
            // next to an invisible surface.
            if (!opened) {
                surface.classList.add("hidden");
                surface.style.removeProperty("translate");
            }
        });
    }

    function toggle() {
        if (opened) {
            close(false);
        } else {
            open();
        }
    }

    anchor.addEventListener("click", toggle);

    if (anchorIsSynthetic) {
        anchor.addEventListener("keydown", (event) => {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                toggle();
            }
        });
    }

    // Outside click. `composedPath` covers the whole component - trigger and
    // surface alike - so operating a control inside the popover never
    // dismisses the thing it lives in, which is the main way a Popover
    // differs from a Tooltip.
    document.addEventListener("click", (event) => {
        if (opened && !event.composedPath().includes(root)) {
            close(false);
        }
    });

    // Escape, on the document rather than the trigger: once the popover is
    // open the user's focus is very often inside it, where a
    // trigger-scoped listener would never see the key. Focus goes back to
    // the trigger because that's where the user was before they opened it.
    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && opened) {
            close(true);
        }
    });

    // Tabbing out of the last control in the surface (or out of the trigger
    // without ever entering it) closes the popover. `relatedTarget` is the
    // element focus actually moved to: when it's null, focus went nowhere
    // in particular - which is what a click on plain text inside the
    // surface looks like - and closing then would rip the popover away
    // mid-interaction.
    root.addEventListener("focusout", (event) => {
        const movingTo = event.relatedTarget;
        if (opened && movingTo && !root.contains(movingTo)) {
            close(false);
        }
    });

    // The clamp is computed against the viewport, so a resize can leave it
    // stale while the popover is still open. Recomputing is cheap and only
    // ever runs for an open popover.
    window.addEventListener("resize", () => {
        if (opened) {
            reposition();
        }
    });
}

document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-popover]").forEach(initPopover);
});
