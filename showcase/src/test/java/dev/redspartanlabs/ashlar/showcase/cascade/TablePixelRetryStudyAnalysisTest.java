package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TEMPORARY DIAGNOSTIC, not for merge: the retry study's analysis on synthetic pictures, and the study's planned counts. No browser. */
class TablePixelRetryStudyAnalysisTest {

    private final TablePixelRetryStudy.Pictures pictures = new TablePixelRetryStudy.Pictures();
    private final String a;
    private final String b;

    TablePixelRetryStudyAnalysisTest() throws IOException {
        a = pictures.add(png(-1));
        b = pictures.add(png(0xFFEEEEF1));
    }

    private static String png(int pixel) throws IOException {
        BufferedImage image = new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 20; x++) {
                image.setRGB(x, y, 0xFFEEEEEE);
            }
        }
        if (pixel != -1) {
            image.setRGB(3, 4, pixel);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    /** A pair of the exact case "plain" (any differing pixel is rejected). */
    private static TablePixelRetryStudy.Pair pair(String mode, int retry, int process, String legacy, String migrated) {
        return new TablePixelRetryStudy.Pair("unit", mode, retry, process, "light/x", 1, "plain", "tb1", 1, legacy, migrated, "", true, true, true);
    }

    private TablePixelRetryStudy.Procedure procedure() {
        List<TablePixelRetryStudy.Pair> retries = List.of(
                pair("case-only", 1, 2, a, b), pair("case-only", 2, 4, a, a), pair("case-only", 3, 6, b, b),
                pair("block-replay", 1, 3, b, a), pair("block-replay", 2, 5, b, b), pair("block-replay", 3, 7, a, b));
        return new TablePixelRetryStudy.Procedure("procedure 1", pair("primary", 0, 1, a, b), retries);
    }

    // ---------------------------------------------------------------- uncertainty

    @Test
    void theExactIntervalsMatchPublishedValues() {
        double[] zero = TablePixelRetryStudy.interval(0, 10);
        assertEquals(0.0, zero[0], 1e-9);
        assertEquals(0.3085, zero[1], 1e-3);
        double[] half = TablePixelRetryStudy.interval(5, 10);
        assertEquals(0.1871, half[0], 1e-3);
        assertEquals(0.8129, half[1], 1e-3);
        double[] all = TablePixelRetryStudy.interval(10, 10);
        assertEquals(0.6915, all[0], 1e-3);
        assertEquals(1.0, all[1], 1e-9);
        assertEquals("1 of 4 (95% interval 0.006-0.806)", TablePixelRetryStudy.rate(1, 4));
    }

    // ---------------------------------------------------------------- the policies

    @Test
    void eachPolicyReadsTheRetriesOfItsOwnModeAsNestedPrefixes() {
        TablePixelRetryStudy.Procedure pr = procedure();
        // case-only: retry 1 (a,b) rejected, retry 2 (a,a) accepted, retry 3 (b,b) accepted
        assertFalse(TablePixelRetryStudy.accepts(pr, pictures, "P1 both sides", "case-only", 1));
        assertTrue(TablePixelRetryStudy.accepts(pr, pictures, "P1 both sides", "case-only", 2));
        // P2 keeps the primary legacy a: against retry migrated b (rejected), then a (accepted)
        assertFalse(TablePixelRetryStudy.accepts(pr, pictures, "P2 migrated only", "case-only", 1));
        assertTrue(TablePixelRetryStudy.accepts(pr, pictures, "P2 migrated only", "case-only", 2));
        // P3 keeps the primary migrated b: retry legacy a, a (rejected), then b (accepted at the third retry)
        assertFalse(TablePixelRetryStudy.accepts(pr, pictures, "P3 legacy only", "case-only", 2));
        assertTrue(TablePixelRetryStudy.accepts(pr, pictures, "P3 legacy only", "case-only", 3));
        // block-replay: retry 1 (b,a) rejected for P1, but a against a for P2 and b against b for P3
        assertFalse(TablePixelRetryStudy.accepts(pr, pictures, "P1 both sides", "block-replay", 1));
        assertTrue(TablePixelRetryStudy.accepts(pr, pictures, "P1 both sides", "block-replay", 2));
        assertTrue(TablePixelRetryStudy.accepts(pr, pictures, "P2 migrated only", "block-replay", 1));
        assertTrue(TablePixelRetryStudy.accepts(pr, pictures, "P3 legacy only", "block-replay", 1));
    }

    @Test
    void theDeviatingSideIsTheOneTheRetriesDidNotReproduce() {
        List<TablePixelRetryStudy.Pair> retries = List.of(pair("case-only", 1, 2, a, a), pair("block-replay", 1, 3, a, a));
        TablePixelRetryStudy.Procedure pr = new TablePixelRetryStudy.Procedure("p", pair("primary", 0, 1, a, b), retries);
        assertEquals("the primary migrated picture was not seen again in any retry", TablePixelRetryStudy.deviatingSide(pr));
        TablePixelRetryStudy.Procedure other = new TablePixelRetryStudy.Procedure("q", pair("primary", 0, 1, b, a), retries);
        assertEquals("the primary legacy picture was not seen again in any retry", TablePixelRetryStudy.deviatingSide(other));
    }

    // ---------------------------------------------------------------- the reports

    @Test
    void thePrimaryReportCountsAttemptsAndPairsSeparatelyAndRecordsEveryRetry() {
        List<TablePixelRetryStudy.Pair> primary = new ArrayList<>(List.of(pair("primary", 0, 1, a, a), pair("primary", 0, 1, a, b), pair("primary", 0, 8, a, a)));
        String report = TablePixelRetryStudy.primaryReport(primary, List.of(procedure()), pictures, 0);
        assertTrue(report.contains("2 attempts, 3 judged pairs (clustered 12 per attempt): 2 identical, 0 tolerated, 1 rejected"), report);
        assertTrue(report.contains("attempts with at least one rejected pair: 1 of 2 (95% interval"), report);
        assertTrue(report.contains("SP3 (within tolerance): 0 tolerated pairs; no retry follows a tolerated pair"), report);
        assertEquals(6, report.lines().filter(l -> l.trim().startsWith("retry ")).count(), "every retry is listed");
        assertTrue(report.contains(String.format("  %-17s %-12s | n=1: %s | n=2: %s", "P1 both sides", "case-only", TablePixelRetryStudy.rate(0, 1), TablePixelRetryStudy.rate(1, 1))),
                report);
        assertTrue(report.contains("case-only    " + TablePixelRetryStudy.rate(1, 3)), "one of the three case-only retry pairs was itself rejected: " + report);
    }

    private static TablePixelRetryStudy.ControlRun run(TablePixelRetryStudy.Control c, int execution, TablePixelRetryStudy.Pair primary, List<TablePixelRetryStudy.Pair> retries) {
        return new TablePixelRetryStudy.ControlRun(c, "light/x", execution, "css", new TablePixelRetryStudy.Procedure("u", primary, retries), 0, 0);
    }

    @Test
    void aMustRejectControlThatEveryProcedureStillRejectsIsReportedAsSuch() {
        TablePixelRetryStudy.Control c = new TablePixelRetryStudy.Control("MR9", "must-reject", "plain", "a defect");
        List<TablePixelRetryStudy.Pair> rejecting = List.of(pair("case-only", 1, 2, a, b), pair("block-replay", 1, 3, a, b));
        String report = TablePixelRetryStudy.controlReport(List.of(run(c, 1, pair("primary", 0, 1, a, b), rejecting), run(c, 2, pair("primary", 0, 4, a, b), rejecting)), pictures);
        assertTrue(report.contains("MR9 a defect on plain: must-reject: the current policy rejected every primary pair"), report);
        assertTrue(report.contains("defect present on 6 of 6 migrated captures"), report);
        assertFalse(report.contains("UNACCEPTABLE"), report);
        assertTrue(report.contains("light/x execution 1 (primary process 1): primary REJECTED; case-only: r1 p2 REJECTED; block-replay: r1 p3 REJECTED"), "every retry is listed: " + report);
        assertTrue(report.contains("light/x execution 2 (primary process 4): primary REJECTED; case-only: r1 p2 REJECTED; block-replay: r1 p3 REJECTED"), report);
    }

    @Test
    void aProcedureThatAcceptsAMustRejectControlIsUnacceptable() {
        TablePixelRetryStudy.Control c = new TablePixelRetryStudy.Control("MR9", "must-reject", "plain", "a defect");
        List<TablePixelRetryStudy.Pair> masking = List.of(pair("case-only", 1, 2, a, a), pair("block-replay", 1, 3, a, b));
        String report = TablePixelRetryStudy.controlReport(List.of(run(c, 1, pair("primary", 0, 1, a, b), masking)), pictures);
        assertTrue(report.contains("UNACCEPTABLE: accepts a deterministic regression the current policy rejects"), report);
        assertTrue(report.lines().filter(l -> l.contains("UNACCEPTABLE")).allMatch(l -> l.contains("case-only")), "only the case-only mode accepted it: " + report);
    }

    @Test
    void aMustRejectControlTheCurrentPolicyAcceptsIsReclassified() {
        TablePixelRetryStudy.Control c = new TablePixelRetryStudy.Control("MR9", "must-reject", "plain", "a defect");
        String report = TablePixelRetryStudy.controlReport(List.of(run(c, 1, pair("primary", 0, 1, a, b), List.of()), run(c, 2, pair("primary", 0, 2, a, a), List.of())), pictures);
        assertTrue(report.contains("RECLASSIFY as a sensitivity probe: the current policy accepted 1 of 2 primary pairs"), report);
    }

    // ---------------------------------------------------------------- the planned counts

    private static int index(String caseKey) {
        for (int i = 0; i < TableBrowser.CASES.size(); i++) {
            if (TableBrowser.CASES.get(i).key().equals(caseKey)) {
                return i;
            }
        }
        throw new IllegalArgumentException(caseKey);
    }

    @Test
    void thePlannedCountsFollowFromTheImplementedMatrices() {
        // primary: 100 attempts of a full block (12 cases, legacy and migrated)
        int attempts = TablePixelRetryStudyTest.MATRIX.stream().mapToInt(TablePixelRetryStudyTest.Cell::attempts).sum();
        assertEquals(100, attempts);
        assertEquals(44, TablePixelRetryStudyTest.MATRIX.stream().filter(c -> c.block() == 0).mapToInt(TablePixelRetryStudyTest.Cell::attempts).sum());
        assertEquals(56, TablePixelRetryStudyTest.MATRIX.stream().filter(c -> c.block() == 1).mapToInt(TablePixelRetryStudyTest.Cell::attempts).sum());
        assertEquals(2_400, attempts * 2 * TablePixelRetryStudyTest.BLOCK);
        // the affected cases are where the design says they are
        assertEquals(5, index("sort button hover") % 12);
        assertEquals(0, index("sort button hover") / 12);
        assertEquals(6, index("clear selection focus-visible") % 12);
        assertEquals(7, index("clear selection hover") % 12);
        assertEquals(1, index("clear selection hover") / 12);
        // controls: each execution is a full block replay plus 3 case-only and 3 block-replay retries up to the case
        int executions = 0;
        int processes = 0;
        int navigations = 0;
        for (TablePixelRetryControlStudyTest.Spec s : TablePixelRetryControlStudyTest.CONTROLS) {
            int i = index(s.control().caseKey()) % 12;
            int n = s.configs().size() * TablePixelRetryControlStudyTest.EXECUTIONS;
            executions += n;
            processes += n * (1 + 2 * TablePixelRetryStudy.MAX_RETRIES);
            navigations += n * (2 * 12 + TablePixelRetryStudy.MAX_RETRIES * (2 + 2 * (i + 1)));
        }
        assertEquals(32, executions);
        assertEquals(224, processes);
        assertEquals(2_328, navigations);
        // setup: one process (one navigation) per case and configuration that a measured control needs: tb20 and tb6 in two configurations each
        long setups = TablePixelRetryControlStudyTest.CONTROLS.stream().filter(s -> List.of("MR4", "SP1", "MR5", "SP2").contains(s.control().id()))
                .flatMap(s -> s.configs().stream().map(c -> s.control().caseKey() + "|" + c)).distinct().count();
        assertEquals(4, setups);
    }
}
