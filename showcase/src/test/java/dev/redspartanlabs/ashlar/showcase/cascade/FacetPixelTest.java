package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The facets' painting, compared in PIXELS, in the same browser, with EXACT equality and no tolerance of any kind. A native-appearance checkbox is drawn by the browser, not by CSS, so
 * computed styles alone cannot say that two renderings look the same: this takes a screenshot of every field in the legacy rendering and in the migrated rendering, in one browser
 * process, in all 22 configurations, and requires identical bytes. Nothing here is the Table family's tolerance: {@link PixelPolicy} is not used, and a difference of one pixel fails.
 *
 * <p>No golden image is stored: native painting differs by operating system and browser version, so only a rendering of the same page in the same browser is a fair reference. The
 * fields are captured with forced states (hover, active, focus, focus-visible) exactly as the parity suite forces them.
 */
class FacetPixelTest {

    /** Fields per browser: at most twice this many screenshots in one session. */
    private static final int CHUNK = 11;

    private static final List<String> MISMATCHES = new ArrayList<>();
    private static final Set<String> DISTINCT_LIGHT = new HashSet<>();
    private static final Set<String> DISTINCT_DARK = new HashSet<>();
    private static final Map<String, String> CONTROL_RESULTS = new LinkedHashMap<>();
    private static int compared;

    static String shot(DevToolsSession cdp, String fieldId) throws Exception {
        cdp.evaluate("(function(){document.querySelector('[data-field=\"" + fieldId + "\"]').scrollIntoView({block:\"center\"});return \"ok\"})()");
        String box = cdp.evaluate("(function(){var b=document.querySelector('[data-field=\"" + fieldId + "\"]').getBoundingClientRect();return JSON.stringify({x:b.x+scrollX-6,y:b.y+scrollY-6,width:b.width+12,height:b.height+12})})()");
        String response = cdp.call("Page.captureScreenshot", "{\"clip\":" + box.replace("}", ",\"scale\":1}") + "}");
        int at = response.indexOf("\"data\":\"");
        assertTrue(at > 0, response);
        return response.substring(at + 8, response.indexOf('"', at + 8));
    }

    static void capture(DevToolsSession cdp, String url, List<FacetParityTest.Field> fields, java.util.function.BiConsumer<FacetParityTest.Field, String> sink) throws Exception {
        cdp.navigate(url);
        for (FacetParityTest.Field f : fields) {
            List<String> pseudo = f.spec().pseudo();
            List<Integer> nodes = pseudo.isEmpty() ? List.of() : cdp.nodes(FacetBrowser.target(f.spec(), f.id()));
            if (!pseudo.isEmpty()) {
                cdp.force(nodes, pseudo);
            }
            sink.accept(f, shot(cdp, f.id()));
            if (!pseudo.isEmpty()) {
                cdp.force(nodes, List.of());
            }
        }
    }

    /** Both pages, in chunks of fields, each chunk in its own browser holding BOTH renderings: the comparison is always between two pictures taken by the same browser process. */
    private static void compareChunks(ParitySession session, String legacyUrl, String migratedUrl, List<FacetParityTest.Field> fields, String where, List<String> mismatches,
            java.util.function.BiConsumer<FacetParityTest.Field, String> legacySink) throws Exception {
        Map<String, String> legacy = new LinkedHashMap<>();
        Map<String, String> migrated = new LinkedHashMap<>();
        for (int from = 0; from < fields.size(); from += CHUNK) {
            List<FacetParityTest.Field> chunk = fields.subList(from, Math.min(fields.size(), from + CHUNK));
            try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                capture(cdp, legacyUrl, chunk, (f, png) -> legacy.put(f.id(), png));
                capture(cdp, migratedUrl, chunk, (f, png) -> migrated.put(f.id(), png));
            }
        }
        for (FacetParityTest.Field f : fields) {
            if (!legacy.get(f.id()).equals(migrated.get(f.id()))) {
                mismatches.add(where + " " + f.id() + " (" + f.spec().key() + ")");
            }
            legacySink.accept(f, legacy.get(f.id()));
        }
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("facet-pixels");
        List<FacetParityTest.Field> fields = FacetParityTest.fields();
        String[] bodies = FacetParityTest.bodies(fields);
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            for (ParityConfigs.Config config : ParityConfigs.all()) {
                String legacyUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("facet-oracle.css")), config, "", bodies[1], ""));
                String migratedUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.APP), config, "", bodies[0], ""));
                compareChunks(session, legacyUrl, migratedUrl, fields, config.id(), MISMATCHES, (f, png) -> {
                    compared++;
                    if (config.id().equals("light/none")) {
                        DISTINCT_LIGHT.add(png);
                    }
                    if (config.id().equals("dark/none")) {
                        DISTINCT_DARK.add(png);
                    }
                });
            }
            FacetPixelControls.run(session, server, CONTROL_RESULTS);
        }
    }

    @Test
    void everyFieldIsTheSamePictureInTheLegacyAndTheMigratedRenderingInAll22Configurations() {
        assertEquals(22 * FacetBrowser.CASES.size(), compared);
        assertEquals(List.of(), MISMATCHES.stream().limit(40).toList(), MISMATCHES.size() + " of " + compared + " fields differ");
    }

    @Test
    void theComparisonIsNotVacuousTheStatesLookDifferentFromEachOther() {
        assertTrue(DISTINCT_LIGHT.size() >= 18, "distinct pictures in light: " + DISTINCT_LIGHT.size());
        assertTrue(DISTINCT_DARK.size() >= 18, "distinct pictures in dark: " + DISTINCT_DARK.size());
    }

    @Test
    void everyBrokenRenderingTriedWasToldApartFromTheRealOne() {
        assertEquals(List.of(), CONTROL_RESULTS.entrySet().stream().filter(e -> !e.getValue().equals("rejected")).map(e -> e.getKey() + " -> " + e.getValue()).toList(),
                "a broken rendering that produced the same picture would mean the comparison cannot see that kind of break");
        assertEquals(FacetPixelControls.CONTROLS.size(), CONTROL_RESULTS.size());
        assertNotEquals(0, CONTROL_RESULTS.size());
    }
}
