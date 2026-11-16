package dev.redspartanlabs.ashlar.css;

import dev.redspartanlabs.ashlar.drawer.DrawerPlacement;
import dev.redspartanlabs.ashlar.drawer.DrawerStyles;
import dev.redspartanlabs.ashlar.modal.ModalSize;
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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Modal and Drawer contract after the move to Ashlar's own CSS: the public template parameters, the markup with its hooks and ARIA, the Java helpers' classes, scripts
 * that toggle no class at all, and a stylesheet (overlay.css, modal.css, drawer.css) that is scoped, layered, self-contained and says how every state looks.
 */
class OverlayContractTest {

    private static final String DARK = ":where([data-theme=\"dark\"], [data-theme=\"dark\"] *)";
    private static final List<String> CSS = List.of("overlay", "modal", "drawer");
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

    // ---- the templates ----

    @Test
    void theParametersAndTheirDefaultsAreUnchanged() throws IOException {
        assertEquals(List.of("@param String id", "@param String title", "@param String size = \"Medium\"", "@param boolean closeOnBackdrop = true", "@param boolean closeOnEscape = true",
                "@param boolean showCloseButton = true", "@param gg.jte.Content body = null", "@param gg.jte.Content footer = null"), declared("modal"));
        assertEquals(List.of("@param String id", "@param String title", "@param String placement = \"Right\"", "@param gg.jte.Content body = null", "@param gg.jte.Content footer = null"),
                declared("drawer"));
    }

    @Test
    void eachTemplateKeepsItsHooksItsAriaItsPartsAndItsOrderExactly() throws IOException {
        assertEquals("<div id=\"${id}\" data-modal data-close-on-backdrop=\"${closeOnBackdrop}\" data-close-on-escape=\"${closeOnEscape}\" data-state=\"closed\" class=\"ash-overlay ash-modal\" hidden >"
                + " <div data-modal-backdrop class=\"ash-overlay__backdrop\"></div>"
                + " <div data-modal-dialog role=\"dialog\" aria-modal=\"true\" aria-labelledby=\"${titleId}\" tabindex=\"-1\" class=\"ash-overlay__surface ash-modal__dialog ${resolvedSize.maxWidthClass()}\" >"
                + " <div class=\"ash-overlay__header\"> <h2 id=\"${titleId}\" class=\"ash-overlay__title\">${title}</h2>"
                + " @if (showCloseButton) <button type=\"button\" data-modal-close=\"${id}\" aria-label=\"Close\" class=\"ash-overlay__close\" > @template.ashlar.components.icon(icon = Icon.CLOSE) </button> @endif </div>"
                + " <div class=\"ash-overlay__body\"> @if (body != null) ${body} @endif </div>"
                + " @if (footer != null) <div class=\"ash-overlay__footer\"> ${footer} </div> @endif </div> </div>"
                + " <script type=\"module\" src=\"${AshlarAssets.js(\"modal.js\")}\"></script>", markup("modal"));
        assertEquals("<div id=\"${id}\" data-drawer data-state=\"closed\" class=\"ash-overlay ash-drawer ${DrawerStyles.alignment(resolvedPlacement)}\" hidden >"
                + " <div data-drawer-backdrop class=\"ash-overlay__backdrop\"></div>"
                + " <div data-drawer-panel role=\"dialog\" aria-modal=\"true\" aria-labelledby=\"${titleId}\" tabindex=\"-1\" class=\"ash-overlay__surface ash-drawer__panel ${DrawerStyles.border(resolvedPlacement)}\" >"
                + " <div class=\"ash-overlay__header\"> <h2 id=\"${titleId}\" class=\"ash-overlay__title\">${title}</h2>"
                + " <button type=\"button\" data-drawer-close=\"${id}\" aria-label=\"Close\" class=\"ash-overlay__close\" > @template.ashlar.components.icon(icon = Icon.CLOSE) </button> </div>"
                + " <div class=\"ash-overlay__body ash-drawer__body\"> @if (body != null) ${body} @endif </div>"
                + " @if (footer != null) <div class=\"ash-overlay__footer\"> ${footer} </div> @endif </div> </div>"
                + " <script type=\"module\" src=\"${AshlarAssets.js(\"drawer.js\")}\"></script>", markup("drawer"));
    }

    @Test
    void theTitleIdIsDerivedFromTheIdAsBefore() throws IOException {
        for (String name : List.of("modal", "drawer")) {
            assertTrue(template(name).contains("!{String titleId = id + \"-title\";}"), name);
        }
    }

    @Test
    void dataDrawerClosedClassIsGoneFromTheLibrary() throws IOException {
        assertFalse(markup("drawer").contains("data-drawer-closed-class"));
        assertFalse(script("drawer").contains("drawerClosedClass") || script("drawer").contains("closed-class"));
        assertFalse(markup("drawer").contains("closedTransform"), "the template no longer hands the closed position to the script");
    }

    @Test
    void noTailwindUtilityRemainsInTheTemplatesTheHelpersOrTheScripts() throws IOException {
        for (String name : List.of("modal", "drawer")) {
            Matcher cls = Pattern.compile("(?<![\\w-])class=\"([^\"]*)\"").matcher(withoutJteComments(template(name)));
            while (cls.find()) {
                for (String token : cls.group(1).replaceAll("\\$\\{[^}]*\\}", " ").trim().split("\\s+")) {
                    assertTrue(token.isEmpty() || token.startsWith("ash-"), name + ": not an Ashlar class: " + token);
                }
            }
            assertTrue(template(name).contains("@template.ashlar.components.icon(icon = Icon.CLOSE)"), name + ": the close icon takes the Icon's own default size, no classes");
        }
        for (ModalSize size : ModalSize.values()) {
            assertTrue(size.maxWidthClass().matches("ash-modal__dialog--(sm|md|lg)"), size.maxWidthClass());
        }
        for (DrawerPlacement p : DrawerPlacement.values()) {
            for (String c : List.of(DrawerStyles.alignment(p), DrawerStyles.border(p), DrawerStyles.closedTransform(p))) {
                assertTrue(c.matches("ash-drawer(__panel)?--(left|right)"), c);
            }
        }
        for (String name : List.of("overlay", "modal", "drawer")) {
            String js = script(name);
            assertFalse(js.contains("classList") || js.contains("className"), name + ".js toggles no class: state is hidden, data-state and --ash-overlay-stack");
        }
    }

    @Test
    void theScriptsShareOneInternalOverlayModuleAndKeepTheirOwnTriggers() throws IOException {
        assertTrue(script("modal").startsWith("import {") && script("modal").contains("from \"./overlay.js\";"));
        assertTrue(script("drawer").startsWith("import {") && script("drawer").contains("from \"./overlay.js\";"));
        assertTrue(script("modal").contains("[data-modal-open=") && script("modal").contains("[data-modal-close=") && script("modal").contains("root.dataset.closeOnBackdrop === \"true\"")
                && script("modal").contains("root.dataset.closeOnEscape === \"true\""));
        assertTrue(script("drawer").contains("[data-drawer-open=") && script("drawer").contains("[data-drawer-close="));
        assertFalse(script("modal").contains("addEventListener(\"keydown\"") || script("drawer").contains("addEventListener(\"keydown\""),
                "Escape and Tab are dispatched by overlay.js to the topmost overlay only");
        assertTrue(script("overlay").contains("document.addEventListener(\"keydown\", (event) => {\n    if (event.key !== \"Escape\") {") && script("overlay").contains("}, true);"),
                "the nested-popup decision is taken in the capture phase");
    }

    // ---- the stylesheet ----

    @Test
    void theThreeFilesAreImportedInOrderIntoTheComponentsLayer() throws IOException {
        String entry = read("src/main/css/ashlar.css");
        int overlay = entry.indexOf("@import \"components/overlay.css\" layer(ash.components);");
        int modal = entry.indexOf("@import \"components/modal.css\" layer(ash.components);");
        int drawer = entry.indexOf("@import \"components/drawer.css\" layer(ash.components);");
        assertTrue(overlay > 0 && overlay < modal && modal < drawer, "overlay.css first (the shared primitives), then modal.css and drawer.css");
    }

    @Test
    void everySelectorIsScopedToTheOverlayClassesAndNothingIsImportant() throws IOException {
        for (String name : CSS) {
            String source = css(name).replaceAll("(?s)/\\*.*?\\*/", "");
            assertFalse(source.contains("!important"), name);
            for (CssScanner.Rule r : rules(name)) {
                for (String selector : CssScanner.selectors(r.selector())) {
                    assertTrue(selector.startsWith(".ash-overlay") || selector.startsWith(".ash-modal") || selector.startsWith(".ash-drawer"), name + ": not scoped: " + selector);
                }
            }
        }
    }

    @Test
    void everyClassTheTemplatesAndHelpersWriteIsDefined() throws IOException {
        Set<String> defined = new LinkedHashSet<>();
        for (String name : CSS) {
            Matcher m = Pattern.compile("\\.(ash-(?:overlay|modal|drawer)[a-z0-9_-]*)").matcher(css(name).replaceAll("(?s)/\\*.*?\\*/", ""));
            while (m.find()) {
                defined.add(m.group(1));
            }
        }
        Set<String> written = new LinkedHashSet<>();
        for (String name : List.of("modal", "drawer")) {
            Matcher cls = Pattern.compile("(?<![\\w-])class=\"([^\"]*)\"").matcher(withoutJteComments(template(name)));
            while (cls.find()) {
                for (String token : cls.group(1).replaceAll("\\$\\{[^}]*\\}", " ").trim().split("\\s+")) {
                    if (!token.isEmpty()) {
                        written.add(token);
                    }
                }
            }
        }
        for (ModalSize size : ModalSize.values()) {
            written.add(size.maxWidthClass());
        }
        for (DrawerPlacement p : DrawerPlacement.values()) {
            written.add(DrawerStyles.alignment(p));
            written.add(DrawerStyles.border(p));
        }
        for (String c : written) {
            assertTrue(defined.contains(c), c + " is written but not defined");
        }
    }

    @Test
    void theRootIsFixedInTheOverlayBandAndHiddenMeansNotRendered() throws IOException {
        Map<String, String> root = declarations("overlay", ".ash-overlay");
        assertEquals("fixed", root.get("position"));
        assertEquals("0", root.get("inset"));
        assertEquals("calc(40 + var(--ash-overlay-stack, 0))", root.get("z-index"), "40, the band between Popover (30) and the toast region (50), plus the stack offset");
        assertEquals("none", declarations("overlay", ".ash-overlay[hidden]").get("display"));
        assertTrue(script("overlay").contains("const MAX_STACK_OFFSET = 9;"), "the offset is capped so no overlay reaches the toast region's 50");
    }

    @Test
    void theOpenStateIsSelectedByDataStateOnTheRoot() throws IOException {
        assertEquals("1", declarations("overlay", ".ash-overlay[data-state=\"open\"] > .ash-overlay__backdrop").get("opacity"));
        Map<String, String> dialog = declarations("modal", ".ash-modal[data-state=\"open\"] > .ash-modal__dialog");
        assertEquals(Map.of("opacity", "1", "translate", "0 0px", "scale", "100% 100%"), dialog);
        assertEquals(Map.of("translate", "0px 0"), declarations("drawer", ".ash-drawer[data-state=\"open\"] > .ash-drawer__panel"));
    }

    @Test
    void everyTransitionHasOneDurationAndIsOffUnderReducedMotion() throws IOException {
        int transitioning = 0;
        for (String name : CSS) {
            for (CssScanner.Rule r : rules(name)) {
                Map<String, String> d = CssScanner.declarations(r.block());
                if (r.media().contains("prefers-reduced-motion: reduce")) {
                    assertEquals("none", d.get("transition-property"), name + " " + r.selector());
                } else if (d.containsKey("transition-property")) {
                    transitioning++;
                    assertEquals("0.2s", d.get("transition-duration"), name + " " + r.selector() + ": one duration for every listed property");
                    assertFalse(d.containsKey("transition-delay"), name + " " + r.selector());
                    assertTrue(rules(name).stream().anyMatch(x -> x.media().contains("prefers-reduced-motion: reduce") && x.selector().equals(r.selector())),
                            name + ": " + r.selector() + " has no reduced-motion rule");
                }
            }
        }
        assertEquals(3, transitioning, "the backdrop, the dialog and the panel");
    }

    @Test
    void darkModeUsesTheZeroSpecificityWhereFormAndTheShadowIsAPropertyASkinCanReplace() throws IOException {
        assertTrue(rules("overlay").stream().anyMatch(r -> r.selector().equals(".ash-overlay" + DARK)));
        assertEquals("var(--ash-overlay-shadow)", declarations("overlay", ".ash-overlay__surface").get("box-shadow"));
    }

    private static Map<String, String> declarations(String cssName, String selector) throws IOException {
        List<CssScanner.Rule> found = rules(cssName).stream().filter(r -> r.selector().equals(selector) && r.media().isEmpty()).toList();
        assertEquals(1, found.size(), selector + " must be defined exactly once in " + cssName);
        return CssScanner.declarations(found.get(0).block());
    }

    @Test
    void theSkinHooksStayOnTheSurfaces() throws IOException {
        assertTrue(markup("modal").contains("data-modal-dialog role=\"dialog\""));
        assertTrue(markup("drawer").contains("data-drawer-panel role=\"dialog\""));
        try (Stream<Path> files = Files.list(module.resolve("src/main/css/components"))) {
            assertTrue(files.anyMatch(f -> f.getFileName().toString().equals("overlay.css")));
        }
    }
}
