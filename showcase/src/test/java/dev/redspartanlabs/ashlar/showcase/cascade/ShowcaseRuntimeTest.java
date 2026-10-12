package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase C, runtime half: the REAL packaged showcase (the Spring Boot jar, run as
 * a separate process) loaded in a REAL Chrome, once with the application's own
 * integrated stylesheet and once with a baseline stylesheet - the same Tailwind
 * entry compiled by the same CLI but without Ashlar's stylesheet. No component
 * uses Ashlar CSS yet, so loading it must not change any computed style.
 *
 * <p>Observed behaviour (computed styles, CSSOM, real clicks) is kept apart from
 * the one SIMULATED check (the Tailwind {@code properties} fallback, see the
 * probe). Controls prove the comparison can fail: the baseline compared with a
 * second baseline load (noise floor) and with a deliberately mutated stylesheet.
 */
class ShowcaseRuntimeTest {

    private static final String EXPECTED_ORDER = "properties;theme;base;ash;ash.tokens;ash.base;ash.layout;ash.components;ash.skin;ash.utilities;components;utilities";
    private static final String BASELINE_ORDER = "properties;theme;base;components;utilities";
    private static final List<String> SKINS = List.of("axiom", "civic", "fathom", "foundry", "harbor", "keystone", "ledger", "sterling", "summit", "veridian");

    record Config(String id, String theme, String skin) {
    }

    record Snapshot(Map<String, String> meta, List<String> descriptors, List<String> values) {
    }

    private static final Map<String, Snapshot> BASELINE = new LinkedHashMap<>();
    private static final Map<String, Snapshot> INTEGRATED = new LinkedHashMap<>();
    private static final Map<String, Snapshot> BASELINE_AGAIN = new LinkedHashMap<>();
    private static final Map<String, Snapshot> MUTATED = new LinkedHashMap<>();
    private static Snapshot interactive;
    private static String browser = "";

    private static List<String> pageSlugs() {
        return List.of("button", "table", "select", "tabs");
    }

    private static List<Config> configsFor(String slug) {
        List<Config> configs = new ArrayList<>();
        configs.add(new Config("default-light", "light", null));
        configs.add(new Config("default-dark", "dark", null));
        for (String skin : SKINS) {
            configs.add(new Config(skin + "-light", "light", skin));
        }
        if (slug.equals("button")) {
            for (String skin : SKINS) {
                configs.add(new Config(skin + "-dark", "dark", skin));
            }
        }
        return configs;
    }

    @BeforeAll
    static void measure() throws Exception {
        String generatedDir = System.getProperty("ashlar.cascade.dir");
        String bootJar = System.getProperty("ashlar.showcase.jar");
        if (generatedDir == null || bootJar == null) {
            throw new IllegalStateException("ashlar.cascade.dir and ashlar.showcase.jar must be set; run through Gradle (:showcase:test)");
        }
        var chromePath = HeadlessChrome.find();
        if (chromePath.isEmpty()) {
            if (System.getenv("CI") != null || "true".equalsIgnoreCase(System.getenv("ASHLAR_REQUIRE_BROWSER"))) {
                throw new IllegalStateException("Chrome is required (CI or ASHLAR_REQUIRE_BROWSER set) but was not found.");
            }
            System.out.println("[phase-c] Chrome not found - runtime scenarios SKIPPED (not passed).");
            Assumptions.abort("Chrome not found; the real-browser runtime checks were not run");
        }
        HeadlessChrome chrome = new HeadlessChrome(chromePath.get());
        byte[] baselineCss = Files.readAllBytes(Path.of(generatedDir, "phase-c-baseline.css"));
        byte[] mutatedCss = (new String(baselineCss, StandardCharsets.UTF_8) + "\nbutton{padding-left:7px !important}\n").getBytes(StandardCharsets.UTF_8);

        try (RunningShowcase app = RunningShowcase.start(Path.of(bootJar)); RealPageProxy proxy = new RealPageProxy(app.port())) {
            for (String slug : pageSlugs()) {
                for (Config config : configsFor(slug)) {
                    String key = slug + " | " + config.id();
                    BASELINE.put(key, load(chrome, proxy, slug, config, "snapshot", baselineCss));
                    INTEGRATED.put(key, load(chrome, proxy, slug, config, "snapshot", null));
                }
            }
            // Controls.
            for (Config config : List.of(new Config("default-light", "light", null), new Config("axiom-light", "light", "axiom"), new Config("default-dark", "dark", null))) {
                String key = "button | " + config.id();
                BASELINE_AGAIN.put(key, load(chrome, proxy, "button", config, "snapshot", baselineCss));
                MUTATED.put(key, load(chrome, proxy, "button", config, "snapshot", mutatedCss));
            }
            interactive = load(chrome, proxy, "button", new Config("default-light", "light", null), "interactive", null);
            browser = chrome.executable() + " | " + INTEGRATED.values().iterator().next().meta().getOrDefault("user-agent", "");
        }
        writeReport();
    }

    private static Snapshot load(HeadlessChrome chrome, RealPageProxy proxy, String slug, Config config, String mode, byte[] cssOverride) throws Exception {
        proxy.configure(new RealPageProxy.Load(config.theme(), mode, cssOverride));
        String url = proxy.url("/ashlar/components/" + slug + (config.skin() == null ? "" : "?skin=" + config.skin()));
        // Scrollbars are hidden so that a horizontal scrollbar appearing or not (a 15px
        // height difference seen between two loads of the SAME baseline) cannot add
        // layout noise. Consequence: a difference that only a classic scrollbar would
        // reveal is not detectable here.
        String dom = chrome.dumpDom(url, List.of("--hide-scrollbars"));
        Matcher m = Pattern.compile("<pre id=\"ash-real-results\">(.*?)</pre>", Pattern.DOTALL).matcher(dom);
        String found = null;
        while (m.find()) {
            found = m.group(1); // the element the probe appended is the last one in the document
        }
        if (found == null || found.isBlank()) {
            throw new IllegalStateException("no probe results for " + slug + " " + config + " - infrastructure failure, not a pass");
        }
        String text = found.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&amp;", "&");
        Map<String, String> meta = new LinkedHashMap<>();
        List<String> descriptors = new ArrayList<>();
        List<String> values = new ArrayList<>();
        for (String line : text.split("\\R")) {
            String[] f = line.split("\t", -1);
            if (f[0].equals("meta") && f.length >= 3) {
                meta.put(f[1], f[2]);
            } else if (f[0].equals("el") && f.length == 4) {
                descriptors.add(f[2]);
                values.add(f[3]);
            }
        }
        if (!meta.containsKey("props") || (mode.equals("snapshot") && values.isEmpty())) {
            throw new IllegalStateException("incomplete probe output for " + slug + " " + config + " (" + mode + ") - infrastructure failure, not a pass. First 600 chars:\n"
                    + text.substring(0, Math.min(600, text.length())));
        }
        return new Snapshot(meta, descriptors, values);
    }

    /** One entry per element and property whose computed value differs between the two snapshots. */
    private static List<String> diff(String key, Snapshot a, Snapshot b) {
        List<String> diffs = new ArrayList<>();
        List<String> props = List.of(a.meta().get("props").split(","));
        if (a.values().size() != b.values().size()) {
            diffs.add(key + ": element count " + a.values().size() + " vs " + b.values().size());
            return diffs;
        }
        for (int i = 0; i < a.values().size(); i++) {
            if (!a.values().get(i).equals(b.values().get(i))) {
                String[] x = a.values().get(i).split("\u001f", -1);
                String[] y = b.values().get(i).split("\u001f", -1);
                for (int p = 0; p < props.size(); p++) {
                    if (!x[p].equals(y[p])) {
                        diffs.add(key + " #" + i + " " + a.descriptors().get(i) + " " + props.get(p) + ": " + x[p] + " -> " + y[p]);
                    }
                }
            }
        }
        return diffs;
    }

    private static void writeReport() throws IOException {
        long comparisons = INTEGRATED.values().stream().mapToLong(s -> s.values().size()).sum();
        StringBuilder sb = new StringBuilder();
        sb.append("Phase C runtime verification. Browser: ").append(browser).append('\n');
        sb.append("Real boot jar + real Chrome; baseline = same Tailwind entry without Ashlar's stylesheet.\n\n");
        sb.append("Page/config loads per variant: ").append(INTEGRATED.size())
                .append(" ; elements compared: ").append(comparisons)
                .append(" ; properties compared per element: ").append(INTEGRATED.values().iterator().next().meta().get("props").split(",").length).append('\n');
        List<String> diffs = new ArrayList<>();
        INTEGRATED.forEach((k, v) -> diffs.addAll(diff(k, BASELINE.get(k), v)));
        sb.append("OBSERVED  computed-style differences baseline -> integrated: ").append(diffs.size()).append('\n');
        diffs.stream().limit(40).forEach(d -> sb.append("   ").append(d).append('\n'));
        sb.append("OBSERVED  layer order integrated: ").append(INTEGRATED.values().iterator().next().meta().get("layer-order")).append('\n');
        sb.append("OBSERVED  layer order baseline  : ").append(BASELINE.values().iterator().next().meta().get("layer-order")).append('\n');
        long gateTrue = INTEGRATED.values().stream().filter(s -> "true".equals(s.meta().get("properties-gate-true-in-this-browser"))).count();
        sb.append("OBSERVED  @supports gate true in this browser on ").append(gateTrue).append(" of ").append(INTEGRATED.size()).append(" loads\n");
        long simChanged = INTEGRATED.values().stream().mapToLong(s -> Long.parseLong(s.meta().getOrDefault("properties-simulation-changed-elements", "-1"))).sum();
        sb.append("SIMULATED properties fallback injected ungated: elements whose computed style changed (sum over loads): ").append(simChanged)
                .append("  [simulation only; says nothing about browsers where the gate is true]\n");
        sb.append("CONTROL   baseline vs second baseline load differences: ")
                .append(BASELINE_AGAIN.entrySet().stream().mapToInt(e -> diff(e.getKey(), BASELINE.get(e.getKey()), e.getValue()).size()).sum()).append('\n');
        sb.append("CONTROL   baseline vs mutated stylesheet differences (must be > 0): ")
                .append(MUTATED.entrySet().stream().mapToInt(e -> diff(e.getKey(), BASELINE.get(e.getKey()), e.getValue()).size()).sum()).append('\n');
        sb.append("OBSERVED  interactive (real clicks / real Select change event on the integrated page):\n");
        interactive.meta().forEach((k, v) -> {
            if (!k.equals("props")) {
                sb.append("   ").append(k).append(" = ").append(v).append('\n');
            }
        });
        Path out = Path.of(System.getProperty("ashlar.cascade.dir")).resolve("../../reports/cascade/phase-c.txt").normalize();
        Files.createDirectories(out.getParent());
        Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
        System.out.println(sb);
    }

    @Test
    void loadingAshlarsStylesheetChangesNoComputedStyleOnAnyRealPageOrState() {
        List<String> all = new ArrayList<>();
        INTEGRATED.forEach((key, snapshot) -> all.addAll(diff(key, BASELINE.get(key), snapshot)));
        assertEquals(List.of(), all.stream().limit(25).toList(), all.size() + " computed-style differences in total");
    }

    @Test
    void theComparisonIsNotAVacuousNoOp() {
        // Without these, "no differences" could just mean the stylesheet never loaded.
        for (Map.Entry<String, Snapshot> e : INTEGRATED.entrySet()) {
            Snapshot base = BASELINE.get(e.getKey());
            assertEquals("true", e.getValue().meta().get("ash-sr-only-rule"), e.getKey() + ": Ashlar's rule must be in the integrated CSSOM");
            assertFalse(e.getValue().meta().get("ash-color-bg").isEmpty(), e.getKey() + ": Ashlar's token must resolve");
            assertEquals("false", base.meta().get("ash-sr-only-rule"), e.getKey() + ": the baseline must not contain Ashlar's rule");
            assertEquals("", base.meta().get("ash-color-bg"), e.getKey() + ": the baseline must not define Ashlar's token");
            assertTrue(e.getValue().values().size() > 20, e.getKey() + ": too few elements compared");
        }
    }

    @Test
    void theBrowsersCssomReportsTheIntendedEffectiveLayerOrder() {
        INTEGRATED.forEach((key, s) -> assertEquals(EXPECTED_ORDER, s.meta().get("layer-order"), key));
        BASELINE.forEach((key, s) -> assertEquals(BASELINE_ORDER, s.meta().get("layer-order"), key));
    }

    @Test
    void everyStylesheetLoadsAndAllTenSkinsAreStillLinked() {
        INTEGRATED.forEach((key, s) -> {
            assertEquals("", s.meta().get("sheets-missing"), key);
            List<String> loaded = List.of(s.meta().get("sheets-loaded").split(","));
            assertTrue(loaded.contains("app.css"), key);
            for (String skin : SKINS) {
                assertTrue(loaded.contains("theme-" + skin + ".css"), key + " is missing theme-" + skin + ".css");
            }
        });
    }

    @Test
    void themeAndSkinStateIsAsRequestedOnEveryLoad() {
        for (String slug : pageSlugs()) {
            for (Config c : configsFor(slug)) {
                Snapshot s = INTEGRATED.get(slug + " | " + c.id());
                assertEquals(c.theme(), s.meta().get("data-theme"), c.id());
                assertEquals(c.skin() == null ? "" : c.skin(), s.meta().get("data-skin"), c.id());
            }
        }
    }

    @Test
    void controlsShowTheComparisonIsStableAndCanFail() {
        for (Map.Entry<String, Snapshot> e : BASELINE_AGAIN.entrySet()) {
            assertEquals(List.of(), diff(e.getKey(), BASELINE.get(e.getKey()), e.getValue()), "noise floor must be zero: " + e.getKey());
        }
        int detected = MUTATED.entrySet().stream().mapToInt(e -> diff(e.getKey(), BASELINE.get(e.getKey()), e.getValue()).size()).sum();
        assertTrue(detected > 0, "a deliberately mutated stylesheet must be detected as a difference");
    }

    /** SIMULATION: says nothing about browsers whose @supports gate is true. See the probe. */
    @Test
    void theSimulatedPropertiesFallbackChangesNothingInTheIntegratedPages() {
        INTEGRATED.forEach((key, s) -> {
            assertEquals("true", s.meta().get("properties-fallback-found"), key);
            assertEquals("0", s.meta().get("properties-simulation-changed-elements"), key);
        });
    }

    @Test
    void realThemeToggleAndSkinSelectionStillWorkAndDriveAshlarsTokens() throws IOException {
        Map<String, String> m = interactive.meta();
        assertEquals("light", m.get("theme-before"));
        assertEquals("true", m.get("theme-toggle-present"));
        assertEquals("dark", m.get("theme-after-click"));
        assertEquals("dark", m.get("color-scheme-after-click"));
        assertNotEquals(m.get("ash-bg-before"), m.get("ash-bg-after-click"), "Ashlar's background token must follow dark mode");
        assertEquals("light", m.get("theme-after-second-click"));
        assertEquals(m.get("ash-bg-before"), m.get("ash-bg-after-second-click"));

        assertEquals("true", m.get("skin-select-present"));
        assertEquals("axiom", m.get("skin-axiom-data-skin"));
        assertEquals("ledger", m.get("skin-ledger-data-skin"));
        assertEquals("", m.get("skin-cleared-data-skin"));
        // The skins' own value reaches the page, and Ashlar's accent token follows it.
        assertEquals(skinBlue("axiom"), m.get("skin-axiom-blue-600"));
        assertEquals(skinBlue("ledger"), m.get("skin-ledger-blue-600"));
        assertEquals(m.get("skin-axiom-blue-600"), m.get("skin-axiom-ash-accent"));
        assertEquals(m.get("skin-ledger-blue-600"), m.get("skin-ledger-ash-accent"));
        assertNotEquals(m.get("skin-axiom-ash-accent"), m.get("skin-ledger-ash-accent"));
    }

    private static String skinBlue(String skin) throws IOException {
        try (InputStream in = ShowcaseRuntimeTest.class.getResourceAsStream("/static/css/theme-" + skin + ".css")) {
            Matcher m = Pattern.compile("(?m)^\\s+--color-blue-600:\\s*([^;]+);").matcher(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            assertTrue(m.find());
            return m.group(1).trim().replaceAll("\\s+", " ");
        }
    }

    @AfterAll
    static void clear() {
        BASELINE.clear();
        INTEGRATED.clear();
        BASELINE_AGAIN.clear();
        MUTATED.clear();
    }
}
