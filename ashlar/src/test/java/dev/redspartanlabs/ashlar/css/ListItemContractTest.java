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
 * Pins the List Item's source contract: the template keeps its five parameters and its structure (an {@code <li>}, a link row only when
 * there is an {@code href}, a plain row otherwise, decorative icon and chevron wrappers, the chevron on link rows only, the Icon sized by
 * Ashlar's own Icon modifiers) and carries only Ashlar's classes; the stylesheet is scoped to {@code ash-list-item}, keeps the palette
 * fallbacks exact, gives a link row its inherited colour, hover background and inset focus ring, and leaves the separators to the List.
 * The rendered result is measured in a real browser by the showcase's {@code ListItemParityTest}; this reads the sources only.
 */
class ListItemContractTest {

    private static String template;
    private static String css;
    private static List<CssScanner.Rule> rules;

    @BeforeAll
    static void load() throws IOException {
        String generated = System.getProperty("ashlar.css.generated");
        assertNotNull(generated, "ashlar.css.generated must be set by the Gradle test task");
        Path module = null;
        for (Path p = Path.of(generated).toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("src/main/css/ashlar.css"))) {
                module = p;
                break;
            }
        }
        assertNotNull(module, "could not find the ashlar module above " + generated);
        template = Files.readString(module.resolve("src/main/jte/ashlar/components/list-item.jte"), StandardCharsets.UTF_8).replace("\r\n", "\n");
        css = Files.readString(module.resolve("src/main/css/components/list-item.css"), StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "");
        rules = CssScanner.flatRules(css);
    }

    private static String withoutJteComments(String source) {
        return source.replaceAll("(?s)<%--.*?--%>", "");
    }

    // ---- the template ----

    @Test
    void theFiveParametersAndTheirDefaultsAreUnchanged() {
        List<String> declared = new ArrayList<>();
        Matcher m = Pattern.compile("(?m)^@(?:param|import) .*$").matcher(template);
        while (m.find()) {
            declared.add(m.group());
        }
        assertEquals(List.of("@import dev.redspartanlabs.ashlar.icon.Icon", "@param String label", "@param String description = \"\"",
                "@param String href = null", "@param Icon icon = null", "@param gg.jte.Content trailing = null"), declared);
    }

    @Test
    void theRowIsALinkOnlyWhenThereIsAnHrefAndAPlainDivOtherwiseInsideAnLi() {
        String markup = withoutJteComments(template);
        assertTrue(markup.contains("boolean isLink = href != null && !href.isEmpty();"));
        assertTrue(markup.contains("<li class=\"ash-list-item\">"));
        assertEquals(1, count(markup, "<li"));
        int link = markup.indexOf("@if (isLink)");
        int plain = markup.indexOf("@else");
        assertTrue(link >= 0 && plain > link);
        String linkBranch = markup.substring(link, plain);
        String plainBranch = markup.substring(plain);
        assertEquals(1, count(linkBranch, "<a href=\"${href}\" class=\"ash-list-item__row ash-list-item__row--link\">"));
        assertEquals(1, count(plainBranch, "<div class=\"ash-list-item__row\">"));
        assertFalse(plainBranch.contains("<a ") || plainBranch.contains("href"), "a missing href means plain content, not a dead link");
    }

    @Test
    void theChevronIsDecorativeAndOnlyOnTheLinkRowAndTheLeadingIconIsDecorativeInBoth() {
        String markup = withoutJteComments(template);
        int plain = markup.indexOf("@else");
        String linkBranch = markup.substring(0, plain);
        String plainBranch = markup.substring(plain);
        assertEquals(1, count(linkBranch, "<span class=\"ash-list-item__chevron\" aria-hidden=\"true\">"));
        assertEquals(0, count(plainBranch, "ash-list-item__chevron"));
        assertEquals(1, count(linkBranch, "<span class=\"ash-list-item__icon\" aria-hidden=\"true\">"));
        assertEquals(1, count(plainBranch, "<span class=\"ash-list-item__icon\" aria-hidden=\"true\">"));
        assertEquals(3, count(markup, "aria-hidden=\"true\""), "two leading-icon wrappers and one chevron wrapper, nothing else is hidden");
        assertEquals(0, count(markup, "role=") + count(markup, "aria-label"), "the row's meaning comes from its text");
    }

    @Test
    void theIconIsSizedByAshlarsIconModifiersNotTailwindSizes() {
        String markup = withoutJteComments(template);
        assertEquals(2, count(markup, "@template.ashlar.components.icon(icon = icon, classes = \"ash-icon--md\")"));
        assertEquals(1, count(markup, "@template.ashlar.components.icon(icon = Icon.CHEVRON_RIGHT, classes = \"ash-icon--sm\")"));
    }

    @Test
    void theLabelDescriptionAndTrailingContentAreProjectedInBothRows() {
        String markup = withoutJteComments(template);
        assertEquals(2, count(markup, "<span class=\"ash-list-item__label\">${label}</span>"));
        assertEquals(2, count(markup, "<span class=\"ash-list-item__description\">${description}</span>"));
        assertEquals(2, count(markup, "<span class=\"ash-list-item__trailing\">"));
        assertEquals(2, count(markup, "${trailing}"));
        assertEquals(2, count(markup, "<span class=\"ash-list-item__body\">"));
    }

    @Test
    void noTailwindUtilityRemainsInTheTemplate() {
        String markup = withoutJteComments(template);
        for (String utility : List.of("flex", "items-center", "gap-", "px-", "py-", "text-sm", "text-slate", "font-medium", "shrink-0", "min-w-0",
                "block", "hover:", "focus:", "focus-visible:", "ring-", "dark:", "h-5", "w-5", "h-4", "w-4")) {
            assertFalse(Pattern.compile("(?<![\\w-])" + Pattern.quote(utility)).matcher(markup).find(), "a Tailwind utility is back: " + utility);
        }
    }

    // ---- the stylesheet ----

    private static String block(String selector) {
        List<CssScanner.Rule> found = rules.stream().filter(r -> r.selector().equals(selector)).toList();
        assertEquals(1, found.size(), selector + " must be defined exactly once");
        return found.get(0).block();
    }

    private static Map<String, String> declarations(String selector) {
        return CssScanner.declarations(block(selector));
    }

    @Test
    void everySelectorIsScopedToAListItemClassAndNothingIsGlobalOrConditional() {
        assertFalse(rules.isEmpty());
        for (CssScanner.Rule rule : rules) {
            assertEquals("", rule.media(), "no media conditions: " + rule.selector());
            for (String selector : CssScanner.selectors(rule.selector())) {
                assertTrue(selector.startsWith(".ash-list-item"), "not scoped to a List Item class: " + selector);
            }
        }
        assertFalse(css.contains("!important"));
        assertFalse(css.contains("@import") || css.contains("@layer") || css.contains("@keyframes"));
        assertFalse(css.contains(".ash-list ") || css.contains(".ash-list>") || css.contains("border-bottom"), "the separators are the List's");
    }

    @Test
    void theColourPropertiesDefaultToTheTailwindPaletteWithItsExactFallbacksAndFlipInTheDarkTheme() {
        Map<String, String> light = declarations(".ash-list-item");
        assertEquals("calc(1.25 / 0.875)", light.get("--ash-list-item-leading"));
        assertEquals("var(--color-slate-900, oklch(20.8% 0.042 265.755))", light.get("--ash-list-item-label"));
        assertEquals("var(--color-slate-500, oklch(55.4% 0.046 257.417))", light.get("--ash-list-item-description"));
        assertEquals("var(--color-slate-400, oklch(70.4% 0.04 256.788))", light.get("--ash-list-item-icon"));
        assertEquals("var(--color-slate-500, oklch(55.4% 0.046 257.417))", light.get("--ash-list-item-trailing"));
        assertEquals("var(--color-slate-50, oklch(98.4% 0.003 247.858))", light.get("--ash-list-item-hover"));
        assertEquals("var(--color-blue-500, oklch(62.3% 0.214 259.815))", light.get("--ash-list-item-focus"));
        assertEquals(Map.of(
                "--ash-list-item-label", "var(--color-slate-100, oklch(96.8% 0.007 247.896))",
                "--ash-list-item-description", "var(--color-slate-400, oklch(70.4% 0.04 256.788))",
                "--ash-list-item-icon", "var(--color-slate-500, oklch(55.4% 0.046 257.417))",
                "--ash-list-item-trailing", "var(--color-slate-400, oklch(70.4% 0.04 256.788))",
                "--ash-list-item-hover", "color-mix(in oklab, var(--color-slate-800, oklch(27.9% 0.041 260.031)) 50%, transparent)"),
                declarations(".ash-list-item:where([data-theme=\"dark\"], [data-theme=\"dark\"] *)"), "the focus ring is blue-500 in BOTH themes");
    }

    @Test
    void theRowStatesItsLayoutSpacingAndTypeWithTheSpacingTokens() {
        assertEquals(Map.of(
                "display", "flex",
                "align-items", "center",
                "gap", "var(--ash-space-3)",
                "padding-inline", "var(--ash-space-4)",
                "padding-block", "var(--ash-space-3)",
                "font-size", "0.875rem",
                "line-height", "var(--ash-list-item-leading)"), declarations(".ash-list-item__row"));
    }

    @Test
    void aLinkRowInheritsItsColourHasNoUnderlineAndOwnsItsHoverAndInsetFocusRing() {
        assertEquals(Map.of("color", "inherit", "text-decoration", "none"), declarations(".ash-list-item__row--link"));
        assertEquals(Map.of("background-color", "var(--ash-list-item-hover)"), declarations(".ash-list-item__row--link:hover"));
        assertEquals(Map.of("outline-style", "none"), declarations(".ash-list-item__row--link:focus"));
        assertEquals(Map.of("box-shadow", "inset 0 0 0 2px var(--ash-list-item-focus)"), declarations(".ash-list-item__row--link:focus-visible"),
                "an inset box-shadow, not the outline of .ash-focus-ring, and blue-500 in both themes, not --ash-color-focus");
        assertFalse(css.contains("ash-focus-ring") || css.contains("--ash-color-focus"));
    }

    @Test
    void thePartsKeepTheirShrinkWrapAndColourBehaviour() {
        assertEquals(Map.of("flex-shrink", "0", "color", "var(--ash-list-item-icon)"), declarations(".ash-list-item__icon"));
        assertEquals(Map.of("min-width", "0", "flex", "1"), declarations(".ash-list-item__body"));
        assertEquals(Map.of("display", "block", "font-weight", "500", "color", "var(--ash-list-item-label)"), declarations(".ash-list-item__label"));
        assertEquals(Map.of("display", "block", "color", "var(--ash-list-item-description)"), declarations(".ash-list-item__description"));
        assertEquals(Map.of("flex-shrink", "0", "color", "var(--ash-list-item-trailing)"), declarations(".ash-list-item__trailing"));
        assertEquals(Map.of("flex-shrink", "0", "color", "var(--ash-list-item-icon)"), declarations(".ash-list-item__chevron"));
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }
}
