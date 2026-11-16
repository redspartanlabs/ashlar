package dev.redspartanlabs.ashlar.showcase.cascade;

import dev.redspartanlabs.ashlar.select.SelectOption;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Modal and Drawer behaviour scenarios, written once and run by four suites: {@link OverlayLegacyCharacterizationTest} (the legacy scripts, recording what they did,
 * their stacking defects included), {@link OverlayBehaviourParityTest} (single-overlay scenarios: the migrated scripts must do exactly what the legacy ones did),
 * {@link OverlayStackTest} (the corrected stacking contract, on the real migrated scripts) and {@link OverlayMutationTest} (the same contract against deliberately broken
 * variants, each of which must fail it).
 *
 * <p>A scenario is a list of actions ({@link OverlayBrowser#perform}) on the scenario page ({@link OverlayBrowser#scenarioBody}); after every action the snapshot records
 * which overlays are shown, the page's inline overflow-y, the focused element, and which overlay paints at the centre of the viewport. An expectation names a step, a
 * snapshot field and the value it must have. The expectations were written from the components' documented behaviour and the approved stacking contract, not from either
 * implementation's output.
 *
 * <p>The probes ({@link #probes}) need pages of their own (transitions under the test's control, the body's own overflow, twelve modals, the floating components inside an
 * overlay); each returns the failures it found rather than throwing, so the mutation suite can require that a broken variant fails at least one of them.
 */
final class OverlayScenarios {

    private OverlayScenarios() {
    }

    static String close(String id) {
        return "#" + id + " [aria-label=\"Close\"]";
    }

    /** One expectation: after step {@code step} (from 0), snapshot field {@code field} is {@code value}. */
    record Expect(int step, String field, String value) {
    }

    record Scenario(String name, List<String> actions, List<Expect> expects) {
    }

    static Expect e(int step, String field, String value) {
        return new Expect(step, field, value);
    }

    // ---------------------------------------------------------------- single overlay: the same expectations for the legacy and the migrated scripts

    static final List<Scenario> SINGLE = List.of(
            new Scenario("modal opens, then closes after its exit, giving focus back", List.of("focus:t1", "click:#t1", "click:" + close("m1"), "settle"),
                    List.of(e(1, "m1", "shown"), e(1, "active", "m1:close"), e(1, "overflow", "hidden"),
                            e(2, "m1", "shown"), e(2, "overflow", "hidden"), e(2, "active", "m1:close"),
                            e(3, "m1", "hidden"), e(3, "overflow", "-"), e(3, "active", "t1"))),
            new Scenario("drawer opens, then gives focus and scroll back at once and hides after its exit", List.of("focus:t2", "click:#t2", "click:" + close("d1"), "settle"),
                    List.of(e(1, "d1", "shown"), e(1, "active", "d1:close"), e(1, "overflow", "hidden"),
                            e(2, "d1", "shown"), e(2, "overflow", "-"), e(2, "active", "t2"),
                            e(3, "d1", "hidden"))),
            new Scenario("modal ignores its opener during its exit", List.of("click:#t1", "settle", "clicks:" + close("m1") + ",#t1", "settle"),
                    List.of(e(2, "m1", "shown"), e(3, "m1", "hidden"), e(3, "overflow", "-"))),
            new Scenario("drawer reopens during its exit", List.of("click:#t2", "clicks:" + close("d1") + ",#t2", "settle"),
                    List.of(e(1, "d1", "shown"), e(1, "overflow", "hidden"), e(1, "active", "d1:close"), e(2, "d1", "shown"), e(2, "overflow", "hidden"))),
            new Scenario("modal traps Tab between its first and last control", List.of("click:#t1", "focus:m1cancel", "key:Tab", "key:Shift+Tab", "focus:t3", "key:Tab"),
                    List.of(e(2, "active", "m1:close"), e(3, "active", "m1cancel"), e(5, "active", "t4"))),
            new Scenario("drawer traps Tab and pulls stray focus back", List.of("click:#t2", "focus:d1openm3", "key:Tab", "key:Shift+Tab", "focus:t3", "key:Tab"),
                    List.of(e(2, "active", "d1:close"), e(3, "active", "d1openm3"), e(5, "active", "d1:close"))),
            new Scenario("Escape and backdrop dismissal follow each overlay's settings",
                    List.of("click:#t3", "settle", "key:Escape", "settle", "backdrop:m3", "settle", "click:" + close("m3"), "settle", "click:#t1", "settle", "backdrop:m1",
                            "settle", "click:#t2", "settle", "backdrop:d1", "settle", "click:#t4", "settle", "key:Escape", "settle"),
                    List.of(e(3, "m3", "shown"), e(5, "m3", "shown"), e(7, "m3", "hidden"), e(7, "active", "t3"), e(11, "m1", "hidden"), e(11, "active", "t1"),
                            e(15, "d1", "hidden"), e(15, "active", "t2"), e(19, "dl", "hidden"), e(19, "active", "t4"), e(19, "overflow", "-"))),
            new Scenario("opening or closing twice is the same as once", List.of("clicks:#t1,#t1", "settle", "clicks:" + close("m1") + "," + close("m1"), "settle",
                    "clicks:#t2,#t2", "settle", "clicks:" + close("d1") + "," + close("d1"), "settle"),
                    List.of(e(0, "m1", "shown"), e(2, "m1", "shown"), e(3, "m1", "hidden"), e(3, "overflow", "-"), e(4, "d1", "shown"), e(7, "d1", "hidden"), e(7, "overflow", "-"))));

    // ---------------------------------------------------------------- a modal closed before its entry has started (its first frame has not run yet)

    static final List<String> CLOSED_BEFORE_FIRST_FRAME = List.of("clicks:#t1," + close("m1"), "wait:100", "settle");

    /** The legacy modal kept the closed modal mounted for its blind 250ms fallback (nothing was transitioning, so no transitionend came), invisible, over the page. */
    static final Scenario LEGACY_CLOSED_BEFORE_FIRST_FRAME = new Scenario("legacy: a modal closed before its first frame stays mounted for the 250ms fallback",
            CLOSED_BEFORE_FIRST_FRAME, List.of(e(0, "m1", "shown"), e(1, "m1", "shown"), e(2, "m1", "hidden"), e(2, "overflow", "-"), e(2, "active", "t1")));

    /** Corrected: nothing is transitioning, so there is no exit to wait for; the modal is hidden at once, and focus and scroll are given back at once. */
    static final Scenario CLOSED_BEFORE_ITS_FIRST_FRAME = new Scenario("a modal closed before its first frame has no exit to wait for and is hidden at once",
            CLOSED_BEFORE_FIRST_FRAME, List.of(e(0, "m1", "hidden"), e(0, "overflow", "-"), e(0, "active", "t1"), e(2, "m1", "hidden")));

    // ---------------------------------------------------------------- stacked: the same actions, the legacy outcome and the corrected one

    static final List<String> DRAWER_THEN_MODAL = List.of("click:#t2", "click:#d1openm1", "clicks:" + close("d1"), "settle", "click:" + close("m1"), "settle");
    static final List<String> MODAL_THEN_DRAWER = List.of("click:#t1", "click:#m1opend1", "clicks:" + close("m1"), "settle", "clicks:" + close("d1"), "settle");
    static final List<String> ESCAPE_MODAL_AND_DRAWER = List.of("click:#t1", "click:#m1opend1", "key:Escape", "settle", "key:Escape", "settle");
    static final List<String> TWO_MODALS = List.of("click:#t1", "click:#m1openm2", "key:Escape", "settle", "key:Escape", "settle");

    /** What the legacy scripts did when overlays were stacked (characterization, defects included). */
    static final List<Scenario> LEGACY_STACKED = List.of(
            new Scenario("legacy: closing a drawer under a modal unlocks the page, and closing the modal then leaves it locked for good", DRAWER_THEN_MODAL,
                    List.of(e(1, "m1", "shown"), e(1, "d1", "shown"), e(1, "overflow", "hidden"), e(1, "top", "m1"),
                            e(3, "d1", "hidden"), e(3, "m1", "shown"), e(3, "overflow", "-"),
                            e(5, "m1", "hidden"), e(5, "overflow", "hidden"))),
            new Scenario("legacy: a drawer opened from a modal paints underneath it, and closing the modal unlocks the page under the open drawer", MODAL_THEN_DRAWER,
                    List.of(e(1, "d1", "shown"), e(1, "m1", "shown"), e(1, "top", "m1"), e(1, "active", "d1:close"),
                            e(3, "m1", "hidden"), e(3, "d1", "shown"), e(3, "overflow", "-"),
                            e(5, "d1", "hidden"), e(5, "overflow", "-"))),
            new Scenario("legacy: one Escape closes a modal and the drawer above it", ESCAPE_MODAL_AND_DRAWER,
                    List.of(e(3, "m1", "hidden"), e(3, "d1", "hidden"))),
            // Which value the page's overflow ends with here depends on which modal's exit callback runs last (each restores what it saw when it opened), which the legacy
            // scripts did not order, so only the part that always happens is recorded. The deterministic stuck lock is the drawer-then-modal scenario above.
            new Scenario("legacy: one Escape closes two stacked modals", TWO_MODALS,
                    List.of(e(1, "m2", "shown"), e(3, "m1", "hidden"), e(3, "m2", "hidden"))));

    /** The corrected stacking contract. */
    static final List<Scenario> STACKED = List.of(
            new Scenario("closing a drawer under a modal keeps the page locked until the modal closes too", DRAWER_THEN_MODAL,
                    List.of(e(1, "top", "m1"), e(1, "active", "m1:close"),
                            e(3, "d1", "hidden"), e(3, "m1", "shown"), e(3, "overflow", "hidden"), e(3, "active", "m1:close"),
                            e(5, "m1", "hidden"), e(5, "overflow", "-"))),
            new Scenario("a drawer opened from a modal paints on top, and closing the modal under it keeps the page locked and focus in the drawer", MODAL_THEN_DRAWER,
                    List.of(e(1, "d1", "shown"), e(1, "top", "d1"), e(1, "active", "d1:close"),
                            e(3, "m1", "hidden"), e(3, "d1", "shown"), e(3, "overflow", "hidden"), e(3, "active", "d1:close"),
                            e(5, "d1", "hidden"), e(5, "overflow", "-"))),
            new Scenario("one Escape closes only the topmost overlay, and focus goes back to its opener inside the overlay below", ESCAPE_MODAL_AND_DRAWER,
                    List.of(e(3, "d1", "hidden"), e(3, "m1", "shown"), e(3, "active", "m1opend1"), e(3, "overflow", "hidden"), e(3, "top", "m1"),
                            e(5, "m1", "hidden"), e(5, "active", "t1"), e(5, "overflow", "-"))),
            new Scenario("one Escape closes one of two stacked modals, and the page is unlocked only after the last", TWO_MODALS,
                    List.of(e(1, "top", "m2"), e(3, "m2", "hidden"), e(3, "m1", "shown"), e(3, "active", "m1openm2"), e(3, "overflow", "hidden"),
                            e(5, "m1", "hidden"), e(5, "overflow", "-"), e(5, "active", "t1"))),
            new Scenario("Escape on a topmost modal that does not close on Escape closes nothing, not even the modal under it",
                    List.of("click:#t1", "click:#m1openm3", "key:Escape", "settle", "click:" + close("m3"), "settle", "key:Escape", "settle"),
                    List.of(e(1, "top", "m3"), e(3, "m3", "shown"), e(3, "m1", "shown"), e(3, "overflow", "hidden"),
                            e(5, "m3", "hidden"), e(5, "m1", "shown"), e(5, "active", "m1openm3"), e(7, "m1", "hidden"), e(7, "overflow", "-"))),
            new Scenario("Escape on a topmost modal that does not close on Escape does not close the drawer under it",
                    List.of("click:#t2", "click:#d1openm3", "key:Escape", "settle"),
                    List.of(e(1, "top", "m3"), e(3, "m3", "shown"), e(3, "d1", "shown"), e(3, "overflow", "hidden"))),
            new Scenario("a modal's deferred focus restore does not pull focus behind a drawer opened during its exit",
                    List.of("click:#t1", "clicks:" + close("m1") + ",#t2", "settle"),
                    List.of(e(2, "m1", "hidden"), e(2, "d1", "shown"), e(2, "active", "d1:close"), e(2, "overflow", "hidden"), e(2, "top", "d1"))),
            new Scenario("closing the overlay under the topmost one leaves focus where it is",
                    List.of("click:#t1", "click:#m1opend1", "click:#d1closem1", "settle"),
                    List.of(e(3, "m1", "hidden"), e(3, "d1", "shown"), e(3, "active", "d1closem1"), e(3, "overflow", "hidden"))),
            new Scenario("Tab brings focus that has ended up behind the topmost overlay back into it",
                    List.of("click:#t1", "click:#m1opend1", "focus:m1a", "key:Tab"),
                    List.of(e(3, "active", "d1:close"))));

    // ---------------------------------------------------------------- running

    /** Runs one scenario on a fresh load of the page and returns its transcript. */
    static List<String> run(DevToolsSession cdp, String url, Scenario s) throws Exception {
        cdp.navigate(url);
        OverlayBrowser.ready(cdp);
        return OverlayBrowser.run(cdp, s.actions());
    }

    /** Every expectation of {@code s} that {@code transcript} does not meet. */
    static List<String> failures(String where, Scenario s, List<String> transcript) {
        List<String> found = new ArrayList<>();
        for (Expect x : s.expects()) {
            String actual = OverlayBrowser.field(transcript.get(x.step()), x.field());
            if (!actual.equals(x.value())) {
                found.add(where + " | " + s.name() + " | step " + x.step() + " (" + s.actions().get(x.step()) + ") " + x.field() + ": expected " + x.value() + ", was " + actual
                        + " | " + transcript.get(x.step()));
            }
        }
        return found;
    }

    /** Runs every scenario against the page and returns the failures. */
    static List<String> check(DevToolsSession cdp, String url, String where, List<Scenario> scenarios) throws Exception {
        List<String> found = new ArrayList<>();
        for (Scenario s : scenarios) {
            List<String> transcript = new ArrayList<>();
            try {
                cdp.navigate(url);
                OverlayBrowser.ready(cdp);
                for (String action : s.actions()) {
                    OverlayBrowser.perform(cdp, action);
                    transcript.add(action + " -> " + OverlayBrowser.snapshot(cdp));
                }
            } catch (IllegalStateException e) {
                // An action that could not be performed (an element not there to click) is a failure of the behaviour under test, reported with what happened so far.
                found.add(where + " | " + s.name() + " | step " + transcript.size() + " (" + s.actions().get(transcript.size()) + ") could not be performed: " + e.getMessage()
                        + " | after " + (transcript.isEmpty() ? "loading" : transcript.get(transcript.size() - 1)));
                continue;
            }
            found.addAll(failures(where, s, transcript));
        }
        return found;
    }

    // ---------------------------------------------------------------- probes (pages of their own)

    private static String ev(DevToolsSession cdp, String js) throws Exception {
        return cdp.evaluate("(function(){try{return String(" + js + ")}catch(e){return 'ERR '+e}})()");
    }

    private static void expect(List<String> found, String where, String what, String expected, String actual) {
        if (!expected.equals(actual)) {
            found.add(where + " | " + what + ": expected " + expected + ", was " + actual);
        }
    }

    private static String shown(DevToolsSession cdp, String id) throws Exception {
        return ev(cdp, "getComputedStyle(document.getElementById('" + id + "')).display === 'none' ? 'hidden' : 'shown'");
    }

    private static final String FINISH_ALL = "document.getAnimations().forEach(function(a){a.finish()})";

    /**
     * Every probe, against the scripts at {@code base} (the real "/ashlar/js/" or a registered set). Each returns its failures: transitions under the test's control, the
     * fallback timer, cancelled and zero-length transitions, reduced motion, an interrupted exit, the page's own overflow, a release by an overlay that holds nothing, twelve
     * stacked modals, the floating components inside a Modal and a Drawer, and a popup whose own listener runs before the overlays'.
     */
    static List<String> probes(ParitySession session, CascadeFixtureServer server, String base) throws Exception {
        List<String> found = new ArrayList<>();
        try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
            OverlayBrowser.viewport(cdp, 900, 700);
            found.addAll(transitionProbe(cdp, server, base));
            found.addAll(scrollProbe(cdp, server, base));
            found.addAll(depthProbe(cdp, server, base));
            found.addAll(floatingProbe(cdp, server, base));
            found.addAll(earlyListenerProbe(cdp, server, base));
        }
        return found;
    }

    private static String nativePage(CascadeFixtureServer server, String base, String extraCss) throws Exception {
        return server.registerRaw(OverlayBrowser.scenarioPage(server, false, base, extraCss));
    }

    static List<String> transitionProbe(DevToolsSession cdp, CascadeFixtureServer server, String base) throws Exception {
        List<String> found = new ArrayList<>();
        String parts = "[data-modal-dialog],[data-modal-backdrop],[data-drawer-panel],[data-drawer-backdrop]";

        // Ten-second transitions, finished by the test: the exit is over when EVERY transition is, never on a timer of its own, never on the first transitionend.
        cdp.navigate(nativePage(server, base, parts + "{transition-duration:10s !important}"));
        OverlayBrowser.ready(cdp);
        OverlayBrowser.click(cdp, "#t1");
        Thread.sleep(100); // the Modal enters on its next animation frame: let its transitions start before finishing them
        ev(cdp, FINISH_ALL);
        OverlayBrowser.click(cdp, close("m1"));
        Thread.sleep(400);
        expect(found, "transitions", "a 10s exit is still running 400ms after close", "shown", shown(cdp, "m1"));
        ev(cdp, "document.querySelector('#m1 [data-modal-backdrop]').getAnimations().forEach(function(a){a.finish()})");
        Thread.sleep(80);
        expect(found, "transitions", "the backdrop has finished, the dialog has not", "shown", shown(cdp, "m1"));
        ev(cdp, FINISH_ALL);
        Thread.sleep(80);
        expect(found, "transitions", "every transition has finished", "hidden", shown(cdp, "m1"));
        expect(found, "transitions", "focus back on the opener", "t1", ev(cdp, "document.activeElement.id"));

        // A drawer reopened during its exit: the old exit's end must not hide it.
        OverlayBrowser.click(cdp, "#t2");
        Thread.sleep(100);
        ev(cdp, FINISH_ALL);
        ev(cdp, "(function(){document.querySelector('" + close("d1").replace("'", "\\'") + "').click();document.getElementById('t2').click();return 'ok'})()");
        ev(cdp, FINISH_ALL);
        Thread.sleep(120);
        expect(found, "transitions", "a drawer reopened during its exit is still shown after the old exit ends", "shown", shown(cdp, "d1"));
        expect(found, "transitions", "and it is open", "open", ev(cdp, "document.getElementById('d1').dataset.state"));
        expect(found, "transitions", "and the page is still locked", "hidden", ev(cdp, "document.body.style.overflowY"));

        // A cancelled transition settles the exit.
        cdp.navigate(nativePage(server, base, parts + "{transition-duration:10s !important}"));
        OverlayBrowser.ready(cdp);
        OverlayBrowser.click(cdp, "#t1");
        Thread.sleep(100); // the Modal enters on its next animation frame: let its transitions start before finishing them
        ev(cdp, FINISH_ALL);
        OverlayBrowser.click(cdp, close("m1"));
        ev(cdp, "document.getAnimations().forEach(function(a){a.cancel()})");
        Thread.sleep(80);
        expect(found, "transitions", "an exit whose transitions were cancelled is over", "hidden", shown(cdp, "m1"));

        // The fallback: transitions that never settle (paused) end on the timer read from the computed 300ms, not before and not never.
        cdp.navigate(nativePage(server, base, parts + "{transition-duration:300ms !important}"));
        OverlayBrowser.ready(cdp);
        OverlayBrowser.click(cdp, "#t1");
        Thread.sleep(100); // the Modal enters on its next animation frame: let its transitions start before finishing them
        ev(cdp, FINISH_ALL);
        OverlayBrowser.click(cdp, close("m1"));
        ev(cdp, "document.getAnimations().forEach(function(a){a.pause()})");
        Thread.sleep(150);
        expect(found, "transitions", "a paused 300ms exit is still running at 150ms", "shown", shown(cdp, "m1"));
        Thread.sleep(500);
        expect(found, "transitions", "a paused 300ms exit has ended on the fallback timer by 650ms", "hidden", shown(cdp, "m1"));

        // Zero-length transitions and reduced motion: over at once, in the same task as the click.
        for (boolean reduced : new boolean[] {false, true}) {
            String css = reduced ? "" : parts + "{transition-duration:0s !important}";
            cdp.navigate(nativePage(server, base, css));
            OverlayBrowser.ready(cdp);
            OverlayBrowser.reducedMotion(cdp, reduced);
            OverlayBrowser.click(cdp, "#t1");
            String same = ev(cdp, "(function(){document.querySelector('" + close("m1").replace("'", "\\'") + "').click();"
                    + "return getComputedStyle(document.getElementById('m1')).display})()");
            expect(found, "transitions", (reduced ? "reduced motion" : "a 0s transition") + ": hidden in the same task as the close", "none", same);
            OverlayBrowser.click(cdp, "#t2");
            same = ev(cdp, "(function(){document.querySelector('" + close("d1").replace("'", "\\'") + "').click();"
                    + "return getComputedStyle(document.getElementById('d1')).display})()");
            expect(found, "transitions", (reduced ? "reduced motion" : "a 0s transition") + ": the drawer is hidden in the same task as the close", "none", same);
            OverlayBrowser.reducedMotion(cdp, false);
        }
        return found;
    }

    static List<String> scrollProbe(DevToolsSession cdp, CascadeFixtureServer server, String base) throws Exception {
        List<String> found = new ArrayList<>();
        for (String own : List.of("", "overflow: scroll", "overflow-y: auto !important", "overflow-x: clip; overflow-y: scroll")) {
            String html = OverlayBrowser.scenarioPage(server, false, base, "").replace("<body style=\"margin:8px\">", "<body style=\"margin:8px" + (own.isEmpty() ? "" : "; " + own) + "\">");
            cdp.navigate(server.registerRaw(html));
            OverlayBrowser.ready(cdp);
            String before = ev(cdp, "document.body.style.cssText");
            ev(cdp, "(function(){window.__writes=0;new MutationObserver(function(r){window.__writes+=r.length}).observe(document.body,{attributes:true,attributeFilter:['style']});return 'ok'})()");
            String where = "scroll (" + (own.isEmpty() ? "no inline overflow" : own) + ")";
            OverlayBrowser.click(cdp, "#t1");
            String afterLock = ev(cdp, "window.__writes");
            List<String> steps = List.of("click:#m1opend1", "key:Escape", "settle", "click:#m1openm2", "click:#m2a", "key:Escape", "settle");
            for (String step : steps) {
                OverlayBrowser.perform(cdp, step);
                expect(found, where, "locked while an overlay is open, after " + step, "hidden", ev(cdp, "getComputedStyle(document.body).overflowY"));
            }
            expect(found, where, "no style write between the first lock and the last release", afterLock, ev(cdp, "window.__writes"));
            OverlayBrowser.perform(cdp, "key:Escape");
            OverlayBrowser.perform(cdp, "settle");
            expect(found, where, "the page's own inline style is back exactly", before, ev(cdp, "document.body.style.cssText"));
            String restored = ev(cdp, "window.__writes");
            Thread.sleep(400);
            expect(found, where, "nothing writes the style again after the release", restored, ev(cdp, "window.__writes"));
        }
        // A release by an owner that holds nothing (or a second release) never touches the page once every overlay has let go.
        cdp.navigate(nativePage(server, base, ""));
        OverlayBrowser.ready(cdp);
        OverlayBrowser.click(cdp, "#t2");
        OverlayBrowser.click(cdp, close("d1"));
        Thread.sleep(450);
        // The premise, checked: the only overlay has closed and let go, so nothing holds the lock when the stray release comes.
        expect(found, "scroll", "the drawer closed before the stray release", "hidden", shown(cdp, "d1"));
        expect(found, "scroll", "and released the page", "", ev(cdp, "document.body.style.overflowY"));
        ev(cdp, "document.body.style.overflow = 'clip'");
        String release = cdp.call("Runtime.evaluate", "{\"expression\":" + DevToolsSession.quote("import(" + OverlayBrowser.js(base.startsWith("/") ? base + "overlay.js" : base + "overlay.js")
                + ").then(function(m){var stranger={};m.unlockScroll(stranger);m.unlockScroll(stranger);return 'ok'},function(e){return 'ERR '+e})") + ",\"awaitPromise\":true,\"returnByValue\":true}");
        if (!release.contains("\"value\":\"ok\"")) {
            found.add("scroll | releasing a lock this owner never held must be a no-op: " + release);
        }
        expect(found, "scroll", "the application's own overflow set after every overlay closed survives a stray release", "clip", ev(cdp, "document.body.style.overflow"));
        return found;
    }

    static List<String> depthProbe(DevToolsSession cdp, CascadeFixtureServer server, String base) throws Exception {
        List<String> found = new ArrayList<>();
        int depth = 12;
        StringBuilder body = new StringBuilder("<button id=\"c0next\" type=\"button\" data-modal-open=\"c1\">open c1</button>");
        // The toast band: a fixed element at z-index 50, the toast region's level.
        body.append("<div id=\"toast\" style=\"position:fixed;right:0;bottom:0;width:60px;height:60px;z-index:50;background:#000\"></div>");
        for (int i = 1; i <= depth; i++) {
            String next = i < depth ? "<button id=\"c" + i + "next\" type=\"button\" data-modal-open=\"c" + (i + 1) + "\">open c" + (i + 1) + "</button>" : "<p>last</p>";
            body.append(OverlayBrowser.modal("c" + i, "Modal " + i, OverlayBrowser.o("size", "Small"), next, null));
        }
        String html = OverlayBrowser.page(List.of(ParityFrames.ASHLAR), "light", OverlayBrowser.withScripts(body.toString(), base), "");
        cdp.navigate(server.registerRaw(html));
        OverlayBrowser.ready(cdp);
        for (int i = 0; i < depth; i++) {
            OverlayBrowser.click(cdp, "#c" + i + "next");
        }
        StringBuilder z = new StringBuilder();
        for (int i = 1; i <= depth; i++) {
            z.append(i == 1 ? "" : ",").append(ev(cdp, "getComputedStyle(document.getElementById('c" + i + "')).zIndex"));
        }
        expect(found, "depth", "z-index of twelve stacked modals: 40 to 49, then capped", "40,41,42,43,44,45,46,47,48,49,49,49", z.toString());
        expect(found, "depth", "the toast band stays above every overlay", "toast", ev(cdp, "document.elementFromPoint(innerWidth-10, innerHeight-10).id"));
        expect(found, "depth", "the last one opened paints on top", "c12", ev(cdp, "document.elementFromPoint(innerWidth/2, innerHeight/2).closest('[data-modal]').id"));
        OverlayBrowser.press(cdp, "Escape");
        Thread.sleep(450);
        expect(found, "depth", "one Escape closes only the topmost of twelve", "hidden,shown", shown(cdp, "c12") + "," + shown(cdp, "c11"));
        expect(found, "depth", "the next one down paints on top", "c11", ev(cdp, "document.elementFromPoint(innerWidth/2, innerHeight/2).closest('[data-modal]').id"));
        return found;
    }

    /** The floating components that announce an open popup, each rendered inside a Modal and inside a Drawer. */
    private record Floating(String name, String markup, String open, String isOpen) {
    }

    private static List<Floating> floating(String prefix) {
        String popTrigger = "<button id=\"" + prefix + "-pop\" type=\"button\">Pop</button>";
        Map<String, Object> pop = OverlayBrowser.o("trigger", OverlayBrowser.raw(popTrigger), "content", OverlayBrowser.raw("<p>Popover body</p>"), "title", "Pop");
        Map<String, Object> tip = OverlayBrowser.o("trigger", OverlayBrowser.raw("<button id=\"" + prefix + "-tip\" type=\"button\">Tip</button>"), "content", "A tip");
        Map<String, Object> menu = OverlayBrowser.o("label", "Menu", "items", (gg.jte.Content) out -> {
            gg.jte.output.StringOutput s = new gg.jte.output.StringOutput();
            OverlayBrowser.renderInto("dropdown-menu-item.jte", OverlayBrowser.o("label", "One"), s);
            OverlayBrowser.renderInto("dropdown-menu-item.jte", OverlayBrowser.o("label", "Two"), s);
            out.writeContent(s.toString());
        });
        List<SelectOption> options = List.of(SelectOption.of("a", "Alpha"), SelectOption.of("b", "Beta"));
        return List.of(
                new Floating("popover", OverlayBrowser.renderComponent("popover.jte", pop), "click:#" + prefix + "-pop",
                        "document.getElementById('" + prefix + "-pop').closest('[data-popover]').querySelector('[aria-expanded]').getAttribute('aria-expanded')"),
                new Floating("tooltip", OverlayBrowser.renderComponent("tooltip.jte", tip), "focus:" + prefix + "-tip",
                        "(function(){var t=document.getElementById('" + prefix + "-tip').closest('[data-tooltip]');var b=t.querySelector('[role=tooltip]');"
                                + "var d=[].some.call(t.querySelectorAll('[aria-describedby]'),function(e){return e.getAttribute('aria-describedby').split(/\\s+/).indexOf(b.id)>=0});return String(d)})()"),
                new Floating("dropdown menu", "<div id=\"" + prefix + "-menu\">" + OverlayBrowser.renderComponent("dropdown-menu.jte", menu) + "</div>", "click:#" + prefix + "-menu [data-dropdown-menu-trigger]",
                        "document.querySelector('#" + prefix + "-menu [data-dropdown-menu-trigger]').getAttribute('aria-expanded')"),
                new Floating("select", "<div id=\"" + prefix + "-sel\">" + OverlayBrowser.renderComponent("select.jte", OverlayBrowser.o("name", prefix + "-select", "label", "Pick", "options", options)) + "</div>",
                        "click:#" + prefix + "-sel [data-select-trigger]", "document.querySelector('#" + prefix + "-sel [data-select-trigger]').getAttribute('aria-expanded')"),
                new Floating("combobox", "<div id=\"" + prefix + "-cb\">" + OverlayBrowser.renderComponent("combobox.jte", OverlayBrowser.o("name", prefix + "-combo", "label", "Find", "options", options)) + "</div>",
                        "focus-then-arrow:#" + prefix + "-cb [data-combobox-input]", "document.querySelector('#" + prefix + "-cb [data-combobox-input]').getAttribute('aria-expanded')"),
                new Floating("date picker", "<div id=\"" + prefix + "-dp\">" + OverlayBrowser.renderComponent("date-picker.jte", OverlayBrowser.o("name", prefix + "-date", "label", "When")) + "</div>",
                        "click:#" + prefix + "-dp [data-date-picker-trigger]", "document.querySelector('#" + prefix + "-dp [data-date-picker-trigger]').getAttribute('aria-expanded')"));
    }

    private static String isOpenValue(String raw) {
        return raw.equals("true") ? "open" : "closed";
    }

    static List<String> floatingProbe(DevToolsSession cdp, CascadeFixtureServer server, String base) throws Exception {
        List<String> found = new ArrayList<>();
        Map<String, List<Floating>> inside = new LinkedHashMap<>();
        inside.put("fm", floating("fm"));
        inside.put("fd", floating("fd"));
        StringBuilder modalBody = new StringBuilder();
        inside.get("fm").forEach(f -> modalBody.append("<div style=\"margin:4px 0\">").append(f.markup()).append("</div>"));
        StringBuilder drawerBody = new StringBuilder();
        inside.get("fd").forEach(f -> drawerBody.append("<div style=\"margin:4px 0\">").append(f.markup()).append("</div>"));
        String body = "<button id=\"tfm\" type=\"button\" data-modal-open=\"fm\">open fm</button><button id=\"tfd\" type=\"button\" data-drawer-open=\"fd\">open fd</button>"
                + OverlayBrowser.modal("fm", "Floating in a modal", OverlayBrowser.o("size", "Large"), modalBody.toString(), null)
                + OverlayBrowser.drawer("fd", "Floating in a drawer", null, drawerBody.toString(), null);
        // The application's stylesheet: Tailwind (for the floating components, not yet migrated) and Ashlar's own.
        String url = server.registerRaw(OverlayBrowser.page(List.of(ParityFrames.APP), "light", OverlayBrowser.withScripts(body, base), ""));
        for (Map.Entry<String, List<Floating>> overlay : inside.entrySet()) {
            String id = overlay.getKey();
            for (Floating f : overlay.getValue()) {
                String where = "floating (" + f.name() + " in " + (id.equals("fm") ? "a Modal" : "a Drawer") + ")";
                cdp.navigate(url);
                OverlayBrowser.ready(cdp);
                OverlayBrowser.click(cdp, "#t" + id);
                Thread.sleep(300);
                if (f.open().startsWith("focus-then-arrow:")) {
                    String sel = f.open().substring("focus-then-arrow:".length());
                    ev(cdp, "document.querySelector(" + OverlayBrowser.js(sel) + ").focus()");
                    OverlayBrowser.press(cdp, "ArrowDown");
                } else {
                    OverlayBrowser.perform(cdp, f.open());
                }
                Thread.sleep(300);
                String opened = isOpenValue(ev(cdp, f.isOpen()));
                if (!opened.equals("open")) {
                    found.add(where + " | the popup did not open, so nothing was tested (" + f.open() + ")");
                    continue;
                }
                OverlayBrowser.press(cdp, "Escape");
                Thread.sleep(450);
                expect(found, where, "the first Escape closes the popup", "closed", isOpenValue(ev(cdp, f.isOpen())));
                expect(found, where, "and leaves the overlay open", "shown", shown(cdp, id));
                OverlayBrowser.press(cdp, "Escape");
                Thread.sleep(450);
                expect(found, where, "a second Escape closes the overlay", "hidden", shown(cdp, id));
            }
        }
        // Not a popup: an expanded Accordion item (aria-expanded="true", aria-controls, no aria-haspopup) owns no Escape, so the first Escape closes the modal around it.
        gg.jte.Content items = out -> OverlayBrowser.renderInto("accordion-item.jte", OverlayBrowser.o("accordionId", "acc", "itemId", "acc-1", "title", "Section",
                "expanded", true, "content", OverlayBrowser.raw("<p>Open section</p>")), out);
        String accordion = OverlayBrowser.renderComponent("accordion.jte", OverlayBrowser.o("id", "acc", "items", items));
        String accordionBody = "<button id=\"tam\" type=\"button\" data-modal-open=\"am\">open am</button>"
                + OverlayBrowser.modal("am", "Accordion in a modal", OverlayBrowser.o(), accordion, null);
        cdp.navigate(server.registerRaw(OverlayBrowser.page(List.of(ParityFrames.APP), "light", OverlayBrowser.withScripts(accordionBody, base), "")));
        OverlayBrowser.ready(cdp);
        OverlayBrowser.click(cdp, "#tam");
        Thread.sleep(300);
        expect(found, "floating (an expanded Accordion in a Modal)", "the accordion item is expanded", "true",
                ev(cdp, "document.querySelector('#am [aria-controls]').getAttribute('aria-expanded')"));
        OverlayBrowser.press(cdp, "Escape");
        Thread.sleep(450);
        expect(found, "floating (an expanded Accordion in a Modal)", "an expanded disclosure owns no Escape: the first Escape closes the modal", "hidden", shown(cdp, "am"));

        // Not open either: a CLOSED Popover whose content holds an expanded Accordion. Only the Popover's anchor (in its trigger slot) says whether it is open.
        gg.jte.Content inner = out -> OverlayBrowser.renderInto("accordion-item.jte", OverlayBrowser.o("accordionId", "pacc", "itemId", "pacc-1", "title", "Inside",
                "expanded", true, "content", OverlayBrowser.raw("<p>Open section inside a popover</p>")), out);
        String popoverWithAccordion = OverlayBrowser.renderComponent("popover.jte", OverlayBrowser.o("trigger", OverlayBrowser.raw("<button id=\"pa-pop\" type=\"button\">Pop</button>"),
                "content", OverlayBrowser.raw(OverlayBrowser.renderComponent("accordion.jte", OverlayBrowser.o("id", "pacc", "items", inner))), "title", "Pop"));
        String closedPopoverBody = "<button id=\"tpm\" type=\"button\" data-modal-open=\"pm\">open pm</button>"
                + OverlayBrowser.modal("pm", "Closed popover in a modal", OverlayBrowser.o(), popoverWithAccordion, null);
        cdp.navigate(server.registerRaw(OverlayBrowser.page(List.of(ParityFrames.APP), "light", OverlayBrowser.withScripts(closedPopoverBody, base), "")));
        OverlayBrowser.ready(cdp);
        OverlayBrowser.click(cdp, "#tpm");
        Thread.sleep(300);
        String where = "floating (a closed Popover holding an expanded Accordion, in a Modal)";
        expect(found, where, "the popover is closed", "false", ev(cdp, "document.getElementById('pa-pop').getAttribute('aria-expanded')"));
        expect(found, where, "the accordion inside it is expanded", "true", ev(cdp, "document.querySelector('#pm [data-popover-surface] [aria-controls]').getAttribute('aria-expanded')"));
        OverlayBrowser.press(cdp, "Escape");
        Thread.sleep(450);
        expect(found, where, "nothing open owns the Escape: the first Escape closes the modal", "hidden", shown(cdp, "pm"));
        return found;
    }

    /** A popup whose own Escape listener was added before Ashlar's scripts ran and closes it without stopping the event: the overlay must still stay open. */
    static List<String> earlyListenerProbe(DevToolsSession cdp, CascadeFixtureServer server, String base) throws Exception {
        List<String> found = new ArrayList<>();
        String early = "<script>document.addEventListener('keydown',function(e){var b=document.getElementById('fake');if(e.key==='Escape'&&b&&b.getAttribute('aria-expanded')==='true')"
                + "{b.setAttribute('aria-expanded','false')}})</script>";
        String body = early + "<button id=\"tm\" type=\"button\" data-modal-open=\"em\">open</button>"
                + OverlayBrowser.modal("em", "Early listener", OverlayBrowser.o(), "<button id=\"fake\" type=\"button\" aria-haspopup=\"menu\" aria-expanded=\"false\">fake popup</button>", null);
        cdp.navigate(server.registerRaw(OverlayBrowser.page(List.of(ParityFrames.ASHLAR), "light", OverlayBrowser.withScripts(body, base), "")));
        OverlayBrowser.ready(cdp);
        OverlayBrowser.click(cdp, "#tm");
        ev(cdp, "document.getElementById('fake').setAttribute('aria-expanded','true')");
        ev(cdp, "document.getElementById('fake').focus()");
        OverlayBrowser.press(cdp, "Escape");
        Thread.sleep(450);
        expect(found, "early listener", "the popup closed on Escape", "false", ev(cdp, "document.getElementById('fake').getAttribute('aria-expanded')"));
        expect(found, "early listener", "the modal stays open although the popup's listener ran first", "shown", shown(cdp, "em"));
        OverlayBrowser.press(cdp, "Escape");
        Thread.sleep(450);
        expect(found, "early listener", "the next Escape closes the modal", "hidden", shown(cdp, "em"));
        return found;
    }
}
