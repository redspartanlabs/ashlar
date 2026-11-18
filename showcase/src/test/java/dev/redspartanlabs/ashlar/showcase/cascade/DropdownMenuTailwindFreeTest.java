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
 * Computed-style parity WITHOUT Tailwind: the cases of {@link DropdownMenuParityTest} on a page with no page-wide Tailwind at all, the legacy markup on the scoped legacy
 * stylesheet (the legacy component's own utilities, the legacy Icon's, and the few preflight rules they relied on, for their own elements only) against the migrated markup on
 * Ashlar's real generated stylesheet alone. That is the proof that the migrated Dropdown Menu needs nothing from Tailwind: no utility, no preflight, no theme variable (every
 * palette, radius and width variable it reads has its exact Tailwind value as the fallback). Exact, with the same single documented box-shadow normalization, and one line
 * left out, as in the Modal and Drawer suite ({@link OverlayTailwindFreeTest#comparable}): the STYLE of a border whose width is 0px.
 *
 * <p>Both bare pages carry the showcase's page environment ({@link ParityFrames#environment}), so what the component inherits from the page is the same as on the full
 * Tailwind page. The scoped legacy stylesheet is itself checked: in the same configurations it computes exactly what the full legacy oracle does for every component element,
 * so it is a faithful stand-in.
 */
class DropdownMenuTailwindFreeTest {

    private static final List<String> FINDINGS = new ArrayList<>();
    private static final List<String> SCOPED_VS_FULL = new ArrayList<>();
    private static final Map<String, Integer> APPLIED = new LinkedHashMap<>();
    private static int configurations;

    private static String insertEnvironment(String page, String environment) {
        return page.replace("</head>", "<style>" + environment + "</style></head>");
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("dropdown-menu-tailwind-free");
        List<DropdownMenuBrowser.Case> cases = DropdownMenuBrowser.cases();
        String[] bodies = DropdownMenuBrowser.bodies(cases);
        String environment = ParityFrames.environment(session.generatedDir());
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            for (ParityConfigs.Config config : ParityConfigs.all()) {
                String scopedUrl = server.registerRaw(insertEnvironment(DropdownMenuBrowser.page(List.of(ParityFrames.generated("dropdown-menu-legacy-scoped.css")), config, "",
                        bodies[1]), environment));
                String ashlarUrl = server.registerRaw(insertEnvironment(DropdownMenuBrowser.page(List.of(ParityFrames.ASHLAR), config, "", bodies[0]), environment));
                String fullUrl = server.registerRaw(DropdownMenuBrowser.page(List.of(ParityFrames.generated("dropdown-menu-oracle.css")), config, "", bodies[1]));
                String scoped;
                String ashlar;
                String scopedComponents;
                String fullComponents;
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    OverlayBrowser.viewport(cdp, 1400, 900);
                    scoped = DropdownMenuBrowser.measure(cdp, scopedUrl, cases, false);
                    ashlar = DropdownMenuBrowser.measure(cdp, ashlarUrl, cases, false);
                    scopedComponents = DropdownMenuBrowser.measure(cdp, scopedUrl, cases, true);
                    fullComponents = DropdownMenuBrowser.measure(cdp, fullUrl, cases, true);
                }
                configurations++;
                for (DropdownMenuBrowser.Case c : cases) {
                    String where = config.id() + " " + c.id() + " (" + c.key() + ")";
                    FINDINGS.addAll(TextEntryBrowser.differences(where, OverlayTailwindFreeTest.comparable(OverlayBrowser.slice(scoped, c.id() + ":")),
                            OverlayTailwindFreeTest.comparable(OverlayBrowser.slice(ashlar, c.id() + ":")), APPLIED));
                    SCOPED_VS_FULL.addAll(TextEntryBrowser.differences(where + " scoped vs full", OverlayTailwindFreeTest.comparable(OverlayBrowser.slice(fullComponents, c.id() + ":")),
                            OverlayTailwindFreeTest.comparable(OverlayBrowser.slice(scopedComponents, c.id() + ":")), new LinkedHashMap<>()));
                }
            }
        }
    }

    @Test
    void everyCaseComputesTheSameWithoutTailwindInEveryConfiguration() {
        assertEquals(List.of(), FINDINGS.size() > 60 ? FINDINGS.subList(0, 60) : FINDINGS);
    }

    @Test
    void theScopedLegacyStylesheetIsAFaithfulStandInForTheFullLegacyOracle() {
        assertEquals(List.of(), SCOPED_VS_FULL.size() > 60 ? SCOPED_VS_FULL.subList(0, 60) : SCOPED_VS_FULL);
    }

    @Test
    void allTwentyTwoConfigurationsWereMeasuredAndOnlyTheDocumentedNormalizationApplied() {
        assertEquals(22, configurations);
        assertTrue(Set.of("invisible-shadow-layers").containsAll(APPLIED.keySet()), APPLIED.toString());
    }
}
