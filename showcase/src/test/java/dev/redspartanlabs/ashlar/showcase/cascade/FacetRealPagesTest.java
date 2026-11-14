package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Facets family on the REAL Showcase: the packaged application, started as a separate process, and its real Faceted Search page. Two things are checked there because they cannot be
 * checked on a fixture.
 *
 * <p>First, the markup the real template emits: every element that carries a Facets class carries nothing but Ashlar classes, every Facets class the real page uses is one the legacy
 * rewriter knows (it fails loudly on any other, so the style comparison in {@link ShowcaseRuntimeTest} reaches all of them), rewriting the page back to the legacy classes leaves no
 * Facets class behind, and the structure holds on the page itself: each facet's name is the name of every checkbox inside it, and every count is announced.
 *
 * <p>Second, the page as the application serves it, driven by TRUSTED input in a real browser: in the application-owned form, clicking two rows and submitting selects what the browser
 * would send ({@code FormData}), each facet under its own name; Clear (the form's reset) undoes it; the keyboard moves through the options and Space toggles them; a disabled option is
 * skipped; and nothing writes a class or a style inside a facet while any of this happens (there is no script).
 */
class FacetRealPagesTest {

    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("<([a-z0-9]+)\\b[^>]*?\\bclass=\"([^\"]*)\"[^>]*>");
    private static String html;
    private static final Map<String, String> DRIVEN = new LinkedHashMap<>();

    private static String fetch(HttpClient client, int port, String path) throws Exception {
        HttpResponse<byte[]> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, response.statusCode(), path);
        return new String(response.body(), StandardCharsets.UTF_8);
    }

    private static String ev(DevToolsSession cdp, String js) throws Exception {
        return cdp.evaluate("(function(){try{return String(" + js + ")}catch(e){return 'ERR '+e}})()");
    }

    private static final String FORM = "[data-fp=\"owned\"]";

    private static String row(String value) {
        return FORM + " label:has(> input[value=\"" + value + "\"])";
    }

    @BeforeAll
    static void run() throws Exception {
        String bootJar = System.getProperty("ashlar.showcase.jar");
        if (bootJar == null) {
            throw new IllegalStateException("ashlar.showcase.jar must be set; run through Gradle (:showcase:test)");
        }
        ParitySession session = ParitySession.open("facet-real-pages");
        HttpClient client = HttpClient.newHttpClient();
        try (RunningShowcase app = RunningShowcase.start(Path.of(bootJar)); RealPageProxy proxy = new RealPageProxy(app.port())) {
            html = fetch(client, app.port(), "/ashlar/components/faceted-search");
            proxy.configure(new RealPageProxy.Load("light", "snapshot", null));
            try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                cdp.navigate(proxy.url("/ashlar/components/faceted-search"));
                Thread.sleep(300);
                drive(cdp);
            }
        }
    }

    private static void drive(DevToolsSession cdp) throws Exception {
        // The first form on the page that holds a Faceted Search is the application-owned form (Variants & States); it has two facets and a Clear and an Apply button.
        DRIVEN.put("forms with facets", ev(cdp, "document.querySelectorAll('form:has([data-faceted-search])').length"));
        DRIVEN.put("marked", ev(cdp, "(function(){var f=document.querySelector('form:has([data-faceted-search])');f.setAttribute('data-fp','owned');"
                + "window.__writes=0;[].forEach.call(document.querySelectorAll('[data-faceted-search]'),function(t){new MutationObserver(function(r){window.__writes+=r.length}).observe(t,{subtree:true,attributes:true,attributeFilter:['class','style']})});"
                + "window.__fd=function(){return new URLSearchParams(new FormData(f)).toString()};"
                + "window.__shown=function(e){return !!e&&e.getClientRects().length>0};return 'ok'})()"));
        DRIVEN.put("start", ev(cdp, "__fd()"));
        TextEntryBrowser.click(cdp, row("running"));
        DRIVEN.put("after Running", ev(cdp, "__fd()"));
        TextEntryBrowser.click(cdp, row("acb-02"));
        DRIVEN.put("after ACB-02", ev(cdp, "__fd()"));
        TextEntryBrowser.click(cdp, row("acb-01"));
        DRIVEN.put("after ACB-01", ev(cdp, "__fd()"));
        TextEntryBrowser.click(cdp, row("running") + " > span[aria-hidden]");
        DRIVEN.put("after the count of Running", ev(cdp, "__fd()"));
        TextEntryBrowser.click(cdp, FORM + " button[type=reset]");
        DRIVEN.put("after Clear", ev(cdp, "__fd()"));
        // The keyboard: focus the first option by script, then Tab and Space.
        ev(cdp, "(function(){document.querySelector('" + FORM + " input').focus();return 1})()");
        TextEntryBrowser.perform(cdp, "key:Space");
        DRIVEN.put("keyboard first option", ev(cdp, "__fd()"));
        TextEntryBrowser.perform(cdp, "key:Tab");
        DRIVEN.put("keyboard focus moved", ev(cdp, "document.activeElement.value"));
        DRIVEN.put("keyboard ring drawn", ev(cdp, "getComputedStyle(document.activeElement).boxShadow.indexOf('0px 0px 0px 4px')>=0"));
        DRIVEN.put("styled checkbox", ev(cdp, "(function(){var s=getComputedStyle(document.querySelector('" + FORM + " input'));return s.width+' '+s.height+' '+(s.accentColor.length>0)})()"));
        DRIVEN.put("row hover cursor", ev(cdp, "getComputedStyle(document.querySelector('" + FORM + " label')).cursor"));
        DRIVEN.put("legend weight", ev(cdp, "getComputedStyle(document.querySelector('" + FORM + " legend')).fontWeight"));
        DRIVEN.put("writes", ev(cdp, "window.__writes"));
    }

    // ---------------------------------------------------------------- the markup the real template emits

    private static List<String[]> facetsElements(String page) {
        List<String[]> found = new ArrayList<>();
        Matcher m = CLASS_ATTRIBUTE.matcher(page);
        while (m.find()) {
            if (m.group(2).contains("ash-facet")) {
                found.add(new String[] {m.group(1), m.group(2)});
            }
        }
        return found;
    }

    @Test
    void everyElementWithAFacetsClassCarriesOnlyAshlarClassesOnTheRealPage() {
        TreeSet<String> foreign = new TreeSet<>();
        for (String[] el : facetsElements(html)) {
            for (String token : el[1].trim().split("\\s+")) {
                if (!token.startsWith("ash-")) {
                    foreign.add(token + " on <" + el[0] + ">");
                }
            }
        }
        assertEquals(new TreeSet<String>(), foreign, "a Facets element carries a class that is not Ashlar's");
        assertTrue(facetsElements(html).size() > 100, "the page renders many Facets elements: " + facetsElements(html).size());
    }

    @Test
    void theRealPageUsesEveryFacetsPartAndRewritingItBackLeavesNoFacetsClass() throws Exception {
        String rewritten = new FacetLegacyMarkup().rewrite(html);
        Matcher left = Pattern.compile("class=\"[^\"]*ash-facet[^\"]*\"").matcher(rewritten);
        assertFalse(left.find(), "a Facets class survived the rewrite: " + (left.find(0) ? left.group() : ""));
        String all = String.join(" ", facetsElements(html).stream().map(e -> e[1]).toList());
        for (String part : List.of("ash-faceted-search", "ash-facet", "ash-facet__legend", "ash-facet__options", "ash-facet-option", "ash-facet-option--disabled", "ash-facet-option__input",
                "ash-facet-option__label", "ash-facet-option__count")) {
            assertTrue(Pattern.compile("(?<![\\w-])" + part + "(?![\\w-])").matcher(all).find(), "the real page uses " + part);
        }
        FacetLegacyMarkup legacy = new FacetLegacyMarkup();
        for (String role : legacy.all().keySet()) {
            assertTrue(rewritten.contains("class=\"" + legacy.classes(role) + "\""), "the rewritten page uses the legacy " + role);
        }
    }

    @Test
    void onTheRealPageEachFacetsNameIsTheNameOfEveryCheckboxInsideItAndEveryCountIsAnnounced() {
        Matcher fieldsets = Pattern.compile("<fieldset[^>]*data-facet-name=\"([^\"]*)\"[^>]*>(.*?)</fieldset>", Pattern.DOTALL).matcher(html);
        int facets = 0;
        int inputs = 0;
        while (fieldsets.find()) {
            facets++;
            Matcher input = Pattern.compile("<input\\b[^>]*\\sname=\"([^\"]*)\"").matcher(fieldsets.group(2));
            while (input.find()) {
                inputs++;
                assertEquals(fieldsets.group(1), input.group(1), "every checkbox submits under its facet's name");
            }
        }
        // The page's examples: Quick Start 1 facet, single facet 1, counts 1, several 3, owned form 2, beside results 2, drawer 2 = 12 facets; 2+3+4+10+4+5+6 = 34 checkboxes. A change to the
        // page's examples means updating this deliberately.
        assertEquals(12, facets);
        assertEquals(34, inputs);
        int visible = count(html, "aria-hidden=\"true\" class=\"ash-facet-option__count\"");
        int announced = Pattern.compile("<span class=\"ash-sr-only\">\\d+ results?</span>").matcher(html).results().toList().size();
        assertEquals(visible, announced, "each visible count has its announcement");
        assertEquals(29, visible, "counts: the options of the examples that show one");
        assertFalse(Pattern.compile("<input\\b[^>]*\\sname=\"\"").matcher(html).find(), "no checkbox on the page is without a name (the Pitfalls text quotes name=\"\" in a code element, which is not a checkbox)");
    }

    private static int count(String text, String part) {
        return text.split(Pattern.quote(part), -1).length - 1;
    }

    // ---------------------------------------------------------------- the page, driven

    @Test
    void trustedClicksOnTheRealPageSelectWhatTheFormWouldSendEachFacetUnderItsOwnName() {
        assertTrue(Integer.parseInt(DRIVEN.get("forms with facets")) >= 3, DRIVEN.toString());
        assertEquals("", DRIVEN.get("start"), DRIVEN.toString());
        assertEquals("status=running", DRIVEN.get("after Running"));
        assertEquals("status=running&machine=acb-02", DRIVEN.get("after ACB-02"));
        assertEquals("status=running&machine=acb-01&machine=acb-02", DRIVEN.get("after ACB-01"), "two checked values of one facet are two values of one field");
        assertEquals("machine=acb-01&machine=acb-02", DRIVEN.get("after the count of Running"), "a click on the visible count toggles its option");
        assertEquals("", DRIVEN.get("after Clear"), "the form's reset undoes it");
    }

    @Test
    void theKeyboardWorksOnTheRealPageAndTheFocusRingIsDrawn() {
        assertEquals("status=running", DRIVEN.get("keyboard first option"));
        assertEquals("ready", DRIVEN.get("keyboard focus moved"));
        assertEquals("true", DRIVEN.get("keyboard ring drawn"));
    }

    @Test
    void theRealStylesheetStylesTheRealPageAndNoScriptWritesAClassOrAStyleInsideAFacet() {
        assertTrue(DRIVEN.get("styled checkbox").startsWith("16px 16px"), DRIVEN.toString());
        assertEquals("pointer", DRIVEN.get("row hover cursor"));
        assertEquals("600", DRIVEN.get("legend weight"));
        assertEquals("0", DRIVEN.get("writes"), "nothing wrote a class or a style inside a Faceted Search");
    }
}
