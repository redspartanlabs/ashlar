package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rewrite that turns migrated Checkbox and Radio Group markup back into the former Tailwind markup: what it changes, what it chooses from the markup (a Radio option's disabled
 * strings, from its own {@code disabled} attribute or its fieldset's), what it must leave alone, that it fails loudly, and that the markup the parity suites render (the mirror in
 * {@link ChoiceBrowser}) writes exactly the classes the shipped templates write.
 */
class ChoiceLegacyMarkupTest {

    private static String legacy(String migrated) throws Exception {
        return new ChoiceLegacyMarkup().rewrite(migrated);
    }

    @Test
    void aCheckboxGetsEachOfItsLegacyStringsAndNoMigratedClassSurvivesButTheRequiredMarker() throws Exception {
        ChoiceLegacyMarkup l = new ChoiceLegacyMarkup();
        String out = l.rewrite(ChoiceBrowser.checkbox(ChoiceBrowser.o("id", "c", "required", true, "help", "h", "error", true, "errMsg", "e")));
        for (String role : List.of("checkbox", "choice-row", "choice-body", "checkbox-input", "checkbox-label", "checkbox-help", "checkbox-error")) {
            assertTrue(out.contains("class=\"" + l.classes(role) + "\""), role + " in " + out);
        }
        assertFalse(out.contains("ash-choice") || out.contains("ash-checkbox"), out);
        assertTrue(out.contains("class=\"ash-field__required\""), "the required marker is Text Entry's to rewrite: " + out);
    }

    @Test
    void aRadioGroupGetsItsLegacyStringsAndTheOrientationChoosesTheOptionsLayout() throws Exception {
        ChoiceLegacyMarkup l = new ChoiceLegacyMarkup();
        String vertical = l.rewrite(ChoiceBrowser.radioGroup(ChoiceBrowser.o("id", "g", "help", "h", "error", true, "errMsg", "e")));
        String horizontal = l.rewrite(ChoiceBrowser.radioGroup(ChoiceBrowser.o("id", "g", "horizontal", true)));
        for (String role : List.of("radio-fieldset", "radio-legend", "radio-options", "radio-input", "choice-row", "choice-body", "radio-help", "radio-error")) {
            assertTrue(vertical.contains("class=\"" + l.classes(role) + "\""), role + " in " + vertical);
        }
        assertTrue(horizontal.contains("class=\"" + l.classes("radio-options-horizontal") + "\"") && !horizontal.contains("class=\"" + l.classes("radio-options") + "\""), horizontal);
        assertFalse(vertical.contains("ash-choice") || vertical.contains("ash-radio-group"), vertical);
    }

    @Test
    void aRadioOptionIsDimmedWhenTheOptionOrTheWholeGroupIsDisabledAndOnlyThenAndPerOption() throws Exception {
        ChoiceLegacyMarkup l = new ChoiceLegacyMarkup();
        String optionOnly = l.rewrite(ChoiceBrowser.radioGroup(ChoiceBrowser.o("id", "g")));
        String wholeGroup = l.rewrite(ChoiceBrowser.radioGroup(ChoiceBrowser.o("id", "g", "disabled", true)));
        assertEquals(1, count(optionOnly, l.classes("radio-label-disabled")), "only the third option's label: " + optionOnly);
        assertEquals(2, count(optionOnly, l.classes("radio-label")), "the other two keep the enabled string");
        assertEquals(2, count(optionOnly, l.classes("radio-description")) + count(optionOnly, l.classes("radio-description-disabled")), "two options have a description");
        assertEquals(1, count(optionOnly, l.classes("radio-description-disabled")));
        assertEquals(3, count(wholeGroup, l.classes("radio-label-disabled")), "a disabled fieldset dims every label: " + wholeGroup);
        assertEquals(0, count(wholeGroup, l.classes("radio-label")));
        assertEquals(2, count(wholeGroup, l.classes("radio-description-disabled")));
    }

    @Test
    void aFieldsetOnlyDimsTheOptionsInsideItNotACheckboxBesideIt() throws Exception {
        ChoiceLegacyMarkup l = new ChoiceLegacyMarkup();
        String page = ChoiceBrowser.radioGroup(ChoiceBrowser.o("id", "g", "disabled", true)) + ChoiceBrowser.checkbox(ChoiceBrowser.o("id", "c", "help", "h"));
        String out = l.rewrite(page);
        assertTrue(out.contains("class=\"" + l.classes("checkbox-label") + "\"") && out.contains("class=\"" + l.classes("checkbox-help") + "\""), "the checkbox after the fieldset is a checkbox: " + out);
    }

    private static int count(String html, String classes) {
        Matcher m = Pattern.compile("class=\"" + Pattern.quote(classes) + "\"").matcher(html);
        int n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }

    @Test
    void otherMarkupIsLeftExactlyAsServedAndAnUnknownClassFailsLoudly() throws Exception {
        String other = "<div class=\"ash-card\"><span class=\"ash-sr-only\">x</span><a class=\"ash-choices\">y</a><i class=\"ash-checkbox-view\"></i></div>";
        assertEquals(other, legacy(other));
        assertThrows(IllegalStateException.class, () -> legacy("<span class=\"ash-choice__unknown\"></span>"));
        assertThrows(IllegalStateException.class, () -> legacy("<div class=\"ash-choice ash-radio-group__help\"></div>"));
    }

    @Test
    void everyLegacyTokenIsCountedForTheOraclesNonVacuityCheck() throws Exception {
        assertTrue(new ChoiceLegacyMarkup().allTokens().containsAll(List.of("accent-blue-600", "focus-visible:ring-2", "focus-visible:ring-offset-2", "focus-visible:ring-blue-500",
                "disabled:opacity-60", "disabled:cursor-not-allowed", "user-invalid:accent-red-600", "aria-invalid:accent-red-600", "rounded-full", "flex-wrap", "gap-x-8", "gap-y-3",
                "text-slate-300", "dark:text-slate-600", "cursor-not-allowed", "mt-0.5", "mt-2", "min-w-0", "border-0")));
    }

    // ---- the mirror

    private static Set<String> ashClasses(String text) {
        Set<String> found = new TreeSet<>();
        Matcher m = Pattern.compile("(?<![\\w-])ash-(?:checkbox|choice|radio-group)(?:__[a-z-]+)?(?:--[a-z]+)?").matcher(text);
        while (m.find()) {
            found.add(m.group());
        }
        return found;
    }

    private static String shipped(String name) throws Exception {
        try (InputStream in = ChoiceLegacyMarkupTest.class.getResourceAsStream("/ashlar/components/" + name + ".jte")) {
            assertTrue(in != null, name + ".jte is not on the test classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("(?s)<%--.*?--%>", "");
        }
    }

    /**
     * The parity suites render markup written by {@link ChoiceBrowser}, which mirrors the templates. This ties the two together: for each template, the Ashlar classes it writes (in
     * its markup and its class expressions) are exactly the classes the mirror writes, over a fixture that uses every variant. A class added to or dropped from a template without the
     * mirror (and so the rewrite and the oracle) following it fails here.
     */
    @Test
    void theMirrorWritesExactlyTheClassesTheShippedTemplatesWrite() throws Exception {
        assertEquals(ashClasses(shipped("checkbox")), ashClasses(ChoiceBrowser.checkbox(ChoiceBrowser.o("required", true, "help", "h", "error", true, "errMsg", "e"))));
        assertEquals(ashClasses(shipped("radio-group")), ashClasses(ChoiceBrowser.radioGroup(ChoiceBrowser.o("required", true, "help", "h", "error", true, "errMsg", "e", "horizontal", true))
                + ChoiceBrowser.radioGroup(ChoiceBrowser.o("id", "v"))));
    }

    @Test
    void theRealPageProbeNamesNoChoiceElement() throws Exception {
        String probe = new String(ChoiceLegacyMarkupTest.class.getResourceAsStream("/cascade/real-page-probe.js").readAllBytes(), StandardCharsets.UTF_8);
        assertFalse(probe.contains("ash-choice") || probe.contains("ash-checkbox") || probe.contains("ash-radio-group"), "the probe is unchanged by this migration and names no Choice element");
    }
}
