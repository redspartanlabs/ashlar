package dev.redspartanlabs.ashlar.css;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The structural check on the generated stylesheet is only worth anything if it
 * actually catches the mistakes it exists for, so this feeds it deliberately bad
 * stylesheets alongside a good one.
 */
class CssScannerTest {

    private static final String STATEMENT = "@layer properties, theme, base, ash.tokens, ash.base, components, utilities;\n";

    private static List<String> violations(String body) {
        return CssScanner.violations(STATEMENT + body);
    }

    @Test
    void aCorrectStylesheetHasNoViolations() {
        assertEquals(List.of(), violations("@layer ash.tokens { :root { --ash-x: 1; } }\n@layer ash.base { .ash-a { color: red; } }\n"));
    }

    @Test
    void anUnlayeredRuleIsDetected() {
        assertTrue(violations("@layer ash.tokens { :root { --ash-x: 1; } }\n.ash-leak { color: red; }\n")
                .stream().anyMatch(v -> v.contains("outside a named ash.* sublayer")));
    }

    @Test
    void aRuleDirectlyInTheParentAshLayerIsDetected() {
        assertTrue(violations("@layer ash { .ash-leak { color: red; } }\n")
                .stream().anyMatch(v -> v.contains("outside a named ash.* sublayer")));
    }

    @Test
    void aRuleInATailwindOwnedLayerIsDetected() {
        assertTrue(violations("@layer utilities { .ash-leak { color: red; } }\n")
                .stream().anyMatch(v -> v.contains("outside a named ash.* sublayer")));
    }

    @Test
    void aNestedLayerOrImportInsideABlockIsDetected() {
        assertTrue(violations("@layer ash.base { @layer inner { .ash-a { color: red; } } }\n")
                .stream().anyMatch(v -> v.contains("contains @import, @layer or @charset")));
        assertTrue(violations("@layer ash.base { @import \"x.css\"; }\n")
                .stream().anyMatch(v -> v.contains("contains @import, @layer or @charset")));
    }

    @Test
    void aSublayerMissingFromTheLeadingStatementIsDetected() {
        assertTrue(violations("@layer ash.undeclared { .ash-a { color: red; } }\n")
                .stream().anyMatch(v -> v.contains("not named in the leading statement")));
    }

    @Test
    void theStatementMustBeFirst() {
        assertTrue(CssScanner.violations(".ash-a { color: red; }\n" + STATEMENT)
                .stream().anyMatch(v -> v.contains("first item is not the layer-order statement")));
        assertTrue(CssScanner.violations("@import \"x.css\";\n" + STATEMENT)
                .stream().anyMatch(v -> v.contains("first item is not the layer-order statement")));
    }

    @Test
    void commentsAndStringsDoNotConfuseTheScanner() {
        String css = STATEMENT + "/* .ash-leak { color: red; } @layer x { } */\n"
                + "@layer ash.base { .ash-a::after { content: \"} { @layer\"; } }\n";
        assertEquals(List.of(), CssScanner.violations(css));
    }

    @Test
    void rulesInsideMediaAndSupportsAreFlattenedWithTheirCondition() {
        List<CssScanner.Rule> rules = CssScanner.flatRules(
                ".a { x: 1; } @media (min-width:40rem) { .b { y: 2; } @supports (display:grid) { .c { z: 3; } } } @font-face { src: url(x); }");
        assertEquals(List.of(
                new CssScanner.Rule("", ".a", " x: 1; "),
                new CssScanner.Rule("@media (min-width:40rem)", ".b", " y: 2; "),
                new CssScanner.Rule("@media (min-width:40rem) and @supports (display:grid)", ".c", " z: 3; ")), rules);
    }

    @Test
    void repeatedLayerBlocksAreAllowedAndNestedMediaDoesNotHideForbiddenAtRules() {
        assertEquals(List.of(), violations("@layer ash.base { .ash-a { x: 1; } }\n@layer ash.base { @media (min-width:40rem) { .ash-b { y: 2; } } }\n"));
        assertTrue(violations("@layer ash.base { @media (min-width:40rem) { @layer inner { .ash-b { y: 2; } } } }\n")
                .stream().anyMatch(v -> v.contains("contains @import, @layer or @charset")));
    }

    @Test
    void declarationsAndSelectorsAreSplitCorrectly() {
        assertEquals(Map.of("a", "1", "b", "var(--x, 2)"), CssScanner.declarations("a: 1; b: var(--x, 2);"));
        assertEquals(List.of(":where([class^=\"ash-\"], [class*=\" ash-\"])", ".ash-b"),
                CssScanner.selectors(":where([class^=\"ash-\"], [class*=\" ash-\"]), .ash-b"));
    }
}
