// Theme is global application state, not something owned by any one
// component instance - unlike every other component in this project (each
// of which manages its own closure-scoped state per [data-*] root), this
// file deliberately has no per-instance init function. There is one theme,
// document-wide, and every [data-theme-toggle] button just reflects and
// controls that same shared state.
const STORAGE_KEY = "theme";
const VALID_THEMES = ["light", "dark"];

function getStoredTheme() {
    try {
        const value = window.localStorage.getItem(STORAGE_KEY);
        return VALID_THEMES.includes(value) ? value : null;
    } catch (error) {
        // Storage blocked or unavailable (private browsing, disabled
        // storage, sandboxed iframe, etc.) - behave as if nothing is stored.
        return null;
    }
}

function storeTheme(theme) {
    try {
        window.localStorage.setItem(STORAGE_KEY, theme);
    } catch (error) {
        // Persistence is a nice-to-have; losing it shouldn't break the page.
    }
}

function currentTheme() {
    return document.documentElement.getAttribute("data-theme") === "dark" ? "dark" : "light";
}

function updateToggleButton(button) {
    const isDark = currentTheme() === "dark";
    button.setAttribute("aria-pressed", String(isDark));

    const lightIcon = button.querySelector("[data-theme-toggle-icon-light]");
    const darkIcon = button.querySelector("[data-theme-toggle-icon-dark]");
    const label = button.querySelector("[data-theme-toggle-label]");

    if (lightIcon) {
        lightIcon.classList.toggle("hidden", isDark);
    }
    if (darkIcon) {
        darkIcon.classList.toggle("hidden", !isDark);
    }
    if (label) {
        label.textContent = isDark ? "Dark" : "Light";
    }
}

// The one place theme state actually changes. Every toggle button syncs to
// it immediately, so two toggles on the same page can never disagree.
function applyTheme(theme) {
    document.documentElement.setAttribute("data-theme", theme);
    document.querySelectorAll("[data-theme-toggle]").forEach(updateToggleButton);
}

function setExplicitTheme(theme) {
    applyTheme(theme);
    storeTheme(theme);
}

function toggleTheme() {
    setExplicitTheme(currentTheme() === "dark" ? "light" : "dark");
}

document.addEventListener("DOMContentLoaded", () => {
    // The inline script in <head> already set the correct data-theme before
    // paint; this just brings every toggle button's icon/label/aria-pressed
    // in line with whatever that already is.
    document.querySelectorAll("[data-theme-toggle]").forEach(updateToggleButton);

    document.querySelectorAll("[data-theme-toggle]").forEach((button) => {
        button.addEventListener("click", toggleTheme);
    });

    // Only follow a live OS-level change when the user has never made an
    // explicit choice here - an explicit choice always wins over a later
    // system-preference change.
    if (window.matchMedia) {
        try {
            window.matchMedia("(prefers-color-scheme: dark)").addEventListener("change", (event) => {
                if (getStoredTheme() === null) {
                    applyTheme(event.matches ? "dark" : "light");
                }
            });
        } catch (error) {
            // Older browsers may not support addEventListener on a
            // MediaQueryList; following live OS changes is a nice-to-have.
        }
    }
});
