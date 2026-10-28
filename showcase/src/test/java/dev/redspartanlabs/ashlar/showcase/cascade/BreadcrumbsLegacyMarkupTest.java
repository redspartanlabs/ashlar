package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rewrite that renders a migrated Breadcrumbs trail with its legacy classes must be exact, per part, and must fail loudly rather than half-rewrite. */
class BreadcrumbsLegacyMarkupTest {

    private static final String FULL = "<nav aria-label=\"Breadcrumb\" data-breadcrumbs class=\"ash-breadcrumbs\"><ol class=\"ash-breadcrumbs__list\">\n"
            + "<li class=\"ash-breadcrumb-item\"><span aria-hidden=\"true\" data-breadcrumb-separator class=\"ash-breadcrumb-item__separator\">/</span>"
            + "<a href=\"/\" class=\"ash-breadcrumb-item__link\">Home</a></li>\n"
            + "<li class=\"ash-breadcrumb-item\"><span aria-hidden=\"true\" data-breadcrumb-separator class=\"ash-breadcrumb-item__separator\">/</span>"
            + "<span class=\"ash-breadcrumb-item__text\">Section</span></li>\n"
            + "<li class=\"ash-breadcrumb-item\"><span aria-hidden=\"true\" data-breadcrumb-separator class=\"ash-breadcrumb-item__separator\">/</span>"
            + "<span aria-current=\"page\" class=\"ash-breadcrumb-item__text ash-breadcrumb-item__text--current\">Widgets</span></li>\n"
            + "</ol></nav>";

    @Test
    void everyPartBecomesTheExactLegacyClassesAndNothingElseChanges() throws IOException {
        BreadcrumbsLegacyMarkup legacy = new BreadcrumbsLegacyMarkup();
        String migrated = "<p class=\"text-sm\">before</p>" + FULL + "<p>after</p>";
        String expected = migrated
                .replace(" class=\"ash-breadcrumbs\">", ">")
                .replace("class=\"ash-breadcrumbs__list\"", "class=\"" + legacy.classes("list") + "\"")
                .replace("class=\"ash-breadcrumb-item\"", "class=\"" + legacy.classes("item") + "\"")
                .replace("class=\"ash-breadcrumb-item__separator\"", "class=\"" + legacy.classes("separator") + "\"")
                .replace("class=\"ash-breadcrumb-item__link\"", "class=\"" + legacy.classes("link") + "\"")
                .replace("class=\"ash-breadcrumb-item__text ash-breadcrumb-item__text--current\"", "class=\"" + legacy.classes("current") + "\"")
                .replace("class=\"ash-breadcrumb-item__text\"", "class=\"" + legacy.classes("text") + "\"");
        String out = legacy.rewrite(migrated);
        assertEquals(expected, out);
        assertFalse(out.contains("ash-breadcrumb"), out);
        assertTrue(out.contains("<nav aria-label=\"Breadcrumb\" data-breadcrumbs><ol class=\""), "the legacy landmark carried no class: " + out);
        // The hooks and the accessibility attributes are untouched.
        assertTrue(out.contains("data-breadcrumbs") && out.contains("data-breadcrumb-separator") && out.contains("aria-current=\"page\"")
                && out.contains("aria-hidden=\"true\"") && out.contains("aria-label=\"Breadcrumb\""));
    }

    @Test
    void theLegacyPartsCarryTheClassesTheTemplatesUsedToWrite() throws IOException {
        BreadcrumbsLegacyMarkup legacy = new BreadcrumbsLegacyMarkup();
        assertEquals("flex flex-wrap items-center gap-x-1.5 gap-y-1 text-sm", legacy.classes("list"));
        assertEquals("group flex items-center gap-1.5", legacy.classes("item"));
        assertTrue(legacy.classes("separator").startsWith("group-first:hidden"));
        assertTrue(legacy.classes("link").contains("hover:underline") && legacy.classes("link").contains("focus-visible:ring-blue-500") && legacy.classes("link").contains("rounded-sm"));
        assertTrue(legacy.classes("current").contains("font-medium"));
        assertFalse(legacy.classes("text").contains("font-medium"));
    }

    @Test
    void markupWithoutABreadcrumbTrailAndOtherAshClassesAreLeftExactlyAsServed() throws IOException {
        String page = "<span class=\"ash-sr-only\">x</span><ol class=\"ash-timeline\"><li class=\"ash-timeline-item\"></li></ol><p>ash-breadcrumbs in prose</p><nav class=\"main\">raw</nav><a class=\"text-blue-700\">raw</a>";
        assertEquals(page, new BreadcrumbsLegacyMarkup().rewrite(page));
    }

    @Test
    void severalTrailsAreEachRewrittenAndCounted() throws IOException {
        BreadcrumbsLegacyMarkup legacy = new BreadcrumbsLegacyMarkup();
        String page = FULL + FULL + "<nav class=\"ash-breadcrumbs\"></nav>";
        assertEquals(3, BreadcrumbsLegacyMarkup.count(page));
        assertEquals(0, BreadcrumbsLegacyMarkup.count(legacy.rewrite(page)));
    }

    @Test
    void anUnknownBreadcrumbClassFailsLoudlyInsteadOfBeingHalfRewritten() throws IOException {
        BreadcrumbsLegacyMarkup legacy = new BreadcrumbsLegacyMarkup();
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<ol class=\"ash-breadcrumbs__list mt-4\"></ol>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<nav class=\"ash-breadcrumbs mt-4\"></nav>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<ol class=\"ash-breadcrumbs__list ash-breadcrumbs--compact\"></ol>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<li class=\"ash-breadcrumb-item extra\"></li>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<span class=\"ash-breadcrumb-item__extra\"></span>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<span class=\"ash-breadcrumb-item__text--current\"></span>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<span class=\"ash-breadcrumb-item__text ash-breadcrumb-item__text--broken\"></span>"));
    }

    @Test
    void everyRoleHasLegacyClassesAndTheTokensAreTheOnesTheOracleMustCompile() throws IOException {
        BreadcrumbsLegacyMarkup legacy = new BreadcrumbsLegacyMarkup();
        for (String role : BreadcrumbsLegacyMarkup.ROLES) {
            assertFalse(legacy.classes(role).isBlank(), role);
        }
        assertTrue(legacy.allTokens().containsAll(Set.of("flex", "flex-wrap", "items-center", "gap-x-1.5", "gap-y-1", "text-sm", "group", "gap-1.5",
                "group-first:hidden", "text-slate-400", "dark:text-slate-600", "break-words", "text-slate-500", "rounded-sm", "hover:text-slate-700",
                "hover:underline", "underline-offset-2", "focus:outline-none", "focus-visible:ring-2", "focus-visible:ring-blue-500", "dark:text-slate-400",
                "dark:hover:text-slate-200", "font-medium", "text-slate-900", "dark:text-slate-100", "bg-slate-700", "dark:bg-slate-200")));
    }
}
