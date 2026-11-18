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
 * CHARACTERIZATION: the ORIGINAL dropdown-menu.js (a byte-for-byte copy of the pre-migration script, provenance-checked below) on the legacy Tailwind markup, with trusted
 * input, next to the page's other components as they are (Modal, Drawer and Popover were already Ashlar's at that commit, and run their real scripts here). It records what
 * the component did, so the migrated script can be required to do the same ({@link DropdownMenuBehaviourTest}) wherever the migration does not deliberately correct it:
 * every shared scenario (S2, S5 and the overlay defect S6 included, kept as they were), and the three defects the migration corrects, which this suite pins as they were.
 */
class DropdownMenuLegacyCharacterizationTest {

    static final String MARKER = "// ==== END OF PROVENANCE HEADER (the original file follows byte for byte) ====\n";
    static final String BLOB = "bfec09793cbf59d712ab3cb59c27cb2678abab92";
    static final String SHA256 = "b2843e34158cd623e98d2196703a37da9a12525c06145761980239b8e26114d4";

    private static final List<String> SHARED_FAILURES = new ArrayList<>();
    private static final List<String> DEFECT_FAILURES = new ArrayList<>();
    private static final List<String> MOTION_FAILURES = new ArrayList<>();

    /** The original script below the provenance header (line endings normalized: a Windows checkout writes CRLF, the original is LF). */
    static String original() throws Exception {
        String fixture = OverlayBrowser.script("/parity/dropdown-menu/dropdown-menu-legacy.js");
        int marker = fixture.indexOf(MARKER);
        if (marker < 0) {
            throw new IllegalStateException("dropdown-menu-legacy.js has no provenance marker");
        }
        return fixture.substring(marker + MARKER.length());
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("dropdown-menu-legacy-characterization");
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir());
             DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
            OverlayBrowser.viewport(cdp, 1000, 800);
            String scripts = server.registerScriptsOverAshlar(Map.of("dropdown-menu.js", original()));
            String url = server.registerRaw(DropdownMenuBrowser.scenarioPage(true, scripts));
            SHARED_FAILURES.addAll(DropdownMenuScenarios.check(cdp, url, "legacy", DropdownMenuScenarios.SHARED));
            DEFECT_FAILURES.addAll(DropdownMenuScenarios.check(cdp, url, "legacy", DropdownMenuScenarios.LEGACY_FIXED));
            MOTION_FAILURES.addAll(DropdownMenuScenarios.reducedMotionProbe(cdp, server, true, scripts));
        }
    }

    @Test
    void theLegacyScriptFixtureIsTheOriginalByteForByte() throws Exception {
        String fixture = OverlayBrowser.script("/parity/dropdown-menu/dropdown-menu-legacy.js");
        String header = fixture.substring(0, fixture.indexOf(MARKER));
        assertTrue(header.contains("8af8ae5") && header.contains(BLOB) && header.contains(SHA256), "the header names the commit, the blob and the hash");
        assertEquals(SHA256, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(original().getBytes(StandardCharsets.UTF_8))), "the copy below the marker was edited");
        assertTrue(original().contains("return !menu.classList.contains(\"hidden\");") && original().contains("let isClosing = false;"),
                "it is the pre-migration dropdown-menu.js: it read its state back off the hidden class");
    }

    @Test
    void theLegacyScriptBehavesAsTheSharedScenariosDescribe() {
        assertEquals(List.of(), SHARED_FAILURES);
    }

    @Test
    void theLegacyScriptDroppedInputDuringItsExitKeptAMenuOpenAfterTabAndConsumedASecondEscape() {
        assertEquals(List.of(), DEFECT_FAILURES, "the recorded legacy defects (S1, S3, S4) no longer reproduce");
    }

    @Test
    void underReducedMotionTheLegacyMenuOpenedAndClosedAtOnce() {
        assertEquals(List.of(), MOTION_FAILURES);
    }
}
