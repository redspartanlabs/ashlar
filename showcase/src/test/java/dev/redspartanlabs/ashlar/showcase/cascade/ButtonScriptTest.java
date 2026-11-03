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
 * The real {@code button.js} running against real Button markup and the real {@code ashlar.css} in Chrome: {@code setButtonLoading}, the toggle, the
 * capture-phase click guard, loading-on-submit and the page-cache restore. The script is loaded exactly as served (as a module, from a Blob), its
 * export is the one called, and every check reads the DOM and the computed style the browser produced. Nothing is mocked.
 *
 * <p>What it proves about the migration: loading and pressed are attributes the script sets and the stylesheet answers, so no class changes (every
 * scenario compares the {@code class} of the button and of its parts before and after), the spinner and the icon follow {@code aria-busy} alone (a
 * scenario sets the attribute by hand and sees them change), loading never sets the native {@code disabled}, and a loading button is inert for clicks
 * and submissions.
 */
class ButtonScriptTest {

    private static final Map<String, String[]> RESULTS = new LinkedHashMap<>();

    private static final List<String> EXPECTED = List.of(
            "loading sets aria-busy and aria-disabled and the status text", "loading never sets the native disabled and the button stays focusable",
            "loading shows the spinner and hides the icon through the stylesheet", "loading changes no class", "loading twice changes nothing (idempotent)",
            "not loading removes the state and restores the spinner and the icon", "a server-rendered loading button is restored by setButtonLoading(false)",
            "a spinner and an icon follow aria-busy alone, without the script", "a non-button and a non-Button element are ignored",
            "a loading button swallows the click before any other listener", "a loading submit button does not submit", "an idle submit button submits and other listeners run",
            "loadingOnSubmit loads the button when the form really submits", "loadingOnSubmit leaves a submit that something else cancelled alone",
            "loadingOnSubmit does nothing when native validation fails", "pageshow from the back-forward cache resets a loadingOnSubmit button",
            "pageshow leaves a manually loading button and a normal load alone", "a toggle flips aria-pressed and the label on click", "a toggle changes no class and the pressed look follows aria-pressed",
            "a toggle that starts pressed unpresses on click", "a toggle can be clicked repeatedly", "a toggle whose pressed text is empty keeps its label");

    private static final String SCRIPT = """
            (async function () {
              var out = [];
              function rec(name, ok, detail) { out.push([name, ok ? 'ok' : 'FAIL', detail || ''].join('\\t')); }
              function $(id) { return document.getElementById(id); }
              function cs(el, p) { return getComputedStyle(el).getPropertyValue(p); }
              function classes(el) { return [el].concat([].slice.call(el.querySelectorAll('*'))).map(function (e) { return e.getAttribute('class') || ''; }).join('|'); }
              try {
                var src = document.getElementById('src').textContent;
                var mod = await import(URL.createObjectURL(new Blob([src], { type: 'text/javascript' })));
                document.dispatchEvent(new Event('DOMContentLoaded'));
                window.addEventListener('submit', function (e) { e.preventDefault(); });
                var set = mod.setButtonLoading;
                var b = $('plain');
                var before = classes(b);
                var spinner = b.querySelector('[data-btn-spinner]'), icon = b.querySelector('[data-btn-icon]'), status = b.querySelector('[data-btn-status]');
                set(b, true);
                rec('loading sets aria-busy and aria-disabled and the status text', b.getAttribute('aria-busy') === 'true' && b.getAttribute('aria-disabled') === 'true' && status.textContent === 'Loading' && cs(spinner, 'display') !== 'none',
                    b.outerHTML.slice(0, 200));
                b.focus();
                rec('loading never sets the native disabled and the button stays focusable', !b.hasAttribute('disabled') && b.disabled === false && document.activeElement === b, 'disabled=' + b.disabled);
                rec('loading shows the spinner and hides the icon through the stylesheet', cs(spinner, 'display') !== 'none' && cs(icon, 'display') === 'none' && cs(b, 'opacity') === '0.6' && cs(b, 'cursor') === 'not-allowed',
                    'spinner=' + cs(spinner, 'display') + ' icon=' + cs(icon, 'display') + ' opacity=' + cs(b, 'opacity'));
                rec('loading changes no class', classes(b) === before, classes(b));
                var once = b.outerHTML;
                set(b, true);
                rec('loading twice changes nothing (idempotent)', b.outerHTML === once, 'second call changed the markup');
                set(b, false);
                rec('not loading removes the state and restores the spinner and the icon', !b.hasAttribute('aria-busy') && !b.hasAttribute('aria-disabled') && status.textContent === ''
                    && cs(spinner, 'display') === 'none' && cs(icon, 'display') !== 'none' && classes(b) === before && cs(b, 'opacity') === '1', b.outerHTML.slice(0, 200));
                var s = $('server-loading');
                var sSpinner = s.querySelector('[data-btn-spinner]');
                var shown = cs(sSpinner, 'display') !== 'none';
                set(s, false);
                rec('a server-rendered loading button is restored by setButtonLoading(false)', shown && !s.hasAttribute('aria-busy') && cs(sSpinner, 'display') === 'none' && s.querySelector('[data-btn-status]').textContent === '',
                    'shownBefore=' + shown);
                var m = $('manual');
                var mSpinner = m.querySelector('[data-btn-spinner]'), mIcon = m.querySelector('[data-btn-icon]');
                m.setAttribute('aria-busy', 'true');
                var follows = cs(mSpinner, 'display') !== 'none' && cs(mIcon, 'display') === 'none';
                m.removeAttribute('aria-busy');
                rec('a spinner and an icon follow aria-busy alone, without the script', follows && cs(mSpinner, 'display') === 'none' && cs(mIcon, 'display') !== 'none', 'follows=' + follows);
                var div = document.createElement('div'); div.textContent = 'x'; document.body.appendChild(div);
                var nodata = $('nodata'); var nodataHtml = nodata.outerHTML;
                var threw = false;
                try { set(div, true); set(null, true); set(undefined, true); set(nodata, true); } catch (e) { threw = true; }
                rec('a non-button and a non-Button element are ignored', !threw && !div.hasAttribute('aria-busy') && nodata.outerHTML === nodataHtml, 'threw=' + threw);

                // the click guard
                var g = $('guard'); var hits = { own: 0, doc: 0, form: 0 };
                g.addEventListener('click', function () { hits.own++; });
                var docListener = function () { hits.doc++; };
                document.addEventListener('click', docListener);
                set(g, true); g.click();
                var loadingHits = JSON.stringify(hits);
                rec('a loading button swallows the click before any other listener', hits.own === 0 && hits.doc === 0, loadingHits);
                var f0 = $('f0'); var submits = 0; f0.addEventListener('submit', function () { submits++; });
                var sb = $('sb0'); set(sb, true); sb.click();
                rec('a loading submit button does not submit', submits === 0, 'submits=' + submits);
                set(sb, false); sb.click();
                set(g, false); g.click();
                rec('an idle submit button submits and other listeners run', submits === 1 && hits.own === 1 && hits.doc >= 1, 'submits=' + submits + ' ' + JSON.stringify(hits));
                document.removeEventListener('click', docListener);

                // loading on submit
                var f1 = $('f1'), sb1 = $('sb1');
                var plainBefore = classes(sb1);
                f1.requestSubmit(sb1);
                rec('loadingOnSubmit loads the button when the form really submits', sb1.getAttribute('aria-busy') === 'true' && sb1.getAttribute('aria-disabled') === 'true' && !sb1.disabled
                    && sb1.querySelector('[data-btn-status]').textContent === 'Loading' && classes(sb1) === plainBefore, sb1.outerHTML.slice(0, 220));
                var f2 = $('f2'), sb2 = $('sb2');
                f2.addEventListener('submit', function (e) { e.preventDefault(); });
                f2.requestSubmit(sb2);
                rec('loadingOnSubmit leaves a submit that something else cancelled alone', !sb2.hasAttribute('aria-busy'), sb2.outerHTML.slice(0, 160));
                var f3 = $('f3'), sb3 = $('sb3'); var f3Submits = 0;
                f3.addEventListener('submit', function () { f3Submits++; });
                f3.requestSubmit(sb3);
                rec('loadingOnSubmit does nothing when native validation fails', f3Submits === 0 && !sb3.hasAttribute('aria-busy'), 'submits=' + f3Submits);

                // pageshow
                var man = $('manual2'); set(man, true);
                window.dispatchEvent(new PageTransitionEvent('pageshow', { persisted: false }));
                var afterPlain = sb1.getAttribute('aria-busy') === 'true' && man.getAttribute('aria-busy') === 'true';
                window.dispatchEvent(new PageTransitionEvent('pageshow', { persisted: true }));
                rec('pageshow from the back-forward cache resets a loadingOnSubmit button', !sb1.hasAttribute('aria-busy') && !sb1.hasAttribute('aria-disabled') && sb1.querySelector('[data-btn-status]').textContent === ''
                    && cs(sb1.querySelector('[data-btn-spinner]'), 'display') === 'none', sb1.outerHTML.slice(0, 200));
                rec('pageshow leaves a manually loading button and a normal load alone', afterPlain && man.getAttribute('aria-busy') === 'true', 'afterPlain=' + afterPlain);

                // toggle
                var t = $('toggle'); var tb = classes(t); var tLabel = t.querySelector('[data-btn-label]');
                var offBg = cs(t, 'background-color');
                t.click();
                var onBg = cs(t, 'background-color');
                rec('a toggle flips aria-pressed and the label on click', t.getAttribute('aria-pressed') === 'true' && tLabel.textContent === 'Following', t.getAttribute('aria-pressed') + ' ' + tLabel.textContent);
                rec('a toggle changes no class and the pressed look follows aria-pressed', classes(t) === tb && onBg !== offBg, 'off=' + offBg + ' on=' + onBg + ' classes=' + classes(t));
                t.click();
                var ok21 = t.getAttribute('aria-pressed') === 'false' && tLabel.textContent === 'Follow' && cs(t, 'background-color') === offBg;
                t.click(); t.click();
                rec('a toggle can be clicked repeatedly', ok21 && t.getAttribute('aria-pressed') === 'false' && tLabel.textContent === 'Follow', 'ok21=' + ok21);
                var p = $('toggle-on'); var pLabel = p.querySelector('[data-btn-label]');
                p.click();
                rec('a toggle that starts pressed unpresses on click', p.getAttribute('aria-pressed') === 'false' && pLabel.textContent === 'Pin', p.getAttribute('aria-pressed') + ' ' + pLabel.textContent);
                var e0 = $('toggle-same'); var eLabel = e0.querySelector('[data-btn-label]'); e0.click();
                rec('a toggle whose pressed text is empty keeps its label', e0.getAttribute('aria-pressed') === 'true' && eLabel.textContent === 'Mute', eLabel.textContent);
              } catch (e) {
                rec('SCRIPT ERROR', false, String(e && e.stack || e));
              }
              var pre = document.createElement('pre'); pre.id = 'ash-btn-js'; pre.textContent = out.join('\\n'); document.body.appendChild(pre);
            })();
            """;

    private static String button(String id, String classes, String extra, String text, boolean icon, boolean loading) {
        return "<button type=\"button\" id=\"" + id + "\" class=\"" + classes + "\" data-btn data-btn-variant=\"primary\"" + extra + (loading ? " aria-busy=\"true\" aria-disabled=\"true\"" : "") + ">"
                + "<span data-btn-spinner class=\"ash-btn__spinner\" aria-hidden=\"true\"></span>"
                + (icon ? "<span data-btn-icon class=\"ash-btn__icon\" aria-hidden=\"true\"><svg class=\"ash-icon ash-icon--sm\" viewBox=\"0 0 24 24\"><path d=\"M5 12h14\"/></svg></span>" : "")
                + "<span data-btn-label class=\"ash-btn__label\">" + text + "</span>"
                + "<span class=\"ash-sr-only ash-btn__status\" data-btn-status aria-live=\"polite\">" + (loading ? "Loading" : "") + "</span></button>";
    }

    private static final String P = "ash-btn ash-btn--primary ash-btn--md";
    private static final String S = "ash-btn ash-btn--secondary ash-btn--md";

    private static String markup() {
        return button("plain", P, "", "Save", true, false)
                + button("server-loading", P, "", "Save", true, true)
                + button("manual", P, "", "Save", true, false)
                + "<button type=\"button\" id=\"nodata\" class=\"" + P + "\"><span data-btn-spinner class=\"ash-btn__spinner\"></span></button>"
                + button("guard", P, "", "Guard", false, false)
                + "<form id=\"f0\" action=\"/sink\">" + button("sb0", P, "", "Go", false, false).replace("type=\"button\"", "type=\"submit\"") + "</form>"
                + "<form id=\"f1\" action=\"/sink\"><input name=\"x\" value=\"1\">"
                + button("sb1", P, " data-btn-loading-on-submit", "Send", true, false).replace("type=\"button\"", "type=\"submit\"") + "</form>"
                + "<form id=\"f2\" action=\"/sink\"><input name=\"x\" value=\"1\">"
                + button("sb2", P, " data-btn-loading-on-submit", "Send", false, false).replace("type=\"button\"", "type=\"submit\"") + "</form>"
                + "<form id=\"f3\" action=\"/sink\"><input name=\"x\" required>"
                + button("sb3", P, " data-btn-loading-on-submit", "Send", false, false).replace("type=\"button\"", "type=\"submit\"") + "</form>"
                + button("manual2", P, "", "Wait", false, false)
                + button("toggle", S, " data-btn-toggle aria-pressed=\"false\" data-btn-default-text=\"Follow\" data-btn-pressed-text=\"Following\"", "Follow", false, false)
                        .replace("data-btn-variant=\"primary\"", "data-btn-variant=\"secondary\"")
                + button("toggle-on", S, " data-btn-toggle aria-pressed=\"true\" data-btn-default-text=\"Pin\" data-btn-pressed-text=\"Pinned\"", "Pinned", false, false)
                        .replace("data-btn-variant=\"primary\"", "data-btn-variant=\"secondary\"")
                + button("toggle-same", S, " data-btn-toggle aria-pressed=\"false\" data-btn-default-text=\"Mute\" data-btn-pressed-text=\"Mute\"", "Mute", false, false)
                        .replace("data-btn-variant=\"primary\"", "data-btn-variant=\"secondary\"");
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("button-script");
        String js;
        try (InputStream in = ButtonScriptTest.class.getResourceAsStream("/META-INF/resources/ashlar/js/button.js")) {
            if (in == null) {
                throw new IOException("button.js is not on the test classpath");
            }
            js = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertFalse(js.contains("</script"), "the script text can be embedded in a text/plain script element");
        String page = "<!doctype html><html data-theme=\"light\"><head><link rel=\"stylesheet\" href=\"" + ParityFrames.ASHLAR + "\"></head><body style=\"margin:8px\">"
                + markup() + "<script id=\"src\" type=\"text/plain\">" + js + "</script><script>window.addEventListener('load',function(){" + SCRIPT.replace("</script", "<\\/script")
                + "});</script></body></html>";
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            String dom = session.chrome().dumpDom(server.registerRaw(page));
            for (String line : ParityProbe.lines(dom, "ash-btn-js")) {
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
