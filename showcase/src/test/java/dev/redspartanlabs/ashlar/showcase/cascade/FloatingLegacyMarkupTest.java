package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rewrite from migrated to legacy Tooltip and Popover markup: the classes of every part, placement and state, what only one form carries, and failing loudly on what it does not know. */
class FloatingLegacyMarkupTest {

    /** The class attribute of the first tag at or after {@code marker}. */
    private static String classOf(String html, String marker) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?<![\\w-])class=\"([^\"]*)\"").matcher(html);
        if (!m.find(html.indexOf(marker))) {
            throw new AssertionError("no class attribute after " + marker);
        }
        return m.group(1);
    }

    @Test
    void aClosedTooltipBecomesTheTemplatesOwnMarkupInEveryPlacement() throws Exception {
        FloatingLegacyMarkup legacy = new FloatingLegacyMarkup();
        for (String placement : FloatingBrowser.PLACEMENTS) {
            String p = placement.toLowerCase();
            String out = legacy.rewrite(FloatingBrowser.tooltip("<button id=\"b\">B</button>", "Tip", placement));
            assertEquals(legacy.classes("tooltip-root"), classOf(out, "<span"));
            assertEquals(legacy.classes("tooltip-trigger"), classOf(out, "data-tooltip-trigger"));
            assertEquals(legacy.classes("tooltip-bubble") + " " + legacy.classes("tooltip-position-" + p), classOf(out, "data-tooltip-bubble"));
            assertEquals(legacy.classes("tooltip-arrow") + " " + legacy.classes("tooltip-arrow-" + p), classOf(out, "data-tooltip-arrow"));
            assertFalse(out.contains("ash-tooltip"), "no migrated class is left");
            assertFalse(out.contains("data-state") || out.matches("(?s).*\\shidden\\s*>.*"), "neither the hidden attribute nor data-state is legacy markup");
            assertTrue(out.contains("role=\"tooltip\"") && out.contains("data-tooltip-placement=\"" + p + "\"") && out.contains("aria-hidden=\"true\""), "every hook and ARIA attribute is kept");
        }
    }

    @Test
    void anOpenSurfaceLosesHiddenAndSwapsExactlyTheClassesTheLegacyScriptSwapped() throws Exception {
        FloatingLegacyMarkup legacy = new FloatingLegacyMarkup();
        String open = classOf(legacy.rewrite(FloatingBrowser.inState(FloatingBrowser.tooltip("<b>t</b>", "Tip", null), true, true)), "data-tooltip-bubble");
        assertTrue(open.contains("opacity-100") && open.contains("scale-100") && !open.contains("opacity-0") && !open.contains("scale-95") && !open.matches(".*(^| )hidden( |$).*"), open);
        String shownButClosed = classOf(legacy.rewrite(FloatingBrowser.inState(FloatingBrowser.popover("<b>p</b>", "<p>c</p>", null, null), true, false)), "data-popover-surface");
        assertTrue(shownButClosed.contains("opacity-0") && shownButClosed.contains("scale-95") && !shownButClosed.matches(".*(^| )hidden( |$).*"), shownButClosed);
        String hidden = classOf(legacy.rewrite(FloatingBrowser.popover("<b>p</b>", "<p>c</p>", null, null)), "data-popover-surface");
        assertTrue(hidden.matches(".*(^| )hidden( |$).*") && hidden.contains("opacity-0"), hidden);
    }

    @Test
    void aPopoverGetsItsPlacementItsTitleAndItsBodyAndKeepsAnInlineStyle() throws Exception {
        FloatingLegacyMarkup legacy = new FloatingLegacyMarkup();
        for (String placement : FloatingBrowser.PLACEMENTS) {
            String out = legacy.rewrite(FloatingBrowser.popover("<button id=\"b\">B</button>", "<p>Body</p>", "Title", placement));
            assertEquals(legacy.classes("popover-root"), classOf(out, "<span"));
            assertEquals(legacy.classes("popover-surface") + " " + legacy.classes("popover-position-" + placement.toLowerCase()), classOf(out, "data-popover-surface"));
            assertTrue(out.contains("class=\"" + legacy.classes("popover-title") + "\">Title</h3>"), out);
            assertTrue(out.contains("class=\"" + legacy.classes("popover-body") + "\""), out);
        }
        String nudged = legacy.rewrite(FloatingBrowser.nudged(FloatingBrowser.tooltip("<b>t</b>", "Tip", null)));
        assertTrue(nudged.contains("style=\"translate: calc(-50% + 40px)\"") && nudged.contains("style=\"translate: calc(-50% - 40px)\""), "an inline style is kept");
    }

    @Test
    void markupWithoutATooltipOrPopoverIsUntouchedAndAnUnknownClassFails() throws Exception {
        FloatingLegacyMarkup legacy = new FloatingLegacyMarkup();
        String plain = "<div class=\"ash-card\"><p class=\"x\">text</p><span class=\"ash-overlay__body\"></span></div>";
        assertEquals(plain, legacy.rewrite(plain));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<div class=\"ash-tooltip__unknown\"></div>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<div class=\"ash-popover__surface ash-popover__surface--top\"></div>"), "a surface without data-state");
    }

    @Test
    void theFixtureDefinesEveryRoleOnceAndItsTokensAreTheLegacyUtilities() throws Exception {
        FloatingLegacyMarkup legacy = new FloatingLegacyMarkup();
        assertEquals(FloatingLegacyMarkup.ROLES.size(), FloatingLegacyMarkup.ROLES.stream().distinct().count());
        assertTrue(legacy.allTokens().containsAll(List.of("max-w-64", "w-72", "rotate-45", "-translate-x-1/2", "-translate-y-1/2", "ring-slate-200", "motion-reduce:transition-none",
                "opacity-100", "scale-100", "max-w-[calc(100vw-1rem)]")));
        assertTrue(legacy.allTokens().stream().noneMatch(t -> t.startsWith("ash-")), "the legacy markup has no Ashlar class");
    }
}
