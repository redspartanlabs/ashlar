package dev.redspartanlabs.ashlar.showcase.cascade;

import dev.redspartanlabs.ashlar.showcase.catalog.ComponentCatalog;
import dev.redspartanlabs.ashlar.showcase.catalog.ComponentPage;
import dev.redspartanlabs.ashlar.skeleton.SkeletonShape;
import dev.redspartanlabs.ashlar.skeleton.SkeletonStyles;
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
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Skeleton's parity gates, measured in real Chrome, on the shared parity harness ({@link ParitySession},
 * {@link ParityConfigs}, {@link ParityFrames}, {@link ParityProbe}, {@link ParityOracle}).
 *
 * <p>Evidence is a controlled oracle: the Skeleton's legacy Tailwind markup (compiled by the real Tailwind CLI) next to its migrated
 * markup, in the same browser, in eight layouts for each of the 22 theme and skin configurations, with Tailwind present and with
 * Tailwind removed. The placeholder pulses forever, so every frame is paused at the same instant (the same negative animation delay on
 * both sides): the colour, the geometry and the opacity at that instant are compared exactly, and further instants (including the
 * exact middle of the pulse) check the keyframes. The animation's duration, easing, count and direction are compared as computed
 * properties; its NAME is not (Tailwind's is "pulse", Ashlar's is "ash-skeleton-pulse"), so the name is asserted separately. A second
 * run, with Chrome told the user prefers reduced motion, checks that the animation is gone on both sides. Every Skeleton on the real
 * showcase pages is compared with its legacy rendering by {@code ShowcaseRuntimeTest}, and the Skeleton markup the real pages serve is
 * checked here. Negative controls show each gate can fail, including one for the skin-driven radius token the Skeleton must NOT use.
 *
 * <p>Evidence is Chrome only. Computed-style and box parity is the visual evidence; there is no screenshot comparison.
 */
class SkeletonParityTest {

    private static final String ORACLE = ParityFrames.generated("skeleton-oracle.css");
    private static final String SCOPED = ParityFrames.generated("skeleton-legacy-scoped.css");
    private static final List<ParityConfigs.Config> CONFIGS = ParityConfigs.all();
    private static final List<ParityConfigs.Config> REDUCED_CONFIGS = ParityConfigs.reduced();
    private static final List<String> MAIN_PROPS = List.of("animation-duration", "animation-timing-function", "animation-iteration-count", "animation-direction", "animation-fill-mode");
    private static final List<String> REDUCED_PROPS = concat(MAIN_PROPS, List.of("animation-name"));
    private static final List<String> FACT_PROPS = List.of("animation-name", "animation-play-state", "animation-duration", "animation-timing-function", "animation-iteration-count");

    /** Pauses every animation at the same instant on both sides of a comparison: progress is then a fixed fraction, not a race. */
    private static String pausedAt(String delay) {
        return "*{animation-play-state:paused !important;animation-delay:" + delay + " !important}\n";
    }

    /** The instant of the main matrix: a quarter second into a two second pulse, which is off every keyframe so the easing matters. */
    private static final String MAIN_PHASE = "-0.25s";
    /** Further instants, measured in the plain configuration: exactly the middle of the pulse (half opacity) and a rising point. */
    private static final List<String> EXTRA_PHASES = List.of("-1s", "-1.5s");

    private static List<String> concat(List<String> a, List<String> b) {
        List<String> all = new ArrayList<>(a);
        all.addAll(b);
        return List.copyOf(all);
    }

    // ---- layouts and cases ----

    private record Layout(String name, String style, String siblingStyle) {
    }

    private static final List<Layout> LAYOUTS = List.of(
            new Layout("block", "box-sizing:border-box;width:300px", ""),
            new Layout("inline-text", "box-sizing:border-box;width:300px;line-height:20px;font-size:14px", "font-size:14px"),
            new Layout("row-flex", "box-sizing:border-box;display:flex;flex-direction:row;align-items:center;gap:12px;width:300px", ""),
            new Layout("column-flex", "box-sizing:border-box;display:flex;flex-direction:column;width:300px", ""),
            new Layout("column-flex-centered", "box-sizing:border-box;display:flex;flex-direction:column;align-items:center;width:300px", ""),
            new Layout("card", "box-sizing:border-box;display:flex;flex-direction:column;gap:12px;width:300px;padding:16px", ""),
            new Layout("row-flex-squeezed", "box-sizing:border-box;display:flex;width:60px", "display:block;flex:none;width:50px"),
            new Layout("grid", "box-sizing:border-box;display:grid;width:300px;height:200px", ""));

    private record Case(String shape, String width, String height) {
        String name() {
            return shape + "-" + width.replace("%", "pct") + "x" + height;
        }
    }

    private static final List<Case> CASES = List.of(
            new Case("rectangle", "100%", "1rem"),
            new Case("rectangle", "60%", "0.875rem"),
            new Case("rectangle", "8rem", "2rem"),
            new Case("rectangle", "3rem", "3rem"),
            new Case("rounded", "3rem", "3rem"),
            new Case("rounded", "8rem", "2rem"),
            new Case("rounded", "2.5rem", "2.5rem"),
            new Case("rounded", "100%", "1rem"));

    // ---- state of the measurement ----

    private static SkeletonLegacyMarkup legacy;
    private static ParityProbe.Probe main;
    private static ParityProbe.Probe animation;
    private static ParityProbe.Probe reduced;
    private static final Map<String, List<ParityProbe.Finding>> PRESENT = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> REMOVED_SCOPED = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> REMOVED_FULL = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> SCOPED_FIDELITY = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> PHASES = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> CONTROLS = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> REDUCED_FINDINGS = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> REDUCED_CONTROLS = new LinkedHashMap<>();
    private static final Map<String, Integer> APPLIED_PRESENT = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_REMOVED = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_SCOPED = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_REDUCED = new TreeMap<>();
    private static final Map<String, Integer> ROWS = new LinkedHashMap<>();

    // ---- markup ----

    private static String skeleton(Case c, boolean legacyClasses, String extraAttributes) {
        String classes = legacyClasses ? legacy.classes(c.shape()) : SkeletonStyles.classes(SkeletonShape.valueOf(c.shape().toUpperCase(Locale.ROOT)));
        return "<div aria-hidden=\"true\" class=\"" + classes + "\" style=\"width: " + c.width() + "; height: " + c.height() + "\" data-skeleton data-skeleton-shape=\"" + c.shape() + "\""
                + extraAttributes + " data-parity=\"skeleton\" data-parity-case=\"" + c.name() + "\"></div>";
    }

    private static String markup(boolean legacyClasses) {
        StringBuilder sb = new StringBuilder();
        for (Layout layout : LAYOUTS) {
            for (Case c : CASES) {
                String sibling = "<span" + (layout.siblingStyle().isEmpty() ? "" : " style=\"" + layout.siblingStyle() + "\"") + ">";
                sb.append("<div style=\"margin:4px 0\"><div data-layout=\"").append(layout.name()).append("\" style=\"").append(layout.style()).append("\">")
                        .append(sibling).append("A</span>").append(skeleton(c, legacyClasses, "")).append(sibling).append("B</span></div></div>");
            }
        }
        return sb.toString();
    }

    // ---- facts: single skeletons whose computed values are checked against what the contract says ----

    private record Fact(String name, Case shape, String classes, String style) {
    }

    private static final List<Fact> FACTS = List.of(
            new Fact("rectangle", new Case("rectangle", "10rem", "2rem"), "ash-skeleton", ""),
            new Fact("rounded", new Case("rounded", "10rem", "2rem"), "ash-skeleton ash-skeleton--rounded", ""),
            new Fact("circle", new Case("rounded", "3rem", "3rem"), "ash-skeleton ash-skeleton--rounded", ""),
            new Fact("bg-property", new Case("rectangle", "10rem", "2rem"), "ash-skeleton", "--ash-skeleton-bg:rgb(1, 2, 3)"),
            new Fact("consumer-rule", new Case("rectangle", "10rem", "2rem"), "ash-skeleton fx-skeleton", ""));

    private static final String FACT_CSS = ".fx-skeleton{--ash-skeleton-bg:rgb(7, 8, 9)}\n";

    private static String factsMarkup() {
        StringBuilder sb = new StringBuilder();
        for (Fact f : FACTS) {
            sb.append("<div data-layout=\"facts\"><div aria-hidden=\"true\" class=\"").append(f.classes()).append("\" style=\"width: ").append(f.shape().width())
                    .append("; height: ").append(f.shape().height()).append(f.style().isEmpty() ? "" : ";" + f.style()).append("\" data-skeleton data-skeleton-shape=\"")
                    .append(f.shape().shape()).append("\" data-parity=\"skeleton\" data-parity-case=\"").append(f.name()).append("\"></div></div>");
        }
        return sb.toString();
    }

    private static String legacyFacts() {
        StringBuilder sb = new StringBuilder();
        for (Fact f : FACTS) {
            if (f.style().isEmpty() && f.classes().equals(SkeletonStyles.classes(SkeletonShape.valueOf(f.shape().shape().toUpperCase(Locale.ROOT))))) {
                sb.append("<div data-layout=\"facts\"><div aria-hidden=\"true\" class=\"").append(legacy.classes(f.shape().shape())).append("\" style=\"width: ").append(f.shape().width())
                        .append("; height: ").append(f.shape().height()).append("\" data-parity=\"skeleton\" data-parity-case=\"").append(f.name()).append("\"></div></div>");
            }
        }
        return sb.toString();
    }

    // ---- control experiments: each breaks the migrated Skeleton in ONE way and must be detected ----

    private record Control(String name, String config, String css, String expectedProperty) {
    }

    private static final List<Control> CONTROL_LIST = List.of(
            new Control("wrong colour", "light/none", ".ash-skeleton{--ash-skeleton-bg:red}", "background-color"),
            new Control("dark colour ignores the theme", "dark/none", ".ash-skeleton{--ash-skeleton-bg:var(--color-slate-200)}", "background-color"),
            new Control("colour ignores the skin", "light/civic", ".ash-skeleton{--ash-skeleton-bg:#e2e8f0}", "background-color"),
            new Control("radius follows the skin", "light/axiom", ".ash-skeleton{border-radius:var(--ash-radius-sm)}", "border-top-left-radius"),
            new Control("wrong rectangle radius", "light/none", ".ash-skeleton{border-radius:0.5rem}", "border-top-left-radius"),
            new Control("rounded is not a pill", "light/none", ".ash-skeleton--rounded{border-radius:2px}", "border-top-left-radius"),
            new Control("displayed inline-block", "light/none", ".ash-skeleton{display:inline-block}", "display"),
            new Control("may shrink", "light/none", ".ash-skeleton{flex-shrink:1}", "flex-shrink"),
            new Control("content box sizing", "light/none", ".ash-skeleton{box-sizing:content-box}", "box-sizing"),
            new Control("wrong duration", "light/none", ".ash-skeleton{animation-duration:1s}", "animation-duration"),
            new Control("wrong easing", "light/none", ".ash-skeleton{animation-timing-function:ease}", "animation-timing-function"),
            new Control("pulse is shallower", "light/none", "@keyframes ash-skeleton-pulse{50%{opacity:.8}}", "opacity"));

    private static final String REDUCED_CONTROL_CSS = "@media (prefers-reduced-motion: reduce){.ash-skeleton{animation:ash-skeleton-pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite}}";

    // ---- running ----

    @BeforeAll
    static void measure() throws Exception {
        ParitySession session = ParitySession.open("skeleton-parity");
        legacy = new SkeletonLegacyMarkup();
        String environment = ParityFrames.environment(session.generatedDir());
        String migrated = markup(false);
        String legacyMarkup = markup(true);

        // Run 1: the whole matrix, every animation paused at one instant.
        StringBuilder frames = new StringBuilder();
        for (ParityConfigs.Config c : CONFIGS) {
            addMatrix(frames, c.id(), c, environment, migrated, legacyMarkup, MAIN_PHASE);
        }
        ParityConfigs.Config light = ParityConfigs.get("light/none");
        for (String phase : EXTRA_PHASES) {
            addMatrix(frames, "phase" + phase, light, environment, migrated, legacyMarkup, phase);
        }
        for (Control control : CONTROL_LIST) {
            frames.append(ParityFrames.frame("ctl:" + control.name() + "|P", ParityConfigs.get(control.config()),
                    ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), pausedAt(MAIN_PHASE) + control.css(), migrated));
        }
        frames.append(ParityFrames.frame("facts|A", light, ParityFrames.with(ParityFrames.ASHLAR, ParityConfigs.SKIN_LINKS), environment + pausedAt("-1s") + FACT_CSS, factsMarkup()));
        frames.append(ParityFrames.frame("facts|D", ParityConfigs.get("dark/none"), ParityFrames.with(ParityFrames.ASHLAR, ParityConfigs.SKIN_LINKS), environment + pausedAt("-1s") + FACT_CSS, factsMarkup()));
        frames.append(ParityFrames.frame("facts|S", ParityConfigs.get("light/axiom"), ParityFrames.with(ParityFrames.ASHLAR, ParityConfigs.SKIN_LINKS), environment + pausedAt("-1s") + FACT_CSS, factsMarkup()));
        main = ParityFrames.measure(session, frames.toString(), MAIN_PROPS, "skeleton frames");
        List<String> props = main.props();
        for (ParityConfigs.Config c : CONFIGS) {
            compareMatrix(c.id(), props, main, PRESENT, REMOVED_SCOPED, REMOVED_FULL, SCOPED_FIDELITY, APPLIED_PRESENT, APPLIED_REMOVED, APPLIED_SCOPED);
        }
        Map<String, Integer> phaseApplied = new TreeMap<>();
        for (String phase : EXTRA_PHASES) {
            compareMatrix("phase" + phase, props, main, PHASES, PHASES, PHASES, PHASES, phaseApplied, phaseApplied, phaseApplied);
        }
        for (Control control : CONTROL_LIST) {
            CONTROLS.put(control.name(), ParityProbe.compare("control: " + control.name(), props, main.phase(control.config() + "|L"),
                    main.phase("ctl:" + control.name() + "|P"), new TreeMap<>()));
        }

        // Run 2: nothing paused, so the animation's own name and state can be read.
        StringBuilder running = new StringBuilder();
        running.append(ParityFrames.frame("anim|A", light, ParityFrames.with(ParityFrames.ASHLAR, ParityConfigs.SKIN_LINKS), environment + FACT_CSS, factsMarkup()));
        running.append(ParityFrames.frame("anim|L", light, ParityFrames.with(ORACLE, ParityConfigs.SKIN_LINKS), "", legacyFacts()));
        animation = ParityFrames.measure(session, running.toString(), FACT_PROPS, "skeleton animation");

        // Run 3: Chrome is told the user prefers reduced motion.
        StringBuilder calm = new StringBuilder();
        for (ParityConfigs.Config c : REDUCED_CONFIGS) {
            addMatrix(calm, c.id(), c, environment, migrated, legacyMarkup, MAIN_PHASE);
        }
        calm.append(ParityFrames.frame("ctl:reduced|P", light, ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), pausedAt(MAIN_PHASE) + REDUCED_CONTROL_CSS, migrated));
        reduced = ParityFrames.measure(session, calm.toString(), REDUCED_PROPS, List.of("--force-prefers-reduced-motion"), "skeleton reduced motion");
        for (ParityConfigs.Config c : REDUCED_CONFIGS) {
            REDUCED_FINDINGS.put(c.id() + " present", ParityProbe.compare(c.id() + " (reduced motion, Tailwind present)", reduced.props(), reduced.phase(c.id() + "|L"), reduced.phase(c.id() + "|P"), APPLIED_REDUCED));
            REDUCED_FINDINGS.put(c.id() + " removed", ParityProbe.compare(c.id() + " (reduced motion, Tailwind removed)", reduced.props(), reduced.phase(c.id() + "|S"), reduced.phase(c.id() + "|R"), APPLIED_REDUCED));
        }
        REDUCED_CONTROLS.put("reduced-motion rule removed", ParityProbe.compare("control: reduced-motion rule removed", reduced.props(), reduced.phase("light/none|L"), reduced.phase("ctl:reduced|P"), new TreeMap<>()));
    }

    private static void addMatrix(StringBuilder frames, String id, ParityConfigs.Config c, String environment, String migrated, String legacyMarkup, String phase) {
        String paused = pausedAt(phase);
        frames.append(ParityFrames.frame(id + "|L", c, ParityFrames.with(ORACLE, ParityConfigs.SKIN_LINKS), paused, legacyMarkup));
        frames.append(ParityFrames.frame(id + "|P", c, ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), paused, migrated));
        frames.append(ParityFrames.frame(id + "|R", c, ParityFrames.with(ParityFrames.ASHLAR, ParityConfigs.SKIN_LINKS), environment + paused, migrated));
        frames.append(ParityFrames.frame(id + "|S", c, ParityFrames.with(SCOPED, ParityConfigs.SKIN_LINKS), environment + paused, legacyMarkup));
    }

    private static void compareMatrix(String id, List<String> props, ParityProbe.Probe probe,
                                      Map<String, List<ParityProbe.Finding>> present, Map<String, List<ParityProbe.Finding>> removedScoped,
                                      Map<String, List<ParityProbe.Finding>> removedFull, Map<String, List<ParityProbe.Finding>> fidelity,
                                      Map<String, Integer> appliedPresent, Map<String, Integer> appliedRemoved, Map<String, Integer> appliedScoped) {
        List<ParityProbe.Row> legacyRows = probe.phase(id + "|L");
        ROWS.put(id + "|L", legacyRows.size());
        present.put(id + " present", ParityProbe.compare(id + " (Tailwind present)", props, legacyRows, probe.phase(id + "|P"), appliedPresent));
        removedScoped.put(id + " removed-scoped", ParityProbe.compare(id + " (Tailwind removed vs scoped legacy)", props, probe.phase(id + "|S"), probe.phase(id + "|R"), appliedRemoved));
        removedFull.put(id + " removed-full", ParityProbe.compare(id + " (Tailwind removed vs full legacy)", props, legacyRows, probe.phase(id + "|R"), appliedRemoved));
        fidelity.put(id + " fidelity", ParityProbe.compare(id + " (scoped legacy vs full legacy)", props, legacyRows, probe.phase(id + "|S"), appliedScoped));
    }

    private static List<String> lines(Map<String, List<ParityProbe.Finding>> findings) {
        List<String> out = new ArrayList<>();
        findings.values().forEach(list -> list.forEach(f -> out.add(f.line())));
        return out;
    }

    private static String fact(ParityProbe.Probe probe, String phase, String name, String property) {
        List<ParityProbe.Row> rows = probe.phase(phase).stream().filter(r -> r.variant().equals(name + "/facts")).toList();
        assertEquals(1, rows.size(), phase + " must measure exactly one " + name);
        return probe.value(rows.get(0), property);
    }

    // ---- tests ----

    @Test
    void theConfigurationsAreTheTwentyTwoThemeAndSkinCombinations() {
        assertEquals(22, CONFIGS.size());
        assertEquals(4, REDUCED_CONFIGS.size());
    }

    @Test
    void controlledOracleWithTailwindPresentMatchesTheLegacySkeletonInEveryConfigurationCaseAndLayout() {
        assertEquals(List.of(), lines(PRESENT));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheLegacySkeletonGivenTheSameSurroundings() {
        assertEquals(List.of(), lines(REMOVED_SCOPED));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheFullLegacyRendering() {
        assertEquals(List.of(), lines(REMOVED_FULL));
    }

    @Test
    void theScopedLegacyStylesheetRendersTheLegacySkeletonExactlyAsTheFullOneDoes() {
        assertEquals(List.of(), lines(SCOPED_FIDELITY));
    }

    @Test
    void theKeyframesMatchTheLegacyPulseAtFurtherInstants() {
        assertEquals(List.of(), lines(PHASES));
        assertEquals(EXTRA_PHASES.size() * 4, PHASES.size());
    }

    @Test
    void everyNegativeControlIsDetectedInTheProperty() {
        for (Control control : CONTROL_LIST) {
            List<ParityProbe.Finding> findings = CONTROLS.get(control.name());
            assertFalse(findings.isEmpty(), "the control was NOT detected: " + control.name());
            assertTrue(findings.stream().anyMatch(f -> f.property().equals(control.expectedProperty())),
                    control.name() + " should be detected in " + control.expectedProperty() + " but was detected in "
                            + findings.stream().map(ParityProbe.Finding::property).collect(Collectors.toCollection(TreeSet::new)));
        }
    }

    @Test
    void theCasesAndLayoutsAreCompleteAndTheAnimationIsReallyMeasuredNotAbsent() {
        assertEquals(LAYOUTS.size() * CASES.size(), ROWS.get("light/none|L"));
        assertEquals(LAYOUTS.size() * CASES.size(), ROWS.get("dark/civic|L"));
        // A paused pulse is part way down: an animation that did not apply would leave every placeholder at full opacity.
        for (ParityProbe.Row row : main.phase("light/none|L")) {
            double opacity = Double.parseDouble(main.value(row, "opacity"));
            assertTrue(opacity < 1 && opacity >= 0.5, "the legacy placeholder is mid-pulse: " + opacity);
        }
        for (ParityProbe.Row row : main.phase("phase-1s|L")) {
            assertEquals("0.5", main.value(row, "opacity"), "the exact middle of the pulse is half opacity");
        }
        for (ParityProbe.Row row : main.phase("phase-1.5s|L")) {
            double opacity = Double.parseDouble(main.value(row, "opacity"));
            assertTrue(opacity > 0.5 && opacity < 1, "on the way back up: " + opacity);
        }
    }

    @Test
    void theRadiusIsAQuarterRemForARectangleAndAPillForRoundedInEveryConfigurationSoNoSkinChangesIt() {
        // The 22 comparisons above already prove it equals the legacy radius under every skin; this states the values.
        assertEquals("4px", fact(main, "facts|A", "rectangle", "border-top-left-radius"));
        assertEquals("4px", fact(main, "facts|S", "rectangle", "border-top-left-radius"), "Axiom sets --radius-sm to 2px; the Skeleton must not follow it");
        for (String phase : List.of("facts|A", "facts|D", "facts|S")) {
            for (String name : List.of("rounded", "circle")) {
                assertTrue(ParityNormalizer.px(fact(main, phase, name, "border-top-left-radius")) > 1000, phase + " " + name + " is fully rounded");
            }
        }
    }

    @Test
    void theBaseRuleStatesTheDisplayShrinkAndSizingPreflightAndTheUtilitiesGave() {
        assertEquals("block", fact(main, "facts|A", "rectangle", "display"));
        assertEquals("0", fact(main, "facts|A", "rectangle", "flex-shrink"));
        assertEquals("border-box", fact(main, "facts|A", "rectangle", "box-sizing"));
        assertEquals("160px", fact(main, "facts|A", "rectangle", "width"), "the inline style gives the size");
        assertEquals("32px", fact(main, "facts|A", "rectangle", "height"));
        assertEquals("48px", fact(main, "facts|A", "circle", "width"));
        assertEquals("48px", fact(main, "facts|A", "circle", "height"));
    }

    @Test
    void theColourIsTheSlatePlaceholderInBothThemesAndTheCustomisationPropertyWorksWhereItIsDocumented() {
        assertNotEquals(fact(main, "facts|A", "rectangle", "background-color"), fact(main, "facts|D", "rectangle", "background-color"), "the dark placeholder differs");
        assertEquals("rgb(1, 2, 3)", fact(main, "facts|A", "bg-property", "background-color"));
        assertEquals("rgb(7, 8, 9)", fact(main, "facts|A", "consumer-rule", "background-color"));
    }

    @Test
    void theAnimationIsAshlarsOwnAndPulsesTwoSecondsForeverWhileTheLegacyOneIsTailwindsPulse() {
        for (String name : List.of("rectangle", "rounded")) {
            assertEquals("ash-skeleton-pulse", fact(animation, "anim|A", name, "animation-name"), name);
            assertEquals("running", fact(animation, "anim|A", name, "animation-play-state"));
            assertEquals("2s", fact(animation, "anim|A", name, "animation-duration"));
            assertEquals("cubic-bezier(0.4, 0, 0.6, 1)", fact(animation, "anim|A", name, "animation-timing-function"));
            assertEquals("infinite", fact(animation, "anim|A", name, "animation-iteration-count"));
            assertEquals("pulse", fact(animation, "anim|L", name, "animation-name"), "the legacy oracle really animates with Tailwind's pulse");
            assertEquals("running", fact(animation, "anim|L", name, "animation-play-state"));
        }
    }

    @Test
    void withReducedMotionRequestedTheAnimationIsGoneOnBothSidesAndEverythingElseStillMatches() {
        assertEquals(List.of(), lines(REDUCED_FINDINGS));
        long rows = 0;
        for (ParityConfigs.Config c : REDUCED_CONFIGS) {
            rows += reduced.phase(c.id() + "|R").size();
        }
        assertEquals(Map.of("H2", (int) (4 * rows)), APPLIED_REDUCED, "only the invisible border-style rule, four properties per placeholder, one removed comparison");
        for (ParityProbe.Row row : reduced.rows()) {
            if (!row.phase().startsWith("ctl:")) {
                assertEquals("none", reduced.value(row, "animation-name"), row.phase() + " " + row.variant());
                assertEquals("1", reduced.value(row, "opacity"), "a still placeholder is at full opacity");
            }
        }
        // It is still drawn: same colour surface, same size.
        assertTrue(reduced.phase("light/none|R").stream().allMatch(r -> !reduced.value(r, "width").equals("0px") && !reduced.value(r, "background-color").equals("rgba(0, 0, 0, 0)")));
    }

    @Test
    void theReducedMotionControlIsDetected() {
        List<ParityProbe.Finding> findings = REDUCED_CONTROLS.get("reduced-motion rule removed");
        assertTrue(findings.stream().anyMatch(f -> f.property().equals("animation-name")), "a placeholder that keeps pulsing must be detected in animation-name: " + findings.size());
    }

    @Test
    void theOracleIsNotVacuousEveryLegacyClassIsCompiledInTheCumulativeAndScopedStylesheets() throws IOException {
        Path dir = Path.of(System.getProperty("ashlar.cascade.dir"));
        String oracle = Files.readString(dir.resolve("skeleton-oracle.css"), StandardCharsets.UTF_8);
        String scoped = Files.readString(dir.resolve("skeleton-legacy-scoped.css"), StandardCharsets.UTF_8);
        assertEquals(List.of(), ParityOracle.uncompiled(oracle, legacy.allTokens()), "the Skeleton oracle does not compile these");
        // The scoped stylesheet reads no source but skeleton-legacy.html, so this also proves its @source path resolves.
        assertEquals(List.of(), ParityOracle.uncompiled(scoped, legacy.allTokens()), "the scoped stylesheet does not compile these");
        // Cumulative, in order Badge -> Alert -> Divider -> Progress -> Empty State -> Icon -> Spinner -> Skeleton: the earlier legacy classes are still in the oracle.
        List<String> earlier = new ArrayList<>();
        new BadgeLegacyMarkup().variants().values().forEach(classes -> earlier.addAll(Arrays.asList(classes.split("\\s+"))));
        earlier.addAll(new AlertLegacyMarkup().allTokens());
        earlier.addAll(new DividerLegacyMarkup().allTokens());
        earlier.addAll(new ProgressLegacyMarkup().allTokens());
        earlier.addAll(new EmptyStateLegacyMarkup().allTokens());
        earlier.addAll(new IconLegacyMarkup().allTokens());
        earlier.addAll(new SpinnerLegacyMarkup().allTokens());
        assertEquals(List.of(), ParityOracle.uncompiled(oracle, earlier), "the Skeleton oracle lost earlier legacy classes");
        assertFalse(scoped.contains("html,:host"), "the scoped legacy stylesheet must not carry Tailwind's page-wide base");
    }

    @Test
    void normalizationsStayWithinTheApprovedRulesAndTailwindPresentNeedsNone() {
        assertEquals(Map.of(), APPLIED_PRESENT, "Tailwind present needs no normalization at all");
        assertEquals(Map.of(), APPLIED_SCOPED, "the scoped legacy rendering must match the full one without any normalization");
        // Tailwind removed: only H2 (a border style is invisible at width 0; preflight gave solid, the browser gives none), on the four
        // border-style properties of every placeholder, in both removed comparisons.
        long rows = 0;
        for (ParityConfigs.Config c : CONFIGS) {
            rows += main.phase(c.id() + "|R").size();
        }
        assertEquals(Map.of("H2", (int) (2 * 4 * rows)), APPLIED_REMOVED, "exactly H2, four properties per placeholder, two comparisons");
    }

    // ---- what the real pages serve ----

    private static final Pattern SKELETON_TAG = Pattern.compile(
            "<div\\s+aria-hidden=\"true\"\\s+class=\"([^\"]*)\"\\s+style=\"width: ([^;\"]*); height: ([^\"]*)\"\\s+data-skeleton\\s+data-skeleton-shape=\"(rectangle|rounded)\"\\s*></div>");

    @Test
    void everySkeletonTheRealPagesServeHasTheApprovedShapeInlineSizeAndAlwaysHiddenSemanticsAndNoTailwind() throws Exception {
        ParitySession session = ParitySession.open("skeleton-parity");
        HttpClient client = HttpClient.newHttpClient();
        List<String> paths = new ArrayList<>(List.of("/ashlar/", "/ashlar/components"));
        for (ComponentPage page : ComponentCatalog.PAGES) {
            paths.add("/ashlar/components/" + page.slug());
        }
        int skeletons = 0;
        Map<String, Integer> perPage = new TreeMap<>();
        List<String> problems = new ArrayList<>();
        try (RunningShowcase app = RunningShowcase.start(session.bootJar())) {
            for (String path : paths) {
                HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + path)).GET().build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                assertEquals(200, response.statusCode(), path);
                String body = response.body();
                Matcher m = SKELETON_TAG.matcher(body);
                int found = 0;
                while (m.find()) {
                    found++;
                    String expected = SkeletonLegacyMarkup.MIGRATED.get(m.group(4));
                    if (!m.group(1).equals(expected)) {
                        problems.add(path + ": class " + m.group(1) + " does not match shape " + m.group(4));
                    }
                    if (m.group(2).isBlank() || m.group(3).isBlank()) {
                        problems.add(path + ": a skeleton without its inline width and height");
                    }
                }
                int hooks = (int) Pattern.compile("\\bdata-skeleton\\b(?!-)").matcher(body).results().count();
                assertEquals(hooks, found, path + ": every data-skeleton must be a Skeleton this check recognises (aria-hidden, class, inline size, hooks, no role)");
                assertEquals(SkeletonLegacyMarkup.count(body), found, path + ": every ash-skeleton class must be on a recognised Skeleton");
                assertFalse(Pattern.compile("data-skeleton[^>]*(animate-pulse|bg-slate)").matcher(body).find(), path + ": a Skeleton still carries Tailwind");
                if (found > 0) {
                    perPage.put(path, found);
                }
                skeletons += found;
            }
        }
        assertEquals(List.of(), problems);
        assertTrue(perPage.containsKey("/ashlar/components/skeleton"), "the Skeleton page renders Skeletons: " + perPage);
        assertTrue(skeletons >= 19, "skeletons=" + skeletons + " " + perPage);
    }
}
