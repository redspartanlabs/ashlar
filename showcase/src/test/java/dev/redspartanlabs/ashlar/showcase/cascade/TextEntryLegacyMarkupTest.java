package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rewrite that turns migrated Text Entry markup back into the former Tailwind markup: what it changes, what it chooses from the markup, what it leaves alone, and that it fails loudly. */
class TextEntryLegacyMarkupTest {

    private static String rewritten(TextEntryLegacyMarkup legacy, String migrated) {
        return legacy.rewrite(migrated);
    }

    private static String inputTag(String html) {
        int at = html.indexOf("<input");
        return html.substring(at, html.indexOf('>', at) + 1);
    }

    @Test
    void aTextInputGetsItsPaddingFromTheLeadingAndTrailingModifiersAsTheTemplateChoseIt() throws Exception {
        TextEntryLegacyMarkup legacy = new TextEntryLegacyMarkup();
        String plain = rewritten(legacy, TextEntryBrowser.text(TextEntryBrowser.o("id", "a")));
        String leading = rewritten(legacy, TextEntryBrowser.text(TextEntryBrowser.o("id", "a", "leading", true)));
        String both = rewritten(legacy, TextEntryBrowser.text(TextEntryBrowser.o("id", "a", "leading", true, "trailing", true)));
        String plainInput = inputTag(plain);
        String leadingInput = inputTag(leading);
        String bothInput = inputTag(both);
        assertTrue(plainInput.contains(" pl-3 ") && plainInput.contains(" pr-3 ") && !plainInput.contains("pl-9") && !plainInput.contains("pr-9"), plainInput);
        assertTrue(leadingInput.contains(" pl-9 ") && leadingInput.contains(" pr-3 "), leadingInput);
        assertTrue(bothInput.contains(" pl-9 ") && bothInput.contains(" pr-9 ") && !bothInput.contains(" pl-3 ") && !bothInput.contains(" pr-3 "), bothInput);
        assertTrue(both.contains("class=\"" + legacy.classes("icon-start") + "\"") && both.contains("class=\"" + legacy.classes("icon-end") + "\""), both);
    }

    @Test
    void eachKindOfControlIsToldApartByItsClassesAndKeepsItsOwnLegacyString() throws Exception {
        TextEntryLegacyMarkup legacy = new TextEntryLegacyMarkup();
        assertTrue(rewritten(legacy, TextEntryBrowser.number(TextEntryBrowser.o("id", "n"))).contains("class=\"" + legacy.classes("number-control") + "\""));
        assertTrue(rewritten(legacy, TextEntryBrowser.search(TextEntryBrowser.o("id", "s"))).contains("class=\"" + legacy.classes("search-control") + "\""));
        assertTrue(rewritten(legacy, TextEntryBrowser.text(TextEntryBrowser.o("id", "t"))).contains("class=\"" + legacy.classes("text-control").replace(" pl-3", " pl-3") + "\""));
        String resizable = rewritten(legacy, TextEntryBrowser.textarea(TextEntryBrowser.o("id", "a")));
        String fixed = rewritten(legacy, TextEntryBrowser.textarea(TextEntryBrowser.o("id", "a", "resizable", false)));
        assertTrue(resizable.contains(" resize-y ") && !resizable.contains(" resize-none "), resizable);
        assertTrue(fixed.contains(" resize-none ") && !fixed.contains(" resize-y "), fixed);
    }

    @Test
    void theClearButtonGetsHiddenOrFlexFromItsHiddenAttributeWhichIsThenRemoved() throws Exception {
        TextEntryLegacyMarkup legacy = new TextEntryLegacyMarkup();
        String empty = rewritten(legacy, TextEntryBrowser.search(TextEntryBrowser.o("id", "s")));
        String filled = rewritten(legacy, TextEntryBrowser.search(TextEntryBrowser.o("id", "s", "value", "q")));
        assertTrue(empty.contains("class=\"hidden " + legacy.classes("search-clear") + "\"") && !java.util.regex.Pattern.compile("\shidden[=\s>]").matcher(empty).find(), empty);
        assertTrue(filled.contains("class=\"flex " + legacy.classes("search-clear") + "\""), filled);
        assertFalse(empty.contains("ash-search-input"), "nothing keeps a migrated class: " + empty);
        assertTrue(filled.contains("aria-hidden") || !filled.contains(" hidden"), "only the clear button's own attribute is touched: " + filled);
    }

    @Test
    void theCharacterCountGetsTheRedPairAtItsLimitAndTheAttributeIsRemoved() throws Exception {
        TextEntryLegacyMarkup legacy = new TextEntryLegacyMarkup();
        String below = rewritten(legacy, TextEntryBrowser.textarea(TextEntryBrowser.o("id", "a", "count", true, "maxlength", 5)));
        String limit = rewritten(legacy, TextEntryBrowser.textarea(TextEntryBrowser.o("id", "a", "count", true, "maxlength", 5, "atLimit", true)));
        assertTrue(below.contains("class=\"" + legacy.classes("textarea-count") + "\"") && !below.contains("text-red-600"), below);
        assertTrue(limit.contains("class=\"" + legacy.classes("textarea-count-limit") + "\"") && !limit.contains("data-at-limit"), limit);
    }

    @Test
    void everyOtherPartGetsItsLegacyStringAndNoMigratedClassSurvives() throws Exception {
        TextEntryLegacyMarkup legacy = new TextEntryLegacyMarkup();
        String all = TextEntryBrowser.text(TextEntryBrowser.o("id", "t", "required", true, "help", "h", "error", true, "errMsg", "e", "leading", true, "trailing", true))
                + TextEntryBrowser.textarea(TextEntryBrowser.o("id", "a", "help", "h", "count", true, "maxlength", 3)) + TextEntryBrowser.formField(TextEntryBrowser.o("id", "f", "required", true, "help", "h", "errMsg", "e"))
                + TextEntryBrowser.search(TextEntryBrowser.o("id", "s", "value", "v")) + TextEntryBrowser.number(TextEntryBrowser.o("id", "n"));
        String out = rewritten(legacy, all);
        for (String role : List.of("field", "label", "required", "help", "error", "box", "icon-start", "icon-end", "textarea-row", "textarea-help")) {
            assertTrue(out.contains("class=\"" + legacy.classes(role) + "\""), role + " in " + out);
        }
        assertFalse(out.contains("ash-field") || out.contains("ash-textarea") || out.contains("ash-search-input"), "nothing carries a migrated class after the rewrite: " + out);
    }

    @Test
    void otherMarkupIsLeftExactlyAsServedAndAnUnknownClassFailsLoudly() throws Exception {
        TextEntryLegacyMarkup legacy = new TextEntryLegacyMarkup();
        String other = "<div class=\"ash-card\"><span class=\"ash-sr-only\">x</span><a class=\"ash-fieldset\">y</a><i class=\"ash-textarea-view\"></i></div>";
        assertEquals(other, legacy.rewrite(other));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<span class=\"ash-field__unknown\"></span>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<input class=\"ash-field__control ash-field__control--readonly ash-field__control--bogus\">"));
    }

    @Test
    void everyLegacyTokenIsCountedForTheOraclesNonVacuityCheck() throws Exception {
        TextEntryLegacyMarkup legacy = new TextEntryLegacyMarkup();
        assertTrue(legacy.allTokens().containsAll(List.of("shadow-sm", "focus:ring-2", "read-only:hover:border-slate-300", "user-invalid:hover:border-red-600", "aria-invalid:focus:ring-red-500",
                "dark:read-only:hover:border-slate-600", "dark:hover:border-slate-500", "placeholder:text-slate-400", "[&::-webkit-search-cancel-button]:appearance-none", "disabled:opacity-40",
                "focus-visible:ring-2", "resize-y", "disabled:resize-none", "max-w-sm", "pl-9", "pr-9", "text-red-600", "shrink-0")));
    }

    @Test
    void countsOnlyExactParts() {
        assertEquals(2, TextEntryLegacyMarkup.count("<p class=\"ash-field__help\"></p><p class=\"ash-field__help\"></p><p class=\"ash-field__help extra\"></p>", "ash-field__help"));
    }

    private static java.util.Set<String> ashClasses(String text) {
        java.util.Set<String> found = new java.util.TreeSet<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?<![\\w-])ash-(?:field|textarea|search-input)(?:__[a-z-]+)?(?:--[a-z]+)?").matcher(text);
        while (m.find()) {
            found.add(m.group());
        }
        return found;
    }

    private static String shippedTemplate(String name) throws Exception {
        try (java.io.InputStream in = TextEntryLegacyMarkupTest.class.getResourceAsStream("/ashlar/components/" + name + ".jte")) {
            assertTrue(in != null, name + ".jte is not on the test classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("(?s)<%--.*?--%>", "");
        }
    }

    /**
     * The parity suites render markup written by {@link TextEntryBrowser}, which mirrors the templates. This is what ties the two together: for each template, the Ashlar classes it
     * writes (in its markup and in its class expressions) are exactly the classes the mirror writes, over a fixture that uses every variant. A class added to or dropped from a
     * template without the mirror (and so the rewrite and the oracle) following it fails here.
     */
    @Test
    void theMirrorWritesExactlyTheClassesTheShippedTemplatesWrite() throws Exception {
        assertEquals(ashClasses(shippedTemplate("form-field")), ashClasses(TextEntryBrowser.formField(TextEntryBrowser.o("required", true, "help", "h", "errMsg", "e"))));
        assertEquals(ashClasses(shippedTemplate("text-input")), ashClasses(TextEntryBrowser.text(TextEntryBrowser.o("required", true, "help", "h", "error", true, "errMsg", "e", "leading", true, "trailing", true))));
        assertEquals(ashClasses(shippedTemplate("number-input")), ashClasses(TextEntryBrowser.number(TextEntryBrowser.o("required", true, "help", "h", "error", true, "errMsg", "e"))));
        assertEquals(ashClasses(shippedTemplate("textarea")), ashClasses(TextEntryBrowser.textarea(TextEntryBrowser.o("required", true, "help", "h", "error", true, "errMsg", "e", "count", true, "resizable", false))
                + TextEntryBrowser.textarea(TextEntryBrowser.o("help", "h", "count", true))));
        assertEquals(ashClasses(shippedTemplate("search-input")), ashClasses(TextEntryBrowser.search(TextEntryBrowser.o("required", true, "help", "h", "error", true, "errMsg", "e", "value", "v"))));
    }

    @Test
    void theRealPageProbeNamesNoTextEntryElement() throws Exception {
        String probe = new String(TextEntryLegacyMarkupTest.class.getResourceAsStream("/cascade/real-page-probe.js").readAllBytes(), StandardCharsets.UTF_8);
        assertFalse(probe.contains("ash-field") || probe.contains("data-search-input") || probe.contains("data-textarea"), "the probe is unchanged by this migration and names no Text Entry element");
    }
}
