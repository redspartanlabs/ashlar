package dev.redspartanlabs.ashlar.showcase.cascade;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Test-only support for the Tree browser tests: one page with two trees, the real (or the legacy) {@code tree.js} loaded exactly as served, and a scripted sequence of
 * TRUSTED input driven through the Chrome DevTools protocol ({@code Input.dispatchKeyEvent} and {@code Input.dispatchMouseEvent}). Those events are what the browser
 * itself generates for a user: they have {@code isTrusted === true}, they move real focus (so {@code Tab} walks the real tab order and {@code :focus-visible} follows the real
 * heuristics) and they run the page's real default actions. A synthetic {@code KeyboardEvent} dispatched from a script does none of that, so it is never used here.
 *
 * <p>After every step the page reports a snapshot of everything the user can observe: the focused element, which items are the Tab stop, every {@code aria-expanded}, which
 * panels are {@code inert}, whether the focused element matches {@code :focus-visible}, the URL hash, and whether what is drawn agrees with the attributes (every chevron turned
 * exactly when its own item is expanded, every panel open exactly when its item is expanded). The same sequence runs against the legacy markup with the original script and
 * against the migrated markup with the migrated script, so {@link TreeLegacyBehaviourTest} can require the two to be identical.
 */
final class TreeBrowser {

    private TreeBrowser() {
    }

    /** One step: the input, the element that must have focus afterwards, and fragments of the snapshot that must be present. */
    record Step(String action, String active, String... fragments) {
    }

    private record Key(String key, String code, int virtualKey, String text) {
    }

    private static Key key(String name) {
        return switch (name) {
            case "ArrowDown" -> new Key("ArrowDown", "ArrowDown", 40, null);
            case "ArrowUp" -> new Key("ArrowUp", "ArrowUp", 38, null);
            case "ArrowLeft" -> new Key("ArrowLeft", "ArrowLeft", 37, null);
            case "ArrowRight" -> new Key("ArrowRight", "ArrowRight", 39, null);
            case "Home" -> new Key("Home", "Home", 36, null);
            case "End" -> new Key("End", "End", 35, null);
            case "Enter" -> new Key("Enter", "Enter", 13, "\r");
            case "Space" -> new Key(" ", "Space", 32, " ");
            case "Tab", "Shift+Tab" -> new Key("Tab", "Tab", 9, null);
            default -> throw new IllegalArgumentException(name);
        };
    }

    // ---- markup ----

    private static final String SVG = "<svg class=\"ash-icon ash-icon--sm\" viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M9 5l7 7-7 7\"/></svg>";

    private record Node(String id, String label, boolean icon, String href, Boolean expanded, List<Node> children) {
    }

    private static Node leaf(String id, String label, boolean icon, String href) {
        return new Node(id, label, icon, href, null, List.of());
    }

    private static Node parent(String id, String label, boolean icon, String href, boolean expanded, Node... children) {
        return new Node(id, label, icon, href, expanded, List.of(children));
    }

    private static final List<Node> TREE_ONE = List.of(
            parent("a", "Alpha", true, null, true,
                    parent("b", "Beta", false, null, true,
                            leaf("c", "Gamma", true, "#c"),
                            parent("d", "Delta", false, null, false, leaf("e", "Epsilon", false, null))),
                    leaf("f", "Zeta", false, "#f")),
            parent("g", "Eta", false, null, false, parent("h", "Theta", false, null, true, leaf("i", "Iota", false, null))),
            leaf("k", "Kappa", true, null),
            parent("m", "Mu", false, "#m", false, leaf("n", "Nu", false, null)));

    private static final List<Node> TREE_TWO = List.of(parent("x", "Xi", false, null, false, leaf("y", "Upsilon", false, null)));

    private static String item(Node node, int level) {
        boolean parent = !node.children().isEmpty();
        StringBuilder sb = new StringBuilder("<li id=\"" + node.id() + "\" role=\"treeitem\"" + (parent ? " aria-expanded=\"" + node.expanded() + "\"" : "") + " aria-level=\"" + level
                + "\" tabindex=\"-1\" data-tree-item class=\"ash-tree__item\"><div class=\"ash-tree__row\" id=\"" + node.id() + "-row\">");
        if (parent) {
            sb.append("<button type=\"button\" tabindex=\"-1\" aria-hidden=\"true\" data-tree-toggle id=\"" + node.id() + "-toggle\" class=\"ash-tree__toggle\"><span data-tree-chevron id=\"" + node.id()
                    + "-chevron\" class=\"ash-tree__chevron\">" + SVG + "</span></button>");
        } else {
            sb.append("<span id=\"" + node.id() + "-spacer\" class=\"ash-tree__spacer\" aria-hidden=\"true\"></span>");
        }
        if (node.icon()) {
            sb.append("<span class=\"ash-tree__icon\" aria-hidden=\"true\">" + SVG + "</span>");
        }
        sb.append(node.href() != null ? "<a id=\"" + node.id() + "-label\" href=\"" + node.href() + "\" tabindex=\"-1\" class=\"ash-tree__label\">" + node.label() + "</a>"
                : "<span id=\"" + node.id() + "-label\" class=\"ash-tree__label\">" + node.label() + "</span>");
        sb.append("</div>");
        if (parent) {
            sb.append("<div id=\"" + node.id() + "-panel\" data-tree-children" + (node.expanded() ? "" : " inert") + " class=\"ash-tree__panel\"><div class=\"ash-tree__clip\"><ul role=\"group\" class=\"ash-tree__group\">");
            node.children().forEach(c -> sb.append(item(c, level + 1)));
            sb.append("</ul></div></div>");
        }
        return sb.append("</li>").toString();
    }

    private static String tree(String id, List<Node> roots) {
        StringBuilder sb = new StringBuilder("<ul id=\"" + id + "\" role=\"tree\" data-tree class=\"ash-tree\">");
        roots.forEach(n -> sb.append(item(n, 1)));
        return sb.append("</ul>").toString();
    }

    /** The two trees between two ordinary buttons, as the migrated templates render them, or rewritten to the legacy Tailwind markup. */
    static String body(boolean legacyMarkup, TreeLegacyMarkup tree, IconLegacyMarkup icons) {
        String html = "<button type=\"button\" id=\"before\">Before</button>" + tree("t1", TREE_ONE) + "<button type=\"button\" id=\"after\">After</button>" + tree("t2", TREE_TWO);
        return legacyMarkup ? icons.rewrite(tree.rewrite(html)) : html;
    }

    private static final String SNAPSHOT = """
            window.__trusted = []; window.__untrusted = [];
            document.addEventListener('keydown', function (e) { (e.isTrusted ? window.__trusted : window.__untrusted).push(e.key); }, true);
            document.addEventListener('click', function (e) { (e.isTrusted ? window.__trusted : window.__untrusted).push('click'); }, true);
            window.__snap = function () {
              var items = [].slice.call(document.querySelectorAll('[data-tree-item]'));
              var ae = document.activeElement;
              var active = ae && ae.id ? ae.id : (ae ? ae.tagName.toLowerCase() : 'none');
              var tab = items.filter(function (i) { return i.getAttribute('tabindex') === '0'; }).map(function (i) { return i.id; }).join(',');
              var exp = items.filter(function (i) { return i.hasAttribute('aria-expanded'); }).map(function (i) { return i.id + ':' + i.getAttribute('aria-expanded'); }).join(',');
              var panels = [].slice.call(document.querySelectorAll('[data-tree-children]'));
              var inert = panels.filter(function (p) { return p.inert; }).map(function (p) { return p.closest('[data-tree-item]').id; }).join(',');
              var sync = panels.every(function (p) { var li = p.closest('[data-tree-item]'); return (li.getAttribute('aria-expanded') === 'true') === !p.inert && p.inert === p.hasAttribute('inert'); });
              var visual = panels.every(function (p) {
                var li = p.closest('[data-tree-item]'); var open = li.getAttribute('aria-expanded') === 'true';
                var chevron = li.querySelector(':scope > div [data-tree-chevron]');
                return (p.getBoundingClientRect().height > 0) === open && (getComputedStyle(chevron).rotate === (open ? '90deg' : 'none')); });
              var fv = ae && ae.matches ? String(ae.matches(':focus-visible')) : 'false';
              var cls = [].map.call(document.querySelectorAll('#t1 *, #t2 *'), function (e) { return (e.getAttribute('class') || '') + '|' + (e.getAttribute('style') || ''); }).join('~');
              var sum = 0; for (var k = 0; k < cls.length; k++) { sum = (sum * 31 + cls.charCodeAt(k)) % 1000003; }
              return 'active=' + active + ';tab=' + tab + ';exp=' + exp + ';inert=' + inert + ';sync=' + sync + ';visual=' + visual + ';fv=' + fv + ';hash=' + location.hash + ';cls=' + sum;
            };
            """;

    /** The whole page: the stylesheets, the markup, the script text (not run) and the snapshot function. */
    static String page(List<String> sheets, String body, String script) {
        StringBuilder links = new StringBuilder();
        sheets.forEach(href -> links.append("<link rel=\"stylesheet\" href=\"").append(href).append("\">"));
        return "<!doctype html><html data-theme=\"light\"><head><meta charset=\"utf-8\">" + links
                + "<style>*,*::before,*::after{transition:none !important;animation-play-state:paused !important}</style></head><body style=\"margin:8px;width:400px\">" + body
                + "<script id=\"src\" type=\"text/plain\">" + script.replace("</script", "<\\/script") + "</script><script>" + SNAPSHOT.replace("</script", "<\\/script") + "</script></body></html>";
    }

    /** Loads the script exactly as served (a module, from a Blob) and fires the DOMContentLoaded the real page had already seen before the Blob was imported. */
    static void initialise(DevToolsSession cdp) throws Exception {
        String response = cdp.call("Runtime.evaluate", "{\"expression\":" + DevToolsSession.quote(
                "(async function(){var u=URL.createObjectURL(new Blob([document.getElementById('src').textContent],{type:'text/javascript'}));await import(u);"
                        + "document.dispatchEvent(new Event('DOMContentLoaded'));return 'ok'})()") + ",\"awaitPromise\":true,\"returnByValue\":true}");
        if (!response.contains("\"value\":\"ok\"")) {
            throw new IllegalStateException("the script did not load: " + response);
        }
    }

    // ---- input ----

    private static void dispatchKey(DevToolsSession cdp, Key k, int modifiers) throws Exception {
        String common = "\"key\":" + DevToolsSession.quote(k.key()) + ",\"code\":" + DevToolsSession.quote(k.code()) + ",\"windowsVirtualKeyCode\":" + k.virtualKey() + ",\"nativeVirtualKeyCode\":" + k.virtualKey()
                + ",\"modifiers\":" + modifiers;
        cdp.call("Input.dispatchKeyEvent", "{\"type\":\"rawKeyDown\"," + common + "}");
        if (k.text() != null) {
            cdp.call("Input.dispatchKeyEvent", "{\"type\":\"char\",\"text\":" + DevToolsSession.quote(k.text()) + "," + common + "}");
        }
        cdp.call("Input.dispatchKeyEvent", "{\"type\":\"keyUp\"," + common + "}");
    }

    private static void click(DevToolsSession cdp, String id) throws Exception {
        String centre = cdp.evaluate("(function(){var r=document.getElementById('" + id + "').getBoundingClientRect();return (r.left+r.width/2)+','+(r.top+r.height/2)})()");
        String[] xy = centre.split(",");
        String at = "\"x\":" + xy[0] + ",\"y\":" + xy[1];
        cdp.call("Input.dispatchMouseEvent", "{\"type\":\"mouseMoved\"," + at + ",\"button\":\"none\"}");
        cdp.call("Input.dispatchMouseEvent", "{\"type\":\"mousePressed\"," + at + ",\"button\":\"left\",\"buttons\":1,\"clickCount\":1}");
        cdp.call("Input.dispatchMouseEvent", "{\"type\":\"mouseReleased\"," + at + ",\"button\":\"left\",\"buttons\":0,\"clickCount\":1}");
    }

    /** Performs one step. {@code key:Name} and {@code click:id} are trusted browser input; {@code focus:id} and {@code focusjs:id} call element.focus() from the page. */
    static void perform(DevToolsSession cdp, String action) throws Exception {
        String[] parts = action.split(":", 2);
        switch (parts[0]) {
            case "key" -> dispatchKey(cdp, key(parts[1]), parts[1].startsWith("Shift+") ? 8 : 0);
            case "click" -> click(cdp, parts[1]);
            case "focus", "focusjs" -> cdp.evaluate("(function(){document.getElementById('" + parts[1] + "').focus();return 'ok'})()");
            case "none" -> { }
            default -> throw new IllegalArgumentException(action);
        }
    }

    static String snapshot(DevToolsSession cdp) throws Exception {
        return cdp.evaluate("window.__snap()");
    }

    // ---- the sequence ----

    /** The sequence both renderings run. The expectations were written from the script's documented keyboard model, not from either rendering's output. */
    static List<Step> steps() {
        List<Step> s = new ArrayList<>();
        s.add(new Step("none", "body", "tab=a,x", "exp=a:true,b:true,d:false,g:false,h:true,m:false,x:false", "inert=d,g,m,x", "sync=true", "visual=true"));
        s.add(new Step("focus:before", "before"));
        s.add(new Step("key:Tab", "a", "tab=a,x", "fv=true"));
        for (String id : List.of("b", "c", "d", "f", "g", "k", "m", "m")) {
            s.add(new Step("key:ArrowDown", id));
        }
        s.add(new Step("key:Home", "a"));
        s.add(new Step("key:ArrowUp", "a"));
        s.add(new Step("key:End", "m"));
        s.add(new Step("key:Home", "a"));
        s.add(new Step("key:ArrowRight", "b", "a:true"));
        s.add(new Step("key:ArrowRight", "c"));
        s.add(new Step("key:ArrowRight", "c", "tab=c,x"));
        s.add(new Step("key:ArrowLeft", "b"));
        s.add(new Step("key:ArrowLeft", "b", "b:false", "inert=b,d,g,m,x"));
        s.add(new Step("key:ArrowLeft", "a"));
        s.add(new Step("key:ArrowRight", "b", "b:false"));
        s.add(new Step("key:ArrowRight", "b", "b:true"));
        s.add(new Step("key:ArrowDown", "c"));
        s.add(new Step("key:ArrowDown", "d"));
        s.add(new Step("key:ArrowRight", "d", "d:true"));
        s.add(new Step("key:ArrowDown", "e"));
        s.add(new Step("key:ArrowLeft", "d"));
        s.add(new Step("key:ArrowLeft", "d", "d:false", "inert=d,g,m,x"));
        s.add(new Step("focusjs:e", "d", "d:false"));
        s.add(new Step("key:End", "m"));
        s.add(new Step("key:ArrowUp", "k"));
        s.add(new Step("key:ArrowUp", "g"));
        s.add(new Step("key:Enter", "g", "g:true"));
        s.add(new Step("key:ArrowDown", "h"));
        s.add(new Step("key:ArrowDown", "i"));
        s.add(new Step("key:ArrowUp", "h"));
        s.add(new Step("key:ArrowLeft", "h", "h:false", "g:true"));
        s.add(new Step("key:ArrowLeft", "g"));
        s.add(new Step("key:Space", "g", "g:false", "h:false"));
        s.add(new Step("key:Space", "g", "g:true", "h:false"));
        s.add(new Step("key:ArrowRight", "h"));
        s.add(new Step("key:ArrowRight", "h", "h:true"));
        s.add(new Step("key:Home", "a"));
        s.add(new Step("key:ArrowLeft", "a", "a:false", "b:true", "d:false"));
        s.add(new Step("key:ArrowDown", "g"));
        s.add(new Step("key:ArrowUp", "a"));
        s.add(new Step("key:ArrowRight", "a", "a:true", "b:true"));
        s.add(new Step("key:ArrowDown", "b"));
        s.add(new Step("key:ArrowDown", "c"));
        s.add(new Step("key:Enter", "c", "hash=#c"));
        s.add(new Step("key:End", "m"));
        s.add(new Step("key:ArrowUp", "k", "hash=#c"));
        s.add(new Step("key:Space", "k", "hash=#c"));
        s.add(new Step("key:ArrowDown", "m"));
        s.add(new Step("key:Enter", "m", "m:true", "hash=#c"));
        s.add(new Step("key:Enter", "m", "m:false", "hash=#c"));
        s.add(new Step("click:g-toggle", "g", "g:false"));
        s.add(new Step("click:g-toggle", "g", "g:true"));
        s.add(new Step("click:a-label", "a", "a:true", "g:true"));
        s.add(new Step("click:f-label", "f", "hash=#f", "tab=f,x"));
        s.add(new Step("key:Tab", "after"));
        s.add(new Step("key:Shift+Tab", "f"));
        s.add(new Step("key:Tab", "after"));
        s.add(new Step("key:Tab", "x", "tab=f,x"));
        s.add(new Step("key:ArrowRight", "x", "x:true", "a:true"));
        s.add(new Step("key:ArrowDown", "y"));
        s.add(new Step("key:ArrowLeft", "x"));
        s.add(new Step("click:x-toggle", "x", "x:false"));
        s.add(new Step("focusjs:y", "x", "x:false"));
        return s;
    }

    /** Runs the whole sequence and returns one snapshot per step. */
    static List<String> run(DevToolsSession cdp, List<Step> steps) throws Exception {
        List<String> transcript = new ArrayList<>();
        for (Step step : steps) {
            perform(cdp, step.action());
            transcript.add(snapshot(cdp));
        }
        return transcript;
    }

    static String script(Class<?> owner, String resource) throws java.io.IOException {
        try (java.io.InputStream in = owner.getResourceAsStream(resource)) {
            if (in == null) {
                throw new java.io.IOException(resource + " is not on the test classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
