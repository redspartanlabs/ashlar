package com.redspartanlabs.ashlar.showcase.catalog;

/**
 * One entry in the showcase's component index: its URL slug, display name, and
 * the category it is grouped under.
 *
 * <p>Showcase-side only. The catalog describes what this demo application
 * chooses to display; it is not part of the Ashlar library.
 */
public record ComponentPage(String slug, String name, String category) {
}
