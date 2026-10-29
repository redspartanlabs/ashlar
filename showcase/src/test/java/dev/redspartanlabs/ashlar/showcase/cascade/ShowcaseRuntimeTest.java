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
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
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
 * entry compiled by the same CLI but without Ashlar's stylesheet. Loading Ashlar's
 * stylesheet must not change any computed style.
 *
 * <p>Components that have migrated to Ashlar's CSS (today: Badge, Alert, Divider, Progress, Empty State, Icon, Spinner, Skeleton, List and List Item) are the one thing
 * the Tailwind-only stylesheet cannot render, so the baseline is their SAME-RUN LEGACY
 * RENDERING: the oracle stylesheet (Tailwind-only plus the classes they used to use)
 * with their markup rewritten back to those classes. Their elements, and only theirs,
 * are compared with {@link ParityNormalizer}; everything else is compared exactly.
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
        return List.of("button", "table", "select", "tabs", "timeline", "stepper", "breadcrumbs", "pagination");
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
        // The baseline is the SAME-RUN LEGACY RENDERING: the Tailwind-only stylesheet plus the Tailwind classes
        // migrated components used to use (so those classes still exist), with the migrated components' markup
        // rewritten back to those classes. Pages that render a migrated component are therefore still compared
        // against how that component looked before it moved to Ashlar's CSS, in the same browser run.
        byte[] baselineCss = Files.readAllBytes(Path.of(generatedDir, "pagination-oracle.css"));
        byte[] mutatedCss = (new String(baselineCss, StandardCharsets.UTF_8) + "\nbutton{padding-left:7px !important}\n").getBytes(StandardCharsets.UTF_8);
        // The oracle is cumulative (Badge, then Alert, then Divider, then Progress, then Empty State, then Icon, then Spinner, then Skeleton, then List and List Item, then Timeline and Timeline Item, then Stepper and Stepper Item, then Breadcrumbs and Breadcrumb Item, then Pagination and Pagination Item legacy classes), so the rewrite is too.
        // The Icon rewrite is LAST: the components that compose an Icon hand it a class value that still carries ash-icon.
        BadgeLegacyMarkup badgeLegacy = new BadgeLegacyMarkup();
        AlertLegacyMarkup alertLegacy = new AlertLegacyMarkup();
        DividerLegacyMarkup dividerLegacy = new DividerLegacyMarkup();
        ProgressLegacyMarkup progressLegacy = new ProgressLegacyMarkup();
        EmptyStateLegacyMarkup emptyStateLegacy = new EmptyStateLegacyMarkup();
        SpinnerLegacyMarkup spinnerLegacy = new SpinnerLegacyMarkup();
        SkeletonLegacyMarkup skeletonLegacy = new SkeletonLegacyMarkup();
        ListLegacyMarkup listLegacy = new ListLegacyMarkup();
        TimelineLegacyMarkup timelineLegacy = new TimelineLegacyMarkup();
        StepperLegacyMarkup stepperLegacy = new StepperLegacyMarkup();
        BreadcrumbsLegacyMarkup breadcrumbsLegacy = new BreadcrumbsLegacyMarkup();
        PaginationLegacyMarkup paginationLegacy = new PaginationLegacyMarkup();
        IconLegacyMarkup iconLegacy = new IconLegacyMarkup();
        Function<String, String> legacyComponents = html -> iconLegacy.rewrite(paginationLegacy.rewrite(breadcrumbsLegacy.rewrite(stepperLegacy.rewrite(timelineLegacy.rewrite(listLegacy.rewrite(skeletonLegacy.rewrite(spinnerLegacy.rewrite(
                emptyStateLegacy.rewrite(progressLegacy.rewrite(dividerLegacy.rewrite(alertLegacy.rewrite(badgeLegacy.rewrite(html)))))))))))));

        try (RunningShowcase app = RunningShowcase.start(Path.of(bootJar)); RealPageProxy proxy = new RealPageProxy(app.port())) {
            for (String slug : pageSlugs()) {
                for (Config config : configsFor(slug)) {
                    String key = slug + " | " + config.id();
                    BASELINE.put(key, load(chrome, proxy, slug, config, "snapshot", baselineCss, legacyComponents));
                    INTEGRATED.put(key, load(chrome, proxy, slug, config, "snapshot", null, null));
                }
            }
            // Controls.
            for (Config config : List.of(new Config("default-light", "light", null), new Config("axiom-light", "light", "axiom"), new Config("default-dark", "dark", null))) {
                String key = "button | " + config.id();
                BASELINE_AGAIN.put(key, load(chrome, proxy, "button", config, "snapshot", baselineCss, legacyComponents));
                MUTATED.put(key, load(chrome, proxy, "button", config, "snapshot", mutatedCss, legacyComponents));
            }
            interactive = load(chrome, proxy, "button", new Config("default-light", "light", null), "interactive", null, null);
            browser = chrome.executable() + " | " + INTEGRATED.values().iterator().next().meta().getOrDefault("user-agent", "");
        }
        writeReport();
    }

    private static Snapshot load(HeadlessChrome chrome, RealPageProxy proxy, String slug, Config config, String mode, byte[] cssOverride,
                                 Function<String, String> htmlRewrite) throws Exception {
        proxy.configure(new RealPageProxy.Load(config.theme(), mode, cssOverride, "/cascade/real-page-probe.js", "null", List.of(), Map.of(), htmlRewrite));
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
        return diff(key, a, b, null);
    }

    /**
     * As {@link #diff(String, Snapshot, Snapshot)}. Elements marked {@code ~badge} (a migrated component and
     * what is inside it) are compared with {@link ParityNormalizer}; every other element is compared exactly.
     * When {@code applied} is not null it counts each normalization that was needed.
     */
    private static List<String> diff(String key, Snapshot a, Snapshot b, Map<String, Integer> applied) {
        List<String> diffs = new ArrayList<>();
        List<String> props = List.of(a.meta().get("props").split(","));
        if (a.values().size() != b.values().size()) {
            diffs.add(key + ": element count " + a.values().size() + " vs " + b.values().size());
            return diffs;
        }
        for (int i = 0; i < a.values().size(); i++) {
            if (a.values().get(i).equals(b.values().get(i))) {
                continue;
            }
            String[] x = a.values().get(i).split("\u001f", -1);
            String[] y = b.values().get(i).split("\u001f", -1);
            if (a.descriptors().get(i).endsWith("~badge") && b.descriptors().get(i).endsWith("~badge")) {
                ParityNormalizer.Result result = ParityNormalizer.compare(
                        ParityNormalizer.side(props, x, null), ParityNormalizer.side(props, y, null));
                for (ParityNormalizer.Difference d : result.remaining()) {
                    diffs.add(key + " #" + i + " " + a.descriptors().get(i) + " " + d.property() + ": " + d.expected() + " -> " + d.actual());
                }
                if (applied != null) {
                    result.applied().forEach(rule -> applied.merge(rule.split(" ")[0], 1, Integer::sum));
                }
                continue;
            }
            for (int p = 0; p < props.size(); p++) {
                if (!x[p].equals(y[p])) {
                    diffs.add(key + " #" + i + " " + a.descriptors().get(i) + " " + props.get(p) + ": " + x[p] + " -> " + y[p]);
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
        Map<String, Integer> applied = new TreeMap<>();
        INTEGRATED.forEach((k, v) -> diffs.addAll(diff(k, BASELINE.get(k), v, applied)));
        sb.append("OBSERVED  computed-style differences baseline -> integrated: ").append(diffs.size()).append('\n');
        sb.append("NORMALIZED (Badge elements only, rule: uses): ").append(applied).append('\n');
        long badges = INTEGRATED.values().stream().mapToLong(s -> Long.parseLong(s.meta().getOrDefault("badge-count", "0"))).sum();
        sb.append("OBSERVED  Badge instances on the compared pages (summed over loads): ").append(badges).append('\n');
        diffs.stream().limit(40).forEach(d -> sb.append("   ").append(d).append('\n'));
        for (Map.Entry<String, Map<String, Snapshot>> side : Map.of("integrated", INTEGRATED, "baseline", BASELINE).entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).toList()) {
            long normal = side.getValue().values().stream().mapToLong(s -> Long.parseLong(s.meta().getOrDefault("badge-normal", "0"))).sum();
            long revealed = side.getValue().values().stream().mapToLong(s -> Long.parseLong(s.meta().getOrDefault("badge-revealed", "0"))).sum();
            long mismatches = side.getValue().values().stream().mapToLong(s -> Long.parseLong(s.meta().getOrDefault("reveal-restore-mismatch", "-1000000"))).sum();
            sb.append("OBSERVED  Badges measured as found (normal) vs revealed then restored, ").append(side.getKey()).append(" (summed over loads): ")
                    .append(normal).append(" normal, ").append(revealed).append(" revealed ; restoration mismatches: ").append(mismatches).append('\n');
        }
        Map<String, Long> revealedByPage = new TreeMap<>();
        Map<String, Long> revealedByAncestor = new TreeMap<>();
        INTEGRATED.forEach((k, s) -> {
            long n = Long.parseLong(s.meta().getOrDefault("badge-revealed", "0"));
            if (n > 0) {
                revealedByPage.merge(k.split(" ")[0], n, Long::sum);
                for (String part : s.meta().getOrDefault("badge-revealed-by", "").split(",")) {
                    if (!part.isEmpty()) {
                        revealedByAncestor.merge(part.substring(0, part.indexOf('=')), Long.parseLong(part.substring(part.indexOf('=') + 1)), Long::sum);
                    }
                }
            }
        });
        sb.append("OBSERVED  revealed Badges by page (integrated, summed over loads): ").append(revealedByPage).append('\n');
        sb.append("OBSERVED  revealed by the hidden ancestor that was un-hidden: ").append(revealedByAncestor).append('\n');
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
    void hiddenBadgesAreRevealedEquivalentlyOnBothSidesAndRestoredExactly() {
        long revealedTotal = 0;
        for (Map.Entry<String, Snapshot> e : INTEGRATED.entrySet()) {
            Snapshot base = BASELINE.get(e.getKey());
            Map<String, String> a = base.meta();
            Map<String, String> b = e.getValue().meta();
            // Same page state on both sides: the same Badges needed revealing, by the same ancestors.
            assertEquals(a.get("badge-normal"), b.get("badge-normal"), e.getKey() + ": normally rendered Badges differ between baseline and integrated");
            assertEquals(a.get("badge-revealed"), b.get("badge-revealed"), e.getKey() + ": revealed Badges differ between baseline and integrated");
            assertEquals(a.get("badge-revealed-by"), b.get("badge-revealed-by"), e.getKey() + ": the revealing ancestors differ between baseline and integrated");
            assertEquals(Integer.parseInt(b.get("badge-count")), Integer.parseInt(b.get("badge-normal")) + Integer.parseInt(b.get("badge-revealed")));
            assertEquals("0", a.get("reveal-restore-mismatch"), e.getKey() + ": a revealed element was not restored exactly (baseline)");
            assertEquals("0", b.get("reveal-restore-mismatch"), e.getKey() + ": a revealed element was not restored exactly (integrated)");
            assertFalse(b.get("badge-revealed-by").contains("STILL-NOT-RENDERED"), e.getKey() + ": a Badge was still not rendered after the reveal");
            revealedTotal += Long.parseLong(b.get("badge-revealed"));
        }
        assertTrue(revealedTotal > 0, "the paged Table page hides rows that contain Badges, so the reveal must have been needed somewhere");
    }

    @Test
    void theLegacyRewriteTookEffectWherePagesRenderBadges() {
        int pagesWithBadges = 0;
        for (Map.Entry<String, Snapshot> e : INTEGRATED.entrySet()) {
            int badges = Integer.parseInt(e.getValue().meta().get("badge-count"));
            Snapshot base = BASELINE.get(e.getKey());
            assertEquals(String.valueOf(badges), base.meta().get("badge-count"), e.getKey() + ": the baseline must render the same badges");
            assertEquals("0", e.getValue().meta().get("badge-legacy-count"), e.getKey() + ": integrated badges must carry no legacy Tailwind ring class");
            assertEquals(String.valueOf(badges), base.meta().get("badge-legacy-count"), e.getKey() + ": every baseline badge must be rendered with its legacy classes");
            if (badges > 0) {
                pagesWithBadges++;
            }
        }
        assertTrue(pagesWithBadges > 0, "the compared pages must include at least one page that renders Badges (the table page does)");
    }

    @Test
    void normalizationsAreExercisedAndOnlyTheApprovedRulesAppear() {
        Map<String, Integer> applied = new TreeMap<>();
        INTEGRATED.forEach((k, v) -> diff(k, BASELINE.get(k), v, applied));
        assertTrue(applied.getOrDefault("H1", 0) > 0, "the Badge ring should have needed H1 somewhere, or the comparison is not reaching Badges: " + applied);
        assertTrue(Set.of("H1", "H2", "H3", "H4").containsAll(applied.keySet()), "unexpected normalization rule used: " + applied);
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
