package dev.redspartanlabs.ashlar.showcase.cascade;

import dev.redspartanlabs.ashlar.icon.Icon;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rewrite from migrated Dropdown Menu markup back to the legacy markup, on the real templates: every part gets exactly the class string its pre-migration template wrote
 * (read from the fixture, which names the commit, the blobs and the lines), the two wrappers that had no class lose theirs, the state attributes become the classes the legacy
 * script toggled, and nothing else changes.
 */
class DropdownMenuLegacyMarkupTest {

    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("(?<![\\w-])class=\"([^\"]*)\"");

    /** The class attribute of the first tag at or after {@code marker}. */
    private static String classOf(String html, String marker) {
        Matcher m = CLASS_ATTRIBUTE.matcher(html);
        int at = html.indexOf(marker);
        if (at < 0 || !m.find(at)) {
            throw new AssertionError("no class attribute after " + marker);
        }
        return m.group(1);
    }

    /** Every class value, in document order. */
    private static List<String> classes(String html) {
        List<String> all = new ArrayList<>();
        Matcher m = CLASS_ATTRIBUTE.matcher(html);
        while (m.find()) {
            all.add(m.group(1));
        }
        return all;
    }

    @Test
    void aClosedMenuBecomesThePreMigrationTemplatesOwnMarkupPartByPart() throws Exception {
        DropdownMenuLegacyMarkup legacy = new DropdownMenuLegacyMarkup();
        String out = legacy.rewrite(OverlayBrowser.withoutScripts(DropdownMenuBrowser.menu("Actions", Icon.SETTINGS, DropdownMenuBrowser.ACTIONS)));
        // In document order: the root, the trigger, the icon (left to IconLegacyMarkup), the chevron's icon after the chevron, the menu, the three links and the disabled entry.
        assertEquals(List.of(legacy.classes("root"), legacy.classes("trigger"), "ash-icon ash-icon--sm", legacy.classes("chevron"), "ash-icon ash-icon--sm", legacy.classes("list"),
                legacy.classes("item-link"), legacy.classes("item-link"), legacy.classes("item-disabled"), legacy.classes("item-link")), classes(out));
        assertTrue(out.contains("<span aria-hidden=\"true\">") && out.contains("<span>Actions</span>"), "the label and icon wrappers have no class, as before");
        assertFalse(out.contains("ash-dropdown-menu"), "no migrated class is left");
        // Read outside the class values: the legacy menu's own class string holds Tailwind's hidden class, which is what the legacy markup is meant to have.
        String attributes = CLASS_ATTRIBUTE.matcher(out).replaceAll("");
        assertFalse(attributes.contains("data-state") || Pattern.compile("\\shidden(?=[\\s>])").matcher(attributes).find(), "neither data-state nor the hidden attribute is legacy markup");
        for (String hook : List.of("data-dropdown-menu>", "data-dropdown-menu-trigger", "data-dropdown-menu-chevron", "data-dropdown-menu-list", "data-dropdown-menu-item",
                "aria-haspopup=\"menu\"", "aria-expanded=\"false\"", "aria-controls=\"dropdown-menu-", "role=\"menu\"", "aria-labelledby=\"dropdown-menu-", "role=\"menuitem\"",
                "aria-disabled=\"true\"", "tabindex=\"-1\"")) {
            assertTrue(out.contains(hook), "kept: " + hook);
        }
    }

    @Test
    void anOpenMenuLosesHiddenSwapsExactlyTheClassesTheLegacyScriptSwappedAndTurnsTheChevron() throws Exception {
        DropdownMenuLegacyMarkup legacy = new DropdownMenuLegacyMarkup();
        String html = OverlayBrowser.withoutScripts(DropdownMenuBrowser.menu("Actions", null, DropdownMenuBrowser.ACTIONS));
        String open = legacy.rewrite(DropdownMenuBrowser.inState(html, true, true));
        String list = classOf(open, "data-dropdown-menu-list");
        assertTrue(list.contains("opacity-100") && list.contains("scale-100") && !list.contains("opacity-0") && !list.contains("scale-95") && !list.matches(".*(^| )hidden( |$).*"), list);
        assertEquals(legacy.classes("chevron") + " " + legacy.classes("chevron-open"), classOf(open, "data-dropdown-menu-chevron"));
        assertTrue(open.contains("aria-expanded=\"true\""));
        String shownButClosed = classOf(legacy.rewrite(DropdownMenuBrowser.inState(html, true, false)), "data-dropdown-menu-list");
        assertTrue(shownButClosed.contains("opacity-0") && shownButClosed.contains("scale-95") && !shownButClosed.matches(".*(^| )hidden( |$).*"), shownButClosed);
        String hidden = classOf(legacy.rewrite(html), "data-dropdown-menu-list");
        assertEquals(legacy.classes("list"), hidden);
    }

    @Test
    void theFullLegacyRenderingAlsoWritesTheIconsBackAsThePreMigrationTemplateDid() throws Exception {
        String out = DropdownMenuBrowser.legacy(OverlayBrowser.withoutScripts(DropdownMenuBrowser.menu("Settings", Icon.SETTINGS, "General|#g")));
        // The template passed classes="h-4 w-4" to the Icon, whose legacy rendering is its base classes and that size: the same as ash-icon ash-icon--sm's.
        assertEquals(new IconLegacyMarkup().legacyFor("ash-icon h-4 w-4"), classOf(out, "<svg"));
        assertEquals(new IconLegacyMarkup().legacyFor("ash-icon ash-icon--sm"), classOf(out, "<svg"));
        assertFalse(out.contains("ash-"), "no Ashlar class is left at all: " + out);
    }

    @Test
    void markupWithoutADropdownMenuIsUntouchedAndAnUnknownClassFails() throws Exception {
        DropdownMenuLegacyMarkup legacy = new DropdownMenuLegacyMarkup();
        String plain = "<div class=\"ash-card\"><p class=\"x\">text</p><span class=\"ash-popover__body\"></span></div>";
        assertEquals(plain, legacy.rewrite(plain));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<div class=\"ash-dropdown-menu__unknown\"></div>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<div class=\"ash-dropdown-menu__list\"></div>"), "a menu without data-state");
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<span class=\"ash-dropdown-menu__chevron\"></span>"), "a chevron without data-state");
    }

    @Test
    void theFixtureDefinesEveryRoleOnceAndItsTokensAreTheLegacyUtilities() throws Exception {
        DropdownMenuLegacyMarkup legacy = new DropdownMenuLegacyMarkup();
        assertEquals(DropdownMenuLegacyMarkup.ROLES.size(), DropdownMenuLegacyMarkup.ROLES.stream().distinct().count());
        assertTrue(legacy.allTokens().containsAll(List.of("min-w-40", "max-w-xs", "origin-top-left", "mt-1.5", "shadow-sm", "ring-1", "ring-slate-200", "motion-reduce:transition-none",
                "focus:bg-slate-100", "dark:focus:bg-slate-700", "opacity-100", "scale-100", "rotate-180")));
        assertTrue(legacy.allTokens().stream().noneMatch(t -> t.startsWith("ash-")), "the legacy markup has no Ashlar class");
    }
}
