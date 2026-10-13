package dev.redspartanlabs.ashlar.showcase.cascade;

import dev.redspartanlabs.ashlar.divider.DividerStyles;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Divider's parity gates, measured in real Chrome.
 *
 * <p>Evidence comes in two kinds, and neither stores a baseline. (1) A controlled oracle: the Divider's legacy
 * Tailwind markup (compiled by the real Tailwind CLI) next to its migrated markup, in the same browser, in ten
 * layouts (block, column flex, row flex and others) for each of the 22 theme and skin configurations, with Tailwind
 * present and with Tailwind removed. (2) Every Divider on the real showcase pages (the Divider page and the Card
 * page), in the same 22 configurations: the real page is measured as served, then the very same elements get their
 * legacy classes back and are measured again, in the same run. Differences are compared with
 * {@link ParityNormalizer} (exact equality except H1 to H4, with their preconditions). Each row also carries the
 * divider's box in its parent's coordinates, so a margin, an offset or a stretch shows even where a computed value
 * is "auto". Negative controls show each gate can fail.
 *
 * <p>Evidence is Chrome only. Computed-style and box parity is the visual evidence; there is no screenshot comparison.
 */
class DividerParityTest {

    private static final String ASHLAR = "/ashlar-real/ashlar.css";
    private static final String ORACLE = "/generated/divider-oracle.css";
    private static final String APP = "/css/app.css";
    private static final String ORACLE_VIRTUAL = "/__parity/divider-oracle.css";
    private static final String SCOPED = "/generated/divider-legacy-scoped.css";
    private static final String SCOPED_VIRTUAL = "/__parity/divider-legacy-scoped.css";
    private static final String DIVIDER_PAGE = "/ashlar/components/divider";
    private static final String CARD_PAGE = "/ashlar/components/card";
    private static final List<String> REAL_PAGES = List.of(DIVIDER_PAGE, CARD_PAGE);
    private static final List<String> SKINS = List.of("axiom", "civic", "fathom", "foundry", "harbor", "keystone", "ledger", "sterling", "summit", "veridian");
    private static final List<String> SKIN_LINKS = SKINS.stream().map(s -> "/css/theme-" + s + ".css").toList();

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

    /** A container the controlled oracle puts both orientations in. Wrapper and sibling styles are inline, so they are identical on every side. */
    private record Layout(String name, String style, String siblingStyle) {
    }

    private static final List<Layout> LAYOUTS = List.of(
            new Layout("block", "box-sizing:border-box;width:300px", ""),
            new Layout("between-paragraphs", "box-sizing:border-box;width:300px", "display:block;margin:12px 0"),
            new Layout("column-flex", "box-sizing:border-box;display:flex;flex-direction:column;width:300px", ""),
            new Layout("column-flex-centered", "box-sizing:border-box;display:flex;flex-direction:column;align-items:center;width:300px;height:160px", ""),
            new Layout("row-flex", "box-sizing:border-box;display:flex;flex-direction:row;width:300px;height:60px", ""),
            new Layout("row-flex-centered", "box-sizing:border-box;display:flex;flex-direction:row;align-items:center;width:300px;height:60px", ""),
            new Layout("toolbar", "box-sizing:border-box;display:flex;height:40px;align-items:center;gap:12px;padding:0 12px;width:300px", ""),
            new Layout("row-flex-wrap", "box-sizing:border-box;display:flex;flex-wrap:wrap;width:120px", ""),
            new Layout("grid", "box-sizing:border-box;display:grid;width:300px;height:120px", ""),
            new Layout("inline-text", "box-sizing:border-box;width:300px;line-height:20px", "font-size:14px"));

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
    private static final Map<String, Integer> DIVIDERS_PER_PAGE = new TreeMap<>();
    private static final List<String> SKIN_PROBLEMS = new ArrayList<>();
    private static final List<String> SHAPE_PROBLEMS = new ArrayList<>();
    private static int realComparisonsPerMode;
    private static long realSeconds;

    // ---- markup ----

    private static DividerLegacyMarkup legacy;

    /** The two orientations exactly as divider.jte renders them (the template itself is measured on the real pages). */
    private static String horizontal() {
        return "<hr class=\"" + DividerStyles.horizontal() + "\" data-divider data-divider-orientation=\"horizontal\">";
    }

    private static String vertical() {
        return "<div role=\"separator\" aria-orientation=\"vertical\" class=\"" + DividerStyles.vertical()
                + "\" data-divider data-divider-orientation=\"vertical\"></div>";
    }

    private static String layoutMarkup(Layout layout) {
        String sibling = "<span" + (layout.siblingStyle().isEmpty() ? "" : " style=\"" + layout.siblingStyle() + "\"") + ">";
        return "<div data-layout=\"" + layout.name() + "\" style=\"" + layout.style() + "\">"
                + sibling + "A</span>" + horizontal() + sibling + "B</span>" + vertical() + sibling + "C</span></div>";
    }

    private static String markup(boolean legacyClasses) {
        StringBuilder sb = new StringBuilder();
        for (Layout layout : LAYOUTS) {
            sb.append("<div style=\"margin:8px 0\">").append(layoutMarkup(layout)).append("</div>");
        }
        return legacyClasses ? legacy.rewrite(sb.toString()) : sb.toString();
    }

    private static String frame(String id, Config config, List<String> sheets, String extraCss, String body) {
        StringBuilder links = new StringBuilder();
        sheets.forEach(href -> links.append("<link rel=\"stylesheet\" href=\"").append(href).append("\">"));
        String skin = config.skin() == null ? "" : " data-skin=\"" + config.skin() + "\"";
        String doc = "<!doctype html><html data-theme=\"" + config.theme() + "\"" + skin + "><head>" + links + "<style>" + extraCss
                + "</style></head><body style=\"margin:0;padding:8px\">" + body + "</body></html>";
        return "<iframe data-id=\"" + id + "\" style=\"width:440px;height:1700px;border:0\" srcdoc=\""
                + doc.replace("&", "&amp;").replace("\"", "&quot;") + "\"></iframe>\n";
    }

    private static List<String> with(String first, List<String> rest) {
        List<String> all = new ArrayList<>();
        all.add(first);
        all.addAll(rest);
        return all;
    }

    /**
     * What the page keeps when Tailwind is removed: the base font and line height Tailwind's stylesheet gave it, plus the
     * showcase's OWN dark colour scheme (a rule in its application CSS, not Tailwind's). Without it dark pages would draw
     * their default text in black, and that would be a difference in the test page, not in the Divider.
     */
    private static String environment(Path generatedDir) throws IOException {
        return PageEnvironment.tailwindRemovedRules(generatedDir.resolve("phase-c-baseline.css")) + ":root[data-theme=\"dark\"]{color-scheme:dark}\n";
    }

    // ---- control experiments: each breaks the migrated Divider in ONE way and must be detected ----

    private record Control(String name, String config, String css, String expectedProperty) {
    }

    private static final List<Control> CONTROL_LIST = List.of(
            new Control("wrong light colour", "light/none", ".ash-divider{--ash-divider-color:red}", "border-top-color"),
            new Control("wrong dark colour", "dark/none", ".ash-divider:where([data-theme=dark],[data-theme=dark] *){--ash-divider-color:var(--color-slate-200)}", "border-top-color"),
            new Control("colour ignores the skin", "light/civic", ".ash-divider{--ash-divider-color:#e2e8f0}", "border-top-color"),
            new Control("horizontal keeps the browser margin", "light/none", ".ash-divider--horizontal{margin:8px 0}", "margin-top"),
            new Control("horizontal is not full width", "light/none", ".ash-divider--horizontal{width:50%}", "width"),
            new Control("horizontal height left to the browser (stretches in a row)", "light/none", ".ash-divider--horizontal{height:auto}", "height"),
            new Control("horizontal line two pixels thick", "light/none", ".ash-divider--horizontal{border-top-width:2px}", "border-top-width"),
            new Control("horizontal keeps the browser's inset style", "light/none", ".ash-divider--horizontal{border-top-style:inset}", "border-top-style"),
            new Control("horizontal keeps the browser's grey colour", "light/none", ".ash-divider--horizontal{color:red}", "color"),
            new Control("horizontal overflow reset", "light/none", ".ash-divider--horizontal{overflow:visible}", "overflow-x"),
            new Control("vertical is two pixels wide", "light/none", ".ash-divider--vertical{width:2px}", "width"),
            new Control("vertical no longer stretches", "light/none", ".ash-divider--vertical{align-self:auto}", "align-self"),
            new Control("vertical is a block", "light/none", ".ash-divider--vertical{display:block}", "display"),
            new Control("vertical is a background box, not a border", "light/none", ".ash-divider--vertical{border-left:0;width:1px;background-color:currentColor}", "border-left-width"),
            new Control("vertical gains a right border", "light/none", ".ash-divider--vertical{border-right:1px solid red}", "border-right-width"),
            new Control("vertical gains a margin", "light/none", ".ash-divider--vertical{margin-left:4px}", "margin-left"));

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

    private static List<Finding> compare(String where, List<String> propNames, List<Row> expected, List<Row> actual, Map<String, Integer> applied) {
        List<Finding> findings = new ArrayList<>();
        if (expected.size() != actual.size()) {
            findings.add(new Finding(where, "#rows", String.valueOf(expected.size()), String.valueOf(actual.size())));
            return findings;
        }
        int w = propNames.indexOf("@w");
        int h = propNames.indexOf("@h");
        for (int i = 0; i < expected.size(); i++) {
            Row e = expected.get(i);
            Row a = actual.get(i);
            String label = where + " #" + e.ordinal() + " " + e.variant() + " [" + e.context() + "]"
                    + " legacyBox=" + e.values()[w] + "x" + e.values()[h] + " actualBox=" + a.values()[w] + "x" + a.values()[h]
                    + " legacy{" + e.diagnostics() + "} actual{" + a.diagnostics() + "}";
            if (e.ordinal() != a.ordinal() || !e.kind().equals(a.kind()) || !e.variant().equals(a.variant())) {
                findings.add(new Finding(label, "#structure", e.ordinal() + e.variant(), a.ordinal() + a.variant()));
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
            System.out.println("[divider-parity] Chrome not found - Divider parity SKIPPED (not passed).");
            Assumptions.abort("Chrome not found; the Divider parity checks were not run");
        }
        HeadlessChrome chrome = new HeadlessChrome(chromePath.get());
        legacy = new DividerLegacyMarkup();
        runControlledFrames(chrome, Path.of(generatedDir));
        long start = System.nanoTime();
        runRealPages(chrome, Path.of(generatedDir), Path.of(bootJar));
        realSeconds = (System.nanoTime() - start) / 1_000_000_000L;
        writeReport();
    }

    // -- controlled oracle: legacy vs migrated, 22 configurations, ten layouts, Tailwind present and removed --

    private static void runControlledFrames(HeadlessChrome chrome, Path generatedDir) throws Exception {
        String environment = environment(generatedDir);
        String migrated = markup(false);
        String legacyMarkup = markup(true);
        StringBuilder frames = new StringBuilder();
        for (Config c : CONFIGS) {
            frames.append(frame(c.id() + "|L", c, with(ORACLE, SKIN_LINKS), "", legacyMarkup));
            frames.append(frame(c.id() + "|P", c, with(APP, SKIN_LINKS), "", migrated));
            frames.append(frame(c.id() + "|R", c, with(ASHLAR, SKIN_LINKS), environment, migrated));
            // The legacy Divider from the scoped stylesheet, in the same unstyled environment as |R.
            frames.append(frame(c.id() + "|S", c, with(SCOPED, SKIN_LINKS), environment, legacyMarkup));
        }
        for (Control control : CONTROL_LIST) {
            frames.append(frame("ctl:" + control.name() + "|P", config(control.config()), with(APP, SKIN_LINKS), control.css(), migrated));
        }
        // The customisation surface: an instance property wins over the class, and an unlayered rule wins over the component's dark rule.
        String pair = "<div data-layout=\"ovr\" style=\"display:flex;height:40px;width:300px\">" + horizontal() + vertical() + "</div>";
        String inline = pair.replace("data-divider ", "data-divider style=\"--ash-divider-color: rgb(1, 2, 3)\" ");
        frames.append(frame("ovr:inline-light|P", config("light/none"), with(APP, SKIN_LINKS), "", inline));
        frames.append(frame("ovr:unlayered-dark|P", config("dark/none"), with(APP, SKIN_LINKS), "[data-divider]{--ash-divider-color: rgb(4, 5, 6)}", pair));
        // The other two combinations: an inline value in dark mode, and an unlayered rule in light mode.
        frames.append(frame("ovr:inline-dark|P", config("dark/none"), with(APP, SKIN_LINKS), "", inline));
        frames.append(frame("ovr:unlayered-light|P", config("light/none"), with(APP, SKIN_LINKS), "[data-divider]{--ash-divider-color: rgb(4, 5, 6)}", pair));
        String probeJs;
        try (InputStream in = DividerParityTest.class.getResourceAsStream("/cascade/divider-probe.js")) {
            probeJs = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String page = "<!doctype html><html><body style=\"margin:0\">\n" + frames + "<script>" + probeJs + "</script>\n<script>"
                + "addEventListener('load',function(){setTimeout(function(){var out=['props\\t'+window.__ashDivider.PROPS.join(',')];"
                + "[].forEach.call(document.querySelectorAll('iframe'),function(f){var id=f.getAttribute('data-id');"
                + "window.__ashDivider.measure(f.contentWindow,f.contentDocument,id).forEach(function(r){out.push(r)});"
                + "window.__ashDivider.tokens(f.contentWindow,f.contentDocument,id).forEach(function(r){out.push(r)})});"
                + "var pre=document.createElement('pre');pre.id='ash-divider-frames';pre.textContent=out.join('\\n');document.body.appendChild(pre)},300)})"
                + "</script></body></html>";
        Probe probe;
        try (CascadeFixtureServer server = new CascadeFixtureServer(generatedDir)) {
            String dom = chrome.dumpDom(server.registerRaw(page), List.of("--hide-scrollbars"));
            probe = parse(lines(dom, "ash-divider-frames"), "controlled frames");
        }
        props = probe.props();
        for (Config c : CONFIGS) {
            List<Row> legacyRows = probe.phase(c.id() + "|L");
            CONTROLLED_ROWS.put(c.id(), legacyRows.size());
            CONTROLLED_PRESENT.put(c.id(), compare(c.id() + " (Tailwind present)", props, legacyRows, probe.phase(c.id() + "|P"), APPLIED_CONTROLLED));
            // Tailwind removed: against the legacy Divider given the same unstyled surroundings ...
            CONTROLLED_REMOVED_SCOPED.put(c.id(), compare(c.id() + " (Tailwind removed vs scoped legacy)", props, probe.phase(c.id() + "|S"), probe.phase(c.id() + "|R"), APPLIED_CONTROLLED));
            // ... and against the full legacy rendering, which also has Tailwind's preflight around it.
            CONTROLLED_REMOVED_FULL.put(c.id(), compare(c.id() + " (Tailwind removed vs full legacy)", props, legacyRows, probe.phase(c.id() + "|R"), APPLIED_CONTROLLED));
            // Fidelity of the scoped legacy stylesheet: it must render the legacy Divider exactly as the full one does.
            SCOPED_FIDELITY.put(c.id(), compare(c.id() + " (scoped legacy vs full legacy)", props, legacyRows, probe.phase(c.id() + "|S"), APPLIED_SCOPED));
        }
        for (Control control : CONTROL_LIST) {
            List<Row> expected = probe.phase(control.config() + "|L");
            CONTROLS.put(control.name(), compare("control: " + control.name(), props, expected, probe.phase("ctl:" + control.name() + "|P"), new TreeMap<>()));
        }
        FRAME_PROBES.put("controlled", probe);
    }

    // -- real pages: every Divider on the Divider and Card pages, 22 configurations, in parallel --

    private static String legacyConfigJson(String phase, String stylesheet, boolean detach) {
        return "{\"phase\":\"" + phase + "\",\"legacy\":{\"stylesheet\":\"" + stylesheet + "\",\"detach\":" + detach + ",\"roles\":" + legacy.configJson() + "}}";
    }

    private static void runRealPages(HeadlessChrome chrome, Path generatedDir, Path bootJar) throws Exception {
        byte[] oracleCss = Files.readAllBytes(generatedDir.resolve("divider-oracle.css"));
        byte[] ashlarCss = Files.readAllBytes(Path.of(System.getProperty("ashlar.css.generated")));
        String environment = environment(generatedDir);
        byte[] removedCss = (new String(ashlarCss, StandardCharsets.UTF_8) + "\n" + environment).getBytes(StandardCharsets.UTF_8);
        byte[] scopedCss = Files.readAllBytes(generatedDir.resolve("divider-legacy-scoped.css"));
        String presentConfig = legacyConfigJson("ash", ORACLE_VIRTUAL, false);
        // Tailwind removed: the migrated Divider is measured in the unstyled page, then the legacy Divider is measured on the
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
                for (String page : REAL_PAGES) {
                    futures.add(pool.submit(() -> {
                        RealPageProxy proxy = proxyOfThread.get();
                        if (proxy == null) {
                            proxy = new RealPageProxy(app.port());
                            proxies.add(proxy);
                            proxyOfThread.set(proxy);
                        }
                        String path = page + (config.skin() == null ? "" : "?skin=" + config.skin());
                        String key = page + "|" + config.id();
                        proxy.configure(new RealPageProxy.Load(config.theme(), "divider", null, "/cascade/divider-probe.js", presentConfig,
                                List.of(), Map.of(ORACLE_VIRTUAL, oracleCss), null));
                        present.put(key, parse(lines(chrome.dumpDom(proxy.url(path), List.of("--hide-scrollbars")), "ash-divider-results"), key + " present"));
                        proxy.configure(new RealPageProxy.Load(config.theme(), "divider", removedCss, "/cascade/divider-probe.js", removedConfig,
                                List.of(), Map.of(SCOPED_VIRTUAL, scopedCss), null));
                        removed.put(key, parse(lines(chrome.dumpDom(proxy.url(path), List.of("--hide-scrollbars")), "ash-divider-results"), key + " removed"));
                        return null;
                    }));
                }
            }
            for (Future<?> f : futures) {
                f.get();
            }
            countDividersOnEveryPage(app.port());
        } finally {
            pool.shutdownNow();
            proxies.forEach(RealPageProxy::close);
        }
        for (Config config : CONFIGS) {
            for (String page : REAL_PAGES) {
                String key = page + "|" + config.id();
                Probe p = present.get(key);
                Probe r = removed.get(key);
                REAL_PROBES.put(key + "|present", p);
                REAL_PROBES.put(key + "|removed", r);
                String where = page + " " + config.id();
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
                realComparisonsPerMode += p.phase("legacy").size();
                skinFacts(config, page, p);
                if (config.id().equals("light/none")) {
                    p.shapes().forEach(s -> shapeProblems(page, s));
                }
            }
        }
    }

    /** Every catalog page, the home page and the index, as served: how many Dividers each renders. */
    private static void countDividersOnEveryPage(int port) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        List<String> paths = new ArrayList<>(List.of("/ashlar/", "/ashlar/components"));
        for (ComponentPage page : ComponentCatalog.PAGES) {
            paths.add("/ashlar/components/" + page.slug());
        }
        for (String path : paths) {
            HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            assertEquals(200, response.statusCode(), path);
            int hooks = (int) Pattern.compile("\\bdata-divider\\b(?!-)").matcher(response.body()).results().count();
            assertEquals(DividerLegacyMarkup.count(response.body()), hooks, path + ": every data-divider must be a migrated Divider");
            DIVIDERS_PER_PAGE.put(path, hooks);
        }
    }

    // -- facts: what each theme and skin must do to a Divider (checked against what the skin defines, not only against the legacy rendering) --

    private static void skinFacts(Config config, String page, Probe p) {
        String skin = config.skin() == null ? "none" : config.skin();
        String where = page + " " + config.id() + ": ";
        Map<String, String> tok = p.tokens().get("page");
        String shade = config.theme().equals("dark") ? "700" : "200";
        for (Row row : p.phase("ash")) {
            boolean horizontal = row.variant().startsWith("horizontal/");
            String side = horizontal ? "top" : "left";
            String here = where + "#" + row.ordinal() + " " + row.variant() + " ";
            expect(here + "line width", "1px", p.value(row, "border-" + side + "-width"));
            for (String other : List.of("top", "right", "bottom", "left")) {
                if (!other.equals(side)) {
                    expect(here + other + " border width", "0px", p.value(row, "border-" + other + "-width"));
                }
            }
            expect(here + "line style", "solid", p.value(row, "border-" + side + "-style"));
            if (horizontal) {
                for (String margin : List.of("top", "right", "bottom", "left")) {
                    expect(here + "margin-" + margin, "0px", p.value(row, "margin-" + margin));
                }
                expect(here + "overflow is still the browser's", "hidden", p.value(row, "overflow-x"));
            } else {
                // inline-block, blockified when its parent is a flex container (the toolbar on the showcase page).
                expect(here + "display", row.parentDisplay().equals("flex") ? "block" : "inline-block", p.value(row, "display"));
                expect(here + "align-self", "stretch", p.value(row, "align-self"));
            }
            if (config.skin() != null) {
                // Only a skin defines the whole slate palette; with no skin the colour is parity-checked against the legacy rendering.
                expect(here + "colour is the skin's --color-slate-" + shade, tok.get("--color-slate-" + shade), p.value(row, "border-" + side + "-color"));
            }
        }
        if (skin.equals("none") && config.theme().equals("light")) {
            assertTrue(!p.phase("ash").isEmpty(), where + "no dividers measured");
        }
    }

    private static void expect(String what, String expected, String actual) {
        if (expected == null || !expected.equals(actual)) {
            SKIN_PROBLEMS.add(what + ": expected " + expected + " but was " + actual);
        }
    }

    private static void shapeProblems(String page, String[] s) {
        String where = page + " divider #" + s[1];
        boolean horizontal = s[6].equals("horizontal");
        if (!s[2].equals(horizontal ? "hr" : "div")) {
            SHAPE_PROBLEMS.add(where + ": a " + s[6] + " divider is a <" + s[2] + ">");
        }
        if (horizontal) {
            if (!s[3].equals("class,data-divider,data-divider-orientation") || !s[4].isEmpty() || !s[5].isEmpty()) {
                SHAPE_PROBLEMS.add(where + ": the native <hr> carries no ARIA, found attributes " + s[3] + " role=" + s[4] + " aria-orientation=" + s[5]);
            }
        } else if (!s[3].equals("aria-orientation,class,data-divider,data-divider-orientation,role") || !s[4].equals("separator") || !s[5].equals("vertical")) {
            SHAPE_PROBLEMS.add(where + ": the vertical divider is role=separator aria-orientation=vertical, found " + s[3] + " role=" + s[4] + " aria-orientation=" + s[5]);
        }
        if (!s[7].equals("0:0")) {
            SHAPE_PROBLEMS.add(where + ": a divider has no children or text, found " + s[7]);
        }
        if (!s[8].equals("ash-divider ash-divider--" + s[6])) {
            SHAPE_PROBLEMS.add(where + ": classes " + s[8]);
        }
    }

    private static void writeReport() throws IOException {
        StringBuilder sb = new StringBuilder("Divider parity (real Chrome; no stored baseline).\n\n");
        sb.append("CONTROLLED ORACLE: ").append(CONFIGS.size()).append(" configurations x ").append(LAYOUTS.size()).append(" layouts x 2 orientations x (Tailwind present, Tailwind removed), ")
                .append(CONTROLLED_ROWS.values().stream().mapToInt(Integer::intValue).sum()).append(" legacy rows measured\n");
        sb.append("   differences (Tailwind present): ").append(CONTROLLED_PRESENT.values().stream().mapToInt(List::size).sum())
                .append(" ; (removed vs scoped legacy): ").append(CONTROLLED_REMOVED_SCOPED.values().stream().mapToInt(List::size).sum())
                .append(" ; (removed vs full legacy): ").append(CONTROLLED_REMOVED_FULL.values().stream().mapToInt(List::size).sum()).append('\n');
        sb.append("   normalizations used (rule: uses): ").append(APPLIED_CONTROLLED).append('\n');
        sb.append("   scoped legacy stylesheet vs full legacy, differences: ").append(SCOPED_FIDELITY.values().stream().mapToInt(List::size).sum())
                .append(" ; normalizations used: ").append(APPLIED_SCOPED).append('\n');
        sb.append("NEGATIVE CONTROLS (each must produce a difference in the named property):\n");
        for (Control c : CONTROL_LIST) {
            List<String> found = CONTROLS.get(c.name()).stream().map(Finding::property).distinct().toList();
            sb.append("   ").append(found.contains(c.expectedProperty()) ? "DETECTED " : "MISSED   ").append(c.name()).append(" -> ").append(found).append('\n');
        }
        sb.append("REAL PAGES: ").append(REAL_PAGES).append(" x ").append(CONFIGS.size()).append(" configurations (parallel, ").append(realSeconds).append(" s)\n");
        sb.append("   dividers per page (every catalog page, home and index): ").append(DIVIDERS_PER_PAGE.entrySet().stream().filter(e -> e.getValue() > 0).toList())
                .append(" ; pages scanned ").append(DIVIDERS_PER_PAGE.size()).append('\n');
        sb.append("   instance comparisons per mode (instances x configurations): ").append(realComparisonsPerMode).append('\n');
        sb.append("   differences (Tailwind present): ").append(REAL_PRESENT.size()).append(" ; (Tailwind removed): ").append(REAL_REMOVED.size()).append('\n');
        sb.append("   normalizations used, Tailwind present: ").append(APPLIED_REAL_PRESENT).append('\n');
        sb.append("   normalizations used, Tailwind removed: ").append(APPLIED_REAL_REMOVED).append('\n');
        sb.append("   facts not met: ").append(SKIN_PROBLEMS.size()).append(" ; DOM shape problems: ").append(SHAPE_PROBLEMS.size()).append('\n');
        appendFindings(sb, "FINDINGS, controlled, Tailwind present", CONTROLLED_PRESENT.values().stream().flatMap(List::stream).toList());
        appendFindings(sb, "FINDINGS, controlled, removed vs scoped legacy", CONTROLLED_REMOVED_SCOPED.values().stream().flatMap(List::stream).toList());
        appendFindings(sb, "FINDINGS, controlled, removed vs full legacy", CONTROLLED_REMOVED_FULL.values().stream().flatMap(List::stream).toList());
        appendFindings(sb, "FINDINGS, scoped legacy vs full legacy", SCOPED_FIDELITY.values().stream().flatMap(List::stream).toList());
        appendFindings(sb, "FINDINGS, real pages, Tailwind present", REAL_PRESENT);
        appendFindings(sb, "FINDINGS, real pages, Tailwind removed", REAL_REMOVED);
        SKIN_PROBLEMS.stream().limit(20).forEach(p -> sb.append("   FACT ").append(p).append('\n'));
        SHAPE_PROBLEMS.stream().limit(20).forEach(p -> sb.append("   SHAPE ").append(p).append('\n'));
        Path out = Path.of(System.getProperty("ashlar.cascade.dir")).resolve("../../reports/parity/divider.txt").normalize();
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
    void controlledOracleWithTailwindPresentMatchesTheLegacyDividerInEveryConfigurationAndLayout() {
        assertEquals(List.of(), CONTROLLED_PRESENT.values().stream().flatMap(List::stream).map(Finding::line).limit(25).toList());
        CONFIGS.forEach(c -> assertEquals(LAYOUTS.size() * 2, CONTROLLED_ROWS.get(c.id()), c.id() + ": rows measured"));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheLegacyDividerGivenTheSameSurroundings() {
        assertEquals(List.of(), CONTROLLED_REMOVED_SCOPED.values().stream().flatMap(List::stream).map(Finding::line).limit(25).toList());
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheFullLegacyRendering() {
        assertEquals(List.of(), CONTROLLED_REMOVED_FULL.values().stream().flatMap(List::stream).map(Finding::line).limit(25).toList());
    }

    @Test
    void theScopedLegacyStylesheetRendersTheLegacyDividerExactlyAsTheFullOneDoes() {
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
    void theControlledLayoutsReallyExerciseTheBrowsersStretchingAndMarginBehaviour() {
        // Anchors the claims in divider.css to measurements, so the layouts cannot silently stop testing what they are for.
        Probe probe = FRAME_PROBES.get("controlled");
        List<Row> rows = probe.phase("light/none|P");
        java.util.function.BiFunction<String, String, String> at = (variant, prop) -> probe.value(
                rows.stream().filter(r -> r.variant().equals(variant)).findFirst().orElseThrow(() -> new AssertionError("no row " + variant)), prop);
        assertEquals("1", at.apply("horizontal/row-flex", "@h"), "a horizontal divider keeps its 1px in a row flex container");
        assertEquals("60", at.apply("vertical/row-flex", "@h"), "a vertical divider stretches to the height of a sized flex row");
        assertEquals("60", at.apply("vertical/row-flex-centered", "@h"), "self-stretch wins over align-items:center");
        assertEquals("0", at.apply("vertical/block", "@h"), "outside a flex row a vertical divider has no height, and no minimum height is added");
        assertEquals("1", at.apply("vertical/block", "@w"), "the vertical line is a 1px border box");
        assertEquals("300", at.apply("horizontal/column-flex", "@w"), "a horizontal divider fills its column");
        assertEquals("300", at.apply("horizontal/column-flex-centered", "@w"), "w-full holds even when the column centres its items");
        assertEquals("0", at.apply("horizontal/between-paragraphs", "@x"), "no auto inline margin pushes the line in");
        assertEquals("1", at.apply("horizontal/between-paragraphs", "@h"));
        assertEquals("0px", at.apply("horizontal/toolbar", "margin-top"), "no margin of its own in a toolbar");
        assertEquals("40", at.apply("vertical/toolbar", "@h"), "the toolbar divider spans the toolbar's whole height despite align-items:center");
    }

    @Test
    void theDarkRuleAndTheLightRuleReallyDiffer() {
        Probe probe = FRAME_PROBES.get("controlled");
        String light = probe.value(probe.phase("light/none|P").get(0), "border-top-color");
        String dark = probe.value(probe.phase("dark/none|P").get(0), "border-top-color");
        assertNotEquals(light, dark, "light and dark must draw different colours");
        assertNotEquals("rgba(0, 0, 0, 0)", light);
    }

    @Test
    void anInstanceCustomPropertyBeatsTheClassAndAnUnlayeredRuleBeatsTheComponentsDarkRule() {
        Probe probe = FRAME_PROBES.get("controlled");
        List<Row> inline = probe.phase("ovr:inline-light|P");
        assertEquals(2, inline.size());
        assertEquals("rgb(1, 2, 3)", probe.value(inline.get(0), "border-top-color"));
        assertEquals("rgb(1, 2, 3)", probe.value(inline.get(1), "border-left-color"));
        List<Row> unlayered = probe.phase("ovr:unlayered-dark|P");
        assertEquals("rgb(4, 5, 6)", probe.value(unlayered.get(0), "border-top-color"));
        assertEquals("rgb(4, 5, 6)", probe.value(unlayered.get(1), "border-left-color"));
    }

    @Test
    void aCallersCustomColourBeatsTheDefaultPaletteColourInBothThemesForBothOrientationsAndBothWaysOfSettingIt() {
        Probe probe = FRAME_PROBES.get("controlled");
        // The default colour of the same two dividers with no override: the first horizontal and the first vertical divider of the plain frames.
        for (String theme : List.of("light", "dark")) {
            List<Row> plain = probe.phase(theme + "/none|P");
            String defaultHorizontal = probe.value(plain.get(0), "border-top-color");
            String defaultVertical = probe.value(plain.get(1), "border-left-color");
            assertEquals("horizontal/block", plain.get(0).variant());
            assertEquals("vertical/block", plain.get(1).variant());
            for (String[] c : new String[][] {{"inline", "rgb(1, 2, 3)"}, {"unlayered", "rgb(4, 5, 6)"}}) {
                String phase = "ovr:" + c[0] + "-" + theme + "|P";
                List<Row> rows = probe.phase(phase);
                assertEquals(2, rows.size(), phase);
                assertEquals("horizontal/ovr", rows.get(0).variant(), phase);
                assertEquals("vertical/ovr", rows.get(1).variant(), phase);
                assertEquals(c[1], probe.value(rows.get(0), "border-top-color"), phase + ": the horizontal divider takes the caller's colour");
                assertEquals(c[1], probe.value(rows.get(1), "border-left-color"), phase + ": the vertical divider takes the caller's colour");
                assertNotEquals(defaultHorizontal, probe.value(rows.get(0), "border-top-color"), phase + ": it replaced the " + theme + " palette colour");
                assertNotEquals(defaultVertical, probe.value(rows.get(1), "border-left-color"), phase + ": it replaced the " + theme + " palette colour");
                // The zero-width sides follow the same property, as with the default colour.
                assertEquals(c[1], probe.value(rows.get(0), "border-left-color"), phase);
                assertEquals(c[1], probe.value(rows.get(1), "border-top-color"), phase);
            }
        }
        // The two themes really do have different defaults, so winning "over the default" means something in each mode.
        assertNotEquals(probe.value(probe.phase("light/none|P").get(0), "border-top-color"), probe.value(probe.phase("dark/none|P").get(0), "border-top-color"));
    }

    @Test
    void everyRealDividerMatchesItsLegacyRenderingWithTailwindPresent() {
        assertEquals(List.of(), REAL_PRESENT.stream().map(Finding::line).limit(25).toList(), REAL_PRESENT.size() + " differences in total");
    }

    @Test
    void everyRealDividerMatchesItsLegacyRenderingWithTailwindRemoved() {
        assertEquals(List.of(), REAL_REMOVED.stream().map(Finding::line).limit(25).toList(), REAL_REMOVED.size() + " differences in total");
    }

    @Test
    void realPageCoverageIsAllFourInstancesInAll22ConfigurationsAndNoOtherPageRendersADivider() {
        assertEquals(4 * 22, realComparisonsPerMode);
        Map<String, Integer> withDividers = DIVIDERS_PER_PAGE.entrySet().stream().filter(e -> e.getValue() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));
        assertEquals(Map.of(DIVIDER_PAGE, 3, CARD_PAGE, 1), withDividers);
        assertEquals(2 + ComponentCatalog.PAGES.size(), DIVIDERS_PER_PAGE.size(), "every catalog page, the home page and the index were scanned");
        for (Config c : CONFIGS) {
            Probe d = REAL_PROBES.get(DIVIDER_PAGE + "|" + c.id() + "|present");
            assertEquals("3", d.meta().get("divider-count"), c.id());
            assertEquals("0", d.meta().get("dividers-without-a-box"), c.id() + ": every divider must be rendered, none is hidden");
            // The three showcase instances, in document order: quick start, between sections, and the toolbar's vertical one.
            assertEquals(List.of("horizontal/page", "horizontal/page", "vertical/page"), d.phase("ash").stream().map(Row::variant).toList(), c.id());
            Probe card = REAL_PROBES.get(CARD_PAGE + "|" + c.id() + "|present");
            assertEquals("1", card.meta().get("divider-count"), c.id());
            assertEquals("0", card.meta().get("dividers-without-a-box"), c.id());
            assertEquals(List.of("horizontal/page"), card.phase("ash").stream().map(Row::variant).toList(), c.id());
        }
    }

    @Test
    void theToolbarsVerticalDividerReallyStretchesAcrossTheToolbarOnTheRealPage() {
        for (Config c : CONFIGS) {
            Probe d = REAL_PROBES.get(DIVIDER_PAGE + "|" + c.id() + "|present");
            Row vertical = d.phase("ash").get(2);
            assertEquals("flex", vertical.parentDisplay(), c.id());
            assertEquals("stretch", d.value(vertical, "align-self"), c.id());
            assertTrue(Double.parseDouble(d.value(vertical, "@h")) >= 30, c.id() + ": the vertical divider must span the toolbar, was " + d.value(vertical, "@h"));
        }
    }

    @Test
    void theDividerDomShapeIsPreservedAndCarriesOnlyAshlarClasses() {
        assertEquals(List.of(), SHAPE_PROBLEMS);
        assertEquals(3, REAL_PROBES.get(DIVIDER_PAGE + "|light/none|present").shapes().size());
        assertEquals(1, REAL_PROBES.get(CARD_PAGE + "|light/none|present").shapes().size());
    }

    @Test
    void whileTheScopedLegacySheetIsLinkedTheLegacyNamesAreOffEveryOtherElementAndAreRestoredExactly() {
        for (Config c : CONFIGS) {
            for (String page : REAL_PAGES) {
                Probe removed = REAL_PROBES.get(page + "|" + c.id() + "|removed");
                assertEquals("0", removed.meta().get("detach-restore-mismatch"), page + " " + c.id() + ": detached class names were not restored exactly");
                assertTrue(Integer.parseInt(removed.meta().get("detached-elements")) > 5, page + " " + c.id() + ": the page around the divider uses the legacy names, so elements must have been detached: " + removed.meta().get("detached-elements"));
                assertEquals("0", REAL_PROBES.get(page + "|" + c.id() + "|present").meta().getOrDefault("detached-elements", "0"), c.id() + ": nothing is detached while Tailwind is present");
            }
        }
    }

    @Test
    void theThemeAndSkinFactsAreMetForEveryDivider() {
        assertEquals(List.of(), SKIN_PROBLEMS.stream().limit(25).toList(), SKIN_PROBLEMS.size() + " facts not met");
    }

    private static String escapeClass(String token) {
        return "." + token.replaceAll("([:/.])", "\\\\$1");
    }

    private static boolean compiles(String css, String token) {
        return css.contains(escapeClass(token) + "{") || css.contains(escapeClass(token) + ":") || css.contains(escapeClass(token) + ",");
    }

    @Test
    void theOracleIsNotVacuousEveryLegacyClassIsCompiledInTheCumulativeAndScopedStylesheets() throws IOException {
        Path dir = Path.of(System.getProperty("ashlar.cascade.dir"));
        String oracle = Files.readString(dir.resolve("divider-oracle.css"), StandardCharsets.UTF_8);
        String scoped = Files.readString(dir.resolve("divider-legacy-scoped.css"), StandardCharsets.UTF_8);
        for (String token : legacy.allTokens()) {
            assertTrue(compiles(oracle, token), "the Divider oracle does not compile " + token);
            // The scoped stylesheet reads no source but divider-legacy.html, so this also proves its @source path resolves.
            assertTrue(compiles(scoped, token), "the scoped stylesheet does not compile " + token);
        }
        // Cumulative, in order Badge -> Alert -> Divider: the earlier legacy classes are still in the Divider oracle.
        for (String classes : new BadgeLegacyMarkup().variants().values()) {
            for (String token : classes.split("\\s+")) {
                assertTrue(oracle.contains(escapeClass(token)), "the Divider oracle lost the Badge legacy class " + token);
            }
        }
        for (String token : new AlertLegacyMarkup().allTokens()) {
            assertTrue(compiles(oracle, token), "the Divider oracle lost the Alert legacy class " + token);
        }
        assertFalse(scoped.contains("html,:host"), "the scoped legacy stylesheet must not carry Tailwind's page-wide base");
    }

    @Test
    void normalizationsStayWithinTheApprovedRulesAndTailwindPresentNeedsNone() {
        assertEquals(Map.of(), APPLIED_REAL_PRESENT, "with Tailwind present the Divider must match exactly");
        assertEquals(Map.of(), APPLIED_CONTROLLED.entrySet().stream().filter(e -> !Set.of("H2", "H3").contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        assertTrue(Set.of("H2", "H3").containsAll(APPLIED_REAL_REMOVED.keySet()), "unexpected normalization rule: " + APPLIED_REAL_REMOVED);
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
