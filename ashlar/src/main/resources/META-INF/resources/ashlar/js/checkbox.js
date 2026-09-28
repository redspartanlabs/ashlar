// The `indeterminate` state has no HTML attribute equivalent - it exists
// only as a DOM/IDL property, so this is the one thing about Checkbox that
// genuinely cannot be server-rendered and needs JavaScript at all.
//
// This is deliberately a one-time pass, not an ongoing per-instance
// controller like toast.js/select.js: once set, the browser owns
// `indeterminate` completely from here, including automatically clearing
// it back to false the moment a user actually toggles the box. There is no
// further state for this script to track.
document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll('[data-checkbox-indeterminate="true"]').forEach((checkbox) => {
        checkbox.indeterminate = true;
    });
});
