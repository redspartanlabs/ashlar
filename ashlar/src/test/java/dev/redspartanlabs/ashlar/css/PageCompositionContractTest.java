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
 * Pins the source contract of the four page composition utilities migrated together: Page Shell, Page Header, Page Section and Footer.
 * Each template keeps its parameters, its native elements and its heading levels (a {@code <div>}, a {@code <header>} with an
 * {@code <h1>}, a {@code <section>} with an {@code <h2>}, and a {@code <footer>}), adds no role, no label, no hook and no escape hatch,
 * and carries only Ashlar's classes; each stylesheet is scoped to its own classes. The rendered result is measured in a real browser by
 * the showcase's {@code PageCompositionParityTest}; this reads the sources only.
 */
class PageCompositionContractTest {

    private static final List<String> NAMES = List.of("page-shell", "page-header", "page-section", "footer");
    private static final String SM = "@media (min-width: 40rem)";
    private static final String LG = "@media (min-width: 64rem)";
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
        return Files.readString(module.resolve("src/main/jte/ashlar/utilities/" + name + ".jte"), StandardCharsets.UTF_8).replace("\r\n", "\n");
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

    private static Map<String, String> declarations(String cssName, String selector, String media) throws IOException {
        List<CssScanner.Rule> found = rules(cssName).stream().filter(r -> r.selector().equals(selector) && r.media().equals(media)).toList();
        assertEquals(1, found.size(), selector + " [" + media + "] must be defined exactly once in " + cssName);
        return CssScanner.declarations(found.get(0).block());
    }

    // ---- the templates ----

    @Test
    void theParametersAndTheirDefaultsAreUnchanged() throws IOException {
        assertEquals(List.of("@param gg.jte.Content content"), declared("page-shell"));
        assertEquals(List.of("@param String title", "@param String description = \"\"", "@param gg.jte.Content actions = null"), declared("page-header"));
        assertEquals(List.of("@param String title", "@param String description = \"\"", "@param gg.jte.Content content"), declared("page-section"));
        assertEquals(List.of("@param gg.jte.Content content"), declared("footer"));
    }

    @Test
    void eachTemplateKeepsItsNativeElementsItsHeadingLevelAndItsOptionalPartsExactly() throws IOException {
        assertEquals("<div class=\"ash-page-shell\"> ${content} </div>", markup("page-shell"));
        assertEquals("<header class=\"ash-page-header\"> <div class=\"ash-page-header__heading\"> <h1 class=\"ash-page-header__title\">${title}</h1>"
                + " @if (hasDescription) <p class=\"ash-page-header__description\">${description}</p> @endif </div>"
                + " @if (hasActions) <div class=\"ash-page-header__actions\"> ${actions} </div> @endif </header>", markup("page-header"));
        assertEquals("<section class=\"ash-page-section\"> <div> <h2 class=\"ash-page-section__title\">${title}</h2>"
                + " @if (hasDescription) <p class=\"ash-page-section__description\">${description}</p> @endif </div> ${content} </section>", markup("page-section"));
        assertEquals("<footer class=\"ash-footer\"> <div class=\"ash-footer__inner\"> ${content} </div> </footer>", markup("footer"));
    }

    @Test
    void headingsAndLandmarksAreNative() throws IOException {
        assertTrue(markup("page-header").contains("<header ") && markup("page-header").contains("<h1 "), "the header renders the page's one h1");
        assertEquals(1, count(markup("page-header"), "<h1"));
        assertEquals(0, count(markup("page-header"), "<h2"));
        assertTrue(markup("page-section").contains("<section ") && markup("page-section").contains("<h2 "), "the section renders an h2 under the page's h1");
        assertEquals(0, count(markup("page-section"), "<h1"));
        assertEquals(1, count(markup("page-section"), "<h2"));
        assertTrue(markup("footer").startsWith("<footer "), "the footer is the native contentinfo landmark");
        assertFalse(markup("page-shell").contains("<main"), "the surrounding layout owns the one main landmark");
        assertFalse(markup("page-shell").contains("<section") || markup("page-shell").contains("<header") || markup("page-shell").contains("<footer"));
    }

    @Test
    void noTemplateAddsARoleALabelAHookOrAnEscapeHatch() throws IOException {
        for (String name : NAMES) {
            String markup = withoutJteComments(template(name));
            for (String forbidden : List.of("role=", "aria-", "data-", "style=", "id=", "tabindex")) {
                assertFalse(markup.contains(forbidden), name + " adds no " + forbidden);
            }
        }
    }

    @Test
    void noTailwindUtilityRemainsInAnyTemplate() throws IOException {
        List<String> utilities = List.of("text-xl", "text-lg", "text-sm", "text-slate", "dark:", "flex", "gap-", "px-", "py-", "mx-auto", "max-w", "w-full",
                "font-semibold", "min-w-0", "mt-1", "border", "items-", "justify-", "sm:", "lg:", "slate-");
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
        assertFalse(stripped.contains("@import") || stripped.contains("@layer") || stripped.contains("@keyframes"), cssName + ": the entry file assigns the layer");
        assertFalse(Pattern.compile("(^|[,{}]\\s*)(\\*|html|body|h1|h2|p|div|header|footer|section|main)\\s*[,{]").matcher(stripped).find(), cssName + ": no bare element selector");
    }

    @Test
    void everySelectorIsScopedToItsOwnClassesAndNothingIsGlobal() throws IOException {
        assertScoped("page-shell", List.of(".ash-page-shell", ".ash-page-shell", ".ash-page-shell"));
        assertScoped("page-header", List.of(".ash-page-header", ".ash-page-header" + DARK, ".ash-page-header__heading", ".ash-page-header__title",
                ".ash-page-header__description", ".ash-page-header__actions", ".ash-page-header"));
        assertScoped("page-section", List.of(".ash-page-section", ".ash-page-section" + DARK, ".ash-page-section__title", ".ash-page-section__description"));
        assertScoped("footer", List.of(".ash-footer", ".ash-footer" + DARK, ".ash-footer__inner"));
    }

    @Test
    void pageShellIsACenteredCappedFlexColumnWithSteppedSidePadding() throws IOException {
        assertEquals(Map.of("display", "flex", "flex-direction", "column", "gap", "var(--ash-space-10)", "width", "100%", "max-width", "72rem",
                        "margin-inline", "auto", "padding-block", "var(--ash-space-8)", "padding-inline", "var(--ash-space-4)"),
                declarations("page-shell", ".ash-page-shell", ""));
        assertEquals(Map.of("padding-inline", "var(--ash-space-6)"), declarations("page-shell", ".ash-page-shell", SM), "sm is 40rem");
        assertEquals(Map.of("padding-inline", "var(--ash-space-8)"), declarations("page-shell", ".ash-page-shell", LG), "lg is 64rem");
    }

    @Test
    void pageHeaderStacksBelowFortyRemAndIsARowFromItWithTheActionsAtTheEnd() throws IOException {
        Map<String, String> header = declarations("page-header", ".ash-page-header", "");
        assertEquals("flex", header.get("display"));
        assertEquals("column", header.get("flex-direction"));
        assertEquals("var(--ash-space-4)", header.get("gap"));
        assertFalse(header.containsKey("align-items") || header.containsKey("justify-content"), "alignment is for the row layout only");
        assertEquals(Map.of("flex-direction", "row", "align-items", "flex-start", "justify-content", "space-between"), declarations("page-header", ".ash-page-header", SM));
        assertEquals(Map.of("min-width", "0"), declarations("page-header", ".ash-page-header__heading", ""), "a long title wraps instead of widening the row");
        assertEquals(Map.of("display", "flex", "flex-wrap", "wrap", "align-items", "center", "gap", "var(--ash-space-3)"),
                declarations("page-header", ".ash-page-header__actions", ""));
    }

    @Test
    void headingsAndDescriptionsOwnTheirMarginsSizesWeightsAndTailwindsExactColours() throws IOException {
        Map<String, String> header = declarations("page-header", ".ash-page-header", "");
        assertEquals("calc(1.75 / 1.25)", header.get("--ash-page-header-title-leading"), "the ratios are in properties so a minifier cannot fold them");
        assertEquals("calc(1.25 / 0.875)", header.get("--ash-page-header-description-leading"));
        assertEquals("var(--color-slate-900, oklch(20.8% 0.042 265.755))", header.get("--ash-page-header-title"));
        assertEquals("var(--color-slate-500, oklch(55.4% 0.046 257.417))", header.get("--ash-page-header-description"));
        assertEquals(Map.of("--ash-page-header-title", "var(--color-slate-100, oklch(96.8% 0.007 247.896))",
                "--ash-page-header-description", "var(--color-slate-400, oklch(70.4% 0.04 256.788))"), declarations("page-header", ".ash-page-header" + DARK, ""));
        Map<String, String> title = declarations("page-header", ".ash-page-header__title", "");
        assertEquals("0", title.get("margin"), "a heading has a browser margin until it is stated away");
        assertEquals("1.25rem", title.get("font-size"));
        assertEquals("600", title.get("font-weight"));
        assertEquals("var(--ash-page-header-title-leading)", title.get("line-height"));
        Map<String, String> description = declarations("page-header", ".ash-page-header__description", "");
        assertEquals("0", description.get("margin"));
        assertEquals("0.25rem", description.get("margin-top"));
        assertEquals("0.875rem", description.get("font-size"));

        Map<String, String> section = declarations("page-section", ".ash-page-section", "");
        assertEquals("calc(1.75 / 1.125)", section.get("--ash-page-section-title-leading"));
        assertEquals("calc(1.25 / 0.875)", section.get("--ash-page-section-description-leading"));
        assertEquals("var(--color-slate-900, oklch(20.8% 0.042 265.755))", section.get("--ash-page-section-title"));
        assertEquals("var(--color-slate-500, oklch(55.4% 0.046 257.417))", section.get("--ash-page-section-description"));
        assertEquals(Map.of("--ash-page-section-title", "var(--color-slate-100, oklch(96.8% 0.007 247.896))",
                "--ash-page-section-description", "var(--color-slate-400, oklch(70.4% 0.04 256.788))"), declarations("page-section", ".ash-page-section" + DARK, ""));
        Map<String, String> sectionTitle = declarations("page-section", ".ash-page-section__title", "");
        assertEquals("0", sectionTitle.get("margin"));
        assertEquals("1.125rem", sectionTitle.get("font-size"));
        assertEquals("600", sectionTitle.get("font-weight"));
        Map<String, String> sectionDescription = declarations("page-section", ".ash-page-section__description", "");
        assertEquals("0.25rem", sectionDescription.get("margin-top"));
        assertEquals("0.875rem", sectionDescription.get("font-size"));
    }

    @Test
    void pageSectionIsAFlexColumnWithTheSpace4GapAndNeverStylesItsContent() throws IOException {
        Map<String, String> section = declarations("page-section", ".ash-page-section", "");
        assertEquals("flex", section.get("display"));
        assertEquals("column", section.get("flex-direction"));
        assertEquals("var(--ash-space-4)", section.get("gap"));
        assertFalse(css("page-section").replaceAll("(?s)/\\*.*?\\*/", "").contains(">"), "no child combinator reaches the caller's content");
        assertFalse(css("page-shell").replaceAll("(?s)/\\*.*?\\*/", "").contains(">"));
    }

    @Test
    void footerIsARuleOnTopWithTheShellsWidthAndPaddingAndQuietSmallText() throws IOException {
        Map<String, String> footer = declarations("footer", ".ash-footer", "");
        assertEquals("0 solid var(--ash-footer-border)", footer.get("border"), "all four sides carry the colour at zero width, as border-color did");
        assertEquals("1px", footer.get("border-top-width"));
        assertEquals("var(--ash-space-8)", footer.get("padding-block"));
        assertEquals("0.875rem", footer.get("font-size"));
        assertEquals("var(--ash-footer-leading)", footer.get("line-height"));
        assertEquals("calc(1.25 / 0.875)", footer.get("--ash-footer-leading"));
        assertEquals("var(--color-slate-200, oklch(92.9% 0.013 255.508))", footer.get("--ash-footer-border"));
        assertEquals("var(--color-slate-500, oklch(55.4% 0.046 257.417))", footer.get("--ash-footer-color"));
        assertEquals("var(--ash-footer-color)", footer.get("color"));
        assertEquals(Map.of("--ash-footer-border", "var(--color-slate-700, oklch(37.2% 0.044 257.287))",
                "--ash-footer-color", "var(--color-slate-400, oklch(70.4% 0.04 256.788))"), declarations("footer", ".ash-footer" + DARK, ""));
        assertEquals(Map.of("max-width", "72rem", "margin-inline", "auto", "padding-inline", "var(--ash-space-4)"), declarations("footer", ".ash-footer__inner", ""));
    }

    // ---- placement ----

    @Test
    void theEntryFileImportsEachInTheComponentsLayer() throws IOException {
        String source = Files.readString(module.resolve("src/main/css/ashlar.css"), StandardCharsets.UTF_8);
        for (String name : NAMES) {
            assertTrue(source.contains("@import \"components/" + name + ".css\" layer(ash.components);"), name);
        }
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }
}
