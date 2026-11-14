package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * TEMPORARY LOCAL DIAGNOSTIC, not for merge. Runs only when {@code ASHLAR_TABLE_STUDY_DIR} names a directory holding {@code inputs/} (the three frozen stylesheets and their
 * {@code manifest.sha256}); everything it writes goes under {@code out/} there.
 *
 * <p>Captures selected Table cases in three variants, L (legacy markup with the legacy stylesheet), Mm and Mp (migrated markup with master's and with PR #32's built stylesheet),
 * with {@link TablePixelTest}'s own browser flags and capture sequence (navigate, force the state, read the geometry, two screenshots), plus a third screenshot after the state is
 * removed and forced again. A browser process does 4 rounds of the three variants of one case and configuration (12 navigations, 36 screenshots, below the 48 TablePixelTest takes
 * in one process), the variants in a seeded random order per round. Nothing here asserts parity: it records and reports.
 */
class TablePixelStudyTest {

    static final List<String> CASES = List.of("sort button hover", "clear selection hover", "sort button focus-visible", "clear selection focus-visible",
            "selectable, two selected", "sortable, none sorted");
    static final List<String> CONFIGS = List.of("light/foundry", "dark/fathom", "light/sterling", "dark/harbor");
    static final int PROCESSES_PER_CELL = 5;
    static final int ROUNDS = 4;
    static final long SEED = 20261113L;

    // ---- copied verbatim from TablePixelTest (private there), so that the capture is the same

    private static String shot(DevToolsSession cdp, String fieldId) throws Exception {
        String box = cdp.evaluate("(function(){var b=document.querySelector('[data-field=\"" + fieldId + "\"]').getBoundingClientRect();return JSON.stringify({x:b.x+scrollX-6,y:b.y+scrollY-6,width:b.width+12,height:b.height+12})})()");
        String response = cdp.call("Page.captureScreenshot", "{\"clip\":" + box.replace("}", ",\"scale\":1}") + "}");
        int at = response.indexOf("\"data\":\"");
        if (at <= 0) {
            throw new IllegalStateException(response);
        }
        return response.substring(at + 8, response.indexOf('"', at + 8));
    }

    private static String geometryScript(String fieldId, String selector) {
        return "(function(){var f=document.querySelector('[data-field=\"" + fieldId + "\"]').getBoundingClientRect();var t=document.querySelector(" + DevToolsSession.quote(selector)
                + ");var r=t.getBoundingClientRect();var c=getComputedStyle(t);var reach=c.outlineStyle==='none'?0:Math.max(0,parseFloat(c.outlineWidth)+parseFloat(c.outlineOffset));"
                + "(c.boxShadow.match(/-?[\\d.]+px -?[\\d.]+px -?[\\d.]+px -?[\\d.]+px/g)||[]).forEach(function(s){var n=s.split(' ').map(parseFloat);reach=Math.max(reach,n[3]+n[2]+Math.max(Math.abs(n[0]),Math.abs(n[1])))});"
                + "return [r.x-f.x+6,r.y-f.y+6,r.width,r.height,reach].join(' ')})()";
    }

    // ----

    private static TablePixelStudy.Capture capture(DevToolsSession cdp, String url, TableParityTest.Field f, String config, String variant, int process, int round, int slot,
                                                   TablePixelStudy.Pictures pictures) throws Exception {
        cdp.navigate(url);
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
                pictures.add(third));
    }

    private static Map<String, String> manifest(Path file) throws Exception {
        Map<String, String> m = new LinkedHashMap<>();
        for (String line : Files.readAllLines(file)) {
            String[] parts = line.trim().split("\\s+\\*?");
            if (parts.length == 2) {
                m.put(parts[1], parts[0]);
            }
        }
        return m;
    }

    private static String fetchHash(HttpClient http, String url) throws Exception {
        HttpResponse<byte[]> r = http.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, r.statusCode(), url);
        return TablePixelStudy.sha256(r.body());
    }

    @Test
    void study() throws Exception {
        String dir = System.getenv("ASHLAR_TABLE_STUDY_DIR");
        Assumptions.assumeTrue(dir != null && !dir.isBlank(), "local diagnostic study: set ASHLAR_TABLE_STUDY_DIR to run it");
        Path inputs = Path.of(dir, "inputs");
        Path out = Path.of(dir, "out", "run-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
        Files.createDirectories(out.resolve("pictures"));
        Files.createDirectories(out.resolve("differences"));
        StringBuilder header = new StringBuilder();

        // ---- the inputs: frozen, hashed, and verified as served
        Map<String, String> expected = manifest(inputs.resolve("manifest.sha256"));
        ParitySession session = ParitySession.open("table-pixel-study");
        for (String v : TablePixelStudy.VARIANTS) {
            byte[] bytes = Files.readAllBytes(inputs.resolve(v + ".css"));
            assertEquals(expected.get(v + ".css"), TablePixelStudy.sha256(bytes), v + ".css does not match its manifest");
            Files.write(session.generatedDir().resolve("study-" + v + ".css"), bytes);
        }
        List<TableParityTest.Field> fields = TableParityTest.fields();
        List<TablePixelStudy.Capture> captures = new ArrayList<>();
        TablePixelStudy.Pictures pictures = new TablePixelStudy.Pictures();
        Set<String> processEnvironments = new LinkedHashSet<>();
        long began = System.nanoTime();
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            String probe = server.registerRaw("<!doctype html>");
            String base = probe.substring(0, probe.indexOf("/page/"));
            HttpClient http = HttpClient.newHttpClient();
            header.append("Inputs (SHA-256 of the file, and of the bytes the fixture server serves):\n");
            for (String v : TablePixelStudy.VARIANTS) {
                String served = fetchHash(http, base + "/generated/study-" + v + ".css");
                assertEquals(expected.get(v + ".css"), served, v + " as served");
                header.append("  ").append(v).append(".css ").append(expected.get(v + ".css")).append(" served identical\n");
            }
            // This branch is master-based: its own built legacy and migrated stylesheets must be the frozen L and Mm.
            String builtLegacy = fetchHash(http, base + "/generated/table-oracle.css");
            String builtApp = fetchHash(http, base + "/css/app.css");
            header.append("  this branch's built table-oracle.css ").append(builtLegacy).append(builtLegacy.equals(expected.get("L.css")) ? " = L" : " != L").append('\n');
            header.append("  this branch's built app.css          ").append(builtApp).append(builtApp.equals(expected.get("Mm.css")) ? " = Mm" : " != Mm").append('\n');
            assertEquals(expected.get("L.css"), builtLegacy, "the frozen L is not this branch's legacy stylesheet");
            assertEquals(expected.get("Mm.css"), builtApp, "the frozen Mm is not this branch's app.css");

            // ---- the schedule: every block is one browser process; blocks in a seeded random order
            List<int[]> blocks = new ArrayList<>();
            for (int ci = 0; ci < CONFIGS.size(); ci++) {
                for (int ki = 0; ki < CASES.size(); ki++) {
                    for (int b = 0; b < PROCESSES_PER_CELL; b++) {
                        blocks.add(new int[] {ci, ki, b});
                    }
                }
            }
            Collections.shuffle(blocks, new Random(SEED));
            int process = 0;
            for (int[] block : blocks) {
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
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    int slot = 0;
                    for (int round = 0; round < ROUNDS; round++) {
                        List<String> variants = new ArrayList<>(TablePixelStudy.VARIANTS);
                        Collections.shuffle(variants, order);
                        for (String v : variants) {
                            captures.add(capture(cdp, urls.get(v), f, config.id(), v, process, round, ++slot, pictures));
                        }
                    }
                    // After the captures, so that they are not affected: what this process renders with.
                    String env = cdp.evaluate("JSON.stringify({ua:navigator.userAgent,dpr:devicePixelRatio,innerWidth:innerWidth,innerHeight:innerHeight})");
                    List<Integer> th = cdp.nodes("[data-field=\"" + f.id() + "\"] th");
                    String fonts = th.isEmpty() ? "-" : cdp.call("CSS.getPlatformFontsForNode", "{\"nodeId\":" + th.get(0) + "}");
                    Matcher fm = Pattern.compile("\"fonts\":(\\[[^\\]]*\\])").matcher(fonts);
                    processEnvironments.add(env + " fonts(th)=" + (fm.find() ? fm.group(1) : fonts));
                }
            }
        }
        long seconds = (System.nanoTime() - began) / 1_000_000_000L;

        // ---- the report
        header.insert(0, "TABLE PIXEL STUDY (temporary local diagnostic)\nenvironment: " + PixelDiagnostics.environment(session.chrome().executable()) + "\n"
                + "seed=" + SEED + " cases=" + CASES + " configs=" + CONFIGS + " processes per case and configuration=" + PROCESSES_PER_CELL + " rounds per process=" + ROUNDS
                + " (each round: L, Mm, Mp in a seeded random order)\nduration: " + seconds + " s\n"
                + "what the browser processes reported (distinct values): " + processEnvironments + "\n");
        TablePixelStudy.Measurements m = new TablePixelStudy.Measurements(pictures);
        String report = header + "\n" + TablePixelStudy.report(captures, pictures, m);
        Files.writeString(out.resolve("report.txt"), report);
        Files.writeString(out.resolve("captures.csv"), TablePixelStudy.csv(captures));
        for (Map.Entry<String, String> p : pictures.all().entrySet()) {
            Files.write(out.resolve("pictures").resolve(TablePixelStudy.id(p.getKey()) + ".png"), Base64.getDecoder().decode(p.getValue()));
        }
        int rendered = 0;
        for (String[] pair : m.pairs()) {
            PixelDiagnostics.Difference d = m.measure(pair[0], pair[1]);
            if (d.any() && rendered < 80) {
                rendered++;
                var a = PixelDiagnostics.decode(pictures.get(pair[0]));
                var b = PixelDiagnostics.decode(pictures.get(pair[1]));
                ImageIO.write(PixelDiagnostics.zoom(a, b, d), "png", out.resolve("differences").resolve(TablePixelStudy.id(pair[0]) + "-vs-" + TablePixelStudy.id(pair[1]) + "-zoom.png").toFile());
            }
        }
        System.out.println(report);
        System.out.println("TABLE PIXEL STUDY written to " + out.toAbsolutePath());
        assertEquals(CONFIGS.size() * CASES.size() * PROCESSES_PER_CELL * ROUNDS * TablePixelStudy.VARIANTS.size(), captures.size(), "every planned navigation was made");
    }
}
