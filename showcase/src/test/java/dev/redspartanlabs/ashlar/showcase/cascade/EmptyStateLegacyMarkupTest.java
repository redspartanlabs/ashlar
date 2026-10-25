package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rewrite that renders a migrated Empty State with its legacy classes must be exact, and must fail loudly rather than half-rewrite. */
class EmptyStateLegacyMarkupTest {

    private static final String FULL = "<div class=\"ash-empty-state\" data-empty-state>\n"
            + "    <div class=\"ash-empty-state__icon\">\n        <svg class=\"ash-icon ash-empty-state__glyph\" aria-hidden=\"true\"></svg>\n    </div>\n"
            + "    <h3 class=\"ash-empty-state__title\">No messages</h3>\n"
            + "    <div class=\"ash-empty-state__body\">\n        <p class=\"keep-me\">Body</p>\n    </div>\n"
            + "    <div class=\"ash-empty-state__action\">\n        <a class=\"mt-4 flex\" href=\"#\">Go</a>\n    </div>\n</div>";

    @Test
    void everyPartOfAnEmptyStateBecomesItsExactLegacyClassesAndNothingElseChanges() throws IOException {
        EmptyStateLegacyMarkup legacy = new EmptyStateLegacyMarkup();
        String migrated = "<p class=\"text-sm\">before</p>" + FULL + "<p>after</p>";
        String expected = migrated
                .replace("class=\"ash-empty-state\"", "class=\"" + legacy.classes("wrapper") + "\"")
                .replace("class=\"ash-empty-state__icon\"", "class=\"" + legacy.classes("icon") + "\"")
                .replace("class=\"ash-icon ash-empty-state__glyph\"", "class=\"" + legacy.classes("glyph") + "\"")
                .replace("class=\"ash-empty-state__title\"", "class=\"" + legacy.classes("title") + "\"")
                .replace("class=\"ash-empty-state__body\"", "class=\"" + legacy.classes("body") + "\"")
                .replace("class=\"ash-empty-state__action\"", "class=\"" + legacy.classes("action") + "\"");
        assertEquals(expected, legacy.rewrite(migrated));
        String out = legacy.rewrite(migrated);
        assertFalse(out.contains("ash-empty-state"));
        // The caller's own content, the data hook, the heading and the Icon's accessibility attributes are untouched.
        assertTrue(out.contains("data-empty-state") && out.contains("<h3 ") && out.contains("aria-hidden=\"true\"")
                && out.contains("<p class=\"keep-me\">") && out.contains("<a class=\"mt-4 flex\" href=\"#\">"));
    }

    @Test
    void theGlyphKeepsTheIconsOwnTwoUtilitiesInTheLegacyValueToo() throws IOException {
        EmptyStateLegacyMarkup legacy = new EmptyStateLegacyMarkup();
        assertEquals("inline-block shrink-0 h-10 w-10", legacy.classes("glyph"));
        assertTrue(legacy.rewrite(FULL).contains("class=\"inline-block shrink-0 h-10 w-10\""));
    }

    @Test
    void markupWithoutAnEmptyStateAndOtherAshClassesAreLeftExactlyAsServed() throws IOException {
        String page = "<span class=\"ash-sr-only\">x</span><div class=\"ash-progress\" data-progress></div><p>ash-empty-state in prose</p>"
                + "<div class=\"flex flex-col\"></div><div data-empty-state-ish class=\"w-full\"></div>";
        assertEquals(page, new EmptyStateLegacyMarkup().rewrite(page));
    }

    @Test
    void severalEmptyStatesAreEachRewrittenAndCounted() throws IOException {
        EmptyStateLegacyMarkup legacy = new EmptyStateLegacyMarkup();
        String page = FULL + FULL + FULL;
        assertEquals(3, EmptyStateLegacyMarkup.count(page));
        String out = legacy.rewrite(page);
        assertEquals(0, EmptyStateLegacyMarkup.count(out));
        assertEquals(3, out.split("data-empty-state", -1).length - 1);
    }

    @Test
    void anEmptyStateOfTheWrongShapeFailsLoudlyInsteadOfBeingHalfRewritten() throws IOException {
        EmptyStateLegacyMarkup legacy = new EmptyStateLegacyMarkup();
        assertThrows(IllegalStateException.class, () -> legacy.rewrite(FULL.replace("ash-empty-state__title", "ash-empty-state__title ash-empty-state__title--big")));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite(FULL.replace("ash-empty-state__body", "ash-empty-state__extra")));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite(FULL.replace("ash-icon ash-empty-state__glyph", "ash-empty-state__glyph")));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite(FULL.replace("class=\"ash-empty-state\"", "class=\"ash-empty-state mt-4\"")));
    }

    @Test
    void everyRoleHasLegacyClassesAndTheTokensAreTheOnesTheOracleMustCompile() throws IOException {
        EmptyStateLegacyMarkup legacy = new EmptyStateLegacyMarkup();
        for (String role : EmptyStateLegacyMarkup.ROLES) {
            assertFalse(legacy.classes(role).isBlank(), role);
            assertTrue(EmptyStateLegacyMarkup.MIGRATED.containsKey(role), role);
        }
        assertEquals(Set.of("flex", "flex-col", "items-center", "px-6", "py-12", "text-center", "mb-3", "text-slate-400", "dark:text-slate-500",
                "inline-block", "shrink-0", "h-10", "w-10", "text-base", "font-semibold", "text-slate-900", "dark:text-slate-100", "mt-1", "max-w-sm",
                "text-sm", "text-slate-500", "dark:text-slate-400", "mt-4", "flex-wrap", "justify-center", "gap-3"), legacy.allTokens());
    }
}
