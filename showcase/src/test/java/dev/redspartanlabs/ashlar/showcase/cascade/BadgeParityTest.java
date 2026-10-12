package dev.redspartanlabs.ashlar.showcase.cascade;

import dev.redspartanlabs.ashlar.badge.BadgeStyles;
import dev.redspartanlabs.ashlar.badge.BadgeVariant;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Badge's parity gates, measured in real Chrome.
 *
 * <p>Evidence comes in two kinds, and neither stores a baseline. (1) A controlled oracle: the Badge's
 * legacy Tailwind markup (compiled by the real Tailwind CLI) next to its migrated markup, in the same
 * browser, for each of the 22 theme and skin configurations, with Tailwind present and with Tailwind removed.
 * (2) Every Badge instance on the 15 showcase pages that render one, in the same 22 configurations: the real
 * page is measured as served, then the very same elements get their legacy classes back and are measured
 * again, in the same run. Differences are compared with {@link ParityNormalizer} (exact equality except H1 to
 * H4, with their preconditions). Negative controls show each gate can fail.
 *
 * <p>Evidence is Chrome only. Computed-style parity is the visual evidence; there is no screenshot comparison.
 */
class BadgeParityTest {

    private static final String ASHLAR = "/ashlar-real/ashlar.css";
    private static final String ORACLE = "/generated/badge-oracle.css";
    private static final String APP = "/css/app.css";
    private static final String ORACLE_VIRTUAL = "/__parity/badge-oracle.css";
    private static final String SCOPED = "/generated/badge-legacy-scoped.css";
    private static final String SCOPED_VIRTUAL = "/__parity/badge-legacy-scoped.css";
    private static final List<String> SKINS = List.of("axiom", "civic", "fathom", "foundry", "harbor", "keystone", "ledger", "sterling", "summit", "veridian");
    private static final List<String> SKIN_LINKS = SKINS.stream().map(s -> "/css/theme-" + s + ".css").toList();
    private static final List<String> VARIANTS = List.of("neutral", "info", "success", "warning", "danger");

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

    record Page(String name, String path) {
    }

    private static final List<Page> PAGES = pages();

    private static List<Page> pages() {
        List<Page> all = new ArrayList<>();
        all.add(new Page("home", "/ashlar/"));
        for (String slug : List.of("accordion", "badge", "card", "data-table", "faceted-search", "filter-bar", "key-value-list",
                "list", "page-shell", "pagination", "popover", "stat-card", "status-line", "table")) {
            all.add(new Page(slug, "/ashlar/components/" + slug));
        }
        return List.copyOf(all);
    }

    /** The Badge count each page renders. Changing a showcase page's badges means updating this deliberately. */
    private static final Map<String, Integer> EXPECTED_BADGES = Map.ofEntries(
            Map.entry("home", 16), Map.entry("accordion", 1), Map.entry("badge", 15), Map.entry("card", 1),
            Map.entry("data-table", 31), Map.entry("faceted-search", 3), Map.entry("filter-bar", 14),
            Map.entry("key-value-list", 3), Map.entry("list", 2), Map.entry("page-shell", 1), Map.entry("pagination", 3),
            Map.entry("popover", 1), Map.entry("stat-card", 5), Map.entry("status-line", 2), Map.entry("table", 34));

    // ---- parsed measurements ----

    record Row(String phase, int ordinal, String kind, String variant, String context, String parentDisplay, String[] values, String rendering) {
    }

    record Probe(List<String> props, Map<String, String> meta, List<String[]> shapes, List<Row> rows) {
        List<Row> phase(String phase) {
            return rows.stream().filter(r -> r.phase().equals(phase)).toList();
        }
    }

    record Finding(String where, String property, String expected, String actual) {
        String line() {
            return where + " | " + property + " | " + expected + " -> " + actual;
        }
    }

    private static List<String> props = List.of();
    private static final Map<String, List<Finding>> CONTROLLED_PRESENT = new LinkedHashMap<>();
    private static final Map<String, List<Finding>> CONTROLLED_REMOVED = new LinkedHashMap<>();
    private static final Map<String, List<Finding>> CONTROLS = new LinkedHashMap<>();
    private static final Map<String, List<Finding>> SCOPED_FIDELITY = new LinkedHashMap<>();
    private static final Map<String, Integer> APPLIED_SCOPED = new TreeMap<>();
    private static final Map<String, Integer> CONTROLLED_ROWS = new LinkedHashMap<>();
    private static final List<Finding> REAL_PRESENT = new ArrayList<>();
    private static final List<Finding> REAL_REMOVED = new ArrayList<>();
    private static final Map<String, Integer> APPLIED_CONTROLLED = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_REAL_PRESENT = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_REAL_REMOVED = new TreeMap<>();
    private static final Map<String, Integer> BADGES_PER_PAGE = new TreeMap<>();
    private static final List<String> SHAPE_PROBLEMS = new ArrayList<>();
    private static final List<String> CONTEXT_COVERAGE = new ArrayList<>();
    private static int realComparisonsPerMode;
    /** "normal-ash", "revealed-legacy", ... -> badge measurements (instances x configurations) taken that way. */
    private static final Map<String, Integer> REVEAL_COUNTS = new TreeMap<>();
    private static int revealRestoreMismatches;
    private static long realSeconds;

    // ---- markup ----

    private static BadgeLegacyMarkup legacy;

    private static final String ICON_SVG = "<svg class=\"inline-block shrink-0 %s\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" "
            + "stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\"><path d=\"M4 12l5 5L20 6\"></path></svg>";

    private static String markup(boolean legacyClasses) {
        StringBuilder sb = new StringBuilder();
        for (String v : VARIANTS) {
            String classes = legacyClasses ? legacy.variantClasses(v) : BadgeStyles.container(BadgeVariant.valueOf(v.toUpperCase(Locale.ROOT)));
            sb.append("<p style=\"margin:8px\"><span class=\"").append(classes).append("\" data-badge data-badge-variant=\"").append(v).append("\">").append(v).append("</span></p>");
        }
        String classes = legacyClasses ? legacy.variantClasses("info") : BadgeStyles.container(BadgeVariant.INFO);
        String svg = ICON_SVG.formatted(legacyClasses ? legacy.iconSizing() : "ash-badge__icon");
        sb.append("<p style=\"margin:8px\"><span class=\"").append(classes).append("\" data-badge data-badge-variant=\"info\">").append(svg).append("with icon</span></p>");
        return sb.toString();
    }

    private static String frame(String id, Config config, List<String> sheets, String extraCss, boolean legacyClasses) {
        StringBuilder links = new StringBuilder();
        sheets.forEach(href -> links.append("<link rel=\"stylesheet\" href=\"").append(href).append("\">"));
        String skin = config.skin() == null ? "" : " data-skin=\"" + config.skin() + "\"";
        String doc = "<!doctype html><html data-theme=\"" + config.theme() + "\"" + skin + "><head>" + links + "<style>" + extraCss
                + "</style></head><body style=\"margin:0;padding:8px\">" + markup(legacyClasses) + "</body></html>";
        return "<iframe data-id=\"" + id + "\" style=\"width:420px;height:420px;border:0\" srcdoc=\""
                + doc.replace("&", "&amp;").replace("\"", "&quot;") + "\"></iframe>\n";
    }

    private static List<String> with(String first, List<String> rest) {
        List<String> all = new ArrayList<>();
        all.add(first);
        all.addAll(rest);
        return all;
    }

    // ---- control experiments: each breaks the migrated Badge in ONE way and must be detected ----

    private record Control(String name, String css, String expectedProperty) {
    }

    private static final List<Control> CONTROL_LIST = List.of(
            new Control("radius below half the height", ".ash-badge{border-radius:11px}", "border-top-left-radius"),
            new Control("wrong background", ".ash-badge--info{--ash-badge-bg:red}", "background-color"),
            new Control("ring removed", ".ash-badge{box-shadow:none}", "box-shadow"),
            new Control("ring one pixel too wide", ".ash-badge{box-shadow:inset 0 0 0 2px var(--ash-badge-ring)}", "box-shadow"),
            new Control("padding changed", ".ash-badge{padding-inline:.6rem}", "padding-left"),
            new Control("vertical-align changed where the parent is a block", ".ash-badge{vertical-align:top}", "vertical-align"),
            new Control("icon size changed", ".ash-badge__icon{inline-size:1rem}", "width"),
            new Control("font weight changed", ".ash-badge{font-weight:600}", "font-weight"));

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
        List<Row> rows = new ArrayList<>();
        for (String line : lines) {
            String[] f = line.split("\t", -1);
            switch (f[0]) {
                case "props" -> propNames = List.of(f[1].split(","));
                case "meta" -> meta.put(f[1], f.length > 2 ? f[2] : "");
                case "shape" -> shapes.add(f);
                case "row" -> rows.add(new Row(f[1], Integer.parseInt(f[2]), f[3], f[4], f[5], f[6], f[7].split("\u001f", -1), f.length > 8 ? f[8] : ""));
                default -> throw new IllegalStateException(what + ": unrecognised result line: " + line);
            }
        }
        if (propNames.isEmpty()) {
            throw new IllegalStateException(what + ": the probe produced no property list - infrastructure failure, not a pass");
        }
        return new Probe(propNames, meta, shapes, rows);
    }

    private static List<Finding> compare(String where, List<String> propNames, List<Row> expected, List<Row> actual, Map<String, Integer> applied) {
        List<Finding> findings = new ArrayList<>();
        if (expected.size() != actual.size()) {
            findings.add(new Finding(where, "#rows", String.valueOf(expected.size()), String.valueOf(actual.size())));
            return findings;
        }
        for (int i = 0; i < expected.size(); i++) {
            Row e = expected.get(i);
            Row a = actual.get(i);
            int w = propNames.indexOf("width");
            int h = propNames.indexOf("height");
            String label = where + " #" + e.ordinal() + " " + e.kind() + " " + e.variant() + " [" + e.context() + "]"
                    + " legacyBox=" + e.values()[w] + "x" + e.values()[h] + " actualBox=" + a.values()[w] + "x" + a.values()[h]
                    + " legacy{" + e.rendering() + "} actual{" + a.rendering() + "}";
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
            System.out.println("[badge-parity] Chrome not found - Badge parity SKIPPED (not passed).");
            Assumptions.abort("Chrome not found; the Badge parity checks were not run");
        }
        HeadlessChrome chrome = new HeadlessChrome(chromePath.get());
        legacy = new BadgeLegacyMarkup();
        runControlledFrames(chrome, Path.of(generatedDir));
        long start = System.nanoTime();
        runRealPages(chrome, Path.of(generatedDir), Path.of(bootJar));
        realSeconds = (System.nanoTime() - start) / 1_000_000_000L;
        writeReport();
    }

    // -- controlled oracle: legacy vs migrated, 22 configurations, Tailwind present and removed --

    private static void runControlledFrames(HeadlessChrome chrome, Path generatedDir) throws Exception {
        String environment = PageEnvironment.tailwindRemovedRules(generatedDir.resolve("phase-c-baseline.css"));
        StringBuilder frames = new StringBuilder();
        for (Config c : CONFIGS) {
            frames.append(frame(c.id() + "|L", c, with(ORACLE, SKIN_LINKS), "", true));
            frames.append(frame(c.id() + "|P", c, with(APP, SKIN_LINKS), "", false));
            frames.append(frame(c.id() + "|R", c, with(ASHLAR, SKIN_LINKS), environment, false));
            // The legacy Badge from the scoped stylesheet, in the same unstyled environment as |R.
            frames.append(frame(c.id() + "|S", c, with(SCOPED, SKIN_LINKS), environment, true));
        }
        Config reference = CONFIGS.get(0);
        for (Control control : CONTROL_LIST) {
            frames.append(frame("ctl:" + control.name() + "|P", reference, with(APP, SKIN_LINKS), control.css(), false));
        }
        String probeJs;
        try (InputStream in = BadgeParityTest.class.getResourceAsStream("/cascade/badge-probe.js")) {
            probeJs = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String page = "<!doctype html><html><body style=\"margin:0\">\n" + frames + "<script>" + probeJs + "</script>\n<script>"
                + "addEventListener('load',function(){setTimeout(function(){var out=['props\\t'+window.__ashBadge.PROPS.join(',')];"
                + "[].forEach.call(document.querySelectorAll('iframe'),function(f){window.__ashBadge.measure(f.contentWindow,f.contentDocument,f.getAttribute('data-id')).forEach(function(r){out.push(r)})});"
                + "var pre=document.createElement('pre');pre.id='ash-badge-frames';pre.textContent=out.join('\\n');document.body.appendChild(pre)},300)})"
                + "</script></body></html>";
        Probe probe;
        try (CascadeFixtureServer server = new CascadeFixtureServer(generatedDir)) {
            String dom = chrome.dumpDom(server.registerRaw(page), List.of("--hide-scrollbars"));
            probe = parse(lines(dom, "ash-badge-frames"), "controlled frames");
        }
        props = probe.props();
        for (Config c : CONFIGS) {
            List<Row> legacyRows = probe.phase(c.id() + "|L");
            CONTROLLED_ROWS.put(c.id(), legacyRows.size());
            CONTROLLED_PRESENT.put(c.id(), compare(c.id() + " (Tailwind present)", props, legacyRows, probe.phase(c.id() + "|P"), APPLIED_CONTROLLED));
            CONTROLLED_REMOVED.put(c.id(), compare(c.id() + " (Tailwind removed)", props, legacyRows, probe.phase(c.id() + "|R"), APPLIED_CONTROLLED));
            // Fidelity of the scoped legacy stylesheet: it must render the legacy Badge exactly as the full one does.
            SCOPED_FIDELITY.put(c.id(), compare(c.id() + " (scoped legacy vs full legacy)", props, legacyRows, probe.phase(c.id() + "|S"), APPLIED_SCOPED));
        }
        List<Row> referenceLegacy = probe.phase(reference.id() + "|L");
        for (Control control : CONTROL_LIST) {
            CONTROLS.put(control.name(), compare("control: " + control.name(), props, referenceLegacy, probe.phase("ctl:" + control.name() + "|P"), new TreeMap<>()));
        }
    }

    // -- real pages: every Badge instance, 15 pages x 22 configurations, in parallel --

    private static String legacyConfigJson(String phase, String stylesheet) {
        String variants = legacy.variants().entrySet().stream()
                .map(e -> "\"" + e.getKey() + "\":\"" + e.getValue() + "\"").collect(Collectors.joining(","));
        return "{\"phase\":\"" + phase + "\",\"legacy\":{\"stylesheet\":\"" + stylesheet + "\",\"variants\":{" + variants + "},\"icon\":\"" + legacy.iconSizing() + "\"}}";
    }

    private static void runRealPages(HeadlessChrome chrome, Path generatedDir, Path bootJar) throws Exception {
        byte[] oracleCss = Files.readAllBytes(generatedDir.resolve("badge-oracle.css"));
        byte[] ashlarCss = Files.readAllBytes(Path.of(System.getProperty("ashlar.css.generated")));
        String environment = PageEnvironment.tailwindRemovedRules(generatedDir.resolve("phase-c-baseline.css"));
        byte[] removedCss = (new String(ashlarCss, StandardCharsets.UTF_8) + "\n" + environment).getBytes(StandardCharsets.UTF_8);
        byte[] scopedCss = Files.readAllBytes(generatedDir.resolve("badge-legacy-scoped.css"));
        String presentConfig = legacyConfigJson("ash", ORACLE_VIRTUAL);
        // Tailwind removed: the migrated Badge is measured in the unstyled page, then the legacy Badge is measured on the
        // very same elements in the very same page with only its own utilities added - so the surroundings are identical.
        String removedConfig = legacyConfigJson("removed", SCOPED_VIRTUAL);
        Map<String, Probe> present = new ConcurrentHashMap<>();
        Map<String, Probe> removed = new ConcurrentHashMap<>();
        int workers = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 2));
        List<RealPageProxy> proxies = Collections.synchronizedList(new ArrayList<>());
        ThreadLocal<RealPageProxy> proxyOfThread = new ThreadLocal<>();
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        try (RunningShowcase app = RunningShowcase.start(bootJar)) {
            List<Future<?>> futures = new ArrayList<>();
            for (Page page : PAGES) {
                for (Config config : CONFIGS) {
                    futures.add(pool.submit(() -> {
                        RealPageProxy proxy = proxyOfThread.get();
                        if (proxy == null) {
                            proxy = new RealPageProxy(app.port());
                            proxies.add(proxy);
                            proxyOfThread.set(proxy);
                        }
                        String key = page.name() + "|" + config.id();
                        String path = page.path() + (config.skin() == null ? "" : "?skin=" + config.skin());
                        proxy.configure(new RealPageProxy.Load(config.theme(), "badge", null, "/cascade/badge-probe.js", presentConfig,
                                List.of(), Map.of(ORACLE_VIRTUAL, oracleCss), null));
                        present.put(key, parse(lines(chrome.dumpDom(proxy.url(path), List.of("--hide-scrollbars")), "ash-badge-results"), key + " present"));
                        proxy.configure(new RealPageProxy.Load(config.theme(), "badge", removedCss, "/cascade/badge-probe.js", removedConfig,
                                List.of(), Map.of(SCOPED_VIRTUAL, scopedCss), null));
                        removed.put(key, parse(lines(chrome.dumpDom(proxy.url(path), List.of("--hide-scrollbars")), "ash-badge-results"), key + " removed"));
                        return null;
                    }));
                }
            }
            for (Future<?> f : futures) {
                f.get();
            }
        } finally {
            pool.shutdownNow();
            proxies.forEach(RealPageProxy::close);
        }
        for (Page page : PAGES) {
            for (Config config : CONFIGS) {
                String key = page.name() + "|" + config.id();
                Probe p = present.get(key);
                Probe r = removed.get(key);
                String where = page.name() + " " + config.id();
                for (Probe probed : List.of(p, r)) {
                    probed.meta().forEach((k, v) -> {
                        if (k.startsWith("normal-") || k.startsWith("revealed-")) {
                            REVEAL_COUNTS.merge(k, Integer.parseInt(v), Integer::sum);
                        }
                    });
                    revealRestoreMismatches += Integer.parseInt(probed.meta().getOrDefault("reveal-restore-mismatch", "-1000000"));
                }
                int count = Integer.parseInt(p.meta().get("badge-count"));
                BADGES_PER_PAGE.merge(page.name(), count, Math::max);
                if (!config.theme().equals(p.meta().get("data-theme")) || !(config.skin() == null ? "" : config.skin()).equals(p.meta().get("data-skin"))) {
                    REAL_PRESENT.add(new Finding(where, "#configuration", config.id(), p.meta().get("data-theme") + "/" + p.meta().get("data-skin")));
                }
                if (p.meta().containsKey("error")) {
                    REAL_PRESENT.add(new Finding(where, "#error", "", p.meta().get("error")));
                }
                List<Row> legacyRows = p.phase("legacy");
                REAL_PRESENT.addAll(compare(where + " (Tailwind present)", p.props(), legacyRows, p.phase("ash"), APPLIED_REAL_PRESENT));
                if (r.meta().containsKey("error")) {
                    REAL_REMOVED.add(new Finding(where, "#error", "", r.meta().get("error")));
                }
                REAL_REMOVED.addAll(compare(where + " (Tailwind removed)", p.props(), r.phase("legacy"), r.phase("removed"), APPLIED_REAL_REMOVED));
                if (count != Integer.parseInt(r.meta().get("badge-count"))) {
                    REAL_REMOVED.add(new Finding(where, "#badge-count", String.valueOf(count), r.meta().get("badge-count")));
                }
                realComparisonsPerMode += (int) legacyRows.stream().filter(row -> row.kind().equals("badge")).count();
                // DOM shape of every badge as served (checked once per page: it does not depend on theme or skin).
                if (config.id().equals("light/none")) {
                    for (String[] shape : p.shapes()) {
                        shapeProblems(page.name(), shape);
                    }
                    p.phase("ash").stream().filter(row -> row.kind().equals("badge"))
                            .forEach(row -> CONTEXT_COVERAGE.add(page.name() + "#" + row.ordinal() + " " + row.variant() + " [" + row.context() + "]"));
                }
            }
        }
    }

    private static void shapeProblems(String page, String[] s) {
        String where = page + "#" + s[1];
        if (!s[2].equals("span")) {
            SHAPE_PROBLEMS.add(where + ": the badge is a <" + s[2] + ">, expected <span>");
        }
        if (!s[3].equals("class,data-badge,data-badge-variant")) {
            SHAPE_PROBLEMS.add(where + ": badge attributes are " + s[3]);
        }
        if (!(s[4].isEmpty() || s[4].equals("svg"))) {
            SHAPE_PROBLEMS.add(where + ": badge children are " + s[4]);
        }
        if (s[4].equals("svg") && !(s[5].equals("aria-hidden,class,fill,stroke,stroke-linecap,stroke-linejoin,stroke-width,viewBox") && s[6].equals("true"))) {
            SHAPE_PROBLEMS.add(where + ": icon attributes are " + s[5] + ", aria-hidden=" + s[6]);
        }
    }

    private static void writeReport() throws IOException {
        StringBuilder sb = new StringBuilder("Badge parity (real Chrome; no stored baseline).\n\n");
        int controlledFrames = CONFIGS.size();
        sb.append("CONTROLLED ORACLE: ").append(controlledFrames).append(" configurations x (Tailwind present, Tailwind removed), ")
                .append(CONTROLLED_ROWS.values().stream().mapToInt(Integer::intValue).sum()).append(" legacy rows measured\n");
        sb.append("   differences (Tailwind present): ").append(CONTROLLED_PRESENT.values().stream().mapToInt(List::size).sum())
                .append(" ; (Tailwind removed): ").append(CONTROLLED_REMOVED.values().stream().mapToInt(List::size).sum()).append('\n');
        sb.append("   normalizations used (rule: uses): ").append(APPLIED_CONTROLLED).append('\n');
        sb.append("   scoped legacy stylesheet vs full legacy, differences: ").append(SCOPED_FIDELITY.values().stream().mapToInt(List::size).sum())
                .append(" ; normalizations used: ").append(APPLIED_SCOPED).append('\n');
        appendFindings(sb, "FINDINGS, scoped legacy vs full legacy", SCOPED_FIDELITY.values().stream().flatMap(List::stream).toList());
        sb.append("NEGATIVE CONTROLS (each must produce a difference in the named property):\n");
        for (Control c : CONTROL_LIST) {
            List<String> found = CONTROLS.get(c.name()).stream().map(Finding::property).distinct().toList();
            sb.append("   ").append(found.contains(c.expectedProperty()) ? "DETECTED " : "MISSED   ").append(c.name()).append(" -> ").append(found).append('\n');
        }
        sb.append("REAL PAGES: ").append(PAGES.size()).append(" pages x ").append(CONFIGS.size()).append(" configurations (parallel, ")
                .append(realSeconds).append(" s)\n");
        sb.append("   Badge instances per page: ").append(BADGES_PER_PAGE).append(" ; total ").append(BADGES_PER_PAGE.values().stream().mapToInt(Integer::intValue).sum()).append('\n');
        sb.append("   instance comparisons per mode (instances x configurations): ").append(realComparisonsPerMode).append('\n');
        sb.append("   differences (Tailwind present): ").append(REAL_PRESENT.size()).append(" ; (Tailwind removed): ").append(REAL_REMOVED.size()).append('\n');
        sb.append("   normalizations used, Tailwind present: ").append(APPLIED_REAL_PRESENT).append('\n');
        sb.append("   normalizations used, Tailwind removed: ").append(APPLIED_REAL_REMOVED).append('\n');
        sb.append("   badge measurements taken as found (normal) vs with hidden ancestors revealed, then restored: ").append(REVEAL_COUNTS).append('\n');
        sb.append("   reveal restoration mismatches (elements not put back exactly): ").append(revealRestoreMismatches).append('\n');
        sb.append("   DOM shape problems: ").append(SHAPE_PROBLEMS.size()).append('\n');
        sb.append("   contexts exercised (page#ordinal variant [context], light/none): ").append(CONTEXT_COVERAGE.size()).append(" instances\n");
        appendFindings(sb, "FINDINGS, controlled, Tailwind present", CONTROLLED_PRESENT.values().stream().flatMap(List::stream).toList());
        appendFindings(sb, "FINDINGS, controlled, Tailwind removed", CONTROLLED_REMOVED.values().stream().flatMap(List::stream).toList());
        appendFindings(sb, "FINDINGS, real pages, Tailwind present", REAL_PRESENT);
        appendFindings(sb, "FINDINGS, real pages, Tailwind removed", REAL_REMOVED);
        SHAPE_PROBLEMS.stream().limit(20).forEach(p -> sb.append("   SHAPE ").append(p).append('\n'));
        Path out = Path.of(System.getProperty("ashlar.cascade.dir")).resolve("../../reports/parity/badge.txt").normalize();
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
            // Diagnostic breakdown: page and rendered state of both sides, with the config count.
            list.stream().collect(Collectors.groupingBy(f -> f.where().split(" ")[0] + " " + f.where().substring(Math.max(0, f.where().indexOf(" legacy{")) + 1).replaceAll("#popover-\\d+", "#popover-N"),
                    TreeMap::new, Collectors.counting()))
                    .forEach((key, count) -> sb.append("         by page/rendering: ").append(key).append(" x").append(count).append('\n'));
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
    void controlledOracleWithTailwindPresentMatchesTheLegacyBadgeInEveryConfiguration() {
        assertEquals(List.of(), CONTROLLED_PRESENT.values().stream().flatMap(List::stream).map(Finding::line).limit(25).toList());
        CONFIGS.forEach(c -> assertEquals(7, CONTROLLED_ROWS.get(c.id()), c.id() + ": five variants, one icon badge and its icon"));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheLegacyBadgeInEveryConfiguration() {
        assertEquals(List.of(), CONTROLLED_REMOVED.values().stream().flatMap(List::stream).map(Finding::line).limit(25).toList());
    }

    @Test
    void theScopedLegacyStylesheetRendersTheLegacyBadgeExactlyAsTheFullOneDoes() {
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
    void everyRealBadgeInstanceOnAllFifteenPagesMatchesItsLegacyRenderingWithTailwindPresent() {
        assertEquals(List.of(), REAL_PRESENT.stream().map(Finding::line).limit(25).toList(), REAL_PRESENT.size() + " differences in total");
    }

    @Test
    void everyRealBadgeInstanceOnAllFifteenPagesMatchesItsLegacyRenderingWithTailwindRemoved() {
        assertEquals(List.of(), REAL_REMOVED.stream().map(Finding::line).limit(25).toList(), REAL_REMOVED.size() + " differences in total");
    }

    @Test
    void realPageCoverageIsAll132InstancesOnAllFifteenPagesInAll22Configurations() {
        assertEquals(EXPECTED_BADGES, BADGES_PER_PAGE);
        assertEquals(132, BADGES_PER_PAGE.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(132 * 22, realComparisonsPerMode);
        // Hidden instances were revealed to be measured; every revealed element must have been put back exactly.
        assertEquals(0, revealRestoreMismatches, "revealed elements were not restored exactly");
        // The Badge's real contexts (table cell, definition list, card, popover, ...) are all represented.
        Set<String> contexts = CONTEXT_COVERAGE.stream().map(s -> s.substring(s.indexOf('[') + 1, s.length() - 1)).collect(Collectors.toSet());
        assertTrue(contexts.size() >= 9, "expected the nine distinct Badge contexts, found " + contexts);
    }

    @Test
    void theBadgeDomShapeIsPreservedOnEveryPage() {
        assertEquals(List.of(), SHAPE_PROBLEMS);
    }

    @Test
    void normalizationsAreExercisedAndStayWithinTheApprovedRules() {
        for (Map<String, Integer> applied : List.of(APPLIED_CONTROLLED, APPLIED_REAL_PRESENT, APPLIED_REAL_REMOVED)) {
            assertTrue(Set.of("H1", "H2", "H3", "H4").containsAll(applied.keySet()), "unexpected normalization rule: " + applied);
            assertTrue(applied.getOrDefault("H1", 0) > 0, "the ring should have needed H1: " + applied);
            assertTrue(applied.getOrDefault("H4", 0) > 0, "the radius should have needed H4: " + applied);
        }
        assertTrue(APPLIED_REAL_REMOVED.getOrDefault("H2", 0) > 0, "Tailwind removed should have needed H2 (preflight's border style): " + APPLIED_REAL_REMOVED);
        assertTrue(APPLIED_REAL_REMOVED.getOrDefault("H3", 0) > 0, "Tailwind removed should have needed H3 (the icon's vertical-align): " + APPLIED_REAL_REMOVED);
    }

    @AfterAll
    static void clear() {
        CONTROLLED_PRESENT.clear();
        CONTROLLED_REMOVED.clear();
        CONTROLS.clear();
        REAL_PRESENT.clear();
        REAL_REMOVED.clear();
    }
}
