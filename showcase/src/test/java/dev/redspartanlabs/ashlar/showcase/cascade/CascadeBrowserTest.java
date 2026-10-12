package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Real-browser computed-style verification of the intended cascade order
 *
 * <pre>
 * theme &lt; base &lt; ash.tokens &lt; ash.base &lt; ash.layout &lt; ash.components &lt; ash.skin
 *       &lt; components &lt; utilities &lt; unlayered consumer CSS
 * </pre>
 *
 * Every scenario is an independent page load in headless Chrome using the
 * real compiled showcase {@code app.css}, the real Tailwind-CLI-compiled Path A
 * builds, and the real skin files. Results are the browser's own
 * {@code getComputedStyle} output.
 *
 * <p>IMPORTANT: a passing Path A scenario proves the MECHANISM using a
 * test-only wrapper. It does not mean the production Tailwind entry point is
 * already correct - that file has no ordering statement yet.
 *
 * <p>SUPPORTED scenarios must pass every check. NEGATIVE-CONTROL scenarios
 * must show at least one failing check; which checks fail is measured and
 * written to {@code build/reports/cascade/results.txt}, not assumed.
 */
class CascadeBrowserTest {

    enum Kind { SUPPORTED, NEGATIVE_CONTROL }

    /**
     * {@code statement} is the layer-order statement a human would read in the
     * setup (documentation only); {@code expectOverride} is the {@code --winner}
     * value a consumer override is expected to produce, or null when the
     * scenario has no override probe.
     */
    record Scenario(String id, Kind kind, String description, List<String> hrefs, boolean tailwind,
                    String skin, String statement, String expectOverride) {
        Scenario(String id, Kind kind, String description, List<String> hrefs, boolean tailwind, String skin) {
            this(id, kind, description, hrefs, tailwind, skin, "", null);
        }

        Scenario(String id, Kind kind, String description, List<String> hrefs, boolean tailwind) {
            this(id, kind, description, hrefs, tailwind, null, "", null);
        }
    }

    record Check(String key, String expected, String actual, boolean pass) {
        boolean informational() {
            return key.startsWith("info-");
        }
    }

    // The Tailwind-only stylesheet: the showcase's pre-Phase-C production app.css, rebuilt from
    // src/test/tailwind/phase-c-baseline-entry.css. The production app.css now bundles Ashlar,
    // so V1/Phase A scenarios that mean "Tailwind with no Ashlar" must not use it.
    private static final String APP = "/generated/phase-c-baseline.css";
    private static final String PATH_A = "/generated/path-a.css";
    private static final String PATH_A_WRONG = "/generated/path-a-wrong-order.css";
    private static final String ASH_BARE = "/cascade/probe-ash-bare.css";
    private static final String ASH_ORDERED = "/cascade/probe-ash-ordered.css";
    private static final String TW_LAYERS = "/cascade/probe-tw-layers.css";
    private static final String CONSUMER = "/cascade/probe-consumer.css";

    private static final List<Scenario> SCENARIOS = new ArrayList<>(List.of(
            new Scenario("S1", Kind.SUPPORTED, "Tailwind compiled WITH Path A; Ashlar (bare) linked after",
                    List.of(PATH_A, ASH_BARE, TW_LAYERS, CONSUMER), true),
            new Scenario("S2", Kind.NEGATIVE_CONTROL, "Tailwind compiled WITH Path A; Ashlar (bare) linked BEFORE it - the bare Ashlar stylesheet's first mention of ash.* fixes their position before Path A's statement is seen",
                    List.of(ASH_BARE, PATH_A, TW_LAYERS, CONSUMER), true),
            new Scenario("S3", Kind.NEGATIVE_CONTROL, "Tailwind WITHOUT Path A (Tailwind-only baseline = the pre-Phase-C production app.css); Ashlar (bare) linked before",
                    List.of(ASH_BARE, APP, TW_LAYERS, CONSUMER), true),
            new Scenario("S4", Kind.NEGATIVE_CONTROL, "Tailwind WITHOUT Path A (Tailwind-only baseline = the pre-Phase-C production app.css); Ashlar (bare) linked after",
                    List.of(APP, ASH_BARE, TW_LAYERS, CONSUMER), true),
            new Scenario("S5", Kind.NEGATIVE_CONTROL, "UNSUPPORTED: Tailwind WITHOUT Path A; Ashlar (self-ordering) linked before - ash.* order is correct, but Tailwind's `properties` fallback layer lands ABOVE utilities",
                    List.of(ASH_ORDERED, APP, TW_LAYERS, CONSUMER), true),
            new Scenario("S6", Kind.NEGATIVE_CONTROL, "Tailwind WITHOUT Path A; Ashlar (self-ordering) linked after",
                    List.of(APP, ASH_ORDERED, TW_LAYERS, CONSUMER), true),
            new Scenario("S7a", Kind.SUPPORTED, "Non-Tailwind consumer; Ashlar (self-ordering); consumer CSS last",
                    List.of(ASH_ORDERED, TW_LAYERS, CONSUMER), false),
            new Scenario("S7b", Kind.SUPPORTED, "Non-Tailwind consumer; Ashlar (self-ordering); consumer CSS first",
                    List.of(CONSUMER, ASH_ORDERED, TW_LAYERS), false),
            new Scenario("S8", Kind.NEGATIVE_CONTROL, "Non-Tailwind consumer; Ashlar WITHOUT a self-ordering statement",
                    List.of(ASH_BARE, TW_LAYERS, CONSUMER), false),
            new Scenario("S9", Kind.NEGATIVE_CONTROL, "Path A with ash.skin mis-ordered below ash.components",
                    List.of(PATH_A_WRONG, ASH_BARE, TW_LAYERS, CONSUMER), true),
            new Scenario("S10", Kind.NEGATIVE_CONTROL, "Harness sanity: no stylesheets at all",
                    List.of(), false)));

    // ---- Phase A. Kinds below are HYPOTHESES recorded before measurement; the
    // first run's outcome versus these is reported, not silently adjusted. ----
    private static final String A2_IMPORT = "/generated/a2-import.css";
    private static final String A2_IMPORT_NO_STATEMENT = "/generated/a2-import-no-statement.css";
    private static final String A2_IMPORT_ASH_FIRST = "/generated/a2-import-ash-first.css";
    private static final String A3_PROPERTIES_FIRST = "/generated/a3-properties-first.css";
    private static final String A4_APP_LAST = "/generated/a4-app-layer-last.css";
    private static final String ASH_ORDERED_PROPS = "/cascade/probe-ash-ordered-properties.css";
    private static final String CONSUMER_LAYER = "/cascade/probe-consumer-layer.css";
    private static final String CONSUMER_UNLAYERED = "/cascade/probe-consumer-unlayered-override.css";

    private static final String STMT = "@layer theme, base, ash.tokens, ash.base, ash.layout, ash.components, ash.skin, components, utilities;";
    private static final String STMT_PROPS = "@layer properties, theme, base, ash.tokens, ash.base, ash.layout, ash.components, ash.skin, components, utilities;";
    private static final String STMT_APP = "@layer theme, base, ash.tokens, ash.base, ash.layout, ash.components, ash.skin, components, utilities, app;";

    private static Scenario a(String id, Kind kind, String description, List<String> hrefs, boolean tailwind, String statement, String expectOverride) {
        return new Scenario(id, kind, description, hrefs, tailwind, null, statement, expectOverride);
    }

    static {
        // A1: self-ordering Ashlar linked AFTER the coordinated Tailwind build.
        SCENARIOS.add(a("A1", Kind.SUPPORTED, "A1: coordinated Tailwind (Path A) + Ashlar SELF-ORDERING linked after",
                List.of(PATH_A, ASH_ORDERED, TW_LAYERS, CONSUMER), true, "in path-a.css: " + STMT + " ; ash file repeats it", null));

        // A2: Ashlar imported into the Tailwind entry.
        SCENARIOS.add(a("A2", Kind.SUPPORTED, "A2: Tailwind entry = statement + @import app.css + @import ash-ordered (bundled, single stylesheet)",
                List.of(A2_IMPORT, TW_LAYERS, CONSUMER), true, STMT, null));
        SCENARIOS.add(a("A2-no-statement", Kind.NEGATIVE_CONTROL, "A2 control: bundled import WITHOUT the coordinated statement first",
                List.of(A2_IMPORT_NO_STATEMENT, TW_LAYERS, CONSUMER), true, "(none in entry; Tailwind's own statement is first)", null));
        SCENARIOS.add(a("A2-ash-first", Kind.SUPPORTED, "A2 variant: bundled, Ashlar imported BEFORE the Tailwind entry, no coordinated statement",
                List.of(A2_IMPORT_ASH_FIRST, TW_LAYERS, CONSUMER), true, "ash file's own statement is first", null));

        // A3: naming `properties` first.
        SCENARIOS.add(a("A3-link-before", Kind.SUPPORTED, "A3: self-ordering Ashlar naming `properties` FIRST, linked before the Tailwind-only app.css (the V1 S5 shape)",
                List.of(ASH_ORDERED_PROPS, APP, TW_LAYERS, CONSUMER), true, STMT_PROPS, null));
        SCENARIOS.add(a("A3-link-after", Kind.NEGATIVE_CONTROL, "A3 control: same Ashlar file linked AFTER the Tailwind-only app.css (V1 S6 shape)",
                List.of(APP, ASH_ORDERED_PROPS, TW_LAYERS, CONSUMER), true, STMT_PROPS, null));
        SCENARIOS.add(a("A3-coordinated", Kind.SUPPORTED, "A3: coordinated Tailwind entry naming `properties` first, bare Ashlar after",
                List.of(A3_PROPERTIES_FIRST, ASH_BARE, TW_LAYERS, CONSUMER), true, STMT_PROPS, null));
        SCENARIOS.add(a("A3-non-tailwind", Kind.SUPPORTED, "A3: non-Tailwind consumer with the `properties`-first Ashlar file",
                List.of(ASH_ORDERED_PROPS, TW_LAYERS, CONSUMER), false, STMT_PROPS, null));

        // A4: consumer-defined named layer `app`.
        SCENARIOS.add(a("A4-layer-after", Kind.SUPPORTED, "A4: Tailwind; consumer `@layer app` stylesheet linked AFTER Ashlar, no coordination",
                List.of(PATH_A, ASH_ORDERED, TW_LAYERS, CONSUMER, CONSUMER_LAYER), true, "app first mentioned after the Ashlar statement", "app-layer-override"));
        SCENARIOS.add(a("A4-layer-before", Kind.NEGATIVE_CONTROL, "A4 control: consumer `@layer app` stylesheet linked BEFORE Ashlar and Tailwind, no coordination",
                List.of(CONSUMER_LAYER, PATH_A, ASH_ORDERED, TW_LAYERS, CONSUMER), true, "app first mentioned before everything", "app-layer-override"));
        SCENARIOS.add(a("A4-coordinated", Kind.SUPPORTED, "A4: consumer owns the statement and names `app` last; its layer stylesheet is linked BEFORE Ashlar",
                List.of(A4_APP_LAST, CONSUMER_LAYER, ASH_ORDERED, TW_LAYERS, CONSUMER), true, STMT_APP, "app-layer-override"));
        SCENARIOS.add(a("A4-unlayered-after", Kind.SUPPORTED, "A4: Tailwind; consumer UNLAYERED override linked after Ashlar",
                List.of(PATH_A, ASH_ORDERED, TW_LAYERS, CONSUMER, CONSUMER_UNLAYERED), true, "(unlayered)", "unlayered-override"));
        SCENARIOS.add(a("A4-unlayered-before", Kind.SUPPORTED, "A4: Tailwind; consumer UNLAYERED override linked BEFORE Ashlar",
                List.of(CONSUMER_UNLAYERED, PATH_A, ASH_ORDERED, TW_LAYERS, CONSUMER), true, "(unlayered)", "unlayered-override"));
        SCENARIOS.add(a("A4-nt-layer-after", Kind.SUPPORTED, "A4: non-Tailwind; consumer `@layer app` linked after Ashlar",
                List.of(ASH_ORDERED, TW_LAYERS, CONSUMER, CONSUMER_LAYER), false, "app first mentioned after the Ashlar statement", "app-layer-override"));
        SCENARIOS.add(a("A4-nt-layer-before", Kind.NEGATIVE_CONTROL, "A4 control: non-Tailwind; consumer `@layer app` linked BEFORE Ashlar",
                List.of(CONSUMER_LAYER, ASH_ORDERED, TW_LAYERS, CONSUMER), false, "app first mentioned before the Ashlar statement", "app-layer-override"));
        SCENARIOS.add(a("A4-nt-unlayered-before", Kind.SUPPORTED, "A4: non-Tailwind; consumer UNLAYERED override linked BEFORE Ashlar",
                List.of(CONSUMER_UNLAYERED, ASH_ORDERED, TW_LAYERS, CONSUMER), false, "(unlayered)", "unlayered-override"));
    }

    static {
        for (String skin : List.of("axiom", "ledger", "summit")) {
            SCENARIOS.add(new Scenario("K-" + skin, Kind.SUPPORTED,
                    "Path A + Ashlar (bare) + real unlayered theme-" + skin + ".css",
                    List.of(PATH_A, ASH_BARE, TW_LAYERS, CONSUMER, "/css/theme-" + skin + ".css"), true, skin));
        }
    }

    private static final Map<String, List<Check>> RESULTS = new LinkedHashMap<>();
    private static String chromeVersion = "";

    @BeforeAll
    static void measureEveryScenario() throws Exception {
        String dir = System.getProperty("ashlar.cascade.dir");
        if (dir == null) {
            throw new IllegalStateException("ashlar.cascade.dir is not set; run through Gradle (:showcase:test)");
        }
        var chromePath = HeadlessChrome.find();
        if (chromePath.isEmpty()) {
            boolean mustRun = System.getenv("CI") != null || "true".equalsIgnoreCase(System.getenv("ASHLAR_REQUIRE_BROWSER"));
            if (mustRun) {
                throw new IllegalStateException("Chrome is required (CI or ASHLAR_REQUIRE_BROWSER set) but was not found. "
                        + "Set ashlar.chrome, ASHLAR_CHROME or CHROME_BIN.");
            }
            System.out.println("[cascade] Chrome not found - browser scenarios SKIPPED (not passed).");
            Assumptions.abort("Chrome not found; browser cascade scenarios were not run");
        }
        HeadlessChrome chrome = new HeadlessChrome(chromePath.get());

        try (CascadeFixtureServer server = new CascadeFixtureServer(Path.of(dir))) {
            for (Scenario s : SCENARIOS) {
                Map<String, String> attrs = new LinkedHashMap<>();
                if (s.tailwind()) {
                    attrs.put("data-tailwind", "1");
                }
                if (s.expectOverride() != null) {
                    attrs.put("data-expect-override", s.expectOverride());
                }
                if (s.skin() != null) {
                    attrs.put("data-skin", s.skin());
                    attrs.put("data-expect-font", skinToken(s.skin(), "--font-sans"));
                    attrs.put("data-expect-blue", skinToken(s.skin(), "--color-blue-600"));
                }
                String url = server.register(s.hrefs(), attrs);
                List<Check> checks = parse(s, chrome.dumpDom(url));
                checks.stream().filter(c -> c.key().equals("info-user-agent")).findFirst()
                        .ifPresent(c -> chromeVersion = chrome.executable() + " | " + c.actual());
                RESULTS.put(s.id(), checks);
            }
        }
        writeReport(Path.of(dir));
    }

    private static String skinToken(String skin, String token) throws IOException {
        try (InputStream in = CascadeBrowserTest.class.getResourceAsStream("/static/css/theme-" + skin + ".css")) {
            String css = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Matcher m = Pattern.compile("(?m)^\\s+" + Pattern.quote(token) + ":\\s*([^;]+);").matcher(css);
            if (!m.find()) {
                throw new IllegalStateException("theme-" + skin + ".css declares no " + token);
            }
            return m.group(1).trim();
        }
    }

    private static List<Check> parse(Scenario s, String dom) {
        Matcher m = Pattern.compile("<pre id=\"ash-cascade-results\">(.*?)</pre>", Pattern.DOTALL).matcher(dom);
        if (!m.find() || m.group(1).isBlank()) {
            throw new IllegalStateException("Scenario " + s.id() + ": the fixture produced no results - infrastructure failure, not a pass.\n" + dom);
        }
        String text = m.group(1).replace("&quot;", "\"").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
        List<Check> checks = new ArrayList<>();
        // Chrome's stdout uses CRLF on Windows; split on any line terminator.
        for (String line : text.split("\\R")) {
            String[] f = line.split("\t", -1);
            if (f.length != 4) {
                throw new IllegalStateException("Scenario " + s.id() + ": malformed result line: " + line);
            }
            checks.add(new Check(f[0], f[1], f[2], "PASS".equals(f[3])));
        }
        if (checks.stream().filter(c -> c.key().startsWith("ladder-")).count() != 10) {
            throw new IllegalStateException("Scenario " + s.id() + ": expected 10 ladder checks, got " + checks);
        }
        return checks;
    }

    private static void writeReport(Path generatedDir) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("Cascade verification (V1). Browser: ").append(chromeVersion).append("\n");
        sb.append("NOTE: Path A scenarios use a test-only wrapper. Passing does NOT mean the production\n")
                .append("Tailwind entry point (showcase/src/main/tailwind/app.css) is already correct.\n\n")
                .append("EVIDENCE LEVELS\n")
                .append("  OBSERVED   : computed style measured in this browser with the real compiled CSS.\n")
                .append("  SIMULATED  : [properties-fallback-*] checks. Tailwind gates its `properties` fallback behind\n")
                .append("               @supports. When the gate is false in this browser (it is, see\n")
                .append("               info-properties-supports-gate-true-in-this-browser) the real fallback is inert here;\n")
                .append("               the check injects the real rule text, ungated, into the same layer to show what\n")
                .append("               WOULD happen. It proves layer-order consequence only. It does NOT prove that any\n")
                .append("               browser whose gate is true behaves this way - no such browser was run (UNVERIFIED).\n\n");
        for (Scenario s : SCENARIOS) {
            List<Check> checks = RESULTS.get(s.id());
            List<Check> failing = checks.stream().filter(c -> !c.pass() && !c.informational()).toList();
            sb.append("== ").append(s.id()).append(" [").append(s.kind()).append("] ").append(s.description()).append('\n');
            sb.append("   links: ").append(s.hrefs()).append('\n');
            if (!s.statement().isEmpty()) {
                sb.append("   layer statement: ").append(s.statement()).append('\n');
            }
            boolean gateActive = checks.stream().anyMatch(c ->
                    c.key().equals("info-properties-supports-gate-true-in-this-browser") && c.actual().equals("true"));
            for (Check c : checks) {
                String level = c.key().startsWith("properties-fallback-")
                        ? (gateActive ? "  [OBSERVED: gate active]" : "  [SIMULATED: gate false in this browser; unverified where gate is true]")
                        : "";
                sb.append("   ").append(c.pass() ? "PASS" : "FAIL").append("  ").append(c.key())
                        .append("  expected=").append(c.expected()).append("  actual=").append(c.actual())
                        .append(level).append('\n');
            }
            sb.append("   failing: ").append(failing.stream().map(Check::key).toList()).append("\n\n");
        }
        Path out = generatedDir.resolve("../../reports/cascade/results.txt").normalize();
        Files.createDirectories(out.getParent());
        Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
        System.out.println(sb);
        System.out.println("[cascade] report written to " + out);
    }

    static Stream<Scenario> supported() {
        return SCENARIOS.stream().filter(s -> s.kind() == Kind.SUPPORTED);
    }

    static Stream<Scenario> negativeControls() {
        return SCENARIOS.stream().filter(s -> s.kind() == Kind.NEGATIVE_CONTROL);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("supported")
    void supportedConfigurationPassesEveryCheck(Scenario s) {
        List<String> failing = RESULTS.get(s.id()).stream()
                .filter(c -> !c.pass() && !c.informational())
                .map(c -> c.key() + " expected=" + c.expected() + " actual=" + c.actual()).toList();
        if (!failing.isEmpty()) {
            fail(s.id() + " (" + s.description() + ") failed: " + failing);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("negativeControls")
    void negativeControlIsDetectedAsFailing(Scenario s) {
        boolean anyFailure = RESULTS.get(s.id()).stream().anyMatch(c -> !c.pass() && !c.informational());
        assertTrue(anyFailure, s.id() + " (" + s.description() + ") showed no failing check, so the harness did not detect it");
    }

    /**
     * S2 is a negative control whose failure was measured and reviewed: it must
     * fail on exactly these checks and pass every other one. Any drift in either
     * direction fails the suite.
     */
    @Test
    void s2FailsExactlyWhereItWasMeasuredToFail() {
        List<String> failing = RESULTS.get("S2").stream()
                .filter(c -> !c.pass() && !c.informational())
                .map(Check::key).toList();
        assertEquals(List.of(
                "ladder-2-ash.tokens", "ladder-3-ash.base", "ladder-4-ash.layout",
                "ladder-5-ash.components", "ladder-6-ash.skin",
                "real-ash-base-beats-preflight", "real-ash-tokens-beats-theme"), failing);
        // Rung-level evidence: every ash.* rung is outranked by Tailwind's base layer (winner 1).
        for (Check c : RESULTS.get("S2")) {
            if (c.key().matches("ladder-[2-6]-.*")) {
                assertEquals("1", c.actual(), c.key());
            }
        }
    }

    /**
     * S5 is unsupported because of the {@code properties} layer, measured and
     * reviewed. Its ash.* ladder is fine; only the two fallback checks fail.
     * S1 is the control for the same injection: there {@code properties} stays
     * below utilities and the injected rule changes nothing.
     */
    @Test
    void s5FailsExactlyOnThePropertiesFallbackChecks() {
        List<String> failing = RESULTS.get("S5").stream()
                .filter(c -> !c.pass() && !c.informational())
                .map(Check::key).toList();
        assertEquals(List.of(
                "properties-fallback-leaves-shadow-utility-intact",
                "properties-fallback-leaves-translate-utility-intact"), failing);
        assertEquals("properties-layer-wins", actual("S5", "info-properties-outranks-utilities-probe"));
        assertEquals("8", actual("S1", "info-properties-outranks-utilities-probe"));
        assertEquals("false", actual("S5", "info-properties-supports-gate-true-in-this-browser"),
                "S5's failure is a simulation: the real @supports gate is false in this browser");
    }

    private static List<String> failingKeys(String scenario) {
        return RESULTS.get(scenario).stream().filter(c -> !c.pass() && !c.informational()).map(Check::key).toList();
    }

    /** Phase A negative controls: exact failing sets, pinned after they were measured and reviewed. */
    @Test
    void phaseANegativeControlsFailExactlyWhereMeasured() {
        List<String> utilitiesOutranked = List.of("ladder-7-components", "ladder-8-utilities", "real-utility-beats-ash-and-components");
        assertEquals(utilitiesOutranked, failingKeys("A2-no-statement"));
        assertEquals(utilitiesOutranked, failingKeys("A3-link-after"));
        assertEquals(List.of("consumer-override-beats-ash", "consumer-override-beats-real-utility"), failingKeys("A4-layer-before"));
        assertEquals(List.of("consumer-override-beats-ash"), failingKeys("A4-nt-layer-before"));
        // In the A4 controls the Ashlar rung (5) beat the consumer's lower layer.
        assertEquals("5", actual("A4-layer-before", "consumer-override-beats-ash"));
        assertEquals("5", actual("A4-nt-layer-before", "consumer-override-beats-ash"));
    }

    /** The browser's own CSSOM must report the intended effective order in every supported Tailwind configuration. */
    @Test
    void supportedTailwindPhaseAConfigurationsReportTheIntendedCssomOrder() {
        String intended = "properties;theme;base;ash;ash.tokens;ash.base;ash.layout;ash.components;ash.skin;components;utilities";
        for (String id : List.of("A1", "A2", "A2-ash-first", "A3-link-before", "A3-coordinated", "A4-unlayered-after", "A4-unlayered-before")) {
            assertEquals(intended, actual(id, "info-cssom-layer-order"), id);
        }
        assertEquals(intended + ";app", actual("A4-layer-after", "info-cssom-layer-order"));
        assertEquals(intended + ";app", actual("A4-coordinated", "info-cssom-layer-order"));
    }

    /**
     * A3: naming `properties` first moves it back below utilities in the V1 S5 shape. This
     * compares the same SIMULATED injection used in S5; it does not show any real browser is affected.
     */
    @Test
    void namingPropertiesFirstKeepsItBelowUtilitiesInTheShapeThatFailedInS5() {
        assertEquals("properties-layer-wins", actual("S5", "info-properties-outranks-utilities-probe"));
        assertEquals("8", actual("A3-link-before", "info-properties-outranks-utilities-probe"));
        assertEquals("false", actual("A3-link-before", "info-properties-supports-gate-true-in-this-browser"));
    }

    private static String actual(String scenario, String key) {
        return RESULTS.get(scenario).stream().filter(c -> c.key().equals(key)).findFirst().orElseThrow().actual();
    }

    @Test
    void ladderIsComplete() {
        for (Scenario s : SCENARIOS) {
            assertFalse(RESULTS.get(s.id()).isEmpty(), s.id());
        }
    }

    @AfterAll
    static void clear() {
        RESULTS.clear();
    }
}
