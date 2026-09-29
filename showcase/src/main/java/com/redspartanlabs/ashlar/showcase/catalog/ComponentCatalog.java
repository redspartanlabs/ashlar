package com.redspartanlabs.ashlar.showcase.catalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The ordered list of component showcase pages this application serves.
 *
 * <p>Deliberately contains only components that have actually been migrated
 * into the Ashlar library. The migration contract requires this: the catalog
 * grows batch by batch so the showcase always builds and the index never
 * points at a page that cannot render. Adding a component here before its
 * templates exist would break the build rather than reveal a gap.
 *
 * <p>Order is authoritative for both the index grouping and each page's
 * previous/next navigation, so a new component is one entry in one list.
 */
public final class ComponentCatalog {

    private ComponentCatalog() {
    }

    public static final List<ComponentPage> PAGES = List.of(
        // Batch 3
        new ComponentPage("checkbox", "Checkbox", "Forms"),
        new ComponentPage("toggle", "Toggle / Switch", "Forms"),
        new ComponentPage("number-input", "Number Input", "Forms"),
        new ComponentPage("radio-group", "Radio Group", "Forms"),
        // Batch 2
        new ComponentPage("text-input", "Text Input", "Forms"),
        // Batch 7
        new ComponentPage("textarea", "Textarea", "Forms"),
        new ComponentPage("file-upload", "File Upload", "Forms"),
        new ComponentPage("search-input", "Search Input", "Forms"),
        // Batch 3
        new ComponentPage("form-field", "Form Field", "Forms"),
        // Batch 1
        new ComponentPage("alert", "Alert", "Feedback"),
        // Batch 8
        new ComponentPage("toast", "Toast", "Feedback"),
        // Batch 2
        new ComponentPage("spinner", "Spinner", "Feedback"),
        new ComponentPage("progress", "Progress", "Feedback"),
        new ComponentPage("skeleton", "Skeleton", "Feedback"),
        // Batch 1
        new ComponentPage("badge", "Badge", "Content"),
        new ComponentPage("card", "Card", "Content"),
        // Batch 7
        new ComponentPage("stat-card", "Stat / Metric Card", "Content"),
        // Batch 3
        new ComponentPage("list", "List", "Content"),
        // Batch 2
        new ComponentPage("divider", "Divider", "Content"),
        // Batch 7
        new ComponentPage("empty-state", "Empty State", "Content"),
        // Batch 4
        new ComponentPage("tree", "Tree View", "Content"),
        new ComponentPage("timeline", "Timeline", "Content"),
        // Batch 1
        new ComponentPage("breadcrumbs", "Breadcrumbs", "Navigation / Overlay"),
        // Batch 3
        new ComponentPage("pagination", "Pagination", "Navigation / Overlay"),
        // Batch 1
        new ComponentPage("modal", "Modal", "Navigation / Overlay"),
        new ComponentPage("tooltip", "Tooltip", "Navigation / Overlay"),
        // Batch 8
        new ComponentPage("tabs", "Tabs", "Navigation / Overlay"),
        // Batch 2
        new ComponentPage("accordion", "Accordion", "Navigation / Overlay"),
        // Batch 4
        new ComponentPage("stepper", "Stepper", "Navigation / Overlay"),
        // Batch 5
        new ComponentPage("dropdown-menu", "Dropdown Menu", "Navigation / Overlay"),
        new ComponentPage("popover", "Popover", "Navigation / Overlay"),
        new ComponentPage("drawer", "Drawer", "Navigation / Overlay"),
        // Batch 6
        new ComponentPage("navbar", "Navbar", "Navigation / Overlay"),
        new ComponentPage("sidebar", "Sidebar", "Navigation / Overlay"),
        new ComponentPage("theme-toggle", "Theme Toggle", "Navigation / Overlay"),
        // Batch 1
        new ComponentPage("page-section", "Page Section", "Utilities")
    );

    public static ComponentPage bySlug(String slug) {
        return PAGES.stream()
                .filter(p -> p.slug().equals(slug))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown component page: " + slug));
    }

    public static Optional<ComponentPage> previous(String slug) {
        int i = indexOf(slug);
        return i <= 0 ? Optional.empty() : Optional.of(PAGES.get(i - 1));
    }

    public static Optional<ComponentPage> next(String slug) {
        int i = indexOf(slug);
        return (i < 0 || i >= PAGES.size() - 1) ? Optional.empty() : Optional.of(PAGES.get(i + 1));
    }

    /** Preserves PAGES' own insertion order within each category. */
    public static Map<String, List<ComponentPage>> byCategory() {
        Map<String, List<ComponentPage>> grouped = new LinkedHashMap<>();
        for (ComponentPage page : PAGES) {
            grouped.computeIfAbsent(page.category(), k -> new ArrayList<>()).add(page);
        }
        return grouped;
    }

    private static int indexOf(String slug) {
        for (int i = 0; i < PAGES.size(); i++) {
            if (PAGES.get(i).slug().equals(slug)) {
                return i;
            }
        }
        return -1;
    }
}
