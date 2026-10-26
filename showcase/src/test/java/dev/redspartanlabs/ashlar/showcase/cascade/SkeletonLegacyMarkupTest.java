package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rewrite that renders a migrated Skeleton with its legacy classes must be exact, and must fail loudly rather than half-rewrite. */
class SkeletonLegacyMarkupTest {

    private static String skeleton(String classes, String shape) {
        return "<div aria-hidden=\"true\" class=\"" + classes + "\" style=\"width: 60%; height: 1rem\" data-skeleton data-skeleton-shape=\"" + shape + "\"></div>";
    }

    @Test
    void eachShapeBecomesItsExactLegacyClassesAndTheInlineSizeAndHooksAreUntouched() throws IOException {
        SkeletonLegacyMarkup legacy = new SkeletonLegacyMarkup();
        String page = "<p class=\"text-sm\">before</p>" + skeleton("ash-skeleton", "rectangle") + skeleton("ash-skeleton ash-skeleton--rounded", "rounded") + "<p>after</p>";
        String out = legacy.rewrite(page);
        assertEquals("<p class=\"text-sm\">before</p>" + skeleton(legacy.classes("rectangle"), "rectangle") + skeleton(legacy.classes("rounded"), "rounded") + "<p>after</p>", out);
        assertTrue(out.contains("style=\"width: 60%; height: 1rem\"") && out.contains("aria-hidden=\"true\"") && out.contains("data-skeleton-shape=\"rounded\""));
    }

    @Test
    void severalSkeletonsAreEachRewrittenAndCounted() throws IOException {
        SkeletonLegacyMarkup legacy = new SkeletonLegacyMarkup();
        String page = skeleton("ash-skeleton", "rectangle") + skeleton("ash-skeleton ash-skeleton--rounded", "rounded") + skeleton("ash-skeleton", "rectangle");
        assertEquals(3, SkeletonLegacyMarkup.count(page));
        assertEquals(0, SkeletonLegacyMarkup.count(legacy.rewrite(page)));
    }

    @Test
    void markupWithoutASkeletonAndOtherAshClassesAreLeftExactlyAsServed() throws IOException {
        String page = "<span class=\"ash-sr-only\">x</span><div class=\"ash-progress\" data-progress></div><p>ash-skeleton in prose</p><div class=\"flex flex-col\"></div>";
        assertEquals(page, new SkeletonLegacyMarkup().rewrite(page));
    }

    @Test
    void aSkeletonOfAnUnknownShapeFailsLoudlyInsteadOfBeingHalfRewritten() throws IOException {
        SkeletonLegacyMarkup legacy = new SkeletonLegacyMarkup();
        assertThrows(IllegalStateException.class, () -> legacy.rewrite(skeleton("ash-skeleton ash-skeleton--circle", "rounded")));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite(skeleton("ash-skeleton mt-4", "rectangle")));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite(skeleton("ash-skeleton--rounded", "rounded")));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite(skeleton("ash-skeleton-extra", "rectangle")));
    }

    @Test
    void everyShapeHasLegacyClassesAndTheTokensAreTheOnesTheOracleMustCompile() throws IOException {
        SkeletonLegacyMarkup legacy = new SkeletonLegacyMarkup();
        assertTrue(legacy.classes("rectangle").endsWith(" rounded") && legacy.classes("rounded").endsWith(" rounded-full"));
        assertEquals(Set.of("block", "shrink-0", "animate-pulse", "bg-slate-200", "motion-reduce:animate-none", "dark:bg-slate-700", "rounded", "rounded-full"), legacy.allTokens());
    }
}
