package dev.redspartanlabs.ashlar.tabs;

import dev.redspartanlabs.ashlar.icon.Icon;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The public Java API of Tabs, which the migration does not change: the {@link Tab} builder and {@link TabsSupport#resolveActiveTabId}. */
class TabsSupportTest {

    private static Tab tab(String id, boolean disabled) {
        return Tab.of(id, id.toUpperCase()).disabled(disabled);
    }

    @Test
    void aRequestedEnabledTabIsTheActiveOne() {
        List<Tab> tabs = List.of(tab("a", false), tab("b", false), tab("c", false));
        assertEquals("b", TabsSupport.resolveActiveTabId(tabs, "b"));
        assertEquals("c", TabsSupport.resolveActiveTabId(tabs, "c"));
    }

    @Test
    void anUnmatchedOrEmptyRequestFallsBackToTheFirstEnabledTab() {
        List<Tab> tabs = List.of(tab("a", true), tab("b", false), tab("c", false));
        assertEquals("b", TabsSupport.resolveActiveTabId(tabs, ""));
        assertEquals("b", TabsSupport.resolveActiveTabId(tabs, "nope"));
        assertEquals("b", TabsSupport.resolveActiveTabId(tabs, null));
    }

    @Test
    void aRequestedDisabledTabIsNeverEligibleAndTheFirstEnabledOneIsUsed() {
        List<Tab> tabs = List.of(tab("a", false), tab("b", true), tab("c", false));
        assertEquals("a", TabsSupport.resolveActiveTabId(tabs, "b"));
        assertEquals("c", TabsSupport.resolveActiveTabId(List.of(tab("a", true), tab("b", true), tab("c", false)), "a"));
    }

    @Test
    void whenEveryTabIsDisabledTheFirstTabOfAnyKindIsTheActiveOne() {
        List<Tab> tabs = List.of(tab("a", true), tab("b", true), tab("c", true));
        assertEquals("a", TabsSupport.resolveActiveTabId(tabs, ""));
        assertEquals("a", TabsSupport.resolveActiveTabId(tabs, "c"), "even a requested disabled tab is not honoured");
    }

    @Test
    void noTabsResolveToAnEmptyId() {
        assertEquals("", TabsSupport.resolveActiveTabId(List.of(), "a"));
    }

    @Test
    void theBuilderIsFluentAndDefaultsToNoIconNoBadgeAndEnabled() {
        Tab plain = Tab.of("id", "Label");
        assertEquals("id", plain.id());
        assertEquals("Label", plain.label());
        assertNull(plain.icon());
        assertNull(plain.badge());
        assertFalse(plain.disabled());
        Tab full = Tab.of("id", "Label");
        assertSame(full, full.icon(Icon.USER));
        assertSame(full, full.badge("4"));
        assertSame(full, full.disabled(true));
        assertEquals(Icon.USER, full.icon());
        assertEquals("4", full.badge());
        assertTrue(full.disabled());
    }

    @Test
    void thePublicSurfaceIsUnchangedAndNamesNoClass() {
        Set<String> tab = Arrays.stream(Tab.class.getDeclaredMethods()).filter(m -> Modifier.isPublic(m.getModifiers())).map(m -> m.getName() + "/" + m.getParameterCount())
                .collect(Collectors.toSet());
        assertEquals(Set.of("of/2", "icon/1", "badge/1", "disabled/1", "id/0", "label/0", "icon/0", "badge/0", "disabled/0"), tab);
        Set<String> support = Arrays.stream(TabsSupport.class.getDeclaredMethods()).filter(m -> Modifier.isPublic(m.getModifiers())).map(m -> m.getName() + "/" + m.getParameterCount())
                .collect(Collectors.toSet());
        assertEquals(Set.of("resolveActiveTabId/2"), support);
        assertTrue(Modifier.isFinal(Tab.class.getModifiers()) && Modifier.isFinal(TabsSupport.class.getModifiers()));
    }
}
