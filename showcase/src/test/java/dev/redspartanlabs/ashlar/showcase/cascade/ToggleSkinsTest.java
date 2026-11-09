package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the ten skins had to change for Toggle, read from the skin files themselves. Every skin flattens a thumb's drop shadow through
 * {@code :is(.shadow-xs, .shadow-sm, ...) { --tw-shadow: 0 0 #0000 }}; the migrated thumb no longer carries {@code shadow-sm}, so that rule no longer reaches it. The thumb owns its
 * shadow in {@code --ash-toggle-thumb-shadow}, and each skin sets that property flat on its root. That this is enough is measured, not read: {@link ToggleParityTest} compares the
 * rendering in all 22 configurations (thumb shadows included) and {@link TogglePixelTest} compares the pictures. This only pins that the skin edits are exactly the one narrow rule each,
 * placed after the Button's, and that the Tailwind rules other components still need are still there.
 */
class ToggleSkinsTest {

    private static final List<String> SKINS = ParityConfigs.SKINS;

    private static String skin(String name) throws IOException {
        try (InputStream in = ToggleSkinsTest.class.getResourceAsStream("/static/css/theme-" + name + ".css")) {
            if (in == null) {
                throw new IOException("theme-" + name + ".css is not on the test classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }

    @Test
    void everySkinFlattensTheThumbShadowWithExactlyOneRuleOnItsRoot() throws IOException {
        assertEquals(10, SKINS.size());
        for (String name : SKINS) {
            String css = skin(name);
            String rule = "[data-skin=\"" + name + "\"] {\n    --ash-toggle-thumb-shadow: 0 0 #0000;\n}\n";
            assertEquals(1, count(css, "--ash-toggle-thumb-shadow: 0 0 #0000;"), name + ": the property is set once");
            assertEquals(2, count(css, "--ash-toggle-thumb-shadow"), name + ": and named only by that declaration and its comment");
            assertEquals(1, count(css, rule), name + ": the rule is exactly the one narrow rule");
            String btn = "[data-skin=\"" + name + "\"] .ash-btn {\n    --ash-btn-shadow: 0 0 #0000;\n}\n";
            assertTrue(css.indexOf(btn) >= 0 && css.indexOf(btn) < css.indexOf(rule), name + ": it follows the Button's shadow rule");
        }
    }

    @Test
    void theTailwindShadowRuleOtherComponentsStillNeedIsStillThereInEverySkin() throws IOException {
        for (String name : SKINS) {
            assertTrue(skin(name).contains("[data-skin=\"" + name + "\"] :is(.shadow-xs, .shadow-sm, .shadow, .shadow-md, .shadow-lg, .shadow-xl, .shadow-2xl) {\n    --tw-shadow: 0 0 #0000;\n}"), name);
        }
    }

    @Test
    void noSkinStylesAToggleClassOrAnythingElseAboutToggle() throws IOException {
        for (String name : SKINS) {
            String css = skin(name);
            assertFalse(css.contains("ash-toggle__") || css.contains("ash-toggle {") || css.contains(".ash-toggle"), name + ": a skin does not style Toggle's classes");
            assertFalse(css.contains("peer-checked") || css.contains("peer-focus"), name);
            assertEquals(2, count(css, "--ash-toggle"), name + ": the only ash-toggle names are the custom property and its comment");
        }
    }

    private static int count(String s, String what) {
        int n = 0;
        for (int at = s.indexOf(what); at >= 0; at = s.indexOf(what, at + 1)) {
            n++;
        }
        return n;
    }
}
