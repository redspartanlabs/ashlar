package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rewrite that turns migrated Faceted Search, Facet and Facet Option markup back into the former Tailwind markup: what it changes, what it chooses from the markup (a row's disabled
 * string, a checkbox's trailing {@code cursor-pointer}), what it must leave alone, and that it fails loudly. It is a pure function of strings, so these hold before the migration too.
 */
class FacetLegacyMarkupTest {

    private static final String MIGRATED = "<div class=\"ash-faceted-search\"><fieldset class=\"ash-facet\"><legend class=\"ash-facet__legend\">S</legend><div class=\"ash-facet__options\">"
            + "<label class=\"ash-facet-option\"><input type=\"checkbox\" name=\"s\" value=\"a\" class=\"ash-facet-option__input\"><span class=\"ash-facet-option__label\">A</span>"
            + "<span aria-hidden=\"true\" class=\"ash-facet-option__count\">3</span><span class=\"ash-sr-only\">3 results</span></label>"
            + "<label class=\"ash-facet-option ash-facet-option--disabled\"><input type=\"checkbox\" name=\"s\" value=\"b\" disabled class=\"ash-facet-option__input\">"
            + "<span class=\"ash-facet-option__label\">B</span></label></div></fieldset></div>";

    @Test
    void everyPartGetsItsLegacyStringAndNoMigratedClassSurvives() throws Exception {
        FacetLegacyMarkup l = new FacetLegacyMarkup();
        String out = l.rewrite(MIGRATED);
        for (String role : List.of("faceted-search", "facet", "legend", "options", "row", "row-disabled", "input", "input-disabled", "label", "count", "announced-count")) {
            assertTrue(out.contains("class=\"" + l.classes(role) + "\""), role + " in " + out);
        }
        assertFalse(out.contains("ash-"), out);
    }

    @Test
    void aDisabledOptionGetsTheDisabledRowAndTheCheckboxWithoutThePointerCursorAndOnlyThatOne() throws Exception {
        FacetLegacyMarkup l = new FacetLegacyMarkup();
        String out = l.rewrite(MIGRATED);
        assertEquals(1, count(out, "class=\"" + l.classes("row") + "\""));
        assertEquals(1, count(out, "class=\"" + l.classes("row-disabled") + "\""));
        assertEquals(1, count(out, "class=\"" + l.classes("input") + "\""));
        assertEquals(1, count(out, "class=\"" + l.classes("input-disabled") + "\""));
        assertTrue(l.classes("input").equals(l.classes("input-disabled") + " cursor-pointer"), "the enabled string is the disabled one plus the pointer cursor the template appended");
        assertTrue(l.classes("row-disabled").endsWith("cursor-not-allowed opacity-60") && l.classes("row").contains("hover:bg-slate-100"));
    }

    @Test
    void theAnnouncedCountIsRewrittenOnlyInsideAnOptionAndASrOnlyElsewhereIsLeftAlone() throws Exception {
        FacetLegacyMarkup l = new FacetLegacyMarkup();
        String elsewhere = "<span class=\"ash-sr-only\">x</span>" + MIGRATED + "<span class=\"ash-sr-only\">y</span>";
        String out = l.rewrite(elsewhere);
        assertTrue(out.startsWith("<span class=\"ash-sr-only\">x</span>") && out.endsWith("<span class=\"ash-sr-only\">y</span>"), out);
        assertEquals(1, count(out, "class=\"" + l.classes("announced-count") + "\""));
    }

    @Test
    void markupWithNoFacetsClassComesBackUnchangedAndAnUnknownFacetsClassFailsLoudly() throws Exception {
        FacetLegacyMarkup l = new FacetLegacyMarkup();
        String other = "<div class=\"ash-card\"><span class=\"ash-sr-only\">x</span><a class=\"ash-facets\">y</a><i class=\"ash-faceted\"></i><b class=\"flex gap-5\"></b></div>";
        assertEquals(other, l.rewrite(other));
        assertThrows(IllegalStateException.class, () -> l.rewrite("<span class=\"ash-facet__unknown\"></span>"));
        assertThrows(IllegalStateException.class, () -> l.rewrite("<div class=\"ash-facet-option ash-facet__legend\"></div>"));
        assertThrows(IllegalStateException.class, () -> l.rewrite("<label class=\"ash-facet-option ash-facet-option--other\"></label>"));
    }

    @Test
    void everyLegacyTokenIsCountedForTheOraclesNonVacuityCheck() throws Exception {
        assertTrue(new FacetLegacyMarkup().allTokens().containsAll(List.of("flex", "min-w-0", "flex-col", "gap-5", "mb-1.5", "text-sm", "font-semibold", "text-slate-900", "dark:text-slate-100", "items-center",
                "gap-2.5", "rounded-md", "px-2", "py-1.5", "cursor-pointer", "hover:bg-slate-100", "dark:hover:bg-slate-800", "cursor-not-allowed", "opacity-60", "h-4", "w-4", "shrink-0", "rounded",
                "border", "border-slate-300", "bg-white", "accent-blue-600", "focus:outline-none", "focus-visible:ring-2", "focus-visible:ring-blue-500", "focus-visible:ring-offset-2",
                "disabled:cursor-not-allowed", "dark:border-slate-600", "dark:bg-slate-800", "flex-1", "break-words", "text-slate-700", "dark:text-slate-300", "text-xs", "tabular-nums",
                "text-slate-500", "dark:text-slate-400", "sr-only")));
    }

    private static int count(String html, String part) {
        Matcher m = Pattern.compile(Pattern.quote(part)).matcher(html);
        int n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }

    // ---- the migrated templates

    private static String shipped(String name) throws Exception {
        try (InputStream in = FacetLegacyMarkupTest.class.getResourceAsStream("/ashlar/components/" + name + ".jte")) {
            assertTrue(in != null, name + ".jte is not on the test classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("(?s)<%--.*?--%>", "");
        }
    }

    @Test
    void theRealTemplatesRenderOnlyAshlarClassesAndTheRewriteHasSomethingToRewriteInEveryFixture() throws Exception {
        FacetLegacyMarkup l = new FacetLegacyMarkup();
        for (FacetBrowser.Case c : FacetBrowser.CASES) {
            String rendered = c.bare() != null ? FacetBrowser.option(c.bare()) : FacetBrowser.facetedSearch(c.facets());
            Matcher classes = Pattern.compile("class=\"([^\"]*)\"").matcher(rendered);
            int found = 0;
            while (classes.find()) {
                found++;
                for (String token : classes.group(1).trim().split("\\s+")) {
                    assertTrue(token.startsWith("ash-"), c.key() + ": a class that is not Ashlar's: " + token);
                }
            }
            assertTrue(found >= 3, c.key() + ": classes written: " + found);
            assertTrue(!l.rewrite(rendered).equals(rendered), c.key() + ": the rewrite changed something");
        }
    }

    @Test
    void theRealPageProbeNamesNoFacetsElement() throws Exception {
        String probe = new String(FacetLegacyMarkupTest.class.getResourceAsStream("/cascade/real-page-probe.js").readAllBytes(), StandardCharsets.UTF_8);
        assertFalse(probe.contains("ash-facet"), "the probe is unchanged by this migration and names no Facets element");
        assertTrue(shipped("facet").contains("data-facet"));
    }
}
