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
 * Pins the source contract of Accordion and Accordion Item, migrated together with {@code accordion.js}. The templates keep every parameter, default, native element,
 * role, id, ARIA relationship and data- hook, carry only Ashlar's classes and render no inline style; the script writes the two synchronised attributes and nothing
 * presentational; the stylesheets key the chevron to {@code aria-expanded} and the collapsed row to {@code inert}. The rendered result is measured in a real browser by
 * the showcase's {@code AccordionParityTest}, {@code AccordionStateParityTest} and {@code AccordionScriptTest}; this reads the sources only.
 */
class AccordionContractTest {

    private static final String DARK = ":where([data-theme=\"dark\"], [data-theme=\"dark\"] *)";
    private static final String BUTTON = ".ash-accordion__button";

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
        return read("src/main/resources/META-INF/resources/ashlar/js/accordion.js");
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
        assertEquals(List.of("@import dev.redspartanlabs.ashlar.accordion.AccordionExpansion", "@import dev.redspartanlabs.ashlar.AshlarAssets", "@param String id", "@param gg.jte.Content items"),
                declared("accordion"));
        assertEquals(List.of("@import dev.redspartanlabs.ashlar.accordion.AccordionExpansion", "@import dev.redspartanlabs.ashlar.icon.Icon", "@param String accordionId", "@param String itemId",
                "@param String title", "@param boolean expanded = false", "@param gg.jte.Content content = null"), declared("accordion-item"));
    }

    @Test
    void theWrapperKeepsItsElementItsIdItsHookItsResetAndItsScript() throws IOException {
        assertEquals("<div id=\"${id}\" data-accordion class=\"ash-accordion\"> ${items} </div> <script type=\"module\" src=\"${AshlarAssets.js(\"accordion.js\")}\"></script>",
                markup("accordion").replace("!{AccordionExpansion.reset();} ", ""));
        assertTrue(template("accordion").contains("!{AccordionExpansion.reset();}"), "single-open scoping per accordion instance");
    }

    @Test
    void theItemKeepsItsNativeElementsRolesIdsAndAriaRelationshipsExactly() throws IOException {
        assertEquals("<div data-accordion-item class=\"ash-accordion__item\"> <h3 class=\"ash-accordion__heading\"> <button type=\"button\" id=\"${buttonId}\" data-accordion-button"
                + " aria-expanded=\"${String.valueOf(resolvedExpanded)}\" aria-controls=\"${panelId}\" class=\"ash-accordion__button\" >"
                + " <span class=\"ash-accordion__title\">${title}</span>"
                + " <span data-accordion-chevron class=\"ash-accordion__chevron\">"
                + " @template.ashlar.components.icon(icon = Icon.CHEVRON_RIGHT, classes = \"ash-icon--sm\") </span> </button> </h3>"
                + " <div id=\"${panelId}\" data-accordion-panel role=\"region\" aria-labelledby=\"${buttonId}\" inert=\"${!resolvedExpanded}\" class=\"ash-accordion__panel\" >"
                + " <div class=\"ash-accordion__clip\"> <div class=\"ash-accordion__content\"> @if (content != null) ${content} @endif </div> </div> </div> </div>", markup("accordion-item"));
        String item = template("accordion-item");
        assertTrue(item.contains("!{boolean resolvedExpanded = AccordionExpansion.resolve(expanded);}"));
        assertTrue(item.contains("!{String buttonId = accordionId + \"-button-\" + itemId;}") && item.contains("!{String panelId = accordionId + \"-panel-\" + itemId;}"));
    }

    @Test
    void noInlineStyleNoStateClassAndNoLocalForPresentationRemainsInEitherTemplate() throws IOException {
        for (String name : List.of("accordion", "accordion-item")) {
            String source = template(name).replaceAll("(?s)<%--.*?--%>", "");
            assertFalse(source.contains("style="), name + ": no inline style");
            assertFalse(source.contains("rowValue") || source.contains("chevronRotation") || source.contains("rotate-90") || source.contains("grid-template-rows"), name + ": no presentation local");
        }
    }

    @Test
    void noTailwindUtilityRemainsInEitherTemplate() throws IOException {
        List<String> utilities = List.of("flex", "grid", "gap-", "border", "px-", "py-", "pt-", "pb-", "text-", "font-", "bg-", "dark:", "hover:", "focus", "ring", "rounded", "divide", "overflow", "min-h",
                "h-4", "w-4", "w-full", "slate-", "blue-", "transition", "duration", "ease-", "motion-reduce", "shrink");
        for (String name : List.of("accordion", "accordion-item")) {
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
        assertTrue(js.contains("button.setAttribute(\"aria-expanded\", String(expanded));"));
        assertTrue(js.contains("panel.inert = !expanded;"));
        assertFalse(js.contains("data-accordion-chevron"), "the chevron is no longer touched");
    }

    @Test
    void theScriptKeepsSingleOpenAndPerInstanceScoping() throws IOException {
        String js = script().replaceAll("(?m)^\\s*//.*$", "");
        assertTrue(js.contains("document.addEventListener(\"DOMContentLoaded\""));
        assertTrue(js.contains("data-accordion-button") && js.contains("aria-expanded") && js.contains("aria-controls"));
        assertTrue(js.contains("root.querySelectorAll") || js.contains("root.querySelector"), "queries stay inside the instance's own root");
    }

    // ---- the stylesheets ----

    private static void assertScoped(String cssName, List<String> selectors) throws IOException {
        assertEquals(selectors, rules(cssName).stream().map(CssScanner.Rule::selector).toList(), cssName);
        String stripped = css(cssName).replaceAll("(?s)/\\*.*?\\*/", "");
        assertFalse(stripped.contains("!important"), cssName);
        assertFalse(stripped.contains("@import") || stripped.contains("@layer") || stripped.contains("@keyframes"), cssName);
        assertFalse(stripped.contains("data-accordion"), cssName + ": styling is by class and ARIA state; the data- hooks belong to the script and the skins");
        assertFalse(Pattern.compile("(^|[,{}]\\s*)(\\*|html|body|button|div|span|svg|h3)\\s*[,{]").matcher(stripped).find(), cssName + ": no bare element selector");
    }

    @Test
    void everySelectorIsScopedToItsOwnClassesAndNothingIsGlobal() throws IOException {
        assertScoped("accordion", List.of(".ash-accordion", ".ash-accordion" + DARK, ".ash-accordion__item", ".ash-accordion__item:not(:last-child)"));
        assertScoped("accordion-item", List.of(".ash-accordion__heading", BUTTON, BUTTON + DARK, BUTTON + ":hover", BUTTON + ":focus", BUTTON + ":focus-visible", ".ash-accordion__chevron",
                BUTTON + "[aria-expanded=\"true\"] .ash-accordion__chevron", ".ash-accordion__panel", ".ash-accordion__panel[inert]", ".ash-accordion__clip", ".ash-accordion__content",
                ".ash-accordion__content" + DARK, ".ash-accordion__chevron,\n  .ash-accordion__panel"));
        assertEquals(List.of("@media (hover: hover)", "@media (prefers-reduced-motion: reduce)"),
                rules("accordion-item").stream().map(CssScanner.Rule::media).filter(m -> !m.isEmpty()).distinct().toList(), "hover and reduced motion are the only conditionals");
    }

    @Test
    void theWrapperIsTailwindsBorderRadiusClipAndDivider() throws IOException {
        Map<String, String> root = declarations("accordion", ".ash-accordion");
        assertEquals("var(--color-slate-200, oklch(92.9% 0.013 255.508))", root.get("--ash-accordion-border"));
        assertEquals("hidden", root.get("overflow"));
        assertEquals("1px solid var(--ash-accordion-border)", root.get("border"));
        assertEquals("var(--ash-radius-lg)", root.get("border-radius"));
        assertEquals(Map.of("--ash-accordion-border", "var(--color-slate-700, oklch(37.2% 0.044 257.287))"), declarations("accordion", ".ash-accordion" + DARK));
        assertEquals(Map.of("border", "0 solid"), declarations("accordion", ".ash-accordion__item"));
        assertEquals(Map.of("border-bottom-width", "1px", "border-color", "var(--ash-accordion-border)"), declarations("accordion", ".ash-accordion__item:not(:last-child)"),
                "a rule under every section but the last, as divide-y drew it");
    }

    @Test
    void theButtonIsTailwindsRowWithItsExactSizesWeightAndColours() throws IOException {
        Map<String, String> button = declarations("accordion-item", BUTTON);
        assertEquals("flex", button.get("display"));
        assertEquals("center", button.get("align-items"));
        assertEquals("space-between", button.get("justify-content"));
        assertEquals("var(--ash-space-3)", button.get("gap"));
        assertEquals("0", button.get("margin"));
        assertEquals("0 solid", button.get("border"));
        assertEquals("0", button.get("border-radius"));
        assertEquals("transparent", button.get("background-color"));
        assertEquals("var(--ash-space-4)", button.get("padding-inline"));
        assertEquals("var(--ash-space-3)", button.get("padding-block"));
        assertEquals("100%", button.get("width"));
        assertEquals("left", button.get("text-align"));
        assertEquals("0.875rem", button.get("font-size"));
        assertEquals("var(--ash-accordion-leading)", button.get("line-height"));
        assertEquals("calc(1.25 / 0.875)", button.get("--ash-accordion-leading"), "the ratio is in a property so a minifier cannot fold it");
        assertEquals("var(--font-weight-medium, 500)", button.get("font-weight"));
        assertEquals("var(--ash-accordion-fg)", button.get("color"));
        assertEquals("button", button.get("appearance"));
        assertEquals("var(--color-slate-900, oklch(20.8% 0.042 265.755))", button.get("--ash-accordion-fg"));
        assertEquals("var(--color-slate-50, oklch(98.4% 0.003 247.858))", button.get("--ash-accordion-hover"));
        assertEquals("var(--color-blue-500, oklch(62.3% 0.214 259.815))", button.get("--ash-accordion-ring"));
        assertEquals("var(--color-slate-400, oklch(70.4% 0.04 256.788))", button.get("--ash-accordion-chevron"));
        assertEquals(Map.of("--ash-accordion-fg", "var(--color-slate-100, oklch(96.8% 0.007 247.896))",
                "--ash-accordion-hover", "color-mix(in oklab, var(--color-slate-800, oklch(27.9% 0.041 260.031)) 50%, transparent)",
                "--ash-accordion-chevron", "var(--color-slate-500, oklch(55.4% 0.046 257.417))"), declarations("accordion-item", BUTTON + DARK));
        assertEquals(Map.of("background-color", "var(--ash-accordion-hover)"), declarations("accordion-item", BUTTON + ":hover", "@media (hover: hover)"));
        assertEquals(Map.of("outline-style", "none"), declarations("accordion-item", BUTTON + ":focus"));
        assertEquals(Map.of("box-shadow", "inset 0 0 0 2px var(--ash-accordion-ring)"), declarations("accordion-item", BUTTON + ":focus-visible"), "the ring is inset: the wrapper clips");
    }

    @Test
    void theChevronTurnsOnAriaExpandedAndThePanelCollapsesOnInertWithNoStateClass() throws IOException {
        Map<String, String> chevron = declarations("accordion-item", ".ash-accordion__chevron");
        assertEquals("0", chevron.get("flex-shrink"));
        assertEquals("var(--ash-accordion-chevron)", chevron.get("color"));
        assertEquals("transform, translate, scale, rotate", chevron.get("transition-property"));
        assertEquals("200ms", chevron.get("transition-duration"));
        assertEquals("cubic-bezier(0, 0, 0.2, 1)", chevron.get("transition-timing-function"));
        assertEquals(Map.of("rotate", "90deg"), declarations("accordion-item", BUTTON + "[aria-expanded=\"true\"] .ash-accordion__chevron"));
        Map<String, String> panel = declarations("accordion-item", ".ash-accordion__panel");
        assertEquals("grid", panel.get("display"));
        assertEquals("1fr", panel.get("grid-template-rows"));
        assertEquals("grid-template-rows", panel.get("transition-property"));
        assertEquals("200ms", panel.get("transition-duration"));
        assertEquals("cubic-bezier(0, 0, 0.2, 1)", panel.get("transition-timing-function"));
        assertEquals(Map.of("grid-template-rows", "0fr"), declarations("accordion-item", ".ash-accordion__panel[inert]"));
        assertEquals(Map.of("min-height", "0", "overflow", "hidden"), declarations("accordion-item", ".ash-accordion__clip"));
        assertFalse(css("accordion-item").replaceAll("(?s)/\\*.*?\\*/", "").contains("display: none"), "the panel is never display: none; that cannot be animated");
    }

    @Test
    void theContentAndReducedMotionAreExact() throws IOException {
        Map<String, String> content = declarations("accordion-item", ".ash-accordion__content");
        assertEquals("var(--ash-space-4)", content.get("padding-inline"));
        assertEquals("0.25rem", content.get("padding-top"), "pt-1: the foundation has no step this small");
        assertEquals("var(--ash-space-4)", content.get("padding-bottom"));
        assertEquals("0.875rem", content.get("font-size"));
        assertEquals("var(--ash-accordion-leading)", content.get("line-height"));
        assertEquals("calc(1.25 / 0.875)", content.get("--ash-accordion-leading"), "defined where it is used: the content is not inside the button");
        assertEquals("var(--ash-accordion-content)", content.get("color"));
        assertEquals("var(--color-slate-700, oklch(37.2% 0.044 257.287))", content.get("--ash-accordion-content"));
        assertEquals(Map.of("--ash-accordion-content", "var(--color-slate-300, oklch(86.9% 0.022 252.894))"), declarations("accordion-item", ".ash-accordion__content" + DARK));
        assertEquals(Map.of("margin", "0", "font-size", "inherit", "font-weight", "inherit"), declarations("accordion-item", ".ash-accordion__heading"));
        assertEquals(Map.of("transition-property", "none"), declarations("accordion-item", ".ash-accordion__chevron,\n  .ash-accordion__panel", "@media (prefers-reduced-motion: reduce)"));
    }

    // ---- placement ----

    @Test
    void theEntryFileImportsEachInTheComponentsLayer() throws IOException {
        String source = read("src/main/css/ashlar.css");
        for (String name : List.of("accordion", "accordion-item")) {
            assertTrue(source.contains("@import \"components/" + name + ".css\" layer(ash.components);"), name);
        }
        assertFalse(source.contains("components/tree"), "Tree and Tree Item are a later migration");
    }

    @Test
    void theSharedFoundationTokensAreNotTouched() throws IOException {
        String tokens = read("src/main/css/tokens/tokens.css");
        assertFalse(tokens.contains("--ash-accordion"), "the accordion adds nothing to the shared tokens");
    }
}
