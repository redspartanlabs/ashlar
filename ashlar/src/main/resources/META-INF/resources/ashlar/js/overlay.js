// Internal coordination for modal.js and drawer.js. NOT a public API: nothing
// else imports it, and its exports may change with those two scripts.
//
// Each Modal and Drawer is still its own instance, with its own triggers, its
// own state and its own timing (see modal.js and drawer.js). What lives here
// is what is page-wide by nature - and what broke when two overlays each
// managed it alone and were then open at the same time:
//
//   visual stack    every overlay that is drawn, bottom to top, from the
//                   moment it starts opening until its exit has finished. It
//                   decides the paint order: the overlay opened last is on
//                   top, whatever the document order (see Stacking below).
//   keyboard stack  the overlays that own the keyboard, bottom to top. Only
//                   the topmost one is offered Escape and Tab, so one Escape
//                   closes one overlay, and an overlay that does not close on
//                   Escape is never bypassed by closing one behind it.
//   scroll lock     a set of owners. The first owner saves the page's own
//                   inline overflow and locks it; every later owner only
//                   joins; the last one to leave puts the saved value back,
//                   once. Joining twice or leaving twice changes nothing.
//   focus           restoring focus never moves it behind an overlay that is
//                   still active: it goes to the opener only when the opener
//                   is inside the topmost remaining overlay (or when no
//                   overlay remains, exactly as a single overlay always did).
//   transitions     settle() waits until every transition the change started
//                   has finished or been cancelled, with a fallback timer read
//                   from the computed CSS. A zero-length transition, reduced
//                   motion included, finishes at once.
//
// When an overlay leaves each of these is each instance's decision, because
// the two components always differed in when they gave things up (a Modal
// keeps the keyboard and the scroll lock until its exit has finished, a
// Drawer gives both up the moment it starts closing). This module only makes
// those decisions safe when several overlays are open.
//
// Stacking. Every overlay root is position: fixed at z-index 40, the overlay
// band between Popover (30) and the toast region (50). Overlays drawn above
// the bottom one get --ash-overlay-stack (1 to 9), which overlay.css adds to
// that 40: the cap keeps every overlay below the toast region, so a toast is
// never hidden behind a dialog. Past ten overlays at once the extra ones share
// 49 and paint in document order. A z-index only orders elements that share a
// parent stacking context, so an overlay rendered inside an element that makes
// its own (a z-indexed relative wrapper, a transform, ...) is drawn within
// that element's level, whatever its offset here.
//
// One instance per page: modal.js and drawer.js both import "./overlay.js",
// which resolves against their own URL, and Ashlar's asset URLs carry no query
// string, so the browser evaluates this module once.

const MAX_STACK_OFFSET = 9;

const visualStack = [];
const keyboardStack = [];
const scrollOwners = new Set();
let savedOverflow = null;

// Keydown events that arrived while a popup inside the topmost overlay was
// open (decided in the capture phase, before any component has reacted).
const popupOwned = new WeakSet();

function remove(list, item) {
    const index = list.indexOf(item);
    if (index < 0) {
        return false;
    }
    list.splice(index, 1);
    return true;
}

function restack() {
    visualStack.forEach((overlay, index) => {
        const offset = Math.min(index, MAX_STACK_OFFSET);
        if (offset === 0) {
            overlay.root.style.removeProperty("--ash-overlay-stack");
        } else {
            overlay.root.style.setProperty("--ash-overlay-stack", String(offset));
        }
    });
}

// ---- visual stack ----------------------------------------------------------

/** The overlay is drawn, on top of every other overlay (it is moved there if it already was drawn, which a Drawer reopened during its exit is). */
export function raise(overlay) {
    remove(visualStack, overlay);
    visualStack.push(overlay);
    restack();
}

/** The overlay is no longer drawn (its exit has finished and it is hidden). */
export function lower(overlay) {
    if (remove(visualStack, overlay)) {
        overlay.root.style.removeProperty("--ash-overlay-stack");
        restack();
    }
}

// ---- keyboard stack --------------------------------------------------------

/** The overlay owns the keyboard, above every other overlay. */
export function takeKeyboard(overlay) {
    remove(keyboardStack, overlay);
    keyboardStack.push(overlay);
}

export function releaseKeyboard(overlay) {
    remove(keyboardStack, overlay);
}

function topmost() {
    return keyboardStack.length > 0 ? keyboardStack[keyboardStack.length - 1] : null;
}

// ---- scroll lock -----------------------------------------------------------

export function lockScroll(owner) {
    if (scrollOwners.has(owner)) {
        return;
    }
    if (scrollOwners.size === 0) {
        // The longhands, so an application's own overflow-y (or an
        // !important one) is put back exactly as it was written.
        const style = document.body.style;
        savedOverflow = ["overflow-x", "overflow-y"].map((name) => [name, style.getPropertyValue(name), style.getPropertyPriority(name)]);
        style.setProperty("overflow", "hidden");
    }
    scrollOwners.add(owner);
}

export function unlockScroll(owner) {
    if (!scrollOwners.delete(owner) || scrollOwners.size > 0) {
        return;
    }
    const style = document.body.style;
    const saved = savedOverflow;
    savedOverflow = null;
    style.removeProperty("overflow");
    for (const [name, value, priority] of saved) {
        if (value) {
            style.setProperty(name, value, priority);
        }
    }
}

// ---- focus -----------------------------------------------------------------

function rendered(element) {
    return element.isConnected && element.getClientRects().length > 0;
}

/**
 * Gives focus back to `target` (the element that opened an overlay that is
 * closing). With no other overlay active this is exactly what a lone overlay
 * always did. With one still active, focus never lands behind it: it stays
 * where it is if it is already inside that overlay, goes to `target` only if
 * `target` is inside it, and otherwise goes to that overlay's own first
 * target (its close button, its first control, or the dialog itself).
 */
export function restoreFocus(target) {
    const top = topmost();
    const focusable = target && typeof target.focus === "function";
    if (!top) {
        if (focusable) {
            target.focus();
        }
        return;
    }
    if (top.root.contains(document.activeElement)) {
        return;
    }
    if (focusable && top.root.contains(target) && rendered(target)) {
        target.focus();
        return;
    }
    top.focusInitial();
}

// ---- Escape and Tab --------------------------------------------------------

// An open popup is announced by the components that have one. A Dropdown
// Menu, Select, Combobox or Date Picker trigger carries aria-haspopup and,
// while its popup is open, aria-expanded="true". A Popover uses disclosure
// semantics on purpose (aria-expanded and aria-controls, no aria-haspopup),
// so its open state is read from its anchor alone: the element popover.js
// marks, which is its data-popover-trigger slot or the first focusable inside
// it. A bare aria-expanded would also match an expanded Accordion or Tree item
// inside the overlay - or inside a closed Popover's surface - neither of which
// owns an Escape. A Tooltip is shown while its bubble
// (role="tooltip") is referenced by aria-describedby. An Escape that arrives
// while one of those is open inside the topmost overlay belongs to that popup,
// which closes itself on it; the overlay then stays open, and a second Escape
// reaches it as normal. Deciding this in the capture phase, before any
// listener has run, makes the outcome the same whatever order the components'
// own listeners were added in, and whether or not they stop the event.
const OPEN_POPUP = '[aria-haspopup]:not([aria-haspopup="false"])[aria-expanded="true"], [data-popover-trigger][aria-expanded="true"], [data-popover-trigger] [aria-expanded="true"]';

function holdsOpenPopup(root) {
    if (root.querySelector(OPEN_POPUP)) {
        return true;
    }
    const described = Array.from(root.querySelectorAll("[aria-describedby]"))
        .flatMap((element) => element.getAttribute("aria-describedby").split(/\s+/));
    return Array.from(root.querySelectorAll('[role="tooltip"][id]')).some((tip) => described.includes(tip.id));
}

document.addEventListener("keydown", (event) => {
    if (event.key !== "Escape") {
        return;
    }
    const top = topmost();
    if (top && holdsOpenPopup(top.root)) {
        popupOwned.add(event);
    }
}, true);

document.addEventListener("keydown", (event) => {
    const top = topmost();
    if (!top) {
        return;
    }
    if (event.key === "Escape") {
        if (!popupOwned.has(event)) {
            top.escape(event);
        }
    } else if (event.key === "Tab") {
        // Stacked: focus that has ended up behind the topmost overlay (in an
        // overlay below it, say) is brought back into it. A lone overlay keeps
        // its own trap exactly as it was.
        if (keyboardStack.length > 1 && !top.root.contains(document.activeElement)) {
            event.preventDefault();
            top.focusFirst();
            return;
        }
        top.tab(event);
    }
});

// ---- transitions -----------------------------------------------------------

function milliseconds(value) {
    const text = value.trim();
    const number = parseFloat(text);
    if (!Number.isFinite(number)) {
        return 0;
    }
    return text.endsWith("ms") ? number : number * 1000;
}

/**
 * The longest duration + delay among the element's transitions, as the
 * browser computed them: the lists are matched the way CSS matches them (each
 * shorter list repeats over transition-property), "none" counts as nothing,
 * and a negative total counts as zero.
 */
export function transitionMs(element) {
    const style = getComputedStyle(element);
    const properties = style.transitionProperty.split(",").map((p) => p.trim());
    const durations = style.transitionDuration.split(",").map(milliseconds);
    const delays = style.transitionDelay.split(",").map(milliseconds);
    let longest = 0;
    properties.forEach((property, index) => {
        if (property !== "none") {
            longest = Math.max(longest, durations[index % durations.length] + delays[index % delays.length]);
        }
    });
    return longest;
}

/**
 * Calls `done` once every transition now running on `elements` has finished
 * or been cancelled - but only if `isCurrent()` is still true then, so a
 * close that was overtaken by a later open (or the reverse) does nothing. It
 * is called at once when nothing transitions (a zero-length or reduced-motion
 * transition, or nothing that changed). The fallback timer, read from the
 * computed transitions, ends a wait the browser never settles.
 */
export function settle(elements, isCurrent, done) {
    const bound = Math.max(0, ...elements.map(transitionMs));
    // getAnimations() flushes pending style changes first, so the transitions
    // the caller started a moment ago are among the ones returned.
    const running = bound === 0 ? [] : elements.flatMap((element) => element.getAnimations()
        .filter((animation) => typeof CSSTransition === "undefined" || animation instanceof CSSTransition));
    if (running.length === 0) {
        if (isCurrent()) {
            done();
        }
        return;
    }
    let finished = false;
    let timer = 0;
    function finish() {
        if (finished) {
            return;
        }
        finished = true;
        window.clearTimeout(timer);
        if (isCurrent()) {
            done();
        }
    }
    timer = window.setTimeout(finish, bound + 50);
    Promise.allSettled(running.map((animation) => animation.finished)).then(finish);
}
