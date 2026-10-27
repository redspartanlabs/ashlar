package dev.redspartanlabs.ashlar.showcase.cascade;

import dev.redspartanlabs.ashlar.icon.Icon;
import dev.redspartanlabs.ashlar.icon.IconSvg;

/**
 * The markup the List and List Item parity suites measure: what list.jte and list-item.jte render (migrated classes), and the same
 * structure with the pre-migration Tailwind classes (legacy), element for element. The legacy class strings come from
 * {@link ListLegacyMarkup} (and the Icon's from {@link IconLegacyMarkup}); the migrated ones are the constants the templates write, which
 * the contract tests pin and the real-page checks compare with what the showcase serves.
 *
 * <p>Elements are marked with {@code data-parity} (and a case name) in test fixtures only, so the shared probe can measure them. A filler
 * item is built unmarked: it is there to be the "other" row in a list, not to be measured.
 */
final class ListFixtures {

    /** One List Item to build: its parameters, as the template takes them. {@code trailing} is caller content (HTML) or null. */
    record Item(String name, String label, String description, boolean link, Icon icon, String trailing) {
    }

    private final ListLegacyMarkup legacy;
    private final IconLegacyMarkup icons;

    ListFixtures(ListLegacyMarkup legacy, IconLegacyMarkup icons) {
        this.legacy = legacy;
        this.icons = icons;
    }

    private String cls(String role, boolean legacyClasses) {
        return legacyClasses ? legacy.classes(role) : ListLegacyMarkup.MIGRATED.get(role);
    }

    private static String mark(boolean marked, String kind, String name) {
        return marked ? " data-parity=\"" + kind + "\" data-parity-case=\"" + name + "\"" : "";
    }

    private String svg(Icon icon, String migratedClasses, boolean legacyClasses, boolean marked, String kind, String name) {
        String classes = legacyClasses ? icons.legacyFor(migratedClasses) : migratedClasses;
        return "<svg class=\"" + classes + "\"" + mark(marked, kind, name) + " viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\""
                + " stroke-linecap=\"round\" stroke-linejoin=\"round\">" + IconSvg.bodyFor(icon) + "</svg>";
    }

    /** One {@code <li>} exactly as list-item.jte renders it (the legacy one has no class). {@code focusHook} marks the link row to be focused. */
    String item(Item it, boolean legacyClasses, boolean marked, boolean focusHook) {
        StringBuilder sb = new StringBuilder();
        sb.append(legacyClasses ? "<li" : "<li class=\"ash-list-item\"").append(mark(marked, "item", it.name())).append('>');
        String rowTag = it.link() ? "a" : "div";
        sb.append('<').append(rowTag).append(it.link() ? " href=\"#\"" : "").append(" class=\"").append(cls(it.link() ? "row-link" : "row", legacyClasses)).append('"')
                .append(focusHook ? " data-focus" : "").append(mark(marked, "row", it.name())).append('>');
        if (it.icon() != null) {
            sb.append("<span class=\"").append(cls("icon", legacyClasses)).append("\" aria-hidden=\"true\"").append(mark(marked, "icon", it.name())).append('>')
                    .append(svg(it.icon(), "ash-icon ash-icon--md", legacyClasses, marked, "glyph", it.name())).append("</span>");
        }
        sb.append("<span class=\"").append(cls("body", legacyClasses)).append('"').append(mark(marked, "body", it.name())).append('>')
                .append("<span class=\"").append(cls("label", legacyClasses)).append('"').append(mark(marked, "label", it.name())).append('>').append(it.label()).append("</span>");
        if (!it.description().isEmpty()) {
            sb.append("<span class=\"").append(cls("description", legacyClasses)).append('"').append(mark(marked, "description", it.name())).append('>')
                    .append(it.description()).append("</span>");
        }
        sb.append("</span>");
        if (it.trailing() != null) {
            sb.append("<span class=\"").append(cls("trailing", legacyClasses)).append('"').append(mark(marked, "trailing", it.name())).append('>').append(it.trailing()).append("</span>");
        }
        if (it.link()) {
            sb.append("<span class=\"").append(cls("chevron", legacyClasses)).append("\" aria-hidden=\"true\"").append(mark(marked, "chevron", it.name())).append('>')
                    .append(svg(Icon.CHEVRON_RIGHT, "ash-icon ash-icon--sm", legacyClasses, marked, "chevron-glyph", it.name())).append("</span>");
        }
        sb.append("</").append(rowTag).append("></li>");
        return sb.toString();
    }

    /** A raw {@code <li>} the caller wrote themselves: no class on either side. */
    static String rawLi(String text, boolean marked, String name) {
        return "<li" + mark(marked, "raw", name) + ">" + text + "</li>";
    }

    /** A raw {@code <div>} child: anything can be projected into {@code items}. */
    static String rawDiv(String text, boolean marked, String name) {
        return "<div" + mark(marked, "raw", name) + ">" + text + "</div>";
    }

    /** The list, as list.jte renders it: a {@code <ul>}, or an {@code <ol>} when ordered. */
    String list(String name, boolean ordered, String children, boolean legacyClasses, boolean marked, String kind) {
        String tag = ordered ? "ol" : "ul";
        return "<" + tag + " class=\"" + cls("list", legacyClasses) + "\"" + mark(marked, kind, name) + ">" + children + "</" + tag + ">";
    }

    /**
     * A swatch painted with the colour a link row's hover state paints. A hover cannot be held in a measurement, so the colour is compared
     * instead: the migrated one reads the row's hover property, the legacy one is the two background utilities the old hover used.
     */
    String hoverSwatch(boolean legacyClasses) {
        String size = "display:block;width:20px;height:20px";
        if (legacyClasses) {
            return "<ul style=\"list-style:none;margin:0;padding:0\"><li><span class=\"" + legacy.classes("hover-swatch") + "\" style=\"" + size + "\" data-parity=\"hover-swatch\" data-parity-case=\"hover\"></span></li></ul>";
        }
        return "<ul style=\"list-style:none;margin:0;padding:0\"><li class=\"ash-list-item\"><span style=\"" + size + ";background-color:var(--ash-list-item-hover)\" data-parity=\"hover-swatch\" data-parity-case=\"hover\"></span></li></ul>";
    }
}
