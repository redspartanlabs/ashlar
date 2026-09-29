// Live character count for a Textarea that opted in via
// `showCharacterCount = true` (textarea.jte). This never runs for a plain
// Textarea - the script tag itself is only emitted when the count is
// requested, so every other Textarea stays exactly as JS-free as Text
// Input's default case.
//
// The count is deliberately NOT an aria-live region: announcing on every
// keystroke would be far too chatty for screen reader users. It's
// associated via aria-describedby instead (set once, server-side in
// textarea.jte), so it's discoverable without being spoken continuously.
function initCharacterCount(root) {
    const textarea = root.querySelector("textarea");
    const counter = root.querySelector("[data-textarea-count]");
    if (!textarea || !counter) {
        return;
    }

    // The DOM property (unlike the attribute) is -1 when maxlength isn't set.
    const hasMax = textarea.maxLength >= 0;

    function update() {
        const length = textarea.value.length;
        counter.textContent = hasMax ? `${length} / ${textarea.maxLength}` : `${length} characters`;

        const atLimit = hasMax && length >= textarea.maxLength;
        counter.classList.toggle("text-red-600", atLimit);
        counter.classList.toggle("dark:text-red-400", atLimit);
        counter.classList.toggle("text-slate-500", !atLimit);
        counter.classList.toggle("dark:text-slate-400", !atLimit);
    }

    // The browser already enforces maxlength on typing and pasting, so this
    // never needs to truncate anything itself - it only ever reflects
    // whatever length the field already legitimately has.
    textarea.addEventListener("input", update);
    update();
}

document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-textarea-counter]").forEach(initCharacterCount);
});
