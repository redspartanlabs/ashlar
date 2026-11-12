package dev.redspartanlabs.ashlar.showcase.cascade;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * DIAGNOSTIC ONLY. Explains, after the fact, why {@link TablePixelTest} found a legacy and a migrated picture different. It never takes part in the comparison: it is called after
 * every picture has been captured and compared, only for the four configurations under investigation, and everything it does is reported on standard output (which the test report
 * keeps and CI uploads with the shard's reports). The exact-equality assertions are untouched; no tolerance, mask or normalisation exists here.
 *
 * <p>For each candidate it reports the pixel statistics of the two pictures (dimensions, differing pixels, bounding box, per-channel differences), where the differences are relative to
 * the focused button's ring, the ring's colour profile on both sides, and four images (legacy, migrated, a difference visualisation, and a magnified side-by-side sheet). Lines are
 * {@code PIXDIAG|...} (text), {@code PIXDIAG-JSON|...} (machine-readable) and {@code PIXDIAG-PNG|id|kind|n/total|base64} (images, in chunks).
 */
final class PixelDiagnostics {

    private PixelDiagnostics() {
    }

    /** The configurations under investigation; nothing else is ever reported. */
    static final Set<String> TARGETS = Set.of("light/civic", "light/ledger", "dark/summit", "dark/veridian");

    /** Set ASHLAR_PIXEL_DIAG_FORCE=1 to report the focus-visible sort button case in the target configurations even when the pictures are equal (to exercise this code locally). */
    static final boolean FORCE = "1".equals(System.getenv("ASHLAR_PIXEL_DIAG_FORCE"));

    /** One pair of pictures worth explaining, with what is needed to capture it again. */
    record Candidate(String configId, String fieldId, String caseKey, String legacy, String migrated, String legacyPage, String migratedPage, TableParityTest.Field field) {
        String id() {
            return (configId + "-" + fieldId).replace('/', '-');
        }
    }

    /** Every differing pair is explained (the hover cases of the last CI run were outside the original target list); FORCE still only adds the focus-visible case of the target configurations. */
    static boolean wanted(String configId, String caseKey, boolean differs) {
        return differs || (FORCE && TARGETS.contains(configId) && caseKey.equals("sort button focus-visible"));
    }

    // ------------------------------------------------------------------ analysis (pure, self-checked)

    /** A rectangle in picture coordinates. */
    record Box(double x, double y, double w, double h) {
        double right() {
            return x + w;
        }

        double bottom() {
            return y + h;
        }
    }

    record Stats(int width, int height, boolean sameSize, int differing, double percent, int minX, int minY, int maxX, int maxY, int maxChannel, double meanChannelOfDiffering,
                 double meanChannelOfAll, int inRingZone, int outsideRingZone, int nearCorners, int firstDifferingPixelsListed, List<String> samples) {
    }

    /** Distance from a point to the outside of a box: negative inside (to the nearest edge), positive outside. */
    static double signedDistance(Box b, double px, double py) {
        double dx = Math.max(Math.max(b.x() - px, 0), px - b.right());
        double dy = Math.max(Math.max(b.y() - py, 0), py - b.bottom());
        if (dx > 0 || dy > 0) {
            return Math.hypot(dx, dy);
        }
        return -Math.min(Math.min(px - b.x(), b.right() - px), Math.min(py - b.y(), b.bottom() - py));
    }

    /**
     * Compares two pictures pixel by pixel. The ring zone is the band from 1.5 px inside the button's box to 3.5 px outside it (the ring is the 2 px just outside); the corner zone is
     * within 6 px of a corner of the button's box. {@code button} may be null.
     */
    static Stats analyze(BufferedImage a, BufferedImage b, Box button) {
        int w = Math.min(a.getWidth(), b.getWidth());
        int h = Math.min(a.getHeight(), b.getHeight());
        boolean same = a.getWidth() == b.getWidth() && a.getHeight() == b.getHeight();
        int differing = 0;
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = -1;
        int maxY = -1;
        int maxChannel = 0;
        long channelSum = 0;
        long channelCount = 0;
        int ring = 0;
        int outside = 0;
        int corners = 0;
        List<String> samples = new ArrayList<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = a.getRGB(x, y);
                int q = b.getRGB(x, y);
                if (p == q) {
                    continue;
                }
                differing++;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
                for (int shift = 0; shift <= 16; shift += 8) { // red, green, blue; the pictures are opaque
                    int d = Math.abs(((p >> shift) & 0xFF) - ((q >> shift) & 0xFF));
                    maxChannel = Math.max(maxChannel, d);
                    channelSum += d;
                    channelCount++;
                }
                if (button != null) {
                    double dist = signedDistance(button, x + 0.5, y + 0.5);
                    if (dist >= -1.5 && dist <= 3.5) {
                        ring++;
                    } else {
                        outside++;
                    }
                    for (double[] c : new double[][] {{button.x(), button.y()}, {button.right(), button.y()}, {button.x(), button.bottom()}, {button.right(), button.bottom()}}) {
                        if (Math.abs(x + 0.5 - c[0]) <= 6 && Math.abs(y + 0.5 - c[1]) <= 6) {
                            corners++;
                            break;
                        }
                    }
                }
                if (samples.size() < 60) {
                    samples.add(x + "," + y + " legacy=" + hex(p) + " migrated=" + hex(q));
                }
            }
        }
        int total = w * h;
        return new Stats(w, h, same, differing, total == 0 ? 0 : 100.0 * differing / total, differing == 0 ? -1 : minX, differing == 0 ? -1 : minY, maxX, maxY, maxChannel,
                channelCount == 0 ? 0 : (double) channelSum / channelCount, total == 0 ? 0 : (double) channelSum / (3.0 * total), ring, outside, corners, samples.size(), samples);
    }

    static String hex(int argb) {
        return String.format("#%02x%02x%02x%02x", (argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF);
    }

    /** The ring's colours sampled outward from each side's midline: for each side, the pixels from 3 px inside the box edge to 5 px outside it. */
    static List<String> ringProfile(BufferedImage img, Box b) {
        List<String> sides = new ArrayList<>();
        int cx = (int) Math.round(b.x() + b.w() / 2);
        int cy = (int) Math.round(b.y() + b.h() / 2);
        StringBuilder left = new StringBuilder("left  ");
        StringBuilder right = new StringBuilder("right ");
        StringBuilder top = new StringBuilder("top   ");
        StringBuilder bottom = new StringBuilder("bottom ");
        for (int d = -3; d <= 5; d++) {
            left.append(sample(img, (int) Math.round(b.x()) - d - 1, cy)).append(' ');
            right.append(sample(img, (int) Math.round(b.right()) + d, cy)).append(' ');
            top.append(sample(img, cx, (int) Math.round(b.y()) - d - 1)).append(' ');
            bottom.append(sample(img, cx, (int) Math.round(b.bottom()) + d)).append(' ');
        }
        sides.add("offsets -3..+5 from the box edge (negative = inside): ");
        sides.add(left.toString());
        sides.add(right.toString());
        sides.add(top.toString());
        sides.add(bottom.toString());
        return sides;
    }

    private static String sample(BufferedImage img, int x, int y) {
        return x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight() ? "(out)" : hex(img.getRGB(x, y)).substring(0, 7);
    }

    /** A copy of the migrated picture dimmed to a quarter, with every differing pixel painted magenta. */
    static BufferedImage differenceImage(BufferedImage a, BufferedImage b) {
        int w = Math.min(a.getWidth(), b.getWidth());
        int h = Math.min(a.getHeight(), b.getHeight());
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int q = b.getRGB(x, y);
                if (a.getRGB(x, y) != q) {
                    out.setRGB(x, y, 0xFF00FF);
                } else {
                    int r = ((q >> 16) & 0xFF) / 4;
                    int g = ((q >> 8) & 0xFF) / 4;
                    int bl = (q & 0xFF) / 4;
                    out.setRGB(x, y, (r << 16) | (g << 8) | bl);
                }
            }
        }
        return out;
    }

    /** Legacy, migrated and difference crops around the differences (or around the button when there are none), magnified with nearest-neighbour, side by side. */
    static BufferedImage zoomSheet(BufferedImage a, BufferedImage b, BufferedImage diff, Stats s, Box button) {
        int cx0;
        int cy0;
        int cx1;
        int cy1;
        if (s.differing() > 0) {
            cx0 = s.minX();
            cy0 = s.minY();
            cx1 = s.maxX();
            cy1 = s.maxY();
        } else if (button != null) {
            cx0 = (int) button.x();
            cy0 = (int) button.y();
            cx1 = (int) button.right();
            cy1 = (int) button.bottom();
        } else {
            cx0 = 0;
            cy0 = 0;
            cx1 = Math.min(a.getWidth(), 40) - 1;
            cy1 = Math.min(a.getHeight(), 40) - 1;
        }
        int pad = 12;
        int x0 = Math.max(0, cx0 - pad);
        int y0 = Math.max(0, cy0 - pad);
        int x1 = Math.min(Math.min(a.getWidth(), b.getWidth()) - 1, cx1 + pad);
        int y1 = Math.min(Math.min(a.getHeight(), b.getHeight()) - 1, cy1 + pad);
        int cw = Math.max(1, x1 - x0 + 1);
        int ch = Math.max(1, y1 - y0 + 1);
        int scale = Math.max(1, Math.min(10, Math.min(1500 / (3 * cw + 8), 900 / ch)));
        BufferedImage sheet = new BufferedImage(3 * cw * scale + 2 * 8, ch * scale, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < sheet.getWidth(); i++) {
            for (int j = 0; j < sheet.getHeight(); j++) {
                sheet.setRGB(i, j, 0x808080);
            }
        }
        BufferedImage[] panels = {a, b, diff};
        for (int p = 0; p < 3; p++) {
            for (int y = 0; y < ch; y++) {
                for (int x = 0; x < cw; x++) {
                    int rgb = panels[p].getRGB(Math.min(x0 + x, panels[p].getWidth() - 1), Math.min(y0 + y, panels[p].getHeight() - 1));
                    for (int dy = 0; dy < scale; dy++) {
                        for (int dx = 0; dx < scale; dx++) {
                            sheet.setRGB(p * (cw * scale + 8) + x * scale + dx, y * scale + dy, rgb);
                        }
                    }
                }
            }
        }
        return sheet;
    }

    // ------------------------------------------------------------------ output

    static BufferedImage decode(String base64) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(base64)));
    }

    static String encode(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    static void line(String text) {
        System.out.println("PIXDIAG|" + text);
    }

    static void emitPng(String id, String kind, String base64) {
        int chunk = 6000;
        int total = Math.max(1, (base64.length() + chunk - 1) / chunk);
        for (int i = 0; i < total; i++) {
            System.out.println("PIXDIAG-PNG|" + id + "|" + kind + "|" + (i + 1) + "/" + total + "|" + base64.substring(i * chunk, Math.min(base64.length(), (i + 1) * chunk)));
        }
        try {
            Path dir = Path.of("build", "pixel-diagnostics", id);
            Files.createDirectories(dir);
            Files.write(dir.resolve(kind + ".png"), Base64.getDecoder().decode(base64));
        } catch (Throwable ignored) {
            // The standard output is the record; a local copy is only a convenience.
        }
    }

    static String json(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c < 0x20 ? String.format("\\u%04x", (int) c) : String.valueOf(c));
            }
        }
        return sb.append('"').toString();
    }

    /** What was learned about one candidate in the browser: geometry and computed style of the focused button on each side, and whether fresh captures repeat. */
    record Probe(String legacyInfo, String migratedInfo, boolean legacyRecaptureIdentical, boolean migratedRecaptureIdentical, int recaptures, String note) {
    }

    /** Parses the {@code box} field ({@code x,y,w,h} relative to the picture) of a probe's info JSON; null when absent. */
    static Box boxOf(String info) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"box\":\\[(-?[\\d.]+),(-?[\\d.]+),([\\d.]+),([\\d.]+)\\]").matcher(info == null ? "" : info);
        return m.find() ? new Box(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)), Double.parseDouble(m.group(3)), Double.parseDouble(m.group(4))) : null;
    }

    static void report(Candidate c, Probe probe) {
        String id = c.id();
        try {
            BufferedImage legacy = decode(c.legacy());
            BufferedImage migrated = decode(c.migrated());
            Box button = boxOf(probe.migratedInfo());
            Stats s = analyze(legacy, migrated, button);
            boolean identical = c.legacy().equals(c.migrated());
            line(id + " config=" + c.configId() + " case=" + c.caseKey() + " pictureBytesEqual=" + identical);
            line(id + " legacy=" + legacy.getWidth() + "x" + legacy.getHeight() + " migrated=" + migrated.getWidth() + "x" + migrated.getHeight() + " sameSize=" + s.sameSize());
            line(id + " differingPixels=" + s.differing() + " of " + (s.width() * s.height()) + " (" + String.format("%.4f", s.percent()) + "%)");
            line(id + " boundingBox=[" + s.minX() + "," + s.minY() + " .. " + s.maxX() + "," + s.maxY() + "]");
            line(id + " maxChannelDifference=" + s.maxChannel() + " meanChannelDifferenceOverDifferingPixels=" + String.format("%.3f", s.meanChannelOfDiffering())
                    + " meanOverAllChannels=" + String.format("%.6f", s.meanChannelOfAll()));
            line(id + " buttonBoxInPicture=" + (button == null ? "unknown" : button) + " differencesInRingZone=" + s.inRingZone() + " outsideRingZone=" + s.outsideRingZone()
                    + " withinSixPixelsOfAButtonCorner=" + s.nearCorners());
            for (String sample : s.samples()) {
                line(id + " diff " + sample);
            }
            List<String> legacyProfile = button == null ? List.of() : ringProfile(legacy, button);
            List<String> migratedProfile = button == null ? List.of() : ringProfile(migrated, button);
            line(id + " ringProfileEqual=" + legacyProfile.equals(migratedProfile));
            for (int i = 0; i < legacyProfile.size(); i++) {
                line(id + " ring legacy   " + legacyProfile.get(i));
                line(id + " ring migrated " + migratedProfile.get(i));
            }
            line(id + " legacyRecaptureIdentical=" + probe.legacyRecaptureIdentical() + " migratedRecaptureIdentical=" + probe.migratedRecaptureIdentical() + " recaptures=" + probe.recaptures()
                    + (probe.note().isEmpty() ? "" : " note=" + probe.note()));
            line(id + " legacyInfo=" + probe.legacyInfo());
            line(id + " migratedInfo=" + probe.migratedInfo());
            BufferedImage diff = differenceImage(legacy, migrated);
            emitPng(id, "legacy", c.legacy());
            emitPng(id, "migrated", c.migrated());
            emitPng(id, "difference", encode(diff));
            emitPng(id, "zoom-sheet", encode(zoomSheet(legacy, migrated, diff, s, button)));
            System.out.println("PIXDIAG-JSON|{\"id\":" + json(id) + ",\"config\":" + json(c.configId()) + ",\"case\":" + json(c.caseKey()) + ",\"width\":" + s.width() + ",\"height\":" + s.height()
                    + ",\"sameSize\":" + s.sameSize() + ",\"differingPixels\":" + s.differing() + ",\"percent\":" + s.percent() + ",\"bbox\":[" + s.minX() + "," + s.minY() + "," + s.maxX() + "," + s.maxY()
                    + "],\"maxChannel\":" + s.maxChannel() + ",\"meanChannelOfDiffering\":" + s.meanChannelOfDiffering() + ",\"inRingZone\":" + s.inRingZone() + ",\"outsideRingZone\":" + s.outsideRingZone()
                    + ",\"nearCorners\":" + s.nearCorners() + ",\"ringProfileEqual\":" + legacyProfile.equals(migratedProfile) + ",\"legacyRecaptureIdentical\":" + probe.legacyRecaptureIdentical()
                    + ",\"migratedRecaptureIdentical\":" + probe.migratedRecaptureIdentical() + ",\"recaptures\":" + probe.recaptures() + ",\"legacyInfo\":" + json(probe.legacyInfo())
                    + ",\"migratedInfo\":" + json(probe.migratedInfo()) + "}");
        } catch (Throwable t) {
            line(id + " diagnostic failed: " + t);
        }
    }

    // ------------------------------------------------------------------ self-check

    /** Checks the arithmetic above on pictures built to have known differences; the result is printed, never asserted, so a bug here cannot fail the test. */
    static void selfCheck() {
        try {
            BufferedImage a = new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB);
            BufferedImage b = new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < 10; y++) {
                for (int x = 0; x < 20; x++) {
                    a.setRGB(x, y, 0xFFFFFF);
                    b.setRGB(x, y, 0xFFFFFF);
                }
            }
            b.setRGB(2, 3, 0xFFFFF0);   // red/green equal, blue 15 less; far from the box below
            b.setRGB(10, 5, 0x000000);  // all channels 255
            b.setRGB(11, 5, 0xFEFEFE);  // all channels 1
            Box box = new Box(9, 4, 4, 2);
            Stats s = analyze(a, b, box);
            boolean ok = s.differing() == 3 && s.minX() == 2 && s.minY() == 3 && s.maxX() == 11 && s.maxY() == 5 && s.maxChannel() == 255
                    && Math.abs(s.meanChannelOfDiffering() - ((15 + 255 * 3 + 3) / 9.0)) < 1e-9 && s.inRingZone() == 2 && s.outsideRingZone() == 1 && s.sameSize()
                    && analyze(a, a, box).differing() == 0 && Math.abs(s.percent() - 1.5) < 1e-9;
            Box one = new Box(5, 5, 10, 10);
            ok &= Math.abs(signedDistance(one, 4, 10) - 1) < 1e-9 && Math.abs(signedDistance(one, 7, 10) + 2) < 1e-9 && Math.abs(signedDistance(one, 4, 4) - Math.sqrt(2)) < 1e-9;
            BufferedImage back = decode(encode(b));
            ok &= back.getRGB(10, 5) == b.getRGB(10, 5) && boxOf("{\"box\":[1.5,2,3.25,4]}").equals(new Box(1.5, 2, 3.25, 4));
            line("selfCheck " + (ok ? "PASS" : "FAIL") + " (stats " + s.differing() + " differing, " + s.inRingZone() + " in ring zone)");
        } catch (Throwable t) {
            line("selfCheck FAIL " + t);
        }
    }
}
