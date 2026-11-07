package dev.redspartanlabs.ashlar.showcase.cascade;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What the Text Entry parity suites share: the migrated markup of the five components exactly as their templates render it (the contract tests pin the templates, and
 * {@link ShowcaseRuntimeTest} compares the REAL rendered pages with the legacy rewrite), the pages that carry it in each stylesheet mode, the in-page measurement, the comparison
 * (which normalises ONE thing, see {@link #visibleShadow}), and the trusted browser input.
 *
 * <p>The stylesheet modes: <b>L</b>, the legacy rewrite with the cumulative Tailwind oracle; <b>P</b>, the migrated markup with the application's real stylesheet (Tailwind and Ashlar
 * together); <b>S</b>, the legacy rewrite with only the utilities and preflight rules the fields used; <b>R</b>, the migrated markup with Ashlar's stylesheet alone. L against P is
 * "Tailwind present"; S against R is "Tailwind removed". Every page also links the skin of its configuration, last, as the showcase does.
 */
final class TextEntryBrowser {

    private TextEntryBrowser() {
    }

    // ---------------------------------------------------------------- the migrated markup

    private static final String SVG = "<svg class=\"ash-icon ash-icon--sm\" viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M9 5l7 7-7 7\"/></svg>";

    static Map<String, Object> o(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
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

    private static String label(String id, String label, boolean required) {
        return "<label for=\"" + id + "\" class=\"ash-field__label\">" + label + (required ? " <span class=\"ash-field__required\" aria-hidden=\"true\"> *</span>" : "") + "</label>";
    }

    private static String describedBy(String id, boolean help, boolean error, boolean count) {
        List<String> ids = new ArrayList<>();
        if (help) {
            ids.add(id + "-help");
        }
        if (error) {
            ids.add(id + "-error");
        }
        if (count) {
            ids.add(id + "-count");
        }
        return ids.isEmpty() ? null : String.join(" ", ids);
    }

    private static String helpError(String id, Map<String, Object> m, boolean inRow) {
        String help = s(m, "help", "");
        boolean showError = b(m, "error") && !s(m, "errMsg", "").isEmpty();
        return (!inRow && !help.isEmpty() ? "<p id=\"" + id + "-help\" class=\"ash-field__help\">" + help + "</p>" : "")
                + (showError ? "<p id=\"" + id + "-error\" class=\"ash-field__error\" role=\"alert\">" + s(m, "errMsg", "") + "</p>" : "");
    }

    /** A Text Input, as text-input.jte renders it. */
    static String text(Map<String, Object> m) {
        String id = s(m, "id", "t");
        boolean showError = b(m, "error") && !s(m, "errMsg", "").isEmpty();
        String classes = "ash-field__control ash-field__control--readonly" + (b(m, "leading") ? " ash-field__control--leading" : "") + (b(m, "trailing") ? " ash-field__control--trailing" : "");
        String mask = s(m, "mask", "");
        return "<div class=\"ash-field\" data-field=\"" + id + "\">" + label(id, s(m, "label", "Label"), b(m, "required"))
                + "<div class=\"ash-field__box\">" + (b(m, "leading") ? "<span class=\"ash-field__icon\">" + SVG + "</span>" : "")
                + "<input type=\"" + s(m, "type", "text") + "\" id=\"" + id + "\" name=\"" + id + "\" value=\"" + s(m, "value", "") + "\"" + attr("placeholder", s(m, "placeholder", ""))
                + attr("required", b(m, "required")) + attr("disabled", b(m, "disabled")) + attr("readonly", b(m, "readonly")) + attr("minlength", m.get("minlength")) + attr("maxlength", m.get("maxlength"))
                + attr("pattern", s(m, "pattern", "")) + attr("aria-invalid", b(m, "error") ? "true" : null) + attr("aria-describedby", describedBy(id, !s(m, "help", "").isEmpty(), showError, false))
                + attr("data-mask", mask) + " class=\"" + classes + "\">" + (b(m, "trailing") ? "<span class=\"ash-field__icon ash-field__icon--end\">" + SVG + "</span>" : "") + "</div>"
                + helpError(id, m, false) + "</div>";
    }

    /** A Number Input, as number-input.jte renders it. */
    static String number(Map<String, Object> m) {
        String id = s(m, "id", "n");
        boolean showError = b(m, "error") && !s(m, "errMsg", "").isEmpty();
        return "<div class=\"ash-field\" data-field=\"" + id + "\">" + label(id, s(m, "label", "Label"), b(m, "required"))
                + "<input type=\"number\" id=\"" + id + "\" name=\"" + id + "\" value=\"" + s(m, "value", "") + "\"" + attr("placeholder", s(m, "placeholder", "")) + attr("min", s(m, "min", ""))
                + attr("max", s(m, "max", "")) + attr("step", s(m, "step", "")) + attr("required", b(m, "required")) + attr("disabled", b(m, "disabled")) + attr("readonly", b(m, "readonly"))
                + attr("aria-invalid", b(m, "error") ? "true" : null) + attr("aria-describedby", describedBy(id, !s(m, "help", "").isEmpty(), showError, false))
                + " class=\"ash-field__control ash-field__control--readonly ash-field__control--narrow\">" + helpError(id, m, false) + "</div>";
    }

    /** A Textarea, as textarea.jte renders it. */
    static String textarea(Map<String, Object> m) {
        String id = s(m, "id", "a");
        boolean count = b(m, "count");
        boolean showError = b(m, "error") && !s(m, "errMsg", "").isEmpty();
        String help = s(m, "help", "");
        String value = s(m, "value", "");
        Object max = m.get("maxlength");
        String counter = max != null ? value.length() + " / " + max : value.length() + " characters";
        String classes = "ash-field__control ash-field__control--readonly ash-textarea__control" + (m.containsKey("resizable") && !b(m, "resizable") ? " ash-textarea__control--fixed" : "");
        return "<div class=\"ash-field\"" + (count ? " data-textarea-counter=\"true\"" : "") + " data-field=\"" + id + "\">" + label(id, s(m, "label", "Label"), b(m, "required"))
                + "<textarea id=\"" + id + "\" name=\"" + id + "\"" + attr("placeholder", s(m, "placeholder", "")) + attr("required", b(m, "required")) + attr("disabled", b(m, "disabled"))
                + attr("readonly", b(m, "readonly")) + attr("maxlength", max) + " rows=\"" + m.getOrDefault("rows", 4) + "\"" + attr("aria-invalid", b(m, "error") ? "true" : null)
                + attr("aria-describedby", describedBy(id, !help.isEmpty(), showError, count)) + " class=\"" + classes + "\">" + value + "</textarea>"
                + (!help.isEmpty() || count ? "<div class=\"ash-textarea__row\">" + (!help.isEmpty() ? "<p id=\"" + id + "-help\" class=\"ash-textarea__help\">" + help + "</p>" : "")
                        + (count ? "<span id=\"" + id + "-count\" data-textarea-count" + (b(m, "atLimit") ? " data-at-limit" : "") + " class=\"ash-textarea__count\">" + counter + "</span>" : "") + "</div>" : "")
                + helpError(id, m, true) + "</div>";
    }

    /** A Search Input, as search-input.jte renders it. */
    static String search(Map<String, Object> m) {
        String id = s(m, "id", "s");
        boolean showError = b(m, "error") && !s(m, "errMsg", "").isEmpty();
        boolean hasValue = !s(m, "value", "").isEmpty();
        return "<div class=\"ash-field\" data-search-input data-field=\"" + id + "\">" + label(id, s(m, "label", "Label"), b(m, "required"))
                + "<div class=\"ash-field__box\"><span class=\"ash-field__icon\">" + SVG + "</span><input type=\"search\" id=\"" + id + "\" name=\"" + id + "\" value=\"" + s(m, "value", "") + "\""
                + attr("placeholder", s(m, "placeholder", "Search...")) + attr("required", b(m, "required")) + attr("disabled", b(m, "disabled")) + attr("aria-invalid", b(m, "error") ? "true" : null)
                + attr("aria-describedby", describedBy(id, !s(m, "help", "").isEmpty(), showError, false)) + " data-search-input-field class=\"ash-field__control ash-search-input__field\">"
                + "<button type=\"button\"" + attr("disabled", b(m, "disabled")) + (hasValue ? "" : " hidden") + " aria-label=\"Clear search\" data-search-input-clear class=\"ash-search-input__clear\">" + SVG + "</button></div>"
                + helpError(id, m, false) + "</div>";
    }

    /** A Form Field around a caller's own control, as form-field.jte renders it. */
    static String formField(Map<String, Object> m) {
        String id = s(m, "id", "ff");
        String help = s(m, "help", "");
        return "<div class=\"ash-field\" data-field=\"" + id + "\">" + label(id, s(m, "label", "Label"), b(m, "required"))
                + "<input type=\"text\" id=\"" + id + "\" data-parity=\"raw\">" + (help.isEmpty() ? "" : "<p id=\"" + id + "-help\" class=\"ash-field__help\">" + help + "</p>")
                + (s(m, "errMsg", "").isEmpty() ? "" : "<p id=\"" + id + "-error\" class=\"ash-field__error\" role=\"alert\">" + s(m, "errMsg", "") + "</p>") + "</div>";
    }

    private static final Pattern TAG_START = Pattern.compile("<(label|div|span|p|input|textarea|button|svg)(?=[\\s>])");

    /** Marks every element of the component (the caller's own control is already marked raw) so the scoped legacy stylesheet can address exactly those. */
    static String marked(String html) {
        Matcher m = TAG_START.matcher(html);
        StringBuilder out = new StringBuilder();
        int at = 0;
        while (m.find()) {
            out.append(html, at, m.end());
            int close = html.indexOf('>', m.end());
            if (!html.substring(m.end(), close).contains("data-parity")) {
                out.append(" data-parity");
            }
            at = m.end();
        }
        return out.append(html.substring(at)).toString();
    }

    // ---------------------------------------------------------------- the parity fixtures

    /** One state of a field: its id suffix, the options that make it, and the pseudo-classes forced on its control while it is measured. */
    record State(String key, Map<String, Object> options, List<String> pseudo) {
    }

    static final List<State> STATES = List.of(
            new State("default", o(), List.of()), new State("hover", o(), List.of("hover")), new State("focus", o(), List.of("focus")), new State("focus+hover", o(), List.of("focus", "hover")),
            new State("disabled", o("disabled", true), List.of()), new State("disabled+hover", o("disabled", true), List.of("hover")),
            new State("readonly", o("readonly", true), List.of()), new State("readonly+hover", o("readonly", true), List.of("hover")), new State("readonly+focus", o("readonly", true), List.of("focus")),
            new State("readonly+focus+hover", o("readonly", true), List.of("focus", "hover")), new State("disabled+readonly", o("disabled", true, "readonly", true), List.of()),
            new State("invalid", o("error", true), List.of()), new State("invalid+hover", o("error", true), List.of("hover")), new State("invalid+focus", o("error", true), List.of("focus")),
            new State("invalid+focus+hover", o("error", true), List.of("focus", "hover")), new State("invalid+readonly", o("error", true, "readonly", true), List.of()),
            new State("invalid+readonly+hover", o("error", true, "readonly", true), List.of("hover")), new State("invalid+disabled", o("error", true, "disabled", true), List.of()),
            new State("placeholder", o("placeholder", "Type here"), List.of()), new State("placeholder+disabled", o("placeholder", "Type here", "disabled", true), List.of()),
            new State("filled", o("value", "Hello"), List.of()), new State("required", o("required", true), List.of()),
            new State("help", o("help", "Help text"), List.of()), new State("error-message", o("error", true, "errMsg", "Error text"), List.of()),
            new State("help+error", o("help", "Help text", "error", true, "errMsg", "Error text", "required", true), List.of()));

    /** Kinds and builders. The Search Input has no read-only state, so those states are not built for it. */
    static String build(String kind, Map<String, Object> options) {
        return switch (kind) {
            case "text" -> text(options);
            case "number" -> number(options);
            case "textarea" -> textarea(options);
            case "search" -> search(options);
            default -> throw new IllegalArgumentException(kind);
        };
    }

    static final List<String> KINDS = List.of("text", "number", "textarea", "search");

    /** Extra, structurally different fields: icons, widths, the counter, the clear button with and without a value, a long help text, the Form Field. */
    static final List<Map.Entry<String, String>> EXTRAS = List.of(
            Map.entry("text-leading", text(o("id", "x-lead", "leading", true, "placeholder", "p"))), Map.entry("text-trailing", text(o("id", "x-trail", "trailing", true, "placeholder", "p"))),
            Map.entry("text-both", text(o("id", "x-both", "leading", true, "trailing", true, "value", "v"))), Map.entry("text-email-pattern", text(o("id", "x-pat", "type", "email", "pattern", "[a-z]+", "maxlength", 12, "minlength", 2))),
            Map.entry("text-long-help", text(o("id", "x-long", "help", "A rather long help text that is certain to wrap onto a second line inside a six hundred pixel wide page, to show the wrapping and the line height."))),
            Map.entry("number-range", number(o("id", "x-num", "min", "0", "max", "10", "step", "1", "value", "5", "help", "0 to 10"))),
            Map.entry("textarea-counter", textarea(o("id", "x-count", "count", true, "maxlength", 20, "value", "abc", "help", "Notes"))),
            Map.entry("textarea-counter-limit", textarea(o("id", "x-limit", "count", true, "maxlength", 3, "value", "abc", "atLimit", true))),
            Map.entry("textarea-counter-nomax", textarea(o("id", "x-nomax", "count", true, "value", "hello"))),
            Map.entry("textarea-fixed", textarea(o("id", "x-fixed", "resizable", false, "rows", 2))), Map.entry("textarea-fixed-disabled", textarea(o("id", "x-fixed-d", "resizable", false, "disabled", true))),
            Map.entry("textarea-help-error", textarea(o("id", "x-the", "help", "Help", "error", true, "errMsg", "Error", "count", true, "maxlength", 9))),
            Map.entry("search-value", search(o("id", "x-sval", "value", "query"))), Map.entry("search-value-disabled", search(o("id", "x-svald", "value", "query", "disabled", true))),
            Map.entry("search-value-invalid", search(o("id", "x-sinv", "value", "query", "error", true, "errMsg", "Bad"))),
            Map.entry("form-field", formField(o("id", "x-ff", "required", true, "help", "Help", "errMsg", "Error"))));

    // ---------------------------------------------------------------- pages

    /** The configuration's stylesheets, then its skin last. */
    static String page(List<String> sheets, ParityConfigs.Config config, String bodyAttributes, String body, String scripts) {
        StringBuilder links = new StringBuilder();
        sheets.forEach(href -> links.append("<link rel=\"stylesheet\" href=\"").append(href).append("\">"));
        if (config.skin() != null) {
            links.append("<link rel=\"stylesheet\" href=\"/css/theme-").append(config.skin()).append(".css\">");
        }
        String skin = config.skin() == null ? "" : " data-skin=\"" + config.skin() + "\"";
        return "<!doctype html><html data-theme=\"" + config.theme() + "\"" + skin + "><head><meta charset=\"utf-8\">" + links
                + "<style>*,*::before,*::after{transition:none !important;animation-play-state:paused !important}</style></head><body" + bodyAttributes
                + " style=\"margin:8px;width:600px\"><div style=\"font-family:Arial,Helvetica,sans-serif;color:rgb(15, 23, 42)\">" + body + "</div>" + scripts + "</body></html>";
    }

    // ---------------------------------------------------------------- measurement

    static final List<String> PROPERTIES = List.of("display", "position", "box-sizing", "width", "height", "min-width", "max-width", "margin-top", "margin-right", "margin-bottom", "margin-left",
            "padding-top", "padding-right", "padding-bottom", "padding-left", "border-top-width", "border-right-width", "border-bottom-width", "border-left-width", "border-top-style",
            "border-top-color", "border-right-color", "border-bottom-color", "border-left-color", "border-top-left-radius", "border-top-right-radius", "border-bottom-right-radius",
            "border-bottom-left-radius", "background-color", "color", "opacity", "font-size", "font-weight", "line-height", "letter-spacing", "text-transform", "text-decoration-line",
            "text-align", "cursor", "resize", "box-shadow", "outline-style", "outline-width", "appearance", "pointer-events", "flex-grow", "flex-shrink", "align-items", "justify-content",
            "row-gap", "column-gap", "top", "right", "bottom", "left", "vertical-align", "overflow-x", "overflow-y", "white-space", "fill", "stroke");

    /**
     * Returns, for one field, a line per element and property (tab separated: field, element position and tag, property, value), plus the placeholder's colour and opacity and the
     * element boxes. The element's class is NOT part of its name (the two renderings do not share classes). Elements inside an svg are not measured. {@code props} is spliced in.
     */
    static String measureScript(String fieldId, boolean skipRaw) {
        return "(function(){var props=" + jsArray(PROPERTIES) + ";var out=[];var root=document.querySelector('[data-field=\"" + fieldId + "\"]');"
                + "var els=[root].concat([].slice.call(root.querySelectorAll('*'))).filter(function(e){return !(e.closest('svg')&&e.tagName.toLowerCase()!=='svg')&&!(" + skipRaw
                + "&&e.getAttribute('data-parity')==='raw')});"
                + "els.forEach(function(e,n){var s=getComputedStyle(e);var name='" + fieldId + ':' + "'+n+':'+e.tagName.toLowerCase();"
                + "props.forEach(function(p){out.push([name,p,s.getPropertyValue(p)].join('\\t'))});"
                + "var r=e.getBoundingClientRect();out.push([name,'@x',String(Math.round(r.x*100)/100)].join('\\t'));out.push([name,'@y',String(Math.round(r.y*100)/100)].join('\\t'));"
                + "out.push([name,'@w',String(Math.round(r.width*100)/100)].join('\\t'));out.push([name,'@h',String(Math.round(r.height*100)/100)].join('\\t'));"
                + "var t=e.tagName.toLowerCase();if(t==='input'||t==='textarea'){var ph=getComputedStyle(e,'::placeholder');out.push([name,'::placeholder color',ph.color].join('\\t'));out.push([name,'::placeholder opacity',ph.opacity].join('\\t'));}});"
                + "return out.join('\\n')})()";
    }

    private static String jsArray(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            sb.append(i == 0 ? "" : ",").append('\'').append(values.get(i)).append('\'');
        }
        return sb.append(']').toString();
    }

    // ---------------------------------------------------------------- comparison

    private static final Pattern SHADOW_LAYER_SPLIT = Pattern.compile(",\\s*(?![^(]*\\))");
    private static final Pattern ZERO_TRANSPARENT = Pattern.compile("^(?:rgba\\(\\s*0,\\s*0,\\s*0,\\s*0\\s*\\)|transparent)\\s+0px\\s+0px\\s+0px\\s+0px$|^0px\\s+0px\\s+0px\\s+0px\\s+(?:rgba\\(\\s*0,\\s*0,\\s*0,\\s*0\\s*\\)|transparent)$");

    /**
     * The one normalisation (decision D11): a computed {@code box-shadow} with its PROVABLY invisible layers removed. A layer is invisible only when its colour is fully transparent
     * AND its offsets, blur and spread are all zero. Tailwind composes a shadow from inset-shadow, inset-ring, ring-offset, ring and drop layers and leaves the unused ones as such
     * transparent zero layers; the migrated component writes only the layers it draws. Every visible layer (a colour, an offset, a blur, a spread) is compared exactly.
     */
    static String visibleShadow(String computed) {
        if (computed == null || computed.equals("none")) {
            return "none";
        }
        List<String> kept = new ArrayList<>();
        for (String layer : SHADOW_LAYER_SPLIT.split(computed.trim())) {
            if (!ZERO_TRANSPARENT.matcher(layer.trim()).matches()) {
                kept.add(layer.trim());
            }
        }
        return kept.isEmpty() ? "none" : String.join(", ", kept);
    }

    /** Differences between two measurements of the same field (lines as {@link #measureScript} returns them); exact except {@link #visibleShadow}. */
    static List<String> differences(String where, String expected, String actual, Map<String, Integer> applied) {
        List<String> found = new ArrayList<>();
        String[] e = expected.split("\n");
        String[] a = actual.split("\n");
        if (e.length != a.length) {
            found.add(where + " | #lines | " + e.length + " -> " + a.length);
            return found;
        }
        for (int i = 0; i < e.length; i++) {
            String[] x = e[i].split("\t", -1);
            String[] y = a[i].split("\t", -1);
            if (!x[0].equals(y[0]) || !x[1].equals(y[1])) {
                found.add(where + " | structure | " + x[0] + " " + x[1] + " -> " + y[0] + " " + y[1]);
                continue;
            }
            String ex = x[1].equals("box-shadow") ? visibleShadow(x[2]) : x[2];
            String ac = y[1].equals("box-shadow") ? visibleShadow(y[2]) : y[2];
            if (x[1].equals("box-shadow") && !x[2].equals(y[2]) && ex.equals(ac)) {
                applied.merge("invisible-shadow-layers", 1, Integer::sum);
            }
            if (!ex.equals(ac)) {
                found.add(where + " | " + x[0] + " | " + x[1] + " | " + ex + " -> " + ac);
            }
        }
        return found;
    }

    // ---------------------------------------------------------------- trusted input

    private record Key(String key, String code, int virtualKey, String text) {
    }

    private static Key key(String name) {
        return switch (name) {
            case "Tab" -> new Key("Tab", "Tab", 9, null);
            case "Enter" -> new Key("Enter", "Enter", 13, "\r");
            case "Space" -> new Key(" ", "Space", 32, " ");
            case "Escape" -> new Key("Escape", "Escape", 27, null);
            case "Backspace" -> new Key("Backspace", "Backspace", 8, null);
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

    private static void typeText(DevToolsSession cdp, String text) throws Exception {
        for (char c : text.toCharArray()) {
            String t = String.valueOf(c);
            cdp.call("Input.dispatchKeyEvent", "{\"type\":\"keyDown\",\"text\":" + DevToolsSession.quote(t) + ",\"key\":" + DevToolsSession.quote(t) + ",\"unmodifiedText\":" + DevToolsSession.quote(t) + "}");
            cdp.call("Input.dispatchKeyEvent", "{\"type\":\"keyUp\",\"key\":" + DevToolsSession.quote(t) + "}");
        }
    }

    private static String centre(DevToolsSession cdp, String selector) throws Exception {
        String xy = cdp.evaluate("(function(){var e=document.querySelector(" + DevToolsSession.quote(selector) + ");e.scrollIntoView({block:\"center\"});var r=e.getBoundingClientRect();return (r.left+r.width/2)+\",\"+(r.top+r.height/2)})()");
        String[] p = xy.split(",");
        return "\"x\":" + p[0] + ",\"y\":" + p[1];
    }

    static void hover(DevToolsSession cdp, String selector) throws Exception {
        cdp.call("Input.dispatchMouseEvent", "{\"type\":\"mouseMoved\"," + centre(cdp, selector) + ",\"button\":\"none\"}");
    }

    static void click(DevToolsSession cdp, String selector) throws Exception {
        String at = centre(cdp, selector);
        cdp.call("Input.dispatchMouseEvent", "{\"type\":\"mouseMoved\"," + at + ",\"button\":\"none\"}");
        cdp.call("Input.dispatchMouseEvent", "{\"type\":\"mousePressed\"," + at + ",\"button\":\"left\",\"buttons\":1,\"clickCount\":1}");
        cdp.call("Input.dispatchMouseEvent", "{\"type\":\"mouseReleased\"," + at + ",\"button\":\"left\",\"buttons\":0,\"clickCount\":1}");
    }

    /** One step: {@code key:Tab}, {@code key:Shift+Tab}, {@code type:abc}, {@code click:selector}, {@code hover:selector}, {@code focus:id} (a script focus) or {@code none}. All but the last two are trusted browser input. */
    static void perform(DevToolsSession cdp, String action) throws Exception {
        String[] parts = action.split(":", 2);
        switch (parts[0]) {
            case "key" -> {
                boolean shift = parts[1].startsWith("Shift+");
                dispatchKey(cdp, key(shift ? parts[1].substring(6) : parts[1]), shift ? 8 : 0);
            }
            case "type" -> typeText(cdp, parts[1]);
            case "click" -> click(cdp, parts[1]);
            case "hover" -> hover(cdp, parts[1]);
            case "focus" -> cdp.evaluate("(function(){document.getElementById(" + DevToolsSession.quote(parts[1]) + ").focus();return \"ok\"})()");
            case "none" -> { }
            default -> throw new IllegalArgumentException(action);
        }
    }
}
