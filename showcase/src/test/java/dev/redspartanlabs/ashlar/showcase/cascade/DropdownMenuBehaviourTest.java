package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ACCEPTANCE: the migrated Dropdown Menu, on the REAL script (served from {@code /ashlar/js/}, as the application serves it) and the migrated markup on Ashlar's own
 * stylesheet, with trusted input.
 *
 * <ul>
 *   <li>Everything the legacy script did that the migration keeps: the shared scenarios {@link DropdownMenuLegacyCharacterizationTest} runs on the original (opening,
 *       the keyboard model, activation, the disabled entry, outside clicks, several menus, Tab from an entry, Escape ownership inside a Modal, a Drawer and a Popover, and
 *       the three behaviors kept as they were: S2, S5, and the overlay defect S6), and reduced motion.</li>
 *   <li>The three corrections. S1: a click, Enter, Space or ArrowDown on the trigger while the menu fades out reopens it, from wherever its fade had got to, and neither the
 *       old close's transitionend nor its fallback timer hides it afterwards. S3: Tab or Shift+Tab from the trigger of an open menu closes it, without moving focus. S4: a
 *       second Escape while the menu fades out reaches the Modal, Drawer or Popover around it. Each stale-close guard is also pinned on its own path, independent of
 *       scheduling ({@link DropdownMenuScenarios#STALE}).</li>
 *   <li>Inside a Modal and a Drawer, a Dropdown Menu (like every popup) still owns the first Escape ({@link OverlayScenarios#floatingProbe}, run here on these scripts; it
 *       signals through aria-haspopup and aria-expanded, which the migration keeps).</li>
 * </ul>
 */
class DropdownMenuBehaviourTest {

    private static final List<String> SHARED_FAILURES = new ArrayList<>();
    private static final List<String> FIXED_FAILURES = new ArrayList<>();
    private static final List<String> STALE_FAILURES = new ArrayList<>();
    private static final List<String> OVERLAY_FAILURES = new ArrayList<>();

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("dropdown-menu-behaviour");
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir());
             DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
            OverlayBrowser.viewport(cdp, 1000, 800);
            String url = server.registerRaw(DropdownMenuBrowser.scenarioPage(false, "/ashlar/js/"));
            SHARED_FAILURES.addAll(DropdownMenuScenarios.check(cdp, url, "migrated", DropdownMenuScenarios.SHARED));
            SHARED_FAILURES.addAll(DropdownMenuScenarios.reducedMotionProbe(cdp, server, false, "/ashlar/js/"));
            FIXED_FAILURES.addAll(DropdownMenuScenarios.check(cdp, url, "migrated", DropdownMenuScenarios.FIXED));
            FIXED_FAILURES.addAll(DropdownMenuScenarios.smoothReopenProbe(cdp, server, "/ashlar/js/"));
            STALE_FAILURES.addAll(DropdownMenuScenarios.check(cdp, url, "migrated", DropdownMenuScenarios.STALE));
            OverlayBrowser.viewport(cdp, 900, 700);
            OVERLAY_FAILURES.addAll(OverlayScenarios.floatingProbe(cdp, server, "/ashlar/js/"));
        }
    }

    @Test
    void theMigratedScriptKeepsEverySharedBehavior() {
        assertEquals(List.of(), SHARED_FAILURES);
    }

    @Test
    void inputDuringTheExitReopensTabClosesAnOpenMenuAndASecondEscapeReachesTheOverlay() {
        assertEquals(List.of(), FIXED_FAILURES);
    }

    @Test
    void aStaleCloseNeverHidesAReopenedMenuWhicheverOfItsEndingsArrives() {
        assertEquals(List.of(), STALE_FAILURES);
    }

    @Test
    void insideAModalOrADrawerTheDropdownMenuOwnsTheFirstEscape() {
        assertEquals(List.of(), OVERLAY_FAILURES);
    }
}
