package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ACCEPTANCE: the corrected stacking contract, on the REAL migrated scripts (served from {@code /ashlar/js/}, so modal.js and drawer.js share one overlay.js exactly as in
 * the application), with trusted input.
 *
 * <ul>
 *   <li>Scroll lock: the page stays locked until the LAST open overlay lets go, whichever closes first, and its own inline overflow (none, a shorthand, an !important longhand,
 *       two different longhands) is put back exactly, once; a release by an overlay that holds nothing never touches the page.</li>
 *   <li>Escape: only the topmost overlay is offered it, so one Escape closes one overlay; a topmost Modal that does not close on Escape closes nothing, and the overlay under it
 *       is not closed instead; an Escape that belongs to an open popup inside the topmost overlay (Popover, Tooltip, Dropdown Menu, Select, Combobox, Date Picker, each inside
 *       a Modal and a Drawer) closes only the popup, whatever order the listeners were added in.</li>
 *   <li>Focus: restored only into the topmost remaining overlay (or, with none left, exactly as before); a Modal's deferred restore never pulls focus behind a Drawer opened
 *       during its exit; closing an overlay under the topmost one leaves focus where it is; Tab brings stray focus back into the topmost overlay.</li>
 *   <li>Paint order: the overlay opened last paints on top whatever the document order, below the toast band, up to twelve deep.</li>
 *   <li>Transitions: an exit ends when every one of its transitions has finished or been cancelled, on the fallback timer read from the computed CSS when they never settle,
 *       at once when they are zero-length or reduced motion is on, and a reopen during an exit is never undone by the old exit's end.</li>
 * </ul>
 * The single-overlay scenarios run here too, so this suite alone covers the whole contract of the real scripts. {@link OverlayMutationTest} runs the same checks against
 * deliberately broken variants and requires each to fail.
 */
class OverlayStackTest {

    private static final List<String> SINGLE = new ArrayList<>();
    private static final List<String> STACKED = new ArrayList<>();
    private static final List<String> PROBES = new ArrayList<>();
    private static String confined;

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("overlay-stack");
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            String url = server.registerRaw(OverlayBrowser.scenarioPage(server, false, "/ashlar/js/", ""));
            try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                OverlayBrowser.viewport(cdp, 900, 700);
                SINGLE.addAll(OverlayScenarios.check(cdp, url, "migrated", OverlayScenarios.SINGLE));
                STACKED.addAll(OverlayScenarios.check(cdp, url, "migrated", OverlayScenarios.STACKED));
                STACKED.addAll(OverlayScenarios.check(cdp, url, "migrated", List.of(OverlayScenarios.CLOSED_BEFORE_ITS_FIRST_FRAME)));
                confined = stackingContextCase(cdp, server);
            }
            PROBES.addAll(OverlayScenarios.probes(session, server, "/ashlar/js/"));
        }
    }

    /**
     * The documented limit of any z-index order: a Drawer rendered inside an element that makes its own stacking context (here a position: relative, z-index: 20 wrapper,
     * the shape of the showcase's own navbar wrapper) is drawn within that element's level, so a Modal at page level paints over it even when the Drawer was opened last.
     */
    private static String stackingContextCase(DevToolsSession cdp, CascadeFixtureServer server) throws Exception {
        String body = "<button id=\"tm\" type=\"button\" data-modal-open=\"pm\">open</button>"
                + "<div id=\"wrap\" style=\"position:relative;z-index:20\">" + OverlayBrowser.drawer("wd", "Wrapped drawer", null, "<p>inside</p>", null) + "</div>"
                + OverlayBrowser.modal("pm", "Page modal", OverlayBrowser.o(), "<button id=\"pmopen\" type=\"button\" data-drawer-open=\"wd\">open the wrapped drawer</button>", null);
        cdp.navigate(server.registerRaw(OverlayBrowser.page(List.of(ParityFrames.ASHLAR), "light", body, "")));
        OverlayBrowser.ready(cdp);
        OverlayBrowser.click(cdp, "#tm");
        OverlayBrowser.click(cdp, "#pmopen");
        Thread.sleep(300);
        return cdp.evaluate("(function(){var d=document.getElementById('wd');var p=document.elementFromPoint(innerWidth-40,innerHeight/2).closest('[data-modal],[data-drawer]');"
                + "return 'drawer z=' + getComputedStyle(d).zIndex + ' shown=' + (getComputedStyle(d).display!=='none') + ' painted at its panel=' + (p?p.id:'page')})()");
    }

    @Test
    void oneOpenOverlayBehavesAsDocumented() {
        assertEquals(List.of(), SINGLE);
    }

    @Test
    void stackedOverlaysShareTheScrollLockTheKeyboardAndThePaintOrderAsSpecified() {
        assertEquals(List.of(), STACKED);
    }

    @Test
    void transitionsTheScrollLockDepthAndNestedPopupsMeetTheContract() {
        assertEquals(List.of(), PROBES);
    }

    @Test
    void anOverlayInsideAnotherStackingContextIsDrawnWithinThatContextAsDocumented() {
        // Opened last, so it is above the modal in the stack (offset 1, z-index 41), but its wrapper is a z-index 20 stacking context, so the page-level modal paints over it.
        assertEquals("drawer z=41 shown=true painted at its panel=pm", confined);
    }
}
