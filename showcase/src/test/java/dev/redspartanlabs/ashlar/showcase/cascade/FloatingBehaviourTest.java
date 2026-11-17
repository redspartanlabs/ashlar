package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ACCEPTANCE: the migrated Tooltip and Popover, on the REAL scripts (served from {@code /ashlar/js/}, as the application serves them) and the migrated markup on Ashlar's own
 * stylesheet, with trusted input.
 *
 * <ul>
 *   <li>Everything the legacy scripts did that the migration keeps: the shared scenarios and probes {@link FloatingLegacyCharacterizationTest} runs on the originals (the
 *       delay, focus, Escape, touch, aria-describedby, the click, outside-click, focus-leaving and keyboard behavior of a Popover, the nudge, reduced motion, and the three
 *       behaviors kept as they were).</li>
 *   <li>The two corrections. T1: a Tooltip whose pointer or focus comes back while it is fading out is shown again, at once, from wherever its fade had got to: it is not
 *       placed again, its transitions are not switched off, and neither the old exit's transitionend nor its fallback timer hides it afterwards. P1: a Popover reopened and
 *       closed again in quick succession is hidden only when its second fade has finished. Each stale-exit guard is also pinned on its own path, independent of
 *       scheduling ({@link FloatingScenarios#STALE}).</li>
 *   <li>Inside a Modal and a Drawer, a Tooltip or a Popover still owns the first Escape, and the Modal or Drawer the second ({@link OverlayScenarios#floatingProbe}, run here
 *       on these scripts; it signals through aria-expanded and aria-describedby, which the migration keeps).</li>
 * </ul>
 */
class FloatingBehaviourTest {

    private static final List<String> SHARED_FAILURES = new ArrayList<>();
    private static final List<String> T1_FAILURES = new ArrayList<>();
    private static final List<String> STALE_FAILURES = new ArrayList<>();
    private static final List<String> RACE_FAILURES = new ArrayList<>();
    private static final List<String> OVERLAY_FAILURES = new ArrayList<>();

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("floating-behaviour");
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir());
             DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
            OverlayBrowser.viewport(cdp, 1000, 800);
            String url = server.registerRaw(FloatingBrowser.scenarioPage(false, "/ashlar/js/", ""));
            SHARED_FAILURES.addAll(FloatingScenarios.check(cdp, url, "migrated", FloatingScenarios.SHARED));
            SHARED_FAILURES.addAll(FloatingScenarios.nudgeProbe(cdp, server, false, "/ashlar/js/"));
            SHARED_FAILURES.addAll(FloatingScenarios.reducedMotionProbe(cdp, server, false, "/ashlar/js/"));
            T1_FAILURES.addAll(FloatingScenarios.check(cdp, url, "migrated", FloatingScenarios.T1));
            T1_FAILURES.addAll(FloatingScenarios.smoothReopenProbe(cdp, server, "/ashlar/js/"));
            STALE_FAILURES.addAll(FloatingScenarios.check(cdp, url, "migrated", FloatingScenarios.STALE));
            RACE_FAILURES.addAll(FloatingScenarios.raceFailures(FloatingScenarios.popoverRaceProbe(cdp, server, false, "/ashlar/js/")));
            OverlayBrowser.viewport(cdp, 900, 700);
            OVERLAY_FAILURES.addAll(OverlayScenarios.floatingProbe(cdp, server, "/ashlar/js/"));
        }
    }

    @Test
    void theMigratedScriptsKeepEverySharedBehavior() {
        assertEquals(List.of(), SHARED_FAILURES);
    }

    @Test
    void aTooltipComingBackDuringItsExitIsShownAgainSmoothly() {
        assertEquals(List.of(), T1_FAILURES);
    }

    @Test
    void aStaleExitNeverHidesATooltipOrPopoverShownAgainWhicheverOfItsEndingsArrives() {
        assertEquals(List.of(), STALE_FAILURES);
    }

    @Test
    void aPopoverReopenedAndClosedAgainFinishesItsSecondFade() {
        assertEquals(List.of(), RACE_FAILURES);
    }

    @Test
    void insideAModalOrADrawerTheFloatingComponentOwnsTheFirstEscape() {
        assertEquals(List.of(), OVERLAY_FAILURES);
    }
}
