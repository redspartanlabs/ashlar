package dev.redspartanlabs.ashlar.showcase.cascade;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What the Tooltip and Popover suites share: the REAL templates rendered in this process ({@code ashlar/components/tooltip.jte} and {@code popover.jte}, through
 * {@link OverlayBrowser#renderComponent}), the legacy rendering of that markup ({@link FloatingLegacyMarkup}), the parity cases, the behavior page and its snapshot, and
 * trusted browser input. Measuring, script sets and the trusted keyboard come from {@link OverlayBrowser}, which the Modal and Drawer suites use the same way.
 *
 * <p>The snapshot reads the state the same way from either rendering, never from a class or an attribute only one of them has: a bubble or a surface is "shown" when it is
 * rendered (its computed display is not none), a tooltip is "described" when an element of its component lists the bubble's id in aria-describedby, and a popover is
 * "open" when its anchor says aria-expanded="true".
 */
final class FloatingBrowser {

    private FloatingBrowser() {
    }

    // ---------------------------------------------------------------- the real templates

    static String tooltip(String trigger, String content, String placement) {
        Map<String, Object> params = OverlayBrowser.o("trigger", OverlayBrowser.raw(trigger), "content", content);
        if (placement != null) {
            params.put("placement", placement);
        }
        return OverlayBrowser.renderComponent("tooltip.jte", params);
    }

    static String popover(String trigger, String content, String title, String placement) {
        Map<String, Object> params = OverlayBrowser.o("trigger", OverlayBrowser.raw(trigger), "content", OverlayBrowser.raw(content));
        if (title != null) {
            params.put("title", title);
        }
        if (placement != null) {
            params.put("placement", placement);
        }
        return OverlayBrowser.renderComponent("popover.jte", params);
    }

    /** The legacy rendering of migrated markup. */
    static String legacy(String migrated) throws IOException {
        return new FloatingLegacyMarkup().rewrite(migrated);
    }

    /** The migrated markup in another state: the surface shown (no hidden attribute) and open (data-state="open"), or shown but still in its closed visual state. */
    static String inState(String html, boolean shown, boolean open) {
        return OverlayBrowser.inState(html, shown, open);
    }

    private static final Pattern MARKABLE = Pattern.compile("<(div|h3|button|p|span)(?=[\\s>])");

    /** Marks every element of the components so the scoped legacy stylesheet can address exactly those (the caller's own content is already marked raw). */
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

    static final List<String> PLACEMENTS = List.of("Top", "Right", "Bottom", "Left");

    // The caller's own content, marked raw. Its box is stated in full, inline, so it is the same with and without Tailwind's preflight (which restyles a <button>): the
    // trigger's size is what the bubble and the surface are centered on, and what is compared is the component.
    private static final String TRIGGER_STYLE = "margin:0;padding:2px 6px;border:1px solid rgb(136, 136, 136);border-radius:0;background-color:rgb(238, 238, 238);"
            + "color:rgb(0, 0, 0);font:16px/20px Arial,Helvetica,sans-serif;letter-spacing:normal;text-transform:none;appearance:none;box-sizing:border-box";

    static String trigger(String id, String label) {
        return "<button data-parity=\"raw\" id=\"" + id + "\" type=\"button\" style=\"" + TRIGGER_STYLE + "\">" + label + "</button>";
    }

    /** One line per element and property of the field, as {@link OverlayBrowser#measureScript}, optionally without the caller's own (raw) content. */
    static String measureScript(String fieldId, boolean skipRaw) {
        return OverlayBrowser.measureScript(fieldId).replace("return !(e.closest('svg')&&e.tagName.toLowerCase()!=='svg')",
                "return !(e.closest('svg')&&e.tagName.toLowerCase()!=='svg')&&!(" + skipRaw + "&&e.getAttribute('data-parity')==='raw')");
    }

    /** Navigates and measures every case (nothing is forced: the components have no pseudo-class states of their own). */
    static String measure(DevToolsSession cdp, String url, List<OverlayBrowser.Case> cases, boolean skipRaw) throws Exception {
        cdp.navigate(url);
        String sheets = cdp.evaluate("(function(){var ok=[].every.call(document.styleSheets,function(s){try{return s.cssRules.length>0}catch(e){return false}});return document.styleSheets.length+':'+ok})()");
        if (!sheets.endsWith(":true") || sheets.startsWith("0:")) {
            throw new IllegalStateException("every stylesheet must have loaded - infrastructure failure, not a parity result: " + sheets);
        }
        StringBuilder all = new StringBuilder();
        for (OverlayBrowser.Case c : cases) {
            String script = measureScript(c.id(), skipRaw);
            if (script.equals(OverlayBrowser.measureScript(c.id())) && skipRaw) {
                throw new IllegalStateException("the raw filter could not be applied to the measuring script");
            }
            all.append(cdp.evaluate(script)).append('\n');
        }
        return all.toString();
    }

    static final String BODY = "<p data-parity=\"raw\" style=\"margin:0\">The popover's own content, a sentence long enough to wrap onto a second line.</p>";
    static final String SHORT_TIP = "Saves the draft";
    static final String LONG_TIP = "A tooltip whose label is long enough to reach the bubble's maximum width and wrap onto a second line";

    /** The inline translates tooltip.js writes when it nudges a Top bubble 40px to the right (the pointer is moved back by as much, so it keeps pointing at the trigger). */
    static String nudged(String tooltipHtml) {
        return tooltipHtml.replaceFirst("data-tooltip-bubble", "data-tooltip-bubble style=\"translate: calc(-50% + 40px)\"")
                .replaceFirst("data-tooltip-arrow", "data-tooltip-arrow style=\"translate: calc(-50% - 40px)\"");
    }

    private static String state(String html, String state) {
        return switch (state) {
            case "hidden" -> html;
            case "closed" -> inState(html, true, false);
            case "open" -> inState(html, true, true);
            default -> throw new IllegalArgumentException(state);
        };
    }

    /**
     * Every case the static suites measure: the Tooltip in each placement, hidden, shown in its closed visual state and open, with a label that wraps at the bubble's maximum
     * width, and nudged as tooltip.js nudges it; the Popover in each placement, with a title, in each state, and without a title. Each case's ids are its own.
     */
    static List<OverlayBrowser.Case> cases() {
        Map<String, String> markups = new LinkedHashMap<>();
        for (String p : PLACEMENTS) {
            markups.put("tooltip " + p.toLowerCase(), tooltip(trigger("x-t", p), SHORT_TIP, p));
        }
        markups.put("tooltip top, wrapping label", tooltip(trigger("x-t", "Long"), LONG_TIP, "Top"));
        for (String p : PLACEMENTS) {
            markups.put("popover " + p.toLowerCase() + " with title", popover(trigger("x-p", p), BODY, "A title", p));
        }
        markups.put("popover bottom without title", popover(trigger("x-p", "Plain"), BODY, null, null));
        List<OverlayBrowser.Case> cases = new ArrayList<>();
        int n = 0;
        for (Map.Entry<String, String> m : markups.entrySet()) {
            for (String s : List.of("hidden", "closed", "open")) {
                String id = "fl" + (++n);
                cases.add(new OverlayBrowser.Case(id, m.getKey() + " " + s, ids(marked(OverlayBrowser.withoutScripts(state(m.getValue(), s))), id), List.of()));
            }
        }
        String id = "fl" + (++n);
        cases.add(new OverlayBrowser.Case(id, "tooltip top open, nudged as tooltip.js nudges it",
                ids(marked(OverlayBrowser.withoutScripts(nudged(state(markups.get("tooltip top"), "open")))), id), List.of()));
        return cases;
    }

    /** Gives a case's ids (the trigger's and the generated tooltip-N / popover-N) a prefix of its own, so two renderings of the same markup share them. */
    private static String ids(String html, String prefix) {
        return html.replaceAll("id=\"(tooltip|popover)-\\d+\"", "id=\"" + prefix + "-$1\"").replace("id=\"x-t\"", "id=\"" + prefix + "-t\"").replace("id=\"x-p\"", "id=\"" + prefix + "-p\"");
    }

    /** The migrated and the legacy body of every case, each in its field. */
    static String[] bodies(List<OverlayBrowser.Case> cases) throws IOException {
        StringBuilder migrated = new StringBuilder();
        StringBuilder legacy = new StringBuilder();
        for (OverlayBrowser.Case c : cases) {
            // Every field has room above, below and to the left of its trigger, so no surface reaches outside the page.
            String field = "<div data-field=\"" + c.id() + "\" data-parity=\"raw\" style=\"padding:150px 0 150px 320px\">";
            migrated.append(field).append(c.markup()).append("</div>");
            legacy.append(field).append(legacy(c.markup())).append("</div>");
        }
        return new String[] {migrated.toString(), legacy.toString()};
    }

    // ---------------------------------------------------------------- the behavior page

    /** A component placed at a fixed point of the page, so a surface that opens never covers another trigger a scenario needs. */
    private static String at(int left, int top, String html) {
        return "<div style=\"position:absolute;left:" + left + "px;top:" + top + "px\">" + html + "</div>";
    }

    /**
     * The page every behavior suite runs, at 1000 x 800. The triggers, by id: tt1 a button (Top), tt2 a plain span (so the slot becomes the keyboard stop), tt3 a button that
     * is already described by #t3help, tt4 a span with tabindex="-1"; pp1 a button with a title, text (#p1text) and two controls (#p1in, #p1last) inside; pp2 a plain span (so
     * the slot becomes a button); pp3 a second button; pp4 a span with tabindex="-1"; pno a Popover holding another (pni); ptp a Popover holding a Tooltip (ptt). #before,
     * #outside, #before2 and #after2 are plain buttons around them. Returns the migrated markup with its script tags as the templates wrote them.
     */
    static String scenarioBody() {
        StringBuilder b = new StringBuilder();
        b.append(at(20, 60, "<button id=\"before\" type=\"button\">Before</button>"));
        b.append(at(120, 60, tooltip("<button id=\"tt1\" type=\"button\">Save</button>", "Saves the draft", null)));
        b.append(at(260, 60, tooltip("<span id=\"tt2\">Status</span>", "Plain status text", null)));
        b.append(at(400, 60, tooltip("<button id=\"tt3\" type=\"button\" aria-describedby=\"t3help\">Described</button>", "Has its own help too", null)
                + "<p id=\"t3help\" style=\"margin:0\">Help text</p>"));
        b.append(at(40, 200, popover("<button id=\"pp1\" type=\"button\">Open</button>",
                "<p id=\"p1text\" style=\"margin:0\">Body text</p><button id=\"p1in\" type=\"button\">Inside</button> <button id=\"p1last\" type=\"button\">Last</button>", "Title",
                null)));
        b.append(at(420, 200, popover("<span id=\"pp2\">Details</span>", "<p style=\"margin:0\">Synthetic</p>", null, null)));
        b.append(at(720, 200, popover("<button id=\"pp3\" type=\"button\">Second</button>", "<p style=\"margin:0\">Second popover</p>", null, null)));
        b.append(at(860, 60, "<button id=\"outside\" type=\"button\">Outside</button>"));
        b.append(at(40, 450, popover("<button id=\"pno\" type=\"button\">Outer</button>",
                "<p style=\"margin:0\">Outer body</p>" + popover("<button id=\"pni\" type=\"button\">Inner</button>", "<p style=\"margin:0\">Inner body</p>", null, "Right"), null, null)));
        b.append(at(420, 450, popover("<button id=\"ptp\" type=\"button\">With a tip</button>",
                "<p style=\"margin:0\">Body</p>" + tooltip("<button id=\"ptt\" type=\"button\">Hint</button>", "A hint", "Right"), null, null)));
        b.append(at(20, 700, "<button id=\"before2\" type=\"button\">Before</button>"));
        b.append(at(120, 700, tooltip("<span id=\"tt4\" tabindex=\"-1\">Not tabbable</span>", "Tabindex -1", null)));
        b.append(at(280, 700, popover("<span id=\"pp4\" tabindex=\"-1\">Not tabbable either</span>", "<p style=\"margin:0\">Body</p>", null, "Top")));
        b.append(at(480, 700, "<button id=\"after2\" type=\"button\">After</button>"));
        return b.toString();
    }

    static final List<String> TOOLTIPS = List.of("tt1", "tt2", "tt3", "tt4", "ptt");
    static final List<String> POPOVERS = List.of("pp1", "pp2", "pp3", "pp4", "pno", "pni", "ptp");

    /** The snapshot, in the page: what a Tooltip or a Popover changes, read the same way from either rendering (see the class comment). */
    private static final String SNAPSHOT = """
            window.__fsnap = function () {
              function rendered(e) { return getComputedStyle(e).display !== 'none'; }
              var parts = [];
              %s.forEach(function (id) {
                var root = document.getElementById(id).closest('[data-tooltip]');
                var bubble = root.querySelector('[data-tooltip-bubble]');
                var described = [].some.call(root.querySelectorAll('[aria-describedby]'), function (e) { return e.getAttribute('aria-describedby').split(/\\s+/).indexOf(bubble.id) >= 0; });
                parts.push('tip:' + id + '=' + (rendered(bubble) ? 'shown' : 'hidden') + (described ? '+described' : ''));
              });
              %s.forEach(function (id) {
                var root = document.getElementById(id).closest('[data-popover]');
                var surface = root.querySelector(':scope > [data-popover-surface]');
                var anchor = root.querySelector(':scope > [data-popover-trigger] [aria-expanded], :scope > [data-popover-trigger][aria-expanded]');
                var expanded = anchor && anchor.getAttribute('aria-expanded') === 'true';
                parts.push('pop:' + id + '=' + (expanded ? (rendered(surface) ? 'open' : 'expanded-but-hidden') : (rendered(surface) ? 'closing' : 'closed')));
              });
              var t3 = document.getElementById('tt3');
              var own = t3.closest('[data-tooltip]').querySelector('[data-tooltip-bubble]').id;
              parts.push('db:tt3=' + (t3.getAttribute('aria-describedby') || '-').split(/\\s+/).map(function (t) { return t === own ? 'own' : t; }).join(' '));
              var a = document.activeElement;
              var active = !a || a === document.body ? 'body'
                  : a.matches('[data-tooltip-trigger],[data-popover-trigger]') ? 'slot:' + (a.firstElementChild ? a.firstElementChild.id : '?') : a.id || a.tagName.toLowerCase();
              parts.push('active=' + active);
              var tr = window.__ftrace;
              if (tr) {
                parts.push('exit=' + (tr.timers.filter(function (r) { return r.ms === 200; }).map(function (r) { return r.state; }).join(',') || 'none')
                    + (tr.late !== '-' ? '/' + tr.late : ''));
              }
              return parts.join(';');
            };
            """.formatted(jsList(TOOLTIPS), jsList(POPOVERS));

    private static String jsList(List<String> ids) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ids.size(); i++) {
            sb.append(i == 0 ? "" : ",").append('\'').append(ids.get(i)).append('\'');
        }
        return sb.append(']').toString();
    }

    /** A page: the stylesheets, the body and extra CSS, at a fixed light theme, with the snapshot function. Nothing overrides the transitions unless {@code extraCss} does. */
    static String page(List<String> sheets, String body, String extraCss) {
        StringBuilder links = new StringBuilder();
        sheets.forEach(href -> links.append("<link rel=\"stylesheet\" href=\"").append(href).append("\">"));
        return "<!doctype html><html data-theme=\"light\"><head><meta charset=\"utf-8\">" + links + "<style>" + extraCss + "</style>"
                + "<script>" + SNAPSHOT + "</script></head><body style=\"margin:0;font-family:Arial,Helvetica,sans-serif;font-size:16px\">" + body + "</body></html>";
    }

    /** The stylesheet a rendering is shown with: the legacy markup on the Tooltip and Popover oracle, the migrated markup on Ashlar's own stylesheet alone. */
    static List<String> sheets(boolean legacyRendering) {
        return legacyRendering ? List.of(ParityFrames.generated("floating-oracle.css")) : List.of(ParityFrames.ASHLAR);
    }

    /** The behavior page with the given script set ("/ashlar/js/" for the real scripts), in the migrated or the legacy rendering. */
    static String scenarioPage(boolean legacyRendering, String scriptBase, String extraCss) throws IOException {
        String body = scenarioBody();
        if (legacyRendering) {
            body = legacy(body);
        }
        return page(sheets(legacyRendering), OverlayBrowser.withScripts(body, scriptBase), extraCss);
    }

    /**
     * The edge page, at 1000 x 800: a Top tooltip (et) on a trigger 60px from the left edge, whose 16rem bubble has to be nudged right (by less than half its width, so its
     * pointer can still reach the trigger's centre), and a Bottom popover (ep) on a trigger at x = 900, which has to be nudged left, and nudged again when the window gets
     * narrower.
     */
    static String edgePage(boolean legacyRendering, String scriptBase) throws IOException {
        String body = "<div style=\"position:absolute;left:60px;top:120px\">" + tooltip("<button id=\"et\" type=\"button\">E</button>",
                "A tooltip near the left edge that has to be nudged", null) + "</div>"
                + "<div style=\"position:absolute;left:900px;top:300px\">" + popover("<button id=\"ep\" type=\"button\">Edge</button>", "<p style=\"margin:0\">A nudged popover</p>",
                "Edge", null) + "</div>";
        if (legacyRendering) {
            body = legacy(body);
        }
        return page(sheets(legacyRendering), OverlayBrowser.withScripts(body, scriptBase), "");
    }

    // ---------------------------------------------------------------- trusted input and actions

    static String js(String s) {
        return DevToolsSession.quote(s);
    }

    /** A trusted pointer move to the centre of the element (it must be on screen). */
    static void hover(DevToolsSession cdp, String selector) throws Exception {
        String centre = cdp.evaluate("(function(){var e=document.querySelector(" + js(selector) + ");if(!e)return 'none';var r=e.getBoundingClientRect();"
                + "return (r.left+r.width/2)+','+(r.top+r.height/2)})()");
        if (!centre.contains(",")) {
            throw new IllegalStateException("cannot hover " + selector + ": " + centre);
        }
        String[] xy = centre.split(",");
        moveTo(cdp, Double.parseDouble(xy[0]), Double.parseDouble(xy[1]));
    }

    static void moveTo(DevToolsSession cdp, double x, double y) throws Exception {
        cdp.call("Input.dispatchMouseEvent", "{\"type\":\"mouseMoved\",\"x\":" + x + ",\"y\":" + y + ",\"button\":\"none\"}");
    }

    /** A trusted Space (OverlayBrowser's keyboard has no Space). */
    static void space(DevToolsSession cdp) throws Exception {
        String common = "\"key\":\" \",\"code\":\"Space\",\"windowsVirtualKeyCode\":32,\"nativeVirtualKeyCode\":32,\"modifiers\":0";
        cdp.call("Input.dispatchKeyEvent", "{\"type\":\"rawKeyDown\"," + common + "}");
        cdp.call("Input.dispatchKeyEvent", "{\"type\":\"char\",\"text\":\" \"," + common + "}");
        cdp.call("Input.dispatchKeyEvent", "{\"type\":\"keyUp\"," + common + "}");
    }

    // ---------------------------------------------------------------- the exit trace, and waiting on what the page shows rather than on the clock

    /** The floating element (a Tooltip's bubble or a Popover's own surface) of the component whose trigger content has this id. */
    static String floatingOf(String id) {
        return "(function(){var r=document.getElementById(" + js(id) + ").closest('[data-tooltip],[data-popover]');"
                + "return r.matches('[data-tooltip]')?r.querySelector('[data-tooltip-bubble]'):r.querySelector(':scope > [data-popover-surface]')})()";
    }

    /**
     * Installs the exit trace on one component, for the rest of the page's life. Both scripts, legacy and migrated, end an exit either with the floating element's
     * transitionend or with a fallback timer of {@code TRANSITION_DURATION_MS + 50} = 200ms, and whichever comes first stops the other. The trace records, without changing
     * either: every 150ms (show delay) and 200ms (fallback) timer and whether it fired or was cleared first; the element's own trusted transitionrun, transitioncancel and
     * transitionend events; and a frame count. The snapshot then reports {@code exit=<the 200ms timers' outcomes>}: for each exit, in order, which of its two endings came
     * first (cleared: its transitionend, whose handler stops the timer; fired: its timer), followed after a {@code latefocus} by {@code /refocused} (focus came back late
     * in the fade) or {@code /missed} (the exit ended first). That is what a stale-exit check needs to know it exercised the path it is aimed at. (The legacy scripts never
     * clear their timer, so on them every exit reads fired.)
     *
     * <p>Whether the element is shown is read the way both renderings share, from its computed display (the legacy markup toggles a hidden class and has no data-state).
     */
    static void trace(DevToolsSession cdp, String id) throws Exception {
        traceElement(cdp, floatingOf(id), id);
    }

    /** {@link #trace} on the element a page expression gives (the Dropdown Menu suites trace a menu), named {@code what} in a failure. */
    static void traceElement(DevToolsSession cdp, String elementExpression, String what) throws Exception {
        String id = what;
        String r = cdp.evaluate("(function(){if(window.__ftrace)return 'already traced';var el=" + elementExpression + ";"
                + "var tr={el:el,timers:[],runs:0,cancels:0,ends:0,frame:0,quiet:0,late:'-'};function changed(){tr.quiet=tr.frame}"
                + "var st=window.setTimeout,ct=window.clearTimeout;"
                + "window.setTimeout=function(fn,ms){if(typeof fn!=='function'||(ms!==150&&ms!==200))return st.apply(window,arguments);"
                + "var rec={ms:ms,state:'pending'},rest=[].slice.call(arguments,2);"
                + "rec.id=st.call(window,function(){if(rec.state==='pending'){rec.state='fired';changed()}return fn.apply(this,rest)},ms);tr.timers.push(rec);changed();return rec.id};"
                + "window.clearTimeout=function(id){tr.timers.forEach(function(r){if(r.id===id&&r.state==='pending'){r.state='cleared';changed()}});return ct.apply(window,arguments)};"
                + "el.addEventListener('transitionrun',function(e){if(e.target===el&&e.isTrusted){tr.runs++;changed()}});"
                + "el.addEventListener('transitioncancel',function(e){if(e.target===el&&e.isTrusted){tr.cancels++;changed()}});"
                + "el.addEventListener('transitionend',function(e){if(e.target===el&&e.isTrusted){tr.ends++;changed()}});"
                + "(function tick(){tr.frame++;requestAnimationFrame(tick)})();window.__ftrace=tr;return 'ok'})()");
        if (!r.equals("ok")) {
            throw new IllegalStateException("cannot trace " + id + ": " + r);
        }
    }

    /**
     * In the page, on the traced element: no transition of it running, every one that ran ended or was cancelled (its events delivered; a hidden element's transitions
     * just stop), no traced timer pending, and two quiet frames since the last of those changed.
     */
    private static final String QUIET = "var shown=getComputedStyle(el).display!=='none';var run=el.getAnimations().some(function(a){return a.playState==='running'});"
            + "var quiet=!run&&(!shown||tr.ends+tr.cancels>=tr.runs)&&tr.frame-tr.quiet>=2&&tr.timers.every(function(r){return r.state!=='pending'});";

    /** Polls a condition in the page (it returns 'yes', 'no', or 'fail:' and why) until it holds; a 'fail:' or 3s without it means the step could not be performed. */
    static void until(DevToolsSession cdp, String what, String condition) throws Exception {
        long deadline = System.nanoTime() + 3_000_000_000L;
        while (true) {
            String r = cdp.evaluate("(function(){var tr=window.__ftrace;if(!tr)return 'fail:nothing is traced';var el=tr.el;" + condition + "})()");
            if (r.equals("yes")) {
                return;
            }
            if (r.startsWith("fail:") || System.nanoTime() > deadline) {
                throw new IllegalStateException(what + ": " + (r.startsWith("fail:") ? r.substring(5) : "not within 3s"));
            }
            Thread.sleep(1);
        }
    }

    /** The traced element is shown and its entry has ended, its transitionend delivered (an exit begun now cannot be ended by it). */
    static void entered(DevToolsSession cdp) throws Exception {
        until(cdp, "the entry did not end", QUIET + "return quiet&&shown&&tr.ends>0?'yes':'no';");
    }

    /** Everything the traced element had pending has ended: both endings of every exit, any show delay, and every transition, its events delivered. */
    static void settled(DevToolsSession cdp) throws Exception {
        until(cdp, "it did not settle", QUIET + "return quiet?'yes':'no';");
    }

    /** The number of the traced element's transitions that have started so far. */
    static int runs(DevToolsSession cdp) throws Exception {
        return Integer.parseInt(cdp.evaluate("String(window.__ftrace.runs)"));
    }

    /**
     * Waits, after a trusted leave, until the traced element's exit is really under way: a transition of it has started since {@code runsBefore} and its opacity has
     * visibly left 1 (so coming back now reverses a fade that has run, rather than one that never started); still on screen, or the step fails.
     */
    static void exiting(DevToolsSession cdp, int runsBefore) throws Exception {
        until(cdp, "the exit did not get under way", "if(getComputedStyle(el).display==='none')return 'fail:the exit ended before the pointer or focus could come back';"
                + "return tr.runs>" + runsBefore + "&&+getComputedStyle(el).opacity<0.95?'yes':'no';");
    }

    static final String SYNTHETIC_END = ".dispatchEvent(new TransitionEvent('transitionend',{propertyName:'opacity',bubbles:true}))";

    /**
     * Loads a page and gives it focus the way a person's first interaction would: a trusted click on an empty corner (a headless page that has never been clicked does not
     * have focus, and a script's {@code focus()} then fires no focus event), with the pointer left there.
     */
    static void load(DevToolsSession cdp, String url) throws Exception {
        cdp.navigate(url);
        OverlayBrowser.ready(cdp);
        OverlayBrowser.clickAt(cdp, 995, 795);
    }

    /**
     * Runs one action: {@code hover:sel} and {@code away} (the pointer to an empty corner) are trusted pointer moves; {@code key:Space} a trusted Space; {@code focusslot:id}
     * focuses the trigger slot around the element; and everything else is {@link OverlayBrowser#perform} ({@code click:}, {@code key:}, {@code focus:}, {@code wait:},
     * {@code js:}; {@code settle} waits out any exit, the 150ms transitions and either script's 200ms fallback).
     *
     * <p>On a traced component ({@code trace:id}, see {@link #trace}), the stale-exit actions wait on what the page shows, not on the clock: {@code entered} and
     * {@code settled} (see those); {@code reenter:sel}, a trusted move away and, once the exit is under way ({@link #exiting}), a trusted move back onto the element (its
     * centre measured beforehand); {@code retab}, a trusted Tab and, once the exit is under way, a trusted Shift+Tab; {@code latefocus:id}, a blur and, in the page, a
     * focus in the first frame the fading bubble is under 5% opaque (late in its fade: the reversed fade then outlasts the old exit's 200ms timer); {@code refocus:id}, a
     * blur and a focus in one task, so the exit's fade never runs and nothing but the old exit's timer can end it; {@code refocus-end:id}, the same followed, still in
     * that task, by a transitionend on the bubble, the way the end of a reversed fade arrives while the old exit still waits; and {@code reclick-end:id}, a close, an
     * open and that transitionend on the surface, in one task.
     */
    static void perform(DevToolsSession cdp, String action) throws Exception {
        String[] p = action.split(":", 2);
        switch (p[0]) {
            case "hover" -> hover(cdp, p[1]);
            case "away" -> moveTo(cdp, 990, 790);
            case "trace" -> trace(cdp, p[1]);
            case "entered" -> entered(cdp);
            case "settled" -> settled(cdp);
            case "reenter" -> {
                String centre = cdp.evaluate("(function(){var r=document.querySelector(" + js(p[1]) + ").getBoundingClientRect();return (r.left+r.width/2)+','+(r.top+r.height/2)})()");
                String[] xy = centre.split(",");
                int before = runs(cdp);
                moveTo(cdp, 990, 790);
                exiting(cdp, before);
                moveTo(cdp, Double.parseDouble(xy[0]), Double.parseDouble(xy[1]));
            }
            case "retab" -> {
                int before = runs(cdp);
                OverlayBrowser.press(cdp, "Tab");
                exiting(cdp, before);
                OverlayBrowser.press(cdp, "Shift+Tab");
            }
            case "latefocus" -> cdp.evaluate("(function(){var t=document.getElementById(" + js(p[1]) + "),tr=window.__ftrace,el=tr.el;t.blur();"
                    + "(function watch(){var cs=getComputedStyle(el);if(cs.display==='none'){tr.late='missed';return}if(+cs.opacity<0.05){tr.late='refocused';t.focus();return}"
                    + "requestAnimationFrame(watch)})();return 'ok'})()");
            case "refocus" -> cdp.evaluate("(function(){var t=document.getElementById(" + js(p[1]) + ");t.blur();t.focus();return 'ok'})()");
            case "refocus-end" -> cdp.evaluate("(function(){var t=document.getElementById(" + js(p[1]) + ");t.blur();t.focus();" + floatingOf(p[1]) + SYNTHETIC_END + ";return 'ok'})()");
            case "reclick-end" -> cdp.evaluate("(function(){var b=document.getElementById(" + js(p[1]) + ");b.click();b.click();" + floatingOf(p[1]) + SYNTHETIC_END + ";return 'ok'})()");
            case "focusslot" -> cdp.evaluate("(function(){document.getElementById(" + js(p[1]) + ").closest('[data-tooltip-trigger],[data-popover-trigger]').focus();return 'ok'})()");
            case "key" -> {
                if (p[1].equals("Space")) {
                    space(cdp);
                } else {
                    OverlayBrowser.press(cdp, p[1]);
                }
            }
            default -> OverlayBrowser.perform(cdp, action);
        }
    }

    static String snapshot(DevToolsSession cdp) throws Exception {
        return cdp.evaluate("window.__fsnap()");
    }
}
