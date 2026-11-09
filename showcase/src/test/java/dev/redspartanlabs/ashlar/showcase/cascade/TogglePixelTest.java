package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Toggle's painting, compared in PIXELS, in the same browser. Computed styles say what each element is; the picture says what they add up to (the hidden input's two clipping
 * declarations paint nothing, the thumb's shadow, the ring, the rounded ends, the colours at rest and under the pointer): this takes a screenshot of every Toggle field in the legacy
 * rendering and in the migrated rendering, in one browser process, in all 22 configurations, and requires identical bytes.
 *
 * <p>No golden image is stored: painting differs by operating system and browser version, so only a rendering of the same page in the same browser is a fair reference. The fields
 * are captured with forced states (focus-visible, plain focus, hover, active) exactly as the parity suite forces them. Also compared, in five configurations, is the migrated toggle
 * with Ashlar's stylesheet ALONE against the legacy toggle with only its own utilities (no Tailwind page reset).
 */
class TogglePixelTest {

    /** Fields per browser: at most twice this many screenshots in one session. */
    private static final int CHUNK = 11;

    private static final List<String> MISMATCHES = new ArrayList<>();
    private static final List<String> FREE_MISMATCHES = new ArrayList<>();
    private static int freeCompared;
    private static final Set<String> DISTINCT_LIGHT = new HashSet<>();
    private static final Set<String> DISTINCT_DARK = new HashSet<>();
    private static int compared;
    private static String controlOriginal;
    private static String controlBrokenTrack;
    private static String controlBrokenThumb;
    private static String controlFlatThumb;

    private static String shot(DevToolsSession cdp, String fieldId) throws Exception {
        cdp.evaluate("(function(){document.querySelector('[data-field=\"" + fieldId + "\"]').scrollIntoView({block:\"center\"});return \"ok\"})()");
        String box = cdp.evaluate("(function(){var b=document.querySelector('[data-field=\"" + fieldId + "\"]').getBoundingClientRect();return JSON.stringify({x:b.x+scrollX-6,y:b.y+scrollY-6,width:b.width+12,height:b.height+12})})()");
        String response = cdp.call("Page.captureScreenshot", "{\"clip\":" + box.replace("}", ",\"scale\":1}") + "}");
        int at = response.indexOf("\"data\":\"");
        assertTrue(at > 0, response);
        return response.substring(at + 8, response.indexOf('"', at + 8));
    }

    private static String capture(DevToolsSession cdp, String url, List<ToggleParityTest.Field> fields, java.util.function.BiConsumer<ToggleParityTest.Field, String> sink) throws Exception {
        cdp.navigate(url);
        StringBuilder all = new StringBuilder();
        for (ToggleParityTest.Field f : fields) {
            List<String> pseudo = f.spec().pseudo();
            List<Integer> nodes = pseudo.isEmpty() ? List.of() : cdp.nodes(ToggleBrowser.target(f.spec(), f.id()));
            if (!pseudo.isEmpty()) {
                cdp.force(nodes, pseudo);
            }
            sink.accept(f, shot(cdp, f.id()));
            if (!pseudo.isEmpty()) {
                cdp.force(nodes, List.of());
            }
        }
        return all.toString();
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("toggle-pixels");
        List<ToggleParityTest.Field> fields = ToggleParityTest.fields();
        String[] bodies = ToggleParityTest.bodies(fields);
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            for (ParityConfigs.Config config : ParityConfigs.all()) {
                String legacyUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("toggle-oracle.css")), config, "", bodies[1], ""));
                String migratedUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.APP), config, "", bodies[0], ""));
                java.util.Map<String, String> legacy = new java.util.LinkedHashMap<>();
                java.util.Map<String, String> migrated = new java.util.LinkedHashMap<>();
                // In chunks, each with its own browser holding BOTH renderings: the comparison is always between two pictures taken by the same browser process, and no browser
                // is asked for more than a few dozen screenshots (a browser asked for sixty or more in one session did not shut down cleanly on this machine).
                for (int from = 0; from < fields.size(); from += CHUNK) {
                    List<ToggleParityTest.Field> chunk = fields.subList(from, Math.min(fields.size(), from + CHUNK));
                    try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                        capture(cdp, legacyUrl, chunk, (f, png) -> legacy.put(f.id(), png));
                        capture(cdp, migratedUrl, chunk, (f, png) -> migrated.put(f.id(), png));
                    }
                }
                for (ToggleParityTest.Field f : fields) {
                    compared++;
                    if (!legacy.get(f.id()).equals(migrated.get(f.id()))) {
                        MISMATCHES.add(config.id() + " " + f.id() + " (" + f.spec().key() + ")");
                    }
                    if (config.id().equals("light/none")) {
                        DISTINCT_LIGHT.add(legacy.get(f.id()));
                    }
                    if (config.id().equals("dark/none")) {
                        DISTINCT_DARK.add(legacy.get(f.id()));
                    }
                }
            }
            // Without Tailwind: the scoped legacy rendering against the migrated one with Ashlar's stylesheet alone, both with the same page environment (the showcase's own dark
            // colour scheme included). This is what shows that the properties the Tailwind-free computed comparison leaves out (the input's inherited text colour, its zero-width border
            // colours) paint nothing.
            String environment = ParityFrames.environment(session.generatedDir());
            for (String id : List.of("light/none", "dark/none", "light/ledger", "dark/axiom", "dark/foundry")) {
                ParityConfigs.Config config = ParityConfigs.get(id);
                String legacyUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("toggle-legacy-scoped.css")), config, "", bodies[1], "").replace("</head>", "<style>" + environment + "</style></head>"));
                String migratedUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.ASHLAR), config, "", bodies[0], "").replace("</head>", "<style>" + environment + "</style></head>"));
                java.util.Map<String, String> legacy = new java.util.LinkedHashMap<>();
                java.util.Map<String, String> migrated = new java.util.LinkedHashMap<>();
                for (int from = 0; from < fields.size(); from += CHUNK) {
                    List<ToggleParityTest.Field> chunk = fields.subList(from, Math.min(fields.size(), from + CHUNK));
                    try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                        capture(cdp, legacyUrl, chunk, (f, png) -> legacy.put(f.id(), png));
                        capture(cdp, migratedUrl, chunk, (f, png) -> migrated.put(f.id(), png));
                    }
                }
                for (ToggleParityTest.Field f : fields) {
                    freeCompared++;
                    if (!legacy.get(f.id()).equals(migrated.get(f.id()))) {
                        FREE_MISMATCHES.add(id + " " + f.id() + " (" + f.spec().key() + ")");
                    }
                }
            }
            // Controls: the comparison can fail. A checked toggle, then the same toggle with one thing wrong.
            ParityConfigs.Config light = ParityConfigs.get("light/none");
            String checked = ToggleBrowser.marked(ToggleBrowser.toggle(ToggleBrowser.o("id", "tgx", "checked", true)));
            try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                String page = TextEntryBrowser.page(List.of(ParityFrames.APP), light, "", checked, "");
                cdp.navigate(server.registerRaw(page));
                controlOriginal = shot(cdp, "tgx");
                cdp.navigate(server.registerRaw(page.replace("</head>", "<style>.ash-toggle__track{background-color:rgb(220,38,38) !important}</style></head>")));
                controlBrokenTrack = shot(cdp, "tgx");
                cdp.navigate(server.registerRaw(page.replace("</head>", "<style>.ash-toggle__thumb{translate:0 !important}</style></head>")));
                controlBrokenThumb = shot(cdp, "tgx");
                cdp.navigate(server.registerRaw(page.replace("</head>", "<style>html{--ash-toggle-thumb-shadow:0 0 #0000}</style></head>")));
                controlFlatThumb = shot(cdp, "tgx");
            }
        }
    }

    @Test
    void everyFieldIsTheSamePictureInTheLegacyAndTheMigratedRenderingInAll22Configurations() {
        assertEquals(22 * ToggleBrowser.CASES.size(), compared);
        assertEquals(List.of(), MISMATCHES.stream().limit(40).toList(), MISMATCHES.size() + " of " + compared + " fields differ");
    }

    @Test
    void withoutTailwindEveryFieldIsStillTheSamePictureAsTheLegacyFieldWithoutPageWideTailwind() {
        assertEquals(5 * ToggleBrowser.CASES.size(), freeCompared);
        assertEquals(List.of(), FREE_MISMATCHES.stream().limit(40).toList(), FREE_MISMATCHES.size() + " of " + freeCompared + " fields differ");
    }

    @Test
    void theComparisonIsNotVacuousTheStatesLookDifferentFromEachOther() {
        assertTrue(DISTINCT_LIGHT.size() >= 20, "distinct pictures in light: " + DISTINCT_LIGHT.size());
        assertTrue(DISTINCT_DARK.size() >= 20, "distinct pictures in dark: " + DISTINCT_DARK.size());
    }

    @Test
    void aWrongTrackColourOrAWrongThumbPositionWouldBeCaught() {
        assertNotEquals(controlOriginal, controlBrokenTrack, "the picture changes when the track colour does");
        assertNotEquals(controlOriginal, controlBrokenThumb, "the picture changes when the thumb is not moved");
    }

    @Test
    void theThumbsShadowIsVisibleInThePictureSoEverySkinsFlatteningIsReallyCompared() {
        assertNotEquals(controlOriginal, controlFlatThumb, "a flat thumb is a different picture from one with its shadow");
    }
}
