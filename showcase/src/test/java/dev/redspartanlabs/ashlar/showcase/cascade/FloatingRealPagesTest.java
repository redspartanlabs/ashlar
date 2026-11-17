package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The REAL showcase application (the packaged boot jar, its own templates, stylesheets and scripts as served) driven with trusted input on the Tooltip and Popover pages:
 *
 * <ul>
 *   <li>the scripts the pages load are the library's packaged tooltip.js and popover.js, byte for byte;</li>
 *   <li>every Tooltip and Popover on the two pages is rendered closed (hidden, data-state="closed");</li>
 *   <li>a Tooltip on the page shows on hover after its delay, describes its trigger, and Escape hides it;</li>
 *   <li>a Popover on the page opens on a click (aria-expanded and aria-controls on its trigger), a click outside closes it, and Escape from inside it closes it and gives focus
 *       back to its trigger.</li>
 * </ul>
 */
class FloatingRealPagesTest {

    private static final List<String> FAILURES = new ArrayList<>();
    private static final Map<String, String> SERVED = new LinkedHashMap<>();
    private static final Map<String, String> CLOSED = new LinkedHashMap<>();

    private static String ev(DevToolsSession cdp, String js) throws Exception {
        return cdp.evaluate("(function(){try{return String(" + js + ")}catch(e){return 'ERR '+e}})()");
    }

    private static void expect(String where, String what, String expected, String actual) {
        if (!expected.equals(actual)) {
            FAILURES.add(where + " | " + what + ": expected " + expected + ", was " + actual);
        }
    }

    /** Every surface of the page and whether it is rendered closed. */
    private static final String ALL_CLOSED = "(function(){var s=[].slice.call(document.querySelectorAll('[data-tooltip-bubble],[data-popover-surface]'));"
            + "var bad=s.filter(function(e){return !e.hidden||e.dataset.state!=='closed'||getComputedStyle(e).display!=='none'});return s.length+' surfaces, '+bad.length+' not closed'})()";

    @BeforeAll
    static void run() throws Exception {
        String bootJar = System.getProperty("ashlar.showcase.jar");
        if (bootJar == null) {
            throw new IllegalStateException("ashlar.showcase.jar must be set; run through Gradle (:showcase:test)");
        }
        ParitySession session = ParitySession.open("floating-real-pages");
        try (RunningShowcase app = RunningShowcase.start(Path.of(bootJar))) {
            String base = "http://127.0.0.1:" + app.port();
            try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                OverlayBrowser.viewport(cdp, 1280, 900);
                tooltipPage(cdp, base);
                popoverPage(cdp, base);
            }
        }
    }

    /** Fetches the script the page itself loads (its own script tag's URL) and records whether it is the library's packaged file, byte for byte. */
    private static void served(DevToolsSession cdp, String name) throws Exception {
        String src = ev(cdp, "document.querySelector('script[type=\"module\"][src$=\"/" + name + "\"]').src");
        HttpResponse<String> r = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(src)).build(), HttpResponse.BodyHandlers.ofString());
        String body = r.body().replace("\r\n", "\n");
        String packaged = OverlayBrowser.script("/META-INF/resources/ashlar/js/" + name);
        SERVED.put(name, body.equals(packaged) ? "true" : "differs: " + src + " status " + r.statusCode() + ", " + body.length() + " vs " + packaged.length() + " characters");
    }

    private static void tooltipPage(DevToolsSession cdp, String base) throws Exception {
        String where = "tooltip page";
        cdp.navigate(base + "/ashlar/components/tooltip");
        OverlayBrowser.ready(cdp);
        served(cdp, "tooltip.js");
        CLOSED.put(where, ev(cdp, ALL_CLOSED));
        String trigger = "main [data-tooltip] [data-tooltip-trigger] button";
        ev(cdp, "document.querySelector('" + trigger + "').scrollIntoView({block:'center'})");
        Thread.sleep(100);
        FloatingBrowser.hover(cdp, trigger);
        Thread.sleep(60);
        String bubble = "document.querySelector('" + trigger + "').closest('[data-tooltip]').querySelector('[data-tooltip-bubble]')";
        expect(where, "not shown before its delay", "true", ev(cdp, bubble + ".hidden"));
        Thread.sleep(300);
        expect(where, "shown after its delay", "false open", ev(cdp, bubble + ".hidden+' '+" + bubble + ".dataset.state"));
        expect(where, "it describes its trigger", "true", ev(cdp, "document.querySelector('" + trigger + "').getAttribute('aria-describedby').split(' ').indexOf(" + bubble + ".id)>=0"));
        OverlayBrowser.press(cdp, "Escape");
        Thread.sleep(400);
        expect(where, "Escape hides it", "true closed", ev(cdp, bubble + ".hidden+' '+" + bubble + ".dataset.state"));
    }

    private static void popoverPage(DevToolsSession cdp, String base) throws Exception {
        String where = "popover page";
        cdp.navigate(base + "/ashlar/components/popover");
        OverlayBrowser.ready(cdp);
        served(cdp, "popover.js");
        CLOSED.put(where, ev(cdp, ALL_CLOSED));
        // The interactive-content example: a Popover whose surface holds controls.
        String trigger = ev(cdp, "(function(){var r=[].filter.call(document.querySelectorAll('main [data-popover]'),function(r){return r.querySelector('[data-popover-surface] button')})[0];"
                + "var t=r.querySelector('[data-popover-trigger] [aria-controls]');t.id=t.id||'real-pop';t.scrollIntoView({block:'center'});return t.id})()");
        Thread.sleep(100);
        String surface = "document.getElementById('" + trigger + "').closest('[data-popover]').querySelector('[data-popover-surface]')";
        OverlayBrowser.click(cdp, "#" + trigger);
        Thread.sleep(300);
        expect(where, "a click opens it", "true false open", ev(cdp, "document.getElementById('" + trigger + "').getAttribute('aria-expanded')+' '+" + surface + ".hidden+' '+"
                + surface + ".dataset.state"));
        expect(where, "its trigger controls the surface", "true", ev(cdp, "document.getElementById('" + trigger + "').getAttribute('aria-controls')===" + surface + ".id"));
        OverlayBrowser.click(cdp, "main h1");
        Thread.sleep(400);
        expect(where, "a click outside closes it", "false true", ev(cdp, "document.getElementById('" + trigger + "').getAttribute('aria-expanded')+' '+" + surface + ".hidden"));
        OverlayBrowser.click(cdp, "#" + trigger);
        Thread.sleep(300);
        OverlayBrowser.press(cdp, "Tab");
        expect(where, "Tab moves into the surface", "true", ev(cdp, surface + ".contains(document.activeElement)"));
        OverlayBrowser.press(cdp, "Escape");
        Thread.sleep(400);
        expect(where, "Escape closes it", "true", ev(cdp, surface + ".hidden"));
        expect(where, "and gives focus back to its trigger", "true", ev(cdp, "document.activeElement===document.getElementById('" + trigger + "')"));
    }

    @Test
    void thePagesLoadTheLibrarysPackagedScripts() {
        assertEquals(Map.of("tooltip.js", "true", "popover.js", "true"), SERVED);
    }

    @Test
    void everyTooltipAndPopoverOnThePagesIsRenderedClosed() {
        CLOSED.forEach((page, value) -> assertTrue(value.matches("[1-9][0-9]* surfaces, 0 not closed"), page + ": " + value));
    }

    @Test
    void theRealTooltipAndPopoverWorkEndToEnd() {
        assertEquals(List.of(), FAILURES);
    }
}
