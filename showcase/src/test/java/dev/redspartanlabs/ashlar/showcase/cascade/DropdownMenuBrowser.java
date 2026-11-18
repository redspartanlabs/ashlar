package dev.redspartanlabs.ashlar.showcase.cascade;

import dev.redspartanlabs.ashlar.icon.Icon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What the Dropdown Menu suites share: the REAL templates rendered in this process ({@code ashlar/components/dropdown-menu.jte} and {@code dropdown-menu-item.jte}, through
 * {@link OverlayBrowser#renderComponent}), the legacy rendering of that markup ({@link DropdownMenuLegacyMarkup}, then {@link IconLegacyMarkup}), the parity cases, the
 * behavior page and its snapshot, and trusted browser input. The exit trace and the waits on what the page shows are {@link FloatingBrowser}'s, on the menu.
 *
 * <p>The snapshot reads the state the same way from either rendering, never from a class or an attribute only one of them has: a menu is "open" while its trigger says
 * aria-expanded="true" and it is rendered (its computed display is not none), "closing" while it is still rendered after its trigger says false, and "closed" once it is not.
 */
final class DropdownMenuBrowser {

    private DropdownMenuBrowser() {
    }

    // ---------------------------------------------------------------- the real templates

    /** A Dropdown Menu with these items, each "Label|href" (an empty href renders the disabled text). */
    static String menu(String label, Icon icon, String... items) {
        gg.jte.Content content = out -> {
            gg.jte.output.StringOutput s = new gg.jte.output.StringOutput();
            for (String item : items) {
                String[] p = item.split("\\|", -1);
                Map<String, Object> params = p[1].isEmpty() ? OverlayBrowser.o("label", p[0]) : OverlayBrowser.o("label", p[0], "href", p[1]);
                OverlayBrowser.renderInto("dropdown-menu-item.jte", params, s);
            }
            out.writeContent(s.toString());
        };
        Map<String, Object> params = OverlayBrowser.o("label", label, "items", content);
        if (icon != null) {
            params.put("icon", icon);
        }
        return OverlayBrowser.renderComponent("dropdown-menu.jte", params);
    }

    static final String[] ACTIONS = {"Edit|#e", "Duplicate|#d", "Archive|", "Delete|#x"};

    /** The legacy rendering of migrated markup: the Dropdown Menu's parts, then the icons. */
    static String legacy(String migrated) throws IOException {
        return new IconLegacyMarkup().rewrite(new DropdownMenuLegacyMarkup().rewrite(migrated));
    }

    private static final Pattern LIST_TAG = Pattern.compile("<div\\b[^>]*data-dropdown-menu-list[^>]*>");
    private static final Pattern CHEVRON_TAG = Pattern.compile("<span\\b[^>]*data-dropdown-menu-chevron[^>]*>");

    /** The migrated markup in another state: the menu shown (no hidden attribute) and open (data-state="open" on the menu and the chevron, aria-expanded="true"). */
    static String inState(String html, boolean shown, boolean open) {
        Matcher list = LIST_TAG.matcher(html);
        if (!list.find()) {
            throw new IllegalStateException("no menu in " + html);
        }
        String tag = list.group();
        String stated = (shown ? tag.replaceFirst("\\s+hidden(?=[\\s>])", "") : tag).replace("data-state=\"closed\"", "data-state=\"" + (open ? "open" : "closed") + "\"");
        String out = html.substring(0, list.start()) + stated + html.substring(list.end());
        if (open) {
            Matcher chevron = CHEVRON_TAG.matcher(out);
            if (!chevron.find()) {
                throw new IllegalStateException("no chevron in " + html);
            }
            out = out.substring(0, chevron.start()) + chevron.group().replace("data-state=\"closed\"", "data-state=\"open\"") + out.substring(chevron.end());
            out = out.replaceFirst("aria-expanded=\"false\"", "aria-expanded=\"true\"");
        }
        return out;
    }

    private static final Pattern MARKABLE = Pattern.compile("<(div|button|span|a|svg)(?=[\\s>])");

    /** Marks every element of the component so the scoped legacy stylesheet can address exactly those. */
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

    // ---------------------------------------------------------------- the parity cases

    /** One measured case: its markup, and the pseudo-classes forced on the one element {@code force} finds in it (null: nothing forced). */
    record Case(String id, String key, String markup, String force, List<String> pseudo) {
    }

    private static final String TRIGGER = "[data-dropdown-menu-trigger]";
    private static final String FIRST_ITEM = "[data-dropdown-menu-item]:first-child";
    private static final String DISABLED = "[data-dropdown-menu-item][aria-disabled]";

    static final String LONG_LABEL = "An action whose label is long enough to reach the menu's maximum width and wrap onto a second line";

    /** Gives a case's generated ids (dropdown-menu-N and its trigger's) a prefix of its own, so two renderings of the same markup share them. */
    private static String ids(String html, String prefix) {
        return html.replaceAll("dropdown-menu-\\d+", prefix + "-menu");
    }

    /**
     * Every case the static suites measure: the menu with a disabled entry, with an icon on its trigger, and with an entry long enough to wrap at the menu's maximum width,
     * each closed (not rendered), shown in its closed visual state, and open; the trigger under a hovering pointer and with keyboard focus (and both); an entry under the
     * pointer, focused, and both; the disabled entry under the pointer and focused; and an entry that carries aria-current (a skin hook).
     */
    static List<Case> cases() {
        Map<String, String> menus = new LinkedHashMap<>();
        menus.put("actions", menu("Actions", null, ACTIONS));
        menus.put("icon trigger", menu("Settings", Icon.SETTINGS, "General|#g", "Notifications|#n"));
        menus.put("long entry", menu("More", null, "Short|#s", LONG_LABEL + "|#l"));
        List<Case> cases = new ArrayList<>();
        int n = 0;
        for (Map.Entry<String, String> m : menus.entrySet()) {
            for (String s : List.of("closed", "shown", "open")) {
                String html = OverlayBrowser.withoutScripts(s.equals("closed") ? m.getValue() : inState(m.getValue(), true, s.equals("open")));
                String id = "dd" + (++n);
                cases.add(new Case(id, m.getKey() + " " + s, ids(marked(html), id), null, List.of()));
            }
        }
        String closed = OverlayBrowser.withoutScripts(menus.get("actions"));
        String open = OverlayBrowser.withoutScripts(inState(menus.get("actions"), true, true));
        Object[][] forced = {
                {"closed, trigger hover", closed, TRIGGER, List.of("hover")},
                {"closed, trigger focus-visible", closed, TRIGGER, List.of("focus", "focus-visible")},
                {"closed, trigger focus-visible and hover", closed, TRIGGER, List.of("focus", "focus-visible", "hover")},
                {"open, trigger hover", open, TRIGGER, List.of("hover")},
                {"open, entry hover", open, FIRST_ITEM, List.of("hover")},
                {"open, entry focus", open, FIRST_ITEM, List.of("focus", "focus-visible")},
                {"open, entry focus and hover", open, FIRST_ITEM, List.of("focus", "focus-visible", "hover")},
                {"open, disabled entry hover and focus", open, DISABLED, List.of("hover", "focus", "focus-visible")},
        };
        for (Object[] f : forced) {
            String id = "dd" + (++n);
            @SuppressWarnings("unchecked")
            List<String> pseudo = (List<String>) f[3];
            cases.add(new Case(id, "actions " + f[0], ids(marked((String) f[1]), id), (String) f[2], pseudo));
        }
        String id = "dd" + (++n);
        cases.add(new Case(id, "actions open, an entry with aria-current", ids(marked(open.replaceFirst("data-dropdown-menu-item", "data-dropdown-menu-item aria-current=\"page\"")), id),
                null, List.of()));
        return cases;
    }

    /** The migrated and the legacy body of every case, each in its field (room on every side, so no menu reaches outside the page). */
    static String[] bodies(List<Case> cases) throws IOException {
        StringBuilder migrated = new StringBuilder();
        StringBuilder legacy = new StringBuilder();
        for (Case c : cases) {
            String field = "<div data-field=\"" + c.id() + "\" data-parity=\"raw\" style=\"padding:16px 40px 320px 40px\">";
            migrated.append(field).append(c.markup()).append("</div>");
            legacy.append(field).append(legacy(c.markup())).append("</div>");
        }
        return new String[] {migrated.toString(), legacy.toString()};
    }

    /** One line per element and property of the field ({@link FloatingBrowser#measureScript}, whose list includes scale, rotate and z-index), plus transform-origin. */
    static String measureScript(String fieldId, boolean skipRaw) {
        String script = FloatingBrowser.measureScript(fieldId, skipRaw);
        String withOrigin = script.replace("var props=[", "var props=['transform-origin',");
        if (withOrigin.equals(script)) {
            throw new IllegalStateException("could not add transform-origin to the measured properties");
        }
        return withOrigin;
    }

    /** Navigates and measures every case, forcing each case's pseudo-classes on its one element while it is measured. */
    static String measure(DevToolsSession cdp, String url, List<Case> cases, boolean skipRaw) throws Exception {
        cdp.navigate(url);
        String sheets = cdp.evaluate("(function(){var ok=[].every.call(document.styleSheets,function(s){try{return s.cssRules.length>0}catch(e){return false}});return document.styleSheets.length+':'+ok})()");
        if (!sheets.endsWith(":true") || sheets.startsWith("0:")) {
            throw new IllegalStateException("every stylesheet must have loaded - infrastructure failure, not a parity result: " + sheets);
        }
        StringBuilder all = new StringBuilder();
        for (Case c : cases) {
            List<Integer> nodes = c.force() == null ? List.of() : cdp.nodes("[data-field=\"" + c.id() + "\"] " + c.force());
            if (c.force() != null) {
                if (nodes.size() != 1) {
                    throw new IllegalStateException("exactly one element to force the state on: " + c.key() + ", found " + nodes.size());
                }
                cdp.force(nodes, c.pseudo());
            }
            all.append(cdp.evaluate(measureScript(c.id(), skipRaw))).append('\n');
            if (c.force() != null) {
                cdp.force(nodes, List.of());
            }
        }
        return all.toString();
    }

    /**
     * A static page in one configuration: the stylesheets, then the configuration's skin, transitions off, and the body inside the same wrapper TextEntryBrowser uses. The
     * optional {@code header} goes in a {@code <header>} directly in the body, where civic's masthead rule reaches a trigger.
     */
    static String page(List<String> sheets, ParityConfigs.Config config, String header, String body) {
        StringBuilder links = new StringBuilder();
        sheets.forEach(href -> links.append("<link rel=\"stylesheet\" href=\"").append(href).append("\">"));
        if (config.skin() != null) {
            links.append("<link rel=\"stylesheet\" href=\"/css/theme-").append(config.skin()).append(".css\">");
        }
        String skin = config.skin() == null ? "" : " data-skin=\"" + config.skin() + "\"";
        return "<!doctype html><html data-theme=\"" + config.theme() + "\"" + skin + "><head><meta charset=\"utf-8\">" + links
                + "<style>*,*::before,*::after{transition:none !important;animation-play-state:paused !important}</style></head><body style=\"margin:8px;width:600px\">"
                + (header.isEmpty() ? "" : "<header>" + header + "</header>")
                + "<div style=\"font-family:Arial,Helvetica,sans-serif;color:rgb(15, 23, 42)\">" + body + "</div></body></html>";
    }

    // ---------------------------------------------------------------- the behavior page

    static final List<String> MENUS = List.of("m1", "m2", "pm", "md", "rd");

    private static String at(int left, int top, String html) {
        return "<div style=\"position:absolute;left:" + left + "px;top:" + top + "px\">" + html + "</div>";
    }

    private static String dd(String id, String html) {
        return "<div data-dd=\"" + id + "\">" + html + "</div>";
    }

    /**
     * The page every behavior suite runs, at 1000 x 800. Menus, by data-dd: m1 "Actions" (Edit #e, Duplicate #d, Archive disabled, Delete #x) between the buttons #before
     * and #after; m2 "Second" with an icon (One #1, Two #2); pm inside the Popover whose trigger is #pop; md the last control of the Modal mo (after #mfirst), opened by
     * #openmo; rd the last control of the Drawer dr (after #dfirst), opened by #opendr. The Modal ng holds no Dropdown Menu: its last tabbable control #nlast is followed by
     * a visible link out of the Tab order (#nneg), opened by #openng. #text is plain text far from every menu; #pageafter is the page's last control, after every overlay.
     * Returns the migrated markup with its script tags as the templates wrote them.
     */
    static String scenarioBody() {
        StringBuilder b = new StringBuilder();
        b.append(at(20, 40, "<button id=\"before\" type=\"button\">Before</button>"));
        b.append(at(110, 40, dd("m1", menu("Actions", null, ACTIONS))));
        b.append(at(300, 40, "<button id=\"after\" type=\"button\">After</button>"));
        b.append(at(400, 40, dd("m2", menu("Second", Icon.SETTINGS, "One|#1", "Two|#2"))));
        b.append(at(620, 40, OverlayBrowser.renderComponent("popover.jte", OverlayBrowser.o("trigger", OverlayBrowser.raw("<button id=\"pop\" type=\"button\">Pop</button>"),
                "content", OverlayBrowser.raw("<p style=\"margin:0 0 8px\">Inside the popover</p>" + dd("pm", menu("Inside", null, "Alpha|#a", "Beta|#b")))))));
        b.append(at(20, 560, "<p id=\"text\" style=\"margin:0;padding:20px\">Plain text, far from every menu</p>"));
        b.append(at(20, 700, "<button id=\"openmo\" type=\"button\" data-modal-open=\"mo\">Modal</button>"));
        b.append(at(140, 700, "<button id=\"opendr\" type=\"button\" data-drawer-open=\"dr\">Drawer</button>"));
        b.append(at(260, 700, "<button id=\"openng\" type=\"button\" data-modal-open=\"ng\">No menu</button>"));
        b.append(OverlayBrowser.modal("mo", "A menu in a modal", OverlayBrowser.o("size", "Large"),
                "<button id=\"mfirst\" type=\"button\">First</button>" + dd("md", menu("In the modal", null, "Alpha|#ma", "Beta|#mb")), null));
        b.append(OverlayBrowser.drawer("dr", "A menu in a drawer", null,
                "<button id=\"dfirst\" type=\"button\">First</button>" + dd("rd", menu("In the drawer", null, "Alpha|#ra", "Beta|#rb")), null));
        b.append(OverlayBrowser.modal("ng", "No menu", OverlayBrowser.o("size", "Large"),
                "<button id=\"nfirst\" type=\"button\">First</button><button id=\"nlast\" type=\"button\">Last tabbable</button>"
                        + "<a id=\"nneg\" href=\"#n\" tabindex=\"-1\">A visible link out of the Tab order</a>", null));
        b.append(at(420, 700, "<button id=\"pageafter\" type=\"button\">Page after</button>"));
        return b.toString();
    }

    /** The snapshot, in the page: what a Dropdown Menu changes, read the same way from either rendering (see the class comment). */
    private static final String SNAPSHOT = """
            window.__dsnap = function () {
              function rendered(e) { return getComputedStyle(e).display !== 'none'; }
              function describe(e) {
                if (!e || e === document.body) { return 'body'; }
                var dd = e.closest('[data-dd]');
                if (e.matches('[data-dropdown-menu-trigger]')) { return 'trigger:' + dd.dataset.dd; }
                if (e.matches('[data-dropdown-menu-item]')) { return 'item:' + dd.dataset.dd + ':' + e.textContent.trim(); }
                if (e.matches('[data-modal-dialog],[data-drawer-panel]')) { return 'panel:' + e.closest('[data-modal],[data-drawer]').id; }
                if (e.matches('[data-modal-close],[data-drawer-close]')) { return 'close:' + e.closest('[data-modal],[data-drawer]').id; }
                return e.id || e.tagName.toLowerCase();
              }
              var parts = [];
              %s.forEach(function (id) {
                var root = document.querySelector('[data-dd="' + id + '"] [data-dropdown-menu]');
                var expanded = root.querySelector('[data-dropdown-menu-trigger]').getAttribute('aria-expanded') === 'true';
                var shown = rendered(root.querySelector('[data-dropdown-menu-list]'));
                parts.push('dd:' + id + '=' + (expanded ? (shown ? 'open' : 'expanded-but-hidden') : (shown ? 'closing' : 'closed')));
              });
              ['mo', 'dr', 'ng'].forEach(function (id) {
                var r = document.getElementById(id);
                parts.push('ov:' + id + '=' + (rendered(r) ? (r.dataset.state === 'closed' ? 'closing' : 'shown') : 'hidden'));
              });
              var pop = document.getElementById('pop');
              var surface = pop.closest('[data-popover]').querySelector(':scope > [data-popover-surface]');
              parts.push('pop=' + (pop.getAttribute('aria-expanded') === 'true' ? (rendered(surface) ? 'open' : 'expanded-but-hidden') : (rendered(surface) ? 'closing' : 'closed')));
              parts.push('active=' + describe(document.activeElement));
              parts.push('hash=' + (location.hash || '-'));
              var tr = window.__ftrace;
              if (tr) {
                parts.push('exit=' + (tr.timers.filter(function (r) { return r.ms === 200; }).map(function (r) { return r.state; }).join(',') || 'none'));
              }
              return parts.join(';');
            };
            """.formatted("['m1','m2','pm','md','rd']");

    /** The stylesheets a rendering is shown with: the legacy menu on the Dropdown Menu oracle, over Ashlar's own (the other components on the page are already Ashlar's). */
    static List<String> sheets(boolean legacyRendering) {
        return legacyRendering ? List.of(ParityFrames.ASHLAR, ParityFrames.generated("dropdown-menu-oracle.css")) : List.of(ParityFrames.ASHLAR);
    }

    /** The behavior page with the given script set ("/ashlar/js/" for the real scripts), in the migrated or the legacy rendering. */
    static String scenarioPage(boolean legacyRendering, String scriptBase) throws IOException {
        String body = scenarioBody();
        if (legacyRendering) {
            body = legacy(body);
        }
        StringBuilder links = new StringBuilder();
        sheets(legacyRendering).forEach(href -> links.append("<link rel=\"stylesheet\" href=\"").append(href).append("\">"));
        return "<!doctype html><html data-theme=\"light\"><head><meta charset=\"utf-8\">" + links + "<script>" + SNAPSHOT + "</script></head>"
                + "<body style=\"margin:0;font-family:Arial,Helvetica,sans-serif;font-size:16px\">" + OverlayBrowser.withScripts(body, scriptBase) + "</body></html>";
    }

    static String snapshot(DevToolsSession cdp) throws Exception {
        return cdp.evaluate("window.__dsnap()");
    }

    // ---------------------------------------------------------------- trusted input and actions

    static String js(String s) {
        return DevToolsSession.quote(s);
    }

    private static String rootOf(String dd) {
        return "document.querySelector('[data-dd=\"" + dd + "\"] [data-dropdown-menu]')";
    }

    static String trigger(String dd) {
        return "[data-dd=\"" + dd + "\"] [data-dropdown-menu-trigger]";
    }

    static String item(String dd, int n) {
        return "[data-dd=\"" + dd + "\"] [data-dropdown-menu-item]:nth-child(" + n + ")";
    }

    /** A trusted key: Escape, Tab, Shift+Tab, Enter, Space, ArrowDown, ArrowUp, Home or End. */
    static void key(DevToolsSession cdp, String name) throws Exception {
        String k;
        String code;
        int vk;
        String text = null;
        int modifiers = 0;
        switch (name) {
            case "Escape" -> { k = "Escape"; code = "Escape"; vk = 27; }
            case "Tab" -> { k = "Tab"; code = "Tab"; vk = 9; }
            case "Shift+Tab" -> { k = "Tab"; code = "Tab"; vk = 9; modifiers = 8; }
            case "Enter" -> { k = "Enter"; code = "Enter"; vk = 13; text = "\r"; }
            case "Space" -> { k = " "; code = "Space"; vk = 32; text = " "; }
            case "ArrowDown" -> { k = "ArrowDown"; code = "ArrowDown"; vk = 40; }
            case "ArrowUp" -> { k = "ArrowUp"; code = "ArrowUp"; vk = 38; }
            case "Home" -> { k = "Home"; code = "Home"; vk = 36; }
            case "End" -> { k = "End"; code = "End"; vk = 35; }
            default -> throw new IllegalArgumentException(name);
        }
        String common = "\"key\":" + DevToolsSession.quote(k) + ",\"code\":" + DevToolsSession.quote(code) + ",\"windowsVirtualKeyCode\":" + vk + ",\"nativeVirtualKeyCode\":" + vk
                + ",\"modifiers\":" + modifiers;
        cdp.call("Input.dispatchKeyEvent", "{\"type\":\"rawKeyDown\"," + common + "}");
        if (text != null) {
            cdp.call("Input.dispatchKeyEvent", "{\"type\":\"char\",\"text\":" + DevToolsSession.quote(text) + "," + common + "}");
        }
        cdp.call("Input.dispatchKeyEvent", "{\"type\":\"keyUp\"," + common + "}");
    }

    private static double[] centre(DevToolsSession cdp, String selector) throws Exception {
        String c = cdp.evaluate("(function(){var e=document.querySelector(" + js(selector) + ");if(!e)return 'none';var r=e.getBoundingClientRect();"
                + "return (r.left+r.width/2)+','+(r.top+r.height/2)})()");
        if (!c.contains(",")) {
            throw new IllegalStateException("no element " + selector);
        }
        String[] p = c.split(",");
        return new double[] {Double.parseDouble(p[0]), Double.parseDouble(p[1])};
    }

    /**
     * Loads a page and gives it focus the way a person's first interaction would: a trusted click on an empty corner (a headless page that has never been clicked does not
     * have focus, and a script's focus() then fires no focus event).
     */
    static void load(DevToolsSession cdp, String url) throws Exception {
        cdp.navigate(url);
        OverlayBrowser.ready(cdp);
        OverlayBrowser.clickAt(cdp, 995, 795);
    }

    /**
     * Runs one action. Trusted input: {@code ctrig:dd} a click on a menu's trigger, {@code citem:dd:n} a click on its n-th entry, {@code click:sel} on anything,
     * {@code cpad:dd} on the menu's own top padding (no control there), {@code key:K} any key {@link #key} knows. {@code ftrig:dd} focuses a trigger (a script's focus, as a
     * person tabbing to it would leave it), {@code blur} takes focus away from wherever it is. The waits are {@code wait:N}, {@code settle} (any exit, the 150ms transitions
     * and the 200ms fallback), and, on a traced menu ({@code trace:dd}), {@code entered} and {@code settled} (FloatingBrowser's).
     *
     * <p>The S1 and S4 actions wait on what the page shows, not on the clock: {@code escape-then:dd:X} is a trusted Escape and, once the menu's exit is visibly under way, X
     * (a trusted click on the trigger, or a key); {@code click-then:dd} a trusted click that closes the menu and, once its exit is under way, a second one. In one task, so
     * no scheduling can change them: {@code reopen:dd} closes and reopens the menu with its trigger (no fade ever runs, so only the old close's timer can end it),
     * {@code reopen-end:dd} the same followed by a transitionend on the menu, the way the end of a reversed fade reaches it while the old close still waits, and
     * {@code escape2} two Escapes on the focused element, one after the other.
     */
    static void perform(DevToolsSession cdp, String action) throws Exception {
        String[] p = action.split(":", 2);
        switch (p[0]) {
            case "ctrig" -> OverlayBrowser.click(cdp, trigger(p[1]));
            case "citem" -> {
                String[] q = p[1].split(":");
                OverlayBrowser.click(cdp, item(q[0], Integer.parseInt(q[1])));
            }
            case "cpad" -> {
                String xy = cdp.evaluate("(function(){var r=" + rootOf(p[1]) + ".querySelector('[data-dropdown-menu-list]').getBoundingClientRect();return (r.left+r.width/2)+','+(r.top+2)})()");
                OverlayBrowser.clickAt(cdp, Double.parseDouble(xy.split(",")[0]), Double.parseDouble(xy.split(",")[1]));
            }
            case "key" -> key(cdp, p[1]);
            case "ftrig" -> cdp.evaluate("(function(){" + rootOf(p[1]) + ".querySelector('[data-dropdown-menu-trigger]').focus();return 'ok'})()");
            case "blur" -> cdp.evaluate("(function(){if(document.activeElement)document.activeElement.blur();return 'ok'})()");
            case "trace" -> FloatingBrowser.traceElement(cdp, rootOf(p[1]) + ".querySelector('[data-dropdown-menu-list]')", p[1]);
            case "entered" -> FloatingBrowser.entered(cdp);
            case "settled" -> FloatingBrowser.settled(cdp);
            case "escape-then" -> {
                String[] q = p[1].split(":");
                double[] t = centre(cdp, trigger(q[0]));
                int before = FloatingBrowser.runs(cdp);
                key(cdp, "Escape");
                FloatingBrowser.exiting(cdp, before);
                if (q[1].equals("click")) {
                    OverlayBrowser.clickAt(cdp, t[0], t[1]);
                } else {
                    key(cdp, q[1]);
                }
            }
            case "click-then" -> {
                double[] t = centre(cdp, trigger(p[1]));
                int before = FloatingBrowser.runs(cdp);
                OverlayBrowser.clickAt(cdp, t[0], t[1]);
                FloatingBrowser.exiting(cdp, before);
                OverlayBrowser.clickAt(cdp, t[0], t[1]);
            }
            case "reopen" -> cdp.evaluate("(function(){var t=" + rootOf(p[1]) + ".querySelector('[data-dropdown-menu-trigger]');t.click();t.click();return 'ok'})()");
            case "reopen-end" -> cdp.evaluate("(function(){var r=" + rootOf(p[1]) + ",t=r.querySelector('[data-dropdown-menu-trigger]');t.click();t.click();"
                    + "r.querySelector('[data-dropdown-menu-list]')" + FloatingBrowser.SYNTHETIC_END + ";return 'ok'})()");
            case "escape2" -> cdp.evaluate("(function(){for(var i=0;i<2;i++){(document.activeElement||document.body).dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',bubbles:true,"
                    + "cancelable:true}))}return 'ok'})()");
            default -> OverlayBrowser.perform(cdp, action);
        }
    }
}
