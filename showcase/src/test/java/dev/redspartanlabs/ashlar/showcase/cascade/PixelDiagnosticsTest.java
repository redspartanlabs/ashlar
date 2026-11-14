package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The diagnostics on synthetic pictures whose differences are known exactly: what they count, where they say it is, that they agree with the production comparison wherever that one
 * produces a number, and that producing them leaves the production comparison's answers as they were. No browser.
 *
 * <p>The picture is 60 x 40, light grey, with a dark 20 x 12 "element" at (20, 14).
 */
class PixelDiagnosticsTest {

    private static final PixelPolicy.Geometry RING = new PixelPolicy.Geometry(new PixelPolicy.Box(20, 14, 20, 12), 2);

    private static BufferedImage picture() {
        BufferedImage image = new BufferedImage(60, 40, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 40; y++) {
            for (int x = 0; x < 60; x++) {
                image.setRGB(x, y, x >= 20 && x < 40 && y >= 14 && y < 26 ? 0x404040 : 0xEEEEEE);
            }
        }
        return image;
    }

    private static BufferedImage copy(BufferedImage image) {
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                copy.setRGB(x, y, image.getRGB(x, y));
            }
        }
        return copy;
    }

    /** A copy of the picture with the given pixels set to the given colors. */
    private static BufferedImage with(int... xyColor) {
        BufferedImage copy = copy(picture());
        for (int i = 0; i < xyColor.length; i += 3) {
            copy.setRGB(xyColor[i], xyColor[i + 1], xyColor[i + 2]);
        }
        return copy;
    }

    private static String png(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    // ---------------------------------------------------------------- measuring

    @Test
    void identicalPicturesDifferInNothing() {
        PixelDiagnostics.Difference d = PixelDiagnostics.measure(picture(), picture());
        assertFalse(d.any());
        assertEquals(0, d.differing());
        assertEquals(-1, d.minX());
        assertEquals(-1, d.maxY());
        assertEquals(0, d.width());
        assertEquals(List.of(), d.samples());
        assertNull(PixelDiagnostics.overlay(picture(), picture(), d));
        assertNull(PixelDiagnostics.zoom(picture(), picture(), d));
    }

    @Test
    void oneChangedPixelIsCountedLocatedAndMeasuredExactly() {
        PixelDiagnostics.Difference d = PixelDiagnostics.measure(picture(), with(33, 7, 0xEEEEEE + 0x000300));
        assertEquals(1, d.differing());
        assertEquals(3, d.maxChannel());
        assertEquals(0, d.maxAlpha());
        assertEquals(33, d.minX());
        assertEquals(33, d.maxX());
        assertEquals(7, d.minY());
        assertEquals(7, d.maxY());
        assertEquals(1, d.width());
        assertEquals(1, d.height());
        assertEquals(List.of(new PixelDiagnostics.Sample(33, 7, 0xFFEEEEEE, 0xFFEEF1EE)), d.samples());
    }

    @Test
    void severalPixelsGiveTheirBoundingRectangleAndTheLargestChannelDifference() {
        // (5,30): red +16; (50,2): blue -128; (21,15): green +1. Bounds: x 5..50, y 2..30.
        PixelDiagnostics.Difference d = PixelDiagnostics.measure(picture(), with(5, 30, 0xFEEEEE, 50, 2, 0xEEEE6E, 21, 15, 0x404140));
        assertEquals(3, d.differing());
        assertEquals(128, d.maxChannel());
        assertEquals(5, d.minX());
        assertEquals(50, d.maxX());
        assertEquals(2, d.minY());
        assertEquals(30, d.maxY());
        assertEquals(46, d.width());
        assertEquals(29, d.height());
        assertEquals(3, d.samples().size());
        assertEquals(50, d.samples().get(0).x(), "samples are in raster order: the one at y=2 comes first");
    }

    @Test
    void anAlphaDifferenceIsReportedSeparatelyFromTheColorChannels() {
        BufferedImage a = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        BufferedImage b = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                a.setRGB(x, y, 0xFF102030);
                b.setRGB(x, y, 0xFF102030);
            }
        }
        b.setRGB(2, 1, 0x80102030);
        PixelDiagnostics.Difference d = PixelDiagnostics.measure(a, b);
        assertEquals(1, d.differing());
        assertEquals(0, d.maxChannel());
        assertEquals(127, d.maxAlpha());
    }

    @Test
    void theSamplesAreCappedButTheCountIsNot() {
        BufferedImage other = copy(picture());
        for (int x = 0; x < 20; x++) {
            other.setRGB(x, 0, 0x000000);
        }
        PixelDiagnostics.Difference d = PixelDiagnostics.measure(picture(), other);
        assertEquals(20, d.differing());
        assertEquals(PixelDiagnostics.MAX_SAMPLES, d.samples().size());
    }

    @Test
    void picturesOfDifferentSizesAreMeasuredOverTheRegionTheyShareAndSaySo() {
        BufferedImage wider = new BufferedImage(70, 40, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 40; y++) {
            for (int x = 0; x < 70; x++) {
                wider.setRGB(x, y, x < 60 ? picture().getRGB(x, y) : 0x000000);
            }
        }
        PixelDiagnostics.Difference same = PixelDiagnostics.measure(picture(), wider);
        assertFalse(same.sameSize());
        assertEquals(60, same.referenceWidth());
        assertEquals(70, same.candidateWidth());
        assertEquals(0, same.differing(), "the shared region is identical");
        assertTrue(same.describe().contains("sizes differ"), same.describe());

        wider.setRGB(10, 10, 0x123456);
        PixelDiagnostics.Difference d = PixelDiagnostics.measure(picture(), wider);
        assertFalse(d.sameSize());
        assertEquals(1, d.differing());
        assertEquals(10, d.minX());
        assertTrue(d.describe().contains("candidate 70x40"), d.describe());
        assertNotNull(PixelDiagnostics.overlay(picture(), wider, d));
        assertNotNull(PixelDiagnostics.zoom(picture(), wider, d));
    }

    // ---------------------------------------------------------------- agreement with the production comparison

    @Test
    void wherePixelPolicyProducesNumbersTheDiagnosticsProduceTheSameOnes() throws IOException {
        List<BufferedImage> candidates = List.of(with(19, 20, 0xEEEEEE + 5), with(20, 14, 0x404040 + 0x07), with(19, 20, 0xEEEEEE + 0x050505, 41, 27, 0xEEEEEE - 0x0A0000),
                with(0, 0, 0x000000, 59, 39, 0x000000, 30, 20, 0x404041));
        for (PixelPolicy.Policy policy : List.of(PixelPolicy.FOCUS_RING, PixelPolicy.HOVER_TEXT_EDGE)) {
            for (BufferedImage candidate : candidates) {
                PixelPolicy.Verdict verdict = PixelPolicy.compare(png(picture()), png(candidate), policy, RING);
                PixelDiagnostics.Difference d = PixelDiagnostics.measure(png(picture()), png(candidate));
                assertEquals(verdict.differing(), d.differing(), policy.name());
                assertEquals(verdict.maxChannel(), d.maxChannel(), policy.name());
                assertEquals(verdict.sameSize(), d.sameSize(), policy.name());
            }
        }
    }

    @Test
    void measuringChangesNoVerdictAndTheExactPolicyStillRejectsWhatItRejectedBefore() throws IOException {
        String base = png(picture());
        String changed = png(with(30, 20, 0x404041));
        PixelPolicy.Verdict before = PixelPolicy.compare(base, changed, PixelPolicy.EXACT, RING);
        PixelDiagnostics.Difference d = PixelDiagnostics.measure(base, changed);
        PixelDiagnostics.report("T", List.of(new PixelDiagnostics.Capture("x", Map.of(), base, changed, before.accepted(), before.describe(), RING)), null, new PrintStream(new ByteArrayOutputStream()), "env");
        PixelPolicy.Verdict after = PixelPolicy.compare(base, changed, PixelPolicy.EXACT, RING);
        assertEquals(before, after, "a record is compared by its components: nothing about the verdict moved");
        assertFalse(after.accepted());
        assertEquals(-1, after.differing(), "the exact policy still decides on the encoded text and does not decode");
        assertEquals(1, d.differing(), "while the diagnostics see the one pixel the verdict never counted");
        // a tolerance still accepts exactly what it accepted: one edge pixel in the band, a few levels off
        assertTrue(PixelPolicy.compare(base, png(with(19, 20, 0xEEEEEE + 5)), PixelPolicy.FOCUS_RING, RING).accepted());
        assertFalse(PixelPolicy.compare(base, png(with(19, 20, 0xEEEEEE + 40)), PixelPolicy.FOCUS_RING, RING).accepted());
    }

    @Test
    void picturesThatAreEncodedDifferentlyButHaveTheSamePixelsAreRejectedByTheExactPolicyAndShownAsPixelIdentical() throws IOException {
        BufferedImage opaqueRgb = picture();
        BufferedImage argb = new BufferedImage(60, 40, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 40; y++) {
            for (int x = 0; x < 60; x++) {
                argb.setRGB(x, y, 0xFF000000 | opaqueRgb.getRGB(x, y));
            }
        }
        String a = png(opaqueRgb);
        String b = png(argb);
        assertFalse(a.equals(b), "the two encodings differ");
        PixelPolicy.Verdict verdict = PixelPolicy.compare(a, b, PixelPolicy.EXACT, null);
        assertFalse(verdict.accepted(), "so the exact policy, which compares the encoded text, rejects them, as it always did");
        PixelDiagnostics.Difference d = PixelDiagnostics.measure(a, b);
        assertEquals(0, d.differing());
        assertTrue(d.describe().contains("pixel-identical"), d.describe());
    }

    // ---------------------------------------------------------------- drawing

    @Test
    void theOverlayMarksEveryDifferingPixelInMagentaAndFadesTheRest() {
        BufferedImage candidate = with(30, 20, 0x404041, 31, 20, 0x404042);
        PixelDiagnostics.Difference d = PixelDiagnostics.measure(picture(), candidate);
        BufferedImage overlay = PixelDiagnostics.overlay(picture(), candidate, d);
        assertEquals(60, overlay.getWidth());
        assertEquals(40, overlay.getHeight());
        assertEquals(0xFFFF00FF, overlay.getRGB(30, 20));
        assertEquals(0xFFFF00FF, overlay.getRGB(31, 20));
        // an untouched dark pixel is faded toward white: 0x40 -> 255 - (255 - 64) / 4 = 208
        assertEquals(0xFFD0D0D0, overlay.getRGB(25, 18));
        // the outline is two pixels outside the bounding rectangle x 30..31, y 20..20
        assertEquals(0xFFFF8000, overlay.getRGB(28, 22));
        assertEquals(0xFFFF8000, overlay.getRGB(33, 18));
        assertEquals(0xFFFF8000, overlay.getRGB(30, 18));
        assertEquals(0xFFFF8000, overlay.getRGB(33, 20));
    }

    @Test
    void theZoomShowsReferenceCandidateAndDifferenceEnlargedAroundTheDifference() {
        BufferedImage candidate = with(30, 20, 0x404041);
        PixelDiagnostics.Difference d = PixelDiagnostics.measure(picture(), candidate);
        BufferedImage zoom = PixelDiagnostics.zoom(picture(), candidate, d);
        // crop = (30,20) +- 6 = x 24..36, y 14..26: 13 x 13, scale min(8, 600 / 13) = 8, three panels of 104 and two gutters of 8
        assertEquals(3 * 104 + 2 * PixelDiagnostics.GUTTER, zoom.getWidth());
        assertEquals(104, zoom.getHeight());
        int centerX = (30 - 24) * 8 + 4;
        int centerY = (20 - 14) * 8 + 4;
        assertEquals(0xFF404040, zoom.getRGB(centerX, centerY), "panel 1: the reference pixel");
        assertEquals(0xFF404041, zoom.getRGB(104 + PixelDiagnostics.GUTTER + centerX, centerY), "panel 2: the candidate pixel");
        assertEquals(0xFFFF00FF, zoom.getRGB(2 * (104 + PixelDiagnostics.GUTTER) + centerX, centerY), "panel 3: marked as different");
        assertEquals(0xFFFF00FF, zoom.getRGB(2 * (104 + PixelDiagnostics.GUTTER) + centerX + 3, centerY - 3), "the mark fills the whole enlarged square");
        assertEquals(0xFFFFFFFF, zoom.getRGB(104 + 3, 3), "the gutter is white");
    }

    @Test
    void theZoomStaysInsideThePictureWhenTheDifferenceIsAtACorner() {
        BufferedImage candidate = with(0, 0, 0x000000, 59, 39, 0x000000);
        PixelDiagnostics.Difference d = PixelDiagnostics.measure(picture(), candidate);
        BufferedImage zoom = PixelDiagnostics.zoom(picture(), candidate, d);
        // the margin would reach past the picture, so the crop is the whole 60 x 40 picture; scale min(8, 600 / 60) = 8; panels of 480
        assertEquals(3 * 480 + 2 * PixelDiagnostics.GUTTER, zoom.getWidth());
        assertEquals(320, zoom.getHeight());
        assertEquals(0xFFFF00FF, zoom.getRGB(2 * (480 + PixelDiagnostics.GUTTER) + 59 * 8 + 4, 39 * 8 + 4), "the far corner is inside the last panel");
    }

    // ---------------------------------------------------------------- the report

    private static PixelDiagnostics.Capture capture(String label, BufferedImage candidate, boolean accepted) throws IOException {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("configuration", "light/sterling");
        context.put("case", label);
        return new PixelDiagnostics.Capture(label, context, png(picture()), png(candidate), accepted, accepted ? "within the policy" : "the pictures differ and this case is compared exactly", RING);
    }

    private static String run(List<PixelDiagnostics.Capture> captures, Path directory) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PixelDiagnostics.report("T", captures, directory, new PrintStream(bytes, true, StandardCharsets.UTF_8), "env-line");
        return bytes.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    @Test
    void theReportNamesEveryPairWithItsNumbersContextAndTheVerdictItWasGiven(@TempDir Path directory) throws IOException {
        String text = run(List.of(capture("clear selection hover", with(30, 20, 0x404041), false), capture("sort button focus-visible", with(19, 20, 0xEEEEEE + 5), true)), directory);
        assertTrue(text.contains("T diagnostics: 2 pair(s) not byte-identical, 1 tolerated, 1 rejected\n"), text);
        assertTrue(text.contains("T environment: env-line\n"), text);
        assertTrue(text.contains("T REJECTED [0] clear selection hover: differing=1 maxChannel=1 maxAlpha=0 bounds=x30..30,y20..20 (1x1)"), text);
        assertTrue(text.contains("configuration=light/sterling case=clear selection hover"), text);
        assertTrue(text.contains("policy said: the pictures differ and this case is compared exactly"), text);
        assertTrue(text.contains("geometry=Geometry["), text);
        assertTrue(text.contains("T tolerated [1] sort button focus-visible: differing=1 maxChannel=5"), text);
    }

    @Test
    void thePicturesOfARejectedPairAreInTheOutputAsTakenAndInFilesAndATolerancedPairHasNone(@TempDir Path directory) throws IOException {
        PixelDiagnostics.Capture rejected = capture("clear selection hover", with(30, 20, 0x404041), false);
        String text = run(List.of(rejected, capture("sort button focus-visible", with(19, 20, 0xEEEEEE + 5), true)), directory);
        assertTrue(text.contains("T image [0] reference " + rejected.reference() + "\n"), "the reference as captured");
        assertTrue(text.contains("T image [0] candidate " + rejected.candidate() + "\n"), "the candidate as captured");
        String zoomLine = text.lines().filter(l -> l.startsWith("T image [0] zoom ")).findFirst().orElseThrow();
        BufferedImage zoom = ImageIO.read(new java.io.ByteArrayInputStream(Base64.getDecoder().decode(zoomLine.substring("T image [0] zoom ".length()))));
        assertNotNull(zoom, "the printed zoom is a picture");
        assertFalse(text.contains("T image [1]"), "a tolerated pair prints no pictures");

        Path folder = directory.resolve("0-clear-selection-hover");
        assertTrue(Files.isDirectory(folder), String.valueOf(directory));
        assertEquals(Base64.getDecoder().decode(rejected.reference()).length, Files.size(folder.resolve("reference.png")));
        assertTrue(Files.exists(folder.resolve("candidate.png")));
        assertTrue(Files.exists(folder.resolve("diff.png")));
        assertTrue(Files.exists(folder.resolve("zoom.png")));
        assertTrue(Files.readString(folder.resolve("report.txt")).contains("differing=1"));
        assertFalse(Files.exists(directory.resolve("1-sort-button-focus-visible")), "nothing is written for a tolerated pair");
    }

    @Test
    void picturesAreShownForTheFirstFewRejectedPairsOnlyButEveryPairKeepsItsNumbers() throws IOException {
        List<PixelDiagnostics.Capture> captures = new ArrayList<>();
        for (int i = 0; i < PixelDiagnostics.MAX_IMAGES + 3; i++) {
            captures.add(capture("case " + i, with(30 + i, 20, 0x404041), false));
        }
        String text = run(captures, null);
        assertEquals(PixelDiagnostics.MAX_IMAGES, text.lines().filter(l -> l.startsWith("T image ") && l.contains(" zoom ")).count());
        assertEquals(captures.size(), text.lines().filter(l -> l.startsWith("T REJECTED [")).count());
        assertTrue(text.contains("shown for the first " + PixelDiagnostics.MAX_IMAGES + " of " + captures.size() + " rejected pairs"), text);
    }

    @Test
    void aPairThatCannotBeMeasuredIsSaidSoAndTheOthersAreStillReported(@TempDir Path directory) throws IOException {
        PixelDiagnostics.Capture broken = new PixelDiagnostics.Capture("broken", Map.of("case", "broken"), "not a picture", "also not", false, "the pictures differ", null);
        String text = run(List.of(broken, capture("fine", with(30, 20, 0x404041), false)), directory);
        assertTrue(text.contains("T REJECTED [0] broken: the pair could not be measured"), text);
        assertTrue(text.contains("T REJECTED [1] fine: differing=1"), text);
    }

    @Test
    void anUnwritableDirectoryDoesNotStopTheReport(@TempDir Path directory) throws IOException {
        Path blocker = directory.resolve("blocker");
        Files.writeString(blocker, "a file where a directory is needed");
        String text = run(List.of(capture("one", with(30, 20, 0x404041), false), capture("two", with(31, 20, 0x404041), false)), blocker);
        assertTrue(text.contains("T REJECTED [0] one: differing=1"), text);
        assertTrue(text.contains("T REJECTED [1] two: differing=1"), text);
        assertTrue(text.contains("T files [0]: could not be written"), text);
        assertFalse(text.contains("could not be measured"), "the pairs were measured; only the files failed");
    }

    @Test
    void labelsBecomeSafeFileNames() {
        assertEquals("light-sterling-tb20-clear-selection-hover", PixelDiagnostics.safe("light/sterling tb20 (clear selection hover)"));
        assertEquals("a", PixelDiagnostics.safe("//a//"));
        assertTrue(PixelDiagnostics.safe("x".repeat(200)).length() <= 80);
    }

    // ---------------------------------------------------------------- the environment

    @Test
    void theEnvironmentNamesTheMachineAndSaysUnknownForABrowserItCannotRun(@TempDir Path directory) {
        String environment = PixelDiagnostics.environment(directory.resolve("no-such-browser"));
        assertTrue(environment.contains("os=") && environment.contains("java=") && environment.contains("cpus=") && environment.contains("ci="), environment);
        assertTrue(environment.endsWith("chrome=unknown"), environment);
        assertEquals("unknown", PixelDiagnostics.chromeVersion(directory.resolve("also-missing")));
    }
}
