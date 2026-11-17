package dev.redspartanlabs.ashlar.showcase.cascade;

import java.util.ArrayList;
import java.util.List;

/**
 * The Tooltip and Popover behavior scenarios, written once and run by three suites: {@link FloatingLegacyCharacterizationTest} (the legacy scripts, recording what they did,
 * their two defects included), {@link FloatingBehaviourTest} (the migrated scripts: the same shared behavior, and the two corrections) and {@link FloatingMutationTest} (the
 * same checks against deliberately broken variants, each of which must fail them).
 *
 * <p>A scenario is a list of actions ({@link FloatingBrowser#perform}) on the behavior page ({@link FloatingBrowser#scenarioBody}); after every action the snapshot records
 * whether each tooltip is shown and described, whether each popover is open, closing or closed, tt3's aria-describedby, and the focused element. An expectation names a step,
 * a snapshot field and the value it must have. The expectations were written from the components' documented behavior, not from either implementation's output.
 *
 * <p>The probes ({@link #nudgeProbe}, {@link #reducedMotionProbe}, {@link #popoverRaceProbe}, {@link #smoothReopenProbe}) measure what a snapshot cannot: geometry, the
 * frames of a fade, the style writes of a show. Each returns what it found rather than throwing, so the mutation suite can require that a broken variant fails one of them.
 */
final class FloatingScenarios {

    private FloatingScenarios() {
    }

    record Scenario(String name, List<String> actions, List<OverlayScenarios.Expect> expects) {
    }

    private static OverlayScenarios.Expect e(int step, String field, String value) {
        return new OverlayScenarios.Expect(step, field, value);
    }

    // ---------------------------------------------------------------- shared: the same expectations for the legacy and the migrated scripts

    static final List<Scenario> SHARED = List.of(
            new Scenario("a tooltip shows after its delay on hover, and hides at once when the pointer leaves",
                    List.of("hover:#tt1", "wait:60", "wait:300", "away", "settle"),
                    List.of(e(1, "tip:tt1", "hidden"), e(2, "tip:tt1", "shown+described"), e(3, "tip:tt1", "shown"), e(4, "tip:tt1", "hidden"))),
            new Scenario("a tooltip shows on keyboard focus and hides when focus leaves",
                    List.of("focus:before", "key:Tab", "wait:300", "key:Tab", "settle"),
                    List.of(e(1, "active", "tt1"), e(2, "tip:tt1", "shown+described"), e(4, "tip:tt1", "hidden"))),
            new Scenario("Escape hides a hovered tooltip and leaves focus where it was",
                    List.of("focus:before", "hover:#tt1", "wait:300", "key:Escape", "settle"),
                    List.of(e(2, "tip:tt1", "shown+described"), e(4, "tip:tt1", "hidden"), e(4, "active", "before"))),
            new Scenario("a trigger with nothing focusable inside becomes a keyboard stop of its own",
                    List.of("focus:tt1", "key:Tab", "wait:300"),
                    List.of(e(1, "active", "slot:tt2"), e(2, "tip:tt2", "shown+described"))),
            new Scenario("the tooltip adds its id to an aria-describedby that was already there and takes only its own id away",
                    List.of("hover:#tt3", "wait:300", "away", "settle"),
                    List.of(e(1, "db:tt3", "t3help own"), e(3, "db:tt3", "t3help"))),
            new Scenario("a touch pointer does not show a tooltip, a mouse pointer does",
                    List.of("js:document.getElementById('tt1').dispatchEvent(new PointerEvent('pointerenter',{pointerType:'touch'}))", "wait:300",
                            "js:document.getElementById('tt1').dispatchEvent(new PointerEvent('pointerenter',{pointerType:'mouse'}))", "wait:300"),
                    List.of(e(1, "tip:tt1", "hidden"), e(3, "tip:tt1", "shown+described"))),
            new Scenario("a popover opens on a click and closes on a second one, and focus stays on its trigger",
                    List.of("click:#pp1", "wait:250", "click:#pp1", "settle"),
                    List.of(e(1, "pop:pp1", "open"), e(1, "active", "pp1"), e(2, "pop:pp1", "closing"), e(3, "pop:pp1", "closed"), e(3, "active", "pp1"))),
            new Scenario("a click outside closes the popover without moving focus back",
                    List.of("click:#pp1", "wait:250", "click:#outside", "settle"),
                    List.of(e(1, "pop:pp1", "open"), e(3, "pop:pp1", "closed"), e(3, "active", "outside"))),
            new Scenario("a click on a control inside the surface keeps the popover open",
                    List.of("click:#pp1", "wait:250", "click:#p1in", "wait:250"),
                    List.of(e(3, "pop:pp1", "open"), e(3, "active", "p1in"))),
            new Scenario("a click on plain text inside the surface (focus goes nowhere) keeps the popover open",
                    List.of("click:#pp1", "wait:250", "click:#p1text", "wait:250"),
                    List.of(e(3, "pop:pp1", "open"))),
            new Scenario("Escape closes the popover from inside it and gives focus back to its trigger",
                    List.of("click:#pp1", "wait:250", "key:Tab", "key:Escape", "settle"),
                    List.of(e(2, "active", "p1in"), e(4, "pop:pp1", "closed"), e(4, "active", "pp1"))),
            new Scenario("tabbing out of the popover's last control closes it",
                    List.of("click:#pp1", "wait:250", "focus:p1last", "key:Tab", "settle"),
                    List.of(e(2, "pop:pp1", "open"), e(4, "pop:pp1", "closed"))),
            new Scenario("a trigger with nothing focusable inside becomes a button: Enter opens the popover, Space closes it",
                    List.of("focusslot:pp2", "key:Enter", "wait:250", "key:Space", "settle"),
                    List.of(e(0, "active", "slot:pp2"), e(2, "pop:pp2", "open"), e(4, "pop:pp2", "closed"))),
            new Scenario("opening one popover closes the one that was open",
                    List.of("click:#pp1", "wait:250", "click:#pp3", "settle"),
                    List.of(e(3, "pop:pp1", "closed"), e(3, "pop:pp3", "open"))),
            new Scenario("a popover reopened during its exit stays open after the old exit's transition and its fallback timer have passed",
                    List.of("click:#pp1", "wait:300", "js:var b=document.getElementById('pp1');b.click();setTimeout(function(){b.click()},20)", "wait:700"),
                    List.of(e(3, "pop:pp1", "open"))),
            // Kept as they were (P2, P3, P6): recorded, not endorsed as a future contract.
            new Scenario("kept: one Escape closes a nested popover and the popover around it, and focus goes to the outer trigger",
                    List.of("click:#pno", "wait:250", "click:#pni", "wait:250", "key:Escape", "settle"),
                    List.of(e(3, "pop:pno", "open"), e(3, "pop:pni", "open"), e(5, "pop:pni", "closed"), e(5, "pop:pno", "closed"), e(5, "active", "pno"))),
            new Scenario("kept: one Escape hides a tooltip inside a popover and closes the popover, and focus goes to the popover's trigger",
                    List.of("click:#ptp", "wait:250", "key:Tab", "wait:300", "key:Escape", "settle"),
                    List.of(e(3, "tip:ptt", "shown+described"), e(5, "tip:ptt", "hidden"), e(5, "pop:ptp", "closed"), e(5, "active", "ptp"))),
            new Scenario("kept: a trigger whose own content has tabindex=-1 is skipped by Tab, and hover and click still work",
                    List.of("focus:before2", "key:Tab", "hover:#tt4", "wait:300", "click:#pp4", "wait:250"),
                    List.of(e(1, "active", "after2"), e(3, "tip:tt4", "shown+described"), e(5, "pop:pp4", "open"))));

    // ---------------------------------------------------------------- T1: a tooltip coming back during its exit, the legacy outcome and the corrected one

    // Every step waits on what the page shows (FloatingBrowser.perform), not on the clock. The pointer or focus leaves only once the entry has ended and its transitionend
    // has been delivered (an exit begun in the very frame the entry ends would be ended at once by the entry's own transitionend, in the legacy scripts and the migrated
    // ones alike, and would then not be an exit to come back during), and comes back as soon as the exit is visibly under way, its fade already running.
    //
    // Early in the fade (T1_MOUSE, T1_KEYBOARD) the reversed fade is short and ends, with a transitionend, long before the old exit's 200ms fallback timer: that
    // transitionend is what the old exit receives first. Late in the fade (T1_LATE, timed in the page on the bubble's own opacity) the reversed fade outlasts the timer,
    // and the timer is what it receives first. Each migrated scenario states, as an expectation, which of the two endings it exercised (exit=..., see
    // FloatingBrowser.trace), so a run whose scheduling took the other path fails as such, for the unbroken scripts too, instead of passing without testing anything.
    static final List<String> T1_MOUSE = List.of("trace:tt1", "hover:#tt1", "entered", "reenter:#tt1", "settled");
    static final List<String> T1_KEYBOARD = List.of("focus:before", "trace:tt1", "key:Tab", "entered", "retab", "settled");
    static final List<String> T1_LATE = List.of("trace:tt1", "focus:tt1", "entered", "latefocus:tt1", "settled");

    static final List<Scenario> LEGACY_T1 = List.of(
            new Scenario("legacy: the pointer coming back during the exit leaves the tooltip hidden", T1_MOUSE,
                    List.of(e(2, "tip:tt1", "shown+described"), e(4, "tip:tt1", "hidden"))),
            new Scenario("legacy: focus coming back during the exit leaves the tooltip hidden", T1_KEYBOARD,
                    List.of(e(3, "tip:tt1", "shown+described"), e(5, "active", "tt1"), e(5, "tip:tt1", "hidden"))),
            new Scenario("legacy: focus coming back late in the exit leaves the tooltip hidden", T1_LATE,
                    List.of(e(2, "tip:tt1", "shown+described"), e(4, "tip:tt1", "hidden"))));

    static final List<Scenario> T1 = List.of(
            new Scenario("the pointer coming back during the exit shows the tooltip again", T1_MOUSE,
                    List.of(e(2, "tip:tt1", "shown+described"), e(4, "tip:tt1", "shown+described"), e(4, "exit", "cleared"))),
            new Scenario("focus coming back during the exit shows the tooltip again", T1_KEYBOARD,
                    List.of(e(3, "tip:tt1", "shown+described"), e(5, "active", "tt1"), e(5, "tip:tt1", "shown+described"), e(5, "exit", "cleared"))),
            new Scenario("focus coming back late in the exit shows the tooltip again, after the old exit's fallback timer", T1_LATE,
                    List.of(e(2, "tip:tt1", "shown+described"), e(4, "tip:tt1", "shown+described"), e(4, "exit", "fired/refocused"))));

    // ---------------------------------------------------------------- the stale-exit guards, each path on its own, independent of scheduling

    // The T1 scenarios exercise each ending of a stale exit with real transitions, and say which one they exercised. These pin each guard so that no scheduling can take
    // the path away. A show or an open in the same task as the hide or close (refocus, reclick-end) means the exit's fade never runs (focus() restyles the page, which
    // starts the exit transition, and the show cancels it in that task, before any frame: it ends nothing); then either nothing but the old exit's 200ms timer can end
    // the exit (refocus), or a transitionend is delivered to the element in that same task, while the old exit is still waiting and long before its timer is due, the
    // way the end of a reversed fade or of a reopened popover's entry reaches it in a real run (refocus-end, reclick-end). Each also states which ending it exercised.
    // Migrated scripts only: the legacy tooltip had no guard at all (T1), and the legacy popover only the one the P1 correction keeps.
    static final List<Scenario> STALE = List.of(
            new Scenario("a stale exit's transitionend does not hide a tooltip shown again while the exit still waits",
                    List.of("trace:tt1", "focus:tt1", "entered", "refocus-end:tt1", "settled"),
                    List.of(e(2, "tip:tt1", "shown+described"), e(3, "tip:tt1", "shown+described"), e(4, "tip:tt1", "shown+described"),
                            e(4, "exit", "cleared"))),
            new Scenario("a stale exit's fallback timer does not hide a tooltip shown again before the exit could start",
                    List.of("trace:tt1", "focus:tt1", "entered", "refocus:tt1", "settled"),
                    List.of(e(2, "tip:tt1", "shown+described"), e(3, "tip:tt1", "shown+described"), e(4, "tip:tt1", "shown+described"),
                            e(4, "exit", "fired"))),
            new Scenario("a stale close's transitionend does not hide a popover opened again while the close still waits",
                    List.of("trace:pp1", "click:#pp1", "entered", "reclick-end:pp1", "settled"),
                    List.of(e(2, "pop:pp1", "open"), e(3, "pop:pp1", "open"), e(4, "pop:pp1", "open"), e(4, "exit", "cleared"))));

    // ---------------------------------------------------------------- running

    /** Runs every scenario on a fresh load of the page and returns the failures. */
    static List<String> check(DevToolsSession cdp, String url, String where, List<Scenario> scenarios) throws Exception {
        List<String> found = new ArrayList<>();
        for (Scenario s : scenarios) {
            List<String> transcript = new ArrayList<>();
            try {
                FloatingBrowser.load(cdp, url);
                for (String action : s.actions()) {
                    FloatingBrowser.perform(cdp, action);
                    transcript.add(action + " -> " + FloatingBrowser.snapshot(cdp));
                }
            } catch (IllegalStateException e) {
                found.add(where + " | " + s.name() + " | step " + transcript.size() + " (" + s.actions().get(transcript.size()) + ") could not be performed: " + e.getMessage());
                continue;
            }
            for (OverlayScenarios.Expect x : s.expects()) {
                String actual = OverlayBrowser.field(transcript.get(x.step()), x.field());
                if (!actual.equals(x.value())) {
                    found.add(where + " | " + s.name() + " | step " + x.step() + " (" + s.actions().get(x.step()) + ") " + x.field() + ": expected " + x.value() + ", was " + actual
                            + " | " + transcript.get(x.step()));
                }
            }
        }
        return found;
    }

    // ---------------------------------------------------------------- probes

    private static String ev(DevToolsSession cdp, String js) throws Exception {
        return cdp.evaluate("(function(){try{return String(" + js + ")}catch(e){return 'ERR '+e}})()");
    }

    private static void expect(List<String> found, String where, String what, String expected, String actual) {
        if (!expected.equals(actual)) {
            found.add(where + " | " + what + ": expected " + expected + ", was " + actual);
        }
    }

    private static final String BUBBLE = "document.getElementById('et').closest('[data-tooltip]').querySelector('[data-tooltip-bubble]')";
    private static final String ARROW = "document.getElementById('et').closest('[data-tooltip]').querySelector('[data-tooltip-arrow]')";
    private static final String SURFACE = "document.getElementById('ep').closest('[data-popover]').querySelector('[data-popover-surface]')";
    private static final String TRANSLATE_RUNNING = "document.getAnimations().some(function(a){return a.transitionProperty==='translate'&&a.effect&&a.effect.target===" + SURFACE
            + "&&a.playState==='running'})";

    /**
     * The nudge, on the edge page at 1000 x 800: a Top tooltip on a trigger 60px from the left edge is moved right until it is 8px inside the viewport, and its pointer is moved back by as much,
     * so it still points at the trigger's centre; a Bottom popover on a trigger at x = 900 is moved left until it is 8px inside, without sliding there (it is placed with transitions off); when
     * the window narrows to 950 it is moved again, and that move is animated.
     */
    static List<String> nudgeProbe(DevToolsSession cdp, CascadeFixtureServer server, boolean legacyRendering, String base) throws Exception {
        List<String> found = new ArrayList<>();
        String where = "nudge";
        OverlayBrowser.viewport(cdp, 1000, 800);
        FloatingBrowser.load(cdp, server.registerRaw(FloatingBrowser.edgePage(legacyRendering, base)));
        FloatingBrowser.hover(cdp, "#et");
        Thread.sleep(400);
        expect(found, where, "the tooltip is shown", "true", ev(cdp, "getComputedStyle(" + BUBBLE + ").display!=='none'"));
        expect(found, where, "the tooltip is moved 8px inside the left edge", "true", ev(cdp, "Math.abs(" + BUBBLE + ".getBoundingClientRect().left-8)<0.5"));
        expect(found, where, "its pointer still points at the trigger", "true", ev(cdp, "(function(){var a=" + ARROW + ".getBoundingClientRect();"
                + "var t=document.getElementById('et').getBoundingClientRect();return Math.abs((a.left+a.width/2)-(t.left+t.width/2))<1})()"));
        FloatingBrowser.moveTo(cdp, 500, 790);
        OverlayBrowser.click(cdp, "#ep");
        Thread.sleep(30);
        expect(found, where, "the popover is not slid into place when it opens", "false", ev(cdp, TRANSLATE_RUNNING));
        Thread.sleep(300);
        expect(found, where, "the popover is moved 8px inside the right edge", "true", ev(cdp, "Math.abs(" + SURFACE + ".getBoundingClientRect().right-992)<0.5"));
        OverlayBrowser.viewport(cdp, 950, 800);
        Thread.sleep(40);
        expect(found, where, "the move a narrower window makes is animated", "true", ev(cdp, TRANSLATE_RUNNING));
        Thread.sleep(400);
        expect(found, where, "the popover is moved again, 8px inside the new edge", "true", ev(cdp, "Math.abs(" + SURFACE + ".getBoundingClientRect().right-942)<0.5"));
        OverlayBrowser.viewport(cdp, 1000, 800);
        return found;
    }

    /** Under prefers-reduced-motion: reduce nothing waits: an Escape hides the tooltip and closes the popover in the same task. */
    static List<String> reducedMotionProbe(DevToolsSession cdp, CascadeFixtureServer server, boolean legacyRendering, String base) throws Exception {
        List<String> found = new ArrayList<>();
        OverlayBrowser.viewport(cdp, 1000, 800);
        OverlayBrowser.reducedMotion(cdp, true);
        try {
            String url = server.registerRaw(FloatingBrowser.scenarioPage(legacyRendering, base, ""));
            String escapeThenRead = "document.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',bubbles:true}));";
            FloatingBrowser.load(cdp, url);
            OverlayBrowser.perform(cdp, "focus:tt1");
            Thread.sleep(300);
            expect(found, "reduced motion", "the tooltip is shown", "block", ev(cdp, "getComputedStyle(document.getElementById('tt1').closest('[data-tooltip]').querySelector('[data-tooltip-bubble]')).display"));
            expect(found, "reduced motion", "an Escape hides the tooltip in the same task", "none", ev(cdp, "(function(){" + escapeThenRead
                    + "return getComputedStyle(document.getElementById('tt1').closest('[data-tooltip]').querySelector('[data-tooltip-bubble]')).display})()"));
            FloatingBrowser.load(cdp, url);
            OverlayBrowser.click(cdp, "#pp1");
            Thread.sleep(200);
            expect(found, "reduced motion", "an Escape closes the popover in the same task", "none", ev(cdp, "(function(){" + escapeThenRead
                    + "return getComputedStyle(document.getElementById('pp1').closest('[data-popover]').querySelector('[data-popover-surface]')).display})()"));
            expect(found, "reduced motion", "the tooltip and the popover have no transition", "none none", ev(cdp, "[getComputedStyle(document.getElementById('tt1').closest('[data-tooltip]')"
                    + ".querySelector('[data-tooltip-bubble]')).transitionProperty,getComputedStyle(document.getElementById('pp1').closest('[data-popover]').querySelector('[data-popover-surface]'))"
                    + ".transitionProperty].join(' ')"));
        } finally {
            OverlayBrowser.reducedMotion(cdp, false);
        }
        return found;
    }

    /**
     * What the popover race (P1) did: when the surface stopped being rendered, after the last close, its opacity in the last frame it was rendered, and the outcome of each
     * close's 200ms fallback timer, in order (fired, or cleared because that close's transitionend came first; see {@link FloatingBrowser#trace}).
     */
    record Race(double hiddenMsAfterLastClose, double lastVisibleOpacity, String closeTimers) {
    }

    /**
     * The P1 sequence, timed in the page so it does not depend on the test's own latency: the popover fully open, then closed, reopened 20ms later and closed again 130ms
     * after that. The first close's fallback timer (200ms) is due part-way through the second close's fade. Every animation frame records the surface's opacity until it is
     * no longer rendered.
     */
    static Race popoverRaceProbe(DevToolsSession cdp, CascadeFixtureServer server, boolean legacyRendering, String base) throws Exception {
        OverlayBrowser.viewport(cdp, 1000, 800);
        FloatingBrowser.load(cdp, server.registerRaw(FloatingBrowser.scenarioPage(legacyRendering, base, "")));
        FloatingBrowser.trace(cdp, "pp1");
        OverlayBrowser.click(cdp, "#pp1");
        FloatingBrowser.entered(cdp);
        cdp.evaluate("(function(){var b=document.getElementById('pp1');var s=b.closest('[data-popover]').querySelector('[data-popover-surface]');window.__race={frames:[]};"
                + "function tick(){window.__race.frames.push([performance.now(),getComputedStyle(s).display==='none'?-1:+getComputedStyle(s).opacity]);"
                + "if(window.__race.frames.length<300)requestAnimationFrame(tick)}requestAnimationFrame(tick);"
                + "b.click();setTimeout(function(){b.click()},20);setTimeout(function(){window.__race.lastClose=performance.now();b.click()},150);return 'ok'})()");
        // Settled cannot hold early: the first close's timer is pending until 200ms, and the in-page 150ms timer of the last close is traced too.
        FloatingBrowser.settled(cdp);
        String r = cdp.evaluate("(function(){var f=window.__race.frames,last=window.__race.lastClose,visible=-1,hiddenAt=-1;for(var i=0;i<f.length;i++){if(f[i][0]<last)continue;"
                + "if(f[i][1]<0){hiddenAt=f[i][0];break}visible=f[i][1]}return (hiddenAt<0?-1:hiddenAt-last)+','+visible+','"
                + "+window.__ftrace.timers.filter(function(t){return t.ms===200}).map(function(t){return t.state}).join('+')})()");
        String[] p = r.split(",");
        return new Race(Double.parseDouble(p[0]), Double.parseDouble(p[1]), p[2]);
    }

    /**
     * The corrected outcome of the P1 sequence: the surface is hidden only when the second close's own fade has finished (at least 100ms after the last close, its last
     * rendered frame at most 5% opaque). The legacy one hid it when the first close's timer fired, about 50ms after the last close, part-way through the fade.
     *
     * <p>And, separately, that the run exercised the path P1 is about: the first close's fallback timer fired (nothing ended that close sooner; the reopened popover's
     * entry is cut off by the second close before it can end). A run in which it was cleared instead tested nothing about the stale timer, and fails as such.
     */
    static List<String> raceFailures(Race race) {
        List<String> found = new ArrayList<>();
        if (!race.closeTimers().startsWith("fired")) {
            found.add("race | precondition: the first close's fallback timer did not fire (its outcomes: " + race.closeTimers() + "), so the stale timer was not exercised");
        }
        if (race.hiddenMsAfterLastClose() < 100 || race.lastVisibleOpacity() < 0 || race.lastVisibleOpacity() > 0.05) {
            found.add("race | the second close's fade is cut short: hidden " + Math.round(race.hiddenMsAfterLastClose()) + "ms after the last close, in its last rendered frame "
                    + "at opacity " + race.lastVisibleOpacity());
        }
        return found;
    }

    /**
     * Showing a tooltip again during its exit, as the correction does it: the bubble is still placed, so nothing switches its transitions off or places it again (no style
     * write at all), and the reversed fade never drops to transparent on the way back.
     */
    static List<String> smoothReopenProbe(DevToolsSession cdp, CascadeFixtureServer server, String base) throws Exception {
        List<String> found = new ArrayList<>();
        OverlayBrowser.viewport(cdp, 1000, 800);
        FloatingBrowser.load(cdp, server.registerRaw(FloatingBrowser.scenarioPage(false, base, "")));
        FloatingBrowser.hover(cdp, "#tt1");
        Thread.sleep(400);
        String bubble = "document.getElementById('tt1').closest('[data-tooltip]').querySelector('[data-tooltip-bubble]')";
        FloatingBrowser.moveTo(cdp, 990, 790);
        Thread.sleep(60);
        cdp.evaluate("(function(){var s=" + bubble + ";window.__reopen={writes:0,min:1};new MutationObserver(function(r){window.__reopen.writes+=r.length})"
                + ".observe(s,{attributes:true,attributeFilter:['style']});function tick(){if(getComputedStyle(s).display!=='none')window.__reopen.min=Math.min(window.__reopen.min,"
                + "+getComputedStyle(s).opacity);if(!window.__reopen.stop)requestAnimationFrame(tick)}requestAnimationFrame(tick);return 'ok'})()");
        FloatingBrowser.hover(cdp, "#tt1");
        Thread.sleep(500);
        cdp.evaluate("(function(){window.__reopen.stop=true;return 'ok'})()");
        expect(found, "smooth reopen", "the tooltip is shown", "true", ev(cdp, "getComputedStyle(" + bubble + ").display!=='none'&&" + bubble + ".dataset.state==='open'"));
        expect(found, "smooth reopen", "no style is written on the bubble when it comes back (it is not placed again)", "0", ev(cdp, "window.__reopen.writes"));
        expect(found, "smooth reopen", "its opacity never drops to transparent on the way back", "true", ev(cdp, "window.__reopen.min>0.05"));
        return found;
    }
}
