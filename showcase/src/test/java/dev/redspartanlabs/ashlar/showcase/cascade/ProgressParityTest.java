package dev.redspartanlabs.ashlar.showcase.cascade;

import dev.redspartanlabs.ashlar.progress.ProgressSize;
import dev.redspartanlabs.ashlar.progress.ProgressStyles;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Progress's parity gates, measured in real Chrome.
 *
 * <p>Evidence comes in two kinds, and neither stores a baseline. (1) A controlled oracle: the Progress's legacy Tailwind
 * markup (compiled by the real Tailwind CLI) next to its migrated markup, in the same browser, for each of the 22 theme
 * and skin configurations, with Tailwind present and with Tailwind removed. It crosses the three sizes, the four
 * header shapes (label and percentage, label only, percentage only, none) and the values 0, 1, 25, 50, 99, 100, -20
 * and 140 in a block, and adds wrapping, narrow, long-unbroken-word, flex-column and flex-row contexts. (2) Every
 * Progress on the real showcase page, in the same 22 configurations: the real page is measured as served, then the
 * very same elements get their legacy classes back and are measured again, in the same run. Differences are compared
 * with {@link ParityNormalizer} (exact equality except H1 to H4, with their preconditions). Each row also carries the
 * element's box in its parent's coordinates. Negative controls show each gate can fail. Facts that do not depend on
 * the legacy rendering (the fill is exactly the clamped percentage of the track, the Foundry skin squares the track and
 * fill, the colours are the skin's) are checked directly.
 *
 * <p>Evidence is Chrome only. Computed-style and box parity is the visual evidence; there is no screenshot comparison.
 */
class ProgressParityTest {

    private static final String ASHLAR = "/ashlar-real/ashlar.css";
    private static final String ORACLE = "/generated/progress-oracle.css";
    private static final String APP = "/css/app.css";
    private static final String ORACLE_VIRTUAL = "/__parity/progress-oracle.css";
    private static final String SCOPED = "/generated/progress-legacy-scoped.css";
    private static final String SCOPED_VIRTUAL = "/__parity/progress-legacy-scoped.css";
    private static final String PROGRESS_PAGE = "/ashlar/components/progress";
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

    // ---- the controlled cases ----

    /** One Progress of the controlled oracle. {@code label} null means no label; {@code percent} is the showPercentage parameter. */
    private record Case(String layout, String size, String label, boolean percent, int value) {
        boolean hasHeader() {
            return label != null || percent;
        }

        int clamped() {
            return Math.max(0, Math.min(100, value));
        }

        /** The rows the probe reports for this Progress: itself, header, label, value, track and fill. */
        int rows() {
            return 1 + (hasHeader() ? 1 : 0) + (label != null ? 1 : 0) + (percent ? 1 : 0) + 2;
        }
    }

    private static final String LONG_WORD = "Supercalifragilisticexpialidocious_Supercalifragilisticexpialidocious_Supercalifragilistic";
    private static final List<String> SIZES = List.of("Small", "Medium", "Large");
    private static final List<Integer> VALUES = List.of(0, 1, 25, 50, 99, 100, -20, 140);

    private static final List<Case> CASES = cases();

    private static List<Case> cases() {
        List<Case> all = new ArrayList<>();
        for (String size : SIZES) {
            for (boolean hasLabel : new boolean[] {true, false}) {
                for (boolean percent : new boolean[] {true, false}) {
                    for (int value : VALUES) {
                        all.add(new Case("block", size, hasLabel ? "Migration progress" : null, percent, value));
                    }
                }
            }
        }
        all.add(new Case("narrow", "Medium", "Quarterly migration progress report", true, 65));
        all.add(new Case("narrow", "Medium", "Quarterly migration progress report", false, 65));
        all.add(new Case("narrow", "Small", "Quarterly migration progress report", true, 0));
        all.add(new Case("long-word", "Medium", LONG_WORD, true, 65));
        all.add(new Case("long-word", "Large", LONG_WORD, false, 65));
        for (String size : SIZES) {
            all.add(new Case("flex-column", size, "Syncing files", true, 40));
        }
        all.add(new Case("flex-row", "Medium", "Syncing files", true, 40));
        all.add(new Case("flex-row", "Medium", null, true, 40));
        return List.copyOf(all);
    }

    private static final Map<String, String> LAYOUT_STYLE = Map.of(
            "block", "box-sizing:border-box;width:320px",
            "narrow", "box-sizing:border-box;width:140px",
            "long-word", "box-sizing:border-box;width:200px",
            "flex-column", "box-sizing:border-box;display:flex;flex-direction:column;gap:12px;width:320px",
            "flex-row", "box-sizing:border-box;display:flex;flex-direction:row;gap:12px;width:320px");
    private static final List<String> LAYOUT_ORDER = List.of("block", "narrow", "long-word", "flex-column", "flex-row");

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
    private static final Map<String, Integer> APPLIED_PRESENT = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_REMOVED = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_SCOPED = new TreeMap<>();
    private static final List<Finding> REAL_PRESENT = new ArrayList<>();
    private static final List<Finding> REAL_REMOVED = new ArrayList<>();
    private static final Map<String, Integer> APPLIED_REAL_PRESENT = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_REAL_REMOVED = new TreeMap<>();
    private static final Map<String, Probe> REAL_PROBES = new LinkedHashMap<>();
    private static final Map<String, Integer> PROGRESS_PER_PAGE = new TreeMap<>();
    private static final List<String> FACT_PROBLEMS = new ArrayList<>();
    private static final List<String> SHAPE_PROBLEMS = new ArrayList<>();
    private static int realComparisonsPerMode;
    private static long realSeconds;

    // ---- markup ----

    private static ProgressLegacyMarkup legacy;

    /** One Progress exactly as progress.jte renders it (the template itself is measured on the real page). */
    private static String progress(Case c) {
        ProgressSize size = ProgressSize.from(c.size());
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"ash-progress\" data-progress data-progress-size=\"").append(size.name().toLowerCase(Locale.ROOT)).append("\">");
        if (c.hasHeader()) {
            sb.append("<div class=\"").append(ProgressLegacyMarkup.MIGRATED.get(c.label() != null ? "header-between" : "header-end")).append("\">");
            if (c.label() != null) {
                sb.append("<span class=\"ash-progress__label\">").append(c.label()).append("</span>");
            }
            if (c.percent()) {
                sb.append("<span class=\"ash-progress__value\">").append(c.clamped()).append("%</span>");
            }
            sb.append("</div>");
        }
        sb.append("<div role=\"progressbar\" aria-valuemin=\"0\" aria-valuemax=\"100\" aria-valuenow=\"").append(c.clamped()).append('"');
        if (c.label() != null) {
            sb.append(" aria-label=\"").append(c.label()).append('"');
        }
        sb.append(" class=\"").append(ProgressStyles.track(size)).append("\"><div class=\"").append(ProgressStyles.fill())
                .append("\" style=\"width: ").append(c.clamped()).append("%\"></div></div></div>");
        return sb.toString();
    }

    private static String markup(boolean legacyClasses) {
        StringBuilder sb = new StringBuilder();
        for (String layout : LAYOUT_ORDER) {
            sb.append("<div data-layout=\"").append(layout).append("\" style=\"").append(LAYOUT_STYLE.get(layout)).append("\">");
            boolean flex = layout.startsWith("flex");
            for (Case c : CASES) {
                if (c.layout().equals(layout)) {
                    sb.append(flex ? "" : "<div style=\"margin:0 0 12px\">").append(progress(c)).append(flex ? "" : "</div>");
                }
            }
            sb.append("</div><div style=\"height:16px\"></div>");
        }
        return legacyClasses ? legacy.rewrite(sb.toString()) : sb.toString();
    }

    private static String frame(String id, Config config, List<String> sheets, String extraCss, String body) {
        StringBuilder links = new StringBuilder();
        sheets.forEach(href -> links.append("<link rel=\"stylesheet\" href=\"").append(href).append("\">"));
        String skin = config.skin() == null ? "" : " data-skin=\"" + config.skin() + "\"";
        String doc = "<!doctype html><html data-theme=\"" + config.theme() + "\"" + skin + "><head>" + links + "<style>" + extraCss
                + "</style></head><body style=\"margin:0;padding:8px\">" + body + "</body></html>";
        return "<iframe data-id=\"" + id + "\" style=\"width:440px;height:6200px;border:0\" srcdoc=\""
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
     * their default text in black, and that would be a difference in the test page, not in the Progress.
     */
    private static String environment(Path generatedDir) throws IOException {
        return PageEnvironment.tailwindRemovedRules(generatedDir.resolve("phase-c-baseline.css")) + ":root[data-theme=\"dark\"]{color-scheme:dark}\n";
    }

    // ---- control experiments: each breaks the migrated Progress in ONE way and must be detected ----

    private record Control(String name, String config, String css, String expectedProperty) {
    }

    private static final String DARK = ":where([data-theme=dark],[data-theme=dark] *)";

    private static final List<Control> CONTROL_LIST = List.of(
            new Control("wrong fill colour", "light/none", ".ash-progress{--ash-progress-fill:red}", "background-color"),
            new Control("wrong dark track colour", "dark/none", ".ash-progress" + DARK + "{--ash-progress-track:var(--color-slate-200)}", "background-color"),
            new Control("wrong dark fill colour", "dark/none", ".ash-progress" + DARK + "{--ash-progress-fill:var(--color-blue-600)}", "background-color"),
            new Control("fill colour ignores the skin", "light/civic", ".ash-progress{--ash-progress-fill:#2563eb}", "background-color"),
            new Control("medium track too thin", "light/none", ".ash-progress__track--medium{height:.375rem}", "height"),
            new Control("large track too thick", "light/none", ".ash-progress__track--large{height:1rem}", "height"),
            new Control("track radius squared", "light/none", ".ash-progress__track{border-radius:1px}", "border-top-left-radius"),
            new Control("fill radius squared", "light/none", ".ash-progress__fill{border-radius:1px}", "border-top-left-radius"),
            new Control("header column gap changed", "light/none", ".ash-progress__header{column-gap:.5rem}", "column-gap"),
            new Control("header row gap changed", "light/none", ".ash-progress__header{row-gap:.5rem}", "row-gap"),
            new Control("between header no longer spreads", "light/none", ".ash-progress__header--between{justify-content:flex-start}", "justify-content"),
            new Control("header items start-aligned", "light/none", ".ash-progress__header{align-items:flex-start}", "align-items"),
            new Control("header no longer wraps", "light/none", ".ash-progress__header{flex-wrap:nowrap}", "flex-wrap"),
            new Control("header margin changed", "light/none", ".ash-progress__header{margin-bottom:.5rem}", "margin-bottom"),
            new Control("track overflow released", "light/none", ".ash-progress__track{overflow:visible}", "overflow-x"),
            new Control("label weight changed", "light/none", ".ash-progress__label{font-weight:400}", "font-weight"),
            new Control("value colour changed", "light/none", ".ash-progress__value{color:red}", "color"),
            new Control("header text size changed", "light/none", ".ash-progress__header{font-size:1rem}", "font-size"),
            new Control("header line height changed", "light/none", ".ash-progress__header{line-height:1}", "line-height"),
            new Control("wrapper no longer full width", "light/none", ".ash-progress{width:50%}", "width"),
            new Control("fill height changed", "light/none", ".ash-progress__fill{height:50%}", "height"));

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
            String label = where + " #" + e.ordinal() + " " + e.kind() + " " + e.variant() + " [" + e.context() + "]"
                    + " legacyBox=" + e.values()[w] + "x" + e.values()[h] + " actualBox=" + a.values()[w] + "x" + a.values()[h]
                    + " legacy{" + e.diagnostics() + "} actual{" + a.diagnostics() + "}";
            if (e.ordinal() != a.ordinal() || !e.kind().equals(a.kind()) || !e.variant().equals(a.variant())) {
                findings.add(new Finding(label, "#structure", e.ordinal() + e.kind() + e.variant(), a.ordinal() + a.kind() + a.variant()));
                continue;
            }
            ParityNormalizer.Result r = ParityNormalizer.compare(
                    ParityNormalizer.side(propNames, e.values(), e.parentDisplay()), ParityNormalizer.side(propNames, a.values(), a.parentDisplay()));
            r.remaining().forEach(d -> findings.add(new Finding(label, d.property(), d.expected(), d.actual())));
            r.applied().forEach(rule -> applied.merge(rule, 1, Integer::sum)); // "H2 border-top-style": the rule and the property it covered
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
            System.out.println("[progress-parity] Chrome not found - Progress parity SKIPPED (not passed).");
            Assumptions.abort("Chrome not found; the Progress parity checks were not run");
        }
        HeadlessChrome chrome = new HeadlessChrome(chromePath.get());
        legacy = new ProgressLegacyMarkup();
        runControlledFrames(chrome, Path.of(generatedDir));
        long start = System.nanoTime();
        runRealPages(chrome, Path.of(generatedDir), Path.of(bootJar));
        realSeconds = (System.nanoTime() - start) / 1_000_000_000L;
        writeReport();
    }

    // -- controlled oracle: legacy vs migrated, 22 configurations, Tailwind present and removed --

    /** Measures one set of frames in one browser run (the matrix is large, so it is measured in several runs and merged). */
    private static Probe runFrames(HeadlessChrome chrome, Path generatedDir, String frames, String what) throws Exception {
        String probeJs;
        try (InputStream in = ProgressParityTest.class.getResourceAsStream("/cascade/progress-probe.js")) {
            probeJs = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String page = "<!doctype html><html><body style=\"margin:0\">\n" + frames + "<script>" + probeJs + "</script>\n<script>"
                + "addEventListener('load',function(){setTimeout(function(){var out=['props\\t'+window.__ashProgress.PROPS.join(',')];"
                + "[].forEach.call(document.querySelectorAll('iframe'),function(f){var id=f.getAttribute('data-id');"
                + "window.__ashProgress.measure(f.contentWindow,f.contentDocument,id).forEach(function(r){out.push(r)});"
                + "window.__ashProgress.tokens(f.contentWindow,f.contentDocument,id).forEach(function(r){out.push(r)})});"
                + "var pre=document.createElement('pre');pre.id='ash-progress-frames';pre.textContent=out.join('\\n');document.body.appendChild(pre)},400)})"
                + "</script></body></html>";
        try (CascadeFixtureServer server = new CascadeFixtureServer(generatedDir)) {
            String dom = chrome.dumpDom(server.registerRaw(page), List.of("--hide-scrollbars"));
            return parse(lines(dom, "ash-progress-frames"), what);
        }
    }

    private static void runControlledFrames(HeadlessChrome chrome, Path generatedDir) throws Exception {
        String environment = environment(generatedDir);
        String migrated = markup(false);
        String legacyMarkup = markup(true);
        List<Row> allRows = new ArrayList<>();
        Map<String, Map<String, String>> allTokens = new LinkedHashMap<>();
        Probe first = null;
        // 22 configurations in runs of four, four frames each: the full oracle, Tailwind present, Tailwind removed, scoped legacy.
        for (int start = 0; start < CONFIGS.size(); start += 4) {
            StringBuilder frames = new StringBuilder();
            for (Config c : CONFIGS.subList(start, Math.min(CONFIGS.size(), start + 4))) {
                frames.append(frame(c.id() + "|L", c, with(ORACLE, SKIN_LINKS), "", legacyMarkup));
                frames.append(frame(c.id() + "|P", c, with(APP, SKIN_LINKS), "", migrated));
                frames.append(frame(c.id() + "|R", c, with(ASHLAR, SKIN_LINKS), environment, migrated));
                // The legacy Progress from the scoped stylesheet, in the same unstyled environment as |R.
                frames.append(frame(c.id() + "|S", c, with(SCOPED, SKIN_LINKS), environment, legacyMarkup));
            }
            Probe chunk = runFrames(chrome, generatedDir, frames.toString(), "controlled frames " + start);
            first = first == null ? chunk : first;
            allRows.addAll(chunk.rows());
            chunk.tokens().forEach(allTokens::put);
        }
        StringBuilder controls = new StringBuilder();
        for (Control control : CONTROL_LIST) {
            controls.append(frame("ctl:" + control.name() + "|P", config(control.config()), with(APP, SKIN_LINKS), control.css(), migrated));
        }
        Probe controlProbe = runFrames(chrome, generatedDir, controls.toString(), "control frames");
        allRows.addAll(controlProbe.rows());
        // The customisation surface: an instance property wins over the class, and an unlayered rule wins over the component, in both themes.
        Case sample = new Case("ovr", "Medium", "Migration progress", true, 50);
        String instance = progress(sample).replace("data-progress ", "data-progress style=\"--ash-progress-track: rgb(1, 2, 3); --ash-progress-fill: rgb(4, 5, 6)\" ");
        String unlayeredCss = "[data-progress]{--ash-progress-track: rgb(7, 8, 9); --ash-progress-fill: rgb(10, 11, 12)}";
        String wrapped = "<div data-layout=\"ovr\" style=\"box-sizing:border-box;width:320px\">%s</div>";
        StringBuilder overrides = new StringBuilder();
        for (String theme : List.of("light", "dark")) {
            overrides.append(frame("ovr:inline-" + theme + "|P", config(theme + "/none"), with(APP, SKIN_LINKS), "", wrapped.formatted(instance)));
            overrides.append(frame("ovr:unlayered-" + theme + "|P", config(theme + "/none"), with(APP, SKIN_LINKS), unlayeredCss, wrapped.formatted(progress(sample))));
        }
        Probe overrideProbe = runFrames(chrome, generatedDir, overrides.toString(), "override frames");
        allRows.addAll(overrideProbe.rows());
        Probe probe = new Probe(first.props(), first.meta(), List.of(), allTokens, allRows);
        props = probe.props();
        int expectedRows = CASES.stream().mapToInt(Case::rows).sum();
        for (Config c : CONFIGS) {
            List<Row> legacyRows = probe.phase(c.id() + "|L");
            CONTROLLED_ROWS.put(c.id(), legacyRows.size());
            assertEquals(expectedRows, legacyRows.size(), c.id() + ": the rows the cases must produce");
            CONTROLLED_PRESENT.put(c.id(), compare(c.id() + " (Tailwind present)", props, legacyRows, probe.phase(c.id() + "|P"), APPLIED_PRESENT));
            // Tailwind removed: against the legacy Progress given the same unstyled surroundings ...
            CONTROLLED_REMOVED_SCOPED.put(c.id(), compare(c.id() + " (Tailwind removed vs scoped legacy)", props, probe.phase(c.id() + "|S"), probe.phase(c.id() + "|R"), APPLIED_REMOVED));
            // ... and against the full legacy rendering, which also has Tailwind's preflight around it.
            CONTROLLED_REMOVED_FULL.put(c.id(), compare(c.id() + " (Tailwind removed vs full legacy)", props, legacyRows, probe.phase(c.id() + "|R"), APPLIED_REMOVED));
            // Fidelity of the scoped legacy stylesheet: it must render the legacy Progress exactly as the full one does.
            SCOPED_FIDELITY.put(c.id(), compare(c.id() + " (scoped legacy vs full legacy)", props, legacyRows, probe.phase(c.id() + "|S"), APPLIED_SCOPED));
        }
        for (Control control : CONTROL_LIST) {
            List<Row> expected = probe.phase(control.config() + "|L");
            CONTROLS.put(control.name(), compare("control: " + control.name(), props, expected, probe.phase("ctl:" + control.name() + "|P"), new TreeMap<>()));
        }
        FRAME_PROBES.put("controlled", probe);
        for (Config c : CONFIGS) {
            controlledFacts(c, probe);
        }
    }

    // -- real page: every Progress on the showcase page, 22 configurations, in parallel --

    private static String legacyConfigJson(String phase, String stylesheet, boolean detach) {
        return "{\"phase\":\"" + phase + "\",\"legacy\":{\"stylesheet\":\"" + stylesheet + "\",\"detach\":" + detach + ",\"map\":" + legacy.mapJson() + "}}";
    }

    /** What the showcase page renders, in document order: shape and the clamped value (from the unmodified baseline build). */
    private static final List<String> EXPECTED_INSTANCES = List.of(
            "medium/lv/page 65", "medium/-v/page 0", "medium/-v/page 25", "medium/-v/page 50", "medium/-v/page 75", "medium/-v/page 100",
            "medium/lv/page 65", "medium/lv/page 40", "medium/lv/page 90", "medium/l-/page 55", "medium/--/page 30",
            "small/lv/page 60", "medium/lv/page 60", "large/lv/page 60", "medium/lv/page 0", "medium/lv/page 100");

    private static void runRealPages(HeadlessChrome chrome, Path generatedDir, Path bootJar) throws Exception {
        byte[] oracleCss = Files.readAllBytes(generatedDir.resolve("progress-oracle.css"));
        byte[] ashlarCss = Files.readAllBytes(Path.of(System.getProperty("ashlar.css.generated")));
        String environment = environment(generatedDir);
        byte[] removedCss = (new String(ashlarCss, StandardCharsets.UTF_8) + "\n" + environment).getBytes(StandardCharsets.UTF_8);
        byte[] scopedCss = Files.readAllBytes(generatedDir.resolve("progress-legacy-scoped.css"));
        String presentConfig = legacyConfigJson("ash", ORACLE_VIRTUAL, false);
        // Tailwind removed: the migrated Progress is measured in the unstyled page, then the legacy Progress is measured on the
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
                    String path = PROGRESS_PAGE + (config.skin() == null ? "" : "?skin=" + config.skin());
                    proxy.configure(new RealPageProxy.Load(config.theme(), "progress", null, "/cascade/progress-probe.js", presentConfig,
                            List.of(), Map.of(ORACLE_VIRTUAL, oracleCss), null));
                    present.put(config.id(), parse(lines(chrome.dumpDom(proxy.url(path), List.of("--hide-scrollbars")), "ash-progress-results"), config.id() + " present"));
                    proxy.configure(new RealPageProxy.Load(config.theme(), "progress", removedCss, "/cascade/progress-probe.js", removedConfig,
                            List.of(), Map.of(SCOPED_VIRTUAL, scopedCss), null));
                    removed.put(config.id(), parse(lines(chrome.dumpDom(proxy.url(path), List.of("--hide-scrollbars")), "ash-progress-results"), config.id() + " removed"));
                    return null;
                }));
            }
            for (Future<?> f : futures) {
                f.get();
            }
            countProgressOnEveryPage(app.port());
        } finally {
            pool.shutdownNow();
            proxies.forEach(RealPageProxy::close);
        }
        for (Config config : CONFIGS) {
            Probe p = present.get(config.id());
            Probe r = removed.get(config.id());
            REAL_PROBES.put(config.id() + "|present", p);
            REAL_PROBES.put(config.id() + "|removed", r);
            String where = "progress page " + config.id();
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
            realComparisonsPerMode += (int) p.phase("legacy").stream().filter(row -> row.kind().equals("progress")).count();
            realFacts(config, p);
            if (config.id().equals("light/none")) {
                p.shapes().forEach(ProgressParityTest::shapeProblems);
            }
        }
    }

    /** Every catalog page, the home page and the index, as served: how many Progress components each renders. */
    private static void countProgressOnEveryPage(int port) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        List<String> paths = new ArrayList<>(List.of("/ashlar/", "/ashlar/components"));
        for (ComponentPage page : ComponentCatalog.PAGES) {
            paths.add("/ashlar/components/" + page.slug());
        }
        for (String path : paths) {
            HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            assertEquals(200, response.statusCode(), path);
            int hooks = (int) Pattern.compile("\\bdata-progress\\b(?!-)").matcher(response.body()).results().count();
            assertEquals(ProgressLegacyMarkup.count(response.body()), hooks, path + ": every data-progress must be a migrated Progress");
            PROGRESS_PER_PAGE.put(path, hooks);
        }
    }

    // -- facts: what must be true of the migrated Progress whatever the legacy rendering did --

    private static void expect(String what, String expected, String actual) {
        if (expected == null || !expected.equals(actual)) {
            FACT_PROBLEMS.add(what + ": expected " + expected + " but was " + actual);
        }
    }

    private static boolean isPill(String radius) {
        return ParityNormalizer.px(radius) > 1_000_000;
    }

    /** Facts about one Progress's rows, in a probe whose rows are in document order: ordinal -> rows by kind. */
    private static Map<Integer, Map<String, Row>> byOrdinal(List<Row> rows) {
        Map<Integer, Map<String, Row>> map = new TreeMap<>();
        rows.forEach(r -> map.computeIfAbsent(r.ordinal(), k -> new LinkedHashMap<>()).put(r.kind(), r));
        return map;
    }

    private static void progressFacts(String where, Config config, Probe p, List<Row> rows, List<Integer> clamped) {
        boolean dark = config.theme().equals("dark");
        boolean foundry = "foundry".equals(config.skin());
        Map<String, String> tok = p.tokens().get(where.startsWith("controlled") ? config.id() + "|P" : "page");
        Map<Integer, Map<String, Row>> parts = byOrdinal(rows);
        assertEquals(clamped.size(), parts.size(), where + ": number of progress components");
        for (Map.Entry<Integer, Map<String, Row>> e : parts.entrySet()) {
            Map<String, Row> r = e.getValue();
            String here = where + " #" + e.getKey() + " " + r.get("progress").variant() + ": ";
            Row track = r.get("track");
            Row fill = r.get("fill");
            String size = r.get("progress").variant().substring(0, r.get("progress").variant().indexOf('/'));
            expect(here + "track height", switch (size) {
                case "small" -> "6";
                case "large" -> "12";
                default -> "8";
            }, p.value(track, "@h"));
            expect(here + "fill is as tall as the track", p.value(track, "@h"), p.value(fill, "@h"));
            double trackWidth = Double.parseDouble(p.value(track, "@w"));
            double fillWidth = Double.parseDouble(p.value(fill, "@w"));
            double expectedWidth = trackWidth * clamped.get(e.getKey() - 1) / 100.0;
            if (Math.abs(fillWidth - expectedWidth) > 0.02) {
                FACT_PROBLEMS.add(here + "fill width " + fillWidth + " is not " + clamped.get(e.getKey() - 1) + "% of the track's " + trackWidth);
            }
            expect(here + "track overflow", "hidden", p.value(track, "overflow-x"));
            for (Row part : List.of(track, fill)) {
                for (String corner : List.of("top-left", "top-right", "bottom-right", "bottom-left")) {
                    String radius = p.value(part, "border-" + corner + "-radius");
                    if (foundry) {
                        expect(here + part.kind() + " " + corner + " radius under Foundry", "2px", radius);
                    } else if (!isPill(radius)) {
                        FACT_PROBLEMS.add(here + part.kind() + " " + corner + " radius is not a full pill: " + radius);
                    }
                }
            }
            if (r.containsKey("header")) {
                Row header = r.get("header");
                expect(here + "header display", "flex", p.value(header, "display"));
                expect(here + "header wraps", "wrap", p.value(header, "flex-wrap"));
                expect(here + "header justification", r.containsKey("label") ? "space-between" : "flex-end", p.value(header, "justify-content"));
                expect(here + "header column gap", "12px", p.value(header, "column-gap"));
                expect(here + "header row gap", "4px", p.value(header, "row-gap"));
                expect(here + "header margin", "6px", p.value(header, "margin-bottom"));
                expect(here + "header font size", "14px", p.value(header, "font-size"));
                expect(here + "header line height", "20px", p.value(header, "line-height"));
            }
            if (r.containsKey("label")) {
                expect(here + "label weight", "500", p.value(r.get("label"), "font-weight"));
            }
            if (config.skin() != null) {
                if (tok == null) {
                    FACT_PROBLEMS.add(here + "no palette tokens were measured, so the skin colours cannot be checked");
                    continue;
                }
                // Only a skin defines the whole palette; with no skin the colours are parity-checked against the legacy rendering.
                expect(here + "track colour is the skin's --color-slate-" + (dark ? "700" : "200"), tok.get("--color-slate-" + (dark ? "700" : "200")), p.value(track, "background-color"));
                expect(here + "fill colour is the skin's --color-blue-" + (dark ? "500" : "600"), tok.get("--color-blue-" + (dark ? "500" : "600")), p.value(fill, "background-color"));
                if (r.containsKey("label")) {
                    expect(here + "label colour is the skin's --color-slate-" + (dark ? "300" : "700"), tok.get("--color-slate-" + (dark ? "300" : "700")), p.value(r.get("label"), "color"));
                }
                if (r.containsKey("value")) {
                    expect(here + "value colour is the skin's --color-slate-" + (dark ? "400" : "500"), tok.get("--color-slate-" + (dark ? "400" : "500")), p.value(r.get("value"), "color"));
                }
            }
        }
    }

    private static void controlledFacts(Config config, Probe probe) {
        List<Integer> clamped = CASES.stream().map(Case::clamped).toList();
        progressFacts("controlled " + config.id(), config, probe, probe.phase(config.id() + "|P"), clamped);
    }

    private static void realFacts(Config config, Probe p) {
        List<Integer> clamped = EXPECTED_INSTANCES.stream().map(s -> Integer.parseInt(s.substring(s.indexOf(' ') + 1))).toList();
        progressFacts("real " + config.id(), config, p, p.phase("ash"), clamped);
    }

    private static void shapeProblems(String[] s) {
        String where = "progress #" + s[1];
        if (!s[2].equals("class,data-progress,data-progress-size")) {
            SHAPE_PROBLEMS.add(where + ": wrapper attributes " + s[2]);
        }
        boolean hasHeader = s[3].startsWith("div,div");
        if (!s[3].equals("div,div") && !s[3].equals("div")) {
            SHAPE_PROBLEMS.add(where + ": wrapper children " + s[3] + ", expected an optional header and then the track");
        }
        if (!s[4].matches("|span|span,span")) {
            SHAPE_PROBLEMS.add(where + ": header children " + s[4]);
        }
        if (hasHeader && s[4].isEmpty()) {
            SHAPE_PROBLEMS.add(where + ": an empty header should not be rendered");
        }
        if (!s[5].matches("aria-label,aria-valuemax,aria-valuemin,aria-valuenow,class,role|aria-valuemax,aria-valuemin,aria-valuenow,class,role")) {
            SHAPE_PROBLEMS.add(where + ": track attributes " + s[5]);
        }
        if (!s[6].equals("div")) {
            SHAPE_PROBLEMS.add(where + ": the track's only child is the fill, found " + s[6]);
        }
        if (!s[7].equals("class,style")) {
            SHAPE_PROBLEMS.add(where + ": fill attributes " + s[7]);
        }
        if (!s[9].equals("width: " + s[8] + "%")) {
            SHAPE_PROBLEMS.add(where + ": the fill's width " + s[9] + " must equal aria-valuenow " + s[8]);
        }
        if (!s[10].isEmpty() && !s[10].equals(s[8] + "%")) {
            SHAPE_PROBLEMS.add(where + ": the percentage text " + s[10] + " must equal aria-valuenow " + s[8]);
        }
        for (String token : s[12].split(" ")) {
            if (!token.startsWith("ash-progress")) {
                SHAPE_PROBLEMS.add(where + ": a class that is not Ashlar's: " + token);
            }
        }
    }

    private static void writeReport() throws IOException {
        StringBuilder sb = new StringBuilder("Progress parity (real Chrome; no stored baseline).\n\n");
        sb.append("CONTROLLED ORACLE: ").append(CONFIGS.size()).append(" configurations x ").append(CASES.size()).append(" progress bars (")
                .append(SIZES.size()).append(" sizes x 4 header shapes x ").append(VALUES.size()).append(" values in a block, plus wrapping, narrow, long-word, flex-column and flex-row cases) x (Tailwind present, Tailwind removed), ")
                .append(CONTROLLED_ROWS.values().stream().mapToInt(Integer::intValue).sum()).append(" legacy rows measured, ").append(props.size()).append(" properties each\n");
        sb.append("   differences (Tailwind present): ").append(CONTROLLED_PRESENT.values().stream().mapToInt(List::size).sum())
                .append(" ; (removed vs scoped legacy): ").append(CONTROLLED_REMOVED_SCOPED.values().stream().mapToInt(List::size).sum())
                .append(" ; (removed vs full legacy): ").append(CONTROLLED_REMOVED_FULL.values().stream().mapToInt(List::size).sum()).append('\n');
        sb.append("   normalizations used, Tailwind present: ").append(APPLIED_PRESENT).append(" ; Tailwind removed: ").append(APPLIED_REMOVED).append('\n');
        sb.append("   scoped legacy stylesheet vs full legacy, differences: ").append(SCOPED_FIDELITY.values().stream().mapToInt(List::size).sum())
                .append(" ; normalizations used: ").append(APPLIED_SCOPED).append('\n');
        sb.append("NEGATIVE CONTROLS (each must produce a difference in the named property):\n");
        for (Control c : CONTROL_LIST) {
            List<String> found = CONTROLS.get(c.name()).stream().map(Finding::property).distinct().toList();
            sb.append("   ").append(found.contains(c.expectedProperty()) ? "DETECTED " : "MISSED   ").append(c.name()).append(" -> ").append(found).append('\n');
        }
        sb.append("REAL PAGE: the Progress page x ").append(CONFIGS.size()).append(" configurations (parallel, ").append(realSeconds).append(" s)\n");
        sb.append("   progress bars per page (every catalog page, home and index): ").append(PROGRESS_PER_PAGE.entrySet().stream().filter(e -> e.getValue() > 0).toList())
                .append(" ; pages scanned ").append(PROGRESS_PER_PAGE.size()).append('\n');
        sb.append("   instance comparisons per mode (instances x configurations): ").append(realComparisonsPerMode).append('\n');
        sb.append("   differences (Tailwind present): ").append(REAL_PRESENT.size()).append(" ; (Tailwind removed): ").append(REAL_REMOVED.size()).append('\n');
        sb.append("   normalizations used, Tailwind present: ").append(APPLIED_REAL_PRESENT).append('\n');
        sb.append("   normalizations used, Tailwind removed: ").append(APPLIED_REAL_REMOVED).append('\n');
        sb.append("   facts not met: ").append(FACT_PROBLEMS.size()).append(" ; DOM shape problems: ").append(SHAPE_PROBLEMS.size()).append('\n');
        appendFindings(sb, "FINDINGS, controlled, Tailwind present", CONTROLLED_PRESENT.values().stream().flatMap(List::stream).toList());
        appendFindings(sb, "FINDINGS, controlled, removed vs scoped legacy", CONTROLLED_REMOVED_SCOPED.values().stream().flatMap(List::stream).toList());
        appendFindings(sb, "FINDINGS, controlled, removed vs full legacy", CONTROLLED_REMOVED_FULL.values().stream().flatMap(List::stream).toList());
        appendFindings(sb, "FINDINGS, scoped legacy vs full legacy", SCOPED_FIDELITY.values().stream().flatMap(List::stream).toList());
        appendFindings(sb, "FINDINGS, real page, Tailwind present", REAL_PRESENT);
        appendFindings(sb, "FINDINGS, real page, Tailwind removed", REAL_REMOVED);
        FACT_PROBLEMS.stream().limit(20).forEach(p -> sb.append("   FACT ").append(p).append('\n'));
        SHAPE_PROBLEMS.stream().limit(20).forEach(p -> sb.append("   SHAPE ").append(p).append('\n'));
        Path out = Path.of(System.getProperty("ashlar.cascade.dir")).resolve("../../reports/parity/progress.txt").normalize();
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
        assertTrue(CONFIGS.stream().anyMatch(c -> c.id().equals("light/foundry")) && CONFIGS.stream().anyMatch(c -> c.id().equals("dark/foundry")));
    }

    @Test
    void theControlledCasesCoverEverySizeHeaderShapeAndValueAndEveryContext() {
        long block = CASES.stream().filter(c -> c.layout().equals("block")).count();
        assertEquals(3 * 4 * 8, block);
        assertEquals(Set.of("Small", "Medium", "Large"), CASES.stream().map(Case::size).collect(Collectors.toSet()));
        assertEquals(Set.of(0, 1, 25, 50, 99, 100, -20, 140), CASES.stream().filter(c -> c.layout().equals("block")).map(Case::value).collect(Collectors.toSet()));
        assertEquals(Set.of("lv", "l-", "-v", "--"), CASES.stream().filter(c -> c.layout().equals("block"))
                .map(c -> (c.label() != null ? "l" : "-") + (c.percent() ? "v" : "-")).collect(Collectors.toSet()));
        assertEquals(Set.copyOf(LAYOUT_ORDER), CASES.stream().map(Case::layout).collect(Collectors.toSet()));
        assertEquals(CASES.stream().mapToInt(Case::rows).sum(), CONTROLLED_ROWS.get("light/none").intValue());
    }

    @Test
    void controlledOracleWithTailwindPresentMatchesTheLegacyProgressInEveryConfiguration() {
        assertEquals(List.of(), CONTROLLED_PRESENT.values().stream().flatMap(List::stream).map(Finding::line).limit(25).toList());
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheLegacyProgressGivenTheSameSurroundings() {
        assertEquals(List.of(), CONTROLLED_REMOVED_SCOPED.values().stream().flatMap(List::stream).map(Finding::line).limit(25).toList());
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheFullLegacyRendering() {
        assertEquals(List.of(), CONTROLLED_REMOVED_FULL.values().stream().flatMap(List::stream).map(Finding::line).limit(25).toList());
    }

    @Test
    void theScopedLegacyStylesheetRendersTheLegacyProgressExactlyAsTheFullOneDoes() {
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
    void theFillIsExactlyTheClampedPercentageOfTheTrackAndEverythingElseAboutTheMigratedProgressHolds() {
        assertEquals(List.of(), FACT_PROBLEMS.stream().limit(25).toList(), FACT_PROBLEMS.size() + " facts not met");
    }

    @Test
    void theHeaderWrapsInTheNarrowContextAndTheLongWordStaysAWord() {
        Probe probe = FRAME_PROBES.get("controlled");
        List<Row> rows = probe.phase("light/none|P");
        int h = props.indexOf("@h");
        double narrowHeader = rows.stream().filter(r -> r.kind().equals("header") && r.variant().equals("medium/lv/narrow")).mapToDouble(r -> Double.parseDouble(r.values()[h])).max().orElseThrow();
        double blockHeader = rows.stream().filter(r -> r.kind().equals("header") && r.variant().equals("medium/lv/block")).mapToDouble(r -> Double.parseDouble(r.values()[h])).max().orElseThrow();
        assertTrue(narrowHeader > blockHeader, "the narrow header must wrap onto more than one line: " + narrowHeader + " vs " + blockHeader);
        double longWord = rows.stream().filter(r -> r.kind().equals("label") && r.variant().equals("medium/lv/long-word")).mapToDouble(r -> Double.parseDouble(r.values()[props.indexOf("@w")])).max().orElseThrow();
        assertTrue(longWord > 200, "an unbroken word is wider than its 200px container, as it was: " + longWord);
    }

    @Test
    void aCallersCustomColoursBeatTheDefaultPaletteInBothThemesAndBothWaysOfSettingThem() {
        Probe probe = FRAME_PROBES.get("controlled");
        for (String theme : List.of("light", "dark")) {
            Map<String, Row> plain = byOrdinal(probe.phase(theme + "/none|P")).get(1 + (int) CASES.stream().takeWhile(c -> !(c.layout().equals("block") && c.size().equals("Medium") && c.label() != null && c.percent() && c.value() == 50)).count());
            String defaultTrack = probe.value(plain.get("track"), "background-color");
            String defaultFill = probe.value(plain.get("fill"), "background-color");
            assertNotEquals("rgba(0, 0, 0, 0)", defaultTrack);
            for (String[] c : new String[][] {{"inline", "rgb(1, 2, 3)", "rgb(4, 5, 6)"}, {"unlayered", "rgb(7, 8, 9)", "rgb(10, 11, 12)"}}) {
                String phase = "ovr:" + c[0] + "-" + theme + "|P";
                Map<String, Row> parts = byOrdinal(probe.phase(phase)).get(1);
                assertEquals(c[1], probe.value(parts.get("track"), "background-color"), phase + ": the track takes the caller's colour");
                assertEquals(c[2], probe.value(parts.get("fill"), "background-color"), phase + ": the fill takes the caller's colour");
                assertNotEquals(defaultTrack, probe.value(parts.get("track"), "background-color"), phase);
                assertNotEquals(defaultFill, probe.value(parts.get("fill"), "background-color"), phase);
            }
        }
        // The two themes really do have different defaults, so winning "over the default" means something in each mode.
        int ordinal = 1 + (int) CASES.stream().takeWhile(c -> !(c.layout().equals("block") && c.size().equals("Medium") && c.label() != null && c.percent() && c.value() == 50)).count();
        assertNotEquals(probe.value(byOrdinal(probe.phase("light/none|P")).get(ordinal).get("track"), "background-color"),
                probe.value(byOrdinal(probe.phase("dark/none|P")).get(ordinal).get("track"), "background-color"));
        assertNotEquals(probe.value(byOrdinal(probe.phase("light/none|P")).get(ordinal).get("fill"), "background-color"),
                probe.value(byOrdinal(probe.phase("dark/none|P")).get(ordinal).get("fill"), "background-color"));
    }

    @Test
    void everyRealProgressMatchesItsLegacyRenderingWithTailwindPresent() {
        assertEquals(List.of(), REAL_PRESENT.stream().map(Finding::line).limit(25).toList(), REAL_PRESENT.size() + " differences in total");
    }

    @Test
    void everyRealProgressMatchesItsLegacyRenderingWithTailwindRemoved() {
        assertEquals(List.of(), REAL_REMOVED.stream().map(Finding::line).limit(25).toList(), REAL_REMOVED.size() + " differences in total");
    }

    @Test
    void realPageCoverageIsAllSixteenInstancesInAll22ConfigurationsAndNoOtherPageRendersAProgress() {
        assertEquals(16 * 22, realComparisonsPerMode);
        Map<String, Integer> withProgress = PROGRESS_PER_PAGE.entrySet().stream().filter(e -> e.getValue() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));
        assertEquals(Map.of(PROGRESS_PAGE, 16), withProgress);
        assertEquals(2 + ComponentCatalog.PAGES.size(), PROGRESS_PER_PAGE.size(), "every catalog page, the home page and the index were scanned");
        for (Config c : CONFIGS) {
            Probe p = REAL_PROBES.get(c.id() + "|present");
            assertEquals("16", p.meta().get("progress-count"), c.id());
            assertEquals("0", p.meta().get("progress-without-a-box"), c.id() + ": every progress bar must be rendered, none is hidden");
            // The sixteen showcase instances, in document order: shape and clamped value.
            List<String> seen = new ArrayList<>();
            for (int i = 0; i < p.shapes().size(); i++) {
                String variant = p.phase("ash").stream().filter(r -> r.kind().equals("progress")).toList().get(i).variant();
                seen.add(variant + " " + p.shapes().get(i)[8]);
            }
            assertEquals(EXPECTED_INSTANCES, seen, c.id());
        }
    }

    @Test
    void theProgressDomShapeAndAccessibilityAttributesArePreservedAndCarryOnlyAshlarClasses() {
        assertEquals(List.of(), SHAPE_PROBLEMS);
        assertEquals(16, REAL_PROBES.get("light/none|present").shapes().size());
        // Out-of-range values: aria-valuenow, the percentage text and the fill width are all clamped together.
        List<String[]> shapes = REAL_PROBES.get("light/none|present").shapes();
        assertEquals("0", shapes.get(14)[8]);
        assertEquals("0%", shapes.get(14)[10]);
        assertEquals("100", shapes.get(15)[8]);
        assertEquals("100%", shapes.get(15)[10]);
        assertEquals("value = -20", shapes.get(14)[11]);
    }

    @Test
    void whileTheScopedLegacySheetIsLinkedTheLegacyNamesAreOffEveryOtherElementAndAreRestoredExactly() {
        for (Config c : CONFIGS) {
            Probe removed = REAL_PROBES.get(c.id() + "|removed");
            assertEquals("0", removed.meta().get("detach-restore-mismatch"), c.id() + ": detached class names were not restored exactly");
            assertTrue(Integer.parseInt(removed.meta().get("detached-elements")) > 20, c.id() + ": the page around the progress bars uses the legacy names, so many elements must have been detached: " + removed.meta().get("detached-elements"));
            assertEquals("0", REAL_PROBES.get(c.id() + "|present").meta().getOrDefault("detached-elements", "0"), c.id() + ": nothing is detached while Tailwind is present");
        }
    }

    @Test
    void theFoundrySkinStillSquaresTheTrackAndTheFillInBothThemes() {
        for (String theme : List.of("light", "dark")) {
            Probe p = REAL_PROBES.get(theme + "/foundry|present");
            for (Row r : p.phase("ash")) {
                if (r.kind().equals("track") || r.kind().equals("fill")) {
                    assertEquals("2px", p.value(r, "border-top-left-radius"), theme + " " + r.kind() + " #" + r.ordinal());
                }
            }
            Probe other = REAL_PROBES.get(theme + "/civic|present");
            assertTrue(other.phase("ash").stream().filter(r -> r.kind().equals("track")).allMatch(r -> isPill(other.value(r, "border-top-left-radius"))));
        }
    }

    /** The one normalization the Tailwind-removed comparisons use, and the only four properties it may cover. */
    private static final List<String> H2_PROPERTIES = List.of("H2 border-top-style", "H2 border-right-style", "H2 border-bottom-style", "H2 border-left-style");

    /**
     * Why {@code applied} is not exactly "H2, on each of the four physical border-style properties, once per measured row
     * and comparison" (empty when it is). Preflight gives every element of the legacy Progress `border: 0 solid`, and the
     * migrated Progress has no reset, so each of its zero-width borders is `none` where the legacy one is `solid`: four
     * properties per row, nothing else. Any other rule, any other property, or any other count is a finding.
     */
    static List<String> h2Problems(Map<String, Integer> applied, int rowComparisons) {
        List<String> problems = new ArrayList<>();
        if (!applied.keySet().equals(Set.copyOf(H2_PROPERTIES))) {
            problems.add("normalizations used " + new TreeMap<>(applied) + ", expected exactly " + H2_PROPERTIES);
        }
        for (String property : H2_PROPERTIES) {
            Integer used = applied.get(property);
            if (used == null || used != rowComparisons) {
                problems.add(property + " was used " + used + " times, expected exactly " + rowComparisons + " (once per measured row and comparison)");
            }
        }
        return problems;
    }

    /** The rows the probe reports for one real-page Progress, from its shape and value: itself, header, label, value, track and fill. */
    private static int realRows(String instance) {
        String shape = instance.substring(instance.indexOf('/') + 1, instance.indexOf('/', instance.indexOf('/') + 1));
        boolean label = shape.charAt(0) == 'l';
        boolean value = shape.charAt(1) == 'v';
        return 1 + (label || value ? 1 : 0) + (label ? 1 : 0) + (value ? 1 : 0) + 2;
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
        String oracle = Files.readString(dir.resolve("progress-oracle.css"), StandardCharsets.UTF_8);
        String scoped = Files.readString(dir.resolve("progress-legacy-scoped.css"), StandardCharsets.UTF_8);
        for (String token : legacy.allTokens()) {
            assertTrue(compiles(oracle, token), "the Progress oracle does not compile " + token);
            // The scoped stylesheet reads no source but progress-legacy.html, so this also proves its @source path resolves.
            assertTrue(compiles(scoped, token), "the scoped stylesheet does not compile " + token);
        }
        // Cumulative, in order Badge -> Alert -> Divider -> Progress: the earlier legacy classes are still in the Progress oracle.
        for (String classes : new BadgeLegacyMarkup().variants().values()) {
            for (String token : classes.split("\\s+")) {
                assertTrue(oracle.contains(escapeClass(token)), "the Progress oracle lost the Badge legacy class " + token);
            }
        }
        for (String token : new AlertLegacyMarkup().allTokens()) {
            assertTrue(compiles(oracle, token), "the Progress oracle lost the Alert legacy class " + token);
        }
        for (String token : new DividerLegacyMarkup().allTokens()) {
            assertTrue(compiles(oracle, token), "the Progress oracle lost the Divider legacy class " + token);
        }
        assertFalse(scoped.contains("html,:host"), "the scoped legacy stylesheet must not carry Tailwind's page-wide base");
    }

    @Test
    void normalizationsStayWithinTheApprovedRulesAndTailwindPresentNeedsNone() {
        assertEquals(Map.of(), APPLIED_PRESENT, "with Tailwind present the controlled Progress must match exactly");
        assertEquals(Map.of(), APPLIED_REAL_PRESENT, "with Tailwind present the real Progress must match exactly");
    }

    @Test
    void theControlledTailwindRemovedComparisonsUseH2ExactlyOncePerMeasuredRowAndComparisonOnTheFourPhysicalBorderStyles() {
        // Two comparisons feed APPLIED_REMOVED for every configuration (removed vs scoped legacy, removed vs full legacy), each over
        // the rows the legacy rendering measured. That row count is derived from the cases themselves, not from what H2 reported.
        int rowsPerConfiguration = CASES.stream().mapToInt(Case::rows).sum();
        int rowsMeasured = CONFIGS.stream().mapToInt(c -> CONTROLLED_ROWS.get(c.id())).sum();
        assertEquals(CONFIGS.size() * rowsPerConfiguration, rowsMeasured, "rows measured across the configurations");
        int comparisons = 2;
        assertEquals(List.of(), h2Problems(APPLIED_REMOVED, comparisons * rowsMeasured));
        assertEquals(4 * comparisons * rowsMeasured, APPLIED_REMOVED.values().stream().mapToInt(Integer::intValue).sum(), "total H2 uses: four borders per row and comparison");
    }

    @Test
    void theRealPageTailwindRemovedComparisonsUseH2ExactlyOncePerMeasuredElementOnTheFourPhysicalBorderStyles() {
        // One comparison per configuration, over every element of every Progress the page renders. The element count is derived
        // from the sixteen expected instances (their shapes), and checked against what the browser measured.
        int elementsPerConfiguration = EXPECTED_INSTANCES.stream().mapToInt(ProgressParityTest::realRows).sum();
        int elementsMeasured = 0;
        for (Config c : CONFIGS) {
            int measured = REAL_PROBES.get(c.id() + "|removed").phase("legacy").size();
            assertEquals(elementsPerConfiguration, measured, c.id() + ": elements measured on the real page");
            elementsMeasured += measured;
        }
        assertEquals(List.of(), h2Problems(APPLIED_REAL_REMOVED, elementsMeasured));
        assertEquals(4 * elementsMeasured, APPLIED_REAL_REMOVED.values().stream().mapToInt(Integer::intValue).sum(), "total H2 uses: four borders per element");
    }

    @Test
    void theH2GateCanFailOnAnExtraUseAnExtraPropertyAnotherRuleAndAMissingProperty() {
        Map<String, Integer> exact = new TreeMap<>();
        H2_PROPERTIES.forEach(p -> exact.put(p, 10));
        assertEquals(List.of(), h2Problems(exact, 10));
        Map<String, Integer> extraUse = new TreeMap<>(exact);
        extraUse.merge("H2 border-top-style", 1, Integer::sum);
        assertFalse(h2Problems(extraUse, 10).isEmpty(), "one extra use must be reported");
        Map<String, Integer> fewer = new TreeMap<>(exact);
        fewer.merge("H2 border-left-style", -1, Integer::sum);
        assertFalse(h2Problems(fewer, 10).isEmpty(), "one use fewer must be reported");
        Map<String, Integer> extraProperty = new TreeMap<>(exact);
        extraProperty.put("H2 border-top-width", 1);
        assertFalse(h2Problems(extraProperty, 10).isEmpty(), "H2 on any other property must be reported");
        Map<String, Integer> otherRule = new TreeMap<>(exact);
        otherRule.put("H3 vertical-align", 1);
        assertFalse(h2Problems(otherRule, 10).isEmpty(), "another rule must be reported");
        Map<String, Integer> missing = new TreeMap<>(exact);
        missing.remove("H2 border-right-style");
        assertFalse(h2Problems(missing, 10).isEmpty(), "a missing property must be reported");
        assertFalse(h2Problems(Map.of(), 10).isEmpty(), "no H2 at all must be reported too: the expectation is exact");
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
