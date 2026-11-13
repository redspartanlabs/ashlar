package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Table family compared in PIXELS, in the same browser. Computed styles say that two renderings are specified the same; a screenshot of every table in the legacy rendering and in the
 * migrated one, taken by one browser process, says they LOOK the same, including what no property list can name (collapsed row borders where a hidden row follows, the native checkbox and
 * its tint, the sort glyph, the focus rings, the wrapping of a long note). It runs in all 22 configurations with Tailwind present and in five with Tailwind removed (the legacy rendering with
 * only the utilities and preflight rules the family used, against Ashlar's stylesheet alone), and requires identical bytes, with three named exceptions (see {@link PixelPolicy}).
 *
 * <p>Every case is a page of its own: the table, alone, never scrolled. That is a measured necessity, not a convenience. Put thirty-four tables on one tall page and Chrome rasterises the
 * anti-aliased corner of a forced focus ring one pixel differently from one capture to the next (the same two shades, in the same order, in the legacy and in the migrated rendering
 * alike, so no styling difference); a one-table page was repeatable in every one of 32 series tried. The suite does not assume that: every picture is captured twice and the two must be
 * identical, or the run fails as an infrastructure failure rather than as a parity result.
 *
 * <p>One-table pages did not remove the variation entirely, only made it rare, and only in three cases. Repeated captures on the CI runner (Linux, Chrome 156) showed one anti-aliased edge
 * pixel of the focus-visible sort button's ring, and of the first letter's left edge in the two sort-button hover cases, coming out as one of two neighboring shades from one capture or one
 * browser to the next, in the legacy rendering as often as in the migrated one, at the same pixel and in the same two shades, with computed styles and layout identical. Those three cases
 * therefore use {@link PixelPolicy}: exact first, then at most two edge pixels, within a band computed from the browser's geometry, within a measured channel difference. Controls below prove
 * that policy still rejects a ring that is missing, thinner, thicker, displaced, weaker or differently rounded, and a hovered label that is recolored, italic, shifted, faded or on a tinted
 * background, and a change inside the element or outside the band. Every pair that is tolerated is listed in the test output.
 *
 * <p>No golden image is stored: native painting differs by operating system and browser version, so only a rendering of the same page in the same browser is a fair reference. The cases
 * are captured with the same forced states as {@link TableParityTest}. Controls show that the comparison can fail: a wrong accent colour changes the picture, and so does the dark
 * {@code color-scheme}.
 */
class TablePixelTest {

    /** Cases per browser: at most twice this many pages in one session (a browser asked for sixty or more screenshots in one session has not shut down cleanly here). */
    private static final int CHUNK = 12;
    private static final List<String> TAILWIND_FREE = List.of("light/none", "dark/none", "light/ledger", "dark/axiom", "dark/foundry");
    /** Configurations that failed in CI before the policy existed, for the controls that the policy still rejects broken renderings. */
    private static final List<String> CONTROL_CONFIGS = List.of("light/ledger", "dark/veridian");

    private static final List<String> MISMATCHES = new ArrayList<>();
    private static final List<String> FREE_MISMATCHES = new ArrayList<>();
    private static final List<String> TOLERATED = new ArrayList<>();
    private static final Set<String> DISTINCT_LIGHT = new HashSet<>();
    private static final Set<String> DISTINCT_DARK = new HashSet<>();
    private static final List<String> CONTROL_FAILURES = new ArrayList<>();
    private static int controlsRun;
    private static int compared;
    private static int freeCompared;
    private static String controlOriginal;
    private static String controlBrokenAccent;
    private static String schemeOn;
    private static String schemeOff;
    /** Every pair that was not byte-identical, kept for the diagnostics that are printed once all pictures have been taken (see {@link PixelDiagnostics}). They decide nothing. */
    private static final List<Seen> SEEN = new ArrayList<>();
    private static long started;

    /** A picture, with the geometry of the forced element in it when the case forces a state (null otherwise). */
    private record Picture(String png, PixelPolicy.Geometry geometry) {
    }

    /** What a comparison saw, kept as it is: references to pictures already taken, and the verdict already reached. */
    private record Seen(String label, ParityConfigs.Config config, TableParityTest.Field field, String where, String legacyPng, String migratedPng, PixelPolicy.Geometry geometry,
                        PixelPolicy.Verdict verdict, long nanos) {
    }

    private static String shot(DevToolsSession cdp, String fieldId) throws Exception {
        String box = cdp.evaluate("(function(){var b=document.querySelector('[data-field=\"" + fieldId + "\"]').getBoundingClientRect();return JSON.stringify({x:b.x+scrollX-6,y:b.y+scrollY-6,width:b.width+12,height:b.height+12})})()");
        String response = cdp.call("Page.captureScreenshot", "{\"clip\":" + box.replace("}", ",\"scale\":1}") + "}");
        int at = response.indexOf("\"data\":\"");
        assertTrue(at > 0, response);
        return response.substring(at + 8, response.indexOf('"', at + 8));
    }

    /**
     * What the browser says about the forced element: its border box in the picture (the picture is the table's box with 6 px around it) and how far a focus ring reaches beyond that box, from
     * the computed outline (when drawn) and the computed box shadows (offset, blur and spread). {@code "x y width height reach"}.
     */
    private static String geometryScript(String fieldId, String selector) {
        return "(function(){var f=document.querySelector('[data-field=\"" + fieldId + "\"]').getBoundingClientRect();var t=document.querySelector(" + DevToolsSession.quote(selector)
                + ");var r=t.getBoundingClientRect();var c=getComputedStyle(t);var reach=c.outlineStyle==='none'?0:Math.max(0,parseFloat(c.outlineWidth)+parseFloat(c.outlineOffset));"
                + "(c.boxShadow.match(/-?[\\d.]+px -?[\\d.]+px -?[\\d.]+px -?[\\d.]+px/g)||[]).forEach(function(s){var n=s.split(' ').map(parseFloat);reach=Math.max(reach,n[3]+n[2]+Math.max(Math.abs(n[0]),Math.abs(n[1])))});"
                + "return [r.x-f.x+6,r.y-f.y+6,r.width,r.height,reach].join(' ')})()";
    }

    /** The picture of one table page, with its state forced, captured twice: the two must be identical (see the class comment). */
    private static Picture picture(DevToolsSession cdp, String url, TableParityTest.Field f) throws Exception {
        cdp.navigate(url);
        List<String> pseudo = f.spec().pseudo();
        List<Integer> nodes = pseudo.isEmpty() ? List.of() : cdp.nodes(TableBrowser.target(f.spec(), f.id()));
        PixelPolicy.Geometry geometry = null;
        if (!pseudo.isEmpty()) {
            assertTrue(!nodes.isEmpty(), f.spec().key() + ": the target " + f.spec().target() + " was not found");
            cdp.force(nodes, pseudo);
            geometry = PixelPolicy.Geometry.parse(cdp.evaluate(geometryScript(f.id(), TableBrowser.target(f.spec(), f.id()))));
        }
        String first = shot(cdp, f.id());
        String second = shot(cdp, f.id());
        if (!pseudo.isEmpty()) {
            cdp.force(nodes, List.of());
        }
        assertEquals(first, second, f.id() + " (" + f.spec().key() + "): one page captured twice gave two pictures - infrastructure failure, not a parity result");
        return new Picture(first, geometry);
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
                    Picture legacy = picture(cdp, server.registerRaw(singlePage(legacySheets, config, legacyBody, environment)), f);
                    Picture migrated = picture(cdp, server.registerRaw(singlePage(migratedSheets, config, migratedBody, environment)), f);
                    if (count) {
                        compared++;
                    } else {
                        freeCompared++;
                    }
                    // Exact first; a tolerance only for the named cases, within the band the browser computed for the legacy (reference) rendering.
                    PixelPolicy.Verdict verdict = PixelPolicy.compare(legacy.png(), migrated.png(), PixelPolicy.forCase(f.spec().key()), legacy.geometry());
                    String label = where + " " + f.id() + " (" + f.spec().key() + ")";
                    if (!verdict.accepted()) {
                        mismatches.add(label + " " + verdict.describe());
                    } else if (!verdict.exact()) {
                        TOLERATED.add(label + " " + verdict.describe());
                    }
                    if (!verdict.exact()) {
                        SEEN.add(new Seen(label, config, f, where, legacy.png(), migrated.png(), legacy.geometry(), verdict, System.nanoTime()));
                    }
                    if (distinct != null) {
                        distinct.add(legacy.png());
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- controls: the policy must still reject broken renderings

    private static final String RING = "[data-table-sort]:focus-visible";
    private static final String HOVERED = "[data-table-sort]:hover";

    /** Ways to break the migrated focus ring (the real one is a 2 px outer box-shadow in {@code --ash-table-ring}) or what surrounds it. Each must be rejected. */
    private static Map<String, String> brokenRings() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("ring missing", RING + "{box-shadow:none !important;outline:none !important}");
        m.put("ring 1 px thinner", RING + "{box-shadow:0 0 0 1px var(--ash-table-ring) !important}");
        m.put("ring 1 px thicker", RING + "{box-shadow:0 0 0 3px var(--ash-table-ring) !important}");
        m.put("ring displaced 1 px", RING + "{box-shadow:1px 1px 0 2px var(--ash-table-ring) !important}");
        for (int percent : new int[] {95, 90, 75}) {
            m.put("ring at " + percent + "% strength", RING + "{box-shadow:0 0 0 2px color-mix(in srgb,var(--ash-table-ring) " + percent + "%,transparent) !important}");
        }
        m.put("ring radius 5 px", RING + "{border-radius:5px !important}");
        m.put("ring radius 6 px", RING + "{border-radius:6px !important}");
        m.put("change inside the button", "[data-table-sort-icon]{color:#000 !important}");
        m.put("change outside the ring", ".ash-table__head{background-color:#fff !important}");
        return m;
    }

    /** Ways to break the hovered sort button. Each must be rejected. */
    private static Map<String, String> brokenHovers() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("label recolored", HOVERED + "{color:#000 !important}");
        m.put("label at 95% opacity", HOVERED + "{opacity:.95 !important}");
        m.put("label italic", HOVERED + "{font-style:italic !important}");
        m.put("label shifted 1 px", HOVERED + "{position:relative !important;left:1px !important}");
        m.put("label letter-spaced", HOVERED + "{letter-spacing:.02em !important}");
        m.put("background tinted", HOVERED + "{background-color:rgb(255 0 0 / .04) !important}");
        m.put("change inside the button", "[data-table-sort-icon]{color:#000 !important}");
        m.put("change outside the button", ".ash-table__head{background-color:#fff !important}");
        return m;
    }

    private static void control(ParitySession session, CascadeFixtureServer server, List<TableParityTest.Field> fields, String caseKey, Map<String, String> broken) throws Exception {
        TableParityTest.Field f = fields.stream().filter(x -> x.spec().key().equals(caseKey)).findFirst().orElseThrow();
        PixelPolicy.Policy policy = PixelPolicy.forCase(caseKey);
        assertTrue(policy.maxDifferingPixels() > 0, caseKey + " has no tolerance, so there is nothing for a control to show");
        for (String id : CONTROL_CONFIGS) {
            ParityConfigs.Config config = ParityConfigs.get(id);
            String migratedBody = f.markup();
            String legacyBody = new IconLegacyMarkup().rewrite(new TableLegacyMarkup().rewrite(migratedBody));
            String migratedPage = singlePage(List.of(ParityFrames.APP), config, migratedBody, null);
            try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                Picture legacy = picture(cdp, server.registerRaw(singlePage(List.of(ParityFrames.generated("table-oracle.css")), config, legacyBody, null)), f);
                for (Map.Entry<String, String> m : broken.entrySet()) {
                    Picture picture = picture(cdp, server.registerRaw(migratedPage.replace("</head>", "<style>" + m.getValue() + "</style></head>")), f);
                    PixelPolicy.Verdict verdict = PixelPolicy.compare(legacy.png(), picture.png(), policy, legacy.geometry());
                    controlsRun++;
                    if (verdict.accepted()) {
                        CONTROL_FAILURES.add(id + " " + caseKey + " / " + m.getKey() + ": accepted (" + verdict.describe() + ")");
                    }
                }
            }
        }
    }

    @BeforeAll
    static void run() throws Exception {
        started = System.nanoTime();
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
            // Controls: the tolerances still reject broken renderings, judged by the production comparison on real renderings.
            control(session, server, fields, "sort button focus-visible", brokenRings());
            control(session, server, fields, "sort button hover", brokenHovers());
            control(session, server, fields, "active sort button hover", brokenHovers());
        }
        System.out.println("TABLE-PIXELS: " + compared + " tables compared, " + TOLERATED.size() + " tolerated under the named policies, " + MISMATCHES.size() + " rejected; " + freeCompared
                + " without Tailwind, " + FREE_MISMATCHES.size() + " rejected; " + controlsRun + " broken renderings tried against the policies, " + CONTROL_FAILURES.size() + " wrongly accepted");
        TOLERATED.forEach(t -> System.out.println("TABLE-PIXELS tolerated: " + t));
        reportDiagnostics(session, fields);
    }

    /**
     * How every pair that was not byte-identical differs, printed after every picture has been taken and every verdict reached. It reads what was kept and changes nothing: whatever
     * happens here, the verdicts, the assertions and the policies are what they were.
     */
    private static void reportDiagnostics(ParitySession session, List<TableParityTest.Field> fields) {
        try {
            List<PixelDiagnostics.Capture> captures = new ArrayList<>();
            for (Seen s : SEEN) {
                TableBrowser.Case spec = s.field().spec();
                int index = fields.indexOf(s.field());
                Map<String, String> context = new LinkedHashMap<>();
                context.put("configuration", s.config().id());
                context.put("theme", s.config().theme());
                context.put("skin", s.config().skin() == null ? "none" : s.config().skin());
                context.put("run", s.where());
                context.put("fixture", s.field().id());
                context.put("case", spec.key());
                context.put("state", spec.pseudo().isEmpty() ? "none" : String.join("+", spec.pseudo()));
                context.put("target", spec.target() == null ? "-" : spec.target());
                context.put("policy", s.verdict().policy());
                context.put("browser", index < 0 ? "?" : (index / CHUNK) + "." + (index % CHUNK));
                context.put("seconds", String.valueOf((s.nanos() - started) / 1_000_000_000L));
                captures.add(new PixelDiagnostics.Capture(s.label(), context, s.legacyPng(), s.migratedPng(), s.verdict().accepted(), s.verdict().describe(), s.geometry()));
            }
            PixelDiagnostics.report("TABLE-PIXELS", captures, Path.of("build", "pixel-diagnostics", "table"), System.out, PixelDiagnostics.environment(session.chrome().executable()));
        } catch (Exception e) {
            System.out.println("TABLE-PIXELS diagnostics unavailable: " + e);
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

    @Test
    void aKnownVisualDifferenceIsStillRejectedAndTheDiagnosticsMeasureItFromTheSamePictures() throws Exception {
        // Two real pictures of one browser that differ in the checkbox tint: the exact policy rejects them, and the diagnostics say how, without changing that.
        PixelPolicy.Verdict verdict = PixelPolicy.compare(controlOriginal, controlBrokenAccent, PixelPolicy.EXACT, null);
        assertFalse(verdict.accepted(), "the exact policy rejects the recolored checkboxes");
        assertFalse(verdict.exact());
        PixelDiagnostics.Difference d = PixelDiagnostics.measure(controlOriginal, controlBrokenAccent);
        assertTrue(d.sameSize() && d.any(), d.describe());
        assertTrue(d.minX() >= 0 && d.minY() >= 0 && d.maxX() < d.referenceWidth() && d.maxY() < d.referenceHeight() && d.minX() <= d.maxX() && d.minY() <= d.maxY(), d.describe());
        assertTrue(d.maxChannel() > 0, d.describe());

        Path directory = Files.createTempDirectory("table-pixel-diagnostics");
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            PixelDiagnostics.report("CONTROL", List.of(new PixelDiagnostics.Capture("accent", Map.of("case", "accent"), controlOriginal, controlBrokenAccent, false, verdict.describe(), null)),
                    directory, new PrintStream(bytes, true, StandardCharsets.UTF_8), "test");
            String text = bytes.toString(StandardCharsets.UTF_8);
            assertTrue(text.contains("CONTROL REJECTED [0] accent: differing=" + d.differing() + " maxChannel=" + d.maxChannel()), text);
            assertTrue(text.contains("CONTROL image [0] reference " + controlOriginal + "\n") || text.contains("CONTROL image [0] reference " + controlOriginal + "\r\n"), "the reference picture is printed as taken");
            assertTrue(text.contains("CONTROL image [0] candidate " + controlBrokenAccent), "the candidate picture is printed as taken");
            for (String file : List.of("reference.png", "candidate.png", "diff.png", "zoom.png", "report.txt")) {
                assertTrue(Files.exists(directory.resolve("0-accent").resolve(file)), file);
            }
        } finally {
            try (var walk = Files.walk(directory)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            } catch (IOException ignored) {
                // a leftover temporary directory is not worth a failure
            }
        }
    }

    @Test
    void theTolerancesStillRejectEveryBrokenRingAndEveryBrokenHoveredButton() {
        int expected = CONTROL_CONFIGS.size() * (brokenRings().size() + 2 * brokenHovers().size());
        assertEquals(expected, controlsRun, "every control ran");
        assertEquals(List.of(), CONTROL_FAILURES, "broken renderings the policy accepted");
    }
}
