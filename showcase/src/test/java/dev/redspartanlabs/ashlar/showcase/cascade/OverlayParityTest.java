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
 * Computed-style parity WITH Tailwind present: every Modal and Drawer case ({@link OverlayBrowser#cases}: each size and placement, with and without a footer and a close
 * button, hidden, shown in the closed visual state and open, and the close button hovered, focused and keyboard-focused), rendered from the REAL templates, measured in all 22
 * configurations (both themes, no skin and the ten showcase skins), legacy (the rewritten markup on the Modal and Drawer oracle) against migrated (the markup as served, on the
 * showcase's own stylesheet). Every property of {@link OverlayBrowser#PROPERTIES} and every box is compared for every element, EXACTLY except one documented normalisation, the
 * same as Text Entry's: a computed box-shadow with its fully transparent zero-size layers removed ({@link TextEntryBrowser#visibleShadow}), because Tailwind composes a shadow
 * from unused layers and the migrated rule writes only what it draws.
 *
 * <p>The transition properties are not compared here (the page turns transitions off so that every state is measured at rest); {@link OverlayMotionTest} compares them.
 */
class OverlayParityTest {

    static final List<String> FINDINGS = new ArrayList<>();
    private static final Map<String, Integer> APPLIED = new LinkedHashMap<>();
    private static int configurations;
    private static int measuredValues;
    private static boolean rewritten;
    private static List<String> uncompiled;
    private static String oracleCss;
    private static String legacyBody;

    /** The migrated markup of every case in its field, and the same markup rewritten to the legacy classes. */
    static String[] bodies(List<OverlayBrowser.Case> cases) throws Exception {
        StringBuilder migrated = new StringBuilder();
        StringBuilder legacy = new StringBuilder();
        for (OverlayBrowser.Case c : cases) {
            migrated.append(OverlayBrowser.field(c, c.markup()));
            legacy.append(OverlayBrowser.field(c, OverlayBrowser.legacy(c.markup())));
        }
        return new String[] {migrated.toString(), legacy.toString()};
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("overlay-parity");
        List<OverlayBrowser.Case> cases = OverlayBrowser.cases();
        String[] bodies = bodies(cases);
        rewritten = !bodies[0].equals(bodies[1]);
        oracleCss = java.nio.file.Files.readString(session.generatedDir().resolve("overlay-oracle.css"));
        legacyBody = bodies[1];
        uncompiled = ParityOracle.uncompiled(oracleCss, new OverlayLegacyMarkup().allTokens());
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            for (ParityConfigs.Config config : ParityConfigs.all()) {
                String legacyUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("overlay-oracle.css")), config, "", bodies[1], ""));
                String migratedUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.APP), config, "", bodies[0], ""));
                String legacy;
                String migrated;
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    OverlayBrowser.viewport(cdp, 900, 700);
                    legacy = OverlayBrowser.measure(cdp, legacyUrl, cases);
                    migrated = OverlayBrowser.measure(cdp, migratedUrl, cases);
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
        assertTrue(measuredValues > 50_000, "expected tens of thousands of measured values per configuration set, got " + measuredValues);
        assertEquals(List.of(), uncompiled, "every legacy class must be compiled into the oracle, or an unstyled legacy would match an unstyled migrated rendering");
    }

    @Test
    void theLegacyRenderingCannotBorrowTheMigratedStyling() {
        // The legacy side is the oracle alone (Tailwind-only, plus the legacy classes): it must hold no Ashlar overlay rule, and the legacy markup must carry no Ashlar
        // overlay class, so a legacy that matched would match only by the legacy classes doing what the migrated ones do.
        for (String own : List.of(".ash-overlay", ".ash-modal", ".ash-drawer", "--ash-overlay")) {
            assertTrue(!oracleCss.contains(own), "the oracle must not contain " + own);
        }
        assertTrue(!legacyBody.contains("ash-overlay") && !legacyBody.contains("ash-modal") && !legacyBody.contains("ash-drawer"), "the legacy markup carries no migrated class");
        assertTrue(legacyBody.contains("bg-slate-900/50") && legacyBody.contains("data-drawer-closed-class"), "the legacy markup is the legacy markup");
    }

    @Test
    void theOnlyNormalisationAppliedIsTheDocumentedOne() {
        assertTrue(Set.of("invisible-shadow-layers").containsAll(APPLIED.keySet()), APPLIED.toString());
    }
}
