package com.redspartanlabs.ashlar.showcase.table;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Demonstration data for the Table showcase, shaped like the batch job
 * records a migration review screen actually deals with - an identifier, a
 * description, an owner, a state, a submission date and a row count.
 *
 * <p>It lives here rather than in the component because the component never
 * sees a data model: Table is handed rendered rows and presents them. This
 * class stands in for the application's own service/view-model layer, and it
 * is also where the two jobs that layer really does are done - formatting a
 * date for display and grouping a status into a Badge variant. Neither
 * belongs to the table.
 *
 * <p>The records are invented. Nothing here comes from a real system.
 */
public record SampleJobHeader(
        String jobNumber,
        String description,
        String owner,
        String status,
        LocalDate submitted,
        long records
) {

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("MMM d, yyyy");

    /** The date as a person reads it. */
    public String submittedDisplay() {
        return submitted.format(DISPLAY);
    }

    /** The same date as a machine sorts it - handed to the cell's sortValue. */
    public String submittedIso() {
        return submitted.toString();
    }

    public String recordsDisplay() {
        return String.format("%,d", records);
    }

    public String recordsSortValue() {
        return String.valueOf(records);
    }

    /** Maps a domain status onto the Badge vocabulary the library already has. */
    public String badgeVariant() {
        return switch (status) {
            case "Completed" -> "Success";
            case "Running" -> "Info";
            case "Failed" -> "Danger";
            case "Cancelled" -> "Warning";
            default -> "Neutral";
        };
    }

    public static List<SampleJobHeader> sample() {
        return List.of(
                new SampleJobHeader("JH-2026-0412", "Nightly inventory extract", "D. Whitfield", "Completed", LocalDate.of(2026, 3, 14), 1_204_981),
                new SampleJobHeader("JH-2026-0411", "Vendor payment reconciliation", "M. Okafor", "Completed", LocalDate.of(2026, 3, 14), 84_220),
                new SampleJobHeader("JH-2026-0410", "Asset depreciation rollup", "P. Natarajan", "Running", LocalDate.of(2026, 3, 13), 512_640),
                new SampleJobHeader("JH-2026-0409", "Quarterly compliance export for enterprise accounts", "D. Whitfield", "Queued", LocalDate.of(2026, 3, 13), 0),
                new SampleJobHeader("JH-2026-0408", "Work order status sync", "L. Sørensen", "Failed", LocalDate.of(2026, 3, 12), 3_918),
                new SampleJobHeader("JH-2026-0407", "Purchase requisition import", "M. Okafor", "Completed", LocalDate.of(2026, 3, 12), 27_405),
                new SampleJobHeader("JH-2026-0406", "Cost center hierarchy refresh", "A. Ibarra", "Completed", LocalDate.of(2026, 3, 11), 1_802),
                new SampleJobHeader("JH-2026-0405", "Timesheet approval batch", "P. Natarajan", "Cancelled", LocalDate.of(2026, 3, 11), 0),
                new SampleJobHeader("JH-2026-0404", "Supplier catalog delta load", "A. Ibarra", "Completed", LocalDate.of(2026, 3, 10), 96_338),
                new SampleJobHeader("JH-2026-0403", "Material master cleanup", "L. Sørensen", "Completed", LocalDate.of(2026, 3, 10), 44_117),
                new SampleJobHeader("JH-2026-0402", "Receiving document archive", "D. Whitfield", "Completed", LocalDate.of(2026, 3, 9), 238_064),
                new SampleJobHeader("JH-2026-0401", "General ledger period close", "M. Okafor", "Completed", LocalDate.of(2026, 3, 9), 7_509),
                new SampleJobHeader("JH-2026-0400", "Equipment calibration audit", "A. Ibarra", "Running", LocalDate.of(2026, 3, 8), 611),
                new SampleJobHeader("JH-2026-0399", "Shipment manifest reconciliation", "P. Natarajan", "Completed", LocalDate.of(2026, 3, 8), 152_770)
        );
    }
}
