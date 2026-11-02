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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The parity gates of Card and Stat Card (migrated together), measured in real Chrome on the shared parity harness (unchanged). Both are
 * JS-free, so the harness's computed-style and box measurement applies.
 *
 * <p>Evidence is a controlled oracle: the components' legacy Tailwind markup (compiled by the real Tailwind CLI) next to their migrated
 * markup, in the same browser, in three layouts for each of the 22 theme and skin configurations, with Tailwind present and with Tailwind
 * removed. The legacy markup carries the same {@code data-card}, {@code data-card-header}, {@code data-card-footer} and
 * {@code data-stat-card} hooks as the migrated markup (an approved additive change), so the skins, which select the hooks, apply to both
 * sides and every skin's card, header, title, footer and value rules are compared like any other style. Neither component has a
 * breakpoint, so the viewport only matters through the width it gives a layout: every configuration is measured at 900px, and the four
 * theme and skin configurations that matter for non-colour properties are also measured at 320px, 440px and 1300px. Negative controls
 * show each gate can fail, including a control that removes the hooks (the skins then stop reaching the header, title and footer).
 * Every Card and Stat Card on the real showcase pages is also compared by {@code ShowcaseRuntimeTest}, and their structure is checked on
 * every real page below.
 *
 * <p>Evidence is Chrome only. Computed-style and box parity is the visual evidence; there is no screenshot comparison.
 */
class CardParityTest {

    private static final String ORACLE = ParityFrames.generated("card-oracle.css");
    private static final String SCOPED = ParityFrames.generated("card-legacy-scoped.css");
    private static final List<ParityConfigs.Config> CONFIGS = ParityConfigs.all();
    private static final List<ParityConfigs.Config> EDGE_CONFIGS = ParityConfigs.reduced();
    private static final List<Integer> WIDTHS = List.of(900);
    private static final List<Integer> EDGE_WIDTHS = List.of(320, 440, 1300);
    private static final List<String> EXTRA_PROPS = List.of("font-family", "font-size", "font-weight", "font-variant-numeric", "line-height", "letter-spacing",
            "text-decoration-line", "text-align", "white-space", "overflow-wrap", "word-break", "flex-direction", "flex-wrap", "align-items", "justify-content",
            "align-content", "row-gap", "column-gap", "padding-inline-start", "margin-inline-start");

    private record Layout(String name, String style) {
    }

    private static final List<Layout> LAYOUTS = List.of(
            new Layout("block", "box-sizing:border-box"),
            new Layout("narrow", "box-sizing:border-box;width:240px"),
            new Layout("column-flex-centered", "box-sizing:border-box;display:flex;flex-direction:column;align-items:center;width:600px"));

    /** The kinds of measured element that are the caller's own content, not the component's: preflight reached them too, so a full-legacy comparison leaves them out. */
    private static final Set<String> CALLER_KINDS = Set.of("raw");

    private static List<ParityProbe.Row> owned(List<ParityProbe.Row> rows) {
        return rows.stream().filter(r -> !CALLER_KINDS.contains(r.kind())).toList();
    }

    private static final String LONG_TITLE = "Quarterly infrastructure capacity review and the migration plan for the remaining regional clusters";
    private static final String LONG_TEXT = "The nightly import finished with 3 warnings and 0 errors, and the next run is scheduled for tomorrow morning before the office opens.";
    private static final String UNBROKEN = "unbrokenidentifier".repeat(7);

    // ---- state of the measurement ----

    private static CardLegacyMarkup legacy;
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
    private static String legacyMarkupForCounting;

    // ---- markup ----

    private static String mark(String kind, String name) {
        return " data-parity=\"" + kind + "\" data-parity-case=\"" + name + "\"";
    }

    /** The class attribute of a part, with its leading space; nothing for a part that had no class before the migration. */
    private static String cls(String role, boolean legacyClasses) {
        String value = legacyClasses ? legacy.classes(role) : CardLegacyMarkup.MIGRATED.get(role);
        return value.isEmpty() ? "" : " class=\"" + value + "\"";
    }

    private static String raw(String tag, String text, String name, String style) {
        return "<" + tag + mark("raw", name) + " style=\"margin:0;" + style + "\">" + text + "</" + tag + ">";
    }

    /** A Card. A null title, body or footer leaves that part out, exactly as the template does. */
    private static String card(String name, String title, String body, String footer, boolean l) {
        StringBuilder sb = new StringBuilder("<div" + cls("card", l) + " data-card" + mark("card", name) + ">");
        if (title != null) {
            sb.append("<div").append(cls("card-header", l)).append(" data-card-header").append(mark("card-header", name)).append("><h3")
                    .append(cls("card-title", l)).append(mark("card-title", name)).append(">").append(title).append("</h3></div>");
        }
        if (body != null) {
            sb.append("<div").append(cls("card-body", l)).append(mark("card-body", name)).append(">").append(body).append("</div>");
        }
        if (footer != null) {
            sb.append("<div").append(cls("card-footer", l)).append(" data-card-footer").append(mark("card-footer", name)).append(">").append(footer).append("</div>");
        }
        return sb.append("</div>").toString();
    }

    /** A Stat Card. A null change or description leaves that part out. */
    private static String stat(String name, String label, String value, String change, String description, boolean l) {
        StringBuilder sb = new StringBuilder("<div" + cls("stat-card", l) + " data-stat-card" + mark("stat-card", name) + ">");
        sb.append("<dl").append(cls(CardLegacyMarkup.LIST_ROLE, l)).append(mark("stat-card-list", name)).append(">");
        sb.append("<dt").append(cls("stat-card-label", l)).append(mark("stat-card-label", name)).append(">").append(label).append("</dt>");
        sb.append("<dd").append(cls("stat-card-values", l)).append(mark("stat-card-values", name)).append("><span").append(cls("stat-card-value", l))
                .append(mark("stat-card-value", name)).append(">").append(value).append("</span>");
        if (change != null) {
            sb.append("<span").append(cls("stat-card-change", l)).append(mark("stat-card-change", name)).append(">").append(change).append("</span>");
        }
        sb.append("</dd>");
        if (description != null) {
            sb.append("<dd").append(cls("stat-card-description", l)).append(mark("stat-card-description", name)).append(">").append(description).append("</dd>");
        }
        return sb.append("</dl></div>").toString();
    }

    private static final List<String> CASES = List.of("card-full", "card-title", "card-body", "card-footer", "card-title-footer", "card-empty", "card-long",
            "card-unbroken", "card-actions-wrap", "card-nested", "card-raw-blocks",
            "stat-minimal", "stat-change", "stat-description", "stat-full", "stat-nonnumeric", "stat-long-value", "stat-long-text", "stat-change-wrap");

    private static String fixture(String name, boolean l) {
        String button = "display:inline-block;width:80px;height:24px;";
        String wide = "display:inline-block;width:110px;height:24px;";
        switch (name) {
            case "card-full":
                return card(name, "Delete account", "This permanently removes the account and all associated records.",
                        raw("button", "Cancel", name + "-a", button) + raw("button", "Delete", name + "-b", button), l);
            case "card-title":
                return card(name, "Recent activity", null, null, l);
            case "card-body":
                return card(name, null, "A plain body with no title and no footer.", null, l);
            case "card-footer":
                return card(name, null, null, raw("button", "Save", name + "-a", button), l);
            case "card-title-footer":
                return card(name, "Settings", null, raw("button", "Save", name + "-a", button), l);
            case "card-empty":
                return card(name, null, null, null, l);
            case "card-long":
                return card(name, LONG_TITLE, LONG_TEXT, raw("button", "Dismiss", name + "-a", button), l);
            case "card-unbroken":
                return card(name, UNBROKEN, UNBROKEN, raw("button", "Dismiss", name + "-a", button), l);
            case "card-actions-wrap":
                return card(name, "Actions", "Four wide actions.", raw("button", "A", name + "-a", wide) + raw("button", "B", name + "-b", wide)
                        + raw("button", "C", name + "-c", wide) + raw("button", "D", name + "-d", wide), l);
            case "card-nested":
                return card(name, "Outer", card(name + "-inner", "Inner", "Nested body", raw("button", "Inner action", name + "-a", button), l), null, l);
            case "card-raw-blocks":
                return card(name, "Blocks", raw("div", "tall content", name + "-a", "height:120px") + raw("div", "second", name + "-b", "height:20px"),
                        raw("button", "Go", name + "-c", button), l);
            case "stat-minimal":
                return stat(name, "Active users", "2,481", null, null, l);
            case "stat-change":
                return stat(name, "Total accounts", "1,248", raw("span", "+32 this month", name + "-a", "display:inline-block;width:100px;height:20px"), null, l);
            case "stat-description":
                return stat(name, "Active", "892", null, "71% of all accounts", l);
            case "stat-full":
                return stat(name, "Revenue", "$48.2K", raw("span", "+12%", name + "-a", "display:inline-block;width:50px;height:20px"), "versus last month", l);
            case "stat-nonnumeric":
                return stat(name, "Status", "Active", null, null, l);
            case "stat-long-value":
                return stat(name, "Hash", UNBROKEN, raw("span", "ok", name + "-a", "display:inline-block;width:30px;height:20px"), "an unbroken value", l);
            case "stat-long-text":
                return stat(name, LONG_TITLE, "12", null, LONG_TEXT + " " + UNBROKEN, l);
            case "stat-change-wrap":
                return stat(name, "Total accounts", "1,248", raw("span", "+32 this month", name + "-a", "display:inline-block;width:140px;height:20px"), "wraps below the value", l);
            default:
                throw new IllegalArgumentException(name);
        }
    }

    private static String markup(boolean l) {
        StringBuilder sb = new StringBuilder();
        for (Layout layout : LAYOUTS) {
            for (String name : CASES) {
                sb.append("<div style=\"margin:4px 0\"><div data-layout=\"").append(layout.name()).append("\" style=\"").append(layout.style()).append("\">")
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

    // ---- control experiments: each breaks the migrated components in ONE way and must be detected ----

    private record Control(String name, String config, int width, String css, String expectedProperty, boolean withoutHooks) {
        Control(String name, String config, int width, String css, String expectedProperty) {
            this(name, config, width, css, expectedProperty, false);
        }
    }

    private static final List<Control> CONTROL_LIST = List.of(
            // Card
            new Control("card border is two pixels", "light/none", 900, ".ash-card{border-width:2px}", "border-top-width"),
            new Control("card border colour is wrong", "light/none", 900, ".ash-card{--ash-card-border:red}", "border-top-color"),
            new Control("card dark border ignores the theme", "dark/none", 900, ".ash-card{--ash-card-border:var(--color-slate-200)}", "border-top-color"),
            new Control("card radius is wrong", "light/none", 900, ".ash-card{border-radius:4px}", "border-top-left-radius"),
            new Control("card radius ignores a skin's --radius-xl", "light/foundry", 900, ".ash-card{border-radius:0.75rem}", "border-top-left-radius"),
            new Control("card no longer clips its children", "light/none", 900, ".ash-card{overflow:visible}", "overflow-x"),
            new Control("card background is wrong", "light/none", 900, ".ash-card{--ash-card-bg:red}", "background-color"),
            new Control("card ignores a skin's --color-white", "light/foundry", 900, ".ash-card{--ash-card-bg:#fff}", "background-color"),
            new Control("card dark background ignores the theme", "dark/none", 900, ".ash-card{--ash-card-bg:var(--color-white)}", "background-color"),
            new Control("header padding is wrong", "light/none", 900, ".ash-card__header{padding:8px 24px}", "padding-top"),
            new Control("header rule is two pixels", "light/none", 900, ".ash-card__header{border-bottom-width:2px}", "border-bottom-width"),
            new Control("header rule is missing", "light/none", 900, ".ash-card__header{border-bottom-width:0}", "border-bottom-width"),
            new Control("header rule dark colour ignores the theme", "dark/none", 900, ".ash-card__header{border-bottom-color:var(--color-slate-200)}", "border-bottom-color"),
            new Control("title is larger", "light/none", 900, ".ash-card__title{font-size:1.125rem}", "font-size"),
            new Control("title is not semibold", "light/none", 900, ".ash-card__title{font-weight:700}", "font-weight"),
            new Control("title keeps the heading margin", "light/none", 900, ".ash-card__title{margin-block:0.83em}", "margin-top"),
            new Control("title dark colour ignores the theme", "dark/none", 900, ".ash-card{--ash-card-title:var(--color-slate-900)}", "color"),
            new Control("body padding is wrong", "light/none", 900, ".ash-card__body{padding:8px}", "padding-left"),
            new Control("body text is larger", "light/none", 900, ".ash-card__body{font-size:1rem}", "font-size"),
            new Control("body line height is wrong", "light/none", 900, ".ash-card__body{line-height:1.5}", "line-height"),
            new Control("body dark colour ignores the theme", "dark/none", 900, ".ash-card{--ash-card-text:var(--color-slate-700)}", "color"),
            new Control("footer gap is wrong", "light/none", 900, ".ash-card__footer{gap:8px}", "column-gap"),
            new Control("footer is not at the end", "light/none", 900, ".ash-card__footer{justify-content:flex-start}", "justify-content"),
            new Control("footer never wraps", "light/none", 440, ".ash-card__footer{flex-wrap:nowrap}", "flex-wrap"),
            new Control("footer is not centred", "light/none", 900, ".ash-card__footer{align-items:flex-start}", "align-items"),
            new Control("footer background is wrong", "light/none", 900, ".ash-card{--ash-card-footer-bg:red}", "background-color"),
            new Control("footer dark background ignores the theme", "dark/none", 900, ".ash-card{--ash-card-footer-bg:var(--color-slate-50)}", "background-color"),
            new Control("footer rule is two pixels", "light/none", 900, ".ash-card__footer{border-top-width:2px}", "border-top-width"),
            // Stat Card
            new Control("stat padding is wrong", "light/none", 900, ".ash-stat-card{padding:16px}", "padding-top"),
            new Control("stat border colour is wrong", "light/none", 900, ".ash-stat-card{--ash-stat-card-border:red}", "border-top-color"),
            new Control("stat radius is wrong", "light/none", 900, ".ash-stat-card{border-radius:4px}", "border-top-left-radius"),
            new Control("stat radius ignores a skin's --radius-xl", "light/foundry", 900, ".ash-stat-card{border-radius:0.75rem}", "border-top-left-radius"),
            new Control("stat dark background ignores the theme", "dark/none", 900, ".ash-stat-card{--ash-stat-card-bg:var(--color-white)}", "background-color"),
            new Control("stat ignores a skin's --color-white", "light/foundry", 900, ".ash-stat-card{--ash-stat-card-bg:#fff}", "background-color"),
            new Control("stat list keeps the browser margin", "light/none", 900, ".ash-stat-card__list{margin-block:1em}", "margin-top"),
            new Control("label is larger", "light/none", 900, ".ash-stat-card__label{font-size:1rem}", "font-size"),
            new Control("label is not medium weight", "light/none", 900, ".ash-stat-card__label{font-weight:400}", "font-weight"),
            new Control("label dark colour ignores the theme", "dark/none", 900, ".ash-stat-card{--ash-stat-card-label:var(--color-slate-500)}", "color"),
            new Control("values lose their top margin", "light/none", 900, ".ash-stat-card__values{margin-top:0}", "margin-top"),
            new Control("values keep the browser indent", "light/none", 900, ".ash-stat-card__values{margin-inline-start:40px}", "margin-left"),
            new Control("values are not on a baseline", "light/none", 900, ".ash-stat-card__values{align-items:center}", "align-items"),
            new Control("values never wrap", "light/none", 440, ".ash-stat-card__values{flex-wrap:nowrap}", "flex-wrap"),
            new Control("values column gap is wrong", "light/none", 900, ".ash-stat-card__values{column-gap:16px}", "column-gap"),
            new Control("values row gap is missing", "light/none", 900, ".ash-stat-card__values{row-gap:0}", "row-gap"),
            new Control("value is larger", "light/none", 900, ".ash-stat-card__value{font-size:2rem}", "font-size"),
            new Control("value is bolder", "light/none", 900, ".ash-stat-card__value{font-weight:700}", "font-weight"),
            new Control("value has no tracking", "light/none", 900, ".ash-stat-card__value{letter-spacing:0}", "letter-spacing"),
            new Control("value does not break inside a long word", "light/none", 440, ".ash-stat-card__value{overflow-wrap:normal}", "overflow-wrap"),
            new Control("value dark colour ignores the theme", "dark/none", 900, ".ash-stat-card{--ash-stat-card-value:var(--color-slate-900)}", "color"),
            new Control("change shrinks", "light/none", 440, ".ash-stat-card__change{flex-shrink:1}", "flex-shrink"),
            new Control("description loses its top margin", "light/none", 900, ".ash-stat-card__description{margin-top:0}", "margin-top"),
            new Control("description is larger", "light/none", 900, ".ash-stat-card__description{font-size:1rem}", "font-size"),
            new Control("description dark colour ignores the theme", "dark/none", 900, ".ash-stat-card{--ash-stat-card-description:var(--color-slate-500)}", "color"),
            new Control("description does not break inside a long word", "light/none", 440, ".ash-stat-card__description{overflow-wrap:normal}", "overflow-wrap"),
            // The hooks: without them the skins stop reaching the header, the title and the footer.
            new Control("Civic's header band needs data-card-header", "light/civic", 900, "", "background-color", true),
            new Control("Summit's card title weight needs data-card-header", "light/summit", 900, "", "font-weight", true),
            new Control("Axiom's footer rule needs data-card-footer", "light/axiom", 900, "", "border-top-color", true));

    // ---- running ----

    @BeforeAll
    static void measure() throws Exception {
        ParitySession session = ParitySession.open("card-parity");
        legacy = new CardLegacyMarkup();
        String environment = ParityFrames.environment(session.generatedDir());
        String migrated = markup(false);
        String legacyMarkup = markup(true);
        legacyMarkupForCounting = legacyMarkup;

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
            String body = control.withoutHooks() ? migrated.replace(" data-card-header", "").replace(" data-card-footer", "") : migrated;
            frames.append(sized(ParityFrames.frame("ctl:" + control.name() + "|P", c, ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), control.css(), body), control.width()));
        }
        probe = ParityFrames.measure(session, frames.toString(), EXTRA_PROPS, "card frames");
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
        REMOVED_FULL.put(at, ParityProbe.compare(at + " (Tailwind removed vs full legacy, the components' own elements)", props, owned(legacyRows), owned(probe.phase(key(c, "R", width))), APPLIED_REMOVED));
        SCOPED_FIDELITY.put(at, ParityProbe.compare(at + " (scoped legacy vs full legacy, the components' own elements)", props, owned(legacyRows), owned(probe.phase(key(c, "S", width))), APPLIED_SCOPED));
    }

    /** The measurements are tens of thousands of rows; every suite in this JVM is run one after another, so this one lets go of them when it is done. */
    @AfterAll
    static void clear() {
        probe = null;
        legacyMarkupForCounting = null;
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
    void controlledOracleWithTailwindPresentMatchesTheLegacyComponentsInEveryConfigurationCaseLayoutAndWidth() {
        assertEquals(List.of(), lines(PRESENT));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheLegacyComponentsGivenTheSameSurroundings() {
        assertEquals(List.of(), lines(REMOVED_SCOPED));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheFullLegacyRendering() {
        assertEquals(List.of(), lines(REMOVED_FULL));
    }

    @Test
    void theScopedLegacyStylesheetRendersTheLegacyComponentsExactlyAsTheFullOneDoes() {
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
    void aCardHasTailwindsBorderRadiusSurfaceSpacingAndTypographyInBothRenderings() {
        for (String phase : RENDERINGS) {
            String at = phase + 900;
            assertEquals("1px", one(at, "card", "card-full", "block", "border-top-width"), at);
            assertEquals("solid", one(at, "card", "card-full", "block", "border-top-style"), at);
            assertEquals("12px", one(at, "card", "card-full", "block", "border-top-left-radius"), at);
            assertEquals("hidden", one(at, "card", "card-full", "block", "overflow-x"), at);
            assertEquals("border-box", one(at, "card", "card-full", "block", "box-sizing"), at);
            assertEquals("1px", one(at, "card-header", "card-full", "block", "border-bottom-width"), at);
            assertEquals("0px", one(at, "card-header", "card-full", "block", "border-top-width"), at);
            assertEquals("16px", one(at, "card-header", "card-full", "block", "padding-top"), at);
            assertEquals("24px", one(at, "card-header", "card-full", "block", "padding-left"), at);
            assertEquals("16px", one(at, "card-title", "card-full", "block", "font-size"), at);
            assertEquals("600", one(at, "card-title", "card-full", "block", "font-weight"), at);
            assertEquals("24px", one(at, "card-title", "card-full", "block", "line-height"), at);
            assertEquals("0px", one(at, "card-title", "card-full", "block", "margin-top"), at);
            assertEquals("0px", one(at, "card-title", "card-full", "block", "margin-bottom"), at);
            assertEquals("16px", one(at, "card-body", "card-full", "block", "padding-top"), at);
            assertEquals("24px", one(at, "card-body", "card-full", "block", "padding-left"), at);
            assertEquals("14px", one(at, "card-body", "card-full", "block", "font-size"), at);
            assertEquals("20px", one(at, "card-body", "card-full", "block", "line-height"), at);
            assertEquals("flex", one(at, "card-footer", "card-full", "block", "display"), at);
            assertEquals("flex-end", one(at, "card-footer", "card-full", "block", "justify-content"), at);
            assertEquals("center", one(at, "card-footer", "card-full", "block", "align-items"), at);
            assertEquals("wrap", one(at, "card-footer", "card-full", "block", "flex-wrap"), at);
            assertEquals("12px", one(at, "card-footer", "card-full", "block", "column-gap"), at);
            assertEquals("1px", one(at, "card-footer", "card-full", "block", "border-top-width"), at);
            assertEquals("16px", one(at, "card-footer", "card-full", "block", "padding-top"), at);
        }
    }

    @Test
    void aStatCardHasTailwindsSpacingTypographyAndNoBrowserMarginOrIndentOnItsListOrDefinitions() {
        for (String phase : RENDERINGS) {
            String at = phase + 900;
            assertEquals("24px", one(at, "stat-card", "stat-full", "block", "padding-top"), at);
            assertEquals("1px", one(at, "stat-card", "stat-full", "block", "border-top-width"), at);
            assertEquals("12px", one(at, "stat-card", "stat-full", "block", "border-top-left-radius"), at);
            assertEquals("0px", one(at, "stat-card-list", "stat-full", "block", "margin-top"), at + ": a <dl> has a browser margin without preflight");
            assertEquals("0px", one(at, "stat-card-list", "stat-full", "block", "margin-bottom"), at);
            assertEquals("14px", one(at, "stat-card-label", "stat-full", "block", "font-size"), at);
            assertEquals("500", one(at, "stat-card-label", "stat-full", "block", "font-weight"), at);
            assertEquals("20px", one(at, "stat-card-label", "stat-full", "block", "line-height"), at);
            assertEquals("flex", one(at, "stat-card-values", "stat-full", "block", "display"), at);
            assertEquals("baseline", one(at, "stat-card-values", "stat-full", "block", "align-items"), at);
            assertEquals("wrap", one(at, "stat-card-values", "stat-full", "block", "flex-wrap"), at);
            assertEquals("8px", one(at, "stat-card-values", "stat-full", "block", "margin-top"), at);
            assertEquals("0px", one(at, "stat-card-values", "stat-full", "block", "margin-left"), at + ": a <dd> is indented by the browser without preflight");
            assertEquals("0px", one(at, "stat-card-values", "stat-full", "block", "margin-bottom"), at);
            assertEquals("8px", one(at, "stat-card-values", "stat-full", "block", "column-gap"), at);
            assertEquals("4px", one(at, "stat-card-values", "stat-full", "block", "row-gap"), at);
            assertEquals("30px", one(at, "stat-card-value", "stat-full", "block", "font-size"), at);
            assertEquals("36px", one(at, "stat-card-value", "stat-full", "block", "line-height"), at);
            assertEquals("600", one(at, "stat-card-value", "stat-full", "block", "font-weight"), at);
            assertEquals("-0.75px", one(at, "stat-card-value", "stat-full", "block", "letter-spacing"), at);
            assertEquals("break-word", one(at, "stat-card-value", "stat-full", "block", "overflow-wrap"), at);
            assertEquals("0", one(at, "stat-card-change", "stat-full", "block", "flex-shrink"), at);
            assertEquals("4px", one(at, "stat-card-description", "stat-full", "block", "margin-top"), at);
            assertEquals("0px", one(at, "stat-card-description", "stat-full", "block", "margin-left"), at);
            assertEquals("14px", one(at, "stat-card-description", "stat-full", "block", "font-size"), at);
            assertEquals("20px", one(at, "stat-card-description", "stat-full", "block", "line-height"), at);
        }
    }

    @Test
    void theFooterWrapsItsActionsAndTheChangeWrapsBelowTheValueInANarrowContainer() {
        for (String phase : RENDERINGS) {
            String at = phase + 900;
            Set<String> tops = new TreeSet<>();
            for (String button : List.of("a", "b", "c", "d")) {
                tops.add(one(at, "raw", "card-actions-wrap-" + button, "narrow", "@y"));
            }
            assertTrue(tops.size() > 1, at + ": four 110px actions wrap in a 240px card: " + tops);
            assertTrue(number(one(at, "stat-card-change", "stat-change-wrap", "narrow", "@y")) > number(one(at, "stat-card-value", "stat-change-wrap", "narrow", "@y")),
                    at + ": the change wraps below the value");
            assertEquals("140", one(at, "stat-card-change", "stat-change-wrap", "narrow", "@w"), at + ": the change never shrinks");
        }
    }

    @Test
    void longAndUnbrokenContentStaysInsideItsCardAndTheValueBreaksInsideALongWord() {
        for (String phase : RENDERINGS) {
            String at = phase + 900;
            assertEquals("240", one(at, "card", "card-unbroken", "narrow", "@w"), at + ": the card keeps its container's width and clips");
            assertEquals("hidden", one(at, "card", "card-unbroken", "narrow", "overflow-x"), at);
            assertEquals("240", one(at, "stat-card", "stat-long-value", "narrow", "@w"), at + ": the Stat Card keeps its container's width (an unbroken value overflows it, as before: parity compares that)");
            assertTrue(number(one(at, "stat-card-description", "stat-long-text", "narrow", "@w")) <= 240 - 2 - 48, at + ": the description breaks inside the padding box");
            assertTrue(number(one(at, "card-title", "card-long", "narrow", "@h")) > 24, at + ": a long title wraps");
        }
    }

    @Test
    void theHooksLetTheSkinsReachTheHeaderTheTitleAndTheFooterInBothRenderings() {
        for (String render : List.of("L", "P", "R")) {
            // Civic puts a band behind the header; the default has none.
            String plain = "light/none|" + render + "|900";
            String civic = "light/civic|" + render + "|900";
            assertEquals("rgba(0, 0, 0, 0)", one(plain, "card-header", "card-full", "block", "background-color"), plain);
            assertNotEquals("rgba(0, 0, 0, 0)", one(civic, "card-header", "card-full", "block", "background-color"), civic + ": data-card-header is what Civic selects");
            // Summit makes a card title bolder than the default 600.
            assertEquals("700", one("light/summit|" + render + "|900", "card-title", "card-full", "block", "font-weight"), render);
            assertEquals("600", one(plain, "card-title", "card-full", "block", "font-weight"), plain);
            // Axiom, Civic and Summit colour the footer's top rule.
            assertNotEquals(one(plain, "card-footer", "card-full", "block", "border-top-color"), one("light/axiom|" + render + "|900", "card-footer", "card-full", "block", "border-top-color"), render);
            // A skin that sets --radius-xl rounds both components; one that sets --color-white repaints them.
            assertEquals("6px", one("light/foundry|" + render + "|900", "card", "card-full", "block", "border-top-left-radius"), render);
            assertEquals("6px", one("light/foundry|" + render + "|900", "stat-card", "stat-full", "block", "border-top-left-radius"), render);
            assertNotEquals(one(plain, "card", "card-full", "block", "background-color"), one("light/foundry|" + render + "|900", "card", "card-full", "block", "background-color"), render);
            // The skins' structural value rule reaches the first span of the first dd (Summit's value is bolder).
            assertEquals("700", one("light/summit|" + render + "|900", "stat-card-value", "stat-full", "block", "font-weight"), render);
        }
    }

    @Test
    void darkModeUsesTailwindsDarkColoursForBothComponentsInBothRenderings() {
        for (String render : List.of("L", "P", "R")) {
            String light = "light/none|" + render + "|900";
            String dark = "dark/none|" + render + "|900";
            for (String[] part : new String[][] {{"card", "card-full", "background-color"}, {"card", "card-full", "border-top-color"}, {"card-title", "card-full", "color"},
                    {"card-body", "card-full", "color"}, {"card-footer", "card-full", "background-color"}, {"stat-card", "stat-full", "background-color"},
                    {"stat-card", "stat-full", "border-top-color"}, {"stat-card-label", "stat-full", "color"}, {"stat-card-value", "stat-full", "color"},
                    {"stat-card-description", "stat-full", "color"}}) {
                assertNotEquals(one(light, part[0], part[1], "block", part[2]), one(dark, part[0], part[1], "block", part[2]), render + " " + part[0] + " " + part[2]);
            }
        }
    }

    @Test
    void theCasesLayoutsAndWidthsAreCompleteAndTheLayoutIsReallyMeasuredNotAbsent() {
        List<ParityProbe.Row> rows = probe.phase("light/none|L|900");
        Map<String, Integer> byKind = new TreeMap<>();
        rows.forEach(r -> byKind.merge(r.kind(), 1, Integer::sum));
        // Every part the fixtures write is measured, in every layout.
        for (String kind : List.of("card", "card-header", "card-title", "card-body", "card-footer", "stat-card", "stat-card-list", "stat-card-label", "stat-card-values",
                "stat-card-value", "stat-card-change", "stat-card-description")) {
            int written = count(legacyMarkupForCounting, "data-parity=\"" + kind + "\"");
            assertTrue(written > 0, kind);
            assertEquals(written, byKind.get(kind), kind + " rows");
        }
        // 12 Cards and 8 Stat Cards per layout (one of the Cards is nested).
        assertEquals(LAYOUTS.size() * 12, byKind.get("card"));
        assertEquals(LAYOUTS.size() * 8, byKind.get("stat-card"));
        assertEquals(LAYOUTS.size() * 8, byKind.get("stat-card-list"));
        assertTrue(byKind.get("raw") > 0);
        for (String key : ROWS.keySet().stream().filter(k -> k.startsWith("light/none|L|")).toList()) {
            assertEquals(ROWS.get("light/none|L|900"), ROWS.get(key), "every width measures the same elements: " + key);
        }
        assertTrue(ROWS.values().stream().allMatch(n -> n > 0));
    }

    @Test
    void theOracleIsNotVacuousEveryLegacyClassIsCompiledInTheCumulativeAndScopedStylesheets() throws IOException {
        Path dir = Path.of(System.getProperty("ashlar.cascade.dir"));
        String oracle = Files.readString(dir.resolve("card-oracle.css"), StandardCharsets.UTF_8);
        String scoped = Files.readString(dir.resolve("card-legacy-scoped.css"), StandardCharsets.UTF_8);
        List<String> tokens = new ArrayList<>(legacy.allTokens());
        assertEquals(List.of(), ParityOracle.uncompiled(oracle, tokens), "the oracle does not compile these");
        assertEquals(List.of(), ParityOracle.uncompiled(scoped, tokens), "the scoped stylesheet does not compile these");
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
        earlier.addAll(new PageCompositionLegacyMarkup().allTokens());
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

    private static final Pattern CARD_START = Pattern.compile("<div class=\"ash-card\" data-card>");
    private static final Pattern HEADER_WITH_TITLE = Pattern.compile("<div class=\"ash-card__header\" data-card-header>\\s*<h3 class=\"ash-card__title\">");
    private static final Pattern FOOTER_HOOK = Pattern.compile("<div class=\"ash-card__footer\" data-card-footer>");
    private static final Pattern STAT_STRUCTURE = Pattern.compile("<div class=\"ash-stat-card\" data-stat-card>\\s*<dl class=\"ash-stat-card__list\">\\s*<dt class=\"ash-stat-card__label\">[^<]*</dt>\\s*<dd class=\"ash-stat-card__values\">\\s*<span class=\"ash-stat-card__value\">");

    /** Hand-written panels on the showcase pages still use these utilities; a Card or Stat Card (an element carrying its hook) must not. */
    private static final Pattern TAILWIND_ON_A_HOOK = Pattern.compile("class=\"[^\"]*(?:rounded-xl|bg-white|border-slate|px-6|p-6)[^\"]*\" data-(?:card|stat-card)(?:-header|-footer)?[ >]");

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }

    @Test
    void everyCardAndStatCardTheRealPagesServeKeepsItsNativeStructureAndHooks() throws Exception {
        ParitySession session = ParitySession.open("card-parity");
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
                for (String role : CardLegacyMarkup.MIGRATED.keySet()) {
                    int n = CardLegacyMarkup.count(body, role);
                    totals.merge(role, n, Integer::sum);
                    if (n > 0) {
                        perPage.merge(role + " " + path, n, Integer::sum);
                    }
                }
                // Every class attribute that carries one of the two components' classes is a known part (the rewrite throws otherwise).
                legacy.rewrite(body);
                int cards = CardLegacyMarkup.count(body, "card");
                int headers = CardLegacyMarkup.count(body, "card-header");
                int footers = CardLegacyMarkup.count(body, "card-footer");
                int stats = CardLegacyMarkup.count(body, "stat-card");
                if (CARD_START.matcher(body).results().count() != cards) {
                    problems.add(path + ": a Card is not a div carrying data-card");
                }
                if (HEADER_WITH_TITLE.matcher(body).results().count() != headers || CardLegacyMarkup.count(body, "card-title") != headers) {
                    problems.add(path + ": a Card header is not a data-card-header div holding the h3 title, or a title is outside a header");
                }
                if (count(body, "data-card-header") != headers) {
                    problems.add(path + ": data-card-header is on something that is not a Card header: " + count(body, "data-card-header") + " vs " + headers);
                }
                if (FOOTER_HOOK.matcher(body).results().count() != footers || count(body, "data-card-footer") != footers) {
                    problems.add(path + ": a Card footer does not carry data-card-footer exactly once");
                }
                if (STAT_STRUCTURE.matcher(body).results().count() != stats || CardLegacyMarkup.count(body, "stat-card-list") != stats
                        || CardLegacyMarkup.count(body, "stat-card-label") != stats || CardLegacyMarkup.count(body, "stat-card-value") != stats
                        || CardLegacyMarkup.count(body, "stat-card-values") != stats) {
                    problems.add(path + ": a Stat Card does not keep its div > dl > dt + dd > span structure");
                }
                if (count(body, "data-stat-card") != stats) {
                    problems.add(path + ": data-stat-card count differs from the Stat Cards");
                }
                if (TAILWIND_ON_A_HOOK.matcher(body).find()) {
                    problems.add(path + ": a Card or Stat Card still renders its Tailwind classes");
                }
            }
        }
        assertEquals(List.of(), problems);
        for (String role : CardLegacyMarkup.MIGRATED.keySet()) {
            if (!role.equals(CardLegacyMarkup.LIST_ROLE) || totals.getOrDefault(role, 0) > 0) {
                assertTrue(totals.getOrDefault(role, 0) > 0, "no " + role + " rendered on any page: " + totals);
            }
        }
        assertEquals(totals.get("stat-card"), totals.get("stat-card-list"));
        assertEquals(totals.get("card-header"), totals.get("card-title"));
        assertTrue(perPage.containsKey("card /ashlar/components/card"), perPage.toString());
        assertTrue(perPage.containsKey("card-footer /ashlar/components/card"), perPage.toString());
        assertTrue(perPage.containsKey("stat-card-change /ashlar/components/stat-card"), perPage.toString());
        assertTrue(perPage.containsKey("stat-card-description /ashlar/components/stat-card"), perPage.toString());
        assertEquals(3, perPage.get("stat-card /ashlar/"), "the home page's Account Registry shows three Stat Cards");
    }
}
