package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * TEMPORARY DIAGNOSTIC, not for merge. Runs on CI, or locally with {@code ASHLAR_TABLE_STUDY_DIR} set (see {@link TablePixelStudyTest#assumeEnabled()}).
 *
 * <p>Replay study: a browser process replays one of {@link TablePixelTest}'s own browser blocks exactly as TablePixelTest runs it: the block's 12 cases in their order, each the
 * legacy page and then the migrated page, each captured with TablePixelTest's sequence (navigate, force the state, read the geometry, two screenshots, remove the state), so that
 * every case is at the navigation position and after the forced states it has there. Block 0 holds "sort button hover" (navigations 11-12); block 1 opens with the unforced
 * selection bar ("selectable, two selected", navigations 1-2) and holds "clear selection focus-visible" (13-14) and "clear selection hover" (15-16).
 *
 * <p>The legacy page uses this branch's built {@code /generated/table-oracle.css} and the migrated page either this branch's built {@code /css/app.css} (Mm: exactly the URL and
 * bytes TablePixelTest uses on master) or the frozen PR #32 stylesheet at {@code /generated/study-Mp.css} (Mp). Each block runs cold and warmed, as in the repeat study. Each
 * legacy/migrated pair is also judged by {@link PixelPolicy} as TablePixelTest would judge it (read only). Differences from TablePixelTest itself: a block runs in its own JVM
 * position (after whatever ran before this class, not after TablePixelTest's earlier configurations), and no third screenshot is taken.
 */
class TablePixelReplayStudyTest {

    static final List<Integer> BLOCKS = List.of(0, 1);
    static final int BLOCK = 12;
    static final List<String> CONFIGS = List.of("light/foundry", "dark/fathom", "light/sterling", "dark/harbor");
    static final List<String> MIGRATED = List.of("Mm", "Mp");
    static final int REPETITIONS = 4;
    static final long SEED = 20261116L;

    private record Shot(String png, String geometry) {
    }

    @Test
    void replay() throws Exception {
        TablePixelStudyTest.assumeEnabled();
        Path out = TablePixelStudyTest.outputDir("replay");
        Map<String, byte[]> inputs = TablePixelStudyTest.frozenInputs();
        ParitySession session = ParitySession.open("table-pixel-replay-study");
        List<TableParityTest.Field> fields = TableParityTest.fields();
        List<TablePixelStudy.Capture> captures = new ArrayList<>();
        TablePixelStudy.Pictures pictures = new TablePixelStudy.Pictures();
        Set<String> processEnvironments = new LinkedHashSet<>();
        List<TablePixelStudy.Process> processRecords = new ArrayList<>();
        List<String> verdicts = new ArrayList<>();
        Map<String, int[]> verdictCounts = new LinkedHashMap<>();
        String inputHeader;
        long began = System.nanoTime();
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            inputHeader = TablePixelStudyTest.serveInputs(session, server, inputs);
            List<int[]> cells = new ArrayList<>();
            for (int bi = 0; bi < BLOCKS.size(); bi++) {
                for (int ci = 0; ci < CONFIGS.size(); ci++) {
                    for (int mi = 0; mi < MIGRATED.size(); mi++) {
                        for (int r = 0; r < REPETITIONS; r++) {
                            cells.add(new int[] {bi, ci, mi, r});
                        }
                    }
                }
            }
            Collections.shuffle(cells, new Random(SEED));
            Random coin = new Random(SEED + 7);
            List<Object[]> runs = new ArrayList<>();
            for (int[] c : cells) {
                List<String> pair = coin.nextBoolean() ? TablePixelStudyTest.CONDITIONS : List.of(TablePixelStudyTest.CONDITIONS.get(1), TablePixelStudyTest.CONDITIONS.get(0));
                pair.forEach(cond -> runs.add(new Object[] {c, cond}));
            }
            String warmupUrl = server.registerRaw(TablePixelStudyTest.WARMUP_PAGE);
            int process = 0;
            for (Object[] run : runs) {
                int[] cell = (int[]) run[0];
                String condition = (String) run[1];
                process++;
                int block = BLOCKS.get(cell[0]);
                ParityConfigs.Config config = ParityConfigs.get(CONFIGS.get(cell[1]));
                String migrated = MIGRATED.get(cell[2]);
                List<String> legacySheets = List.of(ParityFrames.generated("table-oracle.css"));
                List<String> migratedSheets = migrated.equals("Mm") ? List.of(ParityFrames.APP) : List.of("/generated/study-Mp.css");
                List<TableParityTest.Field> chunk = fields.subList(block * BLOCK, Math.min(fields.size(), block * BLOCK + BLOCK));
                long launchStart = System.nanoTime();
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    long launchMillis = (System.nanoTime() - launchStart) / 1_000_000L;
                    long warmupMillis = 0;
                    String ready = "-";
                    if (condition.equals("warm")) {
                        long w = System.nanoTime();
                        ready = TablePixelStudyTest.warmUp(cdp, warmupUrl);
                        warmupMillis = (System.nanoTime() - w) / 1_000_000L;
                    }
                    processRecords.add(new TablePixelStudy.Process(process, condition, "block " + block + " " + migrated, config.id(), launchMillis, warmupMillis, ready));
                    int slot = 0;
                    for (TableParityTest.Field f : chunk) {
                        String migratedBody = f.markup();
                        String legacyBody = new IconLegacyMarkup().rewrite(new TableLegacyMarkup().rewrite(migratedBody));
                        String legacyUrl = server.registerRaw(TextEntryBrowser.page(legacySheets, config, "", legacyBody, TableBrowser.INDETERMINATE));
                        String migratedUrl = server.registerRaw(TextEntryBrowser.page(migratedSheets, config, "", migratedBody, TableBrowser.INDETERMINATE));
                        Shot legacy = picture(cdp, legacyUrl, f, config.id(), "L", process, ++slot, pictures, condition, captures);
                        Shot candidate = picture(cdp, migratedUrl, f, config.id(), migrated, process, ++slot, pictures, condition, captures);
                        // How TablePixelTest would judge this pair (read only; PixelPolicy is not changed).
                        PixelPolicy.Verdict v = PixelPolicy.compare(legacy.png(), candidate.png(), PixelPolicy.forCase(f.spec().key()), PixelPolicy.Geometry.parse(legacy.geometry()));
                        String key = condition + " L vs " + migrated;
                        int[] counts = verdictCounts.computeIfAbsent(key, k -> new int[3]);
                        counts[v.exact() ? 0 : v.accepted() ? 1 : 2]++;
                        if (!v.exact()) {
                            verdicts.add(String.format("    %s, process %d, slots %d-%d, %s %s (%s): %s", key, process, slot - 1, slot, config.id(), f.id(), f.spec().key(), v.describe()));
                        }
                    }
                    processEnvironments.add(TablePixelStudyTest.processEnvironment(cdp, chunk.get(chunk.size() - 1).id())); // the page left open holds the block's last case
                }
            }
        }
        long seconds = (System.nanoTime() - began) / 1_000_000_000L;
        StringBuilder policy = new StringBuilder("== Each legacy/migrated pair judged as TablePixelTest judges it (PixelPolicy, read only): identical / tolerated / rejected ==\n");
        verdictCounts.forEach((k, c) -> policy.append(String.format("  %s: %d identical, %d tolerated, %d rejected%n", k, c[0], c[1], c[2])));
        verdicts.forEach(v -> policy.append(v).append('\n'));
        String header = "TABLE PIXEL STUDY, replay of TablePixelTest's browser blocks (temporary diagnostic)\nenvironment: " + PixelDiagnostics.environment(session.chrome().executable())
                + "\nseed=" + SEED + " conditions=" + TablePixelStudyTest.CONDITIONS + " blocks=" + BLOCKS + " (12 cases each, legacy then migrated, as TablePixelTest) configs=" + CONFIGS
                + " migrated=" + MIGRATED + " repetitions per block, configuration, migrated stylesheet and condition=" + REPETITIONS
                + "\nslots: each case's legacy page is navigation 2i-1 and its migrated page 2i of the block; no third screenshot is taken (the 3rd-shot columns below are always 0)"
                + "\nduration: " + seconds + " s\nwhat the browser processes reported (distinct values): " + processEnvironments + "\n" + inputHeader;
        TablePixelStudy.Measurements m = new TablePixelStudy.Measurements(pictures);
        StringBuilder body = new StringBuilder(policy).append('\n').append(TablePixelStudy.conditions(captures, processRecords, m));
        for (String condition : TablePixelStudyTest.CONDITIONS) {
            List<TablePixelStudy.Capture> cc = captures.stream().filter(c -> c.condition().equals(condition)).toList();
            body.append("\n\n######## CONDITION ").append(condition).append(" ########\n").append(TablePixelStudy.report(cc, pictures, m));
        }
        TablePixelStudyTest.finish(out, header + "\n" + body, captures, pictures, m, "TABLE-STUDY-REPLAY");
        assertEquals(BLOCKS.size() * CONFIGS.size() * MIGRATED.size() * REPETITIONS * TablePixelStudyTest.CONDITIONS.size() * BLOCK * 2, captures.size(), "every planned navigation was made");
    }

    /** TablePixelTest's picture(): navigate, force the state, read the geometry, two screenshots, remove the state. Records instead of asserting that the two screenshots agree. */
    private static Shot picture(DevToolsSession cdp, String url, TableParityTest.Field f, String config, String variant, int process, int slot, TablePixelStudy.Pictures pictures,
                                String condition, List<TablePixelStudy.Capture> captures) throws Exception {
        long t = System.nanoTime();
        cdp.navigate(url);
        long navigateMillis = (System.nanoTime() - t) / 1_000_000L;
        List<String> pseudo = f.spec().pseudo();
        List<Integer> nodes = pseudo.isEmpty() ? List.of() : cdp.nodes(TableBrowser.target(f.spec(), f.id()));
        String geometry = null;
        if (!pseudo.isEmpty()) {
            assertFalse(nodes.isEmpty(), f.spec().key() + ": the target was not found");
            cdp.force(nodes, pseudo);
            geometry = cdp.evaluate(TablePixelStudyTest.geometryScript(f.id(), TableBrowser.target(f.spec(), f.id())));
        }
        String first = TablePixelStudyTest.shot(cdp, f.id());
        String second = TablePixelStudyTest.shot(cdp, f.id());
        if (!pseudo.isEmpty()) {
            cdp.force(nodes, List.of());
        }
        String a = pictures.add(first);
        captures.add(new TablePixelStudy.Capture(f.spec().key(), f.id(), !pseudo.isEmpty(), config, variant, process, 0, slot, a, pictures.add(second), a, condition, navigateMillis));
        return new Shot(first, geometry == null ? "" : geometry);
    }
}
