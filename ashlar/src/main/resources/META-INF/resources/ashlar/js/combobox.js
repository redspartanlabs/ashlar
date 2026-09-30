// Matches combobox.jte's `duration-150` transition class on the listbox.
const TRANSITION_DURATION_MS = 150;

function prefersReducedMotion() {
    return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

// Runs `callback` once the listbox's CSS transition has visibly finished,
// with a timer fallback so an interrupted or reduced-motion (genuinely
// zero-length) transition can't leave it stuck mid-close.
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

function initCombobox(root) {
    const input = root.querySelector("[data-combobox-input]");
    const chevron = root.querySelector("[data-combobox-chevron]");
    const hiddenSelect = root.querySelector("[data-combobox-value]");
    const listbox = root.querySelector("[data-combobox-listbox]");
    const emptyMessage = root.querySelector("[data-combobox-empty]");
    const status = root.querySelector("[data-combobox-status]");
    const errorMessage = root.querySelector("[data-combobox-error]");
    const options = Array.from(root.querySelectorAll("[data-combobox-option]"));

    const placeholder = root.dataset.placeholder || "";

    // Captured once, before any state changes - the same known-class-sets
    // swap select.js uses for its own error styling.
    const normalInputClasses = input.className.split(/\s+/).filter(Boolean);
    const errorInputClasses = (input.dataset.comboboxErrorClasses || "").split(/\s+/).filter(Boolean);

    // combobox.jte already computed the correct initial aria-describedby
    // (help id, error id, both, or neither) server-side - captured once here
    // rather than re-derived, so JS never needs its own copy of "does this
    // instance have help text" logic. Stripping the error id (if any) out of
    // that leaves exactly the non-error ids - currently just the help text's,
    // when present - that must survive every later error show/clear.
    const persistentDescribedBy = (input.getAttribute("aria-describedby") || "")
        .split(/\s+/)
        .filter(Boolean)
        .filter((id) => !errorMessage || id !== errorMessage.id);

    // All state below is private to this instance.
    let activeOption = null; // the currently highlighted option, independent of commitment
    let committedOption = options.find((option) => option.getAttribute("aria-selected") === "true") || null;
    let isClosing = false;
    let suppressFocusOpen = false; // true only during commit()'s own synchronous refocus below

    function enabledOptions() {
        return options.filter((option) => option.getAttribute("aria-disabled") !== "true");
    }

    function visibleEnabledOptions() {
        return enabledOptions().filter((option) => !option.hidden);
    }

    function isOpen() {
        return !listbox.classList.contains("hidden");
    }

    function announce(message) {
        if (status) {
            status.textContent = message;
        }
    }

    // Applies whichever describedby ids apply right now - the persistent
    // ones (help text) plus the error id only when `includeError` is true -
    // or removes the attribute entirely if that leaves nothing to reference.
    function applyDescribedBy(includeError) {
        const ids = includeError && errorMessage ? [...persistentDescribedBy, errorMessage.id] : persistentDescribedBy;
        if (ids.length > 0) {
            input.setAttribute("aria-describedby", ids.join(" "));
        } else {
            input.removeAttribute("aria-describedby");
        }
    }

    function showError() {
        input.classList.remove(...normalInputClasses);
        input.classList.add(...errorInputClasses);
        input.setAttribute("aria-invalid", "true");
        if (errorMessage) {
            applyDescribedBy(true);
            errorMessage.classList.remove("hidden");
        }
    }

    function clearError() {
        input.classList.remove(...errorInputClasses);
        input.classList.add(...normalInputClasses);
        input.removeAttribute("aria-invalid");
        if (errorMessage) {
            applyDescribedBy(false);
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
            input.setAttribute("aria-activedescendant", activeOption.id);
            activeOption.scrollIntoView({ block: "nearest" });
        } else {
            input.removeAttribute("aria-activedescendant");
        }
    }

    // Shows every enabled option regardless of the input's current text -
    // used when the list opens from a click/focus/arrow key, so reopening
    // an already-filled-in combobox lets the user reconsider every choice
    // rather than only re-showing the one option that happens to match
    // its own committed label.
    function showAllOptions() {
        options.forEach((option) => {
            option.hidden = false;
        });
        emptyMessage.classList.add("hidden");
    }

    // Hides options whose label doesn't contain the query (simple,
    // predictable substring matching - not fuzzy search). Disabled options
    // are never hidden by filtering alone; they just stay non-selectable.
    function filterOptions(query) {
        const normalized = query.trim().toLowerCase();
        let anyVisible = false;
        options.forEach((option) => {
            const matches = normalized === "" || optionLabel(option).toLowerCase().includes(normalized);
            option.hidden = !matches;
            anyVisible = anyVisible || matches;
        });
        emptyMessage.classList.toggle("hidden", anyVisible);

        // Keep the highlight on a visible option whenever possible - if
        // filtering just hid the previously active one (or nothing was
        // active yet), move it to the first visible match so Enter can
        // still select immediately without an extra arrow-key press.
        if (!activeOption || activeOption.hidden) {
            setActiveOption(visibleEnabledOptions()[0] || null);
        }
    }

    function openMenu({ filterFromCurrentText } = {}) {
        if (input.disabled || isOpen()) {
            return;
        }
        isClosing = false;

        if (filterFromCurrentText) {
            filterOptions(input.value);
        } else {
            showAllOptions();
        }

        listbox.classList.remove("hidden");
        // Force the browser to register the "closed" state above before the
        // next style change flips it to "open" - without this, both
        // changes would land in the same paint and no transition would be
        // visible.
        void listbox.offsetWidth;
        listbox.classList.remove("opacity-0", "scale-95");
        listbox.classList.add("opacity-100", "scale-100");
        input.setAttribute("aria-expanded", "true");
        chevron.classList.add("rotate-180");

        setActiveOption(committedOption && !committedOption.hidden ? committedOption : visibleEnabledOptions()[0] || null);
    }

    function closeMenu() {
        if (!isOpen() || isClosing) {
            return;
        }
        isClosing = true;
        listbox.classList.remove("opacity-100", "scale-100");
        listbox.classList.add("opacity-0", "scale-95");
        input.setAttribute("aria-expanded", "false");
        chevron.classList.remove("rotate-180");
        setActiveOption(null);

        afterTransition(listbox, () => {
            listbox.classList.add("hidden");
            isClosing = false;
        });
    }

    function commit(option) {
        if (!option || option.getAttribute("aria-disabled") === "true") {
            return;
        }
        options.forEach((other) => other.setAttribute("aria-selected", String(other === option)));
        committedOption = option;
        hiddenSelect.value = option.dataset.comboboxOptionValue;
        hiddenSelect.dispatchEvent(new Event("change", { bubbles: true }));
        input.value = optionLabel(option);
        clearError();
        announce(`${optionLabel(option)} selected`);
        closeMenu();
        // A mouse commit blurs the input first - the option itself isn't
        // focusable, so per spec clicking it unfocuses whatever was
        // focused - before this refocuses it. Under reduced motion,
        // closeMenu()'s afterTransition callback has already run
        // synchronously by the time that refocus fires the listener below,
        // so isOpen() no longer blocks it from reopening the very menu this
        // just closed. Suppressing exactly that one synchronous reopen -
        // cleared immediately after, whether or not a focus event actually
        // consumed it - leaves a later, unrelated focus (tabbing back in)
        // opening the menu as normal.
        suppressFocusOpen = true;
        input.focus();
        suppressFocusOpen = false;
    }

    // Discards whatever is currently typed and snaps the input (and the
    // submitted value) back to the last real commitment - or to nothing,
    // if the field was intentionally emptied. This is what keeps the
    // hidden <select> from ever holding a stale selection the visible
    // text no longer agrees with, without needing a dedicated clear
    // button: emptying the input and leaving is itself how you clear it.
    function revertToCommitted() {
        if (input.value.trim() === "") {
            committedOption = null;
            options.forEach((option) => option.setAttribute("aria-selected", "false"));
        }
        // Whichever way input.value got here, the hidden value must agree
        // with it - typing already cleared it, so it's restored here in
        // lockstep with the visible text rather than left stale.
        input.value = committedOption ? optionLabel(committedOption) : "";
        hiddenSelect.value = committedOption ? committedOption.dataset.comboboxOptionValue : "";
        hiddenSelect.dispatchEvent(new Event("change", { bubbles: true }));
    }

    function moveActiveBy(offset) {
        const visible = visibleEnabledOptions();
        if (visible.length === 0) {
            return;
        }
        const currentIndex = visible.indexOf(activeOption);
        const nextIndex = currentIndex === -1
            ? (offset > 0 ? 0 : visible.length - 1)
            : (currentIndex + offset + visible.length) % visible.length;
        setActiveOption(visible[nextIndex]);
    }

    input.addEventListener("click", () => {
        if (!isOpen()) {
            openMenu();
        }
    });

    input.addEventListener("focus", () => {
        if (suppressFocusOpen) {
            suppressFocusOpen = false;
        } else {
            openMenu();
        }
        input.select();
    });

    // Real edits only - not focus/selection changes. Clearing the hidden
    // value here (rather than leaving the prior commitment in place while
    // the user types something new) is what guarantees the form only ever
    // submits a value the user explicitly picked.
    input.addEventListener("input", () => {
        if (hiddenSelect.value !== "") {
            hiddenSelect.value = "";
            hiddenSelect.dispatchEvent(new Event("change", { bubbles: true }));
        }
        if (!isOpen()) {
            openMenu({ filterFromCurrentText: true });
        } else {
            filterOptions(input.value);
        }
    });

    input.addEventListener("keydown", (event) => {
        switch (event.key) {
            case "Enter":
                if (isOpen() && activeOption) {
                    event.preventDefault();
                    commit(activeOption);
                }
                return;
            case "ArrowDown":
                event.preventDefault();
                isOpen() ? moveActiveBy(1) : openMenu();
                return;
            case "ArrowUp":
                event.preventDefault();
                isOpen() ? moveActiveBy(-1) : openMenu();
                return;
            case "Escape":
                if (isOpen()) {
                    event.preventDefault();
                    closeMenu();
                    revertToCommitted();
                }
                return;
            default:
                return;
        }
    });

    options.forEach((option) => {
        option.addEventListener("click", () => commit(option));
        option.addEventListener("mouseenter", () => {
            if (option.getAttribute("aria-disabled") !== "true") {
                setActiveOption(option);
            }
        });
    });

    // Fires automatically, natively, when an enclosing <form> is submitted
    // while this required field is empty - no submit listener of our own
    // is needed.
    hiddenSelect.addEventListener("invalid", (event) => {
        event.preventDefault();
        showError();
    });

    document.addEventListener("click", (event) => {
        if (isOpen() && !event.composedPath().includes(root)) {
            closeMenu();
            revertToCommitted();
        }
    });

    root.addEventListener("focusout", () => {
        window.setTimeout(() => {
            if (!root.contains(document.activeElement)) {
                if (isOpen()) {
                    closeMenu();
                }
                revertToCommitted();
            }
        }, 0);
    });
}

document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-combobox]").forEach(initCombobox);
});
