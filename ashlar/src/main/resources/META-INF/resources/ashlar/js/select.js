// Matches select.jte's `duration-150` transition class on the listbox menu.
const TRANSITION_DURATION_MS = 150;

// A run of keystrokes counts as one type-ahead search as long as each new
// character arrives within this window; a slower keystroke starts a fresh
// search instead of appending to the old one.
const TYPEAHEAD_RESET_MS = 500;

function prefersReducedMotion() {
    return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

// Runs `callback` once the menu's CSS transition has visibly finished, with
// a timer fallback so an interrupted or reduced-motion (genuinely
// zero-length) transition can't leave the menu stuck mid-close.
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

function optionLabel(option) {
    return option.querySelector("span").textContent.trim();
}

function initSelect(root) {
    const trigger = root.querySelector("[data-select-trigger]");
    const chevron = root.querySelector("[data-select-chevron]");
    const valueLabel = root.querySelector("[data-select-value]");
    const clearButton = root.querySelector("[data-select-clear]");
    const hiddenSelect = root.querySelector("[data-select-input]");
    const menu = root.querySelector("[data-select-menu]");
    const status = root.querySelector("[data-select-status]");
    const errorMessage = root.querySelector("[data-select-error]");
    const options = Array.from(root.querySelectorAll("[data-select-option]"));

    const placeholder = root.dataset.placeholder || "Select an option";

    // Captured once, before any state changes: the trigger's own
    // server-rendered classes are the "valid" look, and its
    // data-select-error-classes attribute (the same ButtonStyles-style
    // pattern used by date-picker.js) is the "invalid" look. Swapping
    // between two known class sets keeps this from drifting out of sync
    // with the template.
    const normalTriggerClasses = trigger.className.split(/\s+/).filter(Boolean);
    const errorTriggerClasses = (trigger.dataset.selectErrorClasses || "").split(/\s+/).filter(Boolean);

    // All state below is private to this instance - nothing here is shared
    // with any other Select on the page.
    let activeOption = null; // the currently highlighted option, independent of selection
    let typeaheadBuffer = "";
    let typeaheadTimer = null;
    let isClosing = false;

    function enabledOptions() {
        return options.filter((option) => option.getAttribute("aria-disabled") !== "true");
    }

    function selectedOption() {
        return options.find((option) => option.getAttribute("aria-selected") === "true") || null;
    }

    function isOpen() {
        return !menu.classList.contains("hidden");
    }

    function announce(message) {
        if (status) {
            status.textContent = message;
        }
    }

    // Reflects an actual validation failure (a real submit attempt while
    // required and empty) - never set just because the trigger was
    // focused, opened, or tabbed away from.
    function showError() {
        trigger.classList.remove(...normalTriggerClasses);
        trigger.classList.add(...errorTriggerClasses);
        trigger.setAttribute("aria-invalid", "true");
        if (errorMessage) {
            trigger.setAttribute("aria-describedby", errorMessage.id);
            errorMessage.classList.remove("hidden");
        }
    }

    function clearError() {
        trigger.classList.remove(...errorTriggerClasses);
        trigger.classList.add(...normalTriggerClasses);
        trigger.removeAttribute("aria-invalid");
        if (errorMessage) {
            trigger.removeAttribute("aria-describedby");
            errorMessage.classList.add("hidden");
        }
    }

    function setActiveOption(option) {
        if (activeOption) {
            activeOption.dataset.active = "false";
        }
        activeOption = option || null;
        if (activeOption) {
            activeOption.dataset.active = "true";
            trigger.setAttribute("aria-activedescendant", activeOption.id);
            activeOption.scrollIntoView({ block: "nearest" });
        } else {
            trigger.removeAttribute("aria-activedescendant");
        }
    }

    function updateTriggerLabel() {
        const current = selectedOption();
        if (current) {
            valueLabel.textContent = optionLabel(current);
            valueLabel.classList.remove("text-slate-400", "dark:text-slate-500");
        } else {
            valueLabel.textContent = placeholder;
            valueLabel.classList.add("text-slate-400", "dark:text-slate-500");
        }

        if (clearButton) {
            clearButton.classList.toggle("hidden", !current);
            clearButton.classList.toggle("flex", Boolean(current));
        }
    }

    function openMenu() {
        if (trigger.disabled || isOpen()) {
            return;
        }
        isClosing = false;
        menu.classList.remove("hidden");
        // Force the browser to register the "closed" state above before the
        // next style change flips it to "open" - without this, both
        // changes would land in the same paint and no transition would be
        // visible.
        void menu.offsetWidth;
        menu.classList.remove("opacity-0", "scale-95");
        menu.classList.add("opacity-100", "scale-100");
        trigger.setAttribute("aria-expanded", "true");
        chevron.classList.add("rotate-180");

        setActiveOption(selectedOption() || enabledOptions()[0] || null);
    }

    function closeMenu() {
        if (!isOpen() || isClosing) {
            return;
        }
        isClosing = true;
        menu.classList.remove("opacity-100", "scale-100");
        menu.classList.add("opacity-0", "scale-95");
        trigger.setAttribute("aria-expanded", "false");
        chevron.classList.remove("rotate-180");
        setActiveOption(null);

        afterTransition(menu, () => {
            menu.classList.add("hidden");
            isClosing = false;
        });
    }

    function selectOption(option) {
        if (!option || option.getAttribute("aria-disabled") === "true") {
            return;
        }
        options.forEach((other) => other.setAttribute("aria-selected", String(other === option)));
        hiddenSelect.value = option.dataset.selectOptionValue;
        hiddenSelect.dispatchEvent(new Event("change", { bubbles: true }));
        updateTriggerLabel();
        clearError();
        announce(`${optionLabel(option)} selected`);
        closeMenu();
        trigger.focus();
    }

    function clearSelection() {
        options.forEach((option) => option.setAttribute("aria-selected", "false"));
        hiddenSelect.value = "";
        hiddenSelect.dispatchEvent(new Event("change", { bubbles: true }));
        updateTriggerLabel();
        announce("Selection cleared");
        trigger.focus();
    }

    function moveActiveBy(offset) {
        const enabled = enabledOptions();
        if (enabled.length === 0) {
            return;
        }
        const currentIndex = enabled.indexOf(activeOption);
        const nextIndex = currentIndex === -1
            ? (offset > 0 ? 0 : enabled.length - 1)
            : (currentIndex + offset + enabled.length) % enabled.length;
        setActiveOption(enabled[nextIndex]);
    }

    function moveActiveToEdge(toStart) {
        const enabled = enabledOptions();
        if (enabled.length === 0) {
            return;
        }
        setActiveOption(toStart ? enabled[0] : enabled[enabled.length - 1]);
    }

    function resetTypeahead() {
        typeaheadBuffer = "";
        typeaheadTimer = null;
    }

    function typeahead(character) {
        window.clearTimeout(typeaheadTimer);
        typeaheadBuffer += character.toLowerCase();
        typeaheadTimer = window.setTimeout(resetTypeahead, TYPEAHEAD_RESET_MS);

        const match = enabledOptions().find((option) => optionLabel(option).toLowerCase().startsWith(typeaheadBuffer));
        if (match) {
            if (!isOpen()) {
                openMenu();
            }
            setActiveOption(match);
        }
    }

    trigger.addEventListener("click", () => {
        isOpen() ? closeMenu() : openMenu();
    });

    trigger.addEventListener("keydown", (event) => {
        switch (event.key) {
            case "Enter":
            case " ":
                event.preventDefault();
                if (isOpen()) {
                    selectOption(activeOption);
                } else {
                    openMenu();
                }
                return;
            case "ArrowDown":
                event.preventDefault();
                isOpen() ? moveActiveBy(1) : openMenu();
                return;
            // Opening on ArrowUp too (not just ArrowDown) is a small,
            // deliberate departure from a literal reading of the spec: a
            // Select that only opens on one of the two arrow keys reads as
            // broken, not intentional.
            case "ArrowUp":
                event.preventDefault();
                isOpen() ? moveActiveBy(-1) : openMenu();
                return;
            case "Home":
                if (isOpen()) {
                    event.preventDefault();
                    moveActiveToEdge(true);
                }
                return;
            case "End":
                if (isOpen()) {
                    event.preventDefault();
                    moveActiveToEdge(false);
                }
                return;
            case "Escape":
                if (isOpen()) {
                    event.preventDefault();
                    // Consumed here, so an enclosing Modal/Drawer/Popover
                    // never also sees this same Escape - each one only
                    // checks event.key, not defaultPrevented, so without
                    // this a single Escape would close this listbox *and*
                    // whatever it's nested in. A second, later Escape
                    // (nothing left open here) reaches that ancestor as
                    // normal, since this branch only runs while isOpen().
                    event.stopPropagation();
                    closeMenu();
                }
                return;
            case "Tab":
                // No preventDefault: closing here just tidies state before
                // the browser moves focus on its own.
                if (isOpen()) {
                    closeMenu();
                }
                return;
            default:
                break;
        }

        if (event.key.length === 1 && !event.ctrlKey && !event.metaKey && !event.altKey) {
            event.preventDefault();
            typeahead(event.key);
        }
    });

    options.forEach((option) => {
        option.addEventListener("click", () => selectOption(option));
        option.addEventListener("mouseenter", () => {
            if (option.getAttribute("aria-disabled") !== "true") {
                setActiveOption(option);
            }
        });
    });

    if (clearButton) {
        clearButton.addEventListener("click", (event) => {
            event.stopPropagation();
            clearSelection();
        });
    }

    // Fires automatically, natively, when an enclosing <form> is submitted
    // while this required field is empty - no submit listener of our own is
    // needed. preventDefault() only suppresses the browser's own bubble/
    // focus for this field (which would otherwise point at an invisible
    // control); it does not mark the field valid or allow the form through.
    hiddenSelect.addEventListener("invalid", (event) => {
        event.preventDefault();
        showError();
    });

    document.addEventListener("click", (event) => {
        if (isOpen() && !event.composedPath().includes(root)) {
            closeMenu();
        }
    });

    root.addEventListener("focusout", () => {
        window.setTimeout(() => {
            if (isOpen() && !root.contains(document.activeElement)) {
                closeMenu();
            }
        }, 0);
    });

    updateTriggerLabel();
}

document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-select]").forEach(initSelect);
});
