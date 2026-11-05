package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The real {@code accordion.js} running against real Accordion and Accordion Item markup and the real {@code ashlar.css} in Chrome: the initial rendered state is
 * consistent, a click expands or collapses a section, opening one closes the one that was open (single-open), independent accordions never touch each other, and
 * {@code aria-expanded} and {@code inert} stay in step. The script is loaded exactly as served (as a module, from a Blob), and every check reads the DOM and the computed
 * style the browser produced. Nothing is mocked.
 *
 * <p>What it proves about the migration: toggling changes the two synchronised attributes and nothing else. Every scenario compares the {@code class} and {@code style}
 * attribute of every accordion element before and after (they must not change, and no panel ever gains an inline {@code grid-template-rows}), the chevron turn and the
 * collapsed row are answered by the stylesheet from {@code aria-expanded} and {@code inert} alone (shown by setting only the attributes, with no script involved), and a
 * collapsed section's content cannot take focus. Enter and Space on a focused button are the native activation of a {@code <button>} (a click), which the click
 * scenarios drive; a browser cannot be handed a trusted key press here.
 */
class AccordionScriptTest {

    private static final Map<String, String[]> RESULTS = new LinkedHashMap<>();

    private static final List<String> EXPECTED = List.of(
            "the server-rendered state is consistent before the script runs", "init changes no attribute, class or style", "a collapsed section is a zero row and an expanded one is open",
            "a click expands a collapsed section and sets both attributes", "a click collapses an expanded section and sets both attributes", "opening one section closes the one that was open",
            "at most one section is ever expanded through a sequence of clicks", "aria-expanded and inert agree after every click", "the chevron turns with aria-expanded and the row follows inert",
            "a click changes no class and writes no inline style", "no panel ever has an inline grid-template-rows", "the chevron class never changes",
            "accordions are independent", "a click inside the content does not toggle", "a collapsed section's content cannot take focus and an expanded one's can",
            "the button keeps focus after a click", "the stylesheet alone answers aria-expanded and inert with no script", "a single click toggles exactly once");

    private static final String SCRIPT = """
            (async function () {
              var out = [];
              function rec(name, ok, detail) { out.push([name, ok ? 'ok' : 'FAIL', detail || ''].join('\\t')); }
              function $(id) { return document.getElementById(id); }
              function cs(el, p) { return getComputedStyle(el).getPropertyValue(p); }
              function btn(g, i) { return $(g + '-button-' + i); }
              function panel(g, i) { return $(g + '-panel-' + i); }
              function chev(g, i) { return btn(g, i).querySelector('[data-accordion-chevron]'); }
              function attrs() { return [].map.call(document.querySelectorAll('[class*="ash-accordion"]'), function (e, n) { return n + ':' + (e.getAttribute('class') || '') + '|' + (e.getAttribute('style') || ''); }).join('\\n'); }
              function state() { return [].map.call(document.querySelectorAll('[data-accordion-button]'), function (b) { var p = $(b.getAttribute('aria-controls')); return b.id + '=' + b.getAttribute('aria-expanded') + '/' + p.hasAttribute('inert'); }).join(','); }
              function open(g) { return [].filter.call(document.querySelectorAll('#' + g + ' [data-accordion-button]'), function (b) { return b.getAttribute('aria-expanded') === 'true'; }).map(function (b) { return b.id; }).join(','); }
              function synced() { return [].every.call(document.querySelectorAll('[data-accordion-button]'), function (b) { var p = $(b.getAttribute('aria-controls')); return (b.getAttribute('aria-expanded') === 'true') === !p.inert && p.inert === p.hasAttribute('inert'); }); }
              function rowsOk() { return [].every.call(document.querySelectorAll('[data-accordion-button]'), function (b) {
                var p = $(b.getAttribute('aria-controls')); var c = b.querySelector('[data-accordion-chevron]'); var expanded = b.getAttribute('aria-expanded') === 'true';
                return expanded ? (p.offsetHeight > 0 && cs(c, 'rotate') === '90deg') : (p.offsetHeight === 0 && cs(c, 'rotate') === 'none'); }); }
              try {
                var src = document.getElementById('src').textContent;
                var mod = URL.createObjectURL(new Blob([src], { type: 'text/javascript' }));
                var initial = state(), beforeAttrs = attrs();
                rec('the server-rendered state is consistent before the script runs', synced() && initial === 'X-button-a=false/true,X-button-b=true/false,X-button-c=false/true,Y-button-p=false/true,Y-button-q=false/true,Z-button-m=false/true,Z-button-n=true/false', initial);
                rec('a collapsed section is a zero row and an expanded one is open', rowsOk(), initial);
                await import(mod);
                document.dispatchEvent(new Event('DOMContentLoaded'));
                rec('init changes no attribute, class or style', state() === initial && attrs() === beforeAttrs, state());
                var start = attrs();

                // expand a collapsed section, collapse an expanded one, single-open
                btn('X', 'a').click();
                rec('a click expands a collapsed section and sets both attributes', btn('X', 'a').getAttribute('aria-expanded') === 'true' && !panel('X', 'a').inert && !panel('X', 'a').hasAttribute('inert'), state());
                rec('opening one section closes the one that was open', open('X') === 'X-button-a' && btn('X', 'b').getAttribute('aria-expanded') === 'false' && panel('X', 'b').inert, open('X'));
                var xs = [open('X')];
                btn('X', 'c').click(); xs.push(open('X'));
                btn('X', 'c').click(); xs.push(open('X'));
                btn('X', 'b').click(); xs.push(open('X'));
                btn('X', 'a').click(); xs.push(open('X'));
                rec('a click collapses an expanded section and sets both attributes', btn('X', 'c').getAttribute('aria-expanded') === 'false' && panel('X', 'c').inert && xs[2] === '', xs.join(' > '));
                rec('at most one section is ever expanded through a sequence of clicks', xs.every(function (v) { return v.split(',').filter(Boolean).length <= 1; }) && xs.join('>') === 'X-button-a>X-button-c>>X-button-b>X-button-a', xs.join(' > '));
                rec('aria-expanded and inert agree after every click', synced(), state());
                rec('the chevron turns with aria-expanded and the row follows inert', rowsOk(), state());
                rec('a click changes no class and writes no inline style', attrs() === start, 'attributes changed');
                rec('no panel ever has an inline grid-template-rows', [].every.call(document.querySelectorAll('[data-accordion-panel]'), function (p) { return !p.hasAttribute('style') && p.style.gridTemplateRows === ''; }), 'a panel has an inline style');
                rec('the chevron class never changes', [].every.call(document.querySelectorAll('[data-accordion-chevron]'), function (c) { return c.getAttribute('class') === 'ash-accordion__chevron'; }), 'a chevron class changed');

                // independent accordions
                var xBefore = open('X'), zBefore = open('Z');
                btn('Y', 'p').click(); btn('Y', 'q').click();
                rec('accordions are independent', open('Y') === 'Y-button-q' && open('X') === xBefore && open('Z') === zBefore && zBefore === 'Z-button-n', 'X=' + open('X') + ' Y=' + open('Y') + ' Z=' + open('Z'));

                // a click inside the content does not toggle
                var contentOpen = open('X'); $('X-inner-a').click();
                rec('a click inside the content does not toggle', open('X') === contentOpen && open('X') === 'X-button-a', open('X'));

                // focus
                var innerOpen = $('X-inner-a'); innerOpen.focus();
                var okOpen = document.activeElement === innerOpen;
                var innerClosed = $('X-inner-b'); innerClosed.focus();
                var okClosed = document.activeElement !== innerClosed;
                rec('a collapsed section\\'s content cannot take focus and an expanded one\\'s can', okOpen && okClosed, 'open=' + okOpen + ' closed=' + okClosed);
                btn('X', 'b').focus(); btn('X', 'b').click();
                rec('the button keeps focus after a click', document.activeElement === btn('X', 'b'), String(document.activeElement && document.activeElement.id));

                // the stylesheet alone: attributes only, no script (W has no script handlers because it is added after init)
                var w = document.createElement('div'); w.innerHTML = document.getElementById('w-template').innerHTML; document.body.appendChild(w);
                var wb = $('W-button-s'), wp = $('W-panel-s'), wc = wb.querySelector('[data-accordion-chevron]');
                var closedOk = wp.offsetHeight === 0 && cs(wc, 'rotate') === 'none';
                wb.setAttribute('aria-expanded', 'true'); wp.removeAttribute('inert');
                var openOk = wp.offsetHeight > 0 && cs(wc, 'rotate') === '90deg';
                wb.setAttribute('aria-expanded', 'false'); wp.setAttribute('inert', '');
                var againOk = wp.offsetHeight === 0 && cs(wc, 'rotate') === 'none';
                rec('the stylesheet alone answers aria-expanded and inert with no script', closedOk && openOk && againOk, closedOk + ' ' + openOk + ' ' + againOk);

                // one handler per button: a click that ran twice would toggle back
                var before2 = btn('Z', 'm').getAttribute('aria-expanded');
                btn('Z', 'm').click();
                rec('a single click toggles exactly once', before2 === 'false' && btn('Z', 'm').getAttribute('aria-expanded') === 'true' && open('Z') === 'Z-button-m', open('Z'));
              } catch (e) {
                rec('SCRIPT ERROR', false, String(e && e.stack || e));
              }
              var pre = document.createElement('pre'); pre.id = 'ash-accordion-js'; pre.textContent = out.join('\\n'); document.body.appendChild(pre);
            })();
            """;

    private static String section(String group, String id, String title, boolean expanded) {
        return "<div data-accordion-item class=\"ash-accordion__item\"><h3 class=\"ash-accordion__heading\">"
                + "<button type=\"button\" id=\"" + group + "-button-" + id + "\" data-accordion-button aria-expanded=\"" + expanded + "\" aria-controls=\"" + group + "-panel-" + id + "\" class=\"ash-accordion__button\">"
                + "<span class=\"ash-accordion__title\">" + title + "</span><span data-accordion-chevron class=\"ash-accordion__chevron\"><svg class=\"ash-icon ash-icon--sm\" viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M9 5l7 7-7 7\"/></svg></span>"
                + "</button></h3><div id=\"" + group + "-panel-" + id + "\" data-accordion-panel role=\"region\" aria-labelledby=\"" + group + "-button-" + id + "\"" + (expanded ? "" : " inert") + " class=\"ash-accordion__panel\">"
                + "<div class=\"ash-accordion__clip\"><div class=\"ash-accordion__content\"><p>Content of " + title + "</p><button type=\"button\" id=\"" + group + "-inner-" + id + "\">Inner</button></div></div></div></div>";
    }

    private static String accordion(String id, String sections) {
        return "<div id=\"" + id + "\" data-accordion class=\"ash-accordion\" style=\"margin-bottom:8px\">" + sections + "</div>";
    }

    private static String markup() {
        return accordion("X", section("X", "a", "A", false) + section("X", "b", "B", true) + section("X", "c", "C", false))
                + accordion("Y", section("Y", "p", "P", false) + section("Y", "q", "Q", false))
                + accordion("Z", section("Z", "m", "M", false) + section("Z", "n", "N", true));
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("accordion-script");
        String js;
        try (InputStream in = AccordionScriptTest.class.getResourceAsStream("/META-INF/resources/ashlar/js/accordion.js")) {
            if (in == null) {
                throw new IOException("accordion.js is not on the test classpath");
            }
            js = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertFalse(js.contains("</script"), "the script text can be embedded in a text/plain script element");
        String page = "<!doctype html><html data-theme=\"light\"><head><link rel=\"stylesheet\" href=\"" + ParityFrames.ASHLAR + "\"><style>*,*::before,*::after{transition:none !important}</style></head><body style=\"margin:8px\">"
                + markup() + "<template id=\"w-template\">" + accordion("W", section("W", "s", "S", false)).replace("style=\"margin-bottom:8px\"", "") + "</template>"
                + "<script id=\"src\" type=\"text/plain\">" + js + "</script><script>window.addEventListener('load',function(){" + SCRIPT.replace("</script", "<\\/script")
                + "});</script></body></html>";
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            String dom = session.chrome().dumpDom(server.registerRaw(page));
            for (String line : ParityProbe.lines(dom, "ash-accordion-js")) {
                String[] f = line.split("\t", -1);
                RESULTS.put(f[0], f);
            }
        }
    }

    @Test
    void thePageRanTheScriptAndEveryScenarioReported() {
        assertFalse(RESULTS.containsKey("SCRIPT ERROR"), () -> RESULTS.get("SCRIPT ERROR") == null ? "" : String.join(" | ", RESULTS.get("SCRIPT ERROR")));
        for (String name : EXPECTED) {
            assertTrue(RESULTS.containsKey(name), "scenario did not report: " + name + " in " + RESULTS.keySet());
        }
        assertEquals(EXPECTED.size(), RESULTS.size(), RESULTS.keySet().toString());
    }

    @Test
    void everyScenarioPassedInRealChrome() {
        for (String name : EXPECTED) {
            String[] result = RESULTS.get(name);
            assertTrue(result != null && result[1].equals("ok"), name + " -> " + (result == null ? "missing" : String.join(" | ", result)));
        }
    }
}
