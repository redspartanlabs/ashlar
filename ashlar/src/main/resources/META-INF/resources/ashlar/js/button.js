// Loading is one state, whether the server rendered it (loading = true) or
// this function applied it: aria-busy="true" + aria-disabled="true" and
// "Loading" in the polite status region. The spinner is shown and the icon
// hidden by the stylesheet (button.css) from the button's own aria-busy, so
// this function toggles no class.
// It deliberately does NOT set the native `disabled` attribute - a disabled
// submit button is left out of the submitted form data, and disabling it
// from its own click handler cancels the submission outright. aria-disabled
// keeps the button focusable and its name/value intact; the click guard at
// the bottom of this file is what makes it inert.
//
// Synchronous and state-only: it owns no async work. Calling it again with
// the same value changes nothing.
export function setButtonLoading(button, loading) {
    if (!(button instanceof HTMLElement) || !button.matches("[data-btn]")) {
        return;
    }
    const status = button.querySelector("[data-btn-status]");
    const isLoading = Boolean(loading);

    if (isLoading) {
        button.setAttribute("aria-busy", "true");
        button.setAttribute("aria-disabled", "true");
    } else {
        button.removeAttribute("aria-busy");
        button.removeAttribute("aria-disabled");
    }
    if (status) {
        status.textContent = isLoading ? "Loading" : "";
    }
}

function initToggleButton(button) {
    button.addEventListener("click", () => {
        const isPressed = button.getAttribute("aria-pressed") === "true";
        const nextPressed = !isPressed;

        // The pressed look is the stylesheet's response to aria-pressed, so
        // flipping the attribute is the whole visual change.
        button.setAttribute("aria-pressed", String(nextPressed));

        const label = button.querySelector("[data-btn-label]");
        if (label) {
            label.textContent = nextPressed ? button.dataset.btnPressedText : button.dataset.btnDefaultText;
        }
    });
}

// A loading button is inert. aria-disabled alone does nothing to a click, so
// this runs in the capture phase on the document and stops the click before
// anything else - the button's own handlers, other components' delegated
// listeners (data-modal-open, data-toast-trigger, ...) and the form's
// native submission. Enter and Space on a focused button arrive as clicks,
// and so does implicit submission (Enter in a text field clicks the form's
// default button), so one guard covers the keyboard as well.
document.addEventListener("click", (event) => {
    if (event.target instanceof Element && event.target.closest('[data-btn][aria-busy="true"]')) {
        event.preventDefault();
        event.stopImmediatePropagation();
    }
}, true);

// Declarative form-submit loading (the loadingOnSubmit param). It listens for
// the form's submit event rather than the button's click, so it only fires
// once native validation has passed and the browser is really submitting.
// event.submitter is the button that triggered it, including the default
// button used by implicit (Enter) submission. A submit that something else
// has already cancelled - a failed validator, or an application's own
// fetch-based handler that called preventDefault() - is left alone; that
// case calls setButtonLoading() itself.
document.addEventListener("submit", (event) => {
    const submitter = event.submitter;
    if (!event.defaultPrevented && submitter instanceof HTMLElement && submitter.matches("[data-btn-loading-on-submit]")) {
        setButtonLoading(submitter, true);
    }
});

// Going Back can restore the page from the browser's back/forward cache with
// the button still loading, because the page never actually unloaded.
window.addEventListener("pageshow", (event) => {
    if (event.persisted) {
        document.querySelectorAll('[data-btn-loading-on-submit][aria-busy="true"]').forEach((button) => {
            setButtonLoading(button, false);
        });
    }
});

document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-btn-toggle]").forEach(initToggleButton);
});
