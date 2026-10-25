package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rewrite that renders a migrated Icon with its legacy classes must be exact, and must fail loudly rather than half-rewrite. */
class IconLegacyMarkupTest {

    @Test
    void anIconWithNoClassesBecomesTheLegacyDefaultIncludingItsFiveByFiveSize() throws IOException {
        IconLegacyMarkup legacy = new IconLegacyMarkup();
        assertEquals("inline-block shrink-0 h-5 w-5", legacy.legacyFor("ash-icon"));
        assertEquals("<svg class=\"inline-block shrink-0 h-5 w-5\" viewBox=\"0 0 24 24\"></svg>", legacy.rewrite("<svg class=\"ash-icon\" viewBox=\"0 0 24 24\"></svg>"));
    }

    @Test
    void callerClassesStayInOrderAndReplaceNothingBecauseTheLegacyClassesReplacedTheDefault() throws IOException {
        IconLegacyMarkup legacy = new IconLegacyMarkup();
        assertEquals("inline-block shrink-0 h-4 w-4", legacy.legacyFor("ash-icon h-4 w-4"));
        assertEquals("inline-block shrink-0 h-6 w-6 text-green-600 dark:text-green-400", legacy.legacyFor("ash-icon h-6 w-6 text-green-600 dark:text-green-400"));
        // The two contracts differ here, on purpose: added classes with no size used to leave the icon unsized and now get Ashlar's default size.
        assertEquals("inline-block shrink-0 text-slate-400", legacy.legacyFor("ash-icon text-slate-400"));
        assertEquals("inline-block shrink-0 ash-badge__icon", legacy.legacyFor("ash-icon ash-badge__icon"));
    }

    @Test
    void theShowcaseDemoClassForTheSizePropertyBecomesTheTailwindSizeOfTheSameThreeRem() throws IOException {
        IconLegacyMarkup legacy = new IconLegacyMarkup();
        assertEquals("inline-block shrink-0 h-12 w-12", legacy.legacyFor("ash-icon hero-icon"));
    }

    @Test
    void eachSizeModifierBecomesTheTailwindUtilitiesThatSizeWasWrittenWith() throws IOException {
        IconLegacyMarkup legacy = new IconLegacyMarkup();
        assertEquals("inline-block shrink-0 h-3 w-3", legacy.legacyFor("ash-icon ash-icon--xs"));
        assertEquals("inline-block shrink-0 h-4 w-4", legacy.legacyFor("ash-icon ash-icon--sm"));
        assertEquals("inline-block shrink-0 h-5 w-5", legacy.legacyFor("ash-icon ash-icon--md"));
        assertEquals("inline-block shrink-0 h-6 w-6", legacy.legacyFor("ash-icon ash-icon--lg"));
        assertEquals("inline-block shrink-0 h-8 w-8", legacy.legacyFor("ash-icon ash-icon--xl"));
        assertEquals("inline-block shrink-0 h-6 w-6 text-red-600", legacy.legacyFor("ash-icon ash-icon--lg text-red-600"));
    }

    @Test
    void onlyClassAttributesThatCarryTheAshIconTokenAreTouched() throws IOException {
        IconLegacyMarkup legacy = new IconLegacyMarkup();
        String page = "<span class=\"ash-sr-only\"></span><div class=\"flex ash-badge\"></div><p>ash-icon in text</p>"
                + "<svg class=\"ash-icon h-4 w-4\"></svg><i class=\"icon\"></i>";
        String out = legacy.rewrite(page);
        assertEquals(page.replace("class=\"ash-icon h-4 w-4\"", "class=\"inline-block shrink-0 h-4 w-4\""), out);
        assertNull(legacy.legacyFor("flex ash-badge"));
        assertNull(legacy.legacyFor(""));
    }

    @Test
    void severalIconsAreEachRewrittenAndCounted() throws IOException {
        IconLegacyMarkup legacy = new IconLegacyMarkup();
        String page = "<svg class=\"ash-icon\"></svg><svg class=\"ash-icon ash-icon--lg\"></svg><svg class=\"ash-icon h-4 w-4\"></svg>";
        assertEquals(3, IconLegacyMarkup.count(page));
        String out = legacy.rewrite(page);
        assertEquals(0, IconLegacyMarkup.count(out));
        assertEquals("<svg class=\"inline-block shrink-0 h-5 w-5\"></svg><svg class=\"inline-block shrink-0 h-6 w-6\"></svg><svg class=\"inline-block shrink-0 h-4 w-4\"></svg>", out);
    }

    @Test
    void anUnknownIconClassOrAModifierWithoutTheBaseClassFailsLoudly() throws IOException {
        IconLegacyMarkup legacy = new IconLegacyMarkup();
        assertThrows(IllegalStateException.class, () -> legacy.legacyFor("ash-icon ash-icon--huge"));
        assertThrows(IllegalStateException.class, () -> legacy.legacyFor("ash-icon ash-icon-extra"));
        assertThrows(IllegalStateException.class, () -> legacy.legacyFor("ash-icon--lg h-4"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<svg class=\"ash-icon ash-icon--huge\"></svg>"));
    }

    @Test
    void everyRoleHasLegacyClassesAndTheTokensAreTheOnesTheOracleMustCompile() throws IOException {
        IconLegacyMarkup legacy = new IconLegacyMarkup();
        for (String modifier : IconLegacyMarkup.MODIFIERS) {
            assertTrue(legacy.classes(modifier).startsWith("inline-block shrink-0 h-"), modifier);
        }
        assertEquals("inline-block shrink-0", legacy.classes("base"));
        assertEquals(Set.of("inline-block", "shrink-0", "h-3", "w-3", "h-4", "w-4", "h-5", "w-5", "h-6", "w-6", "h-8", "w-8", "h-10", "w-10", "h-12", "w-12", "text-green-600", "dark:text-green-400"), legacy.allTokens());
    }
}
