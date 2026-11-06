package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rewrite that turns migrated Tree and Tree Item markup back into the former Tailwind markup: what it changes, what it must leave alone, and that it fails loudly. */
class TreeLegacyMarkupTest {

    private static String item(boolean expanded, String inner) {
        return "<li role=\"treeitem\" aria-expanded=\"" + expanded + "\" class=\"ash-tree__item\"><div class=\"ash-tree__row\"><button type=\"button\" class=\"ash-tree__toggle\">"
                + "<span data-tree-chevron class=\"ash-tree__chevron\"><svg></svg></span></button><span class=\"ash-tree__label\">L</span></div>" + inner + "</li>";
    }

    private static String panel(boolean inert, String inner) {
        return "<div data-tree-children" + (inert ? " inert" : "") + " class=\"ash-tree__panel\"><div class=\"ash-tree__clip\"><ul role=\"group\" class=\"ash-tree__group\">" + inner + "</ul></div></div>";
    }

    @Test
    void eachChevronGetsTheRotatedStringOnlyWhileItsOwnItemIsExpandedHoweverDeepTheTreeIs() throws Exception {
        TreeLegacyMarkup legacy = new TreeLegacyMarkup();
        String open = legacy.rewrite(item(true, ""));
        String closed = legacy.rewrite(item(false, ""));
        assertTrue(open.contains("class=\"" + legacy.classes("tree-chevron-open") + "\"") && open.contains("rotate-90"), open);
        assertTrue(closed.contains("class=\"" + legacy.classes("tree-chevron") + "\"") && !closed.contains("rotate-90"), closed);
        // An expanded parent whose child is collapsed: only the parent's chevron turns. A collapsed parent whose child is expanded: only the child's chevron turns.
        String parentOpen = legacy.rewrite(item(true, panel(false, item(false, ""))));
        assertEquals(1, parentOpen.split("rotate-90", -1).length - 1, parentOpen);
        assertTrue(parentOpen.indexOf("rotate-90") < parentOpen.indexOf("<ul role=\"group\""), "the turned chevron is the parent's, which comes first: " + parentOpen);
        String childOpen = legacy.rewrite(item(false, panel(true, item(true, ""))));
        assertEquals(1, childOpen.split("rotate-90", -1).length - 1, childOpen);
        assertTrue(childOpen.indexOf("rotate-90") > childOpen.indexOf("<ul role=\"group\""), "the turned chevron is the child's: " + childOpen);
        String three = legacy.rewrite(item(true, panel(false, item(false, panel(true, item(true, ""))))));
        assertEquals(2, three.split("rotate-90", -1).length - 1, "the first and third chevrons turn, the middle one does not: " + three);
    }

    @Test
    void thePanelGetsItsInlineRowFromItsOwnInertAttributeAsTheOldTemplateRenderedIt() throws Exception {
        TreeLegacyMarkup legacy = new TreeLegacyMarkup();
        String collapsed = legacy.rewrite(panel(true, ""));
        String open = legacy.rewrite(panel(false, ""));
        assertTrue(collapsed.contains("<div data-tree-children inert style=\"grid-template-rows: 0fr\" class=\"" + legacy.classes("tree-panel") + "\">"), collapsed);
        assertTrue(open.contains("<div data-tree-children style=\"grid-template-rows: 1fr\" class=\"" + legacy.classes("tree-panel") + "\">"), open);
        assertTrue(collapsed.contains("class=\"" + legacy.classes("tree-clip") + "\"") && collapsed.contains("class=\"" + legacy.classes("tree-group") + "\""), collapsed);
        String nested = legacy.rewrite(panel(false, panel(true, "")));
        assertTrue(nested.indexOf("1fr") < nested.indexOf("0fr"), "each panel reads its own attribute: " + nested);
    }

    @Test
    void aLabelIsALinkStringOnAnAnchorAndAPlainStringOnASpan() throws Exception {
        TreeLegacyMarkup legacy = new TreeLegacyMarkup();
        String link = legacy.rewrite("<a href=\"#\" tabindex=\"-1\" class=\"ash-tree__label\">x</a>");
        String span = legacy.rewrite("<span class=\"ash-tree__label\">x</span>");
        assertTrue(link.contains("class=\"" + legacy.classes("tree-label-link") + "\"") && link.contains("hover:underline"), link);
        assertTrue(span.contains("class=\"" + legacy.classes("tree-label") + "\"") && !span.contains("hover:underline"), span);
        assertTrue(link.contains("href=\"#\" tabindex=\"-1\""), "the attributes are left as served");
    }

    @Test
    void everyOtherPartGetsItsLegacyStringAndNoMigratedClassSurvives() throws Exception {
        TreeLegacyMarkup legacy = new TreeLegacyMarkup();
        String out = legacy.rewrite("<ul role=\"tree\" data-tree class=\"ash-tree\">" + item(true, panel(false, item(false, ""))).replace("<span class=\"ash-tree__label\">", "<span class=\"ash-tree__spacer\"></span><span class=\"ash-tree__icon\"></span><span class=\"ash-tree__label\">") + "</ul>");
        assertTrue(out.startsWith("<ul role=\"tree\" data-tree class=\"" + legacy.classes("tree") + "\">"), out);
        for (String role : List.of("tree-item", "tree-row", "tree-toggle", "tree-spacer", "tree-icon", "tree-clip", "tree-group")) {
            assertTrue(out.contains("class=\"" + legacy.classes(role) + "\""), role + " in " + out);
        }
        assertFalse(out.contains("ash-tree"), "nothing carries a migrated class after the rewrite: " + out);
    }

    @Test
    void theRealPageProbeMarksNoTreeElement() throws Exception {
        String probe = new String(TreeLegacyMarkupTest.class.getResourceAsStream("/cascade/real-page-probe.js").readAllBytes(), StandardCharsets.UTF_8);
        assertFalse(probe.contains("ash-tree") || probe.contains("data-tree"), "the probe is unchanged by this migration and names no Tree element");
    }

    @Test
    void otherMarkupIsLeftExactlyAsServedAndAnUnknownTreeClassFailsLoudly() throws Exception {
        TreeLegacyMarkup legacy = new TreeLegacyMarkup();
        String other = "<div class=\"ash-card\"><span class=\"ash-sr-only\">x</span><a class=\"ash-treeish\">y</a><i class=\"ash-tree-view\"></i></div>";
        assertEquals(other, legacy.rewrite(other));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<span class=\"ash-tree__unknown\"></span>"));
    }

    @Test
    void everyLegacyTokenIsCountedForTheOraclesNonVacuityCheck() throws Exception {
        TreeLegacyMarkup legacy = new TreeLegacyMarkup();
        assertTrue(legacy.allTokens().containsAll(List.of("flex-col", "gap-0.5", "text-sm", "focus:outline-none", "rounded-md", "px-2", "py-1.5", "hover:bg-slate-100", "[li:focus-visible>&]:bg-blue-50",
                "[li:focus-visible>&]:ring-2", "dark:[li:focus-visible>&]:bg-blue-950/40", "cursor-pointer", "rotate-90", "motion-reduce:transition-none", "transition-[grid-template-rows]",
                "min-h-0", "overflow-hidden", "pl-5", "truncate", "hover:underline", "h-5", "w-5")));
    }

    @Test
    void countsOnlyExactParts() {
        assertEquals(2, TreeLegacyMarkup.count("<ul class=\"ash-tree__group\"></ul><ul class=\"ash-tree__group\"></ul><ul class=\"ash-tree__group extra\"></ul>", "tree-group"));
    }
}
