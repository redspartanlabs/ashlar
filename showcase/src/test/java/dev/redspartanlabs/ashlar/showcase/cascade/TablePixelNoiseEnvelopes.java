package dev.redspartanlabs.ashlar.showcase.cascade;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The measured noise envelopes of {@link TablePixelTest} (issue #34): where, on Linux CI, the same page has been seen to render with a few different pixels, and which colors it
 * alternated between. A rejected comparison may be retried only when its whole difference lies inside one of these envelopes (see {@link #check}); everything else fails at once.
 *
 * <p>The production table holds exactly what was measured, nothing more. Its source is {@code parity/table/instability/evidence.tsv} (with the pictures it was read from and the
 * method in the same folder's README), and {@code TablePixelRetryTest} fails if the table and the evidence ever disagree. An envelope changes only through a reviewed change backed
 * by new measured evidence, never to let a failing run pass.
 *
 * <p>Coordinates are picture coordinates: the picture is the field's box with 6 px around it. Colors are RGB ({@code 0xRRGGBB}); a pair is unordered, because either rendering was
 * seen with either color.
 */
final class TablePixelNoiseEnvelopes {

    private TablePixelNoiseEnvelopes() {
    }

    /** The two parity runs of TablePixelTest. Only the run with Tailwind has envelopes. */
    enum Mode {
        WITH_TAILWIND, TAILWIND_FREE
    }

    record Point(int x, int y) {
        @Override
        public String toString() {
            return "(" + x + "," + y + ")";
        }
    }

    /** Two RGB colors, unordered. */
    record ColorPair(int low, int high) {
        static ColorPair of(int a, int b) {
            a &= 0xFFFFFF;
            b &= 0xFFFFFF;
            return a <= b ? new ColorPair(a, b) : new ColorPair(b, a);
        }

        /** The largest difference over red, green and blue. */
        int channel() {
            int max = 0;
            for (int shift = 0; shift <= 16; shift += 8) {
                max = Math.max(max, Math.abs(((low >> shift) & 0xFF) - ((high >> shift) & 0xFF)));
            }
            return max;
        }

        @Override
        public String toString() {
            return String.format("%06X<->%06X", low, high);
        }
    }

    /** What may differ at one pixel: only the documented color pairs, each within the channel limit (the largest difference observed there). */
    record PixelRule(int channelLimit, Set<ColorPair> pairs) {
        PixelRule {
            pairs = Set.copyOf(pairs);
        }
    }

    /** The envelope of one case in one configuration: the pixels that were seen to differ, and how many at most differed at once. */
    record Envelope(String fieldId, String caseKey, String configId, int maxPixels, Map<Point, PixelRule> pixels) {
        Envelope {
            pixels = Map.copyOf(pixels);
        }
    }

    /** A table of envelopes. The production one is {@link #PRODUCTION}; the retry mechanism's controls supply their own, to the same {@link #check}. */
    record Table(String name, List<Envelope> envelopes) {
        Table {
            envelopes = List.copyOf(envelopes);
        }

        Optional<Envelope> find(String configId, String fieldId) {
            return envelopes.stream().filter(e -> e.configId().equals(configId) && e.fieldId().equals(fieldId)).findFirst();
        }
    }

    static Map.Entry<Point, PixelRule> pixel(int x, int y, int colorA, int colorB) {
        ColorPair pair = ColorPair.of(colorA, colorB);
        return Map.entry(new Point(x, y), new PixelRule(pair.channel(), Set.of(pair)));
    }

    @SafeVarargs
    static Envelope envelope(String fieldId, String caseKey, String configId, int maxPixels, Map.Entry<Point, PixelRule>... pixels) {
        return new Envelope(fieldId, caseKey, configId, maxPixels, Map.ofEntries(pixels));
    }

    private static final String SORT_HOVER = "sort button hover";
    private static final String ACTIVE_SORT_HOVER = "active sort button hover";
    private static final String CLEAR_FOCUS = "clear selection focus-visible";
    private static final String CLEAR_HOVER = "clear selection hover";

    /** The eight measured envelopes (see evidence.tsv for every observation behind them). */
    static final Table PRODUCTION = new Table("production", List.of(
            // light/foundry: the first letter's left fringe of the hovered sort button
            envelope("tb6", SORT_HOVER, "light/foundry", 2, pixel(22, 23, 0xEDF1ED, 0xEDF1EE), pixel(22, 24, 0xEDE3C5, 0xEDE7E0)),
            envelope("tb8", ACTIVE_SORT_HOVER, "light/foundry", 2, pixel(22, 23, 0xEDF1ED, 0xEDF1EE), pixel(22, 24, 0xEDE3C5, 0xEDE7E0)),
            // dark/fathom: the selection bar's bottom-left corner
            envelope("tb19", CLEAR_FOCUS, "dark/fathom", 2, pixel(23, 50, 0x013142, 0x013A4D), pixel(24, 50, 0x003E53, 0x013A4E)),
            envelope("tb20", CLEAR_HOVER, "dark/fathom", 2, pixel(23, 50, 0x013142, 0x013A4D), pixel(24, 50, 0x003E53, 0x013A4E)),
            // dark/harbor: the selection bar's corners
            envelope("tb19", CLEAR_FOCUS, "dark/harbor", 11, pixel(23, 21, 0x003C5B, 0x013B58), pixel(23, 48, 0x003C5B, 0x013B58), pixel(23, 49, 0x0B2331, 0x0E1F29),
                    pixel(24, 49, 0x04344D, 0x04354F), pixel(25, 50, 0x0A2636, 0x0D1F2A), pixel(26, 50, 0x062F45, 0x0A2838), pixel(27, 50, 0x04344E, 0x063046),
                    pixel(28, 50, 0x013A58, 0x023854), pixel(197, 50, 0x023854, 0x023A57), pixel(198, 50, 0x04344E, 0x063046), pixel(199, 50, 0x072E43, 0x0A2838)),
            envelope("tb20", CLEAR_HOVER, "dark/harbor", 7, pixel(23, 48, 0x003C5B, 0x013B58), pixel(23, 49, 0x0B2331, 0x0E1F29), pixel(24, 49, 0x04344D, 0x04354F),
                    pixel(25, 50, 0x0A2636, 0x0D1F2A), pixel(26, 50, 0x062F45, 0x0A2838), pixel(27, 50, 0x04354E, 0x063046), pixel(28, 50, 0x023854, 0x023A57)),
            // light/sterling: the selection bar's bottom-left corner
            envelope("tb19", CLEAR_FOCUS, "light/sterling", 1, pixel(23, 50, 0xE1EAF8, 0xE4ECF8)),
            envelope("tb20", CLEAR_HOVER, "light/sterling", 1, pixel(23, 50, 0xE1EAF8, 0xE4ECF8))));

    /** Whether a rejected comparison may be retried, why, and (when it may) the per-pixel checks that passed. */
    record Eligibility(boolean eligible, String reason, List<String> checks) {
        static Eligibility no(String reason, List<String> checks) {
            return new Eligibility(false, reason, List.copyOf(checks));
        }
    }

    /**
     * Whether the comparison is eligible for a retry under {@code table}. All of these must hold: the policy rejected it; it comes from the run with Tailwind; the table has an
     * envelope for its configuration and field, for this very case; both pictures have the same size; at least one pixel differs; every differing pixel is opaque on both sides, lies
     * at one of the envelope's coordinates, and shows one of that coordinate's documented color pairs within its channel limit; and no more pixels differ than the envelope allows.
     * Any nonempty subset of the documented pixels qualifies; no undocumented coordinate, color or channel difference does.
     */
    static Eligibility check(Table table, Mode mode, String configId, String fieldId, String caseKey, PixelPolicy.Verdict verdict, BufferedImage legacy, BufferedImage migrated) {
        List<String> checks = new ArrayList<>();
        if (verdict.accepted()) {
            return Eligibility.no("the policy accepted the pair; there is nothing to retry", checks);
        }
        if (mode != Mode.WITH_TAILWIND) {
            return Eligibility.no("the run without Tailwind has no noise envelope", checks);
        }
        Optional<Envelope> found = table.find(configId, fieldId);
        if (found.isEmpty()) {
            return Eligibility.no("no noise envelope for " + configId + " " + fieldId + " in the " + table.name() + " table", checks);
        }
        Envelope envelope = found.get();
        if (!envelope.caseKey().equals(caseKey)) {
            return Eligibility.no("the " + configId + " " + fieldId + " envelope is for '" + envelope.caseKey() + "', not '" + caseKey + "'", checks);
        }
        if (legacy == null || migrated == null) {
            return Eligibility.no("the pictures could not be decoded", checks);
        }
        if (legacy.getWidth() != migrated.getWidth() || legacy.getHeight() != migrated.getHeight()) {
            return Eligibility.no("the pictures have different sizes", checks);
        }
        String where = configId + " " + fieldId + " envelope";
        int differing = 0;
        for (int y = 0; y < legacy.getHeight(); y++) {
            for (int x = 0; x < legacy.getWidth(); x++) {
                int p = legacy.getRGB(x, y);
                int q = migrated.getRGB(x, y);
                if (p == q) {
                    continue;
                }
                differing++;
                Point point = new Point(x, y);
                if (((p >>> 24) & 0xFF) != 0xFF || ((q >>> 24) & 0xFF) != 0xFF) {
                    return Eligibility.no("pixel " + point + " is not opaque on both sides (alpha " + ((p >>> 24) & 0xFF) + " and " + ((q >>> 24) & 0xFF) + ")", checks);
                }
                PixelRule rule = envelope.pixels().get(point);
                if (rule == null) {
                    return Eligibility.no("pixel " + point + " lies outside the " + where, checks);
                }
                ColorPair pair = ColorPair.of(p, q);
                if (!rule.pairs().contains(pair)) {
                    return Eligibility.no("pixel " + point + " shows " + pair + ", which is not a documented color pair there " + rule.pairs(), checks);
                }
                if (pair.channel() > rule.channelLimit()) {
                    return Eligibility.no("pixel " + point + " differs by " + pair.channel() + ", more than the " + rule.channelLimit() + " observed there", checks);
                }
                if (differing > envelope.maxPixels()) {
                    return Eligibility.no(differing + " pixels differ, the " + where + " allows at most " + envelope.maxPixels(), checks);
                }
                checks.add(point + " " + pair + " channel " + pair.channel() + "/" + rule.channelLimit());
            }
        }
        if (differing == 0) {
            return Eligibility.no("no pixel differs (the encoded pictures differ, their pixels do not)", checks);
        }
        return new Eligibility(true, "inside the " + where + ": " + differing + " of its " + envelope.pixels().size() + " documented pixels, each a documented color pair",
                List.copyOf(checks));
    }
}
