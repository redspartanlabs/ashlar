// Toggling is a real <button>'s native click; there is no custom keyboard
// handling here because Tab/Enter/Space already work for free on a real
// button. Each accordion's items are nested inside its own root, so this
// only ever searches within `root` - never `document` - which is what
// keeps multiple Accordion instances on one page from ever seeing each
// other's buttons, without needing any shared id-prefix scoping.
//
// No height is ever measured: the panel's open/closed size is a CSS grid
// row (`0fr` closed, `1fr` open, set here purely via inline style - see
// accordion-item.jte's own comment on why), and the browser's layout engine
// resolves what "open" actually measures to. `inert` toggles alongside it
// because a `0fr` row - unlike the `hidden` class this used to use - keeps
// its content in the tab order and accessibility tree unless something
// explicitly takes it back out.
function initAccordion(root) {
    const buttons = Array.from(root.querySelectorAll("[data-accordion-button]"));

    function setExpanded(button, expanded) {
        const panel = document.getElementById(button.getAttribute("aria-controls"));
        button.setAttribute("aria-expanded", String(expanded));
        panel.style.gridTemplateRows = expanded ? "1fr" : "0fr";
        panel.inert = !expanded;

        const chevron = button.querySelector("[data-accordion-chevron]");
        chevron.classList.toggle("rotate-90", expanded);
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
