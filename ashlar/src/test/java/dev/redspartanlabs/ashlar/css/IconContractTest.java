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
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the Icon's source contract: the template keeps its parameters (only the default of {@code classes} changed, and
 * it now adds to Ashlar's own class instead of replacing a default size), emits {@code ash-icon} and no Tailwind
 * utility of its own, and keeps its accessibility semantics; the stylesheet is scoped to {@code ash-icon}, gives the
 * default size through one custom property, keeps a zero-specificity base so every other size rule wins, and holds the
 * five size modifiers. The rendered result is measured in a real browser by the showcase's {@code IconParityTest}; this
 * reads the sources only.
 */
class IconContractTest {

    private static String template;
    private static String css;
    private static List<CssScanner.Rule> rules;
    private static Path module;

    @BeforeAll
    static void load() throws IOException {
        String generated = System.getProperty("ashlar.css.generated");
        assertNotNull(generated, "ashlar.css.generated must be set by the Gradle test task");
        for (Path p = Path.of(generated).toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("src/main/css/ashlar.css"))) {
                module = p;
                break;
            }
        }
        assertNotNull(module, "could not find the ashlar module above " + generated);
        template = Files.readString(module.resolve("src/main/jte/ashlar/components/icon.jte"), StandardCharsets.UTF_8).replace("\r\n", "\n");
        css = Files.readString(module.resolve("src/main/css/components/icon.css"), StandardCharsets.UTF_8);
        rules = CssScanner.flatRules(css);
    }

    private static String withoutJteComments(String source) {
        return source.replaceAll("(?s)<%--.*?--%>", "");
    }

    // ---- the template ----

    @Test
    void theParametersAreUnchangedExceptThatClassesNowDefaultsToNothingAndIsAdded() {
        List<String> declared = new ArrayList<>();
        Matcher m = Pattern.compile("(?m)^@(?:param|import) .*$").matcher(template);
        while (m.find()) {
            declared.add(m.group());
        }
        assertEquals(List.of("@import dev.redspartanlabs.ashlar.icon.Icon", "@import dev.redspartanlabs.ashlar.icon.IconSvg",
                "@param Icon icon", "@param String classes = \"\"", "@param String label = \"\""), declared);
    }

    @Test
    void theClassAttributeIsAshIconPlusTheCallersClassesAndNothingElse() {
        String markup = withoutJteComments(template);
        List<String> classAttributes = new ArrayList<>();
        Matcher m = Pattern.compile("class=\"([^\"]*)\"").matcher(markup);
        while (m.find()) {
            classAttributes.add(m.group(1));
        }
        assertEquals(List.of("${iconClasses}"), classAttributes, "the svg has exactly one class attribute, built in one place");
        assertTrue(markup.contains("String iconClasses = (classes == null || classes.isBlank()) ? \"ash-icon\" : \"ash-icon \" + classes.trim();"),
                "ash-icon alone for no classes, otherwise ash-icon, one space and the trimmed classes (never a trailing space)");
    }

    @Test
    void noTailwindUtilityRemainsInTheTemplateOrItsDefault() {
        String markup = withoutJteComments(template);
        for (String utility : List.of("inline-block", "shrink-0", "h-5", "w-5", "h-4", "w-4", "flex", "block", "hidden")) {
            assertFalse(Pattern.compile("(?<![\\w-])" + Pattern.quote(utility) + "(?![\\w-])").matcher(markup).find(), "a Tailwind utility is back: " + utility);
        }
    }

    @Test
    void theSvgAndItsAccessibilitySemanticsArePreserved() {
        String markup = withoutJteComments(template);
        for (String expected : List.of(
                "viewBox=\"0 0 24 24\"", "fill=\"none\"", "stroke=\"currentColor\"", "stroke-width=\"1.5\"",
                "stroke-linecap=\"round\"", "stroke-linejoin=\"round\"",
                "aria-hidden=\"${ariaHiddenAttr}\"", "role=\"${roleAttr}\"", "aria-label=\"${labelAttr}\"",
                "$unsafe{IconSvg.bodyFor(icon)}</svg>")) {
            assertTrue(markup.contains(expected), expected);
        }
        assertTrue(markup.contains("String ariaHiddenAttr = label.isEmpty() ? \"true\" : null;"), "decorative unless labelled");
        assertTrue(markup.contains("String roleAttr = label.isEmpty() ? null : \"img\";"));
        assertTrue(markup.contains("String labelAttr = label.isEmpty() ? null : label;"));
        assertEquals(1, count(markup, "<svg"));
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
    void everySelectorIsScopedToAshIconAndNothingIsGlobal() {
        assertFalse(rules.isEmpty());
        for (CssScanner.Rule rule : rules) {
            assertEquals("", rule.media(), "no media conditions: " + rule.selector());
            for (String selector : CssScanner.selectors(rule.selector())) {
                assertTrue(selector.matches("^(:where\\(\\.ash-icon(\\[hidden\\])?\\)|\\.ash-icon--(xs|sm|md|lg|xl))$"), "not an Icon selector: " + selector);
            }
        }
        assertFalse(css.contains("!important"));
        assertFalse(css.contains("@import") || css.contains("@layer"), "the entry file assigns the layer, never the module");
        assertFalse(css.contains("var(--color-"), "the icon states no colour, so it needs no palette variable");
    }

    @Test
    void theBaseRuleHasNoSpecificityAndOwnsTheDefaultSizeAndTheInlineBehaviour() {
        Map<String, String> base = declarations(":where(.ash-icon)");
        assertEquals(Map.of(
                "--ash-icon-size", "1.25rem",
                "display", "inline-block",
                "flex-shrink", "0",
                "width", "var(--ash-icon-size)",
                "height", "var(--ash-icon-size)",
                "vertical-align", "middle"), base);
    }

    @Test
    void theHiddenAttributeStillHidesTheIconAndComesAfterTheBase() {
        assertEquals(Map.of("display", "none"), declarations(":where(.ash-icon[hidden])"));
        List<String> order = rules.stream().map(CssScanner.Rule::selector).toList();
        assertTrue(order.indexOf(":where(.ash-icon[hidden])") > order.indexOf(":where(.ash-icon)"),
                "equal specificity, so the hidden rule has to come later to win");
    }

    @Test
    void theFiveModifiersSetOnlyTheSizePropertyToTheApprovedValues() {
        Map<String, String> sizes = new TreeMap<>(Map.of(
                "xs", "0.75rem", "sm", "1rem", "md", "1.25rem", "lg", "1.5rem", "xl", "2rem"));
        for (Map.Entry<String, String> e : sizes.entrySet()) {
            assertEquals(Map.of("--ash-icon-size", e.getValue()), declarations(".ash-icon--" + e.getKey()), e.getKey());
        }
        assertEquals(sizes.get("md"), declarations(":where(.ash-icon)").get("--ash-icon-size"), "md states the default");
        assertEquals(1 + 1 + 5, rules.size(), "the base, the hidden rule and five modifiers, nothing else");
    }

    // ---- placement ----

    @Test
    void theEntryFileImportsIconFirstInTheComponentsLayer() throws IOException {
        String source = Files.readString(module.resolve("src/main/css/ashlar.css"), StandardCharsets.UTF_8);
        List<String> componentImports = new ArrayList<>();
        Matcher m = Pattern.compile("(?m)^@import \"(components/[a-z-]+\\.css)\" layer\\(ash\\.components\\);$").matcher(source);
        while (m.find()) {
            componentImports.add(m.group(1));
        }
        assertEquals("components/icon.css", componentImports.get(0), "Icon comes first so every component that composes an icon follows it");
        assertEquals(8, componentImports.size());
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }
}
