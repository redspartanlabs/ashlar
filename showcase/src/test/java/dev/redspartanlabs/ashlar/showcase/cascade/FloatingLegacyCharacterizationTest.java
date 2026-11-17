package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LEGACY CHARACTERIZATION: what the original Tooltip and Popover scripts did, recorded, not endorsed. The original tooltip.js and popover.js (the fixtures
 * {@code /parity/floating/tooltip-legacy.js} and {@code popover-legacy.js}, verbatim copies of the scripts at commit bc49524) run on the legacy Tailwind markup, with trusted
 * input.
 *
 * <p>Two kinds of result are recorded. The shared scenarios and probes are the behavior the migration keeps, and {@link FloatingBehaviourTest} requires the migrated scripts
 * to pass the same ones; they include three behaviors kept as they were without being endorsed as a future contract (one Escape closes a nested Popover and the one around
 * it; one Escape hides a Tooltip inside a Popover and closes the Popover; a trigger whose content has tabindex="-1" is skipped by Tab). The other kind records the two defects
 * the migration corrects:
 *
 * <ul>
 *   <li>T1: a Tooltip whose pointer or focus came back while it was fading out stayed hidden, because the script read its state back off the hidden class, which is only
 *       added once the fade has finished;</li>
 *   <li>P1: a Popover reopened and closed again in quick succession was hidden part-way through its second fade, when the FIRST close's fallback timer fired.</li>
 * </ul>
 */
class FloatingLegacyCharacterizationTest {

    private static final String MARKER = "// ==== END OF PROVENANCE HEADER (the original file follows byte for byte) ====\n";
    private static final Map<String, String[]> ORIGINALS = Map.of(
            "tooltip", new String[] {"2281d46858c0291271cd295d9d3be3f6d47f6af0", "9a6fc769c82c8357e41ccf83bb526a8f5f6b19163f286e05dbf7e96b481d10d6"},
            "popover", new String[] {"a301e0b50c038602851fee6743c242a78e00f929", "23420579b20ece75772f43c72c872f8dee5128f6b51ed2470c3d8b3540bc8e09"});

    private static final List<String> SHARED_FAILURES = new ArrayList<>();
    private static final List<String> T1_FAILURES = new ArrayList<>();
    private static FloatingScenarios.Race race;

    /** The original script below the provenance header (line endings normalized: a Windows checkout writes CRLF, the original is LF). */
    static String original(String name) throws Exception {
        String fixture = OverlayBrowser.script("/parity/floating/" + name + "-legacy.js");
        int marker = fixture.indexOf(MARKER);
        if (marker < 0) {
            throw new IllegalStateException(name + "-legacy.js has no provenance marker");
        }
        return fixture.substring(marker + MARKER.length());
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("floating-legacy-characterization");
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir());
             DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
            OverlayBrowser.viewport(cdp, 1000, 800);
            String scripts = server.registerScripts(Map.of("tooltip.js", original("tooltip"), "popover.js", original("popover")));
            String url = server.registerRaw(FloatingBrowser.scenarioPage(true, scripts, ""));
            SHARED_FAILURES.addAll(FloatingScenarios.check(cdp, url, "legacy", FloatingScenarios.SHARED));
            SHARED_FAILURES.addAll(FloatingScenarios.nudgeProbe(cdp, server, true, scripts));
            SHARED_FAILURES.addAll(FloatingScenarios.reducedMotionProbe(cdp, server, true, scripts));
            T1_FAILURES.addAll(FloatingScenarios.check(cdp, url, "legacy", FloatingScenarios.LEGACY_T1));
            race = FloatingScenarios.popoverRaceProbe(cdp, server, true, scripts);
        }
    }

    @Test
    void theLegacyScriptFixturesAreTheOriginalsByteForByte() throws Exception {
        for (Map.Entry<String, String[]> e : ORIGINALS.entrySet()) {
            String fixture = OverlayBrowser.script("/parity/floating/" + e.getKey() + "-legacy.js");
            String header = fixture.substring(0, fixture.indexOf(MARKER));
            assertTrue(header.contains("bc49524") && header.contains(e.getValue()[0]) && header.contains(e.getValue()[1]), e.getKey() + ": the header names the commit, the blob and the hash");
            assertEquals(e.getValue()[1], HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(original(e.getKey()).getBytes(StandardCharsets.UTF_8))),
                    e.getKey() + ": the copy below the marker was edited");
        }
        assertTrue(original("tooltip").contains("return !bubble.classList.contains(\"hidden\");"), "it is the pre-migration tooltip.js: it read its state back off the hidden class");
        assertTrue(original("popover").contains("surface.classList.add(\"hidden\");") && original("popover").contains("if (!opened) {"),
                "it is the pre-migration popover.js: it toggled the hidden class, and its close guarded only against a reopen");
    }

    @Test
    void theLegacyScriptsBehaveAsTheSharedScenariosDescribe() {
        assertEquals(List.of(), SHARED_FAILURES);
    }

    @Test
    void theLegacyTooltipStayedHiddenWhenItsPointerOrFocusCameBackDuringItsExit() {
        assertEquals(List.of(), T1_FAILURES, "the recorded legacy defect (T1) no longer reproduces");
    }

    @Test
    void theLegacyPopoverWasHiddenPartWayThroughItsSecondFadeByTheFirstClosesTimer() {
        // The first close's 200ms timer is due 50ms after the last close; the second fade then still has most of its 150ms to run.
        assertTrue(race.hiddenMsAfterLastClose() > 20 && race.hiddenMsAfterLastClose() < 100 && race.lastVisibleOpacity() > 0.2,
                "the recorded legacy defect (P1) no longer reproduces: " + race);
        assertEquals(1, FloatingScenarios.raceFailures(race).size(), "the corrected expectation fails on the legacy script");
    }
}
