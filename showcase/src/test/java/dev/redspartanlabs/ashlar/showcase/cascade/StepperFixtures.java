package dev.redspartanlabs.ashlar.showcase.cascade;

import dev.redspartanlabs.ashlar.icon.Icon;
import dev.redspartanlabs.ashlar.icon.IconSvg;

/**
 * The markup the Stepper parity suite measures: what stepper.jte and stepper-item.jte render (migrated classes), and the same structure
 * with the pre-migration Tailwind classes (legacy), element for element. The legacy class strings come from {@link StepperLegacyMarkup}
 * (and the Icon's from {@link IconLegacyMarkup}); the migrated ones are the constants the templates and {@code StepperStyles} write, which
 * the contract tests pin and the real-page checks compare with what the showcase serves.
 *
 * <p>Elements are marked with {@code data-parity} (and a case name) in test fixtures only, so the shared probe can measure them. A Stepper
 * has no caller-projected markup (a label is text), so every element is the component's own.
 */
final class StepperFixtures {

    /** One Stepper Item to build: its label and its status ({@code completed}, {@code current} or {@code upcoming}). */
    record Entry(String name, String label, String status) {
    }

    private final StepperLegacyMarkup legacy;
    private final IconLegacyMarkup icons;

    StepperFixtures(StepperLegacyMarkup legacy, IconLegacyMarkup icons) {
        this.legacy = legacy;
        this.icons = icons;
    }

    static Entry entry(String name, String label, String status) {
        return new Entry(name, label, status);
    }

    private static String mark(boolean marked, String kind, String name) {
        return marked ? " data-parity=\"" + kind + "\" data-parity-case=\"" + name + "\"" : "";
    }

    static String statusText(String status) {
        return switch (status) {
            case "completed" -> "Completed";
            case "current" -> "Current step";
            default -> "Not started";
        };
    }

    private String part(String role, String state, boolean legacyClasses) {
        if (legacyClasses) {
            return legacy.classes(role.equals("connector") || role.equals("text") ? role : role + "-" + state);
        }
        return switch (role) {
            case "connector" -> "ash-stepper-item__connector";
            case "line" -> "ash-stepper-item__line";
            case "indicator" -> "ash-stepper-item__indicator";
            case "text" -> "ash-stepper-item__text";
            default -> "ash-stepper-item__label";
        };
    }

    private String svg(boolean legacyClasses, boolean marked, String name) {
        String classes = legacyClasses ? icons.legacyFor("ash-icon ash-icon--sm") : "ash-icon ash-icon--sm";
        return "<svg class=\"" + classes + "\"" + mark(marked, "glyph", name) + " viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\""
                + " stroke-linecap=\"round\" stroke-linejoin=\"round\">" + IconSvg.bodyFor(Icon.CHECK) + "</svg>";
    }

    /** One {@code <li>} exactly as stepper-item.jte renders it. */
    String item(Entry e, boolean legacyClasses, boolean marked) {
        String state = e.status();
        StringBuilder sb = new StringBuilder();
        sb.append("<li").append(state.equals("current") ? " aria-current=\"step\"" : "").append(" class=\"")
                .append(legacyClasses ? legacy.classes("item") : StepperLegacyMarkup.migratedItem(state)).append('"').append(mark(marked, "item", e.name())).append('>');
        sb.append("<div class=\"").append(part("connector", state, legacyClasses)).append("\" aria-hidden=\"true\"").append(mark(marked, "connector", e.name())).append('>')
                .append("<div class=\"").append(part("line", state, legacyClasses)).append('"').append(mark(marked, "line", e.name())).append("></div></div>");
        sb.append("<span aria-hidden=\"true\" class=\"").append(part("indicator", state, legacyClasses)).append('"').append(mark(marked, "indicator", e.name())).append('>');
        if (state.equals("completed")) {
            sb.append(svg(legacyClasses, marked, e.name()));
        }
        sb.append("</span>");
        sb.append("<span class=\"").append(part("text", state, legacyClasses)).append('"').append(mark(marked, "text", e.name())).append('>')
                .append("<span class=\"").append(part("label", state, legacyClasses)).append('"').append(mark(marked, "label", e.name())).append('>').append(e.label())
                .append("<span class=\"").append(legacyClasses ? legacy.classes("status") : "ash-sr-only").append('"').append(mark(marked, "status", e.name())).append(">, ")
                .append(statusText(state)).append("</span></span></span>");
        sb.append("</li>");
        return sb.toString();
    }

    /** The Stepper as stepper.jte renders it: the container wrapper and the ordered list. */
    String stepper(String name, String children, boolean legacyClasses, boolean marked) {
        return "<div class=\"" + (legacyClasses ? legacy.classes("stepper") : "ash-stepper") + "\"" + mark(marked, "stepper", name) + ">"
                + "<ol data-stepper class=\"" + (legacyClasses ? legacy.classes("list") : "ash-stepper__list") + "\"" + mark(marked, "list", name) + ">" + children + "</ol></div>";
    }
}
