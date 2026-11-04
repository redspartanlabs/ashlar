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
 * Pins the source contract of Tabs and Tab Panel, migrated together with {@code tabs.js}. The templates keep every parameter, default, native element, role, id and
 * ARIA relationship, render the selected state as {@code aria-selected} and an inactive panel as the native {@code hidden} attribute, carry only Ashlar's classes and no
 * class-string attribute; the script sets attributes and never a class; the stylesheets are scoped to their own classes and state the colours, sizes and states the Tailwind
 * classes gave. The rendered result is measured in a real browser by the showcase's {@code TabsParityTest}, {@code TabsStateParityTest} and {@code TabsScriptTest}; this reads
 * the sources only.
 */
class TabsContractTest {

    private static final String DARK = ":where([data-theme=\"dark\"], [data-theme=\"dark\"] *)";
    private static final String SELECTED = "[aria-selected=\"true\"]";

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

    private static String template(String name) throws IOException {
        return read("src/main/jte/ashlar/components/" + name + ".jte");
    }

    private static String css(String name) throws IOException {
        return read("src/main/css/components/" + name + ".css");
    }

    private static List<CssScanner.Rule> rules(String name) throws IOException {
        return CssScanner.flatRules(css(name).replaceAll("(?s)/\\*.*?\\*/", ""));
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

    private static String markup(String name) throws IOException {
        return template(name).replaceAll("(?s)<%--.*?--%>", "").replaceAll("(?m)^@(?:param|import) .*$", "").replaceAll("(?m)^!\\{.*\\}$", "").replaceAll("\\s+", " ").trim();
    }

    private static List<String> declared(String name) throws IOException {
        List<String> declared = new ArrayList<>();
        Matcher m = Pattern.compile("(?m)^@(?:param|import) .*$").matcher(template(name));
        while (m.find()) {
            declared.add(m.group());
        }
        return declared;
    }

    // ---- the templates ----

    @Test
    void theParametersAndTheirDefaultsAreUnchanged() throws IOException {
        assertEquals(List.of("@import java.util.List", "@import dev.redspartanlabs.ashlar.tabs.Tab", "@import dev.redspartanlabs.ashlar.tabs.TabsSupport", "@import dev.redspartanlabs.ashlar.icon.Icon",
                "@import dev.redspartanlabs.ashlar.AshlarAssets", "@param String id", "@param List<Tab> tabs", "@param String activeTab = \"\"", "@param String ariaLabel = \"\""), declared("tabs"));
        assertEquals(List.of("@param String tabsId", "@param String tabId", "@param boolean active = false", "@param gg.jte.Content content = null"), declared("tab-panel"));
    }

    @Test
    void theTabsKeepTheirNativeElementsRolesIdsAndAriaRelationshipsExactly() throws IOException {
        assertEquals("<div id=\"${id}\" data-tabs class=\"ash-tabs\"> <div data-tabs-list role=\"tablist\" aria-label=\"${ariaLabelAttr}\" class=\"ash-tabs__list\" >"
                + " @for (Tab tab : tabs) !{boolean isActive = tab.id().equals(resolvedActiveTabId);} !{String tabButtonId = id + \"-tab-\" + tab.id();}"
                + " !{String panelId = id + \"-panel-\" + tab.id();} !{String badgeText = tab.badge();}"
                + " <button type=\"button\" id=\"${tabButtonId}\" data-tab data-tab-id=\"${tab.id()}\" role=\"tab\" aria-selected=\"${String.valueOf(isActive)}\" aria-controls=\"${panelId}\""
                + " tabindex=\"${isActive ? \"0\" : \"-1\"}\" disabled=\"${tab.disabled()}\" class=\"ash-tabs__tab\" >"
                + " @if (tab.icon() != null) @template.ashlar.components.icon(icon = tab.icon(), classes = \"ash-icon--sm\") @endif"
                + " <span class=\"ash-tabs__label\">${tab.label()}</span>"
                + " @if (badgeText != null && !badgeText.isEmpty()) <span class=\"ash-tabs__badge\">${badgeText}</span> @endif"
                + " </button> @endfor </div> </div> <script type=\"module\" src=\"${AshlarAssets.js(\"tabs.js\")}\"></script>", markup("tabs"));
        assertEquals("<div id=\"${panelId}\" data-tab-panel data-tabs-id=\"${tabsId}\" data-tab-id=\"${tabId}\" role=\"tabpanel\" aria-labelledby=\"${tabButtonId}\" tabindex=\"0\""
                + " hidden=\"${hiddenAttr}\" class=\"ash-tab-panel\" > @if (content != null) ${content} @endif </div>", markup("tab-panel"));
    }

    @Test
    void theIdsThatPairATabWithItsPanelAreDerivedTheSameWayInBothTemplates() throws IOException {
        String tabs = template("tabs");
        String panel = template("tab-panel");
        assertTrue(tabs.contains("!{String tabButtonId = id + \"-tab-\" + tab.id();}") && tabs.contains("!{String panelId = id + \"-panel-\" + tab.id();}"));
        assertTrue(panel.contains("!{String panelId = tabsId + \"-panel-\" + tabId;}") && panel.contains("!{String tabButtonId = tabsId + \"-tab-\" + tabId;}"));
    }

    @Test
    void anInactivePanelIsTheNativeHiddenAttributeAndNoClassStringAttributeIsRendered() throws IOException {
        assertTrue(template("tab-panel").contains("!{boolean hiddenAttr = !active;}"));
        String tabs = template("tabs").replaceAll("(?s)<%--.*?--%>", "");
        assertFalse(tabs.contains("data-tab-active-classes") || tabs.contains("data-tab-inactive-classes"), "the two class-string attributes are gone");
        assertFalse(tabs.contains("tabBaseClasses") || tabs.contains("activeTabClasses") || tabs.contains("inactiveTabClasses") || tabs.contains("tabClasses"), "no class-string locals remain");
        assertFalse(read("src/main/resources/META-INF/resources/ashlar/js/tabs.js").contains("tabActiveClasses"), "and nothing reads them");
    }

    @Test
    void noTailwindUtilityRemainsInEitherTemplate() throws IOException {
        List<String> utilities = List.of("flex", "inline-flex", "gap-", "border", "px-", "py-", "text-", "font-", "bg-", "dark:", "hover:", "focus", "ring", "rounded", "hidden ", "min-w", "h-4", "w-4",
                "slate-", "blue-", "disabled:", "transition");
        for (String name : List.of("tabs", "tab-panel")) {
            String source = template(name).replaceAll("(?s)<%--.*?--%>", "").replaceAll("(?m)^@(?:param|import) .*$", "").replace("hidden=\"${hiddenAttr}\"", "");
            for (String utility : utilities) {
                assertFalse(Pattern.compile("(?<![\\w-])" + Pattern.quote(utility)).matcher(source).find(), name + ": a Tailwind utility is back: " + utility);
            }
        }
    }

    // ---- the script ----

    @Test
    void theScriptSetsAttributesAndPropertiesAndNeverAClass() throws IOException {
        String js = read("src/main/resources/META-INF/resources/ashlar/js/tabs.js").replaceAll("(?m)^\\s*//.*$", "");
        assertFalse(js.contains("classList") || js.contains("className") || js.contains("\"hidden\"") && js.contains("toggle"), "no class is added, removed or replaced");
        assertTrue(js.contains("button.setAttribute(\"aria-selected\", String(isActive));"));
        assertTrue(js.contains("button.tabIndex = isActive ? 0 : -1;"));
        assertTrue(js.contains("panel.hidden = panel.dataset.tabId !== tab.dataset.tabId;"));
        // The behaviour the migration must keep: automatic activation, the arrows, Home/End, wrapping, disabled skipping, init reconcile.
        for (String key : List.of("ArrowRight", "ArrowLeft", "Home", "End")) {
            assertTrue(js.contains("case \"" + key + "\":"), key);
        }
        assertTrue(js.contains("tabButtons.filter((tab) => !tab.disabled)") && js.contains("% enabled.length") && js.contains("activateTab(enabled[nextIndex], true);"));
        assertTrue(js.contains("tabButtons.find((tab) => tab.getAttribute(\"aria-selected\") === \"true\") || tabButtons[0]"), "init reconciles to the rendered selection");
        assertTrue(js.contains("document.addEventListener(\"DOMContentLoaded\""));
    }

    // ---- the stylesheets ----

    private static void assertScoped(String cssName, List<String> selectors) throws IOException {
        assertEquals(selectors, rules(cssName).stream().map(CssScanner.Rule::selector).toList(), cssName);
        String stripped = css(cssName).replaceAll("(?s)/\\*.*?\\*/", "");
        assertFalse(stripped.contains("!important"), cssName);
        assertFalse(stripped.contains("@import") || stripped.contains("@layer") || stripped.contains("@keyframes"), cssName);
        assertFalse(stripped.contains("data-tab"), cssName + ": styling is by class and ARIA; the data- hooks belong to the script and the skins");
        assertFalse(Pattern.compile("(^|[,{}]\\s*)(\\*|html|body|button|div|span|svg)\\s*[,{]").matcher(stripped).find(), cssName + ": no bare element selector");
    }

    @Test
    void everySelectorIsScopedToItsOwnClassesAndNothingIsGlobal() throws IOException {
        String tab = ".ash-tabs__tab";
        assertScoped("tabs", List.of(".ash-tabs", ".ash-tabs" + DARK, ".ash-tabs__list", tab, tab + DARK, tab + SELECTED, tab + SELECTED + DARK, tab + ":disabled", tab + ":disabled" + DARK,
                tab + ":hover", tab + ":focus", tab + ":focus-visible", ".ash-tabs__badge"));
        assertScoped("tab-panel", List.of(".ash-tab-panel", ".ash-tab-panel" + DARK, ".ash-tab-panel[hidden]"));
        assertEquals(List.of("@media (hover: hover)"), rules("tabs").stream().map(CssScanner.Rule::media).filter(m -> !m.isEmpty()).distinct().toList(), "hover is the only conditional");
    }

    @Test
    void aTabIsTailwindsButtonWithItsExactSizesBorderTypeAndTransition() throws IOException {
        Map<String, String> tab = declarations("tabs", ".ash-tabs__tab");
        assertEquals("inline-flex", tab.get("display"));
        assertEquals("center", tab.get("align-items"));
        assertEquals("var(--ash-space-2)", tab.get("gap"));
        assertEquals("0", tab.get("margin"));
        assertEquals("0 solid", tab.get("border"), "no border but the bottom one");
        assertEquals("2px", tab.get("border-bottom-width"));
        assertEquals("var(--ash-tabs-border)", tab.get("border-color"));
        assertEquals("0", tab.get("border-radius"));
        assertEquals("transparent", tab.get("background-color"));
        assertEquals("var(--ash-space-3)", tab.get("padding-inline"));
        assertEquals("0.625rem", tab.get("padding-block"), "py-2.5: the foundation has no step this small");
        assertEquals("inherit", tab.get("font"));
        assertEquals("0.875rem", tab.get("font-size"));
        assertEquals("var(--ash-tabs-leading)", tab.get("line-height"));
        assertEquals("calc(1.25 / 0.875)", tab.get("--ash-tabs-leading"), "the ratio is in a property so a minifier cannot fold it");
        assertEquals("var(--font-weight-medium, 500)", tab.get("font-weight"));
        assertEquals("var(--ash-tabs-fg)", tab.get("color"));
        assertEquals("button", tab.get("appearance"));
        assertEquals("color, background-color, border-color, outline-color, text-decoration-color, fill, stroke", tab.get("transition-property"));
        assertEquals("150ms", tab.get("transition-duration"));
        assertEquals("cubic-bezier(0.4, 0, 0.2, 1)", tab.get("transition-timing-function"));
        assertEquals("var(--color-slate-500, oklch(55.4% 0.046 257.417))", tab.get("--ash-tabs-fg"));
        assertEquals("var(--color-slate-700, oklch(37.2% 0.044 257.287))", tab.get("--ash-tabs-fg-hover"));
        assertEquals("transparent", tab.get("--ash-tabs-border"));
        assertEquals("var(--color-slate-300, oklch(86.9% 0.022 252.894))", tab.get("--ash-tabs-border-hover"));
        assertEquals("var(--color-blue-500, oklch(62.3% 0.214 259.815))", tab.get("--ash-tabs-ring"));
    }

    @Test
    void theStateRulesAreOrderedAndValuedSoEachOverridesTheOneBeforeItAsTheUtilitiesDid() throws IOException {
        String tab = ".ash-tabs__tab";
        List<String> order = rules("tabs").stream().map(CssScanner.Rule::selector).toList();
        assertTrue(order.indexOf(tab) < order.indexOf(tab + DARK) && order.indexOf(tab + DARK) < order.indexOf(tab + SELECTED) && order.indexOf(tab + SELECTED) < order.indexOf(tab + SELECTED + DARK)
                && order.indexOf(tab + SELECTED + DARK) < order.indexOf(tab + ":disabled") && order.indexOf(tab + ":disabled") < order.indexOf(tab + ":disabled" + DARK), order.toString());
        assertEquals(Map.of("--ash-tabs-fg", "var(--color-slate-400, oklch(70.4% 0.04 256.788))", "--ash-tabs-fg-hover", "var(--color-slate-200, oklch(92.9% 0.013 255.508))",
                "--ash-tabs-border-hover", "var(--color-slate-600, oklch(44.6% 0.043 257.281))"), declarations("tabs", tab + DARK));
        assertEquals(Map.of("--ash-tabs-fg", "var(--color-blue-600, oklch(54.6% 0.245 262.881))", "--ash-tabs-fg-hover", "var(--color-blue-700, oklch(48.8% 0.243 264.376))",
                "--ash-tabs-border", "var(--color-blue-600, oklch(54.6% 0.245 262.881))", "--ash-tabs-border-hover", "var(--ash-tabs-border)"), declarations("tabs", tab + SELECTED),
                "the selected tab's border does not change on hover");
        assertEquals(Map.of("--ash-tabs-fg", "var(--color-blue-400, oklch(70.7% 0.165 254.624))", "--ash-tabs-fg-hover", "var(--color-blue-300, oklch(80.9% 0.105 251.813))",
                "--ash-tabs-border", "var(--color-blue-500, oklch(62.3% 0.214 259.815))"), declarations("tabs", tab + SELECTED + DARK));
        assertEquals(Map.of("--ash-tabs-fg", "var(--color-slate-300, oklch(86.9% 0.022 252.894))", "--ash-tabs-fg-hover", "var(--color-slate-300, oklch(86.9% 0.022 252.894))",
                "--ash-tabs-border-hover", "transparent", "cursor", "not-allowed"), declarations("tabs", tab + ":disabled"), "a disabled tab does not change on hover and loses its hover border");
        assertEquals(Map.of("--ash-tabs-fg", "var(--color-slate-600, oklch(44.6% 0.043 257.281))", "--ash-tabs-fg-hover", "var(--color-slate-600, oklch(44.6% 0.043 257.281))"),
                declarations("tabs", tab + ":disabled" + DARK));
        assertEquals(Map.of("border-color", "var(--ash-tabs-border-hover)", "color", "var(--ash-tabs-fg-hover)"), declarations("tabs", tab + ":hover", "@media (hover: hover)"));
        assertEquals(Map.of("outline-style", "none"), declarations("tabs", tab + ":focus"));
        assertEquals(Map.of("box-shadow", "0 0 0 2px #fff, 0 0 0 4px var(--ash-tabs-ring)"), declarations("tabs", tab + ":focus-visible"));
    }

    @Test
    void theListTheBadgeAndThePanelAreTailwindsRuleGapPillAndPaddingAndAHiddenPanelCannotBeShown() throws IOException {
        assertEquals(Map.of("display", "flex", "flex-wrap", "wrap", "gap", "0.25rem", "border", "0 solid", "border-bottom-width", "1px", "border-color", "var(--ash-tabs-rule)"),
                declarations("tabs", ".ash-tabs__list"));
        Map<String, String> root = declarations("tabs", ".ash-tabs");
        assertEquals("var(--color-slate-200, oklch(92.9% 0.013 255.508))", root.get("--ash-tabs-rule"));
        assertEquals("var(--color-slate-100, oklch(96.8% 0.007 247.896))", root.get("--ash-tabs-badge-bg"));
        assertEquals("var(--color-slate-600, oklch(44.6% 0.043 257.281))", root.get("--ash-tabs-badge-fg"));
        assertEquals(Map.of("--ash-tabs-rule", "var(--color-slate-700, oklch(37.2% 0.044 257.287))", "--ash-tabs-badge-bg", "var(--color-slate-800, oklch(27.9% 0.041 260.031))",
                "--ash-tabs-badge-fg", "var(--color-slate-300, oklch(86.9% 0.022 252.894))"), declarations("tabs", ".ash-tabs" + DARK));
        Map<String, String> badge = declarations("tabs", ".ash-tabs__badge");
        assertEquals("9999px", badge.get("border-radius"), "the conventional pill radius, as Badge uses");
        assertEquals("1.25rem", badge.get("min-width"));
        assertEquals("0.375rem", badge.get("padding-inline"));
        assertEquals("0.125rem", badge.get("padding-block"));
        assertEquals("0.75rem", badge.get("font-size"));
        assertEquals("calc(1 / 0.75)", badge.get("--ash-tabs-badge-leading"));
        Map<String, String> panel = declarations("tab-panel", ".ash-tab-panel");
        assertEquals("var(--ash-space-4)", panel.get("padding-block"));
        assertEquals("0.875rem", panel.get("font-size"));
        assertEquals("calc(1.25 / 0.875)", panel.get("--ash-tab-panel-leading"));
        assertEquals("var(--color-slate-700, oklch(37.2% 0.044 257.287))", panel.get("--ash-tab-panel-fg"));
        assertFalse(panel.containsKey("display"), "the panel states no display of its own, so nothing here can show it while it is hidden");
        assertEquals(Map.of("--ash-tab-panel-fg", "var(--color-slate-300, oklch(86.9% 0.022 252.894))"), declarations("tab-panel", ".ash-tab-panel" + DARK));
        assertEquals(Map.of("display", "none"), declarations("tab-panel", ".ash-tab-panel[hidden]"));
    }

    // ---- placement ----

    @Test
    void theEntryFileImportsEachInTheComponentsLayer() throws IOException {
        String source = read("src/main/css/ashlar.css");
        for (String name : List.of("tabs", "tab-panel")) {
            assertTrue(source.contains("@import \"components/" + name + ".css\" layer(ash.components);"), name);
        }
    }

    @Test
    void theSharedFoundationTokensAreNotTouched() throws IOException {
        String tokens = read("src/main/css/tokens/tokens.css");
        assertFalse(tokens.contains("--ash-tabs") || tokens.contains("--ash-tab-panel"), "the tabs add nothing to the shared tokens");
    }
}
