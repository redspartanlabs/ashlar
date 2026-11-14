package dev.redspartanlabs.ashlar.showcase.cascade;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Says HOW two pictures differ, after a comparison has said THAT they differ. It measures the difference (how many pixels, how far off, where), and renders it, so that a failure in CI
 * can be read without reproducing it.
 *
 * <p>This class decides nothing. It is never consulted by {@link PixelPolicy}, never changes a verdict, an assertion or a tolerance, and it is meant to run only after every picture of a
 * suite has been captured: the capture loop keeps references to the pictures it has already taken (see {@link Capture}) and nothing else, so no measurement, drawing or file write can
 * happen while a browser is taking a picture. A suite calls {@link #report} inside a handler that prints and continues, so that a defect here can never fail it.
 *
 * <p>The numbers are defined to agree with {@link PixelPolicy.Verdict} wherever both exist: a pixel differs when its ARGB value differs, and the channel difference is the largest
 * absolute difference over red, green and blue. The exact policy never decodes the pictures (it compares the encoded text), so for a case compared exactly this is the only place the
 * magnitude shows. A pair of pictures can also differ in their encoding and not in a single pixel; the measurement then says {@code differing=0}, which is itself the finding.
 *
 * <p>Output: per pair that is not byte-identical, one line {@code <prefix> <REJECTED|tolerated> [n] <label>: <numbers> | <context> | <geometry>}. For the first few rejected pairs, also the pictures
 * themselves, one line each: {@code <prefix> image [n] <reference|candidate|zoom> <base64 PNG>}, which a test report keeps in its {@code system-out}; decode a line with
 * {@code base64 -d}. The same four files are written under the directory given: {@code reference.png}, {@code candidate.png}, {@code diff.png} (the reference faded, every differing
 * pixel magenta, the bounding rectangle outlined) and {@code zoom.png} (reference, candidate and difference side by side, cropped to the difference and enlarged).
 */
final class PixelDiagnostics {

    private PixelDiagnostics() {
    }

    /** Differing pixels listed individually per pair. */
    static final int MAX_SAMPLES = 12;
    /** Rejected pairs whose pictures are printed (the numbers are printed for every pair). */
    static final int MAX_IMAGES = 4;
    /** Pixels kept around the difference in the enlarged view. */
    static final int ZOOM_MARGIN = 6;
    private static final int MAGENTA = 0xFFFF00FF;
    private static final int OUTLINE = 0xFFFF8000;
    private static final int OUTSIDE = 0xFFFFC8C8;

    /** One differing pixel, with both colors as ARGB. */
    record Sample(int x, int y, int reference, int candidate) {
        @Override
        public String toString() {
            return "(" + x + "," + y + ") " + argb(reference) + " -> " + argb(candidate);
        }

        private static String argb(int v) {
            return String.format("#%08X", v);
        }
    }

    /**
     * How two pictures differ. {@code differing}, the bounding rectangle and the samples cover the region both pictures have; when the sizes differ, {@code sameSize} is false and the
     * sizes are given. The bounding rectangle is inclusive and is -1 throughout when nothing differs.
     */
    record Difference(boolean sameSize, int referenceWidth, int referenceHeight, int candidateWidth, int candidateHeight, int differing, int maxChannel, int maxAlpha, int minX, int minY, int maxX,
                      int maxY, List<Sample> samples) {
        boolean any() {
            return differing > 0;
        }

        int width() {
            return any() ? maxX - minX + 1 : 0;
        }

        int height() {
            return any() ? maxY - minY + 1 : 0;
        }

        String describe() {
            String size = sameSize ? referenceWidth + "x" + referenceHeight : "reference " + referenceWidth + "x" + referenceHeight + ", candidate " + candidateWidth + "x" + candidateHeight + " (only the shared region is measured)";
            if (!any()) {
                return sameSize ? "pixel-identical in " + size + " (the encoded pictures differ, no pixel does)" : "no shared pixel differs, but the sizes differ: " + size;
            }
            return "differing=" + differing + " maxChannel=" + maxChannel + " maxAlpha=" + maxAlpha + " bounds=x" + minX + ".." + maxX + ",y" + minY + ".." + maxY + " (" + width() + "x" + height()
                    + ") size=" + size + " pixels=" + samples;
        }
    }

    // ---------------------------------------------------------------- measuring

    static Difference measure(String referencePng, String candidatePng) throws IOException {
        return measure(decode(referencePng), decode(candidatePng));
    }

    static Difference measure(BufferedImage reference, BufferedImage candidate) {
        int width = Math.min(reference.getWidth(), candidate.getWidth());
        int height = Math.min(reference.getHeight(), candidate.getHeight());
        int differing = 0;
        int maxChannel = 0;
        int maxAlpha = 0;
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = -1;
        int maxY = -1;
        List<Sample> samples = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int p = reference.getRGB(x, y);
                int q = candidate.getRGB(x, y);
                if (p == q) {
                    continue;
                }
                differing++;
                for (int shift = 0; shift <= 16; shift += 8) {
                    maxChannel = Math.max(maxChannel, Math.abs(((p >> shift) & 0xFF) - ((q >> shift) & 0xFF)));
                }
                maxAlpha = Math.max(maxAlpha, Math.abs(((p >>> 24) & 0xFF) - ((q >>> 24) & 0xFF)));
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
                if (samples.size() < MAX_SAMPLES) {
                    samples.add(new Sample(x, y, p, q));
                }
            }
        }
        boolean same = reference.getWidth() == candidate.getWidth() && reference.getHeight() == candidate.getHeight();
        return new Difference(same, reference.getWidth(), reference.getHeight(), candidate.getWidth(), candidate.getHeight(), differing, maxChannel, maxAlpha, differing > 0 ? minX : -1,
                differing > 0 ? minY : -1, maxX, maxY, List.copyOf(samples));
    }

    static BufferedImage decode(String base64Png) throws IOException {
        BufferedImage image = PixelPolicy.decode(base64Png);
        if (image == null) {
            throw new IOException("not a picture the platform can decode");
        }
        return image;
    }

    // ---------------------------------------------------------------- drawing

    /** The reference, faded; every differing pixel in magenta; the bounding rectangle outlined two pixels outside, so that no differing pixel is covered. Null when nothing differs. */
    static BufferedImage overlay(BufferedImage reference, BufferedImage candidate, Difference difference) {
        if (!difference.any()) {
            return null;
        }
        int width = Math.max(reference.getWidth(), candidate.getWidth());
        int height = Math.max(reference.getHeight(), candidate.getHeight());
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int sharedWidth = Math.min(reference.getWidth(), candidate.getWidth());
        int sharedHeight = Math.min(reference.getHeight(), candidate.getHeight());
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x >= sharedWidth || y >= sharedHeight) {
                    out.setRGB(x, y, OUTSIDE);
                } else if (reference.getRGB(x, y) != candidate.getRGB(x, y)) {
                    out.setRGB(x, y, MAGENTA);
                } else {
                    out.setRGB(x, y, faded(reference.getRGB(x, y)));
                }
            }
        }
        int left = difference.minX() - 2;
        int top = difference.minY() - 2;
        int right = difference.maxX() + 2;
        int bottom = difference.maxY() + 2;
        for (int x = left; x <= right; x++) {
            plot(out, x, top);
            plot(out, x, bottom);
        }
        for (int y = top; y <= bottom; y++) {
            plot(out, left, y);
            plot(out, right, y);
        }
        return out;
    }

    private static void plot(BufferedImage image, int x, int y) {
        if (x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight()) {
            image.setRGB(x, y, OUTLINE);
        }
    }

    /** A color pulled three quarters of the way to white, opaque. */
    private static int faded(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return 0xFF000000 | ((255 - (255 - r) / 4) << 16) | ((255 - (255 - g) / 4) << 8) | (255 - (255 - b) / 4);
    }

    /** Horizontal space between the three panels of the enlarged view. */
    static final int GUTTER = 8;

    /**
     * Reference, candidate and difference side by side, cropped to the difference plus {@link #ZOOM_MARGIN} pixels on each side (inside the region both pictures have) and enlarged by an
     * integer factor, with nearest-neighbor, so that every source pixel is a square of one color. The factor is the largest, up to 8, that keeps one panel within 600 pixels. Null when
     * nothing differs.
     */
    static BufferedImage zoom(BufferedImage reference, BufferedImage candidate, Difference difference) {
        if (!difference.any()) {
            return null;
        }
        int sharedWidth = Math.min(reference.getWidth(), candidate.getWidth());
        int sharedHeight = Math.min(reference.getHeight(), candidate.getHeight());
        int x0 = Math.max(0, difference.minX() - ZOOM_MARGIN);
        int y0 = Math.max(0, difference.minY() - ZOOM_MARGIN);
        int x1 = Math.min(sharedWidth - 1, difference.maxX() + ZOOM_MARGIN);
        int y1 = Math.min(sharedHeight - 1, difference.maxY() + ZOOM_MARGIN);
        int w = x1 - x0 + 1;
        int h = y1 - y0 + 1;
        int scale = Math.max(1, Math.min(8, 600 / Math.max(w, h)));
        BufferedImage out = new BufferedImage(3 * w * scale + 2 * GUTTER, h * scale, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < out.getHeight(); y++) {
            for (int x = 0; x < out.getWidth(); x++) {
                out.setRGB(x, y, 0xFFFFFFFF);
            }
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = reference.getRGB(x0 + x, y0 + y);
                int q = candidate.getRGB(x0 + x, y0 + y);
                square(out, 0, x, y, scale, 0xFF000000 | p);
                square(out, 1, x, y, scale, 0xFF000000 | q);
                square(out, 2, x, y, scale, p != q ? MAGENTA : faded(p));
            }
        }
        return out;
    }

    /** Paints the source pixel (x, y) as a square in panel {@code panel} of the enlarged view; the panel width follows from the output width. */
    private static void square(BufferedImage out, int panel, int x, int y, int scale, int argb) {
        int panelWidth = (out.getWidth() - 2 * GUTTER) / 3;
        int left = panel * (panelWidth + GUTTER) + x * scale;
        for (int j = 0; j < scale; j++) {
            for (int i = 0; i < scale; i++) {
                out.setRGB(left + i, y * scale + j, argb);
            }
        }
    }

    static String png(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", out)) {
            throw new IOException("no PNG writer");
        }
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    // ---------------------------------------------------------------- the report

    /**
     * A pair of pictures that was not byte-identical, with what is known about where it came from. {@code context} is printed in the order given. {@code verdict} is the policy's own
     * description, copied unchanged.
     */
    record Capture(String label, Map<String, String> context, String reference, String candidate, boolean accepted, String verdict, PixelPolicy.Geometry geometry) {
    }

    /**
     * Prints one line per pair, the pictures of the first {@link #MAX_IMAGES} rejected pairs, and writes their files under {@code directory} (when it is not null). A pair that cannot be
     * measured is reported as such, and the rest are still reported.
     */
    static void report(String prefix, List<Capture> captures, Path directory, PrintStream out, String environment) {
        long rejected = captures.stream().filter(c -> !c.accepted()).count();
        out.println(prefix + " diagnostics: " + captures.size() + " pair(s) not byte-identical, " + (captures.size() - rejected) + " tolerated, " + rejected + " rejected");
        out.println(prefix + " environment: " + environment);
        int imaged = 0;
        for (int n = 0; n < captures.size(); n++) {
            Capture c = captures.get(n);
            String head = prefix + " " + (c.accepted() ? "tolerated" : "REJECTED") + " [" + n + "] " + c.label();
            try {
                BufferedImage reference = decode(c.reference());
                BufferedImage candidate = decode(c.candidate());
                Difference d = measure(reference, candidate);
                out.println(head + ": " + d.describe() + " | " + context(c.context()) + " | policy said: " + c.verdict() + " | geometry=" + c.geometry());
                if (!c.accepted() && imaged < MAX_IMAGES) {
                    imaged++;
                    out.println(prefix + " image [" + n + "] reference " + c.reference());
                    out.println(prefix + " image [" + n + "] candidate " + c.candidate());
                    BufferedImage zoom = zoom(reference, candidate, d);
                    if (zoom != null) {
                        out.println(prefix + " image [" + n + "] zoom " + png(zoom));
                    }
                    if (directory != null) {
                        try {
                            write(directory.resolve(n + "-" + safe(c.label())), c, reference, candidate, d, zoom);
                        } catch (IOException | RuntimeException e) {
                            out.println(prefix + " files [" + n + "]: could not be written (" + e + ")");
                        }
                    }
                }
            } catch (IOException | RuntimeException e) {
                out.println(head + ": the pair could not be measured (" + e + ") | " + context(c.context()) + " | policy said: " + c.verdict());
            }
        }
        if (rejected > MAX_IMAGES) {
            out.println(prefix + " images: shown for the first " + MAX_IMAGES + " of " + rejected + " rejected pairs; the numbers above cover all of them");
        }
    }

    private static void write(Path folder, Capture c, BufferedImage reference, BufferedImage candidate, Difference d, BufferedImage zoom) throws IOException {
        Files.createDirectories(folder);
        Files.write(folder.resolve("reference.png"), Base64.getDecoder().decode(c.reference()));
        Files.write(folder.resolve("candidate.png"), Base64.getDecoder().decode(c.candidate()));
        BufferedImage overlay = overlay(reference, candidate, d);
        if (overlay != null) {
            ImageIO.write(overlay, "png", folder.resolve("diff.png").toFile());
        }
        if (zoom != null) {
            ImageIO.write(zoom, "png", folder.resolve("zoom.png").toFile());
        }
        Files.writeString(folder.resolve("report.txt"), c.label() + System.lineSeparator() + d.describe() + System.lineSeparator() + context(c.context()) + System.lineSeparator() + c.verdict()
                + System.lineSeparator());
    }

    static String safe(String label) {
        String s = label.replaceAll("[^A-Za-z0-9._-]+", "-").replaceAll("^-+|-+$", "");
        return s.length() > 80 ? s.substring(0, 80) : s;
    }

    private static String context(Map<String, String> context) {
        StringBuilder sb = new StringBuilder();
        context.forEach((k, v) -> sb.append(sb.length() == 0 ? "" : " ").append(k).append('=').append(v));
        return sb.toString();
    }

    // ---------------------------------------------------------------- the environment

    /** What the pictures were taken with: the machine, the Java runtime, the runner (on GitHub Actions) and the browser's own version string. Nothing here can fail. */
    static String environment(Path chromeExecutable) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"));
        m.put("cpus", String.valueOf(Runtime.getRuntime().availableProcessors()));
        m.put("java", System.getProperty("java.version"));
        m.put("ci", String.valueOf(System.getenv("CI") != null));
        for (String name : new String[] {"RUNNER_NAME", "RUNNER_OS", "RUNNER_ARCH", "ImageOS", "ImageVersion"}) {
            String v = System.getenv(name);
            if (v != null) {
                m.put(name, v);
            }
        }
        m.put("chrome", chromeVersion(chromeExecutable));
        return context(m);
    }

    /** The output of {@code <browser> --version}, or {@code unknown}. */
    static String chromeVersion(Path executable) {
        Path file = null;
        try {
            file = Files.createTempFile("browser-version", ".txt");
            Process process = new ProcessBuilder(executable.toString(), "--version").redirectErrorStream(true).redirectOutput(file.toFile()).start();
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return "unknown";
            }
            String text = Files.readString(file, java.nio.charset.StandardCharsets.UTF_8).trim().replaceAll("\\s+", " ");
            return text.isEmpty() ? "unknown" : "\"" + text + "\"";
        } catch (IOException | RuntimeException e) {
            return "unknown";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "unknown";
        } finally {
            if (file != null) {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException ignored) {
                    // a leftover temporary file is not worth a failure
                }
            }
        }
    }
}
