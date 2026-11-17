package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ten showcase skins reach Tooltip and Popover only through stable data hooks and Tailwind's own variables: one unlayered rule per skin sets the box-shadow of
 * {@code [data-tooltip-bubble]} and {@code [data-popover-surface]} (with the other floating surfaces), every skin sets {@code --radius-md} and {@code --radius-lg} and
 * recolors the palette, and every skin styles headings, so the Popover's h3 title. No skin selector names a Tooltip or Popover class. This suite shows the migrated components
 * still answer to exactly those:
 *
 * <ul>
 *   <li>under each skin, the open bubble's and surface's computed box-shadow is the skin's, read from a bare probe element that carries only the hook, and it differs from the
 *       component's own default. For the Popover that means the skin's shadow replaces the ring as well, exactly as it replaced Tailwind's ring-1 and shadow-lg, which were
 *       layers of the same box-shadow;</li>
 *   <li>with no skin, the bubble wears its default shadow and the surface its 1px ring over its shadow, in both themes;</li>
 *   <li>the bubble's and the surface's radius are the skin's --radius-md and --radius-lg.</li>
 * </ul>
 * {@link FloatingParityTest} already requires the full computed style of every part (the title's heading font among it) to equal the legacy one under every skin; this suite
 * names the hook contract explicitly.
 */
class FloatingSkinsTest {

    private static final Map<String, String> RESULTS = new LinkedHashMap<>();
    private static final String TOOLTIP_DEFAULT = "rgba(0, 0, 0, 0.1) 0px 10px 15px -3px, rgba(0, 0, 0, 0.1) 0px 4px 6px -4px";

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("floating-skins");
        String body = "<div style=\"padding:150px 0 150px 320px\">"
                + OverlayBrowser.withoutScripts(FloatingBrowser.inState(FloatingBrowser.tooltip(FloatingBrowser.trigger("st", "Tip"), FloatingBrowser.SHORT_TIP, null), true, true))
                + "</div><div style=\"padding:0 0 150px 320px\">"
                + OverlayBrowser.withoutScripts(FloatingBrowser.inState(FloatingBrowser.popover(FloatingBrowser.trigger("sp", "Pop"), FloatingBrowser.BODY, "Title", null), true, true))
                + "</div><div id=\"probe-bubble\" data-tooltip-bubble></div><div id=\"probe-surface\" data-popover-surface></div>";
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir());
             DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
            for (ParityConfigs.Config config : ParityConfigs.all()) {
                cdp.navigate(server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.APP), config, "", body, "")));
                RESULTS.put(config.id(), cdp.evaluate("(function(){function s(q){return getComputedStyle(document.querySelector(q))}"
                        + "var b=s('[data-tooltip] [data-tooltip-bubble]'),p=s('[data-popover] [data-popover-surface]'),r=getComputedStyle(document.documentElement);"
                        + "return [b.boxShadow,s('#probe-bubble').boxShadow,p.boxShadow,s('#probe-surface').boxShadow,b.borderTopLeftRadius,p.borderTopLeftRadius,"
                        + "r.getPropertyValue('--radius-md').trim(),r.getPropertyValue('--radius-lg').trim()].join(' || ')})()"));
            }
        }
    }

    private static String[] parts(String config) {
        return RESULTS.get(config).split(" \\|\\| ");
    }

    private static String px(String rem) {
        return rem.endsWith("rem") ? (int) Math.round(Double.parseDouble(rem.replace("rem", "")) * 16) + "px" : rem;
    }

    @Test
    void underEverySkinTheBubbleAndTheSurfaceWearTheSkinsShadowFromItsHookRule() {
        for (ParityConfigs.Config config : ParityConfigs.all()) {
            if (config.skin() == null) {
                continue;
            }
            String[] p = parts(config.id());
            assertEquals(TextEntryBrowser.visibleShadow(p[1]), TextEntryBrowser.visibleShadow(p[0]), config.id() + ": the bubble's shadow is the skin's");
            assertEquals(TextEntryBrowser.visibleShadow(p[3]), TextEntryBrowser.visibleShadow(p[2]), config.id() + ": the surface's shadow (ring included) is the skin's");
            assertNotEquals(TOOLTIP_DEFAULT, TextEntryBrowser.visibleShadow(p[0]), config.id() + ": the skin replaces the default");
            assertNotEquals("none", TextEntryBrowser.visibleShadow(p[1]), config.id() + ": the skin's hook rule applies to a bare hook");
        }
    }

    @Test
    void withNoSkinTheBubbleWearsItsShadowAndTheSurfaceItsRingOverItsShadow() {
        for (String id : List.of("light/none", "dark/none")) {
            String[] p = parts(id);
            assertEquals(TOOLTIP_DEFAULT, TextEntryBrowser.visibleShadow(p[0]), id + " bubble");
            String surface = TextEntryBrowser.visibleShadow(p[2]);
            assertTrue(surface.endsWith(" 0px 0px 0px 1px, " + TOOLTIP_DEFAULT) && surface.startsWith("oklch("), id + ": a 1px ring first, then the drop shadow: " + surface);
            assertEquals("none", TextEntryBrowser.visibleShadow(p[1]), id + ": no skin, so a bare hook has no shadow");
        }
    }

    @Test
    void theRadiiAreTheSkinsRadiusVariables() {
        for (ParityConfigs.Config config : ParityConfigs.all()) {
            String[] p = parts(config.id());
            assertEquals(px(p[6]), p[4], config.id() + ": the bubble's radius is --radius-md");
            assertEquals(px(p[7]), p[5], config.id() + ": the surface's radius is --radius-lg");
        }
    }
}
