package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rewrite that turns migrated Tabs and Tab Panel markup back into the former Tailwind markup: what it changes, what it must leave alone, and that it fails loudly. */
class TabsLegacyMarkupTest {

    private static String tab(boolean selected, String extra) {
        return "<button type=\"button\" role=\"tab\" aria-selected=\"" + selected + "\"" + extra + " class=\"ash-tabs__tab\"><span class=\"ash-tabs__label\">L</span></button>";
    }

    @Test
    void aTabGetsTheActiveOrInactiveStringByItsOwnAriaSelectedAndADisabledTabKeepsItsTwins() throws Exception {
        TabsLegacyMarkup legacy = new TabsLegacyMarkup();
        String active = legacy.rewrite(tab(true, ""));
        String inactive = legacy.rewrite(tab(false, ""));
        assertTrue(active.contains("class=\"" + legacy.classes("tab-active") + "\""), active);
        assertTrue(inactive.contains("class=\"" + legacy.classes("tab-inactive") + "\""), inactive);
        assertTrue(legacy.classes("tab-active").contains("border-blue-600") && legacy.classes("tab-inactive").contains("border-transparent"));
        assertTrue(legacy.classes("tab-active").contains("disabled:hover:border-transparent"), "both carry the shared base, which has the disabled variants");
        assertTrue(legacy.rewrite(tab(false, " disabled")).contains("class=\"" + legacy.classes("tab-inactive") + "\""), "a disabled tab had its enabled twin's classes");
        assertTrue(inactive.contains("aria-selected=\"false\"") && inactive.contains("role=\"tab\""), "the hooks are left as served");
    }

    @Test
    void theRootAndTheLabelHadNoClassAndTheirWholeAttributeIsRemoved() throws Exception {
        TabsLegacyMarkup legacy = new TabsLegacyMarkup();
        String out = legacy.rewrite("<div id=\"g\" data-tabs class=\"ash-tabs\">" + tab(true, "") + "</div>");
        assertEquals("<div id=\"g\" data-tabs>", out.substring(0, out.indexOf('>') + 1));
        assertTrue(out.contains("<span>L</span>"), out);
        assertFalse(out.contains("ash-tabs"), out);
    }

    @Test
    void theListTheBadgeAndThePanelGetTheirLegacyStringsAndAPanelKeepsItsHiddenAttribute() throws Exception {
        TabsLegacyMarkup legacy = new TabsLegacyMarkup();
        String out = legacy.rewrite("<div role=\"tablist\" class=\"ash-tabs__list\"></div><span class=\"ash-tabs__badge\">4</span><div role=\"tabpanel\" hidden class=\"ash-tab-panel\">x</div>");
        assertTrue(out.contains("class=\"" + legacy.classes("tabs-list") + "\"") && out.contains("class=\"" + legacy.classes("tab-badge") + " ash-tabs__badge\"") && out.contains("class=\"" + legacy.classes("tab-panel") + "\""), out);
        assertTrue(out.contains("hidden class=\"" + legacy.classes("tab-panel") + "\""), "the attribute that carries visibility survives: " + out);
        assertFalse(legacy.classes("tab-panel").contains("hidden"), "and no hidden class is added");
    }

    @Test
    void theRealPageProbeMarksOnlyTheTabsBadgeAmongTheTabsElements() throws Exception {
        String probe = new String(TabsLegacyMarkupTest.class.getResourceAsStream("/cascade/real-page-probe.js").readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(probe.contains("el.closest(\"[data-badge], [data-btn], .ash-tabs__badge\")"), "the marker names the badge class exactly");
        assertFalse(probe.contains("[data-tabs") || probe.contains("ash-tabs__tab") || probe.contains("ash-tab-panel"), "and no other Tabs element, nor a Tabs subtree");
        // The badge is the only element the legacy rewrite leaves carrying a migrated Tabs class.
        TabsLegacyMarkup legacy = new TabsLegacyMarkup();
        String out = legacy.rewrite("<div class=\"ash-tabs\"><div class=\"ash-tabs__list\"><button role=\"tab\" aria-selected=\"true\" class=\"ash-tabs__tab\"><span class=\"ash-tabs__label\">L</span><span class=\"ash-tabs__badge\">1</span></button></div></div><div class=\"ash-tab-panel\"></div>");
        assertEquals(1, out.split("ash-tabs", -1).length - 1, out);
        assertFalse(out.contains("ash-tab-panel") || out.contains("ash-tabs__tab") || out.contains("ash-tabs__list") || out.contains("ash-tabs__label"), out);
    }

    @Test
    void otherMarkupIsLeftExactlyAsServedAndAnUnknownTabsClassFailsLoudly() throws Exception {
        TabsLegacyMarkup legacy = new TabsLegacyMarkup();
        String other = "<div class=\"ash-card\"><span class=\"ash-sr-only\">x</span><a class=\"ash-tabsish\">y</a></div>";
        assertEquals(other, legacy.rewrite(other));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<span class=\"ash-tabs__unknown\"></span>"));
    }

    @Test
    void everyLegacyTokenIsCountedForTheOraclesNonVacuityCheck() throws Exception {
        TabsLegacyMarkup legacy = new TabsLegacyMarkup();
        assertTrue(legacy.allTokens().containsAll(java.util.List.of("border-b-2", "border-blue-600", "hover:text-blue-700", "dark:hover:border-slate-600", "disabled:hover:border-transparent",
                "focus-visible:ring-offset-2", "min-w-5", "rounded-full", "py-4", "gap-1")));
    }

    @Test
    void countsOnlyExactParts() {
        assertEquals(2, TabsLegacyMarkup.count("<div class=\"ash-tab-panel\"></div><div class=\"ash-tab-panel\"></div><div class=\"ash-tab-panel extra\"></div>", "tab-panel"));
    }
}
