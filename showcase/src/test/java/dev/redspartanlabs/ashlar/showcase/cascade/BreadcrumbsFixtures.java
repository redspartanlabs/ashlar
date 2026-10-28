package dev.redspartanlabs.ashlar.showcase.cascade;

/**
 * The markup the Breadcrumbs parity suite measures: what breadcrumbs.jte and breadcrumb-item.jte render (migrated classes), and the same
 * structure with the pre-migration Tailwind classes (legacy), element for element. The legacy class strings come from
 * {@link BreadcrumbsLegacyMarkup}; the migrated ones are the constants the templates write, which the contract tests pin and the real-page
 * checks compare with what the showcase serves.
 *
 * <p>Elements are marked with {@code data-parity} (and a case name) in test fixtures only, so the shared probe can measure them. A trail has
 * no caller-projected markup (a label is text), so every element is the component's own. The item rules mirror breadcrumb-item.jte:
 * {@code current} wins over {@code href}, a missing or empty {@code href} is plain text, and only the current crumb is {@code aria-current}.
 */
final class BreadcrumbsFixtures {

    /** One crumb to build: its label, its link target ({@code null} for none) and whether it is the current page. */
    record Entry(String name, String label, String href, boolean current) {
        boolean isLink() {
            return !current && href != null && !href.isEmpty();
        }
    }

    private final BreadcrumbsLegacyMarkup legacy;

    BreadcrumbsFixtures(BreadcrumbsLegacyMarkup legacy) {
        this.legacy = legacy;
    }

    static Entry entry(String name, String label, String href, boolean current) {
        return new Entry(name, label, href, current);
    }

    private static String mark(boolean marked, String kind, String name) {
        return marked ? " data-parity=\"" + kind + "\" data-parity-case=\"" + name + "\"" : "";
    }

    /** One {@code <li>} exactly as breadcrumb-item.jte renders it. */
    String item(Entry e, boolean legacyClasses, boolean marked) {
        String separator = legacyClasses ? legacy.classes("separator") : "ash-breadcrumb-item__separator";
        StringBuilder sb = new StringBuilder();
        sb.append("<li class=\"").append(legacyClasses ? legacy.classes("item") : "ash-breadcrumb-item").append('"').append(mark(marked, "item", e.name())).append('>');
        sb.append("<span aria-hidden=\"true\" data-breadcrumb-separator class=\"").append(separator).append('"').append(mark(marked, "separator", e.name())).append(">/</span>");
        if (e.isLink()) {
            sb.append("<a href=\"").append(e.href()).append("\" class=\"").append(legacyClasses ? legacy.classes("link") : "ash-breadcrumb-item__link").append('"')
                    .append(mark(marked, "link", e.name())).append('>').append(e.label()).append("</a>");
        } else {
            String classes = legacyClasses ? legacy.classes(e.current() ? "current" : "text")
                    : e.current() ? "ash-breadcrumb-item__text ash-breadcrumb-item__text--current" : "ash-breadcrumb-item__text";
            sb.append("<span").append(e.current() ? " aria-current=\"page\"" : "").append(" class=\"").append(classes).append('"')
                    .append(mark(marked, e.current() ? "current" : "text", e.name())).append('>').append(e.label()).append("</span>");
        }
        return sb.append("</li>").toString();
    }

    /** The trail as breadcrumbs.jte renders it: the landmark and the ordered list. */
    String trail(String name, String children, boolean legacyClasses, boolean marked) {
        return "<nav aria-label=\"Breadcrumb\" data-breadcrumbs" + (legacyClasses ? "" : " class=\"ash-breadcrumbs\"") + mark(marked, "nav", name) + ">"
                + "<ol class=\"" + (legacyClasses ? legacy.classes("list") : "ash-breadcrumbs__list") + "\"" + mark(marked, "list", name) + ">" + children + "</ol></nav>";
    }

    /**
     * A swatch painted with the colour the link takes on hover. A hover cannot be held in a measurement, so the colour is compared through
     * a swatch instead: the migrated one reads the crumb's hover property, the legacy one is the palette colours the hover variants used.
     */
    String hoverSwatch(boolean legacyClasses) {
        // The swatch is the test's own element, not the component's, so it states its box model itself, identically on both sides.
        String size = "display:block;box-sizing:border-box;width:20px;height:20px";
        if (legacyClasses) {
            return "<ul style=\"list-style:none;margin:0;padding:0\"><li style=\"display:block\"><span class=\"" + legacy.classes("hover-swatch") + "\" style=\"" + size
                    + "\" data-parity=\"hover-swatch\" data-parity-case=\"hover\"></span></li></ul>";
        }
        return "<ul style=\"list-style:none;margin:0;padding:0\"><li class=\"ash-breadcrumb-item\" style=\"display:block\"><span style=\"" + size
                + ";background-color:var(--ash-breadcrumb-item-link-hover)\" data-parity=\"hover-swatch\" data-parity-case=\"hover\"></span></li></ul>";
    }
}
