// TEST-ONLY FIXTURE. NOT served by the showcase and NOT part of the Ashlar library.
//
// Provenance: a verbatim copy of ashlar/src/main/resources/META-INF/resources/ashlar/js/search-input.js exactly as it was at commit 831a2f5
// ("feat: migrate Tree and Tree Item to native Ashlar CSS"), i.e. the search-input script BEFORE the Text Entry CSS migration.
//   git blob id of the original : 111f111b75e76dc6f7b109058eea5932b2ddf6ca
//   SHA-256 of everything below the marker line : 35c51ea1518a4946f38b367f8036ac4a7ff1c89123c610de4d2e788fedbe4492
//   (TextEntryBehaviourTest recomputes the hash, so an edit to the copy fails that test.)
//
// Purpose: the migrated script no longer toggles Tailwind classes (hidden and flex on the clear button). TextEntryBehaviourTest runs the SAME trusted
// keyboard and mouse sequence against (a) the legacy Tailwind markup with this original script and (b) the migrated markup with the migrated script, each in its OWN browser,
// and requires the observable behaviour to be identical at every step. Do not edit the copy below the marker; do not use it for anything else.
// ==== END OF PROVENANCE HEADER (the original file follows byte for byte) ====
// The only thing native HTML/CSS can't do here: keep the clear button's
// visibility in sync with the field's content as the user actually types,
// not just whatever `value` this was first server-rendered with.
function initSearchInput(root) {
    const input = root.querySelector("[data-search-input-field]");
    const clearButton = root.querySelector("[data-search-input-clear]");

    function updateClearVisibility() {
        const hasValue = input.value.length > 0;
        clearButton.classList.toggle("hidden", !hasValue);
        clearButton.classList.toggle("flex", hasValue);
    }

    input.addEventListener("input", updateClearVisibility);

    // type="button" (set in search-input.jte) already keeps this from
    // submitting an enclosing form - nothing here needs to guard against
    // that separately.
    clearButton.addEventListener("click", () => {
        input.value = "";
        input.dispatchEvent(new Event("input", { bubbles: true }));
        input.focus();
    });
}

document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-search-input]").forEach(initSearchInput);
});
