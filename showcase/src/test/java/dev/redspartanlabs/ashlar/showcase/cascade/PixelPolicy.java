package dev.redspartanlabs.ashlar.showcase.cascade;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * How two pictures of the same case are compared. The default is exact equality of the encoded pictures, and that is what every case gets unless it is named below.
 *
 * <p>Two kinds of case are named because Chrome was measured to rasterise one anti-aliased edge pixel of them differently from one capture to the next, in the legacy rendering and in the
 * migrated one alike (for the ring, 4.2% of legacy and 5.0% of migrated captures differed from their own most common picture; the same pixel and the same alternate color on both sides):
 * <ul>
 * <li>the sort button with a forced {@code :focus-visible}: the outer corner of its focus ring, one pixel, at most 7/255 off, in all 6 variants observed;</li>
 * <li>the sort buttons with a forced {@code :hover}: the fringe of the first letter's left edge, one or two pixels, at most 18/255 off, in all 4 variants observed (of 8 hover cases in 22 configurations, only the two sort-button cases ever varied).</li>
 * </ul>
 * For those, a pair that is not byte-identical is still accepted when it is the same size and differs in at most {@link Policy#maxDifferingPixels()} pixels, each of them within a band
 * around the element's border box that is computed from the browser's geometry for that very page (never hard-coded coordinates), each channel within {@link Policy#maxChannelDifference()},
 * and, for the hover case, each of them an edge pixel (its neighborhood in the reference picture is not uniform). The band and the limits are the measured noise plus a margin, and nothing
 * more: a ring that is missing, thinner, displaced, weaker or differently rounded, a changed letter, a changed background, or any change outside the band changes far more pixels than
 * the limit, and {@code TablePixelTest} proves that against real renderings on every run.
 */
final class PixelPolicy {

    private PixelPolicy() {
    }

    /** A rectangle in picture coordinates. */
    record Box(double x, double y, double w, double h) {
        double right() {
            return x + w;
        }

        double bottom() {
            return y + h;
        }

        /** Distance from a point to the outside of the box: negative inside (to the nearest edge), positive outside. */
        double signedDistance(double px, double py) {
            double dx = Math.max(Math.max(x - px, 0), px - right());
            double dy = Math.max(Math.max(y - py, 0), py - bottom());
            if (dx > 0 || dy > 0) {
                return Math.hypot(dx, dy);
            }
            return -Math.min(Math.min(px - x, right() - px), Math.min(py - y, bottom() - py));
        }
    }

    /** The element whose state is forced, in the picture, and how far its focus ring reaches beyond its border box (0 when it has none). Always the reference (legacy) rendering's. */
    record Geometry(Box box, double ringExtent) {
        /** The browser's answer, {@code "x y width height ringExtent"} (see {@code TablePixelTest#geometryScript}). */
        static Geometry parse(String text) {
            String[] n = text.trim().split(" ");
            return new Geometry(new Box(Double.parseDouble(n[0]), Double.parseDouble(n[1]), Double.parseDouble(n[2]), Double.parseDouble(n[3])), Double.parseDouble(n[4]));
        }
    }

    /**
     * What may differ. The band is the set of pixel centers whose signed distance to the element's border box lies between {@code -bandInside} and {@code bandOutside} (plus the ring
     * extent when {@code addsRingExtent}).
     */
    record Policy(String name, int maxDifferingPixels, int maxChannelDifference, double bandInside, double bandOutside, boolean addsRingExtent, boolean edgePixelsOnly) {
    }

    static final Policy EXACT = new Policy("exact", 0, 0, 0, 0, false, false);
    /** Focus ring: the ring is the 2 px just outside the box; the band runs from 1 px inside the box to 1.5 px beyond the ring's outer edge. Measured: 1 pixel, 7/255. */
    static final Policy FOCUS_RING = new Policy("focus-ring", 2, 12, 1.0, 1.5, true, false);
    /** Hover on a sort button: the fringe of the first letter lands within a pixel of the border box's left edge. Band: 1 px inside to 2 px outside. Measured: 1-2 pixels, 18/255. */
    static final Policy HOVER_TEXT_EDGE = new Policy("hover-text-edge", 2, 24, 1.0, 2.0, false, true);

    /** The case keys (see {@link TableBrowser#CASES}) that may use a tolerance; every other case is compared exactly. */
    static Policy forCase(String key) {
        return switch (key) {
            case "sort button focus-visible" -> FOCUS_RING;
            case "sort button hover", "active sort button hover" -> HOVER_TEXT_EDGE;
            default -> EXACT;
        };
    }

    /** The outcome of one comparison. {@code exact} is true only for byte-identical pictures. */
    record Verdict(boolean accepted, boolean exact, String policy, boolean sameSize, int differing, int maxChannel, int outsideBand, int notEdge, String reason) {
        String describe() {
            return exact ? "identical" : "policy=" + policy + " " + (accepted ? "tolerated" : "REJECTED") + ": " + reason + " [sameSize=" + sameSize + " differing=" + differing + " maxChannel=" + maxChannel
                    + " outsideBand=" + outsideBand + " notEdge=" + notEdge + "]";
        }
    }

    static BufferedImage decode(String base64Png) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(base64Png)));
    }

    /** The exact comparison first; a tolerance is considered only if the policy allows one and the pictures differ. */
    static Verdict compare(String legacyPng, String migratedPng, Policy policy, Geometry geometry) throws IOException {
        if (legacyPng.equals(migratedPng)) {
            return new Verdict(true, true, policy.name(), true, 0, 0, 0, 0, "identical");
        }
        if (policy.maxDifferingPixels() == 0) {
            return new Verdict(false, false, policy.name(), true, -1, -1, -1, -1, "the pictures differ and this case is compared exactly");
        }
        if (geometry == null) {
            return new Verdict(false, false, policy.name(), true, -1, -1, -1, -1, "no geometry for the permitted region");
        }
        return compare(decode(legacyPng), decode(migratedPng), policy, geometry);
    }

    static Verdict compare(BufferedImage reference, BufferedImage candidate, Policy policy, Geometry geometry) {
        if (reference.getWidth() != candidate.getWidth() || reference.getHeight() != candidate.getHeight()) {
            return new Verdict(false, false, policy.name(), false, -1, -1, -1, -1, "the pictures have different sizes");
        }
        double low = -policy.bandInside();
        double high = policy.bandOutside() + (policy.addsRingExtent() ? geometry.ringExtent() : 0);
        int differing = 0;
        int max = 0;
        int outside = 0;
        int notEdge = 0;
        for (int y = 0; y < reference.getHeight(); y++) {
            for (int x = 0; x < reference.getWidth(); x++) {
                int p = reference.getRGB(x, y);
                int q = candidate.getRGB(x, y);
                if (p == q) {
                    continue;
                }
                differing++;
                for (int shift = 0; shift <= 16; shift += 8) {
                    max = Math.max(max, Math.abs(((p >> shift) & 0xFF) - ((q >> shift) & 0xFF)));
                }
                double distance = geometry.box().signedDistance(x + 0.5, y + 0.5);
                if (distance < low || distance > high) {
                    outside++;
                }
                if (policy.edgePixelsOnly() && !isEdge(reference, x, y)) {
                    notEdge++;
                }
            }
        }
        String reason = differing > policy.maxDifferingPixels() ? differing + " pixels differ, at most " + policy.maxDifferingPixels() + " may"
                : max > policy.maxChannelDifference() ? "a channel differs by " + max + ", at most " + policy.maxChannelDifference() + " may"
                : outside > 0 ? outside + " differing pixel(s) lie outside the permitted band around the element"
                : notEdge > 0 ? notEdge + " differing pixel(s) are not edge pixels" : "within the policy";
        boolean accepted = reason.equals("within the policy");
        return new Verdict(accepted, false, policy.name(), true, differing, max, outside, notEdge, reason);
    }

    /** A pixel is an edge pixel when at least one of its eight neighbors has another color. */
    static boolean isEdge(BufferedImage image, int x, int y) {
        int color = image.getRGB(x, y) & 0xFFFFFF;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx;
                int ny = y + dy;
                if (nx >= 0 && ny >= 0 && nx < image.getWidth() && ny < image.getHeight() && (image.getRGB(nx, ny) & 0xFFFFFF) != color) {
                    return true;
                }
            }
        }
        return false;
    }
}
