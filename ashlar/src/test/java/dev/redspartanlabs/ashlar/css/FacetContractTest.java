package dev.redspartanlabs.ashlar.css;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import static dev.redspartanlabs.ashlar.css.TextEntrySources.declared;
import static dev.redspartanlabs.ashlar.css.TextEntrySources.template;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the source contract of the Facets family: Faceted Search, Facet and Facet Option. The parameters and defaults below are the ones the templates declared BEFORE the migration
 * (commit 9c59c12), so this fails if the public contract moves. The markup is compared without its class attributes, so the same assertions held before the migration and hold after it:
 * the elements, their order, every attribute and every {@code data-*} hook, the {@code FacetContext} hand-off and the count's accessible text. The rendered result is measured in a real
 * browser by the showcase tests FacetParityTest, FacetPixelTest, FacetTailwindFreeTest, FacetHoverMediaTest, FacetMotionTest and FacetBehaviourTest, and FacetRenderTest renders the real
 * templates; this reads the sources only.
 */
class FacetContractTest {

    private static final Pattern LOCALS = Pattern.compile("(?ms)^!\\{.*?\\}$");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile(" class=\"[^\"]*\"");

    /** The template without its comments, parameter and import lines and multi-line local variables, on one line. */
    private static String markup(String name) throws IOException {
        String source = template(name).replaceAll("(?s)<%--.*?--%>", "").replaceAll("(?m)^@(?:param|import) .*$", "");
        return LOCALS.matcher(source).replaceAll("").replaceAll("\\s+", " ").trim();
    }

    /** The markup with every class attribute removed: what the template writes apart from how it is styled. */
    private static String structure(String name) throws IOException {
        return CLASS_ATTRIBUTE.matcher(markup(name)).replaceAll("");
    }

    @Test
    void theParametersAndTheirDefaultsAreUnchanged() throws IOException {
        assertEquals(List.of("@param gg.jte.Content content"), declared("faceted-search"));
        assertEquals(List.of("@import dev.redspartanlabs.ashlar.facet.FacetContext", "@param String title", "@param String name", "@param gg.jte.Content content"), declared("facet"));
        assertEquals(List.of("@import dev.redspartanlabs.ashlar.facet.FacetContext", "@param String label", "@param String value", "@param Integer count = null", "@param boolean checked = false",
                "@param boolean disabled = false"), declared("facet-option"));
    }

    @Test
    void facetedSearchIsOneContainerWithTheDataHookAndNothingElse() throws IOException {
        assertEquals("<div data-faceted-search> ${content} </div>", structure("faceted-search"));
    }

    @Test
    void aFacetIsAFieldsetWithALegendAndHandsItsNameToTheOptionsAroundTheirContent() throws IOException {
        assertEquals("<fieldset data-facet data-facet-name=\"${name}\"> <legend>${title}</legend> <div> !{FacetContext.enter(name);} ${content} !{FacetContext.exit();} </div> </fieldset>",
                structure("facet"));
        String source = template("facet");
        assertTrue(source.indexOf("FacetContext.enter(name)") < source.indexOf("${content}") && source.indexOf("${content}") < source.indexOf("FacetContext.exit()"),
                "enter before the content renders and exit after it");
        assertEquals(1, source.split("FacetContext\\.enter\\(", -1).length - 1);
        assertEquals(1, source.split("FacetContext\\.exit\\(", -1).length - 1);
    }

    @Test
    void aFacetOptionIsANativeCheckboxInsideItsOwnLabelWithTheNameFromTheFacet() throws IOException {
        assertEquals("<label> <input type=\"checkbox\" name=\"${FacetContext.currentName()}\" value=\"${value}\" checked=\"${checked}\" disabled=\"${disabled}\" > <span>${label}</span>"
                + " @if (hasCount) <span aria-hidden=\"true\">${count}</span> <span>${countLabel}</span> @endif </label>", structure("facet-option"));
    }

    @Test
    void theCountIsShownAsABareNumberAndAnnouncedAsResultsAndZeroIsACount() throws IOException {
        String source = template("facet-option").replaceAll("(?s)<%--.*?--%>", "");
        assertTrue(source.contains("!{boolean hasCount = count != null;}"), "no count at all is null; 0 is a count and is shown");
        assertTrue(source.contains("!{String countLabel = hasCount ? (count == 1 ? \"1 result\" : count + \" results\") : \"\";}"));
        String m = markup("facet-option");
        assertTrue(m.contains("aria-hidden=\"true\""), "the visible number is hidden from assistive technology: " + m);
    }

    @Test
    void noFacetsTemplateHasJavaScriptOrAnInlineStyleOrAnAriaWiringOfItsOwn() throws IOException {
        for (String name : List.of("faceted-search", "facet", "facet-option")) {
            String m = markup(name);
            assertFalse(m.contains("<script") || m.contains("AshlarAssets"), name + " has no JavaScript: selection, Space, focus and form participation are native");
            assertFalse(m.contains("style=\""), name + " writes no inline style");
            assertFalse(m.contains("aria-label") || m.contains("aria-labelledby") || m.contains("role="), name + " names its group with the legend and its option with the label, no ARIA wiring needed");
        }
    }
}
