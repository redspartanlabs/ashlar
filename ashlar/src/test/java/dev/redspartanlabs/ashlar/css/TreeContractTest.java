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
 * Pins the source contract of Tree and Tree Item, migrated together with {@code tree.js}. The templates keep every parameter, default, native element, role, ARIA attribute
 * and data- hook, carry only Ashlar's classes and render no inline style; the script writes the two synchronised attributes and nothing presentational; the stylesheets
 * key the chevron to the item's own {@code aria-expanded} and the collapsed row to the panel's {@code inert}, through child combinators only so a parent's state never
 * reaches the items nested under it. The rendered result is measured in a real browser by the showcase's {@code TreeParityTest}, {@code TreeStateParityTest},
 * {@code TreeScriptTest}, {@code TreeLegacyBehaviourTest} and {@code TreeTailwindFreeTest}; this reads the sources only.
 */
class TreeContractTest {

    private static final String DARK = ":where([data-theme=\"dark\"], [data-theme=\"dark\"] *)";
    private static final String CHEVRON_TURN = ".ash-tree__item[aria-expanded=\"true\"] > .ash-tree__row > .ash-tree__toggle > .ash-tree__chevron";

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

    private static String script() throws IOException {
        return read("src/main/resources/META-INF/resources/ashlar/js/tree.js");
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
        assertEquals(List.of("@import dev.redspartanlabs.ashlar.tree.TreeContext", "@import dev.redspartanlabs.ashlar.AshlarAssets", "@param gg.jte.Content items"), declared("tree"));
        assertEquals(List.of("@import dev.redspartanlabs.ashlar.tree.TreeContext", "@import dev.redspartanlabs.ashlar.icon.Icon", "@param String label", "@param String href = null",
                "@param Icon icon = null", "@param boolean expanded = false", "@param gg.jte.Content children = null"), declared("tree-item"));
    }

    @Test
    void theRootKeepsItsListElementItsRoleItsHookItsDepthResetAndItsScript() throws IOException {
        assertEquals("<ul role=\"tree\" data-tree class=\"ash-tree\"> ${items} </ul> <script type=\"module\" src=\"${AshlarAssets.js(\"tree.js\")}\"></script>",
                markup("tree").replace("!{TreeContext.reset();} ", ""));
        assertTrue(template("tree").contains("!{TreeContext.reset();}"), "the depth counter restarts for every tree");
    }

    @Test
    void theItemKeepsItsNativeElementsRolesAriaAttributesAndHooksExactly() throws IOException {
        assertEquals("<li role=\"treeitem\" aria-expanded=\"${ariaExpandedAttr}\" aria-level=\"${level}\" tabindex=\"-1\" data-tree-item class=\"ash-tree__item\" >"
                + " <div class=\"ash-tree__row\"> @if (hasChildren) <button type=\"button\" tabindex=\"-1\" aria-hidden=\"true\" data-tree-toggle class=\"ash-tree__toggle\" >"
                + " <span data-tree-chevron class=\"ash-tree__chevron\"> @template.ashlar.components.icon(icon = Icon.CHEVRON_RIGHT, classes = \"ash-icon--sm\") </span> </button>"
                + " @else <span class=\"ash-tree__spacer\" aria-hidden=\"true\"></span> @endif"
                + " @if (hasIcon) <span class=\"ash-tree__icon\" aria-hidden=\"true\"> @template.ashlar.components.icon(icon = icon, classes = \"ash-icon--sm\") </span> @endif"
                + " @if (isLink) <a href=\"${href}\" tabindex=\"-1\" class=\"ash-tree__label\">${label}</a> @else <span class=\"ash-tree__label\">${label}</span> @endif </div>"
                + " @if (hasChildren) <div data-tree-children inert=\"${!expanded}\" class=\"ash-tree__panel\" > <div class=\"ash-tree__clip\">"
                + " <ul role=\"group\" class=\"ash-tree__group\"> ${children} </ul> </div> </div> @endif </li>",
                markup("tree-item").replace("!{TreeContext.enterChildren();} ", "").replace("!{TreeContext.exitChildren();} ", ""));
        String item = template("tree-item");
        assertTrue(item.contains("!{int level = TreeContext.currentLevel();}"), "aria-level still comes from TreeContext");
        assertTrue(item.contains("!{TreeContext.enterChildren();}") && item.contains("!{TreeContext.exitChildren();}"), "the depth is entered and left around the children");
        assertTrue(item.contains("!{String ariaExpandedAttr = hasChildren ? String.valueOf(expanded) : null;}"), "a leaf has no aria-expanded");
        assertTrue(item.indexOf("TreeContext.currentLevel()") < item.indexOf("TreeContext.enterChildren()"), "the level is read before the children are entered");
    }

    @Test
    void noInlineStyleNoStateClassAndNoLocalForPresentationRemainsInEitherTemplate() throws IOException {
        for (String name : List.of("tree", "tree-item")) {
            String source = template(name).replaceAll("(?s)<%--.*?--%>", "");
            assertFalse(source.contains("style="), name + ": no inline style");
            assertFalse(source.contains("rowValue") || source.contains("chevronRotation") || source.contains("rotate-90") || source.contains("grid-template-rows"), name + ": no presentation local");
        }
    }

    @Test
    void noTailwindUtilityRemainsInEitherTemplate() throws IOException {
        List<String> utilities = List.of("flex", "grid", "gap-", "border", "px-", "py-", "pl-", "text-", "font-", "bg-", "dark:", "hover:", "focus:", "ring", "rounded", "overflow", "min-h", "min-w",
                "truncate", "h-4", "w-4", "h-5", "w-5", "slate-", "blue-", "transition", "duration", "ease-", "motion-reduce", "shrink", "cursor-", "items-", "justify-");
        for (String name : List.of("tree", "tree-item")) {
            String source = template(name).replaceAll("(?s)<%--.*?--%>", "").replaceAll("(?m)^@(?:param|import) .*$", "");
            for (String utility : utilities) {
                assertFalse(Pattern.compile("(?<![\\w-])" + Pattern.quote(utility)).matcher(source).find(), name + ": a Tailwind utility is back: " + utility);
            }
        }
    }

    // ---- the script ----

    @Test
    void theScriptWritesOnlyTheTwoSynchronisedAttributesAndNothingPresentational() throws IOException {
        String js = script().replaceAll("(?m)^\\s*//.*$", "");
        assertFalse(js.contains("classList") || js.contains("className") || js.contains("rotate-90"), "no class is added, removed or replaced");
        assertFalse(js.contains(".style") || js.contains("gridTemplateRows") || js.contains("setProperty"), "no inline style is written");
        assertTrue(js.contains("item.setAttribute(\"aria-expanded\", String(expanded));"));
        assertTrue(js.contains("group.inert = !expanded;"));
        assertFalse(js.contains("data-tree-chevron"), "the chevron is no longer touched");
    }

    @Test
    void theScriptKeepsItsKeyboardModelAndPerInstanceScoping() throws IOException {
        String js = script().replaceAll("(?m)^\\s*//.*$", "");
        assertTrue(js.contains("document.addEventListener(\"DOMContentLoaded\""));
        for (String key : List.of("ArrowDown", "ArrowUp", "ArrowRight", "ArrowLeft", "Home", "End", "Enter", "\" \"")) {
            assertTrue(js.contains("case " + (key.startsWith("\"") ? key : "\"" + key + "\"") + ":"), "still handles " + key);
        }
        assertTrue(js.contains("root.querySelectorAll") && js.contains("root.querySelector"), "queries stay inside the instance's own root");
        assertTrue(js.contains(":scope > [data-tree-children]") && js.contains(":scope > div a[href]"), "the row stays a <div> directly under the item");
        assertTrue(js.contains("\"tabindex\", \"0\"") && js.contains("\"tabindex\", \"-1\""), "roving tabindex");
    }

    // ---- the stylesheets ----

    private static void assertScoped(String cssName, List<String> selectors) throws IOException {
        assertEquals(selectors, rules(cssName).stream().map(CssScanner.Rule::selector).toList(), cssName);
        String stripped = css(cssName).replaceAll("(?s)/\\*.*?\\*/", "");
        assertFalse(stripped.contains("!important"), cssName);
        assertFalse(stripped.contains("@import") || stripped.contains("@layer") || stripped.contains("@keyframes"), cssName);
        assertFalse(stripped.contains("data-tree"), cssName + ": styling is by class and ARIA state; the data- hooks belong to the script and the skins");
        assertFalse(stripped.contains(":inert"), cssName + ": the :inert pseudo-class is true for everything inside a collapsed ancestor, so it would leak; read the attribute");
        assertFalse(Pattern.compile("(^|[,{}]\\s*)(\\*|html|body|button|div|span|svg|ul|li)\\s*[,{:.\\[]").matcher(stripped).find(), cssName + ": no bare element selector");
    }

    @Test
    void everySelectorIsScopedToItsOwnClassesAndNothingIsGlobal() throws IOException {
        assertScoped("tree", List.of(".ash-tree"));
        assertScoped("tree-item", List.of(".ash-tree__item", ".ash-tree__item:focus", ".ash-tree__row", ".ash-tree__row" + DARK, ".ash-tree__row:hover", ".ash-tree__item:focus-visible > .ash-tree__row",
                ".ash-tree__toggle", ".ash-tree__toggle:hover", ".ash-tree__chevron", CHEVRON_TURN, ".ash-tree__spacer", ".ash-tree__icon", ".ash-tree__label", "a.ash-tree__label",
                "a.ash-tree__label:focus", "a.ash-tree__label:hover", ".ash-tree__panel", ".ash-tree__panel[inert]", ".ash-tree__clip", ".ash-tree__group", ".ash-tree__chevron,\n  .ash-tree__panel"));
        assertEquals(List.of("@media (hover: hover)", "@media (prefers-reduced-motion: reduce)"),
                rules("tree-item").stream().map(CssScanner.Rule::media).filter(m -> !m.isEmpty()).distinct().toList(), "hover and reduced motion are the only conditionals");
    }

    @Test
    void everyStateRuleIsAnchoredWithChildCombinatorsSoNoStateReachesANestedItem() throws IOException {
        List<String> selectors = rules("tree-item").stream().map(CssScanner.Rule::selector).toList();
        assertTrue(selectors.contains(CHEVRON_TURN), "the chevron turn is li > row > toggle > chevron");
        assertTrue(selectors.contains(".ash-tree__item:focus-visible > .ash-tree__row"), "the focus ring is li > row");
        for (String selector : selectors) {
            boolean readsState = selector.contains("[aria-expanded") || selector.contains(":focus-visible");
            if (readsState) {
                assertFalse(selector.replaceAll("\\s*>\\s*", ">").contains(" "), "a descendant combinator would let a parent's state reach its children: " + selector);
            }
        }
        assertEquals(List.of(".ash-tree__panel[inert]"), selectors.stream().filter(s -> s.contains("[inert]")).toList(), "the collapsed row reads the panel's own attribute and nothing else");
    }

    @Test
    void theRootIsTheMeasuredFlexColumnWithATwoPixelGapAndTheTextSizeEveryRowInherits() throws IOException {
        Map<String, String> root = declarations("tree", ".ash-tree");
        assertEquals("flex", root.get("display"));
        assertEquals("column", root.get("flex-direction"));
        assertEquals("0.125rem", root.get("gap"));
        assertEquals("0", root.get("margin"));
        assertEquals("0", root.get("padding"));
        assertEquals("none", root.get("list-style"));
        assertEquals("0.875rem", root.get("font-size"));
        assertEquals("var(--ash-tree-leading)", root.get("line-height"));
        assertEquals("calc(1.25 / 0.875)", root.get("--ash-tree-leading"), "the ratio is in a property so a minifier cannot fold it");
    }

    @Test
    void theRowIsTheMeasuredThirtyTwoPixelRowWithItsExactColours() throws IOException {
        Map<String, String> row = declarations("tree-item", ".ash-tree__row");
        assertEquals("flex", row.get("display"));
        assertEquals("center", row.get("align-items"));
        assertEquals("0.25rem", row.get("gap"));
        assertEquals("var(--ash-radius-md)", row.get("border-radius"));
        assertEquals("var(--ash-space-2)", row.get("padding-inline"));
        assertEquals("0.375rem", row.get("padding-block"), "py-1.5: the foundation has no step this small");
        assertEquals("var(--ash-tree-fg)", row.get("color"));
        assertEquals("var(--color-slate-700, oklch(37.2% 0.044 257.287))", row.get("--ash-tree-fg"));
        assertEquals("var(--color-slate-100, oklch(96.8% 0.007 247.896))", row.get("--ash-tree-hover"));
        assertEquals("var(--color-blue-50, oklch(97% 0.014 254.604))", row.get("--ash-tree-focus-bg"));
        assertEquals("var(--color-blue-500, oklch(62.3% 0.214 259.815))", row.get("--ash-tree-ring"));
        assertEquals("var(--color-slate-400, oklch(70.4% 0.04 256.788))", row.get("--ash-tree-toggle"));
        assertEquals("var(--color-slate-600, oklch(44.6% 0.043 257.281))", row.get("--ash-tree-toggle-hover"));
        assertEquals("var(--color-slate-400, oklch(70.4% 0.04 256.788))", row.get("--ash-tree-icon"));
        assertEquals("var(--color-slate-900, oklch(20.8% 0.042 265.755))", row.get("--ash-tree-label"));
        assertEquals(Map.of("--ash-tree-fg", "var(--color-slate-300, oklch(86.9% 0.022 252.894))", "--ash-tree-hover", "var(--color-slate-800, oklch(27.9% 0.041 260.031))",
                "--ash-tree-focus-bg", "color-mix(in oklab, var(--color-blue-950, oklch(28.2% 0.091 267.935)) 40%, transparent)",
                "--ash-tree-toggle", "var(--color-slate-500, oklch(55.4% 0.046 257.417))", "--ash-tree-toggle-hover", "var(--color-slate-300, oklch(86.9% 0.022 252.894))",
                "--ash-tree-icon", "var(--color-slate-500, oklch(55.4% 0.046 257.417))", "--ash-tree-label", "var(--color-slate-100, oklch(96.8% 0.007 247.896))"),
                declarations("tree-item", ".ash-tree__row" + DARK));
        assertEquals(Map.of("margin", "0", "padding", "0", "list-style", "none"), declarations("tree-item", ".ash-tree__item"));
        assertEquals(Map.of("outline-style", "none"), declarations("tree-item", ".ash-tree__item:focus"));
    }

    @Test
    void hoverIsLimitedToHoverDevicesAndFocusVisibleBeatsHoverByTheCascade() throws IOException {
        assertEquals(Map.of("background-color", "var(--ash-tree-hover)"), declarations("tree-item", ".ash-tree__row:hover", "@media (hover: hover)"));
        assertEquals(Map.of("color", "var(--ash-tree-toggle-hover)"), declarations("tree-item", ".ash-tree__toggle:hover", "@media (hover: hover)"));
        assertEquals(Map.of("text-decoration-line", "underline"), declarations("tree-item", "a.ash-tree__label:hover", "@media (hover: hover)"));
        assertEquals(Map.of("background-color", "var(--ash-tree-focus-bg)", "box-shadow", "inset 0 0 0 2px var(--ash-tree-ring)"),
                declarations("tree-item", ".ash-tree__item:focus-visible > .ash-tree__row"), "the fill and the ring are drawn on the item's own row only, and the ring is inset");
        String source = css("tree-item").replaceAll("(?s)/\\*.*?\\*/", "");
        assertTrue(source.indexOf(".ash-tree__row:hover") < source.indexOf(".ash-tree__item:focus-visible > .ash-tree__row"), "the focus rule comes after the hover rule");
        assertEquals(List.of(), rules("tree-item").stream().filter(r -> r.selector().contains(":hover") && r.media().isEmpty()).toList(), "no :hover rule outside the hover media query");
    }

    @Test
    void theToggleKeepsEveryPreflightResetAndTheNativePushButtonAppearance() throws IOException {
        Map<String, String> toggle = declarations("tree-item", ".ash-tree__toggle");
        assertEquals("flex", toggle.get("display"));
        assertEquals("0", toggle.get("flex-shrink"));
        assertEquals("center", toggle.get("align-items"));
        assertEquals("center", toggle.get("justify-content"));
        assertEquals("0", toggle.get("margin"));
        assertEquals("0 solid", toggle.get("border"));
        assertEquals("0.25rem", toggle.get("border-radius"), "a fixed 0.25rem, as the bare rounded utility was: a skin's small radius does not change it");
        assertEquals("transparent", toggle.get("background-color"));
        assertEquals("0", toggle.get("padding"));
        assertEquals("1.25rem", toggle.get("width"));
        assertEquals("1.25rem", toggle.get("height"));
        assertEquals("inherit", toggle.get("font"));
        assertEquals("inherit", toggle.get("letter-spacing"));
        assertEquals("none", toggle.get("text-transform"));
        assertEquals("var(--ash-tree-toggle)", toggle.get("color"));
        assertEquals("pointer", toggle.get("cursor"));
        assertEquals("button", toggle.get("appearance"));
    }

    @Test
    void theChevronTurnsOnItsOwnItemsAriaExpandedAndThePanelCollapsesOnItsOwnInertWithNoStateClass() throws IOException {
        Map<String, String> chevron = declarations("tree-item", ".ash-tree__chevron");
        assertEquals("transform, translate, scale, rotate", chevron.get("transition-property"));
        assertEquals("150ms", chevron.get("transition-duration"));
        assertEquals("cubic-bezier(0, 0, 0.2, 1)", chevron.get("transition-timing-function"));
        assertEquals(Map.of("rotate", "90deg"), declarations("tree-item", CHEVRON_TURN));
        Map<String, String> panel = declarations("tree-item", ".ash-tree__panel");
        assertEquals("grid", panel.get("display"));
        assertEquals("1fr", panel.get("grid-template-rows"));
        assertEquals("grid-template-rows", panel.get("transition-property"));
        assertEquals("150ms", panel.get("transition-duration"));
        assertEquals("cubic-bezier(0, 0, 0.2, 1)", panel.get("transition-timing-function"));
        assertEquals(Map.of("grid-template-rows", "0fr"), declarations("tree-item", ".ash-tree__panel[inert]"));
        assertEquals(Map.of("min-height", "0", "overflow", "hidden"), declarations("tree-item", ".ash-tree__clip"));
        assertFalse(css("tree-item").replaceAll("(?s)/\\*.*?\\*/", "").contains("display: none"), "the panel is never display: none; that cannot be animated");
    }

    @Test
    void theGroupIndentsTwentyPixelsAndTheListResetsAreStated() throws IOException {
        assertEquals(Map.of("margin", "0", "padding", "0", "padding-left", "1.25rem", "list-style", "none"), declarations("tree-item", ".ash-tree__group"));
        Map<String, String> spacer = declarations("tree-item", ".ash-tree__spacer");
        assertEquals(Map.of("flex-shrink", "0", "width", "1.25rem", "height", "1.25rem"), spacer);
        assertEquals(Map.of("flex-shrink", "0", "color", "var(--ash-tree-icon)"), declarations("tree-item", ".ash-tree__icon"));
    }

    @Test
    void theLabelTruncatesItselfAndALinkLabelKeepsItsOwnResets() throws IOException {
        Map<String, String> label = declarations("tree-item", ".ash-tree__label");
        assertEquals("0", label.get("min-width"));
        assertEquals("1 1 0%", label.get("flex"));
        assertEquals("hidden", label.get("overflow"));
        assertEquals("ellipsis", label.get("text-overflow"));
        assertEquals("nowrap", label.get("white-space"));
        assertEquals("var(--font-weight-medium, 500)", label.get("font-weight"));
        assertEquals("var(--ash-tree-label)", label.get("color"));
        assertEquals(Map.of("border-radius", "var(--ash-radius-sm)", "text-decoration", "none", "cursor", "pointer"), declarations("tree-item", "a.ash-tree__label"));
        assertEquals(Map.of("outline-style", "none"), declarations("tree-item", "a.ash-tree__label:focus"));
    }

    @Test
    void reducedMotionRemovesTheTransitionPropertyAndLeavesTheDurationAndEasingAsTheyWere() throws IOException {
        assertEquals(Map.of("transition-property", "none"), declarations("tree-item", ".ash-tree__chevron,\n  .ash-tree__panel", "@media (prefers-reduced-motion: reduce)"),
                "transition-property: none, not transition: none: the legacy motion-reduce:transition-none computed exactly this");
    }

    // ---- placement ----

    @Test
    void theEntryFileImportsEachInTheComponentsLayer() throws IOException {
        String source = read("src/main/css/ashlar.css");
        for (String name : List.of("tree", "tree-item")) {
            assertTrue(source.contains("@import \"components/" + name + ".css\" layer(ash.components);"), name);
        }
    }

    @Test
    void theSharedFoundationTokensAreNotTouched() throws IOException {
        String tokens = read("src/main/css/tokens/tokens.css");
        assertFalse(tokens.contains("--ash-tree"), "the tree adds nothing to the shared tokens");
    }
}
