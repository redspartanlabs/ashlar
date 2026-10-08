package dev.redspartanlabs.ashlar.tabs;

import java.util.List;

public final class TabsSupport {

    private TabsSupport() {
    }

    /**
     * Resolves which tab should render active initially: the requested tab
     * if it exists and isn't disabled, otherwise the first enabled tab,
     * otherwise just the first tab of any kind (so an all-disabled group
     * still renders one coherent, if inert, initial state rather than none).
     */
    public static String resolveActiveTabId(List<Tab> tabs, String requestedId) {
        for (Tab tab : tabs) {
            if (tab.id().equals(requestedId) && !tab.disabled()) {
                return tab.id();
            }
        }
        for (Tab tab : tabs) {
            if (!tab.disabled()) {
                return tab.id();
            }
        }
        return tabs.isEmpty() ? "" : tabs.get(0).id();
    }
}
