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
 * Pins the Skeleton's source contract: the template keeps its three parameters, its inline width and height, its
 * always-hidden semantics and its data hooks, and carries no Tailwind of its own; its stylesheet is scoped to
 * {@code ash-skeleton} classes, keeps the literal 0.25rem radius (never the skin-driven radius token), reads the slate
 * palette with exact fallbacks, owns its keyframes under a name that cannot collide with Tailwind's, and removes the
 * animation for reduced motion. The rendered result is measured in a real browser by the showcase's
 * {@code SkeletonParityTest}; this reads the sources only.
 */
class SkeletonContractTest {

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
        template = Files.readString(module.resolve("src/main/jte/ashlar/components/skeleton.jte"), StandardCharsets.UTF_8).replace("\r\n", "\n");
        css = Files.readString(module.resolve("src/main/css/components/skeleton.css"), StandardCharsets.UTF_8);
        rules = CssScanner.flatRules(css);
    }

    private static String withoutJteComments(String source) {
        return source.replaceAll("(?s)<%--.*?--%>", "");
    }

    // ---- the template ----

    @Test
    void theThreeParametersAndTheirDefaultsAreUnchanged() {
        List<String> declared = new ArrayList<>();
        Matcher m = Pattern.compile("(?m)^@(?:param|import) .*$").matcher(template);
        while (m.find()) {
            declared.add(m.group());
        }
        assertEquals(List.of("@import dev.redspartanlabs.ashlar.skeleton.SkeletonShape", "@import dev.redspartanlabs.ashlar.skeleton.SkeletonStyles",
                "@param String width = \"100%\"", "@param String height = \"1rem\"", "@param String shape = \"Rectangle\""), declared);
    }

    @Test
    void theStructureTheInlineSizeAndTheAlwaysHiddenSemanticsArePreserved() {
        String markup = withoutJteComments(template).replaceAll("\\s+", " ");
        assertTrue(markup.contains("<div aria-hidden=\"true\" class=\"${SkeletonStyles.classes(resolvedShape)}\" style=\"width: ${width}; height: ${height}\" data-skeleton data-skeleton-shape=\"${resolvedShape.name().toLowerCase()}\" ></div>"), markup);
        assertTrue(markup.contains("SkeletonShape resolvedShape = SkeletonShape.from(shape);"));
        assertEquals(1, count(markup, "<div"));
        assertFalse(markup.contains("role="), "a skeleton never announces itself");
        assertFalse(markup.contains("aria-busy"));
    }

    @Test
    void noTailwindUtilityRemainsInTheTemplate() {
        String markup = withoutJteComments(template);
        for (String utility : List.of("block", "shrink-0", "animate-", "bg-slate", "motion-reduce", "rounded", "dark:")) {
            assertFalse(Pattern.compile("(?<![\\w-])" + Pattern.quote(utility)).matcher(markup).find(), "a Tailwind utility is back: " + utility);
        }
    }

    // ---- the stylesheet ----

    private static String block(String selector) {
        List<CssScanner.Rule> found = rules.stream().filter(r -> r.media().isEmpty() && r.selector().equals(selector)).toList();
        assertEquals(1, found.size(), selector + " must be defined exactly once, unconditionally");
        return found.get(0).block();
    }

    private static Map<String, String> declarations(String selector) {
        return CssScanner.declarations(block(selector));
    }

    @Test
    void everySelectorIsScopedToASkeletonClassAndNothingIsGlobal() {
        assertFalse(rules.isEmpty());
        for (CssScanner.Rule rule : rules) {
            for (String selector : CssScanner.selectors(rule.selector())) {
                assertTrue(selector.startsWith(".ash-skeleton"), "not scoped to a Skeleton class: " + selector);
            }
        }
        assertFalse(css.contains("!important"));
        assertFalse(css.contains("@import") || css.contains("@layer"), "the entry file assigns the layer, never the module");
    }

    @Test
    void theRadiusIsTheLiteralQuarterRemAndNeverTheSkinDrivenToken() {
        assertEquals("0.25rem", declarations(".ash-skeleton").get("border-radius"));
        assertEquals("calc(infinity * 1px)", declarations(".ash-skeleton--rounded").get("border-radius"), "what Tailwind's rounded-full is");
        assertFalse(css.contains("var(--ash-radius") || css.contains("var(--radius"), "a skin-driven radius would change the placeholder across skins");
    }

    @Test
    void theBaseRuleStatesWhatPreflightAndTheUtilitiesGaveAndOwnsItsMotion() {
        assertEquals(Map.of(
                "--ash-skeleton-bg", "var(--color-slate-200, oklch(92.9% 0.013 255.508))",
                "display", "block",
                "flex-shrink", "0",
                "background-color", "var(--ash-skeleton-bg)",
                "border-radius", "0.25rem",
                "animation", "ash-skeleton-pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite"), declarations(".ash-skeleton"));
        assertEquals(Map.of("--ash-skeleton-bg", "var(--color-slate-700, oklch(37.2% 0.044 257.287))"),
                declarations(".ash-skeleton:where([data-theme=\"dark\"], [data-theme=\"dark\"] *)"));
        assertFalse(declarations(".ash-skeleton").containsKey("width") || declarations(".ash-skeleton").containsKey("height"), "the size is the template's inline style");
    }

    @Test
    void theKeyframesAreAshlarsOwnAndCannotCollideWithTailwindsPulse() {
        Matcher m = Pattern.compile("@keyframes\\s+([A-Za-z0-9_-]+)\\s*\\{").matcher(css);
        List<String> names = new ArrayList<>();
        while (m.find()) {
            names.add(m.group(1));
        }
        assertEquals(List.of("ash-skeleton-pulse"), names);
        assertTrue(Pattern.compile("@keyframes ash-skeleton-pulse\\s*\\{\\s*50%\\s*\\{\\s*opacity:\\s*0\\.5;\\s*\\}\\s*\\}").matcher(css).find(), "down to half opacity in the middle");
    }

    @Test
    void reducedMotionRemovesTheAnimationAndTouchesNothingElse() {
        List<CssScanner.Rule> conditional = rules.stream().filter(r -> !r.media().isEmpty()).toList();
        assertEquals(1, conditional.size());
        assertEquals("@media (prefers-reduced-motion: reduce)", conditional.get(0).media());
        assertEquals(".ash-skeleton", conditional.get(0).selector());
        assertEquals(Map.of("animation", "none"), CssScanner.declarations(conditional.get(0).block()));
        assertTrue(css.indexOf("@media (prefers-reduced-motion: reduce)") > css.indexOf(".ash-skeleton {"), "equal specificity, so the reduced-motion rule has to come later to win");
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }
}
