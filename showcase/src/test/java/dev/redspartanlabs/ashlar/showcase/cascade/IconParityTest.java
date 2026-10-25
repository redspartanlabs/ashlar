package dev.redspartanlabs.ashlar.showcase.cascade;

import dev.redspartanlabs.ashlar.icon.Icon;
import dev.redspartanlabs.ashlar.icon.IconSvg;
import dev.redspartanlabs.ashlar.showcase.catalog.ComponentCatalog;
import dev.redspartanlabs.ashlar.showcase.catalog.ComponentPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Icon's parity gates, measured in real Chrome, on the shared parity harness ({@link ParitySession},
 * {@link ParityFrames}, {@link ParityProbe}, {@link ParityOracle}).
 *
 * <p>Evidence is a controlled oracle: the Icon's legacy Tailwind markup (compiled by the real Tailwind CLI) next to its
 * migrated markup, in the same browser, in seven layouts (inline text, block, row and column flex, a squeezed flex row,
 * grid, an inline-flex button shape) for each configuration, with Tailwind present and with Tailwind removed. Because an
 * Icon has no colour of its own (it draws with {@code currentColor}), the configurations are the reduced four: both themes,
 * with no skin and with Foundry, the only skin whose rules reach an svg. Every Icon on the real showcase pages is
 * compared with its legacy rendering by {@code ShowcaseRuntimeTest} (its Icon rewrite is the last in the chain), and
 * the Icon markup the real pages serve is checked here. Negative controls show each gate can fail.
 *
 * <p>Evidence is Chrome only. Computed-style and box parity is the visual evidence; there is no screenshot comparison.
 */
class IconParityTest {

    private static final String ORACLE = ParityFrames.generated("icon-oracle.css");
    private static final String SCOPED = ParityFrames.generated("icon-legacy-scoped.css");
    private static final List<ParityConfigs.Config> CONFIGS = ParityConfigs.reduced();
    private static final List<String> EXTRA_PROPS = List.of("fill", "stroke", "stroke-width", "line-height");

    /** An unlayered consumer rule, present in every frame on both sides: it must win over the Icon's default size. */
    private static final String FIXTURE_CSS = ".fx-consumer{width:28px;height:28px}\n";

    /** A layout the controlled oracle puts every Icon in, between two siblings (spans), with the siblings' own style. */
    private record Layout(String name, String style, String siblingStyle) {
    }

    private static final List<Layout> LAYOUTS = List.of(
            new Layout("inline-text", "box-sizing:border-box;width:300px;line-height:20px;font-size:14px", ""),
            new Layout("block", "box-sizing:border-box;width:300px", ""),
            new Layout("row-flex", "box-sizing:border-box;display:flex;flex-direction:row;width:300px;height:60px", ""),
            new Layout("row-flex-centered", "box-sizing:border-box;display:flex;flex-direction:row;align-items:center;width:300px;height:60px", ""),
            new Layout("column-flex", "box-sizing:border-box;display:flex;flex-direction:column;width:300px", ""),
            new Layout("row-flex-squeezed", "box-sizing:border-box;display:flex;width:60px", "display:block;flex:none;width:50px"),
            new Layout("grid", "box-sizing:border-box;display:grid;width:300px;height:120px", ""),
            new Layout("button-shape", "box-sizing:border-box;display:inline-flex;align-items:center;gap:8px;padding:4px 8px;font-size:14px", ""));

    /** One Icon in the oracle. {@code legacyClasses} null means "what the legacy Icon rewrite makes of the migrated classes". */
    private record Case(String name, String migrated, String legacyClasses, String style, String attrs, boolean presentOnly) {
    }

    private static final String VALID_ATTRS = "aria-hidden=\"true\"";

    private static List<Case> cases(IconLegacyMarkup legacy) {
        return List.of(
                new Case("default", "ash-icon", null, "", VALID_ATTRS, false),
                new Case("xs", "ash-icon ash-icon--xs", null, "", VALID_ATTRS, false),
                new Case("sm", "ash-icon ash-icon--sm", null, "", VALID_ATTRS, false),
                new Case("md", "ash-icon ash-icon--md", null, "", VALID_ATTRS, false),
                new Case("lg", "ash-icon ash-icon--lg", null, "", VALID_ATTRS, false),
                new Case("xl", "ash-icon ash-icon--xl", null, "", VALID_ATTRS, false),
                new Case("size-property", "ash-icon", legacy.classes("size-10"), "--ash-icon-size:2.5rem", VALID_ATTRS, false),
                new Case("badge-icon", "ash-icon ash-badge__icon", legacy.classes("xs"), "", VALID_ATTRS, false),
                new Case("empty-state-glyph", "ash-icon ash-empty-state__glyph", legacy.classes("size-10"), "", VALID_ATTRS, false),
                new Case("consumer-rule", "ash-icon fx-consumer", legacy.classes("base") + " fx-consumer", "", VALID_ATTRS, false),
                new Case("labelled", "ash-icon ash-icon--sm", null, "", "role=\"img\" aria-label=\"Verified\"", false),
                new Case("tailwind-size", "ash-icon h-4 w-4", null, "", VALID_ATTRS, true),
                new Case("tailwind-size-colour", "ash-icon h-6 w-6 text-green-600 dark:text-green-400", null, "", VALID_ATTRS, true),
                new Case("hidden", "ash-icon", null, "", VALID_ATTRS + " hidden", true));
    }

    // ---- state of the measurement ----

    private static IconLegacyMarkup legacy;
    private static List<Case> allCases;
    private static ParityProbe.Probe probe;
    private static List<String> props;
    private static final Map<String, List<ParityProbe.Finding>> PRESENT = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> REMOVED_SCOPED = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> REMOVED_FULL = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> SCOPED_FIDELITY = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> CONTROLS = new LinkedHashMap<>();
    private static final Map<String, Integer> APPLIED_PRESENT = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_REMOVED = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_SCOPED = new TreeMap<>();
    private static final Map<String, Integer> ROWS_PER_PHASE = new LinkedHashMap<>();

    // ---- markup ----

    private static String svg(Case c, boolean legacyClasses) {
        String classes = legacyClasses ? (c.legacyClasses() != null ? c.legacyClasses() : legacy.legacyFor(c.migrated())) : c.migrated();
        return "<svg data-parity=\"icon\" data-parity-case=\"" + c.name() + "\" class=\"" + classes + "\""
                + (c.style().isEmpty() ? "" : " style=\"" + c.style() + "\"") + " " + c.attrs()
                + " viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\">"
                + IconSvg.bodyFor(Icon.CHECK) + "</svg>";
    }

    private static String markup(boolean legacyClasses, boolean removableOnly) {
        StringBuilder sb = new StringBuilder();
        for (Layout layout : LAYOUTS) {
            for (Case c : allCases) {
                if (removableOnly && c.presentOnly()) {
                    continue;
                }
                String sibling = "<span" + (layout.siblingStyle().isEmpty() ? "" : " style=\"" + layout.siblingStyle() + "\"") + ">";
                sb.append("<div style=\"margin:4px 0\"><div data-layout=\"").append(layout.name()).append("\" style=\"").append(layout.style()).append("\">")
                        .append(sibling).append("A</span>").append(svg(c, legacyClasses)).append(sibling).append("B</span></div></div>");
            }
        }
        return sb.toString();
    }

    // ---- facts: single icons whose computed size and display are checked against what the contract says ----

    private record Fact(String name, String classes, String style, String attrs, String wrapperStyle) {
    }

    private static final List<Fact> ASHLAR_FACTS = List.of(
            new Fact("default", "ash-icon", "", VALID_ATTRS, ""),
            new Fact("xs", "ash-icon ash-icon--xs", "", VALID_ATTRS, ""),
            new Fact("sm", "ash-icon ash-icon--sm", "", VALID_ATTRS, ""),
            new Fact("md", "ash-icon ash-icon--md", "", VALID_ATTRS, ""),
            new Fact("lg", "ash-icon ash-icon--lg", "", VALID_ATTRS, ""),
            new Fact("xl", "ash-icon ash-icon--xl", "", VALID_ATTRS, ""),
            new Fact("size-property", "ash-icon", "--ash-icon-size:2.5rem", VALID_ATTRS, ""),
            new Fact("property-beats-modifier", "ash-icon ash-icon--lg", "--ash-icon-size:3rem", VALID_ATTRS, ""),
            new Fact("ancestor-property-does-not-leak", "ash-icon", "", VALID_ATTRS, "--ash-icon-size:3rem"),
            new Fact("badge-icon", "ash-icon ash-badge__icon", "", VALID_ATTRS, ""),
            new Fact("empty-state-glyph", "ash-icon ash-empty-state__glyph", "", VALID_ATTRS, ""),
            new Fact("consumer-rule", "ash-icon fx-consumer", "", VALID_ATTRS, ""),
            new Fact("hidden", "ash-icon", "", VALID_ATTRS + " hidden", ""),
            new Fact("hidden-modifier", "ash-icon ash-icon--xl", "", VALID_ATTRS + " hidden", ""));

    private static final List<Fact> TAILWIND_FACTS = List.of(
            new Fact("tailwind-size", "ash-icon h-4 w-4", "", VALID_ATTRS, ""),
            new Fact("tailwind-beats-modifier", "ash-icon ash-icon--xl h-4 w-4", "", VALID_ATTRS, ""),
            new Fact("tailwind-width-only", "ash-icon w-8", "", VALID_ATTRS, ""));

    private static String factsMarkup(List<Fact> facts) {
        StringBuilder sb = new StringBuilder();
        for (Fact f : facts) {
            sb.append("<div data-layout=\"facts\"").append(f.wrapperStyle().isEmpty() ? "" : " style=\"" + f.wrapperStyle() + "\"").append(">")
                    .append(svg(new Case(f.name(), f.classes(), null, f.style(), f.attrs(), false), false))
                    .append("</div>");
        }
        return sb.toString();
    }

    // ---- control experiments: each breaks the migrated Icon in ONE way and must be detected ----

    private record Control(String name, String css, String expectedProperty) {
    }

    private static final List<Control> CONTROL_LIST = List.of(
            new Control("wrong default size", ".ash-icon{--ash-icon-size:1.5rem}", "width"),
            new Control("size ignores the property", ".ash-icon{width:auto}", "width"),
            new Control("display block", ".ash-icon{display:block}", "display"),
            new Control("flex items may shrink", ".ash-icon{flex-shrink:1}", "flex-shrink"),
            new Control("baseline alignment", ".ash-icon{vertical-align:baseline}", "vertical-align"),
            new Control("content box sizing", ".ash-icon{box-sizing:content-box}", "box-sizing"),
            new Control("wrong small size", ".ash-icon--sm{--ash-icon-size:1.25rem}", "width"),
            new Control("wrong extra large size", ".ash-icon--xl{--ash-icon-size:2.5rem}", "height"));

    // ---- running ----

    @BeforeAll
    static void measure() throws Exception {
        ParitySession session = ParitySession.open("icon-parity");
        legacy = new IconLegacyMarkup();
        allCases = cases(legacy);
        String environment = ParityFrames.environment(session.generatedDir());
        String migratedAll = markup(false, false);
        String legacyAll = markup(true, false);
        String migratedRemovable = markup(false, true);
        String legacyRemovable = markup(true, true);

        StringBuilder frames = new StringBuilder();
        for (ParityConfigs.Config c : CONFIGS) {
            frames.append(ParityFrames.frame(c.id() + "|L", c, ParityFrames.with(ORACLE, ParityConfigs.SKIN_LINKS), FIXTURE_CSS, legacyAll));
            frames.append(ParityFrames.frame(c.id() + "|P", c, ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), FIXTURE_CSS, migratedAll));
            frames.append(ParityFrames.frame(c.id() + "|LR", c, ParityFrames.with(ORACLE, ParityConfigs.SKIN_LINKS), FIXTURE_CSS, legacyRemovable));
            frames.append(ParityFrames.frame(c.id() + "|R", c, ParityFrames.with(ParityFrames.ASHLAR, ParityConfigs.SKIN_LINKS), environment + FIXTURE_CSS, migratedRemovable));
            frames.append(ParityFrames.frame(c.id() + "|S", c, ParityFrames.with(SCOPED, ParityConfigs.SKIN_LINKS), environment + FIXTURE_CSS, legacyRemovable));
        }
        ParityConfigs.Config light = ParityConfigs.get("light/none");
        for (Control control : CONTROL_LIST) {
            frames.append(ParityFrames.frame("ctl:" + control.name() + "|P", light, ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), FIXTURE_CSS + control.css(), migratedAll));
        }
        frames.append(ParityFrames.frame("facts|A", light, ParityFrames.with(ParityFrames.ASHLAR, ParityConfigs.SKIN_LINKS), environment + ".fx-consumer{width:50px;height:50px}\n", factsMarkup(ASHLAR_FACTS)));
        frames.append(ParityFrames.frame("facts|T", light, ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), "", factsMarkup(TAILWIND_FACTS)));

        probe = ParityFrames.measure(session, frames.toString(), EXTRA_PROPS, "icon frames");
        props = probe.props();
        for (ParityConfigs.Config c : CONFIGS) {
            List<ParityProbe.Row> legacyRows = probe.phase(c.id() + "|L");
            List<ParityProbe.Row> legacyRowsRemovable = probe.phase(c.id() + "|LR");
            ROWS_PER_PHASE.put(c.id() + "|L", legacyRows.size());
            ROWS_PER_PHASE.put(c.id() + "|LR", legacyRowsRemovable.size());
            PRESENT.put(c.id(), ParityProbe.compare(c.id() + " (Tailwind present)", props, legacyRows, probe.phase(c.id() + "|P"), APPLIED_PRESENT));
            REMOVED_SCOPED.put(c.id(), ParityProbe.compare(c.id() + " (Tailwind removed vs scoped legacy)", props, probe.phase(c.id() + "|S"), probe.phase(c.id() + "|R"), APPLIED_REMOVED));
            REMOVED_FULL.put(c.id(), ParityProbe.compare(c.id() + " (Tailwind removed vs full legacy)", props, legacyRowsRemovable, probe.phase(c.id() + "|R"), APPLIED_REMOVED));
            SCOPED_FIDELITY.put(c.id(), ParityProbe.compare(c.id() + " (scoped legacy vs full legacy)", props, legacyRowsRemovable, probe.phase(c.id() + "|S"), APPLIED_SCOPED));
        }
        for (Control control : CONTROL_LIST) {
            CONTROLS.put(control.name(), ParityProbe.compare("control: " + control.name(), props, probe.phase("light/none|L"), probe.phase("ctl:" + control.name() + "|P"), new TreeMap<>()));
        }
    }

    private static List<String> lines(Map<String, List<ParityProbe.Finding>> findings) {
        List<String> out = new ArrayList<>();
        findings.values().forEach(list -> list.forEach(f -> out.add(f.line())));
        return out;
    }

    private static String fact(String phase, String name, String property) {
        List<ParityProbe.Row> rows = probe.phase(phase).stream().filter(r -> r.variant().equals(name + "/facts")).toList();
        assertEquals(1, rows.size(), phase + " must measure exactly one " + name);
        return probe.value(rows.get(0), property);
    }

    // ---- tests ----

    @Test
    void theConfigurationsAreTheFourWhereAnIconCanDiffer() {
        assertEquals(List.of("light/none", "dark/none", "light/foundry", "dark/foundry"), CONFIGS.stream().map(ParityConfigs.Config::id).toList());
    }

    @Test
    void controlledOracleWithTailwindPresentMatchesTheLegacyIconInEveryConfigurationCaseAndLayout() {
        assertEquals(List.of(), lines(PRESENT));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheLegacyIconGivenTheSameSurroundings() {
        assertEquals(List.of(), lines(REMOVED_SCOPED));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheFullLegacyRendering() {
        assertEquals(List.of(), lines(REMOVED_FULL));
    }

    @Test
    void theScopedLegacyStylesheetRendersTheLegacyIconExactlyAsTheFullOneDoes() {
        assertEquals(List.of(), lines(SCOPED_FIDELITY));
    }

    @Test
    void everyNegativeControlIsDetectedInTheProperty() {
        for (Control control : CONTROL_LIST) {
            List<ParityProbe.Finding> findings = CONTROLS.get(control.name());
            assertFalse(findings.isEmpty(), "the control was NOT detected: " + control.name());
            assertTrue(findings.stream().anyMatch(f -> f.property().equals(control.expectedProperty())),
                    control.name() + " should be detected in " + control.expectedProperty() + " but was detected in "
                            + findings.stream().map(ParityProbe.Finding::property).collect(java.util.stream.Collectors.toCollection(TreeSet::new)));
        }
    }

    @Test
    void theCasesAndLayoutsReallyExerciseDifferentSizesAndTheSqueezedRowReallyTestsShrinking() {
        long casesPerFrame = allCases.size();
        assertEquals((int) (casesPerFrame * LAYOUTS.size()), (int) ROWS_PER_PHASE.get("light/none|L"));
        long removable = allCases.stream().filter(c -> !c.presentOnly()).count();
        assertEquals((int) (removable * LAYOUTS.size()), (int) ROWS_PER_PHASE.get("light/none|LR"));
        Set<String> widths = new TreeSet<>();
        for (ParityProbe.Row row : probe.phase("light/none|L")) {
            widths.add(probe.value(row, "width"));
        }
        assertTrue(widths.containsAll(Set.of("12px", "16px", "20px", "24px", "32px", "40px", "28px")), "the legacy oracle must draw every size: " + widths);
        // In the squeezed row (60px wide holding a 50px non-shrinking sibling twice), an Icon that could shrink would be squeezed to nothing.
        for (ParityProbe.Row row : probe.phase("light/none|L")) {
            if (row.variant().endsWith("/row-flex-squeezed") && row.variant().startsWith("default/")) {
                assertEquals("20px", probe.value(row, "width"), "the legacy Icon keeps its size in a squeezed row (shrink-0)");
            }
        }
    }

    @Test
    void theDefaultSizeIsTwentyPixelsAndEveryModifierAndThePropertyGiveTheirApprovedSizeWithTailwindRemoved() {
        for (Map.Entry<String, String> e : Map.of("default", "20px", "xs", "12px", "sm", "16px", "md", "20px", "lg", "24px", "xl", "32px",
                "size-property", "40px", "property-beats-modifier", "48px").entrySet()) {
            assertEquals(e.getValue(), fact("facts|A", e.getKey(), "width"), e.getKey() + " width");
            assertEquals(e.getValue(), fact("facts|A", e.getKey(), "height"), e.getKey() + " height");
        }
    }

    @Test
    void theBaseRuleStatesTheInlineBehaviourPreflightUsedToGiveEveryIconWithoutTailwind() {
        assertEquals("inline-block", fact("facts|A", "default", "display"));
        assertEquals("0", fact("facts|A", "default", "flex-shrink"));
        assertEquals("middle", fact("facts|A", "default", "vertical-align"));
        assertEquals("border-box", fact("facts|A", "default", "box-sizing"));
        assertEquals(fact("facts|A", "default", "color"), fact("facts|A", "default", "stroke"), "the icon draws in the surrounding text colour (currentColor) and states no colour of its own");
    }

    @Test
    void aPropertyOnAnAncestorIsNotInheritedByTheIcon() {
        assertEquals("20px", fact("facts|A", "ancestor-property-does-not-leak", "width"));
    }

    @Test
    void aComponentsOwnClassAndAnUnlayeredConsumerRuleBeatTheDefaultSize() {
        assertEquals("12px", fact("facts|A", "badge-icon", "width"));
        assertEquals("40px", fact("facts|A", "empty-state-glyph", "width"));
        assertEquals("50px", fact("facts|A", "consumer-rule", "width"));
        assertEquals("50px", fact("facts|A", "consumer-rule", "height"));
    }

    @Test
    void theHiddenAttributeHidesTheIconEvenThoughItsBaseRuleSetsDisplay() {
        assertEquals("none", fact("facts|A", "hidden", "display"));
        assertEquals("none", fact("facts|A", "hidden-modifier", "display"));
        assertEquals("0", fact("facts|A", "hidden", "@w"));
    }

    @Test
    void tailwindSizeClassesStillWinOverTheDefaultAndTheModifiersWhileTailwindIsPresent() {
        assertEquals("16px", fact("facts|T", "tailwind-size", "width"));
        assertEquals("16px", fact("facts|T", "tailwind-size", "height"));
        assertEquals("16px", fact("facts|T", "tailwind-beats-modifier", "width"));
        assertEquals("32px", fact("facts|T", "tailwind-width-only", "width"));
        assertEquals("20px", fact("facts|T", "tailwind-width-only", "height"), "a class with only a width leaves the height at Ashlar's default");
    }

    @Test
    void theOracleIsNotVacuousEveryLegacyClassIsCompiledInTheCumulativeAndScopedStylesheets() throws IOException {
        Path dir = Path.of(System.getProperty("ashlar.cascade.dir"));
        String oracle = Files.readString(dir.resolve("icon-oracle.css"), StandardCharsets.UTF_8);
        String scoped = Files.readString(dir.resolve("icon-legacy-scoped.css"), StandardCharsets.UTF_8);
        assertEquals(List.of(), ParityOracle.uncompiled(oracle, legacy.allTokens()), "the Icon oracle does not compile these");
        // The scoped stylesheet reads no source but icon-legacy.html, so this also proves its @source path resolves.
        assertEquals(List.of(), ParityOracle.uncompiled(scoped, legacy.allTokens()), "the scoped stylesheet does not compile these");
        // Cumulative, in order Badge -> Alert -> Divider -> Progress -> Empty State -> Icon: the earlier legacy classes are still in the oracle.
        List<String> earlier = new ArrayList<>();
        new BadgeLegacyMarkup().variants().values().forEach(classes -> earlier.addAll(Arrays.asList(classes.split("\\s+"))));
        earlier.addAll(new AlertLegacyMarkup().allTokens());
        earlier.addAll(new DividerLegacyMarkup().allTokens());
        earlier.addAll(new ProgressLegacyMarkup().allTokens());
        earlier.addAll(new EmptyStateLegacyMarkup().allTokens());
        assertEquals(List.of(), ParityOracle.uncompiled(oracle, earlier), "the Icon oracle lost earlier legacy classes");
        assertFalse(scoped.contains("html,:host"), "the scoped legacy stylesheet must not carry Tailwind's page-wide base");
    }

    @Test
    void normalizationsStayWithinTheApprovedRulesAndTailwindPresentNeedsNone() {
        assertEquals(Map.of(), APPLIED_PRESENT, "Tailwind present needs no normalization at all");
        assertEquals(Map.of(), APPLIED_SCOPED, "the scoped legacy rendering must match the full one without any normalization");
        // Tailwind removed: only H2 (a border style is invisible at width 0; preflight gave solid, the browser gives none), on the four
        // border-style properties of every measured Icon, in both removed comparisons.
        long rows = 0;
        for (ParityConfigs.Config c : CONFIGS) {
            rows += probe.phase(c.id() + "|R").size();
        }
        assertEquals(Map.of("H2", (int) (2 * 4 * rows)), APPLIED_REMOVED, "exactly H2, four properties per Icon, two comparisons");
    }

    // ---- what the real pages serve ----

    private static final Pattern SVG_TAG = Pattern.compile("<svg\\b([^>]*)>");
    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("\\bclass=\"([^\"]*)\"");

    @Test
    void everyIconTheRealPagesServeStartsWithAshIconAndKeepsItsAccessibilitySemantics() throws Exception {
        ParitySession session = ParitySession.open("icon-parity");
        HttpClient client = HttpClient.newHttpClient();
        List<String> paths = new ArrayList<>(List.of("/ashlar/", "/ashlar/components"));
        for (ComponentPage page : ComponentCatalog.PAGES) {
            paths.add("/ashlar/components/" + page.slug());
        }
        int icons = 0;
        int labelled = 0;
        int decorative = 0;
        List<String> problems = new ArrayList<>();
        try (RunningShowcase app = RunningShowcase.start(session.bootJar())) {
            for (String path : paths) {
                HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + path)).GET().build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                assertEquals(200, response.statusCode(), path);
                Matcher m = SVG_TAG.matcher(response.body());
                while (m.find()) {
                    String attrs = m.group(1);
                    if (!attrs.contains("viewBox=\"0 0 24 24\"") || !attrs.contains("stroke-width=\"1.5\"")) {
                        continue; // not an Icon component (some other inline svg)
                    }
                    icons++;
                    Matcher c = CLASS_ATTRIBUTE.matcher(attrs);
                    List<String> classes = c.find() ? Arrays.asList(c.group(1).trim().split("\\s+")) : List.of();
                    // The template writes ash-icon FIRST and then the caller's classes (a caller may itself pass shrink-0 or inline-block, as the
                    // date picker's trigger icon does), so the Icon's own output is exactly: ash-icon is the first class, and nothing precedes it.
                    if (classes.isEmpty() || !classes.get(0).equals("ash-icon")) {
                        problems.add(path + ": an Icon whose first class is not ash-icon: " + attrs.replaceAll("\\s+", " "));
                    }
                    boolean hasLabel = attrs.contains("aria-label=");
                    if (hasLabel) {
                        labelled++;
                        if (!attrs.contains("role=\"img\"") || attrs.contains("aria-hidden")) {
                            problems.add(path + ": a labelled Icon must have role=img and no aria-hidden");
                        }
                    } else {
                        decorative++;
                        if (!attrs.contains("aria-hidden=\"true\"") || attrs.contains("role=")) {
                            problems.add(path + ": a decorative Icon must be aria-hidden and have no role");
                        }
                    }
                }
                assertEquals(IconLegacyMarkup.count(response.body()), countAshIconSvgs(response.body()), path + ": every ash-icon class must be on an Icon svg");
            }
        }
        assertEquals(List.of(), problems);
        assertTrue(icons > 40, "the real pages render many Icons, found " + icons);
        assertTrue(labelled >= 1 && decorative >= 1, "both kinds are covered: labelled=" + labelled + " decorative=" + decorative);
    }

    private static int countAshIconSvgs(String html) {
        Matcher m = SVG_TAG.matcher(html);
        int n = 0;
        while (m.find()) {
            Matcher c = CLASS_ATTRIBUTE.matcher(m.group(1));
            if (c.find() && Arrays.asList(c.group(1).trim().split("\\s+")).contains("ash-icon")) {
                n++;
            }
        }
        return n;
    }
}
