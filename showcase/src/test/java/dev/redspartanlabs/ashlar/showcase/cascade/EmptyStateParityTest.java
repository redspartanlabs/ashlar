package dev.redspartanlabs.ashlar.showcase.cascade;

import dev.redspartanlabs.ashlar.icon.Icon;
import dev.redspartanlabs.ashlar.icon.IconSvg;
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
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Empty State's parity gates, measured in real Chrome.
 *
 * <p>Evidence comes in two kinds, and neither stores a baseline. (1) A controlled oracle: the Empty State's legacy
 * Tailwind markup (compiled by the real Tailwind CLI) next to its migrated markup, in the same browser, for each of the
 * 22 theme and skin configurations, with Tailwind present and with Tailwind removed. It crosses an optional icon, seven
 * kinds of body (none, plain text, one paragraph, two paragraphs, content wider than the body's maximum width, long
 * wrapping text and an empty body) and three kinds of action (none, one box, three boxes), plus a long title and a title
 * that is one unbroken word, and places Empty States in narrow, flex-column, flex-row, Card-like and Table-like
 * surroundings. (2) Every Empty State on the real showcase pages, in the same 22 configurations: the real page is
 * measured as served, then the very same elements get their legacy classes back and are measured again, in the same
 * run. Differences are compared with {@link ParityNormalizer} (exact equality except H1 to H4, with their
 * preconditions). Each row also carries the element's box in its parent's coordinates. Negative controls show each
 * gate can fail. Facts that do not depend on the legacy rendering (the icon is 40px square, the vertical padding is
 * 48px, the body is at most 384px wide, Foundry's shadow and Harbor's paragraph line height still apply, the colours
 * are the skin's) are checked directly.
 *
 * <p>The caller's own content (a body paragraph, an action's Button) belongs to the caller. The controlled oracle uses
 * inert boxes and plain paragraphs for it. On the real pages, with Tailwind present, it is the real content and is
 * compared like everything else; with Tailwind removed, a Button is no longer styled by anything, so every action's
 * content is replaced by the same inert box in both renderings before anything is measured.
 *
 * <p>Evidence is Chrome only. Computed-style and box parity is the visual evidence; there is no screenshot comparison.
 */
class EmptyStateParityTest {

    private static final String ASHLAR = "/ashlar-real/ashlar.css";
    private static final String ORACLE = "/generated/empty-state-oracle.css";
    private static final String APP = "/css/app.css";
    private static final String ORACLE_VIRTUAL = "/__parity/empty-state-oracle.css";
    private static final String SCOPED = "/generated/empty-state-legacy-scoped.css";
    private static final String SCOPED_VIRTUAL = "/__parity/empty-state-legacy-scoped.css";
    private static final List<String> SKINS = List.of("axiom", "civic", "fathom", "foundry", "harbor", "keystone", "ledger", "sterling", "summit", "veridian");
    private static final List<String> SKIN_LINKS = SKINS.stream().map(s -> "/css/theme-" + s + ".css").toList();

    /** Every page that renders an Empty State, and how many it renders (all but the Empty State page render them through Table). */
    private static final Map<String, Integer> EXPECTED_PAGES = expectedPages();

    private static Map<String, Integer> expectedPages() {
        Map<String, Integer> pages = new TreeMap<>();
        pages.put("/ashlar/", 2);
        pages.put("/ashlar/components/badge", 2);
        pages.put("/ashlar/components/data-table", 8);
        pages.put("/ashlar/components/empty-state", 6);
        pages.put("/ashlar/components/faceted-search", 2);
        pages.put("/ashlar/components/filter-bar", 2);
        pages.put("/ashlar/components/page-shell", 2);
        pages.put("/ashlar/components/pagination", 2);
        pages.put("/ashlar/components/table", 14);
        return pages;
    }

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

    enum Body { NONE, TEXT, PARAGRAPH, TWO_PARAGRAPHS, WIDE, LONG_TEXT, EMPTY }

    enum Action { NONE, ONE, THREE }

    /** One Empty State of the controlled oracle. {@code icon} is null for no icon, else an Icon constant's name. */
    private record Case(String layout, String icon, Body body, Action action, String title) {
        String shape() {
            return (icon != null ? "i" : "-") + (body != Body.NONE ? "b" : "-") + (action != Action.NONE ? "a" : "-");
        }

        /** The body's direct element children. */
        int bodyChildren() {
            return switch (body) {
                case PARAGRAPH -> 1;
                case TWO_PARAGRAPHS, WIDE -> 2;
                default -> 0;
            };
        }

        /** The body's direct paragraph children. */
        int paragraphs() {
            return switch (body) {
                case PARAGRAPH, WIDE -> 1;
                case TWO_PARAGRAPHS -> 2;
                default -> 0;
            };
        }

        int actionChildren() {
            return switch (action) {
                case ONE -> 1;
                case THREE -> 3;
                default -> 0;
            };
        }

        /** The rows the probe reports for this Empty State: wrapper, icon and glyph, title, body and its children, action and its children. */
        int rows() {
            return 1 + (icon != null ? 2 : 0) + 1 + (body != Body.NONE ? 1 + bodyChildren() : 0) + (action != Action.NONE ? 1 + actionChildren() : 0);
        }
    }

    private static final String TITLE = "No results found";
    private static final String LONG_TITLE = "There are no migration jobs scheduled or currently running for this workspace in the selected period";
    private static final String LONG_WORD = "Supercalifragilisticexpialidocious_Supercalifragilisticexpialidocious_Supercalifragilistic";
    private static final String LONG_TEXT = "Create your first project to start tracking work, invite the people you work with, and connect the systems "
            + "whose activity you want to see in one place. Nothing is lost if you start small.";

    private static final List<Case> CASES = cases();

    private static List<Case> cases() {
        List<Case> all = new ArrayList<>();
        for (String icon : new String[] {null, "INBOX"}) {
            for (Body body : Body.values()) {
                for (Action action : Action.values()) {
                    all.add(new Case("block", icon, body, action, TITLE));
                }
            }
        }
        all.add(new Case("block", "SEARCH", Body.TEXT, Action.THREE, LONG_TITLE));
        all.add(new Case("block", "SEARCH", Body.TEXT, Action.THREE, LONG_WORD));
        all.add(new Case("narrow", "INBOX", Body.PARAGRAPH, Action.THREE, TITLE));
        all.add(new Case("narrow", null, Body.WIDE, Action.ONE, LONG_TITLE));
        all.add(new Case("narrow", "INBOX", Body.LONG_TEXT, Action.THREE, TITLE));
        all.add(new Case("flex-column", "INBOX", Body.TEXT, Action.NONE, TITLE));
        all.add(new Case("flex-column", "SEARCH", Body.PARAGRAPH, Action.ONE, TITLE));
        all.add(new Case("flex-row", "INBOX", Body.TEXT, Action.NONE, TITLE));
        all.add(new Case("flex-row", "SEARCH", Body.PARAGRAPH, Action.ONE, TITLE));
        all.add(new Case("card", "INBOX", Body.TEXT, Action.ONE, TITLE));
        all.add(new Case("card", null, Body.NONE, Action.NONE, TITLE));
        // Table's two variants: the "none" region (an empty body) and the "filtered" region (a paragraph), each in a padded region.
        all.add(new Case("table", "INBOX", Body.EMPTY, Action.NONE, "No records found"));
        all.add(new Case("table", "INBOX", Body.PARAGRAPH, Action.NONE, "No records found"));
        all.add(new Case("table", "SEARCH", Body.PARAGRAPH, Action.NONE, "No matching records"));
        return List.copyOf(all);
    }

    private static final Map<String, String> LAYOUT_STYLE = Map.of(
            "block", "box-sizing:border-box;width:400px",
            "narrow", "box-sizing:border-box;width:200px",
            "flex-column", "box-sizing:border-box;display:flex;flex-direction:column;gap:12px;width:400px",
            "flex-row", "box-sizing:border-box;display:flex;flex-direction:row;gap:12px;width:400px",
            "card", "box-sizing:border-box;width:400px",
            "table", "box-sizing:border-box;width:400px");
    private static final List<String> LAYOUT_ORDER = List.of("block", "narrow", "flex-column", "flex-row", "card", "table");

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
    private static final Map<String, Integer> EMPTY_STATES_PER_PAGE = new TreeMap<>();
    private static final List<String> FACT_PROBLEMS = new ArrayList<>();
    private static final List<String> SHAPE_PROBLEMS = new ArrayList<>();
    private static int realComparisonsPerMode;
    private static int realRowsRemoved;
    private static int realParagraphsRemoved;
    private static long realSeconds;

    // ---- markup ----

    private static EmptyStateLegacyMarkup legacy;

    private static String bodyHtml(Body body) {
        return switch (body) {
            case TEXT -> "Try adjusting your filters or search terms.";
            case PARAGRAPH -> "<p>No rows match your search. Try a shorter or different term.</p>";
            case TWO_PARAGRAPHS -> "<p>The first paragraph of supporting text.</p><p>A second paragraph that follows it.</p>";
            case WIDE -> "<div style=\"box-sizing:border-box;width:600px;height:20px;background:#94a3b8\"></div><p>" + LONG_WORD + "</p>";
            case LONG_TEXT -> LONG_TEXT;
            default -> "";
        };
    }

    private static String actionHtml(Action action) {
        String box = "<span style=\"display:inline-block;box-sizing:border-box;width:%dpx;height:%dpx;background:#94a3b8\"></span>";
        return switch (action) {
            case ONE -> box.formatted(96, 36);
            case THREE -> box.formatted(96, 36) + box.formatted(120, 32) + box.formatted(80, 40);
            default -> "";
        };
    }

    /** One Empty State exactly as empty-state.jte renders it (the template itself is measured on the real pages). */
    private static String emptyState(Case c) {
        StringBuilder sb = new StringBuilder("<div class=\"ash-empty-state\" data-empty-state>\n");
        if (c.icon() != null) {
            sb.append("    <div class=\"ash-empty-state__icon\">\n        <svg class=\"inline-block shrink-0 ash-empty-state__glyph\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\""
                    + " stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\">")
                    .append(IconSvg.bodyFor(Icon.valueOf(c.icon()))).append("</svg>\n    </div>\n");
        }
        sb.append("    <h3 class=\"ash-empty-state__title\">").append(c.title()).append("</h3>\n");
        if (c.body() != Body.NONE) {
            sb.append("    <div class=\"ash-empty-state__body\">\n        ").append(bodyHtml(c.body())).append("\n    </div>\n");
        }
        if (c.action() != Action.NONE) {
            sb.append("    <div class=\"ash-empty-state__action\">\n        ").append(actionHtml(c.action())).append("\n    </div>\n");
        }
        return sb.append("</div>").toString();
    }

    private static String surround(Case c) {
        return switch (c.layout()) {
            case "flex-column", "flex-row" -> emptyState(c);
            case "card" -> "<div data-card style=\"box-sizing:border-box;border:1px solid #cbd5e1;border-radius:8px;padding:16px;margin:0 0 12px\">" + emptyState(c) + "</div>";
            case "table" -> "<div data-table-empty=\"none\" style=\"box-sizing:border-box;padding:2.5rem 1rem;margin:0 0 12px\">" + emptyState(c) + "</div>";
            default -> "<div style=\"margin:0 0 12px\">" + emptyState(c) + "</div>";
        };
    }

    private static String markup(boolean legacyClasses) {
        StringBuilder sb = new StringBuilder();
        for (String layout : LAYOUT_ORDER) {
            sb.append("<div data-layout=\"").append(layout).append("\" style=\"").append(LAYOUT_STYLE.get(layout)).append("\">");
            for (Case c : CASES) {
                if (c.layout().equals(layout)) {
                    sb.append(surround(c));
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
        return "<iframe data-id=\"" + id + "\" style=\"width:440px;height:16000px;border:0\" srcdoc=\""
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
     * their default text in black, and that would be a difference in the test page, not in the Empty State.
     */
    private static String environment(Path generatedDir) throws IOException {
        return PageEnvironment.tailwindRemovedRules(generatedDir.resolve("phase-c-baseline.css")) + ":root[data-theme=\"dark\"]{color-scheme:dark}\n";
    }

    // ---- control experiments: each breaks the migrated Empty State in ONE way and must be detected ----

    /** {@code removed}: measured with only Ashlar's stylesheet (needed for what Tailwind's utilities would otherwise hide). */
    private record Control(String name, String config, boolean removed, String css, String expectedProperty) {
    }

    private static final String DARK = ":where([data-theme=dark],[data-theme=dark] *)";

    private static final List<Control> CONTROL_LIST = List.of(
            new Control("wrong icon colour", "light/none", false, ".ash-empty-state{--ash-empty-state-icon:red}", "color"),
            new Control("wrong title colour", "light/none", false, ".ash-empty-state{--ash-empty-state-title:red}", "color"),
            new Control("wrong body colour", "light/none", false, ".ash-empty-state{--ash-empty-state-body:red}", "color"),
            new Control("wrong dark icon colour", "dark/none", false, ".ash-empty-state" + DARK + "{--ash-empty-state-icon:var(--color-slate-400)}", "color"),
            new Control("wrong dark title colour", "dark/none", false, ".ash-empty-state" + DARK + "{--ash-empty-state-title:var(--color-slate-900)}", "color"),
            new Control("wrong dark body colour", "dark/none", false, ".ash-empty-state" + DARK + "{--ash-empty-state-body:var(--color-slate-500)}", "color"),
            new Control("title colour ignores the skin", "light/civic", false, ".ash-empty-state{--ash-empty-state-title:#0f172a}", "color"),
            new Control("vertical padding changed", "light/none", false, ".ash-empty-state{padding-block:2.5rem}", "padding-top"),
            new Control("horizontal padding changed", "light/none", false, ".ash-empty-state{padding-inline:1rem}", "padding-left"),
            new Control("items no longer centred", "light/none", false, ".ash-empty-state{align-items:flex-start}", "align-items"),
            new Control("text no longer centred", "light/none", false, ".ash-empty-state{text-align:left}", "text-align"),
            new Control("wrapper no longer a column", "light/none", false, ".ash-empty-state{flex-direction:row}", "flex-direction"),
            new Control("icon box margin changed", "light/none", false, ".ash-empty-state__icon{margin-bottom:.5rem}", "margin-bottom"),
            new Control("glyph too narrow", "light/none", false, ".ash-empty-state__glyph{width:2rem}", "width"),
            new Control("glyph too short", "light/none", false, ".ash-empty-state__glyph{height:2rem}", "height"),
            new Control("glyph loses vertical-align: middle", "light/none", false, ".ash-empty-state__glyph{vertical-align:baseline}", "vertical-align"),
            new Control("glyph display is not inline-block", "light/none", true, ".ash-empty-state__glyph{display:inline}", "display"),
            new Control("glyph loses flex-shrink: 0", "light/none", true, ".ash-empty-state__glyph{flex-shrink:1}", "flex-shrink"),
            new Control("title weight changed", "light/none", false, ".ash-empty-state__title{font-weight:400}", "font-weight"),
            new Control("title size changed", "light/none", false, ".ash-empty-state__title{font-size:1.125rem}", "font-size"),
            new Control("title line height changed", "light/none", false, ".ash-empty-state__title{line-height:1}", "line-height"),
            new Control("title keeps the browser's heading margin", "light/none", true, ".ash-empty-state__title{margin:1em 0}", "margin-top"),
            new Control("body margin changed", "light/none", false, ".ash-empty-state__body{margin-top:.5rem}", "margin-top"),
            new Control("body max width changed", "light/none", false, ".ash-empty-state__body{max-width:30rem}", "max-width"),
            new Control("body text size changed", "light/none", false, ".ash-empty-state__body{font-size:1rem}", "font-size"),
            new Control("body line height changed", "light/none", false, ".ash-empty-state__body{line-height:1}", "line-height"),
            new Control("body paragraph keeps the browser's margin", "light/none", true, ".ash-empty-state__body>p{margin:1em 0}", "margin-top"),
            new Control("action gap changed", "light/none", false, ".ash-empty-state__action{gap:.5rem}", "column-gap"),
            new Control("action margin changed", "light/none", false, ".ash-empty-state__action{margin-top:.75rem}", "margin-top"),
            new Control("action no longer wraps", "light/none", false, ".ash-empty-state__action{flex-wrap:nowrap}", "flex-wrap"),
            new Control("action no longer centred", "light/none", false, ".ash-empty-state__action{justify-content:flex-start}", "justify-content"),
            new Control("action items start-aligned", "light/none", false, ".ash-empty-state__action{align-items:flex-start}", "align-items"));

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

    /** The one use of the paragraph box-sizing allowance (see {@link #removedNormalizationProblems}), counted next to the H2 uses. */
    private static final String PARAGRAPH_BOX_SIZING = "PARAGRAPH box-sizing";

    /**
     * Compares two renderings row by row. When {@code tailwindRemoved} is true, ONE difference beyond the normalizer's rules
     * is allowed and counted: a direct paragraph child of the body, which the CALLER wrote, has {@code box-sizing: content-box}
     * where Tailwind's preflight gave it {@code border-box}. That is the browser default for content nobody styled, the
     * paragraph has no padding or border, and Ashlar deliberately sets nothing on caller content except the approved margin.
     * That last claim is a precondition and not an assumption: the difference is excused only when all four paddings and all
     * four border widths are {@code 0px} on BOTH rows (the one case where the two box models cannot differ in size), and a
     * row where any of them is missing or nonzero is a finding.
     */
    private static List<Finding> compare(String where, List<String> propNames, List<Row> expected, List<Row> actual, Map<String, Integer> applied,
                                        boolean tailwindRemoved) {
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
            for (ParityNormalizer.Difference d : r.remaining()) {
                if (tailwindRemoved && e.kind().equals("body-p") && d.property().equals("box-sizing")
                        && "border-box".equals(d.expected()) && "content-box".equals(d.actual())
                        && hasNoPaddingOrBorder(propNames, e) && hasNoPaddingOrBorder(propNames, a)) {
                    applied.merge(PARAGRAPH_BOX_SIZING, 1, Integer::sum);
                } else {
                    findings.add(new Finding(label, d.property(), d.expected(), d.actual()));
                }
            }
            r.applied().forEach(rule -> applied.merge(rule, 1, Integer::sum)); // "H2 border-top-style": the rule and the property it covered
        }
        return findings;
    }

    private static final List<String> BOX_EXTRAS = List.of("padding-top", "padding-right", "padding-bottom", "padding-left",
            "border-top-width", "border-right-width", "border-bottom-width", "border-left-width");

    /** True only when every padding and border width of the row was measured and is exactly {@code 0px}. */
    private static boolean hasNoPaddingOrBorder(List<String> propNames, Row row) {
        for (String property : BOX_EXTRAS) {
            int index = propNames.indexOf(property);
            if (index < 0 || !"0px".equals(row.values()[index])) {
                return false;
            }
        }
        return true;
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
            System.out.println("[empty-state-parity] Chrome not found - Empty State parity SKIPPED (not passed).");
            Assumptions.abort("Chrome not found; the Empty State parity checks were not run");
        }
        HeadlessChrome chrome = new HeadlessChrome(chromePath.get());
        legacy = new EmptyStateLegacyMarkup();
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
        try (InputStream in = EmptyStateParityTest.class.getResourceAsStream("/cascade/empty-state-probe.js")) {
            probeJs = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String page = "<!doctype html><html><body style=\"margin:0\">\n" + frames + "<script>" + probeJs + "</script>\n<script>"
                + "addEventListener('load',function(){setTimeout(function(){var out=['props\\t'+window.__ashEmptyState.PROPS.join(',')];"
                + "[].forEach.call(document.querySelectorAll('iframe'),function(f){var id=f.getAttribute('data-id');"
                + "window.__ashEmptyState.measure(f.contentWindow,f.contentDocument,id).forEach(function(r){out.push(r)});"
                + "window.__ashEmptyState.tokens(f.contentWindow,f.contentDocument,id).forEach(function(r){out.push(r)})});"
                + "var pre=document.createElement('pre');pre.id='ash-empty-state-frames';pre.textContent=out.join('\\n');document.body.appendChild(pre)},400)})"
                + "</script></body></html>";
        try (CascadeFixtureServer server = new CascadeFixtureServer(generatedDir)) {
            String dom = chrome.dumpDom(server.registerRaw(page), List.of("--hide-scrollbars"));
            return parse(lines(dom, "ash-empty-state-frames"), what);
        }
    }

    private static void runControlledFrames(HeadlessChrome chrome, Path generatedDir) throws Exception {
        String environment = environment(generatedDir);
        String migrated = markup(false);
        String legacyMarkup = markup(true);
        List<Row> allRows = new ArrayList<>();
        Map<String, Map<String, String>> allTokens = new LinkedHashMap<>();
        Probe first = null;
        // 22 configurations in runs of two, four frames each: the full oracle, Tailwind present, Tailwind removed, scoped legacy.
        for (int start = 0; start < CONFIGS.size(); start += 2) {
            StringBuilder frames = new StringBuilder();
            for (Config c : CONFIGS.subList(start, Math.min(CONFIGS.size(), start + 2))) {
                frames.append(frame(c.id() + "|L", c, with(ORACLE, SKIN_LINKS), "", legacyMarkup));
                frames.append(frame(c.id() + "|P", c, with(APP, SKIN_LINKS), "", migrated));
                frames.append(frame(c.id() + "|R", c, with(ASHLAR, SKIN_LINKS), environment, migrated));
                // The legacy Empty State from the scoped stylesheet, in the same unstyled environment as |R.
                frames.append(frame(c.id() + "|S", c, with(SCOPED, SKIN_LINKS), environment, legacyMarkup));
            }
            Probe chunk = runFrames(chrome, generatedDir, frames.toString(), "controlled frames " + start);
            first = first == null ? chunk : first;
            allRows.addAll(chunk.rows());
            chunk.tokens().forEach(allTokens::put);
        }
        for (int start = 0; start < CONTROL_LIST.size(); start += 8) {
            StringBuilder controls = new StringBuilder();
            for (Control control : CONTROL_LIST.subList(start, Math.min(CONTROL_LIST.size(), start + 8))) {
                controls.append(control.removed()
                        ? frame("ctl:" + control.name() + "|C", config(control.config()), with(ASHLAR, SKIN_LINKS), environment + control.css(), migrated)
                        : frame("ctl:" + control.name() + "|C", config(control.config()), with(APP, SKIN_LINKS), control.css(), migrated));
            }
            allRows.addAll(runFrames(chrome, generatedDir, controls.toString(), "control frames " + start).rows());
        }
        // The customisation surface: an instance property wins over the class, and an unlayered rule wins over the component, in both themes.
        Case sample = new Case("ovr", "INBOX", Body.TEXT, Action.ONE, TITLE);
        String instance = emptyState(sample).replace("data-empty-state>",
                "data-empty-state style=\"--ash-empty-state-icon: rgb(1, 2, 3); --ash-empty-state-title: rgb(4, 5, 6); --ash-empty-state-body: rgb(7, 8, 9)\">");
        String unlayeredCss = "[data-empty-state]{--ash-empty-state-icon: rgb(10, 11, 12); --ash-empty-state-title: rgb(13, 14, 15); --ash-empty-state-body: rgb(16, 17, 18)}";
        String wrapped = "<div data-layout=\"ovr\" style=\"box-sizing:border-box;width:400px\">%s</div>";
        StringBuilder overrides = new StringBuilder();
        for (String theme : List.of("light", "dark")) {
            overrides.append(frame("ovr:inline-" + theme + "|P", config(theme + "/none"), with(APP, SKIN_LINKS), "", wrapped.formatted(instance)));
            overrides.append(frame("ovr:unlayered-" + theme + "|P", config(theme + "/none"), with(APP, SKIN_LINKS), unlayeredCss, wrapped.formatted(emptyState(sample))));
        }
        allRows.addAll(runFrames(chrome, generatedDir, overrides.toString(), "override frames").rows());
        Probe probe = new Probe(first.props(), first.meta(), List.of(), allTokens, allRows);
        props = probe.props();
        int expectedRows = CASES.stream().mapToInt(Case::rows).sum();
        for (Config c : CONFIGS) {
            List<Row> legacyRows = probe.phase(c.id() + "|L");
            CONTROLLED_ROWS.put(c.id(), legacyRows.size());
            assertEquals(expectedRows, legacyRows.size(), c.id() + ": the rows the cases must produce");
            CONTROLLED_PRESENT.put(c.id(), compare(c.id() + " (Tailwind present)", props, legacyRows, probe.phase(c.id() + "|P"), APPLIED_PRESENT, false));
            // Tailwind removed: against the legacy Empty State given the same unstyled surroundings ...
            CONTROLLED_REMOVED_SCOPED.put(c.id(), compare(c.id() + " (Tailwind removed vs scoped legacy)", props, probe.phase(c.id() + "|S"), probe.phase(c.id() + "|R"), APPLIED_REMOVED, true));
            // ... and against the full legacy rendering, which also has Tailwind's preflight around it.
            CONTROLLED_REMOVED_FULL.put(c.id(), compare(c.id() + " (Tailwind removed vs full legacy)", props, legacyRows, probe.phase(c.id() + "|R"), APPLIED_REMOVED, true));
            // Fidelity of the scoped legacy stylesheet: it must render the legacy Empty State exactly as the full one does.
            SCOPED_FIDELITY.put(c.id(), compare(c.id() + " (scoped legacy vs full legacy)", props, legacyRows, probe.phase(c.id() + "|S"), APPLIED_SCOPED, false));
        }
        for (Control control : CONTROL_LIST) {
            // A control measured with Tailwind present is compared with the full legacy rendering; one measured without it, with the scoped one.
            List<Row> expected = probe.phase(control.config() + (control.removed() ? "|S" : "|L"));
            CONTROLS.put(control.name(), compare("control: " + control.name(), props, expected, probe.phase("ctl:" + control.name() + "|C"), new TreeMap<>(), control.removed()));
        }
        FRAME_PROBES.put("controlled", probe);
        for (Config c : CONFIGS) {
            controlledFacts(c, probe);
        }
    }

    // -- real pages: every Empty State on every page that renders one, 22 configurations, in parallel --

    private static String legacyConfigJson(String phase, String stylesheet, boolean detach, boolean neutralize) {
        return "{\"phase\":\"" + phase + "\",\"neutralize\":" + neutralize + ",\"legacy\":{\"stylesheet\":\"" + stylesheet + "\",\"detach\":" + detach
                + ",\"map\":" + legacy.mapJson() + "}}";
    }

    private static String pageKey(String path, Config config, String mode) {
        return path + "|" + config.id() + "|" + mode;
    }

    private static void runRealPages(HeadlessChrome chrome, Path generatedDir, Path bootJar) throws Exception {
        byte[] oracleCss = Files.readAllBytes(generatedDir.resolve("empty-state-oracle.css"));
        byte[] ashlarCss = Files.readAllBytes(Path.of(System.getProperty("ashlar.css.generated")));
        String environment = environment(generatedDir);
        byte[] removedCss = (new String(ashlarCss, StandardCharsets.UTF_8) + "\n" + environment).getBytes(StandardCharsets.UTF_8);
        byte[] scopedCss = Files.readAllBytes(generatedDir.resolve("empty-state-legacy-scoped.css"));
        String presentConfig = legacyConfigJson("ash", ORACLE_VIRTUAL, false, false);
        // Tailwind removed: the migrated Empty State is measured in the unstyled page, then the legacy Empty State is measured on the
        // very same elements in the very same page with only its own utilities added - so the surroundings are identical.
        String removedConfig = legacyConfigJson("removed", SCOPED_VIRTUAL, true, true);
        Map<String, Probe> measured = new ConcurrentHashMap<>();
        int workers = Math.max(1, Math.min(3, Runtime.getRuntime().availableProcessors() / 2));
        List<RealPageProxy> proxies = Collections.synchronizedList(new ArrayList<>());
        ThreadLocal<RealPageProxy> proxyOfThread = new ThreadLocal<>();
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        try (RunningShowcase app = RunningShowcase.start(bootJar)) {
            List<Future<?>> futures = new ArrayList<>();
            for (String path : EXPECTED_PAGES.keySet()) {
                for (Config config : CONFIGS) {
                    futures.add(pool.submit(() -> {
                        RealPageProxy proxy = proxyOfThread.get();
                        if (proxy == null) {
                            proxy = new RealPageProxy(app.port());
                            proxies.add(proxy);
                            proxyOfThread.set(proxy);
                        }
                        String url = path + (config.skin() == null ? "" : "?skin=" + config.skin());
                        proxy.configure(new RealPageProxy.Load(config.theme(), "empty-state", null, "/cascade/empty-state-probe.js", presentConfig,
                                List.of(), Map.of(ORACLE_VIRTUAL, oracleCss), null));
                        measured.put(pageKey(path, config, "present"),
                                parse(lines(chrome.dumpDom(proxy.url(url), List.of("--hide-scrollbars")), "ash-empty-state-results"), url + " present"));
                        proxy.configure(new RealPageProxy.Load(config.theme(), "empty-state", removedCss, "/cascade/empty-state-probe.js", removedConfig,
                                List.of(), Map.of(SCOPED_VIRTUAL, scopedCss), null));
                        measured.put(pageKey(path, config, "removed"),
                                parse(lines(chrome.dumpDom(proxy.url(url), List.of("--hide-scrollbars")), "ash-empty-state-results"), url + " removed"));
                        return null;
                    }));
                }
            }
            for (Future<?> f : futures) {
                f.get();
            }
            countEmptyStatesOnEveryPage(app.port());
        } finally {
            pool.shutdownNow();
            proxies.forEach(RealPageProxy::close);
        }
        for (String path : EXPECTED_PAGES.keySet()) {
            for (Config config : CONFIGS) {
                Probe p = measured.get(pageKey(path, config, "present"));
                Probe r = measured.get(pageKey(path, config, "removed"));
                REAL_PROBES.put(pageKey(path, config, "present"), p);
                REAL_PROBES.put(pageKey(path, config, "removed"), r);
                String where = path + " " + config.id();
                for (Probe probed : List.of(p, r)) {
                    if (!config.theme().equals(probed.meta().get("data-theme")) || !(config.skin() == null ? "" : config.skin()).equals(probed.meta().get("data-skin"))) {
                        REAL_PRESENT.add(new Finding(where, "#configuration", config.id(), probed.meta().get("data-theme") + "/" + probed.meta().get("data-skin")));
                    }
                    if (probed.meta().containsKey("error")) {
                        REAL_PRESENT.add(new Finding(where, "#error", "", probed.meta().get("error")));
                    }
                }
                REAL_PRESENT.addAll(compare(where + " (Tailwind present)", p.props(), p.phase("legacy"), p.phase("ash"), APPLIED_REAL_PRESENT, false));
                REAL_REMOVED.addAll(compare(where + " (Tailwind removed)", p.props(), r.phase("legacy"), r.phase("removed"), APPLIED_REAL_REMOVED, true));
                realComparisonsPerMode += (int) p.phase("legacy").stream().filter(row -> row.kind().equals("wrapper")).count();
                for (String[] s : r.shapes()) {
                    realRowsRemoved += rowsOfShape(s);
                    realParagraphsRemoved += Integer.parseInt(s[12]);
                }
                realFacts(path, config, p);
                if (config.id().equals("light/none")) {
                    p.shapes().forEach(s -> shapeProblems(path, s));
                }
            }
        }
    }

    /** The rows the probe reports for one real-page Empty State, from its shape line: wrapper, icon and glyph, title, body and its children, action and its children. */
    private static int rowsOfShape(String[] s) {
        boolean icon = !s[5].isEmpty();
        boolean body = !s[7].isEmpty();
        boolean action = !s[8].isEmpty();
        return 1 + (icon ? 2 : 0) + 1 + (body ? 1 + Integer.parseInt(s[11]) : 0) + (action ? 1 + Integer.parseInt(s[13]) : 0);
    }

    /** Every catalog page, the home page and the index, as served: how many Empty State components each renders. */
    private static void countEmptyStatesOnEveryPage(int port) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        List<String> paths = new ArrayList<>(List.of("/ashlar/", "/ashlar/components"));
        for (ComponentPage page : ComponentCatalog.PAGES) {
            paths.add("/ashlar/components/" + page.slug());
        }
        for (String path : paths) {
            HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            assertEquals(200, response.statusCode(), path);
            int hooks = (int) Pattern.compile("\\bdata-empty-state\\b(?![-\\w])").matcher(response.body()).results().count();
            assertEquals(EmptyStateLegacyMarkup.count(response.body()), hooks, path + ": every data-empty-state must be a migrated Empty State");
            EMPTY_STATES_PER_PAGE.put(path, hooks);
        }
    }

    // -- facts: what must be true of the migrated Empty State whatever the legacy rendering did --

    private static void expect(String what, String expected, String actual) {
        if (expected == null || !expected.equals(actual)) {
            FACT_PROBLEMS.add(what + ": expected " + expected + " but was " + actual);
        }
    }

    /** Rows of each Empty State in a probe whose rows are in document order: ordinal -> rows by kind (the last row of a kind wins; paragraphs are checked separately). */
    private static Map<Integer, Map<String, Row>> byOrdinal(List<Row> rows) {
        Map<Integer, Map<String, Row>> map = new TreeMap<>();
        rows.forEach(r -> map.computeIfAbsent(r.ordinal(), k -> new LinkedHashMap<>()).put(r.kind(), r));
        return map;
    }

    private static void emptyStateFacts(String where, Config config, Probe p, List<Row> rows, int expectedCount, List<Case> controlledCases) {
        boolean dark = config.theme().equals("dark");
        boolean foundry = "foundry".equals(config.skin());
        boolean harbor = "harbor".equals(config.skin());
        Map<String, String> tok = p.tokens().get(where.startsWith("controlled") ? config.id() + "|P" : "page");
        Map<Integer, Map<String, Row>> parts = byOrdinal(rows);
        assertEquals(expectedCount, parts.size(), where + ": number of empty states");
        if (tok == null) {
            FACT_PROBLEMS.add(where + ": no palette tokens were measured, so the skin facts cannot be checked");
            return;
        }
        if (foundry) {
            if (tok.get("skin-shadow") == null || tok.get("skin-shadow").equals("none")) {
                FACT_PROBLEMS.add(where + ": Foundry gives a [data-empty-state] no shadow: " + tok.get("skin-shadow"));
            }
        }
        if (harbor) {
            expect(where + " Harbor's paragraph line height (1.65 of 14px)", "23.1px", tok.get("skin-p-line-height"));
        } else {
            assertNotEquals("23.1px", tok.get("skin-p-line-height"), where + ": only Harbor sets a paragraph line height of 1.65");
        }
        for (Map.Entry<Integer, Map<String, Row>> e : parts.entrySet()) {
            Map<String, Row> r = e.getValue();
            Case c = controlledCases == null ? null : controlledCases.get(e.getKey() - 1);
            String here = where + " #" + e.getKey() + " " + r.get("wrapper").variant() + ": ";
            Row wrapper = r.get("wrapper");
            // A region a script has not shown yet (Table's "filtered" region) has computed styles but no box, so only its geometry is skipped.
            boolean boxed = !wrapper.diagnostics().startsWith("boxes=0");
            expect(here + "wrapper padding-top", "48px", p.value(wrapper, "padding-top"));
            expect(here + "wrapper padding-bottom", "48px", p.value(wrapper, "padding-bottom"));
            expect(here + "wrapper padding-left", "24px", p.value(wrapper, "padding-left"));
            expect(here + "wrapper padding-right", "24px", p.value(wrapper, "padding-right"));
            expect(here + "wrapper display", "flex", p.value(wrapper, "display"));
            expect(here + "wrapper direction", "column", p.value(wrapper, "flex-direction"));
            expect(here + "wrapper align-items", "center", p.value(wrapper, "align-items"));
            expect(here + "wrapper text-align", "center", p.value(wrapper, "text-align"));
            expect(here + "wrapper shadow is what the skin gives an Empty State", tok.get("skin-shadow"), p.value(wrapper, "box-shadow"));
            Row title = r.get("title");
            expect(here + "title font size", "16px", p.value(title, "font-size"));
            expect(here + "title line height", "24px", p.value(title, "line-height"));
            expect(here + "title weight", "600", p.value(title, "font-weight"));
            expect(here + "title margin-top", "0px", p.value(title, "margin-top"));
            expect(here + "title margin-bottom", "0px", p.value(title, "margin-bottom"));
            if (r.containsKey("icon")) {
                Row icon = r.get("icon");
                Row glyph = r.get("glyph");
                expect(here + "icon box margin-bottom", "12px", p.value(icon, "margin-bottom"));
                if (boxed) {
                    expect(here + "icon width", "40", p.value(glyph, "@w"));
                    expect(here + "icon height", "40", p.value(glyph, "@h"));
                }
                expect(here + "icon display", "inline-block", p.value(glyph, "display"));
                expect(here + "icon vertical-align", "middle", p.value(glyph, "vertical-align"));
                expect(here + "icon flex-shrink", "0", p.value(glyph, "flex-shrink"));
                expect(here + "icon stroke is the icon colour", p.value(icon, "color"), p.value(glyph, "stroke"));
            }
            if (r.containsKey("body")) {
                Row body = r.get("body");
                expect(here + "body margin-top", "4px", p.value(body, "margin-top"));
                expect(here + "body max-width", "384px", p.value(body, "max-width"));
                expect(here + "body font size", "14px", p.value(body, "font-size"));
                expect(here + "body line height", "20px", p.value(body, "line-height"));
                if (boxed && Double.parseDouble(p.value(body, "@w")) > 384.01) {
                    FACT_PROBLEMS.add(here + "body is " + p.value(body, "@w") + "px wide, wider than its 384px maximum");
                }
                if (boxed && c != null && c.body() == Body.WIDE) {
                    expect(here + "wide body content is capped at the maximum width", "384", p.value(body, "@w"));
                }
            }
            for (Row paragraph : rows.stream().filter(x -> x.ordinal() == e.getKey() && x.kind().equals("body-p")).toList()) {
                expect(here + "body paragraph margin-top", "0px", p.value(paragraph, "margin-top"));
                expect(here + "body paragraph margin-bottom", "0px", p.value(paragraph, "margin-bottom"));
                if (harbor) {
                    expect(here + "Harbor's paragraph line height", "23.1px", p.value(paragraph, "line-height"));
                }
            }
            if (r.containsKey("action")) {
                Row action = r.get("action");
                expect(here + "action display", "flex", p.value(action, "display"));
                expect(here + "action wraps", "wrap", p.value(action, "flex-wrap"));
                expect(here + "action justification", "center", p.value(action, "justify-content"));
                expect(here + "action alignment", "center", p.value(action, "align-items"));
                expect(here + "action column gap", "12px", p.value(action, "column-gap"));
                expect(here + "action row gap", "12px", p.value(action, "row-gap"));
                expect(here + "action margin-top", "16px", p.value(action, "margin-top"));
            }
            if (config.skin() != null) {
                // Only a skin defines the whole palette; with no skin the colours are parity-checked against the legacy rendering.
                expect(here + "title colour is the skin's --color-slate-" + (dark ? "100" : "900"), tok.get("--color-slate-" + (dark ? "100" : "900")), p.value(title, "color"));
                if (r.containsKey("icon")) {
                    expect(here + "icon colour is the skin's --color-slate-" + (dark ? "500" : "400"), tok.get("--color-slate-" + (dark ? "500" : "400")), p.value(r.get("icon"), "color"));
                }
                if (r.containsKey("body")) {
                    expect(here + "body colour is the skin's --color-slate-" + (dark ? "400" : "500"), tok.get("--color-slate-" + (dark ? "400" : "500")), p.value(r.get("body"), "color"));
                }
            }
        }
    }

    private static void controlledFacts(Config config, Probe probe) {
        emptyStateFacts("controlled " + config.id(), config, probe, probe.phase(config.id() + "|P"), CASES.size(), CASES);
    }

    private static void realFacts(String path, Config config, Probe p) {
        emptyStateFacts("real " + path + " " + config.id(), config, p, p.phase("ash"), EXPECTED_PAGES.get(path), null);
    }

    private static final Set<String> ICON_ATTRIBUTES = Set.of("aria-hidden", "class", "fill", "stroke", "stroke-linecap", "stroke-linejoin", "stroke-width", "viewBox");
    private static final Set<String> ALLOWED_CLASS_TOKENS = Set.of("ash-empty-state", "ash-empty-state__icon", "ash-empty-state__glyph", "ash-empty-state__title",
            "ash-empty-state__body", "ash-empty-state__action", "inline-block", "shrink-0");

    /** One Empty State's DOM, from its shape line. {@code inert} is true when its action content was replaced (the probe's own change). */
    private static void shapeProblems(String path, String[] s) {
        String where = path + " empty state #" + s[1];
        if (!s[2].equals("class,data-empty-state")) {
            SHAPE_PROBLEMS.add(where + ": wrapper attributes " + s[2]);
        }
        boolean icon = !s[5].isEmpty();
        boolean body = !s[7].isEmpty();
        boolean action = !s[8].isEmpty();
        String expectedChildren = (icon ? "div," : "") + "h3" + (body ? ",div" : "") + (action ? ",div" : "");
        if (!s[3].equals(expectedChildren)) {
            SHAPE_PROBLEMS.add(where + ": children " + s[3] + ", expected the optional icon box, the h3, the optional body and the optional action in that order");
        }
        if (icon) {
            if (!s[4].equals("class")) {
                SHAPE_PROBLEMS.add(where + ": icon box attributes " + s[4]);
            }
            if (!Set.of(s[5].split(",")).equals(ICON_ATTRIBUTES)) {
                SHAPE_PROBLEMS.add(where + ": icon attributes " + s[5] + " (a decorative icon has aria-hidden and no role or aria-label)");
            }
            if (!s[10].equals("true")) {
                SHAPE_PROBLEMS.add(where + ": the icon must be aria-hidden=\"true\", was \"" + s[10] + "\"");
            }
        } else if (!s[4].isEmpty()) {
            SHAPE_PROBLEMS.add(where + ": an icon box without an icon: " + s[4]);
        }
        if (!s[6].equals("class")) {
            SHAPE_PROBLEMS.add(where + ": title attributes " + s[6]);
        }
        if (s[9].isEmpty()) {
            SHAPE_PROBLEMS.add(where + ": the title must have text");
        }
        if (body && !s[7].equals("class")) {
            SHAPE_PROBLEMS.add(where + ": body attributes " + s[7]);
        }
        if (action && !s[8].equals("class")) {
            SHAPE_PROBLEMS.add(where + ": action attributes " + s[8]);
        }
        for (String token : s[14].split(" ")) {
            if (!ALLOWED_CLASS_TOKENS.contains(token)) {
                SHAPE_PROBLEMS.add(where + ": a class that is neither Ashlar's nor the Icon component's own: " + token);
            }
        }
    }

    private static void writeReport() throws IOException {
        StringBuilder sb = new StringBuilder("Empty State parity (real Chrome; no stored baseline).\n\n");
        sb.append("CONTROLLED ORACLE: ").append(CONFIGS.size()).append(" configurations x ").append(CASES.size()).append(" empty states (icon yes/no x ")
                .append(Body.values().length).append(" bodies x ").append(Action.values().length).append(" actions in a block, plus long titles and narrow, flex-column, flex-row, card and table surroundings) x (Tailwind present, Tailwind removed), ")
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
        sb.append("REAL PAGES: ").append(EXPECTED_PAGES.size()).append(" pages x ").append(CONFIGS.size()).append(" configurations (parallel, ").append(realSeconds).append(" s)\n");
        sb.append("   empty states per page (every catalog page, home and index): ").append(EMPTY_STATES_PER_PAGE.entrySet().stream().filter(e -> e.getValue() > 0).toList())
                .append(" ; pages scanned ").append(EMPTY_STATES_PER_PAGE.size()).append('\n');
        sb.append("   instance comparisons per mode (instances x configurations): ").append(realComparisonsPerMode).append('\n');
        sb.append("   differences (Tailwind present): ").append(REAL_PRESENT.size()).append(" ; (Tailwind removed): ").append(REAL_REMOVED.size()).append('\n');
        sb.append("   normalizations used, Tailwind present: ").append(APPLIED_REAL_PRESENT).append('\n');
        sb.append("   normalizations used, Tailwind removed: ").append(APPLIED_REAL_REMOVED).append('\n');
        sb.append("   facts not met: ").append(FACT_PROBLEMS.size()).append(" ; DOM shape problems: ").append(SHAPE_PROBLEMS.size()).append('\n');
        appendFindings(sb, "FINDINGS, controlled, Tailwind present", CONTROLLED_PRESENT.values().stream().flatMap(List::stream).toList());
        appendFindings(sb, "FINDINGS, controlled, removed vs scoped legacy", CONTROLLED_REMOVED_SCOPED.values().stream().flatMap(List::stream).toList());
        appendFindings(sb, "FINDINGS, controlled, removed vs full legacy", CONTROLLED_REMOVED_FULL.values().stream().flatMap(List::stream).toList());
        appendFindings(sb, "FINDINGS, scoped legacy vs full legacy", SCOPED_FIDELITY.values().stream().flatMap(List::stream).toList());
        appendFindings(sb, "FINDINGS, real pages, Tailwind present", REAL_PRESENT);
        appendFindings(sb, "FINDINGS, real pages, Tailwind removed", REAL_REMOVED);
        FACT_PROBLEMS.stream().limit(20).forEach(p -> sb.append("   FACT ").append(p).append('\n'));
        SHAPE_PROBLEMS.stream().limit(20).forEach(p -> sb.append("   SHAPE ").append(p).append('\n'));
        Path out = Path.of(System.getProperty("ashlar.cascade.dir")).resolve("../../reports/parity/empty-state.txt").normalize();
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
        assertTrue(CONFIGS.stream().anyMatch(c -> c.id().equals("light/harbor")) && CONFIGS.stream().anyMatch(c -> c.id().equals("dark/harbor")));
    }

    @Test
    void theControlledCasesCoverEveryIconBodyAndActionCombinationAndEveryContext() {
        List<Case> block = CASES.stream().filter(c -> c.layout().equals("block")).toList();
        // icon (none or one) x 7 bodies x 3 actions, plus the two long titles.
        assertEquals(2 * 7 * 3 + 2, block.size());
        assertEquals(Set.of("---", "--a", "-b-", "-ba", "i--", "i-a", "ib-", "iba"), CASES.stream().map(Case::shape).collect(Collectors.toSet()));
        assertEquals(Set.of(Body.values()), CASES.stream().map(Case::body).collect(Collectors.toSet()));
        assertEquals(Set.of(Action.values()), CASES.stream().map(Case::action).collect(Collectors.toSet()));
        assertEquals(Set.of("INBOX", "SEARCH"), CASES.stream().map(Case::icon).filter(i -> i != null).collect(Collectors.toSet()));
        assertEquals(Set.copyOf(LAYOUT_ORDER), CASES.stream().map(Case::layout).collect(Collectors.toSet()));
        assertTrue(CASES.stream().anyMatch(c -> c.title().equals(LONG_WORD)) && CASES.stream().anyMatch(c -> c.title().equals(LONG_TITLE)));
        assertEquals(CASES.stream().mapToInt(Case::rows).sum(), CONTROLLED_ROWS.get("light/none").intValue());
    }

    @Test
    void controlledOracleWithTailwindPresentMatchesTheLegacyEmptyStateInEveryConfiguration() {
        assertEquals(List.of(), CONTROLLED_PRESENT.values().stream().flatMap(List::stream).map(Finding::line).limit(25).toList());
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheLegacyEmptyStateGivenTheSameSurroundings() {
        assertEquals(List.of(), CONTROLLED_REMOVED_SCOPED.values().stream().flatMap(List::stream).map(Finding::line).limit(25).toList());
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheFullLegacyRendering() {
        assertEquals(List.of(), CONTROLLED_REMOVED_FULL.values().stream().flatMap(List::stream).map(Finding::line).limit(25).toList());
    }

    @Test
    void theScopedLegacyStylesheetRendersTheLegacyEmptyStateExactlyAsTheFullOneDoes() {
        // The Tailwind-removed real-page comparison trusts the scoped stylesheet as the legacy rendering; this is the proof.
        assertEquals(List.of(), SCOPED_FIDELITY.values().stream().flatMap(List::stream).map(Finding::line).limit(25).toList());
        assertEquals(22, SCOPED_FIDELITY.size());
        assertEquals(Map.of(), APPLIED_SCOPED, "the scoped legacy rendering must match the full one without any normalization");
    }

    @Test
    void everyNegativeControlIsDetectedInTheProperty() {
        assertTrue(CONTROL_LIST.size() >= 30, "the negative controls: " + CONTROL_LIST.size());
        for (Control control : CONTROL_LIST) {
            List<String> found = CONTROLS.get(control.name()).stream().map(Finding::property).toList();
            assertTrue(found.contains(control.expectedProperty()),
                    "control '" + control.name() + "' must differ in " + control.expectedProperty() + " but produced " + found);
        }
        assertEquals(CONTROL_LIST.size(), CONTROL_LIST.stream().map(Control::name).distinct().count());
        // The controls that Tailwind's own utilities would hide are measured without Tailwind, and every other one with it.
        assertTrue(CONTROL_LIST.stream().anyMatch(Control::removed) && CONTROL_LIST.stream().anyMatch(c -> !c.removed()));
    }

    @Test
    void theIconIsFortyPixelsTheVerticalPaddingFortyEightTheBodyAtMostThreeEightyFourAndEverythingElseAboutTheMigratedEmptyStateHolds() {
        assertEquals(List.of(), FACT_PROBLEMS.stream().limit(25).toList(), FACT_PROBLEMS.size() + " facts not met");
    }

    @Test
    void wideBodyContentIsCappedAtTheMaximumWidthAndALongTitleAndALongWordStayWhatTheyWere() {
        Probe probe = FRAME_PROBES.get("controlled");
        List<Row> rows = probe.phase("light/none|P");
        int w = props.indexOf("@w");
        List<Row> wide = rows.stream().filter(r -> r.kind().equals("body") && Double.parseDouble(r.values()[w]) >= 383.99).toList();
        long wideCases = CASES.stream().filter(c -> c.body() == Body.WIDE).count();
        assertEquals(wideCases, wide.size(), "exactly the bodies with content wider than the maximum fill the 384px maximum");
        assertTrue(wide.stream().allMatch(r -> r.variant().charAt(1) == 'b'), "and they are bodies");
        List<Row> titles = rows.stream().filter(r -> r.kind().equals("title") && r.variant().endsWith("/block")).toList();
        int h = props.indexOf("@h");
        double tallest = titles.stream().mapToDouble(r -> Double.parseDouble(r.values()[h])).max().orElseThrow();
        double shortest = titles.stream().mapToDouble(r -> Double.parseDouble(r.values()[h])).min().orElseThrow();
        assertEquals(24.0, shortest, 0.01, "a one-line title is one 24px line");
        assertTrue(tallest > 24.0, "the long title wraps onto more than one line: " + tallest);
        double longWord = titles.stream().mapToDouble(r -> Double.parseDouble(r.values()[w])).max().orElseThrow();
        assertTrue(longWord > 352, "an unbroken word is wider than the wrapper's 352px content box, as it was: " + longWord);
    }

    @Test
    void aCallersCustomColoursBeatTheDefaultPaletteInBothThemesAndBothWaysOfSettingThem() {
        Probe probe = FRAME_PROBES.get("controlled");
        for (String theme : List.of("light", "dark")) {
            int ordinal = 1 + (int) CASES.stream().takeWhile(c -> !(c.layout().equals("block") && "INBOX".equals(c.icon()) && c.body() == Body.TEXT && c.action() == Action.ONE && c.title().equals(TITLE))).count();
            Map<String, Row> plain = byOrdinal(probe.phase(theme + "/none|P")).get(ordinal);
            String defaultIcon = probe.value(plain.get("icon"), "color");
            String defaultTitle = probe.value(plain.get("title"), "color");
            String defaultBody = probe.value(plain.get("body"), "color");
            for (String[] c : new String[][] {{"inline", "rgb(1, 2, 3)", "rgb(4, 5, 6)", "rgb(7, 8, 9)"}, {"unlayered", "rgb(10, 11, 12)", "rgb(13, 14, 15)", "rgb(16, 17, 18)"}}) {
                String phase = "ovr:" + c[0] + "-" + theme + "|P";
                Map<String, Row> parts = byOrdinal(probe.phase(phase)).get(1);
                assertEquals(c[1], probe.value(parts.get("icon"), "color"), phase + ": the icon takes the caller's colour");
                assertEquals(c[1], probe.value(parts.get("glyph"), "stroke"), phase + ": the glyph's stroke follows it");
                assertEquals(c[2], probe.value(parts.get("title"), "color"), phase + ": the title takes the caller's colour");
                assertEquals(c[3], probe.value(parts.get("body"), "color"), phase + ": the body takes the caller's colour");
                assertNotEquals(defaultIcon, probe.value(parts.get("icon"), "color"), phase);
                assertNotEquals(defaultTitle, probe.value(parts.get("title"), "color"), phase);
                assertNotEquals(defaultBody, probe.value(parts.get("body"), "color"), phase);
            }
        }
        // The two themes really do have different defaults, so winning "over the default" means something in each mode.
        int ordinal = 1 + (int) CASES.stream().takeWhile(c -> !(c.layout().equals("block") && "INBOX".equals(c.icon()) && c.body() == Body.TEXT && c.action() == Action.ONE && c.title().equals(TITLE))).count();
        for (String kind : List.of("icon", "title", "body")) {
            assertNotEquals(probe.value(byOrdinal(probe.phase("light/none|P")).get(ordinal).get(kind), "color"),
                    probe.value(byOrdinal(probe.phase("dark/none|P")).get(ordinal).get(kind), "color"), kind);
        }
    }

    @Test
    void everyRealEmptyStateMatchesItsLegacyRenderingWithTailwindPresent() {
        assertEquals(List.of(), REAL_PRESENT.stream().map(Finding::line).limit(25).toList(), REAL_PRESENT.size() + " differences in total");
    }

    @Test
    void everyRealEmptyStateMatchesItsLegacyRenderingWithTailwindRemoved() {
        assertEquals(List.of(), REAL_REMOVED.stream().map(Finding::line).limit(25).toList(), REAL_REMOVED.size() + " differences in total");
    }

    @Test
    void realPageCoverageIsEveryEmptyStateOnEveryPageThatRendersOneInAll22Configurations() {
        int perConfiguration = EXPECTED_PAGES.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(40, perConfiguration);
        assertEquals(perConfiguration * 22, realComparisonsPerMode);
        Map<String, Integer> withEmptyStates = EMPTY_STATES_PER_PAGE.entrySet().stream().filter(e -> e.getValue() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));
        assertEquals(EXPECTED_PAGES, withEmptyStates, "no other page renders an Empty State, and these render exactly this many");
        assertEquals(2 + ComponentCatalog.PAGES.size(), EMPTY_STATES_PER_PAGE.size(), "every catalog page, the home page and the index were scanned");
        for (String path : EXPECTED_PAGES.keySet()) {
            for (Config c : CONFIGS) {
                Probe p = REAL_PROBES.get(pageKey(path, c, "present"));
                assertEquals(String.valueOf(EXPECTED_PAGES.get(path)), p.meta().get("empty-state-count"), path + " " + c.id());
                assertEquals(EXPECTED_PAGES.get(path).intValue(), p.shapes().size(), path + " " + c.id());
                Probe r = REAL_PROBES.get(pageKey(path, c, "removed"));
                assertEquals(String.valueOf(EXPECTED_PAGES.get(path)), r.meta().get("empty-state-count"), path + " " + c.id() + " (removed)");
            }
        }
        // The Table's "filtered" regions start hidden (its script shows them), the Empty State page shows all six.
        Probe own = REAL_PROBES.get(pageKey("/ashlar/components/empty-state", config("light/none"), "present"));
        assertEquals("0", own.meta().get("empty-state-without-a-box"));
        Probe table = REAL_PROBES.get(pageKey("/ashlar/components/table", config("light/none"), "present"));
        assertTrue(Integer.parseInt(table.meta().get("empty-state-without-a-box")) > 0, "some of the Table page's regions are hidden until its script shows them");
    }

    @Test
    void theEmptyStateDomShapeAndAccessibilityAttributesArePreservedAndCarryOnlyAshlarClasses() {
        assertEquals(List.of(), SHAPE_PROBLEMS);
        List<String[]> own = REAL_PROBES.get(pageKey("/ashlar/components/empty-state", config("light/none"), "present")).shapes();
        assertEquals(6, own.size());
        // Quick Start and Basic: icon and title only. With body: icon, title, body. With an action: all three. Without an icon: title only. Card: all three.
        assertEquals(List.of("div,h3", "div,h3", "div,h3,div", "div,h3,div,div", "h3", "div,h3,div,div"), own.stream().map(s -> s[3]).toList());
        assertEquals(List.of("No messages", "No messages", "No results found", "No projects yet", "No activity in the last 30 days", "No migration jobs"),
                own.stream().map(s -> s[9]).toList());
        // The icon is decorative whenever there is one: aria-hidden and neither a role nor an accessible name.
        own.stream().filter(s -> !s[5].isEmpty()).forEach(s -> {
            assertEquals("true", s[10]);
            assertFalse(s[5].contains("role") || s[5].contains("aria-label"), s[5]);
        });
        // Table renders both variants through the real component: the "none" region has an empty body and the "filtered" region a paragraph.
        List<String[]> tableShapes = REAL_PROBES.get(pageKey("/ashlar/components/table", config("light/none"), "present")).shapes();
        assertTrue(tableShapes.stream().anyMatch(s -> s[9].equals("No records found") && s[11].equals("0") && !s[7].isEmpty()));
        assertTrue(tableShapes.stream().anyMatch(s -> s[9].equals("No matching records") && s[11].equals("1") && s[12].equals("1")));
    }

    @Test
    void whileTheScopedLegacySheetIsLinkedTheLegacyNamesAreOffEveryOtherElementAndAreRestoredExactlyAndTheActionsWereNeutralized() {
        for (String path : EXPECTED_PAGES.keySet()) {
            for (Config c : CONFIGS) {
                Probe removed = REAL_PROBES.get(pageKey(path, c, "removed"));
                assertEquals("0", removed.meta().get("detach-restore-mismatch"), path + " " + c.id() + ": detached class names were not restored exactly");
                assertTrue(Integer.parseInt(removed.meta().get("detached-elements")) > 20,
                        path + " " + c.id() + ": the page around the empty states uses the legacy names, so many elements must have been detached: " + removed.meta().get("detached-elements"));
                assertEquals("0", REAL_PROBES.get(pageKey(path, c, "present")).meta().getOrDefault("detached-elements", "0"), path + " " + c.id() + ": nothing is detached while Tailwind is present");
                assertEquals("0", REAL_PROBES.get(pageKey(path, c, "present")).meta().getOrDefault("neutralized-actions", "0"), path + " " + c.id() + ": nothing is replaced while Tailwind is present");
            }
        }
        // The Empty State page has two actions (With an action, Card), and only it does.
        assertEquals("2", REAL_PROBES.get(pageKey("/ashlar/components/empty-state", config("light/none"), "removed")).meta().get("neutralized-actions"));
        assertEquals("0", REAL_PROBES.get(pageKey("/ashlar/components/table", config("light/none"), "removed")).meta().get("neutralized-actions"));
    }

    @Test
    void theFoundryShadowAndHarborsParagraphLineHeightStillApplyToTheRealEmptyStates() {
        for (String theme : List.of("light", "dark")) {
            Probe foundry = REAL_PROBES.get(pageKey("/ashlar/components/empty-state", config(theme + "/foundry"), "present"));
            for (Row r : foundry.phase("ash")) {
                if (r.kind().equals("wrapper")) {
                    assertNotEquals("none", foundry.value(r, "box-shadow"), theme + " Foundry wrapper #" + r.ordinal());
                }
            }
            Probe civic = REAL_PROBES.get(pageKey("/ashlar/components/empty-state", config(theme + "/civic"), "present"));
            assertTrue(civic.phase("ash").stream().filter(r -> r.kind().equals("wrapper")).allMatch(r -> civic.value(r, "box-shadow").equals("none")),
                    theme + ": only Foundry shadows an Empty State");
            Probe harbor = REAL_PROBES.get(pageKey("/ashlar/components/table", config(theme + "/harbor"), "present"));
            List<Row> paragraphs = harbor.phase("ash").stream().filter(r -> r.kind().equals("body-p")).toList();
            assertFalse(paragraphs.isEmpty(), "the Table page renders paragraphs in its Empty States");
            paragraphs.forEach(r -> assertEquals("23.1px", harbor.value(r, "line-height"), theme + " Harbor paragraph #" + r.ordinal()));
            Probe plain = REAL_PROBES.get(pageKey("/ashlar/components/table", config(theme + "/civic"), "present"));
            plain.phase("ash").stream().filter(r -> r.kind().equals("body-p")).forEach(r -> assertEquals("20px", plain.value(r, "line-height"), theme + " non-Harbor paragraph (14px at the body's 1.4286)"));
        }
    }

    // ---- the Tailwind-removed normalizations: exactly H2 on four properties, and the caller's paragraph box-sizing ----

    /** The one normalization rule the Tailwind-removed comparisons use, and the only four properties it may cover. */
    private static final List<String> H2_PROPERTIES = List.of("H2 border-top-style", "H2 border-right-style", "H2 border-bottom-style", "H2 border-left-style");

    /**
     * Why {@code applied} is not exactly "H2 on each of the four physical border-style properties, once per measured row and
     * comparison, and the paragraph box-sizing allowance, once per measured body paragraph and comparison" (empty when it is).
     * Preflight gives every element of the legacy Empty State `border: 0 solid`, and the migrated Empty State has no reset, so
     * each of its zero-width borders is `none` where the legacy one is `solid`: four properties per row, nothing else. A
     * paragraph the caller wrote inside the body has no Ashlar class, so it is `content-box` where preflight made it
     * `border-box`: one property per paragraph. Any other rule, any other property, or any other count is a finding.
     */
    static List<String> removedNormalizationProblems(Map<String, Integer> applied, int rowComparisons, int paragraphComparisons) {
        List<String> expectedKeys = new ArrayList<>(H2_PROPERTIES);
        expectedKeys.add(PARAGRAPH_BOX_SIZING);
        List<String> problems = new ArrayList<>();
        if (!applied.keySet().equals(Set.copyOf(expectedKeys))) {
            problems.add("normalizations used " + new TreeMap<>(applied) + ", expected exactly " + expectedKeys);
        }
        for (String property : H2_PROPERTIES) {
            Integer used = applied.get(property);
            if (used == null || used != rowComparisons) {
                problems.add(property + " was used " + used + " times, expected exactly " + rowComparisons + " (once per measured row and comparison)");
            }
        }
        Integer paragraphs = applied.get(PARAGRAPH_BOX_SIZING);
        if (paragraphs == null || paragraphs != paragraphComparisons) {
            problems.add(PARAGRAPH_BOX_SIZING + " was used " + paragraphs + " times, expected exactly " + paragraphComparisons + " (once per measured body paragraph and comparison)");
        }
        return problems;
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
        String oracle = Files.readString(dir.resolve("empty-state-oracle.css"), StandardCharsets.UTF_8);
        String scoped = Files.readString(dir.resolve("empty-state-legacy-scoped.css"), StandardCharsets.UTF_8);
        for (String token : legacy.allTokens()) {
            assertTrue(compiles(oracle, token), "the Empty State oracle does not compile " + token);
            // The scoped stylesheet reads no source but empty-state-legacy.html, so this also proves its @source path resolves.
            assertTrue(compiles(scoped, token), "the scoped stylesheet does not compile " + token);
        }
        // Cumulative, in order Badge -> Alert -> Divider -> Progress -> Empty State: the earlier legacy classes are still in the oracle.
        for (String classes : new BadgeLegacyMarkup().variants().values()) {
            for (String token : classes.split("\\s+")) {
                assertTrue(oracle.contains(escapeClass(token)), "the Empty State oracle lost the Badge legacy class " + token);
            }
        }
        for (String token : new AlertLegacyMarkup().allTokens()) {
            assertTrue(compiles(oracle, token), "the Empty State oracle lost the Alert legacy class " + token);
        }
        for (String token : new DividerLegacyMarkup().allTokens()) {
            assertTrue(compiles(oracle, token), "the Empty State oracle lost the Divider legacy class " + token);
        }
        for (String token : new ProgressLegacyMarkup().allTokens()) {
            assertTrue(compiles(oracle, token), "the Empty State oracle lost the Progress legacy class " + token);
        }
        assertFalse(scoped.contains("html,:host"), "the scoped legacy stylesheet must not carry Tailwind's page-wide base");
    }

    @Test
    void normalizationsStayWithinTheApprovedRulesAndTailwindPresentNeedsNone() {
        assertEquals(Map.of(), APPLIED_PRESENT, "with Tailwind present the controlled Empty State must match exactly");
        assertEquals(Map.of(), APPLIED_REAL_PRESENT, "with Tailwind present the real Empty State must match exactly");
    }

    @Test
    void theControlledTailwindRemovedComparisonsUseH2OncePerMeasuredRowAndComparisonAndTheParagraphAllowanceOncePerMeasuredParagraph() {
        // Two comparisons feed APPLIED_REMOVED for every configuration (removed vs scoped legacy, removed vs full legacy), each over
        // the rows the legacy rendering measured. Both counts are derived from the cases themselves, not from what was reported.
        int rowsPerConfiguration = CASES.stream().mapToInt(Case::rows).sum();
        int paragraphsPerConfiguration = CASES.stream().mapToInt(Case::paragraphs).sum();
        int rowsMeasured = CONFIGS.stream().mapToInt(c -> CONTROLLED_ROWS.get(c.id())).sum();
        assertEquals(CONFIGS.size() * rowsPerConfiguration, rowsMeasured, "rows measured across the configurations");
        int comparisons = 2;
        assertEquals(List.of(), removedNormalizationProblems(APPLIED_REMOVED, comparisons * rowsMeasured, comparisons * CONFIGS.size() * paragraphsPerConfiguration));
        assertEquals(4 * comparisons * rowsMeasured + comparisons * CONFIGS.size() * paragraphsPerConfiguration,
                APPLIED_REMOVED.values().stream().mapToInt(Integer::intValue).sum(), "total uses: four borders per row and comparison, one box-sizing per paragraph and comparison");
    }

    @Test
    void theRealPageTailwindRemovedComparisonsUseH2OncePerMeasuredElementAndTheParagraphAllowanceOncePerMeasuredParagraph() {
        // One comparison per page and configuration, over every element of every Empty State the page renders. The element and
        // paragraph counts come from each page's own DOM (its shape lines), and are checked against what the browser measured.
        int elementsMeasured = 0;
        for (String path : EXPECTED_PAGES.keySet()) {
            for (Config c : CONFIGS) {
                Probe r = REAL_PROBES.get(pageKey(path, c, "removed"));
                int expected = r.shapes().stream().mapToInt(EmptyStateParityTest::rowsOfShape).sum();
                assertEquals(expected, r.phase("legacy").size(), path + " " + c.id() + ": elements measured on the real page");
                elementsMeasured += r.phase("legacy").size();
            }
        }
        assertEquals(realRowsRemoved, elementsMeasured);
        assertEquals(List.of(), removedNormalizationProblems(APPLIED_REAL_REMOVED, elementsMeasured, realParagraphsRemoved));
        assertTrue(realParagraphsRemoved > 0, "the real pages render body paragraphs, so the paragraph allowance is exercised there");
        assertEquals(4 * elementsMeasured + realParagraphsRemoved, APPLIED_REAL_REMOVED.values().stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    void theNormalizationGateCanFailOnAnExtraUseAnExtraPropertyAnotherRuleAndAMissingProperty() {
        Map<String, Integer> exact = new TreeMap<>();
        H2_PROPERTIES.forEach(p -> exact.put(p, 10));
        exact.put(PARAGRAPH_BOX_SIZING, 3);
        assertEquals(List.of(), removedNormalizationProblems(exact, 10, 3));
        Map<String, Integer> extraUse = new TreeMap<>(exact);
        extraUse.merge("H2 border-top-style", 1, Integer::sum);
        assertFalse(removedNormalizationProblems(extraUse, 10, 3).isEmpty(), "one extra use must be reported");
        Map<String, Integer> fewer = new TreeMap<>(exact);
        fewer.merge("H2 border-left-style", -1, Integer::sum);
        assertFalse(removedNormalizationProblems(fewer, 10, 3).isEmpty(), "one use fewer must be reported");
        Map<String, Integer> extraParagraph = new TreeMap<>(exact);
        extraParagraph.merge(PARAGRAPH_BOX_SIZING, 1, Integer::sum);
        assertFalse(removedNormalizationProblems(extraParagraph, 10, 3).isEmpty(), "one extra paragraph use must be reported");
        Map<String, Integer> noParagraph = new TreeMap<>(exact);
        noParagraph.remove(PARAGRAPH_BOX_SIZING);
        assertFalse(removedNormalizationProblems(noParagraph, 10, 3).isEmpty(), "a missing paragraph allowance must be reported");
        Map<String, Integer> extraProperty = new TreeMap<>(exact);
        extraProperty.put("H2 border-top-width", 1);
        assertFalse(removedNormalizationProblems(extraProperty, 10, 3).isEmpty(), "H2 on any other property must be reported");
        Map<String, Integer> otherRule = new TreeMap<>(exact);
        otherRule.put("H3 vertical-align", 1);
        assertFalse(removedNormalizationProblems(otherRule, 10, 3).isEmpty(), "another rule must be reported");
        Map<String, Integer> missing = new TreeMap<>(exact);
        missing.remove("H2 border-right-style");
        assertFalse(removedNormalizationProblems(missing, 10, 3).isEmpty(), "a missing property must be reported");
        assertFalse(removedNormalizationProblems(Map.of(), 10, 3).isEmpty(), "no normalization at all must be reported too: the expectation is exact");
    }

    private static final List<String> ALLOWANCE_NAMES = List.of("box-sizing", "margin-top", "padding-top", "padding-right", "padding-bottom", "padding-left",
            "border-top-width", "border-right-width", "border-bottom-width", "border-left-width", "@w", "@h");

    /** A row with the given kind, box-sizing and margin; padding and border widths are zero except the ones overridden by name. */
    private static Row allowanceRow(String kind, String boxSizing, String marginTop, Map<String, String> overrides) {
        String[] values = new String[ALLOWANCE_NAMES.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = "0px";
        }
        values[0] = boxSizing;
        values[1] = marginTop;
        values[10] = "1";
        values[11] = "1";
        overrides.forEach((name, value) -> values[ALLOWANCE_NAMES.indexOf(name)] = value);
        return new Row("p", 1, kind, "i-b/block", "", "flex", values, "");
    }

    private static List<Finding> allowance(Row legacyRow, Row migratedRow, Map<String, Integer> applied, boolean tailwindRemoved) {
        return compare("t", ALLOWANCE_NAMES, List.of(legacyRow), List.of(migratedRow), applied, tailwindRemoved);
    }

    @Test
    void theParagraphAllowanceOnlyCoversTheCallersBodyParagraphsBoxSizingAndNothingElse() {
        Row legacyP = allowanceRow("body-p", "border-box", "0px", Map.of());
        Row migratedP = allowanceRow("body-p", "content-box", "0px", Map.of());
        Map<String, Integer> applied = new TreeMap<>();
        // A paragraph that is content-box where it was border-box, with no padding and no border on either side: allowed and counted when Tailwind is removed.
        assertEquals(List.of(), allowance(legacyP, migratedP, applied, true));
        assertEquals(Map.of(PARAGRAPH_BOX_SIZING, 1), applied);
        // The same difference with Tailwind present, on any other element, or in the other direction: a finding.
        assertEquals(1, allowance(legacyP, migratedP, new TreeMap<>(), false).size());
        assertEquals(1, allowance(allowanceRow("body-x", "border-box", "0px", Map.of()), allowanceRow("body-x", "content-box", "0px", Map.of()), new TreeMap<>(), true).size());
        assertEquals(1, allowance(allowanceRow("body-p", "content-box", "0px", Map.of()), allowanceRow("body-p", "border-box", "0px", Map.of()), new TreeMap<>(), true).size());
        // A paragraph that differs in anything else is never excused.
        assertEquals(1, allowance(legacyP, allowanceRow("body-p", "border-box", "16px", Map.of()), new TreeMap<>(), true).size());
    }

    @Test
    void theParagraphAllowanceIsRefusedWhenEitherRowHasPaddingOrABorderWidthOrTheyWereNotMeasured() {
        Row legacyP = allowanceRow("body-p", "border-box", "0px", Map.of());
        Row migratedP = allowanceRow("body-p", "content-box", "0px", Map.of());
        for (String property : BOX_EXTRAS) {
            Map<String, Integer> applied = new TreeMap<>();
            Row nonzero = allowanceRow("body-p", "content-box", "0px", Map.of(property, "4px"));
            // Nonzero on the migrated row only: that is a finding for the box-sizing and for the property itself (padding or border width differ).
            List<Finding> migratedSide = allowance(legacyP, nonzero, applied, true);
            assertTrue(migratedSide.stream().anyMatch(f -> f.property().equals("box-sizing")), property + " on the migrated row must not excuse box-sizing");
            assertTrue(migratedSide.stream().anyMatch(f -> f.property().equals(property)), property + " itself differs");
            assertEquals(Map.of(), applied, property + ": nothing may be counted as allowed");
            // Nonzero on both rows: the property no longer differs, but the box models now do, so box-sizing must still be a finding.
            Row bothLegacy = allowanceRow("body-p", "border-box", "0px", Map.of(property, "4px"));
            applied.clear();
            List<Finding> both = allowance(bothLegacy, nonzero, applied, true);
            assertEquals(List.of("box-sizing"), both.stream().map(Finding::property).toList(), property + " on both rows must still leave box-sizing as the finding");
            assertEquals(Map.of(), applied);
            // Nonzero on the legacy row only.
            applied.clear();
            assertTrue(allowance(bothLegacy, migratedP, applied, true).stream().anyMatch(f -> f.property().equals("box-sizing")));
            assertEquals(Map.of(), applied);
        }
        // A row measured without those properties cannot establish the precondition, so nothing is excused.
        Row bare = new Row("p", 1, "body-p", "i-b/block", "", "flex", new String[] {"border-box", "1", "1"}, "");
        Row bareMigrated = new Row("p", 1, "body-p", "i-b/block", "", "flex", new String[] {"content-box", "1", "1"}, "");
        Map<String, Integer> applied = new TreeMap<>();
        assertEquals(1, compare("t", List.of("box-sizing", "@w", "@h"), List.of(bare), List.of(bareMigrated), applied, true).size());
        assertEquals(Map.of(), applied);
    }

    // ---- delivery ----

    @Test
    void theStylesheetTheBootJarServesCarriesTheEmptyStateRulesInTheComponentsLayer() throws IOException {
        String css;
        try (ZipFile boot = new ZipFile(Path.of(System.getProperty("ashlar.showcase.jar")).toFile())) {
            css = new String(boot.getInputStream(boot.getEntry("BOOT-INF/classes/static/css/app.css")).readAllBytes(), StandardCharsets.UTF_8);
        }
        String compact = css.replaceAll("\\s+", "");
        for (String selector : List.of(".ash-empty-state{", ".ash-empty-state__icon{", ".ash-empty-state__glyph{", ".ash-empty-state__title{",
                ".ash-empty-state__body{", ".ash-empty-state__action{")) {
            assertTrue(compact.contains(selector), "the served stylesheet has no " + selector);
        }
        assertTrue(compact.contains(".ash-empty-state__body>p{margin:0"), "the approved scoped paragraph reset is delivered, and only as a child combinator");
        // The generated library stylesheet (the file packaged into the jar) holds the module inside its own ash.components block, between Divider's and Progress's.
        String generated = Files.readString(Path.of(System.getProperty("ashlar.css.generated")), StandardCharsets.UTF_8);
        int rule = generated.indexOf("\n.ash-empty-state {");
        assertTrue(rule > 0, "the generated stylesheet has the Empty State rule");
        int layerStart = generated.lastIndexOf("\n@layer ", rule) + 1;
        assertEquals("@layer ash.components {", generated.substring(layerStart, generated.indexOf('\n', layerStart)),
                "the Empty State rules sit directly inside an ash.components block");
        assertTrue(generated.indexOf("\n.ash-divider {") < rule && rule < generated.indexOf("\n.ash-progress {"),
                "after Divider and before Progress, the order of the entry file");
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
