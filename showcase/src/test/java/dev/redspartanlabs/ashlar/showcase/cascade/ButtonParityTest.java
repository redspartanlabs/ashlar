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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The resting-state parity gates of Button, measured in real Chrome on the shared parity harness (unchanged). The states a measurement cannot
 * hold (hover, active, focus-visible) are measured by {@link ButtonStateParityTest}, which forces them through the browser's own DevTools
 * protocol; the script behaviour is {@link ButtonScriptTest}.
 *
 * <p>Evidence is a controlled oracle: the Buttons' legacy Tailwind markup (compiled by the real Tailwind CLI, from a verbatim copy of the former
 * {@code ButtonStyles}) next to their migrated markup, in the same browser, in each of the 22 theme and skin configurations, with Tailwind
 * present and with Tailwind removed. The legacy markup is the migrated markup rewritten by {@link ButtonLegacyMarkup}, so both sides are the same
 * Buttons in the same state, and carry the same {@code data-btn} and {@code data-btn-variant} hooks, which is what the skins select.
 *
 * <p>The cases are the eight variants in several sizes, the five sizes, the icon-only, leading-icon, trailing-icon and full-width forms, and
 * the disabled, loading, pressed (as an {@code aria-pressed} toggle and as the {@code ash-btn--pressed} modifier) states. A loading spinner is
 * paused at its first frame on both sides, because its box moves while it turns and two frames cannot be measured at the same instant; its
 * animation duration, iteration count and timing function are still compared. Negative controls show each gate can fail, including controls
 * that remove the hooks the skins select (Civic's weight, Foundry's ghost exclusion).
 *
 * <p>Evidence is Chrome only. Computed-style and box parity is the visual evidence; there is no screenshot comparison.
 */
class ButtonParityTest {

    private static final String ORACLE = ParityFrames.generated("button-oracle.css");
    private static final String SCOPED = ParityFrames.generated("button-legacy-scoped.css");
    private static final List<ParityConfigs.Config> CONFIGS = ParityConfigs.all();
    private static final List<String> EXTRA_PROPS = List.of("font-family", "font-size", "font-weight", "font-style", "line-height", "letter-spacing", "text-decoration-line",
            "text-underline-offset", "text-align", "text-transform", "white-space", "cursor", "user-select", "pointer-events", "transition-duration", "transition-delay",
            "transition-timing-function", "animation-duration", "animation-iteration-count", "animation-timing-function", "animation-play-state", "row-gap", "column-gap",
            "align-items", "justify-content", "flex-direction", "flex-wrap", "outline-style", "clip-path", "overflow-wrap", "appearance", "background-image");

    /** Applied to every frame: a turning spinner is measured at its first frame, on both sides. */
    private static final String PAUSE = "*{animation-play-state:paused !important}";

    private record Spec(String name, String variant, String size, boolean iconOnly, boolean block, String pressed, boolean disabled, boolean loading, String icon, String text,
                        String layout) {
    }

    private static final String LONG = "A rather long button label that cannot fit";

    private static final List<Spec> SPECS = new ArrayList<>();

    private static void add(String name, String variant, String size, boolean iconOnly, boolean block, String pressed, boolean disabled, boolean loading, String icon, String text,
                            String layout) {
        SPECS.add(new Spec(name, variant, size, iconOnly, block, pressed, disabled, loading, icon, text, layout));
    }

    static {
        List<String> variants = List.of("primary", "secondary", "success", "warning", "danger", "info", "ghost", "link");
        for (String v : variants) {
            for (String size : List.of("xs", "md", "xl")) {
                add(v + "-" + size, v, size, false, false, "none", false, false, "none", "Save", "block");
            }
        }
        add("primary-sm", "primary", "sm", false, false, "none", false, false, "none", "Save", "block");
        add("primary-lg", "primary", "lg", false, false, "none", false, false, "none", "Save", "block");
        for (String size : List.of("xs", "sm", "md", "lg", "xl")) {
            add("primary-icon-" + size, "primary", size, true, false, "none", false, false, "leading", "Add", "block");
        }
        for (String v : List.of("secondary", "ghost", "link")) {
            add(v + "-icon-md", v, "md", true, false, "none", false, false, "leading", "Add", "block");
        }
        add("primary-leading", "primary", "md", false, false, "none", false, false, "leading", "Add item", "block");
        add("primary-trailing", "primary", "md", false, false, "none", false, false, "trailing", "Next", "block");
        add("secondary-trailing-sm", "secondary", "sm", false, false, "none", false, false, "trailing", "Next", "block");
        add("danger-leading-xl", "danger", "xl", false, false, "none", false, false, "leading", "Delete", "block");
        add("primary-block", "primary", "md", false, true, "none", false, false, "none", "Continue", "block");
        add("primary-block-narrow", "primary", "md", false, true, "none", false, false, "none", "Continue", "narrow");
        add("secondary-icon-block", "secondary", "md", true, true, "none", false, false, "leading", "Add", "narrow");
        add("primary-long-narrow", "primary", "md", false, false, "none", false, false, "none", LONG, "narrow");
        for (String v : variants) {
            add(v + "-disabled", v, "md", false, false, "none", true, false, "none", "Save", "block");
            add(v + "-loading", v, "md", false, false, "none", false, true, "none", "Save", "block");
            add(v + "-pressed", v, "md", false, false, "aria-true", false, false, "none", "Pin", "block");
            add(v + "-pressed-class", v, "md", false, false, "class", false, false, "none", "Pin", "block");
        }
        add("primary-loading-icon", "primary", "md", false, false, "none", false, true, "leading", "Save", "block");
        add("secondary-loading-trailing", "secondary", "md", false, false, "none", false, true, "trailing", "Save", "block");
        add("primary-loading-icon-only", "primary", "md", true, false, "none", false, true, "leading", "Add", "block");
        add("secondary-toggle-off", "secondary", "md", false, false, "aria-false", false, false, "none", "Pin", "block");
        add("ghost-pressed-disabled", "ghost", "md", false, false, "aria-true", true, false, "none", "Pin", "block");
        add("link-pressed-loading", "link", "md", false, false, "aria-true", false, true, "none", "Pin", "block");
        add("secondary-pressed-icon", "secondary", "md", false, false, "aria-true", false, false, "leading", "Pin", "block");
    }

    // ---- state of the measurement ----

    private static ButtonLegacyMarkup buttonLegacy;
    private static IconLegacyMarkup iconLegacy;
    private static FileUploadLegacyMarkup fileUploadLegacy;
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
    private static String migratedForCounting;

    // ---- markup (what button.jte renders, written out) ----

    private static String mark(String kind, String name) {
        return " data-parity=\"" + kind + "\" data-parity-case=\"" + name + "\"";
    }

    private static String iconClass(String size) {
        return switch (size) {
            case "xs" -> "ash-icon--xs";
            case "sm", "md" -> "ash-icon--sm";
            case "lg" -> "ash-icon--md";
            default -> "ash-icon--lg";
        };
    }

    private static String svg(Spec s) {
        return "<svg class=\"ash-icon " + iconClass(s.size()) + "\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" "
                + "stroke-linejoin=\"round\" aria-hidden=\"true\"" + mark("button-svg", s.name()) + "><path d=\"M5 12h14\"/></svg>";
    }

    private static String iconSpan(Spec s) {
        return "<span data-btn-icon class=\"ash-btn__icon\" aria-hidden=\"true\"" + mark("button-icon", s.name()) + ">" + svg(s) + "</span>";
    }

    static String migrated(Spec s) {
        String classes = "ash-btn ash-btn--" + s.variant() + " ash-btn--" + s.size() + (s.iconOnly() ? " ash-btn--icon" : "") + (s.block() ? " ash-btn--block" : "")
                + (s.pressed().equals("class") ? " ash-btn--pressed" : "");
        StringBuilder sb = new StringBuilder("<button type=\"button\" class=\"" + classes + "\" data-btn data-btn-variant=\"" + s.variant() + "\"");
        if (s.disabled()) {
            sb.append(" disabled");
        }
        if (s.loading()) {
            sb.append(" aria-busy=\"true\" aria-disabled=\"true\"");
        }
        if (s.iconOnly()) {
            sb.append(" aria-label=\"").append(s.text()).append("\"");
        }
        if (s.pressed().startsWith("aria-")) {
            sb.append(" data-btn-toggle aria-pressed=\"").append(s.pressed().equals("aria-true")).append("\" data-btn-default-text=\"").append(s.text())
                    .append("\" data-btn-pressed-text=\"").append(s.text()).append("\"");
        }
        sb.append(mark("button", s.name())).append(">");
        sb.append("<span data-btn-spinner class=\"ash-btn__spinner\" aria-hidden=\"true\"").append(mark("button-spinner", s.name())).append("></span>");
        if (!s.icon().equals("none") && (s.iconOnly() || s.icon().equals("leading"))) {
            sb.append(iconSpan(s));
        }
        if (s.iconOnly()) {
            sb.append("<span class=\"ash-sr-only ash-btn__label\"").append(mark("button-label", s.name())).append(">").append(s.text()).append("</span>");
        } else {
            sb.append("<span data-btn-label class=\"ash-btn__label\"").append(mark("button-label", s.name())).append(">").append(s.text()).append("</span>");
        }
        if (!s.icon().equals("none") && !s.iconOnly() && s.icon().equals("trailing")) {
            sb.append(iconSpan(s));
        }
        sb.append("<span class=\"ash-sr-only ash-btn__status\" data-btn-status aria-live=\"polite\"").append(mark("button-status", s.name())).append(">")
                .append(s.loading() ? "Loading" : "").append("</span></button>");
        return sb.toString();
    }

    private static String layoutStyle(String layout) {
        return layout.equals("narrow") ? "box-sizing:border-box;width:140px" : "box-sizing:border-box";
    }

    private static String markup(boolean legacyMarkup) {
        StringBuilder sb = new StringBuilder();
        for (Spec s : SPECS) {
            sb.append("<div style=\"margin:4px 0\"><div data-layout=\"").append(s.layout()).append("\" style=\"").append(layoutStyle(s.layout())).append("\">").append(migrated(s))
                    .append("</div></div>");
        }
        // File Upload's "Choose File": a decorative span that carries the Button's own classes (ButtonStyles.classes) and is not a <button>.
        sb.append("<div style=\"margin:4px 0\"><div data-layout=\"block\" style=\"").append(layoutStyle("block")).append("\"><span class=\"ash-btn ash-btn--secondary ash-btn--sm ash-file-upload__choose\"")
                .append(mark("button-decorative", "file-upload-choose")).append(">Choose File</span></div></div>");
        String html = sb.toString();
        return legacyMarkup ? iconLegacy.rewrite(buttonLegacy.rewrite(fileUploadLegacy.rewrite(html))) : html;
    }

    // ---- frames ----

    private static String key(ParityConfigs.Config c, String phase) {
        return c.id() + "|" + phase;
    }

    private static String frameFor(ParityConfigs.Config c, String phase, String environment, String migrated, String legacyMarkup) {
        String id = key(c, phase);
        switch (phase) {
            case "L":
                return ParityFrames.frame(id, c, ParityFrames.with(ORACLE, ParityConfigs.SKIN_LINKS), PAUSE, legacyMarkup);
            case "P":
                return ParityFrames.frame(id, c, ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), PAUSE, migrated);
            case "R":
                return ParityFrames.frame(id, c, ParityFrames.with(ParityFrames.ASHLAR, ParityConfigs.SKIN_LINKS), environment + PAUSE, migrated);
            case "S":
                return ParityFrames.frame(id, c, ParityFrames.with(SCOPED, ParityConfigs.SKIN_LINKS), environment + PAUSE, legacyMarkup);
            default:
                throw new IllegalArgumentException(phase);
        }
    }

    // ---- control experiments: each breaks the migrated Buttons in ONE way and must be detected ----

    private record Control(String name, String config, String css, String expectedProperty, String withoutHook) {
        Control(String name, String config, String css, String expectedProperty) {
            this(name, config, css, expectedProperty, "");
        }
    }

    private static final List<Control> CONTROL_LIST = List.of(
            new Control("primary background is wrong", "light/none", ".ash-btn--primary{--ash-btn-bg:red}", "background-color"),
            new Control("secondary dark background ignores the theme", "dark/none", ".ash-btn--secondary{--ash-btn-bg:var(--color-white)}", "background-color"),
            new Control("secondary dark border ignores the theme", "dark/none", ".ash-btn--secondary{--ash-btn-border:var(--color-slate-300)}", "border-top-color"),
            new Control("ghost text colour is wrong", "light/none", ".ash-btn--ghost{--ash-btn-fg:red}", "color"),
            new Control("link text colour is wrong", "light/none", ".ash-btn--link{--ash-btn-fg:red}", "color"),
            new Control("warning is not amber", "light/none", ".ash-btn--warning{--ash-btn-bg:var(--color-blue-600)}", "background-color"),
            new Control("radius is wrong", "light/none", ".ash-btn{border-radius:4px}", "border-top-left-radius"),
            new Control("radius ignores a skin's --radius-md", "light/foundry", ".ash-btn{border-radius:0.375rem}", "border-top-left-radius"),
            new Control("medium is not 40px tall", "light/none", ".ash-btn--md{height:36px}", "height"),
            new Control("extra large padding is wrong", "light/none", ".ash-btn--xl{padding-inline:1.5rem}", "padding-left"),
            new Control("icon-only is not a square", "light/none", ".ash-btn--icon{width:auto}", "width"),
            new Control("block is not full width", "light/none", ".ash-btn--block{width:auto}", "width"),
            new Control("font weight is wrong", "light/none", ".ash-btn{font-weight:600}", "font-weight"),
            new Control("gap is wrong", "light/none", ".ash-btn{gap:4px}", "column-gap"),
            new Control("line height is wrong", "light/none", ".ash-btn--lg{--ash-btn-leading:1.4}", "line-height"),
            new Control("disabled is not dimmed", "light/none", ".ash-btn:disabled{opacity:1}", "opacity"),
            new Control("loading is not dimmed", "light/none", ".ash-btn[aria-disabled=\"true\"]{opacity:1}", "opacity"),
            new Control("cursor is wrong", "light/none", ".ash-btn{cursor:default}", "cursor"),
            new Control("transition is slower", "light/none", ".ash-btn{transition-duration:300ms}", "transition-duration"),
            new Control("the spinner is hidden while loading", "light/none", ".ash-btn[aria-busy=\"true\"] .ash-btn__spinner{display:none}", "display"),
            new Control("the icon stays visible while loading", "light/none", ".ash-btn[aria-busy=\"true\"] .ash-btn__icon{display:inline}", "display"),
            new Control("the spinner is the wrong size", "light/none", ".ash-btn__spinner{width:20px}", "width"),
            new Control("the spinner does not spin", "light/none", ".ash-btn__spinner{animation-duration:2s}", "animation-duration"),
            new Control("pressed background is wrong", "light/none", ".ash-btn--primary:is(.ash-btn--pressed,[aria-pressed=\"true\"]){--ash-btn-bg:red}", "background-color"),
            new Control("pressed loses its inset shadow", "light/none", ".ash-btn--primary:is(.ash-btn--pressed,[aria-pressed=\"true\"]){--ash-btn-inset:0 0 #0000}", "box-shadow"),
            new Control("a skin's flat shadow is not honoured", "light/axiom", "html[data-skin] .ash-btn.ash-btn{--ash-btn-shadow:0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1)}",
                    "box-shadow"),
            new Control("pressed link loses its underline", "light/none", ".ash-btn--link:is(.ash-btn--pressed,[aria-pressed=\"true\"]){text-decoration-line:none}",
                    "text-decoration-line"),
            new Control("link underline offset is wrong", "light/none", ".ash-btn--link{text-underline-offset:2px}", "text-underline-offset"),
            new Control("the screen-reader label is visible", "light/none", ".ash-btn__label.ash-sr-only{position:static}", "position"),
            // The hooks: without them the skins stop reaching the Buttons.
            new Control("Civic's weight needs data-btn", "light/civic", "", "font-weight", " data-btn "),
            new Control("Foundry's ghost exclusion needs data-btn-variant", "light/foundry", "", "box-shadow", " data-btn-variant="));

    // ---- running ----

    @BeforeAll
    static void measure() throws Exception {
        ParitySession session = ParitySession.open("button-parity");
        buttonLegacy = new ButtonLegacyMarkup();
        iconLegacy = new IconLegacyMarkup();
        fileUploadLegacy = new FileUploadLegacyMarkup();
        String environment = ParityFrames.environment(session.generatedDir());
        String migrated = markup(false);
        String legacyMarkup = markup(true);
        migratedForCounting = migrated;

        StringBuilder frames = new StringBuilder();
        for (ParityConfigs.Config c : CONFIGS) {
            for (String phase : List.of("L", "P", "R", "S")) {
                frames.append(frameFor(c, phase, environment, migrated, legacyMarkup));
            }
        }
        for (Control control : CONTROL_LIST) {
            ParityConfigs.Config c = ParityConfigs.get(control.config());
            String body = control.withoutHook().isEmpty() ? migrated : migrated.replace(control.withoutHook(), control.withoutHook().equals(" data-btn ") ? " data-x " : " data-x=");
            frames.append(ParityFrames.frame("ctl:" + control.name() + "|P", c, ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), PAUSE + control.css(), body));
        }
        probe = ParityFrames.measure(session, frames.toString(), EXTRA_PROPS, "button frames");
        List<String> props = probe.props();
        for (ParityConfigs.Config c : CONFIGS) {
            compareAll(props, c);
        }
        for (Control control : CONTROL_LIST) {
            ParityConfigs.Config c = ParityConfigs.get(control.config());
            CONTROLS.put(control.name(), ParityProbe.compare("control: " + control.name(), props, probe.phase(key(c, "L")), probe.phase("ctl:" + control.name() + "|P"), new TreeMap<>()));
        }
    }

    /**
     * File Upload's decorative span carries the Button classes and is not a Button element. Its `pointer-events-none` is File Upload's own Tailwind
     * utility (absent, of course, when Tailwind is removed), so it is left out of the Tailwind-removed comparisons and measured on its own below. Its
     * appearance is otherwise compared like any Button, in every configuration with Tailwind present, including Foundry.
     */
    private static List<ParityProbe.Row> withoutDecorative(List<ParityProbe.Row> rows) {
        return rows.stream().filter(r -> !r.kind().equals("button-decorative")).toList();
    }

    private static void compareAll(List<String> props, ParityConfigs.Config c) {
        String at = c.id();
        List<ParityProbe.Row> legacyRows = probe.phase(key(c, "L"));
        ROWS.put(key(c, "L"), legacyRows.size());
        PRESENT.put(at, ParityProbe.compare(at + " (Tailwind present)", props, legacyRows, probe.phase(key(c, "P")), APPLIED_PRESENT));
        REMOVED_SCOPED.put(at, ParityProbe.compare(at + " (Tailwind removed vs scoped legacy)", props, withoutDecorative(probe.phase(key(c, "S"))), withoutDecorative(probe.phase(key(c, "R"))), APPLIED_REMOVED));
        REMOVED_FULL.put(at, ParityProbe.compare(at + " (Tailwind removed vs full legacy)", props, withoutDecorative(legacyRows), withoutDecorative(probe.phase(key(c, "R"))), APPLIED_REMOVED));
        SCOPED_FIDELITY.put(at, ParityProbe.compare(at + " (scoped legacy vs full legacy)", props, legacyRows, probe.phase(key(c, "S")), APPLIED_SCOPED));
    }

    /** The measurements are tens of thousands of rows; every suite in this JVM is run one after another, so this one lets go of them when it is done. */
    @AfterAll
    static void clear() {
        probe = null;
        migratedForCounting = null;
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

    private static String one(String phase, String kind, String caseName, String property) {
        String layout = SPECS.stream().filter(s -> s.name().equals(caseName)).findFirst().orElseThrow().layout();
        List<ParityProbe.Row> found = probe.phase(phase).stream().filter(r -> r.kind().equals(kind) && r.variant().equals(caseName + "/" + layout)).toList();
        assertEquals(1, found.size(), phase + " " + kind + " " + caseName);
        return probe.value(found.get(0), property);
    }

    private static double number(String px) {
        return Double.parseDouble(px.replace("px", ""));
    }

    private static final List<String> RENDERINGS = List.of("light/none|L", "light/none|P", "light/none|R", "light/none|S");

    // ---- tests ----

    @Test
    void theConfigurationsAreTheTwentyTwoThemeAndSkinCombinations() {
        assertEquals(22, CONFIGS.size());
        assertEquals(22, PRESENT.size());
        assertTrue(SPECS.size() > 70, "the case list: " + SPECS.size());
    }

    @Test
    void controlledOracleWithTailwindPresentMatchesTheLegacyButtonsInEveryConfigurationAndCase() {
        assertEquals(List.of(), lines(PRESENT));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheLegacyButtonsGivenTheSameSurroundings() {
        assertEquals(List.of(), lines(REMOVED_SCOPED));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheFullLegacyRendering() {
        assertEquals(List.of(), lines(REMOVED_FULL));
    }

    @Test
    void theScopedLegacyStylesheetRendersTheLegacyButtonsExactlyAsTheFullOneDoes() {
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
    void theFiveSizesAreTailwindsHeightsPaddingsAndTypeInBothRenderings() {
        String[][] sizes = {{"xs", "24px", "8px", "12px", "16px"}, {"md", "40px", "16px", "14px", "20px"}, {"xl", "56px", "32px", "18px", "28px"}};
        for (String phase : RENDERINGS) {
            for (String[] s : sizes) {
                String name = "primary-" + s[0];
                assertEquals(s[1], one(phase, "button", name, "height"), phase + " " + name);
                assertEquals(s[2], one(phase, "button", name, "padding-left"), phase + " " + name);
                assertEquals(s[3], one(phase, "button", name, "font-size"), phase + " " + name);
                assertEquals(s[4], one(phase, "button", name, "line-height"), phase + " " + name);
            }
            assertEquals("32px", one(phase, "button", "primary-sm", "height"), phase);
            assertEquals("48px", one(phase, "button", "primary-lg", "height"), phase);
            assertEquals("24px", one(phase, "button", "primary-lg", "line-height"), phase);
        }
    }

    @Test
    void iconOnlyButtonsAreSquaresAndFullWidthButtonsFillTheirContainer() {
        for (String phase : RENDERINGS) {
            for (String[] s : new String[][] {{"xs", "24"}, {"sm", "32"}, {"md", "40"}, {"lg", "48"}, {"xl", "56"}}) {
                String name = "primary-icon-" + s[0];
                assertEquals(s[1], one(phase, "button", name, "@w"), phase + " " + name);
                assertEquals(s[1], one(phase, "button", name, "@h"), phase + " " + name);
                assertEquals("0px", one(phase, "button", name, "padding-left"), phase + " " + name);
            }
            assertEquals("140", one(phase, "button", "primary-block-narrow", "@w"), phase + ": full width fills a 140px container");
            assertEquals("140", one(phase, "button", "secondary-icon-block", "@w"), phase + ": an icon-only full-width button is full width");
        }
    }

    @Test
    void fileUploadsDecorativeChooseFileSpanIsAnUnclickableSecondarySmallButtonInEveryRendering() {
        for (String phase : RENDERINGS) {
            String where = phase + " file-upload-choose";
            List<ParityProbe.Row> rows = probe.phase(phase).stream().filter(r -> r.kind().equals("button-decorative")).toList();
            assertEquals(1, rows.size(), where);
            ParityProbe.Row r = rows.get(0);
            assertEquals("inline-flex", probe.value(r, "display"), where);
            assertEquals("32px", probe.value(r, "height"), where);
            // File Upload's own class (ash-file-upload__choose, file-upload.css) owns pointer-events: none now, so it is in force in every rendering, Tailwind removed included (it was File Upload's
            // Tailwind utility before the Choice Controls PR 3 migration, and then "auto" with Tailwind removed).
            assertEquals("none", probe.value(r, "pointer-events"), where + ": it is a label inside a label, never a target of its own");
            assertEquals("pointer", probe.value(r, "cursor"), where);
            assertEquals("1px", probe.value(r, "border-top-width"), where);
            assertEquals("none", probe.value(r, "appearance"), where + ": only a <button> element is a native push button");
        }
    }

    @Test
    void fileUploadsDecorativeSpanIsFlatInEverySkinIncludingFoundryAsItWasBeforeTheMigration() {
        // Legacy: the span carried shadow-sm, which every skin neutralised, and Foundry's own box-shadow rule needs data-btn, which the span does not have.
        // Migrated: the span has no shadow class, so each skin flattens it through --ash-btn-shadow (Foundry has that rule too). Foundry's real Buttons are
        // unaffected: its box-shadow rule sets the property itself. In the default theme the span keeps its soft shadow, as it had.
        for (ParityConfigs.Config c : CONFIGS) {
            for (String render : List.of("L", "P", "R")) {
                List<ParityProbe.Row> rows = probe.phase(c.id() + "|" + render).stream().filter(r -> r.kind().equals("button-decorative")).toList();
                assertEquals(1, rows.size(), c.id() + "|" + render);
                List<String> layers = ParityNormalizer.shadowLayers(probe.value(rows.get(0), "box-shadow"));
                if (c.skin() == null) {
                    assertEquals(2, layers.size(), c.id() + "|" + render + ": the default theme keeps the soft drop shadow: " + layers);
                } else {
                    assertEquals(List.of(), layers, c.id() + "|" + render + ": flat in every skin: " + layers);
                }
            }
        }
    }

    @Test
    void foundrysRealButtonsKeepTheirIntentionalShadowsAndOnlyTheOnesTheyHadAfterTheCorrection() {
        for (String render : List.of("L", "P", "R")) {
            String at = "dark/foundry|" + render;
            for (String filled : List.of("primary-md", "secondary-md", "danger-md", "ghost-pressed", "ghost-pressed-class")) {
                assertFalse(ParityNormalizer.shadowLayers(one(at, "button", filled, "box-shadow")).isEmpty(), at + " " + filled + ": Foundry shadows it");
            }
            for (String flat : List.of("ghost-md", "link-md", "link-pressed", "link-pressed-class")) {
                assertTrue(ParityNormalizer.shadowLayers(one(at, "button", flat, "box-shadow")).isEmpty(), at + " " + flat + ": stays flat");
            }
        }
    }

    @Test
    void iconsAreSizedForTheirButtonAndTheLeadingAndTrailingPositionsDiffer() {
        for (String phase : RENDERINGS) {
            assertEquals("16", one(phase, "button-svg", "primary-leading", "@w"), phase);
            assertEquals("12", one(phase, "button-svg", "primary-icon-xs", "@w"), phase);
            assertEquals("20", one(phase, "button-svg", "primary-icon-lg", "@w"), phase);
            assertEquals("24", one(phase, "button-svg", "primary-icon-xl", "@w"), phase);
            assertTrue(number(one(phase, "button-icon", "primary-leading", "@x")) < number(one(phase, "button-label", "primary-leading", "@x")), phase + ": leading");
            assertTrue(number(one(phase, "button-icon", "primary-trailing", "@x")) > number(one(phase, "button-label", "primary-trailing", "@x")), phase + ": trailing");
        }
    }

    @Test
    void disabledAndLoadingButtonsAreDimmedWithTheNotAllowedCursorAndOthersAreNot() {
        for (String phase : RENDERINGS) {
            assertEquals("0.6", one(phase, "button", "primary-disabled", "opacity"), phase);
            assertEquals("not-allowed", one(phase, "button", "primary-disabled", "cursor"), phase);
            assertEquals("0.6", one(phase, "button", "primary-loading", "opacity"), phase);
            assertEquals("not-allowed", one(phase, "button", "primary-loading", "cursor"), phase);
            assertEquals("1", one(phase, "button", "primary-md", "opacity"), phase);
            assertEquals("pointer", one(phase, "button", "primary-md", "cursor"), phase);
            assertEquals("none", one(phase, "button", "primary-md", "user-select"), phase);
        }
    }

    @Test
    void theSpinnerIsShownAndTheIconHiddenOnlyWhileLoadingAndTheStatusSaysSo() {
        for (String phase : RENDERINGS) {
            assertEquals("none", one(phase, "button-spinner", "primary-md", "display"), phase + ": idle");
            assertEquals("block", one(phase, "button-spinner", "primary-loading", "display"), phase + ": loading (an inline-block flex item is blockified)");
            assertEquals("16px", one(phase, "button-spinner", "primary-loading", "width"), phase);
            assertEquals("2px", one(phase, "button-spinner", "primary-loading", "border-top-width"), phase);
            assertEquals("1s", one(phase, "button-spinner", "primary-loading", "animation-duration"), phase + ": the spinner still turns");
            assertEquals("infinite", one(phase, "button-spinner", "primary-loading", "animation-iteration-count"), phase);
            assertEquals("linear", one(phase, "button-spinner", "primary-loading", "animation-timing-function"), phase);
            assertNotEquals("none", one(phase, "button-icon", "primary-leading", "display"), phase + ": an idle icon is shown");
            assertEquals("none", one(phase, "button-icon", "primary-loading-icon", "display"), phase + ": a loading button hides its icon");
            assertEquals("none", one(phase, "button-icon", "secondary-loading-trailing", "display"), phase);
        }
    }

    @Test
    void pressedIsDrawnFromAriaPressedAndFromTheModifierAndTheTwoLookTheSame() {
        for (String phase : RENDERINGS) {
            for (String v : List.of("primary", "secondary", "success", "warning", "danger", "info", "ghost", "link")) {
                assertEquals(one(phase, "button", v + "-pressed", "background-color"), one(phase, "button", v + "-pressed-class", "background-color"), phase + " " + v);
                assertEquals(one(phase, "button", v + "-pressed", "color"), one(phase, "button", v + "-pressed-class", "color"), phase + " " + v);
                assertEquals(one(phase, "button", v + "-pressed", "box-shadow"), one(phase, "button", v + "-pressed-class", "box-shadow"), phase + " " + v);
                assertEquals(one(phase, "button", v + "-pressed", "text-decoration-line"), one(phase, "button", v + "-pressed-class", "text-decoration-line"), phase + " " + v);
            }
            assertNotEquals(one(phase, "button", "primary-md", "background-color"), one(phase, "button", "primary-pressed", "background-color"), phase);
            assertEquals(one(phase, "button", "secondary-md", "background-color"), one(phase, "button", "secondary-toggle-off", "background-color"), phase + ": aria-pressed=false is resting");
            assertEquals("underline", one(phase, "button", "link-pressed", "text-decoration-line"), phase);
            assertEquals("none", one(phase, "button", "link-md", "text-decoration-line"), phase);
        }
    }

    @Test
    void theFilledVariantsHaveTheirDropShadowAtRestAndTheDefaultThemeKeepsIt() {
        for (String render : List.of("L", "P", "R")) {
            String plain = "light/none|" + render;
            assertNotEquals("none", one(plain, "button", "primary-md", "box-shadow"), plain);
            assertTrue(one(plain, "button", "primary-md", "box-shadow").contains("0px 1px 3px"), plain);
            assertTrue(one(plain, "button", "ghost-md", "box-shadow").equals("none") || ParityNormalizer.shadowLayers(one(plain, "button", "ghost-md", "box-shadow")).isEmpty(), plain);
            assertTrue(one(plain, "button", "primary-pressed", "box-shadow").contains("inset"), plain);
            // The nine skins flatten the drop shadow of a resting button but keep the inset shadow of a pressed one.
            for (String skin : List.of("axiom", "civic", "fathom", "harbor", "keystone", "ledger", "sterling", "summit", "veridian")) {
                String at = "light/" + skin + "|" + render;
                assertTrue(ParityNormalizer.shadowLayers(one(at, "button", "primary-md", "box-shadow")).isEmpty(), at + ": flat");
                assertTrue(one(at, "button", "primary-pressed", "box-shadow").contains("inset"), at + ": the pressed inset survives");
            }
        }
    }

    @Test
    void foundrysShadowReachesFilledButtonsAndAPressedGhostButNotARestingGhostOrALink() {
        for (String render : List.of("L", "P", "R")) {
            String at = "light/foundry|" + render;
            assertFalse(ParityNormalizer.shadowLayers(one(at, "button", "primary-md", "box-shadow")).isEmpty(), at + ": a filled button");
            assertFalse(ParityNormalizer.shadowLayers(one(at, "button", "secondary-md", "box-shadow")).isEmpty(), at + ": secondary");
            assertTrue(ParityNormalizer.shadowLayers(one(at, "button", "ghost-md", "box-shadow")).isEmpty(), at + ": a resting ghost is flat");
            assertTrue(ParityNormalizer.shadowLayers(one(at, "button", "link-md", "box-shadow")).isEmpty(), at + ": a link is flat");
            assertFalse(ParityNormalizer.shadowLayers(one(at, "button", "ghost-pressed", "box-shadow")).isEmpty(), at + ": a pressed ghost (toggle) takes the shadow");
            assertFalse(ParityNormalizer.shadowLayers(one(at, "button", "ghost-pressed-class", "box-shadow")).isEmpty(), at + ": a pressed ghost (modifier) takes the shadow");
            assertTrue(ParityNormalizer.shadowLayers(one(at, "button", "link-pressed", "box-shadow")).isEmpty(), at + ": a pressed link stays flat");
        }
    }

    @Test
    void theSkinsReachButtonsThroughTheirHooks() {
        for (String render : List.of("L", "P", "R")) {
            // Civic and Summit set the weight on every Button; the default is 500.
            assertEquals("500", one("light/none|" + render, "button", "primary-md", "font-weight"), render);
            assertEquals("600", one("light/civic|" + render, "button", "primary-md", "font-weight"), render);
            assertEquals("600", one("light/summit|" + render, "button", "primary-md", "font-weight"), render);
            // A skin's --radius-md rounds every Button.
            assertNotEquals(one("light/none|" + render, "button", "primary-md", "border-top-left-radius"), one("light/foundry|" + render, "button", "primary-md", "border-top-left-radius"), render);
        }
    }

    @Test
    void darkModeUsesTailwindsDarkColoursWhereTheLegacyButtonHadThem() {
        for (String render : List.of("L", "P", "R")) {
            String light = "light/none|" + render;
            String dark = "dark/none|" + render;
            for (String[] part : new String[][] {{"secondary-md", "background-color"}, {"secondary-md", "color"}, {"secondary-md", "border-top-color"}, {"ghost-md", "color"},
                    {"link-md", "color"}, {"secondary-pressed", "background-color"}, {"ghost-pressed", "background-color"}, {"link-pressed", "color"}}) {
                assertNotEquals(one(light, "button", part[0], part[1]), one(dark, "button", part[0], part[1]), render + " " + part[0] + " " + part[1]);
            }
            // The filled variants had no dark classes.
            assertEquals(one(light, "button", "primary-md", "background-color"), one(dark, "button", "primary-md", "background-color"), render);
            assertEquals(one(light, "button", "danger-md", "background-color"), one(dark, "button", "danger-md", "background-color"), render);
        }
    }

    @Test
    void theCasesAreCompleteAndEveryPartOfEveryButtonIsMeasured() {
        List<ParityProbe.Row> rows = probe.phase("light/none|L");
        Map<String, Integer> byKind = new TreeMap<>();
        rows.forEach(r -> byKind.merge(r.kind(), 1, Integer::sum));
        for (String kind : List.of("button", "button-spinner", "button-icon", "button-svg", "button-label", "button-status")) {
            int written = count(migratedForCounting, "data-parity=\"" + kind + "\"");
            assertTrue(written > 0, kind);
            assertEquals(written, byKind.get(kind), kind + " rows");
        }
        assertEquals(SPECS.size(), byKind.get("button"));
        assertEquals(SPECS.size() + 1, ButtonLegacyMarkup.countButtons(migratedForCounting), "every Button plus File Upload's decorative one");
        assertEquals(1, byKind.get("button-decorative"));
        for (String key : ROWS.keySet()) {
            assertEquals(ROWS.get("light/none|L"), ROWS.get(key), "every configuration measures the same elements: " + key);
        }
        assertTrue(SPECS.stream().map(Spec::variant).collect(Collectors.toSet()).size() == 8, "all eight variants");
        assertTrue(SPECS.stream().map(Spec::size).collect(Collectors.toSet()).size() == 5, "all five sizes");
    }

    @Test
    void theOracleIsNotVacuousEveryLegacyClassIsCompiledInTheCumulativeAndScopedStylesheets() throws IOException {
        Path dir = Path.of(System.getProperty("ashlar.cascade.dir"));
        String oracle = Files.readString(dir.resolve("button-oracle.css"), StandardCharsets.UTF_8);
        String scoped = Files.readString(dir.resolve("button-legacy-scoped.css"), StandardCharsets.UTF_8);
        List<String> tokens = new ArrayList<>(buttonLegacy.allTokens());
        // ParityOracle recognises a class selector followed by a block, a pseudo-class or a comma; Tailwind writes aria-disabled: as an attribute selector, checked here.
        List<String> plain = tokens.stream().filter(t -> !t.startsWith("aria-disabled:")).toList();
        List<String> aria = tokens.stream().filter(t -> t.startsWith("aria-disabled:")).toList();
        assertTrue(aria.size() > 5, "the aria-disabled: tokens: " + aria);
        for (String css : List.of(oracle, scoped)) {
            assertEquals(List.of(), ParityOracle.uncompiled(css, plain), "the stylesheet does not compile these");
            assertEquals(List.of(), aria.stream().filter(t -> !css.contains("." + t.replace(":", "\\:") + "[aria-disabled=true]")).toList(), "the stylesheet does not compile these aria-disabled: classes");
        }
        List<String> earlier = new ArrayList<>();
        earlier.addAll(new CardLegacyMarkup().allTokens());
        earlier.addAll(new PageCompositionLegacyMarkup().allTokens());
        earlier.addAll(new IconLegacyMarkup().allTokens());
        earlier.addAll(new SpinnerLegacyMarkup().allTokens());
        assertEquals(List.of(), ParityOracle.uncompiled(oracle, earlier), "the oracle lost earlier legacy classes");
        assertFalse(scoped.contains("html,:host"), "the scoped legacy stylesheet must not carry Tailwind's page-wide base");
    }

    @Test
    void normalizationsStayWithinTheApprovedRules() {
        // Tailwind's ring and shadow utilities compute transparent zero-size layers (H1) and a border style on a zero-width border (H2), and a
        // rounded-full spinner is a pill (H4); nothing else may be normalized, with Tailwind present or removed.
        assertTrue(Set.of("H1", "H2", "H4").containsAll(APPLIED_PRESENT.keySet()), "unexpected normalization rule used with Tailwind present: " + APPLIED_PRESENT);
        assertTrue(Set.of("H1", "H2", "H4").containsAll(APPLIED_REMOVED.keySet()), "unexpected normalization rule used with Tailwind removed: " + APPLIED_REMOVED);
        assertTrue(Set.of("H1", "H2", "H4").containsAll(APPLIED_SCOPED.keySet()), "unexpected normalization rule used by the scoped rendering: " + APPLIED_SCOPED);
    }

    // ---- what the real pages serve ----

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }

    private static final Pattern LEFTOVER_TAILWIND = Pattern.compile("<button[^>]*data-btn[^>]*class=\"[^\"]*(?:bg-blue-|inline-flex|rounded-md|focus-visible:)[^\"]*\"|class=\"[^\"]*(?:bg-blue-|inline-flex|rounded-md|focus-visible:)[^\"]*\"[^>]*data-btn[ >]");
    private static final Pattern BUTTON_START = Pattern.compile("<button\\b[^>]*\\bdata-btn\\b[^>]*>");

    @Test
    void everyButtonTheRealPagesServeKeepsItsNativeStructureAndHooksAndNoTailwind() throws Exception {
        ParitySession session = ParitySession.open("button-parity");
        HttpClient client = HttpClient.newHttpClient();
        List<String> paths = new ArrayList<>(List.of("/ashlar/", "/ashlar/components"));
        for (ComponentPage page : ComponentCatalog.PAGES) {
            paths.add("/ashlar/components/" + page.slug());
        }
        int buttons = 0;
        int toggles = 0;
        int loading = 0;
        List<String> problems = new ArrayList<>();
        try (RunningShowcase app = RunningShowcase.start(session.bootJar())) {
            for (String path : paths) {
                HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + path)).GET().build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                assertEquals(200, response.statusCode(), path);
                String body = response.body();
                // The rewrite throws on any class value that carries an ash-btn class but is not a known part.
                buttonLegacy.rewrite(body);
                long starts = BUTTON_START.matcher(body).results().count();
                int classed = ButtonLegacyMarkup.countButtons(body);
                // Every <button data-btn> carries the Button classes (a File Upload's decorative span adds one more classed element, never a button).
                if (classed < starts) {
                    problems.add(path + ": a <button data-btn> does not carry the Button classes: " + starts + " vs " + classed);
                }
                buttons += (int) starts;
                if (LEFTOVER_TAILWIND.matcher(body).find()) {
                    problems.add(path + ": a Button still renders Tailwind classes");
                }
                if (body.contains("data-btn-base-classes") || body.contains("data-btn-pressed-classes")) {
                    problems.add(path + ": a Button still renders a class-list attribute");
                }
                for (String bad : List.of("class=\"hidden h-4", "class=\"inline-block h-4", "animate-spin")) {
                    if (body.contains("data-btn-spinner") && body.contains(bad) && body.indexOf(bad) > body.indexOf("data-btn-spinner") - 200 && body.indexOf(bad) < body.indexOf("data-btn-spinner") + 200) {
                        problems.add(path + ": a Button spinner still renders Tailwind: " + bad);
                    }
                }
                toggles += count(body, "data-btn-toggle");
                loading += count(body, "aria-busy=\"true\"");
            }
            // File Upload: the decorative "Choose File" is a span with the Button classes, never a second <button>.
            HttpResponse<String> upload = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + "/ashlar/components/file-upload")).GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String uploadBody = upload.body();
            if (count(uploadBody, "<span class=\"ash-btn ash-btn--secondary ash-btn--sm ash-file-upload__choose\">Choose File</span>") < 1) {
                problems.add("/ashlar/components/file-upload: the decorative Choose File span does not carry the Secondary Small Button classes");
            }
            if (uploadBody.contains("bg-white text-slate-700") && uploadBody.contains("focus-visible:ring-blue-500")) {
                problems.add("/ashlar/components/file-upload: Button's Tailwind classes are still rendered");
            }
        }
        assertEquals(List.of(), problems);
        assertTrue(buttons > 150, "the real pages render many Buttons: " + buttons);
        assertTrue(toggles > 0, "a toggle Button is rendered on a real page");
        assertTrue(loading > 0, "a loading Button is rendered on a real page");
    }
}
