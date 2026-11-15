package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PixelDiagnostics describes differences exactly and decides nothing; its picture budget bounds what a report prints. */
class PixelDiagnosticsTest {

    private static BufferedImage image(int width, int height, int argb) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, argb);
            }
        }
        return image;
    }

    private static String png(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    @Test
    void measuresCountChannelAlphaBoundsAndEdgePixels() throws IOException {
        BufferedImage reference = image(10, 8, 0xFF102030);
        BufferedImage candidate = image(10, 8, 0xFF102030);
        candidate.setRGB(2, 3, 0xFF10203A);
        candidate.setRGB(7, 5, 0x80102030);
        PixelDiagnostics.Difference d = PixelDiagnostics.measure(png(reference), png(candidate));
        assertTrue(d.sameSize() && d.any());
        assertEquals(2, d.differing());
        assertEquals(10, d.maxChannel(), "alpha does not count as a channel");
        assertEquals(0x7F, d.maxAlpha());
        assertEquals(2, d.minX());
        assertEquals(3, d.minY());
        assertEquals(7, d.maxX());
        assertEquals(5, d.maxY());
        assertEquals(0, d.edgePixels(), "the reference is uniform, so no differing pixel is an edge pixel");
        assertEquals("(2,3) #FF102030 -> #FF10203A", d.samples().get(0).toString());
        assertTrue(d.describe().startsWith("differing=2 maxChannel=10 maxAlpha=127 bounds=x2..7,y3..5 edge pixels=0/2 size=10x8"), d.describe());

        reference.setRGB(3, 3, 0xFF000000);
        candidate.setRGB(3, 3, 0xFF000000);
        assertEquals(1,PixelDiagnostics.measure(reference, candidate).edgePixels(), "(2,3) now neighbors another color in the reference");
    }

    @Test
    void identicalPixelsAndDifferentSizesAreDescribedAsSuch() {
        PixelDiagnostics.Difference same = PixelDiagnostics.measure(image(4, 4, 0xFF000000), image(4, 4, 0xFF000000));
        assertFalse(same.any());
        assertEquals(-1, same.minX());
        assertEquals(-1, same.maxY());
        assertTrue(same.describe().startsWith("pixel-identical in 4x4"), same.describe());
        PixelDiagnostics.Difference sizes = PixelDiagnostics.measure(image(4, 4, 0xFF000000), image(5, 4, 0xFF000000));
        assertFalse(sizes.sameSize());
        assertTrue(sizes.describe().contains("reference 4x4, candidate 5x4"), sizes.describe());
    }

    @Test
    void listsAtMostTheSampleLimit() {
        BufferedImage candidate = image(40, 2, 0xFFFFFFFF);
        PixelDiagnostics.Difference d = PixelDiagnostics.measure(image(40, 2, 0xFF000000), candidate);
        assertEquals(80, d.differing());
        assertEquals(PixelDiagnostics.MAX_SAMPLES, d.samples().size());
        assertTrue(d.describe().endsWith("(first " + PixelDiagnostics.MAX_SAMPLES + ")"), d.describe());
    }

    @Test
    void decodeRefusesWhatIsNotAPicture() {
        assertThrows(IOException.class, () -> PixelDiagnostics.decode(Base64.getEncoder().encodeToString("not a picture".getBytes())));
    }

    @Test
    void thePictureBudgetPrintsWithinBothLimits() {
        PixelDiagnostics.PictureBudget budget = new PixelDiagnostics.PictureBudget();
        String big = "A".repeat(PixelDiagnostics.MAX_PICTURE_CHARS);
        assertEquals("P picture one " + big, budget.line("P", "one", big));
        assertTrue(budget.line("P", "huge", big + "A").contains("omitted: " + (PixelDiagnostics.MAX_PICTURE_CHARS + 1) + " characters"));
        for (int i = 0; i < 3; i++) {
            assertFalse(budget.line("P", "n" + i, big).contains("omitted"));
        }
        assertEquals(PixelDiagnostics.MAX_TOTAL_PICTURE_CHARS, budget.used());
        assertTrue(budget.line("P", "last", "A").contains("would pass the total"));
        assertEquals(PixelDiagnostics.MAX_TOTAL_PICTURE_CHARS, budget.used());
    }
}
