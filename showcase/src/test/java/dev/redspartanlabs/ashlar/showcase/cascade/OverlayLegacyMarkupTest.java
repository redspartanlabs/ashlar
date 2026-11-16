package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rewrite from migrated to legacy Modal and Drawer markup: the classes of every part and state, what only one form carries, and failing loudly on what it does not know. */
class OverlayLegacyMarkupTest {

    /** The class attribute (not a data-*-class one) of the first tag at or after {@code marker}. */
    private static String classOf(String html, String marker) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?<![\\w-])class=\"([^\"]*)\"").matcher(html);
        if (!m.find(html.indexOf(marker))) {
            throw new AssertionError("no class attribute after " + marker);
        }
        return m.group(1);
    }

    @Test
    void aHiddenModalBecomesTheTemplatesOwnClosedMarkup() throws Exception {
        OverlayLegacyMarkup legacy = new OverlayLegacyMarkup();
        String out = legacy.rewrite(OverlayBrowser.modal("m", "Title", OverlayBrowser.o("size", "Large"), "<p>b</p>", "<span>f</span>"));
        assertEquals(legacy.classes("modal-root"), classOf(out, "id=\"m\""));
        assertEquals(legacy.classes("modal-backdrop"), classOf(out, "data-modal-backdrop"));
        assertEquals(legacy.classes("modal-dialog-large"), classOf(out, "data-modal-dialog"));
        assertTrue(out.contains("class=\"" + legacy.classes("header") + "\""));
        assertTrue(out.contains("class=\"" + legacy.classes("title") + "\""));
        assertTrue(out.contains("class=\"" + legacy.classes("close") + "\""));
        assertTrue(out.contains("class=\"" + legacy.classes("close-icon") + "\""));
        assertTrue(out.contains("class=\"" + legacy.classes("modal-body") + "\""));
        assertTrue(out.contains("class=\"" + legacy.classes("footer") + "\""));
        assertFalse(out.contains("ash-overlay") || out.contains("ash-modal"), "no migrated class is left");
        assertFalse(out.contains("data-state") || out.matches("(?s).*\\shidden\\s*>.*"), "neither the hidden attribute nor data-state is legacy markup");
        assertTrue(out.contains("data-modal-backdrop") && out.contains("data-modal-dialog") && out.contains("data-modal-close=\"m\""), "every hook is kept");
    }

    @Test
    void anOpenModalSwapsExactlyTheClassesTheLegacyScriptSwapped() throws Exception {
        OverlayLegacyMarkup legacy = new OverlayLegacyMarkup();
        String out = legacy.rewrite(OverlayBrowser.inState(OverlayBrowser.modal("m", "Title", OverlayBrowser.o("size", "Small"), null, null), true, true));
        String root = classOf(out, "id=\"m\"");
        assertTrue(root.contains(" flex") && !root.contains("hidden"), root);
        String backdrop = classOf(out, "data-modal-backdrop");
        assertTrue(backdrop.contains("opacity-100") && !backdrop.contains("opacity-0"), backdrop);
        String dialog = classOf(out, "data-modal-dialog");
        assertTrue(dialog.contains("opacity-100") && dialog.contains("scale-100") && dialog.contains("translate-y-0") && !dialog.contains("opacity-0") && !dialog.contains("scale-95")
                && !dialog.contains("-translate-y-1") && dialog.contains("max-w-sm"), dialog);
        String shownButClosed = legacy.rewrite(OverlayBrowser.inState(OverlayBrowser.modal("m", "Title", OverlayBrowser.o(), null, null), true, false));
        assertTrue(classOf(shownButClosed, "id=\"m\"").contains(" flex") && classOf(shownButClosed, "data-modal-dialog").contains("opacity-0"), "shown, still closed");
    }

    @Test
    void aDrawerGetsItsEdgeItsStateAndBackTheClosedClassAttributeTheLegacyScriptRead() throws Exception {
        OverlayLegacyMarkup legacy = new OverlayLegacyMarkup();
        for (String placement : List.of("Left", "Right")) {
            String edge = placement.toLowerCase();
            String closed = legacy.rewrite(OverlayBrowser.drawer("d", "T", placement, "<p>b</p>", null));
            assertEquals(legacy.classes("drawer-root-" + edge), classOf(closed, "id=\"d\""));
            assertEquals(legacy.classes("drawer-panel-" + edge), classOf(closed, "data-drawer-panel"));
            assertTrue(closed.contains("class=\"" + legacy.classes("drawer-body") + "\""));
            assertTrue(closed.contains("data-drawer-closed-class=\"" + (edge.equals("left") ? "-translate-x-full" : "translate-x-full") + "\""), closed);
            String open = legacy.rewrite(OverlayBrowser.inState(OverlayBrowser.drawer("d", "T", placement, null, null), true, true));
            String panel = classOf(open, "data-drawer-panel");
            assertTrue(panel.contains("translate-x-0") && !panel.contains("translate-x-full"), panel);
        }
    }

    @Test
    void markupWithoutAnOverlayIsUntouchedAndAnUnknownOverlayClassFails() throws Exception {
        OverlayLegacyMarkup legacy = new OverlayLegacyMarkup();
        String plain = "<div class=\"ash-card\"><p class=\"x\">text</p><svg class=\"ash-icon\"></svg></div>";
        assertEquals(plain, legacy.rewrite(plain));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<div class=\"ash-overlay__unknown\"></div>"));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<div class=\"ash-overlay__backdrop\"></div>"), "a part outside any root");
    }

    @Test
    void theFixtureDefinesEveryRoleOnceAndItsTokensAreTheLegacyUtilities() throws Exception {
        OverlayLegacyMarkup legacy = new OverlayLegacyMarkup();
        assertEquals(OverlayLegacyMarkup.ROLES.size(), OverlayLegacyMarkup.ROLES.stream().distinct().count());
        assertTrue(legacy.allTokens().containsAll(List.of("bg-slate-900/50", "max-w-2xl", "-translate-x-full", "translate-x-0", "motion-reduce:transition-none", "h-5", "w-5")));
        assertTrue(legacy.allTokens().stream().noneMatch(t -> t.startsWith("ash-")), "the Icon's own class is the Icon's");
    }
}
