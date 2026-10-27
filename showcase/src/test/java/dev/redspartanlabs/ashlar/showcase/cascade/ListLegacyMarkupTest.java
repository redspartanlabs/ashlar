package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rewrite that renders a migrated List and List Item with their legacy classes must be exact, and must fail loudly rather than half-rewrite. */
class ListLegacyMarkupTest {

    private static final String FULL = "<ul class=\"ash-list\">\n"
            + "  <li class=\"ash-list-item\">\n"
            + "    <a href=\"#\" class=\"ash-list-item__row ash-list-item__row--link\">\n"
            + "      <span class=\"ash-list-item__icon\" aria-hidden=\"true\"><svg class=\"ash-icon ash-icon--md\"></svg></span>\n"
            + "      <span class=\"ash-list-item__body\"><span class=\"ash-list-item__label\">Account</span><span class=\"ash-list-item__description\">Profile</span></span>\n"
            + "      <span class=\"ash-list-item__trailing\"><span class=\"font-medium\">Pro</span></span>\n"
            + "      <span class=\"ash-list-item__chevron\" aria-hidden=\"true\"><svg class=\"ash-icon ash-icon--sm\"></svg></span>\n"
            + "    </a>\n  </li>\n"
            + "  <li class=\"ash-list-item\"><div class=\"ash-list-item__row\"><span class=\"ash-list-item__body\"><span class=\"ash-list-item__label\">Plain</span></span></div></li>\n"
            + "</ul>";

    @Test
    void everyPartBecomesItsExactLegacyClassesTheLiLosesItsClassAndNothingElseChanges() throws IOException {
        ListLegacyMarkup legacy = new ListLegacyMarkup();
        String migrated = "<p class=\"text-sm\">before</p>" + FULL + "<ol class=\"ash-list\"></ol><p>after</p>";
        String expected = migrated
                .replace("<li class=\"ash-list-item\">", "<li>")
                .replace("class=\"ash-list\"", "class=\"" + legacy.classes("list") + "\"")
                .replace("class=\"ash-list-item__row ash-list-item__row--link\"", "class=\"" + legacy.classes("row-link") + "\"")
                .replace("class=\"ash-list-item__row\"", "class=\"" + legacy.classes("row") + "\"")
                .replace("class=\"ash-list-item__icon\"", "class=\"" + legacy.classes("icon") + "\"")
                .replace("class=\"ash-list-item__body\"", "class=\"" + legacy.classes("body") + "\"")
                .replace("class=\"ash-list-item__label\"", "class=\"" + legacy.classes("label") + "\"")
                .replace("class=\"ash-list-item__description\"", "class=\"" + legacy.classes("description") + "\"")
                .replace("class=\"ash-list-item__trailing\"", "class=\"" + legacy.classes("trailing") + "\"")
                .replace("class=\"ash-list-item__chevron\"", "class=\"" + legacy.classes("chevron") + "\"");
        assertEquals(expected, legacy.rewrite(migrated));
        String out = legacy.rewrite(migrated);
        assertFalse(out.contains("ash-list"));
        // The caller's own content, the Icons (the Icon rewrite's job) and the accessibility attributes are untouched.
        assertTrue(out.contains("<span class=\"font-medium\">Pro</span>") && out.contains("<svg class=\"ash-icon ash-icon--md\">") && out.contains("aria-hidden=\"true\""));
    }

    @Test
    void theLinkRowIsThePlainRowPlusTheInteractiveClasses() throws IOException {
        ListLegacyMarkup legacy = new ListLegacyMarkup();
        assertTrue(legacy.classes("row-link").startsWith(legacy.classes("row") + " hover:bg-slate-50 "));
        assertEquals("flex items-center gap-3 px-4 py-3 text-sm", legacy.classes("row"));
    }

    @Test
    void markupWithoutAListAndOtherAshClassesAreLeftExactlyAsServed() throws IOException {
        String page = "<span class=\"ash-sr-only\">x</span><div class=\"ash-progress\" data-progress></div><p>ash-list in prose</p><ul class=\"flex\"><li>raw</li></ul>";
        assertEquals(page, new ListLegacyMarkup().rewrite(page));
    }

    @Test
    void severalListsAreEachRewrittenAndCounted() throws IOException {
        ListLegacyMarkup legacy = new ListLegacyMarkup();
        String page = FULL + FULL + "<ol class=\"ash-list\"><li>raw</li></ol>";
        assertEquals(3, ListLegacyMarkup.count(page));
        assertEquals(0, ListLegacyMarkup.count(legacy.rewrite(page)));
    }

    @Test
    void anUnknownListClassFailsLoudlyInsteadOfBeingHalfRewritten() throws IOException {
        ListLegacyMarkup legacy = new ListLegacyMarkup();
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<ul class=\"ash-list ash-list--compact\"></ul>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<ul class=\"ash-list mt-4\"></ul>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<span class=\"ash-list-item__extra\"></span>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<div class=\"ash-list-item__row ash-list-item__row--link extra\"></div>"));
    }

    @Test
    void everyRoleHasLegacyClassesAndTheTokensAreTheOnesTheOracleMustCompile() throws IOException {
        ListLegacyMarkup legacy = new ListLegacyMarkup();
        for (String role : ListLegacyMarkup.ROLES) {
            assertFalse(legacy.classes(role).isBlank(), role);
            assertTrue(ListLegacyMarkup.MIGRATED.containsKey(role), role);
        }
        assertEquals("bg-slate-50 dark:bg-slate-800/50", legacy.classes("hover-swatch"));
        assertTrue(legacy.allTokens().containsAll(Set.of("divide-y", "divide-slate-200", "overflow-hidden", "rounded-lg", "border", "dark:divide-slate-700",
                "hover:bg-slate-50", "focus:outline-none", "focus-visible:ring-2", "focus-visible:ring-inset", "focus-visible:ring-blue-500",
                "dark:hover:bg-slate-800/50", "min-w-0", "flex-1", "font-medium", "text-slate-900", "dark:text-slate-100", "shrink-0", "block")));
    }
}
