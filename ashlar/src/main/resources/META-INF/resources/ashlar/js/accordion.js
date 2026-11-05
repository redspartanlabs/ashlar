// Toggling is a real <button>'s native click; there is no custom keyboard
// handling here because Tab/Enter/Space already work for free on a real
// button. Each accordion's items are nested inside its own root, so this
// only ever searches within `root` - never `document` - which is what
// keeps multiple Accordion instances on one page from ever seeing each
// other's buttons, without needing any shared id-prefix scoping.
//
// No height is ever measured and no style or class is ever written: the
// state is two attributes, kept in step here, and accordion.css answers both.
// `aria-expanded` on the button turns the chevron; `inert` on the panel is the
// collapsed grid row (`0fr`, `1fr` otherwise), and it is also what takes the
// collapsed content out of the tab order and the accessibility tree, which a
// `0fr` row would not do by itself.
function initAccordion(root) {
    const buttons = Array.from(root.querySelectorAll("[data-accordion-button]"));

    function setExpanded(button, expanded) {
        const panel = document.getElementById(button.getAttribute("aria-controls"));
        button.setAttribute("aria-expanded", String(expanded));
        panel.inert = !expanded;
    }

    function toggle(button) {
        const isExpanded = button.getAttribute("aria-expanded") === "true";

        if (isExpanded) {
            setExpanded(button, false);
            return;
        }

        // Single-open contract: opening this item closes whichever sibling
        // (if any) was open before it. At most one of `buttons` is ever
        // expanded at a time, so this is always at most one other button.
        buttons.forEach((other) => {
            if (other !== button && other.getAttribute("aria-expanded") === "true") {
                setExpanded(other, false);
            }
        });
        setExpanded(button, true);
    }

    buttons.forEach((button) => {
        button.addEventListener("click", () => toggle(button));
    });
}

document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-accordion]").forEach(initAccordion);
});
