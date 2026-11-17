package dev.redspartanlabs.ashlar.css;

import dev.redspartanlabs.ashlar.popover.PopoverPlacement;
import dev.redspartanlabs.ashlar.popover.PopoverStyles;
import dev.redspartanlabs.ashlar.tooltip.TooltipPlacement;
import dev.redspartanlabs.ashlar.tooltip.TooltipStyles;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Tooltip and Popover contract after the move to Ashlar's own CSS: the public template parameters, the markup with its hooks and ARIA, the Java helpers' classes, scripts
 * that toggle no class and keep their timing, and a stylesheet (tooltip.css, popover.css) that is scoped, layered, self-contained, positions with the individual transform
 * properties the scripts' nudge relies on, and says how every state looks.
 */
class FloatingContractTest {

    private static final String DARK = ":where([data-theme=\"dark\"], [data-theme=\"dark\"] *)";
    private static final List<String> CSS = List.of("tooltip", "popover");
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

    private static String read(String path) throws IOException {
        return Files.readString(module.resolve(path), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static String template(String name) throws IOException {
        return read("src/main/jte/ashlar/components/" + name + ".jte");
    }

    private static String script(String name) throws IOException {
        return read("src/main/resources/META-INF/resources/ashlar/js/" + name + ".js");
    }

    private static String css(String name) throws IOException {
        return read("src/main/css/components/" + name + ".css");
    }

    private static List<CssScanner.Rule> rules(String name) throws IOException {
        return CssScanner.flatRules(css(name).replaceAll("(?s)/\\*.*?\\*/", ""));
    }

    private static String withoutJteComments(String source) {
        return source.replaceAll("(?s)<%--.*?--%>", "");
    }

    private static String markup(String name) throws IOException {
        return withoutJteComments(template(name)).replaceAll("(?m)^@(?:param|import) .*$", "").replaceAll("(?m)^!\\{.*\\}$", "").replaceAll("\\s+", " ").trim();
    }

    private static List<String> declared(String name) throws IOException {
        List<String> declared = new ArrayList<>();
        Matcher m = Pattern.compile("(?m)^@param .*$").matcher(template(name));
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
        assertEquals(List.of("@param gg.jte.Content trigger", "@param String content", "@param String placement = \"Top\""), declared("tooltip"));
        assertEquals(List.of("@param gg.jte.Content trigger", "@param gg.jte.Content content", "@param String title = \"\"", "@param String placement = \"Bottom\""), declared("popover"));
    }

    @Test
    void eachTemplateKeepsItsHooksItsAriaItsPartsAndItsOrderExactly() throws IOException {
        assertEquals("<span class=\"ash-tooltip\" data-tooltip data-tooltip-placement=\"${resolvedPlacement.name().toLowerCase()}\">"
                + " <span data-tooltip-trigger class=\"ash-tooltip__trigger\"> ${trigger} </span>"
                + " <span id=\"${tooltipId}\" role=\"tooltip\" data-tooltip-bubble data-state=\"closed\" class=\"ash-tooltip__bubble ${positionClasses}\" hidden > ${content}"
                + " <span aria-hidden=\"true\" data-tooltip-arrow class=\"ash-tooltip__arrow ${arrowClasses}\"></span> </span> </span>"
                + " <script type=\"module\" src=\"${AshlarAssets.js(\"tooltip.js\")}\"></script>", markup("tooltip"));
        assertEquals("<span class=\"ash-popover\" data-popover data-popover-placement=\"${resolvedPlacement.name().toLowerCase()}\">"
                + " <span data-popover-trigger class=\"ash-popover__trigger\"> ${trigger} </span>"
                + " <div id=\"${popoverId}\" data-popover-surface data-state=\"closed\" class=\"ash-popover__surface ${positionClasses}\" hidden >"
                + " @if (hasTitle) <h3 class=\"ash-popover__title\">${title}</h3> @endif <div class=\"ash-popover__body\"> ${content} </div> </div> </span>"
                + " <script type=\"module\" src=\"${AshlarAssets.js(\"popover.js\")}\"></script>", markup("popover"));
        assertTrue(template("tooltip").contains("!{String tooltipId = TooltipIds.next();}") && template("popover").contains("!{String popoverId = PopoverIds.next();}"));
        assertTrue(template("tooltip").contains("TooltipPlacement.from(placement)") && template("popover").contains("PopoverPlacement.from(placement)"), "the placement fallback is the enums'");
    }

    @Test
    void noTailwindUtilityRemainsInTheTemplatesTheHelpersOrTheScripts() throws IOException {
        for (String name : CSS) {
            Matcher cls = Pattern.compile("(?<![\\w-])class=\"([^\"]*)\"").matcher(withoutJteComments(template(name)));
            while (cls.find()) {
                for (String token : cls.group(1).replaceAll("\\$\\{[^}]*\\}", " ").trim().split("\\s+")) {
                    assertTrue(token.isEmpty() || token.startsWith("ash-"), name + ": not an Ashlar class: " + token);
                }
            }
            String js = script(name);
            assertFalse(js.contains("classList") || js.contains("className"), name + ".js toggles no class: state is hidden and data-state on the surface");
        }
        for (TooltipPlacement p : TooltipPlacement.values()) {
            assertTrue(TooltipStyles.position(p).matches("ash-tooltip__bubble--(top|right|bottom|left)"), TooltipStyles.position(p));
            assertTrue(TooltipStyles.arrow(p).matches("ash-tooltip__arrow--(top|right|bottom|left)"), TooltipStyles.arrow(p));
        }
        for (PopoverPlacement p : PopoverPlacement.values()) {
            assertTrue(PopoverStyles.position(p).matches("ash-popover__surface--(top|right|bottom|left)"), PopoverStyles.position(p));
        }
    }

    @Test
    void theScriptsKeepTheirTimingAndStayIndependentOfTheOverlayModule() throws IOException {
        assertTrue(script("tooltip").contains("const SHOW_DELAY_MS = 150;") && script("tooltip").contains("const TRANSITION_DURATION_MS = 150;"));
        assertTrue(script("popover").contains("const TRANSITION_DURATION_MS = 150;"));
        for (String name : CSS) {
            assertFalse(script(name).contains("import ") || script(name).contains("overlay.js"), name + ".js is a standalone module: no import, no overlay.js");
            assertTrue(script(name).contains("document.addEventListener(\"DOMContentLoaded\""), name);
        }
        // The signals overlay.js reads to leave an Escape to an open popup: a Popover's anchor aria-expanded, a Tooltip's role="tooltip" referenced by aria-describedby.
        assertTrue(script("popover").contains("anchor.setAttribute(\"aria-expanded\", \"true\");") && script("popover").contains("anchor.setAttribute(\"aria-controls\", surface.id);"));
        assertTrue(script("tooltip").contains("anchor.setAttribute(\"aria-describedby\", tokens.join(\" \"));") && markup("tooltip").contains("role=\"tooltip\""));
    }

    // ---- the stylesheet ----

    @Test
    void theTwoFilesAreImportedAfterTheOverlaysIntoTheComponentsLayer() throws IOException {
        String entry = read("src/main/css/ashlar.css");
        int drawer = entry.indexOf("@import \"components/drawer.css\" layer(ash.components);");
        int tooltip = entry.indexOf("@import \"components/tooltip.css\" layer(ash.components);");
        int popover = entry.indexOf("@import \"components/popover.css\" layer(ash.components);");
        assertTrue(drawer > 0 && drawer < tooltip && tooltip < popover, "tooltip.css then popover.css, after drawer.css");
    }

    @Test
    void everySelectorIsScopedToTheComponentsOwnClassesAndNothingIsImportant() throws IOException {
        for (String name : CSS) {
            String source = css(name).replaceAll("(?s)/\\*.*?\\*/", "");
            assertFalse(source.contains("!important"), name);
            for (CssScanner.Rule r : rules(name)) {
                for (String selector : CssScanner.selectors(r.selector())) {
                    assertTrue(selector.startsWith(".ash-" + name), name + ": not scoped: " + selector);
                    assertFalse(selector.contains(" ") && !selector.contains(":where("), name + ": a descendant selector (the state is the surface's own attribute): " + selector);
                }
            }
        }
    }

    @Test
    void everyClassTheTemplatesAndHelpersWriteIsDefined() throws IOException {
        Set<String> defined = new LinkedHashSet<>();
        for (String name : CSS) {
            Matcher m = Pattern.compile("\\.(ash-(?:tooltip|popover)[a-z0-9_-]*)").matcher(css(name).replaceAll("(?s)/\\*.*?\\*/", ""));
            while (m.find()) {
                defined.add(m.group(1));
            }
        }
        Set<String> written = new LinkedHashSet<>();
        for (String name : CSS) {
            Matcher cls = Pattern.compile("(?<![\\w-])class=\"([^\"]*)\"").matcher(withoutJteComments(template(name)));
            while (cls.find()) {
                for (String token : cls.group(1).replaceAll("\\$\\{[^}]*\\}", " ").trim().split("\\s+")) {
                    if (!token.isEmpty()) {
                        written.add(token);
                    }
                }
            }
        }
        for (TooltipPlacement p : TooltipPlacement.values()) {
            written.add(TooltipStyles.position(p));
            written.add(TooltipStyles.arrow(p));
        }
        for (PopoverPlacement p : PopoverPlacement.values()) {
            written.add(PopoverStyles.position(p));
        }
        for (String c : written) {
            assertTrue(defined.contains(c), c + " is written but not defined");
        }
    }

    @Test
    void hiddenMeansNotRenderedAndTheOpenStateIsTheSurfacesOwnDataState() throws IOException {
        assertEquals("none", declarations("tooltip", ".ash-tooltip__bubble[hidden]").get("display"));
        assertEquals("none", declarations("popover", ".ash-popover__surface[hidden]").get("display"));
        assertEquals(Map.of("opacity", "1", "scale", "100% 100%"), declarations("tooltip", ".ash-tooltip__bubble[data-state=\"open\"]"));
        assertEquals(Map.of("opacity", "1", "scale", "100% 100%"), declarations("popover", ".ash-popover__surface[data-state=\"open\"]"));
        assertEquals("0", declarations("tooltip", ".ash-tooltip__bubble").get("opacity"));
        assertEquals("95% 95%", declarations("popover", ".ash-popover__surface").get("scale"));
    }

    @Test
    void placementUsesTheIndividualTransformPropertiesTheScriptsNudgeOverridesAndNeverTransform() throws IOException {
        for (String name : CSS) {
            for (CssScanner.Rule r : rules(name)) {
                assertFalse(CssScanner.declarations(r.block()).containsKey("transform"), name + " " + r.selector() + ": transform would add to the script's inline translate");
            }
        }
        for (String side : List.of("top", "bottom")) {
            assertEquals("-50% 0", declarations("tooltip", ".ash-tooltip__bubble--" + side).get("translate"));
            assertEquals("-50% 0", declarations("tooltip", ".ash-tooltip__arrow--" + side).get("translate"));
            assertEquals("-50% 0", declarations("popover", ".ash-popover__surface--" + side).get("translate"));
        }
        for (String side : List.of("left", "right")) {
            assertEquals("0 -50%", declarations("tooltip", ".ash-tooltip__bubble--" + side).get("translate"));
            assertEquals("0 -50%", declarations("popover", ".ash-popover__surface--" + side).get("translate"));
        }
        assertEquals("45deg", declarations("tooltip", ".ash-tooltip__arrow").get("rotate"));
        assertTrue(script("tooltip").contains("bubble.style.translate = ") && script("popover").contains("surface.style.translate = "), "the nudge is an inline translate");
    }

    @Test
    void everyTransitionIs150msAndIsOffUnderReducedMotion() throws IOException {
        int transitioning = 0;
        for (String name : CSS) {
            for (CssScanner.Rule r : rules(name)) {
                Map<String, String> d = CssScanner.declarations(r.block());
                if (r.media().contains("prefers-reduced-motion: reduce")) {
                    assertEquals("none", d.get("transition-property"), name + " " + r.selector());
                } else if (d.containsKey("transition-property")) {
                    transitioning++;
                    assertEquals("150ms", d.get("transition-duration"), name + " " + r.selector() + ": the scripts wait 150ms (plus 50) for an exit");
                    assertFalse(d.containsKey("transition-delay"), name + " " + r.selector());
                    assertTrue(rules(name).stream().anyMatch(x -> x.media().contains("prefers-reduced-motion: reduce") && x.selector().equals(r.selector())),
                            name + ": " + r.selector() + " has no reduced-motion rule");
                }
            }
        }
        assertEquals(2, transitioning, "the bubble and the surface");
        assertEquals("opacity, scale", declarations("tooltip", ".ash-tooltip__bubble").get("transition-property"));
        assertEquals("opacity, scale, translate", declarations("popover", ".ash-popover__surface").get("transition-property"));
    }

    @Test
    void stackingDarkModeAndTheShadowASkinCanReplace() throws IOException {
        assertEquals("20", declarations("tooltip", ".ash-tooltip__bubble").get("z-index"));
        assertEquals("30", declarations("popover", ".ash-popover__surface").get("z-index"), "below the overlay band (40)");
        assertEquals("relative", declarations("tooltip", ".ash-tooltip").get("position"));
        assertFalse(declarations("tooltip", ".ash-tooltip").containsKey("z-index") || declarations("popover", ".ash-popover").containsKey("z-index"),
                "the root makes no stacking context of its own");
        assertTrue(rules("tooltip").stream().anyMatch(r -> r.selector().equals(".ash-tooltip__bubble" + DARK)));
        assertTrue(rules("popover").stream().anyMatch(r -> r.selector().equals(".ash-popover__surface" + DARK)));
        assertEquals("var(--ash-tooltip-shadow)", declarations("tooltip", ".ash-tooltip__bubble").get("box-shadow"));
        assertEquals("0 0 0 1px var(--ash-popover-ring), var(--ash-popover-shadow)", declarations("popover", ".ash-popover__surface").get("box-shadow"),
                "the ring is a box-shadow layer, so a skin's box-shadow replaces it with the shadow, as before");
        assertEquals("var(--radius-md, 0.375rem)", declarations("tooltip", ".ash-tooltip__bubble").get("border-radius"));
        assertEquals("var(--radius-lg, 0.5rem)", declarations("popover", ".ash-popover__surface").get("border-radius"));
        assertTrue(markup("tooltip").contains("data-tooltip-bubble") && markup("popover").contains("data-popover-surface"), "the skin hooks stay on the surfaces");
    }
}
