package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The REAL showcase application (the packaged boot jar, its own templates, stylesheets and scripts as served) driven with trusted input:
 *
 * <ul>
 *   <li>the site Navbar's mobile menu, end to end, at a phone width: the menu button opens the Navbar's Drawer, focus moves into it, the page is locked, the Drawer holds the
 *       same destinations as the bar, Tab stays inside it, Escape closes it, focus returns to the menu button and the page's own scrolling is back; the backdrop closes it too;</li>
 *   <li>the Modal and Drawer showcase pages: a real dialog and a real panel open from their buttons, close on Escape and give focus back;</li>
 *   <li>where every overlay on those pages sits: an overlay is only ordered against the others when it shares their stacking context, so every ancestor that makes a stacking
 *       context is listed, and there must be none. The showcase layout lifts the Navbar's HEADER (relative, z-index 20) rather than a wrapper around the whole Navbar, so the
 *       site Navbar's Drawer is in the page's overlay band like every other overlay;</li>
 *   <li>what that lift is for, still true: with the skin Select open in the bar on a component page, every one of its options that lies over the sticky "On this page"
 *       outline bar is drawn above it. A control takes the lift away in the page and shows the outline bar then covers an option, so the check can fail.</li>
 * </ul>
 */
class OverlayRealPagesTest {

    private static final List<String> FAILURES = new ArrayList<>();
    private static final Map<String, String> CONTEXTS = new LinkedHashMap<>();
    private static String listboxOverOutline;
    private static String listboxWithoutTheLift;
    private static int navbarLinks;

    private static String ev(DevToolsSession cdp, String js) throws Exception {
        return cdp.evaluate("(function(){try{return String(" + js + ")}catch(e){return 'ERR '+e}})()");
    }

    private static void expect(String where, String what, String expected, String actual) {
        if (!expected.equals(actual)) {
            FAILURES.add(where + " | " + what + ": expected " + expected + ", was " + actual);
        }
    }

    private static final String SHOWN = "(function(id){var r=document.getElementById(id);return getComputedStyle(r).display==='none'?'hidden':'shown'})";
    private static final String ACTIVE = "(function(){var a=document.activeElement;if(!a||a===document.body)return 'body';var o=a.closest('[data-modal],[data-drawer]');"
            + "return (o?o.id+':':'')+(a.getAttribute('aria-label')||a.id||a.tagName.toLowerCase())})()";

    /** Every overlay root of the page, and the ancestors of each that make a stacking context. */
    private static final String STACKING = "(function(){function ctx(e){var s=getComputedStyle(e);return (s.position!=='static'&&s.zIndex!=='auto')||parseFloat(s.opacity)<1||s.transform!=='none'"
            + "||s.filter!=='none'||s.isolation==='isolate'||s.mixBlendMode!=='normal'||/paint|layout|strict|content/.test(s.contain)||/transform|opacity|filter/.test(s.willChange)"
            + "||s.backdropFilter&&s.backdropFilter!=='none'}"
            + "return [].map.call(document.querySelectorAll('[data-modal],[data-drawer]'),function(r){var found=[];for(var e=r.parentElement;e&&e!==document.documentElement;e=e.parentElement){"
            + "if(ctx(e)){var s=getComputedStyle(e);found.push(e.tagName.toLowerCase()+(e.className?'.'+String(e.className).trim().split(/\\s+/).join('.'):'')+'[z='+s.zIndex+']')}}"
            + "var navbar=r.previousElementSibling&&r.previousElementSibling.matches('[data-navbar]');return r.id+(navbar?' (navbar drawer)':'')+' <- '+(found.join(' < ')||'none')}).join('\\n')})()";

    @BeforeAll
    static void run() throws Exception {
        String bootJar = System.getProperty("ashlar.showcase.jar");
        if (bootJar == null) {
            throw new IllegalStateException("ashlar.showcase.jar must be set; run through Gradle (:showcase:test)");
        }
        ParitySession session = ParitySession.open("overlay-real-pages");
        try (RunningShowcase app = RunningShowcase.start(Path.of(bootJar))) {
            String base = "http://127.0.0.1:" + app.port();
            try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                navbar(cdp, base);
                modalPage(cdp, base);
                drawerPage(cdp, base);
                OverlayBrowser.viewport(cdp, 1280, 900);
                skinListbox(cdp, base);
                for (String page : List.of("/ashlar/", "/ashlar/components/modal", "/ashlar/components/drawer", "/ashlar/components/navbar", "/ashlar/components/faceted-search",
                        "/ashlar/components/sidebar")) {
                    cdp.navigate(base + page);
                    OverlayBrowser.ready(cdp);
                    CONTEXTS.put(page, ev(cdp, STACKING));
                }
            }
        }
    }

    private static void navbar(DevToolsSession cdp, String base) throws Exception {
        String where = "navbar (phone width)";
        OverlayBrowser.viewport(cdp, 600, 800);
        cdp.navigate(base + "/ashlar/");
        OverlayBrowser.ready(cdp);
        String drawer = ev(cdp, "document.querySelector('[data-navbar-toggle]').getAttribute('data-drawer-open')");
        String overflowBefore = ev(cdp, "document.body.style.cssText");
        OverlayBrowser.click(cdp, "[data-navbar-toggle]");
        Thread.sleep(400);
        expect(where, "the menu button opens the Navbar's Drawer", "shown", ev(cdp, SHOWN + "('" + drawer + "')"));
        expect(where, "the Drawer is open", "open", ev(cdp, "document.getElementById('" + drawer + "').dataset.state"));
        expect(where, "focus moves to the Drawer's close button", drawer + ":Close", ev(cdp, ACTIVE));
        expect(where, "the page is locked", "hidden", ev(cdp, "document.body.style.overflowY"));
        navbarLinks = Integer.parseInt(ev(cdp, "document.getElementById('" + drawer + "').querySelectorAll('.ash-navbar__menu-list [data-nav-link]').length"));
        expect(where, "the Drawer holds the same destinations as the bar", ev(cdp, "document.querySelectorAll('.ash-navbar__list [data-nav-link]').length"), String.valueOf(navbarLinks));
        expect(where, "the panel paints over the page", drawer, ev(cdp, "(function(){var p=document.getElementById('" + drawer + "').querySelector('[data-drawer-panel]').getBoundingClientRect();"
                + "var e=document.elementFromPoint(p.left+p.width/2,p.top+p.height/2);var o=e&&e.closest('[data-drawer]');return o?o.id:'page'})()"));
        for (int i = 0; i < navbarLinks + 2; i++) {
            OverlayBrowser.press(cdp, "Tab");
        }
        expect(where, "Tab stays inside the Drawer", "true", ev(cdp, "document.getElementById('" + drawer + "').contains(document.activeElement)"));
        OverlayBrowser.press(cdp, "Escape");
        Thread.sleep(400);
        expect(where, "Escape closes it", "hidden", ev(cdp, SHOWN + "('" + drawer + "')"));
        expect(where, "focus returns to the menu button", "true", ev(cdp, "document.activeElement === document.querySelector('[data-navbar-toggle]')"));
        expect(where, "the page's own style is back", overflowBefore, ev(cdp, "document.body.style.cssText"));
        OverlayBrowser.click(cdp, "[data-navbar-toggle]");
        Thread.sleep(400);
        OverlayBrowser.clickBackdrop(cdp, drawer);
        Thread.sleep(400);
        expect(where, "the backdrop closes it", "hidden", ev(cdp, SHOWN + "('" + drawer + "')"));
        expect(where, "and the page is unlocked", overflowBefore, ev(cdp, "document.body.style.cssText"));
    }

    /** For each skin Select option that lies over the outline bar: is it the topmost element at its centre? Returns "over=N covered=M". */
    private static final String LISTBOX_OVER_OUTLINE = "(function(){var bar=document.querySelector('[data-section-nav]').getBoundingClientRect();"
            + "var opts=[].slice.call(document.querySelectorAll('[data-skin-select] [data-select-option]')).filter(function(o){return o.getClientRects().length>0});"
            + "var over=0,covered=0;opts.forEach(function(o){var r=o.getBoundingClientRect();var y=r.top+r.height/2;if(y<bar.top||y>bar.bottom)return;over++;"
            + "var e=document.elementFromPoint(r.left+r.width/2,y);if(!e||(e!==o&&!o.contains(e)))covered++});return 'over='+over+' covered='+covered})()";

    private static void skinListbox(DevToolsSession cdp, String base) throws Exception {
        cdp.navigate(base + "/ashlar/components/table");
        OverlayBrowser.ready(cdp);
        OverlayBrowser.click(cdp, "[data-skin-select] [data-select-trigger]");
        Thread.sleep(300);
        listboxOverOutline = ev(cdp, LISTBOX_OVER_OUTLINE);
        ev(cdp, "document.querySelector('[data-navbar]').style.zIndex='auto'");
        listboxWithoutTheLift = ev(cdp, LISTBOX_OVER_OUTLINE);
        ev(cdp, "document.querySelector('[data-navbar]').style.removeProperty('z-index')");
        OverlayBrowser.press(cdp, "Escape");
    }

    private static void opensAndCloses(DevToolsSession cdp, String where, String trigger, String overlay) throws Exception {
        OverlayBrowser.click(cdp, trigger);
        Thread.sleep(400);
        expect(where, "the trigger opens it", "shown", ev(cdp, SHOWN + "('" + overlay + "')"));
        expect(where, "focus moves into it", "true", ev(cdp, "document.getElementById('" + overlay + "').contains(document.activeElement)"));
        expect(where, "the page is locked", "hidden", ev(cdp, "document.body.style.overflowY"));
        OverlayBrowser.press(cdp, "Escape");
        Thread.sleep(400);
        expect(where, "Escape closes it", "hidden", ev(cdp, SHOWN + "('" + overlay + "')"));
        expect(where, "focus returns to the trigger", "true", ev(cdp, "document.activeElement === document.querySelector(" + OverlayBrowser.js(trigger) + ")"));
        expect(where, "the page is unlocked", "", ev(cdp, "document.body.style.overflowY"));
    }

    private static void modalPage(DevToolsSession cdp, String base) throws Exception {
        OverlayBrowser.viewport(cdp, 1280, 900);
        cdp.navigate(base + "/ashlar/components/modal");
        OverlayBrowser.ready(cdp);
        ev(cdp, "document.querySelector('[data-modal-open=\"basicModal\"]').scrollIntoView({block:'center'})");
        opensAndCloses(cdp, "modal page (Basic Modal)", "[data-modal-open=\"basicModal\"]", "basicModal");
        ev(cdp, "document.querySelector('[data-modal-open=\"noEscapeModal\"]').scrollIntoView({block:'center'})");
        OverlayBrowser.click(cdp, "[data-modal-open=\"noEscapeModal\"]");
        Thread.sleep(400);
        OverlayBrowser.press(cdp, "Escape");
        Thread.sleep(400);
        expect("modal page (Escape Disabled)", "Escape does not close it", "shown", ev(cdp, SHOWN + "('noEscapeModal')"));
        OverlayBrowser.click(cdp, "#noEscapeModal [aria-label=\"Close\"]");
        Thread.sleep(400);
        expect("modal page (Escape Disabled)", "its close button does", "hidden", ev(cdp, SHOWN + "('noEscapeModal')"));
    }

    private static void drawerPage(DevToolsSession cdp, String base) throws Exception {
        cdp.navigate(base + "/ashlar/components/drawer");
        OverlayBrowser.ready(cdp);
        String trigger = ev(cdp, "(function(){var t=[].filter.call(document.querySelectorAll('main [data-drawer-open]'),function(e){return e.getClientRects().length>0})[0];"
                + "t.scrollIntoView({block:'center'});return t.getAttribute('data-drawer-open')})()");
        opensAndCloses(cdp, "drawer page (" + trigger + ")", "main [data-drawer-open=\"" + trigger + "\"]", trigger);
    }

    @Test
    void theNavbarsMobileMenuAndTheShowcaseOverlaysWorkEndToEnd() {
        assertEquals(List.of(), FAILURES);
        assertTrue(navbarLinks >= 2, "the Navbar's Drawer holds its destinations: " + navbarLinks);
    }

    @Test
    void noOverlayOnTheShowcasePagesSitsInsideAnotherStackingContext() {
        List<String> confined = new ArrayList<>();
        CONTEXTS.forEach((page, lines) -> {
            for (String line : lines.split("\n")) {
                if (!line.isBlank() && !line.endsWith(" <- none")) {
                    confined.add(page + ": " + line);
                }
            }
        });
        assertEquals(List.of(), confined, CONTEXTS.toString());
        assertTrue(CONTEXTS.values().stream().allMatch(v -> v.contains("(navbar drawer) <- none")), "every page has the site Navbar's Drawer, outside any stacking context: " + CONTEXTS);
    }

    @Test
    void theSkinSelectsListboxStillPaintsOverTheStickyOutlineBar() {
        assertTrue(listboxOverOutline.matches("over=[1-9][0-9]* covered=0"), "every option over the outline bar is drawn above it: " + listboxOverOutline);
        assertTrue(listboxWithoutTheLift.matches("over=[1-9][0-9]* covered=[1-9][0-9]*"), "control: without the header's lift the outline bar covers an option: " + listboxWithoutTheLift);
    }
}
