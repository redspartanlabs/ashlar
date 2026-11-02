package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rewrite that renders the migrated Card and Stat Card with their legacy classes must be exact, must keep the hooks, and must fail loudly rather than half-rewrite. */
class CardLegacyMarkupTest {

    private static final String CARD = "<div class=\"ash-card\" data-card>"
            + "<div class=\"ash-card__header\" data-card-header><h3 class=\"ash-card__title\">Title</h3></div>"
            + "<div class=\"ash-card__body\"><p class=\"text-sm\">body</p></div>"
            + "<div class=\"ash-card__footer\" data-card-footer><button class=\"ash-button\">OK</button></div></div>";
    private static final String STAT = "<div class=\"ash-stat-card\" data-stat-card><dl class=\"ash-stat-card__list\">"
            + "<dt class=\"ash-stat-card__label\">Users</dt><dd class=\"ash-stat-card__values\"><span class=\"ash-stat-card__value\">12</span>"
            + "<span class=\"ash-stat-card__change\">+1</span></dd><dd class=\"ash-stat-card__description\">now</dd></dl></div>";

    @Test
    void everyPartBecomesItsExactLegacyClassesAndNothingElseChanges() throws IOException {
        CardLegacyMarkup legacy = new CardLegacyMarkup();
        String migrated = "<p class=\"text-sm\">before</p>" + CARD + STAT + "<p>after</p>";
        String expected = migrated;
        for (String role : CardLegacyMarkup.ROLES) {
            expected = expected.replace("class=\"" + CardLegacyMarkup.MIGRATED.get(role) + "\"", "class=\"" + legacy.classes(role) + "\"");
        }
        expected = expected.replace(" class=\"ash-stat-card__list\"", "");
        String out = legacy.rewrite(migrated);
        assertEquals(expected, out);
        assertFalse(out.contains("ash-card") || out.contains("ash-stat-card"));
        // The caller's own content and classes are untouched.
        assertTrue(out.contains("<button class=\"ash-button\">OK</button>") && out.contains("<p class=\"text-sm\">body</p>"));
    }

    @Test
    void theHooksSurviveTheRewriteSoTheSkinsSelectTheSameElementsOnBothSides() throws IOException {
        String out = new CardLegacyMarkup().rewrite(CARD + STAT);
        assertTrue(out.contains("data-card>") && out.contains("data-card-header>") && out.contains("data-card-footer>") && out.contains("data-stat-card>"), out);
        assertEquals(1, count(out, " data-card-header"));
        assertEquals(1, count(out, " data-card-footer"));
    }

    @Test
    void theStatCardsListHadNoClassSoItsAttributeIsRemovedNotLeftEmpty() throws IOException {
        String out = new CardLegacyMarkup().rewrite(STAT);
        assertTrue(out.contains("data-stat-card><dl><dt class="), out);
        assertFalse(out.contains("class=\"\""));
    }

    @Test
    void theLegacyClassStringsAreTheOnesTheTemplatesRenderedBeforeTheMigration() throws IOException {
        CardLegacyMarkup legacy = new CardLegacyMarkup();
        assertEquals("overflow-hidden rounded-xl border border-slate-200 bg-white dark:border-slate-700 dark:bg-slate-900", legacy.classes("card"));
        assertEquals("border-b border-slate-200 px-6 py-4 dark:border-slate-700", legacy.classes("card-header"));
        assertEquals("text-base font-semibold text-slate-900 dark:text-slate-100", legacy.classes("card-title"));
        assertEquals("px-6 py-4 text-sm text-slate-700 dark:text-slate-300", legacy.classes("card-body"));
        assertEquals("flex flex-wrap items-center justify-end gap-3 border-t border-slate-200 bg-slate-50 px-6 py-4 dark:border-slate-700 dark:bg-slate-800",
                legacy.classes("card-footer"));
        assertEquals("rounded-xl border border-slate-200 bg-white p-6 dark:border-slate-700 dark:bg-slate-900", legacy.classes("stat-card"));
        assertEquals("", legacy.classes(CardLegacyMarkup.LIST_ROLE));
        assertEquals("text-sm font-medium text-slate-500 dark:text-slate-400", legacy.classes("stat-card-label"));
        assertEquals("mt-2 flex flex-wrap items-baseline gap-x-2 gap-y-1", legacy.classes("stat-card-values"));
        assertEquals("break-words text-3xl font-semibold tracking-tight text-slate-900 dark:text-slate-100", legacy.classes("stat-card-value"));
        assertEquals("shrink-0", legacy.classes("stat-card-change"));
        assertEquals("mt-1 break-words text-sm text-slate-500 dark:text-slate-400", legacy.classes("stat-card-description"));
    }

    @Test
    void markupWithoutTheseComponentsAndOtherAshClassesAreLeftExactlyAsServed() throws IOException {
        String page = "<span class=\"ash-sr-only\">x</span><ul class=\"ash-list\"></ul><p>ash-card in prose</p><div class=\"rounded-xl border\" data-card-like></div><p class=\"ash-status-line\">s</p>";
        assertEquals(page, new CardLegacyMarkup().rewrite(page));
    }

    @Test
    void severalOfEachAreRewrittenAndCountedPerPart() throws IOException {
        CardLegacyMarkup legacy = new CardLegacyMarkup();
        String page = CARD + CARD + STAT + "<div class=\"ash-card\" data-card></div>";
        assertEquals(3, CardLegacyMarkup.count(page, "card"));
        assertEquals(2, CardLegacyMarkup.count(page, "card-header"), "a part's class is not counted as its parent");
        assertEquals(1, CardLegacyMarkup.count(page, "stat-card-list"));
        assertEquals(0, CardLegacyMarkup.count(legacy.rewrite(page), "card"));
    }

    @Test
    void anUnknownClassOnTheseComponentsFailsLoudlyInsteadOfBeingHalfRewritten() throws IOException {
        CardLegacyMarkup legacy = new CardLegacyMarkup();
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<div class=\"ash-card ash-card--flat\"></div>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<div class=\"ash-card mt-4\"></div>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<div class=\"ash-card__subtitle\"></div>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<div class=\"ash-stat-card ash-stat-card--wide\"></div>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<dd class=\"ash-stat-card__footnote\"></dd>"));
    }

    @Test
    void theTokensAreTheOnesTheOracleMustCompile() throws IOException {
        Set<String> tokens = new CardLegacyMarkup().allTokens();
        assertTrue(tokens.containsAll(Set.of("overflow-hidden", "rounded-xl", "border", "border-slate-200", "bg-white", "dark:border-slate-700", "dark:bg-slate-900",
                "border-b", "px-6", "py-4", "text-base", "font-semibold", "text-sm", "text-slate-700", "dark:text-slate-300", "flex", "flex-wrap", "items-center",
                "justify-end", "gap-3", "border-t", "bg-slate-50", "dark:bg-slate-800", "p-6", "font-medium", "mt-2", "items-baseline", "gap-x-2", "gap-y-1",
                "break-words", "text-3xl", "tracking-tight", "shrink-0", "mt-1")), tokens.toString());
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }
}
