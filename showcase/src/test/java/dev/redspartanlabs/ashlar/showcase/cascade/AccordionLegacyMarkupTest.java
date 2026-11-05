package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rewrite that turns migrated Accordion and Accordion Item markup back into the former Tailwind markup: what it changes, what it must leave alone, and that it fails loudly. */
class AccordionLegacyMarkupTest {

    private static String button(boolean expanded) {
        return "<button type=\"button\" aria-expanded=\"" + expanded + "\" class=\"ash-accordion__button\"><span class=\"ash-accordion__title\">T</span>"
                + "<span data-accordion-chevron class=\"ash-accordion__chevron\"><svg></svg></span></button>";
    }

    private static String panel(boolean inert) {
        return "<div role=\"region\"" + (inert ? " inert" : "") + " class=\"ash-accordion__panel\"><div class=\"ash-accordion__clip\"><div class=\"ash-accordion__content\">x</div></div></div>";
    }

    @Test
    void theChevronGetsTheRotatedStringOnlyWhileItsOwnButtonIsExpanded() throws Exception {
        AccordionLegacyMarkup legacy = new AccordionLegacyMarkup();
        String open = legacy.rewrite(button(true));
        String closed = legacy.rewrite(button(false));
        assertTrue(open.contains("class=\"" + legacy.classes("accordion-chevron-open") + "\"") && open.contains("rotate-90"), open);
        assertTrue(closed.contains("class=\"" + legacy.classes("accordion-chevron") + "\"") && !closed.contains("rotate-90"), closed);
        assertTrue(open.contains("class=\"" + legacy.classes("accordion-button") + "\"") && open.contains("aria-expanded=\"true\""), "the hooks are left as served");
        String two = legacy.rewrite(button(true) + button(false));
        assertEquals(1, two.split("rotate-90", -1).length - 1, "each chevron follows its own button: " + two);
    }

    @Test
    void thePanelGetsItsInlineRowFromTheInertAttributeAsTheOldTemplateRenderedIt() throws Exception {
        AccordionLegacyMarkup legacy = new AccordionLegacyMarkup();
        String collapsed = legacy.rewrite(panel(true));
        String open = legacy.rewrite(panel(false));
        assertTrue(collapsed.contains("<div role=\"region\" inert style=\"grid-template-rows: 0fr\" class=\"" + legacy.classes("accordion-panel") + "\">"), collapsed);
        assertTrue(open.contains("<div role=\"region\" style=\"grid-template-rows: 1fr\" class=\"" + legacy.classes("accordion-panel") + "\">"), open);
        assertTrue(collapsed.contains("class=\"" + legacy.classes("accordion-clip") + "\"") && collapsed.contains("class=\"" + legacy.classes("accordion-content") + "\""), collapsed);
    }

    @Test
    void theItemTheHeadingAndTheTitleHadNoClassAndTheirWholeAttributeIsRemoved() throws Exception {
        AccordionLegacyMarkup legacy = new AccordionLegacyMarkup();
        String out = legacy.rewrite("<div id=\"g\" data-accordion class=\"ash-accordion\"><div data-accordion-item class=\"ash-accordion__item\"><h3 class=\"ash-accordion__heading\">"
                + button(false) + "</h3></div></div>");
        assertTrue(out.startsWith("<div id=\"g\" data-accordion class=\"" + legacy.classes("accordion") + "\"><div data-accordion-item><h3><button"), out);
        assertTrue(out.contains("<span>T</span>"), out);
        assertFalse(out.contains("ash-accordion"), out);
    }

    @Test
    void theRealPageProbeMarksNoAccordionElement() throws Exception {
        String probe = new String(AccordionLegacyMarkupTest.class.getResourceAsStream("/cascade/real-page-probe.js").readAllBytes(), StandardCharsets.UTF_8);
        assertFalse(probe.contains("ash-accordion") || probe.contains("data-accordion"), "the probe is unchanged by this migration and names no Accordion element");
        AccordionLegacyMarkup legacy = new AccordionLegacyMarkup();
        String out = legacy.rewrite("<div class=\"ash-accordion\"><div class=\"ash-accordion__item\"><h3 class=\"ash-accordion__heading\">" + button(true) + "</h3>" + panel(false) + "</div></div>");
        assertFalse(out.contains("ash-accordion"), "nothing carries a migrated class after the rewrite: " + out);
    }

    @Test
    void otherMarkupIsLeftExactlyAsServedAndAnUnknownAccordionClassFailsLoudly() throws Exception {
        AccordionLegacyMarkup legacy = new AccordionLegacyMarkup();
        String other = "<div class=\"ash-card\"><span class=\"ash-sr-only\">x</span><a class=\"ash-accordionish\">y</a></div>";
        assertEquals(other, legacy.rewrite(other));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<span class=\"ash-accordion__unknown\"></span>"));
    }

    @Test
    void everyLegacyTokenIsCountedForTheOraclesNonVacuityCheck() throws Exception {
        AccordionLegacyMarkup legacy = new AccordionLegacyMarkup();
        assertTrue(legacy.allTokens().containsAll(List.of("divide-y", "divide-slate-200", "dark:divide-slate-700", "rounded-lg", "overflow-hidden", "justify-between", "hover:bg-slate-50",
                "dark:hover:bg-slate-800/50", "focus-visible:ring-inset", "rotate-90", "motion-reduce:transition-none", "transition-[grid-template-rows]", "min-h-0", "pt-1", "pb-4")));
    }

    @Test
    void countsOnlyExactParts() {
        assertEquals(2, AccordionLegacyMarkup.count("<div class=\"ash-accordion__panel\"></div><div class=\"ash-accordion__panel\"></div><div class=\"ash-accordion__panel extra\"></div>", "accordion-panel"));
    }
}
