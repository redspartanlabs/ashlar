package dev.redspartanlabs.ashlar.showcase.cascade;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What the Modal and Drawer suites share: the REAL templates rendered in this process, the pages, the scripts a page runs (the real ones, the legacy copies or a broken
 * variant), trusted browser input, and one snapshot of everything an overlay changes.
 *
 * <p>The markup is not a hand-written mirror: {@link #modal} and {@link #drawer} run {@code ashlar/components/modal.jte} and {@code drawer.jte} through the JTE engine, so a
 * class, an attribute or a hook the templates write is the one every suite measures. The legacy markup is that same markup rewritten by {@link OverlayLegacyMarkup} and
 * {@link IconLegacyMarkup}.
 *
 * <p>Scripts are real ES modules loaded over HTTP, never inlined: modal.js and drawer.js import {@code ./overlay.js}, which only resolves against a real URL. The real scripts
 * come from {@code /ashlar/js/} (the library on the test classpath, at the path the application serves them from); a legacy copy or a broken variant is a registered script
 * set ({@link CascadeFixtureServer#registerScripts}) the template's script tags are pointed at.
 *
 * <p>Input is TRUSTED, through the DevTools protocol ({@code Input.dispatchKeyEvent}, {@code Input.dispatchMouseEvent}): a trusted Tab really moves focus when nothing prevents
 * it, and a trusted key goes to the focused element first, so a nested component's own listener sees it before the document does, as with a real keyboard.
 */
final class OverlayBrowser {

    private OverlayBrowser() {
    }

    // ---------------------------------------------------------------- the real templates

    private static final TemplateEngine ENGINE = TemplateEngine.createPrecompiled(ContentType.Html);
    private static final String COMPONENTS = "ashlar/components/";

    static gg.jte.Content raw(String html) {
        return output -> output.writeContent(html);
    }

    private static String render(String template, Map<String, Object> params) {
        StringOutput out = new StringOutput();
        ENGINE.render(COMPONENTS + template, params, out);
        return out.toString();
    }

    /** Any Ashlar component template (by file name), rendered with these parameters. */
    static String renderComponent(String template, Map<String, Object> params) {
        return render(template, params);
    }

    /** Renders a component template into an output that is already being written (for a Content parameter made of other components). */
    static void renderInto(String template, Map<String, Object> params, gg.jte.TemplateOutput out) {
        ENGINE.render(COMPONENTS + template, params, out);
    }

    /** The Modal template, rendered with these parameters (body and footer are raw HTML, or absent). */
    static String modal(String id, String title, Map<String, Object> options, String body, String footer) {
        Map<String, Object> params = new LinkedHashMap<>(options);
        params.put("id", id);
        params.put("title", title);
        if (body != null) {
            params.put("body", raw(body));
        }
        if (footer != null) {
            params.put("footer", raw(footer));
        }
        return render("modal.jte", params);
    }

    /** The Drawer template, rendered with these parameters (body and footer are raw HTML, or absent). */
    static String drawer(String id, String title, String placement, String body, String footer) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("id", id);
        params.put("title", title);
        if (placement != null) {
            params.put("placement", placement);
        }
        if (body != null) {
            params.put("body", raw(body));
        }
        if (footer != null) {
            params.put("footer", raw(footer));
        }
        return render("drawer.jte", params);
    }

    static Map<String, Object> o(Object... pairs) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put((String) pairs[i], pairs[i + 1]);
        }
        return m;
    }

    private static final Pattern SCRIPT = Pattern.compile("<script type=\"module\" src=\"/ashlar/js/([a-z-]+\\.js)\"></script>");

    /** The markup without its script tags (a page that only measures CSS). */
    static String withoutScripts(String html) {
        return SCRIPT.matcher(html).replaceAll("");
    }

    /** The markup with its script tags pointed at {@code base} (a registered script set; it ends in "/"). */
    static String withScripts(String html, String base) {
        return SCRIPT.matcher(html).replaceAll(Matcher.quoteReplacement("<script type=\"module\" src=\"" + base) + "$1\"></script>");
    }

    /** The migrated markup in another state: shown (no hidden attribute) and open (data-state="open"), or shown but still in its closed visual state. */
    static String inState(String html, boolean shown, boolean open) {
        String out = html.replace("data-state=\"closed\"", "data-state=\"" + (open ? "open" : "closed") + "\"");
        return shown ? out.replaceAll("\\s+hidden(?=\\s*>)", "") : out;
    }

    /** The legacy rendering of migrated markup. */
    static String legacy(String migrated) throws IOException {
        return new IconLegacyMarkup().rewrite(new OverlayLegacyMarkup().rewrite(migrated));
    }

    private static final Pattern MARKABLE = Pattern.compile("<(div|h2|button|svg|p|span)(?=[\\s>])");

    /** Marks every element of the component so the scoped legacy stylesheet can address exactly those (the caller's own content is already marked raw). */
    static String marked(String html) {
        Matcher m = MARKABLE.matcher(html);
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

    static String script(String resource) throws IOException {
        try (InputStream in = OverlayBrowser.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException(resource + " is not on the test classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }

    // ---------------------------------------------------------------- the parity cases

    /** One measured case: its field id, a description, the migrated markup (marked, without scripts), and the pseudo-classes forced on its close button (none: nothing forced). */
    record Case(String id, String key, String markup, List<String> pseudo) {
    }

    // The caller's own content, marked raw. Its margin is stated so that it is the same on a page with Tailwind's preflight and on a bare page: what is compared is the overlay.
    private static final String BODY = "<p data-parity=\"raw\" style=\"margin:0\">The body of the dialog, a sentence long enough to wrap onto a second line inside the narrowest dialog.</p>";
    private static final String FOOTER = "<span data-parity=\"raw\">Cancel</span><span data-parity=\"raw\">Continue</span>";

    /** The overlay in one state: hidden (not rendered), shown but in its closed visual state, or open. */
    private static String state(String html, String state) {
        return switch (state) {
            case "hidden" -> html;
            case "closed" -> inState(html, true, false);
            case "open" -> inState(html, true, true);
            default -> throw new IllegalArgumentException(state);
        };
    }

    /** Every case the static suites measure: each size and placement, with and without a footer and a close button, in each state, and the close button's states. */
    static List<Case> cases() {
        List<Case> cases = new ArrayList<>();
        Map<String, String> markups = new LinkedHashMap<>();
        markups.put("modal small with footer", modal("x", "Confirm the change", o("size", "Small"), BODY, FOOTER));
        markups.put("modal medium", modal("x", "Account details", o(), BODY, null));
        markups.put("modal large with footer", modal("x", "A large dialog", o("size", "Large"), BODY, FOOTER));
        markups.put("modal without close button", modal("x", "No close button", o("showCloseButton", false), BODY, FOOTER));
        markups.put("modal without body", modal("x", "Title only", o(), null, null));
        markups.put("drawer right with footer", drawer("x", "Filters", null, BODY, FOOTER));
        markups.put("drawer left", drawer("x", "Menu", "Left", BODY, null));
        int n = 0;
        for (Map.Entry<String, String> m : markups.entrySet()) {
            for (String s : List.of("hidden", "closed", "open")) {
                String id = "ov" + (++n);
                cases.add(new Case(id, m.getKey() + " " + s, marked(withoutScripts(state(m.getValue(), s).replace("id=\"x\"", "id=\"" + id + "-o\"")
                        .replace("x-title", id + "-o-title").replace("=\"x\"", "=\"" + id + "-o\""))), List.of()));
            }
        }
        for (String forced : List.of("hover", "focus", "focus-visible")) {
            for (String which : List.of("modal small with footer", "drawer left")) {
                String id = "ov" + (++n);
                cases.add(new Case(id, which + " open, close button " + forced, marked(withoutScripts(state(markups.get(which), "open").replace("id=\"x\"", "id=\"" + id + "-o\"")
                        .replace("x-title", id + "-o-title").replace("=\"x\"", "=\"" + id + "-o\""))), forced.equals("focus-visible") ? List.of("focus", "focus-visible") : List.of(forced)));
            }
        }
        return cases;
    }

    /** The case's markup inside its measured field. */
    static String field(Case c, String markup) {
        return "<div data-field=\"" + c.id() + "\" data-parity=\"raw\">" + markup + "</div>";
    }

    static final List<String> PROPERTIES = new ArrayList<>(TextEntryBrowser.PROPERTIES);

    static {
        PROPERTIES.addAll(List.of("translate", "scale", "rotate", "transform", "z-index", "flex-direction", "flex-wrap", "flex-basis", "max-height", "min-height", "overscroll-behavior-x",
                "overscroll-behavior-y", "outline-color", "outline-offset", "font-family", "visibility", "inset-block-start"));
    }

    /** One line per element and property of the field (field:position:tag, property, value), and the element boxes. Inside an svg only the svg itself is measured. */
    static String measureScript(String fieldId) {
        StringBuilder props = new StringBuilder("[");
        for (int i = 0; i < PROPERTIES.size(); i++) {
            props.append(i == 0 ? "" : ",").append('\'').append(PROPERTIES.get(i)).append('\'');
        }
        props.append(']');
        return "(function(){var props=" + props + ";var out=[];var root=document.querySelector('[data-field=\"" + fieldId + "\"]');"
                + "var els=[].slice.call(root.querySelectorAll('*')).filter(function(e){return !(e.closest('svg')&&e.tagName.toLowerCase()!=='svg')});"
                + "els.forEach(function(e,n){var s=getComputedStyle(e);var name='" + fieldId + ":'+n+':'+e.tagName.toLowerCase();"
                + "props.forEach(function(p){out.push([name,p,s.getPropertyValue(p)].join('\\t'))});"
                + "var r=e.getBoundingClientRect();['x','y','width','height'].forEach(function(k){out.push([name,'@'+k,String(Math.round(r[k]*100)/100)].join('\\t'))})});"
                + "return out.join('\\n')})()";
    }

    /** Navigates and measures every case, forcing each case's pseudo-classes on its close button while it is measured. */
    static String measure(DevToolsSession cdp, String url, List<Case> cases) throws Exception {
        cdp.navigate(url);
        String sheets = cdp.evaluate("(function(){var ok=[].every.call(document.styleSheets,function(s){try{return s.cssRules.length>0}catch(e){return false}});return document.styleSheets.length+':'+ok})()");
        if (!sheets.endsWith(":true") || sheets.startsWith("0:")) {
            throw new IllegalStateException("every stylesheet must have loaded - infrastructure failure, not a parity result: " + sheets);
        }
        StringBuilder all = new StringBuilder();
        for (Case c : cases) {
            List<Integer> nodes = c.pseudo().isEmpty() ? List.of() : cdp.nodes("[data-field=\"" + c.id() + "\"] [aria-label=\"Close\"]");
            if (!c.pseudo().isEmpty()) {
                if (nodes.size() != 1) {
                    throw new IllegalStateException("exactly one close button to force the state on: " + c.key());
                }
                cdp.force(nodes, c.pseudo());
            }
            all.append(cdp.evaluate(measureScript(c.id()))).append('\n');
            if (!c.pseudo().isEmpty()) {
                cdp.force(nodes, List.of());
            }
        }
        return all.toString();
    }

    static String slice(String all, String prefix) {
        StringBuilder sb = new StringBuilder();
        for (String line : all.split("\n")) {
            if (line.startsWith(prefix)) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString().stripTrailing();
    }

    // ---------------------------------------------------------------- the scenario page

    /**
     * The page every behaviour suite runs: three page triggers, a tall page (so the scroll lock is real), and four overlays with the controls the scenarios need. The right
     * Drawer d1 comes FIRST in the document, so an overlay opened from inside a Modal over it only paints on top if the stacking order (not the document order) decides.
     * m3 closes on neither Escape nor the backdrop. Returns the migrated markup with its script tags as the templates wrote them.
     */
    static String scenarioBody() {
        StringBuilder b = new StringBuilder();
        b.append("<button id=\"t1\" type=\"button\" data-modal-open=\"m1\">open m1</button>");
        b.append("<button id=\"t2\" type=\"button\" data-drawer-open=\"d1\">open d1</button>");
        b.append("<button id=\"t3\" type=\"button\" data-modal-open=\"m3\">open m3</button>");
        b.append("<button id=\"t4\" type=\"button\" data-drawer-open=\"dl\">open dl</button>");
        b.append("<div style=\"height:3000px\">page</div>");
        b.append(drawer("d1", "Drawer one", null, "<button id=\"d1a\" type=\"button\">a</button><button id=\"d1openm1\" type=\"button\" data-modal-open=\"m1\">open m1</button>"
                + "<button id=\"d1closem1\" type=\"button\" data-modal-close=\"m1\">close m1</button><button id=\"d1openm3\" type=\"button\" data-modal-open=\"m3\">open m3</button>",
                null));
        b.append(modal("m1", "Modal one", o(), "<button id=\"m1a\" type=\"button\">a</button><button id=\"m1b\" type=\"button\">b</button>"
                + "<button id=\"m1openm2\" type=\"button\" data-modal-open=\"m2\">open m2</button><button id=\"m1opend1\" type=\"button\" data-drawer-open=\"d1\">open d1</button>"
                + "<button id=\"m1openm3\" type=\"button\" data-modal-open=\"m3\">open m3</button>", "<button id=\"m1cancel\" type=\"button\" data-modal-close=\"m1\">cancel</button>"));
        b.append(modal("m2", "Modal two", o("size", "Small"), "<button id=\"m2a\" type=\"button\">a</button>", null));
        b.append(modal("m3", "Modal three", o("closeOnEscape", false, "closeOnBackdrop", false), "<button id=\"m3a\" type=\"button\">a</button>"
                + "<button id=\"m3opend1\" type=\"button\" data-drawer-open=\"d1\">open d1</button>", null));
        b.append(drawer("dl", "Drawer left", "Left", "<button id=\"dla\" type=\"button\">a</button>", null));
        return b.toString();
    }

    static final List<String> OVERLAYS = List.of("m1", "m2", "m3", "d1", "dl");

    /** The snapshot, in the page: what an overlay changes, read the same way from either rendering. */
    private static final String SNAPSHOT = """
            window.__overlaysSeen = window.__overlaysSeen || [];
            window.__snap = function () {
              var ids = ['m1', 'm2', 'm3', 'd1', 'dl'];
              var parts = ids.map(function (id) { var r = document.getElementById(id); return id + '=' + (getComputedStyle(r).display === 'none' ? 'hidden' : 'shown'); });
              var a = document.activeElement;
              var active = !a || a === document.body ? 'body' : a.id ? a.id
                  : a.getAttribute('aria-label') === 'Close' ? a.closest('[data-modal],[data-drawer]').id + ':close'
                  : a.matches('[data-modal-dialog],[data-drawer-panel]') ? a.closest('[data-modal],[data-drawer]').id + ':surface' : a.tagName.toLowerCase();
              var top = document.elementFromPoint(innerWidth / 2, innerHeight / 2);
              var painted = top && top.closest('[data-modal],[data-drawer]');
              parts.push('overflow=' + (document.body.style.getPropertyValue('overflow-y') || '-') + (document.body.style.getPropertyPriority('overflow-y') ? '!' : ''));
              parts.push('active=' + active);
              parts.push('top=' + (painted ? painted.id : 'page'));
              return parts.join(';');
            };
            """;

    /** A page: the stylesheets, the theme, the body, extra CSS, and the snapshot function. Nothing overrides the transitions unless {@code extraCss} does. */
    static String page(List<String> sheets, String theme, String body, String extraCss) {
        StringBuilder links = new StringBuilder();
        sheets.forEach(href -> links.append("<link rel=\"stylesheet\" href=\"").append(href).append("\">"));
        return "<!doctype html><html data-theme=\"" + theme + "\"><head><meta charset=\"utf-8\">" + links + "<style>" + extraCss + "</style>"
                + "<script>" + SNAPSHOT + "</script></head><body style=\"margin:8px\">" + body + "</body></html>";
    }

    /** The scenario page with the given script set ("/ashlar/js/" for the real scripts), in the migrated or the legacy rendering. */
    static String scenarioPage(CascadeFixtureServer server, boolean legacyRendering, String scriptBase, String extraCss) throws IOException {
        String body = scenarioBody();
        if (legacyRendering) {
            body = legacy(body);
        }
        body = withScripts(body, scriptBase);
        List<String> sheets = legacyRendering ? List.of(ParityFrames.generated("overlay-oracle.css")) : List.of(ParityFrames.ASHLAR);
        return page(sheets, "light", body, extraCss);
    }

    // ---------------------------------------------------------------- trusted input

    private record Key(String key, String code, int virtualKey, String text) {
    }

    private static Key key(String name) {
        return switch (name) {
            case "Escape" -> new Key("Escape", "Escape", 27, null);
            case "Tab", "Shift+Tab" -> new Key("Tab", "Tab", 9, null);
            case "Enter" -> new Key("Enter", "Enter", 13, "\r");
            case "ArrowDown" -> new Key("ArrowDown", "ArrowDown", 40, null);
            default -> throw new IllegalArgumentException(name);
        };
    }

    static void press(DevToolsSession cdp, String name) throws Exception {
        Key k = key(name);
        int modifiers = name.startsWith("Shift+") ? 8 : 0;
        String common = "\"key\":" + DevToolsSession.quote(k.key()) + ",\"code\":" + DevToolsSession.quote(k.code()) + ",\"windowsVirtualKeyCode\":" + k.virtualKey()
                + ",\"nativeVirtualKeyCode\":" + k.virtualKey() + ",\"modifiers\":" + modifiers;
        cdp.call("Input.dispatchKeyEvent", "{\"type\":\"rawKeyDown\"," + common + "}");
        if (k.text() != null) {
            cdp.call("Input.dispatchKeyEvent", "{\"type\":\"char\",\"text\":" + DevToolsSession.quote(k.text()) + "," + common + "}");
        }
        cdp.call("Input.dispatchKeyEvent", "{\"type\":\"keyUp\"," + common + "}");
    }

    static void clickAt(DevToolsSession cdp, double x, double y) throws Exception {
        String at = "\"x\":" + x + ",\"y\":" + y;
        cdp.call("Input.dispatchMouseEvent", "{\"type\":\"mouseMoved\"," + at + ",\"button\":\"none\"}");
        cdp.call("Input.dispatchMouseEvent", "{\"type\":\"mousePressed\"," + at + ",\"button\":\"left\",\"buttons\":1,\"clickCount\":1}");
        cdp.call("Input.dispatchMouseEvent", "{\"type\":\"mouseReleased\"," + at + ",\"button\":\"left\",\"buttons\":0,\"clickCount\":1}");
    }

    /**
     * A trusted click at the centre of the element the selector finds (it must be visible). The centre is taken once the element has stopped moving (an overlay that is still
     * sliding or scaling in moves its controls), as a person's click would be: no animation or transition may be running on the element or any of its ancestors (including
     * one that has been created but has not moved it yet), and two readings 40ms apart must agree; for at most two seconds.
     */
    static void click(DevToolsSession cdp, String selector) throws Exception {
        String script = "(function(){var e=document.querySelector(" + js(selector) + ");if(!e)return 'none';"
                + "var moving=document.getAnimations().some(function(a){var t=a.effect&&a.effect.target;return t&&a.playState!=='finished'&&a.playState!=='idle'&&t.contains(e)});"
                + "if(moving)return 'moving';var r=e.getBoundingClientRect();"
                + "if(r.width===0||r.height===0)return 'invisible';return (r.left+r.width/2)+','+(r.top+r.height/2)})()";
        String centre = cdp.evaluate(script);
        for (int i = 0; i < 50 && !centre.equals("none") && !centre.equals("invisible"); i++) {
            Thread.sleep(40);
            String again = cdp.evaluate(script);
            if (again.equals(centre) && centre.contains(",")) {
                break;
            }
            centre = again;
        }
        if (!centre.contains(",")) {
            throw new IllegalStateException("cannot click " + selector + ": " + centre);
        }
        String[] xy = centre.split(",");
        clickAt(cdp, Double.parseDouble(xy[0]), Double.parseDouble(xy[1]));
    }

    /** A trusted click on the overlay's backdrop, at a corner the surface does not cover. */
    static void clickBackdrop(DevToolsSession cdp, String overlayId) throws Exception {
        String point = cdp.evaluate("(function(){var r=document.getElementById(" + js(overlayId) + ");var s=r.querySelector('[data-modal-dialog],[data-drawer-panel]').getBoundingClientRect();"
                + "var pts=[[4,4],[innerWidth-4,4],[4,innerHeight-4],[innerWidth-4,innerHeight-4]];for(var i=0;i<pts.length;i++){var p=pts[i];"
                + "if(p[0]<s.left||p[0]>s.right||p[1]<s.top||p[1]>s.bottom)return p[0]+','+p[1]}return 'none'})()");
        String[] xy = point.split(",");
        clickAt(cdp, Double.parseDouble(xy[0]), Double.parseDouble(xy[1]));
    }

    static String js(String s) {
        return DevToolsSession.quote(s);
    }

    static String snapshot(DevToolsSession cdp) throws Exception {
        return cdp.evaluate("window.__snap()");
    }

    /**
     * Runs one action. {@code click:sel}, {@code backdrop:id} and {@code key:Name} are trusted input; {@code focus:id} focuses from the page; {@code clicks:a,b,...} clicks the
     * elements one after another inside ONE task (synthetic clicks, for steps that must land inside a running transition); {@code settle} waits until every exit has had time
     * to finish (the 200ms transitions and either script's fallback timer); {@code wait:N} waits N ms; {@code js:...} runs page script.
     */
    static void perform(DevToolsSession cdp, String action) throws Exception {
        String[] p = action.split(":", 2);
        switch (p[0]) {
            case "click" -> click(cdp, p[1]);
            case "backdrop" -> clickBackdrop(cdp, p[1]);
            case "key" -> press(cdp, p[1]);
            case "focus" -> cdp.evaluate("(function(){document.getElementById(" + js(p[1]) + ").focus();return 'ok'})()");
            case "clicks" -> cdp.evaluate("(function(){" + js(p[1]) + ".split(',').forEach(function(s){document.querySelector(s).click()});return 'ok'})()");
            case "settle" -> Thread.sleep(450);
            case "wait" -> Thread.sleep(Long.parseLong(p[1]));
            case "js" -> cdp.evaluate("(function(){" + p[1] + ";return 'ok'})()");
            default -> throw new IllegalArgumentException(action);
        }
    }

    /** Runs every action and returns "action -> snapshot" after each. */
    static List<String> run(DevToolsSession cdp, List<String> actions) throws Exception {
        List<String> transcript = new ArrayList<>();
        for (String action : actions) {
            perform(cdp, action);
            transcript.add(action + " -> " + snapshot(cdp));
        }
        return transcript;
    }

    /** One value of a snapshot ("m1", "overflow", "active", "top"). */
    static String field(String line, String name) {
        String snap = line.substring(line.indexOf(" -> ") + 4);
        for (String part : snap.split(";")) {
            if (part.startsWith(name + "=")) {
                return part.substring(name.length() + 1);
            }
        }
        throw new IllegalArgumentException("no " + name + " in " + line);
    }

    static void viewport(DevToolsSession cdp, int width, int height) throws Exception {
        cdp.call("Emulation.setDeviceMetricsOverride", "{\"width\":" + width + ",\"height\":" + height + ",\"deviceScaleFactor\":1,\"mobile\":false}");
    }

    static void reducedMotion(DevToolsSession cdp, boolean reduce) throws Exception {
        cdp.call("Emulation.setEmulatedMedia", "{\"features\":[{\"name\":\"prefers-reduced-motion\",\"value\":\"" + (reduce ? "reduce" : "no-preference") + "\"}]}");
    }

    /** Waits until the page's scripts have initialised (DOMContentLoaded has run and the modules have been evaluated). */
    static void ready(DevToolsSession cdp) throws Exception {
        for (int i = 0; i < 100; i++) {
            if (cdp.evaluate("String(document.readyState === 'complete')").equals("true")) {
                Thread.sleep(50);
                return;
            }
            Thread.sleep(50);
        }
        throw new IllegalStateException("the page did not finish loading");
    }
}
