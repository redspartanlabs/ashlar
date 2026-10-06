package com.redspartanlabs.ashlar.showcase.catalog;

/**
 * One entry in the showcase's component index: its URL slug, display name, the
 * category it is grouped under, and an optional one-line description.
 *
 * <p>Showcase-side only. The catalog describes what this demo application
 * chooses to display; it is not part of the Ashlar library.
 *
 * <p>{@code description} defaults to empty via the three-argument constructor
 * so existing {@code PAGES} entries need no changes as pages gain a real one
 * batch by batch - an entry with no description simply omits it wherever the
 * index or a page's own meta description would otherwise show it.
 */
public record ComponentPage(String slug, String name, String category, String description) {

    public ComponentPage(String slug, String name, String category) {
        this(slug, name, category, "");
    }
}
