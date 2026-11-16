package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Single-overlay behaviour parity: the SAME trusted input ({@link OverlayScenarios#SINGLE}) runs against (a) the legacy Tailwind markup with the original modal.js and
 * drawer.js and (b) the migrated markup with the migrated scripts, and the observable result must be identical after every step: which overlays are shown, the page's inline
 * overflow, which element has focus, and which overlay paints on top. That is the proof that the migration kept what one open overlay does: when focus goes back (a Modal
 * after its exit, a Drawer at once), when the scroll lock is released, the Tab trap (a Modal wraps; a Drawer also pulls stray focus back), Escape and backdrop dismissal and
 * their settings, a Modal ignoring its opener during its exit, a Drawer reopening during its exit, and that opening or closing twice is the same as once.
 *
 * <p>Each rendering runs in its own fresh browser, one after the other, so both start from identical conditions. The class and attribute changes themselves differ BY DESIGN
 * (the legacy scripts toggled Tailwind classes, the migrated ones set hidden and data-state) and are not part of the snapshot.
 */
class OverlayBehaviourParityTest {

    private static final List<List<String>> LEGACY = new ArrayList<>();
    private static final List<List<String>> MIGRATED = new ArrayList<>();
    private static final List<String> MIGRATED_FAILURES = new ArrayList<>();

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("overlay-behaviour-parity");
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            String legacyUrl = server.registerRaw(OverlayBrowser.scenarioPage(server, true, OverlayLegacyCharacterizationTest.legacyScripts(server), ""));
            String migratedUrl = server.registerRaw(OverlayBrowser.scenarioPage(server, false, "/ashlar/js/", ""));
            try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                OverlayBrowser.viewport(cdp, 900, 700);
                for (OverlayScenarios.Scenario s : OverlayScenarios.SINGLE) {
                    LEGACY.add(OverlayScenarios.run(cdp, legacyUrl, s));
                }
            }
            try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                OverlayBrowser.viewport(cdp, 900, 700);
                for (OverlayScenarios.Scenario s : OverlayScenarios.SINGLE) {
                    MIGRATED.add(OverlayScenarios.run(cdp, migratedUrl, s));
                }
                MIGRATED_FAILURES.addAll(OverlayScenarios.check(cdp, migratedUrl, "migrated", OverlayScenarios.SINGLE));
            }
        }
    }

    @Test
    void everySingleOverlayStepEndsTheSameWithTheLegacyAndTheMigratedScripts() {
        assertEquals(OverlayScenarios.SINGLE.size(), LEGACY.size());
        List<String> differences = new ArrayList<>();
        for (int i = 0; i < LEGACY.size(); i++) {
            List<String> a = LEGACY.get(i);
            List<String> b = MIGRATED.get(i);
            for (int step = 0; step < Math.max(a.size(), b.size()); step++) {
                String x = step < a.size() ? a.get(step) : "(missing)";
                String y = step < b.size() ? b.get(step) : "(missing)";
                if (!x.equals(y)) {
                    differences.add(OverlayScenarios.SINGLE.get(i).name() + " | step " + step + " | legacy " + x + " | migrated " + y);
                }
            }
        }
        assertEquals(List.of(), differences);
    }

    @Test
    void theMigratedScriptsMeetEverySingleOverlayExpectation() {
        assertEquals(List.of(), MIGRATED_FAILURES);
    }

    @Test
    void theComparisonIsNotVacuous() {
        int steps = LEGACY.stream().mapToInt(List::size).sum();
        assertTrue(steps >= 40, "expected at least 40 compared steps, got " + steps);
        assertTrue(LEGACY.stream().flatMap(List::stream).anyMatch(l -> l.contains("m1=shown")) && LEGACY.stream().flatMap(List::stream).anyMatch(l -> l.contains("d1=shown"))
                && LEGACY.stream().flatMap(List::stream).anyMatch(l -> l.contains("overflow=hidden")), "the scenarios really opened overlays and locked the page");
    }
}
