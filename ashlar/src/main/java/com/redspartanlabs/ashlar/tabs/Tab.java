package com.redspartanlabs.ashlar.tabs;

import com.redspartanlabs.ashlar.icon.Icon;

/**
 * Describes one tab button's metadata for the reusable Tabs component: its
 * identity, label, optional icon/badge, and disabled state.
 *
 * <p>This deliberately carries no panel content. JTE's content-block syntax
 * ({@code @`...`}) can only be passed as a direct argument to a
 * {@code @template.x(...)} call - it cannot be nested inside a Java method
 * chain like {@code Tab.of(...).content(@`...`)}. So panel markup is
 * supplied separately, one {@code @template.components.tab-panel(...)} call
 * per tab, rather than being attached to this object.
 */
public final class Tab {

    private final String id;
    private final String label;
    private Icon icon;
    private String badge;
    private boolean disabled;

    private Tab(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public static Tab of(String id, String label) {
        return new Tab(id, label);
    }

    public Tab icon(Icon icon) {
        this.icon = icon;
        return this;
    }

    public Tab badge(String badge) {
        this.badge = badge;
        return this;
    }

    public Tab disabled(boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public Icon icon() {
        return icon;
    }

    public String badge() {
        return badge;
    }

    public boolean disabled() {
        return disabled;
    }
}
