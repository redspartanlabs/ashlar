package dev.redspartanlabs.ashlar.showcase.cascade;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What the Choice Controls (Checkbox and Radio Group) suites share: the migrated markup exactly as the templates render it (the contract tests pin the templates, and
 * {@link ChoiceLegacyMarkupTest} ties this mirror to the shipped templates), the fixtures every parity suite renders, the in-page measurement, and the trusted browser input.
 * The pages, the comparison and its single normalisation are Text Entry's ({@link TextEntryBrowser}): a computed {@code box-shadow} with only its fully transparent zero-size
 * layers removed, which Tailwind's ring composition leaves in and the migrated rule does not write.
 */
final class ChoiceBrowser {

    private ChoiceBrowser() {
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

    private static String required(boolean required) {
        return required ? " <span class=\"ash-field__required\" aria-hidden=\"true\"> *</span>" : "";
    }

    /** A Checkbox, as checkbox.jte renders it. */
    static String checkbox(Map<String, Object> m) {
        String id = s(m, "id", "c");
        boolean help = !s(m, "help", "").isEmpty();
        boolean error = b(m, "error") && !s(m, "errMsg", "").isEmpty();
        return "<div class=\"ash-checkbox\" data-field=\"" + id + "\"><div class=\"ash-choice\"><input type=\"checkbox\" id=\"" + id + "\" name=\"" + id + "\" value=\"" + s(m, "value", "on") + "\""
                + attr("checked", b(m, "checked")) + attr("disabled", b(m, "disabled")) + attr("required", b(m, "required")) + attr("aria-invalid", b(m, "error") ? "true" : null)
                + attr("aria-describedby", describedBy(id, help, error)) + attr("data-checkbox-indeterminate", b(m, "indeterminate") ? "true" : null) + " class=\"ash-choice__input\">"
                + "<div class=\"ash-choice__body\"><label for=\"" + id + "\" class=\"ash-choice__label\">" + s(m, "label", "Label") + required(b(m, "required")) + "</label>"
                + (help ? "<p id=\"" + id + "-help\" class=\"ash-choice__description\">" + s(m, "help", "") + "</p>" : "")
                + (error ? "<p id=\"" + id + "-error\" class=\"ash-choice__error\" role=\"alert\">" + s(m, "errMsg", "") + "</p>" : "") + "</div></div></div>";
    }

    /** One option: value, label, description (may be empty), disabled. */
    record Option(String value, String label, String description, boolean disabled) {
    }

    static final List<Option> OPTIONS = List.of(new Option("a", "Alpha", "First option", false), new Option("b", "Beta", "", false), new Option("c", "Gamma", "Third option", true));

    /** A Radio Group, as radio-group.jte renders it. */
    @SuppressWarnings("unchecked")
    static String radioGroup(Map<String, Object> m) {
        String id = s(m, "id", "g");
        boolean help = !s(m, "help", "").isEmpty();
        boolean error = b(m, "error") && !s(m, "errMsg", "").isEmpty();
        List<Option> options = m.containsKey("options") ? (List<Option>) m.get("options") : OPTIONS;
        StringBuilder rows = new StringBuilder();
        for (Option op : options) {
            String oid = id + "-" + op.value();
            rows.append("<div class=\"ash-choice\"><input type=\"radio\" id=\"").append(oid).append("\" name=\"").append(id).append("\" value=\"").append(op.value()).append("\"")
                    .append(attr("checked", op.value().equals(s(m, "selected", "")))).append(attr("disabled", op.disabled())).append(attr("required", b(m, "required")))
                    .append(attr("aria-invalid", b(m, "error") ? "true" : null)).append(" class=\"ash-choice__input\"><div class=\"ash-choice__body\"><label for=\"").append(oid)
                    .append("\" class=\"ash-choice__label\">").append(op.label()).append("</label>")
                    .append(op.description().isEmpty() ? "" : "<p class=\"ash-choice__description\">" + op.description() + "</p>").append("</div></div>");
        }
        return "<fieldset class=\"ash-radio-group\" data-field=\"" + id + "\"" + attr("disabled", b(m, "disabled")) + attr("aria-describedby", describedBy(id, help, error))
                + "><legend class=\"ash-radio-group__legend\">" + s(m, "label", "Group") + required(b(m, "required")) + "</legend><div class=\"ash-radio-group__options"
                + (b(m, "horizontal") ? " ash-radio-group__options--horizontal" : "") + "\">" + rows + "</div>"
                + (help ? "<p id=\"" + id + "-help\" class=\"ash-choice__description ash-radio-group__help\">" + s(m, "help", "") + "</p>" : "")
                + (error ? "<p id=\"" + id + "-error\" class=\"ash-choice__error ash-radio-group__error\" role=\"alert\">" + s(m, "errMsg", "") + "</p>" : "") + "</fieldset>";
    }

    private static final Pattern TAG_START = Pattern.compile("<(label|div|span|p|input|fieldset|legend)(?=[\\s>])");

    /** Marks every element of the component so the scoped legacy stylesheet can address exactly those. */
    static String marked(String html) {
        Matcher m = TAG_START.matcher(html);
        StringBuilder out = new StringBuilder();
        int at = 0;
        while (m.find()) {
            out.append(html, at, m.end());
            if (!html.substring(m.end(), html.indexOf('>', m.end())).contains("data-parity")) {
                out.append(" data-parity");
            }
            at = m.end();
        }
        return out.append(html.substring(at)).toString();
    }

    // ---------------------------------------------------------------- the parity fixtures

    /** A control state: the field options, and the pseudo-classes forced on {@code target} while it is measured (target null: the field's own control). */
    record Case(String key, String kind, Map<String, Object> options, List<String> pseudo, String target) {
    }

    static final List<Case> CASES = new ArrayList<>();

    static {
        List<Object[]> checkbox = List.of(
                new Object[] {"unchecked", o(), List.of()}, new Object[] {"checked", o("checked", true), List.of()}, new Object[] {"indeterminate", o("indeterminate", true), List.of()},
                new Object[] {"disabled", o("disabled", true, "help", "Help text"), List.of()}, new Object[] {"disabled checked", o("disabled", true, "checked", true), List.of()},
                new Object[] {"required", o("required", true), List.of()}, new Object[] {"invalid", o("error", true), List.of()},
                new Object[] {"invalid checked", o("error", true, "checked", true), List.of()}, new Object[] {"help and error", o("help", "Help text", "error", true, "errMsg", "Error text", "required", true), List.of()},
                new Object[] {"focus-visible", o(), List.of("focus", "focus-visible")}, new Object[] {"checked focus-visible", o("checked", true), List.of("focus", "focus-visible")},
                new Object[] {"indeterminate focus-visible", o("indeterminate", true), List.of("focus", "focus-visible")}, new Object[] {"invalid focus-visible", o("error", true), List.of("focus", "focus-visible")},
                new Object[] {"disabled focus-visible", o("disabled", true), List.of("focus", "focus-visible")}, new Object[] {"focus only", o(), List.of("focus")},
                new Object[] {"hover", o(), List.of("hover")}, new Object[] {"active", o(), List.of("active")}, new Object[] {"disabled hover", o("disabled", true), List.of("hover")});
        for (Object[] c : checkbox) {
            @SuppressWarnings("unchecked") Map<String, Object> opts = (Map<String, Object>) c[1];
            @SuppressWarnings("unchecked") List<String> pseudo = (List<String>) c[2];
            CASES.add(new Case((String) c[0], "checkbox", opts, pseudo, null));
        }
        List<Option> twoDisabled = List.of(new Option("a", "Alpha", "First option", false), new Option("b", "Beta", "Second option", false), new Option("c", "Gamma", "Third option", true));
        List<Object[]> radio = List.of(
                new Object[] {"selected", o("selected", "b"), List.of(), null}, new Object[] {"none selected", o(), List.of(), null}, new Object[] {"with descriptions", o("options", twoDisabled, "selected", "a"), List.of(), null},
                new Object[] {"whole group disabled", o("disabled", true, "selected", "a", "options", twoDisabled), List.of(), null},
                new Object[] {"one option disabled", o("selected", "a", "options", twoDisabled), List.of(), null}, new Object[] {"horizontal", o("horizontal", true, "selected", "a"), List.of(), null},
                new Object[] {"horizontal with descriptions", o("horizontal", true, "options", twoDisabled), List.of(), null}, new Object[] {"required", o("required", true), List.of(), null},
                new Object[] {"invalid", o("error", true, "errMsg", "Choose one", "help", "Pick one", "selected", "a"), List.of(), null},
                new Object[] {"help only", o("help", "Pick one"), List.of(), null}, new Object[] {"required and invalid", o("required", true, "error", true, "errMsg", "E"), List.of(), null},
                new Object[] {"focus-visible on checked", o("selected", "b"), List.of("focus", "focus-visible"), "b"}, new Object[] {"focus-visible on unchecked", o("selected", "b"), List.of("focus", "focus-visible"), "a"},
                new Object[] {"hover on option", o("selected", "b"), List.of("hover"), "a"}, new Object[] {"disabled option focus-visible", o("selected", "b"), List.of("focus", "focus-visible"), "c"});
        for (Object[] c : radio) {
            @SuppressWarnings("unchecked") Map<String, Object> opts = (Map<String, Object>) c[1];
            @SuppressWarnings("unchecked") List<String> pseudo = (List<String>) c[2];
            CASES.add(new Case((String) c[0], "radio", opts, pseudo, (String) c[3]));
        }
    }

    static String build(Case c, String id) {
        Map<String, Object> options = new LinkedHashMap<>(c.options());
        options.put("id", id);
        return c.kind().equals("checkbox") ? checkbox(options) : radioGroup(options);
    }

    /** The selector the forced pseudo-classes go on: the checkbox itself, or the named radio option (the first for none). */
    static String target(Case c, String id) {
        return c.kind().equals("checkbox") ? "#" + id : "#" + id + "-" + (c.target() == null ? "a" : c.target());
    }

    /** Sets the DOM-only {@code indeterminate} property that checkbox.js sets, so a parity page needs no module loading. */
    static final String INDETERMINATE = "<script>document.querySelectorAll('[data-checkbox-indeterminate=\"true\"]').forEach(function(e){e.indeterminate=true})</script>";

    // ---------------------------------------------------------------- measurement

    static final List<String> PROPERTIES = new ArrayList<>(TextEntryBrowser.PROPERTIES);

    static {
        PROPERTIES.addAll(List.of("accent-color", "flex-wrap", "flex-direction", "flex-basis", "outline-offset", "outline-color", "min-width", "color-scheme"));
    }

    private static String jsArray(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            sb.append(i == 0 ? "" : ",").append('\'').append(values.get(i)).append('\'');
        }
        return sb.append(']').toString();
    }

    /** One line per element and property (tab separated: field, element position and tag, property, value), plus every element's box. Elements inside an svg are not measured. */
    static String measureScript(String fieldId) {
        return "(function(){var props=" + jsArray(PROPERTIES) + ";var out=[];var root=document.querySelector('[data-field=\"" + fieldId + "\"]');"
                + "var els=[root].concat([].slice.call(root.querySelectorAll('*')));"
                + "els.forEach(function(e,n){var s=getComputedStyle(e);var name='" + fieldId + ":'+n+':'+e.tagName.toLowerCase();"
                + "props.forEach(function(p){out.push([name,p,s.getPropertyValue(p)].join('\\t'))});"
                + "var r=e.getBoundingClientRect();out.push([name,'@x',String(Math.round(r.x*100)/100)].join('\\t'));out.push([name,'@y',String(Math.round(r.y*100)/100)].join('\\t'));"
                + "out.push([name,'@w',String(Math.round(r.width*100)/100)].join('\\t'));out.push([name,'@h',String(Math.round(r.height*100)/100)].join('\\t'));});"
                + "return out.join('\\n')})()";
    }

    // ---------------------------------------------------------------- trusted input

    private record Key(String key, String code, int virtualKey, String text) {
    }

    private static Key key(String name) {
        return switch (name) {
            case "Tab" -> new Key("Tab", "Tab", 9, null);
            case "Space" -> new Key(" ", "Space", 32, " ");
            case "Enter" -> new Key("Enter", "Enter", 13, "\r");
            case "ArrowDown" -> new Key("ArrowDown", "ArrowDown", 40, null);
            case "ArrowUp" -> new Key("ArrowUp", "ArrowUp", 38, null);
            case "ArrowLeft" -> new Key("ArrowLeft", "ArrowLeft", 37, null);
            case "ArrowRight" -> new Key("ArrowRight", "ArrowRight", 39, null);
            default -> throw new IllegalArgumentException(name);
        };
    }

    private static void dispatchKey(DevToolsSession cdp, Key k, int modifiers) throws Exception {
        String common = "\"key\":" + DevToolsSession.quote(k.key()) + ",\"code\":" + DevToolsSession.quote(k.code()) + ",\"windowsVirtualKeyCode\":" + k.virtualKey()
                + ",\"nativeVirtualKeyCode\":" + k.virtualKey() + ",\"modifiers\":" + modifiers;
        cdp.call("Input.dispatchKeyEvent", "{\"type\":\"rawKeyDown\"," + common + "}");
        if (k.text() != null) {
            cdp.call("Input.dispatchKeyEvent", "{\"type\":\"char\",\"text\":" + DevToolsSession.quote(k.text()) + "," + common + "}");
        }
        cdp.call("Input.dispatchKeyEvent", "{\"type\":\"keyUp\"," + common + "}");
    }

    /** {@code key:Tab}, {@code key:Shift+Tab}, {@code key:ArrowDown}, {@code key:Space}, {@code click:selector}, {@code focus:id} (a script focus) or {@code none}. */
    static void perform(DevToolsSession cdp, String action) throws Exception {
        String[] parts = action.split(":", 2);
        switch (parts[0]) {
            case "key" -> {
                boolean shift = parts[1].startsWith("Shift+");
                dispatchKey(cdp, key(shift ? parts[1].substring(6) : parts[1]), shift ? 8 : 0);
            }
            case "click" -> TextEntryBrowser.click(cdp, parts[1]);
            case "focus" -> cdp.evaluate("(function(){document.getElementById(" + DevToolsSession.quote(parts[1]) + ").focus();return \"ok\"})()");
            case "none" -> { }
            default -> throw new IllegalArgumentException(action);
        }
    }
}
