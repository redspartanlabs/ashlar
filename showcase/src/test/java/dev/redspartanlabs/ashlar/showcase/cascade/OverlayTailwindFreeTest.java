package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Computed-style parity WITHOUT Tailwind: the cases of {@link OverlayParityTest} on a page with no page-wide Tailwind at all, the legacy markup on the scoped legacy stylesheet
 * (the legacy overlay's own utilities and the few preflight rules it relied on, for its own elements only) against the migrated markup on Ashlar's real generated stylesheet
 * alone. That is the proof that the migrated Modal and Drawer need nothing from Tailwind: no utility, no preflight, no theme variable (every palette variable they read has
 * its exact Tailwind value as the fallback). Exact, with the same single documented box-shadow normalisation, and one line left out, as in Facet's suite: the STYLE of a
 * border whose width is 0px (Tailwind's preflight gives every element an invisible {@code border: 0 solid}; the bare page has none, and a zero-width border draws nothing
 * whatever its style). It is left out only where that element's top border width is 0px; every border width and colour is still compared.
 *
 * <p>Both bare pages carry the showcase's page environment ({@link ParityFrames#environment}), as Facet's do, so what the overlay inherits from the page (the root line
 * height, the font) is the same as on the full Tailwind page.
 *
 * <p>The scoped legacy stylesheet is itself checked: in the same configurations it computes exactly what the full legacy oracle does for every overlay element, so it is a
 * faithful stand-in for "the legacy overlay without the rest of Tailwind".
 */
class OverlayTailwindFreeTest {

    private static final List<String> FINDINGS = new ArrayList<>();
    private static final List<String> SCOPED_VS_FULL = new ArrayList<>();
    private static final Map<String, Integer> APPLIED = new LinkedHashMap<>();
    private static int configurations;
    private static int leftOut;

    private static String insertEnvironment(String page, String environment) {
        return page.replace("</head>", "<style>" + environment + "</style></head>");
    }

    /** The measurement without the border-top-style lines of elements whose top border is 0px wide (see the class comment). */
    static String comparable(String measurement) {
        Set<String> zeroTop = measurement.lines().map(l -> l.split("\t", -1)).filter(p -> p.length == 3 && p[1].equals("border-top-width") && p[2].equals("0px"))
                .map(p -> p[0]).collect(Collectors.toSet());
        return measurement.lines().filter(l -> {
            String[] p = l.split("\t", -1);
            boolean drop = p.length == 3 && p[1].equals("border-top-style") && zeroTop.contains(p[0]);
            if (drop) {
                leftOut++;
            }
            return !drop;
        }).collect(Collectors.joining("\n"));
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("overlay-tailwind-free");
        List<OverlayBrowser.Case> cases = OverlayBrowser.cases();
        String[] bodies = OverlayParityTest.bodies(cases);
        String environment = ParityFrames.environment(session.generatedDir());
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            for (ParityConfigs.Config config : ParityConfigs.all()) {
                String scopedUrl = server.registerRaw(insertEnvironment(TextEntryBrowser.page(List.of(ParityFrames.generated("overlay-legacy-scoped.css")), config, "", bodies[1], ""),
                        environment));
                String ashlarUrl = server.registerRaw(insertEnvironment(TextEntryBrowser.page(List.of(ParityFrames.ASHLAR), config, "", bodies[0], ""), environment));
                String fullUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("overlay-oracle.css")), config, "", bodies[1], ""));
                String scoped;
                String ashlar;
                String full;
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    OverlayBrowser.viewport(cdp, 900, 700);
                    scoped = OverlayBrowser.measure(cdp, scopedUrl, cases);
                    ashlar = OverlayBrowser.measure(cdp, ashlarUrl, cases);
                    full = OverlayBrowser.measure(cdp, fullUrl, cases);
                }
                configurations++;
                for (OverlayBrowser.Case c : cases) {
                    String where = config.id() + " " + c.id() + " (" + c.key() + ")";
                    FINDINGS.addAll(TextEntryBrowser.differences(where, comparable(OverlayBrowser.slice(scoped, c.id() + ":")), comparable(OverlayBrowser.slice(ashlar, c.id() + ":")),
                            APPLIED));
                    SCOPED_VS_FULL.addAll(TextEntryBrowser.differences(where + " scoped vs full", comparable(OverlayBrowser.slice(full, c.id() + ":")),
                            comparable(OverlayBrowser.slice(scoped, c.id() + ":")), new LinkedHashMap<>()));
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
        // The caller's own content (data-parity="raw": the body paragraph and the footer spans) is styled by preflight on the full page and not on the bare one, so it is
        // left out of this one check; every overlay element is compared.
        List<String> overlayOnly = SCOPED_VS_FULL.stream().filter(f -> !f.contains(":p |") && !f.contains(":span |")).toList();
        assertEquals(List.of(), overlayOnly.size() > 60 ? overlayOnly.subList(0, 60) : overlayOnly);
    }

    @Test
    void allTwentyTwoConfigurationsWereMeasuredAndOnlyTheDocumentedNormalisationApplied() {
        assertEquals(22, configurations);
        assertTrue(leftOut > 0, "the zero-width border-style filter was exercised: " + leftOut);
        assertTrue(Set.of("invisible-shadow-layers").containsAll(APPLIED.keySet()), APPLIED.toString());
    }
}
