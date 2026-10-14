package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rewrite that renders a migrated Progress with its legacy classes must be exact, and must fail loudly rather than half-rewrite. */
class ProgressLegacyMarkupTest {

    private static String progress(String size, String header, String label) {
        return "<div class=\"ash-progress\" data-progress data-progress-size=\"" + size + "\">\n"
                + "    <div class=\"" + header + "\">\n        " + label + "<span class=\"ash-progress__value\">65%</span>\n    </div>\n"
                + "    <div\n            role=\"progressbar\"\n            aria-valuemin=\"0\"\n            aria-valuemax=\"100\"\n            aria-valuenow=\"65\"\n"
                + "            class=\"ash-progress__track ash-progress__track--" + size + "\"\n    >\n"
                + "        <div class=\"ash-progress__fill\" style=\"width: 65%\"></div>\n    </div>\n</div>";
    }

    private static final String LABEL = "<span class=\"ash-progress__label\">Migration</span>";
    private static final String BETWEEN = "ash-progress__header ash-progress__header--between";
    private static final String END = "ash-progress__header ash-progress__header--end";

    @Test
    void everyPartOfAProgressBecomesItsExactLegacyClassesAndNothingElseChanges() throws IOException {
        ProgressLegacyMarkup legacy = new ProgressLegacyMarkup();
        String migrated = "<p class=\"text-sm\">before</p>" + progress("medium", BETWEEN, LABEL) + "<p>after</p>";
        String expected = migrated
                .replace("class=\"ash-progress\"", "class=\"" + legacy.classes("wrapper") + "\"")
                .replace("class=\"" + BETWEEN + "\"", "class=\"" + legacy.classes("header-between") + "\"")
                .replace("class=\"ash-progress__label\"", "class=\"" + legacy.classes("label") + "\"")
                .replace("class=\"ash-progress__value\"", "class=\"" + legacy.classes("value") + "\"")
                .replace("class=\"ash-progress__track ash-progress__track--medium\"", "class=\"" + legacy.classes("track-medium") + "\"")
                .replace("class=\"ash-progress__fill\"", "class=\"" + legacy.classes("fill") + "\"");
        assertEquals(expected, legacy.rewrite(migrated));
        assertFalse(legacy.rewrite(migrated).contains("ash-progress"));
        assertTrue(legacy.rewrite(migrated).contains("role=\"progressbar\"") && legacy.rewrite(migrated).contains("aria-valuenow=\"65\"")
                && legacy.rewrite(migrated).contains("style=\"width: 65%\"") && legacy.rewrite(migrated).contains("data-progress-size=\"medium\""));
    }

    @Test
    void theThreeSizesAndTheTwoHeaderJustificationsMapToTheirOwnLegacyClasses() throws IOException {
        ProgressLegacyMarkup legacy = new ProgressLegacyMarkup();
        assertTrue(legacy.rewrite(progress("small", END, "")).contains("h-1.5"));
        assertTrue(legacy.rewrite(progress("medium", END, "")).contains(" h-2\""));
        assertTrue(legacy.rewrite(progress("large", END, "")).contains(" h-3\""));
        assertTrue(legacy.rewrite(progress("small", END, "")).contains("justify-end"));
        assertTrue(legacy.rewrite(progress("small", BETWEEN, LABEL)).contains("justify-between"));
    }

    @Test
    void markupWithoutAProgressAndOtherAshClassesAreLeftExactlyAsServed() throws IOException {
        String page = "<span class=\"ash-sr-only\">x</span><div class=\"ash-alert ash-alert--info\"></div><p>ash-progress in prose</p>"
                + "<div class=\"w-full\"></div>";
        assertEquals(page, new ProgressLegacyMarkup().rewrite(page));
    }

    @Test
    void severalProgressesAreEachRewrittenAndCounted() throws IOException {
        ProgressLegacyMarkup legacy = new ProgressLegacyMarkup();
        String page = progress("small", BETWEEN, LABEL) + progress("large", END, "") + progress("medium", END, "");
        assertEquals(3, ProgressLegacyMarkup.count(page));
        String out = legacy.rewrite(page);
        assertEquals(0, ProgressLegacyMarkup.count(out));
        assertEquals(3, out.split("data-progress-size", -1).length - 1);
    }

    @Test
    void aProgressOfTheWrongShapeFailsLoudlyInsteadOfBeingHalfRewritten() throws IOException {
        ProgressLegacyMarkup legacy = new ProgressLegacyMarkup();
        assertThrows(IllegalStateException.class, () -> legacy.rewrite(progress("medium", BETWEEN, LABEL).replace("--medium\"", "--large\"")));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite(progress("medium", BETWEEN, LABEL).replace("ash-progress__fill", "ash-progress__fill ash-progress__fill--x")));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite(progress("medium", BETWEEN, LABEL).replace("ash-progress__label", "ash-progress__extra")));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite(progress("medium", BETWEEN, LABEL).replace("ash-progress__track ash-progress__track--medium", "ash-progress__track")));
    }

    @Test
    void everyRoleHasLegacyClassesAndTheTokensAreTheOnesTheOracleMustCompile() throws IOException {
        ProgressLegacyMarkup legacy = new ProgressLegacyMarkup();
        for (String role : ProgressLegacyMarkup.ROLES) {
            assertFalse(legacy.classes(role).isBlank(), role);
            assertTrue(ProgressLegacyMarkup.MIGRATED.containsKey(role), role);
        }
        assertEquals(java.util.Set.of("w-full", "mb-1.5", "flex", "flex-wrap", "items-center", "gap-x-3", "gap-y-1", "text-sm", "justify-between",
                "justify-end", "font-medium", "text-slate-700", "dark:text-slate-300", "text-slate-500", "dark:text-slate-400", "overflow-hidden",
                "rounded-full", "bg-slate-200", "dark:bg-slate-700", "h-1.5", "h-2", "h-3", "h-full", "bg-blue-600", "dark:bg-blue-500"), legacy.allTokens());
    }
}
