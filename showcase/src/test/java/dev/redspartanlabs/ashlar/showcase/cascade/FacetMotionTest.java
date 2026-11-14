package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Faceted Search, Facet and Facet Option do not move: nothing transitions and nothing animates, so there is nothing for {@code prefers-reduced-motion} to switch off. This measures that on
 * pages that DO run transitions (the other parity pages switch them off so a state is measured at rest), in a real browser, for the legacy rendering and the migrated one, each in its
 * own browser, with and without the reduced-motion preference: the computed {@code transition-property}, {@code transition-duration}, {@code animation-name} and
 * {@code animation-duration} of every element of the facets are the same on both sides and are the browser's defaults (all, 0s, none, 0s); then a real state change (the first option is
 * clicked, a row is hovered through the browser's own pseudo-state API) must start no transition and no animation anywhere in the document.
 */
class FacetMotionTest {

    private static final String PROBE = """
            (function () {
              var root = document.querySelector('[data-field]');
              var seen = {};
              [root].concat([].slice.call(root.querySelectorAll('*'))).forEach(function (e) {
                var s = getComputedStyle(e);
                var key = [s.transitionProperty, s.transitionDuration, s.transitionDelay, s.animationName, s.animationDuration].join(' | ');
                seen[key] = (seen[key] || 0) + 1;
              });
              var before = document.getAnimations().length;
              document.querySelector('[data-field] input').click();
              var after = document.getAnimations().length;
              return JSON.stringify({ styles: seen, before: before, after: after, reduced: matchMedia('(prefers-reduced-motion: reduce)').matches });
            })()
            """;

    private static String legacy;
    private static String migrated;
    private static String legacyReduced;
    private static String migratedReduced;
    private static String hoverMoving;

    /** A parity page with the shared "no transitions" rule taken out. */
    private static String livePage(List<String> sheets, String body) {
        String page = TextEntryBrowser.page(sheets, ParityConfigs.get("light/none"), "", body, "");
        String off = "<style>*,*::before,*::after{transition:none !important;animation-play-state:paused !important}</style>";
        assertTrue(page.contains(off), "the shared page rule that switches transitions off is expected here, and is removed");
        return page.replace(off, "");
    }

    private static String probe(ParitySession session, String url, boolean reduced) throws Exception {
        try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
            if (reduced) {
                cdp.call("Emulation.setEmulatedMedia", "{\"features\":[{\"name\":\"prefers-reduced-motion\",\"value\":\"reduce\"}]}");
            }
            cdp.navigate(url);
            return cdp.evaluate(PROBE);
        }
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("facet-motion");
        FacetBrowser.Case states = FacetBrowser.CASES.stream().filter(c -> c.key().equals("counts and a checked option")).findFirst().orElseThrow();
        String body = FacetBrowser.marked(FacetBrowser.build(states, "fm1"));
        String legacyBody = new FacetLegacyMarkup().rewrite(body);
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            String legacyUrl = server.registerRaw(livePage(List.of(ParityFrames.generated("facet-oracle.css")), legacyBody));
            String migratedUrl = server.registerRaw(livePage(List.of(ParityFrames.APP), body));
            legacy = probe(session, legacyUrl, false);
            migrated = probe(session, migratedUrl, false);
            legacyReduced = probe(session, legacyUrl, true);
            migratedReduced = probe(session, migratedUrl, true);
            // A real hover: the row is hovered through the browser's pseudo-state API and nothing may start moving.
            try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                cdp.navigate(migratedUrl);
                cdp.force(cdp.nodes(FacetBrowser.row("ready")), List.of("hover"));
                hoverMoving = cdp.evaluate("String(document.getAnimations().length)");
            }
        }
    }

    @Test
    void theFacetsDeclareNoTransitionAndNoAnimationAnywhereAndTheLegacyOnesDidNotEither() {
        java.util.regex.Pattern single = java.util.regex.Pattern.compile("\"styles\":\\{\"all \\| 0s \\| 0s \\| none \\| 0s\":\\d+\\},");
        assertTrue(single.matcher(migrated).find(), "every element has the browser's defaults, and only those: " + migrated);
        assertTrue(single.matcher(legacy).find(), "and so did the legacy rendering: " + legacy);
    }

    @Test
    void bothRenderingsAreIdenticalWithAndWithoutTheReducedMotionPreference() {
        assertEquals(legacy.replace("\"reduced\":false", "\"reduced\":X"), migrated.replace("\"reduced\":false", "\"reduced\":X"));
        assertEquals(legacyReduced, migratedReduced);
        assertTrue(migratedReduced.contains("\"reduced\":true") && migrated.contains("\"reduced\":false"), "the preference was really emulated");
        assertEquals(migrated.replace("\"reduced\":false", "\"reduced\":X"), migratedReduced.replace("\"reduced\":true", "\"reduced\":X"), "the preference changes nothing: there is no motion to remove");
    }

    @Test
    void aStateChangeStartsNothingMoving() {
        for (String r : List.of(legacy, migrated, legacyReduced, migratedReduced)) {
            assertTrue(r.contains("\"before\":0,\"after\":0"), "no transition or animation before or after toggling an option: " + r);
        }
        assertEquals("0", hoverMoving, "hovering a row starts no transition");
    }
}
