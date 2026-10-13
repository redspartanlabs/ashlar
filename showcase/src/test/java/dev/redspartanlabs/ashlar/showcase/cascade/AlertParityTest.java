package dev.redspartanlabs.ashlar.showcase.cascade;

import dev.redspartanlabs.ashlar.alert.AlertStyles;
import dev.redspartanlabs.ashlar.alert.AlertVariant;
import dev.redspartanlabs.ashlar.showcase.catalog.ComponentCatalog;
import dev.redspartanlabs.ashlar.showcase.catalog.ComponentPage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Alert's parity gates, measured in real Chrome.
 *
 * <p>Evidence comes in two kinds, and neither stores a baseline. (1) A controlled oracle: the Alert's legacy
 * Tailwind markup (compiled by the real Tailwind CLI) next to its migrated markup, in the same browser, for each
 * of the 22 theme and skin configurations, with Tailwind present and with Tailwind removed. (2) Every Alert on the
 * real showcase page, in the same 22 configurations: the real page is measured as served, then the very same
 * elements get their legacy classes back and are measured again, in the same run. Differences are compared with
 * {@link ParityNormalizer} (exact equality except H1 to H4, with their preconditions). Negative controls show
 * each gate can fail. The skins that target the Alert are checked against what they define, not only against the
 * legacy rendering.
 *
 * <p>Evidence is Chrome only. Computed-style parity is the visual evidence; there is no screenshot comparison.
 */
class AlertParityTest {

    private static final String ASHLAR = "/ashlar-real/ashlar.css";
    private static final String ORACLE = "/generated/alert-oracle.css";
    private static final String APP = "/css/app.css";
    private static final String ORACLE_VIRTUAL = "/__parity/alert-oracle.css";
    private static final String SCOPED = "/generated/alert-legacy-scoped.css";
    private static final String SCOPED_VIRTUAL = "/__parity/alert-legacy-scoped.css";
    private static final String ALERT_PAGE = "/ashlar/components/alert";
    private static final List<String> SKINS = List.of("axiom", "civic", "fathom", "foundry", "harbor", "keystone", "ledger", "sterling", "summit", "veridian");
    private static final List<String> SKIN_LINKS = SKINS.stream().map(s -> "/css/theme-" + s + ".css").toList();
    private static final List<String> VARIANTS = AlertLegacyMarkup.VARIANTS;
    private static final Map<String, String> HUE = Map.of("info", "sky", "success", "green", "warning", "amber", "error", "red");

    record Config(String id, String theme, String skin) {
    }

    private static final List<Config> CONFIGS = configs();

    private static List<Config> configs() {
        List<Config> all = new ArrayList<>();
        for (String theme : List.of("light", "dark")) {
            all.add(new Config(theme + "/none", theme, null));
            for (String skin : SKINS) {
                all.add(new Config(theme + "/" + skin, theme, skin));
            }
        }
        return List.copyOf(all);
    }

    private static Config config(String id) {
        return CONFIGS.stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow();
    }

    /** The Alert shapes of the controlled oracle: what the template can render, plus the cases that stress layout. */
    private record Shape(String name, boolean title, String body) {
    }

    private static final String LONG_WORD = "Supercalifragilisticexpialidocious_Supercalifragilisticexpialidocious_Supercalifragilistic";
    private static final List<Shape> SHAPES = List.of(
            new Shape("titled", true, "Body text that wraps across more than one line so that its line height shows in the measured height."),
            new Shape("body-only", false, "Body text that wraps across more than one line so that its line height shows in the measured height."),
            new Shape("title-only", true, null),
            new Shape("long-word", true, LONG_WORD),
            new Shape("paragraph", true, "<p>A paragraph in the body, as the showcase page writes it.</p>"));
    private static final int PARAGRAPH_SHAPE = 4;

    // ---- parsed measurements ----

    record Row(String phase, int ordinal, String kind, String variant, String context, String parentDisplay, String[] values, String diagnostics) {
    }

    record Probe(List<String> props, Map<String, String> meta, List<String[]> shapes, Map<String, Map<String, String>> tokens, List<Row> rows) {
        List<Row> phase(String phase) {
            return rows.stream().filter(r -> r.phase().equals(phase)).toList();
        }

        String value(Row row, String property) {
            return row.values()[props.indexOf(property)];
        }
    }

    record Finding(String where, String property, String expected, String actual) {
        String line() {
            return where + " | " + property + " | " + expected + " -> " + actual;
        }
    }

    private static List<String> props = List.of();
    private static final Map<String, List<Finding>> CONTROLLED_PRESENT = new LinkedHashMap<>();
    private static final Map<String, List<Finding>> CONTROLLED_REMOVED_SCOPED = new LinkedHashMap<>();
    private static final Map<String, List<Finding>> CONTROLLED_REMOVED_FULL = new LinkedHashMap<>();
    private static final Map<String, List<Finding>> SCOPED_FIDELITY = new LinkedHashMap<>();
    private static final Map<String, List<Finding>> CONTROLS = new LinkedHashMap<>();
    private static final Map<String, Integer> CONTROLLED_ROWS = new LinkedHashMap<>();
    private static final Map<String, Probe> FRAME_PROBES = new LinkedHashMap<>();
    private static final Map<String, Integer> APPLIED_CONTROLLED = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_SCOPED = new TreeMap<>();
    private static final List<Finding> REAL_PRESENT = new ArrayList<>();
    private static final List<Finding> REAL_REMOVED = new ArrayList<>();
    private static final Map<String, Integer> APPLIED_REAL_PRESENT = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_REAL_REMOVED = new TreeMap<>();
    private static final Map<String, Probe> REAL_PROBES = new LinkedHashMap<>();
    private static final Map<String, Integer> ALERTS_PER_PAGE = new TreeMap<>();
    private static final List<String> SKIN_PROBLEMS = new ArrayList<>();
    private static final List<String> SHAPE_PROBLEMS = new ArrayList<>();
    private static int realComparisonsPerMode;
    private static long realSeconds;

    // ---- markup ----

    private static AlertLegacyMarkup legacy;

    private static final String ICON_SVG = "<svg class=\"inline-block shrink-0 %s\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" "
            + "stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\"><path d=\"M4 12l5 5L20 6\"></path></svg>";

    /** One Alert exactly as alert.jte renders it (the template itself is measured on the real page). */
    private static String alert(String variant, Shape shape) {
        AlertVariant v = AlertVariant.valueOf(variant.toUpperCase(Locale.ROOT));
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"margin:8px;width:380px\"><div class=\"").append(AlertStyles.container(v)).append("\" data-alert data-alert-variant=\"").append(variant).append("\">");
        sb.append(ICON_SVG.formatted(AlertStyles.icon(v)));
        sb.append("<div class=\"ash-alert__content\"><span class=\"ash-sr-only\">").append(v.label()).append(":</span>");
        if (shape.title()) {
            sb.append("<h3 class=\"").append(AlertStyles.title(v)).append("\">Title of the ").append(variant).append(" alert</h3>");
        }
        if (shape.body() != null) {
            sb.append("<div class=\"").append(AlertStyles.body(v)).append("\">").append(shape.body()).append("</div>");
        }
        return sb.append("</div></div></div>").toString();
    }

    private static String markup(boolean legacyClasses) {
        StringBuilder sb = new StringBuilder();
        for (String variant : VARIANTS) {
            for (Shape shape : SHAPES) {
                sb.append(alert(variant, shape));
            }
        }
        return legacyClasses ? legacy.rewrite(sb.toString()) : sb.toString();
    }

    private static String frame(String id, Config config, List<String> sheets, String extraCss, String body) {
        StringBuilder links = new StringBuilder();
        sheets.forEach(href -> links.append("<link rel=\"stylesheet\" href=\"").append(href).append("\">"));
        String skin = config.skin() == null ? "" : " data-skin=\"" + config.skin() + "\"";
        String doc = "<!doctype html><html data-theme=\"" + config.theme() + "\"" + skin + "><head>" + links + "<style>" + extraCss
                + "</style></head><body style=\"margin:0;padding:8px\">" + body + "</body></html>";
        return "<iframe data-id=\"" + id + "\" style=\"width:440px;height:3400px;border:0\" srcdoc=\""
                + doc.replace("&", "&amp;").replace("\"", "&quot;") + "\"></iframe>\n";
    }

    private static List<String> with(String first, List<String> rest) {
        List<String> all = new ArrayList<>();
        all.add(first);
        all.addAll(rest);
        return all;
    }

    /** Ordinals (1-based, document order) of the alerts in the paragraph shape, whose body holds caller content. */
    private static Set<Integer> paragraphOrdinals() {
        Set<Integer> ordinals = new java.util.HashSet<>();
        for (int v = 0; v < VARIANTS.size(); v++) {
            ordinals.add(v * SHAPES.size() + PARAGRAPH_SHAPE + 1);
        }
        return ordinals;
    }

    /**
     * What the page keeps when Tailwind is removed: the base font and line height Tailwind's stylesheet gave it, plus the
     * showcase's OWN dark colour scheme (a rule in its application CSS, in app.css and phase-c-baseline-entry.css, not
     * Tailwind's). Without it dark pages would draw their default text in black, and that would be a difference in the
     * test page, not in the Alert.
     */
    private static String environment(Path generatedDir) throws IOException {
        return PageEnvironment.tailwindRemovedRules(generatedDir.resolve("phase-c-baseline.css")) + ":root[data-theme=\"dark\"]{color-scheme:dark}\n";
    }

    // ---- control experiments: each breaks the migrated Alert in ONE way and must be detected ----

    private record Control(String name, String config, String css, String expectedProperty) {
    }

    private static final List<Control> CONTROL_LIST = List.of(
            new Control("wrong info background", "light/none", ".ash-alert--info{--ash-alert-bg:red}", "background-color"),
            new Control("wrong dark border", "dark/none", ".ash-alert--info:where([data-theme=dark],[data-theme=dark] *){--ash-alert-border:var(--color-sky-200)}", "border-top-color"),
            new Control("title weight changed", "light/none", ".ash-alert__title{font-weight:500}", "font-weight"),
            new Control("padding changed", "light/none", ".ash-alert{padding:.875rem}", "padding-top"),
            new Control("border one pixel too wide", "light/none", ".ash-alert{border-width:2px}", "border-top-width"),
            new Control("gap changed", "light/none", ".ash-alert{gap:.5rem}", "gap"),
            new Control("icon margin removed", "light/none", ".ash-alert__icon{margin-top:0}", "margin-top"),
            new Control("icon size changed", "light/none", ".ash-alert__icon{width:1rem}", "width"),
            new Control("radius ignores the skin", "light/ledger", ".ash-alert{border-radius:.5rem}", "border-top-left-radius"),
            new Control("content may not shrink below its text", "light/none", ".ash-alert__content{min-width:auto}", "width"),
            new Control("no spacing between title and body", "light/none", ".ash-alert__title + .ash-alert__body{margin-top:0}", "margin-top"),
            new Control("severity label no longer visually hidden", "light/none", ".ash-sr-only{position:static;width:auto;height:auto;margin:0;overflow:visible;clip-path:none}", "position"));

    // ---- running ----

    private static List<String> lines(String dom, String preId) {
        Matcher m = Pattern.compile("<pre id=\"" + preId + "\">(.*?)</pre>", Pattern.DOTALL).matcher(dom);
        String found = null;
        while (m.find()) {
            found = m.group(1); // the element the probe appended is the last one
        }
        if (found == null || found.isBlank()) {
            throw new IllegalStateException("no results in " + preId + " - infrastructure failure, not a pass");
        }
        return Arrays.asList(found.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&amp;", "&").split("\\R"));
    }

    private static Probe parse(List<String> lines, String what) {
        List<String> propNames = List.of();
        Map<String, String> meta = new LinkedHashMap<>();
        List<String[]> shapes = new ArrayList<>();
        Map<String, Map<String, String>> tokens = new LinkedHashMap<>();
        List<Row> rows = new ArrayList<>();
        for (String line : lines) {
            String[] f = line.split("\t", -1);
            switch (f[0]) {
                case "props" -> propNames = List.of(f[1].split(","));
                case "meta" -> meta.put(f[1], f.length > 2 ? f[2] : "");
                case "shape" -> shapes.add(f);
                case "tok" -> tokens.computeIfAbsent(f[1], k -> new LinkedHashMap<>()).put(f[2], f.length > 3 ? f[3] : "");
                case "row" -> rows.add(new Row(f[1], Integer.parseInt(f[2]), f[3], f[4], f[5], f[6], f[7].split("\u001f", -1), f.length > 8 ? f[8] : ""));
                default -> throw new IllegalStateException(what + ": unrecognised result line: " + line);
            }
        }
        if (propNames.isEmpty()) {
            throw new IllegalStateException(what + ": the probe produced no property list - infrastructure failure, not a pass");
        }
        return new Probe(propNames, meta, shapes, tokens, rows);
    }

    private static List<Row> without(List<Row> rows, Set<Integer> ordinals) {
        return rows.stream().filter(r -> !ordinals.contains(r.ordinal())).toList();
    }

    private static List<Finding> compare(String where, List<String> propNames, List<Row> expected, List<Row> actual, Map<String, Integer> applied) {
        List<Finding> findings = new ArrayList<>();
        if (expected.size() != actual.size()) {
            findings.add(new Finding(where, "#rows", String.valueOf(expected.size()), String.valueOf(actual.size())));
            return findings;
        }
        int w = propNames.indexOf("width");
        int h = propNames.indexOf("height");
        for (int i = 0; i < expected.size(); i++) {
            Row e = expected.get(i);
            Row a = actual.get(i);
            String label = where + " #" + e.ordinal() + " " + e.kind() + " " + e.variant() + " [" + e.context() + "]"
                    + " legacyBox=" + e.values()[w] + "x" + e.values()[h] + " actualBox=" + a.values()[w] + "x" + a.values()[h]
                    + " legacy{" + e.diagnostics() + "} actual{" + a.diagnostics() + "}";
            if (e.ordinal() != a.ordinal() || !e.kind().equals(a.kind())) {
                findings.add(new Finding(label, "#structure", e.ordinal() + e.kind(), a.ordinal() + a.kind()));
                continue;
            }
            ParityNormalizer.Result r = ParityNormalizer.compare(
                    ParityNormalizer.side(propNames, e.values(), e.parentDisplay()), ParityNormalizer.side(propNames, a.values(), a.parentDisplay()));
            r.remaining().forEach(d -> findings.add(new Finding(label, d.property(), d.expected(), d.actual())));
            r.applied().forEach(rule -> applied.merge(rule.split(" ")[0], 1, Integer::sum));
        }
        return findings;
    }

    @BeforeAll
    static void measure() throws Exception {
        String generatedDir = System.getProperty("ashlar.cascade.dir");
        String bootJar = System.getProperty("ashlar.showcase.jar");
        if (generatedDir == null || bootJar == null || System.getProperty("ashlar.css.generated") == null) {
            throw new IllegalStateException("ashlar.cascade.dir, ashlar.showcase.jar and ashlar.css.generated must be set; run through Gradle (:showcase:test)");
        }
        var chromePath = HeadlessChrome.find();
        if (chromePath.isEmpty()) {
            if (System.getenv("CI") != null || "true".equalsIgnoreCase(System.getenv("ASHLAR_REQUIRE_BROWSER"))) {
                throw new IllegalStateException("Chrome is required (CI or ASHLAR_REQUIRE_BROWSER set) but was not found.");
            }
            System.out.println("[alert-parity] Chrome not found - Alert parity SKIPPED (not passed).");
            Assumptions.abort("Chrome not found; the Alert parity checks were not run");
        }
        HeadlessChrome chrome = new HeadlessChrome(chromePath.get());
        legacy = new AlertLegacyMarkup();
        runControlledFrames(chrome, Path.of(generatedDir));
        long start = System.nanoTime();
        runRealPages(chrome, Path.of(generatedDir), Path.of(bootJar));
        realSeconds = (System.nanoTime() - start) / 1_000_000_000L;
        writeReport();
    }

    // -- controlled oracle: legacy vs migrated, 22 configurations, Tailwind present and removed --

    private static void runControlledFrames(HeadlessChrome chrome, Path generatedDir) throws Exception {
        String environment = environment(generatedDir);
        String migrated = markup(false);
        String legacyMarkup = markup(true);
        StringBuilder frames = new StringBuilder();
        for (Config c : CONFIGS) {
            frames.append(frame(c.id() + "|L", c, with(ORACLE, SKIN_LINKS), "", legacyMarkup));
            frames.append(frame(c.id() + "|P", c, with(APP, SKIN_LINKS), "", migrated));
            frames.append(frame(c.id() + "|R", c, with(ASHLAR, SKIN_LINKS), environment, migrated));
            // The legacy Alert from the scoped stylesheet, in the same unstyled environment as |R.
            frames.append(frame(c.id() + "|S", c, with(SCOPED, SKIN_LINKS), environment, legacyMarkup));
        }
        for (Control control : CONTROL_LIST) {
            frames.append(frame("ctl:" + control.name() + "|P", config(control.config()), with(APP, SKIN_LINKS), control.css(), migrated));
        }
        // The customisation surface: an instance property wins over the variant, and an unlayered rule wins over the component.
        Shape plain = SHAPES.get(0);
        String inline = alert("info", plain).replace("data-alert data-alert-variant=\"info\">", "data-alert data-alert-variant=\"info\" style=\"--ash-alert-bg: rgb(1, 2, 3)\">");
        frames.append(frame("ovr:inline|P", CONFIGS.get(0), with(APP, SKIN_LINKS), "", inline));
        frames.append(frame("ovr:unlayered|P", CONFIGS.get(0), with(APP, SKIN_LINKS), "[data-alert]{border-left-width:6px}", alert("info", plain)));
        String probeJs;
        try (InputStream in = AlertParityTest.class.getResourceAsStream("/cascade/alert-probe.js")) {
            probeJs = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String page = "<!doctype html><html><body style=\"margin:0\">\n" + frames + "<script>" + probeJs + "</script>\n<script>"
                + "addEventListener('load',function(){setTimeout(function(){var out=['props\\t'+window.__ashAlert.PROPS.join(',')];"
                + "[].forEach.call(document.querySelectorAll('iframe'),function(f){var id=f.getAttribute('data-id');"
                + "window.__ashAlert.measure(f.contentWindow,f.contentDocument,id).forEach(function(r){out.push(r)});"
                + "window.__ashAlert.tokens(f.contentWindow,f.contentDocument,id).forEach(function(r){out.push(r)})});"
                + "var pre=document.createElement('pre');pre.id='ash-alert-frames';pre.textContent=out.join('\\n');document.body.appendChild(pre)},300)})"
                + "</script></body></html>";
        Probe probe;
        try (CascadeFixtureServer server = new CascadeFixtureServer(generatedDir)) {
            String dom = chrome.dumpDom(server.registerRaw(page), List.of("--hide-scrollbars"));
            probe = parse(lines(dom, "ash-alert-frames"), "controlled frames");
        }
        props = probe.props();
        Set<Integer> paragraphs = paragraphOrdinals();
        for (Config c : CONFIGS) {
            List<Row> legacyRows = probe.phase(c.id() + "|L");
            CONTROLLED_ROWS.put(c.id(), legacyRows.size());
            CONTROLLED_PRESENT.put(c.id(), compare(c.id() + " (Tailwind present)", props, legacyRows, probe.phase(c.id() + "|P"), APPLIED_CONTROLLED));
            // Tailwind removed: against the legacy Alert given the same unstyled surroundings, for every shape ...
            CONTROLLED_REMOVED_SCOPED.put(c.id(), compare(c.id() + " (Tailwind removed vs scoped legacy)", props, probe.phase(c.id() + "|S"), probe.phase(c.id() + "|R"), APPLIED_CONTROLLED));
            // ... and against the full legacy rendering, for the shapes whose body is plain text (a caller's paragraph
            // has no margin under Tailwind's preflight and the browser's own margin without it, with or without Alert).
            CONTROLLED_REMOVED_FULL.put(c.id(), compare(c.id() + " (Tailwind removed vs full legacy)", props, without(legacyRows, paragraphs), without(probe.phase(c.id() + "|R"), paragraphs), APPLIED_CONTROLLED));
            // Fidelity of the scoped legacy stylesheet: it must render the legacy Alert exactly as the full one does.
            SCOPED_FIDELITY.put(c.id(), compare(c.id() + " (scoped legacy vs full legacy)", props, without(legacyRows, paragraphs), without(probe.phase(c.id() + "|S"), paragraphs), APPLIED_SCOPED));
        }
        for (Control control : CONTROL_LIST) {
            List<Row> expected = probe.phase(control.config() + "|L");
            CONTROLS.put(control.name(), compare("control: " + control.name(), props, expected, probe.phase("ctl:" + control.name() + "|P"), new TreeMap<>()));
        }
        FRAME_PROBES.put("controlled", probe);
    }

    // -- real pages: every Alert on the showcase page, 22 configurations, in parallel --

    private static String legacyConfigJson(String phase, String stylesheet, boolean detach) {
        return "{\"phase\":\"" + phase + "\",\"legacy\":{\"stylesheet\":\"" + stylesheet + "\",\"detach\":" + detach + ",\"roles\":" + legacy.configJson() + "}}";
    }

    private static void runRealPages(HeadlessChrome chrome, Path generatedDir, Path bootJar) throws Exception {
        byte[] oracleCss = Files.readAllBytes(generatedDir.resolve("alert-oracle.css"));
        byte[] ashlarCss = Files.readAllBytes(Path.of(System.getProperty("ashlar.css.generated")));
        String environment = environment(generatedDir);
        byte[] removedCss = (new String(ashlarCss, StandardCharsets.UTF_8) + "\n" + environment).getBytes(StandardCharsets.UTF_8);
        byte[] scopedCss = Files.readAllBytes(generatedDir.resolve("alert-legacy-scoped.css"));
        String presentConfig = legacyConfigJson("ash", ORACLE_VIRTUAL, false);
        // Tailwind removed: the migrated Alert is measured in the unstyled page, then the legacy Alert is measured on the
        // very same elements in the very same page with only its own utilities added - so the surroundings are identical.
        String removedConfig = legacyConfigJson("removed", SCOPED_VIRTUAL, true);
        Map<String, Probe> present = new ConcurrentHashMap<>();
        Map<String, Probe> removed = new ConcurrentHashMap<>();
        int workers = Math.max(1, Math.min(3, Runtime.getRuntime().availableProcessors() / 2));
        List<RealPageProxy> proxies = Collections.synchronizedList(new ArrayList<>());
        ThreadLocal<RealPageProxy> proxyOfThread = new ThreadLocal<>();
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        try (RunningShowcase app = RunningShowcase.start(bootJar)) {
            List<Future<?>> futures = new ArrayList<>();
            for (Config config : CONFIGS) {
                futures.add(pool.submit(() -> {
                    RealPageProxy proxy = proxyOfThread.get();
                    if (proxy == null) {
                        proxy = new RealPageProxy(app.port());
                        proxies.add(proxy);
                        proxyOfThread.set(proxy);
                    }
                    String path = ALERT_PAGE + (config.skin() == null ? "" : "?skin=" + config.skin());
                    proxy.configure(new RealPageProxy.Load(config.theme(), "alert", null, "/cascade/alert-probe.js", presentConfig,
                            List.of(), Map.of(ORACLE_VIRTUAL, oracleCss), null));
                    present.put(config.id(), parse(lines(chrome.dumpDom(proxy.url(path), List.of("--hide-scrollbars")), "ash-alert-results"), config.id() + " present"));
                    proxy.configure(new RealPageProxy.Load(config.theme(), "alert", removedCss, "/cascade/alert-probe.js", removedConfig,
                            List.of(), Map.of(SCOPED_VIRTUAL, scopedCss), null));
                    removed.put(config.id(), parse(lines(chrome.dumpDom(proxy.url(path), List.of("--hide-scrollbars")), "ash-alert-results"), config.id() + " removed"));
                    return null;
                }));
            }
            for (Future<?> f : futures) {
                f.get();
            }
            countAlertsOnEveryPage(app.port());
        } finally {
            pool.shutdownNow();
            proxies.forEach(RealPageProxy::close);
        }
        for (Config config : CONFIGS) {
            Probe p = present.get(config.id());
            Probe r = removed.get(config.id());
            REAL_PROBES.put(config.id() + "|present", p);
            REAL_PROBES.put(config.id() + "|removed", r);
            String where = "alert page " + config.id();
            for (Probe probed : List.of(p, r)) {
                if (!config.theme().equals(probed.meta().get("data-theme")) || !(config.skin() == null ? "" : config.skin()).equals(probed.meta().get("data-skin"))) {
                    REAL_PRESENT.add(new Finding(where, "#configuration", config.id(), probed.meta().get("data-theme") + "/" + probed.meta().get("data-skin")));
                }
                if (probed.meta().containsKey("error")) {
                    REAL_PRESENT.add(new Finding(where, "#error", "", probed.meta().get("error")));
                }
            }
            REAL_PRESENT.addAll(compare(where + " (Tailwind present)", p.props(), p.phase("legacy"), p.phase("ash"), APPLIED_REAL_PRESENT));
            REAL_REMOVED.addAll(compare(where + " (Tailwind removed)", p.props(), r.phase("legacy"), r.phase("removed"), APPLIED_REAL_REMOVED));
            realComparisonsPerMode += (int) p.phase("legacy").stream().filter(row -> row.kind().equals("alert")).count();
            skinFacts(config, p);
            if (config.id().equals("light/none")) {
                p.shapes().forEach(AlertParityTest::shapeProblems);
            }
        }
    }

    /** Every catalog page, the home page and the index, as served: how many Alerts each renders. */
    private static void countAlertsOnEveryPage(int port) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        List<String> paths = new ArrayList<>(List.of("/ashlar/", "/ashlar/components"));
        for (ComponentPage page : ComponentCatalog.PAGES) {
            paths.add("/ashlar/components/" + page.slug());
        }
        for (String path : paths) {
            HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            assertEquals(200, response.statusCode(), path);
            int hooks = (int) Pattern.compile("\\bdata-alert\\b(?!-)").matcher(response.body()).results().count();
            assertEquals(AlertLegacyMarkup.count(response.body()), hooks, path + ": every data-alert must be a migrated Alert");
            ALERTS_PER_PAGE.put(path, hooks);
        }
    }

    // -- skins: what each skin that targets the Alert (and the radius every skin re-points) must do to it --

    private static void skinFacts(Config config, Probe p) {
        List<Row> ash = p.phase("ash");
        Map<String, String> tok = p.tokens().get("page");
        String skin = config.skin() == null ? "none" : config.skin();
        String where = "alert page " + config.id() + ": ";
        for (Row row : ash) {
            String variant = row.variant().substring(0, row.variant().indexOf('/'));
            String hue = HUE.get(variant);
            switch (row.kind()) {
                case "alert" -> {
                    String left = switch (skin) {
                        case "civic" -> "4px";
                        case "ledger" -> "3px";
                        default -> "1px";
                    };
                    expect(where + "#" + row.ordinal() + " left border width", left, p.value(row, "border-left-width"));
                    for (String side : List.of("top", "right", "bottom")) {
                        expect(where + "#" + row.ordinal() + " " + side + " border width", "1px", p.value(row, "border-" + side + "-width"));
                    }
                    if (config.skin() != null) {
                        // Only a skin defines the whole palette; with no skin the colours are parity-checked against the legacy rendering.
                        String shade = config.theme().equals("dark") ? "900" : "200";
                        String tint = tok.get("--color-" + hue + "-" + shade);
                        for (String side : List.of("top", "right", "bottom")) {
                            expect(where + "#" + row.ordinal() + " " + side + " border colour is the " + shade + " tint", tint, p.value(row, "border-" + side + "-color"));
                        }
                        if (skin.equals("ledger")) {
                            expect(where + "#" + row.ordinal() + " ledger left colour is the variant's 600", tok.get("--color-" + hue + "-600"), p.value(row, "border-left-color"));
                        } else {
                            expect(where + "#" + row.ordinal() + " left border colour follows the tint", tint, p.value(row, "border-left-color"));
                        }
                    }
                    if (config.skin() == null) {
                        expect(where + "#" + row.ordinal() + " default radius", "8px", p.value(row, "border-top-left-radius"));
                    } else {
                        assertTrue(!tok.get("--radius-lg-raw").isEmpty(), skin + " is expected to define --radius-lg");
                        expect(where + "#" + row.ordinal() + " radius is the skin's --radius-lg", tok.get("--radius-lg"), p.value(row, "border-top-left-radius"));
                    }
                }
                case "icon" -> {
                    boolean foundry = skin.equals("foundry");
                    for (String side : List.of("top", "right", "bottom", "left")) {
                        expect(where + "#" + row.ordinal() + " icon padding-" + side, foundry ? "3px" : "0px", p.value(row, "padding-" + side));
                    }
                    expect(where + "#" + row.ordinal() + " icon box-sizing", foundry ? "content-box" : "border-box", p.value(row, "box-sizing"));
                    expect(where + "#" + row.ordinal() + " icon radius", foundry ? "3px" : "0px", p.value(row, "border-top-left-radius"));
                    assertEquals(foundry, !p.value(row, "background-color").equals("rgba(0, 0, 0, 0)"), where + "#" + row.ordinal() + " icon plate background in " + skin);
                }
                case "p" -> expect(where + "#" + row.ordinal() + " paragraph line-height", skin.equals("harbor") ? "23.1px" : "20px", p.value(row, "line-height"));
                default -> {
                }
            }
        }
        expect(where + "the icon is the first direct svg child of every alert (what Foundry targets)", p.meta().get("alert-count"), p.meta().get("icon-is-first-svg"));
    }

    private static void expect(String what, String expected, String actual) {
        if (expected == null || !expected.equals(actual)) {
            SKIN_PROBLEMS.add(what + ": expected " + expected + " but was " + actual);
        }
    }

    private static final Set<String> ALLOWED_TOKEN_PATTERNS = Set.of("inline-block", "shrink-0");

    private static void shapeProblems(String[] s) {
        String where = "alert #" + s[1];
        if (!s[2].equals("div")) {
            SHAPE_PROBLEMS.add(where + ": the alert is a <" + s[2] + ">, expected <div>");
        }
        if (!s[3].equals("class,data-alert,data-alert-variant")) {
            SHAPE_PROBLEMS.add(where + ": attributes " + s[3]);
        }
        if (!s[4].equals("svg,div")) {
            SHAPE_PROBLEMS.add(where + ": children " + s[4] + ", expected svg,div");
        }
        if (!s[5].equals("aria-hidden,class,fill,stroke,stroke-linecap,stroke-linejoin,stroke-width,viewBox") || !s[6].equals("true")) {
            SHAPE_PROBLEMS.add(where + ": the icon is not the decorative aria-hidden svg: " + s[5] + " aria-hidden=" + s[6]);
        }
        if (!s[8].matches("span(,h3)?(,div)?")) {
            SHAPE_PROBLEMS.add(where + ": content children " + s[8] + ", expected the label, then an optional h3, then an optional div");
        }
        if (!s[9].matches("(Information|Success|Warning|Error):")) {
            SHAPE_PROBLEMS.add(where + ": severity label " + s[9]);
        }
        if (!s[10].isEmpty() && !s[10].equals("h3")) {
            SHAPE_PROBLEMS.add(where + ": the title is a <" + s[10] + ">, expected <h3>");
        }
        for (String token : s[11].split(" ")) {
            if (!token.startsWith("ash-") && !ALLOWED_TOKEN_PATTERNS.contains(token)) {
                SHAPE_PROBLEMS.add(where + ": a class that is not Ashlar's: " + token);
            }
        }
    }

    private static void writeReport() throws IOException {
        StringBuilder sb = new StringBuilder("Alert parity (real Chrome; no stored baseline).\n\n");
        sb.append("CONTROLLED ORACLE: ").append(CONFIGS.size()).append(" configurations x (Tailwind present, Tailwind removed), ")
                .append(CONTROLLED_ROWS.values().stream().mapToInt(Integer::intValue).sum()).append(" legacy rows measured\n");
        sb.append("   differences (Tailwind present): ").append(CONTROLLED_PRESENT.values().stream().mapToInt(List::size).sum())
                .append(" ; (removed vs scoped legacy): ").append(CONTROLLED_REMOVED_SCOPED.values().stream().mapToInt(List::size).sum())
                .append(" ; (removed vs full legacy, plain-text bodies): ").append(CONTROLLED_REMOVED_FULL.values().stream().mapToInt(List::size).sum()).append('\n');
        sb.append("   normalizations used (rule: uses): ").append(APPLIED_CONTROLLED).append('\n');
        sb.append("   scoped legacy stylesheet vs full legacy, differences: ").append(SCOPED_FIDELITY.values().stream().mapToInt(List::size).sum())
                .append(" ; normalizations used: ").append(APPLIED_SCOPED).append('\n');
        sb.append("NEGATIVE CONTROLS (each must produce a difference in the named property):\n");
        for (Control c : CONTROL_LIST) {
            List<String> found = CONTROLS.get(c.name()).stream().map(Finding::property).distinct().toList();
            sb.append("   ").append(found.contains(c.expectedProperty()) ? "DETECTED " : "MISSED   ").append(c.name()).append(" -> ").append(found).append('\n');
        }
        sb.append("REAL PAGE: the Alert page x ").append(CONFIGS.size()).append(" configurations (parallel, ").append(realSeconds).append(" s)\n");
        sb.append("   alerts per page (every catalog page, home and index): ").append(ALERTS_PER_PAGE.entrySet().stream().filter(e -> e.getValue() > 0).toList())
                .append(" ; pages scanned ").append(ALERTS_PER_PAGE.size()).append('\n');
        sb.append("   instance comparisons per mode (instances x configurations): ").append(realComparisonsPerMode).append('\n');
        sb.append("   differences (Tailwind present): ").append(REAL_PRESENT.size()).append(" ; (Tailwind removed): ").append(REAL_REMOVED.size()).append('\n');
        sb.append("   normalizations used, Tailwind present: ").append(APPLIED_REAL_PRESENT).append('\n');
        sb.append("   normalizations used, Tailwind removed: ").append(APPLIED_REAL_REMOVED).append('\n');
        sb.append("   skin facts not met: ").append(SKIN_PROBLEMS.size()).append(" ; DOM shape problems: ").append(SHAPE_PROBLEMS.size()).append('\n');
        appendFindings(sb, "FINDINGS, controlled, Tailwind present", CONTROLLED_PRESENT.values().stream().flatMap(List::stream).toList());
        appendFindings(sb, "FINDINGS, controlled, removed vs scoped legacy", CONTROLLED_REMOVED_SCOPED.values().stream().flatMap(List::stream).toList());
        appendFindings(sb, "FINDINGS, controlled, removed vs full legacy", CONTROLLED_REMOVED_FULL.values().stream().flatMap(List::stream).toList());
        appendFindings(sb, "FINDINGS, scoped legacy vs full legacy", SCOPED_FIDELITY.values().stream().flatMap(List::stream).toList());
        appendFindings(sb, "FINDINGS, real page, Tailwind present", REAL_PRESENT);
        appendFindings(sb, "FINDINGS, real page, Tailwind removed", REAL_REMOVED);
        SKIN_PROBLEMS.stream().limit(20).forEach(p -> sb.append("   SKIN ").append(p).append('\n'));
        SHAPE_PROBLEMS.stream().limit(20).forEach(p -> sb.append("   SHAPE ").append(p).append('\n'));
        Path out = Path.of(System.getProperty("ashlar.cascade.dir")).resolve("../../reports/parity/alert.txt").normalize();
        Files.createDirectories(out.getParent());
        Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
        System.out.println(sb);
    }

    private static void appendFindings(StringBuilder sb, String title, List<Finding> findings) {
        if (findings.isEmpty()) {
            return;
        }
        sb.append(title).append(" (config | instance | property | expected -> actual), grouped by property:\n");
        findings.stream().collect(Collectors.groupingBy(Finding::property, TreeMap::new, Collectors.toList())).forEach((property, list) -> {
            sb.append("   ").append(property).append(" x").append(list.size()).append('\n');
            list.stream().limit(5).forEach(f -> sb.append("      ").append(f.line()).append('\n'));
        });
    }

    // ---- gates ----

    @Test
    void theConfigurationsAreTheTwentyTwoThemeAndSkinCombinations() {
        assertEquals(22, CONFIGS.size());
        assertEquals(22, CONFIGS.stream().map(Config::id).distinct().count());
        assertTrue(CONFIGS.stream().anyMatch(c -> c.id().equals("light/none")) && CONFIGS.stream().anyMatch(c -> c.id().equals("dark/veridian")));
    }

    @Test
    void controlledOracleWithTailwindPresentMatchesTheLegacyAlertInEveryConfiguration() {
        assertEquals(List.of(), CONTROLLED_PRESENT.values().stream().flatMap(List::stream).map(Finding::line).limit(25).toList());
        // Per variant: titled 6 rows, body-only 5, title-only 5, long word 6, paragraph 7 (alert, icon, content, label, title, body, p).
        CONFIGS.forEach(c -> assertEquals(4 * (6 + 5 + 5 + 6 + 7), CONTROLLED_ROWS.get(c.id()), c.id() + ": rows measured"));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheLegacyAlertGivenTheSameSurroundings() {
        assertEquals(List.of(), CONTROLLED_REMOVED_SCOPED.values().stream().flatMap(List::stream).map(Finding::line).limit(25).toList());
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheFullLegacyRenderingForPlainTextBodies() {
        assertEquals(List.of(), CONTROLLED_REMOVED_FULL.values().stream().flatMap(List::stream).map(Finding::line).limit(25).toList());
    }

    @Test
    void theScopedLegacyStylesheetRendersTheLegacyAlertExactlyAsTheFullOneDoes() {
        // The Tailwind-removed real-page comparison trusts the scoped stylesheet as the legacy rendering; this is the proof.
        assertEquals(List.of(), SCOPED_FIDELITY.values().stream().flatMap(List::stream).map(Finding::line).limit(25).toList());
        assertEquals(22, SCOPED_FIDELITY.size());
        assertEquals(Map.of(), APPLIED_SCOPED, "the scoped legacy rendering must match the full one without any normalization");
    }

    @Test
    void everyNegativeControlIsDetectedInTheProperty() {
        for (Control control : CONTROL_LIST) {
            List<String> found = CONTROLS.get(control.name()).stream().map(Finding::property).toList();
            assertTrue(found.contains(control.expectedProperty()),
                    "control '" + control.name() + "' must differ in " + control.expectedProperty() + " but produced " + found);
        }
    }

    @Test
    void theLongUnbrokenWordIsHeldInsideTheAlertByTheContentColumn() {
        Probe probe = FRAME_PROBES.get("controlled");
        int w = props.indexOf("width");
        List<Row> rows = probe.phase("light/none|P");
        Row longWord = rows.stream().filter(r -> r.kind().equals("content") && r.ordinal() == 4).findFirst().orElseThrow();
        Row alert = rows.stream().filter(r -> r.kind().equals("alert") && r.ordinal() == 4).findFirst().orElseThrow();
        assertEquals("380px", alert.values()[w], "the alert fills the 380px column");
        assertTrue(Double.parseDouble(longWord.values()[w].replace("px", "")) < 380, "the content column must stay inside the alert: " + longWord.values()[w]);
    }

    @Test
    void anInstanceCustomPropertyBeatsTheVariantAndAnUnlayeredRuleBeatsTheComponent() {
        Probe probe = FRAME_PROBES.get("controlled");
        Row inline = probe.phase("ovr:inline|P").stream().filter(r -> r.kind().equals("alert")).findFirst().orElseThrow();
        assertEquals("rgb(1, 2, 3)", inline.values()[props.indexOf("background-color")]);
        Row unlayered = probe.phase("ovr:unlayered|P").stream().filter(r -> r.kind().equals("alert")).findFirst().orElseThrow();
        assertEquals("6px", unlayered.values()[props.indexOf("border-left-width")]);
        assertEquals("1px", unlayered.values()[props.indexOf("border-top-width")]);
    }

    @Test
    void everyRealAlertOnTheShowcasePageMatchesItsLegacyRenderingWithTailwindPresent() {
        assertEquals(List.of(), REAL_PRESENT.stream().map(Finding::line).limit(25).toList(), REAL_PRESENT.size() + " differences in total");
    }

    @Test
    void everyRealAlertOnTheShowcasePageMatchesItsLegacyRenderingWithTailwindRemoved() {
        assertEquals(List.of(), REAL_REMOVED.stream().map(Finding::line).limit(25).toList(), REAL_REMOVED.size() + " differences in total");
    }

    @Test
    void realPageCoverageIsAllSevenInstancesInAll22ConfigurationsAndNoOtherPageRendersAnAlert() {
        assertEquals(7 * 22, realComparisonsPerMode);
        Map<String, Integer> withAlerts = ALERTS_PER_PAGE.entrySet().stream().filter(e -> e.getValue() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));
        assertEquals(Map.of(ALERT_PAGE, 7), withAlerts);
        assertEquals(2 + ComponentCatalog.PAGES.size(), ALERTS_PER_PAGE.size(), "every catalog page, the home page and the index were scanned");
        for (Config c : CONFIGS) {
            Probe p = REAL_PROBES.get(c.id() + "|present");
            assertEquals("7", p.meta().get("alert-count"), c.id());
            assertEquals("0", p.meta().get("alerts-without-a-box"), c.id() + ": every alert must be rendered, none is hidden");
            // The seven showcase instances, in document order: variant and whether it has a title and a body.
            assertEquals(List.of("warning/tb", "info/tb", "success/tb", "warning/tb", "error/tb", "info/-b", "warning/-b"),
                    p.phase("ash").stream().filter(r -> r.kind().equals("alert")).map(Row::variant).toList(), c.id());
        }
    }

    @Test
    void theAlertDomShapeIsPreservedAndCarriesOnlyAshlarClasses() {
        assertEquals(List.of(), SHAPE_PROBLEMS);
        assertEquals(7, REAL_PROBES.get("light/none|present").shapes().size());
    }

    @Test
    void whileTheScopedLegacySheetIsLinkedTheLegacyNamesAreOffEveryOtherElementAndAreRestoredExactly() {
        for (Config c : CONFIGS) {
            Probe removed = REAL_PROBES.get(c.id() + "|removed");
            assertEquals("0", removed.meta().get("detach-restore-mismatch"), c.id() + ": detached class names were not restored exactly");
            assertTrue(Integer.parseInt(removed.meta().get("detached-elements")) > 20, c.id() + ": the page around the alert uses the legacy names, so many elements must have been detached: " + removed.meta().get("detached-elements"));
            assertEquals("0", REAL_PROBES.get(c.id() + "|present").meta().getOrDefault("detached-elements", "0"), c.id() + ": nothing is detached while Tailwind is present");
        }
    }

    @Test
    void theSeverityLabelIsVisuallyHiddenButStillThereForEveryVariantAndConfiguration() {
        for (Config c : CONFIGS) {
            Probe p = REAL_PROBES.get(c.id() + "|present");
            List<Row> labels = p.phase("ash").stream().filter(r -> r.kind().equals("label")).toList();
            assertEquals(7, labels.size(), c.id());
            for (Row label : labels) {
                assertEquals("absolute", p.value(label, "position"), c.id());
                assertEquals("1px", p.value(label, "width"), c.id());
                assertEquals("1px", p.value(label, "height"), c.id());
                assertEquals("hidden", p.value(label, "overflow-x"), c.id());
                assertEquals("inset(50%)", p.value(label, "clip-path"), c.id());
                assertEquals("nowrap", p.value(label, "white-space"), c.id());
            }
        }
    }

    @Test
    void theSkinsThatTargetTheAlertStillDoTheirJobAndEverySkinsRadiusReachesIt() {
        assertEquals(List.of(), SKIN_PROBLEMS.stream().limit(25).toList(), SKIN_PROBLEMS.size() + " facts not met");
    }

    private static String escapeClass(String token) {
        return "." + token.replaceAll("([:/.])", "\\\\$1");
    }

    @Test
    void theOracleIsNotVacuousEveryLegacyClassIsCompiledInTheCumulativeAndScopedStylesheets() throws IOException {
        Path dir = Path.of(System.getProperty("ashlar.cascade.dir"));
        String oracle = Files.readString(dir.resolve("alert-oracle.css"), StandardCharsets.UTF_8);
        String scoped = Files.readString(dir.resolve("alert-legacy-scoped.css"), StandardCharsets.UTF_8);
        BadgeLegacyMarkup badge = new BadgeLegacyMarkup();
        for (String token : legacy.allTokens()) {
            assertTrue(oracle.contains(escapeClass(token) + "{") || oracle.contains(escapeClass(token) + ":") || oracle.contains(escapeClass(token) + ","),
                    "the Alert oracle does not compile " + token);
            // The scoped stylesheet reads no source but alert-legacy.html, so this also proves its @source path resolves.
            assertTrue(scoped.contains(escapeClass(token) + "{") || scoped.contains(escapeClass(token) + ":") || scoped.contains(escapeClass(token) + ","),
                    "the scoped stylesheet does not compile " + token);
        }
        // Cumulative: the Badge's legacy classes are still in the Alert oracle, so the nested import works.
        for (String classes : badge.variants().values()) {
            for (String token : classes.split("\\s+")) {
                assertTrue(oracle.contains(escapeClass(token)), "the Alert oracle lost the Badge legacy class " + token);
            }
        }
        assertFalse(scoped.contains("html,:host"), "the scoped legacy stylesheet must not carry Tailwind's page-wide base");
    }

    @Test
    void normalizationsStayWithinTheApprovedRulesAndTailwindPresentNeedsNone() {
        assertEquals(Map.of(), APPLIED_REAL_PRESENT, "with Tailwind present the Alert must match exactly");
        for (Map<String, Integer> applied : List.of(APPLIED_CONTROLLED, APPLIED_REAL_REMOVED)) {
            assertTrue(Set.of("H2", "H3").containsAll(applied.keySet()), "unexpected normalization rule: " + applied);
        }
    }

    @AfterAll
    static void clear() {
        CONTROLLED_PRESENT.clear();
        CONTROLLED_REMOVED_SCOPED.clear();
        CONTROLLED_REMOVED_FULL.clear();
        SCOPED_FIDELITY.clear();
        CONTROLS.clear();
        FRAME_PROBES.clear();
        REAL_PROBES.clear();
        REAL_PRESENT.clear();
        REAL_REMOVED.clear();
    }
}
