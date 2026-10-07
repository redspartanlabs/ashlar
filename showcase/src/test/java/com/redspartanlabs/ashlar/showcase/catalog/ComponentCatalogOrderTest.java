package com.redspartanlabs.ashlar.showcase.catalog;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards the one ordering guarantee {@link ComponentCatalog} makes:
 * categories alphabetical, and within each category, entries alphabetical
 * by display name - so a future entry added out of order (the exact
 * regression this polish pass fixed) fails the build instead of silently
 * reintroducing migration-batch ordering.
 */
class ComponentCatalogOrderTest {

    @Test
    void categoriesAppearInAlphabeticalOrder() {
        List<String> categoriesInOrder = new ArrayList<>(new LinkedHashSet<>(
                ComponentCatalog.PAGES.stream().map(ComponentPage::category).toList()));

        List<String> sorted = categoriesInOrder.stream().sorted(String::compareToIgnoreCase).toList();

        assertEquals(sorted, categoriesInOrder);
    }

    @Test
    void entriesWithinEachCategoryAreAlphabeticalByName() {
        for (List<ComponentPage> pagesInCategory : ComponentCatalog.byCategory().values()) {
            List<String> namesInOrder = pagesInCategory.stream().map(ComponentPage::name).toList();
            List<String> sorted = namesInOrder.stream().sorted(String::compareToIgnoreCase).toList();

            assertEquals(sorted, namesInOrder);
        }
    }
}
