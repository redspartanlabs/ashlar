package dev.redspartanlabs.ashlar.css;

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
 * The Dropdown Menu contract after the move to Ashlar's own CSS: the public template parameters, the markup with its hooks, ARIA and ids, a script that toggles no class
 * and keeps its timing and the signals an enclosing Modal, Drawer or Popover reads, and a stylesheet (dropdown-menu.css, dropdown-menu-item.css) that is scoped, layered,
 * self-contained, keeps the menu's plain static-position placement, and says how every state looks.
 */
class DropdownMenuContractTest {

    private static final String DARK = ":where([data-theme=\"dark\"], [data-theme=\"dark\"] *)";
    private static final List<String> CSS = List.of("dropdown-menu", "dropdown-menu-item");
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

    private static String script() throws IOException {
        return read("src/main/resources/META-INF/resources/ashlar/js/dropdown-menu.js");
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
        assertEquals(1, found.size(), selector + " must be defined exactly once outside any @media in " + cssName);
        return CssScanner.declarations(found.get(0).block());
    }

    private static Map<String, String> inMedia(String cssName, String media, String selector) throws IOException {
        List<CssScanner.Rule> found = rules(cssName).stream().filter(r -> r.selector().replaceAll("\\s+", " ").equals(selector) && r.media().contains(media)).toList();
        assertEquals(1, found.size(), selector + " must be defined exactly once inside @media " + media + " in " + cssName);
        return CssScanner.declarations(found.get(0).block());
    }

    private static Set<String> written() throws IOException {
        Set<String> written = new LinkedHashSet<>();
        for (String name : CSS) {
            Matcher cls = Pattern.compile("(?<![\\w-])class=\"([^\"]*)\"").matcher(withoutJteComments(template(name)));
            while (cls.find()) {
                for (String token : cls.group(1).trim().split("\\s+")) {
                    if (!token.isEmpty()) {
                        written.add(token);
                    }
                }
            }
        }
        return written;
    }

    // ---- the templates ----

    @Test
    void theParametersAndTheirDefaultsAreUnchanged() throws IOException {
        assertEquals(List.of("@param String label", "@param gg.jte.Content items", "@param Icon icon = null"), declared("dropdown-menu"));
        assertEquals(List.of("@param String label", "@param String href = null"), declared("dropdown-menu-item"));
    }

    @Test
    void eachTemplateKeepsItsHooksItsAriaItsIdsItsPartsAndItsOrderExactly() throws IOException {
        assertEquals("<div class=\"ash-dropdown-menu\" data-dropdown-menu>"
                + " <button type=\"button\" id=\"${triggerId}\" data-dropdown-menu-trigger aria-haspopup=\"menu\" aria-expanded=\"false\" aria-controls=\"${menuId}\""
                + " class=\"ash-dropdown-menu__trigger\" >"
                + " @if (icon != null) <span aria-hidden=\"true\" class=\"ash-dropdown-menu__icon\"> @template.ashlar.components.icon(icon = icon, classes = \"ash-icon--sm\") </span> @endif"
                + " <span class=\"ash-dropdown-menu__label\">${label}</span>"
                + " <span data-dropdown-menu-chevron data-state=\"closed\" aria-hidden=\"true\" class=\"ash-dropdown-menu__chevron\">"
                + " @template.ashlar.components.icon(icon = Icon.CHEVRON_DOWN, classes = \"ash-icon--sm\") </span> </button>"
                + " <div id=\"${menuId}\" role=\"menu\" aria-labelledby=\"${triggerId}\" data-dropdown-menu-list data-state=\"closed\" hidden class=\"ash-dropdown-menu__list\" >"
                + " ${items} </div> </div>"
                + " <script type=\"module\" src=\"${AshlarAssets.js(\"dropdown-menu.js\")}\"></script>", markup("dropdown-menu"));
        assertEquals("@if (isLink) <a href=\"${href}\" role=\"menuitem\" tabindex=\"-1\" data-dropdown-menu-item class=\"ash-dropdown-menu-item\">${label}</a>"
                + " @else <span role=\"menuitem\" aria-disabled=\"true\" tabindex=\"-1\" data-dropdown-menu-item class=\"ash-dropdown-menu-item ash-dropdown-menu-item--disabled\">"
                + "${label}</span> @endif", markup("dropdown-menu-item"));
        assertTrue(template("dropdown-menu").contains("!{String menuId = DropdownMenuIds.next();}") && template("dropdown-menu").contains("!{String triggerId = menuId + \"-trigger\";}"),
                "the ids are DropdownMenuIds' and the trigger's is derived from the menu's");
        assertTrue(template("dropdown-menu-item").contains("!{boolean isLink = href != null && !href.isEmpty();}"), "a missing or empty href is the disabled text, as before");
    }

    @Test
    void noTailwindUtilityRemainsInTheTemplatesAndTheScriptTogglesNoClass() throws IOException {
        for (String token : written()) {
            assertTrue(token.startsWith("ash-"), "not an Ashlar class: " + token);
        }
        assertFalse(script().contains("classList") || script().contains("className"), "dropdown-menu.js toggles no class: the state is hidden and data-state");
        assertTrue(script().contains("menu.hidden = true;") && script().contains("menu.dataset.state = \"open\";") && script().contains("chevron.dataset.state = \"open\";"));
    }

    @Test
    void theScriptKeepsItsTimingItsIndependenceAndTheSignalsAnEnclosingOverlayReads() throws IOException {
        String js = script();
        assertTrue(js.contains("const TRANSITION_DURATION_MS = 150;") && js.contains("TRANSITION_DURATION_MS + 50"));
        assertFalse(js.contains("import ") || js.contains("overlay.js"), "a standalone module: no import, no overlay.js");
        assertTrue(js.contains("document.addEventListener(\"DOMContentLoaded\""));
        // overlay.js leaves an Escape to a popup whose trigger says aria-haspopup and aria-expanded="true"; Popover's document-level Escape is kept away by stopPropagation.
        assertTrue(markup("dropdown-menu").contains("aria-haspopup=\"menu\" aria-expanded=\"false\""));
        assertTrue(js.contains("trigger.setAttribute(\"aria-expanded\", \"true\");") && js.contains("trigger.setAttribute(\"aria-expanded\", \"false\");"));
        assertEquals(2, js.split("event\\.stopPropagation\\(\\);", -1).length - 1, "both Escape sites still stop the event at the root");
        assertTrue(js.contains("root.addEventListener(\"keydown\""), "the keyboard is still handled at the root, not the document");
    }

    // ---- the stylesheet ----

    @Test
    void theTwoFilesAreImportedAfterTooltipAndPopoverIntoTheComponentsLayer() throws IOException {
        String entry = read("src/main/css/ashlar.css");
        int popover = entry.indexOf("@import \"components/popover.css\" layer(ash.components);");
        int menu = entry.indexOf("@import \"components/dropdown-menu.css\" layer(ash.components);");
        int item = entry.indexOf("@import \"components/dropdown-menu-item.css\" layer(ash.components);");
        assertTrue(popover > 0 && popover < menu && menu < item, "dropdown-menu.css then dropdown-menu-item.css, after popover.css");
    }

    @Test
    void everySelectorIsScopedToItsFilesOwnClassesNothingIsImportantAndNoneIsADescendantSelector() throws IOException {
        for (String name : CSS) {
            assertFalse(css(name).replaceAll("(?s)/\\*.*?\\*/", "").contains("!important"), name);
            for (CssScanner.Rule r : rules(name)) {
                for (String selector : CssScanner.selectors(r.selector())) {
                    assertTrue(selector.matches("\\.ash-" + name + "(?![a-z-]).*|\\.ash-" + name + "__.*|\\.ash-" + name + "--.*"), name + ": not scoped to its own classes: " + selector);
                    assertFalse(selector.contains(" ") && !selector.contains(":where("), name + ": a descendant selector (each part's state is its own attribute): " + selector);
                }
            }
        }
    }

    @Test
    void everyClassTheTemplatesWriteIsDefinedOrDocumentedAsDeclarationFree() throws IOException {
        Set<String> defined = new LinkedHashSet<>();
        for (String name : CSS) {
            Matcher m = Pattern.compile("\\.(ash-dropdown-menu[a-z0-9_-]*)").matcher(css(name).replaceAll("(?s)/\\*.*?\\*/", ""));
            while (m.find()) {
                defined.add(m.group(1));
            }
        }
        Set<String> declarationFree = Set.of("ash-dropdown-menu__icon", "ash-dropdown-menu__label");
        for (String c : written()) {
            if (c.equals("ash-icon--sm")) {
                continue;
            }
            if (declarationFree.contains(c)) {
                assertFalse(defined.contains(c), c + " is documented as declaring nothing");
                assertTrue(css("dropdown-menu").contains(c), c + " is documented in the file header");
            } else {
                assertTrue(defined.contains(c), c + " is written but not defined");
            }
        }
        assertTrue(css("dropdown-menu").contains("their class is what gives them the box-sizing the Ashlar base layer gives every ash-*"), "why the two wrappers declare nothing");
    }

    @Test
    void hiddenMeansNotRenderedAndTheOpenStatesAreTheMenusAndTheChevronsOwnDataState() throws IOException {
        assertEquals(Map.of("display", "none"), declarations("dropdown-menu", ".ash-dropdown-menu__list[hidden]"));
        assertEquals(Map.of("opacity", "1", "scale", "100% 100%"), declarations("dropdown-menu", ".ash-dropdown-menu__list[data-state=\"open\"]"));
        assertEquals(Map.of("rotate", "180deg"), declarations("dropdown-menu", ".ash-dropdown-menu__chevron[data-state=\"open\"]"));
        Map<String, String> list = declarations("dropdown-menu", ".ash-dropdown-menu__list");
        assertEquals("0", list.get("opacity"));
        assertEquals("95% 95%", list.get("scale"));
    }

    @Test
    void theMenuKeepsItsPlainStaticPositionPlacementAndNothingUsesTransform() throws IOException {
        Map<String, String> list = declarations("dropdown-menu", ".ash-dropdown-menu__list");
        assertEquals("absolute", list.get("position"));
        assertEquals("0", list.get("left"));
        assertFalse(list.containsKey("top") || list.containsKey("bottom") || list.containsKey("right") || list.containsKey("inset"),
                "the menu sits at its static position (top stays auto), as before");
        assertEquals("0.375rem", list.get("margin-top"));
        assertEquals("0 0", list.get("transform-origin"));
        assertEquals("10rem", list.get("min-width"));
        assertEquals("var(--container-xs, 20rem)", list.get("max-width"));
        assertEquals(Map.of("position", "relative", "display", "inline-block"), declarations("dropdown-menu", ".ash-dropdown-menu"), "the root makes no stacking context of its own");
        for (String name : CSS) {
            for (CssScanner.Rule r : rules(name)) {
                assertFalse(CssScanner.declarations(r.block()).containsKey("transform"), name + " " + r.selector() + ": the individual scale and rotate properties, never transform");
            }
        }
        assertFalse(script().contains(".style."), "the script writes no inline style: there is no repositioning");
    }

    @Test
    void theMenusAndTheChevronsTransitionsAre150msAndOffUnderReducedMotion() throws IOException {
        int transitioning = 0;
        for (String name : CSS) {
            for (CssScanner.Rule r : rules(name)) {
                Map<String, String> d = CssScanner.declarations(r.block());
                if (r.media().contains("prefers-reduced-motion: reduce")) {
                    assertEquals("none", d.get("transition-property"), name + " " + r.selector());
                } else if (d.containsKey("transition-property")) {
                    transitioning++;
                    assertEquals("150ms", d.get("transition-duration"), name + " " + r.selector() + ": the script waits 150ms (plus 50) for an exit");
                    assertEquals("var(--ease-out, cubic-bezier(0, 0, 0.2, 1))", d.get("transition-timing-function"), name + " " + r.selector());
                    assertFalse(d.containsKey("transition-delay"), name + " " + r.selector());
                }
            }
        }
        assertEquals(2, transitioning, "the menu and the chevron");
        assertEquals("opacity, scale", declarations("dropdown-menu", ".ash-dropdown-menu__list").get("transition-property"));
        assertEquals("rotate", declarations("dropdown-menu", ".ash-dropdown-menu__chevron").get("transition-property"));
        assertEquals("none", inMedia("dropdown-menu", "prefers-reduced-motion: reduce", ".ash-dropdown-menu__chevron, .ash-dropdown-menu__list").get("transition-property"));
    }

    @Test
    void stackingTheShadowsASkinReplacesDarkModeAndTheHoverAndFocusOrder() throws IOException {
        assertEquals("10", declarations("dropdown-menu", ".ash-dropdown-menu__list").get("z-index"), "below Tooltip (20), Popover (30) and the overlay band (40)");
        assertEquals("0 0 0 1px var(--ash-dropdown-menu-list-ring), var(--ash-dropdown-menu-list-shadow)", declarations("dropdown-menu", ".ash-dropdown-menu__list").get("box-shadow"),
                "the ring is a box-shadow layer, so a skin's box-shadow on the hook replaces it with the shadow, as before");
        assertEquals("var(--ash-dropdown-menu-trigger-shadow)", declarations("dropdown-menu", ".ash-dropdown-menu__trigger").get("box-shadow"),
                "the trigger owns its drop shadow in a property a skin can flatten");
        assertEquals("0 0 0 2px var(--ash-dropdown-menu-trigger-ring), var(--ash-dropdown-menu-trigger-shadow)",
                declarations("dropdown-menu", ".ash-dropdown-menu__trigger:focus-visible").get("box-shadow"), "the focus ring is drawn before the drop shadow");
        assertEquals("button", declarations("dropdown-menu", ".ash-dropdown-menu__trigger").get("appearance"), "a <button> is a native push button, as preflight said");
        for (String selector : List.of(".ash-dropdown-menu__trigger", ".ash-dropdown-menu__chevron", ".ash-dropdown-menu__list")) {
            assertTrue(rules("dropdown-menu").stream().anyMatch(r -> r.selector().equals(selector + DARK)), selector + " has a dark rule");
        }
        for (String selector : List.of(".ash-dropdown-menu-item", ".ash-dropdown-menu-item--disabled")) {
            assertTrue(rules("dropdown-menu-item").stream().anyMatch(r -> r.selector().equals(selector + DARK)), selector + " has a dark rule");
        }
        assertEquals("var(--ash-radius-md)", declarations("dropdown-menu", ".ash-dropdown-menu__trigger").get("border-radius"));
        assertEquals("var(--ash-radius-md)", declarations("dropdown-menu", ".ash-dropdown-menu__list").get("border-radius"));
        assertEquals(Map.of("border-color", "var(--ash-dropdown-menu-trigger-border-hover)"), inMedia("dropdown-menu", "hover: hover", ".ash-dropdown-menu__trigger:hover"));
        assertEquals(Map.of("background-color", "var(--ash-dropdown-menu-item-bg-hover)"),
                inMedia("dropdown-menu-item", "hover: hover", ".ash-dropdown-menu-item:not(.ash-dropdown-menu-item--disabled):hover"));
        String item = css("dropdown-menu-item").replaceAll("(?s)/\\*.*?\\*/", "");
        assertTrue(item.indexOf(":not(.ash-dropdown-menu-item--disabled):hover") < item.indexOf(":not(.ash-dropdown-menu-item--disabled):focus"),
                "the focus rule comes after the hover rule: a focused item under the pointer shows its focus background, as the compiled Tailwind order had it");
        assertTrue(markup("dropdown-menu").contains("data-dropdown-menu-list") && markup("dropdown-menu").contains("data-dropdown-menu-trigger")
                && markup("dropdown-menu-item").contains("data-dropdown-menu-item"), "the skin hooks stay on their parts");
    }
}
