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
 * What the ten skins had to change for the Text Entry family, read from the skin files themselves. Two of their rules reached the family through Tailwind class names that the
 * migrated fields no longer carry: every skin flattens a field's drop shadow with {@code :is(.shadow-xs, .shadow-sm, ...) { --tw-shadow: 0 0 #0000 }}, and Ledger fills a field
 * with its paper tone through {@code ...).bg-white}. The family now owns its shadow in {@code --ash-field-shadow}, so each skin sets that property on its root; Ledger's selector also
 * accepts the native control, and keeps {@code .bg-white} because the pickers (Select, Combobox, Date Picker) are not migrated and still carry it.
 *
 * <p>That this is enough is measured, not read: {@link TextEntryParityTest} compares the rendering in all 22 configurations, shadows and fills included. This only pins that the skin
 * edits are exactly the narrow ones, that nothing else about a skin's field rules was touched, and that the Tailwind rules other components still need are still there.
 */
class TextEntrySkinsTest {

    private static final List<String> SKINS = ParityConfigs.SKINS;

    private static String skin(String name) throws IOException {
        try (InputStream in = TextEntrySkinsTest.class.getResourceAsStream("/static/css/theme-" + name + ".css")) {
            if (in == null) {
                throw new IOException("theme-" + name + ".css is not on the test classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }

    @Test
    void everySkinFlattensTheFieldShadowOnItsRootAndOnlyThere() throws IOException {
        assertEquals(10, SKINS.size());
        for (String name : SKINS) {
            String css = skin(name);
            String rule = "[data-skin=\"" + name + "\"] {\n    --ash-field-shadow: 0 0 #0000;\n}\n";
            assertEquals(1, css.split(java.util.regex.Pattern.quote(rule), -1).length - 1, name + ": exactly one field-shadow rule, on the skin's root");
            assertEquals(1, css.split("--ash-field-shadow:", -1).length - 1, name + ": the property is set once and nowhere else");
            assertTrue(css.contains("[data-skin=\"" + name + "\"] :is(.shadow-xs, .shadow-sm, .shadow, .shadow-md, .shadow-lg, .shadow-xl, .shadow-2xl) {\n    --tw-shadow: 0 0 #0000;\n}"),
                    name + ": the Tailwind shadow rule other components still need is untouched");
        }
    }

    @Test
    void theRuleSitsAfterTheTailwindShadowRuleItReplacesForFields() throws IOException {
        for (String name : SKINS) {
            String css = skin(name);
            assertTrue(css.indexOf("--ash-field-shadow") > css.indexOf(":is(.shadow-xs, .shadow-sm"), name);
        }
    }

    @Test
    void ledgerFillsTheNativeControlAndStillFillsTheUnmigratedPickers() throws IOException {
        String ledger = skin("ledger");
        String selector = ":root[data-skin=\"ledger\"]:not([data-theme=\"dark\"]) :is(input:not([type=\"checkbox\"]):not([type=\"radio\"]), textarea, select):is(.bg-white, .ash-field__control, .ash-table__search-input) {\n    background-color: var(--color-slate-50);";
        assertTrue(ledger.contains(selector), "light-only, the Tailwind class, the native control and the Table's native search input, the same fill");
        assertFalse(ledger.contains("select).bg-white {"), "the old single-class selector is gone");
        assertTrue(ledger.contains(":is(.bg-white, .ash-field__control"), "the Tailwind class and the native field control are still named, in that order, in the same selector");
        assertEquals(1, ledger.split("ash-field__control", -1).length - 1);
        assertEquals(1, ledger.split("ash-table__search-input", -1).length - 1, "the Table's search input is named once, in that same selector");
    }

    @Test
    void noOtherSkinMentionsTheFamilyAndNoSkinChangedItsDisabledOrPlaceholderRules() throws IOException {
        for (String name : SKINS) {
            String css = skin(name);
            assertFalse(css.contains("ash-textarea") || css.contains("ash-search-input"), name);
            if (!name.equals("ledger")) {
                assertFalse(css.contains("ash-field__"), name + ": only Ledger names a field class");
            }
            assertTrue(css.contains("::placeholder"), name + ": its placeholder rule is still there (it is class-independent, so the migration needs no change)");
        }
    }
}
