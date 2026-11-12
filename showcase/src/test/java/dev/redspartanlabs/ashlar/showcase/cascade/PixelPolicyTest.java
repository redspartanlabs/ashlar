package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The picture comparison policy on synthetic pictures: exact equality is the default, and the two tolerances accept exactly what they were measured to need and nothing else. No browser.
 *
 * <p>The picture is 60 x 40, light grey, with a dark 20 x 12 "element" at (20, 14) (so its border box is x 20..40, y 14..26) and a vertical black stripe at x 22..23 inside it, which gives edge
 * pixels in the element's neighborhood and flat pixels everywhere else.
 */
class PixelPolicyTest {

    private static final PixelPolicy.Geometry RING = new PixelPolicy.Geometry(new PixelPolicy.Box(20, 14, 20, 12), 2);
    private static final PixelPolicy.Geometry NO_RING = new PixelPolicy.Geometry(new PixelPolicy.Box(20, 14, 20, 12), 0);

    private static BufferedImage picture() {
        BufferedImage image = new BufferedImage(60, 40, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 40; y++) {
            for (int x = 0; x < 60; x++) {
                int color = 0xEEEEEE;
                if (x >= 20 && x < 40 && y >= 14 && y < 26) {
                    color = x == 22 || x == 23 ? 0x000000 : 0x404040;
                }
                image.setRGB(x, y, color);
            }
        }
        return image;
    }

    private static int shift(int channel, int delta) {
        return channel + delta > 255 ? channel - delta : channel + delta; // always exactly delta away
    }

    private static BufferedImage with(int x, int y, int delta) {
        return with(picture(), x, y, delta);
    }

    /** A copy of the picture with one pixel exactly {@code delta} off in every channel. */
    private static BufferedImage with(BufferedImage image, int x, int y, int delta) {
        int p = image.getRGB(x, y) & 0xFFFFFF;
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int j = 0; j < image.getHeight(); j++) {
            for (int i = 0; i < image.getWidth(); i++) {
                copy.setRGB(i, j, image.getRGB(i, j));
            }
        }
        copy.setRGB(x, y, (shift((p >> 16) & 0xFF, delta) << 16) | (shift((p >> 8) & 0xFF, delta) << 8) | shift(p & 0xFF, delta));
        return copy;
    }

    private static String png(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private static PixelPolicy.Verdict ring(BufferedImage candidate) {
        return PixelPolicy.compare(picture(), candidate, PixelPolicy.FOCUS_RING, RING);
    }

    private static PixelPolicy.Verdict hover(BufferedImage candidate) {
        return PixelPolicy.compare(picture(), candidate, PixelPolicy.HOVER_TEXT_EDGE, NO_RING);
    }

    // ---------------------------------------------------------------- exact equality is the default

    @Test
    void identicalPicturesAreAcceptedAsIdenticalUnderEveryPolicy() throws IOException {
        String same = png(picture());
        for (PixelPolicy.Policy policy : List.of(PixelPolicy.EXACT, PixelPolicy.FOCUS_RING, PixelPolicy.HOVER_TEXT_EDGE)) {
            PixelPolicy.Verdict v = PixelPolicy.compare(same, png(picture()), policy, RING);
            assertTrue(v.accepted() && v.exact(), policy.name());
            assertEquals(0, v.differing());
        }
    }

    @Test
    void underTheExactPolicyOneChangedPixelIsRejectedWhereverItIs() throws IOException {
        String base = png(picture());
        for (int[] at : new int[][] {{0, 0}, {19, 20}, {20, 14}, {30, 20}, {59, 39}}) {
            PixelPolicy.Verdict v = PixelPolicy.compare(base, png(with(at[0], at[1], 1)), PixelPolicy.EXACT, RING);
            assertFalse(v.accepted(), at[0] + "," + at[1]);
        }
    }

    @Test
    void aMissingGeometryNeverWidensAPolicy() throws IOException {
        PixelPolicy.Verdict v = PixelPolicy.compare(png(picture()), png(with(19, 20, 5)), PixelPolicy.FOCUS_RING, null);
        assertFalse(v.accepted());
    }

    @Test
    void onlyTheThreeNamedCasesMayUseATolerance() {
        Map<String, String> byPolicy = TableBrowser.CASES.stream().collect(Collectors.toMap(TableBrowser.Case::key, c -> PixelPolicy.forCase(c.key()).name()));
        assertEquals(TableBrowser.CASES.size(), byPolicy.size());
        Map<String, String> tolerant = byPolicy.entrySet().stream().filter(e -> !e.getValue().equals("exact")).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        assertEquals(Map.of("sort button focus-visible", "focus-ring", "sort button hover", "hover-text-edge", "active sort button hover", "hover-text-edge"), tolerant);
        assertEquals("exact", PixelPolicy.forCase("a case that does not exist").name());
    }

    @Test
    void theLimitsAreTheMeasuredOnes() {
        assertEquals(2, PixelPolicy.FOCUS_RING.maxDifferingPixels());
        assertEquals(12, PixelPolicy.FOCUS_RING.maxChannelDifference());
        assertEquals(2, PixelPolicy.HOVER_TEXT_EDGE.maxDifferingPixels());
        assertEquals(24, PixelPolicy.HOVER_TEXT_EDGE.maxChannelDifference());
        assertEquals(0, PixelPolicy.EXACT.maxDifferingPixels());
    }

    // ---------------------------------------------------------------- the focus ring

    @Test
    void oneBoundaryPixelOfTheRingIsAccepted() {
        // The ring is the 2 px outside the box: x 18..19. One pixel there, 12 off in every channel.
        PixelPolicy.Verdict v = ring(with(19, 20, 12));
        assertTrue(v.accepted(), v.describe());
        assertFalse(v.exact());
        assertEquals(1, v.differing());
        assertEquals(12, v.maxChannel());
    }

    @Test
    void twoBoundaryPixelsOfTheRingAreAccepted() {
        PixelPolicy.Verdict v = ring(with(with(19, 20, 7), 18, 21, 3));
        assertTrue(v.accepted(), v.describe());
        assertEquals(2, v.differing());
    }

    @Test
    void threeDifferingPixelsAreRejectedEvenWhenEachIsOnTheRingAndTiny() {
        PixelPolicy.Verdict v = ring(with(with(with(19, 20, 1), 19, 21, 1), 19, 22, 1));
        assertFalse(v.accepted(), v.describe());
        assertEquals(3, v.differing());
    }

    @Test
    void aChannelOffByMoreThanTwelveIsRejected() {
        assertTrue(ring(with(19, 20, 12)).accepted());
        PixelPolicy.Verdict v = ring(with(19, 20, 13));
        assertFalse(v.accepted(), v.describe());
        assertEquals(13, v.maxChannel());
    }

    @Test
    void aPixelOutsideTheRingBandIsRejected() {
        // The band reaches from 1 px inside the box to ring extent 2 + 1.5 = 3.5 px outside: pixel centers at x = 16.5 (distance 3.5) are in, 15.5 (4.5) are out; 20.5 (inside by 0.5 from the left edge) in.
        assertTrue(ring(with(16, 20, 5)).accepted(), "3.5 px outside is the edge of the band");
        assertFalse(ring(with(15, 20, 5)).accepted(), "4.5 px outside");
        assertFalse(ring(with(30, 5, 5)).accepted(), "far above the element");
        assertFalse(ring(with(21, 20, 5)).accepted(), "1.5 px inside the left edge, beyond the 1 px the band allows (the stripe is at x 22)");
        assertTrue(ring(with(20, 20, 5)).accepted(), "0.5 px inside the left edge");
    }

    @Test
    void theBandFollowsTheBrowsersRingExtentAndNothingIsHardCoded() {
        PixelPolicy.Geometry thinner = new PixelPolicy.Geometry(new PixelPolicy.Box(20, 14, 20, 12), 1);
        // x = 16 is 3.5 px outside: in for a ring that reaches 2 px, out for one that reaches 1 px (band to 2.5).
        assertTrue(PixelPolicy.compare(picture(), with(16, 20, 5), PixelPolicy.FOCUS_RING, RING).accepted());
        assertFalse(PixelPolicy.compare(picture(), with(16, 20, 5), PixelPolicy.FOCUS_RING, thinner).accepted());
        // And the box moves the band with it.
        PixelPolicy.Geometry moved = new PixelPolicy.Geometry(new PixelPolicy.Box(30, 14, 20, 12), 2);
        assertFalse(PixelPolicy.compare(picture(), with(19, 20, 5), PixelPolicy.FOCUS_RING, moved).accepted());
    }

    @Test
    void picturesOfDifferentSizesAreRejected() throws IOException {
        BufferedImage wider = new BufferedImage(61, 40, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 40; y++) {
            for (int x = 0; x < 61; x++) {
                wider.setRGB(x, y, x < 60 ? picture().getRGB(x, y) : 0xEEEEEE);
            }
        }
        PixelPolicy.Verdict v = ring(wider);
        assertFalse(v.accepted());
        assertFalse(v.sameSize());
        assertFalse(PixelPolicy.compare(png(picture()), png(wider), PixelPolicy.HOVER_TEXT_EDGE, NO_RING).accepted());
    }

    // ---------------------------------------------------------------- hover

    // The reference picture's edge pixels near the box are x = 19 (0.5 px outside the left edge, a neighbor of the box) and x = 20 (0.5 px inside); x = 18 is flat and 1.5 px outside.

    @Test
    void oneHoverEdgePixelIsAcceptedUpToTheMeasuredNoise() {
        PixelPolicy.Verdict v = hover(with(19, 20, 18));
        assertTrue(v.accepted(), v.describe());
        assertEquals(18, v.maxChannel());
        assertTrue(hover(with(19, 20, 24)).accepted());
        assertFalse(hover(with(19, 20, 25)).accepted());
        assertTrue(hover(with(20, 20, 5)).accepted(), "0.5 px inside the box is an edge pixel too");
    }

    @Test
    void twoHoverEdgePixelsAreAcceptedAndThreeAreNot() {
        assertTrue(hover(with(with(19, 20, 3), 19, 21, 18)).accepted());
        PixelPolicy.Verdict v = hover(with(with(with(19, 20, 1), 19, 21, 1), 19, 22, 1));
        assertFalse(v.accepted(), v.describe());
        assertEquals(3, v.differing());
    }

    @Test
    void aHoverPixelInAFlatAreaIsRejectedEvenInsideTheBand() {
        PixelPolicy.Verdict v = hover(with(18, 20, 1));
        assertFalse(v.accepted(), v.describe());
        assertEquals(0, v.outsideBand(), "1.5 px outside the box is inside the hover band");
        assertEquals(1, v.notEdge());
    }

    @Test
    void aHoverPixelOutsideTheBandIsRejected() {
        assertTrue(PixelPolicy.compare(picture(), with(19, 20, 5), PixelPolicy.HOVER_TEXT_EDGE, NO_RING).accepted());
        // The same pixel with the element somewhere else: the band moves with the browser's geometry.
        PixelPolicy.Geometry elsewhere = new PixelPolicy.Geometry(new PixelPolicy.Box(40, 14, 10, 12), 0);
        PixelPolicy.Verdict v = PixelPolicy.compare(picture(), with(19, 20, 5), PixelPolicy.HOVER_TEXT_EDGE, elsewhere);
        assertFalse(v.accepted(), v.describe());
        assertEquals(1, v.outsideBand());
        // An edge pixel of the stripe, 1.5 px inside the box, is beyond the 1 px the hover band allows.
        assertFalse(hover(with(21, 20, 5)).accepted());
    }

    @Test
    void theHoverPolicyDoesNotAddTheRingExtent() {
        // x = 17 is 2.5 px outside the box: beyond the hover band (2.0) even when the geometry says a ring reaches 2 px; the focus policy accepts the very same pixel there (band 3.5).
        BufferedImage candidate = with(17, 20, 5);
        PixelPolicy.Verdict hover = PixelPolicy.compare(picture(), candidate, PixelPolicy.HOVER_TEXT_EDGE, RING);
        assertFalse(hover.accepted());
        assertEquals(1, hover.outsideBand());
        assertTrue(PixelPolicy.compare(picture(), candidate, PixelPolicy.FOCUS_RING, RING).accepted());
    }

    @Test
    void theRingPolicyDoesNotRequireAnEdgePixelButTheHoverPolicyDoes() {
        BufferedImage flatOutside = with(18, 20, 5);
        assertTrue(ring(flatOutside).accepted());
        assertFalse(hover(flatOutside).accepted());
    }

    // ---------------------------------------------------------------- geometry that cannot describe a region

    private static final double[] NOT_FINITE = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};

    /** Every way the browser's answer could be unusable: a NaN or an infinity in any of the five numbers, a negative size or reach, and no box at all. */
    private static List<PixelPolicy.Geometry> unusableGeometries() {
        List<PixelPolicy.Geometry> all = new ArrayList<>();
        for (double bad : NOT_FINITE) {
            all.add(new PixelPolicy.Geometry(new PixelPolicy.Box(bad, 14, 20, 12), 2));
            all.add(new PixelPolicy.Geometry(new PixelPolicy.Box(20, bad, 20, 12), 2));
            all.add(new PixelPolicy.Geometry(new PixelPolicy.Box(20, 14, bad, 12), 2));
            all.add(new PixelPolicy.Geometry(new PixelPolicy.Box(20, 14, 20, bad), 2));
            all.add(new PixelPolicy.Geometry(new PixelPolicy.Box(20, 14, 20, 12), bad));
        }
        all.add(new PixelPolicy.Geometry(new PixelPolicy.Box(20, 14, -1, 12), 2));
        all.add(new PixelPolicy.Geometry(new PixelPolicy.Box(20, 14, 20, -1), 2));
        all.add(new PixelPolicy.Geometry(new PixelPolicy.Box(20, 14, 20, 12), -1));
        all.add(new PixelPolicy.Geometry(null, 2));
        return all;
    }

    @Test
    void anUnusableGeometryNeverAuthorizesADifferingPicture() throws IOException {
        // The first candidate is accepted under the finite geometry RING (the control), so a rejection below is due to the geometry alone: an edge pixel on the ring, a flat pixel next to it, two flat
        // pixels farther away.
        List<BufferedImage> candidates = List.of(with(19, 20, 5), with(18, 20, 1), with(2, 2, 1), with(30, 20, 1));
        for (PixelPolicy.Policy policy : List.of(PixelPolicy.FOCUS_RING, PixelPolicy.HOVER_TEXT_EDGE)) {
            assertTrue(PixelPolicy.compare(picture(), candidates.get(0), policy, RING).accepted(), policy.name() + ": the control is accepted under a finite geometry");
            for (PixelPolicy.Geometry bad : unusableGeometries()) {
                for (BufferedImage candidate : candidates) {
                    PixelPolicy.Verdict v = PixelPolicy.compare(picture(), candidate, policy, bad);
                    assertFalse(v.accepted(), policy.name() + " with " + bad + ": " + v.describe());
                    assertTrue(v.reason().contains("not usable"), v.reason());
                    PixelPolicy.Verdict encoded = PixelPolicy.compare(png(picture()), png(candidate), policy, bad);
                    assertFalse(encoded.accepted(), policy.name() + " with " + bad + " (encoded): " + encoded.describe());
                }
            }
        }
    }

    @Test
    void anUnusableOrMissingGeometryToleratesNothingUnderTheExactPolicyEither() {
        for (PixelPolicy.Geometry bad : unusableGeometries()) {
            assertFalse(PixelPolicy.compare(picture(), with(19, 20, 1), PixelPolicy.EXACT, bad).accepted(), String.valueOf(bad));
        }
        assertFalse(PixelPolicy.compare(picture(), with(19, 20, 1), PixelPolicy.FOCUS_RING, null).accepted());
        assertFalse(PixelPolicy.compare(picture(), with(19, 20, 1), PixelPolicy.HOVER_TEXT_EDGE, null).accepted());
    }

    @Test
    void exactEqualityStillPassesWithoutAnyUsableGeometry() throws IOException {
        String same = png(picture());
        for (PixelPolicy.Policy policy : List.of(PixelPolicy.EXACT, PixelPolicy.FOCUS_RING, PixelPolicy.HOVER_TEXT_EDGE)) {
            for (PixelPolicy.Geometry bad : unusableGeometries()) {
                PixelPolicy.Verdict v = PixelPolicy.compare(same, png(picture()), policy, bad);
                assertTrue(v.accepted() && v.exact(), policy.name() + " with " + bad);
                // Pictures with no differing pixel are not a difference either, whatever the geometry.
                PixelPolicy.Verdict pixels = PixelPolicy.compare(picture(), picture(), policy, bad);
                assertTrue(pixels.accepted(), policy.name() + " with " + bad);
                assertEquals(0, pixels.differing());
            }
            assertTrue(PixelPolicy.compare(same, png(picture()), policy, null).accepted());
        }
    }

    @Test
    void theBrowsersAnswerIsRefusedWhenItIsNotFiveFiniteNonNegativeNumbers() throws IOException {
        for (String bad : List.of("NaN 14 20 12 2", "20 NaN 20 12 2", "20 14 Infinity 12 2", "20 14 20 -Infinity 2", "20 14 20 12 NaN", "20 14 20 12 Infinity", "-Infinity 14 20 12 2", "20 14 -1 12 2",
                "20 14 20 -1 2", "20 14 20 12 -1", "1 2 3", "1 2 3 4 5 6", "a b c d e", "", "   ")) {
            assertNull(PixelPolicy.Geometry.parse(bad), "[" + bad + "]");
            assertFalse(PixelPolicy.compare(png(picture()), png(with(19, 20, 1)), PixelPolicy.FOCUS_RING, PixelPolicy.Geometry.parse(bad)).accepted(), "[" + bad + "]");
        }
    }

    @Test
    void validFiniteGeometryKeepsItsBehavior() throws IOException {
        assertEquals(RING, PixelPolicy.Geometry.parse("20 14 20 12 2"));
        assertEquals(RING, PixelPolicy.Geometry.parse("  20.0 14 20 12 2.0 "));
        assertEquals(NO_RING, PixelPolicy.Geometry.parse("20 14 20 12 0"));
        assertTrue(RING.valid() && NO_RING.valid());
        assertTrue(new PixelPolicy.Geometry(new PixelPolicy.Box(0, 0, 0, 0), 0).valid(), "an empty box at the origin is a valid, if useless, geometry");
        assertTrue(new PixelPolicy.Geometry(new PixelPolicy.Box(-5.5, -3, 20, 12), 2).valid(), "a negative position is fine; only sizes and the reach must not be negative");
        // The behaviors the earlier tests pin, once more through parsed geometry.
        PixelPolicy.Geometry parsed = PixelPolicy.Geometry.parse("20 14 20 12 2");
        assertTrue(PixelPolicy.compare(png(picture()), png(with(19, 20, 12)), PixelPolicy.FOCUS_RING, parsed).accepted());
        assertFalse(PixelPolicy.compare(png(picture()), png(with(19, 20, 13)), PixelPolicy.FOCUS_RING, parsed).accepted());
        assertFalse(PixelPolicy.compare(png(picture()), png(with(2, 2, 1)), PixelPolicy.FOCUS_RING, parsed).accepted());
        assertTrue(PixelPolicy.compare(png(picture()), png(with(19, 20, 24)), PixelPolicy.HOVER_TEXT_EDGE, parsed).accepted());
        assertFalse(PixelPolicy.compare(png(picture()), png(with(18, 20, 1)), PixelPolicy.HOVER_TEXT_EDGE, parsed).accepted());
    }

    @Test
    void theBandTestFailsClosedOnAnythingThatIsNotANumberBetweenTheLimits() {
        assertTrue(PixelPolicy.inBand(0.5, -1.0, 3.5));
        assertTrue(PixelPolicy.inBand(-1.0, -1.0, 3.5), "the limits belong to the band");
        assertTrue(PixelPolicy.inBand(3.5, -1.0, 3.5));
        assertFalse(PixelPolicy.inBand(3.6, -1.0, 3.5));
        assertFalse(PixelPolicy.inBand(-1.1, -1.0, 3.5));
        for (double bad : NOT_FINITE) {
            assertFalse(PixelPolicy.inBand(bad, -1.0, 3.5), "distance " + bad);
        }
        assertFalse(PixelPolicy.inBand(0.5, Double.NaN, 3.5), "a NaN lower limit");
        assertFalse(PixelPolicy.inBand(0.5, -1.0, Double.NaN), "a NaN upper limit");
        assertFalse(PixelPolicy.inBand(0.5, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY), "an empty band at infinity");
    }
}
