package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The ten showcase skins reach Modal and Drawer only through stable data hooks: one unlayered rule per skin sets the box-shadow of {@code [data-modal-dialog]} and
 * {@code [data-drawer-panel]} (with the other floating surfaces), and Summit also styles an h3 inside either. No skin selector names a Modal or Drawer class. This suite shows
 * the migrated components still answer to exactly those rules:
 *
 * <ul>
 *   <li>under each skin, the open dialog's and panel's computed box-shadow is the skin's, read from a bare probe element that carries only the hook (so only the skin's rule can
 *       style it), and it differs from the component's own default;</li>
 *   <li>with no skin, the shadow is the component's own default (Tailwind's shadow-xl), in both themes;</li>
 *   <li>under Summit, an h3 in a dialog or a panel body takes the skin's weight.</li>
 * </ul>
 * {@link OverlayParityTest} already requires the full computed style of every part to equal the legacy one under every skin; this suite names the hook contract explicitly.
 */
class OverlaySkinsTest {

    private static final Map<String, String> RESULTS = new LinkedHashMap<>();
    private static final String DEFAULT_SHADOW = "rgba(0, 0, 0, 0.1) 0px 20px 25px -5px, rgba(0, 0, 0, 0.1) 0px 8px 10px -6px";

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("overlay-skins");
        String body = OverlayBrowser.withoutScripts(OverlayBrowser.inState(OverlayBrowser.modal("sm", "Skinned", OverlayBrowser.o(), "<h3 id=\"smh3\">Section</h3><p>Body</p>", null), true, true))
                + OverlayBrowser.withoutScripts(OverlayBrowser.inState(OverlayBrowser.drawer("sd", "Skinned", null, "<h3 id=\"sdh3\">Section</h3><p>Body</p>", null), true, true))
                + "<div id=\"probe-dialog\" data-modal-dialog></div><div id=\"probe-panel\" data-drawer-panel></div>";
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir());
             DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
            for (ParityConfigs.Config config : ParityConfigs.all()) {
                cdp.navigate(server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.APP), config, "", body, "")));
                RESULTS.put(config.id(), cdp.evaluate("(function(){function bs(s){return getComputedStyle(document.querySelector(s)).boxShadow}"
                        + "return [bs('#sm [data-modal-dialog]'),bs('#probe-dialog'),bs('#sd [data-drawer-panel]'),bs('#probe-panel'),"
                        + "getComputedStyle(document.getElementById('smh3')).fontWeight,getComputedStyle(document.getElementById('sdh3')).fontWeight].join(' || ')})()"));
            }
        }
    }

    private static String[] parts(String config) {
        return RESULTS.get(config).split(" \\|\\| ");
    }

    @Test
    void underEverySkinTheDialogAndThePanelWearTheSkinsShadowFromItsHookRule() {
        for (ParityConfigs.Config config : ParityConfigs.all()) {
            if (config.skin() == null) {
                continue;
            }
            String[] p = parts(config.id());
            assertEquals(TextEntryBrowser.visibleShadow(p[1]), TextEntryBrowser.visibleShadow(p[0]), config.id() + ": the dialog's shadow is the skin's");
            assertEquals(TextEntryBrowser.visibleShadow(p[3]), TextEntryBrowser.visibleShadow(p[2]), config.id() + ": the panel's shadow is the skin's");
            assertNotEquals(DEFAULT_SHADOW, TextEntryBrowser.visibleShadow(p[0]), config.id() + ": the skin replaces the default");
            assertNotEquals("none", TextEntryBrowser.visibleShadow(p[1]), config.id() + ": the skin's hook rule applies to a bare hook");
        }
    }

    @Test
    void withNoSkinTheDialogAndThePanelWearTheirOwnDefaultShadow() {
        for (String id : List.of("light/none", "dark/none")) {
            String[] p = parts(id);
            assertEquals(DEFAULT_SHADOW, TextEntryBrowser.visibleShadow(p[0]), id + " dialog");
            assertEquals(DEFAULT_SHADOW, TextEntryBrowser.visibleShadow(p[2]), id + " panel");
            assertEquals("none", TextEntryBrowser.visibleShadow(p[1]), id + ": no skin, so a bare hook has no shadow");
        }
    }

    @Test
    void summitStylesAnH3InADialogOrAPanelBodyThroughTheHooks() {
        for (String id : List.of("light/summit", "dark/summit")) {
            String[] p = parts(id);
            assertEquals("700", p[4], id + " dialog h3");
            assertEquals("700", p[5], id + " panel h3");
        }
        for (String id : List.of("light/none", "light/foundry", "dark/harbor")) {
            assertNotEquals("700", parts(id)[4], id + ": the h3 weight comes from Summit's rule alone");
        }
    }
}
