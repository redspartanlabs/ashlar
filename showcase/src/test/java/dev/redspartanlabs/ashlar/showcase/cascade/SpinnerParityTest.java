package dev.redspartanlabs.ashlar.showcase.cascade;

import dev.redspartanlabs.ashlar.showcase.catalog.ComponentCatalog;
import dev.redspartanlabs.ashlar.showcase.catalog.ComponentPage;
import dev.redspartanlabs.ashlar.spinner.SpinnerSize;
import dev.redspartanlabs.ashlar.spinner.SpinnerStyles;
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
import java.util.Set;
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
 * The Spinner's parity gates, measured in real Chrome, on the shared parity harness ({@link ParitySession},
 * {@link ParityConfigs}, {@link ParityFrames}, {@link ParityProbe}, {@link ParityOracle}).
 *
 * <p>Evidence is a controlled oracle: the Spinner's legacy Tailwind markup (compiled by the real Tailwind CLI) next to its
 * migrated markup, in the same browser, in eight layouts (including the shape of Table's loading row) for each of the 22 theme
 * and skin configurations, with Tailwind present and with Tailwind removed. The ring turns forever, so every frame is paused
 * at the same instant (the same negative animation delay on both sides): the colours, the geometry and the rotated box are
 * then compared exactly, and a handful of further instants check the keyframes. The animation's duration, easing, count and
 * direction are compared as computed properties; its NAME is not (Tailwind's is "spin", Ashlar's is "ash-spinner-rotate"), so
 * the name is asserted separately. A second run, with Chrome told the user prefers reduced motion, checks that the animation
 * is gone on both sides. Every Spinner on the real showcase pages is compared with its legacy rendering by
 * {@code ShowcaseRuntimeTest}, and the Spinner markup the real pages serve is checked here. Negative controls show each gate can
 * fail.
 *
 * <p>Evidence is Chrome only. Computed-style and box parity is the visual evidence; there is no screenshot comparison.
 */
class SpinnerParityTest {

    private static final String ORACLE = ParityFrames.generated("spinner-oracle.css");
    private static final String SCOPED = ParityFrames.generated("spinner-legacy-scoped.css");
    private static final List<ParityConfigs.Config> CONFIGS = ParityConfigs.all();
    private static final List<ParityConfigs.Config> REDUCED_CONFIGS = ParityConfigs.reduced();
    private static final List<String> ANIMATION_PROPS = List.of("animation-duration", "animation-timing-function", "animation-iteration-count",
            "animation-direction", "animation-fill-mode", "transform");
    // The screen-reader-only label is measured too. "clip" is deliberately NOT compared: Ashlar's .ash-sr-only (a foundation
    // utility, unchanged here) sets clip: rect(0 0 0 0) in addition to the clip-path that hides the element, and Tailwind's
    // sr-only sets only the clip-path. The element is hidden identically (same box, same clip-path), so the property is no
    // evidence about the Spinner; clip-path and white-space are compared.
    private static final List<String> MAIN_PROPS = concat(ANIMATION_PROPS, List.of("clip-path", "white-space"));
    private static final List<String> REDUCED_PROPS = concat(MAIN_PROPS, List.of("animation-name"));
    private static final List<String> FACT_PROPS = List.of("animation-name", "animation-play-state", "animation-duration", "animation-timing-function", "animation-iteration-count");

    /** Pauses every animation at the same instant on both sides of a comparison: progress is then a fixed fraction, not a race. */
    private static String pausedAt(String delay) {
        return "*{animation-play-state:paused !important;animation-delay:" + delay + " !important}\n";
    }

    /** The instant of the main matrix: 0.3 of a one second turn, which is 108 degrees. */
    private static final String MAIN_PHASE = "-0.3s";
    /** Further instants, measured in the plain configuration: 45 degrees (the widest box) and three quarters of a turn. */
    private static final List<String> EXTRA_PHASES = List.of("-0.125s", "-0.75s");

    private static List<String> concat(List<String> a, List<String> b) {
        List<String> all = new ArrayList<>(a);
        all.addAll(b);
        return List.copyOf(all);
    }

    // ---- layouts and cases ----

    private record Layout(String name, String style, String siblingStyle) {
    }

    private static final List<Layout> LAYOUTS = List.of(
            new Layout("inline-text", "box-sizing:border-box;width:300px;line-height:20px;font-size:14px", ""),
            new Layout("block", "box-sizing:border-box;width:300px", ""),
            new Layout("loading-row", "box-sizing:border-box;display:flex;align-items:center;justify-content:center;gap:12px;width:300px;padding:24px 16px;font-size:14px", ""),
            new Layout("row-flex", "box-sizing:border-box;display:flex;flex-direction:row;width:300px;height:80px", ""),
            new Layout("column-flex", "box-sizing:border-box;display:flex;flex-direction:column;width:300px", ""),
            new Layout("column-flex-centered", "box-sizing:border-box;display:flex;flex-direction:column;align-items:center;width:300px", ""),
            new Layout("row-flex-squeezed", "box-sizing:border-box;display:flex;width:60px", "display:block;flex:none;width:50px"),
            new Layout("button-shape", "box-sizing:border-box;display:inline-flex;align-items:center;gap:8px;padding:4px 8px;font-size:14px", ""));

    private record Case(String size, boolean labelled) {
        String name() {
            return size + (labelled ? "-labelled" : "-unlabelled");
        }
    }

    private static final List<Case> CASES = SpinnerLegacyMarkup.SIZES.stream()
            .flatMap(size -> java.util.stream.Stream.of(new Case(size, true), new Case(size, false))).toList();

    // ---- state of the measurement ----

    private static SpinnerLegacyMarkup legacy;
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

    private static String spinner(Case c, boolean legacyClasses) {
        String wrapper = legacyClasses ? legacy.classes("wrapper") : "ash-spinner";
        String ring = legacyClasses ? legacy.classes("ring-" + c.size()) : SpinnerStyles.classes(SpinnerSize.valueOf(c.size().toUpperCase(Locale.ROOT)));
        String label = legacyClasses ? legacy.classes("label") : "ash-sr-only";
        return "<span class=\"" + wrapper + "\"" + (c.labelled() ? " role=\"status\" aria-live=\"polite\"" : "")
                + " data-spinner data-spinner-size=\"" + c.size() + "\" data-parity=\"spinner\" data-parity-case=\"" + c.name() + "\">"
                + "<span aria-hidden=\"true\" class=\"" + ring + "\" data-parity=\"ring\" data-parity-case=\"" + c.name() + "\"></span>"
                + (c.labelled() ? "<span class=\"" + label + "\" data-parity=\"label\" data-parity-case=\"" + c.name() + "\">Loading</span>" : "")
                + "</span>";
    }

    private static String markup(boolean legacyClasses) {
        StringBuilder sb = new StringBuilder();
        for (Layout layout : LAYOUTS) {
            for (Case c : CASES) {
                String sibling = "<span" + (layout.siblingStyle().isEmpty() ? "" : " style=\"" + layout.siblingStyle() + "\"") + ">";
                sb.append("<div style=\"margin:4px 0\"><div data-layout=\"").append(layout.name()).append("\" style=\"").append(layout.style()).append("\">")
                        .append(sibling).append("A</span>").append(spinner(c, legacyClasses)).append(sibling).append("B</span></div></div>");
            }
        }
        return sb.toString();
    }

    // ---- facts: single spinners whose computed values are checked against what the contract says ----

    private record Fact(String name, String wrapperClass, String wrapperStyle, String ringClass, String ringStyle) {
    }

    private static final String RING = "ash-spinner__ring ash-spinner__ring--";
    private static final List<Fact> FACTS = List.of(
            new Fact("small", "ash-spinner", "", RING + "small", ""),
            new Fact("medium", "ash-spinner", "", RING + "medium", ""),
            new Fact("large", "ash-spinner", "", RING + "large", ""),
            new Fact("xlarge", "ash-spinner", "", RING + "xlarge", ""),
            new Fact("default-ring", "ash-spinner", "", "ash-spinner__ring", ""),
            new Fact("accent-on-wrapper", "ash-spinner", "--ash-spinner-accent:rgb(1, 2, 3);--ash-spinner-track:rgb(4, 5, 6)", RING + "medium", ""),
            new Fact("size-on-ring", "ash-spinner", "", RING + "medium", "--ash-spinner-size:5rem;--ash-spinner-width:6px"),
            new Fact("size-on-wrapper-is-ignored", "ash-spinner", "--ash-spinner-size:5rem;--ash-spinner-width:6px", RING + "medium", ""),
            new Fact("consumer-rule", "ash-spinner fx-spinner", "", RING + "medium", ""));

    private static final String FACT_CSS = ".fx-spinner{--ash-spinner-accent:rgb(7, 8, 9)}\n";

    private static String factsMarkup() {
        StringBuilder sb = new StringBuilder();
        for (Fact f : FACTS) {
            sb.append("<div data-layout=\"facts\"><span class=\"").append(f.wrapperClass()).append('"')
                    .append(f.wrapperStyle().isEmpty() ? "" : " style=\"" + f.wrapperStyle() + "\"")
                    .append(" data-parity=\"spinner\" data-parity-case=\"").append(f.name()).append("\">")
                    .append("<span aria-hidden=\"true\" class=\"").append(f.ringClass()).append('"')
                    .append(f.ringStyle().isEmpty() ? "" : " style=\"" + f.ringStyle() + "\"")
                    .append(" data-parity=\"ring\" data-parity-case=\"").append(f.name()).append("\"></span></span></div>");
        }
        return sb.toString();
    }

    // ---- control experiments: each breaks the migrated Spinner in ONE way and must be detected ----

    private record Control(String name, String config, String css, String expectedProperty) {
    }

    private static final List<Control> CONTROL_LIST = List.of(
            new Control("wrong track colour", "light/none", ".ash-spinner{--ash-spinner-track:red}", "border-right-color"),
            new Control("wrong accent colour", "light/none", ".ash-spinner{--ash-spinner-accent:red}", "border-top-color"),
            new Control("dark accent ignores the theme", "dark/none", ".ash-spinner{--ash-spinner-accent:var(--color-blue-600)}", "border-top-color"),
            new Control("colour ignores the skin", "light/civic", ".ash-spinner{--ash-spinner-accent:#2563eb;--ash-spinner-track:#e2e8f0}", "border-top-color"),
            new Control("border style dropped", "light/none", ".ash-spinner__ring{border-style:none}", "border-top-style"),
            new Control("small ring too big", "light/none", ".ash-spinner__ring--small{--ash-spinner-size:1.25rem}", "width"),
            new Control("large ring too thin", "light/none", ".ash-spinner__ring--large{--ash-spinner-width:2px}", "border-top-width"),
            new Control("ring not round", "light/none", ".ash-spinner__ring{border-radius:4px}", "border-top-left-radius"),
            new Control("ring uses a content box", "light/none", ".ash-spinner__ring{box-sizing:content-box}", "box-sizing"),
            new Control("ring may shrink", "light/none", ".ash-spinner__ring{flex-shrink:1}", "flex-shrink"),
            new Control("wrapper is not inline-flex", "light/none", ".ash-spinner{display:flex}", "display"),
            new Control("label is not hidden", "light/none", ".ash-sr-only{position:static}", "position"),
            new Control("wrong duration", "light/none", ".ash-spinner__ring{animation-duration:2s}", "animation-duration"),
            new Control("wrong easing", "light/none", ".ash-spinner__ring{animation-timing-function:ease}", "animation-timing-function"),
            new Control("keyframes turn half way", "light/none", "@keyframes ash-spinner-rotate{to{transform:rotate(180deg)}}", "transform"));

    private static final String REDUCED_CONTROL_CSS = "@media (prefers-reduced-motion: reduce){.ash-spinner__ring{animation:ash-spinner-rotate 1s linear infinite}}";

    // ---- running ----

    @BeforeAll
    static void measure() throws Exception {
        ParitySession session = ParitySession.open("spinner-parity");
        legacy = new SpinnerLegacyMarkup();
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
        frames.append(ParityFrames.frame("facts|A", light, ParityFrames.with(ParityFrames.ASHLAR, ParityConfigs.SKIN_LINKS), environment + pausedAt(MAIN_PHASE) + FACT_CSS, factsMarkup()));
        frames.append(ParityFrames.frame("facts|D", ParityConfigs.get("dark/none"), ParityFrames.with(ParityFrames.ASHLAR, ParityConfigs.SKIN_LINKS), environment + pausedAt(MAIN_PHASE) + FACT_CSS, factsMarkup()));
        main = ParityFrames.measure(session, frames.toString(), MAIN_PROPS, "spinner frames");
        List<String> props = main.props();
        for (ParityConfigs.Config c : CONFIGS) {
            compareMatrix(c.id(), c.id(), props, main, PRESENT, REMOVED_SCOPED, REMOVED_FULL, SCOPED_FIDELITY, APPLIED_PRESENT, APPLIED_REMOVED, APPLIED_SCOPED);
        }
        Map<String, Integer> phaseApplied = new TreeMap<>();
        for (String phase : EXTRA_PHASES) {
            Map<String, List<ParityProbe.Finding>> present = new LinkedHashMap<>();
            Map<String, List<ParityProbe.Finding>> scoped = new LinkedHashMap<>();
            Map<String, List<ParityProbe.Finding>> full = new LinkedHashMap<>();
            Map<String, List<ParityProbe.Finding>> fidelity = new LinkedHashMap<>();
            compareMatrix("phase" + phase, "phase" + phase, props, main, present, scoped, full, fidelity, phaseApplied, phaseApplied, phaseApplied);
            PHASES.putAll(present);
            PHASES.putAll(scoped);
            PHASES.putAll(full);
            PHASES.putAll(fidelity);
        }
        for (Control control : CONTROL_LIST) {
            CONTROLS.put(control.name(), ParityProbe.compare("control: " + control.name(), props, main.phase(control.config() + "|L"),
                    main.phase("ctl:" + control.name() + "|P"), new TreeMap<>()));
        }

        // Run 2: nothing paused, so the animation's own name and state can be read.
        StringBuilder running = new StringBuilder();
        running.append(ParityFrames.frame("anim|A", light, ParityFrames.with(ParityFrames.ASHLAR, ParityConfigs.SKIN_LINKS), environment + FACT_CSS, factsMarkup()));
        running.append(ParityFrames.frame("anim|L", light, ParityFrames.with(ORACLE, ParityConfigs.SKIN_LINKS), FACT_CSS, legacyFacts()));
        animation = ParityFrames.measure(session, running.toString(), FACT_PROPS, "spinner animation");

        // Run 3: Chrome is told the user prefers reduced motion.
        StringBuilder calm = new StringBuilder();
        for (ParityConfigs.Config c : REDUCED_CONFIGS) {
            addMatrix(calm, c.id(), c, environment, migrated, legacyMarkup, MAIN_PHASE);
        }
        calm.append(ParityFrames.frame("ctl:reduced|P", light, ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), pausedAt(MAIN_PHASE) + REDUCED_CONTROL_CSS, migrated));
        reduced = ParityFrames.measure(session, calm.toString(), REDUCED_PROPS, List.of("--force-prefers-reduced-motion"), "spinner reduced motion");
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

    private static void compareMatrix(String id, String label, List<String> props, ParityProbe.Probe probe,
                                      Map<String, List<ParityProbe.Finding>> present, Map<String, List<ParityProbe.Finding>> removedScoped,
                                      Map<String, List<ParityProbe.Finding>> removedFull, Map<String, List<ParityProbe.Finding>> fidelity,
                                      Map<String, Integer> appliedPresent, Map<String, Integer> appliedRemoved, Map<String, Integer> appliedScoped) {
        List<ParityProbe.Row> legacyRows = probe.phase(id + "|L");
        ROWS.put(id + "|L", legacyRows.size());
        ROWS.put(id + "|R", probe.phase(id + "|R").size());
        present.put(label + " present", ParityProbe.compare(label + " (Tailwind present)", props, legacyRows, probe.phase(id + "|P"), appliedPresent));
        removedScoped.put(label + " removed-scoped", ParityProbe.compare(label + " (Tailwind removed vs scoped legacy)", props, probe.phase(id + "|S"), probe.phase(id + "|R"), appliedRemoved));
        removedFull.put(label + " removed-full", ParityProbe.compare(label + " (Tailwind removed vs full legacy)", props, legacyRows, probe.phase(id + "|R"), appliedRemoved));
        fidelity.put(label + " fidelity", ParityProbe.compare(label + " (scoped legacy vs full legacy)", props, legacyRows, probe.phase(id + "|S"), appliedScoped));
    }

    /** The facts markup rendered with the legacy classes (sizes only: the legacy Spinner had no customisation properties). */
    private static String legacyFacts() {
        StringBuilder sb = new StringBuilder();
        for (String size : SpinnerLegacyMarkup.SIZES) {
            sb.append("<div data-layout=\"facts\"><span class=\"").append(legacy.classes("wrapper")).append("\" data-parity=\"spinner\" data-parity-case=\"").append(size).append("\">")
                    .append("<span aria-hidden=\"true\" class=\"").append(legacy.classes("ring-" + size)).append("\" data-parity=\"ring\" data-parity-case=\"").append(size).append("\"></span></span></div>");
        }
        return sb.toString();
    }

    private static List<String> lines(Map<String, List<ParityProbe.Finding>> findings) {
        List<String> out = new ArrayList<>();
        findings.values().forEach(list -> list.forEach(f -> out.add(f.line())));
        return out;
    }

    private static String fact(ParityProbe.Probe probe, String phase, String name, String kind, String property) {
        List<ParityProbe.Row> rows = probe.phase(phase).stream().filter(r -> r.kind().equals(kind) && r.variant().equals(name + "/facts")).toList();
        assertEquals(1, rows.size(), phase + " must measure exactly one " + kind + " " + name);
        return probe.value(rows.get(0), property);
    }

    // ---- tests ----

    @Test
    void theConfigurationsAreTheTwentyTwoThemeAndSkinCombinations() {
        assertEquals(22, CONFIGS.size());
        assertEquals(4, REDUCED_CONFIGS.size());
    }

    @Test
    void controlledOracleWithTailwindPresentMatchesTheLegacySpinnerInEveryConfigurationCaseAndLayout() {
        assertEquals(List.of(), lines(PRESENT));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheLegacySpinnerGivenTheSameSurroundings() {
        assertEquals(List.of(), lines(REMOVED_SCOPED));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheFullLegacyRendering() {
        assertEquals(List.of(), lines(REMOVED_FULL));
    }

    @Test
    void theScopedLegacyStylesheetRendersTheLegacySpinnerExactlyAsTheFullOneDoes() {
        assertEquals(List.of(), lines(SCOPED_FIDELITY));
    }

    @Test
    void theKeyframesMatchTheLegacyTurnAtFurtherInstantsInTheRotatedBoxAndTheTransform() {
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
        int perFrame = LAYOUTS.size() * (int) CASES.stream().mapToInt(c -> c.labelled() ? 3 : 2).sum();
        assertEquals(perFrame, ROWS.get("light/none|L"));
        assertEquals(perFrame, ROWS.get("dark/civic|R"));
        Set<String> widths = new TreeSet<>();
        int turned = 0;
        for (ParityProbe.Row row : main.phase("light/none|L")) {
            if (!row.kind().equals("ring")) {
                continue;
            }
            widths.add(main.value(row, "width"));
            // At 108 degrees a square's bounding box is larger than the square, so a ring whose animation did not apply would equal its own size.
            if (Double.parseDouble(main.value(row, "@w")) > Double.parseDouble(main.value(row, "width").replace("px", "")) + 1) {
                turned++;
            }
            assertNotEquals("none", main.value(row, "transform"), "the paused animation must have a transform at this instant");
        }
        assertEquals(Set.of("16px", "24px", "32px", "48px"), widths);
        assertEquals(LAYOUTS.size() * CASES.size(), turned, "every ring is measured mid-turn");
    }

    @Test
    void everyPublicSizeHasItsApprovedDimensionsAndBorderWidthWithTailwindRemoved() {
        Map<String, String[]> expected = Map.of("small", new String[]{"16px", "2px"}, "medium", new String[]{"24px", "2px"},
                "large", new String[]{"32px", "3px"}, "xlarge", new String[]{"48px", "4px"});
        for (Map.Entry<String, String[]> e : expected.entrySet()) {
            assertEquals(e.getValue()[0], fact(main, "facts|A", e.getKey(), "ring", "width"), e.getKey() + " width");
            assertEquals(e.getValue()[0], fact(main, "facts|A", e.getKey(), "ring", "height"), e.getKey() + " height");
            for (String side : List.of("top", "right", "bottom", "left")) {
                assertEquals(e.getValue()[1], fact(main, "facts|A", e.getKey(), "ring", "border-" + side + "-width"), e.getKey() + " " + side);
                assertEquals("solid", fact(main, "facts|A", e.getKey(), "ring", "border-" + side + "-style"), "a border is only drawn with a style, and preflight is gone");
            }
            assertEquals("border-box", fact(main, "facts|A", e.getKey(), "ring", "box-sizing"));
            assertEquals("0", fact(main, "facts|A", e.getKey(), "ring", "flex-shrink"));
        }
        assertEquals("24px", fact(main, "facts|A", "default-ring", "ring", "width"), "a ring with no size modifier is the medium size");
        assertEquals("2px", fact(main, "facts|A", "default-ring", "ring", "border-top-width"));
    }

    @Test
    void theArcIsTheAccentAndTheOtherThreeSidesAreTheTrackInBothThemes() {
        for (String phase : List.of("facts|A", "facts|D")) {
            String top = fact(main, phase, "medium", "ring", "border-top-color");
            String right = fact(main, phase, "medium", "ring", "border-right-color");
            assertNotEquals(top, right, phase + ": the arc must differ from the track");
            assertEquals(right, fact(main, phase, "medium", "ring", "border-bottom-color"));
            assertEquals(right, fact(main, phase, "medium", "ring", "border-left-color"));
        }
        assertNotEquals(fact(main, "facts|A", "medium", "ring", "border-top-color"), fact(main, "facts|D", "medium", "ring", "border-top-color"), "the dark accent differs");
        assertNotEquals(fact(main, "facts|A", "medium", "ring", "border-right-color"), fact(main, "facts|D", "medium", "ring", "border-right-color"), "the dark track differs");
    }

    @Test
    void theCustomisationPropertiesWorkWhereTheyAreDocumented() {
        assertEquals("rgb(1, 2, 3)", fact(main, "facts|A", "accent-on-wrapper", "ring", "border-top-color"));
        assertEquals("rgb(4, 5, 6)", fact(main, "facts|A", "accent-on-wrapper", "ring", "border-right-color"));
        assertEquals("80px", fact(main, "facts|A", "size-on-ring", "ring", "width"));
        assertEquals("6px", fact(main, "facts|A", "size-on-ring", "ring", "border-top-width"));
        assertEquals("24px", fact(main, "facts|A", "size-on-wrapper-is-ignored", "ring", "width"), "the ring sets its own size, so a value on the wrapper changes nothing");
        assertEquals("rgb(7, 8, 9)", fact(main, "facts|A", "consumer-rule", "ring", "border-top-color"));
    }

    @Test
    void theAnimationIsAshlarsOwnAndRunsOneSecondLinearlyForeverWhileTheLegacyOneIsTailwindsSpin() {
        for (String size : SpinnerLegacyMarkup.SIZES) {
            assertEquals("ash-spinner-rotate", fact(animation, "anim|A", size, "ring", "animation-name"), size);
            assertEquals("running", fact(animation, "anim|A", size, "ring", "animation-play-state"));
            assertEquals("1s", fact(animation, "anim|A", size, "ring", "animation-duration"));
            assertEquals("linear", fact(animation, "anim|A", size, "ring", "animation-timing-function"));
            assertEquals("infinite", fact(animation, "anim|A", size, "ring", "animation-iteration-count"));
            assertEquals("spin", fact(animation, "anim|L", size, "ring", "animation-name"), "the legacy oracle really animates with Tailwind's spin");
            assertEquals("running", fact(animation, "anim|L", size, "ring", "animation-play-state"));
        }
    }

    @Test
    void withReducedMotionRequestedTheAnimationIsGoneOnBothSidesAndEverythingElseStillMatches() {
        assertEquals(List.of(), lines(REDUCED_FINDINGS));
        // Only the same invisible border-style rule as above (the borderless wrapper and label, four properties each, one removed comparison).
        long borderless = 0;
        for (ParityConfigs.Config c : REDUCED_CONFIGS) {
            borderless += reduced.phase(c.id() + "|R").stream().filter(r -> !r.kind().equals("ring")).count();
        }
        assertEquals(Map.of("H2", (int) (4 * borderless)), APPLIED_REDUCED);
        for (ParityProbe.Row row : reduced.rows()) {
            if (row.kind().equals("ring") && !row.phase().startsWith("ctl:")) {
                assertEquals("none", reduced.value(row, "animation-name"), row.phase() + " " + row.variant());
                assertEquals("none", reduced.value(row, "transform"), "a still ring is not turned");
            }
        }
        // The ring is still drawn, still the same size, and the labelled spinner is still a status region (the markup is unchanged).
        List<ParityProbe.Row> rings = reduced.phase("light/none|R").stream().filter(r -> r.kind().equals("ring")).toList();
        assertFalse(rings.isEmpty());
        assertTrue(rings.stream().allMatch(r -> !reduced.value(r, "border-top-width").equals("0px") && !reduced.value(r, "width").equals("0px")));
    }

    @Test
    void theReducedMotionControlIsDetected() {
        List<ParityProbe.Finding> findings = REDUCED_CONTROLS.get("reduced-motion rule removed");
        assertTrue(findings.stream().anyMatch(f -> f.property().equals("animation-name")), "a spinner that keeps turning must be detected in animation-name: " + findings.size());
    }

    @Test
    void theOracleIsNotVacuousEveryLegacyClassIsCompiledInTheCumulativeAndScopedStylesheets() throws IOException {
        Path dir = Path.of(System.getProperty("ashlar.cascade.dir"));
        String oracle = Files.readString(dir.resolve("spinner-oracle.css"), StandardCharsets.UTF_8);
        String scoped = Files.readString(dir.resolve("spinner-legacy-scoped.css"), StandardCharsets.UTF_8);
        assertEquals(List.of(), ParityOracle.uncompiled(oracle, legacy.allTokens()), "the Spinner oracle does not compile these (border-[3px] needs the bracket escape)");
        // The scoped stylesheet reads no source but spinner-legacy.html, so this also proves its @source path resolves.
        assertEquals(List.of(), ParityOracle.uncompiled(scoped, legacy.allTokens()), "the scoped stylesheet does not compile these");
        // Cumulative, in order Badge -> Alert -> Divider -> Progress -> Empty State -> Icon -> Spinner: the earlier legacy classes are still in the oracle.
        List<String> earlier = new ArrayList<>();
        new BadgeLegacyMarkup().variants().values().forEach(classes -> earlier.addAll(Arrays.asList(classes.split("\\s+"))));
        earlier.addAll(new AlertLegacyMarkup().allTokens());
        earlier.addAll(new DividerLegacyMarkup().allTokens());
        earlier.addAll(new ProgressLegacyMarkup().allTokens());
        earlier.addAll(new EmptyStateLegacyMarkup().allTokens());
        earlier.addAll(new IconLegacyMarkup().allTokens());
        assertEquals(List.of(), ParityOracle.uncompiled(oracle, earlier), "the Spinner oracle lost earlier legacy classes");
        assertFalse(scoped.contains("html,:host"), "the scoped legacy stylesheet must not carry Tailwind's page-wide base");
    }

    @Test
    void normalizationsStayWithinTheApprovedRulesAndTailwindPresentNeedsNone() {
        assertEquals(Map.of(), APPLIED_PRESENT, "Tailwind present needs no normalization at all");
        assertEquals(Map.of(), APPLIED_SCOPED, "the scoped legacy rendering must match the full one without any normalization");
        // Tailwind removed: only H2 (a border style is invisible at width 0; preflight gave solid, the browser gives none). The ring draws a
        // real solid border on both sides, so it never needs it; the wrapper and the label have no border (width 0 on both sides). Four
        // border-style properties per such element, in both removed comparisons.
        long borderless = 0;
        for (ParityConfigs.Config c : CONFIGS) {
            borderless += main.phase(c.id() + "|R").stream().filter(r -> !r.kind().equals("ring")).count();
        }
        assertTrue(borderless > 0);
        assertEquals(Map.of("H2", (int) (2 * 4 * borderless)), APPLIED_REMOVED, "exactly H2, on the borderless wrapper and label only");
    }

    // ---- what the real pages serve ----

    private static final Pattern SPINNER_TAG = Pattern.compile("<span class=\"ash-spinner\"([^>]*)>(\\s*)<span aria-hidden=\"true\" class=\"([^\"]*)\"></span>(?:\\s*<span class=\"([^\"]*)\">(.*?)</span>)?\\s*</span>", Pattern.DOTALL);

    @Test
    void everySpinnerTheRealPagesServeHasTheApprovedShapeAndAccessibilitySemanticsAndNoTailwind() throws Exception {
        ParitySession session = ParitySession.open("spinner-parity");
        HttpClient client = HttpClient.newHttpClient();
        List<String> paths = new ArrayList<>(List.of("/ashlar/", "/ashlar/components"));
        for (ComponentPage page : ComponentCatalog.PAGES) {
            paths.add("/ashlar/components/" + page.slug());
        }
        int spinners = 0;
        int labelled = 0;
        int unlabelled = 0;
        Map<String, Integer> perPage = new TreeMap<>();
        List<String> problems = new ArrayList<>();
        try (RunningShowcase app = RunningShowcase.start(session.bootJar())) {
            for (String path : paths) {
                HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + path)).GET().build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                assertEquals(200, response.statusCode(), path);
                String body = response.body();
                Matcher m = SPINNER_TAG.matcher(body);
                int found = 0;
                while (m.find()) {
                    found++;
                    String attrs = m.group(1);
                    Matcher size = Pattern.compile("data-spinner-size=\"(small|medium|large|xlarge)\"").matcher(attrs);
                    boolean hook = Pattern.compile("(^|\\s)data-spinner(\\s|$)").matcher(attrs).find();
                    if (!hook || !size.find()) {
                        problems.add(path + ": a spinner without its data hooks: " + attrs.trim());
                        continue;
                    }
                    if (!m.group(3).equals(SpinnerLegacyMarkup.migratedRing(size.group(1)))) {
                        problems.add(path + ": ring class " + m.group(3) + " does not match size " + size.group(1));
                    }
                    boolean status = attrs.contains("role=\"status\"") && attrs.contains("aria-live=\"polite\"");
                    boolean decorative = !attrs.contains("role=") && !attrs.contains("aria-live");
                    if (m.group(5) != null) {
                        labelled++;
                        if (!status || !"ash-sr-only".equals(m.group(4)) || m.group(5).isBlank()) {
                            problems.add(path + ": a labelled spinner must be a polite status region with an ash-sr-only label");
                        }
                    } else {
                        unlabelled++;
                        if (!decorative) {
                            problems.add(path + ": an unlabelled spinner must be purely decorative (no role, no aria-live)");
                        }
                    }
                }
                int hooks = (int) Pattern.compile("\\bdata-spinner\\b(?!-)").matcher(body).results().count();
                assertEquals(hooks, found, path + ": every data-spinner must be a Spinner this check recognises");
                assertEquals(SpinnerLegacyMarkup.count(body), found, path + ": every ash-spinner class must be on a recognised Spinner");
                assertFalse(Pattern.compile("data-spinner[^>]*class=\"[^\"]*(animate-spin|inline-flex)").matcher(body).find(), path + ": a Spinner still carries Tailwind");
                if (found > 0) {
                    perPage.put(path, found);
                }
                spinners += found;
            }
        }
        assertEquals(List.of(), problems);
        assertTrue(perPage.containsKey("/ashlar/components/spinner"), "the Spinner page renders Spinners: " + perPage);
        assertTrue(perPage.containsKey("/ashlar/components/table"), "Table's loading state composes a Spinner: " + perPage);
        assertTrue(spinners >= 9 && labelled >= 1 && unlabelled >= 2, "spinners=" + spinners + " labelled=" + labelled + " unlabelled=" + unlabelled);
    }
}
