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
 * LEGACY CHARACTERIZATION: what the original Modal and Drawer scripts did, recorded, not endorsed. The original modal.js and drawer.js (the fixtures
 * {@code /parity/overlay/modal-legacy.js} and {@code drawer-legacy.js}, verbatim copies of the scripts at commit b4418e2) run on the legacy Tailwind markup, with the trusted
 * input of {@link OverlayScenarios}.
 *
 * <p>Two kinds of result are recorded. The single-overlay scenarios are the behaviour the migration must keep, and {@link OverlayBehaviourParityTest} requires the migrated
 * scripts to match them step by step. The stacked scenarios record the defects the migration corrects: closing a Drawer under a Modal unlocked the page early and closing the
 * Modal then left it locked for good; a Drawer opened from a Modal painted underneath it; one Escape closed every open overlay (two stacked Modals too, after which the page's
 * overflow depended on which exit ended last). It also records a Modal closed before its first frame: nothing transitioned, so it stayed mounted, invisible, for its blind
 * 250ms fallback. Those
 * are NOT the new contract (that is {@link OverlayStackTest}); they are kept here so the record of what changed, and why, is executable.
 */
class OverlayLegacyCharacterizationTest {

    private static final String MARKER = "// ==== END OF PROVENANCE HEADER (the original file follows byte for byte) ====\n";
    private static final Map<String, String[]> ORIGINALS = Map.of(
            "modal", new String[] {"149c6744b0999a187f0b0ec281846c6d42624555", "4058b2dd51787491b6178a4a99d539fb3e951b9a2a12e720910cf520395e9b10"},
            "drawer", new String[] {"b575b7d631102c57e41331daa7c4e5f200dff399", "129194b23931d6fc5e5ff2644f1d987851b701c9f2820a68d83729ba030aeb35"});

    private static final List<String> SINGLE_FAILURES = new ArrayList<>();
    private static final List<String> STACKED_FAILURES = new ArrayList<>();
    private static final List<String> FIRST_FRAME_FAILURES = new ArrayList<>();

    /** The original script below the provenance header (line endings normalised: a Windows checkout writes CRLF, the original is LF). */
    static String original(String name) throws Exception {
        String fixture = OverlayBrowser.script("/parity/overlay/" + name + "-legacy.js");
        int marker = fixture.indexOf(MARKER);
        if (marker < 0) {
            throw new IllegalStateException(name + "-legacy.js has no provenance marker");
        }
        return fixture.substring(marker + MARKER.length());
    }

    /** The legacy script set: the two originals, served side by side. */
    static String legacyScripts(CascadeFixtureServer server) throws Exception {
        return server.registerScripts(Map.of("modal.js", original("modal"), "drawer.js", original("drawer")));
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("overlay-legacy-characterization");
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir());
             DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
            OverlayBrowser.viewport(cdp, 900, 700);
            String url = server.registerRaw(OverlayBrowser.scenarioPage(server, true, legacyScripts(server), ""));
            SINGLE_FAILURES.addAll(OverlayScenarios.check(cdp, url, "legacy", OverlayScenarios.SINGLE));
            STACKED_FAILURES.addAll(OverlayScenarios.check(cdp, url, "legacy", OverlayScenarios.LEGACY_STACKED));
            FIRST_FRAME_FAILURES.addAll(OverlayScenarios.check(cdp, url, "legacy", List.of(OverlayScenarios.LEGACY_CLOSED_BEFORE_FIRST_FRAME)));
        }
    }

    @Test
    void theLegacyScriptFixturesAreTheOriginalsByteForByte() throws Exception {
        for (Map.Entry<String, String[]> e : ORIGINALS.entrySet()) {
            String fixture = OverlayBrowser.script("/parity/overlay/" + e.getKey() + "-legacy.js");
            String header = fixture.substring(0, fixture.indexOf(MARKER));
            assertTrue(header.contains("b4418e2") && header.contains(e.getValue()[0]) && header.contains(e.getValue()[1]), e.getKey() + ": the header names the commit, the blob and the hash");
            assertEquals(e.getValue()[1], HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(original(e.getKey()).getBytes(StandardCharsets.UTF_8))),
                    e.getKey() + ": the copy below the marker was edited");
        }
        assertTrue(original("modal").contains("root.classList.contains(\"hidden\")") && original("modal").contains("previousBodyOverflow"),
                "it is the pre-migration modal.js: it read the hidden class and saved and restored the body's overflow on its own");
        assertTrue(original("drawer").contains("data-drawer-closed-class") || original("drawer").contains("drawerClosedClass"), "it is the pre-migration drawer.js: it read the closed class from the template");
    }

    @Test
    void theLegacyScriptsBehaveAsDocumentedWithOneOverlayOpen() {
        assertEquals(List.of(), SINGLE_FAILURES);
    }

    @Test
    void theLegacyModalClosedBeforeItsFirstFrameStayedMountedForItsBlindFallback() {
        assertEquals(List.of(), FIRST_FRAME_FAILURES);
    }

    @Test
    void theLegacyScriptsHadTheStackingDefectsThisMigrationCorrects() {
        assertEquals(List.of(), STACKED_FAILURES, "the recorded legacy stacking behaviour (defects included) no longer reproduces");
    }
}
