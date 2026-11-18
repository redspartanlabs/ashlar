package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ten showcase skins reach Dropdown Menu through its stable data hooks and Tailwind's own variables, and through ONE new rule each. One unlayered rule per skin sets the
 * box-shadow of {@code [data-dropdown-menu-list]} (with the other floating surfaces); civic restyles {@code [data-dropdown-menu-trigger]} in its masthead and marks a
 * {@code [data-dropdown-menu-item][aria-current]}; summit sets {@code [data-dropdown-menu-item]}'s weight; every skin sets {@code --radius-md} and recolors the palette. And
 * every skin flattened every Tailwind shadow utility ({@code :is(.shadow-xs, .shadow-sm, ...) { --tw-shadow: 0 0 #0000 }}), the trigger's shadow-sm among them: the trigger
 * now owns that shadow in {@code --ash-dropdown-menu-trigger-shadow}, so each skin carries one rule that flattens it, the same pattern Button's and Toggle's migrations
 * added. This suite reads that rule in every skin's source, and shows the migrated component still answers to exactly those hooks:
 *
 * <ul>
 *   <li>under each skin, the open menu's computed box-shadow is the skin's, read from a bare probe element that carries only the hook, and differs from the component's own
 *       default; the trigger has no drop shadow (only a focus ring, when focused);</li>
 *   <li>with no skin, the menu wears its 1px ring over its shadow and the trigger its shadow, in both themes;</li>
 *   <li>the trigger's and the menu's radius are the skin's --radius-md.</li>
 * </ul>
 * {@link DropdownMenuParityTest} already requires the full computed style of every part to equal the legacy one under every skin; this suite names the contract explicitly.
 */
class DropdownMenuSkinsTest {

    private static final Map<String, String> RESULTS = new LinkedHashMap<>();
    private static final String TRIGGER_DEFAULT = "rgba(0, 0, 0, 0.1) 0px 1px 3px 0px, rgba(0, 0, 0, 0.1) 0px 1px 2px -1px";

    private static String skin(String name) throws Exception {
        try (InputStream in = DropdownMenuSkinsTest.class.getResourceAsStream("/static/css/theme-" + name + ".css")) {
            assertNotNull(in, "theme-" + name + ".css is on the classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }

    private static int count(String text, String part) {
        return text.split(java.util.regex.Pattern.quote(part), -1).length - 1;
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("dropdown-menu-skins");
        String open = OverlayBrowser.withoutScripts(DropdownMenuBrowser.inState(DropdownMenuBrowser.menu("Actions", null, DropdownMenuBrowser.ACTIONS), true, true));
        String body = "<div style=\"padding:20px 40px 300px 40px\">" + open + "</div><div id=\"probe-list\" data-dropdown-menu-list></div>"
                + "<div id=\"probe-radius\" style=\"border-radius:var(--radius-md)\"></div>";
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir());
             DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
            for (ParityConfigs.Config config : ParityConfigs.all()) {
                cdp.navigate(server.registerRaw(DropdownMenuBrowser.page(List.of(ParityFrames.APP), config, "", body)));
                RESULTS.put(config.id(), cdp.evaluate("(function(){function s(q,p){return getComputedStyle(document.querySelector(q)).getPropertyValue(p)}"
                        + "return [s('[data-dropdown-menu-list]:not(#probe-list)','box-shadow'),s('#probe-list','box-shadow'),s('[data-dropdown-menu-trigger]','box-shadow'),"
                        + "s('[data-dropdown-menu-trigger]','border-top-left-radius'),s('[data-dropdown-menu-list]:not(#probe-list)','border-top-left-radius'),"
                        + "s('#probe-radius','border-top-left-radius')].join(' || ')})()"));
            }
        }
    }

    @Test
    void everySkinFlattensTheTriggersShadowWithOneRuleAndNamesTheComponentNowhereElse() throws Exception {
        for (String name : ParityConfigs.SKINS) {
            String css = skin(name);
            String rule = "[data-skin=\"" + name + "\"] .ash-dropdown-menu__trigger {\n    --ash-dropdown-menu-trigger-shadow: 0 0 #0000;\n}\n";
            assertEquals(1, count(css, rule), name + ": the trigger's flat shadow, once");
            assertEquals(3, count(css, "ash-dropdown-menu"), name + ": named only by that rule (its selector and declaration) and its comment");
            assertTrue(css.contains(":is(.shadow-xs, .shadow-sm, .shadow, .shadow-md, .shadow-lg, .shadow-xl, .shadow-2xl)") || css.contains(".shadow-sm"),
                    name + ": the rule against Tailwind's shadow classes that the new rule stands in for is still there");
            assertTrue(css.contains("[data-dropdown-menu-list]"), name + ": the menu's hook is still what the skin's box-shadow targets");
        }
    }

    @Test
    void underEverySkinTheMenuWearsTheSkinsShadowAndTheTriggerNone() {
        for (String skin : ParityConfigs.SKINS) {
            for (String theme : List.of("light", "dark")) {
                String[] r = RESULTS.get(theme + "/" + skin).split(" \\|\\| ");
                assertEquals(r[1], r[0], theme + "/" + skin + ": the menu's box-shadow is the skin's (the hook's)");
                assertNotEquals(RESULTS.get(theme + "/none").split(" \\|\\| ")[0], r[0], theme + "/" + skin + ": and not the component's default");
                assertEquals("none", TextEntryBrowser.visibleShadow(r[2]), theme + "/" + skin + ": the trigger's drop shadow is flattened, as shadow-sm was");
            }
        }
    }

    @Test
    void withNoSkinTheMenuWearsItsRingOverItsShadowAndTheTriggerItsShadow() {
        for (String theme : List.of("light", "dark")) {
            String[] r = RESULTS.get(theme + "/none").split(" \\|\\| ");
            assertTrue(r[0].contains(" 0px 0px 0px 1px") && r[0].contains("0px 10px 15px -3px"), theme + ": a 1px ring, then the shadow: " + r[0]);
            assertEquals(TRIGGER_DEFAULT, TextEntryBrowser.visibleShadow(r[2]), theme + ": the trigger's shadow");
        }
    }

    @Test
    void theTriggersAndTheMenusRadiusIsTheSkinsRadiusMd() {
        RESULTS.forEach((config, value) -> {
            String[] r = value.split(" \\|\\| ");
            assertEquals(r[5], r[3], config + ": the trigger's radius");
            assertEquals(r[5], r[4], config + ": the menu's radius");
        });
    }
}
