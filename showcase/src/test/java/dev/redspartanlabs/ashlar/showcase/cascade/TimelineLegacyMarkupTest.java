package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rewrite that renders a migrated Timeline and Timeline Item with their legacy classes must be exact, and must fail loudly rather than half-rewrite. */
class TimelineLegacyMarkupTest {

    private static String entry(String variant, String status, boolean date, boolean content) {
        return "<li class=\"ash-timeline-item\">\n"
                + "  <div class=\"ash-timeline-item__rail\">\n"
                + "    <span aria-hidden=\"true\" class=\"ash-timeline-item__marker ash-timeline-item__marker--" + variant + "\"><svg class=\"ash-icon ash-icon--sm\"></svg></span>\n"
                + "    <div class=\"ash-timeline-item__connector\"></div>\n"
                + "  </div>\n"
                + "  <div class=\"ash-timeline-item__body\">\n"
                + (date ? "    <p class=\"ash-timeline-item__date\">March 2026</p>\n" : "")
                + "    <p class=\"ash-timeline-item__title\">\n"
                + (status.isEmpty() ? "" : "      <span class=\"ash-sr-only\">" + status + "</span>\n")
                + "      Version 2.4.0\n    </p>\n"
                + (content ? "    <div class=\"ash-timeline-item__content\"><p class=\"mb-2\">Caller paragraph</p></div>\n" : "")
                + "  </div>\n</li>";
    }

    private static final String FULL = "<ol data-timeline class=\"ash-timeline\">\n"
            + entry("success", "Success: ", true, true) + "\n" + entry("neutral", "", false, false) + "\n</ol>";

    @Test
    void everyPartBecomesItsExactLegacyClassesAndNothingElseChanges() throws IOException {
        TimelineLegacyMarkup legacy = new TimelineLegacyMarkup();
        String migrated = "<p class=\"text-sm\">before</p>" + FULL + "<p>after</p>";
        String expected = migrated
                .replace("<span class=\"ash-sr-only\">", "<span class=\"" + legacy.classes("status") + "\">")
                .replace("class=\"ash-timeline\"", "class=\"" + legacy.classes("timeline") + "\"")
                .replace("class=\"ash-timeline-item\"", "class=\"" + legacy.classes("item") + "\"")
                .replace("class=\"ash-timeline-item__rail\"", "class=\"" + legacy.classes("rail") + "\"")
                .replace("class=\"ash-timeline-item__marker ash-timeline-item__marker--success\"", "class=\"" + legacy.marker("success") + "\"")
                .replace("class=\"ash-timeline-item__marker ash-timeline-item__marker--neutral\"", "class=\"" + legacy.marker("neutral") + "\"")
                .replace("class=\"ash-timeline-item__connector\"", "class=\"" + legacy.classes("connector") + "\"")
                .replace("class=\"ash-timeline-item__body\"", "class=\"" + legacy.classes("body") + "\"")
                .replace("class=\"ash-timeline-item__date\"", "class=\"" + legacy.classes("date") + "\"")
                .replace("class=\"ash-timeline-item__title\"", "class=\"" + legacy.classes("title") + "\"")
                .replace("class=\"ash-timeline-item__content\"", "class=\"" + legacy.classes("content") + "\"");
        String out = legacy.rewrite(migrated);
        assertEquals(expected, out);
        assertFalse(out.contains("ash-timeline") || out.contains("ash-sr-only"), out);
        // The caller's own content, the Icons (the Icon rewrite's job), the hook and the accessibility attributes are untouched.
        assertTrue(out.contains("<p class=\"mb-2\">Caller paragraph</p>") && out.contains("<svg class=\"ash-icon ash-icon--sm\">")
                && out.contains("aria-hidden=\"true\"") && out.contains("data-timeline"));
    }

    @Test
    void theMarkerIsTheSharedClassesPlusTheVariantColoursAndTheConnectorIsHiddenByTheGroupOnTheLastEntry() throws IOException {
        TimelineLegacyMarkup legacy = new TimelineLegacyMarkup();
        for (String variant : TimelineLegacyMarkup.VARIANTS) {
            assertTrue(legacy.marker(variant).startsWith("relative z-10 flex h-8 w-8 shrink-0 items-center justify-center rounded-full border-2 border-"), variant);
        }
        assertEquals("flex flex-col", legacy.classes("timeline"));
        assertEquals("group relative flex gap-4 pb-8 last:pb-0", legacy.classes("item"));
        assertEquals("absolute -bottom-8 top-8 w-0.5 bg-slate-200 group-last:hidden dark:bg-slate-700", legacy.classes("connector"));
        assertEquals("sr-only", legacy.classes("status"));
    }

    @Test
    void theStatusTextIsRewrittenOnlyInsideATimelineTitleSoAnotherComponentsOwnSrOnlyIsLeftAlone() throws IOException {
        TimelineLegacyMarkup legacy = new TimelineLegacyMarkup();
        String page = "<span class=\"ash-sr-only\">Loading</span>" + FULL;
        String out = legacy.rewrite(page);
        assertTrue(out.startsWith("<span class=\"ash-sr-only\">Loading</span>"), out);
        assertEquals(1, out.split("<span class=\"sr-only\">", -1).length - 1);
    }

    @Test
    void markupWithoutATimelineAndOtherAshClassesAreLeftExactlyAsServed() throws IOException {
        String page = "<span class=\"ash-sr-only\">x</span><div class=\"ash-progress\" data-progress></div><p>ash-timeline in prose</p><ol class=\"flex\"><li>raw</li></ol>";
        assertEquals(page, new TimelineLegacyMarkup().rewrite(page));
    }

    @Test
    void severalTimelinesAreEachRewrittenAndCounted() throws IOException {
        TimelineLegacyMarkup legacy = new TimelineLegacyMarkup();
        String page = FULL + FULL + "<ol class=\"ash-timeline\"><li>raw</li></ol>";
        assertEquals(3, TimelineLegacyMarkup.count(page));
        assertEquals(0, TimelineLegacyMarkup.count(legacy.rewrite(page)));
    }

    @Test
    void anUnknownTimelineClassFailsLoudlyInsteadOfBeingHalfRewritten() throws IOException {
        TimelineLegacyMarkup legacy = new TimelineLegacyMarkup();
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<ol class=\"ash-timeline ash-timeline--compact\"></ol>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<ol class=\"ash-timeline mt-4\"></ol>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<span class=\"ash-timeline-item__extra\"></span>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<span class=\"ash-timeline-item__marker\"></span>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<span class=\"ash-timeline-item__marker ash-timeline-item__marker--info extra\"></span>"));
    }

    @Test
    void everyRoleHasLegacyClassesAndTheTokensAreTheOnesTheOracleMustCompile() throws IOException {
        TimelineLegacyMarkup legacy = new TimelineLegacyMarkup();
        for (String role : TimelineLegacyMarkup.ROLES) {
            assertFalse(legacy.classes(role).isBlank(), role);
            assertTrue(TimelineLegacyMarkup.MIGRATED.containsKey(role), role);
        }
        assertTrue(legacy.allTokens().containsAll(Set.of("group", "group-last:hidden", "last:pb-0", "-bottom-8", "top-8", "w-0.5", "bg-slate-200", "dark:bg-slate-700",
                "z-10", "h-8", "w-8", "rounded-full", "border-2", "bg-white", "dark:bg-slate-900", "border-sky-500", "dark:bg-sky-950", "dark:text-red-400",
                "min-w-0", "flex-1", "pb-1", "pt-1", "mt-0.5", "break-words", "font-semibold", "font-medium", "text-xs", "text-sm", "sr-only")));
    }
}
