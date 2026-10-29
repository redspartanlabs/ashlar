package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rewrite that renders a migrated Pagination bar with its legacy classes must be exact, per part, and must fail loudly rather than half-rewrite. */
class PaginationLegacyMarkupTest {

    private static final String LINK = "ash-pagination-item__control ash-pagination-item__control--link";
    private static final String CURRENT = "ash-pagination-item__control ash-pagination-item__control--current";
    private static final String DISABLED = "ash-pagination-item__control ash-pagination-item__control--disabled";

    private static final String FULL = "<nav aria-label=\"Pagination\" data-pagination class=\"ash-pagination\"><ul class=\"ash-pagination__list\">\n"
            + "<li class=\"ash-pagination-item\"><span aria-disabled=\"true\" class=\"" + DISABLED + "\">Previous</span></li>\n"
            + "<li class=\"ash-pagination-item\"><a href=\"?page=1\" class=\"" + LINK + "\">1</a></li>\n"
            + "<li class=\"ash-pagination-item\"><span aria-current=\"page\" class=\"" + CURRENT + "\">2</span></li>\n"
            + "</ul></nav>";

    @Test
    void everyPartBecomesTheExactLegacyClassesAndNothingElseChanges() throws IOException {
        PaginationLegacyMarkup legacy = new PaginationLegacyMarkup();
        String migrated = "<p class=\"text-sm\">before</p>" + FULL + "<p>after</p>";
        String expected = migrated
                .replace(" class=\"ash-pagination\">", ">")
                .replace("<li class=\"ash-pagination-item\">", "<li>")
                .replace("class=\"ash-pagination__list\"", "class=\"" + legacy.classes("list") + "\"")
                .replace("class=\"" + LINK + "\"", "class=\"" + legacy.classes("link") + "\"")
                .replace("class=\"" + CURRENT + "\"", "class=\"" + legacy.classes("current") + "\"")
                .replace("class=\"" + DISABLED + "\"", "class=\"" + legacy.classes("disabled") + "\"");
        String out = legacy.rewrite(migrated);
        assertEquals(expected, out);
        assertFalse(out.contains("ash-pagination"), out);
        assertTrue(out.contains("<nav aria-label=\"Pagination\" data-pagination><ul class=\""), "the legacy landmark carried no class: " + out);
        assertTrue(out.contains("<li><a "), "the legacy <li> carried no class: " + out);
        // The hooks and the accessibility attributes are untouched.
        assertTrue(out.contains("data-pagination") && out.contains("aria-current=\"page\"") && out.contains("aria-disabled=\"true\"") && out.contains("aria-label=\"Pagination\""));
    }

    @Test
    void theLegacyPartsCarryTheClassesTheTemplatesUsedToWrite() throws IOException {
        PaginationLegacyMarkup legacy = new PaginationLegacyMarkup();
        assertEquals("flex flex-wrap items-center gap-1.5 text-sm", legacy.classes("list"));
        assertTrue(legacy.classes("link").contains("hover:bg-slate-50") && legacy.classes("link").contains("focus-visible:ring-blue-500") && legacy.classes("link").contains("rounded-md"));
        assertTrue(legacy.classes("current").contains("bg-blue-600") && legacy.classes("current").contains("text-white"));
        assertFalse(legacy.classes("disabled").contains("border") || legacy.classes("disabled").contains("rounded"), "a disabled entry has neither border nor radius");
        assertTrue(legacy.classes("disabled").contains("text-slate-300") && legacy.classes("disabled").contains("dark:text-slate-600"));
    }

    @Test
    void markupWithoutAPaginationBarAndOtherAshClassesAreLeftExactlyAsServed() throws IOException {
        String page = "<span class=\"ash-sr-only\">x</span><ol class=\"ash-timeline\"><li class=\"ash-timeline-item\"></li></ol><p>ash-pagination in prose</p>"
                + "<nav class=\"main\">raw</nav><li class=\"ash-breadcrumb-item\"></li><a class=\"text-blue-700\">raw</a>";
        assertEquals(page, new PaginationLegacyMarkup().rewrite(page));
    }

    @Test
    void severalBarsAreEachRewrittenAndCounted() throws IOException {
        PaginationLegacyMarkup legacy = new PaginationLegacyMarkup();
        String page = FULL + FULL + "<nav class=\"ash-pagination\"></nav>";
        assertEquals(3, PaginationLegacyMarkup.count(page));
        assertEquals(0, PaginationLegacyMarkup.count(legacy.rewrite(page)));
    }

    @Test
    void anUnknownPaginationClassFailsLoudlyInsteadOfBeingHalfRewritten() throws IOException {
        PaginationLegacyMarkup legacy = new PaginationLegacyMarkup();
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<ul class=\"ash-pagination__list mt-4\"></ul>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<nav class=\"ash-pagination mt-4\"></nav>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<ul class=\"ash-pagination__list ash-pagination--compact\"></ul>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<li class=\"ash-pagination-item extra\"></li>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<span class=\"ash-pagination-item__extra\"></span>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<span class=\"ash-pagination-item__control--current\"></span>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<span class=\"ash-pagination-item__control\"></span>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<span class=\"" + CURRENT + " ash-pagination-item__control--broken\"></span>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<div class=\"ash-pagination-item\"></div>"));
    }

    @Test
    void everyRoleHasLegacyClassesAndTheTokensAreTheOnesTheOracleMustCompile() throws IOException {
        PaginationLegacyMarkup legacy = new PaginationLegacyMarkup();
        for (String role : PaginationLegacyMarkup.ROLES) {
            assertFalse(legacy.classes(role).isBlank(), role);
        }
        assertTrue(legacy.allTokens().containsAll(Set.of("flex", "flex-wrap", "items-center", "gap-1.5", "text-sm", "inline-flex", "h-8", "min-w-8", "justify-center",
                "rounded-md", "border", "border-slate-200", "px-2", "font-medium", "text-slate-600", "hover:bg-slate-50", "hover:text-slate-900", "focus:outline-none",
                "focus-visible:ring-2", "focus-visible:ring-blue-500", "dark:border-slate-700", "dark:text-slate-300", "dark:hover:bg-slate-800", "dark:hover:text-slate-100",
                "border-blue-600", "bg-blue-600", "text-white", "dark:border-blue-500", "dark:bg-blue-500", "text-slate-300", "dark:text-slate-600",
                "bg-slate-900", "dark:bg-slate-100", "bg-slate-50", "dark:bg-slate-800")));
    }
}
