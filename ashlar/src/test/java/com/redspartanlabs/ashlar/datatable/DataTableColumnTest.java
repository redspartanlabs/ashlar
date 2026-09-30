package com.redspartanlabs.ashlar.datatable;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataTableColumnTest {

    @Test
    void ofIsSortableTextLeftAligned() {
        DataTableColumn column = DataTableColumn.of("id", "Account");

        assertEquals("id", column.key());
        assertEquals("Account", column.label());
        assertTrue(column.sortable());
        assertEquals(Align.START, column.align());
        assertEquals(Sort.TEXT, column.sort());
        assertEquals(Presentation.TEXT, column.presentation());
    }

    @Test
    void textHonorsTheSortableFlagItWasGiven() {
        assertTrue(DataTableColumn.text("owner", "Owner", true).sortable());
        assertFalse(DataTableColumn.text("owner", "Owner", false).sortable());
    }

    @Test
    void monoIsPresentedMono() {
        DataTableColumn column = DataTableColumn.mono("id", "Account");

        assertEquals(Presentation.MONO, column.presentation());
        assertTrue(column.isMono());
        assertFalse(column.isBadge());
    }

    @Test
    void numberIsRightAlignedAndComparedNumerically() {
        DataTableColumn column = DataTableColumn.number("seats", "Seats");

        assertEquals(Align.END, column.align());
        assertEquals(Sort.NUMBER, column.sort());
    }

    @Test
    void dateIsLeftAlignedAndComparedChronologically() {
        DataTableColumn column = DataTableColumn.date("created", "Created");

        assertEquals(Align.START, column.align());
        assertEquals(Sort.DATE, column.sort());
    }

    @Test
    void badgeIsPresentedAsBadge() {
        DataTableColumn column = DataTableColumn.badge("status", "Status", Map.of("Active", "Success"));

        assertEquals(Presentation.BADGE, column.presentation());
        assertTrue(column.isBadge());
        assertFalse(column.isMono());
    }

    @Test
    void badgeVariantForReturnsTheMappedVariant() {
        DataTableColumn column = DataTableColumn.badge("status", "Status", Map.of("Active", "Success"));

        assertEquals("Success", column.badgeVariantFor("Active"));
    }

    @Test
    void badgeVariantForFallsBackToNeutralWhenUnmapped() {
        DataTableColumn column = DataTableColumn.badge("status", "Status", Map.of("Active", "Success"));

        assertEquals("Neutral", column.badgeVariantFor("Archived"));
    }

    @Test
    void nullBadgeVariantsBecomesAnEmptyMapRatherThanNull() {
        DataTableColumn column = new DataTableColumn(
                "status", "Status", true, Align.START, Sort.TEXT, Presentation.BADGE, null);

        assertEquals(Map.of(), column.badgeVariants());
        assertEquals("Neutral", column.badgeVariantFor("anything"));
    }

    @Test
    void badgeVariantsIsDefensivelyCopiedAndImmutable() {
        Map<String, String> source = new HashMap<>(Map.of("Active", "Success"));
        DataTableColumn column = DataTableColumn.badge("status", "Status", source);

        source.put("Pending", "Warning");

        assertEquals("Neutral", column.badgeVariantFor("Pending"));
        assertThrows(UnsupportedOperationException.class, () -> column.badgeVariants().put("x", "y"));
    }
}
