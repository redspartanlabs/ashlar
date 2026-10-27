package dev.redspartanlabs.ashlar.css;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The content decisions of the Phase B foundation: layer order, token naming,
 * dark-mode parity, the absence of a global reset, and the {@code ash-sr-only}
 * declarations. Reads the generated stylesheet (the same bytes
 * {@link AshlarCssPackagingTest} proves are in the jar).
 */
class AshlarCssFoundationTest {

    static final List<String> EXPECTED_LAYER_ORDER = List.of(
            "properties", "theme", "base",
            "ash.tokens", "ash.base", "ash.layout", "ash.components", "ash.skin", "ash.utilities",
            "components", "utilities");

    /** The layout primitives' public classes (selectors), in the order their modules define them. */
    static final List<String> CONTRACT_CLASSES = List.of(
            ".ash-container", ".ash-container--narrow", ".ash-container--fluid",
            ".ash-stack", ".ash-stack--sm", ".ash-stack--lg", ".ash-stack--xl",
            ".ash-grid", ".ash-grid--cols-2", ".ash-grid--cols-3", ".ash-grid--cols-4",
            ".ash-grid--sm", ".ash-grid--lg", ".ash-grid--xl",
            ".ash-split", ".ash-split--side-start", ".ash-split--side-end",
            ".ash-split--sm", ".ash-split--lg", ".ash-split--xl",
            ".ash-cluster", ".ash-cluster--end", ".ash-cluster--between",
            ".ash-cluster--sm", ".ash-cluster--lg", ".ash-cluster--xl");

    static final List<String> UTILITY_CLASSES = List.of(".ash-sr-only", ".ash-truncate");

    /** Every configuration property a primitive reads; each is reset to {@code initial} on that primitive's base rule. */
    static final Map<String, List<String>> RESET_PROPERTIES = Map.of(
            ".ash-container", List.of("--ash-container-max", "--ash-container-pad"),
            ".ash-stack", List.of("--ash-stack-gap"),
            ".ash-grid", List.of("--ash-grid-min", "--ash-grid-gap"),
            ".ash-split", List.of("--ash-split-side", "--ash-split-gap"),
            ".ash-cluster", List.of("--ash-cluster-gap"));

    static final List<String> SPACING_TOKENS = List.of(
            "--ash-space-2", "--ash-space-3", "--ash-space-4", "--ash-space-6", "--ash-space-8", "--ash-space-10");

    private static String css;
    /** Every layer block in the order emitted, repeats included. */
    private static List<String> layerBlockSequence;
    /** Block bodies per layer, with repeated blocks of the same layer joined in emitted order. */
    private static Map<String, String> layerBodies;

    @BeforeAll
    static void load() throws IOException {
        String generated = System.getProperty("ashlar.css.generated");
        assertNotNull(generated, "ashlar.css.generated must be set by the Gradle test task");
        css = Files.readString(Path.of(generated), StandardCharsets.UTF_8);
        layerBodies = new LinkedHashMap<>();
        layerBlockSequence = new java.util.ArrayList<>();
        for (CssScanner.Item item : CssScanner.items(css)) {
            if (item.body() != null) {
                String layer = item.prelude().substring("@layer ".length());
                layerBlockSequence.add(layer);
                layerBodies.merge(layer, item.body(), (a, b) -> a + "\n" + b);
            }
        }
    }

    /** Unconditional style rules of a layer: selector list to declarations (later duplicates replace earlier ones). */
    private static Map<String, Map<String, String>> rulesIn(String layer) {
        Map<String, Map<String, String>> rules = new LinkedHashMap<>();
        for (CssScanner.Rule rule : CssScanner.flatRules(layerBodies.get(layer))) {
            if (rule.media().isEmpty()) {
                rules.put(rule.selector(), CssScanner.declarations(rule.block()));
            }
        }
        return rules;
    }

    @Test
    void theLeadingStatementOrdersAshBetweenTailwindsBaseAndComponentsLayers() {
        CssScanner.Item first = CssScanner.items(css).get(0);
        assertEquals(EXPECTED_LAYER_ORDER, List.of(first.prelude().substring("@layer ".length()).split("\\s*,\\s*")));
    }

    @Test
    void theStatementIsTheVeryFirstThingInTheFile() {
        assertTrue(css.startsWith("@layer properties, theme, base, ash.tokens,"), css.substring(0, 80));
    }

    @Test
    void everyRuleLivesInANamedAshSublayer() {
        assertEquals(List.of(), CssScanner.violations(css));
    }

    @Test
    void layerBlocksAreEmittedInTheOrderTheEntryFileListsThem() {
        assertEquals(List.of("ash.tokens", "ash.base",
                "ash.layout", "ash.layout", "ash.layout", "ash.layout", "ash.layout",
                "ash.components", "ash.components", "ash.components", "ash.components", "ash.components", "ash.components",
                "ash.components", "ash.components", "ash.components", "ash.components",
                "ash.utilities", "ash.utilities"), layerBlockSequence);
    }

    // ---- Phase D: layout primitives and ash-truncate ----

    /** Selector lists of every unconditional and conditional rule in a layer, split into single selectors, in source order. */
    private static List<String> selectorsInOrder(String layer) {
        List<String> selectors = new java.util.ArrayList<>();
        for (CssScanner.Rule rule : CssScanner.flatRules(layerBodies.get(layer))) {
            selectors.addAll(CssScanner.selectors(rule.selector()));
        }
        return selectors;
    }

    @Test
    void theContractClassesLiveInTheRightLayers() {
        List<String> layout = selectorsInOrder("ash.layout");
        for (String cls : CONTRACT_CLASSES) {
            assertTrue(layout.contains(cls), cls + " must be defined in ash.layout");
        }
        List<String> utilities = selectorsInOrder("ash.utilities");
        for (String cls : UTILITY_CLASSES) {
            assertTrue(utilities.contains(cls), cls + " must be defined in ash.utilities");
        }
        for (String other : List.of("ash.tokens", "ash.base")) {
            for (String cls : CONTRACT_CLASSES) {
                assertFalse(selectorsInOrder(other).contains(cls), cls + " must not be in " + other);
            }
        }
    }

    @Test
    void everyConfigurablePropertyIsResetToInitialOnItsPrimitivesBaseRule() {
        for (Map.Entry<String, List<String>> block : RESET_PROPERTIES.entrySet()) {
            Map<String, String> base = rulesIn("ash.layout").get(block.getKey());
            assertNotNull(base, block.getKey());
            for (String property : block.getValue()) {
                assertEquals("initial", base.get(property), block.getKey() + " must reset " + property);
            }
        }
    }

    @Test
    void layoutReadsOnlyAshPropertiesAndNeverUsesImportant() {
        Matcher vars = Pattern.compile("var\\((--[a-z0-9-]+)").matcher(layerBodies.get("ash.layout"));
        int seen = 0;
        while (vars.find()) {
            seen++;
            assertTrue(vars.group(1).startsWith("--ash-"), "layout reads a non-Ashlar property: " + vars.group(1));
        }
        assertTrue(seen > 0);
        assertFalse(css.contains("!important"));
    }

    @Test
    void everyModifierComesAfterItsBaseRuleSoItWinsAtEqualSpecificity() {
        List<String> order = selectorsInOrder("ash.layout");
        for (String block : List.of("container", "stack", "grid", "split", "cluster")) {
            String base = ".ash-" + block;
            int baseIndex = order.indexOf(base);
            assertTrue(baseIndex >= 0, base);
            for (String selector : order) {
                if (selector.startsWith(base + "--")) {
                    assertTrue(order.indexOf(selector) > baseIndex, selector + " must follow " + base);
                }
            }
        }
    }

    @Test
    void theGapModifiersMeanTheSameOnEveryPrimitive() {
        Map<String, Map<String, String>> rules = rulesIn("ash.layout");
        for (String block : List.of("stack", "grid", "split", "cluster")) {
            String property = "--ash-" + block + "-gap";
            assertEquals("var(--ash-space-2)", rules.get(".ash-" + block + "--sm").get(property), block + " --sm");
            assertEquals("var(--ash-space-6)", rules.get(".ash-" + block + "--lg").get(property), block + " --lg");
            assertEquals("var(--ash-space-10)", rules.get(".ash-" + block + "--xl").get(property), block + " --xl");
            assertEquals("var(" + "--ash-" + block + "-gap" + ", var(--ash-space-4))",
                    rules.get(".ash-" + block).get("gap"), block + " default gap is 1rem");
        }
        assertFalse(rules.containsKey(".ash-container--sm"));
    }

    @Test
    void responsiveRulesUseOnlyTheTwoDocumentedLiteralBreakpoints() {
        Set<String> conditions = new java.util.LinkedHashSet<>();
        for (CssScanner.Rule rule : CssScanner.flatRules(layerBodies.get("ash.layout"))) {
            if (!rule.media().isEmpty()) {
                conditions.add(rule.media());
                if (rule.media().contains("64rem")) {
                    assertTrue(rule.selector().equals(".ash-container"), "64rem is only for container padding: " + rule.selector());
                }
            }
        }
        assertEquals(Set.of("@media (min-width: 40rem)", "@media (min-width: 64rem)"), conditions);
    }

    @Test
    void layoutDeclarationsUseLogicalPropertiesNotPhysicalOnes() {
        Set<String> physical = Set.of("width", "height", "min-width", "max-width", "min-height", "max-height",
                "margin-left", "margin-right", "padding-left", "padding-right", "left", "right", "float");
        for (String layer : List.of("ash.layout", "ash.utilities")) {
            for (CssScanner.Rule rule : CssScanner.flatRules(layerBodies.get(layer))) {
                for (String name : CssScanner.declarations(rule.block()).keySet()) {
                    // The visually-hidden utility is an intentional exception: it sets exact pixel sizes.
                    if (!rule.selector().equals(".ash-sr-only")) {
                        assertFalse(physical.contains(name), rule.selector() + " uses the physical property " + name);
                    }
                }
            }
        }
    }

    @Test
    void theSpacingTokensAreDefinedWithTheDocumentedValues() {
        Map<String, String> root = rulesIn("ash.tokens").get(":root");
        assertEquals("0.5rem", root.get("--ash-space-2"));
        assertEquals("0.75rem", root.get("--ash-space-3"));
        assertEquals("1rem", root.get("--ash-space-4"));
        assertEquals("1.5rem", root.get("--ash-space-6"));
        assertEquals("2rem", root.get("--ash-space-8"));
        assertEquals("2.5rem", root.get("--ash-space-10"));
        assertEquals(SPACING_TOKENS.size(), root.keySet().stream().filter(k -> k.startsWith("--ash-space-")).count());
        // Spacing is not themed: dark mode must not redefine it.
        assertTrue(rulesIn("ash.tokens").get(":root[data-theme=\"dark\"]").keySet().stream().noneMatch(k -> k.startsWith("--ash-space-")));
    }

    @Test
    void truncateDeclaresTheEllipsisBehaviour() {
        Map<String, String> t = rulesIn("ash.utilities").get(".ash-truncate");
        assertNotNull(t);
        assertEquals("hidden", t.get("overflow"));
        assertEquals("ellipsis", t.get("text-overflow"));
        assertEquals("nowrap", t.get("white-space"));
        assertEquals("0", t.get("min-inline-size"));
    }

    @Test
    void everyCustomPropertyAndEveryClassIsAshPrefixed() {
        for (Map.Entry<String, String> layer : layerBodies.entrySet()) {
            for (CssScanner.Rule rule : CssScanner.flatRules(layer.getValue())) {
                for (String name : CssScanner.declarations(rule.block()).keySet()) {
                    if (name.startsWith("--")) {
                        assertTrue(name.startsWith("--ash-"), "custom property " + name + " in " + layer.getKey());
                    }
                }
                Matcher classes = Pattern.compile("\\.([A-Za-z_][\\w-]*)").matcher(rule.selector());
                while (classes.find()) {
                    assertTrue(classes.group(1).startsWith("ash-"), "class ." + classes.group(1) + " in " + layer.getKey());
                }
            }
        }
    }

    @Test
    void everyTokenReadingAForeignVariableHasAStandaloneFallback() {
        Matcher foreign = Pattern.compile("var\\((--(?!ash-)[a-z0-9-]+)\\s*([,)])").matcher(layerBodies.get("ash.tokens"));
        int seen = 0;
        while (foreign.find()) {
            seen++;
            assertEquals(",", foreign.group(2), foreign.group(1) + " is read without a fallback, so it would require Tailwind");
        }
        assertTrue(seen > 0, "expected the tokens to bridge existing theme variables");
    }

    @Test
    void darkModeDefinesOnlyNamesTheLightSetDefines() {
        Map<String, Map<String, String>> rules = rulesIn("ash.tokens");
        assertEquals(Set.of(":root", ":root[data-theme=\"dark\"]"), rules.keySet());
        Set<String> light = rules.get(":root").keySet();
        Set<String> dark = rules.get(":root[data-theme=\"dark\"]").keySet();
        assertTrue(light.containsAll(dark), "dark defines a token light does not: " + dark);
        assertFalse(dark.isEmpty());
    }

    @Test
    void baseStylesAreScopedAndSetNoGlobalReset() {
        for (String selectorList : rulesIn("ash.base").keySet()) {
            for (String selector : CssScanner.selectors(selectorList)) {
                assertTrue(selector.startsWith(":where(") && selector.contains("ash-"),
                        "base selector is not scoped to ash-* classes: " + selector);
                assertFalse(selector.matches("^(\\*|html|body|:root).*"), selector);
            }
        }
    }

    @Test
    void srOnlyDeclaresTheEssentialVisuallyHiddenProperties() {
        Map<String, String> sr = rulesIn("ash.utilities").get(".ash-sr-only");
        assertNotNull(sr, ".ash-sr-only must live in ash.utilities");
        assertEquals("absolute", sr.get("position"));
        assertEquals("1px", sr.get("width"));
        assertEquals("1px", sr.get("height"));
        assertEquals("0", sr.get("padding"));
        assertEquals("-1px", sr.get("margin"));
        assertEquals("hidden", sr.get("overflow"));
        assertEquals("nowrap", sr.get("white-space"));
        assertEquals("0", sr.get("border-width"));
        assertEquals("inset(50%)", sr.get("clip-path"));
        assertEquals("rect(0, 0, 0, 0)", sr.get("clip"));
    }
}
