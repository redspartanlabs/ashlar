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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The parity gates of the four page composition utilities migrated together (Page Shell, Page Header, Page Section and Footer), measured in
 * real Chrome on the shared parity harness (unchanged). They are JS-free, so the harness's computed-style and box measurement applies.
 *
 * <p>Evidence is a controlled oracle: the utilities' legacy Tailwind markup (compiled by the real Tailwind CLI) next to their migrated
 * markup, in the same browser, in three layouts for each of the 22 theme and skin configurations, with Tailwind present and with Tailwind
 * removed. The shell's padding and the header's reflow follow the width of the frame they are in (a media query does), so every
 * configuration is measured at 900px, and the four theme and skin configurations that matter for non-colour properties are also
 * measured at 320px, 440px, at 639px and 640px (the 40rem breakpoint), at 1023px and 1024px (the 64rem breakpoint) and at 1300px (wider than the
 * 72rem cap). Negative controls show each gate can fail. Heading levels and landmarks are native elements in the templates (pinned by
 * {@code PageCompositionContractTest}) and are checked again on every real showcase page below. Every utility on the real showcase pages is
 * also compared by {@code ShowcaseRuntimeTest}.
 *
 * <p>Evidence is Chrome only. Computed-style and box parity is the visual evidence; there is no screenshot comparison.
 */
class PageCompositionParityTest {

    private static final String ORACLE = ParityFrames.generated("page-composition-oracle.css");
    private static final String SCOPED = ParityFrames.generated("page-composition-legacy-scoped.css");
    private static final List<ParityConfigs.Config> CONFIGS = ParityConfigs.all();
    private static final List<ParityConfigs.Config> EDGE_CONFIGS = ParityConfigs.reduced();
    private static final List<Integer> WIDTHS = List.of(900);
    private static final List<Integer> EDGE_WIDTHS = List.of(320, 440, 639, 640, 1023, 1024, 1300);
    private static final List<String> EXTRA_PROPS = List.of("font-family", "font-size", "font-weight", "line-height", "letter-spacing", "text-decoration-line",
            "text-align", "white-space", "overflow-wrap", "word-break", "flex-direction", "flex-wrap", "align-items", "justify-content", "align-content",
            "row-gap", "column-gap", "border-top-color", "padding-inline-start", "margin-inline-start");

    private record Layout(String name, String style, String siblingStyle) {
    }

    private static final List<Layout> LAYOUTS = List.of(
            new Layout("block", "box-sizing:border-box", ""),
            new Layout("narrow", "box-sizing:border-box;width:300px", ""),
            new Layout("column-flex-centered", "box-sizing:border-box;display:flex;flex-direction:column;align-items:center;width:600px", ""));

    /** The kinds of measured element that are the caller's own content, not the component's: preflight reached them too, so a full-legacy comparison leaves them out. */
    private static final Set<String> CALLER_KINDS = Set.of("raw");

    private static List<ParityProbe.Row> owned(List<ParityProbe.Row> rows) {
        return rows.stream().filter(r -> !CALLER_KINDS.contains(r.kind())).toList();
    }

    private static final String LONG_TITLE = "Quarterly infrastructure capacity review and the migration plan for the remaining regional clusters";
    private static final String LONG_TEXT = "The nightly import finished with 3 warnings and 0 errors, and the next run is scheduled for tomorrow morning before the office opens.";

    // ---- state of the measurement ----

    private static PageCompositionLegacyMarkup legacy;
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
    /** The one known Tailwind-removed difference: see {@link #withoutKnownBoxSizing}. Findings dropped, by comparison. */
    private static final Map<String, Integer> KNOWN_BOX_SIZING = new TreeMap<>();

    // ---- markup ----

    private static String mark(String kind, String name) {
        return " data-parity=\"" + kind + "\" data-parity-case=\"" + name + "\"";
    }

    private static String cls(String role, boolean legacyClasses) {
        return legacyClasses ? legacy.classes(role) : PageCompositionLegacyMarkup.MIGRATED.get(role);
    }

    private static String raw(String tag, String text, String name, String style) {
        return "<" + tag + mark("raw", name) + (style.isEmpty() ? "" : " style=\"" + style + "\"") + (tag.equals("a") ? " href=\"#\"" : "") + ">" + text + "</" + tag + ">";
    }

    private static String pageShell(String name, String inner, boolean l) {
        return "<div class=\"" + cls("page-shell", l) + "\"" + mark("page-shell", name) + ">" + inner + "</div>";
    }

    private static String pageHeader(String name, String title, String description, String actions, boolean l) {
        return "<header class=\"" + cls("page-header", l) + "\"" + mark("page-header", name) + ">"
                + "<div class=\"" + cls("page-header-heading", l) + "\"" + mark("page-header-heading", name) + ">"
                + "<h1 class=\"" + cls("page-header-title", l) + "\"" + mark("page-header-title", name) + ">" + title + "</h1>"
                + (description.isEmpty() ? "" : "<p class=\"" + cls("page-header-description", l) + "\"" + mark("page-header-description", name) + ">" + description + "</p>")
                + "</div>"
                + (actions == null ? "" : "<div class=\"" + cls("page-header-actions", l) + "\"" + mark("page-header-actions", name) + ">" + actions + "</div>")
                + "</header>";
    }

    private static String pageSection(String name, String title, String description, String content, boolean l) {
        return "<section class=\"" + cls("page-section", l) + "\"" + mark("page-section", name) + ">"
                + "<div" + mark("page-section-heading", name) + ">"
                + "<h2 class=\"" + cls("page-section-title", l) + "\"" + mark("page-section-title", name) + ">" + title + "</h2>"
                + (description.isEmpty() ? "" : "<p class=\"" + cls("page-section-description", l) + "\"" + mark("page-section-description", name) + ">" + description + "</p>")
                + "</div>" + content + "</section>";
    }

    private static String footer(String name, String inner, boolean l) {
        return "<footer class=\"" + cls("footer", l) + "\"" + mark("footer", name) + "><div class=\"" + cls("footer-inner", l) + "\"" + mark("footer-inner", name) + ">"
                + inner + "</div></footer>";
    }

    private static final List<String> CASES = List.of("shell-empty", "shell-blocks",
            "header-desc", "header-actions", "header-long", "header-wrap",
            "section-desc", "section-content", "section-long",
            "footer-text", "footer-link", "footer-long", "footer-empty");

    private static String fixture(String name, boolean l) {
        String button = "display:inline-block;width:80px;height:24px;";
        switch (name) {
            case "shell-empty":
                return pageShell(name, "", l);
            case "shell-blocks":
                return pageShell(name, pageHeader(name + "-h", "Accounts", "Everyone with access", raw("button", "New", name + "-a", button), l)
                        + pageSection(name + "-s", "Recent", "Last 30 days", raw("div", "content", name + "-c", "height:40px"), l)
                        + raw("div", "another block", name + "-d", "height:30px"), l);
            case "header-desc":
                return pageHeader(name, "Accounts", "Everyone with access to the workspace", null, l);
            case "header-actions":
                return pageHeader(name, "Accounts", "Everyone with access", raw("button", "Export", name + "-a", button) + raw("button", "New", name + "-b", button), l);
            case "header-long":
                return pageHeader(name, LONG_TITLE, LONG_TEXT, raw("button", "New", name + "-a", button), l);
            case "header-wrap": {
                String wide = "display:inline-block;width:110px;height:24px;";
                return pageHeader(name, "Accounts", "Everyone", raw("button", "A", name + "-a", wide) + raw("button", "B", name + "-b", wide)
                        + raw("button", "C", name + "-c", wide) + raw("button", "D", name + "-d", wide), l);
            }
            case "section-desc":
                return pageSection(name, "Recent activity", "Everything that changed in the last 30 days", raw("div", "content", name + "-c", "height:40px"), l);
            case "section-content":
                return pageSection(name, "Recent activity", "Last 30 days", raw("div", "tall content", name + "-c", "height:120px") + raw("div", "second", name + "-d", "height:20px"), l);
            case "section-long":
                return pageSection(name, LONG_TITLE, LONG_TEXT, raw("div", "content", name + "-c", "height:40px"), l);
            case "footer-text":
                return footer(name, "&copy; 2026 Ashlar", l);
            case "footer-link":
                return footer(name, "&copy; 2026 Ashlar &middot; " + raw("a", "Privacy", name + "-a", ""), l);
            case "footer-long":
                return footer(name, LONG_TEXT + " " + LONG_TEXT, l);
            case "footer-empty":
                return footer(name, "", l);
            default:
                throw new IllegalArgumentException(name);
        }
    }

    private static String markup(boolean l) {
        StringBuilder sb = new StringBuilder();
        for (Layout layout : LAYOUTS) {
            for (String name : CASES) {
                String sibling = layout.siblingStyle().isEmpty() ? "" : "<span style=\"" + layout.siblingStyle() + "\">S</span>";
                sb.append("<div style=\"margin:4px 0\"><div data-layout=\"").append(layout.name()).append("\" style=\"").append(layout.style()).append("\">").append(sibling)
                        .append(fixture(name, l)).append("</div></div>");
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
            new Control("shell small side padding is wrong", "light/none", 440, ".ash-page-shell{padding-inline:24px}", "padding-left"),
            new Control("shell sm padding step is missing", "light/none", 900, ".ash-page-shell{padding-inline:16px}", "padding-left"),
            new Control("shell lg breakpoint is late", "light/none", 1024,
                    ".ash-page-shell{padding-inline:24px}@media (min-width:65rem){.ash-page-shell{padding-inline:32px}}", "padding-left"),
            new Control("shell is not capped", "light/none", 1300, ".ash-page-shell{max-width:none}", "max-width"),
            new Control("shell cap is wrong", "light/none", 1300, ".ash-page-shell{max-width:70rem}", "max-width"),
            new Control("shell is not centred", "light/none", 1300, ".ash-page-shell{margin-inline:0}", "margin-left"),
            new Control("shell wrong block gap", "light/none", 900, ".ash-page-shell{gap:2rem}", "row-gap"),
            new Control("shell wrong vertical padding", "light/none", 900, ".ash-page-shell{padding-block:1rem}", "padding-top"),
            new Control("header title is larger", "light/none", 900, ".ash-page-header__title{font-size:1.5rem}", "font-size"),
            new Control("header title is not semibold", "light/none", 900, ".ash-page-header__title{font-weight:500}", "font-weight"),
            new Control("header title keeps the heading margin", "light/none", 900, ".ash-page-header__title{margin-block:0.67em}", "margin-top"),
            new Control("header title dark colour ignores the theme", "dark/none", 900, ".ash-page-header{--ash-page-header-title:var(--color-slate-900)}", "color"),
            new Control("header description loses its top margin", "light/none", 900, ".ash-page-header__description{margin-top:0}", "margin-top"),
            new Control("header description colour is wrong", "light/none", 900, ".ash-page-header__description{color:red}", "color"),
            new Control("header never becomes a row", "light/none", 900, ".ash-page-header{flex-direction:column}", "flex-direction"),
            new Control("header breakpoint is late", "light/none", 640,
                    ".ash-page-header{flex-direction:column;align-items:normal;justify-content:normal}@media (min-width:41rem){.ash-page-header{flex-direction:row;align-items:flex-start;justify-content:space-between}}",
                    "flex-direction"),
            new Control("header actions are not pushed to the end", "light/none", 900, ".ash-page-header{justify-content:flex-start}", "justify-content"),
            new Control("header actions are not top-aligned", "light/none", 900, ".ash-page-header{align-items:center}", "align-items"),
            new Control("header heading cannot shrink", "light/none", 900, ".ash-page-header__heading{min-width:auto}", "min-width"),
            new Control("header actions never wrap", "light/none", 440, ".ash-page-header__actions{flex-wrap:nowrap}", "flex-wrap"),
            new Control("header actions wrong gap", "light/none", 900, ".ash-page-header__actions{gap:8px}", "column-gap"),
            new Control("header wrong gap between heading and actions", "light/none", 440, ".ash-page-header{gap:8px}", "row-gap"),
            new Control("section wrong gap", "light/none", 900, ".ash-page-section{gap:8px}", "row-gap"),
            new Control("section title is not semibold", "light/none", 900, ".ash-page-section__title{font-weight:700}", "font-weight"),
            new Control("section title has the wrong size", "light/none", 900, ".ash-page-section__title{font-size:1.25rem}", "font-size"),
            new Control("section title keeps the heading margin", "light/none", 900, ".ash-page-section__title{margin-block:0.83em}", "margin-top"),
            new Control("section title dark colour ignores the theme", "dark/none", 900, ".ash-page-section{--ash-page-section-title:var(--color-slate-900)}", "color"),
            new Control("header description dark colour ignores the theme", "dark/none", 900, ".ash-page-header{--ash-page-header-description:var(--color-slate-500)}", "color"),
            new Control("footer rule dark colour ignores the theme", "dark/none", 900, ".ash-footer{--ash-footer-border:var(--color-slate-200)}", "border-top-color"),
            new Control("section description colour ignores the theme", "dark/none", 900, ".ash-page-section{--ash-page-section-description:var(--color-slate-500)}", "color"),
            new Control("footer rule is two pixels", "light/none", 900, ".ash-footer{border-top-width:2px}", "border-top-width"),
            new Control("footer rule colour is wrong", "light/none", 900, ".ash-footer{--ash-footer-border:red}", "border-top-color"),
            new Control("footer has the wrong vertical padding", "light/none", 900, ".ash-footer{padding-block:1rem}", "padding-top"),
            new Control("footer text is larger", "light/none", 900, ".ash-footer{font-size:1rem}", "font-size"),
            new Control("footer text dark colour ignores the theme", "dark/none", 900, ".ash-footer{--ash-footer-color:var(--color-slate-500)}", "color"),
            new Control("footer inner is not capped", "light/none", 1300, ".ash-footer__inner{max-width:none}", "max-width"),
            new Control("footer inner has no side padding", "light/none", 900, ".ash-footer__inner{padding-inline:0}", "padding-left"));

    // ---- running ----

    @BeforeAll
    static void measure() throws Exception {
        ParitySession session = ParitySession.open("page-composition-parity");
        legacy = new PageCompositionLegacyMarkup();
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
        probe = ParityFrames.measure(session, frames.toString(), EXTRA_PROPS, "page composition frames");
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

    /**
     * A Page Section's heading block is a plain <div> with no class, before and after the migration, so it has no style of its own to
     * migrate. With Tailwind present it is border-box because preflight sets box-sizing on every element; with Tailwind removed nothing
     * does.
     *
     * <p>Why this is not a defect to fix: Ashlar has deliberately NO global reset (base.css and the README say so), so its base layer
     * only reaches elements that carry an ash- class, and a classless element is exactly as content-box as every other plain element on a
     * page without Tailwind. Giving the div a class would change the template's markup, and a universal or element reset would change
     * every consumer's page; neither is in scope or wanted. In every supported configuration (Tailwind present, which the 43 components not
     * yet migrated still require) the element is border-box and the "Tailwind present" comparison matches exactly.
     *
     * <p>Why it is harmless: box-sizing only changes a box that has padding, border or an explicit size, and nothing gives this div any (the
     * caller cannot add a class or style to it; it is internal markup). {@link #theClasslessHeadingBlockHasNothingForBoxSizingToChange}
     * proves that in every configuration and width, and its boxes (@x @y @w @h) must still match. This removes exactly that one finding,
     * on exactly that element and property, and counts it so a test can pin it.
     */
    private static List<ParityProbe.Finding> withoutKnownBoxSizing(String comparison, List<ParityProbe.Finding> findings) {
        List<ParityProbe.Finding> kept = new ArrayList<>();
        for (ParityProbe.Finding f : findings) {
            if (f.property().equals("box-sizing") && f.where().contains(" page-section-heading ") && f.expected().equals("border-box") && f.actual().equals("content-box")) {
                KNOWN_BOX_SIZING.merge(comparison, 1, Integer::sum);
            } else {
                kept.add(f);
            }
        }
        return kept;
    }

    private static void compareAll(List<String> props, ParityConfigs.Config c, int width) {
        String at = c.id() + " @" + width;
        List<ParityProbe.Row> legacyRows = probe.phase(key(c, "L", width));
        ROWS.put(key(c, "L", width), legacyRows.size());
        PRESENT.put(at, ParityProbe.compare(at + " (Tailwind present)", props, legacyRows, probe.phase(key(c, "P", width)), APPLIED_PRESENT));
        REMOVED_SCOPED.put(at, withoutKnownBoxSizing("scoped", ParityProbe.compare(at + " (Tailwind removed vs scoped legacy)", props, probe.phase(key(c, "S", width)), probe.phase(key(c, "R", width)), APPLIED_REMOVED)));
        REMOVED_FULL.put(at, withoutKnownBoxSizing("full", ParityProbe.compare(at + " (Tailwind removed vs full legacy, the utilities' own elements)", props, owned(legacyRows), owned(probe.phase(key(c, "R", width))), APPLIED_REMOVED)));
        SCOPED_FIDELITY.put(at, ParityProbe.compare(at + " (scoped legacy vs full legacy, the utilities' own elements)", props, owned(legacyRows), owned(probe.phase(key(c, "S", width))), APPLIED_SCOPED));
    }

    /** The measurements are tens of thousands of rows; every suite in this JVM is run one after another, so this one lets go of them when it is done. */
    @AfterAll
    static void clear() {
        probe = null;
        List.of(PRESENT, REMOVED_SCOPED, REMOVED_FULL, SCOPED_FIDELITY, CONTROLS).forEach(Map::clear);
        List.of(APPLIED_PRESENT, APPLIED_REMOVED, APPLIED_SCOPED, ROWS, KNOWN_BOX_SIZING).forEach(Map::clear);
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

    private static String one(String phase, String kind, String caseName, String layout, String property) {
        List<ParityProbe.Row> found = rows(phase, kind, caseName, layout);
        assertEquals(1, found.size(), phase + " " + kind + " " + caseName + "/" + layout);
        return probe.value(found.get(0), property);
    }

    private static double number(String px) {
        return Double.parseDouble(px.replace("px", ""));
    }

    private static final List<String> RENDERINGS = List.of("light/none|L|", "light/none|P|", "light/none|R|", "light/none|S|");

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
    void thePageShellSidePaddingStepsAtFortyRemAndSixtyFourRemAndIsCappedAndCenteredAboveTheCap() {
        for (String phase : RENDERINGS) {
            for (int width : List.of(320, 440, 639)) {
                assertEquals("16px", one(phase + width, "page-shell", "shell-empty", "block", "padding-left"), phase + width);
            }
            for (int width : List.of(640, 900, 1023)) {
                assertEquals("24px", one(phase + width, "page-shell", "shell-empty", "block", "padding-left"), phase + width);
            }
            for (int width : List.of(1024, 1300)) {
                assertEquals("32px", one(phase + width, "page-shell", "shell-empty", "block", "padding-left"), phase + width);
            }
            String wide = phase + 1300;
            assertEquals("1152px", one(wide, "page-shell", "shell-empty", "block", "max-width"), wide);
            assertEquals("1152", one(wide, "page-shell", "shell-empty", "block", "@w"), wide + ": capped at 72rem");
            assertTrue(number(one(wide, "page-shell", "shell-empty", "block", "@x")) > 0, wide + ": centred");
            assertEquals("32px", one(wide, "page-shell", "shell-empty", "block", "padding-top"), wide);
            // The blocks inside are 40px apart (gap-10) and the shell does not restyle them.
            String at = phase + 900;
            double header = number(one(at, "page-header", "shell-blocks-h", "block", "@y"));
            double section = number(one(at, "page-section", "shell-blocks-s", "block", "@y"));
            double headerHeight = number(one(at, "page-header", "shell-blocks-h", "block", "@h"));
            assertEquals(40, section - header - headerHeight, 0.01, at + ": the shell separates its blocks by 2.5rem");
        }
    }

    @Test
    void thePageHeaderStacksBelowTheBreakpointAndIsARowWithTheActionsAtTheEndFromIt() {
        for (String phase : RENDERINGS) {
            for (int width : List.of(320, 440, 639)) {
                String at = phase + width;
                assertEquals("column", one(at, "page-header", "header-actions", "block", "flex-direction"), at);
                assertTrue(number(one(at, "page-header-actions", "header-actions", "block", "@y")) > number(one(at, "page-header-heading", "header-actions", "block", "@y")),
                        at + ": the actions are below the heading");
            }
            for (int width : List.of(640, 900, 1024)) {
                String at = phase + width;
                assertEquals("row", one(at, "page-header", "header-actions", "block", "flex-direction"), at);
                assertEquals("flex-start", one(at, "page-header", "header-actions", "block", "align-items"), at);
                assertEquals("space-between", one(at, "page-header", "header-actions", "block", "justify-content"), at);
                assertEquals(one(at, "page-header-heading", "header-actions", "block", "@y"), one(at, "page-header-actions", "header-actions", "block", "@y"), at + ": top aligned");
                assertTrue(number(one(at, "page-header-actions", "header-actions", "block", "@x")) > number(one(at, "page-header-heading", "header-actions", "block", "@x")), at);
            }
        }
    }

    @Test
    void headingsAndDescriptionsHaveTheirTailwindSizesWeightsLineHeightsAndNoBrowserMargin() {
        for (String phase : RENDERINGS) {
            String at = phase + 900;
            assertEquals("20px", one(at, "page-header-title", "header-desc", "block", "font-size"), at);
            assertEquals("600", one(at, "page-header-title", "header-desc", "block", "font-weight"), at);
            assertEquals("28px", one(at, "page-header-title", "header-desc", "block", "line-height"), at);
            assertEquals("0px", one(at, "page-header-title", "header-desc", "block", "margin-top"), at);
            assertEquals("0px", one(at, "page-header-title", "header-desc", "block", "margin-bottom"), at);
            assertEquals("4px", one(at, "page-header-description", "header-desc", "block", "margin-top"), at);
            assertEquals("14px", one(at, "page-header-description", "header-desc", "block", "font-size"), at);
            assertEquals("20px", one(at, "page-header-description", "header-desc", "block", "line-height"), at);
            assertEquals("18px", one(at, "page-section-title", "section-desc", "block", "font-size"), at);
            assertEquals("600", one(at, "page-section-title", "section-desc", "block", "font-weight"), at);
            assertEquals("28px", one(at, "page-section-title", "section-desc", "block", "line-height"), at);
            assertEquals("0px", one(at, "page-section-title", "section-desc", "block", "margin-top"), at);
            assertEquals("4px", one(at, "page-section-description", "section-desc", "block", "margin-top"), at);
            assertEquals("16px", one(at, "page-section", "section-desc", "block", "row-gap"), at);
        }
    }

    @Test
    void aLongTitleWrapsInsideItsColumnAndTheActionsWrapOntoFurtherRows() {
        for (String phase : RENDERINGS) {
            String at = phase + 900;
            assertEquals("0px", one(at, "page-header-heading", "header-long", "narrow", "min-width"), at);
            assertTrue(number(one(at, "page-header-heading", "header-long", "narrow", "@w")) <= 300, at + ": the heading stays inside a 300px container");
            Set<String> tops = new TreeSet<>();
            for (String button : List.of("a", "b", "c", "d")) {
                tops.add(one(at, "raw", "header-wrap-" + button, "narrow", "@y"));
            }
            assertTrue(tops.size() > 1, at + ": four 110px actions wrap in a 300px container: " + tops);
        }
    }

    @Test
    void theFooterIsARuleOnTopWithTheShellsPaddingAndAnInnerElementCappedAtTheShellsWidth() {
        for (String phase : RENDERINGS) {
            String at = phase + 900;
            assertEquals("1px", one(at, "footer", "footer-text", "block", "border-top-width"), at);
            assertEquals("0px", one(at, "footer", "footer-text", "block", "border-bottom-width"), at);
            assertEquals("solid", one(at, "footer", "footer-text", "block", "border-top-style"), at);
            assertEquals("32px", one(at, "footer", "footer-text", "block", "padding-top"), at);
            assertEquals("32px", one(at, "footer", "footer-text", "block", "padding-bottom"), at);
            assertEquals("14px", one(at, "footer", "footer-text", "block", "font-size"), at);
            assertEquals("20px", one(at, "footer", "footer-text", "block", "line-height"), at);
            assertEquals("16px", one(at, "footer-inner", "footer-text", "block", "padding-left"), at);
            assertEquals("1152px", one(at, "footer-inner", "footer-text", "block", "max-width"), at);
            String wide = phase + 1300;
            assertEquals("1152", one(wide, "footer-inner", "footer-text", "block", "@w"), wide + ": capped at 72rem");
            assertTrue(number(one(wide, "footer-inner", "footer-text", "block", "@x")) > 0, wide + ": centred");
        }
    }

    @Test
    void theCasesLayoutsAndWidthsAreCompleteAndTheLayoutIsReallyMeasuredNotAbsent() {
        List<ParityProbe.Row> rows = probe.phase("light/none|L|900");
        // Page Shell: the three shell cases, and the header and section inside shell-blocks.
        assertEquals((long) LAYOUTS.size() * 2, rows.stream().filter(r -> r.kind().equals("page-shell")).count());
        assertEquals((long) LAYOUTS.size() * (4 + 1), rows.stream().filter(r -> r.kind().equals("page-header")).count());
        assertEquals((long) LAYOUTS.size() * (3 + 1), rows.stream().filter(r -> r.kind().equals("page-section")).count());
        assertEquals((long) LAYOUTS.size() * 4, rows.stream().filter(r -> r.kind().equals("footer")).count());
        assertEquals(rows.stream().filter(r -> r.kind().equals("footer")).count(), rows.stream().filter(r -> r.kind().equals("footer-inner")).count());
        assertEquals(rows.stream().filter(r -> r.kind().equals("page-header")).count(), rows.stream().filter(r -> r.kind().equals("page-header-title")).count());
        assertEquals(rows.stream().filter(r -> r.kind().equals("page-section")).count(), rows.stream().filter(r -> r.kind().equals("page-section-title")).count());
        assertTrue(rows.stream().anyMatch(r -> r.kind().equals("raw")));
        for (String key : ROWS.keySet().stream().filter(k -> k.startsWith("light/none|L|")).toList()) {
            assertEquals(ROWS.get("light/none|L|900"), ROWS.get(key), "every width measures the same elements: " + key);
        }
        assertTrue(ROWS.values().stream().allMatch(n -> n > 0));
    }

    @Test
    void theOracleIsNotVacuousEveryLegacyClassIsCompiledInTheCumulativeAndScopedStylesheets() throws IOException {
        Path dir = Path.of(System.getProperty("ashlar.cascade.dir"));
        String oracle = Files.readString(dir.resolve("page-composition-oracle.css"), StandardCharsets.UTF_8);
        String scoped = Files.readString(dir.resolve("page-composition-legacy-scoped.css"), StandardCharsets.UTF_8);
        List<String> tokens = new ArrayList<>(legacy.allTokens());
        assertEquals(List.of(), ParityOracle.uncompiled(oracle, tokens), "the oracle does not compile these");
        assertEquals(List.of(), ParityOracle.uncompiled(scoped, tokens), "the scoped stylesheet does not compile these");
        for (String css : List.of(oracle, scoped)) {
            assertTrue(css.contains("min-width:40rem") && css.contains("min-width:64rem"), "the responsive rules are compiled");
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
        earlier.addAll(new LayoutUtilitiesLegacyMarkup().allTokens().stream().filter(t -> !t.contains("[")).toList());
        assertEquals(List.of(), ParityOracle.uncompiled(oracle, earlier), "the oracle lost earlier legacy classes");
        assertFalse(scoped.contains("html,:host"), "the scoped legacy stylesheet must not carry Tailwind's page-wide base");
    }

    @Test
    void theOnlyTailwindRemovedDifferenceIsTheClasslessHeadingBlocksBoxSizingAndItsBoxesAreIdentical() {
        int blocksPerFrame = LAYOUTS.size() * 4; // section-desc, section-content, section-long and the one inside shell-blocks, in each layout
        int perComparison = (CONFIGS.size() * WIDTHS.size() + EDGE_CONFIGS.size() * EDGE_WIDTHS.size()) * blocksPerFrame;
        assertEquals(Map.of("scoped", perComparison, "full", perComparison), KNOWN_BOX_SIZING, "one content-box finding per classless heading block, in each removed comparison");
        for (String phase : List.of("light/none|L|900", "light/none|R|900")) {
            List<ParityProbe.Row> blocks = probe.phase(phase).stream().filter(r -> r.kind().equals("page-section-heading")).toList();
            assertEquals(blocksPerFrame, blocks.size(), phase);
        }
        List<ParityProbe.Row> legacyBlocks = probe.phase("light/none|L|900").stream().filter(r -> r.kind().equals("page-section-heading")).toList();
        List<ParityProbe.Row> migratedBlocks = probe.phase("light/none|R|900").stream().filter(r -> r.kind().equals("page-section-heading")).toList();
        for (int i = 0; i < legacyBlocks.size(); i++) {
            for (String box : List.of("@x", "@y", "@w", "@h", "padding-left", "border-left-width")) {
                assertEquals(probe.value(legacyBlocks.get(i), box), probe.value(migratedBlocks.get(i), box), "heading block #" + i + " " + box);
            }
        }
    }

    @Test
    void theClasslessHeadingBlockHasNothingForBoxSizingToChange() {
        List<String> inert = List.of("padding-top", "padding-right", "padding-bottom", "padding-left", "border-top-width", "border-right-width",
                "border-bottom-width", "border-left-width", "min-width", "min-height");
        int checked = 0;
        List<String> problems = new ArrayList<>();
        for (String phaseKey : ROWS.keySet().stream().filter(k -> k.contains("|L|")).toList()) {
            String removedKey = phaseKey.replace("|L|", "|R|");
            List<ParityProbe.Row> legacyBlocks = probe.phase(phaseKey).stream().filter(r -> r.kind().equals("page-section-heading")).toList();
            List<ParityProbe.Row> blocks = probe.phase(removedKey).stream().filter(r -> r.kind().equals("page-section-heading")).toList();
            assertEquals(legacyBlocks.size(), blocks.size(), removedKey);
            for (int i = 0; i < blocks.size(); i++) {
                for (String property : inert) {
                    String value = probe.value(blocks.get(i), property);
                    if (!(value.equals("0px") || value.equals("auto")) || !value.equals(probe.value(legacyBlocks.get(i), property))) {
                        problems.add(removedKey + " #" + i + " " + property + "=" + value + " (legacy " + probe.value(legacyBlocks.get(i), property) + ")");
                    }
                }
                for (String property : List.of("max-width", "max-height")) {
                    if (!probe.value(blocks.get(i), property).equals("none")) {
                        problems.add(removedKey + " #" + i + " " + property + "=" + probe.value(blocks.get(i), property));
                    }
                }
                checked++;
            }
        }
        assertEquals(List.of(), problems.subList(0, Math.min(problems.size(), 10)));
        assertEquals(50 * LAYOUTS.size() * 4, checked, "every configuration and width, every layout, every heading block");
    }

    @Test
    void normalizationsStayWithinTheApprovedRulesAndTailwindPresentNeedsNone() {
        assertEquals(Map.of(), APPLIED_PRESENT, "Tailwind present needs no normalization at all");
        assertEquals(Map.of(), APPLIED_SCOPED, "the scoped legacy rendering must match the full one without any normalization");
        // Tailwind removed: only H2 (a border style is invisible at width 0; preflight gave solid, the browser gives none).
        assertTrue(Set.of("H2").containsAll(APPLIED_REMOVED.keySet()), "unexpected normalization rule used: " + APPLIED_REMOVED);
    }

    // ---- what the real pages serve ----

    private static final Pattern SECTION_WITH_H2 = Pattern.compile("<section class=\"ash-page-section\">\\s*<div>\\s*<h2 class=\"ash-page-section__title\">");
    private static final Pattern HEADER_WITH_H1 = Pattern.compile("<header class=\"ash-page-header\">\\s*<div class=\"ash-page-header__heading\">\\s*<h1 class=\"ash-page-header__title\">");
    private static final Pattern FOOTER_WITH_INNER = Pattern.compile("<footer class=\"ash-footer\">\\s*<div class=\"ash-footer__inner\">");

    /** The pages that render these utilities as examples, so they have several of them. */
    private static final Set<String> DEMONSTRATES_THE_UTILITIES = Set.of("page-header", "page-section", "page-shell", "footer");
    /** Pages whose own example hand-writes an extra h1 (not a Page Header). */
    private static final Map<String, Integer> HAND_WRITTEN_H1 = Map.of("breadcrumbs", 1);

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }

    @Test
    void everyUtilityTheRealPagesServeKeepsItsNativeElementAndHeadingLevelAndEveryComponentPageHasOneH1AndOneFooter() throws Exception {
        ParitySession session = ParitySession.open("page-composition-parity");
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
                for (String role : PageCompositionLegacyMarkup.ROLES) {
                    int n = PageCompositionLegacyMarkup.count(body, role);
                    totals.merge(role, n, Integer::sum);
                    if (n > 0) {
                        perPage.merge(role + " " + path, n, Integer::sum);
                    }
                }
                // Every class attribute that carries one of the four utilities' classes is a known part (the rewrite throws otherwise).
                legacy.rewrite(body);
                int sections = PageCompositionLegacyMarkup.count(body, "page-section");
                int headers = PageCompositionLegacyMarkup.count(body, "page-header");
                int footers = PageCompositionLegacyMarkup.count(body, "footer");
                if (SECTION_WITH_H2.matcher(body).results().count() != sections) {
                    problems.add(path + ": a Page Section is not a section whose first child holds its h2");
                }
                if (HEADER_WITH_H1.matcher(body).results().count() != headers) {
                    problems.add(path + ": a Page Header is not a header whose heading block holds its h1");
                }
                if (FOOTER_WITH_INNER.matcher(body).results().count() != footers) {
                    problems.add(path + ": a Footer is not a footer holding its inner element");
                }
                if (count(body, "<footer") != footers) {
                    problems.add(path + ": a footer element that is not the Footer utility: " + count(body, "<footer") + " vs " + footers);
                }
                if (path.startsWith("/ashlar/components/")) {
                    String slug = path.substring("/ashlar/components/".length());
                    if (count(body, "<main") != 1) {
                        problems.add(path + ": a component page must have exactly one main landmark: " + count(body, "<main"));
                    }
                    if (headers < 1 || footers < 1 || count(body, "<h1") < headers) {
                        problems.add(path + ": a component page has the layout's Page Header (an h1) and Footer: headers=" + headers + " footers=" + footers + " h1=" + count(body, "<h1"));
                    }
                    // Most pages have exactly one of each. The four utilities' own pages render several as examples, and the Breadcrumbs example
                    // hand-writes one more h1 of its own (before this migration too).
                    if (!DEMONSTRATES_THE_UTILITIES.contains(slug)) {
                        int expectedH1 = HAND_WRITTEN_H1.getOrDefault(slug, 0) + 1;
                        if (headers != 1 || footers != 1 || count(body, "<h1") != expectedH1) {
                            problems.add(path + ": expected one Page Header, one Footer and " + expectedH1 + " h1: headers=" + headers + " footers=" + footers + " h1=" + count(body, "<h1"));
                        }
                    }
                    if (count(body, "<h2 class=\"ash-page-section__title\">") != sections) {
                        problems.add(path + ": every Page Section title is an h2");
                    }
                }
            }
        }
        assertEquals(List.of(), problems);
        for (String role : List.of("page-header", "page-header-heading", "page-header-title", "page-section", "page-section-title", "footer", "footer-inner")) {
            assertTrue(totals.getOrDefault(role, 0) > 0, "no " + role + " rendered on any page: " + totals);
        }
        assertEquals(totals.get("page-header"), totals.get("page-header-title"));
        assertEquals(totals.get("page-section"), totals.get("page-section-title"));
        assertEquals(totals.get("footer"), totals.get("footer-inner"));
        assertTrue(perPage.containsKey("page-section /ashlar/components/page-section"), perPage.toString());
        assertTrue(perPage.containsKey("page-header /ashlar/components/page-header"), perPage.toString());
        assertTrue(perPage.containsKey("footer /ashlar/components/footer"), perPage.toString());
    }
}
