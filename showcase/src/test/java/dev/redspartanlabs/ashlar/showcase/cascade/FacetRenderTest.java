package dev.redspartanlabs.ashlar.showcase.cascade;

import dev.redspartanlabs.ashlar.facet.FacetContext;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The REAL Faceted Search, Facet and Facet Option templates, rendered through the JTE engine (the same compiled templates the showcase serves), without a browser. What this proves is
 * what a class-only change must not move: the field name each option submits under (the {@code FacetContext} hand-off from its facet, and none outside one), the attributes, the escaping,
 * the count's visible and announced text, and the data hooks. The assertions never read a class, so they held before the migration and hold after it; and the last test compares every
 * fixture, after the legacy rewrite, with the frozen output of the templates before the migration (see {@code facet-legacy-golden.html}).
 */
class FacetRenderTest {

    private static final Pattern INPUT = Pattern.compile("<input\\b[^>]*>");
    private static final Pattern NAME = Pattern.compile("\\sname=\"([^\"]*)\"");
    private static final Pattern VALUE = Pattern.compile("\\svalue=\"([^\"]*)\"");

    private record Control(String name, String value, boolean checked, boolean disabled, boolean hasName) {
    }

    private static List<Control> controls(String html) {
        List<Control> found = new ArrayList<>();
        Matcher m = INPUT.matcher(html);
        while (m.find()) {
            String tag = m.group();
            Matcher name = NAME.matcher(tag);
            Matcher value = VALUE.matcher(tag);
            found.add(new Control(name.find() ? name.group(1) : null, value.find() ? value.group(1) : null, Pattern.compile("\\schecked(?=[\\s>/])").matcher(tag).find(),
                    Pattern.compile("\\sdisabled(?=[\\s>/])").matcher(tag).find(), tag.contains(" name=")));
        }
        return found;
    }

    private static String stripClasses(String html) {
        return html.replaceAll(" class=\"[^\"]*\"", "");
    }

    @Test
    void everyOptionSubmitsUnderTheNameOfItsOwnFacetAndNoNameLeaksToTheNextFacet() {
        String html = FacetBrowser.facetedSearch(List.of(
                FacetBrowser.Fac.of("Status", "status", FacetBrowser.Opt.of("Running", "running"), FacetBrowser.Opt.of("Ready", "ready")),
                FacetBrowser.Fac.of("Machine", "machine", FacetBrowser.Opt.of("ACB-01", "acb-01"), FacetBrowser.Opt.of("ACB-02", "acb-02"), FacetBrowser.Opt.of("ACB-03", "acb-03")),
                FacetBrowser.Fac.of("Type", "type", FacetBrowser.Opt.of("Batch", "batch"))));
        Map<String, List<String>> byName = new LinkedHashMap<>();
        for (Control c : controls(html)) {
            byName.computeIfAbsent(c.name(), k -> new ArrayList<>()).add(c.value());
        }
        assertEquals(Map.of("status", List.of("running", "ready"), "machine", List.of("acb-01", "acb-02", "acb-03"), "type", List.of("batch")), byName);
        assertEquals(List.of("status", "machine", "type"), List.copyOf(byName.keySet()), "in the order the facets are written");
    }

    @Test
    void theNameIsClearedWhenTheFacetHasRenderedSoAnOptionLaterOnTheSameThreadHasNone() {
        FacetBrowser.facet(FacetBrowser.Fac.of("Status", "status", FacetBrowser.Opt.of("Running", "running")));
        assertEquals("", FacetContext.currentName(), "the facet cleared its name");
        List<Control> orphan = controls(FacetBrowser.option(FacetBrowser.Opt.of("Orphan", "orphan")));
        assertEquals(1, orphan.size());
        assertEquals("", orphan.get(0).name(), "an option outside a facet renders an empty name, so the browser never submits it");
        assertTrue(orphan.get(0).hasName(), "the attribute is there, empty: `name=\"\"`");
    }

    @Test
    void anOptionRenderedBeforeAnyFacetHasNoNameEither() {
        assertEquals("", controls(FacetBrowser.option(FacetBrowser.Opt.of("First", "first"))).get(0).name());
    }

    @Test
    void checkedAndDisabledAreBooleanAttributesWrittenOnlyWhenTrue() {
        String html = FacetBrowser.facet(FacetBrowser.Fac.of("Status", "status", FacetBrowser.Opt.of("A", "a"), FacetBrowser.Opt.of("B", "b").on(), FacetBrowser.Opt.of("C", "c").off(),
                FacetBrowser.Opt.of("D", "d").on().off()));
        List<Control> c = controls(html);
        assertEquals(List.of(false, true, false, true), c.stream().map(Control::checked).toList());
        assertEquals(List.of(false, false, true, true), c.stream().map(Control::disabled).toList());
        assertFalse(html.contains("checked=\"") || html.contains("disabled=\""), "a boolean attribute carries no value: " + html);
    }

    @Test
    void theCountIsShownAsABareNumberHiddenFromAssistiveTechnologyAndAnnouncedInWords() {
        String none = stripClasses(FacetBrowser.option(FacetBrowser.Opt.of("L", "v")));
        assertFalse(none.contains("aria-hidden") || none.contains("result"), "no count, no number and no announcement: " + none);
        assertCount(FacetBrowser.Opt.of("L", "v").count(0), "0", "0 results");
        assertCount(FacetBrowser.Opt.of("L", "v").count(1), "1", "1 result");
        assertCount(FacetBrowser.Opt.of("L", "v").count(2), "2", "2 results");
        assertCount(FacetBrowser.Opt.of("L", "v").count(12345), "12345", "12345 results");
    }

    private static void assertCount(FacetBrowser.Opt option, String visible, String announced) {
        String html = FacetBrowser.normalized(stripClasses(FacetBrowser.option(option)));
        assertTrue(html.contains("<span aria-hidden=\"true\">" + visible + "</span><span>" + announced + "</span></label>"), "count " + visible + ": " + html);
    }

    @Test
    void textIsEscapedInTheLegendTheLabelAndTheValue() {
        String html = FacetBrowser.facet(FacetBrowser.Fac.of("Ops & <b>\"Support\"", "ops", FacetBrowser.Opt.of("Tier <1> & 2", "a\"b")));
        assertTrue(html.contains("Ops &amp; &lt;b&gt;"), html);
        assertTrue(html.contains("Tier &lt;1&gt; &amp; 2"), html);
        assertEquals("a&#34;b", controls(html).get(0).value(), "a quote in a value cannot end the attribute");
        assertFalse(html.contains("<b>") || html.contains("<1>"), html);
    }

    @Test
    void theDataHooksAreWrittenOnTheContainerAndOnEveryFacetWithItsName() {
        String html = stripClasses(FacetBrowser.facetedSearch(List.of(FacetBrowser.Fac.of("A", "alpha", FacetBrowser.Opt.of("x", "x")), FacetBrowser.Fac.of("B", "beta", FacetBrowser.Opt.of("y", "y")))));
        assertEquals(1, count(html, "<div data-faceted-search>"), html);
        assertEquals(1, count(html, "<fieldset data-facet data-facet-name=\"alpha\">"), html);
        assertEquals(1, count(html, "<fieldset data-facet data-facet-name=\"beta\">"), html);
        assertEquals(2, count(html, "<legend>"));
    }

    @Test
    void theElementsAreAFieldsetWithALegendAndAnOptionsContainerHoldingLabelsThatWrapTheirCheckbox() {
        String html = FacetBrowser.normalized(stripClasses(FacetBrowser.facet(FacetBrowser.Fac.of("Status", "status", FacetBrowser.Opt.of("Running", "running").count(42).on()))));
        assertEquals("<fieldset data-facet data-facet-name=\"status\"><legend>Status</legend><div><label><input type=\"checkbox\" name=\"status\" value=\"running\" checked>"
                + "<span>Running</span><span aria-hidden=\"true\">42</span><span>42 results</span></label></div></fieldset>", html);
    }

    private static int count(String text, String part) {
        return text.split(Pattern.quote(part), -1).length - 1;
    }

    // ---------------------------------------------------------------- the frozen output of the templates before the migration

    private static Map<String, String> golden() throws Exception {
        String text;
        try (InputStream in = FacetRenderTest.class.getResourceAsStream("/parity/facet/facet-legacy-golden.html")) {
            assertTrue(in != null, "facet-legacy-golden.html must be on the test classpath");
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
        Map<String, String> cases = new LinkedHashMap<>();
        String[] parts = text.split("<!-- case: (.*?) -->\n");
        Matcher keys = Pattern.compile("<!-- case: (.*?) -->\n").matcher(text);
        int i = 1;
        while (keys.find()) {
            cases.put(keys.group(1), parts[i++].strip());
        }
        return cases;
    }

    @Test
    void everyFixtureRendersTheFrozenMarkupOfTheTemplatesBeforeTheMigrationOnceRewrittenToTheLegacyClasses() throws Exception {
        Map<String, String> golden = golden();
        FacetLegacyMarkup legacy = new FacetLegacyMarkup();
        assertEquals(FacetBrowser.CASES.stream().map(FacetBrowser.Case::key).toList(), List.copyOf(golden.keySet()), "the golden has exactly the fixtures, in order");
        List<String> differences = new ArrayList<>();
        for (FacetBrowser.Case c : FacetBrowser.CASES) {
            String rendered = c.bare() != null ? FacetBrowser.option(c.bare()) : FacetBrowser.facetedSearch(c.facets());
            String actual = FacetBrowser.normalized(legacy.rewrite(rendered));
            if (!actual.equals(golden.get(c.key()))) {
                differences.add(c.key() + "\n  frozen:  " + golden.get(c.key()) + "\n  actual:  " + actual);
            }
        }
        assertEquals(List.of(), differences);
    }

    @Test
    void theFrozenMarkupCoversTheOutsideOptionTheCountsTheDisabledStateAndEveryLegacyClassString() throws Exception {
        String all = String.join("\n", golden().values());
        FacetLegacyMarkup legacy = new FacetLegacyMarkup();
        for (Map.Entry<String, String> role : legacy.all().entrySet()) {
            assertTrue(all.contains("class=\"" + role.getValue() + "\""), "the frozen output uses the class string of " + role.getKey());
        }
        Matcher m = Pattern.compile("class=\"([^\"]*)\"").matcher(all);
        while (m.find()) {
            assertTrue(legacy.all().containsValue(m.group(1)), "a class string in the frozen output that facet-legacy.html does not define: " + m.group(1));
        }
        assertTrue(all.contains("name=\"\""), "the option outside a facet");
        assertTrue(all.contains("1 result<") && all.contains("0 results<"), "the singular and the zero");
    }
}
