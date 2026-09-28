function setButtonLoading(button, isLoading) {
    const spinner = button.querySelector("[data-btn-spinner]");
    const icon = button.querySelector("[data-btn-icon]");
    const status = button.querySelector("[data-btn-status]");

    if (isLoading) {
        button.dataset.btnWasDisabled = button.disabled ? "true" : "false";
        button.disabled = true;
        button.setAttribute("aria-busy", "true");

        if (spinner) {
            spinner.classList.remove("hidden");
            spinner.classList.add("inline-block");
        }
        if (icon) {
            icon.classList.add("hidden");
        }
        if (status) {
            status.textContent = "Loading";
        }
    } else {
        button.disabled = button.dataset.btnWasDisabled === "true";
        delete button.dataset.btnWasDisabled;
        button.removeAttribute("aria-busy");

        if (spinner) {
            spinner.classList.add("hidden");
            spinner.classList.remove("inline-block");
        }
        if (icon) {
            icon.classList.remove("hidden");
        }
        if (status) {
            status.textContent = "";
        }
    }
}

function initToggleButton(button) {
    button.addEventListener("click", () => {
        const isPressed = button.getAttribute("aria-pressed") === "true";
        const nextPressed = !isPressed;

        const baseClasses = (button.dataset.btnBaseClasses || "").split(/\s+/).filter(Boolean);
        const pressedClasses = (button.dataset.btnPressedClasses || "").split(/\s+/).filter(Boolean);

        button.classList.remove(...(nextPressed ? baseClasses : pressedClasses));
        button.classList.add(...(nextPressed ? pressedClasses : baseClasses));
        button.setAttribute("aria-pressed", String(nextPressed));

        const label = button.querySelector("[data-btn-label]");
        if (label) {
            label.textContent = nextPressed ? button.dataset.btnPressedText : button.dataset.btnDefaultText;
        }
    });
}

// Demo-only: any button marked with data-btn-loading-demo simulates an
// async action (e.g. a form save) so the loading state can be seen without
// wiring up a real backend call. A real page would call setButtonLoading()
// the same way around its own fetch/submit logic.
function initLoadingDemoButton(button) {
    button.addEventListener("click", () => {
        if (button.disabled) {
            return;
        }
        setButtonLoading(button, true);
        window.setTimeout(() => setButtonLoading(button, false), 1600);
    });
}

document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-btn-toggle]").forEach(initToggleButton);
    document.querySelectorAll("[data-btn-loading-demo]").forEach(initLoadingDemoButton);
});
