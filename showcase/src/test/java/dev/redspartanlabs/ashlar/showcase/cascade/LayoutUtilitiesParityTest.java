package dev.redspartanlabs.ashlar.showcase.cascade;

import dev.redspartanlabs.ashlar.showcase.catalog.ComponentCatalog;
import dev.redspartanlabs.ashlar.showcase.catalog.ComponentPage;
import org.junit.jupiter.api.AfterAll;
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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The parity gates of the four layout utilities migrated together (Status Line, Form Actions, Key Value and Key Value List), measured in
 * real Chrome on the shared parity harness (unchanged). These four are JS-free and have no surface, so the harness's computed-style and
 * box measurement applies to them as it does to the List.
 *
 * <p>Evidence is a controlled oracle: the utilities' legacy Tailwind markup (compiled by the real Tailwind CLI) next to their migrated
 * markup, in the same browser, in four layouts for each of the 22 theme and skin configurations, with Tailwind present and with Tailwind
 * removed. Key Value is responsive, and a media query follows the width of the frame it is in, so every configuration is measured at a
 * narrow width (440px, stacked) and a wide one (900px, side by side), and the four theme and skin configurations that matter for
 * non-colour properties are also measured at 320px, at 639px and at 640px (the breakpoint is 40rem). Negative controls show each gate
 * can fail. Every utility on the real showcase pages is also compared by {@code ShowcaseRuntimeTest}.
 *
 * <p>Evidence is Chrome only. Computed-style and box parity is the visual evidence; there is no screenshot comparison.
 */
class LayoutUtilitiesParityTest {

    private static final String ORACLE = ParityFrames.generated("layout-utilities-oracle.css");
    private static final String SCOPED = ParityFrames.generated("layout-utilities-legacy-scoped.css");
    private static final List<ParityConfigs.Config> CONFIGS = ParityConfigs.all();
    private static final List<ParityConfigs.Config> EDGE_CONFIGS = ParityConfigs.reduced();
    private static final List<Integer> WIDTHS = List.of(440, 900);
    private static final List<Integer> EDGE_WIDTHS = List.of(320, 639, 640);
    private static final List<String> EXTRA_PROPS = List.of("font-family", "font-size", "font-weight", "line-height", "letter-spacing", "text-decoration-line",
            "text-align", "white-space", "overflow-wrap", "word-break", "flex-direction", "flex-wrap", "align-items", "justify-content", "align-content",
            "row-gap", "column-gap", "grid-template-columns", "grid-template-rows", "list-style-type");

    private record Layout(String name, String style, String siblingStyle) {
    }

    private static final List<Layout> LAYOUTS = List.of(
            new Layout("block", "box-sizing:border-box", ""),
            new Layout("narrow", "box-sizing:border-box;width:160px", ""),
            new Layout("column-flex-centered", "box-sizing:border-box;display:flex;flex-direction:column;align-items:center;width:300px", ""),
            new Layout("row-flex", "box-sizing:border-box;display:flex;flex-direction:row;align-items:flex-start;width:300px", "display:block;flex:none;width:60px"));

    /** The kinds of measured element that are the caller's own content, not the component's: preflight reached them too, so a full-legacy comparison leaves them out. */
    private static final Set<String> CALLER_KINDS = Set.of("raw");

    private static List<ParityProbe.Row> owned(List<ParityProbe.Row> rows) {
        return rows.stream().filter(r -> !CALLER_KINDS.contains(r.kind())).toList();
    }

    private static final String LONG_URL = "https://example.com/a/really/long/path/with-no-spaces/at-all/that-keeps-going-and-going/forever/and-ever/and-ever";
    private static final String LONG_TEXT = "The nightly import finished with 3 warnings and 0 errors, and the next run is scheduled for tomorrow morning before the office opens.";

    // ---- state of the measurement ----

    private static LayoutUtilitiesLegacyMarkup legacy;
    private static ParityProbe.Probe probe;
    private static final Map<String, List<ParityProbe.Finding>> PRESENT = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> REMOVED_SCOPED = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> REMOVED_FULL = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> SCOPED_FIDELITY = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> CONTROLS = new LinkedHashMap<>();
    private static final Map<String, Integer> APPLIED_PRESENT = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_REMOVED = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_SCOPED = new TreeMap<>();
    private static final Map<String, Integer> ROWS = new LinkedHashMap<>();

    // ---- markup ----

    private static String mark(String kind, String name) {
        return " data-parity=\"" + kind + "\" data-parity-case=\"" + name + "\"";
    }

    private static String cls(String role, boolean legacyClasses) {
        return legacyClasses ? legacy.classes(role) : LayoutUtilitiesLegacyMarkup.MIGRATED.get(role);
    }

    private static String raw(String tag, String text, String name, String style) {
        return "<" + tag + mark("raw", name) + (style.isEmpty() ? "" : " style=\"" + style + "\"") + (tag.equals("a") ? " href=\"#\"" : "") + ">" + text + "</" + tag + ">";
    }

    private static String statusLine(String name, String inner, boolean legacyClasses) {
        return "<p class=\"" + cls("status-line", legacyClasses) + "\"" + mark("status-line", name) + ">" + inner + "</p>";
    }

    private static String formActions(String name, String inner, boolean legacyClasses) {
        return "<div class=\"" + cls("form-actions", legacyClasses) + "\"" + mark("form-actions", name) + ">" + inner + "</div>";
    }

    private static String keyValueList(String name, String inner, boolean legacyClasses) {
        return "<dl class=\"" + cls("key-value-list", legacyClasses) + "\"" + mark("key-value-list", name) + ">" + inner + "</dl>";
    }

    private static String keyValue(String name, String label, String value, boolean legacyClasses) {
        return "<div class=\"" + cls("key-value", legacyClasses) + "\"" + mark("key-value", name) + ">"
                + "<dt class=\"" + cls("key-value-label", legacyClasses) + "\"" + mark("key-value-label", name) + ">" + label + "</dt>"
                + "<dd class=\"" + cls("key-value-value", legacyClasses) + "\"" + mark("key-value-value", name) + ">" + value + "</dd></div>";
    }

    private static final List<String> CASES = List.of("status-plain", "status-link", "status-long",
            "actions-1", "actions-2", "actions-3", "actions-empty", "actions-mixed",
            "kvl-1", "kvl-3", "kvl-empty",
            "kv-short", "kv-long-label", "kv-long-url", "kv-markup", "kv-tall-value", "kv-empty-value");

    private static String fixture(String name, boolean legacyClasses) {
        String button = "display:inline-block;width:80px;height:24px;";
        switch (name) {
            case "status-plain":
                return statusLine(name, "Last updated 4 minutes ago", legacyClasses);
            case "status-link":
                return statusLine(name, "Showing 24 of 128 records &middot; " + raw("a", "Clear filters", name + "-a", ""), legacyClasses);
            case "status-long":
                return statusLine(name, LONG_TEXT, legacyClasses);
            case "actions-1":
                return formActions(name, raw("button", "Save", name + "-a", button), legacyClasses);
            case "actions-2":
                return formActions(name, raw("button", "Cancel", name + "-a", button) + raw("button", "Save", name + "-b", button), legacyClasses);
            case "actions-3": {
                String wide = "display:inline-block;width:110px;height:24px;";
                return formActions(name, raw("button", "Back", name + "-a", wide) + raw("button", "Cancel", name + "-b", wide) + raw("button", "Save", name + "-c", wide), legacyClasses);
            }
            case "actions-empty":
                return formActions(name, "", legacyClasses);
            case "actions-mixed":
                return formActions(name, raw("span", "tall", name + "-a", "display:inline-block;width:60px;height:48px") + raw("button", "short", name + "-b", button), legacyClasses);
            case "kvl-1":
                return keyValueList(name, keyValue(name + "-1", "Plan", "Professional", legacyClasses), legacyClasses);
            case "kvl-3":
                return keyValueList(name, keyValue(name + "-1", "Plan", "Professional", legacyClasses) + keyValue(name + "-2", "Owner", "Dana Whitfield", legacyClasses)
                        + keyValue(name + "-3", "Created", "12 March 2026", legacyClasses), legacyClasses);
            case "kvl-empty":
                return keyValueList(name, "", legacyClasses);
            case "kv-short":
                return keyValueList(name + "-list", keyValue(name, "Plan", "Professional", legacyClasses), legacyClasses);
            case "kv-long-label":
                return keyValueList(name + "-list", keyValue(name, "A rather long label that has to wrap inside its column", "Value", legacyClasses), legacyClasses);
            case "kv-long-url":
                return keyValueList(name + "-list", keyValue(name, "Website", LONG_URL, legacyClasses), legacyClasses);
            case "kv-markup":
                return keyValueList(name + "-list", keyValue(name, "Status", raw("span", "Active", name + "-a", "display:inline-block;width:56px;height:20px") + " "
                        + raw("a", "View history", name + "-b", ""), legacyClasses), legacyClasses);
            case "kv-tall-value":
                return keyValueList(name + "-list", keyValue(name, "Total", raw("span", "$48.2K", name + "-a", "font-size:28px;line-height:1"), legacyClasses), legacyClasses);
            case "kv-empty-value":
                return keyValueList(name + "-list", keyValue(name, "Notes", "", legacyClasses), legacyClasses);
            default:
                throw new IllegalArgumentException(name);
        }
    }

    private static String markup(boolean legacyClasses) {
        StringBuilder sb = new StringBuilder();
        for (Layout layout : LAYOUTS) {
            for (String name : CASES) {
                String sibling = layout.siblingStyle().isEmpty() ? "" : "<span style=\"" + layout.siblingStyle() + "\">S</span>";
                sb.append("<div style=\"margin:4px 0\"><div data-layout=\"").append(layout.name()).append("\" style=\"").append(layout.style()).append("\">").append(sibling)
                        .append(fixture(name, legacyClasses)).append("</div></div>");
            }
        }
        return sb.toString();
    }

    // ---- frames ----

    private static String key(ParityConfigs.Config c, String phase, int width) {
        return c.id() + "|" + phase + "|" + width;
    }

    private static String sized(String frame, int width) {
        return frame.replace("width:440px", "width:" + width + "px");
    }

    private static String frameFor(ParityConfigs.Config c, String phase, int width, String environment, String migrated, String legacyMarkup) {
        String id = key(c, phase, width);
        switch (phase) {
            case "L":
                return sized(ParityFrames.frame(id, c, ParityFrames.with(ORACLE, ParityConfigs.SKIN_LINKS), "", legacyMarkup), width);
            case "P":
                return sized(ParityFrames.frame(id, c, ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), "", migrated), width);
            case "R":
                return sized(ParityFrames.frame(id, c, ParityFrames.with(ParityFrames.ASHLAR, ParityConfigs.SKIN_LINKS), environment, migrated), width);
            case "S":
                return sized(ParityFrames.frame(id, c, ParityFrames.with(SCOPED, ParityConfigs.SKIN_LINKS), environment, legacyMarkup), width);
            default:
                throw new IllegalArgumentException(phase);
        }
    }

    // ---- control experiments: each breaks the migrated utility in ONE way and must be detected ----

    private record Control(String name, String config, int width, String css, String expectedProperty) {
    }

    private static final List<Control> CONTROL_LIST = List.of(
            new Control("status line colour is wrong", "light/none", 900, ".ash-status-line{--ash-status-line-color:red}", "color"),
            new Control("status line text is larger", "light/none", 900, ".ash-status-line{font-size:1rem}", "font-size"),
            new Control("status line keeps the paragraph margin", "light/none", 900, ".ash-status-line{margin-block:1em}", "margin-top"),
            new Control("status line dark colour ignores the theme", "dark/none", 900, ".ash-status-line{--ash-status-line-color:var(--color-slate-500)}", "color"),
            new Control("status line colour ignores the skin", "light/civic", 900, ".ash-status-line{--ash-status-line-color:#64748b}", "color"),
            new Control("form actions start-aligned", "light/none", 900, ".ash-form-actions{justify-content:flex-start}", "justify-content"),
            new Control("form actions never wrap", "light/none", 440, ".ash-form-actions{flex-wrap:nowrap}", "flex-wrap"),
            new Control("form actions wrong gap", "light/none", 900, ".ash-form-actions{gap:8px}", "column-gap"),
            new Control("form actions stretch", "light/none", 900, ".ash-form-actions{align-items:stretch}", "align-items"),
            new Control("key value list wrong gap", "light/none", 900, ".ash-key-value-list{gap:4px}", "row-gap"),
            new Control("key value list is a row", "light/none", 900, ".ash-key-value-list{flex-direction:row}", "flex-direction"),
            new Control("key value list keeps the dl margin", "light/none", 900, ".ash-key-value-list{margin-block:1em}", "margin-top"),
            new Control("key value is never two columns", "light/none", 900, ".ash-key-value{grid-template-columns:minmax(0,1fr)}", "grid-template-columns"),
            new Control("key value breakpoint is late", "light/none", 640,
                    ".ash-key-value{grid-template-columns:minmax(0,1fr)}@media (min-width:41rem){.ash-key-value{grid-template-columns:minmax(8rem,12rem) minmax(0,1fr)}}", "grid-template-columns"),
            new Control("key value has no baseline alignment", "light/none", 900, ".ash-key-value{align-items:normal}", "align-items"),
            new Control("key value wrong column gap", "light/none", 900, ".ash-key-value{column-gap:8px}", "column-gap"),
            new Control("key value wrong label column", "light/none", 900, ".ash-key-value{grid-template-columns:8rem minmax(0,1fr)}", "grid-template-columns"),
            new Control("key value stacked row gap lost", "light/none", 440, ".ash-key-value{row-gap:0}", "row-gap"),
            new Control("key value value keeps the dd margin", "light/none", 900, ".ash-key-value__value{margin-inline-start:40px}", "margin-left"),
            new Control("key value label is not bold", "light/none", 900, ".ash-key-value__label{font-weight:400}", "font-weight"),
            new Control("key value value colour is wrong", "light/none", 900, ".ash-key-value__value{color:red}", "color"),
            new Control("key value label dark colour ignores the theme", "dark/none", 900, ".ash-key-value{--ash-key-value-label:var(--color-slate-500)}", "color"),
            new Control("key value value does not break long words", "light/none", 900, ".ash-key-value__value{overflow-wrap:normal}", "overflow-wrap"),
            new Control("key value value cannot shrink", "light/none", 900, ".ash-key-value__value{min-width:auto}", "min-width"));

    // ---- running ----

    @BeforeAll
    static void measure() throws Exception {
        ParitySession session = ParitySession.open("layout-utilities-parity");
        legacy = new LayoutUtilitiesLegacyMarkup();
        String environment = ParityFrames.environment(session.generatedDir());
        String migrated = markup(false);
        String legacyMarkup = markup(true);

        StringBuilder frames = new StringBuilder();
        for (ParityConfigs.Config c : CONFIGS) {
            for (int width : WIDTHS) {
                for (String phase : List.of("L", "P", "R", "S")) {
                    frames.append(frameFor(c, phase, width, environment, migrated, legacyMarkup));
                }
            }
        }
        for (ParityConfigs.Config c : EDGE_CONFIGS) {
            for (int width : EDGE_WIDTHS) {
                for (String phase : List.of("L", "P", "R", "S")) {
                    frames.append(frameFor(c, phase, width, environment, migrated, legacyMarkup));
                }
            }
        }
        for (Control control : CONTROL_LIST) {
            ParityConfigs.Config c = ParityConfigs.get(control.config());
            frames.append(sized(ParityFrames.frame("ctl:" + control.name() + "|P", c, ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), control.css(), migrated), control.width()));
        }
        probe = ParityFrames.measure(session, frames.toString(), EXTRA_PROPS, "layout utility frames");
        List<String> props = probe.props();
        for (ParityConfigs.Config c : CONFIGS) {
            for (int width : WIDTHS) {
                compareAll(props, c, width);
            }
        }
        for (ParityConfigs.Config c : EDGE_CONFIGS) {
            for (int width : EDGE_WIDTHS) {
                compareAll(props, c, width);
            }
        }
        for (Control control : CONTROL_LIST) {
            ParityConfigs.Config c = ParityConfigs.get(control.config());
            CONTROLS.put(control.name(), ParityProbe.compare("control: " + control.name(), props, probe.phase(key(c, "L", control.width())),
                    probe.phase("ctl:" + control.name() + "|P"), new TreeMap<>()));
        }
    }

    private static void compareAll(List<String> props, ParityConfigs.Config c, int width) {
        String at = c.id() + " @" + width;
        List<ParityProbe.Row> legacyRows = probe.phase(key(c, "L", width));
        ROWS.put(key(c, "L", width), legacyRows.size());
        PRESENT.put(at, ParityProbe.compare(at + " (Tailwind present)", props, legacyRows, probe.phase(key(c, "P", width)), APPLIED_PRESENT));
        REMOVED_SCOPED.put(at, ParityProbe.compare(at + " (Tailwind removed vs scoped legacy)", props, probe.phase(key(c, "S", width)), probe.phase(key(c, "R", width)), APPLIED_REMOVED));
        REMOVED_FULL.put(at, ParityProbe.compare(at + " (Tailwind removed vs full legacy, the utilities' own elements)", props, owned(legacyRows), owned(probe.phase(key(c, "R", width))), APPLIED_REMOVED));
        SCOPED_FIDELITY.put(at, ParityProbe.compare(at + " (scoped legacy vs full legacy, the utilities' own elements)", props, owned(legacyRows), owned(probe.phase(key(c, "S", width))), APPLIED_SCOPED));
    }

    /** The measurements are tens of thousands of rows; every suite in this JVM is run one after another, so this one lets go of them when it is done. */
    @AfterAll
    static void clear() {
        probe = null;
        List.of(PRESENT, REMOVED_SCOPED, REMOVED_FULL, SCOPED_FIDELITY, CONTROLS).forEach(Map::clear);
        List.of(APPLIED_PRESENT, APPLIED_REMOVED, APPLIED_SCOPED, ROWS).forEach(Map::clear);
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

    /** The rows of one kind and case in one layout, in document order. */
    private static List<ParityProbe.Row> rows(String phase, String kind, String caseName, String layout) {
        return probe.phase(phase).stream().filter(r -> r.kind().equals(kind) && r.variant().equals(caseName + "/" + layout)).toList();
    }

    private static List<String> values(String phase, String kind, String caseName, String layout, String property) {
        return rows(phase, kind, caseName, layout).stream().map(r -> probe.value(r, property)).toList();
    }

    private static String one(String phase, String kind, String caseName, String layout, String property) {
        List<String> found = values(phase, kind, caseName, layout, property);
        assertEquals(1, found.size(), phase + " " + kind + " " + caseName + "/" + layout);
        return found.get(0);
    }

    private static double number(String px) {
        return Double.parseDouble(px.replace("px", ""));
    }

    // ---- tests ----

    @Test
    void theConfigurationsAreTheTwentyTwoThemeAndSkinCombinationsAndTheWidthsAreMeasured() {
        assertEquals(22, CONFIGS.size());
        assertEquals(4, EDGE_CONFIGS.size());
        assertEquals(22 * WIDTHS.size() + 4 * EDGE_WIDTHS.size(), PRESENT.size());
    }

    @Test
    void controlledOracleWithTailwindPresentMatchesTheLegacyUtilitiesInEveryConfigurationCaseLayoutAndWidth() {
        assertEquals(List.of(), lines(PRESENT));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheLegacyUtilitiesGivenTheSameSurroundings() {
        assertEquals(List.of(), lines(REMOVED_SCOPED));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheFullLegacyRendering() {
        assertEquals(List.of(), lines(REMOVED_FULL));
    }

    @Test
    void theScopedLegacyStylesheetRendersTheLegacyUtilitiesExactlyAsTheFullOneDoes() {
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
    void keyValueStacksBelowTheBreakpointAndIsTwoColumnsFromItInEveryRendering() {
        for (String phase : List.of("light/none|L|", "light/none|P|", "light/none|R|", "light/none|S|")) {
            for (int width : List.of(320, 440, 639)) {
                String stacked = phase + width;
                assertEquals(1, one(stacked, "key-value", "kv-short", "block", "grid-template-columns").split(" ").length, stacked + " one column");
                assertTrue(number(one(stacked, "key-value-value", "kv-short", "block", "@y")) > number(one(stacked, "key-value-label", "kv-short", "block", "@y")),
                        stacked + ": the value is below its label");
                assertEquals("24px", one(stacked, "key-value", "kv-short", "block", "column-gap"), stacked + " column gap");
                assertEquals("4px", one(stacked, "key-value", "kv-short", "block", "row-gap"), stacked + " row gap");
                assertEquals("normal", one(stacked, "key-value", "kv-short", "block", "align-items"), stacked + " no baseline alignment while stacked");
            }
            for (int width : List.of(640, 900)) {
                String wide = phase + width;
                assertEquals(2, one(wide, "key-value", "kv-short", "block", "grid-template-columns").split(" ").length, wide + " two columns");
                assertEquals("baseline", one(wide, "key-value", "kv-short", "block", "align-items"), wide);
                assertEquals("0", one(wide, "key-value-label", "kv-short", "block", "@x"), wide + " label first");
                assertTrue(number(one(wide, "key-value-value", "kv-short", "block", "@x")) >= 128 + 24, wide + ": the value starts after the label column (>= 8rem) and the 1.5rem gap");
                assertTrue(number(one(wide, "key-value-label", "kv-short", "block", "@w")) >= 128 && number(one(wide, "key-value-label", "kv-short", "block", "@w")) <= 192, wide + " label column is 8rem to 12rem");
                assertEquals("24px", one(wide, "key-value", "kv-short", "block", "column-gap"), wide);
            }
        }
    }

    @Test
    void aTallValueMovesTheLabelToTheSharedBaselineOnlyWhenSideBySide() {
        for (String phase : List.of("light/none|L|", "light/none|P|", "light/none|R|")) {
            assertEquals("0", one(phase + 440, "key-value-label", "kv-tall-value", "block", "@y"), phase + " stacked: the label is at the top");
            assertTrue(number(one(phase + 900, "key-value-label", "kv-tall-value", "block", "@y")) > 0, phase + " side by side: the label sits on the value's baseline");
        }
    }

    @Test
    void theValueBreaksALongWordInsteadOfWideningTheRowAndTheLabelWraps() {
        for (String phase : List.of("light/none|L|", "light/none|P|", "light/none|R|")) {
            for (int width : List.of(440, 900)) {
                String at = phase + width;
                assertEquals("break-word", one(at, "key-value-value", "kv-long-url", "narrow", "overflow-wrap"), at);
                assertTrue(number(one(at, "key-value-value", "kv-long-url", "narrow", "@w")) <= 160, at + ": the value stays inside a 160px container");
                assertEquals("0px", one(at, "key-value-value", "kv-long-url", "narrow", "min-width"), at);
            }
        }
    }

    @Test
    void formActionsAreAnEndAlignedWrappingRowAndWrapAtNarrowWidths() {
        for (String phase : List.of("light/none|L|", "light/none|P|", "light/none|R|", "light/none|S|")) {
            String at = phase + 900;
            assertEquals("flex", one(at, "form-actions", "actions-2", "block", "display"), at);
            assertEquals("flex-end", one(at, "form-actions", "actions-2", "block", "justify-content"), at);
            assertEquals("wrap", one(at, "form-actions", "actions-2", "block", "flex-wrap"), at);
            assertEquals("center", one(at, "form-actions", "actions-2", "block", "align-items"), at);
            assertEquals("12px", one(at, "form-actions", "actions-2", "block", "column-gap"), at);
            List<ParityProbe.Row> two = rows(at, "raw", "actions-2-a", "block");
            List<ParityProbe.Row> last = rows(at, "raw", "actions-2-b", "block");
            assertEquals(1, two.size());
            assertTrue(number(probe.value(last.get(0), "@x")) > number(probe.value(two.get(0), "@x")), at + ": DOM order is kept, first control first");
            // Three 110px controls do not fit in a 160px container, so they wrap onto further rows.
            String narrow = phase + 900;
            List<String> tops = new ArrayList<>(values(narrow, "raw", "actions-3-a", "narrow", "@y"));
            tops.addAll(values(narrow, "raw", "actions-3-b", "narrow", "@y"));
            tops.addAll(values(narrow, "raw", "actions-3-c", "narrow", "@y"));
            assertEquals(3, tops.stream().distinct().count(), narrow + ": each control wraps onto its own row: " + tops);
            // Mixed heights are centred on the cross axis.
            double tall = number(one(at, "raw", "actions-mixed-a", "block", "@h"));
            double shortHeight = number(one(at, "raw", "actions-mixed-b", "block", "@h"));
            assertEquals((tall - shortHeight) / 2, number(one(at, "raw", "actions-mixed-b", "block", "@y")) - number(one(at, "raw", "actions-mixed-a", "block", "@y")), 0.01, at);
        }
    }

    @Test
    void keyValueListIsAFlexColumnWithTheEntriesTwelvePixelsApartAndNoBrowserMargin() {
        for (String phase : List.of("light/none|L|", "light/none|P|", "light/none|R|", "light/none|S|")) {
            String at = phase + 900;
            assertEquals("flex", one(at, "key-value-list", "kvl-3", "block", "display"), at);
            assertEquals("column", one(at, "key-value-list", "kvl-3", "block", "flex-direction"), at);
            assertEquals("12px", one(at, "key-value-list", "kvl-3", "block", "row-gap"), at);
            assertEquals("0px", one(at, "key-value-list", "kvl-3", "block", "margin-top"), at);
            assertEquals("0px", one(at, "key-value-list", "kvl-3", "block", "margin-bottom"), at);
            List<ParityProbe.Row> entries = rows(at, "key-value", "kvl-3-1", "block");
            assertEquals(1, entries.size(), at);
        }
    }

    @Test
    void theStatusLineIsMutedFourteenPixelTextWithoutABrowserMargin() {
        for (String phase : List.of("light/none|L|", "light/none|P|", "light/none|R|", "light/none|S|")) {
            String at = phase + 900;
            assertEquals("14px", one(at, "status-line", "status-plain", "block", "font-size"), at);
            assertEquals("20px", one(at, "status-line", "status-plain", "block", "line-height"), at);
            assertEquals("0px", one(at, "status-line", "status-plain", "block", "margin-top"), at);
            assertEquals("0px", one(at, "status-line", "status-plain", "block", "margin-bottom"), at);
        }
    }

    @Test
    void theCasesLayoutsAndWidthsAreCompleteAndTheLayoutIsReallyMeasuredNotAbsent() {
        List<ParityProbe.Row> rows = probe.phase("light/none|L|900");
        assertEquals((long) LAYOUTS.size() * 3, rows.stream().filter(r -> r.kind().equals("status-line")).count());
        assertEquals((long) LAYOUTS.size() * 5, rows.stream().filter(r -> r.kind().equals("form-actions")).count());
        assertEquals((long) LAYOUTS.size() * 3 + (long) LAYOUTS.size() * 6, rows.stream().filter(r -> r.kind().equals("key-value-list")).count());
        // Entries: kvl-1 (1) + kvl-3 (3) + six single-entry cases.
        assertEquals((long) LAYOUTS.size() * (1 + 3 + 6), rows.stream().filter(r -> r.kind().equals("key-value")).count());
        assertEquals(rows.stream().filter(r -> r.kind().equals("key-value")).count(), rows.stream().filter(r -> r.kind().equals("key-value-label")).count());
        assertEquals(rows.stream().filter(r -> r.kind().equals("key-value")).count(), rows.stream().filter(r -> r.kind().equals("key-value-value")).count());
        assertTrue(rows.stream().anyMatch(r -> r.kind().equals("raw")));
        for (String key : ROWS.keySet().stream().filter(k -> k.startsWith("light/none|L|")).toList()) {
            assertEquals(ROWS.get("light/none|L|900"), ROWS.get(key), "every width measures the same elements: " + key);
        }
        assertTrue(ROWS.values().stream().allMatch(n -> n > 0));
    }

    @Test
    void theOracleIsNotVacuousEveryLegacyClassIsCompiledInTheCumulativeAndScopedStylesheets() throws IOException {
        Path dir = Path.of(System.getProperty("ashlar.cascade.dir"));
        String oracle = Files.readString(dir.resolve("layout-utilities-oracle.css"), StandardCharsets.UTF_8);
        String scoped = Files.readString(dir.resolve("layout-utilities-legacy-scoped.css"), StandardCharsets.UTF_8);
        // The arbitrary-value class contains parentheses and a comma, which the class-escaping check does not model; it is checked by its rule's content.
        String arbitrary = "sm:grid-cols-[minmax(8rem,12rem)_minmax(0,1fr)]";
        List<String> simple = legacy.allTokens().stream().filter(t -> !t.equals(arbitrary)).toList();
        assertEquals(List.of(), ParityOracle.uncompiled(oracle, simple), "the oracle does not compile these");
        assertEquals(List.of(), ParityOracle.uncompiled(scoped, simple), "the scoped stylesheet does not compile these");
        for (String css : List.of(oracle, scoped)) {
            assertTrue(css.contains("minmax(8rem,12rem) minmax(0,1fr)") && css.contains("min-width:40rem"), "the responsive two-column rule is compiled");
        }
        List<String> earlier = new ArrayList<>();
        new BadgeLegacyMarkup().variants().values().forEach(classes -> earlier.addAll(Arrays.asList(classes.split("\\s+"))));
        earlier.addAll(new AlertLegacyMarkup().allTokens());
        earlier.addAll(new DividerLegacyMarkup().allTokens());
        earlier.addAll(new ProgressLegacyMarkup().allTokens());
        earlier.addAll(new EmptyStateLegacyMarkup().allTokens());
        earlier.addAll(new IconLegacyMarkup().allTokens());
        earlier.addAll(new SpinnerLegacyMarkup().allTokens());
        earlier.addAll(new SkeletonLegacyMarkup().allTokens());
        earlier.addAll(new ListLegacyMarkup().allTokens().stream().filter(t -> !List.of("divide-y", "divide-slate-200", "dark:divide-slate-700").contains(t)).toList());
        assertEquals(List.of(), ParityOracle.uncompiled(oracle, earlier), "the oracle lost earlier legacy classes");
        assertFalse(scoped.contains("html,:host"), "the scoped legacy stylesheet must not carry Tailwind's page-wide base");
    }

    @Test
    void normalizationsStayWithinTheApprovedRulesAndTailwindPresentNeedsNone() {
        assertEquals(Map.of(), APPLIED_PRESENT, "Tailwind present needs no normalization at all");
        assertEquals(Map.of(), APPLIED_SCOPED, "the scoped legacy rendering must match the full one without any normalization");
        // Tailwind removed: only H2 (a border style is invisible at width 0; preflight gave solid, the browser gives none).
        assertTrue(Set.of("H2").containsAll(APPLIED_REMOVED.keySet()), "unexpected normalization rule used: " + APPLIED_REMOVED);
    }

    // ---- what the real pages serve ----

    private static final Pattern KEY_VALUE_LIST = Pattern.compile("<dl class=\"ash-key-value-list\">(.*?)</dl>", Pattern.DOTALL);
    private static final Pattern KEY_VALUE = Pattern.compile("<div class=\"ash-key-value\">\\s*<dt class=\"ash-key-value__label\">.*?</dt>\\s*<dd class=\"ash-key-value__value\">.*?</dd>\\s*</div>", Pattern.DOTALL);

    @Test
    void everyUtilityTheRealPagesServeIsItsNativeElementWithOnlyAshlarsClassesAndEveryEntryIsADtDdPair() throws Exception {
        ParitySession session = ParitySession.open("layout-utilities-parity");
        HttpClient client = HttpClient.newHttpClient();
        List<String> paths = new ArrayList<>(List.of("/ashlar/", "/ashlar/components"));
        for (ComponentPage page : ComponentCatalog.PAGES) {
            paths.add("/ashlar/components/" + page.slug());
        }
        Map<String, Integer> totals = new TreeMap<>();
        Map<String, Integer> perPage = new TreeMap<>();
        List<String> problems = new ArrayList<>();
        try (RunningShowcase app = RunningShowcase.start(session.bootJar())) {
            for (String path : paths) {
                HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + path)).GET().build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                assertEquals(200, response.statusCode(), path);
                String body = response.body();
                for (String role : LayoutUtilitiesLegacyMarkup.ROLES) {
                    int n = LayoutUtilitiesLegacyMarkup.count(body, role);
                    totals.merge(role, n, Integer::sum);
                    if (n > 0) {
                        perPage.merge(role + " " + path, n, Integer::sum);
                    }
                }
                // Every class attribute that carries one of the four utilities' classes is a known part (the rewrite throws otherwise).
                legacy.rewrite(body);
                Matcher lists = KEY_VALUE_LIST.matcher(body);
                while (lists.find()) {
                    String rest = KEY_VALUE.matcher(lists.group(1)).replaceAll("").trim();
                    if (!rest.isEmpty()) {
                        problems.add(path + ": unexpected content directly inside a key-value list: " + rest.substring(0, Math.min(80, rest.length())));
                    }
                }
                assertEquals(LayoutUtilitiesLegacyMarkup.count(body, "key-value"), KEY_VALUE.matcher(body).results().count(), path + ": every entry is a div holding a dt then a dd");
            }
        }
        assertEquals(List.of(), problems);
        for (String role : LayoutUtilitiesLegacyMarkup.ROLES) {
            assertTrue(totals.get(role) > 0, "no " + role + " rendered on any page: " + totals);
        }
        assertEquals(totals.get("key-value"), totals.get("key-value-label"));
        assertEquals(totals.get("key-value"), totals.get("key-value-value"));
        assertTrue(perPage.containsKey("status-line /ashlar/components/status-line"), perPage.toString());
        assertTrue(perPage.containsKey("form-actions /ashlar/components/form-actions"), perPage.toString());
        assertTrue(perPage.containsKey("key-value-list /ashlar/components/key-value-list"), perPage.toString());
        assertTrue(perPage.containsKey("key-value /ashlar/components/key-value-list"), perPage.toString());
    }
}
