package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TEMPORARY DIAGNOSTIC, not for merge. Runs on CI, or locally with {@code ASHLAR_TABLE_STUDY_DIR} set (see {@link TablePixelRetryStudyTest#assumeEnabled()}). With
 * {@code ASHLAR_TABLE_RETRY_BASELINE_ONLY=true} it runs one execution per control and configuration without retries: the check that the current policy rejects every must-reject
 * control before any retry behavior is evaluated.
 *
 * <p>Negative controls for the retry study. Each execution is a block replay (a new browser process, TablePixelTest's block and order) with a deliberate defect injected into the
 * target case's migrated page only, followed by {@value TablePixelRetryStudy#MAX_RETRIES} retries in each mode with the same defect. A defect is fixed once per control and
 * configuration: either a CSS rule, or injected pixels whose positions and colors are measured from the legacy page in a separate setup process and never recomputed. The legacy
 * (reference) page is never changed. Every migrated capture of the target records whether the defect stylesheet was present, and injected pixels are checked in every capture.
 */
class TablePixelRetryControlStudyTest {

    static final int EXECUTIONS = 2;
    static final long SEED = 20261118L;
    static final String CLEAR_HOVER = "clear selection hover";
    static final String CLEAR_FOCUS = "clear selection focus-visible";
    static final String SORT_HOVER = "sort button hover";

    /** A control and the configurations it runs in. */
    record Spec(TablePixelRetryStudy.Control control, List<String> configs) {
    }

    static final List<Spec> CONTROLS = List.of(
            new Spec(new TablePixelRetryStudy.Control("MR1", "must-reject", CLEAR_HOVER, "hover underline removed"), List.of("dark/fathom", "light/sterling")),
            new Spec(new TablePixelRetryStudy.Control("MR2", "must-reject", CLEAR_FOCUS, "focus ring 1 px thinner"), List.of("dark/harbor", "light/sterling")),
            new Spec(new TablePixelRetryStudy.Control("MR3", "must-reject", CLEAR_HOVER, "selection bar corner radius 1 px smaller"), List.of("dark/fathom", "light/sterling")),
            new Spec(new TablePixelRetryStudy.Control("MR4", "must-reject", CLEAR_HOVER, "1 px dot, +3 levels, inside the bar near its bottom-left corner"),
                    List.of("dark/fathom", "light/sterling")),
            new Spec(new TablePixelRetryStudy.Control("MR5", "must-reject", SORT_HOVER, "hovered label color +1 level"), List.of("light/foundry", "dark/harbor")),
            new Spec(new TablePixelRetryStudy.Control("MR6", "must-reject", SORT_HOVER, "hovered label shifted 1 px"), List.of("light/foundry", "dark/harbor")),
            new Spec(new TablePixelRetryStudy.Control("SP1", "sensitivity", CLEAR_HOVER, "1 px dot, +1 level, inside the bar near its bottom-left corner"),
                    List.of("dark/fathom", "light/sterling")),
            new Spec(new TablePixelRetryStudy.Control("SP2", "sensitivity", SORT_HOVER, "3 px, +30 levels, in the hover band left of the label (one more than allowed)"),
                    List.of("light/foundry", "dark/harbor")));

    /** What a setup process measured on the legacy page of a case: its picture, the field's viewport origin, the bar's box in picture coordinates, and the target's color. */
    record Setup(String png, double fieldLeft, double fieldTop, double[] bar, String targetColor, String geometry) {
    }

    /** A fixed defect: the CSS, and the injected pixels (picture x, y, ARGB) to look for in every capture. */
    record Defect(String css, List<int[]> pixels) {
    }

    static boolean baselineOnly() {
        return "true".equalsIgnoreCase(System.getenv("ASHLAR_TABLE_RETRY_BASELINE_ONLY"));
    }

    /** The legacy page of the case, alone in a new process, with its state forced; measured while forced. */
    static Setup setup(ParitySession session, CascadeFixtureServer server, ParityConfigs.Config config, TableParityTest.Field f, TablePixelRetryStudyTest.Counts counts)
            throws Exception {
        counts.process("setup");
        try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
            cdp.navigate(server.registerRaw(TablePixelRetryStudyTest.legacyPage(config, f)));
            counts.navigation("setup");
            String target = TableBrowser.target(f.spec(), f.id());
            List<Integer> nodes = cdp.nodes(target);
            assertFalse(nodes.isEmpty(), f.spec().key() + ": the target was not found");
            cdp.force(nodes, f.spec().pseudo());
            String geometry = cdp.evaluate(TablePixelRetryStudyTest.geometryScript(f.id(), target));
            String measured = cdp.evaluate("(function(){var f=document.querySelector('[data-field=\"" + f.id() + "\"]').getBoundingClientRect();"
                    + "var b=document.querySelector('[data-field=\"" + f.id() + "\"] [data-table-selection-bar]');var r=b?b.getBoundingClientRect():null;"
                    + "var t=getComputedStyle(document.querySelector(" + DevToolsSession.quote(target) + ")).color;"
                    // the computed color can be in any color space (a skin may use oklch); painted into a 1x1 canvas it reads back as the sRGB bytes it renders as
                    + "var cv=document.createElement('canvas');cv.width=cv.height=1;var cx=cv.getContext('2d');cx.fillStyle=t;cx.fillRect(0,0,1,1);var d=cx.getImageData(0,0,1,1).data;"
                    + "return [f.left,f.top,r?(r.left-f.left+6):-1,r?(r.top-f.top+6):-1,r?r.width:-1,r?r.height:-1].join(' ')+'|rgb('+d[0]+', '+d[1]+', '+d[2]+')|'+t})()");
            String png = TablePixelRetryStudyTest.shot(cdp, f.id());
            TablePixelRetryStudyTest.shot(cdp, f.id()); // the second screenshot, as in every capture (both are counted with the navigation)
            cdp.force(nodes, List.of());
            String[] parts = measured.split("\\|");
            String[] n = parts[0].split(" ");
            double[] bar = Double.parseDouble(n[2]) < 0 ? null : new double[] {Double.parseDouble(n[2]), Double.parseDouble(n[3]), Double.parseDouble(n[4]), Double.parseDouble(n[5])};
            return new Setup(png, Double.parseDouble(n[0]), Double.parseDouble(n[1]), bar, parts[1] + " (computed " + parts[2] + ")", geometry);
        }
    }

    private static int shift(int channel, int k) {
        return channel + k <= 255 ? channel + k : channel - k;
    }

    private static String rgb(int argb) {
        return "rgb(" + ((argb >> 16) & 0xFF) + "," + ((argb >> 8) & 0xFF) + "," + (argb & 0xFF) + ")";
    }

    private static boolean flat(BufferedImage image, int x, int y) {
        int c = image.getRGB(x, y);
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (image.getRGB(x + dx, y + dy) != c) {
                    return false;
                }
            }
        }
        return true;
    }

    /** One fixed-position element (viewport coordinates) painting the given pixels: the first as its background, the others as offset box-shadows. */
    private static String pixelsCss(Setup s, List<int[]> pixels) {
        int[] first = pixels.get(0);
        double x = s.fieldLeft() - 6 + first[0];
        double y = s.fieldTop() - 6 + first[1];
        assertTrue(x == Math.rint(x) && y == Math.rint(y), "the field is not at whole pixels: " + x + "," + y);
        StringBuilder shadows = new StringBuilder();
        for (int i = 1; i < pixels.size(); i++) {
            int[] p = pixels.get(i);
            shadows.append(shadows.length() == 0 ? "" : ",").append(p[0] - first[0]).append("px ").append(p[1] - first[1]).append("px 0 0 ").append(rgb(p[2]));
        }
        return "body::after{content:\"\";position:fixed;left:" + (int) x + "px;top:" + (int) y + "px;width:1px;height:1px;background:" + rgb(first[2]) + ";z-index:2147483647;"
                + "pointer-events:none" + (shadows.length() == 0 ? "" : ";box-shadow:" + shadows) + "}";
    }

    /** The fixed defect of a control in a configuration. */
    static Defect defect(String id, Setup s) throws Exception {
        switch (id) {
            case "MR1":
                return new Defect("[data-table-clear-selection]:hover{text-decoration-line:none !important}", List.of());
            case "MR2":
                return new Defect("[data-table-clear-selection]:focus-visible{box-shadow:0 0 0 2px #fff, 0 0 0 3px var(--ash-table-ring) !important}", List.of());
            case "MR3":
                return new Defect("[data-table-selection-bar]{border-radius:calc(var(--ash-radius-md) - 1px) !important}", List.of());
            case "MR6":
                return new Defect("[data-table-sort]:hover{position:relative !important;left:1px !important}", List.of());
            case "MR5": {
                Matcher m = Pattern.compile("rgba?\\((\\d+),\\s*(\\d+),\\s*(\\d+)").matcher(s.targetColor());
                assertTrue(m.find(), "no color: " + s.targetColor());
                int r = Integer.parseInt(m.group(1));
                int g = Integer.parseInt(m.group(2));
                int b = Integer.parseInt(m.group(3));
                return new Defect("[data-table-sort]:hover{color:rgb(" + r + "," + g + "," + shift(b, 1) + ") !important}", List.of());
            }
            case "MR4":
            case "SP1": {
                BufferedImage legacy = PixelDiagnostics.decode(s.png());
                assertTrue(s.bar() != null, "no selection bar on this page");
                int px = (int) Math.floor(s.bar()[0]) + 10;
                int py = (int) Math.floor(s.bar()[1] + s.bar()[3]) - 5;
                int tries = 0;
                while (!flat(legacy, px, py) && tries++ < 20) {
                    px++;
                }
                assertTrue(flat(legacy, px, py), "no flat pixel inside the bar near its bottom-left corner");
                int c = legacy.getRGB(px, py);
                int k = id.equals("MR4") ? 3 : 1;
                int dot = 0xFF000000 | (c & 0xFFFF00) | shift(c & 0xFF, k);
                List<int[]> pixels = List.<int[]>of(new int[] {px, py, dot});
                return new Defect(pixelsCss(s, pixels), pixels);
            }
            case "SP2": {
                BufferedImage legacy = PixelDiagnostics.decode(s.png());
                PixelPolicy.Geometry g = PixelPolicy.Geometry.parse(s.geometry());
                assertTrue(g != null, "no geometry: " + s.geometry());
                int px = (int) Math.floor(g.box().x()) - 1;
                int py = (int) Math.floor(g.box().y()) + 4;
                List<int[]> pixels = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    int c = legacy.getRGB(px, py + i);
                    pixels.add(new int[] {px, py + i, 0xFF000000 | (c & 0xFFFF00) | shift(c & 0xFF, 30)});
                }
                return new Defect(pixelsCss(s, pixels), pixels);
            }
            default:
                throw new IllegalArgumentException(id);
        }
    }

    @Test
    void controlStudy() throws Exception {
        TablePixelRetryStudyTest.assumeEnabled();
        boolean baselineOnly = baselineOnly();
        int executions = baselineOnly ? 1 : EXECUTIONS;
        Path out = TablePixelRetryStudyTest.outputDir(baselineOnly ? "controls-baseline" : "controls");
        ParitySession session = ParitySession.open("table-pixel-retry-control-study");
        List<TableParityTest.Field> fields = TableParityTest.fields();
        TablePixelRetryStudy.Pictures pictures = new TablePixelRetryStudy.Pictures();
        TablePixelRetryStudyTest.Counts counts = new TablePixelRetryStudyTest.Counts();
        List<TablePixelRetryStudy.ControlRun> runs = new ArrayList<>();
        Map<String, Setup> setups = new LinkedHashMap<>();
        Map<String, Defect> defects = new LinkedHashMap<>();
        int plannedNavigations = 0;
        long began = System.nanoTime();
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            // the defects, fixed once per control and configuration
            for (Spec spec : CONTROLS) {
                TableParityTest.Field f = fields.stream().filter(x -> x.spec().key().equals(spec.control().caseKey())).findFirst().orElseThrow();
                for (String configId : spec.configs()) {
                    ParityConfigs.Config config = ParityConfigs.get(configId);
                    Setup s = null;
                    if (List.of("MR4", "SP1", "MR5", "SP2").contains(spec.control().id())) {
                        String key = f.spec().key() + "|" + configId;
                        if (!setups.containsKey(key)) {
                            setups.put(key, setup(session, server, config, f, counts));
                            plannedNavigations += 1;
                        }
                        s = setups.get(key);
                    }
                    defects.put(spec.control().id() + "|" + configId, defect(spec.control().id(), s));
                }
            }
            // the executions, in a seeded random order
            List<Object[]> schedule = new ArrayList<>();
            for (Spec spec : CONTROLS) {
                for (String configId : spec.configs()) {
                    for (int e = 1; e <= executions; e++) {
                        schedule.add(new Object[] {spec, configId, e});
                    }
                }
            }
            Collections.shuffle(schedule, new Random(SEED));
            Random order = new Random(SEED + 7);
            for (Object[] run : schedule) {
                Spec spec = (Spec) run[0];
                String configId = (String) run[1];
                int e = (Integer) run[2];
                ParityConfigs.Config config = ParityConfigs.get(configId);
                TableParityTest.Field f = fields.stream().filter(x -> x.spec().key().equals(spec.control().caseKey())).findFirst().orElseThrow();
                int block = fields.indexOf(f) / TablePixelRetryStudyTest.BLOCK;
                int index = fields.indexOf(f) % TablePixelRetryStudyTest.BLOCK;
                List<TableParityTest.Field> chunk = TablePixelRetryStudyTest.block(fields, block);
                Defect d = defects.get(spec.control().id() + "|" + configId);
                String unit = spec.control().id() + " " + configId + " execution " + e;
                int process = counts.process("control primary");
                TablePixelRetryStudy.Pair primary;
                long started = System.nanoTime();
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    primary = TablePixelRetryStudyTest.replay(cdp, server, chunk, block, config, chunk.size() - 1, f.spec().key(), d.css(), pictures, counts, "control primary", unit,
                            "primary", 0, process, true).get(0);
                }
                counts.time("control primary", (System.nanoTime() - started) / 1_000_000L);
                plannedNavigations += 2 * chunk.size();
                List<TablePixelRetryStudy.Pair> retries = List.of();
                if (!baselineOnly) {
                    retries = TablePixelRetryStudyTest.retries(session, server, chunk, block, config, f.spec().key(), d.css(), pictures, counts, unit, "control retry ", order);
                    plannedNavigations += TablePixelRetryStudy.MAX_RETRIES * (2 + 2 * (index + 1));
                }
                int checks = 0;
                int passed = 0;
                if (!d.pixels().isEmpty()) {
                    List<TablePixelRetryStudy.Pair> all = new ArrayList<>(List.of(primary));
                    all.addAll(retries);
                    for (TablePixelRetryStudy.Pair p : all) {
                        BufferedImage migrated = PixelDiagnostics.decode(pictures.get(p.migrated()));
                        for (int[] px : d.pixels()) {
                            checks++;
                            if ((migrated.getRGB(px[0], px[1]) | 0xFF000000) == px[2]) {
                                passed++;
                            }
                        }
                    }
                }
                runs.add(new TablePixelRetryStudy.ControlRun(spec.control(), configId, e, d.css(), new TablePixelRetryStudy.Procedure(unit, primary, retries), checks, passed));
            }
        }
        long seconds = (System.nanoTime() - began) / 1_000_000_000L;
        StringBuilder defectList = new StringBuilder();
        defects.forEach((k, d) -> defectList.append("  ").append(k).append(": ").append(d.css()).append('\n'));
        String header = "TABLE PIXEL RETRY STUDY, controls (temporary diagnostic)" + (baselineOnly ? " - BASELINE ONLY (one execution each, no retries)" : "") + "\nenvironment: "
                + PixelDiagnostics.environment(session.chrome().executable()) + "\nseed=" + SEED + " executions per control and configuration=" + executions + "\ndefects:\n" + defectList
                + "planned: " + plannedNavigations + " navigations, " + 2 * plannedNavigations + " screenshots\nactual: " + counts.describe() + "\nwall time per process: "
                + counts.timings() + "\nduration: " + seconds + " s\n";
        String report = header + "\n" + TablePixelRetryStudy.controlReport(runs, pictures);
        long bytes = TablePixelRetryStudyTest.finish(out, report, pictures);
        System.out.println("TABLE-RETRY-STUDY report bytes: " + bytes);
        int expectedRuns = CONTROLS.stream().mapToInt(s -> s.configs().size()).sum() * executions;
        assertEquals(expectedRuns, runs.size(), "every planned control execution ran");
        assertEquals(plannedNavigations, counts.navigations, "navigation accounting");
    }
}
