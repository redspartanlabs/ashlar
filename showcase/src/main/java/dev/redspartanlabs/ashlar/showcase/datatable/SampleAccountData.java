package dev.redspartanlabs.ashlar.showcase.datatable;

import dev.redspartanlabs.ashlar.datatable.DataTableColumn;
import dev.redspartanlabs.ashlar.datatable.DataTableRow;

import java.util.List;
import java.util.Map;

/**
 * Demonstration data for the Data Table showcase. Invented records, not
 * connected to anything.
 *
 * <p>This class is where the application-side work lives, and that is the
 * point: it formats dates and numbers for display, supplies the machine
 * values those columns sort by, and maps its own status vocabulary onto the
 * Badge component's variants. None of that is the table's knowledge -
 * "Active means success" is a fact about this data, not about tables.
 */
public final class SampleAccountData {

    private SampleAccountData() {
    }

    /** The application's own status vocabulary, mapped to Badge variants. */
    private static final Map<String, String> STATUS_VARIANTS = Map.of(
            "Active", "Success",
            "Pending", "Warning",
            "Inactive", "Neutral",
            "Error", "Danger"
    );

    public static List<DataTableColumn> columns() {
        return List.of(
                DataTableColumn.mono("id", "Account"),
                DataTableColumn.of("name", "Account name"),
                DataTableColumn.badge("status", "Status", STATUS_VARIANTS),
                DataTableColumn.of("type", "Type"),
                DataTableColumn.date("created", "Created"),
                DataTableColumn.number("seats", "Seats"),
                DataTableColumn.number("annualValue", "Annual value"),
                DataTableColumn.text("owner", "Owner", false)
        );
    }

    public static List<DataTableRow> rows() {
        return List.of(
                row("ACC-1001", "Acme Manufacturing", "Active", "Enterprise", "January 12, 2024", "2024-01-12", "1,250", "1250", "$482,000", "482000", "Dana Whitfield"),
                row("ACC-1002", "Beacon Health Partners", "Pending", "Government", "February 3, 2024", "2024-02-03", "340", "340", "$96,500", "96500", "Marcus Lee"),
                row("ACC-1003", "Cascade Logistics", "Active", "Enterprise", "November 22, 2023", "2023-11-22", "2,480", "2480", "$1,204,000", "1204000", "Priya Natarajan"),
                row("ACC-1004", "Dunmore Legal Group", "Inactive", "SMB", "August 9, 2022", "2022-08-09", "18", "18", "$7,200", "7200", "Priya Natarajan"),
                row("ACC-1005", "Everline Insurance", "Error", "Enterprise", "March 15, 2024", "2024-03-15", "905", "905", "$318,400", "318400", "Dana Whitfield"),
                row("ACC-1006", "Fairwind Energy", "Active", "Government", "May 30, 2023", "2023-05-30", "612", "612", "$204,750", "204750", "Sofia Marchetti"),
                row("ACC-1007", "Granite Peak Bank", "Pending", "Enterprise", "April 1, 2024", "2024-04-01", "1,004", "1004", "$540,900", "540900", "Marcus Lee"),
                row("ACC-1008", "Harborview Retail", "Active", "SMB", "September 18, 2023", "2023-09-18", "76", "76", "$24,300", "24300", "Sofia Marchetti"),
                // Blank owner and no recorded value: the table renders an em
                // dash and announces "No value" rather than leaving a hole.
                row("ACC-1009", "Ironclad Security Consultants of the Pacific Northwest", "Inactive", "Enterprise", "December 5, 2022", "2022-12-05", "", "", "", "", ""),
                row("ACC-1010", "Juniper Nonprofit Alliance", "Active", "Non-Profit", "January 27, 2024", "2024-01-27", "45", "45", "$11,900", "11900", "Priya Natarajan"),
                row("ACC-1011", "Kestrel Aerospace", "Error", "Government", "July 14, 2023", "2023-07-14", "3,120", "3120", "$2,015,600", "2015600", "Marcus Lee"),
                row("ACC-1012", "Lonestar Utilities", "Active", "Enterprise", "October 2, 2023", "2023-10-02", "884", "884", "$372,100", "372100", "Sofia Marchetti"),
                row("ACC-1013", "Meridian Freight", "Pending", "SMB", "February 19, 2024", "2024-02-19", "129", "129", "$38,600", "38600", "Dana Whitfield"),
                row("ACC-1014", "Northgate University", "Active", "Non-Profit", "June 8, 2023", "2023-06-08", "2,006", "2006", "$688,250", "688250", "Priya Natarajan")
        );
    }

    /** The same shape with no records, for the empty-state demonstration. */
    public static List<DataTableRow> noRows() {
        return List.of();
    }

    private static DataTableRow row(
            String id, String name, String status, String type,
            String createdDisplay, String createdSort,
            String seatsDisplay, String seatsSort,
            String valueDisplay, String valueSort,
            String owner
    ) {
        return new DataTableRow(
                Map.of(
                        "id", id,
                        "name", name,
                        "status", status,
                        "type", type,
                        "created", createdDisplay,
                        "seats", seatsDisplay,
                        "annualValue", valueDisplay,
                        "owner", owner
                ),
                Map.of(
                        "created", createdSort,
                        "seats", seatsSort,
                        "annualValue", valueSort
                )
        );
    }
}
