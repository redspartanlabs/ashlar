package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rewrite that renders a migrated Stepper and Stepper Item with their legacy classes must be exact, per state, and must fail loudly rather than half-rewrite. */
class StepperLegacyMarkupTest {

    private static String item(String state, String label, String status, boolean current) {
        return "<li" + (current ? " aria-current=\"step\"" : "") + " class=\"ash-stepper-item ash-stepper-item--" + state + "\">\n"
                + "  <div class=\"ash-stepper-item__connector\" aria-hidden=\"true\"><div class=\"ash-stepper-item__line\"></div></div>\n"
                + "  <span aria-hidden=\"true\" class=\"ash-stepper-item__indicator\">" + (state.equals("completed") ? "<svg class=\"ash-icon ash-icon--sm\"></svg>" : "") + "</span>\n"
                + "  <span class=\"ash-stepper-item__text\"><span class=\"ash-stepper-item__label\">" + label + "<span class=\"ash-sr-only\">, " + status + "</span></span></span>\n"
                + "</li>";
    }

    private static final String FULL = "<div class=\"ash-stepper\"><ol data-stepper class=\"ash-stepper__list\">\n"
            + item("completed", "Account", "Completed", false) + "\n" + item("current", "Details", "Current step", true) + "\n" + item("upcoming", "Review", "Not started", false)
            + "\n</ol></div>";

    @Test
    void everyPartBecomesTheExactLegacyClassesOfItsOwnStateAndNothingElseChanges() throws IOException {
        StepperLegacyMarkup legacy = new StepperLegacyMarkup();
        String migrated = "<p class=\"text-sm\">before</p>" + FULL + "<p>after</p>";
        String expected = migrated
                .replace("class=\"ash-stepper\"", "class=\"" + legacy.classes("stepper") + "\"")
                .replace("class=\"ash-stepper__list\"", "class=\"" + legacy.classes("list") + "\"")
                .replaceAll("class=\"ash-stepper-item ash-stepper-item--[a-z]+\"", "class=\"" + legacy.classes("item") + "\"")
                .replace("class=\"ash-stepper-item__connector\"", "class=\"" + legacy.classes("connector") + "\"")
                .replace("class=\"ash-stepper-item__text\"", "class=\"" + legacy.classes("text") + "\"")
                .replace("<span class=\"ash-sr-only\">", "<span class=\"" + legacy.classes("status") + "\">");
        // The state-dependent parts, in document order: completed, current, upcoming.
        for (String state : StepperLegacyMarkup.STATES) {
            expected = expected.replaceFirst("class=\"ash-stepper-item__line\"", "class=\"" + legacy.classes("line-" + state) + "\"");
            expected = expected.replaceFirst("class=\"ash-stepper-item__indicator\"", "class=\"" + legacy.classes("indicator-" + state) + "\"");
            expected = expected.replaceFirst("class=\"ash-stepper-item__label\"", "class=\"" + legacy.classes("label-" + state) + "\"");
        }
        String out = legacy.rewrite(migrated);
        assertEquals(expected, out);
        assertFalse(out.contains("ash-stepper") || out.contains("ash-sr-only"), out);
        // The hook, the accessibility attributes and the Icon (the Icon rewrite's job) are untouched.
        assertTrue(out.contains("data-stepper") && out.contains("aria-current=\"step\"") && out.contains("aria-hidden=\"true\"") && out.contains("<svg class=\"ash-icon ash-icon--sm\">"));
    }

    @Test
    void theLegacyPartsCarryTheStateThatStepperStylesUsedToReturnAndTheNumberOnlyOnStepsThatAreNotCompleted() throws IOException {
        StepperLegacyMarkup legacy = new StepperLegacyMarkup();
        assertFalse(legacy.classes("indicator-completed").contains("before:content-[counter(step)]"));
        assertTrue(legacy.classes("indicator-current").endsWith("before:content-[counter(step)]"));
        assertTrue(legacy.classes("indicator-upcoming").endsWith("before:content-[counter(step)]"));
        assertEquals("h-0.5 w-full bg-blue-600 dark:bg-blue-500", legacy.classes("line-completed"));
        assertEquals(legacy.classes("line-current"), legacy.classes("line-upcoming"));
        assertTrue(legacy.classes("label-current").contains("font-semibold"));
        assertEquals("@container", legacy.classes("stepper"));
        assertTrue(legacy.classes("item").contains("[counter-increment:step]") && legacy.classes("list").contains("[counter-reset:step]"));
        assertTrue(legacy.classes("connector").endsWith("@sm:group-last:hidden"));
        assertEquals("sr-only", legacy.classes("status"));
    }

    @Test
    void theStatusTextIsRewrittenOnlyInsideAStepperItemSoAnotherComponentsOwnSrOnlyIsLeftAlone() throws IOException {
        String out = new StepperLegacyMarkup().rewrite("<span class=\"ash-sr-only\">Loading</span>" + FULL);
        assertTrue(out.startsWith("<span class=\"ash-sr-only\">Loading</span>"), out);
        assertEquals(3, out.split("<span class=\"sr-only\">", -1).length - 1);
    }

    @Test
    void markupWithoutAStepperAndOtherAshClassesAreLeftExactlyAsServed() throws IOException {
        String page = "<span class=\"ash-sr-only\">x</span><div class=\"ash-progress\" data-progress></div><p>ash-stepper in prose</p><ol class=\"flex\"><li>raw</li></ol>"
                + "<li class=\"ash-timeline-item\"></li>";
        assertEquals(page, new StepperLegacyMarkup().rewrite(page));
    }

    @Test
    void severalSteppersAreEachRewrittenAndCounted() throws IOException {
        StepperLegacyMarkup legacy = new StepperLegacyMarkup();
        String page = FULL + FULL + "<div class=\"ash-stepper\"></div>";
        assertEquals(3, StepperLegacyMarkup.count(page));
        assertEquals(0, StepperLegacyMarkup.count(legacy.rewrite(page)));
    }

    @Test
    void anUnknownStepperClassFailsLoudlyInsteadOfBeingHalfRewritten() throws IOException {
        StepperLegacyMarkup legacy = new StepperLegacyMarkup();
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<div class=\"ash-stepper mt-4\"></div>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<ol class=\"ash-stepper__list ash-stepper__list--compact\"></ol>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<span class=\"ash-stepper-item__extra\"></span>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<li class=\"ash-stepper-item\"></li>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<li class=\"ash-stepper-item ash-stepper-item--broken\"></li>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<li class=\"ash-stepper-item ash-stepper-item--current\"><span class=\"ash-stepper-item__label extra\"></span></li>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<span class=\"ash-stepper-item__indicator\"></span>"));
    }

    @Test
    void everyRoleHasLegacyClassesAndTheTokensAreTheOnesTheOracleMustCompile() throws IOException {
        StepperLegacyMarkup legacy = new StepperLegacyMarkup();
        for (String role : StepperLegacyMarkup.ROLES) {
            assertFalse(legacy.classes(role).isBlank(), role);
        }
        assertTrue(legacy.allTokens().containsAll(Set.of("@container", "@sm:flex-row", "@sm:gap-0", "[counter-reset:step]", "[counter-increment:step]", "group",
                "@sm:flex-col", "@sm:items-center", "@sm:gap-2", "@sm:flex", "@sm:group-last:hidden", "top-4", "h-0.5", "w-full", "bg-blue-600", "dark:bg-blue-500",
                "z-10", "h-8", "w-8", "rounded-full", "border-2", "text-white", "dark:text-blue-400", "before:content-[counter(step)]", "@sm:text-center", "@sm:pt-0",
                "min-w-0", "flex-1", "break-words", "font-semibold", "sr-only")));
    }
}
