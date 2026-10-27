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
 * Pins the Spinner's source contract: the template keeps its two parameters, its structure and its accessibility
 * semantics and carries only Ashlar's own classes (a Tailwind utility sneaking back in would make the component depend
 * on Tailwind again), and its stylesheet is scoped to {@code ash-spinner} classes, declares the four customisation
 * properties with palette fallbacks, owns its own keyframes under a name that cannot collide with Tailwind's, and
 * removes the animation for reduced motion. The rendered result is measured in a real browser by the showcase's
 * {@code SpinnerParityTest}; this reads the sources only.
 */
class SpinnerContractTest {

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
        template = Files.readString(module.resolve("src/main/jte/ashlar/components/spinner.jte"), StandardCharsets.UTF_8).replace("\r\n", "\n");
        css = Files.readString(module.resolve("src/main/css/components/spinner.css"), StandardCharsets.UTF_8);
        rules = CssScanner.flatRules(css);
    }

    private static String withoutJteComments(String source) {
        return source.replaceAll("(?s)<%--.*?--%>", "");
    }

    // ---- the template ----

    @Test
    void theTwoParametersAndTheirDefaultsAreUnchanged() {
        List<String> declared = new ArrayList<>();
        Matcher m = Pattern.compile("(?m)^@(?:param|import) .*$").matcher(template);
        while (m.find()) {
            declared.add(m.group());
        }
        assertEquals(List.of("@import dev.redspartanlabs.ashlar.spinner.SpinnerSize", "@import dev.redspartanlabs.ashlar.spinner.SpinnerStyles",
                "@param String size = \"Medium\"", "@param String label = \"Loading\""), declared);
    }

    @Test
    void theStructureAndTheAccessibilitySemanticsArePreserved() {
        String markup = withoutJteComments(template);
        assertTrue(markup.contains("<span class=\"ash-spinner\" role=\"${roleAttr}\" aria-live=\"${ariaLiveAttr}\" data-spinner data-spinner-size=\"${resolvedSize.name().toLowerCase()}\">"));
        assertTrue(markup.contains("<span aria-hidden=\"true\" class=\"${SpinnerStyles.classes(resolvedSize)}\"></span>"), "the ring is always hidden from assistive technology");
        assertTrue(markup.contains("@if (hasLabel)\n        <span class=\"ash-sr-only\">${label}</span>\n    @endif"), "the label is the Ashlar screen-reader-only utility, only when there is one");
        assertTrue(markup.contains("boolean hasLabel = !label.isEmpty();"));
        assertTrue(markup.contains("String roleAttr = hasLabel ? \"status\" : null;"));
        assertTrue(markup.contains("String ariaLiveAttr = hasLabel ? \"polite\" : null;"));
        assertTrue(markup.contains("SpinnerSize resolvedSize = SpinnerSize.from(size);"));
        assertEquals(3, count(markup, "<span"), "the wrapper, the ring and the label");
    }

    @Test
    void noTailwindUtilityRemainsInTheTemplate() {
        String markup = withoutJteComments(template);
        // The lookbehind excludes a hyphen, so the Ashlar utility ash-sr-only does not count as sr-only.
        for (String utility : List.of("inline-flex", "items-center", "sr-only", "animate-", "motion-reduce", "border-", "rounded", "h-4", "w-4", "inline-block", "shrink-0")) {
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
    void everySelectorIsScopedToASpinnerClassAndNothingIsGlobal() {
        assertFalse(rules.isEmpty());
        for (CssScanner.Rule rule : rules) {
            for (String selector : CssScanner.selectors(rule.selector())) {
                assertTrue(selector.startsWith(".ash-spinner"), "not scoped to a Spinner class: " + selector);
            }
        }
        assertFalse(css.contains("!important"));
        assertFalse(css.contains("@import") || css.contains("@layer"), "the entry file assigns the layer, never the module");
        assertFalse(css.contains("var(--ash-radius") || css.contains("var(--ash-color"), "the spinner reads the same palette variables it always did");
    }

    @Test
    void theCustomisationPropertiesDefaultToTheTailwindPaletteWithItsExactFallbacksAndFlipInTheDarkTheme() {
        Map<String, String> light = declarations(".ash-spinner");
        assertEquals("var(--color-slate-200, oklch(92.9% 0.013 255.508))", light.get("--ash-spinner-track"));
        assertEquals("var(--color-blue-600, oklch(54.6% 0.245 262.881))", light.get("--ash-spinner-accent"));
        assertEquals("inline-flex", light.get("display"));
        assertEquals("center", light.get("align-items"));
        Map<String, String> dark = declarations(".ash-spinner:where([data-theme=\"dark\"], [data-theme=\"dark\"] *)");
        assertEquals(Map.of(
                "--ash-spinner-track", "var(--color-slate-700, oklch(37.2% 0.044 257.287))",
                "--ash-spinner-accent", "var(--color-blue-500, oklch(62.3% 0.214 259.815))"), dark);
    }

    @Test
    void theRingStatesWhatPreflightUsedToGiveItAndOwnsItsGeometryAndMotion() {
        Map<String, String> ring = declarations(".ash-spinner__ring");
        assertEquals("1.5rem", ring.get("--ash-spinner-size"));
        assertEquals("2px", ring.get("--ash-spinner-width"));
        assertEquals("inline-block", ring.get("display"));
        assertEquals("0", ring.get("flex-shrink"));
        assertEquals("var(--ash-spinner-size)", ring.get("width"));
        assertEquals("var(--ash-spinner-size)", ring.get("height"));
        assertEquals("var(--ash-spinner-width) solid var(--ash-spinner-track)", ring.get("border"), "a border is not drawn without a style, and preflight is gone");
        assertEquals("var(--ash-spinner-accent)", ring.get("border-top-color"));
        assertEquals("calc(infinity * 1px)", ring.get("border-radius"), "what Tailwind's rounded-full is");
        assertEquals("ash-spinner-rotate 1s linear infinite", ring.get("animation"));
    }

    @Test
    void theFourSizeModifiersSetOnlyTheSizeAndTheThicknessToTheApprovedValues() {
        assertEquals(Map.of("--ash-spinner-size", "1rem", "--ash-spinner-width", "2px"), declarations(".ash-spinner__ring--small"));
        assertEquals(Map.of("--ash-spinner-size", "1.5rem", "--ash-spinner-width", "2px"), declarations(".ash-spinner__ring--medium"));
        assertEquals(Map.of("--ash-spinner-size", "2rem", "--ash-spinner-width", "3px"), declarations(".ash-spinner__ring--large"));
        assertEquals(Map.of("--ash-spinner-size", "3rem", "--ash-spinner-width", "4px"), declarations(".ash-spinner__ring--xlarge"));
    }

    @Test
    void theKeyframesAreAshlarsOwnAndCannotCollideWithTailwindsSpin() {
        Matcher m = Pattern.compile("@keyframes\\s+([A-Za-z0-9_-]+)\\s*\\{").matcher(css);
        List<String> names = new ArrayList<>();
        while (m.find()) {
            names.add(m.group(1));
        }
        assertEquals(List.of("ash-spinner-rotate"), names);
        assertTrue(Pattern.compile("@keyframes ash-spinner-rotate\\s*\\{\\s*to\\s*\\{\\s*transform:\\s*rotate\\(360deg\\);\\s*\\}\\s*\\}").matcher(css).find(), "one full turn");
    }

    @Test
    void reducedMotionRemovesTheAnimationAndTouchesNothingElse() {
        List<CssScanner.Rule> conditional = rules.stream().filter(r -> !r.media().isEmpty()).toList();
        assertEquals(1, conditional.size());
        assertEquals("@media (prefers-reduced-motion: reduce)", conditional.get(0).media());
        assertEquals(".ash-spinner__ring", conditional.get(0).selector());
        assertEquals(Map.of("animation", "none"), CssScanner.declarations(conditional.get(0).block()));
        int base = css.indexOf(".ash-spinner__ring {");
        int media = css.indexOf("@media (prefers-reduced-motion: reduce)");
        assertTrue(base >= 0 && media > base, "equal specificity, so the reduced-motion rule has to come later to win");
    }

    // ---- placement ----

    @Test
    void theEntryFileImportsSpinnerAfterSkeletonInTheComponentsLayerAndIconStaysFirst() throws IOException {
        String source = Files.readString(module.resolve("src/main/css/ashlar.css"), StandardCharsets.UTF_8);
        List<String> imports = new ArrayList<>();
        Matcher m = Pattern.compile("(?m)^@import \"(components/[a-z-]+\\.css)\" layer\\(ash\\.components\\);$").matcher(source);
        while (m.find()) {
            imports.add(m.group(1));
        }
        assertEquals("components/icon.css", imports.get(0));
        assertTrue(imports.indexOf("components/spinner.css") > imports.indexOf("components/skeleton.css") && imports.indexOf("components/skeleton.css") > imports.indexOf("components/progress.css"));
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }
}
