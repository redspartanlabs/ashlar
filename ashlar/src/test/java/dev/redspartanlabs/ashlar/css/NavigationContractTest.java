package dev.redspartanlabs.ashlar.css;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the source contract of Nav Link, Navbar, Sidebar and Sidebar Item, migrated together. Each template keeps its parameters, its native elements,
 * its optional parts, its order, its {@code aria-current} handling and the data hooks the skins select (and the structure they key on), and carries only
 * Ashlar's classes; each stylesheet is scoped to its own classes and states the colours, sizes, states and the one breakpoint the Tailwind classes gave.
 * The rendered result is measured in a real browser by the showcase's {@code NavigationParityTest} and {@code NavigationStateParityTest}; this reads the sources only.
 */
class NavigationContractTest {

    private static final List<String> NAMES = List.of("nav-link", "navbar", "sidebar", "sidebar-item");
    private static final String DARK = ":where([data-theme=\"dark\"], [data-theme=\"dark\"] *)";
    private static final String NOT_CURRENT_NAV = ":where(:not(.ash-nav-link--current))";
    private static final String NOT_CURRENT_SIDEBAR = ":where(:not(.ash-sidebar-item--current))";

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

    private static String template(String name) throws IOException {
        return Files.readString(module.resolve("src/main/jte/ashlar/components/" + name + ".jte"), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static String css(String name) throws IOException {
        return Files.readString(module.resolve("src/main/css/components/" + name + ".css"), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static List<CssScanner.Rule> rules(String name) throws IOException {
        return CssScanner.flatRules(css(name).replaceAll("(?s)/\\*.*?\\*/", ""));
    }

    private static String withoutJteComments(String source) {
        return source.replaceAll("(?s)<%--.*?--%>", "");
    }

    /** The template's markup alone: no comments, no parameter or local-variable lines, whitespace collapsed. */
    private static String markup(String name) throws IOException {
        return withoutJteComments(template(name)).replaceAll("(?m)^@(?:param|import) .*$", "").replaceAll("(?m)^!\\{.*\\}$", "").replaceAll("\\s+", " ").trim();
    }

    private static List<String> declared(String name) throws IOException {
        List<String> declared = new ArrayList<>();
        Matcher m = Pattern.compile("(?m)^@(?:param|import) .*$").matcher(template(name));
        while (m.find()) {
            declared.add(m.group());
        }
        return declared;
    }

    private static Map<String, String> declarations(String cssName, String selector) throws IOException {
        return declarations(cssName, selector, "");
    }

    private static Map<String, String> declarations(String cssName, String selector, String media) throws IOException {
        List<CssScanner.Rule> found = rules(cssName).stream().filter(r -> r.selector().equals(selector) && r.media().equals(media)).toList();
        assertEquals(1, found.size(), selector + " must be defined exactly once in " + cssName + (media.isEmpty() ? "" : " inside " + media) + "; rules: "
                + rules(cssName).stream().map(r -> r.media() + "|" + r.selector()).toList());
        return CssScanner.declarations(found.get(0).block());
    }

    // ---- the templates ----

    @Test
    void theParametersAndTheirDefaultsAreUnchanged() throws IOException {
        assertEquals(List.of("@param String label", "@param String href = null", "@param boolean current = false"), declared("nav-link"));
        assertEquals(List.of("@import dev.redspartanlabs.ashlar.navbar.NavbarIds", "@import dev.redspartanlabs.ashlar.icon.Icon", "@param gg.jte.Content brand",
                "@param gg.jte.Content items", "@param gg.jte.Content actions = null", "@param String ariaLabel = \"Primary\"", "@param String menuTitle = \"Menu\""), declared("navbar"));
        assertEquals(List.of("@param gg.jte.Content items", "@param String ariaLabel = \"Sidebar\""), declared("sidebar"));
        assertEquals(List.of("@import dev.redspartanlabs.ashlar.icon.Icon", "@param String label", "@param String href = null", "@param boolean current = false", "@param Icon icon = null"),
                declared("sidebar-item"));
    }

    @Test
    void eachTemplateKeepsItsNativeElementsItsHooksItsCurrentHandlingAndItsOrderExactly() throws IOException {
        assertEquals("<li class=\"ash-nav-link__item\"> @if (isLink) <a href=\"${href}\" data-nav-link class=\"${linkClasses}\">${label}</a> @else"
                + " <span aria-current=\"${ariaCurrentAttr}\" data-nav-link class=\"${currentClasses}\">${label}</span> @endif </li>", markup("nav-link"));
        assertEquals("<nav aria-label=\"${ariaLabel}\" data-sidebar class=\"ash-sidebar\"> <ul class=\"ash-sidebar__list\"> ${items} </ul> </nav>", markup("sidebar"));
        assertEquals("<li class=\"ash-sidebar-item__entry\"> @if (isLink) <a href=\"${href}\" class=\"${linkClasses}\">"
                + " @if (hasIcon) <span class=\"ash-sidebar-item__icon\" aria-hidden=\"true\"> @template.ashlar.components.icon(icon = icon, classes = \"ash-icon--sm\") </span> @endif"
                + " <span class=\"ash-sidebar-item__label\">${label}</span> </a> @else <span aria-current=\"${ariaCurrentAttr}\" class=\"${currentClasses}\">"
                + " @if (hasIcon) <span class=\"ash-sidebar-item__icon\" aria-hidden=\"true\"> @template.ashlar.components.icon(icon = icon, classes = \"ash-icon--sm\") </span> @endif"
                + " <span class=\"ash-sidebar-item__label\">${label}</span> </span> @endif </li>", markup("sidebar-item"));
        // The Navbar: the masthead shape the skins key on (header > div > the brand first), the items written twice, the actions optional, the Drawer borrowed.
        String navbar = markup("navbar");
        assertTrue(navbar.startsWith("<header data-navbar class=\"ash-navbar\"> <div class=\"ash-navbar__inner\"> ${brand} <div class=\"ash-navbar__end\">"
                + " <nav aria-label=\"${ariaLabel}\" class=\"ash-navbar__nav\"> <ul class=\"ash-navbar__list\"> ${items} </ul> </nav>"
                + " @if (actions != null) <div class=\"ash-navbar__actions\"> ${actions} </div> @endif <div class=\"ash-navbar__menu\">"), navbar);
        assertTrue(navbar.contains("@template.ashlar.components.button( iconOnly = true, icon = Icon.MENU, ariaLabel = \"Open menu\", type = \"Ghost\","
                + " attributes = \"data-drawer-open=\\\"\" + drawerId + \"\\\" data-navbar-toggle\" ) </div> </div> </div> </header>"), navbar);
        assertTrue(navbar.contains("@template.ashlar.components.drawer( id = drawerId, title = menuTitle, placement = \"Left\", body = @` <nav aria-label=\"${ariaLabel}\">"
                + " <ul class=\"ash-navbar__menu-list\"> ${items} </ul> </nav> ` )"), navbar);
        assertEquals(2, count(navbar, "${items}"), "the same items are rendered twice");
        assertEquals(1, count(navbar, "${brand}"));
        assertEquals(1, count(navbar, "${actions}"), "the actions are not duplicated");
    }

    @Test
    void theLinkAndCurrentClassStringsAreAshlarsAndTheCurrentPageIsNeverALink() throws IOException {
        assertTrue(template("nav-link").contains("!{String linkClasses = \"ash-nav-link\";}"));
        assertTrue(template("nav-link").contains("!{String currentClasses = \"ash-nav-link ash-nav-link--current\";}"));
        assertTrue(template("sidebar-item").contains("!{String linkClasses = \"ash-sidebar-item\";}"));
        assertTrue(template("sidebar-item").contains("!{String currentClasses = \"ash-sidebar-item ash-sidebar-item--current\";}"));
        for (String name : List.of("nav-link", "sidebar-item")) {
            String source = template(name);
            assertTrue(source.contains("!{boolean isLink = !isCurrent && href != null && !href.isEmpty();}"), name + ": current wins over href");
            assertTrue(source.contains("!{String ariaCurrentAttr = isCurrent ? \"page\" : null;}"), name);
        }
    }

    @Test
    void noTailwindUtilityRemainsInAnyTemplate() throws IOException {
        List<String> utilities = List.of("flex", "w-full", "items-center", "rounded", "px-", "py-", "text-", "font-", "bg-", "dark:", "hover:", "focus", "ring", "border", "gap-", "hidden", "md:",
                "max-w", "mx-auto", "justify-", "shrink", "truncate", "min-w", "slate-", "blue-");
        for (String name : NAMES) {
            String source = withoutJteComments(template(name)).replaceAll("(?m)^@(?:param|import) .*$", "");
            for (String utility : utilities) {
                assertFalse(Pattern.compile("(?<![\\w-])" + Pattern.quote(utility)).matcher(source).find(), name + ": a Tailwind utility is back: " + utility);
            }
        }
    }

    @Test
    void theDataHooksTheSkinsSelectAreAllStillThere() throws IOException {
        List<String> hooks = new ArrayList<>();
        for (String name : NAMES) {
            Matcher m = Pattern.compile("(?<=[\\s\"])(data-[a-z-]+)(?=[\\s>=])").matcher(withoutJteComments(template(name)));
            while (m.find()) {
                hooks.add(name + ":" + m.group(1));
            }
        }
        // Navbar's trigger hooks are inside the Button's attributes string, so they are checked there.
        assertEquals(List.of("nav-link:data-nav-link", "nav-link:data-nav-link", "navbar:data-navbar", "navbar:data-drawer-open", "sidebar:data-sidebar"), hooks);
        assertTrue(template("navbar").contains("\" data-navbar-toggle\""), "the trigger keeps data-navbar-toggle, which Civic's masthead selects");
    }

    // ---- the stylesheets ----

    private static void assertScoped(String cssName, List<String> selectors) throws IOException {
        assertEquals(selectors, rules(cssName).stream().map(CssScanner.Rule::selector).toList(), cssName);
        String stripped = css(cssName).replaceAll("(?s)/\\*.*?\\*/", "");
        assertFalse(stripped.contains("!important"), cssName);
        assertFalse(stripped.contains("@import") || stripped.contains("@layer") || stripped.contains("@keyframes"), cssName);
        assertFalse(stripped.contains("data-nav") || stripped.contains("data-sidebar"), cssName + ": styling is by class; the data- hooks belong to skins");
        assertFalse(Pattern.compile("(^|[,{}]\\s*)(\\*|html|body|header|nav|ul|li|a|span|div)\\s*[,{]").matcher(stripped).find(), cssName + ": no bare element selector");
    }

    @Test
    void everySelectorIsScopedToItsOwnClassesAndNothingIsGlobal() throws IOException {
        assertScoped("nav-link", List.of(".ash-nav-link", ".ash-nav-link" + DARK, ".ash-nav-link--current", ".ash-nav-link" + NOT_CURRENT_NAV + ":hover",
                ".ash-nav-link" + NOT_CURRENT_NAV + ":focus", ".ash-nav-link" + NOT_CURRENT_NAV + ":focus-visible"));
        assertScoped("sidebar-item", List.of(".ash-sidebar-item", ".ash-sidebar-item" + DARK, ".ash-sidebar-item--current", ".ash-sidebar-item" + NOT_CURRENT_SIDEBAR + ":hover",
                ".ash-sidebar-item" + NOT_CURRENT_SIDEBAR + ":focus", ".ash-sidebar-item" + NOT_CURRENT_SIDEBAR + ":focus-visible", ".ash-sidebar-item__icon", ".ash-sidebar-item__label"));
        assertScoped("sidebar", List.of(".ash-sidebar__list"));
        assertScoped("navbar", List.of(".ash-navbar", ".ash-navbar" + DARK, ".ash-navbar__inner", ".ash-navbar__end", ".ash-navbar__nav", ".ash-navbar__list", ".ash-navbar__actions",
                ".ash-navbar__menu-list", ".ash-navbar__nav", ".ash-navbar__menu", ".ash-navbar__list > li > a"));
    }

    @Test
    void aNavLinkIsTailwindsRowWithItsExactColoursRadiusAndTypeAndTheCurrentPageIsBlueAndSemibold() throws IOException {
        Map<String, String> link = declarations("nav-link", ".ash-nav-link");
        assertEquals("flex", link.get("display"));
        assertEquals("center", link.get("align-items"));
        assertEquals("100%", link.get("width"), "a link fills its row; navbar.css sets it back to content width on desktop");
        assertEquals("var(--ash-radius-md)", link.get("border-radius"), "the foundation token, which is var(--radius-md, 0.375rem)");
        assertEquals("var(--ash-space-3)", link.get("padding-inline"));
        assertEquals("var(--ash-space-2)", link.get("padding-block"));
        assertEquals("0.875rem", link.get("font-size"));
        assertEquals("var(--ash-nav-link-leading)", link.get("line-height"));
        assertEquals("calc(1.25 / 0.875)", link.get("--ash-nav-link-leading"), "the ratio is in a property so a minifier cannot fold it");
        assertEquals("var(--font-weight-medium, 500)", link.get("font-weight"));
        assertEquals("none", link.get("text-decoration"), "preflight removed the link decoration");
        assertEquals("var(--ash-nav-link-fg)", link.get("color"));
        assertEquals("var(--color-slate-600, oklch(44.6% 0.043 257.281))", link.get("--ash-nav-link-fg"));
        assertEquals("var(--color-slate-900, oklch(20.8% 0.042 265.755))", link.get("--ash-nav-link-fg-hover"));
        assertEquals("var(--color-slate-100, oklch(96.8% 0.007 247.896))", link.get("--ash-nav-link-bg-hover"));
        assertEquals("var(--color-blue-500, oklch(62.3% 0.214 259.815))", link.get("--ash-nav-link-ring"));
        assertEquals("var(--color-blue-50, oklch(97% 0.014 254.604))", link.get("--ash-nav-link-current-bg"));
        assertEquals("var(--color-blue-700, oklch(48.8% 0.243 264.376))", link.get("--ash-nav-link-current-fg"));
        assertEquals(Map.of("--ash-nav-link-fg", "var(--color-slate-300, oklch(86.9% 0.022 252.894))", "--ash-nav-link-fg-hover", "var(--color-slate-100, oklch(96.8% 0.007 247.896))",
                "--ash-nav-link-bg-hover", "var(--color-slate-800, oklch(27.9% 0.041 260.031))",
                "--ash-nav-link-current-bg", "color-mix(in oklab, var(--color-blue-950, oklch(28.2% 0.091 267.935)) 50%, transparent)",
                "--ash-nav-link-current-fg", "var(--color-blue-400, oklch(70.7% 0.165 254.624))"), declarations("nav-link", ".ash-nav-link" + DARK));
        assertEquals(Map.of("background-color", "var(--ash-nav-link-current-bg)", "font-weight", "var(--font-weight-semibold, 600)", "color", "var(--ash-nav-link-current-fg)"),
                declarations("nav-link", ".ash-nav-link--current"));
        assertEquals(Map.of("background-color", "var(--ash-nav-link-bg-hover)", "color", "var(--ash-nav-link-fg-hover)"),
                declarations("nav-link", ".ash-nav-link" + NOT_CURRENT_NAV + ":hover", "@media (hover: hover)"));
        assertEquals(Map.of("outline-style", "none"), declarations("nav-link", ".ash-nav-link" + NOT_CURRENT_NAV + ":focus"));
        assertEquals(Map.of("box-shadow", "0 0 0 1px #fff, 0 0 0 3px var(--ash-nav-link-ring)"), declarations("nav-link", ".ash-nav-link" + NOT_CURRENT_NAV + ":focus-visible"));
    }

    @Test
    void aSidebarItemIsTheSameLanguageAsABlockRowWithAnInsetFocusRingAndAClippingLabel() throws IOException {
        Map<String, String> item = declarations("sidebar-item", ".ash-sidebar-item");
        assertEquals("flex", item.get("display"));
        assertEquals("var(--ash-space-3)", item.get("gap"));
        assertEquals("var(--ash-radius-md)", item.get("border-radius"));
        assertFalse(item.containsKey("width"), "a sidebar item is a block-level flex row; it does not state a width");
        assertEquals("calc(1.25 / 0.875)", item.get("--ash-sidebar-item-leading"));
        assertEquals("var(--color-slate-600, oklch(44.6% 0.043 257.281))", item.get("--ash-sidebar-item-fg"));
        assertEquals("var(--color-blue-50, oklch(97% 0.014 254.604))", item.get("--ash-sidebar-item-current-bg"));
        assertEquals("none", item.get("text-decoration"));
        assertEquals(Map.of("box-shadow", "inset 0 0 0 2px var(--ash-sidebar-item-ring)"), declarations("sidebar-item", ".ash-sidebar-item" + NOT_CURRENT_SIDEBAR + ":focus-visible"),
                "the ring is INSET so a narrow column cannot clip it");
        assertEquals(Map.of("outline-style", "none"), declarations("sidebar-item", ".ash-sidebar-item" + NOT_CURRENT_SIDEBAR + ":focus"));
        assertEquals(Map.of("background-color", "var(--ash-sidebar-item-bg-hover)", "color", "var(--ash-sidebar-item-fg-hover)"),
                declarations("sidebar-item", ".ash-sidebar-item" + NOT_CURRENT_SIDEBAR + ":hover", "@media (hover: hover)"));
        assertEquals(Map.of("flex-shrink", "0"), declarations("sidebar-item", ".ash-sidebar-item__icon"));
        assertEquals(Map.of("min-width", "0", "flex", "1 1 0%", "overflow", "hidden", "text-overflow", "ellipsis", "white-space", "nowrap"), declarations("sidebar-item", ".ash-sidebar-item__label"));
        assertEquals(Map.of("flex-direction", "column", "display", "flex", "gap", "0.25rem", "margin", "0", "padding", "0", "list-style", "none"), declarations("sidebar", ".ash-sidebar__list"));
    }

    @Test
    void theNavbarStatesItsRuleItsRowItsListAndTheOneBreakpointAt48rem() throws IOException {
        Map<String, String> bar = declarations("navbar", ".ash-navbar");
        assertEquals("0 solid var(--ash-navbar-border)", bar.get("border"));
        assertEquals("1px", bar.get("border-bottom-width"));
        assertEquals("var(--ash-navbar-bg)", bar.get("background-color"));
        assertEquals("var(--color-white, #fff)", bar.get("--ash-navbar-bg"));
        assertEquals("var(--color-slate-200, oklch(92.9% 0.013 255.508))", bar.get("--ash-navbar-border"));
        assertEquals(Map.of("--ash-navbar-bg", "var(--color-slate-900, oklch(20.8% 0.042 265.755))", "--ash-navbar-border", "var(--color-slate-700, oklch(37.2% 0.044 257.287))"),
                declarations("navbar", ".ash-navbar" + DARK));
        assertEquals(Map.of("display", "flex", "align-items", "center", "justify-content", "space-between", "gap", "var(--ash-space-4)", "margin-inline", "auto", "max-width", "72rem",
                "padding-inline", "var(--ash-space-4)", "padding-block", "var(--ash-space-4)"), declarations("navbar", ".ash-navbar__inner"));
        assertEquals(Map.of("display", "flex", "align-items", "center", "gap", "var(--ash-space-3)"), declarations("navbar", ".ash-navbar__end"));
        assertEquals(Map.of("display", "flex", "align-items", "center", "gap", "var(--ash-space-3)"), declarations("navbar", ".ash-navbar__actions"));
        assertEquals("flex", declarations("navbar", ".ash-navbar__list").get("display"));
        assertEquals("0.25rem", declarations("navbar", ".ash-navbar__list").get("gap"));
        assertEquals("none", declarations("navbar", ".ash-navbar__list").get("list-style"));
        assertEquals("0.875rem", declarations("navbar", ".ash-navbar__list").get("font-size"));
        assertEquals("calc(1.25 / 0.875)", declarations("navbar", ".ash-navbar__list").get("--ash-navbar-leading"));
        assertEquals("column", declarations("navbar", ".ash-navbar__menu-list").get("flex-direction"));
        assertEquals(Map.of("display", "none"), declarations("navbar", ".ash-navbar__nav"));
        String breakpoint = "@media (min-width: 48rem)";
        assertEquals(Map.of("display", "block"), declarations("navbar", ".ash-navbar__nav", breakpoint));
        assertEquals(Map.of("display", "none"), declarations("navbar", ".ash-navbar__menu", breakpoint), "hidden from 48rem, shown below it");
        assertEquals(Map.of("width", "auto"), declarations("navbar", ".ash-navbar__list > li > a", breakpoint), "a desktop LINK is set back to content width");
        List<String> media = rules("navbar").stream().map(CssScanner.Rule::media).filter(m -> !m.isEmpty()).distinct().toList();
        assertEquals(List.of(breakpoint), media, "one breakpoint, 48rem, Tailwind's md");
    }

    // ---- placement ----

    @Test
    void theEntryFileImportsEachInTheComponentsLayer() throws IOException {
        String source = Files.readString(module.resolve("src/main/css/ashlar.css"), StandardCharsets.UTF_8);
        for (String name : NAMES) {
            assertTrue(source.contains("@import \"components/" + name + ".css\" layer(ash.components);"), name);
        }
    }

    @Test
    void theSharedFoundationTokensAreNotTouched() throws IOException {
        String tokens = Files.readString(module.resolve("src/main/css/tokens/tokens.css"), StandardCharsets.UTF_8);
        assertFalse(tokens.contains("--ash-nav") || tokens.contains("--ash-sidebar"), "the navigation adds nothing to the shared tokens");
        assertTrue(tokens.contains("--ash-radius-md: var(--radius-md, 0.375rem);"));
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }
}
