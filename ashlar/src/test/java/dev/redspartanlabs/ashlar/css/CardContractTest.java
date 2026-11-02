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
 * Pins the source contract of Card and Stat Card, migrated together. Each template keeps its parameters, its native elements and its optional
 * parts exactly, adds no role, label or escape hatch, carries only Ashlar's classes, and Card gains exactly the two additive hooks
 * ({@code data-card-header}, {@code data-card-footer}) a skin selects instead of the Tailwind classes it used to depend on; each stylesheet
 * is scoped to its own classes. The rendered result is measured in a real browser by the showcase's {@code CardParityTest}; this reads the
 * sources only.
 */
class CardContractTest {

    private static final List<String> NAMES = List.of("card", "stat-card");
    private static final String DARK = ":where([data-theme=\"dark\"], [data-theme=\"dark\"] *)";

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
        return withoutJteComments(template(name)).replaceAll("(?m)^@param .*$", "").replaceAll("(?m)^!\\{.*\\}$", "").replaceAll("\\s+", " ").trim();
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
        List<CssScanner.Rule> found = rules(cssName).stream().filter(r -> r.selector().equals(selector) && r.media().isEmpty()).toList();
        assertEquals(1, found.size(), selector + " must be defined exactly once in " + cssName);
        return CssScanner.declarations(found.get(0).block());
    }

    // ---- the templates ----

    @Test
    void theParametersAndTheirDefaultsAreUnchanged() throws IOException {
        assertEquals(List.of("@param String title = \"\"", "@param gg.jte.Content body = null", "@param gg.jte.Content footer = null"), declared("card"));
        assertEquals(List.of("@param String label", "@param String value", "@param String description = \"\"", "@param gg.jte.Content change = null"),
                declared("stat-card"));
    }

    @Test
    void eachTemplateKeepsItsNativeElementsItsOptionalPartsAndItsOrderExactly() throws IOException {
        assertEquals("<div class=\"ash-card\" data-card>"
                + " @if (hasTitle) <div class=\"ash-card__header\" data-card-header> <h3 class=\"ash-card__title\">${title}</h3> </div> @endif"
                + " @if (body != null) <div class=\"ash-card__body\"> ${body} </div> @endif"
                + " @if (footer != null) <div class=\"ash-card__footer\" data-card-footer> ${footer} </div> @endif </div>", markup("card"));
        assertEquals("<div class=\"ash-stat-card\" data-stat-card> <dl class=\"ash-stat-card__list\">"
                + " <dt class=\"ash-stat-card__label\">${label}</dt>"
                + " <dd class=\"ash-stat-card__values\"> <span class=\"ash-stat-card__value\">${value}</span>"
                + " @if (hasChange) <span class=\"ash-stat-card__change\">${change}</span> @endif </dd>"
                + " @if (hasDescription) <dd class=\"ash-stat-card__description\">${description}</dd> @endif </dl> </div>", markup("stat-card"));
    }

    @Test
    void theCardsTitleIsAnH3AndTheStatCardKeepsItsDefinitionList() throws IOException {
        assertEquals(1, count(markup("card"), "<h3"));
        assertEquals(0, count(markup("card"), "<h1") + count(markup("card"), "<h2"));
        String stat = markup("stat-card");
        assertEquals(1, count(stat, "<dl"));
        assertEquals(1, count(stat, "<dt"));
        assertEquals(2, count(stat, "<dd"), "the values row and the optional description");
        assertFalse(stat.contains("<h"), "the label is a <dt>, not a heading");
    }

    @Test
    void cardGainsExactlyTheTwoAdditiveHooksAndNothingElseIsAddedToEitherTemplate() throws IOException {
        for (String name : NAMES) {
            String markup = withoutJteComments(template(name));
            for (String forbidden : List.of("role=", "aria-", "style=", "id=", "tabindex")) {
                assertFalse(markup.contains(forbidden), name + " adds no " + forbidden);
            }
        }
        List<String> hooks = new ArrayList<>();
        Matcher m = Pattern.compile("(?<=[\\s\"])(data-[a-z-]+)(?=[\\s>=])").matcher(withoutJteComments(template("card")));
        while (m.find()) {
            hooks.add(m.group(1));
        }
        assertEquals(List.of("data-card", "data-card-header", "data-card-footer"), hooks);
        List<String> statHooks = new ArrayList<>();
        m = Pattern.compile("(?<=[\\s\"])(data-[a-z-]+)(?=[\\s>=])").matcher(withoutJteComments(template("stat-card")));
        while (m.find()) {
            statHooks.add(m.group(1));
        }
        assertEquals(List.of("data-stat-card"), statHooks, "the value hook is deferred: skins select the value structurally");
    }

    @Test
    void noTailwindUtilityRemainsInEitherTemplate() throws IOException {
        List<String> utilities = List.of("overflow-hidden", "rounded", "border", "bg-", "dark:", "text-", "font-", "px-", "py-", "p-6", "mt-", "flex", "gap-", "items-",
                "justify-", "shrink", "break-words", "tracking-", "slate-");
        for (String name : NAMES) {
            String markup = withoutJteComments(template(name));
            for (String utility : utilities) {
                assertFalse(Pattern.compile("(?<![\\w-])" + Pattern.quote(utility)).matcher(markup).find(), name + ": a Tailwind utility is back: " + utility);
            }
        }
    }

    // ---- the stylesheets ----

    private static void assertScoped(String cssName, List<String> selectors) throws IOException {
        assertEquals(selectors, rules(cssName).stream().map(CssScanner.Rule::selector).toList(), cssName);
        String stripped = css(cssName).replaceAll("(?s)/\\*.*?\\*/", "");
        assertFalse(stripped.contains("!important"), cssName);
        assertFalse(stripped.contains("@import") || stripped.contains("@layer") || stripped.contains("@keyframes") || stripped.contains("@media"), cssName);
        assertFalse(stripped.contains(">"), cssName + ": no child combinator reaches the caller's content");
        assertFalse(stripped.contains("data-card") || stripped.contains("data-stat-card"), cssName + ": styling is by class; the data- hooks belong to skins");
        assertFalse(Pattern.compile("(^|[,{}]\\s*)(\\*|html|body|h1|h2|h3|p|div|dl|dt|dd|span)\\s*[,{]").matcher(stripped).find(), cssName + ": no bare element selector");
    }

    @Test
    void everySelectorIsScopedToItsOwnClassesAndNothingIsGlobal() throws IOException {
        assertScoped("card", List.of(".ash-card", ".ash-card" + DARK, ".ash-card__header", ".ash-card__title", ".ash-card__body", ".ash-card__footer"));
        assertScoped("stat-card", List.of(".ash-stat-card", ".ash-stat-card" + DARK, ".ash-stat-card__list", ".ash-stat-card__label", ".ash-stat-card__values",
                ".ash-stat-card__value", ".ash-stat-card__change", ".ash-stat-card__description"));
    }

    @Test
    void cardIsABorderedClippedSurfaceWithTailwindsExactColoursRadiusAndNoLayoutOfItsOwn() throws IOException {
        Map<String, String> card = declarations("card", ".ash-card");
        assertEquals("hidden", card.get("overflow"));
        assertEquals("1px solid var(--ash-card-border)", card.get("border"));
        assertEquals("var(--radius-xl, 0.75rem)", card.get("border-radius"), "a skin's --radius-xl rounds the card; the foundation tokens are untouched");
        assertEquals("var(--ash-card-bg)", card.get("background-color"));
        assertEquals("var(--color-white, #fff)", card.get("--ash-card-bg"), "a skin's --color-white is the card surface");
        assertEquals("var(--color-slate-200, oklch(92.9% 0.013 255.508))", card.get("--ash-card-border"));
        assertEquals("var(--color-slate-900, oklch(20.8% 0.042 265.755))", card.get("--ash-card-title"));
        assertEquals("var(--color-slate-700, oklch(37.2% 0.044 257.287))", card.get("--ash-card-text"));
        assertEquals("var(--color-slate-50, oklch(98.4% 0.003 247.858))", card.get("--ash-card-footer-bg"));
        assertEquals("calc(1.25 / 0.875)", card.get("--ash-card-body-leading"), "the ratio is in a property so a minifier cannot fold it");
        for (String layout : List.of("width", "max-width", "margin", "display", "position", "grid-column", "box-shadow")) {
            assertFalse(card.containsKey(layout), "the card does not place itself or draw a shadow: " + layout);
        }
        assertEquals(Map.of("--ash-card-bg", "var(--color-slate-900, oklch(20.8% 0.042 265.755))", "--ash-card-border", "var(--color-slate-700, oklch(37.2% 0.044 257.287))",
                "--ash-card-title", "var(--color-slate-100, oklch(96.8% 0.007 247.896))", "--ash-card-text", "var(--color-slate-300, oklch(86.9% 0.022 252.894))",
                "--ash-card-footer-bg", "var(--color-slate-800, oklch(27.9% 0.041 260.031))"), declarations("card", ".ash-card" + DARK));
    }

    @Test
    void cardsHeaderTitleBodyAndFooterHaveTheirTailwindSpacingRulesAndTypography() throws IOException {
        assertEquals(Map.of("border", "0 solid var(--ash-card-border)", "border-bottom-width", "1px", "padding", "var(--ash-space-4) var(--ash-space-6)"),
                declarations("card", ".ash-card__header"));
        assertEquals(Map.of("margin", "0", "font-size", "1rem", "line-height", "1.5", "font-weight", "600", "color", "var(--ash-card-title)"),
                declarations("card", ".ash-card__title"));
        assertEquals(Map.of("padding", "var(--ash-space-4) var(--ash-space-6)", "font-size", "0.875rem", "line-height", "var(--ash-card-body-leading)",
                "color", "var(--ash-card-text)"), declarations("card", ".ash-card__body"));
        assertEquals(Map.of("display", "flex", "flex-wrap", "wrap", "align-items", "center", "justify-content", "flex-end", "gap", "var(--ash-space-3)",
                "border", "0 solid var(--ash-card-border)", "border-top-width", "1px", "background-color", "var(--ash-card-footer-bg)",
                "padding", "var(--ash-space-4) var(--ash-space-6)"), declarations("card", ".ash-card__footer"));
    }

    @Test
    void statCardIsABorderedSurfaceThatStatesEveryMarginPreflightUsedToRemove() throws IOException {
        Map<String, String> card = declarations("stat-card", ".ash-stat-card");
        assertEquals("1px solid var(--ash-stat-card-border)", card.get("border"));
        assertEquals("var(--radius-xl, 0.75rem)", card.get("border-radius"));
        assertEquals("var(--ash-stat-card-bg)", card.get("background-color"));
        assertEquals("var(--ash-space-6)", card.get("padding"));
        assertEquals("var(--color-white, #fff)", card.get("--ash-stat-card-bg"));
        assertEquals("var(--color-slate-200, oklch(92.9% 0.013 255.508))", card.get("--ash-stat-card-border"));
        assertEquals("var(--color-slate-500, oklch(55.4% 0.046 257.417))", card.get("--ash-stat-card-label"));
        assertEquals("var(--color-slate-900, oklch(20.8% 0.042 265.755))", card.get("--ash-stat-card-value"));
        assertEquals("var(--color-slate-500, oklch(55.4% 0.046 257.417))", card.get("--ash-stat-card-description"));
        assertEquals("calc(1.25 / 0.875)", card.get("--ash-stat-card-label-leading"));
        assertEquals("calc(2.25 / 1.875)", card.get("--ash-stat-card-value-leading"), "the ratio is in a property so a minifier cannot fold it");
        assertEquals(Map.of("--ash-stat-card-bg", "var(--color-slate-900, oklch(20.8% 0.042 265.755))", "--ash-stat-card-border", "var(--color-slate-700, oklch(37.2% 0.044 257.287))",
                "--ash-stat-card-label", "var(--color-slate-400, oklch(70.4% 0.04 256.788))", "--ash-stat-card-value", "var(--color-slate-100, oklch(96.8% 0.007 247.896))",
                "--ash-stat-card-description", "var(--color-slate-400, oklch(70.4% 0.04 256.788))"), declarations("stat-card", ".ash-stat-card" + DARK));
        assertEquals("0", declarations("stat-card", ".ash-stat-card__list").get("margin"), "a <dl> has a browser margin until it is stated away");
        Map<String, String> values = declarations("stat-card", ".ash-stat-card__values");
        assertEquals("0", values.get("margin"), "a <dd> is indented by the browser until it is stated away");
        assertEquals("var(--ash-space-2)", values.get("margin-top"));
        assertEquals(Map.of("display", "flex", "flex-wrap", "wrap", "align-items", "baseline", "column-gap", "var(--ash-space-2)", "row-gap", "0.25rem",
                "margin", "0", "margin-top", "var(--ash-space-2)"), values);
        Map<String, String> description = declarations("stat-card", ".ash-stat-card__description");
        assertEquals("0", description.get("margin"));
        assertEquals("0.25rem", description.get("margin-top"));
        assertEquals("break-word", description.get("overflow-wrap"));
    }

    @Test
    void statCardsLabelValueChangeAndDescriptionHaveTheirTailwindTypography() throws IOException {
        assertEquals(Map.of("font-size", "0.875rem", "line-height", "var(--ash-stat-card-label-leading)", "font-weight", "500", "color", "var(--ash-stat-card-label)"),
                declarations("stat-card", ".ash-stat-card__label"));
        assertEquals(Map.of("overflow-wrap", "break-word", "font-size", "1.875rem", "line-height", "var(--ash-stat-card-value-leading)", "font-weight", "600",
                "letter-spacing", "-0.025em", "color", "var(--ash-stat-card-value)"), declarations("stat-card", ".ash-stat-card__value"));
        assertEquals(Map.of("flex-shrink", "0"), declarations("stat-card", ".ash-stat-card__change"), "the change never shrinks");
        assertEquals(Map.of("margin", "0", "margin-top", "0.25rem", "overflow-wrap", "break-word", "font-size", "0.875rem",
                "line-height", "var(--ash-stat-card-label-leading)", "color", "var(--ash-stat-card-description)"), declarations("stat-card", ".ash-stat-card__description"));
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
        assertFalse(tokens.contains("--ash-radius-xl"), "the radius is read locally as var(--radius-xl, 0.75rem), not added to the shared tokens");
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }
}
