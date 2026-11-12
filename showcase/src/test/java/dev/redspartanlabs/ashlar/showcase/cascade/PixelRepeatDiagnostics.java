package dev.redspartanlabs.ashlar.showcase.cascade;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DIAGNOSTIC ONLY (PR #31 investigation, final round). The focus-visible sort button's picture is captured many times, in the same browser session and in fresh ones, on both renderings,
 * and the captures are analysed: how many distinct pictures each rendering produces, which pixels vary and by how much, whether the migrated rendering ever equals the legacy one, whether
 * the order or the position in a session matters. A set of deliberately broken migrated renderings (no ring, a thinner ring, a displaced ring, a weaker ring, another radius, a change
 * inside the button, a change outside it) is captured too, and the proposed tolerance is evaluated against all of it. The tolerance is NOT enabled anywhere: the assertions in
 * {@link TablePixelTest} stay exact. Everything is reported on standard output as {@code PIXREP|...} lines, a {@code PIXREP-JSON|...} line per configuration and images through
 * {@link PixelDiagnostics#emitPng}.
 */
final class PixelRepeatDiagnostics {

    private PixelRepeatDiagnostics() {
    }

    /** Every configuration that failed in either CI run (run 1: civic, ledger, summit, veridian; run 2: civic, fathom, ledger, axiom, harbor, keystone, summit). */
    static final List<String> CONFIGS = List.of("light/civic", "light/ledger", "light/fathom", "dark/summit", "dark/veridian", "dark/axiom", "dark/harbor", "dark/keystone");

    static final int REPEATS = 10;

    static boolean enabled() {
        return System.getenv("CI") != null || "1".equals(System.getenv("ASHLAR_PIXEL_REPEAT"));
    }

    /** The configurations to run: ASHLAR_PIXEL_REPEAT_CONFIGS (comma separated) narrows them, for a quick local run. */
    static List<String> configs() {
        String only = System.getenv("ASHLAR_PIXEL_REPEAT_CONFIGS");
        if (only == null || only.isBlank()) {
            return CONFIGS;
        }
        return List.of(only.split(",")).stream().map(String::trim).filter(CONFIGS::contains).toList();
    }

    static void line(String text) {
        System.out.println("PIXREP|" + text);
    }

    /** One screenshot and where it came from. {@code mode} names the session pattern, {@code index} is its position among that side's captures in that session. */
    record Capture(String mode, int session, int index, String side, String png) {
    }

    record Diff(int differing, int inBand, int inBandEdge, int outside, int maxChannel, double meanChannel, boolean sameSize, List<int[]> pixels) {
        /** The proposed tolerance, first form: at most 2 differing pixels, every one inside the ring band, no channel off by more than 12. */
        boolean policyBand() {
            return sameSize && differing <= 2 && outside == 0 && maxChannel <= 12;
        }

        /** The stricter form: as above, and every differing pixel is also an edge pixel (its neighbourhood in the legacy picture is not uniform). */
        boolean policyBandEdge() {
            return policyBand() && inBandEdge == differing;
        }
    }

    /** What the browser reported about the focused button, parsed from the probe JSON. */
    record Ring(PixelDiagnostics.Box box, double outlineWidth, double outlineOffset, String outlineColor, double spread, String shadowColor, double outer) {
    }

    static Ring ring(String info) {
        PixelDiagnostics.Box box = PixelDiagnostics.boxOf(info);
        double width = 0;
        String outlineColor = "currentcolor";
        Matcher o = Pattern.compile("\"outline\":\"\\w+ ([\\d.]+)px ([^\"]+)\"").matcher(info);
        if (o.find()) {
            width = Double.parseDouble(o.group(1));
            outlineColor = o.group(2);
        }
        double offset = 0;
        Matcher of = Pattern.compile("\"outlineOffset\":\"(-?[\\d.]+)px\"").matcher(info);
        if (of.find()) {
            offset = Double.parseDouble(of.group(1));
        }
        double spread = 2;
        String shadowColor = outlineColor;
        Matcher s = Pattern.compile("\"boxShadow\":\"([^\"]*?) (-?[\\d.]+)px (-?[\\d.]+)px ([\\d.]+)px ([\\d.]+)px\"").matcher(info);
        if (s.find()) {
            shadowColor = s.group(1);
            spread = Double.parseDouble(s.group(5));
        }
        return new Ring(box, width, offset, outlineColor, spread, shadowColor, Math.max(spread, offset + width));
    }

    // ------------------------------------------------------------------ the deliberately broken renderings

    /** CSS that breaks the migrated rendering in one specific way, applied only while the sort button is :focus-visible (or, for the last two, to the named part). */
    static Map<String, String> mutations(Ring r) {
        String s = "[data-table-sort]:focus-visible";
        Map<String, String> m = new LinkedHashMap<>();
        m.put("ring-missing", s + "{box-shadow:none !important;outline:none !important}");
        m.put("ring-1px-thinner", s + "{outline-width:" + Math.max(0, r.outlineWidth() - 1) + "px !important}");
        m.put("ring-displaced-outline-1px", s + "{outline-offset:" + (r.outlineOffset() + 1) + "px !important}");
        m.put("ring-displaced-shadow-1px", s + "{box-shadow:1px 1px 0 " + r.spread() + "px " + r.shadowColor() + " !important}");
        for (int p : new int[] {95, 90, 75}) {
            m.put("contrast-" + p + "pct", s + "{outline-color:color-mix(in srgb," + r.outlineColor() + " " + p + "%,transparent) !important;box-shadow:0 0 0 " + r.spread()
                    + "px color-mix(in srgb," + r.shadowColor() + " " + p + "%,transparent) !important}");
        }
        m.put("radius-5px", s + "{border-radius:5px !important}");
        m.put("radius-6px", s + "{border-radius:6px !important}");
        m.put("change-inside-button", "[data-table-sort-icon]{color:#000 !important}");
        m.put("change-outside-ring", ".ash-table__head{background-color:#ffffff !important}");
        return m;
    }

    // ------------------------------------------------------------------ analysis

    static boolean isEdge(BufferedImage ref, int x, int y) {
        int c = ref.getRGB(x, y) & 0xFFFFFF;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx;
                int ny = y + dy;
                if (nx >= 0 && ny >= 0 && nx < ref.getWidth() && ny < ref.getHeight() && (ref.getRGB(nx, ny) & 0xFFFFFF) != c) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The ring band: from 1 px inside the button's box to 1.5 px beyond the outermost edge of the ring (shadow spread, or outline offset plus width, whichever reaches farther). */
    static Diff diff(BufferedImage a, BufferedImage b, Ring r) {
        int w = Math.min(a.getWidth(), b.getWidth());
        int h = Math.min(a.getHeight(), b.getHeight());
        boolean same = a.getWidth() == b.getWidth() && a.getHeight() == b.getHeight();
        int differing = 0;
        int inBand = 0;
        int inBandEdge = 0;
        int outside = 0;
        int max = 0;
        long sum = 0;
        long n = 0;
        List<int[]> pixels = new ArrayList<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = a.getRGB(x, y);
                int q = b.getRGB(x, y);
                if (p == q) {
                    continue;
                }
                differing++;
                int pixelMax = 0;
                for (int shift = 0; shift <= 16; shift += 8) {
                    int d = Math.abs(((p >> shift) & 0xFF) - ((q >> shift) & 0xFF));
                    pixelMax = Math.max(pixelMax, d);
                    sum += d;
                    n++;
                }
                max = Math.max(max, pixelMax);
                double dist = r.box() == null ? Double.NaN : PixelDiagnostics.signedDistance(r.box(), x + 0.5, y + 0.5);
                if (!Double.isNaN(dist) && dist >= -1.0 && dist <= r.outer() + 1.5) {
                    inBand++;
                    if (isEdge(a, x, y)) {
                        inBandEdge++;
                    }
                } else {
                    outside++;
                }
                if (pixels.size() < 400) {
                    pixels.add(new int[] {x, y, pixelMax});
                }
            }
        }
        return new Diff(differing, inBand, inBandEdge, outside, max, n == 0 ? 0 : (double) sum / n, same, pixels);
    }

    static String describe(Diff d) {
        return "differing=" + d.differing() + " inBand=" + d.inBand() + " inBandEdge=" + d.inBandEdge() + " outside=" + d.outside() + " maxChannel=" + d.maxChannel()
                + " meanChannel=" + String.format("%.2f", d.meanChannel()) + " sameSize=" + d.sameSize() + " policyBand=" + (d.policyBand() ? "ACCEPT" : "reject")
                + " policyBandEdge=" + (d.policyBandEdge() ? "ACCEPT" : "reject");
    }

    static String modal(List<Capture> captures, String side) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        captures.stream().filter(c -> c.side().equals(side)).forEach(c -> counts.merge(c.png(), 1, Integer::sum));
        return counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
    }

    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    static void report(String configId, String fieldId, String info, List<Capture> captures, Map<String, String> broken) throws Exception {
        String id = ("repeat-" + configId + "-" + fieldId).replace('/', '-');
        Ring ring = ring(info);
        line(id + " ring box=" + ring.box() + " outlineWidth=" + ring.outlineWidth() + " outlineOffset=" + ring.outlineOffset() + " shadowSpread=" + ring.spread() + " outerExtent=" + ring.outer()
                + " band=[-1.0," + (ring.outer() + 1.5) + "]");
        // A global letter for every distinct picture, in order of first appearance (legacy captures come first).
        Map<String, String> letter = new LinkedHashMap<>();
        for (Capture c : captures) {
            letter.computeIfAbsent(c.png(), k -> String.valueOf(LETTERS.charAt(Math.min(letter.size(), LETTERS.length() - 1))));
        }
        Map<String, BufferedImage> images = new HashMap<>();
        for (String png : letter.keySet()) {
            images.put(png, PixelDiagnostics.decode(png));
        }
        String legacyModal = modal(captures, "legacy");
        String migratedModal = modal(captures, "migrated");
        for (String side : List.of("legacy", "migrated")) {
            List<Capture> mine = captures.stream().filter(c -> c.side().equals(side)).toList();
            Set<String> distinct = new LinkedHashSet<>();
            mine.forEach(c -> distinct.add(c.png()));
            Map<String, Integer> counts = new TreeMap<>();
            mine.forEach(c -> counts.merge(letter.get(c.png()), 1, Integer::sum));
            line(id + " " + side + ": captures=" + mine.size() + " distinctPictures=" + distinct.size() + " pictureCounts=" + counts + " modal=" + letter.get(side.equals("legacy") ? legacyModal : migratedModal));
        }
        // Sequences, per session pattern: the letter of each capture in order, side by side.
        Map<String, List<Capture>> bySession = new LinkedHashMap<>();
        captures.forEach(c -> bySession.computeIfAbsent(c.mode() + "#" + c.session(), k -> new ArrayList<>()).add(c));
        Map<String, StringBuilder> bySequence = new LinkedHashMap<>();
        for (Map.Entry<String, List<Capture>> e : bySession.entrySet()) {
            String mode = e.getKey().substring(0, e.getKey().indexOf('#'));
            StringBuilder sb = bySequence.computeIfAbsent(mode, k -> new StringBuilder());
            sb.append("[");
            for (Capture c : e.getValue()) {
                sb.append(c.side().charAt(0) == 'l' ? "L" : "M").append(letter.get(c.png())).append(' ');
            }
            sb.append("] ");
        }
        bySequence.forEach((mode, sb) -> line(id + " sequence " + mode + ": " + sb));
        // Does the migrated rendering ever equal the legacy one? Does the first capture of a session differ?
        long migratedEqualsLegacy = captures.stream().filter(c -> c.side().equals("migrated") && c.png().equals(legacyModal)).count();
        long migratedTotal = captures.stream().filter(c -> c.side().equals("migrated")).count();
        long legacyEqualsModal = captures.stream().filter(c -> c.side().equals("legacy") && c.png().equals(legacyModal)).count();
        long legacyTotal = captures.stream().filter(c -> c.side().equals("legacy")).count();
        line(id + " migratedCapturesEqualToLegacyModal=" + migratedEqualsLegacy + "/" + migratedTotal + " legacyCapturesEqualToLegacyModal=" + legacyEqualsModal + "/" + legacyTotal);
        long firstDiffers = 0;
        long firstTotal = 0;
        long laterDiffers = 0;
        long laterTotal = 0;
        for (String side : List.of("legacy", "migrated")) {
            String modal = side.equals("legacy") ? legacyModal : migratedModal;
            for (List<Capture> session : bySession.values()) {
                List<Capture> sideCaptures = session.stream().filter(c -> c.side().equals(side)).toList();
                for (int i = 0; i < sideCaptures.size(); i++) {
                    boolean differs = !sideCaptures.get(i).png().equals(modal);
                    if (i == 0) {
                        firstTotal++;
                        firstDiffers += differs ? 1 : 0;
                    } else {
                        laterTotal++;
                        laterDiffers += differs ? 1 : 0;
                    }
                }
            }
        }
        line(id + " firstCaptureOfASideInASessionDiffersFromItsModal=" + firstDiffers + "/" + firstTotal + " laterCapturesDifferFromTheirModal=" + laterDiffers + "/" + laterTotal);
        for (String order : List.of("fresh-legacy-first", "fresh-migrated-first")) {
            long total = captures.stream().filter(c -> c.mode().equals(order)).count();
            long off = captures.stream().filter(c -> c.mode().equals(order) && !c.png().equals(c.side().equals("legacy") ? legacyModal : migratedModal)).count();
            line(id + " " + order + ": captures=" + total + " differingFromTheirModal=" + off);
        }
        // Which pixels vary within a side, and what colours they take.
        for (String side : List.of("legacy", "migrated")) {
            String modal = side.equals("legacy") ? legacyModal : migratedModal;
            BufferedImage m = images.get(modal);
            Map<String, Set<String>> pixels = new TreeMap<>();
            for (Capture c : captures) {
                if (!c.side().equals(side) || c.png().equals(modal)) {
                    continue;
                }
                BufferedImage img = images.get(c.png());
                for (int y = 0; y < Math.min(m.getHeight(), img.getHeight()); y++) {
                    for (int x = 0; x < Math.min(m.getWidth(), img.getWidth()); x++) {
                        if (m.getRGB(x, y) != img.getRGB(x, y)) {
                            pixels.computeIfAbsent(x + "," + y, k -> new LinkedHashSet<>()).add(PixelDiagnostics.hex(img.getRGB(x, y)).substring(0, 7));
                        }
                    }
                }
            }
            int inBand = 0;
            int edge = 0;
            StringBuilder list = new StringBuilder();
            for (Map.Entry<String, Set<String>> e : pixels.entrySet()) {
                String[] xy = e.getKey().split(",");
                int x = Integer.parseInt(xy[0]);
                int y = Integer.parseInt(xy[1]);
                double dist = ring.box() == null ? Double.NaN : PixelDiagnostics.signedDistance(ring.box(), x + 0.5, y + 0.5);
                boolean band = !Double.isNaN(dist) && dist >= -1.0 && dist <= ring.outer() + 1.5;
                inBand += band ? 1 : 0;
                edge += band && isEdge(m, x, y) ? 1 : 0;
                if (list.length() < 1500) {
                    list.append('(').append(e.getKey()).append(" modal=").append(PixelDiagnostics.hex(m.getRGB(x, y)).substring(0, 7)).append(" seen=").append(e.getValue()).append(band ? " band" : " OUTSIDE")
                            .append(") ");
                }
            }
            line(id + " " + side + " pixelsThatVaryAcrossCaptures=" + pixels.size() + " inBand=" + inBand + " inBandEdge=" + edge + " " + list);
        }
        // Each distinct migrated picture against the legacy modal, and the proposed tolerance applied to it.
        BufferedImage lm = images.get(legacyModal);
        BufferedImage worst = null;
        int worstCount = -1;
        for (Map.Entry<String, String> e : letter.entrySet()) {
            boolean isMigrated = captures.stream().anyMatch(c -> c.png().equals(e.getKey()) && c.side().equals("migrated"));
            if (!isMigrated || e.getKey().equals(legacyModal)) {
                continue;
            }
            Diff d = diff(lm, images.get(e.getKey()), ring);
            line(id + " migratedPicture " + e.getValue() + " versus legacy modal " + letter.get(legacyModal) + ": " + describe(d));
            if (d.differing() > worstCount) {
                worstCount = d.differing();
                worst = images.get(e.getKey());
            }
        }
        // The deliberately broken renderings against the legacy modal.
        for (Map.Entry<String, String> e : broken.entrySet()) {
            Diff d = diff(lm, PixelDiagnostics.decode(e.getValue()), ring);
            line(id + " broken " + e.getKey() + ": " + describe(d));
        }
        // Single-pixel changes made directly on a copy of the legacy picture, to probe the tolerance's edges.
        if (ring.box() != null) {
            int ox = (int) Math.min(lm.getWidth() - 1, ring.box().x() + 10);
            int oy = (int) Math.min(lm.getHeight() - 1, ring.box().bottom() + ring.outer() + 8);
            int ix = (int) (ring.box().x() - ring.outer() + 0.5);
            int iy = (int) (ring.box().y() + ring.box().h() / 2);
            for (int delta : new int[] {1, 12, 13, 60}) {
                line(id + " synthetic one pixel outside the band (" + ox + "," + oy + ") changed by " + delta + ": " + describe(diff(lm, tweak(lm, ox, oy, delta), ring)));
                line(id + " synthetic one pixel on the ring (" + ix + "," + iy + ") changed by " + delta + ": " + describe(diff(lm, tweak(lm, ix, iy, delta), ring)));
            }
            line(id + " synthetic three pixels on the ring changed by 5: " + describe(diff(lm, tweak(tweak(tweak(lm, ix, iy, 5), ix, iy + 1, 5), ix, iy + 2, 5), ring)));
        }
        if (worst != null || migratedModal != null) {
            BufferedImage other = worst != null ? worst : images.get(migratedModal);
            BufferedImage diffImage = PixelDiagnostics.differenceImage(lm, other);
            PixelDiagnostics.Stats stats = PixelDiagnostics.analyze(lm, other, ring.box());
            PixelDiagnostics.emitPng(id, "legacy-modal-vs-widest-migrated", PixelDiagnostics.encode(PixelDiagnostics.zoomSheet(lm, other, diffImage, stats, ring.box())));
        }
        StringBuilder json = new StringBuilder("{\"id\":" + PixelDiagnostics.json(id) + ",\"config\":" + PixelDiagnostics.json(configId) + ",\"captures\":" + captures.size());
        json.append(",\"legacyDistinct\":").append(captures.stream().filter(c -> c.side().equals("legacy")).map(Capture::png).distinct().count());
        json.append(",\"migratedDistinct\":").append(captures.stream().filter(c -> c.side().equals("migrated")).map(Capture::png).distinct().count());
        json.append(",\"migratedEqualToLegacyModal\":").append(migratedEqualsLegacy).append(",\"migratedTotal\":").append(migratedTotal);
        json.append(",\"legacyEqualToModal\":").append(legacyEqualsModal).append(",\"legacyTotal\":").append(legacyTotal);
        json.append(",\"firstDiffers\":").append(firstDiffers).append(",\"firstTotal\":").append(firstTotal).append(",\"laterDiffers\":").append(laterDiffers).append(",\"laterTotal\":").append(laterTotal);
        json.append(",\"broken\":{");
        boolean firstEntry = true;
        for (Map.Entry<String, String> e : broken.entrySet()) {
            Diff d = diff(lm, PixelDiagnostics.decode(e.getValue()), ring);
            json.append(firstEntry ? "" : ",").append(PixelDiagnostics.json(e.getKey())).append(":{\"differing\":").append(d.differing()).append(",\"maxChannel\":").append(d.maxChannel())
                    .append(",\"outside\":").append(d.outside()).append(",\"policyBand\":").append(d.policyBand()).append(",\"policyBandEdge\":").append(d.policyBandEdge()).append('}');
            firstEntry = false;
        }
        System.out.println("PIXREP-JSON|" + json + "}}");
    }

    private static int shift(int channel, int delta) {
        return channel + delta > 255 ? channel - delta : channel + delta;
    }

    static BufferedImage tweak(BufferedImage src, int x, int y, int delta) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int j = 0; j < src.getHeight(); j++) {
            for (int i = 0; i < src.getWidth(); i++) {
                copy.setRGB(i, j, src.getRGB(i, j));
            }
        }
        if (x >= 0 && y >= 0 && x < copy.getWidth() && y < copy.getHeight()) {
            int p = src.getRGB(x, y);
            // Brighter, or darker where the channel would saturate, so the change is always exactly delta.
            int r = shift((p >> 16) & 0xFF, delta);
            int g = shift((p >> 8) & 0xFF, delta);
            int b = shift(p & 0xFF, delta);
            copy.setRGB(x, y, (r << 16) | (g << 8) | b);
        }
        return copy;
    }
}
