package dev.redspartanlabs.ashlar.css;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the stylesheet contract of the Table family: {@code table.css}, {@code table-column.css}, {@code table-row.css} and {@code table-cell.css}. Every selector belongs to the family,
 * the eighteen colour properties are declared on the root only (and flip in the dark theme), every palette colour has Tailwind's own value as its fallback, state is the native attribute
 * (aria-sort, data-selected, hidden) and never a class, hover sits under the hover-capability condition, and the geometry is the one the Tailwind classes measured. The result is measured
 * in a real browser by the showcase's {@code TableParityTest} and its siblings; this reads the sources only.
 */
class TableCssContractTest {

    private static final List<String> FILES = List.of("table", "table-column", "table-row", "table-cell");
    private static final String DARK = ":where([data-theme=\"dark\"], [data-theme=\"dark\"] *)";
    private static final String HOVER = "@media (hover: hover)";
    private static final String WIDE = "@media (min-width: 40rem)";

    /** Tailwind v4.3's own values for every palette colour the family reads (checked against its theme file when this was written). */
    private static final Map<String, String> TAILWIND = Map.ofEntries(
            Map.entry("--color-white", "#fff"),
            Map.entry("--color-slate-50", "oklch(98.4% 0.003 247.858)"), Map.entry("--color-slate-100", "oklch(96.8% 0.007 247.896)"),
            Map.entry("--color-slate-200", "oklch(92.9% 0.013 255.508)"), Map.entry("--color-slate-300", "oklch(86.9% 0.022 252.894)"),
            Map.entry("--color-slate-400", "oklch(70.4% 0.04 256.788)"), Map.entry("--color-slate-500", "oklch(55.4% 0.046 257.417)"),
            Map.entry("--color-slate-600", "oklch(44.6% 0.043 257.281)"), Map.entry("--color-slate-700", "oklch(37.2% 0.044 257.287)"),
            Map.entry("--color-slate-800", "oklch(27.9% 0.041 260.031)"), Map.entry("--color-slate-900", "oklch(20.8% 0.042 265.755)"),
            Map.entry("--color-blue-50", "oklch(97% 0.014 254.604)"), Map.entry("--color-blue-300", "oklch(80.9% 0.105 251.813)"),
            Map.entry("--color-blue-400", "oklch(70.7% 0.165 254.624)"), Map.entry("--color-blue-500", "oklch(62.3% 0.214 259.815)"),
            Map.entry("--color-blue-600", "oklch(54.6% 0.245 262.881)"), Map.entry("--color-blue-700", "oklch(48.8% 0.243 264.376)"),
            Map.entry("--color-blue-950", "oklch(28.2% 0.091 267.935)"));

    private static final List<String> PROPERTIES = List.of("--ash-table-surface", "--ash-table-border", "--ash-table-head", "--ash-table-ink", "--ash-table-ink-cell", "--ash-table-ink-muted",
            "--ash-table-ink-faint", "--ash-table-ink-pager", "--ash-table-control-border", "--ash-table-control-surface", "--ash-table-page-ink", "--ash-table-page-hover",
            "--ash-table-selection", "--ash-table-selection-ink", "--ash-table-row-hover", "--ash-table-row-selected", "--ash-table-sort-hover", "--ash-table-sort-active", "--ash-table-ring");

    private static Path module;

    @BeforeAll
    static void locate() {
        String generated = System.getProperty("ashlar.css.generated");
        assertNotNull(generated, "ashlar.css.generated must be set by the Gradle test task");
        for (Path p = Path.of(generated).toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("src/main/css/ashlar.css"))) {
                module = p;
                break;
            }
        }
        assertNotNull(module, "could not find the ashlar module above " + generated);
    }

    private static String read(String relative) throws IOException {
        return Files.readString(module.resolve(relative), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static String css(String name) throws IOException {
        return read("src/main/css/components/" + name + ".css");
    }

    private static String stripped(String name) throws IOException {
        return css(name).replaceAll("(?s)/\\*.*?\\*/", "");
    }

    private static List<CssScanner.Rule> rules(String name) throws IOException {
        return CssScanner.flatRules(stripped(name));
    }

    private static Map<String, String> declarations(String cssName, String selector) throws IOException {
        return declarations(cssName, selector, "");
    }

    private static Map<String, String> declarations(String cssName, String selector, String media) throws IOException {
        List<CssScanner.Rule> found = rules(cssName).stream().filter(r -> r.selector().replaceAll("\\s+", " ").equals(selector) && r.media().equals(media)).toList();
        assertEquals(1, found.size(), selector + " must be defined exactly once in " + cssName + (media.isEmpty() ? "" : " inside " + media) + "; rules: "
                + rules(cssName).stream().map(r -> r.media() + "|" + r.selector()).toList());
        return CssScanner.declarations(found.get(0).block());
    }

    private static List<CssScanner.Rule> all() throws IOException {
        List<CssScanner.Rule> all = new ArrayList<>();
        for (String name : FILES) {
            all.addAll(rules(name));
        }
        return all;
    }

    // ---- scope and structure ----

    @Test
    void theEntryFileImportsTheFourInTheComponentsLayerAfterFileUpload() throws IOException {
        String entry = read("src/main/css/ashlar.css");
        int upload = entry.indexOf("@import \"components/file-upload.css\" layer(ash.components);");
        assertTrue(upload > 0);
        String expected = "@import \"components/table.css\" layer(ash.components);\n@import \"components/table-column.css\" layer(ash.components);\n"
                + "@import \"components/table-row.css\" layer(ash.components);\n@import \"components/table-cell.css\" layer(ash.components);";
        assertTrue(entry.contains(expected), "the four imports are in this order");
        assertTrue(entry.indexOf(expected) > upload);
    }

    @Test
    void everySelectorIsScopedToTheFamilyAndNothingIsGlobal() throws IOException {
        Pattern own = Pattern.compile("^(?:\\.ash-table(?:__[a-z-]+)?(?:--[a-z]+)?(?:[:\\[\\s>.].*)?|:where\\(\\.ash-table__body > :not\\(:last-child\\)\\))$");
        for (String name : FILES) {
            for (CssScanner.Rule rule : rules(name)) {
                for (String selector : CssScanner.selectors(rule.selector())) {
                    String s = selector.replaceAll("\\s+", " ").trim();
                    assertTrue(own.matcher(s).matches(), name + ": a selector that is not the family's own: " + s);
                    assertFalse(s.startsWith("*") || s.startsWith(":root") || s.startsWith("html") || s.startsWith("body") || s.matches("^[a-z].*"), name + ": a global selector: " + s);
                }
            }
        }
    }

    @Test
    void noRuleIsImportantAndTheOnlyConditionsAreHoverAndTheWideBreakpoint() throws IOException {
        for (String name : FILES) {
            assertFalse(stripped(name).contains("!important"), name + ": no !important");
            assertFalse(stripped(name).contains("@import") || stripped(name).contains("@layer"), name + ": the entry file decides the layer");
        }
        Set<String> conditions = new LinkedHashSet<>();
        for (CssScanner.Rule r : all()) {
            conditions.add(r.media());
        }
        assertEquals(Set.of("", HOVER, WIDE), conditions);
    }

    @Test
    void nothingAnimates() throws IOException {
        for (String name : FILES) {
            String s = stripped(name);
            assertFalse(s.contains("transition") || s.contains("animation") || s.contains("@keyframes") || s.contains("prefers-reduced-motion"), name + ": the Table family has no motion");
        }
    }

    // ---- colour ----

    @Test
    void theRootDeclaresTheNineteenPropertiesAndTheOtherFilesOnlyReadThem() throws IOException {
        Map<String, String> light = declarations("table", ".ash-table");
        Map<String, String> dark = declarations("table", ".ash-table" + DARK);
        assertEquals(19, PROPERTIES.size());
        for (String property : PROPERTIES) {
            assertTrue(light.containsKey(property), "light declares " + property);
            if (!property.equals("--ash-table-ring")) {
                assertTrue(dark.containsKey(property), "dark flips " + property);
            }
        }
        assertEquals(18, dark.size(), "the dark block flips every property but the ring, which is the same blue in both themes");
        assertFalse(dark.containsKey("--ash-table-ring"));
        for (String property : light.keySet()) {
            if (property.startsWith("--")) {
                assertTrue(PROPERTIES.contains(property), "an undocumented property: " + property);
            }
        }
        for (String other : List.of("table-column", "table-row", "table-cell")) {
            assertFalse(Pattern.compile("--ash-table-[a-z-]+\\s*:").matcher(stripped(other)).find(), other + " declares no property; it only reads the root's");
        }
        assertTrue(light.get("--ash-table-row-selected").startsWith("color-mix(in oklab, var(--color-blue-50, oklch(97% 0.014 254.604)) 60%, transparent)"));
        assertEquals("color-mix(in oklab, var(--color-blue-950, " + TAILWIND.get("--color-blue-950") + ") 40%, transparent)", dark.get("--ash-table-row-selected"));
        assertEquals("color-mix(in oklab, var(--color-slate-800, " + TAILWIND.get("--color-slate-800") + ") 60%, transparent)", dark.get("--ash-table-row-hover"));
    }

    @Test
    void everyPaletteColourHasTailwindsOwnValueAsItsFallback() throws IOException {
        Pattern read = Pattern.compile("var\\(\\s*(--color-[a-z]+(?:-\\d+)?)\\s*(?:,\\s*([^()]*(?:\\([^()]*\\))?[^()]*))?\\)");
        int n = 0;
        for (String name : FILES) {
            Matcher m = read.matcher(stripped(name));
            while (m.find()) {
                String fallback = m.group(2) == null ? null : m.group(2).trim();
                assertNotNull(fallback, name + ": " + m.group(1) + " has a fallback");
                assertEquals(TAILWIND.get(m.group(1)), fallback, name + ": " + m.group(1) + " falls back to Tailwind's own value");
                n++;
            }
        }
        assertTrue(n >= 35, "palette reads: " + n);
    }

    @Test
    void typeAndShapeTokensAreTheOnesTailwindReadWithTheirFallbacks() throws IOException {
        for (String name : FILES) {
            for (Matcher m = Pattern.compile("var\\((--(?:text|font-weight|radius|font-mono)[a-z0-9-]*)(,?)").matcher(stripped(name)); m.find(); ) {
                assertEquals(",", m.group(2), name + ": " + m.group(1) + " has a fallback");
            }
        }
        assertEquals("var(--radius-xl, 0.75rem)", declarations("table", ".ash-table").get("border-radius"), "the root radius is the skin-overridable Tailwind radius");
        assertEquals("var(--ash-radius-md)", declarations("table", ".ash-table__search-input").get("border-radius"));
    }

    // ---- state ----

    @Test
    void hiddenIsStatedForEveryElementThatHasADisplayOfItsOwn() throws IOException {
        for (String selector : List.of(".ash-table__selection[hidden]", ".ash-table__scroll[hidden]", ".ash-table__empty[hidden]")) {
            assertEquals(Map.of("display", "none"), declarations("table", selector), selector);
        }
        assertFalse(rules("table-row").stream().anyMatch(r -> r.selector().contains("[hidden]")), "a hidden row needs no rule: a row has no display of its own");
        assertFalse(declarations("table-row", ".ash-table__row[data-selected]").containsKey("display"));
    }

    @Test
    void theSortGlyphTurnsActiveOffTheHeadingsAriaSortAndNoClass() throws IOException {
        List<CssScanner.Rule> active = rules("table-column").stream().filter(r -> r.selector().contains("aria-sort")).toList();
        assertEquals(1, active.size());
        List<String> selectors = CssScanner.selectors(active.get(0).selector()).stream().map(s -> s.replaceAll("\\s+", " ").trim()).toList();
        assertEquals(List.of(".ash-table__column[aria-sort=\"ascending\"] .ash-table__sort-icon", ".ash-table__column[aria-sort=\"descending\"] .ash-table__sort-icon"), selectors);
        assertEquals(Map.of("color", "var(--ash-table-sort-active)"), CssScanner.declarations(active.get(0).block()));
        assertEquals("var(--ash-table-ink-faint)", declarations("table-column", ".ash-table__sort-icon").get("color"), "an unsorted glyph is faint");
    }

    @Test
    void aSelectedRowIsTheDataSelectedAttributeAndAHoveredRowTakesTheHoverFillEvenWhenSelected() throws IOException {
        assertEquals(Map.of("background-color", "var(--ash-table-row-selected)"), declarations("table-row", ".ash-table__row[data-selected]"));
        assertEquals(Map.of("background-color", "var(--ash-table-row-hover)"), declarations("table-row", ".ash-table__row:hover", HOVER));
        List<String> order = rules("table-row").stream().map(r -> r.media() + "|" + r.selector()).toList();
        assertTrue(order.indexOf("|.ash-table__row[data-selected]") < order.indexOf(HOVER + "|.ash-table__row:hover"), "the hover fill comes later at equal specificity, so it wins");
    }

    @Test
    void everyHoverRuleIsInsideTheHoverCapabilityCondition() throws IOException {
        int hover = 0;
        for (CssScanner.Rule r : all()) {
            if (r.selector().contains(":hover")) {
                assertEquals(HOVER, r.media(), "a hover rule outside the hover condition: " + r.selector());
                hover++;
            }
        }
        assertEquals(4, hover, "the clear button, the page buttons, the sort button and the row");
    }

    // ---- geometry and the rules Preflight used to supply ----

    @Test
    void theDividerIsTailwindsDivideYAtZeroSpecificity() throws IOException {
        Map<String, String> d = declarations("table", ":where(.ash-table__body > :not(:last-child))");
        assertEquals("solid", d.get("border-style"));
        assertEquals("0 0 1px", d.get("border-width"), "a rule under every child but the last, hidden or not");
        assertEquals("var(--ash-table-border)", d.get("border-color"));
    }

    @Test
    void theToolbarAndTheFooterAreColumnsThatBecomeRowsAtFortyRem() throws IOException {
        for (String part : List.of(".ash-table__toolbar", ".ash-table__footer")) {
            assertEquals("column", declarations("table", part).get("flex-direction"));
            Map<String, String> wide = declarations("table", part, WIDE);
            assertEquals(Map.of("flex-direction", "row", "align-items", "center", "justify-content", "space-between"), wide);
        }
        assertEquals("0 0 1px", declarations("table", ".ash-table__toolbar").get("border-width"));
        assertEquals("1px 0 0", declarations("table", ".ash-table__footer").get("border-width"));
        assertEquals(Map.of("width", "16rem"), declarations("table", ".ash-table__search", WIDE));
    }

    @Test
    void theTableIsTheMeasuredSurface() throws IOException {
        Map<String, String> root = declarations("table", ".ash-table");
        assertEquals("hidden", root.get("overflow"));
        assertEquals("1px", root.get("border-width"));
        assertEquals("var(--ash-table-border)", root.get("border-color"));
        Map<String, String> table = declarations("table", ".ash-table__table");
        assertEquals("collapse", table.get("border-collapse"), "Preflight's table reset is stated");
        assertEquals("0", table.get("text-indent"));
        assertEquals("inherit", table.get("border-color"));
        assertEquals("100%", table.get("width"));
        assertEquals("var(--text-sm, 0.875rem)", table.get("font-size"));
        assertEquals("100%", declarations("table", ".ash-table__scroll").get("width"));
        assertEquals("auto", declarations("table", ".ash-table__scroll").get("overflow-x"));
    }

    @Test
    void theColumnsRowsAndCellsHaveTheMeasuredPaddingAlignmentAndWrapping() throws IOException {
        assertEquals("0.625rem", declarations("table-column", ".ash-table__column").get("padding-block"));
        assertEquals("1rem", declarations("table-column", ".ash-table__column").get("padding-inline"));
        assertEquals("right", declarations("table-column", ".ash-table__column--end").get("text-align"));
        assertEquals("center", declarations("table-column", ".ash-table__column--center").get("text-align"));
        Map<String, String> cell = declarations("table-cell", ".ash-table__cell");
        assertEquals("0.625rem", cell.get("padding-block"));
        assertEquals("1rem", cell.get("padding-inline"));
        assertEquals("left", cell.get("text-align"));
        assertEquals("break-word", cell.get("overflow-wrap"), "a cell wraps long words by default");
        assertEquals(Map.of("overflow-wrap", "normal", "white-space", "nowrap"), declarations("table-cell", ".ash-table__cell--nowrap"));
        Map<String, String> mono = declarations("table-cell", ".ash-table__cell--mono");
        assertEquals("0.8125rem", mono.get("font-size"));
        assertFalse(mono.containsKey("line-height"), "a monospaced cell keeps the table's inherited line height");
        assertTrue(mono.get("font-family").startsWith("var(--font-mono, ui-monospace,"));
        assertEquals("var(--font-weight-medium, 500)", declarations("table-cell", ".ash-table__cell--header").get("font-weight"));
        assertEquals("2.5rem", declarations("table-row", ".ash-table__select").get("width"));
    }

    @Test
    void theSortButtonInheritsItsHeadingsCasingAndEveryButtonResetIsStated() throws IOException {
        Map<String, String> sort = declarations("table-column", ".ash-table__sort");
        assertEquals("inherit", sort.get("text-transform"), "a skin that sets header casing reaches the label");
        assertEquals("0.25rem", sort.get("border-radius"), "a fixed radius, as Tailwind's bare rounded was");
        assertEquals("var(--font-weight-semibold, 600)", sort.get("font-weight"));
        for (Map<String, String> button : List.of(sort, declarations("table", ".ash-table__clear"), declarations("table", ".ash-table__page-button"))) {
            assertEquals("inherit", button.get("font"), "a button reads the surrounding font");
            assertEquals("inherit", button.get("letter-spacing"));
            assertEquals("transparent", button.get("background-color"));
            assertEquals("1", button.get("opacity"));
            assertEquals("button", button.get("appearance"));
        }
        Map<String, String> checkbox = declarations("table-row", ".ash-table__checkbox");
        assertEquals("pointer", checkbox.get("cursor"));
        assertEquals("0.25rem", checkbox.get("border-radius"));
        assertEquals("var(--color-blue-600, " + TAILWIND.get("--color-blue-600") + ")", checkbox.get("accent-color"));
        assertEquals("1rem", checkbox.get("width"));
        assertEquals("1rem", checkbox.get("height"));
    }

    @Test
    void theFocusRingsAreTheOuterBoxShadowsTheTailwindClassesDrew() throws IOException {
        String ring = "var(--ash-table-ring)";
        assertEquals("0 0 0 2px #fff, 0 0 0 4px " + ring, declarations("table", ".ash-table__clear:focus-visible").get("box-shadow"));
        assertEquals("0 0 0 2px #fff, 0 0 0 4px " + ring, declarations("table-row", ".ash-table__checkbox:focus-visible").get("box-shadow"));
        assertEquals("0 0 0 2px " + ring, declarations("table-column", ".ash-table__sort:focus-visible").get("box-shadow"));
        assertEquals("0 0 0 2px " + ring, declarations("table", ".ash-table__page-button:focus-visible").get("box-shadow"));
        Map<String, String> search = declarations("table", ".ash-table__search-input:focus");
        assertEquals("0 0 0 2px " + ring, search.get("box-shadow"));
        assertEquals(ring, search.get("border-color"));
        assertEquals("none", search.get("outline-style"));
        for (String selector : List.of(".ash-table__clear:focus", ".ash-table__checkbox:focus", ".ash-table__sort:focus", ".ash-table__page-button:focus")) {
            String file = selector.contains("checkbox") ? "table-row" : selector.contains("sort") ? "table-column" : "table";
            assertEquals("none", declarations(file, selector).get("outline-style"), selector + " removes the outline on :focus, as focus:outline-none did");
        }
        assertEquals(Map.of("cursor", "not-allowed", "opacity", "0.5"), declarations("table", ".ash-table__page-button:disabled"));
    }

    @Test
    void theSearchFieldKeepsItsPlaceholderItsDecorationAndItsLeadingIcon() throws IOException {
        Map<String, String> placeholder = declarations("table", ".ash-table__search-input::placeholder");
        assertEquals("var(--color-slate-400, " + TAILWIND.get("--color-slate-400") + ")", placeholder.get("color"), "one placeholder colour in both themes, as before");
        assertEquals("1", placeholder.get("opacity"));
        assertEquals(Map.of("-webkit-appearance", "none"), declarations("table", ".ash-table__search-input::-webkit-search-decoration"));
        Map<String, String> icon = declarations("table", ".ash-table__search-icon");
        assertEquals("none", icon.get("pointer-events"));
        assertEquals("absolute", icon.get("position"));
        assertEquals("0.5rem", icon.get("left"));
        assertEquals("2rem", declarations("table", ".ash-table__search-input").get("padding-left"));
    }
}
