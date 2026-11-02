package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rewrite that renders the migrated layout utilities with their legacy classes must be exact, and must fail loudly rather than half-rewrite. */
class LayoutUtilitiesLegacyMarkupTest {

    private static final String FULL = "<p class=\"ash-status-line\">Updated <a href=\"#\" class=\"underline\">now</a></p>"
            + "<div class=\"ash-form-actions\"><button class=\"ash-button\">Cancel</button></div>"
            + "<dl class=\"ash-key-value-list\">"
            + "<div class=\"ash-key-value\"><dt class=\"ash-key-value__label\">Plan</dt><dd class=\"ash-key-value__value\">Pro</dd></div>"
            + "</dl>";

    @Test
    void everyPartBecomesItsExactLegacyClassesAndNothingElseChanges() throws IOException {
        LayoutUtilitiesLegacyMarkup legacy = new LayoutUtilitiesLegacyMarkup();
        String migrated = "<p class=\"text-sm\">before</p>" + FULL + "<p>after</p>";
        String expected = migrated
                .replace("class=\"ash-status-line\"", "class=\"" + legacy.classes("status-line") + "\"")
                .replace("class=\"ash-form-actions\"", "class=\"" + legacy.classes("form-actions") + "\"")
                .replace("class=\"ash-key-value-list\"", "class=\"" + legacy.classes("key-value-list") + "\"")
                .replace("class=\"ash-key-value\"", "class=\"" + legacy.classes("key-value") + "\"")
                .replace("class=\"ash-key-value__label\"", "class=\"" + legacy.classes("key-value-label") + "\"")
                .replace("class=\"ash-key-value__value\"", "class=\"" + legacy.classes("key-value-value") + "\"");
        String out = legacy.rewrite(migrated);
        assertEquals(expected, out);
        assertFalse(out.contains("ash-status-line") || out.contains("ash-form-actions") || out.contains("ash-key-value"));
        // The caller's own content and its classes are untouched.
        assertTrue(out.contains("<a href=\"#\" class=\"underline\">now</a>") && out.contains("<button class=\"ash-button\">Cancel</button>"));
    }

    @Test
    void theLegacyClassStringsAreTheOnesTheTemplatesRenderedBeforeTheMigration() throws IOException {
        LayoutUtilitiesLegacyMarkup legacy = new LayoutUtilitiesLegacyMarkup();
        assertEquals("text-sm text-slate-500 dark:text-slate-400", legacy.classes("status-line"));
        assertEquals("flex flex-wrap items-center justify-end gap-3", legacy.classes("form-actions"));
        assertEquals("grid grid-cols-1 gap-x-6 gap-y-1 sm:grid-cols-[minmax(8rem,12rem)_minmax(0,1fr)] sm:items-baseline", legacy.classes("key-value"));
        assertEquals("text-sm font-medium text-slate-500 dark:text-slate-400", legacy.classes("key-value-label"));
        assertEquals("min-w-0 break-words text-sm text-slate-900 dark:text-slate-100", legacy.classes("key-value-value"));
        assertEquals("flex flex-col gap-3", legacy.classes("key-value-list"));
    }

    @Test
    void markupWithoutTheseUtilitiesAndOtherAshClassesAreLeftExactlyAsServed() throws IOException {
        String page = "<span class=\"ash-sr-only\">x</span><ul class=\"ash-list\"></ul><p>ash-status-line in prose</p><dl class=\"flex flex-col gap-3\"></dl>";
        assertEquals(page, new LayoutUtilitiesLegacyMarkup().rewrite(page));
    }

    @Test
    void severalOfEachAreRewrittenAndCountedPerPart() throws IOException {
        LayoutUtilitiesLegacyMarkup legacy = new LayoutUtilitiesLegacyMarkup();
        String page = FULL + FULL + "<dl class=\"ash-key-value-list\"></dl>";
        assertEquals(3, LayoutUtilitiesLegacyMarkup.count(page, "key-value-list"));
        assertEquals(2, LayoutUtilitiesLegacyMarkup.count(page, "key-value"), "the list class is not counted as an entry");
        assertEquals(2, LayoutUtilitiesLegacyMarkup.count(page, "status-line"));
        assertEquals(0, LayoutUtilitiesLegacyMarkup.count(legacy.rewrite(page), "key-value-list"));
    }

    @Test
    void anUnknownClassOnTheseUtilitiesFailsLoudlyInsteadOfBeingHalfRewritten() throws IOException {
        LayoutUtilitiesLegacyMarkup legacy = new LayoutUtilitiesLegacyMarkup();
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<p class=\"ash-status-line ash-status-line--wide\"></p>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<p class=\"ash-status-line mt-4\"></p>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<dd class=\"ash-key-value__extra\"></dd>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<div class=\"ash-form-actions ash-form-actions--start\"></div>"));
    }

    @Test
    void theTokensAreTheOnesTheOracleMustCompile() throws IOException {
        Set<String> tokens = new LayoutUtilitiesLegacyMarkup().allTokens();
        assertTrue(tokens.containsAll(Set.of("text-sm", "text-slate-500", "dark:text-slate-400", "flex", "flex-wrap", "items-center", "justify-end", "gap-3",
                "grid", "grid-cols-1", "gap-x-6", "gap-y-1", "sm:grid-cols-[minmax(8rem,12rem)_minmax(0,1fr)]", "sm:items-baseline", "font-medium",
                "min-w-0", "break-words", "text-slate-900", "dark:text-slate-100", "flex-col")), tokens.toString());
    }
}
