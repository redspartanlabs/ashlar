package dev.redspartanlabs.ashlar.showcase.cascade;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DIAGNOSTIC ONLY (PR #31 investigation, hover round). Two hover pictures differed in the last CI run (light/foundry "active sort button hover" and dark/none "sort button hover"), without a
 * focus ring and outside the configurations the earlier diagnostics looked at. This class explains pairs of pictures that are not about a ring: every differing pixel with its colors, whether
 * it lies on an edge (its neighborhood is not uniform), where it is relative to the hovered element, a few neighborhoods, and a comparison of every computed property and every box of the
 * case's subtree between the two renderings. Nothing here is asserted, no tolerance is enabled anywhere and nothing can fail a test. Output is {@code PIXHOV|...} lines on standard output.
 */
final class PixelHoverDiagnostics {

    private PixelHoverDiagnostics() {
    }

    /** One configuration and case that failed in CI run 43285178797. */
    record Pair(String config, String caseKey) {
    }

    static final List<Pair> PAIRS = List.of(new Pair("light/foundry", "active sort button hover"), new Pair("dark/none", "sort button hover"));

    /** Captures per side per case in the sweep over all hover cases and configurations. */
    static final int SWEEP_CAPTURES = 3;

    static boolean enabled() {
        return PixelRepeatDiagnostics.enabled();
    }

    /** ASHLAR_PIXEL_REPEAT_CONFIGS (comma separated) narrows the configurations, for a quick local run. */
    static List<String> onlyConfigs() {
        String only = System.getenv("ASHLAR_PIXEL_REPEAT_CONFIGS");
        return only == null || only.isBlank() ? List.of() : List.of(only.split(",")).stream().map(String::trim).toList();
    }

    static List<Pair> pairs() {
        List<String> only = onlyConfigs();
        return only.isEmpty() ? PAIRS : PAIRS.stream().filter(p -> only.contains(p.config())).toList();
    }

    static void line(String text) {
        System.out.println("PIXHOV|" + text);
    }

    // ------------------------------------------------------------------ pictures

    private static int maxChannel(int p, int q) {
        int max = 0;
        for (int shift = 0; shift <= 16; shift += 8) {
            max = Math.max(max, Math.abs(((p >> shift) & 0xFF) - ((q >> shift) & 0xFF)));
        }
        return max;
    }

    /** Statistics of one pair, as plain numbers so the self-check can verify them. */
    record Detail(int differing, int maxChannel, int edgeInLegacy, int edgeInMigrated, int insideBox, int outsideBox, boolean sameSize, int minX, int minY, int maxX, int maxY) {
    }

    static Detail detail(BufferedImage a, BufferedImage b, PixelDiagnostics.Box box) {
        int w = Math.min(a.getWidth(), b.getWidth());
        int h = Math.min(a.getHeight(), b.getHeight());
        int n = 0;
        int max = 0;
        int edgeA = 0;
        int edgeB = 0;
        int inside = 0;
        int outside = 0;
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (a.getRGB(x, y) == b.getRGB(x, y)) {
                    continue;
                }
                n++;
                max = Math.max(max, maxChannel(a.getRGB(x, y), b.getRGB(x, y)));
                edgeA += PixelRepeatDiagnostics.isEdge(a, x, y) ? 1 : 0;
                edgeB += PixelRepeatDiagnostics.isEdge(b, x, y) ? 1 : 0;
                if (box != null && PixelDiagnostics.signedDistance(box, x + 0.5, y + 0.5) <= 0) {
                    inside++;
                } else {
                    outside++;
                }
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        return new Detail(n, max, edgeA, edgeB, inside, outside, a.getWidth() == b.getWidth() && a.getHeight() == b.getHeight(), n == 0 ? -1 : minX, n == 0 ? -1 : minY, maxX, maxY);
    }

    private static String neighborhood(BufferedImage img, int cx, int cy) {
        StringBuilder sb = new StringBuilder();
        for (int y = cy - 2; y <= cy + 2; y++) {
            sb.append(" | ");
            for (int x = cx - 2; x <= cx + 2; x++) {
                sb.append(x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight() ? "(out)" : PixelDiagnostics.hex(img.getRGB(x, y)).substring(1, 7)).append(' ');
            }
        }
        return sb.toString();
    }

    /** Explains one pair of pictures of a hover (or any non-ring) case. {@code box} is the hovered element's box in picture coordinates, or null. */
    static void pairDetail(String id, BufferedImage a, BufferedImage b, PixelDiagnostics.Box box) {
        try {
            Detail d = detail(a, b, box);
            line(id + " pair sameSize=" + d.sameSize() + " differing=" + d.differing() + " maxChannel=" + d.maxChannel() + " bbox=[" + d.minX() + "," + d.minY() + " .. " + d.maxX() + "," + d.maxY()
                    + "] edgePixelsInFirstPicture=" + d.edgeInLegacy() + " edgePixelsInSecondPicture=" + d.edgeInMigrated() + " insideHoveredBox=" + d.insideBox() + " outsideHoveredBox=" + d.outsideBox()
                    + " hoveredBox=" + (box == null ? "unknown" : box));
            int w = Math.min(a.getWidth(), b.getWidth());
            int h = Math.min(a.getHeight(), b.getHeight());
            int listed = 0;
            for (int y = 0; y < h && listed < 40; y++) {
                for (int x = 0; x < w && listed < 40; x++) {
                    int p = a.getRGB(x, y);
                    int q = b.getRGB(x, y);
                    if (p == q) {
                        continue;
                    }
                    String where = box == null ? "unknown" : String.format("%.1f", PixelDiagnostics.signedDistance(box, x + 0.5, y + 0.5));
                    line(id + " pixel " + x + "," + y + " first=" + PixelDiagnostics.hex(p).substring(0, 7) + " second=" + PixelDiagnostics.hex(q).substring(0, 7) + " maxChannel=" + maxChannel(p, q)
                            + " edgeInFirst=" + PixelRepeatDiagnostics.isEdge(a, x, y) + " edgeInSecond=" + PixelRepeatDiagnostics.isEdge(b, x, y) + " signedDistanceToHoveredBox=" + where);
                    if (listed < 2) {
                        line(id + " neighborhood first  (5x5 around " + x + "," + y + ")" + neighborhood(a, x, y));
                        line(id + " neighborhood second (5x5 around " + x + "," + y + ")" + neighborhood(b, x, y));
                    }
                    listed++;
                }
            }
        } catch (Throwable t) {
            line(id + " pairDetail failed: " + t);
        }
    }

    // ------------------------------------------------------------------ computed styles and boxes

    /** Every computed property (custom properties included), every box and the running-animation count of every element in the case's field, with the case's state already forced. */
    static String styleScript(String fieldId) {
        return "(function(){var out=[];var root=document.querySelector('[data-field=\"" + fieldId + "\"]');"
                + "var els=[root].concat([].slice.call(root.querySelectorAll('*'))).filter(function(e){return !(e.closest('svg')&&e.tagName.toLowerCase()!=='svg')});"
                + "els.forEach(function(e,n){var s=getComputedStyle(e);var name=n+':'+e.tagName.toLowerCase();"
                + "for(var i=0;i<s.length;i++){var p=s[i];out.push([name,p,s.getPropertyValue(p)].join('\\t'))}"
                + "var r=e.getBoundingClientRect();['x','y','width','height'].forEach(function(k){out.push([name,'@'+k,String(Math.round(r[k]*100)/100)].join('\\t'))});"
                + "out.push([name,'@animations',String(e.getAnimations().length)].join('\\t'));out.push([name,'@hovered',String(e.matches(':hover'))].join('\\t'))});"
                + "return out.join('\\n')})()";
    }

    private static Map<String, String> parse(String text) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String row : text.split("\n")) {
            String[] parts = row.split("\t", 3);
            if (parts.length == 3) {
                map.put(parts[0] + "\t" + parts[1], parts[2]);
            }
        }
        return map;
    }

    /** Compares the dumps of two renderings. Custom properties (the legacy rendering's Tailwind variables) are counted apart from the properties that style the elements. */
    static void styleDiff(String id, String legacyDump, String migratedDump) {
        try {
            Map<String, String> a = parse(legacyDump);
            Map<String, String> b = parse(migratedDump);
            Set<String> keys = new LinkedHashSet<>(a.keySet());
            keys.addAll(b.keySet());
            List<String> differences = new ArrayList<>();
            Set<String> custom = new LinkedHashSet<>();
            int layout = 0;
            for (String key : keys) {
                String va = a.get(key);
                String vb = b.get(key);
                if (va != null && va.equals(vb)) {
                    continue;
                }
                String prop = key.substring(key.indexOf('\t') + 1);
                if (prop.startsWith("--")) {
                    custom.add(prop);
                    continue;
                }
                layout += prop.startsWith("@") ? 1 : 0;
                differences.add(key.replace('\t', ' ') + " legacy=" + abbreviate(va) + " migrated=" + abbreviate(vb));
            }
            line(id + " styles compared=" + keys.size() + " legacyEntries=" + a.size() + " migratedEntries=" + b.size() + " differingStyleOrBoxEntries=" + differences.size() + " ofWhichBoxOrStateEntries=" + layout
                    + " differingCustomProperties=" + custom.size());
            for (int i = 0; i < Math.min(differences.size(), 80); i++) {
                line(id + " styleDifference " + differences.get(i));
            }
            line(id + " customPropertiesThatDiffer " + (custom.size() > 40 ? new ArrayList<>(custom).subList(0, 40) + " ... (" + custom.size() + " in all)" : custom));
        } catch (Throwable t) {
            line(id + " styleDiff failed: " + t);
        }
    }

    private static String abbreviate(String s) {
        return s == null ? "(absent)" : s.length() > 120 ? s.substring(0, 120) + "..." : s;
    }

    // ------------------------------------------------------------------ self-check

    /** Checks the arithmetic above on pictures with known differences; printed, never asserted. */
    static void selfCheck() {
        try {
            BufferedImage a = new BufferedImage(12, 12, BufferedImage.TYPE_INT_RGB);
            BufferedImage b = new BufferedImage(12, 12, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < 12; y++) {
                for (int x = 0; x < 12; x++) {
                    int c = x < 6 ? 0xFFFFFF : 0x000000; // a vertical edge between x=5 and x=6
                    a.setRGB(x, y, c);
                    b.setRGB(x, y, c);
                }
            }
            b.setRGB(5, 4, 0xF0F0F0); // on the edge, 15 off
            b.setRGB(1, 1, 0xFEFEFE); // in a flat area, 1 off
            Detail d = detail(a, b, new PixelDiagnostics.Box(3, 3, 6, 6));
            boolean ok = d.differing() == 2 && d.maxChannel() == 15 && d.edgeInLegacy() == 1 && d.insideBox() == 1 && d.outsideBox() == 1 && d.sameSize();
            String dump = "0:div\twidth\t10px\n0:div\t--tw-x\t1\n0:div\t@x\t4";
            Map<String, String> m = parse(dump);
            ok &= m.size() == 3 && m.get("0:div\twidth").equals("10px");
            line("selfCheck " + (ok ? "OK" : "FAILED") + " " + d);
        } catch (Throwable t) {
            line("selfCheck failed: " + t);
        }
    }
}
