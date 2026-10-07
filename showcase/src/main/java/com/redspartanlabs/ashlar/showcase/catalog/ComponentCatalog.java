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
 *
 * <p>Deterministic ordering (documentation-site polish pass): categories
 * alphabetical (Content, Feedback, Forms, Navigation / Overlay, Utilities),
 * and within each category, entries alphabetical by their display
 * {@code name()} - not by slug, not by migration batch. This list is the
 * single place that ordering is decided; {@link #byCategory()} and
 * {@link #previous(String)}/{@link #next(String)} both derive from this
 * same list's position, so sorting it once is sufficient everywhere.
 */
public final class ComponentCatalog {

    private ComponentCatalog() {
    }

    public static final List<ComponentPage> PAGES = List.of(
        // Content
        new ComponentPage("badge", "Badge", "Content"),
        new ComponentPage("card", "Card", "Content"),
        new ComponentPage("data-table", "Data Table", "Content", "The model-driven table: declare columns, hand over rows, get sorting, pagination, search, and selection for free."),
        new ComponentPage("divider", "Divider", "Content"),
        new ComponentPage("empty-state", "Empty State", "Content"),
        new ComponentPage("icon", "Icon", "Content", "The most-composed primitive in the library - bundled, stroke-only SVG artwork with the full set shown in place."),
        new ComponentPage("list", "List", "Content"),
        new ComponentPage("stat-card", "Stat / Metric Card", "Content"),
        new ComponentPage("table", "Table", "Content"),
        new ComponentPage("timeline", "Timeline", "Content"),
        new ComponentPage("tree", "Tree View", "Content"),

        // Feedback
        new ComponentPage("alert", "Alert", "Feedback"),
        new ComponentPage("progress", "Progress", "Feedback"),
        new ComponentPage("skeleton", "Skeleton", "Feedback"),
        new ComponentPage("spinner", "Spinner", "Feedback"),
        new ComponentPage("toast", "Toast", "Feedback"),

        // Forms
        new ComponentPage("button", "Button", "Forms", "The action primitive: variants, sizes, icons, loading, and a self-contained toggle mode."),
        new ComponentPage("checkbox", "Checkbox", "Forms"),
        new ComponentPage("combobox", "Combobox", "Forms"),
        new ComponentPage("date-picker", "Date Picker", "Forms"),
        new ComponentPage("date-range-picker", "Date Range Picker", "Forms"),
        new ComponentPage("faceted-search", "Faceted Search", "Forms"),
        new ComponentPage("file-upload", "File Upload", "Forms"),
        new ComponentPage("form-field", "Form Field", "Forms"),
        new ComponentPage("number-input", "Number Input", "Forms"),
        new ComponentPage("radio-group", "Radio Group", "Forms"),
        new ComponentPage("search-input", "Search Input", "Forms"),
        new ComponentPage("select", "Select List", "Forms", "A custom listbox dropdown backed by a real, hidden <select> - full keyboard support, type-ahead, and native form submission."),
        new ComponentPage("text-input", "Text Input", "Forms"),
        new ComponentPage("textarea", "Textarea", "Forms"),
        new ComponentPage("toggle", "Toggle / Switch", "Forms"),

        // Navigation / Overlay
        new ComponentPage("accordion", "Accordion", "Navigation / Overlay"),
        new ComponentPage("breadcrumbs", "Breadcrumbs", "Navigation / Overlay"),
        new ComponentPage("drawer", "Drawer", "Navigation / Overlay"),
        new ComponentPage("dropdown-menu", "Dropdown Menu", "Navigation / Overlay"),
        new ComponentPage("modal", "Modal", "Navigation / Overlay", "Accessible dialogs with focus trapping, Escape/backdrop control, and scroll locking."),
        new ComponentPage("navbar", "Navbar", "Navigation / Overlay"),
        new ComponentPage("nav-link", "NavLink", "Navigation / Overlay", "One entry in a navigation link list - a real link, or non-interactive current-page text."),
        new ComponentPage("pagination", "Pagination", "Navigation / Overlay"),
        new ComponentPage("popover", "Popover", "Navigation / Overlay"),
        new ComponentPage("sidebar", "Sidebar", "Navigation / Overlay"),
        new ComponentPage("stepper", "Stepper", "Navigation / Overlay"),
        new ComponentPage("tabs", "Tabs", "Navigation / Overlay"),
        new ComponentPage("theme-toggle", "Theme Toggle", "Navigation / Overlay"),
        new ComponentPage("tooltip", "Tooltip", "Navigation / Overlay"),

        // Utilities
        new ComponentPage("filter-bar", "Filter Bar", "Utilities"),
        new ComponentPage("footer", "Footer", "Utilities"),
        new ComponentPage("form-actions", "Form Actions", "Utilities"),
        new ComponentPage("key-value-list", "Key-Value List", "Utilities"),
        new ComponentPage("page-header", "Page Header", "Utilities"),
        new ComponentPage("page-section", "Page Section", "Utilities"),
        new ComponentPage("page-shell", "Page Shell", "Utilities"),
        new ComponentPage("status-line", "Status Line", "Utilities")
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
