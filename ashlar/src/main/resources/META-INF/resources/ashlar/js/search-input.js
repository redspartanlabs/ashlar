// The only thing native HTML/CSS can't do here: keep the clear button's
// visibility in sync with the field's content as the user actually types,
// not just whatever `value` this was first server-rendered with. The button is shown
// and hidden through the native hidden attribute (search-input.jte renders it for an
// empty field; search-input.css makes sure its flex display cannot override it).
function initSearchInput(root) {
    const input = root.querySelector("[data-search-input-field]");
    const clearButton = root.querySelector("[data-search-input-clear]");

    function updateClearVisibility() {
        const hasValue = input.value.length > 0;
        clearButton.hidden = !hasValue;
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
