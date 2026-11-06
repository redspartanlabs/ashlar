package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tree and Tree Item in a page that has Ashlar's stylesheet and NO Tailwind, and so no Tailwind Preflight. The parity fixtures all load Tailwind utilities (a host app has
 * them), so on their own they cannot see a Tree that quietly depends on a host utility such as {@code h-4 w-4}, or on a Preflight reset such as {@code list-style: none} or
 * {@code appearance: button}. This page loads only {@code ashlar.css}.
 *
 * <p>Three kinds of evidence. (1) Absolute: the chevron's and the item icon's classes are read from the shipped {@code tree-item.jte} and must compute to 16 by 16px; every
 * native element the component uses ({@code ul}, {@code li}, {@code button}, {@code a}, {@code svg}) shows the reset a browser's default would otherwise leave; each level is
 * indented 20px; a row is 32px. (2) A control on the same page proves Tailwind is absent ({@code ash-icon h-4 w-4} is 20px and a plain {@code h-4 w-4} box is not 16px).
 * (3) Equality: the same migrated markup is rendered with Tailwind's baseline stylesheet present and with Ashlar alone, and the COMPONENT-RELEVANT computed properties of every
 * element of both trees must be identical in light and dark.
 *
 * <p>The compared properties are listed in {@link #PROPERTIES}, and what is deliberately NOT compared is stated here. (1) {@code font-family} and the inherited text
 * {@code color} are the page's own: Preflight sets a font on {@code html}, the host sets the text colour on the body, the bare page has the browser's, and
 * {@code vertical-align: middle} on the icon places it by the parent font's metrics. So BOTH pages give the trees the same font and colour on a wrapper and the page-wide
 * difference stays out of the comparison (the body fonts are shown to differ). Only the rows give their text a colour, so this touches nothing the component draws. (2) A border
 * style is not compared, only the width:
 * Preflight's blanket {@code border: 0 solid} makes every element's invisible zero-width style differ from the bare page's {@code none}. (3) The drawing elements inside an
 * {@code svg} (the {@code path}) are not the component's, and Preflight's blanket {@code border-box} reaches them while Ashlar's scoped reset does not; the {@code svg} itself
 * is compared. Nothing else is excluded, and anything else that differs is a finding.
 */
class TreeTailwindFreeTest {

    private static final Pattern ICON_CLASSES = Pattern.compile("icon\\(icon\\s*=\\s*[A-Za-z_.]+\\s*,\\s*classes\\s*=\\s*\"([^\"]*)\"\\)");

    /**
     * The component-relevant computed properties compared between the page with Tailwind and the page without it. Everything the Tree draws or lays out is here: box, spacing,
     * borders, radius, colour, type, truncation, list markers, flex and grid, the chevron's turn and the panel's row, transitions, cursor, appearance, outline and shadow.
     * NOT compared, and why: see the class comment (font-family, the style of a zero-width border, the drawing elements of an svg).
     */
    private static final List<String> PROPERTIES = List.of("display", "position", "box-sizing", "margin-top", "margin-right", "margin-bottom", "margin-left", "padding-top", "padding-right",
            "padding-bottom", "padding-left", "border-top-width", "border-right-width", "border-bottom-width", "border-left-width", "border-top-left-radius", "border-top-right-radius",
            "border-bottom-right-radius", "border-bottom-left-radius", "list-style-type", "list-style-position", "color", "background-color", "background-image", "font-size", "font-weight",
            "line-height", "letter-spacing", "text-transform", "text-decoration-line", "text-align", "text-overflow", "white-space", "overflow-x", "overflow-y", "min-width", "min-height",
            "flex-grow", "flex-shrink", "flex-basis", "flex-direction", "align-items", "justify-content", "row-gap", "column-gap", "grid-template-rows", "rotate", "transition-property",
            "transition-duration", "transition-timing-function", "cursor", "appearance", "vertical-align", "outline-style", "box-shadow", "opacity", "fill", "stroke");

    private static final String SCRIPT = """
            (function () {
              var props = %s;
              var out = [];
              function size(id) { var e = document.getElementById(id); var s = getComputedStyle(e); var r = e.getBoundingClientRect();
                out.push(['size', id, s.width, s.height, r.x, r.width, r.height].join('\\t')); }
              try {
                ['probe', 'control-svg', 'a-chevron', 'a-chevsvg', 'a-iconsvg', 'a-row', 'b-row', 'c-row', 'd-row', 'e-row', 'f-row', 'g-row', 'k-row', 'a-toggle', 'c-spacer', 'x-row', 'y-row'].forEach(function (id) { var e = document.getElementById(id); if (e) { size(id); } });
                out.push(['sheets', [].map.call(document.styleSheets, function (s) { return s.href || 'inline'; }).join(',')].join('\\t'));
                [].filter.call(document.querySelectorAll('#t1, #t1 *, #t2, #t2 *'), function (e) { return !(e.parentNode && e.parentNode.closest && e.parentNode.closest('svg')); }).forEach(function (e, n) {
                  var s = getComputedStyle(e);
                  var name = n + ':' + e.tagName.toLowerCase() + '.' + (e.getAttribute('class') || '');
                  props.forEach(function (p) {
                    var v = s.getPropertyValue(p);
                    out.push(['prop', name, p, v].join('\\t'));
                  });
                  var r = e.getBoundingClientRect();
                  out.push(['prop', name, '@x', String(Math.round(r.x * 100) / 100)].join('\\t'));
                  out.push(['prop', name, '@w', String(Math.round(r.width * 100) / 100)].join('\\t'));
                  out.push(['prop', name, '@h', String(Math.round(r.height * 100) / 100)].join('\\t'));
                });
                var a = document.getElementById('a-label'); var c = document.getElementById('c-label');
                out.push(['font', getComputedStyle(document.body).fontFamily].join('\\t'));
                out.push(['label', getComputedStyle(a).color, getComputedStyle(c).color, getComputedStyle(c).textDecorationLine, getComputedStyle(c).cursor].join('\\t'));
              } catch (e) { out.push(['error', String(e)].join('\\t')); }
              var pre = document.createElement('pre'); pre.id = 'ash-tree-tailwind-free'; pre.textContent = out.join('\\n'); document.body.appendChild(pre);
            })();
            """.formatted("[" + String.join(",", PROPERTIES.stream().map(p -> "'" + p + "'").toList()) + "]");

    private static final String ROOT = "\\d+:ul\\.ash-tree";
    private static final String GROUP = "\\d+:ul\\.ash-tree__group";
    private static final String ITEM = "\\d+:li\\.ash-tree__item";
    private static final String TOGGLE = "\\d+:button\\.ash-tree__toggle";
    private static final String SVG = "\\d+:svg\\.ash-icon.*";

    private static List<String> templateIconClasses;
    private static final Map<String, List<String[]>> LINES = new LinkedHashMap<>();

    private static String page(List<String> sheets, String theme, String body) {
        StringBuilder links = new StringBuilder();
        sheets.forEach(href -> links.append("<link rel=\"stylesheet\" href=\"").append(href).append("\">"));
        return "<!doctype html><html data-theme=\"" + theme + "\"><head><meta charset=\"utf-8\">" + links + "<style>*,*::before,*::after{transition:none !important}</style></head>"
                + "<body style=\"margin:8px;width:400px\"><div style=\"font-family:Arial,Helvetica,sans-serif;color:rgb(15, 23, 42)\">" + body + "</div>"
                + "<div id=\"probe\" class=\"h-4 w-4\"></div><svg id=\"control-svg\" class=\"ash-icon h-4 w-4\" viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M9 5l7 7-7 7\"/></svg>"
                + "<script>" + SCRIPT.replace("</script", "<\\/script") + "</script></body></html>";
    }

    /** The trees, with the ids the script reads; the icon classes are the ones the shipped template passes to the Icon component. */
    private static String body() {
        String icon = templateIconClasses.get(0);
        String tree = TreeBrowser.body(false, null, null);
        // Give the chevron and the item icon svgs ids and the template's own icon classes.
        tree = tree.replaceAll("(<span data-tree-chevron id=\"([a-z])-chevron\" class=\"ash-tree__chevron\">)<svg class=\"ash-icon ash-icon--sm\"", "$1<svg id=\"$2-chevsvg\" class=\"ash-icon " + icon + "\"");
        tree = tree.replaceFirst("(<span class=\"ash-tree__icon\" aria-hidden=\"true\">)<svg class=\"ash-icon ash-icon--sm\"", "$1<svg id=\"a-iconsvg\" class=\"ash-icon " + icon + "\"");
        return tree;
    }

    @BeforeAll
    static void measure() throws Exception {
        String template;
        try (InputStream in = TreeTailwindFreeTest.class.getResourceAsStream("/ashlar/components/tree-item.jte")) {
            if (in == null) {
                throw new IOException("tree-item.jte is not on the test classpath");
            }
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = ICON_CLASSES.matcher(template);
        templateIconClasses = new ArrayList<>();
        while (m.find()) {
            templateIconClasses.add(m.group(1));
        }
        ParitySession session = ParitySession.open("tree-tailwind-free");
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            for (String theme : List.of("light", "dark")) {
                LINES.put("free-" + theme, read(session, server, page(List.of(ParityFrames.ASHLAR), theme, body())));
                LINES.put("tailwind-" + theme, read(session, server, page(List.of(ParityFrames.APP), theme, body())));
            }
        }
    }

    private static List<String[]> read(ParitySession session, CascadeFixtureServer server, String page) throws Exception {
        String dom = session.chrome().dumpDom(server.registerRaw(page));
        return ParityProbe.lines(dom, "ash-tree-tailwind-free").stream().map(l -> l.split("\t", -1)).toList();
    }

    private static String[] size(String key, String id) {
        return LINES.get(key).stream().filter(f -> f[0].equals("size") && f[1].equals(id)).findFirst().orElseThrow(() -> new AssertionError("no size for " + id));
    }

    private static String prop(String key, String namePrefix, String property) {
        List<String[]> found = LINES.get(key).stream().filter(f -> f[0].equals("prop") && f[1].matches(namePrefix) && f[2].equals(property)).toList();
        assertFalse(found.isEmpty(), namePrefix + " " + property);
        return found.get(0)[3];
    }

    // ---- the template ----

    @Test
    void theTemplatePassesTheSmallIconModifierAndNoTailwindSizeToBothIcons() {
        assertEquals(List.of("ash-icon--sm", "ash-icon--sm"), templateIconClasses, "the chevron and the item icon, read from the shipped tree-item.jte");
    }

    // ---- absolute ----

    @Test
    void thePageHasAshlarsStylesheetAndNoTailwind() {
        String[] sheets = LINES.get("free-light").stream().filter(f -> f[0].equals("sheets")).findFirst().orElseThrow();
        List<String> linked = List.of(sheets[1].split(",")).stream().filter(s -> !s.equals("inline")).toList();
        assertEquals(1, linked.size(), "only Ashlar's stylesheet is linked: " + sheets[1]);
        assertTrue(linked.get(0).endsWith("/ashlar-real/ashlar.css"), sheets[1]);
        String[] probe = size("free-light", "probe");
        assertNotEquals("16px", probe[2], "h-4 w-4 on a plain box did nothing: no Tailwind utility reached the page: " + String.join(" | ", probe));
        assertNotEquals("16px", probe[3], String.join(" | ", probe));
        assertEquals("16px", size("tailwind-light", "probe")[2], "and with the app stylesheet the same box IS 16px, so the probe can tell the two pages apart");
        String[] control = size("free-light", "control-svg");
        assertEquals("20px", control[2], "ash-icon h-4 w-4 falls back to the Icon default when nothing supplies h-4 w-4: " + String.join(" | ", control));
        assertEquals("20px", control[3], String.join(" | ", control));
    }

    @Test
    void withoutTailwindTheChevronAndTheItemIconAreStillSixteenBySixteen() {
        for (String id : List.of("a-chevsvg", "a-iconsvg")) {
            String[] r = size("free-light", id);
            assertEquals("16px", r[2], id + " computed width " + String.join(" | ", r));
            assertEquals("16px", r[3], id + " computed height " + String.join(" | ", r));
            assertEquals("16.0", String.valueOf(Double.parseDouble(r[5])), id + " rendered width " + String.join(" | ", r));
            assertEquals("16.0", String.valueOf(Double.parseDouble(r[6])), id + " rendered height " + String.join(" | ", r));
        }
    }

    @Test
    void withoutPreflightEveryNativeElementShowsTheResetABrowsersDefaultWouldLeave() {
        for (String key : List.of("free-light", "free-dark")) {
            // lists: no marker, no margin, no padding on the root; the group is indented 20px and nothing else
            assertEquals("none", prop(key, ROOT, "list-style-type"), key);
            assertEquals("0px", prop(key, ROOT, "padding-left"), key + ": the root has no browser padding");
            assertEquals("0px", prop(key, ROOT, "margin-top"), key + ": and no browser margin");
            assertEquals("none", prop(key, GROUP, "list-style-type"), key);
            assertEquals("20px", prop(key, GROUP, "padding-left"), key + ": a group is indented 20px, not the browser's 40px");
            assertEquals("0px", prop(key, GROUP, "margin-top"), key);
            assertEquals("none", prop(key, ITEM, "list-style-type"), key + ": an item has no marker");
            assertEquals("list-item", prop(key, ITEM, "display"), key);
            // buttons: no border, no padding, transparent, the page's font, the native push-button appearance, a pointer
            assertEquals("0px", prop(key, TOGGLE, "border-top-width"), key);
            assertEquals("0px", prop(key, TOGGLE, "padding-top"), key);
            assertEquals("rgba(0, 0, 0, 0)", prop(key, TOGGLE, "background-color"), key);
            assertEquals("14px", prop(key, TOGGLE, "font-size"), key + ": the button inherits the tree's text size, not the browser's 13.33px");
            assertEquals("20px", prop(key, TOGGLE, "line-height"), key);
            assertEquals("button", prop(key, TOGGLE, "appearance"), key);
            assertEquals("pointer", prop(key, TOGGLE, "cursor"), key);
            assertEquals("none", prop(key, TOGGLE, "text-transform"), key);
            assertEquals("0px", prop(key, TOGGLE, "margin-top"), key);
            // links: not the browser's blue, not underlined, a pointer
            String[] label = LINES.get(key).stream().filter(f -> f[0].equals("label")).findFirst().orElseThrow();
            assertNotEquals("rgb(0, 0, 238)", label[2], key + ": a link is not the browser's blue");
            assertNotEquals("rgb(85, 26, 139)", label[2], key);
            assertEquals(label[1], label[2], key + ": a link and a plain label have one colour");
            assertEquals("none", label[3], key + ": and the link is not underlined");
            assertEquals("pointer", label[4], key);
            // svg: sized by the Icon, vertical-align stated
            assertEquals("middle", prop(key, SVG, "vertical-align"), key);
        }
    }

    @Test
    void aRowIsThirtyTwoPixelsAndEachLevelIsIndentedTwentyPixelsWithoutTailwind() {
        for (String key : List.of("free-light", "free-dark")) {
            assertEquals("32", String.valueOf(Math.round(Double.parseDouble(size(key, "a-row")[6]))), key);
            double a = Double.parseDouble(size(key, "a-row")[4]);
            double b = Double.parseDouble(size(key, "b-row")[4]);
            double c = Double.parseDouble(size(key, "c-row")[4]);
            double e = Double.parseDouble(size(key, "e-row")[4]);
            assertEquals(20.0, b - a, 0.01, key + ": level 2");
            assertEquals(20.0, c - b, 0.01, key + ": level 3");
            assertEquals(20.0, e - c, 0.01, key + ": level 4");
            assertEquals(Double.parseDouble(size(key, "a-toggle")[5]), Double.parseDouble(size(key, "c-spacer")[5]), 0.01, key + ": a leaf's spacer is as wide as a parent's toggle, so labels line up");
        }
    }

    // ---- equality ----

    @Test
    void everyComponentRelevantComputedPropertyOfEveryElementIsIdenticalWithAndWithoutTailwind() {
        for (String theme : List.of("light", "dark")) {
            Map<String, String> with = values("tailwind-" + theme);
            Map<String, String> without = values("free-" + theme);
            assertEquals(with.keySet(), without.keySet(), theme + ": the two pages measured different things");
            assertTrue(with.size() > 3000, "a real measurement: " + with.size() + " values");
            List<String> differences = new ArrayList<>();
            for (Map.Entry<String, String> e : with.entrySet()) {
                if (!e.getValue().equals(without.get(e.getKey()))) {
                    differences.add(theme + " " + e.getKey() + ": with Tailwind " + e.getValue() + " -> without " + without.get(e.getKey()));
                }
            }
            assertEquals(List.of(), differences.stream().limit(25).toList(), differences.size() + " differences in " + theme);
        }
    }

    @Test
    void theEqualityCheckCanFailAndTheTwoPagesReallyDifferInTheirGlobalStyles() {
        Map<String, String> with = values("tailwind-light");
        Map<String, String> without = values("free-light");
        // font-family is deliberately not compared; show why: the pages do differ in it, so it is a global stylesheet difference and not a component property.
        assertFalse(PROPERTIES.contains("font-family"));
        String withFont = LINES.get("tailwind-light").stream().filter(f -> f[0].equals("font")).findFirst().orElseThrow()[1];
        String withoutFont = LINES.get("free-light").stream().filter(f -> f[0].equals("font")).findFirst().orElseThrow()[1];
        assertNotEquals(withFont, withoutFont, "Preflight sets a font on html and the bare page has the browser default: a page-wide difference the component does not own, so both pages pin the trees' font");
        // The comparison is sensitive: change one value and it is seen.
        String key = without.keySet().stream().filter(k -> k.endsWith("|padding-left")).findFirst().orElseThrow();
        Map<String, String> tampered = new LinkedHashMap<>(without);
        tampered.put(key, "999px");
        assertEquals(1, with.entrySet().stream().filter(e -> !e.getValue().equals(tampered.get(e.getKey()))).count(), "exactly the tampered value is a difference");
    }

    private static Map<String, String> values(String key) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String[] f : LINES.get(key)) {
            if (f[0].equals("prop")) {
                out.put(f[1] + "|" + f[2], f[3]);
            }
        }
        return out;
    }
}
