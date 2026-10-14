package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rewrite that renders a migrated Divider with its legacy classes must be exact, and must fail loudly rather than half-rewrite. */
class DividerLegacyMarkupTest {

    private static final String HORIZONTAL = "<hr class=\"ash-divider ash-divider--horizontal\" data-divider data-divider-orientation=\"horizontal\">";
    private static final String VERTICAL = "<div\n            role=\"separator\"\n            aria-orientation=\"vertical\"\n            class=\"ash-divider ash-divider--vertical\"\n"
            + "            data-divider\n            data-divider-orientation=\"vertical\"\n    ></div>";

    @Test
    void aHorizontalDividerBecomesItsExactLegacyClassesAndNothingElseChanges() throws IOException {
        DividerLegacyMarkup legacy = new DividerLegacyMarkup();
        String out = legacy.rewrite("<p>before</p>" + HORIZONTAL + "<p>after</p>");
        assertEquals("<p>before</p><hr class=\"" + legacy.classes("horizontal") + "\" data-divider data-divider-orientation=\"horizontal\"><p>after</p>", out);
        assertEquals("m-0 w-full border-0 border-t border-slate-200 dark:border-slate-700", legacy.classes("horizontal"));
    }

    @Test
    void aVerticalDividerSpanningSeveralLinesKeepsItsAttributesAndAriaExactly() throws IOException {
        DividerLegacyMarkup legacy = new DividerLegacyMarkup();
        String out = legacy.rewrite(VERTICAL);
        assertEquals(VERTICAL.replace("ash-divider ash-divider--vertical", legacy.classes("vertical")), out);
        assertTrue(out.contains("role=\"separator\"") && out.contains("aria-orientation=\"vertical\""), out);
        assertEquals("inline-block w-0 self-stretch border-l border-slate-200 dark:border-slate-700", legacy.classes("vertical"));
    }

    @Test
    void markupWithoutADividerAndOtherAshClassesAreLeftExactlyAsServed() throws IOException {
        String page = "<span class=\"ash-sr-only\">x</span><span class=\"ash-badge ash-badge--info\" data-badge></span><hr class=\"border-t\">"
                + "<p>ash-divider ash-divider--horizontal in prose</p>";
        assertEquals(page, new DividerLegacyMarkup().rewrite(page));
    }

    @Test
    void severalDividersAreEachRewrittenToTheirOwnOrientationAndCounted() throws IOException {
        DividerLegacyMarkup legacy = new DividerLegacyMarkup();
        String page = HORIZONTAL + "<div>" + VERTICAL + "</div>" + HORIZONTAL;
        assertEquals(3, DividerLegacyMarkup.count(page));
        String out = legacy.rewrite(page);
        assertEquals(0, DividerLegacyMarkup.count(out));
        assertFalse(out.contains("ash-divider"), out);
        assertEquals(2, out.split(legacy.classes("horizontal"), -1).length - 1);
        assertEquals(1, out.split(legacy.classes("vertical"), -1).length - 1);
    }

    @Test
    void aDividerOfTheWrongShapeFailsLoudlyInsteadOfBeingHalfRewritten() throws IOException {
        DividerLegacyMarkup legacy = new DividerLegacyMarkup();
        assertThrows(IllegalStateException.class, () -> legacy.rewrite(HORIZONTAL.replace("<hr", "<div")));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite(VERTICAL.replace("<div", "<hr")));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite(HORIZONTAL.replace("data-divider-orientation=\"horizontal\"", "data-divider-orientation=\"vertical\"")));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite(HORIZONTAL.replace("ash-divider--horizontal", "ash-divider--diagonal")));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite(HORIZONTAL.replace(" data-divider-orientation=\"horizontal\"", "")));
    }

    @Test
    void everyOrientationHasLegacyClassesAndTheTokensAreTheTenTheOracleMustCompile() throws IOException {
        DividerLegacyMarkup legacy = new DividerLegacyMarkup();
        for (String orientation : DividerLegacyMarkup.ORIENTATIONS) {
            assertFalse(legacy.classes(orientation).isBlank(), orientation);
        }
        assertEquals(10, legacy.allTokens().size(), legacy.allTokens().toString());
    }
}
