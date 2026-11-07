// TEST-ONLY FIXTURE. NOT served by the showcase and NOT part of the Ashlar library.
//
// Provenance: a verbatim copy of ashlar/src/main/resources/META-INF/resources/ashlar/js/textarea.js exactly as it was at commit 831a2f5
// ("feat: migrate Tree and Tree Item to native Ashlar CSS"), i.e. the textarea script BEFORE the Text Entry CSS migration.
//   git blob id of the original : c4cd28776f0043b5402748dfb8a4cb12bb9a7aab
//   SHA-256 of everything below the marker line : a145ceab73eb22efd2c868abb201fc1037abdd3e9f85e39f617eed4c59106303
//   (TextEntryBehaviourTest recomputes the hash, so an edit to the copy fails that test.)
//
// Purpose: the migrated script no longer toggles Tailwind classes (the counter's text-red-600 / text-slate-500 pairs). TextEntryBehaviourTest runs the SAME trusted
// keyboard and mouse sequence against (a) the legacy Tailwind markup with this original script and (b) the migrated markup with the migrated script, each in its OWN browser,
// and requires the observable behaviour to be identical at every step. Do not edit the copy below the marker; do not use it for anything else.
// ==== END OF PROVENANCE HEADER (the original file follows byte for byte) ====
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
