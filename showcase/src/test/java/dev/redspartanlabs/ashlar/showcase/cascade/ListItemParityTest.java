package dev.redspartanlabs.ashlar.showcase.cascade;

import dev.redspartanlabs.ashlar.icon.Icon;
import dev.redspartanlabs.ashlar.showcase.catalog.ComponentCatalog;
import dev.redspartanlabs.ashlar.showcase.catalog.ComponentPage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
 * The List Item's parity gates, measured in real Chrome, on the shared parity harness (unchanged since the Icon migration).
 *
 * <p>Evidence is a controlled oracle: the List Item's legacy Tailwind markup (compiled by the real Tailwind CLI) next to its migrated
 * markup, in the same browser, in four widths (down to one narrow enough to force the label to wrap) for each of the 22 theme and skin
 * configurations, with Tailwind present and with Tailwind removed. Every part of every row is measured: the row, the icon wrapper and its
 * Icon, the text column, the label, the description, the trailing content, the chevron and its Icon (the migrated Icon modifiers must give
 * exactly the legacy sizes). A hover cannot be held in a measurement, so the colour a hover paints is compared through a swatch; the
 * keyboard focus ring IS measured, one frame per Chrome run because only one frame holds focus. Negative controls show each gate can fail.
 * The List-level measurements are in {@code ListParityTest}; every row on the real showcase page is also compared by {@code ShowcaseRuntimeTest}.
 *
 * <p>Evidence is Chrome only. Computed-style and box parity is the visual evidence; there is no screenshot comparison.
 */
class ListItemParityTest {

    private static final String ORACLE = ParityFrames.generated("list-oracle.css");
    private static final String SCOPED = ParityFrames.generated("list-legacy-scoped.css");
    private static final List<ParityConfigs.Config> CONFIGS = ParityConfigs.all();
    private static final List<String> EXTRA_PROPS = List.of("column-gap", "row-gap", "align-items", "list-style-type", "text-decoration-line", "line-height",
            "font-size", "font-weight", "outline-style", "outline-width");

    /** The kinds of measured element that are the caller's own content, not the component's: preflight reached them too, so a full-legacy comparison leaves them out. */
    private static final java.util.Set<String> CALLER_KINDS = java.util.Set.of("trailing-content", "hover-swatch");

    private static List<ParityProbe.Row> owned(List<ParityProbe.Row> rows) {
        return rows.stream().filter(r -> !CALLER_KINDS.contains(r.kind())).toList();
    }

    private record Layout(String name, String style) {
    }

    private static final List<Layout> LAYOUTS = List.of(
            new Layout("wide", "box-sizing:border-box;width:360px"),
            new Layout("narrow", "box-sizing:border-box;width:220px"),
            new Layout("very-narrow", "box-sizing:border-box;width:140px"),
            new Layout("shrink-to-fit", "box-sizing:border-box;display:flex;flex-direction:column;align-items:flex-start;width:420px"));

    private static final String TRAILING = "<span style=\"font-weight:500\" data-parity=\"trailing-content\" data-parity-case=\"%s\">Pro</span>";
    private static final String LONG_LABEL = "A very long label that has to wrap inside the row instead of pushing the other content out of the list";
    private static final String LONG_DESCRIPTION = "A long description that also has to wrap inside the column rather than overflowing the row";

    private static ListFixtures.Item item(String name, String label, String description, boolean link, Icon icon, boolean trailing) {
        return new ListFixtures.Item(name, label, description, link, icon, trailing ? TRAILING.formatted(name) : null);
    }

    private static final List<ListFixtures.Item> CASES = List.of(
            item("label", "Overview", "", false, null, false),
            item("description", "Overview", "Summary of the account", false, null, false),
            item("icon", "Overview", "", false, Icon.USER, false),
            item("trailing", "Plan", "", false, null, true),
            item("link", "Overview", "", true, null, false),
            item("link-icon-description", "Notifications", "Email and in-app alerts", true, Icon.BELL, false),
            item("link-trailing", "Plan", "", true, null, true),
            item("full-link", "Security", "Two-factor authentication and sessions", true, Icon.SHIELD, true),
            item("full-plain", "Security", "Two-factor authentication and sessions", false, Icon.SHIELD, true),
            item("long-label", LONG_LABEL, "", true, Icon.USER, false),
            item("long-description", "Overview", LONG_DESCRIPTION, false, null, true),
            item("long-everything", LONG_LABEL, LONG_DESCRIPTION, true, Icon.SHIELD, true));

    // ---- state of the measurement ----

    private static ListLegacyMarkup legacy;
    private static ListFixtures fixtures;
    private static ParityProbe.Probe probe;
    private static final Map<String, List<ParityProbe.Finding>> PRESENT = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> REMOVED_SCOPED = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> REMOVED_FULL = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> SCOPED_FIDELITY = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> CONTROLS = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> FOCUS = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> FOCUS_CONTROLS = new LinkedHashMap<>();
    private static final Map<String, ParityProbe.Probe> FOCUS_PROBES = new LinkedHashMap<>();
    private static final Map<String, Integer> APPLIED_PRESENT = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_REMOVED = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_SCOPED = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_FOCUS = new TreeMap<>();
    private static final Map<String, Integer> ROWS = new LinkedHashMap<>();

    // ---- markup ----

    private static String markup(boolean legacyClasses) {
        StringBuilder sb = new StringBuilder();
        for (Layout layout : LAYOUTS) {
            for (ListFixtures.Item c : CASES) {
                String children = fixtures.item(c, legacyClasses, true, false) + fixtures.item(item("filler", "Another row", "", false, null, false), legacyClasses, false, false);
                sb.append("<div style=\"margin:4px 0\"><div data-layout=\"").append(layout.name()).append("\" style=\"").append(layout.style()).append("\">")
                        .append(fixtures.list(c.name(), false, children, legacyClasses, true, "list")).append("</div></div>");
            }
        }
        return sb.append("<div data-layout=\"swatch\">").append(fixtures.hoverSwatch(legacyClasses)).append("</div>").toString();
    }

    /** One link row, focused by a script as soon as its frame loads, as the only content of its frame. */
    private static String focusBody(boolean legacyClasses) {
        ListFixtures.Item focused = item("focus", "Account", "Profile and password", true, Icon.USER, true);
        return fixtures.list("focus", false, fixtures.item(focused, legacyClasses, true, true) + fixtures.item(item("filler", "Another row", "", false, null, false), legacyClasses, false, false),
                legacyClasses, true, "list") + "<script>document.querySelector('[data-focus]').focus();</script>";
    }

    // ---- control experiments: each breaks the migrated List Item in ONE way and must be detected ----

    private record Control(String name, String config, String css, String expectedProperty) {
    }

    private static final List<Control> CONTROL_LIST = List.of(
            new Control("wrong gap", "light/none", ".ash-list-item__row{gap:8px}", "column-gap"),
            new Control("wrong horizontal padding", "light/none", ".ash-list-item__row{padding-inline:12px}", "padding-left"),
            new Control("wrong vertical padding", "light/none", ".ash-list-item__row{padding-block:8px}", "padding-top"),
            new Control("wrong font size", "light/none", ".ash-list-item__row{font-size:1rem}", "font-size"),
            new Control("wrong line height", "light/none", ".ash-list-item__row{line-height:1.2}", "line-height"),
            new Control("row is not a flex box", "light/none", ".ash-list-item__row{display:block}", "display"),
            new Control("label is lighter", "light/none", ".ash-list-item__label{font-weight:700}", "font-weight"),
            new Control("wrong label colour", "light/none", ".ash-list-item__label{color:red}", "color"),
            new Control("wrong description colour", "light/none", ".ash-list-item__description{color:red}", "color"),
            new Control("wrong icon colour", "light/none", ".ash-list-item__icon{color:red}", "color"),
            new Control("wrong trailing colour", "light/none", ".ash-list-item__trailing{color:red}", "color"),
            new Control("wrong chevron colour", "light/none", ".ash-list-item__chevron{color:red}", "color"),
            new Control("dark label ignores the theme", "dark/none", ".ash-list-item{--ash-list-item-label:var(--color-slate-900)}", "color"),
            new Control("colour ignores the skin", "light/civic", ".ash-list-item{--ash-list-item-label:#0f172a}", "color"),
            new Control("long label no longer wraps", "light/none", ".ash-list-item__body{min-width:auto}", "min-width"),
            new Control("trailing content shrinks", "light/none", ".ash-list-item__trailing{flex-shrink:1}", "flex-shrink"),
            new Control("link row has its own colour", "light/none", ".ash-list-item__row--link{color:red}", "color"),
            new Control("link row is underlined", "light/none", ".ash-list-item__row--link{text-decoration:underline}", "text-decoration-line"),
            new Control("wrong hover colour", "light/none", ".ash-list-item{--ash-list-item-hover:red}", "background-color"),
            new Control("dark hover ignores the theme", "dark/none", ".ash-list-item{--ash-list-item-hover:var(--color-slate-50)}", "background-color"),
            new Control("icon is the wrong size", "light/none", ".ash-list-item__icon .ash-icon{--ash-icon-size:2rem}", "width"));

    private static final List<ParityConfigs.Config> FOCUS_CONFIGS = List.of(ParityConfigs.get("light/none"), ParityConfigs.get("dark/none"));
    private static final String FOCUS_CONTROL_RING = ".ash-list-item__row--link:focus-visible{box-shadow:none}";
    private static final String FOCUS_CONTROL_OUTLINE = ".ash-list-item__row--link:focus{outline-style:auto}";

    // ---- running ----

    @BeforeAll
    static void measure() throws Exception {
        ParitySession session = ParitySession.open("list-item-parity");
        legacy = new ListLegacyMarkup();
        fixtures = new ListFixtures(legacy, new IconLegacyMarkup());
        String environment = ParityFrames.environment(session.generatedDir());
        String migrated = markup(false);
        String legacyMarkup = markup(true);

        StringBuilder frames = new StringBuilder();
        for (ParityConfigs.Config c : CONFIGS) {
            frames.append(ParityFrames.frame(c.id() + "|L", c, ParityFrames.with(ORACLE, ParityConfigs.SKIN_LINKS), "", legacyMarkup));
            frames.append(ParityFrames.frame(c.id() + "|P", c, ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), "", migrated));
            frames.append(ParityFrames.frame(c.id() + "|R", c, ParityFrames.with(ParityFrames.ASHLAR, ParityConfigs.SKIN_LINKS), environment, migrated));
            frames.append(ParityFrames.frame(c.id() + "|S", c, ParityFrames.with(SCOPED, ParityConfigs.SKIN_LINKS), environment, legacyMarkup));
        }
        for (Control control : CONTROL_LIST) {
            frames.append(ParityFrames.frame("ctl:" + control.name() + "|P", ParityConfigs.get(control.config()),
                    ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), control.css(), migrated));
        }
        probe = ParityFrames.measure(session, frames.toString(), EXTRA_PROPS, "list item frames");
        List<String> props = probe.props();
        for (ParityConfigs.Config c : CONFIGS) {
            List<ParityProbe.Row> legacyRows = probe.phase(c.id() + "|L");
            ROWS.put(c.id() + "|L", legacyRows.size());
            PRESENT.put(c.id(), ParityProbe.compare(c.id() + " (Tailwind present)", props, legacyRows, probe.phase(c.id() + "|P"), APPLIED_PRESENT));
            REMOVED_SCOPED.put(c.id(), ParityProbe.compare(c.id() + " (Tailwind removed vs scoped legacy)", props, probe.phase(c.id() + "|S"), probe.phase(c.id() + "|R"), APPLIED_REMOVED));
            REMOVED_FULL.put(c.id(), ParityProbe.compare(c.id() + " (Tailwind removed vs full legacy, the components' own elements)", props, owned(legacyRows), owned(probe.phase(c.id() + "|R")), APPLIED_REMOVED));
            SCOPED_FIDELITY.put(c.id(), ParityProbe.compare(c.id() + " (scoped legacy vs full legacy, the components' own elements)", props, owned(legacyRows), owned(probe.phase(c.id() + "|S")), APPLIED_SCOPED));
        }
        for (Control control : CONTROL_LIST) {
            CONTROLS.put(control.name(), ParityProbe.compare("control: " + control.name(), props, probe.phase(control.config() + "|L"),
                    probe.phase("ctl:" + control.name() + "|P"), new TreeMap<>()));
        }

        // The keyboard focus ring: one frame per Chrome run, because only one frame holds focus.
        for (ParityConfigs.Config c : FOCUS_CONFIGS) {
            ParityProbe.Probe l = focusRun(session, c, "L", ParityFrames.with(ORACLE, ParityConfigs.SKIN_LINKS), "", true);
            ParityProbe.Probe p = focusRun(session, c, "P", ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), "", false);
            ParityProbe.Probe s = focusRun(session, c, "S", ParityFrames.with(SCOPED, ParityConfigs.SKIN_LINKS), environment, true);
            ParityProbe.Probe r = focusRun(session, c, "R", ParityFrames.with(ParityFrames.ASHLAR, ParityConfigs.SKIN_LINKS), environment, false);
            FOCUS_PROBES.put(c.id() + "|L", l);
            FOCUS_PROBES.put(c.id() + "|P", p);
            FOCUS_PROBES.put(c.id() + "|R", r);
            FOCUS.put(c.id() + " present", ParityProbe.compare(c.id() + " (focused, Tailwind present)", l.props(), l.rows(), p.rows(), APPLIED_FOCUS));
            FOCUS.put(c.id() + " removed", ParityProbe.compare(c.id() + " (focused, Tailwind removed)", s.props(), s.rows(), r.rows(), APPLIED_FOCUS));
        }
        ParityConfigs.Config light = ParityConfigs.get("light/none");
        ParityProbe.Probe legacyFocused = FOCUS_PROBES.get("light/none|L");
        FOCUS_CONTROLS.put("focus ring dropped", ParityProbe.compare("control: focus ring dropped", legacyFocused.props(), legacyFocused.rows(),
                focusRun(session, light, "C1", ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), FOCUS_CONTROL_RING, false).rows(), new TreeMap<>()));
        FOCUS_CONTROLS.put("browser outline comes back", ParityProbe.compare("control: browser outline comes back", legacyFocused.props(), legacyFocused.rows(),
                focusRun(session, light, "C2", ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), FOCUS_CONTROL_OUTLINE, false).rows(), new TreeMap<>()));
    }

    private static ParityProbe.Probe focusRun(ParitySession session, ParityConfigs.Config config, String id, List<String> sheets, String css, boolean legacyClasses) throws Exception {
        String frame = ParityFrames.frame(config.id() + "|" + id, config, sheets, css, focusBody(legacyClasses));
        return ParityFrames.measure(session, frame, EXTRA_PROPS, "focus " + config.id() + "|" + id);
    }

    /** The measurements are tens of thousands of rows; every suite in this JVM is run one after another, so this one lets go of them when it is done. */
    @AfterAll
    static void clear() {
        probe = null;
        List.of(PRESENT, REMOVED_SCOPED, REMOVED_FULL, SCOPED_FIDELITY, CONTROLS, FOCUS, FOCUS_CONTROLS).forEach(Map::clear);
        FOCUS_PROBES.clear();
        List.of(APPLIED_PRESENT, APPLIED_REMOVED, APPLIED_SCOPED, APPLIED_FOCUS, ROWS).forEach(Map::clear);
    }

    private static List<String> lines(Map<String, List<ParityProbe.Finding>> findings) {
        // A broken stylesheet makes tens of thousands of findings; the first few say what moved, and building a message from all of them runs out of heap.
        List<String> out = new ArrayList<>();
        long total = 0;
        for (List<ParityProbe.Finding> list : findings.values()) {
            for (ParityProbe.Finding f : list) {
                if (out.size() < 20) {
                    out.add(f.line());
                }
                total++;
            }
        }
        if (total > out.size()) {
            out.add("(" + (total - out.size()) + " more findings not shown)");
        }
        return out;
    }

    private static List<ParityProbe.Row> rows(ParityProbe.Probe p, String phase, String kind, String caseName, String layout) {
        return p.phase(phase).stream().filter(r -> r.kind().equals(kind) && r.variant().equals(caseName + "/" + layout)).toList();
    }

    // ---- tests ----

    @Test
    void theConfigurationsAreTheTwentyTwoThemeAndSkinCombinations() {
        assertEquals(22, CONFIGS.size());
    }

    @Test
    void controlledOracleWithTailwindPresentMatchesTheLegacyListItemInEveryConfigurationCaseAndWidth() {
        assertEquals(List.of(), lines(PRESENT));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheLegacyListItemGivenTheSameSurroundings() {
        assertEquals(List.of(), lines(REMOVED_SCOPED));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheFullLegacyRendering() {
        assertEquals(List.of(), lines(REMOVED_FULL));
    }

    @Test
    void theScopedLegacyStylesheetRendersTheLegacyListItemExactlyAsTheFullOneDoes() {
        assertEquals(List.of(), lines(SCOPED_FIDELITY));
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
    void theCasesAreCompleteEveryPartIsMeasuredAndTheNarrowWidthsReallyWrapTheLabel() {
        Map<String, Integer> kinds = new TreeMap<>();
        for (ParityProbe.Row r : probe.phase("light/none|L")) {
            kinds.merge(r.kind(), 1, Integer::sum);
        }
        for (String kind : List.of("list", "item", "row", "icon", "glyph", "body", "label", "description", "trailing", "chevron", "chevron-glyph", "trailing-content", "hover-swatch")) {
            assertTrue(kinds.getOrDefault(kind, 0) > 0, "no " + kind + " was measured: " + kinds);
        }
        assertTrue(ROWS.values().stream().allMatch(n -> n.equals(ROWS.get("light/none|L"))), "every configuration measures the same rows");
        // The long label is taller than one line (20px) at a narrow width, and a one-line label is not: the wrapping is really exercised.
        double wide = Double.parseDouble(probe.value(rows(probe, "light/none|L", "label", "long-label", "wide").get(0), "@h"));
        double narrow = Double.parseDouble(probe.value(rows(probe, "light/none|L", "label", "long-label", "very-narrow").get(0), "@h"));
        double oneLine = Double.parseDouble(probe.value(rows(probe, "light/none|L", "label", "label", "wide").get(0), "@h"));
        assertTrue(narrow > oneLine * 2, "the label wraps onto several lines when narrow: " + narrow + " vs " + oneLine);
        assertTrue(wide < narrow);
        // The text column never pushes the list wider than its box: it is the shrinking part, so the row stays inside the list.
        for (String layout : List.of("narrow", "very-narrow")) {
            double list = Double.parseDouble(probe.value(rows(probe, "light/none|L", "list", "long-everything", layout).get(0), "@w"));
            double row = Double.parseDouble(probe.value(rows(probe, "light/none|L", "row", "long-everything", layout).get(0), "@w"));
            assertTrue(row <= list, layout + ": the row (" + row + ") stays inside the list (" + list + ")");
        }
    }

    @Test
    void theIconModifiersGiveTheExactLegacySizesInsideTheRow() {
        for (String phase : List.of("light/none|L", "light/none|P", "light/none|R")) {
            ParityProbe.Row glyph = rows(probe, phase, "glyph", "link-icon-description", "wide").get(0);
            ParityProbe.Row chevron = rows(probe, phase, "chevron-glyph", "link-icon-description", "wide").get(0);
            assertEquals("20px", probe.value(glyph, "width"), phase);
            assertEquals("20px", probe.value(glyph, "height"), phase);
            assertEquals("16px", probe.value(chevron, "width"), phase);
            assertEquals("16px", probe.value(chevron, "height"), phase);
        }
    }

    @Test
    void aLinkRowIsAnAnchorWithAChevronAndAPlainRowIsADivWithoutOne() {
        for (String phase : List.of("light/none|L", "light/none|P")) {
            assertEquals(1, rows(probe, phase, "chevron", "link", "wide").size(), phase);
            assertEquals(0, rows(probe, phase, "chevron", "label", "wide").size(), phase);
            assertEquals(0, rows(probe, phase, "chevron", "full-plain", "wide").size(), phase);
            assertEquals("flex", probe.value(rows(probe, phase, "row", "link", "wide").get(0), "display"));
            assertEquals("flex", probe.value(rows(probe, phase, "row", "label", "wide").get(0), "display"));
            // A link row's colour and decoration are inherited / removed, like the plain row's.
            assertEquals(probe.value(rows(probe, phase, "row", "label", "wide").get(0), "color"), probe.value(rows(probe, phase, "row", "link", "wide").get(0), "color"), phase);
            assertEquals("none", probe.value(rows(probe, phase, "row", "link", "wide").get(0), "text-decoration-line"), phase);
        }
    }

    @Test
    void theColourAHoverPaintsMatchesTheLegacyHoverInEveryConfigurationBecauseItIsComparedThroughASwatch() {
        // The swatch rows are part of the comparisons above (kind hover-swatch); this states that they carry a real colour, different in the two themes.
        String light = probe.value(rows(probe, "light/none|P", "hover-swatch", "hover", "swatch").get(0), "background-color");
        String dark = probe.value(rows(probe, "dark/none|P", "hover-swatch", "hover", "swatch").get(0), "background-color");
        assertNotEquals("rgba(0, 0, 0, 0)", light);
        assertNotEquals("rgba(0, 0, 0, 0)", dark);
        assertNotEquals(light, dark, "the dark hover is the translucent slate-800");
        assertEquals(light, probe.value(rows(probe, "light/none|L", "hover-swatch", "hover", "swatch").get(0), "background-color"));
    }

    @Test
    void theKeyboardFocusRingIsTheLegacyInsetBlueRingAndTheBrowsersOwnOutlineIsOffInBothThemesAndModes() {
        assertEquals(List.of(), lines(FOCUS));
        for (ParityConfigs.Config c : FOCUS_CONFIGS) {
            ParityProbe.Probe migrated = FOCUS_PROBES.get(c.id() + "|P");
            ParityProbe.Row row = migrated.rows().stream().filter(r -> r.kind().equals("row")).findFirst().orElseThrow();
            String shadow = migrated.value(row, "box-shadow");
            assertTrue(shadow.contains("inset") && !shadow.equals("none"), c.id() + ": the focused row draws an inset ring: " + shadow);
            assertEquals("none", migrated.value(row, "outline-style"), c.id() + ": the browser's focus outline is off");
        }
    }

    @Test
    void theFocusControlsAreDetected() {
        assertTrue(FOCUS_CONTROLS.get("focus ring dropped").stream().anyMatch(f -> f.property().equals("box-shadow")), "a dropped ring must be detected in box-shadow");
        assertTrue(FOCUS_CONTROLS.get("browser outline comes back").stream().anyMatch(f -> f.property().equals("outline-style")), "the outline coming back must be detected in outline-style");
    }

    @Test
    void theOracleIsNotVacuousEveryLegacyClassOfTheItemIsCompiledInTheCumulativeAndScopedStylesheets() throws java.io.IOException {
        Path dir = Path.of(System.getProperty("ashlar.cascade.dir"));
        String oracle = Files.readString(dir.resolve("list-oracle.css"), StandardCharsets.UTF_8);
        String scoped = Files.readString(dir.resolve("list-legacy-scoped.css"), StandardCharsets.UTF_8);
        List<String> itemTokens = new ArrayList<>();
        for (String role : List.of("row", "row-link", "icon", "body", "label", "description", "trailing", "chevron", "hover-swatch")) {
            for (String token : legacy.classes(role).split("\\s+")) {
                itemTokens.add(token);
            }
        }
        // Hover, focus and dark variants are written by Tailwind as a pseudo-class or where() selector after the class, which compiles() accepts.
        assertEquals(List.of(), ParityOracle.uncompiled(oracle, itemTokens), "the List Item oracle does not compile these");
        assertEquals(List.of(), ParityOracle.uncompiled(scoped, itemTokens), "the scoped stylesheet does not compile these");
        for (String icon : List.of("inline-block", "shrink-0", "h-4", "w-4", "h-5", "w-5")) {
            assertTrue(ParityOracle.uncompiled(scoped, List.of(icon)).isEmpty(), "the scoped stylesheet lost the Icon's legacy class " + icon);
        }
    }

    @Test
    void normalizationsStayWithinTheApprovedRulesAndTailwindPresentNeedsNone() {
        assertEquals(Map.of(), APPLIED_PRESENT, "Tailwind present needs no normalization at all");
        assertEquals(Map.of(), APPLIED_SCOPED, "the scoped legacy rendering must match the full one without any normalization");
        // Tailwind removed: only H2, one use per border side where the migrated element has no border style (every legacy one is solid), in both removed comparisons.
        long none = 0;
        for (ParityConfigs.Config c : CONFIGS) {
            for (ParityProbe.Row legacyRow : owned(probe.phase(c.id() + "|L"))) {
                for (String side : List.of("top", "right", "bottom", "left")) {
                    assertEquals("solid", probe.value(legacyRow, "border-" + side + "-style"), "every legacy border style is solid (preflight)");
                }
            }
            for (ParityProbe.Row row : owned(probe.phase(c.id() + "|R"))) {
                for (String side : List.of("top", "right", "bottom", "left")) {
                    if (probe.value(row, "border-" + side + "-style").equals("none")) {
                        none++;
                    }
                }
            }
        }
        assertTrue(none > 0);
        assertEquals(Map.of("H2", (int) (2 * none)), APPLIED_REMOVED, "exactly H2, one use per borderless side, in both removed comparisons");
        // The focus comparisons use the same rule and only that: the focused frames hold one list each.
        assertTrue(APPLIED_FOCUS.keySet().stream().allMatch(k -> k.equals("H2") || k.equals("H1")), "the focus comparisons use only H1 (transparent ring layers) and H2: " + APPLIED_FOCUS);
    }

    // ---- what the real pages serve ----

    private static final Pattern ITEM = Pattern.compile("<li class=\"ash-list-item\">(.*?)</li>", Pattern.DOTALL);

    @Test
    void everyListItemTheRealPagesServeHasTheApprovedShapeAndDecorativeSemanticsAndNoTailwindOfItsOwn() throws Exception {
        ParitySession session = ParitySession.open("list-item-parity");
        HttpClient client = HttpClient.newHttpClient();
        List<String> paths = new ArrayList<>(List.of("/ashlar/", "/ashlar/components"));
        for (ComponentPage page : ComponentCatalog.PAGES) {
            paths.add("/ashlar/components/" + page.slug());
        }
        int items = 0;
        int links = 0;
        int icons = 0;
        int trailing = 0;
        List<String> problems = new ArrayList<>();
        try (RunningShowcase app = RunningShowcase.start(session.bootJar())) {
            for (String path : paths) {
                HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + path)).GET().build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                assertEquals(200, response.statusCode(), path);
                Matcher m = ITEM.matcher(response.body());
                while (m.find()) {
                    items++;
                    String body = m.group(1);
                    boolean link = Pattern.compile("^\\s*<a href=\"[^\"]*\" class=\"ash-list-item__row ash-list-item__row--link\">").matcher(body).find();
                    boolean plain = Pattern.compile("^\\s*<div class=\"ash-list-item__row\">").matcher(body).find();
                    if (link == plain) {
                        problems.add(path + ": a row that is neither a link row nor a plain row: " + body.strip().substring(0, Math.min(90, body.strip().length())));
                        continue;
                    }
                    int chevrons = count(body, "class=\"ash-list-item__chevron\" aria-hidden=\"true\"");
                    int iconWrappers = count(body, "class=\"ash-list-item__icon\" aria-hidden=\"true\"");
                    links += link ? 1 : 0;
                    icons += iconWrappers;
                    trailing += count(body, "class=\"ash-list-item__trailing\"");
                    if (chevrons != (link ? 1 : 0)) {
                        problems.add(path + ": the chevron must appear exactly on link rows (link=" + link + ", chevrons=" + chevrons + ")");
                    }
                    // Every decorative wrapper holds a decorative Icon: each svg is aria-hidden too, so the hidden count is twice the wrappers.
                    if (count(body, "aria-hidden=\"true\"") != 2 * (iconWrappers + chevrons)) {
                        problems.add(path + ": only the icon and chevron wrappers and their Icons are hidden");
                    }
                    if (count(body, "class=\"ash-list-item__label\"") != 1 || count(body, "class=\"ash-list-item__body\"") != 1 || count(body, "class=\"ash-list-item__description\"") > 1) {
                        problems.add(path + ": one label in one body, at most one description");
                    }
                    if (count(body, "class=\"ash-icon ash-icon--md\"") != iconWrappers || count(body, "class=\"ash-icon ash-icon--sm\"") != chevrons) {
                        problems.add(path + ": the Icons must use ash-icon--md (leading) and ash-icon--sm (chevron)");
                    }
                    if (Pattern.compile("class=\"[^\"]*(h-4|h-5|min-w-0|shrink-0 text-slate)").matcher(body).find()) {
                        problems.add(path + ": a List Item still carries a Tailwind utility of its own");
                    }
                }
            }
        }
        assertEquals(List.of(), problems);
        assertTrue(items >= 18 && links >= 6 && icons >= 6 && trailing >= 4, "items=" + items + " links=" + links + " icons=" + icons + " trailing=" + trailing);
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }
}
