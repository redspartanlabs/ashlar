package dev.redspartanlabs.ashlar.showcase.cascade;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Says HOW two pictures differ, after a comparison has said THAT they differ: how many pixels, how far off, where, and which of them are edge pixels. It decides nothing:
 * {@link PixelPolicy} still judges every pair; this only describes, for reports.
 *
 * <p>A pixel differs when its ARGB value differs; the channel difference is the largest absolute difference over red, green and blue (alpha is reported on its own), as in
 * {@link PixelPolicy.Verdict}. When the encoded pictures differ but no pixel does, the measurement says {@code differing=0}.
 */
final class PixelDiagnostics {

    private PixelDiagnostics() {
    }

    /** Differing pixels listed individually. */
    static final int MAX_SAMPLES = 32;
    /** The longest base64 text printed for one picture, and the most printed for all of them together. */
    static final int MAX_PICTURE_CHARS = 150_000;
    static final int MAX_TOTAL_PICTURE_CHARS = 600_000;

    /** One differing pixel, with both colors as ARGB. */
    record Sample(int x, int y, int reference, int candidate) {
        @Override
        public String toString() {
            return String.format("(%d,%d) #%08X -> #%08X", x, y, reference, candidate);
        }
    }

    /**
     * How two pictures differ, over the region both have. The bounding rectangle is inclusive and -1 throughout when nothing differs. {@code edgePixels} counts the differing pixels
     * that are edge pixels of the reference ({@link PixelPolicy#isEdge}).
     */
    record Difference(boolean sameSize, int referenceWidth, int referenceHeight, int candidateWidth, int candidateHeight, int differing, int maxChannel, int maxAlpha,
                      int minX, int minY, int maxX, int maxY, int edgePixels, List<Sample> samples) {
        boolean any() {
            return differing > 0;
        }

        String describe() {
            String size = sameSize ? referenceWidth + "x" + referenceHeight : "reference " + referenceWidth + "x" + referenceHeight + ", candidate " + candidateWidth + "x" + candidateHeight;
            if (!any()) {
                return sameSize ? "pixel-identical in " + size + " (the encoded pictures differ, no pixel does)" : "no shared pixel differs, but the sizes differ: " + size;
            }
            return "differing=" + differing + " maxChannel=" + maxChannel + " maxAlpha=" + maxAlpha + " bounds=x" + minX + ".." + maxX + ",y" + minY + ".." + maxY + " edge pixels="
                    + edgePixels + "/" + differing + " size=" + size + " pixels=" + samples + (differing > samples.size() ? " (first " + samples.size() + ")" : "");
        }
    }

    static Difference measure(String referencePng, String candidatePng) throws IOException {
        return measure(decode(referencePng), decode(candidatePng));
    }

    static Difference measure(BufferedImage reference, BufferedImage candidate) {
        int width = Math.min(reference.getWidth(), candidate.getWidth());
        int height = Math.min(reference.getHeight(), candidate.getHeight());
        int differing = 0;
        int maxChannel = 0;
        int maxAlpha = 0;
        int edges = 0;
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
                if (PixelPolicy.isEdge(reference, x, y)) {
                    edges++;
                }
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
        return new Difference(same, reference.getWidth(), reference.getHeight(), candidate.getWidth(), candidate.getHeight(), differing, maxChannel, maxAlpha,
                differing > 0 ? minX : -1, differing > 0 ? minY : -1, maxX, maxY, edges, List.copyOf(samples));
    }

    static BufferedImage decode(String base64Png) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(base64Png)));
        if (image == null) {
            throw new IOException("not a picture the platform can decode");
        }
        return image;
    }

    /** Prints pictures as base64 lines within a per-picture and a total limit; a picture over a limit is named and left out. */
    static final class PictureBudget {
        private long used;

        String line(String prefix, String what, String base64) {
            if (base64.length() > MAX_PICTURE_CHARS) {
                return prefix + " picture " + what + " omitted: " + base64.length() + " characters, at most " + MAX_PICTURE_CHARS + " are printed for one picture";
            }
            if (used + base64.length() > MAX_TOTAL_PICTURE_CHARS) {
                return prefix + " picture " + what + " omitted: printing its " + base64.length() + " characters would pass the total of " + MAX_TOTAL_PICTURE_CHARS;
            }
            used += base64.length();
            return prefix + " picture " + what + " " + base64;
        }

        long used() {
            return used;
        }
    }

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
        StringBuilder sb = new StringBuilder();
        m.forEach((k, v) -> sb.append(sb.length() == 0 ? "" : " ").append(k).append('=').append(v));
        return sb.toString();
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
            String text = Files.readString(file, StandardCharsets.UTF_8).trim().replaceAll("\\s+", " ");
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
