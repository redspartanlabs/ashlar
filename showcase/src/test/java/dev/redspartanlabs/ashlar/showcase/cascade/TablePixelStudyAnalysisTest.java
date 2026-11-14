package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TEMPORARY LOCAL DIAGNOSTIC, not for merge: the study's analysis on synthetic pictures whose differences are known. No browser. */
class TablePixelStudyAnalysisTest {

    private static String png(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private static BufferedImage picture(int type, int x, int y, int color) {
        BufferedImage image = new BufferedImage(20, 10, type);
        for (int j = 0; j < 10; j++) {
            for (int i = 0; i < 20; i++) {
                image.setRGB(i, j, 0xFFEEEEEE);
            }
        }
        if (x >= 0) {
            image.setRGB(x, y, color);
        }
        return image;
    }

    private final TablePixelStudy.Pictures pictures = new TablePixelStudy.Pictures();
    private final String a;
    private final String b;
    private final String sameAsAButEncodedDifferently;

    TablePixelStudyAnalysisTest() throws IOException {
        a = pictures.add(png(picture(BufferedImage.TYPE_INT_RGB, -1, 0, 0)));
        b = pictures.add(png(picture(BufferedImage.TYPE_INT_RGB, 3, 4, 0xFFEEEEF1)));
        sameAsAButEncodedDifferently = pictures.add(png(picture(BufferedImage.TYPE_INT_ARGB, -1, 0, 0)));
    }

    private static TablePixelStudy.Capture c(String caseKey, boolean forced, String variant, int process, int round, int slot, String first, String second, String third) {
        return new TablePixelStudy.Capture(caseKey, "tb1", forced, "light/x", variant, process, round, slot, first, second, third);
    }

    @Test
    void picturesAreKeptOnceByTheirHash() throws IOException {
        int before = pictures.size();
        assertEquals(a, pictures.add(png(picture(BufferedImage.TYPE_INT_RGB, -1, 0, 0))));
        assertEquals(before, pictures.size());
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", TablePixelStudy.sha256("abc".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    void theStabilityOfOneInputIsCountedPerCellWithItsProcesses() {
        List<TablePixelStudy.Capture> captures = new ArrayList<>();
        // process 1: A, A ; process 2: A, B (so one process saw two pictures); one 2nd shot differs; one re-forced shot differs
        captures.add(c("hover", true, "L", 1, 0, 1, a, a, a));
        captures.add(c("hover", true, "L", 1, 1, 2, a, a, b));
        captures.add(c("hover", true, "L", 2, 0, 1, a, b, a));
        captures.add(c("hover", true, "L", 2, 1, 2, b, b, b));
        String report = TablePixelStudy.report(captures, pictures, new TablePixelStudy.Measurements(pictures));
        assertTrue(report.contains(String.format("%-30s %-15s %-3s %3d (%d) | %d [%s] | %d | %d | %d%n", "hover", "light/x", "L", 4, 2, 2, "3,1", 1, 1, 1)), report);
        assertTrue(report.contains("first shot " + TablePixelStudy.id(a) + " vs " + TablePixelStudy.id(b) + ": differing=1 maxChannel=3 bounds=x3..3,y4..4"), report);
        assertTrue(report.contains("seen 1x in process(es) 2, slots 2"), report);
        assertTrue(report.contains("2nd shot, process 2 slot 1: "), report);
        assertTrue(report.contains("3rd shot, process 1 slot 2: "), report);
        assertTrue(report.contains("Forced-state cells: 4 navigations; 1 cells with more than one first shot; 1 processes whose own first shots differ; 1 2nd-shot and 1 re-forced differences"), report);
    }

    @Test
    void variantsAreComparedOnlyWithinTheSameProcessAndRound() {
        List<TablePixelStudy.Capture> captures = new ArrayList<>();
        captures.add(c("hover", true, "L", 1, 0, 1, a, a, a));
        captures.add(c("hover", true, "Mm", 1, 0, 2, a, a, a));
        captures.add(c("hover", true, "Mp", 1, 0, 3, b, b, b));
        captures.add(c("hover", true, "L", 1, 1, 4, a, a, a));
        captures.add(c("hover", true, "Mm", 1, 1, 5, a, a, a));
        captures.add(c("hover", true, "Mp", 1, 1, 6, a, a, a));
        // another process, round 0, with no Mp: its L and Mm still pair, nothing pairs with Mp
        captures.add(c("hover", true, "L", 2, 0, 1, a, a, a));
        captures.add(c("hover", true, "Mm", 2, 0, 2, b, b, b));
        String report = TablePixelStudy.report(captures, pictures, new TablePixelStudy.Measurements(pictures));
        String lm = report.substring(report.indexOf("-- L vs Mm"), report.indexOf("-- L vs Mp"));
        String lp = report.substring(report.indexOf("-- L vs Mp"), report.indexOf("-- Mm vs Mp"));
        String mp = report.substring(report.indexOf("-- Mm vs Mp"), report.indexOf("== 3."));
        assertTrue(lm.contains(String.format("%-30s %-15s %d/%d | %d/%d%n", "hover", "light/x", 2, 3, 1, 2)), lm);
        assertTrue(lp.contains(String.format("%-30s %-15s %d/%d | %d/%d%n", "hover", "light/x", 1, 2, 1, 1)), lp);
        assertTrue(mp.contains(String.format("%-30s %-15s %d/%d | %d/%d%n", "hover", "light/x", 1, 2, 1, 1)), mp);
        assertTrue(lp.contains("1x " + TablePixelStudy.id(a) + " vs " + TablePixelStudy.id(b) + ": differing=1"), lp);
        assertTrue(lm.contains("forced-state cells: 1 of 3 pairs differ, in 1 of 2 process-cells; controls: 0 of 0 pairs differ"), lm);
    }

    @Test
    void picturesWhoseBytesDifferButWhosePixelsDoNotAreSaidToBeSo() {
        assertTrue(!a.equals(sameAsAButEncodedDifferently), "precondition: two encodings");
        List<TablePixelStudy.Capture> captures = List.of(c("plain", false, "L", 1, 0, 1, a, a, a), c("plain", false, "L", 2, 0, 1, sameAsAButEncodedDifferently, sameAsAButEncodedDifferently,
                sameAsAButEncodedDifferently));
        String report = TablePixelStudy.report(captures, pictures, new TablePixelStudy.Measurements(pictures));
        assertTrue(report.contains(": bytes differ, PIXELS IDENTICAL"), report);
        assertTrue(report.contains("(no state forced)"), report);
        assertTrue(report.contains("Control cells (no state forced): 2 navigations; 1 cells with more than one first shot; 0 processes whose own first shots differ"), report);
    }

    @Test
    void theSetsOfPicturesPerVariantAreListed() {
        List<TablePixelStudy.Capture> captures = List.of(c("hover", true, "L", 1, 0, 1, a, a, a), c("hover", true, "Mm", 1, 0, 2, a, a, a), c("hover", true, "Mp", 1, 0, 3, b, b, b));
        String report = TablePixelStudy.report(captures, pictures, new TablePixelStudy.Measurements(pictures));
        String id = TablePixelStudy.id(a);
        assertTrue(report.contains("L={" + id + "}  Mm={" + id + "}  Mp={" + TablePixelStudy.id(b) + "}"), report);
    }

    @Test
    void theCsvHasOneLinePerNavigation() {
        List<TablePixelStudy.Capture> captures = List.of(c("hover", true, "L", 1, 0, 1, a, a, b), c("plain", false, "Mm", 1, 0, 2, a, a, a));
        String csv = TablePixelStudy.csv(captures);
        assertEquals(3, csv.lines().count());
        assertTrue(csv.contains("\"hover\",tb1,true,light/x,L,1,0,1," + TablePixelStudy.id(a) + "," + TablePixelStudy.id(a) + "," + TablePixelStudy.id(b)), csv);
    }
}
