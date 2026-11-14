package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * TEMPORARY DIAGNOSTIC, not for merge. Runs on CI (the {@code CI} variable is set) or locally when {@code ASHLAR_TABLE_STUDY_DIR} is set; skipped otherwise.
 *
 * <p>Fresh-browser retry study, primary part. A primary attempt is one browser process replaying one of {@link TablePixelTest}'s browser blocks exactly as TablePixelTest runs it
 * (its 12 cases in order, each the legacy page then the migrated page, with TablePixelTest's capture sequence and the stylesheets TablePixelTest uses), and every pair is judged by
 * {@link PixelPolicy}, read only. Each pair the policy rejects starts a procedure (at most {@link #MAX_PROCEDURES}): {@value TablePixelRetryStudy#MAX_RETRIES} retries in each of
 * two modes, every retry a new browser process: case-only (the case's legacy and migrated pages as the process's first two pages) and block-replay (the block from its first case
 * up to and including the case, so the case is at its own position). All retries are always run, so that every candidate policy can be read from the same record. Nothing here
 * asserts parity; see {@link TablePixelRetryStudy} for the analysis.
 */
class TablePixelRetryStudyTest {

    static final int BLOCK = 12;
    static final long SEED = 20261117L;
    static final int MAX_PROCEDURES = 20;

    /** One cell of the primary matrix: a TablePixelTest block, a configuration, and how many attempts (browser processes) replay it. */
    record Cell(int block, String config, int attempts) {
    }

    static final List<Cell> MATRIX = List.of(
            new Cell(0, "light/foundry", 16), new Cell(0, "dark/fathom", 4), new Cell(0, "light/sterling", 4), new Cell(0, "dark/harbor", 4),
            new Cell(0, "light/none", 4), new Cell(0, "dark/none", 4), new Cell(0, "light/axiom", 4), new Cell(0, "dark/summit", 4),
            new Cell(1, "light/foundry", 4), new Cell(1, "dark/fathom", 12), new Cell(1, "light/sterling", 12), new Cell(1, "dark/harbor", 12),
            new Cell(1, "light/none", 4), new Cell(1, "dark/none", 4), new Cell(1, "light/axiom", 4), new Cell(1, "dark/summit", 4));

    // ---------------------------------------------------------------- shared with the control study

    /** Processes, navigations and screenshots actually made, by kind of process. */
    static final class Counts {
        int processes;
        int navigations;
        int screenshots;
        final Map<String, int[]> byKind = new java.util.LinkedHashMap<>();

        int process(String kind) {
            processes++;
            byKind.computeIfAbsent(kind, k -> new int[3])[0]++;
            return processes;
        }

        void navigation(String kind) {
            navigations++;
            screenshots += 2;
            int[] c = byKind.computeIfAbsent(kind, k -> new int[3]);
            c[1]++;
            c[2] += 2;
        }

        String describe() {
            StringBuilder sb = new StringBuilder(String.format("%d processes, %d navigations, %d screenshots", processes, navigations, screenshots));
            byKind.forEach((k, c) -> sb.append(String.format("; %s: %d processes, %d navigations, %d screenshots", k, c[0], c[1], c[2])));
            return sb.toString();
        }
    }

    static void assumeEnabled() {
        String dir = System.getenv("ASHLAR_TABLE_STUDY_DIR");
        Assumptions.assumeTrue(System.getenv("CI") != null || (dir != null && !dir.isBlank()), "diagnostic study: runs on CI, or locally with ASHLAR_TABLE_STUDY_DIR set");
    }

    static Path outputDir(String study) throws IOException {
        String dir = System.getenv("ASHLAR_TABLE_STUDY_DIR");
        Path root = dir != null && !dir.isBlank() ? Path.of(dir, "out") : Path.of("build", "table-pixel-study");
        Path out = root.resolve(study + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
        Files.createDirectories(out.resolve("pictures"));
        return out;
    }

    // copied verbatim from TablePixelTest (private there), so that the capture is the same
    static String shot(DevToolsSession cdp, String fieldId) throws Exception {
        String box = cdp.evaluate("(function(){var b=document.querySelector('[data-field=\"" + fieldId + "\"]').getBoundingClientRect();return JSON.stringify({x:b.x+scrollX-6,y:b.y+scrollY-6,width:b.width+12,height:b.height+12})})()");
        String response = cdp.call("Page.captureScreenshot", "{\"clip\":" + box.replace("}", ",\"scale\":1}") + "}");
        int at = response.indexOf("\"data\":\"");
        if (at <= 0) {
            throw new IllegalStateException(response);
        }
        return response.substring(at + 8, response.indexOf('"', at + 8));
    }

    // copied verbatim from TablePixelTest (private there)
    static String geometryScript(String fieldId, String selector) {
        return "(function(){var f=document.querySelector('[data-field=\"" + fieldId + "\"]').getBoundingClientRect();var t=document.querySelector(" + DevToolsSession.quote(selector)
                + ");var r=t.getBoundingClientRect();var c=getComputedStyle(t);var reach=c.outlineStyle==='none'?0:Math.max(0,parseFloat(c.outlineWidth)+parseFloat(c.outlineOffset));"
                + "(c.boxShadow.match(/-?[\\d.]+px -?[\\d.]+px -?[\\d.]+px -?[\\d.]+px/g)||[]).forEach(function(s){var n=s.split(' ').map(parseFloat);reach=Math.max(reach,n[3]+n[2]+Math.max(Math.abs(n[0]),Math.abs(n[1])))});"
                + "return [r.x-f.x+6,r.y-f.y+6,r.width,r.height,reach].join(' ')})()";
    }

    /** A page as captured: the first screenshot, the geometry when a state was forced (empty otherwise), and whether the two screenshots agreed. */
    record Shot(String png, String geometry, boolean stable) {
    }

    /** TablePixelTest's picture(): navigate, force the state, read the geometry, two screenshots, remove the state. Records instead of asserting that the screenshots agree. */
    static Shot picture(DevToolsSession cdp, String url, TableParityTest.Field f, Counts counts, String kind) throws Exception {
        cdp.navigate(url);
        counts.navigation(kind);
        List<String> pseudo = f.spec().pseudo();
        List<Integer> nodes = pseudo.isEmpty() ? List.of() : cdp.nodes(TableBrowser.target(f.spec(), f.id()));
        String geometry = "";
        if (!pseudo.isEmpty()) {
            assertFalse(nodes.isEmpty(), f.spec().key() + ": the target was not found");
            cdp.force(nodes, pseudo);
            geometry = cdp.evaluate(geometryScript(f.id(), TableBrowser.target(f.spec(), f.id())));
        }
        String first = shot(cdp, f.id());
        String second = shot(cdp, f.id());
        if (!pseudo.isEmpty()) {
            cdp.force(nodes, List.of());
        }
        return new Shot(first, geometry, first.equals(second));
    }

    /** The legacy page, exactly as TablePixelTest builds it. */
    static String legacyPage(ParityConfigs.Config config, TableParityTest.Field f) throws Exception {
        String body = new IconLegacyMarkup().rewrite(new TableLegacyMarkup().rewrite(f.markup()));
        return TextEntryBrowser.page(List.of(ParityFrames.generated("table-oracle.css")), config, "", body, TableBrowser.INDETERMINATE);
    }

    /** The migrated page, exactly as TablePixelTest builds it, plus (when given) the injected defect as the last stylesheet of the head. */
    static String migratedPage(ParityConfigs.Config config, TableParityTest.Field f, String defectCss) {
        String page = TextEntryBrowser.page(List.of(ParityFrames.APP), config, "", f.markup(), TableBrowser.INDETERMINATE);
        return defectCss == null ? page : page.replace("</head>", "<style id=\"study-defect\">" + defectCss + "</style></head>");
    }

    static boolean defectPresent(DevToolsSession cdp) throws Exception {
        return cdp.evaluate("document.getElementById('study-defect')?'y':'n'").equals("y");
    }

    static List<TableParityTest.Field> block(List<TableParityTest.Field> fields, int block) {
        return fields.subList(block * BLOCK, Math.min(fields.size(), block * BLOCK + BLOCK));
    }

    /**
     * In the given (open) process, the block's cases from the first up to and including {@code upTo}, each the legacy page then the migrated page; the defect, if any, only on
     * {@code target}'s migrated page. Returns the pairs, all of them or only the target's.
     */
    static List<TablePixelRetryStudy.Pair> replay(DevToolsSession cdp, CascadeFixtureServer server, List<TableParityTest.Field> chunk, int block, ParityConfigs.Config config,
                                                  int upTo, String target, String defectCss, TablePixelRetryStudy.Pictures pictures, Counts counts, String kind, String unit,
                                                  String mode, int retry, int process, boolean onlyTarget) throws Exception {
        List<TablePixelRetryStudy.Pair> pairs = new ArrayList<>();
        for (int i = 0; i <= upTo; i++) {
            TableParityTest.Field f = chunk.get(i);
            boolean isTarget = f.spec().key().equals(target);
            Shot legacy = picture(cdp, server.registerRaw(legacyPage(config, f)), f, counts, kind);
            Shot migrated = picture(cdp, server.registerRaw(migratedPage(config, f, isTarget ? defectCss : null)), f, counts, kind);
            boolean present = isTarget && defectCss != null && defectPresent(cdp);
            if (!onlyTarget || isTarget) {
                pairs.add(new TablePixelRetryStudy.Pair(unit, mode, retry, process, config.id(), block, f.spec().key(), f.id(), 2 * i + 1, pictures.add(legacy.png()),
                        pictures.add(migrated.png()), legacy.geometry(), legacy.stable(), migrated.stable(), present));
            }
        }
        return pairs;
    }

    /**
     * The retries of one procedure: for r = 1..3, one case-only and one block-replay retry (in a seeded random order), each in a new process, each with the same defect.
     */
    static List<TablePixelRetryStudy.Pair> retries(ParitySession session, CascadeFixtureServer server, List<TableParityTest.Field> chunk, int block, ParityConfigs.Config config,
                                                   String target, String defectCss, TablePixelRetryStudy.Pictures pictures, Counts counts, String unit, String kindPrefix,
                                                   Random order) throws Exception {
        int index = -1;
        for (int i = 0; i < chunk.size(); i++) {
            if (chunk.get(i).spec().key().equals(target)) {
                index = i;
            }
        }
        List<TablePixelRetryStudy.Pair> out = new ArrayList<>();
        for (int r = 1; r <= TablePixelRetryStudy.MAX_RETRIES; r++) {
            List<String> modes = new ArrayList<>(TablePixelRetryStudy.MODES);
            Collections.shuffle(modes, order);
            for (String mode : modes) {
                String kind = kindPrefix + mode;
                int process = counts.process(kind);
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    if (mode.equals("case-only")) {
                        TableParityTest.Field f = chunk.get(index);
                        Shot legacy = picture(cdp, server.registerRaw(legacyPage(config, f)), f, counts, kind);
                        Shot migrated = picture(cdp, server.registerRaw(migratedPage(config, f, defectCss)), f, counts, kind);
                        boolean present = defectCss != null && defectPresent(cdp);
                        out.add(new TablePixelRetryStudy.Pair(unit, mode, r, process, config.id(), block, f.spec().key(), f.id(), 1, pictures.add(legacy.png()),
                                pictures.add(migrated.png()), legacy.geometry(), legacy.stable(), migrated.stable(), present));
                    } else {
                        List<TablePixelRetryStudy.Pair> pairs = replay(cdp, server, chunk, block, config, index, target, defectCss, pictures, counts, kind, unit, mode, r, process, true);
                        out.addAll(pairs);
                    }
                }
            }
        }
        return out;
    }

    /** Writes the report and every distinct picture, prints the report, and returns its size in bytes. */
    static long finish(Path out, String report, TablePixelRetryStudy.Pictures pictures) throws IOException {
        Files.writeString(out.resolve("report.txt"), report);
        for (Map.Entry<String, String> p : pictures.all().entrySet()) {
            Files.write(out.resolve("pictures").resolve(TablePixelRetryStudy.id(p.getKey()) + ".png"), Base64.getDecoder().decode(p.getValue()));
        }
        System.out.println(report);
        System.out.println("TABLE-RETRY-STUDY written to " + out.toAbsolutePath());
        return report.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    }

    // ---------------------------------------------------------------- the primary study

    @Test
    void retryStudy() throws Exception {
        assumeEnabled();
        Path out = outputDir("retry");
        ParitySession session = ParitySession.open("table-pixel-retry-study");
        List<TableParityTest.Field> fields = TableParityTest.fields();
        TablePixelRetryStudy.Pictures pictures = new TablePixelRetryStudy.Pictures();
        Counts counts = new Counts();
        List<TablePixelRetryStudy.Pair> primary = new ArrayList<>();
        List<TablePixelRetryStudy.Procedure> procedures = new ArrayList<>();
        int beyondCap = 0;
        int plannedRetryNavigations = 0;
        long began = System.nanoTime();
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            List<Cell> schedule = new ArrayList<>();
            for (Cell c : MATRIX) {
                for (int k = 0; k < c.attempts(); k++) {
                    schedule.add(c);
                }
            }
            Collections.shuffle(schedule, new Random(SEED));
            Random order = new Random(SEED + 7);
            int attempt = 0;
            for (Cell c : schedule) {
                attempt++;
                ParityConfigs.Config config = ParityConfigs.get(c.config());
                List<TableParityTest.Field> chunk = block(fields, c.block());
                int process = counts.process("primary");
                List<TablePixelRetryStudy.Pair> pairs;
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    pairs = replay(cdp, server, chunk, c.block(), config, chunk.size() - 1, null, null, pictures, counts, "primary", "attempt " + attempt, "primary", 0, process, false);
                }
                primary.addAll(pairs);
                for (TablePixelRetryStudy.Pair pair : pairs) {
                    if (TablePixelRetryStudy.judge(pictures, pair).accepted()) {
                        continue;
                    }
                    if (procedures.size() >= MAX_PROCEDURES) {
                        beyondCap++;
                        continue;
                    }
                    int index = (pair.legacySlot() - 1) / 2;
                    plannedRetryNavigations += TablePixelRetryStudy.MAX_RETRIES * (2 + 2 * (index + 1));
                    List<TablePixelRetryStudy.Pair> retries = retries(session, server, chunk, c.block(), config, pair.caseKey(), null, pictures, counts, "procedure " + (procedures.size() + 1),
                            "retry ", order);
                    procedures.add(new TablePixelRetryStudy.Procedure("procedure " + (procedures.size() + 1), pair, retries));
                }
            }
        }
        long seconds = (System.nanoTime() - began) / 1_000_000_000L;
        int attempts = MATRIX.stream().mapToInt(Cell::attempts).sum();
        String header = "TABLE PIXEL RETRY STUDY, primary (temporary diagnostic)\nenvironment: " + PixelDiagnostics.environment(session.chrome().executable()) + "\nseed=" + SEED
                + " matrix=" + MATRIX + " procedure cap=" + MAX_PROCEDURES + "\nplanned: primary " + attempts + " processes, " + attempts * 2 * BLOCK + " navigations, "
                + attempts * 4 * BLOCK + " screenshots; retries for the " + procedures.size() + " procedures that ran: " + procedures.size() * 2 * TablePixelRetryStudy.MAX_RETRIES
                + " processes, " + plannedRetryNavigations + " navigations, " + 2 * plannedRetryNavigations + " screenshots\nactual: " + counts.describe() + "\nduration: " + seconds
                + " s, distinct pictures: " + pictures.size() + "\n";
        String report = header + "\n" + TablePixelRetryStudy.primaryReport(primary, procedures, pictures, beyondCap);
        long bytes = finish(out, report, pictures);
        System.out.println("TABLE-RETRY-STUDY report bytes: " + bytes);
        assertEquals(attempts * BLOCK, primary.size(), "every planned primary pair was captured");
        assertEquals(attempts + procedures.size() * 2 * TablePixelRetryStudy.MAX_RETRIES, counts.processes, "process accounting");
        assertEquals(attempts * 2 * BLOCK + plannedRetryNavigations, counts.navigations, "navigation accounting");
    }
}
