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
 * The parity gates of Nav Link, Navbar, Sidebar and Sidebar Item (migrated together), measured in real Chrome on the shared parity harness (unchanged).
 * They are JS-free, so the harness's computed-style and box measurement applies; hover, active and keyboard focus are {@link NavigationStateParityTest}.
 *
 * <p>Evidence is a controlled oracle: the components' legacy Tailwind markup (compiled by the real Tailwind CLI) next to their migrated markup, in the same
 * browser, for each of the 22 theme and skin configurations, with Tailwind present and with Tailwind removed. The legacy markup is the migrated markup
 * rewritten by {@link NavigationLegacyMarkup}, so both sides are the same navigation, and they carry the same {@code data-nav-link}, {@code data-navbar} and
 * {@code data-sidebar} hooks, which is what the skins select. The first Navbar is the page's first child of {@code body} (the shape the skins' masthead rules
 * key on); the Mobile menu list is shown the way it is inside an open Drawer, so the Navbar's second rendering of the same items is measured too.
 *
 * <p>The Navbar has one breakpoint, 48rem, so every one of the 22 configurations is measured on a phone-sized (500px) and a desktop-sized (900px) viewport,
 * and the four theme and skin configurations that matter for non-colour properties are measured at 320, 767, 768, 769 and 1300px: below, at and above it.
 * Negative controls show each gate can fail, including controls that remove the hooks the skins select.
 *
 * <p>Evidence is Chrome only. Computed-style and box parity is the visual evidence; there is no screenshot comparison.
 */
class NavigationParityTest {

    private static final String ORACLE = ParityFrames.generated("navigation-oracle.css");
    private static final String SCOPED = ParityFrames.generated("navigation-legacy-scoped.css");
    private static final List<ParityConfigs.Config> CONFIGS = ParityConfigs.all();
    private static final List<ParityConfigs.Config> EDGE_CONFIGS = ParityConfigs.reduced();
    private static final List<Integer> WIDTHS = List.of(500, 900);
    private static final List<Integer> EDGE_WIDTHS = List.of(320, 767, 768, 769, 1300);
    private static final List<String> EXTRA_PROPS = List.of("font-family", "font-size", "font-weight", "line-height", "letter-spacing", "text-decoration-line", "text-align",
            "white-space", "text-overflow", "cursor", "list-style-type", "list-style-position", "row-gap", "column-gap", "align-items", "justify-content", "flex-direction",
            "flex-wrap", "outline-style", "padding-inline-start", "margin-inline-start", "background-image");

    private static final Set<String> CALLER_KINDS = Set.of("raw");

    private static List<ParityProbe.Row> owned(List<ParityProbe.Row> rows) {
        return rows.stream().filter(r -> !CALLER_KINDS.contains(r.kind())).toList();
    }

    // ---- state of the measurement ----

    private static NavigationLegacyMarkup navigationLegacy;
    private static IconLegacyMarkup iconLegacy;
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

    // ---- markup (what the four templates render, written out) ----

    private static String mark(String kind, String name) {
        return " data-parity=\"" + kind + "\" data-parity-case=\"" + name + "\"";
    }

    private static String raw(String tag, String text, String name, String style, String extra) {
        return "<" + tag + extra + mark("raw", name) + " style=\"margin:0;" + style + "\">" + text + "</" + tag + ">";
    }

    /** One Nav Link: a link, or, for the current page, a span. */
    private static String navLink(String label, boolean current, String name) {
        if (current) {
            return "<li class=\"ash-nav-link__item\"" + mark("navbar-li", name) + "><span aria-current=\"page\" data-nav-link class=\"ash-nav-link ash-nav-link--current\"" + mark("nav-link-current", name) + ">" + label + "</span></li>";
        }
        return "<li class=\"ash-nav-link__item\"" + mark("navbar-li", name) + "><a href=\"#\" data-nav-link class=\"ash-nav-link\"" + mark("nav-link", name) + ">" + label + "</a></li>";
    }

    private static String items(String name) {
        return navLink("Components", true, name + "-1") + navLink("Guides", false, name + "-2") + navLink("GitHub", false, name + "-3");
    }

    /** A Navbar, as navbar.jte writes it. {@code header} is the page's masthead; a null {@code actions} leaves that part out. */
    private static String navbar(String name, boolean actions) {
        StringBuilder sb = new StringBuilder("<header data-navbar class=\"ash-navbar\"" + mark("navbar", name) + ">");
        sb.append("<div class=\"ash-navbar__inner\"").append(mark("navbar-inner", name)).append(">");
        sb.append(raw("a", "Ashlar", name + "-brand", "", " href=\"#\""));
        sb.append("<div class=\"ash-navbar__end\"").append(mark("navbar-end", name)).append(">");
        sb.append("<nav aria-label=\"Primary\" class=\"ash-navbar__nav\"").append(mark("navbar-nav", name)).append(">");
        sb.append("<ul class=\"ash-navbar__list\"").append(mark("navbar-list", name)).append(">").append(items(name)).append("</ul></nav>");
        if (actions) {
            sb.append("<div class=\"ash-navbar__actions\"").append(mark("navbar-actions", name)).append(">")
                    .append(raw("span", "Theme", name + "-action", "display:inline-block;width:70px;height:28px", "")).append("</div>");
        }
        sb.append("<div class=\"ash-navbar__menu\"").append(mark("navbar-menu", name)).append(">")
                .append(raw("button", "Menu", name + "-toggle", "display:inline-block;width:40px;height:40px", " data-navbar-toggle")).append("</div>");
        return sb.append("</div></div></header>").toString();
    }

    /** The mobile menu's list, as it is inside an open Drawer: the same items again, in a column. */
    private static String drawerBody(String name) {
        return "<nav aria-label=\"Primary\"><ul class=\"ash-navbar__menu-list\"" + mark("navbar-menu-list", name) + ">" + items(name) + "</ul></nav>";
    }

    private static String svg(String name) {
        return "<svg class=\"ash-icon ash-icon--sm\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" aria-hidden=\"true\"" + mark("sidebar-item-svg", name)
                + "><path d=\"M5 12h14\"/></svg>";
    }

    private static String sidebarItem(String label, boolean current, boolean icon, String name) {
        String classes = current ? "ash-sidebar-item ash-sidebar-item--current" : "ash-sidebar-item";
        String open = current ? "<span aria-current=\"page\" class=\"" + classes + "\"" + mark("sidebar-item-current", name) + ">"
                : "<a href=\"#\" class=\"" + classes + "\"" + mark("sidebar-item", name) + ">";
        String close = current ? "</span>" : "</a>";
        String iconHtml = icon ? "<span class=\"ash-sidebar-item__icon\" aria-hidden=\"true\"" + mark("sidebar-item-icon", name) + ">" + svg(name) + "</span>" : "";
        return "<li class=\"ash-sidebar-item__entry\"" + mark("sidebar-li", name) + ">" + open + iconHtml + "<span class=\"ash-sidebar-item__label\"" + mark("sidebar-item-label", name) + ">" + label + "</span>" + close + "</li>";
    }

    private static String sidebar(String name, String longLabel) {
        return "<nav aria-label=\"Sidebar\" data-sidebar class=\"ash-sidebar\"" + mark("sidebar", name) + "><ul class=\"ash-sidebar__list\"" + mark("sidebar-list", name) + ">"
                + sidebarItem("Overview", true, true, name + "-1") + sidebarItem("Reports", false, true, name + "-2") + sidebarItem("Settings", false, false, name + "-3")
                + sidebarItem("Current without icon", true, false, name + "-4") + sidebarItem(longLabel, false, true, name + "-5") + "</ul></nav>";
    }

    private static String markup(boolean legacyMarkup) {
        String longLabel = "A rather long sidebar destination that cannot possibly fit in the column";
        // The first Navbar is a direct child of body, the shape the skins' masthead rules select; the others sit in wrappers, as a Navbar inside a page may.
        String html = navbar("masthead", true)
                + "<div data-layout=\"wrapped\" style=\"margin:8px 0\">" + navbar("wrapped", false) + "</div>"
                + "<div data-layout=\"drawer\" style=\"box-sizing:border-box;width:280px;margin:8px 0\">" + drawerBody("drawer") + "</div>"
                + "<div data-layout=\"column\" style=\"box-sizing:border-box;width:240px;margin:8px 0\">" + sidebar("column", longLabel) + "</div>"
                + "<div data-layout=\"narrow\" style=\"box-sizing:border-box;width:140px;margin:8px 0\">" + sidebar("narrow", longLabel) + "</div>";
        return legacyMarkup ? iconLegacy.rewrite(navigationLegacy.rewrite(html)) : html;
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

    // ---- control experiments: each breaks the migrated navigation in ONE way and must be detected ----

    private record Control(String name, String config, int width, String css, String expectedProperty, String withoutHook) {
        Control(String name, String config, int width, String css, String expectedProperty) {
            this(name, config, width, css, expectedProperty, "");
        }
    }

    private static final List<Control> CONTROL_LIST = List.of(
            // Nav Link
            new Control("nav link padding is wrong", "light/none", 900, ".ash-nav-link{padding-inline:8px}", "padding-left"),
            new Control("nav link radius is wrong", "light/none", 900, ".ash-nav-link{border-radius:2px}", "border-top-left-radius"),
            new Control("nav link radius ignores a skin's --radius-md", "light/foundry", 900, ".ash-nav-link{border-radius:0.375rem}", "border-top-left-radius"),
            new Control("nav link weight is wrong", "light/none", 900, ".ash-nav-link{font-weight:600}", "font-weight"),
            new Control("nav link text colour is wrong", "light/none", 900, ".ash-nav-link{--ash-nav-link-fg:red}", "color"),
            new Control("nav link dark text ignores the theme", "dark/none", 900, ".ash-nav-link{--ash-nav-link-fg:var(--color-slate-600)}", "color"),
            new Control("the current page background is wrong", "light/none", 900, ".ash-nav-link--current{background-color:red}", "background-color"),
            new Control("the current page dark background ignores the theme", "dark/none", 900, ".ash-nav-link{--ash-nav-link-current-bg:var(--color-blue-50)}", "background-color"),
            new Control("the current page is not semibold", "light/none", 900, ".ash-nav-link--current{font-weight:500}", "font-weight"),
            new Control("a nav link does not fill its row in the mobile menu", "light/none", 500, ".ash-nav-link{width:auto}", "width"),
            new Control("a link keeps its decoration", "light/none", 900, ".ash-nav-link{text-decoration:underline}", "text-decoration-line"),
            // Navbar
            new Control("the desktop links have a fixed width", "light/none", 900, "@media (min-width:48rem){.ash-navbar__list > li > a{width:200px}}", "width"),
            new Control("the bar background is wrong", "light/none", 900, ".ash-navbar{--ash-navbar-bg:red}", "background-color"),
            new Control("the bar rule dark colour ignores the theme", "dark/none", 900, ".ash-navbar{--ash-navbar-border:var(--color-slate-200)}", "border-bottom-color"),
            new Control("the bar rule is two pixels", "light/none", 900, ".ash-navbar{border-bottom-width:2px}", "border-bottom-width"),
            new Control("the inner row is narrower", "light/none", 1300, ".ash-navbar__inner{max-width:60rem}", "max-width"),
            new Control("the inner row gap is wrong", "light/none", 900, ".ash-navbar__inner{gap:8px}", "column-gap"),
            new Control("the desktop nav is hidden on desktop", "light/none", 900, "@media (min-width:48rem){.ash-navbar__nav{display:none}}", "display"),
            new Control("the mobile trigger is shown on desktop", "light/none", 900, "@media (min-width:48rem){.ash-navbar__menu{display:block}}", "display"),
            new Control("the breakpoint is 40rem", "light/none", 767, "@media (min-width:40rem){.ash-navbar__nav{display:block}.ash-navbar__menu{display:none}}", "display"),
            new Control("the desktop nav is shown on a phone", "light/none", 320, ".ash-navbar__nav{display:block}", "display"),
            new Control("the list text is larger", "light/none", 900, ".ash-navbar__list{font-size:1rem}", "font-size"),
            new Control("the list gap is wrong", "light/none", 900, ".ash-navbar__list{gap:0}", "column-gap"),
            new Control("the list keeps its bullets", "light/none", 900, ".ash-navbar__list{list-style:disc}", "list-style-type"),
            new Control("the actions gap is wrong", "light/none", 900, ".ash-navbar__actions{gap:4px}", "column-gap"),
            new Control("the mobile menu is a row", "light/none", 500, ".ash-navbar__menu-list{flex-direction:row}", "flex-direction"),
            // Sidebar
            new Control("the sidebar gap is wrong", "light/none", 900, ".ash-sidebar__list{gap:0}", "row-gap"),
            new Control("the sidebar keeps its list style", "light/none", 900, ".ash-sidebar__list{list-style:disc}", "list-style-type"),
            new Control("the sidebar item gap is wrong", "light/none", 900, ".ash-sidebar-item{gap:4px}", "column-gap"),
            new Control("the sidebar item padding is wrong", "light/none", 900, ".ash-sidebar-item{padding-block:4px}", "padding-top"),
            new Control("a long label no longer clips", "light/none", 900, ".ash-sidebar-item__label{text-overflow:clip}", "text-overflow"),
            new Control("a long label no longer shrinks", "light/none", 900, ".ash-sidebar-item__label{min-width:auto}", "min-width"),
            new Control("the icon may shrink", "light/none", 900, ".ash-sidebar-item__icon{flex-shrink:1}", "flex-shrink"),
            new Control("the sidebar current page colour is wrong", "light/none", 900, ".ash-sidebar-item--current{color:red}", "color"),
            new Control("the sidebar link dark colour ignores the theme", "dark/none", 900, ".ash-sidebar-item{--ash-sidebar-item-fg:var(--color-slate-600)}", "color"),
            // The hooks: without them the skins stop reaching the navigation.
            new Control("Civic's masthead needs data-nav-link", "light/civic", 900, "", "color", " data-nav-link"));

    // ---- running ----

    @BeforeAll
    static void measure() throws Exception {
        ParitySession session = ParitySession.open("navigation-parity");
        navigationLegacy = new NavigationLegacyMarkup();
        iconLegacy = new IconLegacyMarkup();
        String environment = ParityFrames.environment(session.generatedDir());
        String migrated = markup(false);
        String legacyMarkup = markup(true);
        migratedForCounting = migrated;

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
            String body = control.withoutHook().isEmpty() ? migrated : migrated.replace(control.withoutHook(), " data-x");
            frames.append(sized(ParityFrames.frame("ctl:" + control.name() + "|P", c, ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), control.css(), body), control.width()));
        }
        probe = ParityFrames.measure(session, frames.toString(), EXTRA_PROPS, "navigation frames");
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

    /** The rows of one kind, one case, in one layout, in document order. */
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
    void theConfigurationsAreTheTwentyTwoThemeAndSkinCombinationsAndTheBreakpointWidthsAreMeasured() {
        assertEquals(22, CONFIGS.size());
        assertEquals(4, EDGE_CONFIGS.size());
        assertEquals(22 * WIDTHS.size() + 4 * EDGE_WIDTHS.size(), PRESENT.size());
        assertTrue(EDGE_WIDTHS.containsAll(List.of(767, 768, 769)), "below, at and above 48rem (768px)");
    }

    @Test
    void controlledOracleWithTailwindPresentMatchesTheLegacyNavigationInEveryConfigurationWidthAndState() {
        assertEquals(List.of(), lines(PRESENT));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheLegacyNavigationGivenTheSameSurroundings() {
        assertEquals(List.of(), lines(REMOVED_SCOPED));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheFullLegacyRendering() {
        assertEquals(List.of(), lines(REMOVED_FULL));
    }

    @Test
    void theScopedLegacyStylesheetRendersTheLegacyNavigationExactlyAsTheFullOneDoes() {
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
    void theNavbarSwitchesBetweenItsMobileAndDesktopFormsAtExactly48remInBothRenderings() {
        for (String render : List.of("L", "P", "R")) {
            for (int width : List.of(320, 767)) {
                String at = "light/none|" + render + "|" + width;
                assertEquals("none", one(at, "navbar-nav", "masthead", "page", "display"), at + ": the desktop links are hidden below 48rem");
                assertEquals("block", one(at, "navbar-menu", "masthead", "page", "display"), at + ": the mobile trigger is shown");
            }
            for (int width : List.of(768, 769, 1300)) {
                String at = "light/none|" + render + "|" + width;
                assertEquals("block", one(at, "navbar-nav", "masthead", "page", "display"), at + ": the desktop links are shown from 48rem");
                assertEquals("none", one(at, "navbar-menu", "masthead", "page", "display"), at + ": the mobile trigger is hidden");
            }
            for (int width : List.of(320, 767, 768, 1300)) {
                assertEquals("flex", one("light/none|" + render + "|" + width, "navbar-actions", "masthead", "page", "display"), "the actions stay visible at every width: " + width);
            }
        }
    }

    @Test
    void theSameItemsRenderTwiceADesktopLinkTakesItsContentWidthAndAMobileLinkFillsItsRow() {
        for (String render : List.of("L", "P", "R", "S")) {
            String desktop = "light/none|" + render + "|900";
            // Desktop: the link is set back to content width; the current page, a span, keeps its full width, which in a shrink-to-fit item is its content width.
            double linkWidth = number(one(desktop, "nav-link", "masthead-2", "page", "@w"));
            double itemWidth = number(one(desktop, "navbar-li", "masthead-2", "page", "@w"));
            assertEquals(itemWidth, linkWidth, 0.01, desktop + ": a link is as wide as its list item");
            assertTrue(linkWidth < 120, desktop + ": and that is its content width: " + linkWidth);
            assertEquals(number(one(desktop, "navbar-li", "masthead-1", "page", "@w")), number(one(desktop, "nav-link-current", "masthead-1", "page", "@w")), 0.01, desktop);
            // The second copy, in the open Drawer: every row fills the 280px column.
            for (String item : List.of("drawer-1", "drawer-2", "drawer-3")) {
                double listWidth = number(one(desktop, "navbar-menu-list", "drawer", "drawer", "@w"));
                String kind = item.equals("drawer-1") ? "nav-link-current" : "nav-link";
                assertEquals(listWidth, number(one(desktop, kind, item, "drawer", "@w")), 0.01, desktop + " " + item + ": the row is the tap target");
            }
            assertEquals("column", one(desktop, "navbar-menu-list", "drawer", "drawer", "flex-direction"), desktop);
            assertEquals("row", one(desktop, "navbar-list", "masthead", "page", "flex-direction"), desktop);
        }
    }

    @Test
    void aNavLinkHasTailwindsSizeSpacingTypeAndTheCurrentPageIsBlueAndSemibold() {
        for (String phase : RENDERINGS) {
            String at = phase + "900";
            assertEquals("flex", one(at, "nav-link", "masthead-2", "page", "display"), at);
            assertEquals("6px", one(at, "nav-link", "masthead-2", "page", "border-top-left-radius"), at);
            assertEquals("12px", one(at, "nav-link", "masthead-2", "page", "padding-left"), at);
            assertEquals("8px", one(at, "nav-link", "masthead-2", "page", "padding-top"), at);
            assertEquals("14px", one(at, "nav-link", "masthead-2", "page", "font-size"), at);
            assertEquals("20px", one(at, "nav-link", "masthead-2", "page", "line-height"), at);
            assertEquals("500", one(at, "nav-link", "masthead-2", "page", "font-weight"), at);
            assertEquals("none", one(at, "nav-link", "masthead-2", "page", "text-decoration-line"), at + ": preflight removed the link decoration");
            assertEquals("600", one(at, "nav-link-current", "masthead-1", "page", "font-weight"), at);
            assertNotEquals("rgba(0, 0, 0, 0)", one(at, "nav-link-current", "masthead-1", "page", "background-color"), at + ": the current page has a fill");
            assertEquals("rgba(0, 0, 0, 0)", one(at, "nav-link", "masthead-2", "page", "background-color"), at + ": a link does not");
            assertEquals("none", one(at, "navbar-list", "masthead", "page", "list-style-type"), at + ": preflight removed the list style");
            assertEquals("0px", one(at, "navbar-list", "masthead", "page", "padding-left"), at);
            assertEquals("4px", one(at, "navbar-list", "masthead", "page", "column-gap"), at);
            assertEquals("1px", one(at, "navbar", "masthead", "page", "border-bottom-width"), at);
            assertEquals("0px", one(at, "navbar", "masthead", "page", "border-top-width"), at);
            assertEquals("16px", one(at, "navbar-inner", "masthead", "page", "padding-left"), at);
            assertEquals("space-between", one(at, "navbar-inner", "masthead", "page", "justify-content"), at);
        }
    }

    @Test
    void theInnerRowIsCentredAndCappedAt72remAsTailwindsMaxW6xlWas() {
        for (String phase : List.of("light/none|L|", "light/none|P|", "light/none|R|")) {
            String at = phase + "1300";
            assertEquals("1152", one(at, "navbar-inner", "masthead", "page", "@w"), at + ": 72rem");
            assertNotEquals("0px", one(at, "navbar-inner", "masthead", "page", "margin-left"), at + ": centred with auto margins");
        }
    }

    @Test
    void aSidebarIsAColumnOfItemsWhoseLongLabelClipsInsteadOfWideningTheColumn() {
        for (String phase : RENDERINGS) {
            String at = phase + "900";
            assertEquals("column", one(at, "sidebar-list", "column", "column", "flex-direction"), at);
            assertEquals("4px", one(at, "sidebar-list", "column", "column", "row-gap"), at);
            assertEquals("none", one(at, "sidebar-list", "column", "column", "list-style-type"), at);
            assertEquals("240", one(at, "sidebar-item", "column-5", "column", "@w"), at + ": the item keeps the column's width");
            assertEquals("140", one(at, "sidebar-item", "narrow-5", "narrow", "@w"), at + ": and a narrower column's");
            assertEquals("ellipsis", one(at, "sidebar-item-label", "narrow-5", "narrow", "text-overflow"), at);
            assertEquals("hidden", one(at, "sidebar-item-label", "narrow-5", "narrow", "overflow-x"), at);
            assertEquals("nowrap", one(at, "sidebar-item-label", "narrow-5", "narrow", "white-space"), at);
            assertEquals("0", one(at, "sidebar-item-icon", "narrow-5", "narrow", "flex-shrink"), at + ": the icon never shrinks");
            assertEquals("16", one(at, "sidebar-item-svg", "column-1", "column", "@w"), at + ": the icon is 1rem");
            assertEquals("12px", one(at, "sidebar-item", "column-2", "column", "column-gap"), at);
            assertEquals("600", one(at, "sidebar-item-current", "column-1", "column", "font-weight"), at);
            assertEquals("500", one(at, "sidebar-item", "column-2", "column", "font-weight"), at);
        }
    }

    @Test
    void theHooksLetTheSkinsReachTheMastheadInBothRenderings() {
        for (String render : List.of("L", "P", "R")) {
            // Civic recolours a nav link in its masthead through data-nav-link, and its bar through `body > header`; the default does neither.
            String plain = "light/none|" + render + "|900";
            String civic = "light/civic|" + render + "|900";
            assertNotEquals(one(plain, "nav-link", "masthead-2", "page", "color"), one(civic, "nav-link", "masthead-2", "page", "color"), civic + ": data-nav-link is what Civic selects");
            assertNotEquals(one(plain, "navbar", "masthead", "page", "background-color"), one(civic, "navbar", "masthead", "page", "background-color"), civic + ": `body > header`");
            // A skin that sets --radius-md rounds the links.
            assertNotEquals(one(plain, "nav-link", "masthead-2", "page", "border-top-left-radius"), one("light/foundry|" + render + "|900", "nav-link", "masthead-2", "page", "border-top-left-radius"), render);
        }
    }

    @Test
    void darkModeUsesTailwindsDarkColoursInBothRenderings() {
        for (String render : List.of("L", "P", "R")) {
            String light = "light/none|" + render + "|900";
            String dark = "dark/none|" + render + "|900";
            for (String[] part : new String[][] {{"nav-link", "masthead-2", "page", "color"}, {"nav-link-current", "masthead-1", "page", "color"},
                    {"nav-link-current", "masthead-1", "page", "background-color"}, {"navbar", "masthead", "page", "background-color"}, {"navbar", "masthead", "page", "border-bottom-color"},
                    {"sidebar-item", "column-2", "column", "color"}, {"sidebar-item-current", "column-1", "column", "color"}, {"sidebar-item-current", "column-1", "column", "background-color"}}) {
                assertNotEquals(one(light, part[0], part[1], part[2], part[3]), one(dark, part[0], part[1], part[2], part[3]), render + " " + part[0] + " " + part[3]);
            }
        }
    }

    @Test
    void theCasesAreCompleteAndEveryPartIsMeasured() {
        List<ParityProbe.Row> rows = probe.phase("light/none|L|900");
        Map<String, Integer> byKind = new TreeMap<>();
        rows.forEach(r -> byKind.merge(r.kind(), 1, Integer::sum));
        for (String kind : List.of("navbar", "navbar-inner", "navbar-end", "navbar-nav", "navbar-list", "navbar-li", "nav-link", "nav-link-current", "navbar-actions", "navbar-menu",
                "navbar-menu-list", "sidebar", "sidebar-list", "sidebar-li", "sidebar-item", "sidebar-item-current", "sidebar-item-icon", "sidebar-item-svg", "sidebar-item-label")) {
            int written = count(migratedForCounting, "data-parity=\"" + kind + "\"");
            assertTrue(written > 0, kind);
            assertEquals(written, byKind.get(kind), kind + " rows");
        }
        assertEquals(2, byKind.get("navbar"));
        assertEquals(2, byKind.get("navbar-list"), "the desktop list of each navbar");
        assertEquals(1, byKind.get("navbar-menu-list"), "the drawer's copy");
        for (String key : ROWS.keySet().stream().filter(k -> k.startsWith("light/none|L|")).toList()) {
            assertEquals(ROWS.get("light/none|L|900"), ROWS.get(key), "every width measures the same elements: " + key);
        }
    }

    @Test
    void theOracleIsNotVacuousEveryLegacyClassIsCompiledInTheCumulativeAndScopedStylesheets() throws IOException {
        Path dir = Path.of(System.getProperty("ashlar.cascade.dir"));
        String oracle = Files.readString(dir.resolve("navigation-oracle.css"), StandardCharsets.UTF_8);
        String scoped = Files.readString(dir.resolve("navigation-legacy-scoped.css"), StandardCharsets.UTF_8);
        List<String> tokens = navigationLegacy.allTokens().stream().filter(t -> !t.contains("[")).collect(Collectors.toList());
        assertEquals(List.of(), ParityOracle.uncompiled(oracle, tokens), "the oracle does not compile these");
        assertEquals(List.of(), ParityOracle.uncompiled(scoped, tokens), "the scoped stylesheet does not compile these");
        // The arbitrary-variant utility has a bracketed name Tailwind writes with escapes; look for its rule by what it does.
        assertTrue(scoped.contains("md\\:\\[\\&\\>li\\>a\\]\\:w-auto") || scoped.contains("md\\:\\[\\&\\>li\\>a\\]\\:w-auto".replace("\\&", "\\&")), "the child-selector utility is compiled");
        List<String> earlier = new ArrayList<>();
        earlier.addAll(new ButtonLegacyMarkup().allTokens().stream().filter(t -> !t.startsWith("aria-disabled:")).toList());
        earlier.addAll(new CardLegacyMarkup().allTokens());
        earlier.addAll(new PageCompositionLegacyMarkup().allTokens());
        earlier.addAll(new IconLegacyMarkup().allTokens());
        assertEquals(List.of(), ParityOracle.uncompiled(oracle, earlier), "the oracle lost earlier legacy classes");
        assertFalse(scoped.contains("html,:host"), "the scoped legacy stylesheet must not carry Tailwind's page-wide base");
    }

    @Test
    void normalizationsStayWithinTheApprovedRulesAndTailwindPresentNeedsNone() {
        assertEquals(Map.of(), APPLIED_PRESENT.entrySet().stream().filter(e -> !Set.of("H1").contains(e.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                "Tailwind present needs at most the transparent-shadow-layer rule (the focus ring is not at rest, so usually none)");
        assertEquals(Map.of(), APPLIED_SCOPED, "the scoped legacy rendering must match the full one without any normalization");
        assertTrue(Set.of("H2").containsAll(APPLIED_REMOVED.keySet()), "unexpected normalization rule used: " + APPLIED_REMOVED);
    }

    // ---- what the real pages serve ----

    private static final Pattern NAVBAR_STRUCTURE = Pattern.compile("<header data-navbar class=\"ash-navbar\">\\s*<div class=\"ash-navbar__inner\">\\s*<a ");
    private static final Pattern TAILWIND_ON_A_HOOK = Pattern.compile("class=\"[^\"]*(?:rounded-md|px-3|text-slate-600|bg-blue-50|flex-col|max-w-6xl|md:hidden)[^\"]*\"\\s*(?:data-(?:nav-link|navbar|sidebar)|aria-current)");

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }

    @Test
    void everyNavigationPartTheRealPagesServeKeepsItsNativeStructureHooksAndTheMastheadShape() throws Exception {
        ParitySession session = ParitySession.open("navigation-parity");
        HttpClient client = HttpClient.newHttpClient();
        List<String> paths = new ArrayList<>(List.of("/ashlar/", "/ashlar/components"));
        for (ComponentPage page : ComponentCatalog.PAGES) {
            paths.add("/ashlar/components/" + page.slug());
        }
        Map<String, Integer> totals = new TreeMap<>();
        List<String> problems = new ArrayList<>();
        try (RunningShowcase app = RunningShowcase.start(session.bootJar())) {
            for (String path : paths) {
                HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + path)).GET().build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                assertEquals(200, response.statusCode(), path);
                String body = response.body();
                for (String role : NavigationLegacyMarkup.MIGRATED.keySet()) {
                    totals.merge(role, NavigationLegacyMarkup.count(body, role), Integer::sum);
                }
                // The rewrite throws on any class value that carries a Navigation class but is not a known part.
                navigationLegacy.rewrite(body);
                int navbars = NavigationLegacyMarkup.count(body, "navbar");
                if (count(body, "<header data-navbar") != navbars || NAVBAR_STRUCTURE.matcher(body).results().count() != navbars) {
                    problems.add(path + ": a Navbar is not <header data-navbar> > <div> > <a> (the brand first), the shape the skins key on");
                }
                if (NavigationLegacyMarkup.count(body, "navbar-list") != navbars || NavigationLegacyMarkup.count(body, "navbar-menu-list") != navbars) {
                    problems.add(path + ": a Navbar must render its items twice, in the bar and in the menu");
                }
                if (count(body, "data-nav-link") != NavigationLegacyMarkup.count(body, "nav-link") + NavigationLegacyMarkup.count(body, "nav-link-current")) {
                    problems.add(path + ": data-nav-link must be on every Nav Link, and only on them");
                }
                if (count(body, "data-sidebar") != NavigationLegacyMarkup.count(body, "sidebar-list")) {
                    problems.add(path + ": a Sidebar's nav carries data-sidebar and its list carries the class");
                }
                if (TAILWIND_ON_A_HOOK.matcher(body).find()) {
                    problems.add(path + ": a navigation part still renders its Tailwind classes");
                }
            }
        }
        assertEquals(List.of(), problems);
        for (String role : List.of("navbar", "navbar-inner", "navbar-end", "navbar-nav", "navbar-list", "navbar-menu", "navbar-menu-list", "nav-link", "nav-link-current", "sidebar-list",
                "sidebar-item", "sidebar-item-current", "sidebar-item-icon", "sidebar-item-label")) {
            assertTrue(totals.getOrDefault(role, 0) > 0, "no " + role + " rendered on any page: " + totals);
        }
        assertEquals(totals.get("navbar-list"), totals.get("navbar-menu-list"));
        assertTrue(totals.get("navbar") > 50, "every page has the masthead: " + totals);
    }
}
