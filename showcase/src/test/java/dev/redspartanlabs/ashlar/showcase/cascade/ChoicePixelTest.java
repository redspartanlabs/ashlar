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
 * The native control's painting, compared in PIXELS, in the same browser. A native-appearance checkbox or radio is drawn by the browser, not by CSS, so computed styles alone cannot
 * say that two renderings look the same: this takes a screenshot of every Checkbox and Radio Group field in the legacy rendering and in the migrated rendering, in one browser
 * process, in all 22 configurations, and requires identical bytes. This is what proves the choices the computed-style parity cannot: the migrated control
 * declares no border and no radius (a native-appearance control ignores both) yet is the same picture in every state, and it DOES declare the background colour, because the first
 * version of the migration left it out and this suite caught it: with the focus ring drawn, the square corners around the native mark show the background (slate-800 in dark), and
 * leaving it out changed those pixels in dark mode.
 *
 * <p>No golden image is stored: native painting differs by operating system and browser version, so only a rendering of the same page in the same browser is a fair reference. The
 * fields are captured with forced states (focus-visible, plain focus, hover, active) exactly as the parity suite forces them, and the page carries the showcase's own dark
 * {@code color-scheme}, which changes how the browser draws the mark (a control drawn with and without it is shown to differ).
 */
class ChoicePixelTest {

    /** Fields per browser: at most twice this many screenshots in one session. */
    private static final int CHUNK = 11;

    private static final List<String> MISMATCHES = new ArrayList<>();
    private static final List<String> FREE_MISMATCHES = new ArrayList<>();
    private static int freeCompared;
    private static final Set<String> DISTINCT_LIGHT = new HashSet<>();
    private static final Set<String> DISTINCT_DARK = new HashSet<>();
    private static int compared;
    private static String controlBrokenAccent;
    private static String controlOriginal;
    private static String schemeOn;
    private static String schemeOff;

    private static String shot(DevToolsSession cdp, String fieldId) throws Exception {
        cdp.evaluate("(function(){document.querySelector('[data-field=\"" + fieldId + "\"]').scrollIntoView({block:\"center\"});return \"ok\"})()");
        String box = cdp.evaluate("(function(){var b=document.querySelector('[data-field=\"" + fieldId + "\"]').getBoundingClientRect();return JSON.stringify({x:b.x+scrollX-6,y:b.y+scrollY-6,width:b.width+12,height:b.height+12})})()");
        String response = cdp.call("Page.captureScreenshot", "{\"clip\":" + box.replace("}", ",\"scale\":1}") + "}");
        int at = response.indexOf("\"data\":\"");
        assertTrue(at > 0, response);
        return response.substring(at + 8, response.indexOf('"', at + 8));
    }

    private static String capture(DevToolsSession cdp, String url, List<ChoiceParityTest.Field> fields, java.util.function.BiConsumer<ChoiceParityTest.Field, String> sink) throws Exception {
        cdp.navigate(url);
        StringBuilder all = new StringBuilder();
        for (ChoiceParityTest.Field f : fields) {
            List<String> pseudo = f.spec().pseudo();
            List<Integer> nodes = pseudo.isEmpty() ? List.of() : cdp.nodes(ChoiceBrowser.target(f.spec(), f.id()));
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
        ParitySession session = ParitySession.open("choice-pixels");
        List<ChoiceParityTest.Field> fields = ChoiceParityTest.fields();
        String[] bodies = ChoiceParityTest.bodies(fields);
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            for (ParityConfigs.Config config : ParityConfigs.all()) {
                String legacyUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("choice-oracle.css")), config, "", bodies[1], ChoiceBrowser.INDETERMINATE));
                String migratedUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.APP), config, "", bodies[0], ChoiceBrowser.INDETERMINATE));
                java.util.Map<String, String> legacy = new java.util.LinkedHashMap<>();
                java.util.Map<String, String> migrated = new java.util.LinkedHashMap<>();
                // In chunks, each with its own browser holding BOTH renderings: the comparison is always between two pictures taken by the same browser process, and no browser
                // is asked for more than a few dozen screenshots (a browser asked for sixty or more in one session did not shut down cleanly on this machine).
                for (int from = 0; from < fields.size(); from += CHUNK) {
                    List<ChoiceParityTest.Field> chunk = fields.subList(from, Math.min(fields.size(), from + CHUNK));
                    try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                        capture(cdp, legacyUrl, chunk, (f, png) -> legacy.put(f.id(), png));
                        capture(cdp, migratedUrl, chunk, (f, png) -> migrated.put(f.id(), png));
                    }
                }
                for (ChoiceParityTest.Field f : fields) {
                    compared++;
                    if (!legacy.get(f.id()).equals(migrated.get(f.id()))) {
                        MISMATCHES.add(config.id() + " " + f.id() + " (" + f.spec().kind() + " " + f.spec().key() + ")");
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
                String legacyUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("choice-legacy-scoped.css")), config, "", bodies[1], ChoiceBrowser.INDETERMINATE).replace("</head>", "<style>" + environment + "</style></head>"));
                String migratedUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.ASHLAR), config, "", bodies[0], ChoiceBrowser.INDETERMINATE).replace("</head>", "<style>" + environment + "</style></head>"));
                java.util.Map<String, String> legacy = new java.util.LinkedHashMap<>();
                java.util.Map<String, String> migrated = new java.util.LinkedHashMap<>();
                for (int from = 0; from < fields.size(); from += CHUNK) {
                    List<ChoiceParityTest.Field> chunk = fields.subList(from, Math.min(fields.size(), from + CHUNK));
                    try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                        capture(cdp, legacyUrl, chunk, (f, png) -> legacy.put(f.id(), png));
                        capture(cdp, migratedUrl, chunk, (f, png) -> migrated.put(f.id(), png));
                    }
                }
                for (ChoiceParityTest.Field f : fields) {
                    freeCompared++;
                    if (!legacy.get(f.id()).equals(migrated.get(f.id()))) {
                        FREE_MISMATCHES.add(id + " " + f.id() + " (" + f.spec().kind() + " " + f.spec().key() + ")");
                    }
                }
            }
            // Controls: the comparison can fail, and the colour scheme matters.
            ParityConfigs.Config light = ParityConfigs.get("light/none");
            ParityConfigs.Config dark = ParityConfigs.get("dark/none");
            String checked = ChoiceBrowser.marked(ChoiceBrowser.checkbox(ChoiceBrowser.o("id", "cbx", "checked", true)));
            try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                String page = TextEntryBrowser.page(List.of(ParityFrames.APP), light, "", checked, "");
                cdp.navigate(server.registerRaw(page));
                controlOriginal = shot(cdp, "cbx");
                cdp.navigate(server.registerRaw(page.replace("</head>", "<style>.ash-choice__input{accent-color:rgb(220,38,38) !important}</style></head>")));
                controlBrokenAccent = shot(cdp, "cbx");
                String darkPage = TextEntryBrowser.page(List.of(ParityFrames.APP), dark, "", checked, "");
                cdp.navigate(server.registerRaw(darkPage));
                schemeOn = shot(cdp, "cbx");
                cdp.navigate(server.registerRaw(darkPage.replace("</head>", "<style>:root{color-scheme:light !important}</style></head>")));
                schemeOff = shot(cdp, "cbx");
            }
        }
    }

    @Test
    void everyFieldIsTheSamePictureInTheLegacyAndTheMigratedRenderingInAll22Configurations() {
        assertEquals(22 * ChoiceBrowser.CASES.size(), compared);
        assertEquals(List.of(), MISMATCHES.stream().limit(40).toList(), MISMATCHES.size() + " of " + compared + " fields differ");
    }

    @Test
    void withoutTailwindEveryFieldIsStillTheSamePictureAsTheLegacyFieldWithoutPageWideTailwind() {
        assertEquals(5 * ChoiceBrowser.CASES.size(), freeCompared);
        assertEquals(List.of(), FREE_MISMATCHES.stream().limit(40).toList(), FREE_MISMATCHES.size() + " of " + freeCompared + " fields differ");
    }

    @Test
    void theComparisonIsNotVacuousTheStatesLookDifferentFromEachOther() {
        assertTrue(DISTINCT_LIGHT.size() >= 20, "distinct pictures in light: " + DISTINCT_LIGHT.size());
        assertTrue(DISTINCT_DARK.size() >= 20, "distinct pictures in dark: " + DISTINCT_DARK.size());
    }

    @Test
    void aWrongAccentColourWouldBeCaught() {
        assertNotEquals(controlOriginal, controlBrokenAccent, "the picture changes when the accent colour does");
    }

    @Test
    void theColourSchemeChangesHowTheBrowserDrawsTheControlSoBothSidesMustShareIt() {
        assertNotEquals(schemeOn, schemeOff, "a dark page draws the native mark differently with the dark colour scheme");
    }
}
