package dev.redspartanlabs.ashlar.showcase.cascade;

import dev.redspartanlabs.ashlar.icon.Icon;
import dev.redspartanlabs.ashlar.icon.IconSvg;

/**
 * The markup the Timeline parity suite measures: what timeline.jte and timeline-item.jte render (migrated classes), and the same structure
 * with the pre-migration Tailwind classes (legacy), element for element. The legacy class strings come from {@link TimelineLegacyMarkup}
 * (and the Icon's from {@link IconLegacyMarkup}); the migrated ones are the constants the templates and {@code TimelineStyles} write, which
 * the contract tests pin and the real-page checks compare with what the showcase serves.
 *
 * <p>Elements are marked with {@code data-parity} (and a case name) in test fixtures only, so the shared probe can measure them. The
 * projected content is a paragraph the caller wrote ({@code content-child}); it carries an explicit zero margin, so the size of the
 * component-owned content wrapper around it does not depend on whether Tailwind's preflight (which used to zero a paragraph's margin) is
 * present, and the wrapper stays compared everywhere.
 */
final class TimelineFixtures {

    /** One Timeline Item to build: its parameters as the template takes them. {@code content} is the caller's text, or null; {@code variant} is a lower-case variant name. */
    record Entry(String name, String title, String date, String content, Icon icon, String variant) {
    }

    private final TimelineLegacyMarkup legacy;
    private final IconLegacyMarkup icons;

    TimelineFixtures(TimelineLegacyMarkup legacy, IconLegacyMarkup icons) {
        this.legacy = legacy;
        this.icons = icons;
    }

    static Entry entry(String name, String title, String date, String content, Icon icon, String variant) {
        return new Entry(name, title, date, content, icon, variant);
    }

    private String cls(String role, boolean legacyClasses) {
        return legacyClasses ? legacy.classes(role) : TimelineLegacyMarkup.MIGRATED.get(role);
    }

    private static String mark(boolean marked, String kind, String name) {
        return marked ? " data-parity=\"" + kind + "\" data-parity-case=\"" + name + "\"" : "";
    }

    static String statusText(String variant) {
        return switch (variant) {
            case "info" -> "Info: ";
            case "success" -> "Success: ";
            case "warning" -> "Warning: ";
            case "danger" -> "Danger: ";
            default -> "";
        };
    }

    private String svg(Icon icon, boolean legacyClasses, boolean marked, String name) {
        String classes = legacyClasses ? icons.legacyFor("ash-icon ash-icon--sm") : "ash-icon ash-icon--sm";
        return "<svg class=\"" + classes + "\"" + mark(marked, "glyph", name) + " viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\""
                + " stroke-linecap=\"round\" stroke-linejoin=\"round\">" + IconSvg.bodyFor(icon) + "</svg>";
    }

    /** One {@code <li>} exactly as timeline-item.jte renders it. {@code contentHtml} replaces the entry's plain caller paragraph when it is not null (a nested Timeline). */
    String item(Entry e, boolean legacyClasses, boolean marked, String contentHtml) {
        StringBuilder sb = new StringBuilder();
        sb.append("<li class=\"").append(cls("item", legacyClasses)).append('"').append(mark(marked, "item", e.name())).append('>');
        sb.append("<div class=\"").append(cls("rail", legacyClasses)).append('"').append(mark(marked, "rail", e.name())).append('>');
        sb.append("<span aria-hidden=\"true\" class=\"").append(legacyClasses ? legacy.marker(e.variant()) : TimelineLegacyMarkup.MIGRATED.get("marker-" + e.variant())).append('"')
                .append(mark(marked, "marker", e.name())).append('>');
        if (e.icon() != null) {
            sb.append(svg(e.icon(), legacyClasses, marked, e.name()));
        }
        sb.append("</span>");
        sb.append("<div class=\"").append(cls("connector", legacyClasses)).append('"').append(mark(marked, "connector", e.name())).append("></div></div>");
        sb.append("<div class=\"").append(cls("body", legacyClasses)).append('"').append(mark(marked, "body", e.name())).append('>');
        if (!e.date().isEmpty()) {
            sb.append("<p class=\"").append(cls("date", legacyClasses)).append('"').append(mark(marked, "date", e.name())).append('>').append(e.date()).append("</p>");
        }
        sb.append("<p class=\"").append(cls("title", legacyClasses)).append('"').append(mark(marked, "title", e.name())).append('>');
        String status = statusText(e.variant());
        if (!status.isEmpty()) {
            sb.append("<span class=\"").append(cls("status", legacyClasses)).append('"').append(mark(marked, "status", e.name())).append('>').append(status).append("</span>");
        }
        sb.append(e.title()).append("</p>");
        if (contentHtml != null) {
            sb.append("<div class=\"").append(cls("content", legacyClasses)).append('"').append(mark(marked, "content", e.name())).append('>').append(contentHtml).append("</div>");
        } else if (e.content() != null) {
            sb.append("<div class=\"").append(cls("content", legacyClasses)).append('"').append(mark(marked, "content", e.name())).append('>')
                    .append("<p style=\"margin:0\"").append(mark(marked, "content-child", e.name())).append('>').append(e.content()).append("</p></div>");
        }
        sb.append("</div></li>");
        return sb.toString();
    }

    String item(Entry e, boolean legacyClasses, boolean marked) {
        return item(e, legacyClasses, marked, null);
    }

    /** The timeline, as timeline.jte renders it. */
    String timeline(String name, String children, boolean legacyClasses, boolean marked, String kind) {
        return "<ol data-timeline class=\"" + cls("timeline", legacyClasses) + "\"" + mark(marked, kind, name) + ">" + children + "</ol>";
    }
}
