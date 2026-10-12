package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Static (no browser) checks of the compiled cascade-layer order.
 *
 * <p>These are supporting evidence only. Browser behaviour is proven by
 * {@link CascadeBrowserTest}. Note that a passing Path A wrapper does NOT mean
 * the production Tailwind integration is correct: the production entry point
 * (showcase/src/main/tailwind/app.css) has no ordering statement, and
 * {@link #productionStylesheetHasNoAshLayers()} records exactly that.
 */
class CascadeLayerOrderTest {

    private static final List<String> SKINS = List.of(
            "axiom", "civic", "fathom", "foundry", "harbor",
            "keystone", "ledger", "sterling", "summit", "veridian");

    /**
     * The Tailwind-only stylesheet: what the production app.css was before Ashlar's own
     * stylesheet was imported into it (Phase C). The production app.css now bundles Ashlar;
     * ShowcaseBuildOutputTest checks that one.
     */
    private static String tailwindOnly() throws IOException {
        return generated("phase-c-baseline.css");
    }

    private static String generated(String file) throws IOException {
        String dir = System.getProperty("ashlar.cascade.dir");
        assertNotNull(dir, "ashlar.cascade.dir must be set by the Gradle test task");
        return Files.readString(Path.of(dir, file), StandardCharsets.UTF_8);
    }

    @Test
    void tailwindOnlyStylesheetHasNoAshLayers() throws IOException {
        LayerOrder order = LayerOrder.of(tailwindOnly());
        assertEquals(List.of("properties", "theme", "base", "components", "utilities"), order.topLevel());
        assertTrue(order.children("ash").isEmpty());
        assertFalse(order.matchesIntendedOrder());
    }

    @Test
    void pathACompilesToTheIntendedEffectiveOrder() throws IOException {
        LayerOrder order = LayerOrder.of(generated("path-a.css"));
        assertEquals(List.of("properties", "theme", "base", "ash", "components", "utilities"), order.topLevel(), order.toString());
        assertEquals(LayerOrder.ASH_SUBLAYERS, order.children("ash"), order.toString());
        assertTrue(order.matchesIntendedOrder());
    }

    // ---- Phase A: how Tailwind's import processing treats the coordinated entries ----

    @Test
    void a2BundledImportCompilesToTheSameEffectiveOrderAsPathA() throws IOException {
        LayerOrder pathA = LayerOrder.of(generated("path-a.css"));
        LayerOrder bundled = LayerOrder.of(generated("a2-import.css"));
        assertEquals(pathA.topLevel(), bundled.topLevel(), bundled.toString());
        assertEquals(pathA.children("ash"), bundled.children("ash"), bundled.toString());
        assertTrue(bundled.matchesIntendedOrder());
    }

    @Test
    void a2ImportingAshlarFirstCompilesToTheSameEffectiveOrderAsImportingItAfterTheStatement() throws IOException {
        LayerOrder bundled = LayerOrder.of(generated("a2-import.css"));
        LayerOrder ashFirst = LayerOrder.of(generated("a2-import-ash-first.css"));
        assertEquals(bundled.topLevel(), ashFirst.topLevel(), ashFirst.toString());
        assertEquals(bundled.children("ash"), ashFirst.children("ash"), ashFirst.toString());
    }

    @Test
    void a2BundledImportWithoutTheStatementPlacesAshAboveUtilities() throws IOException {
        LayerOrder order = LayerOrder.of(generated("a2-import-no-statement.css"));
        assertFalse(order.matchesIntendedOrder(), order.toString());
        List<String> top = order.topLevel();
        assertTrue(top.indexOf("ash") > top.indexOf("utilities"), order.toString());
    }

    @Test
    void a3NamingPropertiesFirstChangesNothingInTailwindsOwnCompiledOutput() throws IOException {
        // Tailwind already emits its `properties` layer first, so naming it first
        // in the entry statement is a no-op for the compiled stylesheet.
        assertEquals(generated("path-a.css"), generated("a3-properties-first.css"));
    }

    @Test
    void a4ConsumerLayerNamedLastInTheStatementSurvivesCompilationAsTheTopLayer() throws IOException {
        LayerOrder order = LayerOrder.of(generated("a4-app-layer-last.css"));
        List<String> top = order.topLevel();
        assertEquals("app", top.get(top.size() - 1), order.toString());
        assertEquals(LayerOrder.ASH_SUBLAYERS, order.children("ash"), order.toString());
    }

    @Test
    void wrongOrderVariantIsRejectedByThePredicate() throws IOException {
        LayerOrder order = LayerOrder.of(generated("path-a-wrong-order.css"));
        assertFalse(order.matchesIntendedOrder(), order.toString());
    }

    @Test
    void predicateHandlesSyntheticInputs() {
        assertTrue(LayerOrder.of("@layer theme,base,ash.tokens,ash.base,ash.layout,ash.components,ash.skin,components,utilities;").matchesIntendedOrder());
        assertFalse(LayerOrder.of("@layer theme,base,components,utilities;@layer ash.tokens{}").matchesIntendedOrder());
        assertFalse(LayerOrder.of("@layer ash.tokens{} @layer theme,base,components,utilities;").matchesIntendedOrder());
        assertTrue(LayerOrder.of("/* @layer bogus; */ @layer theme{.a{content:\"@layer x;\"}} @layer base,ash.tokens,ash.base,ash.layout,ash.components,ash.skin,components,utilities;").matchesIntendedOrder());
    }

    @Test
    void skinStylesheetsAreUnlayeredAndFreeOfImportant() throws IOException {
        Pattern comment = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
        for (String skin : SKINS) {
            try (InputStream in = CascadeLayerOrderTest.class.getResourceAsStream("/static/css/theme-" + skin + ".css")) {
                assertNotNull(in, "theme-" + skin + ".css");
                String css = comment.matcher(new String(in.readAllBytes(), StandardCharsets.UTF_8)).replaceAll("");
                assertFalse(css.contains("@layer"), skin + " must stay unlayered");
                assertFalse(css.contains("!important"), skin + " must not use !important");
            }
        }
    }
}
