package dev.redspartanlabs.ashlar.showcase.cascade;

import dev.redspartanlabs.ashlar.button.ButtonSize;
import dev.redspartanlabs.ashlar.button.ButtonStyles;
import dev.redspartanlabs.ashlar.button.ButtonVariant;
import dev.redspartanlabs.ashlar.icon.Icon;
import dev.redspartanlabs.ashlar.icon.IconSvg;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What the File Upload suites share: the migrated markup exactly as file-upload.jte renders it (the contract tests pin the template, and {@link FileUploadLegacyMarkupTest} ties this mirror
 * to the shipped template), the fixtures every parity suite renders, the in-page measurement, the page that carries the component's own script, and the files a test selects. The pages, the
 * comparison and its single other normalisation are Text Entry's ({@link TextEntryBrowser}); the trusted browser input is Choice's ({@link ChoiceBrowser#perform}).
 *
 * <p>The one documented difference is Toggle's: Tailwind's {@code sr-only} sets {@code clip-path: inset(50%)} and Ashlar's {@code ash-sr-only} sets that AND {@code clip: rect(0, 0, 0, 0)}, so
 * the hidden input's computed {@code clip} is {@code auto} in the legacy rendering and {@code rect(0px, 0px, 0px, 0px)} in the migrated one ({@link ToggleBrowser#setAsideSrOnlyClip}).
 */
final class FileUploadBrowser {

    private FileUploadBrowser() {
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

    /** The Icon as icon.jte renders it for {@code Icon.UPLOAD} with the class {@code ash-icon--lg}. */
    static String icon() {
        return "<svg class=\"ash-icon ash-icon--lg\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\">"
                + IconSvg.bodyFor(Icon.UPLOAD) + "</svg>";
    }

    /**
     * A File Upload, as file-upload.jte renders it (without the script tag, which {@link #page} carries). Options: id, name, label, multiple, accept, required, disabled, help, error,
     * errMsg. The root also carries {@code data-field}, which only the measurement uses.
     */
    static String fileUpload(Map<String, Object> m) {
        String id = s(m, "id", "f");
        String label = s(m, "label", "Upload Document");
        boolean multiple = b(m, "multiple");
        boolean help = !s(m, "help", "").isEmpty();
        boolean error = b(m, "error") && !s(m, "errMsg", "").isEmpty();
        String idle = multiple ? "Choose files to upload" : "Choose a file to upload";
        String choose = ButtonStyles.classes(ButtonVariant.SECONDARY, ButtonSize.SMALL, false, false, false);
        return "<div class=\"ash-field ash-file-upload\" data-file-upload data-field=\"" + id + "\"><label for=\"" + id + "\" class=\"ash-field__label\">" + label
                + (b(m, "required") ? " <span class=\"ash-field__required\" aria-hidden=\"true\"> *</span>" : "") + "</label>"
                + "<label class=\"ash-file-upload__zone\"><input type=\"file\" id=\"" + id + "\" name=\"" + s(m, "name", id) + "\"" + attr("multiple", multiple) + attr("accept", s(m, "accept", ""))
                + attr("required", b(m, "required")) + attr("disabled", b(m, "disabled")) + " aria-label=\"" + label + "\"" + attr("aria-invalid", b(m, "error") ? "true" : null)
                + attr("aria-describedby", describedBy(id, help, error)) + " data-file-upload-input class=\"ash-file-upload__input ash-sr-only\">"
                + "<span aria-hidden=\"true\" class=\"ash-file-upload__icon\">" + icon() + "</span>"
                + "<p data-file-upload-status data-idle-text=\"" + idle + "\" class=\"ash-file-upload__status\">" + idle + "</p>"
                + "<span class=\"" + choose + " ash-file-upload__choose\">Choose File</span></label>"
                + (help ? "<p id=\"" + id + "-help\" class=\"ash-field__help\">" + s(m, "help", "") + "</p>" : "")
                + (error ? "<p id=\"" + id + "-error\" class=\"ash-field__error\" role=\"alert\">" + s(m, "errMsg", "") + "</p>" : "") + "</div>";
    }

    // ---------------------------------------------------------------- the parity fixtures

    static final String LONG_NAME = "a-really-quite-long-quarterly-financial-report-final-version-2026-revised-again-and-again.pdf";
    static final String LONG_LABEL = "A long label that is long enough to wrap onto a second line when the upload sits in a narrow column of text";

    /** A state: the options, the pseudo-classes forced on {@code target} while it is measured ("input" the real file input, "zone" the card), the files selected first, and a column width. */
    record Case(String key, Map<String, Object> options, List<String> pseudo, String target, List<String> files, String width) {
    }

    static final List<Case> CASES = new ArrayList<>();

    private static void add(String key, Map<String, Object> options, List<String> pseudo, String target, List<String> files, String width) {
        CASES.add(new Case(key, options, pseudo, target, files, width));
    }

    static {
        add("idle", o(), List.of(), "input", List.of(), null);
        add("multiple", o("multiple", true), List.of(), "input", List.of(), null);
        add("accept", o("accept", ".pdf,application/pdf", "help", "Supported format: PDF only."), List.of(), "input", List.of(), null);
        add("required", o("required", true), List.of(), "input", List.of(), null);
        add("disabled", o("disabled", true), List.of(), "input", List.of(), null);
        add("invalid", o("error", true, "errMsg", "Error text"), List.of(), "input", List.of(), null);
        add("invalid without message", o("error", true), List.of(), "input", List.of(), null);
        add("help", o("help", "Help text"), List.of(), "input", List.of(), null);
        add("help and error", o("help", "Help text", "error", true, "errMsg", "Error text", "required", true), List.of(), "input", List.of(), null);
        add("long label", o("label", LONG_LABEL), List.of(), "input", List.of(), null);
        add("focus-visible", o(), List.of("focus", "focus-visible"), "input", List.of(), null);
        add("focus only", o(), List.of("focus"), "input", List.of(), null);
        add("invalid focus-visible", o("error", true, "errMsg", "Error text"), List.of("focus", "focus-visible"), "input", List.of(), null);
        add("disabled focus-visible", o("disabled", true), List.of("focus", "focus-visible"), "input", List.of(), null);
        add("hover zone", o(), List.of("hover"), "zone", List.of(), null);
        add("hover zone disabled", o("disabled", true), List.of("hover"), "zone", List.of(), null);
        add("hover zone invalid", o("error", true, "errMsg", "Error text"), List.of("hover"), "zone", List.of(), null);
        add("hover zone disabled invalid", o("disabled", true, "error", true, "errMsg", "Error text"), List.of("hover"), "zone", List.of(), null);
        add("hover zone selected", o(), List.of("hover"), "zone", List.of("report.pdf"), null);
        add("active zone", o(), List.of("active"), "zone", List.of(), null);
        add("selected one", o(), List.of(), "input", List.of("report.pdf"), null);
        add("selected three", o("multiple", true), List.of(), "input", List.of("a.txt", "b.txt", "c.txt"), null);
        add("selected long name", o(), List.of(), "input", List.of(LONG_NAME), null);
        add("narrow 240 idle", o(), List.of(), "input", List.of(), "240px");
        add("narrow 240 long name", o(), List.of(), "input", List.of(LONG_NAME), "240px");
        add("narrow 120 idle", o(), List.of(), "input", List.of(), "120px");
        add("narrow 120 long name", o(), List.of(), "input", List.of(LONG_NAME), "120px");
    }

    /** Marks every element of the component (the svg included) so the scoped legacy stylesheet can address exactly those. */
    private static final Pattern TAG_START = Pattern.compile("<(label|div|span|p|input|svg)(?=[\\s>])");

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

    static String build(Case c, String id) {
        Map<String, Object> options = new LinkedHashMap<>(c.options());
        options.put("id", id);
        String html = fileUpload(options);
        return c.width() == null ? html : "<div style=\"width:" + c.width() + "\">" + html + "</div>";
    }

    /** The element the forced pseudo-classes go on: the real file input, or the card (the label after the field label). */
    static String target(Case c, String id) {
        return c.target().equals("zone") ? "[data-field=\"" + id + "\"] > label:nth-of-type(2)" : "#" + id;
    }

    // ---------------------------------------------------------------- files and the page

    private static Path files;

    /** The files a case may select, created once in a temporary directory outside the repository. */
    static synchronized Path file(String name) throws IOException {
        if (files == null) {
            files = Files.createTempDirectory("ashlar-file-upload-");
            files.toFile().deleteOnExit();
            for (String n : List.of("report.pdf", "a.txt", "b.txt", "c.txt", LONG_NAME)) {
                Path p = Files.writeString(files.resolve(n), "x");
                p.toFile().deleteOnExit();
            }
        }
        return files.resolve(name);
    }

    /** The component's own script, exactly as shipped, as a module script (what the template's script tag loads). */
    static String script() throws IOException {
        try (InputStream in = FileUploadBrowser.class.getResourceAsStream("/META-INF/resources/ashlar/js/file-upload.js")) {
            if (in == null) {
                throw new IOException("file-upload.js is not on the test classpath");
            }
            return "<script type=\"module\">" + new String(in.readAllBytes(), StandardCharsets.UTF_8) + "</script>";
        }
    }

    /** Selects files in the real file input exactly as the browser's own file chooser would (the trusted DevTools call, which fires input and change). */
    static void select(DevToolsSession cdp, String selector, List<String> names) throws Exception {
        List<Integer> nodes = cdp.nodes(selector);
        StringBuilder list = new StringBuilder("[");
        for (int i = 0; i < names.size(); i++) {
            list.append(i == 0 ? "" : ",").append(DevToolsSession.quote(file(names.get(i)).toString()));
        }
        cdp.call("DOM.setFileInputFiles", "{\"nodeId\":" + nodes.get(0) + ",\"files\":" + list + "]}");
    }

    // ---------------------------------------------------------------- measurement

    static final List<String> PROPERTIES = new ArrayList<>(ToggleBrowser.PROPERTIES);

    static {
        PROPERTIES.addAll(List.of("text-overflow", "outline-offset", "outline-color", "stroke-width", "stroke-linecap", "stroke-linejoin", "pointer-events", "flex-direction"));
    }

    private static String jsArray(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            sb.append(i == 0 ? "" : ",").append('\'').append(values.get(i)).append('\'');
        }
        return sb.append(']').toString();
    }

    /**
     * One line per element and property (tab separated: field, element position and tag, property, value), plus every element's box and the status text. The Icon's children are not
     * measured (the svg itself is): they carry no class of their own and no rule reaches them.
     */
    static String measureScript(String fieldId) {
        return "(function(){var props=" + jsArray(PROPERTIES) + ";var out=[];var root=document.querySelector('[data-field=\"" + fieldId + "\"]');"
                + "var els=[root].concat([].slice.call(root.querySelectorAll('*'))).filter(function(e){return !(e.closest('svg')&&e.tagName.toLowerCase()!=='svg')});"
                + "els.forEach(function(e,n){var s=getComputedStyle(e);var name='" + fieldId + ":'+n+':'+e.tagName.toLowerCase();"
                + "props.forEach(function(p){out.push([name,p,s.getPropertyValue(p)].join('\\t'))});"
                + "var r=e.getBoundingClientRect();out.push([name,'@x',String(Math.round(r.x*100)/100)].join('\\t'));out.push([name,'@y',String(Math.round(r.y*100)/100)].join('\\t'));"
                + "out.push([name,'@w',String(Math.round(r.width*100)/100)].join('\\t'));out.push([name,'@h',String(Math.round(r.height*100)/100)].join('\\t'));"
                + "if(e.hasAttribute('data-file-upload-status')){out.push([name,'@text',e.textContent].join('\\t'));out.push([name,'@scrollWidth',String(e.scrollWidth)].join('\\t'));}});"
                + "return out.join('\\n')})()";
    }

    /** See {@link ToggleBrowser#setAsideSrOnlyClip}: the one documented difference of the hidden input's computed clip. */
    static String setAsideSrOnlyClip(String measurement, boolean migrated, Map<String, Integer> counts) {
        return ToggleBrowser.setAsideSrOnlyClip(measurement, migrated, counts);
    }
}
