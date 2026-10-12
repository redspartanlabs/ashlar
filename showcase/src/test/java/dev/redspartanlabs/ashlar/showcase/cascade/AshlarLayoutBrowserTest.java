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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase D: the layout primitives and {@code ash-truncate}, measured in a real
 * browser against the REAL generated {@code ashlar.css} (the file packaged into the
 * jar), with no Tailwind on the page unless a case says so.
 *
 * <p>Every case is a same-origin {@code srcdoc} iframe of an exact width, so media
 * queries evaluate against that width: headless Chrome will not give a window
 * narrower than 512px. Expected numbers come from the contracts in the CSS module
 * headers and were first observed in a throwaway prototype; wherever an expectation
 * was not previously measured, the first run is reported before it is pinned.
 *
 * <p>Evidence is Chrome only.
 */
class AshlarLayoutBrowserTest {

    private static final String ASHLAR = "/ashlar-real/ashlar.css";
    private static final int[] WIDTHS = {320, 639, 640, 1023, 1024, 1280};

    record Frame(String id, int width, String dir, List<String> sheets, String htmlAttrs, String extraCss, String body) {
        static Frame plain(String id, int width, String body) {
            return new Frame(id, width, "ltr", List.of(ASHLAR), "", "", body);
        }

        Frame withCss(String css) {
            return new Frame(id, width, dir, sheets, htmlAttrs, css, body);
        }

        Frame rtl() {
            return new Frame(id, width, "rtl", sheets, htmlAttrs, extraCss, body);
        }
    }

    /** frame id to element id to measured property. */
    private static final Map<String, Map<String, Map<String, String>>> MEASURED = new LinkedHashMap<>();
    private static final List<Frame> FRAMES = new ArrayList<>();

    // ---- markup helpers ----

    private static String box(int count, String style) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append("<div class=\"i\" style=\"").append(style).append("\"></div>");
        }
        return sb.toString();
    }

    private static final List<String> SKINS = List.of("axiom", "civic", "fathom", "foundry", "harbor", "keystone", "ledger", "sterling", "summit", "veridian");

    /** What page.jte links in the real showcase: the compiled app.css (Tailwind plus Ashlar) and every skin. */
    private static final List<String> INTEGRATED_SHEETS = integratedSheets();

    private static List<String> integratedSheets() {
        List<String> sheets = new ArrayList<>();
        sheets.add("/css/app.css");
        SKINS.forEach(s -> sheets.add("/css/theme-" + s + ".css"));
        return List.copyOf(sheets);
    }

    private static final String PAIR ="<p style=\"margin:0;block-size:20px\"></p><p style=\"margin:0;block-size:20px\"></p>";

    static {
        // Isolation, stack. extraCss comes AFTER the ashlar.css link, so `base` and `utilities` below are
        // the layers ashlar.css's own statement declared: base is lower than ash.layout, utilities higher.
        FRAMES.add(Frame.plain("iso-stack", 800, ""
                + "<div id=\"s-outer\" class=\"ash-stack\" style=\"--ash-stack-gap:40px\">"
                + "<div id=\"s-inner\" class=\"ash-stack\">" + PAIR + "</div>"
                + "<div id=\"s-inner-inline\" class=\"ash-stack\" style=\"--ash-stack-gap:8px\">" + PAIR + "</div>"
                + "<div id=\"s-inner-lg\" class=\"ash-stack ash-stack--lg\">" + PAIR + "</div>"
                + "<div id=\"s-inner-lg-inline\" class=\"ash-stack ash-stack--lg\" style=\"--ash-stack-gap:8px\">" + PAIR + "</div>"
                + "<div style=\"--ash-stack-gap:70px\"><div id=\"s-in-plain\" class=\"ash-stack\">" + PAIR + "</div></div>"
                + "<div id=\"s-unl\" class=\"ash-stack cfg-unl\">" + PAIR + "</div>"
                + "<div id=\"s-unl-lg\" class=\"ash-stack ash-stack--lg cfg-unl\">" + PAIR + "</div>"
                + "<div id=\"s-low\" class=\"ash-stack cfg-low\">" + PAIR + "</div>"
                + "<div id=\"s-high\" class=\"ash-stack cfg-high\">" + PAIR + "</div>"
                + "</div>").withCss(
                ".cfg-unl{--ash-stack-gap:33px} @layer base{.cfg-low{--ash-stack-gap:50px}} @layer utilities{.cfg-high{--ash-stack-gap:60px}}"));

        // Isolation, container.
        FRAMES.add(Frame.plain("iso-container", 800, ""
                + "<div id=\"c-outer\" class=\"ash-container\" style=\"--ash-container-pad:50px;--ash-container-max:30rem\">"
                + "<div id=\"c-inner\" class=\"ash-container\"><div id=\"c-leaf\" style=\"block-size:5px\"></div></div></div>"
                + "<div style=\"--ash-container-max:20rem;--ash-container-pad:5px\"><div id=\"c-plain\" class=\"ash-container\"></div></div>"
                + "<div id=\"c-inline-max\" class=\"ash-container\" style=\"--ash-container-max:20rem\"></div>"
                + "<div id=\"c-narrow-inline\" class=\"ash-container ash-container--narrow\" style=\"--ash-container-max:30rem\"></div>"
                + "<div id=\"c-unl\" class=\"ash-container cfg-cont\"></div>").withCss(".cfg-cont{--ash-container-pad:7px}"));

        for (int w : WIDTHS) {
            FRAMES.add(Frame.plain("container-" + w, w, ""
                    + "<div id=\"cont\" class=\"ash-container\"><div style=\"block-size:10px\"></div></div>"
                    + "<div id=\"narrow\" class=\"ash-container ash-container--narrow\"></div>"
                    + "<div id=\"fluid\" class=\"ash-container ash-container--fluid\"></div>"
                    + "<div id=\"pad\" class=\"ash-container\" style=\"--ash-container-pad:10px\"></div>"));
        }

        FRAMES.add(Frame.plain("stack-gaps", 400, ""
                + "<div id=\"st\" class=\"ash-stack\">" + PAIR + "</div>"
                + "<div id=\"st-sm\" class=\"ash-stack ash-stack--sm\">" + PAIR + "</div>"
                + "<div id=\"st-lg\" class=\"ash-stack ash-stack--lg\">" + PAIR + "</div>"
                + "<div id=\"st-xl\" class=\"ash-stack ash-stack--xl\">" + PAIR + "</div>"));

        // Control: a stack WITHOUT the reset (defined here, not in ashlar.css) must show the leak the reset prevents.
        FRAMES.add(Frame.plain("iso-control", 400, ""
                + "<div id=\"k-outer\" class=\"ash-stack\" style=\"--ash-stack-gap:40px\">"
                + "<div id=\"k-leaky\" class=\"leaky\">" + PAIR + "</div>"
                + "<div id=\"k-real\" class=\"ash-stack\">" + PAIR + "</div></div>").withCss(
                ".leaky{display:flex;flex-direction:column;gap:var(--ash-stack-gap,1rem)}"));

        // Isolation, grid / split / cluster.
        FRAMES.add(Frame.plain("iso-grid", 800, ""
                + "<div style=\"--ash-grid-min:5rem\"><div id=\"gm-wrapped\" class=\"ash-grid\">" + box(6, "block-size:20px") + "</div></div>"
                + "<div id=\"gm-own\" class=\"ash-grid\" style=\"--ash-grid-min:5rem\">" + box(6, "block-size:20px") + "</div>"
                + "<div id=\"gm-outer\" class=\"ash-grid\" style=\"--ash-grid-gap:40px\"><div id=\"gm-inner\" class=\"ash-grid\">" + box(4, "block-size:20px") + "</div></div>"));
        FRAMES.add(Frame.plain("iso-split", 800, ""
                + "<div style=\"--ash-split-side:15rem\"><div id=\"sp-wrapped\" class=\"ash-split ash-split--side-start\"><div class=\"i\" style=\"inline-size:100px;block-size:20px\"></div><div class=\"i\" style=\"block-size:20px\"></div></div></div>"
                + "<div id=\"sp-own\" class=\"ash-split ash-split--side-start\" style=\"--ash-split-side:20rem\"><div class=\"i\" style=\"block-size:20px\"></div><div class=\"i\" style=\"block-size:20px\"></div></div>"
                + "<div id=\"sp-outer\" class=\"ash-split\" style=\"--ash-split-gap:40px\"><div class=\"i\" style=\"block-size:20px\"></div>"
                + "<div class=\"i\"><div id=\"sp-inner\" class=\"ash-split\"><div class=\"i\" style=\"block-size:20px\"></div><div class=\"i\" style=\"block-size:20px\"></div></div></div></div>"));
        FRAMES.add(Frame.plain("iso-cluster", 800, ""
                + "<div style=\"--ash-cluster-gap:40px\"><div id=\"cl-wrapped\" class=\"ash-cluster\">" + box(2, "inline-size:50px;block-size:20px") + "</div></div>"
                + "<div id=\"cl-outer\" class=\"ash-cluster\" style=\"--ash-cluster-gap:40px\"><div id=\"cl-inner\" class=\"ash-cluster\">" + box(2, "inline-size:50px;block-size:20px") + "</div></div>"));

        for (int w : WIDTHS) {
            FRAMES.add(Frame.plain("grid-" + w, w, ""
                    + "<div id=\"g6\" class=\"ash-grid\">" + box(6, "block-size:20px") + "</div>"
                    + "<div id=\"g2\" class=\"ash-grid\">" + box(2, "block-size:20px") + "</div>"
                    + "<div id=\"c2\" class=\"ash-grid ash-grid--cols-2\">" + box(6, "block-size:20px") + "</div>"
                    + "<div id=\"c3\" class=\"ash-grid ash-grid--cols-3\">" + box(6, "block-size:20px") + "</div>"
                    + "<div id=\"c4\" class=\"ash-grid ash-grid--cols-4\">" + box(6, "block-size:20px") + "</div>"
                    + "<div id=\"cmin\" class=\"ash-grid ash-grid--cols-3\" style=\"--ash-grid-min:5rem\">" + box(6, "block-size:20px") + "</div>"));
            FRAMES.add(Frame.plain("split-" + w, w, ""
                    + "<div id=\"sp\" class=\"ash-split\">" + box(2, "block-size:20px") + "</div>"
                    + "<div id=\"ss\" class=\"ash-split ash-split--side-start\" style=\"--ash-split-side:15rem\">" + box(2, "block-size:20px") + "</div>"
                    + "<div id=\"sa\" class=\"ash-split ash-split--side-start\"><div class=\"i\" style=\"inline-size:100px;block-size:20px\"></div><div class=\"i\" style=\"block-size:20px\"></div></div>"
                    + "<div id=\"se\" class=\"ash-split ash-split--side-end\" style=\"--ash-split-side:15rem\">" + box(2, "block-size:20px") + "</div>"));
        }
        FRAMES.add(Frame.plain("gaps", 1280, ""
                + "<div id=\"gr\" class=\"ash-grid\">" + box(2, "block-size:20px") + "</div>"
                + "<div id=\"gr-sm\" class=\"ash-grid ash-grid--sm\">" + box(2, "block-size:20px") + "</div>"
                + "<div id=\"gr-lg\" class=\"ash-grid ash-grid--lg\">" + box(2, "block-size:20px") + "</div>"
                + "<div id=\"gr-xl\" class=\"ash-grid ash-grid--xl\">" + box(2, "block-size:20px") + "</div>"
                + "<div id=\"sp\" class=\"ash-split\">" + box(2, "block-size:20px") + "</div>"
                + "<div id=\"sp-sm\" class=\"ash-split ash-split--sm\">" + box(2, "block-size:20px") + "</div>"
                + "<div id=\"sp-lg\" class=\"ash-split ash-split--lg\">" + box(2, "block-size:20px") + "</div>"
                + "<div id=\"sp-xl\" class=\"ash-split ash-split--xl\">" + box(2, "block-size:20px") + "</div>"
                + "<div id=\"cl\" class=\"ash-cluster\">" + box(2, "inline-size:50px;block-size:20px") + "</div>"
                + "<div id=\"cl-sm\" class=\"ash-cluster ash-cluster--sm\">" + box(2, "inline-size:50px;block-size:20px") + "</div>"
                + "<div id=\"cl-lg\" class=\"ash-cluster ash-cluster--lg\">" + box(2, "inline-size:50px;block-size:20px") + "</div>"
                + "<div id=\"cl-xl\" class=\"ash-cluster ash-cluster--xl\">" + box(2, "inline-size:50px;block-size:20px") + "</div>"));

        FRAMES.add(Frame.plain("cluster", 300, ""
                + "<div id=\"cl5\" class=\"ash-cluster\">" + box(5, "inline-size:100px;block-size:20px") + "</div>"
                + "<div id=\"cl-between\" class=\"ash-cluster ash-cluster--between\">" + box(2, "inline-size:50px;block-size:20px") + "</div>"
                + "<div id=\"cl-end\" class=\"ash-cluster ash-cluster--end\">" + box(2, "inline-size:50px;block-size:20px") + "</div>"));

        FRAMES.add(Frame.plain("truncate", 200, ""
                + "<div style=\"display:flex\"><div id=\"t\" class=\"ash-truncate\">" + "long text ".repeat(30) + "</div></div>"
                + "<div id=\"nt\">" + "long text ".repeat(30) + "</div>"));

        String directional = ""
                + "<div id=\"ss\" class=\"ash-split ash-split--side-start\" style=\"--ash-split-side:15rem\">" + box(2, "block-size:20px") + "</div>"
                + "<div id=\"gr\" class=\"ash-grid ash-grid--cols-3\">" + box(3, "block-size:20px") + "</div>"
                + "<div id=\"cl\" class=\"ash-cluster ash-cluster--end\">" + box(3, "inline-size:50px;block-size:20px") + "</div>";
        FRAMES.add(Frame.plain("dir-ltr", 800, directional));
        FRAMES.add(Frame.plain("dir-rtl", 800, directional).rtl());

        FRAMES.add(Frame.plain("composition", 1280, ""
                + "<div id=\"cs\" class=\"ash-container ash-stack\">" + PAIR + "</div>"
                + "<div class=\"ash-stack\"><div id=\"nested-split\" class=\"ash-split ash-split--side-end\" style=\"--ash-split-side:200px\">" + box(2, "block-size:20px") + "</div></div>"));

        // Integrated: the production app.css (Tailwind plus the bundled Ashlar stylesheet) and all ten skins.
        FRAMES.add(new Frame("tw-beat", 1024, "ltr", INTEGRATED_SHEETS, " data-theme=\"light\"", ".cx{gap:3px}", ""
                + "<div id=\"tb-lg\" class=\"ash-stack ash-stack--lg\">" + PAIR + "</div>"
                + "<div id=\"tb-utility\" class=\"ash-stack gap-10\">" + PAIR + "</div>"
                + "<div id=\"tb-unlayered\" class=\"ash-stack gap-10 cx\">" + PAIR + "</div>"
                + "<div id=\"tb-flex-col\" class=\"ash-cluster flex-col\">" + box(2, "inline-size:50px;block-size:20px") + "</div>"));
        String battery = ""
                + "<div id=\"b-cont\" class=\"ash-container\"><div style=\"block-size:10px\"></div></div>"
                + "<div id=\"b-stack\" class=\"ash-stack ash-stack--lg\">" + PAIR + "</div>"
                + "<div id=\"b-grid\" class=\"ash-grid\">" + box(6, "block-size:20px") + "</div>"
                + "<div id=\"b-cols\" class=\"ash-grid ash-grid--cols-3\">" + box(6, "block-size:20px") + "</div>"
                + "<div id=\"b-split\" class=\"ash-split ash-split--side-start\" style=\"--ash-split-side:15rem\">" + box(2, "block-size:20px") + "</div>"
                + "<div id=\"b-cluster\" class=\"ash-cluster ash-cluster--between\">" + box(3, "inline-size:50px;block-size:20px") + "</div>";
        for (String theme : List.of("light", "dark")) {
            FRAMES.add(new Frame("bat-default-" + theme, 1024, "ltr", INTEGRATED_SHEETS, " data-theme=\"" + theme + "\"", "", battery));
            for (String skin : SKINS) {
                FRAMES.add(new Frame("bat-" + skin + "-" + theme, 1024, "ltr", INTEGRATED_SHEETS,
                        " data-theme=\"" + theme + "\" data-skin=\"" + skin + "\"", "", battery));
            }
        }
    }

    @BeforeAll
    static void measureEveryFrame() throws Exception {
        var chromePath = HeadlessChrome.find();
        if (chromePath.isEmpty()) {
            if (System.getenv("CI") != null || "true".equalsIgnoreCase(System.getenv("ASHLAR_REQUIRE_BROWSER"))) {
                throw new IllegalStateException("Chrome is required (CI or ASHLAR_REQUIRE_BROWSER set) but was not found.");
            }
            System.out.println("[layout] Chrome not found - layout scenarios SKIPPED (not passed).");
            Assumptions.abort("Chrome not found; the real-browser layout checks were not run");
        }
        String dir = System.getProperty("ashlar.cascade.dir");
        if (dir == null || System.getProperty("ashlar.css.generated") == null) {
            throw new IllegalStateException("ashlar.cascade.dir and ashlar.css.generated must be set; run through Gradle (:showcase:test)");
        }
        String template;
        try (InputStream in = AshlarLayoutBrowserTest.class.getResourceAsStream("/cascade/layout-fixture.html")) {
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        StringBuilder frames = new StringBuilder();
        for (Frame f : FRAMES) {
            frames.append(iframe(f)).append('\n');
        }
        String html = template.replace("<!--FRAMES-->", frames);
        try (CascadeFixtureServer server = new CascadeFixtureServer(Path.of(dir))) {
            String dom = new HeadlessChrome(chromePath.get()).dumpDom(server.registerRaw(html), List.of("--hide-scrollbars"));
            parse(dom);
        }
        writeReport();
    }

    private static String iframe(Frame f) {
        StringBuilder links = new StringBuilder();
        for (String href : f.sheets()) {
            links.append("<link rel=\"stylesheet\" href=\"").append(href).append("\">");
        }
        String doc = "<!doctype html><html dir=\"" + f.dir() + "\"" + f.htmlAttrs() + "><head>" + links
                + "<style>body{margin:0;font:16px/1.2 sans-serif} .i{box-sizing:border-box;background:#ccc}" + f.extraCss() + "</style></head><body>"
                + f.body() + "</body></html>";
        return "<iframe data-id=\"" + f.id() + "\" style=\"width:" + f.width() + "px;height:1400px;border:0;display:block\" srcdoc=\""
                + doc.replace("&", "&amp;").replace("\"", "&quot;") + "\"></iframe>";
    }

    private static void parse(String dom) {
        Matcher m = Pattern.compile("<pre id=\"ash-layout-results\">(.*?)</pre>", Pattern.DOTALL).matcher(dom);
        String found = null;
        while (m.find()) {
            found = m.group(1);
        }
        if (found == null || found.isBlank()) {
            throw new IllegalStateException("the layout fixture produced no results - infrastructure failure, not a pass");
        }
        String text = found.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&amp;", "&");
        for (String line : text.split("\\R")) {
            String[] f = line.split("\t", 3);
            if (f.length != 3) {
                throw new IllegalStateException("malformed layout result line: " + line);
            }
            Map<String, String> props = new LinkedHashMap<>();
            for (String pair : f[2].split("\u001f")) {
                int eq = pair.indexOf('=');
                props.put(pair.substring(0, eq), pair.substring(eq + 1));
            }
            MEASURED.computeIfAbsent(f[0], k -> new LinkedHashMap<>()).put(f[1], props);
        }
        for (Frame f : FRAMES) {
            if (!MEASURED.containsKey(f.id()) || MEASURED.get(f.id()).size() < 2) {
                throw new IllegalStateException("frame " + f.id() + " produced no measurements - infrastructure failure, not a pass");
            }
        }
    }

    private static void writeReport() throws IOException {
        StringBuilder sb = new StringBuilder("Phase D layout measurements (Chrome, real generated ashlar.css)\n");
        MEASURED.forEach((frame, els) -> {
            sb.append("== ").append(frame).append('\n');
            els.forEach((el, p) -> {
                if (el.equals("#frame")) {
                    sb.append("   frame ").append(p).append('\n');
                } else {
                    sb.append("   #").append(el).append(" display=").append(p.get("display")).append(" w=").append(p.get("w"))
                            .append(" rowGap=").append(p.get("rowGap")).append(" columnGap=").append(p.get("columnGap"))
                            .append(" padL=").append(p.get("paddingLeft")).append(" marginL=").append(p.get("marginLeft"))
                            .append(" kids=").append(p.get("kids")).append('\n');
                }
            });
        });
        Path out = Path.of(System.getProperty("ashlar.cascade.dir")).resolve("../../reports/cascade/phase-d.txt").normalize();
        Files.createDirectories(out.getParent());
        Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
    }

    // ---- accessors ----

    private static String val(String frame, String el, String key) {
        Map<String, String> p = MEASURED.get(frame).get(el);
        assertTrue(p != null, "no element #" + el + " in frame " + frame);
        return p.get(key);
    }

    private static double px(String frame, String el, String key) {
        String v = val(frame, el, key);
        assertTrue(v.endsWith("px") || v.matches("-?[0-9.]+"), frame + " #" + el + " " + key + " is not a pixel value: " + v);
        return Double.parseDouble(v.replace("px", ""));
    }

    /** Rects of the children of an element: left, top, width, height. */
    private static List<double[]> kids(String frame, String el) {
        List<double[]> result = new ArrayList<>();
        String v = val(frame, el, "kids");
        if (!v.isEmpty()) {
            for (String k : v.split(",")) {
                String[] n = k.split(":");
                result.add(new double[]{Double.parseDouble(n[0]), Double.parseDouble(n[1]), Double.parseDouble(n[2]), Double.parseDouble(n[3])});
            }
        }
        return result;
    }

    private static void assertPx(double expected, String frame, String el, String key) {
        assertEquals(expected, px(frame, el, key), 0.51, frame + " #" + el + " " + key);
    }

    // ---- tests: no Tailwind on the page ----

    @Test
    void theNoTailwindFramesLoadOnlyTheRealAshlarStylesheet() {
        for (Frame f : FRAMES) {
            if (f.sheets().equals(List.of(ASHLAR))) {
                assertEquals("ashlar.css,inline", val(f.id(), "#frame", "sheets"), f.id());
                assertEquals(String.valueOf(f.width()), val(f.id(), "#frame", "innerWidth"), f.id() + " viewport width");
            }
        }
    }

    @Test
    void stackIsolationResetsInheritedConfigurationOnEveryInstance() {
        String f = "iso-stack";
        assertPx(40, f, "s-outer", "rowGap");          // the outer stack's own inline configuration
        assertPx(16, f, "s-inner", "rowGap");          // not 40: nested stack ignores the outer value
        assertPx(16, f, "s-in-plain", "rowGap");       // not 70: a plain wrapper configures nothing
    }

    @Test
    void stackConfigurationPrecedenceIsInlineOrUnlayeredThenModifierThenDefault() {
        String f = "iso-stack";
        assertPx(8, f, "s-inner-inline", "rowGap");     // inline style beats the default
        assertPx(24, f, "s-inner-lg", "rowGap");        // modifier beats the default
        assertPx(8, f, "s-inner-lg-inline", "rowGap");  // inline style beats the modifier
        assertPx(33, f, "s-unl", "rowGap");             // unlayered consumer rule beats the default
        assertPx(33, f, "s-unl-lg", "rowGap");          // ...and beats the modifier
        assertPx(60, f, "s-high", "rowGap");            // a layer above ash.layout beats the primitive
    }

    @Test
    void configurationFromALayerBelowAshLayoutIsOverriddenByTheReset() {
        // DOCUMENTED LIMITATION: ash.layout resets the property on every instance, and ash.layout
        // outranks the `base` layer, so configuration written there does not take effect.
        assertPx(16, "iso-stack", "s-low", "rowGap");
    }

    @Test
    void containerIsolationAndPrecedence() {
        String f = "iso-container";
        assertPx(480, f, "c-outer", "w");                 // its own inline max of 30rem
        assertPx(50, f, "c-outer", "paddingLeft");
        assertPx(24, f, "c-inner", "paddingLeft");        // not 50: the nested container's padding is not inherited
        assertPx(800, f, "c-plain", "w");                 // not 320: a plain wrapper's max configures nothing
        assertPx(24, f, "c-plain", "paddingLeft");        // not 5
        assertPx(320, f, "c-inline-max", "w");            // inline max of 20rem
        assertPx(240, f, "c-inline-max", "left");         // centered in 800
        assertPx(480, f, "c-narrow-inline", "w");         // inline 30rem beats the --narrow modifier's 48rem
        assertPx(7, f, "c-unl", "paddingLeft");           // unlayered rule fixes the padding at this width
    }

    @Test
    void containerWidthCenteringAndResponsivePaddingAtEveryWidth() {
        for (int w : WIDTHS) {
            String f = "container-" + w;
            assertPx(Math.min(w, 1152), f, "cont", "w");
            assertPx(w < 640 ? 16 : w < 1024 ? 24 : 32, f, "cont", "paddingLeft");
            assertPx(w < 640 ? 16 : w < 1024 ? 24 : 32, f, "cont", "paddingRight");
            assertPx(w > 1152 ? (w - 1152) / 2.0 : 0, f, "cont", "left");
            assertPx(Math.min(w, 768), f, "narrow", "w");
            assertPx(w, f, "fluid", "w");
            assertPx(10, f, "pad", "paddingLeft");         // an explicit padding is used at every width
            assertEquals("border-box", val(f, "cont", "boxSizing"), f);
        }
    }

    @Test
    void stackIsAFlexColumnWithTheGapModifiers() {
        String f = "stack-gaps";
        for (String el : List.of("st", "st-sm", "st-lg", "st-xl")) {
            assertEquals("flex", val(f, el, "display"));
            assertEquals("column", val(f, el, "flexDirection"));
        }
        assertPx(16, f, "st", "rowGap");
        assertPx(8, f, "st-sm", "rowGap");
        assertPx(24, f, "st-lg", "rowGap");
        assertPx(40, f, "st-xl", "rowGap");
        // The gap is real: the second child starts one child-height plus the gap below the first.
        List<double[]> kids = kids(f, "st-lg");
        assertEquals(kids.get(0)[1] + kids.get(0)[3] + 24, kids.get(1)[1], 0.51);
    }

    /** Number of columns, counted from the items' own positions (not from the track list, which also counts collapsed auto-fit tracks). */
    private static long columns(String frame, String el) {
        return kids(frame, el).stream().map(k -> Math.round(k[0])).distinct().count();
    }

    // ---- isolation: every primitive ----

    @Test
    void isolationControlShowsThatWithoutTheResetAnOuterValueWouldLeak() {
        // CONTROL: a stack that lacks the reset inherits the outer 40px, so the isolation assertions above can fail.
        assertPx(40, "iso-control", "k-leaky", "rowGap");
        assertPx(16, "iso-control", "k-real", "rowGap");   // the real primitive does not leak
    }

    @Test
    void gridIgnoresInheritedMinimumAndGapButHonoursItsOwn() {
        // A wrapper's --ash-grid-min does not apply (800px at 16rem gives 3 columns); the element's own does (all 6 fit at 5rem).
        assertEquals(3, columns("iso-grid", "gm-wrapped"));
        assertEquals(6, columns("iso-grid", "gm-own"));
        assertPx(40, "iso-grid", "gm-outer", "columnGap");
        assertPx(16, "iso-grid", "gm-inner", "columnGap");  // not the outer 40px
    }

    @Test
    void splitIgnoresInheritedSideAndGapButHonoursItsOwn() {
        assertEquals(100.0, kids("iso-split", "sp-wrapped").get(0)[2], 0.51);   // not 240
        assertEquals(320.0, kids("iso-split", "sp-own").get(0)[2], 0.51);       // its own 20rem
        assertPx(40, "iso-split", "sp-outer", "columnGap");
        assertPx(16, "iso-split", "sp-inner", "columnGap");                      // nested split ignores the outer 40px
    }

    @Test
    void clusterIgnoresInheritedGapButHonoursItsOwn() {
        assertPx(16, "iso-cluster", "cl-wrapped", "columnGap");
        assertPx(40, "iso-cluster", "cl-outer", "columnGap");
        assertPx(16, "iso-cluster", "cl-inner", "columnGap");
    }

    // ---- grid ----

    @Test
    void autoFitGridHasAsManyColumnsAsFitAtTheMinimumWidth() {
        int[] expected = {1, 2, 2, 3, 3, 4};
        for (int i = 0; i < WIDTHS.length; i++) {
            assertEquals(expected[i], columns("grid-" + WIDTHS[i], "g6"), "6 items at " + WIDTHS[i]);
        }
        // With fewer items than columns the items grow to fill the row.
        assertEquals(1, columns("grid-320", "g2"));
        assertEquals(2, columns("grid-639", "g2"));
        assertEquals(2, columns("grid-1280", "g2"));
        assertEquals((1280 - 16) / 2.0, kids("grid-1280", "g2").get(0)[2], 0.51);
        assertEquals((1023 - 16) / 2.0, kids("grid-1023", "g2").get(0)[2], 0.51);
    }

    @Test
    void fixedColumnGridsAreOneColumnBelowFortyRemAndNColumnsFromFortyRem() {
        for (int w : WIDTHS) {
            String f = "grid-" + w;
            assertEquals(w < 640 ? 1 : 2, columns(f, "c2"), "cols-2 at " + w);
            assertEquals(w < 640 ? 1 : 3, columns(f, "c3"), "cols-3 at " + w);
            assertEquals(w < 640 ? 1 : 4, columns(f, "c4"), "cols-4 at " + w);
            assertEquals(w < 640 ? 1 : 3, columns(f, "cmin"), "--ash-grid-min is ignored by cols-N at " + w);
        }
        assertEquals((640 - 3 * 16) / 4.0, kids("grid-640", "c4").get(0)[2], 0.51);
    }

    @Test
    void gapModifiersAreTheSameOnGridSplitAndCluster() {
        for (String block : List.of("gr", "sp", "cl")) {
            assertPx(16, "gaps", block, "columnGap");
            assertPx(8, "gaps", block + "-sm", "columnGap");
            assertPx(24, "gaps", block + "-lg", "columnGap");
            assertPx(40, "gaps", block + "-xl", "columnGap");
        }
        assertPx(8, "gaps", "gr-sm", "rowGap");
        assertPx(40, "gaps", "gr-xl", "rowGap");
    }

    // ---- split ----

    @Test
    void splitStacksInSourceOrderBelowFortyRemAndSplitsFromFortyRem() {
        for (int w : WIDTHS) {
            String f = "split-" + w;
            for (String el : List.of("sp", "ss", "se")) {
                List<double[]> k = kids(f, el);
                if (w < 640) {
                    assertEquals(k.get(0)[0], k.get(1)[0], 0.51, el + " stacked at " + w);
                    assertTrue(k.get(1)[1] > k.get(0)[1], el + " second part below the first at " + w);
                    assertEquals(w, k.get(0)[2], 0.51, el + " full width at " + w);
                } else {
                    assertEquals(k.get(0)[1], k.get(1)[1], 0.51, el + " side by side at " + w);
                    assertTrue(k.get(1)[0] > k.get(0)[0], el + " second part to the right at " + w);
                }
            }
            if (w >= 640) {
                assertEquals((w - 16) / 2.0, kids(f, "sp").get(0)[2], 0.51);
                assertEquals((w - 16) / 2.0, kids(f, "sp").get(1)[2], 0.51);
                assertEquals(240, kids(f, "ss").get(0)[2], 0.51);              // --ash-split-side: 15rem
                assertEquals(w - 256, kids(f, "ss").get(1)[2], 0.51);
                assertEquals(100, kids(f, "sa").get(0)[2], 0.51);              // side defaults to auto: content-sized
                assertEquals(w - 116, kids(f, "sa").get(1)[2], 0.51);
                assertEquals(w - 256, kids(f, "se").get(0)[2], 0.51);          // --side-end: the second part is the side
                assertEquals(240, kids(f, "se").get(1)[2], 0.51);
            }
        }
    }

    // ---- cluster, truncate ----

    @Test
    void clusterWrapsAndPlacesItemsWithTheEndAndBetweenModifiers() {
        assertEquals(3, kids("cluster", "cl5").stream().map(k -> Math.round(k[1])).distinct().count());   // 2 + 2 + 1 items
        List<double[]> between = kids("cluster", "cl-between");
        assertEquals(0, between.get(0)[0], 0.51);
        assertEquals(250, between.get(1)[0], 0.51);
        List<double[]> end = kids("cluster", "cl-end");
        assertEquals(300 - 50 - 16 - 50, end.get(0)[0], 0.51);   // two 50px items and the 16px gap end at the right edge
        assertEquals(250, end.get(1)[0], 0.51);
        assertEquals("center", val("cluster", "cl5", "alignItems"));
        assertEquals("wrap", val("cluster", "cl5", "flexWrap"));
    }

    @Test
    void truncateClipsOverflowingTextWithAnEllipsisInsideAFlexParent() {
        assertEquals("hidden", val("truncate", "t", "overflow"));
        assertEquals("ellipsis", val("truncate", "t", "textOverflow"));
        assertEquals("nowrap", val("truncate", "t", "whiteSpace"));
        assertEquals("0px", val("truncate", "t", "minWidth"));
        assertEquals("true", val("truncate", "t", "clipped"));
        assertPx(200, "truncate", "t", "w");                                  // it shrank to its flex parent
        // Control: the same text without the class is not clipped and wraps.
        assertEquals("normal", val("truncate", "nt", "whiteSpace"));
        assertEquals("false", val("truncate", "nt", "clipped"));
    }

    // ---- RTL ----

    @Test
    void logicalPropertiesFlipUnderRightToLeft() {
        // split --side-start: the first part is the side column, at the inline start.
        assertEquals(0, kids("dir-ltr", "ss").get(0)[0], 0.51);
        assertEquals(256, kids("dir-ltr", "ss").get(1)[0], 0.51);
        assertEquals(560, kids("dir-rtl", "ss").get(0)[0], 0.51);             // 800 - 240: on the right
        assertEquals(0, kids("dir-rtl", "ss").get(1)[0], 0.51);
        // grid: the first item starts at the inline start.
        assertEquals(0, kids("dir-ltr", "gr").get(0)[0], 0.51);
        assertEquals(544, kids("dir-rtl", "gr").get(0)[0], 0.51);
        assertEquals(0, kids("dir-rtl", "gr").get(2)[0], 0.51);
        // cluster --end: packed to the inline end, which is the right in LTR and the left in RTL.
        assertEquals(750 + 50, kids("dir-ltr", "cl").get(2)[0] + kids("dir-ltr", "cl").get(2)[2], 0.51);
        assertEquals(0, kids("dir-rtl", "cl").get(2)[0], 0.51);
        assertEquals(132, kids("dir-rtl", "cl").get(0)[0], 0.51);
    }

    // ---- composition ----

    @Test
    void primitivesComposeOnOneElementAndNest() {
        assertEquals("flex", val("composition", "cs", "display"));
        assertEquals("column", val("composition", "cs", "flexDirection"));
        assertPx(1152, "composition", "cs", "w");
        assertPx(32, "composition", "cs", "paddingLeft");
        assertPx(16, "composition", "cs", "rowGap");
        // A split nested in a stack: --side-end gives the second part exactly 200px and the first the rest.
        assertEquals(1280 - 200 - 16, kids("composition", "nested-split").get(0)[2], 0.51);
        assertEquals(200, kids("composition", "nested-split").get(1)[2], 0.51);
    }

    // ---- integrated showcase: Tailwind plus Ashlar plus all ten skins ----

    private static String productionCss() throws IOException {
        try (InputStream in = AshlarLayoutBrowserTest.class.getResourceAsStream("/static/css/app.css")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void tailwindUtilitiesBeatThePrimitivesAndUnlayeredCssBeatsBoth() throws IOException {
        String css = productionCss();
        assertTrue(css.contains(".gap-10{") && css.contains(".flex-col{"),
                "the comparison needs real compiled Tailwind utilities, otherwise it proves nothing");
        assertPx(24, "tw-beat", "tb-lg", "rowGap");                            // the primitive works beside Tailwind
        assertPx(40, "tw-beat", "tb-utility", "rowGap");                       // Tailwind's gap-10 (layer utilities) beats ash.layout
        assertPx(3, "tw-beat", "tb-unlayered", "rowGap");                      // an unlayered rule beats both
        assertEquals("column", val("tw-beat", "tb-flex-col", "flexDirection")); // Tailwind's flex-col beats the cluster's row
    }

    @Test
    void theIntegratedFramesLoadTheProductionStylesheetAndAllTenSkins() {
        for (Frame f : FRAMES) {
            if (f.sheets().equals(INTEGRATED_SHEETS)) {
                String sheets = val(f.id(), "#frame", "sheets");
                assertTrue(sheets.startsWith("app.css,theme-axiom.css,"), f.id() + ": " + sheets);
                for (String skin : SKINS) {
                    assertTrue(sheets.contains("theme-" + skin + ".css"), f.id() + " is missing the " + skin + " skin");
                }
            }
        }
    }

    @Test
    void layoutIsIdenticalUnderEverySkinAndInLightAndDark() {
        List<String> elements = List.of("b-cont", "b-stack", "b-grid", "b-cols", "b-split", "b-cluster");
        Map<String, Map<String, String>> reference = MEASURED.get("bat-default-light");
        int compared = 0;
        for (Frame f : FRAMES) {
            if (!f.id().startsWith("bat-")) {
                continue;
            }
            for (String el : elements) {
                assertEquals(reference.get(el), MEASURED.get(f.id()).get(el), f.id() + " #" + el + " differs from the unskinned light layout");
                compared++;
            }
        }
        assertEquals(22 * elements.size(), compared);   // (default + 10 skins) x (light, dark) = 22 frames, the reference included
    }

    @AfterAll
    static void clear() {
        MEASURED.clear();
        FRAMES.clear();
    }
}
