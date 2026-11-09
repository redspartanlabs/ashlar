package dev.redspartanlabs.ashlar.showcase.cascade;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the Toggle suites share: the migrated markup exactly as toggle.jte renders it (the contract tests pin the template, and {@link ToggleLegacyMarkupTest} ties this mirror to the
 * shipped template), the fixtures every parity suite renders, the in-page measurement, and the narrow normalisation of the one documented difference between the hidden input's two
 * utilities. The pages, the comparison and its single other normalisation are Text Entry's ({@link TextEntryBrowser}); the trusted browser input is Choice's ({@link ChoiceBrowser#perform}).
 *
 * <p>The documented difference: Tailwind's {@code sr-only} sets {@code clip-path: inset(50%)} and Ashlar's {@code ash-sr-only} sets that AND {@code clip: rect(0, 0, 0, 0)}, so the
 * hidden input's computed {@code clip} is {@code auto} in the legacy rendering and {@code rect(0px, 0px, 0px, 0px)} in the migrated one. Only that one property of that one element is
 * set aside, only with exactly those two values ({@link #setAsideSrOnlyClip}); the pixel suite shows it paints nothing.
 */
final class ToggleBrowser {

    private ToggleBrowser() {
    }

    static Map<String, Object> o(Object... kv) {
        return TextEntryBrowser.o(kv);
    }

    private static String s(Map<String, Object> m, String k, String d) {
        return m.containsKey(k) ? (String) m.get(k) : d;
    }

    private static boolean b(Map<String, Object> m, String k) {
        return Boolean.TRUE.equals(m.get(k));
    }

    private static String attr(String name, Object v) {
        if (v == null || Boolean.FALSE.equals(v) || "".equals(v)) {
            return "";
        }
        return Boolean.TRUE.equals(v) ? " " + name : " " + name + "=\"" + v + "\"";
    }

    private static String describedBy(String id, boolean help, boolean error) {
        return help && error ? id + "-help " + id + "-error" : help ? id + "-help" : error ? id + "-error" : null;
    }

    /** A Toggle, as toggle.jte renders it. Options: id, name, label, value, checked, disabled, required, help, error, errMsg, start. */
    static String toggle(Map<String, Object> m) {
        String id = s(m, "id", "t");
        boolean help = !s(m, "help", "").isEmpty();
        boolean error = b(m, "error") && !s(m, "errMsg", "").isEmpty();
        return "<div class=\"ash-toggle\" data-field=\"" + id + "\"><label class=\"ash-toggle__label" + (b(m, "start") ? " ash-toggle__label--start" : "") + "\"><span class=\"ash-toggle__switch\">"
                + "<input type=\"checkbox\" id=\"" + id + "\" name=\"" + s(m, "name", id) + "\" value=\"" + s(m, "value", "on") + "\"" + attr("checked", b(m, "checked")) + attr("disabled", b(m, "disabled"))
                + attr("required", b(m, "required")) + attr("aria-invalid", b(m, "error") ? "true" : null) + attr("aria-describedby", describedBy(id, help, error)) + " class=\"ash-toggle__input ash-sr-only\">"
                + "<span aria-hidden=\"true\" class=\"ash-toggle__track\"></span><span aria-hidden=\"true\" class=\"ash-toggle__thumb\"></span></span>"
                + "<span class=\"ash-toggle__text\">" + s(m, "label", "Label") + (b(m, "required") ? " <span class=\"ash-field__required\" aria-hidden=\"true\"> *</span>" : "") + "</span></label>"
                + (help ? "<p id=\"" + id + "-help\" class=\"ash-choice__description ash-toggle__help\">" + s(m, "help", "") + "</p>" : "")
                + (error ? "<p id=\"" + id + "-error\" class=\"ash-choice__error ash-toggle__error\" role=\"alert\">" + s(m, "errMsg", "") + "</p>" : "") + "</div>";
    }

    // ---------------------------------------------------------------- the parity fixtures

    /** A state: the options, the pseudo-classes forced on {@code target} while it is measured ("input" the real checkbox, "track", "text"). */
    record Case(String key, Map<String, Object> options, List<String> pseudo, String target) {
    }

    private static final String LONG = "A long label that is long enough to wrap onto a second line when the toggle sits in a narrow column of text";

    static final List<Case> CASES = new ArrayList<>();

    static {
        Object[][] states = {
                {"unchecked", o(), List.of(), "input"}, {"checked", o("checked", true), List.of(), "input"}, {"disabled", o("disabled", true), List.of(), "input"},
                {"disabled checked", o("disabled", true, "checked", true), List.of(), "input"}, {"required", o("required", true), List.of(), "input"}, {"invalid", o("error", true), List.of(), "input"},
                {"invalid checked", o("error", true, "checked", true), List.of(), "input"}, {"help and error", o("help", "Help text", "error", true, "errMsg", "Error text", "required", true), List.of(), "input"},
                {"help only", o("help", "Help text"), List.of(), "input"},
                {"label start", o("start", true), List.of(), "input"}, {"label start checked", o("start", true, "checked", true), List.of(), "input"},
                {"label start with help", o("start", true, "help", "Help text", "required", true), List.of(), "input"}, {"label start disabled", o("start", true, "disabled", true), List.of(), "input"},
                {"long label", o("label", LONG), List.of(), "input"},
                {"focus-visible", o(), List.of("focus", "focus-visible"), "input"}, {"checked focus-visible", o("checked", true), List.of("focus", "focus-visible"), "input"},
                {"invalid focus-visible", o("error", true), List.of("focus", "focus-visible"), "input"}, {"invalid checked focus-visible", o("error", true, "checked", true), List.of("focus", "focus-visible"), "input"},
                {"disabled focus-visible", o("disabled", true), List.of("focus", "focus-visible"), "input"}, {"focus only", o(), List.of("focus"), "input"},
                {"hover track", o(), List.of("hover"), "track"}, {"hover track checked", o("checked", true), List.of("hover"), "track"}, {"hover track disabled", o("disabled", true), List.of("hover"), "track"},
                {"hover track disabled checked", o("disabled", true, "checked", true), List.of("hover"), "track"}, {"hover track invalid", o("error", true), List.of("hover"), "track"},
                {"active track", o(), List.of("active"), "track"},
                {"hover text", o(), List.of("hover"), "text"}, {"hover text disabled", o("disabled", true), List.of("hover"), "text"}};
        for (Object[] c : states) {
            @SuppressWarnings("unchecked") Map<String, Object> opts = (Map<String, Object>) c[1];
            @SuppressWarnings("unchecked") List<String> pseudo = (List<String>) c[2];
            CASES.add(new Case((String) c[0], opts, pseudo, (String) c[3]));
        }
    }

    /** Marks every element of the component so the scoped legacy stylesheet can address exactly those (Choice's marker, which knows these tags). */
    static String marked(String html) {
        return ChoiceBrowser.marked(html);
    }

    static String build(Case c, String id) {
        Map<String, Object> options = new LinkedHashMap<>(c.options());
        options.put("id", id);
        return toggle(options);
    }

    /** The element the forced pseudo-classes go on: the real checkbox, the track (its next sibling) or the label text. */
    static String target(Case c, String id) {
        return switch (c.target()) {
            case "track" -> "#" + id + " + span";
            case "text" -> "[data-field=\"" + id + "\"] label > span:last-child";
            default -> "#" + id;
        };
    }

    // ---------------------------------------------------------------- measurement

    static final List<String> PROPERTIES = new ArrayList<>(ChoiceBrowser.PROPERTIES);

    static {
        PROPERTIES.addAll(List.of("transition-property", "transition-duration", "transition-timing-function", "transition-delay", "translate", "transform", "clip", "clip-path", "inset-inline-start"));
    }

    private static String jsArray(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            sb.append(i == 0 ? "" : ",").append('\'').append(values.get(i)).append('\'');
        }
        return sb.append(']').toString();
    }

    /** One line per element and property (tab separated: field, element position and tag, property, value), plus every element's box. */
    static String measureScript(String fieldId) {
        return "(function(){var props=" + jsArray(PROPERTIES) + ";var out=[];var root=document.querySelector('[data-field=\"" + fieldId + "\"]');"
                + "var els=[root].concat([].slice.call(root.querySelectorAll('*')));"
                + "els.forEach(function(e,n){var s=getComputedStyle(e);var name='" + fieldId + ":'+n+':'+e.tagName.toLowerCase();"
                + "props.forEach(function(p){out.push([name,p,s.getPropertyValue(p)].join('\\t'))});"
                + "var r=e.getBoundingClientRect();out.push([name,'@x',String(Math.round(r.x*100)/100)].join('\\t'));out.push([name,'@y',String(Math.round(r.y*100)/100)].join('\\t'));"
                + "out.push([name,'@w',String(Math.round(r.width*100)/100)].join('\\t'));out.push([name,'@h',String(Math.round(r.height*100)/100)].join('\\t'));});"
                + "return out.join('\\n')})()";
    }

    /**
     * Removes, from a measurement, the hidden input's {@code clip} line and counts it, so the documented ash-sr-only difference is set aside and nothing else is. The value must be exactly
     * the one the side is documented to have (legacy: auto; migrated: rect(0px, 0px, 0px, 0px)): any other value is a real difference and stays in.
     */
    static String setAsideSrOnlyClip(String measurement, boolean migrated, Map<String, Integer> counts) {
        String expected = migrated ? "rect(0px, 0px, 0px, 0px)" : "auto";
        StringBuilder kept = new StringBuilder();
        for (String line : measurement.split("\n")) {
            String[] p = line.split("\t", -1);
            if (p.length == 3 && p[0].endsWith(":input") && p[1].equals("clip") && p[2].equals(expected)) {
                counts.merge("sr-only-clip-" + (migrated ? "migrated" : "legacy"), 1, Integer::sum);
                continue;
            }
            kept.append(line).append('\n');
        }
        return kept.toString().stripTrailing();
    }
}
