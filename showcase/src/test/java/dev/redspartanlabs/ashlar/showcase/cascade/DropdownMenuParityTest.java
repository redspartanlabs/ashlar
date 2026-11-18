package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Computed-style parity WITH Tailwind present: every case of {@link DropdownMenuBrowser#cases} in all 22 configurations (light and dark, with no skin and with each of the
 * ten), the legacy markup on the Dropdown Menu oracle (the pre-migration stylesheet plus the legacy classes) against the migrated markup on the application stylesheet (which
 * now carries dropdown-menu.css and dropdown-menu-item.css, and the skins' one new rule), every property of every element of the component and every element box. Exact, with
 * the single normalization the other families use: a computed box-shadow with its fully transparent zero-size layers removed ({@link TextEntryBrowser#visibleShadow}),
 * because Tailwind composes its shadow from five layers and leaves the unused ones transparent. Two more cases put the trigger in a {@code <header>} directly in the body,
 * where civic's masthead rule reaches it, at rest and under the pointer.
 */
class DropdownMenuParityTest {

    private static final List<String> FINDINGS = new ArrayList<>();
    private static final Map<String, Integer> APPLIED = new LinkedHashMap<>();
    private static int configurations;
    private static int measuredValues;
    private static boolean rewritten;
    private static List<String> uncompiled;
    private static String oracleCss;
    private static String legacyBody;

    static List<DropdownMenuBrowser.Case> headerCases() {
        String closed = OverlayBrowser.withoutScripts(DropdownMenuBrowser.menu("Account", null, "Profile|#p"));
        return List.of(new DropdownMenuBrowser.Case("hd1", "in body > header, at rest", DropdownMenuBrowser.marked(closed).replaceAll("dropdown-menu-\\d+", "hd1-menu"), null,
                        List.of()),
                new DropdownMenuBrowser.Case("hd2", "in body > header, under the pointer", DropdownMenuBrowser.marked(closed).replaceAll("dropdown-menu-\\d+", "hd2-menu"),
                        "[data-dropdown-menu-trigger]", List.of("hover")));
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("dropdown-menu-parity");
        List<DropdownMenuBrowser.Case> cases = DropdownMenuBrowser.cases();
        List<DropdownMenuBrowser.Case> header = headerCases();
        String[] bodies = DropdownMenuBrowser.bodies(cases);
        String[] headers = DropdownMenuBrowser.bodies(header);
        rewritten = !bodies[0].equals(bodies[1]);
        oracleCss = java.nio.file.Files.readString(session.generatedDir().resolve("dropdown-menu-oracle.css"));
        legacyBody = bodies[1] + headers[1];
        Set<String> tokens = new TreeSet<>(new DropdownMenuLegacyMarkup().allTokens());
        tokens.addAll(List.of("inline-block", "shrink-0", "h-4", "w-4"));
        uncompiled = ParityOracle.uncompiled(oracleCss, tokens);
        List<DropdownMenuBrowser.Case> all = new ArrayList<>(cases);
        all.addAll(header);
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            for (ParityConfigs.Config config : ParityConfigs.all()) {
                String legacyUrl = server.registerRaw(DropdownMenuBrowser.page(List.of(ParityFrames.generated("dropdown-menu-oracle.css")), config, headers[1], bodies[1]));
                String migratedUrl = server.registerRaw(DropdownMenuBrowser.page(List.of(ParityFrames.APP), config, headers[0], bodies[0]));
                String legacy;
                String migrated;
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    OverlayBrowser.viewport(cdp, 1400, 900);
                    legacy = DropdownMenuBrowser.measure(cdp, legacyUrl, all, false);
                    migrated = DropdownMenuBrowser.measure(cdp, migratedUrl, all, false);
                }
                configurations++;
                measuredValues += legacy.split("\n").length;
                for (DropdownMenuBrowser.Case c : all) {
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
        assertTrue(measuredValues > 50_000, "expected tens of thousands of measured values, got " + measuredValues);
        assertEquals(List.of(), uncompiled, "every legacy class must be compiled into the oracle, or an unstyled legacy would match an unstyled migrated rendering");
    }

    @Test
    void theLegacyRenderingCannotBorrowTheMigratedStyling() {
        // The legacy side is the oracle alone (Tailwind-only, plus the legacy classes): it must hold no Ashlar Dropdown Menu rule, and the legacy markup must carry no
        // Ashlar Dropdown Menu class, so a legacy that matched would match only by the legacy classes doing what the migrated ones do.
        for (String own : List.of(".ash-dropdown-menu", "--ash-dropdown-menu")) {
            assertTrue(!oracleCss.contains(own), "the oracle must not contain " + own);
        }
        assertTrue(!legacyBody.contains("ash-dropdown-menu") && !legacyBody.contains("data-state="), "the legacy markup carries no migrated class or state attribute");
        assertTrue(legacyBody.contains("min-w-40") && legacyBody.contains("origin-top-left") && legacyBody.contains("rotate-180") && legacyBody.contains("opacity-100"),
                "the legacy markup is the legacy markup, in its open states too");
    }

    @Test
    void theOnlyNormalizationAppliedIsTheDocumentedOne() {
        assertTrue(Set.of("invisible-shadow-layers").containsAll(APPLIED.keySet()), APPLIED.toString());
        assertTrue(APPLIED.getOrDefault("invisible-shadow-layers", 0) > 0, "Tailwind's unused shadow layers were met and normalized: " + APPLIED);
    }
}
