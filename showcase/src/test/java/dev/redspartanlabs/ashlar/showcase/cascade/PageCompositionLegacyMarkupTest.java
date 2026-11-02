package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rewrite that renders the migrated page composition utilities with their legacy classes must be exact, and must fail loudly rather than half-rewrite. */
class PageCompositionLegacyMarkupTest {

    private static final String FULL = "<div class=\"ash-page-shell\">"
            + "<header class=\"ash-page-header\"><div class=\"ash-page-header__heading\"><h1 class=\"ash-page-header__title\">Title</h1>"
            + "<p class=\"ash-page-header__description\">About</p></div><div class=\"ash-page-header__actions\"><button class=\"ash-button\">New</button></div></header>"
            + "<section class=\"ash-page-section\"><div><h2 class=\"ash-page-section__title\">Section</h2><p class=\"ash-page-section__description\">More</p></div>"
            + "<p class=\"text-sm\">content</p></section></div>"
            + "<footer class=\"ash-footer\"><div class=\"ash-footer__inner\">&copy; Ashlar</div></footer>";

    @Test
    void everyPartBecomesItsExactLegacyClassesAndNothingElseChanges() throws IOException {
        PageCompositionLegacyMarkup legacy = new PageCompositionLegacyMarkup();
        String migrated = "<p class=\"text-sm\">before</p>" + FULL + "<p>after</p>";
        String expected = migrated;
        for (String role : PageCompositionLegacyMarkup.ROLES) {
            expected = expected.replace("class=\"" + PageCompositionLegacyMarkup.MIGRATED.get(role) + "\"", "class=\"" + legacy.classes(role) + "\"");
        }
        String out = legacy.rewrite(migrated);
        assertEquals(expected, out);
        assertFalse(out.contains("ash-page-") || out.contains("ash-footer"));
        // The caller's own content, its classes, and the classless heading block inside a section are untouched.
        assertTrue(out.contains("<button class=\"ash-button\">New</button>") && out.contains("<p class=\"text-sm\">content</p>") && out.contains("<section class=\"flex flex-col gap-4\"><div><h2 "));
    }

    @Test
    void theLegacyClassStringsAreTheOnesTheTemplatesRenderedBeforeTheMigration() throws IOException {
        PageCompositionLegacyMarkup legacy = new PageCompositionLegacyMarkup();
        assertEquals("mx-auto flex w-full max-w-6xl flex-col gap-10 px-4 py-8 sm:px-6 lg:px-8", legacy.classes("page-shell"));
        assertEquals("flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between", legacy.classes("page-header"));
        assertEquals("min-w-0", legacy.classes("page-header-heading"));
        assertEquals("text-xl font-semibold text-slate-900 dark:text-slate-100", legacy.classes("page-header-title"));
        assertEquals("mt-1 text-sm text-slate-500 dark:text-slate-400", legacy.classes("page-header-description"));
        assertEquals("flex flex-wrap items-center gap-3", legacy.classes("page-header-actions"));
        assertEquals("flex flex-col gap-4", legacy.classes("page-section"));
        assertEquals("text-lg font-semibold text-slate-900 dark:text-slate-100", legacy.classes("page-section-title"));
        assertEquals("mt-1 text-sm text-slate-500 dark:text-slate-400", legacy.classes("page-section-description"));
        assertEquals("border-t border-slate-200 py-8 text-sm text-slate-500 dark:border-slate-700 dark:text-slate-400", legacy.classes("footer"));
        assertEquals("mx-auto max-w-6xl px-4", legacy.classes("footer-inner"));
    }

    @Test
    void markupWithoutTheseUtilitiesAndOtherAshClassesAreLeftExactlyAsServed() throws IOException {
        String page = "<span class=\"ash-sr-only\">x</span><ul class=\"ash-list\"></ul><p>ash-footer in prose</p><footer class=\"border-t\"></footer><p class=\"ash-status-line\">s</p>";
        assertEquals(page, new PageCompositionLegacyMarkup().rewrite(page));
    }

    @Test
    void severalOfEachAreRewrittenAndCountedPerPart() throws IOException {
        PageCompositionLegacyMarkup legacy = new PageCompositionLegacyMarkup();
        String page = FULL + FULL + "<footer class=\"ash-footer\"></footer>";
        assertEquals(3, PageCompositionLegacyMarkup.count(page, "footer"));
        assertEquals(2, PageCompositionLegacyMarkup.count(page, "page-header"), "a part's class is not counted as its parent");
        assertEquals(2, PageCompositionLegacyMarkup.count(page, "page-section-title"));
        assertEquals(0, PageCompositionLegacyMarkup.count(legacy.rewrite(page), "footer"));
    }

    @Test
    void anUnknownClassOnTheseUtilitiesFailsLoudlyInsteadOfBeingHalfRewritten() throws IOException {
        PageCompositionLegacyMarkup legacy = new PageCompositionLegacyMarkup();
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<footer class=\"ash-footer ash-footer--sticky\"></footer>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<div class=\"ash-page-shell mt-4\"></div>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<h2 class=\"ash-page-section__subtitle\"></h2>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<header class=\"ash-page-header ash-page-header--compact\"></header>"));
    }

    @Test
    void theTokensAreTheOnesTheOracleMustCompile() throws IOException {
        Set<String> tokens = new PageCompositionLegacyMarkup().allTokens();
        assertTrue(tokens.containsAll(Set.of("mx-auto", "flex", "w-full", "max-w-6xl", "flex-col", "gap-10", "px-4", "py-8", "sm:px-6", "lg:px-8", "gap-4",
                "sm:flex-row", "sm:items-start", "sm:justify-between", "min-w-0", "text-xl", "font-semibold", "text-slate-900", "dark:text-slate-100", "mt-1",
                "text-sm", "text-slate-500", "dark:text-slate-400", "flex-wrap", "items-center", "gap-3", "text-lg", "border-t", "border-slate-200",
                "dark:border-slate-700")), tokens.toString());
    }
}
