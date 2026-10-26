package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rewrite that renders a migrated Spinner with its legacy classes must be exact, and must fail loudly rather than half-rewrite. */
class SpinnerLegacyMarkupTest {

    private static String spinner(String size, boolean labelled) {
        return "<span class=\"ash-spinner\"" + (labelled ? " role=\"status\" aria-live=\"polite\"" : "") + " data-spinner data-spinner-size=\"" + size + "\">\n"
                + "    <span aria-hidden=\"true\" class=\"ash-spinner__ring ash-spinner__ring--" + size + "\"></span>\n"
                + (labelled ? "    <span class=\"ash-sr-only\">Loading</span>\n" : "")
                + "</span>";
    }

    @Test
    void everyPartOfASpinnerBecomesItsExactLegacyClassesAndNothingElseChanges() throws IOException {
        SpinnerLegacyMarkup legacy = new SpinnerLegacyMarkup();
        for (String size : SpinnerLegacyMarkup.SIZES) {
            String migrated = "<p class=\"text-sm\">before</p>" + spinner(size, true) + "<p>after</p>";
            String expected = migrated
                    .replace("class=\"ash-spinner\"", "class=\"" + legacy.classes("wrapper") + "\"")
                    .replace("class=\"ash-spinner__ring ash-spinner__ring--" + size + "\"", "class=\"" + legacy.classes("ring-" + size) + "\"")
                    .replace("class=\"ash-sr-only\"", "class=\"" + legacy.classes("label") + "\"");
            assertEquals(expected, legacy.rewrite(migrated), size);
        }
    }

    @Test
    void anUnlabelledSpinnerHasNoLabelToRewriteAndKeepsItsAttributes() throws IOException {
        SpinnerLegacyMarkup legacy = new SpinnerLegacyMarkup();
        String out = legacy.rewrite(spinner("small", false));
        assertTrue(out.startsWith("<span class=\"" + legacy.classes("wrapper") + "\" data-spinner data-spinner-size=\"small\">"), out);
        assertFalse(out.contains("sr-only"));
        assertTrue(out.contains("aria-hidden=\"true\""));
    }

    @Test
    void theLabelsClassIsOnlyRewrittenInsideASpinnerAndOtherAshClassesAreLeftAlone() throws IOException {
        SpinnerLegacyMarkup legacy = new SpinnerLegacyMarkup();
        String page = "<span class=\"ash-sr-only\">elsewhere</span><div class=\"ash-progress\"></div><p>ash-spinner in prose</p>" + spinner("large", true);
        String out = legacy.rewrite(page);
        assertTrue(out.startsWith("<span class=\"ash-sr-only\">elsewhere</span><div class=\"ash-progress\"></div><p>ash-spinner in prose</p>"));
        assertTrue(out.contains("class=\"" + legacy.classes("label") + "\">Loading</span>"));
    }

    @Test
    void severalSpinnersAreEachRewrittenAndCounted() throws IOException {
        SpinnerLegacyMarkup legacy = new SpinnerLegacyMarkup();
        String page = spinner("small", false) + spinner("medium", true) + spinner("xlarge", true);
        assertEquals(3, SpinnerLegacyMarkup.count(page));
        String out = legacy.rewrite(page);
        assertEquals(0, SpinnerLegacyMarkup.count(out));
        assertEquals(3, out.split("data-spinner ", -1).length - 1);
    }

    @Test
    void aSpinnerOfTheWrongShapeFailsLoudlyInsteadOfBeingHalfRewritten() throws IOException {
        SpinnerLegacyMarkup legacy = new SpinnerLegacyMarkup();
        assertThrows(IllegalStateException.class, () -> legacy.rewrite(spinner("small", true).replace("ring--small", "ring--tiny")));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite(spinner("small", true).replace("ash-spinner\"", "ash-spinner extra\"")));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite(spinner("small", true).replace("<span aria-hidden=\"true\" ", "<span ")));
    }

    @Test
    void everyRoleHasLegacyClassesAndTheTokensAreTheOnesTheOracleMustCompile() throws IOException {
        SpinnerLegacyMarkup legacy = new SpinnerLegacyMarkup();
        assertEquals("inline-flex items-center", legacy.classes("wrapper"));
        assertEquals("sr-only", legacy.classes("label"));
        assertTrue(legacy.classes("ring-large").endsWith(" h-8 w-8 border-[3px]"));
        assertEquals(Set.of("inline-flex", "items-center", "sr-only", "inline-block", "shrink-0", "animate-spin", "rounded-full", "border-slate-200",
                "border-t-blue-600", "motion-reduce:animate-none", "dark:border-slate-700", "dark:border-t-blue-500",
                "h-4", "w-4", "border-2", "h-6", "w-6", "h-8", "w-8", "border-[3px]", "h-12", "w-12", "border-4"), legacy.allTokens());
    }
}
