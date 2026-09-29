// This implementation uses the WAI-ARIA "automatic activation" tabs model:
// moving focus with the arrow keys immediately activates that tab and shows
// its panel, rather than requiring a separate Enter/Space press. Enter/Space
// still work when a tab has focus, purely because it's a real <button> -
// native click semantics call the same activation logic the click handler
// uses. With automatic activation the focused tab and the active tab are
// always the same one, so that's normally a no-op rather than a second way
// to activate a *different* tab.
function initTabs(root) {
    const tabButtons = Array.from(root.querySelectorAll("[data-tab]"));
    const panels = Array.from(
        document.querySelectorAll(`[data-tab-panel][data-tabs-id="${CSS.escape(root.id)}"]`)
    );

    function enabledTabs() {
        return tabButtons.filter((tab) => !tab.disabled);
    }

    function activateTab(tab, moveFocus) {
        tabButtons.forEach((button) => {
            const isActive = button === tab;
            button.setAttribute("aria-selected", String(isActive));
            button.tabIndex = isActive ? 0 : -1;
            // Read the active/inactive look from the button's own data
            // attributes (computed once by tabs.jte) rather than hardcoding
            // a second copy of the color classes here - otherwise this and
            // the template's initial render can silently drift apart.
            button.className = isActive ? button.dataset.tabActiveClasses : button.dataset.tabInactiveClasses;
        });

        panels.forEach((panel) => {
            panel.classList.toggle("hidden", panel.dataset.tabId !== tab.dataset.tabId);
        });

        if (moveFocus) {
            tab.focus();
        }
    }

    tabButtons.forEach((button) => {
        button.addEventListener("click", () => {
            if (!button.disabled) {
                activateTab(button, false);
            }
        });
    });

    const tabList = root.querySelector("[data-tabs-list]");
    tabList.addEventListener("keydown", (event) => {
        const enabled = enabledTabs();
        if (enabled.length === 0) {
            return;
        }

        const currentIndex = enabled.indexOf(document.activeElement);
        let nextIndex;

        switch (event.key) {
            case "ArrowRight":
                nextIndex = currentIndex === -1 ? 0 : (currentIndex + 1) % enabled.length;
                break;
            case "ArrowLeft":
                nextIndex = currentIndex === -1 ? enabled.length - 1 : (currentIndex - 1 + enabled.length) % enabled.length;
                break;
            case "Home":
                nextIndex = 0;
                break;
            case "End":
                nextIndex = enabled.length - 1;
                break;
            default:
                return;
        }

        event.preventDefault();
        activateTab(enabled[nextIndex], true);
    });

    // Reconcile panel visibility with whichever tab the server actually
    // rendered as aria-selected="true", rather than trusting each
    // tab-panel's own `active` param to have been kept in sync.
    const initiallySelected = tabButtons.find((tab) => tab.getAttribute("aria-selected") === "true") || tabButtons[0];
    if (initiallySelected) {
        activateTab(initiallySelected, false);
    }
}

document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-tabs]").forEach(initTabs);
});
