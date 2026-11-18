package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The REAL showcase application (the packaged boot jar, its own templates, stylesheets and scripts as served) driven with trusted input on the Dropdown Menu page:
 *
 * <ul>
 *   <li>the script the page loads is the library's packaged dropdown-menu.js, byte for byte;</li>
 *   <li>every Dropdown Menu on the page is rendered closed (hidden, data-state="closed", the chevron closed, aria-expanded="false");</li>
 *   <li>a menu on the page opens on a click (aria-expanded, the menu shown and open, the chevron turned), Escape closes it with focus on the trigger, a click outside
 *       closes it, Enter opens it on its first entry, and Tab from its trigger closes it.</li>
 * </ul>
 */
class DropdownMenuRealPagesTest {

    private static final List<String> FAILURES = new ArrayList<>();
    private static String served;
    private static String closed;

    private static String ev(DevToolsSession cdp, String js) throws Exception {
        return cdp.evaluate("(function(){try{return String(" + js + ")}catch(e){return 'ERR '+e}})()");
    }

    private static void expect(String what, String expected, String actual) {
        if (!expected.equals(actual)) {
            FAILURES.add(what + ": expected " + expected + ", was " + actual);
        }
    }

    private static final String ALL_CLOSED = "(function(){var r=[].slice.call(document.querySelectorAll('[data-dropdown-menu]'));var bad=r.filter(function(x){"
            + "var l=x.querySelector('[data-dropdown-menu-list]'),c=x.querySelector('[data-dropdown-menu-chevron]'),t=x.querySelector('[data-dropdown-menu-trigger]');"
            + "return !l.hidden||l.dataset.state!=='closed'||getComputedStyle(l).display!=='none'||c.dataset.state!=='closed'||t.getAttribute('aria-expanded')!=='false'});"
            + "return r.length+' menus, '+bad.length+' not closed'})()";

    private static final String MENU = "document.getElementById('real-menu')";
    private static final String LIST = MENU + ".querySelector('[data-dropdown-menu-list]')";
    private static final String TRIGGER = MENU + ".querySelector('[data-dropdown-menu-trigger]')";
    private static final String STATE = "(function(){var l=" + LIST + ",t=" + TRIGGER + ";return t.getAttribute('aria-expanded')+' '+(getComputedStyle(l).display!=='none')+' '+l.dataset.state"
            + "+' '+" + MENU + ".querySelector('[data-dropdown-menu-chevron]').dataset.state})()";

    @BeforeAll
    static void run() throws Exception {
        String bootJar = System.getProperty("ashlar.showcase.jar");
        if (bootJar == null) {
            throw new IllegalStateException("ashlar.showcase.jar must be set; run through Gradle (:showcase:test)");
        }
        ParitySession session = ParitySession.open("dropdown-menu-real-pages");
        try (RunningShowcase app = RunningShowcase.start(Path.of(bootJar))) {
            String base = "http://127.0.0.1:" + app.port();
            try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                OverlayBrowser.viewport(cdp, 1280, 900);
                cdp.navigate(base + "/ashlar/components/dropdown-menu");
                OverlayBrowser.ready(cdp);
                OverlayBrowser.clickAt(cdp, 1275, 895);
                String src = ev(cdp, "document.querySelector('script[type=\"module\"][src$=\"/dropdown-menu.js\"]').src");
                HttpResponse<String> r = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(src)).build(), HttpResponse.BodyHandlers.ofString());
                String body = r.body().replace("\r\n", "\n");
                String packaged = OverlayBrowser.script("/META-INF/resources/ashlar/js/dropdown-menu.js");
                served = body.equals(packaged) ? "true" : "differs: " + src + " status " + r.statusCode() + ", " + body.length() + " vs " + packaged.length() + " characters";
                closed = ev(cdp, ALL_CLOSED);
                // The quick-start example: the first menu in the page's main content.
                ev(cdp, "(function(){var m=document.querySelector('main [data-dropdown-menu]');m.id='real-menu';m.scrollIntoView({block:'center'});return 'ok'})()");
                Thread.sleep(100);
                String trigger = "#real-menu [data-dropdown-menu-trigger]";
                OverlayBrowser.click(cdp, trigger);
                Thread.sleep(300);
                expect("a click opens it (aria-expanded, shown, data-state, chevron)", "true true open open", ev(cdp, STATE));
                expect("its trigger controls the menu", "true", ev(cdp, TRIGGER + ".getAttribute('aria-controls')===" + LIST + ".id"));
                OverlayBrowser.press(cdp, "Escape");
                Thread.sleep(400);
                expect("Escape closes it", "false false closed closed", ev(cdp, STATE));
                expect("and focus is on its trigger", "true", ev(cdp, "document.activeElement===" + TRIGGER));
                OverlayBrowser.click(cdp, trigger);
                Thread.sleep(300);
                OverlayBrowser.click(cdp, "main h1");
                Thread.sleep(400);
                expect("a click outside closes it", "false false closed closed", ev(cdp, STATE));
                ev(cdp, TRIGGER + ".focus()");
                OverlayBrowser.press(cdp, "Enter");
                Thread.sleep(300);
                expect("Enter opens it", "true true open open", ev(cdp, STATE));
                expect("on its first entry", "true", ev(cdp, "document.activeElement===" + MENU + ".querySelector('[data-dropdown-menu-item]')"));
                OverlayBrowser.press(cdp, "Escape");
                Thread.sleep(400);
                OverlayBrowser.click(cdp, trigger);
                Thread.sleep(300);
                OverlayBrowser.press(cdp, "Tab");
                Thread.sleep(400);
                expect("Tab from the trigger closes it", "false false closed closed", ev(cdp, STATE));
                expect("and focus has moved on", "false", ev(cdp, MENU + ".contains(document.activeElement)"));
            }
        }
    }

    @Test
    void thePageLoadsTheLibrarysPackagedScript() {
        assertEquals("true", served);
    }

    @Test
    void everyDropdownMenuOnThePageIsRenderedClosed() {
        assertTrue(closed.matches("[1-9][0-9]* menus, 0 not closed"), closed);
    }

    @Test
    void theRealDropdownMenuWorksEndToEnd() {
        assertEquals(List.of(), FAILURES);
    }
}
