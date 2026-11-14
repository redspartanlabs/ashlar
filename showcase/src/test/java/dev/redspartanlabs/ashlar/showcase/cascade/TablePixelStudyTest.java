package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * TEMPORARY DIAGNOSTIC, not for merge. Runs on CI (the {@code CI} variable is set) or locally when {@code ASHLAR_TABLE_STUDY_DIR} is set; it is skipped otherwise.
 *
 * <p>Repeat study: selected Table cases in three variants, L (legacy markup with the legacy stylesheet), Mm and Mp (migrated markup with master's and with PR #32's built stylesheet,
 * all three frozen as {@code /study/table/*.css.gz} and verified against {@code manifest.sha256}), with {@link TablePixelTest}'s browser flags and capture sequence (navigate, force
 * the state, read the geometry, two screenshots) plus a third screenshot after the state is removed and forced again. A browser process does 4 rounds of the three variants of one
 * case and configuration (12 navigations), the variants in a seeded random order per round. Every case, configuration and repetition runs once in a cold browser (the first page is
 * the first measured page) and once in a warmed one (a neutral page first, then readiness: load event, document.fonts.ready, two animation frames), the two back to back in a seeded
 * random order. Nothing here asserts parity: it records and reports. {@link TablePixelReplayStudyTest} replays TablePixelTest's own browser blocks.
 */
class TablePixelStudyTest {

    static final List<String> CASES = List.of("clear selection hover", "clear selection focus-visible", "selectable, two selected", "sort button hover");
    static final List<String> CONFIGS = List.of("light/foundry", "dark/fathom", "light/sterling", "dark/harbor");
    static final int PROCESSES_PER_CELL = 5;
    static final int ROUNDS = 4;
    static final long SEED = 20261115L;
    static final List<String> CONDITIONS = List.of("cold", "warm");
    /** The neutral warm-up page: no stylesheet, no Table markup, one line of text in the study's font stack. */
    static final String WARMUP_PAGE = "<!doctype html><html><head><meta charset=\"utf-8\"><title>warm-up</title></head><body><p style=\"font-family:Arial,Helvetica,sans-serif\">warm-up</p></body></html>";
    /** Readiness after the load event (which navigate() already waits for): document.fonts.ready has resolved and two animation frames have run. Returns what it saw. */
    static final String READY = "new Promise(function(r){(document.fonts?document.fonts.ready:Promise.resolve()).then(function(){requestAnimationFrame(function(){requestAnimationFrame(function(){"
            + "r(document.readyState+' fonts='+(document.fonts?document.fonts.status:'n/a'))})})})})";
    /** Zoom pictures printed into the report (base64), and the longest one printed. */
    static final int MAX_PRINTED_ZOOMS = 8;
    static final int MAX_ZOOM_CHARS = 60_000;

    // ---- copied verbatim from TablePixelTest (private there), so that the capture is the same

    static String shot(DevToolsSession cdp, String fieldId) throws Exception {
        String box = cdp.evaluate("(function(){var b=document.querySelector('[data-field=\"" + fieldId + "\"]').getBoundingClientRect();return JSON.stringify({x:b.x+scrollX-6,y:b.y+scrollY-6,width:b.width+12,height:b.height+12})})()");
        String response = cdp.call("Page.captureScreenshot", "{\"clip\":" + box.replace("}", ",\"scale\":1}") + "}");
        int at = response.indexOf("\"data\":\"");
        if (at <= 0) {
            throw new IllegalStateException(response);
        }
        return response.substring(at + 8, response.indexOf('"', at + 8));
    }

    static String geometryScript(String fieldId, String selector) {
        return "(function(){var f=document.querySelector('[data-field=\"" + fieldId + "\"]').getBoundingClientRect();var t=document.querySelector(" + DevToolsSession.quote(selector)
                + ");var r=t.getBoundingClientRect();var c=getComputedStyle(t);var reach=c.outlineStyle==='none'?0:Math.max(0,parseFloat(c.outlineWidth)+parseFloat(c.outlineOffset));"
                + "(c.boxShadow.match(/-?[\\d.]+px -?[\\d.]+px -?[\\d.]+px -?[\\d.]+px/g)||[]).forEach(function(s){var n=s.split(' ').map(parseFloat);reach=Math.max(reach,n[3]+n[2]+Math.max(Math.abs(n[0]),Math.abs(n[1])))});"
                + "return [r.x-f.x+6,r.y-f.y+6,r.width,r.height,reach].join(' ')})()";
    }

    // ---- shared by the two studies

    /** Runs on CI, or locally when ASHLAR_TABLE_STUDY_DIR is set; skipped otherwise. */
    static void assumeEnabled() {
        String dir = System.getenv("ASHLAR_TABLE_STUDY_DIR");
        Assumptions.assumeTrue(System.getenv("CI") != null || (dir != null && !dir.isBlank()), "diagnostic study: runs on CI, or locally with ASHLAR_TABLE_STUDY_DIR set");
    }

    /** Where the files go: under ASHLAR_TABLE_STUDY_DIR/out when set, otherwise build/table-pixel-study (not uploaded by CI; the report is also printed). */
    static Path outputDir(String study) throws IOException {
        String dir = System.getenv("ASHLAR_TABLE_STUDY_DIR");
        Path root = dir != null && !dir.isBlank() ? Path.of(dir, "out") : Path.of("build", "table-pixel-study");
        Path out = root.resolve(study + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
        Files.createDirectories(out.resolve("pictures"));
        Files.createDirectories(out.resolve("differences"));
        return out;
    }

    /** The frozen stylesheets, decompressed and checked against the manifest; fails if any does not match. */
    static Map<String, byte[]> frozenInputs() throws Exception {
        Map<String, String> expected = new LinkedHashMap<>();
        try (InputStream in = TablePixelStudyTest.class.getResourceAsStream("/study/table/manifest.sha256")) {
            for (String line : new String(in.readAllBytes(), StandardCharsets.US_ASCII).split("\\R")) {
                String[] parts = line.trim().split("\\s+\\*?");
                if (parts.length == 2) {
                    expected.put(parts[1], parts[0]);
                }
            }
        }
        Map<String, byte[]> inputs = new LinkedHashMap<>();
        for (String v : TablePixelStudy.VARIANTS) {
            byte[] gz;
            try (InputStream in = TablePixelStudyTest.class.getResourceAsStream("/study/table/" + v + ".css.gz")) {
                gz = in.readAllBytes();
            }
            byte[] bytes;
            try (GZIPInputStream unzip = new GZIPInputStream(new ByteArrayInputStream(gz))) {
                bytes = unzip.readAllBytes();
            }
            assertEquals(expected.get(v + ".css"), TablePixelStudy.sha256(bytes), v + ".css does not match its manifest");
            inputs.put(v, bytes);
        }
        return inputs;
    }

    static String fetchHash(HttpClient http, String url) throws Exception {
        HttpResponse<byte[]> r = http.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, r.statusCode(), url);
        return TablePixelStudy.sha256(r.body());
    }

    /**
     * Copies the frozen stylesheets into the generated directory as study-{L,Mm,Mp}.css, checks what the server serves, and records (without asserting) whether this branch's own
     * built legacy stylesheet and app.css are the frozen L and Mm.
     */
    static String serveInputs(ParitySession session, CascadeFixtureServer server, Map<String, byte[]> inputs) throws Exception {
        StringBuilder header = new StringBuilder("Inputs (SHA-256 of the frozen file, checked against the manifest and against the bytes the fixture server serves):\n");
        for (Map.Entry<String, byte[]> e : inputs.entrySet()) {
            Files.write(session.generatedDir().resolve("study-" + e.getKey() + ".css"), e.getValue());
        }
        String probe = server.registerRaw("<!doctype html>");
        String base = probe.substring(0, probe.indexOf("/page/"));
        HttpClient http = HttpClient.newHttpClient();
        for (Map.Entry<String, byte[]> e : inputs.entrySet()) {
            String hash = TablePixelStudy.sha256(e.getValue());
            assertEquals(hash, fetchHash(http, base + "/generated/study-" + e.getKey() + ".css"), e.getKey() + " as served");
            header.append("  ").append(e.getKey()).append(".css ").append(hash).append(" served identical\n");
        }
        String builtLegacy = fetchHash(http, base + "/generated/table-oracle.css");
        String builtApp = fetchHash(http, base + "/css/app.css");
        header.append("  this branch's built table-oracle.css ").append(builtLegacy).append(builtLegacy.equals(TablePixelStudy.sha256(inputs.get("L"))) ? " = L" : " != L (DIFFERS)").append('\n');
        header.append("  this branch's built app.css          ").append(builtApp).append(builtApp.equals(TablePixelStudy.sha256(inputs.get("Mm"))) ? " = Mm" : " != Mm (DIFFERS)").append('\n');
        return header.toString();
    }

    /** Loads the warm-up page and waits for {@link #READY}; returns what the readiness check returned. */
    static String warmUp(DevToolsSession cdp, String url) throws Exception {
        cdp.navigate(url);
        String response = cdp.call("Runtime.evaluate", "{\"expression\":" + DevToolsSession.quote(READY) + ",\"awaitPromise\":true,\"returnByValue\":true}");
        Matcher v = Pattern.compile("\"value\":\"([^\"]*)\"").matcher(response);
        if (!v.find()) {
            throw new IllegalStateException("the warm-up page did not report ready: " + response);
        }
        return v.group(1);
    }

    /** After a process's captures: what it renders with, and the platform fonts used for a data cell's text and for the selection bar's button (when present). */
    static String processEnvironment(DevToolsSession cdp, String fieldId) throws Exception {
        String env = cdp.evaluate("JSON.stringify({ua:navigator.userAgent,dpr:devicePixelRatio,innerWidth:innerWidth,innerHeight:innerHeight})");
        StringBuilder fonts = new StringBuilder();
        for (String selector : List.of("tbody td:nth-child(3)", "[data-table-clear-selection]")) {
            List<Integer> nodes = cdp.nodes("[data-field=\"" + fieldId + "\"] " + selector);
            if (!nodes.isEmpty()) {
                String r = cdp.call("CSS.getPlatformFontsForNode", "{\"nodeId\":" + nodes.get(0) + "}");
                Matcher fm = Pattern.compile("\"fonts\":(\\[[^\\]]*\\])").matcher(r);
                fonts.append(" fonts(").append(selector).append(")=").append(fm.find() ? fm.group(1) : r);
            }
        }
        return env + fonts;
    }

    /** Writes the report and files, prints the report and a bounded number of zoom pictures (base64), and returns the report. */
    static String finish(Path out, String report, List<TablePixelStudy.Capture> captures, TablePixelStudy.Pictures pictures, TablePixelStudy.Measurements m, String prefix)
            throws Exception {
        Files.writeString(out.resolve("report.txt"), report);
        Files.writeString(out.resolve("captures.csv"), TablePixelStudy.csv(captures));
        for (Map.Entry<String, String> p : pictures.all().entrySet()) {
            Files.write(out.resolve("pictures").resolve(TablePixelStudy.id(p.getKey()) + ".png"), Base64.getDecoder().decode(p.getValue()));
        }
        System.out.println(report);
        int printed = 0;
        int rendered = 0;
        for (String[] pair : m.pairs()) {
            PixelDiagnostics.Difference d = m.measure(pair[0], pair[1]);
            if (!d.any() || rendered >= 80) {
                continue;
            }
            rendered++;
            var zoom = PixelDiagnostics.zoom(PixelDiagnostics.decode(pictures.get(pair[0])), PixelDiagnostics.decode(pictures.get(pair[1])), d);
            String name = TablePixelStudy.id(pair[0]) + "-vs-" + TablePixelStudy.id(pair[1]);
            ImageIO.write(zoom, "png", out.resolve("differences").resolve(name + "-zoom.png").toFile());
            String b64 = PixelDiagnostics.png(zoom);
            if (printed < MAX_PRINTED_ZOOMS && b64.length() <= MAX_ZOOM_CHARS) {
                printed++;
                System.out.println(prefix + " zoom " + name + " " + b64);
            }
        }
        System.out.println(prefix + " written to " + out.toAbsolutePath());
        return report;
    }

    // ---- the repeat study

    private static TablePixelStudy.Capture capture(DevToolsSession cdp, String url, TableParityTest.Field f, String config, String variant, int process, int round, int slot,
                                                   TablePixelStudy.Pictures pictures, String condition) throws Exception {
        long t = System.nanoTime();
        cdp.navigate(url);
        long navigateMillis = (System.nanoTime() - t) / 1_000_000L;
        List<String> pseudo = f.spec().pseudo();
        List<Integer> nodes = pseudo.isEmpty() ? List.of() : cdp.nodes(TableBrowser.target(f.spec(), f.id()));
        if (!pseudo.isEmpty()) {
            assertFalse(nodes.isEmpty(), f.spec().key() + ": the target was not found");
            cdp.force(nodes, pseudo);
            cdp.evaluate(geometryScript(f.id(), TableBrowser.target(f.spec(), f.id())));
        }
        String first = shot(cdp, f.id());
        String second = shot(cdp, f.id());
        if (!pseudo.isEmpty()) {
            cdp.force(nodes, List.of());
            cdp.force(nodes, pseudo);
        }
        String third = shot(cdp, f.id());
        if (!pseudo.isEmpty()) {
            cdp.force(nodes, List.of());
        }
        return new TablePixelStudy.Capture(f.spec().key(), f.id(), !pseudo.isEmpty(), config, variant, process, round, slot, pictures.add(first), pictures.add(second),
                pictures.add(third), condition, navigateMillis);
    }

    @Test
    void study() throws Exception {
        assumeEnabled();
        Path out = outputDir("repeat");
        Map<String, byte[]> inputs = frozenInputs();
        ParitySession session = ParitySession.open("table-pixel-study");
        List<TableParityTest.Field> fields = TableParityTest.fields();
        List<TablePixelStudy.Capture> captures = new ArrayList<>();
        TablePixelStudy.Pictures pictures = new TablePixelStudy.Pictures();
        Set<String> processEnvironments = new LinkedHashSet<>();
        List<TablePixelStudy.Process> processRecords = new ArrayList<>();
        String inputHeader;
        long began = System.nanoTime();
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            inputHeader = serveInputs(session, server, inputs);
            // Every block (case, configuration, repetition) runs once in each condition, the two back to back in a seeded random order; the blocks in a seeded random order.
            List<int[]> cells = new ArrayList<>();
            for (int ci = 0; ci < CONFIGS.size(); ci++) {
                for (int ki = 0; ki < CASES.size(); ki++) {
                    for (int b = 0; b < PROCESSES_PER_CELL; b++) {
                        cells.add(new int[] {ci, ki, b});
                    }
                }
            }
            Collections.shuffle(cells, new Random(SEED));
            Random coin = new Random(SEED + 7);
            List<Object[]> blocks = new ArrayList<>();
            for (int[] b : cells) {
                List<String> pair = coin.nextBoolean() ? CONDITIONS : List.of(CONDITIONS.get(1), CONDITIONS.get(0));
                pair.forEach(cond -> blocks.add(new Object[] {b, cond}));
            }
            String warmupUrl = server.registerRaw(WARMUP_PAGE);
            int process = 0;
            for (Object[] run : blocks) {
                int[] block = (int[]) run[0];
                String condition = (String) run[1];
                process++;
                ParityConfigs.Config config = ParityConfigs.get(CONFIGS.get(block[0]));
                String caseKey = CASES.get(block[1]);
                TableParityTest.Field f = fields.stream().filter(x -> x.spec().key().equals(caseKey)).findFirst().orElseThrow();
                Map<String, String> urls = new LinkedHashMap<>();
                for (String v : TablePixelStudy.VARIANTS) {
                    String body = v.equals("L") ? new IconLegacyMarkup().rewrite(new TableLegacyMarkup().rewrite(f.markup())) : f.markup();
                    urls.put(v, server.registerRaw(TextEntryBrowser.page(List.of("/generated/study-" + v + ".css"), config, "", body, TableBrowser.INDETERMINATE)));
                }
                Random order = new Random(SEED * 31 + process);
                long launchStart = System.nanoTime();
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    long launchMillis = (System.nanoTime() - launchStart) / 1_000_000L;
                    long warmupMillis = 0;
                    String ready = "-";
                    if (condition.equals("warm")) {
                        long w = System.nanoTime();
                        ready = warmUp(cdp, warmupUrl);
                        warmupMillis = (System.nanoTime() - w) / 1_000_000L;
                    }
                    processRecords.add(new TablePixelStudy.Process(process, condition, caseKey, config.id(), launchMillis, warmupMillis, ready));
                    int slot = 0;
                    for (int round = 0; round < ROUNDS; round++) {
                        List<String> variants = new ArrayList<>(TablePixelStudy.VARIANTS);
                        Collections.shuffle(variants, order);
                        for (String v : variants) {
                            captures.add(capture(cdp, urls.get(v), f, config.id(), v, process, round, ++slot, pictures, condition));
                        }
                    }
                    processEnvironments.add(processEnvironment(cdp, f.id()));
                }
            }
        }
        long seconds = (System.nanoTime() - began) / 1_000_000_000L;
        String header = "TABLE PIXEL STUDY, repeat (temporary diagnostic)\nenvironment: " + PixelDiagnostics.environment(session.chrome().executable()) + "\n"
                + "seed=" + SEED + " conditions=" + CONDITIONS + " cases=" + CASES + " configs=" + CONFIGS + " processes per case, configuration and condition=" + PROCESSES_PER_CELL
                + " rounds per process=" + ROUNDS + " (each round: L, Mm, Mp in a seeded random order)\nwarm-up page: " + WARMUP_PAGE + "\nwarm-up readiness: load event, then " + READY
                + "\nduration: " + seconds + " s\nwhat the browser processes reported (distinct values): " + processEnvironments + "\n" + inputHeader;
        TablePixelStudy.Measurements m = new TablePixelStudy.Measurements(pictures);
        StringBuilder body = new StringBuilder(TablePixelStudy.conditions(captures, processRecords, m));
        for (String condition : CONDITIONS) {
            List<TablePixelStudy.Capture> cc = captures.stream().filter(c -> c.condition().equals(condition)).toList();
            body.append("\n\n######## CONDITION ").append(condition).append(" ########\n").append(TablePixelStudy.report(cc, pictures, m));
        }
        finish(out, header + "\n" + body, captures, pictures, m, "TABLE-STUDY-REPEAT");
        assertEquals(CONDITIONS.size() * CONFIGS.size() * CASES.size() * PROCESSES_PER_CELL * ROUNDS * TablePixelStudy.VARIANTS.size(), captures.size(), "every planned navigation was made");
    }
}
