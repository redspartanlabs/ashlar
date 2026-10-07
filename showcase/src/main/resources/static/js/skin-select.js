// Showcase-only glue between the navbar's skin Select (layout/page.jte) and
// the pre-paint bootstrap script in that same file. Not an Ashlar component
// asset - select.jte/select.js are used entirely as published, including
// the `clearable` clear-button fix made directly in select.jte itself.
//
// `localStorage.skin` + `data-skin` on <html> remain the one source of truth
// for the active skin, exactly as the bootstrap script already establishes.
// This file never invents a second place that state lives; it only:
//
//   1. Corrects the Select's own selected option to match whatever
//      `data-skin` the bootstrap already set, since the server has no way
//      to know the client's stored choice at render time - the same
//      category of problem theme-toggle.jte's own theme.js already solves
//      for light/dark, solved here for skins instead.
//   2. Writes through to `localStorage.skin`/`data-skin` on a real user
//      selection (or a clear), applying immediately - skins are pure CSS
//      keyed off that one attribute, so setting/removing it re-paints with
//      no reload.
//
// Deliberately a plain, synchronous classic script (no type="module", no
// defer/async): it runs the moment the parser reaches it, which is before
// any deferred/module script - including select.js's own initSelect() -
// has run, even though this tag appears after the Select's markup in the
// document. That ordering is what lets step 1 below write the "correct"
// attributes before select.js reads them to build its own visible label,
// so this file never needs to duplicate select.js's own rendering logic.
(function () {
    var root = document.querySelector("[data-skin-select]");
    if (!root) {
        return;
    }

    var hiddenSelect = root.querySelector("[data-select-input]");
    var options = Array.prototype.slice.call(root.querySelectorAll("[data-select-option]"));
    if (!hiddenSelect || options.length === 0) {
        return;
    }

    // The rendered <option> values ARE the list of valid skins - derived
    // from the same SelectOption list passed into the component, so this
    // never hardcodes a second copy of the ten skin names next to the one
    // already in page.jte. The empty value is Select's own internal
    // placeholder option - the same "nothing selected" state its clear
    // button already produces, never a separate sentinel invented here.
    var validSkins = Array.prototype.slice.call(hiddenSelect.options)
        .map(function (option) { return option.value; })
        .filter(Boolean);

    function applySelection(skin) {
        var matched = validSkins.indexOf(skin) !== -1 ? skin : "";
        hiddenSelect.value = matched;
        options.forEach(function (option) {
            option.setAttribute("aria-selected", String(option.dataset.selectOptionValue === matched && matched !== ""));
        });
    }

    // Step 1: reflect what the bootstrap script already applied, before
    // select.js's own deferred init reads these same attributes.
    try {
        applySelection(document.documentElement.getAttribute("data-skin") || "");
    } catch (error) {
        // No skin applies; the Select's own server-rendered cleared state
        // (clearable = true, no value) is already correct as-is.
    }

    // Step 2: a real user selection (or a clear, via Select's own clear
    // button) writes through to the one source of truth - select.js
    // dispatches this event itself either way, so no polling and no second
    // event system is needed.
    hiddenSelect.addEventListener("change", function () {
        var skin = hiddenSelect.value;
        var isRealSkin = validSkins.indexOf(skin) !== -1 && skin !== "";

        // Applying the attribute always succeeds and always runs, even if
        // the storage write just below fails - a visitor whose storage is
        // unavailable (private browsing, a sandboxed iframe) still sees the
        // skin apply to this page view; it just will not be remembered on
        // the next navigation, the same degradation theme-toggle.jte's own
        // theme.js already accepts for light/dark.
        if (isRealSkin) {
            document.documentElement.setAttribute("data-skin", skin);
        } else {
            document.documentElement.removeAttribute("data-skin");
        }

        try {
            if (isRealSkin) {
                window.localStorage.setItem("skin", skin);
            } else {
                window.localStorage.removeItem("skin");
            }
        } catch (error) {
            // Storage unavailable - the attribute above already applied.
        }
    });
})();
