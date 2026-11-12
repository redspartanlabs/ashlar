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
 * The Table family compared in PIXELS, in the same browser. Computed styles say that two renderings are specified the same; a screenshot of every table in the legacy rendering and in the
 * migrated one, taken by one browser process, says they LOOK the same, including what no property list can name (collapsed row borders where a hidden row follows, the native checkbox and
 * its tint, the sort glyph, the focus rings, the wrapping of a long note). It runs in all 22 configurations with Tailwind present and in five with Tailwind removed (the legacy rendering with
 * only the utilities and preflight rules the family used, against Ashlar's stylesheet alone), and requires identical bytes.
 *
 * <p>Every case is a page of its own: the table, alone, never scrolled. That is a measured necessity, not a convenience. Put thirty-four tables on one tall page and Chrome rasterises the
 * anti-aliased corner of a forced focus ring one pixel differently from one capture to the next (the same two shades, in the same order, in the legacy and in the migrated rendering
 * alike, so no styling difference); a one-table page was repeatable in every one of 32 series tried. The suite does not assume that: every picture is captured twice and the two must be
 * identical, or the run fails as an infrastructure failure rather than as a parity result.
 *
 * <p>No golden image is stored: native painting differs by operating system and browser version, so only a rendering of the same page in the same browser is a fair reference. The cases
 * are captured with the same forced states as {@link TableParityTest}. Controls show that the comparison can fail: a wrong accent colour changes the picture, and so does the dark
 * {@code color-scheme}.
 */
class TablePixelTest {

    /** Cases per browser: at most twice this many pages in one session (a browser asked for sixty or more screenshots in one session has not shut down cleanly here). */
    private static final int CHUNK = 12;
    private static final List<String> TAILWIND_FREE = List.of("light/none", "dark/none", "light/ledger", "dark/axiom", "dark/foundry");

    private static final List<PixelDiagnostics.Candidate> CANDIDATES = new ArrayList<>();
    private static final List<String> MISMATCHES = new ArrayList<>();
    private static final List<String> FREE_MISMATCHES = new ArrayList<>();
    private static final Set<String> DISTINCT_LIGHT = new HashSet<>();
    private static final Set<String> DISTINCT_DARK = new HashSet<>();
    private static int compared;
    private static int freeCompared;
    private static String controlOriginal;
    private static String controlBrokenAccent;
    private static String schemeOn;
    private static String schemeOff;

    private static String shot(DevToolsSession cdp, String fieldId) throws Exception {
        String box = cdp.evaluate("(function(){var b=document.querySelector('[data-field=\"" + fieldId + "\"]').getBoundingClientRect();return JSON.stringify({x:b.x+scrollX-6,y:b.y+scrollY-6,width:b.width+12,height:b.height+12})})()");
        String response = cdp.call("Page.captureScreenshot", "{\"clip\":" + box.replace("}", ",\"scale\":1}") + "}");
        int at = response.indexOf("\"data\":\"");
        assertTrue(at > 0, response);
        return response.substring(at + 8, response.indexOf('"', at + 8));
    }

    /** The picture of one table page, with its state forced, captured twice: the two must be identical (see the class comment). */
    private static String picture(DevToolsSession cdp, String url, TableParityTest.Field f) throws Exception {
        cdp.navigate(url);
        List<String> pseudo = f.spec().pseudo();
        List<Integer> nodes = pseudo.isEmpty() ? List.of() : cdp.nodes(TableBrowser.target(f.spec(), f.id()));
        if (!pseudo.isEmpty()) {
            assertTrue(!nodes.isEmpty(), f.spec().key() + ": the target " + f.spec().target() + " was not found");
            cdp.force(nodes, pseudo);
        }
        String first = shot(cdp, f.id());
        String second = shot(cdp, f.id());
        if (!pseudo.isEmpty()) {
            cdp.force(nodes, List.of());
        }
        assertEquals(first, second, f.id() + " (" + f.spec().key() + "): one page captured twice gave two pictures - infrastructure failure, not a parity result");
        return first;
    }

    private static String singlePage(List<String> sheets, ParityConfigs.Config config, String body, String environment) {
        String page = TextEntryBrowser.page(sheets, config, "", body, TableBrowser.INDETERMINATE);
        return environment == null ? page : page.replace("</head>", "<style>" + environment + "</style></head>");
    }

    /** Both renderings of every case, chunk by chunk, each chunk in a browser of its own that captures BOTH renderings, so a comparison is always between two pictures of one process. */
    private static void compare(ParitySession session, CascadeFixtureServer server, List<TableParityTest.Field> fields, List<String> legacySheets, List<String> migratedSheets,
                                ParityConfigs.Config config, String environment, String where, List<String> mismatches, Set<String> distinct, boolean count) throws Exception {
        for (int from = 0; from < fields.size(); from += CHUNK) {
            List<TableParityTest.Field> chunk = fields.subList(from, Math.min(fields.size(), from + CHUNK));
            try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                for (TableParityTest.Field f : chunk) {
                    String migratedBody = f.markup();
                    String legacyBody = new IconLegacyMarkup().rewrite(new TableLegacyMarkup().rewrite(migratedBody));
                    String legacyPage = singlePage(legacySheets, config, legacyBody, environment);
                    String migratedPage = singlePage(migratedSheets, config, migratedBody, environment);
                    String legacy = picture(cdp, server.registerRaw(legacyPage), f);
                    String migrated = picture(cdp, server.registerRaw(migratedPage), f);
                    if (count) {
                        compared++;
                    } else {
                        freeCompared++;
                    }
                    if (!legacy.equals(migrated)) {
                        mismatches.add(where + " " + f.id() + " (" + f.spec().key() + ")");
                    }
                    // DIAGNOSTIC ONLY (PR #31 investigation): remember the pair so it can be explained after every comparison is done. Nothing here changes a capture or a verdict.
                    if (count && PixelDiagnostics.wanted(config.id(), f.spec().key(), !legacy.equals(migrated))) {
                        CANDIDATES.add(new PixelDiagnostics.Candidate(config.id(), f.id(), f.spec().key(), legacy, migrated, legacyPage, migratedPage, f));
                    }
                    if (distinct != null) {
                        distinct.add(legacy);
                    }
                }
            }
        }
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("table-pixels");
        List<TableParityTest.Field> fields = TableParityTest.fields();
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            for (ParityConfigs.Config config : ParityConfigs.all()) {
                boolean plain = config.id().equals("light/none") || config.id().equals("dark/none");
                compare(session, server, fields, List.of(ParityFrames.generated("table-oracle.css")), List.of(ParityFrames.APP), config, null, config.id(), MISMATCHES,
                        plain ? (config.theme().equals("light") ? DISTINCT_LIGHT : DISTINCT_DARK) : null, true);
            }
            // Without Tailwind: the scoped legacy rendering against the migrated one with Ashlar's stylesheet alone, both in the same page environment (the showcase's own dark colour scheme included).
            String environment = ParityFrames.environment(session.generatedDir());
            for (String id : TAILWIND_FREE) {
                compare(session, server, fields, List.of(ParityFrames.generated("table-legacy-scoped.css")), List.of(ParityFrames.ASHLAR), ParityConfigs.get(id), environment, id + " (Tailwind removed)",
                        FREE_MISMATCHES, null, false);
            }
            // Controls: the comparison can fail, and the colour scheme matters.
            ParityConfigs.Config light = ParityConfigs.get("light/none");
            ParityConfigs.Config dark = ParityConfigs.get("dark/none");
            String field = "<div data-field=\"c1\">" + TableBrowser.marked(TableBrowser.table(TableBrowser.o("selectable", true, "selected", List.of(0, 1)))) + "</div>";
            try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                String page = singlePage(List.of(ParityFrames.APP), light, field, null);
                cdp.navigate(server.registerRaw(page));
                controlOriginal = shot(cdp, "c1");
                cdp.navigate(server.registerRaw(page.replace("</head>", "<style>.ash-table__checkbox{accent-color:rgb(220,38,38) !important}</style></head>")));
                controlBrokenAccent = shot(cdp, "c1");
                String darkPage = singlePage(List.of(ParityFrames.APP), dark, field, null);
                cdp.navigate(server.registerRaw(darkPage));
                schemeOn = shot(cdp, "c1");
                cdp.navigate(server.registerRaw(darkPage.replace("</head>", "<style>:root{color-scheme:light !important}</style></head>")));
                schemeOff = shot(cdp, "c1");
            }
            diagnose(session, server);
        }
    }

    // ---------------------------------------------------------------- DIAGNOSTIC ONLY (PR #31 investigation)

    /** What the browser says about the focused target of a case, read while its state is forced: geometry relative to the picture, computed style, fonts, pixel ratio. */
    private static String probe(DevToolsSession cdp, String url, TableParityTest.Field f) throws Exception {
        cdp.navigate(url);
        String selector = TableBrowser.target(f.spec(), f.id());
        List<Integer> nodes = cdp.nodes(selector);
        cdp.force(nodes, f.spec().pseudo());
        String fonts = "n/a";
        try {
            cdp.call("DOM.enable", "{}");
            cdp.call("CSS.enable", "{}");
            if (!nodes.isEmpty()) {
                fonts = cdp.call("CSS.getPlatformFontsForNode", "{\"nodeId\":" + nodes.get(0) + "}");
            }
        } catch (Throwable t) {
            fonts = "error " + t.getMessage();
        }
        String info = cdp.evaluate("(function(){var f=document.querySelector('[data-field=\"" + f.id() + "\"]').getBoundingClientRect();var t=document.querySelector(" + DevToolsSession.quote(selector)
                + ");var r=t.getBoundingClientRect();var c=getComputedStyle(t);return JSON.stringify({box:[r.x-f.x+6,r.y-f.y+6,r.width,r.height],field:[f.x,f.y,f.width,f.height],dpr:devicePixelRatio,"
                + "inner:[innerWidth,innerHeight],boxShadow:c.boxShadow,borderRadius:c.borderTopLeftRadius+' '+c.borderTopRightRadius+' '+c.borderBottomRightRadius+' '+c.borderBottomLeftRadius,"
                + "outline:c.outlineStyle+' '+c.outlineWidth+' '+c.outlineColor,font:c.fontFamily+' '+c.fontSize+' '+c.fontWeight,focused:t.matches(':focus-visible'),display:c.display,"
                + "matchesFocus:t.matches(':focus')})})()");
        cdp.force(nodes, List.of());
        return info.substring(0, info.length() - 1) + ",\"platformFonts\":" + PixelDiagnostics.json(fonts) + "}";
    }

    /** Explains every pair collected for the target configurations: statistics, ring profile, images, and whether fresh captures of each side repeat. Never throws. */
    private static void diagnose(ParitySession session, CascadeFixtureServer server) {
        try {
            PixelDiagnostics.selfCheck();
            PixelDiagnostics.line("environment os=" + System.getProperty("os.name") + " " + System.getProperty("os.version") + " java=" + System.getProperty("java.version") + " ci=" + (System.getenv("CI") != null)
                    + " force=" + PixelDiagnostics.FORCE + " candidates=" + CANDIDATES.size() + " mismatchesInTotal=" + MISMATCHES.size());
            if (System.getProperty("os.name", "").toLowerCase().contains("linux")) {
                for (String family : List.of("Arial", "Helvetica", "sans-serif", "Arial:bold")) {
                    try {
                        Process p = new ProcessBuilder("fc-match", family).redirectErrorStream(true).start();
                        p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                        PixelDiagnostics.line("environment fc-match " + family + " = " + new String(p.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim());
                    } catch (Throwable t) {
                        PixelDiagnostics.line("environment fc-match " + family + " unavailable: " + t);
                    }
                }
            }
            if (CANDIDATES.isEmpty()) {
                PixelDiagnostics.line("none: every target-configuration picture pair was identical");
                return;
            }
            try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                PixelDiagnostics.line("environment browser=" + cdp.call("Browser.getVersion", "{}"));
                for (PixelDiagnostics.Candidate c : CANDIDATES) {
                    String note = "";
                    String legacyInfo = "";
                    String migratedInfo = "";
                    boolean legacyRepeats = true;
                    boolean migratedRepeats = true;
                    int recaptures = 3;
                    try {
                        String legacyUrl = server.registerRaw(c.legacyPage());
                        String migratedUrl = server.registerRaw(c.migratedPage());
                        legacyInfo = probe(cdp, legacyUrl, c.field());
                        migratedInfo = probe(cdp, migratedUrl, c.field());
                        for (int i = 0; i < recaptures; i++) {
                            legacyRepeats &= c.legacy().equals(picture(cdp, legacyUrl, c.field()));
                            migratedRepeats &= c.migrated().equals(picture(cdp, migratedUrl, c.field()));
                        }
                    } catch (Throwable t) {
                        note = "probe failed: " + t;
                    }
                    PixelDiagnostics.report(c, new PixelDiagnostics.Probe(legacyInfo, migratedInfo, legacyRepeats, migratedRepeats, recaptures, note));
                }
            }
        } catch (Throwable t) {
            PixelDiagnostics.line("diagnostics failed: " + t);
        }
    }

    @Test
    void everyTableIsTheSamePictureInTheLegacyAndTheMigratedRenderingInAll22Configurations() {
        assertEquals(22 * TableBrowser.CASES.size(), compared);
        assertEquals(List.of(), MISMATCHES.stream().limit(40).toList(), MISMATCHES.size() + " of " + compared + " tables differ");
    }

    @Test
    void withoutTailwindEveryTableIsStillTheSamePictureAsTheLegacyTableWithoutPageWideTailwind() {
        assertEquals(TAILWIND_FREE.size() * TableBrowser.CASES.size(), freeCompared);
        assertEquals(List.of(), FREE_MISMATCHES.stream().limit(40).toList(), FREE_MISMATCHES.size() + " of " + freeCompared + " tables differ");
    }

    @Test
    void theComparisonIsNotVacuousTheStatesLookDifferentFromEachOther() {
        assertTrue(DISTINCT_LIGHT.size() >= 28, "distinct pictures in light: " + DISTINCT_LIGHT.size() + " of " + TableBrowser.CASES.size());
        assertTrue(DISTINCT_DARK.size() >= 28, "distinct pictures in dark: " + DISTINCT_DARK.size() + " of " + TableBrowser.CASES.size());
    }

    @Test
    void aWrongAccentColourWouldBeCaught() {
        assertNotEquals(controlOriginal, controlBrokenAccent, "the picture changes when the checkbox tint does");
    }

    @Test
    void theColourSchemeChangesHowTheBrowserDrawsTheCheckboxSoBothSidesMustShareIt() {
        assertNotEquals(schemeOn, schemeOff, "a dark page draws the native mark differently with the dark colour scheme");
    }
}
