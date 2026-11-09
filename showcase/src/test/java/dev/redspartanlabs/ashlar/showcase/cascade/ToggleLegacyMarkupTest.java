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
 * The rewrite that turns migrated Toggle markup back into the former Tailwind markup: what it changes, what it chooses (the label position), what it must leave alone, that it
 * fails loudly, that the legacy fixture is grounded in the original template, and that the markup the parity suites render (the mirror in {@link ToggleBrowser}) writes exactly the
 * classes the shipped template writes.
 */
class ToggleLegacyMarkupTest {

    @Test
    void aToggleGetsEachOfItsLegacyStringsAndNoMigratedClassSurvivesButTheRequiredMarker() throws Exception {
        ToggleLegacyMarkup l = new ToggleLegacyMarkup();
        String out = l.rewrite(ToggleBrowser.toggle(ToggleBrowser.o("id", "t", "required", true, "help", "h", "error", true, "errMsg", "e")));
        for (String role : List.of("toggle", "label", "switch", "input", "track", "thumb", "text", "help", "error")) {
            assertTrue(out.contains("class=\"" + l.classes(role) + "\""), role + " in " + out);
        }
        assertFalse(out.contains("ash-toggle") || out.contains("ash-choice") || out.contains("ash-sr-only"), out);
        assertTrue(out.contains("class=\"ash-field__required\""), "the required marker is Text Entry's to rewrite: " + out);
    }

    @Test
    void theLabelPositionChoosesTheLabelsSecondString() throws Exception {
        ToggleLegacyMarkup l = new ToggleLegacyMarkup();
        String end = l.rewrite(ToggleBrowser.toggle(ToggleBrowser.o("id", "t")));
        String start = l.rewrite(ToggleBrowser.toggle(ToggleBrowser.o("id", "t", "start", true)));
        assertTrue(end.contains("class=\"" + l.classes("label") + "\"") && !end.contains("flex-row-reverse"), end);
        assertTrue(start.contains("class=\"" + l.classes("label-start") + "\"") && l.classes("label-start").equals(l.classes("label") + " flex-row-reverse"), start);
    }

    @Test
    void otherMarkupIsLeftExactlyAsServedAndAnUnknownClassFailsLoudly() throws Exception {
        String other = "<div class=\"ash-card\"><span class=\"ash-sr-only\">x</span><a class=\"ash-toggles\">y</a><i class=\"ash-theme-toggle\"></i><b class=\"ash-choice__description\"></b></div>";
        assertEquals(other, new ToggleLegacyMarkup().rewrite(other));
        assertThrows(IllegalStateException.class, () -> new ToggleLegacyMarkup().rewrite("<span class=\"ash-toggle__unknown\"></span>"));
        assertThrows(IllegalStateException.class, () -> new ToggleLegacyMarkup().rewrite("<span class=\"ash-toggle__track ash-toggle__thumb\"></span>"));
        assertThrows(IllegalStateException.class, () -> new ToggleLegacyMarkup().rewrite("<input class=\"ash-toggle__input\">"), "the hidden input always carries ash-sr-only");
    }

    @Test
    void theLegacyFixtureIsGroundedInTheOriginalTemplateAndHasEveryTokenTheComparisonRelies() throws Exception {
        ToggleLegacyMarkup l = new ToggleLegacyMarkup();
        assertTrue(l.allTokens().containsAll(List.of("peer", "sr-only", "rounded-full", "transition-colors", "transition-transform", "duration-150", "motion-reduce:transition-none", "hover:bg-slate-400",
                "dark:hover:bg-slate-500", "peer-checked:bg-blue-600", "peer-checked:hover:bg-blue-700", "dark:peer-checked:hover:bg-blue-400", "peer-disabled:hover:bg-slate-300",
                "peer-checked:peer-disabled:hover:bg-blue-600", "dark:peer-checked:peer-disabled:hover:bg-blue-500", "peer-disabled:opacity-60", "peer-focus-visible:ring-offset-2",
                "peer-aria-invalid:ring-red-500", "peer-checked:translate-x-5", "shadow-sm", "has-[:disabled]:cursor-not-allowed", "flex-row-reverse", "mt-1", "h-6", "w-11", "h-5", "w-5")));
        assertEquals("mb-4", l.classes("toggle"));
    }

    // ---- the mirror

    private static Set<String> ashClasses(String text) {
        Set<String> found = new TreeSet<>();
        Matcher m = Pattern.compile("(?<![\\w-])ash-(?:toggle|choice__description|choice__error|sr-only|field__required)(?:__[a-z-]+)?(?:--[a-z]+)?").matcher(text);
        while (m.find()) {
            found.add(m.group());
        }
        return found;
    }

    private static String shipped() throws Exception {
        try (InputStream in = ToggleLegacyMarkupTest.class.getResourceAsStream("/ashlar/components/toggle.jte")) {
            assertTrue(in != null, "toggle.jte is not on the test classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("(?s)<%--.*?--%>", "");
        }
    }

    /** The parity suites render markup written by {@link ToggleBrowser}, which mirrors the template: the Ashlar classes it writes are exactly the ones the mirror writes. */
    @Test
    void theMirrorWritesExactlyTheClassesTheShippedTemplateWrites() throws Exception {
        assertEquals(ashClasses(shipped()), ashClasses(ToggleBrowser.toggle(ToggleBrowser.o("required", true, "help", "h", "error", true, "errMsg", "e", "start", true))));
    }

    @Test
    void theMirrorAndTheTemplateAgreeOnTheDomShape() throws Exception {
        String shape = shipped().replaceAll("(?m)^@(?:param|import) .*$", "").replaceAll("(?m)^!\\{.*\\}$", "").replaceAll("\\s+", " ");
        for (String fragment : List.of("<div class=\"ash-toggle\">", "<span class=\"ash-toggle__switch\">", "class=\"ash-toggle__input ash-sr-only\"", "<span aria-hidden=\"true\" class=\"ash-toggle__track\"></span>",
                "<span aria-hidden=\"true\" class=\"ash-toggle__thumb\"></span>", "<span class=\"ash-toggle__text\">")) {
            assertTrue(shape.contains(fragment), fragment);
        }
        String mirror = ToggleBrowser.toggle(ToggleBrowser.o("id", "t"));
        assertTrue(mirror.indexOf("ash-toggle__input") < mirror.indexOf("ash-toggle__track") && mirror.indexOf("ash-toggle__track") < mirror.indexOf("ash-toggle__thumb"),
                "the input, then the track, then the thumb: siblings in that order");
    }

    @Test
    void theRealPageProbeNamesNoToggleElement() throws Exception {
        String probe = new String(ToggleLegacyMarkupTest.class.getResourceAsStream("/cascade/real-page-probe.js").readAllBytes(), StandardCharsets.UTF_8);
        assertFalse(probe.contains("ash-toggle"), "the probe is unchanged by this migration and names no Toggle element");
    }
}
