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
 * Pins the source contract of Button, migrated together with {@code ButtonStyles} and {@code button.js}. The template keeps every parameter, default,
 * native element, ARIA attribute and the data hooks the script and the skins use, carries only Ashlar's classes, and no longer renders the two
 * class-list attributes whose only reader was the script; the script flips attributes, never classes; the stylesheet is scoped to its own classes and
 * states every colour, size and state. The rendered result is measured in a real browser by the showcase's {@code ButtonParityTest},
 * {@code ButtonStateParityTest} and {@code ButtonScriptTest}; this reads the sources only.
 */
class ButtonContractTest {

    private static final String DARK = ":where([data-theme=\"dark\"], [data-theme=\"dark\"] *)";
    private static final String PRESSED = ":is(.ash-btn--pressed, [aria-pressed=\"true\"])";
    private static final List<String> VARIANTS = List.of("primary", "secondary", "success", "warning", "danger", "info", "ghost", "link");

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

    private static String template() throws IOException {
        return read("src/main/jte/ashlar/components/button.jte");
    }

    private static String css() throws IOException {
        return read("src/main/css/components/button.css");
    }

    private static List<CssScanner.Rule> rules() throws IOException {
        return CssScanner.flatRules(css().replaceAll("(?s)/\\*.*?\\*/", ""));
    }

    private static Map<String, String> declarations(String selector) throws IOException {
        return declarations(selector, "");
    }

    private static Map<String, String> declarations(String selector, String media) throws IOException {
        List<CssScanner.Rule> found = rules().stream().filter(r -> r.selector().equals(selector) && r.media().equals(media)).toList();
        assertEquals(1, found.size(), selector + " must be defined exactly once" + (media.isEmpty() ? "" : " inside " + media) + "; rules: "
                + rules().stream().map(r -> r.media() + "|" + r.selector()).toList());
        return CssScanner.declarations(found.get(0).block());
    }

    private static String markup() throws IOException {
        return template().replaceAll("(?s)<%--.*?--%>", "").replaceAll("(?m)^@(param|import) .*$", "").replaceAll("(?m)^!\\{.*\\}$", "").replaceAll("\\s+", " ").trim();
    }

    // ---- the template ----

    @Test
    void theParametersAndTheirDefaultsAreUnchanged() throws IOException {
        List<String> declared = new ArrayList<>();
        Matcher m = Pattern.compile("(?m)^@param .*$").matcher(template());
        while (m.find()) {
            declared.add(m.group());
        }
        assertEquals(List.of("@param String text = \"\"", "@param String type = \"Primary\"", "@param String size = \"Medium\"", "@param String buttonType = \"button\"",
                "@param Icon icon = null", "@param String iconPosition = \"leading\"", "@param boolean iconOnly = false", "@param String ariaLabel = \"\"",
                "@param boolean disabled = false", "@param boolean loading = false", "@param boolean loadingOnSubmit = false", "@param boolean fullWidth = false",
                "@param boolean toggle = false", "@param boolean pressed = false", "@param String pressedText = \"\"", "@param String id = \"\"",
                "@param String attributes = \"\""), declared);
    }

    @Test
    void theButtonKeepsItsNativeElementItsAriaStateAndItsHooksAndGainsOnlyAshlarClasses() throws IOException {
        String markup = markup();
        assertTrue(markup.startsWith("<button type=\"${resolvedButtonType}\" id=\"${idAttr}\" class=\"${buttonClasses}\" data-btn data-btn-variant=\"${resolvedVariant.name().toLowerCase()}\""
                + " disabled=\"${disabled}\" aria-busy=\"${ariaBusyAttr}\" aria-disabled=\"${ariaDisabledAttr}\" data-btn-loading-on-submit=\"${loadingOnSubmit}\""
                + " aria-label=\"${ariaLabelAttr}\" data-btn-toggle=\"${toggle}\" aria-pressed=\"${togglePressedAttr}\" data-btn-default-text=\"${toggleDefaultTextAttr}\""
                + " data-btn-pressed-text=\"${togglePressedTextAttr}\" $unsafe{attributes} >"), markup);
        assertTrue(markup.contains("<span data-btn-spinner class=\"ash-btn__spinner\" aria-hidden=\"true\"></span>"));
        assertEquals(2, count(markup, "<span data-btn-icon class=\"ash-btn__icon\" aria-hidden=\"true\">"), "the leading and the trailing icon wrapper");
        assertTrue(markup.contains("<span class=\"ash-sr-only ash-btn__label\">${text}</span>"), "the icon-only accessible name");
        assertTrue(markup.contains("<span data-btn-label class=\"ash-btn__label\">${text}</span>"));
        assertTrue(markup.contains("<span class=\"ash-sr-only ash-btn__status\" data-btn-status aria-live=\"polite\">${loading ? \"Loading\" : \"\"}</span> </button>"));
        assertTrue(markup.contains("@template.ashlar.components.icon(icon = icon, classes = iconSizeClasses)"));
    }

    @Test
    void theClassListAttributesWhoseOnlyReaderWasTheScriptAreGoneAndTheTextAttributesRemain() throws IOException {
        String source = template().replaceAll("(?s)<%--.*?--%>", "");
        assertFalse(source.contains("data-btn-base-classes"));
        assertFalse(source.contains("data-btn-pressed-classes"));
        assertFalse(source.contains("restingClasses") || source.contains("pressedClasses"), "the template no longer needs the two class sets");
        assertTrue(source.contains("data-btn-default-text=") && source.contains("data-btn-pressed-text="), "the label swap still reads these");
        String js = read("src/main/resources/META-INF/resources/ashlar/js/button.js");
        assertFalse(js.contains("btnBaseClasses") || js.contains("btnPressedClasses"));
        assertTrue(js.contains("btnPressedText") && js.contains("btnDefaultText"));
    }

    @Test
    void theTemplateMarksAPressedNonToggleWithTheModifierAndNeverMarksAToggleWithIt() throws IOException {
        assertTrue(template().contains("ButtonStyles.classes(resolvedVariant, resolvedSize, effectiveIconOnly, fullWidth, pressed && !toggle)"));
    }

    @Test
    void noTailwindUtilityRemainsInTheTemplate() throws IOException {
        String markup = (markup() + " " + template().replaceAll("(?s)<%--.*?--%>", "").replaceAll("(?m)^@param .*$", "")).replace("ash-sr-only", "");
        for (String utility : List.of("hidden", "inline-block", "inline-flex", "h-4", "w-4", "animate-spin", "rounded-", "border-", "sr-only", "bg-", "text-", "font-", "shadow", "ring")) {
            assertFalse(Pattern.compile("(?<![\\w-])" + Pattern.quote(utility)).matcher(markup).find(), "a Tailwind utility is back: " + utility);
        }
    }

    // ---- the script ----

    @Test
    void theScriptFlipsAttributesAndNeverTogglesAClass() throws IOException {
        String js = read("src/main/resources/META-INF/resources/ashlar/js/button.js").replaceAll("(?m)^\\s*//.*$", "");
        assertFalse(js.contains("classList"), "no class is toggled by the script");
        assertFalse(js.contains("\"hidden\"") || js.contains("inline-block"));
        assertTrue(js.contains("export function setButtonLoading(button, loading)"), "the public function and signature");
        assertTrue(js.contains("button.setAttribute(\"aria-busy\", \"true\")") && js.contains("button.setAttribute(\"aria-disabled\", \"true\")"));
        assertTrue(js.contains("button.removeAttribute(\"aria-busy\")") && js.contains("button.removeAttribute(\"aria-disabled\")"));
        assertFalse(js.contains("\"disabled\""), "loading never sets the native disabled attribute");
        assertTrue(js.contains("button.setAttribute(\"aria-pressed\", String(nextPressed))"));
        assertTrue(js.contains("event.target.closest('[data-btn][aria-busy=\"true\"]')") && js.contains("}, true);"), "the capture-phase click guard");
        assertTrue(js.contains("matches(\"[data-btn-loading-on-submit]\")") && js.contains("event.persisted"));
    }

    // ---- the stylesheet ----

    @Test
    void everySelectorIsScopedToTheButtonsOwnClassesAndNothingIsGlobal() throws IOException {
        String stripped = css().replaceAll("(?s)/\\*.*?\\*/", "");
        assertFalse(stripped.contains("!important"));
        assertFalse(stripped.contains("@import") || stripped.contains("@layer"));
        assertFalse(stripped.contains("data-btn"), "styling is by class and ARIA; the data-btn hooks belong to skins and the script");
        for (CssScanner.Rule rule : rules()) {
            for (String selector : CssScanner.selectors(rule.selector())) {
                assertTrue(selector.startsWith(".ash-btn") || selector.startsWith("button.ash-btn"), "every selector starts at a Button class: " + selector);
            }
        }
        assertEquals(List.of("@media (hover: hover)"), rules().stream().map(CssScanner.Rule::media).filter(m -> !m.isEmpty()).distinct().toList());
    }

    @Test
    void theBaseStatesWhatPreflightGaveAButtonAndWhatTheClassesMean() throws IOException {
        Map<String, String> base = declarations(".ash-btn");
        assertEquals("inline-flex", base.get("display"));
        assertEquals("center", base.get("align-items"));
        assertEquals("center", base.get("justify-content"));
        assertEquals("var(--ash-space-2)", base.get("gap"));
        assertEquals("0", base.get("margin"));
        assertEquals("0", base.get("padding"));
        assertEquals("0 solid", base.get("border"), "no border until a variant draws one");
        assertEquals("var(--ash-radius-md)", base.get("border-radius"), "the foundation token, which is var(--radius-md, 0.375rem): a skin's --radius-md rounds every Button");
        assertEquals("inherit", base.get("font"));
        assertEquals("var(--font-weight-medium, 500)", base.get("font-weight"));
        assertEquals("inherit", base.get("letter-spacing"));
        assertEquals("none", base.get("text-transform"));
        assertEquals("pointer", base.get("cursor"));
        assertEquals("none", base.get("user-select"));
        assertEquals("150ms", base.get("transition-duration"));
        assertEquals("cubic-bezier(0.4, 0, 0.2, 1)", base.get("transition-timing-function"));
        assertEquals("var(--ash-btn-bg)", base.get("background-color"));
        assertEquals("var(--ash-btn-fg)", base.get("color"));
        assertEquals("var(--ash-btn-shadow), var(--ash-btn-inset)", base.get("box-shadow"));
        assertEquals("0 0 #0000", base.get("--ash-btn-shadow"), "no shadow unless a variant gives one: a skin flattens the drop shadow by setting this");
        assertEquals("0 0 #0000", base.get("--ash-btn-inset"));
        assertEquals(Map.of("appearance", "button"), declarations("button.ash-btn"), "a <button> is a push button; the same classes on a decorative element are not");
        assertEquals(Map.of("outline-style", "none"), declarations(".ash-btn:focus"));
        assertEquals(Map.of("box-shadow", "0 0 0 2px #fff, 0 0 0 4px var(--ash-btn-ring), var(--ash-btn-shadow), var(--ash-btn-inset)"), declarations(".ash-btn:focus-visible"));
        assertEquals(Map.of("cursor", "not-allowed", "opacity", "0.6"), declarations(".ash-btn:disabled,\n.ash-btn[aria-disabled=\"true\"]"));
    }

    @Test
    void theFiveSizesAreTailwindsHeightsPaddingsAndTypeAndTheLineHeightRatiosLiveInProperties() throws IOException {
        Object[][] sizes = {{"xs", "1.5rem", "var(--ash-space-2)", "0.75rem", "calc(1 / 0.75)"}, {"sm", "2rem", "var(--ash-space-3)", "0.875rem", null},
                {"md", "var(--ash-space-10)", "var(--ash-space-4)", "0.875rem", null}, {"lg", "3rem", "var(--ash-space-6)", "1rem", "calc(1.5 / 1)"},
                {"xl", "3.5rem", "var(--ash-space-8)", "1.125rem", "calc(1.75 / 1.125)"}};
        assertEquals("calc(1.25 / 0.875)", declarations(".ash-btn").get("--ash-btn-leading"));
        for (Object[] s : sizes) {
            Map<String, String> d = declarations(".ash-btn--" + s[0]);
            assertEquals(s[1], d.get("--ash-btn-size"), s[0].toString());
            assertEquals("var(--ash-btn-size)", d.get("height"), s[0].toString());
            assertEquals(s[2], d.get("padding-inline"), s[0].toString());
            assertEquals(s[3], d.get("font-size"), s[0].toString());
            assertEquals("var(--ash-btn-leading)", d.get("line-height"), s[0].toString());
            assertEquals(s[4], d.get("--ash-btn-leading"), s[0].toString());
        }
        assertEquals(Map.of("width", "var(--ash-btn-size)", "padding-inline", "0"), declarations(".ash-btn--icon"));
        assertEquals(Map.of("width", "100%"), declarations(".ash-btn--block"));
    }

    @Test
    void everyVariantSetsEveryPropertyItReadsSoAButtonNeverInheritsOne() throws IOException {
        for (String v : VARIANTS) {
            Map<String, String> d = declarations(".ash-btn--" + v);
            for (String property : List.of("--ash-btn-bg", "--ash-btn-bg-hover", "--ash-btn-bg-active", "--ash-btn-fg", "--ash-btn-fg-hover", "--ash-btn-fg-active", "--ash-btn-ring")) {
                assertTrue(d.containsKey(property), v + " sets " + property);
            }
        }
        String white = "var(--color-white, #fff)";
        for (String v : List.of("primary", "success", "warning", "danger", "info")) {
            Map<String, String> d = declarations(".ash-btn--" + v);
            assertEquals(white, d.get("--ash-btn-fg"), v);
            assertEquals("0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1)", d.get("--ash-btn-shadow"), v + ": the resting drop shadow");
            assertFalse(d.containsKey("border-width"), v + " draws no border");
        }
        assertEquals("var(--color-blue-600, oklch(54.6% 0.245 262.881))", declarations(".ash-btn--primary").get("--ash-btn-bg"));
        assertEquals("var(--color-amber-500, oklch(76.9% 0.188 70.08))", declarations(".ash-btn--warning").get("--ash-btn-bg"), "Warning is amber-500 and its ring is too");
        assertEquals("var(--color-amber-500, oklch(76.9% 0.188 70.08))", declarations(".ash-btn--warning").get("--ash-btn-ring"));
        Map<String, String> secondary = declarations(".ash-btn--secondary");
        assertEquals("1px", secondary.get("border-width"));
        assertEquals("var(--ash-btn-border)", secondary.get("border-color"));
        assertEquals(white, secondary.get("--ash-btn-bg"));
        assertEquals("var(--color-slate-300, oklch(86.9% 0.022 252.894))", secondary.get("--ash-btn-border"));
        assertEquals("transparent", declarations(".ash-btn--ghost").get("--ash-btn-bg"));
        assertFalse(declarations(".ash-btn--ghost").containsKey("--ash-btn-shadow"), "Ghost never had a shadow");
        assertFalse(declarations(".ash-btn--link").containsKey("--ash-btn-shadow"), "Link never had one (shadow-none)");
        assertEquals("4px", declarations(".ash-btn--link").get("text-underline-offset"));
    }

    @Test
    void darkModeUsesTheSameZeroSpecificityFormAndOnlyWhereTheLegacyButtonHadDarkClasses() throws IOException {
        List<String> darkRules = rules().stream().map(CssScanner.Rule::selector).filter(s -> s.contains("data-theme")).toList();
        assertEquals(List.of(".ash-btn--secondary" + DARK, ".ash-btn--ghost" + DARK, ".ash-btn--link" + DARK, ".ash-btn--secondary" + PRESSED + DARK,
                ".ash-btn--ghost" + PRESSED + DARK, ".ash-btn--link" + PRESSED + DARK), darkRules, "the filled variants had no dark classes");
        assertEquals("var(--color-slate-800, oklch(27.9% 0.041 260.031))", declarations(".ash-btn--secondary" + DARK).get("--ash-btn-bg"));
        assertEquals("var(--color-slate-600, oklch(44.6% 0.043 257.281))", declarations(".ash-btn--secondary" + DARK).get("--ash-btn-border"));
        assertEquals("var(--color-blue-400, oklch(70.7% 0.165 254.624))", declarations(".ash-btn--link" + DARK).get("--ash-btn-fg"));
    }

    @Test
    void pressedPinsTheHoverAndActiveColoursSoAPressedButtonDoesNotChange() throws IOException {
        for (String v : List.of("primary", "success", "warning", "danger", "info")) {
            // The five filled variants share one rule, so look for the selector inside the grouped selector list.
            List<CssScanner.Rule> grouped = rules().stream().filter(r -> r.selector().contains(".ash-btn--" + v + PRESSED)).toList();
            assertEquals(1, grouped.size(), v);
            Map<String, String> d = CssScanner.declarations(grouped.get(0).block());
            assertEquals("var(--ash-btn-bg-active)", d.get("--ash-btn-bg"), v);
            assertEquals("var(--ash-btn-bg-active)", d.get("--ash-btn-bg-hover"), v);
            assertEquals("0 0 #0000", d.get("--ash-btn-shadow"), v + ": the pressed look replaces the drop shadow with the inset shadow");
            assertEquals("inset 0 2px 4px 0 rgb(0 0 0 / 0.05)", d.get("--ash-btn-inset"), v);
        }
        Map<String, String> secondary = declarations(".ash-btn--secondary" + PRESSED);
        assertEquals("var(--ash-btn-bg)", secondary.get("--ash-btn-bg-hover"));
        assertEquals("var(--ash-btn-bg)", secondary.get("--ash-btn-bg-active"));
        assertEquals("inset 0 2px 4px 0 rgb(0 0 0 / 0.05)", secondary.get("--ash-btn-inset"));
        assertEquals("underline", declarations(".ash-btn--link" + PRESSED).get("text-decoration-line"));
        assertEquals("auto", declarations(".ash-btn--link" + PRESSED).get("text-underline-offset"), "the pressed Link never had the resting Link's underline offset");
        assertFalse(declarations(".ash-btn--ghost" + PRESSED).containsKey("--ash-btn-inset"), "a pressed Ghost had shadow-none");
    }

    @Test
    void hoverIsInsideTheHoverMediaQueryAndActiveIsNotAndDisabledAndLoadingResetTheColours() throws IOException {
        String hover = "@media (hover: hover)";
        assertEquals(Map.of("background-color", "var(--ash-btn-bg-hover)", "color", "var(--ash-btn-fg-hover)"), declarations(".ash-btn:hover", hover));
        assertEquals(Map.of("text-decoration-line", "underline"), declarations(".ash-btn--link:hover", hover));
        assertEquals(Map.of("background-color", "var(--ash-btn-bg-active)", "color", "var(--ash-btn-fg-active)"), declarations(".ash-btn:active"));
        List<CssScanner.Rule> reset = rules().stream().filter(r -> r.media().equals(hover) && r.selector().startsWith(".ash-btn:disabled:hover")).toList();
        assertEquals(1, reset.size());
        assertEquals(Map.of("background-color", "var(--ash-btn-bg)"), CssScanner.declarations(reset.get(0).block()));
        assertTrue(reset.get(0).selector().contains(".ash-btn[aria-disabled=\"true\"]:hover"), "loading resets too");
        List<CssScanner.Rule> underline = rules().stream().filter(r -> r.selector().contains(":disabled:hover") && r.selector().contains("link")).toList();
        assertEquals(1, underline.size());
        assertEquals(Map.of("text-decoration-line", "none"), CssScanner.declarations(underline.get(0).block()));
        assertTrue(underline.get(0).selector().contains(":where(:not(.ash-btn--pressed, [aria-pressed=\"true\"]))"), "a pressed Link never had the no-underline reset");
    }

    @Test
    void theSpinnerIsShownAndTheIconHiddenByTheButtonsOwnAriaBusyAndNothingElse() throws IOException {
        Map<String, String> spinner = declarations(".ash-btn__spinner");
        assertEquals("none", spinner.get("display"));
        assertEquals("1rem", spinner.get("width"));
        assertEquals("1rem", spinner.get("height"));
        assertEquals("2px solid currentColor", spinner.get("border"));
        assertEquals("transparent", spinner.get("border-top-color"));
        assertEquals("9999px", spinner.get("border-radius"));
        assertEquals("ash-btn-spin 1s linear infinite", spinner.get("animation"), "the animation is preserved exactly, with no reduced-motion override");
        assertEquals(Map.of("display", "inline-block"), declarations(".ash-btn[aria-busy=\"true\"] .ash-btn__spinner"));
        assertEquals(Map.of("display", "none"), declarations(".ash-btn[aria-busy=\"true\"] .ash-btn__icon"));
        assertFalse(css().contains("prefers-reduced-motion"), "the legacy spinner kept spinning under reduced motion; this migration preserves that");
    }

    @Test
    void theEntryFileImportsTheStylesheetInTheComponentsLayer() throws IOException {
        assertTrue(read("src/main/css/ashlar.css").contains("@import \"components/button.css\" layer(ash.components);"));
    }

    @Test
    void theSharedFoundationTokensAreNotTouched() throws IOException {
        String tokens = read("src/main/css/tokens/tokens.css");
        assertFalse(tokens.contains("--ash-btn"), "Button adds nothing to the shared tokens");
        assertTrue(tokens.contains("--ash-radius-md: var(--radius-md, 0.375rem);"), "the radius token Button reads is the existing one, unchanged");
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }
}
