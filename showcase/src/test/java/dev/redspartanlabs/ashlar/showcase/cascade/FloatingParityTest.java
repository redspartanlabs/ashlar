package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Computed-style parity WITH Tailwind present: every Tooltip and Popover case ({@link FloatingBrowser#cases}: each placement, hidden, shown in the closed visual state and
 * open, a wrapping label, a nudged bubble, a surface with and without a title), rendered from the REAL templates, measured in all 22 configurations (both themes, no skin and
 * the ten showcase skins), legacy (the rewritten markup on the Tooltip and Popover oracle) against migrated (the markup as served, on the showcase's own stylesheet). Every
 * property of {@link OverlayBrowser#PROPERTIES} (translate, scale and rotate among them) and every box is compared for every element, EXACTLY except the one documented
 * normalization the other families use: a computed box-shadow with its fully transparent zero-size layers removed ({@link TextEntryBrowser#visibleShadow}), because Tailwind
 * composes a shadow from unused layers and the migrated rule writes only what it draws.
 *
 * <p>The transition properties are not compared here (the page turns transitions off so that every state is measured at rest); {@link FloatingMotionTest} compares them.
 */
class FloatingParityTest {

    static final List<String> FINDINGS = new ArrayList<>();
    private static final Map<String, Integer> APPLIED = new LinkedHashMap<>();
    private static int configurations;
    private static int measuredValues;
    private static boolean rewritten;
    private static List<String> uncompiled;
    private static String oracleCss;
    private static String legacyBody;

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("floating-parity");
        List<OverlayBrowser.Case> cases = FloatingBrowser.cases();
        String[] bodies = FloatingBrowser.bodies(cases);
        rewritten = !bodies[0].equals(bodies[1]);
        oracleCss = java.nio.file.Files.readString(session.generatedDir().resolve("floating-oracle.css"));
        legacyBody = bodies[1];
        uncompiled = ParityOracle.uncompiled(oracleCss, new FloatingLegacyMarkup().allTokens());
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            for (ParityConfigs.Config config : ParityConfigs.all()) {
                String legacyUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("floating-oracle.css")), config, "", bodies[1], ""));
                String migratedUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.APP), config, "", bodies[0], ""));
                String legacy;
                String migrated;
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    OverlayBrowser.viewport(cdp, 1400, 900);
                    legacy = FloatingBrowser.measure(cdp, legacyUrl, cases, false);
                    migrated = FloatingBrowser.measure(cdp, migratedUrl, cases, false);
                }
                configurations++;
                measuredValues += legacy.split("\n").length;
                for (OverlayBrowser.Case c : cases) {
                    FINDINGS.addAll(TextEntryBrowser.differences(config.id() + " " + c.id() + " (" + c.key() + ")", OverlayBrowser.slice(legacy, c.id() + ":"),
                            OverlayBrowser.slice(migrated, c.id() + ":"), APPLIED));
                }
            }
        }
    }

    @Test
    void everyCaseComputesTheSameInEveryConfigurationWithTailwindPresent() {
        assertEquals(List.of(), FINDINGS.size() > 60 ? FINDINGS.subList(0, 60) : FINDINGS);
    }

    @Test
    void theComparisonIsNotVacuous() {
        assertTrue(rewritten, "the legacy markup must differ from the migrated markup");
        assertEquals(22, configurations);
        assertTrue(measuredValues > 20_000, "expected tens of thousands of measured values, got " + measuredValues);
        assertEquals(List.of(), uncompiled, "every legacy class must be compiled into the oracle, or an unstyled legacy would match an unstyled migrated rendering");
    }

    @Test
    void theLegacyRenderingCannotBorrowTheMigratedStyling() {
        // The legacy side is the oracle alone (Tailwind-only, plus the legacy classes): it must hold no Ashlar Tooltip or Popover rule, and the legacy markup must carry no
        // Ashlar Tooltip or Popover class, so a legacy that matched would match only by the legacy classes doing what the migrated ones do.
        for (String own : List.of(".ash-tooltip", ".ash-popover", "--ash-tooltip", "--ash-popover")) {
            assertTrue(!oracleCss.contains(own), "the oracle must not contain " + own);
        }
        assertTrue(!legacyBody.contains("ash-tooltip") && !legacyBody.contains("ash-popover"), "the legacy markup carries no migrated class");
        assertTrue(!legacyBody.contains("data-state=") && legacyBody.contains("max-w-64") && legacyBody.contains("ring-slate-200") && legacyBody.contains("rotate-45"),
                "the legacy markup is the legacy markup");
    }

    @Test
    void theOnlyNormalizationAppliedIsTheDocumentedOne() {
        assertTrue(Set.of("invisible-shadow-layers").containsAll(APPLIED.keySet()), APPLIED.toString());
        assertTrue(APPLIED.getOrDefault("invisible-shadow-layers", 0) > 0, "Tailwind's unused shadow layers were met and normalized: " + APPLIED);
    }
}
