package dev.redspartanlabs.ashlar.showcase.cascade;

/**
 * The markup the Pagination parity suite measures: what pagination.jte and pagination-item.jte render (migrated classes), and the same
 * structure with the pre-migration Tailwind classes (legacy), element for element. The legacy class strings come from
 * {@link PaginationLegacyMarkup}; the migrated ones are the constants the templates write, which the contract tests pin and the real-page
 * checks compare with what the showcase serves.
 *
 * <p>Elements are marked with {@code data-parity} (and a case name) in test fixtures only, so the shared probe can measure them. A bar has
 * no caller-projected markup (a label is text), so every element is the component's own. The item rules mirror pagination-item.jte:
 * {@code current} wins over {@code href}, a missing or empty {@code href} on a non-current entry is a disabled, non-interactive entry.
 */
final class PaginationFixtures {

    static final String LINK = "ash-pagination-item__control ash-pagination-item__control--link";
    static final String CURRENT = "ash-pagination-item__control ash-pagination-item__control--current";
    static final String DISABLED = "ash-pagination-item__control ash-pagination-item__control--disabled";

    /** One entry to build: its label, its link target ({@code null} for none) and whether it is the current page. */
    record Entry(String name, String label, String href, boolean current) {
        boolean isLink() {
            return !current && href != null && !href.isEmpty();
        }

        /** The kind of entry it renders as: {@code link}, {@code current} or {@code disabled}. */
        String kind() {
            return isLink() ? "link" : current ? "current" : "disabled";
        }
    }

    private final PaginationLegacyMarkup legacy;

    PaginationFixtures(PaginationLegacyMarkup legacy) {
        this.legacy = legacy;
    }

    static Entry entry(String name, String label, String href, boolean current) {
        return new Entry(name, label, href, current);
    }

    private static String mark(boolean marked, String kind, String name) {
        return marked ? " data-parity=\"" + kind + "\" data-parity-case=\"" + name + "\"" : "";
    }

    /** One {@code <li>} exactly as pagination-item.jte renders it. */
    String item(Entry e, boolean legacyClasses, boolean marked) {
        StringBuilder sb = new StringBuilder();
        sb.append("<li").append(legacyClasses ? "" : " class=\"ash-pagination-item\"").append(mark(marked, "item", e.name())).append('>');
        switch (e.kind()) {
            case "link" -> sb.append("<a href=\"").append(e.href()).append("\" class=\"").append(legacyClasses ? legacy.classes("link") : LINK).append('"')
                    .append(mark(marked, "link", e.name())).append('>').append(e.label()).append("</a>");
            case "current" -> sb.append("<span aria-current=\"page\" class=\"").append(legacyClasses ? legacy.classes("current") : CURRENT).append('"')
                    .append(mark(marked, "current", e.name())).append('>').append(e.label()).append("</span>");
            default -> sb.append("<span aria-disabled=\"true\" class=\"").append(legacyClasses ? legacy.classes("disabled") : DISABLED).append('"')
                    .append(mark(marked, "disabled", e.name())).append('>').append(e.label()).append("</span>");
        }
        return sb.append("</li>").toString();
    }

    /** The bar as pagination.jte renders it: the landmark and the list. */
    String bar(String name, String children, boolean legacyClasses, boolean marked) {
        return "<nav aria-label=\"Pagination\" data-pagination" + (legacyClasses ? "" : " class=\"ash-pagination\"") + mark(marked, "nav", name) + ">"
                + "<ul class=\"" + (legacyClasses ? legacy.classes("list") : "ash-pagination__list") + "\"" + mark(marked, "list", name) + ">" + children + "</ul></nav>";
    }

    /**
     * Two swatches painted with the colours a link takes on hover: its text colour and its fill. A hover cannot be held in a measurement, so
     * those colours are compared through swatches instead: the migrated ones read the entry's hover properties, the legacy ones are the
     * palette colours the hover variants used.
     */
    String hoverSwatches(boolean legacyClasses) {
        // The swatches are the test's own elements, not the component's, so they state their box model themselves, identically on both sides.
        String size = "display:block;box-sizing:border-box;width:20px;height:20px";
        String text = "data-parity=\"hover-swatch\" data-parity-case=\"hover\"";
        String fill = "data-parity=\"hover-fill-swatch\" data-parity-case=\"hover-fill\"";
        if (legacyClasses) {
            return "<ul style=\"list-style:none;margin:0;padding:0\"><li style=\"display:block\"><span class=\"" + legacy.classes("hover-swatch") + "\" style=\"" + size + "\" " + text
                    + "></span><span class=\"" + legacy.classes("hover-fill-swatch") + "\" style=\"" + size + "\" " + fill + "></span></li></ul>";
        }
        return "<ul style=\"list-style:none;margin:0;padding:0\"><li class=\"ash-pagination-item\" style=\"display:block\"><span style=\"" + size
                + ";background-color:var(--ash-pagination-item-link-hover)\" " + text + "></span><span style=\"" + size
                + ";background-color:var(--ash-pagination-item-link-hover-bg)\" " + fill + "></span></li></ul>";
    }
}
